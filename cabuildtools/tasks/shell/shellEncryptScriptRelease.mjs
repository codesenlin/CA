import path from "node:path";
import signscript from "../../lib/signscript.mjs";
export default function (context, args) {
	var signPath = path.resolve(context.shellConfig.shellPath, context.shellConfig.releaseSignPath);
	return signscript(args[0], signPath);
}