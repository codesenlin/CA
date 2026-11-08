package ca.kotlin.network

import ca.kotlin.core.*


/**
 * modules/network/NetworkUtils.js 的 Kotlin/JS 版本。
 *
 * 保持原有对象 API 与 Java 网络调用；需要动态 this / arguments 的入口
 * 通过很薄的 JS wrapper 适配，实际逻辑仍在 Kotlin 侧。
 */
private val networkApi: dynamic = js("({})")
private var networkRequestError: dynamic = js("null")

// ---------------- JS 语义 helper ----------------
private val nuTruthyJs: dynamic = js("function (v) { return !!v; }")
private val nuStringJs: dynamic = js("function (v) { return String(v); }")
private val nuLengthJs: dynamic = js("function (v) { return v.length | 0; }")
private val nuForInJs: dynamic = js("function (o, f) { for (var i in o) f(i); }")
private val nuTypeofJs: dynamic = js("function (v) { return typeof v; }")
private val nuTypeofScriptInterfaceJs: dynamic = js("function () { return typeof ScriptInterface; }")
private val nuIsObjectJs: dynamic = js("function (v) { return v instanceof Object; }")
private val nuIsInet4Js: dynamic = js("function (v) { return v instanceof java.net.Inet4Address; }")
private val nuIsUndefLooseJs: dynamic = js("function (v) { return v == undefined; }")
private val nuLooseEqJs: dynamic = js("function (a, b) { return a == b; }")
private val nuJoinJs: dynamic = js("function (a, sep) { return a.join(sep); }")
private val nuEncodeJs: dynamic = js("function (v) { return encodeURIComponent(v); }")
private val nuObjectCreateJs: dynamic = js("function (o) { return Object.create(o); }")
private val nuJsonParseJs: dynamic = js("function (s) { return JSON.parse(s); }")
private val nuJsonStringifyJs: dynamic = js("function (v) { return JSON.stringify(v); }")
private val nuHasParamJs: dynamic = js("function (u) { return /:(\\w+)/.test(u); }")
private val nuReadJsonJs: dynamic = js("function () { return json; }")
private val nuUndefinedTimeJs: dynamic = js("function () { return time; }")
private val nuVariadicJs: dynamic = js(
    "function (f) { return function (method, url) { return f(method, url, arguments); }; }"
)
private val nuThis0Js: dynamic = js("function (f) { return function () { return f(this); }; }")
private val nuThis1Js: dynamic = js("function (f) { return function (a) { return f(this, a); }; }")
private val nuReplaceParamsJs: dynamic = js(
    """function (url, params) {
        var regexp = /:(\w+)/g, found = false;
        var out = url.replace(regexp, function (match, key) {
            if (key in params) {
                found = true;
                return encodeURIComponent(params[key]);
            }
            return match;
        });
        return [out, found];
    }"""
)
private val nuDefineErrorMessagesJs: dynamic = js(
    """function (o) {
        Object.defineProperty(o, "errorMessages", {
            enumerable: false,
            configurable: false,
            writable: false,
            value: Object.create(null)
        });
    }"""
)

private fun nuTruthy(v: dynamic): Boolean = nuTruthyJs(v)
private fun nuLength(v: dynamic): Int = nuLengthJs(v)
private fun nuIsObject(v: dynamic): Boolean = nuIsObjectJs(v)
private fun nuIsInet4(v: dynamic): Boolean = nuIsInet4Js(v)
private fun nuIsUndefLoose(v: dynamic): Boolean = nuIsUndefLooseJs(v)
private fun nuLooseEq(a: dynamic, b: dynamic): Boolean = nuLooseEqJs(a, b)
private fun nuHasParam(v: dynamic): Boolean = nuHasParamJs(v)

// ---------------- Java 互操作 helper ----------------
private val nuNewURLJs: dynamic = js("function (s) { return new java.net.URL(s); }")
private val nuNewReaderJs: dynamic = js(
    "function (s) { return new java.io.BufferedReader(new java.io.InputStreamReader(s)); }"
)
private val nuStringBytesJs: dynamic = js(
    "function (v) { var s = new java.lang.String(v); return s.getBytes(); }"
)
private val nuNewFileOutputStreamJs: dynamic = js("function (p) { return new java.io.FileOutputStream(p); }")
private val nuNewFileInputStreamJs: dynamic = js("function (p) { return new java.io.FileInputStream(p); }")
private val nuNewGZIPInputStreamJs: dynamic = js("function (s) { return new java.util.zip.GZIPInputStream(s); }")
private val nuNewDigestInputStreamJs: dynamic = js(
    "function (s, d) { return new java.security.DigestInputStream(s, d); }"
)
private val nuNewMessageDigestJs: dynamic = js(
    "function (n) { return java.security.MessageDigest.getInstance(n); }"
)
private val nuBase64Js: dynamic = js(
    "function (b) { return android.util.Base64.encodeToString(b, android.util.Base64.NO_WRAP); }"
)
private val nuNewByteArrayJs: dynamic = js(
    "function (n) { return java.lang.reflect.Array.newInstance(java.lang.Byte.TYPE, n); }"
)

private fun nuCreateRequestError(): dynamic {
    val o = nuObjectCreateJs(js("Error").prototype)
    o.toString = nuThis0Js { self: dynamic ->
        val parts = js("[]")
        parts.push("RequestError: " + self.responseCode + " " + self.responseMessage)
        parts.push(self.errorMessage)
        parts.push(self.error)
        nuJoinJs(parts, "\n")
    }
    o.create = nuThis1Js { self: dynamic, err: dynamic ->
        val r = nuObjectCreateJs(self)
        r.error = err
        r.message = err.message
        r.stack = err.stack
        r
    }
    return o
}

private fun nuRequest(url: dynamic, method: dynamic, data: dynamic, headers: dynamic): dynamic {
    val u = nuNewURLJs(url)
    val conn = u.openConnection()
    conn.setConnectTimeout(5000)
    conn.setUseCaches(false)
    conn.setRequestMethod(method)
    if (nuTruthy(data)) {
        conn.setDoInput(true)
        conn.setDoOutput(true)
    }
    if (nuTruthy(headers)) {
        if (nuIsObject(headers)) {
            nuForInJs(headers) { i: dynamic -> conn.setRequestProperty(i, headers[i]) }
        } else {
            conn.setRequestProperty("Content-Type", headers)
        }
    }

    var rd: dynamic = null
    var ln: dynamic = null
    try {
        conn.connect()
        if (nuTruthy(data)) {
            val wr = conn.getOutputStream()
            wr.write(nuStringBytesJs(data))
            wr.flush()
        }
        rd = nuNewReaderJs(conn.getInputStream())
        val lines = js("[]")
        ln = rd.readLine()
        while (nuTruthy(ln)) {
            lines.push(ln)
            ln = rd.readLine()
        }
        rd.close()
        return nuJoinJs(lines, "\n")
    } catch (e: dynamic) {
        try {
            rd = conn.getErrorStream()
        } catch (er: dynamic) {
            throw e
        }
        val err = networkApi.RequestError.create(e)
        err.responseCode = conn.getResponseCode()
        err.responseMessage = nuStringJs(conn.getResponseMessage())
        val lines = js("[]")
        if (nuTruthy(rd)) {
            val erd = nuNewReaderJs(rd)
            var eln = erd.readLine()
            while (nuTruthy(eln)) {
                lines.push(eln)
                eln = erd.readLine()
            }
            erd.close()
        }
        err.errorMessage = nuJoinJs(lines, "\n")
        throw err
    }
}

private fun nuDownload(url: dynamic, path: dynamic) {
    val u = nuNewURLJs(url)
    val conn = u.openConnection()
    conn.setConnectTimeout(5000)
    conn.setUseCaches(false)
    conn.setRequestMethod("GET")
    conn.connect()
    val input = conn.getInputStream()
    val output = nuNewFileOutputStreamJs(path)
    val buf = nuNewByteArrayJs(8192)
    var hr = input.read(buf)
    while (hr > 0) {
        output.write(buf, 0, hr)
        hr = input.read(buf)
    }
    output.close()
    input.close()
}

private fun nuDownloadGz(url: dynamic, path: dynamic, sha1: dynamic): dynamic {
    val u = nuNewURLJs(url)
    val conn = u.openConnection()
    conn.setConnectTimeout(5000)
    conn.setUseCaches(false)
    conn.setRequestMethod("GET")
    conn.connect()
    val digest = nuNewMessageDigestJs("SHA-1")
    val input = nuNewGZIPInputStreamJs(nuNewDigestInputStreamJs(conn.getInputStream(), digest))
    val output = nuNewFileOutputStreamJs(path)
    val buf = nuNewByteArrayJs(8192)
    var hr = input.read(buf)
    while (hr > 0) {
        output.write(buf, 0, hr)
        hr = input.read(buf)
    }
    output.close()
    input.close()
    return nuLooseEq(nuBase64Js(digest.digest()), sha1)
}

private fun nuDownloadToUri(url: dynamic, uri: dynamic) {
    val u = nuNewURLJs(url)
    val conn = u.openConnection()
    conn.setConnectTimeout(5000)
    conn.setUseCaches(false)
    conn.setRequestMethod("GET")
    conn.connect()
    val input = conn.getInputStream()
    val output = js("ExternalStorage").openOutputStream(uri)
    js("ExternalStorage").pipe(input, output, 8192)
    output.close()
    input.close()
}

private fun nuVerifyFile(path: dynamic, sha1: dynamic): dynamic {
    val digest = nuNewMessageDigestJs("SHA-1")
    val input = nuNewFileInputStreamJs(path)
    val buf = nuNewByteArrayJs(8192)
    var hr = input.read(buf)
    while (hr > 0) {
        digest.update(buf, 0, hr)
        hr = input.read(buf)
    }
    input.close()
    return nuLooseEq(nuBase64Js(digest.digest()), sha1)
}

private fun nuToQueryString(obj: dynamic): dynamic {
    val r = js("[]")
    nuForInJs(obj) { i: dynamic ->
        if (!nuIsUndefLoose(obj[i])) r.push(i + "=" + nuEncodeJs(obj[i]))
    }
    return nuJoinJs(r, "&")
}

private fun nuGetIps(): dynamic {
    val ni = js("Common").iterableToArray(js("java.net.NetworkInterface").getNetworkInterfaces())
    val ips = js("[]")
    var i = 0
    val n = nuLength(ni)
    while (i < n) {
        val e = js("Common").iterableToArray(ni[i].getInetAddresses())
        var j = 0
        val m = nuLength(e)
        while (j < m) {
            val ip = e[j]
            if (nuIsInet4(ip)) ips.push(ip.getHostAddress())
            j++
        }
        i++
    }
    return ips
}

private fun nuRequestApi(method: dynamic, url0: dynamic, args: dynamic): dynamic {
    var url = url0
    val argCount = nuLength(args)
    var argIndex = 2
    var query: dynamic = js("undefined")
    var content: dynamic = js("undefined")

    if (nuHasParam(url) && argIndex < argCount) {
        val params = args[argIndex]
        val replaced = nuReplaceParamsJs(url, params)
        url = replaced[0]
        if (nuTruthy(replaced[1])) argIndex++
    }
    if (method == "GET" || method == "HEAD" || method == "DELETE") {
        query = args[argIndex]
    } else {
        if (argCount - argIndex > 1) {
            query = args[argIndex]
            argIndex++
        }
        content = args[argIndex]
    }
    if (nuTruthy(query)) url = url + "?" + networkApi.toQueryString(query)
    try {
        val result = nuJsonParseJs(
            networkApi.request(
                url,
                method,
                if (nuTruthy(content)) nuJsonStringifyJs(content) else null,
                if (nuTruthy(content)) "application/json" else null
            )
        )
        return result.result
    } catch (e: dynamic) {
        throw networkApi.parseError(e)
    }
}

private fun nuParseError(e: dynamic): dynamic {
    if (!nuTruthy(e.errorMessage)) return e
    if (nuLooseEq(e.responseCode, 500)) {
        return "内部错误"
    } else if (nuLooseEq(e.responseCode, 503)) {
        return "服务不可用"
    }
    var json: dynamic = js("undefined")
    try {
        json = nuJsonParseJs(e.errorMessage)
    } catch (err: dynamic) {
        // Not a json
    }
    if (!nuTruthy(json)) return e
    var message = networkApi.errorMessages[json.error]
    if (!nuTruthy(message)) {
        message = "未知错误(" + json.error + ")\n" + json
    }
    return message
}

private fun nuAddErrorMessages(messages: dynamic) {
    nuForInJs(messages) { i: dynamic -> networkApi.errorMessages[i] = messages[i] }
}

private fun nuOnCreate() {
    nuDefineErrorMessagesJs(networkApi)
}

private fun nuConnectWSEvent(uri: dynamic, listeners: dynamic): dynamic {
    if (nuTypeofScriptInterfaceJs() != "object") return null
    var wsInterface: dynamic = js("undefined")
    var wsClient: dynamic = js("undefined")
    val handler = js("({})")

    handler.onOpen = { _thisObj: dynamic, _handshake: dynamic ->
        try {
            wsInterface.available = true
            if (nuTruthy(listeners.onOpen)) listeners.onOpen(wsInterface)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }
    handler.onClose = { _thisObj: dynamic, code: dynamic, reason: dynamic, remote: dynamic ->
        try {
            wsInterface.available = false
            if (nuTruthy(listeners.onClose)) listeners.onClose(wsInterface, code, reason, remote)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }
    handler.onMessage = { _thisObj: dynamic, message: dynamic ->
        try {
            var json: dynamic = js("undefined")
            var invalid = false
            try {
                json = nuJsonParseJs(message)
                if (nuTypeofJs(json) != "object") invalid = true
            } catch (e: dynamic) {
                invalid = true
            }
            if (invalid) {
                wsInterface.sendError("wsevent.invalidFormat")
                wsClient.close()
            } else {
                val type = json.type
                if (type == "event") {
                    if (nuTruthy(listeners.onEvent)) listeners.onEvent(wsInterface, json.name, json.data)
                } else if (type == "command") {
                    if (nuTruthy(listeners.onCommand)) {
                        listeners.onCommand(wsInterface, json.requestId, json.name, json.data)
                    }
                } else if (type == "command_response") {
                    if (nuTruthy(listeners.onCommandResponse)) {
                        listeners.onCommandResponse(wsInterface, json.requestId, json.data)
                    }
                } else if (type == "ping") {
                    wsInterface.sendPong(json.time)
                } else if (type == "pong") {
                    if (nuTruthy(listeners.onPingPong)) {
                        // 原实现引用了未定义的全局 time；用 helper 保留这一行为。
                        listeners.onPingPong(
                            wsInterface,
                            (js("android.os.SystemClock").uptimeMillis() - nuUndefinedTimeJs()) / 1000
                        )
                    }
                } else {
                    wsInterface.sendError("wsevent.invalidType")
                }
            }
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }
    handler.onError = { _thisObj: dynamic, err: dynamic ->
        try {
            if (nuTruthy(listeners.onError)) listeners.onError(wsInterface, err)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }

    wsClient = js("ScriptInterface").createWSClient(uri, handler)
    wsInterface = js("({})")
    // 原实现这里引用的是未定义的外层 json，且调用方传参也被忽略；保持原行为。
    wsInterface.sendRaw = { nuJsonStringifyJs(nuReadJsonJs()) }
    wsInterface.sendEvent = { eventName: dynamic, data: dynamic ->
        val json = js("({})")
        json.type = "event"
        json.name = eventName
        json.data = data
        wsInterface.sendRaw(json)
        js("undefined")
    }
    wsInterface.sendCommand = { requestId: dynamic, commandName: dynamic, data: dynamic ->
        val json = js("({})")
        json.type = "command"
        json.requestId = requestId
        json.name = commandName
        json.data = data
        wsInterface.sendRaw(json)
        js("undefined")
    }
    wsInterface.sendCommandResponse = { requestId: dynamic, data: dynamic ->
        val json = js("({})")
        json.type = "command_response"
        json.requestId = requestId
        json.data = data
        wsInterface.sendRaw(json)
        js("undefined")
    }
    wsInterface.sendError = { error: dynamic, data: dynamic ->
        val json = js("({})")
        json.type = "error"
        json.error = error
        json.data = data
        wsInterface.sendRaw(json)
        js("undefined")
    }
    wsInterface.sendPing = {
        val json = js("({})")
        json.type = "ping"
        json.time = js("android.os.SystemClock").uptimeMillis()
        wsInterface.sendRaw(json)
        js("undefined")
    }
    wsInterface.sendPong = { time: dynamic ->
        val json = js("({})")
        json.type = "pong"
        json.time = time
        wsInterface.sendRaw(json)
        js("undefined")
    }
    wsInterface.close = {
        wsClient.close()
        js("undefined")
    }
    wsInterface.client = wsClient
    wsInterface.listeners = listeners
    wsInterface.available = false
    wsClient.connect()
    return wsInterface
}

fun installNetworkUtils() {
    networkRequestError = nuCreateRequestError()
    networkApi.RequestError = networkRequestError

    networkApi.queryPage = { url: dynamic -> networkApi.request(url, "GET") }
    networkApi.postPage = { url: dynamic, data: dynamic, headers: dynamic ->
        networkApi.request(url, "POST", data, headers)
    }
    networkApi.request = { url: dynamic, method: dynamic, data: dynamic, headers: dynamic ->
        nuRequest(url, method, data, headers)
    }
    networkApi.download = { url: dynamic, path: dynamic ->
        nuDownload(url, path)
        js("undefined")
    }
    networkApi.downloadGz = { url: dynamic, path: dynamic, sha1: dynamic ->
        nuDownloadGz(url, path, sha1)
    }
    networkApi.downloadToUri = { url: dynamic, uri: dynamic ->
        nuDownloadToUri(url, uri)
        js("undefined")
    }
    networkApi.verifyFile = { path: dynamic, sha1: dynamic -> nuVerifyFile(path, sha1) }
    networkApi.toQueryString = { obj: dynamic -> nuToQueryString(obj) }
    networkApi.getIps = { nuGetIps() }
    networkApi.requestApi = nuVariadicJs { method: dynamic, url: dynamic, args: dynamic ->
        nuRequestApi(method, url, args)
    }
    networkApi.parseError = { e: dynamic -> nuParseError(e) }
    networkApi.addErrorMessages = { messages: dynamic ->
        nuAddErrorMessages(messages)
        js("undefined")
    }
    networkApi.connectWSEvent = { uri: dynamic, listeners: dynamic ->
        nuConnectWSEvent(uri, listeners)
    }
    networkApi.onCreate = {
        nuOnCreate()
        js("undefined")
    }

    val urlBase = js("({})")
    urlBase.api = "https://ca.projectxero.top"
    urlBase.ws = "wss://ca.projectxero.top"
    networkApi.urlBase = urlBase

    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("NetworkUtils", networkApi)
    }
}
