MapScript.loadModule("Log", (function () {
	var proto = {
		nullFunc: function (v) { return v },
		start: function (target) {
			var i;
			for (i in this) if (i.length < 3) delete this[i];
			this.setTarget(target);
			return this;
		},
		stop: function () {
			var i;
			for (i in this) if (i.length < 3) this[i] = proto.nullFunc;
			this.setTarget("null");
			return this;
		},
		setTarget: function (target) {
			if (target instanceof Function) {
				return this.println = target;
			}
			this.println = proto.nullFunc;
		},
		throwError: function self(err) {
			Error.captureStackTrace(err, self);
			throw err;
		},
		a: function (a, b, m) { //断言
			if (a !== b) {
				this.println("Fatal", m + ": " + a + " !== " + b);
				this.r();
				this.throwError(new Error(m));
			}
		},
		c: function (f, scope) { //尝试调用函数
			try {
				for (var i = 2, s = []; i < arguments.length; i++) s.push(arguments[i]);
				return this.d(f.apply(scope, s), s);
			} catch (e) {
				this.e(e);
			}
		},
		d: function (v) { //打印多个信息
			for (var i = 0, s = []; i < arguments.length; i++) s.push(arguments[i]);
			this.println("Debug", s.join("; "));
			return v;
		},
		e: function (e) { //打印错误
			var s = [e, e.stack];
			this.println("Error", s.join("\n"));
		},
		f: function (name, args) { //记录函数
			for (var i = 0, s = []; i < args.length; i++) s.push(args[i]);
			this.println("Verbose", name + "(" + s.join(", ") + ")");
		},
		r: function captureStack() { //查看堆栈
			var k = {};
			Error.captureStackTrace(k, captureStack);
			return this.println("Debug", k.stack);
		},
		s: function (s) { //树状解析对象
			return (this.println("Debug", this.debug("D", s, 0).join("\n")), s);
		},
		t: function self(s) { //显示Toast
			gHandler.post(function () {
				if (self.last) self.last.cancel();
				(self.last = android.widget.Toast.makeText(ctx, String(s), 0)).show();
			});
		},
		e: function (e) { //打印警告
			var s = [e, e.stack];
			this.println("Warning", s.join("\n"));
		},
		debug: function self(name, o, depth, objs) {
			var i, r = [], circular;
			if (!objs) objs = [];
			if (depth > 8) return [name + ": " + o];
			if (o instanceof java.lang.String) o = String(o);
			circular = objs.indexOf(o) >= 0;
			if (o instanceof Array) {
				r.push(name + ": " + "Array[" + o.length + "]");
			} else {
				r.push(name + ": " + (typeof o) + ": " + (o instanceof Function ? "[Function]" : circular ? "[Circular]" : o));
			}
			if (o instanceof Object && !circular) {
				objs.push(o);
				for (i in o) {
					self(i, o[i], depth + 1, objs).forEach(function (e) {
						r.push("\t" + e);
					});
				}
			}
			return r;
		},
		captureStack: function self(srcFunc) {
			var k = {};
			Error.captureStackTrace(k, srcFunc || self);
			return k.stack;
		}
	};
	return Object.create(proto).stop();
})());

MapScript.loadModule("erp", function self(error, silent, extra) {
	if (error instanceof java.lang.Throwable) {
		error = {
			javaException: error,
			stack: "",
			fileName: "",
			toString: function () {
				return this.javaException.toString();
			}
		};
	}
	var tech = [
		error,
		"\n版本: {DATE}",
		"\n来源:", error.fileName,
		"\n包名:", ctx.getPackageName(),
		"\nSDK版本:", android.os.Build.VERSION.SDK_INT,
		"\n制造商:", android.os.Build.MANUFACTURER,
		"\n堆栈:", error.stack
	].join("");
	if (MapScript.host == "BlockLauncher") tech += "\nMinecraft版本: " + ModPE.getMinecraftVersion();
	if (error.javaException) {
		var strw = new java.io.StringWriter(), strp = new java.io.PrintWriter(strw);
		error.javaException.printStackTrace(strp);
		tech += "\nJavaException: " + strw.toString();
	}
	if (extra) tech += "\n" + Log.debug("额外数据", extra, 0).join("\n");
	android.util.Log.e("CA", tech);
	try {
		var fs = new java.io.PrintWriter(new java.io.FileOutputStream(MapScript.baseDir + "com.xero.ca.error.log", true));
		fs.println("* " + (silent ? "Warning" : "Error") + ": " + new Date().toLocaleString());
		fs.println(tech);
		fs.close();
		if (silent) {
			Log.w(error);
		} else {
			Log.e(error);
		}
	} catch (e) {
		android.util.Log.e("CA", e);
	}
	if (silent) return;
	if (self.count) {
		self.count++;
	} else {
		self.count = 1;
	}
	if (self.count > 3) return;
	if (!self.notReport) {
		new java.lang.Thread(function () {
			try {
				var url = new java.net.URL("https://ca.projectxero.top/bugreport");
				var conn = url.openConnection();
				conn.setConnectTimeout(5000);
				conn.setUseCaches(false);
				conn.setRequestMethod("POST");
				conn.setDoInput(true);
				conn.setDoOutput(true);
				var rd, s, ln;
				var wr = conn.getOutputStream();
				wr.write(new java.lang.String(tech).getBytes());
				wr.flush();
				conn.getInputStream().close();
			} catch (e) {
				android.util.Log.e("CA", e);
			}
		}).start();
	}
	if (MapScript.host == "Android") {
		ScriptInterface.reportError(tech);
		return;
	}
	gHandler.post(new java.lang.Runnable({
		run: function () {
			try {
				android.widget.Toast.makeText(ctx, error.fileName + "出现了一个错误：" + error + "\n查看对话框获得更多信息。", 0).show();
				var dialog = new android.app.AlertDialog.Builder(ctx);
				dialog.setTitle("错误");
				dialog.setCancelable(false);
				dialog.setMessage("您好，" + error.fileName + "出现了一个错误。您可以将这个错误反馈给我们，来推动这个Mod的更新。您也可以选择忽略。您可以直接在设置-关于-意见与反馈里进行反馈，也可直接联系作者：QQ-2687587184；Email-projectxero@163.com\n\n错误信息：\n" + tech);
				dialog.setPositiveButton("忽略", new android.content.DialogInterface.OnClickListener({
					onClick: function (dia, w) {
						dia.dismiss();
					}
				}));
				dialog.setNegativeButton("立即停止", new android.content.DialogInterface.OnClickListener({
					onClick: function (dia, w) {
						unload()
						ctx.finish();
					}
				}));
				dialog.setNeutralButton("复制错误信息", new android.content.DialogInterface.OnClickListener({
					onClick: function (dia, w) {
						try {
							ctx.getSystemService(ctx.CLIPBOARD_SERVICE).setText(tech);
							android.widget.Toast.makeText(ctx, "错误信息已复制", 0).show();
							dia.dismiss();
						} catch (e) { }
					}
				}));
				dialog.show();
			} catch (e) { }
		}
	}));
});
