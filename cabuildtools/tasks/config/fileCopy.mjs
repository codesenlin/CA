import fs from "node:fs";
import path from "node:path";
export default function (context, args) {
	return new Promise(function (resolve, reject) {
		fs.mkdirSync(path.dirname(args.dest), { recursive: true });
		fs.copyFile(args.src, args.dest, function (err) {
			if (err) {
				reject(err);
			} else {
				resolve();
			}
		});
	});
}