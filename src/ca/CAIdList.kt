package ca.kotlin

/**
 * modules/CA.js 的 idlist 组 Kotlin/JS 版本（原文件约 5914-6500 行，以成员名为准）。
 *
 * 只提供 installCaIdList(api)：把本组顶层成员挂到调用方传入的 CA 共享 API 上；
 * 不注册模块、不定义全局 caApi、不调用安装函数，也不重复实现其它组字段。
 *
 * 本组包含：checkFeatures / hasFeature / Features / drawQRCode / showDonateDialog /
 * chooseIDList / buildIDList / idListCachePath / idListCacheKey / loadIDListDiskCache /
 * saveIDListDiskCache / prewarmIDList / filterIDList / showIDList / SpecialTips / getTip。
 *
 * 原实现里 showDonateDialog / showIDList 是具名函数表达式 `function self(...)`，
 * 内部状态（getDonateImage / textHeight / linear / adpt / popup 等）挂在函数对象本身；
 * 这里用薄 JS 工厂保留同一个 self 语义与 arity。所有原来的 `this` 改为显式 api，
 * 跨组成员（IntelliSense / Library / settings / _idAllIndices / _idListBuildCache 等）
 * 只通过 api 动态读写。
 *
 * 原实现的既有行为按原样保留，例如：
 *  - buildIDList 会原地把字符串项解析成 library.enums 里的对象，并复用 _idListBuildCache；
 *  - 同一库 + 同一 ID 表命中缓存时直接返回，不刷新 Accel 缓存（与旧行为一致）；
 *  - prewarmIDList 依赖 api.Library.loadingStatus，最多重试 20 次，之后走 Threads；
 *  - filterIDList 空前缀直接返回 _idAllIndices，否则先 Accel.setIDTexts；
 *  - getTip 用 for-in 遍历 SpecialTips，再修改 settings.tipsRead；
 *  - drawQRCode 的 Base64.decode(code.bytes, 2)、Integer 颜色包装与 G.Paint 构造保持旧写法；
 *  - showDonateDialog 的 getDonateImage 只在首次打开时构建并缓存到函数对象上。
 */

// ---------------- JS 语义 helper ----------------

private val caIdTruthyJs: dynamic = js("function (v) { return !!v; }")
private fun caIdTruthy(v: dynamic): Boolean = caIdTruthyJs(v)

/** Java/JS 的 length 统一按旧代码的数值语义读取（`v.length | 0`）。 */
private val caIdLengthJs: dynamic = js("function (v) { return v.length | 0; }")
private fun caIdLength(v: dynamic): Int = caIdLengthJs(v)

private val caIdStringJs: dynamic = js("function (v) { return String(v); }")
private fun caIdString(v: dynamic): dynamic = caIdStringJs(v)

private val caIdLooseEqJs: dynamic = js("function (a, b) { return a == b; }")
private fun caIdLooseEq(a: dynamic, b: dynamic): Boolean = caIdLooseEqJs(a, b)

private val caIdStrictEqJs: dynamic = js("function (a, b) { return a === b; }")
private fun caIdStrictEq(a: dynamic, b: dynamic): Boolean = caIdStrictEqJs(a, b)

private val caIdStrictNeJs: dynamic = js("function (a, b) { return a !== b; }")
private fun caIdStrictNe(a: dynamic, b: dynamic): Boolean = caIdStrictNeJs(a, b)

private val caIdLtJs: dynamic = js("function (a, b) { return a < b; }")
private fun caIdLt(a: dynamic, b: dynamic): Boolean = caIdLtJs(a, b)

private val caIdGtJs: dynamic = js("function (a, b) { return a > b; }")
private fun caIdGt(a: dynamic, b: dynamic): Boolean = caIdGtJs(a, b)

private val caIdGeJs: dynamic = js("function (a, b) { return a >= b; }")
private fun caIdGe(a: dynamic, b: dynamic): Boolean = caIdGeJs(a, b)

private val caIdAddJs: dynamic = js("function (a, b) { return a + b; }")
private fun caIdAdd(a: dynamic, b: dynamic): dynamic = caIdAddJs(a, b)

private val caIdSubJs: dynamic = js("function (a, b) { return a - b; }")
private fun caIdSub(a: dynamic, b: dynamic): dynamic = caIdSubJs(a, b)

private val caIdMulJs: dynamic = js("function (a, b) { return a * b; }")
private fun caIdMul(a: dynamic, b: dynamic): dynamic = caIdMulJs(a, b)

private val caIdDivJs: dynamic = js("function (a, b) { return a / b; }")
private fun caIdDiv(a: dynamic, b: dynamic): dynamic = caIdDivJs(a, b)

private val caIdModJs: dynamic = js("function (a, b) { return a % b; }")
private fun caIdMod(a: dynamic, b: dynamic): dynamic = caIdModJs(a, b)

private val caIdOrJs: dynamic = js("function (a, b) { return a || b; }")
private fun caIdOr(a: dynamic, b: dynamic): dynamic = caIdOrJs(a, b)

/** 对应原 JS 的 `v == null`（同时匹配 null 与 undefined）。 */
private val caIdLooseUndefinedJs: dynamic = js("function (v) { return v == null; }")
private fun caIdLooseUndefined(v: dynamic): Boolean = caIdLooseUndefinedJs(v)

/** 对应原 JS 的 `v != null`。 */
private val caIdNotNullJs: dynamic = js("function (v) { return v != null; }")
private fun caIdNotNull(v: dynamic): Boolean = caIdNotNullJs(v)

private val caIdIsNaNJs: dynamic = js("function (v) { return isNaN(v); }")
private fun caIdIsNaN(v: dynamic): Boolean = caIdIsNaNJs(v)

private val caIdIsArrayJs: dynamic = js("function (v) { return Array.isArray(v); }")
private fun caIdIsArray(v: dynamic): Boolean = caIdIsArrayJs(v)

private val caIdInstanceOfJs: dynamic = js("function (v, c) { return v instanceof c; }")
private fun caIdInstanceOf(v: dynamic, c: dynamic): Boolean = caIdInstanceOfJs(v, c)

private val caIdTypeIsStringJs: dynamic = js("function (v) { return typeof v === 'string'; }")
private fun caIdTypeIsString(v: dynamic): Boolean = caIdTypeIsStringJs(v)

private val caIdObjectKeysJs: dynamic = js("function (o) { return Object.keys(o); }")
private fun caIdObjectKeys(o: dynamic): dynamic = caIdObjectKeysJs(o)

private val caIdForInJs: dynamic = js("function (o, f) { for (var i in o) f(i); }")
private fun caIdForIn(o: dynamic, f: dynamic) {
    caIdForInJs(o, f)
}

private val caIdObjectCreateNullJs: dynamic = js("function () { return Object.create(null); }")
private fun caIdObjectCreateNull(): dynamic = caIdObjectCreateNullJs()

private val caIdNewArrayJs: dynamic = js("function (n) { return new Array(n); }")
private fun caIdNewArray(n: dynamic): dynamic = caIdNewArrayJs(n)

private val caIdNowJs: dynamic = js("function () { return Date.now(); }")
private fun caIdNow(): dynamic = caIdNowJs()

private val caIdSeenHasJs: dynamic = js("function (seen, id) { return seen[id] !== undefined; }")
private fun caIdSeenHas(seen: dynamic, id: dynamic): Boolean = caIdSeenHasJs(seen, id)

private val caIdSingleArrayJs: dynamic = js("function (v) { return v ? [v] : []; }")
private fun caIdSingleArray(v: dynamic): dynamic = caIdSingleArrayJs(v)

private val caIdFirstSpecialTipJs: dynamic = js(
    "function (tips, date) {" +
        " for (var i in tips) {" +
        "  var t = tips[i](date);" +
        "  if (t) return t;" +
        " }" +
        " return undefined;" +
        "}"
)
private fun caIdFirstSpecialTip(tips: dynamic, date: dynamic): dynamic = caIdFirstSpecialTipJs(tips, date)

private val caIdKvSortCmpJs: dynamic = js(
    "function (a, b) { return a > b ? 1 : a == b ? 0 : -1; }"
)

private val caIdQrBitJs: dynamic = js(
    "function (bytes, p) {" +
        " return bytes[p >> 3] & new java.lang.Integer(1 << (p & 7)).byteValue();" +
        "}"
)
private fun caIdQrBit(bytes: dynamic, p: dynamic): dynamic = caIdQrBitJs(bytes, p)

private val caIdCacheEntryJs: dynamic = js(
    "function (lib, key, ids, texts, allIndices) {" +
        " return { lib: lib, key: key, ids: ids, texts: texts, allIndices: allIndices };" +
        "}"
)
private fun caIdCacheEntry(
    lib: dynamic, key: dynamic, ids: dynamic, texts: dynamic, allIndices: dynamic
): dynamic = caIdCacheEntryJs(lib, key, ids, texts, allIndices)

private val caIdCacheDataJs: dynamic = js(
    "function (key, ids, texts) { return { key: key, ids: ids, texts: texts }; }"
)
private fun caIdCacheData(key: dynamic, ids: dynamic, texts: dynamic): dynamic =
    caIdCacheDataJs(key, ids, texts)

private val caIdBuildResult2Js: dynamic = js(
    "function (ids, texts) { return { ids: ids, texts: texts }; }"
)
private fun caIdBuildResult2(ids: dynamic, texts: dynamic): dynamic = caIdBuildResult2Js(ids, texts)

private val caIdBuildResult3Js: dynamic = js(
    "function (ids, texts, allIndices) {" +
        " return { ids: ids, texts: texts, allIndices: allIndices };" +
        "}"
)
private fun caIdBuildResult3(ids: dynamic, texts: dynamic, allIndices: dynamic): dynamic =
    caIdBuildResult3Js(ids, texts, allIndices)

private val caIdDrawableDecoratorJs: dynamic = js(
    "function (drawable) { return { drawable: drawable }; }"
)
private fun caIdDrawableDecorator(drawable: dynamic): dynamic = caIdDrawableDecoratorJs(drawable)

// ---------------- Java 互操作 helper ----------------

private val caIdNewFile1Js: dynamic = js("function (p) { return new java.io.File(p); }")
private fun caIdNewFile1(p: dynamic): dynamic = caIdNewFile1Js(p)

private val caIdNewFile2Js: dynamic = js(
    "function (parent, child) { return new java.io.File(parent, child); }"
)
private fun caIdNewFile2(parent: dynamic, child: dynamic): dynamic = caIdNewFile2Js(parent, child)

private val caIdNewFileOutputStreamJs: dynamic = js(
    "function (f) { return new java.io.FileOutputStream(f); }"
)
private fun caIdNewFileOutputStream(f: dynamic): dynamic = caIdNewFileOutputStreamJs(f)

private val caIdNewJavaStringJs: dynamic = js(
    "function (s) { return new java.lang.String(s); }"
)
private fun caIdNewJavaString(s: dynamic): dynamic = caIdNewJavaStringJs(s)

private val caIdNewRunnableJs: dynamic = js(
    "function (f) { return new java.lang.Runnable({ run: f }); }"
)
private fun caIdNewRunnable(f: dynamic): dynamic = caIdNewRunnableJs(f)

// ---------------- Android UI helper ----------------

private val caIdDpJs: dynamic = js("function (n) { return n * G.dp; }")
private fun caIdDp(n: Int): dynamic = caIdDpJs(n)

private val caIdNewLinearLayoutJs: dynamic = js("function () { return new G.LinearLayout(ctx); }")
private val caIdNewFrameLayoutJs: dynamic = js("function () { return new G.FrameLayout(ctx); }")
private val caIdNewTextViewJs: dynamic = js("function () { return new G.TextView(ctx); }")
private val caIdNewEditTextJs: dynamic = js("function () { return new G.EditText(ctx); }")
private val caIdNewListViewJs: dynamic = js("function () { return new G.ListView(ctx); }")
private val caIdNewImageViewJs: dynamic = js("function () { return new G.ImageView(ctx); }")
private val caIdNewScrollViewJs: dynamic = js("function () { return new G.ScrollView(ctx); }")
private val caIdNewCanvasJs: dynamic = js("function (b) { return new G.Canvas(b); }")
private val caIdNewPaintJs: dynamic = js("function () { return new G.Paint(); }")
private val caIdNewPaintCopyJs: dynamic = js("function (p) { return new G.Paint(p); }")
private val caIdBitmapCreateJs: dynamic = js(
    "function (w, h, c) { return G.Bitmap.createBitmap(w, h, c); }"
)

private fun caIdNewLinearLayout(): dynamic = caIdNewLinearLayoutJs()
private fun caIdNewFrameLayout(): dynamic = caIdNewFrameLayoutJs()
private fun caIdNewTextView(): dynamic = caIdNewTextViewJs()
private fun caIdNewEditText(): dynamic = caIdNewEditTextJs()
private fun caIdNewListView(): dynamic = caIdNewListViewJs()
private fun caIdNewImageView(): dynamic = caIdNewImageViewJs()
private fun caIdNewScrollView(): dynamic = caIdNewScrollViewJs()
private fun caIdNewCanvas(bitmap: dynamic): dynamic = caIdNewCanvasJs(bitmap)
private fun caIdNewPaint(): dynamic = caIdNewPaintJs()
private fun caIdNewPaintCopy(paint: dynamic): dynamic = caIdNewPaintCopyJs(paint)
private fun caIdBitmapCreate(width: dynamic, height: dynamic, config: dynamic): dynamic =
    caIdBitmapCreateJs(width, height, config)

private val caIdNewAbsParamsJs: dynamic = js(
    "function (w, h) { return new G.AbsListView.LayoutParams(w, h); }"
)
private val caIdNewLinearParamsJs: dynamic = js(
    "function (w, h) { return new G.LinearLayout.LayoutParams(w, h); }"
)
private val caIdNewWeightedParamsJs: dynamic = js(
    "function (w, h, weight) { return new G.LinearLayout.LayoutParams(w, h, weight); }"
)
private val caIdNewFrameParamsJs: dynamic = js(
    "function (w, h) { return new G.FrameLayout.LayoutParams(w, h); }"
)

private fun caIdNewAbsParams(w: dynamic, h: dynamic): dynamic = caIdNewAbsParamsJs(w, h)
private fun caIdNewLinearParams(w: dynamic, h: dynamic): dynamic = caIdNewLinearParamsJs(w, h)
private fun caIdNewWeightedParams(w: dynamic, h: dynamic, weight: dynamic): dynamic =
    caIdNewWeightedParamsJs(w, h, weight)
private fun caIdNewFrameParams(w: dynamic, h: dynamic): dynamic = caIdNewFrameParamsJs(w, h)

private val caIdNewClickListenerJs: dynamic = js(
    "function (f) { return new G.View.OnClickListener({ onClick: f }); }"
)
private val caIdNewLongClickListenerJs: dynamic = js(
    "function (f) { return new G.View.OnLongClickListener({ onLongClick: f }); }"
)
private val caIdNewTextWatcherJs: dynamic = js(
    "function (f) { return new G.TextWatcher({ afterTextChanged: f }); }"
)
private val caIdNewItemClickListenerJs: dynamic = js(
    "function (f) { return new G.AdapterView.OnItemClickListener({ onItemClick: f }); }"
)
private val caIdNewPopupPageJs: dynamic = js(
    "function (v, n) { return new PopupPage(v, n); }"
)
private val caIdNewSimpleListAdapterJs: dynamic = js(
    "function (a, b, c, d, e) { return new SimpleListAdapter(a, b, c, d, e); }"
)

private fun caIdNewClickListener(f: dynamic): dynamic = caIdNewClickListenerJs(f)
private fun caIdNewLongClickListener(f: dynamic): dynamic = caIdNewLongClickListenerJs(f)
private fun caIdNewTextWatcher(f: dynamic): dynamic = caIdNewTextWatcherJs(f)
private fun caIdNewItemClickListener(f: dynamic): dynamic = caIdNewItemClickListenerJs(f)
private fun caIdNewPopupPage(view: dynamic, name: dynamic): dynamic = caIdNewPopupPageJs(view, name)
private fun caIdNewSimpleListAdapter(
    arr: dynamic, maker: dynamic, binder: dynamic, params: dynamic, readOnly: dynamic
): dynamic = caIdNewSimpleListAdapterJs(arr, maker, binder, params, readOnly)

// ---------------- 具名函数 self 工厂 ----------------

/** function self(donateMethods) { ... }：getDonateImage / getTextHeight 缓存挂在函数对象上。 */
private val caIdShowDonateDialogFactoryJs: dynamic = js(
    "function (impl) { return function self(donateMethods) { return impl(self, donateMethods); }; }"
)

/** function self(list, callback) { ... }：linear / adpt / popup 缓存挂在函数对象上。 */
private val caIdShowIDListFactoryJs: dynamic = js(
    "function (impl) { return function self(list, callback) { return impl(self, list, callback); }; }"
)

// ---------------- Features ----------------

private val caIdV16Js: dynamic = js("function (o) { o.minSupportVer = '0.16'; return o; }")
private val caIdV105Js: dynamic = js("function (o) { o.minSupportVer = '1.0.5'; return o; }")
private val caIdV12Js: dynamic = js("function (o) { o.minSupportVer = '1.2'; return o; }")
private val caIdV11Js: dynamic = js(
    "function (o) { o.minSupportVer = '1.1'; o.maxSupportVer = '1.1.*'; return o; }"
)

private fun caIdFeatures(): dynamic {
    val features = js("({})")
    features.enableCommand = caIdV16Js(js("({})"))
    features.enableCommandBlock = caIdV105Js(js("({})"))
    features.enableLocalCoord = caIdV12Js(js("({})"))
    features.version_1_1 = caIdV11Js(js("({})"))
    return features
}

private fun caIdCheckFeatures(api: dynamic) {
    caIdForIn(api.Features, { i: dynamic ->
        val feature = api.Features[i]
        feature.flag = api.Library.checkPackVer(feature)
    })
}

private fun caIdHasFeature(api: dynamic, feature: dynamic): Boolean =
    caIdLooseEq(api.Features[feature].flag, 0)

// ---------------- 二维码 / 捐赠 ----------------

private fun caIdDrawQRCode(bmp: dynamic, code: dynamic, decorator: dynamic) {
    val bytes = android.util.Base64.decode(code.bytes, 2)
    val cv = caIdNewCanvas(bmp)
    val pt = caIdNewPaint()
    pt.setAntiAlias(false)
    js("IntColor").Paint.setColor(pt, js("G.Color.BLACK"))
    pt.setStyle(js("G.Paint.Style.FILL"))
    js("IntColor").Canvas.drawColor(cv, js("new java.lang.Integer(G.Color.WHITE)"))
    cv.scale(caIdDiv(bmp.width, code.width), caIdDiv(bmp.height, code.height))
    var y: dynamic = 0
    while (caIdLt(y, code.height)) {
        var x: dynamic = 0
        while (caIdLt(x, code.width)) {
            val p = caIdAdd(caIdMul(y, code.width), x)
            if (caIdTruthy(caIdQrBit(bytes, p))) {
                cv.drawRect(x, y, caIdAdd(x, 1), caIdAdd(y, 1), pt)
            }
            x = caIdAdd(x, 1)
        }
        y = caIdAdd(y, 1)
    }
    if (caIdTruthy(decorator.drawable)) {
        val wr = code.whiteRect
        val dr = decorator.drawable
        dr.setBounds(wr.x, wr.y, caIdAdd(wr.width, wr.x), caIdAdd(wr.height, wr.y))
        dr.draw(cv)
    }
}

private fun caIdGetTextHeight(
    text: dynamic, maxWidth: dynamic, pt: dynamic, spacingMult: dynamic, spacingAdd: dynamic
): dynamic {
    val fontHeight = caIdSub(pt.descent(), pt.ascent())
    val spacing = caIdAdd(caIdMul(spacingMult, fontHeight), spacingAdd)
    var fromIndex: dynamic = 0
    var charCount: dynamic
    var lfPos: dynamic
    var lines = 0
    while (caIdLt(fromIndex, caIdLength(text))) {
        charCount = pt.breakText(text, fromIndex, caIdLength(text), true, maxWidth, null)
        lfPos = text.indexOf("\n", fromIndex)
        if (caIdGe(lfPos, 0) && caIdGe(charCount, lfPos)) charCount = caIdAdd(lfPos, 1)
        lines++
        fromIndex = caIdAdd(fromIndex, charCount)
    }
    return caIdAdd(caIdMul(fontHeight, lines), caIdMul(spacing, caIdSub(lines, 1)))
}

private fun caIdDrawText(
    canvas: dynamic, text: dynamic, x: dynamic, yIn: dynamic, maxWidth: dynamic,
    pt: dynamic, spacingMult: dynamic, spacingAdd: dynamic
) {
    var y = yIn
    val fontHeight = caIdSub(pt.descent(), pt.ascent())
    val spacing = caIdAdd(caIdMul(spacingMult, fontHeight), spacingAdd)
    var fromIndex: dynamic = 0
    var charCount: dynamic
    var lfPos: dynamic
    while (caIdLt(fromIndex, caIdLength(text))) {
        charCount = pt.breakText(text, fromIndex, caIdLength(text), true, maxWidth, null)
        lfPos = text.indexOf("\n", fromIndex)
        if (caIdGe(lfPos, 0) && caIdGe(charCount, lfPos)) charCount = caIdAdd(lfPos, 1)
        canvas.drawText(text, fromIndex, caIdAdd(fromIndex, charCount), x, y, pt)
        y = caIdAdd(y, caIdAdd(fontHeight, spacing))
        fromIndex = caIdAdd(fromIndex, charCount)
    }
}

private fun caIdGetDonateImage(api: dynamic, self: dynamic, width: dynamic, o: dynamic): dynamic {
    var bmp: dynamic
    var cv: dynamic
    val pt1 = caIdNewPaint()
    var pt2: dynamic
    var pt3: dynamic
    var totalHeight: dynamic = 0
    var fontHeight1: dynamic
    var fontHeight2: dynamic
    var fontHeight3: dynamic
    var textHeight1: dynamic
    // 原实现先把 footerHeight 设为 0.1 * width，text3 存在时才覆盖；这里保持同一初值。
    var footerHeight: dynamic = caIdMul(0.1, width)
    val text1 = o.title
    val text2 = o.description
    val text3 = o.comments
    val qr = caIdBitmapCreate(
        caIdMul(width, 0.8), caIdMul(width, 0.8), js("G.Bitmap.Config.ARGB_8888")
    )
    var drawable: dynamic
    if (caIdLooseEq(MapScript.host, "Android")) {
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            drawable = js("ctx").getResources().getDrawable(
                com.xero.ca.R.drawable.icon, js("ctx").getTheme()
            )
        } else {
            drawable = js("ctx").getResources().getDrawable(com.xero.ca.R.drawable.icon)
        }
    } else {
        drawable = js("new G.ColorDrawable(G.Color.BLACK)")
    }
    api.drawQRCode(qr, o.qrCode, caIdDrawableDecorator(drawable))
    pt1.setAntiAlias(true)
    pt1.setTextAlign(js("G.Paint.Align.CENTER"))
    js("IntColor").Paint.setColor(pt1, js("G.Color.BLACK"))
    pt1.setTextSize(caIdMul(0.1, width))
    fontHeight1 = caIdSub(pt1.descent(), pt1.ascent())
    textHeight1 = self.getTextHeight(text1, caIdMul(width, 0.6), pt1, 0, 0)
    totalHeight = caIdAdd(totalHeight, caIdAdd(textHeight1, fontHeight1))
    pt2 = caIdNewPaintCopy(pt1)
    js("IntColor").Paint.setColor(pt2, js("G.Color.GRAY"))
    pt2.setTextSize(caIdMul(0.07, width))
    fontHeight2 = caIdSub(pt2.descent(), pt2.ascent())
    pt3 = caIdNewPaintCopy(pt2)
    pt3.setTextSize(caIdMul(0.05, width))
    fontHeight3 = caIdSub(pt3.descent(), pt3.ascent())
    if (caIdTruthy(text2)) {
        totalHeight = caIdAdd(
            totalHeight,
            caIdAdd(
                self.getTextHeight(text2, caIdMul(width, 0.8), pt2, 0, 0),
                caIdMul(fontHeight2, 0.5)
            )
        )
    }
    if (caIdTruthy(text3)) {
        footerHeight = caIdAdd(
            self.getTextHeight(text3, caIdMul(width, 0.8), pt3, 0, 0),
            fontHeight3
        )
    }
    bmp = caIdBitmapCreate(
        width,
        caIdAdd(caIdAdd(totalHeight, caIdMul(width, 0.8)), footerHeight),
        js("G.Bitmap.Config.ARGB_8888")
    )
    cv = caIdNewCanvas(bmp)
    js("IntColor").Canvas.drawColor(cv, js("new java.lang.Integer(G.Color.WHITE)"))
    self.drawText(
        cv, text1, caIdMul(0.5, width), caIdSub(caIdMul(fontHeight1, 0.5), pt1.ascent()),
        caIdMul(width, 0.6), pt1, 0, 0
    )
    if (caIdTruthy(text2)) {
        self.drawText(
            cv, text2, caIdMul(0.5, width),
            caIdSub(
                caIdAdd(caIdAdd(caIdMul(fontHeight1, 0.5), caIdMul(fontHeight2, 0.5)), textHeight1),
                pt2.ascent()
            ),
            caIdMul(width, 0.8), pt2, 0, 0
        )
    }
    cv.drawBitmap(qr, caIdMul(0.1, width), totalHeight, pt1)
    if (caIdTruthy(text3)) {
        self.drawText(
            cv, text3, caIdMul(0.5, width),
            caIdSub(
                caIdAdd(caIdAdd(totalHeight, caIdMul(width, 0.8)), caIdMul(fontHeight3, 0.5)),
                pt3.ascent()
            ),
            caIdMul(width, 0.8), pt3, 0, 0
        )
    }
    qr.recycle()
    return bmp
}

private fun caIdShowDonateDialog(self: dynamic, api: dynamic, donateMethods: dynamic) {
    js("G").ui({
        try {
            var layout: dynamic
            var scr: dynamic
            var text: dynamic
            var img: dynamic
            var save: dynamic
            var exit: dynamic
            var popup: dynamic = js("undefined")
            var imgSaved = false
            if (!caIdTruthy(self.getDonateImage)) {
                self.getTextHeight = {
                        textIn: dynamic, maxWidth: dynamic, pt: dynamic,
                        spacingMult: dynamic, spacingAdd: dynamic ->
                    caIdGetTextHeight(textIn, maxWidth, pt, spacingMult, spacingAdd)
                }
                self.drawText = {
                        canvas: dynamic, textIn: dynamic, x: dynamic, y: dynamic,
                        maxWidth: dynamic, pt: dynamic, spacingMult: dynamic, spacingAdd: dynamic ->
                    caIdDrawText(canvas, textIn, x, y, maxWidth, pt, spacingMult, spacingAdd)
                }
                self.getDonateImage = { width: dynamic, o: dynamic ->
                    caIdGetDonateImage(api, self, width, o)
                }
            }
            var bmp = self.getDonateImage(caIdMul(240, js("G.dp")), donateMethods[0])
            scr = caIdNewScrollView()
            js("Common").applyStyle(scr, "message_bg")
            layout = caIdNewLinearLayout()
            layout.setOrientation(js("G.LinearLayout.VERTICAL"))
            layout.setPadding(caIdDp(10), caIdDp(10), caIdDp(10), 0)
            layout.setLayoutParams(caIdNewFrameParams(-1, -2))
            text = caIdNewTextView()
            text.setLayoutParams(caIdNewLinearParams(-1, -2))
            text.setText("捐助通道")
            text.setGravity(js("G.Gravity.CENTER"))
            text.setPadding(caIdDp(10), caIdDp(10), caIdDp(10), caIdDp(10))
            js("Common").applyStyle(text, "textview_default", 4)
            layout.addView(text)
            img = caIdNewImageView()
            img.setImageBitmap(bmp)
            img.setAdjustViewBounds(true)
            img.setScaleType(js("G.ImageView.ScaleType.CENTER_INSIDE"))
            img.setLayoutParams(caIdNewLinearParams(-2, -2))
            img.setOnClickListener(caIdNewClickListener({ _v: dynamic ->
                try {
                    try {
                        val f = caIdNewFile2(
                            android.os.Environment.getExternalStorageDirectory(),
                            "Pictures/ca_donate.png"
                        )
                        f.getParentFile().mkdirs()
                        val out = caIdNewFileOutputStream(f)
                        bmp.compress(js("G.Bitmap.CompressFormat.PNG"), 0, out)
                        out.close()
                        js("Common").toast("图片已保存至" + f.getPath())
                        js("AndroidBridge").scanMedia(f)
                        imgSaved = true
                    } catch (e: dynamic) {
                        js("Common").toast(caIdAdd("图片保存失败\n", e))
                    }
                } catch (e: dynamic) {
                    js("erp")(e)
                }
                js("undefined")
            }))
            img.setOnLongClickListener(caIdNewLongClickListener({ _v: dynamic ->
                try {
                    js("Common").showListChooser(donateMethods.map({ e: dynamic -> e.name }), {
                            pos: dynamic ->
                        val oldBmp = bmp
                        bmp = self.getDonateImage(caIdMul(240, js("G.dp")), donateMethods[pos])
                        img.setImageBitmap(bmp)
                        if (caIdTruthy(oldBmp)) oldBmp.recycle()
                        js("undefined")
                    })
                    true
                } catch (e: dynamic) {
                    js("erp")(e)
                    true
                }
            }))
            layout.addView(img)
            save = caIdNewTextView()
            save.setLayoutParams(caIdNewLinearParams(-1, -2))
            save.setText("点击二维码保存图片\n长按切换捐款方式")
            save.setGravity(js("G.Gravity.CENTER"))
            save.setPadding(caIdDp(10), caIdDp(10), caIdDp(10), 0)
            js("Common").applyStyle(save, "textview_prompt", 2)
            layout.addView(save)
            exit = caIdNewTextView()
            exit.setLayoutParams(caIdNewLinearParams(-1, -2))
            exit.setText("关闭")
            exit.setGravity(js("G.Gravity.CENTER"))
            exit.setPadding(caIdDp(10), caIdDp(20), caIdDp(10), caIdDp(20))
            js("Common").applyStyle(exit, "button_critical", 3)
            exit.setOnClickListener(caIdNewClickListener({ _v: dynamic ->
                try {
                    popup.exit()
                    if (imgSaved) js("Common").toast("感谢您的支持！")
                } catch (e: dynamic) {
                    js("erp")(e)
                }
                js("undefined")
            }))
            layout.addView(exit)
            scr.addView(layout)
            popup = js("PopupPage").showDialog("ca.DonateDialog", scr, -2, -2)
            popup.on("exit", { ->
                img.postDelayed({ ->
                    try {
                        img.setImageDrawable(null)
                        bmp.recycle()
                    } catch (e: dynamic) {
                        js("erp")(e)
                    }
                    js("undefined")
                }, 1000)
                js("undefined")
            })
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

// ---------------- ID 列表 / 缓存 / 过滤 ----------------

private fun caIdChooseIDList(api: dynamic, callback: dynamic) {
    val allIds = js("[]")
    val r = api.IntelliSense.library.idlist.map({ e: dynamic ->
        val item = js("({})")
        item.text = e.name
        item.list = caIdOr(e.lists, caIdSingleArray(e.list))
        caIdForIn(item.list, { i: dynamic -> allIds.push(item.list[i]) })
        item
    })
    val all = js("({})")
    all.text = "全部"
    all.list = allIds
    r.unshift(all)
    js("Common").showListChooser(r, { pos: dynamic ->
        api.showIDList(r[pos].list, callback)
        js("undefined")
    })
}

/**
 * 构建 ID 列表（去重 + 按 ID 排序）；被 showIDList 与调试基准共用。
 * 注意：命中 _idListBuildCache 时不会重新调用 Accel.setIDTexts，这是旧行为。
 */
private fun caIdBuildIDList(api: dynamic, list: dynamic): dynamic {
    val t0 = caIdNow()
    var tResolve: dynamic = t0
    var tBuild: dynamic = t0
    var tSort: dynamic = t0
    var tSync: dynamic = t0
    var tAll: dynamic = t0
    val cur = list
    val seen = caIdObjectCreateNull()
    var ids = js("[]")
    var texts = js("[]")
    var cacheKey: dynamic = js("null")
    val lib = api.IntelliSense.library
    // 同一库 + 同一 ID 表重复打开时直接复用（只缓存字符串 list）
    if (caIdTruthy(list) && caIdTruthy(caIdLength(list)) && caIdTypeIsString(list[0])) {
        cacheKey = list.join("\u0001")
        // 缓存最近打开过的多个列表；在「全部 / 方块 / 物品」之间切换时不用重建
        var caches = api._idListBuildCache
        if (!caIdIsArray(caches)) {
            caches = js("[]")
            api._idListBuildCache = caches
        }
        var ci = 0
        val cacheCount = caIdLength(caches)
        while (ci < cacheCount) {
            val cache = caches[ci]
            if (caIdStrictEq(cache.lib, lib) && caIdStrictEq(cache.key, cacheKey)) {
                api._idAllIndices = cache.allIndices
                if (caIdLooseEq(js("BuildConfig").variants, "debug")) {
                    try {
                        android.util.Log.i(
                            "Script", "IDList init cached, " + caIdLength(cache.ids) + " 项"
                        )
                    } catch (unused: dynamic) {
                    }
                }
                return caIdBuildResult3(cache.ids, cache.texts, cache.allIndices)
            }
            ci++
        }
    }
    var i = 0
    val curCount = caIdLength(cur)
    while (i < curCount) {
        if (!caIdInstanceOf(cur[i], js("Object"))) {
            cur[i] = api.IntelliSense.library.enums[cur[i]]
        }
        i++
    }
    tResolve = caIdNow()
    // 用 seen 做 O(n) 去重，并顺手存下显示文本（省掉后面再建一次 id→text 映射）
    var j = 0
    var e: dynamic = js("null")
    var ks: dynamic = js("null")
    var id: dynamic = js("null")
    i = 0
    val resolveCount = caIdLength(cur)
    while (i < resolveCount) {
        e = cur[i]
        if (!caIdLooseUndefined(e)) {
            if (caIdIsArray(e)) {
                j = 0
                val elementCount = caIdLength(e)
                while (j < elementCount) {
                    id = e[j]
                    if (!caIdSeenHas(seen, id)) {
                        seen[id] = id
                        ids.push(id)
                    }
                    j++
                }
            } else {
                ks = caIdObjectKeys(e)
                j = 0
                val keyCount = caIdLength(ks)
                while (j < keyCount) {
                    id = ks[j]
                    if (!caIdSeenHas(seen, id)) {
                        seen[id] = caIdAdd(
                            id,
                            if (caIdTruthy(e[id])) caIdAdd(" - ", e[id]) else ""
                        )
                        ids.push(id)
                    }
                    j++
                }
            }
        }
        i++
    }
    tBuild = caIdNow()
    // ID 表都是字符串；用原生 sort，再按 seen 还原 texts（比逐次比较器快 ~10x）
    var allString = true
    var i2 = 0
    val idsCount = caIdLength(ids)
    while (i2 < idsCount) {
        if (!caIdTypeIsString(ids[i2])) {
            allString = false
            break
        }
        i2++
    }
    if (allString) {
        ids.sort()
    } else {
        texts = caIdNewArray(caIdLength(ids))
        i2 = 0
        val unsortedCount = caIdLength(ids)
        while (i2 < unsortedCount) {
            texts[i2] = seen[ids[i2]]
            i2++
        }
        js("ISegment").kvSort(ids, texts, caIdKvSortCmpJs)
    }
    texts = caIdNewArray(caIdLength(ids))
    i2 = 0
    val sortedCount = caIdLength(ids)
    while (i2 < sortedCount) {
        texts[i2] = seen[ids[i2]]
        i2++
    }
    tSort = caIdNow()
    // 同步给 Java/JS 缓存：之后每次输入只传前缀，不再传整个 texts 数组
    js("Accel").setIDTexts(texts)
    tSync = caIdNow()
    api._idAllIndices = js("Accel").rangeIndices(caIdLength(ids))
    tAll = caIdNow()
    if (caIdNotNull(cacheKey)) {
        var caches2 = api._idListBuildCache
        if (!caIdIsArray(caches2)) {
            caches2 = js("[]")
            api._idListBuildCache = caches2
        }
        caches2.push(caIdCacheEntry(lib, cacheKey, ids, texts, api._idAllIndices))
        // 只保留最近 4 个列表，避免多库/多列表时无限占内存
        if (caIdGt(caIdLength(caches2), 4)) caches2.shift()
    }
    if (caIdLooseEq(js("BuildConfig").variants, "debug")) {
        try {
            android.util.Log.i(
                "Script",
                "IDList init " + caIdSub(caIdNow(), t0) + "ms, " + caIdLength(ids) +
                    " 项 (resolve=" + caIdSub(tResolve, t0) + " build=" + caIdSub(tBuild, tResolve) +
                    " sort=" + caIdSub(tSort, tBuild) + " sync=" + caIdSub(tSync, tSort) +
                    " all=" + caIdSub(tAll, tSync) + ")"
            )
        } catch (unused: dynamic) {
        }
    }
    return caIdBuildResult2(ids, texts)
}

/** 跨会话 ID 列表缓存文件；键包含版本、库列表和 ID 原始表，数据变化会自然失效 */
private fun caIdIdListCachePath(): dynamic = caIdAdd(MapScript.baseDir, "idlist_cache.dat")

private fun caIdIdListCacheKey(api: dynamic, names: dynamic): dynamic {
    val lib = if (caIdTruthy(api.IntelliSense)) api.IntelliSense.library else api.IntelliSense
    val info = if (caIdTruthy(lib) && caIdTruthy(lib.info)) {
        caIdString(MapScript.toSource(lib.info))
    } else {
        ""
    }
    val hash = caIdNewJavaString(info)
    val parts = js("[]")
    parts.push(1)
    parts.push(js("BuildConfig").versionCode)
    parts.push(js("BuildConfig").date)
    parts.push(caIdOr(api.settings.coreLibrarys, js("[]")).join(","))
    parts.push(caIdOr(api.settings.enabledLibrarys, js("[]")).join(","))
    if (caIdTruthy(lib) && caIdTruthy(lib.idlist)) {
        parts.push(caIdLength(lib.idlist))
    } else {
        parts.push(0)
    }
    parts.push(caIdAdd(caIdAdd(hash.hashCode(), "@"), hash.length()))
    parts.push(names.join("\u0001"))
    return parts.join("|")
}

private fun caIdLoadIDListDiskCache(api: dynamic, key: dynamic): dynamic {
    try {
        val text = js("SafeFileUtils").readText(caIdNewFile1(api.idListCachePath()), null)
        if (!caIdTruthy(text)) return null
        var data: dynamic
        var ids: dynamic = js("null")
        var texts: dynamic = js("null")
        // 兼容旧版 JSON 缓存
        if (caIdLooseEq(text.charAt(0), "{")) {
            data = js("JSON").parse(text)
        } else {
            val p1: dynamic = text.indexOf("\n")
            var p2: dynamic = -1
            if (!caIdLt(p1, 0)) p2 = text.indexOf("\n", caIdAdd(p1, 1))
            if (caIdLt(p1, 0) || caIdLt(p2, 0) || !caIdStrictEq(text.slice(0, p1), key)) return null
            ids = text.slice(caIdAdd(p1, 1), p2).split("\u0001")
            texts = text.slice(caIdAdd(p2, 1)).split("\u0001")
            if (caIdStrictEq(caIdLength(ids), 1) && caIdStrictEq(ids[0], "")) ids = js("[]")
            if (caIdStrictEq(caIdLength(texts), 1) && caIdStrictEq(texts[0], "")) texts = js("[]")
            data = caIdCacheData(key, ids, texts)
        }
        if (!caIdTruthy(data) || !caIdStrictEq(data.key, key) ||
            !caIdIsArray(data.ids) || !caIdIsArray(data.texts)
        ) {
            return null
        }
        if (!caIdStrictEq(caIdLength(data.ids), caIdLength(data.texts))) return null
        return data
    } catch (e: dynamic) {
        return null
    }
}

private fun caIdSaveIDListDiskCache(api: dynamic, key: dynamic, built: dynamic) {
    try {
        // 行格式比 JSON 数组解析快：header\nids\ntexts，元素内用 U+0001 分隔
        js("SafeFileUtils").writeText(
            caIdNewFile1(api.idListCachePath()),
            caIdAdd(
                caIdAdd(
                    caIdAdd(caIdAdd(key, "\n"), built.ids.join("\u0001")),
                    "\n"
                ),
                built.texts.join("\u0001")
            )
        )
    } catch (e: dynamic) {
        android.util.Log.w("Script", caIdAdd("IDList disk cache save failed: ", e))
    }
}

/** 启动后在后台预热「全部」ID 列表，减少用户第一次打开 ID 选择器的等待 */
private fun caIdPrewarmIDList(api: dynamic) {
    if (caIdTruthy(api._idListPrewarmStarted)) return
    // 拓展包是异步加载的，必须等库加载完成再预热，否则会缓存一份不完整的「全部」列表
    if (caIdTruthy(api.Library) && caIdTruthy(api.Library.loadingStatus)) {
        api._idListPrewarmRetry = caIdAdd(caIdOr(api._idListPrewarmRetry, 0), 1)
        if (caIdGt(api._idListPrewarmRetry, 20)) return
        js("gHandler").postDelayed(caIdNewRunnable({ ->
            try {
                api.prewarmIDList()
            } catch (e: dynamic) {
            }
            js("undefined")
        }), 500)
        return
    }
    api._idListPrewarmStarted = true
    val lib = if (caIdTruthy(api.IntelliSense)) api.IntelliSense.library else api.IntelliSense
    val names = js("[]")
    if (!caIdTruthy(lib) || !caIdTruthy(lib.idlist)) return
    try {
        var i = 0
        val listCount = caIdLength(lib.idlist)
        while (i < listCount) {
            val e = lib.idlist[i]
            val l = caIdOr(e.lists, caIdSingleArray(e.list))
            var j = 0
            val nameCount = caIdLength(l)
            while (j < nameCount) {
                names.push(l[j])
                j++
            }
            i++
        }
        if (!caIdTruthy(caIdLength(names))) return
        val key = api.idListCacheKey(names)
        js("Threads").run({ ->
            val t0 = caIdNow()
            try {
                val disk = api.loadIDListDiskCache(key)
                if (caIdTruthy(disk)) {
                    val allIndices = js("Accel").rangeIndices(caIdLength(disk.ids))
                    api._idAllIndices = allIndices
                    val arr = js("[]")
                    arr.push(
                        caIdCacheEntry(
                            api.IntelliSense.library, names.join("\u0001"),
                            disk.ids, disk.texts, allIndices
                        )
                    )
                    api._idListBuildCache = arr
                    js("Accel").setIDTexts(disk.texts)
                    if (caIdLooseEq(js("BuildConfig").variants, "debug")) {
                        android.util.Log.i(
                            "Script",
                            "IDList prewarm disk hit " + caIdSub(caIdNow(), t0) + "ms, " +
                                caIdLength(disk.ids) + " 项"
                        )
                    }
                } else {
                    val built = api.buildIDList(names)
                    api.saveIDListDiskCache(key, built)
                    if (caIdLooseEq(js("BuildConfig").variants, "debug")) {
                        android.util.Log.i(
                            "Script",
                            "IDList prewarm " + caIdSub(caIdNow(), t0) + "ms, " +
                                caIdLength(built.ids) + " 项"
                        )
                    }
                }
            } catch (e: dynamic) {
                android.util.Log.w("Script", caIdAdd("IDList prewarm failed: ", e))
            }
            js("undefined")
        })
    } catch (e: dynamic) {
        android.util.Log.w("Script", caIdAdd("IDList prewarm setup failed: ", e))
    }
}

/** 过滤 ID 列表，返回 texts 下标数组；有 jar 走 Accel.filter，没有走等价 JS */
private fun caIdFilterIDList(api: dynamic, texts: dynamic, s: dynamic): dynamic {
    val t0 = caIdNow()
    val p = caIdString(s)
    // 空前缀直接返回构建时缓存的全部下标，避免 4000+ 结果反复构造
    if (caIdStrictEq(p, "") && caIdNotNull(api._idAllIndices)) {
        if (caIdLooseEq(js("BuildConfig").variants, "debug")) {
            try {
                android.util.Log.i(
                    "Script",
                    "IDList filter \"\" cached " + caIdSub(caIdNow(), t0) + "ms, " +
                        caIdLength(texts) + " -> " + caIdLength(api._idAllIndices)
                )
            } catch (unused: dynamic) {
            }
        }
        return api._idAllIndices
    }
    // 第一次非空前缀才把 texts 同步到 Java/JS 缓存；空前缀走 _idAllIndices
    js("Accel").setIDTexts(texts)
    val arr = js("Accel").filterIDTextsRaw(p)
    if (caIdLooseEq(js("BuildConfig").variants, "debug")) {
        try {
            android.util.Log.i(
                "Script",
                "IDList filter \"" + p.slice(0, 20) + "\" " + caIdSub(caIdNow(), t0) + "ms, " +
                    caIdLength(texts) + " -> " + caIdLength(arr)
            )
        } catch (unused: dynamic) {
        }
    }
    return arr
}

// ---------------- ID 列表界面 ----------------

private fun caIdShowIDListUi(api: dynamic, self: dynamic) {
    self.vmaker = { _holder: dynamic ->
        val view = caIdNewTextView()
        view.setLayoutParams(caIdNewAbsParams(-1, -2))
        view.setPadding(caIdDp(15), caIdDp(15), caIdDp(15), caIdDp(15))
        js("Common").applyStyle(view, "textview_default", 3)
        view
    }
    self.vbinder = { holder: dynamic, e: dynamic, _i: dynamic, _a: dynamic ->
        holder.self.setText(self.texts[e])
    }
    self.init = { listIn: dynamic ->
        val t = api.buildIDList(listIn)
        self.ids = t.ids
        self.texts = t.texts
        self.update("")
        js("undefined")
    }
    self.update = { s: dynamic ->
        self.adpt.setArray(api.filterIDList(self.texts, s))
        js("undefined")
    }
    self.adpt = js("SimpleListAdapter").getController(
        caIdNewSimpleListAdapter(js("[]"), self.vmaker, self.vbinder, null, true)
    )
    self.linear = caIdNewLinearLayout()
    self.linear.setOrientation(js("G.LinearLayout.VERTICAL"))
    js("Common").applyStyle(self.linear, "container_default")
    self.header = caIdNewLinearLayout()
    self.header.setOrientation(js("G.LinearLayout.HORIZONTAL"))
    self.header.setPadding(caIdDp(5), 0, 0, 0)
    self.header.setLayoutParams(caIdNewLinearParams(-1, -2))
    js("Common").applyStyle(self.header, "bar_float")
    self.edit = caIdNewEditText()
    self.edit.setSingleLine(true)
    self.edit.setLayoutParams(caIdNewWeightedParams(0, -2, 1.0))
    self.edit.setImeOptions(js("G.EditorInfo.IME_FLAG_NO_FULLSCREEN"))
    self.edit.setTypeface(caIdOr(js("G.Typeface.MONOSPACE"), js("G.Typeface.DEFAULT")))
    js("Common").applyStyle(self.edit, "edittext_default", 3)
    self.edit.addTextChangedListener(caIdNewTextWatcher({ s: dynamic ->
        try {
            self.update(caIdString(s))
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }))
    self.header.addView(self.edit)
    self.exit = caIdNewTextView()
    self.exit.setText("×")
    self.exit.setGravity(js("G.Gravity.CENTER"))
    self.exit.setPadding(caIdDp(10), 0, caIdDp(10), 0)
    self.exit.setLayoutParams(caIdNewLinearParams(-2, -1))
    js("Common").applyStyle(self.exit, "button_critical", 3)
    self.exit.setOnClickListener(caIdNewClickListener({ _v: dynamic ->
        try {
            self.popup.exit()
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }))
    self.header.addView(self.exit)
    self.linear.addView(self.header)
    self.list = caIdNewListView()
    self.list.setAdapter(self.adpt.self)
    self.list.setLayoutParams(caIdNewLinearParams(-1, -1))
    self.list.setOnItemClickListener(caIdNewItemClickListener(
        { _parent: dynamic, _view: dynamic, pos: dynamic, _id: dynamic ->
            try {
                val text = self.ids[self.adpt.array[pos]]
                if (caIdTruthy(self.callback)) {
                    self.popup.exit()
                    self.callback(text)
                } else {
                    self.edit.setText(text)
                    self.edit.setSelection(self.edit.length())
                }
            } catch (e: dynamic) {
                js("erp")(e)
            }
            js("undefined")
        }
    ))
    if (caIdLooseEq(js("G").style, "Material")) {
        self.list.setFastScrollEnabled(true)
        self.list.setFastScrollAlwaysVisible(false)
    }
    self.linear.addView(self.list)

    self.popup = caIdNewPopupPage(self.linear, "ca.IDList")

    js("PWM").registerResetFlag(self, "linear")
}

private fun caIdShowIDList(self: dynamic, api: dynamic, list: dynamic, callback: dynamic) {
    js("G").ui({
        try {
            if (!caIdTruthy(self.linear)) caIdShowIDListUi(api, self)
            js("Common").showProgressDialog({ o: dynamic ->
                o.setText("正在加载列表……")
                self.init(list)
                if (!caIdTruthy(o.cancelled)) {
                    js("G").ui({
                        try {
                            self.callback = callback
                            self.popup.enter()
                        } catch (e: dynamic) {
                            js("erp")(e)
                        }
                        js("undefined")
                    })
                }
                js("undefined")
            }, true)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

// ---------------- 提示语 ----------------

private fun caIdSpecialTips(): dynamic {
    val tips = js("[]")
    tips.push({ d: dynamic ->
        if (caIdGt(d.getFullYear(), 2017) && caIdLooseEq(d.getMonth(), 2) &&
            caIdLooseEq(d.getDate(), 20)
        ) {
            "命令助手" + caIdSub(d.getFullYear(), 2017) + "周年！感谢你们的支持！"
        } else {
            js("undefined")
        }
    })
    tips.push({ d: dynamic ->
        if (caIdLooseEq(d.getMonth(), 4) && caIdLooseEq(d.getDate(), 1)) {
            "劳动节快乐！"
        } else {
            js("undefined")
        }
    })
    tips.push({ d: dynamic ->
        if (caIdLooseEq(d.getMonth(), 5) && caIdLooseEq(d.getDate(), 1)) {
            "儿童节快乐！"
        } else {
            js("undefined")
        }
    })
    tips.push({ d: dynamic ->
        if (caIdLooseEq(d.getMonth(), 9) && caIdLooseEq(d.getDate(), 1)) {
            "国庆节快乐！"
        } else {
            js("undefined")
        }
    })
    return tips
}

private fun caIdGetTip(api: dynamic): dynamic {
    val date = js("new Date()")
    val tip = caIdFirstSpecialTip(api.SpecialTips, date)
    if (caIdTruthy(tip)) return tip
    if (caIdIsNaN(api.settings.tipsRead)) {
        api.settings.tipsRead = 0
    } else {
        api.settings.tipsRead =
            caIdMod(caIdAdd(api.settings.tipsRead, 1), caIdLength(api.tips))
    }
    return api.tips[api.settings.tipsRead]
}

// ---------------- 安装 ----------------

fun installCaIdList(api: dynamic) {
    // 顺序与原对象字面量一致，方便对照 API key。
    api.checkFeatures = { ->
        caIdCheckFeatures(api)
        js("undefined")
    }
    api.hasFeature = { feature: dynamic -> caIdHasFeature(api, feature) }
    api.Features = caIdFeatures()

    api.drawQRCode = { bmp: dynamic, code: dynamic, decorator: dynamic ->
        caIdDrawQRCode(bmp, code, decorator)
        js("undefined")
    }
    api.showDonateDialog = caIdShowDonateDialogFactoryJs {
            self: dynamic, donateMethods: dynamic ->
        caIdShowDonateDialog(self, api, donateMethods)
        js("undefined")
    }

    api.chooseIDList = { callback: dynamic ->
        caIdChooseIDList(api, callback)
        js("undefined")
    }
    api.buildIDList = { list: dynamic -> caIdBuildIDList(api, list) }
    api.idListCachePath = { -> caIdIdListCachePath() }
    api.idListCacheKey = { names: dynamic -> caIdIdListCacheKey(api, names) }
    api.loadIDListDiskCache = { key: dynamic -> caIdLoadIDListDiskCache(api, key) }
    api.saveIDListDiskCache = { key: dynamic, built: dynamic ->
        caIdSaveIDListDiskCache(api, key, built)
        js("undefined")
    }
    api.prewarmIDList = { ->
        caIdPrewarmIDList(api)
        js("undefined")
    }
    api.filterIDList = { texts: dynamic, s: dynamic -> caIdFilterIDList(api, texts, s) }
    api.showIDList = caIdShowIDListFactoryJs {
            self: dynamic, list: dynamic, callback: dynamic ->
        caIdShowIDList(self, api, list, callback)
        js("undefined")
    }

    api.SpecialTips = caIdSpecialTips()
    api.getTip = { -> caIdGetTip(api) }
}
