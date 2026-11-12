import fs from "node:fs";
import path from "node:path";
export default function (context, args) {
	var outPath = path.resolve(context.shellConfig.shellPath, "./app/build/outputs/apk/release/app-release.apk");
	fs.copyFileSync(outPath, "./build/outputs/releaseApk/app-release.apk");
	var flavor = context.buildConfig.flavor,
		name = "命令助手(" + context.buildConfig.version + (flavor && flavor != "default" ? "-" + flavor : "") + ").apk";
	fs.copyFileSync("./build/outputs/releaseApk/app-release.apk", "./build/dist/" + name);
}