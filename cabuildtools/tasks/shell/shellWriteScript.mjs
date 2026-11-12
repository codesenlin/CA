import fs from "node:fs";
import path from "node:path";
export default function (context, args) {
	var script = path.resolve(context.shellConfig.shellPath, "./app/src/main/assets/script.js");
	fs.writeFileSync(script, args[0]);
}