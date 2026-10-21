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

	return {
		available : function() {
			return impl != null;
		},
		version : function() {
			return impl != null ? String(impl.version()) : "js";
		},
		matchString : function(ps, a, r) {
			if (impl == null) return jsMatchString(ps, a, r);
			var res = impl.matchString(String(ps), a);
			if (!(r instanceof Object)) r = {};
			if (!Array.isArray(r.input)) r.input = [];
			if (!(r.output instanceof Object)) r.output = {};
			var outs = res.outputs, keys = outs.keySet().toArray();
			for (var i = 0; i < keys.length; i++) {
				r.output[String(keys[i])] = String(outs.get(keys[i]));
			}
			var ins = res.inputs.toArray();
			for (var j = 0; j < ins.length; j++) {
				var v = String(ins[j]);
				if (r.input.indexOf(v) < 0) r.input.push(v);
			}
			return r;
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
		// 供单测/调试：强制走 JS 实现
		_matchStringJS : jsMatchString
	};
})());
