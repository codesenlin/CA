import path from "node:path";
import { createRequire } from "node:module";
const require = createRequire(import.meta.url);

export default function (context, args) {
	context.updateConfig = require(path.resolve("./config/update-sources.json"));
}
