import publish from "../../lib/publish.mjs";

export default function (context, args) {
	publish.log(args[1].remotePath);
	return args[0].upload(context, args[1])
		.then(() => args[0]);
}