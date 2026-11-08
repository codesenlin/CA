package ca.kotlin

/**
 * modules/CA.js icon 组的 Kotlin/JS 迁移（原文件第 314-601 行，以成员名为准）。
 *
 * 只提供 installCaIcon(api)：把 showIcon / hideIcon / quickBarDefaultActions
 * 挂到调用方传入的 CA 共享 API 上；不注册模块、不定义全局 caApi、也不调用安装函数。
 *
 * 迁移约定：
 *  - 原实现里的 this 改成显式 api；跨组成员（settings / Icon / customIcon /
 *    showActions / showMain / trySave 等）只通过 api 动态解析。
 *  - showIcon 是具名函数表达式 `function self()`，界面状态（view / icon / longClicked /
 *    cx / cy / scrWidth / scrHeight / longClick / open / refresh / ...）挂在函数对象
 *    本身；这里用薄 JS 工厂保留同一个 self 语义与 arity 0，其它组通过
 *    CA.showIcon.xxx 以及 PWM.registerResetFlag 的访问保持有效。
 *  - OnTouchListener 里的 `function touch(v, e)` 同样把 stead / offx / offy / lx / ly
 *    存在函数对象上，也用工厂保留。
 *  - 原实现的既有行为/缺陷按原样保留，并在对应位置加注释：
 *      * OnTouch 的 switch 里 ACTION_UP 没有 break，会贯穿到 ACTION_CANCEL 的收尾逻辑；
 *      * animateTranslation 的 delay 参数从未使用；
 *      * self.iconUpdate 判断的是 CA.icon 而不是 self.icon；
 *      * showIcon 末尾的 `//if (self.tutor) self.tutor();` 仍是注释状态，教程入口未启用；
 *      * PWM.registerResetFlag(CA, "icon") / PWM.registerResetFlag(self, "view") 原样保留。
 *  - JS 的 truthy / 宽松相等 / 算术语义走带参 JS helper；js("...") 里不引用 Kotlin 局部变量。
 */

// ---------------- JS 语义 helper ----------------

private val ciTruthyJs: dynamic = js("function (v) { return !!v; }")
private fun ciTruthy(v: dynamic): Boolean = ciTruthyJs(v)

private val ciLooseEqJs: dynamic = js("function (a, b) { return a == b; }")
private fun ciLooseEq(a: dynamic, b: dynamic): Boolean = ciLooseEqJs(a, b)

private val ciLtJs: dynamic = js("function (a, b) { return a < b; }")
private fun ciLt(a: dynamic, b: dynamic): Boolean = ciLtJs(a, b)

private val ciGtJs: dynamic = js("function (a, b) { return a > b; }")
private fun ciGt(a: dynamic, b: dynamic): Boolean = ciGtJs(a, b)

private val ciAddJs: dynamic = js("function (a, b) { return a + b; }")
private fun ciAdd(a: dynamic, b: dynamic): dynamic = ciAddJs(a, b)

private val ciSubJs: dynamic = js("function (a, b) { return a - b; }")
private fun ciSub(a: dynamic, b: dynamic): dynamic = ciSubJs(a, b)

private val ciMulJs: dynamic = js("function (a, b) { return a * b; }")
private fun ciMul(a: dynamic, b: dynamic): dynamic = ciMulJs(a, b)

private val ciDivJs: dynamic = js("function (a, b) { return a / b; }")
private fun ciDiv(a: dynamic, b: dynamic): dynamic = ciDivJs(a, b)

private val ciAbsJs: dynamic = js("function (v) { return Math.abs(v); }")
private fun ciAbs(v: dynamic): dynamic = ciAbsJs(v)

private val ciFloorJs: dynamic = js("function (v) { return Math.floor(v); }")
private fun ciFloor(v: dynamic): dynamic = ciFloorJs(v)

private val ciCeilJs: dynamic = js("function (v) { return Math.ceil(v); }")
private fun ciCeil(v: dynamic): dynamic = ciCeilJs(v)

private val ciIsNaNRawJs: dynamic = js("function (v) { return isNaN(v); }")
private fun ciIsNaN(v: dynamic): Boolean = ciIsNaNRawJs(v)

private val ciParseFloatJs: dynamic = js("function (v) { return parseFloat(v); }")
private fun ciParseFloat(v: dynamic): dynamic = ciParseFloatJs(v)

private val ciInJs: dynamic = js("function (k, o) { return k in o; }")
private fun ciIn(k: dynamic, o: dynamic): Boolean = ciInJs(k, o)

// ---------------- 动态对象 helper ----------------

private fun ciObject(): dynamic = js("({})")

private fun ciArray(vararg items: dynamic): dynamic {
    val a = js("[]")
    var i = 0
    while (i < items.size) {
        a.push(items[i])
        i++
    }
    return a
}

private fun ciActionItem(action: String): dynamic {
    val o = ciObject()
    o.action = action
    return o
}

// ---------------- 具名函数工厂 ----------------

/** function self()：状态挂在 showIcon 函数对象上，arity 0。 */
private val ciSelf0Js: dynamic = js("function (f) { return function self() { return f(self); }; }")

/** function touch(v, e)：状态挂在 OnTouchListener 的函数对象上。 */
private val ciTouchJs: dynamic = js(
    "function (f) { var self = function touch(v, e) { return f(self, v, e); }; return self; }"
)

/**
 * 原 animateToPos 里的 `var updater = new java.lang.Runnable({run: ...})`：
 * run 内部通过闭包再次 gHandler.post(updater) 自己。这里把同一个 Runnable
 * 回传给 Kotlin 实现，保持原来的自引用时序。
 */
private val ciSelfRunnableJs: dynamic = js(
    "function (f) { var r = new java.lang.Runnable({ run: function () { f(r); } }); return r; }"
)

// ---------------- Android / UI helper ----------------

private val ciNewFrameLayoutJs: dynamic = js("function () { return new G.FrameLayout(ctx); }")
private val ciNewClickListenerJs: dynamic = js(
    "function (f) { return new G.View.OnClickListener({ onClick: f }); }"
)
private val ciNewTouchListenerJs: dynamic = js(
    "function (f) { return new G.View.OnTouchListener({ onTouch: f }); }"
)
private val ciNewLayoutChangeListenerJs: dynamic = js(
    "function (f) { return new G.View.OnLayoutChangeListener({ onLayoutChange: f }); }"
)
private val ciNewRunnableJs: dynamic = js(
    "function (f) { return new java.lang.Runnable({ run: f }); }"
)
private val ciNewTranslate4Js: dynamic = js(
    "function (a, b, c, d) { return new G.TranslateAnimation(a, b, c, d); }"
)
private val ciNewAccelerateJs: dynamic = js(
    "function (v) { return new G.AccelerateInterpolator(v); }"
)
private val ciNewPopupWindowJs: dynamic = js(
    "function (v, n) { return new PopupWindow(v, n); }"
)
private val ciNewValueAnimatorOfIntJs: dynamic = js(
    "function (a) { return G.ValueAnimator.ofInt(a); }"
)
private val ciViewConfigJs: dynamic = js("function () { return G.ViewConfiguration.get(ctx); }")

private fun ciNewFrameLayout(): dynamic = ciNewFrameLayoutJs()
private fun ciNewClickListener(f: dynamic): dynamic = ciNewClickListenerJs(f)
private fun ciNewTouchListener(f: dynamic): dynamic = ciNewTouchListenerJs(f)
private fun ciNewLayoutChangeListener(f: dynamic): dynamic = ciNewLayoutChangeListenerJs(f)
private fun ciNewRunnable(f: dynamic): dynamic = ciNewRunnableJs(f)
private fun ciNewTranslate4(a: dynamic, b: dynamic, c: dynamic, d: dynamic): dynamic =
    ciNewTranslate4Js(a, b, c, d)
private fun ciNewAccelerate(v: dynamic): dynamic = ciNewAccelerateJs(v)
private fun ciNewPopupWindow(view: dynamic, name: dynamic): dynamic = ciNewPopupWindowJs(view, name)
private fun ciNewValueAnimatorOfInt(values: dynamic): dynamic = ciNewValueAnimatorOfIntJs(values)
private fun ciViewConfig(): dynamic = ciViewConfigJs()

// ---------------- showIcon 的 self 方法 ----------------

/** 图标点击：原实现里这个 onClick 是自执行时定义的闭包，self.open 在稍后才赋值。 */
private fun caIconClick(self: dynamic, api: dynamic): dynamic {
    try {
        if (ciIsNaN(api.settings.iiMode) || ciLt(api.settings.iiMode, 0)) {
            api.settings.iiMode = 3
        }
        self.open()
    } catch (e: dynamic) {
        js("erp")(e)
    }
    return js("undefined")
}

private fun caIconLayoutChange(self: dynamic): dynamic {
    try {
        self.layoutChanged(true)
    } catch (e: dynamic) {
        js("erp")(e)
    }
    return js("undefined")
}

/**
 * 原 OnTouchListener 的 onTouch。
 *
 * 注意原 switch 的 ACTION_UP 分支没有 break，会继续执行 ACTION_CANCEL 的收尾
 * （layoutChanged / refreshPos / 保存 iconX/iconY / 清 longClicked），这里按顺序保留。
 */
private fun caIconTouch(
    self: dynamic,
    api: dynamic,
    touch: dynamic,
    v: dynamic,
    e: dynamic,
    longPressTimeout: dynamic,
    touchSlop: dynamic
): dynamic {
    try {
        val action = e.getAction()
        if (ciLooseEq(action, e.ACTION_MOVE)) {
            var dragging = true
            if (ciTruthy(touch.stead)) {
                // 原 switch 里的 break：手指按下后位移还没超过 touchSlop 时，
                // 直接结束本次 ACTION_MOVE 的处理，不更新拖动坐标。
                if (ciLt(
                        ciAdd(
                            ciAbs(ciSub(touch.lx, e.getRawX())),
                            ciAbs(ciSub(touch.ly, e.getRawY()))
                        ),
                        touchSlop
                    )
                ) {
                    dragging = false
                } else {
                    self.longClicked = false
                    touch.stead = false
                    self.animateTranslation(0)
                }
            }
            // 原实现 iconDragMode == 2 时直接 break，不拖动图标。
            if (dragging && !ciLooseEq(api.settings.iconDragMode, 2)) {
                // 原写法 `CA.icon.attributes.x = self.cx = e.getRawX() + touch.offx`：
                // 先解析 LHS 引用（CA.icon 为空时在这里就抛错），再算 RHS 写 self.cx，
                // 最后写 attributes.x。
                val attributes = api.icon.attributes
                self.cx = ciAdd(e.getRawX(), touch.offx)
                attributes.x = self.cx
                self.cy = ciAdd(e.getRawY(), touch.offy)
                attributes.y = self.cy
                api.icon.update()
            }
        } else if (ciLooseEq(action, e.ACTION_DOWN)) {
            touch.lx = e.getRawX()
            touch.offx = ciSub(self.cx, touch.lx)
            touch.ly = e.getRawY()
            touch.offy = ciSub(self.cy, touch.ly)
            touch.stead = true
            v.postDelayed(self.longClick, longPressTimeout)
            self.longClicked = true
            self.cancelAnimator()
            self.layoutChanged()
            return true
        } else if (ciLooseEq(action, e.ACTION_UP) || ciLooseEq(action, e.ACTION_CANCEL)) {
            if (ciLooseEq(action, e.ACTION_UP) && ciTruthy(touch.stead)) {
                if (ciLt(ciSub(e.getEventTime(), e.getDownTime()), longPressTimeout)) {
                    v.performClick()
                }
            }
            self.layoutChanged()
            self.refreshPos()
            api.settings.iconX = ciFloor(self.cx)
            api.settings.iconY = ciFloor(self.cy)
            self.longClicked = false
        }
        self.icon.dispatchTouchEvent(e)
        return true
    } catch (err: dynamic) {
        // 原实现的 `catch (e) { return erp(e), true }`：记录错误后仍然返回 true。
        js("erp")(err)
        return true
    }
}

/** 长按 Runnable 的 run：未拖动时打开快捷栏，或切换悬浮窗全屏。 */
private fun caIconLongClick(self: dynamic, api: dynamic) {
    try {
        if (ciTruthy(self.longClicked)) {
            if (ciLooseEq(js("PopupPage").getCount(), 0) || !ciTruthy(js("PopupPage").visible)) {
                api.showActions(api.settings.quickBarActions)
            } else if (ciTruthy(js("PopupPage").supportResize)) {
                if (ciTruthy(js("PopupPage").isLocked())) {
                    js("PopupPage").setFullScreen(js("PopupPage").isFullScreen(), false)
                } else {
                    js("PopupPage").setFullScreen(!ciTruthy(js("PopupPage").isFullScreen()), false)
                }
            }
        }
        self.longClicked = false
    } catch (e: dynamic) {
        js("erp")(e)
    }
}

private fun caIconUpdateScreenInfo(self: dynamic): Boolean {
    val lw = self.scrWidth
    val lh = self.scrHeight
    val metrics = js("Common").getMetrics()
    self.scrWidth = metrics[0]
    self.scrHeight = metrics[1]
    return !ciLooseEq(lw, self.scrWidth) || !ciLooseEq(lh, self.scrHeight)
}

private fun caIconLayoutChanged(self: dynamic, api: dynamic, updateIcon: dynamic) {
    // 原写法 `var smChanged = updateIcon && self.updateScreenInfo();`：
    // updateIcon 为假时 smChanged 就是那个假值，后面只按 truthy 判断。
    var smChanged: dynamic = js("undefined")
    if (ciTruthy(updateIcon)) smChanged = caIconUpdateScreenInfo(self)
    if (ciLt(self.cx, 0)) self.cx = 0
    if (ciLt(self.cy, 0)) self.cy = 0
    if (ciGt(self.cx, self.scrWidth)) self.cx = self.scrWidth
    if (ciGt(self.cy, self.scrHeight)) self.cy = self.scrHeight
    if (ciTruthy(smChanged)) {
        self.icon.setTranslationX(0)
        self.refreshPos()
    }
}

private fun caIconAnimatorRun(
    self: dynamic,
    api: dynamic,
    xani: dynamic,
    yani: dynamic,
    callback: dynamic,
    updater: dynamic
): dynamic {
    try {
        if (!ciTruthy(api.icon)) return js("undefined")
        // 原写法 `CA.icon.attributes.x = self.cx = xani.getAnimatedValue()`：
        // 先解析 LHS 引用，再取动画值写 self.cx，最后写 attributes.x。
        val attributes = api.icon.attributes
        self.cx = xani.getAnimatedValue()
        attributes.x = self.cx
        self.cy = yani.getAnimatedValue()
        attributes.y = self.cy
        api.icon.update()
        if (!ciTruthy(xani.isRunning())) {
            if (ciTruthy(callback)) callback()
            return js("undefined")
        }
        js("gHandler").post(updater)
    } catch (e: dynamic) {
        js("erp")(e)
    }
    return js("undefined")
}

private fun caIconAnimateToPos(
    self: dynamic,
    api: dynamic,
    x: dynamic,
    y: dynamic,
    dur: dynamic,
    interpolator: dynamic,
    callback: dynamic
) {
    if (!ciTruthy(api.icon)) return
    self.cancelAnimator()
    val xani = ciNewValueAnimatorOfInt(ciArray(self.cx, x))
    val yani = ciNewValueAnimatorOfInt(ciArray(self.cy, y))
    self.xanimator = xani
    self.yanimator = yani
    xani.setDuration(dur)
    yani.setDuration(dur)
    if (ciTruthy(interpolator)) {
        xani.setInterpolator(interpolator)
        yani.setInterpolator(interpolator)
    }
    val updater = ciSelfRunnableJs { selfRunnable: dynamic ->
        caIconAnimatorRun(self, api, xani, yani, callback, selfRunnable)
    }
    xani.start()
    yani.start()
    js("gHandler").post(updater)
}

private fun caIconCancelAnimator(self: dynamic) {
    if (ciTruthy(self.xanimator)) {
        self.xanimator.cancel()
        self.yanimator.cancel()
        self.xanimator = js("null")
        self.yanimator = js("null")
    }
}

/** 原参数表是 (offset, delay)，其中 delay 从未使用，按原样保留参数名与 arity。 */
private fun caIconAnimateTranslation(self: dynamic, offset: dynamic, delay: dynamic) {
    if (ciLooseEq(offset, self.icon.getTranslationX())) return
    val animation = ciNewTranslate4(ciSub(self.icon.getTranslationX(), offset), 0, 0, 0)
    animation.setDuration(100)
    self.icon.setTranslationX(offset)
    self.icon.startAnimation(animation)
}

private fun caIconOpen(self: dynamic, api: dynamic) {
    if (ciGt(js("PopupPage").getCount(), 0)) {
        if (ciTruthy(js("PopupPage").visible)) {
            js("PopupPage").hide()
        } else {
            js("PopupPage").show()
        }
    } else {
        api.showMain(api.settings.noAnimation)
    }
    js("PushService").notify()
}

private fun caIconRefreshAlpha(self: dynamic, api: dynamic) {
    if (ciTruthy(api.settings.iconAlpha)) {
        self.view.setAlpha(ciDiv(api.settings.iconAlpha, 10))
    } else {
        val alpha = if (ciTruthy(js("PopupPage").visible) &&
            ciGt(js("PopupPage").getCount(), 0) &&
            ciTruthy(js("PopupPage").isFullScreen())
        ) 0.3 else 0.7
        self.view.setAlpha(alpha)
    }
}

private fun caIconRefreshPos(self: dynamic, api: dynamic) {
    if (ciLooseEq(api.settings.iconDragMode, 1)) {
        if (ciGt(ciMul(self.cx, 2), self.scrWidth)) {
            self.animateToPos(self.scrWidth, self.cy, 150, ciNewAccelerate(2.0), { ->
                self.animateTranslation(ciMul(0.6, self.view.getMeasuredWidth()))
                js("undefined")
            })
        } else {
            self.animateToPos(0, self.cy, 150, ciNewAccelerate(2.0), { ->
                self.animateTranslation(ciMul(-0.6, self.view.getMeasuredWidth()))
                js("undefined")
            })
        }
    } else {
        self.animateTranslation(0)
    }
}

private fun caIconRefreshIcon(self: dynamic, api: dynamic) {
    var iconSize = ciParseFloat(api.settings.iconSize)
    if (!ciGt(iconSize, 0)) {
        api.settings.iconSize = 1
        iconSize = 1
    }
    val icon = api.settings.icon
    self.icon = if (ciIn(icon, api.Icon)) {
        val maker = api.Icon[icon]
        maker(iconSize, false)
    } else {
        api.customIcon(icon, iconSize)
    }
    self.view.removeAllViews()
    self.view.addView(self.icon)
    caIconRefreshAlpha(self, api)
}

private fun caIconRefresh(self: dynamic, api: dynamic) {
    caIconRefreshIcon(self, api)
    caIconRefreshAlpha(self, api)
    caIconRefreshPos(self, api)
}

/** self.iconUpdate 里判断的是 CA.icon（不是 self.icon），按原样保留。 */
private fun caIconIconUpdate(self: dynamic, api: dynamic) {
    js("gHandler").post({
        caIconIconUpdateUi(self, api)
        js("undefined")
    })
}

private fun caIconIconUpdateUi(self: dynamic, api: dynamic) {
    try {
        if (!ciTruthy(api.icon)) return
        caIconRefreshAlpha(self, api)
    } catch (e: dynamic) {
        js("erp")(e)
    }
}

private fun caIconTutor(self: dynamic, api: dynamic) {
    val off = ciArray(self.cx, self.cy)
    val t1 = ciObject()
    t1.text = "欢迎使用命令助手"
    t1.offset = off
    js("Common").showTutorial(t1)
    val t2 = ciObject()
    t2.text = "点击图标进入命令生成器\n长按图标打开快捷栏"
    t2.offset = off
    t2.view = self.view
    t2.callback = { ->
        api.settings.tutor_icon = true
        api.trySave()
        js("undefined")
    }
    js("Common").showTutorial(t2)
    self.tutor = js("null")
}

private fun caIconAddPopup(self: dynamic, api: dynamic) {
    val rect = api.settings.pageRect
    if (ciTruthy(rect)) {
        js("PopupPage").setRect(rect[0], rect[1], rect[2], rect[3])
    }
    if (ciTruthy(api.settings.pageWindowed)) {
        js("PopupPage").setFullScreen(false, js("PopupPage").isLocked())
    }
    if (!ciIsNaN(api.settings.unfocusedAlpha)) {
        js("PopupPage").setAlpha(1, api.settings.unfocusedAlpha)
    }
    self.iconUpdate()
}

private fun caIconRectUpdate(api: dynamic, x: dynamic, y: dynamic, w: dynamic, h: dynamic) {
    val rect = api.settings.pageRect
    if (ciTruthy(rect)) {
        rect[0] = ciFloor(x)
        rect[1] = ciFloor(y)
        rect[2] = ciCeil(w)
        rect[3] = ciCeil(h)
    } else {
        api.settings.pageRect = ciArray(x, y, w, h)
    }
}

private fun caIconFullscreenChanged(self: dynamic, api: dynamic, isFullScreen: dynamic) {
    api.settings.pageWindowed = !ciTruthy(isFullScreen)
    self.iconUpdate()
}

// ---------------- 构建 / 显示 ----------------

/** 首次调用时构建图标视图、监听器与全部 self 方法（对应原 `if (!self.view) { ... }`）。 */
private fun caBuildIconUi(self: dynamic, api: dynamic) {
    val vcfg = ciViewConfig()
    val longPressTimeout = vcfg.getLongPressTimeout()
    val touchSlop = vcfg.getScaledTouchSlop()
    self.view = ciNewFrameLayout()
    self.view.setOnClickListener(ciNewClickListener({ _v: dynamic ->
        caIconClick(self, api)
    }))
    self.view.setOnTouchListener(ciNewTouchListener(ciTouchJs { touch: dynamic, v: dynamic, e: dynamic ->
        caIconTouch(self, api, touch, v, e, longPressTimeout, touchSlop)
    }))
    self.view.addOnLayoutChangeListener(ciNewLayoutChangeListener({
        _v: dynamic, _l: dynamic, _t: dynamic, _r: dynamic, _b: dynamic,
        _ol: dynamic, _ot: dynamic, _or: dynamic, _ob: dynamic ->
        caIconLayoutChange(self)
    }))
    self.longClick = ciNewRunnable({ ->
        caIconLongClick(self, api)
        js("undefined")
    })
    self.layoutChanged = { updateIcon: dynamic ->
        caIconLayoutChanged(self, api, updateIcon)
        js("undefined")
    }
    self.updateScreenInfo = { ->
        caIconUpdateScreenInfo(self)
        js("undefined")
    }
    self.animateToPos = { x: dynamic, y: dynamic, dur: dynamic, interpolator: dynamic, callback: dynamic ->
        caIconAnimateToPos(self, api, x, y, dur, interpolator, callback)
        js("undefined")
    }
    self.cancelAnimator = { ->
        caIconCancelAnimator(self)
        js("undefined")
    }
    self.animateTranslation = { offset: dynamic, delay: dynamic ->
        caIconAnimateTranslation(self, offset, delay)
        js("undefined")
    }
    self.open = { ->
        caIconOpen(self, api)
        js("undefined")
    }
    self.refreshAlpha = { ->
        caIconRefreshAlpha(self, api)
        js("undefined")
    }
    self.refreshPos = { ->
        caIconRefreshPos(self, api)
        js("undefined")
    }
    self.refreshIcon = { ->
        caIconRefreshIcon(self, api)
        js("undefined")
    }
    self.refresh = { ->
        caIconRefresh(self, api)
        js("undefined")
    }
    self.iconUpdate = { ->
        caIconIconUpdate(self, api)
        js("undefined")
    }
    self.tutor = if (ciTruthy(api.settings.tutor_icon)) js("null") else ({ ->
        caIconTutor(self, api)
        js("undefined")
    })
    // 原实现注册的是 CA 对象本身（不是 CA.icon），resetUICache 会把 CA.icon 置回原值。
    js("PWM").registerResetFlag(api, "icon")
    js("PWM").registerResetFlag(self, "view")
    js("PopupPage").on("addPopup", { ->
        caIconAddPopup(self, api)
        js("undefined")
    }).on("removePopup", self.iconUpdate)
        .on("show", self.iconUpdate)
        .on("hide", self.iconUpdate)
        .on("rectUpdate", { _eventName: dynamic, x: dynamic, y: dynamic, w: dynamic, h: dynamic ->
            caIconRectUpdate(api, x, y, w, h)
            js("undefined")
        })
        .on("fullscreenChanged", { _eventName: dynamic, isFullScreen: dynamic, _isLocked: dynamic ->
            caIconFullscreenChanged(self, api, isFullScreen)
            js("undefined")
        })
}

private fun caShowIconUi(self: dynamic, api: dynamic) {
    try {
        if (!ciTruthy(self.view)) {
            caBuildIconUi(self, api)
        }
        if (ciTruthy(api.icon)) return
        caIconUpdateScreenInfo(self)
        caIconRefreshIcon(self, api)
        if (ciIsNaN(api.settings.iconX)) {
            self.view.measure(0, 0)
            api.settings.iconX = 0
            api.settings.iconY = ciCeil(
                ciSub(
                    ciMul(0.25, js("G.screenHeight")),
                    ciMul(0.5, self.view.getMeasuredHeight())
                )
            )
        }
        api.icon = ciNewPopupWindow(self.view, "CA.Icon")
        // 原对象字面量里 x/y 用赋值表达式的值：self.cx = CA.settings.iconX 等。
        self.cx = api.settings.iconX
        self.cy = api.settings.iconY
        val showOptions = ciObject()
        showOptions.x = self.cx
        showOptions.y = self.cy
        showOptions.width = -2
        showOptions.height = -2
        showOptions.focusable = false
        showOptions.touchable = true
        api.icon.show(showOptions)
        caIconRefreshPos(self, api)
        js("PWM").addFloat(api.icon)
        //if (self.tutor) self.tutor();
    } catch (e: dynamic) {
        js("erp")(e)
    }
}

private fun caShowIcon(self: dynamic, api: dynamic): dynamic {
    js("G").ui({
        caShowIconUi(self, api)
        js("undefined")
    })
    return js("undefined")
}

private fun caHideIconUi(api: dynamic) {
    try {
        if (ciTruthy(api.icon)) api.icon.hide()
        api.icon = js("null")
    } catch (e: dynamic) {
        js("erp")(e)
    }
}

private fun caHideIcon(api: dynamic) {
    js("G").ui({
        caHideIconUi(api)
        js("undefined")
    })
}

private fun caQuickBarDefaultActions(): dynamic {
    val actions = js("[]")
    actions.push(ciActionItem("ca.exit"))
    actions.push(ciActionItem("ca.quickPaste"))
    return actions
}

// ---------------- 安装 ----------------

fun installCaIcon(api: dynamic) {
    // 具名 function self()，状态挂在函数对象上，arity 0。
    api.showIcon = ciSelf0Js { self: dynamic ->
        caShowIcon(self, api)
    }
    api.hideIcon = { ->
        caHideIcon(api)
        js("undefined")
    }
    api.quickBarDefaultActions = caQuickBarDefaultActions()
}
