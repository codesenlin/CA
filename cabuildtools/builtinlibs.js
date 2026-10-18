const fs = require("fs");
const path = require("path");

/**
 * 收集 caclib/index.json 里列出的内置命令库，返回 [{path, source}]（source 为 base64）。
 *
 * 结果需要注入到脚本里 CA.Library.builtinLibs 的占位符 "__BUILTIN_LIBS__"。
 * 任何会产出"可运行的脚本"的构建路径（release / debug / snapshot）都必须替换该占位符，
 * 否则核心拿到的是字符串，CA.Library.loadBuiltinLibraries() 里的 libs.forEach 会抛 TypeError。
 */
/**
 * 找出一个命令库的入口文件：
 *  1. 元数据（library.js 的 source 字段）里声明的入口，如 main.js / main.json
 *  2. 约定俗成的 main.js / main.json
 *  3. 兜底：目录里第一个 .js/.json（排除 library.* 元数据本身）
 */
function findEntry(libDir) {
	var metaPath = path.join(libDir, "library.js");
	if (fs.existsSync(metaPath)) {
		try {
			var meta = require(metaPath);
			if (meta && meta.source && fs.existsSync(path.join(libDir, String(meta.source)))) {
				return path.join(libDir, String(meta.source));
			}
		} catch(e) {
			// 元数据坏了就继续往下找
		}
	}
	var preferred = ["main.js", "main.json"], i;
	for (i = 0; i < preferred.length; i++) {
		if (fs.existsSync(path.join(libDir, preferred[i]))) return path.join(libDir, preferred[i]);
	}
	var files = fs.readdirSync(libDir).filter(function(f) {
		return /\.(js|json)$/.test(f) && !/^library\./.test(f);
	});
	return files.length ? path.join(libDir, files[0]) : null;
}

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
			var entry = findEntry(libDir);
			if (!entry) {
				console.warn("[caclib] no entry in: " + libDir);
				return;
			}
			var code = fs.readFileSync(entry, "utf-8");
			// 内嵌的源码必须是能 eval 的（它会以 eval("(" + code + ")") 的形式加载）
			try {
				new Function("return (" + code + ")");
			} catch(e) {
				throw new Error(path.relative(caclibRoot, entry) + " 不是合法的命令库代码：" + e.message);
			}
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
