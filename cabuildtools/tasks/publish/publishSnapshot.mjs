import publish from "../../lib/publish.mjs";

export default function (context) {
	return publish.files(context, [{
		localPath: "./build/outputs/snapshot/snapshot.json",
		remotePath: "snapshot.json"
	}, {
		localPath: "./build/outputs/snapshot/snapshot.js",
		remotePath: "snapshot.js"
	}]);
}
export const input = "cli";