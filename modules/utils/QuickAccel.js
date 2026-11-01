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

	// 其余无 jar 回退用的纯计算函数。数据由 quickWorkerSetVar 缓存一次，
	// 每次只传前缀，回传 flags / 掩码。
	r.accelSource = r.matchSource + "\n" + [
		"function classifyCached(ps) {",
		"  var g = globalThis, keys = g.__classifyKeys, descs = g.__classifyDescs;",
		"  var p = String(ps), out = [], i, k, d;",
		"  if (!keys) return out;",
		"  for (i = 0; i < keys.length; i++) {",
		"    k = keys[i];",
		"    if (p == k || p.indexOf(k + ' ') == 0) { out.push(1); continue; }",
		"    d = (descs && i < descs.length && descs[i]) ? descs[i] : '';",
		"    out.push((k.indexOf(p) >= 0 || d.indexOf(p) >= 0) ? 2 : 0);",
		"  }",
		"  return out;",
		"}",
		"function classifyArrayCached(ps) {",
		"  var g = globalThis, arr = g.__classifyArray, p = String(ps);",
		"  var flags = [], length = -1, canFinish = false, i, e;",
		"  if (!arr) return { flags: flags, length: length, canFinish: canFinish };",
		"  for (i = 0; i < arr.length; i++) {",
		"    e = arr[i];",
		"    if (e == null) { flags.push(0); continue; }",
		"    if (p == e || p.indexOf(e + ' ') == 0) {",
		"      flags.push(1); canFinish = true; length = Math.max(length, e.length);",
		"    } else if (e.indexOf(p) == 0) {",
		"      flags.push(2); length = Math.max(length, p.length);",
		"    } else {",
		"      flags.push(0);",
		"    }",
		"  }",
		"  return { flags: flags, length: length, canFinish: canFinish };",
		"}",
		"function filterTextsCached(ps) {",
		"  var g = globalThis, a = g.__filterTexts, p = String(ps), out = [], i;",
		"  if (!a) return out;",
		"  for (i = 0; i < a.length; i++) out.push(a[i] != null && String(a[i]).indexOf(p) >= 0);",
		"  return out;",
		"}"
	].join("\n");

	r.registered = false;
	r.snapSent = false;
	r.classifyKeysRef = r.classifyDescsRef = null;
	r.classifyArrayRef = null;
	r.filterTextsRef = null;

	r.ensureWorker = function () {
		if (!r.available) return false;
		if (!r.registered) {
			r.registered = ScriptInterface.quickWorkerRegister(r.accelSource);
			if (!r.registered) android.util.Log.w("Script", "QuickAccel worker 注册失败");
		}
		return !!r.registered;
	};

	r.syncVar = function (name, value, refKey) {
		try {
			var json = JSON.stringify(value);
			var ok = ScriptInterface.quickWorkerSetVar(name, json);
			if (ok) r[refKey] = value;
			else android.util.Log.w("Script", "QuickAccel syncVar 失败: " + name + " len=" + json.length);
			return ok;
		} catch (e) {
			android.util.Log.w("Script", "QuickAccel syncVar 异常: " + name + " => " + e);
			return false;
		}
	};

	r.classify = function (keys, descs, ps) {
		if (!r.available || !keys || !r.ensureWorker()) return null;
		try {
			if (keys !== r.classifyKeysRef || descs !== r.classifyDescsRef) {
				if (!r.syncVar("__classifyKeys", keys, "classifyKeysRef")) return null;
				if (!r.syncVar("__classifyDescs", descs || [], "classifyDescsRef")) return null;
			}
			var res = ScriptInterface.quickWorkerCall("classifyCached", JSON.stringify([ps]));
			if (!res) android.util.Log.w("Script", "QuickAccel classifyCached 返回 null");
			return res ? JSON.parse(res) : null;
		} catch (e) {
			android.util.Log.w("Script", "QuickAccel classifyCached 异常: " + e);
			return null;
		}
	};

	r.classifyArray = function (arr, ps) {
		if (!r.available || !arr || !r.ensureWorker()) return null;
		try {
			if (arr !== r.classifyArrayRef) {
				if (!r.syncVar("__classifyArray", arr, "classifyArrayRef")) return null;
			}
			var res = ScriptInterface.quickWorkerCall("classifyArrayCached", JSON.stringify([ps]));
			if (!res) android.util.Log.w("Script", "QuickAccel classifyArrayCached 返回 null");
			return res ? JSON.parse(res) : null;
		} catch (e) {
			android.util.Log.w("Script", "QuickAccel classifyArrayCached 异常: " + e);
			return null;
		}
	};

	r.filterTexts = function (texts, ps) {
		if (!r.available || !texts || !r.ensureWorker()) return null;
		try {
			if (texts !== r.filterTextsRef) {
				if (!r.syncVar("__filterTexts", texts, "filterTextsRef")) return null;
			}
			var res = ScriptInterface.quickWorkerCall("filterTextsCached", JSON.stringify([ps]));
			if (!res) android.util.Log.w("Script", "QuickAccel filterTextsCached 返回 null");
			return res ? JSON.parse(res) : null;
		} catch (e) {
			android.util.Log.w("Script", "QuickAccel filterTextsCached 异常: " + e);
			return null;
		}
	};

	r.ensureLibrary = function (snap) {
		if (!r.available || !snap) return false;
		if (!r.ensureWorker()) return false;
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
