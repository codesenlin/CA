import configTemplate from "../../lib/config-template.mjs";

/**
 * 显式检查 config/ 并补全缺失文件。
 * 构建工具启动时也会自动执行一次；这个任务方便单独排查。
 */
export default function () {
	var result = configTemplate();
	if (result.created.length) {
		console.log("[config] 本次补全：" + result.created.join(", "));
	}
	if (result.missingTemplates.length) {
		console.log("[config] 缺少模板：" + result.missingTemplates.join(", "));
	}
	if (!result.created.length && !result.missingTemplates.length) {
		console.log("[config] 配置完整");
	}
}
export const input = "cli";
