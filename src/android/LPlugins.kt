package ca.kotlin.android

import ca.kotlin.core.*


/** modules/uiCore/LPlugins.js 的 Kotlin/JS 版本。 */
private val lPluginsApi: dynamic = js("({})")

fun installLPlugins() {
    lPluginsApi.onCreate = {
        val keys = js("Object").keys(lPluginsApi)
        var i = 0
        val n = keys.length as Int
        while (i < n) {
            val k = keys[i]
            if (k != "onCreate") js("L")[k] = lPluginsApi[k]
            i++
        }
    }

    lPluginsApi.SimpleAdapter = { baseView: dynamic, callback: dynamic, array: dynamic ->
        val template = js("L").Template(baseView)
        val event = js("EventSender").init(js("({ listener: {} })"))
        val adapter = js("SimpleListAdapter")(
            array ?: js("[]"),
            { holder: dynamic ->
                event.trigger("beforeCreate", holder, template)
                val r = template.create(holder)
                event.trigger("afterCreate", holder, r, template)
                r
            },
            { holder: dynamic, e: dynamic, i: dynamic, a: dynamic ->
                event.trigger("beforeBind", holder, e, i, a, template)
                template.bind(holder, e)
                event.trigger("afterBind", holder, e, i, a, template)
            }
        )
        val controller = js("SimpleListAdapter").getController(adapter)
        if (callback != null) callback(controller, template, event)
        adapter
    }

    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("LPlugins", lPluginsApi)
    }
}
