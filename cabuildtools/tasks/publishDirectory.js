const fs = require("fs");
const path = require("path");

module.exports = function(context, args) {
	var operations = prepareUpload([], args[1].localPath, args[1].remotePath);
	if (args[0].enableAsyncUpload) {
		return Promise.all(operations.map(e => {
			console.log("Publishing " + e.remotePath);
			return args[0].upload(context, e);
		})).then(() => args[0]);
	} else {
		return Promise.resolve(0)
			.then(function executor(index) {
				if (index < operations.length) {
					console.log("Publishing " + operations[index].remotePath);
					return args[0].upload(context, operations[index])
						.then(e => executor(index + 1));
				} else {
					return args[0];
				}
			})
	}
}

// readdirSync 的 recursive 选项（Node 20.1+）直接给出所有层级的条目，
// 省掉手写递归；parentPath 是它所在目录的绝对路径。
function prepareUpload(operations, localPath, remotePath) {
	fs.readdirSync(localPath, { recursive : true, withFileTypes : true }).forEach(function(entry) {
		if (entry.isDirectory()) return;
		var relative = path.relative(localPath, path.join(entry.parentPath, entry.name));
		operations.push({
			localPath : path.join(localPath, relative),
			remotePath : remotePath + "/" + relative.split(path.sep).join("/")
		});
	});
	return operations;
}
