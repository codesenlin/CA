package ca.kotlin.ui

import ca.kotlin.core.*


/**
 * modules/uiCore/UIHelp.js 的 Kotlin/JS 版本。
 *
 * settingItem 只在 G/Common 初始化后调用；bundle 提前注册不影响。
 */
private val uiHelpApi: dynamic = js("({})")

fun installUIHelp() {
    uiHelpApi.settingItem = { holder: dynamic ->
        val view = js("new G.LinearLayout(ctx)")
        view.setOrientation(js("G.LinearLayout.VERTICAL"))
        view.setPadding(15 * js("G.dp") as Int, 10 * js("G.dp") as Int, 15 * js("G.dp") as Int, 10 * js("G.dp") as Int)
        view.setLayoutParams(js("new G.AbsListView.LayoutParams(-1, -2)"))

        val title = js("new G.TextView(ctx)")
        holder.title = title
        title.setGravity(js("G.Gravity.CENTER | G.Gravity.LEFT"))
        title.setLayoutParams(js("new G.LinearLayout.LayoutParams(-1, -2)"))
        js("Common").applyStyle(title, "textview_default", 2)
        view.addView(title)

        val desp = js("new G.TextView(ctx)")
        holder.desp = desp
        desp.setPadding(0, 3 * js("G.dp") as Int, 0, 0)
        desp.setLayoutParams(js("new G.LinearLayout.LayoutParams(-1, -2)"))
        js("Common").applyStyle(desp, "textview_prompt", 1)
        view.addView(desp)
        view
    }

    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("UIHelp", uiHelpApi)
    }
}
