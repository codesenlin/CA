const fs = require("fs");
const loader = require("../loader");
const builtinlibs = require("../builtinlibs");

module.exports = function(context, args) {
	var libs = builtinlibs.collect();
	var result = loader.load("./main.js", {
		buildConfig : context.buildConfig,
		// 更新源配置（config/update-sources.js），注入成核心里的 UpdateSources
		updateSources : require("../../config/update-sources.js"),
		builtinLibs : libs
	});
	result = builtinlibs.inject(result, libs);
	fs.writeFileSync("./build/outputs/releaseSource/main.js", result);
	return result;
}
