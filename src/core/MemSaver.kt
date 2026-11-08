package ca.kotlin

/**
 * modules/core/MemSaver.js 的 Kotlin/JS 版本。
 * 单例模块，调用方都走 MemSaver.xxx。
 */
private val memApi: dynamic = js("({})")
private val memLru: dynamic = js("[]")

fun installMemSaver() {
    memApi.lru = memLru

    memApi.onCreate = {
        memApi.trimFunction = memApi.trimProto.bind(null, js("Function.prototype"))
    }

    memApi.cache = { target: dynamic, onTrimMemory: dynamic ->
        target.__lru_onTrim__ = onTrimMemory
        if (memLru.lastIndexOf(target) < 0) memLru.push(target)
    }

    memApi.accessStart = { target: dynamic ->
        target.__lru_accessing__ = true
        val i = memLru.lastIndexOf(target)
        if (js("i >= 0")) {
            memApi.bringToEnd(i)
        } else {
            memApi.cache(target)
        }
    }

    memApi.accessEnd = { target: dynamic ->
        target.__lru_accessing__ = false
        if (memApi.needTrim()) memApi.startTrim()
    }

    memApi.startTrim = {
        var i = memLru.length - 1
        while (js("i >= 0")) {
            if (!memLru[i].__lru_accessing__) {
                if (memLru[i].__lru_onTrim__ && memLru[i].__lru_onTrim__(memLru[i])) memLru.splice(i, 1)
            }
            i--
        }
    }

    memApi.needTrim = { false }

    memApi.trimProto = { proto: dynamic, obj: dynamic ->
        val a = js("Object").getOwnPropertyNames(obj)
        var i = 0
        while (js("i < a.length")) {
            if (!js("a[i] in proto")) js("delete obj[a[i]]")
            i++
        }
    }

    memApi.bringToEnd = { index: dynamic ->
        val t = memLru[index]
        var i = index + 1
        val len = memLru.length as Int
        while (i < len) {
            memLru[i - 1] = memLru[i]
            i++
        }
        memLru[memLru.length - 1] = t
    }

    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("MemSaver", memApi)
    }
}
