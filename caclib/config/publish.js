/**
 * clib 的发布方式（preparePublish 读取）。
 *
 * 更完整的发布配置见 CA 仓库根目录的 config/publish.js；这里通常只需要 method。
 * method = "local" 把产物复制到 destPath；= "sftp" 通过 SFTP 上传。
 */
module.exports = {
	"method": "local"
};
