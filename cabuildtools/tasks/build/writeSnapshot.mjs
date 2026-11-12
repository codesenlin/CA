import fs from "node:fs";
import zlib from "node:zlib";
export default function (context, args) {
	var name = context.buildConfig.variants + ".js";
	fs.writeFileSync("./build/outputs/snapshot/" + name, zlib.gzipSync(Buffer.from(args[0])));
	return args[0];
}