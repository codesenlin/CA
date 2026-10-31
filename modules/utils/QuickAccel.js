MapScript.loadModule("QuickAccel", (function () {
	// QuickJS 侧载加速（Rhino 主引擎下的纯 JS 计算 worker）
	// 目的：验证把纯计算迁移到 QuickJS worker 的收益。
	var r = {};
	r.available = false;
	try {
		r.available = !!ScriptInterface.quickWorkerAvailable();
	} catch (e) {
		r.available = false;
	}

	if (r.available) {
		android.util.Log.i("Script", "QuickAccel: QuickJS worker 可用（quickjs-kt 16KB native）");
	}

	// 基准函数（纯 JS、无 Java 依赖；结果要稳定便于校验）
	r.benchSource =
		"function bench(n) {" +
			"var a = [], i, s = 0;" +
			"for (i = 0; i < n; i++) a.push((i * 2654435761) % 1000003);" +
			"a.sort(function (x, y) { return x - y; });" +
			"for (i = 0; i < a.length; i++) s = (s + a[i] * (i + 1)) % 1000000007;" +
			"return s;" +
		"}";

	r.runBench = function (n) {
		try {
			if (!r.available) {
				android.util.Log.i("Script", "QuickAccel: worker 不可用，跳过");
				return;
			}
			var rhinoFn = eval("(" + r.benchSource + ")");
			var t0 = Date.now();
			var rhinoResult = rhinoFn(n);
			var rhinoMs = Date.now() - t0;

			var ok = ScriptInterface.quickWorkerRegister(r.benchSource);
			var t1 = Date.now();
			var quickResult = ScriptInterface.quickWorkerCall("bench", JSON.stringify([n]));
			var quickMs = Date.now() - t1;

			android.util.Log.i("Script", "QuickAccel bench n=" + n +
				" rhino=" + rhinoMs + "ms quickjs=" + quickMs + "ms" +
				" regOk=" + ok + " same=" + (String(rhinoResult) === String(quickResult)) +
				" quickResult=" + quickResult);
		} catch (e) {
			android.util.Log.w("Script", "QuickAccel 基准失败: " + e + " | " + (e.stack || ""));
		}
	};


	// ---------------- 命令名匹配（真实热点：每次输入都跑） ----------------
	// worker 里缓存 command_snap（只同步一次），之后每次只传前缀、回传匹配数组
	r.matchSource =
		"function matchCommands(prefix) {" +
			"var t = globalThis.__commandSnap, out = [], k;" +
			"if (!t) return [];" +
			"for (k in t) {" +
				"if (k.indexOf(prefix) >= 0 || String(t[k]).indexOf(prefix) >= 0) out.push(k);" +
			"}" +
			"out.sort();" +
			"return out;" +
		"}";

	r.registered = false;
	r.snapSent = false;

	r.ensureLibrary = function (snap) {
		if (!r.available || !snap) return false;
		if (!r.registered) {
			r.registered = ScriptInterface.quickWorkerRegister(r.matchSource);
			if (!r.registered) return false;
		}
		if (!r.snapSent) {
			var t0 = Date.now();
			var ok = ScriptInterface.quickWorkerSetVar("__commandSnap", JSON.stringify(snap));
			r.snapSent = ok;
			android.util.Log.i("Script", "QuickAccel: 命令库同步到 worker " + (Date.now() - t0) + "ms ok=" + ok);
		}
		return r.registered && r.snapSent;
	};

	// 返回匹配命令名数组；worker 不可用/失败时返回 null（调用方回退原 JS 实现）
	r.matchCommandNames = function (snap, prefix) {
		try {
			if (!r.available || !r.ensureLibrary(snap)) return null;
			var res = ScriptInterface.quickWorkerCall("matchCommands", JSON.stringify([prefix]));
			return res ? JSON.parse(res) : null;
		} catch (e) {
			android.util.Log.w("Script", "QuickAccel.matchCommandNames 失败: " + e);
			return null;
		}
	};

	// 匹配基准：Rhino 直接 filter vs worker
	r.runMatchBench = function (n, prefix) {
		try {
			var snap = {}, i;
			for (i = 0; i < n; i++) {
				snap["cmd" + i] = "参数说明 " + i + " description text for command " + i;
			}
			var t0 = Date.now();
			var rhinoNames = Object.keys(snap).filter(function (e, i, a) {
				return e.indexOf(prefix) >= 0 || snap[e].indexOf(prefix) >= 0;
			}).sort();
			var rhinoMs = Date.now() - t0;

			r.snapSent = false;
			r.registered = false;
			var t1 = Date.now();
			var quickNames = r.matchCommandNames(snap, prefix);
			var quickMs = Date.now() - t1;
			android.util.Log.i("Script", "QuickAccel match n=" + n + " rhino=" + rhinoMs +
				"ms quickjs=" + quickMs + "ms same=" + (quickNames && quickNames.length === rhinoNames.length && quickNames[0] === rhinoNames[0]));
		} catch (e) {
			android.util.Log.w("Script", "QuickAccel match 基准失败: " + e);
		}
	};

	// 基准不自动跑（Rhino 侧 n=100000 要 ~3.9s，会拖慢启动）；调试时手动调
	return r;
})());
