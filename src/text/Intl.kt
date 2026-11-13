package ca.kotlin.text

import ca.kotlin.core.*


/**
 * modules/Intl.js 的 Kotlin/JS 版本。
 *
 * 模块本身是单例；Namespace / Entry 的方法依赖 JS 的 `this`，
 * 因此这几个方法仍用带 api 参数的 JS helper 生成，其余逻辑放在 Kotlin 侧。
 * `onCreate` 由 MapScript.loadModule 立即调用，用于初始化 locales。
 */
private val intlApi: dynamic = js("({})")

private val intlStringJs: dynamic = js("function (v) { return String(v); }")
private val intlLengthJs: dynamic = js("function (v) { return v.length | 0; }")
private val intlTruthyJs: dynamic = js("function (v) { return !!v; }")
private val intlTypeofJs: dynamic = js("function (v) { return typeof v; }")
private val intlLooseEqJs: dynamic = js("function (a, b) { return a == b; }")
private val intlLooseNeJs: dynamic = js("function (a, b) { return a != b; }")
private val intlKeyInJs: dynamic = js("function (o, k) { return k in o; }")
private val intlForInJs: dynamic = js("function (o, f) { for (var k in o) f(k); }")
private val intlObjectCreateJs: dynamic = js("function (p) { return Object.create(p); }")
private val intlDefineGetterJs: dynamic = js(
    "function (o, n, g) { Object.defineProperty(o, n, { enumerable: false, configurable: true, get: g }); }"
)
private val intlNewArrayJs: dynamic = js("function (n) { return new Array(n); }")
private val intlIsPrototypeOfJs: dynamic = js("function (ns, v) { return ns.isPrototypeOf(v); }")
private val intlUndefined: dynamic = js("undefined")
private val intlInfinity: dynamic = js("Infinity")

// Namespace / Entry 的方法需要真实 this；把模块 API 作为参数传进去，避免依赖全局名字。
private val intlNamespaceGetJs: dynamic = js(
    "function (api) { return function (id) { return api.get(id, this); }; }"
)
private val intlNamespaceResolveJs: dynamic = js(
    "function (api) {" +
        " return function (id) {" +
        "  var i, args = new Array(arguments.length);" +
        "  for (i = 1; i < arguments.length; i++) args[i] = arguments[i];" +
        "  args[0] = api.getData(id, this);" +
        "  return api.resolve.apply(api, args);" +
        " };" +
        "}"
)
private val intlNamespaceToStringJs: dynamic = js("function () { return this.__defaultEntry__; }")
private val intlEntryGetJs: dynamic = js(
    "function (api) { return function () { return api.getData(this.id, this.root); }; }"
)
private val intlEntryResolveJs: dynamic = js(
    "function (api) {" +
        " return function () {" +
        "  var i, args = new Array(arguments.length + 1);" +
        "  args[0] = this.get();" +
        "  for (i = 0; i < arguments.length; i++) args[i + 1] = arguments[i];" +
        "  return api.resolve.apply(api, args);" +
        " };" +
        "}"
)
private val intlEntryToStringJs: dynamic = js(
    "function (api) {" +
        " return function () {" +
        "  if (this === api.Entry) return '[class Entry]';" +
        "  return String(this.get());" +
        " };" +
        "}"
)

private fun intlString(v: dynamic): dynamic = intlStringJs(v)
private fun intlLength(v: dynamic): Int = intlLengthJs(v)
private fun intlTruthy(v: dynamic): Boolean = intlTruthyJs(v)
private fun intlTypeof(v: dynamic): dynamic = intlTypeofJs(v)
private fun intlLooseEq(a: dynamic, b: dynamic): Boolean = intlLooseEqJs(a, b)
private fun intlLooseNe(a: dynamic, b: dynamic): Boolean = intlLooseNeJs(a, b)
private fun intlKeyIn(o: dynamic, k: dynamic): Boolean = intlKeyInJs(o, k)
private fun intlForIn(o: dynamic, f: dynamic) { intlForInJs(o, f) }
private fun intlObjectCreate(p: dynamic): dynamic = intlObjectCreateJs(p)
private fun intlNewArray(n: dynamic): dynamic = intlNewArrayJs(n)
private fun intlIsPrototypeOf(ns: dynamic, v: dynamic): Boolean = intlIsPrototypeOfJs(ns, v)

private fun intlGetLocales(): dynamic {
    if (js("ScriptInterface").isLocaleListSupported()) {
        val a = js("ctx").getResources().getConfiguration().getLocales()
        val r = intlNewArray(a.size())
        val n = intlLength(r)
        var i = 0
        while (i < n) {
            r[i] = a.get(i)
            i++
        }
        return r
    }
    val r = js("[]")
    r.push(js("java.util.Locale").getDefault())
    return r
}

private fun intlGetData(path: dynamic, root: dynamic): dynamic {
    val seg = path.split(".")
    var data = if (intlTruthy(root)) root else intlApi.namespaces
    val n = intlLength(seg)
    var i = 0
    while (i < n) {
        data = data[seg[i]]
        if (intlTypeof(data) == "undefined") return intlUndefined
        i++
    }
    return data
}

private fun intlCreateNamespace(path: dynamic, root: dynamic): dynamic {
    val seg = path.split(".")
    var data = if (intlTruthy(root)) root else intlApi.namespaces
    val n = intlLength(seg)
    var i = 0
    while (i < n) {
        val key = seg[i]
        if (intlTypeof(data[key]) == "undefined") {
            data[key] = intlObjectCreate(intlApi.Namespace)
        }
        data = data[key]
        i++
    }
    return data
}

private fun intlGetNamespace(path: dynamic, createIfNotExist: dynamic): dynamic {
    var namespace = intlApi.getData(path)
    if (intlTypeof(namespace) == "undefined") {
        if (intlTruthy(createIfNotExist)) {
            namespace = intlApi.createNamespace(path)
        } else {
            return intlUndefined
        }
    }
    return namespace
}

private fun intlMapNamespace(obj: dynamic, propertyName: dynamic, path: dynamic) {
    var cache: dynamic = intlUndefined
    intlDefineGetterJs(obj, intlString(propertyName), {
        if (!intlTruthy(cache)) {
            cache = intlApi.getNamespace(path, true)
        }
        cache
    })
}

private fun intlGetRes(name: dynamic): dynamic {
    if (intlKeyIn(intlApi.rescache, name)) return intlApi.rescache[name]
    val res = js("ctx").getResources()
    val id = res.getIdentifier(name, "string", "android")
    if (intlLooseNe(id, 0)) {
        val value = intlString(res.getString(id))
        intlApi.rescache[name] = value
        return value
    }
    return intlUndefined
}

private fun intlGet(path: dynamic, root: dynamic): dynamic {
    val entry = intlObjectCreate(intlApi.Entry)
    entry.id = path
    entry.root = root
    return entry
}

private fun intlMixNamespace(o: dynamic, target: dynamic) {
    if (intlTypeof(o) == "object") {
        intlForIn(o, { key: dynamic ->
            val oldValue = target[key]
            if (intlTruthy(oldValue) && intlIsPrototypeOf(intlApi.Namespace, oldValue)) {
                intlApi.mixNamespace(o[key], oldValue)
            } else {
                target[key] = intlApi.cloneNamespace(o[key])
            }
        })
    } else {
        target.__defaultEntry__ = o
    }
}

private fun intlCloneNamespace(o: dynamic): dynamic {
    if (intlTypeof(o) == "object") {
        val r = intlObjectCreate(intlApi.Namespace)
        intlForIn(o, { key: dynamic ->
            r[key] = intlApi.cloneNamespace(o[key])
        })
        return r
    }
    return o
}

private fun intlGetFitLocaleIndex(range: dynamic): Int {
    val locales = intlApi.locales
    val n = intlLength(locales)
    var i = 0
    while (i < n) {
        val e = locales[i]
        if (intlTruthy(range.language) && intlLooseNe(range.language, e.getLanguage())) {
            i++
            continue
        }
        // 系统「应用语言」可能只给语言码；原逻辑对国家码也做 ignoreCountry 处理。
        if (intlTruthy(range.country) && intlLooseNe(range.country, e.getCountry()) && !intlTruthy(range.ignoreCountry)) {
            i++
            continue
        }
        if (intlTruthy(range.variant) && intlLooseNe(range.variant, e.getVariant())) {
            i++
            continue
        }
        return i
    }
    return if (intlTruthy(range.unspecifiedLang)) n else -1
}

private fun intlLoadLang(range: dynamic, o: dynamic, full: dynamic): Boolean {
    val index = intlApi.getFitLocaleIndex(range)
    if (index < 0) return false
    if (intlLooseEq(intlApi.currentLocIndex, index)) {
        intlApi.mixNamespace(o, intlApi.namespaces)
    } else if (intlApi.currentLocIndex > index || intlApi.currentLocIndex < 0) {
        intlApi.currentLocIndex = index
        if (intlTruthy(full)) {
            intlApi.namespaces = intlApi.cloneNamespace(o)
        } else {
            intlApi.namespaces = intlApi.cloneNamespace(intlApi.defaultLang)
            intlApi.mixNamespace(o, intlApi.namespaces)
        }
    } else {
        return false
    }
    return true
}

private fun intlLookupLang(ranges: dynamic): dynamic {
    var minIndex: dynamic = intlUndefined
    var minValue: dynamic = intlInfinity
    intlForIn(ranges, { key: dynamic ->
        val v = intlApi.getFitLocaleIndex(ranges[key])
        if (v >= 0 && v < minValue) {
            minIndex = key
            minValue = v
        }
    })
    return minIndex
}

private fun intlOnCreate() {
    intlApi.locales = intlApi.getLocales()
    intlApi.currentLocIndex = -1
}

fun installIntl() {
    intlApi.rescache = js("({})")

    intlApi.getLocales = { intlGetLocales() }
    intlApi.getData = { path: dynamic, root: dynamic -> intlGetData(path, root) }
    intlApi.createNamespace = { path: dynamic, root: dynamic -> intlCreateNamespace(path, root) }
    intlApi.getNamespace = { path: dynamic, createIfNotExist: dynamic -> intlGetNamespace(path, createIfNotExist) }
    intlApi.mapNamespace = { obj: dynamic, propertyName: dynamic, path: dynamic ->
        intlMapNamespace(obj, propertyName, path)
    }
    intlApi.getRes = { name: dynamic -> intlGetRes(name) }
    intlApi.resolve = js("java.lang.String.format")
    intlApi.get = { path: dynamic, root: dynamic -> intlGet(path, root) }

    val namespace = js("({})")
    namespace.get = intlNamespaceGetJs(intlApi)
    namespace.resolve = intlNamespaceResolveJs(intlApi)
    namespace.toString = intlNamespaceToStringJs
    intlApi.Namespace = namespace

    val entry = js("({})")
    entry.get = intlEntryGetJs(intlApi)
    entry.resolve = intlEntryResolveJs(intlApi)
    entry.toString = intlEntryToStringJs(intlApi)
    intlApi.Entry = entry

    intlApi.mixNamespace = { o: dynamic, target: dynamic -> intlMixNamespace(o, target) }
    intlApi.cloneNamespace = { o: dynamic -> intlCloneNamespace(o) }
    intlApi.getFitLocaleIndex = { range: dynamic -> intlGetFitLocaleIndex(range) }
    intlApi.loadLang = { range: dynamic, o: dynamic, full: dynamic -> intlLoadLang(range, o, full) }
    intlApi.lookupLang = { ranges: dynamic -> intlLookupLang(ranges) }
    intlApi.onCreate = { intlOnCreate() }

    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("Intl", intlApi)
    }
}
