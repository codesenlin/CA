package ca.kotlin

/**
 * modules/CA.js agreement/actions 组 Kotlin/JS 版本（原文件约 7934-8357 行，以成员名为准）。
 *
 * 只提供 installCaAgreement(api: dynamic)：把本组 5 个顶层成员
 * （showActions / showActionEdit / Actions / showAgreement / showAgreementSync）
 * 挂到调用方传入的 CA 共享 API 上；不注册模块、不定义全局 caApi、不调用安装函数。
 *
 * 原实现里 showActions / showActionEdit 是具名函数表达式 `function self(...)`：
 * showActions 把 vmaker / vbinder 缓存在函数对象上，showActionEdit 把 contextMenu /
 * itemMenu / removeInvaildAction / vmaker / vbinder / createAction 挂在函数对象上。
 * 这里用薄 JS 工厂生成同名的 `function self(...)`，把真实 self 传回 Kotlin；
 * 缓存仍落在同一个函数对象上，跨次调用与外部访问 CA.showActions.xxx 的语义不变。
 *
 * 所有原来的 `this` 引用改成显式 api；跨组成员（Actions / his / performExit /
 * performPaste / icon / hideIcon / showIcon / settings.readAgreement 等）统一走
 * api.xxx 动态调用，本文件不定义它们。
 *
 * 原 JS 的既有行为/毛病按原样保留，并在对应位置注释：
 *  - showActionEdit 用 `self.linear` 作为“是否已初始化”的判据，但原代码从未给
 *    self.linear 赋值，所以每次调用都会重新定义菜单与回调（照原样保留）；
 *  - Actions["ca.quickPaste"] 里的 `t` 是未定义变量，`e == t` 实际会过滤掉
 *    null/undefined 的历史项；这里用同义 JS helper 保留；
 *  - showActionEdit 的“恢复默认”直接用 tag.defaultActions 覆盖并逐项
 *    Object.copy（GlobalUtils 安装的深拷贝），不更新 self.linear；
 *  - 动作菜单排序对话框的 getTitle/getDescription 直接解引用 CA.Actions[...]，
 *    找不到动作时与原实现一样抛错；
 *  - showAgreement 的“关闭应用”会先 popup.exit(true) 再 killProcess。
 */

// ---------------- JS 语义 helper ----------------

private val caaTruthyJs: dynamic = js("function (v) { return !!v; }")
private fun caaTruthy(v: dynamic): Boolean = caaTruthyJs(v)

/** 所有 length 统一走 `| 0`，兼容 Rhino 的 Java 数组/包装对象。 */
private val caaLengthJs: dynamic = js("function (v) { return v.length | 0; }")
private fun caaLength(v: dynamic): Int = caaLengthJs(v)

private val caaSetLengthJs: dynamic = js("function (a, n) { a.length = n; }")
private fun caaSetLength(a: dynamic, n: Int) {
    caaSetLengthJs(a, n)
}

private val caaStringJs: dynamic = js("function (v) { return String(v); }")
private fun caaString(v: dynamic): dynamic = caaStringJs(v)

private val caaGtJs: dynamic = js("function (a, b) { return a > b; }")
private fun caaGt(a: dynamic, b: dynamic): Boolean = caaGtJs(a, b)

private val caaBoolJs: dynamic = js("function (v) { return Boolean(v); }")
private fun caaBool(v: dynamic): Boolean = caaBoolJs(v)

private val caaDateParseJs: dynamic = js("function (v) { return Date.parse(v); }")
private fun caaDateParse(v: dynamic): dynamic = caaDateParseJs(v)

private val caaNowJs: dynamic = js("function () { return Date.now(); }")
private fun caaNow(): dynamic = caaNowJs()

private val caaDpJs: dynamic = js("function (n) { return n * G.dp; }")
private fun caaDp(n: Int): dynamic = caaDpJs(n)

private val caaBitOrJs: dynamic = js("function (a, b) { return a | b; }")
private fun caaBitOr(a: dynamic, b: dynamic): dynamic = caaBitOrJs(a, b)

private val caaParseIntJs: dynamic = js("function (v) { return parseInt(v); }")
private fun caaParseInt(v: dynamic): dynamic = caaParseIntJs(v)

private val caaIsArrayJs: dynamic = js("function (v) { return Array.isArray(v); }")
private fun caaIsArray(v: dynamic): Boolean = caaIsArrayJs(v)

private val caaInstanceOfJs: dynamic = js("function (v, c) { return v instanceof c; }")
private fun caaInstanceOf(v: dynamic, c: dynamic): Boolean = caaInstanceOfJs(v, c)

private fun caaObj(): dynamic = js("({})")

private fun caaList(vararg items: dynamic): dynamic {
    val a = js("[]")
    var i = 0
    while (i < items.size) {
        a.push(items[i])
        i++
    }
    return a
}

private fun caaSize(width: dynamic, height: dynamic): dynamic {
    val o = caaObj()
    o.width = width
    o.height = height
    return o
}

private fun caaWeighted(width: dynamic, height: dynamic, weight: dynamic): dynamic {
    val o = caaObj()
    o.width = width
    o.height = height
    o.weight = weight
    return o
}

private fun caaUrlObj(url: dynamic): dynamic {
    val o = caaObj()
    o.url = url
    return o
}

/** 生成具名函数表达式 `function self(...)`，与旧 API 的 arity/name 对齐。 */
private val caaSelf2Js: dynamic = js("function (f) { return function self(a, b) { f(self, a, b); }; }")
private val caaSelf3Js: dynamic = js("function (f) { return function self(a, b, c) { f(self, a, b, c); }; }")

// ---------------- Android UI helper ----------------

private val caaNewLinearLayoutJs: dynamic = js("function () { return new G.LinearLayout(ctx); }")
private val caaNewFrameLayoutJs: dynamic = js("function () { return new G.FrameLayout(ctx); }")
private val caaNewTextViewJs: dynamic = js("function () { return new G.TextView(ctx); }")
private val caaNewListViewJs: dynamic = js("function () { return new G.ListView(ctx); }")
private val caaNewLinearParams2Js: dynamic = js(
    "function (w, h) { return new G.LinearLayout.LayoutParams(w, h); }"
)
private val caaNewLinearParams3Js: dynamic = js(
    "function (w, h, weight) { return new G.LinearLayout.LayoutParams(w, h, weight); }"
)
private val caaNewFrameParams2Js: dynamic = js(
    "function (w, h) { return new G.FrameLayout.LayoutParams(w, h); }"
)
private val caaNewAbsParamsJs: dynamic = js(
    "function (w, h) { return new G.AbsListView.LayoutParams(w, h); }"
)
private val caaNewClickListenerJs: dynamic = js(
    "function (f) { return new G.View.OnClickListener({ onClick: f }); }"
)
private val caaNewItemClickListenerJs: dynamic = js(
    "function (f) { return new G.AdapterView.OnItemClickListener({ onItemClick: f }); }"
)
private val caaNewPopupPageJs: dynamic = js("function (v, n) { return new PopupPage(v, n); }")
private val caaNewSimpleListAdapterJs: dynamic = js(
    "function (a, m, b) { return new SimpleListAdapter(a, m, b); }"
)
private val caaSimpleGetControllerJs: dynamic = js(
    "function (a) { return SimpleListAdapter.getController(a); }"
)

private fun caaNewLinearLayout(): dynamic = caaNewLinearLayoutJs()
private fun caaNewFrameLayout(): dynamic = caaNewFrameLayoutJs()
private fun caaNewTextView(): dynamic = caaNewTextViewJs()
private fun caaNewListView(): dynamic = caaNewListViewJs()
private fun caaNewLinearParams2(w: dynamic, h: dynamic): dynamic = caaNewLinearParams2Js(w, h)
private fun caaNewLinearParams3(w: dynamic, h: dynamic, weight: dynamic): dynamic =
    caaNewLinearParams3Js(w, h, weight)
private fun caaNewFrameParams2(w: dynamic, h: dynamic): dynamic = caaNewFrameParams2Js(w, h)
private fun caaNewAbsParams(w: dynamic, h: dynamic): dynamic = caaNewAbsParamsJs(w, h)
private fun caaNewClickListener(f: dynamic): dynamic = caaNewClickListenerJs(f)
private fun caaNewItemClickListener(f: dynamic): dynamic = caaNewItemClickListenerJs(f)
private fun caaNewPopupPage(v: dynamic, n: dynamic): dynamic = caaNewPopupPageJs(v, n)
private fun caaNewSimpleListAdapter(a: dynamic, m: dynamic, b: dynamic): dynamic =
    caaNewSimpleListAdapterJs(a, m, b)
private fun caaSimpleController(a: dynamic): dynamic = caaSimpleGetControllerJs(a)

// ---------------- 动作数据 helper（保持原 JS 的 map/filter/for 语义） ----------------

/** showActions 里的 `actions.map(...).filter(...)`，动作表由调用方传入 api.Actions。 */
private val caaMapActionItemsJs: dynamic = js(
    "function (actions, actionMap) {" +
        " return actions.map(function (e) {" +
        "  var action = actionMap[e.action];" +
        "  if (!action) return {};" +
        "  return {" +
        "   text: action.getName ? action.getName(e) : action.name," +
        "   description: action.getDescription ? action.getDescription(e) : action.description," +
        "   action: action.available && !action.available(e) ? null : action," +
        "   param: e" +
        "  };" +
        " }).filter(function (e) {" +
        "  return e.action != null;" +
        " });" +
        "}"
)
private fun caaMapActionItems(actions: dynamic, actionMap: dynamic): dynamic =
    caaMapActionItemsJs(actions, actionMap)

/** showActionEdit 的 removeInvaildAction：从后往前删除动作表里已不存在的项。 */
private val caaRemoveInvalidActionsJs: dynamic = js(
    "function (actions, actionMap) {" +
        " var i;" +
        " for (i = actions.length - 1; i >= 0; i--) {" +
        "  if (!(actions[i].action in actionMap)) {" +
        "   actions.splice(i, 1);" +
        "  }" +
        " }" +
        "}"
)
private fun caaRemoveInvalidActions(actions: dynamic, actionMap: dynamic) {
    caaRemoveInvalidActionsJs(actions, actionMap)
}

/** createAction 的候选列表：Object.keys(CA.Actions).map(...)。 */
private val caaActionChoicesJs: dynamic = js(
    "function (actionMap) {" +
        " return Object.keys(actionMap).map(function (e) {" +
        "  var data = actionMap[e];" +
        "  return {" +
        "   text: data.name," +
        "   description: data.description," +
        "   key: e" +
        "  };" +
        " });" +
        "}"
)
private fun caaActionChoices(actionMap: dynamic): dynamic = caaActionChoicesJs(actionMap)

/** ca.quickPaste：原代码用未定义的 `t` 过滤 null/undefined 历史项。 */
private val caaCollectPasteItemsJs: dynamic = js(
    "function (list) {" +
        " var a = [], t;" +
        " list.forEach(function (e) {" +
        "  if (e == t) return;" +
        "  a.push({" +
        "   text: e," +
        "   cmd: e" +
        "  });" +
        " });" +
        " return a;" +
        "}"
)
private fun caaCollectPasteItems(list: dynamic): dynamic = caaCollectPasteItemsJs(list)

// ---------------- showActions ----------------

private fun caaShowActions(
    api: dynamic,
    self: dynamic,
    actions: dynamic,
    onDismiss: dynamic
): dynamic {
    js("G").ui({
        caaShowActionsUi(api, self, actions, onDismiss)
        js("undefined")
    })
    return js("undefined")
}

private fun caaShowActionsUi(api: dynamic, self: dynamic, actions: dynamic, onDismiss: dynamic) {
    try {
        if (!caaTruthy(self.vmaker)) {
            self.vmaker = js("UIHelp").settingItem
            self.vbinder = { holder: dynamic, e: dynamic ->
                caaShowActionsBinder(holder, e)
                js("undefined")
            }
        }
        val l = caaMapActionItems(actions, api.Actions)
        if (caaLength(l) == 0) {
            js("Common").toast("没有可选的动作")
            return
        }
        val frame = caaNewFrameLayout()
        js("Common").applyStyle(frame, "message_bg")
        val list = caaNewListView()
        list.setLayoutParams(caaNewFrameParams2(-1, -2))
        list.setAdapter(caaNewSimpleListAdapter(l, self.vmaker, self.vbinder))
        // 弹窗在点击回调之后才赋值，闭包按引用读取，和原代码的 var popup 一致。
        var popup: dynamic = js("null")
        list.setOnItemClickListener(caaNewItemClickListener(
            { _parent: dynamic, _view: dynamic, pos: dynamic, _id: dynamic ->
                try {
                    val e = l[pos]
                    if (!caaTruthy(e.action.execute(e.param))) popup.hide()
                    true
                } catch (err: dynamic) {
                    js("erp")(err)
                    js("undefined")
                }
            }
        ))
        frame.addView(list)
        popup = js("PopupWindow").showDialog("ca.selectAction", frame, -1, -2)
        js("PWM").addPopup(popup)
        if (caaTruthy(onDismiss)) popup.on("hide", onDismiss)
    } catch (e: dynamic) {
        js("erp")(e)
    }
}

private fun caaShowActionsBinder(holder: dynamic, e: dynamic) {
    if (caaInstanceOf(e, js("Object"))) {
        holder.title.setText(js("Common").toString(e.text))
        if (caaTruthy(e.description)) {
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

// ---------------- showActionEdit ----------------

private fun caaShowActionEdit(
    api: dynamic,
    self: dynamic,
    actions: dynamic,
    callback: dynamic,
    defaultActions: dynamic
): dynamic {
    js("G").ui({
        caaShowActionEditUi(api, self, actions, callback, defaultActions)
        js("undefined")
    })
    return js("undefined")
}

private fun caaShowActionEditUi(
    api: dynamic,
    self: dynamic,
    actions: dynamic,
    callback: dynamic,
    defaultActions: dynamic
) {
    try {
        // 原实现的判据是 self.linear，但从未赋值；照原样每次重建菜单与回调。
        if (!caaTruthy(self.linear)) {
            self.contextMenu = caaBuildContextMenu(self)
            self.itemMenu = caaBuildItemMenu(api, self)
            self.removeInvaildAction = { list: dynamic ->
                caaRemoveInvalidActions(list, api.Actions)
                js("undefined")
            }
            self.vmaker = { holder: dynamic -> caaActionEditMaker(holder) }
            self.vbinder = { holder: dynamic, e: dynamic, _i: dynamic, _a: dynamic ->
                caaActionEditBinder(api, holder, e)
                js("undefined")
            }
            self.createAction = { cb: dynamic -> caaCreateAction(api, cb) }
        }
        self.removeInvaildAction(actions)
        val adpt = caaSimpleController(caaNewSimpleListAdapter(actions, self.vmaker, self.vbinder))

        val linear = caaNewLinearLayout()
        linear.setOrientation(js("G.LinearLayout.VERTICAL"))
        linear.setPadding(caaDp(15), caaDp(15), caaDp(15), 0)
        js("Common").applyStyle(linear, "message_bg")

        var popup: dynamic = js("null")

        val header = caaNewLinearLayout()
        header.setOrientation(js("G.LinearLayout.HORIZONTAL"))
        header.setPadding(0, 0, 0, caaDp(10))
        header.setOnClickListener(caaNewClickListener({ _v: dynamic ->
            try {
                val tag = caaObj()
                tag.actions = actions
                tag.defaultActions = defaultActions
                tag.callback = { ->
                    adpt.notifyChange()
                    js("undefined")
                }
                js("Common").showOperateDialog(self.contextMenu, tag)
                true
            } catch (e: dynamic) {
                js("erp")(e)
                js("undefined")
            }
        }))

        val title = caaNewTextView()
        title.setText("编辑动作菜单")
        title.setGravity(caaBitOr(js("G.Gravity.LEFT"), js("G.Gravity.CENTER")))
        title.setPadding(caaDp(10), 0, caaDp(10), 0)
        js("Common").applyStyle(title, "textview_default", 4)
        header.addView(title, caaNewLinearParams3(0, -2, 1.0))

        val menu = caaNewTextView()
        menu.setText("▼")
        menu.setPadding(caaDp(10), 0, caaDp(10), 0)
        menu.setGravity(js("G.Gravity.CENTER"))
        js("Common").applyStyle(menu, "button_highlight", 3)
        header.addView(menu, caaNewLinearParams2(-2, -1))
        linear.addView(header, caaNewLinearParams2(-1, -2))

        val list = caaNewListView()
        list.setAdapter(adpt.self)
        list.setOnItemClickListener(caaNewItemClickListener(
            { parent: dynamic, _view: dynamic, pos: dynamic, _id: dynamic ->
                try {
                    val data = parent.getAdapter().getItem(pos)
                    val tag = caaObj()
                    tag.pos = caaParseInt(pos)
                    tag.data = data
                    tag.actions = actions
                    tag.callback = { ->
                        adpt.notifyChange()
                        js("undefined")
                    }
                    js("Common").showOperateDialog(self.itemMenu, tag)
                } catch (e: dynamic) {
                    js("erp")(e)
                }
                js("undefined")
            }
        ))
        linear.addView(list, caaNewLinearParams3(-1, 0, 1.0))

        val exit = caaNewTextView()
        exit.setText("关闭")
        exit.setGravity(js("G.Gravity.CENTER"))
        exit.setPadding(caaDp(10), caaDp(20), caaDp(10), caaDp(20))
        js("Common").applyStyle(exit, "button_critical", 3)
        exit.setOnClickListener(caaNewClickListener({ _v: dynamic ->
            try {
                popup.exit()
            } catch (e: dynamic) {
                js("erp")(e)
            }
            js("undefined")
        }))
        linear.addView(exit, caaNewLinearParams2(-1, -2))

        popup = caaNewPopupPage(linear, "ca.ActionEdit")
        if (caaTruthy(callback)) popup.on("exit", callback)
        popup.enter()
    } catch (e: dynamic) {
        js("erp")(e)
    }
}

/** showActionEdit 顶部菜单：添加动作 / 恢复默认。 */
private fun caaBuildContextMenu(self: dynamic): dynamic {
    val add = caaObj()
    add.text = "添加动作"
    add.onclick = { _v: dynamic, tag: dynamic ->
        self.createAction({ data: dynamic ->
            tag.actions.push(data)
            tag.callback()
            js("undefined")
        })
        js("undefined")
    }

    val restore = caaObj()
    restore.text = "恢复默认"
    restore.hidden = { tag: dynamic -> !caaIsArray(tag.defaultActions) }
    restore.onclick = { _v: dynamic, tag: dynamic ->
        val a = tag.defaultActions
        caaSetLength(tag.actions, caaLength(a))
        var i = 0
        while (i < caaLength(a)) {
            tag.actions[i] = js("Object").copy(a[i])
            i++
        }
        tag.callback()
        js("undefined")
    }
    return caaList(add, restore)
}

/** showActionEdit 列表项菜单：编辑 / 替换 / 排序 / 移除。 */
private fun caaBuildItemMenu(api: dynamic, self: dynamic): dynamic {
    val edit = caaObj()
    edit.text = "编辑"
    edit.description = "编辑该动作"
    edit.hidden = { tag: dynamic ->
        val action = api.Actions[tag.data.action]
        !caaTruthy(action) || !caaTruthy(action.edit)
    }
    edit.onclick = { _v: dynamic, tag: dynamic ->
        val action = api.Actions[tag.data.action]
        action.edit(tag.data, false, { ->
            tag.callback()
            js("undefined")
        })
        js("undefined")
    }

    val replace = caaObj()
    replace.text = "替换"
    replace.description = "用新的动作替换该动作"
    replace.onclick = { _v: dynamic, tag: dynamic ->
        self.createAction({ data: dynamic ->
            tag.actions[tag.pos] = data
            tag.callback()
            js("undefined")
        })
        js("undefined")
    }

    val sort = caaObj()
    sort.text = "排序"
    sort.description = "调整动作显示的顺序"
    sort.onclick = { _v: dynamic, tag: dynamic ->
        val o = caaObj()
        o.array = tag.actions
        o.selectIndex = tag.pos
        o.getTitle = { e: dynamic ->
            val action = api.Actions[e.action]
            if (caaTruthy(action.getName)) action.getName(e) else action.name
        }
        o.getDescription = { e: dynamic ->
            val action = api.Actions[e.action]
            if (caaTruthy(action.getDescription)) action.getDescription(e) else action.description
        }
        o.callback = { _a: dynamic ->
            tag.callback()
            js("undefined")
        }
        js("Common").showSortDialog(o)
        js("undefined")
    }

    val remove = caaObj()
    remove.text = "移除"
    remove.description = "从列表中移除该动作"
    remove.onclick = { _v: dynamic, tag: dynamic ->
        tag.actions.splice(tag.pos, 1)
        tag.callback()
        js("undefined")
    }
    return caaList(edit, replace, sort, remove)
}

private fun caaActionEditMaker(holder: dynamic): dynamic {
    val layout = caaNewLinearLayout()
    val text1 = caaNewTextView()
    val text2 = caaNewTextView()
    holder.text1 = text1
    holder.text2 = text2
    layout.setLayoutParams(caaNewAbsParams(-1, -2))
    layout.setOrientation(js("G.LinearLayout.VERTICAL"))
    layout.setPadding(caaDp(15), caaDp(15), caaDp(15), caaDp(15))
    text1.setLayoutParams(caaNewLinearParams2(-1, -2))
    js("Common").applyStyle(text1, "item_default", 3)
    layout.addView(text1)
    text2.setPadding(0, caaDp(5), 0, 0)
    text2.setLayoutParams(caaNewLinearParams2(-1, -2))
    js("Common").applyStyle(text2, "textview_prompt", 1)
    layout.addView(text2)
    return layout
}

private fun caaActionEditBinder(api: dynamic, holder: dynamic, e: dynamic) {
    val action = api.Actions[e.action]
    val desp = if (caaTruthy(action.getDescription)) action.getDescription(e) else action.description
    holder.text1.setText(caaString(if (caaTruthy(action.getName)) action.getName(e) else action.name))
    if (caaTruthy(desp)) {
        holder.text2.setText(caaString(desp))
        holder.text2.setVisibility(js("G.View.VISIBLE"))
    } else {
        holder.text2.setVisibility(js("G.View.GONE"))
    }
}

private fun caaCreateAction(api: dynamic, callback: dynamic) {
    val keys = caaActionChoices(api.Actions)
    js("Common").showListChooser(keys, { i: dynamic ->
        val e = keys[i]
        val action = api.Actions[e.key]
        val data = if (caaTruthy(action.create)) action.create() else caaObj()
        data.action = e.key
        if (caaTruthy(action.edit)) {
            action.edit(data, true, { ->
                callback(data)
                js("undefined")
            })
        } else {
            callback(data)
        }
        js("undefined")
    })
}

// ---------------- Actions ----------------

private fun caaBuildActions(api: dynamic): dynamic {
    val actions = caaObj()

    val exit = caaObj()
    exit.name = "关闭命令助手"
    exit.execute = { ->
        api.performExit()
        js("undefined")
    }
    actions["ca.exit"] = exit

    val quickPaste = caaObj()
    quickPaste.name = "快速粘贴"
    quickPaste.execute = { ->
        val a = caaCollectPasteItems(api.his)
        js("Common").showListChooser(a, { id: dynamic ->
            js("gHandler").post({ ->
                api.performPaste(caaString(a[id].cmd), true)
                js("undefined")
            })
            js("undefined")
        }, true)
        js("undefined")
    }
    actions["ca.quickPaste"] = quickPaste

    val switchIconVisibility = caaObj()
    switchIconVisibility.name = "显示/隐藏图标"
    switchIconVisibility.execute = { ->
        if (caaTruthy(api.icon)) {
            api.hideIcon()
        } else {
            api.showIcon()
        }
        js("undefined")
    }
    actions["ca.switchIconVisibility"] = switchIconVisibility

    return actions
}

// ---------------- showAgreement ----------------

private fun caaShowAgreement(api: dynamic, callback: dynamic): dynamic {
    js("G").ui({
        caaShowAgreementUi(api, callback)
        js("undefined")
    })
    return js("undefined")
}

private fun caaShowAgreementUi(api: dynamic, callback: dynamic) {
    try {
        // 这两个变量被 L 配置里的 onClick 闭包捕获，顺序与原 JS 的 var 声明一致。
        var agree: dynamic = js("null")
        var popup: dynamic = js("null")

        val titleOpts = caaObj()
        titleOpts.text = "命令助手"
        titleOpts.padding = caaList(caaDp(10), caaDp(15), caaDp(10), caaDp(15))
        titleOpts.gravity = js("L").Gravity("center")
        titleOpts.layout = caaSize(-1, -2)
        titleOpts.style = "textview_default"
        titleOpts.fontSize = 4
        val title = js("L").TextView(titleOpts)

        val checkOpts = caaObj()
        checkOpts.layout = caaSize(-2, -2)
        checkOpts.checked = caaBool(
            caaGt(api.settings.readAgreement, caaDateParse(js("BuildConfig").licenceUpdate))
        )
        agree = js("L").CheckBox(checkOpts)

        val readTextOpts = caaObj()
        readTextOpts.text = "我已阅读并同意"
        readTextOpts.layout = caaSize(-2, -2)
        readTextOpts.style = "textview_default"
        readTextOpts.fontSize = 2
        val readText = js("L").TextView(readTextOpts)

        val licenseOpts = caaObj()
        licenseOpts.text = "使用许可协议"
        licenseOpts.layout = caaSize(-2, -2)
        licenseOpts.style = "textview_highlight"
        licenseOpts.fontSize = 2
        licenseOpts.onClick = { _v: dynamic ->
            try {
                js("Common").showWebViewDialog(
                    caaUrlObj("https://ca.projectxero.top/blog/about/license/")
                )
            } catch (e: dynamic) {
                js("erp")(e)
            }
            js("undefined")
        }
        val license = js("L").TextView(licenseOpts)

        val andOpts = caaObj()
        andOpts.text = "与"
        andOpts.layout = caaSize(-2, -2)
        andOpts.style = "textview_default"
        andOpts.fontSize = 2
        val andText = js("L").TextView(andOpts)

        val privacyOpts = caaObj()
        privacyOpts.text = "隐私政策"
        privacyOpts.layout = caaSize(-2, -2)
        privacyOpts.style = "textview_highlight"
        privacyOpts.fontSize = 2
        privacyOpts.onClick = { _v: dynamic ->
            try {
                js("Common").showWebViewDialog(
                    caaUrlObj("https://ca.projectxero.top/blog/about/privacy/")
                )
            } catch (e: dynamic) {
                js("erp")(e)
            }
            js("undefined")
        }
        val privacy = js("L").TextView(privacyOpts)

        val agreeRowOpts = caaObj()
        agreeRowOpts.orientation = js("L").LinearLayout("horizontal")
        agreeRowOpts.gravity = js("L").Gravity("center")
        agreeRowOpts.padding = caaList(caaDp(10), caaDp(10), caaDp(10), caaDp(10))
        agreeRowOpts.layout = caaSize(-1, -2)
        agreeRowOpts.children = caaList(agree, readText, license, andText, privacy)
        val agreeRow = js("L").LinearLayout(agreeRowOpts)

        val startOpts = caaObj()
        startOpts.text = "开始使用"
        startOpts.padding = caaList(caaDp(10), caaDp(15), caaDp(10), caaDp(15))
        startOpts.gravity = js("L").Gravity("center")
        startOpts.layout = caaWeighted(0, -2, 0.5)
        startOpts.style = "button_highlight"
        startOpts.fontSize = 3
        startOpts.onClick = { _v: dynamic ->
            try {
                if (caaTruthy(agree.checked)) {
                    api.settings.readAgreement = caaNow()
                    popup.exit()
                } else {
                    js("Common").toast("请先同意使用许可协议与隐私政策")
                }
            } catch (e: dynamic) {
                js("erp")(e)
            }
            js("undefined")
        }
        val start = js("L").TextView(startOpts)

        val closeOpts = caaObj()
        closeOpts.text = "关闭应用"
        closeOpts.padding = caaList(caaDp(10), caaDp(15), caaDp(10), caaDp(15))
        closeOpts.gravity = js("L").Gravity("center")
        closeOpts.layout = caaWeighted(0, -2, 0.5)
        closeOpts.style = "button_critical"
        closeOpts.fontSize = 3
        closeOpts.onClick = { _v: dynamic ->
            try {
                popup.exit(true)
                android.os.Process.killProcess(android.os.Process.myPid())
            } catch (e: dynamic) {
                js("erp")(e)
            }
            js("undefined")
        }
        val close = js("L").TextView(closeOpts)

        val buttonRowOpts = caaObj()
        buttonRowOpts.orientation = js("L").LinearLayout("horizontal")
        buttonRowOpts.layout = caaSize(-1, -2)
        buttonRowOpts.children = caaList(start, close)
        val buttonRow = js("L").LinearLayout(buttonRowOpts)

        val rootOpts = caaObj()
        rootOpts.orientation = js("L").LinearLayout("vertical")
        rootOpts.gravity = js("L").Gravity("center")
        rootOpts.onClick = { _v: dynamic ->
            try {
                agree.performClick()
            } catch (e: dynamic) {
                js("erp")(e)
            }
            js("undefined")
        }
        rootOpts.children = caaList(title, agreeRow, buttonRow)
        val root = js("L").LinearLayout(rootOpts)

        val scrollOpts = caaObj()
        scrollOpts.style = "message_bg"
        scrollOpts.child = root
        val scroll = js("L").ScrollView(scrollOpts)

        popup = js("PopupPage").showSideBar("ca.AgreementDialog", scroll, "bottom", -2, 0, true)
        if (caaTruthy(callback)) popup.on("exit", callback)
    } catch (e: dynamic) {
        js("erp")(e)
    }
}

// ---------------- showAgreementSync ----------------

private fun caaShowAgreementSync(api: dynamic): dynamic {
    js("Threads").awaitPromise({ resolve: dynamic ->
        api.showAgreement(resolve)
        js("undefined")
    })
    return js("undefined")
}

// ---------------- 安装 ----------------

fun installCaAgreement(api: dynamic) {
    // 顺序与原对象字面量一致，方便对照 API key。
    api.showActions = caaSelf2Js { self: dynamic, actions: dynamic, onDismiss: dynamic ->
        caaShowActions(api, self, actions, onDismiss)
    }
    api.showActionEdit = caaSelf3Js {
            self: dynamic, actions: dynamic, callback: dynamic, defaultActions: dynamic ->
        caaShowActionEdit(api, self, actions, callback, defaultActions)
    }
    api.Actions = caaBuildActions(api)
    api.showAgreement = { callback: dynamic ->
        caaShowAgreement(api, callback)
    }
    api.showAgreementSync = { ->
        caaShowAgreementSync(api)
    }
}
