import fs from "node:fs";
import loader from "../../lib/loader-ast.mjs";
import postprocess from "../../lib/postprocess.mjs";
import buildKotlinJs from "./buildKotlinJs.mjs";
import kotlinData from "../../lib/kotlin-data.mjs";
import { createRequire } from "node:module";
const require = createRequire(import.meta.url);

export default function (context, args) {
	buildKotlinJs();
	kotlinData(context);
	var result = loader.load("./modules/main.cjs", {
		buildConfig: context.buildConfig,
		// 更新源配置（config/update-sources.json），注入成核心里的 UpdateSources
		updateSources: require("../../../config/update-sources.json")
	}, "utf-8", { transformAsync: true });
	// AST 加载器是逐文件处理的，压缩/日期替换要在拼装结果上统一做一次
	result = postprocess(result, context.buildConfig);
	// caclib 是在线源工程，不参与打包：占位符替换成空数组
	result = result.replace('"__BUILTIN_LIBS__"', "[]");
	return result;
}
