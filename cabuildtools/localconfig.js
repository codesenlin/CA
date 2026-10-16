const fs = require("fs");
const path = require("path");

/**
 * 读取本机配置。
 *
 * 仓库里跟踪的 config/xxx.txt 是**模板**（占位路径 + 注释），各人自己的配置放在同目录的
 * config/xxx.local.txt（已在 .gitignore 里，不入版本库）。本机文件不存在时从这里从模板
 * 生成一份，之后按注释填写即可。
 */
function localPathOf(templatePath) {
	var ext = path.extname(templatePath);
	return templatePath.slice(0, templatePath.length - ext.length) + ".local" + ext;
}

function load(templatePath) {
	var localPath = localPathOf(templatePath);
	if (!fs.existsSync(localPath)) {
		if (!fs.existsSync(templatePath)) {
			throw new Error("找不到配置模板：" + templatePath);
		}
		fs.writeFileSync(localPath, fs.readFileSync(templatePath));
		console.log("已从模板生成 " + localPath + "，请按文件内注释填写本机路径");
	}
	return fs.readFileSync(localPath, "utf-8");
}

module.exports = {
	pathOf : localPathOf,
	load : load
};
