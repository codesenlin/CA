package ca.kotlin.text

import ca.kotlin.core.*


/**
 * modules/sense/ISegment.js 的 Kotlin/JS 版本。
 *
 * 包含流式字符串读写、lenient string、kvSort、parseUnit 和 rawJson。
 * rawJson 的 Span 构造依赖 G / FCString / ctx，全部通过带参 JS helper；
 * Java 数组长度统一走 `length|0` helper。
 */
private val isegApi: dynamic = js("({})")

private val isegStringJs: dynamic = js("function (v) { return String(v); }")
private val isegLengthJs: dynamic = js("function (v) { return v.length | 0; }")
// alignStringEnd 按原实现直接读取 s.length；非字符串入参时保留 NaN/字符串拼接行为。
private val isegRawLengthJs: dynamic = js("function (v) { return v.length; }")
private val isegTruthyJs: dynamic = js("function (v) { return !!v; }")
private val isegKeyInJs: dynamic = js("function (o, k) { return k in o; }")
private val isegForInJs: dynamic = js("function (o, f) { for (var k in o) f(k); }")
private val isegIsArrayJs: dynamic = js("function (v) { return Array.isArray(v); }")
private val isegIsObjectJs: dynamic = js("function (v) { return v instanceof Object; }")
private val isegIsFunctionJs: dynamic = js("function (v) { return typeof v === 'function'; }")
private val isegIsCharSequenceJs: dynamic = js("function (v) { return v instanceof java.lang.CharSequence; }")
private val isegStrictEqJs: dynamic = js("function (a, b) { return a === b; }")
private val isegLtJs: dynamic = js("function (a, b) { return a < b; }")
private val isegGtJs: dynamic = js("function (a, b) { return a > b; }")
private val isegCharAtJs: dynamic = js("function (s, i) { return s.charAt(i); }")
private val isegCharIndexJs: dynamic = js("function (s, i) { return s[i]; }")
private val isegSliceJs: dynamic = js("function (s, i) { return s.slice(i); }")
private val isegSlice2Js: dynamic = js("function (s, a, b) { return s.slice(a, b); }")
private val isegFromCharCodeJs: dynamic = js("function (n) { return String.fromCharCode(n); }")
private val isegParseIntHexJs: dynamic = js("function (c) { return parseInt(c, 16); }")
private val isegParseFloatJs: dynamic = js("function (s) { return parseFloat(s); }")
private val isegIsFiniteJs: dynamic = js("function (n) { return isFinite(n); }")
private val isegSearchUnitJs: dynamic = js("""function (s) { return s.search(/[^\d\s\.]/); }""")
private val isegTrimRightJs: dynamic = js("""function (s) { return s.replace(/\s*$/, ""); }""")
private val isegNewArrayJs: dynamic = js("function (n) { return new Array(n); }")
private val isegMathMaxJs: dynamic = js("function (a, b) { return Math.max(a, b); }")
private val isegArraySortJs: dynamic = js("function (a, f) { a.sort(function (x, y) { return f(x[0], y[0]); }); }")
private val isegMapJoinJs: dynamic = js("function (a, f, sep) { return a.map(f).join(sep); }")
private val isegHexJs: dynamic = js("function (c) { return c.charCodeAt(0).toString(16); }")
private val isegUndefined: dynamic = js("undefined")

private val isegNewSpannableStringBuilderJs: dynamic = js("function () { return new G.SpannableStringBuilder(); }")
private val isegNewAbsoluteSizeSpanJs: dynamic = js("function (n) { return new G.AbsoluteSizeSpan(n); }")
private val isegNewAbsoluteSizeSpan2Js: dynamic = js("function (n, b) { return new G.AbsoluteSizeSpan(n, b); }")
private val isegNewAbsoluteSizeSpanSpJs: dynamic = js("function (n) { return new G.AbsoluteSizeSpan(n * G.sp); }")
private val isegNewRelativeSizeSpanJs: dynamic = js("function (n) { return new G.RelativeSizeSpan(n); }")
private val isegNewForegroundColorSpanJs: dynamic = js("function (c) { return new G.ForegroundColorSpan(c); }")
private val isegNewBackgroundColorSpanJs: dynamic = js("function (c) { return new G.BackgroundColorSpan(c); }")
private val isegNewStyleSpanJs: dynamic = js("function (s) { return new G.StyleSpan(s); }")
private val isegNewUnderlineSpanJs: dynamic = js("function () { return new G.UnderlineSpan(); }")
private val isegNewStrikethroughSpanJs: dynamic = js("function () { return new G.StrikethroughSpan(); }")
private val isegNewSuperscriptSpanJs: dynamic = js("function () { return new G.SuperscriptSpan(); }")
private val isegNewSubscriptSpanJs: dynamic = js("function () { return new G.SubscriptSpan(); }")
private val isegNewTypefaceSpanJs: dynamic = js("function (t) { return new G.TypefaceSpan(t); }")
private val isegNewBulletSpanJs: dynamic = js("function () { return new G.BulletSpan(); }")
private val isegNewImageSpanJs: dynamic = js("function (c, u) { return new G.ImageSpan(c, u); }")
private val isegNewUrlSpanJs: dynamic = js("function (u) { return new G.URLSpan(u); }")
private val isegNewAlignmentSpanJs: dynamic = js("function (a) { return new G.AlignmentSpan.Standard(a); }")
private val isegNewTabStopSpanJs: dynamic = js("function (w) { return new G.TabStopSpan.Standard(w); }")
private val isegNewHtmlJs: dynamic = js("function (v) { return G.Html.fromHtml(v, 0); }")
private val isegNewHtmlLegacyJs: dynamic = js("function (v) { return G.Html.fromHtml(v); }")
private val isegParseColorJs: dynamic = js("function (v) { return G.Color.parseColor(v); }")
private val isegParseUriJs: dynamic = js("function (u) { return android.net.Uri.parse(u); }")

private fun isegString(v: dynamic): dynamic = isegStringJs(v)
private fun isegLength(v: dynamic): Int = isegLengthJs(v)
private fun isegRawLength(v: dynamic): dynamic = isegRawLengthJs(v)
private fun isegTruthy(v: dynamic): Boolean = isegTruthyJs(v)
private fun isegKeyIn(o: dynamic, k: dynamic): Boolean = isegKeyInJs(o, k)
private fun isegForIn(o: dynamic, f: dynamic) { isegForInJs(o, f) }
private fun isegIsArray(v: dynamic): Boolean = isegIsArrayJs(v)
private fun isegIsObject(v: dynamic): Boolean = isegIsObjectJs(v)
private fun isegIsFunction(v: dynamic): Boolean = isegIsFunctionJs(v)
private fun isegIsCharSequence(v: dynamic): Boolean = isegIsCharSequenceJs(v)
private fun isegStrictEq(a: dynamic, b: dynamic): Boolean = isegStrictEqJs(a, b)
private fun isegLt(a: dynamic, b: dynamic): Boolean = isegLtJs(a, b)
private fun isegGt(a: dynamic, b: dynamic): Boolean = isegGtJs(a, b)
private fun isegCharAt(s: dynamic, i: dynamic): dynamic = isegCharAtJs(s, i)

private fun isegCoverSpan(src: dynamic, span: dynamic) {
    if (isegTruthy(span)) src.setSpan(span, 0, src.length(), js("G.Spanned.SPAN_INCLUSIVE_EXCLUSIVE"))
}

private fun isegParseColor(v: dynamic): dynamic {
    if (isegKeyIn(js("Common").theme, v)) return js("Common").theme[v]
    return isegParseColorJs(v)
}

private fun isegParseTextSize(v: dynamic): dynamic {
    val size = isegApi.parseUnit(isegString(v))
    if (!isegTruthy(size)) return null
    val number = size.number
    val unit = size.unit
    if (isegStrictEq(unit, "px")) return isegNewAbsoluteSizeSpanJs(number)
    if (isegStrictEq(unit, "dp")) return isegNewAbsoluteSizeSpan2Js(number, true)
    if (isegStrictEq(unit, "ts")) return isegNewAbsoluteSizeSpanSpJs(js("Common").theme.textsize[number])
    if (isegStrictEq(unit, "%")) return isegNewRelativeSizeSpanJs(number / 100)
    if (isegStrictEq(unit, null)) return isegNewRelativeSizeSpanJs(number)
    return null
}

private fun isegParseAlign(v: dynamic): dynamic {
    if (isegStrictEq(v, "normal") || isegStrictEq(v, "left")) return js("G.Layout.Alignment.ALIGN_NORMAL")
    if (isegStrictEq(v, "opposite") || isegStrictEq(v, "right")) return js("G.Layout.Alignment.ALIGN_OPPOSITE")
    if (isegStrictEq(v, "center")) return js("G.Layout.Alignment.ALIGN_CENTER")
    return null
}

private fun isegParseHtml(v: dynamic): dynamic {
    if (js("ScriptInterface").isHtmlSegmentSupported()) return isegNewHtmlJs(v)
    return isegNewHtmlLegacyJs(v)
}

private fun isegRawJsonImpl(o: dynamic, variableMap: dynamic): dynamic {
    val result = isegNewSpannableStringBuilderJs()
    if (isegIsArray(o)) {
        isegForIn(o, { key: dynamic -> result.append(isegRawJsonImpl(o[key], variableMap)) })
    } else if (isegIsFunction(o)) {
        result.append(isegRawJsonImpl(o(variableMap), variableMap))
    } else if (isegIsObject(o)) {
        if (isegTruthy(o.text)) {
            result.append(o.text)
        } else if (isegTruthy(o.lines)) {
            val n = isegLength(o.lines)
            var i = 0
            while (i < n) {
                if (i > 0) result.append("\n")
                result.append(isegRawJsonImpl(o.lines[i], variableMap))
                i++
            }
        } else if (isegTruthy(o.variable)) {
            result.append(isegString(variableMap[o.variable]))
        } else if (isegTruthy(o.formattedCommand)) {
            result.append(o.formattedCommand)
            isegCoverSpan(result, isegNewForegroundColorSpanJs(js("G.Color.WHITE")))
            js("FCString").colorFC(result, js("G.Color.WHITE"))
            isegCoverSpan(result, isegNewTypefaceSpanJs("monospace"))
            isegCoverSpan(result, isegNewBackgroundColorSpanJs(js("G.Color.BLACK")))
        } else if (isegTruthy(o.formattedText)) {
            result.append(js("FCString").parseFC(o.formattedText))
        } else if (isegTruthy(o.command)) {
            result.append(o.command)
            isegCoverSpan(result, isegNewTypefaceSpanJs("monospace"))
        } else if (isegTruthy(o.list)) {
            isegForIn(o.list, { key: dynamic ->
                result.setSpan(isegNewBulletSpanJs(), result.length(), result.length(), js("G.Spanned.SPAN_INCLUSIVE_EXCLUSIVE"))
                result.append(isegRawJsonImpl(o.list[key], variableMap))
                result.append("\n")
            })
        } else if (isegTruthy(o.html)) {
            result.append(isegParseHtml(o.html))
        } else if (isegTruthy(o.image)) {
            result.setSpan(isegNewImageSpanJs(js("ctx"), isegParseUriJs(o.image)), result.length(), result.length(), js("G.Spanned.SPAN_INCLUSIVE_EXCLUSIVE"))
        }
        if (isegTruthy(o.extra)) result.append(isegRawJsonImpl(o.extra, variableMap))
        if (isegTruthy(o.link)) isegCoverSpan(result, isegNewUrlSpanJs(o.link))
        if (isegTruthy(o.color)) isegCoverSpan(result, isegNewForegroundColorSpanJs(isegParseColor(o.color)))
        if (isegTruthy(o.bgcolor)) isegCoverSpan(result, isegNewBackgroundColorSpanJs(isegParseColor(o.bgcolor)))
        if (isegTruthy(o.bold)) isegCoverSpan(result, isegNewStyleSpanJs(js("G.Typeface.BOLD")))
        if (isegTruthy(o.italic)) isegCoverSpan(result, isegNewStyleSpanJs(js("G.Typeface.ITALIC")))
        if (isegTruthy(o.underlined)) isegCoverSpan(result, isegNewUnderlineSpanJs())
        if (isegTruthy(o.strikethrough)) isegCoverSpan(result, isegNewStrikethroughSpanJs())
        if (isegTruthy(o.superscript)) isegCoverSpan(result, isegNewSuperscriptSpanJs())
        if (isegTruthy(o.subscript)) isegCoverSpan(result, isegNewSubscriptSpanJs())
        if (isegTruthy(o.typeface)) isegCoverSpan(result, isegNewTypefaceSpanJs(o.typeface))
        if (isegTruthy(o.size)) isegCoverSpan(result, isegParseTextSize(o.size))
        if (isegTruthy(o.align)) isegCoverSpan(result, isegNewAlignmentSpanJs(isegParseAlign(o.align)))
        if (isegTruthy(o.tabWidth)) isegCoverSpan(result, isegNewTabStopSpanJs(o.tabWidth))
    } else if (isegIsCharSequence(o)) {
        result.append(o)
    } else {
        result.append(isegString(o))
    }
    return result
}

private fun isegAlignStringEnd(s: dynamic, len: dynamic, char: dynamic): dynamic {
    var t: dynamic = ""
    var i = len - isegRawLength(s)
    while (i > 0) {
        t = t + char
        i -= 1
    }
    return t + s
}

private fun isegIsStringEOS(strStream: dynamic): Boolean {
    return strStream.cur >= isegLength(strStream.str)
}

private fun isegReadStreamStr(strStream: dynamic): dynamic {
    val c = isegCharAt(strStream.str, strStream.cur)
    strStream.cur = strStream.cur + 1
    return c
}

private fun isegPeekStreamStr(strStream: dynamic): dynamic {
    return isegCharAt(strStream.str, strStream.cur)
}

private fun isegReadStreamAll(strStream: dynamic): dynamic {
    val oldcur = strStream.cur
    strStream.cur = isegLength(strStream.str)
    return isegSliceJs(strStream.str, oldcur)
}

private fun isegPeekStreamAll(strStream: dynamic): dynamic {
    return isegSliceJs(strStream.str, strStream.cur)
}

private fun isegReadLenientString(strStream: dynamic, options: dynamic): dynamic {
    var state = 0
    val r = js("[]")
    var hex: dynamic = 0
    var hexn: dynamic = 0
    val endchars = if (isegTruthy(options.endChars)) options.endChars else ""
    while (strStream.cur < isegLength(strStream.str)) {
        val c = isegCharAt(strStream.str, strStream.cur)
        strStream.cur = strStream.cur + 1
        when (state) {
            0 -> {
                if (isegStrictEq(c, "\\")) {
                    state = 1
                } else if (endchars.indexOf(c) >= 0) {
                    return r.join("")
                } else {
                    r.push(c)
                }
            }
            1 -> {
                var resetState = true
                if (isegStrictEq(c, "0")) {
                    r.push("\u0000")
                } else if (isegStrictEq(c, "n")) {
                    r.push("\n")
                } else if (isegStrictEq(c, "r")) {
                    r.push("\r")
                } else if (isegStrictEq(c, "v")) {
                    r.push("\u000B")
                } else if (isegStrictEq(c, "t")) {
                    r.push("\t")
                } else if (isegStrictEq(c, "b")) {
                    r.push("\u0008")
                } else if (isegStrictEq(c, "f")) {
                    r.push("\u000C")
                } else if (isegStrictEq(c, "x")) {
                    state = 2
                    hex = 0
                    hexn = 2
                    resetState = false
                } else if (isegStrictEq(c, "u")) {
                    state = 3
                    hex = 0
                    hexn = 4
                    resetState = false
                } else {
                    r.push(c)
                }
                if (resetState) state = 0
            }
            else -> {
                hex = hex * 16 + isegParseIntHexJs(c)
                hexn = hexn - 1
                if (hexn <= 0) {
                    r.push(isegFromCharCodeJs(hex))
                    state = 0
                }
            }
        }
    }
    return r.join("")
}

private fun isegWriteLenientString(s: dynamic, options: dynamic): dynamic {
    var i = 0
    val r = js("[]")
    val skipchars = if (isegTruthy(options.skipChars)) options.skipChars else ""
    while (i < isegLength(s)) {
        val c = isegCharAt(s, i)
        i++
        if (isegStrictEq(c, "\u0000")) {
            r.push("\\0")
        } else if (isegStrictEq(c, "\n")) {
            r.push("\\n")
        } else if (isegStrictEq(c, "\r")) {
            r.push("\\r")
        } else if (isegStrictEq(c, "\u000B")) {
            r.push("\\v")
        } else if (isegStrictEq(c, "\t")) {
            r.push("\\t")
        } else if (isegStrictEq(c, "\u0008")) {
            r.push("\\b")
        } else if (isegStrictEq(c, "\u000C")) {
            r.push("\\f")
        } else if (skipchars.indexOf(c) >= 0) {
            r.push("\\" + c)
        } else if (isegLt(c, " ") || isegGt(c, "~")) {
            r.push("\\u" + isegAlignStringEnd(isegHexJs(c), 4, "0"))
        } else {
            r.push(c)
        }
    }
    return r.join("")
}

private fun isegReadLenientStringArray(strStream: dynamic, options: dynamic): dynamic {
    val r = js("[]")
    val opt = js("({})")
    opt.endChars = options.splitChars + options.endChars
    while (strStream.cur < isegLength(strStream.str)) {
        r.push(isegApi.readLenientString(strStream, opt))
        if (options.endChars.indexOf(isegCharIndexJs(strStream.str, strStream.cur - 1)) >= 0) break
    }
    return r
}

private fun isegWriteLenientStringArray(arr: dynamic, options: dynamic): dynamic {
    val opt = js("({})")
    opt.skipChars = options.splitChar + options.skipChars
    return isegMapJoinJs(arr, { e: dynamic -> isegApi.writeLenientString(e, opt) }, options.splitChar)
}

private fun isegKvSort(k: dynamic, v: dynamic, f: dynamic) {
    val arr = isegNewArrayJs(isegMathMaxJs(isegLength(k), isegLength(v)))
    val n = isegLength(arr)
    var i = 0
    while (i < n) {
        val e = js("[]")
        e.push(k[i])
        e.push(v[i])
        arr[i] = e
        i++
    }
    isegArraySortJs(arr, f)
    i = 0
    while (i < n) {
        val e = arr[i]
        k[i] = e[0]
        v[i] = e[1]
        i++
    }
}

private fun isegParseUnit(str: dynamic): dynamic {
    val p = isegSearchUnitJs(str)
    if (p >= 0) {
        val number = isegParseFloatJs(isegSlice2Js(str, 0, p))
        if (!isegIsFiniteJs(number)) return null
        val out = js("({})")
        out.number = number
        out.unit = isegTrimRightJs(isegSliceJs(str, p))
        return out
    }
    val number = isegParseFloatJs(str)
    val out = js("({})")
    out.number = number
    out.unit = null
    return out
}

fun installISegment() {
    isegApi.alignStringEnd = { s: dynamic, len: dynamic, char: dynamic -> isegAlignStringEnd(s, len, char) }
    isegApi.isStringEOS = { strStream: dynamic -> isegIsStringEOS(strStream) }
    isegApi.readStreamStr = { strStream: dynamic -> isegReadStreamStr(strStream) }
    isegApi.peekStreamStr = { strStream: dynamic -> isegPeekStreamStr(strStream) }
    isegApi.readStreamAll = { strStream: dynamic -> isegReadStreamAll(strStream) }
    isegApi.peekStreamAll = { strStream: dynamic -> isegPeekStreamAll(strStream) }
    isegApi.readLenientString = { strStream: dynamic, options: dynamic -> isegReadLenientString(strStream, options) }
    isegApi.writeLenientString = { s: dynamic, options: dynamic -> isegWriteLenientString(s, options) }
    isegApi.readLenientStringArray = { strStream: dynamic, options: dynamic ->
        isegReadLenientStringArray(strStream, options)
    }
    isegApi.writeLenientStringArray = { arr: dynamic, options: dynamic ->
        isegWriteLenientStringArray(arr, options)
    }
    isegApi.kvSort = { k: dynamic, v: dynamic, f: dynamic -> isegKvSort(k, v, f); isegUndefined }
    isegApi.parseUnit = { str: dynamic -> isegParseUnit(str) }

    val rawJson: dynamic = { o: dynamic, variableMap: dynamic -> isegRawJsonImpl(o, variableMap) }
    rawJson.coverSpan = { src: dynamic, span: dynamic -> isegCoverSpan(src, span); isegUndefined }
    rawJson.parseColor = { v: dynamic -> isegParseColor(v) }
    rawJson.parseTextSize = { v: dynamic -> isegParseTextSize(v) }
    rawJson.parseAlign = { v: dynamic -> isegParseAlign(v) }
    rawJson.parseHtml = { v: dynamic -> isegParseHtml(v) }
    isegApi.rawJson = rawJson

    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("ISegment", isegApi)
    }
}
