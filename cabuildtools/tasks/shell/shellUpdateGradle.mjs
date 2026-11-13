import fs from "node:fs";
import path from "node:path";
export default function (context, args) {
	var root = path.resolve(context.shellConfig.shellPath, "./app"),
		gradle = path.resolve(root, "build.gradle.kts"),
		versionFile = path.resolve(root, "version.json"),
		gradleConfig = context.gradleConfig = {};
	if (!fs.existsSync(gradle)) gradle = path.resolve(root, "build.gradle");
	var isKts = /\.kts$/.test(gradle);
	var s = fs.readFileSync(gradle, "utf-8");
	gradleConfig.versionCode = Math.floor(Date.parse(context.buildConfig.date) / 86400000);
	gradleConfig.versionName = context.buildConfig.version;
	gradleConfig.minSdkVersion = parseInt((s.match(/minSdk(?:Version)?\s*=?\s*(\d+)/) || [undefined, 1])[1]);
	gradleConfig.shellVersion = parseInt((s.match(/SHELL_VERSION[^\d]*(\d+)/) || [undefined, 0])[1]);
	// 版本号只写 app/version.json；app/build.gradle.kts 从该文件读取。
	fs.writeFileSync(versionFile, JSON.stringify({
		version: gradleConfig.versionName,
		versionCode: gradleConfig.versionCode
	}, null, "\t") + "\n");
	fs.writeFileSync(gradle, s);
}
