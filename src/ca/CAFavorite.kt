package ca.kotlin

/**
 * modules/CA.js 的 favorite 组 Kotlin/JS 版本（原文件 1765-2706 行）。
 *
 * 只提供 installCaFavorite(api: dynamic)，把本组六个顶层成员挂到传入的 api 上；
 * 不注册模块、不定义 caApi、不调用安装函数，也不重复实现 core/main 组字段。
 *
 * 原实现的四个 UI 入口都是具名函数表达式 `function self(...)`，并把
 * getChildren / vmaker / refresh / actions / linear 等一次性构建结果缓存到
 * 函数对象自身。这里用薄 JS wrapper 生成同名的 `function self(...)`，把真实
 * self 传回 Kotlin；缓存仍然挂在同一个函数对象上，跨次调用语义不变。
 * 所有原来是 `this` 的引用改成显式 api，跨组调用走 api.xxx。
 *
 * 原 JS 的已知毛病（回调依赖 this、删除循环从 length 开始、removeFavorite
 * 不带 folder、getChildren 只初始化一次等）按原样保留，并在对应位置注释。
 */

// ---------------- JS 语义 helper ----------------

private val cafTruthyJs: dynamic = js("function (v) { return !!v; }")
private fun cafTruthy(v: dynamic): Boolean = cafTruthyJs(v)

private val cafLooseEqJs: dynamic = js("function (a, b) { return a == b; }")
private fun cafLooseEq(a: dynamic, b: dynamic): Boolean = cafLooseEqJs(a, b)

private val cafNotNullJs: dynamic = js("function (v) { return v != null; }")
private fun cafNotNull(v: dynamic): Boolean = cafNotNullJs(v)

private val cafOrJs: dynamic = js("function (a, b) { return a || b; }")
private fun cafOr(a: dynamic, b: dynamic): dynamic = cafOrJs(a, b)

private val cafStringJs: dynamic = js("function (v) { return String(v); }")
private fun cafString(v: dynamic): dynamic = cafStringJs(v)

// 所有 length 统一走 `| 0`，兼容 Rhino 的 Java 数组/包装对象。
private val cafLengthJs: dynamic = js("function (v) { return v.length | 0; }")
private fun cafLength(v: dynamic): Int = cafLengthJs(v)

private val cafSetLengthJs: dynamic = js("function (a, n) { a.length = n; }")
private fun cafSetLength(a: dynamic, n: Int) {
    cafSetLengthJs(a, n)
}

private val cafNegJs: dynamic = js("function (v) { return -v; }")
private fun cafNeg(v: dynamic): dynamic = cafNegJs(v)

private val cafAbsJs: dynamic = js("function (v) { return Math.abs(v); }")
private fun cafAbs(v: dynamic): dynamic = cafAbsJs(v)

private val cafMulJs: dynamic = js("function (a, b) { return a * b; }")
private fun cafMul(a: dynamic, b: dynamic): dynamic = cafMulJs(a, b)

private val cafDpJs: dynamic = js("function (n) { return n * G.dp; }")
private fun cafDp(n: Int): dynamic = cafDpJs(n)

private val cafDepthDpJs: dynamic = js("function (d) { return (1 + d) * 15 * G.dp; }")
private fun cafDepthDp(depth: dynamic): dynamic = cafDepthDpJs(depth)

private val cafNewArrayJs: dynamic = js("function (n) { return new Array(n); }")
private fun cafNewArray(n: Int): dynamic = cafNewArrayJs(n)

private val cafArray1Js: dynamic = js("function (a) { return [a]; }")
private fun cafArray1(a: dynamic): dynamic = cafArray1Js(a)

private val cafThrowJs: dynamic = js("function (m) { throw m; }")
private fun cafThrow(m: dynamic) {
    cafThrowJs(m)
}

private val cafJsonParseJs: dynamic = js("function (s) { return JSON.parse(s); }")
private fun cafJsonParse(s: dynamic): dynamic = cafJsonParseJs(s)

private val cafJsonStringifyJs: dynamic = js("function (v) { return JSON.stringify(v, null, 4); }")
private fun cafJsonStringify(v: dynamic): dynamic = cafJsonStringifyJs(v)

private val cafIsArrayJs: dynamic = js("function (v) { return Array.isArray(v); }")
private fun cafIsArray(v: dynamic): Boolean = cafIsArrayJs(v)

/** 保留回调里的真实 this（原实现有 `this.folder` 的用法）。 */
private val cafThisJs: dynamic = js("function (f) { return function () { f(this); }; }")

/** 生成具名函数表达式 `function self(...)`，与旧 API 的 arity/name 对齐。 */
private val cafSelf0Js: dynamic = js("function (f) { return function self() { f(self); }; }")
private val cafSelf1Js: dynamic = js("function (f) { return function self(o) { f(self, o); }; }")
private val cafSelf2Js: dynamic = js("function (f) { return function self(a, b) { f(self, a, b); }; }")

/** 丢弃 Kotlin Unit 返回值，保持旧 API/回调返回 undefined。 */
private val cafVoid0Js: dynamic = js("function (f) { return function () { f(); }; }")
private val cafVoid1Js: dynamic = js("function (f) { return function (a) { f(a); }; }")
private val cafVoid2Js: dynamic = js("function (f) { return function (a, b) { f(a, b); }; }")

// ---------------- Android UI helper ----------------

private val cafNewLinearLayoutJs: dynamic = js("function () { return new G.LinearLayout(ctx); }")
private val cafNewFrameLayoutJs: dynamic = js("function () { return new G.FrameLayout(ctx); }")
private val cafNewTextViewJs: dynamic = js("function () { return new G.TextView(ctx); }")
private val cafNewEditTextJs: dynamic = js("function () { return new G.EditText(ctx); }")
private val cafNewListViewJs: dynamic = js("function () { return new G.ListView(ctx); }")
private val cafNewImageViewJs: dynamic = js("function () { return new G.ImageView(ctx); }")
private val cafNewCheckBoxJs: dynamic = js("function () { return new G.CheckBox(ctx); }")
private val cafNewHorizontalScrollViewJs: dynamic = js("function () { return new G.HorizontalScrollView(ctx); }")
private val cafNewFrameLayoutParamsJs: dynamic = js(
    "function (w, h) { return new G.FrameLayout.LayoutParams(w, h); }"
)
private val cafNewLinearLayoutParamsJs: dynamic = js(
    "function (w, h) { return new G.LinearLayout.LayoutParams(w, h); }"
)
private val cafNewWeightedLayoutParamsJs: dynamic = js(
    "function (w, h, weight) { return new G.LinearLayout.LayoutParams(w, h, weight); }"
)
private val cafNewAbsListViewParamsJs: dynamic = js(
    "function (w, h) { return new G.AbsListView.LayoutParams(w, h); }"
)
private val cafNewLayoutChangeListenerJs: dynamic = js(
    "function (f) { return new G.View.OnLayoutChangeListener({ onLayoutChange: f }); }"
)
private val cafNewTouchListenerJs: dynamic = js(
    "function (f) { return new G.View.OnTouchListener({ onTouch: f }); }"
)
private val cafNewClickListenerJs: dynamic = js(
    "function (f) { return new G.View.OnClickListener({ onClick: f }); }"
)
private val cafNewItemClickListenerJs: dynamic = js(
    "function (f) { return new G.AdapterView.OnItemClickListener({ onItemClick: f }); }"
)
private val cafNewCheckedChangeListenerJs: dynamic = js(
    "function (f) { return new G.CompoundButton.OnCheckedChangeListener({ onCheckedChanged: f }); }"
)
private val cafNewPopupPageJs: dynamic = js(
    "function (v, n) { return new PopupPage(v, n); }"
)
private val cafNewSimpleListAdapterJs: dynamic = js(
    "function (a, m, b) { return new SimpleListAdapter(a, m, b); }"
)
private val cafNewExpandableListAdapterJs: dynamic = js(
    "function (a, gc, im, ib, gm, gb, p) { return new ExpandableListAdapter(a, gc, im, ib, gm, gb, p); }"
)
private val cafNewTranslateAnimationJs: dynamic = js(
    "function (a, b, c, d, e, f, g, h) { return new G.TranslateAnimation(a, b, c, d, e, f, g, h); }"
)
private val cafNewDecelerateInterpolatorJs: dynamic = js(
    "function (f) { return new G.DecelerateInterpolator(f); }"
)

private fun cafNewLinearLayout(): dynamic = cafNewLinearLayoutJs()
private fun cafNewFrameLayout(): dynamic = cafNewFrameLayoutJs()
private fun cafNewTextView(): dynamic = cafNewTextViewJs()
private fun cafNewEditText(): dynamic = cafNewEditTextJs()
private fun cafNewListView(): dynamic = cafNewListViewJs()
private fun cafNewImageView(): dynamic = cafNewImageViewJs()
private fun cafNewCheckBox(): dynamic = cafNewCheckBoxJs()
private fun cafNewHorizontalScrollView(): dynamic = cafNewHorizontalScrollViewJs()
private fun cafNewFrameLayoutParams(w: dynamic, h: dynamic): dynamic = cafNewFrameLayoutParamsJs(w, h)
private fun cafNewLinearLayoutParams(w: dynamic, h: dynamic): dynamic = cafNewLinearLayoutParamsJs(w, h)
private fun cafNewWeightedLayoutParams(w: dynamic, h: dynamic, weight: dynamic): dynamic =
    cafNewWeightedLayoutParamsJs(w, h, weight)
private fun cafNewAbsListViewParams(w: dynamic, h: dynamic): dynamic = cafNewAbsListViewParamsJs(w, h)
private fun cafNewLayoutChangeListener(f: dynamic): dynamic = cafNewLayoutChangeListenerJs(f)
private fun cafNewTouchListener(f: dynamic): dynamic = cafNewTouchListenerJs(f)
private fun cafNewClickListener(f: dynamic): dynamic = cafNewClickListenerJs(f)
private fun cafNewItemClickListener(f: dynamic): dynamic = cafNewItemClickListenerJs(f)
private fun cafNewCheckedChangeListener(f: dynamic): dynamic = cafNewCheckedChangeListenerJs(f)
private fun cafNewPopupPage(v: dynamic, n: dynamic): dynamic = cafNewPopupPageJs(v, n)
private fun cafNewSimpleListAdapter(a: dynamic, m: dynamic, b: dynamic): dynamic =
    cafNewSimpleListAdapterJs(a, m, b)
private fun cafNewExpandableListAdapter(
    a: dynamic,
    gc: dynamic,
    im: dynamic,
    ib: dynamic,
    gm: dynamic,
    gb: dynamic,
    p: dynamic
): dynamic = cafNewExpandableListAdapterJs(a, gc, im, ib, gm, gb, p)
private fun cafNewTranslateAnimation(
    a: dynamic,
    b: dynamic,
    c: dynamic,
    d: dynamic,
    e: dynamic,
    f: dynamic,
    g: dynamic,
    h: dynamic
): dynamic = cafNewTranslateAnimationJs(a, b, c, d, e, f, g, h)
private fun cafNewDecelerateInterpolator(f: dynamic): dynamic = cafNewDecelerateInterpolatorJs(f)

// ---------------- showFavEditDialog ----------------

private fun cafFavDialogChildren(e: dynamic, params: dynamic): dynamic {
    if (cafTruthy(e.children)) {
        val arr = e.children.filter({ c: dynamic -> cafTruthy(c.children) && params.hiddenFolder.indexOf(c) < 0 })
        if (cafLooseEq(params.selected, e)) {
            val add = js("({})")
            add.key = "新增收藏夹"
            add.newFolder = true
            arr.push(add)
        }
        return arr
    }
    // 无 children 时原实现隐式返回 undefined。
    return js("undefined")
}

private fun cafFavDialogConfirm(
    o: dynamic,
    param: dynamic,
    key: dynamic,
    value: dynamic,
    popup: dynamic
) {
    try {
        if (cafLooseEq(o.mode, 0) || cafLooseEq(o.mode, 1)) {
            if (!cafTruthy(key.length())) {
                js("Common").toast("名称不能为空")
                return
            }
            if (!cafTruthy(value.length())) {
                js("Common").toast("内容不能为空")
                return
            }
            o.data.key = cafString(key.getText())
            o.data.value = cafString(value.getText())
        }
        o.folder = param.selected
        if (cafTruthy(o.callback)) o.callback()
        popup.exit()
    } catch (e: dynamic) {
        js("erp")(e)
    }
}

private fun cafShowFavEditDialog(api: dynamic, self: dynamic, o: dynamic) {
    js("G").ui({
        try {
            cafFavDialogBuild(api, self, o)
        } catch (e: dynamic) {
            js("erp")(e)
        }
    })
}

private fun cafFavDialogBuild(api: dynamic, self: dynamic, o: dynamic) {
    if (!cafTruthy(self.getChildren)) {
        // 原实现把四个回调缓存在函数对象自身，只初始化一次。
        self.getChildren = { e: dynamic, i: dynamic, a: dynamic, depth: dynamic, params: dynamic ->
            cafFavDialogChildren(e, params)
        }
        self.vmaker = { holder: dynamic ->
            val view = cafNewTextView()
            view.setLayoutParams(cafNewAbsListViewParams(-1, -2))
            js("Common").applyStyle(view, "item_default", 2)
            view
        }
        self.ibinder = { holder: dynamic, e: dynamic, i: dynamic, a: dynamic, depth: dynamic ->
            holder.self.setPadding(cafDepthDp(depth), cafDp(15), cafDp(15), cafDp(15))
            holder.self.setText(e.key)
        }
        self.gbinder = {
                holder: dynamic,
                e: dynamic,
                i: dynamic,
                a: dynamic,
                depth: dynamic,
                isExpanded: dynamic,
                params: dynamic ->
            self.ibinder(holder, e, i, a, depth)
            js("Common").applyStyle(
                holder.self,
                if (cafLooseEq(params.selected, e)) "item_highlight" else "item_default",
                2
            )
        }
    }

    val param = js("({})")
    param.hiddenFolder = cafOr(o.hiddenFolder, js("[]"))
    val root = js("({})")
    root.key = "根收藏夹"
    root.children = api.fav
    root.root = true
    val adpt = js("ExpandableListAdapter").control(
        cafNewExpandableListAdapter(
            cafArray1(root),
            self.getChildren,
            self.vmaker,
            self.ibinder,
            self.vmaker,
            self.gbinder,
            param
        )
    )

    val layout = cafNewLinearLayout()
    layout.setLayoutParams(cafNewFrameLayoutParams(-1, -2))
    layout.setOrientation(js("G.LinearLayout.VERTICAL"))
    js("Common").applyStyle(layout, "message_bg")

    val linear = cafNewLinearLayout()
    linear.setLayoutParams(cafNewAbsListViewParams(-1, -2))
    linear.setOrientation(js("G.LinearLayout.VERTICAL"))
    linear.setPadding(cafDp(15), cafDp(5), cafDp(15), cafDp(5))

    val title = cafNewTextView()
    title.setText("添加收藏")
    title.setLayoutParams(cafNewAbsListViewParams(-1, -2))
    title.setPadding(cafDp(15), cafDp(15), cafDp(15), cafDp(15))
    title.setFocusable(true)
    js("Common").applyStyle(title, "textview_default", 4)

    val key = cafNewEditText()
    key.setHint("名称")
    key.setSingleLine(true)
    key.setLayoutParams(cafNewLinearLayoutParams(-1, -2))
    key.setSelection(key.length())
    js("Common").applyStyle(key, "edittext_default", 2)
    linear.addView(key)

    val value = cafNewEditText()
    value.setHint("内容")
    value.setSingleLine(true)
    value.setSelection(value.length())
    js("Common").applyStyle(value, "edittext_default", 2)
    linear.addView(value)

    val folder = cafNewListView()
    folder.addHeaderView(title)
    folder.addHeaderView(linear)
    val bindOpts = js("({})")
    bindOpts.onItemClick = { e: dynamic, pos: dynamic, parent: dynamic, view: dynamic, adpt: dynamic ->
        val inputOpts = js("({})")
        inputOpts.title = "新建收藏夹"
        inputOpts.callback = { s: dynamic ->
            if (!cafTruthy(s)) {
                js("Common").toast("收藏夹名称不能为空")
            } else {
                val t = api.getFavoriteDir(s, adpt.getItem(adpt.getParent(pos)).children)
                if (param.hiddenFolder.indexOf(t) < 0) {
                    param.selected = t
                }
                js("Common").toast("收藏夹已创建")
                adpt.update(adpt.getParent(pos))
            }
        }
        inputOpts.singleLine = true
        js("Common").showInputDialog(inputOpts)
    }
    bindOpts.onGroupClick = { e: dynamic, pos: dynamic, parent: dynamic, view: dynamic, adpt: dynamic ->
        if (!cafLooseEq(param.selected, e)) {
            param.selected = e
            adpt.expand(pos)
        }
        parent.smoothScrollToPositionFromTop(pos + parent.getHeaderViewsCount(), 0, 100)
        adpt.updateAll()
    }
    adpt.bindListView(folder, bindOpts)
    folder.setLayoutParams(cafNewWeightedLayoutParams(-1, 0, 1))
    layout.addView(folder)

    // popup 在 exit 监听器之后才赋值；监听器只在点击时读取，因此用可变局部保留原结构。
    var popup: dynamic = js("undefined")
    val exit = cafNewTextView()
    exit.setLayoutParams(cafNewLinearLayoutParams(-1, -2))
    exit.setText("确定")
    exit.setGravity(js("G.Gravity.CENTER"))
    exit.setPadding(cafDp(10), cafDp(20), cafDp(10), cafDp(20))
    js("Common").applyStyle(exit, "button_critical", 3)
    exit.setOnClickListener(cafNewClickListener({ v: dynamic ->
        cafFavDialogConfirm(o, param, key, value, popup)
    }))
    layout.addView(exit)

    param.selected = if (cafTruthy(o.folder)) o.folder else adpt.getItem(0)
    adpt.revealNode(param.selected)
    adpt.updateAll()
    if (cafTruthy(o.data)) {
        key.setText(cafOr(o.data.key, ""))
        value.setText(cafOr(o.data.value, ""))
    }
    if (cafLooseEq(o.mode, 0)) {
        title.setText(cafOr(o.title, "添加收藏"))
    } else if (cafLooseEq(o.mode, 1)) {
        title.setText(cafOr(o.title, "编辑收藏"))
        adpt.setArray(js("[]"))
    } else if (cafLooseEq(o.mode, 2)) {
        title.setText(cafOr(o.title, "移动收藏"))
        folder.removeHeaderView(linear)
    }

    popup = js("PopupPage").showDialog("ca.FavEditDialog", layout, -1, -2)
    if (cafTruthy(o.onDismiss)) popup.on("exit", o.onDismiss)
}

// ---------------- showAssist / hideAssist ----------------

private fun cafAssistPostHelp(self: dynamic, type: dynamic, text: dynamic) {
    if (cafLooseEq(type, self.htype) && cafLooseEq(text, self.htext)) return
    self.htype = type
    self.htext = text
    self.hUpdate = true
    self.hCheck()
}

private fun cafAssistCheck(api: dynamic, self: dynamic) {
    js("G").ui({
        cafAssistCheckUi(api, self)
    })
}

private fun cafAssistCheckUi(api: dynamic, self: dynamic) {
    try {
        if (!cafTruthy(self.hUpdate)) return
        if (cafTruthy(api.settings.splitScreenMode) || self.tx < 0) self.hLoad()
    } catch (e: dynamic) {
        js("erp")(e)
    }
}

private fun cafAssistLoad(self: dynamic) {
    if (!cafTruthy(self.wvAvailable)) {
        self.hUpdate = false
        return
    }
    self.help.getSettings().setCacheMode(
        if (cafTruthy(js("Updater").isConnected())) {
            android.webkit.WebSettings.LOAD_DEFAULT
        } else {
            android.webkit.WebSettings.LOAD_CACHE_ELSE_NETWORK
        }
    )
    if (cafLooseEq(self.htype, 0)) {
        self.help.loadUrl(self.htext)
    } else if (cafLooseEq(self.htype, 1)) {
        self.help.loadData(self.htext, "text/html; charset=UTF-8", null)
    } else {
        self.help.loadUrl("about:blank")
    }
    self.hUpdate = false
}

private fun cafAssistLayoutChange(
    api: dynamic,
    self: dynamic,
    l: dynamic,
    t: dynamic,
    r: dynamic,
    b: dynamic,
    ol: dynamic,
    ot: dynamic,
    oldRight: dynamic,
    ob: dynamic
) {
    try {
        if (cafLooseEq(r - l, oldRight - ol)) return
        self.screenWidth = r - l
        if (cafTruthy(api.settings.splitScreenMode)) {
            self.linear.setLayoutParams(cafNewFrameLayoutParams(self.screenWidth, -1))
            self.con.setLayoutParams(cafNewLinearLayoutParams(cafMul(self.screenWidth, 0.5), -1))
            self.help.setLayoutParams(cafNewLinearLayoutParams(cafMul(self.screenWidth, 0.5), -1))
        } else {
            self.linear.setLayoutParams(cafNewFrameLayoutParams(cafMul(self.screenWidth, 2), -1))
            self.con.setLayoutParams(cafNewLinearLayoutParams(self.screenWidth, -1))
            self.help.setLayoutParams(cafNewLinearLayoutParams(self.screenWidth, -1))
        }
        if (self.tx < 0) {
            self.tx = cafNeg(self.screenWidth)
            self.linear.setTranslationX(self.tx)
        }
    } catch (e: dynamic) {
        js("erp")(e)
    }
}

private fun cafAssistTouch(
    api: dynamic,
    self: dynamic,
    touchSlop: dynamic,
    switchSlop: dynamic,
    v: dynamic,
    e: dynamic
): dynamic {
    try {
        val action = e.getAction()
        if (cafLooseEq(action, e.ACTION_MOVE)) {
            var f = true
            val t = self.x
            self.x = e.getRawX()
            // 网页情况下检查网页是否滑到最左侧
            if (cafLooseEq(self.lx, cafNeg(self.screenWidth)) &&
                cafTruthy(self.wvAvailable) && !cafLooseEq(self.help.getScrollX(), 0)
            ) {
                return false
            }
            if (cafTruthy(self.vscr) &&
                cafAbs(e.getRawX() - self.sx) - cafAbs(e.getRawY() - self.sy) < touchSlop
            ) {
                return false
            }
            self.vscr = false
            self.stead = false
            self.tx = self.tx + (e.getRawX() - t)
            if (self.tx > 0) {
                self.tx = 0
            } else if (self.tx < cafNeg(self.screenWidth)) {
                self.tx = cafNeg(self.screenWidth)
            } else {
                f = false
            }
            self.linear.setTranslationX(self.tx)
            if (f) return false
            if (cafTruthy(self.cancelled)) return true
            if (cafTruthy(self.hPaused)) {
                self.hPaused = false
                if (cafTruthy(self.wvAvailable)) self.help.onResume()
            }
            self.hCheck() // 检测是否需要加载网页
            e.setAction(e.ACTION_CANCEL)
            self.cancelled = true
            return false
        } else if (cafLooseEq(action, e.ACTION_DOWN)) {
            val rawX = e.getRawX()
            self.x = rawX
            self.sx = rawX
            self.sy = e.getRawY()
            self.lx = self.tx
            self.vscr = true
            self.cancelled = false
            return false
        } else if (cafLooseEq(action, e.ACTION_CANCEL) || cafLooseEq(action, e.ACTION_UP)) {
            if (cafTruthy(self.vscr)) return false
            val t = self.tx
            self.tx = if (cafAbs(t - self.lx) < switchSlop) {
                self.lx
            } else if (t < self.lx) {
                cafNeg(self.screenWidth)
            } else {
                0
            }
            self.linear.setTranslationX(self.tx)
            if (!cafTruthy(api.settings.noAnimation)) {
                val animation = cafNewTranslateAnimation(
                    js("G.Animation.ABSOLUTE"), t - self.tx,
                    js("G.Animation.ABSOLUTE"), 0,
                    js("G.Animation.RELATIVE_TO_SELF"), 0,
                    js("G.Animation.RELATIVE_TO_SELF"), 0
                )
                animation.setInterpolator(cafNewDecelerateInterpolator(1.0))
                animation.setDuration(200)
                self.linear.startAnimation(animation)
            }
            if (!cafTruthy(self.hPaused) && cafLooseEq(self.tx, 0)) {
                if (cafTruthy(self.wvAvailable)) self.help.onPause()
                self.hPaused = true
            }
            if (cafTruthy(self.cancelled)) return true
        }
        return false
    } catch (err: dynamic) {
        js("erp")(err)
        return true
    }
}

private fun cafAssistBuild(api: dynamic, self: dynamic) {
    self.htype = -1
    self.htext = ""
    self.keep = true
    self.hUpdate = false
    self.hPaused = false
    self.postHelp = cafVoid2Js { type: dynamic, text: dynamic ->
        cafAssistPostHelp(self, type, text)
    }
    self.hCheck = cafVoid0Js { cafAssistCheck(api, self) }
    self.hLoad = cafVoid0Js { cafAssistLoad(self) }
    self.initContent = cafVoid1Js { v: dynamic ->
        if (!cafTruthy(api.settings.splitScreenMode)) {
            v.setOnTouchListener(self.scroller)
        }
    }

    self.linear = cafNewLinearLayout()
    self.linear.setOrientation(js("G.LinearLayout.HORIZONTAL"))
    self.con = cafNewFrameLayout()
    self.linear.addView(self.con)
    // 第二个参数是历史遗留的 oldWebView，此时 self.help 尚未赋值（原实现即如此）。
    self.help = js("Common").createWebView(
        { wv: dynamic -> self.wvAvailable = true },
        self.help
    )
    self.linear.addView(self.help)
    val listener = cafNewLayoutChangeListener({
            v: dynamic,
            l: dynamic,
            t: dynamic,
            r: dynamic,
            b: dynamic,
            ol: dynamic,
            ot: dynamic,
            oldRight: dynamic,
            ob: dynamic ->
        cafAssistLayoutChange(api, self, l, t, r, b, ol, ot, oldRight, ob)
    })
    self.layoutListener = listener
    api.con.addOnLayoutChangeListener(listener)

    if (!cafTruthy(api.settings.splitScreenMode)) {
        val touchSlop = js("G.ViewConfiguration").get(js("ctx")).getScaledTouchSlop()
        val switchSlop = cafDp(80)
        self.scroller = cafNewTouchListener({ v: dynamic, e: dynamic ->
            cafAssistTouch(api, self, touchSlop, switchSlop, v, e)
        })
        self.help.setOnTouchListener(self.scroller)
    }

    js("PWM").registerResetFlag(api, "assist")
    js("PWM").registerResetFlag(self, "con")
    js("PWM").on("reset", {
        js("Common").destroyWebView(self.help)
        self.help = null
    })
}

private fun cafShowAssist(api: dynamic, self: dynamic) {
    js("G").ui({
        try {
            cafShowAssistImpl(api, self)
        } catch (e: dynamic) {
            js("erp")(e)
        }
    })
}

private fun cafShowAssistImpl(api: dynamic, self: dynamic) {
    if (cafTruthy(api.assist)) return
    if (!cafTruthy(self.con)) cafAssistBuild(api, self)
    self.lx = 0
    self.tx = 0
    self.linear.setTranslationX(0)
    api.assist = self.linear
    api.con.addView(api.assist)
    self.layoutListener.onLayoutChange(
        api.con, 0, 0,
        api.con.getMeasuredWidth(), api.con.getMeasuredHeight(),
        0, 0, 0, 0
    )
}

private fun cafHideAssist(api: dynamic) {
    js("G").ui({
        try {
            cafHideAssistImpl(api)
        } catch (e: dynamic) {
            js("erp")(e)
        }
    })
}

private fun cafHideAssistImpl(api: dynamic) {
    if (!cafTruthy(api.assist)) return
    api.showAssist.postHelp(-1)
    api.showAssist.hLoad()
    api.con.removeView(api.assist)
    api.Assist.hide()
    api.IntelliSense.hide()
    api.assist = null
}

// ---------------- showHistoryEdit ----------------

private fun cafSelectionSelectAll(self: dynamic) {
    var i = 0
    while (i < cafLength(self.selection)) {
        self.selection[i] = true
        i++
    }
    if (cafTruthy(self.adapter)) self.adapter.notifyChange()
    self.refreshBar()
}

private fun cafSelectionInvert(self: dynamic) {
    var i = 0
    while (i < cafLength(self.selection)) {
        self.selection[i] = !cafTruthy(self.selection[i])
        i++
    }
    if (cafTruthy(self.adapter)) self.adapter.notifyChange()
    self.refreshBar()
}

private fun cafSelectionClear(self: dynamic) {
    var i = 0
    while (i < cafLength(self.selection)) {
        self.selection[i] = false
        i++
    }
    if (cafTruthy(self.adapter)) self.adapter.notifyChange()
    self.refreshBar()
}

private fun cafPushAction(actions: dynamic, type: Int, text: String, action: dynamic) {
    val o = js("({})")
    o.type = type
    o.text = text
    o.action = action
    actions.push(o)
}

/**
 * 原实现靠 `o.callback()` 以 o 为 this 调用，再把 this.folder 当目标文件夹；
 * 这里用 cafThisJs 保留真实 this。
 */
private fun cafHistoryAddFavoriteCallback(api: dynamic, self: dynamic, thisObj: dynamic) {
    val z = thisObj.folder.children
    var c = 0
    var i = 0
    while (i < cafLength(self.selection)) {
        if (cafTruthy(self.selection[i])) {
            c++
            val entry = js("({})")
            entry.key = "历史记录(" + c + ")"
            entry.value = api.his[i]
            api.addFavorite(entry, z)
        }
        i++
    }
    js("Common").toast(c.toString() + "条命令已收藏")
}

private fun cafHistoryAddFavorite(api: dynamic, self: dynamic) {
    val opts = js("({})")
    opts.title = "选择收藏夹"
    opts.mode = 2
    opts.callback = cafThisJs { thisObj: dynamic ->
        cafHistoryAddFavoriteCallback(api, self, thisObj)
    }
    opts.onDismiss = {
        if (cafTruthy(api.history)) api.showHistory()
    }
    api.showFavEditDialog(opts)
}

private fun cafHistoryCopy(api: dynamic, self: dynamic) {
    val z = js("[]")
    var i = 0
    while (i < cafLength(self.selection)) {
        if (cafTruthy(self.selection[i])) z.push(api.his[i])
        i++
    }
    js("Common").setClipboardText(z.join("\n"))
    js("Common").toast(cafLength(z).toString() + "条命令已复制")
}

private fun cafHistoryImportUri(api: dynamic, self: dynamic, uri: dynamic) {
    try {
        val content = js("ExternalStorage").readFileContent(uri, "UTF-8", "[]")
        val r = cafJsonParse(content)
        if (!cafTruthy(cafIsArray(r))) cafThrow("不正确的收藏夹格式")
        r.forEach({ e0: dynamic ->
            val e = cafString(e0)
            if (cafLength(e) > 0) api.addHistory(e)
        })
        self.refresh()
        js("Common").toast("历史已成功导入")
    } catch (e: dynamic) {
        js("erp")(e, true)
        js("Common").toast("历史导入失败\n" + e)
    }
}

private fun cafHistoryImport(api: dynamic, self: dynamic) {
    val opts = js("({})")
    opts.mimeType = "application/json"
    opts.uri = { uri: dynamic -> cafHistoryImportUri(api, self, uri) }
    js("ExternalStorage").showImportActions(opts)
}

private fun cafHistoryExport(api: dynamic, self: dynamic) {
    val z = js("[]")
    var i = 0
    while (i < cafLength(self.selection)) {
        if (cafTruthy(self.selection[i])) z.push(api.his[i])
        i++
    }
    val opts = js("({})")
    opts.mimeType = "application/json"
    opts.hint = "历史.json"
    opts.export = { uri: dynamic ->
        js("ExternalStorage").writeFileContent(uri, cafJsonStringify(z))
    }
    js("ExternalStorage").showExportActions(opts)
}

private fun cafHistoryDelete(api: dynamic, self: dynamic) {
    var c = 0
    var i = cafLength(self.selection) - 1
    while (i >= 0) {
        if (cafTruthy(self.selection[i])) {
            api.his.splice(i, 1)
            c++
        }
        i--
    }
    js("Common").toast(c.toString() + "条命令已删除")
    self.refresh()
}

private fun cafHistoryEditActions(api: dynamic, self: dynamic): dynamic {
    val actions = js("[]")
    val selectAll: dynamic = { cafSelectionSelectAll(self) }
    val invert: dynamic = { cafSelectionInvert(self) }
    val clear: dynamic = { cafSelectionClear(self) }
    val addFavorite: dynamic = { cafHistoryAddFavorite(api, self) }
    val copy: dynamic = { cafHistoryCopy(api, self) }
    val importAction: dynamic = { cafHistoryImport(api, self) }
    val export: dynamic = { cafHistoryExport(api, self) }
    val delete: dynamic = { cafHistoryDelete(api, self) }
    cafPushAction(actions, 0, "全选", selectAll)
    cafPushAction(actions, 2, "反选", invert)
    cafPushAction(actions, 2, "清除选择", clear)
    cafPushAction(actions, 2, "添加收藏", addFavorite)
    cafPushAction(actions, 2, "复制", copy)
    cafPushAction(actions, 0, "导入", importAction)
    cafPushAction(actions, 2, "导出", export)
    cafPushAction(actions, 2, "删除", delete)
    return actions
}

private fun cafHistoryEditMaker(self: dynamic, holder: dynamic): dynamic {
    val layout = cafNewLinearLayout()
    val check = cafNewCheckBox()
    val text = cafNewTextView()
    holder.check = check
    holder.text = text
    layout.setGravity(js("G.Gravity.CENTER"))
    layout.setLayoutParams(cafNewAbsListViewParams(-1, -2))
    layout.setOrientation(js("G.LinearLayout.HORIZONTAL"))
    check.setOnCheckedChangeListener(cafNewCheckedChangeListener({ v: dynamic, s: dynamic ->
        try {
            self.selection[holder.pos] = s
            if (!cafTruthy(holder.busy)) self.refreshBar()
        } catch (e: dynamic) {
            js("erp")(e)
        }
    }))
    check.setFocusable(false)
    layout.addView(check)
    text.setPadding(cafDp(5), cafDp(15), cafDp(15), cafDp(15))
    text.setLayoutParams(cafNewWeightedLayoutParams(0, -2, 1.0))
    text.setMaxLines(2)
    text.setEllipsize(js("G.TextUtils.TruncateAt.END"))
    js("Common").applyStyle(text, "textview_default", 3)
    layout.addView(text)
    return layout
}

private fun cafHistoryEditBinder(self: dynamic, holder: dynamic, e: dynamic, i: dynamic) {
    holder.busy = true
    holder.check.setChecked(cafLooseEq(self.selection[i], true))
    holder.text.setText(e)
    holder.busy = false
}

private fun cafHistoryEditRefresh(api: dynamic, self: dynamic, pos: dynamic) {
    // 原实现用 new Array(n) 生成稀疏数组，未选中的槽位保持 undefined。
    self.selection = cafNewArray(cafLength(api.his))
    if (cafNotNull(pos)) self.selection[pos] = true
    if (cafLength(api.his) == 0) {
        self.adapter = null
        self.list.setAdapter(js("EmptyAdapter"))
    } else {
        if (cafTruthy(self.adapter)) {
            self.adapter.notifyChange()
        } else {
            val a = cafNewSimpleListAdapter(api.his, self.vmaker, self.vbinder)
            self.list.setAdapter(a)
            self.adapter = js("SimpleListAdapter").getController(a)
        }
    }
    self.refreshBar()
}

private fun cafHistoryEditRefreshBar(self: dynamic) {
    var c = 0
    var i = 0
    while (i < cafLength(self.selection)) {
        if (cafTruthy(self.selection[i])) c++
        i++
    }
    i = 0
    while (i < cafLength(self.actions)) {
        val e = self.actions[i]
        if (cafLooseEq(e.type, 0)) {
            // 总是显示
            e.view.setVisibility(js("G.View.VISIBLE"))
        } else if (cafLooseEq(e.type, 1)) {
            // 仅选中 1 个时显示
            e.view.setVisibility(if (c == 1) js("G.View.VISIBLE") else js("G.View.GONE"))
        } else if (cafLooseEq(e.type, 2)) {
            // 选中 1 个或多个时显示
            e.view.setVisibility(if (c > 0) js("G.View.VISIBLE") else js("G.View.GONE"))
        }
        i++
    }
    self.title.setText("编辑 历史 （" + c + "/" + cafLength(self.selection) + "）")
}

private fun cafHistoryEditBack(self: dynamic) {
    try {
        self.popup.exit()
    } catch (e: dynamic) {
        js("erp")(e)
    }
}

private fun cafAddActionView(self: dynamic, o: dynamic) {
    val b = cafNewTextView()
    b.setLayoutParams(cafNewLinearLayoutParams(-2, -1))
    b.setText(o.text)
    b.setGravity(js("G.Gravity.CENTER"))
    b.setPadding(cafDp(5), cafDp(5), cafDp(5), cafDp(5))
    js("Common").applyStyle(b, "button_highlight", 2)
    b.setOnClickListener(cafNewClickListener({ v: dynamic ->
        try {
            o.action()
        } catch (e: dynamic) {
            js("erp")(e)
        }
    }))
    o.view = b
    self.bar.addView(b)
}

private fun cafHistoryEditBuild(api: dynamic, self: dynamic) {
    self.adapter = null
    self.refresh = { pos: dynamic -> cafHistoryEditRefresh(api, self, pos) }
    self.refreshBar = { cafHistoryEditRefreshBar(self) }
    self.actions = cafHistoryEditActions(api, self)
    self.vmaker = { holder: dynamic -> cafHistoryEditMaker(self, holder) }
    self.vbinder = { holder: dynamic, e: dynamic, i: dynamic ->
        cafHistoryEditBinder(self, holder, e, i)
    }

    self.linear = cafNewLinearLayout()
    self.linear.setOrientation(js("G.LinearLayout.VERTICAL"))
    js("Common").applyStyle(self.linear, "container_default")

    self.header = cafNewLinearLayout()
    self.header.setOrientation(js("G.LinearLayout.HORIZONTAL"))
    self.header.setLayoutParams(cafNewLinearLayoutParams(-1, -2))
    js("Common").applyStyle(self.header, "bar_float")

    self.back = cafNewTextView()
    self.back.setText("< 返回")
    self.back.setGravity(js("G.Gravity.CENTER"))
    self.back.setPadding(cafDp(15), 0, cafDp(15), 0)
    self.back.setLayoutParams(cafNewLinearLayoutParams(-2, -1))
    js("Common").applyStyle(self.back, "button_highlight", 2)
    self.back.setOnClickListener(cafNewClickListener({ v: dynamic ->
        cafHistoryEditBack(self)
    }))
    self.header.addView(self.back)

    self.title = cafNewTextView()
    self.title.setPadding(0, cafDp(10), cafDp(15), cafDp(10))
    self.title.setLayoutParams(cafNewLinearLayoutParams(-2, -2))
    js("Common").applyStyle(self.title, "textview_default", 4)
    self.header.addView(self.title)

    self.hscr = cafNewHorizontalScrollView()
    self.hscr.setLayoutParams(cafNewLinearLayoutParams(-1, -1))
    self.hscr.setHorizontalScrollBarEnabled(false)
    self.hscr.setFillViewport(true)

    self.bar = cafNewLinearLayout()
    self.bar.setOrientation(js("G.LinearLayout.HORIZONTAL"))
    self.bar.setPadding(0, 0, cafDp(5), 0)
    self.bar.setLayoutParams(cafNewFrameLayoutParams(-1, -1))
    self.bar.setGravity(js("G.Gravity.RIGHT"))
    self.actions.forEach({ o: dynamic -> cafAddActionView(self, o) })
    self.hscr.addView(self.bar)
    self.header.addView(self.hscr)
    self.linear.addView(self.header)

    self.list = cafNewListView()
    self.list.setOnItemClickListener(cafNewItemClickListener({
            parent: dynamic,
            view: dynamic,
            pos: dynamic,
            id: dynamic ->
        try {
            if (cafTruthy(self.adapter)) self.adapter.getHolder(view).check.performClick()
        } catch (e: dynamic) {
            js("erp")(e)
        }
    }))
    self.linear.addView(self.list)
    if (cafLooseEq(js("G").style, "Material")) {
        self.list.setFastScrollEnabled(true)
        self.list.setFastScrollAlwaysVisible(false)
    }

    self.popup = cafNewPopupPage(self.linear, "ca.HistoryEdit")
    self.popup.on("exit", {
        if (cafTruthy(self.callback)) self.callback()
    })

    js("PWM").registerResetFlag(self, "linear")
}

private fun cafShowHistoryEdit(api: dynamic, self: dynamic, pos: dynamic, callback: dynamic) {
    js("G").ui({
        try {
            if (!cafTruthy(self.linear)) cafHistoryEditBuild(api, self)
            self.refresh(pos)
            self.callback = callback
            self.popup.enter()
        } catch (e: dynamic) {
            js("erp")(e)
        }
    })
}

// ---------------- showFavoriteEdit ----------------

private fun cafFavoriteTrace(self: dynamic, data: dynamic, root: dynamic): dynamic {
    val a = root.children
    var i = 0
    while (i < cafLength(a)) {
        if (cafLooseEq(data, a[i])) return cafArray1(root)
        if (cafTruthy(a[i].children)) {
            val r = self.trace(data, a[i])
            if (cafTruthy(r)) {
                r.unshift(root)
                return r
            }
        }
        i++
    }
    return js("undefined")
}

private fun cafFavoriteInit(api: dynamic, self: dynamic, data: dynamic) {
    val t = js("({})")
    t.key = "根"
    t.children = api.fav
    // 对应 `self.trace(data, t) || [t]`：左侧为真时不再构造 [t]。
    val traced = self.trace(data, t)
    self.path = if (cafTruthy(traced)) traced else cafArray1(t)
    self.refresh(data)
}

private fun cafFavoriteRefresh(api: dynamic, self: dynamic, data: dynamic) {
    val t = self.path[cafLength(self.path) - 1]
    self.array = t.children.slice()
    self.selection = cafNewArray(cafLength(self.array))
    if (cafNotNull(data)) self.selection[self.array.indexOf(data)] = true
    if (cafLength(self.array) == 0) {
        self.adapter = null
        self.list.setAdapter(js("EmptyAdapter"))
    } else {
        if (cafTruthy(self.adapter)) {
            self.adapter.setSync(self.array)
        } else {
            val a = cafNewSimpleListAdapter(self.array, self.vmaker, self.vbinder)
            self.list.setAdapter(a)
            self.adapter = js("SimpleListAdapter").getController(a)
        }
    }
    self.pathbar.setVisibility(if (cafLength(self.path) > 1) js("G.View.VISIBLE") else js("G.View.GONE"))
    self.pathbar.setText("返回上层 " + self.path.map({ e: dynamic -> e.key }).join(" > "))
    self.refreshBar()
}

private fun cafFavoriteRefreshBar(self: dynamic) {
    var c = 0
    var i = 0
    while (i < cafLength(self.selection)) {
        if (cafTruthy(self.selection[i])) c++
        i++
    }
    i = 0
    while (i < cafLength(self.actions)) {
        val e = self.actions[i]
        if (cafLooseEq(e.type, 0)) {
            // 总是显示
            e.view.setVisibility(js("G.View.VISIBLE"))
        } else if (cafLooseEq(e.type, 1)) {
            // 仅选中 1 个时显示
            e.view.setVisibility(if (c == 1) js("G.View.VISIBLE") else js("G.View.GONE"))
        } else if (cafLooseEq(e.type, 2)) {
            // 选中 1 个或多个时显示
            e.view.setVisibility(if (c > 0) js("G.View.VISIBLE") else js("G.View.GONE"))
        }
        i++
    }
    self.title.setText("编辑 收藏 （" + c + "/" + cafLength(self.selection) + "）")
}

private fun cafFavoriteEditFav(api: dynamic, self: dynamic, pos: dynamic) {
    val opts = js("({})")
    opts.mode = 1
    opts.data = self.array[pos]
    opts.folder = self.path[cafLength(self.path) - 1]
    opts.callback = { self.refresh() }
    api.showFavEditDialog(opts)
}

private fun cafFavoriteMoveCallback(api: dynamic, self: dynamic, thisObj: dynamic, fd: dynamic, a: dynamic) {
    var i = 0
    while (i < cafLength(a)) {
        api.removeFavorite(a[i], fd.children)
        api.addFavorite(a[i], thisObj.folder.children)
        i++
    }
    self.refresh()
}

private fun cafFavoriteMove(api: dynamic, self: dynamic) {
    val fd = self.path[cafLength(self.path) - 1]
    val a = js("[]")
    var i = 0
    while (i < cafLength(self.selection)) {
        if (cafTruthy(self.selection[i])) a.push(self.array[i])
        i++
    }
    val opts = js("({})")
    opts.mode = 2
    opts.folder = fd
    opts.hiddenFolder = a
    opts.callback = cafThisJs { thisObj: dynamic ->
        cafFavoriteMoveCallback(api, self, thisObj, fd, a)
    }
    api.showFavEditDialog(opts)
}

private fun cafFavoriteImportUri(api: dynamic, self: dynamic, fd: dynamic, uri: dynamic) {
    try {
        val content = js("ExternalStorage").readFileContent(uri, "UTF-8", "[]")
        val r = cafJsonParse(content)
        if (!cafTruthy(cafIsArray(r))) cafThrow("不正确的收藏夹格式")
        r.forEach({ e: dynamic -> api.addFavorite(e, fd.children) })
        self.refresh()
        js("Common").toast("收藏已成功导入")
    } catch (e: dynamic) {
        js("erp")(e, true)
        js("Common").toast("收藏夹导入失败\n" + e)
    }
}

private fun cafFavoriteImport(api: dynamic, self: dynamic) {
    val fd = self.path[cafLength(self.path) - 1]
    val opts = js("({})")
    opts.mimeType = "application/json"
    opts.uri = { uri: dynamic -> cafFavoriteImportUri(api, self, fd, uri) }
    js("ExternalStorage").showImportActions(opts)
}

private fun cafFavoriteExport(api: dynamic, self: dynamic) {
    // 原实现同样声明了 fd 但未使用，保留以对照行为。
    @Suppress("UNUSED_VARIABLE")
    val fd = self.path[cafLength(self.path) - 1]
    val a = js("[]")
    var i = 0
    while (i < cafLength(self.selection)) {
        if (cafTruthy(self.selection[i])) a.push(self.array[i])
        i++
    }
    val opts = js("({})")
    opts.mimeType = "application/json"
    opts.hint = "收藏.json"
    opts.export = { uri: dynamic ->
        js("ExternalStorage").writeFileContent(uri, cafJsonStringify(a))
    }
    js("ExternalStorage").showExportActions(opts)
}

private fun cafFavoriteDelete(api: dynamic, self: dynamic) {
    var c = 0
    // 原实现的 bug 原样保留：从 selection.length 开始（多一轮且永远跳过），
    // 且 removeFavorite 不传 folder，所以只会从根收藏夹里删除。
    var i = cafLength(self.selection)
    while (i >= 0) {
        if (cafTruthy(self.selection[i])) {
            api.removeFavorite(self.array[i])
            c++
        }
        i--
    }
    js("Common").toast(c.toString() + "条命令已删除")
    self.refresh()
}

private fun cafFavoriteEditActions(api: dynamic, self: dynamic): dynamic {
    val actions = js("[]")
    val selectAll: dynamic = { cafSelectionSelectAll(self) }
    val invert: dynamic = { cafSelectionInvert(self) }
    val clear: dynamic = { cafSelectionClear(self) }
    val move: dynamic = { cafFavoriteMove(api, self) }
    val importAction: dynamic = { cafFavoriteImport(api, self) }
    val export: dynamic = { cafFavoriteExport(api, self) }
    val delete: dynamic = { cafFavoriteDelete(api, self) }
    cafPushAction(actions, 0, "全选", selectAll)
    cafPushAction(actions, 2, "反选", invert)
    cafPushAction(actions, 2, "清除选择", clear)
    cafPushAction(actions, 2, "移动", move)
    cafPushAction(actions, 0, "导入", importAction)
    cafPushAction(actions, 2, "导出", export)
    cafPushAction(actions, 2, "删除", delete)
    return actions
}

private fun cafFavoriteMaker(self: dynamic, holder: dynamic): dynamic {
    val layout = cafNewLinearLayout()
    val check = cafNewCheckBox()
    val linear = cafNewLinearLayout()
    val text1 = cafNewTextView()
    val text2 = cafNewTextView()
    val edit = cafNewImageView()
    holder.check = check
    holder.text1 = text1
    holder.text2 = text2
    holder.edit = edit
    layout.setGravity(js("G.Gravity.CENTER"))
    layout.setLayoutParams(cafNewAbsListViewParams(-1, -2))
    layout.setOrientation(js("G.LinearLayout.HORIZONTAL"))
    check.setOnCheckedChangeListener(cafNewCheckedChangeListener({ v: dynamic, s: dynamic ->
        try {
            self.selection[holder.pos] = s
            if (!cafTruthy(holder.busy)) self.refreshBar()
        } catch (e: dynamic) {
            js("erp")(e)
        }
    }))
    check.setFocusable(false)
    layout.addView(check)
    edit.setImageResource(js("G.R.drawable.ic_menu_edit"))
    edit.setScaleType(js("G.ImageView.ScaleType.FIT_CENTER"))
    edit.setLayoutParams(cafNewLinearLayoutParams(cafDp(24), -1))
    edit.getLayoutParams().setMargins(cafDp(5), 0, cafDp(5), 0)
    edit.setOnClickListener(cafNewClickListener({ v: dynamic ->
        try {
            self.editFav(holder.pos)
        } catch (e: dynamic) {
            js("erp")(e)
        }
    }))
    layout.addView(edit)
    linear.setLayoutParams(cafNewWeightedLayoutParams(0, -2, 1))
    linear.setOrientation(js("G.LinearLayout.VERTICAL"))
    text1.setPadding(cafDp(10), cafDp(15), cafDp(15), cafDp(5))
    text1.setLayoutParams(cafNewLinearLayoutParams(-1, -2))
    js("Common").applyStyle(text1, "textview_default", 3)
    linear.addView(text1)
    text2.setPadding(cafDp(10), 0, cafDp(15), cafDp(15))
    text2.setLayoutParams(cafNewLinearLayoutParams(-1, -2))
    text2.setEllipsize(js("G.TextUtils.TruncateAt.END"))
    text2.setSingleLine(true)
    js("Common").applyStyle(text2, "textview_prompt", 1)
    linear.addView(text2)
    layout.addView(linear)
    return layout
}

private fun cafFavoriteBinder(self: dynamic, holder: dynamic, e: dynamic, i: dynamic) {
    holder.busy = true
    holder.check.setChecked(cafLooseEq(self.selection[i], true))
    holder.text1.setText(cafString(e.key))
    holder.text2.setText(
        if (cafTruthy(e.children)) {
            "文件夹，包含" + cafLength(e.children) + "个成员"
        } else {
            e.value
        }
    )
    holder.edit.setVisibility(if (cafTruthy(e.children)) js("G.View.GONE") else js("G.View.VISIBLE"))
    holder.busy = false
}

private fun cafFavoritePathbarClick(self: dynamic) {
    try {
        if (cafLength(self.path) < 2) return
        cafSetLength(self.path, cafLength(self.path) - 1)
        self.refresh()
    } catch (e: dynamic) {
        js("erp")(e)
    }
}

private fun cafFavoriteItemClick(self: dynamic, view: dynamic, pos: dynamic) {
    try {
        if (cafTruthy(self.array[pos].children)) {
            self.path.push(self.array[pos])
            self.refresh()
        } else {
            if (cafTruthy(self.adapter)) self.adapter.getHolder(view).check.performClick()
        }
    } catch (e: dynamic) {
        js("erp")(e)
    }
}

private fun cafFavoriteEditBuild(api: dynamic, self: dynamic) {
    self.adapter = null
    self.trace = { data: dynamic, root: dynamic -> cafFavoriteTrace(self, data, root) }
    self.init = { data: dynamic -> cafFavoriteInit(api, self, data) }
    self.refresh = { data: dynamic -> cafFavoriteRefresh(api, self, data) }
    self.refreshBar = { cafFavoriteRefreshBar(self) }
    self.editFav = { pos: dynamic -> cafFavoriteEditFav(api, self, pos) }
    self.actions = cafFavoriteEditActions(api, self)
    self.vmaker = { holder: dynamic -> cafFavoriteMaker(self, holder) }
    self.vbinder = { holder: dynamic, e: dynamic, i: dynamic ->
        cafFavoriteBinder(self, holder, e, i)
    }

    self.linear = cafNewLinearLayout()
    self.linear.setOrientation(js("G.LinearLayout.VERTICAL"))
    js("Common").applyStyle(self.linear, "container_default")

    self.header = cafNewLinearLayout()
    self.header.setOrientation(js("G.LinearLayout.HORIZONTAL"))
    self.header.setLayoutParams(cafNewLinearLayoutParams(-1, -2))
    js("Common").applyStyle(self.header, "bar_float")

    self.back = cafNewTextView()
    self.back.setText("< 返回")
    self.back.setGravity(js("G.Gravity.CENTER"))
    self.back.setPadding(cafDp(15), 0, cafDp(15), 0)
    self.back.setLayoutParams(cafNewLinearLayoutParams(-2, -1))
    js("Common").applyStyle(self.back, "button_highlight", 2)
    self.back.setOnClickListener(cafNewClickListener({ v: dynamic ->
        try {
            self.popup.exit()
        } catch (e: dynamic) {
            js("erp")(e)
        }
    }))
    self.header.addView(self.back)

    self.title = cafNewTextView()
    self.title.setPadding(0, cafDp(10), cafDp(15), cafDp(10))
    self.title.setLayoutParams(cafNewLinearLayoutParams(-2, -2))
    js("Common").applyStyle(self.title, "textview_default", 4)
    self.header.addView(self.title)

    self.hscr = cafNewHorizontalScrollView()
    self.hscr.setLayoutParams(cafNewLinearLayoutParams(-1, -1))
    self.hscr.setHorizontalScrollBarEnabled(false)
    self.hscr.setFillViewport(true)

    self.bar = cafNewLinearLayout()
    self.bar.setOrientation(js("G.LinearLayout.HORIZONTAL"))
    self.bar.setPadding(0, 0, cafDp(5), 0)
    self.bar.setLayoutParams(cafNewFrameLayoutParams(-1, -1))
    self.bar.setGravity(js("G.Gravity.RIGHT"))
    self.actions.forEach({ o: dynamic -> cafAddActionView(self, o) })
    self.hscr.addView(self.bar)
    self.header.addView(self.hscr)
    self.linear.addView(self.header)

    self.pathbar = cafNewTextView()
    self.pathbar.setLayoutParams(cafNewLinearLayoutParams(-1, -2))
    self.pathbar.setPadding(cafDp(10), cafDp(10), cafDp(10), cafDp(10))
    js("Common").applyStyle(self.pathbar, "bar_float_second")
    js("Common").applyStyle(self.pathbar, "text_prompt", 2)
    self.pathbar.setOnClickListener(cafNewClickListener({ v: dynamic ->
        cafFavoritePathbarClick(self)
    }))
    self.linear.addView(self.pathbar)

    self.list = cafNewListView()
    self.list.setOnItemClickListener(cafNewItemClickListener({
            parent: dynamic,
            view: dynamic,
            pos: dynamic,
            id: dynamic ->
        cafFavoriteItemClick(self, view, pos)
    }))
    self.linear.addView(self.list)
    if (cafLooseEq(js("G").style, "Material")) {
        self.list.setFastScrollEnabled(true)
        self.list.setFastScrollAlwaysVisible(false)
    }

    self.popup = cafNewPopupPage(self.linear, "ca.FavoriteEdit")
    self.popup.on("exit", {
        if (cafTruthy(self.callback)) self.callback()
    })

    js("PWM").registerResetFlag(self, "linear")
}

private fun cafShowFavoriteEdit(api: dynamic, self: dynamic, data: dynamic, callback: dynamic) {
    js("G").ui({
        try {
            if (!cafTruthy(self.linear)) cafFavoriteEditBuild(api, self)
            self.init(data)
            self.callback = callback
            self.popup.enter()
        } catch (e: dynamic) {
            js("erp")(e)
        }
    })
}

// ---------------- performExit ----------------

private fun cafPerformExit() {
    js("G").ui({
        try {
            js("unload")()
            if (cafLooseEq(MapScript.host, "AutoJs")) {
                js("ctx").finish()
            } else if (cafLooseEq(MapScript.host, "Android")) {
                js("ScriptInterface").quit()
            }
        } catch (e: dynamic) {
            js("erp")(e)
        }
    })
}

// ---------------- 安装 ----------------

fun installCaFavorite(api: dynamic) {
    api.showFavEditDialog = cafSelf1Js { self: dynamic, o: dynamic ->
        cafShowFavEditDialog(api, self, o)
    }
    api.showAssist = cafSelf0Js { self: dynamic ->
        cafShowAssist(api, self)
    }
    api.hideAssist = cafVoid0Js {
        cafHideAssist(api)
    }
    api.showHistoryEdit = cafSelf2Js { self: dynamic, pos: dynamic, callback: dynamic ->
        cafShowHistoryEdit(api, self, pos, callback)
    }
    api.showFavoriteEdit = cafSelf2Js { self: dynamic, data: dynamic, callback: dynamic ->
        cafShowFavoriteEdit(api, self, data, callback)
    }
    api.performExit = cafVoid0Js {
        cafPerformExit()
    }
}
