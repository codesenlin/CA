/** 渠道参数：-beta / -dev / -canary（不带则是默认渠道） */
function channelOf(args) {
	var list = args || [], i;
	for (i = 0; i < list.length; i++) {
		if (list[i] == "-beta" || list[i] == "--beta") return "beta";
		if (list[i] == "-dev" || list[i] == "--dev") return "dev";
		if (list[i] == "-canary" || list[i] == "--canary") return "canary";
	}
	return null;
}

module.exports = function(context, args) {
	// 带 -beta 时 variants = "beta"：产物叫 beta.js / beta.lib，lib 的 uuid 与名字也不同
	return context.execute("getBuildConfig", channelOf(args) || "snapshot")
		.then(context.task("getUpdateConfig"))
		.then(context.task("initDirectory", {
			"./build/": {
				"outputs/": [
					"snapshotSource",
					"snapshot",
					"snapshotLib"
				],
				"dist": ""
			}
		}))
		.then(context.task("assembleSourceSnapshot"))
		.then(context.pipe("writeSnapshot"))
		.then(context.pipe("writeSnapshotLib"))
		.then(function(r) {
			return context.buildConfig.variants == "snapshot" ? context.execute("writeUpdateSnapshot") : r;
		});
}
module.exports.input = "cli";
