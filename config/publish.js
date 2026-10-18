/**
 * 发布（publish）配置。
 *
 * method = "local"：把产物复制到 destPath
 * method = "sftp" ：通过 SFTP 上传到 remoteRoot，sshConfig 是 ssh2.Client.connect 的参数
 *
 * 需要多套发布目标时，可以在这里加「名字 -> 配置文件名」的映射，运行时用
 * `main.js publish <名字>` 选择，例如：
 *   module.exports = { method: "local", destPath: "./build/dist", sftp: "publish.sftp" };
 * 然后另建 config/publish.sftp.js。
 */
module.exports = {
	method : "local",
	destPath : "./build/dist",

	// method = "sftp" 时需要
	remoteRoot : null,
	sshConfig : null
};
