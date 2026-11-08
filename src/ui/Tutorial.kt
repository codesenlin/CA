package ca.kotlin.ui

import ca.kotlin.core.*


/**
 * modules/Tutorial.js 的 Kotlin/JS 版本。
 *
 * 原文件把界面状态挂在 showList / showTutorial 这两个具名函数上（函数即
 * self），PWM 触发 resetUICache 时再把 self.linear 清空以强制重建。这里用
 * 薄 JS 工厂保留同一个函数对象，状态仍挂在函数自身，不额外引入 state 对象。
 *
 * 所有 UI 构造都走带参数的 JS helper，避免把 Kotlin 局部变量名写进 js("...")。
 */
private val tutorialApi: dynamic = js("({})")

// ---------------- JS 语义 helper ----------------
private val tutTruthyJs: dynamic = js("function (v) { return !!v; }")
private val tutStringJs: dynamic = js("function (v) { return String(v); }")
private val tutLengthJs: dynamic = js("function (v) { return v.length | 0; }")
private val tutLooseEqJs: dynamic = js("function (a, b) { return a == b; }")
private val tutStrictEqJs: dynamic = js("function (a, b) { return a === b; }")
private val tutOrJs: dynamic = js("function (a, b) { return a || b; }")
private val tutLtJs: dynamic = js("function (a, b) { return a < b; }")
private val tutIncJs: dynamic = js("function (v) { return ++v; }")
private val tutIsNaNRawJs: dynamic = js("function (v) { return isNaN(v); }")
private val tutBitOrJs: dynamic = js("function (a, b) { return a | b; }")

private fun tutTruthy(v: dynamic): Boolean = tutTruthyJs(v)
private fun tutString(v: dynamic): dynamic = tutStringJs(v)
private fun tutLength(v: dynamic): Int = tutLengthJs(v)
private fun tutLooseEq(a: dynamic, b: dynamic): Boolean = tutLooseEqJs(a, b)
private fun tutStrictEq(a: dynamic, b: dynamic): Boolean = tutStrictEqJs(a, b)
private fun tutOr(a: dynamic, b: dynamic): dynamic = tutOrJs(a, b)
private fun tutLt(a: dynamic, b: dynamic): Boolean = tutLtJs(a, b)
private fun tutInc(v: dynamic): dynamic = tutIncJs(v)
private fun tutIsNaN(v: dynamic): Boolean = tutIsNaNRawJs(v)
private fun tutBitOr(a: dynamic, b: dynamic): dynamic = tutBitOrJs(a, b)

private fun tutObject(): dynamic = js("({})")
private fun tutArray(): dynamic = js("[]")

// ---------------- Android / UI helper ----------------
private val tutDpJs: dynamic = js("function (n) { return n * G.dp; }")
private val tutNewAbsListParamsJs: dynamic = js(
    "function (w, h) { return new G.AbsListView.LayoutParams(w, h); }"
)
private val tutNewLinearParamsJs: dynamic = js(
    "function (w, h) { return new G.LinearLayout.LayoutParams(w, h); }"
)
private val tutNewWeightedLinearParamsJs: dynamic = js(
    "function (w, h, weight) { return new G.LinearLayout.LayoutParams(w, h, weight); }"
)
private val tutNewFrameParamsJs: dynamic = js(
    "function (w, h) { return new G.FrameLayout.LayoutParams(w, h); }"
)
private val tutNewLinearLayoutJs: dynamic = js(
    "function () { return new G.LinearLayout(ctx); }"
)
private val tutNewTextViewJs: dynamic = js("function () { return new G.TextView(ctx); }")
private val tutNewListViewJs: dynamic = js("function () { return new G.ListView(ctx); }")
private val tutNewScrollViewJs: dynamic = js("function () { return new G.ScrollView(ctx); }")
private val tutNewItemClickListenerJs: dynamic = js(
    "function (f) { return new G.AdapterView.OnItemClickListener({ onItemClick: f }); }"
)
private val tutNewClickListenerJs: dynamic = js(
    "function (f) { return new G.View.OnClickListener({ onClick: f }); }"
)
private val tutNewRhinoListAdapterJs: dynamic = js(
    "function (a, vmaker) { return new RhinoListAdapter(a, vmaker); }"
)
private val tutNewPopupPageJs: dynamic = js(
    "function (view, name) { return new PopupPage(view, name); }"
)
private val tutOpenUrlJs: dynamic = js(
    "function (url) {" +
        " var intent = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url));" +
        " intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);" +
        " AndroidBridge.startActivity(intent);" +
        "}"
)
private val tutFormattedCommandJs: dynamic = js(
    "function (command) { return { formattedCommand: command }; }"
)

private fun tutDp(n: Int): dynamic = tutDpJs(n)

// ---------------- Tutorial 状态函数 ----------------
private val tutShowListFactoryJs: dynamic = js(
    "function (impl) { return function self(callback) { return impl(self, arguments); }; }"
)
private val tutShowTutorialFactoryJs: dynamic = js(
    "function (impl) { return function self(o, callback) { return impl(self, arguments); }; }"
)

private fun tutBuildListAdapter(e: dynamic): dynamic {
    val layout = tutNewLinearLayoutJs()
    val text1 = tutNewTextViewJs()
    val text2 = tutNewTextViewJs()
    layout.setLayoutParams(tutNewAbsListParamsJs(-1, -2))
    layout.setOrientation(js("G.LinearLayout.VERTICAL"))
    layout.setPadding(tutDp(15), tutDp(15), tutDp(15), tutDp(15))
    text1.setLayoutParams(tutNewLinearParamsJs(-1, -2))
    text1.setText(e.title)
    text1.setEllipsize(js("G.TextUtils.TruncateAt.MIDDLE"))
    text1.setSingleLine(true)
    js("Common").applyStyle(text1, if (tutLooseEq(e.state, 2)) "item_disabled" else "item_default", 3)
    layout.addView(text1)
    if (tutTruthy(e.description)) {
        text2.setPadding(0, tutDp(5), 0, 0)
        text2.setLayoutParams(tutNewLinearParamsJs(-1, -2))
        text2.setText(e.description)
        js("Common").applyStyle(text2, "textview_prompt", 1)
        layout.addView(text2)
    }
    return layout
}

private fun tutRefreshList(self: dynamic): dynamic {
    val data = tutorialApi.getSettings()
    val a = tutObject()
    val states = js("[[], [], []]")
    val library = tutorialApi.library
    var i = 0
    while (i < tutLength(library)) {
        val e = library[i]
        val item = tutObject()
        item.index = i
        item.type = e.type
        item.name = e.name
        item.description = e.description
        item.segmentLen = tutLength(e.segments)
        item.progress = if (tutTruthy(data[e.id])) data[e.id].progress else -1
        item.source = e
        a[e.id] = item
        i++
    }

    val keys = js("Object").keys(a)
    i = 0
    while (i < tutLength(keys)) {
        val item = a[keys[i]]
        if (item.progress >= item.segmentLen) {
            item.title = item.name
            item.state = 2
            states[2].push(item)
        } else if (item.progress >= 0) {
            item.title = item.name + " （" + ((item.progress + 1) / item.segmentLen * 100).toFixed(0) + "%）"
            item.state = 0
            states[0].push(item)
        } else {
            item.title = item.name + " *"
            item.state = 1
            states[1].push(item)
        }
        i++
    }

    self.title.setText(
        "教程 (进行中:" + tutLength(states[0]) +
            "|未读:" + tutLength(states[1]) +
            "|已读:" + tutLength(states[2]) + ")"
    )
    self.list.setAdapter(
        tutNewRhinoListAdapterJs(states[0].concat(states[1], states[2]), self.adapter)
    )
    return js("undefined")
}

private fun tutShowList(self: dynamic, callback: dynamic): dynamic {
    js("G").ui({
        try {
            if (!tutTruthy(self.linear)) {
                self.adapter = { e: dynamic, _i: dynamic, _a: dynamic -> tutBuildListAdapter(e) }
                self.refresh = {
                    tutRefreshList(self)
                    js("undefined")
                }

                self.linear = tutNewLinearLayoutJs()
                self.linear.setOrientation(js("G.LinearLayout.VERTICAL"))
                self.linear.setPadding(tutDp(15), tutDp(15), tutDp(15), 0)
                js("Common").applyStyle(self.linear, "message_bg")

                self.title = tutNewTextViewJs()
                self.title.setText("教程")
                self.title.setGravity(tutBitOr(js("G.Gravity.LEFT"), js("G.Gravity.CENTER")))
                self.title.setPadding(tutDp(10), 0, tutDp(10), tutDp(10))
                js("Common").applyStyle(self.title, "textview_default", 4)
                self.linear.addView(self.title, tutNewLinearParamsJs(-1, -2))

                self.list = tutNewListViewJs()
                self.list.setOnItemClickListener(tutNewItemClickListenerJs({
                    parent: dynamic, _view: dynamic, pos: dynamic, _id: dynamic ->
                    try {
                        val data = parent.getAdapter().getItem(pos)
                        tutorialApi.showIntro(data.source, {
                            self.refresh()
                            js("undefined")
                        })
                    } catch (e: dynamic) {
                        js("erp")(e)
                    }
                    js("undefined")
                }))
                self.linear.addView(self.list, tutNewWeightedLinearParamsJs(-1, 0, 1.0))

                self.exit = tutNewTextViewJs()
                self.exit.setText("关闭")
                self.exit.setGravity(js("G.Gravity.CENTER"))
                self.exit.setPadding(tutDp(10), tutDp(20), tutDp(10), tutDp(20))
                js("Common").applyStyle(self.exit, "button_critical", 3)
                self.exit.setOnClickListener(tutNewClickListenerJs({
                    _v: dynamic ->
                    try {
                        self.popup.exit()
                    } catch (e: dynamic) {
                        js("erp")(e)
                    }
                    js("undefined")
                }))
                self.linear.addView(self.exit, tutNewLinearParamsJs(-1, -2))

                self.popup = tutNewPopupPageJs(self.linear, "tutorial.List")
                self.popup.on("exit", {
                    CA.trySave()
                    if (tutTruthy(self.callback)) self.callback()
                    js("undefined")
                })

                js("PWM").registerResetFlag(self, "linear")
            }
            self.callback = callback
            self.refresh()
            self.popup.enter()
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
    return js("undefined")
}

private fun tutShowIntro(o: dynamic, callback: dynamic): dynamic {
    js("G").ui({
        try {
            var popup: dynamic = js("null")
            val linear = tutNewLinearLayoutJs()
            linear.setOrientation(js("G.LinearLayout.VERTICAL"))
            linear.setPadding(tutDp(15), tutDp(15), tutDp(15), 0)
            js("Common").applyStyle(linear, "message_bg")

            val title = tutNewTextViewJs()
            title.setText(o.name)
            title.setPadding(0, 0, 0, tutDp(10))
            js("Common").applyStyle(title, "textview_default", 4)
            linear.addView(title, tutNewLinearParamsJs(-1, -2))

            val scr = tutNewScrollViewJs()
            val desc = tutNewTextViewJs()
            desc.setText(js("ISegment").rawJson(tutOr(tutOr(o.intro, o.description), "暂无简介")))
            js("Common").applyStyle(desc, "textview_default", 3)
            scr.addView(desc, tutNewFrameParamsJs(-1, -2))
            linear.addView(scr, tutNewWeightedLinearParamsJs(-1, 0, 1.0))

            val enter = tutNewTextViewJs()
            enter.setText("进入")
            enter.setGravity(js("G.Gravity.RIGHT"))
            enter.setPadding(0, tutDp(10), tutDp(20), tutDp(20))
            js("Common").applyStyle(enter, "button_critical", 3)
            enter.setOnClickListener(tutNewClickListenerJs({
                _v: dynamic ->
                try {
                    popup.exit()
                    if (tutLooseEq(o.type, "tutorial")) {
                        tutorialApi.showTutorial(o, callback)
                    }
                } catch (e: dynamic) {
                    js("erp")(e)
                }
                js("undefined")
            }))
            linear.addView(enter, tutNewLinearParamsJs(-1, -2))

            popup = js("PopupPage").showDialog("tutorial.Intro", linear, -1, -1)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
    return js("undefined")
}

private fun tutGenerateText(str: dynamic, focusable: dynamic): dynamic {
    val text = tutNewTextViewJs()
    text.setPadding(tutDp(15), tutDp(15), tutDp(15), tutDp(15))
    text.setLayoutParams(tutNewAbsListParamsJs(-1, -2))
    text.setText(str)
    text.setFocusable(focusable)
    js("Common").applyStyle(text, "textview_default", 3)
    return text
}

private fun tutGenerateCopyable(str: dynamic): dynamic {
    val layout = tutNewLinearLayoutJs()
    val text1 = tutNewTextViewJs()
    val text2 = tutNewTextViewJs()
    layout.setLayoutParams(tutNewAbsListParamsJs(-1, -2))
    layout.setOrientation(js("G.LinearLayout.HORIZONTAL"))

    text1.setPadding(tutDp(15), tutDp(15), 0, tutDp(15))
    text1.setLayoutParams(tutNewWeightedLinearParamsJs(0, -2, 1.0))
    text1.setText(str)
    js("Common").applyStyle(text1, "textview_default", 3)
    layout.addView(text1)

    text2.setPadding(tutDp(15), 0, tutDp(15), 0)
    text2.setLayoutParams(tutNewLinearParamsJs(-2, -1))
    text2.setText("\uD83D\uDCCB") // Emoji:Paste
    text2.setGravity(js("G.Gravity.CENTER"))
    js("Common").applyStyle(text2, "button_default", 3)
    layout.addView(text2)
    return layout
}

private fun tutConvertView(self: dynamic, e: dynamic, sets: dynamic): dynamic {
    if (tutTruthy(e.text)) {
        val t = js("ISegment").rawJson(e.text, sets.varmap)
        val item = tutObject()
        item.type = "text"
        item.text = t
        item.view = self.generateText(t, true)
        return item
    } else if (tutTruthy(e.command)) {
        val item = tutObject()
        item.type = "command"
        item.command = e.command
        item.view = self.generateCopyable(
            js("ISegment").rawJson(tutFormattedCommandJs(e.command), js("null"))
        )
        return item
    } else if (tutTruthy(e.link)) {
        val t = tutOr(e.prompt, e.link)
        val item = tutObject()
        item.type = "link"
        item.prompt = t
        item.url = e.link
        item.view = self.generateText(js("ISegment").rawJson(t, sets.varmap), false)
        return item
    }
    val item = tutObject()
    item.type = "unknown"
    item.view = self.generateText("未知的片段")
    return item
}

private fun tutTutorialInit(self: dynamic, o: dynamic): dynamic {
    val r = tutArray()
    val titleItem = tutObject()
    titleItem.type = "title"
    titleItem.view = self.linear
    r.push(titleItem)

    self.current = o
    self.sets = tutorialApi.getSettings(tutString(o.id))
    self.title.setText(o.name)
    if (tutIsNaN(self.sets.progress)) self.sets.progress = 0
    if (!tutTruthy(self.sets.varmap)) self.sets.varmap = tutObject()

    val a = o.segments
    var i: dynamic = 0
    while (tutLt(i, self.sets.progress) && tutLt(i, tutLength(a))) {
        r.push(self.convertView(a[i], self.sets))
        i = tutInc(i)
    }
    val adapter = tutNewRhinoListAdapterJs(r, self.adapter)
    self.list.setAdapter(adapter)
    self.adpt = js("RhinoListAdapter").getController(adapter)
    self.next()
    return js("undefined")
}

private fun tutTutorialNext(self: dynamic): dynamic {
    val a = self.current.segments
    var i: dynamic = self.sets.progress
    var f: dynamic = js("undefined")
    while (tutLt(i, tutLength(a))) {
        val t = a[i]
        self.adpt.add(self.convertView(t, self.sets))
        if (tutStrictEq(t.stepMode, "manual")) {
            f = true
            val item = tutObject()
            item.type = "step.manual"
            item.view = self.generateText("点击进入下一步", false)
            self.adpt.add(item)
        }
        if (tutTruthy(f)) break
        i = tutInc(i)
    }
    self.sets.progress = i
    if (tutLooseEq(i, tutLength(a))) {
        val item = tutObject()
        item.type = "ending"
        item.view = self.generateText(self.current.name + "已结束，点击以退出", false)
        self.adpt.add(item)
    }
    //self.list.setSelectionFromTop(self.adpt.length() - 1, 0);
    self.list.smoothScrollToPosition(self.adpt.length() - 1)
    return js("undefined")
}

private fun tutOnTutorialItemClick(self: dynamic, parent: dynamic, pos: dynamic): dynamic {
    try {
        val e = parent.getAdapter().getItem(pos)
        if (!tutTruthy(e)) return js("undefined")
        when {
            tutStrictEq(e.type, "command") -> {
                js("Common").setClipboardText(e.command)
                js("Common").toast("内容已复制")
            }
            tutStrictEq(e.type, "link") -> {
                try {
                    tutOpenUrlJs(e.url)
                } catch (err: dynamic) {
                    js("Common").toast("打开链接失败\n" + err)
                }
            }
            tutStrictEq(e.type, "step.manual") -> {
                self.sets.progress = tutInc(self.sets.progress)
                self.adpt.removeByIndex(pos)
                self.next()
            }
            tutStrictEq(e.type, "ending") || tutStrictEq(e.type, "title") -> {
                self.popup.exit()
            }
        }
    } catch (e: dynamic) {
        js("erp")(e)
    }
    return js("undefined")
}

private fun tutShowTutorial(self: dynamic, o: dynamic, callback: dynamic): dynamic {
    js("G").ui({
        try {
            if (!tutTruthy(self.linear)) {
                self.adapter = { e: dynamic, _i: dynamic, _a: dynamic -> e.view }
                self.init = { o2: dynamic ->
                    tutTutorialInit(self, o2)
                    js("undefined")
                }
                self.next = {
                    tutTutorialNext(self)
                    js("undefined")
                }
                self.convertView = { e: dynamic, sets: dynamic -> tutConvertView(self, e, sets) }
                self.generateText = { str: dynamic, focusable: dynamic ->
                    tutGenerateText(str, focusable)
                }
                self.generateCopyable = { str: dynamic -> tutGenerateCopyable(str) }

                self.linear = tutNewLinearLayoutJs()
                self.linear.setOrientation(js("G.LinearLayout.HORIZONTAL"))
                js("Common").applyStyle(self.linear, "bar_float")
                self.linear.setLayoutParams(tutNewAbsListParamsJs(-1, -2))

                self.title = tutNewTextViewJs()
                self.title.setPadding(tutDp(20), tutDp(20), 0, tutDp(20))
                js("Common").applyStyle(self.title, "textview_default", 4)
                self.linear.addView(self.title, tutNewWeightedLinearParamsJs(0, -1, 1.0))

                self.exit = tutNewTextViewJs()
                self.exit.setText("关闭")
                self.exit.setGravity(js("G.Gravity.CENTER"))
                self.exit.setPadding(tutDp(20), tutDp(20), tutDp(20), tutDp(20))
                js("Common").applyStyle(self.exit, "button_critical", 3)
                self.linear.addView(self.exit, tutNewLinearParamsJs(-2, -1))
                self.linear.setFocusable(false)

                self.list = tutNewListViewJs()
                js("Common").applyStyle(self.list, "message_bg")
                self.list.setOnItemClickListener(tutNewItemClickListenerJs({
                    parent: dynamic, _view: dynamic, pos: dynamic, _id: dynamic ->
                    tutOnTutorialItemClick(self, parent, pos)
                }))

                self.popup = tutNewPopupPageJs(self.list, "tutorial.Tutorial")
                self.popup.on("exit", {
                    CA.trySave()
                    if (tutTruthy(self.callback)) self.callback()
                    js("undefined")
                })

                js("PWM").registerResetFlag(self, "linear")
            }
            self.callback = callback
            self.init(o)
            self.popup.enter()
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
    return js("undefined")
}

private fun tutGetSettings(id: dynamic): dynamic {
    if (!tutTruthy(CA.settings.tutorialData)) {
        CA.settings.tutorialData = tutObject()
    }
    if (tutTruthy(id)) {
        if (!tutTruthy(CA.settings.tutorialData[id])) {
            CA.settings.tutorialData[id] = tutObject()
        }
        return CA.settings.tutorialData[id]
    }
    return CA.settings.tutorialData
}

fun installTutorial() {
    tutorialApi.library = tutArray()

    val showList = tutShowListFactoryJs { self: dynamic, args: dynamic ->
        tutShowList(self, args[0])
    }
    tutorialApi.showList = showList

    tutorialApi.showIntro = { o: dynamic, callback: dynamic ->
        tutShowIntro(o, callback)
        js("undefined")
    }

    val showTutorial = tutShowTutorialFactoryJs { self: dynamic, args: dynamic ->
        tutShowTutorial(self, args[0], args[1])
    }
    tutorialApi.showTutorial = showTutorial

    tutorialApi.getSettings = { id: dynamic -> tutGetSettings(id) }

    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("Tutorial", tutorialApi)
    }
}
