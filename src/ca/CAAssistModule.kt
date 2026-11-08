package ca.kotlin.ca

import ca.kotlin.core.*


/**
 * modules/CA.Assist.js 的 Kotlin/JS 版本。
 *
 * 只提供 buildCaAssistModule()：构造并返回与原 CA.Assist 对象字面量等价的动态对象。
 * 不调用 MapScript.loadModule、不定义全局 CA、不自行注册；由 CA.kt 在总装时赋值给
 * caApi.Assist。
 *
 * 迁移约定：
 *  - 原对象字面量里的 this 改成显式局部对象 asApi；内部方法之间通过 asApi.xxx 调用。
 *  - 原 show/editParamDialog/editParamJSON/editParamPosition/editParamSelector/editComponent
 *    是具名函数表达式 `function self(...)`，缓存挂在函数对象自身上。这里用薄 JS wrapper
 *    生成同名同 arity 的 function self(...)，把真实 self 传回 Kotlin，缓存位置不变。
 *  - 对外部 CA / CA.IntelliSense / Common / G / Accel / PWM / JSONEdit / PopupPage 等
 *    的引用保持运行时动态查找，不提前缓存。
 *  - JS 的 truthy、宽松相等、typeof、in、instanceof、数组 length、加法拼接等语义走
 *    带参 JS helper；js("...") 里不引用 Kotlin 局部变量。
 *  - 原有 bug 按原样保留并注释：
 *      * editComponent 用 self.selectors 作为一次性初始化守卫，但该分支从不设置
 *        self.selectors，所以每次调用都会重建内部函数；
 *      * editParamSelector 的标签编辑回调里，玩家名以 "@" 开头时会先 toast 再
 *        callback("")；
 *      * editParamPosition 的“使用局部坐标”复选框在 setChecked 之后才设置可见性；
 *      * show 的 refresh 里先给 CA.Assist.params 赋完整数组，再 filter 出列表，
 *        params 本身不保存过滤结果。
 */

// ---------------- JS 语义 helper ----------------

private val caaTruthyJs: dynamic = js("function (v) { return !!v; }")
private fun caaTruthy(v: dynamic): Boolean = caaTruthyJs(v)

private val caaLooseEqJs: dynamic = js("function (a, b) { return a == b; }")
private fun caaLooseEq(a: dynamic, b: dynamic): Boolean = caaLooseEqJs(a, b)

private val caaLooseNeJs: dynamic = js("function (a, b) { return a != b; }")
private fun caaLooseNe(a: dynamic, b: dynamic): Boolean = caaLooseNeJs(a, b)

private val caaNullishJs: dynamic = js("function (v) { return v == null; }")
private fun caaNullish(v: dynamic): Boolean = caaNullishJs(v)

private val caaTypeofJs: dynamic = js("function (v) { return typeof v; }")
private fun caaTypeof(v: dynamic): dynamic = caaTypeofJs(v)

private val caaStringJs: dynamic = js("function (v) { return String(v); }")
private fun caaString(v: dynamic): dynamic = caaStringJs(v)

private val caaBooleanJs: dynamic = js("function (v) { return Boolean(v); }")
private fun caaBoolean(v: dynamic): dynamic = caaBooleanJs(v)

// 所有 Java/JS 数组的 length 统一走 `| 0`。
private val caaLengthJs: dynamic = js("function (v) { return v.length | 0; }")
private fun caaLength(v: dynamic): Int = caaLengthJs(v)

private val caaAddJs: dynamic = js("function (a, b) { return a + b; }")
private fun caaAdd(a: dynamic, b: dynamic): dynamic = caaAddJs(a, b)

private val caaOrJs: dynamic = js("function (a, b) { return a || b; }")
private fun caaOr(a: dynamic, b: dynamic): dynamic = caaOrJs(a, b)

private val caaBitOrJs: dynamic = js("function (a, b) { return a | b; }")
private fun caaBitOr(a: dynamic, b: dynamic): dynamic = caaBitOrJs(a, b)

private val caaIsNaNRawJs: dynamic = js("function (v) { return isNaN(v); }")
private fun caaIsNaN(v: dynamic): Boolean = caaIsNaNRawJs(v)

private val caaIsFiniteJs: dynamic = js("function (v) { return isFinite(v); }")
private fun caaIsFinite(v: dynamic): Boolean = caaIsFiniteJs(v)

private val caaParseIntJs: dynamic = js("function (v) { return parseInt(v); }")
private fun caaParseInt(v: dynamic): dynamic = caaParseIntJs(v)

private val caaParseFloatJs: dynamic = js("function (v) { return parseFloat(v); }")
private fun caaParseFloat(v: dynamic): dynamic = caaParseFloatJs(v)

private val caaAbsJs: dynamic = js("function (v) { return Math.abs(v); }")
private fun caaAbs(v: dynamic): dynamic = caaAbsJs(v)

private val caaNumberJs: dynamic = js("function (v) { return Number(v); }")
private fun caaNumber(v: dynamic): dynamic = caaNumberJs(v)

private val caaJsonStringifyJs: dynamic = js("function (v) { return JSON.stringify(v); }")
private fun caaJsonStringify(v: dynamic): dynamic = caaJsonStringifyJs(v)

private val caaObjectKeysJs: dynamic = js("function (o) { return Object.keys(o); }")
private fun caaObjectKeys(o: dynamic): dynamic = caaObjectKeysJs(o)

private val caaIsArrayJs: dynamic = js("function (v) { return Array.isArray(v); }")
private fun caaIsArray(v: dynamic): Boolean = caaIsArrayJs(v)

private val caaInstanceOfJs: dynamic = js("function (v, c) { return v instanceof c; }")
private fun caaInstanceOf(v: dynamic, c: dynamic): Boolean = caaInstanceOfJs(v, c)

private val caaInJs: dynamic = js("function (k, o) { return k in o; }")
private fun caaIn(k: dynamic, o: dynamic): Boolean = caaInJs(k, o)

private val caaForInJs: dynamic = js("function (o, f) { for (var k in o) f(k); }")
private fun caaForIn(o: dynamic, f: dynamic) { caaForInJs(o, f) }

private val caaForEachJs: dynamic = js("function (a, f) { a.forEach(f); }")
private fun caaForEach(a: dynamic, f: dynamic) { caaForEachJs(a, f) }

private val caaMapJs: dynamic = js("function (a, f) { return a.map(f); }")
private fun caaMap(a: dynamic, f: dynamic): dynamic = caaMapJs(a, f)

private val caaFilterJs: dynamic = js("function (a, f) { return a.filter(f); }")
private fun caaFilter(a: dynamic, f: dynamic): dynamic = caaFilterJs(a, f)

private val caaJoinJs: dynamic = js("function (a, s) { return a.join(s); }")
private fun caaJoin(a: dynamic, s: dynamic): dynamic = caaJoinJs(a, s)

private val caaSliceJs: dynamic = js("function (s, a, b) { return s.slice(a, b); }")
private fun caaSlice(s: dynamic, a: dynamic, b: dynamic): dynamic = caaSliceJs(s, a, b)

private val caaStartsWithJs: dynamic = js("function (s, p) { return s.startsWith(p); }")
private fun caaStartsWith(s: dynamic, p: dynamic): Boolean = caaStartsWithJs(s, p)

private val caaRegExpTestJs: dynamic = js(
    "function (p, f, s) { return new RegExp(p, f).test(s); }"
)
private fun caaRegExpTest(p: dynamic, f: dynamic, s: dynamic): Boolean = caaRegExpTestJs(p, f, s)

private val caaNewURLJs: dynamic = js("function (u) { return new java.net.URL(u); }")
private fun caaNewURL(u: dynamic): dynamic = caaNewURLJs(u)

// ---------------- 函数对象 / 动态 this helper ----------------

/** 生成具名函数表达式 `function self(...)`，与旧 API 的 arity/name 对齐。 */
private val caaSelf0Js: dynamic = js("function (f) { return function self() { return f(self); }; }")
private val caaSelf3Js: dynamic = js(
    "function (f) { return function self(a, b, c) { return f(self, a, b, c); }; }"
)

/** JSONEdit 的 update 回调依赖动态 this（读取 this.source）。 */
private val caaThis0Js: dynamic = js("function (f) { return function () { return f(this); }; }")

private fun caaObj(): dynamic = js("({})")

private fun caaArr(vararg items: dynamic): dynamic {
    val a = js("[]")
    var i = 0
    while (i < items.size) {
        a.push(items[i])
        i++
    }
    return a
}

private fun caaParamObj(param: dynamic): dynamic {
    val o = caaObj()
    o.param = param
    return o
}

// ---------------- Android UI helper ----------------

private val caaDpJs: dynamic = js("function (n) { return n * G.dp; }")
private fun caaDp(n: Int): dynamic = caaDpJs(n)

private val caaNewTextViewJs: dynamic = js("function () { return new G.TextView(ctx); }")
private val caaNewEditTextJs: dynamic = js("function () { return new G.EditText(ctx); }")
private val caaNewListViewJs: dynamic = js("function () { return new G.ListView(ctx); }")
private val caaNewLinearLayoutJs: dynamic = js("function () { return new G.LinearLayout(ctx); }")
private val caaNewScrollViewJs: dynamic = js("function () { return new G.ScrollView(ctx); }")
private val caaNewTableLayoutJs: dynamic = js("function () { return new G.TableLayout(ctx); }")
private val caaNewTableRowJs: dynamic = js("function () { return new G.TableRow(ctx); }")
private val caaNewCheckBoxJs: dynamic = js("function () { return new G.CheckBox(ctx); }")

private fun caaNewTextView(): dynamic = caaNewTextViewJs()
private fun caaNewEditText(): dynamic = caaNewEditTextJs()
private fun caaNewListView(): dynamic = caaNewListViewJs()
private fun caaNewLinearLayout(): dynamic = caaNewLinearLayoutJs()
private fun caaNewScrollView(): dynamic = caaNewScrollViewJs()
private fun caaNewTableLayout(): dynamic = caaNewTableLayoutJs()
private fun caaNewTableRow(): dynamic = caaNewTableRowJs()
private fun caaNewCheckBox(): dynamic = caaNewCheckBoxJs()

private val caaNewAbsListViewParamsJs: dynamic = js(
    "function (w, h) { return new G.AbsListView.LayoutParams(w, h); }"
)
private val caaNewLinearLayoutParams2Js: dynamic = js(
    "function (w, h) { return new G.LinearLayout.LayoutParams(w, h); }"
)
private val caaNewLinearLayoutParams3Js: dynamic = js(
    "function (w, h, weight) { return new G.LinearLayout.LayoutParams(w, h, weight); }"
)
private val caaNewFrameLayoutParamsJs: dynamic = js(
    "function (w, h) { return new G.FrameLayout.LayoutParams(w, h); }"
)
private val caaNewTableLayoutParamsJs: dynamic = js(
    "function (w, h) { return new G.TableLayout.LayoutParams(w, h); }"
)
private val caaNewTableRowParams2Js: dynamic = js(
    "function (w, h) { return new G.TableRow.LayoutParams(w, h); }"
)
private val caaNewTableRowParams3Js: dynamic = js(
    "function (w, h, weight) { return new G.TableRow.LayoutParams(w, h, weight); }"
)

private fun caaNewAbsListViewParams(w: dynamic, h: dynamic): dynamic =
    caaNewAbsListViewParamsJs(w, h)
private fun caaNewLinearLayoutParams2(w: dynamic, h: dynamic): dynamic =
    caaNewLinearLayoutParams2Js(w, h)
private fun caaNewLinearLayoutParams3(w: dynamic, h: dynamic, weight: dynamic): dynamic =
    caaNewLinearLayoutParams3Js(w, h, weight)
private fun caaNewFrameLayoutParams(w: dynamic, h: dynamic): dynamic =
    caaNewFrameLayoutParamsJs(w, h)
private fun caaNewTableLayoutParams(w: dynamic, h: dynamic): dynamic =
    caaNewTableLayoutParamsJs(w, h)
private fun caaNewTableRowParams2(w: dynamic, h: dynamic): dynamic =
    caaNewTableRowParams2Js(w, h)
private fun caaNewTableRowParams3(w: dynamic, h: dynamic, weight: dynamic): dynamic =
    caaNewTableRowParams3Js(w, h, weight)

private val caaNewClickListenerJs: dynamic = js(
    "function (f) { return new G.View.OnClickListener({ onClick: f }); }"
)
private val caaNewItemClickListenerJs: dynamic = js(
    "function (f) { return new G.AdapterView.OnItemClickListener({ onItemClick: f }); }"
)
private val caaNewCheckedChangeListenerJs: dynamic = js(
    "function (f) { return new G.CompoundButton.OnCheckedChangeListener({ onCheckedChanged: f }); }"
)
private val caaNewTextWatcherJs: dynamic = js(
    "function (f) { return new G.TextWatcher({ afterTextChanged: f }); }"
)

private fun caaNewClickListener(f: dynamic): dynamic = caaNewClickListenerJs(f)
private fun caaNewItemClickListener(f: dynamic): dynamic = caaNewItemClickListenerJs(f)
private fun caaNewCheckedChangeListener(f: dynamic): dynamic = caaNewCheckedChangeListenerJs(f)
private fun caaNewTextWatcher(f: dynamic): dynamic = caaNewTextWatcherJs(f)

private val caaNewSpannableStringBuilderJs: dynamic = js(
    "function (s) { return new G.SpannableStringBuilder(s); }"
)
private val caaNewForegroundColorSpanJs: dynamic = js(
    "function (c) { return new G.ForegroundColorSpan(c); }"
)

private fun caaNewSpannableStringBuilder(s: dynamic): dynamic = caaNewSpannableStringBuilderJs(s)
private fun caaNewForegroundColorSpan(c: dynamic): dynamic = caaNewForegroundColorSpanJs(c)

private val caaNewRhinoListAdapterJs: dynamic = js(
    "function (a, v, p) { return new RhinoListAdapter(a, v, p); }"
)
private val caaNewSimpleListAdapterJs: dynamic = js(
    "function (a, m, b, p) { return new SimpleListAdapter(a, m, b, p); }"
)
private val caaNewFilterListAdapterJs: dynamic = js(
    "function (w) { return new FilterListAdapter(w); }"
)

private fun caaNewRhinoListAdapter(a: dynamic, v: dynamic, p: dynamic): dynamic =
    caaNewRhinoListAdapterJs(a, v, p)
private fun caaNewSimpleListAdapter(a: dynamic, m: dynamic, b: dynamic, p: dynamic): dynamic =
    caaNewSimpleListAdapterJs(a, m, b, p)
private fun caaNewFilterListAdapter(w: dynamic): dynamic = caaNewFilterListAdapterJs(w)

// ---------------- 参数编辑器公共逻辑 ----------------

private fun caaInitTextBox(e: dynamic, defVal: dynamic): dynamic {
    val ret = caaNewEditText()
    ret.setText(
        if (caaTruthy(defVal)) caaString(defVal)
        else if (caaTruthy(e.text)) e.text
        else ""
    )
    ret.setSingleLine(true)
    ret.setPadding(0, caaDp(10), 0, caaDp(10))
    ret.setLayoutParams(caaNewLinearLayoutParams2(-1, -2))
    ret.setSelection(ret.length())
    js("Common").applyStyle(ret, "edittext_default", 2)
    return ret
}

private fun caaInitListener(ret: dynamic, l: dynamic, gText: dynamic) {
    l.getText = { pure: dynamic ->
        if (caaTruthy(pure)) ret.getText() else gText()
    }
    l.setText = { value: dynamic ->
        ret.setText(caaString(value))
        js("undefined")
    }
    ret.addTextChangedListener(caaNewTextWatcher({ s: dynamic ->
        try {
            l.onTextChanged(s)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }))
}

private fun caaDialogIntText(ret: dynamic): dynamic {
    val t = ret.getText()
    if (!caaTruthy(t.length())) return js("undefined")
    if (caaIsFinite(t)) return caaParseInt(t)
    js("Common").toast("内容不是数字！")
    return js("null")
}

private fun caaDialogUintText(ret: dynamic): dynamic {
    val t = ret.getText()
    if (!caaTruthy(t.length())) return js("undefined")
    if (caaIsFinite(t)) return caaAbs(caaParseInt(t))
    js("Common").toast("内容不是数字！")
    return js("null")
}

private fun caaDialogFloatText(ret: dynamic): dynamic {
    val t = ret.getText()
    if (!caaTruthy(t.length())) return js("undefined")
    if (caaIsFinite(t)) return caaParseFloat(t)
    js("Common").toast("内容不是数字！")
    return js("null")
}

private fun caaDialogCustomText(ret: dynamic, finish: dynamic): dynamic {
    if (ret.length() == 0) return js("undefined")
    if (caaRegExpTest(finish, "", ret.getText())) return ret.getText()
    js("Common").toast("内容不合规范！")
    return js("null")
}

private fun caaDialogPlainText(ret: dynamic): dynamic {
    return if (ret.length() > 0) ret.getText() else js("undefined")
}

private fun caaRelativeGetText(e: dynamic, ret: dynamic, rela: dynamic, pure: dynamic): dynamic {
    e.isRela = rela.isChecked()
    e.offset = ret.getText()
    if (caaTruthy(pure)) {
        return caaAdd(if (caaTruthy(e.isRela)) "~" else "", caaParseFloat(e.offset))
    }
    if (!caaTruthy(e.offset.length())) return js("undefined")
    if (caaIsFinite(e.offset)) {
        return caaAdd(if (caaTruthy(e.isRela)) "~" else "", caaParseFloat(e.offset))
    }
    js("Common").toast("内容不是数字！")
    return js("null")
}

private fun caaRelativeSetText(value: dynamic, ret: dynamic, rela: dynamic) {
    val s = caaString(value)
    val f = caaStartsWith(s, "~")
    rela.setChecked(f)
    ret.setText(if (f) caaSlice(s, 1, js("undefined")) else s)
}

private fun caaParamDialogOk(listener: dynamic, onReset: dynamic, callback: dynamic, popup: dynamic) {
    try {
        val t = listener.getText()
        if (caaLooseEq(caaTypeof(t), "undefined") && caaTruthy(onReset)) {
            onReset()
        } else if (caaNullish(t)) {
            // 原实现：t == null（含 undefined 且没有 onReset）时直接 return，不关闭对话框。
            return
        } else {
            callback(caaString(t))
        }
        popup.exit()
    } catch (e: dynamic) {
        js("erp")(e)
    }
}

private fun caaParamFilterText(adpt: dynamic, s: dynamic) {
    val text = caaString(s)
    if (caaTruthy(text)) {
        val keep = js("Accel").filterTexts(text)
        adpt.setFilter({ _e: dynamic, i: dynamic -> keep[i] })
    } else {
        adpt.clearFilter()
    }
}

// ---------------- show / hide ----------------

private fun caaShowItemClick(
    asApi: dynamic,
    self: dynamic,
    parent: dynamic,
    view: dynamic,
    pos: dynamic,
    id: dynamic
) {
    try {
        if (caaLooseEq(pos, 0)) {
            self.choosePattern()
            return
        }
        val e = parent.getItemAtPosition(pos)
        asApi.editParam(
            e,
            { t: dynamic ->
                js("G").ui({
                    try {
                        e.text = caaString(t)
                        e._text.setText(e.text)
                        asApi.refreshCommand()
                    } catch (e2: dynamic) {
                        js("erp")(e2)
                    }
                    js("undefined")
                })
                js("undefined")
            },
            {
                asApi.params[pos - 1] = caaParamObj(e.param)
                self.adpt.replace(asApi.params[pos - 1], pos - 1)
                asApi.refreshCommand()
                js("undefined")
            }
        )
    } catch (e: dynamic) {
        js("erp")(e)
    }
}

private fun caaShowRefresh(asApi: dynamic, self: dynamic) {
    var pp: dynamic = js("null")
    var arr: dynamic = js("null")
    var adpt: dynamic = js("null")
    var help: dynamic = js("null")

    if (caaTruthy(asApi.command)) {
        pp = caaNewSpannableStringBuilder(asApi.formatPattern(asApi.command, asApi.pattern))
        pp.append("\n")
        js("appendSSB")(
            pp,
            asApi.getPatternDescription(asApi.command, asApi.pattern),
            caaNewForegroundColorSpan(js("Common").theme.promptcolor)
        )
        val base = if (caaTruthy(asApi.pattern)) {
            js("CA").IntelliSense.library.commands[asApi.command].patterns[asApi.pattern].params
        } else {
            js("[]")
        }
        arr = caaOr(base, js("[]"))
        arr = caaMap(arr, { item: dynamic, _i: dynamic -> caaParamObj(item) })
    } else {
        pp = "选择命令……"
        arr = js("[]")
    }

    self.head.setText(pp)
    // 原实现先把完整数组赋给 CA.Assist.params，再 filter 出列表；filter 结果不回写。
    asApi.params = arr
    val filtered = caaFilter(asApi.params, { item: dynamic ->
        if (caaLooseEq(item.param.type, "plain")) {
            item.text = item.param.name
            false
        } else {
            true
        }
    })
    adpt = caaNewRhinoListAdapter(filtered, asApi.paramAdapter, self)
    self.list.setAdapter(adpt)
    self.adpt = js("RhinoListAdapter").getController(adpt)

    try {
        help = if (caaTruthy(asApi.command)) {
            js("CA").IntelliSense.library.commands[asApi.command].help
        } else {
            js("CA").IntelliSense.library.help.command
        }
        caaNewURL(help)
        js("CA").showAssist.postHelp(0, help)
    } catch (e: dynamic) {
        js("CA").showAssist.postHelp(1, caaOr(help, "暂时没有帮助，以后会加上的啦"))
    }

    asApi.refreshCommand()
    js("undefined")
}

private fun caaShow(asApi: dynamic, self: dynamic) {
    js("G").ui({
        try {
            if (!caaTruthy(self.head)) {
                self.init = {
                    asApi.command = js("null")
                    asApi.pattern = js("null")
                    self.refresh()
                    self.choosePattern(true)
                    js("undefined")
                }
                self.refresh = { caaShowRefresh(asApi, self) }
                self.choosePattern = { optional: dynamic ->
                    asApi.chooseCommand({ cmd: dynamic ->
                        asApi.choosePatterns(
                            cmd,
                            { pattern: dynamic ->
                                asApi.command = cmd
                                asApi.pattern = pattern
                                self.refresh()
                                js("undefined")
                            },
                            optional
                        )
                        js("undefined")
                    }, optional)
                    js("undefined")
                }

                self.head = caaNewTextView()
                self.head.setLayoutParams(caaNewAbsListViewParams(-1, -2))
                self.head.setPadding(caaDp(20), caaDp(10), caaDp(20), caaDp(10))
                self.head.setTypeface(caaOr(js("G").Typeface.MONOSPACE, js("G").Typeface.DEFAULT))
                self.head.setLineSpacing(10, 1)
                js("Common").applyStyle(self.head, "textview_default", 2)

                self.list = caaNewListView()
                self.list.setOnItemClickListener(caaNewItemClickListener({
                    parent: dynamic,
                    view: dynamic,
                    pos: dynamic,
                    id: dynamic ->
                    caaShowItemClick(asApi, self, parent, view, pos, id)
                    js("undefined")
                }))
                self.list.addHeaderView(self.head)
                self.list.setLayoutParams(caaNewFrameLayoutParams(-1, -1))
                js("CA").showAssist.initContent(self.list)
                js("PWM").registerResetFlag(asApi, "ui")
                js("PWM").registerResetFlag(self, "head")
            }

            self.init()
            if (!caaTruthy(asApi.ui)) {
                asApi.ui = self.list
                js("CA").showAssist.con.addView(asApi.ui)
            }
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

private fun caaHide(asApi: dynamic) {
    js("G").ui({
        try {
            if (caaTruthy(asApi.ui)) {
                js("CA").showAssist.con.removeView(asApi.ui)
                asApi.ui = js("null")
            }
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

// ---------------- 列表项 / 命令刷新 ----------------

private fun caaParamAdapter(asApi: dynamic, e: dynamic, i: dynamic, a: dynamic): dynamic {
    val p = e.param
    val hl = caaNewLinearLayout()
    hl.setOrientation(js("G.LinearLayout.HORIZONTAL"))
    hl.setLayoutParams(caaNewAbsListViewParams(-1, -2))
    hl.setPadding(caaDp(20), caaDp(10), caaDp(20), caaDp(10))

    val vl = caaNewLinearLayout()
    vl.setOrientation(js("G.LinearLayout.VERTICAL"))
    vl.setLayoutParams(caaNewLinearLayoutParams3(-2, -2, 1.0))
    vl.getLayoutParams().gravity = js("G.Gravity.CENTER")

    val name = caaNewTextView()
    name.setText(caaAdd(
        caaString(p.name),
        if (caaTruthy(p.optional) || caaTruthy(p.canIgnore) || caaTruthy(p.chainOptional)) " (可选)" else ""
    ))
    name.setLayoutParams(caaNewLinearLayoutParams2(-1, -2))
    js("Common").applyStyle(name, "textview_default", 3)
    vl.addView(name)

    val desp = caaNewTextView()
    desp.setText(
        if (caaTruthy(p.description)) caaString(p.description) else asApi.getParamType(p)
    )
    desp.setSingleLine(true)
    desp.setEllipsize(js("G.TextUtils.TruncateAt.END"))
    desp.setLayoutParams(caaNewLinearLayoutParams2(-1, -2))
    js("Common").applyStyle(desp, "textview_prompt", 1)
    vl.addView(desp)
    hl.addView(vl)

    e._text = caaNewTextView()
    e._text.setText("点击以编辑")
    e._text.setPadding(caaDp(10), caaDp(10), caaDp(10), caaDp(10))
    e._text.setMaxEms(10)
    e._text.setSingleLine(true)
    e._text.setEllipsize(js("G.TextUtils.TruncateAt.END"))
    e._text.setLayoutParams(caaNewLinearLayoutParams3(-2, -2, 0))
    e._text.getLayoutParams().gravity = js("G.Gravity.CENTER")
    js("Common").applyStyle(e._text, "textview_prompt", 2)
    hl.addView(e._text)
    return hl
}

private fun caaRefreshCommand(asApi: dynamic) {
    if (caaTruthy(asApi.command)) {
        val r = caaArr(caaAdd("/", asApi.command))
        val p = asApi.params
        var i = 0
        while (i < caaLength(p)) {
            if (!caaTruthy(p[i].text)) break
            r.push(p[i].text)
            i++
        }
        js("CA").cmd.setText(caaJoin(r, " "))
    } else {
        js("CA").cmd.setText("/")
    }
}

// ---------------- 参数类型分派 ----------------

private fun caaEditParam(asApi: dynamic, e: dynamic, callback: dynamic, onReset: dynamic) {
    val type = e.param.type
    if (caaLooseEq(type, "plain")) {
        val edit = caaObj()
        edit.text = e.param.name
        edit.onclick = {
            callback(e.param.name)
            js("undefined")
        }
        val reset = caaObj()
        reset.text = "重置参数"
        reset.onclick = {
            onReset()
            js("undefined")
        }
        reset.hidden = { !caaTruthy(onReset) }
        js("Common").showOperateDialog(caaArr(edit, reset))
    } else if (caaLooseEq(type, "enum")) {
        asApi.editParamEnum(e, callback, onReset)
    } else if (caaLooseEq(type, "nbt") || caaLooseEq(type, "json")) {
        asApi.editParamJSON(e, callback, onReset)
    } else if (caaLooseEq(type, "position")) {
        asApi.editParamPosition(e, callback, onReset)
    } else if (caaLooseEq(type, "selector")) {
        asApi.editParamSelector(e, callback, onReset)
    } else {
        asApi.editParamDialog(e, callback, onReset)
    }
}

private fun caaEditParamEnum(asApi: dynamic, e: dynamic, callback: dynamic, onReset: dynamic) {
    val t = if (caaInstanceOf(e.param.list, js("Object"))) {
        e.param.list
    } else {
        js("CA").IntelliSense.library.enums[e.param.list]
    }
    val arr = js("[]")
    if (caaTruthy(onReset)) {
        val reset = caaObj()
        reset.text = "重置参数"
        reset.reset = true
        arr.push(reset)
    }
    if (caaIsArray(t)) {
        caaForIn(t, { i: dynamic ->
            arr.push(t[i])
            js("undefined")
        })
    } else {
        caaForIn(t, { i: dynamic ->
            if (caaTruthy(t[i])) {
                val o = caaObj()
                o.text = i
                o.description = t[i]
                arr.push(o)
            } else {
                arr.push(i)
            }
            js("undefined")
        })
    }
    js("Common").showListChooser(arr, { pos: dynamic ->
        val item = arr[pos]
        if (caaTruthy(item.reset)) {
            onReset()
        } else if (caaInstanceOf(item, js("Object"))) {
            callback(item.text)
        } else {
            callback(item)
        }
        js("undefined")
    })
    js("undefined")
}

// ---------------- 通用参数编辑对话框 ----------------

private fun caaEditParamDialogUi(
    asApi: dynamic,
    self: dynamic,
    e: dynamic,
    callback: dynamic,
    onReset: dynamic
) {
    var layout: dynamic = js("null")
    var title: dynamic = js("null")
    var ret: dynamic = js("null")
    var exit: dynamic = js("null")
    var popup: dynamic = js("null")
    var t: dynamic = js("null")
    val listener = caaObj()
    val suggestion = caaObj()

    if (!caaTruthy(self.initTextBox)) {
        self.initTextBox = { e2: dynamic, defVal: dynamic -> caaInitTextBox(e2, defVal) }
        self.initListener = { ret2: dynamic, l: dynamic, gText: dynamic ->
            caaInitListener(ret2, l, gText)
            js("undefined")
        }
    }

    layout = caaNewLinearLayout()
    layout.setOrientation(js("G.LinearLayout.VERTICAL"))
    layout.setPadding(caaDp(15), caaDp(15), caaDp(15), 0)
    js("Common").applyStyle(layout, "message_bg")

    title = caaNewTextView()
    title.setText(caaAdd(caaAdd("编辑“", e.param.name), "”"))
    title.setLayoutParams(caaNewLinearLayoutParams2(-1, -2))
    title.setPadding(0, 0, 0, caaDp(10))
    js("Common").applyStyle(title, "textview_default", 4)
    layout.addView(title)

    val p = e.param.type
    if (caaLooseEq(p, "int")) {
        ret = self.initTextBox(e)
        layout.addView(ret)
        ret.setInputType(caaBitOr(
            js("G.InputType.TYPE_CLASS_NUMBER"),
            js("G.InputType.TYPE_NUMBER_FLAG_SIGNED")
        ))
        self.initListener(ret, listener, { caaDialogIntText(ret) })
        js("Common").postIME(ret)
    } else if (caaLooseEq(p, "uint")) {
        ret = self.initTextBox(e)
        layout.addView(ret)
        ret.setInputType(js("G.InputType.TYPE_CLASS_NUMBER"))
        self.initListener(ret, listener, { caaDialogUintText(ret) })
        js("Common").postIME(ret)
    } else if (caaLooseEq(p, "float")) {
        ret = self.initTextBox(e)
        layout.addView(ret)
        ret.setInputType(caaBitOr(
            caaBitOr(
                js("G.InputType.TYPE_CLASS_NUMBER"),
                js("G.InputType.TYPE_NUMBER_FLAG_SIGNED")
            ),
            js("G.InputType.TYPE_NUMBER_FLAG_DECIMAL")
        ))
        self.initListener(ret, listener, { caaDialogFloatText(ret) })
        js("Common").postIME(ret)
    } else if (caaLooseEq(p, "relative")) {
        ret = self.initTextBox(e, if (caaIsNaN(e.offset)) "" else e.offset)
        layout.addView(ret)
        ret.setInputType(caaBitOr(
            caaBitOr(
                js("G.InputType.TYPE_CLASS_NUMBER"),
                js("G.InputType.TYPE_NUMBER_FLAG_SIGNED")
            ),
            js("G.InputType.TYPE_NUMBER_FLAG_DECIMAL")
        ))
        val rela = caaNewCheckBox()
        rela.setChecked(caaBoolean(e.isRela))
        rela.setLayoutParams(caaNewLinearLayoutParams3(-2, -2, 0))
        rela.getLayoutParams().setMargins(0, 0, 0, caaDp(10))
        rela.setText("启用相对参数")
        layout.addView(rela)
        listener.getText = { pure: dynamic -> caaRelativeGetText(e, ret, rela, pure) }
        listener.setText = { value: dynamic ->
            caaRelativeSetText(value, ret, rela)
            js("undefined")
        }
        // 原实现无条件调用 listener.onTextChanged；没有 suggestion 时该字段不存在，
        // 异常被下面的 catch 吞掉。这里保留同样行为。
        ret.addTextChangedListener(caaNewTextWatcher({ s: dynamic ->
            try {
                listener.onTextChanged()
            } catch (e2: dynamic) {
                js("erp")(e2)
            }
            js("undefined")
        }))
        js("Common").postIME(ret)
    } else if (caaLooseEq(p, "custom")) {
        ret = self.initTextBox(e)
        layout.addView(ret)
        ret.setInputType(js("G.InputType.TYPE_CLASS_TEXT"))
        self.initListener(ret, listener, { caaDialogCustomText(ret, e.param.finish) })
        js("Common").postIME(ret)
    } else {
        if (caaLooseEq(p, "command")) {
            js("CA").his.forEach({ h: dynamic ->
                suggestion[h] = h
                js("undefined")
            })
        }
        ret = self.initTextBox(e)
        layout.addView(ret)
        ret.setInputType(js("G.InputType.TYPE_CLASS_TEXT"))
        self.initListener(ret, listener, { caaDialogPlainText(ret) })
        js("Common").postIME(ret)
    }

    if (caaTruthy(e.param.suggestion)) {
        t = if (caaInstanceOf(e.param.suggestion, js("Object"))) {
            e.param.suggestion
        } else {
            js("CA").IntelliSense.library.enums[e.param.suggestion]
        }
        if (caaIsArray(t)) {
            caaForIn(t, { i: dynamic ->
                suggestion[t[i]] = t[i]
                js("undefined")
            })
        } else {
            caaForIn(t, { i: dynamic ->
                if (caaTruthy(t[i])) {
                    suggestion[caaAdd(caaAdd(i, " - "), t[i])] = i
                } else {
                    suggestion[i] = i
                }
                js("undefined")
            })
        }
    }

    if (caaTruthy(listener.setText)) {
        val suggKeys = caaObjectKeys(suggestion)
        js("Accel").setFilterTexts(suggKeys)
        val sugg = caaNewListView()
        val adpt = caaNewFilterListAdapter(
            caaNewSimpleListAdapter(suggKeys, asApi.smallVMaker, asApi.smallVBinder, js("undefined"))
        )
        sugg.setBackgroundColor(js("G.Color.TRANSPARENT"))
        sugg.setLayoutParams(caaNewLinearLayoutParams3(-1, 0, 1))
        sugg.setAdapter(adpt.build())
        sugg.setOnItemClickListener(caaNewItemClickListener({
            parent: dynamic,
            view: dynamic,
            pos: dynamic,
            id: dynamic ->
            try {
                listener.setText(suggestion[parent.getItemAtPosition(pos)])
            } catch (e2: dynamic) {
                js("erp")(e2)
            }
            js("undefined")
        }))
        layout.addView(sugg)
        if (caaLooseEq(js("G").style, "Material")) {
            sugg.setFastScrollEnabled(true)
            sugg.setFastScrollAlwaysVisible(false)
        }
        listener.onTextChanged = { s: dynamic ->
            caaParamFilterText(adpt, s)
            js("undefined")
        }
        if (caaTruthy(listener.getText)) listener.onTextChanged(listener.getText(true))
    }

    exit = caaNewTextView()
    exit.setLayoutParams(caaNewLinearLayoutParams2(-1, -2))
    exit.setText("确定")
    exit.setGravity(js("G.Gravity.CENTER"))
    exit.setPadding(caaDp(10), caaDp(10), caaDp(10), caaDp(15))
    js("Common").applyStyle(exit, "button_critical", 3)
    exit.setOnClickListener(caaNewClickListener({ _v: dynamic ->
        caaParamDialogOk(listener, onReset, callback, popup)
        js("undefined")
    }))
    layout.addView(exit)
    popup = js("PopupPage").showDialog("ca.assist.ParamEditor.Common", layout, -1, -2)
}

private fun caaEditParamDialog(
    asApi: dynamic,
    self: dynamic,
    e: dynamic,
    callback: dynamic,
    onReset: dynamic
) {
    js("G").ui({
        try {
            caaEditParamDialogUi(asApi, self, e, callback, onReset)
        } catch (e2: dynamic) {
            js("erp")(e2)
        }
        js("undefined")
    })
}

// ---------------- JSON 参数编辑 ----------------

private fun caaEditParamJSON(
    asApi: dynamic,
    self: dynamic,
    e: dynamic,
    callback: dynamic,
    onReset: dynamic
): dynamic {
    if (!caaTruthy(self.refresh)) {
        self.refresh = { e2: dynamic, data: dynamic, callback2: dynamic ->
            e2.jsonData = data
            callback2(MapScript.toSource(data))
            js("undefined")
        }
        self.modify = { e2: dynamic, callback2: dynamic ->
            val opts = caaObj()
            opts.source = e2.jsonData
            opts.rootname = e2.param.name
            opts.update = caaThis0Js { thisObj: dynamic ->
                self.refresh(e2, thisObj.source, callback2)
                js("undefined")
            }
            js("JSONEdit").show(opts)
            js("undefined")
        }
        self.buildnew = { e2: dynamic, callback2: dynamic ->
            js("JSONEdit").create({ data: dynamic ->
                self.refresh(e2, data, callback2)
                js("undefined")
            }, e2.param.name)
            js("undefined")
        }

        val edit = caaObj()
        edit.text = "编辑"
        edit.onclick = { _v: dynamic, tag: dynamic ->
            self.modify(tag.e, tag.callback)
            js("undefined")
        }
        val build = caaObj()
        build.text = "新建"
        build.onclick = { _v: dynamic, tag: dynamic ->
            self.buildnew(tag.e, tag.callback)
            js("undefined")
        }
        val reset = caaObj()
        reset.text = "重置参数"
        reset.onclick = { _v: dynamic, tag: dynamic ->
            tag.onReset()
            js("undefined")
        }
        reset.hidden = { tag: dynamic -> !caaTruthy(tag.onReset) }
        val cancel = caaObj()
        cancel.text = "取消"
        cancel.onclick = { _v: dynamic ->
            js("undefined")
        }
        self.editmenu = caaArr(edit, build, reset, cancel)
    }

    if (caaTruthy(e.param.component)) {
        return asApi.editComponent(e, callback, onReset)
    }
    if (caaIn("jsonData", e)) {
        val tag = caaObj()
        tag.e = e
        tag.callback = callback
        tag.onReset = onReset
        js("Common").showOperateDialog(self.editmenu, tag)
    } else {
        self.buildnew(e, callback)
    }
    return js("undefined")
}

// ---------------- 坐标参数编辑 ----------------

private fun caaPositionReset(onReset: dynamic, popup: dynamic) {
    try {
        onReset()
        popup.exit()
    } catch (e: dynamic) {
        js("erp")(e)
    }
}

private fun caaPositionOk(
    e: dynamic,
    ret: dynamic,
    rela: dynamic,
    screla: dynamic,
    posp: dynamic,
    callback: dynamic,
    popup: dynamic
) {
    try {
        val r = js("[]")
        e.screla = caaTruthy(js("CA").hasFeature("enableLocalCoord")) && caaTruthy(screla.isChecked())
        var i = 0
        while (i < 3) {
            e.pos[i] = caaParseFloat(ret[i].getText())
            e.rela[i] = rela[i].isChecked()
            if (!caaTruthy(e.screla) && !caaTruthy(e.rela[i]) && !caaIsFinite(e.pos[i])) {
                js("Common").toast(caaAdd(posp[i], "坐标不是数字！"))
                return
            }
            val prefix = if (caaTruthy(e.screla)) "^" else if (caaTruthy(e.rela[i])) "~" else ""
            val value = if (caaIsFinite(e.pos[i])) e.pos[i] else ""
            r.push(caaAdd(prefix, value))
            i++
        }
        callback(caaJoin(r, " "))
        popup.exit()
    } catch (e2: dynamic) {
        js("erp")(e2)
    }
}

private fun caaEditParamPositionUi(
    asApi: dynamic,
    self: dynamic,
    e: dynamic,
    callback: dynamic,
    onReset: dynamic
) {
    val scr = caaNewScrollView()
    js("Common").applyStyle(scr, "message_bg")

    val layout = caaNewTableLayout()
    layout.setPadding(caaDp(15), caaDp(15), caaDp(15), 0)

    val title = caaNewTextView()
    title.setText(caaAdd(caaAdd("编辑“", e.param.name), "”"))
    title.setLayoutParams(caaNewTableLayoutParams(-1, -2))
    title.setPadding(0, 0, 0, caaDp(10))
    js("Common").applyStyle(title, "textview_default", 4)
    layout.addView(title)

    if (!caaTruthy(e.pos)) {
        e.pos = js("[]")
        e.rela = js("[]")
    }

    val ret = js("[]")
    val rela = js("[]")
    val posp = caaArr("X", "Y", "Z")

    var i = 0
    while (i < 3) {
        val row = caaNewTableRow()
        row.setLayoutParams(caaNewTableLayoutParams(-1, -2))
        row.setGravity(js("G.Gravity.CENTER"))

        val label = caaNewTextView()
        label.setText(posp[i])
        label.setPadding(caaDp(10), caaDp(10), caaDp(10), caaDp(10))
        label.setLayoutParams(caaNewTableRowParams2(-1, -2))
        js("Common").applyStyle(label, "textview_default", 2)
        row.addView(label)

        ret[i] = caaNewEditText()
        ret[i].setText(if (caaIsNaN(e.pos[i])) "" else caaString(e.pos[i]))
        ret[i].setSingleLine(true)
        ret[i].setInputType(caaBitOr(
            caaBitOr(
                js("G.InputType.TYPE_CLASS_NUMBER"),
                js("G.InputType.TYPE_NUMBER_FLAG_SIGNED")
            ),
            js("G.InputType.TYPE_NUMBER_FLAG_DECIMAL")
        ))
        ret[i].setLayoutParams(caaNewTableRowParams3(0, -2, 1))
        ret[i].setSelection(ret[i].length())
        js("Common").applyStyle(ret[i], "edittext_default", 2)
        row.addView(ret[i])

        rela[i] = caaNewCheckBox()
        rela[i].setChecked(caaBoolean(e.rela[i]))
        rela[i].setLayoutParams(caaNewTableRowParams2(-2, -2))
        rela[i].getLayoutParams().setMargins(0, 0, caaDp(10), 0)
        // 原实现注释：CheckBox 需重新着色（保留原样）。
        rela[i].setText("~")
        row.addView(rela[i])
        layout.addView(row)
        i++
    }

    val screla = caaNewCheckBox()
    screla.setChecked(false)
    screla.setLayoutParams(caaNewTableLayoutParams(-1, -2))
    screla.getLayoutParams().setMargins(0, 0, 0, caaDp(10))
    screla.setText("使用局部坐标（^左 ^上 ^前）")
    screla.setOnCheckedChangeListener(caaNewCheckedChangeListener({
        _v: dynamic,
        s: dynamic ->
        try {
            var j = 0
            while (j < 3) {
                rela[j].setVisibility(if (caaTruthy(s)) js("G.View.GONE") else js("G.View.VISIBLE"))
                j++
            }
        } catch (e2: dynamic) {
            js("erp")(e2)
        }
        js("undefined")
    }))
    screla.setChecked(caaBoolean(e.screla))
    screla.setVisibility(
        if (caaTruthy(js("CA").hasFeature("enableLocalCoord"))) js("G.View.VISIBLE") else js("G.View.GONE")
    )
    layout.addView(screla)

    var popup: dynamic = js("null")

    if (caaTruthy(onReset)) {
        val reset = caaNewTextView()
        reset.setLayoutParams(caaNewTableLayoutParams(-1, -2))
        reset.setText("重置参数")
        reset.setGravity(js("G.Gravity.CENTER"))
        reset.setPadding(caaDp(10), caaDp(10), caaDp(10), caaDp(15))
        js("Common").applyStyle(reset, "button_critical", 3)
        reset.setOnClickListener(caaNewClickListener({ _v: dynamic ->
            caaPositionReset(onReset, popup)
            js("undefined")
        }))
        layout.addView(reset)
    }

    val exit = caaNewTextView()
    exit.setLayoutParams(caaNewTableLayoutParams(-1, -2))
    exit.setText("确定")
    exit.setGravity(js("G.Gravity.CENTER"))
    exit.setPadding(caaDp(10), caaDp(10), caaDp(10), caaDp(15))
    js("Common").applyStyle(exit, "button_critical", 3)
    exit.setOnClickListener(caaNewClickListener({ _v: dynamic ->
        caaPositionOk(e, ret, rela, screla, posp, callback, popup)
        js("undefined")
    }))
    layout.addView(exit)

    scr.addView(layout)
    popup = js("PopupPage").showDialog("ca.assist.ParamEditor.Position", scr, -1, -2)
}

private fun caaEditParamPosition(
    asApi: dynamic,
    self: dynamic,
    e: dynamic,
    callback: dynamic,
    onReset: dynamic
) {
    js("G").ui({
        try {
            caaEditParamPositionUi(asApi, self, e, callback, onReset)
        } catch (e2: dynamic) {
            js("erp")(e2)
        }
        js("undefined")
    })
}

// ---------------- 选择器参数编辑 ----------------

private fun caaSelectorEditLabel(self: dynamic, e: dynamic, callback: dynamic) {
    var a: dynamic = js("[]")
    val t = e.param.target
    if (caaLooseEq(t, "entity") || caaLooseEq(t, "player")) {
        a.push("@a")
        a.push("@p")
        a.push("@r")
    }
    if (caaLooseEq(t, "entity") || caaLooseEq(t, "nonplayer")) a.push("@e")
    if (caaLooseNe(t, "nonselector")) a.push("@s")
    a = caaMap(a, { item: dynamic ->
        val o = caaObj()
        o.text = item
        o.description = self.selectors[item]
        o
    })
    val custom = caaObj()
    custom.text = "玩家名"
    custom.description = "选择具有指定名称的玩家"
    custom.custom = true
    a.push(custom)

    js("Common").showListChooser(a, { pos: dynamic ->
        if (caaTruthy(a[pos].custom)) {
            val opts = caaObj()
            opts.title = "选择玩家名"
            opts.callback = { s: dynamic ->
                if (caaStartsWith(s, "@")) {
                    // 原实现先提示再 callback("")，保留这个行为。
                    js("Common").toast("玩家名不合法")
                    callback("")
                } else {
                    callback(s)
                }
                js("undefined")
            }
            opts.singleLine = true
            js("Common").showInputDialog(opts)
        } else {
            callback(a[pos].text)
        }
        js("undefined")
    })
    js("undefined")
}

private fun caaSelectorCheckPar(self: dynamic, label: dynamic, list: dynamic) {
    list.setVisibility(
        if (caaIn(label.getText(), self.selectors)) js("G.View.VISIBLE") else js("G.View.GONE")
    )
}

private fun caaSelectorRefresh(self: dynamic, e: dynamic, list: dynamic) {
    val params = caaObj()
    params.delete = { i: dynamic ->
        e.selpar.splice(i, 1)
        caaSelectorRefresh(self, e, list)
        js("undefined")
    }
    list.setAdapter(caaNewRhinoListAdapter(e.selpar, self.adapter, params))
}

private fun caaSelectorAddParam(asApi: dynamic, self: dynamic, e: dynamic, list: dynamic) {
    val a = js("[]")
    val ss = js("CA").IntelliSense.library.selectors
    caaForEach(caaObjectKeys(ss), { key: dynamic ->
        val item = caaObj()
        item.text = ss[key].name
        item.description = key
        item.name = key
        item.par = ss[key]
        item.inverted = false
        a.push(item)
        if (caaTruthy(ss[key].hasInverted)) {
            val inv = caaObj()
            inv.text = caaAdd("(不满足)", ss[key].name)
            inv.description = caaAdd("非", key)
            inv.name = key
            inv.par = ss[key]
            inv.inverted = true
            a.push(inv)
        }
        js("undefined")
    })
    js("Common").showListChooser(a, { pos: dynamic ->
        val p = caaObj()
        p.name = a[pos].name
        p.param = a[pos].par
        p.isInverted = a[pos].inverted
        asApi.editParam(p, { text: dynamic ->
            p.text = text
            e.selpar.push(p)
            caaSelectorRefresh(self, e, list)
            js("undefined")
        })
        js("undefined")
    })
    js("undefined")
}

private fun caaSelectorEditParam(asApi: dynamic, self: dynamic, e: dynamic, i: dynamic, list: dynamic) {
    asApi.editParam(e.selpar[i], { text: dynamic ->
        e.selpar[i].text = text
        caaSelectorRefresh(self, e, list)
        js("undefined")
    })
}

private fun caaSelectorAdapter(
    self: dynamic,
    e: dynamic,
    i: dynamic,
    a: dynamic,
    tag: dynamic
): dynamic {
    val view = caaNewLinearLayout()
    val text = caaNewTextView()
    val del = caaNewTextView()

    view.setLayoutParams(caaNewAbsListViewParams(-1, -2))
    view.setOrientation(js("G.LinearLayout.HORIZONTAL"))
    view.setLayoutParams(caaNewAbsListViewParams(-1, -2))

    text.setLayoutParams(caaNewLinearLayoutParams3(0, -2, 1))
    text.setText(caaAdd(
        caaAdd(
            caaAdd(if (caaTruthy(e.isInverted)) "(不满足)" else "", e.param.name),
            "："
        ),
        e.text
    ))
    text.setSingleLine(true)
    text.setEllipsize(js("G.TextUtils.TruncateAt.END"))
    text.setPadding(caaDp(10), caaDp(10), 0, caaDp(10))
    js("Common").applyStyle(text, "textview_default", 2)
    view.addView(text)

    del.setLayoutParams(caaNewLinearLayoutParams2(-2, -2))
    del.setText("×")
    del.setPadding(caaDp(10), caaDp(10), caaDp(10), caaDp(10))
    js("Common").applyStyle(del, "textview_default", 2)
    del.setOnClickListener(caaNewClickListener({ _v: dynamic ->
        try {
            tag.delete(i)
        } catch (e2: dynamic) {
            js("erp")(e2)
        }
        js("undefined")
    }))
    view.addView(del)
    return view
}

private fun caaSelectorReset(onReset: dynamic, popup: dynamic) {
    try {
        onReset()
        popup.exit()
    } catch (e: dynamic) {
        js("erp")(e)
    }
}

private fun caaSelectorOk(
    e: dynamic,
    self: dynamic,
    label: dynamic,
    list: dynamic,
    callback: dynamic,
    popup: dynamic
) {
    try {
        e.label = caaString(label.getText())
        if (!caaTruthy(e.label)) {
            js("Common").toast("选择器不可为空！")
            return
        }
        val suffix = if (caaIn(e.label, self.selectors) && caaLength(e.selpar) > 0) {
            val parts = caaMap(e.selpar, { item: dynamic ->
                caaAdd(
                    caaAdd(
                        caaAdd(item.name, "="),
                        if (caaTruthy(item.isInverted)) "!" else ""
                    ),
                    item.text
                )
            })
            caaAdd(caaAdd("[", caaJoin(parts, ",")), "]")
        } else {
            ""
        }
        callback(caaAdd(e.label, suffix))
        popup.exit()
    } catch (e2: dynamic) {
        js("erp")(e2)
    }
}

private fun caaEditParamSelectorUi(
    asApi: dynamic,
    self: dynamic,
    e: dynamic,
    callback: dynamic,
    onReset: dynamic
) {
    if (!caaTruthy(self.selectors)) {
        val selectors = caaObj()
        selectors["@a"] = "选择所有玩家"
        selectors["@p"] = "选择距离最近的玩家"
        selectors["@r"] = "选择随机玩家"
        selectors["@e"] = "选择所有实体"
        selectors["@s"] = "选择命令执行者"
        self.selectors = selectors

        self.editLabel = { e2: dynamic, callback2: dynamic ->
            caaSelectorEditLabel(self, e2, callback2)
            js("undefined")
        }
        self.checkPar = { label: dynamic, list: dynamic ->
            caaSelectorCheckPar(self, label, list)
            js("undefined")
        }
        self.refresh = { e2: dynamic, list: dynamic ->
            caaSelectorRefresh(self, e2, list)
            js("undefined")
        }
        self.addParam = { e2: dynamic, list: dynamic ->
            caaSelectorAddParam(asApi, self, e2, list)
            js("undefined")
        }
        self.editParam = { e2: dynamic, i: dynamic, list: dynamic ->
            caaSelectorEditParam(asApi, self, e2, i, list)
            js("undefined")
        }
        self.adapter = { e2: dynamic, i: dynamic, a: dynamic, tag: dynamic ->
            caaSelectorAdapter(self, e2, i, a, tag)
        }
    }

    val layout = caaNewLinearLayout()
    layout.setOrientation(js("G.LinearLayout.VERTICAL"))
    layout.setPadding(caaDp(15), caaDp(15), caaDp(15), 0)
    js("Common").applyStyle(layout, "message_bg")

    val title = caaNewTextView()
    title.setText(caaAdd(caaAdd("编辑“", e.param.name), "”"))
    title.setLayoutParams(caaNewLinearLayoutParams2(-1, -2))
    title.setPadding(0, 0, 0, caaDp(10))
    js("Common").applyStyle(title, "textview_default", 4)
    layout.addView(title)

    if (!caaTruthy(e.selpar)) e.selpar = js("[]")

    val label = caaNewEditText()
    label.setHint("点击以选择")
    label.setSingleLine(true)
    label.setPadding(0, 0, 0, caaDp(10))
    label.setInputType(js("G.InputType.TYPE_NULL"))
    label.setLayoutParams(caaNewLinearLayoutParams2(-1, -2))
    js("Common").applyStyle(label, "edittext_default", 2)

    val list = caaNewListView()
    label.setOnClickListener(caaNewClickListener({ v: dynamic ->
        try {
            self.editLabel(e, { text: dynamic ->
                js("G").ui({
                    try {
                        v.setText(text)
                        self.checkPar(v, list)
                    } catch (e2: dynamic) {
                        js("erp")(e2)
                    }
                    js("undefined")
                })
                js("undefined")
            })
        } catch (e2: dynamic) {
            js("erp")(e2)
        }
        js("undefined")
    }))
    if (caaTruthy(e.label)) {
        label.setText(e.label)
    } else {
        label.post({
            try {
                label.performClick()
            } catch (e2: dynamic) {
                js("erp")(e2)
            }
            js("undefined")
        })
    }
    layout.addView(label)

    val add = caaNewTextView()
    add.setText("+ 添加选择器参数")
    add.setSingleLine(true)
    add.setPadding(caaDp(10), caaDp(10), caaDp(10), caaDp(10))
    add.setLayoutParams(caaNewAbsListViewParams(-1, -2))
    js("Common").applyStyle(add, "textview_default", 2)

    list.setLayoutParams(caaNewLinearLayoutParams3(-1, 0, 1))
    list.addFooterView(add)
    list.setOnItemClickListener(caaNewItemClickListener({
        parent: dynamic,
        view: dynamic,
        pos: dynamic,
        id: dynamic ->
        try {
            if (caaLooseEq(view, add)) {
                self.addParam(e, parent)
            } else {
                self.editParam(e, pos, parent)
            }
        } catch (e2: dynamic) {
            js("erp")(e2)
        }
        js("undefined")
    }))
    layout.addView(list)

    var popup: dynamic = js("null")

    if (caaTruthy(onReset)) {
        val reset = caaNewTextView()
        reset.setLayoutParams(caaNewTableLayoutParams(-1, -2))
        reset.setText("重置参数")
        reset.setGravity(js("G.Gravity.CENTER"))
        reset.setPadding(caaDp(10), caaDp(10), caaDp(10), caaDp(15))
        js("Common").applyStyle(reset, "button_critical", 3)
        reset.setOnClickListener(caaNewClickListener({ _v: dynamic ->
            caaSelectorReset(onReset, popup)
            js("undefined")
        }))
        layout.addView(reset)
    }

    val exit = caaNewTextView()
    exit.setLayoutParams(caaNewLinearLayoutParams2(-1, -2))
    exit.setText("确定")
    exit.setGravity(js("G.Gravity.CENTER"))
    exit.setPadding(caaDp(10), caaDp(10), caaDp(10), caaDp(15))
    js("Common").applyStyle(exit, "button_critical", 3)
    exit.setOnClickListener(caaNewClickListener({ _v: dynamic ->
        caaSelectorOk(e, self, label, list, callback, popup)
        js("undefined")
    }))
    layout.addView(exit)

    self.checkPar(label, list)
    self.refresh(e, list)
    popup = js("PopupPage").showDialog("ca.assist.ParamEditor.Selector", layout, -1, -2)
}

private fun caaEditParamSelector(
    asApi: dynamic,
    self: dynamic,
    e: dynamic,
    callback: dynamic,
    onReset: dynamic
) {
    js("G").ui({
        try {
            caaEditParamSelectorUi(asApi, self, e, callback, onReset)
        } catch (e2: dynamic) {
            js("erp")(e2)
        }
        js("undefined")
    })
}

// ---------------- 组件参数编辑 ----------------

private fun caaComponentExtendComponent(self: dynamic, c: dynamic): dynamic {
    if (!caaTruthy(c)) return js("null")
    if (caaInstanceOf(c, js("Object"))) {
        val o = if (caaTruthy(c["extends"])) {
            caaComponentExtendComponent(self, c["extends"])
        } else {
            caaObj()
        }
        caaForIn(c, { i: dynamic ->
            o[i] = c[i]
            js("undefined")
        })
        return o
    }
    return caaComponentExtendComponent(self, js("CA").IntelliSense.library.json[c])
}

private fun caaComponentGetJSON(e: dynamic): dynamic {
    val p = e.param.type
    if (caaLooseEq(p, "nbt") || caaLooseEq(p, "json")) return e.jsonData
    if (caaLooseEq(p, "int") || caaLooseEq(p, "uint") || caaLooseEq(p, "float")) {
        return caaNumber(e.text)
    }
    return e.text
}

private fun caaComponentRefresh(self: dynamic, e: dynamic, adpt: dynamic) {
    adpt.notifyChange()
}

private fun caaComponentAddParam(asApi: dynamic, self: dynamic, e: dynamic, adpt: dynamic) {
    val a = js("[]")
    val cs = e.current_component
    if (caaLooseEq(cs.type, "object")) {
        caaForIn(cs.children, { i: dynamic ->
            val c = self.extendComponent(cs.children[i])
            val o = caaObj()
            o.text = c.name
            o.description = caaOr(c.description, i)
            o.data = c
            o.id = i
            a.push(o)
            js("undefined")
        })
    } else if (caaLooseEq(cs.type, "array")) {
        val c = self.extendComponent(cs.children)
        val o = caaObj()
        o.text = caaOr(c.name, "元素")
        o.description = c.description
        o.data = c
        a.push(o)
    }

    js("Common").showListChooser(a, { pos: dynamic ->
        val p = caaObj()
        p["_id"] = a[pos].id
        p.param = a[pos].data
        if (caaLooseEq(p.param.type, "object") || caaLooseEq(p.param.type, "array")) {
            val comp = caaObj()
            comp.type = "json"
            comp.name = a[pos].text
            comp.component = p.param
            p.param = comp
        }
        asApi.editParam(p, { text: dynamic ->
            p.text = text
            p.jsonData = self.getJSON(p)
            e.components.push(p)
            self.refresh(e, adpt)
            js("undefined")
        })
        js("undefined")
    }, true)
    js("undefined")
}

private fun caaComponentEditParam(
    asApi: dynamic,
    self: dynamic,
    e: dynamic,
    i: dynamic,
    adpt: dynamic
) {
    asApi.editParam(e.components[i], { text: dynamic ->
        val p = e.components[i]
        p.text = text
        p.jsonData = self.getJSON(p)
        self.refresh(e, adpt)
        js("undefined")
    })
}

private fun caaComponentVmaker(self: dynamic, holder: dynamic, params: dynamic): dynamic {
    val view = caaNewLinearLayout()
    val text = caaNewTextView()
    val del = caaNewTextView()
    holder.text = text

    view.setLayoutParams(caaNewAbsListViewParams(-1, -2))
    view.setOrientation(js("G.LinearLayout.HORIZONTAL"))
    view.setLayoutParams(caaNewAbsListViewParams(-1, -2))

    text.setLayoutParams(caaNewLinearLayoutParams3(0, -2, 1))
    text.setSingleLine(true)
    text.setEllipsize(js("G.TextUtils.TruncateAt.END"))
    text.setPadding(caaDp(10), caaDp(10), 0, caaDp(10))
    js("Common").applyStyle(text, "textview_default", 2)
    view.addView(text)

    del.setLayoutParams(caaNewLinearLayoutParams2(-2, -2))
    del.setText("×")
    del.setPadding(caaDp(10), caaDp(10), caaDp(10), caaDp(10))
    js("Common").applyStyle(del, "textview_default", 2)
    del.setOnClickListener(caaNewClickListener({ _v: dynamic ->
        try {
            params.delete(holder.pos)
        } catch (e2: dynamic) {
            js("erp")(e2)
        }
        js("undefined")
    }))
    view.addView(del)
    return view
}

private fun caaComponentVbinder(holder: dynamic, e: dynamic, i: dynamic, a: dynamic) {
    val prefix = if (caaTruthy(e._id)) caaAdd(e.param.name, "：") else ""
    holder.text.setText(caaAdd(prefix, e.text))
}

private fun caaComponentOk(e: dynamic, callback: dynamic, popup: dynamic) {
    try {
        var o: dynamic = js("undefined")
        if (caaLooseEq(e.current_component.type, "object")) {
            o = caaObj()
            caaForIn(e.components, { i: dynamic ->
                o[e.components[i]._id] = e.components[i].jsonData
                js("undefined")
            })
        } else if (caaLooseEq(e.current_component.type, "array")) {
            o = js("[]")
            caaForIn(e.components, { i: dynamic ->
                o.push(e.components[i].jsonData)
                js("undefined")
            })
        }
        e.jsonData = o
        callback(caaJsonStringify(o))
        popup.exit()
    } catch (e2: dynamic) {
        js("erp")(e2)
    }
}

private fun caaEditComponentUi(
    asApi: dynamic,
    self: dynamic,
    e: dynamic,
    callback: dynamic,
    onReset: dynamic
) {
    // 原实现的守卫是 self.selectors，但本函数从不设置它，所以每次进来都会重建内部函数。
    if (!caaTruthy(self.selectors)) {
        self.refresh = { e2: dynamic, adpt2: dynamic ->
            caaComponentRefresh(self, e2, adpt2)
            js("undefined")
        }
        self.addParam = { e2: dynamic, adpt2: dynamic ->
            caaComponentAddParam(asApi, self, e2, adpt2)
            js("undefined")
        }
        self.editParam = { e2: dynamic, i: dynamic, adpt2: dynamic ->
            caaComponentEditParam(asApi, self, e2, i, adpt2)
            js("undefined")
        }
        self.extendComponent = { c: dynamic ->
            caaComponentExtendComponent(self, c)
        }
        self.getJSON = { e2: dynamic -> caaComponentGetJSON(e2) }
        self.vmaker = { holder: dynamic, params: dynamic ->
            caaComponentVmaker(self, holder, params)
        }
        self.vbinder = { holder: dynamic, e2: dynamic, i: dynamic, a: dynamic ->
            caaComponentVbinder(holder, e2, i, a)
            js("undefined")
        }
    }

    val layout = caaNewLinearLayout()
    layout.setOrientation(js("G.LinearLayout.VERTICAL"))
    layout.setPadding(caaDp(15), caaDp(15), caaDp(15), 0)
    js("Common").applyStyle(layout, "message_bg")

    val title = caaNewTextView()
    title.setText(caaAdd(caaAdd("编辑“", e.param.name), "”"))
    title.setLayoutParams(caaNewLinearLayoutParams2(-1, -2))
    title.setPadding(0, 0, 0, caaDp(10))
    js("Common").applyStyle(title, "textview_default", 4)
    layout.addView(title)

    val add = caaNewTextView()
    add.setText("+ 添加组件")
    add.setSingleLine(true)
    add.setPadding(caaDp(10), caaDp(10), caaDp(10), caaDp(10))
    add.setLayoutParams(caaNewAbsListViewParams(-1, -2))
    js("Common").applyStyle(add, "button_default", 2)

    val list = caaNewListView()
    list.setLayoutParams(caaNewLinearLayoutParams3(-1, 0, 1))
    list.addFooterView(add)

    var adpt: dynamic = js("null")
    var popup: dynamic = js("null")

    list.setOnItemClickListener(caaNewItemClickListener({
        parent: dynamic,
        view: dynamic,
        pos: dynamic,
        id: dynamic ->
        try {
            if (caaLooseEq(view, add)) {
                self.addParam(e, adpt)
            } else {
                self.editParam(e, pos, adpt)
            }
        } catch (e2: dynamic) {
            js("erp")(e2)
        }
        js("undefined")
    }))
    layout.addView(list)

    if (caaTruthy(onReset)) {
        val reset = caaNewTextView()
        reset.setLayoutParams(caaNewTableLayoutParams(-1, -2))
        reset.setText("重置参数")
        reset.setGravity(js("G.Gravity.CENTER"))
        reset.setPadding(caaDp(10), caaDp(10), caaDp(10), caaDp(15))
        js("Common").applyStyle(reset, "button_critical", 3)
        reset.setOnClickListener(caaNewClickListener({ _v: dynamic ->
            caaSelectorReset(onReset, popup)
            js("undefined")
        }))
        layout.addView(reset)
    }

    val exit = caaNewTextView()
    exit.setLayoutParams(caaNewLinearLayoutParams2(-1, -2))
    exit.setText("确定")
    exit.setGravity(js("G.Gravity.CENTER"))
    exit.setPadding(caaDp(10), caaDp(10), caaDp(10), caaDp(15))
    js("Common").applyStyle(exit, "button_critical", 3)
    exit.setOnClickListener(caaNewClickListener({ _v: dynamic ->
        caaComponentOk(e, callback, popup)
        js("undefined")
    }))
    layout.addView(exit)

    if (!caaTruthy(e.components)) e.components = js("[]")
    val params = caaObj()
    params.delete = { i: dynamic ->
        e.components.splice(i, 1)
        self.refresh(e, adpt)
        js("undefined")
    }
    adpt = caaNewSimpleListAdapter(e.components, self.vmaker, self.vbinder, params)
    list.setAdapter(adpt)
    e.current_component = self.extendComponent(e.param.component)
    adpt = js("SimpleListAdapter").getController(adpt)
    self.refresh(e, adpt)
    popup = js("PopupPage").showDialog("ca.assist.ParamEditor.Component", layout, -1, -2)
}

private fun caaEditComponent(
    asApi: dynamic,
    self: dynamic,
    e: dynamic,
    callback: dynamic,
    onReset: dynamic
) {
    js("G").ui({
        try {
            caaEditComponentUi(asApi, self, e, callback, onReset)
        } catch (e2: dynamic) {
            js("erp")(e2)
        }
        js("undefined")
    })
}

// ---------------- 顶层 API ----------------

fun buildCaAssistModule(): dynamic {
    val asApi: dynamic = js("({})")

    asApi.active = false
    asApi.show = caaSelf0Js { self: dynamic ->
        caaShow(asApi, self)
        js("undefined")
    }
    asApi.hide = {
        caaHide(asApi)
        js("undefined")
    }
    asApi.paramAdapter = { e: dynamic, i: dynamic, a: dynamic ->
        caaParamAdapter(asApi, e, i, a)
    }
    asApi.refreshCommand = {
        caaRefreshCommand(asApi)
        js("undefined")
    }
    asApi.editParam = { e: dynamic, callback: dynamic, onReset: dynamic ->
        caaEditParam(asApi, e, callback, onReset)
        js("undefined")
    }
    asApi.editParamDialog = caaSelf3Js { self: dynamic, e: dynamic, callback: dynamic, onReset: dynamic ->
        caaEditParamDialog(asApi, self, e, callback, onReset)
        js("undefined")
    }
    asApi.editParamEnum = { e: dynamic, callback: dynamic, onReset: dynamic ->
        caaEditParamEnum(asApi, e, callback, onReset)
        js("undefined")
    }
    asApi.editParamJSON = caaSelf3Js { self: dynamic, e: dynamic, callback: dynamic, onReset: dynamic ->
        caaEditParamJSON(asApi, self, e, callback, onReset)
    }
    asApi.editParamPosition = caaSelf3Js { self: dynamic, e: dynamic, callback: dynamic, onReset: dynamic ->
        caaEditParamPosition(asApi, self, e, callback, onReset)
        js("undefined")
    }
    asApi.editParamSelector = caaSelf3Js { self: dynamic, e: dynamic, callback: dynamic, onReset: dynamic ->
        caaEditParamSelector(asApi, self, e, callback, onReset)
        js("undefined")
    }
    asApi.smallVMaker = { holder: dynamic ->
        val view = caaNewTextView()
        holder.view = view
        view.setPadding(caaDp(10), caaDp(10), caaDp(10), caaDp(10))
        view.setLayoutParams(caaNewAbsListViewParams(-1, -2))
        js("Common").applyStyle(view, "textview_default", 2)
        view
    }
    asApi.smallVBinder = { holder: dynamic, s: dynamic ->
        holder.view.setText(s)
        js("undefined")
    }
    asApi.getParamType = { cp: dynamic -> caaGetParamType(cp) }
    asApi.formatPattern = { cmd: dynamic, pattern: dynamic ->
        caaFormatPattern(asApi, cmd, pattern)
    }
    asApi.getPatternDescription = { cmd: dynamic, pattern: dynamic ->
        caaGetPatternDescription(asApi, cmd, pattern)
    }
    asApi.chooseCommand = { callback: dynamic, optional: dynamic ->
        caaChooseCommand(asApi, callback, optional)
        js("undefined")
    }
    asApi.choosePatterns = { cmd: dynamic, callback: dynamic, optional: dynamic ->
        caaChoosePatterns(asApi, cmd, callback, optional)
        js("undefined")
    }
    asApi.editComponent = caaSelf3Js { self: dynamic, e: dynamic, callback: dynamic, onReset: dynamic ->
        caaEditComponent(asApi, self, e, callback, onReset)
        js("undefined")
    }

    return asApi
}

// ---------------- 其余查询 / 选择逻辑 ----------------

private fun caaGetParamType(cp: dynamic): dynamic {
    val type = cp.type
    if (caaLooseEq(type, "int")) return "整数"
    if (caaLooseEq(type, "uint")) return "自然数"
    if (caaLooseEq(type, "float") || caaLooseEq(type, "relative")) return "数值"
    if (caaLooseEq(type, "nbt")) return "数据标签"
    if (caaLooseEq(type, "rawjson")) return "文本JSON"
    if (caaLooseEq(type, "json")) return "JSON"
    if (caaLooseEq(type, "selector")) return "实体"
    if (caaLooseEq(type, "enum")) return "列表"
    if (caaLooseEq(type, "plain")) return "常量"
    if (caaLooseEq(type, "custom")) {
        if (caaTruthy(cp.vtype)) return cp.vtype
        return "自定义类型"
    }
    if (caaLooseEq(type, "position")) return "坐标"
    if (caaLooseEq(type, "command")) return "命令"
    if (caaLooseEq(type, "subcommand")) return "子命令"
    return "文本"
}

private fun caaFormatPattern(asApi: dynamic, cmd: dynamic, pattern: dynamic): dynamic {
    val c = js("CA").IntelliSense.library.commands[cmd]
    val r = caaArr(caaAdd("/", cmd))
    if (caaTruthy(pattern)) {
        caaForEach(c.patterns[pattern].params, { item: dynamic ->
            r.push(js("CA").IntelliSense.getParamTag(item, js("null"), 0, js("null")))
            js("undefined")
        })
    }
    return caaJoin(r, " ")
}

private fun caaGetPatternDescription(asApi: dynamic, cmd: dynamic, pattern: dynamic): dynamic {
    val c = js("CA").IntelliSense.library.commands[cmd]
    val desc = if (caaTruthy(pattern)) {
        c.patterns[pattern].description
    } else {
        c.noparams.description
    }
    return caaOr(desc, c.description)
}

private fun caaChooseCommand(asApi: dynamic, callback: dynamic, optional: dynamic) {
    val lib = js("CA").IntelliSense.library
    val cmds = caaFilter(caaObjectKeys(lib.commands), { key: dynamic ->
        !caaTruthy(lib.commands[key].alias)
    })
    cmds.sort()
    if (caaLength(cmds) == 0) {
        js("Common").toast("没有可选的命令")
        return
    }
    val items = caaMap(cmds, { key: dynamic ->
        val o = caaObj()
        o.text = key
        o.description = lib.commands[key].description
        o
    })
    js("Common").showListChooser(items, { id: dynamic ->
        callback(cmds[id])
        js("undefined")
    }, optional)
}

private fun caaChoosePatterns(asApi: dynamic, cmd: dynamic, callback: dynamic, optional: dynamic) {
    val c = js("CA").IntelliSense.library.commands[cmd]
    if (!caaTruthy(c.patterns) && !caaTruthy(c.noparams)) {
        js("Common").toast("该命令不存在命令模式")
        return
    }
    val ps = if (caaTruthy(c.patterns)) caaObjectKeys(c.patterns) else js("[]")
    if (caaTruthy(c.noparams)) ps.unshift(js("null"))
    if (caaLength(ps) == 0) {
        js("Common").toast("没有可选的命令模式")
        return
    }
    val items = caaMap(ps, { item: dynamic ->
        val o = caaObj()
        o.text = asApi.formatPattern(cmd, item)
        o.description = asApi.getPatternDescription(cmd, item)
        o
    })
    js("Common").showListChooser(items, { id: dynamic ->
        callback(ps[id])
        js("undefined")
    }, optional)
}
