package ca.kotlin

/**
 * modules/Accel.js 的 Kotlin/JS 版本。
 *
 * 约定与原模块一致：
 *   Accel.available()  —— 返回当前是否加载了 caaccel.jar
 *   Accel.xxx(...)     —— 有 jar 走 Java，没有走这里的等价实现
 *
 * QuickAccel 在主脚本后加载，所以 quickAccel() 只在调用时查全局，
 * 不能把 QuickAccel 缓存到顶层。
 */
private val accelApi: dynamic = js("({})")

private var accelImpl: dynamic = null
private var idTextsCache: dynamic = null
private var lastIDTexts: dynamic = null
private var filterTextsCache: dynamic = null
private var commandKeys: dynamic = null
private var commandDescs: dynamic = null
private var commandSnapRef: dynamic = null
private var matchSourceRef: dynamic = null
private var matchCacheSupported = true

private val accelString: dynamic = js("function (v) { return String(v); }")
private val accelObjectKeys: dynamic = js("function (o) { return Object.keys(o); }")
private val accelTruthyJs: dynamic = js("function (v) { return !!v; }")
private val accelIsObjectJs: dynamic = js("function (v) { return v instanceof Object; }")
private val accelIsArrayJs: dynamic = js("function (v) { return Array.isArray(v); }")
private val accelIsFunctionJs: dynamic = js("function (v) { return typeof v === 'function'; }")
private val accelIsNaNNs: dynamic = js("function (v) { return isNaN(v); }")
private val accelParseIntNs: dynamic = js("function (v) { return parseInt(v); }")
private val accelMaxJs: dynamic = js("function (a, b) { return Math.max(a, b); }")
private val accelNewErrorJs: dynamic = js("function (m) { return new Error(m); }")
// 用 JS 的 ToInt32 语义取 length：兼容 Rhino 的 Java 数组/包装对象，也与原 JS 比较语义一致
private val accelLengthJs: dynamic = js("function (v) { return v.length | 0; }")

/** QuickAccel 在主脚本后加载，调用时再取；不能提前初始化。 */
private val accelLookupQuickAccel: dynamic = js(
    "function () { try { return (typeof QuickAccel !== 'undefined' && QuickAccel && QuickAccel.available) ? QuickAccel : null; } catch (e) { return null; } }"
)

private fun accelTruthy(v: dynamic): Boolean = accelTruthyJs(v)
private fun accelIsObject(v: dynamic): Boolean = accelIsObjectJs(v)
private fun accelIsArray(v: dynamic): Boolean = accelIsArrayJs(v)
private fun accelIsFunction(v: dynamic): Boolean = accelIsFunctionJs(v)
private fun quickAccel(): dynamic = accelLookupQuickAccel()
private fun accelLength(v: dynamic): Int = accelLengthJs(v)

private fun commandDescAt(index: Int): dynamic {
    if (commandDescs != null) {
        val n = accelLength(commandDescs)
        if (index < n && accelTruthy(commandDescs[index])) return commandDescs[index]
    }
    return ""
}

private fun jsMatchCommandNames(prefix: dynamic): dynamic {
    if (commandKeys == null) return null
    val p = accelString(prefix)
    val out = js("[]")
    val n = accelLength(commandKeys)
    var i = 0
    while (i < n) {
        val d = commandDescAt(i)
        if (commandKeys[i].indexOf(p) >= 0 || d.indexOf(p) >= 0) out.push(commandKeys[i])
        i++
    }
    out.sort()
    return out
}

private fun syncCommandSnapshot(snap: dynamic) {
    if (snap === commandSnapRef || !accelIsObject(snap)) return
    val ks = accelObjectKeys(snap)
    val ds = js("[]")
    val n = accelLength(ks)
    var i = 0
    while (i < n) {
        val v: dynamic = snap[ks[i]]
        ds.push(accelString(if (v == null) "" else v))
        i++
    }
    commandKeys = ks
    commandDescs = ds
    commandSnapRef = snap
    if (accelImpl != null) {
        try {
            accelImpl.setCommandSnapshot(ks, ds)
        } catch (e: dynamic) { }
    }
}

private fun jsMatchString(ps: dynamic, a: dynamic, r: dynamic): dynamic {
    var out = r
    if (!accelIsObject(out)) out = js("({})")
    if (!accelIsArray(out.input)) out.input = js("[]")
    if (!accelIsObject(out.output)) out.output = js("({})")
    if (accelIsArray(a)) {
        val t = js("[]")
        val n = accelLength(a)
        var i = 0
        while (i < n) {
            val e = a[i]
            if (e.indexOf(ps) >= 0) {
                out.output[e] = e
                if (out.input.indexOf(e) < 0) t.push(e)
            }
            i++
        }
        t.sort()
        out.input = out.input.concat(t)
    } else {
        val t = js("[]")
        val t2 = js("[]")
        val ks = accelObjectKeys(a)
        val n = accelLength(ks)
        var i = 0
        while (i < n) {
            val e = ks[i]
            val v: dynamic = a[e]
            if (!(e.indexOf(ps) < 0 && v.indexOf(ps) < 0)) {
                if (accelTruthy(v)) {
                    val t3: dynamic = e + " - " + v
                    out.output[t3] = e
                    if (out.input.indexOf(t3) < 0) t.push(t3)
                } else {
                    out.output[e] = e
                    if (out.input.indexOf(e) < 0) t2.push(e)
                }
            }
            i++
        }
        t.sort()
        t2.sort()
        out.input = out.input.concat(t, t2)
    }
    return out
}

private fun syncMatchSource(a: dynamic) {
    if (a === matchSourceRef) return
    if (accelIsArray(a)) {
        accelImpl.setMatchSource(a, null, false)
    } else if (accelIsObject(a)) {
        val ks = accelObjectKeys(a)
        val ds = js("[]")
        val n = accelLength(ks)
        var i = 0
        while (i < n) {
            val v: dynamic = a[ks[i]]
            ds.push(accelString(if (v == null) "" else v))
            i++
        }
        accelImpl.setMatchSource(ks, ds, true)
    } else {
        throw accelNewErrorJs("unsupported match source")
    }
    matchSourceRef = a
}

private fun merge(r: dynamic, res: dynamic): dynamic {
    var out = r
    if (!accelIsObject(out)) out = js("({})")
    if (!accelIsArray(out.input)) out.input = js("[]")
    if (!accelIsObject(out.output)) out.output = js("({})")
    var keys: dynamic = null
    var ins: dynamic = null
    if (accelIsFunction(res.outputKeys)) {
        keys = res.outputKeys()
        val vals = res.outputValues()
        val n = accelLength(keys)
        var i = 0
        while (i < n) {
            out.output[keys[i]] = vals[i]
            i++
        }
        ins = res.inputArray()
    } else {
        val outs = res.outputs
        keys = outs.keySet().toArray()
        val n = accelLength(keys)
        var i = 0
        while (i < n) {
            out.output[accelString(keys[i])] = accelString(outs.get(keys[i]))
            i++
        }
        ins = res.inputs.toArray()
    }
    // 用 seen 去重；原实现每个结果都 indexOf 一次，结果上千条时是 O(n²)
    val seen = js("Object.create(null)")
    val inputLength = accelLength(out.input)
    var i = 0
    while (i < inputLength) {
        seen[out.input[i]] = true
        i++
    }
    val insLength = accelLength(ins)
    i = 0
    while (i < insLength) {
        val v = ins[i]
        if (!accelTruthy(seen[v])) {
            seen[v] = true
            out.input.push(v)
        }
        i++
    }
    return out
}

private fun matchStringImpl(ps: dynamic, a: dynamic, r: dynamic): dynamic {
    if (accelImpl != null && matchCacheSupported) {
        try {
            syncMatchSource(a)
            return merge(r, accelImpl.matchCached(accelString(ps)))
        } catch (e: dynamic) {
            matchCacheSupported = false
        }
    }
    if (accelImpl != null) {
        try {
            return merge(r, accelImpl.matchString(accelString(ps), a))
        } catch (e: dynamic) { }
    }
    return jsMatchString(ps, a, r)
}

/** 批量过滤：返回与 arr 等长的下标掩码，true 表示保留。 */
private fun filterImpl(arr: dynamic, s: dynamic): dynamic {
    val keep = js("[]")
    val p = accelString(s)
    if (accelImpl != null) {
        try {
            return accelImpl.filter(arr, p)
        } catch (e: dynamic) { }
    }
    val n = accelLength(arr)
    var i = 0
    while (i < n) {
        keep.push(arr[i] != null && arr[i].indexOf(p) >= 0)
        i++
    }
    return keep
}

private fun setFilterTextsImpl(texts: dynamic) {
    filterTextsCache = texts
    if (accelImpl != null) {
        try {
            accelImpl.setFilterTexts(texts)
        } catch (e: dynamic) { }
    }
}

/** 缓存版 filter：返回 boolean 掩码；没有 jar 时走 JS。 */
private fun filterTextsImpl(s: dynamic): dynamic {
    val p = accelString(s)
    val keep = js("[]")
    if (accelImpl != null) {
        try {
            return accelImpl.filterTexts(p)
        } catch (e: dynamic) { }
    }
    val qa = quickAccel()
    if (qa != null && filterTextsCache != null) {
        val qres = qa.filterTexts(filterTextsCache, p)
        if (accelTruthy(qres)) return qres
    }
    if (filterTextsCache != null) {
        val n = accelLength(filterTextsCache)
        var i = 0
        while (i < n) {
            keep.push(filterTextsCache[i] != null && filterTextsCache[i].indexOf(p) >= 0)
            i++
        }
    }
    return keep
}

private fun setIDTextsImpl(texts: dynamic) {
    if (texts === lastIDTexts) return
    lastIDTexts = texts
    idTextsCache = texts
    if (accelImpl != null) {
        try {
            accelImpl.setIDTexts(texts)
        } catch (e: dynamic) { }
    }
}

private fun filterIDTextsImpl(s: dynamic): dynamic {
    val p = accelString(s)
    val out = js("[]")
    if (accelImpl != null) {
        try {
            val res = accelImpl.filterIDTexts(p)
            val n = accelLength(res)
            var i = 0
            while (i < n) {
                out.push(res[i])
                i++
            }
            return out
        } catch (e: dynamic) { }
    }
    if (idTextsCache != null) {
        val n = accelLength(idTextsCache)
        var i = 0
        while (i < n) {
            if (idTextsCache[i] != null && idTextsCache[i].indexOf(p) >= 0) out.push(i)
            i++
        }
    }
    return out
}

private fun filterIDTextsRawImpl(s: dynamic): dynamic {
    val p = accelString(s)
    val out = js("[]")
    if (accelImpl != null) {
        try {
            return accelImpl.filterIDTexts(p)
        } catch (e: dynamic) { }
    }
    if (idTextsCache != null) {
        val n = accelLength(idTextsCache)
        var i = 0
        while (i < n) {
            if (idTextsCache[i] != null && idTextsCache[i].indexOf(p) >= 0) out.push(i)
            i++
        }
    }
    return out
}

private fun rangeIndicesImpl(n: dynamic): dynamic {
    val out = js("[]")
    if (accelImpl != null) {
        try {
            return accelImpl.rangeIndices(n)
        } catch (e: dynamic) { }
    }
    // n 是外部传入的计数；用 dynamic 保持 JS 的 i < n 语义
    var i: dynamic = 0
    while (i < n) {
        out.push(i)
        i = i + 1
    }
    return out
}

private fun matchCommandNamesImpl(snap: dynamic, prefix: dynamic): dynamic {
    syncCommandSnapshot(snap)
    if (accelImpl != null) {
        try {
            val res = accelImpl.matchCommandNames(accelString(prefix))
            val out = js("[]")
            val n = accelLength(res)
            var i = 0
            while (i < n) {
                out.push(accelString(res[i]))
                i++
            }
            return out
        } catch (e: dynamic) { }
    }
    val qa = quickAccel()
    if (qa != null) {
        val qres = qa.matchCommandNames(snap, accelString(prefix))
        if (accelTruthy(qres)) return qres
    }
    return jsMatchCommandNames(prefix)
}

private fun compareVersionArrayImpl(a: dynamic, b: dynamic): dynamic {
    if (accelImpl != null) {
        try {
            return accelImpl.compareVersionArray(a, b)
        } catch (e: dynamic) { }
    }
    val n = accelMaxJs(a.length, b.length) as Int
    var i = 0
    while (i < n) {
        val p1: dynamic = if (accelTruthy(accelIsNaNNs(a[i]))) -1 else accelParseIntNs(a[i])
        val p2: dynamic = if (accelTruthy(accelIsNaNNs(b[i]))) -1 else accelParseIntNs(b[i])
        if (p1 < p2) return -1
        if (p1 > p2) return 1
        i++
    }
    return 0
}

/** 数组 enum 匹配：返回 { flags, length, canFinish }；有 jar 走 Java。 */
private fun classifyArrayImpl(arr: dynamic, ps: dynamic): dynamic {
    if (accelImpl != null) {
        try {
            val res = accelImpl.classifyArray(arr, accelString(ps))
            val result = js("({})")
            result.flags = res.flags
            result.length = res.length
            result.canFinish = res.canFinish
            return result
        } catch (e: dynamic) { }
    }
    val p = accelString(ps)
    val flags = js("[]")
    var length: dynamic = -1
    var canFinish = false
    val n = accelLength(arr)
    var i = 0
    while (i < n) {
        val e: dynamic = arr[i]
        if (e == null) {
            flags.push(0)
        } else if (p == e || p.indexOf(e + " ") == 0) {
            flags.push(1)
            canFinish = true
            length = accelMaxJs(length, e.length)
        } else if (e.indexOf(p) == 0) {
            flags.push(2)
            length = accelMaxJs(length, p.length)
        } else {
            flags.push(0)
        }
        i++
    }
    val result = js("({})")
    result.flags = flags
    result.length = length
    result.canFinish = canFinish
    return result
}

/** 枚举候选分类：1=精确命中 2=模糊命中 0=不命中。 */
private fun classifyImpl(keys: dynamic, descs: dynamic, ps: dynamic): dynamic {
    if (accelImpl != null) {
        try {
            return accelImpl.classify(keys, descs, accelString(ps))
        } catch (e: dynamic) { }
    }
    val flags = js("[]")
    val p = accelString(ps)
    val n = accelLength(keys)
    var i = 0
    while (i < n) {
        val k = keys[i]
        if (p == k || p.indexOf(k + " ") == 0) {
            flags.push(1)
        } else {
            val d: dynamic = if (i < (accelLength(descs)) && accelTruthy(descs[i])) descs[i] else ""
            flags.push(if (k.indexOf(p) >= 0 || d.indexOf(p) >= 0) 2 else 0)
        }
        i++
    }
    return flags
}

// 调试用：绕过候选源缓存，每次调用都直接走 Java matchString（模拟旧路径）
private fun matchStringDirectImpl(ps: dynamic, a: dynamic, r: dynamic): dynamic {
    if (accelImpl == null) return jsMatchString(ps, a, r)
    return merge(r, accelImpl.matchString(accelString(ps), a))
}

fun installAccel() {
    accelImpl = null
    idTextsCache = null
    lastIDTexts = null
    filterTextsCache = null
    commandKeys = null
    commandDescs = null
    commandSnapRef = null
    matchSourceRef = null
    matchCacheSupported = true

    try {
        val found: dynamic = js("Packages.top.projectxero.ca.accel.Accel")
        accelImpl = if (accelTruthy(found) && !accelTruthy(found.version())) null else found
    } catch (e: dynamic) {
        accelImpl = null
    }

    accelApi.available = { accelImpl != null }
    accelApi.version = { if (accelImpl != null) accelString(accelImpl.version()) else "js" }
    accelApi.matchString = { ps: dynamic, a: dynamic, r: dynamic -> matchStringImpl(ps, a, r) }
    accelApi.filter = { arr: dynamic, s: dynamic -> filterImpl(arr, s) }
    accelApi.setFilterTexts = { texts: dynamic -> setFilterTextsImpl(texts); js("undefined") }
    accelApi.filterTexts = { s: dynamic -> filterTextsImpl(s) }
    accelApi.setIDTexts = { texts: dynamic -> setIDTextsImpl(texts); js("undefined") }
    accelApi.filterIDTexts = { s: dynamic -> filterIDTextsImpl(s) }
    accelApi.filterIDTextsRaw = { s: dynamic -> filterIDTextsRawImpl(s) }
    accelApi.rangeIndices = { n: dynamic -> rangeIndicesImpl(n) }
    accelApi.setCommandSnapshot = { snap: dynamic -> syncCommandSnapshot(snap); js("undefined") }
    accelApi.matchCommandNames = { snap: dynamic, prefix: dynamic -> matchCommandNamesImpl(snap, prefix) }
    accelApi._matchCommandNamesJS = { prefix: dynamic -> jsMatchCommandNames(prefix) }
    accelApi.compareVersionArray = { a: dynamic, b: dynamic -> compareVersionArrayImpl(a, b) }
    accelApi.classifyArray = { arr: dynamic, ps: dynamic -> classifyArrayImpl(arr, ps) }
    accelApi.classify = { keys: dynamic, descs: dynamic, ps: dynamic -> classifyImpl(keys, descs, ps) }
    accelApi._matchStringDirect = { ps: dynamic, a: dynamic, r: dynamic -> matchStringDirectImpl(ps, a, r) }
    accelApi._matchStringJS = { ps: dynamic, a: dynamic, r: dynamic -> jsMatchString(ps, a, r) }

    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("Accel", accelApi)
    }
}
