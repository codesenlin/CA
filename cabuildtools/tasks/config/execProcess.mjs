import process from "node:process";
import child_process from "node:child_process";
export default function (context, args) {
	return new Promise(function (resolve, reject) {
		var child = child_process.spawn(args.command, args.args, args);
		if (args.input) {
			child.stdin.end(args.input);
		}
		child.on("exit", function (code, signal) {
			if (code != 0) {
				reject(new Error("Process returns " + code));
			} else {
				resolve();
			}
		});
		child.on("error", function (err) {
			reject(err);
		});
	});
}