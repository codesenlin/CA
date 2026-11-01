const publish = require("../publish");

module.exports = function (context) {
	return publish.files(context, [{
		localPath: "./build/outputs/hotfix/release.js",
		remotePath: "hotfix/release.js"
	}, {
		localPath: "./build/outputs/hotfix/release.sign",
		remotePath: "hotfix/release.sign"
	}, {
		localPath: "./build/outputs/hotfix/hotfix.json",
		remotePath: "hotfix.json"
	}]);
}
module.exports.input = "cli";