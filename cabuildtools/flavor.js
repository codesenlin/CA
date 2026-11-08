/**
 * 风味构建参数解析（可选，默认 default）：
 *   -huangyxHUTAO / --huangyxHUTAO   南鸢晨星社区版风味（粉色主题 + 社区欢迎语）
 *   -flavor <名字> / --flavor        自定义风味（需在 data/flavors/<名字>/ 下定义）
 */
module.exports = function flavorOf(args) {
	var list = args || [], i;
	for (i = 0; i < list.length; i++) {
		if (/^--?huangyxhutao$/i.test(list[i])) return "huangyxHUTAO";
		if ((list[i] == "-flavor" || list[i] == "--flavor") && list[i + 1]) return String(list[i + 1]);
	}
	return "default";
};
