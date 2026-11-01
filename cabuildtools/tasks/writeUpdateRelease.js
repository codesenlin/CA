const fs = require("fs");
module.exports = function (context, args) {
	// 风味构建不写更新清单，避免覆盖正式版/快照版的更新源
	if (context.buildConfig.flavor && context.buildConfig.flavor != "default") return;
	fs.writeFileSync("./build/outputs/release/update.json", JSON.stringify({
		"time": context.buildConfig.publishTime,
		"version": context.buildConfig.date,
		"belongs": context.buildConfig.version,
		"info": context.buildConfig.description,
		"downloads": context.updateConfig.downloadSource
	}));
}