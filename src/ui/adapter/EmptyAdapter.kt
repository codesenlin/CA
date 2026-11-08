package ca.kotlin.ui.adapter

import ca.kotlin.core.*


/**
 * modules/listAdapter/EmptyAdapter.js 的 Kotlin/JS 版本。
 *
 * EmptyAdapter 是一个已经构造好的 G.ListAdapter 单例（不是构造函数），
 * 每个 parent 复用同一个“空空如也”TextView，因此仍按原实现把
 * k / v 两个缓存数组放在 install 的闭包里。
 */
private val eaIndexOfJs: dynamic = js("function (a, v) { return a.indexOf(v); }")
private val eaNewListAdapterJs: dynamic = js("function (o) { return new G.ListAdapter(o); }")
private val eaErrorViewJs: dynamic = js(
    """function (e) {
        var a = new G.TextView(ctx);
        a.setText(e + "\n" + e.stack);
        erp(e);
        return a;
    }"""
)

private fun eaIndexOf(a: dynamic, v: dynamic): Int = eaIndexOfJs(a, v)

/** 原实现里的 build()：构造提示文本并应用 textview_prompt 样式。 */
private val eaBuildJs: dynamic = js(
    """function () {
        var text = new G.TextView(ctx);
        text.setLayoutParams(new G.AbsListView.LayoutParams(-1, -2));
        text.setText("空空如也");
        text.setPadding(0, 40 * G.dp, 0, 40 * G.dp);
        text.setGravity(G.Gravity.CENTER);
        text.setFocusable(true);
        Common.applyStyle(text, "textview_prompt", 4);
        return text;
    }"""
)

/**
 * 原实现按 parent 缓存视图：第一次遇到 parent 时 build()，
 * 之后直接从 v[i] 复用。convert 参数会被局部覆盖，不写回调用方。
 */
private fun eaGetView(k: dynamic, v: dynamic, pos: dynamic, convertArg: dynamic, parent: dynamic): dynamic {
    var convert = convertArg
    try {
        val i = eaIndexOf(k, parent)
        if (i < 0) {
            k.push(parent)
            convert = eaBuildJs()
            v.push(convert)
        } else {
            convert = v[i]
        }
        // 原实现这里调用 resize(convert, parent) 的语句被注释掉了，故不启用。
        return convert
    } catch (e: dynamic) {
        return eaErrorViewJs(e)
    }
}

fun installEmptyAdapter() {
    val k = js("[]")
    val v = js("[]")
    val adapter = js("({})")

    adapter.getCount = { 1 }
    adapter.getItem = { _: dynamic -> null }
    adapter.getItemId = { pos: dynamic -> pos }
    adapter.getItemViewType = { _: dynamic -> 0 }
    adapter.getView = { pos: dynamic, convert: dynamic, parent: dynamic ->
        eaGetView(k, v, pos, convert, parent)
    }
    adapter.getViewTypeCount = { 1 }
    adapter.hasStableIds = { true }
    adapter.isEmpty = { false }
    adapter.areAllItemsEnabled = { true }
    adapter.isEnabled = { _: dynamic -> true }
    adapter.registerDataSetObserver = { _: dynamic -> js("undefined") }
    adapter.unregisterDataSetObserver = { _: dynamic -> js("undefined") }

    val instance = eaNewListAdapterJs(adapter)
    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("EmptyAdapter", instance)
    }
}
