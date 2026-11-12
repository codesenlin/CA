import flavorOf from "../../lib/flavor.mjs";
import channelOf from "../../lib/channel.mjs";

export default function (context, args) {
	// 带 -beta 时 variants = "beta"：产物叫 beta.js / beta.lib，lib 的 uuid 与名字也不同
	var channel = channelOf(args);
	// 不带渠道参数就是 stable（正式版）；带参数时渠道名同时作为 variants
	// 渠道默认也用 snapshot：安装器按渠道判断能否覆盖，若这里给 stable，
	// 装过一次快照后（App 的 CA.variants 变成 snapshot）再装同一天的构建会被
	// rank 比较挡住 —— 提示“已装更高渠道构建”，实际什么都没装。
	return context.execute("getBuildConfig", [channel || "snapshot", channel || "snapshot", flavorOf(args)])
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
		.then(function (r) {
			return context.buildConfig.variants == "snapshot" ? context.execute("writeUpdateSnapshot") : r;
		});
}
export const input = "cli";
