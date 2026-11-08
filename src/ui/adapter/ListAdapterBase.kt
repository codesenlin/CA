package ca.kotlin

/**
 * modules/listAdapter/ListAdapterBase.js 的 Kotlin/JS 版本。
 *
 * 这个模块不是适配器，而是各 ListAdapter 共用的公共方法工厂：
 *   - register / unregister 维护 DataSetObserver 数组；
 *   - create 以 opts 为模板生成普通 JS 对象，并持有可替换的数组引用；
 *   - errorView 仍是列表项绑定异常时的兜底视图。
 *
 * 原实现大量使用 JS 真值、宽松相等和动态数组 length，这些语义
 * 通过很薄的 JS helper 保留；逻辑本身仍在 Kotlin 侧。
 */
private val laApi: dynamic = js("({})")

// ---------------- JS 语义 helper ----------------
private val laTruthyJs: dynamic = js("function (v) { return !!v; }")
private val laLooseEqJs: dynamic = js("function (a, b) { return a == b; }")
private val laIndexOfJs: dynamic = js("function (a, v) { return a.indexOf(v); }")
private val laLengthJs: dynamic = js("function (v) { return v.length | 0; }")
private val laIsEnabledJs: dynamic = js("function (p, n) { return p >= 0 && p < n; }")
private val laErrorViewJs: dynamic = js(
    """function (e) {
        var a = new G.TextView(ctx);
        a.setText(e + "\n" + e.stack);
        erp(e);
        return a;
    }"""
)

private fun laTruthy(v: dynamic): Boolean = laTruthyJs(v)
private fun laLooseEq(a: dynamic, b: dynamic): Boolean = laLooseEqJs(a, b)
private fun laLength(v: dynamic): Int = laLengthJs(v)

private fun laRegister(dso: dynamic, p: dynamic) {
    if (laIndexOfJs(dso, p) >= 0) return
    dso.push(p)
}

private fun laUnregister(dso: dynamic, p: dynamic) {
    val i = laIndexOfJs(dso, p)
    if (i >= 0) dso.splice(i, 1)
}

/**
 * 生成 G.ListAdapter 的公共方法。
 *
 * opts.array 是适配器当前持有的数组；opts.controller、opts.dso 以及
 * getCount/getItem/... 覆盖项都按原实现原样保存。注意这里故意返回普通
 * JS 对象而不是 Kotlin 类实例：调用方会直接给返回值补 getView，
 * 再交给 new G.ListAdapter(...)。
 */
private fun laCreate(opts: dynamic): dynamic {
    var arr = opts.array
    val adapter = js("({})")

    adapter._replaceArray = { a: dynamic -> arr = a }

    val defaultCount: dynamic = { laLengthJs(arr) }
    adapter.getCount = if (laTruthy(opts.getCount)) opts.getCount else defaultCount

    val defaultItem: dynamic = { pos: dynamic ->
        if (laLooseEq(pos, -1)) opts.controller else arr[pos]
    }
    adapter.getItem = if (laTruthy(opts.getItem)) opts.getItem else defaultItem

    val defaultItemId: dynamic = { pos: dynamic -> pos }
    adapter.getItemId = if (laTruthy(opts.getItemId)) opts.getItemId else defaultItemId

    val defaultViewType: dynamic = { _: dynamic -> 0 }
    adapter.getItemViewType = if (laTruthy(opts.getItemViewType)) opts.getItemViewType else defaultViewType

    val defaultViewTypeCount: dynamic = { 1 }
    adapter.getViewTypeCount = if (laTruthy(opts.getViewTypeCount)) opts.getViewTypeCount else defaultViewTypeCount

    val defaultHasStableIds: dynamic = { false }
    adapter.hasStableIds = if (laTruthy(opts.hasStableIds)) opts.hasStableIds else defaultHasStableIds

    val defaultIsEmpty: dynamic = { laLengthJs(arr) == 0 }
    adapter.isEmpty = if (laTruthy(opts.isEmpty)) opts.isEmpty else defaultIsEmpty

    val defaultAllEnabled: dynamic = { true }
    adapter.areAllItemsEnabled = if (laTruthy(opts.areAllItemsEnabled)) opts.areAllItemsEnabled else defaultAllEnabled

    val defaultIsEnabled: dynamic = { pos: dynamic -> laIsEnabledJs(pos, laLengthJs(arr)) }
    adapter.isEnabled = if (laTruthy(opts.isEnabled)) opts.isEnabled else defaultIsEnabled

    adapter.registerDataSetObserver = { p: dynamic ->
        laApi.register(opts.dso, p)
        js("undefined")
    }
    adapter.unregisterDataSetObserver = { p: dynamic ->
        laApi.unregister(opts.dso, p)
        js("undefined")
    }
    return adapter
}

fun installListAdapterBase() {
    laApi.register = { dso: dynamic, p: dynamic ->
        laRegister(dso, p)
        js("undefined")
    }
    laApi.unregister = { dso: dynamic, p: dynamic ->
        laUnregister(dso, p)
        js("undefined")
    }
    laApi.create = { opts: dynamic -> laCreate(opts) }
    laApi.errorView = { e: dynamic -> laErrorViewJs(e) }

    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("ListAdapterBase", laApi)
    }
}
