package ca.kotlin.ui

import ca.kotlin.core.*


/**
 * modules/uiCore/PopupPage.js 的 Kotlin/JS 版本。
 *
 * 模块本身是构造函数：new PopupPage(mainView, name, modal)，
 * 同时构造函数上挂着页面管理器的静态状态、事件、对话框/侧栏与动画控制器。
 *
 * Android 分支的默认窗口、顶部拖拽条、缩放角、悬浮窗、页面栈、覆盖层
 * 与动画都保持原实现的结构和调用顺序；原型方法与管理器静态方法都依赖
 * 动态 this，因此用一层薄 JS wrapper 把真实 this / arguments 传回 Kotlin。
 *
 * 监听器里需要提前 return 的位置都写成普通 Kotlin 函数，避免 dynamic
 * lambda 的 return@label。数组 length 统一走 `| 0` helper。
 */
private var ppApi: dynamic = js("null")
private var ppNextId = 0

// ---------------- JS 语义 helper ----------------
private val ppTruthyJs: dynamic = js("function (v) { return !!v; }")
private val ppLengthJs: dynamic = js("function (v) { return v.length | 0; }")
private fun ppLength(v: dynamic): Int = ppLengthJs(v)
private val ppSetLengthJs: dynamic = js("function (a, n) { a.length = n; }")
private val ppLooseEqJs: dynamic = js("function (a, b) { return a == b; }")
private fun ppLooseEq(a: dynamic, b: dynamic): Boolean = ppLooseEqJs(a, b)
private val ppLooseNeJs: dynamic = js("function (a, b) { return a != b; }")
private val ppAbsJs: dynamic = js("function (v) { return Math.abs(v); }")
private val ppMaxJs: dynamic = js("function (a, b) { return Math.max(a, b); }")
private val ppIsNaNJ: dynamic = js("function (v) { return isNaN(v); }")
private val ppBooleanJs: dynamic = js("function (v) { return Boolean(v); }")
private val ppDpJs: dynamic = js("function (n) { return n * G.dp; }")
private fun ppDp(n: Int): dynamic = ppDpJs(n)
private val ppSdkJs: dynamic = js("function () { return android.os.Build.VERSION.SDK_INT; }")
private val ppThisJs: dynamic = js("function (f) { return function () { return f(this, arguments); }; }")
private val ppMakeCtorJs: dynamic = js(
    "function (init) { return function (mainView, name, modal) { init(this, mainView, name, modal); }; }"
)
private val ppNewJs: dynamic = js("function (f, a, b, c) { return new f(a, b, c); }")
private val ppMakeTouchJs: dynamic = js(
    "function (f) { function touch(v, e) { return f(touch, v, e); } return touch; }"
)
private val ppNewErrorJs: dynamic = js("function (m) { return new Error(m); }")
private val ppMakeOverlayCtorJs: dynamic = js(
    "function (init) {" +
        " return function (view, width, height, gravity, x, y) {" +
        "  init(this, view, width, height, gravity, x, y);" +
        " };" +
        "}"
)
private val ppMakeControllerCtorJs: dynamic = js(
    "function (init) { return function (v, ani) { init(this, v, ani); }; }"
)
private val ppNewControllerJs: dynamic = js("function (f, a, b) { return new f(a, b); }")
private val ppArray2Js: dynamic = js("function (a, b) { return [a, b]; }")
private val ppParamLayoutJs: dynamic = js("function (layout) { return { layout: layout }; }")
private val ppSideBarParamJs: dynamic = js(
    "function (background, layout, direction) {" +
        " return { background: background, layout: layout, direction: direction };" +
        "}"
)
private val ppOverlayParamJs: dynamic = js(
    "function (x, y, width, height, gravity) {" +
        " return { x: x, y: y, width: width, height: height, gravity: gravity," +
        " focusable: false, touchable: false };" +
        "}"
)

// ---------------- Android UI helper ----------------
private val ppCreateFrameLayoutJs: dynamic = js(
    "function (cb) { return ScriptInterface.createFrameLayout(cb); }"
)
private val ppNewFrameLayoutJs: dynamic = js("function () { return new G.FrameLayout(ctx); }")
private val ppNewLinearLayoutJs: dynamic = js("function () { return new G.LinearLayout(ctx); }")
private val ppNewTextViewJs: dynamic = js("function () { return new G.TextView(ctx); }")
private val ppNewImageViewJs: dynamic = js("function () { return new G.ImageView(ctx); }")
private val ppNewFrameLayoutParamsJs: dynamic = js(
    "function (w, h) { return new G.FrameLayout.LayoutParams(w, h); }"
)
private val ppNewFrameLayoutParamsGravityJs: dynamic = js(
    "function (w, h, g) { return new G.FrameLayout.LayoutParams(w, h, g); }"
)
private val ppNewLinearLayoutParamsJs: dynamic = js(
    "function (w, h) { return new G.LinearLayout.LayoutParams(w, h); }"
)
private val ppNewWeightedLayoutParamsJs: dynamic = js(
    "function (w, h, weight) { return new G.LinearLayout.LayoutParams(w, h, weight); }"
)
private val ppNewWindowManagerParamsJs: dynamic = js(
    "function () { return new G.WindowManager.LayoutParams(); }"
)
private val ppNewTouchListenerJs: dynamic = js(
    "function (f) { return new G.View.OnTouchListener({ onTouch: f }); }"
)
private val ppNewLayoutChangeListenerJs: dynamic = js(
    "function (f) { return new G.View.OnLayoutChangeListener({ onLayoutChange: f }); }"
)
private val ppNewAnimationListenerJs: dynamic = js(
    "function (f) { return new G.Animation.AnimationListener({ onAnimationEnd: f }); }"
)
private val ppNewDismissListenerJs: dynamic = js(
    "function (f) { return new G.PopupWindow.OnDismissListener({ onDismiss: f }); }"
)
private val ppNewRunnableJs: dynamic = js(
    "function (f) { return new java.lang.Runnable({ run: f }); }"
)
private val ppNewColorDrawableJs: dynamic = js("function (c) { return new G.ColorDrawable(c); }")
private val ppNewPopupWindowFullJs: dynamic = js(
    "function (v, w, h) { return new G.PopupWindow(v, w, h); }"
)
private val ppNewPopupWindowGlobalJs: dynamic = js(
    "function (v, w, h) { return new PopupWindow(v, w, h); }"
)
private val ppNewAlphaAnimationJs: dynamic = js(
    "function (a, b) { return new G.AlphaAnimation(a, b); }"
)
private val ppNewAnimationSetJs: dynamic = js(
    "function (share) { return new G.AnimationSet(share); }"
)
private val ppNewScaleAnimationJs: dynamic = js(
    "function (fromX, toX, fromY, toY, pivotXType, pivotXValue, pivotYType, pivotYValue) {" +
        " return new G.ScaleAnimation(fromX, toX, fromY, toY, pivotXType, pivotXValue, pivotYType, pivotYValue);" +
        "}"
)
private val ppNewTranslateAnimationJs: dynamic = js(
    "function (fromXType, fromXValue, toXType, toXValue, fromYType, fromYValue, toYType, toYValue) {" +
        " return new G.TranslateAnimation(fromXType, fromXValue, toXType, toXValue, fromYType, fromYValue, toYType, toYValue);" +
        "}"
)
private val ppIsActivityJs: dynamic = js(
    "function (c) { return c instanceof android.app.Activity; }"
)
private val ppGravityLeftTopJs: dynamic = js(
    "function () { return G.Gravity.LEFT | G.Gravity.TOP; }"
)

// ---------------- 构造函数 ----------------

private fun ppInit(self: dynamic, mainView: dynamic, name: dynamic, modal: dynamic) {
    val currentId = ppNextId
    self.mainView = mainView
    self.name = if (ppTruthyJs(name)) name else "Unnamed@" + currentId
    self.id = currentId
    ppNextId = currentId + 1
    self.modal = modal
    self._enterAnimation = ppApi.fadeInAnimation
    self._exitAnimation = ppApi.fadeOutAnimation
    self.listener = js("({})")
    self.init()
}

// ---------------- Android：初始化窗口与 UI ----------------

private fun ppBackDispatchKey(event: dynamic, thisObj: dynamic): dynamic {
    val state = thisObj.getKeyDispatcherState()
    if (ppLooseEqJs(event.getKeyCode(), event.KEYCODE_BACK)) {
        if (!ppTruthyJs(state)) return 0
        if (ppLooseEqJs(event.getAction(), event.ACTION_DOWN) && ppLooseEqJs(event.getRepeatCount(), 0)) {
            state.startTracking(event, thisObj)
            return 1
        } else if (ppLooseEqJs(event.getAction(), event.ACTION_UP)) {
            if (ppTruthyJs(state.isTracking(event)) && !ppTruthyJs(event.isCanceled())) {
                ppApi.back(ppApi.defaultContainer)
                return 1
            }
        }
    }
    return 0
}

private fun ppDefaultDispatchTouch(event: dynamic, thisObj: dynamic): dynamic {
    val action = event.getAction()
    if (ppLooseEqJs(action, event.ACTION_DOWN)) {
        ppApi.setFocused(true)
    } else if (ppLooseEqJs(action, event.ACTION_OUTSIDE)) {
        ppApi.setFocused(false)
    }
    return 0
}

private fun ppFloatDispatchTouch(event: dynamic, thisObj: dynamic): dynamic = 0

private fun ppDefaultLayoutChange(
    view: dynamic,
    left: dynamic,
    top: dynamic,
    right: dynamic,
    bottom: dynamic,
    oldLeft: dynamic,
    oldTop: dynamic,
    oldRight: dynamic,
    oldBottom: dynamic
): dynamic {
    try {
        val w = right - left
        val h = bottom - top
        val ow = oldRight - oldLeft
        val oh = oldBottom - oldTop
        if (ppLooseEqJs(w, ow) && ppLooseEqJs(h, oh)) return js("undefined")
        var i = ppLength(ppApi.defaultStack) - 1
        while (i >= 0) {
            val e = ppApi.defaultStack[i]
            e.page.trigger("resize", w, h)
            i--
        }
    } catch (e: dynamic) {
        js("erp")(e)
    }
    return js("undefined")
}

private fun ppFloatLayoutChange(
    view: dynamic,
    left: dynamic,
    top: dynamic,
    right: dynamic,
    bottom: dynamic,
    oldLeft: dynamic,
    oldTop: dynamic,
    oldRight: dynamic,
    oldBottom: dynamic
): dynamic {
    try {
        val w = right - left
        val h = bottom - top
        val ow = oldRight - oldLeft
        val oh = oldBottom - oldTop
        if (ppLooseEqJs(w, ow) && ppLooseEqJs(h, oh)) return js("undefined")
        var i = ppLength(ppApi.floatStack) - 1
        while (i >= 0) {
            val e = ppApi.floatStack[i]
            e.page.trigger("resize", w, h)
            i--
        }
    } catch (e: dynamic) {
        js("erp")(e)
    }
    return js("undefined")
}

private fun ppTitleTouch(touch: dynamic, v: dynamic, e: dynamic, touchSlop: dynamic): dynamic {
    try {
        val action = e.getAction()
        if (ppLooseEqJs(action, e.ACTION_MOVE)) {
            if (ppTruthyJs(touch.stead)) {
                if (ppAbsJs(touch.lx - e.getRawX()) + ppAbsJs(touch.ly - e.getRawY()) < touchSlop) {
                    return true
                }
                touch.stead = false
            }
            ppApi.setRect(e.getRawX() + touch.offx, e.getRawY() + touch.offy, -1, -1, true)
        } else if (ppLooseEqJs(action, e.ACTION_DOWN)) {
            touch.lx = e.getRawX()
            touch.offx = ppApi.x - touch.lx
            touch.ly = e.getRawY()
            touch.offy = ppApi.y - touch.ly
            touch.stead = true
            js("Common").applyStyle(v, "button_reactive_pressed", 2)
        } else if (ppLooseEqJs(action, e.ACTION_UP) || ppLooseEqJs(action, e.ACTION_CANCEL)) {
            js("Common").applyStyle(v, "button_reactive", 2)
        }
        return true
    } catch (err: dynamic) {
        js("erp")(err)
        return false
    }
}

private fun ppResizeTouch(touch: dynamic, v: dynamic, e: dynamic, touchSlop: dynamic, longPressTimeout: dynamic): dynamic {
    try {
        val action = e.getAction()
        if (ppLooseEqJs(action, e.ACTION_MOVE)) {
            if (ppTruthyJs(ppApi.locked)) return true
            if (ppTruthyJs(touch.stead)) {
                if (ppAbsJs(touch.lx - e.getRawX()) + ppAbsJs(touch.ly - e.getRawY()) < touchSlop) {
                    return true
                }
                ppApi.longClicked = false
                touch.stead = false
                ppApi.defaultStub.setVisibility(js("G.View.VISIBLE"))
                ppApi.defaultContainer.setVisibility(js("G.View.GONE"))
            }
            return true
        } else if (ppLooseEqJs(action, e.ACTION_DOWN)) {
            touch.lx = e.getRawX()
            touch.offwidth = ppApi.width - touch.lx
            touch.ly = e.getRawY()
            touch.offheight = ppApi.height + touch.ly
            touch.offy = ppApi.y - touch.ly
            touch.stead = true
            v.postDelayed(ppApi.longClick, longPressTimeout)
            ppApi.longClicked = true
            js("Common").applyStyle(v, "button_reactive_pressed", 2)
            return true
        } else if (ppLooseEqJs(action, e.ACTION_UP) || ppLooseEqJs(action, e.ACTION_CANCEL)) {
            ppApi.longClicked = false
            js("Common").applyStyle(v, "button_reactive", 2)
            if (ppTruthyJs(ppApi.locked)) return true
            if (!ppTruthyJs(touch.stead)) {
                ppApi.defaultStub.setVisibility(js("G.View.GONE"))
                ppApi.defaultContainer.setVisibility(js("G.View.VISIBLE"))
                ppApi.setRect(
                    -1,
                    e.getRawY() + touch.offy,
                    e.getRawX() + touch.offwidth,
                    touch.offheight - e.getRawY(),
                    true
                )
            }
            return true
        }
        return true
    } catch (err: dynamic) {
        js("erp")(err)
        return false
    }
}

private fun ppBuildDefaultUi(): dynamic {
    val vcfg = js("G.ViewConfiguration").get(js("ctx"))
    val longPressTimeout = vcfg.getLongPressTimeout()
    val touchSlop = vcfg.getScaledTouchSlop()

    ppApi.baseTouchListener = ppNewTouchListenerJs({ v: dynamic, e: dynamic ->
        try {
            if (ppLooseEqJs(e.getAction(), e.ACTION_DOWN)) ppApi.onPageMissing(v)
            false
        } catch (err: dynamic) {
            js("erp")(err)
            false
        }
    })

    val defaultWindowCallbacks = js("({})")
    defaultWindowCallbacks.dispatchKeyEvent = { event: dynamic, thisObj: dynamic ->
        ppBackDispatchKey(event, thisObj)
    }
    defaultWindowCallbacks.dispatchTouchEvent = { e: dynamic, thisObj: dynamic ->
        ppDefaultDispatchTouch(e, thisObj)
    }
    ppApi.defaultWindow = ppCreateFrameLayoutJs(defaultWindowCallbacks)
    ppApi.defaultWindow.setRoundRectRadius(ppDp(8), 2)
    ppApi.defaultWindow.setContentDescription("DefaultWindow")

    ppApi.longClick = ppNewRunnableJs({
        try {
            if (ppTruthyJs(ppApi.longClicked)) ppApi.setFullScreen(false, true)
            ppApi.longClicked = false
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })

    ppApi.defaultDecorLinear = ppNewLinearLayoutJs()
    ppApi.defaultDecorLinear.setOrientation(js("G.LinearLayout.VERTICAL"))
    ppApi.defaultDecorLinear.setLayoutParams(ppNewFrameLayoutParamsJs(-1, -1))

    ppApi.headerView = ppNewLinearLayoutJs()
    ppApi.headerView.setOrientation(js("G.LinearLayout.HORIZONTAL"))
    ppApi.headerView.setLayoutParams(ppNewLinearLayoutParamsJs(-1, -2))

    ppApi.titleView = ppNewTextViewJs()
    ppApi.titleView.setText("CA")
    ppApi.titleView.setPadding(ppDp(5), ppDp(5), ppDp(5), ppDp(5))
    ppApi.titleView.setSingleLine(true)
    ppApi.titleView.setLayoutParams(ppNewWeightedLayoutParamsJs(-2, -2, 1))
    ppApi.titleView.setOnTouchListener(ppNewTouchListenerJs(ppMakeTouchJs(
        { touch: dynamic, v: dynamic, e: dynamic -> ppTitleTouch(touch, v, e, touchSlop) }
    )))
    ppApi.headerView.addView(ppApi.titleView)

    ppApi.resizeView = ppNewTextViewJs()
    ppApi.resizeView.setText("■")
    ppApi.resizeView.setPadding(ppDp(5), ppDp(5), ppDp(5), ppDp(5))
    ppApi.resizeView.setSingleLine(true)
    ppApi.resizeView.setGravity(js("G.Gravity.RIGHT"))
    ppApi.resizeView.setLayoutParams(ppNewLinearLayoutParamsJs(-2, -2))
    ppApi.resizeView.setOnTouchListener(ppNewTouchListenerJs(ppMakeTouchJs(
        { touch: dynamic, v: dynamic, e: dynamic ->
            ppResizeTouch(touch, v, e, touchSlop, longPressTimeout)
        }
    )))
    ppApi.headerView.addView(ppApi.resizeView)
    ppApi.headerView.measure(0, 0)
    ppApi.minWidth = ppApi.headerView.getMeasuredWidth()
    ppApi.minHeight = ppApi.headerView.getMeasuredHeight()
    ppApi.defaultDecorLinear.addView(ppApi.headerView)

    ppApi.defaultContainer = ppNewFrameLayoutJs()
    ppApi.defaultContainer.setLayoutParams(ppNewLinearLayoutParamsJs(-1, -1))
    ppApi.defaultContainer.setOnTouchListener(ppApi.baseTouchListener)
    ppApi.defaultContainer.addOnLayoutChangeListener(ppNewLayoutChangeListenerJs({
        view: dynamic,
        left: dynamic,
        top: dynamic,
        right: dynamic,
        bottom: dynamic,
        oldLeft: dynamic,
        oldTop: dynamic,
        oldRight: dynamic,
        oldBottom: dynamic ->
        ppDefaultLayoutChange(view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom)
    }))
    ppApi.defaultDecorLinear.addView(ppApi.defaultContainer)

    ppApi.defaultStub = ppNewTextViewJs()
    ppApi.defaultStub.setLayoutParams(ppNewLinearLayoutParamsJs(-1, -1))
    ppApi.defaultStub.setVisibility(js("G.View.GONE"))
    ppApi.defaultStub.setGravity(js("G.Gravity.CENTER"))
    ppApi.defaultStub.setPadding(ppDp(20), ppDp(20), ppDp(20), ppDp(20))
    ppApi.defaultStub.setText("拖动右上角方块调整大小\n长按右上角方块隐藏顶栏")
    ppApi.defaultDecorLinear.addView(ppApi.defaultStub)
    ppApi.defaultWindow.addView(ppApi.defaultDecorLinear)

    val floatWindowCallbacks = js("({})")
    floatWindowCallbacks.dispatchKeyEvent = { event: dynamic, thisObj: dynamic ->
        ppBackDispatchKey(event, thisObj)
    }
    floatWindowCallbacks.dispatchTouchEvent = { event: dynamic, thisObj: dynamic ->
        ppFloatDispatchTouch(event, thisObj)
    }
    ppApi.floatWindow = ppCreateFrameLayoutJs(floatWindowCallbacks)
    ppApi.floatContainer = ppApi.floatWindow
    ppApi.floatWindow.setContentDescription("FloatWindow")
    ppApi.floatContainer.setOnTouchListener(ppApi.baseTouchListener)
    ppApi.floatContainer.addOnLayoutChangeListener(ppNewLayoutChangeListenerJs({
        view: dynamic,
        left: dynamic,
        top: dynamic,
        right: dynamic,
        bottom: dynamic,
        oldLeft: dynamic,
        oldTop: dynamic,
        oldRight: dynamic,
        oldBottom: dynamic ->
        ppFloatLayoutChange(view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom)
    }))
    ppApi.thread = js("java.lang.Thread.currentThread()")
    return js("undefined")
}

private fun ppInitialize(): dynamic {
    js("G").ui({
        try {
            ppBuildDefaultUi()
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
    return js("undefined")
}

private fun ppCheckThread(self: dynamic): dynamic {
    val th = js("java.lang.Thread.currentThread()")
    if (ppTruthyJs(self.thread)) {
        if (ppLooseNeJs(self.thread, th)) {
            js("Log").throwError(ppNewErrorJs(
                "You can only touch page on " + self.thread + " instead of " + th + "."
            ))
        }
    } else {
        if (ppLooseNeJs(js("ctx").getMainLooper(), js("android.os.Looper").myLooper())) {
            js("Log").throwError(ppNewErrorJs(
                "You can only touch page on mainLooper thread of Context " + js("ctx") +
                    " instead of " + th + "."
            ))
        }
    }
    return js("undefined")
}

private fun ppUpdateDefault(self: dynamic): dynamic {
    if (ppTruthyJs(self.fullscreen) || ppTruthyJs(self.locked)) {
        self.headerView.setVisibility(js("G.View.GONE"))
    } else {
        js("Common").applyStyle(self.headerView, "bar_float_second")
        js("Common").applyStyle(self.titleView, "button_reactive", 2)
        js("Common").applyStyle(self.resizeView, "button_reactive", 2)
        js("Common").applyStyle(self.defaultStub, "container_default")
        js("Common").applyStyle(self.defaultStub, "textview_prompt", 3)
        self.headerView.setVisibility(js("G.View.VISIBLE"))
    }
    self.updateAlpha()
    return js("undefined")
}

private fun ppSetFullScreen(self: dynamic, isFullScreen: dynamic, isLocked: dynamic): dynamic {
    self.locked = ppBooleanJs(isLocked)
    if (ppTruthyJs(self.defaultVisible)) {
        if (ppTruthyJs(isFullScreen)) {
            self.fullscreen = true
            self.updateView(self.defaultWindow, 0, 0, -1, -1)
        } else {
            if (ppIsNaNJ(self.x)) self.initRect()
            self.fullscreen = false
            self.updateView(self.defaultWindow, self.x, self.y, self.width, self.height)
        }
        self.updateDefault()
    } else {
        self.fullscreen = ppBooleanJs(isFullScreen)
    }
    self.trigger("fullscreenChanged", isFullScreen, isLocked)
    return js("undefined")
}

private fun ppInitRect(self: dynamic): dynamic {
    val metrics = js("Common").getMetrics()
    self.x = metrics[0] * 0.25
    self.y = metrics[1] * 0.25
    self.width = metrics[0] * 0.5
    self.height = metrics[1] * 0.5
    return js("undefined")
}

private fun ppGetRect(self: dynamic): dynamic {
    val a = js("[]")
    a.push(self.x)
    a.push(self.y)
    a.push(self.width)
    a.push(self.height)
    return a
}

private fun ppSetRect(
    self: dynamic,
    x: dynamic,
    y: dynamic,
    width: dynamic,
    height: dynamic,
    fromUser: dynamic
): dynamic {
    val oldX = self.x
    val oldY = self.y
    val oldWidth = self.width
    val oldHeight = self.height
    if (x >= 0) self.x = x
    if (y >= 0) self.y = y
    if (width >= 0) self.width = ppMaxJs(width, self.minWidth)
    if (height >= 0) self.height = ppMaxJs(height, self.minHeight)
    if (ppTruthyJs(self.defaultVisible) && !ppTruthyJs(self.fullscreen)) {
        self.updateView(self.defaultWindow, self.x, self.y, self.width, self.height)
        self.trigger(
            "rectUpdate",
            self.x,
            self.y,
            self.width,
            self.height,
            oldX,
            oldY,
            oldWidth,
            oldHeight,
            fromUser
        )
    }
    return js("undefined")
}

private fun ppSetFocused(self: dynamic, focused: dynamic): dynamic {
    if (ppTruthyJs(focused) && !ppTruthyJs(self.focusable)) {
        self.focusable = true
        self.resizeView.setVisibility(js("G.View.VISIBLE"))
        self.setFocusable(self.defaultWindow, true)
        self.updateAlpha()
        self.trigger("focus")
    } else if (!ppTruthyJs(focused) && ppTruthyJs(self.focusable)) {
        self.focusable = false
        self.resizeView.setVisibility(js("G.View.GONE"))
        self.setFocusable(self.defaultWindow, false)
        self.updateAlpha()
        self.trigger("blur")
    }
    return js("undefined")
}

private fun ppSetAlpha(self: dynamic, focused: dynamic, unfocused: dynamic): dynamic {
    self.focusedAlpha = focused
    self.unfocusedAlpha = unfocused
    self.updateAlpha()
    return js("undefined")
}

private fun ppUpdateAlpha(self: dynamic): dynamic {
    if (ppTruthyJs(self.focusable)) {
        self.defaultWindow.setAlpha(ppApi.focusedAlpha)
    } else {
        self.defaultWindow.setAlpha(ppApi.unfocusedAlpha)
    }
    return js("undefined")
}

private fun ppBuildLayoutParams(view: dynamic, x: dynamic, y: dynamic, width: dynamic, height: dynamic): dynamic {
    var p = view.getLayoutParams()
    if (!ppTruthyJs(p)) p = ppNewWindowManagerParamsJs()
    val title = view.getContentDescription()
    p.gravity = ppGravityLeftTopJs()
    p.flags = ppOrFlags(
        p.flags,
        ppOrFlags(p.FLAG_NOT_TOUCH_MODAL, p.FLAG_WATCH_OUTSIDE_TOUCH)
    )
    p.type = if (ppTruthyJs(js("G.supportFloat"))) {
        if (ppSdkJs() >= 26) {
            js("G.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY")
        } else {
            js("G.WindowManager.LayoutParams.TYPE_PHONE")
        }
    } else {
        js("G.WindowManager.LayoutParams.TYPE_APPLICATION_PANEL")
    }
    if (ppIsActivityJs(js("ctx"))) {
        p.token = js("ctx").getWindow().getDecorView().getWindowToken()
    }
    p.format = js("G.PixelFormat.TRANSLUCENT")
    p.height = height
    p.width = width
    p.x = x
    p.y = y
    if (ppTruthyJs(title)) p.setTitle(title)
    return p
}

private val ppOrFlagsJs: dynamic = js("function (a, b) { return a | b; }")
private fun ppOrFlags(a: dynamic, b: dynamic): dynamic = ppOrFlagsJs(a, b)

private fun ppSetFocusable(view: dynamic, focusable: dynamic): dynamic {
    var p = view.getLayoutParams()
    if (!ppTruthyJs(p)) p = ppNewWindowManagerParamsJs()
    if (ppTruthyJs(focusable)) {
        p.flags = ppAndFlags(p.flags, ppNotFlags(p.FLAG_NOT_FOCUSABLE))
    } else {
        p.flags = ppOrFlags(p.flags, p.FLAG_NOT_FOCUSABLE)
    }
    js("PWM").wm.updateViewLayout(view, p)
    return js("undefined")
}

private val ppAndFlagsJs: dynamic = js("function (a, b) { return a & b; }")
private val ppNotFlagsJs: dynamic = js("function (a) { return ~a; }")
private fun ppAndFlags(a: dynamic, b: dynamic): dynamic = ppAndFlagsJs(a, b)
private fun ppNotFlags(a: dynamic): dynamic = ppNotFlagsJs(a)

private fun ppShowView(self: dynamic, view: dynamic, x: dynamic, y: dynamic, width: dynamic, height: dynamic): dynamic {
    try {
        js("PWM").wm.addView(view, self.buildLayoutParams(view, x, y, width, height))
        return true
    } catch (e: dynamic) {
        js("erp")(e, true)
    }
    return false
}

private fun ppHideView(view: dynamic): dynamic {
    try {
        js("PWM").wm.removeViewImmediate(view)
        return true
    } catch (e: dynamic) {
        js("erp")(e, true)
    }
    return false
}

private fun ppUpdateView(self: dynamic, view: dynamic, x: dynamic, y: dynamic, width: dynamic, height: dynamic): dynamic {
    try {
        js("PWM").wm.updateViewLayout(view, self.buildLayoutParams(view, x, y, width, height))
        return true
    } catch (e: dynamic) {
        js("erp")(e, true)
    }
    return false
}

private fun ppBack(self: dynamic, source: dynamic): dynamic {
    val stack = if (ppLooseEqJs(source, self.floatContainer)) self.floatStack else self.defaultStack
    var cancelEvent: dynamic = js("undefined")
    if (ppLength(stack) > 0) {
        val stackItem = stack[ppLength(stack) - 1]
        cancelEvent = stackItem.page.modal
        stackItem.page.trigger("back", {
            cancelEvent = true
            js("undefined")
        })
        if (!ppTruthyJs(cancelEvent)) stackItem.page.exit()
    }
    return js("undefined")
}

private fun ppShowPage(self: dynamic, page: dynamic): dynamic {
    self.checkThread()
    if (ppTruthyJs(page.currentContainer)) page.currentContainer.removeView(page.mainView)
    page.currentContainer = if (ppTruthyJs(self.visible)) self.defaultContainer else self.floatContainer
    page.currentContainer.addView(page.mainView)
    if (ppTruthyJs(self.debugPrint)) {
        js("Log").d("Attach " + page + " to " + page.currentContainer)
    }
    if (ppTruthyJs(self.visible) && !ppTruthyJs(self.defaultVisible)) {
        if (ppTruthyJs(self.fullscreen)) {
            self.showView(self.defaultWindow, 0, 0, -1, -1)
        } else {
            if (ppIsNaNJ(self.x)) self.initRect()
            self.showView(self.defaultWindow, self.x, self.y, self.width, self.height)
        }
        self.updateDefault()
        self.defaultVisible = true
        self.updateOverlays()
        self.trigger("addPopup")
        if (ppTruthyJs(self.debugPrint)) js("Log").d("Show DefaultWindow")
    } else if (!ppTruthyJs(self.visible) && !ppTruthyJs(self.floatVisible)) {
        self.showView(self.floatWindow, 0, 0, -1, -1)
        self.floatVisible = true
        self.updateOverlays()
        self.trigger("addPopup")
        if (ppTruthyJs(self.debugPrint)) js("Log").d("Show FloatWindow")
    }
    if (ppTruthyJs(self.visible)) {
        self.setFocused(true)
    }
    return js("undefined")
}

private fun ppHidePage(self: dynamic, page: dynamic, notRemoveWindow: dynamic): dynamic {
    val stack = if (ppLooseEqJs(page.currentContainer, self.floatContainer)) self.floatStack else self.defaultStack
    self.checkThread()
    if (!ppTruthyJs(page.currentContainer)) {
        js("Log").throwError(ppNewErrorJs(page + " was removed."))
    }
    if (ppLooseNeJs(page.mainView.getParent(), page.currentContainer)) {
        js("Log").throwError(ppNewErrorJs("This view has been moved unexpectedly: " + page))
    }
    if (ppTruthyJs(page.currentAnimation)) page.currentAnimation.cancel()
    page.currentContainer.removeView(page.mainView)
    if (ppTruthyJs(self.debugPrint)) {
        js("Log").d("Detach " + page + " from " + page.currentContainer)
    }
    if (ppLength(stack) == 0 && !ppTruthyJs(notRemoveWindow)) {
        if (ppLooseEqJs(page.currentContainer, self.defaultContainer) && ppTruthyJs(self.defaultVisible)) {
            self.hideView(self.defaultWindow)
            self.trigger("removePopup")
            self.defaultVisible = false
            self.updateOverlays()
            if (!ppTruthyJs(self.visible)) self.show()
            if (ppTruthyJs(self.debugPrint)) js("Log").d("Hide DefaultWindow")
        } else if (ppLooseEqJs(page.currentContainer, self.floatContainer) && ppTruthyJs(self.floatVisible)) {
            self.hideView(self.floatWindow)
            self.trigger("removePopup")
            self.floatVisible = false
            self.updateOverlays()
            if (ppTruthyJs(self.debugPrint)) js("Log").d("Hide FloatWindow")
        }
    }
    page.currentContainer = js("null")
    return js("undefined")
}

private fun ppPushPage(self: dynamic, name: dynamic, page: dynamic): dynamic {
    val stack = if (ppLooseEqJs(page.currentContainer, self.floatContainer)) self.floatStack else self.defaultStack
    self.checkThread()
    if (ppTruthyJs(self.busy)) {
        js("Log").d("pushPage(" + name + "," + page + ") cancelled")
        return js("undefined")
    }
    if (ppLength(stack) > 0) {
        val t = stack[ppLength(stack) - 1]
        self.analytics.onPageEnd(js("ctx"), t.name)
        if (ppLooseNeJs(page.currentContainer, self.defaultContainer) || ppTruthyJs(self.visible)) {
            t.page.trigger("pause")
            if (ppTruthyJs(self.debugPrint)) js("Log").d(t.page + " paused")
        }
    }
    val item = js("({})")
    item.name = name
    item.page = page
    stack.push(item)
    page.trigger("enter")
    self.analytics.onPageStart(js("ctx"), name)
    if (ppTruthyJs(self.debugPrint)) js("Log").d(item.page + " entered")
    self.trigger("pushPage", name, page)
    return js("undefined")
}

private fun ppPopPage(self: dynamic, page: dynamic): dynamic {
    val stack = if (ppLooseEqJs(page.currentContainer, self.floatContainer)) self.floatStack else self.defaultStack
    self.checkThread()
    if (ppTruthyJs(self.busy)) {
        js("Log").d("popPage(" + page + ") cancelled")
        return js("undefined")
    }
    var i = ppLength(stack) - 1
    while (i >= 0) {
        if (ppLooseNeJs(stack[i].page, page)) {
            i--
            continue
        }
        val t = stack[i]
        stack.splice(i, 1)
        t.page.trigger("exit")
        self.analytics.onPageEnd(js("ctx"), t.name)
        if (ppTruthyJs(self.debugPrint)) js("Log").d(t.page + " exited")
        if (i > 0 && i == ppLength(stack)) {
            val prev = stack[i - 1]
            self.analytics.onPageStart(js("ctx"), prev.name)
            if (ppLooseNeJs(page.currentContainer, self.defaultContainer) || ppTruthyJs(self.visible)) {
                prev.page.trigger("resume")
                if (ppTruthyJs(self.debugPrint)) js("Log").d(prev.page + " resumed")
            }
            i--
            while (i >= 0) {
                stack[i].page.requestShow()
                if (!ppTruthyJs(stack[i].page.dialog)) break
                i--
            }
        }
        break
    }
    self.trigger("popPage", page)
    return js("undefined")
}

private fun ppPageShown(self: dynamic, page: dynamic): dynamic {
    val stack = if (ppLooseEqJs(page.currentContainer, self.floatContainer)) self.floatStack else self.defaultStack
    self.checkThread()
    if (ppLength(stack) > 1) {
        if (!ppTruthyJs(page.dialog)) {
            var i = ppLength(stack) - 1
            while (i > 0) {
                i--
                stack[i].page.requestHide()
            }
        }
    }
    self.trigger("pageShown", page)
    return js("undefined")
}

private fun ppOnPageMissing(self: dynamic, v: dynamic): dynamic {
    val count = v.getChildCount()
    if (count > 0 && ppLooseEqJs(v.getChildAt(count - 1).getVisibility(), js("G.View.VISIBLE"))) {
        return js("undefined")
    }
    js("erp")(
        ppNewErrorJs("Page touch event leaked! Debug:\n" + self.debug()),
        true
    )
    self.dismiss()
    return js("undefined")
}

private fun ppGetTopPage(self: dynamic): dynamic {
    if (ppTruthyJs(self.floatVisible) && ppLength(self.floatStack) > 0) {
        return self.floatStack[ppLength(self.floatStack) - 1].page
    } else if (ppTruthyJs(self.defaultVisible) && ppTruthyJs(self.visible) && ppLength(self.defaultStack) > 0) {
        return self.defaultStack[ppLength(self.defaultStack) - 1].page
    }
    return js("null")
}

private fun ppShow(self: dynamic): dynamic {
    self.checkThread()
    if (ppTruthyJs(self.visible)) return js("undefined")
    self.visible = true
    if (ppTruthyJs(self.debugPrint)) js("Log").d("DefaultWindow visible = true")
    if (ppLength(self.floatStack) > 0) {
        self.hideView(self.floatWindow)
        self.floatVisible = false
        var i = 0
        while (i < ppLength(self.floatStack)) {
            self.showPage(self.floatStack[i].page)
            self.defaultStack.push(self.floatStack[i])
            i++
        }
        ppSetLengthJs(self.floatStack, 0)
    } else {
        if (ppLength(self.defaultStack) > 0) {
            val page = self.defaultStack[ppLength(self.defaultStack) - 1].page
            page.trigger("resume")
            if (ppTruthyJs(self.debugPrint)) js("Log").d(page + " resumed")
        }
    }
    self.defaultWindow.setVisibility(js("G.View.VISIBLE"))
    self.setFocused(true)
    self.updateOverlays()
    self.trigger("show")
    return js("undefined")
}

private fun ppHide(self: dynamic): dynamic {
    self.checkThread()
    if (!ppTruthyJs(self.visible)) return js("undefined")
    if (ppTruthyJs(self.debugPrint)) js("Log").d("DefaultWindow visible = false")
    if (ppLength(self.defaultStack) > 0) {
        val page = self.defaultStack[ppLength(self.defaultStack) - 1].page
        page.trigger("pause")
        if (ppTruthyJs(self.debugPrint)) js("Log").d(page + " paused")
    }
    self.defaultWindow.setVisibility(js("G.View.GONE"))
    self.visible = false
    self.updateOverlays()
    self.trigger("hide")
    return js("undefined")
}

private fun ppAddDisappearing(self: dynamic, page: dynamic): dynamic {
    val i = self.disappearingList.indexOf(page)
    if (i < 0) self.disappearingList.push(page)
    return js("undefined")
}

private fun ppRemoveDisappearing(self: dynamic, page: dynamic): dynamic {
    val i = self.disappearingList.indexOf(page)
    if (i >= 0) self.disappearingList.splice(i, 1)
    return js("undefined")
}

private fun ppDismiss(self: dynamic): dynamic {
    self.checkThread()
    self.busy = true
    try {
        var i = ppLength(self.floatStack) - 1
        while (i >= 0) {
            val e = self.floatStack[i]
            e.page.trigger("exit")
            self.analytics.onPageEnd(js("ctx"), e.name)
            self.hidePage(e.page, true)
            e.page.showing = false
            i--
        }
        i = ppLength(self.defaultStack) - 1
        while (i >= 0) {
            val e = self.defaultStack[i]
            e.page.trigger("exit")
            self.analytics.onPageEnd(js("ctx"), e.name)
            self.hidePage(e.page, true)
            e.page.showing = false
            i--
        }
        i = ppLength(self.disappearingList) - 1
        while (i >= 0) {
            self.disappearingList[i].dismiss()
            i--
        }
        ppSetLengthJs(self.defaultStack, 0)
        ppSetLengthJs(self.floatStack, 0)
        ppSetLengthJs(self.disappearingList, 0)
        if (ppTruthyJs(self.defaultVisible)) {
            self.hideView(self.defaultWindow)
            self.trigger("removePopup")
            self.defaultVisible = false
        }
        if (ppTruthyJs(self.floatVisible)) {
            self.hideView(self.floatWindow)
            self.trigger("removePopup")
            self.floatVisible = false
        }
        self.updateOverlays()
    } catch (e: dynamic) {
        js("erp")(e)
    }
    self.busy = false
    self.trigger("dismiss")
    return js("undefined")
}

private fun ppReset(self: dynamic): dynamic {
    self.dismiss()
    self.fullscreen = true
    self.focusable = true
    self.x = js("undefined")
    self.y = js("undefined")
    self.width = js("undefined")
    self.height = js("undefined")
    self.trigger("reset")
    self.clearListeners()
    return js("undefined")
}

private fun ppGetCount(self: dynamic): dynamic =
    ppLength(self.defaultStack) + ppLength(self.floatStack)

private fun ppDebug(self: dynamic): dynamic {
    val s = js("[]")
    s.push("PageManager[visible=" + self.visible + "]")
    s.push("DefaultWindowPageManager[showing=" + self.defaultVisible + ",fullscreen=" + self.fullscreen + "]")
    var i = 0
    while (i < ppLength(self.defaultStack)) {
        val e = self.defaultStack[i]
        s.push(
            "" + i + ":" + e.name + "[" +
                (if (ppTruthyJs(e.page.modal)) "M" else "") +
                "]" + e.page.mainView
        )
        i++
    }
    s.push("FloatWindowPageManager[showing=" + self.floatVisible + ",fullscreen=true]")
    i = 0
    while (i < ppLength(self.floatStack)) {
        val e = self.floatStack[i]
        s.push(
            "" + i + ":" + e.name + "[" +
                (if (ppTruthyJs(e.page.modal)) "M" else "") +
                "]" + e.page.mainView
        )
        i++
    }
    return s.join("\n")
}

private fun ppGetActiveContainer(self: dynamic): dynamic {
    if (ppTruthyJs(self.floatVisible)) {
        return self.floatContainer
    } else if (ppTruthyJs(self.defaultVisible) && ppTruthyJs(self.visible)) {
        return self.defaultContainer
    }
    return js("null")
}

// ---------------- 非 Android 分支（暂不维护，保持原有行为） ----------------

private fun ppOtherInit(self: dynamic): dynamic {
    self.popup = ppNewPopupWindowFullJs(self.mainView, -1, -1)
    self.popup.setOnDismissListener(ppNewDismissListenerJs({
        try {
            ppApi.popPage(self)
            self.showing = false
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }))
    if (!ppTruthyJs(self.modal)) {
        self.popup.setBackgroundDrawable(ppNewColorDrawableJs(js("G.Color.TRANSPARENT")))
    }
    self.popup.setFocusable(true)
    self.popup.setSoftInputMode(js("G.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE"))
    self.popup.setWindowLayoutType(
        if (ppTruthyJs(js("G.supportFloat"))) {
            if (ppSdkJs() >= 26) {
                js("G.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY")
            } else {
                js("G.WindowManager.LayoutParams.TYPE_PHONE")
            }
        } else {
            js("G.WindowManager.LayoutParams.TYPE_APPLICATION_PANEL")
        }
    )
    return js("undefined")
}

private fun ppOtherEnter(self: dynamic, noAnimation: dynamic): dynamic {
    if (ppTruthyJs(self.showing)) self.popup.dismiss()
    self.popup.showAtLocation(
        js("ctx").getWindow().getDecorView(),
        ppGravityLeftTopJs(),
        0,
        0
    )
    if (!ppTruthyJs(noAnimation) && ppTruthyJs(self._enterAnimation)) {
        self._enterAnimation(self.mainView, {
            ppApi.pushPage(self.name, self)
            js("undefined")
        })
    } else {
        ppApi.pushPage(self.name, self)
    }
    self.showing = true
    return self
}

private fun ppOtherExit(self: dynamic, noAnimation: dynamic): dynamic {
    if (!ppTruthyJs(self.showing)) return self
    if (!ppTruthyJs(noAnimation) && ppTruthyJs(self._exitAnimation)) {
        self._exitAnimation(self.mainView, {
            self.popup.dismiss()
            js("undefined")
        })
    } else {
        self.popup.dismiss()
    }
    return self
}

private fun ppOtherResizable(self: dynamic): dynamic = false

private fun ppOtherDismiss(self: dynamic): dynamic = self.exit(true)

private fun ppOtherRequestShow(self: dynamic): dynamic {
    self.mainView.getRootView().setVisibility(js("G.View.VISIBLE"))
    return self
}

private fun ppOtherRequestHide(self: dynamic): dynamic {
    self.mainView.getRootView().setVisibility(js("G.View.GONE"))
    return self
}

private fun ppOtherGetWidth(self: dynamic): dynamic = self.mainView.getWidth()

private fun ppOtherGetHeight(self: dynamic): dynamic = self.mainView.getHeight()

private fun ppOtherGetMetrics(self: dynamic): dynamic = ppArray2Js(self.getWidth(), self.getHeight())

private fun ppOtherPushPage(self: dynamic, name: dynamic, page: dynamic): dynamic {
    if (ppTruthyJs(self.busy)) return js("undefined")
    if (ppLength(self.stack) > 0 && ppTruthyJs(self.stack[ppLength(self.stack) - 1].visible)) {
        self.stack[ppLength(self.stack) - 1].page.trigger("pause")
    }
    val item = js("({})")
    item.name = name
    item.page = page
    item.visible = true
    self.stack.push(item)
    page.trigger("enter")
    self.trigger("pushPage", name, page)
    self.trigger("addPopup")
    return js("undefined")
}

private fun ppOtherPopPage(self: dynamic, page: dynamic): dynamic {
    if (ppTruthyJs(self.busy)) return js("undefined")
    var i = ppLength(self.stack) - 1
    while (i >= 0) {
        if (ppLooseNeJs(self.stack[i].page, page)) {
            i--
            continue
        }
        val removed = self.stack.splice(i, ppLength(self.stack) - i)
        var j = 0
        while (j < ppLength(removed)) {
            removed[j].page.trigger("exit")
            j++
        }
        if (i > 0 && ppTruthyJs(self.visible)) {
            self.stack[i - 1].page.trigger("resume")
        }
        break
    }
    self.trigger("popPage", page)
    self.trigger("removePopup")
    return js("undefined")
}

private fun ppOtherShow(self: dynamic): dynamic {
    if (ppTruthyJs(self.visible)) return js("undefined")
    if (ppLength(self.stack) > 0) {
        self.stack[ppLength(self.stack) - 1].page.trigger("resume")
    }
    var i = 0
    while (i < ppLength(self.stack)) {
        val e = self.stack[i]
        if (!ppTruthyJs(e.visible)) {
            e.page.requestShow()
            e.visible = true
        }
        i++
    }
    self.visible = true
    self.trigger("show")
    return js("undefined")
}

private fun ppOtherHide(self: dynamic): dynamic {
    if (!ppTruthyJs(self.visible)) return js("undefined")
    if (ppLength(self.stack) > 0) {
        self.stack[ppLength(self.stack) - 1].page.trigger("pause")
    }
    var i = ppLength(self.stack) - 1
    while (i >= 0) {
        val e = self.stack[i]
        if (ppTruthyJs(e.visible)) {
            e.page.requestHide()
            e.visible = false
        }
        i--
    }
    self.visible = false
    self.trigger("hide")
    return js("undefined")
}

private fun ppOtherDismissAll(self: dynamic): dynamic {
    self.busy = true
    var i = ppLength(self.stack) - 1
    while (i >= 0) {
        val e = self.stack[i]
        e.page.trigger("exit")
        e.page.exit()
        i--
    }
    ppSetLengthJs(self.stack, 0)
    self.busy = false
    self.trigger("dismiss")
    return js("undefined")
}

private fun ppOtherReset(self: dynamic): dynamic {
    self.dismiss()
    self.trigger("reset")
    self.clearListeners()
    return js("undefined")
}

private fun ppOtherGetCount(self: dynamic): dynamic = ppLength(self.stack)

private fun ppOtherDebug(self: dynamic): dynamic {
    val s = js("[]")
    s.push("PopupWindowPageManager[visible=" + self.visible + "]")
    var i = 0
    while (i < ppLength(self.stack)) {
        val e = self.stack[i]
        s.push(
            "" + i + ":" + e.name + "[" +
                (if (ppTruthyJs(e.visible)) "V" else "") +
                (if (ppTruthyJs(e.page.showing)) "S" else "") +
                (if (ppTruthyJs(e.page.modal)) "M" else "") +
                "]" + e.page.mainView
        )
        i++
    }
    return s.join("\n")
}

private fun ppOtherGetActiveContainer(self: dynamic): dynamic = js("null")

// ---------------- 动画 ----------------

private fun ppFadeInAnimation(v: dynamic, callback: dynamic): dynamic {
    val aniSet = ppNewAnimationSetJs(true)
    aniSet.setDuration(300)
    if (js("G.style") == "Material") {
        aniSet.setInterpolator(js("ctx"), js("G.R.interpolator.fast_out_slow_in"))
    }
    aniSet.setAnimationListener(ppNewAnimationListenerJs({ _: dynamic ->
        try {
            if (ppTruthyJs(callback)) callback()
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }))
    aniSet.addAnimation(ppNewAlphaAnimationJs(0, 1))
    aniSet.addAnimation(ppNewScaleAnimationJs(
        0.95,
        1,
        0.95,
        1,
        js("G.Animation.RELATIVE_TO_SELF"),
        0.5,
        js("G.Animation.RELATIVE_TO_SELF"),
        0.5
    ))
    return ppNewControllerJs(ppApi.ViewAnimationController, v, aniSet).start()
}

private fun ppFadeOutAnimation(v: dynamic, callback: dynamic): dynamic {
    val aniSet = ppNewAnimationSetJs(true)
    aniSet.setDuration(200)
    aniSet.setFillAfter(true)
    if (js("G.style") == "Material") {
        aniSet.setInterpolator(js("ctx"), js("G.R.interpolator.fast_out_slow_in"))
    }
    aniSet.setAnimationListener(ppNewAnimationListenerJs({ _: dynamic ->
        try {
            v.post({
                try {
                    if (ppTruthyJs(callback)) callback()
                } catch (e: dynamic) {
                    js("erp")(e)
                }
                js("undefined")
            })
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }))
    aniSet.addAnimation(ppNewAlphaAnimationJs(1, 0))
    return ppNewControllerJs(ppApi.ViewAnimationController, v, aniSet).start()
}

private fun ppDialogEnterAnimation(param: dynamic, v: dynamic, callback: dynamic): dynamic {
    val layout = param.layout
    val alphaAni = ppNewAlphaAnimationJs(0, 1)
    alphaAni.setDuration(200)
    val scaleAni = ppNewScaleAnimationJs(
        0.95,
        1,
        0.95,
        1,
        js("G.Animation.RELATIVE_TO_SELF"),
        0.5,
        js("G.Animation.RELATIVE_TO_SELF"),
        0.5
    )
    scaleAni.setDuration(200)
    scaleAni.setAnimationListener(ppNewAnimationListenerJs({ _: dynamic ->
        try {
            if (ppTruthyJs(callback)) callback()
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }))
    if (js("G.style") == "Material") {
        alphaAni.setInterpolator(js("ctx"), js("G.R.interpolator.fast_out_slow_in"))
        scaleAni.setInterpolator(js("ctx"), js("G.R.interpolator.fast_out_slow_in"))
    }
    return ppNewControllerJs(ppApi.ViewAnimationController, v, alphaAni)
        .addAnimation(layout, scaleAni)
        .start()
}

private fun ppSideBarEnterAnimation(param: dynamic, v: dynamic, callback: dynamic): dynamic {
    val background = param.background
    val layout = param.layout
    val direction = param.direction
    val offsets = if (ppLooseEqJs(direction, "left")) {
        js("([-1, 0, 0, 0])")
    } else if (ppLooseEqJs(direction, "top")) {
        js("([0, 0, -1, 0])")
    } else if (ppLooseEqJs(direction, "right")) {
        js("([1, 0, 0, 0])")
    } else {
        js("([0, 0, 1, 0])")
    }
    val alphaAni = ppNewAlphaAnimationJs(0, 1)
    alphaAni.setDuration(200)
    val transAni = ppNewTranslateAnimationJs(
        js("G.Animation.RELATIVE_TO_SELF"),
        offsets[0],
        js("G.Animation.RELATIVE_TO_SELF"),
        offsets[1],
        js("G.Animation.RELATIVE_TO_SELF"),
        offsets[2],
        js("G.Animation.RELATIVE_TO_SELF"),
        offsets[3]
    )
    transAni.setDuration(200)
    transAni.setAnimationListener(ppNewAnimationListenerJs({ _: dynamic ->
        try {
            if (ppTruthyJs(callback)) callback()
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }))
    if (js("G.style") == "Material") {
        alphaAni.setInterpolator(js("ctx"), js("G.R.interpolator.fast_out_slow_in"))
        transAni.setInterpolator(js("ctx"), js("G.R.interpolator.fast_out_slow_in"))
    }
    return ppNewControllerJs(ppApi.ViewAnimationController, background, alphaAni)
        .addAnimation(layout, transAni)
        .start()
}

private fun ppSideBarExitAnimation(param: dynamic, v: dynamic, callback: dynamic): dynamic {
    val background = param.background
    val layout = param.layout
    val direction = param.direction
    val offsets = if (ppLooseEqJs(direction, "left")) {
        js("([0, -1, 0, 0])")
    } else if (ppLooseEqJs(direction, "top")) {
        js("([0, 0, 0, -1])")
    } else if (ppLooseEqJs(direction, "right")) {
        js("([0, 1, 0, 0])")
    } else {
        js("([0, 0, 0, 1])")
    }
    val alphaAni = ppNewAlphaAnimationJs(1, 0)
    alphaAni.setDuration(200)
    val transAni = ppNewTranslateAnimationJs(
        js("G.Animation.RELATIVE_TO_SELF"),
        offsets[0],
        js("G.Animation.RELATIVE_TO_SELF"),
        offsets[1],
        js("G.Animation.RELATIVE_TO_SELF"),
        offsets[2],
        js("G.Animation.RELATIVE_TO_SELF"),
        offsets[3]
    )
    transAni.setDuration(200)
    transAni.setAnimationListener(ppNewAnimationListenerJs({ _: dynamic ->
        try {
            if (ppTruthyJs(callback)) callback()
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }))
    if (js("G.style") == "Material") {
        alphaAni.setInterpolator(js("ctx"), js("G.R.interpolator.fast_out_slow_in"))
        transAni.setInterpolator(js("ctx"), js("G.R.interpolator.fast_out_slow_in"))
    }
    return ppNewControllerJs(ppApi.ViewAnimationController, background, alphaAni)
        .addAnimation(layout, transAni)
        .start()
}

private fun ppControllerAdd(self: dynamic, v: dynamic, ani: dynamic): dynamic {
    if (ppTruthyJs(v) || ppTruthyJs(ani)) {
        self.views.push(v)
        self.animations.push(ani)
    }
    return self
}

private fun ppControllerStart(self: dynamic): dynamic {
    var i = 0
    while (i < ppLength(self.views)) {
        if (ppTruthyJs(self.views[i]) && ppTruthyJs(self.animations[i])) {
            self.views[i].startAnimation(self.animations[i])
        }
        i++
    }
    return self
}

private fun ppControllerCancel(self: dynamic): dynamic {
    var i = 0
    while (i < ppLength(self.views)) {
        if (ppTruthyJs(self.views[i])) self.views[i].clearAnimation()
        i++
    }
    return self
}

// ---------------- 对话框 / 侧栏 ----------------

private fun ppDialogResize(state: dynamic, layout: dynamic, w: dynamic, h: dynamic): Unit {
    val newHasMargins = w > ppDp(40) && h > ppDp(40)
    if (ppLooseEqJs(state.hasMargins, newHasMargins)) return
    state.hasMargins = newHasMargins
    if (ppTruthyJs(newHasMargins)) {
        layout.getLayoutParams().setMargins(ppDp(20), ppDp(20), ppDp(20), ppDp(20))
    } else {
        layout.getLayoutParams().setMargins(0, 0, 0, 0)
    }
    layout.requestLayout()
}

private fun ppShowDialog(
    name: dynamic,
    layout: dynamic,
    width: dynamic,
    height: dynamic,
    modal: dynamic
): dynamic {
    var popup: dynamic = js("null")
    val state = js("({ hasMargins: false })")
    val frame = ppNewFrameLayoutJs()
    frame.setBackgroundColor(js("Common").argbInt(0x80, 0, 0, 0))
    frame.setOnTouchListener(ppNewTouchListenerJs({ v: dynamic, e: dynamic ->
        try {
            if (ppLooseEqJs(e.getAction(), e.ACTION_DOWN) && !ppTruthyJs(modal)) {
                if (e.getX() < layout.getLeft() || e.getX() >= layout.getRight() ||
                    e.getY() < layout.getTop() || e.getY() >= layout.getBottom()
                ) {
                    popup.exit()
                }
            }
            true
        } catch (err: dynamic) {
            js("erp")(err)
            true
        }
    }))
    layout.setLayoutParams(ppNewFrameLayoutParamsGravityJs(width, height, js("G.Gravity.CENTER")))
    frame.addView(layout)
    if (js("G.style") == "Material") layout.setElevation(ppDp(16))
    popup = ppNewJs(ppApi, frame, name, modal)
    popup.enterAnimation(ppApi.dialogEnterAnimation.bind(ppApi, ppParamLayoutJs(layout)))
    popup.on("resume", {
        frame.setBackgroundColor(js("Common").argbInt(0x80, 0, 0, 0))
        js("undefined")
    })
    popup.on("pause", {
        frame.setBackground(js("null"))
        js("undefined")
    })
    popup.on("resize", { event: dynamic, w: dynamic, h: dynamic ->
        ppDialogResize(state, layout, w, h)
        js("undefined")
    })
    popup.dialog = true
    popup.enter()
    popup.trigger("resize", popup.getWidth(), popup.getHeight())
    return popup
}

private fun ppShowSideBar(
    name: dynamic,
    layout: dynamic,
    direction: dynamic,
    offsetAdd: dynamic,
    offsetMul: dynamic,
    modal: dynamic
): dynamic {
    var popup: dynamic = js("null")
    val frame = ppNewFrameLayoutJs()
    val background = ppNewImageViewJs()
    val bgdrawable = ppNewColorDrawableJs(js("Common").argbInt(0x80, 0, 0, 0))
    background.setImageDrawable(bgdrawable)
    background.setLayoutParams(ppNewFrameLayoutParamsJs(-1, -1))
    frame.addView(background)

    val linear = ppNewLinearLayoutJs()
    linear.setWeightSum(1.0)
    linear.setLayoutParams(ppNewFrameLayoutParamsJs(-1, -1))
    linear.setOnTouchListener(ppNewTouchListenerJs({ v: dynamic, e: dynamic ->
        try {
            if (ppLooseEqJs(e.getAction(), e.ACTION_DOWN) && !ppTruthyJs(modal)) {
                if (e.getX() < layout.getLeft() || e.getX() >= layout.getRight() ||
                    e.getY() < layout.getTop() || e.getY() >= layout.getBottom()
                ) {
                    popup.exit()
                }
            }
            true
        } catch (err: dynamic) {
            js("erp")(err)
            true
        }
    }))
    if (ppLooseEqJs(direction, "left")) {
        linear.setOrientation(js("G.LinearLayout.HORIZONTAL"))
        linear.setGravity(js("G.Gravity.LEFT"))
        layout.setLayoutParams(ppNewWeightedLayoutParamsJs(offsetAdd, -1, offsetMul))
    } else if (ppLooseEqJs(direction, "top")) {
        linear.setOrientation(js("G.LinearLayout.VERTICAL"))
        linear.setGravity(js("G.Gravity.TOP"))
        layout.setLayoutParams(ppNewWeightedLayoutParamsJs(-1, offsetAdd, offsetMul))
    } else if (ppLooseEqJs(direction, "right")) {
        linear.setOrientation(js("G.LinearLayout.HORIZONTAL"))
        linear.setGravity(js("G.Gravity.RIGHT"))
        layout.setLayoutParams(ppNewWeightedLayoutParamsJs(offsetAdd, -1, offsetMul))
    } else {
        linear.setOrientation(js("G.LinearLayout.VERTICAL"))
        linear.setGravity(js("G.Gravity.BOTTOM"))
        layout.setLayoutParams(ppNewWeightedLayoutParamsJs(-1, offsetAdd, offsetMul))
    }
    linear.addView(layout)
    frame.addView(linear)
    if (js("G.style") == "Material") layout.setElevation(ppDp(16))
    popup = ppNewJs(ppApi, frame, name, modal)
    val param = ppSideBarParamJs(background, layout, direction)
    popup.enterAnimation(ppApi.sideBarEnterAnimation.bind(ppApi, param))
    popup.exitAnimation(ppApi.sideBarExitAnimation.bind(ppApi, param))
    popup.on("resume", {
        background.setImageDrawable(bgdrawable)
        js("undefined")
    })
    popup.on("pause", {
        background.setImageDrawable(js("null"))
        js("undefined")
    })
    popup.dialog = true
    popup.enter()
    return popup
}

// ---------------- 覆盖层 ----------------

private fun ppOverlayInit(
    self: dynamic,
    view: dynamic,
    width: dynamic,
    height: dynamic,
    gravity: dynamic,
    x: dynamic,
    y: dynamic
) {
    self.view = view
    self.width = if (ppTruthyJs(width)) width else -1
    self.height = if (ppTruthyJs(height)) height else -1
    self.gravity = if (ppTruthyJs(gravity)) gravity else ppGravityLeftTopJs()
    self.x = if (ppTruthyJs(x)) x else 0
    self.y = if (ppTruthyJs(y)) y else 0
    self.update()
}

private fun ppOverlayAttach(self: dynamic, container: dynamic): dynamic {
    if (ppTruthyJs(container)) {
        container.addView(
            self.view,
            ppNewFrameLayoutParamsGravityJs(self.width, self.height, self.gravity)
        )
        self.view.setTranslationX(self.x)
        self.view.setTranslationY(self.y)
        self.container = container
    } else {
        self.popup = ppNewPopupWindowGlobalJs(self.view, self.width, self.height)
        self.popup.show(ppOverlayParamJs(self.x, self.y, self.width, self.height, self.gravity))
        js("PWM").addFloat(self.popup)
    }
    return js("undefined")
}

private fun ppOverlayDetach(self: dynamic): dynamic {
    if (ppTruthyJs(self.popup)) {
        if (ppTruthyJs(self.popup.showing)) self.popup.hide()
        self.popup = js("null")
    }
    if (ppTruthyJs(self.container)) {
        self.container.removeView(self.view)
        self.container = js("null")
    }
    return js("undefined")
}

private fun ppOverlayUpdate(self: dynamic, force: dynamic): dynamic {
    val newContainer = ppApi.getActiveContainer()
    if (!ppTruthyJs(force) && ppLooseEqJs(self.container, newContainer)) return js("undefined")
    self.detach()
    self.attach(newContainer)
    return js("undefined")
}

// ---------------- 安装 ----------------

private fun ppInstallAndroid() {
    ppApi.fullscreen = true
    ppApi.focusable = true
    ppApi.debugPrint = false
    ppApi.focusedAlpha = 1
    ppApi.unfocusedAlpha = 0.6
    ppApi.defaultVisible = false
    ppApi.floatVisible = false
    ppApi.defaultStack = js("[]")
    ppApi.floatStack = js("[]")
    ppApi.disappearingList = js("[]")
    ppApi.visible = true
    ppApi.supportResize = true

    ppApi.initialize = { ppInitialize() }
    ppApi.analytics = js("ScriptInterface").getAnalyticsPlatform()
    ppApi.checkThread = ppThisJs { self: dynamic, _: dynamic -> ppCheckThread(self) }
    ppApi.updateDefault = ppThisJs { self: dynamic, _: dynamic -> ppUpdateDefault(self) }
    ppApi.setFullScreen = ppThisJs { self: dynamic, args: dynamic ->
        ppSetFullScreen(self, args[0], args[1])
    }
    ppApi.isFullScreen = ppThisJs { self: dynamic, _: dynamic -> self.fullscreen }
    ppApi.isLocked = ppThisJs { self: dynamic, _: dynamic -> self.locked }
    ppApi.initRect = ppThisJs { self: dynamic, _: dynamic -> ppInitRect(self) }
    ppApi.getRect = ppThisJs { self: dynamic, _: dynamic -> ppGetRect(self) }
    ppApi.setRect = ppThisJs { self: dynamic, args: dynamic ->
        ppSetRect(self, args[0], args[1], args[2], args[3], args[4])
    }
    ppApi.setFocused = ppThisJs { self: dynamic, args: dynamic -> ppSetFocused(self, args[0]) }
    ppApi.setAlpha = ppThisJs { self: dynamic, args: dynamic -> ppSetAlpha(self, args[0], args[1]) }
    ppApi.updateAlpha = ppThisJs { self: dynamic, _: dynamic -> ppUpdateAlpha(self) }
    ppApi.buildLayoutParams = { view: dynamic, x: dynamic, y: dynamic, width: dynamic, height: dynamic ->
        ppBuildLayoutParams(view, x, y, width, height)
    }
    ppApi.setFocusable = { view: dynamic, focusable: dynamic -> ppSetFocusable(view, focusable) }
    ppApi.showView = ppThisJs { self: dynamic, args: dynamic ->
        ppShowView(self, args[0], args[1], args[2], args[3], args[4])
    }
    ppApi.hideView = { view: dynamic -> ppHideView(view) }
    ppApi.updateView = ppThisJs { self: dynamic, args: dynamic ->
        ppUpdateView(self, args[0], args[1], args[2], args[3], args[4])
    }
    ppApi.back = ppThisJs { self: dynamic, args: dynamic -> ppBack(self, args[0]) }
    ppApi.showPage = ppThisJs { self: dynamic, args: dynamic -> ppShowPage(self, args[0]) }
    ppApi.hidePage = ppThisJs { self: dynamic, args: dynamic ->
        ppHidePage(self, args[0], args[1])
    }
    ppApi.pushPage = ppThisJs { self: dynamic, args: dynamic -> ppPushPage(self, args[0], args[1]) }
    ppApi.popPage = ppThisJs { self: dynamic, args: dynamic -> ppPopPage(self, args[0]) }
    ppApi.pageShown = ppThisJs { self: dynamic, args: dynamic -> ppPageShown(self, args[0]) }
    ppApi.onPageMissing = ppThisJs { self: dynamic, args: dynamic -> ppOnPageMissing(self, args[0]) }
    ppApi.getTopPage = ppThisJs { self: dynamic, _: dynamic -> ppGetTopPage(self) }
    ppApi.show = ppThisJs { self: dynamic, _: dynamic -> ppShow(self) }
    ppApi.hide = ppThisJs { self: dynamic, _: dynamic -> ppHide(self) }
    ppApi.addDisappearing = ppThisJs { self: dynamic, args: dynamic ->
        ppAddDisappearing(self, args[0])
    }
    ppApi.removeDisappearing = ppThisJs { self: dynamic, args: dynamic ->
        ppRemoveDisappearing(self, args[0])
    }
    ppApi.dismiss = ppThisJs { self: dynamic, _: dynamic -> ppDismiss(self) }
    ppApi.reset = ppThisJs { self: dynamic, _: dynamic -> ppReset(self) }
    ppApi.getCount = ppThisJs { self: dynamic, _: dynamic -> ppGetCount(self) }
    ppApi.debug = ppThisJs { self: dynamic, _: dynamic -> ppDebug(self) }
    ppApi.getActiveContainer = ppThisJs { self: dynamic, _: dynamic -> ppGetActiveContainer(self) }

    val proto = js("({})")
    proto.init = ppThisJs { self: dynamic, _: dynamic -> js("undefined") }
    proto.enter = ppThisJs { self: dynamic, args: dynamic -> ppEnter(self, args[0]) }
    proto.exit = ppThisJs { self: dynamic, args: dynamic -> ppExit(self, args[0]) }
    proto.dismiss = ppThisJs { self: dynamic, _: dynamic ->
        self.exit(true)
        js("undefined")
    }
    proto.resizable = ppThisJs { self: dynamic, _: dynamic ->
        ppLooseEqJs(self.currentContainer, ppApi.defaultContainer)
    }
    proto.visible = ppThisJs { self: dynamic, _: dynamic -> ppVisible(self) }
    proto.processHide = ppThisJs { self: dynamic, _: dynamic -> ppProcessHide(self) }
    proto.requestShow = ppThisJs { self: dynamic, _: dynamic -> ppRequestShow(self) }
    proto.requestHide = ppThisJs { self: dynamic, _: dynamic -> ppRequestHide(self) }
    proto.getWidth = ppThisJs { self: dynamic, _: dynamic -> self.currentContainer.getWidth() }
    proto.getHeight = ppThisJs { self: dynamic, _: dynamic -> self.currentContainer.getHeight() }
    proto.getMetrics = ppThisJs { self: dynamic, _: dynamic ->
        ppArray2Js(self.getWidth(), self.getHeight())
    }
    proto.toString = ppThisJs { self: dynamic, _: dynamic ->
        "[Page " + self.name + "/" + self.id + "]"
    }
    ppApi.prototype = proto
}

private fun ppInstallOther() {
    ppApi.isFullScreen = { true }
    ppApi.isLocked = { false }
    ppApi.visible = true
    ppApi.stack = js("[]")
    ppApi.supportResize = false

    ppApi.pushPage = ppThisJs { self: dynamic, args: dynamic ->
        ppOtherPushPage(self, args[0], args[1])
    }
    ppApi.popPage = ppThisJs { self: dynamic, args: dynamic -> ppOtherPopPage(self, args[0]) }
    ppApi.show = ppThisJs { self: dynamic, _: dynamic -> ppOtherShow(self) }
    ppApi.hide = ppThisJs { self: dynamic, _: dynamic -> ppOtherHide(self) }
    ppApi.dismiss = ppThisJs { self: dynamic, _: dynamic -> ppOtherDismissAll(self) }
    ppApi.reset = ppThisJs { self: dynamic, _: dynamic -> ppOtherReset(self) }
    ppApi.getCount = ppThisJs { self: dynamic, _: dynamic -> ppOtherGetCount(self) }
    ppApi.debug = ppThisJs { self: dynamic, _: dynamic -> ppOtherDebug(self) }
    ppApi.getActiveContainer = ppThisJs { self: dynamic, _: dynamic -> ppOtherGetActiveContainer(self) }

    val proto = js("({})")
    proto.init = ppThisJs { self: dynamic, _: dynamic -> ppOtherInit(self) }
    proto.enter = ppThisJs { self: dynamic, args: dynamic -> ppOtherEnter(self, args[0]) }
    proto.exit = ppThisJs { self: dynamic, args: dynamic -> ppOtherExit(self, args[0]) }
    proto.resizable = ppThisJs { self: dynamic, _: dynamic -> ppOtherResizable(self) }
    proto.dismiss = ppThisJs { self: dynamic, _: dynamic -> ppOtherDismiss(self) }
    proto.requestShow = ppThisJs { self: dynamic, _: dynamic -> ppOtherRequestShow(self) }
    proto.requestHide = ppThisJs { self: dynamic, _: dynamic -> ppOtherRequestHide(self) }
    proto.getWidth = ppThisJs { self: dynamic, _: dynamic -> ppOtherGetWidth(self) }
    proto.getHeight = ppThisJs { self: dynamic, _: dynamic -> ppOtherGetHeight(self) }
    proto.getMetrics = ppThisJs { self: dynamic, _: dynamic ->
        ppArray2Js(self.getWidth(), self.getHeight())
    }
    ppApi.prototype = proto
}

private fun ppEnter(self: dynamic, noAnimation: dynamic): dynamic {
    if (ppTruthyJs(self.showing)) return self
    self.showing = true
    self.mainView.setVisibility(js("G.View.VISIBLE"))
    ppApi.showPage(self)
    ppApi.pushPage(self.name, self)
    if (!ppTruthyJs(noAnimation) && ppTruthyJs(self._enterAnimation)) {
        self.currentAnimation = self._enterAnimation(self.mainView, {
            self.currentAnimation = js("null")
            ppApi.pageShown(self)
            js("undefined")
        })
    }
    return self
}

private fun ppExit(self: dynamic, noAnimation: dynamic): dynamic {
    if (!ppTruthyJs(self.showing)) return self
    ppApi.popPage(self)
    self.showing = false
    if (!ppTruthyJs(noAnimation) && ppTruthyJs(self._exitAnimation) && ppTruthyJs(self.visible())) {
        ppApi.addDisappearing(self)
        self.currentAnimation = self._exitAnimation(self.mainView, {
            self.currentAnimation = js("null")
            self.processHide()
            ppApi.removeDisappearing(self)
            js("undefined")
        })
    } else {
        self.processHide()
    }
    return self
}

private fun ppVisible(self: dynamic): dynamic {
    if (ppLooseNeJs(self.mainView.getVisibility(), js("G.View.VISIBLE"))) return false
    if (ppLooseEqJs(self.currentContainer, ppApi.floatContainer)) {
        return ppApi.floatVisible
    } else {
        return ppTruthyJs(ppApi.defaultVisible) && ppTruthyJs(ppApi.visible)
    }
}

private fun ppProcessHide(self: dynamic): dynamic {
    if (!ppTruthyJs(self.currentContainer)) return self
    ppApi.hidePage(self)
    ppApi.trigger("pageHide", self)
    return self
}

private fun ppRequestShow(self: dynamic): dynamic {
    if (ppTruthyJs(ppApi.debugPrint)) js("Log").d("Show " + self)
    self.mainView.setVisibility(js("G.View.VISIBLE"))
    return self
}

private fun ppRequestHide(self: dynamic): dynamic {
    if (ppTruthyJs(ppApi.debugPrint)) js("Log").d("Hide " + self)
    self.mainView.setVisibility(js("G.View.GONE"))
    return self
}

fun installPopupPage() {
    ppNextId = 0
    ppApi = ppMakeCtorJs { self: dynamic, mainView: dynamic, name: dynamic, modal: dynamic ->
        ppInit(self, mainView, name, modal)
        js("undefined")
    }

    if (js("MapScript").host == "Android") {
        ppInstallAndroid()
    } else {
        ppInstallOther()
    }

    ppApi.prototype.show = ppApi.prototype.enter
    ppApi.prototype.hide = ppApi.prototype.exit
    ppApi.prototype.enterAnimation = ppThisJs { self: dynamic, args: dynamic ->
        self._enterAnimation = args[0]
        self
    }
    ppApi.prototype.exitAnimation = ppThisJs { self: dynamic, args: dynamic ->
        self._exitAnimation = args[0]
        self
    }

    js("EventSender").init(ppApi.prototype)
    ppApi.listener = js("({})")
    ppApi.isBusy = ppThisJs { self: dynamic, _: dynamic -> self.busy }

    ppApi.showDialog = { name: dynamic, layout: dynamic, width: dynamic, height: dynamic, modal: dynamic ->
        ppShowDialog(name, layout, width, height, modal)
    }
    ppApi.showSideBar = { name: dynamic, layout: dynamic, direction: dynamic, offsetAdd: dynamic, offsetMul: dynamic, modal: dynamic ->
        ppShowSideBar(name, layout, direction, offsetAdd, offsetMul, modal)
    }

    ppApi.overlays = js("[]")
    ppApi.Overlay = ppMakeOverlayCtorJs(
        { self: dynamic, view: dynamic, width: dynamic, height: dynamic, gravity: dynamic, x: dynamic, y: dynamic ->
            ppOverlayInit(self, view, width, height, gravity, x, y)
            js("undefined")
        }
    )
    val overlayProto = js("({})")
    overlayProto.attach = ppThisJs { self: dynamic, args: dynamic -> ppOverlayAttach(self, args[0]) }
    overlayProto.detach = ppThisJs { self: dynamic, _: dynamic -> ppOverlayDetach(self) }
    overlayProto.update = ppThisJs { self: dynamic, args: dynamic -> ppOverlayUpdate(self, args[0]) }
    ppApi.Overlay.prototype = overlayProto

    ppApi.addOverlay = { overlay: dynamic ->
        overlay.update(true)
        ppApi.overlays.push(overlay)
        overlay
    }
    ppApi.removeOverlay = { overlay: dynamic ->
        val i = ppApi.overlays.indexOf(overlay)
        if (i >= 0) {
            overlay.detach()
            ppApi.overlays.splice(i, 1)
        }
        js("undefined")
    }
    ppApi.updateOverlays = {
        var i = 0
        while (i < ppLength(ppApi.overlays)) {
            ppApi.overlays[i].update()
            i++
        }
        js("undefined")
    }

    js("EventSender").init(ppApi)

    ppApi.fadeInAnimation = { v: dynamic, callback: dynamic -> ppFadeInAnimation(v, callback) }
    ppApi.fadeOutAnimation = { v: dynamic, callback: dynamic -> ppFadeOutAnimation(v, callback) }
    ppApi.dialogEnterAnimation = { param: dynamic, v: dynamic, callback: dynamic ->
        ppDialogEnterAnimation(param, v, callback)
    }
    ppApi.sideBarEnterAnimation = { param: dynamic, v: dynamic, callback: dynamic ->
        ppSideBarEnterAnimation(param, v, callback)
    }
    ppApi.sideBarExitAnimation = { param: dynamic, v: dynamic, callback: dynamic ->
        ppSideBarExitAnimation(param, v, callback)
    }

    ppApi.ViewAnimationController = ppMakeControllerCtorJs(
        { self: dynamic, v: dynamic, ani: dynamic ->
            self.views = js("[]")
            self.animations = js("[]")
            if (ppTruthyJs(v) || ppTruthyJs(ani)) self.addAnimation(v, ani)
            js("undefined")
        }
    )
    val controllerProto = js("({})")
    controllerProto.addAnimation = ppThisJs { self: dynamic, args: dynamic ->
        ppControllerAdd(self, args[0], args[1])
    }
    controllerProto.start = ppThisJs { self: dynamic, _: dynamic -> ppControllerStart(self) }
    controllerProto.cancel = ppThisJs { self: dynamic, _: dynamic -> ppControllerCancel(self) }
    ppApi.ViewAnimationController.prototype = controllerProto

    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("PopupPage", ppApi)
    }
}
