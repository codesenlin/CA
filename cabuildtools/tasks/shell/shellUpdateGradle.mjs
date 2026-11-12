import fs from "node:fs";
import path from "node:path";
export default function (context, args) {
	var root = path.resolve(context.shellConfig.shellPath, "./app"),
		gradle = path.resolve(root, "build.gradle.kts"),
		gradleConfig = context.gradleConfig = {};
	if (!fs.existsSync(gradle)) gradle = path.resolve(root, "build.gradle");
	var isKts = /\.kts$/.test(gradle);
	var s = fs.readFileSync(gradle, "utf-8");
	gradleConfig.versionCode = Math.floor(Date.parse(context.buildConfig.date) / 86400000);
	gradleConfig.versionName = context.buildConfig.version;
	gradleConfig.minSdkVersion = parseInt((s.match(/minSdk(?:Version)?\s*=?\s*(\d+)/) || [undefined, 1])[1]);
	gradleConfig.shellVersion = parseInt((s.match(/SHELL_VERSION[^\d]*(\d+)/) || [undefined, 0])[1]);
	if (isKts) {
		s = s.replace(/versionCode\s*=\s*\d+/, "versionCode = " + gradleConfig.versionCode);
		s = s.replace(/versionName\s*=\s*".*"/, "versionName = " + JSON.stringify(gradleConfig.versionName));
	} else {
		s = s.replace(/versionCode\s+\d+/, "versionCode " + gradleConfig.versionCode);
		s = s.replace(/versionName\s+".*"/, "versionName " + JSON.stringify(gradleConfig.versionName));
	}
	fs.writeFileSync(gradle, s);
}
