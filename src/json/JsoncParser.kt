package ca.kotlin.json

import ca.kotlin.core.*


/**
 * JSON/JSONC 解析器（JSONLanguageService 迁移阶段 1）。
 *
 * 目标不是实现一个“更好的” JSON 解析器，而是复刻旧 bundle 里
 * `vscode-json-languageservice` 自带 parser（`jsonParser.parse` + `jsonc-parser` scanner）
 * 的节点树、错误和偏移，供 CAIntelliSenseModule.kt 的 jsonPro 路径无缝替换。
 *
 * 节点结构与旧实现一致（这些字段 CA 会直接读）：
 *   object:   type/offset/length/parent/properties/children
 *   array:    type/offset/length/parent/items/children
 *   property: type/offset/length/parent/colonOffset/keyNode/valueNode/children
 *   string:   type/offset/length/parent/value
 *   number:   type/offset/length/parent/value/isInteger
 *   boolean:  type/offset/length/parent/value
 *   null:     type/offset/length/parent/value
 *
 * 解析结果 JSONDocument：
 *   root         节点或 undefined
 *   syntaxErrors 与旧实现相同的 Diagnostic 数组（range/message/severity/code/source）
 *   comments     注释 Range 数组
 *   errors       由 syntaxErrors 派生的 { error, offset, length }（jsonc-parser ParseError 形状）
 *   getNodeFromOffset(offset, includeRightBound)
 *
 * 注意：`errors` 是给 CA 现有 `jsonDoc.errors` 读取逻辑准备的兼容视图；旧 bundle 的
 * JSONDocument 只有 `syntaxErrors`，没有 `errors`。两者来自同一组诊断，不会分叉。
 */

private val jpUndefined: dynamic = js("undefined")

private val jpStrictEqJs: dynamic = js("function (a, b) { return a === b ? 1 : 0; }")
private fun jpStrictEq(a: dynamic, b: dynamic): Boolean = (jpStrictEqJs(a, b) as Int) != 0

private val jpTruthyJs: dynamic = js("function (v) { return !!v ? 1 : 0; }")
private fun jpTruthy(v: dynamic): Boolean = (jpTruthyJs(v) as Int) != 0

private val jpIsUndefinedJs: dynamic = js("function (v) { return v === undefined ? 1 : 0; }")
private fun jpIsUndefined(v: dynamic): Boolean = (jpIsUndefinedJs(v) as Int) != 0

private val jpTypeOfJs: dynamic = js("function (v) { return typeof v; }")
private fun jpIsNumber(v: dynamic): Boolean = jpStrictEq(jpTypeOfJs(v), "number")
private fun jpIsObject(v: dynamic): Boolean = jpStrictEq(jpTypeOfJs(v), "object") && v != null

private val jpIsSpaceJs: dynamic = js("function (s) { return /\\s/.test(s) ? 1 : 0; }")
private fun jpIsSpace(ch: dynamic): Boolean = (jpIsSpaceJs(ch) as Int) != 0

private val jpFromCharCodeJs: dynamic = js("function (c) { return String.fromCharCode(c); }")
private fun jpFromCharCode(code: Int): dynamic = jpFromCharCodeJs(code)

private val jpJsonParseJs: dynamic = js("JSON")

// ---------------- node builders ----------------

private fun jpNewNode(type: String, parent: dynamic, offset: Int): dynamic {
    val node = js("({})")
    node.type = type
    node.offset = offset
    node.length = 0
    node.parent = parent
    node.children = js("[]")
    return node
}

private fun jpNewObjectNode(parent: dynamic, offset: Int): dynamic {
    val node = jpNewNode("object", parent, offset)
    val properties = js("[]")
    node.properties = properties
    node.children = properties
    return node
}

private fun jpNewArrayNode(parent: dynamic, offset: Int): dynamic {
    val node = jpNewNode("array", parent, offset)
    val items = js("[]")
    node.items = items
    node.children = items
    return node
}

private fun jpNewPropertyNode(parent: dynamic, offset: Int): dynamic {
    val node = jpNewNode("property", parent, offset)
    node.colonOffset = -1
    return node
}

private fun jpNewStringNode(parent: dynamic, offset: Int, length: Int): dynamic {
    val node = jpNewNode("string", parent, offset)
    node.length = length
    node.value = ""
    return node
}

private fun jpNewNumberNode(parent: dynamic, offset: Int): dynamic {
    val node = jpNewNode("number", parent, offset)
    node.isInteger = true
    node.value = js("NaN")
    return node
}

private fun jpNewBooleanNode(parent: dynamic, offset: Int, value: Boolean): dynamic {
    val node = jpNewNode("boolean", parent, offset)
    node.value = value
    return node
}

private fun jpNewNullNode(parent: dynamic, offset: Int): dynamic {
    val node = jpNewNode("null", parent, offset)
    node.value = null
    return node
}

private fun jpRefreshPropertyChildren(node: dynamic) {
    val children = js("[]")
    children.push(node.keyNode)
    if (!jpIsUndefined(node.valueNode)) children.push(node.valueNode)
    node.children = children
}

// ---------------- scanner（jsonc-parser createScanner 的移植） ----------------

private const val JP_EOF = 17
private const val JP_UNKNOWN = 16
private const val JP_STRING = 10
private const val JP_NUMBER = 11
private const val JP_LINE_COMMENT = 12
private const val JP_BLOCK_COMMENT = 13
private const val JP_LINE_BREAK = 14
private const val JP_TRIVIA = 15

private fun jpIsWhiteSpace(code: Int): Boolean = code == 32 || code == 9
private fun jpIsLineBreak(code: Int): Boolean = code == 10 || code == 13
private fun jpIsDigit(code: Int): Boolean = code >= 48 && code <= 57

private fun jpIsUnknownContentCharacter(code: Int): Boolean {
    if (jpIsWhiteSpace(code) || jpIsLineBreak(code)) return false
    when (code) {
        125, 93, 123, 91, 34, 58, 44, 47 -> return false
    }
    return true
}

private class JsoncScanner(private val text: dynamic, private val length: Int) {
    var pos = 0
    var tokenOffset = 0
    var token = JP_UNKNOWN
    var scanError = 0
    var value: dynamic = ""

    val tokenLength: Int
        get() = pos - tokenOffset

    private fun charCodeAt(index: Int): Int {
        if (index < 0 || index >= length) return -1
        return text.charCodeAt(index) as Int
    }

    private fun scanHexDigits(count: Int, exact: Boolean): Int {
        var digits = 0
        var result = 0
        while (digits < count || !exact) {
            val ch = charCodeAt(pos)
            if (ch >= 48 && ch <= 57) {
                result = result * 16 + ch - 48
            } else if (ch >= 65 && ch <= 70) {
                result = result * 16 + ch - 65 + 10
            } else if (ch >= 97 && ch <= 102) {
                result = result * 16 + ch - 97 + 10
            } else {
                break
            }
            pos++
            digits++
        }
        if (digits < count) result = -1
        return result
    }

    private fun scanNumber(): dynamic {
        val start = pos
        if (charCodeAt(pos) == 48) {
            pos++
        } else {
            pos++
            while (pos < length && jpIsDigit(charCodeAt(pos))) pos++
        }
        if (pos < length && charCodeAt(pos) == 46) {
            pos++
            if (pos < length && jpIsDigit(charCodeAt(pos))) {
                pos++
                while (pos < length && jpIsDigit(charCodeAt(pos))) pos++
            } else {
                scanError = 3
                return text.substring(start, pos)
            }
        }
        var end = pos
        if (pos < length && (charCodeAt(pos) == 69 || charCodeAt(pos) == 101)) {
            pos++
            // 旧实现是 `pos < len && ch === '+' || ch === '-'`（&& 优先），这里保持同样的优先级。
            if ((pos < length && charCodeAt(pos) == 43) || charCodeAt(pos) == 45) pos++
            if (pos < length && jpIsDigit(charCodeAt(pos))) {
                pos++
                while (pos < length && jpIsDigit(charCodeAt(pos))) pos++
                end = pos
            } else {
                scanError = 3
            }
        }
        return text.substring(start, end)
    }

    private fun scanString(): dynamic {
        var result: dynamic = ""
        var start = pos
        while (true) {
            if (pos >= length) {
                result = result + text.substring(start, pos)
                scanError = 2
                break
            }
            val ch = charCodeAt(pos)
            if (ch == 34) {
                result = result + text.substring(start, pos)
                pos++
                break
            }
            if (ch == 92) {
                result = result + text.substring(start, pos)
                pos++
                if (pos >= length) {
                    scanError = 2
                    break
                }
                val ch2 = charCodeAt(pos)
                pos++
                when (ch2) {
                    34 -> result = result + "\""
                    92 -> result = result + "\\"
                    47 -> result = result + "/"
                    98 -> result = result + "\b"
                    102 -> result = result + "\u000C"
                    110 -> result = result + "\n"
                    114 -> result = result + "\r"
                    116 -> result = result + "\t"
                    117 -> {
                        val ch3 = scanHexDigits(4, true)
                        if (ch3 >= 0) {
                            result = result + jpFromCharCode(ch3)
                        } else {
                            scanError = 4
                        }
                    }
                    else -> scanError = 5
                }
                start = pos
                continue
            }
            if (ch >= 0 && ch <= 0x1f) {
                if (jpIsLineBreak(ch)) {
                    result = result + text.substring(start, pos)
                    scanError = 2
                    break
                } else {
                    scanError = 6
                }
            }
            pos++
        }
        return result
    }

    fun scan(): Int {
        value = ""
        scanError = 0
        tokenOffset = pos
        if (pos >= length) {
            tokenOffset = length
            token = JP_EOF
            return JP_EOF
        }

        var code = charCodeAt(pos)
        if (jpIsWhiteSpace(code)) {
            do {
                pos++
                value = value + jpFromCharCode(code)
                code = charCodeAt(pos)
            } while (jpIsWhiteSpace(code))
            token = JP_TRIVIA
            return JP_TRIVIA
        }
        if (jpIsLineBreak(code)) {
            pos++
            value = value + jpFromCharCode(code)
            if (code == 13 && charCodeAt(pos) == 10) {
                pos++
                value = value + "\n"
            }
            token = JP_LINE_BREAK
            return JP_LINE_BREAK
        }

        when (code) {
            123 -> {
                pos++
                token = 1
                return 1
            }
            125 -> {
                pos++
                token = 2
                return 2
            }
            91 -> {
                pos++
                token = 3
                return 3
            }
            93 -> {
                pos++
                token = 4
                return 4
            }
            58 -> {
                pos++
                token = 6
                return 6
            }
            44 -> {
                pos++
                token = 5
                return 5
            }
            34 -> {
                pos++
                value = scanString()
                token = JP_STRING
                return JP_STRING
            }
            47 -> {
                // 旧实现此处用 `pos - 1`，只在注释 value 上偏了一位；offset/length 不受影响。
                val start = pos - 1
                if (charCodeAt(pos + 1) == 47) {
                    pos += 2
                    while (pos < length) {
                        if (jpIsLineBreak(charCodeAt(pos))) break
                        pos++
                    }
                    value = text.substring(start, pos)
                    token = JP_LINE_COMMENT
                    return JP_LINE_COMMENT
                }
                if (charCodeAt(pos + 1) == 42) {
                    pos += 2
                    val safeLength = length - 1
                    var commentClosed = false
                    while (pos < safeLength) {
                        val ch = charCodeAt(pos)
                        if (ch == 42 && charCodeAt(pos + 1) == 47) {
                            pos += 2
                            commentClosed = true
                            break
                        }
                        pos++
                        if (jpIsLineBreak(ch)) {
                            if (ch == 13 && charCodeAt(pos) == 10) pos++
                        }
                    }
                    if (!commentClosed) {
                        pos++
                        scanError = 1
                    }
                    value = text.substring(start, pos)
                    token = JP_BLOCK_COMMENT
                    return JP_BLOCK_COMMENT
                }
                value = value + jpFromCharCode(code)
                pos++
                token = JP_UNKNOWN
                return JP_UNKNOWN
            }
            45 -> {
                value = value + jpFromCharCode(code)
                pos++
                if (pos == length || !jpIsDigit(charCodeAt(pos))) {
                    token = JP_UNKNOWN
                    return JP_UNKNOWN
                }
                value = value + scanNumber()
                token = JP_NUMBER
                return JP_NUMBER
            }
            in 48..57 -> {
                value = value + scanNumber()
                token = JP_NUMBER
                return JP_NUMBER
            }
            else -> {
                while (pos < length && jpIsUnknownContentCharacter(code)) {
                    pos++
                    code = charCodeAt(pos)
                }
                if (tokenOffset != pos) {
                    value = text.substring(tokenOffset, pos)
                    if (jpStrictEq(value, "true")) {
                        token = 8
                        return 8
                    }
                    if (jpStrictEq(value, "false")) {
                        token = 9
                        return 9
                    }
                    if (jpStrictEq(value, "null")) {
                        token = 7
                        return 7
                    }
                    token = JP_UNKNOWN
                    return JP_UNKNOWN
                }
                value = value + jpFromCharCode(code)
                pos++
                token = JP_UNKNOWN
                return JP_UNKNOWN
            }
        }
    }
}

// ---------------- parser（vscode-json-languageservice jsonParser.parse 的移植） ----------------

private class JsoncParser(private val document: dynamic, collectComments: Boolean) {
    private val text: dynamic = document.getText()
    private val textLength = text.length as Int
    private val scanner = JsoncScanner(text, textLength)
    private val problems: dynamic = js("[]")
    private val commentRanges: dynamic = if (collectComments) js("[]") else jpUndefined
    private var lastProblemOffset = -1
    private val keyPlaceholder: dynamic = jpNewStringNode(jpUndefined, 0, 0)

    fun parse(): dynamic {
        var root: dynamic = jpUndefined
        val token = scanNext()
        if (token != JP_EOF) {
            root = parseValue(root)
            if (jpIsUndefined(root)) {
                error("Expected a JSON object, array or literal.", 0)
            } else if (scanner.token != JP_EOF) {
                error("End of file expected.", 0)
            }
        }
        return jpBuildDocument(document, root, problems, commentRanges)
    }

    // ---------------- trivia / errors ----------------

    private fun scanNext(): Int {
        while (true) {
            val token = scanner.scan()
            checkScanError()
            when (token) {
                JP_LINE_COMMENT, JP_BLOCK_COMMENT -> {
                    if (!jpIsUndefined(commentRanges)) {
                        commentRanges.push(
                            jpMakeRange(
                                document,
                                scanner.tokenOffset,
                                scanner.tokenOffset + scanner.tokenLength
                            )
                        )
                    }
                }
                JP_TRIVIA, JP_LINE_BREAK -> {
                    // trivia：继续扫
                }
                else -> return token
            }
        }
    }

    private fun checkScanError() {
        when (scanner.scanError) {
            4 -> error("Invalid unicode sequence in string.", 260)
            5 -> error("Invalid escape character in string.", 261)
            3 -> error("Unexpected end of number.", 259)
            1 -> error("Unexpected end of comment.", 257)
            2 -> error("Unexpected end of string.", 258)
            6 -> error("Invalid characters in string. Control characters must be escaped.", 262)
        }
    }

    private fun diagnosticAt(
        message: String,
        code: Int,
        startOffset: Int,
        endOffset: Int,
        severity: Int
    ) {
        val problemCount = problems.length as Int
        if (problemCount == 0 || startOffset != lastProblemOffset) {
            val diagnostic = js("({})")
            diagnostic.range = jpMakeRange(document, startOffset, endOffset)
            diagnostic.message = message
            diagnostic.severity = severity
            diagnostic.code = code
            diagnostic.source = document.languageId
            problems.push(diagnostic)
            lastProblemOffset = startOffset
        }
    }

    private fun error(
        message: String,
        code: Int,
        node: dynamic = jpUndefined,
        skipUntilAfter: List<Int> = emptyList(),
        skipUntil: List<Int> = emptyList()
    ): dynamic {
        var start = scanner.tokenOffset
        var end = scanner.tokenOffset + scanner.tokenLength
        if (start == end && start > 0) {
            start--
            while (start > 0 && jpIsSpace(text.charAt(start))) start--
            end = start + 1
        }
        diagnosticAt(message, code, start, end, 1)
        if (!jpIsUndefined(node)) finalize(node, false)
        if (skipUntilAfter.isNotEmpty() || skipUntil.isNotEmpty()) {
            var token = scanner.token
            while (token != JP_EOF) {
                if (skipUntilAfter.contains(token)) {
                    scanNext()
                    break
                } else if (skipUntil.contains(token)) {
                    break
                }
                token = scanNext()
            }
        }
        return node
    }

    private fun finalize(node: dynamic, doScan: Boolean): dynamic {
        node.length = scanner.tokenOffset + scanner.tokenLength - (node.offset as Int)
        if (doScan) scanNext()
        return node
    }

    // ---------------- value parsers ----------------

    private fun parseValue(parent: dynamic): dynamic {
        var node = parseArray(parent)
        if (!jpIsUndefined(node)) return node
        node = parseObject(parent)
        if (!jpIsUndefined(node)) return node
        node = parseString(parent)
        if (!jpIsUndefined(node)) return node
        node = parseNumber(parent)
        if (!jpIsUndefined(node)) return node
        return parseLiteral(parent)
    }

    private fun parseArray(parent: dynamic): dynamic {
        if (scanner.token != 3) return jpUndefined
        val node = jpNewArrayNode(parent, scanner.tokenOffset)
        scanNext()
        var needsComma = false
        while (scanner.token != 4 && scanner.token != JP_EOF) {
            if (scanner.token == 5) {
                if (!needsComma) {
                    error("Value expected", 516)
                }
                val commaOffset = scanner.tokenOffset
                scanNext()
                if (scanner.token == 4) {
                    if (needsComma) {
                        diagnosticAt("Trailing comma", 519, commaOffset, commaOffset + 1, 1)
                    }
                    continue
                }
            } else if (needsComma) {
                error("Expected comma", 514)
            }
            val item = parseValue(node)
            if (jpIsUndefined(item)) {
                error("Value expected", 516, jpUndefined, emptyList(), listOf(4, 5))
            } else {
                node.items.push(item)
            }
            needsComma = true
        }
        if (scanner.token != 4) {
            return error("Expected comma or closing bracket", 517, node)
        }
        return finalize(node, true)
    }

    private fun parseProperty(parent: dynamic, keysSeen: dynamic): dynamic {
        val node = jpNewPropertyNode(parent, scanner.tokenOffset)
        var key = parseString(node)
        if (jpIsUndefined(key)) {
            if (scanner.token == JP_UNKNOWN) {
                error("Property keys must be doublequoted", 528)
                val keyNode = jpNewStringNode(node, scanner.tokenOffset, scanner.tokenLength)
                keyNode.value = scanner.value
                key = keyNode
                scanNext()
            } else {
                return jpUndefined
            }
        }
        node.keyNode = key
        jpRefreshPropertyChildren(node)
        if (!jpStrictEq(key.value, "//")) {
            val seen = keysSeen[key.value]
            if (jpTruthy(seen)) {
                diagnosticAt(
                    "Duplicate object key",
                    520,
                    node.keyNode.offset as Int,
                    (node.keyNode.offset as Int) + (node.keyNode.length as Int),
                    2
                )
                if (jpIsObject(seen)) {
                    diagnosticAt(
                        "Duplicate object key",
                        520,
                        seen.keyNode.offset as Int,
                        (seen.keyNode.offset as Int) + (seen.keyNode.length as Int),
                        2
                    )
                }
                keysSeen[key.value] = true
            } else {
                keysSeen[key.value] = node
            }
        }
        if (scanner.token == 6) {
            node.colonOffset = scanner.tokenOffset
            scanNext()
        } else {
            error("Colon expected", 515)
            if (scanner.token == JP_STRING) {
                val keyEndLine = document.positionAt(
                    (key.offset as Int) + (key.length as Int)
                ).line as Int
                val nextLine = document.positionAt(scanner.tokenOffset).line as Int
                if (keyEndLine < nextLine) {
                    node.length = key.length
                    return node
                }
            }
        }
        val value = parseValue(node)
        if (jpIsUndefined(value)) {
            return error("Value expected", 516, node, emptyList(), listOf(2, 5))
        }
        node.valueNode = value
        jpRefreshPropertyChildren(node)
        node.length = (value.offset as Int) + (value.length as Int) - (node.offset as Int)
        return node
    }

    private fun parseObject(parent: dynamic): dynamic {
        if (scanner.token != 1) return jpUndefined
        val node = jpNewObjectNode(parent, scanner.tokenOffset)
        val keysSeen = js("Object.create(null)")
        scanNext()
        var needsComma = false
        while (scanner.token != 2 && scanner.token != JP_EOF) {
            if (scanner.token == 5) {
                if (!needsComma) {
                    error("Property expected", 513)
                }
                val commaOffset = scanner.tokenOffset
                scanNext()
                if (scanner.token == 2) {
                    if (needsComma) {
                        diagnosticAt("Trailing comma", 519, commaOffset, commaOffset + 1, 1)
                    }
                    continue
                }
            } else if (needsComma) {
                error("Expected comma", 514)
            }
            val property = parseProperty(node, keysSeen)
            if (jpIsUndefined(property)) {
                error("Property expected", 513, jpUndefined, emptyList(), listOf(2, 5))
            } else {
                node.properties.push(property)
            }
            needsComma = true
        }
        if (scanner.token != 2) {
            return error("Expected comma or closing brace", 518, node)
        }
        return finalize(node, true)
    }

    private fun parseString(parent: dynamic): dynamic {
        if (scanner.token != JP_STRING) return jpUndefined
        val node = jpNewStringNode(parent, scanner.tokenOffset, 0)
        node.value = scanner.value
        return finalize(node, true)
    }

    private fun parseNumber(parent: dynamic): dynamic {
        if (scanner.token != JP_NUMBER) return jpUndefined
        val node = jpNewNumberNode(parent, scanner.tokenOffset)
        if (scanner.scanError == 0) {
            val tokenValue = scanner.value
            try {
                val numberValue = jpJsonParseJs.parse(tokenValue)
                if (!jpIsNumber(numberValue)) {
                    return error("Invalid number format.", 0, node)
                }
                node.value = numberValue
            } catch (e: dynamic) {
                return error("Invalid number format.", 0, node)
            }
            node.isInteger = jpStrictEq(tokenValue.indexOf("."), -1)
        }
        return finalize(node, true)
    }

    private fun parseLiteral(parent: dynamic): dynamic {
        when (scanner.token) {
            7 -> return finalize(jpNewNullNode(parent, scanner.tokenOffset), true)
            8 -> return finalize(jpNewBooleanNode(parent, scanner.tokenOffset, true), true)
            9 -> return finalize(jpNewBooleanNode(parent, scanner.tokenOffset, false), true)
        }
        return jpUndefined
    }
}

// ---------------- document ----------------

private fun jpMakeRange(document: dynamic, startOffset: Int, endOffset: Int): dynamic {
    val range = js("({})")
    range.start = document.positionAt(startOffset)
    range.end = document.positionAt(endOffset)
    return range
}

/** 旧 bundle 的 Diagnostic 错误码（vscode-json-languageservice ErrorCode）→ jsonc-parser ParseErrorCode。 */
private fun jpMapErrorCode(code: Int): Int = when (code) {
    257 -> 11 // UnexpectedEndOfComment
    258 -> 12 // UnexpectedEndOfString
    259 -> 13 // UnexpectedEndOfNumber
    260 -> 14 // InvalidUnicode
    261 -> 15 // InvalidEscapeCharacter
    262 -> 16 // InvalidCharacter
    513 -> 3  // PropertyExpected -> PropertyNameExpected
    514 -> 6  // CommaExpected
    515 -> 5  // ColonExpected
    516 -> 4  // ValueExpected
    517 -> 8  // CommaOrCloseBacketExpected -> CloseBracketExpected
    518 -> 7  // CommaOrCloseBraceExpected -> CloseBraceExpected
    519 -> 0  // TrailingComma：老 jsonc-parser 无对应码
    520 -> 19 // DuplicateKey（新 jsonc-parser 的码）
    521 -> 10 // CommentNotPermitted -> InvalidCommentToken
    528 -> 17 // PropertyKeysMustBeDoublequoted
    else -> code
}

private fun jpBuildParseErrors(document: dynamic, syntaxErrors: dynamic): dynamic {
    val errors = js("[]")
    val count = syntaxErrors.length as Int
    var i = 0
    while (i < count) {
        val diagnostic = syntaxErrors[i]
        val start = document.offsetAt(diagnostic.range.start) as Int
        val end = document.offsetAt(diagnostic.range.end) as Int
        val item = js("({})")
        item.error = jpMapErrorCode(diagnostic.code as Int)
        item.offset = start
        item.length = end - start
        errors.push(item)
        i++
    }
    return errors
}

private fun jpBuildDocument(
    document: dynamic,
    root: dynamic,
    syntaxErrors: dynamic,
    comments: dynamic
): dynamic {
    val jsonDocument = js("({})")
    jsonDocument.root = root
    jsonDocument.syntaxErrors = syntaxErrors
    jsonDocument.comments = comments
    jsonDocument.errors = jpBuildParseErrors(document, syntaxErrors)
    jsonDocument.getNodeFromOffset = { offset: dynamic, includeRightBound: dynamic ->
        if (jpIsUndefined(jsonDocument.root)) {
            jpUndefined
        } else {
            jsonFindNodeAtOffset(jsonDocument.root, offset as Int, jpTruthy(includeRightBound))
        }
    }
    return jsonDocument
}

/** `jsonc-parser` 旧版 findNodeAtOffset：先 contains，再按 children 顺序深度优先。 */
private fun jpContains(node: dynamic, offset: Int, includeRightBound: Boolean): Boolean {
    val start = node.offset as Int
    val end = start + (node.length as Int)
    return (offset >= start && offset < end) || (includeRightBound && offset == end)
}

private fun jpChildrenOf(node: dynamic): dynamic {
    val type = node.type
    if (jpStrictEq(type, "object")) return node.properties
    if (jpStrictEq(type, "array")) return node.items
    if (jpStrictEq(type, "property")) {
        val children = js("[]")
        children.push(node.keyNode)
        if (!jpIsUndefined(node.valueNode)) children.push(node.valueNode)
        return children
    }
    return js("[]")
}

private fun jpFindNodeAtOffset(
    node: dynamic,
    offset: Int,
    includeRightBound: Boolean
): dynamic {
    if (jpIsUndefined(node)) return jpUndefined
    if (!jpContains(node, offset, includeRightBound)) return jpUndefined
    val children = jpChildrenOf(node)
    val count = children.length as Int
    var i = 0
    while (i < count) {
        val child = children[i]
        if ((child.offset as Int) > offset) break
        val item = jpFindNodeAtOffset(child, offset, includeRightBound)
        if (!jpIsUndefined(item)) return item
        i++
    }
    return node
}

/**
 * `findNodeAtOffset(root, offset, includeRightBound)`：
 * 与旧 bundle 导出的 jsonc-parser 实现相同的包含判断与遍历顺序。
 */
fun jsonFindNodeAtOffset(node: dynamic, offset: Int, includeRightBound: Boolean): dynamic =
    jpFindNodeAtOffset(node, offset, includeRightBound)

/** 解析 TextDocument（其 getText/positionAt 会被调用），返回与旧 JSONDocument 对齐的对象。 */
fun parseJsonDocument(document: dynamic): dynamic = JsoncParser(document, true).parse()
