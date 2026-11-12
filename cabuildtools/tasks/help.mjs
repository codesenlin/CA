import fs from "node:fs";
import path from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";
const __dirname = path.dirname(fileURLToPath(import.meta.url));

/**
 * 构建工具帮助。
 *
 *   node ./cabuildtools/main.mjs help
 *   node ./cabuildtools/main.mjs help buildRelease
 *   node ./cabuildtools/main.mjs help caaccel
 *   node ./cabuildtools/main.mjs help caaccel test
 *
 * 普通 help 只列任务和入口；help <任务> 才展开该任务的参数与示例。
 */

const TASK_DESC = {
	buildRelease: "构建正式版脚本 + 安装器 .lib",
	buildSnapshot: "构建快照版脚本 + 安装器 .lib",
	shellBuildRelease: "打包正式版 APK（含签名、对齐）",
	shellBuildDebug: "打包调试版 APK（debuggable，走 R8）",
	shellBuildReleaseHotfix: "生成热更新包（core.js + 签名）",
	buildCLib: "构建内置命令库（caclib）",
	caaccel: "构建 / 测试 / 基准加速库",
	publishRelease: "发布正式版到配置的发布端",
	publishSnapshot: "发布快照版",
	publishHotfix: "发布热更新包",
	publishCLib: "发布内置命令库",
	generateImport: "生成 import 用的 android.jar 索引",
	clean: "清理 build 目录",
	delay: "延时若干毫秒（调试流水线用）",
	shellPrepareDebug: "准备调试版壳子资源"
};

const CHANNEL_ARGS = [
	["-beta", "测试渠道"],
	["-dev", "开发渠道"],
	["-canary", "前沿渠道"]
];

const FLAVOR_ARGS = [
	["-huangyxHUTAO", "南鸢晨星社区版风味"],
	["-flavor <名字>", "自定义风味（data/flavors/<名字>/）"]
];

const OUTPUT_ARGS = [
	["build/outputs/", "脚本中间产物（release / snapshot / snapshotLib）"],
	["build/dist/", "成品 APK、安装器 .lib、update.json"],
	["build/generated/", "生成的配置（shellConfig.json / importConfig.json）"]
];

const TASK_DETAIL = {
	buildRelease: {
		usage: "buildRelease [渠道参数] [风味参数]",
		npmUsage: "npm run build -- [渠道参数] [风味参数]",
		note: "构建正式版脚本和安装器 .lib。",
		args: CHANNEL_ARGS.concat(FLAVOR_ARGS),
		examples: [
			"npm run build",
			"npm run build -- -beta",
			"npm run build -- -huangyxHUTAO"
		],
		outputs: OUTPUT_ARGS
	},
	buildSnapshot: {
		usage: "buildSnapshot [渠道参数] [风味参数]",
		npmUsage: "npm run snapshot -- [渠道参数] [风味参数]",
		note: "构建快照版脚本和安装器 .lib。",
		args: CHANNEL_ARGS.concat(FLAVOR_ARGS),
		examples: [
			"npm run snapshot",
			"npm run snapshot -- -dev",
			"npm run snapshot -- -canary"
		],
		outputs: OUTPUT_ARGS
	},
	shellBuildRelease: {
		usage: "shellBuildRelease [渠道参数] [风味参数]",
		npmUsage: "npm run gradlew -- [渠道参数] [风味参数]",
		note: "打包正式版 APK，包含签名与 zipalign。",
		args: CHANNEL_ARGS.concat(FLAVOR_ARGS),
		examples: ["npm run gradlew", "npm run gradlew -- -huangyxHUTAO"]
	},
	shellBuildDebug: {
		usage: "shellBuildDebug [风味参数]",
		npmUsage: "npm run gradlew -- -debug [风味参数]",
		note: "打包调试版 APK，debuggable，走 R8。",
		args: FLAVOR_ARGS,
		examples: ["npm run gradlew -- -debug", "npm run gradlew -- -debug -huangyxHUTAO"]
	},
	shellBuildReleaseHotfix: {
		usage: "shellBuildReleaseHotfix [渠道参数] [风味参数]",
		note: "生成热更新包 core.js 与签名。",
		args: CHANNEL_ARGS.concat(FLAVOR_ARGS)
	},
	caaccel: {
		usage: "caaccel [build|test|bench]",
		npmUsage: "npm run caaccel -- [build|test|bench]",
		note: "构建 / 测试 / 基准加速库，默认 build。",
		subcommands: [
			["build", "构建 build/caaccel/caaccel.jar"],
			["test", "跑 caaccel/test 下的单测"],
			["bench", "matchString 的 Java vs Rhino 基准"]
		],
		examples: [
			"npm run caaccel",
			"npm run caaccel -- test",
			"npm run caaccel -- bench"
		]
	},
	buildCLib: {
		usage: "buildCLib",
		note: "构建内置命令库 caclib。"
	},
	publishRelease: {
		usage: "publishRelease",
		npmUsage: "npm run publishRelease",
		note: "发布正式版到配置的发布端。"
	},
	publishSnapshot: {
		usage: "publishSnapshot",
		npmUsage: "npm run publishSnapshot",
		note: "发布快照版到配置的发布端。"
	},
	publishHotfix: {
		usage: "publishHotfix",
		npmUsage: "npm run publishHotfix",
		note: "发布热更新包到配置的发布端。"
	},
	publishCLib: {
		usage: "publishCLib",
		npmUsage: "npm run publishCLib",
		note: "发布内置命令库到配置的发布端。"
	},
	generateImport: {
		usage: "generateImport",
		note: "从 android.jar 生成 import 用的类索引。"
	},
	clean: {
		usage: "clean",
		note: "清理 build/ 目录。"
	},
	delay: {
		usage: "delay <毫秒>",
		note: "延时若干毫秒，调试流水线用。"
	},
	shellPrepareDebug: {
		usage: "shellPrepareDebug",
		note: "准备调试版壳子资源。"
	}
};

async function listCliTasks() {
	const found = [];
	function walk(dir) {
		fs.readdirSync(dir).forEach(function (f) {
			const p = path.join(dir, f);
			if (fs.statSync(p).isDirectory()) {
				walk(p);
			} else if (f.endsWith(".mjs") && f !== "index.mjs" && f !== "help.mjs") {
				found.push({ name: path.basename(f, ".mjs"), path: p });
			}
		});
	}
	walk(__dirname);
	const names = [];
	for (const item of found) {
		try {
			const mod = await import(pathToFileURL(item.path).href);
			const task = mod.default;
			if (task && (mod.input === "cli" || task.input === "cli")) names.push(item.name);
		} catch (e) {
			// 无法导入的任务不出现在帮助里
		}
	}
	return names.sort();
}

/** 按终端显示宽度补齐：中文/全角算 2 列，否则列会错开 */
function pad(text, width) {
	var w = 0, i, c;
	for (i = 0; i < text.length; i++) {
		c = text.charCodeAt(i);
		w += (c > 0x2e80 && c < 0xfe10) || (c >= 0xff00 && c <= 0xff60) ? 2 : 1;
	}
	while (w < width) {
		text += " ";
		w++;
	}
	return text;
}

function printArgRows(lines, rows, width) {
	rows.forEach(function (row) {
		lines.push("  " + pad(row[0], width) + row[1]);
	});
}

function concise(lines, cliTasks) {
	const described = cliTasks.filter(function (name) { return TASK_DESC[name]; });
	const others = cliTasks.filter(function (name) { return !TASK_DESC[name]; });

	lines.push("");
	lines.push("命令助手构建工具");
	lines.push("");
	lines.push("用法");
	lines.push("  node ./cabuildtools/main.mjs <任务> [参数...]");
	lines.push("  npm run <脚本>");
	lines.push("  node ./cabuildtools/main.mjs help <任务>    查看任务详细参数");
	lines.push("  npm run help -- <任务>");
	lines.push("");
	lines.push("常用任务");
	described.forEach(function (name) {
		lines.push("  " + pad(name, 26) + TASK_DESC[name]);
	});
	if (others.length) {
		lines.push("");
		lines.push("其他 CLI 任务");
		others.forEach(function (name) { lines.push("  " + name); });
	}
	lines.push("");
	lines.push("常用入口");
	lines.push("  " + pad("npm run build", 26) + "正式版脚本 + 安装器 .lib");
	lines.push("  " + pad("npm run snapshot", 26) + "快照版脚本 + 安装器 .lib");
	lines.push("  " + pad("npm run help", 26) + "本帮助");
	lines.push("  " + pad("npm run caaccel -- test", 26) + "caaccel 单测");
	lines.push("");
	lines.push("提示");
	lines.push("  渠道 / 风味 / 产物等参数：help buildRelease");
	lines.push("  caaccel 子命令：help caaccel");
	lines.push("");
}

function detail(lines, name, subcommand, cliTasks) {
	const info = TASK_DETAIL[name];
	if (!info) {
		if (TASK_DESC[name] || cliTasks.includes(name)) {
			lines.push("");
			lines.push("任务: " + name);
			lines.push("");
			lines.push("  " + (TASK_DESC[name] || "CLI 任务"));
			lines.push("");
			lines.push("用法");
			lines.push("  node ./cabuildtools/main.mjs " + name);
			lines.push("");
			return;
		}
		lines.push("");
		lines.push("未知任务：" + name);
		lines.push("");
		lines.push("可用任务：");
		cliTasks.forEach(function (task) { lines.push("  " + task); });
		lines.push("");
		return;
	}

	lines.push("");
	lines.push("任务: " + name);
	lines.push("");
	lines.push("  " + info.note);
	lines.push("");

	if (info.subcommands && subcommand) {
		const found = info.subcommands.find(function (row) { return row[0] === subcommand; });
		if (!found) {
			lines.push("未知子命令：" + subcommand);
			lines.push("");
			lines.push("可用子命令：");
			info.subcommands.forEach(function (row) { lines.push("  " + row[0]); });
			lines.push("");
			return;
		}
		lines.push("子命令: " + subcommand);
		lines.push("");
		lines.push("  " + found[1]);
		lines.push("");
		lines.push("用法");
		lines.push("  npm run " + name + " -- " + subcommand);
		lines.push("  node ./cabuildtools/main.mjs " + name + " " + subcommand);
		lines.push("");
		lines.push("更多帮助：help " + name);
		lines.push("");
		return;
	}

	lines.push("用法");
	lines.push("  node ./cabuildtools/main.mjs " + info.usage);
	if (info.npmUsage) lines.push("  " + info.npmUsage);
	lines.push("");

	if (info.args && info.args.length) {
		lines.push("参数");
		printArgRows(lines, info.args, 20);
		lines.push("");
	}

	if (info.subcommands && info.subcommands.length) {
		lines.push("子命令");
		printArgRows(lines, info.subcommands, 14);
		lines.push("");
		if (subcommand) {
			const found = info.subcommands.find(function (row) { return row[0] === subcommand; });
			if (!found) {
				lines.push("未知子命令：" + subcommand);
				lines.push("");
			}
		}
	}

	if (info.examples && info.examples.length) {
		lines.push("示例");
		info.examples.forEach(function (example) { lines.push("  " + example); });
		lines.push("");
	}

	if (info.outputs && info.outputs.length) {
		lines.push("产物");
		printArgRows(lines, info.outputs, 20);
		lines.push("");
	}

	lines.push("更多帮助：help <任务>，例如 help caaccel test");
	lines.push("");
}

export default async function (context, args) {
	const cliTasks = await listCliTasks();
	const lines = [];
	const task = args && args[0];
	const subcommand = args && args[1];
	if (task) detail(lines, task, subcommand, cliTasks);
	else concise(lines, cliTasks);
	console.log(lines.join("\n"));
	return lines;
};

export const input = "cli";
