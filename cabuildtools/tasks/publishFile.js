const publish = require("../publish");

module.exports = function(context, args) {
	publish.log(args[1].remotePath);
	return args[0].upload(context, args[1])
		.then(() => args[0]);
}