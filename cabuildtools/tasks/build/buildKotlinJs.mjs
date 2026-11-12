import path from "node:path";
import { fileURLToPath } from "node:url";
import { spawnSync } from "node:child_process";
const __dirname = path.dirname(fileURLToPath(import.meta.url));

/**
 * 构建 CA 根目录 Kotlin/JS 工程的 production bundle，并生成 build/outputs/kotlin-js/Internal.kotlin.js。
 *
 * assembleSource / assembleSourceRelease / assembleSourceSnapshot 会先调用它，
 * 保证 npm run build 等入口不会使用过期的 Kotlin 产物。
 */
export default function () {
	const script = path.resolve(__dirname, "../../../scripts/build-kotlin-js.mjs");
	const result = spawnSync(process.execPath, [script], {
		cwd: path.resolve(__dirname, "../../.."),
		stdio: "inherit",
		env: process.env
	});
	if (result.error) throw result.error;
	if (result.status !== 0) throw new Error("Kotlin/JS build failed");
}
