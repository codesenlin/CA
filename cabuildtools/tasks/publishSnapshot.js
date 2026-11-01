const publish = require("../publish");

module.exports = function (context) {
	return publish.files(context, [{
		localPath: "./build/outputs/snapshot/snapshot.json",
		remotePath: "snapshot.json"
	}, {
		localPath: "./build/outputs/snapshot/snapshot.js",
		remotePath: "snapshot.js"
	}]);
}
module.exports.input = "cli";