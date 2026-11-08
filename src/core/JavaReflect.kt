package ca.kotlin.core


/**
 * modules/utils/JavaReflect.js 的 Kotlin/JS 版本。
 *
 * 反射返回的函数仍然接受任意数量参数，因此参数适配交给一个很薄的 JS wrapper；
 * Java 类型解析、重载选择、数组构造等逻辑都在 Kotlin 侧。
 */
private val jrApi: dynamic = js("({})")
private val jrBridge: dynamic = js("({})")

private val wrapPrimitive: dynamic = js(
    """function(hints, f) {
        if (!hints.some(function (e) { return e.isPrimitive(); })) return f;
        return function () {
            var args = arguments;
            return f.apply(this, globalThis.__caJavaReflect.prepareArgs(hints, args));
        };
    }"""
)

private val isStringJs: dynamic = js("function (v) { return typeof v === 'string'; }")
private val isClassJs: dynamic = js("function (v) { return v instanceof java.lang.Class; }")
private val hasArrayOfJs: dynamic = js("function (v) { return v.arrayOf != null; }")
private fun isStringType(t: dynamic): Boolean = isStringJs(t)
private fun isClassType(t: dynamic): Boolean = isClassJs(t)

private fun parseClass(type: dynamic): dynamic {
    if (isStringType(type)) {
        val s = type.toString()
        try {
            return js("java.lang.Class").forName(s, true, js("ctx").getClassLoader())
        } catch (e: dynamic) { }
        if (s == "string") return js("java.lang.Class").forName("java.lang.String")
        if (s == "boolean") return js("java.lang.Boolean").TYPE
        if (s == "byte") return js("java.lang.Byte").TYPE
        if (s == "char") return js("java.lang.Character").TYPE
        if (s == "double") return js("java.lang.Double").TYPE
        if (s == "float") return js("java.lang.Float").TYPE
        if (s == "int") return js("java.lang.Integer").TYPE
        if (s == "long") return js("java.lang.Long").TYPE
        if (s == "short") return js("java.lang.Short").TYPE
        if (s == "void") return js("java.lang.Void").TYPE
        if (s.length >= 2 && s.substring(s.length - 2) == "[]") return parseArrayDefinition(s)
        if (!s.contains("$")) return guessSubclass(s)
        throw js("new Error")("Unable to parse \"" + s + "\" to class")
    }
    if (isClassType(type)) return type
    if (hasArrayOfJs(type)) return arrayClass(type.arrayOf, type.dimensions)
    return type.getClass()
}

private fun parseArrayDefinition(str: String): dynamic {
    val start = str.indexOf("[]")
    var current = start
    var dimensions = 0
    while (current < str.length) {
        if (str.substring(current, current + 2) != "[]") throw js("new Error")("Not an array definition")
        dimensions++
        current += 2
    }
    return arrayClass(str.substring(0, start), dimensions)
}

private fun guessSubclass(className: String): dynamic {
    val parts = className.split(".")
    for (i in parts.size - 1 downTo 1) {
        try {
            return js("java.lang.Class").forName(parts.subList(0, i).joinToString(".") + "$" + parts.subList(i, parts.size).joinToString("$"), true, js("ctx").getClassLoader())
        } catch (e: dynamic) { }
    }
    throw js("new Error")("Unable to parse \"" + className + "\" to class")
}

private fun arrayClass(arrayOf: dynamic, dimensions: dynamic): dynamic {
    var dims = js("parseInt")(dimensions) as Int
    if (dims <= 0) dims = 1
    val comp = parseClass(arrayOf)
    var str = "["
    var i = 1
    while (i < dims) {
        str += "["
        i++
    }
    if (comp.isPrimitive()) {
        when {
            comp.equals(js("java.lang.Boolean").TYPE) -> str += "Z"
            comp.equals(js("java.lang.Byte").TYPE) -> str += "B"
            comp.equals(js("java.lang.Character").TYPE) -> str += "C"
            comp.equals(js("java.lang.Double").TYPE) -> str += "D"
            comp.equals(js("java.lang.Float").TYPE) -> str += "F"
            comp.equals(js("java.lang.Integer").TYPE) -> str += "I"
            comp.equals(js("java.lang.Long").TYPE) -> str += "J"
            comp.equals(js("java.lang.Short").TYPE) -> str += "S"
            else -> throw js("new Error")("Component type cannot be void")
        }
    } else if (comp.isArray()) {
        str += comp.getName()
    } else {
        str += "L" + comp.getName() + ";"
    }
    return js("java.lang.Class").forName(str, true, js("ctx").getClassLoader())
}

private fun getPrimitiveWrapper(clazz: dynamic): dynamic {
    if (!clazz.isPrimitive()) return js("null")
    return when {
        clazz.equals(js("java.lang.Boolean").TYPE) -> js("java.lang.Boolean").valueOf
        clazz.equals(js("java.lang.Byte").TYPE) -> js("java.lang.Byte").valueOf
        clazz.equals(js("java.lang.Character").TYPE) -> js("java.lang.Character").valueOf
        clazz.equals(js("java.lang.Double").TYPE) -> js("java.lang.Double").valueOf
        clazz.equals(js("java.lang.Float").TYPE) -> js("java.lang.Float").valueOf
        clazz.equals(js("java.lang.Integer").TYPE) -> js("java.lang.Integer").valueOf
        clazz.equals(js("java.lang.Long").TYPE) -> js("java.lang.Long").valueOf
        clazz.equals(js("java.lang.Short").TYPE) -> js("java.lang.Short").valueOf
        else -> js("null")
    }
}

private fun makeInvokeArgs(clazz: dynamic, argTypes: dynamic): dynamic {
    val invokeArgs = js("[]")
    var i = 0
    val n = argTypes.length as Int
    while (i < n) {
        invokeArgs.push(parseClass(argTypes[i]))
        i++
    }
    return invokeArgs
}

private fun makeConstructor(clazz: dynamic, argTypes: dynamic, declared: Boolean): dynamic {
    val type = parseClass(clazz)
    val invokeArgs = makeInvokeArgs(type, argTypes)
    val ctor = if (declared) type.getDeclaredConstructor(invokeArgs) else type.getConstructor(invokeArgs)
    if (declared) ctor.setAccessible(true)
    return wrapPrimitive(invokeArgs, ctor.newInstance.bind(ctor))
}

private fun makeMethod(clazz: dynamic, methodName: dynamic, argTypes: dynamic, declared: Boolean, static: Boolean): dynamic {
    val type = parseClass(clazz)
    val invokeArgs = makeInvokeArgs(type, argTypes)
    val method = when {
        declared -> type.getDeclaredMethod(methodName.toString(), invokeArgs)
        else -> type.getMethod(methodName.toString(), invokeArgs)
    }
    if (declared) method.setAccessible(true)
    if (static && !js("java.lang.reflect.Modifier").isStatic(method.getModifiers())) throw js("new Error")("Method is not static")
    val f = if (static) method.invoke.bind(method, null) else method.invoke.bind(method)
    val hints = js("[]")
    if (!static) hints.push(type)
    var i = 0
    val n = invokeArgs.length as Int
    while (i < n) {
        hints.push(invokeArgs[i])
        i++
    }
    return wrapPrimitive(hints, f)
}

private fun makeField(clazz: dynamic, fieldName: dynamic, declared: Boolean): dynamic {
    val type = parseClass(clazz)
    val field = (if (declared) type.getDeclaredField(fieldName.toString()) else type.getField(fieldName.toString()))
    if (declared) field.setAccessible(true)
    val r = js("({})")
    r.get = js("function(field) { return function () { return field.get(); }; }")(field)
    val setHints = js("[]")
    setHints.push(field.getType())
    val rawSet = js("function(field) { return function () { field.set(arguments[0]); }; }")(field)
    r.set = wrapPrimitive(setHints, rawSet)
    return r
}

fun installJavaReflect() {
    jrBridge.prepareArgs = { hints: dynamic, args: dynamic ->
        val out = js("[]")
        val n = hints.length as Int
        var i = 0
        while (i < n) {
            val hint = hints[i]
            val wrapper = getPrimitiveWrapper(hint)
            out.push(if (wrapper != js("null")) wrapper(args[i]) else args[i])
            i++
        }
        out
    }

    jrApi.constructor = { clazz: dynamic, argTypes: dynamic -> makeConstructor(clazz, argTypes, false) }
    jrApi.declaredConstructor = { clazz: dynamic, argTypes: dynamic -> makeConstructor(clazz, argTypes, true) }
    jrApi.method = { clazz: dynamic, methodName: dynamic, argTypes: dynamic -> makeMethod(clazz, methodName, argTypes, false, false) }
    jrApi.declaredMethod = { clazz: dynamic, methodName: dynamic, argTypes: dynamic -> makeMethod(clazz, methodName, argTypes, true, false) }
    jrApi.staticMethod = { clazz: dynamic, methodName: dynamic, argTypes: dynamic -> makeMethod(clazz, methodName, argTypes, false, true) }
    jrApi.declaredStaticMethod = { clazz: dynamic, methodName: dynamic, argTypes: dynamic -> makeMethod(clazz, methodName, argTypes, true, true) }
    jrApi.field = { clazz: dynamic, fieldName: dynamic -> makeField(clazz, fieldName, false) }
    jrApi.declaredField = { clazz: dynamic, fieldName: dynamic -> makeField(clazz, fieldName, true) }
    jrApi.parseClass = { type: dynamic -> parseClass(type) }
    jrApi.parseArrayDefinition = { str: dynamic -> parseArrayDefinition(str.toString()) }
    jrApi.guessSubclass = { name: dynamic -> guessSubclass(name.toString()) }
    jrApi.arrayClass = { arrayOf: dynamic, dimensions: dynamic -> arrayClass(arrayOf, dimensions) }
    jrApi.getPrimitiveWrapper = { clazz: dynamic -> getPrimitiveWrapper(clazz) }
    jrApi.toPrimitiveAcceptable = { hints: dynamic, f: dynamic -> wrapPrimitive(hints, f) }
    jrApi.array = js("function(arrayOf) { var args = Array.prototype.slice.call(arguments, 1); return globalThis.__caJavaReflect.array(arrayOf, args); }")
    jrBridge.array = { arrayOf: dynamic, args: dynamic ->
        val c = parseClass(arrayOf)
        val all = js("[]")
        all.push(c)
        var i = 0
        val n = args.length as Int
        while (i < n) {
            all.push(args[i])
            i++
        }
        js("java.lang.reflect.Array").newInstance.apply(null, all)
    }

    js("globalThis").__caJavaReflect = jrBridge
    if (js("typeof MapScript !== 'undefined'")) MapScript.loadModule("JavaReflect", jrApi)
}
