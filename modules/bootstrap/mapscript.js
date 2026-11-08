var MapScript = {
	//可访问钩子
	hooks: ["attackHook", "chatHook", "continueDestroyBlock", "destroyBlock", "projectileHitEntityHook", "eatHook", "entityAddedHook", "entityHurtHook", "entityRemovedHook", "explodeHook", "serverMessageReceiveHook", "deathHook", "playerAddExpHook", "playerExpLevelChangeHook", "redstoneUpdateHook", "screenChangeHook", "newLevel", "startDestroyBlock", "projectileHitBlockHook", "modTick", "leaveGame", "useItem", "initialize", "unload"],

	//已加载模块列表
	modules: [],

	//重置函数代码
	clearCode: function (func) {
		if (!(func in this) || typeof this[func] != "function") return null;
		var q = this[func].toString();
		q = q.slice(q.indexOf("function"), q.indexOf("{"));
		return this[func] = eval("(" + q + "{})");
	},

	//补充函数代码
	addCode: function (func, code) {
		if (!(func in this) || typeof this[func] != "function") return null;
		var q = this[func].toString();
		q = q.slice(q.indexOf("function"), q.lastIndexOf("}"));
		return this[func] = eval("(" + q + code + "})");
	},

	//读取并解析JSON-EX
	readJSON: function (path, defaultValue, gzipped) {
		try {
			if (!(new java.io.File(path)).isFile()) return defaultValue;
			var rd, s = [], q;
			if (gzipped) {
				rd = new java.io.BufferedReader(new java.io.InputStreamReader(new java.util.zip.GZIPInputStream(new java.io.FileInputStream(path))));
			} else {
				rd = new java.io.BufferedReader(new java.io.FileReader(path));
			}
			while (q = rd.readLine()) s.push(q);
			rd.close();
			return eval("(" + s.join("\n") + ")");
		} catch (e) {
			return defaultValue;
		}
	},

	//保存JSON-EX
	saveJSON: function (path, object, gzipped) {
		var wr;
		var f = new java.io.File(path).getParentFile();
		if (f) f.mkdirs();
		if (gzipped) {
			wr = new java.util.zip.GZIPOutputStream(new java.io.FileOutputStream(path));
		} else {
			wr = new java.io.FileOutputStream(path);
		}
		wr.write(new java.lang.String(this.toSource(object)).getBytes());
		wr.close();
	},

	//加载模块
	loadModule: function (name, obj, ignoreHook) {
		var i, sn, dx = this.modules.indexOf(name);
		if (obj === undefined && dx >= 0) {
			delete this.global[name];
		} else if (!(name in this.global) || dx >= 0) {
			this.global[name] = obj;
			if (dx < 0) this.modules.push(name);
			if (!ignoreHook && (obj instanceof Object)) {
				if (typeof obj.onCreate == "function") obj.onCreate();
				sn = this.toSource(name);
				for (i in obj)
					if (typeof obj[i] == "function" && this.hooks.indexOf(i) >= 0 && this.global[i].length == obj[i].length)
						this.addCode.call(this.global, i, "this[" + sn + "]." + i + ".apply(this[" + sn + "],arguments);");
			}
		} else return false;
		return true;
	},

	//返回对象源代码
	toSource: function (obj) {
		var strtok = ["\\\\", "\\n", "\\t", /*"\\b",*/ "\\r", "\\f", "\\\"", "\\\'"];
		var _toJSON = function toJSON(x, lev) {
			var p = "", r, i;
			if (lev < 0) return toJSON(String(x), 0);
			if (typeof x == "string") {
				for (i = 0; i < strtok.length; i++) x = x.replace(new RegExp(strtok[i], "g"), strtok[i]);
				return "\"" + x + "\"";
			} else if (Array.isArray(x)) {
				r = new Array();
				for (i = 0; i < x.length; i++) r.push(toJSON(x[i], lev - 1));
				p = "[" + r.join(",") + "]";
			} else if (x instanceof Error) {
				p = "new Error(" + toJSON(x.message) + ")";
			} else if (x instanceof RegExp) {
				p = x.toString();
			} else if (x instanceof Date) {
				p = "new Date(" + x.getTime() + ")";
			} else if (x instanceof Function) {
				p = x.toString();
			} else if (x instanceof Object) {
				r = new Array();
				for (i in x) r.push(toJSON(i, lev) + ":" + toJSON(x[i], lev - 1));
				p = "{" + r.join(",") + "}";
			} else if (typeof x == "object" && x != null) {
				p = toJSON(String(x), lev);
			} else {
				p = String(x);
			}
			return p;
		}
		return _toJSON(obj, 32);
	},

	//初始化
	init: function (g) {
		Object.defineProperty(this, "global", {
			enumerable: false,
			configurable: false,
			writable: false,
			value: g
		});
		if ("module" in g) { //Node.js
			module.exports = function (name) {
				return g[name];
			}
		}
	},

	initialize: function () {
		this.global.initialize();
	}
}
MapScript.init(this);
