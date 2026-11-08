package ca.kotlin.ui

import ca.kotlin.core.*


/**
 * modules/JSONEdit.js 的 Kotlin/JS 版本。
 *
 * JSON 编辑器包含三种状态：主菜单、树形编辑器、新建条目菜单。
 * 原实现把状态挂在具名函数表达式 self 上（JSONEdit.showEdit.showEdit 之类），
 * 这里改成稳定的 state 对象，其它模块仍通过 JSONEdit.xxx 调用。
 * `openCallback` / `saveCallback` 依赖动态 this（操作对话框的 tag），
 * 因此保留薄 JS wrapper；菜单、回调时序和异常行为与原文件一致。
 */
private val jsonEditApi: dynamic = js("({})")
private val jsonEditMainState: dynamic = js("({})")
private val jsonEditEditState: dynamic = js("({})")
private val jsonEditNewItemState: dynamic = js("({})")

// ---------------- JS 语义 helper ----------------
private val jeTruthyJs: dynamic = js("function (v) { return !!v; }")
private val jeStringJs: dynamic = js("function (v) { return String(v); }")
private val jeLengthJs: dynamic = js("function (v) { return v.length | 0; }")
private val jeTypeofJs: dynamic = js("function (v) { return typeof v; }")
private val jeLooseEqJs: dynamic = js("function (a, b) { return a == b; }")
private val jeStrictEqJs: dynamic = js("function (a, b) { return a === b; }")
private val jeStrictNeJs: dynamic = js("function (a, b) { return a !== b; }")
private val jeNotLooselyNullJs: dynamic = js("function (v) { return v != null; }")
private val jeIsArrayJs: dynamic = js("function (v) { return Array.isArray(v); }")
private val jeInJs: dynamic = js("function (o, k) { return k in o; }")
private val jeDeleteJs: dynamic = js("function (o, k) { delete o[k]; }")
private val jeForInJs: dynamic = js("function (o, f) { for (var i in o) f(i); }")
private val jeThisJs: dynamic = js("function (f) { return function () { return f(this, arguments); }; }")
private val jeInstanceOfJs: dynamic = js("function (v, c) { return v instanceof c; }")
private val jeNewErrorJs: dynamic = js("function (m) { return new Error(m); }")
private val jeDpJs: dynamic = js("function (n) { return n * G.dp; }")
private val jeFloorHalfJs: dynamic = js("function (v) { return Math.floor(v / 2); }")
private val jeNumberJs: dynamic = js("function (v) { return Number(v); }")
private val jeIsFiniteJs: dynamic = js("function (v) { return isFinite(v); }")
private val jeJsonParseJs: dynamic = js("function (s) { return JSON.parse(s); }")
private val jeJsonStringifyJs: dynamic = js("function (v, r, s) { return JSON.stringify(v, r, s); }")
private val jeGapJs: dynamic = js("function (n) { return G.dp * n; }")
private val jeHalfScreenWidthJs: dynamic = js("function () { return 0.5 * Common.getScreenWidth(); }")
private val jeToStringArrayJs: dynamic = js("function (a) { return a.map(function (e) { return String(e); }); }")

private fun jeTruthy(v: dynamic): Boolean = jeTruthyJs(v)
private fun jeString(v: dynamic): dynamic = jeStringJs(v)
private fun jeLength(v: dynamic): Int = jeLengthJs(v)
private fun jeTypeof(v: dynamic): dynamic = jeTypeofJs(v)
private fun jeLooseEq(a: dynamic, b: dynamic): Boolean = jeLooseEqJs(a, b)
private fun jeStrictEq(a: dynamic, b: dynamic): Boolean = jeStrictEqJs(a, b)
private fun jeStrictNe(a: dynamic, b: dynamic): Boolean = jeStrictNeJs(a, b)
private fun jeNotLooselyNull(v: dynamic): Boolean = jeNotLooselyNullJs(v)
private fun jeIsArray(v: dynamic): Boolean = jeIsArrayJs(v)
private fun jeIn(o: dynamic, k: dynamic): Boolean = jeInJs(o, k)
private fun jeDelete(o: dynamic, k: dynamic) {
    jeDeleteJs(o, k)
}
private fun jeForIn(o: dynamic, f: dynamic) {
    jeForInJs(o, f)
}
private fun jeInstanceOf(v: dynamic, c: dynamic): Boolean = jeInstanceOfJs(v, c)
private fun jeDp(n: Int): dynamic = jeDpJs(n)

private fun jeObject(): dynamic = js("({})")

private fun jeList(vararg items: dynamic): dynamic {
    val a = js("[]")
    var i = 0
    while (i < items.size) {
        a.push(items[i])
        i++
    }
    return a
}

private fun jeSize(width: dynamic, height: dynamic): dynamic {
    val o = js("({})")
    o.width = width
    o.height = height
    return o
}

private fun jeApplyStyle(view: dynamic, style: dynamic, fontSize: dynamic) {
    js("Common").applyStyle(view, style, fontSize)
}

// ---------------- Android / UI helper ----------------
private val jeNewTextViewJs: dynamic = js("function (c) { return new G.TextView(c); }")
private val jeNewEditTextJs: dynamic = js("function (c) { return new G.EditText(c); }")
private val jeNewCheckBoxJs: dynamic = js("function (c) { return new G.CheckBox(c); }")
private val jeNewListViewJs: dynamic = js("function (c) { return new G.ListView(c); }")
private val jeNewScrollViewJs: dynamic = js("function (c) { return new G.ScrollView(c); }")
private val jeNewLinearLayoutJs: dynamic = js("function (c) { return new G.LinearLayout(c); }")
private val jeNewLinearParamsJs: dynamic = js(
    "function (w, h, weight) { return arguments.length > 2 ? new G.LinearLayout.LayoutParams(w, h, weight) : new G.LinearLayout.LayoutParams(w, h); }"
)
private val jeNewAbsListParamsJs: dynamic = js(
    "function (w, h) { return new G.AbsListView.LayoutParams(w, h); }"
)
private val jeNewFrameParamsJs: dynamic = js(
    "function (w, h, g) { return new G.FrameLayout.LayoutParams(w, h, g); }"
)
private val jeNewClickListenerJs: dynamic = js(
    "function (f) { return new G.View.OnClickListener({ onClick: f }); }"
)
private val jeNewItemClickListenerJs: dynamic = js(
    "function (f) { return new G.AdapterView.OnItemClickListener({ onItemClick: f }); }"
)
private val jeNewItemLongClickListenerJs: dynamic = js(
    "function (f) { return new G.AdapterView.OnItemLongClickListener({ onItemLongClick: f }); }"
)
private val jeNewTouchListenerJs: dynamic = js(
    "function (f) { return new G.View.OnTouchListener({ onTouch: f }); }"
)
private val jeNewPopupPageJs: dynamic = js("function (v, k) { return new PopupPage(v, k); }")
private val jeNewSimpleListAdapterJs: dynamic = js(
    "function (a, b, c, d, e) { return new SimpleListAdapter(a, b, c, d, e); }"
)
private val jeGetControllerJs: dynamic = js(
    "function (a) { return SimpleListAdapter.getController(a); }"
)
private val jeNewCanvasJs: dynamic = js("function (b) { return new G.Canvas(b); }")
private val jeNewPaintJs: dynamic = js("function () { return new G.Paint(); }")
private val jeNewPathJs: dynamic = js("function () { return new G.Path(); }")
private val jeNewBitmapDrawableJs: dynamic = js(
    "function (r, b) { return new G.BitmapDrawable(r, b); }"
)

private fun jeNewTextView(context: dynamic): dynamic = jeNewTextViewJs(context)
private fun jeNewEditText(context: dynamic): dynamic = jeNewEditTextJs(context)
private fun jeNewCheckBox(context: dynamic): dynamic = jeNewCheckBoxJs(context)
private fun jeNewListView(context: dynamic): dynamic = jeNewListViewJs(context)
private fun jeNewScrollView(context: dynamic): dynamic = jeNewScrollViewJs(context)
private fun jeNewLinearLayout(context: dynamic): dynamic = jeNewLinearLayoutJs(context)
private fun jeNewPopupPage(view: dynamic, key: dynamic): dynamic = jeNewPopupPageJs(view, key)

// ---------------- 状态迁移 ----------------

private fun jsonEditOnCreate(self: dynamic): dynamic {
    js("Intl").mapNamespace(self, "intl", "jsonEdit")
    return js("undefined")
}

private fun jsonEditCreate(self: dynamic, callback: dynamic, rootname: dynamic) {
    self.showNewItem({ data: dynamic ->
        jsonEditCreateCallback(callback, rootname, data)
    })
}

private fun jsonEditCreateCallback(callback: dynamic, rootname: dynamic, data: dynamic): dynamic {
    if (jeInstanceOf(data, js("Object"))) {
        val options = jeObject()
        options.source = data
        options.rootname = rootname
        options.update = {
            callback(data)
            js("undefined")
        }
        js("JSONEdit").show(options)
    } else {
        return callback(data)
    }
    return js("undefined")
}

private fun jsonEditOpenCallback(self: dynamic, uri: dynamic, data: dynamic, needEditablePrompt: dynamic) {
    js("Log").d("open")
    val options = jeObject()
    options.source = data
    options.update = {
        js("Log").d("update")
        self.saveCallback(uri, data)
        js("undefined")
    }
    if (!jeTruthy(js("JSONEdit").show(options))) {
        if (jeTruthy(needEditablePrompt)) js("Common").toast(self.intl.nowhereEditable)
        self.saveCallback(uri, data)
    }
}

private fun jsonEditSaveCallback(self: dynamic, mainState: dynamic, uri: dynamic, data: dynamic) {
    val tag = jeObject()
    tag.uri = uri
    tag.data = data
    tag.editCallback = {
        self.openCallback(uri, data, true)
        js("undefined")
    }
    tag.replaceCallback = { newData: dynamic ->
        self.openCallback(uri, newData)
        js("undefined")
    }
    js("Common").showOperateDialog(mainState.saveMenu, tag)
}

private fun jsonEditMain(self: dynamic) {
    if (!jeTruthy(self.menu)) {
        self.intl = js("Intl").getNamespace("jsonEdit.main")

        val saveMenu = js("[]")
        val edit = jeObject()
        edit.text = self.intl.edit
        edit.description = self.intl.edit_desc
        edit.hidden = { tag: dynamic -> jeStrictEq(tag.data, js("null")) }
        edit.onclick = { _v: dynamic, tag: dynamic ->
            tag.editCallback()
            js("undefined")
        }
        saveMenu.push(edit)

        val replace = jeObject()
        replace.text = self.intl.replace
        replace.description = self.intl.replace_desc
        replace.hidden = { tag: dynamic -> jeStrictNe(tag.data, js("null")) }
        replace.onclick = { _v: dynamic, tag: dynamic ->
            js("JSONEdit").create({ data: dynamic ->
                tag.replaceCallback(data)
                js("undefined")
            })
            js("undefined")
        }
        saveMenu.push(replace)

        val copy = jeObject()
        copy.text = self.intl.copy
        copy.description = self.intl.copy_desc
        copy.onclick = { _v: dynamic, tag: dynamic ->
            js("Common").setClipboardText(jeJsonStringifyJs(tag.data, js("null"), "\t"))
            js("Common").toast(self.intl.copy_success)
            true
        }
        saveMenu.push(copy)

        val save = jeObject()
        save.text = self.intl.save
        save.description = self.intl.save_desc
        save.hidden = { tag: dynamic -> !jeTruthy(tag.uri) }
        save.onclick = { _v: dynamic, tag: dynamic ->
            try {
                js("ExternalStorage").writeFileContent(tag.uri, jeJsonStringifyJs(tag.data))
                js("Common").toast(self.intl.save_success)
            } catch (e: dynamic) {
                js("Common").toast(self.intl.resolve("save_failed", e))
            }
            true
        }
        saveMenu.push(save)

        val saveAs = jeObject()
        saveAs.text = self.intl.saveAs
        saveAs.description = self.intl.saveAs_desc
        saveAs.onclick = { _v: dynamic, tag: dynamic ->
            val actions = jeObject()
            actions.mimeType = "application/json"
            actions.hint = "untitled.json"
            actions.callback = { uri: dynamic ->
                try {
                    js("ExternalStorage").writeFileContent(uri, jeJsonStringifyJs(tag.data))
                    tag.uri = uri
                    js("Common").toast(self.intl.save_success)
                } catch (e: dynamic) {
                    js("Common").toast(self.intl.resolve("save_failed", e))
                }
                js("undefined")
            }
            js("ExternalStorage").showSaveActions(actions)
            true
        }
        saveMenu.push(saveAs)

        val close = jeObject()
        close.text = js("Common").intl.close
        close.onclick = { _v: dynamic, _tag: dynamic -> js("undefined") }
        saveMenu.push(close)

        self.saveMenu = saveMenu

        val openMenu = js("[]")
        val newItem = jeObject()
        newItem.text = self.intl.new
        newItem.description = self.intl.new_desc
        newItem.onclick = { _v: dynamic, tag: dynamic ->
            js("JSONEdit").create({ data: dynamic ->
                tag.saveCallback(null, data)
                js("undefined")
            })
            js("undefined")
        }
        openMenu.push(newItem)

        val open = jeObject()
        open.text = self.intl.open
        open.description = self.intl.open_desc
        open.onclick = { _v: dynamic, tag: dynamic ->
            val actions = jeObject()
            actions.mimeType = "application/json"
            actions.callback = { uri: dynamic ->
                var data: dynamic = js("undefined")
                try {
                    data = jeJsonParseJs(js("ExternalStorage").readFileContent(uri, "UTF-8"))
                    js("ExternalStorage").tryTakeUriPermission(uri)
                    tag.openCallback(uri, data, false)
                } catch (e: dynamic) {
                    js("Common").toast(js("JSONEdit").intl.resolve("invaildJSON", e))
                }
                js("undefined")
            }
            js("ExternalStorage").showOpenActions(actions)
            js("undefined")
        }
        openMenu.push(open)

        val closeOpen = jeObject()
        closeOpen.text = js("Common").intl.close
        closeOpen.onclick = { _v: dynamic, _tag: dynamic -> js("undefined") }
        openMenu.push(closeOpen)

        self.openMenu = openMenu
    }

    val tag = jeObject()
    tag.openCallback = jeThisJs { thisObj: dynamic, args: dynamic ->
        jsonEditOpenCallback(thisObj, args[0], args[1], args[2])
        js("undefined")
    }
    tag.saveCallback = jeThisJs { thisObj: dynamic, args: dynamic ->
        jsonEditSaveCallback(thisObj, self, args[0], args[1])
        js("undefined")
    }
    js("Common").showOperateDialog(self.openMenu, tag)
}

private fun jsonEditDebugIsTree(o: dynamic): Boolean {
    if (jeLooseEq(o, js("null"))) return false
    if (jeInstanceOf(o, js("java.lang.CharSequence"))) return false
    val t = jeTypeof(o)
    return jeLooseEq(t, "object") || jeLooseEq(t, "function")
}

private fun jsonEditBuildItemAccessors(): dynamic {
    val jsonAccessor = jeObject()
    jsonAccessor.listItems = { o: dynamic -> js("Object").keys(o) }
    jsonAccessor.getCount = { o: dynamic -> jeLength(jsonAccessor.listItems(o)) }
    jsonAccessor.isTree = { o: dynamic -> jeInstanceOf(o, js("Object")) }
    jsonAccessor.toArrayItemName = js("function (index) { return '#' + (parseInt(index) + 1); }")

    val debugAccessor = jeObject()
    debugAccessor.listItems = { o: dynamic ->
        try {
            js("Object").getOwnPropertyNames(o)
        } catch (e: dynamic) {
            js("Object").keys(o)
        }
    }
    debugAccessor.getCount = { o: dynamic -> jeLength(debugAccessor.listItems(o)) }
    debugAccessor.isTree = { o: dynamic -> jsonEditDebugIsTree(o) }
    debugAccessor.toArrayItemName = { index: dynamic -> index }

    val accessors = jeObject()
    accessors.json = jsonAccessor
    accessors.debug = debugAccessor
    return accessors
}

private fun jsonEditShow(self: dynamic, o: dynamic): dynamic {
    val options = jeObject()
    options.itemAccessor = if (jeTruthy(o.showAll)) self.itemAccessor.debug else self.itemAccessor.json

    val name = if (jeTruthy(o.rootname)) o.rootname else self.intl.root
    var data = o.source
    if (jeStrictEq(data, js("null"))) {
        return false
    } else if (jeTruthy(options.itemAccessor.isTree(data))) {
        val path = js("[]")
        val root = jeObject()
        root.name = name
        root.data = data
        root.pos = 0
        path.push(root)
        options.path = path
        if (jeTruthy(o.path)) {
            jeForIn(o.path) { i: dynamic ->
                val item = o.path[i]
                data = data[item]
                val entry = jeObject()
                entry.name = jeString(item)
                entry.data = if (jeTruthy(options.itemAccessor.isTree(data))) data else jeObject()
                entry.pos = 0
                path.push(entry)
            }
        }
        self.showEdit(options, o.update)
    } else {
        self.showData(self.intl.resolve("editData", name), data, { newValue: dynamic ->
            o.source = newValue
            if (jeTruthy(o.update)) o.update()
            js("undefined")
        })
    }
    return true
}

private fun jsonEditGetHeaderDivider(height: dynamic): dynamic {
    val width = jeFloorHalfJs(height)
    val bmp = js("G.Bitmap").createBitmap(width, height, js("G.Bitmap.Config.ARGB_8888"))
    val cv = jeNewCanvasJs(bmp)
    val pa = jeNewPaintJs()
    val ph = jeNewPathJs()
    pa.setStrokeCap(js("G.Paint.Cap.BUTT"))
    pa.setStyle(js("G.Paint.Style.STROKE"))
    js("IntColor").Paint.setColor(pa, js("Common").theme.promptcolor)
    pa.setStrokeWidth(2)
    pa.setAntiAlias(true)
    ph.moveTo(0, 0)
    ph.lineTo(width, width)
    ph.lineTo(0, height)
    cv.drawPath(ph, pa)
    return jeNewBitmapDrawableJs(js("ctx").getResources(), bmp)
}

private fun jsonEditEditShowItemAction(self: dynamic, name: dynamic) {
    val cd = self.currentTree.data
    var data: dynamic = js("undefined")
    try {
        data = cd[name]
    } catch (e: dynamic) {
        js("Common").toast(e)
    }
    val menu = if (jeIsArray(cd)) {
        self.arrMenu
    } else if (jeTruthy(self.itemAccessor.isTree(cd))) {
        self.objMenu
    } else {
        js("obj.menu")
    }
    val tag = jeObject()
    tag.name = name
    tag.src = cd
    tag.data = data
    js("Common").showOperateDialog(menu, tag)
}

private fun jsonEditEditShowCreateKey(
    self: dynamic,
    data: dynamic,
    newItem: dynamic,
    s: dynamic
): dynamic {
    if (!jeTruthy(s)) {
        js("Common").toast(js("JSONEdit").intl.keyNameEmpty)
        return js("undefined")
    } else if (jeIn(s, data)) {
        js("Common").toast(js("JSONEdit").intl.keyNameExists)
        return js("undefined")
    } else {
        try {
            data[s] = newItem
        } catch (e: dynamic) {
            js("Common").toast(e)
        }
        self.refresh()
    }
    return js("undefined")
}

private fun jsonEditEditShowCreate(self: dynamic): dynamic {
    js("JSONEdit").showNewItem({ newItem: dynamic ->
        jsonEditEditShowCreateItem(self, newItem)
    })
    return js("undefined")
}

private fun jsonEditEditShowCreateItem(self: dynamic, newItem: dynamic): dynamic {
    val data = self.currentTree.data
    if (jeIsArray(data)) {
        data.push(newItem)
        self.refresh()
    } else if (jeTruthy(self.itemAccessor.isTree(data))) {
        val dialog = jeObject()
        dialog.title = js("JSONEdit").intl.inputKeyName
        dialog.callback = { s: dynamic ->
            jsonEditEditShowCreateKey(self, data, newItem, s)
        }
        js("Common").showInputDialog(dialog)
    } else {
        js("Common").toast(js("JSONEdit").intl.unableToInsert)
    }
    return js("undefined")
}

private fun jsonEditEditPathClick(self: dynamic, v: dynamic): dynamic {
    val i = self.pathbar.indexOfChild(v)
    self.path.splice(i + 1)
    self.refresh()
    return js("undefined")
}

private fun jsonEditEditOnBack(self: dynamic): dynamic {
    if (jeLength(self.path) > 1) {
        self.path.pop()
        self.refresh()
    } else {
        self.popup.exit()
    }
    return js("undefined")
}

private fun jsonEditEditRefresh(self: dynamic): dynamic {
    self.currentTree = self.path[jeLength(self.path) - 1]
    val cd = self.currentTree.data
    self.pathbar.removeAllViews()
    jeForIn(self.path) { i: dynamic ->
        val e = self.path[i]
        val lbl = jeNewTextView(js("ctx"))
        lbl.setText(jeString(e.name))
        lbl.setLayoutParams(jeNewLinearParamsJs(-2, -1))
        lbl.setPadding(jeDp(10), jeDp(10), jeDp(10), jeDp(10))
        lbl.setOnClickListener(self.pathClick)
        jeApplyStyle(lbl, "item_default", 2)
        self.pathbar.addView(lbl)
    }
    val items = self.itemAccessor.listItems(cd)
    self.adpt.setArray(items)
    js("gHandler").post({
        try {
            self.list.setSelection(self.currentTree.pos)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
    return js("undefined")
}

private fun jsonEditEditVmaker(self: dynamic, holder: dynamic): dynamic {
    val ctx = js("ctx")
    val hl = jeNewLinearLayout(ctx)
    hl.setOrientation(js("G.LinearLayout.HORIZONTAL"))
    hl.setLayoutParams(jeNewAbsListParamsJs(-1, -2))
    hl.setPadding(jeDp(20), jeDp(10), jeDp(20), jeDp(10))

    val vl = jeNewLinearLayout(ctx)
    vl.setOrientation(js("G.LinearLayout.VERTICAL"))
    vl.setLayoutParams(jeNewLinearParamsJs(-2, -2, 1.0))
    vl.getLayoutParams().gravity = js("G.Gravity.CENTER")

    val name = jeNewTextView(ctx)
    holder.name = name
    name.setEllipsize(js("G.TextUtils.TruncateAt.END"))
    name.setLayoutParams(jeNewLinearParamsJs(-1, -2))
    jeApplyStyle(name, "textview_default", 3)
    vl.addView(name)

    val dataView = jeNewTextView(ctx)
    holder.data = dataView
    dataView.setMaxLines(2)
    dataView.setEllipsize(js("G.TextUtils.TruncateAt.END"))
    dataView.setLayoutParams(jeNewLinearParamsJs(-1, -2))
    jeApplyStyle(dataView, "textview_prompt", 1)
    vl.addView(dataView)
    hl.addView(vl)

    val more = jeNewTextView(ctx)
    more.setText(">")
    more.setPadding(jeDp(10), jeDp(10), jeDp(10), jeDp(10))
    more.setLayoutParams(jeNewLinearParamsJs(-2, -2, 0))
    more.getLayoutParams().gravity = js("G.Gravity.CENTER")
    jeApplyStyle(more, "button_secondary", 4)
    more.setOnClickListener(jeNewClickListenerJs({ _v: dynamic ->
        try {
            self.showItemAction(holder.e)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }))
    hl.addView(more)
    return hl
}

private fun jsonEditEditVbinder(
    self: dynamic,
    holder: dynamic,
    e: dynamic,
    _i: dynamic,
    _a: dynamic
): dynamic {
    val par = self.currentTree.data
    holder.name.setText(if (jeIsArray(par)) self.itemAccessor.toArrayItemName(e) else jeString(e))
    holder.data.setText(self.getDesp(par, e))
    holder.e = e
    return js("undefined")
}

private fun jsonEditEditGetDesp(self: dynamic, obj: dynamic, propertyName: dynamic): dynamic {
    var result: dynamic = js("undefined")
    try {
        val e = obj[propertyName]
        if (jeIsArray(e)) {
            result = if (jeLength(e) != 0) {
                js("JSONEdit").intl.resolve("arrayDesc", e[0], jeLength(e))
            } else {
                js("JSONEdit").intl.emptyArrayDesc
            }
        } else if (jeInstanceOf(e, js("Object")) &&
            jeStrictNe(jeTypeof(e), "function") &&
            !jeInstanceOf(e, js("java.lang.CharSequence"))
        ) {
            result = js("JSONEdit").intl.resolve("objectDesc", self.itemAccessor.getCount(e))
        } else if (jeStrictEq(e, js("null"))) {
            result = js("JSONEdit").intl.nullDesc
        } else {
            result = jeString(e)
        }
        return result
    } catch (er: dynamic) {
        js("Log").e(er)
        return jeString(er)
    }
}

private fun jsonEditEditListItemClick(
    self: dynamic,
    parent: dynamic,
    view: dynamic,
    pos: dynamic,
    _id: dynamic
): dynamic {
    try {
        if (jeLooseEq(view, self.create)) {
            self.showCreate()
            return true
        }
        val name = parent.getAdapter().getItem(pos)
        var data: dynamic = js("undefined")
        try {
            data = self.currentTree.data[name]
        } catch (e: dynamic) {
            js("Common").toast(e)
            return js("undefined")
        }
        if (jeTruthy(self.itemAccessor.isTree(data))) {
            self.enterTree(name, data)
        } else if (jeNotLooselyNull(data)) {
            js("JSONEdit").showData(
                js("JSONEdit").intl.resolve("editData", name),
                data,
                { newValue: dynamic ->
                    self.path[jeLength(self.path) - 1].data[name] = newValue
                    self.refresh()
                    js("undefined")
                }
            )
        }
    } catch (e: dynamic) {
        js("erp")(e)
    }
    return js("undefined")
}

private fun jsonEditEditListItemLongClick(
    self: dynamic,
    parent: dynamic,
    view: dynamic,
    pos: dynamic,
    _id: dynamic
): dynamic {
    try {
        if (jeLooseEq(view, self.create)) return true
        self.showItemAction(parent.getAdapter().getItem(pos))
        return true
    } catch (e: dynamic) {
        js("erp")(e)
        return true
    }
}

private fun jsonEditEditBuild(self: dynamic) {
    self.menuIntl = js("Intl").getNamespace("jsonEdit.itemMenu")
    self.getHeaderDivider = { height: dynamic -> jsonEditGetHeaderDivider(height) }

    val menu = js("[]")

    val copy = jeObject()
    copy.text = self.menuIntl.copy
    copy.onclick = { _v: dynamic, tag: dynamic ->
        val clip = jeObject()
        clip.name = tag.name
        clip.item = tag.data
        js("JSONEdit").clipboard = clip
        self.refresh()
        js("undefined")
    }
    menu.push(copy)

    val cut = jeObject()
    cut.text = self.menuIntl.cut
    cut.onclick = { _v: dynamic, tag: dynamic ->
        val clip = jeObject()
        clip.name = tag.name
        clip.item = tag.data
        js("JSONEdit").clipboard = clip
        if (jeIsArray(tag.src)) {
            tag.src.splice(js("parseInt")(tag.name), 1)
        } else {
            jeDelete(tag.src, tag.name)
        }
        self.refresh()
        js("undefined")
    }
    menu.push(cut)

    val replace = jeObject()
    replace.text = self.menuIntl.replace
    replace.onclick = { _v: dynamic, tag: dynamic ->
        js("JSONEdit").showNewItem({ newItem: dynamic ->
            try {
                tag.src[tag.name] = newItem
            } catch (e: dynamic) {
                js("Common").toast(e)
            }
            self.refresh()
            js("undefined")
        })
        js("undefined")
    }
    menu.push(replace)

    val remove = jeObject()
    remove.text = self.menuIntl.remove
    remove.onclick = { _v: dynamic, tag: dynamic ->
        if (jeIsArray(tag.src)) {
            tag.src.splice(js("parseInt")(tag.name), 1)
        } else {
            jeDelete(tag.src, tag.name)
        }
        self.refresh()
        js("undefined")
    }
    menu.push(remove)

    val rawEdit = jeObject()
    rawEdit.text = self.menuIntl.rawEdit
    rawEdit.onclick = { _v: dynamic, tag: dynamic ->
        js("JSONEdit").showRawEdit(tag.data, { v: dynamic ->
            try {
                tag.src[tag.name] = v
            } catch (e: dynamic) {
                js("Common").toast(e)
            }
            self.refresh()
            js("undefined")
        })
        js("undefined")
    }
    menu.push(rawEdit)
    self.menu = menu

    val objMenu = js("[]")
    val rename = jeObject()
    rename.text = self.menuIntl.rename
    rename.onclick = { _v: dynamic, tag: dynamic ->
        val dialog = jeObject()
        dialog.title = self.menuIntl.rename
        dialog.callback = { s: dynamic ->
            try {
                tag.src[s] = tag.src[tag.name]
                jeDelete(tag.src, tag.name)
            } catch (e: dynamic) {
                js("Common").toast(e)
            }
            self.refresh()
            js("undefined")
        }
        dialog.defaultValue = tag.name
        js("Common").showInputDialog(dialog)
        js("undefined")
    }
    objMenu.push(rename)
    self.objMenu = objMenu.concat(self.menu)

    val arrMenu = js("[]")
    val insertBefore = jeObject()
    insertBefore.text = self.menuIntl.insertBefore
    insertBefore.onclick = { _v: dynamic, tag: dynamic ->
        js("JSONEdit").showNewItem({ newItem: dynamic ->
            tag.src.splice(js("parseInt")(tag.name), 0, newItem)
            self.refresh()
            js("undefined")
        })
        js("undefined")
    }
    arrMenu.push(insertBefore)
    self.arrMenu = arrMenu.concat(self.menu)

    self.showItemAction = { name: dynamic -> jsonEditEditShowItemAction(self, name) }
    self.pathClick = { v: dynamic -> jsonEditEditPathClick(self, v) }
    self.onBack = { jsonEditEditOnBack(self) }
    self.init = { options: dynamic, callback: dynamic ->
        self.callback = callback
        self.path = options.path
        self.itemAccessor = options.itemAccessor
        self.refresh()
        js("undefined")
    }
    self.refresh = { jsonEditEditRefresh(self) }
    self.showCreate = { jsonEditEditShowCreate(self) }
    self.enterTree = { name: dynamic, data: dynamic ->
        if (jeTruthy(self.currentTree)) {
            self.currentTree.pos = self.list.getFirstVisiblePosition()
        }
        val entry = jeObject()
        entry.name = jeString(name)
        entry.data = data
        entry.pos = 0
        self.path.push(entry)
        self.refresh()
        js("gHandler").post({
            try {
                self.hscr.fullScroll(js("G.View.FOCUS_RIGHT"))
            } catch (e: dynamic) {
                js("erp")(e)
            }
            js("undefined")
        })
        js("undefined")
    }
    self.vmaker = { holder: dynamic -> jsonEditEditVmaker(self, holder) }
    self.vbinder = { holder: dynamic, e: dynamic, i: dynamic, a: dynamic ->
        jsonEditEditVbinder(self, holder, e, i, a)
    }
    self.getDesp = { obj: dynamic, propertyName: dynamic ->
        jsonEditEditGetDesp(self, obj, propertyName)
    }
    self.adpt = jeGetControllerJs(jeNewSimpleListAdapterJs(js("[]"), self.vmaker, self.vbinder, js("null"), true))

    // 返回按钮 + 路径栏
    val backConfig = jeObject()
    backConfig.text = "< " + js("Common").intl.back
    backConfig.padding = jeList(jeDp(10), jeDp(10), jeDp(10), jeDp(10))
    backConfig.layoutWidth = -2
    backConfig.layoutHeight = -2
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
    backConfig.inflate = { view: dynamic ->
        view.measure(0, 0)
        self.headerHeight = view.getMeasuredHeight()
        js("undefined")
    }
    val back = js("L").TextView(backConfig)

    val pathbarConfig = jeObject()
    pathbarConfig.dividerDrawable = self.getHeaderDivider(self.headerHeight)
    pathbarConfig.showDividers = js("L").LinearLayout("show_divider_middle")
    pathbarConfig.padding = jeList(jeDp(10), 0, jeDp(10), 0)
    pathbarConfig.layoutWidth = -1
    pathbarConfig.layoutHeight = -1
    val pathbar = js("L").LinearLayout(pathbarConfig)
    self.pathbar = pathbar

    val hscrConfig = jeObject()
    hscrConfig.horizontalScrollBarEnabled = true
    hscrConfig.layoutWidth = -1
    hscrConfig.layoutHeight = -1
    hscrConfig.child = pathbar
    val hscr = js("L").HorizontalScrollView(hscrConfig)
    self.hscr = hscr

    val topBarConfig = jeObject()
    topBarConfig.orientation = js("L").LinearLayout("horizontal")
    topBarConfig.layoutWidth = -1
    topBarConfig.layoutHeight = -2
    topBarConfig.style = "bar_float"
    topBarConfig.children = jeList(back, hscr)
    val topBar = js("L").LinearLayout(topBarConfig)

    // 列表头（新建条目）
    val createConfig = jeObject()
    createConfig.text = js("JSONEdit").intl.addItem
    createConfig.gravity = js("L").Gravity("center")
    createConfig.padding = jeList(jeDp(20), jeDp(20), jeDp(20), jeDp(20))
    createConfig.layoutWidth = -1
    createConfig.layoutHeight = -2
    createConfig.style = "textview_default"
    createConfig.fontSize = 3
    self.create = js("L").TextView(createConfig)
    val createView = self.create

    val listConfig = jeObject()
    listConfig.adapter = self.adpt.self
    listConfig.style = "message_bg"
    listConfig.layoutWidth = -1
    listConfig.layoutHeight = -1
    listConfig._headerView = createView
    listConfig.inflate = { view: dynamic ->
        view.addHeaderView(listConfig._headerView)
        if (jeLooseEq(js("G").style, "Material")) {
            view.setVerticalScrollbarPosition(js("G.View.SCROLLBAR_POSITION_LEFT"))
            view.setFastScrollEnabled(true)
            view.setFastScrollAlwaysVisible(false)
        }
        js("undefined")
    }
    listConfig.onItemClick = { parent: dynamic, view: dynamic, pos: dynamic, id: dynamic ->
        jsonEditEditListItemClick(self, parent, view, pos, id)
    }
    listConfig.onItemLongClick = { parent: dynamic, view: dynamic, pos: dynamic, id: dynamic ->
        jsonEditEditListItemLongClick(self, parent, view, pos, id)
    }
    self.list = js("L").ListView(listConfig)

    val mainConfig = jeObject()
    mainConfig.orientation = js("L").LinearLayout("vertical")
    mainConfig.children = jeList(topBar, self.list)
    self.main = js("L").LinearLayout(mainConfig)

    self.popup = jeNewPopupPage(self.main, "jsonedit.Main")
    self.popup.on("back", { _name: dynamic, cancelDefault: dynamic ->
        self.onBack()
        cancelDefault()
        js("undefined")
    })
    self.popup.on("exit", {
        if (jeTruthy(self.callback)) self.callback()
        js("undefined")
    })
    js("PWM").registerResetFlag(self, "main")
}

private fun jsonEditShowEdit(self: dynamic, options: dynamic, callback: dynamic) {
    js("G").ui({
        try {
            if (!jeTruthy(self.main)) {
                jsonEditEditBuild(self)
            }
            self.init(options, callback)
            self.popup.enter()
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

private fun jsonEditShowDataOk(data: dynamic, callback: dynamic, ret: dynamic, popup: dynamic): dynamic {
    try {
        if (jeTruthy(callback)) {
            if (jeLooseEq(jeTypeof(data), "boolean")) {
                callback(js("Boolean")(ret.isChecked()))
            } else if (jeLooseEq(jeTypeof(data), "number")) {
                val t = jeNumberJs(ret.getText())
                if (jeIsFiniteJs(t)) {
                    callback(t)
                } else {
                    js("Common").toast(js("JSONEdit").intl.irregularNumber)
                }
            } else {
                callback(jeString(ret.getText()))
            }
        }
        popup.exit()
        return true
    } catch (e: dynamic) {
        js("erp")(e)
    }
    return js("undefined")
}

private fun jsonEditShowData(msg: dynamic, data: dynamic, callback: dynamic) {
    js("G").ui({
        try {
            val ctx = js("ctx")
            val scr = jeNewScrollView(ctx)
            js("function (v, s) { Common.applyStyle(v, s); }")(scr, "message_bg")

            val layout = jeNewLinearLayout(ctx)
            layout.setOrientation(js("G.LinearLayout.VERTICAL"))
            layout.setPadding(jeDp(15), jeDp(15), jeDp(15), 0)

            val title = jeNewTextView(ctx)
            title.setText(msg)
            title.setLayoutParams(jeNewLinearParamsJs(-2, -2))
            title.setPadding(0, 0, 0, jeDp(10))
            jeApplyStyle(title, "textview_default", 4)
            layout.addView(title)

            val ret: dynamic
            if (jeLooseEq(jeTypeof(data), "boolean")) {
                ret = jeNewCheckBox(ctx)
                ret.setLayoutParams(jeNewLinearParamsJs(-2, -2, 0))
                ret.getLayoutParams().setMargins(jeDp(10), jeDp(10), jeDp(10), jeDp(10))
                ret.setChecked(data)
                ret.setText(js("JSONEdit").intl.booleanCheckbox)
            } else {
                ret = jeNewEditText(ctx)
                ret.setText(js("Common").toString(data))
                ret.setSingleLine(false)
                ret.setMinWidth(jeHalfScreenWidthJs())
                ret.setLayoutParams(jeNewLinearParamsJs(-2, 0, 1.0))
                if (jeLooseEq(jeTypeof(data), "number")) {
                    ret.setInputType(
                        js("G.InputType.TYPE_CLASS_NUMBER | G.InputType.TYPE_NUMBER_FLAG_SIGNED | G.InputType.TYPE_NUMBER_FLAG_DECIMAL")
                    )
                }
                ret.setSelection(ret.length())
                jeApplyStyle(ret, "edittext_default", 2)
                js("Common").postIME(ret)
            }
            layout.addView(ret)

            val exit = jeNewTextView(ctx)
            exit.setLayoutParams(jeNewLinearParamsJs(-1, -2))
            exit.setText(js("Common").intl.ok)
            exit.setGravity(js("G.Gravity.CENTER"))
            exit.setPadding(jeDp(10), jeDp(20), jeDp(10), jeDp(20))
            jeApplyStyle(exit, "button_critical", 3)
            var popup: dynamic = js("undefined")
            exit.setOnClickListener(jeNewClickListenerJs({ _v: dynamic ->
                jsonEditShowDataOk(data, callback, ret, popup)
            }))
            layout.addView(exit)
            scr.addView(layout)

            popup = js("PopupPage").showDialog("jsonedit.DataEditor", scr, -2, -2)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

private fun jsonEditShowRawEdit(data: dynamic, callback: dynamic) {
    js("G").ui({
        try {
            jsonEditShowRawEditImpl(data, callback)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

private fun jsonEditShowRawEditImpl(data: dynamic, callback: dynamic) {
    var datastr: dynamic = js("undefined")
    try {
        datastr = if (jeTruthy(js("JSONEdit").showAll)) {
            js("MapScript").toSource(data)
        } else {
            jeJsonStringifyJs(data, js("null"), 4)
        }
    } catch (e: dynamic) {
        js("Log").e(e)
    }
    if (!jeTruthy(datastr)) {
        js("Common").toast(js("JSONEdit").intl.cannotStringify)
        return
    }

    val ctx = js("ctx")
    val layout = jeNewLinearLayout(ctx)
    layout.setLayoutParams(jeNewFrameParamsJs(-1, -1, js("G.Gravity.CENTER")))
    layout.setOrientation(js("G.LinearLayout.VERTICAL"))
    layout.setPadding(jeDp(15), jeDp(15), jeDp(15), 0)
    js("Common").applyStyle(layout, "message_bg")
    layout.setOnTouchListener(jeNewTouchListenerJs({ _v: dynamic, _e: dynamic -> true }))

    val ret = jeNewEditText(ctx)
    ret.setText(datastr)
    ret.setSingleLine(false)
    ret.setGravity(js("G.Gravity.LEFT | G.Gravity.TOP"))
    ret.setLayoutParams(jeNewLinearParamsJs(-1, 0, 1.0))
    jeApplyStyle(ret, "edittext_default", 2)
    layout.addView(ret)

    val exit = jeNewTextView(ctx)
    exit.setLayoutParams(jeNewLinearParamsJs(-1, -2))
    exit.setText(js("Common").intl.ok)
    exit.setGravity(js("G.Gravity.CENTER"))
    exit.setPadding(jeDp(10), jeDp(20), jeDp(10), jeDp(20))
    jeApplyStyle(exit, "button_critical", 3)

    var popup: dynamic = js("undefined")
    exit.setOnClickListener(jeNewClickListenerJs({ _v: dynamic ->
        try {
            if (jeTruthy(callback)) {
                try {
                    callback(jeJsonParseJs(ret.getText()))
                    popup.exit()
                } catch (e: dynamic) {
                    js("Common").toast(js("JSONEdit").intl.resolve("cannotParse", e))
                }
            }
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }))
    layout.addView(exit)
    js("Common").initEnterAnimation(layout)
    popup = jeNewPopupPage(layout, "jsonedit.BatchEdit")
    popup.enter()
}

private fun jsonEditNewItemClipboard(tag: dynamic): dynamic {
    if (!jeTruthy(js("JSONEdit").clipboard)) {
        js("Common").toast(js("JSONEdit").intl.emptyClipboard)
        return true
    }
    tag.callback(js("Object").copy(js("JSONEdit").clipboard.item))
    return js("undefined")
}

private fun jsonEditShowNewItem(self: dynamic, callback: dynamic) {
    if (!jeTruthy(self.menu)) {
        self.intl = js("Intl").getNamespace("jsonEdit.type")
        val menu = js("[]")

        val emptyObject = jeObject()
        emptyObject.text = self.intl.emptyObject
        emptyObject.description = self.intl.object_desc
        emptyObject.onclick = { _v: dynamic, tag: dynamic ->
            tag.callback(js("({})"))
            js("undefined")
        }
        menu.push(emptyObject)

        val emptyArray = jeObject()
        emptyArray.text = self.intl.emptyArray
        emptyArray.description = self.intl.array_desc
        emptyArray.onclick = { _v: dynamic, tag: dynamic ->
            tag.callback(js("[]"))
            js("undefined")
        }
        menu.push(emptyArray)

        val stringItem = jeObject()
        stringItem.text = self.intl.string
        stringItem.description = self.intl.string_desc
        stringItem.onclick = { _v: dynamic, tag: dynamic ->
            js("JSONEdit").showData(
                js("JSONEdit").intl.resolve("newValue", self.intl.string),
                "",
                { newValue: dynamic ->
                    tag.callback(newValue)
                    js("undefined")
                }
            )
            js("undefined")
        }
        menu.push(stringItem)

        val numberItem = jeObject()
        numberItem.text = self.intl.number
        numberItem.description = self.intl.number_desc
        numberItem.onclick = { _v: dynamic, tag: dynamic ->
            js("JSONEdit").showData(
                js("JSONEdit").intl.resolve("newValue", self.intl.number),
                0,
                { newValue: dynamic ->
                    tag.callback(newValue)
                    js("undefined")
                }
            )
            js("undefined")
        }
        menu.push(numberItem)

        val booleanItem = jeObject()
        booleanItem.text = self.intl.boolean
        booleanItem.description = self.intl.boolean_desc
        booleanItem.onclick = { _v: dynamic, tag: dynamic ->
            js("JSONEdit").showData(
                js("JSONEdit").intl.resolve("newValue", self.intl.boolean),
                true,
                { newValue: dynamic ->
                    tag.callback(newValue)
                    js("undefined")
                }
            )
            js("undefined")
        }
        menu.push(booleanItem)

        val nullItem = jeObject()
        nullItem.text = self.intl["null"]
        nullItem.description = self.intl.null_desc
        nullItem.onclick = { _v: dynamic, tag: dynamic ->
            tag.callback(js("null"))
            js("undefined")
        }
        menu.push(nullItem)

        val gap = jeObject()
        gap.gap = jeGapJs(10)
        menu.push(gap)

        val clipboard = jeObject()
        clipboard.text = self.intl.clipboard
        clipboard.description = self.intl.clipboard_desc
        clipboard.onclick = { _v: dynamic, tag: dynamic -> jsonEditNewItemClipboard(tag) }
        menu.push(clipboard)

        val raw = jeObject()
        raw.text = self.intl.raw
        raw.description = self.intl.raw_desc
        raw.onclick = { _v: dynamic, tag: dynamic ->
            val dialog = jeObject()
            dialog.title = self.intl.manualInput_desc
            dialog.callback = { s: dynamic ->
                try {
                    tag.callback(jeJsonParseJs(s))
                } catch (e: dynamic) {
                    js("Common").toast(js("JSONEdit").intl.resolve("cannotParse", e))
                }
                js("undefined")
            }
            js("Common").showInputDialog(dialog)
            js("undefined")
        }
        menu.push(raw)

        self.menu = menu
    }

    val tag = jeObject()
    tag.callback = callback
    js("Common").showOperateDialog(self.menu, tag)
}

private fun jsonEditTraceGlobal(self: dynamic) {
    val options = jeObject()
    options.source = js("function () { return eval.call(null, 'this'); }")()
    options.rootname = "Global object"
    options.showAll = true
    self.show(options)
}

private fun jsonEditTrace(self: dynamic, obj: dynamic) {
    val options = jeObject()
    options.source = obj
    options.rootname = "Trace"
    options.showAll = true
    self.show(options)
}

fun installJSONEdit() {
    jsonEditApi.clipboard = js("null")
    jsonEditApi.itemAccessor = jsonEditBuildItemAccessors()

    jsonEditApi.onCreate = jeThisJs { self: dynamic, _args: dynamic ->
        jsonEditOnCreate(self)
    }
    jsonEditApi.create = { callback: dynamic, rootname: dynamic ->
        jsonEditCreate(jsonEditApi, callback, rootname)
        js("undefined")
    }
    jsonEditApi.main = {
        jsonEditMain(jsonEditMainState)
        js("undefined")
    }
    jsonEditApi.show = { o: dynamic -> jsonEditShow(jsonEditApi, o) }
    jsonEditApi.showEdit = { options: dynamic, callback: dynamic ->
        jsonEditShowEdit(jsonEditEditState, options, callback)
        js("undefined")
    }
    jsonEditApi.showData = { msg: dynamic, data: dynamic, callback: dynamic ->
        jsonEditShowData(msg, data, callback)
        js("undefined")
    }
    jsonEditApi.showRawEdit = { data: dynamic, callback: dynamic ->
        jsonEditShowRawEdit(data, callback)
        js("undefined")
    }
    jsonEditApi.showNewItem = { callback: dynamic ->
        jsonEditShowNewItem(jsonEditNewItemState, callback)
        js("undefined")
    }
    jsonEditApi.traceGlobal = {
        jsonEditTraceGlobal(jsonEditApi)
        js("undefined")
    }
    jsonEditApi.trace = { obj: dynamic ->
        jsonEditTrace(jsonEditApi, obj)
        js("undefined")
    }

    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("JSONEdit", jsonEditApi)
    }
}
