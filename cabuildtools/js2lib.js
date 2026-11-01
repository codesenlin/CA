var fs = require("fs");
var process = require("process");
var libformat = require("./libformat");

function main(src, dest) {
	var s = src instanceof Buffer ? src : fs.readFileSync(src);
	var o = libformat.packLibrary(s);
	if (dest) fs.writeFileSync(dest, o);
	return o;
}

if (require.main == module) main(process.argv[2], process.argv[3]);
module.exports = main;
