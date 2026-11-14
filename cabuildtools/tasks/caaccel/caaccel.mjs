import caaccel from "../../lib/caaccel.mjs";

/**
 * CA 加速库任务：
 *   node ./cabuildtools/main.mjs caaccel [build|test|bench]
 */
export default function (context, args) {
	const command = args[0] || "build";
	if (command === "build") return caaccel.build(false);
	if (command === "test") return caaccel.test();
	if (command === "bench") return caaccel.bench();
	if (command === "help" || command === "-h" || command === "--help") {
		console.log([
			"用法：",
			"  npm run caaccel",
			"  npm run caaccel -- <命令>",
			"  node ./cabuildtools/main.mjs caaccel <命令>",
			"",
			"  build   构建 build/caaccel.jar（Kotlin/JVM Gradle 模块）",
			"  test    跑 caaccel/test 下的单测",
			"  bench   matchString 的 Java vs Rhino 基准（需要 Rhino 1.9.2 jar）",
			"",
			"构建走 CA 根目录的 Gradle wrapper，需要 JDK。"
		].join("\n"));
		return;
	}
	throw new Error("未知命令：" + command);
};

export const input = "cli";
