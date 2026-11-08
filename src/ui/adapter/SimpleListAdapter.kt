package ca.kotlin

/**
 * modules/listAdapter/SimpleListAdapter.js 的 Kotlin/JS 版本。
 *
 * 保持原有 API：
 *   new SimpleListAdapter(arr, maker, binder, params, readOnly) -> G.ListAdapter
 *   SimpleListAdapter.Controller(...)
 *   SimpleListAdapter.getController(adapter)
 *
 * 原实现是 IIFE 里的工厂函数 + Controller 原型，方法全部读取动态 this；
 * 这里用两层很薄的 JS wrapper 把真实 this / arguments 传回 Kotlin：
 *   - makeFactory/makeCtor 负责 new 与返回值语义；
 *   - this/voidThis 负责 Controller.prototype 上的方法。
 * 适配器本身仍交给 ListAdapterBase.create，避免复制已有模块的逻辑。
 */

// ---------------- JS 语义 helper ----------------
private val slTruthyJs: dynamic = js("function (v) { return !!v; }")
private val slLengthJs: dynamic = js("function (v) { return v.length | 0; }")
private val slParseIntJs: dynamic = js("function (v) { return parseInt(v); }")
private val slInJs: dynamic = js("function (k, o) { return k in o; }")
private val slForInJs: dynamic = js("function (o, f) { for (var i in o) f(i); }")
private val slLooseEqJs: dynamic = js("function (a, b) { return a == b; }")
private val slLooseNeJs: dynamic = js("function (a, b) { return a != b; }")
private val slSliceJs: dynamic = js("function (a) { return a.slice(); }")
private val slNewListAdapterJs: dynamic = js("function (a) { return new G.ListAdapter(a); }")

// 把 Controller 方法调用时的动态 this / arguments 原样带回 Kotlin。
private val slThisJs: dynamic = js(
    "function (f) { return function () { return f(this, arguments); }; }"
)
private val slVoidThisJs: dynamic = js(
    "function (f) { return function () { f(this, arguments); }; }"
)
private val slMakeCtorJs: dynamic = js(
    "function (init) {" +
        " return function (array, holders, dso, maker, binder, params, sync, readOnly) {" +
        "  init(this, array, holders, dso, maker, binder, params, sync, readOnly);" +
        " };" +
        "}"
)
private val slMakeFactoryJs: dynamic = js(
    "function (impl) {" +
        " return function (arr, maker, binder, params, readOnly) {" +
        "  return impl(arr, maker, binder, params, readOnly);" +
        " };" +
        "}"
)
private val slNewControllerJs: dynamic = js(
    "function (ctor, array, holders, dso, maker, binder, params, sync, readOnly) {" +
        " return new ctor(array, holders, dso, maker, binder, params, sync, readOnly);" +
        "}"
)

private fun slTruthy(v: dynamic): Boolean = slTruthyJs(v)
private fun slLength(v: dynamic): Int = slLengthJs(v)
private fun slParseInt(v: dynamic): dynamic = slParseIntJs(v)
private fun slIn(key: dynamic, obj: dynamic): Boolean = slInJs(key, obj)
private fun slLooseEq(a: dynamic, b: dynamic): Boolean = slLooseEqJs(a, b)
private fun slLooseNe(a: dynamic, b: dynamic): Boolean = slLooseNeJs(a, b)
private fun slForIn(obj: dynamic, f: dynamic) {
    slForInJs(obj, f)
}

// ---------------- Controller 方法 ----------------

private fun slInitController(
    self: dynamic,
    array: dynamic,
    holders: dynamic,
    dso: dynamic,
    maker: dynamic,
    binder: dynamic,
    params: dynamic,
    sync: dynamic,
    readOnly: dynamic
) {
    self.array = array
    self.holders = holders
    self.dso = dso
    self.maker = maker
    self.binder = binder
    self.params = params
    self.sync = sync
    self.readOnly = slTruthy(readOnly)
}

private fun slClearHolder(self: dynamic) {
    slForIn(self.holders, { i: dynamic -> self.holders[i].self.setTag("") })
    self.holders.length = 0
    self.notifyChange()
}

private fun slGetHolder(self: dynamic, view: dynamic): dynamic = self.holders[view.getTag()]

private fun slNotifyChange(self: dynamic, hasSync: dynamic) {
    // 原实现先 setArray（其内部会 notifyChange(true)），然后当前这次
    // 调用仍会继续遍历 dso，所以同步数组变化时观察者会收到两次 onChanged。
    if (!slTruthy(hasSync) && slTruthy(self.sync)) self.setArray(self.sync)
    self.dso.forEach({ e: dynamic ->
        if (slTruthy(e)) e.onChanged()
    })
}

private fun slNotifyInvalidate(self: dynamic) {
    self.dso.forEach({ e: dynamic ->
        if (slTruthy(e)) e.onInvalidated()
    })
}

private fun slRebind(self: dynamic, pos: dynamic) {
    slForIn(self.holders, { i: dynamic ->
        val holder = self.holders[i]
        if (slLooseEq(holder.pos, pos)) {
            self.binder(holder, self.array[pos], slParseInt(pos), self.array, self.params)
        }
    })
}

private fun slSetArray(self: dynamic, a: dynamic) {
    if (slLooseNe(self.array, a)) {
        if (slTruthy(self.readOnly)) {
            // readOnly 适配器没有外部同步数组，直接换引用即可；
            // 这样 Accel.filterIDTexts 返回的 Java int[] 不用逐项转成 JS 数组。
            self.array = a
            self.adapter._replaceArray(a)
        } else {
            self.array.length = slLength(a)
            var i = 0
            val n = slLength(a)
            while (i < n) {
                self.array[i] = a[i]
                i++
            }
        }
    }
    self.notifyChange(true)
}

private fun slSetSync(self: dynamic, a: dynamic) {
    self.sync = a
    self.notifyChange(false)
}

// ---------------- 构造函数 / 工厂 ----------------

private fun slCreate(
    arr: dynamic,
    maker: dynamic,
    binder: dynamic,
    params: dynamic,
    readOnly: dynamic
): dynamic {
    // arr 是列表数组，maker(holder, params) 生成基础 view，
    // binder(holder, element, index, array, params) 修改 view。
    // readOnly 表示数组是否只读（true 时直接持有原数组引用）。
    val src = if (slTruthy(readOnly)) arr else slSliceJs(arr)
    val holders = js("[]")
    val dso = js("[]")
    val controller = slNewControllerJs(
        slControllerCtor,
        src,
        holders,
        dso,
        maker,
        binder,
        params,
        if (slTruthy(readOnly)) null else arr,
        readOnly
    )
    val opts = js("({})")
    opts.array = src
    opts.controller = controller
    opts.dso = dso
    val adapter = js("ListAdapterBase").create(opts)
    controller.adapter = adapter
    adapter.getView = { pos: dynamic, convert: dynamic, parent: dynamic ->
        try {
            var view = convert
            if (!slTruthy(view) || !slIn(view.getTag(), holders)) {
                val holder = js("({})")
                view = maker(holder, params)
                holder.self = view
                view.setTag(slLength(holders).toString())
                holders.push(holder)
            }
            val holder = holders[view.getTag()]
            holder.pos = slParseInt(pos)
            binder(holder, controller.array[pos], slParseInt(pos), controller.array, params)
            view
        } catch (e: dynamic) {
            js("ListAdapterBase").errorView(e)
        }
    }
    return slNewListAdapterJs(adapter)
}

private fun slMakeProto(): dynamic {
    val p = js("({})")

    p.clearHolder = slVoidThisJs { self: dynamic, args: dynamic -> slClearHolder(self) }
    p.getHolder = slThisJs { self: dynamic, args: dynamic -> slGetHolder(self, args[0]) }
    p.notifyChange = slVoidThisJs { self: dynamic, args: dynamic ->
        slNotifyChange(self, args[0])
    }
    p.notifyInvalidate = slVoidThisJs { self: dynamic, args: dynamic ->
        slNotifyInvalidate(self)
    }
    p.rebind = slVoidThisJs { self: dynamic, args: dynamic -> slRebind(self, args[0]) }
    p.setArray = slVoidThisJs { self: dynamic, args: dynamic -> slSetArray(self, args[0]) }
    p.setSync = slVoidThisJs { self: dynamic, args: dynamic -> slSetSync(self, args[0]) }

    return p
}

private val slControllerCtor: dynamic = slMakeCtorJs(
    { self: dynamic, array: dynamic, holders: dynamic, dso: dynamic, maker: dynamic,
      binder: dynamic, params: dynamic, sync: dynamic, readOnly: dynamic ->
        slInitController(self, array, holders, dso, maker, binder, params, sync, readOnly)
    }
)

private val slFactory: dynamic = slMakeFactoryJs(
    { arr: dynamic, maker: dynamic, binder: dynamic, params: dynamic, readOnly: dynamic ->
        slCreate(arr, maker, binder, params, readOnly)
    }
)

private fun slGetController(adapter: dynamic): dynamic {
    val r = adapter.getItem(-1)
    r.self = adapter
    return r
}

fun installSimpleListAdapter() {
    slControllerCtor.prototype = slMakeProto()
    slFactory.Controller = slControllerCtor
    slFactory.getController = { adapter: dynamic -> slGetController(adapter) }

    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("SimpleListAdapter", slFactory)
    }
}
