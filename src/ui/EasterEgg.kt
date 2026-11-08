package ca.kotlin.ui

import ca.kotlin.core.*


/**
 * modules/EasterEgg.js 的 Kotlin/JS 版本。
 *
 * onCreate 仍由 MapScript.loadModule 立即调用；动态 this 通过薄 JS wrapper
 * 传回来，保证 this.initIcon 的查找方式和原实现一致。
 *
 * Paint.setColor / setShadowLayer 这类有 int/long 重载的方法继续走
 * IntColor 的 JavaReflect 精确签名，不直接依赖 Rhino 的重载推断。
 */
private val easterEggApi: dynamic = js("({})")

// ---------------- JS 语义 helper ----------------
private val eeTruthyJs: dynamic = js("function (v) { return !!v; }")
private fun eeTruthy(v: dynamic): Boolean = eeTruthyJs(v)

private val eeThis0Js: dynamic = js("function (f) { return function () { return f(this); }; }")

// ---------------- Java / Android helper ----------------
private val eeAppSourceDirJs: dynamic = js(
    "function () { return ctx.getPackageManager().getApplicationInfo('com.mojang.minecraftpe', 128).publicSourceDir; }"
)
private val eeNewZipFileJs: dynamic = js(
    "function (path) { return new java.util.zip.ZipFile(path); }"
)
private val eeOpenEntryStreamJs: dynamic = js(
    "function (zf, name) { return zf.getInputStream(zf.getEntry(name)); }"
)
private val eeNewBitmapJs: dynamic = js(
    "function (w, h) { return G.Bitmap.createBitmap(w, h, G.Bitmap.Config.ARGB_8888); }"
)
private val eeNewCanvasJs: dynamic = js(
    "function (bitmap) { return new G.Canvas(bitmap); }"
)
private val eeNewPaintJs: dynamic = js("function () { return new G.Paint(); }")
private val eeNewBitmapShaderJs: dynamic = js(
    "function (stream) {" +
        " return new G.BitmapShader(" +
        "  G.Bitmap.createScaledBitmap(G.BitmapFactory.decodeStream(stream), 160, 160, false)," +
        "  G.Shader.TileMode.REPEAT, G.Shader.TileMode.REPEAT);" +
        "}"
)
private val eeNewFrameLayoutJs: dynamic = js(
    "function () { return new G.FrameLayout(ctx); }"
)
private val eeNewImageViewJs: dynamic = js(
    "function () { return new G.ImageView(ctx); }"
)
private val eeNewFrameParamsJs: dynamic = js(
    "function (w, h) { return new G.FrameLayout.LayoutParams(w, h); }"
)
private val eeDpJs: dynamic = js("function (n) { return n * G.dp; }")
private val eeSubJs: dynamic = js("function (a, b) { return a - b; }")
private val eeMulJs: dynamic = js("function (a, b) { return a * b; }")

private fun eeGetBitmap(w: dynamic): dynamic {
    val zf = eeNewZipFileJs(eeAppSourceDirJs())
    val b = eeOpenEntryStreamJs(
        zf,
        "assets/resource_packs/vanilla/textures/blocks/command_block_front_mipmap.png"
    )
    val bmp = eeNewBitmapJs(w, w)
    val cv = eeNewCanvasJs(bmp)
    cv.scale(w / 170, w / 170)
    val pt = eeNewPaintJs()
    pt.setAntiAlias(true)
    pt.setShader(eeNewBitmapShaderJs(b))
    cv.drawRect(0, 0, 170, 170, pt)
    pt.setShader(js("null"))
    pt.setTextSize(60)
    val fm = pt.getFontMetrics()
    val th = fm.bottom - fm.top
    js("IntColor").Paint.setColor(pt, js("Common").argbInt(0x80, 0, 0, 0))
    js("IntColor").Paint.setShadowLayer(pt, 1, 0, 0, pt.getColor())
    cv.drawRoundRect(0, eeSubJs(170, th), 170, 200, 10, 10, pt)
    js("IntColor").Paint.setColor(pt, js("G.Color.WHITE"))
    js("IntColor").Paint.setShadowLayer(pt, 1, 0, 0, js("G.Color.BLACK"))
    cv.drawText(" CA_", 0, eeSubJs(170, fm.descent), pt)
    return bmp
}

private fun eeInitIcon(): dynamic {
    var img: dynamic = js("null")
    try {
        img = easterEggApi.getBitmap(480)
    } catch (e: dynamic) {
        js("Log").e(e)
    }
    if (eeTruthy(img)) {
        js("CA").Icon.easteregg = { size: dynamic ->
            val zp = eeDpJs(size)
            val frm = eeNewFrameLayoutJs()
            val view = eeNewImageViewJs()
            view.setImageBitmap(img)
            view.setLayoutParams(eeNewFrameParamsJs(eeMulJs(32, zp), eeMulJs(32, zp)))
            frm.addView(view)
            frm
        }
    }
    return js("undefined")
}

fun installEasterEgg() {
    easterEggApi.onCreate = eeThis0Js { self: dynamic ->
        js("G").ui(self.initIcon)
        js("undefined")
    }
    easterEggApi.getBitmap = { w: dynamic -> eeGetBitmap(w) }
    easterEggApi.initIcon = {
        eeInitIcon()
        js("undefined")
    }

    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("EasterEgg", easterEggApi)
    }
}
