package ca.kotlin.ui.adapter

import ca.kotlin.core.*


/**
 * modules/listAdapter/MultipleListAdapter.js 的 Kotlin/JS 版本。
 *
 * 保持原有 API：
 *   new MultipleListAdapter(arr, types, getType, params) -> G.ListAdapter
 *   MultipleListAdapter.Controller(...)
 *   MultipleListAdapter.getController(adapter)
 *
 * types 是“类型名 -> { maker, binder }”的普通对象；holders 是按类型名
 * 分组的 holder 数组。类型枚举顺序、动态 this 与回调参数都按原实现保留。
 */

// ---------------- JS 语义 helper ----------------
private val mlTruthyJs: dynamic = js("function (v) { return !!v; }")
private val mlStringJs: dynamic = js("function (v) { return String(v); }")
private val mlLengthJs: dynamic = js("function (v) { return v.length | 0; }")
private val mlParseIntJs: dynamic = js("function (v) { return parseInt(v); }")
private val mlInJs: dynamic = js("function (k, o) { return k in o; }")
private val mlForInJs: dynamic = js("function (o, f) { for (var i in o) f(i); }")
private val mlLooseNeJs: dynamic = js("function (a, b) { return a != b; }")
private val mlNewListAdapterJs: dynamic = js("function (a) { return new G.ListAdapter(a); }")

private val mlThisJs: dynamic = js(
    "function (f) { return function () { return f(this, arguments); }; }"
)
private val mlVoidThisJs: dynamic = js(
    "function (f) { return function () { f(this, arguments); }; }"
)
private val mlMakeCtorJs: dynamic = js(
    "function (init) {" +
        " return function (array, holders, dso, types, vtypes, getType, params) {" +
        "  init(this, array, holders, dso, types, vtypes, getType, params);" +
        " };" +
        "}"
)
private val mlMakeFactoryJs: dynamic = js(
    "function (impl) {" +
        " return function (arr, types, getType, params) {" +
        "  return impl(arr, types, getType, params);" +
        " };" +
        "}"
)
private val mlNewControllerJs: dynamic = js(
    "function (ctor, array, holders, dso, types, vtypes, getType, params) {" +
        " return new ctor(array, holders, dso, types, vtypes, getType, params);" +
        "}"
)

private fun mlTruthy(v: dynamic): Boolean = mlTruthyJs(v)
private fun mlString(v: dynamic): dynamic = mlStringJs(v)
private fun mlLength(v: dynamic): Int = mlLengthJs(v)
private fun mlParseInt(v: dynamic): dynamic = mlParseIntJs(v)
private fun mlIn(key: dynamic, obj: dynamic): Boolean = mlInJs(key, obj)
private fun mlLooseNe(a: dynamic, b: dynamic): Boolean = mlLooseNeJs(a, b)
private fun mlForIn(obj: dynamic, f: dynamic) {
    mlForInJs(obj, f)
}

// ---------------- Controller 方法 ----------------

private fun mlInitController(
    self: dynamic,
    array: dynamic,
    holders: dynamic,
    dso: dynamic,
    types: dynamic,
    vtypes: dynamic,
    getType: dynamic,
    params: dynamic
) {
    self.array = array
    self.holders = holders
    self.dso = dso
    self.types = types
    self.vtypes = vtypes
    self.getType = getType
    self.params = params
}

private fun mlClearHolder(self: dynamic) {
    mlForIn(self.holders, { i: dynamic ->
        val e = self.holders[i]
        mlForIn(e, { j: dynamic -> e[j].self.setTag("") })
        e.length = 0
    })
    self.notifyChange()
}

private fun mlGetHolder(self: dynamic, pos: dynamic, view: dynamic): dynamic =
    self.holders[self.getType(self.array[pos], pos, self.array, self.params)][view.getTag()]

private fun mlNotifyChange(self: dynamic) {
    self.dso.forEach({ e: dynamic ->
        if (mlTruthy(e)) e.onChanged()
    })
}

private fun mlNotifyInvalidate(self: dynamic) {
    self.dso.forEach({ e: dynamic ->
        if (mlTruthy(e)) e.onInvalidated()
    })
}

private fun mlSetArray(self: dynamic, a: dynamic) {
    if (mlLooseNe(self.array, a)) {
        self.array.length = 0
        mlForIn(a, { i: dynamic -> self.array.push(a[i]) })
    }
    self.notifyChange()
}

// ---------------- 构造函数 / 工厂 ----------------

private fun mlCreate(
    arr: dynamic,
    types: dynamic,
    getType: dynamic,
    params: dynamic
): dynamic {
    // arr 是列表数组；types 的每个成员提供 maker(holder, params) 和
    // binder(holder, element, index, array, params)；getType 返回元素类型名。
    val src = arr
    val vtypes = js("[]")
    val holders = js("({})")
    val dso = js("[]")
    mlForIn(types, { i: dynamic ->
        vtypes.push(i)
        holders[i] = js("[]")
    })
    val controller = mlNewControllerJs(
        mlControllerCtor, src, holders, dso, types, vtypes, getType, params
    )
    val opts = js("({})")
    opts.array = src
    opts.controller = controller
    opts.dso = dso
    opts.getItemViewType = { pos: dynamic ->
        vtypes.indexOf(mlString(getType(src[pos], pos, src, params)))
    }
    opts.getViewTypeCount = { mlLength(vtypes) }
    val adapter = js("ListAdapterBase").create(opts)
    adapter.getView = { pos: dynamic, convert: dynamic, parent: dynamic ->
        try {
            val vtype = getType(src[pos], pos, src, params)
            var view = convert
            if (!mlTruthy(view) || !mlIn(view.getTag(), holders[vtype])) {
                val holder = js("({})")
                view = types[vtype].maker(holder, params)
                holder.self = view
                view.setTag(mlLength(holders[vtype]).toString())
                holders[vtype].push(holder)
            }
            val holder = holders[vtype][view.getTag()]
            holder.pos = mlParseInt(pos)
            types[vtype].binder(holder, src[pos], mlParseInt(pos), src, params)
            view
        } catch (e: dynamic) {
            js("ListAdapterBase").errorView(e)
        }
    }
    return mlNewListAdapterJs(adapter)
}

private fun mlMakeProto(): dynamic {
    val p = js("({})")

    p.clearHolder = mlVoidThisJs { self: dynamic, args: dynamic -> mlClearHolder(self) }
    p.getHolder = mlThisJs { self: dynamic, args: dynamic ->
        mlGetHolder(self, args[0], args[1])
    }
    p.notifyChange = mlVoidThisJs { self: dynamic, args: dynamic -> mlNotifyChange(self) }
    p.notifyInvalidate = mlVoidThisJs { self: dynamic, args: dynamic ->
        mlNotifyInvalidate(self)
    }
    p.setArray = mlVoidThisJs { self: dynamic, args: dynamic -> mlSetArray(self, args[0]) }

    return p
}

private val mlControllerCtor: dynamic = mlMakeCtorJs(
    { self: dynamic, array: dynamic, holders: dynamic, dso: dynamic, types: dynamic,
      vtypes: dynamic, getType: dynamic, params: dynamic ->
        mlInitController(self, array, holders, dso, types, vtypes, getType, params)
    }
)

private val mlFactory: dynamic = mlMakeFactoryJs(
    { arr: dynamic, types: dynamic, getType: dynamic, params: dynamic ->
        mlCreate(arr, types, getType, params)
    }
)

private fun mlGetController(adapter: dynamic): dynamic {
    val r = adapter.getItem(-1)
    r.self = adapter
    return r
}

fun installMultipleListAdapter() {
    mlControllerCtor.prototype = mlMakeProto()
    mlFactory.Controller = mlControllerCtor
    mlFactory.getController = { adapter: dynamic -> mlGetController(adapter) }

    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("MultipleListAdapter", mlFactory)
    }
}
