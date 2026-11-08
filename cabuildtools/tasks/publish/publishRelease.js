const publish = require("../../lib/publish");

module.exports = function (context) {
	return publish.files(context, [{
		localPath: "./build/outputs/release/update.json",
		remotePath: "update.json"
	}]);
}
module.exports.input = "cli";