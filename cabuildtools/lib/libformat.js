const crypto = require("crypto");
const zlib = require("zlib");

const MAGIC_LIBRARY = "LIBRARY";
const MAGIC_SIGNED = "SIGNLIB01";
const SIGN_LEN = 256;

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

module.exports = {
	MAGIC_LIBRARY: MAGIC_LIBRARY,
	MAGIC_SIGNED: MAGIC_SIGNED,
	SIGN_LEN: SIGN_LEN,
	gzip: gzip,
	writeStamp: writeStamp,
	readStamp: readStamp,
	packLibrary: packLibrary,
	packSigned: packSigned
};
