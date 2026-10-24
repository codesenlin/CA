const fs = require("fs");
const loader = require("../loader-ast");
const builtinlibs = require("../builtinlibs");
const postprocess = require("../postprocess");

module.exports = function(context, args) {
	var result = loader.load("./main.js", {
		buildConfig : context.buildConfig,
		// 更新源配置（config/update-sources.js），注入成核心里的 UpdateSources
		updateSources : require("../../config/update-sources.js")
	}, "utf-8", { transformAsync: true });
	result = postprocess(result, context.buildConfig);
	result = builtinlibs.inject(result, builtinlibs.collect());
	fs.writeFileSync("./build/outputs/snapshotSource/main.js", result);
	return result;
}
