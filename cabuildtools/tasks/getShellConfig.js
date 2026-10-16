const fs = require("fs");
const readConfig = require("../readconfig");
const localconfig = require("../localconfig");

module.exports = function(context, args) {
	var localPath = localconfig.pathOf("./config/shell.txt");
	context.shellConfig = readConfig(localconfig.load("./config/shell.txt"));
	// shellPath 是所有 shell 任务的基础，缺了它后面会报一堆看不懂的错，这里直接说清楚
	if (!context.shellConfig.shellPath || !fs.existsSync(context.shellConfig.shellPath)) {
		throw new Error("请先填写 " + localPath + " 里的 shellPath（cadroid 项目路径），当前值：" + context.shellConfig.shellPath);
	}
	if (!context.shellConfig.buildToolPath || !fs.existsSync(context.shellConfig.buildToolPath)) {
		console.log("提示：" + localPath + " 里的 buildToolPath 不存在（只有发布流程的 zipalign / apksigner 会用到）：" + context.shellConfig.buildToolPath);
	}
}
