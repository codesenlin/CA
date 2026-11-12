import path from "node:path";
import fs from "node:fs";
import { createRequire } from "node:module";
const require = createRequire(import.meta.url);
export default function (context, args) {
	var config = require(path.resolve("./config.js"));
	return context.execute("preparePublish")
		.then(context.pipe("publishDirectory", {
			localPath: config.outputPath,
			remotePath: "."
		}))
		.then(context.pipe("endPublish"));
}
export const input = "cli";
