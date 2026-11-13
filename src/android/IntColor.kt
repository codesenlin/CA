package ca.kotlin.android

import ca.kotlin.core.*


/**
 * modules/utils/IntColor.js 的 Kotlin/JS 版本。
 *
 * Java 重载必须按签名精确选择：Paint.setColor(int) 和 setColor(long)
 * 在 Rhino 里用动态数字调用容易落到 long 分支，导致 ColorSpace 越界。
 * 因此这里仍使用 JavaReflect 的显式签名查找；等 JavaReflect 迁移后再换桥。
 */
private val intColorApi: dynamic = js("({})")
private fun argTypes(vararg types: String): dynamic {
    val out = js("[]")
    var i = 0
    while (i < types.size) {
        out.push(types[i])
        i++
    }
    return out
}

fun installIntColor() {
    try {
        val color = js("({})")
        color.alpha = JavaReflect.staticMethod("android.graphics.Color", "alpha", argTypes("int"))
        color.argb = JavaReflect.staticMethod("android.graphics.Color", "argb", argTypes("int", "int", "int", "int"))
        color.blue = JavaReflect.staticMethod("android.graphics.Color", "blue", argTypes("int"))
        color.green = JavaReflect.staticMethod("android.graphics.Color", "green", argTypes("int"))
        color.red = JavaReflect.staticMethod("android.graphics.Color", "red", argTypes("int"))
        color.rgb = JavaReflect.staticMethod("android.graphics.Color", "rgb", argTypes("int", "int", "int"))
        intColorApi.Color = color

        val canvas = js("({})")
        canvas.drawColor = JavaReflect.method("android.graphics.Canvas", "drawColor", argTypes("int"))
        intColorApi.Canvas = canvas

        val bitmap = js("({})")
        bitmap.eraseColor = JavaReflect.method("android.graphics.Bitmap", "eraseColor", argTypes("int"))
        intColorApi.Bitmap = bitmap

        val paint = js("({})")
        paint.setColor = JavaReflect.method("android.graphics.Paint", "setColor", argTypes("int"))
        paint.setShadowLayer = JavaReflect.method("android.graphics.Paint", "setShadowLayer", argTypes("float", "float", "float", "int"))
        intColorApi.Paint = paint

        val linear = js("({})")
        linear.buildFromArray = JavaReflect.constructor("android.graphics.LinearGradient", argTypes("float", "float", "float", "float", "int[]", "float[]", "android.graphics.Shader.TileMode"))
        linear.buildFromEnds = JavaReflect.constructor("android.graphics.LinearGradient", argTypes("float", "float", "float", "float", "int", "int", "android.graphics.Shader.TileMode"))
        intColorApi.LinearGradient = linear

        val radial = js("({})")
        radial.buildFromArray = JavaReflect.constructor("android.graphics.RadialGradient", argTypes("float", "float", "float", "int[]", "float[]", "android.graphics.Shader.TileMode"))
        radial.buildFromCenterEdge = JavaReflect.constructor("android.graphics.RadialGradient", argTypes("float", "float", "float", "int", "int", "android.graphics.Shader.TileMode"))
        intColorApi.RadialGradient = radial

        val sweep = js("({})")
        sweep.buildFromArray = JavaReflect.constructor("android.graphics.SweepGradient", argTypes("float", "float", "int[]", "float[]"))
        sweep.buildFromEnds = JavaReflect.constructor("android.graphics.SweepGradient", argTypes("float", "float", "int", "int"))
        intColorApi.SweepGradient = sweep

        if (js("ScriptInterface").isExtendedColorApiSupported()) {
            color.alphaLong = JavaReflect.staticMethod("android.graphics.Color", "alpha", argTypes("long"))
            color.argbFloat = JavaReflect.staticMethod("android.graphics.Color", "argb", argTypes("float", "float", "float", "float"))
            color.blueLong = JavaReflect.staticMethod("android.graphics.Color", "blue", argTypes("long"))
            color.greenLong = JavaReflect.staticMethod("android.graphics.Color", "green", argTypes("long"))
            color.redLong = JavaReflect.staticMethod("android.graphics.Color", "red", argTypes("long"))
            color.rgbFloat = JavaReflect.staticMethod("android.graphics.Color", "rgb", argTypes("float", "float", "float"))
            color.valueOf = JavaReflect.staticMethod("android.graphics.Color", "valueOf", argTypes("int"))
            color.valueOfLong = JavaReflect.staticMethod("android.graphics.Color", "valueOf", argTypes("long"))
        }
        if (js("ScriptInterface").isColorLongApiSupported()) {
            canvas.drawColorLong = JavaReflect.method("android.graphics.Canvas", "drawColor", argTypes("long"))
            canvas.drawColorWithBlendMode = JavaReflect.method("android.graphics.Canvas", "drawColor", argTypes("int", "android.graphics.BlendMode"))
            canvas.drawColorWithBlendModeLong = JavaReflect.method("android.graphics.Canvas", "drawColor", argTypes("long", "android.graphics.BlendMode"))
            bitmap.eraseColorLong = JavaReflect.method("android.graphics.Bitmap", "eraseColor", argTypes("long"))
            paint.setColorLong = JavaReflect.method("android.graphics.Paint", "setColor", argTypes("long"))
            paint.setShadowLayerLong = JavaReflect.method("android.graphics.Paint", "setShadowLayer", argTypes("float", "float", "float", "long"))
            linear.buildFromArrayLong = JavaReflect.constructor("android.graphics.LinearGradient", argTypes("float", "float", "float", "float", "long[]", "float[]", "android.graphics.Shader.TileMode"))
            linear.buildFromEndsLong = JavaReflect.constructor("android.graphics.LinearGradient", argTypes("float", "float", "float", "float", "long", "long", "android.graphics.Shader.TileMode"))
            radial.buildFromArrayLong = JavaReflect.constructor("android.graphics.RadialGradient", argTypes("float", "float", "float", "long[]", "float[]", "android.graphics.Shader.TileMode"))
            radial.buildFromCenterEdgeLong = JavaReflect.constructor("android.graphics.RadialGradient", argTypes("float", "float", "float", "long", "long", "android.graphics.Shader.TileMode"))
            sweep.buildFromArrayLong = JavaReflect.constructor("android.graphics.SweepGradient", argTypes("float", "float", "long[]", "float[]"))
            sweep.buildFromEndsLong = JavaReflect.constructor("android.graphics.SweepGradient", argTypes("float", "float", "long", "long"))
        }

        if (js("typeof MapScript !== 'undefined'")) MapScript.loadModule("IntColor", intColorApi)
    } catch (e: dynamic) {
        js("erp(e)")
    }
}
