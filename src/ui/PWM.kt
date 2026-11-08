package ca.kotlin.ui

import ca.kotlin.core.*


/**
 * modules/uiCore/PWM.js 的 Kotlin/JS 版本。
 *
 * PWM 的方法全部依赖动态 this（EventSender 也会把 on/off/trigger 装到 PWM 上），
 * 因此用一层薄 JS wrapper 把真实 this / arguments 传回 Kotlin。
 * reset 里 `floats.length = popups.length` 是原实现的既有行为，按原样保留。
 */
private val pwmApi: dynamic = js("({})")

private val pwmLengthJs: dynamic = js("function (v) { return v.length | 0; }")
private val pwmSetLengthJs: dynamic = js("function (v, n) { v.length = n; }")
private val pwmLooseEqJs: dynamic = js("function (a, b) { return a == b; }")
private val pwmThisJs: dynamic = js("function (f) { return function () { return f(this, arguments); }; }")
private val pwmForInBreakJs: dynamic = js(
    "function (o, f) { for (var k in o) { if (f(k)) return; } }"
)

private fun pwmOnCreate(self: dynamic): dynamic {
    js("EventSender").init(self)
    return js("undefined")
}

private fun pwmInitialize(self: dynamic): dynamic {
    js("PopupPage").on("addPopup", { js("PWM").onPageAdd() })
    return js("undefined")
}

private fun pwmOnResume(self: dynamic): dynamic {
    js("PopupPage").show()
    return false
}

private fun pwmOnPageAdd(self: dynamic): dynamic {
    self.floats.forEach({ e: dynamic -> e.bringToFront() })
    return js("undefined")
}

private fun pwmAddFloat(self: dynamic, w: dynamic): dynamic {
    if (self.floats.indexOf(w) < 0) self.floats.push(w)
    self.trigger("addFloat", w)
    return js("undefined")
}

private fun pwmAddPopup(self: dynamic, w: dynamic): dynamic {
    if (self.popups.indexOf(w) < 0) self.popups.push(w)
    self.trigger("addPopup", w)
    return js("undefined")
}

private fun pwmDismissFloat(self: dynamic): dynamic {
    self.busy = true
    self.floats.forEach({ e: dynamic -> e.hide() })
    self.busy = false
    self.trigger("dismissFloat")
    return js("undefined")
}

private fun pwmDismissPopup(self: dynamic): dynamic {
    self.busy = true
    self.popups.forEach({ e: dynamic -> e.hide() })
    self.busy = false
    self.trigger("dismissPopup")
    return js("undefined")
}

private fun pwmReset(self: dynamic): dynamic {
    self.trigger("reset")
    // 保留原实现：floats 的长度被 popups 的长度覆盖。
    pwmSetLengthJs(self.floats, pwmLengthJs(self.popups))
    self.clearListeners()
    js("PopupPage").reset()
    self.initialize()
    return js("undefined")
}

private fun pwmResetUICache(self: dynamic): dynamic {
    self.resetFlags.forEach({ e: dynamic ->
        e.obj[e.prop] = e.value
    })
    return js("undefined")
}

private fun pwmRegisterResetFlag(self: dynamic, obj: dynamic, prop: dynamic, value: dynamic): dynamic {
    var matched = false
    pwmForInBreakJs(self.resetFlags) { i: dynamic ->
        val e = self.resetFlags[i]
        if (pwmLooseEqJs(e.obj, obj) && pwmLooseEqJs(e.prop, prop)) {
            e.value = value
            matched = true
            true
        } else {
            false
        }
    }
    if (!matched) {
        val e = js("({})")
        e.obj = obj
        e.prop = prop
        e.value = value
        self.resetFlags.push(e)
    }
    return js("undefined")
}

fun installPWM() {
    pwmApi.floats = js("[]")
    pwmApi.popups = js("[]")
    pwmApi.listener = js("({})")
    pwmApi.resetFlags = js("[]")
    pwmApi.intentBack = false
    pwmApi.busy = false
    // 原文件在对象字面量构造时求值 wm。
    pwmApi.wm = js("ctx").getSystemService(js("ctx").WINDOW_SERVICE)

    pwmApi.onCreate = pwmThisJs { self: dynamic, args: dynamic -> pwmOnCreate(self) }
    pwmApi.initialize = pwmThisJs { self: dynamic, args: dynamic -> pwmInitialize(self) }
    pwmApi.onResume = pwmThisJs { self: dynamic, args: dynamic -> pwmOnResume(self) }
    pwmApi.onPageAdd = pwmThisJs { self: dynamic, args: dynamic -> pwmOnPageAdd(self) }
    pwmApi.addFloat = pwmThisJs { self: dynamic, args: dynamic -> pwmAddFloat(self, args[0]) }
    pwmApi.addPopup = pwmThisJs { self: dynamic, args: dynamic -> pwmAddPopup(self, args[0]) }
    pwmApi.dismissFloat = pwmThisJs { self: dynamic, args: dynamic -> pwmDismissFloat(self) }
    pwmApi.dismissPopup = pwmThisJs { self: dynamic, args: dynamic -> pwmDismissPopup(self) }
    pwmApi.reset = pwmThisJs { self: dynamic, args: dynamic -> pwmReset(self) }
    pwmApi.resetUICache = pwmThisJs { self: dynamic, args: dynamic -> pwmResetUICache(self) }
    pwmApi.registerResetFlag = pwmThisJs { self: dynamic, args: dynamic ->
        pwmRegisterResetFlag(self, args[0], args[1], args[2])
    }

    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("PWM", pwmApi)
    }
}
