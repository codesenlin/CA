#!/usr/bin/env node

import process from "node:process";
import fs from "node:fs";
import pathUtils from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = pathUtils.dirname(__filename);

// 任务和 lib 仍处于 CommonJS 阶段；入口先迁 ESM，用 createRequire 兼容。
var Tasks = {};

// CLI / 流水线共用的任务日志格式，避免 execute 与 procArgs 各写一份
function logTaskStart(task) {
	if (task != "help") console.log("Process " + task);
}
function logTaskSuccess(task, startTime) {
	if (task == "help") return;
	console.log("Success " + task + (startTime ? " in " + ((Date.now() - startTime) / 1000).toFixed(2) + "s" : ""));
}
function logTaskFailure(task, reason) {
	console.log("Failed  " + task);
	if (reason !== undefined) console.log(reason);
}
var Build = {
	execute: function (task, args) {
		// help 只是打印用法，不需要 Process/Success 这类流水线日志
		logTaskStart(task);
		return executeTask(task, args)
			.then(result => {
				logTaskSuccess(task);
				return result;
			}, reason => {
				logTaskFailure(task, reason);
				throw reason;
			});
	},
	task: function (task, args) {
		return () => this.execute(task, args);
	},
	pipe: function (task, args) {
		return result => this.execute(task, [result, args]);
	},
	loadTasks: async function (path) {
		await initTasks(path);
	},
	Result: {
		OK: "ok"
	}
}
var context = Build.context = Object.create(Build);
function getFileName(nameWithExt) {
	var p = nameWithExt.lastIndexOf(".");
	return p >= 0 ? nameWithExt.slice(0, p) : nameWithExt;
}
function procArgs() {
	var i, args = process.argv, task, taskArgs = [], time = Date.now();
	task = args[2] || "help";
	for (i = 3; i < args.length; i++) {
		taskArgs.push(args[i]);
	}
	// help 只是打印用法，不需要 Process/Success 这类流水线日志
	logTaskStart(task);
	try {
		if (!(task in Tasks)) throw new Error("Task not found: " + task);
		if (Tasks[task].input != "cli") throw new Error("Task is not available in CLI: " + task);
		executeTask(task, taskArgs)
			.then(result => {
				logTaskSuccess(task, time);
				return result;
			}, reason => {
				logTaskFailure(task, reason);
				process.exit(1);
			});
	} catch (e) {
		console.log(String(e));
		// 未知任务 / 非 CLI 任务也返回非 0，方便脚本/CI 判断失败
		process.exitCode = 1;
	}
}
async function initTasks(path) {
	var i, e, fn, ext, files = fs.readdirSync(path);
	for (i = 0; i < files.length; i++) {
		e = pathUtils.resolve(path, files[i]);
		if (fs.statSync(e).isDirectory()) {
			await initTasks(e);
		} else {
			ext = pathUtils.extname(e);
			fn = pathUtils.basename(e, ext);
			if (ext.toLowerCase() == ".mjs" && fn.length != 0) {
				try {
					const mod = await import(pathToFileURL(e).href);
					const task = mod.default;
					if (mod.input) task.input = mod.input;
					Tasks[fn] = task;
				} catch (error) {
					console.log("Cannot load task: " + fn + "\n" + error);
				}
			}
		}
	}
}
function executeTask(task, args) {
	if (!(task in Tasks)) throw new Error("Task not found: " + task);
	var result;
	try {
		result = Tasks[task](context, args);
	} catch (e) {
		return Promise.reject(e);
	}
	return result instanceof Promise ? result : Promise.resolve(result);
}
await initTasks(pathUtils.resolve(__dirname, "./tasks"));
if (pathUtils.resolve(process.argv[1] || "") === pathUtils.resolve(__filename)) {
	procArgs();
} else {
	// 兼容 ESM 下的程序化导入
}

export default Build;
