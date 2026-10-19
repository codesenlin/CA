const fs = require("fs");
const loader = require("../loader");
const builtinlibs = require("../builtinlibs");
module.exports = function(context, args) {
	var result = loader.load("./main.js", {
		buildConfig : context.buildConfig,
		// 更新源配置（config/update-sources.js），注入成核心里的 UpdateSources
		updateSources : require("../../config/update-sources.js") });
	result = builtinlibs.inject(result, builtinlibs.collect());
	return result;
}