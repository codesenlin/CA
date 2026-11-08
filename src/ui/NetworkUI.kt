package ca.kotlin

/**
 * modules/network/NetworkUI.js 的 Kotlin/JS 版本。
 *
 * NetworkUI 是网络模块共用的 UI 外壳：列表页、反馈状态菜单、详情页、
 * 评论条、编辑/表单对话框和通用两行列表项。
 * 调用方仍按 `self.xxx` 约定传状态对象，这里保持所有字段读写与回调时序。
 */
private val networkUiApi: dynamic = js("({})")

// ---------------- JS 语义 helper ----------------
private val nuTruthyJs: dynamic = js("function (v) { return !!v; }")
private val nuStringJs: dynamic = js("function (v) { return String(v); }")
private val nuLengthJs: dynamic = js("function (v) { return v.length | 0; }")
private val nuTypeofJs: dynamic = js("function (v) { return typeof v; }")
private val nuLooseEqJs: dynamic = js("function (a, b) { return a == b; }")
private val nuStrictNeJs: dynamic = js("function (a, b) { return a !== b; }")
private val nuNotUndefinedJs: dynamic = js("function (v) { return v !== undefined; }")
private val nuBitOrJs: dynamic = js("function (a, b) { return a | b; }")
private val nuDpJs: dynamic = js("function (n) { return n * G.dp; }")

private fun nuTruthy(v: dynamic): Boolean = nuTruthyJs(v)
private fun nuString(v: dynamic): dynamic = nuStringJs(v)
private fun nuLength(v: dynamic): Int = nuLengthJs(v)
private fun nuTypeof(v: dynamic): dynamic = nuTypeofJs(v)
private fun nuLooseEq(a: dynamic, b: dynamic): Boolean = nuLooseEqJs(a, b)
private fun nuNotUndefined(v: dynamic): Boolean = nuNotUndefinedJs(v)
private fun nuDp(n: Int): dynamic = nuDpJs(n)

private fun nuObject(): dynamic = js("({})")

private fun nuSize(width: dynamic, height: dynamic): dynamic {
    val o = js("({})")
    o.width = width
    o.height = height
    return o
}

private fun nuList(vararg items: dynamic): dynamic {
    val a = js("[]")
    var i = 0
    while (i < items.size) {
        a.push(items[i])
        i++
    }
    return a
}

// ---------------- Android / UI helper ----------------
private val nuNewLinearLayoutJs: dynamic = js("function (c) { return new G.LinearLayout(c); }")
private val nuNewTextViewJs: dynamic = js("function (c) { return new G.TextView(c); }")
private val nuNewListViewJs: dynamic = js("function (c) { return new G.ListView(c); }")
private val nuNewLinearParamsJs: dynamic = js(
    "function (w, h, weight) { return arguments.length > 2 ? new G.LinearLayout.LayoutParams(w, h, weight) : new G.LinearLayout.LayoutParams(w, h); }"
)
private val nuNewAbsListParamsJs: dynamic = js(
    "function (w, h) { return new G.AbsListView.LayoutParams(w, h); }"
)
private val nuNewPopupPageJs: dynamic = js("function (v, k) { return new PopupPage(v, k); }")
private val nuNewClickListenerJs: dynamic = js(
    "function (f) { return new G.View.OnClickListener({ onClick: f }); }"
)
private val nuNewItemClickListenerJs: dynamic = js(
    "function (f) { return new G.AdapterView.OnItemClickListener({ onItemClick: f }); }"
)

private fun nuNewLinearLayout(context: dynamic): dynamic = nuNewLinearLayoutJs(context)
private fun nuNewTextView(context: dynamic): dynamic = nuNewTextViewJs(context)
private fun nuNewListView(context: dynamic): dynamic = nuNewListViewJs(context)

private fun nuApplyStyle(view: dynamic, style: dynamic, fontSize: dynamic) {
    js("Common").applyStyle(view, style, fontSize)
}

private fun nuApplyStyle2(view: dynamic, style: dynamic) {
    js("function (v, s) { Common.applyStyle(v, s); }")(view, style)
}

// ---------------- 对话框回调（需要提前 return 的都写成具名函数） ----------------

private fun nuHeaderClick(self: dynamic, _v: dynamic): dynamic {
    try {
        js("Common").showOperateDialog(self.contextMenu)
        return true
    } catch (e: dynamic) {
        js("erp")(e)
    }
    return js("undefined")
}

private fun nuListItemClick(
    self: dynamic,
    onItemClick: dynamic,
    parent: dynamic,
    view: dynamic,
    pos: dynamic,
    _id: dynamic
): dynamic {
    try {
        if (nuLooseEq(view, self.more)) {
            self.appendPage()
            return js("undefined")
        }
        onItemClick(parent, parent.getAdapter().getItem(pos), view)
    } catch (e: dynamic) {
        js("erp")(e)
    }
    return js("undefined")
}

private fun nuExitClick(self: dynamic, _v: dynamic): dynamic {
    try {
        self.popup.exit()
    } catch (e: dynamic) {
        js("erp")(e)
    }
    return js("undefined")
}

private fun nuCloseButtonClick(popup: dynamic, _v: dynamic): dynamic {
    try {
        val target = if (nuTypeof(popup) == "function") popup() else popup
        if (nuTruthy(target)) target.exit()
    } catch (e: dynamic) {
        js("erp")(e)
    }
    return js("undefined")
}

private fun nuCommentSend(opts: dynamic, input: dynamic): dynamic {
    try {
        val s = nuString(input.text)
        if (!nuTruthy(s)) return js("Common").toast("内容不可为空！")
        opts.onSend(s, input)
    } catch (e: dynamic) {
        js("erp")(e)
    }
    return js("undefined")
}

private fun nuEditIssueSubmit(
    opts: dynamic,
    data: dynamic,
    field: dynamic,
    title: dynamic,
    content: dynamic,
    popup: dynamic
): dynamic {
    try {
        data.title = nuString(title.text)
        data[field] = nuString(content.text)
        if (!nuTruthy(data.title)) return js("Common").toast("标题不能为空！")
        if (opts.onSubmit) opts.onSubmit(data)
        popup.exit()
    } catch (e: dynamic) {
        js("erp")(e)
    }
    return js("undefined")
}

private fun nuFormButtonClick(fields: dynamic, button: dynamic, popup: dynamic, _v: dynamic): dynamic {
    try {
        button.onClick(fields, popup)
    } catch (e: dynamic) {
        js("erp")(e)
    }
    return js("undefined")
}

// ---------------- API ----------------

private fun nuCreateListPage(self: dynamic, title: dynamic, onItemClick: dynamic, optsIn: dynamic) {
    val opts = if (nuTruthy(optsIn)) optsIn else nuObject()
    val ctx = js("ctx")

    self.linear = nuNewLinearLayout(ctx)
    self.linear.setOrientation(js("G.LinearLayout.VERTICAL"))
    self.linear.setPadding(nuDp(15), nuDp(15), nuDp(15), 0)
    nuApplyStyle2(self.linear, "message_bg")

    self.header = nuNewLinearLayout(ctx)
    self.header.setOrientation(js("G.LinearLayout.HORIZONTAL"))
    self.header.setPadding(0, 0, 0, nuDp(10))
    self.header.setOnClickListener(nuNewClickListenerJs({ v: dynamic -> nuHeaderClick(self, v) }))

    self.title = nuNewTextView(ctx)
    self.title.setText(if (nuTruthy(title)) title else "")
    self.title.setGravity(js("G.Gravity.LEFT | G.Gravity.CENTER"))
    self.title.setPadding(nuDp(10), 0, nuDp(10), 0)
    nuApplyStyle(self.title, "textview_default", 4)
    self.header.addView(self.title, nuNewLinearParamsJs(0, -2, 1.0))

    self.menu = nuNewTextView(ctx)
    self.menu.setText("▼")
    self.menu.setPadding(nuDp(10), 0, nuDp(10), 0)
    self.menu.setGravity(js("G.Gravity.CENTER"))
    nuApplyStyle(self.menu, "button_highlight", 3)
    self.header.addView(self.menu, nuNewLinearParamsJs(-2, -1))
    self.linear.addView(self.header, nuNewLinearParamsJs(-1, -2))

    self.more = nuNewTextView(ctx)
    self.more.setGravity(js("G.Gravity.CENTER"))
    self.more.setPadding(nuDp(15), nuDp(15), nuDp(15), nuDp(15))
    self.more.setLayoutParams(nuNewAbsListParamsJs(-1, -2))
    nuApplyStyle(self.more, "textview_prompt", 2)

    self.list = nuNewListView(ctx)
    self.list.setAdapter(self.adpt.self)
    if (nuTruthy(opts.headerView)) self.list.addHeaderView(opts.headerView)
    if (nuNotUndefined(opts.dividerHeight)) self.list.setDividerHeight(opts.dividerHeight)
    self.list.setOnItemClickListener(nuNewItemClickListenerJs({ parent: dynamic, view: dynamic, pos: dynamic, id: dynamic ->
        nuListItemClick(self, onItemClick, parent, view, pos, id)
    }))
    self.linear.addView(self.list, nuNewLinearParamsJs(-1, 0, 1.0))

    self.exit = nuNewTextView(ctx)
    self.exit.setText("关闭")
    self.exit.setGravity(js("G.Gravity.CENTER"))
    self.exit.setPadding(nuDp(10), nuDp(20), nuDp(10), nuDp(20))
    nuApplyStyle(self.exit, "button_critical", 3)
    self.exit.setOnClickListener(nuNewClickListenerJs({ v: dynamic -> nuExitClick(self, v) }))
    self.linear.addView(self.exit, nuNewLinearParamsJs(-1, -2))

    self.popup = nuNewPopupPageJs(self.linear, if (nuTruthy(opts.popupKey)) opts.popupKey else "feedback.Issues")
    self.popup.on("exit", {
        if (nuTruthy(self.callback)) self.callback()
        js("undefined")
    })
    js("PWM").registerResetFlag(self, if (nuTruthy(opts.resetFlag)) opts.resetFlag else "linear")
}

private fun nuIssueStateMenu(self: dynamic): dynamic {
    return nuList(
        js("NetworkUI").issueStateMenuEntry(self, "open"),
        js("NetworkUI").issueStateMenuEntry(self, "all")
    )
}

private fun nuIssueStateMenuEntry(self: dynamic, state: dynamic): dynamic {
    val entry = nuObject()
    entry.text = if (nuLooseEq(state, "open")) "仅显示未处理" else "显示所有"
    entry.hidden = {
        nuLooseEq(self.issueState, state)
    }
    entry.onclick = { _v: dynamic, _tag: dynamic ->
        self.issueState = state
        self.reload()
        js("undefined")
    }
    return entry
}

private fun nuCloseButton(popup: dynamic): dynamic {
    val config = nuObject()
    config.text = "关闭"
    config.padding = nuList(nuDp(10), nuDp(20), nuDp(10), nuDp(20))
    config.gravity = js("L").Gravity("center")
    config.layout = nuSize(-1, -2)
    config.style = "button_critical"
    config.fontSize = 3
    config.onClick = { v: dynamic -> nuCloseButtonClick(popup, v) }
    return js("L").TextView(config)
}

private fun nuAppendPage(self: dynamic, sync: dynamic, opts: dynamic): dynamic {
    if (!nuTruthy(sync)) {
        return js("NetworkUI").progress(
            if (nuTruthy(opts.loadingText)) opts.loadingText else "正在加载……",
            {
                self.appendPage(true)
                js("undefined")
            }
        )
    }
    if (nuTruthy(self.loading)) return js("Common").toast("正在加载中……")
    if (nuTruthy(opts.canLoad) && !nuTruthy(opts.canLoad())) return js("undefined")
    self.loading = true
    var page: dynamic = js("undefined")
    try {
        page = opts.fetch()
    } catch (e: dynamic) {
        js("Log").e(e)
    }
    if (!nuTruthy(page)) {
        self.loading = false
        return js("Common").toast(if (nuTruthy(opts.failText)) opts.failText else "加载失败")
    }
    js("G").ui({
        try {
            opts.apply(page)
            self.loading = false
            if (nuTruthy(opts.afterApply)) opts.afterApply(page)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
    return js("undefined")
}

private fun nuUpdateMore(self: dynamic, hasNext: dynamic, text: dynamic, useHeader: dynamic) {
    if (nuTruthy(text)) self.more.setText(text)
    if (nuTruthy(hasNext) && !nuTruthy(self.moreVisible)) {
        self.moreVisible = true
        if (nuTruthy(useHeader)) self.list.addHeaderView(self.more) else self.list.addFooterView(self.more)
    } else if (!nuTruthy(hasNext) && nuTruthy(self.moreVisible)) {
        self.moreVisible = false
        if (nuTruthy(useHeader)) self.list.removeHeaderView(self.more) else self.list.removeFooterView(self.more)
    }
}

private fun nuProgress(text: dynamic, task: dynamic): dynamic {
    val dialog = js("Common").showProgressDialog()
    if (nuTruthy(text)) dialog.setText(text)
    dialog.async(task)
    return dialog
}

private fun nuIssueDetailPage(self: dynamic, children: dynamic, optsIn: dynamic): dynamic {
    val opts = if (nuTruthy(optsIn)) optsIn else nuObject()
    val config = nuObject()
    config.orientation = js("L").LinearLayout("vertical")
    config.padding = nuList(0, nuDp(15), 0, 0)
    config.style = "message_bg"
    config.children = children
    val linear = js("L").LinearLayout(config)
    self.popup = nuNewPopupPageJs(linear, if (nuTruthy(opts.popupKey)) opts.popupKey else "feedback.IssueDetail")
    if (nuTruthy(opts.closeButton)) linear.addView(js("NetworkUI").closeButton(self.popup))
    self.popup.on("exit", {
        if (nuTruthy(self.callback)) self.callback()
        js("undefined")
    })
    js("PWM").registerResetFlag(self, "popup")
    return self.popup
}

private fun nuIssueDetailHeader(self: dynamic, hasBack: dynamic): dynamic {
    val children = js("[]")
    if (nuTruthy(hasBack)) {
        val backConfig = nuObject()
        backConfig.text = "< 返回"
        backConfig.padding = nuList(nuDp(10), nuDp(10), nuDp(10), nuDp(10))
        backConfig.layout = nuSize(-2, -2)
        backConfig.style = "button_critical"
        backConfig.fontSize = 2
        backConfig.onClick = { _v: dynamic ->
            try {
                self.popup.exit()
            } catch (e: dynamic) {
                js("erp")(e)
            }
            js("undefined")
        }
        children.push(js("L").TextView(backConfig))
    }
    val titleConfig = nuObject()
    titleConfig.singleLine = true
    titleConfig.ellipsize = js("G.TextUtils.TruncateAt.END")
    titleConfig.gravity = js("L").Gravity("left|center")
    titleConfig.padding = nuList(nuDp(10), 0, nuDp(10), 0)
    titleConfig.style = "textview_default"
    titleConfig.fontSize = 4
    titleConfig.layout = nuObject()
    titleConfig.layout.width = 0
    titleConfig.layout.height = -2
    titleConfig.layout.weight = 1.0
    self.title = js("L").TextView(titleConfig)
    children.push(self.title)

    val menuConfig = nuObject()
    menuConfig.text = "▼"
    menuConfig.padding = nuList(nuDp(10), 0, nuDp(10), 0)
    menuConfig.gravity = js("L").Gravity("center")
    menuConfig.style = "button_highlight"
    menuConfig.fontSize = 3
    menuConfig.layout = nuSize(-2, -2)
    children.push(js("L").TextView(menuConfig))

    val headerConfig = nuObject()
    headerConfig.orientation = js("L").LinearLayout("horizontal")
    headerConfig.padding = nuList(nuDp(15), 0, nuDp(15), nuDp(10))
    headerConfig.gravity = js("L").Gravity("center")
    headerConfig.onClick = { _v: dynamic ->
        try {
            js("Common").showOperateDialog(self.contextMenu)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }
    headerConfig.children = children
    return js("L").LinearLayout(headerConfig)
}

private fun nuCommentBar(optsIn: dynamic): dynamic {
    val opts = if (nuTruthy(optsIn)) optsIn else nuObject()

    val inputOpts = nuObject()
    inputOpts.layout = nuObject()
    inputOpts.layout.width = 0
    inputOpts.layout.height = -2
    inputOpts.layout.weight = 1.0
    inputOpts.padding = nuList(nuDp(10), nuDp(10), 0, nuDp(10))
    inputOpts.imeOptions = js("L").EditorInfo("IME_FLAG_NO_FULLSCREEN")
    inputOpts.style = "edittext_default"
    inputOpts.fontSize = 3
    if (nuTruthy(opts.hint)) {
        inputOpts.hint = opts.hint
        inputOpts.focusableInTouchMode = true
    }
    val input = js("L").EditText(inputOpts)

    val sendConfig = nuObject()
    sendConfig.layout = nuSize(-2, -1)
    sendConfig.gravity = js("L").Gravity("center")
    sendConfig.text = "发送"
    sendConfig.padding = nuList(nuDp(10), nuDp(10), nuDp(10), nuDp(10))
    sendConfig.style = "button_reactive_auto"
    sendConfig.fontSize = 3
    sendConfig.onClick = { _v: dynamic -> nuCommentSend(opts, input) }

    val viewConfig = nuObject()
    viewConfig.layout = nuSize(-1, -2)
    viewConfig.orientation = js("L").LinearLayout("horizontal")
    viewConfig.style = "bar_float"
    viewConfig.children = nuList(input, js("L").TextView(sendConfig))
    val view = js("L").LinearLayout(viewConfig)

    val result = nuObject()
    result.view = view
    result.input = input
    return result
}

private fun nuEditIssueDialog(opts: dynamic) {
    js("G").ui({
        try {
            var data = if (nuTruthy(opts.data)) opts.data else nuObject()
            val field = if (nuTruthy(opts.contentField)) opts.contentField else "content"

            val titleConfig = nuObject()
            titleConfig.text = opts.dialogTitle
            titleConfig.padding = nuList(0, 0, 0, nuDp(10))
            titleConfig.layout = nuSize(-1, -2)
            titleConfig.style = "textview_default"
            titleConfig.fontSize = 4

            val titleOpts = nuObject()
            titleOpts.text = data.title
            titleOpts.hint = opts.titleHint
            titleOpts.singleLine = true
            titleOpts.padding = nuList(0, 0, 0, 0)
            titleOpts.imeOptions = js("L").EditorInfo("IME_FLAG_NO_FULLSCREEN")
            titleOpts.style = "edittext_default"
            titleOpts.fontSize = 3
            titleOpts.layout = nuSize(-1, -2)
            val title = js("L").EditText(titleOpts)

            val contentOpts = nuObject()
            contentOpts.text = opts.contentText
            contentOpts.hint = opts.contentHint
            contentOpts.padding = nuList(0, nuDp(20), 0, 0)
            contentOpts.imeOptions = js("L").EditorInfo("IME_FLAG_NO_FULLSCREEN")
            contentOpts.style = "edittext_default"
            contentOpts.fontSize = 2
            contentOpts.layout = nuSize(-1, -2)
            val content = js("L").EditText(contentOpts)

            var popup: dynamic = js("undefined")

            val buttonConfig = nuObject()
            buttonConfig.text = "确定"
            buttonConfig.padding = nuList(nuDp(10), nuDp(20), nuDp(10), nuDp(20))
            buttonConfig.gravity = js("L").Gravity("center")
            buttonConfig.layout = nuSize(-1, -2)
            buttonConfig.style = "button_critical"
            buttonConfig.fontSize = 3
            buttonConfig.onClick = { _v: dynamic ->
                nuEditIssueSubmit(opts, data, field, title, content, popup)
            }

            val innerConfig = nuObject()
            innerConfig.orientation = js("L").LinearLayout("vertical")
            innerConfig.padding = nuList(nuDp(15), nuDp(15), nuDp(15), 0)
            innerConfig.children = nuList(
                js("L").TextView(titleConfig),
                title,
                content,
                js("L").TextView(buttonConfig)
            )

            val scrollConfig = nuObject()
            scrollConfig.style = "message_bg"
            scrollConfig.child = js("L").LinearLayout(innerConfig)

            popup = js("PopupPage").showDialog(opts.popupName, js("L").ScrollView(scrollConfig), -1, -2)
            if (nuTruthy(opts.onDismiss)) popup.on("exit", opts.onDismiss)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

private fun nuFormDialog(opts: dynamic) {
    js("G").ui({
        try {
            var popup: dynamic = js("undefined")
            val fields = nuObject()
            val children = js("[]")

            val titleConfig = nuObject()
            titleConfig.text = opts.title
            titleConfig.padding = nuList(0, 0, 0, nuDp(10))
            titleConfig.layout = nuSize(-1, -2)
            titleConfig.gravity = js("L").Gravity("center")
            titleConfig.style = "textview_default"
            titleConfig.fontSize = 4
            children.push(js("L").TextView(titleConfig))

            var i = 0
            val fieldCount = nuLength(opts.fields)
            while (i < fieldCount) {
                val f = opts.fields[i]
                val inputOpts = nuObject()
                inputOpts.hint = f.hint
                inputOpts.singleLine = nuStrictNeJs(f.singleLine, false)
                inputOpts.padding = nuList(0, if (i == 0) 0 else nuDp(20), 0, 0)
                inputOpts.imeOptions = js("L").EditorInfo("IME_FLAG_NO_FULLSCREEN")
                inputOpts.style = "edittext_default"
                inputOpts.fontSize = 3
                inputOpts.layout = nuSize(-1, -2)
                if (nuNotUndefined(f.value)) inputOpts.text = f.value
                if (nuTruthy(f.inputType)) inputOpts.inputType = js("L").InputType(f.inputType)
                if (nuTruthy(f.password)) {
                    inputOpts.transformationMethod = js("L").asClass(js("L").PasswordTransformationMethod).instance
                }
                fields[f.name] = js("L").EditText(inputOpts)
                children.push(fields[f.name])
                i++
            }

            if (nuTruthy(opts.note)) {
                val noteConfig = nuObject()
                noteConfig.text = opts.note.text
                noteConfig.padding = if (nuTruthy(opts.note.padding)) {
                    opts.note.padding
                } else {
                    nuList(0, nuDp(20), 0, nuDp(10))
                }
                noteConfig.layout = nuSize(-1, -2)
                noteConfig.style = if (nuTruthy(opts.note.style)) opts.note.style else "textview_highlight"
                noteConfig.fontSize = if (nuTruthy(opts.note.fontSize)) opts.note.fontSize else 2
                children.push(js("L").TextView(noteConfig))
            }

            i = 0
            val buttonCount = nuLength(opts.buttons)
            while (i < buttonCount) {
                val b = opts.buttons[i]
                val buttonOpts = nuObject()
                buttonOpts.text = b.text
                buttonOpts.padding = if (nuTruthy(b.padding)) {
                    b.padding
                } else {
                    nuList(nuDp(10), nuDp(10), nuDp(10), nuDp(10))
                }
                buttonOpts.gravity = js("L").Gravity("center")
                buttonOpts.layout = nuSize(-1, -2)
                buttonOpts.style = if (nuTruthy(b.style)) b.style else "button_critical"
                buttonOpts.fontSize = if (nuTruthy(b.fontSize)) b.fontSize else 3
                buttonOpts.onClick = { v: dynamic -> nuFormButtonClick(fields, b, popup, v) }
                children.push(js("L").TextView(buttonOpts))
                i++
            }

            val innerConfig = nuObject()
            innerConfig.orientation = js("L").LinearLayout("vertical")
            innerConfig.padding = if (nuTruthy(opts.padding)) {
                opts.padding
            } else {
                nuList(nuDp(15), nuDp(15), nuDp(15), nuDp(5))
            }
            innerConfig.children = children

            val scrollConfig = nuObject()
            scrollConfig.style = "message_bg"
            scrollConfig.child = js("L").LinearLayout(innerConfig)

            popup = js("PopupPage").showDialog(opts.popupName, js("L").ScrollView(scrollConfig), -1, -2)
            fields.__popup = popup
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

private fun nuListItem(holder: dynamic, optsIn: dynamic): dynamic {
    val opts = if (nuTruthy(optsIn)) optsIn else nuObject()
    val ctx = js("ctx")
    val width = if (nuTruthy(opts.width)) opts.width else -1
    val padding = if (nuTruthy(opts.padding)) {
        opts.padding
    } else {
        nuList(nuDp(15), nuDp(15), nuDp(15), nuDp(15))
    }
    val layout = nuNewLinearLayout(ctx)
    val text1 = nuNewTextView(ctx)
    val text2 = nuNewTextView(ctx)
    holder.linear = layout
    holder.text1 = text1
    holder.text2 = text2

    layout.setLayoutParams(nuNewAbsListParamsJs(-1, -2))
    layout.setOrientation(js("G.LinearLayout.VERTICAL"))
    layout.setPadding(padding[0], padding[1], padding[2], padding[3])
    text1.setLayoutParams(nuNewLinearParamsJs(width, -2))
    text1.setEllipsize(js("G.TextUtils.TruncateAt.END"))
    text1.setSingleLine(true)
    if (nuTruthy(opts.text1Style)) {
        nuApplyStyle(text1, opts.text1Style, if (nuTruthy(opts.text1FontSize)) opts.text1FontSize else 3)
    }
    layout.addView(text1)
    text2.setPadding(0, nuDp(5), 0, 0)
    text2.setLayoutParams(nuNewLinearParamsJs(width, -2))
    nuApplyStyle(
        text2,
        if (nuTruthy(opts.text2Style)) opts.text2Style else "textview_prompt",
        if (nuTruthy(opts.text2FontSize)) opts.text2FontSize else 1
    )
    layout.addView(text2)
    return layout
}

fun installNetworkUI() {
    networkUiApi.createListPage = { self: dynamic, title: dynamic, onItemClick: dynamic, opts: dynamic ->
        nuCreateListPage(self, title, onItemClick, opts)
        js("undefined")
    }
    networkUiApi.issueStateMenu = { self: dynamic -> nuIssueStateMenu(self) }
    networkUiApi.issueStateMenuEntry = { self: dynamic, state: dynamic -> nuIssueStateMenuEntry(self, state) }
    networkUiApi.closeButton = { popup: dynamic -> nuCloseButton(popup) }
    networkUiApi.appendPage = { self: dynamic, sync: dynamic, opts: dynamic -> nuAppendPage(self, sync, opts) }
    networkUiApi.updateMore = { self: dynamic, hasNext: dynamic, text: dynamic, useHeader: dynamic ->
        nuUpdateMore(self, hasNext, text, useHeader)
        js("undefined")
    }
    networkUiApi.progress = { text: dynamic, task: dynamic -> nuProgress(text, task) }
    networkUiApi.issueDetailPage = { self: dynamic, children: dynamic, opts: dynamic ->
        nuIssueDetailPage(self, children, opts)
    }
    networkUiApi.issueDetailHeader = { self: dynamic, hasBack: dynamic -> nuIssueDetailHeader(self, hasBack) }
    networkUiApi.commentBar = { opts: dynamic -> nuCommentBar(opts) }
    networkUiApi.editIssueDialog = { opts: dynamic ->
        nuEditIssueDialog(opts)
        js("undefined")
    }
    networkUiApi.formDialog = { opts: dynamic ->
        nuFormDialog(opts)
        js("undefined")
    }
    networkUiApi.listItem = { holder: dynamic, opts: dynamic -> nuListItem(holder, opts) }

    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("NetworkUI", networkUiApi)
    }
}
