#!/usr/bin/env node
/**
 * .lib 打包 / 解包工具（命令助手拓展包容器）
 *
 *   npm run pack   -- <输入.js|输入.json> [-o 输出.lib] [--sign 私钥.pem]
 *   npm run unpack -- <输入.lib> [-o 输出] [--key 公钥.pem|der]
 *   npm run info   -- <输入.lib>
 *
 * 支持的容器：
 *   LIBRARY    本地/安装器库：7 字节魔数 + 8 字节时间戳 + gzip 载荷
 *   SIGNLIB01  分发格式：9 字节魔数 + 256 字节 RSA 签名 + 8 字节时间戳 + gzip
 *   LIBSIGN01  本地已验签：9 字节魔数 + int32 sourceId 长度 + sourceId + SIGNLIB01 的 [9..]
 *
 * 打包默认出 LIBRARY（锁定包）；加 --sign 出 SIGNLIB01。
 * 解包会自动识别格式，内容能解析成 JSON 就输出 .json（格式化），否则输出 .js。
 */
const fs = require("fs");
const path = require("path");
const zlib = require("zlib");
const crypto = require("crypto");
const { parseArgs : parseCliArgs } = require("util");

const MAGIC_LIBRARY = "LIBRARY";
const MAGIC_SIGNED = "SIGNLIB01";
const MAGIC_LOCAL = "LIBSIGN01";
const SIGN_LEN = 256;

function usage() {
	console.log([
		"用法：",
		"  npm run pack   -- [目标] [选项]",
		"  npm run unpack -- [目标] [选项]",
		"  npm run info   -- <文件> [选项]",
		"",
		"目标（pack/unpack）：",
		"  不给目标      批量：工作区 src/ 或 lib/ → build/libwork/{pack,unpack}",
		"                 （批量模式不参与密钥）",
		"  纯文件名      只在工作区对应目录（pack→src，unpack→lib）里找；",
		"                 可省略后缀（会自动匹配并提示选中的文件）",
		"  带路径文件    对指定文件操作；可省略后缀（提示后处理）",
		"  文件夹        只处理里面的 .json/.js（pack）或 .lib（unpack）",
		"",
		"密钥（--sign 给 pack，--key 给 unpack/info）：",
		"  不给          pack 不加密；unpack/info 遍历工作区 keys/ 尝试匹配",
		"  纯文件名      只在工作区 keys/ 里找，需要完整文件名（含后缀）",
		"  带路径文件    对指定密钥文件操作",
		"  文件夹        仅 unpack/info：先遍历工作区 keys/，再遍历该目录",
		"                （pack 的 --sign 不支持目录）",
		"",
		"info：非签名包直接输出基础信息；签名包按上面的密钥规则验签",
		"其他选项：",
		"  -o, --out <路径>   输出文件或目录（目录时按内容自动命名）",
		"  --raw              unpack 时 JSON 不做格式化",
		"  -d, --dir <目录>   工作区目录（默认 libwork/）",
		"",
		"容器格式：LIBRARY / SIGNLIB01 / LIBSIGN01（也识别裸 GZIP）",
		"目录结构见 libwork/README.md"
	].join("\n"));
}

function formatVersion(v) {
	if (!Array.isArray(v)) return String(v);
	if (v.length === 6 && v[0] >= 2000) {
		var pad2 = function(n) { return ("0" + n).slice(-2); };
		return [v[0], v[1], v[2], pad2(v[3]), pad2(v[4]), pad2(v[5])].join(".");
	}
	return v.join(".");
}

function fail(msg) {
	console.error("失败：" + msg);
	process.exit(1);
}

function ensureDir(dir) {
	fs.mkdirSync(dir, { recursive : true });
}

function isDir(target) {
	try {
		return fs.existsSync(target) && fs.statSync(target).isDirectory();
	} catch (e) {
		return false;
	}
}

/** -o 可以给文件名，也可以给目录（目录时按输入文件名 + 扩展名自动命名） */
function resolveOut(outOpt, input, ext) {
	if (!outOpt) return path.join(path.dirname(input), path.basename(input, path.extname(input)) + ext);
	if (isDir(outOpt)) return path.join(outOpt, path.basename(input, path.extname(input)) + ext);
	return outOpt;
}

function isFile(target) {
	try {
		return fs.existsSync(target) && fs.statSync(target).isFile();
	} catch (e) {
		return false;
	}
}

function hasPathSep(value) {
	return /[\\/]/.test(value) || path.isAbsolute(value);
}

function warnInferred(raw, file, kind) {
	console.log("注意：未指定后缀，按 " + path.basename(file) + "（" + kind + "）处理");
}

/**
 * 目标解析（pack/unpack/info 通用）：
 *  - 不给目标        → 批量（工作区）
 *  - 纯文件名        → 只在工作区对应目录（src/lib）找，支持省略后缀
 *  - 带路径的文件    → 指定文件，支持省略后缀
 *  - 文件夹          → 对文件夹里对应后缀（json/js/lib）的文件批量操作
 */
function resolveTarget(args, dirs, cmd) {
	const exts = cmd === "pack" ? [".json", ".js"] : [".lib"];
	const kindName = cmd === "pack" ? "源文件" : "库文件";
	const sub = cmd === "pack" ? dirs.src : dirs.lib;
	const raw = args._[0];
	if (raw === undefined) return { kind : "batch" };
	if (isDir(raw)) return { kind : "dir", dir : path.resolve(raw) };
	if (isFile(raw)) return { kind : "file", file : path.resolve(raw) };

	if (hasPathSep(raw)) {
		for (const ext of exts) {
			if (isFile(raw + ext)) {
				warnInferred(raw, raw + ext, kindName);
				return { kind : "file", file : path.resolve(raw + ext) };
			}
		}
		fail("找不到文件：" + raw);
	}

	// 纯文件名：只在工作区对应目录里找（先按原名，再补后缀）
	const exact = path.join(sub, raw);
	if (isFile(exact)) return { kind : "file", file : exact };
	const matches = [];
	for (const ext of exts) {
		const cand = path.join(sub, raw + ext);
		if (isFile(cand)) matches.push(cand);
	}
	if (matches.length > 1) {
		fail("工作区里有多个同名文件，请指定后缀：\n  " + matches.map(function(f) {
			return path.basename(f);
		}).join("\n  "));
	}
	if (matches.length === 1) {
		if (!path.extname(raw)) warnInferred(raw, matches[0], kindName);
		return { kind : "file", file : matches[0] };
	}
	fail("工作区 " + sub + " 里找不到文件：" + raw);
}

/**
 * 密钥解析：
 *  - 没给：pack 不加密；unpack/info 遍历工作区 keys 匹配
 *  - 纯文件名：工作区 keys 里找（需要完整文件名，不加后缀）
 *  - 带路径：指定文件；带路径的目录：pack 不支持，unpack/info 遍历（工作区优先）
 */
function resolveKeySpec(args, dirs, cmd) {
	if (args["key-dir"]) return { type : "traversal", dirs : [dirs.keys, path.resolve(args["key-dir"])] };
	if (args["no-verify"]) return { type : "none" };
	const value = cmd === "pack" ? args.sign : args.key;
	const flag = cmd === "pack" ? "--sign" : "--key";
	if (value === undefined) {
		return cmd === "pack" ? { type : "none" } : { type : "auto" };
	}
	if (isFile(value)) return { type : "file", file : path.resolve(value) };
	if (isDir(value)) {
		if (cmd === "pack") fail("pack 的 " + flag + " 不支持目录：" + value);
		return { type : "traversal", dirs : [dirs.keys, path.resolve(value)] };
	}
	if (hasPathSep(value) || path.isAbsolute(value)) fail("找不到密钥文件：" + value);
	const exact = path.join(dirs.keys, value);
	if (isFile(exact)) return { type : "file", file : exact };
	fail("工作区 keys 里找不到密钥文件：" + value + "\n  需要完整文件名（含后缀）；工作区：" + dirs.keys);
}

function looksLikePublicKeyFile(file) {
	try {
		const head = fs.readFileSync(file).slice(0, 4096).toString("utf-8");
		if (head.indexOf("CERTIFICATE") >= 0 || head.indexOf("PUBLIC KEY") >= 0) return true;
	} catch (e) {
	}
	return [".pem", ".crt", ".cer", ".der", ".pub"].indexOf(path.extname(file).toLowerCase()) >= 0;
}

/** 收集验签候选公钥：目录里所有像公钥/证书的文件；单文件直接返回 */
function publicKeyFiles(target) {
	const out = [];
	if (!target) return out;
	if (isDir(target)) {
		fs.readdirSync(target).sort().forEach(function(name) {
			const full = path.join(target, name);
			try {
				if (fs.statSync(full).isFile() && looksLikePublicKeyFile(full)) out.push(full);
			} catch (e) {
			}
		});
	} else if (fs.existsSync(target) && fs.statSync(target).isFile()) {
		out.push(target);
	}
	return out;
}

function candidatesForSpec(spec, dirs) {
	if (spec.type === "auto") return publicKeyFiles(dirs.keys);
	if (spec.type !== "traversal") return [];
	const out = [], seen = {};
	spec.dirs.forEach(function(dir) {
		publicKeyFiles(dir).forEach(function(file) {
			const key = path.resolve(file);
			if (!seen[key]) {
				seen[key] = true;
				out.push(file);
			}
		});
	});
	return out;
}

/**
 * 验签；返回 true=通过，false=显式密钥验签失败（调用方 exit 2），
 * null=没有签名/跳过。
 */
function verifyContainer(info, spec, dirs) {
	if (!info.verify) {
		if (spec.type === "file") console.log("验签：跳过（" + info.format + " 容器没有签名）");
		return null;
	}
	if (spec.type === "none") return null;
	if (spec.type === "file") {
		const publicKey = readPublicKey(spec.file);
		const ok = crypto.createVerify("RSA-SHA256").update(info.verify.data).verify(publicKey, info.verify.signature);
		console.log("验签：" + (ok ? "通过" : "失败") + "（" + spec.file + "）");
		return ok ? true : false;
	}
	const keys = candidatesForSpec(spec, dirs);
	if (!keys.length) {
		console.log("验签：未找到可用密钥（跳过）");
		return null;
	}
	for (const keyFile of keys) {
		try {
			const publicKey = readPublicKey(keyFile);
			if (crypto.createVerify("RSA-SHA256").update(info.verify.data).verify(publicKey, info.verify.signature)) {
				console.log("验签：通过（" + keyFile + "）");
				return true;
			}
		} catch (e) {
			// 不是可用的公钥/证书，试下一个
		}
	}
	console.log("验签：未找到匹配密钥（跳过；试过 " + keys.length + " 个）");
	return null;
}

function parseArgs(argv) {
	let parsed;
	try {
		parsed = parseCliArgs({
			args : argv,
			options : {
				out : { type : "string", short : "o" },
				sign : { type : "string" },
				key : { type : "string" },
				raw : { type : "boolean" },
				dir : { type : "string", short : "d" },
				// 内部使用：批量/目录模式把解析好的密钥候选传给子进程
				"key-dir" : { type : "string" },
				"no-verify" : { type : "boolean" },
				help : { type : "boolean", short : "h" }
			},
			allowPositionals : true,
			strict : true
		});
	} catch (e) {
		// 未知参数 / 缺值时直接报错，不再像手写版那样默默当路径
		console.error("失败：" + e.message);
		usage();
		process.exit(1);
	}
	return Object.assign(parsed.values, { _ : parsed.positionals });
}

/** 读私钥：PEM 或 DER 都行 */
function readPrivateKey(file) {
	const buf = fs.readFileSync(file);
	const text = buf.toString("utf-8");
	if (text.indexOf("BEGIN") >= 0) return text;
	return crypto.createPrivateKey({ key: buf, format: "der", type: "pkcs8" });
}

/** 读公钥：PEM / DER / 证书 PEM / 证书 DER */
function readPublicKey(file) {
	const buf = fs.readFileSync(file);
	const text = buf.toString("utf-8");
	try {
		if (text.indexOf("BEGIN CERTIFICATE") >= 0) {
			const x509 = new crypto.X509Certificate(text);
			return x509.publicKey;
		}
		if (text.indexOf("BEGIN") >= 0) return crypto.createPublicKey(text);
		return crypto.createPublicKey({ key: buf, format: "der", type: "spki" });
	} catch (e) {
		// 可能是证书 DER
		const x509 = new crypto.X509Certificate(buf);
		return x509.publicKey;
	}
}

function gzip(content) {
	return zlib.gzipSync(Buffer.from(content, "utf-8"));
}

/** 时间戳：8 字节大端 int64（毫秒）。App 侧只跳过这 8 字节，不解析内容 */
function writeStamp(buf, offset, ms) {
	buf.writeBigInt64BE(BigInt(ms), offset);
}

function readStamp(buf, offset) {
	return Number(buf.readBigInt64BE(offset));
}

/** 打成 LIBRARY：7 字节魔数 + 8 字节时间戳 + gzip */
function packLibrary(content, timestamp) {
	const now = timestamp || Date.now();
	const head = Buffer.alloc(15);
	head.write(MAGIC_LIBRARY, 0, "utf-8");
	writeStamp(head, 7, now);
	return Buffer.concat([head, gzip(content)]);
}

/** 打成 SIGNLIB01：9 字节魔数 + 256 字节 RSA-SHA256 + 8 字节时间戳 + gzip */
function packSigned(content, privateKey, timestamp) {
	const now = timestamp || Date.now();
	const stamp = Buffer.alloc(8);
	writeStamp(stamp, 0, now);
	const payload = Buffer.concat([stamp, gzip(content)]);
	const signature = crypto.createSign("RSA-SHA256").update(payload).sign(privateKey);
	return Buffer.concat([Buffer.from(MAGIC_SIGNED, "utf-8"), signature, payload]);
}

/** 识别容器类型并取出 gzip 载荷 + 元信息 */
function detect(buf) {
	const head = buf.slice(0, 9).toString("utf-8");
	if (buf.slice(0, 7).toString("utf-8") === MAGIC_LIBRARY) {
		return {
			format: MAGIC_LIBRARY,
			timestamp: readStamp(buf, 7),
			payload: buf.slice(15),
			verify: null
		};
	}
	if (head === MAGIC_SIGNED) {
		const dataStart = 9 + SIGN_LEN;              // 签名覆盖「时间戳 + gzip」
		return {
			format: MAGIC_SIGNED,
			timestamp: readStamp(buf, dataStart),
			payload: buf.slice(dataStart + 8),        // 解压只取 gzip
			verify: {
				signature: buf.slice(9, 9 + SIGN_LEN),
				data: buf.slice(dataStart)
			}
		};
	}
	if (head === MAGIC_LOCAL) {
		const idLen = buf.readInt32BE(9);
		const sourceId = buf.slice(13, 13 + idLen).toString("utf-8");
		const rest = buf.slice(13 + idLen); // 从 SIGNLIB01 的 [9..] 开始
		return {
			format: MAGIC_LOCAL,
			sourceId,
			timestamp: readStamp(rest, SIGN_LEN),
			payload: rest.slice(SIGN_LEN + 8),        // 跳过签名与时间戳，只剩 gzip
			verify: {
				signature: rest.slice(0, SIGN_LEN),
				data: rest.slice(SIGN_LEN)
			}
		};
	}
	if (buf.slice(0, 2).toString("hex") === "1f8b") {
		return { format: "GZIP", timestamp: 0, payload: buf, verify: null };
	}
	return null;
}

function decompress(info) {
	return zlib.gunzipSync(info.payload).toString("utf-8");
}

/** 从内容里猜库元数据（JSON 库直接解析；插件库用正则从源码里抽） */
function guessMeta(content) {
	let json = null;
	try {
		json = JSON.parse(content);
	} catch (e) {}
	let meta = {};
	if (json) {
		meta = {
			name: json.name,
			uuid: json.uuid,
			version: json.version,
			author: json.author,
			type: "JSON 库"
		};
	} else {
		const grab = function(k) {
			const arr = content.match(new RegExp(k + "\\s*=\\s*\\[([^\\]]*)\\]"));
			if (arr) return arr[1].split(",").map(function(x) { return parseInt(x, 10) || 0; });
			const str = content.match(new RegExp(k + "\\s*=\\s*[\"']([^\"']*)[\"']"));
			return str ? str[1] : undefined;
		};
		meta = {
			name: grab("name"),
			uuid: grab("uuid"),
			version: grab("version"),
			author: grab("author"),
			type: /^\s*Plugins\.inject/.test(content) ? "插件库" : "JS"
		};
	}
	return meta;
}

function cmdPack(args) {
	const dirs = workDirs(args);
	const target = resolveTarget(args, dirs, "pack");
	if (target.kind === "batch") return cmdPackBatch(args, dirs);
	if (target.kind === "dir") return cmdPackDirTarget(target.dir, args, dirs);

	const input = target.file;
	const spec = resolveKeySpec(args, dirs, "pack");
	const signPath = spec.type === "file" ? spec.file : null;

	const raw = fs.readFileSync(input);
	let content, isJson;
	const inputInfo = detect(raw);
	if (inputInfo) {
		// 输入本身就是一个 .lib：先解出内容，再按目标格式重新打包（可用来补签名）
		content = decompress(inputInfo).replace(/\s+$/, "");
		isJson = (function() {
			try {
				JSON.parse(content);
				return true;
			} catch (e) {
				return false;
			}
		})();
		console.log("输入是 " + inputInfo.format + " 容器，先解出内容再重新打包");
	} else {
		content = raw.toString("utf-8").replace(/\s+$/, "");
		isJson = path.extname(input).toLowerCase() === ".json";
	}
	if (!content) fail("输入是空的");

	// 校验一下内容是否能被 App 加载（JSON 就直接解析，JS 按表达式求值）
	if (isJson) {
		try {
			JSON.parse(content);
		} catch (e) {
			fail("JSON 解析失败：" + e.message);
		}
	} else {
		try {
			new Function("return (" + content + ")");
		} catch (e) {
			fail("不是合法的库代码：" + e.message);
		}
	}

	let out = resolveOut(args.out, input, ".lib");
	ensureDir(path.dirname(out));
	if (path.resolve(out) === path.resolve(input)) {
		console.log("注意：输出与输入是同一个文件，将覆盖原文件（建议用 -o 指定新文件名）");
	}
	let buf;
	if (signPath) {
		buf = packSigned(content, readPrivateKey(signPath));
	} else {
		buf = packLibrary(content);
	}
	fs.writeFileSync(out, buf);
	const format = signPath ? MAGIC_SIGNED : MAGIC_LIBRARY;
	console.log("打包完成：" + out);
	console.log("  格式：" + format + "  " + buf.length + " 字节");
	console.log("  内容：" + content.length + " 字符（" + (isJson ? "JSON" : "JS") + "）");
	return 0;
}

function cmdUnpack(args) {
	const dirs = workDirs(args);
	const target = resolveTarget(args, dirs, "unpack");
	if (target.kind === "batch") return cmdUnpackBatch(args, dirs);
	if (target.kind === "dir") return cmdUnpackDirTarget(target.dir, args, dirs);

	const input = target.file;
	const spec = resolveKeySpec(args, dirs, "unpack");
	const buf = fs.readFileSync(input);
	const info = detect(buf);
	if (!info) fail("无法识别的容器格式（头 9 字节：" + JSON.stringify(buf.slice(0, 9).toString("utf-8")) + "）");
	const verified = verifyContainer(info, spec, dirs);
	if (verified === false) process.exit(2);

	let content;
	try {
		content = decompress(info);
	} catch (e) {
		fail("解压失败（签名/格式可能不对）：" + e.message);
	}

	const isJson = (function() {
		try {
			JSON.parse(content);
			return true;
		} catch (e) {
			return false;
		}
	})();

	let out = resolveOut(args.out, input, isJson ? ".json" : ".js");
	ensureDir(path.dirname(out));
	const body = isJson && !args.raw ? JSON.stringify(JSON.parse(content), null, "\t") : content;
	fs.writeFileSync(out, body + (body.endsWith("\n") ? "" : "\n"));

	const meta = guessMeta(content);
	console.log("解包完成：" + out);
	console.log("  格式：" + info.format + (info.sourceId ? "（sourceId=" + info.sourceId + "）" : ""));
	console.log("  内容：" + (isJson ? "JSON" : "JS") + "  " + content.length + " 字符");
	if (meta.name) console.log("  库信息：" + meta.name + "  uuid=" + (meta.uuid || "?") + "  version=" + (meta.version === undefined ? "?" : formatVersion(meta.version)));
	return 0;
}

function cmdInfo(args) {
	const dirs = workDirs(args);
	const target = resolveTarget(args, dirs, "info");
	if (target.kind === "batch") fail("info 需要指定文件");
	if (target.kind === "dir") fail("info 只接受单个文件：" + target.dir);

	const input = target.file;
	const spec = resolveKeySpec(args, dirs, "info");
	const buf = fs.readFileSync(input);
	const info = detect(buf);
	if (!info) fail("无法识别的容器格式");

	// 非签名包：直接输出基础信息；签名包：按密钥模块规则验签
	if (info.verify) {
		const verified = verifyContainer(info, spec, dirs);
		if (verified === false) process.exit(2);
	}

	const content = (function() {
		try {
			return decompress(info);
		} catch (e) {
			return null;
		}
	})();
	const meta = content ? guessMeta(content) : {};
	console.log("文件：" + input);
	console.log("  容器：" + info.format + "  " + buf.length + " 字节");
	if (info.sourceId) console.log("  sourceId：" + info.sourceId);
	if (info.timestamp) console.log("  时间戳：" + new Date(info.timestamp).toLocaleString());
	if (content) console.log("  内容：" + content.length + " 字符  " + (meta.type || "?"));
	if (meta.name) console.log("  库名：" + meta.name);
	if (meta.uuid) console.log("  uuid：" + meta.uuid);
	if (meta.version) console.log("  版本：" + formatVersion(meta.version));
	if (meta.author) console.log("  作者：" + meta.author);
	return 0;
}

// ---------- 批量 / 目录模式：工作目录约定见 libwork/README.md ----------

const REPO_ROOT = path.resolve(__dirname, "..");
const { spawnSync } = require("child_process");

/** 批量命令都用“调自己”实现，行为和单文件命令完全一致 */
function runSingle(cmd, argv) {
	const r = spawnSync(process.execPath, [__filename, cmd].concat(argv), { encoding : "utf-8" });
	return { code : r.status === null ? 1 : r.status, out : (r.stdout || "") + (r.stderr || "") };
}

function listFiles(dir, exts) {
	if (!fs.existsSync(dir)) return [];
	return fs.readdirSync(dir).filter(function(name) {
		const full = path.join(dir, name);
		return fs.statSync(full).isFile() && exts.indexOf(path.extname(name).toLowerCase()) >= 0;
	}).sort().map(function(name) { return path.join(dir, name); });
}

function workDirs(args) {
	const work = path.resolve(args.dir || path.join(REPO_ROOT, "libwork"));
	const out = path.join(REPO_ROOT, "build", "libwork");
	return {
		work : work,
		src : path.join(work, "src"),
		lib : path.join(work, "lib"),
		keys : path.join(work, "keys"),
		out : out,
		pack : path.join(out, "pack"),
		unpack : path.join(out, "unpack")
	};
}

function printChildOutput(text) {
	text.trimEnd().split("\n").forEach(function(line) {
		console.log("  " + line);
	});
}

function runChild(cmd, file, extraArgs) {
	console.log("");
	console.log("── " + path.basename(file));
	const r = runSingle(cmd, [file].concat(extraArgs || []));
	printChildOutput(r.out);
	return r;
}

function batchSummary(title, dirName, ok, total, worst, extra) {
	console.log("");
	console.log(title + "：" + ok + "/" + total + " 个（输出目录 " + dirName + "）" + (extra || ""));
	return worst;
}

function ensureWorkspaceDirs(dirs) {
	// 目录约定见 libwork/README.md；空目录不入库，这里按需创建
	ensureDir(dirs.work);
	ensureDir(dirs.src);
	ensureDir(dirs.lib);
	ensureDir(dirs.keys);
}

function cmdPackBatch(args, dirs) {
	ensureWorkspaceDirs(dirs);
	// 未指定目标 = 批量，不参与密钥
	if (args.sign !== undefined || args.key !== undefined) {
		console.log("注意：未指定目标（批量模式）不参与密钥，已忽略 --sign/--key");
	}
	const files = listFiles(dirs.src, [".json", ".js"]);
	if (!files.length) {
		console.log("批量打包：" + dirs.src + " 里没有 .json/.js 源码，跳过");
		return 0;
	}
	ensureDir(dirs.pack);
	console.log("批量打包：" + files.length + " 个源码 → " + dirs.pack);
	let worst = 0, ok = 0;
	files.forEach(function(file) {
		const r = runChild("pack", file, ["-o", dirs.pack]);
		if (r.code) worst = Math.max(worst, r.code); else ok++;
	});
	return batchSummary("批量打包完成", dirs.pack, ok, files.length, worst);
}

function cmdPackDirTarget(dir, args, dirs) {
	const spec = resolveKeySpec(args, dirs, "pack");
	const signPath = spec.type === "file" ? spec.file : null;
	const files = listFiles(dir, [".json", ".js"]);
	if (!files.length) {
		console.log("目录里没有 .json/.js 文件：" + dir);
		return 0;
	}
	ensureDir(dirs.pack);
	console.log("打包目录：" + files.length + " 个文件 → " + dirs.pack);
	let worst = 0, ok = 0;
	files.forEach(function(file) {
		const extra = ["-o", dirs.pack];
		if (signPath) extra.push("--sign", signPath);
		const r = runChild("pack", file, extra);
		if (r.code) worst = Math.max(worst, r.code); else ok++;
	});
	return batchSummary("打包目录完成", dirs.pack, ok, files.length, worst);
}

function cmdUnpackBatch(args, dirs) {
	ensureWorkspaceDirs(dirs);
	// 未指定目标 = 批量，不参与密钥
	if (args.key !== undefined || args.sign !== undefined) {
		console.log("注意：未指定目标（批量模式）不参与密钥，已忽略 --sign/--key");
	}
	const files = listFiles(dirs.lib, [".lib"]);
	if (!files.length) {
		console.log("批量解包：" + dirs.lib + " 里没有 .lib，跳过");
		return 0;
	}
	ensureDir(dirs.unpack);
	console.log("批量解包：" + files.length + " 个包 → " + dirs.unpack);
	let worst = 0, ok = 0;
	files.forEach(function(file) {
		const r = runChild("unpack", file, ["-o", dirs.unpack, "--no-verify"]);
		if (r.code) worst = Math.max(worst, r.code); else ok++;
	});
	return batchSummary("批量解包完成", dirs.unpack, ok, files.length, worst);
}

function cmdUnpackDirTarget(dir, args, dirs) {
	const spec = resolveKeySpec(args, dirs, "unpack");
	const files = listFiles(dir, [".lib"]);
	if (!files.length) {
		console.log("目录里没有 .lib 文件：" + dir);
		return 0;
	}
	ensureDir(dirs.unpack);
	console.log("解包目录：" + files.length + " 个包 → " + dirs.unpack);
	const baseExtra = ["-o", dirs.unpack, "-d", dirs.work];
	if (spec.type === "file") baseExtra.push("--key", spec.file);
	else if (spec.type === "traversal") baseExtra.push("--key-dir", spec.dirs[1]);
	else if (spec.type === "none") baseExtra.push("--no-verify");
	let worst = 0, ok = 0, verifyFailed = 0;
	files.forEach(function(file) {
		const r = runChild("unpack", file, baseExtra);
		if (r.code === 2) verifyFailed++;
		if (r.code) worst = Math.max(worst, r.code); else ok++;
	});
	return batchSummary("解包目录完成", dirs.unpack, ok, files.length, worst,
		verifyFailed ? "，验签失败 " + verifyFailed + " 个" : "");
}

const argv = process.argv.slice(2);
const command = argv[0];
const args = parseArgs(argv.slice(1));

let exitCode = 0;
if (!command || command === "-h" || command === "--help" || args.help) {
	usage();
} else if (command === "pack") {
	exitCode = cmdPack(args) || 0;
} else if (command === "unpack") {
	exitCode = cmdUnpack(args) || 0;
} else if (command === "info") {
	exitCode = cmdInfo(args) || 0;
} else {
	fail("未知命令：" + command + "\n\n" + usage.call(null));
}
if (exitCode) process.exit(exitCode);
