package ca.kotlin.json

import ca.kotlin.core.*


/**
 * modules/JSONLanguageService.js 中 `TextDocument`（vscode-languageserver-textdocument）的
 * Kotlin/JS 版本 —— JSONLanguageService 迁移阶段 1。
 *
 * 复刻对象的行为，而不是类结构：
 *   TextDocument.create(uri, languageId, version, text)   // 与 CAIntelliSenseModule.kt 现有调用一致
 *   new TextDocument(uri, languageId, version, text)      // 额外支持 JS 直接构造
 *   getText(range?) / positionAt(offset) / offsetAt(position) / lineCount
 *   getLineOffsets() / ensureBeforeEOL() / update(changes, version)
 *
 * 字段名保持 `_uri` / `_languageId` / `_version` / `_content` / `_lineOffsets`，
 * 与旧 bundle 里的 FullTextDocument 完全一致，方便 parity 对比和后续阶段接续。
 *
 * 实现约定：
 * - 对外用一层薄 JS wrapper（prototype 方法 + getter），逻辑全部在 Kotlin 里；
 * - 只用 ES5 能力（Rhino 可跑）：function / Object.defineProperty / String.prototype.substring；
 * - 行偏移与 offset 一律按 UTF-16 code unit 计算，与 JS String API 一致。
 */

private val tdUndefined: dynamic = js("undefined")

private val tdStrictEqJs: dynamic = js("function (a, b) { return a === b ? 1 : 0; }")
private fun tdStrictEq(a: dynamic, b: dynamic): Boolean = (tdStrictEqJs(a, b) as Int) != 0

private val tdTruthyJs: dynamic = js("function (v) { return !!v ? 1 : 0; }")
private fun tdTruthy(v: dynamic): Boolean = (tdTruthyJs(v) as Int) != 0

private val tdIsUndefinedJs: dynamic = js("function (v) { return v === undefined ? 1 : 0; }")
private fun tdIsUndefined(v: dynamic): Boolean = (tdIsUndefinedJs(v) as Int) != 0

private val tdIsNullJs: dynamic = js("function (v) { return v === null ? 1 : 0; }")
private fun tdIsNull(v: dynamic): Boolean = (tdIsNullJs(v) as Int) != 0

private val tdTypeOfJs: dynamic = js("function (v) { return typeof v; }")
private fun tdIsString(v: dynamic): Boolean = tdStrictEq(tdTypeOfJs(v), "string")
private fun tdIsNumber(v: dynamic): Boolean = tdStrictEq(tdTypeOfJs(v), "number")

private val tdThrowJs: dynamic = js("function (m) { throw new Error(m); }")
private fun tdThrowError(message: String): dynamic {
    tdThrowJs(message)
    return tdUndefined
}

private fun tdCharCodeAt(content: dynamic, index: Int): Int {
    if (index < 0) return -1
    val length = content.length as Int
    if (index >= length) return -1
    return content.charCodeAt(index) as Int
}

private fun tdIsEOL(code: Int): Boolean = code == 13 || code == 10

// ---------------- line offsets ----------------

private fun tdComputeLineOffsets(content: dynamic): dynamic {
    val length = content.length as Int
    val result = js("[]")
    result.push(0)
    var i = 0
    while (i < length) {
        val ch = tdCharCodeAt(content, i)
        if (tdIsEOL(ch)) {
            if (ch == 13 && i + 1 < length && tdCharCodeAt(content, i + 1) == 10) {
                i++
            }
            result.push(i + 1)
        }
        i++
    }
    return result
}

private fun tdGetLineOffsets(self: dynamic): dynamic {
    val cached = self._lineOffsets
    if (!tdIsUndefined(cached)) return cached
    val offsets = tdComputeLineOffsets(self._content)
    self._lineOffsets = offsets
    return offsets
}

private fun tdEnsureBeforeEol(self: dynamic, offsetIn: Int, lineOffset: Int): Int {
    var offset = offsetIn
    val content = self._content
    while (offset > lineOffset) {
        val ch = tdCharCodeAt(content, offset - 1)
        if (!tdIsEOL(ch)) break
        offset--
    }
    return offset
}

// ---------------- positions ----------------

private fun tdPositionAt(self: dynamic, offsetIn: Int): dynamic {
    val content = self._content
    val length = content.length as Int
    var offset = offsetIn
    if (offset > length) offset = length
    if (offset < 0) offset = 0

    val lineOffsets = tdGetLineOffsets(self)
    val lineCount = lineOffsets.length as Int
    if (lineCount == 0) {
        val empty = js("({})")
        empty.line = 0
        empty.character = offset
        return empty
    }

    var low = 0
    var high = lineCount
    while (low < high) {
        val mid = (low + high) / 2
        if ((lineOffsets[mid] as Int) > offset) {
            high = mid
        } else {
            low = mid + 1
        }
    }
    val line = low - 1
    val lineOffset = lineOffsets[line] as Int
    offset = tdEnsureBeforeEol(self, offset, lineOffset)

    val position = js("({})")
    position.line = line
    position.character = offset - lineOffset
    return position
}

private fun tdOffsetAt(self: dynamic, position: dynamic): Int {
    val content = self._content
    val length = content.length as Int
    val lineOffsets = tdGetLineOffsets(self)
    val lineCount = lineOffsets.length as Int
    val line = position.line as Int

    if (line >= lineCount) return length
    if (line < 0) return 0

    val lineOffset = lineOffsets[line] as Int
    val character = position.character as Int
    if (character <= 0) return lineOffset

    val nextLineOffset = if (line + 1 < lineCount) lineOffsets[line + 1] as Int else length
    var offset = lineOffset + character
    if (offset > nextLineOffset) offset = nextLineOffset
    return tdEnsureBeforeEol(self, offset, lineOffset)
}

// ---------------- update helpers ----------------

private fun tdIsIncremental(change: dynamic): Boolean {
    if (tdIsUndefined(change) || tdIsNull(change)) return false
    if (!tdIsString(change.text)) return false
    if (tdIsUndefined(change.range)) return false
    if (!tdIsUndefined(change.rangeLength) && !tdIsNumber(change.rangeLength)) return false
    return true
}

private fun tdIsFull(change: dynamic): Boolean {
    if (tdIsUndefined(change) || tdIsNull(change)) return false
    if (!tdIsString(change.text)) return false
    if (!tdIsUndefined(change.range)) return false
    if (!tdIsUndefined(change.rangeLength)) return false
    return true
}

private fun tdWellformedRange(range: dynamic): dynamic {
    val start = range.start
    val end = range.end
    val startLine = start.line as Int
    val endLine = end.line as Int
    val startCharacter = start.character as Int
    val endCharacter = end.character as Int
    if (startLine > endLine || (startLine == endLine && startCharacter > endCharacter)) {
        val reversed = js("({})")
        reversed.start = end
        reversed.end = start
        return reversed
    }
    return range
}

// ---------------- bridge ----------------

private fun tdMakeBridge(): dynamic {
    val bridge = js("({})")

    bridge.init = { self: dynamic, uri: dynamic, languageId: dynamic, version: dynamic, content: dynamic ->
        self._uri = uri
        self._languageId = languageId
        self._version = version
        self._content = content
        self._lineOffsets = tdUndefined
    }

    bridge.getUri = { self: dynamic -> self._uri }
    bridge.getLanguageId = { self: dynamic -> self._languageId }
    bridge.getVersion = { self: dynamic -> self._version }
    bridge.getLineCount = { self: dynamic -> tdGetLineOffsets(self).length as Int }

    bridge.getText = { self: dynamic, range: dynamic ->
        if (tdTruthy(range)) {
            val start = tdOffsetAt(self, range.start)
            val end = tdOffsetAt(self, range.end)
            self._content.substring(start, end)
        } else {
            self._content
        }
    }

    bridge.update = { self: dynamic, changes: dynamic, version: dynamic ->
        val count = changes.length as Int
        var i = 0
        while (i < count) {
            val change = changes[i]
            if (tdIsIncremental(change)) {
                val range = tdWellformedRange(change.range)
                val startOffset = tdOffsetAt(self, range.start)
                val endOffset = tdOffsetAt(self, range.end)
                self._content = self._content.substring(0, startOffset) +
                    change.text +
                    self._content.substring(endOffset, self._content.length as Int)
                self._lineOffsets = tdUndefined
            } else if (tdIsFull(change)) {
                self._content = change.text
                self._lineOffsets = tdUndefined
            } else {
                tdThrowError("Unknown change event received")
            }
            i++
        }
        self._version = version
        tdUndefined
    }

    bridge.getLineOffsets = { self: dynamic -> tdGetLineOffsets(self) }
    bridge.positionAt = { self: dynamic, offset: dynamic -> tdPositionAt(self, offset as Int) }
    bridge.offsetAt = { self: dynamic, position: dynamic -> tdOffsetAt(self, position) }
    bridge.ensureBeforeEOL = { self: dynamic, offset: dynamic, lineOffset: dynamic ->
        tdEnsureBeforeEol(self, offset as Int, lineOffset as Int)
    }

    return bridge
}

/**
 * 构造 JS 可见的 TextDocument 构造函数（含 `.create` 静态入口）。
 *
 * prototype 方法只做 `bridge.xxx(this, ...)` 转发，避免在 JS 字符串里引用 Kotlin 局部变量。
 */
private val tdCtorFactory: dynamic = js(
    """
    (function (bridge) {
        function TextDocument(uri, languageId, version, content) {
            if (!(this instanceof TextDocument)) {
                return new TextDocument(uri, languageId, version, content);
            }
            bridge.init(this, uri, languageId, version, content);
        }
        TextDocument.create = function (uri, languageId, version, content) {
            return new TextDocument(uri, languageId, version, content);
        };
        TextDocument.prototype.getText = function (range) {
            return bridge.getText(this, range);
        };
        TextDocument.prototype.update = function (changes, version) {
            return bridge.update(this, changes, version);
        };
        TextDocument.prototype.getLineOffsets = function () {
            return bridge.getLineOffsets(this);
        };
        TextDocument.prototype.positionAt = function (offset) {
            return bridge.positionAt(this, offset);
        };
        TextDocument.prototype.offsetAt = function (position) {
            return bridge.offsetAt(this, position);
        };
        TextDocument.prototype.ensureBeforeEOL = function (offset, lineOffset) {
            return bridge.ensureBeforeEOL(this, offset, lineOffset);
        };
        Object.defineProperty(TextDocument.prototype, 'uri', {
            enumerable: false,
            configurable: true,
            get: function () { return bridge.getUri(this); }
        });
        Object.defineProperty(TextDocument.prototype, 'languageId', {
            enumerable: false,
            configurable: true,
            get: function () { return bridge.getLanguageId(this); }
        });
        Object.defineProperty(TextDocument.prototype, 'version', {
            enumerable: false,
            configurable: true,
            get: function () { return bridge.getVersion(this); }
        });
        Object.defineProperty(TextDocument.prototype, 'lineCount', {
            enumerable: false,
            configurable: true,
            get: function () { return bridge.getLineCount(this); }
        });
        return TextDocument;
    })
    """
)

/** 供 JsonLanguageServiceCore 组装阶段 1 API：`TextDocument`。 */
fun buildJsonTextDocumentCtor(): dynamic = tdCtorFactory(tdMakeBridge())
