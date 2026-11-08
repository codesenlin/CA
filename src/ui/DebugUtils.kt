package ca.kotlin

/**
 * modules/DebugUtils.js 的 Kotlin/JS 版本。
 *
 * 包含调试控制台（ConsolePanel）、交互式 WebSocket 调试、自定义动作/快捷开关
 * 以及若干基准入口。控制台状态原本挂在 showDebugDialog 的具名函数 self 上，
 * 这里用稳定 state 对象；凡是用到动态 this 的入口（onCommand / 快捷开关 /
 * updateDebugAction）都通过薄 JS wrapper 把真实 this 传回。
 */
private val debugUtilsApi: dynamic = js("({})")
private val debugConsoleState: dynamic = js("({})")
private var debugInteractiveScope: dynamic = js("null")

// ---------------- JS 语义 helper ----------------
private val dbgTruthyJs: dynamic = js("function (v) { return !!v; }")
private val dbgStringJs: dynamic = js("function (v) { return String(v); }")
private val dbgLengthJs: dynamic = js("function (v) { return v.length | 0; }")
private val dbgTypeofJs: dynamic = js("function (v) { return typeof v; }")
private val dbgLooseEqJs: dynamic = js("function (a, b) { return a == b; }")
private val dbgStrictEqJs: dynamic = js("function (a, b) { return a === b; }")
private val dbgStartsWithJs: dynamic = js("function (s, p) { return String(s).startsWith(p); }")
private val dbgLowerJs: dynamic = js("function (s) { return String(s).toLowerCase(); }")
private val dbgSliceJs: dynamic = js("function (s, a, b) { return String(s).slice(a, b); }")
private val dbgIndexOfJs: dynamic = js("function (s, p) { return String(s).indexOf(p); }")
private val dbgForInJs: dynamic = js("function (o, f) { for (var i in o) f(i); }")
private val dbgThisJs: dynamic = js("function (f) { return function () { return f(this, arguments); }; }")
private val dbgBindJs: dynamic = js("function (f, t) { return f.bind(t); }")
private val dbgNewErrorJs: dynamic = js("function (m) { return new Error(m); }")
private val dbgSetLengthJs: dynamic = js("function (v, n) { v.length = n; }")
private val dbgJoinJs: dynamic = js("function (a, s) { return a.join(s); }")
private val dbgToIntJs: dynamic = js("function (v) { return Number(v) | 0; }")
private val dbgNumberJs: dynamic = js("function (v) { return Number(v); }")
private val dbgToFixedJs: dynamic = js("function (v, n) { return v.toFixed(n); }")
private val dbgNowJs: dynamic = js("function () { return Date.now(); }")
private val dbgElapsedJs: dynamic = js("function (t) { return Date.now() - t; }")
private val dbgElapsedPerJs: dynamic = js("function (t, r) { return (Date.now() - t) / r; }")
private val dbgMathMaxJs: dynamic = js("function (a, b) { return Math.max(a, b); }")
private val dbgGlobalThisJs: dynamic = js("function () { return eval.call(null, 'this'); }")
private val dbgEvalCallJs: dynamic = js("function (expr) { return eval.call(null, expr); }")
private val dbgJsonParseJs: dynamic = js("function (s) { return JSON.parse(s); }")
private val dbgJsonStringifyJs: dynamic = js("function (v) { return JSON.stringify(v); }")
private val dbgToSourceJs: dynamic = js("function (v) { return MapScript.toSource(v); }")
private val dbgErrorTextJs: dynamic = js("function (e) { return e + '\\n' + e.stack; }")
private val dbgLogDebugJs: dynamic = js("function (v) { return Log.debug('D', v, 0).join('\\n'); }")
private val dbgLogIJs: dynamic = js("function (m) { try { android.util.Log.i('Script', m); } catch (e) { } }")
private val dbgEvalSpecialJs: dynamic = js(
    "function (expr, name, scope) { return Loader.evalSpecial(expr, name, 0, scope, null); }"
)
private val dbgQuickAccelExistsJs: dynamic = js("function () { return typeof QuickAccel !== 'undefined'; }")
private val dbgNewArrayJs: dynamic = js("function (n) { return new Array(n); }")
private val dbgArraySliceJs: dynamic = js("function (a) { return a.slice(); }")
private val dbgNotLooselyNullJs: dynamic = js("function (v) { return v != null; }")
private val dbgSingleArrayJs: dynamic = js("function (v) { return [v]; }")
private val dbgDpJs: dynamic = js("function (n) { return n * G.dp; }")

private fun dbgTruthy(v: dynamic): Boolean = dbgTruthyJs(v)
private fun dbgString(v: dynamic): dynamic = dbgStringJs(v)
private fun dbgLength(v: dynamic): Int = dbgLengthJs(v)
private fun dbgTypeof(v: dynamic): dynamic = dbgTypeofJs(v)
private fun dbgLooseEq(a: dynamic, b: dynamic): Boolean = dbgLooseEqJs(a, b)
private fun dbgStrictEq(a: dynamic, b: dynamic): Boolean = dbgStrictEqJs(a, b)
private fun dbgStartsWith(s: dynamic, p: dynamic): Boolean = dbgStartsWithJs(s, p)
private fun dbgLower(s: dynamic): dynamic = dbgLowerJs(s)
private fun dbgSlice(s: dynamic, a: dynamic, b: dynamic): dynamic = dbgSliceJs(s, a, b)
private fun dbgIndexOf(s: dynamic, p: dynamic): dynamic = dbgIndexOfJs(s, p)
private fun dbgForIn(o: dynamic, f: dynamic) {
    dbgForInJs(o, f)
}
private fun dbgToInt(v: dynamic): Int = dbgToIntJs(v)
private fun dbgNumber(v: dynamic): dynamic = dbgNumberJs(v)
private fun dbgToFixed(v: dynamic, n: Int): dynamic = dbgToFixedJs(v, n)
private fun dbgNow(): dynamic = dbgNowJs()
private fun dbgElapsed(t: dynamic): dynamic = dbgElapsedJs(t)
private fun dbgElapsedPer(t: dynamic, r: dynamic): dynamic = dbgElapsedPerJs(t, r)
private fun dbgMathMax(a: dynamic, b: dynamic): dynamic = dbgMathMaxJs(a, b)
private fun dbgNotLooselyNull(v: dynamic): Boolean = dbgNotLooselyNullJs(v)
private fun dbgErrorText(e: dynamic): dynamic = dbgErrorTextJs(e)
private fun dbgLogDebug(v: dynamic): dynamic = dbgLogDebugJs(v)
private fun dbgLogI(m: dynamic) {
    dbgLogIJs(m)
}
private fun dbgEvalSpecial(expr: dynamic, name: dynamic, scope: dynamic): dynamic =
    dbgEvalSpecialJs(expr, name, scope)
private fun dbgDp(n: Int): dynamic = dbgDpJs(n)

private fun dbgObject(): dynamic = js("({})")

private fun dbgList(vararg items: dynamic): dynamic {
    val a = js("[]")
    var i = 0
    while (i < items.size) {
        a.push(items[i])
        i++
    }
    return a
}

private fun dbgSize(width: dynamic, height: dynamic): dynamic {
    val o = js("({})")
    o.width = width
    o.height = height
    return o
}

// ---------------- Android / UI helper ----------------
private val dbgNewTextViewJs: dynamic = js("function (c) { return new G.TextView(c); }")
private val dbgNewAbsListParamsJs: dynamic = js(
    "function (w, h) { return new G.AbsListView.LayoutParams(w, h); }"
)
private val dbgNewSpannableJs: dynamic = js(
    "function () { return new G.SpannableStringBuilder(); }"
)
private val dbgNewStyleSpanJs: dynamic = js("function (s) { return new G.StyleSpan(s); }")
private val dbgNewForegroundSpanJs: dynamic = js(
    "function (c) { return new G.ForegroundColorSpan(c); }"
)
private val dbgNewFileJs: dynamic = js(
    "function (dir, name) { return new java.io.File(dir, name); }"
)
private val dbgNewFileOutputStreamJs: dynamic = js(
    "function (f) { return new java.io.FileOutputStream(f); }"
)
private val dbgNewPrintWriterJs: dynamic = js(
    "function (s) { return new java.io.PrintWriter(s); }"
)
private val dbgCreateWSClientJs: dynamic = js(
    "function (uri, handler) { return ScriptInterface.createWSClient(uri, handler); }"
)
private val dbgReadFileJs: dynamic = js(
    "function (path) { return Common.readFile(path, ''); }"
)
private val dbgSendFileJs: dynamic = js("function (f) { AndroidBridge.sendFile(f); }")

private fun dbgNewTextView(context: dynamic): dynamic = dbgNewTextViewJs(context)

// ---------------- 控制台 ----------------

private fun dbgStatusListener(state: dynamic, status: dynamic, arg1: dynamic, arg2: dynamic) {
    if (dbgStrictEq(status, "connecting")) {
        state.print("\n[Interactive Debug] Connecting...")
    } else if (dbgStrictEq(status, "connected")) {
        state.print("\n[Interactive Debug] Connected!")
    } else if (dbgStrictEq(status, "disconnected")) {
        state.print("\n[Interactive Debug] Disconnected!")
    } else if (dbgStrictEq(status, "remoteExec")) {
        state.print("\n[Interactive Debug] Remote:" + arg1)
    } else if (dbgStrictEq(status, "log")) {
        state.print("\n[Interactive Debug] [" + arg1 + "]" + arg2)
    } else if (dbgStrictEq(status, "error")) {
        state.print(
            "\n[Interactive Debug] Error: " + arg1,
            dbgNewForegroundSpanJs(js("Common").theme.criticalcolor)
        )
    }
}

private fun dbgDefaultOnCommand(
    state: dynamic,
    selfInterface: dynamic,
    cmd: dynamic
): dynamic {
    val lc = dbgLower(cmd)
    if (dbgStartsWith(lc, "id ")) {
        try {
            js("DebugUtils").startInteractiveDebug(dbgSlice(cmd, 3, dbgLength(cmd)), selfInterface.debugStatusListener)
        } catch (_e: dynamic) {
            state.print(
                dbgErrorText(_e),
                dbgNewForegroundSpanJs(js("Common").theme.criticalcolor)
            )
        }
    } else {
        return false
    }
    return true
}

private fun dbgBuildDefaultInterface(state: dynamic): dynamic {
    val selfInterface = dbgObject()
    selfInterface.getWelcomeText = { "控制台 - 输入exit以退出" }
    selfInterface.getGlobal = { dbgGlobalThisJs() }

    val evalScope = dbgObject()
    evalScope.print = { str: dynamic ->
        state.print(dbgString(str))
        js("undefined")
    }
    evalScope.println = { str: dynamic ->
        state.print(dbgString(str))
        state.print("\n")
        js("undefined")
    }
    selfInterface.evalScope = evalScope
    selfInterface.evalExpr = dbgThisJs { thisObj: dynamic, args: dynamic ->
        dbgEvalSpecial(args[0], "Console", thisObj.evalScope)
    }
    selfInterface.onCommand = dbgThisJs { thisObj: dynamic, args: dynamic ->
        dbgDefaultOnCommand(state, thisObj, args[0])
    }
    selfInterface.setPrinter = { _printer: dynamic -> js("undefined") }
    selfInterface.debugStatusListener = { status: dynamic, arg1: dynamic, arg2: dynamic ->
        dbgStatusListener(state, status, arg1, arg2)
        js("undefined")
    }
    return selfInterface
}

private fun dbgCls(state: dynamic) {
    dbgSetLengthJs(state.lines, 0)
    dbgSetLengthJs(state.history, 0)
    state.lines.push(dbgNewSpannableJs())
    state.print(
        state["interface"].getWelcomeText(),
        dbgNewStyleSpanJs(js("G.Typeface.BOLD"))
    )
    state.ready("exit")
}

private fun dbgPrint(state: dynamic, str: dynamic, span: dynamic) {
    js("G").ui({
        try {
            val t = state.lines[dbgLength(state.lines) - 1]
            if (dbgTruthy(span)) {
                js("appendSSB")(t, str, span)
            } else {
                t.append(str)
            }
            state.adapter.notifyChange()
            state.prompt.smoothScrollToPosition(dbgLength(state.lines) - 1)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

private fun dbgReady(state: dynamic, cmd: dynamic) {
    val s = dbgString(cmd)
    state.history[dbgLength(state.lines) - 1] = s
    state.lines.push(dbgNewSpannableJs())
    if (dbgLength(state.lines) > dbgToInt(state.LINE_LIMIT)) {
        state.lines.splice(0, dbgLength(state.lines) - dbgToInt(state.LINE_LIMIT) - 1)
        state.history.splice(0, dbgLength(state.history) - dbgToInt(state.LINE_LIMIT) - 1)
    }
    state.hiscur = -1
    state.adapter.notifyChange()
    state.print(
        ">  ",
        dbgNewForegroundSpanJs(js("Common").theme.highlightcolor)
    )
}

private fun dbgExec(state: dynamic, s: dynamic) {
    var t: dynamic = js("undefined")
    val ls = dbgLower(s)
    if (dbgTruthy(state["interface"].onCommand(s))) {
        return
    } else if (dbgLooseEq(ls, "exit")) {
        state.popup.exit()
        return
    } else if (dbgLooseEq(ls, "cls")) {
        state.cls()
        return
    } else if (dbgLooseEq(ls, "ls")) {
        js("JSONEdit").trace(state["interface"].getGlobal())
    } else if (dbgStartsWith(ls, "ls ")) {
        try {
            js("JSONEdit").trace(state["interface"].evalExpr(dbgSlice(s, 3, dbgLength(s))))
        } catch (_e: dynamic) {
            state.print(
                dbgErrorText(_e),
                dbgNewForegroundSpanJs(js("Common").theme.criticalcolor)
            )
        }
    } else if (dbgStartsWith(ls, "cp ")) {
        try {
            t = dbgToSourceJs(state["interface"].evalExpr(dbgSlice(s, 3, dbgLength(s))))
            state.print(t)
            js("Common").setClipboardText(t)
        } catch (_e: dynamic) {
            state.print(
                dbgErrorText(_e),
                dbgNewForegroundSpanJs(js("Common").theme.criticalcolor)
            )
            js("Common").setClipboardText(dbgErrorText(_e))
        }
    } else if (dbgStartsWith(ls, "sn ")) {
        try {
            t = dbgToSourceJs(state["interface"].evalExpr(dbgSlice(s, 3, dbgLength(s))))
            state.print(t)
        } catch (_e: dynamic) {
            t = dbgErrorText(_e)
            state.print(
                t,
                dbgNewForegroundSpanJs(js("Common").theme.criticalcolor)
            )
        }
        val file = dbgNewFileJs(js("ctx").getExternalCacheDir(), "sn.txt")
        val fs = dbgNewPrintWriterJs(dbgNewFileOutputStreamJs(file))
        fs.println(t)
        fs.close()
        try {
            dbgSendFileJs(file)
        } catch (e: dynamic) {
            js("Common").toast("文件已生成于" + file.getAbsolutePath())
        }
    } else if (dbgStartsWith(ls, "exec ")) {
        try {
            t = state["interface"].evalExpr(dbgReadFileJs(dbgSlice(s, 5, dbgLength(s))))
            state.print(dbgLogDebug(t))
        } catch (_e: dynamic) {
            state.print(
                dbgErrorText(_e),
                dbgNewForegroundSpanJs(js("Common").theme.criticalcolor)
            )
        }
    } else if (dbgStartsWith(ls, "#")) {
        js("Threads").run({
            try {
                val tr = state["interface"].evalExpr(dbgSlice(s, 1, dbgLength(s)))
                state.print(dbgLogDebug(tr))
            } catch (_e: dynamic) {
                state.print(
                    dbgErrorText(_e),
                    dbgNewForegroundSpanJs(js("Common").theme.criticalcolor)
                )
            }
            js("G").ui({
                try {
                    state.ready(s)
                } catch (e: dynamic) {
                    js("erp")(e)
                }
                js("undefined")
            })
            js("undefined")
        })
        return
    } else {
        try {
            t = state["interface"].evalExpr(s)
            state.print(dbgLogDebug(t))
        } catch (_e: dynamic) {
            state.print(
                dbgErrorText(_e),
                dbgNewForegroundSpanJs(js("Common").theme.criticalcolor)
            )
        }
    }
    state.ready(s)
}

private fun dbgShowDebugDialog(state: dynamic, interfaceIn: dynamic) {
    js("G").ui({
        try {
            var lastInterface: dynamic = js("undefined")
            if (!dbgTruthy(state.main)) {
                state.LINE_LIMIT = 200
                state.history = js("[]")
                state.lines = js("[]")
                state.defaultInterface = dbgBuildDefaultInterface(state)

                state.vmaker = { holder: dynamic ->
                    val text = dbgNewTextView(js("ctx"))
                    holder.text = text
                    text.setLayoutParams(dbgNewAbsListParamsJs(-1, -2))
                    js("Common").applyStyle(text, "textview_default", 2)
                    text
                }
                state.vbinder = { holder: dynamic, e: dynamic, i: dynamic, a: dynamic ->
                    holder.text.setText(e)
                    holder.text.setPadding(
                        dbgDp(10),
                        if (dbgLooseEq(i, 0)) dbgDp(10) else 0,
                        dbgDp(10),
                        if (dbgLooseEq(i, dbgLength(a) - 1)) dbgDp(10) else 0
                    )
                    js("undefined")
                }
                state.cls = {
                    dbgCls(state)
                    js("undefined")
                }
                state.print = { str: dynamic, span: dynamic ->
                    dbgPrint(state, str, span)
                    js("undefined")
                }
                state.ready = { cmd: dynamic ->
                    dbgReady(state, cmd)
                    js("undefined")
                }
                state.exec = { s: dynamic ->
                    dbgExec(state, s)
                    js("undefined")
                }
                js("ConsolePanel").build(state, "common.Console")
            }
            lastInterface = state["interface"]
            state["interface"] = if (dbgTruthy(interfaceIn)) interfaceIn else state.defaultInterface
            state["interface"].setPrinter(dbgBindJs(state.print, state))
            if (!dbgLooseEq(state["interface"], lastInterface)) state.cls()
            state.popup.enter()
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

private fun dbgTraceStack(): dynamic {
    val s = js("[]")
    val ts = js("java.lang.Thread").getAllStackTraces()
    val it = ts.keySet().iterator()
    val ctid = js("java.lang.Thread").currentThread().getId()
    while (it.hasNext()) {
        val ct = it.next()
        s.push(
            (if (dbgLooseEq(ctid, ct.getId())) "<当前>" else "") +
                "线程" + ct.getId() + ":" + ct.getName() + " (优先级" + ct.getPriority() +
                (if (dbgTruthy(ct.isDaemon())) "守护线程" else "") + ") - " +
                ct.getState().toString()
        )
        val cts = ts.get(ct)
        dbgForIn(cts) { i: dynamic ->
            s.push(" at " + cts[i].toString())
        }
        s.push("")
    }
    return dbgJoinJs(s, "\n")
}

// ---------------- 交互式调试 ----------------

private fun dbgStartInteractiveDebug(
    self: dynamic,
    scope: dynamic,
    uri: dynamic,
    statusListener: dynamic
) {
    if (!dbgStrictEq(js("MapScript").host, "Android")) {
        js("Log").throwError(dbgNewErrorJs("Your device not support Interactive Debug!"))
    }
    if (dbgTruthy(self.wsclient)) {
        js("Log").throwError(dbgNewErrorJs("Channel busy"))
    }
    statusListener("connecting")

    val handler = dbgObject()
    handler.onOpen = { thisObj: dynamic, _handshake: dynamic ->
        try {
            val version = dbgObject()
            version.type = "version"
            version.version = js("CA").version
            version.publishDate = js("CA").publishDate
            thisObj.send(dbgJsonStringifyJs(version))
            js("Log").start({ level: dynamic, text: dynamic ->
                statusListener("log", level, text)
                val line = dbgObject()
                line.type = "println"
                line.level = level
                line.text = text
                thisObj.send(dbgJsonStringifyJs(line))
                js("undefined")
            })
            statusListener("connected")
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }
    handler.onClose = { _thisObj: dynamic, _code: dynamic, _reason: dynamic, _remote: dynamic ->
        try {
            js("Log").stop()
            statusListener("disconnected")
            js("DebugUtils").wsclient = js("null")
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }
    handler.onMessage = { thisObj: dynamic, message: dynamic ->
        try {
            try {
                val o = dbgJsonParseJs(message)
                if (dbgStrictEq(o.type, "exec")) {
                    statusListener("remoteExec", o.cmd)
                    if (dbgStartsWith(o.cmd, "#")) {
                        js("G").ui({
                            try {
                                js("Log").s(
                                    dbgEvalSpecial(
                                        dbgSlice(o.cmd, 1, dbgLength(o.cmd)),
                                        "InteractiveDebugger",
                                        scope
                                    )
                                )
                            } catch (e: dynamic) {
                                js("Log").e(e)
                            }
                            js("undefined")
                        })
                    } else {
                        try {
                            js("Log").s(
                                dbgEvalSpecial(o.cmd, "InteractiveDebugger", scope)
                            )
                        } catch (e: dynamic) {
                            js("Log").e(e)
                        }
                    }
                } else if (dbgStrictEq(o.type, "ping")) {
                    thisObj.send(dbgJsonStringifyJs(dbgObjectWithType("pong")))
                }
            } catch (e: dynamic) {
                js("Log").e(e)
            }
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }
    handler.onError = { _thisObj: dynamic, err: dynamic ->
        js("erp")(err, true)
        statusListener("error", err)
        js("undefined")
    }

    self.wsclient = dbgCreateWSClientJs(uri, handler)
    self.wsclient.connect()
}

private fun dbgObjectWithType(type: dynamic): dynamic {
    val o = dbgObject()
    o.type = type
    return o
}

private fun dbgStopInteractiveDebug(self: dynamic) {
    if (dbgTruthy(self.wsclient) &&
        dbgLooseEq(self.wsclient.getReadyState(), js("org.java_websocket.WebSocket.READYSTATE.OPEN"))
    ) {
        self.wsclient.close()
    }
}

// ---------------- 自定义动作 / 快捷开关 ----------------

private fun dbgShowEditDebugAction(data: dynamic, newCreated: dynamic, callback: dynamic) {
    js("G").ui({
        try {
            var popup: dynamic = js("undefined")
            val nameOpts = dbgObject()
            nameOpts.text = data.name
            nameOpts.hint = "标题"
            nameOpts.singleLine = true
            nameOpts.padding = dbgList(0, 0, 0, 0)
            nameOpts.imeOptions = js("L").EditorInfo("IME_FLAG_NO_FULLSCREEN")
            nameOpts.style = "edittext_default"
            nameOpts.fontSize = 3
            nameOpts.layout = dbgSize(-1, -2)
            val name = js("L").EditText(nameOpts)

            val despOpts = dbgObject()
            despOpts.text = data.desp
            despOpts.hint = "描述（可选）"
            despOpts.singleLine = true
            despOpts.padding = dbgList(0, dbgDp(10), 0, 0)
            despOpts.imeOptions = js("L").EditorInfo("IME_FLAG_NO_FULLSCREEN")
            despOpts.style = "edittext_default"
            despOpts.fontSize = 2
            despOpts.layout = dbgSize(-1, -2)
            val desp = js("L").EditText(despOpts)

            val exprOpts = dbgObject()
            exprOpts.text = data.expr
            exprOpts.hint = "在此输入代码"
            exprOpts.padding = dbgList(0, dbgDp(20), 0, dbgDp(10))
            exprOpts.imeOptions = js("L").EditorInfo("IME_FLAG_NO_FULLSCREEN")
            exprOpts.style = "edittext_default"
            exprOpts.fontSize = 2
            exprOpts.layout = dbgSize(-1, -2)
            val expr = js("L").EditText(exprOpts)

            val enabledOpts = dbgObject()
            enabledOpts.text = "启用"
            enabledOpts.checked = js("Boolean")(data.enabled)
            val enabled = js("L").CheckBox(enabledOpts)

            val titleOpts = dbgObject()
            titleOpts.text = if (dbgTruthy(newCreated)) "新建自定义动作" else "编辑自定义动作"
            titleOpts.padding = dbgList(0, 0, 0, dbgDp(10))
            titleOpts.layout = dbgSize(-1, -2)
            titleOpts.style = "textview_default"
            titleOpts.fontSize = 4

            val buttonOpts = dbgObject()
            buttonOpts.text = "确定"
            buttonOpts.padding = dbgList(dbgDp(10), dbgDp(20), dbgDp(10), dbgDp(20))
            buttonOpts.gravity = js("L").Gravity("center")
            buttonOpts.layout = dbgSize(-1, -2)
            buttonOpts.style = "button_critical"
            buttonOpts.fontSize = 3
            buttonOpts.onClick = { _v: dynamic ->
                try {
                    data.name = dbgString(name.text)
                    if (!dbgTruthy(data.name)) data.name = "自定义动作"
                    data.desp = dbgString(desp.text)
                    data.enabled = js("Boolean")(enabled.checked)
                    data.expr = dbgString(expr.text)
                    if (dbgTruthy(callback)) callback(data)
                    popup.exit()
                } catch (e: dynamic) {
                    js("erp")(e)
                }
                js("undefined")
            }

            val innerConfig = dbgObject()
            innerConfig.orientation = js("L").LinearLayout("vertical")
            innerConfig.padding = dbgList(dbgDp(15), dbgDp(15), dbgDp(15), 0)
            innerConfig.children = dbgList(
                js("L").TextView(titleOpts),
                name,
                desp,
                expr,
                enabled,
                js("L").TextView(buttonOpts)
            )

            val scrollConfig = dbgObject()
            scrollConfig.style = "message_bg"
            scrollConfig.child = js("L").LinearLayout(innerConfig)

            val layout = js("L").ScrollView(scrollConfig)
            popup = js("PopupPage").showDialog("debug.actionEdit", layout, -1, -2)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

private fun dbgShowEditDebugTile(data: dynamic, newCreated: dynamic, callback: dynamic) {
    js("G").ui({
        try {
            var popup: dynamic = js("undefined")
            val labelOpts = dbgObject()
            labelOpts.text = data.label
            labelOpts.hint = "标题(可选)"
            labelOpts.singleLine = true
            labelOpts.padding = dbgList(0, 0, 0, 0)
            labelOpts.imeOptions = js("L").EditorInfo("IME_FLAG_NO_FULLSCREEN")
            labelOpts.style = "edittext_default"
            labelOpts.fontSize = 3
            labelOpts.layout = dbgSize(-1, -2)
            val label = js("L").EditText(labelOpts)

            val updateOpts = dbgObject()
            updateOpts.text = data.update
            updateOpts.hint = "在此输入代码，会在更新快捷设置时触发"
            updateOpts.padding = dbgList(0, dbgDp(20), 0, dbgDp(10))
            updateOpts.imeOptions = js("L").EditorInfo("IME_FLAG_NO_FULLSCREEN")
            updateOpts.style = "edittext_default"
            updateOpts.fontSize = 2
            updateOpts.layout = dbgSize(-1, -2)
            val update = js("L").EditText(updateOpts)

            val clickOpts = dbgObject()
            clickOpts.text = data.onClick
            clickOpts.hint = "在此输入代码，会在点击快捷设置时触发"
            clickOpts.padding = dbgList(0, dbgDp(20), 0, dbgDp(10))
            clickOpts.imeOptions = js("L").EditorInfo("IME_FLAG_NO_FULLSCREEN")
            clickOpts.style = "edittext_default"
            clickOpts.fontSize = 2
            clickOpts.layout = dbgSize(-1, -2)
            val onClick = js("L").EditText(clickOpts)

            val titleOpts = dbgObject()
            titleOpts.text = if (dbgTruthy(newCreated)) "新建自定义快捷开关" else "编辑自定义快捷开关"
            titleOpts.padding = dbgList(0, 0, 0, dbgDp(10))
            titleOpts.layout = dbgSize(-1, -2)
            titleOpts.style = "textview_default"
            titleOpts.fontSize = 4

            val buttonOpts = dbgObject()
            buttonOpts.text = "确定"
            buttonOpts.padding = dbgList(dbgDp(10), dbgDp(20), dbgDp(10), dbgDp(20))
            buttonOpts.gravity = js("L").Gravity("center")
            buttonOpts.layout = dbgSize(-1, -2)
            buttonOpts.style = "button_critical"
            buttonOpts.fontSize = 3
            buttonOpts.onClick = { _v: dynamic ->
                try {
                    data.label = dbgString(label.text)
                    data.update = dbgString(update.text)
                    data.onClick = dbgString(onClick.text)
                    if (dbgTruthy(callback)) callback(data)
                    popup.exit()
                } catch (e: dynamic) {
                    js("erp")(e)
                }
                js("undefined")
            }

            val innerConfig = dbgObject()
            innerConfig.orientation = js("L").LinearLayout("vertical")
            innerConfig.padding = dbgList(dbgDp(15), dbgDp(15), dbgDp(15), 0)
            innerConfig.children = dbgList(
                js("L").TextView(titleOpts),
                label,
                update,
                onClick,
                js("L").TextView(buttonOpts)
            )

            val scrollConfig = dbgObject()
            scrollConfig.style = "message_bg"
            scrollConfig.child = js("L").LinearLayout(innerConfig)

            val layout = js("L").ScrollView(scrollConfig)
            popup = js("PopupPage").showDialog("debug.tileEdit", layout, -1, -2)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

private fun dbgBuildDebugAction(): dynamic {
    val o = dbgObject()
    o.name = "自定义动作"
    o.description = "点击后会执行指定的代码，可自定义名称与代码"
    o.create = {
        val d = dbgObject()
        d.name = ""
        d.desp = ""
        d.expr = ""
        d.enabled = true
        d
    }
    o.edit = { data: dynamic, newCreated: dynamic, callback: dynamic ->
        js("DebugUtils").showEditDebugAction(data, newCreated, callback)
        js("undefined")
    }
    o.getName = { data: dynamic -> dbgString(data.name) }
    o.getDescription = { data: dynamic -> dbgString(data.desp) }
    o.available = { data: dynamic -> data.enabled }
    o.execute = { data: dynamic ->
        try {
            dbgEvalCallJs(data.expr)
        } catch (e: dynamic) {
            js("erp")(e, true)
            js("Common").toast(
                "无法执行自定义动作:" + data.name + "，错误已保存至错误日志\n" + e
            )
        }
        js("undefined")
    }
    return o
}

private fun dbgBuildDebugTile(): dynamic {
    val o = dbgObject()
    o.name = "自定义快捷开关"
    o.description = "点击后会切换指定功能的开启与关闭"
    o.create = {
        val d = dbgObject()
        d.label = ""
        d.update = ""
        d.onClick = ""
        d
    }
    o.edit = { data: dynamic, newCreated: dynamic, callback: dynamic ->
        js("DebugUtils").showEditDebugTile(data, newCreated, callback)
        js("undefined")
    }
    o.updateTile = dbgThisJs { self: dynamic, args: dynamic ->
        val data = args[0]
        val tile = args[1]
        if (dbgTruthy(data.label)) tile.label = data.label
        self.evaluateTile("update", data, tile)
        js("undefined")
    }
    o.onTileClick = dbgThisJs { self: dynamic, args: dynamic ->
        self.evaluateTile("onClick", args[0], args[1])
        js("undefined")
    }
    o.evaluateTile = dbgThisJs { self: dynamic, args: dynamic ->
        dbgEvaluateTile(self, args[0], args[1], args[2])
    }
    o.stateToString = { tile: dynamic ->
        if (dbgStrictEq(tile.state, tile.STATE_ACTIVE)) {
            "active"
        } else if (dbgStrictEq(tile.state, tile.STATE_INACTIVE)) {
            "inactive"
        } else {
            "unavailable"
        }
    }
    o.parseState = { state: dynamic, tile: dynamic ->
        if (dbgStrictEq(state, "active")) {
            tile.STATE_ACTIVE
        } else if (dbgStrictEq(state, "inactive")) {
            tile.STATE_INACTIVE
        } else if (dbgStrictEq(state, "unavailable")) {
            tile.STATE_UNAVAILABLE
        } else {
            tile.state
        }
    }
    o.invertState = dbgThisJs { self: dynamic, _args: dynamic ->
        if (dbgStrictEq(self.state, "active")) {
            self.state = "inactive"
        } else if (dbgStrictEq(self.state, "inactive")) {
            self.state = "active"
        } else {
            self.state = "unavailable"
        }
        js("undefined")
    }
    return o
}

private fun dbgEvaluateTile(self: dynamic, type: dynamic, data: dynamic, tile: dynamic): dynamic {
    val tileScope = dbgObject()
    tileScope.label = tile.label
    tileScope.subtitle = tile.subtitle
    tileScope.state = self.stateToString(tile)
    tileScope.tile = tile
    tileScope.invertState = self.invertState
    tileScope.update = {
        dbgEvalSpecial(data.update, "DebugTile", tileScope)
        js("undefined")
    }
    tileScope.onClick = {
        dbgEvalSpecial(data.onClick, "DebugTile", tileScope)
        js("undefined")
    }
    try {
        if (dbgStrictEq(type, "onClick")) {
            tileScope.onClick()
        } else {
            tileScope.update()
        }
    } catch (e: dynamic) {
        js("erp")(e, true)
        js("Common").toast("快捷开关表达式计算出错，错误已保存至错误日志\n" + e)
    }
    tile.label = tileScope.label
    tile.subtitle = tileScope.subtitle
    tile.state = self.parseState(tileScope.state, tile)
    return js("undefined")
}

// ---------------- 基准入口 ----------------

private fun dbgBenchMatchString(nIn: dynamic, roundsIn: dynamic): dynamic {
    try {
        val n = dbgToInt(if (dbgTruthy(nIn)) nIn else 1749)
        val rounds = dbgToInt(if (dbgTruthy(roundsIn)) roundsIn else 10)
        val a = dbgObject()
        var i = 0
        val prefix = "minecraft:"
        while (i < n) {
            a["minecraft:item_" + i] = "物品 " + i
            i++
        }
        js("Accel")._matchStringDirect("__warmup__", a, dbgObject())
        var t = dbgNow()
        i = 0
        while (i < rounds) {
            js("Accel")._matchStringDirect(prefix, a, dbgObject())
            i++
        }
        val directMs = dbgElapsedPer(t, rounds)
        t = dbgNow()
        js("Accel").matchString(prefix, a, dbgObject())
        val firstMs = dbgElapsed(t)
        t = dbgNow()
        i = 0
        while (i < rounds) {
            js("Accel").matchString(prefix, a, dbgObject())
            i++
        }
        val cachedMs = dbgElapsedPer(t, rounds)
        val msg = "MatchStringBench n=" + n + " rounds=" + rounds +
            " direct=" + dbgToFixed(directMs, 2) + "ms first=" + firstMs +
            "ms cached=" + dbgToFixed(cachedMs, 2) + "ms"
        dbgLogI(msg)
        return msg
    } catch (e: dynamic) {
        return "MatchStringBench error: " + e
    }
}

private fun dbgRhinoCommandMatch(keys: dynamic, snap: dynamic, p: dynamic): dynamic {
    val out = js("[]")
    var i = 0
    val n = dbgLength(keys)
    while (i < n) {
        val e = keys[i]
        if (dbgIndexOf(e, p) >= 0 || dbgIndexOf(snap[e], p) >= 0) out.push(e)
        i++
    }
    out.sort()
    return out
}

private fun dbgBenchCommandMatch(): dynamic {
    try {
        val snap = js("CA").IntelliSense.library.command_snap
        if (!dbgTruthy(snap)) return "CommandMatchBench: no command_snap"
        val prefixes = dbgList("", "block", "cmd", "minecraft", "zzz")
        var out = js("[]")
        val keys = js("Object").keys(snap)
        val rhinoMatch = { p: dynamic -> dbgRhinoCommandMatch(keys, snap, p) }

        js("Accel").matchCommandNames(snap, "__warmup__")
        try {
            if (dbgQuickAccelExistsJs()) js("QuickAccel").matchCommandNames(snap, "__warmup__")
        } catch (e: dynamic) {
        }

        var i = 0
        val n = dbgLength(prefixes)
        while (i < n) {
            val p = prefixes[i]
            var t = dbgNow()
            val r = rhinoMatch(p)
            val rhinoMs = dbgElapsed(t)
            t = dbgNow()
            val j = js("Accel").matchCommandNames(snap, p)
            val javaMs = dbgElapsed(t)
            var q: dynamic = js("null")
            var qMs: dynamic = js("null")
            try {
                if (dbgQuickAccelExistsJs()) {
                    t = dbgNow()
                    q = js("QuickAccel").matchCommandNames(snap, p)
                    qMs = dbgElapsed(t)
                }
            } catch (e: dynamic) {
            }
            val same = if (dbgTruthy(j) && dbgTruthy(q)) {
                dbgStrictEq(dbgLength(j), dbgLength(q)) &&
                    dbgStrictEq(j[0], q[0]) &&
                    dbgStrictEq(j[dbgLength(j) - 1], q[dbgLength(q) - 1])
            } else {
                "?"
            }
            out.push(
                "[" + p + "] rhino=" + rhinoMs + "ms java=" + javaMs + "ms" +
                    (if (dbgLooseEq(qMs, js("null"))) "" else " quick=" + qMs + "ms") +
                    " r=" + dbgLength(r) + " j=" + (if (dbgTruthy(j)) dbgLength(j) else "null") +
                    " same=" + same
            )
            i++
        }
        out = "CommandMatchBench n=" + dbgLength(keys) + " " + dbgJoinJs(out, " | ")
        dbgLogI(out)
        return out
    } catch (e: dynamic) {
        return "CommandMatchBench error: " + e
    }
}

private fun dbgBenchIDListCompare(): dynamic {
    try {
        val lib = js("CA").IntelliSense.library
        val names = js("[]")
        val prefixes = dbgList("", "minecraft:", "block", "item_1", "zzz")
        var i = 0
        var j = 0
        while (i < dbgLength(lib.idlist)) {
            val e = lib.idlist[i]
            val l = if (dbgTruthy(e.lists)) e.lists else if (dbgTruthy(e.list)) dbgSingleArrayJs(e.list) else js("[]")
            j = 0
            while (j < dbgLength(l)) {
                names.push(l[j])
                j++
            }
            i++
        }
        val built = js("CA").buildIDList(names)
        val texts = built.texts
        val old = { s: dynamic ->
            val arr = js("[]")
            var k = 0
            while (k < dbgLength(texts)) {
                if (dbgIndexOf(texts[k], s) >= 0) arr.push(k)
                k++
            }
            arr
        }
        val out = js("[]")
        i = 0
        while (i < dbgLength(prefixes)) {
            var t = dbgNow()
            old(prefixes[i])
            val oldMs = dbgElapsed(t)
            t = dbgNow()
            js("CA").filterIDList(texts, prefixes[i])
            val newMs = dbgElapsed(t)
            t = dbgNow()
            js("Accel").filter(texts, prefixes[i])
            val javaMs = dbgElapsed(t)
            out.push("[" + prefixes[i] + "] old=" + oldMs + "ms new=" + newMs + "ms java=" + javaMs + "ms")
            i++
        }
        val result = "IDListCompare n=" + dbgLength(texts) +
            " accel=" + js("Accel").available() + " " + dbgJoinJs(out, " | ")
        dbgLogI(result)
        return result
    } catch (e: dynamic) {
        return "IDListCompare error: " + e
    }
}

private fun dbgBenchQuickAccel(): dynamic {
    try {
        val keys = dbgList("stone", "stone_brick", "dirt")
        val descs = dbgList("方块", "", "泥土")
        val enumArr = dbgList("stone", "stone_brick", "dirt")
        val t = dbgNow()
        val flags = js("Accel").classify(keys, descs, "stone")
        val ca = js("Accel").classifyArray(enumArr, "stone")
        js("Accel").setFilterTexts(dbgList("stone", "dirt", "grass"))
        val mask = js("Accel").filterTexts("st")
        val help = dbgObject()
        help.cmd = "命令说明"
        help.help = "帮助"
        val cmds = js("Accel").matchCommandNames(help, "cmd")
        val out = "QuickFallback avail=" + js("Accel").available() +
            " classify=" + dbgJsonStringifyJs(flags) +
            " array=" + dbgJsonStringifyJs(ca) +
            " filter=" + dbgJsonStringifyJs(mask) +
            " cmd=" + dbgJsonStringifyJs(cmds) +
            " " + dbgElapsed(t) + "ms"
        dbgLogI(out)
        return out
    } catch (e: dynamic) {
        return "QuickFallback error: " + e
    }
}

private fun dbgJsClassify(k: dynamic, d: dynamic, ps: dynamic): dynamic {
    val out = js("[]")
    val p = dbgString(ps)
    var i = 0
    val n = dbgLength(k)
    while (i < n) {
        val key = k[i]
        if (dbgLooseEq(p, key) || dbgStartsWith(p, key + " ")) {
            out.push(1)
            i++
            continue
        }
        val desc = if (i < dbgLength(d) && dbgTruthy(d[i])) d[i] else ""
        out.push(if (dbgIndexOf(key, p) >= 0 || dbgIndexOf(desc, p) >= 0) 2 else 0)
        i++
    }
    return out
}

private fun dbgJsClassifyArray(a: dynamic, ps: dynamic): dynamic {
    val flags = js("[]")
    var length: dynamic = -1
    var canFinish = false
    val p = dbgString(ps)
    var i = 0
    val n = dbgLength(a)
    while (i < n) {
        val e = a[i]
        if (dbgLooseEq(e, js("null"))) {
            flags.push(0)
            i++
            continue
        }
        if (dbgLooseEq(p, e) || dbgStartsWith(p, e + " ")) {
            flags.push(1)
            canFinish = true
            length = dbgMathMax(length, dbgLength(e))
        } else if (dbgLooseEq(dbgIndexOf(e, p), 0)) {
            flags.push(2)
            length = dbgMathMax(length, dbgLength(p))
        } else {
            flags.push(0)
        }
        i++
    }
    val result = dbgObject()
    result.flags = flags
    result.length = length
    result.canFinish = canFinish
    return result
}

private fun dbgJsFilterTexts(a: dynamic, ps: dynamic): dynamic {
    val out = js("[]")
    val p = dbgString(ps)
    var i = 0
    val n = dbgLength(a)
    while (i < n) {
        out.push(dbgNotLooselyNull(a[i]) && dbgIndexOf(a[i], p) >= 0)
        i++
    }
    return out
}

private fun dbgJsCommands(t: dynamic, p: dynamic): dynamic {
    val out = js("[]")
    dbgForIn(t) { k: dynamic ->
        if (dbgIndexOf(k, p) >= 0 || dbgIndexOf(t[k], p) >= 0) out.push(k)
    }
    out.sort()
    return out
}

private fun dbgMeasure(fn: dynamic, rounds: Int): dynamic {
    val t0 = dbgNow()
    var r: dynamic = js("undefined")
    var n = 0
    while (n < rounds) {
        r = fn()
        n++
    }
    val result = dbgObject()
    result.ms = dbgElapsed(t0)
    result.result = r
    return result
}

private fun dbgBenchQuickAccelLarge(roundsIn: dynamic): dynamic {
    try {
        val rounds = dbgToInt(if (dbgTruthy(roundsIn)) roundsIn else 20)
        val keys = js("[]")
        val descs = js("[]")
        val arr = js("[]")
        val texts = js("[]")
        val snap = dbgObject()
        var i = 0
        while (i < 1749) {
            keys.push("key_" + i)
            descs.push("描述 " + i)
            arr.push("key_" + i)
            i++
        }
        i = 0
        while (i < 4434) {
            texts.push(if (i % 3 != 0) "item_" + i else "block_" + i)
            i++
        }
        i = 0
        while (i < 3000) {
            snap["cmd_" + i] = "description " + i
            i++
        }

        val jsClassify = { k: dynamic, d: dynamic, ps: dynamic -> dbgJsClassify(k, d, ps) }
        val jsClassifyArray = { a: dynamic, ps: dynamic -> dbgJsClassifyArray(a, ps) }
        val jsFilterTexts = { a: dynamic, ps: dynamic -> dbgJsFilterTexts(a, ps) }
        val jsCommands = { t: dynamic, p: dynamic -> dbgJsCommands(t, p) }
        val measure = { fn: dynamic -> dbgMeasure(fn, rounds) }

        val prefix = "key_1"
        js("QuickAccel").classify(keys, descs, prefix)
        js("QuickAccel").classifyArray(arr, prefix)
        js("QuickAccel").filterTexts(texts, prefix)
        js("QuickAccel").matchCommandNames(snap, prefix)

        val cases = js("[]")
        val classify = dbgObject()
        classify.name = "classify"
        classify.js = { jsClassify(keys, descs, prefix) }
        classify.quick = { js("QuickAccel").classify(keys, descs, prefix) }
        cases.push(classify)

        val classifyArray = dbgObject()
        classifyArray.name = "classifyArray"
        classifyArray.js = { jsClassifyArray(arr, prefix) }
        classifyArray.quick = { js("QuickAccel").classifyArray(arr, prefix) }
        cases.push(classifyArray)

        val filterTexts = dbgObject()
        filterTexts.name = "filterTexts"
        filterTexts.js = { jsFilterTexts(texts, prefix) }
        filterTexts.quick = { js("QuickAccel").filterTexts(texts, prefix) }
        cases.push(filterTexts)

        val commands = dbgObject()
        commands.name = "commands"
        commands.js = { jsCommands(snap, "cmd_1") }
        commands.quick = { js("QuickAccel").matchCommandNames(snap, "cmd_1") }
        cases.push(commands)

        val out = js("[]")
        val dbg = js("[]")
        i = 0
        while (i < dbgLength(cases)) {
            val c = cases[i]
            val jsM = measure(c.js)
            val qM = measure(c.quick)
            val jsS = dbgJsonStringifyJs(jsM.result)
            val qS = dbgJsonStringifyJs(qM.result)
            val same = dbgStrictEq(jsS, qS)
            out.push(
                c.name + " rhino=" + dbgToFixed(dbgNumber(jsM.ms) / rounds, 2) + "ms quick=" +
                    dbgToFixed(dbgNumber(qM.ms) / rounds, 2) + "ms same=" + same
            )
            if (!same) {
                dbg.push(
                    c.name + " js=" + dbgSlice(jsS, 0, 80) + " q=" + dbgSlice(qS, 0, 80)
                )
            }
            i++
        }
        var summary: dynamic = dbgJoinJs(out, " | ")
        if (dbgLength(dbg) != 0) summary = summary + " DEBUG " + dbgJoinJs(dbg, " ; ")
        val result = "QuickLarge n=1749/4434/3000 " + summary
        dbgLogI(result)
        return result
    } catch (e: dynamic) {
        return "QuickLarge error: " + e
    }
}

private fun dbgBenchIDListAdapter(): dynamic {
    try {
        val lib = js("CA").IntelliSense.library
        val names = js("[]")
        val prefixes = dbgList("minecraft:", "block", "item_1", "zzz")
        var i = 0
        var j = 0
        while (i < dbgLength(lib.idlist)) {
            val e = lib.idlist[i]
            val l = if (dbgTruthy(e.lists)) e.lists else if (dbgTruthy(e.list)) dbgSingleArrayJs(e.list) else js("[]")
            j = 0
            while (j < dbgLength(l)) {
                names.push(l[j])
                j++
            }
            i++
        }
        val built = js("CA").buildIDList(names)
        val maker = { _holder: dynamic -> dbgNewTextView(js("ctx")) }
        val binder = { _holder: dynamic, _e: dynamic, _i: dynamic, _a: dynamic -> js("undefined") }
        val adpt = js("SimpleListAdapter").getController(
            js("function (a, b, c, d, e) { return new SimpleListAdapter(a, b, c, d, e); }")(
                js("[]"), maker, binder, js("null"), true
            )
        )
        val results = js("[]")
        i = 0
        while (i < dbgLength(prefixes)) {
            val arr = js("CA").filterIDList(built.texts, prefixes[i])
            val t = dbgNow()
            adpt.setArray(arr)
            results.push("[" + prefixes[i] + "]=" + dbgLength(arr) + "/" + dbgElapsed(t) + "ms")
            i++
        }
        val result = "IDListAdapterBench n=" + dbgLength(built.ids) + " " + dbgJoinJs(results, ", ")
        dbgLogI(result)
        return result
    } catch (e: dynamic) {
        return "IDListAdapterBench error: " + e
    }
}

private fun dbgBenchIDList(rounds: dynamic): dynamic {
    try {
        val lib = js("CA").IntelliSense.library
        val names = js("[]")
        val prefixes = dbgList("", "minecraft:", "block", "item_1", "zzz")
        var i = 0
        var j = 0
        while (i < dbgLength(lib.idlist)) {
            val e = lib.idlist[i]
            val l = if (dbgTruthy(e.lists)) e.lists else if (dbgTruthy(e.list)) dbgSingleArrayJs(e.list) else js("[]")
            j = 0
            while (j < dbgLength(l)) {
                names.push(l[j])
                j++
            }
            i++
        }
        var t = dbgNow()
        val built = js("CA").buildIDList(names)
        val buildMs = dbgElapsed(t)
        val results = js("[]")
        var total = 0
        i = 0
        while (i < dbgLength(prefixes)) {
            t = dbgNow()
            val arr = js("CA").filterIDList(built.texts, prefixes[i])
            results.push("[" + prefixes[i] + "]=" + dbgLength(arr) + "/" + dbgElapsed(t) + "ms")
            total += dbgLength(arr)
            i++
        }
        val result = "IDListBench n=" + dbgLength(built.ids) + " build=" + buildMs +
            "ms filter: " + dbgJoinJs(results, ", ")
        dbgLogI(result)
        return result
    } catch (e: dynamic) {
        return "IDListBench error: " + e
    }
}

private fun dbgBenchLargeSearch(nIn: dynamic): dynamic {
    try {
        if (!dbgQuickAccelExistsJs() || !dbgTruthy(js("QuickAccel").available)) {
            return "LargeSearch unavailable"
        }
        val n = dbgToInt(if (dbgTruthy(nIn)) nIn else 12000)
        val texts = js("[]")
        val query = "item_99"
        var i = 0
        while (i < n) {
            texts.push("item_" + i + " description text " + (i % 97))
            i++
        }
        val jsSearch = {
            val out = js("[]")
            var k = 0
            while (k < dbgLength(texts)) {
                if (dbgIndexOf(texts[k], query) >= 0) out.push(k)
                k++
            }
            out
        }
        var t0 = dbgNow()
        val js = jsSearch()
        val jsMs = dbgElapsed(t0)
        var t1 = dbgNow()
        var quick = js("QuickAccel").searchTexts(texts, query)
        val firstMs = dbgElapsed(t1)
        t1 = dbgNow()
        quick = js("QuickAccel").searchTexts(texts, query)
        val warmMs = dbgElapsed(t1)
        var same = dbgTruthy(quick) && dbgStrictEq(dbgLength(quick), dbgLength(js))
        if (same) {
            i = 0
            while (i < dbgLength(js)) {
                if (!dbgStrictEq(js[i], quick[i])) {
                    same = false
                    break
                }
                i++
            }
        }
        val out = "LargeSearch n=" + n + " rhino=" + jsMs + "ms quickFirst=" + firstMs +
            "ms quickWarm=" + warmMs + "ms hits=" + dbgLength(js) + " same=" + same
        dbgLogI(out)
        return out
    } catch (e: dynamic) {
        return "LargeSearch error: " + e
    }
}

private fun dbgJsBundle(arrayIn: dynamic): dynamic {
    var array = arrayIn
    val r = js("[]")
    if (!dbgTruthy(array)) return r
    val msp = js("[]")
    var i = 0
    while (i < dbgLength(array)) {
        if (js("Array").isArray(array[i])) msp.push(i)
        i++
    }
    if (dbgLength(msp) == 0) {
        if (dbgLength(array) == 0) array = dbgSingleArrayJs("")
        array[0] = dbgSingleArrayJs(array[0])
        msp.push(0)
    }
    val msi = dbgNewArrayJs(dbgLength(msp))
    val msn = dbgNewArrayJs(dbgLength(msp))
    i = 0
    while (i < dbgLength(msp)) {
        msi[i] = 0
        msn[i] = dbgLength(array[msp[i]])
        i++
    }
    while (msi[0] < msn[0]) {
        val cur = dbgArraySliceJs(array)
        i = 0
        while (i < dbgLength(msp)) {
            cur[msp[i]] = cur[msp[i]][msi[i]]
            i++
        }
        r.push(dbgJoinJs(cur, ""))
        val last = dbgLength(msi) - 1
        msi[last] = msi[last] + 1
        var k = last
        while (k > 0) {
            if (msi[k] >= msn[k]) {
                msi[k] = 0
                msi[k - 1] = msi[k - 1] + 1
            } else {
                break
            }
            k--
        }
    }
    return r
}

private fun dbgBenchBatchBundle(): dynamic {
    try {
        if (!dbgQuickAccelExistsJs() || !dbgTruthy(js("QuickAccel").available)) {
            return "BatchBundle unavailable"
        }
        val parts = js("[]")
        parts.push(js("[]"))
        parts.push(js("[]"))
        parts.push(js("[]"))
        var i = 0
        while (i < 24) {
            parts[0].push("line_" + i + ":")
            i++
        }
        i = 0
        while (i < 16) {
            parts[1].push("x" + i)
            i++
        }
        i = 0
        while (i < 24) {
            parts[2].push("_" + i)
            i++
        }

        var t0 = dbgNow()
        val rhino = dbgJsBundle(parts)
        val rhinoMs = dbgElapsed(t0)
        var t1 = dbgNow()
        val quick = js("QuickAccel").bundleStrings(parts)
        val quickMs = dbgElapsed(t1)
        var same = dbgTruthy(quick) && dbgStrictEq(dbgLength(quick), dbgLength(rhino))
        if (same) {
            i = 0
            while (i < dbgLength(rhino)) {
                if (!dbgStrictEq(rhino[i], quick[i])) {
                    same = false
                    break
                }
                i++
            }
        }

        val complexParts = js("[]")
        complexParts.push(dbgList("A", "B"))
        val syncmap = dbgObject()
        syncmap.type = "syncmap"
        syncmap.arr = dbgList("1", "2")
        syncmap.target = 0
        complexParts.push(syncmap)
        complexParts.push(dbgList("X", "Y"))
        val map = dbgObject()
        map.type = "map"
        map.index = 0
        complexParts.push(map)
        val complex = js("QuickAccel").bundleStrings(complexParts)
        val expected = dbgList("A1XA", "A1YA", "B2XB", "B2YB")
        var complexOk = false
        if (dbgTruthy(complex) && dbgStrictEq(dbgLength(complex), dbgLength(expected))) {
            complexOk = true
            i = 0
            while (i < dbgLength(expected)) {
                if (!dbgStrictEq(complex[i], expected[i])) {
                    complexOk = false
                    break
                }
                i++
            }
        }
        val out = "BatchBundle n=" + dbgLength(rhino) + " rhino=" + rhinoMs + "ms quick=" + quickMs +
            "ms same=" + same + " complex=" + complexOk +
            (if (dbgTruthy(complex) && !complexOk) " " + dbgJsonStringifyJs(complex) else "")
        dbgLogI(out)
        return out
    } catch (e: dynamic) {
        return "BatchBundle error: " + e
    }
}

private fun dbgBenchIDListQuick(): dynamic {
    try {
        val lib = js("CA").IntelliSense.library
        val names = js("[]")
        val enums = lib.enums
        var i = 0
        var j = 0
        while (i < dbgLength(lib.idlist)) {
            val e = lib.idlist[i]
            val l = if (dbgTruthy(e.lists)) e.lists else if (dbgTruthy(e.list)) dbgSingleArrayJs(e.list) else js("[]")
            j = 0
            while (j < dbgLength(l)) {
                names.push(l[j])
                j++
            }
            i++
        }
        if (!dbgQuickAccelExistsJs() || !dbgTruthy(js("QuickAccel").available)) {
            return "IDListQuick unavailable"
        }
        js("CA")._idListBuildCache = js("[]")
        var t0 = dbgNow()
        val rhino = js("CA").buildIDList(names)
        val rhinoMs = dbgElapsed(t0)
        var t1 = dbgNow()
        val quick = js("QuickAccel").buildIDList(names, enums)
        val quickMs = dbgElapsed(t1)
        var same = dbgTruthy(quick) && dbgStrictEq(dbgLength(quick.ids), dbgLength(rhino.ids))
        if (same) {
            i = 0
            while (i < dbgLength(rhino.ids)) {
                if (!dbgStrictEq(rhino.ids[i], quick.ids[i]) || !dbgStrictEq(rhino.texts[i], quick.texts[i])) {
                    same = false
                    break
                }
                i++
            }
        }
        val out = "IDListQuick n=" + dbgLength(rhino.ids) +
            " rhino=" + rhinoMs + "ms quick=" + quickMs + "ms" +
            " sync=" + (if (dbgTruthy(js("QuickAccel").lastIDSyncMs)) js("QuickAccel").lastIDSyncMs else 0) + "ms" +
            " workerBuild=" + (if (dbgTruthy(js("QuickAccel").lastIDBuildMs)) js("QuickAccel").lastIDBuildMs else 0) + "ms" +
            " same=" + same
        dbgLogI(out)
        return out
    } catch (e: dynamic) {
        return "IDListQuick error: " + e
    }
}

private fun dbgUpdateDebugAction(self: dynamic) {
    if (dbgTruthy(js("CA").settings.enableDebugAction)) {
        js("CA").Actions["debug.action"] = self.debugAction
        js("AndroidBridge").Tiles["debug.tile"] = self.debugTile
    } else {
        deleteDynamic(js("CA").Actions, "debug.action")
        deleteDynamic(js("AndroidBridge").Tiles, "debug.tile")
    }
}

private fun deleteDynamic(obj: dynamic, key: String) {
    js("function (o, k) { delete o[k]; }")(obj, key)
}

fun installDebugUtils() {
    debugUtilsApi.debugAction = dbgBuildDebugAction()
    debugUtilsApi.debugTile = dbgBuildDebugTile()

    debugUtilsApi.showDebugDialog = { interfaceIn: dynamic ->
        dbgShowDebugDialog(debugConsoleState, interfaceIn)
        js("undefined")
    }
    debugUtilsApi.traceStack = { dbgTraceStack() }

    debugInteractiveScope = dbgObject()
    debugInteractiveScope.KEY = js("Internal").getKey()

    debugUtilsApi.startInteractiveDebug = dbgThisJs { self: dynamic, args: dynamic ->
        dbgStartInteractiveDebug(self, debugInteractiveScope, args[0], args[1])
        js("undefined")
    }
    debugUtilsApi.stopInteractiveDebug = dbgThisJs { self: dynamic, _args: dynamic ->
        dbgStopInteractiveDebug(self)
        js("undefined")
    }
    debugUtilsApi.showEditDebugAction = { data: dynamic, newCreated: dynamic, callback: dynamic ->
        dbgShowEditDebugAction(data, newCreated, callback)
        js("undefined")
    }
    debugUtilsApi.showEditDebugTile = { data: dynamic, newCreated: dynamic, callback: dynamic ->
        dbgShowEditDebugTile(data, newCreated, callback)
        js("undefined")
    }
    debugUtilsApi.benchMatchString = { n: dynamic, rounds: dynamic -> dbgBenchMatchString(n, rounds) }
    debugUtilsApi.benchCommandMatch = { dbgBenchCommandMatch() }
    debugUtilsApi.benchIDListCompare = { dbgBenchIDListCompare() }
    debugUtilsApi.benchQuickAccel = { dbgBenchQuickAccel() }
    debugUtilsApi.benchQuickAccelLarge = { rounds: dynamic -> dbgBenchQuickAccelLarge(rounds) }
    debugUtilsApi.benchIDListAdapter = { dbgBenchIDListAdapter() }
    debugUtilsApi.benchIDList = { rounds: dynamic -> dbgBenchIDList(rounds) }
    debugUtilsApi.benchLargeSearch = { n: dynamic -> dbgBenchLargeSearch(n) }
    debugUtilsApi.benchBatchBundle = { dbgBenchBatchBundle() }
    debugUtilsApi.benchIDListQuick = { dbgBenchIDListQuick() }
    debugUtilsApi.updateDebugAction = dbgThisJs { self: dynamic, _args: dynamic ->
        dbgUpdateDebugAction(self)
        js("undefined")
    }
    debugUtilsApi.initialize = dbgThisJs { self: dynamic, _args: dynamic ->
        try {
            self.updateDebugAction()
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }

    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("DebugUtils", debugUtilsApi)
    }
}
