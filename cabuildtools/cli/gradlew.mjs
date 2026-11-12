/**
 * npm run gradlew 的入口：按参数选择 Release / debug 形态。
 * （放在 cabuildtools/ 下，和 main.mjs 同级）
 *
 *   npm run gradlew              # 正式版 APK（shellBuildRelease）
 *   npm run gradlew -- -debug    # 调试版 APK（shellBuildDebug）
 *
 * cadroid 的 Gradle 任务由 cabuildtools 的 shellGradle 内部调用，
 * 这个脚本只是把「形态 → 任务名」这层映射补上。
 */
import path from "node:path";
import process from "node:process";
import { fileURLToPath } from "node:url";
import { spawnSync } from "node:child_process";
const __dirname = path.dirname(fileURLToPath(import.meta.url));

const rawArgs = process.argv.slice(2);
const isDebug = rawArgs.some(function (arg) { return /^--?debug$/i.test(arg); });
const task = isDebug ? "shellBuildDebug" : "shellBuildRelease";
const taskArgs = rawArgs.filter(function (arg) { return !/^--?debug$/i.test(arg); });

console.log("[gradlew] " + (isDebug ? "调试版" : "正式版") + " → " + task);

const result = spawnSync(
	process.execPath,
	[path.join(__dirname, "../main.mjs"), task],
	{ stdio: "inherit" }
);

process.exit(result.status === null ? 1 : result.status);
