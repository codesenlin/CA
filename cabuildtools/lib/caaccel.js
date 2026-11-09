/**
 * caaccel（加速库）的构建 / 测试 / 基准脚本。
 *
 *   node ./cabuildtools/main.js caaccel build   # 构建 build/caaccel/caaccel.jar
 *   node ./cabuildtools/main.js caaccel test    # 跑 caaccel/test 下的单测
 *   node ./cabuildtools/main.js caaccel bench   # matchString 的 Java vs Rhino 基准
 *
 * 原来对应 caaccel/build.sh、test.sh、bench.sh 三个 shell 脚本，项目是 Node，
 * 统一成这一个入口，也方便挂 npm 脚本。
 */
const fs = require("fs");
const path = require("path");
const { spawnSync } = require("child_process");

const ROOT = path.resolve(__dirname, "../..");       // CA 根目录
const CAACCEL_ROOT = path.join(ROOT, "caaccel");     // 加速库源码 / 测试 / 基准
const BUILD = path.join(ROOT, "build", "caaccel");   // 统一输出到 CA/build/caaccel

/** 删目录/文件（兼容没有 fs.rmSync 的旧 Node） */
function rm(target) {
	if (fs.rmSync) return fs.rmSync(target, { recursive: true, force: true });
	if (!fs.existsSync(target)) return;
	if (fs.statSync(target).isDirectory()) fs.rmdirSync(target, { recursive: true });
	else fs.unlinkSync(target);
}

function log(msg) {
	console.log("[caaccel] " + msg);
}

function fail(msg) {
	console.error("[caaccel] 失败：" + msg);
	process.exit(1);
}

/** 找 JDK：优先 JAVA_HOME/bin，其次 PATH */
function findJdk() {
	const home = process.env.JAVA_HOME;
	if (home) {
		const bin = path.join(home, "bin");
		const javac = path.join(bin, process.platform === "win32" ? "javac.exe" : "javac");
		if (fs.existsSync(javac)) {
			return {
				javac: javac,
				jar: path.join(bin, process.platform === "win32" ? "jar.exe" : "jar"),
				java: path.join(bin, process.platform === "win32" ? "java.exe" : "java")
			};
		}
	}
	return { javac: "javac", jar: "jar", java: "java" };
}

/** 执行命令，失败直接退出（和 shell 里 set -e 一致） */
function run(command, args, options) {
	log("$ " + path.basename(command) + " " + args.join(" "));
	const result = spawnSync(command, args, Object.assign({ stdio: "inherit", cwd: CAACCEL_ROOT }, options));
	if (result.error) fail(result.error.message);
	if (result.status !== 0) fail(path.basename(command) + " 退出码 " + result.status);
}

/** 收集所有 .java 源文件 */
function listJava(dir) {
	if (!fs.existsSync(dir)) return [];
	// fs.globSync 是 Node 22 起的内置实现，省掉手写递归
	return fs.globSync("**/*.java", { cwd: dir })
		.map(function (rel) {
			return path.join(dir, rel);
		})
		.sort();
}

/** 构建 caaccel.jar：纯 javac + jar，不依赖 Gradle/Android */
function build(quiet) {
	const jdk = findJdk();
	const classes = path.join(BUILD, "classes");
	const outJar = path.join(BUILD, "caaccel.jar");

	rm(classes);
	rm(outJar);
	fs.mkdirSync(classes, { recursive: true });

	const sources = listJava(path.join(CAACCEL_ROOT, "src"));
	if (!sources.length) fail("src/ 下没有找到 .java 文件");
	const listFile = path.join(BUILD, "sources.txt");
	fs.writeFileSync(listFile, sources.join("\n") + "\n");

	if (!quiet) log("编译 " + sources.length + " 个源文件");
	run(jdk.javac, ["-source", "8", "-target", "8", "-encoding", "UTF-8", "-d", classes, "@" + listFile],
		quiet ? { stdio: "ignore" } : undefined);
	run(jdk.jar, ["cf", outJar, "-C", classes, "."], quiet ? { stdio: "ignore" } : undefined);
	if (!quiet) log("built: " + outJar + " (" + fs.statSync(outJar).size + " 字节)");
	return { jdk, outJar };
}

/**
 * 在 Gradle 缓存里找 Rhino jar。
 * cadroid 已从 rhino-runtime 1.7.15.1 换到完整版 org.mozilla:rhino:1.9.2
 * （rhino-runtime 1.8+ 停更；完整版才带 E4X/xml 支持），这里优先找新 jar。
 */
function findRhino() {
	const targets = ["rhino-1.9.2.jar", "rhino-1.9.1.jar"];
	const gradleCaches = path.join(process.env.HOME || process.env.USERPROFILE || "", ".gradle", "caches");
	const found = {};
	(function walk(dir, depth) {
		if (depth > 8 || !fs.existsSync(dir)) return;
		let entries;
		try {
			entries = fs.readdirSync(dir, { withFileTypes: true });
		} catch (e) {
			return;
		}
		entries.forEach(function (entry) {
			const full = path.join(dir, entry.name);
			if (entry.isDirectory()) walk(full, depth + 1);
			else if (targets.indexOf(entry.name) >= 0 && !found[entry.name]) found[entry.name] = full;
		});
	})(gradleCaches, 0);
	for (let i = 0; i < targets.length; i++) {
		if (found[targets[i]]) return found[targets[i]];
	}

	for (let i = 0; i < targets.length; i++) {
		const candidate = path.join(CAACCEL_ROOT, "test", targets[i]);
		if (fs.existsSync(candidate)) return candidate;
	}
	fail("找不到 Rhino jar（rhino-1.9.2.jar）：先跑一次 cadroid 的构建（会下载到 Gradle 缓存），或放一份到 caaccel/test/rhino-1.9.2.jar");
}

/** 跑单测 */
function test() {
	const { jdk, outJar } = build(true);
	const classes = path.join(BUILD, "test-classes");
	rm(classes);
	fs.mkdirSync(classes, { recursive: true });

	const sources = listJava(path.join(CAACCEL_ROOT, "test"));
	if (!sources.length) fail("test/ 下没有找到 .java 文件");
	const listFile = path.join(BUILD, "test-sources.txt");
	fs.writeFileSync(listFile, sources.join("\n") + "\n");
	run(jdk.javac, ["-encoding", "UTF-8", "-cp", outJar, "-d", classes, "@" + listFile]);
	// 逐个跑 test/ 下的 main，新增测试文件不需要再改这里
	sources.map(function (file) { return path.basename(file, ".java"); }).forEach(function (name) {
		run(jdk.java, ["-cp", outJar + path.delimiter + classes, name]);
	});
}

/** 跑基准 */
function bench() {
	const rhino = findRhino();
	const { jdk, outJar } = build(true);
	const classes = path.join(BUILD, "bench-classes");
	rm(classes);
	fs.mkdirSync(classes, { recursive: true });

	const cp = [outJar, rhino].join(path.delimiter);
	run(jdk.javac, ["-encoding", "UTF-8", "-cp", cp, "-d", classes, path.join(CAACCEL_ROOT, "bench", "BenchMatchString.java")]);
	run(jdk.java, ["-cp", cp + path.delimiter + classes, "BenchMatchString"]);
}

module.exports = { build, test, bench };
