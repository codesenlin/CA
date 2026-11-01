/*
 * 高性能加速库的 JS 侧封装。
 *
 * 约定：
 *   Accel.available()           —— 返回当前是否加载了 caaccel.jar
 *   Accel.xxx(...)              —— 有 jar 走 Java，没有走这里的等价实现
 *
 * jar 通过 Android 侧 libs/ 引入，Rhino 里用 Packages 直接访问；
 * 纯 JS 环境（比如 node 构建脚本）里 Packages 不存在，自动回退。
 */
MapScript.loadModule("Accel", (function() {
	var impl = null;
	var idTextsCache = null;
	var lastIDTexts = null;
	var filterTextsCache = null;
	var commandKeys = null, commandDescs = null, commandSnapRef = null;
	var matchSourceRef = null, matchCacheSupported = true;
	function jsMatchCommandNames(prefix) {
		if (commandKeys == null) return null;
		var p = String(prefix), out = [], i, d;
		for (i = 0; i < commandKeys.length; i++) {
			d = (commandDescs && i < commandDescs.length && commandDescs[i]) ? commandDescs[i] : "";
			if (commandKeys[i].indexOf(p) >= 0 || d.indexOf(p) >= 0) out.push(commandKeys[i]);
		}
		out.sort();
		return out;
	}
	function syncCommandSnapshot(snap) {
		if (snap === commandSnapRef || !(snap instanceof Object)) return;
		var ks = Object.keys(snap), ds = [], i;
		for (i = 0; i < ks.length; i++) ds.push(String(snap[ks[i]] == null ? "" : snap[ks[i]]));
		commandKeys = ks;
		commandDescs = ds;
		commandSnapRef = snap;
		if (impl != null) {
			try { impl.setCommandSnapshot(ks, ds); } catch(e) { /* 老 jar */ }
		}
	}
	try {
		impl = Packages.top.projectxero.ca.accel.Accel;
		if (impl && !impl.version()) impl = null;
	} catch (e) {
		impl = null;
	}

	function jsMatchString(ps, a, r) {
		var t, t2, t3;
		if (!(r instanceof Object)) r = {};
		if (!Array.isArray(r.input)) r.input = [];
		if (!(r.output instanceof Object)) r.output = {};
		if (Array.isArray(a)) {
			t = [];
			a.forEach(function(e) {
				if (e.indexOf(ps) < 0) return;
				r.output[e] = e;
				if (r.input.indexOf(e) < 0) t.push(e);
			});
			t.sort();
			r.input = r.input.concat(t);
		} else {
			t = []; t2 = [];
			Object.keys(a).forEach(function(e) {
				if (e.indexOf(ps) < 0 && a[e].indexOf(ps) < 0) return;
				if (a[e]) {
					t3 = e + " - " + a[e];
					r.output[t3] = e;
					if (r.input.indexOf(t3) < 0) t.push(t3);
				} else {
					r.output[e] = e;
					if (r.input.indexOf(e) < 0) t2.push(e);
				}
			});
			t.sort(); t2.sort();
			r.input = r.input.concat(t, t2);
		}
		return r;
	}

	function syncMatchSource(a) {
		if (a === matchSourceRef) return;
		if (Array.isArray(a)) {
			impl.setMatchSource(a, null, false);
		} else if (a instanceof Object) {
			var ks = Object.keys(a), ds = [], i;
			for (i = 0; i < ks.length; i++) ds.push(String(a[ks[i]] == null ? "" : a[ks[i]]));
			impl.setMatchSource(ks, ds, true);
		} else {
			throw new Error("unsupported match source");
		}
		matchSourceRef = a;
	}

	function merge(r, res) {
		if (!(r instanceof Object)) r = {};
		if (!Array.isArray(r.input)) r.input = [];
		if (!(r.output instanceof Object)) r.output = {};
		var keys, vals, i, v, ins;
		if (typeof res.outputKeys === "function") {
			// 新 jar：一次取 keys/values 数组，避免逐项 Map.get 穿 Rhino
			keys = res.outputKeys();
			vals = res.outputValues();
			for (i = 0; i < keys.length; i++) r.output[String(keys[i])] = String(vals[i]);
			ins = res.inputArray();
		} else {
			var outs = res.outputs;
			keys = outs.keySet().toArray();
			for (i = 0; i < keys.length; i++) r.output[String(keys[i])] = String(outs.get(keys[i]));
			ins = res.inputs.toArray();
		}
		// 用 seen 去重；原实现每个结果都 indexOf 一次，结果上千条时是 O(n²)
		var seen = Object.create(null);
		for (i = 0; i < r.input.length; i++) seen[r.input[i]] = true;
		for (i = 0; i < ins.length; i++) {
			v = String(ins[i]);
			if (!seen[v]) {
				seen[v] = true;
				r.input.push(v);
			}
		}
		return r;
	}

	return {
		available : function() {
			return impl != null;
		},
		version : function() {
			return impl != null ? String(impl.version()) : "js";
		},
		matchString : function(ps, a, r) {
			function mergeMatchResult(res) {
				return merge(r, res);
			}
			if (impl != null && matchCacheSupported) {
				try {
					syncMatchSource(a);
					return mergeMatchResult(impl.matchCached(String(ps)));
				} catch(e) {
					matchCacheSupported = false;
				}
			}
			if (impl != null) {
				try {
					return mergeMatchResult(impl.matchString(String(ps), a));
				} catch(e) { }
			}
			return jsMatchString(ps, a, r);
		},
		/**
		 * 批量过滤：返回与 arr 等长的下标掩码，true 表示保留。
		 * 对应 setFilter(function(e, i){ return e.indexOf(s) >= 0; })
		 */
		filter : function(arr, s) {
			var keep = [], i, p = String(s);
			if (impl != null) {
				try { return impl.filter(arr, p); } catch (e) { /* jar 太旧没有 filter，走下面的回退 */ }
			}
			for (i = 0; i < arr.length; i++) keep.push(arr[i] != null && arr[i].indexOf(p) >= 0);
			return keep;
		},
		/** 同步建议列表文本到缓存（CA.Assist 打开时调用一次） */
		setFilterTexts : function(texts) {
			filterTextsCache = texts;
			if (impl != null) {
				try { impl.setFilterTexts(texts); } catch(e) { /* 老 jar */ }
			}
		},
		/** 缓存版 filter：返回 boolean 掩码；没有 jar 时走 JS */
		filterTexts : function(s) {
			var p = String(s), keep = [], i;
			if (impl != null) {
				try { return impl.filterTexts(p); } catch(e) { }
			}
			if (filterTextsCache != null) {
				for (i = 0; i < filterTextsCache.length; i++) {
					keep.push(filterTextsCache[i] != null && filterTextsCache[i].indexOf(p) >= 0);
				}
			}
			return keep;
		},
		/** 同步 ID 列表文本到 Java/JS 缓存（CA.buildIDList 时调用一次） */
		setIDTexts : function(texts) {
			if (texts === lastIDTexts) return;
			lastIDTexts = texts;
			idTextsCache = texts;
			if (impl != null) {
				try { impl.setIDTexts(texts); } catch(e) { /* 老 jar 没有缓存方法 */ }
			}
		},
		/** 在缓存的 ID 文本里找包含 s 的下标；有 Java 缓存走 Java，否则走 JS 循环 */
		filterIDTexts : function(s) {
			var p = String(s), out = [], i;
			if (impl != null) {
				try {
					var res = impl.filterIDTexts(p);
					for (i = 0; i < res.length; i++) out.push(res[i]);
					return out;
				} catch(e) { }
			}
			if (idTextsCache != null) {
				for (i = 0; i < idTextsCache.length; i++) {
					if (idTextsCache[i] != null && idTextsCache[i].indexOf(p) >= 0) out.push(i);
				}
			}
			return out;
		},
		/** 同步命令快照（命令库重建时调用一次） */
		setCommandSnapshot : function(snap) {
			syncCommandSnapshot(snap);
		},
		/** 匹配命令名/描述；有 jar 走 Java，否则走等价 JS 实现 */
		matchCommandNames : function(snap, prefix) {
			syncCommandSnapshot(snap);
			if (impl != null) {
				try {
					var res = impl.matchCommandNames(String(prefix));
					var out = [], i;
					for (i = 0; i < res.length; i++) out.push(String(res[i]));
					return out;
				} catch(e) { }
			}
			return jsMatchCommandNames(prefix);
		},
		/** 调试用：强制走 JS 命令匹配 */
		_matchCommandNamesJS : function(prefix) {
			return jsMatchCommandNames(prefix);
		},
		/** 版本号数组比较，对应 NeteaseAdapter.compareVersion */
		compareVersionArray : function(a, b) {
			if (impl != null) {
				try { return impl.compareVersionArray(a, b); } catch (e) { }
			}
			var n = Math.max(a.length, b.length), i, p1, p2;
			for (i = 0; i < n; i++) {
				p1 = isNaN(a[i]) ? -1 : parseInt(a[i]);
				p2 = isNaN(b[i]) ? -1 : parseInt(b[i]);
				if (p1 < p2) return -1;
				if (p1 > p2) return 1;
			}
			return 0;
		},
		/** 数组 enum 匹配：返回 { flags, length, canFinish }；有 jar 走 Java */
		classifyArray : function(arr, ps) {
			if (impl != null) {
				try {
					var res = impl.classifyArray(arr, String(ps));
					return {
						flags : res.flags,
						length : res.length,
						canFinish : res.canFinish
					};
				} catch(e) { }
			}
			var p = String(ps), flags = [], length = -1, canFinish = false, i, e;
			for (i = 0; i < arr.length; i++) {
				e = arr[i];
				if (e == null) {
					flags.push(0);
					continue;
				}
				if (p == e || p.indexOf(e + " ") === 0) {
					flags.push(1);
					canFinish = true;
					length = Math.max(length, e.length);
				} else if (e.indexOf(p) === 0) {
					flags.push(2);
					length = Math.max(length, p.length);
				} else {
					flags.push(0);
				}
			}
			return {
				flags : flags,
				length : length,
				canFinish : canFinish
			};
		},
		/** 枚举候选分类：1=精确命中 2=模糊命中 0=不命中 */
		classify : function(keys, descs, ps) {
			if (impl != null) {
				try { return impl.classify(keys, descs, String(ps)); } catch (e) { }
			}
			var flags = [], i, k, d, p = String(ps);
			for (i = 0; i < keys.length; i++) {
				k = keys[i];
				if (p == k || p.indexOf(k + " ") == 0) { flags.push(1); continue; }
				d = (i < descs.length && descs[i]) ? descs[i] : "";
				flags.push((k.indexOf(p) >= 0 || d.indexOf(p) >= 0) ? 2 : 0);
			}
			return flags;
		},
		// 调试用：绕过候选源缓存，每次调用都直接走 Java matchString（模拟旧路径）
		_matchStringDirect : function(ps, a, r) {
			if (impl == null) return jsMatchString(ps, a, r);
			return merge(r, impl.matchString(String(ps), a));
		},
		// 供单测/调试：强制走 JS 实现
		_matchStringJS : jsMatchString
	};
})());
