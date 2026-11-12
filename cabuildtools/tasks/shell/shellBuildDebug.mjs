import flavorOf from "../../lib/flavor.mjs";
export default function (context, args) {
	return context.execute("getBuildConfig", ["debug", "stable", flavorOf(args)])
		.then(context.task("getShellConfig"))
		.then(context.task("initDirectory", {
			"./build/": {
				"outputs/": [
					"debugApk"
				],
				"dist": ""
			}
		}))
		.then(context.task("shellUpdateGradle"))
		.then(context.task("assembleSource"))
		.then(context.pipe("shellEncryptScriptDebug"))
		.then(context.pipe("shellWriteScript"))
		.then(context.task("shellGradle", ":app:assembleDebug"))
		.then(context.task("shellExportDebugApk"));
}
export const input = "cli";
