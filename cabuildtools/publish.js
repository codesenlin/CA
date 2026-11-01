/**
 * 发布任务的公共流程：日志、文件列表上传、异步/串行切换。
 * 具体 SFTP/本地实现仍由 preparePublish 返回的 owner 提供。
 */
function log(remotePath) {
	console.log("Publishing " + remotePath);
}

/** preparePublish → 依次 publishFile → endPublish */
function files(context, list) {
	var chain = context.execute("preparePublish");
	list.forEach(function (e) {
		chain = chain.then(context.pipe("publishFile", {
			localPath: e.localPath,
			remotePath: e.remotePath
		}));
	});
	return chain.then(context.pipe("endPublish"));
}

/** 上传一组 operations，返回 owner；enableAsyncUpload 时并发，否则串行 */
function uploadOperations(context, owner, operations) {
	function upload(e) {
		log(e.remotePath);
		return owner.upload(context, e);
	}
	if (owner.enableAsyncUpload) {
		return Promise.all(operations.map(upload)).then(function () {
			return owner;
		});
	}
	return operations.reduce(function (chain, e) {
		return chain.then(function () {
			return upload(e);
		});
	}, Promise.resolve()).then(function () {
		return owner;
	});
}

module.exports = {
	log: log,
	files: files,
	uploadOperations: uploadOperations
};
