/**
 * clib 发布源的设置（由 caclib 里的 clibspawn / publishCLib 读取）。
 *
 * uuid       这个命令库源自己的 uuid
 * outputUrl  发布后的访问前缀（产物会写成 <outputUrl><id>.lib / <id>.json）
 * outputPath 本地产物目录
 * perPage    索引分页大小
 * signKey / verifyKey  clib 签名私钥 / 公钥（不入库，需自己生成）
 * details    展示给用户的源说明
 */
module.exports = {
	"uuid": "7c9d392b-7a1f-4226-a35a-a1137cf64e47",
	"outputUrl": "https://ca.projectxero.top/clib/",
	"outputPath": "./outputs",
	"perPage": 10,
	"signKey": "./signature/source.pem",
	"verifyKey": "./signature/source.pub",
	"maintainer": "ProjectXero",
	"details": "++官方拓展包源++"
};
