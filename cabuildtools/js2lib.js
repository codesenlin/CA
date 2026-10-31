var fs = require("fs");
var process = require("process");
var zlib = require("zlib");

function main(src, dest) {
	var s, o;
	var dh = Buffer.alloc(15), date = Date.now();
	s = src instanceof Buffer ? src : fs.readFileSync(src);
	dh.write("LIBRARY");
	// 8 字节大端 int64（毫秒）；旧写法用 0xffffffff 拆分 + 有符号 int32，日期会偏 ~49 天
	dh.writeBigInt64BE(BigInt(date), 7);
	s = zlib.gzipSync(s);
	o = Buffer.alloc(dh.length + s.length);
	dh.copy(o, 0);
	s.copy(o, dh.length);
	if (dest) fs.writeFileSync(dest, o);
	return o;
}

if (require.main == module) main(process.argv[2], process.argv[3]);
module.exports = main;