package ca.kotlin.text

import ca.kotlin.core.*


/**
 * modules/sense/FCString.js 的 Kotlin/JS 版本。
 *
 * 保留原始 API：parseFC / colorFC / clearSpans 与全部字符常量。
 * 样式解析依赖 G 的 Span 类，构造全部通过带参 JS helper，
 * 避免在 js("...") 里引用 Kotlin 局部变量。
 */
private val fcApi: dynamic = js("({})")

private val fcStringJs: dynamic = js("function (v) { return String(v); }")
private val fcLengthJs: dynamic = js("function (v) { return v.length | 0; }")
private val fcTruthyJs: dynamic = js("function (v) { return !!v; }")
private val fcIsNaNNs: dynamic = js("function (v) { return isNaN(v); }")
private val fcKeyInJs: dynamic = js("function (o, k) { return k in o; }")
private val fcSliceCharJs: dynamic = js("function (s, i) { return s.slice(i, i + 1); }")
private val fcLooseEqJs: dynamic = js("function (a, b) { return a == b; }")
private val fcForInJs: dynamic = js("function (o, f) { for (var k in o) f(k); }")
private val fcNewSpannableStringJs: dynamic = js("function (s) { return new G.SpannableString(s); }")
private val fcNewForegroundColorSpanJs: dynamic = js("function (c) { return new G.ForegroundColorSpan(c); }")
private val fcNewBoldSpanJs: dynamic = js("function () { return new G.StyleSpan(G.Typeface.BOLD); }")
private val fcNewItalicSpanJs: dynamic = js("function () { return new G.StyleSpan(G.Typeface.ITALIC); }")
private val fcNewUnknownStyleSpanJs: dynamic = js("function () { return new G.StyleSpan(0); }")
private val fcNewStrikethroughSpanJs: dynamic = js("function () { return new G.StrikethroughSpan(); }")
private val fcNewUnderlineSpanJs: dynamic = js("function () { return new G.UnderlineSpan(); }")
private val fcNaN: dynamic = js("NaN")
private val fcUndefined: dynamic = js("undefined")

private fun fcString(v: dynamic): dynamic = fcStringJs(v)
private fun fcLength(v: dynamic): Int = fcLengthJs(v)
private fun fcTruthy(v: dynamic): Boolean = fcTruthyJs(v)
private fun fcIsNaN(v: dynamic): Boolean = fcIsNaNNs(v)
private fun fcKeyIn(o: dynamic, k: dynamic): Boolean = fcKeyInJs(o, k)
private fun fcSliceChar(s: dynamic, i: dynamic): dynamic = fcSliceCharJs(s, i)
private fun fcLooseEq(a: dynamic, b: dynamic): Boolean = fcLooseEqJs(a, b)
private fun fcForIn(o: dynamic, f: dynamic) { fcForInJs(o, f) }

private fun fcNewParseObj(): dynamic {
    val o = js("({})")
    o.color = fcNaN
    o.colorStart = 0
    o.style = js("({})")
    o.spans = js("[]")
    o.result = js("[]")
    o.index = 0
    val style = o.style
    style[fcApi.BOLD] = fcNaN
    style[fcApi.STRIKETHROUGH] = fcNaN
    style[fcApi.UNDERLINE] = fcNaN
    style[fcApi.ITALIC] = fcNaN
    style[fcApi.RANDOMCHAR] = fcNaN
    return o
}

private fun fcNewColorObj(defaultcolor: dynamic): dynamic {
    val o = js("({})")
    o.defaultcolor = defaultcolor
    o.color = fcNaN
    o.colorStart = 0
    o.style = js("({})")
    o.spans = js("[]")
    o.index = 0
    val style = o.style
    style[fcApi.BOLD] = fcNaN
    style[fcApi.STRIKETHROUGH] = fcNaN
    style[fcApi.UNDERLINE] = fcNaN
    style[fcApi.ITALIC] = fcNaN
    style[fcApi.RANDOMCHAR] = fcNaN
    return o
}

private fun fcBuildStyleSpan(ch: dynamic): dynamic {
    if (fcLooseEq(ch, fcApi.BOLD)) return fcNewBoldSpanJs()
    if (fcLooseEq(ch, fcApi.STRIKETHROUGH)) return fcNewStrikethroughSpanJs()
    if (fcLooseEq(ch, fcApi.UNDERLINE)) return fcNewUnderlineSpanJs()
    if (fcLooseEq(ch, fcApi.ITALIC)) return fcNewItalicSpanJs()
    if (fcLooseEq(ch, fcApi.RANDOMCHAR)) return fcNewUnknownStyleSpanJs()
    return fcUndefined
}

private fun fcStartColor(o: dynamic, char: dynamic) {
    if (!fcIsNaN(o.color)) fcEndColor(o)
    o.color = fcApi.COLOR[char]
    o.colorStart = o.index
}

private fun fcEndColor(o: dynamic) {
    if (fcIsNaN(o.color)) return
    val span = js("({})")
    span.span = fcNewForegroundColorSpanJs(o.color)
    span.start = o.colorStart
    span.end = o.index
    o.spans.push(span)
    o.color = fcNaN
}

private fun fcStartStyle(o: dynamic, char: dynamic) {
    if (!fcIsNaN(o.style[char])) fcEndStyle(o, char)
    o.style[char] = o.index
}

private fun fcEndStyle(o: dynamic, char: dynamic) {
    if (fcIsNaN(o.style[char])) return
    val span = js("({})")
    span.span = fcBuildStyleSpan(char)
    span.start = o.style[char]
    span.end = o.index
    o.spans.push(span)
    o.style[char] = fcNaN
}

private fun fcReset(o: dynamic) {
    fcForIn(o.style, { char: dynamic -> fcEndStyle(o, char) })
    fcEndColor(o)
}

private fun fcColorTag(o: dynamic, len: dynamic) {
    if (!fcIsNaN(o.color)) {
        if (o.colorStart < o.index) {
            val span = js("({})")
            span.span = fcNewForegroundColorSpanJs(o.color)
            span.start = o.colorStart
            span.end = o.index
            o.spans.push(span)
        }
        o.colorStart = o.index + len
    }
    val span = js("({})")
    val color = if (fcIsNaN(o.color)) o.defaultcolor else o.color
    span.span = fcNewForegroundColorSpanJs(js("Common").setAlpha(color, 0x80))
    span.start = o.index
    span.end = o.index + len
    o.spans.push(span)
}

private fun fcTokenizeParse(o: dynamic, s: dynamic) {
    var f = false
    var i = 0
    val n = fcLength(s)
    while (i < n) {
        val c = fcSliceChar(s, i)
        if (f) {
            if (fcKeyIn(fcApi.COLOR, c)) {
                if (fcTruthy(fcApi.JAVA_EDITION)) fcReset(o)
                fcStartColor(o, c)
            } else if (fcKeyIn(o.style, c)) {
                fcStartStyle(o, c)
            } else if (fcLooseEq(c, fcApi.RESET)) {
                fcReset(o)
            } else if (fcLooseEq(c, fcApi.BEGIN)) {
                o.result.push(fcApi.BEGIN)
                o.index = o.index + 1
            } else {
                o.result.push(fcApi.BEGIN)
                o.result.push(c)
                o.index = o.index + 2
            }
            f = false
        } else if (fcLooseEq(c, fcApi.BEGIN)) {
            f = true
        } else {
            o.result.push(c)
            o.index = o.index + 1
        }
        i++
    }
    fcReset(o)
    if (f) o.result.push(fcApi.BEGIN)
}

private fun fcTokenizeColor(o: dynamic, s: dynamic) {
    var f = false
    var i = 0
    val n = fcLength(s)
    while (i < n) {
        val c = fcSliceChar(s, i)
        if (f) {
            if (fcKeyIn(fcApi.COLOR, c)) {
                o.index = o.index - 1
                if (fcTruthy(fcApi.JAVA_EDITION)) fcReset(o)
                fcStartColor(o, c)
                fcColorTag(o, 2)
            } else if (fcKeyIn(o.style, c)) {
                o.index = o.index - 1
                fcStartStyle(o, c)
                fcColorTag(o, 2)
            } else if (fcLooseEq(c, fcApi.RESET)) {
                o.index = o.index - 1
                fcReset(o)
                fcColorTag(o, 2)
            } else if (fcLooseEq(c, fcApi.BEGIN)) {
                o.index = o.index - 1
                fcColorTag(o, 1)
            }
            f = false
        } else if (fcLooseEq(c, fcApi.BEGIN)) {
            f = true
        }
        i++
        o.index = i
    }
    if (f) {
        o.index = o.index - 1
        fcColorTag(o, 1)
        o.index = o.index + 1
    }
    fcReset(o)
}

private fun fcApplySpans(target: dynamic, spans: dynamic) {
    val n = fcLength(spans)
    var i = 0
    while (i < n) {
        val e = spans[i]
        target.setSpan(e.span, e.start, e.end, js("G.Spanned.SPAN_INCLUSIVE_EXCLUSIVE"))
        i++
    }
}

private fun fcParseFC(s: dynamic): dynamic {
    val o = fcNewParseObj()
    fcTokenizeParse(o, fcString(s))
    val r = fcNewSpannableStringJs(o.result.join(""))
    fcApplySpans(r, o.spans)
    return r
}

private fun fcColorFC(ss: dynamic, defaultcolor: dynamic): dynamic {
    val o = fcNewColorObj(defaultcolor)
    fcTokenizeColor(o, fcString(ss))
    fcApplySpans(ss, o.spans)
    return fcUndefined
}

private fun fcClearSpans(ss: dynamic): dynamic {
    val classes = js("[G.ForegroundColorSpan, G.StyleSpan, G.StrikethroughSpan, G.UnderlineSpan]")
    val n = fcLength(classes)
    var i = 0
    while (i < n) {
        val e = classes[i]
        val a = ss.getSpans(0, ss.length(), e)
        val an = fcLength(a)
        var j = 0
        while (j < an) {
            ss.removeSpan(a[j])
            j++
        }
        i++
    }
    return fcUndefined
}

fun installFCString() {
    fcApi.JAVA_EDITION = false
    fcApi.BEGIN = "\u00a7"

    val color = js("({})")
    color["0"] = js("Common").rgbInt(0, 0, 0)
    color["1"] = js("Common").rgbInt(0, 0, 170)
    color["2"] = js("Common").rgbInt(0, 170, 0)
    color["3"] = js("Common").rgbInt(0, 170, 170)
    color["4"] = js("Common").rgbInt(170, 0, 0)
    color["5"] = js("Common").rgbInt(170, 0, 170)
    color["6"] = js("Common").rgbInt(255, 170, 0)
    color["7"] = js("Common").rgbInt(170, 170, 170)
    color["8"] = js("Common").rgbInt(85, 85, 85)
    color["9"] = js("Common").rgbInt(85, 85, 255)
    color["a"] = js("Common").rgbInt(85, 255, 85)
    color["b"] = js("Common").rgbInt(85, 255, 255)
    color["c"] = js("Common").rgbInt(255, 85, 85)
    color["d"] = js("Common").rgbInt(255, 85, 255)
    color["e"] = js("Common").rgbInt(255, 255, 85)
    color["f"] = js("Common").rgbInt(255, 255, 255)
    fcApi.COLOR = color

    fcApi.BOLD = "l"
    fcApi.STRIKETHROUGH = "m"
    fcApi.UNDERLINE = "n"
    fcApi.ITALIC = "o"
    fcApi.RANDOMCHAR = "k"
    fcApi.RESET = "r"

    val parseFC: dynamic = { s: dynamic -> fcParseFC(s) }
    parseFC.tokenize = { o: dynamic, s: dynamic -> fcTokenizeParse(o, s); fcUndefined }
    parseFC.startColor = { o: dynamic, char: dynamic -> fcStartColor(o, char); fcUndefined }
    parseFC.endColor = { o: dynamic -> fcEndColor(o); fcUndefined }
    parseFC.startStyle = { o: dynamic, char: dynamic -> fcStartStyle(o, char); fcUndefined }
    parseFC.endStyle = { o: dynamic, char: dynamic -> fcEndStyle(o, char); fcUndefined }
    parseFC.reset = { o: dynamic -> fcReset(o); fcUndefined }
    parseFC.buildStyleSpan = { ch: dynamic -> fcBuildStyleSpan(ch) }
    fcApi.parseFC = parseFC

    val colorFC: dynamic = { ss: dynamic, defaultcolor: dynamic -> fcColorFC(ss, defaultcolor) }
    colorFC.tokenize = { o: dynamic, s: dynamic -> fcTokenizeColor(o, s); fcUndefined }
    colorFC.startColor = { o: dynamic, char: dynamic -> fcStartColor(o, char); fcUndefined }
    colorFC.endColor = { o: dynamic -> fcEndColor(o); fcUndefined }
    colorFC.startStyle = { o: dynamic, char: dynamic -> fcStartStyle(o, char); fcUndefined }
    colorFC.endStyle = { o: dynamic, char: dynamic -> fcEndStyle(o, char); fcUndefined }
    colorFC.reset = { o: dynamic -> fcReset(o); fcUndefined }
    colorFC.colorTag = { o: dynamic, len: dynamic -> fcColorTag(o, len); fcUndefined }
    colorFC.buildStyleSpan = { ch: dynamic -> fcBuildStyleSpan(ch) }
    fcApi.colorFC = colorFC

    fcApi.clearSpans = { ss: dynamic -> fcClearSpans(ss) }

    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("FCString", fcApi)
    }
}
