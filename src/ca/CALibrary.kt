package ca.kotlin.ca

import ca.kotlin.core.*


/**
 * modules/CA.js library 组的 Kotlin/JS 迁移（原文件约第 4376-6132 行，以成员名为准）。
 *
 * 只提供 installCaLibrary(api)：把 showSourceMan / showLibraryMan / showOnlineLib /
 * showModeChooser / showManageBgImage / showIconChooser / customIcon / Icon /
 * checkFeatures / hasFeature / Features / drawQRCode / showDonateDialog 挂到调用方
 * 传入的共享 API 对象上；不注册模块、不定义全局 caApi、不调用安装函数。
 *
 * 迁移约定：
 *  - 原实现里的 this 改成显式 api；跨组成员（Library / IntelliSense / settings /
 *    trySave / checkFeatures / customIcon 等）只通过 api 动态解析。
 *  - showSourceMan / showLibraryMan / showOnlineLib / showIconChooser /
 *    showDonateDialog 仍是具名函数表达式 `function self(...)`，界面状态挂在函数对象
 *    本身；这里用薄 JS 工厂保留同一个 self 语义与 PWM.registerResetFlag 的调用方式。
 *  - 原实现里只有 showLibraryMan 的“设置安全级别”菜单项和 Common 的操作对话框
 *    依赖回调的动态 this；这两处分别用 caThis1Js / caThis2Js 把真实 this 传回 Kotlin。
 *  - 原实现的既有行为按原样保留，包括：
 *      * 安全级别切换里 `_refresh` 作为 showSlider 选项对象的字段，通过 this 调用；
 *      * 编辑拓展包时原代码使用隐式全局变量 a，这里改为本次调用的局部 source，
 *        避免污染脚本作用域，界面流程不变；
 *      * 扩展包更新下载确认框里的 startActivity 失败只记日志并提示打开链接失败。
 *  - JS 的 truthy / 宽松相等 / length / 加法与拼接语义走带参 JS helper；
 *    js("...") 里不引用 Kotlin 局部变量。
 */
private val caTruthyJs: dynamic = js("function (v) { return !!v; }")
private fun caTruthy(v: dynamic): Boolean = caTruthyJs(v)

private val caLengthJs: dynamic = js("function (v) { return v.length | 0; }")
private fun caLength(v: dynamic): Int = caLengthJs(v)

private val caStringJs: dynamic = js("function (v) { return String(v); }")
private fun caString(v: dynamic): dynamic = caStringJs(v)

private val caLooseEqJs: dynamic = js("function (a, b) { return a == b; }")
private fun caLooseEq(a: dynamic, b: dynamic): Boolean = caLooseEqJs(a, b)

private val caLooseNeJs: dynamic = js("function (a, b) { return a != b; }")
private fun caLooseNe(a: dynamic, b: dynamic): Boolean = caLooseNeJs(a, b)

private val caLtJs: dynamic = js("function (a, b) { return a < b; }")
private fun caLt(a: dynamic, b: dynamic): Boolean = caLtJs(a, b)

private val caGtJs: dynamic = js("function (a, b) { return a > b; }")
private fun caGt(a: dynamic, b: dynamic): Boolean = caGtJs(a, b)

private val caGeJs: dynamic = js("function (a, b) { return a >= b; }")
private fun caGe(a: dynamic, b: dynamic): Boolean = caGeJs(a, b)

private val caAddJs: dynamic = js("function (a, b) { return a + b; }")
private fun caAdd(a: dynamic, b: dynamic): dynamic = caAddJs(a, b)

private val caSubJs: dynamic = js("function (a, b) { return a - b; }")
private fun caSub(a: dynamic, b: dynamic): dynamic = caSubJs(a, b)

private val caMulJs: dynamic = js("function (a, b) { return a * b; }")
private fun caMul(a: dynamic, b: dynamic): dynamic = caMulJs(a, b)

private val caDivJs: dynamic = js("function (a, b) { return a / b; }")
private fun caDiv(a: dynamic, b: dynamic): dynamic = caDivJs(a, b)

private val caOrJs: dynamic = js("function (a, b) { return a || b; }")
private fun caOr(a: dynamic, b: dynamic): dynamic = caOrJs(a, b)

private val caAndJs: dynamic = js("function (a, b) { return a && b; }")
private fun caAnd(a: dynamic, b: dynamic): dynamic = caAndJs(a, b)

private val caIsNaNRawJs: dynamic = js("function (v) { return isNaN(v); }")
private fun caIsNaN(v: dynamic): Boolean = caIsNaNRawJs(v)

private val caInJs: dynamic = js("function (k, o) { return k in o; }")
private fun caIn(k: dynamic, o: dynamic): Boolean = caInJs(k, o)

private val caInstanceOfJs: dynamic = js("function (v, c) { return v instanceof c; }")
private fun caInstanceOf(v: dynamic, c: dynamic): Boolean = caInstanceOfJs(v, c)

private val caObjectKeysJs: dynamic = js("function (o) { return Object.keys(o); }")
private fun caObjectKeys(o: dynamic): dynamic = caObjectKeysJs(o)

private val caForInJs: dynamic = js("function (o, f) { for (var i in o) f(i); }")
private fun caForIn(o: dynamic, f: dynamic) {
    caForInJs(o, f)
}

private val caJsonStringifyJs: dynamic = js("function (v, r, s) { return JSON.stringify(v, r, s); }")
private fun caJsonStringify(v: dynamic, r: dynamic, s: dynamic): dynamic = caJsonStringifyJs(v, r, s)

private val caDateParseJs: dynamic = js("function (v) { return Date.parse(v); }")
private fun caDateParse(v: dynamic): dynamic = caDateParseJs(v)

private val caDpJs: dynamic = js("function (n) { return n * G.dp; }")
private fun caDp(n: Int): dynamic = caDpJs(n)

private val caGetMinecraftVersionJs: dynamic = js("function () { return getMinecraftVersion(); }")
private fun caGetMinecraftVersion(): dynamic = caGetMinecraftVersionJs()

private val caNewDateLocaleStringJs: dynamic = js("function () { return new Date().toLocaleString(); }")
private fun caNewDateLocaleString(): dynamic = caNewDateLocaleStringJs()

private val caRandomUuidJs: dynamic = js("function () { return String(java.util.UUID.randomUUID().toString()); }")
private fun caRandomUuid(): dynamic = caRandomUuidJs()

private val caThrowJs: dynamic = js("function (m) { throw m; }")
private fun caThrow(m: dynamic) {
    caThrowJs(m)
}

private val caCtxJs: dynamic = js("function () { return ctx; }")
private fun caCtx(): dynamic = caCtxJs()

private val caNewIntegerJs: dynamic = js("function (v) { return new java.lang.Integer(v); }")
private fun caNewInteger(v: dynamic): dynamic = caNewIntegerJs(v)

/** 与 QR 码逐位读取保持一致的 Java byte 运算。 */
private val caQrPixelJs: dynamic = js(
    "function (bytes, p) {" +
        " return !!(bytes[p >> 3] & new java.lang.Integer(1 << (p & 7)).byteValue());" +
        "}"
)
private fun caQrPixel(bytes: dynamic, p: dynamic): Boolean = caQrPixelJs(bytes, p)

private val caNewFile1Js: dynamic = js("function (p) { return new java.io.File(p); }")
private fun caNewFile1(p: dynamic): dynamic = caNewFile1Js(p)

private val caNewFile2Js: dynamic = js("function (p, c) { return new java.io.File(p, c); }")
private fun caNewFile2(p: dynamic, c: dynamic): dynamic = caNewFile2Js(p, c)

private val caNewFileOutputStreamJs: dynamic = js("function (f) { return new java.io.FileOutputStream(f); }")
private fun caNewFileOutputStream(f: dynamic): dynamic = caNewFileOutputStreamJs(f)

private val caExternalStorageDirJs: dynamic = js(
    "function () { return android.os.Environment.getExternalStorageDirectory(); }"
)
private fun caExternalStorageDir(): dynamic = caExternalStorageDirJs()

private val caNewIntentJs: dynamic = js("function (a) { return new android.content.Intent(a); }")
private fun caNewIntent(action: dynamic): dynamic = caNewIntentJs(action)

private val caNewShortcutIntentJs: dynamic = js(
    "function () {" +
        " return new android.content.Intent(\"com.xero.ca.DEBUG_EXEC\")" +
        "  .setComponent(new android.content.ComponentName(ctx.getPackageName(), \"com.xero.ca.MainActivity\"))" +
        "  .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);" +
        "}"
)
private fun caNewShortcutIntent(): dynamic = caNewShortcutIntentJs()

private val caNewViewIntentJs: dynamic = js(
    "function (uri) {" +
        " return new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(uri))" +
        "  .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);" +
        "}"
)
private fun caNewViewIntent(uri: dynamic): dynamic = caNewViewIntentJs(uri)

/** 旧壳子/启动器上 createShortcut 失败时使用的系统快捷方式接口。 */
private val caCreatePinnedShortcutJs: dynamic = js(
    "function (name, intent) {" +
        " var mgr = ctx.getSystemService(ctx.SHORTCUT_SERVICE);" +
        " var si = new android.content.pm.ShortcutInfo.Builder(ctx, name)" +
        "  .setShortLabel(name).setLongLabel(name).setIntent(intent)" +
        "  .setIcon(android.graphics.drawable.Icon.createWithResource(ctx, com.xero.ca.R.mipmap.icon_small))" +
        "  .build();" +
        " mgr.requestPinShortcut(si, null);" +
        "}"
)
private fun caCreatePinnedShortcut(name: dynamic, intent: dynamic) {
    caCreatePinnedShortcutJs(name, intent)
}

// ---------------- UI 构造 helper ----------------

private val caNewLinearLayoutJs: dynamic = js("function () { return new G.LinearLayout(ctx); }")
private val caNewFrameLayoutJs: dynamic = js("function () { return new G.FrameLayout(ctx); }")
private val caNewTextViewJs: dynamic = js("function () { return new G.TextView(ctx); }")
private val caNewImageViewJs: dynamic = js("function () { return new G.ImageView(ctx); }")
private val caNewListViewJs: dynamic = js("function () { return new G.ListView(ctx); }")
private val caNewGridViewJs: dynamic = js("function () { return new G.GridView(ctx); }")
private val caNewScrollViewJs: dynamic = js("function () { return new G.ScrollView(ctx); }")
private val caNewCanvasJs: dynamic = js("function (b) { return new G.Canvas(b); }")
private val caNewPaintJs: dynamic = js("function () { return new G.Paint(); }")
private val caNewPaintWithJs: dynamic = js("function (p) { return new G.Paint(p); }")
private val caNewRectJs: dynamic = js("function () { return new G.Rect(); }")
private val caNewColorDrawableJs: dynamic = js("function (c) { return new G.ColorDrawable(c); }")
private val caNewAbsParamsJs: dynamic = js(
    "function (w, h) { return new G.AbsListView.LayoutParams(w, h); }"
)
private val caNewLinearParams2Js: dynamic = js(
    "function (w, h) { return new G.LinearLayout.LayoutParams(w, h); }"
)
private val caNewLinearParams3Js: dynamic = js(
    "function (w, h, wt) { return new G.LinearLayout.LayoutParams(w, h, wt); }"
)
private val caNewFrameParams2Js: dynamic = js(
    "function (w, h) { return new G.FrameLayout.LayoutParams(w, h); }"
)
private val caNewClickListenerJs: dynamic = js(
    "function (f) { return new G.View.OnClickListener({ onClick: f }); }"
)
private val caNewLongClickListenerJs: dynamic = js(
    "function (f) { return new G.View.OnLongClickListener({ onLongClick: f }); }"
)
private val caNewTouchListenerJs: dynamic = js(
    "function (f) { return new G.View.OnTouchListener({ onTouch: f }); }"
)
private val caNewItemClickListenerJs: dynamic = js(
    "function (f) { return new G.AdapterView.OnItemClickListener({ onItemClick: f }); }"
)
private val caNewHeaderDecodedListenerJs: dynamic = js(
    "function (f) { return new G.ImageDecoder.OnHeaderDecodedListener({ onHeaderDecoded: f }); }"
)
private val caNewPopupPageJs: dynamic = js(
    "function (v, n) { return new PopupPage(v, n); }"
)
private val caNewSimpleListAdapter5Js: dynamic = js(
    "function (a, b, c, d, e) { return new SimpleListAdapter(a, b, c, d, e); }"
)
private val caSimpleGetControllerJs: dynamic = js(
    "function (a) { return SimpleListAdapter.getController(a); }"
)
private val caNewRhinoListAdapterJs: dynamic = js(
    "function (a, v) { return new RhinoListAdapter(a, v); }"
)

private fun caNewLinearLayout(): dynamic = caNewLinearLayoutJs()
private fun caNewFrameLayout(): dynamic = caNewFrameLayoutJs()
private fun caNewTextView(): dynamic = caNewTextViewJs()
private fun caNewImageView(): dynamic = caNewImageViewJs()
private fun caNewListView(): dynamic = caNewListViewJs()
private fun caNewGridView(): dynamic = caNewGridViewJs()
private fun caNewScrollView(): dynamic = caNewScrollViewJs()
private fun caNewCanvas(bitmap: dynamic): dynamic = caNewCanvasJs(bitmap)
private fun caNewPaint(): dynamic = caNewPaintJs()
private fun caNewPaintWith(paint: dynamic): dynamic = caNewPaintWithJs(paint)
private fun caNewRect(): dynamic = caNewRectJs()
private fun caNewColorDrawable(color: dynamic): dynamic = caNewColorDrawableJs(color)
private fun caNewAbsParams(w: dynamic, h: dynamic): dynamic = caNewAbsParamsJs(w, h)
private fun caNewLinearParams2(w: dynamic, h: dynamic): dynamic = caNewLinearParams2Js(w, h)
private fun caNewLinearParams3(w: dynamic, h: dynamic, weight: dynamic): dynamic =
    caNewLinearParams3Js(w, h, weight)
private fun caNewFrameParams2(w: dynamic, h: dynamic): dynamic = caNewFrameParams2Js(w, h)
private fun caNewClickListener(f: dynamic): dynamic = caNewClickListenerJs(f)
private fun caNewLongClickListener(f: dynamic): dynamic = caNewLongClickListenerJs(f)
private fun caNewTouchListener(f: dynamic): dynamic = caNewTouchListenerJs(f)
private fun caNewItemClickListener(f: dynamic): dynamic = caNewItemClickListenerJs(f)
private fun caNewHeaderDecodedListener(f: dynamic): dynamic = caNewHeaderDecodedListenerJs(f)
private fun caNewPopupPage(view: dynamic, name: dynamic): dynamic = caNewPopupPageJs(view, name)
private fun caNewSimpleListAdapter5(
    arr: dynamic, maker: dynamic, binder: dynamic, params: dynamic, readOnly: dynamic
): dynamic = caNewSimpleListAdapter5Js(arr, maker, binder, params, readOnly)
private fun caSimpleGetController(adapter: dynamic): dynamic = caSimpleGetControllerJs(adapter)
private fun caNewRhinoListAdapter(arr: dynamic, maker: dynamic): dynamic =
    caNewRhinoListAdapterJs(arr, maker)

private fun caObject(): dynamic = js("({})")
private fun caEmptyArray(): dynamic = js("[]")
private fun caArray(vararg items: dynamic): dynamic {
    val a = js("[]")
    var i = 0
    while (i < items.size) {
        a.push(items[i])
        i++
    }
    return a
}

// ---------------- 具名函数 / 动态 this helper ----------------

/** function self() */
private val caSelf0Js: dynamic = js("function (f) { return function self() { f(self); }; }")
/** function self(a) */
private val caSelf1Js: dynamic = js("function (f) { return function self(a) { f(self, a); }; }")
/** function self(a, b) */
private val caSelf2Js: dynamic = js("function (f) { return function self(a, b) { f(self, a, b); }; }")

/** function (a) { ... }, 并把调用时的真实 this 作为第一个参数传给 Kotlin。 */
private val caThis1Js: dynamic = js("function (f) { return function (a) { return f(this, a); }; }")
/** function (a, b) { ... }, 并把调用时的真实 this 作为第一个参数传给 Kotlin。 */
private val caThis2Js: dynamic = js("function (f) { return function (a, b) { return f(this, a, b); }; }")

private fun caMenuItem(text: dynamic, onclick: dynamic): dynamic {
    val o = caObject()
    o.text = text
    o.onclick = onclick
    return o
}

private fun caMenuItemHidden(text: dynamic, hidden: dynamic, onclick: dynamic): dynamic {
    val o = caObject()
    o.text = text
    o.hidden = hidden
    o.onclick = onclick
    return o
}

private fun caMenuGap(gap: dynamic): dynamic {
    val o = caObject()
    o.gap = gap
    return o
}

// ---------------- showSourceMan ----------------

private fun caShowSourceMan(self: dynamic, api: dynamic) {
    js("G").ui({
        try {
            caShowSourceManUi(self, api)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

private fun caShowSourceManUi(self: dynamic, api: dynamic) {
    if (!caTruthy(self.linear)) {
        self.contextMenu = caArray(
            caMenuItem("添加拓展包源…", { _v: dynamic, _tag: dynamic ->
                self.promptAddSource("")
                js("undefined")
            })
        )
        self.contextMenu[0].description = "填写源站根目录或 info.json 的地址"
        self.promptAddSource = { defaultValue: dynamic ->
            caSourcePromptAddSource(self, api, defaultValue)
            js("undefined")
        }
        self.refresh = { ->
            self.adpt.setArray(api.Library.getSources().slice())
            js("undefined")
        }
        self.vmaker = { holder: dynamic -> caSourceManVmaker(holder) }
        self.vbinder = { holder: dynamic, src: dynamic, _i: dynamic, _a: dynamic ->
            caSourceManVbinder(holder, src)
            js("undefined")
        }
        self.adpt = caSimpleGetController(
            caNewSimpleListAdapter5(caEmptyArray(), self.vmaker, self.vbinder, js("null"), true)
        )
        caBuildSourceManUi(self, api)
        self.popup = caNewPopupPage(self.linear, "ca.SourceMan")
        js("PWM").registerResetFlag(self, "linear")
    }
    self.refresh()
    self.popup.enter()
}

private fun caSourcePromptAddSource(self: dynamic, api: dynamic, defaultValue: dynamic) {
    val o = caObject()
    o.title = "添加拓展包源"
    o.description = "例如 https://ca.projectxero.top/clib/"
    o.defaultValue = caOr(defaultValue, "")
    o.callback = { text: dynamic ->
        js("Common").showProgressDialog({ dia: dynamic ->
            caSourceAddSource(self, api, text, dia)
            js("undefined")
        })
        js("undefined")
    }
    js("Common").showInputDialog(o)
}

private fun caSourceAddSource(self: dynamic, api: dynamic, text: dynamic, dia: dynamic) {
    var src: dynamic = js("null")
    var err: dynamic = js("null")
    dia.setText("正在读取源信息…")
    try {
        src = api.Library.addSource(text)
    } catch (e: dynamic) {
        err = e
    }
    js("G").ui({
        try {
            if (caTruthy(err)) {
                js("Common").toast(caAdd("添加失败：", err))
                self.promptAddSource(text)
            } else {
                js("Common").toast(caAdd("已添加源：", src.name))
                self.refresh()
            }
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

private fun caSourceManVmaker(holder: dynamic): dynamic {
    val layout = caNewLinearLayout()
    val text1 = caNewTextView()
    val text2 = caNewTextView()
    holder.text1 = text1
    holder.text2 = text2
    layout.setLayoutParams(caNewAbsParams(-1, -2))
    layout.setOrientation(js("G.LinearLayout.VERTICAL"))
    layout.setPadding(caDp(15), caDp(15), caDp(15), caDp(15))
    text1.setLayoutParams(caNewLinearParams2(-1, -2))
    text1.setEllipsize(js("G.TextUtils.TruncateAt.MIDDLE"))
    text1.setSingleLine(true)
    layout.addView(text1)
    text2.setPadding(0, caDp(5), 0, 0)
    text2.setLayoutParams(caNewLinearParams2(-1, -2))
    js("Common").applyStyle(text2, "textview_prompt", 1)
    layout.addView(text2)
    return layout
}

private fun caSourceManVbinder(holder: dynamic, src: dynamic) {
    val title = caEmptyArray()
    val detail = caEmptyArray()
    val name = caOr(src.name, src.url)
    if (caTruthy(src.builtin)) title.push("[官方] ")
    title.push(if (caTruthy(src.note)) src.note else name)
    title.push(if (caTruthy(src.enabled)) "（已启用）" else "（已禁用）")
    holder.text1.setText(title.join(""))
    js("Common").applyStyle(
        holder.text1,
        if (caTruthy(src.enabled)) "item_default" else "item_disabled",
        3
    )
    if (caTruthy(src.note)) detail.push(caAdd("原名：", name))
    detail.push(caAdd("地址 : ", src.url))
    detail.push("", if (caTruthy(src.builtin)) "内置源，不能禁用" else "点按可修改备注或禁用")
    holder.text2.setText(detail.join("\n"))
}

private fun caBuildSourceManUi(self: dynamic, api: dynamic) {
    self.linear = caNewLinearLayout()
    self.linear.setOrientation(js("G.LinearLayout.VERTICAL"))
    self.linear.setPadding(caDp(15), caDp(15), caDp(15), 0)
    js("Common").applyStyle(self.linear, "message_bg")

    self.header = caNewLinearLayout()
    self.header.setOrientation(js("G.LinearLayout.HORIZONTAL"))
    self.header.setPadding(0, 0, 0, caDp(10))
    self.header.setOnClickListener(caNewClickListener({ _v: dynamic ->
        try {
            val tag = caObject()
            tag.callback = { ->
                js("G").ui({
                    try {
                        self.refresh()
                    } catch (e: dynamic) {
                        js("erp")(e)
                    }
                    js("undefined")
                })
                js("undefined")
            }
            js("Common").showOperateDialog(self.contextMenu, tag)
            true
        } catch (e: dynamic) {
            js("erp")(e)
            js("undefined")
        }
    }))

    self.title = caNewTextView()
    self.title.setText("管理拓展包源")
    self.title.setGravity(caAdd(js("G.Gravity.LEFT"), js("G.Gravity.CENTER")))
    self.title.setPadding(caDp(10), 0, caDp(10), 0)
    js("Common").applyStyle(self.title, "textview_default", 4)
    self.header.addView(self.title, caNewLinearParams3(0, -2, 1.0))

    self.menu = caNewTextView()
    self.menu.setText("▼")
    self.menu.setPadding(caDp(10), 0, caDp(10), 0)
    self.menu.setGravity(js("G.Gravity.CENTER"))
    js("Common").applyStyle(self.menu, "button_highlight", 3)
    self.header.addView(self.menu, caNewLinearParams2(-2, -1))
    self.linear.addView(self.header, caNewLinearParams2(-1, -2))

    self.list = caNewListView()
    self.list.setAdapter(self.adpt.self)
    self.list.setOnItemClickListener(caNewItemClickListener({
        parent: dynamic, _view: dynamic, pos: dynamic, _id: dynamic ->
        try {
            caSourceManItemClick(self, api, parent, pos)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }))
    self.linear.addView(self.list, caNewLinearParams3(-1, 0, 1.0))

    self.exit = caNewTextView()
    self.exit.setText("关闭")
    self.exit.setGravity(js("G.Gravity.CENTER"))
    self.exit.setPadding(caDp(10), caDp(20), caDp(10), caDp(20))
    js("Common").applyStyle(self.exit, "button_critical", 3)
    self.exit.setOnClickListener(caNewClickListener({ _v: dynamic ->
        try {
            self.popup.exit()
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }))
    self.linear.addView(self.exit, caNewLinearParams2(-1, -2))
}

private fun caSourceManItemClick(self: dynamic, api: dynamic, parent: dynamic, pos: dynamic) {
    val src = parent.getAdapter().getItem(pos)
    val mnu = caEmptyArray()
    val noteItem = caObject()
    noteItem.text = if (caTruthy(src.note)) "编辑备注…" else "添加备注…"
    noteItem.onclick = { _v: dynamic, _tag: dynamic ->
        val o = caObject()
        o.title = "拓展包源备注"
        o.description = src.url
        o.defaultValue = caOr(src.note, "")
        o.callback = { text: dynamic ->
            api.Library.setSourceNote(src.url, text)
            js("Common").toast(if (caTruthy(src.note)) "备注已保存" else "已清除备注")
            js("undefined")
        }
        js("Common").showInputDialog(o)
        js("undefined")
    }
    mnu.push(noteItem)
    if (!caTruthy(src.builtin)) {
        val enabledItem = caObject()
        enabledItem.text = if (caTruthy(src.enabled)) "禁用该拓展包源" else "启用该拓展包源"
        enabledItem.onclick = { _v: dynamic, _tag: dynamic ->
            try {
                api.Library.setSourceEnabled(src.url, !caTruthy(src.enabled))
            } catch (err: dynamic) {
                js("Common").toast(caString(err))
            }
            js("undefined")
        }
        mnu.push(enabledItem)

        val removeItem = caObject()
        removeItem.text = "删除该拓展包源"
        removeItem.onclick = { _v: dynamic, _tag: dynamic ->
            val o = caObject()
            o.title = "删除拓展包源？"
            o.description = src.url
            o.callback = { id: dynamic ->
                if (caLooseNe(id, 0)) {
                    js("undefined")
                } else {
                    try {
                        api.Library.removeSource(src.url)
                        js("Common").toast("已删除")
                    } catch (err: dynamic) {
                        js("Common").toast(caString(err))
                    }
                    js("undefined")
                }
            }
            js("Common").showConfirmDialog(o)
            js("undefined")
        }
        mnu.push(removeItem)
    }
    val tag = caObject()
    tag.data = src
    tag.callback = { ->
        js("G").ui({
            try {
                self.refresh()
            } catch (e: dynamic) {
                js("erp")(e)
            }
            js("undefined")
        })
        js("undefined")
    }
    js("Common").showOperateDialog(mnu, tag)
}

// ---------------- showLibraryMan ----------------

private fun caShowLibraryMan(self: dynamic, api: dynamic, callback: dynamic) {
    js("G").ui({
        try {
            caShowLibraryManUi(self, api, callback)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

private fun caShowLibraryManUi(self: dynamic, api: dynamic, callback: dynamic) {
    if (!caTruthy(self.linear)) {
        self.contextMenu = caLibraryContextMenu(self, api)
        self.itemMenu = caLibraryItemMenu(self, api)
        self.enabledMenu = caLibraryEnabledMenu(self, api)
        self.disabledMenu = caLibraryDisabledMenu(self, api)
        self.errMenu = caLibraryErrMenu(self, api)
        self.vmaker = { holder: dynamic -> caLibraryManVmaker(holder) }
        self.vbinder = { holder: dynamic, e: dynamic, _i: dynamic, _a: dynamic ->
            caLibraryManVbinder(api, holder, e)
            js("undefined")
        }
        self.refresh = { ->
            caLibraryManRefresh(self, api)
            js("undefined")
        }
        self.postTask = { f: dynamic ->
            caLibraryManPostTask(self, api, f)
        }
        self.processing = false
        self.adpt = caSimpleGetController(
            caNewSimpleListAdapter5(caEmptyArray(), self.vmaker, self.vbinder, js("null"), true)
        )
        caBuildLibraryManUi(self, api)
        self.popup = caNewPopupPage(self.linear, "ca.LibraryManage")
        self.popup.on("exit", { ->
            if (caTruthy(self.callback)) self.callback()
            js("undefined")
        })
        js("PWM").registerResetFlag(self, "linear")
    }
    self.refresh()
    self.callback = callback
    self.popup.enter()
}

private fun caBuildLibraryManUi(self: dynamic, api: dynamic) {
    self.linear = caNewLinearLayout()
    self.linear.setOrientation(js("G.LinearLayout.VERTICAL"))
    self.linear.setPadding(caDp(15), caDp(15), caDp(15), 0)
    js("Common").applyStyle(self.linear, "message_bg")

    self.header = caNewLinearLayout()
    self.header.setOrientation(js("G.LinearLayout.HORIZONTAL"))
    self.header.setPadding(0, 0, 0, caDp(10))
    self.header.setOnClickListener(caNewClickListener({ _v: dynamic ->
        try {
            val tag = caObject()
            tag.callback = { ->
                js("G").ui({
                    try {
                        self.refresh()
                    } catch (e: dynamic) {
                        js("erp")(e)
                    }
                    js("undefined")
                })
                js("undefined")
            }
            js("Common").showOperateDialog(self.contextMenu, tag)
            true
        } catch (e: dynamic) {
            js("erp")(e)
            js("undefined")
        }
    }))

    self.title = caNewTextView()
    self.title.setText("管理拓展包")
    self.title.setGravity(caAdd(js("G.Gravity.LEFT"), js("G.Gravity.CENTER")))
    self.title.setPadding(caDp(10), 0, caDp(10), 0)
    js("Common").applyStyle(self.title, "textview_default", 4)
    self.header.addView(self.title, caNewLinearParams3(0, -2, 1.0))

    self.menu = caNewTextView()
    self.menu.setText("▼")
    self.menu.setPadding(caDp(10), 0, caDp(10), 0)
    self.menu.setGravity(js("G.Gravity.CENTER"))
    js("Common").applyStyle(self.menu, "button_highlight", 3)
    self.header.addView(self.menu, caNewLinearParams2(-2, -1))
    self.linear.addView(self.header, caNewLinearParams2(-1, -2))

    self.list = caNewListView()
    self.list.setAdapter(self.adpt.self)
    self.list.setOnItemClickListener(caNewItemClickListener({
        parent: dynamic, _view: dynamic, pos: dynamic, _id: dynamic ->
        try {
            caLibraryManItemClick(self, api, parent, pos)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }))
    self.linear.addView(self.list, caNewLinearParams3(-1, 0, 1.0))

    self.exit = caNewTextView()
    self.exit.setText("关闭")
    self.exit.setGravity(js("G.Gravity.CENTER"))
    self.exit.setPadding(caDp(10), caDp(20), caDp(10), caDp(20))
    js("Common").applyStyle(self.exit, "button_critical", 3)
    self.exit.setOnClickListener(caNewClickListener({ _v: dynamic ->
        try {
            self.popup.exit()
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }))
    self.linear.addView(self.exit, caNewLinearParams2(-1, -2))
}

private fun caLibraryContextMenu(self: dynamic, api: dynamic): dynamic {
    val importItem = caObject()
    importItem.text = "从文件中导入"
    importItem.description = "导入外置拓展包"
    importItem.onclick = { _v: dynamic, _tag: dynamic ->
        caLibraryImportFiles(self, api)
        js("undefined")
    }
    importItem.hidden = { -> caGe(api.settings.securityLevel, 4) }

    val openItem = caObject()
    openItem.text = "从外部存储加载"
    openItem.description = "从外部存储中加载外置拓展包"
    openItem.onclick = { _v: dynamic, _tag: dynamic ->
        caLibraryOpenFiles(self, api)
        js("undefined")
    }
    openItem.hidden = { -> caLooseNe(api.settings.securityLevel, 0) }

    val createItem = caObject()
    createItem.text = "新建拓展包"
    createItem.description = "新建一个不包含内容的空包"
    createItem.onclick = { _v: dynamic, _tag: dynamic ->
        caLibraryCreateFile(self, api)
        js("undefined")
    }
    createItem.hidden = { -> caLooseNe(api.settings.securityLevel, 0) }

    val refreshItem = caObject()
    refreshItem.text = "刷新"
    refreshItem.description = "刷新所有的拓展包"
    refreshItem.onclick = { _v: dynamic, _tag: dynamic ->
        api.Library.clearCache()
        self.postTask({ cb: dynamic ->
            cb(true, { ->
                js("Common").toast("刷新成功")
                js("undefined")
            })
            js("undefined")
        })
        js("undefined")
    }

    val updateItem = caObject()
    updateItem.text = "检测更新"
    updateItem.description = "连接网络检测所有拓展包是否有更新"
    updateItem.hidden = { -> caLooseNe(api.settings.libraryAutoUpdate, 0) }
    updateItem.onclick = { _v: dynamic, _tag: dynamic ->
        self.postTask({ cb: dynamic ->
            caLibraryUpdateLibraries(api, cb, 1)
            js("undefined")
        })
        js("undefined")
    }

    val updateDownloadItem = caObject()
    updateDownloadItem.text = "检测更新并下载"
    updateDownloadItem.description = "连接网络检测所有拓展包是否有更新，如果有就下载更新"
    updateDownloadItem.hidden = { -> caLooseNe(api.settings.libraryAutoUpdate, 1) }
    updateDownloadItem.onclick = { _v: dynamic, _tag: dynamic ->
        self.postTask({ cb: dynamic ->
            caLibraryUpdateLibraries(api, cb, 2)
            js("undefined")
        })
        js("undefined")
    }

    val securityItem = caSecurityMenuItem(self, api)

    val versionItem = caObject()
    versionItem.text = "切换版本"
    versionItem.description = "切换命令所属版本"
    versionItem.onclick = { _v: dynamic, _tag: dynamic ->
        js("NeteaseAdapter").switchVersion({ ->
            api.checkFeatures()
            self.postTask({ cb: dynamic ->
                cb(true, { ->
                    js("Common").toast(caAdd("版本已切换为", caAdd(caGetMinecraftVersion(), "。")))
                    js("undefined")
                })
                js("undefined")
            })
            js("undefined")
        })
        js("undefined")
    }

    val ignoreItem = caObject()
    ignoreItem.text = "忽略版本"
    ignoreItem.description = "暂时忽略版本限制"
    ignoreItem.hidden = { ->
        caLooseNe(api.settings.securityLevel, 0) || caTruthy(api.Library.ignoreVersion)
    }
    ignoreItem.onclick = { _v: dynamic, _tag: dynamic ->
        self.postTask({ cb: dynamic ->
            api.Library.ignoreVersion = true
            api.checkFeatures()
            cb(true, { ->
                js("Common").toast("版本限制已关闭")
                js("undefined")
            })
            js("undefined")
        })
        js("undefined")
    }

    val restoreIgnoreItem = caObject()
    restoreIgnoreItem.text = "取消忽略版本"
    restoreIgnoreItem.description = "取消忽略版本限制"
    restoreIgnoreItem.hidden = { ->
        caLooseNe(api.settings.securityLevel, 0) || !caTruthy(api.Library.ignoreVersion)
    }
    restoreIgnoreItem.onclick = { _v: dynamic, _tag: dynamic ->
        self.postTask({ cb: dynamic ->
            api.Library.ignoreVersion = false
            api.checkFeatures()
            cb(true, { ->
                js("Common").toast("版本限制已开启")
                js("undefined")
            })
            js("undefined")
        })
        js("undefined")
    }

    val restoreItem = caObject()
    restoreItem.text = "恢复默认"
    restoreItem.description = "将拓展包列表恢复为默认"
    restoreItem.onclick = { _v: dynamic, _tag: dynamic ->
        self.postTask({ cb: dynamic ->
            api.settings.enabledLibrarys.concat(
                api.settings.disabledLibrarys,
                api.settings.coreLibrarys
            ).forEach({ e: dynamic ->
                api.Library.removeLibrary(e)
                js("undefined")
            })
            api.Library.recommended.forEach({ e: dynamic ->
                api.Library.enableLibrary(e)
                js("undefined")
            })
            api.Library.defaultDisabled.forEach({ e: dynamic ->
                api.Library.disableLibrary(e)
                js("undefined")
            })
            api.Library.clearCache()
            cb(true, { ->
                js("Common").toast("已恢复为默认拓展包列表")
                js("undefined")
            })
            js("undefined")
        })
        js("undefined")
    }

    return caArray(
        importItem, openItem, createItem, refreshItem, updateItem, updateDownloadItem,
        securityItem, versionItem, ignoreItem, restoreIgnoreItem, restoreItem
    )
}

private fun caSecurityMenuItem(self: dynamic, api: dynamic): dynamic {
    val details = caObject()
    val level0 = caObject()
    level0.text = "禁用"
    level0.summary = "完全加载，不使用安全机制"
    level0.confirm = "切到这一档会关闭所有来源检查：任何文件（包括没打包、没签名的 .js/.json）都会被当作拓展包执行。\n恶意文件可以借此运行任意代码，确定要切换吗？"
    details["0"] = level0

    val level1 = caObject()
    level1.text = "低"
    level1.summary = "只放行打包过的拓展包"
    level1.confirm = "这一档会额外允许加载未签名的锁定包（LIBRARY），这类包没有来源保证。\n如果手上的包都已经签名，建议留在「中」。是否继续？"
    details["1"] = level1

    val level2 = caObject()
    level2.text = "中"
    level2.summary = "(推荐) 只加载签名包：官方 + 社区"
    level2.confirm = "「中」会加载社区签名的拓展包，它们的来源由各自拓展包源保证，没有官方背书。是否继续？"
    details["2"] = level2

    val level3 = caObject()
    level3.text = "高"
    level3.summary = "只加载官方签名"
    level3.confirm = "「高」会拒绝社区签名的拓展包（已安装的也不再加载），只保留官方签名和内置包。是否继续？"
    details["3"] = level3

    val level4 = caObject()
    level4.text = "严格"
    level4.summary = "只加载内置命令库"
    level4.confirm = "「严格」下只加载内置命令库，所有外部拓展包（包括已安装的）都不会生效。是否继续？"
    details["4"] = level4

    val item = caObject()
    item.text = "设置安全级别"
    item.secLevels = caArray(0, 1, 2, 3, 4)
    item.secLevelDetails = details
    item.onclick = caThis2Js { thisObj: dynamic, _v: dynamic, _tag: dynamic ->
        caSecurityLevelClick(self, api, thisObj)
        js("undefined")
    }
    item.hidden = caThis1Js { thisObj: dynamic, _tag: dynamic ->
        caSecurityLevelHidden(api, thisObj)
    }
    return item
}

private fun caSecurityLevelClick(self: dynamic, api: dynamic, item: dynamic) {
    val t = item.secLevels.indexOf(api.settings.securityLevel)
    val o = caObject()
    o.max = caSub(caLength(item.secLevels), 1)
    o.progress = if (caLt(t, 0)) 2 else t
    o.prompt = { progress: dynamic ->
        val d = item.secLevelDetails[item.secLevels[progress]]
        if (caTruthy(d)) caAdd(caAdd(d.text, "\n\n"), d.summary) else "未知安全级别"
    }
    o.callback = caThis1Js { thisObj: dynamic, progress: dynamic ->
        caSecurityLevelCallback(self, api, item, thisObj, progress)
        js("undefined")
    }
    o._refresh = caThis1Js { _thisObj: dynamic, progress: dynamic ->
        caSecurityLevelRefresh(self, api, item, progress)
        js("undefined")
    }
    js("Common").showSlider(o)
}

private fun caSecurityLevelCallback(
    self: dynamic, api: dynamic, item: dynamic, thisObj: dynamic, progress: dynamic
) {
    val target = item.secLevels[progress]
    val d = item.secLevelDetails[target]
    if (caLooseEq(target, api.settings.securityLevel)) return
    if (caTruthy(d.confirm) && caLt(target, api.settings.securityLevel)) {
        val o = caObject()
        o.title = "放宽安全级别"
        o.description = d.confirm
        o.callback = { id: dynamic ->
            if (caLooseEq(id, 0)) thisObj._refresh(progress)
            js("undefined")
        }
        js("Common").showConfirmDialog(o)
    } else {
        thisObj._refresh(progress)
    }
}

private fun caSecurityLevelRefresh(self: dynamic, api: dynamic, item: dynamic, progress: dynamic) {
    api.settings.securityLevel = item.secLevels[progress]
    api.Library.clearCache()
    self.postTask({ cb: dynamic ->
        cb(true, { ->
            val d = item.secLevelDetails[api.settings.securityLevel]
            js("Common").toast(
                caAdd(
                    caAdd("安全级别已切换为「", if (caTruthy(d)) d.text else "未知"),
                    "」"
                )
            )
            js("undefined")
        })
        js("undefined")
    })
}

private fun caSecurityLevelHidden(api: dynamic, item: dynamic): Boolean {
    val d = item.secLevelDetails[api.settings.securityLevel]
    item.description = if (caTruthy(d)) {
        caAdd("当前安全级别 ", d.text)
    } else {
        "当前安全级别未知"
    }
    return false
}

private fun caLibraryImportFiles(self: dynamic, api: dynamic) {
    val o = caObject()
    o.mimeType = "*/*"
    o.hint = "caclib/*/**"
    o.allowMultiple = true
    o.file = { files: dynamic ->
        self.postTask({ cb: dynamic ->
            val results = files.map({ f: dynamic ->
                api.Library.addLibrary(caString(js("ExternalStorage").toUri(f)))
            })
            val message = api.Library.summarizeAddResults(results)
            val done = { ->
                cb(true, { ->
                    js("Common").toast(message)
                    js("undefined")
                })
                js("undefined")
            }
            if (!caTruthy(api.Library.initLibrary(done))) done()
            js("undefined")
        })
        js("undefined")
    }
    js("ExternalStorage").showImportActions(o)
}

private fun caLibraryOpenFiles(self: dynamic, api: dynamic) {
    val o = caObject()
    o.mimeType = "*/*"
    o.allowMultiple = true
    o.callback = { uriList: dynamic ->
        uriList.forEach({ uri: dynamic ->
            js("ExternalStorage").tryTakeUriPermission(uri)
            js("undefined")
        })
        self.postTask({ cb: dynamic ->
            val results = uriList.map({ uri: dynamic ->
                api.Library.addLibrary(caString(uri))
            })
            val message = api.Library.summarizeAddResults(results)
            val done = { ->
                cb(true, { ->
                    js("Common").toast(message)
                    js("undefined")
                })
                js("undefined")
            }
            if (!caTruthy(api.Library.initLibrary(done))) done()
            js("undefined")
        })
        js("undefined")
    }
    js("ExternalStorage").showOpenActions(o)
}

private fun caLibraryCreateFile(self: dynamic, api: dynamic) {
    val o = caObject()
    o.mimeType = "application/json"
    o.hint = "新建拓展包.json"
    o.callback = { uri: dynamic ->
        js("ExternalStorage").tryTakeUriPermission(uri)
        self.postTask({ cb: dynamic ->
            try {
                val lib = caObject()
                lib.name = "新建拓展包"
                lib.author = "作者名"
                lib.description = caAdd("此处填写介绍，可留空，新建于", caNewDateLocaleString())
                lib.uuid = caRandomUuid()
                lib.version = caArray(0, 0, 1)
                lib.require = caEmptyArray()
                js("ExternalStorage").writeFileContent(
                    uri, caJsonStringify(lib, js("null"), "\t")
                )
                api.Library.enableLibrary(caString(uri))
                cb(true, { ->
                    js("Common").toast("拓展包已新建")
                    js("undefined")
                })
            } catch (e: dynamic) {
                js("Common").toast(caAdd("文件保存失败，无法新建\n", e))
                cb(false)
            }
            js("undefined")
        })
        js("undefined")
    }
    js("ExternalStorage").showSaveActions(o)
}

private fun caLibraryUpdateLibraries(api: dynamic, cb: dynamic, mode: dynamic) {
    js("Threads").run({
        try {
            val count = api.Library.updateLibraries(mode)
            if (caGt(count, 0)) {
                js("Common").toast(caAdd("检测到", caAdd(count, "个拓展包有更新")))
            } else {
                js("Common").toast("所有拓展包都是最新的")
            }
            cb(false)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

private fun caLibraryItemMenu(self: dynamic, api: dynamic): dynamic {
    val removeItem = caObject()
    removeItem.text = "移除"
    removeItem.description = "将该拓展包从列表中移除"
    removeItem.hidden = { tag: dynamic -> caLooseEq(tag.data.mode, 0) }
    removeItem.onclick = { _v: dynamic, tag: dynamic ->
        self.postTask({ cb: dynamic ->
            api.Library.removeLibrary(tag.data.src)
            cb(true, { ->
                js("Common").toast("该拓展包已从列表中移除")
                js("undefined")
            })
            js("undefined")
        })
        js("undefined")
    }

    val infoItem = caObject()
    infoItem.text = "查看信息"
    infoItem.description = "查看该拓展包的相关信息"
    infoItem.onclick = { _v: dynamic, tag: dynamic ->
        caLibraryShowInfo(api, tag)
        js("undefined")
    }

    return caArray(removeItem, infoItem)
}

private fun caLibraryShowInfo(api: dynamic, tag: dynamic) {
    val uri = js("ExternalStorage").toUri(tag.data.src)
    val s = caEmptyArray()
    s.push(caAdd("名称 : ", tag.data.name))
    if (caTruthy(js("ExternalStorage").isFile(uri))) {
        s.push(caAdd("位置 : ", js("ExternalStorage").uriToReadablePath(uri)))
        s.push(caAdd("大小 : ", js("ExternalStorage").getLengthString(uri)))
        s.push(caAdd("时间 : ", js("ExternalStorage").getLastModifiedString(uri)))
    }
    if (caTruthy(tag.data.updateState)) {
        val state = tag.data.updateState
        val stateStr = if (caLooseEq(state, "checking")) {
            "正在检测"
        } else if (caLooseEq(state, "latest")) {
            "已是最新版"
        } else if (caLooseEq(state, "unavailable")) {
            "更新源不可用"
        } else if (caLooseEq(state, "ready")) {
            "已准备更新"
        } else if (caLooseEq(state, "waitForUser")) {
            "等待用户手动更新"
        } else if (caLooseEq(state, "error")) {
            "下载更新出错"
        } else if (caLooseEq(state, "finished")) {
            "已下载更新"
        } else {
            "未知"
        }
        s.push(caAdd("更新状态 : ", stateStr))
    }
    if (!caTruthy(tag.data.disabled) && !caTruthy(tag.data.hasError) && caTruthy(tag.data.stat)) {
        s.push("", tag.data.stat.toString())
    }
    js("Common").showTextDialog(s.join("\n"))
}

private fun caLibraryEnabledMenu(self: dynamic, api: dynamic): dynamic {
    val itemMenu = self.itemMenu

    val updateItem = caObject()
    updateItem.text = "检测更新"
    updateItem.description = "如果可行，连接服务器检测是否有更新"
    updateItem.hidden = { tag: dynamic ->
        caTruthy(tag.data.installer) || caLooseEq(tag.data.mode, 0)
    }
    updateItem.onclick = { _v: dynamic, tag: dynamic ->
        self.postTask({ cb: dynamic ->
            caLibraryCheckOneUpdate(api, tag, cb)
            js("undefined")
        })
        js("undefined")
    }

    val editItem = caObject()
    editItem.text = "编辑"
    editItem.description = "用JSON编辑器编辑该拓展包"
    editItem.hidden = { tag: dynamic ->
        caLooseNe(api.settings.securityLevel, 0) || caLooseNe(tag.data.mode, 1)
    }
    editItem.onclick = { _v: dynamic, tag: dynamic ->
        caLibraryEdit(api, self, tag)
        js("undefined")
    }

    val exportItem = caObject()
    exportItem.text = "导出"
    exportItem.description = "将该拓展包保存到一个新文件里"
    exportItem.hidden = { tag: dynamic -> caTruthy(tag.data.installer) }
    exportItem.onclick = { _v: dynamic, tag: dynamic ->
        caLibraryExport(api, tag)
    }

    val shortcutItem = caObject()
    shortcutItem.text = "重新创建快捷方式"
    shortcutItem.description = "向桌面重新请求为安装器创建快捷方式"
    shortcutItem.hidden = { tag: dynamic -> caLibraryShortcutHidden(api, tag) }
    shortcutItem.onclick = { _v: dynamic, tag: dynamic ->
        self.postTask({ cb: dynamic ->
            js("Threads").run({
                caLibraryCreateShortcut(api, tag, cb)
                js("undefined")
            })
            js("undefined")
        })
        js("undefined")
    }

    val copyItem = caObject()
    copyItem.text = "创建副本"
    copyItem.description = "创建该拓展包的副本（副本不会被认为与原拓展包相同）"
    copyItem.hidden = { tag: dynamic ->
        caTruthy(tag.data.installer) || caLooseNe(api.settings.securityLevel, 0) ||
            caTruthy(tag.data.hasError) || caGe(tag.data.mode, 2)
    }
    copyItem.onclick = { _v: dynamic, tag: dynamic ->
        caLibraryCreateCopy(api, self, tag)
        js("undefined")
    }

    val lockItem = caObject()
    lockItem.text = "锁定"
    lockItem.description = "锁定拓展包，使其不能被编辑"
    lockItem.hidden = { tag: dynamic ->
        caTruthy(tag.data.installer) || caLooseNe(api.settings.securityLevel, 0) ||
            caTruthy(tag.data.hasError) || caLooseNe(tag.data.mode, 1)
    }
    lockItem.onclick = { _v: dynamic, tag: dynamic ->
        caLibraryLock(api, self, tag)
        js("undefined")
    }

    val sortItem = caObject()
    sortItem.text = "排序"
    sortItem.description = "调整拓展包加载的顺序"
    sortItem.onclick = { _v: dynamic, tag: dynamic ->
        caLibrarySort(api, self, tag)
        js("undefined")
    }

    val disableItem = caObject()
    disableItem.text = "停用"
    disableItem.description = "停用该拓展包"
    disableItem.onclick = { _v: dynamic, tag: dynamic ->
        self.postTask({ cb: dynamic ->
            api.Library.clearCache(tag.data.src)
            api.Library.disableLibrary(tag.data.src)
            cb(true, { ->
                js("Common").toast("拓展包已停用")
                js("undefined")
            })
            js("undefined")
        })
        js("undefined")
    }

    return caArray(
        updateItem, editItem, exportItem, shortcutItem, copyItem, lockItem, sortItem, disableItem
    ).concat(itemMenu)
}

private fun caLibraryDisabledMenu(self: dynamic, api: dynamic): dynamic {
    val enableItem = caObject()
    enableItem.text = "启用"
    enableItem.description = "启用该拓展包"
    enableItem.onclick = { _v: dynamic, tag: dynamic ->
        self.postTask({ cb: dynamic ->
            api.Library.enableLibrary(tag.data.src)
            cb(true, { ->
                js("Common").toast("拓展包已启用")
                js("undefined")
            })
            js("undefined")
        })
        js("undefined")
    }
    return caArray(enableItem).concat(self.itemMenu)
}

private fun caLibraryErrMenu(self: dynamic, api: dynamic): dynamic {
    val stackItem = caObject()
    stackItem.text = "查看堆栈"
    stackItem.onclick = { _v: dynamic, tag: dynamic ->
        if (caInstanceOf(tag.data.error, js("Object")) && caTruthy(tag.data.error.stack)) {
            js("Common").showTextDialog(caString(tag.data.error.stack))
            js("undefined")
        } else {
            js("Common").toast("错误堆栈不存在")
            true
        }
    }
    return caArray(stackItem).concat(self.enabledMenu)
}

private fun caLibraryCheckOneUpdate(api: dynamic, tag: dynamic, cb: dynamic) {
    js("Threads").run({
        try {
            api.Library.requestUpdateInfo(tag.data, { statusCode: dynamic, arg1: dynamic, arg2: dynamic ->
                if (caLooseEq(statusCode, 1)) {
                    js("Common").toast(
                        caAdd(
                            caAdd("检测到更新：\n", api.Library.versionToString(arg2.version)),
                            caAdd(" -> ", api.Library.versionToString(arg1.version))
                        )
                    )
                    api.Library.clearCache(tag.data.src)
                    api.Library.doUpdate(arg1, arg2, { statusMessage: dynamic, arg_1: dynamic ->
                        caLibraryDoUpdateCallback(api, tag, cb, statusMessage, arg_1)
                        js("undefined")
                    })
                } else {
                    if (caLooseEq(statusCode, -2)) {
                        js("Common").toast(caAdd("检测更新失败\n", arg1))
                    } else if (caLooseEq(statusCode, 0)) {
                        js("Common").toast(
                            caAdd(
                                caAdd(caAdd("拓展包“", tag.data.name), "”已是最新版本："),
                                api.Library.versionToString(arg1.version)
                            )
                        )
                    } else {
                        js("Common").toast(caAdd(caAdd("拓展包“", tag.data.name), "”没有更新数据"))
                    }
                    cb(false)
                }
                js("undefined")
            })
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

private fun caLibraryDoUpdateCallback(
    api: dynamic, tag: dynamic, cb: dynamic, statusMessage: dynamic, arg1: dynamic
) {
    if (caLooseEq(statusMessage, "downloadFromUri")) {
        cb(true)
        val o = caObject()
        o.title = caAdd(caAdd("拓展包“", tag.data.name), "”请求访问下方的链接，确定访问？")
        o.description = arg1
        o.callback = { id: dynamic ->
            if (caLooseNe(id, 0)) {
                js("undefined")
            } else {
                try {
                    js("AndroidBridge").startActivity(caNewViewIntent(arg1))
                    js("undefined")
                } catch (e: dynamic) {
                    js("Log").e(e)
                    js("Common").toast("打开链接失败")
                    js("undefined")
                }
            }
        }
        js("Common").showConfirmDialog(o)
    } else if (caLooseEq(statusMessage, "downloadError")) {
        js("Common").toast(caAdd("更新失败\n", arg1))
        cb(false)
    } else if (caLooseEq(statusMessage, "completeDownload")) {
        cb(true, { ->
            js("Common").toast(
                caAdd(
                    caAdd(caAdd("更新完成：拓展包“", tag.data.name), "”已是最新版本："),
                    api.Library.versionToString(arg1.version)
                )
            )
            js("undefined")
        })
    }
}

private fun caLibraryEdit(api: dynamic, self: dynamic, tag: dynamic) {
    val uri = js("ExternalStorage").toUri(tag.data.src)
    var content: dynamic
    try {
        content = js("ExternalStorage").readFileContent(uri, "UTF-8")
    } catch (e: dynamic) {
        js("Common").toast(caAdd("此拓展包无法使用JSON编辑器进行编辑\n", e))
        return
    }
    var source: dynamic
    try {
        source = js("JSON").parse(content)
    } catch (e: dynamic) {
        js("Common").toast(caAdd("此拓展包无法使用JSON编辑器进行编辑\n", e))
        return
    }
    if (!caInstanceOf(source, js("Object"))) source = caObject()
    val o = caObject()
    o.source = source
    o.rootname = "拓展包"
    o.update = { ->
        self.postTask({ cb: dynamic ->
            try {
                js("ExternalStorage").writeFileContent(
                    uri, caJsonStringify(source, js("null"), js("null"))
                )
                api.Library.clearCache(tag.data.src)
                cb(true, { ->
                    js("Common").toast("加载成功！")
                    js("undefined")
                })
            } catch (e: dynamic) {
                js("Common").toast(caAdd("文件保存失败\n", e))
                cb(false)
            }
            js("undefined")
        })
        js("undefined")
    }
    js("JSONEdit").show(o)
}

private fun caLibraryExport(api: dynamic, tag: dynamic): dynamic {
    if (caTruthy(tag.data.hasError)) {
        js("Common").toast(
            caAdd(caAdd("拓展包“", tag.data.name), "”有错误，请先解决错误再导出")
        )
        return true
    }
    val o = caObject()
    o.mimeType = "application/json"
    o.hint = caAdd(tag.data.name, ".json")
    if (caLooseEq(tag.data.mode, 0)) {
        o.export = { uri: dynamic ->
            js("ExternalStorage").writeFileContent(
                uri,
                caJsonStringify(api.Library.inner[tag.data.src], js("null"), js("null"))
            )
            js("undefined")
        }
    } else {
        o.export = tag.data.src
    }
    js("ExternalStorage").showExportActions(o)
    return js("undefined")
}

private fun caLibraryShortcutHidden(api: dynamic, tag: dynamic): Boolean {
    if (!caTruthy(tag.data.installer)) return true
    val rec = api.Library.getInstallerRecord(tag.data.src)
    return !(caTruthy(rec) && caTruthy(rec.shortcutName))
}

private fun caLibraryCreateShortcut(api: dynamic, tag: dynamic, cb: dynamic) {
    try {
        val rec = caOr(api.Library.getInstallerRecord(tag.data.src), caObject())
        val name = caOr(rec.shortcutName, caOr(tag.data.name, "命令助手"))
        val intent = caNewShortcutIntent()
        try {
            js("AndroidBridge").createShortcut(intent, name, js("com.xero.ca.R.mipmap.icon_small"))
        } catch (e: dynamic) {
            // 旧壳子或部分启动器上 createShortcut 会失败，退回系统快捷方式接口
            caCreatePinnedShortcut(name, intent)
        }
        cb(true, { ->
            js("Common").toast("已向桌面请求创建快捷方式\n如果没反应，可在桌面长按空白处手动添加")
            js("undefined")
        })
    } catch (e: dynamic) {
        js("erp")(e)
        cb(false)
    }
}

private fun caLibraryCreateCopy(api: dynamic, self: dynamic, tag: dynamic) {
    val o = caObject()
    o.mimeType = "application/json"
    o.hint = caAdd(tag.data.name, ".json")
    o.callback = { uri: dynamic ->
        self.postTask({ cb: dynamic ->
            caLibraryCreateCopyTask(api, tag, cb, uri)
            js("undefined")
        })
        js("undefined")
    }
    js("ExternalStorage").showSaveActions(o)
}

private fun caLibraryCreateCopyTask(api: dynamic, tag: dynamic, cb: dynamic, uri: dynamic) {
    val uriStr = caString(uri)
    var lib: dynamic
    if (caLooseEq(tag.data.mode, 0)) {
        lib = js("Object").copy(api.Library.inner[tag.data.src])
    } else {
        try {
            val origUri = js("ExternalStorage").toUri(tag.data.src)
            val content = js("ExternalStorage").readFileContent(origUri, "UTF-8")
            lib = js("JSON").parse(content)
            if (!caInstanceOf(lib, js("Object"))) caThrow("格式错误")
        } catch (e: dynamic) {
            js("Common").toast(caAdd("此拓展包无法创建副本\n", e))
            cb(false)
            return
        }
    }
    lib.name = caAdd(caString(lib.name), " 的副本")
    lib.uuid = caRandomUuid()
    try {
        js("ExternalStorage").writeFileContent(
            uri, caJsonStringify(lib, js("null"), js("null"))
        )
        api.Library.clearCache(uriStr)
        api.Library.enableLibrary(uriStr)
        cb(true, { ->
            js("Common").toast(caAdd(caAdd("拓展包“", tag.data.name), "”的副本已创建"))
            js("undefined")
        })
    } catch (e: dynamic) {
        js("Common").toast(caAdd("文件保存失败，无法创建副本\n", e))
        cb(false)
    }
}

private fun caLibraryLock(api: dynamic, self: dynamic, tag: dynamic) {
    val confirm = caObject()
    confirm.title = caAdd(caAdd("确定锁定拓展包“", tag.data.name), "”？")
    confirm.description = "*此操作无法撤销"
    confirm.callback = { id: dynamic ->
        if (caLooseEq(id, 0)) {
            self.postTask({ cb: dynamic ->
                try {
                    val uri = js("ExternalStorage").toUri(tag.data.src)
                    val content = js("ExternalStorage").readFileContent(uri, "UTF-8")
                    val obj = js("JSON").parse(content)
                    api.Library.savePrefixed(uri, obj)
                    api.Library.clearCache(tag.data.src)
                    cb(true, { ->
                        js("Common").toast(caAdd(caAdd("拓展包“", tag.data.name), "”已被锁定"))
                        js("undefined")
                    })
                } catch (e: dynamic) {
                    js("Common").toast(caAdd("文件保存失败\n", e))
                    cb(false)
                }
                js("undefined")
            })
        }
        js("undefined")
    }
    js("Common").showConfirmDialog(confirm)
}

private fun caLibrarySort(api: dynamic, self: dynamic, tag: dynamic) {
    val o = caObject()
    o.array = api.IntelliSense.library.info.slice()
    o.selectIndex = tag.pos
    o.getTitle = { e: dynamic -> e.name }
    o.getDescription = { e: dynamic -> e.description }
    o.canExchange = { array: dynamic, fromIndex: dynamic, toIndex: dynamic ->
        if (caLooseEq(array[fromIndex].core, array[toIndex].core)) {
            true
        } else {
            js("Common").toast("您不能交换优先加载拓展包和普通拓展包的顺序")
            false
        }
    }
    o.callback = { a: dynamic ->
        self.postTask({ cb: dynamic ->
            api.settings.coreLibrarys.length = 0
            api.settings.enabledLibrarys.length = 0
            var i = 0
            while (i < caLength(a)) {
                if (caTruthy(a[i].core)) {
                    api.settings.coreLibrarys.push(a[i].src)
                } else {
                    api.settings.enabledLibrarys.push(a[i].src)
                }
                i++
            }
            cb(true, { -> js("undefined") })
            js("undefined")
        })
        js("undefined")
    }
    js("Common").showSortDialog(o)
}

private fun caLibraryManVmaker(holder: dynamic): dynamic {
    val layout = caNewLinearLayout()
    val text1 = caNewTextView()
    val text2 = caNewTextView()
    holder.text1 = text1
    holder.text2 = text2
    layout.setLayoutParams(caNewAbsParams(-1, -2))
    layout.setOrientation(js("G.LinearLayout.VERTICAL"))
    layout.setPadding(caDp(15), caDp(15), caDp(15), caDp(15))
    text1.setLayoutParams(caNewLinearParams2(-1, -2))
    text1.setEllipsize(js("G.TextUtils.TruncateAt.MIDDLE"))
    text1.setSingleLine(true)
    layout.addView(text1)
    text2.setPadding(0, caDp(5), 0, 0)
    text2.setLayoutParams(caNewLinearParams2(-1, -2))
    js("Common").applyStyle(text2, "textview_prompt", 1)
    layout.addView(text2)
    return layout
}

private fun caLibraryManVbinder(api: dynamic, holder: dynamic, e: dynamic) {
    val title = caEmptyArray()
    val detail = caEmptyArray()
    if (caTruthy(e.installer)) {
        title.push("[安装器] ")
    } else if (caLooseEq(e.mode, 0)) {
        title.push("[内置] ")
    } else if (caLooseEq(e.mode, 2)) {
        title.push("[锁定] ")
    } else if (caLooseEq(e.mode, 3)) {
        title.push(if (caTruthy(e.community)) "[社区] " else "[官方] ")
    }
    title.push(e.name)
    if (!caTruthy(e.disabled) && !caTruthy(e.hasError) && !caTruthy(e.installer)) {
        if (caTruthy(e.core)) {
            title.push(" (已优先启用)")
        } else {
            title.push(" (已启用)")
        }
    }
    holder.text1.setText(title.join(""))
    js("Common").applyStyle(
        holder.text1,
        if (caTruthy(e.disabled)) {
            "item_disabled"
        } else if (caTruthy(e.hasError) || caTruthy(e.deprecated)) {
            "item_critical"
        } else {
            "item_default"
        },
        3
    )
    if (caTruthy(e.disabled)) {
        detail.push("已禁用")
    } else if (caTruthy(e.hasError)) {
        detail.push("加载出错 :", e.error)
    } else {
        if (caLooseEq(e.updateState, "ready") || caLooseEq(e.updateState, "waitForUser") ||
            caLooseEq(e.updateState, "error")
        ) {
            detail.push(caAdd("检测到更新 : ", api.Library.versionToString(e.updateInfo.version)))
        } else if (caLooseEq(e.updateState, "finished")) {
            detail.push(
                caAdd(
                    caAdd("已经更新至最新版本 ", api.Library.versionToString(e.updateInfo.version)),
                    "，重启命令助手生效"
                )
            )
        }
        if (caTruthy(e.deprecated)) {
            detail.push("目前该拓展包不适合在您的设备上使用")
        }
        if (caLength(detail) > 0) detail.push("")
        detail.push(
            caAdd("版本 : ", api.Library.versionToString(e.version)),
            caAdd("作者 : ", e.author)
        )
        if (caTruthy(e.sourceName)) {
            detail.push(
                caAdd(
                    caAdd("来源 : ", e.sourceName),
                    if (caTruthy(e.sourceNote)) caAdd(caAdd("（", e.sourceNote), "）") else ""
                )
            )
        }
        if (caTruthy(e.description) && caLength(e.description) > 0) {
            detail.push(caAdd("\n", e.description))
        }
    }
    holder.text2.setText(detail.join("\n"))
}

private fun caLibraryManRefresh(self: dynamic, api: dynamic) {
    if (caTruthy(api.Library.loadingStatus)) {
        js("Common").toast("命令库加载中，请加载完成后手动刷新")
        return
    }
    val arr = api.IntelliSense.library.info.concat(
        api.settings.disabledLibrarys.map({ e: dynamic, i: dynamic, _a: dynamic ->
            val isInternal = caIn(e, api.Library.inner)
            val name = if (isInternal) {
                api.Library.inner[e].name
            } else {
                js("ExternalStorage").uriToName(js("ExternalStorage").toUri(e))
            }
            val o = caObject()
            o.src = e
            o.index = i
            o.mode = if (isInternal) 0 else -1
            o.name = name
            o.disabled = true
            o
        })
    )
    self.adpt.setArray(arr)
}

private fun caLibraryManPostTask(self: dynamic, api: dynamic, f: dynamic): dynamic {
    if (caTruthy(self.processing)) {
        js("Common").toast("处理中，请稍候……")
        return true
    }
    val progress = js("Common").showProgressDialog()
    progress.setText("正在处理……")
    self.processing = true
    f({ success: dynamic, callback: dynamic ->
        caLibraryPostTaskDone(self, api, progress, success, callback)
        js("undefined")
    })
    return js("undefined")
}

private fun caLibraryPostTaskDone(
    self: dynamic, api: dynamic, progress: dynamic, success: dynamic, callback: dynamic
) {
    if (!caTruthy(success)) {
        progress.close()
        js("G").ui({
            try {
                self.adpt.notifyChange(true)
            } catch (e: dynamic) {
                js("erp")(e)
            }
            js("undefined")
        })
        self.processing = false
        return
    }
    progress.setText("正在刷新命令库……")
    val done = { ->
        progress.close()
        js("G").ui({
            try {
                self.refresh()
                self.processing = false
                callback()
            } catch (e: dynamic) {
                js("erp")(e)
            }
            js("undefined")
        })
        js("undefined")
    }
    if (!caTruthy(api.Library.initLibrary(done))) {
        progress.close()
        js("Common").toast("无法加载拓展包，请稍后重试")
        self.processing = false
        return
    }
}

private fun caLibraryManItemClick(self: dynamic, api: dynamic, parent: dynamic, pos: dynamic) {
    val data = parent.getAdapter().getItem(pos)
    var mnu = if (caTruthy(data.disabled)) {
        self.disabledMenu
    } else if (caTruthy(data.hasError)) {
        self.errMenu
    } else {
        self.enabledMenu
    }
    if (caTruthy(data.menu)) {
        mnu = data.menu.concat(mnu)
    }
    val tag = caObject()
    tag.pos = caParseInt(pos)
    tag.data = data
    tag.callback = { ->
        js("G").ui({
            try {
                self.refresh()
                // 启用/禁用/删除/设为核心都改了 CA.settings，必须立刻落盘
                api.trySave()
            } catch (e: dynamic) {
                js("erp")(e)
            }
            js("undefined")
        })
        js("undefined")
    }
    js("Common").showOperateDialog(mnu, tag)
}

private val caParseIntJs: dynamic = js("function (v) { return parseInt(v); }")
private fun caParseInt(v: dynamic): dynamic = caParseIntJs(v)

// ---------------- showOnlineLib ----------------

private fun caShowOnlineLib(self: dynamic, api: dynamic, callback: dynamic) {
    js("G").ui({
        try {
            caShowOnlineLibUi(self, api, callback)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

private fun caShowOnlineLibUi(self: dynamic, api: dynamic, callback: dynamic) {
    if (!caTruthy(self.linear)) {
        self.contextMenu = caOnlineContextMenu(self, api)
        self.itemMenu = caOnlineItemMenu(self)
        self.vmaker = { holder: dynamic -> caOnlineVmaker(holder) }
        self.vbinder = { holder: dynamic, e: dynamic, _i: dynamic, _a: dynamic ->
            caOnlineVbinder(api, holder, e)
            js("undefined")
        }
        self.reload = { ->
            caOnlineReload(self, api)
            js("undefined")
        }
        self.downloadLib = { data: dynamic ->
            caOnlineDownload(self, api, data)
            js("undefined")
        }
        val libs = caEmptyArray()
        self.libs = libs
        self.adpt = caSimpleGetController(
            caNewSimpleListAdapter5(libs, self.vmaker, self.vbinder, js("undefined"), js("undefined"))
        )
        caBuildOnlineLibUi(self, api)
        self.popup = caNewPopupPage(self.linear, "ca.OnlineLibSource")
        self.popup.on("exit", { ->
            if (caTruthy(self.callback)) self.callback()
            js("undefined")
        })
        js("PWM").registerResetFlag(self, "linear")
    }
    self.callback = callback
    self.popup.enter()
    self.reload()
}

private fun caBuildOnlineLibUi(self: dynamic, api: dynamic) {
    self.linear = caNewLinearLayout()
    self.linear.setOrientation(js("G.LinearLayout.VERTICAL"))
    self.linear.setPadding(caDp(15), caDp(15), caDp(15), 0)
    js("Common").applyStyle(self.linear, "message_bg")

    self.header = caNewLinearLayout()
    self.header.setOrientation(js("G.LinearLayout.HORIZONTAL"))
    self.header.setPadding(0, 0, 0, caDp(10))
    self.header.setOnClickListener(caNewClickListener({ _v: dynamic ->
        try {
            js("Common").showOperateDialog(self.contextMenu)
            true
        } catch (e: dynamic) {
            js("erp")(e)
            js("undefined")
        }
    }))

    self.title = caNewTextView()
    self.title.setText("在线拓展包")
    self.title.setGravity(caAdd(js("G.Gravity.LEFT"), js("G.Gravity.CENTER")))
    self.title.setPadding(caDp(10), 0, caDp(10), 0)
    js("Common").applyStyle(self.title, "textview_default", 4)
    self.header.addView(self.title, caNewLinearParams3(0, -2, 1.0))

    self.menu = caNewTextView()
    self.menu.setText("▼")
    self.menu.setPadding(caDp(10), 0, caDp(10), 0)
    self.menu.setGravity(js("G.Gravity.CENTER"))
    js("Common").applyStyle(self.menu, "button_highlight", 3)
    self.header.addView(self.menu, caNewLinearParams2(-2, -1))
    self.linear.addView(self.header, caNewLinearParams2(-1, -2))

    self.list = caNewListView()
    self.list.setAdapter(self.adpt.self)
    self.list.setOnItemClickListener(caNewItemClickListener({
        parent: dynamic, _view: dynamic, pos: dynamic, _id: dynamic ->
        try {
            val data = parent.getAdapter().getItem(pos)
            if (!caTruthy(data.installed)) {
                val tag = caObject()
                tag.pos = caParseInt(pos)
                tag.data = data
                js("Common").showOperateDialog(self.itemMenu, tag)
            }
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }))
    self.linear.addView(self.list, caNewLinearParams3(-1, 0, 1.0))

    self.exit = caNewTextView()
    self.exit.setText("关闭")
    self.exit.setGravity(js("G.Gravity.CENTER"))
    self.exit.setPadding(caDp(10), caDp(20), caDp(10), caDp(20))
    js("Common").applyStyle(self.exit, "button_critical", 3)
    self.exit.setOnClickListener(caNewClickListener({ _v: dynamic ->
        try {
            self.popup.exit()
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }))
    self.linear.addView(self.exit, caNewLinearParams2(-1, -2))
}

private fun caOnlineContextMenu(self: dynamic, api: dynamic): dynamic {
    val refreshItem = caObject()
    refreshItem.text = "刷新"
    refreshItem.onclick = { _v: dynamic, _tag: dynamic ->
        self.reload()
        js("undefined")
    }

    val sourceItem = caObject()
    sourceItem.text = "查看源信息"
    sourceItem.hidden = { ->
        if (caTruthy(self.libs)) caLength(self.libs) == 0 else true
    }
    sourceItem.onclick = { _v: dynamic, _tag: dynamic ->
        caOnlineShowSourceInfo(api)
        js("undefined")
    }
    return caArray(refreshItem, sourceItem)
}

private fun caOnlineShowSourceInfo(api: dynamic) {
    val s = caEmptyArray()
    val seen = caObject()
    api.Library.enabledSources().forEach({ src: dynamic ->
        val info = api.Library.requestSourceInfoCached(src.url)
        if (!caTruthy(info) || caTruthy(seen[src.url])) {
            js("undefined")
        } else {
            seen[src.url] = true
            s.push(caAdd(if (caTruthy(src.builtin)) "[官方] " else "", caOr(src.name, src.url)))
            if (caTruthy(src.note)) s.push(caAdd("  备注 : ", src.note))
            s.push(caAdd("  地址 : ", src.url))
            s.push(
                caAdd(
                    caAdd(caAdd(caAdd("  库大小 : ", info.libCount), "，由 "),
                        caOr(info.maintainer, "未知")),
                    " 维护"
                )
            )
            if (caTruthy(info.lastUpdate)) {
                s.push(caAdd("  上次更新 : ", js("Updater").toChineseDate(info.lastUpdate)))
            }
            if (caTruthy(info.details)) s.push(caAdd("  ", info.details))
            s.push("")
            js("undefined")
        }
    })
    js("Common").showTextDialog(s.join("\n"))
}

private fun caOnlineItemMenu(self: dynamic): dynamic {
    val downloadItem = caObject()
    downloadItem.text = "下载"
    downloadItem.onclick = { _v: dynamic, tag: dynamic ->
        if (caTruthy(tag.data.requirement)) {
            val o = caObject()
            o.title = caAdd(caAdd("确定下载拓展包“", tag.data.name), "”？")
            o.description = caAdd("使用要求: ", tag.data.requirement)
            o.callback = { id: dynamic ->
                if (caLooseEq(id, 0)) self.downloadLib(tag.data)
                js("undefined")
            }
            js("Common").showConfirmDialog(o)
        } else {
            self.downloadLib(tag.data)
        }
        js("undefined")
    }
    return caArray(downloadItem)
}

private fun caOnlineVmaker(holder: dynamic): dynamic {
    val layout = caNewLinearLayout()
    val text1 = caNewTextView()
    val text2 = caNewTextView()
    holder.text1 = text1
    holder.text2 = text2
    layout.setLayoutParams(caNewAbsParams(-1, -2))
    layout.setOrientation(js("G.LinearLayout.VERTICAL"))
    layout.setPadding(caDp(15), caDp(15), caDp(15), caDp(15))
    text1.setLayoutParams(caNewLinearParams2(-1, -2))
    text1.setEllipsize(js("G.TextUtils.TruncateAt.MIDDLE"))
    text1.setSingleLine(true)
    layout.addView(text1)
    text2.setPadding(0, caDp(5), 0, 0)
    text2.setLayoutParams(caNewLinearParams2(-1, -2))
    js("Common").applyStyle(text2, "textview_prompt", 1)
    layout.addView(text2)
    return layout
}

private fun caOnlineVbinder(api: dynamic, holder: dynamic, e: dynamic) {
    val detail = caEmptyArray()
    holder.text1.setText(caAdd(if (caTruthy(e.installed)) "[已安装] " else "", e.name))
    js("Common").applyStyle(
        holder.text1,
        if (caTruthy(e.disabled)) "item_disabled" else "item_default",
        3
    )
    if (caTruthy(e.disabled)) detail.push("目前您使用的版本不支持此命令库\n")
    detail.push(
        caAdd("版本 : ", api.Library.versionToString(e.version)),
        caAdd("作者 : ", e.author)
    )
    if (caTruthy(e.description)) detail.push(caAdd("\n", e.description))
    holder.text2.setText(detail.join("\n"))
}

private fun caOnlineReload(self: dynamic, api: dynamic) {
    if (caTruthy(self.loading)) {
        js("Common").toast("正在加载中……")
        return
    }
    if (caLooseNe(js("MapScript").host, "Android")) {
        js("Common").toast("您目前的版本不允许访问在线命令库，请使用命令助手App版")
        return
    }
    self.loading = true
    self.libs.length = 0
    self.adpt.notifyChange()
    val progress = js("Common").showProgressDialog()
    progress.setText("正在加载……")
    progress.async({ _o: dynamic ->
        caOnlineReloadAsync(self, api)
        js("undefined")
    })
}

private fun caOnlineReloadAsync(self: dynamic, api: dynamic) {
    val srcs = api.Library.enabledSources()
    val best = caObject()
    val order = caEmptyArray()
    var err: dynamic = js("null")
    try {
        srcs.forEach({ src: dynamic ->
            val info = api.Library.requestSourceInfoCached(src.url)
            var pageNo: dynamic = 0
            if (caTruthy(info)) {
                var page = api.Library.requestSourceIndex(info, pageNo)
                while (caTruthy(page)) {
                    pageNo = caAdd(pageNo, 1)
                    page.forEach({ e: dynamic ->
                        val key = e.uuid
                        e.sourceName = caOr(src.name, src.url)
                        e.sourceInfo = info
                        e.sourceNote = src.note
                        if (!caTruthy(best[key])) {
                            best[key] = e
                            order.push(key)
                        } else {
                            val old = best[key]
                            if (caGe(api.Library.compareLibraryVersion(e.version, old.version), 0)) {
                                best[key] = e
                            }
                        }
                        js("undefined")
                    })
                    page = api.Library.requestSourceIndex(info, pageNo)
                }
            }
            js("undefined")
        })
    } catch (e: dynamic) {
        err = e
    }
    self.loading = false
    if (caTruthy(err)) js("erp")(err)
    if (caLength(order) == 0) {
        js("Common").toast("拓展包源加载失败")
        return
    }
    js("G").ui({
        try {
            order.forEach({ key: dynamic ->
                val e = best[key]
                // 保持原表达式的短路顺序：前面的条件成立时不再计算 Date.parse。
                var disabled: dynamic = e.desperated
                if (!caTruthy(disabled) && caTruthy(e.minSupport)) {
                    disabled = caGt(caDateParse(e.minSupport), caDateParse(api.publishDate))
                }
                if (!caTruthy(disabled) && caTruthy(e.maxSupport)) {
                    disabled = caLt(caDateParse(e.maxSupport), caDateParse(api.publishDate))
                }
                e.disabled = disabled
                e.installed = api.Library.findByUUID(e.uuid)
                self.libs.push(e)
                js("undefined")
            })
            self.adpt.notifyChange()
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

private fun caOnlineDownload(self: dynamic, api: dynamic, data: dynamic) {
    js("Common").showProgressDialog({ dia: dynamic ->
        caOnlineDownloadTask(self, api, data, dia)
        js("undefined")
    })
}

private fun caOnlineDownloadTask(self: dynamic, api: dynamic, data: dynamic, dia: dynamic) {
    dia.setText(caAdd("正在下载拓展包: ", data.name))
    var uriStr: dynamic
    try {
        uriStr = caString(api.Library.downloadLib(data, data.sourceInfo))
        api.Library.clearCache(uriStr)
        // 走版本判断：已装的版本更高就忽略这次下载
        if (caLooseEq(api.Library.addLibrary(uriStr), "ignored")) {
            js("Common").toast("已安装更高版本的拓展包，忽略本次下载")
            return
        }
    } catch (e: dynamic) {
        js("Common").toast(caAdd(caAdd("下载拓展包“", data.name), caAdd("”失败\n", e)))
        return
    }
    val progress = js("Common").showProgressDialog()
    progress.setText("正在刷新命令库...")
    api.Library.initLibrary({ ->
        progress.close()
        js("Common").toast(caAdd(caAdd("拓展包“", data.name), "”已下载并启用"))
        data.installed = true
        js("G").ui({
            try {
                self.adpt.notifyChange()
            } catch (e: dynamic) {
                js("erp")(e)
            }
            js("undefined")
        })
        js("undefined")
    })
}

// ---------------- showModeChooser / showManageBgImage ----------------

private fun caShowModeChooser(api: dynamic, callback: dynamic) {
    js("Common").showOperateDialog(caArray(
        caMenuItem("自动选择", { ->
            api.settings.iiMode = 3
            callback()
            js("undefined")
        }),
        caMenuItem("初学者模式", { ->
            api.settings.iiMode = 1
            callback()
            js("undefined")
        }),
        caMenuItem("专家模式", { ->
            api.settings.iiMode = 2
            callback()
            js("undefined")
        }),
        caMenuItem("关闭", { ->
            api.settings.iiMode = 0
            callback()
            js("undefined")
        })
    ))
}

private fun caShowManageBgImage(api: dynamic, callback: dynamic) {
    js("Common").showOperateDialog(caArray(
        caMenuItem("不使用", { ->
            if (caTruthy(api.settings.bgImage)) {
                val file = caNewFile1(api.settings.bgImage)
                js("ExternalStorage").tryReleaseImportFile(file)
            }
            api.settings.bgImage = js("null")
            callback()
            js("Common").toast("背景图片已设置为 无")
            js("undefined")
        }),
        caMenuItem("从文件中选择", { _v: dynamic, _tag: dynamic ->
            caManageBgChoose(api, callback)
            js("undefined")
        }),
        caMenuGap(caDp(10)),
        caMenuItem("调整背景透明度", { ->
            caManageBgAlpha(api, callback)
            js("undefined")
        })
    ))
}

private fun caManageBgChoose(api: dynamic, callback: dynamic) {
    val o = caObject()
    o.mimeType = "image/*"
    o.hint = "ca_background_image"
    o.file = { f: dynamic ->
        api.settings.bgImage = caString(f.getAbsolutePath())
        callback()
        js("Common").toast("背景图片已设置")
        js("undefined")
    }
    js("ExternalStorage").showImportActions(o)
}

private fun caManageBgAlpha(api: dynamic, callback: dynamic) {
    if (!caTruthy(api.settings.bgImage)) {
        js("Common").toast("您还没有设置背景图片")
        return
    }
    val o = caObject()
    o.max = 100
    o.progress = if (caIsNaN(api.settings.bgAlpha)) 75 else caMul(api.settings.bgAlpha, 100)
    o.prompt = { progress: dynamic ->
        caAdd(
            caAdd("透明度：", progress),
            "%\n\n本设置调整的实际上是在背景图上覆盖的背景色的不透明度，因此如果主题是半透明的话即使本设置调到100%也可以看见背景图片"
        )
    }
    o.callback = { progress: dynamic ->
        api.settings.bgAlpha = caDiv(progress, 100)
        callback()
        js("undefined")
    }
    js("Common").showSlider(o)
}

// ---------------- showIconChooser / customIcon / Icon ----------------

private fun caShowIconChooser(self: dynamic, api: dynamic, callback: dynamic, onDismiss: dynamic) {
    js("G").ui({
        try {
            caShowIconChooserUi(self, api, callback, onDismiss)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

private fun caShowIconChooserUi(
    self: dynamic, api: dynamic, callback: dynamic, onDismiss: dynamic
) {
    if (!caTruthy(self.addCustom)) {
        self.addCustom = { -> caIconChooserAddCustom() }
        self.selectIcon = { cb: dynamic ->
            caIconChooserSelect(self, api, cb)
            js("undefined")
        }
        self.recent = caEmptyArray()
    }
    if (api.settings.icon.startsWith("/") &&
        caLt(self.recent.indexOf(api.settings.icon), 0)
    ) {
        self.recent.push(api.settings.icon)
    }
    val ci = caObjectKeys(api.Icon).concat(self.recent, "")
    val frame = caNewFrameLayout()
    js("Common").applyStyle(frame, "message_bg")
    val list = caNewGridView()
    list.setLayoutParams(caNewFrameParams2(-1, -1))
    list.setHorizontalSpacing(caDp(20))
    list.setVerticalSpacing(caDp(20))
    list.setPadding(caDp(20), caDp(20), caDp(20), caDp(20))
    list.setGravity(js("G.Gravity.CENTER"))
    list.setNumColumns(-1)
    list.setStretchMode(2)
    list.setAdapter(caNewRhinoListAdapter(ci, { e: dynamic ->
        caIconChooserView(self, api, e)
    }))
    var popup: dynamic = js("null")
    list.setOnItemClickListener(caNewItemClickListener({
        parent: dynamic, _view: dynamic, pos: dynamic, _id: dynamic ->
        try {
            caIconChooserItemClick(self, api, parent, pos, callback, popup)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }))
    frame.addView(list)
    popup = js("PopupPage").showDialog("ca.IconChooser", frame, -1, -1)
    if (caTruthy(onDismiss)) popup.on("exit", onDismiss)
}

private fun caIconChooserAddCustom(): dynamic {
    val view = caNewTextView()
    view.setText("自定义图标")
    view.setPadding(caDp(5), caDp(5), caDp(5), caDp(5))
    js("Common").applyStyle(view, "button_reactive", 2)
    return view
}

private fun caIconChooserView(self: dynamic, api: dynamic, e: dynamic): dynamic {
    val view = if (caLooseEq(e, "")) {
        self.addCustom()
    } else if (caIn(e, api.Icon)) {
        api.Icon[e](1, true)
    } else {
        api.customIcon(e, 1, true)
    }
    view.setLayoutParams(caNewAbsParams(-2, -2))
    return view
}

private fun caIconChooserSelect(self: dynamic, api: dynamic, cb: dynamic) {
    val o = caObject()
    o.mimeType = "image/*"
    o.hint = "ca_icon_image"
    o.file = { f: dynamic ->
        val path = caString(f.getAbsolutePath())
        api.settings.icon = path
        if (caLt(self.recent.indexOf(path), 0)) self.recent.push(path)
        if (caTruthy(cb)) cb()
        js("undefined")
    }
    js("ExternalStorage").showImportActions(o)
}

private fun caIconChooserItemClick(
    self: dynamic, api: dynamic, parent: dynamic, pos: dynamic, callback: dynamic, popup: dynamic
) {
    val z = caString(parent.getItemAtPosition(pos))
    if (caTruthy(z)) {
        api.settings.icon = z
        if (caTruthy(callback)) callback()
    } else {
        self.selectIcon(callback)
    }
    popup.exit()
}

private fun caCustomIcon(api: dynamic, path: dynamic, size: dynamic, preview: dynamic): dynamic {
    val w = caMul(caDp(32), size)
    val frm = caNewFrameLayout()
    val view = caNewImageView()
    var drawable: dynamic = js("null")
    if (js("ScriptInterface").isImageDecoderSupported()) {
        try {
            drawable = js("G.ImageDecoder").decodeDrawable(
                js("G.ImageDecoder").createSource(caNewFile1(path)),
                caNewHeaderDecodedListener({ decoder: dynamic, _info: dynamic, _source: dynamic ->
                    decoder.setTargetSize(w, w)
                    js("undefined")
                })
            )
            if (caInstanceOf(drawable, js("G.AnimatedImageDrawable"))) {
                drawable.setRepeatCount(-1)
                drawable.start()
            }
        } catch (e: dynamic) {
            js("Log").e(e)
        }
    } else {
        drawable = js("G.Drawable").createFromPath(path)
    }
    if (caTruthy(drawable)) {
        view.setImageDrawable(drawable)
    } else if (caTruthy(preview)) {
        view.setImageResource(js("G.R.drawable.ic_delete"))
    } else {
        return api.Icon.default(size, false)
    }
    view.setScaleType(js("G.ImageView.ScaleType.FIT_XY"))
    view.setLayoutParams(caNewFrameParams2(w, w))
    frm.addView(view)
    return frm
}

private fun caBuildIcon(api: dynamic): dynamic {
    val icon = caObject()
    icon["default"] = { size: dynamic -> caIconDefault(size) }
    icon.default_old = { size: dynamic -> caIconDefaultOld(size) }
    return icon
}

private fun caIconDefault(size: dynamic): dynamic {
    val w = caMul(caDp(32), size)
    val bmp = js("G.Bitmap").createBitmap(w, w, js("G.Bitmap.Config.ARGB_8888"))
    val cv = caNewCanvas(bmp)
    cv.scale(caDiv(w, 256), caDiv(w, 256))
    val pt = caNewPaint()
    pt.setAntiAlias(true)
    js("IntColor").Paint.setColor(pt, js("Common").theme.go_bgcolor)
    js("IntColor").Paint.setShadowLayer(
        pt, 16, 0, 0, js("Common").theme.go_touchbgcolor
    )
    cv.drawCircle(128, 128, 112, pt)
    pt.setTextSize(128)
    pt.setTypeface(
        js("G.Typeface").create(
            caOr(js("G.Typeface.MONOSPACE"), js("G.Typeface.DEFAULT")),
            js("G.Typeface.BOLD")
        )
    )
    pt.clearShadowLayer()
    val fb = caNewRect()
    val fm = pt.getFontMetrics()
    pt.getTextBounds("CA", 0, 2, fb)
    js("IntColor").Paint.setColor(pt, js("Common").theme.go_textcolor)
    cv.drawText(
        "CA",
        caSub(128, fb.centerX()),
        caSub(128, caDiv(caAdd(fm.descent, fm.ascent), 2)),
        pt
    )
    val frm = caNewFrameLayout()
    val view = caNewImageView()
    view.setImageBitmap(bmp)
    view.setLayoutParams(caNewFrameParams2(w, w))
    frm.addView(view)
    return frm
}

private fun caIconDefaultOld(size: dynamic): dynamic {
    val zp = caMul(js("G.dp"), size)
    val view = caNewTextView()
    view.setText("CA")
    view.setPadding(caMul(5, zp), caMul(5, zp), caMul(5, zp), caMul(5, zp))
    view.setTextSize(caMul(18, size))
    view.setBackgroundColor(js("Common").theme.go_bgcolor)
    view.setTextColor(js("Common").theme.go_textcolor)
    view.setOnTouchListener(caNewTouchListener({ v: dynamic, e: dynamic ->
        try {
            val action = e.getAction()
            if (caLooseEq(action, e.ACTION_DOWN)) {
                v.setBackgroundColor(js("Common").theme.go_touchbgcolor)
                v.setTextColor(js("Common").theme.go_touchtextcolor)
            } else if (caLooseEq(action, e.ACTION_UP) ||
                caLooseEq(action, e.ACTION_CANCEL)
            ) {
                v.setBackgroundColor(js("Common").theme.go_bgcolor)
                v.setTextColor(js("Common").theme.go_textcolor)
            }
            false
        } catch (e: dynamic) {
            js("erp")(e)
            false
        }
    }))
    return view
}

// ---------------- checkFeatures / hasFeature / Features ----------------

private fun caCheckFeatures(api: dynamic) {
    caForIn(api.Features, { i: dynamic ->
        api.Features[i].flag = api.Library.checkPackVer(api.Features[i])
        js("undefined")
    })
}

private fun caHasFeature(api: dynamic, feature: dynamic): Boolean {
    return caLooseEq(api.Features[feature].flag, 0)
}

private fun caBuildFeatures(): dynamic {
    val features = caObject()
    val enableCommand = caObject()
    enableCommand.minSupportVer = "0.16"
    features.enableCommand = enableCommand

    val enableCommandBlock = caObject()
    enableCommandBlock.minSupportVer = "1.0.5"
    features.enableCommandBlock = enableCommandBlock

    val enableLocalCoord = caObject()
    enableLocalCoord.minSupportVer = "1.2"
    features.enableLocalCoord = enableLocalCoord

    val version11 = caObject()
    version11.minSupportVer = "1.1"
    version11.maxSupportVer = "1.1.*"
    features.version_1_1 = version11
    return features
}

// ---------------- drawQRCode / showDonateDialog ----------------

private fun caDrawQRCode(bmp: dynamic, code: dynamic, decorator: dynamic) {
    val bytes = js("android.util.Base64").decode(code.bytes, 2)
    val cv = caNewCanvas(bmp)
    val pt = caNewPaint()
    pt.setAntiAlias(false)
    js("IntColor").Paint.setColor(pt, js("G.Color.BLACK"))
    pt.setStyle(js("G.Paint.Style.FILL"))
    js("IntColor").Canvas.drawColor(cv, caNewInteger(js("G.Color.WHITE")))
    cv.scale(caDiv(bmp.width, code.width), caDiv(bmp.height, code.height))
    var y = 0
    while (caLt(y, code.height)) {
        var x = 0
        while (caLt(x, code.width)) {
            val p = caAdd(caMul(y, code.width), x)
            if (caQrPixel(bytes, p)) {
                cv.drawRect(x, y, x + 1, y + 1, pt)
            }
            x++
        }
        y++
    }
    if (caTruthy(decorator.drawable)) {
        val wr = code.whiteRect
        val dr = decorator.drawable
        dr.setBounds(wr.x, wr.y, caAdd(wr.width, wr.x), caAdd(wr.height, wr.y))
        dr.draw(cv)
    }
}

private fun caShowDonateDialog(self: dynamic, api: dynamic, donateMethods: dynamic) {
    js("G").ui({
        try {
            caShowDonateDialogUi(self, api, donateMethods)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

private fun caShowDonateDialogUi(self: dynamic, api: dynamic, donateMethods: dynamic) {
    if (!caTruthy(self.getDonateImage)) {
        self.getTextHeight = { text: dynamic, maxWidth: dynamic, pt: dynamic,
                               spacingMult: dynamic, spacingAdd: dynamic ->
            caDonateGetTextHeight(text, maxWidth, pt, spacingMult, spacingAdd)
        }
        self.drawText = { canvas: dynamic, text: dynamic, x: dynamic, y: dynamic,
                          maxWidth: dynamic, pt: dynamic, spacingMult: dynamic,
                          spacingAdd: dynamic ->
            caDonateDrawText(canvas, text, x, y, maxWidth, pt, spacingMult, spacingAdd)
            js("undefined")
        }
        self.getDonateImage = { width: dynamic, o: dynamic ->
            caDonateGetImage(self, api, width, o)
        }
    }
    val state = caObject()
    state.imgSaved = false
    state.bmp = self.getDonateImage(caDp(240), donateMethods[0])

    val scr = caNewScrollView()
    js("Common").applyStyle(scr, "message_bg")
    val layout = caNewLinearLayout()
    layout.setOrientation(js("G.LinearLayout.VERTICAL"))
    layout.setPadding(caDp(10), caDp(10), caDp(10), 0)
    layout.setLayoutParams(caNewFrameParams2(-1, -2))

    val text = caNewTextView()
    text.setLayoutParams(caNewLinearParams2(-1, -2))
    text.setText("捐助通道")
    text.setGravity(js("G.Gravity.CENTER"))
    text.setPadding(caDp(10), caDp(10), caDp(10), caDp(10))
    js("Common").applyStyle(text, "textview_default", 4)
    layout.addView(text)

    val img = caNewImageView()
    img.setImageBitmap(state.bmp)
    img.setAdjustViewBounds(true)
    img.setScaleType(js("G.ImageView.ScaleType.CENTER_INSIDE"))
    img.setLayoutParams(caNewLinearParams2(-2, -2))
    img.setOnClickListener(caNewClickListener({ _v: dynamic ->
        caDonateSaveImage(state)
        js("undefined")
    }))
    img.setOnLongClickListener(caNewLongClickListener({ _v: dynamic ->
        caDonateLongClick(self, donateMethods, img, state)
    }))
    layout.addView(img)

    val save = caNewTextView()
    save.setLayoutParams(caNewLinearParams2(-1, -2))
    save.setText("点击二维码保存图片\n长按切换捐款方式")
    save.setGravity(js("G.Gravity.CENTER"))
    save.setPadding(caDp(10), caDp(10), caDp(10), 0)
    js("Common").applyStyle(save, "textview_prompt", 2)
    layout.addView(save)

    val exit = caNewTextView()
    exit.setLayoutParams(caNewLinearParams2(-1, -2))
    exit.setText("关闭")
    exit.setGravity(js("G.Gravity.CENTER"))
    exit.setPadding(caDp(10), caDp(20), caDp(10), caDp(20))
    js("Common").applyStyle(exit, "button_critical", 3)
    var popup: dynamic = js("null")
    exit.setOnClickListener(caNewClickListener({ _v: dynamic ->
        try {
            popup.exit()
            if (caTruthy(state.imgSaved)) js("Common").toast("感谢您的支持！")
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
                img.setImageDrawable(js("null"))
                state.bmp.recycle()
            } catch (e: dynamic) {
                js("erp")(e)
            }
            js("undefined")
        }, 1000)
        js("undefined")
    })
}

private fun caDonateGetTextHeight(
    text: dynamic, maxWidth: dynamic, pt: dynamic,
    spacingMult: dynamic, spacingAdd: dynamic
): dynamic {
    val fontHeight = caSub(pt.descent(), pt.ascent())
    val spacing = caAdd(caMul(spacingMult, fontHeight), spacingAdd)
    var fromIndex: dynamic = 0
    var lines = 0
    while (caLt(fromIndex, caLength(text))) {
        var charCount = pt.breakText(text, fromIndex, caLength(text), true, maxWidth, js("null"))
        val lfPos = text.indexOf("\n", fromIndex)
        if (caGe(lfPos, 0) && caGe(charCount, lfPos)) charCount = caAdd(lfPos, 1)
        lines++
        fromIndex = caAdd(fromIndex, charCount)
    }
    return caAdd(caMul(fontHeight, lines), caMul(spacing, lines - 1))
}

private fun caDonateDrawText(
    canvas: dynamic, text: dynamic, x: dynamic, y: dynamic, maxWidth: dynamic,
    pt: dynamic, spacingMult: dynamic, spacingAdd: dynamic
) {
    val fontHeight = caSub(pt.descent(), pt.ascent())
    val spacing = caAdd(caMul(spacingMult, fontHeight), spacingAdd)
    var fromIndex: dynamic = 0
    var dy: dynamic = y
    while (caLt(fromIndex, caLength(text))) {
        var charCount = pt.breakText(text, fromIndex, caLength(text), true, maxWidth, js("null"))
        val lfPos = text.indexOf("\n", fromIndex)
        if (caGe(lfPos, 0) && caGe(charCount, lfPos)) charCount = caAdd(lfPos, 1)
        canvas.drawText(text, fromIndex, caAdd(fromIndex, charCount), x, dy, pt)
        dy = caAdd(dy, caAdd(fontHeight, spacing))
        fromIndex = caAdd(fromIndex, charCount)
    }
}

private fun caDonateGetImage(self: dynamic, api: dynamic, width: dynamic, o: dynamic): dynamic {
    var bmp: dynamic = js("null")
    var cv: dynamic = js("null")
    val pt1 = caNewPaint()
    var pt2: dynamic = js("null")
    var pt3: dynamic = js("null")
    var totalHeight: dynamic = 0
    var fontHeight1: dynamic = js("null")
    var fontHeight2: dynamic = js("null")
    var fontHeight3: dynamic = js("null")
    var textHeight1: dynamic = js("null")
    var footerHeight: dynamic = caMul(0.1, width)
    val text1 = o.title
    val text2 = o.description
    val text3 = o.comments
    val qr = js("G.Bitmap").createBitmap(
        caMul(width, 0.8), caMul(width, 0.8), js("G.Bitmap.Config.ARGB_8888")
    )
    val drawable: dynamic
    if (caLooseEq(js("MapScript").host, "Android")) {
        drawable = caCtx().getResources().getDrawable(
            js("com.xero.ca.R.drawable.icon"), caCtx().getTheme()
            )
    } else {
        drawable = caNewColorDrawable(js("G.Color.BLACK"))
    }
    val decorator = caObject()
    decorator.drawable = drawable
    api.drawQRCode(qr, o.qrCode, decorator)
    pt1.setAntiAlias(true)
    pt1.setTextAlign(js("G.Paint.Align.CENTER"))
    js("IntColor").Paint.setColor(pt1, js("G.Color.BLACK"))
    pt1.setTextSize(caMul(0.1, width))
    fontHeight1 = caSub(pt1.descent(), pt1.ascent())
    textHeight1 = self.getTextHeight(text1, caMul(width, 0.6), pt1, 0, 0)
    totalHeight = caAdd(totalHeight, caAdd(textHeight1, fontHeight1))
    pt2 = caNewPaintWith(pt1)
    js("IntColor").Paint.setColor(pt2, js("G.Color.GRAY"))
    pt2.setTextSize(caMul(0.07, width))
    fontHeight2 = caSub(pt2.descent(), pt2.ascent())
    pt3 = caNewPaintWith(pt2)
    pt3.setTextSize(caMul(0.05, width))
    fontHeight3 = caSub(pt3.descent(), pt3.ascent())
    if (caTruthy(text2)) {
        totalHeight = caAdd(
            totalHeight,
            caAdd(self.getTextHeight(text2, caMul(width, 0.8), pt2, 0, 0),
                caMul(fontHeight2, 0.5))
        )
    }
    if (caTruthy(text3)) {
        footerHeight = caAdd(
            self.getTextHeight(text3, caMul(width, 0.8), pt3, 0, 0), fontHeight3
        )
    }
    bmp = js("G.Bitmap").createBitmap(
        width,
        caAdd(caAdd(totalHeight, caMul(width, 0.8)), footerHeight),
        js("G.Bitmap.Config.ARGB_8888")
    )
    cv = caNewCanvas(bmp)
    js("IntColor").Canvas.drawColor(cv, caNewInteger(js("G.Color.WHITE")))
    self.drawText(
        cv, text1, caMul(0.5, width),
        caSub(caMul(fontHeight1, 0.5), pt1.ascent()),
        caMul(width, 0.6), pt1, 0, 0
    )
    if (caTruthy(text2)) {
        self.drawText(
            cv, text2, caMul(0.5, width),
            caSub(
                caAdd(caAdd(caMul(fontHeight1, 0.5), caMul(fontHeight2, 0.5)), textHeight1),
                pt2.ascent()
            ),
            caMul(width, 0.8), pt2, 0, 0
        )
    }
    cv.drawBitmap(qr, caMul(0.1, width), totalHeight, pt1)
    if (caTruthy(text3)) {
        self.drawText(
            cv, text3, caMul(0.5, width),
            caSub(
                caAdd(caAdd(totalHeight, caMul(width, 0.8)), caMul(fontHeight3, 0.5)),
                pt3.ascent()
            ),
            caMul(width, 0.8), pt3, 0, 0
        )
    }
    qr.recycle()
    return bmp
}

private fun caDonateSaveImage(state: dynamic) {
    try {
        try {
            val f = caNewFile2(caExternalStorageDir(), "Pictures/ca_donate.png")
            f.getParentFile().mkdirs()
            val out = caNewFileOutputStream(f)
            state.bmp.compress(js("G.Bitmap.CompressFormat.PNG"), 0, out)
            out.close()
            js("Common").toast(caAdd("图片已保存至", f.getPath()))
            js("AndroidBridge").scanMedia(f)
            state.imgSaved = true
        } catch (e: dynamic) {
            js("Common").toast(caAdd("图片保存失败\n", e))
        }
    } catch (e: dynamic) {
        js("erp")(e)
    }
}

private fun caDonateLongClick(
    self: dynamic, donateMethods: dynamic, img: dynamic, state: dynamic
): dynamic {
    try {
        js("Common").showListChooser(
            donateMethods.map({ e: dynamic -> e.name }),
            { pos: dynamic ->
                val oldBmp = state.bmp
                state.bmp = self.getDonateImage(caDp(240), donateMethods[pos])
                img.setImageBitmap(state.bmp)
                if (caTruthy(oldBmp)) oldBmp.recycle()
                js("undefined")
            }
        )
        return true
    } catch (e: dynamic) {
        js("erp")(e)
        return true
    }
}

// ---------------- 安装 ----------------

fun installCaLibrary(api: dynamic) {
    api.showSourceMan = caSelf0Js { self: dynamic ->
        caShowSourceMan(self, api)
    }
    api.showLibraryMan = caSelf1Js { self: dynamic, callback: dynamic ->
        caShowLibraryMan(self, api, callback)
    }
    api.showOnlineLib = caSelf1Js { self: dynamic, callback: dynamic ->
        caShowOnlineLib(self, api, callback)
    }
    api.showModeChooser = { callback: dynamic ->
        caShowModeChooser(api, callback)
        js("undefined")
    }
    api.showManageBgImage = { callback: dynamic ->
        caShowManageBgImage(api, callback)
        js("undefined")
    }
    api.showIconChooser = caSelf2Js { self: dynamic, callback: dynamic, onDismiss: dynamic ->
        caShowIconChooser(self, api, callback, onDismiss)
    }
    api.customIcon = { path: dynamic, size: dynamic, preview: dynamic ->
        caCustomIcon(api, path, size, preview)
    }
    api.Icon = caBuildIcon(api)
    api.checkFeatures = { ->
        caCheckFeatures(api)
        js("undefined")
    }
    api.hasFeature = { feature: dynamic -> caHasFeature(api, feature) }
    api.Features = caBuildFeatures()
    api.drawQRCode = { bmp: dynamic, code: dynamic, decorator: dynamic ->
        caDrawQRCode(bmp, code, decorator)
        js("undefined")
    }
    api.showDonateDialog = caSelf1Js { self: dynamic, donateMethods: dynamic ->
        caShowDonateDialog(self, api, donateMethods)
    }
}
