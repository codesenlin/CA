package ca.kotlin

/**
 * modules/network/WSServer.js 的 Kotlin/JS 版本。
 *
 * WebSocket 服务器、事件订阅表、命令响应表与控制台都保持原 API；
 * onError 回调在原实现里读取动态 this.server，这里用薄 wrapper 保留。
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
private val wssCreateWebSocketHelperJs: dynamic = js(
    "function (p, l) { return ScriptInterface.createWebSocketHelper(p, l); }"
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

private fun wssIsConnected(): Boolean = wssApi.conn != null && wssTruthy(wssApi.conn.isOpen())

private fun wssUnload() {
    if (wssIsAvailable()) wssStop()
}

private fun wssOnError(self: dynamic, err: dynamic) {
    try {
        if (wssIsBindException(err) && wssApi.port < wssApi.endPort) {
            js("Log").e(err)
            js("Common").toast("在端口" + wssApi.port + "上建立服务器失败，正在尝试其他端口")
            wssApi.port = wssApi.port + 1
            wssApi.start()
        } else {
            js("Common").toast("WebSocket服务器出错，连接已终止\n" + err)
            js("erp")(err, true)
            if (wssTruthy(self.server)) wssStop()
        }
    } catch (e: dynamic) {
        js("erp")(e)
    }
}

private fun wssBuild(port: dynamic) {
    val handler = js("({})")
    handler.onOpen = { conn: dynamic, handshake: dynamic ->
        try {
            wssApi.onOpen(conn, handshake)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }
    handler.onClose = { conn: dynamic, code: dynamic, reason: dynamic, remote: dynamic ->
        try {
            wssApi.onClose(conn, code, reason, remote)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }
    handler.onMessage = { conn: dynamic, message: dynamic ->
        try {
            wssApi.onMessage(conn, message)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }
    handler.onError = wssThis2Js { self: dynamic, _conn: dynamic, err: dynamic ->
        wssOnError(self, err)
        js("undefined")
    }
    handler.onStart = {
        try {
            wssApi.onStart()
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }
    wssApi.server = wssCreateWebSocketHelperJs(port, handler)
    wssApi.server.setConnectionLostTimeout(-1)
    wssApi.server.setTcpNoDelay(true)
}

private fun wssStart(silent: dynamic) {
    if (!wssTruthy(wssApi.port)) wssApi.port = wssApi.startPort
    wssApi.conn = null
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
    js("AndroidBridge").notifySettings()
}

private fun wssOnStart() {
    if (!wssTruthy(wssApi.silent)) wssApi.howToUse()
    wssApi.running = true
    js("AndroidBridge").notifySettings()
}

private fun wssOnOpen(conn: dynamic, handshake: dynamic) {
    if (wssApi.conn != null) {
        conn.close(1003, "A client has been binded to CA.")
        js("Common").toast(
            "WebSocket服务器已拒绝设备" + conn.getRemoteSocketAddress() +
                "连接，因为本设备已经和其他设备连接"
        )
    } else {
        wssApi.conn = conn
        wssApi.events.clear()
        wssApi.responsers.clear()
        js("Common").toast("设备" + conn.getRemoteSocketAddress() + "已连接")
        js("AndroidBridge").notifySettings()
        js("Plugins").emit("WSServer", "connectionOpen")
        js("MCAdapter").initWSServer()
    }
}

private fun wssOnClose(conn: dynamic, code: dynamic, reason: dynamic, remote: dynamic) {
    wssApi.conn = null
    js("Common").toast("设备已断开")
    js("AndroidBridge").notifySettings()
    if (wssTruthy(wssApi.showConsole.onClose)) wssApi.showConsole.onClose()
    js("Plugins").emit("WSServer", "connectionClose")
}

private fun wssOnMessage(conn: dynamic, message: dynamic) {
    try {
        val json = wssJsonParseJs(message)
        val header = json.header
        val purpose = header.messagePurpose
        if (purpose == "event") {
            wssOnEvent(json)
        } else if (purpose == "commandResponse") {
            wssOnResponse(json)
        } else if (purpose == "error") {
            wssOnErrorJson(json)
        }
    } catch (e: dynamic) {
        js("erp")(e, true, message)
    }
}

private fun wssOnEvent(json: dynamic) {
    val listeners = wssApi.events.get(json.body.eventName)
    if (listeners != null) {
        wssForEachIteratorJs(listeners) { e: dynamic ->
            try {
                e(json.body, json)
            } catch (err: dynamic) {
                js("erp")(err, true)
            }
            js("undefined")
        }
    }
}

private fun wssOnResponse(json: dynamic) {
    val callback = wssApi.responsers.remove(json.header.requestId)
    if (callback != null) {
        try {
            callback(json.body, json)
        } catch (e: dynamic) {
            js("erp")(e, true)
        }
    }
}

private fun wssOnErrorJson(json: dynamic) {
    js("Common").toast("出现错误！错误代码：" + json.body.statusCode + "\n" + json.body.statusMessage)
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

private fun wssUuid(): dynamic {
    return wssString(js("java.util.UUID").randomUUID().toString())
}

private fun wssBuildHeader(purpose: dynamic): dynamic {
    val header = js("({})")
    header.version = 1
    header.requestId = wssApi.uuid()
    header.messagePurpose = purpose
    header.messageType = "commandRequest"
    return header
}

private fun wssSubscribeEvent(name: dynamic, callback: dynamic): Boolean {
    if (!wssTruthy(wssApi.conn) || !wssTruthy(wssApi.conn.isOpen())) return false
    var listeners = wssApi.events.get(name)
    if (listeners == null) {
        listeners = wssNewCopyOnWriteArrayListJs()
        wssApi.events.put(name, listeners)
    }
    listeners.add(callback)
    val msg = js("({})")
    msg.header = wssApi.buildHeader("subscribe")
    val body = js("({})")
    body.eventName = name
    msg.body = body
    wssApi.conn.send(wssJsonStringifyJs(msg))
    return true
}

private fun wssUnsubscribeEvent(name: dynamic, callback: dynamic): Boolean {
    if (!wssTruthy(wssApi.conn) || !wssTruthy(wssApi.conn.isOpen())) return false
    val listeners = wssApi.events.get(name)
    if (listeners != null) {
        listeners.remove(callback)
        if (wssTruthy(listeners.isEmpty())) {
            wssApi.events.remove(name, listeners)
            val msg = js("({})")
            msg.header = wssApi.buildHeader("unsubscribe")
            val body = js("({})")
            body.eventName = name
            msg.body = body
            wssApi.conn.send(wssJsonStringifyJs(msg))
        }
    }
    return true
}

private fun wssSendCommand(cmd: dynamic, callback: dynamic): dynamic {
    if (!wssTruthy(wssApi.conn) || !wssTruthy(wssApi.conn.isOpen())) return null
    val json = js("({})")
    json.header = wssApi.buildHeader("commandRequest")
    val body = js("({})")
    body.version = 1
    body.commandLine = cmd
    body.origin = "player"
    json.body = body
    wssApi.responsers.put(json.header.requestId, callback)
    wssApi.conn.send(wssJsonStringifyJs(json))
    return json.header.requestId
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
    wssApi.conn = null
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
    wssApi.onOpen = { conn: dynamic, handshake: dynamic ->
        wssOnOpen(conn, handshake)
        js("undefined")
    }
    wssApi.onClose = { conn: dynamic, code: dynamic, reason: dynamic, remote: dynamic ->
        wssOnClose(conn, code, reason, remote)
        js("undefined")
    }
    wssApi.onMessage = { conn: dynamic, message: dynamic ->
        wssOnMessage(conn, message)
        js("undefined")
    }
    wssApi.onEvent = { json: dynamic ->
        wssOnEvent(json)
        js("undefined")
    }
    wssApi.onResponse = { json: dynamic ->
        wssOnResponse(json)
        js("undefined")
    }
    wssApi.onError = { json: dynamic ->
        wssOnErrorJson(json)
        js("undefined")
    }
    wssApi.howToUse = {
        wssHowToUse()
        js("undefined")
    }
    wssApi.getConnectCommands = { wssGetConnectCommands() }
    wssApi.uuid = { wssUuid() }
    wssApi.buildHeader = { purpose: dynamic -> wssBuildHeader(purpose) }
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
