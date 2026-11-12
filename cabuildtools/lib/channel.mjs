/**
 * 渠道参数解析：-beta / -dev / -canary（不带则返回 null）
 * 供 buildRelease / buildSnapshot 共用。
 */
export default function channelOf(args) {
	var list = args || [], i;
	for (i = 0; i < list.length; i++) {
		if (list[i] == "-beta" || list[i] == "--beta") return "beta";
		if (list[i] == "-dev" || list[i] == "--dev") return "dev";
		if (list[i] == "-canary" || list[i] == "--canary") return "canary";
	}
	return null;
};
