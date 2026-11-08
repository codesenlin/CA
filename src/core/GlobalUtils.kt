package ca.kotlin.core


/**
 * modules/utils/GlobalUtils.js 的 Kotlin/JS 版本。
 *
 * appendSSB / ES6Ex 都是低依赖模块，但会操作全局原型，因此 onCreate 的时序保持原样：
 * bundle 在 Internal 位置注册，实际调用发生在后续模块初始化时。
 */
private val es6Api: dynamic = js("({})")
private val es6Bridge: dynamic = js("({})")

private fun copyValue(x: dynamic, lev: Int): dynamic {
    if (lev < 0) return x
    if (js("Array.isArray(x)")) {
        val r = js("x.slice()")
        var i = 0
        while (js("i < x.length")) {
            r[i] = copyValue(x[i], lev - 1)
            i++
        }
        return r
    }
    if (js("x instanceof Date")) return js("new Date(x.getTime())")
    if (js("x instanceof Object")) {
        val r = js("({})")
        val keys = js("Object").keys(x)
        var i = 0
        while (js("i < keys.length")) {
            val k = keys[i]
            r[k] = copyValue(x[k], lev - 1)
            i++
        }
        return r
    }
    return x
}

fun installGlobalUtils() {
    val appendSsb: dynamic = { src: dynamic, str: dynamic, span: dynamic ->
        val c = src.length()
        src.append(str)
        src.setSpan(span, c, src.length(), js("src.SPAN_INCLUSIVE_EXCLUSIVE"))
    }
    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("appendSSB", appendSsb)
    }

    es6Bridge.startsWith = { self: dynamic, s: dynamic -> js("self.slice(0, s.length) == s") }
    es6Bridge.endsWith = { self: dynamic, s: dynamic -> js("self.slice(-s.length) == s") }
    es6Bridge.objectCopy = { o: dynamic -> copyValue(o, 32) }

    es6Api.onCreate = {
        if (js("!String.prototype.startsWith")) js("String.prototype.startsWith = globalThis.__caEs6.string_startsWith")
        if (js("!String.prototype.endsWith")) js("String.prototype.endsWith = globalThis.__caEs6.string_endsWith")
        if (js("!Object.copy")) js("Object.copy = globalThis.__caEs6.object_copy")
    }
    es6Api.string_startsWith = js("function(s) { return globalThis.__caEs6.bridge.startsWith(this, s); }")
    es6Api.string_endsWith = js("function(s) { return globalThis.__caEs6.bridge.endsWith(this, s); }")
    es6Api.object_copy = { o: dynamic -> es6Bridge.objectCopy(o) }

    val es6Globals = js("({})")
    es6Globals.bridge = es6Bridge
    es6Globals.string_startsWith = es6Api.string_startsWith
    es6Globals.string_endsWith = es6Api.string_endsWith
    es6Globals.object_copy = es6Api.object_copy
    js("globalThis").__caEs6 = es6Globals
    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("ES6Ex", es6Api)
    }
}
