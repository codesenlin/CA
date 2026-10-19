/**
 * 库元数据（clibspawn / 构建脚本读这个文件）：
 *   id/name/author/source（入口文件）/uuid/version/versionCode/description
 * 构建脚本会把 version / versionCode 重写成本次演示的版本。
 */
module.exports = {
	id : "demo",
	name : "演示拓展包",
	author : "demo",
	source : "main.js",
	uuid : "a1b2c3d4-0000-4000-8000-000000000001",
	version : "1.0.1",
	versionCode : [1,0,1],
	description : "用来演示拓展包更新机制的最小库"
};
