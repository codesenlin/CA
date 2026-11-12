export default function (context, args) {
	return args[0].replace(/AndroidBridge\.HOTFIX/g, "true");
}