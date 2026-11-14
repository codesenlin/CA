/**
 * caaccel（加速库）的构建 / 测试 / 基准脚本。
 *
 *   node ./cabuildtools/main.mjs caaccel build   # 构建 build/caaccel.jar
 *   node ./cabuildtools/main.mjs caaccel test    # 跑 caaccel/test 下的单测
 *   node ./cabuildtools/main.mjs caaccel bench   # matchString 的 Kotlin/JVM vs Rhino 基准
 *
 * 源码已改为 Kotlin/JVM，构建统一走 caaccel/build.gradle.kts；
 * 这个脚本只负责定位 CA 根目录并转调 Gradle wrapper。
 */
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { spawnSync } from "node:child_process";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname, "../..");
const BUILD = path.join(ROOT, "build");
const GRADLEW = path.join(ROOT, process.platform === "win32" ? "gradlew.bat" : "gradlew");

function fail(msg) {
	console.error("[caaccel] 失败：" + msg);
	process.exit(1);
}

function runGradle(task, extraArgs) {
	const args = [task, "--console=plain"].concat(extraArgs || []);
	console.log("[caaccel] $ gradlew " + args.join(" "));
	const result = spawnSync(GRADLEW, args, {
		cwd: ROOT,
		stdio: "inherit",
		env: Object.assign({}, process.env, {
			JAVA_HOME: process.env.JAVA_HOME || "/usr/lib/jvm/java-25-openjdk-amd64"
		})
	});
	if (result.error) fail(result.error.message);
	if (result.status !== 0) fail("gradlew 退出码 " + result.status);
}

/** 构建 caaccel.jar：Kotlin/JVM Gradle 模块，不依赖 Android */
function build(quiet) {
	runGradle(":caaccel:jar", quiet ? ["-q"] : []);
	const outJar = path.join(BUILD, "caaccel.jar");
	if (!fs.existsSync(outJar)) fail("没有生成 " + outJar);
	if (!quiet) console.log("[caaccel] built: " + outJar + " (" + fs.statSync(outJar).size + " 字节)");
	return { outJar };
}

/** 跑单测 */
function test() {
	runGradle(":caaccel:caaccelTest");
}

/** 跑基准 */
function bench() {
	runGradle(":caaccel:bench");
}

export default { build, test, bench };
