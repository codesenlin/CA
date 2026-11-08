package ca.kotlin.core


/**
 * modules/core/EventSender.js 的 Kotlin/JS 版本。
 *
 * 这些方法最终会挂到任意对象/原型上，因此用一层极薄的 JS wrapper
 * 保留动态 this 语义，真正的逻辑仍在 Kotlin 里。
 */
private val eventApi: dynamic = js("({})")
private val eventBridge: dynamic = js("({})")

private fun getListener(receiver: dynamic): dynamic {
    if (receiver.listener === undefined || receiver.listener === null) {
        receiver.listener = js("({})")
    }
    return receiver.listener
}

private fun on(receiver: dynamic, name: dynamic, f: dynamic): dynamic {
    val listener = getListener(receiver)
    if (listener[name] === undefined) listener[name] = js("[]")
    if (listener[name].indexOf(f) < 0) listener[name].push(f)
    if (receiver.__eventsender_observer__) receiver.__eventsender_observer__("on", name, f)
    return receiver
}

private fun off(receiver: dynamic, name: dynamic, argc: Int, f: dynamic): dynamic {
    val listener = getListener(receiver)
    if (listener[name] !== undefined) {
        if (argc == 1) {
            js("delete listener[name]")
        } else {
            val i = listener[name].indexOf(f)
            if (i >= 0) listener[name].splice(i, 1)
        }
    }
    if (receiver.__eventsender_observer__) receiver.__eventsender_observer__("off", name, f)
    return receiver
}

private fun trigger(receiver: dynamic, args: dynamic): dynamic {
    val name = args[0]
    val listener = getListener(receiver)
    if (listener[name] !== undefined) {
        val a = listener[name]
        var i = a.length - 1
        while (i >= 0) {
            a[i].apply(receiver, args)
            i--
        }
    }
    if (receiver.__eventsender_observer__) receiver.__eventsender_observer__("trigger", args)
    return receiver
}

private fun clearListeners(receiver: dynamic) {
    val listener = getListener(receiver)
    val keys = js("Object").keys(listener)
    var i = 0
    while (js("i < keys.length")) {
        js("delete listener[keys[i]]")
        i++
    }
    if (receiver.__eventsender_observer__) receiver.__eventsender_observer__("clear")
}

private fun installWrappers(receiver: dynamic): dynamic {
    receiver.on = js("function(name, f) { return globalThis.__caEventSender.on(this, name, f); }")
    receiver.off = js("function(name, f) { return globalThis.__caEventSender.off(this, name, arguments.length, f); }")
    receiver.trigger = js("function(name) { return globalThis.__caEventSender.trigger(this, arguments); }")
    receiver.clearListeners = js("function() { return globalThis.__caEventSender.clear(this); }")
    return receiver
}

fun installEventSender() {
    eventBridge.on = { receiver: dynamic, name: dynamic, f: dynamic -> on(receiver, name, f) }
    eventBridge.off = { receiver: dynamic, name: dynamic, argc: Int, f: dynamic -> off(receiver, name, argc, f) }
    eventBridge.trigger = { receiver: dynamic, args: dynamic -> trigger(receiver, args) }
    eventBridge.clear = { receiver: dynamic -> clearListeners(receiver) }
    js("globalThis").__caEventSender = eventBridge

    eventApi.init = { o: dynamic -> installWrappers(o) }
    eventApi.on = js("function(name, f) { return globalThis.__caEventSender.on(this, name, f); }")
    eventApi.off = js("function(name, f) { return globalThis.__caEventSender.off(this, name, arguments.length, f); }")
    eventApi.trigger = js("function(name) { return globalThis.__caEventSender.trigger(this, arguments); }")
    eventApi.clearListeners = js("function() { return globalThis.__caEventSender.clear(this); }")

    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("EventSender", eventApi)
    }
}
