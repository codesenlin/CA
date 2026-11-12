import flavorOf from "../../lib/flavor.mjs";
export default function (context, args) {
	return context.execute("getBuildConfig", ["debug", "stable", flavorOf(args)])
		.then(context.task("getShellConfig"))
		.then(context.task("shellUpdateGradle"))
		.then(context.task("assembleSource"))
		.then(context.pipe("shellEncryptScriptDebug"))
		.then(context.pipe("shellWriteScript"));
}
export const input = "cli";