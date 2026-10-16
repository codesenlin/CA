const fs = require("fs");
const path = require("path");

/**
 * 收集 caclib/index.json 里列出的内置命令库，返回 [{path, source}]（source 为 base64）。
 *
 * 结果需要注入到脚本里 CA.Library.builtinLibs 的占位符 "__BUILTIN_LIBS__"。
 * 任何会产出"可运行的脚本"的构建路径（release / debug / snapshot）都必须替换该占位符，
 * 否则核心拿到的是字符串，CA.Library.loadBuiltinLibraries() 里的 libs.forEach 会抛 TypeError。
 */
function collect() {
	var builtinLibs = [];
	try {
		var caclibRoot = path.resolve(__dirname, "../caclib");
		var indexPath = path.join(caclibRoot, "index.json");
		if (!fs.existsSync(indexPath)) return builtinLibs;
		var index = JSON.parse(fs.readFileSync(indexPath, "utf-8"));
		index.forEach(function(item) {
			var libPath = String(item.path).replace(/^\.\//, "");
			var libDir = path.join(caclibRoot, libPath);
			if (!fs.existsSync(libDir)) {
				console.warn("[caclib] missing dir: " + libDir);
				return;
			}
			// 扫描目录里的 .js / .json 文件，优先 main.js
			var files = fs.readdirSync(libDir).filter(function(f) {
				return /\.(js|json)$/.test(f);
			});
			var entryName = files.indexOf("main.js") >= 0 ? "main.js" : files[0];
			if (!entryName) {
				console.warn("[caclib] no entry in: " + libDir);
				return;
			}
			var entry = path.join(libDir, entryName);
			var code = fs.readFileSync(entry, "utf-8");
			builtinLibs.push({
				path : item.path,
				source : Buffer.from(code, "utf-8").toString("base64")
			});
		});
		console.log("[caclib] embedded " + builtinLibs.length + " builtin libraries");
	} catch(e) {
		console.warn("[caclib] failed to collect builtin libraries: " + e.message);
	}
	return builtinLibs;
}

/** 把内置命令库注入到脚本源码里（替换占位符） */
function inject(source, builtinLibs) {
	return source.replace('"__BUILTIN_LIBS__"', JSON.stringify(builtinLibs));
}

module.exports = collect;
module.exports.collect = collect;
module.exports.inject = inject;
