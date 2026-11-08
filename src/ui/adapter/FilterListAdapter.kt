package ca.kotlin

/**
 * modules/listAdapter/FilterListAdapter.js 的 Kotlin/JS 版本。
 *
 * 它是一个构造包装器：new FilterListAdapter(wrap) 返回可以 setFilter 的
 * 过滤控制器；build() 生成真正交给 G.ListAdapter 的适配器对象。
 * 原型方法和绑定回调大量依赖动态 this / self，这里用薄 JS wrapper
 * 把真实 this 传回 Kotlin，并保持 JavaAdapter 的注册顺序。
 */
private val faTruthyJs: dynamic = js("function (v) { return !!v; }")
private val faForInJs: dynamic = js("function (o, f) { for (var i in o) f(i); }")
private val faLengthJs: dynamic = js("function (v) { return v.length | 0; }")
private val faLtJs: dynamic = js("function (a, b) { return a < b; }")
private val faNotNullJs: dynamic = js("function (v) { return v != null; }")
private val faOrZeroJs: dynamic = js("function (v) { return v || 0; }")
private val faNewListAdapterJs: dynamic = js("function (o) { return new G.ListAdapter(o); }")
// 原型方法保持原 Function.length，同时把真实 this / arguments 转发给 Kotlin。
private val faThis0Js: dynamic = js(
    "function (f) { return function () { return f(this, arguments); }; }"
)
private val faThis1Js: dynamic = js(
    "function (f) { return function (a) { return f(this, arguments); }; }"
)
private val faVoidThis0Js: dynamic = js(
    "function (f) { return function () { f(this, arguments); }; }"
)
private val faVoidThis1Js: dynamic = js(
    "function (f) { return function (a) { f(this, arguments); }; }"
)
private val faMakeCtorJs: dynamic = js(
    "function (init) { return function (wrap) { init(this, wrap); }; }"
)
private val faTryAttachJs: dynamic = js(
    """function (self) {
        var r = new java.lang.Runnable({
            run: function () { // 防止直接从 InterfaceAdapter 抛出
                self._wrap.registerDataSetObserver(new JavaAdapter(android.database.DataSetObserver, {
                    onChanged: function () {
                        self.requestFilter();
                    }
                }));
            }
        });
        r.run();
    }"""
)

private fun faTruthy(v: dynamic): Boolean = faTruthyJs(v)
private fun faLt(a: dynamic, b: dynamic): Boolean = faLtJs(a, b)
private fun faNotNull(v: dynamic): Boolean = faNotNullJs(v)

private fun faInit(self: dynamic, wrap: dynamic) {
    self._wrap = wrap
    self._dso = js("[]")
    self._pos = js("[]")
}

private fun faTryAttach(self: dynamic): Boolean {
    try {
        faTryAttachJs(self)
        return true
    } catch (e: dynamic) {
        js("Log").e(e)
    }
    return false
}

private fun faBuild(self: dynamic): dynamic {
    if (faTruthy(self.buildAdapter)) return self.buildAdapter

    val adapter = js("({})")
    adapter.getCount = {
        if (faTruthy(self._filter)) faLengthJs(self._pos) else self._wrap.getCount()
    }
    adapter.getItem = { pos: dynamic ->
        self._wrap.getItem(self.getRealPosition(pos))
    }
    adapter.getItemId = { pos: dynamic ->
        self._wrap.getItemId(self.getRealPosition(pos))
    }
    adapter.getItemViewType = { pos: dynamic ->
        self._wrap.getItemViewType(self.getRealPosition(pos))
    }
    adapter.getView = { pos: dynamic, convert: dynamic, parent: dynamic ->
        self._wrap.getView(self.getRealPosition(pos), convert, parent)
    }
    adapter.getViewTypeCount = { self._wrap.getViewTypeCount() }
    adapter.hasStableIds = { self._wrap.hasStableIds() }
    adapter.isEmpty = {
        if (faTruthy(self._filter)) faLengthJs(self._pos) == 0 else self._wrap.isEmpty()
    }
    adapter.areAllItemsEnabled = { self._wrap.areAllItemsEnabled() }
    adapter.isEnabled = { pos: dynamic ->
        self._wrap.isEnabled(self.getRealPosition(pos))
    }
    adapter.registerDataSetObserver = { p: dynamic ->
        self._wrap.registerDataSetObserver(p)
        js("ListAdapterBase").register(self._dso, p)
        js("undefined")
    }
    adapter.unregisterDataSetObserver = { p: dynamic ->
        self._wrap.unregisterDataSetObserver(p)
        js("ListAdapterBase").unregister(self._dso, p)
        js("undefined")
    }

    val instance = faNewListAdapterJs(adapter)
    self.buildAdapter = instance
    return instance
}

private fun faSetFilter(self: dynamic, f: dynamic) {
    self._filter = f
    self.requestFilter()
}

private fun faHasFilter(self: dynamic): Boolean = faNotNullJs(self._filter)

private fun faRequestFilter(self: dynamic) {
    if (faNotNull(self._filter)) {
        val n = self._wrap.getCount()
        self._pos.length = 0
        var i = 0
        while (faLt(i, n)) {
            if (faTruthy(self._filter(self._wrap.getItem(i), i))) {
                self._pos.push(i)
            }
            i++
        }
    }
    self.notifyDataSetChanged()
}

private fun faGetRealPosition(self: dynamic, pos: dynamic): dynamic =
    if (faTruthy(self._filter)) faOrZeroJs(self._pos[pos]) else pos

private fun faNotifyDataSetChanged(self: dynamic) {
    faForInJs(self._dso, { i: dynamic ->
        self._dso[i].onChanged()
    })
}

private fun faMakeProto(): dynamic {
    val p = js("({})")

    p.tryAttach = faThis0Js { self: dynamic, args: dynamic -> faTryAttach(self) }
    p.build = faThis0Js { self: dynamic, args: dynamic -> faBuild(self) }
    p.setFilter = faVoidThis1Js { self: dynamic, args: dynamic -> faSetFilter(self, args[0]) }
    p.clearFilter = faVoidThis0Js { self: dynamic, args: dynamic -> self.setFilter(null) }
    p.hasFilter = faThis0Js { self: dynamic, args: dynamic -> faHasFilter(self) }
    p.requestFilter = faVoidThis0Js { self: dynamic, args: dynamic -> faRequestFilter(self) }
    p.getRealPosition = faThis1Js { self: dynamic, args: dynamic ->
        faGetRealPosition(self, args[0])
    }
    p.notifyDataSetChanged = faVoidThis0Js { self: dynamic, args: dynamic ->
        faNotifyDataSetChanged(self)
    }

    return p
}

private val faProto: dynamic = faMakeProto()
private val faCtor: dynamic = faMakeCtorJs({ self: dynamic, wrap: dynamic -> faInit(self, wrap) })

fun installFilterListAdapter() {
    faCtor.prototype = faProto

    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("FilterListAdapter", faCtor)
    }
}
