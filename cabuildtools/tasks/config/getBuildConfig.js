const fs = require("fs");
const path = require("path");

/**
 * args 可以是：
 *   "release" / "snapshot" / 渠道名 —— 老用法，渠道名由 variants 推出来
 *   [variants, channel]              —— 分别指定构建形态（决定核心行为）与对外渠道名
 */
module.exports = function (context, args) {
	var variants, channel, flavor = "default";
	if (Array.isArray(args)) {
		variants = args[0];
		channel = args[1];
		if (args[2]) flavor = String(args[2]);
	} else {
		variants = args;
	}
	if (!channel) channel = variants == "release" || variants == "snapshot" ? "stable" : variants;
	var config = Object.assign({}, require(path.resolve("./config/build.json")));
	// 风味定义在 data/flavors/<名字>/：flavor.json 是元数据，data/*.json 是风味专属数据。
	// data/flavors/default 是基线：所有风味都会先带上 default 的数据，再用自己的数据覆盖/合并。
	var flavorRoot = path.resolve("./data/flavors");
	function isPlainObject(value) {
		return value !== null && typeof value === "object" && !Array.isArray(value);
	}
	function mergeData(base, over) {
		if (!isPlainObject(base) || !isPlainObject(over)) return over === undefined ? base : over;
		var out = Object.assign({}, base);
		Object.keys(over).forEach(function (key) {
			out[key] = isPlainObject(base[key]) && isPlainObject(over[key]) ? mergeData(base[key], over[key]) : over[key];
		});
		return out;
	}
	function readFlavor(name) {
		var dir = path.join(flavorRoot, name);
		var metaPath = path.join(dir, "flavor.json");
		if (!fs.existsSync(metaPath)) return null;
		var meta = JSON.parse(fs.readFileSync(metaPath, "utf-8"));
		var data = {};
		var dataDir = path.join(dir, "data");
		if (fs.existsSync(dataDir)) {
			fs.readdirSync(dataDir).filter(function (file) {
				return /\.json$/i.test(file);
			}).sort().forEach(function (file) {
				data[path.basename(file, path.extname(file))] = JSON.parse(fs.readFileSync(path.join(dataDir, file), "utf-8"));
			});
		}
		return { meta: meta, data: data };
	}
	var flavors = fs.existsSync(flavorRoot) ? fs.readdirSync(flavorRoot).filter(function (name) {
		return fs.existsSync(path.join(flavorRoot, name, "flavor.json"));
	}).sort() : [];
	var baseFlavor = readFlavor("default");
	if (!baseFlavor) throw new Error("缺少默认风味 data/flavors/default/flavor.json");
	if (flavors.indexOf(flavor) < 0) {
		throw new Error("未知的风味构建：" + flavor + "（可用：" + flavors.join(" / ") + "）");
	}
	var selected = flavor == "default" ? baseFlavor : readFlavor(flavor);
	var flavorData = mergeData(baseFlavor.meta, selected.meta);
	flavorData.data = {};
	Object.keys(baseFlavor.data).forEach(function (key) {
		flavorData.data[key] = baseFlavor.data[key];
	});
	Object.keys(selected.data).forEach(function (key) {
		flavorData.data[key] = mergeData(baseFlavor.data[key] || {}, selected.data[key]);
	});
	config.flavor = flavor;
	config.flavorData = flavorData;
	config.variants = variants;
	config.channel = channel;
	config.publishTime = Date.now();
	context.buildConfig = config;
}
