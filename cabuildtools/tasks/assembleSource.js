const fs = require("fs");
const loader = require("../loader");
const builtinlibs = require("../builtinlibs");
module.exports = function(context, args) {
	var result = loader.load("./main.js", { buildConfig : context.buildConfig });
	result = builtinlibs.inject(result, builtinlibs.collect());
	return result;
}