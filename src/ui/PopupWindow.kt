package ca.kotlin.ui

import ca.kotlin.core.*


/**
 * modules/uiCore/PopupWindow.js 的 Kotlin/JS 版本。
 *
 * 和原文件一样，模块本身是构造函数：new PopupWindow(mainView, name)，
 * 同时构造函数上挂着 showDialog / applyAttributes 等静态方法。
 * 原型方法与静态方法都依赖动态 this，因此统一用薄 JS wrapper 把真实
 * this / arguments 传回 Kotlin；逻辑仍在 Kotlin 侧。
 *
 * Android 分支和原文件中的“其他宿主”分支都保留。后者在原实现里
 * 就带着 popup / decorView 的旧路径与异常行为，这里按原样迁移，
 * 不额外修正。
 */
private var pwApi: dynamic = js("null")
private var pwNextId = 0

// ---------------- JS 语义 helper ----------------
private val pwTruthyJs: dynamic = js("function (v) { return !!v; }")
private val pwLengthJs: dynamic = js("function (v) { return v.length | 0; }")
private fun pwLength(v: dynamic): Int = pwLengthJs(v)
private val pwLooseEqJs: dynamic = js("function (a, b) { return a == b; }")
private fun pwLooseEq(a: dynamic, b: dynamic): Boolean = pwLooseEqJs(a, b)
private val pwTypeofJs: dynamic = js("function (v) { return typeof v; }")
private val pwApplyAttrsJs: dynamic = js(
    "function (t, s) { for (var i in t) { if (i in s) t[i] = s[i]; } }"
)
private val pwOrJs: dynamic = js("function (a, b) { return a | b; }")
private val pwAndJs: dynamic = js("function (a, b) { return a & b; }")
private val pwNotJs: dynamic = js("function (a) { return ~a; }")
private val pwThisJs: dynamic = js("function (f) { return function () { return f(this, arguments); }; }")
private val pwMakeCtorJs: dynamic = js(
    "function (init) { return function (mainView, name) { init(this, mainView, name); }; }"
)
private val pwNewJs: dynamic = js("function (f, a, b, c) { return new f(a, b, c); }")
private val pwArray2Js: dynamic = js("function (a, b) { return [a, b]; }")

// ---------------- Android UI helper ----------------
private val pwCreateFrameLayoutJs: dynamic = js(
    "function (cb) { return ScriptInterface.createFrameLayout(cb); }"
)
private val pwNewFrameLayoutJs: dynamic = js("function (c) { return new G.FrameLayout(c); }")
private val pwNewPopupWindowJs: dynamic = js("function (c) { return new G.PopupWindow(c); }")
private val pwNewTouchListenerJs: dynamic = js(
    "function (f) { return new G.View.OnTouchListener({ onTouch: f }); }"
)
private val pwNewDismissListenerJs: dynamic = js(
    "function (f) { return new G.PopupWindow.OnDismissListener({ onDismiss: f }); }"
)
private val pwNewFrameLayoutParamsJs: dynamic = js(
    "function (w, h) { return new G.FrameLayout.LayoutParams(w, h); }"
)
private val pwNewFrameLayoutParamsGravityJs: dynamic = js(
    "function (w, h, g) { return new G.FrameLayout.LayoutParams(w, h, g); }"
)
private val pwNewWindowManagerParamsJs: dynamic = js(
    "function () { return new G.WindowManager.LayoutParams(); }"
)
private val pwIsActivityJs: dynamic = js(
    "function (c) { return c instanceof android.app.Activity; }"
)
private val pwDpJs: dynamic = js("function (n) { return n * G.dp; }")
private fun pwDp(n: Int): dynamic = pwDpJs(n)
private val pwSdkJs: dynamic = js("function () { return ScriptInterface.getAndroidSdk(); }")
private val pwGravityStartTopJs: dynamic = js(
    "function () { return G.Gravity.START | G.Gravity.TOP; }"
)
private val pwLayoutParamJs: dynamic = js(
    "function (layout) { return { layout: layout }; }"
)

private fun pwInit(self: dynamic, mainView: dynamic, name: dynamic) {
    val currentId = pwNextId
    self.mainView = mainView
    self.name = if (pwTruthyJs(name)) name else "Unnamed@" + currentId
    self.id = currentId
    pwNextId = currentId + 1
    self.listener = js("({})")
    val attributes = js("({})")
    attributes.x = 0
    attributes.y = 0
    attributes.height = -2
    attributes.width = -2
    attributes.gravity = pwGravityStartTopJs()
    attributes.touchable = true
    attributes.focusable = true
    attributes.modal = false
    attributes.needIME = js("undefined")
    attributes.outsideTouch = false
    self.attributes = attributes
    self.init()
}

// ---------------- Android 分支 ----------------

private fun pwAndroidInit(self: dynamic) {
    val callbacks = js("({})")
    callbacks.dispatchKeyEvent = { event: dynamic, thisObj: dynamic ->
        val state = thisObj.getKeyDispatcherState()
        if (pwLooseEqJs(event.getKeyCode(), event.KEYCODE_BACK)) {
            if (!pwTruthyJs(state)) {
                0
            } else if (pwLooseEqJs(event.getAction(), event.ACTION_DOWN) && pwLooseEqJs(event.getRepeatCount(), 0)) {
                state.startTracking(event, thisObj)
                1
            } else if (pwLooseEqJs(event.getAction(), event.ACTION_UP)) {
                if (pwTruthyJs(state.isTracking(event)) && !pwTruthyJs(event.isCanceled())) {
                    if (!pwTruthyJs(self.attributes.modal)) self.hide()
                    1
                } else {
                    0
                }
            } else {
                0
            }
        } else {
            0
        }
    }
    callbacks.dispatchTouchEvent = { e: dynamic, _: dynamic ->
        var consumed = false
        self.trigger("touch", e, {
            consumed = true
            js("undefined")
        })
        if (consumed) 1 else 0
    }
    self.decorView = pwCreateFrameLayoutJs(callbacks)
    self.decorView.setContentDescription("PopupWindow@" + self.name + "/" + self.id)
    js("undefined")
}

private fun pwShow(self: dynamic, attr: dynamic): dynamic {
    if (pwTruthyJs(self.showing)) return self
    self.trigger("show")
    self.showing = true
    if (pwTruthyJs(attr)) self.attr(attr)
    val width = if (pwLooseEqJs(self.attributes.width, -2)) -2 else -1
    val height = if (pwLooseEqJs(self.attributes.height, -2)) -2 else -1
    self.decorView.addView(self.mainView, pwNewFrameLayoutParamsJs(width, height))
    pwApi.showView(self.decorView, self.attributes)
    return self
}

private fun pwHide(self: dynamic): dynamic {
    if (!pwTruthyJs(self.showing)) return self
    self.trigger("hide")
    self.showing = false
    pwApi.hideView(self.decorView)
    self.decorView.removeView(self.mainView)
    return self
}

private fun pwUpdate(self: dynamic, attr: dynamic): dynamic {
    if (pwTruthyJs(attr)) self.attr(attr)
    if (pwTruthyJs(self.showing)) {
        pwApi.updateView(self.decorView, self.attributes)
    }
    return self
}

private fun pwBringToFront(self: dynamic): dynamic {
    if (pwTruthyJs(self.showing)) {
        pwApi.bringToFront(self.decorView)
    }
    return js("undefined")
}

private fun pwIsVisible(self: dynamic): dynamic =
    pwLooseEqJs(self.decorView.getVisibility(), js("G.View.VISIBLE"))

private fun pwSetVisibility(self: dynamic, visible: dynamic): dynamic {
    self.decorView.setVisibility(if (pwTruthyJs(visible)) js("G.View.VISIBLE") else js("G.View.GONE"))
    return js("undefined")
}

private fun pwGetWidth(self: dynamic): dynamic = self.mainView.getWidth()

private fun pwGetHeight(self: dynamic): dynamic = self.mainView.getHeight()

private fun pwGetMetrics(self: dynamic): dynamic = pwArray2Js(self.getWidth(), self.getHeight())

private fun pwBuildLayoutParams(view: dynamic, attributes: dynamic): dynamic {
    var p = view.getLayoutParams()
    if (!pwTruthyJs(p)) p = pwNewWindowManagerParamsJs()
    val title = view.getContentDescription()
    p.gravity = attributes.gravity
    p.flags = pwApi.computeFlags(attributes, p.flags)
    p.type = if (pwTruthyJs(js("G.supportFloat"))) {
        if (pwSdkJs() >= 26) {
            js("G.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY")
        } else {
            js("G.WindowManager.LayoutParams.TYPE_PHONE")
        }
    } else {
        js("G.WindowManager.LayoutParams.TYPE_APPLICATION_PANEL")
    }
    if (pwIsActivityJs(js("ctx"))) {
        p.token = js("ctx").getWindow().getDecorView().getWindowToken()
    }
    p.format = js("G.PixelFormat.TRANSLUCENT")
    p.height = attributes.height
    p.width = attributes.width
    p.x = attributes.x
    p.y = attributes.y
    if (pwTruthyJs(title)) p.setTitle(title)
    return p
}

private fun pwComputeFlags(w: dynamic, f: dynamic): dynamic {
    val c = js("G.WindowManager.LayoutParams")
    val clearMask = pwOrJs(
        pwOrJs(c.FLAG_NOT_FOCUSABLE, c.FLAG_NOT_TOUCHABLE),
        pwOrJs(c.FLAG_WATCH_OUTSIDE_TOUCH, c.FLAG_ALT_FOCUSABLE_IM)
    )
    var flags = pwAndJs(f, pwNotJs(clearMask))
    if (!pwTruthyJs(w.touchable)) flags = pwOrJs(flags, c.FLAG_NOT_TOUCHABLE)
    if (!pwTruthyJs(w.focusable)) {
        flags = pwOrJs(flags, c.FLAG_NOT_FOCUSABLE)
        if (pwTruthyJs(w.needIME)) flags = pwOrJs(flags, c.FLAG_ALT_FOCUSABLE_IM)
    } else {
        if (pwLooseEqJs(w.needIME, false)) flags = pwOrJs(flags, c.FLAG_ALT_FOCUSABLE_IM)
    }
    if (pwTruthyJs(w.outsideTouch)) flags = pwOrJs(flags, c.FLAG_WATCH_OUTSIDE_TOUCH)
    return flags
}

private fun pwShowView(self: dynamic, view: dynamic, attributes: dynamic): dynamic {
    try {
        js("PWM").wm.addView(view, self.buildLayoutParams(view, attributes))
        return true
    } catch (e: dynamic) {
        js("erp")(e, true)
    }
    return false
}

private fun pwHideView(view: dynamic): dynamic {
    try {
        js("PWM").wm.removeViewImmediate(view)
        return true
    } catch (e: dynamic) {
        js("erp")(e, true)
    }
    return false
}

private fun pwBringToFrontView(view: dynamic): dynamic {
    try {
        js("PWM").wm.removeViewImmediate(view)
        js("PWM").wm.addView(view, view.getLayoutParams())
        return true
    } catch (e: dynamic) {
        js("erp")(e, true)
    }
    return false
}

private fun pwUpdateView(self: dynamic, view: dynamic, attributes: dynamic): dynamic {
    try {
        js("PWM").wm.updateViewLayout(view, self.buildLayoutParams(view, attributes))
        return true
    } catch (e: dynamic) {
        js("erp")(e, true)
    }
    return false
}

// ---------------- 其他宿主分支（原实现“有很大问题”，按原样保留） ----------------

private fun pwOtherInit(self: dynamic) {
    self.popupWnd = pwNewPopupWindowJs(js("ctx"))
    self.popup.setOnDismissListener(pwNewDismissListenerJs({
        try {
            self.trigger("hide")
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }))
    js("undefined")
}

private fun pwOtherShow(self: dynamic, attr: dynamic): dynamic {
    if (pwTruthyJs(attr)) self.attr(attr)
    pwApi.configueWnd(self.popupWnd, self.attributes)
    self.popupWnd.showAtLocation(
        js("ctx").getWindow().getDecorView(),
        self.attributes.gravity,
        self.attributes.x,
        self.attributes.y
    )
    return self
}

private fun pwOtherHide(self: dynamic): dynamic {
    self.popupWnd.dismiss()
    return self
}

private fun pwOtherUpdate(self: dynamic, attr: dynamic): dynamic {
    if (pwTruthyJs(attr)) self.attr(attr)
    if (pwTruthyJs(self.showing)) {
        pwApi.updateView(self.decorView, self.attributes)
    }
    return self
}

private fun pwOtherBringToFront(self: dynamic): dynamic {
    if (!pwTruthyJs(self.popupWnd.isShowing())) return js("undefined")
    var v = self.popupWnd.getContentView()
    if (!pwTruthyJs(self.popupWnd)) return js("undefined")
    v = v.getRootView()
    val wp = v.getLayoutParams()
    js("PWM").wm.removeViewImmediate(v)
    js("PWM").wm.addView(v, wp)
    return js("undefined")
}

private fun pwOtherIsVisible(self: dynamic): dynamic {
    self.popupWnd.getContentView()
    return pwLooseEqJs(self.decorView.getVisibility(), js("G.View.VISIBLE"))
}

private fun pwOtherSetVisibility(self: dynamic, visible: dynamic): dynamic {
    self.decorView.setVisibility(if (pwTruthyJs(visible)) js("G.View.VISIBLE") else js("G.View.GONE"))
    return js("undefined")
}

private fun pwOtherGetWidth(self: dynamic): dynamic = self.mainView.getWidth()

private fun pwOtherGetHeight(self: dynamic): dynamic = self.mainView.getHeight()

private fun pwOtherGetMetrics(self: dynamic): dynamic = pwArray2Js(self.getWidth(), self.getHeight())

private fun pwConfigueWnd(popup: dynamic, attrs: dynamic): dynamic {
    popup.setWindowLayoutType(
        if (pwTruthyJs(js("G.supportFloat"))) {
            if (pwSdkJs() >= 26) {
                js("G.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY")
            } else {
                js("G.WindowManager.LayoutParams.TYPE_PHONE")
            }
        } else {
            js("G.WindowManager.LayoutParams.TYPE_APPLICATION_PANEL")
        }
    )
    popup.setHeight(attrs.height)
    popup.setWidth(attrs.width)
    popup.setTouchable(attrs.touchable)
    popup.setFocusable(attrs.focusable)
    popup.setOutsideTouchable(attrs.outsideTouch)
    popup.setInputMethodMode(
        if (pwTruthyJs(attrs.needIME)) {
            popup.INPUT_METHOD_NEEDED
        } else if (pwLooseEqJs(attrs.needIME, false)) {
            popup.INPUT_METHOD_NOT_NEEDED
        } else {
            popup.INPUT_METHOD_FROM_FOCUSABLE
        }
    )
    return js("undefined")
}

// ---------------- 公共原型 / 静态方法 ----------------

private fun pwApplyAttributes(target: dynamic, source: dynamic): dynamic {
    pwApplyAttrsJs(target, source)
    return js("undefined")
}

private fun pwAttr(self: dynamic, args: dynamic): dynamic {
    val argc = pwLength(args)
    if (argc == 1) {
        val name = args[0]
        if (pwTypeofJs(name) == "object") {
            pwApi.applyAttributes(self.attributes, name)
            return js("undefined")
        }
        return self.attributes[name]
    } else if (argc > 1) {
        return pwLooseEqJs(self.attributes[args[0]], args[1])
    }
    return js("undefined")
}

private fun pwToString(self: dynamic): dynamic =
    "[PopupWindow " + self.name + "/" + self.id + "]"

private fun pwShowDialog(
    name: dynamic,
    layout: dynamic,
    width: dynamic,
    height: dynamic,
    modal: dynamic
): dynamic {
    var popup: dynamic = js("null")
    val frame = pwNewFrameLayoutJs(js("ctx"))
    frame.setBackgroundColor(js("Common").argbInt(0x80, 0, 0, 0))
    frame.setOnTouchListener(pwNewTouchListenerJs({ v: dynamic, e: dynamic ->
        try {
            if (pwLooseEqJs(e.getAction(), e.ACTION_DOWN) && !pwTruthyJs(modal)) {
                if (e.getX() < layout.getLeft() || e.getX() >= layout.getRight() ||
                    e.getY() < layout.getTop() || e.getY() >= layout.getBottom()
                ) {
                    popup.hide()
                }
            }
            true
        } catch (err: dynamic) {
            js("erp")(err)
            true
        }
    }))
    layout.setLayoutParams(pwNewFrameLayoutParamsGravityJs(width, height, js("G.Gravity.CENTER")))
    layout.getLayoutParams().setMargins(pwDp(20), pwDp(20), pwDp(20), pwDp(20))
    frame.addView(layout)
    if (js("G.style") == "Material") layout.setElevation(pwDp(16))
    popup = pwNewJs(pwApi, frame, name, js("undefined"))
    js("PopupPage").dialogEnterAnimation(pwLayoutParamJs(layout), frame, js("null"))
    val attrs = js("({})")
    attrs.height = -1
    attrs.width = -1
    attrs.modal = modal
    popup.show(attrs)
    return popup
}

private fun pwInstallAndroid() {
    val proto = js("({})")
    proto.init = pwThisJs { self: dynamic, _: dynamic -> pwAndroidInit(self) }
    proto.show = pwThisJs { self: dynamic, args: dynamic -> pwShow(self, args[0]) }
    proto.hide = pwThisJs { self: dynamic, _: dynamic -> pwHide(self) }
    proto.update = pwThisJs { self: dynamic, args: dynamic -> pwUpdate(self, args[0]) }
    proto.bringToFront = pwThisJs { self: dynamic, _: dynamic -> pwBringToFront(self) }
    proto.isVisible = pwThisJs { self: dynamic, _: dynamic -> pwIsVisible(self) }
    proto.setVisibility = pwThisJs { self: dynamic, args: dynamic -> pwSetVisibility(self, args[0]) }
    proto.getWidth = pwThisJs { self: dynamic, _: dynamic -> pwGetWidth(self) }
    proto.getHeight = pwThisJs { self: dynamic, _: dynamic -> pwGetHeight(self) }
    proto.getMetrics = pwThisJs { self: dynamic, _: dynamic -> pwGetMetrics(self) }
    pwApi.prototype = proto

    pwApi.buildLayoutParams = { view: dynamic, attributes: dynamic ->
        pwBuildLayoutParams(view, attributes)
    }
    pwApi.computeFlags = { w: dynamic, f: dynamic -> pwComputeFlags(w, f) }
    pwApi.showView = pwThisJs { self: dynamic, args: dynamic ->
        pwShowView(self, args[0], args[1])
    }
    pwApi.hideView = { view: dynamic -> pwHideView(view) }
    pwApi.bringToFront = { view: dynamic -> pwBringToFrontView(view) }
    pwApi.updateView = pwThisJs { self: dynamic, args: dynamic ->
        pwUpdateView(self, args[0], args[1])
    }
}

private fun pwInstallOther() {
    val proto = js("({})")
    proto.init = pwThisJs { self: dynamic, _: dynamic -> pwOtherInit(self) }
    proto.show = pwThisJs { self: dynamic, args: dynamic -> pwOtherShow(self, args[0]) }
    proto.hide = pwThisJs { self: dynamic, _: dynamic -> pwOtherHide(self) }
    proto.update = pwThisJs { self: dynamic, args: dynamic -> pwOtherUpdate(self, args[0]) }
    proto.bringToFront = pwThisJs { self: dynamic, _: dynamic -> pwOtherBringToFront(self) }
    proto.isVisible = pwThisJs { self: dynamic, _: dynamic -> pwOtherIsVisible(self) }
    proto.setVisibility = pwThisJs { self: dynamic, args: dynamic -> pwOtherSetVisibility(self, args[0]) }
    proto.getWidth = pwThisJs { self: dynamic, _: dynamic -> pwOtherGetWidth(self) }
    proto.getHeight = pwThisJs { self: dynamic, _: dynamic -> pwOtherGetHeight(self) }
    proto.getMetrics = pwThisJs { self: dynamic, _: dynamic -> pwOtherGetMetrics(self) }
    pwApi.prototype = proto

    pwApi.configueWnd = { popup: dynamic, attrs: dynamic -> pwConfigueWnd(popup, attrs) }
}

fun installPopupWindow() {
    pwNextId = 0
    pwApi = pwMakeCtorJs { self: dynamic, mainView: dynamic, name: dynamic ->
        pwInit(self, mainView, name)
        js("undefined")
    }

    if (js("MapScript").host == "Android") {
        pwInstallAndroid()
    } else {
        pwInstallOther()
    }

    pwApi.applyAttributes = { target: dynamic, source: dynamic -> pwApplyAttributes(target, source) }
    pwApi.prototype.attr = pwThisJs { self: dynamic, args: dynamic -> pwAttr(self, args) }
    pwApi.prototype.toString = pwThisJs { self: dynamic, _: dynamic -> pwToString(self) }

    js("EventSender").init(pwApi.prototype)
    pwApi.showDialog = { name: dynamic, layout: dynamic, width: dynamic, height: dynamic, modal: dynamic ->
        pwShowDialog(name, layout, width, height, modal)
    }

    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("PopupWindow", pwApi)
    }
}
