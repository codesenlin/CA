var fs = require("fs");
var process = require("process");
var libformat = require("./libformat");

function main(src, prvkey, dest) {
	var s = src instanceof Buffer ? src : fs.readFileSync(src);
	var o = libformat.packSigned(s, fs.readFileSync(prvkey));
	if (dest) fs.writeFileSync(dest, o);
	return o;
}

if (require.main == module) main(process.argv[2], process.argv[3], process.argv[4]);
module.exports = main;
