package ca.kotlin

/**
 * modules/utils/QuickAccel.js 的 Kotlin/JS 版本。
 *
 * QuickJS 侧载加速（Rhino 主引擎下的纯 JS 计算 worker）。
 * worker 不可用时所有方法都返回 null，由 Accel 回退到纯 JS 实现。
 *
 * 安装位置保持在 QuickAccel 原来的加载点；Accel 里的 quickAccel()
 * 只在调用时查全局 QuickAccel，不能提前初始化。
 */
private val quickApi: dynamic = js("({})")

private val quickString: dynamic = js("function (v) { return String(v); }")
private val quickTruthyJs: dynamic = js("function (v) { return !!v; }")
private val quickIsArrayJs: dynamic = js("function (v) { return Array.isArray(v); }")
private val quickIsObjectTypeJs: dynamic = js("function (v) { return typeof v === 'object'; }")
private val quickIsFunctionJs: dynamic = js("function (v) { return typeof v === 'function'; }")
private val quickIsNumberJs: dynamic = js("function (v) { return typeof v === 'number'; }")
private val quickObjectKeys: dynamic = js("function (o) { return Object.keys(o); }")
private val quickJsonStringify: dynamic = js("function (v) { return JSON.stringify(v); }")
private val quickJsonParse: dynamic = js("function (v) { return JSON.parse(v); }")
private val quickNow: dynamic = js("function () { return Date.now(); }")
private val quickEval: dynamic = js("function (src) { return eval(src); }")

// ScriptInterface.quickWorker* 桥：参数通过 JS helper 传入，避免 js("...") 里用 Kotlin 局部名。
private val quickWorkerAvailableJs: dynamic = js("function () { return ScriptInterface.quickWorkerAvailable(); }")
private val quickWorkerRegisterJs: dynamic = js("function (source) { return ScriptInterface.quickWorkerRegister(source); }")
private val quickWorkerSetVarJs: dynamic = js("function (name, json) { return ScriptInterface.quickWorkerSetVar(name, json); }")
private val quickWorkerCallJs: dynamic = js("function (name, json) { return ScriptInterface.quickWorkerCall(name, json); }")
private val quickLengthJs: dynamic = js("function (v) { return v.length | 0; }")

private fun quickTruthy(v: dynamic): Boolean = quickTruthyJs(v)
private fun quickIsArray(v: dynamic): Boolean = quickIsArrayJs(v)
private fun quickIsObjectType(v: dynamic): Boolean = quickIsObjectTypeJs(v)
private fun quickIsFunction(v: dynamic): Boolean = quickIsFunctionJs(v)
private fun quickIsNumber(v: dynamic): Boolean = quickIsNumberJs(v)
private fun quickLength(v: dynamic): Int = quickLengthJs(v)

private fun args1(a: dynamic): dynamic {
    val out = js("[]")
    out.push(a)
    return out
}

// 基准函数（纯 JS、无 Java 依赖；结果要稳定便于校验）
private const val BENCH_SOURCE =
    "function bench(n) {" +
    "var a = [], i, s = 0;" +
    "for (i = 0; i < n; i++) a.push((i * 2654435761) % 1000003);" +
    "a.sort(function (x, y) { return x - y; });" +
    "for (i = 0; i < a.length; i++) s = (s + a[i] * (i + 1)) % 1000000007;" +
    "return s;" +
    "}"

// ---------------- 命令名匹配（真实热点：每次输入都跑） ----------------
// worker 里缓存 command_snap（只同步一次），之后每次只传前缀、回传匹配数组
private const val MATCH_SOURCE =
    "function matchCommands(prefix) {" +
    "var t = globalThis.__commandSnap, out = [], k;" +
    "if (!t) return [];" +
    "for (k in t) {" +
    "if (k.indexOf(prefix) >= 0 || String(t[k]).indexOf(prefix) >= 0) out.push(k);" +
    "}" +
    "out.sort();" +
    "return out;" +
    "}"

// 其余无 jar 回退用的纯计算函数。数据由 quickWorkerSetVar 缓存一次，
// 每次只传前缀，回传 flags / 掩码。
private val ACCEL_SOURCE: String = MATCH_SOURCE + "\n" + listOf(
    "function classifyCached(ps) {",
    "  var g = globalThis, keys = g.__classifyKeys, descs = g.__classifyDescs;",
    "  var p = String(ps), out = [], i, k, d;",
    "  if (!keys) return out;",
    "  for (i = 0; i < keys.length; i++) {",
    "    k = keys[i];",
    "    if (p == k || p.indexOf(k + ' ') == 0) { out.push(1); continue; }",
    "    d = (descs && i < descs.length && descs[i]) ? descs[i] : '';",
    "    out.push((k.indexOf(p) >= 0 || d.indexOf(p) >= 0) ? 2 : 0);",
    "  }",
    "  return out;",
    "}",
    "function classifyArrayCached(ps) {",
    "  var g = globalThis, arr = g.__classifyArray, p = String(ps);",
    "  var flags = [], length = -1, canFinish = false, i, e;",
    "  if (!arr) return { flags: flags, length: length, canFinish: canFinish };",
    "  for (i = 0; i < arr.length; i++) {",
    "    e = arr[i];",
    "    if (e == null) { flags.push(0); continue; }",
    "    if (p == e || p.indexOf(e + ' ') == 0) {",
    "      flags.push(1); canFinish = true; length = Math.max(length, e.length);",
    "    } else if (e.indexOf(p) == 0) {",
    "      flags.push(2); length = Math.max(length, p.length);",
    "    } else {",
    "      flags.push(0);",
    "    }",
    "  }",
    "  return { flags: flags, length: length, canFinish: canFinish };",
    "}",
    "function filterTextsCached(ps) {",
    "  var g = globalThis, a = g.__filterTexts, p = String(ps), out = [], i;",
    "  if (!a) return out;",
    "  for (i = 0; i < a.length; i++) out.push(a[i] != null && String(a[i]).indexOf(p) >= 0);",
    "  return out;",
    "}",
    "function searchTextsCached(ps) {",
    "  var g = globalThis, a = g.__searchTexts, p = String(ps), out = [], i;",
    "  if (!a) return out;",
    "  for (i = 0; i < a.length; i++) if (String(a[i]).indexOf(p) >= 0) out.push(i);",
    "  return out;",
    "}",
    "function bundleStringsCached(parts) {",
    "  var msp = [], msi, msn, r = [], cur, i;",
    "  if (!parts) return r;",
    "  for (i = 0; i < parts.length; i++) if (Array.isArray(parts[i])) msp.push(i);",
    "  if (msp.length === 0) { if (parts.length === 0) parts = ['']; parts[0] = [parts[0]]; msp.push(0); }",
    "  msi = new Array(msp.length); msn = new Array(msp.length);",
    "  for (i = 0; i < msp.length; i++) { msi[i] = 0; msn[i] = parts[msp[i]].length; }",
    "  while (msi[0] < msn[0]) {",
    "    cur = parts.slice();",
    "    for (i = 0; i < msp.length; i++) cur[msp[i]] = cur[msp[i]][msi[i]];",
    "    for (i = 0; i < cur.length; i++) {",
    "      if (cur[i] && typeof cur[i] === 'object' && !Array.isArray(cur[i])) {",
    "        if (cur[i].type === 'map') {",
    "          cur[i] = cur[i].index < 0 ? '{找不到标签}' : cur[cur[i].index];",
    "        } else if (cur[i].type === 'syncmap') {",
    "          var t2 = msp.indexOf(cur[i].target);",
    "          var idx = t2 >= 0 ? msi[t2] : 0;",
    "          cur[i] = idx < cur[i].arr.length ? cur[i].arr[idx] : '{下标超出}';",
    "        } else { cur[i] = ''; }",
    "      }",
    "    }",
    "    r.push(cur.join(''));",
    "    msi[msi.length - 1]++;",
    "    for (i = msi.length - 1; i > 0; i--) {",
    "      if (msi[i] >= msn[i]) { msi[i] = 0; msi[i - 1]++; } else break;",
    "    }",
    "  }",
    "  return r;",
    "}",
    "function buildIDCached() {",
    "  var g = globalThis, names = g.__idNames, enums = g.__idEnums || {};",
    "  var seen = {}, ids = [], texts = [], i, j, e, ks, id;",
    "  if (!names) return { ids: ids, texts: texts };",
    "  for (i = 0; i < names.length; i++) {",
    "    e = names[i];",
    "    if (typeof e === 'string') e = enums[e];",
    "    if (e == null) continue;",
    "    if (Array.isArray(e)) {",
    "      for (j = 0; j < e.length; j++) {",
    "        id = e[j];",
    "        if (seen[id] !== undefined) continue;",
    "        seen[id] = id; ids.push(id);",
    "      }",
    "    } else {",
    "      ks = Object.keys(e);",
    "      for (j = 0; j < ks.length; j++) {",
    "        id = ks[j];",
    "        if (seen[id] !== undefined) continue;",
    "        seen[id] = id + (e[id] ? ' - ' + e[id] : '');",
    "        ids.push(id);",
    "      }",
    "    }",
    "  }",
    "  ids.sort();",
    "  for (i = 0; i < ids.length; i++) texts[i] = seen[ids[i]];",
    "  return { ids: ids, texts: texts };",
    "}"
).joinToString("\n")

private fun ensureWorkerImpl(): Boolean {
    if (!quickTruthy(quickApi.available)) return false
    if (!quickTruthy(quickApi.registered)) {
        quickApi.registered = quickWorkerRegisterJs(quickApi.accelSource)
        if (!quickTruthy(quickApi.registered)) android.util.Log.w("Script", "QuickAccel worker 注册失败")
    }
    return quickTruthy(quickApi.registered)
}

private fun syncVarImpl(name: dynamic, value: dynamic, refKey: dynamic): dynamic {
    try {
        val json = quickJsonStringify(value)
        val ok = quickWorkerSetVarJs(name, json)
        if (quickTruthy(ok)) quickApi[refKey] = value
        else android.util.Log.w("Script", "QuickAccel syncVar 失败: " + name + " len=" + json.length)
        return ok
    } catch (e: dynamic) {
        android.util.Log.w("Script", "QuickAccel syncVar 异常: " + name + " => " + e)
        return false
    }
}

private fun classifyImpl(keys: dynamic, descs: dynamic, ps: dynamic): dynamic {
    if (!quickTruthy(quickApi.available) || !quickTruthy(keys) || !ensureWorkerImpl()) return null
    try {
        if (keys !== quickApi.classifyKeysRef || descs !== quickApi.classifyDescsRef) {
            if (!quickTruthy(syncVarImpl("__classifyKeys", keys, "classifyKeysRef"))) return null
            val descsValue: dynamic = if (quickTruthy(descs)) descs else js("[]")
            if (!quickTruthy(syncVarImpl("__classifyDescs", descsValue, "classifyDescsRef"))) return null
        }
        val res = quickWorkerCallJs("classifyCached", quickJsonStringify(args1(ps)))
        if (!quickTruthy(res)) android.util.Log.w("Script", "QuickAccel classifyCached 返回 null")
        return if (quickTruthy(res)) quickJsonParse(res) else null
    } catch (e: dynamic) {
        android.util.Log.w("Script", "QuickAccel classifyCached 异常: " + e)
        return null
    }
}

private fun classifyArrayImpl(arr: dynamic, ps: dynamic): dynamic {
    if (!quickTruthy(quickApi.available) || !quickTruthy(arr) || !ensureWorkerImpl()) return null
    try {
        if (arr !== quickApi.classifyArrayRef) {
            if (!quickTruthy(syncVarImpl("__classifyArray", arr, "classifyArrayRef"))) return null
        }
        val res = quickWorkerCallJs("classifyArrayCached", quickJsonStringify(args1(ps)))
        if (!quickTruthy(res)) android.util.Log.w("Script", "QuickAccel classifyArrayCached 返回 null")
        return if (quickTruthy(res)) quickJsonParse(res) else null
    } catch (e: dynamic) {
        android.util.Log.w("Script", "QuickAccel classifyArrayCached 异常: " + e)
        return null
    }
}

private fun filterTextsImpl(texts: dynamic, ps: dynamic): dynamic {
    if (!quickTruthy(quickApi.available) || !quickTruthy(texts) || !ensureWorkerImpl()) return null
    try {
        if (texts !== quickApi.filterTextsRef) {
            if (!quickTruthy(syncVarImpl("__filterTexts", texts, "filterTextsRef"))) return null
        }
        val res = quickWorkerCallJs("filterTextsCached", quickJsonStringify(args1(ps)))
        if (!quickTruthy(res)) android.util.Log.w("Script", "QuickAccel filterTextsCached 返回 null")
        return if (quickTruthy(res)) quickJsonParse(res) else null
    } catch (e: dynamic) {
        android.util.Log.w("Script", "QuickAccel filterTextsCached 异常: " + e)
        return null
    }
}

private fun ensureLibraryImpl(snap: dynamic): Boolean {
    if (!quickTruthy(quickApi.available) || !quickTruthy(snap)) return false
    if (!ensureWorkerImpl()) return false
    if (!quickTruthy(quickApi.snapSent)) {
        val t0 = quickNow()
        val ok = quickWorkerSetVarJs("__commandSnap", quickJsonStringify(snap))
        quickApi.snapSent = ok
        android.util.Log.i("Script", "QuickAccel: 命令库同步到 worker " + (quickNow() - t0) + "ms ok=" + ok)
    }
    return quickTruthy(quickApi.registered) && quickTruthy(quickApi.snapSent)
}

// 返回匹配命令名数组；worker 不可用/失败时返回 null（调用方回退原 JS 实现）
private fun matchCommandNamesImpl(snap: dynamic, prefix: dynamic): dynamic {
    try {
        if (!quickTruthy(quickApi.available) || !ensureLibraryImpl(snap)) return null
        val res = quickWorkerCallJs("matchCommands", quickJsonStringify(args1(prefix)))
        return if (quickTruthy(res)) quickJsonParse(res) else null
    } catch (e: dynamic) {
        android.util.Log.w("Script", "QuickAccel.matchCommandNames 失败: " + e)
        return null
    }
}

// 基准用：把 ID 原始数据和 enums 同步到 worker，再在 worker 里构建
private fun buildIDListImpl(names: dynamic, enums: dynamic): dynamic {
    try {
        if (!quickTruthy(quickApi.available) || !quickTruthy(names) || !ensureWorkerImpl()) return null
        val t0 = quickNow()
        var ok: dynamic = true
        if (names !== quickApi.idNamesRef) ok = ok && syncVarImpl("__idNames", names, "idNamesRef")
        val enumsValue: dynamic = if (quickTruthy(enums)) enums else js("({})")
        if (enums !== quickApi.idEnumsRef) ok = ok && syncVarImpl("__idEnums", enumsValue, "idEnumsRef")
        quickApi.lastIDSyncMs = quickNow() - t0
        if (!quickTruthy(ok)) return null
        val t1 = quickNow()
        val res = quickWorkerCallJs("buildIDCached", "[]")
        quickApi.lastIDBuildMs = quickNow() - t1
        if (!quickTruthy(res)) android.util.Log.w("Script", "QuickAccel buildIDCached 返回 null")
        return if (quickTruthy(res)) quickJsonParse(res) else null
    } catch (e: dynamic) {
        android.util.Log.w("Script", "QuickAccel.buildIDList 异常: " + e)
        return null
    }
}

// 基准用：批量导出（只接受字符串/字符串数组，含函数或对象时返回 null）
private fun bundleStringsImpl(parts: dynamic): dynamic {
    try {
        if (!quickTruthy(quickApi.available) || !quickTruthy(parts) || !ensureWorkerImpl()) return null
        val partCount = quickLength(parts)
        var i = 0
        while (i < partCount) {
            val e: dynamic = parts[i]
            if (quickIsFunction(e)) return null
            if (quickTruthy(e) && quickIsObjectType(e) && !quickIsArray(e)) {
                if (e.type == "map" && quickIsNumber(e.index)) {
                    // 允许
                } else if (e.type == "syncmap" && quickIsArray(e.arr) && quickIsNumber(e.target)) {
                    // 允许
                } else {
                    return null
                }
            } else if (quickIsArray(e)) {
                val itemCount = quickLength(e)
                var j = 0
                while (j < itemCount) {
                    val item: dynamic = e[j]
                    if (quickIsFunction(item) || (quickTruthy(item) && quickIsObjectType(item))) return null
                    j++
                }
            }
            i++
        }
        val res = quickWorkerCallJs("bundleStringsCached", quickJsonStringify(args1(parts)))
        return if (quickTruthy(res)) quickJsonParse(res) else null
    } catch (e: dynamic) {
        android.util.Log.w("Script", "QuickAccel.bundleStrings 异常: " + e)
        return null
    }
}

// 大文本搜索：缓存 texts，之后每次只传查询串；供未来搜索 UI/基准使用
private fun searchTextsImpl(texts: dynamic, query: dynamic): dynamic {
    try {
        if (!quickTruthy(quickApi.available) || !quickTruthy(texts) || !ensureWorkerImpl()) return null
        if (texts !== quickApi.searchTextsRef) {
            if (!quickTruthy(syncVarImpl("__searchTexts", texts, "searchTextsRef"))) return null
        }
        val res = quickWorkerCallJs("searchTextsCached", quickJsonStringify(args1(quickString(query))))
        return if (quickTruthy(res)) quickJsonParse(res) else null
    } catch (e: dynamic) {
        android.util.Log.w("Script", "QuickAccel.searchTexts 异常: " + e)
        return null
    }
}

// 匹配基准：Rhino 直接 filter vs worker
private fun runMatchBenchImpl(n: dynamic, prefix: dynamic) {
    try {
        val snap = js("({})")
        // n 是外部传入的计数；用 dynamic 保持 JS 的 i < n 语义
        var i: dynamic = 0
        while (i < n) {
            snap["cmd" + i] = "参数说明 " + i + " description text for command " + i
            i = i + 1
        }
        val t0 = quickNow()
        val ks = quickObjectKeys(snap)
        val ksCount = quickLength(ks)
        val rhinoNames = js("[]")
        i = 0
        while (i < ksCount) {
            val e = ks[i]
            if (e.indexOf(prefix) >= 0 || snap[e].indexOf(prefix) >= 0) rhinoNames.push(e)
            i++
        }
        rhinoNames.sort()
        val rhinoMs = quickNow() - t0

        quickApi.snapSent = false
        quickApi.registered = false
        val t1 = quickNow()
        val quickNames = matchCommandNamesImpl(snap, prefix)
        val quickMs = quickNow() - t1
        // 与原实现一致：quickNames 为假值时 "same" 输出该假值（例如 null）
        val same: dynamic = if (quickTruthy(quickNames)) {
            quickNames.length === rhinoNames.length && quickNames[0] === rhinoNames[0]
        } else {
            quickNames
        }
        android.util.Log.i("Script", "QuickAccel match n=" + n + " rhino=" + rhinoMs +
            "ms quickjs=" + quickMs + "ms same=" + same)
    } catch (e: dynamic) {
        android.util.Log.w("Script", "QuickAccel match 基准失败: " + e)
    }
}

private fun runBenchImpl(n: dynamic) {
    try {
        if (!quickTruthy(quickApi.available)) {
            android.util.Log.i("Script", "QuickAccel: worker 不可用，跳过")
            return
        }
        val rhinoFn = quickEval("(" + quickApi.benchSource + ")")
        val t0 = quickNow()
        val rhinoResult = rhinoFn(n)
        val rhinoMs = quickNow() - t0

        val ok = quickWorkerRegisterJs(quickApi.benchSource)
        val t1 = quickNow()
        val quickResult = quickWorkerCallJs("bench", quickJsonStringify(args1(n)))
        val quickMs = quickNow() - t1

        android.util.Log.i("Script", "QuickAccel bench n=" + n +
            " rhino=" + rhinoMs + "ms quickjs=" + quickMs + "ms" +
            " regOk=" + ok + " same=" + (quickString(rhinoResult) === quickString(quickResult)) +
            " quickResult=" + quickResult)
    } catch (e: dynamic) {
        val stack: dynamic = if (quickTruthy(e.stack)) e.stack else ""
        android.util.Log.w("Script", "QuickAccel 基准失败: " + e + " | " + stack)
    }
}

fun installQuickAccel() {
    var available = false
    try {
        available = quickTruthy(quickWorkerAvailableJs())
    } catch (e: dynamic) {
        available = false
    }
    quickApi.available = available

    if (available) {
        android.util.Log.i("Script", "QuickAccel: QuickJS worker 可用（quickjs-kt 16KB native）")
    }

    quickApi.benchSource = BENCH_SOURCE
    quickApi.matchSource = MATCH_SOURCE
    quickApi.accelSource = ACCEL_SOURCE
    quickApi.registered = false
    quickApi.snapSent = false
    quickApi.classifyKeysRef = null
    quickApi.classifyDescsRef = null
    quickApi.classifyArrayRef = null
    quickApi.filterTextsRef = null
    quickApi.idNamesRef = null
    quickApi.idEnumsRef = null
    quickApi.searchTextsRef = null
    quickApi.lastIDSyncMs = 0
    quickApi.lastIDBuildMs = 0

    quickApi.ensureWorker = { -> ensureWorkerImpl() }
    quickApi.syncVar = { name: dynamic, value: dynamic, refKey: dynamic -> syncVarImpl(name, value, refKey) }
    quickApi.classify = { keys: dynamic, descs: dynamic, ps: dynamic -> classifyImpl(keys, descs, ps) }
    quickApi.classifyArray = { arr: dynamic, ps: dynamic -> classifyArrayImpl(arr, ps) }
    quickApi.filterTexts = { texts: dynamic, ps: dynamic -> filterTextsImpl(texts, ps) }
    quickApi.ensureLibrary = { snap: dynamic -> ensureLibraryImpl(snap) }
    quickApi.matchCommandNames = { snap: dynamic, prefix: dynamic -> matchCommandNamesImpl(snap, prefix) }
    quickApi.buildIDList = { names: dynamic, enums: dynamic -> buildIDListImpl(names, enums) }
    quickApi.bundleStrings = { parts: dynamic -> bundleStringsImpl(parts) }
    quickApi.searchTexts = { texts: dynamic, query: dynamic -> searchTextsImpl(texts, query) }
    quickApi.runMatchBench = { n: dynamic, prefix: dynamic -> runMatchBenchImpl(n, prefix); js("undefined") }
    quickApi.runBench = { n: dynamic -> runBenchImpl(n); js("undefined") }

    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("QuickAccel", quickApi)
    }
}
