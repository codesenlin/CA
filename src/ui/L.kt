package ca.kotlin

/**
 * modules/uiCore/L.js 的 Kotlin/JS 版本。
 *
 * L 是一个按 context 构建的布局 DSL：
 *  - LHolder / LValue / LTemplate 都是普通对象（用 Object.create 当原型）；
 *  - 方法全部依赖动态 this，因此统一用 thisJs wrapper 把真实 this 传回 Kotlin；
 *  - 返回值是 Rhino 的 Scriptable 代理，保留 __noSuchMethod__ 与按需反射取类；
 *  - 每个 withContext 会创建一份独立的缓存/listener/原型，和原 IIFE 一致。
 *
 * 原文件里 calculateLayoutParams 的 layout 分支引用了不存在的全局 view，
 * 这里保留同样的自由变量查找（lCallLayoutJs 里的 view），不额外修正行为。
 */
private val lTruthyJs: dynamic = js("function (v) { return !!v; }")
private val lStringJs: dynamic = js("function (v) { return String(v); }")
private val lLengthJs: dynamic = js("function (v) { return v.length | 0; }")
private fun lLength(v: dynamic): Int = lLengthJs(v)
private val lTypeofJs: dynamic = js("function (v) { return typeof v; }")
private val lInJs: dynamic = js("function (k, o) { return k in o; }")
private val lDeleteJs: dynamic = js("function (o, k) { delete o[k]; }")
private val lForInJs: dynamic = js("function (o, f) { for (var k in o) f(k); }")
private val lThisJs: dynamic = js("function (f) { return function () { return f(this, arguments); }; }")
private val lApplyJs: dynamic = js("function (f, t, a) { return f.apply(t, a); }")
private val lCall0Js: dynamic = js("function (f, t) { return f.call(t); }")
private val lCall1Js: dynamic = js("function (f, t, a) { return f.call(t, a); }")
private val lBind0Js: dynamic = js("function (f, t) { return f.bind(t); }")
private val lBind1Js: dynamic = js("function (f, t, a) { return f.bind(t, a); }")
private val lIsArrayJs: dynamic = js("function (v) { return Array.isArray(v); }")
private val lIsArrayInstanceJs: dynamic = js("function (v) { return v instanceof Array; }")
private val lIsObjectJs: dynamic = js("function (v) { return v instanceof Object; }")
private val lIsFunctionJs: dynamic = js("function (v) { return v instanceof Function; }")
private val lIsUndefinedJs: dynamic = js("function (v) { return typeof v == 'undefined'; }")
private val lLooseEqJs: dynamic = js("function (a, b) { return a == b; }")
private val lInstanceOfJs: dynamic = js("function (v, c) { return v instanceof c; }")
private val lIsPrototypeOfJs: dynamic = js("function (p, o) { return p.isPrototypeOf(o); }")
private val lNewErrorJs: dynamic = js("function (m) { return new Error(m); }")
private val lThrowJs: dynamic = js("function (e) { throw e; }")
private val lSliceJs: dynamic = js("function (s, a, b) { return s.slice(a, b); }")
private val lJoinJs: dynamic = js("function (a, s) { return a.join(s); }")
private val lSplitJs: dynamic = js("function (s, d) { return String(s).split(d); }")
private val lUpperJs: dynamic = js("function (s) { return s.toUpperCase(); }")
private val lConcatJs: dynamic = js("function (a, b) { return a + b; }")
private val lOrJs: dynamic = js("function (a, b) { return a || b; }")
private val lBitOrJs: dynamic = js("function (a, b) { return a | b; }")
private val lIsNullishJs: dynamic = js("function (v) { return v == null; }")
private val lObjectKeysJs: dynamic = js("function (o) { return Object.keys(o); }")
private val lObjectCreateJs: dynamic = js("function (p) { return Object.create(p); }")
private val lUccJs: dynamic = js("function (s) { return s.slice(0, 1).toUpperCase() + s.slice(1); }")
private val lLccJs: dynamic = js("function (s) { return s.slice(0, 1).toLowerCase() + s.slice(1); }")
private val lMakeKeyJs: dynamic = js(
    "function (a, b, c) { return a + '|' + b + '|' + c; }"
)

// Rhino / Java 互操作
private val lContextJs: dynamic = js(
    "function () { return org.mozilla.javascript.Context.getCurrentContext(); }"
)
private val lForNameJs: dynamic = js("function (n) { return java.lang.Class.forName(n); }")
private val lNewScriptableJs: dynamic = js(
    "function (cb) { return new org.mozilla.javascript.Scriptable(cb); }"
)
private val lToObjectJs: dynamic = js(
    "function (cx, s, scope) { return cx.toObject(s, scope); }"
)
private val lWrapJavaClassJs: dynamic = js(
    "function (cx, scope, c) { return cx.getWrapFactory().wrapJavaClass(cx, scope, c); }"
)
// 原实现的 layout 函数分支读取的是全局 view；保留这个自由变量查找。
private val lCallLayoutJs: dynamic = js(
    "function (layout, parent, modelContext) { return layout(parent, view, modelContext.data); }"
)

/** 每个 L context 独立持有的缓存、listener 和原型对象。 */
private class LState(
    val cx: dynamic,
    val scope: dynamic,
    val defaultContext: dynamic,
    val baseClass: dynamic,
    val groupClass: dynamic,
    val layoutParamsClass: dynamic
) {
    val declaredMethodCache: dynamic = js("({})")
    val constantCache: dynamic = js("({})")
    val listener: dynamic = js("EventSender").init(js("({ listener: {} })"))
    val pprefix: dynamic = js(
        "['android.widget.', 'android.view.', 'android.view.animation', 'android.animation.'," +
            " 'android.app.', 'android.content.', 'android.graphics.', 'android.graphics.drawable.'," +
            " 'android.media.', 'android.os.', 'android.text.', 'android.text.format.'," +
            " 'android.text.method.', 'android.text.style.', 'android.view.inputmethod.'," +
            " 'android.webkit.']"
    )
    var holder: dynamic = js("null")
    var value: dynamic = js("null")
    var template: dynamic = js("null")
    var kv: dynamic = js("null")
}

private fun lArgs(vararg items: dynamic): dynamic {
    val a = js("[]")
    var i = 0
    while (i < items.size) {
        a.push(items[i])
        i++
    }
    return a
}

private fun lCreateHolder(state: LState, data: dynamic, parent: dynamic): dynamic {
    val o = lObjectCreateJs(state.holder)
    o.data = data
    o.parent = parent
    return o
}

// ---------------- LHolder ----------------

private fun lHolderGet(self: dynamic, name: dynamic): dynamic {
    if (lInJs(name, self.data)) return self.data[name]
    if (lTruthyJs(self.parent)) return self.parent.get(name)
    return js("undefined")
}

private fun lHolderGetAsTopLevel(self: dynamic, name: dynamic): dynamic = self.data[name]

private fun lHolderGetData(self: dynamic): dynamic = self.data

private fun lHolderGetChildren(self: dynamic): dynamic {
    var p = self
    val r = js("[]")
    while (lTruthyJs(p)) {
        if (lTruthyJs(p.data.children)) {
            val a = p.data.children
            lForInJs(a) { i: dynamic -> r.push(a[i]) }
        } else if (lTruthyJs(p.data.child)) {
            r.push(p.data.child)
        }
        p = p.parent
    }
    return r
}

private fun lHolderFlatten(self: dynamic, targetIn: dynamic): dynamic {
    var target = targetIn
    if (!lTruthyJs(target)) target = js("({})")
    if (lTruthyJs(self.parent)) self.parent.flatten(target)
    lForInJs(self.data) { i: dynamic -> target[i] = self.data[i] }
    return target
}

// ---------------- LValue ----------------

private fun lValueGet(self: dynamic, modelContext: dynamic): dynamic {
    if (lTruthyJs(self.getter)) return self.getter(modelContext.data)
    if (lTruthyJs(self.field)) return modelContext.data[self.field]
    return modelContext.data
}

private fun lValueFill(state: LState, self: dynamic, obj: dynamic, modelContext: dynamic): dynamic {
    if (!lTruthyJs(self.isAbstract(obj))) return obj
    if (lIsPrototypeOfJs(state.value, obj)) return obj.get(modelContext)
    if (lIsArrayInstanceJs(obj)) {
        val r = obj.slice()
        var i = 0
        val n = lLength(obj)
        while (i < n) {
            r[i] = self.fill(obj[i], modelContext)
            i++
        }
        return r
    } else if (lIsObjectJs(obj)) {
        val r = js("({})")
        lForInJs(obj) { i: dynamic -> r[i] = self.fill(obj[i], modelContext) }
        return r
    }
    return obj
}

private fun lIsAbstract(state: LState, obj: dynamic): Boolean {
    if (lIsPrototypeOfJs(state.value, obj)) return true
    if (lIsObjectJs(obj)) {
        var found = false
        lForInJs(obj) { i: dynamic ->
            if (!found && lIsAbstract(state, obj[i])) found = true
        }
        return found
    }
    return false
}

// ---------------- 通用属性/布局 ----------------

private fun lApplyAttributes(
    state: LState,
    source: dynamic,
    target: dynamic,
    modelContext: dynamic,
    ignoreLayout: Boolean
) {
    lForInJs(source) { i: dynamic ->
        if (!(ignoreLayout && lSliceJs(i, 0, 6) == "layout")) {
            var e = source[i]
            var skip = false
            if (lTruthyJs(modelContext) && lTruthyJs(modelContext.data)) {
                e = state.value.fill(e, modelContext)
            } else if (lTruthyJs(state.value.isAbstract(e))) {
                skip = true
            }
            if (!skip) {
                val t = lConcatJs("set", lUccJs(i))
                if (lInJs(t, target)) {
                    if (lIsArrayJs(e)) {
                        lApplyJs(target[t], target, e)
                    } else {
                        target[t](e)
                    }
                } else if (lInJs(i, target) && lTypeofJs(target[i]) != "function") {
                    target[i] = e
                }
            }
        }
    }
}

private fun lApplyListeners(state: LState, source: dynamic, target: dynamic, modelContext: dynamic) {
    lForInJs(source) { i: dynamic ->
        var e = source[i]
        var skip = false
        if (lTruthyJs(modelContext) && lTruthyJs(modelContext.data)) {
            e = state.value.fill(e, modelContext)
        } else if (lTruthyJs(state.value.isAbstract(e))) {
            skip = true
        }
        if (!skip) {
            val ty = lTypeofJs(e)
            if (ty == "object" || ty == "function") {
                val t = lConcatJs(lConcatJs("set", lUccJs(i)), "Listener")
                if (lInJs(t, target)) target[t](e)
            }
        }
    }
}

private fun lAttachProperties(state: LState, view: dynamic, json: dynamic, modelContext: dynamic) {
    lApplyAttributes(state, json, view, modelContext, true)
    lApplyListeners(state, json, view, modelContext)
}

private fun lClassKeyOf(cls: dynamic): dynamic {
    try {
        if (lTruthyJs(cls) && lTruthyJs(cls.__name)) return cls.__name
    } catch (e: dynamic) {
        // Rhino 的 NativeJavaClass 没有 __name，访问会抛异常
    }
    return lStringJs(cls)
}

private fun lFindDeclaredMethod(
    state: LState,
    cls: dynamic,
    params: dynamic,
    parent: dynamic
): dynamic {
    val key = lMakeKeyJs(
        lClassKeyOf(cls),
        lJoinJs(params, ","),
        if (lTruthyJs(parent)) lClassKeyOf(parent) else ""
    )
    if (lInJs(key, state.declaredMethodCache)) return state.declaredMethodCache[key]
    val result = lFindDeclaredMethod0(state, cls, params, parent)
    state.declaredMethodCache[key] = result
    return result
}

private fun lFindDeclaredMethod0(
    state: LState,
    cls: dynamic,
    params: dynamic,
    parent: dynamic
): dynamic {
    try {
        return lApplyJs(cls.getDeclaredMethod, cls, params)
    } catch (e: dynamic) {
        // class not found
    }
    var pr = parent
    if (!lTruthyJs(pr)) pr = js("java.lang.Object")
    if (lLooseEqJs(cls, js("java.lang.Object")) || lLooseEqJs(cls, pr)) return js("null")
    return lFindDeclaredMethod(state, cls.getSuperclass(), params, pr)
}

private fun lGenerateDefaultLayoutParams(state: LState, parent: dynamic): dynamic {
    val method = lFindDeclaredMethod(
        state,
        parent.getClass(),
        lArgs("generateDefaultLayoutParams"),
        state.groupClass
    )
    method.setAccessible(true)
    return method.invoke(parent)
}

private fun lGenerateLayoutParams(state: LState, parent: dynamic, oldLp: dynamic): dynamic {
    val cls = parent.getClass()
    val checkMethod = lFindDeclaredMethod(
        state,
        cls,
        lArgs("checkLayoutParams", state.layoutParamsClass),
        state.groupClass
    )
    val generateMethod = lFindDeclaredMethod(
        state,
        cls,
        lArgs("generateLayoutParams", state.layoutParamsClass),
        state.groupClass
    )
    checkMethod.setAccessible(true)
    generateMethod.setAccessible(true)
    return if (lTruthyJs(checkMethod.invoke(parent, oldLp))) {
        oldLp
    } else {
        generateMethod.invoke(parent, oldLp)
    }
}

private fun lAttachLayoutParams(
    state: LState,
    lp: dynamic,
    json: dynamic,
    modelContext: dynamic
) {
    if (lTruthyJs(json.layout)) {
        lApplyAttributes(state, json.layout, lp, modelContext, false)
    } else {
        val attrs = js("({})")
        lForInJs(json) { i: dynamic ->
            if (lSliceJs(i, 0, 6) == "layout") {
                attrs[lLccJs(lSliceJs(i, 6))] = json[i]
            }
        }
        lApplyAttributes(state, attrs, lp, modelContext, false)
    }
}

private fun lCalculateLayoutParams(
    state: LState,
    parent: dynamic,
    json: dynamic,
    modelContext: dynamic,
    oldLp: dynamic
): dynamic {
    if (lTruthyJs(json.layoutParams)) return json.layoutParams
    val lp: dynamic
    if (lIsFunctionJs(json.layout)) {
        lp = lCallLayoutJs(json.layout, parent, modelContext)
    } else {
        lp = if (lTruthyJs(oldLp)) {
            lGenerateLayoutParams(state, parent, oldLp)
        } else {
            lGenerateDefaultLayoutParams(state, parent)
        }
        lAttachLayoutParams(state, lp, json, modelContext)
    }
    return lp
}

// ---------------- attach / inflate ----------------

private fun lAttachChild(state: LState, view: dynamic, child: dynamic, modelContext: dynamic) {
    var e = child
    val lp: dynamic
    if (lTruthyJs(modelContext)) {
        lp = lCalculateLayoutParams(state, view, e, js("null"), js("undefined"))
        e = lFromJSON(state, e, view.getContext(), modelContext, js("undefined"))
    } else if (lIsPrototypeOfJs(state.holder, e.tag)) {
        lp = lCalculateLayoutParams(state, view, e.tag.flatten(), e.layoutParams, js("undefined"))
    } else {
        lp = e.layoutParams
    }
    view.addView(e, lp)
}

private fun lAttach(
    state: LState,
    view: dynamic,
    json: dynamic,
    modelContext: dynamic,
    rootView: dynamic
): dynamic {
    val parentJson = view.tag
    view.tag = lCreateHolder(
        state,
        json,
        if (lIsPrototypeOfJs(state.holder, parentJson)) parentJson else js("null")
    )
    state.listener.trigger("beforeAttach", view, view.tag)
    lAttachProperties(state, view, json, modelContext)
    if (lTruthyJs(rootView)) {
        view.setLayoutParams(
            lCalculateLayoutParams(state, rootView, json, view.getLayoutParams(), js("undefined"))
        )
    }
    if (state.groupClass.isAssignableFrom(view.getClass())) {
        if (lTruthyJs(json.children)) {
            lForInJs(json.children) { i: dynamic ->
                lAttachChild(state, view, json.children[i], modelContext)
            }
        } else if (lTruthyJs(json.child)) {
            lAttachChild(state, view, json.child, modelContext)
        }
    }
    if (lTruthyJs(json.inflate)) json.inflate(view)
    state.listener.trigger("afterAttach", view, view.tag)
    return view
}

private fun lConstructView(state: LState, clazz: dynamic, context: dynamic): dynamic {
    if (!state.baseClass.isAssignableFrom(clazz)) {
        js("Log").throwError(lNewErrorJs(lConcatJs(clazz, " is not a view class")))
    }
    var constructor: dynamic = js("null")
    try {
        constructor = clazz.getConstructor(js("android.content.Context"))
    } catch (e: dynamic) {
        // constructor not found
    }
    if (!lTruthyJs(constructor)) {
        js("Log").throwError(lNewErrorJs(lConcatJs("Unable to construct ", clazz)))
    }
    return constructor.newInstance(context)
}

private fun lInflate(
    state: LState,
    clazz: dynamic,
    context: dynamic,
    json: dynamic,
    modelContext: dynamic,
    rootView: dynamic
): dynamic = lAttach(state, lConstructView(state, clazz, context), json, modelContext, rootView)

// ---------------- constants ----------------

private fun lFindConstant(state: LState, cls: dynamic, name: dynamic): dynamic {
    val key = lConcatJs(lConcatJs(lClassKeyOf(cls), "|"), name)
    if (lInJs(key, state.constantCache)) return state.constantCache[key]
    var value: dynamic = js("undefined")
    try {
        val field = cls.getField(name)
        if (lTruthyJs(field)) value = field.get(js("null"))
    } catch (e: dynamic) {
        // field not found or not static
    }
    state.constantCache[key] = value
    return value
}

private fun lCalculateConstant(state: LState, clazz: dynamic, exp: dynamic): dynamic {
    val parts = lSplitJs(exp, "|")
    val values = js("[]")
    var i = 0
    val n = lLength(parts)
    while (i < n) {
        val e = parts[i]
        values.push(
            lOrJs(
                lFindConstant(state, clazz, lUpperJs(e)),
                lFindConstant(state, clazz, e)
            )
        )
        i++
    }
    var r = values[0]
    var j = 1
    while (j < n) {
        if (lIsNullishJs(r)) r = 0
        if (lTypeofJs(values[j]) == "number") {
            r = lBitOrJs(r, values[j])
        } else {
            r = lOrJs(r, values[j])
        }
        j++
    }
    return lOrJs(r, 0)
}

// ---------------- fromJSON ----------------

private fun lFromJSON(
    state: LState,
    jsonIn: dynamic,
    context: dynamic,
    modelContext: dynamic,
    rootView: dynamic
): dynamic {
    var json = jsonIn
    if (lIsPrototypeOfJs(state.value, json)) json = json.get(modelContext)
    var clazz = json._class
    if (lTypeofJs(clazz) == "string") clazz = lForNameJs(clazz)
    val view = lInflate(state, clazz, context, json, modelContext, rootView)
    if (lTruthyJs(modelContext.holder) && lInJs("_holderId", json)) {
        modelContext.holder[json._holderId] = view
    }
    return view
}

// ---------------- LTemplate ----------------

private fun lTemplateInit(state: LState, self: dynamic, baseView: dynamic): dynamic {
    self.context = baseView.getContext()
    self.views = js("[]")
    self.jsons = js("[]")
    self.valueData = js("({})")
    self.srcJson = self.viewToJson(baseView)
    self.jsons.forEach({ e: dynamic ->
        val k = self.analyzeJson(e)
        if (lTruthyJs(k)) self.valueData[e._holderId] = k
    })
    return js("undefined")
}

private fun lTemplateCreate(
    state: LState,
    self: dynamic,
    holderIn: dynamic,
    rootView: dynamic
): dynamic {
    var holder = holderIn
    if (!lTruthyJs(holder)) holder = js("({})")
    val lHolder = js("({})")
    holder._lHolder = lHolder
    val mc = js("({})")
    mc.holder = lHolder
    val view = lFromJSON(state, self.srcJson, self.context, mc, js("undefined"))
    holder._lTag = view.tag
    holder._lRoot = view
    view.tag = holder
    return view
}

private fun lTemplateBind(
    state: LState,
    self: dynamic,
    viewOrHolder: dynamic,
    data: dynamic,
    rootView: dynamic
): dynamic {
    var holder = viewOrHolder
    if (lInstanceOfJs(viewOrHolder, js("android.view.View"))) holder = viewOrHolder.tag

    val mc = js("({})")
    mc.data = data
    val vholder = holder._lHolder
    lForInJs(self.valueData) { i: dynamic ->
        var filledJson = state.value.fill(self.valueData[i], mc)
        val holderTag = vholder[i].tag
        val parentHolder = if (lIsPrototypeOfJs(state.holder, holderTag)) {
            holderTag
        } else if (lLooseEqJs(holder._lRoot, vholder[i])) {
            holder._lTag
        } else {
            lCreateHolder(state, self.jsons[i], js("null"))
        }
        val filledHolder = lCreateHolder(state, filledJson, parentHolder)
        state.listener.trigger("beforeAttach", vholder[i], filledHolder)
        lAttachProperties(state, vholder[i], filledJson, js("undefined"))
        filledJson = filledHolder.flatten()
        if (lTruthyJs(vholder[i].layoutParams)) {
            lAttachLayoutParams(state, vholder[i].layoutParams, filledJson, js("undefined"))
        } else if (lTruthyJs(rootView)) {
            vholder[i].layoutParams =
                lCalculateLayoutParams(state, rootView, filledJson, mc, js("undefined"))
        }
        state.listener.trigger("afterAttach", vholder[i], filledHolder)
    }
    return viewOrHolder
}

private fun lTemplateMakeView(
    state: LState,
    self: dynamic,
    data: dynamic,
    rootView: dynamic
): dynamic {
    val holder = js("({})")
    val r = self.create(holder)
    self.bind(holder, data, rootView)
    r.tag = holder._lTag
    return r
}

private fun lTemplateViewToJson(state: LState, self: dynamic, view: dynamic): dynamic {
    if (lIsPrototypeOfJs(state.value, view)) return view
    val holder = view.tag
    if (!lIsPrototypeOfJs(state.holder, holder)) {
        js("Log").throwError(lNewErrorJs(lConcatJs(holder, " is not a LHolder")))
    }
    val json = holder.flatten()
    lDeleteJs(json, "child")
    json.children = holder.getChildren().map({ e: dynamic -> self.viewToJson(e) })
    json._holderId = lLength(self.views)
    json._class = lStringJs(view.getClass().getName())
    self.views.push(view)
    self.jsons.push(json)
    return json
}

private fun lTemplateAnalyzeJson(state: LState, self: dynamic, json: dynamic): dynamic {
    val r = js("({})")
    var hasAbstractKey = false
    val keys = lObjectKeysJs(json)
    var i = 0
    val n = lLength(keys)
    while (i < n) {
        val key = keys[i]
        if (key != "child" && key != "children") {
            val v = json[key]
            if (lTruthyJs(state.value.isAbstract(v))) {
                hasAbstractKey = true
                r[key] = v
                lDeleteJs(json, key)
            }
        }
        i++
    }
    return if (hasAbstractKey) r else js("undefined")
}

// ---------------- LView / 类代理 ----------------

private fun lView(state: LState, clazzIn: dynamic, json: dynamic): dynamic {
    var clazz = clazzIn
    try {
        if (lTypeofJs(clazz) == "string") clazz = lForNameJs(clazz)
        if (lTypeofJs(json) == "function") {
            if (lLooseEqJs(json, state.kv.asClass)) {
                return lWrapJavaClassJs(state.cx, state.scope, clazz)
            }
            return state.kv.Class(clazz, lCall0Js(json, state.kv))
        } else if (lTypeofJs(json) == "object") {
            return lInflate(state, clazz, state.defaultContext, json, js("null"), js("undefined"))
        } else if (lTypeofJs(json) == "string") {
            return lCalculateConstant(state, clazz, json)
        } else if (lIsUndefinedJs(json)) {
            return lInflate(state, clazz, state.defaultContext, js("({})"), js("null"), js("undefined"))
        }
    } catch (e: dynamic) {
        js("Log").e(e)
        lThrowJs(e)
        return js("undefined")
    }
    return js("undefined")
}

private fun lWrapViewClass(state: LState, cls: dynamic): dynamic =
    lBind1Js(state.kv.Class, state.kv, cls)

private fun lPeekClass(state: LState, name: dynamic): dynamic {
    var i = 0
    val n = lLength(state.pprefix)
    while (i < n) {
        try {
            return lForNameJs(lConcatJs(state.pprefix[i], name))
        } catch (e: dynamic) {
            // class not found in this package
        }
        i++
    }
    return js("undefined")
}

// ---------------- Scriptable 代理 ----------------

private fun lBuildProxy(state: LState): dynamic {
    val callbacks = js("({})")
    callbacks["delete"] = { name: dynamic ->
        lDeleteJs(state.kv, name)
        js("undefined")
    }
    callbacks.get = { name: dynamic, start: dynamic ->
        if (lInJs(name, state.kv)) {
            state.kv[name]
        } else {
            var cls: dynamic = js("null")
            state.listener.trigger("pickClass", { v: dynamic ->
                cls = v
                js("undefined")
            })
            if (!lTruthyJs(cls)) cls = lPeekClass(state, name)
            if (!lTruthyJs(cls)) {
                js("undefined")
            } else {
                state.kv[name] = lWrapViewClass(state, cls)
                state.kv[name]
            }
        }
    }
    callbacks.getClassName = { "Proxy_L" }
    callbacks.getDefaultValue = { hint: dynamic -> state.kv }
    callbacks.getIds = { lObjectKeysJs(state.kv) }
    callbacks.getParentScope = { state.scope }
    callbacks.getPrototype = { state.kv }
    callbacks.has = { name: dynamic, start: dynamic -> lInJs(name, state.kv) }
    callbacks.hasInstance = { instance: dynamic -> false }
    callbacks.put = { name: dynamic, start: dynamic, value: dynamic ->
        state.kv[name] = value
        js("undefined")
    }
    callbacks.setParentScope = { scope: dynamic -> js("undefined") }
    callbacks.setPrototype = { prototype: dynamic -> js("undefined") }

    val scriptable = lNewScriptableJs(callbacks)
    return lToObjectJs(state.cx, scriptable, state.scope)
}

// ---------------- 工厂 ----------------

private fun createL(defaultContext: dynamic): dynamic {
    val cx = lContextJs()
    val scope = js("globalThis")
    val state = LState(
        cx,
        scope,
        defaultContext,
        lForNameJs("android.view.View"),
        lForNameJs("android.view.ViewGroup"),
        js("android.view.ViewGroup.LayoutParams")
    )

    val holder = js("({})")
    holder.get = lThisJs { self: dynamic, args: dynamic -> lHolderGet(self, args[0]) }
    holder.getAsTopLevel = lThisJs { self: dynamic, args: dynamic -> lHolderGetAsTopLevel(self, args[0]) }
    holder.getData = lThisJs { self: dynamic, args: dynamic -> lHolderGetData(self) }
    holder.getChildren = lThisJs { self: dynamic, args: dynamic -> lHolderGetChildren(self) }
    holder.flatten = lThisJs { self: dynamic, args: dynamic -> lHolderFlatten(self, args[0]) }
    state.holder = holder

    val value = js("({})")
    value.get = lThisJs { self: dynamic, args: dynamic -> lValueGet(self, args[0]) }
    value.fill = lThisJs { self: dynamic, args: dynamic -> lValueFill(state, self, args[0], args[1]) }
    value.isAbstract = lThisJs { self: dynamic, args: dynamic -> lIsAbstract(state, args[0]) }
    state.value = value

    val template = js("({})")
    template.init = lThisJs { self: dynamic, args: dynamic -> lTemplateInit(state, self, args[0]) }
    template.create = lThisJs { self: dynamic, args: dynamic ->
        lTemplateCreate(state, self, args[0], args[1])
    }
    template.bind = lThisJs { self: dynamic, args: dynamic ->
        lTemplateBind(state, self, args[0], args[1], args[2])
    }
    template.makeView = lThisJs { self: dynamic, args: dynamic ->
        lTemplateMakeView(state, self, args[0], args[1])
    }
    template.viewToJson = lThisJs { self: dynamic, args: dynamic ->
        lTemplateViewToJson(state, self, args[0])
    }
    template.analyzeJson = lThisJs { self: dynamic, args: dynamic ->
        lTemplateAnalyzeJson(state, self, args[0])
    }
    state.template = template

    val kv = js("({})")
    kv.__noSuchMethod__ = { name: dynamic ->
        lThrowJs(lNewErrorJs(lConcatJs(name, " is not a function, it is undefined.")))
        js("undefined")
    }
    kv.attach = { view: dynamic, json: dynamic, modelContext: dynamic, rootView: dynamic ->
        lAttach(state, view, json, modelContext, rootView)
    }
    kv.inflate = { clazz: dynamic, context: dynamic, json: dynamic, modelContext: dynamic, rootView: dynamic ->
        lInflate(state, clazz, context, json, modelContext, rootView)
    }
    kv.Template = { view: dynamic ->
        val o = lObjectCreateJs(template)
        o.init(view)
        o
    }
    kv.Value = { f: dynamic ->
        val o = lObjectCreateJs(value)
        if (lIsFunctionJs(f)) o.getter = f else o.field = f
        o
    }
    kv.on = lBind0Js(state.listener.on, state.listener)
    kv.off = lBind0Js(state.listener.off, state.listener)
    kv.clearListeners = lBind0Js(state.listener.clearListeners, state.listener)
    kv.withContext = { context: dynamic -> createL(context) }
    kv.asClass = { clazz: dynamic ->
        lCall1Js(clazz, js("globalThis"), state.kv.asClass)
    }
    state.kv = kv

    kv.Class = { clazz: dynamic, json: dynamic -> lView(state, clazz, json) }

    return lBuildProxy(state)
}

fun installL() {
    val api = createL(js("ctx"))
    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("L", api)
    }
}
