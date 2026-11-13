package ca.kotlin.network

import ca.kotlin.core.*


/**
 * WSServer 的 Kotlin/JS 实现。
 *
 * 传输、事件订阅和命令响应由 PEWS bridge 处理；
 * 这里保留控制台、UI 和对外脚本 API。
 */
private val wssApi: dynamic = js("({})")
private val wssConsole: dynamic = js("({})")

// ---------------- JS 语义 helper ----------------
private val wssTruthyJs: dynamic = js("function (v) { return !!v; }")
private val wssStringJs: dynamic = js("function (v) { return String(v); }")
private val wssLengthJs: dynamic = js("function (v) { return v.length | 0; }")
private val wssLowerJs: dynamic = js("function (v) { return String(v).toLowerCase(); }")
private val wssStartsWithJs: dynamic = js("function (s, p) { return s.startsWith(p); }")
private val wssLooseEqJs: dynamic = js("function (a, b) { return a == b; }")
private val wssDeleteJs: dynamic = js("function (o, k) { delete o[k]; }")
private val wssEvalJs: dynamic = js("function (code) { return eval(code); }")
private val wssJsonParseJs: dynamic = js("function (s) { return JSON.parse(s); }")
private val wssJsonStringifyJs: dynamic = js("function (v) { return JSON.stringify(v); }")
private val wssThis2Js: dynamic = js("function (f) { return function (a, b) { return f(this, a, b); }; }")
private val wssForEachIteratorJs: dynamic = js(
    "function (list, f) { var it = list.iterator(); while (it.hasNext()) f(it.next()); }"
)
private val wssIsBindExceptionJs: dynamic = js(
    "function (e) { return e instanceof java.net.BindException; }"
)

// ---------------- Java / Android / UI helper ----------------
private val wssNewConcurrentHashMapJs: dynamic = js(
    "function (a, b, c) { return new java.util.concurrent.ConcurrentHashMap(a, b, c); }"
)
private val wssNewCopyOnWriteArrayListJs: dynamic = js(
    "function () { return new java.util.concurrent.CopyOnWriteArrayList(); }"
)
private val wssCreatePewsBridgeJs: dynamic = js(
    "function (p, l) { return ScriptInterface.createPewsWebSocketBridge(p, l); }"
)
private val wssNewTextViewJs: dynamic = js("function (c) { return new G.TextView(c); }")
private val wssNewAbsListViewParamsJs: dynamic = js(
    "function (w, h) { return new G.AbsListView.LayoutParams(w, h); }"
)
private val wssNewSpannableStringBuilderJs: dynamic = js(
    "function () { return new G.SpannableStringBuilder(); }"
)
private val wssNewStyleSpanJs: dynamic = js("function (s) { return new G.StyleSpan(s); }")
private val wssNewForegroundColorSpanJs: dynamic = js(
    "function (c) { return new G.ForegroundColorSpan(c); }"
)
private val wssDpJs: dynamic = js("function (n) { return n * G.dp; }")
private val wssToIntJs: dynamic = js("function (v) { return Number(v) | 0; }")

private fun wssTruthy(v: dynamic): Boolean = wssTruthyJs(v)
private fun wssString(v: dynamic): dynamic = wssStringJs(v)
private fun wssLength(v: dynamic): Int = wssLengthJs(v)
private fun wssLower(v: dynamic): dynamic = wssLowerJs(v)
private fun wssStartsWith(s: dynamic, p: dynamic): Boolean = wssStartsWithJs(s, p)
private fun wssLooseEq(a: dynamic, b: dynamic): Boolean = wssLooseEqJs(a, b)
private fun wssIsBindException(e: dynamic): Boolean = wssIsBindExceptionJs(e)
private fun wssDp(n: Int): dynamic = wssDpJs(n)
private fun wssToInt(v: dynamic): Int = wssToIntJs(v)

private fun wssIsAvailable(): Boolean = wssApi.server != null && wssTruthy(wssApi.running)

private fun wssIsConnected(): Boolean = wssTruthy(wssApi.server) && wssTruthy(wssApi.server.isConnected())

private fun wssUnload() {
    if (wssIsAvailable()) wssStop()
}

private fun wssOnError(message: dynamic, bindFailure: Boolean) {
    try {
        if (bindFailure && wssApi.port < wssApi.endPort) {
            js("Log").e(message)
            js("Common").toast("在端口" + wssApi.port + "上建立服务器失败，正在尝试其他端口")
            wssApi.port = wssApi.port + 1
            wssApi.start()
        } else {
            js("Common").toast("WebSocket服务器出错，连接已终止\n" + message)
            js("erp")(message, true)
            if (wssTruthy(wssApi.server)) wssStop()
        }
    } catch (e: dynamic) {
        js("erp")(e)
    }
}

private fun wssBuild(port: dynamic) {
    val handler = js("({})")
    handler.onStart = {
        try {
            wssOnStart()
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }
    handler.onOpen = { address: dynamic ->
        try {
            wssOnOpen(address)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }
    handler.onClose = {
        try {
            wssOnClose()
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }
    handler.onEvent = { json: dynamic ->
        try {
            wssOnEvent(json)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }
    handler.onResponse = { id: dynamic, json: dynamic ->
        try {
            wssOnResponse(wssToInt(id), json)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }
    handler.onError = { message: dynamic, bindFailure: dynamic ->
        try {
            wssOnError(message, wssTruthy(bindFailure))
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }
    wssApi.server = wssCreatePewsBridgeJs(port, handler)
    wssApi.server.setConnectionLostTimeout(-1)
    wssApi.server.setTcpNoDelay(true)
}

private fun wssStart(silent: dynamic) {
    if (!wssTruthy(wssApi.port)) wssApi.port = wssApi.startPort
    wssApi.connected = false
    wssApi.remoteAddress = null
    wssApi.silent = silent
    wssApi.build(wssApi.port)
    wssApi.server.start()
}

private fun wssStop() {
    try {
        wssApi.server.stop()
    } catch (e: dynamic) {
        js("Common").toast("无法停止WebSocket服务器\n" + e)
        js("Log").e(e)
    }
    wssApi.server = null
    wssApi.running = false
    wssApi.connected = false
    js("AndroidBridge").notifySettings()
}

private fun wssOnStart() {
    if (!wssTruthy(wssApi.silent)) wssApi.howToUse()
    wssApi.running = true
    js("AndroidBridge").notifySettings()
}

private fun wssOnOpen(address: dynamic) {
    wssApi.connected = true
    wssApi.remoteAddress = address
    wssApi.events.clear()
    wssApi.responsers.clear()
    js("Common").toast("设备" + address + "已连接")
    js("AndroidBridge").notifySettings()
    js("Plugins").emit("WSServer", "connectionOpen")
    js("MCAdapter").initWSServer()
}

private fun wssOnClose() {
    wssApi.connected = false
    wssApi.remoteAddress = null
    js("Common").toast("设备已断开")
    js("AndroidBridge").notifySettings()
    if (wssTruthy(wssApi.showConsole.onClose)) wssApi.showConsole.onClose()
    js("Plugins").emit("WSServer", "connectionClose")
}

private fun wssOnEvent(json: dynamic) {
    val parsed = wssJsonParseJs(json)
    val listeners = wssApi.events.get(parsed.body.eventName)
    if (listeners != null) {
        wssForEachIteratorJs(listeners) { e: dynamic ->
            try {
                e(parsed.body, parsed)
            } catch (err: dynamic) {
                js("erp")(err, true)
            }
            js("undefined")
        }
    }
}

private fun wssOnResponse(callbackId: Int, json: dynamic) {
    val callback = wssApi.responsers.remove(callbackId)
    if (callback != null) {
        try {
            callback.body(wssJsonParseJs(json))
        } catch (e: dynamic) {
            js("erp")(e, true)
        }
    }
}

private fun wssHowToUse() {
    val cmd = wssApi.getConnectCommands()
    val dialog = js("({})")
    dialog.description = "WebSocket服务器已开启。请在客户端输入以下命令之一来连接到服务器。\n" +
        cmd.join("\n") +
        "\n\n用法：\n长按命令助手主界面右下角的按钮可执行主界面输入框中的命令\n\n" +
        "如果显示无法连接请重启命令助手与Minecraft客户端。"
    val buttons = js("[]")
    buttons.push("复制命令")
    buttons.push("关闭")
    dialog.buttons = buttons
    dialog.callback = { i: dynamic ->
        if (i == 0) {
            js("Common").showListChooser(cmd, { j: dynamic ->
                js("Common").setClipboardText(cmd[j])
                js("undefined")
            }, true)
        }
        js("undefined")
    }
    js("Common").showConfirmDialog(dialog)
}

private fun wssGetConnectCommands(): dynamic {
    return js("NetworkUtils").getIps().map({ e: dynamic ->
        "/connect " + e + ":" + wssApi.port
    })
}

private fun wssSubscribeEvent(name: dynamic, callback: dynamic): Boolean {
    if (!wssIsConnected()) return false
    if (!wssTruthy(wssApi.server.subscribeEvent(name))) return false
    var listeners = wssApi.events.get(name)
    if (listeners == null) {
        listeners = wssNewCopyOnWriteArrayListJs()
        wssApi.events.put(name, listeners)
    }
    listeners.add(callback)
    return true
}

private fun wssUnsubscribeEvent(name: dynamic, callback: dynamic): Boolean {
    if (!wssIsConnected()) return false
    val listeners = wssApi.events.get(name)
    if (listeners != null && listeners.remove(callback)) {
        if (listeners.size() == 0) {
            wssApi.events.remove(name)
            wssApi.server.unsubscribeEvent(name)
        }
        return true
    }
    return false
}

private fun wssSendCommand(cmd: dynamic, callback: dynamic): dynamic {
    if (!wssIsConnected()) return null
    val id = wssApi.nextCallbackId
    wssApi.nextCallbackId = id + 1
    wssApi.responsers.put(id, callback)
    if (!wssTruthy(wssApi.server.sendCommand(cmd, id))) {
        wssApi.responsers.remove(id)
        return null
    }
    return id
}

private fun wssCls() {
    wssConsole.lines.length = 0
    wssConsole.history.length = 0
    wssConsole.lines.push(wssNewSpannableStringBuilderJs())
    wssConsole.print("WSServer控制台 - 输入exit以退出", wssNewStyleSpanJs(js("G.Typeface.BOLD")))
    wssConsole.ready("exit")
}

private fun wssPrint(str: dynamic, span: dynamic) {
    val t = wssConsole.lines[wssLength(wssConsole.lines) - 1]
    if (wssTruthy(span)) {
        js("appendSSB")(t, str, span)
    } else {
        t.append(str)
    }
    js("gHandler").post({
        try {
            wssConsole.prompt.smoothScrollToPosition(wssLength(wssConsole.lines) - 1)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

private fun wssReady(cmd: dynamic) {
    val c = wssString(cmd)
    wssConsole.history[wssLength(wssConsole.lines) - 1] = c
    wssConsole.lines.push(wssNewSpannableStringBuilderJs())
    val limit = wssToInt(wssConsole.LINE_LIMIT)
    if (wssLength(wssConsole.lines) > limit) {
        wssConsole.lines.splice(0, wssLength(wssConsole.lines) - limit - 1)
        wssConsole.history.splice(0, wssLength(wssConsole.history) - limit - 1)
    }
    wssConsole.hiscur = -1
    wssConsole.adapter.notifyChange()
    wssConsole.print(">  ", wssNewForegroundColorSpanJs(js("Common").theme.highlightcolor))
}

private fun wssExec(s: dynamic) {
    val lower = wssLower(s)
    var doReady = true
    if (lower == "exit") {
        wssConsole.popup.exit()
        doReady = false
    } else if (lower == "cls") {
        wssConsole.cls()
        doReady = false
    } else if (lower == "close") {
        wssApi.sendCommand("closewebsocket")
    } else if (wssStartsWith(lower, "subscribe ")) {
        val name = s.slice(10)
        val receiver: dynamic = { body: dynamic, message: dynamic ->
            js("G").ui({
                try {
                    val t = if (wssTruthy(body.eventName)) body.eventName else message.eventName
                    wssDeleteJs(body, "eventName")
                    wssConsole.print(
                        js("Log").debug(t, body, 0).join("\n"),
                        wssNewForegroundColorSpanJs(js("Common").theme.promptcolor)
                    )
                    wssConsole.ready(null)
                } catch (e: dynamic) {
                    js("erp")(e)
                }
                js("undefined")
            })
            js("undefined")
        }
        wssConsole.eventReceiver[name] = receiver
        wssApi.subscribeEvent(name, receiver)
        wssConsole.print("Event subscribed!")
    } else if (wssStartsWith(lower, "unsubscribe ")) {
        val name = s.slice(12)
        wssApi.unsubscribeEvent(name, wssConsole.eventReceiver[name])
        wssConsole.print("Event unsubscribed!")
    } else if (wssStartsWith(lower, "/")) {
        val startTime = js("Date").now()
        wssApi.sendCommand(s.slice(1), { body: dynamic ->
            val timer = js("Date").now() - startTime
            js("G").ui({
                try {
                    wssConsole.print("Client responded in " + timer + "ms\n")
                    wssConsole.print(js("Log").debug("Command", body, 0).join("\n"))
                    wssConsole.ready(null)
                } catch (e: dynamic) {
                    js("erp")(e)
                }
                js("undefined")
            })
            js("undefined")
        })
    } else {
        try {
            val t = wssEvalJs(s)
            wssConsole.print(js("Log").debug("D", t, 0).join("\n"))
        } catch (e: dynamic) {
            wssConsole.print(
                e + "\n" + e.stack,
                wssNewForegroundColorSpanJs(js("Common").theme.criticalcolor)
            )
        }
    }
    if (doReady) wssConsole.ready(s)
}

private fun wssConsoleOnClose() {
    js("G").ui({
        try {
            if (wssTruthy(wssConsole.popup)) wssConsole.popup.exit()
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

private fun wssBuildConsole() {
    wssConsole.LINE_LIMIT = 200
    wssConsole.history = js("[]")
    wssConsole.lines = js("[]")
    wssConsole.eventReceiver = js("({})")
    wssConsole.vmaker = { holder: dynamic ->
        val text = wssNewTextViewJs(js("ctx"))
        holder.text = text
        text.setLayoutParams(wssNewAbsListViewParamsJs(-1, -2))
        js("Common").applyStyle(text, "textview_default", 2)
        text
    }
    wssConsole.vbinder = { holder: dynamic, e: dynamic, i: dynamic, a: dynamic ->
        holder.text.setText(e)
        holder.text.setPadding(
            wssDp(10),
            if (wssLooseEq(i, 0)) wssDp(10) else 0,
            wssDp(10),
            if (wssLooseEq(i, wssLength(a) - 1)) wssDp(10) else 0
        )
        js("undefined")
    }
    wssConsole.cls = {
        wssCls()
        js("undefined")
    }
    wssConsole.print = { str: dynamic, span: dynamic ->
        wssPrint(str, span)
        js("undefined")
    }
    wssConsole.ready = { cmd: dynamic ->
        wssReady(cmd)
        js("undefined")
    }
    wssConsole.exec = { s: dynamic ->
        wssExec(s)
        js("undefined")
    }
    wssApi.showConsole.onClose = {
        wssConsoleOnClose()
        js("undefined")
    }

    val opts = js("({})")
    opts.afterBuild = {
        wssConsole.cls()
        js("undefined")
    }
    js("ConsolePanel").build(wssConsole, "wsserver.Console", opts)
}

private fun wssShowConsole() {
    js("G").ui({
        try {
            if (!wssTruthy(wssConsole.main)) wssBuildConsole()
            if (!wssIsConnected()) {
                js("Common").toast("请先连接上WSServer")
            } else {
                wssConsole.popup.enter()
            }
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

fun installWSServer() {
    wssApi.startPort = 19134
    wssApi.endPort = 19165
    wssApi.connected = false
    wssApi.remoteAddress = null
    wssApi.nextCallbackId = 1
    wssApi.events = wssNewConcurrentHashMapJs(8, 0.75, 4)
    wssApi.responsers = wssNewConcurrentHashMapJs(32, 0.75, 16)

    wssApi.unload = {
        wssUnload()
        js("undefined")
    }
    wssApi.isAvailable = { wssIsAvailable() }
    wssApi.isConnected = { wssIsConnected() }
    wssApi.build = { port: dynamic ->
        wssBuild(port)
        js("undefined")
    }
    wssApi.start = { silent: dynamic ->
        wssStart(silent)
        js("undefined")
    }
    wssApi.stop = {
        wssStop()
        js("undefined")
    }
    wssApi.onStart = {
        wssOnStart()
        js("undefined")
    }
    wssApi.onOpen = { address: dynamic ->
        wssOnOpen(address)
        js("undefined")
    }
    wssApi.onClose = {
        wssOnClose()
        js("undefined")
    }
    wssApi.onEvent = { json: dynamic ->
        wssOnEvent(json)
        js("undefined")
    }
    wssApi.onResponse = { id: dynamic, json: dynamic ->
        wssOnResponse(wssToInt(id), json)
        js("undefined")
    }
    wssApi.onError = { message: dynamic, bindFailure: dynamic ->
        wssOnError(message, wssTruthy(bindFailure))
        js("undefined")
    }
    wssApi.howToUse = {
        wssHowToUse()
        js("undefined")
    }
    wssApi.getConnectCommands = { wssGetConnectCommands() }
    wssApi.subscribeEvent = { name: dynamic, callback: dynamic ->
        wssSubscribeEvent(name, callback)
    }
    wssApi.unsubscribeEvent = { name: dynamic, callback: dynamic ->
        wssUnsubscribeEvent(name, callback)
    }
    wssApi.sendCommand = { cmd: dynamic, callback: dynamic -> wssSendCommand(cmd, callback) }
    wssApi.showConsole = {
        wssShowConsole()
        js("undefined")
    }

    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("WSServer", wssApi)
    }
}