import fs from "node:fs";
import process from "node:process";
import { pathToFileURL } from "node:url";
import libformat from "./libformat.mjs";

function main(src, prvkey, dest) {
	var s = src instanceof Buffer ? src : fs.readFileSync(src);
	var o = libformat.packSigned(s, fs.readFileSync(prvkey));
	if (dest) fs.writeFileSync(dest, o);
	return o;
}

if (import.meta.url === pathToFileURL(process.argv[1]).href) main(process.argv[2], process.argv[3], process.argv[4]);
export default main;
