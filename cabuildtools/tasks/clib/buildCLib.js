const clibspawn = require("../../lib/clibspawn");
module.exports = function (context, args) {
	return clibspawn();
}
module.exports.input = "cli";