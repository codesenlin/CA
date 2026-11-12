import fs from "node:fs";
export default function (context, args) {
	fs.writeFileSync("./build/outputs/release/main.js", args[0]);
	var channel = context.buildConfig.channel,
		flavor = context.buildConfig.flavor,
		suffix = " " + channel + (flavor && flavor != "default" ? "-" + flavor : "");
	fs.copyFileSync("./build/outputs/release/main.js", "./build/dist/命令助手(" + context.buildConfig.date + ")" + suffix + ".js");
	return args[0];
}