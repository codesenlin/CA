const fs = require("fs");
const path = require("path");
const child_process = require("child_process");
module.exports = function(context, args) {
	var jarsignerPath = path.resolve(context.shellConfig.shellPath, context.shellConfig.buildToolPath, "apksigner");
	var keystonePath = path.resolve(context.shellConfig.shellPath, context.shellConfig.keystorePath);
	var unsignedPath = path.resolve(context.shellConfig.shellPath, "./app/build/outputs/apk/release/app-release.apk");
	var keyPassPath = path.resolve(context.shellConfig.shellPath, context.shellConfig.keyPasswordPath);
	return context.execute("execProcess", {
		command : jarsignerPath,
		args : [
			"sign",
			"-ks", keystonePath,
			unsignedPath
		],
		cwd : context.shellConfig.shellPath,
		stdio : ["pipe", "inherit", "inherit"],
		input : fs.readFileSync(keyPassPath),
		shell : true
	});
}