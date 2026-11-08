package ca.kotlin.ui

import ca.kotlin.core.*


/**
 * modules/uiCore/ConsolePanel.js 的 Kotlin/JS 版本。
 *
 * 只有 build 一个入口，调用方传入的 self 需要提供 print / exec / history /
 * lines / vmaker / vbinder；界面上用到的 Android 类型全部经 G 访问。
 * 监听器里用普通具名函数处理提前 return，避免 dynamic lambda 的 return@label。
 */
private val cpApi: dynamic = js("({})")

private val cpTruthyJs: dynamic = js("function (v) { return !!v; }")
private val cpStringJs: dynamic = js("function (v) { return String(v); }")
private val cpLengthJs: dynamic = js("function (v) { return v.length | 0; }")
private fun cpLength(v: dynamic): Int = cpLengthJs(v)
private val cpStrictEqJs: dynamic = js("function (a, b) { return a === b; }")
private val cpDpJs: dynamic = js("function (n) { return n * G.dp; }")
private fun cpDp(n: Int): dynamic = cpDpJs(n)

private val cpNewLinearLayoutJs: dynamic = js("function (c) { return new G.LinearLayout(c); }")
private val cpNewEditTextJs: dynamic = js("function (c) { return new G.EditText(c); }")
private val cpNewTextViewJs: dynamic = js("function (c) { return new G.TextView(c); }")
private val cpNewListViewJs: dynamic = js("function (c) { return new G.ListView(c); }")
private val cpNewLayoutParamsJs: dynamic = js(
    "function (w, h) { return new G.LinearLayout.LayoutParams(w, h); }"
)
private val cpNewWeightedLayoutParamsJs: dynamic = js(
    "function (w, h, weight) { return new G.LinearLayout.LayoutParams(w, h, weight); }"
)
private val cpNewPopupPageJs: dynamic = js(
    "function (view, key) { return new PopupPage(view, key); }"
)
private val cpNewSimpleListAdapterJs: dynamic = js(
    "function (lines, maker, binder) { return new SimpleListAdapter(lines, maker, binder); }"
)
private val cpGetControllerJs: dynamic = js(
    "function (adapter) { return SimpleListAdapter.getController(adapter); }"
)
private val cpNewTouchListenerJs: dynamic = js(
    "function (f) { return new G.View.OnTouchListener({ onTouch: f }); }"
)
private val cpNewClickListenerJs: dynamic = js(
    "function (f) { return new G.View.OnClickListener({ onClick: f }); }"
)
private val cpNewItemClickListenerJs: dynamic = js(
    "function (f) { return new G.AdapterView.OnItemClickListener({ onItemClick: f }); }"
)
private val cpNewItemLongClickListenerJs: dynamic = js(
    "function (f) { return new G.AdapterView.OnItemLongClickListener({ onItemLongClick: f }); }"
)

private fun cpOnTouch(v: dynamic, ev: dynamic): dynamic {
    try {
        val action = ev.getAction()
        if (cpStrictEqJs(action, ev.ACTION_DOWN)) {
            js("Common").applyStyle(v, "button_reactive_pressed", 3)
        } else if (cpStrictEqJs(action, ev.ACTION_CANCEL) || cpStrictEqJs(action, ev.ACTION_UP)) {
            js("Common").applyStyle(v, "button_reactive", 3)
        }
        return false
    } catch (err: dynamic) {
        js("erp")(err)
        return true
    }
}

private fun cpOnClick(self: dynamic, v: dynamic): dynamic {
    try {
        if (!cpTruthyJs(self.cmd.getText().length())) return js("undefined")
        val s = cpStringJs(self.cmd.getText())
        self.print(s)
        self.print("\n")
        self.exec(s)
        self.cmd.setText("")
    } catch (err: dynamic) {
        js("erp")(err)
    }
    return js("undefined")
}

private fun cpOnItemClick(self: dynamic, parent: dynamic, view: dynamic, pos: dynamic, id: dynamic): dynamic {
    try {
        if (cpTruthyJs(self.history[pos])) {
            self.history[cpLength(self.lines) - 1] = cpStringJs(self.cmd.getText())
            self.cmd.setText(self.history[pos])
            self.cmd.setSelection(self.cmd.length())
        }
    } catch (err: dynamic) {
        js("erp")(err)
    }
    return js("undefined")
}

private fun cpOnItemLongClick(self: dynamic, parent: dynamic, view: dynamic, pos: dynamic, id: dynamic): dynamic {
    try {
        js("Common").setClipboardText(self.lines[pos])
        js("Common").toast("内容已复制")
    } catch (err: dynamic) {
        js("erp")(err)
    }
    return true
}

private fun cpBuild(self: dynamic, popupKey: dynamic, optsIn: dynamic): dynamic {
    val opts = if (cpTruthyJs(optsIn)) optsIn else js("({})")

    self.adapter = cpGetControllerJs(cpNewSimpleListAdapterJs(self.lines, self.vmaker, self.vbinder))

    val main = cpNewLinearLayoutJs(js("ctx"))
    self.main = main
    main.setOrientation(js("G.LinearLayout.VERTICAL"))

    val bar = cpNewLinearLayoutJs(js("ctx"))
    self.bar = bar
    bar.setLayoutParams(cpNewLayoutParamsJs(-1, -2))
    bar.setOrientation(js("G.LinearLayout.HORIZONTAL"))
    js("Common").applyStyle(bar, "bar_float")

    val cmd = cpNewEditTextJs(js("ctx"))
    self.cmd = cmd
    cmd.setLayoutParams(cpNewWeightedLayoutParamsJs(-1, -2, 1.0))
    cmd.setFocusableInTouchMode(true)
    cmd.setPadding(
        cpDp(5), cpDp(10), 0, cpDp(10)
    )
    cmd.setImeOptions(js("G.EditorInfo.IME_FLAG_NO_FULLSCREEN"))
    js("Common").applyStyle(cmd, "edittext_default", 3)
    bar.addView(cmd)
    js("Common").postIME(cmd)

    val eval = cpNewTextViewJs(js("ctx"))
    self.eval = eval
    eval.setLayoutParams(cpNewLayoutParamsJs(-2, -1))
    eval.setGravity(js("G.Gravity.CENTER"))
    eval.setText(">")
    eval.setPadding(
        cpDp(10), cpDp(10), cpDp(10), cpDp(10)
    )
    js("Common").applyStyle(eval, "button_reactive", 3)
    eval.setOnTouchListener(cpNewTouchListenerJs({ v: dynamic, ev: dynamic -> cpOnTouch(v, ev) }))
    eval.setOnClickListener(cpNewClickListenerJs({ v: dynamic -> cpOnClick(self, v) }))
    bar.addView(eval)

    val prompt = cpNewListViewJs(js("ctx"))
    self.prompt = prompt
    prompt.setLayoutParams(cpNewWeightedLayoutParamsJs(-1, 0, 1.0))
    prompt.setDividerHeight(0)
    js("Common").applyStyle(prompt, "message_bg")
    prompt.setOnItemClickListener(cpNewItemClickListenerJs({ parent: dynamic, view: dynamic, pos: dynamic, id: dynamic ->
        cpOnItemClick(self, parent, view, pos, id)
    }))
    prompt.setOnItemLongClickListener(cpNewItemLongClickListenerJs({ parent: dynamic, view: dynamic, pos: dynamic, id: dynamic ->
        cpOnItemLongClick(self, parent, view, pos, id)
    }))
    prompt.setAdapter(self.adapter.self)
    main.addView(prompt)
    main.addView(bar)

    self.popup = cpNewPopupPageJs(main, popupKey)

    js("PWM").registerResetFlag(self, "main")
    if (cpTruthyJs(opts.afterBuild)) opts.afterBuild()
    js("PWM").registerResetFlag(self, "main")
    return self
}

fun installConsolePanel() {
    cpApi.build = { self: dynamic, popupKey: dynamic, opts: dynamic ->
        cpBuild(self, popupKey, opts)
    }

    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("ConsolePanel", cpApi)
    }
}
