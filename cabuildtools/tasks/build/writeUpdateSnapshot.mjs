import fs from "node:fs";
import crypto from "node:crypto";
function digestSHA1(data) {
	var digest = crypto.createHash("sha1");
	digest.update(data);
	return digest.digest("base64");
}
export default function (context, args) {
	// 风味构建不写更新清单，避免覆盖正式版/快照版的更新源
	if (context.buildConfig.flavor && context.buildConfig.flavor != "default") return;
	fs.writeFileSync("./build/outputs/snapshot/snapshot.json", JSON.stringify({
		"time": context.buildConfig.publishTime,
		"version": context.buildConfig.date,
		"belongs": context.buildConfig.version,
		"info": context.buildConfig.description,
		"snapshot": {
			"url": context.updateConfig.remoteRoot + "snapshot.js",
			"sha1": digestSHA1(fs.readFileSync("./build/outputs/snapshot/snapshot.js"))
		}
	}));
}