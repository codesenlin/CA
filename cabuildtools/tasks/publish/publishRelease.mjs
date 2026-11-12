import publish from "../../lib/publish.mjs";

export default function (context) {
	return publish.files(context, [{
		localPath: "./build/outputs/release/update.json",
		remotePath: "update.json"
	}]);
}
export const input = "cli";