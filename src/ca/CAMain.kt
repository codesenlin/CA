package ca.kotlin.ca

import ca.kotlin.core.*


/**
 * modules/CA.js main 组的 Kotlin/JS 版本（原文件第 602-1763 行，以成员名为准）。
 *
 * 只提供 installCaMain(api)：把 showMain / showGen / hideGen / showHistory /
 * hideHistory 挂到调用方传入的 CA 共享 API 上；不注册模块、不定义 caApi、
 * 也不调用安装函数。
 *
 * 原实现里 showGen / showHistory 是具名函数表达式 `function self(...)`，
 * 界面状态（self.main、self.cmdEdit、self.hisAdapter、self.linear 等）挂在
 * 函数对象本身；这里用薄 JS 工厂保留同一个 self 语义，PWM.resetUICache 与
 * 其它组通过 CA.showGen.xxx / CA.showHistory 的访问保持有效。
 *
 * 命令输入框与历史列表的 OnTouchListener 同样依赖具名函数对象自身的状态
 * （function touch(v, e) 里的 touch.cbk / touch.sx 等），也用工厂保留。
 *
 * 对 this 的引用改为显式 api；跨组字段/方法统一走 api.xxx 动态调用。
 * 原实现已有的 bug（self.nula 未定义等）按原样保留并加注释。
 */
private val caTruthyJs: dynamic = js("function (v) { return !!v; }")
private fun caTruthy(v: dynamic): Boolean = caTruthyJs(v)

private val caLengthJs: dynamic = js("function (v) { return v.length | 0; }")
private fun caLength(v: dynamic): Int = caLengthJs(v)

private val caStringJs: dynamic = js("function (v) { return String(v); }")
private fun caString(v: dynamic): dynamic = caStringJs(v)

private val caLooseEqJs: dynamic = js("function (a, b) { return a == b; }")
private fun caLooseEq(a: dynamic, b: dynamic): Boolean = caLooseEqJs(a, b)

private val caStrictEqJs: dynamic = js("function (a, b) { return a === b; }")
private fun caStrictEq(a: dynamic, b: dynamic): Boolean = caStrictEqJs(a, b)

private val caInstanceOfJs: dynamic = js("function (v, c) { return v instanceof c; }")
private fun caInstanceOf(v: dynamic, c: dynamic): Boolean = caInstanceOfJs(v, c)

private val caParseIntJs: dynamic = js("function (v) { return parseInt(v); }")
private fun caParseInt(v: dynamic): dynamic = caParseIntJs(v)

private val caParseFloatJs: dynamic = js("function (v) { return parseFloat(v); }")
private fun caParseFloat(v: dynamic): dynamic = caParseFloatJs(v)

private val caIsNaNRawJs: dynamic = js("function (v) { return isNaN(v); }")
private fun caIsNaN(v: dynamic): Boolean = caIsNaNRawJs(v)

private val caDpJs: dynamic = js("function (n) { return n * G.dp; }")
private fun caDp(n: Int): dynamic = caDpJs(n)

private val caMulJs: dynamic = js("function (a, b) { return a * b; }")
private fun caMul(a: dynamic, b: dynamic): dynamic = caMulJs(a, b)

private val caAbsJs: dynamic = js("function (v) { return Math.abs(v); }")
private fun caAbs(v: dynamic): dynamic = caAbsJs(v)

private val caNegJs: dynamic = js("function (v) { return -v; }")
private fun caNeg(v: dynamic): dynamic = caNegJs(v)

private val caOrJs: dynamic = js("function (a, b) { return a || b; }")
private fun caOr(a: dynamic, b: dynamic): dynamic = caOrJs(a, b)

private val caBitOrJs: dynamic = js("function (a, b) { return a | b; }")
private fun caBitOr(a: dynamic, b: dynamic): dynamic = caBitOrJs(a, b)

private val caForInJs: dynamic = js("function (o, f) { for (var i in o) f(i); }")
private fun caForIn(o: dynamic, f: dynamic) {
    caForInJs(o, f)
}

/** Array.prototype.splice.apply(target, [0, target.length].concat(items)) 的原样封装。 */
private val caSpliceReplaceJs: dynamic = js(
    "function (target, items) {" +
        " Array.prototype.splice.apply(target, [0, target.length].concat(items));" +
        "}"
)

/** 把回调需要的动态 this 传回 Kotlin（用于 Common 操作对话框的 callback）。 */
private val caThis0Js: dynamic = js("function (f) { return function () { return f(this); }; }")

private fun caObject(): dynamic = js("({})")

private fun caArray(vararg items: dynamic): dynamic {
    val a = js("[]")
    var i = 0
    while (i < items.size) {
        a.push(items[i])
        i++
    }
    return a
}

private val caChildrenObjJs: dynamic = js("function (v) { return { children: v }; }")
private fun caChildrenObj(v: dynamic): dynamic = caChildrenObjJs(v)

/** Common.showOperateDialog 第二个参数的 { cmd: ... }。 */
private val caCmdTagJs: dynamic = js("function (c) { return { cmd: c }; }")

// ---------------- 菜单项 helper ----------------

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

// ---------------- 具名函数 self 工厂 ----------------

/** function self(noani) { ... }：状态挂在 showGen 函数对象上。 */
private val caShowGenFactoryJs: dynamic = js(
    "function (impl) { return function self(noani) { return impl(self, arguments); }; }"
)

/** function self() { ... }：状态挂在 showHistory 函数对象上。 */
private val caShowHistoryFactoryJs: dynamic = js(
    "function (impl) { return function self() { return impl(self, arguments); }; }"
)

/** function touch(v, e) { ... }：状态挂在 OnTouchListener 的函数对象上。 */
private val caTouchFactoryJs: dynamic = js(
    "function (impl) { var self = function touch(v, e) { return impl(self, v, e); }; return self; }"
)

// ---------------- Android / UI helper ----------------

private val caNewLinearLayoutJs: dynamic = js("function () { return new G.LinearLayout(ctx); }")
private val caNewTextViewJs: dynamic = js("function () { return new G.TextView(ctx); }")
private val caNewEditTextJs: dynamic = js("function () { return new G.EditText(ctx); }")
private val caNewFrameLayoutJs: dynamic = js("function () { return new G.FrameLayout(ctx); }")
private val caNewImageViewJs: dynamic = js("function () { return new G.ImageView(ctx); }")
private val caNewListViewJs: dynamic = js("function () { return new G.ListView(ctx); }")
private val caNewLinearParams2Js: dynamic = js(
    "function (w, h) { return new G.LinearLayout.LayoutParams(w, h); }"
)
private val caNewLinearParams3Js: dynamic = js(
    "function (w, h, wt) { return new G.LinearLayout.LayoutParams(w, h, wt); }"
)
private val caNewAbsParamsJs: dynamic = js(
    "function (w, h) { return new G.AbsListView.LayoutParams(w, h); }"
)
private val caNewFrameParams2Js: dynamic = js(
    "function (w, h) { return new G.FrameLayout.LayoutParams(w, h); }"
)
private val caNewTranslate8Js: dynamic = js(
    "function (a, b, c, d, e, f, g, h) { return new G.TranslateAnimation(a, b, c, d, e, f, g, h); }"
)
private val caNewAccelerateJs: dynamic = js(
    "function (v) { return new G.AccelerateInterpolator(v); }"
)
private val caNewDecelerateJs: dynamic = js(
    "function (v) { return new G.DecelerateInterpolator(v); }"
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
private val caNewTextWatcherJs: dynamic = js(
    "function (f) { return new G.TextWatcher({ afterTextChanged: f }); }"
)
private val caNewSpanWatcherJs: dynamic = js(
    "function (f) { return new G.SpanWatcher({ onSpanChanged: f }); }"
)
private val caNewItemClickListenerJs: dynamic = js(
    "function (f) { return new G.AdapterView.OnItemClickListener({ onItemClick: f }); }"
)
private val caNewItemLongClickListenerJs: dynamic = js(
    "function (f) { return new G.AdapterView.OnItemLongClickListener({ onItemLongClick: f }); }"
)
private val caNewLayoutChangeListenerJs: dynamic = js(
    "function (f) { return new G.View.OnLayoutChangeListener({ onLayoutChange: f }); }"
)
private val caNewAnimationListenerJs: dynamic = js(
    "function (f) { return new G.Animation.AnimationListener({ onAnimationEnd: f }); }"
)
private val caNewRunnableJs: dynamic = js(
    "function (f) { return new java.lang.Runnable({ run: f }); }"
)
private val caNewMotionCancelJs: dynamic = js(
    "function () { return G.MotionEvent.obtain(0, 0, G.MotionEvent.ACTION_CANCEL, 0, 0, 0, 0, 0, 0, 0, 0, 0); }"
)
private val caNewLengthFilterJs: dynamic = js(
    "function (n) { return new G.InputFilter.LengthFilter(n); }"
)
private val caNewPopupPageJs: dynamic = js(
    "function (v, n) { return new PopupPage(v, n); }"
)
private val caNewRhinoListAdapterJs: dynamic = js(
    "function (a, v) { return new RhinoListAdapter(a, v); }"
)
private val caNewSimpleListAdapterJs: dynamic = js(
    "function (a, b, c) { return new SimpleListAdapter(a, b, c); }"
)
private val caSimpleGetControllerJs: dynamic = js(
    "function (a) { return SimpleListAdapter.getController(a); }"
)
private val caNewExpandableListAdapterJs: dynamic = js(
    "function (a, b, c, d, e, f) { return new ExpandableListAdapter(a, b, c, d, e, f); }"
)
private val caExpandableControlJs: dynamic = js(
    "function (a) { return ExpandableListAdapter.control(a); }"
)
private val caNewCanvasJs: dynamic = js("function (b) { return new G.Canvas(b); }")
private val caNewPaintJs: dynamic = js("function () { return new G.Paint(); }")
private val caNewPathJs: dynamic = js("function () { return new G.Path(); }")
private val caBitmapCreateJs: dynamic = js(
    "function (w, h, c) { return G.Bitmap.createBitmap(w, h, c); }"
)

private fun caNewLinearLayout(): dynamic = caNewLinearLayoutJs()
private fun caNewTextView(): dynamic = caNewTextViewJs()
private fun caNewEditText(): dynamic = caNewEditTextJs()
private fun caNewFrameLayout(): dynamic = caNewFrameLayoutJs()
private fun caNewImageView(): dynamic = caNewImageViewJs()
private fun caNewListView(): dynamic = caNewListViewJs()
private fun caNewLinearParams2(w: dynamic, h: dynamic): dynamic = caNewLinearParams2Js(w, h)
private fun caNewLinearParams3(w: dynamic, h: dynamic, weight: dynamic): dynamic =
    caNewLinearParams3Js(w, h, weight)
private fun caNewAbsParams(w: dynamic, h: dynamic): dynamic = caNewAbsParamsJs(w, h)
private fun caNewFrameParams2(w: dynamic, h: dynamic): dynamic = caNewFrameParams2Js(w, h)
private fun caNewTranslate8(
    a: dynamic, b: dynamic, c: dynamic, d: dynamic,
    e: dynamic, f: dynamic, g: dynamic, h: dynamic
): dynamic = caNewTranslate8Js(a, b, c, d, e, f, g, h)
private fun caNewAccelerate(v: dynamic): dynamic = caNewAccelerateJs(v)
private fun caNewDecelerate(v: dynamic): dynamic = caNewDecelerateJs(v)
private fun caNewClickListener(f: dynamic): dynamic = caNewClickListenerJs(f)
private fun caNewLongClickListener(f: dynamic): dynamic = caNewLongClickListenerJs(f)
private fun caNewTouchListener(f: dynamic): dynamic = caNewTouchListenerJs(f)
private fun caNewTextWatcher(f: dynamic): dynamic = caNewTextWatcherJs(f)
private fun caNewSpanWatcher(f: dynamic): dynamic = caNewSpanWatcherJs(f)
private fun caNewItemClickListener(f: dynamic): dynamic = caNewItemClickListenerJs(f)
private fun caNewItemLongClickListener(f: dynamic): dynamic = caNewItemLongClickListenerJs(f)
private fun caNewLayoutChangeListener(f: dynamic): dynamic = caNewLayoutChangeListenerJs(f)
private fun caNewAnimationListener(f: dynamic): dynamic = caNewAnimationListenerJs(f)
private fun caNewRunnable(f: dynamic): dynamic = caNewRunnableJs(f)
private fun caNewMotionCancel(): dynamic = caNewMotionCancelJs()
private fun caNewLengthFilter(n: dynamic): dynamic = caNewLengthFilterJs(n)
private fun caNewPopupPage(view: dynamic, name: dynamic): dynamic = caNewPopupPageJs(view, name)
private fun caNewRhinoListAdapter(arr: dynamic, vmaker: dynamic): dynamic =
    caNewRhinoListAdapterJs(arr, vmaker)
private fun caNewSimpleListAdapter(arr: dynamic, maker: dynamic, binder: dynamic): dynamic =
    caNewSimpleListAdapterJs(arr, maker, binder)
private fun caSimpleGetController(adapter: dynamic): dynamic = caSimpleGetControllerJs(adapter)
private fun caNewExpandableListAdapter(
    root: dynamic, getChildren: dynamic, itemMaker: dynamic, itemBinder: dynamic,
    groupMaker: dynamic, groupBinder: dynamic
): dynamic = caNewExpandableListAdapterJs(root, getChildren, itemMaker, itemBinder, groupMaker, groupBinder)
private fun caExpandableControl(adapter: dynamic): dynamic = caExpandableControlJs(adapter)
private fun caNewCanvas(bitmap: dynamic): dynamic = caNewCanvasJs(bitmap)
private fun caNewPaint(): dynamic = caNewPaintJs()
private fun caNewPath(): dynamic = caNewPathJs()
private fun caBitmapCreate(width: dynamic, height: dynamic, config: dynamic): dynamic =
    caBitmapCreateJs(width, height, config)

// ---------------- showMain / showGen ----------------

private fun caShowMain(api: dynamic, noAnimation: dynamic): dynamic {
    api.showGen(noAnimation)
    return js("undefined")
}

private fun caShowGen(self: dynamic, api: dynamic, noani: dynamic): dynamic {
    js("G").ui({
        caShowGenUi(self, api, noani)
        js("undefined")
    })
    return js("undefined")
}

private fun caShowGenUi(self: dynamic, api: dynamic, noani: dynamic): dynamic {
    try {
        if (!caTruthy(self.main)) {
            caBuildGenUi(self, api)
        }
        api.gen.enter()
        self.textUpdate(api.cmd.getText())
        self.activate(false)
        // 无动画直接返回：只跳过下面的入场动画。
        if (caTruthy(noani)) return js("undefined")

        var animation = caNewTranslate8(
            js("G.Animation.RELATIVE_TO_SELF"), 0,
            js("G.Animation.RELATIVE_TO_SELF"), 0,
            js("G.Animation.RELATIVE_TO_SELF"), if (caTruthy(api.settings.barTop)) -1 else 1,
            js("G.Animation.RELATIVE_TO_SELF"), 0
        )
        animation.setInterpolator(caNewDecelerate(2.0))
        animation.setDuration(100)
        animation.setStartOffset(100)
        self.bar.startAnimation(animation)
        animation = caNewTranslate8(
            js("G.Animation.RELATIVE_TO_SELF"), 0,
            js("G.Animation.RELATIVE_TO_SELF"), 0,
            js("G.Animation.RELATIVE_TO_SELF"), if (caTruthy(api.settings.barTop)) 1 else -1,
            js("G.Animation.RELATIVE_TO_SELF"), 0
        )
        animation.setInterpolator(caNewDecelerate(2))
        animation.setDuration(200)
        self.bgContainer.startAnimation(animation)
        //if (self.tutor) self.tutor();
    } catch (e: dynamic) {
        js("erp")(e)
    }
    return js("undefined")
}

private fun caGenGetBgImage(api: dynamic): dynamic {
    if (caTruthy(api.settings.bgImage)) {
        val drawable = js("G.Drawable").createFromPath(api.settings.bgImage)
        if (caInstanceOf(drawable, js("G.AnimatedImageDrawable"))) {
            drawable.setRepeatCount(-1)
            drawable.start()
        }
        return drawable
    }
    return js("undefined")
}

private fun caBuildGenUi(self: dynamic, api: dynamic) {
    self.cmdEdit = caArray(
        caMenuItem("插入……", { _v: dynamic ->
            js("Common").showOperateDialog(self.insertDialog)
            js("undefined")
        }),
        caMenuItem("显示样式代码栏", { _v: dynamic ->
            api.showFCS(api.cmd.getText())
            js("undefined")
        }),
        caMenuItem("创建批量生成模板", { _v: dynamic, tag: dynamic ->
            api.showBatchBuilder(tag.cmd)
            js("undefined")
        }),
        caMenuGap(caDp(10)),
        caMenuItemHidden(
            "切换全屏/悬浮窗",
            { -> !caTruthy(js("PopupPage").supportResize) },
            { _v: dynamic ->
                js("PopupPage").setFullScreen(
                    !caTruthy(js("PopupPage").isFullScreen()),
                    js("PopupPage").isLocked()
                )
                js("undefined")
            }
        ),
        caMenuItemHidden(
            "插件",
            { -> caLength(api.PluginMenu) == 0 },
            { _v: dynamic ->
                js("Common").showOperateDialog(api.PluginMenu)
                js("undefined")
            }
        ),
        caMenuItem("教程", { _v: dynamic ->
            js("Tutorial").showList()
            js("undefined")
        }),
        caMenuItem("设置", { _v: dynamic ->
            api.showSettings()
            js("undefined")
        })
    )
    if (caTruthy(js("G").supportFloat)) {
        self.cmdEdit.push(caMenuItem("退出命令助手", { _v: dynamic ->
            api.performExit()
            js("undefined")
        }))
    }

    self.insertDialog = caArray(
        caMenuItem("JSON/组件", { _v: dynamic ->
            caGenInsertJson(self, api)
            js("undefined")
        }),
        caMenuItem("英文ID", { _v: dynamic ->
            api.chooseIDList({ text: dynamic ->
                js("Common").replaceSelection(api.cmd.getText(), text)
                js("undefined")
            })
            js("undefined")
        }),
        caMenuItem("短语", { _v: dynamic ->
            api.showCustomExpression()
            js("undefined")
        })
    )

    self.getBgImage = { -> caGenGetBgImage(api) }
    self.showMenu = { ->
        js("Common").showOperateDialog(self.cmdEdit, caCmdTagJs(caString(api.cmd.getText())))
        js("undefined")
    }
    self.performClose = { callback: dynamic -> caGenPerformClose(self, api, callback) }
    self.performCopy = { s: dynamic ->
        caGenPerformCopy(self, api, s)
        js("undefined")
    }
    self.activate = { fl: dynamic ->
        caGenActivate(api, fl)
        js("undefined")
    }

    val textState = CaGenTextState()
    self.textUpdate = { s: dynamic ->
        caGenTextUpdate(self, api, textState, s)
        js("undefined")
    }
    self.pointerChanged = { _p: dynamic ->
        // 即将支持
        js("undefined")
    }
    self.spanWatcher = caNewSpanWatcher(
        { _text: dynamic, what: dynamic, _ostart: dynamic, _oend: dynamic, nstart: dynamic, _nend: dynamic ->
            try {
                if (caStrictEq(what, js("G.Selection.SELECTION_START"))) {
                    self.pointerChanged(nstart)
                }
            } catch (e: dynamic) {
                js("erp")(e)
            }
            js("undefined")
        }
    )
    self.tutor = if (caTruthy(api.settings.tutor_gen)) js("null") else ({ ->
        caGenTutor(self, api)
        js("undefined")
    })

    // ---- 主界面构建 ----

    self.main = caNewLinearLayout()
    self.main.setOrientation(js("G.LinearLayout.VERTICAL"))

    self.bar = caNewLinearLayout()
    self.bar.setLayoutParams(caNewLinearParams2(-1, -2))
    self.bar.setOrientation(js("G.LinearLayout.HORIZONTAL"))
    js("Common").applyStyle(self.bar, "bar_float")

    if (!caTruthy(api.settings.openMenuByLongClick)) {
        self.menu = caNewTextView()
        self.menu.setText("CA")
        self.menu.setTypeface(caOr(js("G.Typeface.MONOSPACE"), js("G.Typeface.DEFAULT")))
        self.menu.setGravity(js("G.Gravity.CENTER"))
        self.menu.setPadding(caDp(10), caDp(10), caDp(10), caDp(10))
        js("Common").applyStyle(self.menu, "button_reactive_auto", 3)
        self.menu.setLayoutParams(caNewLinearParams2(-2, -1))
        self.menu.setOnClickListener(caNewClickListener({ _v: dynamic ->
            try {
                self.showMenu()
            } catch (e: dynamic) {
                js("erp")(e)
            }
            js("undefined")
        }))
        self.bar.addView(self.menu)
    }

    self.add = caNewTextView()
    self.add.setLayoutParams(caNewLinearParams2(-2, -1))
    self.add.setText("╋")
    self.add.setGravity(js("G.Gravity.CENTER"))
    self.add.setPadding(caDp(10), caDp(10), caDp(5), caDp(10))
    js("Common").applyStyle(self.add, "textview_default", 3)
    self.add.setOnClickListener(caNewClickListener({ _v: dynamic ->
        try {
            caGenAddClick(self, api)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }))
    self.bar.addView(self.add)

    api.cmd = caNewEditText()
    api.cmd.setLayoutParams(caNewLinearParams3(-1, -1, 1.0))
    api.cmd.setHint("命令")
    js("Common").applyStyle(api.cmd, "edittext_default", 3)
    api.cmd.setInputType(caBitOr(
        js("G.InputType.TYPE_CLASS_TEXT"),
        js("G.InputType.TYPE_TEXT_FLAG_AUTO_CORRECT")
    ))
    api.cmd.setFilters(caArray(caNewLengthFilter(32768)))
    api.cmd.setFocusableInTouchMode(true)
    api.cmd.setPadding(caDp(5), caDp(10), caDp(10), caDp(10))
    api.cmd.setImeOptions(js("G.EditorInfo.IME_FLAG_NO_FULLSCREEN"))
    api.cmd.setTypeface(caOr(js("G.Typeface.MONOSPACE"), js("G.Typeface.DEFAULT")))
    api.cmd.setText(api.cmdstr)
    api.cmd.addTextChangedListener(caNewTextWatcher({ s: dynamic ->
        try {
            self.textUpdate(s)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }))
    api.cmd.setOnClickListener(caNewClickListener({ _v: dynamic ->
        try {
            caGenCmdClick(self, api)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }))
    api.cmd.setOnTouchListener(caNewTouchListener(
        caTouchFactoryJs { touch: dynamic, v: dynamic, e: dynamic ->
            caGenCmdTouch(self, api, touch, v, e)
        }
    ))
    self.bar.addView(api.cmd)

    self.clear = caNewTextView()
    self.clear.setLayoutParams(caNewLinearParams2(-2, -1))
    self.clear.setText("×")
    self.clear.setGravity(js("G.Gravity.CENTER"))
    self.clear.setPadding(caDp(5), caDp(10), caDp(10), caDp(10))
    js("Common").applyStyle(self.clear, "button_secondary", 3)
    self.clear.setOnClickListener(caNewClickListener({ _v: dynamic ->
        try {
            api.cmd.setText("")
            self.activate(false)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }))
    self.bar.addView(self.clear)

    self.copy = caNewTextView()
    self.copy.setLayoutParams(caNewLinearParams2(-2, -1))
    self.copy.setGravity(js("G.Gravity.CENTER"))
    self.copy.setPadding(caDp(10), caDp(10), caDp(10), caDp(10))
    js("Common").applyStyle(self.copy, "button_reactive", 3)
    self.copy.setOnClickListener(caNewClickListener({ v: dynamic ->
        try {
            caGenCopyClick(self, api, v)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }))
    if (caLooseEq(js("MapScript").host, "Android")) {
        self.copy.setOnLongClickListener(caNewLongClickListener({ _v: dynamic ->
            caGenCopyLongClick(api)
        }))
    }
    self.copy.setOnTouchListener(caNewTouchListener({ v: dynamic, e: dynamic ->
        caGenCopyTouch(v, e)
    }))
    self.bar.addView(self.copy)

    api.con = caNewFrameLayout()
    api.con.setLayoutParams(caNewLinearParams3(-1, 0, 1))
    js("Common").applyStyle(api.con, "container_default")

    var hasBg = false
    self.bgImg = self.getBgImage()
    hasBg = caTruthy(self.bgImg)
    if (hasBg) {
        self.bgAlpha = caParseFloat(api.settings.bgAlpha)
        if (caIsNaN(self.bgAlpha)) self.bgAlpha = 0.75
        self.bgContainer = caNewFrameLayout()
        self.bgContainer.setLayoutParams(caNewLinearParams3(-1, 0, 1))
        self.bgImage = caNewImageView()
        self.bgImage.setScaleType(js("G.ImageView.ScaleType.CENTER_CROP"))
        self.bgImage.setImageDrawable(self.bgImg)
        self.bgImage.setImageAlpha(js("Math").ceil(caMul(api.settings.alpha, 255)))
        self.bgContainer.addView(self.bgImage, caNewFrameParams2(-1, -1))
        api.con.setBackgroundColor(js("Common").setAlpha(
            js("Common").theme.bgcolor,
            js("Math").ceil(caMul(caMul(api.settings.alpha, 255), self.bgAlpha))
        ))
        self.bgContainer.addView(api.con, caNewFrameParams2(-1, -1))
        self.main.addView(self.bgContainer)
    } else {
        self.bgContainer = api.con
        self.main.addView(self.bgContainer)
    }
    self.main.addView(self.bar)

    api.gen = caNewPopupPage(self.main, "ca.Generator")
    api.gen.enterAnimation(js("null"))
    api.gen.exitAnimation(js("null"))
    api.gen.on("exit", { ->
        caGenExitHandler(api)
        js("undefined")
    })
    api.gen.on("resume", { ->
        js("G").ui({
            try {
                self.textUpdate(api.cmd.getText())
            } catch (e: dynamic) {
                js("erp")(e)
            }
            js("undefined")
        })
        js("undefined")
    })

    js("PWM").registerResetFlag(api, "con")
    js("PWM").registerResetFlag(api, "cmd")
    js("PWM").registerResetFlag(self, "main")
}

private fun caGenInsertJson(self: dynamic, api: dynamic) {
    js("JSONEdit").create({ data: dynamic ->
        var d = data
        var showMenu: dynamic = null
        showMenu = { ->
            js("Common").showOperateDialog(caArray(
                caMenuItem("插入该JSON", { _v: dynamic ->
                    val k = js("MapScript").toSource(d)
                    js("Common").replaceSelection(api.cmd.getText(), k)
                    js("undefined")
                }),
                caMenuItem("继续编辑", { _v: dynamic ->
                    val options = caObject()
                    options.source = d
                    options.rootname = "新JSON"
                    options.update = caThis0Js { thisObj: dynamic ->
                        d = thisObj.source
                        showMenu()
                        js("undefined")
                    }
                    if (!caTruthy(js("JSONEdit").show(options))) showMenu()
                    js("undefined")
                }),
                caMenuItem("取消", { _v: dynamic ->
                    js("undefined")
                })
            ))
            js("undefined")
        }
        showMenu()
        js("undefined")
    })
}

private fun caGenAddClick(self: dynamic, api: dynamic) {
    if (caTruthy(api.Library.loadingStatus)) {
        js("Common").toast("拓展包正在加载中，请稍候")
        return
    }
    if (caLooseEq(api.settings.iiMode, 1) || caLooseEq(api.settings.iiMode, 3)) {
        api.Assist.active = true
        api.cmd.setFocusable(false)
        api.cmd.setText(api.cmdstr)
    } else {
        api.cmd.setText("/")
        self.activate(true)
    }
}

private fun caGenCmdClick(self: dynamic, api: dynamic) {
    if (caTruthy(api.Assist.active)) {
        api.Assist.active = false
        api.cmd.setFocusableInTouchMode(true)
        api.cmd.setText(api.cmdstr)
        self.activate(true)
    }
}

private fun caGenCopyClick(self: dynamic, api: dynamic, v: dynamic) {
    val t = api.cmd.getText()
    val s = v.getText()
    if (caLooseEq(s, "复制") || caLooseEq(s, "粘贴")) {
        self.performCopy(t)
    } else {
        self.performClose()
    }
    api.cmd.setText("")
}

private fun caGenCopyLongClick(api: dynamic): dynamic {
    try {
        if (caTruthy(js("WSServer").isConnected())) {
            js("WSServer").sendCommand(caString(api.cmd.getText()), { json: dynamic ->
                js("Common").toast(
                    "已执行！状态代码：" + json.statusCode + "\n" + json.statusMessage
                )
                js("undefined")
            })
        } else {
            if (!caTruthy(js("WSServer").isAvailable())) {
                js("Common").toast("请先在设置中打开WebSocket服务器")
            } else {
                js("Common").toast(
                    "请在客户端输入以下指令之一来连接到服务器。\n" +
                        js("WSServer").getConnectCommands().join("\n")
                )
            }
        }
        return true
    } catch (e: dynamic) {
        js("erp")(e)
        return true
    }
}

private fun caGenCopyTouch(v: dynamic, e: dynamic): dynamic {
    try {
        val action = e.getAction()
        if (caLooseEq(action, e.ACTION_DOWN)) {
            js("Common").applyStyle(v, "button_reactive_pressed", 3)
        } else if (caLooseEq(action, e.ACTION_CANCEL) || caLooseEq(action, e.ACTION_UP)) {
            js("Common").applyStyle(v, "button_reactive", 3)
        }
        return false
    } catch (err: dynamic) {
        js("erp")(err)
        return false
    }
}

private fun caGenCmdTouch(self: dynamic, api: dynamic, touch: dynamic, _v: dynamic, e: dynamic): dynamic {
    try {
        var t: dynamic
        val action = e.getAction()
        if (caTruthy(touch.ignore) && !caLooseEq(action, e.ACTION_DOWN)) return true
        if (caLooseEq(action, e.ACTION_MOVE)) {
            t = e.getRawY() - touch.sy
            if (caTruthy(touch.cbk) && caAbs(t) + caAbs(e.getRawX() - touch.sx) > caDp(20)) {
                self.main.removeCallbacks(touch.cbk)
                touch.cbk = js("null")
            }
            if (t > 0) t = 0
            if (caTruthy(touch.stead) && caAbs(t) < caDp(20)) return false
            touch.stead = false
            self.main.setTranslationY(t)
        } else if (caLooseEq(action, e.ACTION_DOWN)) {
            touch.sx = e.getRawX()
            touch.sy = e.getRawY()
            touch.stead = true
            touch.ignore = false
            if (caTruthy(api.settings.openMenuByLongClick) && !caTruthy(api.Assist.active)) {
                touch.cbk = caNewRunnable({ ->
                    try {
                        self.showMenu()
                        touch.cbk = js("null")
                        api.cmd.dispatchTouchEvent(caNewMotionCancel())
                    } catch (err: dynamic) {
                        js("erp")(err)
                    }
                    js("undefined")
                })
                self.main.postDelayed(touch.cbk, 300)
            }
        } else if (caLooseEq(action, e.ACTION_CANCEL) || caLooseEq(action, e.ACTION_UP)) {
            val isCancel = caLooseEq(action, e.ACTION_CANCEL)
            if (isCancel) touch.ignore = true
            if (caTruthy(touch.cbk)) {
                self.main.removeCallbacks(touch.cbk)
                touch.cbk = js("null")
            }
            self.main.setTranslationY(0)
            if (isCancel || caTruthy(touch.stead)) return false
            t = e.getRawY() - touch.sy
            if (t > 0) t = 0
            if (caAbs(t) > caMul(0.4, self.main.getMeasuredHeight())) {
                if (caTruthy(api.settings.noAnimation)) {
                    api.hideGen()
                } else {
                    val animation = caNewTranslate8(
                        js("G.Animation.RELATIVE_TO_SELF"), 0,
                        js("G.Animation.RELATIVE_TO_SELF"), 0,
                        js("G.Animation.ABSOLUTE"), t,
                        js("G.Animation.ABSOLUTE"), caNeg(self.main.getMeasuredHeight())
                    )
                    animation.setInterpolator(caNewAccelerate(2.0))
                    animation.setDuration(100)
                    animation.setAnimationListener(caNewAnimationListener({ _a: dynamic ->
                        api.hideGen()
                        js("undefined")
                    }))
                    self.main.startAnimation(animation)
                }
            } else if (!caTruthy(api.settings.noAnimation)) {
                val animation = caNewTranslate8(
                    js("G.Animation.RELATIVE_TO_SELF"), 0,
                    js("G.Animation.RELATIVE_TO_SELF"), 0,
                    js("G.Animation.ABSOLUTE"), t,
                    js("G.Animation.RELATIVE_TO_SELF"), 0
                )
                animation.setInterpolator(caNewDecelerate(2.0))
                animation.setDuration(100)
                self.main.startAnimation(animation)
            }
            return true
        }
        return false
    } catch (err: dynamic) {
        js("erp")(err)
        return true
    }
}

private fun caGenPerformClose(self: dynamic, api: dynamic, callback: dynamic): dynamic {
    if (caTruthy(api.settings.noAnimation)) {
        api.hideGen()
        if (caTruthy(callback)) callback()
        return js("undefined")
    }
    var animation = caNewTranslate8(
        js("G.Animation.RELATIVE_TO_SELF"), 0,
        js("G.Animation.RELATIVE_TO_SELF"), 0,
        js("G.Animation.RELATIVE_TO_SELF"), 0,
        js("G.Animation.RELATIVE_TO_SELF"), 1
    )
    animation.setInterpolator(caNewAccelerate(2.0))
    animation.setDuration(100)
    animation.setStartOffset(100)
    self.bar.startAnimation(animation)
    animation = caNewTranslate8(
        js("G.Animation.RELATIVE_TO_SELF"), 0,
        js("G.Animation.RELATIVE_TO_SELF"), 0,
        js("G.Animation.RELATIVE_TO_SELF"), 0,
        js("G.Animation.RELATIVE_TO_SELF"), -1
    )
    animation.setInterpolator(caNewAccelerate(2.0))
    animation.setDuration(200)
    animation.setAnimationListener(caNewAnimationListener({ _a: dynamic ->
        try {
            api.hideGen()
            if (caTruthy(callback)) callback()
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }))
    self.bgContainer.startAnimation(animation)
    return js("undefined")
}

private fun caGenPerformCopy(self: dynamic, api: dynamic, sArg: dynamic) {
    val s = caString(sArg)
    js("Common").setClipboardText(s)
    api.addHistory(s)
    if (caTruthy(api.history)) api.showHistory()
    if (caLooseEq(api.settings.pasteMode, 1)) {
        if (caLength(api.his) != 0) api.showPaste(0)
    } else if (caLooseEq(api.settings.pasteMode, 2)) {
        self.performClose({ ->
            js("gHandler").postDelayed({ ->
                try {
                    api.performPaste(s, true)
                } catch (e: dynamic) {
                    js("erp")(e)
                }
                js("undefined")
            }, 100)
            js("undefined")
        })
        return
    }
    self.performClose()
    if (caLength(s) > 20) js("UserManager").enqueueExp("copyCommand")
}

private fun caGenActivate(api: dynamic, fl: dynamic) {
    api.cmd.requestFocus()
    api.cmd.setSelection(api.cmd.getText().length())
    if (caTruthy(fl)) {
        js("ctx").getSystemService(js("ctx").INPUT_METHOD_SERVICE)
            .showSoftInput(api.cmd, js("G.InputMethodManager.SHOW_IMPLICIT"))
    }
}

private class CaGenTextState {
    var state: Int = -1
}

private fun caGenRep(s: dynamic) {
    js("FCString").clearSpans(s)
    js("FCString").colorFC(s, js("Common").theme.textcolor)
}

private fun caGenGostate0(self: dynamic, api: dynamic, st: CaGenTextState) {
    // 输入内容为空
    st.state = 0
    api.hideAssist()
    api.showHistory()
    self.copy.setText("关闭")
    self.add.setVisibility(js("G.View.VISIBLE"))
    self.clear.setVisibility(js("G.View.GONE"))
}

private fun caGenGostate1(self: dynamic, api: dynamic, st: CaGenTextState) {
    // 输入了命令
    st.state = 1
    if (caLooseEq(api.settings.iiMode, 2) || caLooseEq(api.settings.iiMode, 3)) {
        api.hideHistory()
        api.showAssist()
        api.Assist.hide()
        api.IntelliSense.show()
    } else {
        api.hideAssist()
        api.showHistory()
    }
    self.copy.setText(if (caLooseEq(api.settings.pasteMode, 2)) "粘贴" else "复制")
    self.add.setVisibility(js("G.View.GONE"))
    self.clear.setVisibility(
        if (caTruthy(api.settings.showClearButton)) js("G.View.VISIBLE") else js("G.View.GONE")
    )
}

private fun caGenGostate2(self: dynamic, api: dynamic, st: CaGenTextState) {
    // 输入了 /help
    st.state = 2
    api.hideHistory()
    api.showAssist()
    api.Assist.hide()
    api.IntelliSense.show()
    api.IntelliSense.showHelp()
    self.copy.setText("关闭")
    self.add.setVisibility(js("G.View.GONE"))
    self.clear.setVisibility(js("G.View.VISIBLE"))
}

private fun caGenGostate3(self: dynamic, api: dynamic, st: CaGenTextState) {
    // 辅助输入模式
    st.state = 3
    api.hideHistory()
    api.showAssist()
    api.IntelliSense.hide()
    api.Assist.show()
    api.hideFCS()
    self.copy.setText(if (caLooseEq(api.settings.pasteMode, 2)) "粘贴" else "复制")
    self.add.setVisibility(js("G.View.GONE"))
    self.clear.setVisibility(js("G.View.GONE"))
}

private fun caGenTextUpdate(self: dynamic, api: dynamic, st: CaGenTextState, s: dynamic): dynamic {
    s.setSpan(self.spanWatcher, 0, s.length(), js("G.Spanned.SPAN_INCLUSIVE_EXCLUSIVE"))
    api.cmdstr = caString(s)
    if ((caLooseEq(api.settings.iiMode, 1) || caLooseEq(api.settings.iiMode, 3)) &&
        caTruthy(api.Assist.active)
    ) {
        if (st.state != 3) caGenGostate3(self, api, st)
    } else if (caLooseEq(s, "/help")) {
        if (st.state != 2) caGenGostate2(self, api, st)
    } else if (caTruthy(s.length()) && !caTruthy(api.Library.loadingStatus)) {
        if (st.state != 1) caGenGostate1(self, api, st)
    } else {
        if (st.state != 0) caGenGostate0(self, api, st)
    }
    if (caTruthy(api.fcs)) api.showFCS(s)
    if (caTruthy(api.settings.autoFormatCmd)) caGenRep(s)
    if ((!caLooseEq(api.settings.iiMode, 2) && !caLooseEq(api.settings.iiMode, 3)) || st.state != 1) {
        return js("undefined")
    }
    if (caTruthy(api.settings.senseDelay)) {
        api.IntelliSense.callDelay(caString(s))
    } else {
        api.IntelliSense.proc(caString(s))
    }
    return js("undefined")
}

private fun caGenTutor(self: dynamic, api: dynamic) {
    val l = api.cmd.getText()
    api.cmd.setText("")
    val t1 = caObject()
    t1.text = "命令生成器可以协助你输入命令"
    t1.view = self.main
    js("Common").showTutorial(t1)
    val t2 = caObject()
    t2.text = "按下加号进入命令创建模式"
    t2.view = self.add
    js("Common").showTutorial(t2)
    val t3 = caObject()
    t3.text = "命令输入栏\n\n在此输入命令\n长按显示菜单\n向上拖动可以关闭命令生成器"
    t3.view = api.cmd
    t3.callback = { ->
        api.settings.tutor_gen = true
        api.trySave()
        js("undefined")
    }
    t3.onDismiss = { ->
        api.cmd.setText(l)
        js("undefined")
    }
    js("Common").showTutorial(t3)
    self.tutor = js("null")
}

private fun caGenExitHandler(api: dynamic) {
    if (caTruthy(js("PopupPage").isBusy())) return
    api.screenChangeHook()
    api.trySave()
}

private fun caHideGen(api: dynamic) {
    js("G").ui({
        try {
            if (caTruthy(api.gen.showing)) api.gen.exit()
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

// ---------------- showHistory ----------------

private fun caShowHistory(self: dynamic, api: dynamic): dynamic {
    js("G").ui({
        caShowHistoryUi(self, api)
        js("undefined")
    })
    return js("undefined")
}

private fun caShowHistoryUi(self: dynamic, api: dynamic): dynamic {
    try {
        if (!caTruthy(self.history)) {
            caBuildHistoryUi(self, api)
        }
        self.refreshHistory()
        self.refreshFavorite()
        if (caTruthy(api.history)) return js("undefined")
        api.history = self.linear
        self.lx = 0
        self.tx = self.lx
        self.linear.setTranslationX(self.tx)
        api.con.addView(api.history)
        self.layoutListener.onLayoutChange(
            api.con, 0, 0, api.con.getMeasuredWidth(), api.con.getMeasuredHeight(), 0, 0, 0, 0
        )
        //if (self.tutor) self.tutor();
    } catch (e: dynamic) {
        js("erp")(e)
    }
    return js("undefined")
}

/** tag.folder ? tag.folder.children : null 的共用写法。 */
private fun caHistFolderChildren(tag: dynamic): dynamic {
    return if (caTruthy(tag.folder)) tag.folder.children else js("null")
}

private fun caHistAddFavFromHistory(self: dynamic, api: dynamic, tag: dynamic) {
    val o = caObject()
    o.mode = 0
    val data = caObject()
    data.value = tag.cmd
    o.data = data
    o.callback = caThis0Js { thisObj: dynamic ->
        thisObj.folder.children.push(thisObj.data)
        js("undefined")
    }
    o.onDismiss = { ->
        self.refreshFavorite()
        js("undefined")
    }
    api.showFavEditDialog(o)
}

private fun caHistFavGroupRename(self: dynamic, api: dynamic, tag: dynamic, s: dynamic): dynamic {
    if (!caTruthy(s)) {
        js("Common").toast("名称不能为空")
        return js("undefined")
    }
    if (caTruthy(api.getFavoriteDir(s, caHistFolderChildren(tag), true))) {
        js("Common").toast("名称已存在")
        return js("undefined")
    }
    tag.data.key = s
    self.refreshFavorite()
    return js("undefined")
}

private fun caHistFavHeaderClick(self: dynamic, api: dynamic) {
    val o = caObject()
    o.mode = 0
    o.data = caObject()
    o.callback = caThis0Js { thisObj: dynamic ->
        thisObj.folder.children.push(thisObj.data)
        js("undefined")
    }
    o.onDismiss = { ->
        self.refreshFavorite()
        js("undefined")
    }
    api.showFavEditDialog(o)
}

private fun caHistFavHeaderLongClick(self: dynamic) {
    if (!caTruthy(self.favEmpty)) self.favAdapter.expandAll()
}

private fun caHistFavItemClick(api: dynamic, e: dynamic) {
    if (caLooseEq(e.source, "batch")) {
        api.showBatchBuilder(e.value, true)
    } else {
        api.cmd.setText(e.value)
        api.showGen.activate(false)
    }
}

private fun caHistFavItemLongClick(
    self: dynamic,
    api: dynamic,
    e: dynamic,
    pos: dynamic,
    adpt: dynamic
) {
    val p = adpt.getParent(pos)
    val tag = caObject()
    tag.data = e
    tag.folder = if (caIsNaN(p)) js("null") else adpt.getItem(p)
    js("Common").showOperateDialog(self.favoriteItemEdit, tag)
}

private fun caHistGroupClick(parent: dynamic, pos: dynamic) {
    parent.smoothScrollToPositionFromTop(pos + parent.getHeaderViewsCount(), 0, 100)
}

private fun caHistGroupLongClick(
    self: dynamic,
    api: dynamic,
    e: dynamic,
    pos: dynamic,
    adpt: dynamic
) {
    val p = adpt.getParent(pos)
    val tag = caObject()
    tag.data = e
    tag.folder = if (caIsNaN(p)) js("null") else adpt.getItem(p)
    tag.pos = pos
    js("Common").showOperateDialog(self.favoriteGroupEdit, tag)
}

private fun caHistFavListOptions(self: dynamic, api: dynamic): dynamic {
    val o = caObject()
    o.onHeaderClick = { _pos: dynamic ->
        caHistFavHeaderClick(self, api)
        js("undefined")
    }
    o.onHeaderLongClick = { _pos: dynamic ->
        caHistFavHeaderLongClick(self)
        js("undefined")
    }
    o.onItemClick = { e: dynamic ->
        caHistFavItemClick(api, e)
        js("undefined")
    }
    o.onItemLongClick = { e: dynamic, pos: dynamic, _parent: dynamic, _view: dynamic, adpt: dynamic ->
        caHistFavItemLongClick(self, api, e, pos, adpt)
        js("undefined")
    }
    o.onGroupClick = { _e: dynamic, pos: dynamic, parent: dynamic, _view: dynamic, _adpt: dynamic ->
        caHistGroupClick(parent, pos)
        js("undefined")
    }
    o.onGroupLongClick = { e: dynamic, pos: dynamic, _parent: dynamic, _view: dynamic, adpt: dynamic ->
        caHistGroupLongClick(self, api, e, pos, adpt)
        js("undefined")
    }
    return o
}

private fun caHistItemClick(self: dynamic, api: dynamic, parent: dynamic, pos: dynamic) {
    try {
        if (pos < 1 || !caTruthy(parent.getItemAtPosition(pos))) return
        val p = pos - 1
        api.cmd.setText(api.his[p])
        api.showGen.activate(true)
    } catch (e: dynamic) {
        js("erp")(e)
    }
}

private fun caHistItemLongClick(
    self: dynamic,
    api: dynamic,
    parent: dynamic,
    pos: dynamic
): dynamic {
    try {
        if (pos < 1 || !caTruthy(parent.getItemAtPosition(pos))) return js("undefined")
        val p = pos - 1
        val tag = caObject()
        tag.pos = caParseInt(p)
        tag.cmd = api.his[p]
        js("Common").showOperateDialog(self.historyEdit, tag)
        return true
    } catch (e: dynamic) {
        js("erp")(e)
        return true
    }
}

private fun caHistLayoutChange(
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
        if (caLooseEq(r - l, or - ol)) return js("undefined")
        self.screenWidth = r - l
        if (caTruthy(api.settings.splitScreenMode)) {
            self.linear.setLayoutParams(caNewFrameParams2(self.screenWidth, -1))
            self.history.setLayoutParams(caNewLinearParams2(caMul(self.screenWidth, 0.5), -1))
            self.favorite.setLayoutParams(caNewLinearParams2(caMul(self.screenWidth, 0.5), -1))
        } else {
            self.linear.setLayoutParams(caNewFrameParams2(caMul(self.screenWidth, 2), -1))
            self.history.setLayoutParams(caNewLinearParams2(self.screenWidth, -1))
            self.favorite.setLayoutParams(caNewLinearParams2(self.screenWidth, -1))
        }
        if (self.tx < 0) {
            self.tx = caNeg(self.screenWidth)
            self.linear.setTranslationX(self.tx)
        }
    } catch (e: dynamic) {
        js("erp")(e)
    }
    return js("undefined")
}

private fun caHistScrollerTouch(
    self: dynamic,
    api: dynamic,
    touch: dynamic,
    _v: dynamic,
    e: dynamic,
    touchSlop: dynamic,
    switchSlop: dynamic
): dynamic {
    try {
        var t: dynamic
        var f = false
        val action = e.getAction()
        if (caLooseEq(action, e.ACTION_MOVE)) {
            f = true
            t = self.x
            self.x = e.getRawX()
            if (caTruthy(self.vscr) &&
                caAbs(e.getRawX() - self.sx) - caAbs(e.getRawY() - self.sy) < touchSlop
            ) {
                return false
            }
            // 超过范围，开始滑动
            self.vscr = false
            self.stead = false
            // 计算当前偏移量（当前点X - 上个点X）
            self.tx = self.tx + (e.getRawX() - t)
            if (self.tx > 0) {
                self.tx = 0
            } else if (self.tx < caNeg(self.screenWidth)) {
                self.tx = caNeg(self.screenWidth)
            } else {
                f = false // 未超出范围
            }
            self.linear.setTranslationX(self.tx)
            if (f) return false
            if (caTruthy(self.cancelled)) return true
            e.setAction(e.ACTION_CANCEL) // 取回控制权
            self.cancelled = true
        } else if (caLooseEq(action, e.ACTION_DOWN)) {
            // 开始点
            self.x = e.getRawX()
            self.sx = self.x
            self.sy = e.getRawY()
            self.lx = self.tx // 上个偏移量状态
            self.vscr = true
            self.cancelled = false
        } else if (caLooseEq(action, e.ACTION_CANCEL) || caLooseEq(action, e.ACTION_UP)) {
            if (caTruthy(self.vscr)) return false
            t = self.tx // 计算偏移量
            self.tx = if (caAbs(t - self.lx) < switchSlop) {
                self.lx
            } else if (t < self.lx) {
                caNeg(self.screenWidth)
            } else {
                0
            }
            self.linear.setTranslationX(self.tx)
            if (!caTruthy(api.settings.noAnimation)) { // 动画
                val animation = caNewTranslate8(
                    js("G.Animation.ABSOLUTE"), t - self.tx,
                    js("G.Animation.ABSOLUTE"), 0,
                    js("G.Animation.RELATIVE_TO_SELF"), 0,
                    js("G.Animation.RELATIVE_TO_SELF"), 0
                )
                animation.setInterpolator(caNewDecelerate(1.0))
                animation.setDuration(200)
                self.linear.startAnimation(animation)
            }
            if (caTruthy(self.cancelled)) return true
        }
        return false
    } catch (err: dynamic) {
        js("erp")(err)
        return true
    }
}

private fun caHistGetFavChildren(e: dynamic): dynamic {
    val d = js("[]")
    val g = js("[]")
    val a = e.children
    if (!caTruthy(a)) return js("undefined")
    caForIn(a, { i: dynamic ->
        if (caTruthy(a[i].children)) {
            g.push(a[i])
        } else {
            d.push(a[i])
        }
        js("undefined")
    })
    return g.concat(d)
}

private fun caHistRefreshHistory(self: dynamic, api: dynamic, force: dynamic) {
    if (caLength(api.his) == 0) {
        self.hisEmpty = true
        self.history.setAdapter(js("EmptyAdapter"))
    } else {
        if (caTruthy(self.hisEmpty) || caTruthy(force)) {
            self.hisAdapter.notifyChange()
            self.history.setAdapter(self.hisAdapter.self)
        } else {
            self.hisAdapter.notifyChange()
        }
        self.hisEmpty = false
    }
    if (caTruthy(api.paste)) api.showPaste.refresh()
}

private fun caHistRefreshFavorite(self: dynamic, api: dynamic) {
    if (caLength(api.fav) == 0) {
        self.favEmpty = true
        self.favorite.setAdapter(js("EmptyAdapter"))
    } else {
        caSpliceReplaceJs(self.favList, self.getFavChildren(caChildrenObj(api.fav)))
        self.favAdapter.updateAll()
        if (caTruthy(self.favEmpty)) self.favorite.setAdapter(self.favAdapter.self)
        self.favEmpty = false
    }
}

private fun caHistScrollToLeft(self: dynamic, api: dynamic, noani: dynamic) {
    self.linear.setTranslationX(0)
    if (!caTruthy(api.settings.noAnimation) && !caTruthy(noani)) {
        val animation = caNewTranslate8(
            js("G.Animation.ABSOLUTE"), self.tx,
            js("G.Animation.ABSOLUTE"), 0,
            js("G.Animation.RELATIVE_TO_SELF"), 0,
            js("G.Animation.RELATIVE_TO_SELF"), 0
        )
        animation.setInterpolator(caNewDecelerate(1.0))
        animation.setDuration(300)
        self.linear.startAnimation(animation)
    }
    self.tx = 0
}

private fun caHistScrollToRight(self: dynamic, api: dynamic, noani: dynamic) {
    self.linear.setTranslationX(caNeg(self.screenWidth))
    if (!caTruthy(api.settings.noAnimation) && !caTruthy(noani)) {
        val animation = caNewTranslate8(
            js("G.Animation.ABSOLUTE"), self.screenWidth + self.tx,
            js("G.Animation.ABSOLUTE"), 0,
            js("G.Animation.RELATIVE_TO_SELF"), 0,
            js("G.Animation.RELATIVE_TO_SELF"), 0
        )
        animation.setInterpolator(caNewDecelerate(1.0))
        animation.setDuration(300)
        self.linear.startAnimation(animation)
    }
    self.tx = caNeg(self.screenWidth)
}

private fun caHistDrawCursor(api: dynamic, height: dynamic): dynamic {
    val width = height
    val bmp = caBitmapCreate(width, height, js("G.Bitmap.Config.ARGB_8888"))
    val cv = caNewCanvas(bmp)
    val pa = caNewPaint()
    pa.setStyle(js("G.Paint.Style.FILL"))
    js("IntColor").Paint.setColor(pa, js("Common").theme.promptcolor)
    pa.setAntiAlias(true)
    val ph = caNewPath()
    ph.moveTo(caMul(0.3, width), caMul(0.3, height))
    ph.lineTo(caMul(0.7, width), caMul(0.5, height))
    ph.lineTo(caMul(0.3, width), caMul(0.7, height))
    ph.close()
    cv.drawPath(ph, pa)
    return bmp
}

private fun caHistHisMaker(self: dynamic, api: dynamic, holder: dynamic): dynamic {
    val layout = caNewLinearLayout()
    val text1 = caNewTextView()
    holder.text = text1
    val text2 = caNewTextView()
    layout.setLayoutParams(caNewAbsParams(-1, -2))
    layout.setOrientation(js("G.LinearLayout.HORIZONTAL"))
    text1.setPadding(caDp(15), caDp(15), 0, caDp(15))
    text1.setLayoutParams(caNewLinearParams3(0, -2, 1.0))
    text1.setMaxLines(2)
    text1.setEllipsize(js("G.TextUtils.TruncateAt.END"))
    js("Common").applyStyle(text1, "textview_default", 3)
    layout.addView(text1)
    text2.setPadding(caDp(15), 0, caDp(15), 0)
    text2.setLayoutParams(caNewLinearParams2(-2, -1))
    text2.setText("📋")
    text2.setGravity(js("G.Gravity.CENTER"))
    js("Common").applyStyle(text2, "textview_prompt", 3)
    text2.setOnClickListener(caNewClickListener({ _v: dynamic ->
        caHistPasteClick(api, holder)
    }))
    layout.addView(text2)
    return layout
}

private fun caHistPasteClick(api: dynamic, holder: dynamic): dynamic {
    try {
        api.showGen.performCopy(holder.value)
        return true
    } catch (e: dynamic) {
        js("erp")(e)
        return js("undefined")
    }
}

private fun caHistHisBinder(holder: dynamic, s: dynamic) {
    holder.value = s
    holder.text.setText(holder.value)
}

private fun caHistFavItemMaker(holder: dynamic): dynamic {
    val layout = caNewLinearLayout()
    val text1 = caNewTextView()
    holder.text1 = text1
    val text2 = caNewTextView()
    holder.text2 = text2
    layout.setLayoutParams(caNewAbsParams(-1, -2))
    layout.setOrientation(js("G.LinearLayout.VERTICAL"))
    text1.setPadding(caDp(15), caDp(15), caDp(15), caDp(5))
    text1.setLayoutParams(caNewLinearParams2(-1, -2))
    js("Common").applyStyle(text1, "textview_default", 3)
    layout.addView(text1)
    text2.setPadding(caDp(15), 0, caDp(15), caDp(15))
    text2.setLayoutParams(caNewLinearParams2(-1, -2))
    text2.setEllipsize(js("G.TextUtils.TruncateAt.END"))
    text2.setSingleLine(true)
    js("Common").applyStyle(text2, "textview_prompt", 1)
    layout.addView(text2)
    return layout
}

private fun caHistFavItemBinder(holder: dynamic, e: dynamic, depth: dynamic) {
    holder.text1.setText(e.key)
    holder.text2.setText(e.value)
    holder.self.setPadding(caMul(depth, caDp(16)), 0, 0, 0)
}

private fun caHistFavGroupMaker(self: dynamic, holder: dynamic): dynamic {
    val layout = caNewLinearLayout()
    val img = caNewImageView()
    holder.img = img
    val text = caNewTextView()
    holder.text = text
    layout.setLayoutParams(caNewAbsParams(-1, -2))
    layout.setOrientation(js("G.LinearLayout.HORIZONTAL"))
    img.setLayoutParams(caNewLinearParams2(-2, -2))
    img.setImageBitmap(self.cursorImg)
    layout.addView(img)
    text.setPadding(0, 0, caDp(15), 0)
    text.setLayoutParams(caNewLinearParams2(-1, -1))
    text.setEllipsize(js("G.TextUtils.TruncateAt.END"))
    text.setGravity(caBitOr(js("G.Gravity.CENTER_VERTICAL"), js("G.Gravity.LEFT")))
    text.setSingleLine(true)
    js("Common").applyStyle(text, "textview_default", 3)
    layout.addView(text)
    return layout
}

private fun caHistFavGroupBinder(holder: dynamic, e: dynamic, depth: dynamic, isExpanded: dynamic) {
    holder.img.setRotation(if (caTruthy(isExpanded)) 90 else 0)
    holder.text.setText(e.key)
    holder.self.setPadding(caMul(depth, caDp(16)), 0, 0, 0)
}

private fun caHistTutor(self: dynamic, api: dynamic) {
    val lhis = api.his
    val lfav = api.fav
    api.his = caArray(
        "/say 欢迎使用命令助手!",
        "/clone 45 5 3 47 5 5 50 2 1",
        "/execute @p[x=45,y=6,z=3,dx=2,dy=2,dz=2,m=0] ~ ~ ~ gamemode 1"
    )
    val favFolder = caObject()
    favFolder.key = "收藏夹"
    val favChild = caObject()
    favChild.key = "设置title"
    favChild.value = "/title @a times \${渐入时间:param} \${显示时间:param} \${渐出时间:param}"
    favFolder.children = caArray(favChild)
    val fav1 = caObject()
    fav1.key = "获得命令方块"
    fav1.value = "/give @p command_block"
    val fav2 = caObject()
    fav2.key = "关闭命令提示"
    fav2.value = "/gamerule commandblockoutput false"
    val fav3 = caObject()
    fav3.key = "命令助手设置"
    fav3.value = "/help"
    api.fav = caArray(favFolder, fav1, fav2, fav3)

    self.refreshHistory(true)
    self.refreshFavorite(true)

    val t1 = caObject()
    t1.text = "左侧这里是使用命令的历史记录列表"
    t1.view = if (caTruthy(api.settings.splitScreenMode)) self.history else self.linear
    t1.callback = { ->
        self.scrollToLeft()
        js("undefined")
    }
    js("Common").showTutorial(t1)

    val t2 = caObject()
    t2.text = "点击条目将会编辑该命令\n点击右侧粘贴按钮可粘贴该命令\n长按弹出上下文菜单"
    t2.view = self.history
    js("Common").showTutorial(t2)

    val t3 = caObject()
    t3.text = "右侧这里是使用命令的收藏夹"
    t3.view = if (caTruthy(api.settings.splitScreenMode)) self.favorite else self.linear
    t3.callback = { ->
        self.scrollToRight()
        js("undefined")
    }
    js("Common").showTutorial(t3)

    val t4 = caObject()
    t4.text = "收藏夹类似于文件夹，您可以轻松分类命令"
    t4.view = self.favorite
    t4.callback = { ->
        self.favAdapter.expandAll()
        js("undefined")
    }
    js("Common").showTutorial(t4)

    val t5 = caObject()
    t5.text = "点击条目进入编辑状态\n点击组展开或折叠收藏夹\n长按打开上下文菜单"
    t5.view = self.favorite
    t5.callback = { ->
        api.settings.tutor_his = true
        api.trySave()
        js("undefined")
    }
    t5.onDismiss = { ->
        api.his = lhis
        api.fav = lfav
        self.refreshHistory(true)
        self.refreshFavorite(true)
        self.scrollToLeft()
        js("undefined")
    }
    js("Common").showTutorial(t5)
    self.tutor = js("null")
}

private fun caBuildHistoryUi(self: dynamic, api: dynamic) {
    self.historyEdit = caArray(
        caMenuItem("复制", { _v: dynamic, tag: dynamic ->
            js("Common").setClipboardText(tag.cmd)
            js("Common").toast("已复制到您的剪贴板～")
            js("undefined")
        }),
        caMenuItem("添加收藏", { _v: dynamic, tag: dynamic ->
            caHistAddFavFromHistory(self, api, tag)
            js("undefined")
        }),
        caMenuItem("删除", { _v: dynamic, tag: dynamic ->
            api.his.splice(tag.pos, 1)
            js("Common").toast("已删除")
            self.refreshHistory()
            js("undefined")
        }),
        caMenuItem("批量编辑", { _v: dynamic, tag: dynamic ->
            api.showHistoryEdit(tag.pos, { ->
                self.refreshHistory()
                js("undefined")
            })
            js("undefined")
        })
    )

    self.favoriteItemEdit = caArray(
        caMenuItemHidden(
            "快速输入",
            { tag: dynamic -> !caLooseEq(tag.data.source, "batch") },
            { _v: dynamic, tag: dynamic ->
                api.cmd.setText(tag.data.value)
                api.showGen.activate(false)
                js("undefined")
            }
        ),
        caMenuItem("复制", { _v: dynamic, tag: dynamic ->
            js("Common").setClipboardText(tag.data.value)
            js("Common").toast("已复制到您的剪贴板～")
            js("undefined")
        }),
        caMenuItemHidden(
            "从模板创建",
            { tag: dynamic -> caLooseEq(tag.data.source, "batch") },
            { _v: dynamic, tag: dynamic ->
                api.showBatchBuilder(tag.data.value, true)
                js("undefined")
            }
        ),
        caMenuItem("编辑", { _v: dynamic, tag: dynamic ->
            val o = caObject()
            o.mode = 1
            o.data = tag.data
            o.folder = tag.folder
            o.onDismiss = { ->
                self.refreshFavorite()
                js("undefined")
            }
            api.showFavEditDialog(o)
            js("undefined")
        }),
        caMenuItem("移动", { _v: dynamic, tag: dynamic ->
            val o = caObject()
            o.mode = 2
            o.data = tag.data
            o.folder = tag.folder
            o.callback = caThis0Js { thisObj: dynamic ->
                api.removeFavorite(tag.data, caHistFolderChildren(tag))
                api.addFavorite(thisObj.data, thisObj.folder.children)
                js("undefined")
            }
            o.onDismiss = { ->
                self.refreshFavorite()
                js("undefined")
            }
            api.showFavEditDialog(o)
            js("undefined")
        }),
        caMenuItem("删除", { _v: dynamic, tag: dynamic ->
            api.removeFavorite(tag.data, caHistFolderChildren(tag))
            js("Common").toast("已删除")
            self.refreshFavorite()
            js("undefined")
        }),
        caMenuItem("批量编辑", { _v: dynamic, tag: dynamic ->
            api.showFavoriteEdit(tag.data, { ->
                self.refreshFavorite()
                js("undefined")
            })
            js("undefined")
        })
    )

    self.favoriteGroupEdit = caArray(
        caMenuItemHidden(
            "全部展开",
            { tag: dynamic -> self.favAdapter.isExpanded(tag.pos) },
            { _v: dynamic, tag: dynamic ->
                self.favAdapter.expandTree(tag.pos)
                js("undefined")
            }
        ),
        caMenuItemHidden(
            "全部折叠",
            { tag: dynamic -> !self.favAdapter.isExpanded(tag.pos) },
            { _v: dynamic, tag: dynamic ->
                self.favAdapter.collapseTree(tag.pos)
                js("undefined")
            }
        ),
        caMenuItem("重命名", { _v: dynamic, tag: dynamic ->
            val o = caObject()
            o.title = "重命名"
            o.callback = { s: dynamic -> caHistFavGroupRename(self, api, tag, s) }
            o.singleLine = true
            o.defaultValue = tag.data.key
            js("Common").showInputDialog(o)
            js("undefined")
        }),
        caMenuItem("移动", { _v: dynamic, tag: dynamic ->
            val o = caObject()
            o.mode = 2
            o.data = tag.data
            o.folder = tag.folder
            o.hiddenFolder = caArray(tag.data)
            o.callback = caThis0Js { thisObj: dynamic ->
                api.removeFavorite(tag.data, caHistFolderChildren(tag))
                api.addFavorite(thisObj.data, thisObj.folder.children)
                js("undefined")
            }
            o.onDismiss = { ->
                self.refreshFavorite()
                js("undefined")
            }
            api.showFavEditDialog(o)
            js("undefined")
        }),
        caMenuItem("删除", { _v: dynamic, tag: dynamic ->
            api.removeFavorite(tag.data, caHistFolderChildren(tag))
            js("Common").toast("已删除")
            self.refreshFavorite()
            js("undefined")
        }),
        caMenuItem("批量编辑", { _v: dynamic, tag: dynamic ->
            api.showFavoriteEdit(tag.data, { ->
                self.refreshFavorite()
                js("undefined")
            })
            js("undefined")
        })
    )

    self.drawCursor = { height: dynamic -> caHistDrawCursor(api, height) }
    self.hismaker = { holder: dynamic -> caHistHisMaker(self, api, holder) }
    self.hisbinder = { holder: dynamic, s: dynamic ->
        caHistHisBinder(holder, s)
        js("undefined")
    }
    self.favimaker = { holder: dynamic -> caHistFavItemMaker(holder) }
    self.favibinder = { holder: dynamic, e: dynamic, _i: dynamic, _a: dynamic, depth: dynamic ->
        caHistFavItemBinder(holder, e, depth)
        js("undefined")
    }
    self.favgmaker = { holder: dynamic -> caHistFavGroupMaker(self, holder) }
    self.favgbinder = {
            holder: dynamic,
            e: dynamic,
            _i: dynamic,
            _a: dynamic,
            depth: dynamic,
            isExpanded: dynamic ->
        caHistFavGroupBinder(holder, e, depth, isExpanded)
        js("undefined")
    }
    self.getFavChildren = { e: dynamic -> caHistGetFavChildren(e) }
    self.refreshHistory = { force: dynamic ->
        caHistRefreshHistory(self, api, force)
        js("undefined")
    }
    self.refreshFavorite = { _force: dynamic ->
        caHistRefreshFavorite(self, api)
        js("undefined")
    }
    self.scrollToLeft = { noani: dynamic ->
        caHistScrollToLeft(self, api, noani)
        js("undefined")
    }
    self.scrollToRight = { noani: dynamic ->
        caHistScrollToRight(self, api, noani)
        js("undefined")
    }
    self.tutor = if (caTruthy(api.settings.tutor_his)) js("null") else ({ ->
        caHistTutor(self, api)
        js("undefined")
    })

    // 原实现的赋值顺序是 self.hisEmpty = self.favEmpty = true。
    self.favEmpty = true
    self.hisEmpty = self.favEmpty
    self.cursorImg = self.drawCursor(caDp(32))
    // 原实现 bug：self.nula 从未定义，这里按原样保留（vmaker 为 undefined）。
    self.nulAdapter = caNewRhinoListAdapter(caArray(js("null")), self.nula)
    self.hisAdapter = caSimpleGetController(
        caNewSimpleListAdapter(api.his, self.hismaker, self.hisbinder)
    )
    self.favList = caArray()
    self.favAdapter = caExpandableControl(
        caNewExpandableListAdapter(
            self.favList,
            self.getFavChildren,
            self.favimaker,
            self.favibinder,
            self.favgmaker,
            self.favgbinder
        )
    )

    self.linear = caNewLinearLayout()
    self.linear.setOrientation(js("G.LinearLayout.HORIZONTAL"))
    self.tag1 = caNewTextView()
    self.tag1.setLayoutParams(caNewAbsParams(-1, -2))
    self.tag1.setText("历史")
    self.tag1.setGravity(js("G.Gravity.LEFT"))
    self.tag1.setPadding(caDp(10), caDp(5), caDp(5), caDp(5))
    self.tag1.setFocusable(true)
    js("Common").applyStyle(self.tag1, "textview_prompt", 1)
    self.history = caNewListView()
    self.history.setOnItemClickListener(caNewItemClickListener(
        { parent: dynamic, _view: dynamic, pos: dynamic, _id: dynamic ->
            caHistItemClick(self, api, parent, pos)
            js("undefined")
        }
    ))
    self.history.setOnItemLongClickListener(caNewItemLongClickListener(
        { parent: dynamic, _view: dynamic, pos: dynamic, _id: dynamic ->
            caHistItemLongClick(self, api, parent, pos)
        }
    ))
    self.history.addHeaderView(self.tag1)
    self.linear.addView(self.history)
    self.tag2 = caNewTextView()
    self.tag2.setLayoutParams(caNewAbsParams(-1, -2))
    self.tag2.setText("收藏")
    self.tag2.setGravity(js("G.Gravity.LEFT"))
    self.tag2.setPadding(caDp(10), caDp(5), caDp(5), caDp(5))
    js("Common").applyStyle(self.tag2, "textview_prompt", 1)
    self.favorite = caNewListView()
    self.favorite.addHeaderView(self.tag2)
    self.favAdapter.bindListView(self.favorite, caHistFavListOptions(self, api))
    self.linear.addView(self.favorite)
    self.layoutListener = caNewLayoutChangeListener(
        { v: dynamic, l: dynamic, t: dynamic, r: dynamic, b: dynamic,
          ol: dynamic, ot: dynamic, or: dynamic, ob: dynamic ->
            caHistLayoutChange(self, api, v, l, t, r, b, ol, ot, or, ob)
        }
    )
    api.con.addOnLayoutChangeListener(self.layoutListener)
    if (caLooseEq(js("G").style, "Material")) {
        self.history.setVerticalScrollbarPosition(js("G.View.SCROLLBAR_POSITION_LEFT"))
        self.history.setFastScrollEnabled(true)
        self.history.setFastScrollAlwaysVisible(false)
        self.favorite.setFastScrollEnabled(true)
        self.favorite.setFastScrollAlwaysVisible(false)
    }
    if (!caTruthy(api.settings.splitScreenMode)) {
        val touchSlop = js("G.ViewConfiguration").get(js("ctx")).getScaledTouchSlop()
        val switchSlop = caDp(80)
        self.scroller = caTouchFactoryJs { touch: dynamic, v: dynamic, e: dynamic ->
            caHistScrollerTouch(self, api, touch, v, e, touchSlop, switchSlop)
        }
        self.history.setOnTouchListener(self.scroller)
        self.favorite.setOnTouchListener(self.scroller)
    }
    js("PWM").registerResetFlag(api, "history")
    js("PWM").registerResetFlag(self, "history")
}

private fun caHideHistory(api: dynamic) {
    js("G").ui({
        caHideHistoryUi(api)
        js("undefined")
    })
}

private fun caHideHistoryUi(api: dynamic) {
    try {
        if (!caTruthy(api.history)) return
        api.con.removeView(api.history)
        api.history = js("null")
    } catch (e: dynamic) {
        js("erp")(e)
    }
}

// ---------------- 安装 ----------------

fun installCaMain(api: dynamic) {
    api.showMain = { noAnimation: dynamic ->
        caShowMain(api, noAnimation)
    }
    api.showGen = caShowGenFactoryJs { self: dynamic, args: dynamic ->
        caShowGen(self, api, args[0])
    }
    api.hideGen = { ->
        caHideGen(api)
        js("undefined")
    }
    api.showHistory = caShowHistoryFactoryJs { self: dynamic, args: dynamic ->
        caShowHistory(self, api)
    }
    api.hideHistory = { ->
        caHideHistory(api)
        js("undefined")
    }
}
