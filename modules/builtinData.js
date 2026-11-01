/*LOADER
if (variables.buildConfig.variants == "release") {
	postprocessor = function(src) {
		var jsmin = require("jsmin").jsmin;
		return jsmin(src, 2);
	};
}
*/
// 内置拓展包：内容用 Loader.fromFile 在构建时内联，默认启用/禁用由 data/builtinLibs.json 清单决定。
// 新增内置包：①把 .json 放到 data/idlibs/ ②在这里加一行 Loader.fromFile
// ③在 data/builtinLibs.json 的 libraries 里登记（default: enabled/disabled）④更新 docs/data-notes/builtinLibs.md
CA.Library.inner["default"] = Loader.fromFile("../data/idlibs/default.json");
CA.Library.inner["addition"] = Loader.fromFile("../data/idlibs/addition.json");
CA.Library.inner["education"] = Loader.fromFile("../data/idlibs/education.json");
CA.Library.inner["experiment"] = Loader.fromFile("../data/idlibs/experiment.json");
CA.Library.inner["basicedu"] = Loader.fromFile("../data/idlibs/basicedu.json");
// JSON Schema 包：给 jsonPro 参数提供补全/校验用的 schema
CA.Library.inner["JSONSchema"] = Loader.fromFile("../data/idlibs/JSONSchema.json");
CA.Library.inner["states"] = Loader.fromFile("../data/idlibs/states.json");

(function() {
	// 清单在 data/builtinLibs.json；未登记的库为避免意外启用，按默认禁用处理
	var manifest = Loader.fromFile("../data/builtinLibs.json");
	var recommended = [], defaultDisabled = [];
	manifest.libraries.forEach(function(lib) {
		if (!CA.Library.inner[lib.id]) return;
		if (lib["default"] == "disabled") defaultDisabled.push(lib.id);
		else recommended.push(lib.id);
	});
	Object.keys(CA.Library.inner).forEach(function(id) {
		if (recommended.indexOf(id) < 0 && defaultDisabled.indexOf(id) < 0) {
			defaultDisabled.push(id);
		}
	});
	CA.Library.recommended = recommended;
	CA.Library.defaultDisabled = defaultDisabled;
})();

// 主题数据来自风味：flavors/default/data/theme.json 是基线，其它风味的数据在
// 构建时合并进来（getBuildConfig），最终注入 BuildConfig.flavorData.data.theme
Common.themelist = (function(list) {
	Object.keys(list).forEach(function(k) {
		if (list[k].nameKey) {
			list[k].name = Intl.get(list[k].nameKey);
			delete list[k].nameKey;
		}
	});
	return list;
})((BuildConfig.flavorData && BuildConfig.flavorData.data && BuildConfig.flavorData.data.theme) || {});

CA.tips = CA.defalutTips = Loader.fromFile("../data/tips.json");