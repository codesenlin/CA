package ca.kotlin

/**
 * modules/listAdapter/MoreListAdapter.js 的 Kotlin/JS 版本。
 *
 * 保持原有 API：
 *   new MoreListAdapter(baseAdapter, loader) -> G.ListAdapter
 *   MoreListAdapter.Controller(...)
 *   MoreListAdapter.getController(adapter)
 *
 * loader 是外部可变对象（loading / finished / latestSession / loaderView ...），
 * 适配器只包装原 baseAdapter，不复制它的数据。原实现里的 baseAdapterCount、
 * baseAdapterTypeCount 是闭包变量，这里同样放在每次 create 的闭包里。
 */

// ---------------- JS 语义 helper ----------------
private val moreTruthyJs: dynamic = js("function (v) { return !!v; }")
private val moreForInJs: dynamic = js("function (o, f) { for (var i in o) f(i); }")
private val moreLooseEqJs: dynamic = js("function (a, b) { return a == b; }")
private val moreBindJs: dynamic = js("function (f, recv, a) { return f.bind(recv, a); }")
private val moreNewViewJs: dynamic = js("function () { return new G.View(ctx); }")
private val moreNewListAdapterJs: dynamic = js("function (a) { return new G.ListAdapter(a); }")

private val moreVoidThisJs: dynamic = js(
    "function (f) { return function () { f(this, arguments); }; }"
)
private val moreMakeCtorJs: dynamic = js(
    "function (init) {" +
        " return function (baseAdapter, dso, loader) {" +
        "  init(this, baseAdapter, dso, loader);" +
        " };" +
        "}"
)
private val moreMakeFactoryJs: dynamic = js(
    "function (impl) {" +
        " return function (baseAdapter, loader) {" +
        "  return impl(baseAdapter, loader);" +
        " };" +
        "}"
)
private val moreNewControllerJs: dynamic = js(
    "function (ctor, baseAdapter, dso, loader) {" +
        " return new ctor(baseAdapter, dso, loader);" +
        "}"
)

private fun moreTruthy(v: dynamic): Boolean = moreTruthyJs(v)
private fun moreLooseEq(a: dynamic, b: dynamic): Boolean = moreLooseEqJs(a, b)
private fun moreForIn(obj: dynamic, f: dynamic) {
    moreForInJs(obj, f)
}

// 原模块 IIFE 里的 latestSession，所有 controller 共享。
private var moreLatestSession: dynamic = 0

// ---------------- Controller 方法 ----------------

private fun moreInitController(
    self: dynamic,
    baseAdapter: dynamic,
    dso: dynamic,
    loader: dynamic
) {
    self.base = baseAdapter
    self.dso = dso
    self.loader = loader
}

private fun moreNotifyChange(self: dynamic) {
    moreForIn(self.dso, { i: dynamic -> self.dso[i].onChanged() })
}

private fun moreRequestLoad(self: dynamic) {
    moreLatestSession = moreLatestSession + 1
    val session = moreLatestSession
    self.loader.loading = true
    self.loader.latestSession = session
    self.loader.load(moreBindJs(self.asyncLoadComplete, self, session), session)
    if (moreTruthy(self.loader.loading)) {
        self.notifyChange()
    }
}

private fun morePostLoad(self: dynamic) {
    js("gHandler").post({
        try {
            self.requestLoad()
        } catch (e: dynamic) {
            js("erp")(e)
        }
    })
}

private fun moreCancelLoading(self: dynamic) {
    self.loader.latestSession = null
    self.loader.loading = false
}

private fun moreUpdateFinished(self: dynamic, finished: dynamic, alreadyNotify: dynamic) {
    self.loader.finished = moreTruthy(finished)
    if (!moreTruthy(alreadyNotify)) self.notifyChange()
}

private fun moreReset(self: dynamic, finished: dynamic, alreadyNotify: dynamic) {
    self.cancelLoading()
    self.updateFinished(finished, alreadyNotify)
}

private fun moreAsyncLoadComplete(
    self: dynamic,
    session: dynamic,
    finished: dynamic,
    alreadyNotify: dynamic
) {
    if (moreLooseEq(self.loader.latestSession, session)) {
        self.loader.loading = false
        self.updateFinished(finished, alreadyNotify)
    }
}

// ---------------- 构造函数 / 工厂 ----------------

private fun moreCreate(baseAdapter: dynamic, loader: dynamic): dynamic {
    // baseAdapter 原始 ListAdapter；loader 提供 loadingView / loaderView /
    // load(callback, session) / loading / finished / latestSession 等字段。
    val dso = js("[]")
    val autoload = loader.autoload
    val controller = moreNewControllerJs(moreControllerCtor, baseAdapter, dso, loader)
    if (moreTruthy(autoload)) {
        loader.loaderView = moreNewViewJs()
    }

    var baseAdapterCount: dynamic = js("undefined")
    var baseAdapterTypeCount: dynamic = js("undefined")

    val adapter = js("({})")
    adapter.getCount = {
        baseAdapterCount = baseAdapter.getCount()
        if (moreTruthy(loader.finished)) baseAdapterCount else baseAdapterCount + 1
    }
    adapter.getItem = { pos: dynamic ->
        if (moreLooseEq(pos, -1)) {
            controller
        } else if (moreLooseEq(pos, baseAdapterCount)) {
            loader
        } else {
            baseAdapter.getItem(pos)
        }
    }
    adapter.getItemId = { pos: dynamic ->
        if (moreLooseEq(pos, baseAdapterCount)) {
            if (moreTruthy(loader.loading)) 9999 else 9998
        } else {
            baseAdapter.getItemId(pos)
        }
    }
    adapter.getItemViewType = { pos: dynamic ->
        if (moreLooseEq(pos, baseAdapterCount)) {
            if (moreTruthy(loader.loading)) baseAdapterTypeCount + 1 else baseAdapterTypeCount
        } else {
            baseAdapter.getItemViewType(pos)
        }
    }
    adapter.getView = { pos: dynamic, convert: dynamic, parent: dynamic ->
        if (moreLooseEq(pos, baseAdapterCount)) {
            if (!moreTruthy(loader.loading) && moreTruthy(autoload)) {
                controller.postLoad()
            }
            if (moreTruthy(loader.loading)) loader.loadingView else loader.loaderView
        } else {
            baseAdapter.getView(pos, convert, parent)
        }
    }
    adapter.getViewTypeCount = {
        baseAdapterTypeCount = baseAdapter.getViewTypeCount()
        baseAdapterTypeCount + 2
    }
    adapter.hasStableIds = { baseAdapter.hasStableIds() }
    adapter.isEmpty = { false }
    adapter.areAllItemsEnabled = { baseAdapter.areAllItemsEnabled() }
    adapter.isEnabled = { pos: dynamic ->
        if (moreLooseEq(pos, baseAdapterCount)) true else baseAdapter.isEnabled(pos)
    }
    adapter.registerDataSetObserver = { p: dynamic ->
        baseAdapter.registerDataSetObserver(p)
        js("ListAdapterBase").register(dso, p)
        js("undefined")
    }
    adapter.unregisterDataSetObserver = { p: dynamic ->
        baseAdapter.unregisterDataSetObserver(p)
        js("ListAdapterBase").unregister(dso, p)
        js("undefined")
    }
    return moreNewListAdapterJs(adapter)
}

private fun moreMakeProto(): dynamic {
    val p = js("({})")

    p.notifyChange = moreVoidThisJs { self: dynamic, args: dynamic -> moreNotifyChange(self) }
    p.requestLoad = moreVoidThisJs { self: dynamic, args: dynamic -> moreRequestLoad(self) }
    p.postLoad = moreVoidThisJs { self: dynamic, args: dynamic -> morePostLoad(self) }
    p.cancelLoading = moreVoidThisJs { self: dynamic, args: dynamic ->
        moreCancelLoading(self)
    }
    p.updateFinished = moreVoidThisJs { self: dynamic, args: dynamic ->
        moreUpdateFinished(self, args[0], args[1])
    }
    p.reset = moreVoidThisJs { self: dynamic, args: dynamic ->
        moreReset(self, args[0], args[1])
    }
    p.asyncLoadComplete = moreVoidThisJs { self: dynamic, args: dynamic ->
        moreAsyncLoadComplete(self, args[0], args[1], args[2])
    }

    return p
}

private val moreControllerCtor: dynamic = moreMakeCtorJs(
    { self: dynamic, baseAdapter: dynamic, dso: dynamic, loader: dynamic ->
        moreInitController(self, baseAdapter, dso, loader)
    }
)

private val moreFactory: dynamic = moreMakeFactoryJs(
    { baseAdapter: dynamic, loader: dynamic -> moreCreate(baseAdapter, loader) }
)

private fun moreGetController(adapter: dynamic): dynamic {
    val r = adapter.getItem(-1)
    r.self = adapter
    return r
}

fun installMoreListAdapter() {
    moreControllerCtor.prototype = moreMakeProto()
    moreFactory.Controller = moreControllerCtor
    moreFactory.getController = { adapter: dynamic -> moreGetController(adapter) }

    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("MoreListAdapter", moreFactory)
    }
}
