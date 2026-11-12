import ssh2 from "ssh2";
export default function (context, args) {
	return new Promise(function (resolve, reject) {
		var sftp = args.session.sftp;
		sftp.fastPut(args.localPath, args.remotePath, function (err) {
			if (err) {
				reject(err);
				return;
			}
			resolve();
		});
	});
}