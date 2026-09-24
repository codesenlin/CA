const fs = require("fs");
const path = require("path");
const loader = require("../loader");

module.exports = function(context, args) {
	var builtinLibs = [];
	try {
		var caclibRoot = path.resolve(__dirname, "../../caclib");
		var indexPath = path.join(caclibRoot, "index.json");
		if (fs.existsSync(indexPath)) {
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
		}
	} catch(e) {
		console.warn("[caclib] failed to collect builtin libraries: " + e.message);
	}

	var result = loader.load("./main.js", {
		buildConfig : context.buildConfig,
		builtinLibs : builtinLibs
	});
	result = result.replace('"__BUILTIN_LIBS__"', JSON.stringify(builtinLibs));
	fs.writeFileSync("./build/outputs/releaseSource/main.js", result);
	return result;
}
