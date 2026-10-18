const path = require("path");

module.exports = function(context, args) {
	// 配置见 config/build.js（不要直接改模块缓存，拷一份再附加运行时字段）
	context.buildConfig = Object.assign({}, require(path.resolve("./config/build.js")));
	context.buildConfig.variants = args;
	context.buildConfig.publishTime = Date.now();
}
