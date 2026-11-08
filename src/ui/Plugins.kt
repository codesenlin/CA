package ca.kotlin

/**
 * modules/core/Plugins.js 的 Kotlin/JS 版本。
 *
 * 这个模块几乎每个方法都依赖调用时的 this：
 * - Plugins 自身的方法通过 this 访问 modules / observers / FEATURES；
 * - Plugin 原型方法通过 this 访问 _parent / uuid / core。
 *
 * 因此所有入口都用薄 JS wrapper 把真实 this 和 arguments 传回 Kotlin，
 * 同时保持原函数的 length（MapScript 的 hook 判断会看函数参数个数）。
 *
 * inject 只捕获 core 构造阶段的异常（与旧实现一致），emit 的 observer 异常
 * 也照旧先交给 t.onError，再由内层 catch 调用 erp(e, true)。
 */
private val pluginsApi: dynamic = js("({})")
private var pluginsHookMethodFn: dynamic = js("null")

// ---------------- JS 语义 helper ----------------
private val plgTruthyJs: dynamic = js("function (v) { return !!v; }")
private val plgLengthJs: dynamic = js("function (v) { return v.length | 0; }")
private val plgLooseEqJs: dynamic = js("function (a, b) { return a == b; }")
private val plgStrictEqJs: dynamic = js("function (a, b) { return a === b; }")
private val plgInJs: dynamic = js("function (k, o) { return k in o; }")
private val plgForInJs: dynamic = js("function (o, f) { for (var i in o) f(i); }")
private val plgOrJs: dynamic = js("function (a, b) { return a || b; }")
private val plgIsArrayJs: dynamic = js("function (v) { return Array.isArray(v); }")
private val plgIsFunctionJs: dynamic = js("function (v) { return typeof v === 'function'; }")
private val plgApplyJs: dynamic = js("function (f, t, a) { return f.apply(t, a); }")
private val plgNewErrorJs: dynamic = js("function (m) { return new Error(m); }")
private val plgObjectCreateJs: dynamic = js("function (p) { return Object.create(p); }")
private val plgObjectCtorJs: dynamic = js("function (v) { return Object(v); }")
private val plgCallWithSelfJs: dynamic = js("function (f, o) { return f.call(o, o); }")
private val plgInstanceOfFunctionJs: dynamic = js("function (v) { return v instanceof Function; }")

private fun plgTruthy(v: dynamic): Boolean = plgTruthyJs(v)
private fun plgLength(v: dynamic): Int = plgLengthJs(v)
private fun plgLooseEq(a: dynamic, b: dynamic): Boolean = plgLooseEqJs(a, b)
private fun plgStrictEq(a: dynamic, b: dynamic): Boolean = plgStrictEqJs(a, b)
private fun plgIn(key: dynamic, obj: dynamic): Boolean = plgInJs(key, obj)
private fun plgForIn(obj: dynamic, f: dynamic) {
    plgForInJs(obj, f)
}
private fun plgOr(a: dynamic, b: dynamic): dynamic = plgOrJs(a, b)
private fun plgIsArray(v: dynamic): Boolean = plgIsArrayJs(v)
private fun plgIsFunction(v: dynamic): Boolean = plgIsFunctionJs(v)
private fun plgApply(f: dynamic, t: dynamic, args: dynamic): dynamic = plgApplyJs(f, t, args)
private fun plgNewError(msg: dynamic): dynamic = plgNewErrorJs(msg)
private fun plgObjectCreate(proto: dynamic): dynamic = plgObjectCreateJs(proto)
private fun plgObjectCtor(v: dynamic): dynamic = plgObjectCtorJs(v)
private fun plgCallWithSelf(f: dynamic, o: dynamic): dynamic = plgCallWithSelfJs(f, o)
private fun plgInstanceOfFunction(v: dynamic): Boolean = plgInstanceOfFunctionJs(v)

private fun plgObject(): dynamic = js("({})")
private fun plgArray1(v: dynamic): dynamic {
    val a = js("[]")
    a.push(v)
    return a
}

private fun plgThrowError(msg: dynamic): dynamic {
    js("Log").throwError(plgNewError(msg))
    return js("undefined")
}

private val plgFeaturesJs: dynamic = js(
    "function () { return [" +
        "\"injectable\"," +
        "\"observable\"," +
        "\"mainMenuAppendable\"," +
        "\"userExpressionMenuAppendable\"," +
        "\"corePlugin\"," +
        "\"hookMethod\"" +
        "]; }"
)

private val plgObserversJs: dynamic = js(
    "function () { return ({" +
        " Plugin: { inject: [] }," +
        " WSServer: { connectionOpen: [], connectionClose: [] }," +
        " Custom: {}" +
        "}); }"
)

private val plgBuildInfoJs: dynamic = js(
    "function (o) {" +
        " return {" +
        "  name: o.name," +
        "  description: o.description," +
        "  author: o.author," +
        "  uuid: o.uuid," +
        "  version: o.version," +
        "  require: o.require," +
        "  update: o.update," +
        "  menu: o.menu," +
        "  noCommand: o.noCommand," +
        "  installer: o.installer === true," +
        "  shortcutName: o.shortcutName" +
        " };" +
        "}"
)

private val plgNewHookWrapperJs: dynamic = js(
    "function (replacement, obj, propName, oldFunc, tag) {" +
        " return function () {" +
        "  return replacement.call(obj, propName, oldFunc, arguments, tag);" +
        " };" +
        "}"
)

private val plgDefineHookPropsJs: dynamic = js(
    "function (fn, helper, replacement, tag) {" +
        " return Object.defineProperties(fn, {" +
        "  \"__hookHelper__\": { value: helper }," +
        "  \"__hookReplacement__\": { value: replacement }," +
        "  \"__hookTag__\": { value: tag }" +
        " });" +
        "}"
)

// ---------------- 动态 this wrapper ----------------
// 参数个数与原函数一致；arguments 原样传给 Kotlin 侧。
private val plgThis0Js: dynamic = js("function (f) { return function () { return f(this, arguments); }; }")
private val plgThis1Js: dynamic = js("function (f) { return function (a) { return f(this, arguments); }; }")
private val plgThis2Js: dynamic = js("function (f) { return function (a, b) { return f(this, arguments); }; }")
private val plgThis3Js: dynamic = js("function (f) { return function (a, b, c) { return f(this, arguments); }; }")
private val plgThis4Js: dynamic = js(
    "function (f) { return function (a, b, c, d) { return f(this, arguments); }; }"
)

// ---------------- Plugin 原型 ----------------
private fun plgPluginGet(self: dynamic): dynamic = self.core

private fun plgPluginObserve(self: dynamic, args: dynamic): dynamic {
    self._parent.registerObserver(self.uuid, args[0], args[1], args[2])
    return js("undefined")
}

private fun plgPluginUnobserve(self: dynamic, args: dynamic): dynamic {
    self._parent.unregisterObserver(self.uuid, args[0], args[1], args[2])
    return js("undefined")
}

private fun plgPluginFeature(self: dynamic, args: dynamic): dynamic {
    plgForIn(args, { i: dynamic ->
        if (self._parent.FEATURES.indexOf(args[i]) < 0) {
            plgThrowError("Require Feature:" + args[i])
        }
        js("undefined")
    })
    return js("undefined")
}

private fun plgPluginRequestLoadAsCore(self: dynamic): dynamic {
    if (plgTruthy(self.corePlugin)) return false
    return CA.Library.enableCoreLibrary(self.path)
}

private fun plgPluginCancelLoadAsCore(self: dynamic): dynamic {
    if (!plgTruthy(self.corePlugin)) return false
    return CA.Library.enableLibrary(self.path)
}

// ---------------- Plugins ----------------
private fun plgInject(self: dynamic, f: dynamic): dynamic {
    val o = plgObjectCreate(self.Plugin)
    o._parent = self
    o.path = CA.Library.currentLoadingLibrary
    o.corePlugin = plgLooseEq(CA.Library.loadingStatus, "core")
    try {
        o.core = if (plgIsFunction(f)) plgCallWithSelf(f, o) else plgObjectCtor(f)
    } catch (e: dynamic) {
        o.error = e
    }
    self.fillInfo(o)
    if (plgIn(o.uuid, self.modules)) {
        return self.modules[o.uuid].info
    }
    self.modules[o.uuid] = o
    self.emit("Plugin", "inject", o.uuid)
    if (plgTruthy(o.init)) o.init(o)
    if (plgTruthy(o.core.init)) o.core.init(o)
    return o.info
}

private fun plgFillInfo(o: dynamic): dynamic {
    if (!plgTruthy(o.core)) o.core = plgObject()
    if (!plgTruthy(o.info)) o.info = plgObject()
    if (!plgTruthy(o.name)) o.name = plgOr(o.core.name, "未知插件")
    if (!plgTruthy(o.description)) o.description = plgOr(o.core.description, "")
    if (!plgTruthy(o.author)) o.author = plgOr(o.core.author, "Anonymous")
    if (!plgTruthy(o.uuid)) o.uuid = plgOr(o.core.uuid, o.author + ":" + o.name)
    if (!plgTruthy(o.update)) o.update = plgOr(o.core.update, "store")
    if (!plgIsArray(o.version)) o.version = plgOr(o.core.version, plgArray1(0))
    if (!plgIsArray(o.require)) o.require = plgOr(o.core.require, js("[]"))
    if (!plgIsArray(o.menu)) o.menu = plgOr(o.core.menu, js("[]"))
    if (plgTruthy(o.error)) {
        val item = plgObject()
        item.text = "查看错误"
        item.onclick = {
            js("erp")(o.error)
            js("undefined")
        }
        o.menu.unshift(item)
        o.name = o.name + "[出错]"
    }
    o.info = plgBuildInfoJs(o)
    if (!plgTruthy(CA.settings.moduleSettings)) CA.settings.moduleSettings = plgObject()
    if (!plgTruthy(CA.settings.moduleSettings[o.uuid])) {
        CA.settings.moduleSettings[o.uuid] = plgObject()
    }
    o.settings = CA.settings.moduleSettings[o.uuid]
    return js("undefined")
}

private fun plgRegisterObserver(self: dynamic, args: dynamic): dynamic {
    val o = self.getObservers(args[1], args[2])
    val item = plgObject()
    item.module = args[0]
    item.observer = args[3]
    o.push(item)
    return js("undefined")
}

private fun plgUnregisterObserver(self: dynamic, args: dynamic): dynamic {
    val module = args[0]
    val type = args[1]
    val target = args[2]
    val f = args[3]
    var o: dynamic = js("null")
    if (plgTruthy(f)) {
        o = self.getObservers(type, target)
        var i = plgLength(o) - 1
        while (i >= 0) {
            if (plgLooseEq(o[i].module, module) && plgLooseEq(o[i].observer, f)) {
                o.splice(i, 1)
            }
            i--
        }
    } else if (plgTruthy(target)) {
        o = self.getObservers(type, target)
        var i = plgLength(o) - 1
        while (i >= 0) {
            if (plgLooseEq(o[i].module, module)) o.splice(i, 1)
            i--
        }
    } else if (plgTruthy(type)) {
        o = self.observers[type]
        if (!plgTruthy(o)) plgThrowError("Invalid event type: " + type)
        plgForIn(o, { i: dynamic ->
            self.unregisterObserver(module, type, i)
            js("undefined")
        })
    } else {
        o = self.observers
        plgForIn(o, { i: dynamic ->
            self.unregisterObserver(module, i)
            js("undefined")
        })
    }
    return js("undefined")
}

private fun plgGetObservers(self: dynamic, args: dynamic): dynamic {
    var o = self.observers
    val type = args[0]
    val target = args[1]
    if (!plgIn(type, o)) plgThrowError("Invalid event type: " + type)
    o = o[type]
    if (!plgTruthy(o[target])) o[target] = js("[]")
    return o[target]
}

private fun plgEmit(self: dynamic, args: dynamic): dynamic {
    val o = self.getObservers(args[0], args[1])
    plgForIn(o, { i: dynamic ->
        val t = self.modules[o[i].module]
        if (!plgTruthy(t)) js("coutinue")
        try {
            plgApply(o[i], t, args)
        } catch (e: dynamic) {
            try {
                if (plgInstanceOfFunction(t.onError)) t.onError(e)
            } catch (e2: dynamic) {
                js("erp")(e2, true)
            }
        }
        js("undefined")
    })
    return js("undefined")
}

private fun plgAddMenu(obj: dynamic): dynamic {
    val a = CA.PluginMenu
    var i = 0
    while (i < plgLength(a)) {
        if (plgLooseEq(a[i].text, obj.text)) {
            a[i] = obj
            return obj
        }
        i++
    }
    a.push(obj)
    return obj
}

private fun plgAddExpressionMenu(obj: dynamic): dynamic {
    val a = CA.PluginExpression
    var i = 0
    while (i < plgLength(a)) {
        if (plgLooseEq(a[i].text, obj.text)) {
            a[i] = obj
            return obj
        }
        i++
    }
    a.push(obj)
    return obj
}

private fun plgHookMethod(args: dynamic): dynamic {
    val obj = args[0]
    val propName = args[1]
    val replacement = args[2]
    val tag = args[3]
    val oldFunc = obj[propName]
    if (!plgIsFunction(oldFunc)) plgThrowError(propName + " is not a method.")
    if (plgStrictEq(oldFunc.__hookHelper__, pluginsHookMethodFn)) {
        plgThrowError(propName + " is already hooked.")
    }
    val wrapper = plgNewHookWrapperJs(replacement, obj, propName, oldFunc, tag)
    val hooked = plgDefineHookPropsJs(wrapper, pluginsHookMethodFn, replacement, tag)
    obj[propName] = hooked
    return hooked
}

fun installPlugins() {
    pluginsApi.FEATURES = plgFeaturesJs()
    pluginsApi.modules = plgObject()
    pluginsApi.observers = plgObserversJs()

    val plugin = plgObject()
    plugin.get = plgThis0Js { self: dynamic, _args: dynamic -> plgPluginGet(self) }
    plugin.observe = plgThis3Js { self: dynamic, args: dynamic -> plgPluginObserve(self, args) }
    plugin.unobserve = plgThis3Js { self: dynamic, args: dynamic -> plgPluginUnobserve(self, args) }
    plugin.feature = plgThis0Js { self: dynamic, args: dynamic -> plgPluginFeature(self, args) }
    plugin.requestLoadAsCore = plgThis0Js { self: dynamic, _args: dynamic ->
        plgPluginRequestLoadAsCore(self)
    }
    plugin.cancelLoadAsCore = plgThis0Js { self: dynamic, _args: dynamic ->
        plgPluginCancelLoadAsCore(self)
    }
    pluginsApi.Plugin = plugin

    pluginsApi.inject = plgThis1Js { self: dynamic, args: dynamic -> plgInject(self, args[0]) }
    pluginsApi.fillInfo = plgThis1Js { _self: dynamic, args: dynamic -> plgFillInfo(args[0]) }
    pluginsApi.registerObserver = plgThis4Js { self: dynamic, args: dynamic ->
        plgRegisterObserver(self, args)
    }
    pluginsApi.unregisterObserver = plgThis4Js { self: dynamic, args: dynamic ->
        plgUnregisterObserver(self, args)
    }
    pluginsApi.getObservers = plgThis2Js { self: dynamic, args: dynamic ->
        plgGetObservers(self, args)
    }
    pluginsApi.emit = plgThis2Js { self: dynamic, args: dynamic -> plgEmit(self, args) }
    pluginsApi.addMenu = plgThis1Js { _self: dynamic, args: dynamic -> plgAddMenu(args[0]) }
    pluginsApi.addExpressionMenu = plgThis1Js { _self: dynamic, args: dynamic ->
        plgAddExpressionMenu(args[0])
    }

    val hookMethod = plgThis4Js { _self: dynamic, args: dynamic -> plgHookMethod(args) }
    pluginsApi.hookMethod = hookMethod
    pluginsHookMethodFn = hookMethod

    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("Plugins", pluginsApi)
    }
}
