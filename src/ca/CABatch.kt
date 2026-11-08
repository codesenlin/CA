package ca.kotlin

/**
 * modules/CA.js batch 组的 Kotlin/JS 版本（原文件约 6478-7989 行，以成员名为准）。
 *
 * 只提供 installCaBatch(api)：把本组的顶层成员挂到调用方传入的 CA 共享 API 上；
 * 不注册模块、不定义全局 caApi、也不调用任何安装函数。
 *
 * 本组成员：
 *   SpecialTips / getTip / showBatchBuilder / BatchPattern /
 *   createParamTable / createParamRow / createParamTextbox /
 *   createVariableSelector / PluginMenu / PluginExpression / showActions。
 *
 * 原实现里 showBatchBuilder / showActions 是具名函数表达式 `function self(...)`，
 * 把 variables / clipbard / linear / popup / vmaker / vbinder 等状态缓存在函数
 * 对象自身。这里用薄 JS wrapper 生成同名同 arity 的 `function self(...)`，把真实
 * self 传回 Kotlin，跨次调用与其它组通过 CA.showBatchBuilder.xxx 的访问保持有效。
 *
 * 原来的 `this` 改成显式 api；跨组成员（Actions / addHistory / showHistory /
 * showFavEditDialog / history）只通过 api 动态解析，本文件不定义它们。
 *
 * 原 JS 的既有行为/毛病按原样保留，并在对应位置注释：
 *   - createParamTable 的 for 循环没有 var，会写全局 i（这里收成局部变量）；
 *   - range.export 的 to/from 比较、geometric.update 用 parseFloat 读整数项数；
 *   - synclist.export 在 return 后还有一条不可达的 return o.list；
 *   - expr 的 eval 字符串在原 CA 模块作用域执行，迁移后在 bundle 作用域执行，
 *     表达式里能看到的模块局部变量因此略有不同（全局 G/Common/CA 仍可见）。
 */

// ---------------- JS 语义 helper ----------------

private val cbTruthyJs: dynamic = js("function (v) { return !!v; }")
private fun cbTruthy(v: dynamic): Boolean = cbTruthyJs(v)

private val cbStringJs: dynamic = js("function (v) { return String(v); }")
private fun cbString(v: dynamic): dynamic = cbStringJs(v)

/** Java/JS 的 length 统一按 `| 0` 读取。 */
private val cbLengthJs: dynamic = js("function (v) { return v.length | 0; }")
private fun cbLength(v: dynamic): Int = cbLengthJs(v)

private val cbSetLengthJs: dynamic = js("function (v, n) { v.length = n; }")
private fun cbSetLength(v: dynamic, n: dynamic) {
    cbSetLengthJs(v, n)
}

private val cbAddJs: dynamic = js("function (a, b) { return a + b; }")
private fun cbAdd(a: dynamic, b: dynamic): dynamic = cbAddJs(a, b)

private val cbSubJs: dynamic = js("function (a, b) { return a - b; }")
private fun cbSub(a: dynamic, b: dynamic): dynamic = cbSubJs(a, b)

private val cbMulJs: dynamic = js("function (a, b) { return a * b; }")
private fun cbMul(a: dynamic, b: dynamic): dynamic = cbMulJs(a, b)

private val cbDivJs: dynamic = js("function (a, b) { return a / b; }")
private fun cbDiv(a: dynamic, b: dynamic): dynamic = cbDivJs(a, b)

private val cbModJs: dynamic = js("function (a, b) { return a % b; }")
private fun cbMod(a: dynamic, b: dynamic): dynamic = cbModJs(a, b)

private val cbNegJs: dynamic = js("function (v) { return -v; }")
private fun cbNeg(v: dynamic): dynamic = cbNegJs(v)

private val cbAbsJs: dynamic = js("function (v) { return Math.abs(v); }")
private fun cbAbs(v: dynamic): dynamic = cbAbsJs(v)

private val cbBitOrJs: dynamic = js("function (a, b) { return a | b; }")
private fun cbBitOr(a: dynamic, b: dynamic): dynamic = cbBitOrJs(a, b)

private val cbLooseEqJs: dynamic = js("function (a, b) { return a == b; }")
private fun cbLooseEq(a: dynamic, b: dynamic): Boolean = cbLooseEqJs(a, b)

private val cbLooseNeJs: dynamic = js("function (a, b) { return a != b; }")
private fun cbLooseNe(a: dynamic, b: dynamic): Boolean = cbLooseNeJs(a, b)

private val cbStrictEqJs: dynamic = js("function (a, b) { return a === b; }")
private fun cbStrictEq(a: dynamic, b: dynamic): Boolean = cbStrictEqJs(a, b)

private val cbLtJs: dynamic = js("function (a, b) { return a < b; }")
private fun cbLt(a: dynamic, b: dynamic): Boolean = cbLtJs(a, b)

private val cbGtJs: dynamic = js("function (a, b) { return a > b; }")
private fun cbGt(a: dynamic, b: dynamic): Boolean = cbGtJs(a, b)

private val cbLeJs: dynamic = js("function (a, b) { return a <= b; }")
private fun cbLe(a: dynamic, b: dynamic): Boolean = cbLeJs(a, b)

private val cbGeJs: dynamic = js("function (a, b) { return a >= b; }")
private fun cbGe(a: dynamic, b: dynamic): Boolean = cbGeJs(a, b)

private val cbIsNaNRawJs: dynamic = js("function (v) { return isNaN(v); }")
private fun cbIsNaN(v: dynamic): Boolean = cbIsNaNRawJs(v)

private val cbIsFiniteJs: dynamic = js("function (v) { return isFinite(v); }")
private fun cbIsFinite(v: dynamic): Boolean = cbIsFiniteJs(v)

private val cbParseIntJs: dynamic = js("function (v) { return parseInt(v); }")
private fun cbParseInt(v: dynamic): dynamic = cbParseIntJs(v)

private val cbParseFloatJs: dynamic = js("function (v) { return parseFloat(v); }")
private fun cbParseFloat(v: dynamic): dynamic = cbParseFloatJs(v)

private val cbTypeofJs: dynamic = js("function (v) { return typeof v; }")
private fun cbTypeof(v: dynamic): dynamic = cbTypeofJs(v)

private val cbIsArrayJs: dynamic = js("function (v) { return Array.isArray(v); }")
private fun cbIsArray(v: dynamic): Boolean = cbIsArrayJs(v)

private val cbIsObjectJs: dynamic = js("function (v) { return v instanceof Object; }")
private fun cbIsObject(v: dynamic): Boolean = cbIsObjectJs(v)

private val cbIsFunctionJs: dynamic = js("function (v) { return v instanceof Function; }")
private fun cbIsFunction(v: dynamic): Boolean = cbIsFunctionJs(v)

private val cbInJs: dynamic = js("function (k, o) { return k in o; }")
private fun cbIn(k: dynamic, o: dynamic): Boolean = cbInJs(k, o)

private val cbObjectKeysJs: dynamic = js("function (o) { return Object.keys(o); }")
private fun cbObjectKeys(o: dynamic): dynamic = cbObjectKeysJs(o)

private val cbObjectCreateJs: dynamic = js("function (p) { return Object.create(p); }")
private fun cbObjectCreate(p: dynamic): dynamic = cbObjectCreateJs(p)

private val cbIndexOfJs: dynamic = js("function (a, v, from) { return a.indexOf(v, from); }")
private fun cbIndexOf(a: dynamic, v: dynamic, from: dynamic): dynamic = cbIndexOfJs(a, v, from)

private val cbSliceJs: dynamic = js("function (s, a, b) { return s.slice(a, b); }")
private fun cbSlice(s: dynamic, a: dynamic, b: dynamic): dynamic = cbSliceJs(s, a, b)

private val cbSliceFromJs: dynamic = js("function (s, a) { return s.slice(a); }")
private fun cbSliceFrom(s: dynamic, a: dynamic): dynamic = cbSliceFromJs(s, a)

private val cbConcatJs: dynamic = js("function (a, b) { return a.concat(b); }")
private fun cbConcat(a: dynamic, b: dynamic): dynamic = cbConcatJs(a, b)

private val cbJoinJs: dynamic = js("function (a, s) { return a.join(s); }")
private fun cbJoin(a: dynamic, s: dynamic): dynamic = cbJoinJs(a, s)

private val cbSplitJs: dynamic = js("function (s, d) { return String(s).split(d); }")
private fun cbSplit(s: dynamic, d: dynamic): dynamic = cbSplitJs(s, d)

private val cbNewArrayJs: dynamic = js("function (n) { return new Array(n); }")
private fun cbNewArray(n: dynamic): dynamic = cbNewArrayJs(n)

private val cbArraySliceJs: dynamic = js("function (a) { return a.slice(); }")
private fun cbArraySlice(a: dynamic): dynamic = cbArraySliceJs(a)

private val cbSortJs: dynamic = js("function (a, f) { a.sort(f); }")
private fun cbSort(a: dynamic, f: dynamic) {
    cbSortJs(a, f)
}

private val cbMapJs: dynamic = js("function (a, f) { return a.map(f); }")
private fun cbMap(a: dynamic, f: dynamic): dynamic = cbMapJs(a, f)

private val cbFilterJs: dynamic = js("function (a, f) { return a.filter(f); }")
private fun cbFilter(a: dynamic, f: dynamic): dynamic = cbFilterJs(a, f)

private val cbForEachJs: dynamic = js("function (a, f) { a.forEach(f); }")
private fun cbForEach(a: dynamic, f: dynamic) {
    cbForEachJs(a, f)
}

private val cbForInJs: dynamic = js("function (o, f) { for (var k in o) f(k); }")
private fun cbForIn(o: dynamic, f: dynamic) {
    cbForInJs(o, f)
}

private val cbOrJs: dynamic = js("function (a, b) { return a || b; }")
private fun cbOr(a: dynamic, b: dynamic): dynamic = cbOrJs(a, b)

private val cbThis1Js: dynamic = js(
    "function (f) { return function (a) { return f(this, a); }; }"
)
private val cbThis2Js: dynamic = js(
    "function (f) { return function (a, b) { return f(this, a, b); }; }"
)
private val cbThis3Js: dynamic = js(
    "function (f) { return function (a, b, c) { return f(this, a, b, c); }; }"
)
private val cbThis0Js: dynamic = js(
    "function (f) { return function () { return f(this); }; }"
)

/** 生成具名函数表达式 `function self(...)`，与旧 API 的 arity/name 对齐。 */
private val cbMakeSelf2Js: dynamic = js(
    "function (f) { return function self(a, b) { return f(self, a, b); }; }"
)

private val cbNewRunnableJs: dynamic = js(
    "function (f) { return new java.lang.Runnable(f); }"
)
private fun cbNewRunnable(f: dynamic): dynamic = cbNewRunnableJs(f)

private val cbEvalJs: dynamic = js("function (c) { return eval(c); }")
private fun cbEval(c: dynamic): dynamic = cbEvalJs(c)

private val cbCall1Js: dynamic = js("function (f, t, a) { return f.call(t, a); }")
private fun cbCall1(f: dynamic, t: dynamic, a: dynamic): dynamic = cbCall1Js(f, t, a)

private val cbApplyJs: dynamic = js("function (f, t, a) { return f.apply(t, a); }")
private fun cbApply(f: dynamic, t: dynamic, a: dynamic): dynamic = cbApplyJs(f, t, a)

private val cbThrowJs: dynamic = js("function (e) { throw e; }")
private fun cbThrow(e: dynamic) {
    cbThrowJs(e)
}

private val cbDollarBrace: String = "$" + "{"

private val cbReplaceDollarSpaceJs: dynamic = js(
    """function (s) { return s.replace(/\$ /g, "$"); }"""
)
private fun cbReplaceDollarSpace(s: dynamic): dynamic = cbReplaceDollarSpaceJs(s)

private val cbReplaceDollarJs: dynamic = js(
    """function (s) { return s.replace(/\$/g, "$ "); }"""
)
private fun cbReplaceDollar(s: dynamic): dynamic = cbReplaceDollarJs(s)

private val cbNewlineToSpaceJs: dynamic = js(
    """function (s) { return s.replace(/\n/g, " "); }"""
)
private fun cbNewlineToSpace(s: dynamic): dynamic = cbNewlineToSpaceJs(s)

private val cbHasQuickAccelJs: dynamic = js(
    "function () { return typeof QuickAccel !== 'undefined'; }"
)
private fun cbHasQuickAccel(): dynamic = cbHasQuickAccelJs()

private fun cbObj(): dynamic = js("({})")

private fun cbArray(vararg items: dynamic): dynamic {
    val a = js("[]")
    var i = 0
    while (i < items.size) {
        a.push(items[i])
        i++
    }
    return a
}

// ---------------- Android UI helper ----------------

private val cbNewLinearLayoutJs: dynamic = js("function () { return new G.LinearLayout(ctx); }")
private val cbNewEditTextJs: dynamic = js("function () { return new G.EditText(ctx); }")
private val cbNewTextViewJs: dynamic = js("function () { return new G.TextView(ctx); }")
private val cbNewFrameLayoutJs: dynamic = js("function () { return new G.FrameLayout(ctx); }")
private val cbNewScrollViewJs: dynamic = js("function () { return new G.ScrollView(ctx); }")
private val cbNewListViewJs: dynamic = js("function () { return new G.ListView(ctx); }")
private val cbNewTableLayoutJs: dynamic = js("function () { return new G.TableLayout(ctx); }")
private val cbNewTableRowJs: dynamic = js("function () { return new G.TableRow(ctx); }")
private val cbNewPaintJs: dynamic = js("function () { return new G.Paint(); }")
private val cbNewRectJs: dynamic = js("function () { return new G.Rect(); }")
private val cbNewCanvasJs: dynamic = js("function (b) { return new G.Canvas(b); }")
private val cbNewSpannableStringBuilderJs: dynamic = js(
    "function () { return new G.SpannableStringBuilder(); }"
)
private val cbNewSpannableStringJs: dynamic = js(
    "function (s) { return new G.SpannableString(s); }"
)
private val cbNewImageSpanJs: dynamic = js(
    "function (c, b, a) { return new G.ImageSpan(c, b, a); }"
)
private val cbNewFrameParams2Js: dynamic = js(
    "function (w, h) { return new G.FrameLayout.LayoutParams(w, h); }"
)
private val cbNewLinearParams2Js: dynamic = js(
    "function (w, h) { return new G.LinearLayout.LayoutParams(w, h); }"
)
private val cbNewLinearParams3Js: dynamic = js(
    "function (w, h, weight) { return new G.LinearLayout.LayoutParams(w, h, weight); }"
)
private val cbNewTableParamsJs: dynamic = js(
    "function (w, h) { return new G.TableLayout.LayoutParams(w, h); }"
)
private val cbNewTableRowParams2Js: dynamic = js(
    "function (w, h) { return new G.TableRow.LayoutParams(w, h); }"
)
private val cbNewTableRowParams3Js: dynamic = js(
    "function (w, h, weight) { return new G.TableRow.LayoutParams(w, h, weight); }"
)
private val cbNewAbsParamsJs: dynamic = js(
    "function (w, h) { return new G.AbsListView.LayoutParams(w, h); }"
)
private val cbNewTouchListenerJs: dynamic = js(
    "function (f) { return new G.View.OnTouchListener({ onTouch: f }); }"
)
private val cbNewClickListenerJs: dynamic = js(
    "function (f) { return new G.View.OnClickListener({ onClick: f }); }"
)
private val cbNewItemClickListenerJs: dynamic = js(
    "function (f) { return new G.AdapterView.OnItemClickListener({ onItemClick: f }); }"
)
private val cbNewPopupPageJs: dynamic = js(
    "function (v, n) { return new PopupPage(v, n); }"
)
private val cbNewSimpleListAdapterJs: dynamic = js(
    "function (a, m, b) { return new SimpleListAdapter(a, m, b); }"
)

private fun cbNewLinearLayout(): dynamic = cbNewLinearLayoutJs()
private fun cbNewEditText(): dynamic = cbNewEditTextJs()
private fun cbNewTextView(): dynamic = cbNewTextViewJs()
private fun cbNewFrameLayout(): dynamic = cbNewFrameLayoutJs()
private fun cbNewScrollView(): dynamic = cbNewScrollViewJs()
private fun cbNewListView(): dynamic = cbNewListViewJs()
private fun cbNewTableLayout(): dynamic = cbNewTableLayoutJs()
private fun cbNewTableRow(): dynamic = cbNewTableRowJs()
private fun cbNewPaint(): dynamic = cbNewPaintJs()
private fun cbNewRect(): dynamic = cbNewRectJs()
private fun cbNewCanvas(b: dynamic): dynamic = cbNewCanvasJs(b)
private fun cbNewSpannableStringBuilder(): dynamic = cbNewSpannableStringBuilderJs()
private fun cbNewSpannableString(s: dynamic): dynamic = cbNewSpannableStringJs(s)
private fun cbNewImageSpan(c: dynamic, b: dynamic, a: dynamic): dynamic = cbNewImageSpanJs(c, b, a)
private fun cbNewFrameParams2(w: dynamic, h: dynamic): dynamic = cbNewFrameParams2Js(w, h)
private fun cbNewLinearParams2(w: dynamic, h: dynamic): dynamic = cbNewLinearParams2Js(w, h)
private fun cbNewLinearParams3(w: dynamic, h: dynamic, weight: dynamic): dynamic =
    cbNewLinearParams3Js(w, h, weight)
private fun cbNewTableParams(w: dynamic, h: dynamic): dynamic = cbNewTableParamsJs(w, h)
private fun cbNewTableRowParams2(w: dynamic, h: dynamic): dynamic = cbNewTableRowParams2Js(w, h)
private fun cbNewTableRowParams3(w: dynamic, h: dynamic, weight: dynamic): dynamic =
    cbNewTableRowParams3Js(w, h, weight)
private fun cbNewAbsParams(w: dynamic, h: dynamic): dynamic = cbNewAbsParamsJs(w, h)
private fun cbNewTouchListener(f: dynamic): dynamic = cbNewTouchListenerJs(f)
private fun cbNewClickListener(f: dynamic): dynamic = cbNewClickListenerJs(f)
private fun cbNewItemClickListener(f: dynamic): dynamic = cbNewItemClickListenerJs(f)
private fun cbNewPopupPage(v: dynamic, n: dynamic): dynamic = cbNewPopupPageJs(v, n)
private fun cbNewSimpleListAdapter(a: dynamic, m: dynamic, b: dynamic): dynamic =
    cbNewSimpleListAdapterJs(a, m, b)

private val cbDpJs: dynamic = js("function (n) { return n * G.dp; }")
private fun cbDp(n: Int): dynamic = cbDpJs(n)

private val cbNewDateJs: dynamic = js("function () { return new Date(); }")
private fun cbNewDate(): dynamic = cbNewDateJs()

// ---------------- SpecialTips / getTip ----------------

private fun cbBuildSpecialTips(): dynamic {
    val a = cbArray()
    a.push({ d: dynamic ->
        if (cbGt(d.getFullYear(), 2017) && cbLooseEq(d.getMonth(), 2) && cbLooseEq(d.getDate(), 20)) {
            cbAdd(cbAdd("命令助手", cbSub(d.getFullYear(), 2017)), "周年！感谢你们的支持！")
        } else {
            js("undefined")
        }
    })
    a.push({ d: dynamic ->
        if (cbLooseEq(d.getMonth(), 4) && cbLooseEq(d.getDate(), 1)) "劳动节快乐！" else js("undefined")
    })
    a.push({ d: dynamic ->
        if (cbLooseEq(d.getMonth(), 5) && cbLooseEq(d.getDate(), 1)) "儿童节快乐！" else js("undefined")
    })
    a.push({ d: dynamic ->
        if (cbLooseEq(d.getMonth(), 9) && cbLooseEq(d.getDate(), 1)) "国庆节快乐！" else js("undefined")
    })
    return a
}

private fun cbGetTip(api: dynamic): dynamic {
    val date = cbNewDate()
    val special = api.SpecialTips
    var i = 0
    while (i < cbLength(special)) {
        val t = special[i](date)
        if (cbTruthy(t)) return t
        i++
    }
    val read = api.settings.tipsRead
    api.settings.tipsRead = if (cbIsNaN(read)) 0 else cbMod(cbAdd(read, 1), cbLength(api.tips))
    return api.tips[api.settings.tipsRead]
}

// ---------------- showBatchBuilder ----------------

private fun cbShowBatchBuilder(api: dynamic, self: dynamic, text: dynamic, reset: dynamic) {
    js("G").ui({
        try {
            if (!cbTruthy(self.linear)) cbInitBatchBuilder(api, self)
            self.popup.enter()
            if (cbTruthy(reset) || !cbTruthy(self.edit.length())) self.init(cbOr(text, ""))
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

private fun cbInitBatchBuilder(api: dynamic, self: dynamic) {
    val vcfg = js("G.ViewConfiguration").get(js("ctx"))
    val longPressTimeout = vcfg.getLongPressTimeout()
    val touchSlop = vcfg.getScaledTouchSlop()

    self.variables = cbArray()
    self.clipboard = js("null")
    self.bmpcache = cbArray()
    self.labelOption = cbObj()
    self.labelOption.endChars = ":"
    self.labelOption.skipChars = ":"
    self.typeOption = cbObj()
    self.typeOption.endChars = "(}"
    self.typeOption.skipChars = "(}"

    val spanEdit = cbArray()
    val editItem = cbObj()
    editItem.text = "编辑"
    editItem.onclick = { _v: dynamic, tag: dynamic ->
        self.clickVariable(tag.variable)
        js("undefined")
    }
    spanEdit.push(editItem)

    val copyItem = cbObj()
    copyItem.text = "复制"
    copyItem.onclick = { _v: dynamic, tag: dynamic ->
        self.copyVariable(tag.variable)
        js("undefined")
    }
    spanEdit.push(copyItem)

    val cutItem = cbObj()
    cutItem.text = "剪切"
    cutItem.onclick = { _v: dynamic, tag: dynamic ->
        self.copyVariable(tag.variable)
        self.deleteVariable(tag.variable, true)
        js("undefined")
    }
    spanEdit.push(cutItem)

    val renameItem = cbObj()
    renameItem.text = "重命名"
    renameItem.onclick = { _v: dynamic, tag: dynamic ->
        val input = cbObj()
        input.title = "重命名"
        input.callback = { s: dynamic ->
            cbBatchRename(api, self, tag, s)
            js("undefined")
        }
        input.singleLine = true
        input.defaultValue = tag.variable.label
        js("Common").showInputDialog(input)
        js("undefined")
    }
    spanEdit.push(renameItem)

    val replaceItem = cbObj()
    replaceItem.text = "替换"
    replaceItem.onclick = { _v: dynamic, tag: dynamic ->
        self.makeVariable({ variable: dynamic ->
            self.replaceVariable(tag.variable, variable, true)
            js("undefined")
        })
        js("undefined")
    }
    spanEdit.push(replaceItem)

    val resetItem = cbObj()
    resetItem.text = "重置"
    resetItem.onclick = { _v: dynamic, tag: dynamic ->
        self.replaceVariable(
            tag.variable,
            self.createVariable(tag.variable.label, tag.variable.type),
            true
        )
        js("undefined")
    }
    spanEdit.push(resetItem)

    val deleteItem = cbObj()
    deleteItem.text = "删除"
    deleteItem.onclick = { _v: dynamic, tag: dynamic ->
        self.deleteVariable(tag.variable, true)
        js("undefined")
    }
    spanEdit.push(deleteItem)
    self.spanEdit = spanEdit

    self.init = { s: dynamic ->
        self.variables.length = 0
        self.edit.setText("")
        self.clearBmpCache()
        self.edit.setText(self.unflatten(s))
        self.edit.setSelection(self.edit.length())
        self.setContent(self.getDefaultContent())
        js("undefined")
    }
    self.setContent = { v: dynamic ->
        if (cbLooseNe(self.container.getChildAt(0), v)) {
            self.container.removeAllViews()
            self.container.addView(v, cbNewFrameParams2(-1, -1))
        }
        js("undefined")
    }
    self.clearBmpCache = {
        var i = cbLength(self.bmpcache) - 1
        while (i >= 0) {
            val e = self.bmpcache[i]
            if (cbTruthy(e)) e.recycle()
            i--
        }
        self.bmpcache.length = 0
        js("undefined")
    }
    self.unflatten = { str: dynamic -> cbBatchUnflatten(api, self, str) }
    self.makeVariable = { callback: dynamic -> cbBatchMakeVariable(api, self, callback) }
    self.chooseVariable = { callback: dynamic ->
        val a = cbMap(self.getVariables(), { e: dynamic ->
            val type = api.BatchPattern[e.type]
            val o = cbObj()
            o.text = cbAdd(cbAdd(e.label, " : "), type.name)
            o.data = e
            o
        })
        js("Common").showListChooser(a, { pos: dynamic ->
            callback(a[pos].data)
            js("undefined")
        }, true)
        js("undefined")
    }
    self.clickVariable = { variable: dynamic ->
        if (cbTruthy(variable.data.onClick)) variable.data.onClick()
        if (cbTruthy(variable.data.layout)) {
            self.setContent(variable.data.layout)
        } else {
            self.setContent(self.getDefaultContent())
        }
        js("undefined")
    }
    self.showVariableMenu = { variable: dynamic ->
        val tag = cbObj()
        tag.variable = variable
        js("Common").showOperateDialog(self.spanEdit, tag)
        js("undefined")
    }
    self.createImageSpan = { bgcolor: dynamic, frcolor: dynamic, fontsize: dynamic, text: dynamic ->
        cbBatchCreateImageSpan(self, bgcolor, frcolor, fontsize, text)
    }
    self.createVariable = { label: dynamic, type: dynamic ->
        val o = cbObj()
        o.label = label
        o.type = type
        o.data = api.BatchPattern[type].create()
        o
    }
    self.insertVariable = { o: dynamic, notify: dynamic ->
        self.variables.push(o)
        js("Common").replaceSelection(self.edit.getText(), self.buildSpan(o))
        if (cbTruthy(notify)) self.clickSpan(o.span)
        js("undefined")
    }
    self.replaceVariable = { old: dynamic, replacement: dynamic, notify: dynamic ->
        cbBatchReplaceVariable(self, old, replacement, notify)
    }
    self.deleteVariable = { o: dynamic, notify: dynamic ->
        cbBatchDeleteVariable(self, o, notify)
    }
    self.copyVariable = { variable: dynamic ->
        val type = api.BatchPattern[variable.type]
        self.clipboard = cbObj()
        self.clipboard.label = variable.label
        self.clipboard.type = variable.type
        self.clipboard.dataStr = type.stringify(variable.data)
        js("undefined")
    }
    self.pasteVariable = { -> cbBatchPasteVariable(api, self) }
    self.buildSpan = { o: dynamic -> cbBatchBuildSpan(api, self, o) }
    self.findVariableBySpan = { span: dynamic ->
        var i = 0
        var found: dynamic = js("null")
        while (i < cbLength(self.variables)) {
            val e = self.variables[i]
            if (cbLooseEq(e.span, span)) {
                found = e
                break
            }
            i++
        }
        found
    }
    self.findVariableByLabel = { label: dynamic, inline: dynamic ->
        val text = self.edit.getText()
        var i = 0
        var found: dynamic = js("null")
        while (i < cbLength(self.variables)) {
            val e = self.variables[i]
            if (cbLooseEq(e.label, label) && (!cbTruthy(inline) || cbGe(text.getSpanStart(e.span), 0))) {
                found = e
                break
            }
            i++
        }
        found
    }
    self.generateName = { prefix: dynamic, suffix: dynamic, notInline: dynamic ->
        var i: dynamic = 1
        while (cbTruthy(self.findVariableByLabel(cbAdd(cbAdd(prefix, i), suffix), !cbTruthy(notInline)))) {
            i = cbAdd(i, 1)
        }
        cbAdd(cbAdd(prefix, i), suffix)
    }
    self.endSpanEdit = {
        self.setContent(self.getDefaultContent())
        js("undefined")
    }
    self.getVariables = { -> cbBatchGetVariables(self) }
    self.flatten = { -> cbBatchFlatten(api, self) }
    self.concatStrBundle = { target: dynamic, string: dynamic ->
        cbBatchConcatStrBundle(target, string)
    }
    self.bundleStrings = { array: dynamic -> cbBatchBundleStrings(array) }
    self.export = { -> cbBatchExport(api, self) }

    val varsController = cbObj()
    varsController.create = cbThis3Js { ctl: dynamic, a: dynamic, b: dynamic, c: dynamic ->
        val o = cbObjectCreate(ctl)
        o.vars = a
        o.lineData = b
        o.globalData = c
        o
    }
    varsController.getBundleIndexByLabel = cbThis1Js { ctl: dynamic, label: dynamic ->
        var i = 0
        var r: dynamic = -1
        while (i < cbLength(ctl.vars)) {
            if (cbLooseEq(ctl.vars[i].label, label)) {
                r = cbAdd(cbMul(i, 2), 1)
                break
            }
            i++
        }
        r
    }
    varsController.getLabelByData = cbThis1Js { ctl: dynamic, data: dynamic ->
        var i = 0
        var r: dynamic = js("null")
        while (i < cbLength(ctl.vars)) {
            if (cbStrictEq(ctl.vars[i].data, data)) {
                r = ctl.vars[i].label
                break
            }
            i++
        }
        r
    }
    varsController.getLineData = cbThis2Js { ctl: dynamic, spec: dynamic, index: dynamic ->
        val t = ctl.lineData[index]
        if (cbTruthy(t)) {
            if (!cbTruthy(t[spec])) t[spec] = cbObj()
            t[spec]
        } else {
            val n = cbObj()
            ctl.lineData[index] = n
            n[spec] = cbObj()
            n[spec]
        }
    }
    varsController.getGlobalData = cbThis1Js { ctl: dynamic, spec: dynamic ->
        if (!cbTruthy(ctl.globalData[spec])) ctl.globalData[spec] = cbObj()
        ctl.globalData[spec]
    }
    self.varsController = varsController

    self.clickSpan = { span: dynamic ->
        val e = self.findVariableBySpan(span)
        if (cbTruthy(e)) self.clickVariable(e)
        js("undefined")
    }
    self.longClick = cbNewRunnable({
        try {
            if (cbTruthy(self.lcReady) && !cbTruthy(self.lcFinish)) {
                if (cbTruthy(self.lcTarget)) {
                    val variable = self.findVariableBySpan(self.lcTarget)
                    if (cbTruthy(variable)) self.showVariableMenu(variable)
                }
                self.lcFinish = true
            }
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
    self.getLink = { x: dynamic, y: dynamic -> cbBatchGetLink(self, x, y) }
    self.selectLink = { link: dynamic ->
        val buffer = self.edit.getText()
        js("G.Selection").setSelection(
            buffer,
            buffer.getSpanStart(link),
            buffer.getSpanEnd(link)
        )
        js("undefined")
    }
    self.menuVMaker = { holder: dynamic ->
        val view = cbNewTextView()
        view.setPadding(cbDp(15), cbDp(15), cbDp(15), cbDp(15))
        view.setLayoutParams(cbNewAbsParams(-1, -2))
        js("Common").applyStyle(view, "textview_default", 3)
        holder.text = view
        view
    }
    self.menuVBinder = { holder: dynamic, e: dynamic ->
        holder.text.setText(e.text)
        js("undefined")
    }
    self.buildMenuView = { arr: dynamic ->
        val list = cbNewListView()
        list.setAdapter(cbNewSimpleListAdapter(arr, self.menuVMaker, self.menuVBinder))
        list.setOnItemClickListener(cbNewItemClickListener({ parent: dynamic, _view: dynamic, pos: dynamic, _id: dynamic ->
            try {
                parent.getItemAtPosition(pos).onclick()
            } catch (e: dynamic) {
                js("erp")(e)
            }
            js("undefined")
        }))
        list
    }
    self.getDefaultContent = { -> cbBatchDefaultContent(api, self) }
    self.addExp = {
        if (cbGt(cbLength(self.getVariables()), 0)) js("UserManager").enqueueExp("createTemplate")
        js("undefined")
    }

    self.linear = cbNewLinearLayout()
    self.linear.setOrientation(js("G.LinearLayout.VERTICAL"))
    js("Common").applyStyle(self.linear, "container_default")
    self.header = cbNewLinearLayout()
    self.header.setOrientation(js("G.LinearLayout.HORIZONTAL"))
    self.header.setPadding(cbDp(5), 0, 0, 0)
    self.header.setLayoutParams(cbNewLinearParams2(-1, -2))
    js("Common").applyStyle(self.header, "bar_float")
    self.edit = cbNewEditText()
    self.edit.setSingleLine(true)
    self.edit.setLayoutParams(cbNewLinearParams3(0, -2, 1.0))
    self.edit.setImeOptions(js("G.EditorInfo.IME_FLAG_NO_FULLSCREEN"))
    self.edit.setTypeface(cbOr(js("G.Typeface.MONOSPACE"), js("G.Typeface.DEFAULT")))
    js("Common").applyStyle(self.edit, "edittext_default", 3)
    self.edit.setOnTouchListener(cbNewTouchListener({ v: dynamic, e: dynamic ->
        cbBatchEditTouch(self, v, e, touchSlop, longPressTimeout)
    }))
    self.header.addView(self.edit)

    self.exit = cbNewTextView()
    self.exit.setText("×")
    self.exit.setGravity(js("G.Gravity.CENTER"))
    self.exit.setPadding(cbDp(10), 0, cbDp(10), 0)
    self.exit.setLayoutParams(cbNewLinearParams2(-2, -1))
    js("Common").applyStyle(self.exit, "button_critical", 3)
    self.exit.setOnClickListener(cbNewClickListener({ _v: dynamic ->
        try {
            self.popup.exit()
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }))
    self.header.addView(self.exit)
    self.linear.addView(self.header)

    self.cscr = cbNewScrollView()
    self.cscr.setFillViewport(true)
    self.cscr.setLayoutParams(cbNewLinearParams2(-1, -1))
    self.container = cbNewFrameLayout()
    self.container.setLayoutParams(cbNewLinearParams2(-1, -1))
    self.cscr.addView(self.container)
    self.linear.addView(self.cscr)

    self.popup = cbNewPopupPage(self.linear, "ca.BatchBuilder")
    js("PWM").registerResetFlag(self, "linear")
}

private fun cbBatchRename(api: dynamic, self: dynamic, tag: dynamic, s: dynamic) {
    if (!cbTruthy(s)) {
        js("Common").toast("名称不能为空")
        return
    }
    if (cbTruthy(self.findVariableByLabel(s, true))) {
        js("Common").toast("名称已存在")
        return
    }
    tag.variable.label = s
    self.replaceVariable(tag.variable, tag.variable, true)
}

private fun cbBatchUnflatten(api: dynamic, self: dynamic, str: dynamic): dynamic {
    val r = cbNewSpannableStringBuilder()
    val error = cbArray()
    val stream = cbObj()
    stream.cur = 0
    stream.str = str
    var posStart: dynamic = cbIndexOf(str, cbDollarBrace, stream.cur)
    while (cbGe(posStart, 0)) {
        r.append(cbReplaceDollarSpace(cbSlice(str, stream.cur, posStart)))
        try {
            var cur: dynamic = cbObj()
            stream.cur = cbAdd(posStart, 2)
            cur.label = js("ISegment").readLenientString(stream, self.labelOption)
            if (cbTruthy(js("ISegment").isStringEOS(stream))) cbThrow("找不到变量类型")
            cur.type = js("ISegment").readLenientString(stream, self.typeOption)
            if (cbTruthy(js("ISegment").isStringEOS(stream))) cbThrow("找不到变量结束符")
            if (cbLength(cur.label) == 0) cbThrow("变量名称不能为空")
            if (cbTruthy(self.findVariableByLabel(cur.label, false))) {
                cur.label = self.generateName(cbAdd(cur.label, " ("), ")", true)
            }
            if (!cbIn(cur.type, api.BatchPattern)) cbThrow(cbAdd("不存在的变量类型：", cur.type))
            stream.cur = cbSub(stream.cur, 1)
            val endChar = js("ISegment").peekStreamStr(stream)
            if (cbLooseEq(endChar, "(")) {
                val match = api.BatchPattern[cur.type].parse(js("ISegment").peekStreamAll(stream))
                if (!cbTruthy(match)) cbThrow("变量参数格式错误")
                stream.cur = cbAdd(stream.cur, match.length)
                cur.data = match.data
            } else if (cbLooseEq(endChar, "}")) {
                cur = self.createVariable(cur.label, cur.type)
            }
            if (cbLooseNe(js("ISegment").readStreamStr(stream), "}")) cbThrow("找不到变量结束符")
            r.append(self.buildSpan(cur))
            self.variables.push(cur)
        } catch (e: dynamic) {
            error.push(cbAdd(cbAdd(e, "(位置："), cbAdd(posStart, ")")))
            r.append(cbDollarBrace)
            stream.cur = cbAdd(posStart, 2)
        }
        posStart = cbIndexOf(str, cbDollarBrace, stream.cur)
    }
    r.append(cbReplaceDollarSpace(js("ISegment").readStreamAll(stream)))
    if (cbGt(cbLength(error), 0)) js("Common").showTextDialog(cbJoin(error, "\n"))
    return r
}

private fun cbBatchMakeVariable(api: dynamic, self: dynamic, callback: dynamic) {
    val r = cbArray()
    val first = cbObj()
    first.text = "副本"
    first.description = "创建已有标签的副本"
    first.copy = true
    r.push(first)
    val a = api.BatchPattern
    if (cbTruthy(self.clipboard)) {
        val pasteItem = cbObj()
        pasteItem.text = "剪切板"
        pasteItem.description = cbAdd(
            cbAdd(self.clipboard.label, " : "),
            a[self.clipboard.type].name
        )
        pasteItem.paste = true
        r.push(pasteItem)
    }
    cbForIn(a, { i: dynamic ->
        val item = cbObj()
        item.text = a[i].name
        item.description = a[i].description
        item.obj = a[i]
        item.id = i
        r.push(item)
    })
    js("Common").showListChooser(r, { pos: dynamic ->
        cbBatchMakeVariableChoose(api, self, r, callback, pos)
        js("undefined")
    }, true)
}

private fun cbBatchMakeVariableChoose(
    api: dynamic,
    self: dynamic,
    r: dynamic,
    callback: dynamic,
    pos: dynamic
) {
    var e = r[pos]
    if (cbTruthy(e.copy)) {
        self.chooseVariable({ o: dynamic ->
            val type = api.BatchPattern[o.type]
            val v = cbObj()
            v.label = self.generateName(type.name, "")
            v.type = o.type
            v.data = if (cbTruthy(type.clone)) {
                type.clone(o.data)
            } else {
                type.parse(type.stringify(o.data)).data
            }
            callback(v)
            js("undefined")
        })
        return
    }
    if (cbTruthy(e.paste)) {
        e = self.pasteVariable()
        if (cbTruthy(self.findVariableByLabel(e.label, true))) {
            e.label = self.generateName(cbAdd(e.label, "("), ")")
        }
        callback(e)
        return
    }
    callback(self.createVariable(self.generateName(e.text, ""), e.id))
}

private fun cbBatchCreateImageSpan(
    self: dynamic,
    bgcolor: dynamic,
    frcolor: dynamic,
    fontsize: dynamic,
    text: dynamic
): dynamic {
    val margin = cbMul(2, js("G").dp)
    val padding = cbMul(4, js("G").dp)
    val offset = cbAdd(margin, padding)
    val pt = cbNewPaint()
    var size: dynamic = cbMul(fontsize, js("G").sp)
    pt.setAntiAlias(true)
    pt.setTextSize(size)
    pt.setTypeface(cbOr(js("G.Typeface.MONOSPACE"), js("G.Typeface.DEFAULT")))
    val fb = cbNewRect()
    val fm = pt.getFontMetrics()
    size = cbSub(size, cbDiv(cbMul(2, offset), cbSub(fm.descent, fm.ascent)))
    pt.setTextSize(size)
    pt.getFontMetrics(fm)
    pt.getTextBounds(text, 0, cbLength(text), fb)
    fb.top = fm.ascent
    fb.bottom = fm.descent
    val bmp = js("G.Bitmap").createBitmap(
        cbAdd(fb.width(), cbMul(2, offset)),
        cbAdd(fb.height(), cbMul(2, offset)),
        js("G.Bitmap.Config.ARGB_8888")
    )
    val cv = cbNewCanvas(bmp)
    val ox = cbNeg(fb.left)
    val oy = cbNeg(fb.top)
    js("IntColor").Paint.setColor(pt, bgcolor)
    fb.inset(cbNeg(padding), cbNeg(padding))
    fb.offsetTo(margin, margin)
    cv.drawRect(fb, pt)
    js("IntColor").Paint.setColor(pt, frcolor)
    cv.drawText(text, cbAdd(offset, ox), cbAdd(offset, oy), pt)
    self.bmpcache.push(bmp)
    return cbNewImageSpan(js("ctx"), bmp, 0) // 0 = ALIGN_BOTTOM
}

private fun cbBatchReplaceVariable(self: dynamic, old: dynamic, replacement: dynamic, notify: dynamic) {
    var p = self.variables.indexOf(old)
    val buffer = self.edit.getText()
    val oldSpan = old.span
    if (cbGe(p, 0)) {
        self.variables[p] = replacement
    } else {
        self.variables.push(replacement)
    }
    p = buffer.getSpanStart(oldSpan)
    if (cbGe(p, 0)) {
        buffer.replace(p, buffer.getSpanEnd(oldSpan), self.buildSpan(replacement))
        // 与被替换区域重合的 span 会被保留，在被替换区域内的 span 会被移除
        buffer.removeSpan(oldSpan)
    } else {
        js("Common").replaceSelection(buffer, self.buildSpan(replacement))
    }
    if (cbTruthy(notify)) self.clickSpan(replacement.span)
}

private fun cbBatchDeleteVariable(self: dynamic, o: dynamic, notify: dynamic) {
    var p = self.variables.indexOf(o)
    val buffer = self.edit.getText()
    if (cbGe(p, 0)) self.variables.splice(p, 1)
    p = buffer.getSpanStart(o.span)
    if (cbGe(p, 0)) {
        buffer.delete(p, buffer.getSpanEnd(o.span))
        // 与被删除区域重合的 span 会被保留，在被删除区域内的 span 会被移除
        buffer.removeSpan(o.span)
    }
    if (cbTruthy(notify)) self.endSpanEdit()
}

private fun cbBatchPasteVariable(api: dynamic, self: dynamic): dynamic {
    val e = self.clipboard
    if (!cbTruthy(e)) return js("undefined")
    val type = api.BatchPattern[e.type]
    val o = cbObj()
    o.label = e.label
    o.type = e.type
    o.data = type.parse(e.dataStr).data
    return o
}

private fun cbBatchBuildSpan(api: dynamic, self: dynamic, o: dynamic): dynamic {
    val ss = cbNewSpannableString(
        cbAdd(cbAdd(cbAdd(cbAdd(cbDollarBrace, o.label), ":"), o.type), "}")
    )
    val span = self.createImageSpan(
        js("Common").theme.highlightcolor,
        js("Common").theme.bgcolor,
        js("Common").theme.textsize[3],
        o.label
    )
    ss.setSpan(span, 0, ss.length(), js("G.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE"))
    o.span = span
    return ss
}

private fun cbBatchGetVariables(self: dynamic): dynamic {
    val template = self.edit.getText()
    val spans = template.getSpans(0, template.length(), js("G.ImageSpan"))
    val variables = cbArray()
    var i = 0
    while (i < cbLength(spans)) {
        val e = self.findVariableBySpan(spans[i])
        if (cbTruthy(e)) {
            e.start = template.getSpanStart(spans[i])
            e.end = template.getSpanEnd(spans[i])
            variables.push(e)
        }
        i++
    }
    cbSort(variables, { a: dynamic, b: dynamic -> cbSub(a.start, b.start) })
    return variables
}

private fun cbBatchFlatten(api: dynamic, self: dynamic): dynamic {
    val template = cbString(self.edit.getText())
    val vars = self.getVariables()
    val r = cbArray()
    var pos: dynamic = 0
    var i = 0
    while (i < cbLength(vars)) {
        r.push(cbReplaceDollar(cbSlice(template, pos, vars[i].start)))
        r.push(cbDollarBrace)
        r.push(js("ISegment").writeLenientString(vars[i].label, self.labelOption))
        r.push(":")
        r.push(js("ISegment").writeLenientString(vars[i].type, self.typeOption))
        r.push(api.BatchPattern[vars[i].type].stringify(vars[i].data))
        r.push("}")
        pos = vars[i].end
        i++
    }
    r.push(cbReplaceDollar(cbSlice(template, pos, template.length)))
    return cbJoin(r, "")
}

private fun cbBatchConcatStrBundle(target: dynamic, string: dynamic): dynamic {
    if (!cbTruthy(cbLength(target))) target.push("")
    var str: dynamic = string
    if (!cbIsArray(str)) str = cbArray(str)
    val dstLen = cbLength(target)
    val strLen = cbLength(str)
    if (cbGt(strLen, 1)) {
        cbSetLength(target, cbMul(cbLength(target), strLen))
        var i: dynamic = cbSub(dstLen, 1)
        while (cbGe(i, 0)) {
            var j: dynamic = 0
            while (cbLt(j, strLen)) {
                target[cbAdd(cbMul(i, strLen), j)] = target[i]
                j = cbAdd(j, 1)
            }
            i = cbSub(i, 1)
        }
    }
    var i: dynamic = 0
    while (cbLt(i, dstLen)) {
        var j: dynamic = 0
        while (cbLt(j, strLen)) {
            val idx = cbAdd(cbMul(i, strLen), j)
            target[idx] = cbAdd(target[idx], str[j])
            j = cbAdd(j, 1)
        }
        i = cbAdd(i, 1)
    }
    return target
}

private fun cbBatchBundleStrings(arrayIn: dynamic): dynamic {
    var array = arrayIn
    val msp = cbArray()
    val r = cbArray()
    var i = 0

    // 大批量导出：所有片段都是字符串/字符串数组时交给 QuickJS worker；
    // 含 map/syncmap/expr 函数时保留原 JS 路径。
    if (cbTruthy(cbHasQuickAccel()) && cbTruthy(js("QuickAccel").available)) {
        var estimate: dynamic = 1
        var serializable = true
        i = 0
        while (i < cbLength(array)) {
            val q = array[i]
            if (cbLooseEq(cbTypeof(q), "function")) {
                serializable = false
                break
            }
            if (cbTruthy(q) && cbLooseEq(cbTypeof(q), "object") && !cbIsArray(q)) {
                if (cbLooseEq(q.type, "map") && cbLooseEq(cbTypeof(q.index), "number")) {
                    i++
                    continue
                }
                if (cbLooseEq(q.type, "syncmap") && cbIsArray(q.arr) &&
                    cbLooseEq(cbTypeof(q.target), "number")
                ) {
                    i++
                    continue
                }
                serializable = false
                break
            }
            if (cbIsArray(q)) {
                estimate = cbMul(estimate, cbLength(q))
                if (cbGt(estimate, 2000)) break
            }
            i++
        }
        if (serializable && cbGt(estimate, 2000)) {
            val quick = js("QuickAccel").bundleStrings(array)
            if (cbTruthy(quick)) return quick
        }
    }

    i = 0
    while (i < cbLength(array)) {
        if (cbIsFunction(array[i])) array[i] = array[i]() // 懒计算
        if (cbIsArray(array[i])) msp.push(i) // 记录所有的片段位置
        i++
    }
    if (cbLength(msp) == 0) { // 处理片段数量为零的情况
        if (cbLength(array) == 0) array = cbArray("")
        array[0] = cbArray(array[0]) // 将 array[0] 转换为片段，便于处理
        msp.push(0)
    }
    val msi = cbNewArray(cbLength(msp)) // 每个片段的当前子片段索引
    val msn = cbNewArray(cbLength(msp)) // 每个片段的总子片段数
    i = 0
    while (i < cbLength(msp)) {
        msi[i] = 0
        msn[i] = cbLength(array[msp[i]])
        i++
    }
    while (cbLt(msi[0], msn[0])) {
        val cur = cbArraySlice(array) // 当前静态片段列表
        i = 0
        while (i < cbLength(msp)) {
            cur[msp[i]] = cur[msp[i]][msi[i]]
            i++
        } // 将每个片段替换为当前子片段
        i = 0
        while (i < cbLength(cur)) {
            if (cbLooseEq(cbTypeof(cur[i]), "object")) { // 处理特殊子片段
                when {
                    cbLooseEq(cur[i].type, "map") -> {
                        // 当前子片段的内容取决于当前静态子片段内容
                        cur[i] = cur[i].map(cur)
                    }
                    cbLooseEq(cur[i].type, "syncmap") -> {
                        // 当前子片段的内容取决于当前 target 指定的子片段内容、索引及子片段总数
                        val t = cur[i].target
                        val t2 = cbIndexOf(msp, t, 0)
                        cur[i] = cur[i].map(cur[t], msi[t2], msn[t2])
                    }
                    cbLooseEq(cur[i].type, "expr") -> {
                        // 当前子片段的内容为表达式计算的结果
                        cur[i] = cur[i].expr(i, cur, array, cbLength(r), msp, msi, msn)
                    }
                }
            }
            i++
        }
        r.push(cbJoin(cur, "")) // 生成当前静态片段文本并保存
        msi[cbLength(msi) - 1] = cbAdd(msi[cbLength(msi) - 1], 1) // 递增索引
        var j = cbLength(msi) - 1
        while (j > 0) {
            if (cbGe(msi[j], msn[j])) {
                msi[j] = 0
                msi[j - 1] = cbAdd(msi[j - 1], 1)
            } else {
                break
            }
            j--
        } // 处理索引进位
    }
    return r
}

private fun cbBatchExport(api: dynamic, self: dynamic): dynamic {
    val template = cbString(self.edit.getText())
    val vars = self.getVariables()
    val lineData = cbArray()
    val globalData = cbObj()
    val varsController = self.varsController.create(vars, lineData, globalData)
    val r = cbArray()
    var pos: dynamic = 0
    var i = 0
    while (i < cbLength(vars)) {
        r.push(cbSlice(template, pos, vars[i].start))
        r.push(api.BatchPattern[vars[i].type].export(vars[i].data, varsController))
        pos = vars[i].end
        i++
    }
    r.push(cbSlice(template, pos, template.length))
    return self.bundleStrings(r)
}

private fun cbBatchGetLink(self: dynamic, xIn: dynamic, yIn: dynamic): dynamic {
    val widget = self.edit
    val buffer = self.edit.getText()
    var x: dynamic = xIn
    var y: dynamic = yIn
    x = cbSub(x, widget.getTotalPaddingLeft())
    y = cbSub(y, widget.getTotalPaddingTop())
    x = cbAdd(x, widget.getScrollX())
    y = cbAdd(y, widget.getScrollY())
    val layout = widget.getLayout()
    val line = layout.getLineForVertical(y)
    val off = layout.getOffsetForHorizontal(line, x)
    val links = buffer.getSpans(off, off, js("G.ImageSpan"))
    return if (cbLt(x, layout.getLineMax(line)) && cbLength(links) != 0) {
        links[0]
    } else {
        js("null")
    }
}

private fun cbBatchEditTouch(
    self: dynamic,
    v: dynamic,
    e: dynamic,
    touchSlop: dynamic,
    longPressTimeout: dynamic
): dynamic {
    try {
        val x = e.getX()
        val y = e.getY()
        when {
            cbLooseEq(e.getAction(), e.ACTION_MOVE) -> {
                if (cbTruthy(self.lcStead)) {
                    if (cbLt(cbAdd(cbAbs(cbSub(x, self.lcLX)), cbAbs(cbSub(y, self.lcLY))), touchSlop)) {
                        return false
                    }
                    self.lcStead = false
                    self.lcReady = false
                }
            }
            cbLooseEq(e.getAction(), e.ACTION_DOWN) -> {
                self.lcLX = x
                self.lcLY = y
                self.lcStead = true
                self.lcFinish = false
                val link = self.getLink(x, y)
                self.lcTarget = link
                if (cbTruthy(link)) {
                    self.selectLink(link)
                    self.lcReady = true
                    v.postDelayed(self.longClick, longPressTimeout)
                    return true
                }
            }
            cbLooseEq(e.getAction(), e.ACTION_UP) -> {
                if (cbTruthy(self.lcStead) && !cbTruthy(self.lcFinish)) {
                    if (cbTruthy(self.lcTarget)) {
                        self.clickSpan(self.lcTarget)
                    } else {
                        self.endSpanEdit()
                    }
                }
                // 原实现 ACTION_UP 没有 break，会继续落入 ACTION_CANCEL 的收尾逻辑。
                self.lcReady = false
                v.removeCallbacks(self.longClick)
            }
            cbLooseEq(e.getAction(), e.ACTION_CANCEL) -> {
                self.lcReady = false
                v.removeCallbacks(self.longClick)
            }
        }
        return false
    } catch (e: dynamic) {
        js("erp")(e)
        return true
    }
}

private fun cbBatchDefaultContent(api: dynamic, self: dynamic): dynamic {
    if (cbTruthy(self.content_default)) return self.content_default
    val menu = cbArray()

    val addItem = cbObj()
    addItem.text = "添加/粘贴变量……"
    addItem.onclick = { ->
        self.makeVariable({ variable: dynamic ->
            self.insertVariable(variable, true)
            js("undefined")
        })
        js("undefined")
    }
    menu.push(addItem)

    val chooseItem = cbObj()
    chooseItem.text = "选择变量……"
    chooseItem.onclick = { ->
        self.chooseVariable({ o: dynamic ->
            self.selectLink(o.span)
            self.showVariableMenu(o)
            js("undefined")
        })
        js("undefined")
    }
    menu.push(chooseItem)

    val previewItem = cbObj()
    previewItem.text = "预览"
    previewItem.onclick = { ->
        if (!cbTruthy(self.edit.length())) {
            js("Common").toast("模板为空")
        } else {
            js("Common").showTextDialog(cbJoin(self.export(), "\n"))
        }
        js("undefined")
    }
    menu.push(previewItem)

    val historyItem = cbObj()
    historyItem.text = "保存至历史"
    historyItem.onclick = { ->
        if (!cbTruthy(self.edit.length())) {
            js("Common").toast("模板为空")
            js("undefined")
        } else {
            cbForEach(self.export(), { e: dynamic ->
                if (cbTruthy(cbLength(e))) api.addHistory(e)
                js("undefined")
            })
            if (cbTruthy(api.history)) api.showHistory()
            self.popup.exit()
            js("Common").toast("已保存至历史")
            js("undefined")
        }
    }
    menu.push(historyItem)

    val fileItem = cbObj()
    fileItem.text = "保存至函数文件"
    fileItem.onclick = { ->
        if (!cbTruthy(self.edit.length())) {
            js("Common").toast("模板为空")
            js("undefined")
        } else {
            self.addExp()
            val opts = cbObj()
            opts.mimeType = "text/plain"
            opts.hint = "generated.mcfunction"
            opts.export = { uri: dynamic ->
                try {
                    js("ExternalStorage").writeFileContent(
                        uri,
                        cbAdd(
                            cbAdd("# This file is spawned by CA\n# Template: ", self.flatten()),
                            cbAdd("\n\n", cbJoin(self.export(), "\n"))
                        )
                    )
                    js("Common").toast("函数文件已保存")
                } catch (e: dynamic) {
                    js("Common").toast(cbAdd("保存函数文件失败\n", e))
                }
                js("undefined")
            }
            js("ExternalStorage").showExportActions(opts)
            js("undefined")
        }
    }
    menu.push(fileItem)

    val favItem = cbObj()
    favItem.text = "收藏模板"
    favItem.onclick = { ->
        if (!cbTruthy(self.edit.length())) {
            js("Common").toast("模板为空")
            js("undefined")
        } else {
            val cmd = self.flatten()
            val data = cbObj()
            data.value = cmd
            data.source = "batch"
            val opts = cbObj()
            opts.mode = 0
            opts.data = data
            opts.callback = cbThis0Js { dlg: dynamic ->
                dlg.folder.children.push(dlg.data)
                js("Common").toast("模板已收藏")
                self.addExp()
                js("undefined")
            }
            opts.onDismiss = { ->
                if (cbTruthy(api.history)) api.showHistory()
                js("undefined")
            }
            api.showFavEditDialog(opts)
            js("undefined")
        }
    }
    menu.push(favItem)

    val clearItem = cbObj()
    clearItem.text = "清空并关闭"
    clearItem.onclick = { ->
        self.edit.setText("")
        self.popup.exit()
        js("undefined")
    }
    menu.push(clearItem)

    val view = self.buildMenuView(menu)
    self.content_default = view
    js("PWM").registerResetFlag(self, "content_default")
    return view
}

// ---------------- BatchPattern ----------------

private fun cbPatternOptions(
    endChars: dynamic,
    skipChars: dynamic,
    splitChar: dynamic,
    splitChars: dynamic
): dynamic {
    val o = cbObj()
    o.endChars = endChars
    o.skipChars = skipChars
    o.splitChar = splitChar
    o.splitChars = splitChars
    return o
}

private fun cbPatternOptions2(skipChars: dynamic, endChars: dynamic): dynamic {
    val o = cbObj()
    o.skipChars = skipChars
    o.endChars = endChars
    return o
}

private fun cbPatternMapFunc(thisObj: dynamic, e: dynamic, i: dynamic, n: dynamic): dynamic {
    return if (cbLt(i, cbLength(thisObj.arr))) thisObj.arr[i] else "{下标超出}"
}

private fun cbPatternCreateParse(
    p: dynamic,
    s: dynamic,
    makeData: dynamic
): dynamic {
    val o = cbObj()
    o.str = cbSliceFrom(s, 1)
    o.cur = 0
    val r = js("ISegment").readLenientStringArray(o, p.options)
    val res = cbObj()
    res.length = cbAdd(o.cur, 1)
    res.data = p.buildLayout(makeData(r))
    return res
}

private fun cbBuildPatternList(api: dynamic): dynamic {
    val p = cbObj()
    p.name = "列表"
    p.description = "包含了一系列参数的列表"
    p.options = cbPatternOptions(")", "|)", "|", "|")
    p.create = {
        val o = cbObj()
        o.list = cbArray()
        p.buildLayout(o)
    }
    p.parse = { s: dynamic ->
        cbPatternCreateParse(p, s, { r: dynamic ->
            val d = cbObj()
            d.list = r
            d
        })
    }
    p.stringify = { o: dynamic ->
        p.update(o)
        cbAdd(cbAdd("(", js("ISegment").writeLenientStringArray(o.list, p.options)), ")")
    }
    p.export = { o: dynamic ->
        p.update(o)
        o.list
    }
    p.buildLayout = { o: dynamic ->
        val opts = cbObj()
        opts.text = cbJoin(o.list, "\n")
        opts.hint = "每一行为列表的一个条目"
        opts.padding = cbArray(cbDp(10), cbDp(10), cbDp(10), cbDp(10))
        opts.gravity = js("L").Gravity("left|top")
        opts.imeOptions = js("L").EditorInfo("IME_FLAG_NO_FULLSCREEN")
        opts.style = "edittext_default"
        opts.fontSize = 3
        val edittext = js("L").EditText(opts)
        o.edittext = edittext
        o.layout = edittext
        o
    }
    p.update = { o: dynamic ->
        o.list = cbSplit(o.edittext.getText(), "\n")
        js("undefined")
    }
    return p
}

private fun cbBuildPatternSyncList(api: dynamic): dynamic {
    val p = cbObj()
    p.name = "同步列表"
    p.description = "与某个变量同步的列表"
    p.options = cbPatternOptions(")", "|)", "|", "|")
    p.create = {
        val o = cbObj()
        o.syncLabel = ""
        o.list = cbArray()
        p.buildLayout(o)
    }
    p.parse = { s: dynamic ->
        cbPatternCreateParse(p, s, { r: dynamic ->
            val d = cbObj()
            d.syncLabel = r[0]
            d.list = cbSliceFrom(r, 1)
            d
        })
    }
    p.stringify = { o: dynamic ->
        p.update(o)
        cbAdd(
            cbAdd("(", js("ISegment").writeLenientStringArray(
                cbConcat(cbArray(o.syncLabel), o.list),
                p.options
            )),
            ")"
        )
    }
    p.export = { o: dynamic, controller: dynamic ->
        p.update(o)
        val r = cbObj()
        r.type = "syncmap"
        r.arr = o.list
        r.target = controller.getBundleIndexByLabel(o.syncLabel)
        r.map = p.mapFunc
        // 原实现这里还有一条 return o.list，位于 return 之后，不可达。
        r
    }
    p.mapFunc = cbThis3Js { thisObj: dynamic, e: dynamic, i: dynamic, n: dynamic ->
        cbPatternMapFunc(thisObj, e, i, n)
    }
    p.buildLayout = { o: dynamic ->
        val selector = api.createVariableSelector({ label: dynamic ->
            o.syncLabel = label
            js("undefined")
        }, js("null"), o.syncLabel, "标签名")
        val labelOpts = cbObj()
        labelOpts.padding = cbArray(cbDp(10), cbDp(10), cbDp(10), cbDp(10))
        labelOpts.gravity = js("L").Gravity("left|top")
        labelOpts.imeOptions = js("L").EditorInfo("IME_FLAG_NO_FULLSCREEN")
        labelOpts.layoutWidth = -1
        labelOpts.layoutHeight = -2
        labelOpts.style = "edittext_default"
        labelOpts.fontSize = 3
        o.editlabel = js("L").attach(selector, labelOpts)

        val editOpts = cbObj()
        editOpts.text = cbJoin(o.list, "\n")
        editOpts.hint = "每一行为列表的一个条目"
        editOpts.padding = cbArray(cbDp(10), cbDp(10), cbDp(10), cbDp(10))
        editOpts.gravity = js("L").Gravity("left|top")
        editOpts.layoutWidth = -1
        editOpts.layoutHeight = -1
        editOpts.style = "edittext_default"
        editOpts.fontSize = 3
        o.edittext = js("L").EditText(editOpts)

        val cfg = cbObj()
        cfg.orientation = js("L").LinearLayout("vertical")
        cfg.children = cbArray(o.editlabel, o.edittext)
        o.layout = js("L").LinearLayout(cfg)
        o
    }
    p.update = { o: dynamic ->
        o.list = cbSplit(o.edittext.getText(), "\n")
        o.syncLabel = cbString(o.editlabel.getText())
        js("undefined")
    }
    return p
}

private fun cbBuildPatternParam(api: dynamic): dynamic {
    val p = cbObj()
    p.name = "固定参数"
    p.description = "仅含1个参数的列表"
    p.options = cbPatternOptions2(")", ")")
    p.create = {
        val o = cbObj()
        o.text = ""
        p.buildLayout(o)
    }
    p.parse = { s: dynamic ->
        val o = cbObj()
        o.str = cbSliceFrom(s, 1)
        o.cur = 0
        val r = js("ISegment").readLenientString(o, p.options)
        val res = cbObj()
        res.length = cbAdd(o.cur, 1)
        val d = cbObj()
        d.text = r
        res.data = p.buildLayout(d)
        res
    }
    p.stringify = { o: dynamic ->
        p.update(o)
        cbAdd(cbAdd("(", js("ISegment").writeLenientString(o.text, p.options)), ")")
    }
    p.export = { o: dynamic, controller: dynamic ->
        val param = controller.getGlobalData("param:global")
        p.update(o)
        var value: dynamic = o.text
        try {
            value = js("JSON").parse(value)
        } catch (e: dynamic) {
            // non-JSON value
        }
        param[controller.getLabelByData(o)] = value
        cbArray(o.text)
    }
    p.buildLayout = { o: dynamic ->
        val opts = cbObj()
        opts.text = o.text
        opts.hint = "在这里填入需要的参数"
        opts.singleLine = true
        opts.padding = cbArray(cbDp(10), cbDp(10), cbDp(10), cbDp(10))
        opts.gravity = js("L").Gravity("left|top")
        opts.style = "edittext_default"
        opts.fontSize = 3
        val edittext = js("L").EditText(opts)
        o.edittext = edittext
        o.layout = edittext
        o
    }
    p.update = { o: dynamic ->
        o.text = cbNewlineToSpace(cbString(o.edittext.getText()))
        js("undefined")
    }
    return p
}

private fun cbPatternRangeExport(p: dynamic, o: dynamic, controller: dynamic): dynamic {
    p.update(o)
    val from: dynamic = o.from
    val to: dynamic = o.to
    var step: dynamic = o.step
    var syncLabel: dynamic = o.syncLabel
    val r = cbArray()
    if (cbLooseEq(step, 0) || cbIsNaN(step)) step = 1
    if (cbLt(to, from) && cbGt(step, 0)) step = cbNeg(step)
    val t = cbDiv(cbSub(to, from), step)
    if (!cbIsFinite(step) || cbLt(t, 0)) {
        return cbArray("{参数不合法}")
    }
    var i: dynamic = 0
    while (cbLe(i, t)) {
        r.push(cbString(cbAdd(from, cbMul(step, i))))
        i = cbAdd(i, 1)
    }
    syncLabel = controller.getBundleIndexByLabel(syncLabel)
    if (cbGe(syncLabel, 0)) {
        val out = cbObj()
        out.type = "syncmap"
        out.arr = r
        out.target = syncLabel
        out.map = p.mapFunc
        return out
    }
    return r
}

private fun cbBuildPatternRange(api: dynamic): dynamic {
    val p = cbObj()
    p.name = "等差数列"
    p.description = "一个等差数列的列表"
    p.options = cbPatternOptions(")", "|)", "|", "|")
    p.create = {
        val o = cbObj()
        o.from = 1
        o.to = 10
        o.step = 1
        o.syncLabel = ""
        p.buildLayout(o)
    }
    p.parse = { s: dynamic ->
        cbPatternCreateParse(p, s, { r: dynamic ->
            val d = cbObj()
            d.from = cbParseFloat(r[0])
            d.to = cbParseFloat(r[1])
            d.step = if (cbTruthy(r[2])) cbParseFloat(r[2]) else 1
            d.syncLabel = cbOr(r[3], "")
            d
        })
    }
    p.stringify = { o: dynamic ->
        p.update(o)
        cbAdd(
            cbAdd("(", js("ISegment").writeLenientStringArray(
                cbArray(cbString(o.from), cbString(o.to), cbString(o.step), o.syncLabel),
                p.options
            )),
            ")"
        )
    }
    p.export = { o: dynamic, controller: dynamic ->
        cbPatternRangeExport(p, o, controller)
    }
    p.mapFunc = cbThis3Js { thisObj: dynamic, e: dynamic, i: dynamic, n: dynamic ->
        cbPatternMapFunc(thisObj, e, i, n)
    }
    p.buildLayout = { o: dynamic ->
        val inputType = cbBitOr(
            cbBitOr(
                js("G.InputType.TYPE_CLASS_NUMBER"),
                js("G.InputType.TYPE_NUMBER_FLAG_SIGNED")
            ),
            js("G.InputType.TYPE_NUMBER_FLAG_DECIMAL")
        )
        val fromOpts = cbObj()
        fromOpts.text = o.from
        fromOpts.inputType = inputType
        val fromBox = api.createParamTextbox(fromOpts)
        o._from = fromBox

        val toOpts = cbObj()
        toOpts.text = o.to
        toOpts.inputType = inputType
        val toBox = api.createParamTextbox(toOpts)
        o._to = toBox

        val stepOpts = cbObj()
        stepOpts.text = o.step
        stepOpts.inputType = inputType
        val stepBox = api.createParamTextbox(stepOpts)
        o._step = stepBox

        val syncOpts = cbObj()
        syncOpts.checked = cbGt(cbLength(o.syncLabel), 0)
        syncOpts.onCheckedChange = { _view: dynamic, checked: dynamic ->
            if (cbTruthy(checked)) {
                o._labelrow.visibility = js("G.View.VISIBLE")
            } else {
                o._labelrow.visibility = js("G.View.GONE")
                o._label.text = ""
                o.syncLabel = ""
            }
            js("undefined")
        }
        o._sync = js("L").CheckBox(syncOpts)

        val labelOpts = cbObj()
        labelOpts.text = o.syncLabel
        labelOpts.hint = "点击选择标签"
        val labelBox = api.createParamTextbox(labelOpts)
        val selector = api.createVariableSelector({ label: dynamic ->
            o.syncLabel = label
            js("undefined")
        }, labelBox)
        o._label = selector
        o._labelrow = api.createParamRow("同步标签", selector)

        o.layout = api.createParamTable(cbArray(
            api.createParamRow("开始计数", fromBox),
            api.createParamRow("结束计数", toBox),
            api.createParamRow("步长", stepBox),
            api.createParamRow("启用同步", o._sync),
            o._labelrow
        ))
        o._labelrow.visibility = if (cbGt(cbLength(o.syncLabel), 0)) {
            js("G.View.VISIBLE")
        } else {
            js("G.View.GONE")
        }
        o
    }
    p.update = { o: dynamic ->
        o.from = cbParseFloat(o._from.getText())
        o.to = cbParseFloat(o._to.getText())
        o.step = cbParseFloat(o._step.getText())
        js("undefined")
    }
    return p
}

private fun cbBuildPatternGeometric(api: dynamic): dynamic {
    val p = cbObj()
    p.name = "等比数列"
    p.description = "一个等比数列的列表"
    p.options = cbPatternOptions(")", "|)", "|", "|")
    p.create = {
        val o = cbObj()
        o.start = 1024
        o.count = 10
        o.scale = 0.5
        o.syncLabel = ""
        p.buildLayout(o)
    }
    p.parse = { s: dynamic ->
        cbPatternCreateParse(p, s, { r: dynamic ->
            val d = cbObj()
            d.start = cbParseFloat(r[0])
            d.count = cbParseInt(r[1])
            d.scale = if (cbTruthy(r[2])) cbParseFloat(r[2]) else 1
            d.syncLabel = cbOr(r[3], "")
            d
        })
    }
    p.stringify = { o: dynamic ->
        p.update(o)
        cbAdd(
            cbAdd("(", js("ISegment").writeLenientStringArray(
                cbArray(cbString(o.start), cbString(o.count), cbString(o.scale), o.syncLabel),
                p.options
            )),
            ")"
        )
    }
    p.export = { o: dynamic, controller: dynamic ->
        p.update(o)
        var start: dynamic = o.start
        var count: dynamic = o.count
        var scale: dynamic = o.scale
        var syncLabel: dynamic = o.syncLabel
        val r = cbArray()
        if (cbLooseEq(scale, 0) || cbIsNaN(scale)) scale = 1
        if (cbLt(count, 0)) {
            count = cbNeg(count)
            scale = cbDiv(1, scale)
        }
        var i: dynamic = 0
        var t = start
        while (cbLt(i, count)) {
            r.push(cbString(t))
            t = cbMul(t, scale)
            i = cbAdd(i, 1)
        }
        syncLabel = controller.getBundleIndexByLabel(syncLabel)
        if (cbGe(syncLabel, 0)) {
            val out = cbObj()
            out.type = "syncmap"
            out.arr = r
            out.target = syncLabel
            out.map = p.mapFunc
            out
        } else {
            r
        }
    }
    p.mapFunc = cbThis3Js { thisObj: dynamic, e: dynamic, i: dynamic, n: dynamic ->
        cbPatternMapFunc(thisObj, e, i, n)
    }
    p.buildLayout = { o: dynamic ->
        val inputType = cbBitOr(
            cbBitOr(
                js("G.InputType.TYPE_CLASS_NUMBER"),
                js("G.InputType.TYPE_NUMBER_FLAG_SIGNED")
            ),
            js("G.InputType.TYPE_NUMBER_FLAG_DECIMAL")
        )
        val countType = cbBitOr(
            js("G.InputType.TYPE_CLASS_NUMBER"),
            js("G.InputType.TYPE_NUMBER_FLAG_SIGNED")
        )
        val startOpts = cbObj()
        startOpts.text = o.start
        startOpts.inputType = inputType
        val startBox = api.createParamTextbox(startOpts)
        o._start = startBox

        val countOpts = cbObj()
        countOpts.text = o.count
        countOpts.inputType = countType
        val countBox = api.createParamTextbox(countOpts)
        o._count = countBox

        val scaleOpts = cbObj()
        scaleOpts.text = o.scale
        scaleOpts.inputType = inputType
        val scaleBox = api.createParamTextbox(scaleOpts)
        o._scale = scaleBox

        val syncOpts = cbObj()
        syncOpts.checked = cbGt(cbLength(o.syncLabel), 0)
        syncOpts.onCheckedChange = { _view: dynamic, checked: dynamic ->
            if (cbTruthy(checked)) {
                o._labelrow.visibility = js("G.View.VISIBLE")
            } else {
                o._labelrow.visibility = js("G.View.GONE")
                o._label.text = ""
                o.syncLabel = ""
            }
            js("undefined")
        }
        o._sync = js("L").CheckBox(syncOpts)

        val labelOpts = cbObj()
        labelOpts.text = o.syncLabel
        labelOpts.hint = "点击选择标签"
        val labelBox = api.createParamTextbox(labelOpts)
        val selector = api.createVariableSelector({ label: dynamic ->
            o.syncLabel = label
            js("undefined")
        }, labelBox)
        o._label = selector
        o._labelrow = api.createParamRow("同步标签", selector)

        o.layout = api.createParamTable(cbArray(
            api.createParamRow("首项", startBox),
            api.createParamRow("项数", countBox),
            api.createParamRow("公比", scaleBox),
            api.createParamRow("启用同步", o._sync),
            o._labelrow
        ))
        o._labelrow.visibility = if (cbGt(cbLength(o.syncLabel), 0)) {
            js("G.View.VISIBLE")
        } else {
            js("G.View.GONE")
        }
        o
    }
    p.update = { o: dynamic ->
        // 原实现用 parseFloat 读取项数（parseInt 只在 parse 时使用）。
        o.start = cbParseFloat(o._start.getText())
        o.count = cbParseFloat(o._count.getText())
        o.scale = cbParseFloat(o._scale.getText())
        js("undefined")
    }
    return p
}

private fun cbBuildPatternLink(api: dynamic): dynamic {
    val p = cbObj()
    p.name = "链接"
    p.description = "显示与指定标签相同的内容"
    p.options = cbPatternOptions2(")", ")")
    p.create = {
        val o = cbObj()
        o.label = ""
        p.buildLayout(o)
    }
    p.parse = { s: dynamic ->
        val o = cbObj()
        o.str = cbSliceFrom(s, 1)
        o.cur = 0
        val r = js("ISegment").readLenientString(o, p.options)
        val res = cbObj()
        res.length = cbAdd(o.cur, 1)
        val d = cbObj()
        d.label = r
        res.data = p.buildLayout(d)
        res
    }
    p.stringify = { o: dynamic ->
        cbAdd(cbAdd("(", js("ISegment").writeLenientString(o.label, p.options)), ")")
    }
    p.export = { o: dynamic, controller: dynamic ->
        val index = controller.getBundleIndexByLabel(o.label)
        val out = cbObj()
        out.type = "map"
        out.index = index
        out.map = { arr: dynamic ->
            if (cbLt(index, 0)) "{找不到标签}" else arr[index]
        }
        out
    }
    p.buildLayout = { o: dynamic ->
        val selector = api.createVariableSelector({ label: dynamic ->
            o.label = label
            js("undefined")
        }, js("null"), o.label, "标签名")
        val opts = cbObj()
        opts.padding = cbArray(cbDp(10), cbDp(10), cbDp(10), cbDp(10))
        opts.gravity = js("L").Gravity("left|top")
        opts.imeOptions = js("L").EditorInfo("IME_FLAG_NO_FULLSCREEN")
        opts.style = "edittext_default"
        opts.fontSize = 3
        val edittext = js("L").attach(selector, opts)
        o.edittext = edittext
        o.layout = edittext
        o
    }
    return p
}

private fun cbBuildExprVars(): dynamic {
    val keys = cbArray("i", "n", "global", "line", "param")
    val getters = cbArray(
        { prop: dynamic -> if (cbGe(prop.curSeqIndex, 0)) prop.seqIndex[prop.curSeqIndex] else 0 },
        { prop: dynamic -> prop.count },
        { prop: dynamic -> prop.controller.getGlobalData("expr:global") },
        { prop: dynamic -> prop.controller.getLineData("expr:line", prop.resultIndex) },
        { prop: dynamic -> prop.controller.getGlobalData("param:global") }
    )
    val descs = cbArray(
        "i - 当前子片段索引",
        "n - 子片段总数",
        "global - 全局变量",
        "line - 当前行变量",
        "param - 参数变量"
    )
    val o = cbObj()
    o.k = keys
    o.v = getters
    o.d = descs
    o.desc = cbJoin(descs, "\n")
    return o
}

private fun cbExecExpr(
    p: dynamic,
    f: dynamic,
    o: dynamic,
    controller: dynamic,
    prop: dynamic,
    srcIndex: dynamic,
    dstArray: dynamic,
    srcArray: dynamic,
    resultIndex: dynamic,
    seqPos: dynamic,
    seqIndex: dynamic,
    seqLen: dynamic
): dynamic {
    prop.data = o
    prop.controller = controller
    prop.srcIndex = srcIndex
    prop.dstArray = dstArray
    prop.srcArray = srcArray
    prop.resultIndex = resultIndex
    prop.seqPos = seqPos
    prop.seqIndex = seqIndex
    prop.seqLen = seqLen
    prop.curSeqIndex = cbIndexOf(seqPos, srcIndex, 0)
    try {
        val args = cbMap(p.vars.v, { e: dynamic -> cbCall1(e, js("null"), prop) })
        return cbApply(f, prop, args)
    } catch (e: dynamic) {
        return cbAdd(cbAdd("{", e), "}")
    }
}

private fun cbPatternExprExport(p: dynamic, o: dynamic, controller: dynamic): dynamic {
    p.update(o)
    var n: dynamic = js("undefined")
    var f: dynamic = js("undefined")
    try {
        val fn = cbEval(cbAdd(
            "function(global) {return (",
            cbAdd(o.count, ")}")
        ))
        n = cbParseInt(cbCall1(fn, js("null"), controller.getGlobalData("expr:global")))
        f = p.compileExpr(o.expr)
    } catch (e: dynamic) {
        return cbAdd(cbAdd("{", e), "}")
    }
    if (!(cbGt(n, 0) && cbLe(n, 10000))) {
        return cbAdd(cbAdd("{Error: Count cannot be ", n), "}")
    }
    val r = cbNewArray(n)
    val prop = cbObj()
    prop.count = n
    val item = cbObj()
    item.type = "expr"
    item.expr = {
            srcIndex: dynamic,
            dstArray: dynamic,
            srcArray: dynamic,
            resultIndex: dynamic,
            seqPos: dynamic,
            seqIndex: dynamic,
            seqLen: dynamic ->
        cbExecExpr(
            p, f, o, controller, prop, srcIndex, dstArray, srcArray,
            resultIndex, seqPos, seqIndex, seqLen
        )
    }
    r[0] = item
    var i: dynamic = 1
    while (cbLt(i, n)) {
        r[i] = item
        i = cbAdd(i, 1)
    }
    return r
}

private fun cbBuildPatternExpr(api: dynamic): dynamic {
    val p = cbObj()
    p.name = "表达式"
    p.description = "计算表达式的值"
    p.options = cbPatternOptions(")", "|)", "|", "|")
    p.create = {
        val o = cbObj()
        o.count = "10"
        o.expr = "i * 10"
        p.buildLayout(o)
    }
    p.parse = { s: dynamic ->
        cbPatternCreateParse(p, s, { r: dynamic ->
            val d = cbObj()
            d.expr = r[0]
            d.count = r[1]
            d
        })
    }
    p.stringify = { o: dynamic ->
        p.update(o)
        cbAdd(
            cbAdd("(", js("ISegment").writeLenientStringArray(
                cbArray(o.expr, o.count),
                p.options
            )),
            ")"
        )
    }
    p.export = { o: dynamic, controller: dynamic ->
        cbPatternExprExport(p, o, controller)
    }
    p.compileExpr = { expr: dynamic ->
        cbEval(cbAdd(
            cbAdd(cbAdd("function(", cbJoin(p.vars.k, ",")), "){return ("),
            cbAdd(expr, ")}")
        ))
    }
    p.execExpr = {
            f: dynamic,
            o: dynamic,
            controller: dynamic,
            prop: dynamic,
            srcIndex: dynamic,
            dstArray: dynamic,
            srcArray: dynamic,
            resultIndex: dynamic,
            seqPos: dynamic,
            seqIndex: dynamic,
            seqLen: dynamic ->
        cbExecExpr(
            p, f, o, controller, prop, srcIndex, dstArray, srcArray,
            resultIndex, seqPos, seqIndex, seqLen
        )
    }
    p.buildLayout = { o: dynamic ->
        val countOpts = cbObj()
        countOpts.text = o.count
        val countBox = api.createParamTextbox(countOpts)
        o._count = countBox

        val exprOpts = cbObj()
        exprOpts.text = o.expr
        exprOpts.hint = "在这里填入表达式"
        exprOpts.style = "edittext_default"
        exprOpts.fontSize = 2
        o._expr = js("L").EditText(exprOpts)

        val descOpts = cbObj()
        descOpts.text = p.vars.desc
        descOpts.padding = cbArray(cbDp(10), cbDp(10), cbDp(10), cbDp(10))
        descOpts.style = "textview_prompt"
        descOpts.fontSize = 2

        o.layout = api.createParamTable(cbArray(
            api.createParamRow("项数", countBox),
            api.createParamRow("表达式", o._expr),
            js("L").TextView(descOpts)
        ))
        o
    }
    p.update = { o: dynamic ->
        o.expr = cbString(o._expr.getText())
        o.count = cbString(o._count.getText())
        js("undefined")
    }
    p.vars = cbBuildExprVars()
    return p
}

private fun cbBuildBatchPattern(api: dynamic): dynamic {
    val bp = cbObj()
    bp.list = cbBuildPatternList(api)
    bp.synclist = cbBuildPatternSyncList(api)
    bp.param = cbBuildPatternParam(api)
    bp.range = cbBuildPatternRange(api)
    bp.geometric = cbBuildPatternGeometric(api)
    bp.link = cbBuildPatternLink(api)
    bp.expr = cbBuildPatternExpr(api)
    return bp
}

// ---------------- 参数表 helper ----------------

private fun cbCreateParamTable(table: dynamic): dynamic {
    val layout = cbNewTableLayout()
    layout.setPadding(cbDp(10), cbDp(10), cbDp(10), cbDp(10))
    // 原实现这里写的是 `for (i = 0; ...)`，会泄漏一个全局 i；这里收成局部变量。
    var i = 0
    while (i < cbLength(table)) {
        layout.addView(table[i], cbNewTableParams(-1, -2))
        i++
    }
    return layout
}

private fun cbCreateParamRow(text: dynamic, view: dynamic): dynamic {
    val row = cbNewTableRow()
    row.setGravity(js("G.Gravity.CENTER"))
    val label = cbNewTextView()
    label.setText(text)
    label.setPadding(cbDp(10), cbDp(10), cbDp(10), cbDp(10))
    label.setLayoutParams(cbNewTableRowParams2(-1, -2))
    js("Common").applyStyle(label, "textview_default", 2)
    row.addView(label)
    row.addView(view, cbNewTableRowParams3(0, -2, 1))
    return row
}

private fun cbCreateParamTextbox(o: dynamic): dynamic {
    val ret = cbNewEditText()
    ret.setText(if (cbTruthy(o.text)) cbString(o.text) else "")
    ret.setHint(if (cbTruthy(o.hint)) cbString(o.hint) else "")
    ret.setSingleLine(!cbTruthy(o.multiline))
    ret.setImeOptions(js("G.EditorInfo.IME_FLAG_NO_FULLSCREEN"))
    if (cbTruthy(o.inputType)) ret.setInputType(o.inputType)
    if (cbTruthy(o.keyListener)) ret.setKeyListener(o.keyListener)
    if (cbTruthy(o.transformationMethod)) ret.setTransformationMethod(o.transformationMethod)
    ret.setSelection(ret.length())
    js("Common").applyStyle(ret, "edittext_default", 2)
    return ret
}

private fun cbCreateVariableSelector(
    api: dynamic,
    onChange: dynamic,
    wrapped: dynamic,
    text: dynamic,
    hint: dynamic
): dynamic {
    // 原实现是 `wrapped || new G.EditText(ctx)`，需要短路，不能提前创建。
    val edit = if (cbTruthy(wrapped)) wrapped else cbNewEditText()
    if (!cbTruthy(wrapped)) {
        edit.setText(cbOr(text, ""))
        edit.setHint(cbOr(hint, "点击选择标签"))
    }
    edit.setImeOptions(js("G.EditorInfo.IME_FLAG_NO_FULLSCREEN"))
    edit.setInputType(js("G.InputType.TYPE_NULL"))
    edit.setOnClickListener(cbNewClickListener({ _v: dynamic ->
        cbVariableSelectorClick(api, edit, onChange)
    }))
    return edit
}

private fun cbVariableSelectorClick(api: dynamic, edit: dynamic, onChange: dynamic): dynamic {
    try {
        val o = api.showBatchBuilder
        if (!cbTruthy(o.chooseVariable)) return js("undefined")
        o.chooseVariable({ data: dynamic ->
            edit.setText(data.label)
            onChange(data.label)
            js("undefined")
        })
    } catch (e: dynamic) {
        js("erp")(e)
    }
    return js("undefined")
}

// ---------------- showActions ----------------

private fun cbShowActions(api: dynamic, self: dynamic, actions: dynamic, onDismiss: dynamic) {
    js("G").ui({
        cbShowActionsUi(api, self, actions, onDismiss)
        js("undefined")
    })
}

private fun cbShowActionsUi(api: dynamic, self: dynamic, actions: dynamic, onDismiss: dynamic) {
    try {
        if (!cbTruthy(self.vmaker)) {
            self.vmaker = js("UIHelp").settingItem
            self.vbinder = { holder: dynamic, e: dynamic ->
                cbShowActionsBinder(holder, e)
                js("undefined")
            }
        }
        val l = cbFilter(
            cbMap(actions, { e: dynamic -> cbShowActionsMap(api, e) }),
            { e: dynamic -> cbLooseNe(e.action, js("null")) }
        )
        if (cbLength(l) == 0) {
            js("Common").toast("没有可选的动作")
            return
        }
        val frame = cbNewFrameLayout()
        js("Common").applyStyle(frame, "message_bg")
        val list = cbNewListView()
        list.setLayoutParams(cbNewFrameParams2(-1, -2))
        list.setAdapter(cbNewSimpleListAdapter(l, self.vmaker, self.vbinder))
        var popup: dynamic = js("undefined")
        list.setOnItemClickListener(cbNewItemClickListener(
            { _parent: dynamic, _view: dynamic, pos: dynamic, _id: dynamic ->
                cbShowActionsClick(l, popup, pos)
            }
        ))
        frame.addView(list)
        popup = js("PopupWindow").showDialog("ca.selectAction", frame, -1, -2)
        js("PWM").addPopup(popup)
        if (cbTruthy(onDismiss)) popup.on("hide", onDismiss)
    } catch (e: dynamic) {
        js("erp")(e)
    }
}

private fun cbShowActionsMap(api: dynamic, e: dynamic): dynamic {
    val action = api.Actions[e.action]
    if (!cbTruthy(action)) return cbObj()
    val o = cbObj()
    o.text = if (cbTruthy(action.getName)) action.getName(e) else action.name
    o.description = if (cbTruthy(action.getDescription)) {
        action.getDescription(e)
    } else {
        action.description
    }
    o.action = if (cbTruthy(action.available) && !cbTruthy(action.available(e))) {
        js("null")
    } else {
        action
    }
    o.param = e
    return o
}

private fun cbShowActionsBinder(holder: dynamic, e: dynamic) {
    if (cbIsObject(e)) {
        holder.title.setText(js("Common").toString(e.text))
        if (cbTruthy(e.description)) {
            holder.desp.setText(js("Common").toString(e.description))
            holder.desp.setVisibility(js("G.View.VISIBLE"))
        } else {
            holder.desp.setVisibility(js("G.View.GONE"))
        }
    } else {
        holder.title.setText(js("Common").toString(e))
        holder.desp.setVisibility(js("G.View.GONE"))
    }
}

private fun cbShowActionsClick(l: dynamic, popup: dynamic, pos: dynamic): dynamic {
    try {
        val e = l[pos]
        if (!cbTruthy(e.action.execute(e.param))) popup.hide()
        return true
    } catch (e: dynamic) {
        js("erp")(e)
    }
    return js("undefined")
}

// ---------------- 安装 ----------------

fun installCaBatch(api: dynamic) {
    api.SpecialTips = cbBuildSpecialTips()
    api.getTip = { -> cbGetTip(api) }
    api.showBatchBuilder = cbMakeSelf2Js { self: dynamic, text: dynamic, reset: dynamic ->
        cbShowBatchBuilder(api, self, text, reset)
        js("undefined")
    }
    api.BatchPattern = cbBuildBatchPattern(api)
    api.createParamTable = { table: dynamic -> cbCreateParamTable(table) }
    api.createParamRow = { text: dynamic, view: dynamic -> cbCreateParamRow(text, view) }
    api.createParamTextbox = { o: dynamic -> cbCreateParamTextbox(o) }
    api.createVariableSelector = {
            onChange: dynamic,
            wrapped: dynamic,
            text: dynamic,
            hint: dynamic ->
        cbCreateVariableSelector(api, onChange, wrapped, text, hint)
    }
    api.PluginMenu = js("[]")
    api.PluginExpression = js("[]")
    api.showActions = cbMakeSelf2Js { self: dynamic, actions: dynamic, onDismiss: dynamic ->
        cbShowActions(api, self, actions, onDismiss)
        js("undefined")
    }
}
