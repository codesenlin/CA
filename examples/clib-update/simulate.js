#!/usr/bin/env node
/**
 * 模拟 App 的拓展包更新流程：把 CA.Library 的关键步骤按顺序重放一遍，
 * 每一步都打印出来，方便对照源码看「更新」到底是怎么发生的。
 *
 * 用法：
 *   node simulate.js --installed 1.0.0            # 假装本地装的是 1.0.0
 *   node simulate.js --installed 1.0.1            # 假装已经是最新
 *
 * 前置：先跑 node build.js 生成 site/
 */
const fs = require("fs");
const path = require("path");
const http = require("http");
const crypto = require("crypto");

const SITE = path.join(__dirname, "site");

function arg(name, def) {
	var i = process.argv.indexOf(name);
	return i >= 0 && process.argv[i + 1] ? process.argv[i + 1] : def;
}

/** 复刻 NeteaseAdapter.compareVersion：a>b 返回 1，相等 0，a<b 返回 -1 */
function compareVersion(a, b) {
	var n = Math.max(a.length, b.length), i, p1, p2;
	for (i = 0; i < n; i++) {
		p1 = isNaN(a[i]) ? -1 : parseInt(a[i]);
		p2 = isNaN(b[i]) ? -1 : parseInt(b[i]);
		if (p1 < p2) return -1;
		if (p1 > p2) return 1;
	}
	return 0;
}

function toArray(v) {
	return Array.isArray(v) ? v : String(v).split(".").map(Number);
}

function get(port, urlPath) {
	return new Promise(function(resolve, reject) {
		http.get({ host: "127.0.0.1", port: port, path: urlPath }, function(res) {
			var chunks = [];
			if (res.statusCode != 200) {
				reject(new Error("HTTP " + res.statusCode + " " + urlPath));
				return;
			}
			res.on("data", function(c) { chunks.push(c); });
			res.on("end", function() { resolve(Buffer.concat(chunks)); });
		}).on("error", reject);
	});
}

function serve(port) {
	var types = { ".json": "application/json", ".lib": "application/octet-stream", ".js": "text/javascript", ".key": "application/octet-stream" };
	return new Promise(function(resolve) {
		var server = http.createServer(function(req, res) {
			var file = path.join(SITE, decodeURIComponent(req.url.split("?")[0]));
			if (!file.startsWith(SITE) || !fs.existsSync(file) || fs.statSync(file).isDirectory()) {
				res.writeHead(404); res.end("not found"); return;
			}
			res.writeHead(200, { "Content-Type": types[path.extname(file)] || "application/octet-stream" });
			res.end(fs.readFileSync(file));
		});
		server.listen(port, "127.0.0.1", function() { resolve(server); });
	});
}

async function main() {
	var installed = toArray(arg("--installed", "1.0.0"));
	var port = parseInt(arg("--port", "8000"), 10);
	if (!fs.existsSync(path.join(SITE, "info.json"))) {
		console.log("没有 site/，先跑 node build.js");
		process.exit(1);
	}
	var server = await serve(port);
	var base = "http://127.0.0.1:" + port + "/";
	console.log("源站已启动：" + base + "（模拟 CA.Library.getSourceUrl()）\n");

	try {
		// 1) info.json —— 源站入口（CA.Library.requestSourceInfo）
		var info = JSON.parse((await get(port, "/info.json")).toString());
		console.log("[1] GET info.json");
		console.log("    sourceId=" + info.sourceId + "  indexLibs=" + info.indexLibs +
			"  indexPages=" + info.indexPages + "  pubkey=" + info.pubkey);

		// 2) map.json —— uuid -> 更新描述地址（CA.Library.requestSourceMap）
		var map = JSON.parse((await get(port, "/map.json")).toString());
		console.log("\n[2] GET map.json");
		console.log("    " + JSON.stringify(map.content));

		var uuid = Object.keys(map.content)[0];
		var updateUrl = map.content[uuid];

		// 3) 更新描述（CA.Library.requestUpdateInfo 拉的就是它）
		var update = JSON.parse((await get(port, new URL(updateUrl).pathname)).toString());
		console.log("\n[3] GET " + path.basename(updateUrl) + "（更新描述）");
		console.log("    " + JSON.stringify(update, null, 2).replace(/\n/g, "\n    "));

		// 4) 比较版本（NeteaseAdapter.compareVersion）
		var flag = compareVersion(toArray(update.version), installed);
		console.log("\n[4] 比较版本：远端 " + update.version.join(".") + " vs 本地 " + installed.join(".") + " → " +
			(flag > 0 ? "有更新 ✓" : "已是最新"));
		if (flag <= 0) {
			console.log("\n流程结束：不需要更新。");
			return;
		}

		// 5) 下载 + 校验（CA.Library.downloadLib）
		var libBytes = await get(port, new URL(update.url).pathname);
		var sha1 = crypto.createHash("sha1").update(libBytes).digest("base64");
		console.log("\n[5] GET " + path.basename(update.url) + "（" + libBytes.length + " 字节）");
		console.log("    SHA-1 校验：" + (sha1 === update.sha1 ? "通过 ✓" : "失败 ✗（" + sha1 + "）"));
		if (sha1 !== update.sha1) throw new Error("sha1 不匹配");

		var pubDer = await get(port, new URL(info.pubkey).pathname);
		// App 里是 X509EncodedKeySpec(verify)，Node 里等价于 der/spki 的 KeyObject
		var pubKey = crypto.createPublicKey({ key: pubDer, format: "der", type: "spki" });
		var v = crypto.createVerify("RSA-SHA256");
		v.update(libBytes.slice(9 + 256));
		console.log("    RSA 验签（源站 verify.key）：" + (v.verify(pubKey, libBytes.slice(9, 9 + 256)) ? "通过 ✓" : "失败 ✗"));

		// 6) 转成本地 LIBSIGN01（downloadLib 的写法）
		var payload = libBytes.slice(9), idLen = Buffer.alloc(4);
		idLen.writeInt32BE(Buffer.byteLength(info.sourceId), 0);
		var local = Buffer.concat([Buffer.from("LIBSIGN01"), idLen, Buffer.from(info.sourceId), payload]);
		console.log("\n[6] 转成本地格式 LIBSIGN01（" + local.length + " 字节）");
		console.log("    magic=" + local.slice(0, 9) + "  sourceId 长度=" + info.sourceId.length + "  payload=" + payload.length);
		fs.writeFileSync(path.join(__dirname, "simulated-local.lib"), local);
		console.log("    已写入 simulated-local.lib（真实 App 还会多写一个 .hash）");

		console.log("\n[7] 完成：当前库会从 " + installed.join(".") + " 更新到 " + update.version.join(".") +
			"，界面提示「已经更新至最新版本，重启命令助手生效」");
	} finally {
		server.close();
	}
}

main().catch(function(e) {
	console.error("\n失败：" + e.message);
	process.exit(1);
});
