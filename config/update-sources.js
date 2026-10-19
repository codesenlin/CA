/**
 * 更新配置（一处管全）：客户端「检查更新」用哪里当源，以及发布/热更新用到的地址。
 *
 * 上半部分 release / beta 是**客户端**用的：每个渠道给一组地址，按顺序尝试，
 * 前一个失败才用下一个。换成自己的服务器就改 content；清空 content（[]）就彻底
 * 不检查更新（Updater 会静默跳过，不会报错）。
 *
 * 下半部分 downloadSource / remoteRoot 是**发布端**用的：生成 update.json、
 * 热更新文件地址时用。只影响产物，不影响客户端检查。
 *
 * 注意：这里的地址只决定「从哪拉更新信息 / 更新文件」，热更新文件本身还会用
 * App 内置公钥（Secret.getVerifyKey）验签——指向别人的服务器也装不上别人的脚本。
 */
module.exports = {
	// ─────────────── 客户端：检查更新的源 ───────────────

	// 正式渠道：更新信息文件 hotfix.json（每 7 天检查一次，设置里可关）
	// 结构：{ version, info, downloads{...}, hotfix{url,sign,shell,sha1}, requirements[] }
	// id 只是本地缓存键，随便填一个唯一字符串即可
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

	// 非正式渠道（debug / beta / snapshot）：更新信息文件 snapshot.json（每次启动都查）
	// 结构：{ version, snapshot{url,sha1}, ... }，下载后写入 debugSource
	beta : {
		id : "7a0df683-bae8-477d-9d84-b2a0c72eadcc",
		content : [
			"https://ca.projectxero.top/snapshot.json",
			"https://projectxero.top/ca/snapshot.json",
			"http://47.102.100.56/ca/snapshot.json"
		]
	},

	// ─────────────── 发布端：生成更新信息时用 ───────────────

	// 展示给用户的下载页（写进 update.json 的 downloads 字段）
	downloadSource : {
		"官方网站（最推荐）" : "https://projectxero.top/ca/release.apk"
	},

	// 更新文件所在的站点根目录，必须以 / 结尾
	// 拼出来的是 <remoteRoot>hotfix/release.js、<remoteRoot>hotfix/release.sign、<remoteRoot>snapshot.js
	remoteRoot : "https://ca.projectxero.top/"
};
