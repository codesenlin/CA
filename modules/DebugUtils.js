MapScript.loadModule("DebugUtils", {
	showDebugDialog: function self(interface) {
		G.ui(function () {
			try {
				var lastInterface;
				if (!self.main) {
					self.LINE_LIMIT = 200;
					self.history = [];
					self.lines = [];
					self.defaultInterface = {
						getWelcomeText: function () {
							return "控制台 - 输入exit以退出";
						},
						getGlobal: function () {
							return eval.call(null, "this");
						},
						evalExpr: function (expr) {
							return Loader.evalSpecial(expr, "Console", 0, this.evalScope, null);
						},
						evalScope: {
							print: function (str) {
								self.print(Common.toString(str));
							},
							println: function (str) {
								self.print(Common.toString(str));
								self.print("\n");
							}
						},
						onCommand: function (cmd) {
							var lc = cmd.toLowerCase();
							if (lc.startsWith("id ")) {
								try {
									DebugUtils.startInteractiveDebug(cmd.slice(3), this.debugStatusListener);
								} catch (_e) {
									self.print(_e + "\n" + _e.stack, new G.ForegroundColorSpan(Common.theme.criticalcolor));
								}
							} else {
								return false;
							}
							return true;
						},
						setPrinter: function (printer) { },
						debugStatusListener: function (status, arg1, arg2) {
							switch (status) {
								case "connecting":
									self.print("\n[Interactive Debug] Connecting...");
									break;
								case "connected":
									self.print("\n[Interactive Debug] Connected!");
									break;
								case "disconnected":
									self.print("\n[Interactive Debug] Disconnected!");
									break;
								case "remoteExec":
									self.print("\n[Interactive Debug] Remote:" + arg1);
									break;
								case "log":
									self.print("\n[Interactive Debug] [" + arg1 + "]" + arg2);
									break;
								case "error":
									self.print("\n[Interactive Debug] Error: " + arg1, new G.ForegroundColorSpan(Common.theme.criticalcolor));
									break;
							}
						}
					};
					self.vmaker = function (holder) {
						var text = holder.text = new G.TextView(ctx);
						text.setLayoutParams(new G.AbsListView.LayoutParams(-1, -2));
						Common.applyStyle(text, "textview_default", 2);
						return text;
					}
					self.vbinder = function (holder, e, i, a) {
						holder.text.setText(e);
						holder.text.setPadding(10 * G.dp, i == 0 ? 10 * G.dp : 0, 10 * G.dp, i == a.length - 1 ? 10 * G.dp : 0);
					}
					self.cls = function () {
						self.lines.length = 0;
						self.history.length = 0;
						self.lines.push(new G.SpannableStringBuilder());
						self.print(self.interface.getWelcomeText(), new G.StyleSpan(G.Typeface.BOLD));
						self.ready("exit");
					}
					self.print = function (str, span) {
						G.ui(function () {
							try {
								var t = self.lines[self.lines.length - 1];
								if (span) {
									appendSSB(t, str, span);
								} else {
									t.append(str);
								}
								self.adapter.notifyChange();
								self.prompt.smoothScrollToPosition(self.lines.length - 1);
							} catch (e) { erp(e) }
						})
					};
					self.ready = function (cmd) {
						cmd = String(cmd);
						self.history[self.lines.length - 1] = cmd;
						self.lines.push(new G.SpannableStringBuilder());
						if (self.lines.length > self.LINE_LIMIT) {
							self.lines.splice(0, self.lines.length - self.LINE_LIMIT - 1);
							self.history.splice(0, self.history.length - self.LINE_LIMIT - 1);
						}
						self.hiscur = -1;
						self.adapter.notifyChange();
						self.print(">  ", new G.ForegroundColorSpan(Common.theme.highlightcolor));
					}
					self.exec = function (_s) {
						var _t, _ls = _s.toLowerCase();
						if (self.interface.onCommand(_s)) {
							return;
						} else if (_ls == "exit") {
							self.popup.exit();
							return;
						} else if (_ls == "cls") {
							self.cls();
							return;
						} else if (_ls == "ls") {
							JSONEdit.trace(self.interface.getGlobal());
						} else if (_ls.startsWith("ls ")) {
							try {
								JSONEdit.trace(self.interface.evalExpr(_s.slice(3)));
							} catch (_e) {
								self.print(_e + "\n" + _e.stack, new G.ForegroundColorSpan(Common.theme.criticalcolor));
							}
						} else if (_ls.startsWith("cp ")) {
							try {
								var _t = MapScript.toSource(self.interface.evalExpr(_s.slice(3)));
								self.print(_t);
								Common.setClipboardText(_t);
							} catch (_e) {
								self.print(_e + "\n" + _e.stack, new G.ForegroundColorSpan(Common.theme.criticalcolor));
								Common.setClipboardText(_e + "\n" + _e.stack);
							}
						} else if (_ls.startsWith("sn ")) {
							try {
								_t = MapScript.toSource(self.interface.evalExpr(_s.slice(3)));
								self.print(_t);
							} catch (_e) {
								self.print(_t = _e + "\n" + _e.stack, new G.ForegroundColorSpan(Common.theme.criticalcolor));
							}
							var _file = new java.io.File(ctx.getExternalCacheDir(), "sn.txt");
							var _fs = new java.io.PrintWriter(new java.io.FileOutputStream(_file));
							_fs.println(_t);
							_fs.close();
							try {
								AndroidBridge.sendFile(_file);
							} catch (e) {
								Common.toast("文件已生成于" + _file.getAbsolutePath());
							}
						} else if (_ls.startsWith("exec ")) {
							try {
								_t = self.interface.evalExpr(Common.readFile(_s.slice(5), ""));
								self.print(Log.debug("D", _t, 0).join("\n"));
							} catch (_e) {
								self.print(_e + "\n" + _e.stack, new G.ForegroundColorSpan(Common.theme.criticalcolor));
							}
						} else if (_ls.startsWith("#")) {
							Threads.run(function () {
								try {
									var _t = self.interface.evalExpr(_s.slice(1));
									self.print(Log.debug("D", _t, 0).join("\n"));
								} catch (_e) {
									self.print(_e + "\n" + _e.stack, new G.ForegroundColorSpan(Common.theme.criticalcolor));
								}
								G.ui(function () {
									try {
										self.ready(_s);
									} catch (e) { erp(e) }
								});
							});
							return;
						} else {
							try {
								_t = self.interface.evalExpr(_s);
								self.print(Log.debug("D", _t, 0).join("\n"));
							} catch (_e) {
								self.print(_e + "\n" + _e.stack, new G.ForegroundColorSpan(Common.theme.criticalcolor));
							}
						}
						self.ready(_s);
					}
					ConsolePanel.build(self, "common.Console");
				}
				lastInterface = self.interface;
				self.interface = interface || self.defaultInterface;
				self.interface.setPrinter(self.print.bind(self));
				if (self.interface != lastInterface) {
					self.cls();
				}
				self.popup.enter();
			} catch (e) { erp(e) }
		})
	},

	traceStack: function () {
		var s = [], i;
		var ts = java.lang.Thread.getAllStackTraces();
		var it = ts.keySet().iterator();
		var ct, cts, ctid = java.lang.Thread.currentThread().getId();
		while (it.hasNext()) {
			ct = it.next();
			s.push((ctid == ct.getId() ? "<当前>" : "") + "线程" + ct.getId() + ":" + ct.getName() + " (优先级" + ct.getPriority() + (ct.isDaemon() ? "守护线程" : "") + ") - " + ct.getState().toString());
			cts = ts.get(ct);
			for (i in cts) {
				s.push(" at " + cts[i].toString());
			}
			s.push("");
		}
		return s.join("\n");
	},

	startInteractiveDebug: (function () {
		var scope = {
			KEY: Internal.getKey()
		};
		return function (uri, statusListener) {
			if (MapScript.host != "Android") {
				Log.throwError(new Error("Your device not support Interactive Debug!"));
			}
			if (this.wsclient) Log.throwError(new Error("Channel busy"));
			statusListener("connecting");
			this.wsclient = ScriptInterface.createWSClient(uri, {
				onOpen: function (thisObj, handshake) {
					try {
						thisObj.send(JSON.stringify({
							type: "version",
							version: CA.version,
							publishDate: CA.publishDate
						}));
						Log.start(function (level, text) {
							statusListener("log", level, text);
							thisObj.send(JSON.stringify({
								type: "println",
								level: level,
								text: text
							}));
						});
						statusListener("connected");
					} catch (e) { erp(e) }
				},
				onClose: function (thisObj, code, reason, remote) {
					try {
						Log.stop();
						statusListener("disconnected");
						DebugUtils.wsclient = null;
					} catch (e) { erp(e) }
				},
				onMessage: function (thisObj, message) {
					try {
						try {
							var o = JSON.parse(message);
							switch (o.type) {
								case "exec":
									statusListener("remoteExec", o.cmd);
									if (o.cmd.startsWith("#")) {
										G.ui(function () {
											try {
												Log.s(Loader.evalSpecial(o.cmd.slice(1), "InteractiveDebugger", 0, scope, null));
											} catch (e) {
												Log.e(e);
											}
										});
									} else {
										try {
											Log.s(Loader.evalSpecial(o.cmd, "InteractiveDebugger", 0, scope, null));
										} catch (e) {
											Log.e(e);
										}
									};
									break;
								case "ping":
									thisObj.send(JSON.stringify({ type: "pong" }));
									break;
							}
						} catch (e) {
							Log.e(e);
						}
					} catch (e) { erp(e) }
				},
				onError: function (thisObj, err) {
					erp(err, true);
					statusListener("error", err);
				}
			});
			this.wsclient.connect();
		};
	})(),
	stopInteractiveDebug: function () {
		if (this.wsclient && this.wsclient.getReadyState() == org.java_websocket.WebSocket.READYSTATE.OPEN) this.wsclient.close();
	},
	debugAction: {
		name: "自定义动作",
		description: "点击后会执行指定的代码，可自定义名称与代码",
		create: function () {
			return {
				name: "",
				desp: "",
				expr: "",
				enabled: true
			};
		},
		edit: function (data, newCreated, callback) {
			DebugUtils.showEditDebugAction(data, newCreated, callback);
		},
		getName: function (data) {
			return String(data.name);
		},
		getDescription: function (data) {
			return String(data.desp);
		},
		available: function (data) {
			return data.enabled;
		},
		execute: function (data) {
			try {
				eval.call(null, data.expr);
			} catch (e) {
				erp(e, true);
				Common.toast("无法执行自定义动作:" + data.name + "，错误已保存至错误日志\n" + e);
			}
		}
	},
	showEditDebugAction: function (data, newCreated, callback) {
		G.ui(function () {
			try {
				var popup, layout, name, desp, expr, enabled;
				layout = L.ScrollView({
					style: "message_bg",
					child: L.LinearLayout({
						orientation: L.LinearLayout("vertical"),
						padding: [15 * G.dp, 15 * G.dp, 15 * G.dp, 0],
						children: [
							L.TextView({
								text: newCreated ? "新建自定义动作" : "编辑自定义动作",
								padding: [0, 0, 0, 10 * G.dp],
								layout: { width: -1, height: -2 },
								style: "textview_default",
								fontSize: 4
							}),
							name = L.EditText({
								text: data.name,
								hint: "标题",
								singleLine: true,
								padding: [0, 0, 0, 0],
								imeOptions: L.EditorInfo("IME_FLAG_NO_FULLSCREEN"),
								style: "edittext_default",
								fontSize: 3,
								layout: { width: -1, height: -2 }
							}),
							desp = L.EditText({
								text: data.desp,
								hint: "描述（可选）",
								singleLine: true,
								padding: [0, 10 * G.dp, 0, 0],
								imeOptions: L.EditorInfo("IME_FLAG_NO_FULLSCREEN"),
								style: "edittext_default",
								fontSize: 2,
								layout: { width: -1, height: -2 }
							}),
							expr = L.EditText({
								text: data.expr,
								hint: "在此输入代码",
								padding: [0, 20 * G.dp, 0, 10 * G.dp],
								imeOptions: L.EditorInfo("IME_FLAG_NO_FULLSCREEN"),
								style: "edittext_default",
								fontSize: 2,
								layout: { width: -1, height: -2 }
							}),
							enabled = L.CheckBox({
								text: "启用",
								checked: Boolean(data.enabled)
							}),
							L.TextView({
								text: "确定",
								padding: [10 * G.dp, 20 * G.dp, 10 * G.dp, 20 * G.dp],
								gravity: L.Gravity("center"),
								layout: { width: -1, height: -2 },
								style: "button_critical",
								fontSize: 3,
								onClick: function () {
									try {
										data.name = String(name.text) || "自定义动作";
										data.desp = String(desp.text);
										data.enabled = Boolean(enabled.checked);
										data.expr = String(expr.text);
										if (callback) callback(data);
										popup.exit();
									} catch (e) { erp(e) }
								}
							})
						]
					})
				});
				popup = PopupPage.showDialog("debug.actionEdit", layout, -1, -2);
			} catch (e) { erp(e) }
		})
	},
	debugTile: {
		name: "自定义快捷开关",
		description: "点击后会切换指定功能的开启与关闭",
		create: function () {
			return {
				label: "",
				update: "",
				onClick: ""
			};
		},
		edit: function (data, newCreated, callback) {
			DebugUtils.showEditDebugTile(data, newCreated, callback);
		},
		updateTile: function (data, tile) {
			if (data.label) tile.label = data.label;
			this.evaluateTile("update", data, tile);
		},
		onTileClick: function (data, tile) {
			this.evaluateTile("onClick", data, tile);
		},
		evaluateTile: function self(type, data, tile) {
			var tileScope = {
				label: tile.label,
				subtitle: tile.subtitle,
				state: this.stateToString(tile),
				tile: tile,
				invertState: this.invertState
			};
			tileScope.update = function () {
				Loader.evalSpecial(data.update, "DebugTile", 0, tileScope, null);
			};
			tileScope.onClick = function () {
				Loader.evalSpecial(data.onClick, "DebugTile", 0, tileScope, null);
			};
			try {
				if (type == "onClick") {
					tileScope.onClick();
				} else {
					tileScope.update();
				}
			} catch (e) {
				erp(e, true);
				Common.toast("快捷开关表达式计算出错，错误已保存至错误日志\n" + e);
			}
			tile.label = tileScope.label;
			tile.subtitle = tileScope.subtitle;
			tile.state = this.parseState(tileScope.state, tile);
		},
		stateToString: function (tile) {
			switch (tile.state) {
				case tile.STATE_ACTIVE:
					return "active";
				case tile.STATE_INACTIVE:
					return "inactive";
				case tile.STATE_UNAVAILABLE:
				default:
					return "unavailable";
			}
		},
		parseState: function (state, tile) {
			switch (state) {
				case "active":
					return tile.STATE_ACTIVE;
				case "inactive":
					return tile.STATE_INACTIVE;
				case "unavailable":
					return tile.STATE_UNAVAILABLE;
			}
			return tile.state;
		},
		invertState: function () {
			switch (this.state) {
				case "active":
					this.state = "inactive";
					break;
				case "inactive":
					this.state = "active";
					break;
				default:
					this.state = "unavailable";
			}
		}
	},
	showEditDebugTile: function (data, newCreated, callback) {
		G.ui(function () {
			try {
				var popup, layout, label, update, onClick;
				layout = L.ScrollView({
					style: "message_bg",
					child: L.LinearLayout({
						orientation: L.LinearLayout("vertical"),
						padding: [15 * G.dp, 15 * G.dp, 15 * G.dp, 0],
						children: [
							L.TextView({
								text: newCreated ? "新建自定义快捷开关" : "编辑自定义快捷开关",
								padding: [0, 0, 0, 10 * G.dp],
								layout: { width: -1, height: -2 },
								style: "textview_default",
								fontSize: 4
							}),
							label = L.EditText({
								text: data.label,
								hint: "标题(可选)",
								singleLine: true,
								padding: [0, 0, 0, 0],
								imeOptions: L.EditorInfo("IME_FLAG_NO_FULLSCREEN"),
								style: "edittext_default",
								fontSize: 3,
								layout: { width: -1, height: -2 }
							}),
							update = L.EditText({
								text: data.update,
								hint: "在此输入代码，会在更新快捷设置时触发",
								padding: [0, 20 * G.dp, 0, 10 * G.dp],
								imeOptions: L.EditorInfo("IME_FLAG_NO_FULLSCREEN"),
								style: "edittext_default",
								fontSize: 2,
								layout: { width: -1, height: -2 }
							}),
							onClick = L.EditText({
								text: data.onClick,
								hint: "在此输入代码，会在点击快捷设置时触发",
								padding: [0, 20 * G.dp, 0, 10 * G.dp],
								imeOptions: L.EditorInfo("IME_FLAG_NO_FULLSCREEN"),
								style: "edittext_default",
								fontSize: 2,
								layout: { width: -1, height: -2 }
							}),
							L.TextView({
								text: "确定",
								padding: [10 * G.dp, 20 * G.dp, 10 * G.dp, 20 * G.dp],
								gravity: L.Gravity("center"),
								layout: { width: -1, height: -2 },
								style: "button_critical",
								fontSize: 3,
								onClick: function () {
									try {
										data.label = String(label.text);
										data.update = String(update.text);
										data.onClick = String(onClick.text);
										if (callback) callback(data);
										popup.exit();
									} catch (e) { erp(e) }
								}
							})
						]
					})
				});
				popup = PopupPage.showDialog("debug.tileEdit", layout, -1, -2);
			} catch (e) { erp(e) }
		})
	},
	/** 调试控制台用：对比 matchString 旧路径（每次传候选数组）与缓存路径
	 *  用法：DebugUtils.benchMatchString() / benchMatchString(3000, 20)
	 */
	benchMatchString: function (n, rounds) {
		try {
			n = n || 1749;
			rounds = rounds || 10;
			var a = {}, i, t, prefix = "minecraft:", msg;
			for (i = 0; i < n; i++) a["minecraft:item_" + i] = "物品 " + i;
			// 旧路径：每次调用都把对象/数组传给 Java matchString
			Accel._matchStringDirect("__warmup__", a, {});
			t = Date.now();
			for (i = 0; i < rounds; i++) Accel._matchStringDirect(prefix, a, {});
			var directMs = (Date.now() - t) / rounds;
			// 新路径：第一次同步缓存，后续只传 prefix
			t = Date.now();
			Accel.matchString(prefix, a, {});
			var firstMs = Date.now() - t;
			t = Date.now();
			for (i = 0; i < rounds; i++) Accel.matchString(prefix, a, {});
			var cachedMs = (Date.now() - t) / rounds;
			msg = "MatchStringBench n=" + n + " rounds=" + rounds +
				" direct=" + directMs.toFixed(2) + "ms first=" + firstMs +
				"ms cached=" + cachedMs.toFixed(2) + "ms";
			try { android.util.Log.i("Script", msg); } catch (e) { }
			return msg;
		} catch (e) {
			return "MatchStringBench error: " + e;
		}
	},
	/** 调试控制台用：对比 Rhino / QuickJS worker / Java 命令名匹配 */
	benchCommandMatch: function () {
		try {
			var snap = CA.IntelliSense.library.command_snap;
			if (!snap) return "CommandMatchBench: no command_snap";
			var prefixes = ["", "block", "cmd", "minecraft", "zzz"], out = [], i;
			var keys = Object.keys(snap);
			var rhinoMatch = function (p) {
				return keys.filter(function (e) {
					return e.indexOf(p) >= 0 || String(snap[e]).indexOf(p) >= 0;
				}).sort();
			};
			// 预热：同步快照 / 注册 worker
			Accel.matchCommandNames(snap, "__warmup__");
			try { if (typeof QuickAccel !== "undefined") QuickAccel.matchCommandNames(snap, "__warmup__"); } catch (e) { }
			for (i = 0; i < prefixes.length; i++) {
				var p = prefixes[i], t;
				t = Date.now(); var r = rhinoMatch(p); var rhinoMs = Date.now() - t;
				t = Date.now(); var j = Accel.matchCommandNames(snap, p); var javaMs = Date.now() - t;
				var q = null, qMs = null;
				try {
					if (typeof QuickAccel !== "undefined") {
						t = Date.now(); q = QuickAccel.matchCommandNames(snap, p); qMs = Date.now() - t;
					}
				} catch (e) { }
				out.push("[" + p + "] rhino=" + rhinoMs + "ms java=" + javaMs + "ms" +
					(qMs == null ? "" : " quick=" + qMs + "ms") +
					" r=" + r.length + " j=" + (j ? j.length : "null") +
					" same=" + (j && q ? (j.length === q.length && j[0] === q[0] && j[j.length - 1] === q[q.length - 1]) : "?"));
			}
			out = "CommandMatchBench n=" + keys.length + " " + out.join(" | ");
			try { android.util.Log.i("Script", out); } catch (e) { }
			return out;
		} catch (e) {
			return "CommandMatchBench error: " + e;
		}
	},
	/** 调试控制台用：同一份 ID 表对比旧 JS 循环 / CA.filterIDList / Accel.filter */
	benchIDListCompare: function () {
		try {
			var lib = CA.IntelliSense.library, names = [], prefixes = ["", "minecraft:", "block", "item_1", "zzz"];
			var i, j, e, l, t, old, built, out = [];
			var texts;
			for (i = 0; i < lib.idlist.length; i++) {
				e = lib.idlist[i];
				l = e.lists || (e.list ? [e.list] : []);
				for (j = 0; j < l.length; j++) names.push(l[j]);
			}
			built = CA.buildIDList(names);
			texts = built.texts;
			old = function (s) {
				var arr = [], k;
				for (k = 0; k < texts.length; k++) {
					if (texts[k].indexOf(s) >= 0) arr.push(k);
				}
				return arr;
			};
			for (i = 0; i < prefixes.length; i++) {
				t = Date.now(); old(prefixes[i]); var oldMs = Date.now() - t;
				t = Date.now(); CA.filterIDList(texts, prefixes[i]); var newMs = Date.now() - t;
				t = Date.now(); Accel.filter(texts, prefixes[i]); var javaMs = Date.now() - t;
				out.push("[" + prefixes[i] + "] old=" + oldMs + "ms new=" + newMs + "ms java=" + javaMs + "ms");
			}
			out = "IDListCompare n=" + texts.length + " accel=" + Accel.available() + " " + out.join(" | ");
			try { android.util.Log.i("Script", out); } catch (e) { }
			return out;
		} catch (e) {
			return "IDListCompare error: " + e;
		}
	},
	/** 调试控制台用：验证无 jar 时 QuickAccel 回退（classify/classifyArray/filterTexts/命令匹配） */
	benchQuickAccel: function () {
		try {
			var keys = ["stone", "stone_brick", "dirt"], descs = ["方块", "", "泥土"];
			var enumArr = ["stone", "stone_brick", "dirt"];
			var t = Date.now(), flags, ca, mask, cmds;
			flags = Accel.classify(keys, descs, "stone");
			ca = Accel.classifyArray(enumArr, "stone");
			Accel.setFilterTexts(["stone", "dirt", "grass"]);
			mask = Accel.filterTexts("st");
			cmds = Accel.matchCommandNames({ cmd: "命令说明", help: "帮助" }, "cmd");
			var out = "QuickFallback avail=" + Accel.available() +
				" classify=" + JSON.stringify(flags) +
				" array=" + JSON.stringify(ca) +
				" filter=" + JSON.stringify(mask) +
				" cmd=" + JSON.stringify(cmds) +
				" " + (Date.now() - t) + "ms";
			try { android.util.Log.i("Script", out); } catch (e) { }
			return out;
		} catch (e) {
			return "QuickFallback error: " + e;
		}
	},
	/** 调试控制台用：大数据量对比 Rhino JS 与 QuickAccel worker（classify/classifyArray/filterTexts/命令匹配） */
	benchQuickAccelLarge: function (rounds) {
		rounds = rounds || 20;
		try {
			var i, keys = [], descs = [], arr = [], texts = [], snap = {};
			for (i = 0; i < 1749; i++) {
				keys.push("key_" + i);
				descs.push("描述 " + i);
				arr.push("key_" + i);
			}
			for (i = 0; i < 4434; i++) texts.push(i % 3 ? "item_" + i : "block_" + i);
			for (i = 0; i < 3000; i++) snap["cmd_" + i] = "description " + i;
			function jsClassify(k, d, ps) {
				var out = [], i, key, desc, p = String(ps);
				for (i = 0; i < k.length; i++) {
					key = k[i];
					if (p == key || p.indexOf(key + " ") == 0) { out.push(1); continue; }
					desc = (i < d.length && d[i]) ? d[i] : "";
					out.push((key.indexOf(p) >= 0 || desc.indexOf(p) >= 0) ? 2 : 0);
				}
				return out;
			}
			function jsClassifyArray(a, ps) {
				var flags = [], length = -1, canFinish = false, i, e, p = String(ps);
				for (i = 0; i < a.length; i++) {
					e = a[i];
					if (e == null) { flags.push(0); continue; }
					if (p == e || p.indexOf(e + " ") == 0) {
						flags.push(1); canFinish = true; length = Math.max(length, e.length);
					} else if (e.indexOf(p) == 0) {
						flags.push(2); length = Math.max(length, p.length);
					} else flags.push(0);
				}
				return { flags: flags, length: length, canFinish: canFinish };
			}
			function jsFilterTexts(a, ps) {
				var out = [], i, p = String(ps);
				for (i = 0; i < a.length; i++) out.push(a[i] != null && String(a[i]).indexOf(p) >= 0);
				return out;
			}
			function jsCommands(t, p) {
				var out = [], k;
				for (k in t) if (k.indexOf(p) >= 0 || String(t[k]).indexOf(p) >= 0) out.push(k);
				out.sort();
				return out;
			}
			function measure(fn) {
				var t0 = Date.now(), n, r;
				for (n = 0; n < rounds; n++) r = fn();
				return { ms: Date.now() - t0, result: r };
			}
			var prefix = "key_1";
			// 预热 worker 数据缓存，不计入计时
			QuickAccel.classify(keys, descs, prefix);
			QuickAccel.classifyArray(arr, prefix);
			QuickAccel.filterTexts(texts, prefix);
			QuickAccel.matchCommandNames(snap, prefix);

			var cases = [
				{
					name: "classify", js: function () { return jsClassify(keys, descs, prefix); },
					quick: function () { return QuickAccel.classify(keys, descs, prefix); }
				},
				{
					name: "classifyArray", js: function () { return jsClassifyArray(arr, prefix); },
					quick: function () { return QuickAccel.classifyArray(arr, prefix); }
				},
				{
					name: "filterTexts", js: function () { return jsFilterTexts(texts, prefix); },
					quick: function () { return QuickAccel.filterTexts(texts, prefix); }
				},
				{
					name: "commands", js: function () { return jsCommands(snap, "cmd_1"); },
					quick: function () { return QuickAccel.matchCommandNames(snap, "cmd_1"); }
				}
			];
			var out = [], c, jsM, qM, same, jsS, qS, dbg = [];
			for (i = 0; i < cases.length; i++) {
				c = cases[i];
				jsM = measure(c.js);
				qM = measure(c.quick);
				jsS = JSON.stringify(jsM.result);
				qS = JSON.stringify(qM.result);
				same = jsS === qS;
				out.push(c.name + " rhino=" + (jsM.ms / rounds).toFixed(2) + "ms quick=" +
					(qM.ms / rounds).toFixed(2) + "ms same=" + same);
				if (!same) dbg.push(c.name + " js=" + (jsS || "null").slice(0, 80) + " q=" + (qS || "null").slice(0, 80));
			}
			var summary = out.join(" | ");
			if (dbg.length) summary += " DEBUG " + dbg.join(" ; ");
			out = "QuickLarge n=1749/4434/3000 " + summary;
			try { android.util.Log.i("Script", out); } catch (e) { }
			return out;
		} catch (e) {
			return "QuickLarge error: " + e;
		}
	},
	/** 调试控制台用：测量 filter 结果写进 SimpleListAdapter 的成本 */
	benchIDListAdapter: function () {
		try {
			var lib = CA.IntelliSense.library, names = [], prefixes = ["minecraft:", "block", "item_1", "zzz"];
			var i, j, e, l, t, arr, built, results = [];
			for (i = 0; i < lib.idlist.length; i++) {
				e = lib.idlist[i];
				l = e.lists || (e.list ? [e.list] : []);
				for (j = 0; j < l.length; j++) names.push(l[j]);
			}
			built = CA.buildIDList(names);
			var adpt = SimpleListAdapter.getController(new SimpleListAdapter([], function (holder) {
				return new G.TextView(ctx);
			}, function () { }, null, true));
			for (i = 0; i < prefixes.length; i++) {
				arr = CA.filterIDList(built.texts, prefixes[i]);
				t = Date.now();
				adpt.setArray(arr);
				results.push("[" + prefixes[i] + "]=" + arr.length + "/" + (Date.now() - t) + "ms");
			}
			results = "IDListAdapterBench n=" + built.ids.length + " " + results.join(", ");
			try { android.util.Log.i("Script", results); } catch (e) { }
			return results;
		} catch (e) {
			return "IDListAdapterBench error: " + e;
		}
	},
	/** 调试控制台用：用当前 ID 表跑 buildIDList / filterIDList 并打日志
	 *  控制台输入 DebugUtils.benchIDList() 即可；rounds 参数保留给以后重复计时
	 */
	benchIDList: function (rounds) {
		try {
			var lib = CA.IntelliSense.library, names = [], prefixes = ["", "minecraft:", "block", "item_1", "zzz"];
			var i, j, e, l, t, arr, results = [], built, buildMs, total = 0;
			for (i = 0; i < lib.idlist.length; i++) {
				e = lib.idlist[i];
				l = e.lists || (e.list ? [e.list] : []);
				for (j = 0; j < l.length; j++) names.push(l[j]);
			}
			t = Date.now();
			built = CA.buildIDList(names);
			buildMs = Date.now() - t;
			for (i = 0; i < prefixes.length; i++) {
				t = Date.now();
				arr = CA.filterIDList(built.texts, prefixes[i]);
				results.push("[" + prefixes[i] + "]=" + arr.length + "/" + (Date.now() - t) + "ms");
				total += arr.length;
			}
			results = "IDListBench n=" + built.ids.length + " build=" + buildMs + "ms filter: " + results.join(", ");
			try { android.util.Log.i("Script", results); } catch (e) { }
			return results;
		} catch (e) {
			return "IDListBench error: " + e;
		}
	},
	/** 调试控制台用：批量导出 bundleStrings Rhino vs QuickJS A/B，只测不接生产 */
	benchBatchBundle: function () {
		try {
			if (typeof QuickAccel === "undefined" || !QuickAccel.available) return "BatchBundle unavailable";
			var i, parts = [[], [], []], a, b;
			for (i = 0; i < 24; i++) parts[0].push("line_" + i + ":");
			for (i = 0; i < 16; i++) parts[1].push("x" + i);
			for (i = 0; i < 24; i++) parts[2].push("_" + i);
			function jsBundle(array) {
				var msp = [], msi, msn, r = [], cur, i;
				if (!array) return r;
				for (i = 0; i < array.length; i++) if (Array.isArray(array[i])) msp.push(i);
				if (msp.length == 0) { if (array.length == 0) array = [""]; array[0] = [array[0]]; msp.push(0); }
				msi = new Array(msp.length); msn = new Array(msp.length);
				for (i = 0; i < msp.length; i++) { msi[i] = 0; msn[i] = array[msp[i]].length; }
				while (msi[0] < msn[0]) {
					cur = array.slice();
					for (i = 0; i < msp.length; i++) cur[msp[i]] = cur[msp[i]][msi[i]];
					r.push(cur.join(""));
					msi[msi.length - 1]++;
					for (i = msi.length - 1; i > 0; i--) {
						if (msi[i] >= msn[i]) { msi[i] = 0; msi[i - 1]++; } else break;
					}
				}
				return r;
			}
			var t0 = Date.now();
			var rhino = jsBundle(parts);
			var rhinoMs = Date.now() - t0;
			var t1 = Date.now();
			var quick = QuickAccel.bundleStrings(parts);
			var quickMs = Date.now() - t1;
			var same = !!quick && quick.length === rhino.length;
			if (same) {
				for (i = 0; i < rhino.length; i++) if (rhino[i] !== quick[i]) { same = false; break; }
			}
			var out = "BatchBundle n=" + rhino.length + " rhino=" + rhinoMs + "ms quick=" + quickMs + "ms same=" + same;
			try { android.util.Log.i("Script", out); } catch (e) { }
			return out;
		} catch (e) {
			return "BatchBundle error: " + e;
		}
	},
	/** 调试控制台用：ID 首次构建 Rhino vs QuickJS A/B，只测不接生产 */
	benchIDListQuick: function () {
		try {
			var lib = CA.IntelliSense.library, names = [], enums = lib.enums, i, j, e, l;
			for (i = 0; i < lib.idlist.length; i++) {
				e = lib.idlist[i];
				l = e.lists || (e.list ? [e.list] : []);
				for (j = 0; j < l.length; j++) names.push(l[j]);
			}
			if (typeof QuickAccel === "undefined" || !QuickAccel.available) {
				return "IDListQuick unavailable";
			}
			// 先清掉内存缓存，保证测到首次构建
			CA._idListBuildCache = [];
			var t0 = Date.now();
			var rhino = CA.buildIDList(names);
			var rhinoMs = Date.now() - t0;
			var t1 = Date.now();
			var quick = QuickAccel.buildIDList(names, enums);
			var quickMs = Date.now() - t1;
			var same = !!quick && quick.ids.length === rhino.ids.length;
			if (same) {
				for (i = 0; i < rhino.ids.length; i++) {
					if (rhino.ids[i] !== quick.ids[i] || rhino.texts[i] !== quick.texts[i]) {
						same = false;
						break;
					}
				}
			}
			var out = "IDListQuick n=" + rhino.ids.length +
				" rhino=" + rhinoMs + "ms quick=" + quickMs + "ms" +
				" sync=" + (QuickAccel.lastIDSyncMs || 0) + "ms" +
				" workerBuild=" + (QuickAccel.lastIDBuildMs || 0) + "ms" +
				" same=" + same;
			try { android.util.Log.i("Script", out); } catch (e) { }
			return out;
		} catch (e) {
			return "IDListQuick error: " + e;
		}
	},
	updateDebugAction: function () {
		if (CA.settings.enableDebugAction) {
			CA.Actions["debug.action"] = this.debugAction;
			AndroidBridge.Tiles["debug.tile"] = this.debugTile;
		} else {
			delete CA.Actions["debug.action"];
			delete AndroidBridge.Tiles["debug.tile"];
		}
	},
	initialize: function () {
		try {
			this.updateDebugAction();
		} catch (e) { erp(e) }
	}
});