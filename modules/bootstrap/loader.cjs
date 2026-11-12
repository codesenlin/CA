MapScript.loadModule("Loader", {
	loading: false,
	modules: {},
	load: function (f) {
		var lto, lm, lmb;
		if (MapScript.host == "Android") {
			lm = MapScript.loadModule;
			lmb = lm.bind(MapScript);
			MapScript.loadModule = function (name, obj, ignoreHook) {
				ScriptInterface.setLoadingTitle("正在加载模块：" + name);
				lmb(name, obj, ignoreHook);
			};
		}
		this.loading = true;
		this.enableCache();
		if (MapScript.host != "Android") {
			gHandler.post(function () {
				try {
					var startupText = (BuildConfig.flavorData && BuildConfig.flavorData.startupText) || "命令助手 by ProjectXero";
					lto = android.widget.Toast.makeText(ctx, startupText + "\n基于Rhino (" + MapScript.host + ")\n加载中……", 1);
					lto.setGravity(android.view.Gravity.CENTER, 0, 0);
					lto.show();
				} catch (e) { erp(e) }
			});
		}
		var th = new java.lang.Thread(new java.lang.Runnable({
			run: function () {
				try { //Async Loading
					f();
					gHandler.post(function () {
						try {
							if (lto) lto.cancel();
							if (lm) ScriptInterface.setLoadingTitle("初始化模块");
						} catch (e) { erp(e) }
					});
					if (lm) MapScript.loadModule = lm;
					Loader.loading = false;
					Loader.disableCache();
					MapScript.initialize();
				} catch (e) { erp(e) }
			}
		}));
		th.start();
	},
	enableCache: function () {
		if (!this.cache) this.cache = {};
	},
	disableCache: function () {
		if (this.cache) this.cache = null;
	},
	open: function (path) {
		if (MapScript.host == "Android") {
			var manager = ScriptInterface.getScriptManager();
			return manager.open(path);
		} else if (MapScript.global.modulePath) {
			return new java.io.FileInputStream(new java.io.File(MapScript.global.modulePath, path));
		} else Log.throwError(new Error("不支持的平台"));
	},
	getCanonicalFile: function (path) {
		var pathFile, rd, s, parentDir, t;
		pathFile = new java.io.File(path.replace(/\\/g, "/")).getCanonicalFile();
		return {
			path: String(pathFile.getPath()),
			name: String(pathFile.getName()),
			parent: String(pathFile.getParent())
		};
	},
	readFile: function (path) {
		var rd, s, t;
		rd = new java.io.BufferedReader(new java.io.InputStreamReader(this.open(path)));
		s = [];
		while (t = rd.readLine()) s.push(t);
		rd.close();
		return s.join("\n");
	},
	fromFile: function (path) { //这是一个占位符函数，它只会在调试过程中起作用
		var file = this.getCanonicalFile(path), s, t;
		path = file.path;
		if (this.cache && path in this.cache) return this.cache[path];
		s = this.readFile(path).replace(/Loader.fromFile\("(.+)"\)/g, function (match, mpath) {
			return match.replace(mpath, file.parent + "/" + mpath);
		});
		if (s.search(/;\s*$/) < 0) s = "(" + s + ")";
		t = this.evalSpecial(s, file.name, 0, MapScript.global, Loader);
		if (this.cache) this.cache[path] = t;
		return t;
	},
	require: function (path, module) { //这是一个调试用函数
		var file = this.getCanonicalFile(path), s, t;
		path = file.path;
		if (path in this.modules) return this.modules[path].exports;
		if (arguments.length == 2) {
			t = {
				exports: module,
				file: file,
				require: function (path) {
					return Loader.require(file.parent + "/" + path);
				}
			};
		} else {
			s = this.readFile(path);
			t = {
				exports: {},
				file: file,
				require: function (path) {
					return Loader.require(file.parent + "/" + path);
				}
			};
			this.modules[path] = t;
			this.evalSpecial("(function(exports, require, module, __filename, __dirname) {" + s + "})(this.exports, this.require, this, this.path, this.parent)", file.name, 0, MapScript.global, t);
		}
		return t.exports;
	},
	evalSpecial: function (source, sourceName, lineNumber, scope, thisArg) {
		var cx = org.mozilla.javascript.Context.getCurrentContext();
		var ret = org.mozilla.javascript.ScriptRuntime.evalSpecial(cx, scope, thisArg, [new java.lang.String(source)], sourceName, lineNumber);
		if (ret instanceof java.lang.String) {
			return String(ret);
		} else if (ret instanceof java.lang.Boolean) {
			return ret.booleanValue();
		} else if (ret instanceof java.lang.Number) {
			return ret.doubleValue();
		} else {
			return ret;
		}
	},
	lockProperty: function (obj, propertyName) {
		Object.defineProperty(obj, propertyName, {
			enumerable: false,
			configurable: false,
			writable: false,
			value: obj[propertyName]
		});
	},
	lockMethods: function (obj, methods) {
		var i, a = methods || Object.getOwnPropertyNames(obj);
		for (i = 0; i < a.length; i++) {
			if (typeof obj[a[i]] == "function") this.lockProperty(obj, a[i]);
		}
	},
	lockFields: function (obj, fields) {
		var i, a = fields || Object.getOwnPropertyNames(obj);
		for (i = 0; i < a.length; i++) {
			if (typeof obj[a[i]] != "function") this.lockProperty(obj, a[i]);
		}
	},
	freezeObject: function (obj) {
		var i, a = Object.getOwnPropertyNames(obj);
		for (i = 0; i < a.length; i++) {
			if (typeof obj[a[i]] == "object") this.freezeObject(obj[a[i]]);
		}
		Object.freeze(obj);
	},
	freezeProperty: function (obj, propertyName) {
		if (typeof obj[propertyName] == "object") this.freezeObject(obj[propertyName]);
		this.lockProperty(obj, propertyName);
	},
	freezeFields: function (obj, fields) {
		var i, a = fields || Object.getOwnPropertyNames(obj);
		for (i = 0; i < a.length; i++) {
			if (typeof obj[a[i]] != "function") this.freezeProperty(obj, a[i]);
		}
	},
	ProtectedMethodWrapper: function (realFunc) {
		var i, args = new Array(arguments.length - 1);
		for (i = 0; i < args.length; i++) {
			args[i] = arguments[i + 1];
		}
		return realFunc.apply(this, args);
	},
	protectMethods: function (parent, objName, publicProp) {
		var i, obj = parent[objName], target = {}, propName, propData;
		for (i = 0; i < publicProp.length; i++) {
			propName = publicProp[i];
			propData = obj[propName];
			if (typeof propData == "function") {
				target[propName] = this.ProtectedMethodWrapper.bind(obj, propData);
			} else {
				Object.defineProperty(obj, propertyName, {
					enumerable: true,
					configurable: false,
					get: function () {
						return obj[propName];
					},
					set: function (value) {
						obj[propName] = value;
					},
				});
			}
		}
		parent[objName] = target;
	}
});
