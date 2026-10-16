const fs = require("fs");
const readConfig = require("../readconfig");
const localconfig = require("../localconfig");
module.exports = function(context, args) {
	var localPath = localconfig.pathOf("./config/import.txt");
	var importConfig = readConfig(localconfig.load("./config/import.txt")), jarClasses = [];
	if (!importConfig.androidJarPath || !fs.existsSync(importConfig.androidJarPath)) {
		throw new Error("请先填写 " + localPath + " 里的 androidJarPath，当前值：" + importConfig.androidJarPath);
	}
	return context.execute("getBuildConfig", "inspect")
		.then(context.task("readJarClasses", [jarClasses, {
			path : importConfig.androidJarPath
		}]))
		.then(context.task("assembleSource"))
		.then(context.pipe("processImportScope", {
			target : "./modules/uiCore/G.js",
			scopeName : "G",
			classes : jarClasses,
			packages : [
				"android",
				"android.widget",
				"android.view",
				"android.view.animation",
				"android.view.inputmethod",
				"android.animation",
				"android.app",
				"android.content",
				"android.graphics",
				"android.graphics.drawable",
				"android.media",
				"android.os",
				"android.text",
				"android.text.format",
				"android.text.method",
				"android.text.style",
				"android.webkit"
			]
		}));
}
module.exports.input = "cli";