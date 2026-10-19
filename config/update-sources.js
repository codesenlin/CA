/**
 * 更新源配置（App 内的「检查更新」用，Updater 读取）。
 *
 * 每个渠道给一组地址，**按顺序尝试**，前一个失败才用下一个（顺序 fallback）。
 * 想接管更新检查就改这里：
 *   - 换成自己的服务器：把 content 改成自己的 URL 列表
 *   - 彻底不检查更新：把 content 改成空数组 []（Updater 会静默跳过）
 *
 * 注意：这里的地址只决定「从哪拉更新信息」，更新信息里再带具体的
 * 热更新文件（hotfix.url / hotfix.sign）和下载页链接（downloads）。
 *
 * 热更新文件会用 App 内置公钥（Secret.getVerifyKey）验签，所以就算这里
 * 指向别人的服务器，拉到别人的 core.js 也装不上，会回落到内置脚本。
 */
module.exports = {
	// 正式渠道：更新信息文件 hotfix.json
	// 结构：{ version, info, downloads{...}, hotfix{url,sign,shell,sha1}, requirements[] }
	// 检查频率：每 7 天一次（可在设置里关闭）
	release : {
		id : "9f15605c-b7fa-49c7-8ee8-55b525570d96",
		content : [
			"https://ca.projectxero.top/hotfix.json",
			"https://projectxero.top/ca/hotfix.json",
			"http://47.102.100.56/ca/hotfix.json",
			"https://projectxero.gitee.io/ca/hotfix.json",
			"https://xeroalpha.github.io/CA/pages/hotfix.json"
		]
	},

	// 非正式渠道（debug / beta / snapshot）：更新信息文件 snapshot.json
	// 结构：{ version, snapshot{url,sha1}, ... }，下载后写入 debugSource
	// 检查频率：每次启动
	beta : {
		id : "7a0df683-bae8-477d-9d84-b2a0c72eadcc",
		content : [
			"https://ca.projectxero.top/snapshot.json",
			"https://projectxero.top/ca/snapshot.json",
			"http://47.102.100.56/ca/snapshot.json"
		]
	}
};
