package ca.kotlin.ui

import ca.kotlin.core.*


/**
 * modules/Common.js 的 Kotlin/JS 版本。
 *
 * Common 是主题、通用对话框、文件选择、WebView、Toast、剪贴板与一批
 * 集合/字符串工具的组合模块，几乎所有其他模块都会调用。
 *
 * 这里的迁移原则：
 *  - 保持原 API、字段与调用顺序；MapScript.loadModule("Common", api) 在
 *    installCommon() 内部完成，onCreate 仍由 loadModule 立即触发。
 *  - 依赖动态 this 的 loadTheme/applyStyle/getMetrics 等用薄 JS wrapper
 *    把真实 this 传回 Kotlin。
 *  - 原文件里 `function self(...)` 的函数对象状态（self.linear、
 *    self.refreshText 等）必须留在函数对象本身上，因此用 cMakeSelfJs
 *    生成带 self 引用的 JS 函数，PWM.registerResetFlag / AndroidBridge
 *    的外部访问语义不变。
 *  - JS 数组/Java 数组长度统一走 `| 0` helper；需要传 Kotlin 值的
 *    new/Java 调用都放在带参 JS helper 里，不在 js("...") 中引用局部变量。
 *  - 颜色重载继续走 IntColor / JavaReflect 的精确签名（argbInt 自己算位），
 *    不直接调用 Color.argb/red/green 等会与 float 版本混淆的方法。
 *
 * 本次按约束只新增本文件，不改 Pending.kt 与 main.js。接入时：
 *  - Pending.kt 增加 pending.installCommon = { installCommon() }；
 *  - 由 CaKotlinPending.installAll() 统一安装
 *    Loader.fromFile("modules/Common.js")。
 */

private val commonApi: dynamic = js("({})")

// ---------------- JS 语义 helper ----------------

private val cTruthyJs: dynamic = js("function (v) { return !!v; }")
private fun cTruthy(v: dynamic): Boolean = cTruthyJs(v)

private val cLengthJs: dynamic = js("function (v) { return v.length | 0; }")
private fun cLength(v: dynamic): Int = cLengthJs(v)

private val cStringJs: dynamic = js("function (v) { return String(v); }")
private fun cString(v: dynamic): dynamic = cStringJs(v)

private val cLowerJs: dynamic = js("function (v) { return String(v).toLowerCase(); }")
private fun cLower(v: dynamic): dynamic = cLowerJs(v)

private val cTypeofJs: dynamic = js("function (v) { return typeof v; }")
private fun cTypeof(v: dynamic): dynamic = cTypeofJs(v)

private val cIsNaNJ: dynamic = js("function (v) { return isNaN(v); }")
private fun cIsNaN(v: dynamic): Boolean = cIsNaNJ(v)

private val cIsFiniteJ: dynamic = js("function (v) { return isFinite(v); }")
private fun cIsFinite(v: dynamic): Boolean = cIsFiniteJ(v)

private val cParseIntJs: dynamic = js("function (v) { return parseInt(v); }")
private fun cParseInt(v: dynamic): dynamic = cParseIntJs(v)

private val cParseFloatJs: dynamic = js("function (v) { return parseFloat(v); }")
private fun cParseFloat(v: dynamic): dynamic = cParseFloatJs(v)

private val cLooseEqJs: dynamic = js("function (a, b) { return a == b; }")
private fun cLooseEq(a: dynamic, b: dynamic): Boolean = cLooseEqJs(a, b)

private val cLooseNeJs: dynamic = js("function (a, b) { return a != b; }")
private fun cLooseNe(a: dynamic, b: dynamic): Boolean = cLooseNeJs(a, b)

private val cStrictEqJs: dynamic = js("function (a, b) { return a === b; }")
private fun cStrictEq(a: dynamic, b: dynamic): Boolean = cStrictEqJs(a, b)

private val cInJs: dynamic = js("function (k, o) { return k in o; }")
private fun cIn(k: dynamic, o: dynamic): Boolean = cInJs(k, o)

private val cInstanceOfJs: dynamic = js("function (v, c) { return v instanceof c; }")
private fun cInstanceOf(v: dynamic, c: dynamic): Boolean = cInstanceOfJs(v, c)

private val cForInJs: dynamic = js("function (o, f) { for (var k in o) f(k); }")
private fun cForIn(o: dynamic, f: dynamic) { cForInJs(o, f) }

private val cObjectKeysJs: dynamic = js("function (o) { return Object.keys(o); }")
private fun cObjectKeys(o: dynamic): dynamic = cObjectKeysJs(o)

private val cObjectCreateJs: dynamic = js("function (p) { return Object.create(p); }")
private fun cObjectCreate(p: dynamic): dynamic = cObjectCreateJs(p)

private val cIsArrayJs: dynamic = js("function (v) { return Array.isArray(v); }")
private fun cIsArray(v: dynamic): Boolean = cIsArrayJs(v)

private val cIsFunctionJs: dynamic = js("function (v) { return typeof v == 'function'; }")
private fun cIsFunction(v: dynamic): Boolean = cIsFunctionJs(v)

private val cJsonStringifyJs: dynamic = js("function (v, r, s) { return JSON.stringify(v, r, s); }")
private fun cJsonStringify(v: dynamic, r: dynamic, s: dynamic): dynamic = cJsonStringifyJs(v, r, s)

private val cThisArgsJs: dynamic = js("function (f) { return function () { return f(this, arguments); }; }")
private val cMakeSelfJs: dynamic = js(
    "function (f) { var self = function () { return f(self, arguments); }; return self; }"
)
private val cThis1Js: dynamic = js("function (f) { return function (a) { return f(this, a); }; }")
private val cThis2Js: dynamic = js("function (f) { return function (a, b) { return f(this, a, b); }; }")

private val cMulJs: dynamic = js("function (a, b) { return a * b; }")
private fun cMul(a: dynamic, b: dynamic): dynamic = cMulJs(a, b)

private val cAddJs: dynamic = js("function (a, b) { return a + b; }")
private fun cAdd(a: dynamic, b: dynamic): dynamic = cAddJs(a, b)

private val cDivJs: dynamic = js("function (a, b) { return a / b; }")
private fun cDiv(a: dynamic, b: dynamic): dynamic = cDivJs(a, b)

private val cOrJs: dynamic = js("function (a, b) { return a | b; }")
private fun cOr(a: dynamic, b: dynamic): dynamic = cOrJs(a, b)

private val cDpJs: dynamic = js("function (n) { return n * G.dp; }")
private fun cDp(n: Int): dynamic = cDpJs(n)

private val cHexNumberJs: dynamic = js("function (v) { return Number('0x' + String(v).slice(1)); }")
private fun cHexNumber(v: dynamic): dynamic = cHexNumberJs(v)

private val cArgbBitsJs: dynamic = js(
    "function (a, r, g, b) { return (a << 24) | (r << 16) | (g << 8) | b; }"
)
private fun cArgbBits(alpha: dynamic, red: dynamic, green: dynamic, blue: dynamic): dynamic =
    cArgbBitsJs(alpha, red, green, blue)

private val cRgbBitsJs: dynamic = js(
    "function (r, g, b) { return (0xff << 24) | (r << 16) | (g << 8) | b; }"
)
private fun cRgbBits(red: dynamic, green: dynamic, blue: dynamic): dynamic = cRgbBitsJs(red, green, blue)

private val cAlphaBitsJs: dynamic = js(
    "function (c, a) { return (a << 24) | (c & 0xffffff); }"
)
private fun cAlphaBits(color: dynamic, alpha: dynamic): dynamic = cAlphaBitsJs(color, alpha)

private val cIntValueJs: dynamic = js("function (v) { var n = new java.lang.Long(v); return n.intValue(); }")
private fun cIntValue(v: dynamic): dynamic = cIntValueJs(v)

private val cChannelJs: dynamic = js("function (n, shift) { return (n >>> shift) & 0xff; }")
private fun cChannel(n: dynamic, shift: Int): dynamic = cChannelJs(n, shift)

private fun cArrayOf(vararg items: dynamic): dynamic {
    val a = js("[]")
    var i = 0
    while (i < items.size) {
        a.push(items[i])
        i++
    }
    return a
}

private fun cToString(s: dynamic): dynamic {
    if (cInstanceOf(s, js("java.lang.CharSequence"))) return s
    return cString(s)
}

// ---------------- UI 构造 helper ----------------

private val cNewTextViewJs: dynamic = js("function () { return new G.TextView(ctx); }")
private fun cNewTextView(): dynamic = cNewTextViewJs()

private val cNewLinearLayoutJs: dynamic = js("function () { return new G.LinearLayout(ctx); }")
private fun cNewLinearLayout(): dynamic = cNewLinearLayoutJs()

private val cNewFrameLayoutJs: dynamic = js("function () { return new G.FrameLayout(ctx); }")
private fun cNewFrameLayout(): dynamic = cNewFrameLayoutJs()

private val cNewListViewJs: dynamic = js("function () { return new G.ListView(ctx); }")
private fun cNewListView(): dynamic = cNewListViewJs()

private val cNewScrollViewJs: dynamic = js("function () { return new G.ScrollView(ctx); }")
private fun cNewScrollView(): dynamic = cNewScrollViewJs()

private val cNewEditTextJs: dynamic = js("function () { return new G.EditText(ctx); }")
private fun cNewEditText(): dynamic = cNewEditTextJs()

private val cNewCheckBoxJs: dynamic = js("function () { return new G.CheckBox(ctx); }")
private fun cNewCheckBox(): dynamic = cNewCheckBoxJs()

private val cNewSeekBarJs: dynamic = js("function () { return new G.SeekBar(ctx); }")
private fun cNewSeekBar(): dynamic = cNewSeekBarJs()

private val cNewImageViewJs: dynamic = js("function () { return new G.ImageView(ctx); }")
private fun cNewImageView(): dynamic = cNewImageViewJs()

private val cNewSpaceJs: dynamic = js("function () { return new G.Space(ctx); }")
private fun cNewSpace(): dynamic = cNewSpaceJs()

private val cNewViewJs: dynamic = js("function () { return new G.View(ctx); }")
private fun cNewView(): dynamic = cNewViewJs()

private val cNewWebViewJs: dynamic = js("function () { return new G.WebView(ctx); }")
private fun cNewWebView(): dynamic = cNewWebViewJs()

private val cNewCanvasJs: dynamic = js("function () { return new G.Canvas(); }")
private fun cNewCanvas(): dynamic = cNewCanvasJs()

private val cNewRectJs: dynamic = js("function () { return new G.Rect(); }")
private fun cNewRect(): dynamic = cNewRectJs()

private val cNewPaintJs: dynamic = js("function () { return new G.Paint(); }")
private fun cNewPaint(): dynamic = cNewPaintJs()

private val cNewPopupWindowJs: dynamic = js("function (v, w, h) { return new G.PopupWindow(v, w, h); }")
private fun cNewPopupWindow(view: dynamic, width: dynamic, height: dynamic): dynamic =
    cNewPopupWindowJs(view, width, height)

private val cNewColorDrawableJs: dynamic = js("function (c) { return new G.ColorDrawable(c); }")
private fun cNewColorDrawable(color: dynamic): dynamic = cNewColorDrawableJs(color)

private val cNewAbsParamsJs: dynamic = js("function (w, h) { return new G.AbsListView.LayoutParams(w, h); }")
private fun cNewAbsParams(width: dynamic, height: dynamic): dynamic = cNewAbsParamsJs(width, height)

private val cNewLinearParams2Js: dynamic = js(
    "function (w, h) { return new G.LinearLayout.LayoutParams(w, h); }"
)
private fun cNewLinearParams2(width: dynamic, height: dynamic): dynamic = cNewLinearParams2Js(width, height)

private val cNewLinearParams3Js: dynamic = js(
    "function (w, h, wt) { return new G.LinearLayout.LayoutParams(w, h, wt); }"
)
private fun cNewLinearParams3(width: dynamic, height: dynamic, weight: dynamic): dynamic =
    cNewLinearParams3Js(width, height, weight)

private val cNewFrameParams2Js: dynamic = js(
    "function (w, h) { return new G.FrameLayout.LayoutParams(w, h); }"
)
private fun cNewFrameParams2(width: dynamic, height: dynamic): dynamic = cNewFrameParams2Js(width, height)

private val cNewFrameParams3Js: dynamic = js(
    "function (w, h, g) { return new G.FrameLayout.LayoutParams(w, h, g); }"
)
private fun cNewFrameParams3(width: dynamic, height: dynamic, gravity: dynamic): dynamic =
    cNewFrameParams3Js(width, height, gravity)

private val cNewAlphaAnimationJs: dynamic = js("function (a, b) { return new G.AlphaAnimation(a, b); }")
private fun cNewAlphaAnimation(from: dynamic, to: dynamic): dynamic = cNewAlphaAnimationJs(from, to)

private val cNewAnimationSetJs: dynamic = js("function (s) { return new G.AnimationSet(s); }")
private fun cNewAnimationSet(shareInterpolator: dynamic): dynamic = cNewAnimationSetJs(shareInterpolator)

private val cNewTranslate4Js: dynamic = js(
    "function (a, b, c, d) { return new G.TranslateAnimation(a, b, c, d); }"
)
private fun cNewTranslate4(a: dynamic, b: dynamic, c: dynamic, d: dynamic): dynamic =
    cNewTranslate4Js(a, b, c, d)

private val cNewTranslate8Js: dynamic = js(
    "function (a, b, c, d, e, f, g, h) { return new G.TranslateAnimation(a, b, c, d, e, f, g, h); }"
)
private fun cNewTranslate8(
    a: dynamic, b: dynamic, c: dynamic, d: dynamic,
    e: dynamic, f: dynamic, g: dynamic, h: dynamic
): dynamic = cNewTranslate8Js(a, b, c, d, e, f, g, h)

private val cNewScale6Js: dynamic = js(
    "function (a, b, c, d, e, f) { return new G.ScaleAnimation(a, b, c, d, e, f); }"
)
private fun cNewScale6(a: dynamic, b: dynamic, c: dynamic, d: dynamic, e: dynamic, f: dynamic): dynamic =
    cNewScale6Js(a, b, c, d, e, f)

private val cNewDecelerateJs: dynamic = js(
    "function (v) { return new G.DecelerateInterpolator(v); }"
)
private fun cNewDecelerate(v: dynamic): dynamic = cNewDecelerateJs(v)

private val cNewAccelerateJs: dynamic = js(
    "function (v) { return new G.AccelerateInterpolator(v); }"
)
private fun cNewAccelerate(v: dynamic): dynamic = cNewAccelerateJs(v)

private val cNewCycleJs: dynamic = js("function (v) { return new G.CycleInterpolator(v); }")
private fun cNewCycle(v: dynamic): dynamic = cNewCycleJs(v)

private val cNewBitmapDrawableJs: dynamic = js("function (b) { return new G.BitmapDrawable(b); }")
private fun cNewBitmapDrawable(bitmap: dynamic): dynamic = cNewBitmapDrawableJs(bitmap)

private val cNewPorterDuffXfermodeJs: dynamic = js(
    "function (m) { return new G.PorterDuffXfermode(m); }"
)
private fun cNewPorterDuffXfermode(mode: dynamic): dynamic = cNewPorterDuffXfermodeJs(mode)

// ---------------- 监听器 helper ----------------

private val cTouchListenerJs: dynamic = js(
    "function (f) { return new G.View.OnTouchListener({ onTouch: f }); }"
)
private fun cTouchListener(f: dynamic): dynamic = cTouchListenerJs(f)

private val cClickListenerJs: dynamic = js(
    "function (f) { return new G.View.OnClickListener({ onClick: f }); }"
)
private fun cClickListener(f: dynamic): dynamic = cClickListenerJs(f)

private val cItemClickListenerJs: dynamic = js(
    "function (f) { return new G.AdapterView.OnItemClickListener({ onItemClick: f }); }"
)
private fun cItemClickListener(f: dynamic): dynamic = cItemClickListenerJs(f)

private val cCheckedChangeListenerJs: dynamic = js(
    "function (f) { return new G.CompoundButton.OnCheckedChangeListener({ onCheckedChanged: f }); }"
)
private fun cCheckedChangeListener(f: dynamic): dynamic = cCheckedChangeListenerJs(f)

private val cSeekProgressListenerJs: dynamic = js(
    "function (f) { return new G.SeekBar.OnSeekBarChangeListener({ onProgressChanged: f }); }"
)
private fun cSeekProgressListener(f: dynamic): dynamic = cSeekProgressListenerJs(f)

private val cSeekChangeListenerJs: dynamic = js(
    "function (a, b) { return new G.SeekBar.OnSeekBarChangeListener({ onProgressChanged: a, onStopTrackingTouch: b }); }"
)
private fun cSeekChangeListener(progress: dynamic, stop: dynamic): dynamic =
    cSeekChangeListenerJs(progress, stop)

private val cLayoutChangeListenerJs: dynamic = js(
    "function (f) { return new G.View.OnLayoutChangeListener({ onLayoutChange: f }); }"
)
private fun cLayoutChangeListener(f: dynamic): dynamic = cLayoutChangeListenerJs(f)

private val cDismissListenerJs: dynamic = js(
    "function (f) { return new G.PopupWindow.OnDismissListener({ onDismiss: f }); }"
)
private fun cDismissListener(f: dynamic): dynamic = cDismissListenerJs(f)

// ---------------- Java / 适配器 helper ----------------

private val cNewRunnableJs: dynamic = js("function (f) { return new java.lang.Runnable(f); }")
private fun cNewRunnable(f: dynamic): dynamic = cNewRunnableJs(f)

private val cNewFile1Js: dynamic = js("function (p) { return new java.io.File(p); }")
private fun cNewFile1(path: dynamic): dynamic = cNewFile1Js(path)

private val cNewFile2Js: dynamic = js("function (p, n) { return new java.io.File(p, n); }")
private fun cNewFile2(parent: dynamic, name: dynamic): dynamic = cNewFile2Js(parent, name)

private val cNewFileInputStreamJs: dynamic = js(
    "function (p) { return new java.io.FileInputStream(p); }"
)
private fun cNewFileInputStream(path: dynamic): dynamic = cNewFileInputStreamJs(path)

private val cNewFileOutputStreamJs: dynamic = js(
    "function (p) { return new java.io.FileOutputStream(p); }"
)
private fun cNewFileOutputStream(path: dynamic): dynamic = cNewFileOutputStreamJs(path)

private val cNewFileReaderJs: dynamic = js("function (p) { return new java.io.FileReader(p); }")
private fun cNewFileReader(path: dynamic): dynamic = cNewFileReaderJs(path)

private val cNewBufferedReaderJs: dynamic = js(
    "function (r) { return new java.io.BufferedReader(r); }"
)
private fun cNewBufferedReader(reader: dynamic): dynamic = cNewBufferedReaderJs(reader)

private val cNewInputStreamReaderJs: dynamic = js(
    "function (s) { return new java.io.InputStreamReader(s); }"
)
private fun cNewInputStreamReader(stream: dynamic): dynamic = cNewInputStreamReaderJs(stream)

private val cNewGzipInputStreamJs: dynamic = js(
    "function (s) { return new java.util.zip.GZIPInputStream(s); }"
)
private fun cNewGzipInputStream(stream: dynamic): dynamic = cNewGzipInputStreamJs(stream)

private val cNewGzipOutputStreamJs: dynamic = js(
    "function (s) { return new java.util.zip.GZIPOutputStream(s); }"
)
private fun cNewGzipOutputStream(stream: dynamic): dynamic = cNewGzipOutputStreamJs(stream)

private val cNewByteArrayJs: dynamic = js(
    "function (n) { return java.lang.reflect.Array.newInstance(java.lang.Byte.TYPE, n); }"
)
private fun cNewByteArray(size: Int): dynamic = cNewByteArrayJs(size)

private val cStringBytesJs: dynamic = js(
    "function (v) { var s = new java.lang.String(v); return s.getBytes(); }"
)
private fun cStringBytes(text: dynamic): dynamic = cStringBytesJs(text)

private val cFormatFileSizeJs: dynamic = js(
    "function (c, n, longer) {" +
        " return longer ? android.text.format.Formatter.formatFileSize(c, n)" +
        " : android.text.format.Formatter.formatShortFileSize(c, n);" +
        "}"
)
private fun cFormatFileSize(context: dynamic, length: dynamic, longer: dynamic): dynamic =
    cFormatFileSizeJs(context, length, longer)

private val cNewDisplayMetricsJs: dynamic = js(
    "function () { return new android.util.DisplayMetrics(); }"
)
private fun cNewDisplayMetrics(): dynamic = cNewDisplayMetricsJs()

private val cNewClipDataJs: dynamic = js(
    "function (t) { return android.content.ClipData.newPlainText('', t); }"
)
private fun cNewClipData(text: dynamic): dynamic = cNewClipDataJs(text)

private val cNewRhinoListAdapterJs: dynamic = js(
    "function (a, b) { return new RhinoListAdapter(a, b); }"
)
private fun cNewRhinoListAdapter(array: dynamic, adapter: dynamic): dynamic =
    cNewRhinoListAdapterJs(array, adapter)

private val cNewSimpleListAdapterJs: dynamic = js(
    "function (a, b, c, d) { return new SimpleListAdapter(a, b, c, d); }"
)
private fun cNewSimpleListAdapter(
    array: dynamic,
    maker: dynamic,
    binder: dynamic,
    params: dynamic
): dynamic = cNewSimpleListAdapterJs(array, maker, binder, params)

private val cSimpleAdapterControllerJs: dynamic = js(
    "function (a) { return SimpleListAdapter.getController(a); }"
)
private fun cSimpleAdapterController(adapter: dynamic): dynamic = cSimpleAdapterControllerJs(adapter)

private val cNewMultipleListAdapterJs: dynamic = js(
    "function (a, b, c) { return new MultipleListAdapter(a, b, c); }"
)
private fun cNewMultipleListAdapter(array: dynamic, types: dynamic, typeOf: dynamic): dynamic =
    cNewMultipleListAdapterJs(array, types, typeOf)

private val cMultipleAdapterControllerJs: dynamic = js(
    "function (a) { return MultipleListAdapter.getController(a); }"
)
private fun cMultipleAdapterController(adapter: dynamic): dynamic = cMultipleAdapterControllerJs(adapter)

private val cNewPopupPageJs: dynamic = js("function (v, n) { return new PopupPage(v, n); }")
private fun cNewPopupPage(view: dynamic, name: dynamic): dynamic = cNewPopupPageJs(view, name)

private val cShowDialog4Js: dynamic = js(
    "function (n, l, w, h) { return PopupPage.showDialog(n, l, w, h); }"
)
private fun cShowDialog4(name: dynamic, layout: dynamic, width: dynamic, height: dynamic): dynamic =
    cShowDialog4Js(name, layout, width, height)

private val cShowDialog5Js: dynamic = js(
    "function (n, l, w, h, m) { return PopupPage.showDialog(n, l, w, h, m); }"
)
private fun cShowDialog5(
    name: dynamic, layout: dynamic, width: dynamic, height: dynamic, modal: dynamic
): dynamic = cShowDialog5Js(name, layout, width, height, modal)

private val cNewOverlayJs: dynamic = js(
    "function (v, w, h, g) { return new PopupPage.Overlay(v, w, h, g); }"
)
private fun cNewOverlay(view: dynamic, width: dynamic, height: dynamic, gravity: dynamic): dynamic =
    cNewOverlayJs(view, width, height, gravity)

private val cCreateWebViewJs: dynamic = js(
    "function (o) { return ScriptInterface.createWebView(o); }"
)
private fun cCreateWebView(callbacks: dynamic): dynamic = cCreateWebViewJs(callbacks)

// ---------------- 主题 ----------------

private fun cOnCreate(self: dynamic): dynamic {
    js("L").on("afterAttach", { _name: dynamic, view: dynamic, holder: dynamic ->
        val style = holder.get("style")
        if (cTruthy(style)) {
            commonApi.applyStyle(view, style, holder.get("fontSize"))
        }
        js("undefined")
    })
    js("Intl").mapNamespace(self, "intl", "common")
    return js("undefined")
}

private fun cBuildLightTheme(): dynamic = js(
    "({ 'bgcolor': '#FAFAFA', 'float_bgcolor': '#F5F5F5', 'message_bgcolor': '#FAFAFA'," +
        " 'textcolor': '#212121', 'promptcolor': '#9E9E9E', 'highlightcolor': '#0000FF'," +
        " 'criticalcolor': '#FF0000', 'go_bgcolor': '#EEEEEE', 'go_textcolor': '#000000'," +
        " 'go_touchbgcolor': '#616161', 'go_touchtextcolor': '#FAFAFA' })"
)

private fun cConvertThemeColor(v: dynamic, d: dynamic): dynamic {
    var n = cHexNumber(v)
    if (cIsNaN(n)) n = cHexNumber(d)
    return commonApi.argbInt(0xff, cChannel(n, 16), cChannel(n, 8), cChannel(n, 0))
}

private fun cLoadTheme(self: dynamic, id: dynamic): dynamic {
    val light = cBuildLightTheme()
    val flavorData = js("BuildConfig").flavorData
    val flavorTheme = if (cTruthy(flavorData)) flavorData.theme else flavorData
    val defaultTheme = if (cTruthy(flavorTheme) && cIn(flavorTheme, self.themelist)) {
        cString(flavorTheme)
    } else {
        "light"
    }
    val r = js("({})")
    r.id = if (cIn(id, self.themelist)) cString(id) else defaultTheme
    val k = if (cIn(r.id, self.themelist)) self.themelist[r.id] else light
    cForIn(light, { key: dynamic ->
        r[key] = cConvertThemeColor(k[key], light[key])
        js("undefined")
    })
    r.name = cString(if (cStrictEq(k, light)) self.themelist.light.name else k.name)

    var i = js("Math").floor(cMul(js("CA").settings.alpha, 255))
    if (i >= 0 && i < 255) {
        r.bgcolor = self.setAlpha(r.bgcolor, i)
        r.float_bgcolor = self.setAlpha(r.float_bgcolor, i)
        r.message_bgcolor = self.setAlpha(r.message_bgcolor, 0xe0)
    } else {
        js("CA").settings.alpha = 1
    }
    i = cParseFloat(js("CA").settings.textSize)
    if (!(i > 0)) {
        js("CA").settings.textSize = 1
        i = 1
    }
    val textsize = js("[]")
    textsize.push(js("Math").ceil(cMul(10, i)))
    textsize.push(js("Math").ceil(cMul(12, i)))
    textsize.push(js("Math").ceil(cMul(14, i)))
    textsize.push(js("Math").ceil(cMul(16, i)))
    textsize.push(js("Math").ceil(cMul(18, i)))
    r.textsize = textsize

    val metrics = js("ctx").getResources().getDisplayMetrics()
    js("G").dp = cMul(metrics.density, i)
    self.theme = r
    return js("undefined")
}

private fun cApplyStyleTouch(view: dynamic, event: dynamic, size: dynamic): Boolean {
    try {
        val action = event.getAction()
        if (cLooseEq(action, event.ACTION_DOWN)) {
            commonApi.applyStyle(view, "button_reactive_pressed", size)
        } else if (cLooseEq(action, event.ACTION_CANCEL) || cLooseEq(action, event.ACTION_UP)) {
            commonApi.applyStyle(view, "button_reactive", size)
        }
        return false
    } catch (e: dynamic) {
        js("erp")(e)
        return true
    }
}

private fun cApplyStyle(self: dynamic, v: dynamic, style: dynamic, size: dynamic): dynamic {
    if (style == "bar_float") {
        v.setBackgroundColor(self.theme.float_bgcolor)
        if (js("G.style") == "Material") v.setElevation(cDp(8))
    } else if (style == "bar_float_second") {
        v.setBackgroundColor(self.theme.float_bgcolor)
        if (js("G.style") == "Material") v.setElevation(cDp(4))
    } else if (style == "container_default") {
        v.setBackgroundColor(commonApi.theme.bgcolor)
    } else if (style == "message_bg") {
        v.setBackgroundColor(commonApi.theme.message_bgcolor)
    } else if (style == "textview_default" || style == "button_default" || style == "item_default") {
        v.setTextSize(self.theme.textsize[size])
        v.setTextColor(self.theme.textcolor)
    } else if (style == "textview_prompt" || style == "button_secondary" || style == "item_disabled") {
        v.setTextSize(self.theme.textsize[size])
        v.setTextColor(self.theme.promptcolor)
    } else if (style == "textview_critial" || style == "button_critical" || style == "item_critical") {
        v.setTextSize(self.theme.textsize[size])
        v.setTextColor(self.theme.criticalcolor)
    } else if (style == "textview_highlight" || style == "button_highlight" || style == "item_highlight") {
        v.setTextSize(self.theme.textsize[size])
        v.setTextColor(self.theme.highlightcolor)
    } else if (style == "button_reactive") {
        v.setBackgroundColor(commonApi.theme.go_bgcolor)
        v.setTextSize(commonApi.theme.textsize[size])
        v.setTextColor(commonApi.theme.go_textcolor)
    } else if (style == "button_reactive_pressed") {
        v.setBackgroundColor(commonApi.theme.go_touchbgcolor)
        v.setTextSize(commonApi.theme.textsize[size])
        v.setTextColor(commonApi.theme.go_touchtextcolor)
    } else if (style == "button_reactive_auto") {
        commonApi.applyStyle(v, "button_reactive", size)
        v.setOnTouchListener(cTouchListener({ view: dynamic, event: dynamic ->
            cApplyStyleTouch(view, event, size)
        }))
    } else if (style == "edittext_default") {
        v.setBackgroundColor(js("G.Color.TRANSPARENT"))
        v.setTextSize(commonApi.theme.textsize[size])
        v.setTextColor(commonApi.theme.textcolor)
        v.setHintTextColor(commonApi.theme.promptcolor)
    }
    return js("undefined")
}

private fun cApplyPopup(popup: dynamic): dynamic {
    if (cTruthy(js("G.supportFloat"))) {
        if (js("ScriptInterface").isAndroidSdkAtLeast(26)) {
            popup.setWindowLayoutType(js("G.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY"))
        } else {
            popup.setWindowLayoutType(js("G.WindowManager.LayoutParams.TYPE_PHONE"))
        }
    }
    return js("undefined")
}

// ---------------- 主题切换 ----------------

private fun cChangeThemeAdapter(self: dynamic, e: dynamic): dynamic {
    val view = cNewTextView()
    commonApi.loadTheme(e)
    view.setPadding(cDp(15), cDp(15), cDp(15), cDp(15))
    view.setLayoutParams(cNewAbsParams(-1, -2))
    view.setBackgroundColor(commonApi.theme.bgcolor)
    view.setText(
        if (cLooseEq(self.current, e)) self.intl.resolve("currentTheme", commonApi.theme.name)
        else commonApi.theme.name
    )
    view.setTextSize(commonApi.theme.textsize[3])
    view.setTextColor(commonApi.theme.textcolor)
    commonApi.loadTheme(self.current)
    view.setGravity(js("G.Gravity.CENTER"))
    return view
}

private fun cChangeThemeRefresh(self: dynamic): dynamic {
    self.current = commonApi.theme.id
    self.list.setAdapter(cNewRhinoListAdapter(cObjectKeys(commonApi.themelist), self.adapter))
    self.linear.setBackgroundColor(commonApi.theme.message_bgcolor)
    self.linear.setPadding(cDp(15), cDp(15), cDp(15), 0)
    self.title.setPadding(0, 0, 0, cDp(10))
    self.title.setTextSize(commonApi.theme.textsize[4])
    self.title.setTextColor(commonApi.theme.textcolor)
    self.alpha.setPadding(cDp(10), cDp(10), cDp(10), cDp(10))
    self.alpha.setText(
        self.intl.resolve(
            "alphaField",
            if (cIsFinite(js("CA").settings.alpha)) cParseInt(cMul(js("CA").settings.alpha, 100)) else 100
        )
    )
    self.alpha.setTextSize(commonApi.theme.textsize[2])
    self.alpha.setTextColor(commonApi.theme.highlightcolor)
    self.tsz.setPadding(cDp(10), cDp(10), cDp(10), cDp(10))
    self.tsz.setText(
        self.intl.resolve(
            "textsizeField",
            if (cIsFinite(js("CA").settings.textSize)) cParseInt(cMul(js("CA").settings.textSize, 100)) else 100
        )
    )
    self.tsz.setTextSize(commonApi.theme.textsize[2])
    self.tsz.setTextColor(commonApi.theme.highlightcolor)
    self.exit.setPadding(cDp(10), cDp(20), cDp(10), cDp(20))
    self.exit.setTextSize(commonApi.theme.textsize[3])
    self.exit.setTextColor(commonApi.theme.criticalcolor)
    return js("undefined")
}

private fun cChangeThemeAlphaSetting(self: dynamic): dynamic {
    val o = js("({})")
    o.max = 100
    o.progress = js("Math").floor(cMul(js("CA").settings.alpha, 100))
    o.prompt = { progress: dynamic -> self.intl.resolve("alphaField", progress) }
    o.callback = { progress: dynamic ->
        js("CA").settings.alpha = cDiv(progress, 100)
        commonApi.loadTheme(self.current)
        self.refresh()
        js("undefined")
    }
    commonApi.showSlider(o)
    return js("undefined")
}

private fun cChangeThemeTszSetting(self: dynamic): dynamic {
    val l = js("[0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1, 1.25, 1.5, 1.75, 2, 2.5, 3]")
    commonApi.showListChooser(
        l.map({ e: dynamic -> cAdd(cString(cMul(e, 100)), "%") }),
        { p: dynamic ->
            js("CA").settings.textSize = l[p]
            commonApi.loadTheme(self.current)
            self.refresh()
            js("undefined")
        }
    )
    return js("undefined")
}

private fun cShowChangeThemeImpl(self: dynamic, update: dynamic, dismiss: dynamic): dynamic {
    try {
        if (!cTruthy(self.linear)) {
            self.intl = js("Intl").getNamespace("common.ChangeTheme")
            self.adapter = { e: dynamic, _i: dynamic, _a: dynamic -> cChangeThemeAdapter(self, e) }
            self.refresh = { cChangeThemeRefresh(self); js("undefined") }
            self.alphaSetting = { cChangeThemeAlphaSetting(self); js("undefined") }
            self.tszSetting = { cChangeThemeTszSetting(self); js("undefined") }

            self.linear = cNewLinearLayout()
            self.linear.setOrientation(js("G.LinearLayout.VERTICAL"))

            self.title = cNewTextView()
            self.title.setText(self.intl.title)
            self.title.setGravity(js("G.Gravity.CENTER"))
            self.linear.addView(self.title, cNewLinearParams2(-1, -2))

            self.list = cNewListView()
            self.list.setDividerHeight(0)
            self.list.setOnItemClickListener(cItemClickListener({ _parent: dynamic, _view: dynamic, pos: dynamic, _id: dynamic ->
                try {
                    commonApi.loadTheme(_parent.getAdapter().getItem(pos))
                    self.refresh()
                } catch (e: dynamic) {
                    js("erp")(e)
                }
                false
            }))
            self.linear.addView(self.list, cNewLinearParams3(-1, 0, 1.0))

            self.exbar = cNewLinearLayout()
            self.exbar.setOrientation(js("G.LinearLayout.HORIZONTAL"))

            self.alpha = cNewTextView()
            self.alpha.setGravity(js("G.Gravity.CENTER"))
            self.alpha.setOnClickListener(cClickListener({ _v: dynamic ->
                try {
                    self.alphaSetting()
                } catch (e: dynamic) {
                    js("erp")(e)
                }
                false
            }))
            self.exbar.addView(self.alpha, cNewLinearParams3(-2, -2, 1))

            self.tsz = cNewTextView()
            self.tsz.setGravity(js("G.Gravity.CENTER"))
            self.tsz.setOnClickListener(cClickListener({ _v: dynamic ->
                try {
                    self.tszSetting()
                } catch (e: dynamic) {
                    js("erp")(e)
                }
                false
            }))
            self.exbar.addView(self.tsz, cNewLinearParams3(-2, -2, 1))
            self.linear.addView(self.exbar, cNewLinearParams2(-1, -2))

            self.exit = cNewTextView()
            self.exit.setText(commonApi.intl.ok)
            self.exit.setGravity(js("G.Gravity.CENTER"))
            self.exit.setOnClickListener(cClickListener({ _v: dynamic ->
                try {
                    if (cLooseNe(commonApi.theme.id, self.last) ||
                        cLooseNe(js("CA").settings.alpha, self.lastalpha) ||
                        cLooseNe(js("CA").settings.textSize, self.lasttsz)
                    ) {
                        self.modified = true
                        if (cTruthy(self.update)) self.update()
                    } else {
                        self.popup.exit()
                    }
                    true
                } catch (e: dynamic) {
                    js("erp")(e)
                    false
                }
            }))
            self.linear.addView(self.exit, cNewLinearParams2(-1, -2))

            self.popup = cNewPopupPage(self.linear, "common.ChangeTheme")
            js("PWM").registerResetFlag(self, "linear")
        }
        self.update = update
        self.modified = false
        self.popup.on("exit", {
            if (!cTruthy(self.modified)) commonApi.loadTheme(self.last)
            if (cTruthy(dismiss)) dismiss()
            js("undefined")
        })
        self.last = commonApi.theme.id
        self.lastalpha = js("CA").settings.alpha
        self.lasttsz = js("CA").settings.textSize
        self.refresh()
        self.popup.enter()
    } catch (e: dynamic) {
        js("erp")(e)
    }
    return js("undefined")
}

private fun cShowChangeTheme(self: dynamic, update: dynamic, dismiss: dynamic): dynamic {
    js("G").ui({ cShowChangeThemeImpl(self, update, dismiss); js("undefined") })
    return js("undefined")
}

// ---------------- 通用 UI 小函数 ----------------

private fun cCustomVMaker(holder: dynamic): dynamic {
    val view = cNewTextView()
    view.setPadding(cDp(15), cDp(15), cDp(15), cDp(15))
    view.setLayoutParams(cNewAbsParams(-1, -2))
    commonApi.applyStyle(view, "textview_default", 3)
    return view
}

private fun cInitEnterAnimation(v: dynamic): dynamic {
    if (!cTruthy(js("CA").settings.noAnimation)) {
        val trans = cNewAlphaAnimation(0, 1)
        trans.setDuration(150)
        v.startAnimation(trans)
    }
    return js("undefined")
}

private fun cShowDialog(layout: dynamic, width: dynamic, height: dynamic, onExit: dynamic, modal: dynamic): dynamic {
    val p = cShowDialog5("common.Dialog", layout, width, height, modal)
    if (cTruthy(onExit)) p.on("exit", onExit)
    return p
}

private fun cShowTextDialog(s: dynamic, onDismiss: dynamic): dynamic {
    js("G").ui({
        try {
            val layout: dynamic
            val scr: dynamic
            var text: dynamic = js("null")
            val exit: dynamic
            var popup: dynamic = js("null")
            layout = cNewLinearLayout()
            layout.setOrientation(js("G.LinearLayout.VERTICAL"))
            layout.setPadding(cDp(10), cDp(10), cDp(10), 0)
            commonApi.applyStyle(layout, "message_bg")
            scr = cNewScrollView()
            scr.setLayoutParams(cNewLinearParams3(-2, 0, 1))
            text = cNewTextView()
            text.setLayoutParams(cNewFrameParams2(-2, -2))
            text.setText(s)
            text.setPadding(cDp(10), cDp(10), cDp(10), 0)
            text.setMovementMethod(js("G.LinkMovementMethod").getInstance())
            commonApi.applyStyle(text, "textview_default", 2)
            scr.addView(text)
            layout.addView(scr)
            exit = cNewTextView()
            exit.setLayoutParams(cNewLinearParams2(-1, -2))
            exit.setText(commonApi.intl.close)
            exit.setGravity(js("G.Gravity.CENTER"))
            exit.setPadding(cDp(10), cDp(20), cDp(10), cDp(20))
            commonApi.applyStyle(exit, "button_critical", 3)
            exit.measure(0, 0)
            text.setMinWidth(exit.getMeasuredWidth())
            exit.setOnClickListener(cClickListener({ _v: dynamic ->
                try {
                    popup.exit()
                } catch (e: dynamic) {
                    js("erp")(e)
                }
                false
            }))
            layout.addView(exit)
            popup = cShowDialog4("common.TextDialog", layout, -2, -2)
            if (cTruthy(onDismiss)) popup.on("exit", onDismiss)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
    return js("undefined")
}

// ---------------- 操作菜单 / 输入框 / 确认框 / 列表选择 ----------------

private fun cOperateDialogAdapter(e: dynamic): dynamic {
    if (cIsFinite(e.gap)) {
        e.view = cNewView()
        e.view.setLayoutParams(cNewAbsParams(-1, e.gap))
        e.view.setFocusable(true)
        return e.view
    }
    e.view = cNewLinearLayout()
    e.view.setOrientation(js("G.LinearLayout.VERTICAL"))
    e.view.setPadding(cDp(15), cDp(15), cDp(15), cDp(15))
    e.view.setLayoutParams(cNewAbsParams(-1, -2))
    e._title = cNewTextView()
    e._title.setText(commonApi.toString(e.text))
    e._title.setGravity(cOr(js("G.Gravity.CENTER"), js("G.Gravity.LEFT")))
    e._title.setFocusable(false)
    e._title.setLayoutParams(cNewLinearParams2(-1, -2))
    commonApi.applyStyle(e._title, "textview_default", 2)
    e.view.addView(e._title)
    if (cTruthy(e.description)) {
        e._description = cNewTextView()
        e._description.setText(commonApi.toString(e.description))
        e._description.setPadding(0, cDp(3), 0, 0)
        e._description.setLayoutParams(cNewLinearParams2(-1, -2))
        commonApi.applyStyle(e._description, "textview_prompt", 1)
        e.view.addView(e._description)
    }
    return e.view
}

private fun cShowOperateDialogImpl(self: dynamic, s: dynamic, tag: dynamic, onDismiss: dynamic): dynamic {
    try {
        if (!cTruthy(self.adapter)) {
            self.adapter = { e: dynamic -> cOperateDialogAdapter(e) }
        }
        val items = s.filter({ e: dynamic ->
            !(cTruthy(e.hidden) && cTruthy(e.hidden(tag)))
        })
        val frame = cNewFrameLayout()
        frame.setPadding(cDp(5), cDp(5), cDp(5), cDp(5))
        commonApi.applyStyle(frame, "message_bg")
        val list = cNewListView()
        list.setLayoutParams(cNewFrameParams2(-1, -2))
        list.setDividerHeight(0)
        list.setAdapter(cNewRhinoListAdapter(items, self.adapter))
        var popup: dynamic = js("null")
        list.setOnItemClickListener(cItemClickListener({ _parent: dynamic, _view: dynamic, pos: dynamic, _id: dynamic ->
            try {
                val e = items[pos]
                if (cTruthy(e.onclick)) {
                    if (!cTruthy(e.onclick(e, tag))) popup.exit()
                }
            } catch (e: dynamic) {
                js("erp")(e)
            }
            false
        }))
        frame.addView(list)
        popup = cShowDialog4("common.OperateDialog", frame, -1, -2)
        if (cTruthy(onDismiss)) popup.on("exit", onDismiss)
    } catch (e: dynamic) {
        js("erp")(e)
    }
    return js("undefined")
}

private fun cShowOperateDialog(self: dynamic, s: dynamic, tag: dynamic, onDismiss: dynamic): dynamic {
    js("G").ui({ cShowOperateDialogImpl(self, s, tag, onDismiss); js("undefined") })
    return js("undefined")
}

private fun cInputDialogOk(s: dynamic, ret: dynamic, popup: dynamic): Boolean {
    try {
        if (cTruthy(s.callback)) {
            s.text = cString(ret.getText())
            if (cTruthy(s.callback(s.text))) return true
        }
        popup.exit()
        return true
    } catch (e: dynamic) {
        js("erp")(e)
        return false
    }
}

private fun cShowInputDialogImpl(s: dynamic): dynamic {
    try {
        val scr = cNewScrollView()
        commonApi.applyStyle(scr, "message_bg")
        val layout = cNewLinearLayout()
        layout.setOrientation(js("G.LinearLayout.VERTICAL"))
        layout.setPadding(cDp(15), cDp(15), cDp(15), 0)
        if (cTruthy(s.title)) {
            val title = cNewTextView()
            title.setText(commonApi.toString(s.title))
            title.setLayoutParams(cNewLinearParams2(-1, -2))
            title.setPadding(0, 0, 0, cDp(10))
            commonApi.applyStyle(title, "textview_default", 4)
            layout.addView(title)
        }
        if (cTruthy(s.description)) {
            val text = cNewTextView()
            text.setText(commonApi.toString(s.description))
            text.setPadding(0, 0, 0, cDp(10))
            text.setLayoutParams(cNewLinearParams2(-1, -2))
            commonApi.applyStyle(text, "textview_prompt", 2)
            layout.addView(text)
        }
        val ret = cNewEditText()
        if (cTruthy(s.defaultValue)) ret.setText(commonApi.toString(s.defaultValue))
        ret.setSingleLine(cTruthy(s.singleLine))
        if (cTruthy(s.inputType)) ret.setInputType(s.inputType)
        if (cTruthy(s.keyListener)) ret.setKeyListener(s.keyListener)
        if (cTruthy(s.transformationMethod)) ret.setTransformationMethod(s.transformationMethod)
        ret.setLayoutParams(cNewLinearParams2(-2, -2))
        ret.setSelection(ret.length())
        commonApi.applyStyle(ret, "edittext_default", 2)
        layout.addView(ret)
        commonApi.postIME(ret)

        val exit = cNewTextView()
        exit.setLayoutParams(cNewLinearParams2(-1, -2))
        exit.setText(commonApi.intl.ok)
        exit.setGravity(js("G.Gravity.CENTER"))
        exit.setPadding(cDp(10), cDp(20), cDp(10), cDp(20))
        commonApi.applyStyle(exit, "button_critical", 3)

        var popup: dynamic = js("null")
        exit.setOnClickListener(cClickListener({ _v: dynamic -> cInputDialogOk(s, ret, popup) }))
        layout.addView(exit)
        layout.addOnLayoutChangeListener(cLayoutChangeListener(
            { _v: dynamic, _l: dynamic, _t: dynamic, _r: dynamic, _b: dynamic,
              _ol: dynamic, _ot: dynamic, _or: dynamic, _ob: dynamic ->
                try {
                    ret.setMinWidth(cMul(0.5, popup.getWidth()))
                } catch (e: dynamic) {
                    js("erp")(e)
                }
                false
            }
        ))
        scr.addView(layout)
        s.text = js("null")
        popup = cShowDialog4("common.InputDialog", scr, -2, -2)
        s.dialog = popup
        if (cTruthy(s.onDismiss)) popup.on("exit", s.onDismiss)
    } catch (e: dynamic) {
        js("erp")(e)
    }
    return js("undefined")
}

private fun cShowInputDialog(s: dynamic): dynamic {
    js("G").ui({ cShowInputDialogImpl(s); js("undefined") })
    return js("undefined")
}

private fun cConfirmClick(s: dynamic, skip: dynamic, popup: dynamic, i: dynamic): dynamic {
    if (cTruthy(s.skip)) s.skip(skip.isChecked())
    if (!(cTruthy(s.callback) && cTruthy(s.callback(i)))) popup.exit()
    return js("undefined")
}

private fun cShowConfirmDialogImpl(s: dynamic): dynamic {
    try {
        val intl = js("Intl").getNamespace("common")
        val scr = cNewScrollView()
        commonApi.applyStyle(scr, "message_bg")
        val layout = cNewLinearLayout()
        layout.setLayoutParams(cNewFrameParams2(-2, -2))
        layout.setOrientation(js("G.LinearLayout.VERTICAL"))
        layout.setPadding(cDp(15), cDp(15), cDp(15), cDp(5))
        if (cTruthy(s.title)) {
            val title = cNewTextView()
            title.setText(commonApi.toString(s.title))
            title.setLayoutParams(cNewLinearParams2(-2, -2))
            title.setPadding(0, 0, 0, cDp(10))
            commonApi.applyStyle(title, "textview_default", 4)
            layout.addView(title)
        }
        if (cTruthy(s.description)) {
            val text = cNewTextView()
            text.setText(commonApi.toString(s.description))
            text.setPadding(0, 0, 0, cDp(10))
            text.setLayoutParams(cNewLinearParams2(-2, -2))
            commonApi.applyStyle(
                text,
                if (cTruthy(s.title)) "textview_prompt" else "textview_default",
                2
            )
            layout.addView(text)
        }
        var skip: dynamic = js("null")
        if (cTruthy(s.skip)) {
            skip = cNewCheckBox()
            skip.setChecked(cTruthy(s.canSkip))
            skip.setLayoutParams(cNewLinearParams3(-2, -2, 0))
            skip.getLayoutParams().setMargins(0, 0, 0, cDp(10))
            skip.setText(intl.dontAskAgain)
            layout.addView(skip)
        }
        var popup: dynamic = js("null")
        val onClick = { i: dynamic -> cConfirmClick(s, skip, popup, i) }
        val buttons = if (cTruthy(s.buttons)) s.buttons else cArrayOf(intl.ok, intl.cancel)
        buttons.map({ e: dynamic, i: dynamic ->
            val b = cNewTextView()
            b.setLayoutParams(cNewLinearParams2(-1, -2))
            b.setText(cString(e))
            b.setGravity(js("G.Gravity.CENTER"))
            b.setPadding(0, cDp(10), 0, cDp(10))
            commonApi.applyStyle(b, "button_critical", 3)
            b.setOnClickListener(cClickListener({ _v: dynamic ->
                try {
                    onClick(i)
                } catch (e: dynamic) {
                    js("erp")(e)
                }
                false
            }))
            layout.addView(b)
            b
        })
        scr.addView(layout)
        popup = cShowDialog4("common.ConfirmDialog", scr, -2, -2)
        if (cTruthy(s.onDismiss)) popup.on("exit", s.onDismiss)
    } catch (e: dynamic) {
        js("erp")(e)
    }
    return js("undefined")
}

private fun cShowConfirmDialog(s: dynamic): dynamic {
    js("G").ui({ cShowConfirmDialogImpl(s); js("undefined") })
    return js("undefined")
}

private fun cListChooserVbinder(self: dynamic, holder: dynamic, e: dynamic): dynamic {
    if (cInstanceOf(e, js("Object"))) {
        holder.title.setText(commonApi.toString(e.text))
        if (cTruthy(e.description)) {
            holder.desp.setText(commonApi.toString(e.description))
            holder.desp.setVisibility(js("G.View.VISIBLE"))
        } else {
            holder.desp.setVisibility(js("G.View.GONE"))
        }
    } else {
        holder.title.setText(commonApi.toString(e))
        holder.desp.setVisibility(js("G.View.GONE"))
    }
    return js("undefined")
}

private fun cShowListChooserImpl(self: dynamic, l: dynamic, callback: dynamic, optional: dynamic, onDismiss: dynamic): dynamic {
    try {
        if (!cTruthy(self.vmaker)) {
            self.vmaker = js("UIHelp").settingItem
            self.vbinder = { holder: dynamic, e: dynamic -> cListChooserVbinder(self, holder, e) }
        }
        if (cLength(l) == 0) {
            commonApi.toast(commonApi.intl.noneOption)
            return js("undefined")
        }
        if (cTruthy(optional) && cLength(l) == 1 && !cTruthy(callback(0, l))) return js("undefined")

        val frame = cNewFrameLayout()
        commonApi.applyStyle(frame, "message_bg")
        val list = cNewListView()
        list.setLayoutParams(cNewFrameParams2(-1, -2))
        list.setAdapter(cNewSimpleListAdapter(l, self.vmaker, self.vbinder, js("undefined")))
        var popup: dynamic = js("null")
        list.setOnItemClickListener(cItemClickListener({ _parent: dynamic, _view: dynamic, pos: dynamic, _id: dynamic ->
            try {
                if (!cTruthy(callback(pos, l))) popup.exit()
                true
            } catch (e: dynamic) {
                js("erp")(e)
                false
            }
        }))
        frame.addView(list)
        popup = cShowDialog4("common.ListChooser", frame, -1, -2)
        if (cTruthy(onDismiss)) popup.on("exit", onDismiss)
    } catch (e: dynamic) {
        js("erp")(e)
    }
    return js("undefined")
}

private fun cShowListChooser(self: dynamic, l: dynamic, callback: dynamic, optional: dynamic, onDismiss: dynamic): dynamic {
    js("G").ui({ cShowListChooserImpl(self, l, callback, optional, onDismiss); js("undefined") })
    return js("undefined")
}

// ---------------- 进度对话框 ----------------

private fun cProgressLoadAnimation(prg: dynamic): dynamic {
    prg.setImageDrawable(cNewColorDrawable(commonApi.theme.highlightcolor))
    val aset = cNewAnimationSet(false)
    val tani = cNewTranslate4(cMul(-180, js("G.dp")), cMul(180, js("G.dp")), 0, 0)
    val sani = cNewScale6(0.5, 0.3, 1, 1, cMul(120, js("G.dp")), 0)
    tani.setDuration(1500)
    tani.setRepeatMode(js("G.Animation.RESTART"))
    tani.setRepeatCount(-1)
    sani.setDuration(1000)
    sani.setRepeatMode(js("G.Animation.REVERSE"))
    sani.setRepeatCount(-1)
    aset.addAnimation(sani)
    aset.addAnimation(tani)
    prg.startAnimation(aset)
    return js("undefined")
}

private fun cProgressInit(self: dynamic, o: dynamic): dynamic {
    js("G").ui({
        try {
            val layout = cNewLinearLayout()
            layout.setOrientation(js("G.LinearLayout.VERTICAL"))
            commonApi.applyStyle(layout, "message_bg")
            val text = cNewTextView()
            o.text = text
            text.setLayoutParams(cNewFrameParams2(-2, -2))
            text.setPadding(cDp(10), cDp(10), cDp(10), cDp(10))
            commonApi.applyStyle(text, "textview_default", 2)
            layout.addView(text)
            val prg = cNewImageView()
            prg.setLayoutParams(cNewLinearParams2(-1, cDp(4)))
            self.loadAnimation(prg)
            layout.addView(prg)
            o.popup = cShowDialog5(
                "common.ProgressDialog",
                layout,
                cMul(240, js("G.dp")),
                -2,
                !cTruthy(o.onCancel)
            )
            o.popup.on("exit", {
                if (!cTruthy(o.closed)) {
                    o.cancelled = true
                    if (cIsFunction(o.onCancel)) o.onCancel()
                }
                o.closed = true
                js("undefined")
            })
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
    return js("undefined")
}

private fun cProgressSetTextInner(self: dynamic, o: dynamic, s: dynamic): dynamic {
    try {
        if (cTruthy(o.closed)) return js("undefined")
        o.currentText = s
        if (!cTruthy(o.popup)) {
            self.init(o)
        }
        o.text.setText(commonApi.toString(s))
    } catch (e: dynamic) {
        js("erp")(e)
    }
    return js("undefined")
}

private fun cProgressSetText(self: dynamic, o: dynamic, s: dynamic, uptimeMillis: dynamic): dynamic {
    val time = if (cTruthy(uptimeMillis)) uptimeMillis else js("android.os.SystemClock").uptimeMillis()
    js("gHandler").postAtTime({
        cProgressSetTextInner(self, o, s)
        js("undefined")
    }, time)
    return js("undefined")
}

private fun cProgressClose(o: dynamic, callback: dynamic): dynamic {
    js("G").ui({
        try {
            if (!cTruthy(o.closed)) {
                o.closed = true
                if (cTruthy(o.popup)) o.popup.exit()
                if (cTruthy(callback)) callback()
            }
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
    return js("undefined")
}

private fun cProgressAsync(o: dynamic, f: dynamic, onFinally: dynamic): dynamic {
    js("Threads").run({
        try {
            f(o)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        o.close(onFinally)
        js("undefined")
    })
    return js("undefined")
}

private fun cShowProgressDialog(self: dynamic, f: dynamic, onCancel: dynamic): dynamic {
    if (!cTruthy(self.loadAnimation)) {
        self.loadAnimation = { prg: dynamic -> cProgressLoadAnimation(prg) }
        self.init = { o: dynamic -> cProgressInit(self, o) }
        val controller = js("({})")
        controller.setText = cThis2Js { thisObj: dynamic, s: dynamic, uptimeMillis: dynamic ->
            cProgressSetText(self, thisObj, s, uptimeMillis)
            js("undefined")
        }
        controller.setTextDelayed = cThis2Js { thisObj: dynamic, s: dynamic, millis: dynamic ->
            thisObj.setText(s, cAdd(js("android.os.SystemClock").uptimeMillis(), millis))
            js("undefined")
        }
        controller.close = cThis1Js { thisObj: dynamic, callback: dynamic ->
            cProgressClose(thisObj, callback)
            js("undefined")
        }
        controller.async = cThis2Js { thisObj: dynamic, callback: dynamic, onFinally: dynamic ->
            cProgressAsync(thisObj, callback, onFinally)
            js("undefined")
        }
        self.controller = controller
    }
    val o = cObjectCreate(self.controller)
    o.startTime = js("android.os.SystemClock").uptimeMillis()
    o.onCancel = onCancel
    if (cTruthy(f)) o.async(f)
    return o
}

// ---------------- 滑动条 ----------------

private fun cShowSliderImpl(o: dynamic): dynamic {
    try {
        val scr = cNewScrollView()
        commonApi.applyStyle(scr, "message_bg")
        val layout = cNewLinearLayout()
        layout.setOrientation(js("G.LinearLayout.VERTICAL"))
        layout.setPadding(cDp(10), cDp(10), cDp(10), 0)
        val seekbar = cNewSeekBar()
        seekbar.setLayoutParams(cNewLinearParams2(-1, -2))
        seekbar.setMax(o.max)
        seekbar.setProgress(o.progress)
        var text: dynamic = js("null")
        seekbar.setOnSeekBarChangeListener(cSeekProgressListener(
            { _v: dynamic, progress: dynamic, _fromUser: dynamic ->
                try {
                    text.setText(o.prompt(progress))
                } catch (e: dynamic) {
                    js("erp")(e)
                }
                false
            }
        ))
        layout.addView(seekbar)
        text = cNewTextView()
        text.setLayoutParams(cNewLinearParams2(-2, -2))
        text.setPadding(cDp(10), cDp(10), cDp(10), 0)
        commonApi.applyStyle(text, "textview_default", 2)
        layout.addView(text)
        val exit = cNewTextView()
        exit.setLayoutParams(cNewLinearParams2(-1, -2))
        exit.setText(commonApi.intl.ok)
        exit.setGravity(js("G.Gravity.CENTER"))
        exit.setPadding(cDp(10), cDp(20), cDp(10), cDp(20))
        commonApi.applyStyle(exit, "button_critical", 3)
        var popup: dynamic = js("null")
        exit.setOnClickListener(cClickListener({ _v: dynamic ->
            try {
                o.callback(seekbar.getProgress())
                popup.exit()
            } catch (e: dynamic) {
                js("erp")(e)
            }
            false
        }))
        layout.addView(exit)
        text.setText(o.prompt(o.progress))
        scr.addView(layout)
        popup = cShowDialog4("common.SliderDialog", scr, -1, -2)
        if (cTruthy(o.onDismiss)) popup.on("exit", o.onDismiss)
    } catch (e: dynamic) {
        js("erp")(e)
    }
    return js("undefined")
}

private fun cShowSlider(self: dynamic, o: dynamic): dynamic {
    js("G").ui({ cShowSliderImpl(o); js("undefined") })
    return js("undefined")
}

// ---------------- 设置界面 ----------------

private fun cSettingsRefreshText(self: dynamic): dynamic {
    if (!cTruthy(self.popup.showing)) return js("undefined")
    js("G").ui({
        try {
            self.adpt.notifyChange()
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
    return js("undefined")
}

private fun cSettingsBooleanMaker(self: dynamic, holder: dynamic): dynamic {
    val hl = cNewLinearLayout()
    hl.setOrientation(js("G.LinearLayout.HORIZONTAL"))
    hl.setLayoutParams(cNewAbsParams(-1, -2))
    hl.setPadding(cDp(20), cDp(10), cDp(20), cDp(10))
    val vl = cNewLinearLayout()
    vl.setOrientation(js("G.LinearLayout.VERTICAL"))
    vl.setLayoutParams(cNewLinearParams3(-2, -2, 1.0))
    vl.getLayoutParams().gravity = js("G.Gravity.CENTER")
    holder.name = cNewTextView()
    holder.name.setSingleLine(true)
    holder.name.setLayoutParams(cNewLinearParams2(-2, -2))
    commonApi.applyStyle(holder.name, "textview_default", 3)
    vl.addView(holder.name)
    holder.description = cNewTextView()
    holder.description.setLayoutParams(cNewLinearParams2(-2, -2))
    commonApi.applyStyle(holder.description, "textview_prompt", 1)
    vl.addView(holder.description)
    hl.addView(vl)
    holder.box = cNewCheckBox()
    holder.box.setLayoutParams(cNewLinearParams3(-2, -2, 0))
    holder.box.getLayoutParams().gravity = js("G.Gravity.CENTER")
    holder.box.setOnCheckedChangeListener(cCheckedChangeListener({ _v: dynamic, s: dynamic ->
        try {
            if (!cTruthy(holder.busy)) {
                holder.e.set(s)
                if (cTruthy(holder.e.onclick)) {
                    holder.e.onclick({ self.refreshText(); js("undefined") })
                }
                holder.busy = true
                holder.box.setChecked(holder.e.get())
                holder.busy = false
            }
        } catch (e: dynamic) {
            js("erp")(e)
        }
        false
    }))
    holder.box.setFocusable(false)
    hl.addView(holder.box)
    return hl
}

private fun cSettingsBooleanBinder(self: dynamic, holder: dynamic, e: dynamic): dynamic {
    holder.e = e
    holder.busy = true
    holder.box.setChecked(e.get())
    holder.busy = false
    holder.name.setText(cString(e.name))
    if (cTruthy(e.description)) {
        holder.description.setText(cString(e.description))
        holder.description.setVisibility(js("G.View.VISIBLE"))
    } else {
        holder.description.setVisibility(js("G.View.GONE"))
    }
    return js("undefined")
}

private fun cSettingsCustomMaker(self: dynamic, holder: dynamic): dynamic {
    val hl = cNewLinearLayout()
    hl.setOrientation(js("G.LinearLayout.HORIZONTAL"))
    hl.setLayoutParams(cNewAbsParams(-1, -2))
    hl.setPadding(cDp(20), cDp(10), cDp(20), cDp(10))
    val vl = cNewLinearLayout()
    vl.setOrientation(js("G.LinearLayout.VERTICAL"))
    vl.setLayoutParams(cNewLinearParams3(-2, -2, 1.0))
    vl.getLayoutParams().gravity = js("G.Gravity.CENTER")
    holder.name = cNewTextView()
    holder.name.setSingleLine(true)
    holder.name.setLayoutParams(cNewLinearParams2(-2, -2))
    commonApi.applyStyle(holder.name, "textview_default", 3)
    vl.addView(holder.name)
    holder.description = cNewTextView()
    holder.description.setLayoutParams(cNewLinearParams2(-2, -2))
    commonApi.applyStyle(holder.description, "textview_prompt", 1)
    vl.addView(holder.description)
    hl.addView(vl)
    holder.text = cNewTextView()
    holder.text.setPadding(cDp(10), cDp(10), cDp(10), cDp(10))
    holder.text.setLayoutParams(cNewLinearParams2(-2, -2))
    holder.text.getLayoutParams().gravity = js("G.Gravity.CENTER")
    commonApi.applyStyle(holder.text, "textview_prompt", 2)
    hl.addView(holder.text)
    return hl
}

private fun cSettingsCustomBinder(self: dynamic, holder: dynamic, e: dynamic): dynamic {
    holder.e = e
    holder.text.setText(if (cTruthy(e.get)) cString(e.get()) else "")
    holder.name.setText(cString(e.name))
    if (cTruthy(e.description)) {
        holder.description.setText(cString(e.description))
        holder.description.setVisibility(js("G.View.VISIBLE"))
    } else {
        holder.description.setVisibility(js("G.View.GONE"))
    }
    return js("undefined")
}

private fun cSettingsSpaceMaker(self: dynamic, holder: dynamic): dynamic {
    holder.sp = cNewSpace()
    holder.lp = cNewAbsParams(-1, 0)
    holder.sp.setLayoutParams(holder.lp)
    holder.sp.setFocusable(true)
    return holder.sp
}

private fun cSettingsSpaceBinder(self: dynamic, holder: dynamic, e: dynamic): dynamic {
    holder.lp.height = e.height
    holder.sp.setLayoutParams(holder.lp)
    return js("undefined")
}

private fun cSettingsTagMaker(self: dynamic, holder: dynamic): dynamic {
    holder.tag = cNewTextView()
    holder.tag.setPadding(cDp(20), cDp(20), 0, 0)
    holder.tag.setLayoutParams(cNewAbsParams(-1, -2))
    holder.tag.setFocusable(true)
    commonApi.applyStyle(holder.tag, "textview_highlight", 2)
    return holder.tag
}

private fun cSettingsTagBinder(self: dynamic, holder: dynamic, e: dynamic): dynamic {
    holder.tag.setText(cString(e.name))
    return js("undefined")
}

private fun cSettingsTextMaker(self: dynamic, holder: dynamic): dynamic {
    holder.text = cNewTextView()
    holder.text.setPadding(cDp(20), 0, cDp(20), cDp(10))
    holder.text.setLayoutParams(cNewAbsParams(-1, -2))
    holder.text.setFocusable(true)
    commonApi.applyStyle(holder.text, "textview_prompt", 2)
    return holder.text
}

private fun cSettingsTextBinder(self: dynamic, holder: dynamic, e: dynamic): dynamic {
    holder.text.setText(if (cTruthy(e.get)) cString(e.get()) else cString(e.text))
    return js("undefined")
}

private fun cSettingsSeekMaker(self: dynamic, holder: dynamic): dynamic {
    val vl = cNewLinearLayout()
    vl.setOrientation(js("G.LinearLayout.VERTICAL"))
    vl.setLayoutParams(cNewAbsParams(-1, -2))
    vl.setPadding(cDp(20), cDp(10), cDp(20), cDp(10))
    val hl = cNewLinearLayout()
    hl.setOrientation(js("G.LinearLayout.HORIZONTAL"))
    hl.setLayoutParams(cNewLinearParams2(-1, -2))
    hl.setPadding(0, 0, 0, cDp(10))
    hl.getLayoutParams().gravity = js("G.Gravity.CENTER")
    holder.name = cNewTextView()
    holder.name.setLayoutParams(cNewLinearParams2(-2, -2))
    commonApi.applyStyle(holder.name, "textview_default", 3)
    hl.addView(holder.name)
    holder.progress = cNewTextView()
    holder.progress.setLayoutParams(cNewLinearParams2(-1, -1))
    holder.progress.setGravity(cOr(js("G.Gravity.CENTER"), js("G.Gravity.RIGHT")))
    holder.progress.setPadding(0, 0, cDp(10), 0)
    commonApi.applyStyle(holder.progress, "textview_prompt", 2)
    hl.addView(holder.progress)
    vl.addView(hl)
    holder.seekbar = cNewSeekBar()
    holder.seekbar.setLayoutParams(cNewLinearParams2(-1, -2))
    holder.seekbar.setOnSeekBarChangeListener(cSeekChangeListener(
        { _v: dynamic, progress: dynamic, _fromUser: dynamic ->
            try {
                holder.progress.setText(
                    if (cTruthy(holder.e.current)) holder.e.current(progress) else progress
                )
                true
            } catch (e: dynamic) {
                js("erp")(e)
                false
            }
        },
        { v: dynamic ->
            try {
                holder.e.set(v.getProgress())
                true
            } catch (e: dynamic) {
                js("erp")(e)
                false
            }
        }
    ))
    vl.addView(holder.seekbar)
    return vl
}

private fun cSettingsSeekBinder(self: dynamic, holder: dynamic, e: dynamic): dynamic {
    holder.e = e
    holder.seekbar.setMax(e.max)
    holder.seekbar.setProgress(e.get())
    holder.name.setText(cString(e.name))
    return js("undefined")
}

private fun cSettingsLayoutMaker(self: dynamic, holder: dynamic): dynamic {
    val frame = cNewFrameLayout()
    frame.setLayoutParams(cNewAbsParams(-1, -2))
    return frame
}

private fun cSettingsLayoutBinder(self: dynamic, holder: dynamic, e: dynamic): dynamic {
    if (cTruthy(e.maker) && !cTruthy(e.view)) e.view = e.maker()
    if (cTruthy(e.binder)) e.binder()
    holder.self.removeAllViews()
    if (cTruthy(e.view)) {
        val parent = e.view.getParent()
        if (cTruthy(parent)) parent.removeView(e.view)
        holder.self.addView(e.view, cNewFrameParams2(-1, -2))
    }
    return js("undefined")
}

private fun cSettingsLastValue(e: dynamic): dynamic {
    val type = e.type
    if (type == "boolean" || type == "seekbar") return e.get()
    if (type == "custom" || type == "space" || type == "tag" || type == "text") return js("null")
    return js("undefined")
}

private fun cSettingsSetData(self: dynamic, data: dynamic): dynamic {
    self.adpt.setArray(data.data)
    self.title.setText(if (cTruthy(data.title)) data.title else commonApi.intl.settings)
    self.list.setSelection(0)
    return js("undefined")
}

private fun cSettingsOnBackItem(self: dynamic, e: dynamic, i: dynamic): dynamic {
    val type = e.type
    if (type == "boolean" || type == "seekbar") {
        if (cLooseNe(e.get(), self.current.last[i]) && cTruthy(e.refresh)) e.refresh()
    } else if (type == "layout") {
        if (cTruthy(e.onExit)) e.onExit()
    }
    return js("undefined")
}

private fun cSettingsOnBack(self: dynamic): dynamic {
    self.current.data.forEach({ e: dynamic, i: dynamic ->
        cSettingsOnBackItem(self, e, i)
        js("undefined")
    })
    if (cTruthy(self.current.callback)) self.current.callback()
    self.current = self.stack.pop()
    if (cTruthy(self.current)) {
        self.setData(self.current)
    } else {
        self.popup.exit()
    }
    return js("undefined")
}

private fun cSettingsInit(self: dynamic): dynamic {
    self.refreshText = { cSettingsRefreshText(self); js("undefined") }

    val types = js("({})")
    val booleanType = js("({})")
    booleanType.maker = { holder: dynamic -> cSettingsBooleanMaker(self, holder) }
    booleanType.binder = { holder: dynamic, e: dynamic ->
        cSettingsBooleanBinder(self, holder, e)
        js("undefined")
    }
    types["boolean"] = booleanType

    val customType = js("({})")
    customType.maker = { holder: dynamic -> cSettingsCustomMaker(self, holder) }
    customType.binder = { holder: dynamic, e: dynamic ->
        cSettingsCustomBinder(self, holder, e)
        js("undefined")
    }
    types["custom"] = customType

    val spaceType = js("({})")
    spaceType.maker = { holder: dynamic -> cSettingsSpaceMaker(self, holder) }
    spaceType.binder = { holder: dynamic, e: dynamic ->
        cSettingsSpaceBinder(self, holder, e)
        js("undefined")
    }
    types["space"] = spaceType

    val tagType = js("({})")
    tagType.maker = { holder: dynamic -> cSettingsTagMaker(self, holder) }
    tagType.binder = { holder: dynamic, e: dynamic ->
        cSettingsTagBinder(self, holder, e)
        js("undefined")
    }
    types["tag"] = tagType

    val textType = js("({})")
    textType.maker = { holder: dynamic -> cSettingsTextMaker(self, holder) }
    textType.binder = { holder: dynamic, e: dynamic ->
        cSettingsTextBinder(self, holder, e)
        js("undefined")
    }
    types["text"] = textType

    val seekType = js("({})")
    seekType.maker = { holder: dynamic -> cSettingsSeekMaker(self, holder) }
    seekType.binder = { holder: dynamic, e: dynamic ->
        cSettingsSeekBinder(self, holder, e)
        js("undefined")
    }
    types["seekbar"] = seekType

    val layoutType = js("({})")
    layoutType.maker = { holder: dynamic -> cSettingsLayoutMaker(self, holder) }
    layoutType.binder = { holder: dynamic, e: dynamic ->
        cSettingsLayoutBinder(self, holder, e)
        js("undefined")
    }
    types["layout"] = layoutType
    self.adapterTypes = types

    self.setData = { data: dynamic -> cSettingsSetData(self, data); js("undefined") }
    self.onBack = { cSettingsOnBack(self); js("undefined") }
    self.adpt = cMultipleAdapterController(
        cNewMultipleListAdapter(js("[]"), self.adapterTypes, { e: dynamic -> e.type })
    )

    self.linear = cNewLinearLayout()
    self.linear.setOrientation(js("G.LinearLayout.VERTICAL"))
    commonApi.applyStyle(self.linear, "message_bg")

    self.titlebar = cNewLinearLayout()
    self.titlebar.setOrientation(js("G.LinearLayout.HORIZONTAL"))
    commonApi.applyStyle(self.titlebar, "bar_float")

    self.title = cNewTextView()
    self.title.setPadding(cDp(15), cDp(15), 0, cDp(15))
    commonApi.applyStyle(self.title, "textview_default", 4)
    self.titlebar.addView(self.title, cNewLinearParams3(0, -1, 1.0))

    self.exit = cNewTextView()
    self.exit.setText(commonApi.intl.back)
    self.exit.setGravity(js("G.Gravity.CENTER"))
    self.exit.setPadding(cDp(15), cDp(15), cDp(15), cDp(15))
    commonApi.applyStyle(self.exit, "button_critical", 3)
    self.exit.setOnClickListener(cClickListener({ _v: dynamic ->
        try {
            if (cTruthy(self.current)) self.onBack()
            true
        } catch (e: dynamic) {
            js("erp")(e)
            false
        }
    }))
    self.titlebar.addView(self.exit, cNewLinearParams2(-2, -1))
    self.linear.addView(self.titlebar, cNewLinearParams2(-1, -2))

    self.list = cNewListView()
    self.list.setDividerHeight(0)
    self.list.setAdapter(self.adpt.self)
    self.list.setOnItemClickListener(cItemClickListener(
        { _parent: dynamic, view: dynamic, pos: dynamic, _id: dynamic ->
            try {
                val e = self.adpt.array[pos]
                if (cTruthy(e)) {
                    if (e.type == "custom" || e.type == "layout") {
                        if (cTruthy(e.onclick)) {
                            e.onclick({ self.refreshText(); js("undefined") })
                        }
                    } else if (e.type == "boolean") {
                        self.adpt.getHolder(pos, view).box.performClick()
                    }
                }
            } catch (e: dynamic) {
                js("erp")(e)
            }
            false
        }
    ))
    self.linear.addView(self.list, cNewLinearParams3(-1, 0, 1.0))

    self.popup = cNewPopupPage(self.linear, "common.Settings")
    self.popup.on("back", { _name: dynamic, cancelDefault: dynamic ->
        self.onBack()
        cancelDefault()
        js("undefined")
    })
    self.popup.on("exit", {
        while (cLength(self.stack) > 0) {
            val e = self.stack.pop()
            if (cTruthy(e.callback)) e.callback()
        }
        js("undefined")
    })

    self.stack = js("[]")
    self.current = js("null")
    js("PWM").registerResetFlag(self, "linear")
    return js("undefined")
}

private fun cShowSettingsImpl(self: dynamic, title: dynamic, data: dynamic, callback: dynamic): dynamic {
    try {
        if (!cTruthy(self.linear)) cSettingsInit(self)
        val filtered = data.filter({ e: dynamic ->
            !(cTruthy(e.hidden) && cTruthy(e.hidden()))
        })
        if (cTruthy(self.current)) self.stack.push(self.current)
        self.current = js("({})")
        self.current.title = title
        self.current.data = filtered
        self.current.last = filtered.map({ e: dynamic -> cSettingsLastValue(e) })
        self.current.callback = callback
        self.setData(self.current)
        self.popup.enter()
    } catch (e: dynamic) {
        js("erp")(e)
    }
    return js("undefined")
}

private fun cShowSettings(self: dynamic, title: dynamic, data: dynamic, callback: dynamic): dynamic {
    js("G").ui({ cShowSettingsImpl(self, title, data, callback); js("undefined") })
    return js("undefined")
}

// ---------------- 文件选择 ----------------

private fun cFileDialogVmaker(): dynamic {
    val name = cNewTextView()
    name.setPadding(cDp(15), cDp(15), cDp(15), cDp(15))
    name.setSingleLine(true)
    name.setEllipsize(js("G.TextUtils.TruncateAt.END"))
    name.setLayoutParams(cNewAbsParams(-1, -2))
    return name
}

private fun cFileDialogVbinder(self: dynamic, holder: dynamic, e: dynamic): dynamic {
    if (cTruthy(e)) {
        holder.self.setText(cAdd(
            if (cTruthy(e.isDirectory())) "\uD83D\uDCC1 " else "\uD83D\uDCC4 ",
            cString(e.getName())
        ))
        commonApi.applyStyle(
            holder.self,
            if (cTruthy(e.isHidden())) "item_disabled" else "item_default",
            3
        )
    } else {
        holder.self.setText(cAdd("\uD83D\uDCC2 ", self.intl.parentDir))
        commonApi.applyStyle(holder.self, "item_default", 3)
    }
    return js("undefined")
}

private fun cFileDialogChoose(self: dynamic, e: dynamic): Boolean {
    val o = self.sets
    if (cTruthy(o.check) && !cTruthy(o.check(e))) return false
    self.popup.exit()
    o.result = e
    if (cTruthy(o.callback)) o.callback(o)
    self.lastDir = o.curdir.getAbsolutePath()
    return true
}

private fun cFileDialogRefresh(self: dynamic): dynamic {
    val o = self.sets
    val f = o.curdir.listFiles()
    val dir = js("[]")
    val fi = js("[]")
    cForIn(f, { key: dynamic ->
        val file = f[key]
        if (!(cTruthy(o.filter) && !cTruthy(o.filter(file)))) {
            if (cTruthy(file.isDirectory())) {
                dir.push(file)
            } else if (cTruthy(file.isFile())) {
                fi.push(file)
            }
        }
        js("undefined")
    })
    self.path.setText(o.curdir.getAbsolutePath())
    if (cTruthy(o.compare)) {
        dir.sort(o.compare)
        fi.sort(o.compare)
    } else {
        dir.sort(self.compare)
        fi.sort(self.compare)
    }
    val a = if (cTruthy(o.fileFirst)) fi.concat(dir) else dir.concat(fi)
    val parent = o.curdir.getParent()
    if (cTruthy(parent) && cTruthy(js("ExternalStorage").isInStorage(parent))) {
        a.unshift(js("null"))
    }
    self.curadp = cNewSimpleListAdapter(a, self.vmaker, self.vbinder, js("undefined"))
    self.list.setAdapter(self.curadp)
    return js("undefined")
}

private fun cFileDialogItemClick(self: dynamic, pos: dynamic): Boolean {
    try {
        val o = self.sets
        val e = self.curadp.getItem(pos)
        if (!cTruthy(e) || cTruthy(e.isDirectory())) {
            val newdir = if (cTruthy(e)) e else o.curdir.getParentFile()
            if (cTruthy(js("ExternalStorage").isAccessible(newdir.getAbsolutePath()))) {
                o.curdir = newdir
            } else {
                js("ExternalStorage").ensureExternalStorage({
                    o.curdir = newdir
                    self.refresh()
                    js("undefined")
                })
            }
        } else if (cLooseEq(o.type, 0)) {
            self.choose(e)
            return true
        } else if (cLooseEq(o.type, 1)) {
            self.fname.setText(e.getName())
            return true
        }
        self.refresh()
        return true
    } catch (e: dynamic) {
        js("erp")(e)
        return false
    }
}

private fun cFileDialogPathCallback(self: dynamic, o: dynamic, s: dynamic): dynamic {
    val f = cNewFile1(s)
    if (!cTruthy(f.exists())) {
        commonApi.toast(self.intl.fileNotExist)
        return js("undefined")
    }
    if (cLooseEq(o.type, 0)) {
        if (cTruthy(f.isDirectory())) {
            o.curdir = f
        } else if (cTruthy(f.isFile())) {
            self.choose(f)
        } else {
            return js("undefined")
        }
    } else if (cLooseEq(o.type, 1) || cLooseEq(o.type, 2)) {
        o.curdir = if (cTruthy(f.isDirectory())) f else f.getParentFile()
    }
    self.refresh()
    return js("undefined")
}

private fun cFileDialogPathClick(self: dynamic): Boolean {
    try {
        val o = self.sets
        val input = js("({})")
        input.title = self.intl.path
        input.callback = { s: dynamic -> cFileDialogPathCallback(self, o, s) }
        input.singleLine = true
        input.defaultValue = o.curdir.getAbsolutePath()
        commonApi.showInputDialog(input)
        return true
    } catch (e: dynamic) {
        js("erp")(e)
        return false
    }
}

private fun cFileDialogNewDirCallback(self: dynamic, s: dynamic): dynamic {
    if (cLength(s) == 0) {
        commonApi.toast(self.intl.emptyDirName)
        return js("undefined")
    }
    try {
        if (!cTruthy(cNewFile2(self.sets.curdir, s).mkdirs())) {
            commonApi.toast(self.intl.failedCreateDir)
        }
        self.refresh()
    } catch (e: dynamic) {
        commonApi.toast(self.intl.resolve("errCreateDir", e))
    }
    return js("undefined")
}

private fun cFileDialogNewDirClick(self: dynamic): Boolean {
    try {
        val input = js("({})")
        input.title = self.intl.createDir
        input.callback = { s: dynamic -> cFileDialogNewDirCallback(self, s) }
        commonApi.showInputDialog(input)
        return true
    } catch (e: dynamic) {
        js("erp")(e)
        return false
    }
}

private fun cFileDialogOkClick(self: dynamic): Boolean {
    try {
        val o = self.sets
        if (cLooseEq(o.type, 1)) {
            val fname = cString(self.fname.getText())
            if (cLength(fname) == 0) {
                commonApi.toast(self.intl.emptyFileName)
                return true
            }
            var e = cNewFile2(o.curdir, fname)
            if (!cTruthy(e.getParentFile().exists())) {
                e = cNewFile1(fname)
                if (!cTruthy(e.getParentFile().exists())) {
                    commonApi.toast(self.intl.invaildFileName)
                    return true
                }
            }
            if (cTruthy(e.exists()) && !cTruthy(e.isFile())) {
                commonApi.toast(self.intl.dirAlreadyExist)
                return true
            }
            self.choose(e)
        } else if (cLooseEq(o.type, 2)) {
            self.choose(o.curdir)
        }
        return true
    } catch (e: dynamic) {
        js("erp")(e)
        return false
    }
}

private fun cFileDialogBuild(self: dynamic): dynamic {
    self.intl = js("Intl").getNamespace("common.FileChooser")
    self.vmaker = { cFileDialogVmaker() }
    self.vbinder = { holder: dynamic, e: dynamic -> cFileDialogVbinder(self, holder, e) }
    if (js("MapScript").host == "AutoJs") {
        self.compare = { a: dynamic, b: dynamic ->
            val an = cLower(a.getName())
            val bn = cLower(b.getName())
            if (an == bn) 0 else if (an > bn) 1 else -1
        }
    } else {
        self.compare = { a: dynamic, b: dynamic -> a.getName().compareToIgnoreCase(b.getName()) }
    }
    self.choose = { e: dynamic -> cFileDialogChoose(self, e) }
    self.refresh = { cFileDialogRefresh(self); js("undefined") }

    self.linear = cNewLinearLayout()
    self.linear.setOrientation(js("G.LinearLayout.VERTICAL"))

    self.header = cNewLinearLayout()
    self.header.setOrientation(js("G.LinearLayout.HORIZONTAL"))
    commonApi.applyStyle(self.header, "bar_float")

    self.back = cNewTextView()
    self.back.setText(cAdd("< ", commonApi.intl.back))
    self.back.setGravity(js("G.Gravity.CENTER"))
    self.back.setPadding(cDp(20), 0, cDp(20), 0)
    commonApi.applyStyle(self.back, "button_highlight", 2)
    self.back.setOnClickListener(cClickListener({ _v: dynamic ->
        try {
            self.popup.exit()
            true
        } catch (e: dynamic) {
            js("erp")(e)
            false
        }
    }))
    self.header.addView(self.back, cNewLinearParams2(-2, -1))

    self.title = cNewTextView()
    self.title.setPadding(0, cDp(10), 0, cDp(10))
    commonApi.applyStyle(self.title, "textview_default", 4)
    self.header.addView(self.title, cNewLinearParams2(-2, -2))

    self.path = cNewTextView()
    self.path.setGravity(cOr(js("G.Gravity.CENTER"), js("G.Gravity.LEFT")))
    self.path.setPadding(cDp(15), 0, cDp(5), 0)
    self.path.setSingleLine(true)
    self.path.setEllipsize(js("G.TextUtils.TruncateAt.START"))
    commonApi.applyStyle(self.path, "textview_prompt", 2)
    self.path.setOnClickListener(cClickListener({ _v: dynamic -> cFileDialogPathClick(self) }))
    self.header.addView(self.path, cNewLinearParams3(0, -1, 1.0))

    self.newDir = cNewTextView()
    self.newDir.setText("\uD83D\uDCC1+")
    self.newDir.setGravity(js("G.Gravity.CENTER"))
    self.newDir.setPadding(cDp(20), 0, cDp(20), 0)
    commonApi.applyStyle(self.newDir, "button_default", 2)
    self.newDir.setOnClickListener(cClickListener({ _v: dynamic -> cFileDialogNewDirClick(self) }))
    self.header.addView(self.newDir, cNewLinearParams2(-2, -1))
    self.linear.addView(self.header, cNewLinearParams2(-1, -2))

    self.list = cNewListView()
    commonApi.applyStyle(self.list, "message_bg")
    self.list.setOnItemClickListener(cItemClickListener(
        { _parent: dynamic, _view: dynamic, pos: dynamic, _id: dynamic ->
            cFileDialogItemClick(self, pos)
        }
    ))
    if (js("G.style") == "Material") {
        self.list.setFastScrollEnabled(true)
        self.list.setFastScrollAlwaysVisible(false)
    }
    self.linear.addView(self.list, cNewLinearParams3(-1, 0, 1.0))

    self.inputbar = cNewLinearLayout()
    self.inputbar.setOrientation(js("G.LinearLayout.HORIZONTAL"))
    commonApi.applyStyle(self.inputbar, "bar_float")

    self.fname = cNewEditText()
    self.fname.setHint(self.intl.fileName)
    self.fname.setSingleLine(true)
    self.fname.setGravity(cOr(js("G.Gravity.LEFT"), js("G.Gravity.CENTER")))
    self.fname.setInputType(js("G.InputType.TYPE_CLASS_TEXT"))
    self.fname.setPadding(cDp(10), cDp(10), cDp(10), cDp(10))
    commonApi.applyStyle(self.fname, "edittext_default", 3)
    self.inputbar.addView(self.fname, cNewLinearParams3(0, -1, 4.0))

    self.exit = cNewTextView()
    self.exit.setText(commonApi.intl.ok)
    self.exit.setGravity(js("G.Gravity.CENTER"))
    self.exit.setPadding(cDp(10), cDp(10), cDp(10), cDp(10))
    commonApi.applyStyle(self.exit, "button_critical", 3)
    self.exit.setOnClickListener(cClickListener({ _v: dynamic -> cFileDialogOkClick(self) }))
    self.inputbar.addView(self.exit, cNewLinearParams3(0, -2, 1.0))
    self.linear.addView(self.inputbar, cNewLinearParams2(-1, -2))

    self.popup = cNewPopupPage(self.linear, "common.FileChooser")
    js("PWM").registerResetFlag(self, "linear")
    return js("undefined")
}

private fun cShowFileDialogImpl(self: dynamic, o: dynamic): dynamic {
    try {
        if (!cTruthy(self.linear)) cFileDialogBuild(self)
        if (cTruthy(o.onDismiss)) self.popup.on("exit", o.onDismiss)
        self.sets = o
        try {
            o.curdir = cNewFile1(cString(if (cTruthy(o.initDir)) o.initDir else self.lastDir))
            if (!cTruthy(o.curdir.isDirectory()) ||
                !cTruthy(js("ExternalStorage").isAccessible(o.curdir.getAbsolutePath()))
            ) {
                o.curdir = js("ExternalStorage").getAccessibleRoot()
            }
            self.refresh()
        } catch (e: dynamic) {
            commonApi.toast(self.intl.resolve("errAccessDir", e))
            return js("undefined")
        }
        self.title.setText(commonApi.toString(
            if (cTruthy(o.title)) o.title else self.intl.defaultTitle
        ))
        val type = o.type
        if (cLooseEq(type, 1)) { // 新建文件（保存）
            self.exit.setVisibility(js("G.View.VISIBLE"))
            self.fname.setVisibility(js("G.View.VISIBLE"))
            self.fname.setText(cString(if (cTruthy(o.defaultFileName)) o.defaultFileName else ""))
        } else if (cLooseEq(type, 2)) { // 选择目录（打开）
            self.exit.setVisibility(js("G.View.VISIBLE"))
            self.fname.setVisibility(js("G.View.GONE"))
        } else { // 选择文件（打开）
            o.type = 0
            self.exit.setVisibility(js("G.View.GONE"))
            self.fname.setVisibility(js("G.View.GONE"))
        }
        self.popup.enter()
    } catch (e: dynamic) {
        js("erp")(e)
    }
    return js("undefined")
}

private fun cShowFileDialog(self: dynamic, o: dynamic): dynamic {
    js("G").ui({ cShowFileDialogImpl(self, o); js("undefined") })
    return js("undefined")
}

// ---------------- WebView 对话框 ----------------

private fun cShowWebViewDialogImpl(s: dynamic): dynamic {
    try {
        val layout = cNewLinearLayout()
        layout.setOrientation(js("G.LinearLayout.VERTICAL"))
        layout.setPadding(cDp(10), cDp(10), cDp(10), 0)
        commonApi.applyStyle(layout, "message_bg")
        val wv = commonApi.createWebView({ webView: dynamic ->
            if (cTruthy(s.url) && cTruthy(s.code)) {
                webView.loadDataWithBaseURL(
                    cString(s.url),
                    cString(s.code),
                    if (cTruthy(s.mimeType)) cString(s.mimeType) else js("null"),
                    js("null"),
                    js("null")
                )
            } else if (cTruthy(s.code)) {
                webView.loadData(
                    cString(s.code),
                    if (cTruthy(s.mimeType)) cString(s.mimeType) else js("null"),
                    js("null")
                )
            } else if (cTruthy(s.url)) {
                webView.loadUrl(cString(s.url))
            } else {
                webView.loadUrl("about:blank")
            }
            js("undefined")
        })
        wv.setLayoutParams(cNewLinearParams3(-1, 0, 1.0))
        layout.addView(wv)
        val exit = cNewTextView()
        exit.setLayoutParams(cNewLinearParams2(-1, -2))
        exit.setText(commonApi.intl.close)
        exit.setGravity(js("G.Gravity.CENTER"))
        exit.setPadding(cDp(10), cDp(20), cDp(10), cDp(20))
        commonApi.applyStyle(exit, "button_critical", 3)
        var popup: dynamic = js("null")
        exit.setOnClickListener(cClickListener({ _v: dynamic ->
            try {
                popup.exit()
                true
            } catch (e: dynamic) {
                js("erp")(e)
                false
            }
        }))
        layout.addView(exit)
        popup = cShowDialog4("common.WebDialog", layout, -1, -1)
        popup.on("exit", { commonApi.destroyWebView(wv); js("undefined") })
    } catch (e: dynamic) {
        js("erp")(e)
    }
    return js("undefined")
}

private fun cShowWebViewDialog(s: dynamic): dynamic {
    js("G").ui({ cShowWebViewDialogImpl(s); js("undefined") })
    return js("undefined")
}

// ---------------- 排序对话框 ----------------

private fun cTryExchange(params: dynamic, from: dynamic, to: dynamic): Boolean {
    if (cTruthy(params.canExchange(params.array, from, to))) {
        commonApi.exchangeProperty(params.array, from, to)
        params.adpt.notifyChange()
        return true
    }
    return false
}

private fun cShowSortDialogImpl(self: dynamic, o: dynamic): dynamic {
    try {
        if (!cTruthy(self.vmaker)) {
            self.vmaker = js("UIHelp").settingItem
            self.vbinder = {
                    holder: dynamic, e: dynamic, i: dynamic, _a: dynamic, params: dynamic ->
                val title = params.getTitle(e)
                val desp = params.getDescription(e)
                holder.title.setText(commonApi.toString(title))
                if (cTruthy(desp)) {
                    holder.desp.setText(commonApi.toString(desp))
                    holder.desp.setVisibility(js("G.View.VISIBLE"))
                } else {
                    holder.desp.setVisibility(js("G.View.GONE"))
                }
                commonApi.applyStyle(
                    holder.title,
                    if (cLooseEq(params.selectIndex, i)) "item_highlight" else "item_default",
                    2
                )
                js("undefined")
            }
            self.tryExchange = { params: dynamic, from: dynamic, to: dynamic ->
                cTryExchange(params, from, to)
            }
        }
        val params = js("({})")
        params.selectIndex = cParseInt(o.selectIndex)
        params.array = if (cIsArray(o.array)) o.array else js("[]")
        params.getTitle = if (cTruthy(o.getTitle)) o.getTitle else { e: dynamic -> e.title }
        params.getDescription = if (cTruthy(o.getDescription)) o.getDescription else { e: dynamic -> e.description }
        params.canExchange = if (cTruthy(o.canExchange)) o.canExchange else {
                _array: dynamic, _from: dynamic, _to: dynamic -> true
            }
        params.adpt = cSimpleAdapterController(
            cNewSimpleListAdapter(params.array, self.vmaker, self.vbinder, params)
        )

        val layout = cNewLinearLayout()
        layout.setOrientation(js("G.LinearLayout.HORIZONTAL"))
        commonApi.applyStyle(layout, "message_bg")

        val list = cNewListView()
        list.setAdapter(params.adpt.self)
        list.setLayoutParams(cNewLinearParams3(0, -1, 1.0))
        list.setOnItemClickListener(cItemClickListener(
            { _parent: dynamic, _view: dynamic, pos: dynamic, _id: dynamic ->
                try {
                    params.selectIndex = pos
                    params.adpt.notifyChange()
                } catch (e: dynamic) {
                    js("erp")(e)
                }
                false
            }
        ))
        layout.addView(list)

        val right = cNewLinearLayout()
        right.setOrientation(js("G.LinearLayout.VERTICAL"))
        right.setLayoutParams(cNewLinearParams2(-2, -1))
        commonApi.applyStyle(right, "bar_float_second")

        val up = cNewTextView()
        up.setLayoutParams(cNewLinearParams3(-1, 0, 2.0))
        up.setText("\u25B2")
        up.setGravity(js("G.Gravity.BOTTOM"))
        up.setPadding(cDp(15), cDp(15), cDp(15), cDp(15))
        commonApi.applyStyle(up, "button_highlight", 4)
        up.setOnClickListener(cClickListener({ _v: dynamic ->
            try {
                if (params.selectIndex > 0) {
                    if (self.tryExchange(params, params.selectIndex - 1, params.selectIndex)) {
                        params.selectIndex--
                    }
                }
            } catch (e: dynamic) {
                js("erp")(e)
            }
            false
        }))
        right.addView(up)

        val down = cNewTextView()
        down.setLayoutParams(cNewLinearParams3(-1, 0, 2.0))
        down.setText("\u25BC")
        down.setGravity(js("G.Gravity.TOP"))
        down.setPadding(cDp(15), cDp(15), cDp(15), cDp(15))
        commonApi.applyStyle(down, "button_highlight", 4)
        down.setOnClickListener(cClickListener({ _v: dynamic ->
            try {
                if (params.selectIndex < cLength(params.array) - 1) {
                    if (self.tryExchange(params, params.selectIndex, params.selectIndex + 1)) {
                        params.selectIndex++
                    }
                }
            } catch (e: dynamic) {
                js("erp")(e)
            }
            false
        }))
        right.addView(down)

        val exit = cNewTextView()
        exit.setLayoutParams(cNewLinearParams3(-1, 0, 1.0))
        exit.setText("\u00D7")
        exit.setGravity(js("G.Gravity.CENTER"))
        exit.setPadding(cDp(15), cDp(15), cDp(15), cDp(15))
        commonApi.applyStyle(exit, "button_critical", 4)
        var popup: dynamic = js("null")
        exit.setOnClickListener(cClickListener({ _v: dynamic ->
            try {
                popup.exit()
                true
            } catch (e: dynamic) {
                js("erp")(e)
                false
            }
        }))
        right.addView(exit)
        layout.addView(right)
        popup = cShowDialog4("common.SortDialog", layout, -1, -1)
        o.dialog = popup
        if (cTruthy(o.callback)) {
            popup.on("exit", { o.callback(params.array); js("undefined") })
        }
    } catch (e: dynamic) {
        js("erp")(e)
    }
    return js("undefined")
}

private fun cShowSortDialog(self: dynamic, o: dynamic): dynamic {
    js("G").ui({ cShowSortDialogImpl(self, o); js("undefined") })
    return js("undefined")
}

// ---------------- 教程遮罩 ----------------

private fun cTutorialDraw(self: dynamic, o: dynamic): dynamic {
    self.cv.drawColor(commonApi.argbInt(0xa0, 0, 0, 0), js("G.PorterDuff.Mode.SRC"))
    if (cTruthy(o.view)) {
        o.rect = cNewRect()
        o.view.getGlobalVisibleRect(o.rect)
        if (cTruthy(o.offset)) {
            o.rect.offset(
                o.offset[0] - self.frameRect.left,
                o.offset[1] - self.frameRect.top
            )
        }
    }
    if (cTruthy(o.rect)) {
        self.cv.drawRect(o.rect, self.paint)
    }
    return js("undefined")
}

private fun cTutorialTouch(self: dynamic, event: dynamic): Boolean {
    try {
        if (cLooseEq(event.getAction(), event.ACTION_DOWN)) {
            if (cTruthy(self.current.onDismiss)) self.current.onDismiss()
            self.next()
        }
        return true
    } catch (e: dynamic) {
        js("erp")(e)
        return true
    }
}

private fun cTutorialNext(self: dynamic): dynamic {
    if (!cTruthy(self.popup.isShowing())) {
        val decor = js("ctx").getWindow().getDecorView()
        decor.getWindowVisibleDisplayFrame(self.frameRect)
        if (cTruthy(self.bmp)) self.bmp.recycle()
        self.bmp = js("G.Bitmap").createBitmap(
            self.frameRect.width(),
            self.frameRect.height(),
            js("G.Bitmap.Config.ARGB_8888")
        )
        self.frame.setBackground(cNewBitmapDrawable(self.bmp))
        self.cv.setBitmap(self.bmp)
        self.popup.showAtLocation(
            decor,
            cOr(js("G.Gravity.LEFT"), js("G.Gravity.TOP")),
            0,
            0
        )
        js("PWM").addFloat(self.popup)
    }
    if (cLength(self.queue) == 0) {
        self.popup.dismiss()
        return js("undefined")
    }
    self.current = self.queue.shift()
    if (cTruthy(self.current.callback)) self.current.callback()
    self.text.setText(if (cTruthy(self.current.text)) self.current.text else "")
    self.draw(self.current)
    return js("undefined")
}

private fun cTutorialBuild(self: dynamic): dynamic {
    self.queue = js("[]")
    self.next = { cTutorialNext(self); js("undefined") }
    self.draw = { o: dynamic -> cTutorialDraw(self, o); js("undefined") }

    self.paint = cNewPaint()
    self.paint.setStyle(js("G.Paint.Style.FILL"))
    js("IntColor").Paint.setColor(self.paint, js("G.Color.WHITE"))
    self.paint.setAntiAlias(true)
    self.paint.setXfermode(cNewPorterDuffXfermode(js("G.PorterDuff.Mode.DST_OUT")))
    self.cv = cNewCanvas()
    self.frameRect = cNewRect()
    self.frame = cNewFrameLayout()
    self.frame.setOnTouchListener(cTouchListener({ _view: dynamic, event: dynamic ->
        cTutorialTouch(self, event)
    }))
    self.text = cNewTextView()
    self.text.setBackgroundColor(commonApi.argbInt(0x80, 0, 0, 0))
    self.text.setTextColor(js("G.Color.WHITE"))
    self.text.setTextSize(16)
    self.text.setGravity(js("G.Gravity.CENTER"))
    self.text.setPadding(cDp(15), cDp(15), cDp(15), cDp(15))
    self.text.setLayoutParams(cNewFrameParams3(-2, -2, js("G.Gravity.CENTER")))
    self.frame.addView(self.text)
    self.popup = cNewPopupWindow(self.frame, -1, -1)
    commonApi.applyPopup(self.popup)
    self.popup.setFocusable(true)
    self.popup.setBackgroundDrawable(cNewColorDrawable(js("G.Color.TRANSPARENT")))
    self.popup.setOnDismissListener(cDismissListener({
        try {
            self.bmp.recycle()
            self.bmp = js("null")
            if (cTruthy(self.current.onDismiss)) self.current.onDismiss()
            self.queue.splice(0, cLength(self.queue)).forEach({ e: dynamic ->
                if (cTruthy(e.onDismiss)) e.onDismiss()
                js("undefined")
            })
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }))
    return js("undefined")
}

private fun cShowTutorial(self: dynamic, o: dynamic): dynamic {
    js("gHandler").post({
        try {
            if (!cTruthy(self.popup)) cTutorialBuild(self)
            self.queue.push(o)
            if (!cTruthy(self.popup.isShowing())) self.next()
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
    return js("undefined")
}

// ---------------- Toast ----------------

private fun cShowToast(self: dynamic, str: dynamic): dynamic {
    js("G").ui({
        try {
            if (!cTruthy(self.frame)) {
                self.show = {
                    if (!cTruthy(self.overlay)) {
                        self.overlay = cNewOverlay(
                            self.frame,
                            -2,
                            -2,
                            cOr(js("G.Gravity.CENTER_HORIZONTAL"), js("G.Gravity.BOTTOM"))
                        )
                        js("PopupPage").addOverlay(self.overlay)
                    }
                    if (!cTruthy(js("CA").settings.noAnimation)) {
                        val animation = cNewTranslate4(
                            0,
                            0,
                            cAdd(cDp(8), self.text.getHeight()),
                            0
                        )
                        animation.setInterpolator(cNewDecelerate(1.0))
                        animation.setDuration(100)
                        self.text.startAnimation(animation)
                    }
                    js("undefined")
                }
                self.hide = {
                    if (cTruthy(self.overlay)) {
                        if (cTruthy(js("CA").settings.noAnimation)) {
                            js("PopupPage").removeOverlay(self.overlay)
                            self.overlay = js("null")
                        } else {
                            val animation = cNewTranslate4(
                                0,
                                0,
                                0,
                                cAdd(cDp(8), self.text.getHeight())
                            )
                            animation.setInterpolator(cNewAccelerate(2.0))
                            animation.setDuration(100)
                            animation.setFillEnabled(true)
                            animation.setFillAfter(true)
                            self.text.startAnimation(animation)
                            self.lastCbk = cNewRunnable({
                                try {
                                    self.lastCbk = js("null")
                                    js("PopupPage").removeOverlay(self.overlay)
                                    self.overlay = js("null")
                                } catch (e: dynamic) {
                                    js("erp")(e)
                                }
                                js("undefined")
                            })
                            js("gHandler").postDelayed(self.lastCbk, 150)
                        }
                    }
                    js("undefined")
                }
                self.flash = {
                    val animation = cNewTranslate4(0, cDp(8), 0, 0)
                    animation.setInterpolator(cNewCycle(2))
                    animation.setDuration(300)
                    self.text.startAnimation(animation)
                    js("undefined")
                }
                self.toast = { s: dynamic ->
                    if (cTruthy(self.lastCbk)) js("gHandler").removeCallbacks(self.lastCbk)
                    self.text.clearAnimation()
                    if (!cTruthy(self.overlay)) self.show()
                    self.text.setText(commonApi.toString(s))
                    if (cTruthy(cLooseEq(self.lastToast, s))) self.flash()
                    self.lastToast = s
                    self.lastCbk = cNewRunnable({
                        try {
                            self.lastCbk = js("null")
                            self.hide()
                            self.lastToast = ""
                        } catch (e: dynamic) {
                            js("erp")(e)
                        }
                        js("undefined")
                    })
                    js("gHandler").postDelayed(self.lastCbk, 2000)
                    js("undefined")
                }
                self.frame = cNewFrameLayout()
                self.text = cNewTextView()
                self.text.setBackgroundColor(commonApi.argbInt(0xc0, 0, 0, 0))
                self.text.setTextColor(js("G.Color.WHITE"))
                self.text.setTextSize(14)
                self.text.setGravity(js("G.Gravity.CENTER"))
                self.text.setPadding(cDp(10), cDp(10), cDp(10), cDp(10))
                self.text.setLayoutParams(cNewFrameParams3(
                    -2,
                    -2,
                    cOr(js("G.Gravity.CENTER_HORIZONTAL"), js("G.Gravity.BOTTOM"))
                ))
                self.text.getLayoutParams().setMargins(cDp(8), cDp(8), cDp(8), cDp(8))
                self.frame.addView(self.text)
                if (js("G.style") == "Material") self.text.setElevation(cDp(8))
            }
            self.toast(str)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
    return js("undefined")
}

// ---------------- WebView 创建 / 销毁 ----------------

private fun cWebViewConsole(message: dynamic): Boolean {
    try {
        val o = js("({})")
        o.message = message.message()
        o.lineNumber = message.lineNumber()
        o.sourceId = message.sourceId()
        o.messageLevel = cString(message.messageLevel())
        js("Log").d(cAdd("[WebView]", cJsonStringify(o, js("null"), "\t")))
        return true
    } catch (e: dynamic) {
        js("erp")(e)
        return false
    }
}

private fun cWebViewAlert(message: dynamic, result: dynamic): Boolean {
    try {
        commonApi.showTextDialog(message, { result.confirm(); js("undefined") })
        return true
    } catch (e: dynamic) {
        js("erp")(e)
        return false
    }
}

private fun cWebViewConfirm(url: dynamic, message: dynamic, result: dynamic): Boolean {
    try {
        var resumed = false
        val o = js("({})")
        o.title = url
        o.description = message
        o.callback = { id: dynamic ->
            if (cLooseEq(id, 0)) {
                result.confirm()
            } else {
                result.cancel()
            }
            resumed = true
            js("undefined")
        }
        o.onDismiss = {
            if (!resumed) {
                result.cancel()
                resumed = true
            }
            js("undefined")
        }
        commonApi.showConfirmDialog(o)
        return true
    } catch (e: dynamic) {
        js("erp")(e)
        return false
    }
}

private fun cWebViewPrompt(
    url: dynamic,
    message: dynamic,
    defaultValue: dynamic,
    result: dynamic
): Boolean {
    try {
        var resumed = false
        val o = js("({})")
        o.title = url
        o.description = message
        o.defaultValue = defaultValue
        o.callback = { s: dynamic ->
            result.confirm(s)
            resumed = true
            js("undefined")
        }
        o.onDismiss = {
            if (!resumed) {
                result.confirm("")
                resumed = true
            }
            js("undefined")
        }
        commonApi.showInputDialog(o)
        return true
    } catch (e: dynamic) {
        js("erp")(e)
        return false
    }
}

private fun cCreateWebViewImpl(callback: dynamic, oldWebView: dynamic): dynamic {
    var result: dynamic = js("null")
    var error: dynamic = js("null")
    try {
        if (cInstanceOf(oldWebView, js("G.WebView"))) {
            commonApi.destroyWebView(oldWebView)
        }
        if (js("typeof ScriptInterface !== 'undefined'")) {
            val o = js("({})")
            o.onConsoleMessage = { _wv: dynamic, message: dynamic -> cWebViewConsole(message) }
            o.onCreateWindow = { _wv: dynamic, _isDialog: dynamic, _isUserGesture: dynamic, _resultMsg: dynamic ->
                false
            }
            o.onJsAlert = { _wv: dynamic, _url: dynamic, message: dynamic, resultArg: dynamic ->
                cWebViewAlert(message, resultArg)
            }
            o.onJsBeforeUnload = { _wv: dynamic, _url: dynamic, _message: dynamic, _result: dynamic ->
                false
            }
            o.onJsConfirm = { _wv: dynamic, url: dynamic, message: dynamic, resultArg: dynamic ->
                cWebViewConfirm(url, message, resultArg)
            }
            o.onJsPrompt = { _wv: dynamic, url: dynamic, message: dynamic, defaultValue: dynamic, resultArg: dynamic ->
                cWebViewPrompt(url, message, defaultValue, resultArg)
            }
            o.onShowFileChooser = { _wv: dynamic, _filePathCallback: dynamic, _fileChooserParams: dynamic ->
                false
            }
            result = cCreateWebView(o)
        } else {
            result = cNewWebView()
        }
        val ws = result.getSettings()
        ws.setLoadWithOverviewMode(true)
        ws.setBlockNetworkImage(cTruthy(js("CA").settings.noWebImage))
        ws.setDatabaseEnabled(true)
        ws.setDomStorageEnabled(true)
        ws.setJavaScriptEnabled(true)
        ws.setJavaScriptCanOpenWindowsAutomatically(true)
        callback(result, ws)
        return result
    } catch (e: dynamic) {
        js("erp")(e, true)
        error = e
    }
    result = cNewTextView()
    result.setBackgroundColor(js("G.Color.WHITE"))
    result.setTextColor(js("G.Color.BLACK"))
    result.setGravity(js("G.Gravity.CENTER"))
    result.setPadding(cDp(10), cDp(10), cDp(10), cDp(10))
    result.setText(commonApi.intl.resolve("webviewUnavailable", error))
    return result
}

private fun cDestroyWebView(wv: dynamic): dynamic {
    if (cInstanceOf(wv, js("G.WebView"))) {
        wv.clearHistory()
        wv.loadUrl("about:blank")
        val parentView = wv.getParent()
        if (cTruthy(parentView)) parentView.removeView(wv)
        wv.destroy()
    }
    return js("undefined")
}

// ---------------- 文件读写 ----------------

private fun cFileCopy(src: dynamic, dest: dynamic): dynamic {
    val bufferSize = 8192
    val fd = (if (cInstanceOf(dest, js("java.io.File"))) dest else cNewFile1(dest)).getParentFile()
    if (cTruthy(fd)) fd.mkdirs()
    val fi = cNewFileInputStream(src)
    val fo = cNewFileOutputStream(dest)
    val buf = cNewByteArray(bufferSize)
    var hr = fi.read(buf)
    while (hr > 0) {
        fo.write(buf, 0, hr)
        hr = fi.read(buf)
    }
    fi.close()
    fo.close()
    return js("undefined")
}

private fun cReadFile(path: dynamic, defaultValue: dynamic, gzipped: dynamic, error: dynamic): dynamic {
    try {
        val s = js("[]")
        val rd = if (cTruthy(gzipped)) {
            cNewBufferedReader(
                cNewInputStreamReader(cNewGzipInputStream(cNewFileInputStream(path)))
            )
        } else {
            cNewBufferedReader(cNewFileReader(path))
        }
        var q = rd.readLine()
        while (cTruthy(q)) {
            s.push(q)
            q = rd.readLine()
        }
        rd.close()
        return s.join("\n")
    } catch (e: dynamic) {
        if (cTruthy(error)) error.error = e
        return defaultValue
    }
}

private fun cSaveFile(path: dynamic, text: dynamic, gzipped: dynamic): dynamic {
    val f = cNewFile1(path).getParentFile()
    if (cTruthy(f)) f.mkdirs()
    val wr = if (cTruthy(gzipped)) {
        cNewGzipOutputStream(cNewFileOutputStream(path))
    } else {
        cNewFileOutputStream(path)
    }
    wr.write(cStringBytes(text))
    wr.close()
    return js("undefined")
}

private fun cGetFileSize(f: dynamic, longer: dynamic): dynamic {
    val l = f.length()
    if (cTruthy(longer)) {
        return cString(cFormatFileSize(js("ctx"), l, true))
    }
    return cString(cFormatFileSize(js("ctx"), l, false))
}

private fun cDeleteFile(path: dynamic): dynamic {
    cNewFile1(path).delete()
    return js("undefined")
}

private fun cToastSystem(self: dynamic, s: dynamic, dur: dynamic): dynamic {
    js("G").ui({
        try {
            if (cTruthy(self.last)) self.last.cancel()
            self.last = js("G.Toast").makeText(
                js("ctx"),
                commonApi.toString(s),
                if (cTruthy(dur)) 1 else 0
            )
            self.last.show()
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
    return js("undefined")
}

private fun cPostIME(v: dynamic, delay: dynamic): dynamic {
    v.postDelayed({
        try {
            v.requestFocus()
            js("ctx").getSystemService(js("ctx").INPUT_METHOD_SERVICE).showSoftInput(
                v,
                js("G.InputMethodManager.SHOW_IMPLICIT")
            )
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }, if (cIsNaN(delay)) 0 else delay)
    return js("undefined")
}

private fun cHideIME(v: dynamic): dynamic {
    val imm = js("ctx").getSystemService(js("ctx").INPUT_METHOD_SERVICE)
    if (cTruthy(v)) {
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0)
    } else if (cTruthy(imm.isActive())) {
        imm.toggleSoftInput(0, imm.HIDE_NOT_ALWAYS)
    }
    return js("undefined")
}

private fun cHasClipboardText(): dynamic {
    return js("ctx").getSystemService(js("ctx").CLIPBOARD_SERVICE).hasPrimaryClip()
}

private fun cGetClipboardText(): dynamic {
    val clip = js("ctx").getSystemService(js("ctx").CLIPBOARD_SERVICE).getPrimaryClip()
    if (!cTruthy(clip)) return js("null")
    return clip.getItemAt(0).coerceToText(js("ctx"))
}

private fun cSetClipboardText(text: dynamic): dynamic {
    try {
        js("ctx").getSystemService(js("ctx").CLIPBOARD_SERVICE)
            .setPrimaryClip(cNewClipData(text))
    } catch (e: dynamic) {
        js("Log").e(e)
        commonApi.toast(
            cAdd("无法复制指定文本到剪贴板，请检查您是否授权命令助手修改剪贴板的权限\n", e)
        )
    }
    return js("undefined")
}

private fun cGetMetrics(): dynamic {
    val display = js("ctx").getSystemService(js("ctx").WINDOW_SERVICE).getDefaultDisplay()
    val out = cNewDisplayMetrics()
    display.getMetrics(out)
    val r = cArrayOf(out.widthPixels, out.heightPixels)
    var rot = js("ctx").getResources().getConfiguration().orientation
    if (cLooseEq(rot, js("android.content.res.Configuration.ORIENTATION_LANDSCAPE"))) {
        r.reverse()
    }
    rot = display.getRotation()
    if (cLooseEq(rot, js("G.Surface.ROTATION_90")) ||
        cLooseEq(rot, js("G.Surface.ROTATION_270"))
    ) {
        r.reverse()
    }
    return r
}

private fun cGetScreenHeight(self: dynamic): dynamic = self.getMetrics()[1]

private fun cGetScreenWidth(self: dynamic): dynamic = self.getMetrics()[0]

private fun cReplaceSelection(s: dynamic, text: dynamic): dynamic {
    var start = js("G.Selection").getSelectionStart(s)
    var end = js("G.Selection").getSelectionEnd(s)
    if (start > end) {
        val t = start
        start = end
        end = t
    }
    if (start < 0) return js("undefined")
    s.replace(start, end, text)
    return js("undefined")
}

private fun cToFixedNumber(number: dynamic, bits: dynamic): dynamic {
    val pw = js("Math").pow(10, bits)
    return cDiv(js("Math").floor(cMul(number, pw)), pw)
}

private fun cAddSet(s: dynamic, value: dynamic): Boolean {
    val p = s.indexOf(value)
    if (p < 0) {
        s.push(value)
        return true
    }
    return false
}

private fun cRemoveSet(s: dynamic, value: dynamic): Boolean {
    val p = s.indexOf(value)
    if (p >= 0) {
        s.splice(p, 1)
        return true
    }
    return false
}

private fun cReplaceLinkedSet(s: dynamic, fromValue: dynamic, toValue: dynamic): dynamic {
    val p1 = s.indexOf(fromValue)
    val p2 = s.indexOf(toValue)
    if (p1 >= 0) {
        s[p1] = toValue
        if (p2 > 0 && cLooseNe(p2, p1)) s.splice(p2, 1)
    } else if (p2 < 0) {
        s.push(toValue)
    }
    return js("undefined")
}

private fun cInSet(s: dynamic, value: dynamic): Boolean = s.indexOf(value) >= 0

private fun cExchangeProperty(o: dynamic, i: dynamic, j: dynamic): dynamic {
    val t = o[i]
    o[i] = o[j]
    o[j] = t
    return js("undefined")
}

private fun cIterableToArray(o: dynamic): dynamic {
    var obj = o
    val r = js("[]")
    if (cInstanceOf(obj, js("java.lang.Iterable"))) {
        obj = obj.iterator()
    }
    if (cInstanceOf(obj, js("java.util.Enumeration"))) {
        while (cTruthy(obj.hasMoreElements())) r.push(obj.nextElement())
    } else {
        while (cTruthy(obj.hasNext())) r.push(obj.next())
    }
    return r
}

private fun cStringComparator(a: dynamic, b: dynamic): Int {
    val sa = cString(a)
    val sb = cString(b)
    return if (sa > sb) 1 else if (sa < sb) -1 else 0
}

private fun cStringComparatorIgnoreCase(a: dynamic, b: dynamic): Int {
    val sa = cLower(a)
    val sb = cLower(b)
    return if (sa > sb) 1 else if (sa < sb) -1 else 0
}

// ---------------- 安装 ----------------

fun installCommon() {
    commonApi.themelist = js("({})")
    commonApi.theme = js("null")

    commonApi.onCreate = cThisArgsJs { self: dynamic, _args: dynamic -> cOnCreate(self) }

    commonApi.argbInt = { alpha: dynamic, red: dynamic, green: dynamic, blue: dynamic ->
        cIntValue(cArgbBits(alpha, red, green, blue))
    }
    commonApi.rgbInt = { red: dynamic, green: dynamic, blue: dynamic ->
        cIntValue(cRgbBits(red, green, blue))
    }
    commonApi.setAlpha = { color: dynamic, alpha: dynamic ->
        cIntValue(cAlphaBits(color, alpha))
    }
    commonApi.setPaintColor = js("IntColor").Paint.setColor

    commonApi.loadTheme = cThisArgsJs { self: dynamic, args: dynamic ->
        cLoadTheme(self, args[0])
    }
    commonApi.applyStyle = cThisArgsJs { self: dynamic, args: dynamic ->
        cApplyStyle(self, args[0], args[1], args[2])
    }
    commonApi.applyPopup = { popup: dynamic -> cApplyPopup(popup) }

    commonApi.showChangeTheme = cMakeSelfJs { self: dynamic, args: dynamic ->
        cShowChangeTheme(self, args[0], args[1])
    }
    commonApi.customVMaker = { holder: dynamic -> cCustomVMaker(holder) }
    commonApi.initEnterAnimation = { v: dynamic -> cInitEnterAnimation(v) }
    commonApi.showDialog = { layout: dynamic, width: dynamic, height: dynamic, onExit: dynamic, modal: dynamic ->
        cShowDialog(layout, width, height, onExit, modal)
    }
    commonApi.showTextDialog = { s: dynamic, onDismiss: dynamic -> cShowTextDialog(s, onDismiss) }
    commonApi.showOperateDialog = cMakeSelfJs { self: dynamic, args: dynamic ->
        cShowOperateDialog(self, args[0], args[1], args[2])
    }
    commonApi.showInputDialog = { s: dynamic -> cShowInputDialog(s) }
    commonApi.showConfirmDialog = { s: dynamic -> cShowConfirmDialog(s) }
    commonApi.showListChooser = cMakeSelfJs { self: dynamic, args: dynamic ->
        cShowListChooser(self, args[0], args[1], args[2], args[3])
    }
    commonApi.showProgressDialog = cMakeSelfJs { self: dynamic, args: dynamic ->
        cShowProgressDialog(self, args[0], args[1])
    }
    commonApi.showSlider = cMakeSelfJs { self: dynamic, args: dynamic ->
        cShowSlider(self, args[0])
    }
    commonApi.showSettings = cMakeSelfJs { self: dynamic, args: dynamic ->
        cShowSettings(self, args[0], args[1], args[2])
    }
    commonApi.showFileDialog = cMakeSelfJs { self: dynamic, args: dynamic ->
        cShowFileDialog(self, args[0])
    }
    commonApi.showWebViewDialog = { s: dynamic -> cShowWebViewDialog(s) }
    commonApi.showSortDialog = cMakeSelfJs { self: dynamic, args: dynamic ->
        cShowSortDialog(self, args[0])
    }
    commonApi.showTutorial = cMakeSelfJs { self: dynamic, args: dynamic ->
        cShowTutorial(self, args[0])
    }
    commonApi.toast = cMakeSelfJs { self: dynamic, args: dynamic ->
        cShowToast(self, args[0])
    }

    commonApi.createWebView = { callback: dynamic, oldWebView: dynamic ->
        cCreateWebViewImpl(callback, oldWebView)
    }
    commonApi.destroyWebView = { wv: dynamic -> cDestroyWebView(wv) }
    commonApi.fileCopy = { src: dynamic, dest: dynamic -> cFileCopy(src, dest) }
    commonApi.readFile = { path: dynamic, defaultValue: dynamic, gzipped: dynamic, error: dynamic ->
        cReadFile(path, defaultValue, gzipped, error)
    }
    commonApi.saveFile = { path: dynamic, text: dynamic, gzipped: dynamic ->
        cSaveFile(path, text, gzipped)
    }
    commonApi.getFileSize = { f: dynamic, longer: dynamic -> cGetFileSize(f, longer) }
    commonApi.deleteFile = { path: dynamic -> cDeleteFile(path) }
    commonApi.toString = { s: dynamic -> cToString(s) }

    commonApi.toastSystem = cMakeSelfJs { self: dynamic, args: dynamic ->
        cToastSystem(self, args[0], args[1])
    }
    commonApi.postIME = { v: dynamic, delay: dynamic -> cPostIME(v, delay) }
    commonApi.hideIME = { v: dynamic -> cHideIME(v) }
    commonApi.hasClipboardText = { cHasClipboardText() }
    commonApi.getClipboardText = { cGetClipboardText() }
    commonApi.setClipboardText = { text: dynamic -> cSetClipboardText(text) }

    commonApi.getMetrics = { cGetMetrics() }
    commonApi.getScreenHeight = cThisArgsJs { self: dynamic, _args: dynamic ->
        cGetScreenHeight(self)
    }
    commonApi.getScreenWidth = cThisArgsJs { self: dynamic, _args: dynamic ->
        cGetScreenWidth(self)
    }
    commonApi.replaceSelection = { s: dynamic, text: dynamic -> cReplaceSelection(s, text) }
    commonApi.toFixedNumber = { number: dynamic, bits: dynamic -> cToFixedNumber(number, bits) }
    commonApi.addSet = { s: dynamic, value: dynamic -> cAddSet(s, value) }
    commonApi.removeSet = { s: dynamic, value: dynamic -> cRemoveSet(s, value) }
    commonApi.replaceLinkedSet = { s: dynamic, fromValue: dynamic, toValue: dynamic ->
        cReplaceLinkedSet(s, fromValue, toValue)
    }
    commonApi.inSet = { s: dynamic, value: dynamic -> cInSet(s, value) }
    commonApi.exchangeProperty = { o: dynamic, i: dynamic, j: dynamic -> cExchangeProperty(o, i, j) }
    commonApi.iterableToArray = { o: dynamic -> cIterableToArray(o) }
    commonApi.stringComparator = { a: dynamic, b: dynamic -> cStringComparator(a, b) }
    commonApi.stringComparatorIgnoreCase = { a: dynamic, b: dynamic ->
        cStringComparatorIgnoreCase(a, b)
    }

    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("Common", commonApi)
    }
}
