const caaccel = require("../../lib/caaccel");

/**
 * CA 加速库任务：
 *   node ./cabuildtools/main.js caaccel [build|test|bench]
 */
module.exports = function (context, args) {
	const command = args[0] || "build";
	if (command === "build") return caaccel.build(false);
	if (command === "test") return caaccel.test();
	if (command === "bench") return caaccel.bench();
	if (command === "help" || command === "-h" || command === "--help") {
		console.log([
			"用法：node ./cabuildtools/main.js caaccel <命令>",
			"",
			"  build   构建 build/caaccel/caaccel.jar（纯 javac + jar）",
			"  test    跑 caaccel/test 下的单测",
			"  bench   matchString 的 Java vs Rhino 基准（需要 Rhino 1.9.2 jar）",
			"",
			"JDK 优先用 $JAVA_HOME/bin，没有就用 PATH 里的 javac/jar/java。"
		].join("\n"));
		return;
	}
	throw new Error("未知命令：" + command);
};

module.exports.input = "cli";
