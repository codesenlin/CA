const fs = require("fs");
const loader = require("../loader");
const builtinlibs = require("../builtinlibs");

module.exports = function(context, args) {
	var libs = builtinlibs.collect();
	var result = loader.load("./main.js", {
		buildConfig : context.buildConfig,
		builtinLibs : libs
	});
	result = builtinlibs.inject(result, libs);
	fs.writeFileSync("./build/outputs/releaseSource/main.js", result);
	return result;
}
