import fs from "node:fs";
import path from "node:path";
import crc32 from "../../lib/crc32.mjs";
function crc32wrap(buffer) {
	return crc32(0, buffer, buffer.length, 0);
}
export default function (context, args) {
	var dexPath = path.resolve(context.shellConfig.shellPath, context.shellConfig.dexPath);
	var crc = crc32wrap(fs.readFileSync(dexPath)).toString(16);
	context.dexCrc = crc;
	return args[0].replace(/\$dexCrc\$/g, crc);
}