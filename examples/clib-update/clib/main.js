/**
 * 拓展包本体：App 运行时会 eval 这个对象。
 *
 * update 字段告诉 App「去哪查这个库有没有新版本」——构建脚本会把 <BASE>
 * 替换成你这次构建的源站地址（比如 http://192.168.1.10:8000/）。
 */
({
	$schema : "https://ca.projectxero.top/clib/schema/schema_v1.json",
	name : "演示拓展包",
	author : "demo",
	description : "当前版本见下方 version，更新说明见 update 指向的 demo.json",
	uuid : "a1b2c3d4-0000-4000-8000-000000000001",
	version : [1,0,1],
	require : [],
	update : "http://127.0.0.1:8000/demo.json",
	commands : {
		"/demo" : {
			description : "演示命令（版本 1.0.0）"
		}
	}
})
