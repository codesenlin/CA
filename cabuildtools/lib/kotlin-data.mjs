import fs from "node:fs";
import { createRequire } from "node:module";
import path from "node:path";
const require = createRequire(import.meta.url);

/**
 * 在 assemble 阶段把构建配置写成 JSON，供 Kotlin bundle 内的
 * Loader.fromFile("../../build/kotlin-data/xxx.json") 在 AST 拼装时内联。
 */
export default function (context) {
	const dir = path.resolve("build/kotlin-data");
	fs.mkdirSync(dir, { recursive: true });
	if (context.buildConfig) {
		fs.writeFileSync(path.join(dir, "BuildConfig.json"), JSON.stringify(context.buildConfig));
	}
	fs.writeFileSync(
		path.join(dir, "UpdateSources.json"),
		JSON.stringify(require("../../config/update-sources.json"))
	);
};
