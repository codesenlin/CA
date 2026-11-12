import fs from "node:fs";
export default function (context, args) {
	fs.writeFileSync("./build/outputs/hotfix/release.js", args[0]);
	return args[0];
}