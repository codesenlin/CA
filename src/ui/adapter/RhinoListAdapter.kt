package ca.kotlin.ui.adapter

import ca.kotlin.core.*


/**
 * modules/listAdapter/RhinoListAdapter.js 的 Kotlin/JS 版本。
 *
 * 这是一个构造函数：new RhinoListAdapter(arr, vmaker, params, preload) 返回
 * 包装后的 G.ListAdapter，并通过 RhinoListAdapter.Controller 暴露控制器。
 * 控制器原型方法大量读取动态 this，并由外部以 controller.xxx() 调用，
 * 因此原型方法统一用一层极薄的 JS wrapper 把真实 this / arguments 传回 Kotlin。
 *
 * 注意 vmaker 在原实现里通过 this.vmaker(...) 调用（this 为 controller），
 * 这里用 helper 保持同样的 this 绑定。
 */
private val rhTruthyJs: dynamic = js("function (v) { return !!v; }")
private val rhLooseNeJs: dynamic = js("function (a, b) { return a != b; }")
private val rhLengthJs: dynamic = js("function (v) { return v.length | 0; }")
private val rhParseIntJs: dynamic = js("function (v) { return parseInt(v); }")
private val rhForInJs: dynamic = js("function (o, f) { for (var i in o) f(i); }")
private val rhSliceJs: dynamic = js("function (a) { return a.slice(); }")
private val rhNewArrayJs: dynamic = js("function (n) { return new Array(n); }")
private val rhArgsToArrayJs: dynamic = js(
    "function (args) { var z = []; for (var i in args) z.push(args[i]); return z; }"
)
private val rhSpliceApplyJs: dynamic = js(
    "function (a, z) { return Array.prototype.splice.apply(a, z); }"
)
private val rhSliceApplyJs: dynamic = js(
    "function (a, args) { return Array.prototype.slice.apply(a, args); }"
)
private val rhCallVmakerJs: dynamic = js(
    "function (self, e, i, a, p) { return self.vmaker(e, i, a, p); }"
)
private val rhCallFilterJs: dynamic = js(
    "function (f, thisArg, e, i, a) { return f.call(thisArg, e, i, a); }"
)
private val rhNewListAdapterJs: dynamic = js("function (o) { return new G.ListAdapter(o); }")
private val rhErrorViewJs: dynamic = js(
    "function (e) { return ListAdapterBase.errorView(e); }"
)
private val rhNewControllerJs: dynamic = js(
    "function (src, views, dso, vmaker, params, preload) {" +
        " return new RhinoListAdapter.Controller(src, views, dso, vmaker, params, preload);" +
        "}"
)
private val rhSpliceIndexJs: dynamic = js("function (i, index) { return i - 2 + index; }")
// 原型方法的 this / arguments 通过薄 wrapper 转发；formal 参数只用于
// 保持与原实现相同的 Function.length，调用时仍转发完整 arguments。
private val rhThis0Js: dynamic = js(
    "function (f) { return function () { return f(this, arguments); }; }"
)
private val rhThis1Js: dynamic = js(
    "function (f) { return function (a) { return f(this, arguments); }; }"
)
private val rhThis2Js: dynamic = js(
    "function (f) { return function (a, b) { return f(this, arguments); }; }"
)
private val rhVoidThis0Js: dynamic = js(
    "function (f) { return function () { f(this, arguments); }; }"
)
private val rhVoidThis1Js: dynamic = js(
    "function (f) { return function (a) { f(this, arguments); }; }"
)
private val rhVoidThis2Js: dynamic = js(
    "function (f) { return function (a, b) { f(this, arguments); }; }"
)
private val rhVoidThis3Js: dynamic = js(
    "function (f) { return function (a, b, c) { f(this, arguments); }; }"
)
private val rhMakeCtorJs: dynamic = js(
    "function (init) {" +
        " return function (src, views, dso, vmaker, params, preload) {" +
        "  init(this, src, views, dso, vmaker, params, preload);" +
        " };" +
        "}"
)
private val rhMakeFactoryJs: dynamic = js(
    "function (impl) {" +
        " return function (arr, vmaker, params, preload) {" +
        "  return impl(arr, vmaker, params, preload);" +
        " };" +
        "}"
)

private fun rhTruthy(v: dynamic): Boolean = rhTruthyJs(v)
private fun rhLooseNe(a: dynamic, b: dynamic): Boolean = rhLooseNeJs(a, b)
private fun rhLength(v: dynamic): Int = rhLengthJs(v)

// ---------------- Controller 方法 ----------------

private fun rhNotifyChange(self: dynamic) {
    self.dso.forEach({ e: dynamic ->
        if (rhTruthy(e)) e.onChanged()
    })
}

private fun rhNotifyInvalidate(self: dynamic) {
    self.dso.forEach({ e: dynamic ->
        if (rhTruthy(e)) e.onInvalidated()
    })
}

private fun rhAdd(self: dynamic, e: dynamic) {
    self.src.push(e)
    if (rhTruthy(self.preload)) {
        self.views.push(rhCallVmakerJs(self, e, rhLength(self.src) - 1, self.src, self.params))
    }
    rhNotifyChange(self)
}

private fun rhConcat(self: dynamic, arr: dynamic) {
    arr.forEach({ e: dynamic ->
        self.src.push(e)
        if (rhTruthy(self.preload)) {
            self.views.push(rhCallVmakerJs(self, e, rhLength(self.src) - 1, self.src, self.params))
        }
    })
    rhNotifyChange(self)
}

private fun rhFilter(self: dynamic, f: dynamic, thisArg: dynamic) {
    var i = 0
    while (i < rhLength(self.src)) {
        if (!rhTruthy(rhCallFilterJs(f, thisArg, self.src[i], i, self.src))) {
            self.src.splice(i, 1)
            self.views.splice(i, 1)
            i--
        }
        i++
    }
    rhNotifyChange(self)
}

private fun rhForEach(self: dynamic, f: dynamic, thisArg: dynamic) {
    rhForInJs(self.src, { i: dynamic ->
        if (rhTruthy(rhCallFilterJs(f, thisArg, self.src[i], i, self.src))) {
            self.views[i] = rhCallVmakerJs(self, self.src[i], i, self.src, self.params)
        }
    })
    rhNotifyChange(self)
}

private fun rhGet(self: dynamic, i: dynamic): dynamic = self.src[i]

private fun rhInsert(self: dynamic, e: dynamic, i: dynamic, respawn: dynamic) {
    self.src.splice(i, 0, e)
    if (rhTruthy(respawn)) {
        self.respawnAll()
    } else {
        self.views.splice(
            i,
            0,
            if (rhTruthy(self.preload)) rhCallVmakerJs(self, e, i, self.src, self.params) else null
        )
    }
    rhNotifyChange(self)
}

private fun rhRemove(self: dynamic, e: dynamic, respawn: dynamic) {
    var i = rhLength(self.src)
    while (i >= 0) {
        if (rhLooseNe(self.src[i], e)) {
            i--
            continue
        }
        self.src.splice(i, 1)
        self.views.splice(i, 1)
        i--
    }
    if (rhTruthy(respawn)) self.respawnAll()
    rhNotifyChange(self)
}

private fun rhRemoveByIndex(self: dynamic, i: dynamic, respawn: dynamic) {
    self.src.splice(i, 1)
    self.views.splice(i, 1)
    if (rhTruthy(respawn)) self.respawnAll()
    rhNotifyChange(self)
}

private fun rhReplace(self: dynamic, e: dynamic, i: dynamic) {
    self.src[i] = e
    self.views[i] = if (rhTruthy(self.preload)) rhCallVmakerJs(self, e, i, self.src, self.params) else null
    rhNotifyChange(self)
}

private fun rhRespawn(self: dynamic, i: dynamic) {
    self.views[i] = rhCallVmakerJs(self, self.src[i], i, self.src, self.params)
    rhNotifyChange(self)
}

private fun rhRespawnAll(self: dynamic) {
    self.src.forEach({ e: dynamic, i: dynamic, a: dynamic ->
        self.views[i] = rhCallVmakerJs(self, e, i, a, self.params)
    })
    rhNotifyChange(self)
}

private fun rhSlice(self: dynamic, args: dynamic): dynamic = rhSliceApplyJs(self.src, args)

private fun rhSplice(self: dynamic, args: dynamic) {
    val z = rhArgsToArrayJs(args)
    val index = args[0]
    rhSpliceApplyJs(self.src, z)
    var i = 2
    while (i < rhLength(z)) {
        z[i] = if (rhTruthy(self.preload)) {
            rhCallVmakerJs(self, z[i], rhSpliceIndexJs(i, index), self.src, self.params)
        } else {
            null
        }
        i++
    }
    rhSpliceApplyJs(self.views, z)
    rhNotifyChange(self)
}

private fun rhGetArray(self: dynamic): dynamic = rhSliceJs(self.src)

private fun rhSetArray(self: dynamic, a: dynamic) {
    self.views.length = 0
    self.src.length = 0
    rhForInJs(a, { i: dynamic -> self.src.push(a[i]) })
    self.views.length = rhLength(self.src)
    if (rhTruthy(self.preload)) {
        self.respawnAll()
    } else {
        rhNotifyChange(self)
    }
}

private fun rhInitController(
    self: dynamic,
    src: dynamic,
    views: dynamic,
    dso: dynamic,
    vmaker: dynamic,
    params: dynamic,
    preload: dynamic
) {
    self.src = src
    self.views = views
    self.dso = dso
    self.vmaker = vmaker
    self.params = params
    self.preload = preload
}

// ---------------- 构造函数 / 原型 ----------------

private fun rhMakeProto(): dynamic {
    val p = js("({})")

    p.notifyChange = rhVoidThis0Js { self: dynamic, args: dynamic -> rhNotifyChange(self) }
    p.notifyInvalidate = rhVoidThis0Js { self: dynamic, args: dynamic -> rhNotifyInvalidate(self) }
    p.add = rhVoidThis1Js { self: dynamic, args: dynamic -> rhAdd(self, args[0]) }
    p.concat = rhVoidThis1Js { self: dynamic, args: dynamic -> rhConcat(self, args[0]) }
    p.filter = rhVoidThis2Js { self: dynamic, args: dynamic ->
        rhFilter(self, args[0], args[1])
    }
    p.forEach = rhVoidThis2Js { self: dynamic, args: dynamic ->
        rhForEach(self, args[0], args[1])
    }
    p.get = rhThis1Js { self: dynamic, args: dynamic -> rhGet(self, args[0]) }
    p.insert = rhVoidThis3Js { self: dynamic, args: dynamic ->
        rhInsert(self, args[0], args[1], args[2])
    }
    p.length = rhThis0Js { self: dynamic, args: dynamic -> rhLength(self.src) }
    p.remove = rhVoidThis2Js { self: dynamic, args: dynamic ->
        rhRemove(self, args[0], args[1])
    }
    p.removeByIndex = rhVoidThis2Js { self: dynamic, args: dynamic ->
        rhRemoveByIndex(self, args[0], args[1])
    }
    p.replace = rhVoidThis2Js { self: dynamic, args: dynamic ->
        rhReplace(self, args[0], args[1])
    }
    p.respawn = rhVoidThis1Js { self: dynamic, args: dynamic -> rhRespawn(self, args[0]) }
    p.respawnAll = rhVoidThis1Js { self: dynamic, args: dynamic -> rhRespawnAll(self) }
    p.slice = rhThis2Js { self: dynamic, args: dynamic -> rhSlice(self, args) }
    p.splice = rhVoidThis2Js { self: dynamic, args: dynamic -> rhSplice(self, args) }
    p.getArray = rhThis0Js { self: dynamic, args: dynamic -> rhGetArray(self) }
    p.setArray = rhVoidThis1Js { self: dynamic, args: dynamic -> rhSetArray(self, args[0]) }

    return p
}

private val rhControllerProto: dynamic = rhMakeProto()
private val rhControllerCtor: dynamic = rhMakeCtorJs(
    { self: dynamic, src: dynamic, views: dynamic, dso: dynamic, vmaker: dynamic, params: dynamic, preload: dynamic ->
        rhInitController(self, src, views, dso, vmaker, params, preload)
    }
)
private val rhFactory: dynamic = rhMakeFactoryJs(
    { arr: dynamic, vmaker: dynamic, params: dynamic, preload: dynamic ->
        rhCreate(arr, vmaker, params, preload)
    }
)

private fun rhCreate(arr: dynamic, vmaker: dynamic, params: dynamic, preload: dynamic): dynamic {
    // arr 是列表数组，vmaker(element, index, array, params) 从 item 生成指定 view。
    val src = rhSliceJs(arr)
    val views = rhNewArrayJs(rhLength(arr))
    val dso = js("[]")
    if (rhTruthy(preload)) {
        src.forEach({ e: dynamic, i: dynamic, a: dynamic ->
            views[i] = vmaker(e, i, a, params)
        })
    }
    val controller = rhNewControllerJs(src, views, dso, vmaker, params, preload)
    val opts = js("({})")
    opts.array = src
    opts.controller = controller
    opts.dso = dso
    val adapter = js("ListAdapterBase").create(opts)
    adapter.getView = { pos: dynamic, convert: dynamic, parent: dynamic ->
        try {
            val existing = views[pos]
            if (rhTruthy(existing)) {
                existing
            } else {
                val created = vmaker(src[pos], rhParseIntJs(pos), src, params)
                views[pos] = created
                created
            }
        } catch (e: dynamic) {
            rhErrorViewJs(e)
        }
    }
    return rhNewListAdapterJs(adapter)
}

private fun rhGetController(adapter: dynamic): dynamic {
    val r = adapter.getItem(-1)
    r.self = adapter
    return r
}

fun installRhinoListAdapter() {
    rhControllerCtor.prototype = rhControllerProto
    rhFactory.Controller = rhControllerCtor
    rhFactory.getController = { adapter: dynamic -> rhGetController(adapter) }

    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("RhinoListAdapter", rhFactory)
    }
}
