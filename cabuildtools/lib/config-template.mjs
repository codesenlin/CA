/**
 * CA config/ 的模板与自动补全。
 *
 * - 模板放在 config/templates/<name>.json
 * - 构建工具启动时检查：模板缺失就警告；实际配置缺失就从模板复制
 * - 已存在的配置不覆盖，只做 JSON 解析和必要字段检查
 *
 * 目标文件仍然写回 CA/config/，不生成第二份运行配置。
 */
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const DEFAULT_ROOT = path.resolve(__dirname, "../..");
const CONFIG_FILES = ["build", "import", "publish", "shell", "update-sources"];

/** 每个配置必须存在的顶层字段，缺失只警告，不阻塞构建 */
const REQUIRED_KEYS = {
	build: ["version", "versionCode", "date"],
	import: [],
	publish: ["method"],
	shell: [],
	"update-sources": ["release", "beta"]
};

function warn(message) {
	console.warn("[config] " + message);
}

function validate(name, filePath) {
	var data;
	try {
		data = JSON.parse(fs.readFileSync(filePath, "utf-8"));
	} catch (e) {
		throw new Error("解析 " + filePath + " 失败：" + e.message);
	}
	if (data === null || typeof data !== "object" || Array.isArray(data)) {
		throw new Error(filePath + " 必须是 JSON 对象");
	}
	(REQUIRED_KEYS[name] || []).forEach(function (key) {
		if (!(key in data)) warn("config/" + name + ".json 缺少字段 " + key);
	});
	return data;
}

/**
 * 检查并补全 config/ 下的 JSON。
 * @param {{root?: string, quiet?: boolean}} [options]
 * @returns {{created: string[], checked: string[], missingTemplates: string[]}}
 */
export function ensureConfigs(options) {
	options = options || {};
	var root = options.root || DEFAULT_ROOT;
	var configDir = path.join(root, "config");
	var templateDir = path.join(configDir, "templates");
	var created = [];
	var checked = [];
	var missingTemplates = [];

	fs.mkdirSync(configDir, { recursive: true });
	CONFIG_FILES.forEach(function (name) {
		var template = path.join(templateDir, name + ".json");
		var target = path.join(configDir, name + ".json");
		if (!fs.existsSync(template)) {
			missingTemplates.push(name);
			warn("缺少模板 config/templates/" + name + ".json，跳过 " + name + ".json 的自动补全");
			// 模板都没有时，至少检查一下已有配置
			if (fs.existsSync(target)) validate(name, target);
			return;
		}
		if (!fs.existsSync(target)) {
			fs.mkdirSync(path.dirname(target), { recursive: true });
			fs.copyFileSync(template, target);
			created.push(name);
			if (!options.quiet) console.log("[config] 已从模板创建 config/" + name + ".json");
		}
		validate(name, target);
		checked.push(name);
	});
	return { created: created, checked: checked, missingTemplates: missingTemplates };
}

export default ensureConfigs;
