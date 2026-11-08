package ca.kotlin.ca

import ca.kotlin.core.*


/**
 * modules/CA.IntelliSense.js 的 Kotlin/JS 版本。
 *
 * 只提供 fun buildCaIntelliSenseModule(): dynamic，返回一个与原 CA.IntelliSense 等价的
 * JS 对象；这里不调用 MapScript.loadModule、不定义全局 CA、也不自行注册。
 * Lead 会在 CA.kt 里执行 `caApi.IntelliSense = buildCaIntelliSenseModule()`。
 *
 * 迁移约定：
 *  - 原对象字面量里的 this 改成显式的 isApi；各方法之间通过 isApi.xxx 调用。
 *  - 对 CA / Common / G / Accel / MCAdapter / PWM 等外部全局的引用保持原动态查找。
 *  - callDelay 的 `function self(s)` 与 show 的 `function self()` 把状态挂在函数对象本身，
 *    这里用薄 JS 工厂保留 self 语义、函数名与 arity；show 内部后来覆盖的 self.apply /
 *    self.vmaker / self.vbinder 也仍然挂在这个函数对象上。
 *  - JS 的动态真值、宽松相等、字符串切片/正则/数组长度都走带参 helper；
 *    js("...") 里不引用 Kotlin 局部变量。
 *  - Java 重载问题：本文件用到的构造器都按参数个数即可区分（SpannableStringBuilder、
 *    TranslateAnimation 4/8 参、LayoutParams 等），因此没有额外走 JavaReflect。
 *
 * 原实现里刻意按原样保留的行为/缺陷（相应位置都有注释）：
 *  - procParams 的 `t2` 从未声明，原实现会写成隐式全局；这里显式写入
 *    globalThis.t2 并从全局读回，保留原行为（包括污染全局对象）。
 *  - procParams 每个 pattern 复用同一个 cpl 数组（cpl.length = 0 后 push 同一个引用），
 *    所以 c.patterns 里的多项最终会共享最后一次的内容；这里保持同一数组引用。
 *  - procParams 的 `ci += t.length; ++ci` 写在 if 条件里，空白检查失败时 ci 也会前进；
 *    这里按原求值顺序先更新 ci 再判断。
 *  - jsonPro 最后把 { text, cursor } 降级成字符串，放弃精确光标跳转；保持。
 *  - getSchemaByContext 的 `this && this.library && ...` 保留短路语义。
 */

// ---------------- JS 语义 helper ----------------

private val iiTruthyJs: dynamic = js("function (v) { return !!v; }")
private fun iiTruthy(v: dynamic): Boolean = iiTruthyJs(v)

private val iiLengthJs: dynamic = js("function (v) { return v.length | 0; }")
private fun iiLength(v: dynamic): Int = iiLengthJs(v)

private val iiStringJs: dynamic = js("function (v) { return String(v); }")
private fun iiString(v: dynamic): dynamic = iiStringJs(v)

private val iiTypeofJs: dynamic = js("function (v) { return typeof v; }")
private fun iiTypeof(v: dynamic): dynamic = iiTypeofJs(v)

private val iiLooseEqJs: dynamic = js("function (a, b) { return a == b; }")
private fun iiLooseEq(a: dynamic, b: dynamic): Boolean = iiLooseEqJs(a, b)

private val iiLooseNeJs: dynamic = js("function (a, b) { return a != b; }")
private fun iiLooseNe(a: dynamic, b: dynamic): Boolean = iiLooseNeJs(a, b)

private val iiStrictEqJs: dynamic = js("function (a, b) { return a === b; }")
private fun iiStrictEq(a: dynamic, b: dynamic): Boolean = iiStrictEqJs(a, b)

private val iiStrictNeJs: dynamic = js("function (a, b) { return a !== b; }")
private fun iiStrictNe(a: dynamic, b: dynamic): Boolean = iiStrictNeJs(a, b)

private val iiInJs: dynamic = js("function (k, o) { return k in o; }")
private fun iiIn(k: dynamic, o: dynamic): Boolean = iiInJs(k, o)

private val iiInstanceOfJs: dynamic = js("function (v, c) { return v instanceof c; }")
private fun iiInstanceOf(v: dynamic, c: dynamic): Boolean = iiInstanceOfJs(v, c)

private val iiIsArrayJs: dynamic = js("function (v) { return Array.isArray(v); }")
private fun iiIsArray(v: dynamic): Boolean = iiIsArrayJs(v)

private val iiIsNaNJs: dynamic = js("function (v) { return isNaN(v); }")
private fun iiIsNaN(v: dynamic): Boolean = iiIsNaNJs(v)

private val iiIsUndefinedJs: dynamic = js("function (v) { return v === undefined; }")
private fun iiIsUndefined(v: dynamic): Boolean = iiIsUndefinedJs(v)

private val iiAndJs: dynamic = js("function (a, b) { return a && b; }")
private fun iiAnd(a: dynamic, b: dynamic): dynamic = iiAndJs(a, b)

private val iiOrJs: dynamic = js("function (a, b) { return a || b; }")
private fun iiOr(a: dynamic, b: dynamic): dynamic = iiOrJs(a, b)

private val iiNotJs: dynamic = js("function (v) { return !v; }")
private fun iiNot(v: dynamic): dynamic = iiNotJs(v)

private val iiAddJs: dynamic = js("function (a, b) { return a + b; }")
private fun iiAdd(a: dynamic, b: dynamic): dynamic = iiAddJs(a, b)

private val iiSubJs: dynamic = js("function (a, b) { return a - b; }")
private fun iiSub(a: dynamic, b: dynamic): dynamic = iiSubJs(a, b)

private val iiMulJs: dynamic = js("function (a, b) { return a * b; }")
private fun iiMul(a: dynamic, b: dynamic): dynamic = iiMulJs(a, b)

private val iiDivJs: dynamic = js("function (a, b) { return a / b; }")
private fun iiDiv(a: dynamic, b: dynamic): dynamic = iiDivJs(a, b)

private val iiNegJs: dynamic = js("function (v) { return -v; }")
private fun iiNeg(v: dynamic): dynamic = iiNegJs(v)

private val iiLtJs: dynamic = js("function (a, b) { return a < b; }")
private fun iiLt(a: dynamic, b: dynamic): Boolean = iiLtJs(a, b)

private val iiLeJs: dynamic = js("function (a, b) { return a <= b; }")
private fun iiLe(a: dynamic, b: dynamic): Boolean = iiLeJs(a, b)

private val iiGtJs: dynamic = js("function (a, b) { return a > b; }")
private fun iiGt(a: dynamic, b: dynamic): Boolean = iiGtJs(a, b)

private val iiGeJs: dynamic = js("function (a, b) { return a >= b; }")
private fun iiGe(a: dynamic, b: dynamic): Boolean = iiGeJs(a, b)

private val iiMinJs: dynamic = js("function (a, b) { return Math.min(a, b); }")
private fun iiMin(a: dynamic, b: dynamic): dynamic = iiMinJs(a, b)

private val iiMaxJs: dynamic = js("function (a, b) { return Math.max(a, b); }")
private fun iiMax(a: dynamic, b: dynamic): dynamic = iiMaxJs(a, b)

private val iiObjectKeysJs: dynamic = js("function (o) { return Object.keys(o); }")
private fun iiKeys(o: dynamic): dynamic = iiObjectKeysJs(o)

/** for...in 的键快照；保留原实现包含继承属性的语义。 */
private val iiForInKeysJs: dynamic = js(
    "function (o) { var a = []; for (var k in o) a.push(k); return a; }"
)
private fun iiForInKeys(o: dynamic): dynamic = iiForInKeysJs(o)

private val iiHasOwnJs: dynamic = js("function (o, k) { return o.hasOwnProperty(k); }")
private fun iiHasOwn(o: dynamic, k: dynamic): Boolean = iiHasOwnJs(o, k)

/** 原 procParams 里未声明的 t2 实际是全局变量；显式读写 globalThis.t2 保留该行为。 */
private val iiSetGlobalT2Js: dynamic = js("function (v) { globalThis.t2 = v; }")
private val iiGetGlobalT2Js: dynamic = js("function () { return globalThis.t2; }")

private val iiSortCountDescJs: dynamic = js("function (a, b) { return b.count - a.count; }")
private val iiSortOffsetAscJs: dynamic = js("function (a, b) { return a.offset - b.offset; }")

private fun iiObject(): dynamic = js("({})")

private fun iiArray(): dynamic = js("[]")

private fun iiArrayOf(vararg items: dynamic): dynamic {
    val a = js("[]")
    var i = 0
    while (i < items.size) {
        a.push(items[i])
        i++
    }
    return a
}

private fun iiDescription(msg: dynamic): dynamic {
    val o = iiObject()
    o.description = msg
    return o
}

// ---------------- 字符串 helper ----------------

private val iiSliceJs: dynamic = js("function (s, a, b) { return s.slice(a, b); }")
private fun iiSlice(s: dynamic, a: dynamic, b: dynamic): dynamic = iiSliceJs(s, a, b)

private val iiSliceFromJs: dynamic = js("function (s, a) { return s.slice(a); }")
private fun iiSliceFrom(s: dynamic, a: dynamic): dynamic = iiSliceFromJs(s, a)

private val iiSubstringJs: dynamic = js("function (s, a, b) { return s.substring(a, b); }")
private fun iiSubstring(s: dynamic, a: dynamic, b: dynamic): dynamic = iiSubstringJs(s, a, b)

private val iiSubstringFromJs: dynamic = js("function (s, a) { return s.substring(a); }")
private fun iiSubstringFrom(s: dynamic, a: dynamic): dynamic = iiSubstringFromJs(s, a)

private val iiCharAtJs: dynamic = js("function (s, i) { return s.charAt(i); }")
private fun iiCharAt(s: dynamic, i: dynamic): dynamic = iiCharAtJs(s, i)

private val iiIndexOfJs: dynamic = js("function (s, x) { return s.indexOf(x); }")
private fun iiIndexOf(s: dynamic, x: dynamic): dynamic = iiIndexOfJs(s, x)

private val iiLastIndexOfJs: dynamic = js("function (s, x) { return s.lastIndexOf(x); }")
private fun iiLastIndexOf(s: dynamic, x: dynamic): dynamic = iiLastIndexOfJs(s, x)

private val iiStartsWithJs: dynamic = js("function (s, p) { return s.startsWith(p); }")
private fun iiStartsWith(s: dynamic, p: dynamic): Boolean = iiStartsWithJs(s, p)

private val iiEndsWithJs: dynamic = js("function (s, p) { return s.endsWith(p); }")
private fun iiEndsWith(s: dynamic, p: dynamic): Boolean = iiEndsWithJs(s, p)

private val iiTrimJs: dynamic = js("function (s) { return s.trim(); }")
private fun iiTrim(s: dynamic): dynamic = iiTrimJs(s)

private val iiSplitJs: dynamic = js("function (s, d) { return s.split(d); }")
private fun iiSplit(s: dynamic, d: dynamic): dynamic = iiSplitJs(s, d)

private val iiJoinJs: dynamic = js("function (a, s) { return a.join(s); }")
private fun iiJoin(a: dynamic, s: dynamic): dynamic = iiJoinJs(a, s)

private val iiConcat2Js: dynamic = js("function (a, b) { return a.concat(b); }")
private fun iiConcat2(a: dynamic, b: dynamic): dynamic = iiConcat2Js(a, b)

private val iiConcat3Js: dynamic = js("function (a, b, c) { return a.concat(b, c); }")
private fun iiConcat3(a: dynamic, b: dynamic, c: dynamic): dynamic = iiConcat3Js(a, b, c)

/** 原实现里反复出现的 `search(" ") < 0 ? ps : ps.slice(0, search(" "))`。 */
private val iiFirstTokenJs: dynamic = js(
    "function (s) { return s.search(\" \") < 0 ? s : s.slice(0, s.search(\" \")); }"
)
private fun iiFirstToken(s: dynamic): dynamic = iiFirstTokenJs(s)

private val iiFloorJoin3Js: dynamic = js(
    "function (a, b, c) { return [Math.floor(a), Math.floor(b), Math.floor(c)].join(\" \"); }"
)
private fun iiFloorJoin3(a: dynamic, b: dynamic, c: dynamic): dynamic = iiFloorJoin3Js(a, b, c)

// ---------------- 正则 helper ----------------

private val iiExecCmdJs: dynamic = js(
    """function (s) { return /^(\/)?(\S*)(\s+)?(.*)/.exec(s); }"""
)

private val iiTestWs01Js: dynamic = js(
    """function (s) { return /^\s?$/.test(s); }"""
)

private val iiLeadingWsLengthJs: dynamic = js(
    """function (s) { return /^\s*/.exec(s)[0].length; }"""
)
private fun iiLeadingWsLength(s: dynamic): dynamic = iiLeadingWsLengthJs(s)

private val iiExecPositionTagJs: dynamic = js(
    """function (s) { return /(\S*)\s*(\S*)\s*(\S*)/.exec(s); }"""
)

private val iiExecSelectorJs: dynamic = js(
    """function (s) { return /^@(p|e|a|r|s|)(\[)?([^\s\]]*)(\])?(\s)?/.exec(s); }"""
)

private val iiTestPlayerNameJs: dynamic = js(
    """function (s) { return /^[^@\^~]*$/.test(s); }"""
)

private val iiTestNumPairJs: dynamic = js(
    """function (s) { return /^(\+|-)?(\d*\.)?\d+$/.test(s); }"""
)

private val iiExecSelectorPairJs: dynamic = js(
    """function (s) { return /^([^\=]+)(\=)(.*)$/.exec(s); }"""
)

private val iiExecSelectorNumJs: dynamic = js(
    """function (s) { return /^(\+|-)?(\d*\.)?\d*$/.exec(s); }"""
)

private val iiExecSelectorPartialJs: dynamic = js(
    """function (s) { return /^([^\=]*)(\=)?(.*)$/.exec(s); }"""
)

private val iiTestInvertedJs: dynamic = js(
    """function (s) { return s.search(/^!/) == 0; }"""
)

private val iiTestUintJs: dynamic = js(
    """function (s) { return /^\d*$/.test(s); }"""
)

private val iiTestIntJs: dynamic = js(
    """function (s) { return /^(\+|-)?\d*$/.test(s); }"""
)

private val iiExecFloatJs: dynamic = js(
    """function (s) { return /^(\+|-)?(\d*\.)?(\d)*$/.exec(s); }"""
)

private val iiExecRelativeJs: dynamic = js(
    """function (s) { return /^(~)?((\+|-)?(\d*\.)?(\d)*)$/.exec(s); }"""
)

private val iiTestCoordValueJs: dynamic = js(
    """function (s) { return /^(\+|-)?(\d*\.)?\d+$/.test(s); }"""
)

private val iiExecCoordLocalJs: dynamic = js(
    """function (s) { return /^(?:(\^)((\+|-)?(\d*\.)?\d*))?$/.exec(s); }"""
)

private val iiExecCoordJs: dynamic = js(
    """function (s) { return /^(~)?((\+|-)?(\d*\.)?\d*)$/.exec(s); }"""
)

private val iiExecPosition3Js: dynamic = js(
    """function (s) { return /^\S+\s+\S+\s+\S+/.exec(s); }"""
)

private val iiSplitWsJs: dynamic = js(
    """function (s) { return s.split(/\s+/); }"""
)

private val iiRemoveSnippetJs: dynamic = js(
    """function (s) { return s.replace(/\$\d+/g, ""); }"""
)

private val iiEscapeSelectorValueJs: dynamic = js(
    """function (s) { return s.replace(/\\/g, '\\\\').replace(/"/g, '\\"'); }"""
)

private val iiNewRegExpExecJs: dynamic = js(
    "function (p, s) { return new RegExp(p, \"\").exec(s); }"
)
private fun iiNewRegExpExec(p: dynamic, s: dynamic): dynamic = iiNewRegExpExecJs(p, s)

private val iiRegExpTestJs: dynamic = js(
    "function (p, s) { return new RegExp(p, \"\").test(s); }"
)
private fun iiRegExpTest(p: dynamic, s: dynamic): Boolean = iiRegExpTestJs(p, s)

// ---------------- 全局对象 / 构造 helper ----------------

private val iiAccelDefinedJs: dynamic = js(
    "function () { return typeof Accel !== 'undefined'; }"
)
private fun iiAccelDefined(): Boolean = iiAccelDefinedJs()

private val iiAccelAvailableJs: dynamic = js(
    "function () { return Accel.available(); }"
)
private fun iiAccelAvailable(): dynamic = iiAccelAvailableJs()

private val iiAccelMatchCommandNamesJs: dynamic = js(
    "function (t, s) { return Accel.matchCommandNames(t, s); }"
)
private fun iiAccelMatchCommandNames(t: dynamic, s: dynamic): dynamic =
    iiAccelMatchCommandNamesJs(t, s)

private val iiQuickAccelDefinedJs: dynamic = js(
    "function () { return typeof QuickAccel !== 'undefined'; }"
)
private fun iiQuickAccelDefined(): Boolean = iiQuickAccelDefinedJs()

private val iiQuickAccelMatchCommandNamesJs: dynamic = js(
    "function (t, s) { return QuickAccel.matchCommandNames(t, s); }"
)
private fun iiQuickAccelMatchCommandNames(t: dynamic, s: dynamic): dynamic =
    iiQuickAccelMatchCommandNamesJs(t, s)

private val iiAccelClassifyArrayJs: dynamic = js(
    "function (t, ps) { return Accel.classifyArray(t, ps); }"
)
private fun iiAccelClassifyArray(t: dynamic, ps: dynamic): dynamic = iiAccelClassifyArrayJs(t, ps)

private val iiAccelClassifyJs: dynamic = js(
    "function (keys, descs, ps) { return Accel.classify(keys, descs, ps); }"
)
private fun iiAccelClassify(keys: dynamic, descs: dynamic, ps: dynamic): dynamic =
    iiAccelClassifyJs(keys, descs, ps)

private val iiAccelMatchStringJs: dynamic = js(
    "function (ps, a, r) { return Accel.matchString(ps, a, r); }"
)
private fun iiAccelMatchString(ps: dynamic, a: dynamic, r: dynamic): dynamic =
    iiAccelMatchStringJs(ps, a, r)

private val iiMcAvailableJs: dynamic = js(
    "function () { return MCAdapter.available(); }"
)
private fun iiMcAvailable(): dynamic = iiMcAvailableJs()

private val iiMcGetInfoJs: dynamic = js(
    "function (k) { return MCAdapter.getInfo(k); }"
)
private fun iiMcGetInfo(k: dynamic): dynamic = iiMcGetInfoJs(k)

private val iiMcApplySenseJs: dynamic = js(
    "function (t) { MCAdapter.applySense(t); }"
)
private fun iiMcApplySense(t: dynamic) {
    iiMcApplySenseJs(t)
}

private val iiJsonLanguageServiceJs: dynamic = js(
    "function (o) { return JSONLanguageService.getLanguageService(o); }"
)
private fun iiJsonLanguageService(o: dynamic): dynamic = iiJsonLanguageServiceJs(o)

private val iiTextDocumentCreateJs: dynamic = js(
    "function (uri, lang, v, text) { return JSONLanguageService.TextDocument.create(uri, lang, v, text); }"
)
private fun iiTextDocumentCreate(uri: dynamic, lang: dynamic, v: dynamic, text: dynamic): dynamic =
    iiTextDocumentCreateJs(uri, lang, v, text)

private val iiFindNodeAtOffsetJs: dynamic = js(
    "function (root, off, b) { return JSONLanguageService.findNodeAtOffset(root, off, b); }"
)
private fun iiFindNodeAtOffset(root: dynamic, off: dynamic, b: dynamic): dynamic =
    iiFindNodeAtOffsetJs(root, off, b)

private val iiLogEJs: dynamic = js("function (m) { Log.e(m); }")
private fun iiLogE(m: dynamic) {
    iiLogEJs(m)
}

private val iiThrowNewErrorJs: dynamic = js("function (m) { throw new Error(m); }")
private fun iiThrowNewError(m: dynamic) {
    iiThrowNewErrorJs(m)
}

private val iiNewSpannableStringBuilderJs: dynamic = js(
    "function () { return new G.SpannableStringBuilder(); }"
)
private val iiNewSpannableStringBuilder1Js: dynamic = js(
    "function (s) { return new G.SpannableStringBuilder(s); }"
)
private val iiNewSpannableStringJs: dynamic = js(
    "function (s) { return new G.SpannableString(s); }"
)
private val iiNewForegroundColorSpanJs: dynamic = js(
    "function (c) { return new G.ForegroundColorSpan(c); }"
)
private val iiNewTranslate8Js: dynamic = js(
    "function (a, b, c, d, e, f, g, h) { return new G.TranslateAnimation(a, b, c, d, e, f, g, h); }"
)
private val iiNewTextViewJs: dynamic = js(
    "function () { return new G.TextView(ctx); }"
)
private val iiNewListViewJs: dynamic = js(
    "function () { return new G.ListView(ctx); }"
)
private val iiNewAbsParamsJs: dynamic = js(
    "function (w, h) { return new G.AbsListView.LayoutParams(w, h); }"
)
private val iiNewFrameParams2Js: dynamic = js(
    "function (w, h) { return new G.FrameLayout.LayoutParams(w, h); }"
)
private val iiNewRectJs: dynamic = js(
    "function () { return new G.Rect(); }"
)
private val iiNewItemClickListenerJs: dynamic = js(
    "function (f) { return new G.AdapterView.OnItemClickListener({ onItemClick: f }); }"
)
private val iiNewItemLongClickListenerJs: dynamic = js(
    "function (f) { return new G.AdapterView.OnItemLongClickListener({ onItemLongClick: f }); }"
)
private val iiNewLayoutChangeListenerJs: dynamic = js(
    "function (f) { return new G.View.OnLayoutChangeListener({ onLayoutChange: f }); }"
)
private val iiNewSimpleListAdapter5Js: dynamic = js(
    "function (a, b, c, d, e) { return new SimpleListAdapter(a, b, c, d, e); }"
)
private val iiSimpleGetControllerJs: dynamic = js(
    "function (a) { return SimpleListAdapter.getController(a); }"
)
private val iiToastMakeTextJs: dynamic = js(
    "function (c, t, d) { return G.Toast.makeText(c, t, d); }"
)
private val iiUiJs: dynamic = js("function (f) { return G.ui(f); }")
private val iiPostJs: dynamic = js("function (v, f) { return v.post(f); }")
private val iiNewUrlJs: dynamic = js("function (s) { return new java.net.URL(s); }")
private val iiOpenWikiJs: dynamic = js(
    "function (url) {" +
        // 拆成两步：Kotlin/JS 的 js() 生成器会把 new Intent(...).addFlags(...)
        // 重排成 new (Intent(...).addFlags)(...)，这里显式保留先构造再加 flag 的顺序。
        " var intent = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url));" +
        " intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);" +
        " AndroidBridge.startActivity(intent);" +
        "}"
)
private val iiShowWebViewDialogJs: dynamic = js(
    "function (url) { Common.showWebViewDialog({ url: url }); }"
)
private val iiDpJs: dynamic = js("function (n) { return n * G.dp; }")
private fun iiDp(n: dynamic): dynamic = iiDpJs(n)

/** 原 show 里的 `function self()`：状态挂在函数对象上，arity 0。 */
private val iiShowFactoryJs: dynamic = js(
    "function (impl) { var self = function self() { return impl(self); }; return self; }"
)

/** 原 callDelay 里的 `function self(s)`：pool 挂在函数对象上，arity 1。 */
private val iiCallDelayFactoryJs: dynamic = js(
    "function (impl) { var self = function self(s) { return impl(self, s); }; return self; }"
)

/** 原 getSchemaByContext 的 `this && this.library && ... || []` 短路语义。 */
private val iiStateListJs: dynamic = js(
    "function (api, type) { return api && api.library && api.library.states && api.library.states[type] || []; }"
)
private fun iiStateList(api: dynamic, type: dynamic): dynamic = iiStateListJs(api, type)

// ---------------- callDelay / apply / proc ----------------

private fun iiCallDelay(self: dynamic, s: dynamic): dynamic {
    val mode = js("CA").settings.iiMode
    if (iiLooseNe(mode, 2) && iiLooseNe(mode, 3)) return js("undefined")
    if (!iiTruthy(self.pool)) {
        self.pool = js("java.util.concurrent.Executors").newCachedThreadPool()
        self.pool.setMaximumPoolSize(1)
        self.pool.setRejectedExecutionHandler(
            js("new java.util.concurrent.ThreadPoolExecutor.DiscardPolicy()")
        )
    }
    self.pool.execute({ ->
        js("CA").IntelliSense.proc(s)
        js("undefined")
    })
    return js("undefined")
}

private fun iiApply(api: dynamic): dynamic {
    return if (iiTruthy(api.ui)) api.show.apply(api) else js("undefined")
}

private fun iiProc(api: dynamic, s: dynamic): dynamic {
    try {
        val mode = js("CA").settings.iiMode
        if ((iiLooseNe(mode, 2) && iiLooseNe(mode, 3)) || iiTruthy(js("CA").Library.loadingStatus)) {
            return js("undefined")
        }
        if (iiLooseEq(api.commandText, s)) return js("undefined")
        api.commandText = s
        val r = api.procCmd(s)
        api.source = r.source
        api.cmdname = r.cmdname
        api.hasSlash = r.hasSlash
        api.strParam = r.strParam
        api.input = r.input
        api.output = r.output
        api.help = r.help
        api.prompt = r.prompt
        api.patterns = r.patterns
        // 应用更改
        api.apply()
    } catch (e: dynamic) {
        js("erp")(e, true)
        js("Common").showTextDialog(
            iiAdd(
                iiAdd("当前命令库解析出错。\n", e),
                if (iiInstanceOf(e, js("Error"))) iiAdd("\n堆栈：\n", e.stack) else ""
            )
        )
    }
    return js("undefined")
}

private fun iiProcCmd(api: dynamic, s: dynamic): dynamic {
    var ca: dynamic
    var t: dynamic
    var pp: dynamic

    // 分析命令结构 - 拆分
    val c = iiExecCmdJs(s)
    // c = [匹配文本, 是否存在/, 命令名称, 是否存在命令名称后的空格, 命令参数]
    if (!iiTruthy(c)) return js("undefined")

    val r = iiObject()
    r.source = c[0]
    r.cmdname = c[2]
    r.hasSlash = iiTruthy(c[1])
    r.strParam = c[4]
    r.input = iiArray()
    r.output = iiObject()
    r.prompt = iiArray()
    r.patterns = iiArray()
    r.help = js("null")
    r.canFinish = false

    if (iiTruthy(c[3])) {
        // 分类 - 输入参数中
        if (iiIn(c[2], api.library.commands)) {
            // 分类 - 存在命令
            api.procParams(r)
        } else {
            // 分类 - 不存在命令 / 提示命令未找到
            pp = iiNewSpannableStringBuilder1Js(
                iiAdd(if (iiTruthy(c[1])) "/" else "", iiAdd(c[2], " "))
            )
            js("appendSSB")(pp, "...", iiNewForegroundColorSpanJs(js("Common").theme.highlightcolor))
            pp.append("\n")
            js("appendSSB")(
                pp,
                iiAdd(iiAdd("无法在库中找到命令“", c[2]), "”。"),
                iiNewForegroundColorSpanJs(js("Common").theme.criticalcolor)
            )
            r.prompt.push(pp)
            r.help = "找不到这样的命令"
            r.mode = api.UNKNOWN_COMMAND
        }
    } else {
        // 分类 - 未输入参数
        // 获得可选命令：优先 Java 缓存命令表；无 jar 时用 QuickJS worker；最后 JS
        t = api.library.command_snap
        ca = js("null")
        try {
            if (iiAccelDefined() && iiTruthy(iiAccelAvailable())) {
                ca = iiAccelMatchCommandNames(t, c[2])
            }
        } catch (e: dynamic) {
            ca = js("null")
        }
        if (iiLooseEq(ca, js("null"))) {
            try {
                if (iiQuickAccelDefined()) ca = iiQuickAccelMatchCommandNames(t, c[2])
            } catch (e: dynamic) {
                ca = js("null")
            }
        }
        if (iiLooseEq(ca, js("null"))) {
            val keys = iiKeys(t)
            val matched = iiArray()
            var ki = 0
            while (ki < iiLength(keys)) {
                val e = keys[ki]
                if (iiGe(iiIndexOf(e, c[2]), 0) || iiGe(iiIndexOf(t[e], c[2]), 0)) {
                    matched.push(e)
                }
                ki++
            }
            matched.sort()
            ca = matched
        }

        if (iiGt(iiLength(ca), 0)) {
            // 分类 - 可选命令长度大于0
            var cai = 0
            while (cai < iiLength(ca)) {
                val e = ca[cai]
                pp = iiNewSpannableStringBuilder1Js(if (iiTruthy(c[1])) "/" else "")
                js("appendSSB")(
                    pp,
                    e,
                    iiNewForegroundColorSpanJs(js("Common").theme.highlightcolor)
                )
                t = api.library.commands[e]
                while (iiTruthy(t.alias)) t = api.library.commands[t.alias]

                // 存在无参数用法
                if (!iiTruthy(t.noparams)) pp.append(" ...")
                if (iiTruthy(t.noparams) && iiLooseEq(c[2], e) && iiTruthy(t.noparams.description)) {
                    // 当命令全输入且存在无参数用法时
                    r.canFinish = true
                    pp.append("\n")
                    js("appendSSB")(
                        pp,
                        t.noparams.description,
                        iiNewForegroundColorSpanJs(js("Common").theme.promptcolor)
                    )
                } else if (iiIn("description", t)) {
                    // 存在提示则显示提示
                    pp.append("\n")
                    js("appendSSB")(
                        pp,
                        t.description,
                        iiNewForegroundColorSpanJs(js("Common").theme.promptcolor)
                    )
                }
                r.prompt.push(pp)
                r.output[if (iiTruthy(t.description)) iiAdd(iiAdd(e, " - "), t.description) else e] =
                    iiAdd(
                        iiAdd(if (iiTruthy(r.hasSlash)) "/" else "", e),
                        if (iiTruthy(t.noparams)) "" else " "
                    )
                cai++
            }

            t = api.library.commands[ca[0]]
            while (iiTruthy(t.alias)) t = api.library.commands[t.alias]
            r.help = if (iiTruthy(t.help)) t.help else "该命令帮助还未上线"
            r.mode = api.ONLY_COMMAND_NAME
        } else {
            // 分类 - 可选命令长度等于0（无可选命令）/ 提示命令不存在
            pp = iiNewSpannableStringBuilder1Js(if (iiTruthy(c[1])) "/" else "")
            js("appendSSB")(
                pp,
                c[2],
                iiNewForegroundColorSpanJs(js("Common").theme.highlightcolor)
            )
            pp.append(" ...\n")
            js("appendSSB")(
                pp,
                iiAdd(iiAdd("无法在库中找到命令“", c[2]), "”。"),
                iiNewForegroundColorSpanJs(js("Common").theme.criticalcolor)
            )
            r.prompt.push(pp)
            r.help = "命令不存在"
            r.mode = api.UNKNOWN_COMMAND
        }

        // 设置列表内容及反应
        r.input = iiKeys(r.output)
    }
    return r
}

// ---------------- procParams ----------------

private fun iiProcParams(api: dynamic, c: dynamic): dynamic {
    var cm = api.library.commands[c.cmdname]
    var ps: dynamic = js("null")
    var pa: dynamic = js("null")
    var ci: dynamic = 0
    var cp: dynamic = js("null")
    var t: dynamic = js("null")
    var f = true
    var u: dynamic = js("null")
    var pp: dynamic = js("null")
    var nn = false
    val cpl = iiArray()
    val erm = iiArray()
    var parsedParams: dynamic = iiArray()
    var paramStart: dynamic = 0

    // 别名处理
    while (iiTruthy(cm.alias)) cm = api.library.commands[cm.alias]

    c.help = if (iiTruthy(cm.help)) cm.help else "该命令帮助还未上线"
    ps = cm.patterns
    c.canFinish = false

    // 对每一种模式进行判断
    val patternKeys = iiForInKeys(ps)
    var patternIndex = 0
    while (patternIndex < iiLength(patternKeys)) {
        val i = patternKeys[patternIndex]
        pa = ps[i].params
        ci = 0
        parsedParams = iiArray()

        // 重置提示
        pp = iiNewSpannableStringBuilder1Js(
            iiAdd(if (iiTruthy(c.hasSlash)) "/" else "", c.cmdname)
        )
        cpl.length = 0

        val paLen = iiLength(pa)
        var j = 0
        while (j < paLen) {
            cp = pa[j]

            // 匹配参数
            paramStart = ci
            t = api.matchParam(cp, iiSliceFrom(c.strParam, ci), parsedParams)

            var matched = false
            if (iiTruthy(t) && iiGe(t.length, 0)) {
                // 原实现把 `ci += t.length` 和 `++ci` 写在 if 条件里；
                // 即使后面的空白检查失败，ci 也已经前进。这里按原顺序先更新 ci。
                val sliceStart = iiAdd(ci, t.length)
                ci = iiAdd(sliceStart, 1)
                matched = iiTestWs01Js(iiSlice(c.strParam, sliceStart, ci))
            }

            if (matched) {
                // 分类 - 匹配成功
                ci = iiAdd(ci, iiLeadingWsLength(iiSliceFrom(c.strParam, ci)))
                // 记下这个参数的值，给后续参数当上下文（方块状态要靠方块 id 找 schema）。
                // 原实现这里写的是未声明的 t2，会在非严格模式下落到全局对象上；
                // Kotlin 产物是严格模式，所以用 globalThis.t2 显式保留同样的全局读写。
                iiSetGlobalT2Js(
                    iiTrim(iiSlice(c.strParam, paramStart, iiAdd(paramStart, t.length)))
                )
                if (iiTruthy(iiGetGlobalT2Js())) parsedParams.push(iiGetGlobalT2Js())

                if (iiGt(ci, iiLength(c.strParam))) {
                    // 分类 - 到达末尾 / 处理提示与输入
                    u = iiAdd(
                        iiAdd(
                            iiAdd(if (iiTruthy(c.hasSlash)) "/" else "", c.cmdname),
                            " "
                        ),
                        iiSlice(c.strParam, 0, iiSub(iiSub(ci, t.length), 1))
                    )
                    val next = pa[j + 1]
                    if (iiTruthy(next) && !iiTruthy(next.optional)) {
                        val outKeys = iiForInKeys(t.output)
                        var oi = 0
                        while (oi < iiLength(outKeys)) {
                            val k = outKeys[oi]
                            t.output[k] = iiAdd(t.output[k], " ")
                            oi++
                        }
                    }
                    if (iiTruthy(t.length) && iiTruthy(t.canFinish) && iiTruthy(next)) nn = true

                    if (iiTruthy(t.input)) {
                        val inKeys = iiForInKeys(t.input)
                        var ini = 0
                        while (ini < iiLength(inKeys)) {
                            val k = inKeys[ini]
                            if (iiLt(iiIndexOf(c.input, t.input[k]), 0)) {
                                c.input.push(t.input[k])
                            }
                            ini++
                        }
                    }
                    if (iiTruthy(t.output)) {
                        val outKeys = iiForInKeys(t.output)
                        var oi = 0
                        while (oi < iiLength(outKeys)) {
                            val k = outKeys[oi]
                            if (!iiIn(k, c.output)) c.output[k] = iiAdd(u, t.output[k])
                            oi++
                        }
                    }
                    if (iiTruthy(t.recommend)) {
                        val recKeys = iiForInKeys(t.recommend)
                        var ri = 0
                        while (ri < iiLength(recKeys)) {
                            val k = recKeys[ri]
                            if (!iiIn(k, c.output)) c.output[k] = iiAdd(u, t.recommend[k])
                            ri++
                        }
                    }
                    if (iiTruthy(t.assist)) {
                        val asKeys = iiForInKeys(t.assist)
                        var ai = 0
                        while (ai < iiLength(asKeys)) {
                            val k = asKeys[ai]
                            if (!iiIn(k, c.output)) c.output[k] = iiAdd(c.source, t.assist[k])
                            ai++
                        }
                    }
                    if (iiTruthy(t.menu)) {
                        val menuKeys = iiForInKeys(t.menu)
                        var mi = 0
                        while (mi < iiLength(menuKeys)) {
                            val k = menuKeys[mi]
                            if (!iiIn(k, c.output)) c.output[k] = t.menu[k]
                            mi++
                        }
                    }
                    if (iiTruthy(t.canFinish) && (!iiTruthy(next) || iiTruthy(next.optional))) {
                        c.canFinish = true
                    }
                    f = false
                    pp.append(" ")
                    pp.append(
                        api.getParamTag(
                            cp,
                            iiSlice(c.strParam, iiSub(iiSub(ci, t.length), 1), ci),
                            1,
                            t
                        )
                    )
                    // 原实现的 `for (j++; j < pa.length; j++)`：先自增，再补后续未输入参数。
                    j++
                    while (j < paLen) {
                        pp.append(" ")
                        pp.append(api.getParamTag(pa[j], "", 2, js("null")))
                        j++
                    }
                    if (iiTruthy(t.description) || iiTruthy(cp.description) ||
                        iiTruthy(ps[i].description) || iiTruthy(cm.description)
                    ) {
                        var desc: dynamic = js("null")
                        if (iiTruthy(t.description)) desc = iiString(t.description)
                        else if (iiTruthy(cp.description)) desc = iiString(cp.description)
                        else if (iiTruthy(ps[i].description)) desc = iiString(ps[i].description)
                        else desc = iiString(cm.description)
                        js("appendSSB")(
                            pp,
                            iiAdd("\n", desc),
                            iiNewForegroundColorSpanJs(js("Common").theme.promptcolor)
                        )
                    }
                    // 详情优先级：匹配函数动态产生 > 当前参数 > 当前用法 > 当前命令 > 不显示

                    c.prompt.push(pp)
                    // 保持原实现：所有 pattern 复用同一个 cpl 数组引用。
                    c.patterns.push(cpl)
                    break
                } else {
                    // 分类 - 未到达末尾
                    if (!iiTruthy(t.canFinish)) {
                        if (iiTruthy(cp.canIgnore)) {
                            j++
                            continue
                        } else {
                            pp.append(" ")
                            pp.append(api.getParamTag(cp, "", 3, t))
                            var k = j + 1
                            while (k < paLen) {
                                pp.append(" ")
                                pp.append(api.getParamTag(pa[k], "", 2, js("null")))
                                k++
                            }
                            val e = iiObject()
                            e.desp = "未结束的参数"
                            e.count = j
                            e.pp = pp
                            erm.push(e)
                            break
                        }
                    }
                    pp.append(" ")
                    pp.append(
                        api.getParamTag(
                            cp,
                            iiSlice(c.strParam, iiSub(iiSub(ci, t.length), 1), iiSub(ci, 1)),
                            0
                        )
                    )
                    cpl.push(t)
                }
            } else {
                // 分类 - 匹配失败
                if (iiTruthy(cp.canIgnore)) {
                    // 忽略参数
                    j++
                    continue
                } else {
                    pp.append(" ")
                    pp.append(api.getParamTag(cp, "", 3, t))
                    var k = j + 1
                    while (k < paLen) {
                        pp.append(" ")
                        pp.append(api.getParamTag(pa[k], "", 2, js("null")))
                        k++
                    }
                    val e = iiObject()
                    if (!iiTruthy(t)) {
                        e.desp = js("null")
                    } else if (iiGe(t.length, 0)) {
                        e.desp = iiAdd("字符多余：", iiSliceFrom(c.strParam, iiSub(ci, 1)))
                    } else {
                        e.desp = t.description
                    }
                    e.count = j
                    e.pp = pp
                    erm.push(e)
                    break
                    // 下一个模式
                }
            }
            if (iiTruthy(cp.repeat)) {
                // 原实现是 `j--; continue;`，for 的更新表达式随后再 j++，
                // 所以下一轮仍是同一个 j；while 里等价于先 j-- 再落到下面的 j++。
                j--
            }
            j++
        }
        patternIndex++
    }

    // 如果未找到正确用法
    if (f) {
        c.input = iiArray()
        erm.sort(iiSortCountDescJs)
        var ei = 0
        while (ei < iiLength(erm)) {
            val e = erm[ei]
            e.pp.append("\n")
            js("appendSSB")(
                e.pp,
                if (iiTruthy(e.desp)) e.desp else "用法不存在",
                iiNewForegroundColorSpanJs(js("Common").theme.criticalcolor)
            )
            c.prompt.push(e.pp)
            ei++
        }
        if (iiStrictEq(iiLength(erm), 0)) {
            pp = iiNewSpannableStringBuilder1Js(
                iiAdd(if (iiTruthy(c.hasSlash)) "/" else "", iiAdd(c.cmdname, " "))
            )
            js("appendSSB")(
                pp,
                "...",
                iiNewForegroundColorSpanJs(js("Common").theme.highlightcolor)
            )
            pp.append("\n")
            js("appendSSB")(
                pp,
                iiAdd(iiAdd("无法在库中找到命令“", c.cmdname), "”的此类用法。"),
                iiNewForegroundColorSpanJs(js("Common").theme.criticalcolor)
            )
            c.prompt.push(pp)
        }
    } else if (nn) {
        c.input.push("  - 下一个参数")
        c.output["  - 下一个参数"] = iiAdd(c.source, " ")
    }
    c.mode = if (f) api.UNKNOWN_PATTERN else api.COMMAND_WITH_PATTERN
    return js("undefined")
}

// ---------------- matchParam ----------------

private fun iiMatchParam(api: dynamic, cp: dynamic, ps: dynamic, parsedParams: dynamic): dynamic {
    var r: dynamic
    var t: dynamic
    var t2: dynamic
    var t3: dynamic
    val type = cp.type

    if (iiStrictEq(type, "text") || iiStrictEq(type, "rawjson")) {
        r = iiObject()
        r.length = iiLength(ps)
        r.canFinish = true
    } else if (iiStrictEq(type, "nbt") || iiStrictEq(type, "json")) {
        r = iiObject()
        r.input = iiArrayOf("插入JSON")
        val menu = iiObject()
        menu["插入JSON"] = { ->
            js("CA").IntelliSense.assistJSON(cp)
            js("undefined")
        }
        r.menu = menu
        r.length = iiLength(ps)
        r.canFinish = true
    } else if (iiStrictEq(type, "jsonPro")) {
        r = iiMatchParamJsonPro(api, cp, ps)
    } else if (iiStrictEq(type, "status")) {
        // 方块状态补全（param 形如 {"type":"status","subject":"block"}）
        r = iiMatchParamStatus(api, cp, ps, parsedParams)
    } else if (iiStrictEq(type, "plain")) {
        t = cp.name
        if (iiTruthy(cp.prompt)) t = iiAdd(iiAdd(t, " - "), cp.prompt)
        r = iiObject()
        r.input = iiArrayOf(t)
        r.output = iiObject()
        r.output[t] = cp.name
        if (iiStartsWith(ps, iiAdd(cp.name, " ")) || iiLooseEq(ps, cp.name)) {
            r.length = iiLength(cp.name)
            r.canFinish = true
        } else if (iiGe(iiIndexOf(cp.name, ps), 0) ||
            (iiTruthy(cp.prompt) && iiGe(iiIndexOf(cp.prompt, ps), 0))
        ) {
            r.length = iiLength(ps)
            r.canFinish = false
        } else {
            return iiDescription(iiAdd("不可为", ps))
        }
    } else if (iiStrictEq(type, "selector")) {
        r = api.procSelector(cp, ps)
        if (!iiTruthy(r) || !iiGe(r.length, 0)) return r
    } else if (iiStrictEq(type, "uint")) {
        t = iiFirstToken(ps)
        if (!iiTestUintJs(t)) return iiDescription(iiAdd(t, "不是自然数"))
        r = iiObject()
        r.length = iiLength(t)
        r.canFinish = iiGt(iiLength(t), 0)
    } else if (iiStrictEq(type, "int")) {
        t = iiFirstToken(ps)
        if (!iiTestIntJs(t)) return iiDescription(iiAdd(t, "不是整数"))
        r = iiObject()
        r.length = iiLength(t)
        // 原实现是 `t.length && !isNaN(t)`，0 会原样成为 0 而不是 false。
        r.canFinish = iiAnd(iiLength(t), iiNot(iiIsNaN(t)))
    } else if (iiStrictEq(type, "float")) {
        t = iiFirstToken(ps)
        t2 = iiExecFloatJs(t)
        if (!iiTruthy(t2)) return iiDescription(iiAdd(t, "不是数值"))
        r = iiObject()
        r.length = iiLength(t)
        // 原实现是 `t.length && t2[3]`，值可能是字符串。
        r.canFinish = iiAnd(iiLength(t), t2[3])
    } else if (iiStrictEq(type, "relative")) {
        t = iiFirstToken(ps)
        t2 = iiExecRelativeJs(t)
        if (!iiTruthy(t2)) return iiDescription(iiAdd(t, "不是数值"))
        r = iiObject()
        r.length = iiLength(t)
        r.input = iiArrayOf("~ - 相对标识符")
        r.assist = iiObject()
        r.assist["~ - 相对标识符"] = "~"
        // 原实现是 `t2[5] || t2[1] && !t2[2].length`。
        r.canFinish = iiOr(t2[5], iiAnd(t2[1], iiNot(iiLength(t2[2]))))
    } else if (iiStrictEq(type, "position")) {
        r = api.procPosition(cp, ps)
        if (!iiTruthy(r) || !iiGe(r.length, 0)) return r
    } else if (iiStrictEq(type, "custom")) {
        t = iiNewRegExpExec(cp.input, ps)
        if (!iiTruthy(t)) return iiDescription(iiAdd(t, "不满足指定的条件"))
        r = iiObject()
        r.length = iiLength(t[0])
        r.canFinish = iiRegExpTest(cp.finish, ps)
    } else if (iiStrictEq(type, "enum")) {
        r = iiMatchParamEnum(api, cp, ps)
    } else if (iiStrictEq(type, "command")) {
        r = iiMatchParamCommand(api, ps)
    } else if (iiStrictEq(type, "subcommand")) {
        r = iiMatchParamSubcommand(api, cp, ps)
    } else if (iiStrictEq(type, "string")) {
        t = iiFirstToken(ps)
        r = iiObject()
        r.length = iiLength(t)
        r.canFinish = iiGt(iiLength(t), 0)
    } else {
        t = iiFirstToken(ps)
        r = iiObject()
        r.length = iiLength(t)
        r.canFinish = true
    }

    if (!iiTruthy(cp.suggestion)) return r
    t = if (iiInstanceOf(cp.suggestion, js("Object"))) {
        cp.suggestion
    } else {
        api.library.enums[cp.suggestion]
    }
    t2 = iiSlice(ps, 0, r.length)
    iiMatchString(t2, t, r)
    return r
}

// ---------------- matchParam: jsonPro ----------------

private fun iiMatchParamJsonPro(api: dynamic, cp: dynamic, ps: dynamic): dynamic {
    val schema = api.library.jsonSchema[cp.schema]
    if (!iiTruthy(schema)) {
        val r = iiObject()
        r.length = iiLength(ps)
        r.canFinish = false
        r.description = iiAdd("找不到Schema: ", cp.schema)
        return r
    }

    // 获取或创建缓存的 Language Service
    var ls = api.jsonLanguageServices[cp.schema]
    if (!iiTruthy(ls)) {
        val options = iiObject()
        options.schemaRequestService = { _uri: dynamic -> js("null") }
        ls = iiJsonLanguageService(options)
        val schemaEntry = iiObject()
        schemaEntry.uri = iiAdd(iiAdd("ca://", cp.schema), ".json")
        schemaEntry.fileMatch = iiArrayOf("ca://json.json")
        schemaEntry.schema = schema
        val config = iiObject()
        config.schemas = iiArrayOf(schemaEntry)
        ls.configure(config)
        api.jsonLanguageServices[cp.schema] = ls
    }

    // 计算光标位置
    var cursorOffset: dynamic =
        if (!iiIsUndefined(cp._cursorOffset)) cp._cursorOffset else iiLength(ps)
    if (iiLt(cursorOffset, 0)) cursorOffset = 0
    if (iiGt(cursorOffset, iiLength(ps))) cursorOffset = iiLength(ps)

    // 创建 TextDocument 并获取补全
    val doc = iiTextDocumentCreate("ca://json.json", "json", 1, ps)
    val jsonDoc = ls.parseJSONDocument(doc)
    var completions: dynamic = js("null")
    try {
        val position = iiObject()
        position.line = 0
        position.character = cursorOffset
        val completionResult = ls.doComplete(doc, position, jsonDoc)
        completions = if (iiTruthy(completionResult) && iiTruthy(completionResult.value)) {
            completionResult.value
        } else {
            completionResult
        }
    } catch (e: dynamic) {
        iiLogE(iiAdd("doComplete 错误: ", e))
    }

    // 如果光标在 score.name 字段，添加选择器补全
    var selectorNode: dynamic = js("null")
    if (iiTruthy(jsonDoc.root) && iiGe(cursorOffset, 0) && iiLe(cursorOffset, iiLength(ps))) {
        selectorNode = iiFindNodeAtOffset(jsonDoc.root, cursorOffset, true)
    }
    if (iiTruthy(selectorNode) && iiTruthy(selectorNode.parent) &&
        iiStrictEq(selectorNode.parent.type, "property") &&
        iiTruthy(selectorNode.parent.keyNode) &&
        iiStrictEq(selectorNode.parent.keyNode.value, "name")
    ) {
        val prop = selectorNode.parent
        val scoreObj = prop.parent
        if (iiTruthy(scoreObj) && iiStrictEq(scoreObj.type, "object") &&
            iiTruthy(scoreObj.parent) && iiStrictEq(scoreObj.parent.type, "property") &&
            iiTruthy(scoreObj.parent.keyNode) &&
            iiStrictEq(scoreObj.parent.keyNode.value, "score")
        ) {
            val valueNode = iiOr(prop.valueNode, selectorNode)
            val valueStart = valueNode.offset
            val valueEnd = iiAdd(valueNode.offset, valueNode.length)
            var nameValue = iiSubstring(ps, valueStart, valueEnd)
            if (iiGe(iiLength(nameValue), 2) &&
                iiStrictEq(iiCharAt(nameValue, 0), "\"") &&
                iiStrictEq(iiCharAt(nameValue, iiSub(iiLength(nameValue), 1)), "\"")
            ) {
                nameValue = iiSubstring(nameValue, 1, iiSub(iiLength(nameValue), 1))
            }
            val selResult = api.procSelector(iiObject(), nameValue)
            if (iiTruthy(selResult) && iiTruthy(selResult.recommend)) {
                if (!iiTruthy(completions)) {
                    completions = iiObject()
                    completions.items = iiArray()
                }
                if (!iiTruthy(completions.items)) completions.items = iiArray()
                val selKeys = iiForInKeys(selResult.recommend)
                var si = 0
                while (si < iiLength(selKeys)) {
                    val selKey = selKeys[si]
                    val selVal = selResult.recommend[selKey]
                    val escapedVal = iiEscapeSelectorValueJs(selVal)
                    val newText = iiAdd(iiAdd("\"", escapedVal), "\"")
                    var selCursor = iiAdd(iiAdd(valueStart, 1), iiLength(escapedVal))
                    val eqPos = iiIndexOf(escapedVal, "=")
                    if (iiGe(eqPos, 0)) {
                        selCursor = iiAdd(iiAdd(iiAdd(valueStart, 1), eqPos), 1)
                    }
                    val item = iiObject()
                    item.label = selKey
                    item.insertText = newText
                    val range = iiObject()
                    range.start = iiObject()
                    range.start.line = 0
                    range.start.character = valueStart
                    range.end = iiObject()
                    range.end.line = 0
                    range.end.character = valueEnd
                    item.textEdit = iiObject()
                    item.textEdit.range = range
                    item.textEdit.newText = newText
                    item._selectorCursor = selCursor
                    completions.items.push(item)
                    si++
                }
            }
        }
    }

    // 通用 ca:parser 处理：根据 Schema 自定义属性调用项目解析器
    try {
        val matchingSchemas = jsonDoc.getMatchingSchemas(schema, cursorOffset)
        var parserType: dynamic = js("null")
        var parserNode: dynamic = js("null")
        var mi = 0
        while (mi < iiLength(matchingSchemas)) {
            val ms = matchingSchemas[mi]
            if (iiTruthy(ms.schema) && iiTruthy(ms.schema["ca:parser"])) {
                parserType = ms.schema["ca:parser"]
                parserNode = ms.node
                break
            }
            mi++
        }
        if (iiTruthy(parserType)) {
            val parserItems = api.getParserCompletions(parserType, ps, cursorOffset, parserNode)
            if (iiTruthy(parserItems) && iiGt(iiLength(parserItems), 0)) {
                if (!iiTruthy(completions)) {
                    completions = iiObject()
                    completions.items = iiArray()
                }
                if (!iiTruthy(completions.items)) completions.items = iiArray()
                var pi = 0
                while (pi < iiLength(parserItems)) {
                    completions.items.push(parserItems[pi])
                    pi++
                }
            }
        }
    } catch (e: dynamic) {
        // 解析器补全失败不影响主流程
    }

    // 转换结果
    val r = iiObject()
    r.length = iiLength(ps)
    r.canFinish = false
    r.input = iiArray()
    r.output = iiObject()
    r.prompt = iiArray()
    val completionLabels = iiArray()

    if (iiTruthy(completions) && iiTruthy(completions.items)) {
        val items = completions.items
        var itemIndex = 0
        while (itemIndex < iiLength(items)) {
            val item = items[itemIndex]
            val label = item.label
            var newText = iiOr(item.insertText, label)
            var start: dynamic = cursorOffset
            var end: dynamic = cursorOffset

            if (iiTruthy(item.textEdit)) {
                newText = item.textEdit.newText
                start = item.textEdit.range.start.character
                end = item.textEdit.range.end.character
            }

            // 计算光标位置（基于 Snippet 占位符 $1）
            val snippetText = if (iiTruthy(item.textEdit)) {
                item.textEdit.newText
            } else {
                item.insertText
            }
            var cursorInParam: dynamic = start
            if (!iiIsUndefined(item._selectorCursor)) {
                cursorInParam = item._selectorCursor
            } else {
                val snippetPos = if (iiTruthy(snippetText)) iiIndexOf(snippetText, "$1") else -1
                if (iiTruthy(snippetText) && iiGe(snippetPos, 0)) {
                    cursorInParam = iiAdd(start, snippetPos)
                } else {
                    cursorInParam = iiAdd(start, iiLength(newText))
                }
            }

            // 去掉 Snippet 占位符
            newText = iiRemoveSnippetJs(newText)

            // 记录是否是对象/数组插入（必须在逗号补全之前记录）
            val isObjectOrArray = iiGt(iiLength(newText), 0) &&
                (iiStrictEq(iiCharAt(newText, 0), "{") || iiStrictEq(iiCharAt(newText, 0), "["))

            // 自动补全逗号（当插入对象/数组且前面是 } 或 ] 时）
            if (isObjectOrArray && iiGt(start, 0)) {
                val prevChar = iiCharAt(ps, iiSub(start, 1))
                if (iiStrictEq(prevChar, "}") || iiStrictEq(prevChar, "]")) {
                    newText = iiAdd(",", newText)
                    cursorInParam = iiAdd(cursorInParam, 1)
                }
            }

            val replacedParam = iiAdd(
                iiAdd(iiSubstring(ps, 0, start), newText),
                iiSubstringFrom(ps, end)
            )

            // 获取字段说明（来自 Schema 的 description）
            var displayLabel: dynamic = label
            var desc: dynamic = item.detail

            // 补全项 detail 中文映射（detail 才是 "New object"/"New array"）
            if (iiStrictEq(desc, "New object")) desc = "新建对象"
            else if (iiStrictEq(desc, "New array")) desc = "新建数组"
            if (!iiTruthy(desc) && iiTruthy(item.documentation)) {
                if (iiStrictEq(iiTypeof(item.documentation), "string")) {
                    desc = item.documentation
                } else if (iiTruthy(item.documentation.value)) {
                    desc = item.documentation.value
                }
            }
            if (iiTruthy(desc)) {
                displayLabel = iiAdd(iiAdd(label, " - "), iiSplit(desc, "\n")[0])
            }

            // 模拟检测：插入后在光标位置是否还有补全选项
            // 如果插入的是对象/数组，但光标位置不合法，则过滤掉
            var isValidCompletion = true
            if (isObjectOrArray) {
                val simDoc = iiTextDocumentCreate("ca://json.json", "json", 1, replacedParam)
                val simJsonDoc = ls.parseJSONDocument(simDoc)
                // 如果光标在根节点范围外部，说明这个位置不合法
                if (iiTruthy(simJsonDoc.root) &&
                    iiGt(cursorInParam, iiAdd(simJsonDoc.root.offset, simJsonDoc.root.length))
                ) {
                    isValidCompletion = false
                } else {
                    val simPos = iiObject()
                    simPos.line = 0
                    simPos.character = cursorInParam
                    val simResult = ls.doComplete(simDoc, simPos, simJsonDoc)
                    val simCompletions = if (iiTruthy(simResult) && iiTruthy(simResult.value)) {
                        simResult.value
                    } else {
                        simResult
                    }
                    if (!iiTruthy(simCompletions) || !iiTruthy(simCompletions.items) ||
                        iiStrictEq(iiLength(simCompletions.items), 0)
                    ) {
                        isValidCompletion = false
                    }
                }
            }

            if (isValidCompletion) {
                completionLabels.push(displayLabel)
                val output = iiObject()
                output.text = replacedParam
                output.cursor = cursorInParam
                r.output[displayLabel] = output
            }
            itemIndex++
        }
    }

    // 引号/括号扫描 + 闭合检测 + 字段导航（合并为一次扫描）
    val unclosed = iiArray()
    val stringPositions = iiArray()
    val navLabels = iiArray()
    var inQuote = false
    var escapeNext = false
    var idx = 0
    while (idx < iiLength(ps)) {
        val ch = iiCharAt(ps, idx)
        if (escapeNext) {
            escapeNext = false
            idx++
            continue
        }
        if (iiStrictEq(ch, "\\")) {
            escapeNext = true
            idx++
            continue
        }
        if (iiStrictEq(ch, "\"")) {
            if (!inQuote) {
                inQuote = true
                unclosed.push("quote")
                stringPositions.push(idx + 1) // 字符串内容起始位置（引号后）
            } else {
                inQuote = false
                unclosed.pop()
            }
            idx++
            continue
        }
        if (!inQuote) {
            if (iiStrictEq(ch, "{")) {
                unclosed.push("brace")
            } else if (iiStrictEq(ch, "}")) {
                var j = iiLength(unclosed) - 1
                while (j >= 0) {
                    if (iiStrictEq(unclosed[j], "brace")) {
                        unclosed.splice(j, 1)
                        break
                    }
                    j--
                }
            } else if (iiStrictEq(ch, "[")) {
                unclosed.push("bracket")
            } else if (iiStrictEq(ch, "]")) {
                var j2 = iiLength(unclosed) - 1
                while (j2 >= 0) {
                    if (iiStrictEq(unclosed[j2], "bracket")) {
                        unclosed.splice(j2, 1)
                        break
                    }
                    j2--
                }
            }
        }
        idx++
    }

    // 闭合提示（一次只闭合最内层的一个）
    val closeLabels = iiArray()
    val lastUnclosed: dynamic =
        if (iiGt(iiLength(unclosed), 0)) unclosed[iiSub(iiLength(unclosed), 1)] else js("null")
    if (iiStrictEq(lastUnclosed, "quote")) {
        closeLabels.push("\" - 闭合引号")
        r.output["\" - 闭合引号"] = iiObject()
        r.output["\" - 闭合引号"].text = iiAdd(ps, "\"")
        r.output["\" - 闭合引号"].cursor = iiAdd(iiLength(ps), 1)
    } else if (iiStrictEq(lastUnclosed, "brace")) {
        closeLabels.push("} - 闭合对象")
        r.output["} - 闭合对象"] = iiObject()
        r.output["} - 闭合对象"].text = iiAdd(ps, "}")
        r.output["} - 闭合对象"].cursor = iiAdd(iiLength(ps), 1)
    } else if (iiStrictEq(lastUnclosed, "bracket")) {
        closeLabels.push("] - 闭合数组")
        r.output["] - 闭合数组"] = iiObject()
        r.output["] - 闭合数组"].text = iiAdd(ps, "]")
        r.output["] - 闭合数组"].cursor = iiAdd(iiLength(ps), 1)
    }
    if (iiGt(iiLength(stringPositions), 0)) {
        var prevPos: dynamic = -1
        var nextPos: dynamic = -1
        var si = 0
        while (si < iiLength(stringPositions)) {
            if (iiLt(stringPositions[si], cursorOffset)) prevPos = stringPositions[si]
            if (iiGt(stringPositions[si], cursorOffset) && iiLt(nextPos, 0)) {
                nextPos = stringPositions[si]
            }
            si++
        }
        // 边界回退（循环）
        if (iiLt(prevPos, 0)) {
            prevPos = stringPositions[iiSub(iiLength(stringPositions), 1)]
        }
        if (iiLt(nextPos, 0)) nextPos = stringPositions[0]

        navLabels.push("◀ 上一个字段")
        r.output["◀ 上一个字段"] = iiObject()
        r.output["◀ 上一个字段"].text = ps
        r.output["◀ 上一个字段"].cursor = prevPos
        navLabels.push("▶ 下一个字段")
        r.output["▶ 下一个字段"] = iiObject()
        r.output["▶ 下一个字段"].text = ps
        r.output["▶ 下一个字段"].cursor = nextPos
    }

    // JSON 报错检测
    val allErrors = iiArray()
    // 语法错误（jsonc-parser）
    if (iiTruthy(jsonDoc.errors) && iiGt(iiLength(jsonDoc.errors), 0)) {
        val errors = jsonDoc.errors
        var ei = 0
        while (ei < iiLength(errors)) {
            val err = errors[ei]
            var errMsg: dynamic = "JSON语法错误"
            val code = err.error
            if (iiStrictEq(code, 1)) errMsg = "非法符号"
            else if (iiStrictEq(code, 3)) errMsg = "缺少属性名"
            else if (iiStrictEq(code, 4)) errMsg = "缺少值"
            else if (iiStrictEq(code, 5)) errMsg = "缺少冒号"
            else if (iiStrictEq(code, 6)) errMsg = "缺少逗号"
            else if (iiStrictEq(code, 7)) errMsg = "缺少右大括号"
            else if (iiStrictEq(code, 8)) errMsg = "缺少右中括号"
            else if (iiStrictEq(code, 12)) errMsg = "字符串未闭合"
            val allError = iiObject()
            allError.offset = err.offset
            allError.message = errMsg
            allErrors.push(allError)
            ei++
        }
    }
    // Schema 验证错误
    val validationResult = ls.doValidation(doc, jsonDoc)
    val diagnostics = if (iiTruthy(validationResult) && iiTruthy(validationResult.value)) {
        validationResult.value
    } else {
        validationResult
    }
    if (iiTruthy(diagnostics) && iiGt(iiLength(diagnostics), 0)) {
        var di = 0
        while (di < iiLength(diagnostics)) {
            val d = diagnostics[di]
            if (iiStrictEq(d.severity, 1)) { // Error
                var errMsg: dynamic = d.message
                // 这里的错误信息比较长且不友好，做个映射转换一下
                if (iiStrictEq(errMsg, "Expected a JSON object, array or literal.")) errMsg = "需要 对象、数组、字面量"
                else if (iiStrictEq(errMsg, "End of file expected.")) errMsg = "需要文件结束符"
                else if (iiStrictEq(errMsg, "Value expected")) errMsg = "需要值"
                else if (iiStrictEq(errMsg, "Property expected")) errMsg = "需要属性名"
                else if (iiStrictEq(errMsg, "Expected comma")) errMsg = "需要 ,"
                else if (iiStrictEq(errMsg, "Expected comma or closing bracket")) errMsg = "需要 , 或 ]"
                else if (iiStrictEq(errMsg, "Expected comma or closing brace")) errMsg = "需要 , 或 }"
                else if (iiStrictEq(errMsg, "Colon expected")) errMsg = "需要 :"
                else if (iiStrictEq(errMsg, "Trailing comma")) errMsg = "多余的 ,"
                else if (iiStrictEq(errMsg, "Property keys must be doublequoted")) errMsg = "属性名必须用\" \"包裹"
                else if (iiStrictEq(errMsg, "Invalid number format.")) errMsg = "数字格式无效"
                else if (iiStrictEq(errMsg, "Invalid unicode sequence in string.")) errMsg = "字符串中的 Unicode 序列无效"
                else if (iiStrictEq(errMsg, "Invalid escape character in string.")) errMsg = "字符串中的转义字符无效"
                else if (iiStrictEq(errMsg, "Unexpected end of string.")) errMsg = "字符串未闭合"
                else if (iiStrictEq(errMsg, "Unexpected end of comment.")) errMsg = "注释未闭合"
                else if (iiStrictEq(errMsg, "Unexpected end of number.")) errMsg = "数字未结束"
                else if (iiStrictEq(errMsg, "Invalid characters in string. Control characters must be escaped.")) errMsg = "字符串中包含非法字符"
                else if (iiStrictEq(errMsg, "Duplicate object key")) errMsg = "重复的对象键"
                val allError = iiObject()
                allError.offset = d.range.start.character
                allError.message = errMsg
                allErrors.push(allError)
            }
            di++
        }
    }
    var errorNavLabel: dynamic = js("null")
    if (iiGt(iiLength(allErrors), 0)) {
        allErrors.sort(iiSortOffsetAscJs)
        val firstError = allErrors[0]
        val pp = iiNewSpannableStringBuilderJs()
        js("appendSSB")(
            pp,
            iiAdd(
                iiAdd(
                    iiAdd("JSON错误：", firstError.message),
                    iiAdd(" (位置: ", firstError.offset)
                ),
                ")"
            ),
            iiNewForegroundColorSpanJs(js("Common").theme.criticalcolor)
        )
        r.prompt.push(pp)
        errorNavLabel = "跳到报错位置"
        r.output[errorNavLabel] = iiObject()
        r.output[errorNavLabel].text = ps
        r.output[errorNavLabel].cursor = firstError.offset
    }

    // 组装列表顺序：导航 → 补全 → 闭合 → 报错跳转
    r.input = iiConcat3(navLabels, completionLabels, closeLabels)
    if (iiTruthy(errorNavLabel)) r.input.push(errorNavLabel)

    // CA 的 output 约定是「字符串」：proc 里会做 u + output[k]。
    // 社区版这里给的是 { text, cursor } 对象，直接拼会变成 "[object Object]"，
    // 所以统一降级成字符串（光标只能落在末尾，暂时放弃精确跳转）。
    val outputKeys = iiKeys(r.output)
    var oi = 0
    while (oi < iiLength(outputKeys)) {
        val k = outputKeys[oi]
        val v = r.output[k]
        if (iiTruthy(v) && iiStrictEq(iiTypeof(v), "object") &&
            iiStrictEq(iiTypeof(v.text), "string")
        ) {
            r.output[k] = v.text
        }
        oi++
    }
    return r
}

// ---------------- matchParam: status ----------------

private fun iiMatchParamStatus(
    api: dynamic,
    cp: dynamic,
    ps: dynamic,
    parsedParams: dynamic
): dynamic {
    var selResult: dynamic
    var contextValue: dynamic
    var curInput: dynamic
    var stateSchema: dynamic
    var pathArr: dynamic
    var selKeys: dynamic
    var subParam: dynamic
    var subRec: dynamic
    var prefix: dynamic

    val recommend = iiObject()
    var recommendEnums: dynamic = iiObject()

    val r = iiObject()
    r.length = iiLength(ps)
    r.canFinish = false
    r.recommend = iiObject()
    r.input = iiArray()

    if (!iiStrictEq(iiCharAt(ps, 0), "[")) {
        // 没在写方块状态
        r.length = 0
        r.canFinish = true
        return r
    }

    selResult = api.parseSelectorPath(ps)
    if (iiLooseEq(selResult.state, "complete")) {
        r.length = selResult.debug.selectorEnd
        r.canFinish = true
        return r
    }

    // 上下文取上一个参数的原始值（比如 setblock 的方块 id）
    contextValue = if (iiTruthy(parsedParams) && iiGt(iiLength(parsedParams), 0)) {
        parsedParams[iiSub(iiLength(parsedParams), 1)]
    } else {
        ""
    }
    curInput = iiOr(selResult.currentInput, "")
    stateSchema = api.getSchemaByContext(contextValue, cp.subject)
    if (!iiTruthy(stateSchema)) {
        stateSchema = iiObject()
        r.description = "无法找到该命令的状态参数信息"
    }
    pathArr = if (iiTruthy(selResult.stack)) selResult.stack.slice() else iiArray()
    selKeys = selResult.keys

    if (iiLooseEq(selResult.state, "wait_key")) {
        recommendEnums = api.getSelectorParamCompletions(
            curInput,
            ps,
            api.removeKeysFromSchema(stateSchema, selKeys),
            "="
        )
    } else if (iiLooseEq(selResult.state, "wait_value")) {
        if (iiGe(iiLength(curInput), 1)) {
            recommend[", - 下一个参数"] = iiAdd(ps, ",")
            recommend["] - 结束参数"] = iiAdd(ps, "] ")
        }
        val stateKey = pathArr[iiSub(iiLength(pathArr), 2)]
        if (iiTruthy(stateKey) && iiTruthy(stateSchema[stateKey])) {
            subParam = api.matchParam(stateSchema[stateKey], curInput, iiArray())
            subRec = iiOr(
                if (iiTruthy(subParam)) iiOr(subParam.output, subParam.recommend) else js("null"),
                iiObject()
            )
            prefix = iiSlice(ps, 0, iiAdd(iiLastIndexOf(ps, "="), 1))
            val recKeys = iiForInKeys(subRec)
            var ri = 0
            while (ri < iiLength(recKeys)) {
                val rk = recKeys[ri]
                if (iiHasOwn(subRec, rk)) recommendEnums[rk] = iiAdd(prefix, subRec[rk])
                ri++
            }
        }
    }

    val enumKeys = iiForInKeys(recommendEnums)
    var ei = 0
    while (ei < iiLength(enumKeys)) {
        val ek = enumKeys[ei]
        if (iiHasOwn(recommendEnums, ek)) recommend[ek] = recommendEnums[ek]
        ei++
    }
    r.recommend = recommend
    r.input = iiKeys(recommend)
    return r
}

// ---------------- matchParam: enum ----------------

private fun iiMatchParamEnum(api: dynamic, cp: dynamic, ps: dynamic): dynamic {
    val t: dynamic = if (iiInstanceOf(cp.list, js("Object"))) {
        cp.list
    } else {
        api.library.enums[cp.list]
    }
    if (!iiTruthy(t)) iiThrowNewError("无法找到指定枚举类型")

    val r = iiObject()
    r.output = iiObject()
    r.canFinish = false
    r.length = -1

    if (iiIsArray(t)) {
        val ac = iiAccelClassifyArray(t, ps)
        val flags = ac.flags
        r.length = ac.length
        r.canFinish = ac.canFinish
        r.input = iiArray()
        var i = 0
        while (i < iiLength(flags)) {
            if (!iiTruthy(flags[i])) {
                i++
                continue
            }
            val e = t[i]
            r.output[e] = e
            r.input.push(e)
            i++
        }
        r.input.sort()
    } else {
        val t2 = iiArray()
        r.input = iiArray()
        val cache = api.enumClassifyCache
        val keys: dynamic
        val descs: dynamic
        if (iiTruthy(cache) && iiStrictEq(cache.src, t)) {
            keys = cache.keys
            descs = cache.descs
        } else {
            keys = iiKeys(t)
            descs = iiArray()
            var i = 0
            while (i < iiLength(keys)) {
                descs.push(t[keys[i]])
                i++
            }
            val newCache = iiObject()
            newCache.src = t
            newCache.keys = keys
            newCache.descs = descs
            api.enumClassifyCache = newCache
        }
        val flags = iiAccelClassify(keys, descs, ps)
        var i = 0
        while (i < iiLength(keys)) {
            val f = flags[i]
            if (iiStrictEq(f, 0)) {
                i++
                continue
            }
            val e = keys[i]
            if (iiStrictEq(f, 1)) {
                r.length = iiMax(r.length, iiLength(e))
                r.canFinish = true
            } else {
                r.length = iiMax(r.length, iiLength(ps))
            }
            val d = t[e]
            if (iiTruthy(d)) {
                r.output[iiAdd(iiAdd(e, " - "), d)] = e
                r.input.push(iiAdd(iiAdd(e, " - "), d))
            } else {
                r.output[e] = e
                t2.push(e)
            }
            i++
        }
        r.input.sort()
        t2.sort()
        r.input = iiConcat2(r.input, t2)
    }
    if (iiLt(r.length, 0)) {
        r.description = iiAdd(ps, "不是有效的元素")
    }
    return r
}

// ---------------- matchParam: command / subcommand ----------------

private fun iiMatchParamCommand(api: dynamic, ps: dynamic): dynamic {
    val t = api.procCmd(ps)
    if (!iiTruthy(t)) return iiDescription("不是合法的命令格式")
    val t2 = t.prompt[0]
    val t3 = iiIndexOf(t2.toString(), "\n")

    val r = iiObject()
    r.length = if (iiLt(t.mode, 0)) -1 else iiLength(t.source)
    r.input = t.input
    r.output = iiObject()
    r.menu = iiObject()
    r.canFinish = t.canFinish
    r.description = iiString(t2.subSequence(iiAdd(t3, 1), t2.length()))
    r.tag = t2.subSequence(0, t3)

    val keys = iiForInKeys(t.output)
    var i = 0
    while (i < iiLength(keys)) {
        val k = keys[i]
        if (iiInstanceOf(t.output[k], js("Function"))) {
            r.menu[k] = t.output[k]
        } else {
            r.output[k] = t.output[k]
        }
        i++
    }
    return r
}

private fun iiMatchParamSubcommand(api: dynamic, cp: dynamic, ps: dynamic): dynamic {
    val t = iiObject()
    t.source = iiAdd(iiAdd(cp.mainCommand, " "), ps)
    t.cmdname = cp.mainCommand
    t.hasSlash = false
    t.strParam = ps
    t.input = iiArray()
    t.output = iiObject()
    t.prompt = iiArray()
    t.patterns = iiArray()
    t.help = js("null")
    t.canFinish = false
    api.procParams(t)

    val t2 = t.prompt[0]
    val t3 = iiIndexOf(t2.toString(), "\n")
    val r = iiObject()
    r.length = if (iiLt(t.mode, 0)) -1 else iiLength(ps)
    r.input = t.input
    r.output = iiObject()
    r.menu = iiObject()
    r.canFinish = t.canFinish
    r.description = iiString(t2.subSequence(iiAdd(t3, 1), t2.length()))
    r.tag = t2.subSequence(iiAdd(iiLength(t.cmdname), 1), t3)

    val keys = iiForInKeys(t.output)
    var i = 0
    while (i < iiLength(keys)) {
        val k = keys[i]
        if (iiInstanceOf(t.output[k], js("Function"))) {
            r.menu[k] = t.output[k]
        } else {
            r.output[k] = iiSliceFrom(t.output[k], iiAdd(iiLength(t.cmdname), 1))
        }
        i++
    }
    return r
}

// ---------------- getParamTag ----------------

private fun iiGetParamTag(cp: dynamic, ms: dynamic, mt: dynamic, md: dynamic): dynamic {
    var z: dynamic = cp.name
    var t: dynamic
    var t2: dynamic

    if (iiLooseEq(mt, 1) || iiLooseEq(mt, 3)) {
        val type = cp.type
        if (iiStrictEq(type, "int")) {
            z = iiAdd(z, ":整数")
        } else if (iiStrictEq(type, "uint")) {
            z = iiAdd(z, ":正整数")
        } else if (iiStrictEq(type, "float") || iiStrictEq(type, "relative")) {
            z = iiAdd(z, ":数值")
        } else if (iiStrictEq(type, "nbt")) {
            z = iiAdd(z, ":数据标签")
        } else if (iiStrictEq(type, "rawjson")) {
            z = iiAdd(z, ":文本JSON")
        } else if (iiStrictEq(type, "json")) {
            z = iiAdd(z, ":JSON")
        } else if (iiStrictEq(type, "selector")) {
            z = iiAdd(z, ":实体")
        } else if (iiStrictEq(type, "enum")) {
            z = iiAdd(z, ":列表")
        } else if (iiStrictEq(type, "plain")) {
            // 不追加类型
        } else if (iiStrictEq(type, "custom")) {
            if (iiTruthy(cp.vtype)) z = iiAdd(iiAdd(z, ":"), cp.vtype)
        } else if (iiStrictEq(type, "position")) {
            if (iiLooseEq(mt, 3)) {
                z = iiAdd(z, ":x y z")
            } else {
                t2 = if (iiTruthy(md.uv)) {
                    iiArrayOf("左", "上", "前")
                } else {
                    iiArrayOf("x", "y", "z")
                }
                t = iiExecPositionTagJs(ms)
                if (iiTruthy(t[1])) t2[0] = t[1]
                if (iiTruthy(t[2])) t2[1] = t[2]
                if (iiTruthy(t[3])) t2[2] = t[3]
                z = iiAdd(iiAdd(z, ":"), iiJoin(t2, " "))
            }
        } else if (iiStrictEq(type, "command")) {
            if (iiTruthy(md)) return md.tag
            z = iiAdd(z, ":命令")
        } else if (iiStrictEq(type, "subcommand")) {
            if (iiTruthy(md)) return md.tag
            z = iiAdd(z, ":子命令")
        } else {
            z = iiAdd(z, ":文本")
        }
    }

    if (iiLooseNe(cp.type, "plain") && !iiTruthy(cp.optional) && !iiTruthy(cp.canIgnore) &&
        !iiTruthy(cp.chainOptional)
    ) {
        z = iiAdd(iiAdd("<", z), ">")
    }
    if (iiTruthy(cp.optional) || iiTruthy(cp.canIgnore) || iiTruthy(cp.chainOptional)) {
        z = iiAdd(iiAdd("[", z), "]")
    }
    if (iiStrictEq(cp.type, "custom")) {
        if (iiTruthy(cp.prefix)) z = iiAdd(cp.prefix, z)
        if (iiTruthy(cp.suffix)) z = iiAdd(z, cp.suffix)
    }
    if (iiTruthy(cp.repeat) && iiLooseEq(mt, 1)) z = iiAdd(z, " ...")
    z = iiNewSpannableStringJs(z)
    if (iiLooseEq(mt, 2)) {
        z.setSpan(
            iiNewForegroundColorSpanJs(js("Common").theme.promptcolor),
            0,
            z.length(),
            z.SPAN_INCLUSIVE_EXCLUSIVE
        )
    } else if (iiLooseEq(mt, 1)) {
        z.setSpan(
            iiNewForegroundColorSpanJs(js("Common").theme.highlightcolor),
            0,
            z.length(),
            z.SPAN_INCLUSIVE_EXCLUSIVE
        )
    } else if (iiLooseEq(mt, 3)) {
        z.setSpan(
            iiNewForegroundColorSpanJs(js("Common").theme.criticalcolor),
            0,
            z.length(),
            z.SPAN_INCLUSIVE_EXCLUSIVE
        )
    }
    return z
}

// ---------------- procSelector ----------------

private fun iiProcSelector(api: dynamic, cp: dynamic, ps: dynamic): dynamic {
    var t: dynamic
    var pl: dynamic
    var ms: dynamic
    var ls: dynamic
    var cp2: dynamic
    var mr: dynamic
    var bb: dynamic
    var sk: dynamic = false

    // [全文, 选择器类型, "[", 修饰符, "]", 后置空格]
    val c = iiExecSelectorJs(ps)
    if (!iiTruthy(c)) {
        // 正在输入@ / 输入的是玩家名
        ms = iiFirstToken(ps)
        t = iiObject()
        t.length = iiLength(ms)
        t.recommend = iiObject()
        t.canFinish = iiGt(iiLength(ps), 0)
        if (!iiTestPlayerNameJs(ms) || (iiLength(ms) != 0 && !iiIsNaN(ms))) {
            return iiDescription(iiAdd(ms, "不是合法的玩家名或选择器"))
        }
        if (iiLooseEq(cp.target, "entity") || iiLooseEq(cp.target, "player")) {
            t.recommend["@a - 选择所有玩家"] = "@a"
            t.recommend["@p - 选择距离最近的玩家"] = "@p"
            t.recommend["@r - 选择随机玩家"] = "@r"
        }
        if (iiLooseEq(cp.target, "entity") || iiLooseEq(cp.target, "nonplayer")) {
            t.recommend["@e - 选择所有实体"] = "@e"
        }
        if (iiLooseNe(cp.target, "nonselector")) {
            t.recommend["@s - 选择命令执行者"] = "@s"
        }
        t.input = iiKeys(t.recommend)
        if (iiTruthy(iiMcAvailable())) {
            t.output = iiObject()
            pl = iiMcGetInfo("playernames")
            if (iiTruthy(pl)) {
                val plKeys = iiForInKeys(pl)
                var pi = 0
                while (pi < iiLength(plKeys)) {
                    val k = plKeys[pi]
                    if (iiStartsWith(iiString(pl[k]), ms)) {
                        t.output[pl[k]] = iiString(pl[k])
                    }
                    pi++
                }
                t.input = iiConcat2(t.input, iiKeys(t.output))
            }
        } else {
            iiMcApplySense(t)
        }
        return t
    } else if (iiLt(iiLength(c[1]), 1)) {
        // 正在输入p/e/a/r
        t = iiObject()
        t.length = 1
        t.recommend = iiObject()
        t.recommend["@a - 选择所有玩家"] = "@a"
        t.recommend["@p - 选择距离最近的玩家"] = "@p"
        t.recommend["@r - 选择随机玩家"] = "@r"
        t.recommend["@s - 选择命令执行者"] = "@s"
        t.canFinish = false
        if (iiLooseEq(cp.target, "entity")) t.recommend["@e - 选择所有实体"] = "@e"
        t.input = iiKeys(t.recommend)
        return t
    } else if (iiLooseEq(iiLength(c[1]), 1) && !iiTruthy(c[2])) {
        // 正在输入[ / 结束
        t = iiObject()
        t.length = 2
        t.assist = iiObject()
        t.assist["[...] - 插入参数"] = "["
        t.input = iiArrayOf("[...] - 插入参数")
        t.canFinish = true
        return t
    } else if (iiTruthy(c[2]) && !iiTruthy(c[4]) && !iiTruthy(c[5])) {
        // 正在输入修饰符
        t = iiObject()
        t.length = iiAdd(3, iiLength(c[3]))
        t.recommend = iiObject()
        t.output = iiObject()
        t.menu = iiObject()
        t.input = iiArray()
        t.canFinish = false

        val ml = iiSplit(c[3], ",")
        pl = iiObject()
        ls = ml.pop()
        bb = iiSlice(ps, 0, iiSub(iiLength(ps), iiLength(ls)))
        if (iiLt(iiLength(ml), 4) && iiGt(iiLength(ml), 0)) {
            sk = true
            var mi = 0
            while (mi < iiLength(ml)) {
                // 特殊情况：前面是纯数字坐标
                sk = iiAnd(sk, iiTestNumPairJs(ml[mi]))
                mi++
            }
        }
        var mi = 0
        while (mi < iiLength(ml)) {
            // 检验之前的参数，此处需更新
            ms = if (iiTruthy(sk)) iiExecSelectorNumJs(ml[mi]) else iiExecSelectorPairJs(ml[mi])
            if (!iiTruthy(ms)) {
                return iiDescription(iiAdd(ml[mi], "不是合法的选择器参数对"))
            }
            if (iiTruthy(sk)) {
                mi++
                continue
            }
            cp2 = api.library.selectors[ms[1]]
            if (!iiTruthy(cp2)) {
                mi++
                continue
            }
            if (iiTruthy(cp2.hasInverted) && iiTestInvertedJs(ms[3])) {
                ms[3] = iiSliceFrom(ms[3], 1)
            } else {
                pl[ms[1]] = true
            }
            mr = api.matchParam(cp2, iiAdd(ms[3], " "))
            if (!iiTruthy(mr) || !iiGe(mr.length, 0)) {
                return iiDescription(
                    if (iiTruthy(mr)) mr.description
                    else iiAdd(ml[mi], "不是合法的选择器参数对")
                )
            } else if (iiLt(mr.length, iiLength(ms[3])) || !iiTruthy(mr.canFinish)) {
                return iiDescription(iiAdd("未结束的选择器参数：", ms[3]))
            }
            mi++
        }

        ms = if (iiTruthy(sk)) iiExecSelectorNumJs(ls) else iiExecSelectorPartialJs(ls)
        if (!iiTruthy(ms)) {
            return iiDescription(iiAdd(ls, "不是合法的选择器参数对"))
        }
        if (iiTruthy(sk)) { // 特殊处理
            t.recommend[", - 下一个参数"] = iiAdd(iiAdd(bb, ls), ",")
            t.output["] - 结束参数"] = iiAdd(iiAdd(bb, ls), "]")
            t.input.push(", - 下一个参数", "] - 结束参数")
            return t
        }
        if (iiTruthy(ms[2])) { // 输入修饰符内容
            if (!iiTruthy(ms[1])) {
                return iiDescription(iiAdd(ls, "缺少等号"))
            }
            bb = iiAdd(bb, iiAdd(ms[1], ms[2]))
            cp2 = api.library.selectors[ms[1]]
            if (iiTruthy(cp2)) {
                if (iiTruthy(cp2.hasInverted)) {
                    if (iiStartsWith(ms[3], "!")) {
                        ms[3] = iiSliceFrom(ms[3], 1)
                        bb = iiAdd(bb, "!")
                    } else if (!iiTruthy(ms[3])) {
                        t.recommend["! - 反向选择"] = iiAdd(bb, "!")
                        t.input.push("! - 反向选择")
                    }
                }
                mr = api.matchParam(cp2, ms[3])
                if (!iiTruthy(mr) || iiLt(mr.length, iiLength(ms[3]))) {
                    return iiDescription(
                        if (iiTruthy(mr)) mr.description
                        else iiAdd(ls, "不是合法的选择器参数对")
                    )
                }
                if (iiTruthy(mr.canFinish)) {
                    t.recommend[", - 下一个参数"] = iiAdd(iiAdd(bb, ms[3]), ",")
                    t.output["] - 结束参数"] = iiAdd(iiAdd(bb, ms[3]), "]")
                    t.input.push(", - 下一个参数", "] - 结束参数")
                }
                val assistKeys = iiForInKeys(mr.assist)
                var ai = 0
                while (ai < iiLength(assistKeys)) {
                    val k = assistKeys[ai]
                    if (!iiIn(k, t.recommend)) t.recommend[k] = iiAdd(ps, mr.assist[k])
                    ai++
                }
                val recKeys = iiForInKeys(mr.recommend)
                var ri = 0
                while (ri < iiLength(recKeys)) {
                    val k = recKeys[ri]
                    if (!iiIn(k, t.recommend)) t.recommend[k] = iiAdd(bb, mr.recommend[k])
                    ri++
                }
                val outKeys = iiForInKeys(mr.output)
                var oi = 0
                while (oi < iiLength(outKeys)) {
                    val k = outKeys[oi]
                    if (!iiIn(k, t.recommend)) t.recommend[k] = iiAdd(bb, mr.output[k])
                    oi++
                }
                val menuKeys = iiForInKeys(mr.menu)
                var mi2 = 0
                while (mi2 < iiLength(menuKeys)) {
                    val k = menuKeys[mi2]
                    if (!iiIn(k, t.menu)) t.menu[k] = mr.menu[k]
                    mi2++
                }
                val inKeys = iiForInKeys(mr.input)
                var ini = 0
                while (ini < iiLength(inKeys)) {
                    val k = inKeys[ini]
                    if (iiLt(iiIndexOf(t.input, mr.input[k]), 0)) t.input.push(mr.input[k])
                    ini++
                }
            } else {
                t.recommend[", - 下一个参数"] = iiAdd(iiAdd(bb, ms[3]), ",")
                t.output["] - 结束参数"] = iiAdd(iiAdd(bb, ms[3]), "]")
                t.input.push(", - 下一个参数", "] - 结束参数")
            }
        } else { // 输入修饰符名称
            if (iiTruthy(ms[1])) {
                t.recommend["= - 输入参数"] = iiAdd(iiAdd(bb, ms[1]), "=")
                t.input.push("= - 输入参数")
            }
            val selectorKeys = iiKeys(api.library.selectors)
            var si = 0
            while (si < iiLength(selectorKeys)) {
                val e = selectorKeys[si]
                if (!iiStartsWith(e, ms[1])) {
                    si++
                    continue
                }
                if (iiTruthy(pl[e])) {
                    si++
                    continue
                }
                val label = iiAdd(iiAdd(e, " - "), api.library.selectors[e].name)
                t.recommend[label] = iiAdd(iiAdd(bb, e), "=")
                t.input.push(label)
                si++
            }
        }
        return t
    } else if (iiTruthy(c[4])) {
        // 输入完毕
        ms = iiFirstToken(ps)
        t = iiObject()
        t.length = iiLength(ms)
        t.canFinish = true
        return t
    }
    return iiDescription(iiAdd(c[0], "不是合法的选择器"))
}

// ---------------- procPosition ----------------

private fun iiProcPosition(api: dynamic, cp: dynamic, ps: dynamic): dynamic {
    val l = iiSplitWsJs(ps)
    var f = true
    var uv = false
    val n = iiMin(iiLength(l), 3)
    var t: dynamic
    var pp: dynamic
    var t2: dynamic

    var i = 0
    while (iiLt(i, n)) {
        if (iiLooseEq(i, 0) && iiStartsWith(l[0], "^") &&
            iiTruthy(js("CA").hasFeature("enableLocalCoord"))
        ) {
            uv = true
        }
        t = if (uv) iiExecCoordLocalJs(l[i]) else iiExecCoordJs(l[i])
        if (!iiTruthy(t)) {
            return iiDescription(iiAdd(l[i], "不是合法的坐标值"))
        }
        if ((!iiTruthy(t[1]) || iiTruthy(t[2])) && !iiTestCoordValueJs(t[2])) {
            if (iiLooseEq(i, iiSub(n, 1))) {
                f = false
            } else {
                return iiDescription(iiAdd(l[i], "不是合法的坐标值"))
            }
        }
        i++
    }

    val r = iiObject()
    r.length = if (iiLooseEq(n, 3) && iiGt(iiLength(l[2]), 0)) {
        iiLength(iiExecPosition3Js(ps)[0])
    } else {
        iiLength(ps)
    }
    r.input = iiArray()
    r.assist = iiObject()
    r.canFinish = f && iiLooseEq(n, 3)
    r.uv = uv

    if (iiGt(iiLength(l[iiSub(n, 1)]), 0)) {
        if (iiLt(n, 3)) {
            r.input.push("  - 空格")
            r.assist["  - 空格"] = " "
        }
    } else {
        if (!uv) {
            r.input.push("~ - 相对坐标")
            r.assist["~ - 相对坐标"] = "~"
        }
        if ((iiStrictEq(iiLength(ps), 0) || uv) &&
            iiTruthy(js("CA").hasFeature("enableLocalCoord"))
        ) {
            r.input.push("^ - 局部坐标(^左 ^上 ^前)")
            r.assist["^ - 局部坐标(^左 ^上 ^前)"] = "^"
        }
    }

    if (iiTruthy(iiMcAvailable())) {
        r.output = iiObject()
        pp = iiMcGetInfo("playerposition").slice()
        if (iiTruthy(pp) && iiLooseNe(pp[1], 0)) {
            pp[1] = iiSub(pp[1], 1.619999885559082)
            t2 = iiJoin(pp, " ")
            r.output[iiAdd(t2, " - 玩家实际坐标")] = t2
            t2 = iiFloorJoin3(pp[0], pp[1], pp[2])
            r.output[iiAdd(t2, " - 玩家脚部方块坐标")] = t2
            t2 = iiFloorJoin3(pp[0], iiAdd(pp[1], 1), pp[2])
            r.output[iiAdd(t2, " - 玩家头部方块坐标")] = t2
            t2 = iiFloorJoin3(pp[0], iiSub(pp[1], 1), pp[2])
            r.output[iiAdd(t2, " - 玩家脚下方块坐标")] = t2
            pp = iiMcGetInfo("pointedblockpos")
            if (iiTruthy(pp) && iiGe(pp[1], 0)) {
                t2 = iiJoin(pp, " ")
                r.output[iiAdd(t2, " - 玩家指向方块坐标")] = t2
            }
        }
        r.input = iiConcat2(r.input, iiKeys(r.output))
    } else {
        iiMcApplySense(r)
    }
    return r
}

// ---------------- matchString / assistJSON ----------------

private fun iiMatchString(ps: dynamic, a: dynamic, r: dynamic): dynamic {
    // 匹配实现在 Kotlin/JS 的 Accel.kt：有 jar 走 Java，没有走那里的等价 JS 实现
    return iiAccelMatchString(ps, a, r)
}

private fun iiAssistJson(param: dynamic): dynamic {
    val options = iiObject()
    options.param = param
    js("CA").Assist.editParamJSON(options, { text: dynamic ->
        js("CA").cmd.getText().append(text)
        js("undefined")
    })
    return js("undefined")
}

// ---------------- showHelp / show / hide ----------------

private fun iiShowHelp(api: dynamic): dynamic {
    val pp = iiNewSpannableStringBuilderJs()
    api.source = "/help"
    api.cmdname = "help"
    api.hasSlash = true
    api.strParam = ""
    api.output = iiObject()
    api.output["设置"] = { ->
        js("CA").showSettings()
        js("undefined")
    }
    api.output["关于命令助手..."] = { ->
        iiShowAssistAbout()
    }
    api.output["查看中文Wiki"] = { ->
        iiShowWiki()
    }
    api.output["意见反馈"] = { ->
        js("GiteeFeedback").showFeedbacks()
        js("undefined")
    }
    api.input = iiKeys(api.output)
    pp.append("命令助手 - 设置 & 关于\n")
    js("appendSSB")(
        pp,
        "（这个命令的用途是显示帮助，不过你有这个工具就不需要帮助了吧）",
        iiNewForegroundColorSpanJs(js("Common").theme.promptcolor)
    )
    api.prompt = iiArrayOf(pp)
    api.help = "https://ca.projectxero.top/blog/about/"
    api.patterns = iiArray()
    return api.apply()
}

private fun iiShowAssistAbout(): dynamic {
    if (iiTruthy(js("CA").settings.splitScreenMode)) return js("undefined")
    js("CA").showAssist.tx = iiNeg(js("CA").showAssist.screenWidth)
    js("CA").showAssist.linear.setTranslationX(js("CA").showAssist.tx)
    js("CA").showAssist.hCheck()
    if (iiTruthy(js("CA").settings.noAnimation)) return js("undefined")
    val animation = iiNewTranslate8Js(
        js("G").Animation.ABSOLUTE,
        js("CA").showAssist.screenWidth,
        js("G").Animation.ABSOLUTE,
        0,
        js("G").Animation.RELATIVE_TO_SELF,
        0,
        js("G").Animation.RELATIVE_TO_SELF,
        0
    )
    animation.setDuration(200)
    js("CA").showAssist.linear.startAnimation(animation)
    return js("undefined")
}

private fun iiShowWiki(): dynamic {
    try {
        iiOpenWikiJs("https://minecraft-zh.gamepedia.com/%E5%91%BD%E4%BB%A4")
    } catch (e: dynamic) {
        iiShowWebViewDialogJs("https://minecraft-zh.gamepedia.com/%E5%91%BD%E4%BB%A4")
    }
    return js("undefined")
}

private fun iiShow(self: dynamic, api: dynamic): dynamic {
    iiUiJs({ ->
        iiShowUi(self, api)
        js("undefined")
    })
    return js("undefined")
}

private fun iiShowUi(self: dynamic, api: dynamic): dynamic {
    try {
        // 原实现这里读的是 CA.IntelliSense.ui，不是闭包里的 this。
        if (iiTruthy(js("CA").IntelliSense.ui)) return js("undefined")
        if (!iiTruthy(self.prompt)) {
            self.adptcon = js("null")
            self.apply = { z: dynamic -> iiShowApply(self, api, z) }
            self.vmaker = { _holder: dynamic -> iiShowVmaker() }
            self.vbinder = { holder: dynamic, s: dynamic, _i: dynamic, _a: dynamic ->
                iiShowVbinder(self, holder, s)
            }
            self.prompt = iiNewTextViewJs()
            self.prompt.setLayoutParams(iiNewAbsParamsJs(-1, -2))
            self.prompt.setPadding(iiDp(20), iiDp(10), iiDp(20), iiDp(10))
            self.prompt.setTypeface(iiOr(js("G").Typeface.MONOSPACE, js("G").Typeface.DEFAULT))
            self.prompt.setLineSpacing(10, 1)
            js("Common").applyStyle(self.prompt, "textview_default", 2)
            self.list = iiNewListViewJs()
            self.list.setOnItemClickListener(iiNewItemClickListenerJs(
                { parent: dynamic, view: dynamic, pos: dynamic, id: dynamic ->
                    iiShowItemClick(api, parent, view, pos, id)
                }
            ))
            self.list.setOnItemLongClickListener(iiNewItemLongClickListenerJs(
                { parent: dynamic, view: dynamic, pos: dynamic, id: dynamic ->
                    iiShowItemLongClick(self, api, parent, view, pos, id)
                }
            ))
            self.list.addOnLayoutChangeListener(iiNewLayoutChangeListenerJs(
                { v: dynamic, l: dynamic, t: dynamic, r: dynamic, b: dynamic,
                  ol: dynamic, ot: dynamic, or: dynamic, ob: dynamic ->
                    iiShowLayoutChange(self, api, v, l, t, r, b, ol, ot, or, ob)
                }
            ))
            self.list.addHeaderView(self.prompt)
            self.list.setLayoutParams(iiNewFrameParams2Js(-1, -1))
            if (iiLooseEq(js("G").style, "Material")) {
                // Fixed：Android 5.0以下FastScroller会尝试将RhinoListAdapter强转为BaseAdapter
                self.list.setFastScrollEnabled(true)
                self.list.setFastScrollAlwaysVisible(false)
            }
            js("CA").showAssist.initContent(self.list)
            js("PWM").registerResetFlag(js("CA").IntelliSense, "ui")
            js("PWM").registerResetFlag(self, "prompt")
        }
        js("CA").IntelliSense.ui = self.list
        js("CA").showAssist.con.addView(self.list)
    } catch (e: dynamic) {
        js("erp")(e)
    }
    return js("undefined")
}

private fun iiShowApply(self: dynamic, api: dynamic, z: dynamic): dynamic {
    iiUiJs({ ->
        iiShowApplyUi(self, api, z)
        js("undefined")
    })
    return js("undefined")
}

private fun iiShowApplyUi(self: dynamic, api: dynamic, z: dynamic): dynamic {
    try {
        self.prompt.setText(iiOr(z.prompt[0], ""))
        try {
            iiNewUrlJs(z.help)
            js("CA").showAssist.postHelp(0, z.help)
        } catch (e: dynamic) {
            js("CA").showAssist.postHelp(
                1,
                iiOr(z.help, "暂时没有帮助，以后会加上的啦")
            )
        }
        if (iiTruthy(self.adptcon)) {
            self.adptcon.setArray(z.input)
        } else {
            val a = iiNewSimpleListAdapter5Js(
                z.input.slice(),
                self.vmaker,
                self.vbinder,
                js("null"),
                true
            )
            self.adptcon = iiSimpleGetControllerJs(a)
            self.list.setAdapter(a)
        }
    } catch (e: dynamic) {
        js("erp")(e)
    }
    return js("undefined")
}

private fun iiShowVmaker(): dynamic {
    val view = iiNewTextViewJs()
    view.setLayoutParams(iiNewAbsParamsJs(-1, -2))
    js("Common").applyStyle(view, "textview_default", 3)
    return view
}

private fun iiShowVbinder(self: dynamic, holder: dynamic, s: dynamic): dynamic {
    val target = holder.self
    if (iiTruthy(self.keep)) {
        target.setPadding(iiDp(15), iiDp(15), iiDp(15), iiDp(15))
    } else {
        target.setPadding(iiDp(15), iiDp(2), iiDp(15), iiDp(2))
    }
    target.setText(s)
    return js("undefined")
}

private fun iiShowItemClick(
    api: dynamic,
    parent: dynamic,
    view: dynamic,
    pos: dynamic,
    id: dynamic
): dynamic {
    try {
        if (iiLooseEq(pos, 0)) {
            js("CA").IntelliSense.showMoreUsage()
            return js("undefined")
        }
        // 原实现从 CA.IntelliSense.output/input 取当前列表数据。
        val intelli = js("CA").IntelliSense
        val a = intelli.output[intelli.input[iiSub(pos, parent.getHeaderViewsCount())]]
        if (iiInstanceOf(a, js("Function"))) {
            a()
        } else if (iiTruthy(a)) {
            js("CA").cmd.setText(iiString(a))
            js("CA").showGen.activate(false)
        }
    } catch (e: dynamic) {
        js("erp")(e)
    }
    return js("undefined")
}

private fun iiShowItemLongClick(
    self: dynamic,
    api: dynamic,
    parent: dynamic,
    view: dynamic,
    pos: dynamic,
    id: dynamic
): dynamic {
    try {
        if (iiLooseEq(pos, 0)) {
            js("CA").IntelliSense.showMoreUsage()
            return true
        }
        // 原实现从 CA.IntelliSense.output/input 取当前列表数据。
        val intelli = js("CA").IntelliSense
        val a = intelli.output[intelli.input[iiSub(pos, parent.getHeaderViewsCount())]]
        if (iiTruthy(a) && !iiInstanceOf(a, js("Function"))) {
            val metrics = js("Common").getMetrics()
            if (iiTruthy(self.lastToast)) self.lastToast.cancel()
            self.lastToast = iiToastMakeTextJs(js("ctx"), iiString(a), 0)
            val rect = iiNewRectJs()
            view.getGlobalVisibleRect(rect)
            self.lastToast.setGravity(
                js("G").Gravity.CENTER,
                iiSub(rect.centerX(), iiDiv(metrics[0], 2)),
                iiSub(rect.centerY(), iiDiv(metrics[1], 2))
            )
            self.lastToast.show()
        }
        return true
    } catch (e: dynamic) {
        js("erp")(e)
        return true
    }
}

private fun iiShowLayoutChange(
    self: dynamic,
    api: dynamic,
    v: dynamic,
    l: dynamic,
    t: dynamic,
    r: dynamic,
    b: dynamic,
    ol: dynamic,
    ot: dynamic,
    or: dynamic,
    ob: dynamic
): dynamic {
    try {
        val isKeep = iiOr(
            iiGt(
                iiSub(b, t),
                iiMul(iiMul(js("Common").theme.textsize[3], js("G").sp), 8)
            ),
            js("CA").settings.keepWhenIME
        )
        if (iiLooseEq(self.keep, isKeep)) return js("undefined")
        self.keep = isKeep
        if (iiTruthy(isKeep)) {
            self.prompt.setPadding(iiDp(20), iiDp(10), iiDp(20), iiDp(10))
        } else {
            self.prompt.setPadding(iiDp(20), iiDp(2), iiDp(20), iiDp(2))
        }
        iiPostJs(v, { ->
            js("CA").IntelliSense.apply()
            js("undefined")
        })
    } catch (e: dynamic) {
        js("erp")(e)
    }
    return js("undefined")
}

private fun iiHide(api: dynamic): dynamic {
    iiUiJs({ ->
        iiHideUi(api)
        js("undefined")
    })
    return js("undefined")
}

private fun iiHideUi(api: dynamic): dynamic {
    try {
        // 原实现读写的都是 CA.IntelliSense.ui。
        if (!iiTruthy(js("CA").IntelliSense.ui)) return js("undefined")
        js("CA").showAssist.con.removeView(js("CA").IntelliSense.ui)
        js("CA").IntelliSense.ui = js("null")
    } catch (e: dynamic) {
        js("erp")(e)
    }
    return js("undefined")
}

private fun iiShowMoreUsage(api: dynamic): dynamic {
    val pp = iiNewSpannableStringBuilderJs()
    val l = iiLength(js("CA").IntelliSense.prompt)
    pp.append(api.prompt[0])
    var i = 1
    while (i < l) {
        pp.append("\n\n")
        pp.append(api.prompt[i])
        i++
    }
    js("Common").showTextDialog(pp)
    return js("undefined")
}

// ---------------- getParserCompletions ----------------

private fun iiAddEnumItems(
    api: dynamic,
    enumName: dynamic,
    valueStart: dynamic,
    valueEnd: dynamic,
    result: dynamic
) {
    val enumData = api.library.enums[enumName]
    if (!iiTruthy(enumData)) return
    val keys = iiForInKeys(enumData)
    var i = 0
    while (i < iiLength(keys)) {
        val id = keys[i]
        if (!iiHasOwn(enumData, id)) {
            i++
            continue
        }
        val displayName = enumData[id]
        val newText = iiAdd(iiAdd("\"", id), "\"")
        val item = iiObject()
        item.label = iiAdd(iiAdd(id, " - "), displayName)
        item.insertText = newText
        val range = iiObject()
        range.start = iiObject()
        range.start.line = 0
        range.start.character = valueStart
        range.end = iiObject()
        range.end.line = 0
        range.end.character = valueEnd
        item.textEdit = iiObject()
        item.textEdit.range = range
        item.textEdit.newText = newText
        result.push(item)
        i++
    }
}

private fun iiGetParserCompletions(
    api: dynamic,
    parserType: dynamic,
    ps: dynamic,
    cursorOffset: dynamic,
    node: dynamic
): dynamic {
    var valueStart: dynamic
    var valueEnd: dynamic
    var value: dynamic

    if (iiTruthy(node) && (iiStrictEq(node.type, "string") || iiStrictEq(node.type, "number") ||
            iiStrictEq(node.type, "boolean") || iiStrictEq(node.type, "null"))
    ) {
        valueStart = node.offset
        valueEnd = iiAdd(node.offset, node.length)
        value = iiSubstring(ps, valueStart, valueEnd)
        if (iiStrictEq(node.type, "string") && iiGe(iiLength(value), 2) &&
            iiStrictEq(iiCharAt(value, 0), "\"") &&
            iiStrictEq(iiCharAt(value, iiSub(iiLength(value), 1)), "\"")
        ) {
            value = iiSubstring(value, 1, iiSub(iiLength(value), 1))
        }
    } else {
        valueStart = cursorOffset
        valueEnd = cursorOffset
        value = ""
    }

    val result = iiArray()
    if (iiStrictEq(parserType, "selector")) {
        val selResult = api.procSelector(iiObject(), value)
        if (iiTruthy(selResult) && iiTruthy(selResult.recommend)) {
            val selKeys = iiForInKeys(selResult.recommend)
            var si = 0
            while (si < iiLength(selKeys)) {
                val selKey = selKeys[si]
                if (!iiHasOwn(selResult.recommend, selKey)) {
                    si++
                    continue
                }
                val selVal = selResult.recommend[selKey]
                val escapedVal = iiEscapeSelectorValueJs(selVal)
                val newText = iiAdd(iiAdd("\"", escapedVal), "\"")
                var selCursor = iiAdd(iiAdd(valueStart, 1), iiLength(escapedVal))
                val eqPos = iiIndexOf(escapedVal, "=")
                if (iiGe(eqPos, 0)) {
                    selCursor = iiAdd(iiAdd(iiAdd(valueStart, 1), eqPos), 1)
                }
                val item = iiObject()
                item.label = selKey
                item.insertText = newText
                val range = iiObject()
                range.start = iiObject()
                range.start.line = 0
                range.start.character = valueStart
                range.end = iiObject()
                range.end.line = 0
                range.end.character = valueEnd
                item.textEdit = iiObject()
                item.textEdit.range = range
                item.textEdit.newText = newText
                item._selectorCursor = selCursor
                result.push(item)
                si++
            }
        }
    } else if (iiStrictEq(iiIndexOf(parserType, "enum:"), 0)) {
        iiAddEnumItems(api, iiSubstringFrom(parserType, 5), valueStart, valueEnd, result)
    } else if (iiTruthy(api.library.enums[parserType])) {
        iiAddEnumItems(api, parserType, valueStart, valueEnd, result)
    }
    return result
}

// ---------------- isSelectorComplete / parseSelectorPath ----------------

private fun iiIsSelectorComplete(inputStr: dynamic): dynamic {
    val stack = iiArray() // 用于跟踪未闭合的括号
    var inQuote = false // 标记是否在引号内
    var leftBrace = 0
    var rightBrace = 0 // 统计大括号数量
    var leftBracket = 0
    var rightBracket = 0 // 统计中括号数量
    var selectorEnd = -1 // 完整选择器的结束位置

    // 遍历输入字符串的每个字符
    var i = 0
    while (i < iiLength(inputStr)) {
        val c = iiCharAt(inputStr, i)

        // 处理引号（字符串内的内容忽略括号匹配）
        if (iiStrictEq(c, "\"")) inQuote = !inQuote
        if (inQuote) {
            i++
            continue
        }

        // 处理开括号
        if (iiStrictEq(c, "{")) {
            stack.push(c)
            leftBrace++
        }
        if (iiStrictEq(c, "[")) {
            stack.push(c)
            leftBracket++
        }

        // 处理闭括号
        if (iiStrictEq(c, "}")) {
            rightBrace++
            // 如果栈顶是匹配的开括号，则弹出
            if (iiGt(iiLength(stack), 0) &&
                iiStrictEq(stack[iiSub(iiLength(stack), 1)], "{")
            ) {
                stack.pop()
            }
        }
        if (iiStrictEq(c, "]")) {
            rightBracket++
            if (iiGt(iiLength(stack), 0) &&
                iiStrictEq(stack[iiSub(iiLength(stack), 1)], "[")
            ) {
                stack.pop()
            }
            // 如果栈已清空且不在引号内，记录选择器结束位置
            if (iiStrictEq(iiLength(stack), 0) && !inQuote) {
                selectorEnd = i + 1
                break
            }
        }
        i++
    }

    val debugInfo = iiObject()
    debugInfo.leftBrace = leftBrace
    debugInfo.rightBrace = rightBrace
    debugInfo.leftBracket = leftBracket
    debugInfo.rightBracket = rightBracket
    debugInfo.remainingStack = stack.slice() // 未闭合的括号栈
    debugInfo.inQuote = inQuote // 是否在引号内
    debugInfo.selectorEnd = selectorEnd // 选择器结束位置

    val result = iiObject()
    // 有效的选择器：栈清空、引号关闭、括号匹配
    result.isValid = iiStrictEq(iiLength(stack), 0) && !inQuote &&
        leftBrace == rightBrace && leftBracket == rightBracket
    result.debugInfo = debugInfo
    return result
}

/**
 * 按逗号分割字符串，但忽略嵌套结构内的逗号。
 */
private fun iiSplitByComma(str: dynamic): dynamic {
    val segments = iiArray()
    var braceCount = 0
    var bracketCount = 0 // 跟踪括号嵌套深度
    var inQuote = false // 是否在引号内
    var lastSplit = 0 // 上次分割的位置

    var i = 0
    while (i < iiLength(str)) {
        val char = iiCharAt(str, i)

        // 处理引号
        if (iiStrictEq(char, "\"")) inQuote = !inQuote
        if (inQuote) {
            i++
            continue
        }

        // 更新括号计数
        if (iiStrictEq(char, "{")) braceCount++
        else if (iiStrictEq(char, "}")) braceCount--
        if (iiStrictEq(char, "[")) bracketCount++
        else if (iiStrictEq(char, "]")) bracketCount--

        // 在顶层（不在嵌套结构内）遇到逗号时分割
        if (iiStrictEq(char, ",") && braceCount == 0 && bracketCount == 0) {
            segments.push(iiSubstring(str, lastSplit, i))
            lastSplit = i + 1 // 更新分割位置
        }
        i++
    }

    // 添加最后一段
    segments.push(iiSubstring(str, lastSplit, iiLength(str)))
    return segments
}

/**
 * 查找顶层的等号（忽略嵌套结构内的等号），未找到返回 -1。
 */
private fun iiFindTopLevelEqual(str: dynamic): Int {
    var braceCount = 0
    var bracketCount = 0 // 括号嵌套深度
    var inQuote = false // 是否在引号内

    var j = 0
    while (j < iiLength(str)) {
        val c = iiCharAt(str, j)

        // 处理引号
        if (iiStrictEq(c, "\"")) inQuote = !inQuote
        if (inQuote) {
            j++
            continue
        }

        // 更新括号计数
        if (iiStrictEq(c, "{")) braceCount++
        else if (iiStrictEq(c, "}")) braceCount--
        if (iiStrictEq(c, "[")) bracketCount++
        else if (iiStrictEq(c, "]")) bracketCount--

        // 在顶层找到等号
        if (iiStrictEq(c, "=") && braceCount == 0 && bracketCount == 0) {
            return j
        }
        j++
    }
    return -1 // 未找到
}

private fun iiSelectorPathResult(
    stack: dynamic,
    state: dynamic,
    currentInput: dynamic,
    keys: dynamic
): dynamic {
    val o = iiObject()
    o.stack = stack
    o.state = state
    o.currentInput = currentInput
    o.keys = keys
    return o
}

/**
 * 递归解析选择器字符串的核心函数。
 */
private fun iiRecursiveParse(
    currentStr: dynamic,
    parentStack: dynamic,
    keys: dynamic
): dynamic {
    var keyStore = keys
    if (iiLooseEq(keyStore, js("undefined"))) keyStore = iiArray()
    val str = iiTrim(currentStr)
    val stack = parentStack.slice() // 复制父级栈

    // === 处理数组开头 '[' ===
    if (iiStartsWith(str, "[")) {
        // 判断是否是顶层数组
        val isTopLevel = iiStrictEq(iiLength(parentStack), 0)
        // 推入对应的占位符（顶层为{...}，嵌套为[...]）
        stack.push(if (isTopLevel) "{...}" else "[...]")

        // 提取数组内容（去掉开头的'['）
        val innerContent = iiSliceFrom(str, 1)
        // 按逗号分割数组元素
        val items = iiSplitByComma(innerContent)
        var itemIndex = 0
        while (itemIndex < iiLength(items)) {
            val item = items[itemIndex]
            keyStore.push(iiTrim(iiSubstring(item, 0, iiFindTopLevelEqual(item))))
            itemIndex++
        }

        // 获取最后一个元素（当前活跃部分）
        val activeItem = iiTrim(items[iiSub(iiLength(items), 1)])

        // 如果最后一个元素为空
        if (iiStrictEq(activeItem, "")) {
            if (iiStrictEq(stack[iiSub(iiLength(stack), 1)], "{...}")) {
                // 对象上下文：等待键
                return iiSelectorPathResult(stack, "wait_key", "", keyStore)
            } else {
                // 数组上下文：等待数组元素
                return iiSelectorPathResult(stack, "wait_value_array", "", keyStore)
            }
        }

        // 递归解析活跃元素
        return iiRecursiveParse(activeItem, stack, keyStore)
    }

    // === 处理对象开头 '{' ===
    if (iiStartsWith(str, "{")) {
        // 推入对象占位符
        stack.push("{...}")

        // 提取对象内容（去掉开头的'{'）
        val innerContent = iiSliceFrom(str, 1)
        // 按逗号分割键值对
        val pairs = iiSplitByComma(innerContent)
        // 获取最后一个键值对
        val activeItem = iiTrim(pairs[iiSub(iiLength(pairs), 1)])

        // 如果最后一个键值对为空
        if (iiStrictEq(activeItem, "")) {
            // 等待键
            return iiSelectorPathResult(stack, "wait_key", "", keyStore)
        }

        // 递归解析活跃元素
        return iiRecursiveParse(activeItem, stack, keyStore)
    }

    // === 处理键值对 ===
    // 查找顶层的等号（键值对分隔符）
    val equalIndex = iiFindTopLevelEqual(str)

    // 如果找到等号
    if (iiStrictEq(equalIndex, -1)) {
        // === 处理没有等号的字符串 ===
        // 如果是空字符串
        if (iiStrictEq(str, "")) {
            if (iiGt(iiLength(stack), 0) &&
                iiStrictEq(stack[iiSub(iiLength(stack), 1)], "[...]")
            ) {
                // 在数组中等待元素
                return iiSelectorPathResult(stack, "wait_value_array", "", keyStore)
            } else {
                // 等待键
                return iiSelectorPathResult(stack, "wait_key", "", keyStore)
            }
        }

        // 如果在对象上下文中
        if (iiGt(iiLength(stack), 0) &&
            iiStrictEq(stack[iiSub(iiLength(stack), 1)], "{...}")
        ) {
            // 等待键（用户正在输入键名）
            return iiSelectorPathResult(stack, "wait_key", str, keyStore)
        }

        // 默认情况：等待值
        return iiSelectorPathResult(stack, "wait_value", str, keyStore)
    } else {
        // 分割键和值
        val key = iiTrim(iiSubstring(str, 0, equalIndex))
        val value = iiTrim(iiSubstring(str, iiAdd(equalIndex, 1), iiLength(str)))
        val currentInput = value

        // 如果栈顶是对象占位符，替换为实际键名
        if (iiGt(iiLength(stack), 0) &&
            iiStrictEq(stack[iiSub(iiLength(stack), 1)], "{...}")
        ) {
            stack.pop()
        }
        stack.push(key)

        // 如果值是嵌套结构（数组或对象）
        if (iiStartsWith(value, "[") || iiStartsWith(value, "{")) {
            // 递归解析嵌套值
            return iiRecursiveParse(value, stack, keyStore)
        } else {
            // 等待值输入
            stack.push("...")
            return iiSelectorPathResult(stack, "wait_value", currentInput, keyStore)
        }
    }
}

private fun iiParseSelectorPath(api: dynamic, inputStr: dynamic): dynamic {
    // 首先检查选择器是否完整
    val selectorCompleteResult = api.isSelectorComplete(inputStr)

    // 如果选择器完整，直接返回完成状态
    if (iiTruthy(selectorCompleteResult.isValid)) {
        val complete = iiObject()
        complete.stack = iiArray()
        complete.state = "complete"
        complete.debug = selectorCompleteResult.debugInfo
        complete.currentInput = ""
        complete.keys = iiArray()
        return complete
    }

    // 开始递归解析
    val result = iiRecursiveParse(iiTrim(inputStr), iiArray(), js("undefined"))
    // 添加调试信息
    result.debug = selectorCompleteResult.debugInfo
    return result
}

// ---------------- getSelectorParamCompletions / getSchemaByContext / removeKeysFromSchema ----------------

private fun iiGetSelectorParamCompletions(
    input: dynamic,
    base: dynamic,
    selectors: dynamic,
    addtext: dynamic,
    sort: dynamic
): dynamic {
    // Common.toast(`getSelectorParamCompletions(...)`)
    val completions = iiObject()
    val sortedKeys = iiKeys(selectors)
    val add = iiOr(addtext, "") // addtext 默认为空字符串
    var i = 0
    while (i < iiLength(sortedKeys)) {
        val key = sortedKeys[i]
        var displayText: dynamic
        val value = selectors[key]
        if (iiStrictEq(iiTypeof(value), "object") && iiTruthy(value.name)) {
            // 如果值是一个对象并且有 name 属性
            displayText = iiAdd(iiAdd(key, " - "), value.name)
        } else {
            // 如果值是一个字符串
            displayText = iiAdd(iiAdd(key, " - "), value)
        }
        if (iiTruthy(input) && iiLt(iiIndexOf(displayText, input), 0)) {
            i++
            continue
        }
        var newBase = base
        if (iiTruthy(input) && iiEndsWith(newBase, input)) {
            newBase = iiSlice(newBase, 0, iiNeg(iiLength(input)))
        }
        completions[displayText] = iiAdd(iiAdd(newBase, key), add)
        i++
    }

    // 如果需要排序，根据 displayText 排序
    if (iiTruthy(sort)) {
        val sortedCompletions = iiObject()
        val completionKeys = iiKeys(completions)
        completionKeys.sort()
        var si = 0
        while (si < iiLength(completionKeys)) {
            val key = completionKeys[si]
            sortedCompletions[key] = completions[key]
            si++
        }
        return sortedCompletions
    }

    return completions
}

private fun iiGetSchemaByContext(api: dynamic, contextValue: dynamic, type: dynamic): dynamic {
    val blockStates = iiStateList(api, type)

    var i = 0
    while (i < iiLength(blockStates)) {
        val item = blockStates[i]
        val ctx = item.context
        if (iiIsArray(ctx)) {
            var j = 0
            while (j < iiLength(ctx)) {
                if (iiStrictEq(ctx[j], contextValue)) {
                    return iiOr(item.schema, js("null"))
                }
                j++
            }
        }
        i++
    }
    return js("null")
}

private fun iiRemoveKeysFromSchema(schema: dynamic, keys: dynamic): dynamic {
    if (!iiTruthy(schema) || iiStrictNe(iiTypeof(schema), "object")) return iiObject()

    val result = iiObject() // 浅拷贝
    val schemaKeys = iiForInKeys(schema)
    var i = 0
    while (i < iiLength(schemaKeys)) {
        val k = schemaKeys[i]
        if (iiHasOwn(schema, k) && iiStrictEq(iiIndexOf(keys, k), -1)) {
            result[k] = schema[k]
        }
        i++
    }
    return result
}

// ---------------- 安装入口 ----------------

fun buildCaIntelliSenseModule(): dynamic {
    val isApi = js("({})")

    isApi.UNINITIALIZED = 0
    isApi.ONLY_COMMAND_NAME = 1
    isApi.UNKNOWN_COMMAND = -1
    isApi.COMMAND_WITH_PATTERN = 2
    isApi.UNKNOWN_PATTERN = -2

    isApi.commandText = js("null")
    isApi.input = iiArray()
    isApi.output = iiArray()
    isApi.cmdname = ""
    isApi.prompt = iiArray()
    isApi.help = ""
    isApi.patterns = iiArray()
    isApi.mode = 0
    isApi.last = iiObject()
    // 枚举对象分类缓存：对象身份不变时复用 keys/descs，避免每次输入都 Object.keys
    isApi.enumClassifyCache = js("null")
    // jsonPro 参数的 JSON Language Service 实例缓存（按 schema 名）
    isApi.jsonLanguageServices = iiObject()

    isApi.callDelay = iiCallDelayFactoryJs { self: dynamic, s: dynamic ->
        iiCallDelay(self, s)
    }
    isApi.apply = { -> iiApply(isApi) }
    isApi.proc = { s: dynamic -> iiProc(isApi, s) }
    isApi.procCmd = { s: dynamic -> iiProcCmd(isApi, s) }
    isApi.procParams = { c: dynamic -> iiProcParams(isApi, c) }
    isApi.matchParam = { cp: dynamic, ps: dynamic, parsedParams: dynamic ->
        iiMatchParam(isApi, cp, ps, parsedParams)
    }
    isApi.getParamTag = { cp: dynamic, ms: dynamic, mt: dynamic, md: dynamic ->
        iiGetParamTag(cp, ms, mt, md)
    }
    isApi.procSelector = { cp: dynamic, ps: dynamic -> iiProcSelector(isApi, cp, ps) }
    isApi.procPosition = { cp: dynamic, ps: dynamic -> iiProcPosition(isApi, cp, ps) }
    isApi.matchString = { ps: dynamic, a: dynamic, r: dynamic -> iiMatchString(ps, a, r) }
    isApi.assistJSON = { param: dynamic -> iiAssistJson(param) }
    isApi.showHelp = { -> iiShowHelp(isApi) }
    isApi.show = iiShowFactoryJs { self: dynamic -> iiShow(self, isApi) }
    isApi.hide = { -> iiHide(isApi) }
    isApi.showMoreUsage = { -> iiShowMoreUsage(isApi) }
    isApi.getParserCompletions =
        { parserType: dynamic, ps: dynamic, cursorOffset: dynamic, node: dynamic ->
            iiGetParserCompletions(isApi, parserType, ps, cursorOffset, node)
        }
    isApi.isSelectorComplete = { inputStr: dynamic -> iiIsSelectorComplete(inputStr) }
    isApi.parseSelectorPath = { inputStr: dynamic -> iiParseSelectorPath(isApi, inputStr) }
    isApi.getSelectorParamCompletions =
        { input: dynamic, base: dynamic, selectors: dynamic, addtext: dynamic, sort: dynamic ->
            iiGetSelectorParamCompletions(input, base, selectors, addtext, sort)
        }
    isApi.getSchemaByContext = { contextValue: dynamic, type: dynamic ->
        iiGetSchemaByContext(isApi, contextValue, type)
    }
    isApi.removeKeysFromSchema = { schema: dynamic, keys: dynamic ->
        iiRemoveKeysFromSchema(schema, keys)
    }

    return isApi
}
