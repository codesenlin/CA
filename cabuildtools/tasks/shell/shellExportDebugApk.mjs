import fs from "node:fs";
import path from "node:path";
export default function (context, args) {
	var outPath = path.resolve(context.shellConfig.shellPath, "./app/build/outputs/apk/debug/app-debug.apk");
	var flavor = context.buildConfig.flavor,
		name = "app-debug" + (flavor && flavor != "default" ? "-" + flavor : "") + ".apk";
	fs.copyFileSync(outPath, "./build/outputs/debugApk/" + name);
	console.log("Debug APK exported to: ./build/outputs/debugApk/" + name);
}
export const input = "pipe";
