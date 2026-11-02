/**
 * 构建产物后处理。
 *
 * AST 加载器逐文件处理并重新生成代码，旧的逐文件 postprocessor 会被排版回去，
 * 且 {DATE} 等处理会失效。现在所有构建产物的压缩/日期替换统一在这里执行一次，
 * 不再依赖 main.js / G.js / builtinData.js / intl/index.js 里的旧 LOADER 块。
 */
const jsmin = require("jsmin").jsmin;

module.exports = function postprocess(source, buildConfig) {
	var variants = buildConfig.variants;
	if (variants == "release") {
		return jsmin(source, 1)
			.replace(/\{DATE\}/g, buildConfig.date)
			.replace(/^\s+/, "")
			.replace(/^"ui";\n/, "")
			.replace(/CA\.RELEASE/g, "true");
	}
	if (variants == "snapshot") {
		return source.replace(/\{DATE\}/g, "S" + buildConfig.date);
	}
	if (/^(beta|dev|canary)$/.test(variants)) {
		return jsmin(source, 1)
			.replace(/\{DATE\}/g, buildConfig.date)
			.replace(/^\s+/, "");
	}
	if (variants == "debug") {
		return source.replace(/\{DATE\}/g, buildConfig.date);
	}
	return source;
};
