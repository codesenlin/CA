const path = require("path");
const { spawnSync } = require("child_process");

/**
 * 构建 CA 根目录 Kotlin/JS 工程的 production bundle，并生成 build/outputs/kotlin-js/Internal.kotlin.js。
 *
 * assembleSource / assembleSourceRelease / assembleSourceSnapshot 会先调用它，
 * 保证 npm run build 等入口不会使用过期的 Kotlin 产物。
 */
module.exports = function () {
	const script = path.resolve(__dirname, "../../../scripts/build-kotlin-js.js");
	const result = spawnSync(process.execPath, [script], {
		cwd: path.resolve(__dirname, "../../.."),
		stdio: "inherit",
		env: process.env
	});
	if (result.error) throw result.error;
	if (result.status !== 0) throw new Error("Kotlin/JS build failed");
}
