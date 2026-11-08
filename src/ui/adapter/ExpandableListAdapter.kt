package ca.kotlin.ui.adapter

import ca.kotlin.core.*


/**
 * modules/listAdapter/ExpandableListAdapter.js 的 Kotlin/JS 版本。
 *
 * 保持原有 API：
 *   new ExpandableListAdapter(arr, getChildren, itemMaker, itemBinder,
 *                             groupMaker, groupBinder, params) -> G.ListAdapter
 *   ExpandableListAdapter.Controller(...)
 *   ExpandableListAdapter.control(adapter)
 *
 * 原实现是工厂函数 + Controller 原型，原型方法全部依赖动态 this；这里
 * 用 makeCtor / this / voidThis 薄 wrapper 保留 this 与 arguments 语义。
 * 节点树扩展、折叠、搜索等逻辑仍在 Kotlin 侧，节点的 ext/pos 状态
 * 继续保存在原样的 JS 对象上。
 */

private const val EXP_DEPTH_LIMIT = 40

// ---------------- JS 语义 helper ----------------
private val expTruthyJs: dynamic = js("function (v) { return !!v; }")
private val expLengthJs: dynamic = js("function (v) { return v.length | 0; }")
private val expParseIntJs: dynamic = js("function (v) { return parseInt(v); }")
private val expIsArrayJs: dynamic = js("function (v) { return Array.isArray(v); }")
private val expIsNaNNs: dynamic = js("function (v) { return isNaN(v); }")
private val expInJs: dynamic = js("function (k, o) { return k in o; }")
private val expForInJs: dynamic = js("function (o, f) { for (var i in o) f(i); }")
private val expLooseEqJs: dynamic = js("function (a, b) { return a == b; }")
private val expNaNJs: dynamic = js("NaN")
private val expNewListAdapterJs: dynamic = js("function (a) { return new G.ListAdapter(a); }")
private val expNewOnItemClickListenerJs: dynamic = js(
    "function (o) { return new G.AdapterView.OnItemClickListener(o); }"
)
private val expNewOnItemLongClickListenerJs: dynamic = js(
    "function (o) { return new G.AdapterView.OnItemLongClickListener(o); }"
)
private val expSpliceApplyJs: dynamic = js(
    "function (a, z) { return Array.prototype.splice.apply(a, z); }"
)
private val expArray5Js: dynamic = js("function (a, b, c, d, e) { return [a, b, c, d, e]; }")

// 原实现 extendNodeTree / findNodePos 引用了未定义的 data / extend；保留
// 这两个 helper，让运行时仍然抛出与原来一致的 ReferenceError。
private val expDataRefJs: dynamic = js("function () { return data; }")
private val expFindNodePosJs: dynamic = js(
    "function (arr, node) {" +
        " for (var i in arr) { if (extend[i].data == node) return i; }" +
        " return -1;" +
        "}"
)

private val expThisJs: dynamic = js(
    "function (f) { return function () { return f(this, arguments); }; }"
)
private val expVoidThisJs: dynamic = js(
    "function (f) { return function () { f(this, arguments); }; }"
)
private val expMakeCtorJs: dynamic = js(
    "function (init) {" +
        " return function (root, extend, getChildren, groupholders, itemholders, itemMaker," +
        " groupMaker, itemBinder, groupBinder, dso, idHolder, params) {" +
        "  init(this, root, extend, getChildren, groupholders, itemholders, itemMaker," +
        " groupMaker, itemBinder, groupBinder, dso, idHolder, params);" +
        " };" +
        "}"
)
private val expMakeFactoryJs: dynamic = js(
    "function (impl) {" +
        " return function (arr, getChildren, itemMaker, itemBinder, groupMaker, groupBinder, params) {" +
        "  return impl(arr, getChildren, itemMaker, itemBinder, groupMaker, groupBinder, params);" +
        " };" +
        "}"
)
private val expNewControllerJs: dynamic = js(
    "function (ctor, root, extend, getChildren, groupholders, itemholders, itemMaker, groupMaker," +
        " itemBinder, groupBinder, dso, idHolder, params) {" +
        " return new ctor(root, extend, getChildren, groupholders, itemholders, itemMaker, groupMaker," +
        " itemBinder, groupBinder, dso, idHolder, params);" +
        "}"
)

private fun expTruthy(v: dynamic): Boolean = expTruthyJs(v)
private fun expLength(v: dynamic): Int = expLengthJs(v)
private fun expParseInt(v: dynamic): dynamic = expParseIntJs(v)
private fun expIsArray(v: dynamic): Boolean = expIsArrayJs(v)
private fun expIsNaN(v: dynamic): Boolean = expIsNaNNs(v)
private fun expIn(key: dynamic, obj: dynamic): Boolean = expInJs(key, obj)
private fun expLooseEq(a: dynamic, b: dynamic): Boolean = expLooseEqJs(a, b)
private fun expForIn(obj: dynamic, f: dynamic) {
    expForInJs(obj, f)
}

// ---------------- 节点树辅助函数 ----------------

private fun expBuildExtendData(
    e: dynamic,
    i: dynamic,
    parent: dynamic,
    children: dynamic,
    idHolder: dynamic,
    newDepth: dynamic
): dynamic {
    val out = js("({})")
    out.data = e
    out.id = idHolder[0]
    idHolder[0] = idHolder[0] + 1
    out.group = expIsArray(children)
    out.children = children
    out.extend = null
    out.expanded = false
    out.children_expanded = 0
    out.parent = parent
    out.index = i
    out.depth = newDepth
    return out
}

private fun expExtendArray(
    item: dynamic,
    getChildren: dynamic,
    idHolder: dynamic,
    params: dynamic
): dynamic {
    if (!expTruthy(item.group)) return js("undefined")
    val newDepth = item.depth + 1
    if (newDepth > EXP_DEPTH_LIMIT) {
        val empty = js("[]")
        item.extend = empty
        item.children = empty
        return empty
    }
    val source = item.children
    val out = js("[]")
    var i = 0
    val n = expLength(source)
    while (i < n) {
        out.push(
            expBuildExtendData(
                source[i],
                i,
                item,
                getChildren(source[i], i, source, newDepth, params),
                idHolder,
                newDepth
            )
        )
        i++
    }
    item.extend = out
    return out
}

private fun expMakePointer(controller: dynamic) {
    val a = controller.extend
    var i = 0
    val n = expLength(a)
    while (i < n) {
        a[i].pos = i
        i++
    }
}

private fun expExtendNodeTree(controller: dynamic, node: dynamic) {
    if (!expTruthy(node.extend)) {
        expExtendArray(expDataRefJs(), controller._getChildren, controller.idHolder, controller.params)
    }
    node.extend.forEach({ e: dynamic ->
        if (expTruthy(e.group)) expExtendNodeTree(controller, e)
    })
}

private fun expExpandNode(
    controller: dynamic,
    cursor: dynamic,
    data: dynamic,
    mode: dynamic
) {
    val a = controller.extend
    var m = mode
    if (!expTruthy(data.extend)) {
        expExtendArray(data, controller._getChildren, controller.idHolder, controller.params)
    }
    if (expLooseEq(m, 0)) m = data.children_expanded
    expForIn(data.extend, { i: dynamic ->
        val e = data.extend[i]
        if (expIsNaN(e.pos)) a.splice(cursor[0], 0, e)
        cursor[0] = cursor[0] + 1
        if (expTruthy(e.group)) {
            if (m > 0) e.expanded = expLooseEq(m, 1)
            e.children_expanded = data.children_expanded
            if (expTruthy(e.expanded)) expExpandNode(controller, cursor, e, m)
        }
    })
    data.expanded = true
}

private fun expCollapseNode(
    controller: dynamic,
    delpos: dynamic,
    data: dynamic,
    recursive: dynamic
) {
    expForIn(data.extend, { i: dynamic ->
        val e = data.extend[i]
        delpos.push(e.pos)
        if (expTruthy(e.expanded)) expCollapseNode(controller, delpos, e, recursive)
    })
    if (expTruthy(recursive)) data.children_expanded = 2
}

private fun expGetLastNodePos(node: dynamic): dynamic {
    if (!expTruthy(node.extend) || expLength(node.extend) == 0) return node.pos
    return expGetLastNodePos(node.extend[expLength(node.extend) - 1])
}

private fun expUpdateNode(
    controller: dynamic,
    data: dynamic,
    target: dynamic,
    isRoot: dynamic,
    silent: dynamic
): dynamic {
    var newExtend: dynamic = js("undefined")
    val newArr = if (expTruthy(isRoot)) {
        data.data
    } else {
        controller._getChildren(
            data.data, data.index, data.parent.children, data.depth, controller.params
        )
    }
    data.group = expIsArray(newArr)
    if (!expTruthy(data.group)) {
        data.expanded = false
        data.children_expanded = 0
    }
    var isSilent = silent
    if (!expTruthy(data.expanded)) isSilent = true
    if (expTruthy(data.group) && expTruthy(data.extend)) {
        newExtend = js("[]")
        val newDepth = data.depth + 1
        if (data.depth <= EXP_DEPTH_LIMIT) {
            var i = 0
            val n = expLength(newArr)
            while (i < n) {
                var itemIndex: dynamic = -1
                var j = 0
                val m = expLength(data.extend)
                while (j < m) {
                    if (expLooseEq(newArr[i], data.extend[j].data)) {
                        itemIndex = j
                        break
                    }
                    j++
                }
                val e: dynamic
                if (itemIndex < 0) {
                    e = expBuildExtendData(
                        newArr[i],
                        i,
                        data,
                        controller._getChildren(newArr[i], i, newArr, newDepth, controller.params),
                        controller.idHolder,
                        newDepth
                    )
                } else {
                    e = data.extend[itemIndex]
                    data.extend.splice(itemIndex, 1)
                }
                e.index = i
                newExtend.push(e)
                if (!expTruthy(isSilent)) target.push(e)
                expUpdateNode(controller, e, target, false, isSilent)
                i++
            }
        }
    }
    data.children = newArr
    data.extend = newExtend
    return target
}

private fun expSearchNode(
    controller: dynamic,
    data: dynamic,
    root: dynamic,
    arr: dynamic
): Boolean {
    val i = root.children.indexOf(data)
    if (i >= 0) {
        arr.push(i)
        return true
    }
    if (!expTruthy(root.extend)) {
        expExtendArray(root, controller._getChildren, controller.idHolder, controller.params)
    }
    var j = 0
    val n = expLength(root.extend)
    while (j < n) {
        if (expTruthy(root.extend[j].group)) {
            if (expSearchNode(controller, data, root.extend[j], arr)) {
                arr.push(j)
                return true
            }
        }
        j++
    }
    return false
}

// ---------------- Controller 方法 ----------------

private fun expInitController(
    self: dynamic,
    root: dynamic,
    extend: dynamic,
    getChildren: dynamic,
    groupholders: dynamic,
    itemholders: dynamic,
    itemMaker: dynamic,
    groupMaker: dynamic,
    itemBinder: dynamic,
    groupBinder: dynamic,
    dso: dynamic,
    idHolder: dynamic,
    params: dynamic
) {
    self.root = root
    self.extend = extend
    self._getChildren = getChildren
    self.groupholders = groupholders
    self.itemholders = itemholders
    self.itemMaker = itemMaker
    self.groupMaker = groupMaker
    self.itemBinder = itemBinder
    self.groupBinder = groupBinder
    self.dso = dso
    self.idHolder = idHolder
    self.params = params
}

private fun expBindListView(self: dynamic, lv: dynamic, o: dynamic) {
    val adpt = self
    lv.setAdapter(adpt.self)

    val click = js("({})")
    click.onItemClick = { parent: dynamic, view: dynamic, pos: dynamic, id: dynamic ->
        try {
            val hn = parent.getHeaderViewsCount()
            val fn = parent.getFooterViewsCount()
            val c = parent.getCount()
            if (pos < hn) {
                if (expTruthy(o.onHeaderClick)) o.onHeaderClick(pos)
            } else if (pos >= c - fn) {
                if (expTruthy(o.onFooterClick)) o.onFooterClick(pos - c + fn)
            } else {
                val p = pos - hn
                val e = adpt.extend[p]
                val args = expArray5Js(e.data, p, parent, view, adpt)
                if (expTruthy(e.group)) {
                    if (expTruthy(e.expanded)) {
                        adpt.collapse(p)
                        if (expTruthy(o.onGroupCollapse)) o.onGroupCollapse.apply(o, args)
                    } else {
                        adpt.expand(p)
                        if (expTruthy(o.onGroupExpand)) o.onGroupExpand.apply(o, args)
                    }
                    if (expTruthy(o.onGroupClick)) o.onGroupClick.apply(o, args)
                } else {
                    if (expTruthy(o.onItemClick)) o.onItemClick.apply(o, args)
                }
                if (expTruthy(o.onClick)) o.onClick.apply(o, args)
            }
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }
    lv.setOnItemClickListener(expNewOnItemClickListenerJs(click))

    if (expTruthy(o.onLongClick) || expTruthy(o.onGroupLongClick) ||
        expTruthy(o.onItemLongClick) || expTruthy(o.onHeaderLongClick) ||
        expTruthy(o.onFooterLongClick)
    ) {
        val longClick = js("({})")
        longClick.onItemLongClick = {
                parent: dynamic, view: dynamic, pos: dynamic, id: dynamic ->
            try {
                val hn = parent.getHeaderViewsCount()
                val fn = parent.getFooterViewsCount()
                val c = parent.getCount()
                if (pos < hn) {
                    if (expTruthy(o.onHeaderLongClick)) o.onHeaderLongClick(pos)
                } else if (pos >= c - fn) {
                    if (expTruthy(o.onFooterLongClick)) o.onFooterLongClick(pos - c + fn)
                } else {
                    val p = pos - hn
                    val e = adpt.extend[p]
                    val args = expArray5Js(e.data, p, parent, view, adpt)
                    if (expTruthy(e.group)) {
                        if (expTruthy(o.onGroupLongClick)) o.onGroupLongClick.apply(o, args)
                    } else {
                        if (expTruthy(o.onItemLongClick)) o.onItemLongClick.apply(o, args)
                    }
                    if (expTruthy(o.onLongClick)) o.onLongClick.apply(o, args)
                }
                true
            } catch (e: dynamic) {
                js("erp")(e)
                true
            }
        }
        lv.setOnItemLongClickListener(expNewOnItemLongClickListenerJs(longClick))
    }
}

private fun expCollapse(self: dynamic, pos: dynamic, recursive: dynamic) {
    val e = self.extend[pos]
    if (!expTruthy(e.expanded)) return
    val delpos = js("[]")
    expCollapseNode(self, delpos, e, recursive)
    var i = expLength(delpos) - 1
    while (i >= 0) {
        self.extend[delpos[i]].pos = expNaNJs
        self.extend.splice(delpos[i], 1)
        i--
    }
    e.expanded = false
    expMakePointer(self)
    self.notifyChange()
}

private fun expCollapseAll(self: dynamic) {
    val a = self.root.extend
    val delpos = js("[]")
    var i = 0
    val n = expLength(a)
    while (i < n) {
        val e = a[i]
        if (expTruthy(e.expanded)) {
            expCollapseNode(self, delpos, e, true)
            e.expanded = false
        } else if (expTruthy(e.group)) {
            e.children_expanded = 2
        }
        i++
    }
    i = expLength(delpos) - 1
    while (i >= 0) {
        self.extend[delpos[i]].pos = expNaNJs
        self.extend.splice(delpos[i], 1)
        i--
    }
    expMakePointer(self)
    self.notifyChange()
}

private fun expClearHolder(self: dynamic) {
    // 原实现引用 this.holders，但 Controller 上只有 groupholders/itemholders；
    // 这里保持同样的（有缺陷的）行为。
    expForIn(self.holders, { i: dynamic -> self.holders[i].self.setTag("") })
    self.holders.length = 0
    self.notifyChange()
}

private fun expExtendAll(self: dynamic) {
    expExtendNodeTree(self, self.root)
}

private fun expExpand(self: dynamic, pos: dynamic, recursive: dynamic) {
    val e = self.extend[pos]
    if (!expTruthy(e.group) || expTruthy(e.expanded)) return
    val cursor = js("[]")
    cursor.push(pos + 1)
    expExpandNode(self, cursor, e, if (expTruthy(recursive)) 1 else 0)
    expMakePointer(self)
    self.notifyChange()
}

private fun expExpandAll(self: dynamic) {
    val cursor = js("[]")
    cursor.push(0)
    expExpandNode(self, cursor, self.root, 1)
    expMakePointer(self)
    self.notifyChange()
}

private fun expFindNodePath(self: dynamic, node: dynamic): dynamic {
    val r = js("[]")
    if (expSearchNode(self, node, self.root, r)) {
        r.reverse()
        return r
    }
    return null
}

private fun expFindNodePos(self: dynamic, node: dynamic): dynamic =
    expFindNodePosJs(self.extend, node)

private fun expGetChildren(self: dynamic, pos: dynamic): dynamic = self.extend[pos].children

private fun expGetDepth(self: dynamic, pos: dynamic): dynamic = self.extend[pos].depth

private fun expGetGroupIndex(self: dynamic, pos: dynamic): dynamic = self.extend[pos].index

private fun expGetHolder(self: dynamic, view: dynamic): dynamic = self.holders[view.getTag()]

private fun expGetVisibleChildren(self: dynamic, pos: dynamic, arr: dynamic): dynamic {
    var out = arr
    if (!expTruthy(out)) out = js("[]")
    val e = self.extend[pos]
    if (!expTruthy(e.extend)) return out
    expForIn(e.extend, { i: dynamic ->
        if (!expIsNaN(e.extend[i].pos)) out.push(e.extend[i].pos)
    })
    return js("undefined")
}

private fun expGetItem(self: dynamic, pos: dynamic): dynamic = self.extend[pos].data

private fun expGetParent(self: dynamic, pos: dynamic): dynamic = self.extend[pos].parent.pos

private fun expGetSiblings(self: dynamic, pos: dynamic, arr: dynamic): dynamic {
    var out = arr
    if (!expTruthy(out)) out = js("[]")
    val e = self.extend[pos].parent
    expForIn(e.extend, { i: dynamic -> out.push(e.extend[i].pos) })
    return js("undefined")
}

private fun expGetTree(self: dynamic, pos: dynamic, arr: dynamic): dynamic {
    var out = arr
    if (!expTruthy(out)) out = js("[]")
    val e = self.extend[pos]
    out.push(pos)
    if (!expTruthy(e.extend) || !expTruthy(e.expanded)) return out
    expForIn(e.extend, { i: dynamic -> self.getTree(e.extend[i].pos, out) })
    return out
}

private fun expGetPath(self: dynamic, pos: dynamic, arr: dynamic): dynamic {
    var out = arr
    if (!expTruthy(out)) out = js("[]")
    if (expIsNaN(pos)) return out
    self.getPath(self.getParent(pos), out)
    out.push(self.extend[pos].index)
    return out
}

private fun expGetPosition(self: dynamic, path: dynamic): dynamic {
    var s = self.root
    var i = 0
    val n = expLength(path)
    while (i < n) {
        if (!expTruthy(s.extend)) return expNaNJs
        s = s.extend[path[i]]
        if (!expTruthy(s)) return expNaNJs
        i++
    }
    return s.pos
}

private fun expHasChildren(self: dynamic, pos: dynamic): Boolean {
    if (!expTruthy(self.isGroup(pos))) return false
    return expLength(self.extend[pos].children) > 0
}

private fun expIsCollapsed(self: dynamic, pos: dynamic): Boolean =
    !expTruthy(self.extend[pos].expanded)

private fun expIsExpanded(self: dynamic, pos: dynamic): dynamic = self.extend[pos].expanded

private fun expIsGroup(self: dynamic, pos: dynamic): dynamic = self.extend[pos].group

private fun expIsVisible(self: dynamic, node: dynamic): dynamic = self.findNode(node) >= 0

private fun expNotifyChange(self: dynamic) {
    self.dso.forEach({ e: dynamic ->
        if (expTruthy(e)) e.onChanged()
    })
}

private fun expNotifyInvalidate(self: dynamic) {
    self.dso.forEach({ e: dynamic ->
        if (expTruthy(e)) e.onInvalidated()
    })
}

private fun expRebind(self: dynamic, pos: dynamic) {
    expForIn(self.groupholders, { i: dynamic ->
        if (expLooseEq(self.groupholders[i].pos, pos)) {
            val e = self.extend[pos]
            self.groupBinder(
                self.groupholders[i], e.data, e.index, e.parent.children, e.depth, self.params
            )
        }
    })
    expForIn(self.itemholders, { i: dynamic ->
        if (expLooseEq(self.itemholders[i].pos, pos)) {
            val e = self.extend[pos]
            self.itemBinder(
                self.itemholders[i], e.data, e.index, e.parent.children, e.depth, self.params
            )
        }
    })
}

private fun expReveal(self: dynamic, path: dynamic): dynamic {
    var s = self.root
    var i = 0
    val n = expLength(path)
    while (i < n) {
        if (!expTruthy(s.extend)) return expNaNJs
        s = s.extend[path[i]]
        if (!expTruthy(s)) return expNaNJs
        if (expTruthy(s.group) && !expTruthy(s.expanded) && i < n - 1) self.expand(s.pos)
        i++
    }
    return s.pos
}

private fun expRevealNode(self: dynamic, node: dynamic): dynamic {
    val a = self.findNodePath(node)
    if (expTruthy(a)) return self.reveal(a)
    return expNaNJs
}

private fun expSetArray(self: dynamic, a: dynamic) {
    self.extend.length = 0
    self.root.data = a
    self.root.children = a
    val u = expExtendArray(self.root, self._getChildren, self.idHolder, self.params)
    expForIn(u, { i: dynamic -> self.extend.push(u[i]) })
    expMakePointer(self)
    self.notifyChange()
}

private fun expUpdate(self: dynamic, pos: dynamic) {
    val target = js("[]")
    target.push(pos + 1)
    target.push(expGetLastNodePos(self.extend[pos]) - pos)
    expUpdateNode(self, self.extend[pos], target, false, false)
    expSpliceApplyJs(self.extend, target)
    expMakePointer(self)
    self.notifyChange()
}

private fun expUpdateAll(self: dynamic) {
    val target = js("[]")
    target.push(0)
    target.push(expLength(self.extend))
    expUpdateNode(self, self.root, target, true, false)
    expSpliceApplyJs(self.extend, target)
    expMakePointer(self)
    self.notifyChange()
}

// ---------------- 构造函数 / 工厂 ----------------

private fun expCreate(
    arr: dynamic,
    getChildren: dynamic,
    itemMaker: dynamic,
    itemBinder: dynamic,
    groupMaker: dynamic,
    groupBinder: dynamic,
    params: dynamic
): dynamic {
    val idHolder = js("[]")
    idHolder.push(0)

    val root = js("({})")
    root.data = arr
    root.id = idHolder[0]
    idHolder[0] = idHolder[0] + 1
    root.group = true
    root.children = arr
    root.extend = null
    root.expanded = true
    root.depth = -1
    root.pos = expNaNJs

    val fullExtend = expExtendArray(root, getChildren, idHolder, params)
    val extend = fullExtend.slice()
    val itemholders = js("[]")
    val groupholders = js("[]")
    val dso = js("[]")
    val controller = expNewControllerJs(
        expControllerCtor,
        root,
        extend,
        getChildren,
        groupholders,
        itemholders,
        itemMaker,
        groupMaker,
        itemBinder,
        groupBinder,
        dso,
        idHolder,
        params
    )
    expMakePointer(controller)

    val opts = js("({})")
    opts.array = extend
    opts.controller = controller
    opts.dso = dso
    opts.getItem = { pos: dynamic ->
        if (expLooseEq(pos, -1)) controller else extend[pos].data
    }
    opts.getItemId = { pos: dynamic -> extend[pos].id }
    opts.getItemViewType = { pos: dynamic -> if (expTruthy(extend[pos].group)) 1 else 0 }
    opts.getViewTypeCount = { 2 }
    opts.hasStableIds = { true }

    val adapter = js("ListAdapterBase").create(opts)
    adapter.getView = { pos: dynamic, convert: dynamic, parent: dynamic ->
        try {
            val e = extend[pos]
            var view = convert
            if (expTruthy(e.group)) {
                if (!expTruthy(view) || !expIn(view.getTag(), groupholders)) {
                    val holder = js("({})")
                    view = groupMaker(holder, params)
                    holder.self = view
                    view.setTag(expLength(groupholders).toString())
                    groupholders.push(holder)
                }
                val holder = groupholders[view.getTag()]
                holder.pos = expParseInt(pos)
                groupBinder(
                    holder, e.data, e.index, e.parent.children, e.depth, e.expanded, params
                )
            } else {
                if (!expTruthy(view) || !expIn(view.getTag(), itemholders)) {
                    val holder = js("({})")
                    view = itemMaker(holder, params)
                    holder.self = view
                    view.setTag(expLength(itemholders).toString())
                    itemholders.push(holder)
                }
                val holder = itemholders[view.getTag()]
                holder.pos = expParseInt(pos)
                itemBinder(holder, e.data, e.index, e.parent.children, e.depth, params)
            }
            view
        } catch (e: dynamic) {
            js("ListAdapterBase").errorView(e)
        }
    }
    return expNewListAdapterJs(adapter)
}

private fun expMakeProto(): dynamic {
    val p = js("({})")

    p.bindListView = expVoidThisJs { self: dynamic, args: dynamic ->
        expBindListView(self, args[0], args[1])
    }
    p.collapse = expVoidThisJs { self: dynamic, args: dynamic ->
        expCollapse(self, args[0], args[1])
    }
    p.collapseAll = expVoidThisJs { self: dynamic, args: dynamic -> expCollapseAll(self) }
    p.collapseTree = expVoidThisJs { self: dynamic, args: dynamic ->
        self.collapse(args[0], true)
    }
    p.clearHolder = expVoidThisJs { self: dynamic, args: dynamic -> expClearHolder(self) }
    p.extendAll = expVoidThisJs { self: dynamic, args: dynamic -> expExtendAll(self) }
    p.expand = expVoidThisJs { self: dynamic, args: dynamic ->
        expExpand(self, args[0], args[1])
    }
    p.expandAll = expVoidThisJs { self: dynamic, args: dynamic -> expExpandAll(self) }
    p.expandTree = expVoidThisJs { self: dynamic, args: dynamic ->
        self.expand(args[0], true)
    }
    p.findNodePath = expThisJs { self: dynamic, args: dynamic ->
        expFindNodePath(self, args[0])
    }
    p.findNodePos = expThisJs { self: dynamic, args: dynamic ->
        expFindNodePos(self, args[0])
    }
    p.getChildren = expThisJs { self: dynamic, args: dynamic ->
        expGetChildren(self, args[0])
    }
    p.getDepth = expThisJs { self: dynamic, args: dynamic -> expGetDepth(self, args[0]) }
    p.getGroupIndex = expThisJs { self: dynamic, args: dynamic -> expGetGroupIndex(self, args[0]) }
    p.getHolder = expThisJs { self: dynamic, args: dynamic -> expGetHolder(self, args[0]) }
    p.getVisibleChildren = expThisJs { self: dynamic, args: dynamic ->
        expGetVisibleChildren(self, args[0], args[1])
    }
    p.getItem = expThisJs { self: dynamic, args: dynamic -> expGetItem(self, args[0]) }
    p.getParent = expThisJs { self: dynamic, args: dynamic -> expGetParent(self, args[0]) }
    p.getSiblings = expThisJs { self: dynamic, args: dynamic ->
        expGetSiblings(self, args[0], args[1])
    }
    p.getTree = expThisJs { self: dynamic, args: dynamic -> expGetTree(self, args[0], args[1]) }
    p.getPath = expThisJs { self: dynamic, args: dynamic -> expGetPath(self, args[0], args[1]) }
    p.getPosition = expThisJs { self: dynamic, args: dynamic -> expGetPosition(self, args[0]) }
    p.hasChildren = expThisJs { self: dynamic, args: dynamic -> expHasChildren(self, args[0]) }
    p.isCollapsed = expThisJs { self: dynamic, args: dynamic -> expIsCollapsed(self, args[0]) }
    p.isExpanded = expThisJs { self: dynamic, args: dynamic -> expIsExpanded(self, args[0]) }
    p.isGroup = expThisJs { self: dynamic, args: dynamic -> expIsGroup(self, args[0]) }
    p.isVisible = expThisJs { self: dynamic, args: dynamic -> expIsVisible(self, args[0]) }
    p.notifyChange = expVoidThisJs { self: dynamic, args: dynamic -> expNotifyChange(self) }
    p.notifyInvalidate = expVoidThisJs { self: dynamic, args: dynamic ->
        expNotifyInvalidate(self)
    }
    p.rebind = expVoidThisJs { self: dynamic, args: dynamic -> expRebind(self, args[0]) }
    p.reveal = expThisJs { self: dynamic, args: dynamic -> expReveal(self, args[0]) }
    p.revealNode = expThisJs { self: dynamic, args: dynamic -> expRevealNode(self, args[0]) }
    p.setArray = expVoidThisJs { self: dynamic, args: dynamic -> expSetArray(self, args[0]) }
    p.update = expVoidThisJs { self: dynamic, args: dynamic -> expUpdate(self, args[0]) }
    p.updateAll = expVoidThisJs { self: dynamic, args: dynamic -> expUpdateAll(self) }

    return p
}

private val expControllerCtor: dynamic = expMakeCtorJs(
    { self: dynamic, root: dynamic, extend: dynamic, getChildren: dynamic, groupholders: dynamic,
      itemholders: dynamic, itemMaker: dynamic, groupMaker: dynamic, itemBinder: dynamic,
      groupBinder: dynamic, dso: dynamic, idHolder: dynamic, params: dynamic ->
        expInitController(
            self, root, extend, getChildren, groupholders, itemholders, itemMaker, groupMaker,
            itemBinder, groupBinder, dso, idHolder, params
        )
    }
)

private val expFactory: dynamic = expMakeFactoryJs(
    { arr: dynamic, getChildren: dynamic, itemMaker: dynamic, itemBinder: dynamic,
      groupMaker: dynamic, groupBinder: dynamic, params: dynamic ->
        expCreate(arr, getChildren, itemMaker, itemBinder, groupMaker, groupBinder, params)
    }
)

private fun expControl(adapter: dynamic): dynamic {
    val r = adapter.getItem(-1)
    r.self = adapter
    return r
}

fun installExpandableListAdapter() {
    expControllerCtor.prototype = expMakeProto()
    expFactory.Controller = expControllerCtor
    expFactory.control = { adapter: dynamic -> expControl(adapter) }

    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("ExpandableListAdapter", expFactory)
    }
}
