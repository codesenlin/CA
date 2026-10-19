#!/usr/bin/env node
/**
 * 模拟 clib 的构建与发布：把 clib/ 下的源码编译成源站产物（site/）。
 *
 * 用法：
 *   node build.js                          # 用 library.js 里现有的版本构建
 *   node build.js --version 1.0.1          # 改版本后再构建（会写回 clib/ 里的两个文件）
 *   node build.js --base http://127.0.0.1:8000/   # 指定源站地址（写进 update 字段）
 *
 * 产物（对应真实环境里 caclib/outputs/ 的那一套）：
 *   site/demo.lib     SIGNLIB01（分发格式）
 *   site/demo.json    更新描述：{uuid, version, url, message, source, sha1}
 *   site/index.json   索引（分页）
 *   site/map.json     {sourceId, content:{uuid: updateurl}}
 *   site/info.json    源站入口：{sourceId, map, index, indexPages, indexLibs, pubkey, ...}
 *   site/verify.key   验签公钥（二进制，从这里读出来给 App 用）
 *   site/demo-source.js  未编译的源码（方便直接导入手机，走安全级别 0）
 */
const fs = require("fs");
const path = require("path");
const crypto = require("crypto");
const js2signlib = require("../../cabuildtools/js2signlib.js");

const ROOT = __dirname;
const CLIB = path.join(ROOT, "clib");
const SITE = path.join(ROOT, "site");
const KEYS = path.join(ROOT, "keys");

function arg(name, def) {
	var i = process.argv.indexOf(name);
	return i >= 0 && process.argv[i + 1] ? process.argv[i + 1] : def;
}

/** 演示用密钥：真实环境请自己用 openssl 生成并保管好私钥 */
function ensureKeys() {
	var pem = path.join(KEYS, "source.pem"), pub = path.join(KEYS, "source.pub");
	fs.mkdirSync(KEYS, { recursive: true });
	if (!fs.existsSync(pem)) {
		var pair = crypto.generateKeyPairSync("rsa", { modulusLength: 2048 });
		fs.writeFileSync(pem, pair.privateKey.export({ type: "pkcs1", format: "pem" }));
		fs.writeFileSync(pub, pair.publicKey.export({ type: "spki", format: "pem" }));
		console.log("[keys ] 已生成演示密钥 keys/source.pem（仅演示用，真实环境自己生成）");
	} else {
		console.log("[keys ] 复用已有密钥 keys/source.pem");
	}
	return { pem: pem, pub: pub };
}

/** 把版本号和源站地址写回源码，方便反复构建不同版本 */
function prepareSource(version, base) {
	var libPath = path.join(CLIB, "library.js"), mainPath = path.join(CLIB, "main.js");
	var lib = fs.readFileSync(libPath, "utf-8"), main = fs.readFileSync(mainPath, "utf-8");
	var code = JSON.stringify(version.split(".").map(Number));
	lib = lib.replace(/version *: *"[^"]*"/, 'version : "' + version + '"')
		.replace(/versionCode *: *\[[^\]]*\]/, 'versionCode : ' + code);
	main = main.replace(/version *: *\[[^\]]*\]/, 'version : ' + code)
		.replace(/update *: *"[^"]*"/, 'update : ' + JSON.stringify(base + "demo.json"))
		.replace(/"演示命令（版本 " \+ "[^"]*" \+ "）"/, JSON.stringify("演示命令（版本 " + version + "）"));
	fs.writeFileSync(libPath, lib);
	fs.writeFileSync(mainPath, main);
	delete require.cache[require.resolve(libPath)];
	return require(libPath);
}

function main() {
	var version = arg("--version", null);
	var base = arg("--base", "http://127.0.0.1:8000/");
	if (base.slice(-1) != "/") base += "/";
	var keys = ensureKeys();
	var meta = prepareSource(version || require(path.join(CLIB, "library.js")).version, base);
	var code = path.join(CLIB, "main.js");

	console.log("[build] 版本 " + meta.version + "，源站 " + base);
	fs.rmSync(SITE, { recursive: true, force: true });
	fs.mkdirSync(SITE, { recursive: true });

	// 1) 编译成 SIGNLIB01（真实环境是 js2signlib，没有私钥时会退化成 LIBRARY）
	js2signlib(fs.readFileSync(code), keys.pem, path.join(SITE, "demo.lib"));
	var libBytes = fs.readFileSync(path.join(SITE, "demo.lib"));
	var sha1 = crypto.createHash("sha1").update(libBytes).digest("base64");
	console.log("[lib  ] demo.lib " + libBytes.length + " 字节（SIGNLIB01），sha1=" + sha1);

	// 2) 更新描述（App 检查更新时读的就是它）
	fs.writeFileSync(path.join(SITE, "demo.json"), JSON.stringify({
		uuid: meta.uuid,
		version: meta.versionCode,
		url: base + "demo.lib",
		message: "演示更新：命令 /demo 现在会显示 " + meta.version,
		source: base,
		sha1: sha1
	}, null, "\t") + "\n");

	// 3) 索引 / map / info / verify.key（对应 clibspawn 的 writeIndex/writeMap/writeInfo/writeVerifyKey）
	var entry = {
		name: meta.name,
		author: meta.author,
		description: meta.description,
		uuid: meta.uuid,
		version: meta.versionCode,
		downloadurl: base + "demo.lib",
		updateurl: base + "demo.json",
		sha1: sha1
	};
	fs.writeFileSync(path.join(SITE, "index.json"), JSON.stringify({
		sourceId: "demo-source",
		pageNo: 0,
		content: [entry]
	}, null, "\t") + "\n");
	fs.writeFileSync(path.join(SITE, "map.json"), JSON.stringify({
		sourceId: "demo-source",
		content: (function() { var m = {}; m[meta.uuid] = base + "demo.json"; return m; })()
	}, null, "\t") + "\n");
	var pubPem = fs.readFileSync(keys.pub, "utf-8");
	var pubDer = Buffer.from(pubPem.replace(/-----[A-Z ]+-----/g, "").replace(/\s+/g, ""), "base64");
	fs.writeFileSync(path.join(SITE, "verify.key"), pubDer);
	fs.writeFileSync(path.join(SITE, "info.json"), JSON.stringify({
		sourceId: "demo-source",
		map: base + "map.json",
		index: base + "index.json",
		indexPages: 1,
		indexLibs: 1,
		pubkey: base + "verify.key",
		authorities: undefined,
		lastUpdate: Date.now(),
		maintainer: "demo",
		details: "用来演示拓展包更新机制的本地源站"
	}, null, "\t") + "\n");

	// 4) 未编译的源码副本，方便直接导入手机（安全级别 0 时可加载）
	fs.copyFileSync(code, path.join(SITE, "demo-source.js"));

	console.log("[site ] 已生成 site/：demo.lib / demo.json / index.json / map.json / info.json / verify.key");
	console.log("[next ] node simulate.js --installed " + meta.version + "   # 模拟 App 检查更新");
}
main();
