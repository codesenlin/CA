package ca.kotlin

/**
 * modules/core/Internal.js 的 Kotlin/JS 版本。
 *
 * 保持原有对象 API：
 *   Internal.onCreate()
 *   Internal.initialize()
 *   Internal.getKey()
 *   Internal.get(id, providedKey)
 *   Internal.add(id, namespace)
 *   Internal.once(key, value)
 */
private val KEY: dynamic = js("Object.create(null)")
private var loading = false
private var namespaces: dynamic = js("Object.create(null)")

private val api: dynamic = js("({})")

fun installInternal() {
    api.onCreate = {
        loading = true
    }

    api.initialize = {
        js("Object").freeze(namespaces)
        loading = false
    }

    api.getKey = {
        if (loading) KEY else js("undefined")
    }

    api.get = { id: dynamic, providedKey: dynamic ->
        if (loading || providedKey === KEY) namespaces[id] else js("undefined")
    }

    api.add = { id: dynamic, namespace: dynamic ->
        if (!loading) throw Exception("Internal is freezed")
        if (js("Object").prototype.hasOwnProperty.call(namespaces, id)) throw Exception("$id is occupied")
        namespace["internal"] = namespaces
        js("Object").defineProperty(namespaces, id, js("({ enumerable: false, writable: false, configurable: true, value: namespace })"))
        namespace
    }

    api.once = { key: dynamic, value: dynamic ->
        var stored = value
        { providedKey: dynamic ->
            if (key !== providedKey) {
                null
            } else {
                val result = stored
                stored = null
                result
            }
        }
    }

    js("globalThis").CaKotlinInternal = api
    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("Internal", api)
    }
}

fun main() {
    installInternal()
    installEventSender()
    installMemSaver()
    installUIHelp()
    installGlobalUtils()
    exposeLate()
    exposePending()
}
