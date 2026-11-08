package ca.kotlin

/**
 * JSONLanguageService 迁移阶段 3：JSON Schema 驱动的 `doComplete`。
 *
 * 对照实现：`modules/JSONLanguageService.js` 里的 `JSONCompletion.doComplete`
 * （vscode-json-languageservice）。CA 只读取补全项的
 * `label` / `insertText` / `textEdit.range.start|end.character` / `textEdit.newText` /
 * `detail` / `documentation`，因此这里以这几项与旧 bundle 对齐为主要目标：
 *
 *  - 对象属性名补全（properties / propertyNames，按光标处的 key 前缀覆盖）；
 *  - 值补全（enum / const / default / examples / boolean / null / filler `{}` `[]`）；
 *  - oneOf / anyOf / allOf 的候选合并；
 *  - 数组 items（含 tuple items）补全；`$ref` 在匹配与生成候选时按本地指针解引用；
 *  - textEdit 使用 document.positionAt()，保证 CA 的 `range.start.character` 读取正确；
 *  - detail / documentation 与旧实现的取值顺序一致（completionDetail、description、
 *    enumDescriptions、New object / New array 等）。
 *
 * 明确不做的事（按迁移计划）：
 *  - 不调用 `MapScript.loadModule`、不注册 `JSONLanguageService`；
 *  - 不修改 `CAIntelliSenseModule.kt` 或旧 vendor；
 *  - 不实现 vendor 的 contributions / `$schema` 自动补全 / Markdown 文档能力
 *    （CA 的 options 不带 clientCapabilities，旧 bundle 在 CA 场景下同样不会走这些分支）。
 *
 * 注意：本文件只负责补全与阶段 3 的 API 组装；validation / matching 直接复用阶段 2 的
 * `JsonSchemaRegistry`、`jsonMatchingSchemas`，parser 复用阶段 1 的 `parseJsonDocument`。
 */

// ---------------- JS 语义 helper ----------------

private fun jcTruthy(v: dynamic): Boolean = jsxTruthy(v)

private fun jcIsUndefined(v: dynamic): Boolean = jsxIsUndefined(v)

private fun jcStrictEq(a: dynamic, b: dynamic): Boolean = jsxStrictEq(a, b)

private fun jcIsArray(v: dynamic): Boolean = jsxIsArray(v)

private fun jcIsObject(v: dynamic): Boolean = jsxIsObject(v)

private fun jcIsString(v: dynamic): Boolean = jsxIsString(v)

private val jcLengthJs: dynamic = js("function (v) { return v.length | 0; }")

private fun jcLength(v: dynamic): Int = jcLengthJs(v) as Int

private fun jcKeys(v: dynamic): dynamic = jsxKeys(v)

private fun jcString(v: dynamic): String = jsxString(v) as String

private fun jcJsonStringify(v: dynamic): dynamic = jsxJsonStringify(v)

/**
 * 补全器用的 JSONC scanner / 文本生成 helper。
 *
 * 全部逻辑放在一个 JS 工厂里，Kotlin 侧只传参数（遵守“js("...") 里不引用 Kotlin 局部变量”），
 * 并且保持 ES5 写法，方便以后在 Rhino / QuickJS 上复用。
 *
 * token 编号与旧 bundle 的 jsonc-parser SyntaxKind 对齐：
 *   1 {, 2 }, 3 [, 4 ], 5 ,, 6 :, 10 string, 11 number, 12 line comment,
 *   13 block comment, 16 unknown, 17 EOF。
 */
private val jcHelpers: dynamic = js(
    """
    (function () {
        function isWhiteSpace(code) {
            return code === 32 || code === 9;
        }
        function isLineBreak(code) {
            return code === 10 || code === 13;
        }
        function isDigit(code) {
            return code >= 48 && code <= 57;
        }
        function isUnknownContentCharacter(code) {
            if (isWhiteSpace(code) || isLineBreak(code)) return false;
            switch (code) {
                case 125:
                case 93:
                case 123:
                case 91:
                case 34:
                case 58:
                case 44:
                case 47:
                    return false;
            }
            return true;
        }
        function scanNumber(text, pos) {
            var len = text.length | 0;
            var start = pos;
            if (text.charCodeAt(pos) === 48) {
                pos++;
            } else {
                pos++;
                while (pos < len && isDigit(text.charCodeAt(pos))) pos++;
            }
            if (pos < len && text.charCodeAt(pos) === 46) {
                pos++;
                if (pos < len && isDigit(text.charCodeAt(pos))) {
                    pos++;
                    while (pos < len && isDigit(text.charCodeAt(pos))) pos++;
                }
                return pos;
            }
            var end = pos;
            if (pos < len && (text.charCodeAt(pos) === 69 || text.charCodeAt(pos) === 101)) {
                pos++;
                if (pos < len && (text.charCodeAt(pos) === 43 || text.charCodeAt(pos) === 45)) pos++;
                if (pos < len && isDigit(text.charCodeAt(pos))) {
                    pos++;
                    while (pos < len && isDigit(text.charCodeAt(pos))) pos++;
                    end = pos;
                }
            }
            return Math.max(end, pos);
        }
        function scanRaw(text, position) {
            var pos = position | 0;
            var len = text.length | 0;
            if (pos >= len) return { token: 17, offset: len, length: 0 };
            var start = pos;
            var code = text.charCodeAt(pos);
            if (isWhiteSpace(code)) {
                do {
                    pos++;
                    code = text.charCodeAt(pos);
                } while (pos < len && isWhiteSpace(code));
                return { token: 15, offset: start, length: pos - start };
            }
            if (isLineBreak(code)) {
                pos++;
                if (code === 13 && text.charCodeAt(pos) === 10) pos++;
                return { token: 14, offset: start, length: pos - start };
            }
            switch (code) {
                case 123: return { token: 1, offset: start, length: 1 };
                case 125: return { token: 2, offset: start, length: 1 };
                case 91: return { token: 3, offset: start, length: 1 };
                case 93: return { token: 4, offset: start, length: 1 };
                case 58: return { token: 6, offset: start, length: 1 };
                case 44: return { token: 5, offset: start, length: 1 };
                case 34:
                    pos++;
                    while (pos < len) {
                        var quote = text.charCodeAt(pos);
                        if (quote === 92) { pos += 2; continue; }
                        if (quote === 34) { pos++; break; }
                        pos++;
                    }
                    if (pos > len) pos = len;
                    return { token: 10, offset: start, length: pos - start };
                case 47:
                    if (text.charCodeAt(pos + 1) === 47) {
                        pos += 2;
                        while (pos < len) {
                            if (isLineBreak(text.charCodeAt(pos))) break;
                            pos++;
                        }
                        return { token: 12, offset: start, length: pos - start };
                    }
                    if (text.charCodeAt(pos + 1) === 42) {
                        pos += 2;
                        var safeLength = len - 1;
                        while (pos < safeLength) {
                            var commentCode = text.charCodeAt(pos);
                            if (commentCode === 42 && text.charCodeAt(pos + 1) === 47) {
                                pos += 2;
                                return { token: 13, offset: start, length: pos - start };
                            }
                            pos++;
                        }
                        pos = len;
                        return { token: 13, offset: start, length: pos - start };
                    }
                    return { token: 16, offset: start, length: 1 };
                case 45:
                    pos++;
                    if (pos === len || !isDigit(text.charCodeAt(pos))) {
                        return { token: 16, offset: start, length: pos - start };
                    }
                    pos = scanNumber(text, pos);
                    return { token: 11, offset: start, length: pos - start };
                case 48:
                case 49:
                case 50:
                case 51:
                case 52:
                case 53:
                case 54:
                case 55:
                case 56:
                case 57:
                    pos = scanNumber(text, pos);
                    return { token: 11, offset: start, length: pos - start };
                default:
                    while (pos < len && isUnknownContentCharacter(code)) {
                        pos++;
                        code = text.charCodeAt(pos);
                    }
                    if (start !== pos) {
                        var word = text.substring(start, pos);
                        if (word === 'true') return { token: 8, offset: start, length: pos - start };
                        if (word === 'false') return { token: 9, offset: start, length: pos - start };
                        if (word === 'null') return { token: 7, offset: start, length: pos - start };
                        return { token: 16, offset: start, length: pos - start };
                    }
                    return { token: 16, offset: start, length: 1 };
            }
        }
        function scan(text, position, ignoreTrivia) {
            var token = scanRaw(text, position);
            if (ignoreTrivia) {
                while (token.token >= 12 && token.token <= 15 && token.token !== 17) {
                    token = scanRaw(text, token.offset + token.length);
                }
            }
            return token;
        }
        function isInComment(text, start, offset) {
            var token = scan(text, start, false);
            var guard = 0;
            while (token.token !== 17 && token.offset + token.length < offset) {
                token = scan(text, token.offset + token.length, false);
                guard++;
                if (guard > 100000) break;
            }
            return (token.token === 12 || token.token === 13) && token.offset <= offset ? 1 : 0;
        }
        function evaluateSeparatorAfter(text, offset) {
            var token = scan(text, offset, true);
            switch (token.token) {
                case 5:
                case 2:
                case 4:
                case 17:
                    return '';
                default:
                    return ',';
            }
        }
        function findItemAtOffset(node, text, offset) {
            var children = node.items;
            var len = children.length | 0;
            for (var i = len - 1; i >= 0; i--) {
                var child = children[i];
                var end = (child.offset | 0) + (child.length | 0);
                if (offset > end) {
                    var token = scan(text, end, true);
                    if (token.token === 5 && offset >= token.offset + token.length) return i + 1;
                    return i;
                } else if (offset >= (child.offset | 0)) {
                    return i;
                }
            }
            return 0;
        }
        function plainText(text) {
            return text.replace(/[\\\$\}]/g, '\\$&');
        }
        function insertTextForValue(value, separatorAfter) {
            var text = JSON.stringify(value, null, '\t');
            if (text === '{}') return '{$1}' + separatorAfter;
            if (text === '[]') return '[$1]' + separatorAfter;
            return plainText(text + separatorAfter);
        }
        function insertTextForGuessedValue(value, separatorAfter) {
            var kind = typeof value;
            if (kind === 'object') {
                if (value === null) return '\u0024{1:null}' + separatorAfter;
                return insertTextForValue(value, separatorAfter);
            }
            if (kind === 'string') {
                var snippetValue = JSON.stringify(value);
                snippetValue = snippetValue.substr(1, snippetValue.length - 2);
                return '"' + '\u0024{1:' + plainText(snippetValue) + '}"' + separatorAfter;
            }
            if (kind === 'number' || kind === 'boolean') {
                return '\u0024{1:' + JSON.stringify(value) + '}' + separatorAfter;
            }
            return insertTextForValue(value, separatorAfter);
        }
        function stringifySnippet(value, indent) {
            var replacer = function (item) {
                if (typeof item === 'string' && item.charAt(0) === '^') return item.substr(1);
                return JSON.stringify(item);
            };
            function walk(node, currentIndent) {
                if (node !== null && typeof node === 'object') {
                    var newIndent = currentIndent + '\t';
                    var i;
                    if (Array.isArray(node)) {
                        if (node.length === 0) return '[]';
                        var arrayText = '[\n';
                        for (i = 0; i < node.length; i++) {
                            arrayText += newIndent + walk(node[i], newIndent);
                            if (i < node.length - 1) arrayText += ',';
                            arrayText += '\n';
                        }
                        arrayText += currentIndent + ']';
                        return arrayText;
                    }
                    var keys = Object.keys(node);
                    if (keys.length === 0) return '{}';
                    var objectText = '{\n';
                    for (i = 0; i < keys.length; i++) {
                        objectText += newIndent + JSON.stringify(keys[i]) + ': ' + walk(node[keys[i]], newIndent);
                        if (i < keys.length - 1) objectText += ',';
                        objectText += '\n';
                    }
                    objectText += currentIndent + '}';
                    return objectText;
                }
                return replacer(node);
            }
            return walk(value, indent);
        }
        function insertTextForSnippetValue(value, separatorAfter) {
            return stringifySnippet(value, '') + separatorAfter;
        }
        function createMap() {
            var values = Object.create(null);
            return {
                get: function (key) { return values[key]; },
                set: function (key, value) { values[key] = value; },
                has: function (key) { return Object.prototype.hasOwnProperty.call(values, key); }
            };
        }
        function normalizeLabel(label, proposed) {
            label = String(label).replace(/[\n]/g, '\u21b5');
            if (label.length > 60) {
                var shortened = label.substr(0, 57).trim() + '...';
                if (!proposed.has(shortened)) label = shortened;
            }
            return label;
        }
        function substr(text, start, length) {
            return text.substr(start, length);
        }
        function charAt(text, index) {
            if (index < 0 || index >= text.length) return '';
            return text.charAt(index);
        }
        function labelTextForMatchingNode(node, text) {
            var type = node.type;
            if (type === 'array') return '[]';
            if (type === 'object') return '{}';
            return text.substr(node.offset, node.length);
        }
        function insertTextForMatchingNode(node, text, separatorAfter) {
            var type = node.type;
            if (type === 'array') return insertTextForValue([], separatorAfter);
            if (type === 'object') return insertTextForValue({}, separatorAfter);
            return plainText(text.substr(node.offset, node.length) + separatorAfter);
        }
        function extendedRegExp(pattern) {
            var flags = '';
            if (pattern.indexOf('(?i)') === 0) {
                pattern = pattern.substring(4);
                flags = 'i';
            }
            try {
                return new RegExp(pattern, flags + 'u');
            } catch (e) {
                try {
                    return new RegExp(pattern, flags);
                } catch (e2) {
                    return undefined;
                }
            }
        }
        function matchesPattern(pattern, text) {
            if (typeof pattern !== 'string') return 0;
            var regex = extendedRegExp(pattern);
            if (!regex) return 0;
            return regex.test(text) ? 1 : 0;
        }
        return {
            isInComment: isInComment,
            evaluateSeparatorAfter: evaluateSeparatorAfter,
            findItemAtOffset: findItemAtOffset,
            insertTextForValue: insertTextForValue,
            insertTextForGuessedValue: insertTextForGuessedValue,
            insertTextForSnippetValue: insertTextForSnippetValue,
            createMap: createMap,
            normalizeLabel: normalizeLabel,
            substr: substr,
            charAt: charAt,
            labelTextForMatchingNode: labelTextForMatchingNode,
            insertTextForMatchingNode: insertTextForMatchingNode,
            matchesPattern: matchesPattern
        };
    })()
    """
)

private fun jcText(document: dynamic): String = jcString(document.getText())

private fun jcNewNodeRange(document: dynamic, startOffset: Int, endOffset: Int): dynamic {
    val range = jsxNewObject()
    range.start = document.positionAt(startOffset)
    range.end = document.positionAt(endOffset)
    return range
}

private fun jcNewTextEdit(
    document: dynamic,
    startOffset: Int,
    endOffset: Int,
    newText: dynamic
): dynamic {
    val textEdit = jsxNewObject()
    textEdit.range = jcNewNodeRange(document, startOffset, endOffset)
    textEdit.newText = newText
    return textEdit
}

// ---------------- node helpers ----------------

private fun jcIsLeafNode(node: dynamic): Boolean {
    if (!jcTruthy(node)) return false
    val type = node.type
    return jcStrictEq(type, "string") || jcStrictEq(type, "number") ||
        jcStrictEq(type, "boolean") || jcStrictEq(type, "null")
}

private fun jcNodeOffset(node: dynamic): Int = node.offset as Int

private fun jcNodeEnd(node: dynamic): Int = jcNodeOffset(node) + (node.length as Int)

private fun jcColonOffset(node: dynamic): Int {
    val colon = node.colonOffset
    return if (jcTruthy(colon)) colon as Int else 0
}

private fun jcNodeAt(jsonDocument: dynamic, offset: Int): dynamic {
    val getNode = jsonDocument.getNodeFromOffset
    if (jcStrictEq(jsxTypeof(getNode), "function")) {
        return getNode(offset, true)
    }
    val root = jsonDocument.root
    if (!jcTruthy(root)) return jsxUndefined
    return jsonFindNodeAtOffset(root, offset, true)
}

/** 沿节点树递归，把属性名等于 `key` 且值为对象的 valueNode 交给 callback。 */
private fun jcWalkObjectProperties(
    node: dynamic,
    key: dynamic,
    excludeProperty: dynamic,
    callback: (dynamic) -> Unit
) {
    if (!jcTruthy(node)) return
    val type = node.type
    if (jcStrictEq(type, "object")) {
        val properties = node.properties
        var index = 0
        while (index < jcLength(properties)) {
            val property = properties[index]
            if ((jcIsUndefined(excludeProperty) || !jcStrictEq(excludeProperty, property)) &&
                jcStrictEq(property.keyNode.value, key) &&
                jcTruthy(property.valueNode) &&
                jcStrictEq(property.valueNode.type, "object")
            ) {
                callback(property.valueNode)
            }
            jcWalkObjectProperties(property.valueNode, key, excludeProperty, callback)
            index++
        }
    } else if (jcStrictEq(type, "array")) {
        val items = node.items
        var index = 0
        while (index < jcLength(items)) {
            jcWalkObjectProperties(items[index], key, excludeProperty, callback)
            index++
        }
    }
}

/** 沿节点树递归，把属性名等于 `key` 的 valueNode 交给 callback。 */
private fun jcWalkPropertyValues(node: dynamic, key: dynamic, callback: (dynamic) -> Unit) {
    if (!jcTruthy(node)) return
    val type = node.type
    if (jcStrictEq(type, "object")) {
        val properties = node.properties
        var index = 0
        while (index < jcLength(properties)) {
            val property = properties[index]
            if (jcStrictEq(property.keyNode.value, key) && jcTruthy(property.valueNode)) {
                callback(property.valueNode)
            }
            jcWalkPropertyValues(property.valueNode, key, callback)
            index++
        }
    } else if (jcStrictEq(type, "array")) {
        val items = node.items
        var index = 0
        while (index < jcLength(items)) {
            jcWalkPropertyValues(items[index], key, callback)
            index++
        }
    }
}

// ---------------- suggestion / collector ----------------

private fun jcSuggestionKind(type: dynamic): Int {
    var effectiveType = type
    if (jcIsArray(effectiveType)) {
        effectiveType = if (jcLength(effectiveType) > 0) effectiveType[0] else jsxUndefined
    }
    if (!jcTruthy(effectiveType)) return 12
    if (jcStrictEq(effectiveType, "string")) return 12
    if (jcStrictEq(effectiveType, "object")) return 9
    if (jcStrictEq(effectiveType, "property")) return 10
    return 12
}

private fun jcFromMarkup(schema: dynamic): dynamic {
    // CA 不声明 Markdown 文档能力（旧 bundle 的 doesSupportMarkdown() 为 false），
    // 因此 fromMarkup() 总是 undefined。
    return jsxUndefined
}

/**
 * 旧 `collector.add` 的移植：按 label 去重、截断过长 label、补 textEdit，
 * 并把同 label 的 documentation / detail 合并到先加入的补全项上。
 */
private open class JsonCompletionCollector(
    private val document: dynamic,
    private val proposed: dynamic,
    private val result: dynamic,
    private val overwriteStart: Int,
    private val overwriteEnd: Int
) {
    fun count(): Int = jcLength(result.items)

    fun uniqueSubset(existingValues: dynamic): JsonCompletionCollector =
        JsonUniqueCompletionCollector(
            document, proposed, result, overwriteStart, overwriteEnd, existingValues, this
        )

    open fun add(suggestion: dynamic) {
        val originalLabel = suggestion.label
        val existing = proposed.get(originalLabel)
        if (!jcTruthy(existing)) {
            val label = jcHelpers.normalizeLabel(originalLabel, proposed)
            suggestion.textEdit =
                jcNewTextEdit(document, overwriteStart, overwriteEnd, suggestion.insertText)
            suggestion.label = label
            proposed.set(label, suggestion)
            result.items.push(suggestion)
        } else {
            if (!jcTruthy(existing.documentation)) existing.documentation = suggestion.documentation
            if (!jcTruthy(existing.detail)) existing.detail = suggestion.detail
            if (!jcTruthy(existing.labelDetails)) existing.labelDetails = suggestion.labelDetails
        }
    }
}

/**
 * `uniqueItems` 的过滤包装：旧实现会在添加数组 items 候选前跳过已经出现过的原始值。
 * 这里对应 `collector.add` 的代理，label 判断发生在去重/补 textEdit 之前。
 */
private class JsonUniqueCompletionCollector(
    document: dynamic,
    proposed: dynamic,
    result: dynamic,
    overwriteStart: Int,
    overwriteEnd: Int,
    private val existingValues: dynamic,
    private val delegate: JsonCompletionCollector
) : JsonCompletionCollector(document, proposed, result, overwriteStart, overwriteEnd) {
    override fun add(suggestion: dynamic) {
        val label = suggestion.label
        if (!jcTruthy(existingValues.get(label))) delegate.add(suggestion)
    }
}

// ---------------- schema value completions ----------------

private fun jcAddEnumValueCompletions(
    schema: dynamic,
    separatorAfter: String,
    collector: JsonCompletionCollector
) {
    val constValue = schema["const"]
    if (!jcIsUndefined(constValue)) {
        val suggestion = jsxNewObject()
        suggestion.kind = jcSuggestionKind(schema.type)
        suggestion.label = jcJsonStringify(constValue)
        suggestion.insertText = jcHelpers.insertTextForValue(constValue, separatorAfter)
        suggestion.insertTextFormat = 2
        val constDocumentation = jcFromMarkup(schema)
        suggestion.documentation =
            if (jcTruthy(constDocumentation)) constDocumentation else schema.description
        collector.add(suggestion)
    }

    val enumValues = schema["enum"]
    if (jcIsArray(enumValues)) {
        var index = 0
        while (index < jcLength(enumValues)) {
            val enumValue = enumValues[index]
            var documentation: dynamic = jcFromMarkup(schema)
            if (!jcTruthy(documentation)) documentation = schema.description
            val enumDescriptions = schema.enumDescriptions
            if (jcTruthy(enumDescriptions) && index < jcLength(enumDescriptions)) {
                documentation = enumDescriptions[index]
            }
            val suggestion = jsxNewObject()
            suggestion.kind = jcSuggestionKind(schema.type)
            suggestion.label = jcJsonStringify(enumValue)
            suggestion.insertText = jcHelpers.insertTextForValue(enumValue, separatorAfter)
            suggestion.insertTextFormat = 2
            val enumSortTexts = schema.enumSortTexts
            if (jcTruthy(enumSortTexts) && index < jcLength(enumSortTexts)) {
                suggestion.sortText = enumSortTexts[index]
            }
            val enumDetails = schema.enumDetails
            if (jcTruthy(enumDetails) && index < jcLength(enumDetails)) {
                suggestion.detail = enumDetails[index]
            }
            suggestion.documentation = documentation
            collector.add(suggestion)
            index++
        }
    }
}

private fun jcAddDefaultValueCompletions(
    schemaInput: dynamic,
    separatorAfter: String,
    collector: JsonCompletionCollector,
    arrayDepth: Int,
    schemaRoot: dynamic
) {
    val schema = jsxResolveSchemaRef(schemaInput, schemaRoot)
    if (!jcIsObject(schema)) return
    var hasProposals = false

    val defaultValue = schema["default"]
    if (!jcIsUndefined(defaultValue)) {
        var type = schema.type
        var value = defaultValue
        var depth = arrayDepth
        while (depth > 0) {
            val wrapped = jsxNewArray()
            wrapped.push(value)
            value = wrapped
            type = "array"
            depth--
        }
        val suggestion = jsxNewObject()
        suggestion.kind = jcSuggestionKind(type)
        suggestion.label = jcJsonStringify(value)
        suggestion.insertText = jcHelpers.insertTextForValue(value, separatorAfter)
        suggestion.insertTextFormat = 2
        // CA 的 options 不带 labelDetails 能力，旧 bundle 这里走 detail 分支。
        suggestion.detail = "Default value"
        collector.add(suggestion)
        hasProposals = true
    }

    val examples = schema.examples
    if (jcIsArray(examples)) {
        var index = 0
        while (index < jcLength(examples)) {
            var type = schema.type
            var value = examples[index]
            var depth = arrayDepth
            while (depth > 0) {
                val wrapped = jsxNewArray()
                wrapped.push(value)
                value = wrapped
                type = "array"
                depth--
            }
            val suggestion = jsxNewObject()
            suggestion.kind = jcSuggestionKind(type)
            suggestion.label = jcJsonStringify(value)
            suggestion.insertText = jcHelpers.insertTextForValue(value, separatorAfter)
            suggestion.insertTextFormat = 2
            collector.add(suggestion)
            hasProposals = true
            index++
        }
    }

    val defaultSnippets = schema.defaultSnippets
    if (jcIsArray(defaultSnippets)) {
        var index = 0
        while (index < jcLength(defaultSnippets)) {
            val snippet = defaultSnippets[index]
            var type = schema.type
            var value = snippet.body
            var label = snippet.label
            var insertText: dynamic = jsxUndefined
            var filterText: dynamic = jsxUndefined
            if (!jcIsUndefined(value)) {
                var depth = arrayDepth
                while (depth > 0) {
                    val wrapped = jsxNewArray()
                    wrapped.push(value)
                    value = wrapped
                    type = "array"
                    depth--
                }
                insertText = jcHelpers.insertTextForSnippetValue(value, separatorAfter)
                filterText = jcJsonStringify(value)
                if (!jcTruthy(label)) label = jcJsonStringify(value)
            } else if (jcIsString(snippet.bodyText)) {
                var prefix = ""
                var suffix = ""
                var indent = ""
                var depth = arrayDepth
                while (depth > 0) {
                    prefix += indent + "[\n"
                    suffix += "\n" + indent + "]"
                    indent += "\t"
                    type = "array"
                    depth--
                }
                val bodyText = snippet.bodyText as String
                insertText = prefix + indent + bodyText.replace("\n", "\n" + indent) + suffix + separatorAfter
                if (!jcTruthy(label)) label = insertText
                filterText = (insertText as String).replace("\n", "")
            } else {
                index++
                continue
            }
            val suggestion = jsxNewObject()
            suggestion.kind = jcSuggestionKind(type)
            suggestion.label = label
            val documentation = jcFromMarkup(snippet)
            if (jcTruthy(documentation)) {
                suggestion.documentation = documentation
            } else if (jcTruthy(snippet.description)) {
                suggestion.documentation = snippet.description
            }
            suggestion.insertText = insertText
            suggestion.insertTextFormat = 2
            if (!jcIsUndefined(filterText)) suggestion.filterText = filterText
            collector.add(suggestion)
            hasProposals = true
            index++
        }
    }

    val items = schema.items
    if (!hasProposals && jcIsObject(items) && !jcIsArray(items) && arrayDepth < 5) {
        jcAddDefaultValueCompletions(items, separatorAfter, collector, arrayDepth + 1, schemaRoot)
    }
}

private fun jcCollectTypes(schema: dynamic, types: dynamic) {
    if (jcIsArray(schema["enum"]) || !jcIsUndefined(schema["const"])) return
    val type = schema.type
    if (jcIsArray(type)) {
        var index = 0
        while (index < jcLength(type)) {
            types[jcString(type[index])] = true
            index++
        }
    } else if (jcTruthy(type)) {
        types[jcString(type)] = true
    }
}

private fun jcAddSchemaValueCompletions(
    schemaInput: dynamic,
    separatorAfter: String,
    collector: JsonCompletionCollector,
    types: dynamic,
    schemaRoot: dynamic
) {
    val schema = jsxResolveSchemaRef(schemaInput, schemaRoot)
    if (!jcIsObject(schema)) return
    jcAddEnumValueCompletions(schema, separatorAfter, collector)
    jcAddDefaultValueCompletions(schema, separatorAfter, collector, 0, schemaRoot)
    jcCollectTypes(schema, types)

    val allOf = schema.allOf
    if (jcIsArray(allOf)) {
        var index = 0
        while (index < jcLength(allOf)) {
            jcAddSchemaValueCompletions(allOf[index], separatorAfter, collector, types, schemaRoot)
            index++
        }
    }
    val anyOf = schema.anyOf
    if (jcIsArray(anyOf)) {
        var index = 0
        while (index < jcLength(anyOf)) {
            jcAddSchemaValueCompletions(anyOf[index], separatorAfter, collector, types, schemaRoot)
            index++
        }
    }
    val oneOf = schema.oneOf
    if (jcIsArray(oneOf)) {
        var index = 0
        while (index < jcLength(oneOf)) {
            jcAddSchemaValueCompletions(oneOf[index], separatorAfter, collector, types, schemaRoot)
            index++
        }
    }
}

// ---------------- property insert text ----------------

private fun jcGetInsertTextForProperty(
    key: dynamic,
    propertySchema: dynamic,
    addValue: Boolean,
    separatorAfter: String,
    schemaRoot: dynamic
): String {
    val propertyText = jcHelpers.insertTextForValue(key, "") as String
    if (!addValue) return propertyText
    val resultText = propertyText + ": "
    var value: String? = null
    var valueProposals = 0

    if (jcTruthy(propertySchema)) {
        val defaultSnippets = propertySchema.defaultSnippets
        if (jcIsArray(defaultSnippets)) {
            if (jcLength(defaultSnippets) == 1) {
                val body = defaultSnippets[0].body
                if (!jcIsUndefined(body)) {
                    value = jcHelpers.insertTextForSnippetValue(body, "") as String
                }
            }
            valueProposals += jcLength(defaultSnippets)
        }
        val enumValues = propertySchema["enum"]
        if (jcTruthy(enumValues)) {
            if ((value == null || value == "") && jcLength(enumValues) == 1) {
                value = jcHelpers.insertTextForGuessedValue(enumValues[0], "") as String
            }
            valueProposals += jcLength(enumValues)
        }
        val constValue = propertySchema["const"]
        if (!jcIsUndefined(constValue)) {
            if (value == null || value == "") {
                value = jcHelpers.insertTextForGuessedValue(constValue, "") as String
            }
            valueProposals++
        }
        val defaultValue = propertySchema["default"]
        if (!jcIsUndefined(defaultValue)) {
            if (value == null || value == "") {
                value = jcHelpers.insertTextForGuessedValue(defaultValue, "") as String
            }
            valueProposals++
        }
        val examples = propertySchema.examples
        if (jcIsArray(examples) && jcLength(examples) > 0) {
            if (value == null || value == "") {
                value = jcHelpers.insertTextForGuessedValue(examples[0], "") as String
            }
            valueProposals += jcLength(examples)
        }
        if (valueProposals == 0) {
            var type = propertySchema.type
            if (jcIsArray(type)) {
                type = if (jcLength(type) > 0) type[0] else jsxUndefined
            }
            if (!jcTruthy(type)) {
                if (jcTruthy(propertySchema.properties)) {
                    type = "object"
                } else if (jcTruthy(propertySchema.items)) {
                    type = "array"
                }
            }
            if (jcStrictEq(type, "boolean")) {
                value = "\$1"
            } else if (jcStrictEq(type, "string")) {
                value = "\"\$1\""
            } else if (jcStrictEq(type, "object")) {
                value = "{\$1}"
            } else if (jcStrictEq(type, "array")) {
                value = "[\$1]"
            } else if (jcStrictEq(type, "number") || jcStrictEq(type, "integer")) {
                value = "\${1:0}"
            } else if (jcStrictEq(type, "null")) {
                value = "\${1:null}"
            } else {
                return propertyText
            }
        }
    }

    if (value == null || value == "" || valueProposals > 1) value = "\$1"
    return resultText + value + separatorAfter
}

// ---------------- property completions ----------------

private fun jcAddPropertySuggestionCommand(
    proposal: dynamic,
    separatorAfter: String
) {
    val insertText = proposal.insertText
    if (jcTruthy(insertText) && (insertText as String).endsWith("\$1" + separatorAfter)) {
        val command = jsxNewObject()
        command.title = "Suggest"
        command.command = "editor.action.triggerSuggest"
        proposal.command = command
    }
}

private fun jcPropertyNameCompletionItem(
    name: dynamic,
    documentation: dynamic,
    detail: dynamic,
    sortText: dynamic,
    schemaPropertyNames: dynamic,
    addValue: Boolean,
    separatorAfter: String,
    schemaRoot: dynamic,
    collector: JsonCompletionCollector
) {
    val proposal = jsxNewObject()
    proposal.kind = 10
    proposal.label = name
    proposal.insertText = jcGetInsertTextForProperty(
        name,
        jsxUndefined,
        addValue,
        separatorAfter,
        schemaRoot
    )
    proposal.insertTextFormat = 2
    proposal.filterText = jcJsonStringify(name)
    var documentationValue = documentation
    if (!jcTruthy(documentationValue)) {
        documentationValue = jcFromMarkup(schemaPropertyNames)
    }
    if (!jcTruthy(documentationValue) && jcTruthy(schemaPropertyNames.description)) {
        documentationValue = schemaPropertyNames.description
    }
    proposal.documentation = if (jcTruthy(documentationValue)) documentationValue else ""
    if (!jcIsUndefined(sortText)) proposal.sortText = sortText
    if (!jcIsUndefined(detail)) proposal.detail = detail
    jcAddPropertySuggestionCommand(proposal, separatorAfter)
    collector.add(proposal)
}

private fun jcGetPropertyCompletions(
    schemaRoot: dynamic,
    jsonDocument: dynamic,
    node: dynamic,
    addValue: Boolean,
    separatorAfter: String,
    collector: JsonCompletionCollector
) {
    val matchingSchemas = jsonMatchingSchemas(
        jsonDocument.root,
        schemaRoot,
        jcNodeOffset(node),
        jsxUndefined,
        true
    )
    var matchIndex = 0
    while (matchIndex < jcLength(matchingSchemas)) {
        val match = matchingSchemas[matchIndex]
        if (jcStrictEq(match.node, node) && !jcTruthy(match.inverted)) {
            val schema = match.schema
            val schemaProperties = schema.properties
            if (jcTruthy(schemaProperties)) {
                val propertyKeys = jcKeys(schemaProperties)
                var keyIndex = 0
                while (keyIndex < jcLength(propertyKeys)) {
                    val key = propertyKeys[keyIndex]
                    val propertySchema =
                        jsxResolveSchemaRef(schemaProperties[key], schemaRoot)
                    if (jcIsObject(propertySchema) &&
                        !jcTruthy(propertySchema.deprecationMessage) &&
                        !jcTruthy(propertySchema.doNotSuggest)
                    ) {
                        val proposal = jsxNewObject()
                        proposal.kind = 10
                        proposal.label = key
                        proposal.insertText = jcGetInsertTextForProperty(
                            key,
                            propertySchema,
                            addValue,
                            separatorAfter,
                            schemaRoot
                        )
                        proposal.insertTextFormat = 2
                        proposal.filterText = jcJsonStringify(key)
                        var documentation = jcFromMarkup(propertySchema)
                        if (!jcTruthy(documentation) && jcTruthy(propertySchema.description)) {
                            documentation = propertySchema.description
                        }
                        proposal.documentation = if (jcTruthy(documentation)) documentation else ""
                        if (!jcIsUndefined(propertySchema.completionDetail)) {
                            proposal.detail = propertySchema.completionDetail
                        }
                        if (!jcIsUndefined(propertySchema.suggestSortText)) {
                            proposal.sortText = propertySchema.suggestSortText
                        }
                        jcAddPropertySuggestionCommand(proposal, separatorAfter)
                        collector.add(proposal)
                    }
                    keyIndex++
                }
            }

            val schemaPropertyNames = schema.propertyNames
            if (jcIsObject(schemaPropertyNames) &&
                !jcTruthy(schemaPropertyNames.deprecationMessage) &&
                !jcTruthy(schemaPropertyNames.doNotSuggest)
            ) {
                val enumValues = schemaPropertyNames["enum"]
                if (jcTruthy(enumValues)) {
                    var index = 0
                    while (index < jcLength(enumValues)) {
                        var enumDescription: dynamic = jsxUndefined
                        val markdownEnumDescriptions = schemaPropertyNames.markdownEnumDescriptions
                        if (jcTruthy(markdownEnumDescriptions) &&
                            index < jcLength(markdownEnumDescriptions)
                        ) {
                            enumDescription = jcFromMarkup(markdownEnumDescriptions[index])
                        } else {
                            val enumDescriptions = schemaPropertyNames.enumDescriptions
                            if (jcTruthy(enumDescriptions) && index < jcLength(enumDescriptions)) {
                                enumDescription = enumDescriptions[index]
                            }
                        }
                        var enumSortText: dynamic = jsxUndefined
                        val enumSortTexts = schemaPropertyNames.enumSortTexts
                        if (jcTruthy(enumSortTexts) && index < jcLength(enumSortTexts)) {
                            enumSortText = enumSortTexts[index]
                        }
                        var enumDetail: dynamic = jsxUndefined
                        val enumDetails = schemaPropertyNames.enumDetails
                        if (jcTruthy(enumDetails) && index < jcLength(enumDetails)) {
                            enumDetail = enumDetails[index]
                        }
                        jcPropertyNameCompletionItem(
                            enumValues[index],
                            enumDescription,
                            enumDetail,
                            enumSortText,
                            schemaPropertyNames,
                            addValue,
                            separatorAfter,
                            schemaRoot,
                            collector
                        )
                        index++
                    }
                }
                val examples = schemaPropertyNames.examples
                if (jcIsArray(examples)) {
                    var index = 0
                    while (index < jcLength(examples)) {
                        jcPropertyNameCompletionItem(
                            examples[index],
                            jsxUndefined,
                            jsxUndefined,
                            jsxUndefined,
                            schemaPropertyNames,
                            addValue,
                            separatorAfter,
                            schemaRoot,
                            collector
                        )
                        index++
                    }
                }
                val constValue = schemaPropertyNames["const"]
                if (!jcIsUndefined(constValue)) {
                    jcPropertyNameCompletionItem(
                        constValue,
                        jsxUndefined,
                        schemaPropertyNames.completionDetail,
                        schemaPropertyNames.suggestSortText,
                        schemaPropertyNames,
                        addValue,
                        separatorAfter,
                        schemaRoot,
                        collector
                    )
                }
            }
        }
        matchIndex++
    }
}

// ---------------- value completions ----------------

private fun jcAddBooleanValueCompletion(
    value: Boolean,
    separatorAfter: String,
    collector: JsonCompletionCollector
) {
    val suggestion = jsxNewObject()
    suggestion.kind = jcSuggestionKind("boolean")
    suggestion.label = if (value) "true" else "false"
    suggestion.insertText = jcHelpers.insertTextForValue(value, separatorAfter)
    suggestion.insertTextFormat = 2
    suggestion.documentation = ""
    collector.add(suggestion)
}

private fun jcAddNullValueCompletion(
    separatorAfter: String,
    collector: JsonCompletionCollector
) {
    val suggestion = jsxNewObject()
    suggestion.kind = jcSuggestionKind("null")
    suggestion.label = "null"
    suggestion.insertText = "null" + separatorAfter
    suggestion.insertTextFormat = 2
    suggestion.documentation = ""
    collector.add(suggestion)
}

private fun jcAddFillerValueCompletions(
    types: dynamic,
    separatorAfter: String,
    collector: JsonCompletionCollector
) {
    if (jcTruthy(types["object"])) {
        val suggestion = jsxNewObject()
        suggestion.kind = jcSuggestionKind("object")
        suggestion.label = "{}"
        val emptyObject = jsxNewObject()
        suggestion.insertText = jcHelpers.insertTextForGuessedValue(emptyObject, separatorAfter)
        suggestion.insertTextFormat = 2
        suggestion.detail = "New object"
        suggestion.documentation = ""
        collector.add(suggestion)
    }
    if (jcTruthy(types["array"])) {
        val suggestion = jsxNewObject()
        suggestion.kind = jcSuggestionKind("array")
        suggestion.label = "[]"
        val emptyArray = jsxNewArray()
        suggestion.insertText = jcHelpers.insertTextForGuessedValue(emptyArray, separatorAfter)
        suggestion.insertTextFormat = 2
        suggestion.detail = "New array"
        suggestion.documentation = ""
        collector.add(suggestion)
    }
}

private fun jcGetValueCompletions(
    schemaRoot: dynamic,
    jsonDocument: dynamic,
    startNode: dynamic,
    offset: Int,
    document: dynamic,
    collector: JsonCompletionCollector,
    types: dynamic
) {
    var node = startNode
    var offsetForSeparator = offset
    var parentKey: dynamic = jsxUndefined
    var valueNode: dynamic = jsxUndefined

    if (jcTruthy(node) && jcIsLeafNode(node)) {
        offsetForSeparator = jcNodeEnd(node)
        valueNode = node
        node = node.parent
    }
    if (!jcTruthy(node)) {
        jcAddSchemaValueCompletions(schemaRoot, "", collector, types, schemaRoot)
        return
    }

    if (jcStrictEq(node.type, "property") && offset > jcColonOffset(node)) {
        val currentValueNode = node.valueNode
        if (jcTruthy(currentValueNode) && offset > jcNodeEnd(currentValueNode)) {
            return
        }
        parentKey = node.keyNode.value
        node = node.parent
    }

    if (jcTruthy(node) && (!jcIsUndefined(parentKey) || jcStrictEq(node.type, "array"))) {
        val text = jcText(document)
        val separatorAfter = jcHelpers.evaluateSeparatorAfter(text, offsetForSeparator) as String
        val matchingSchemas = jsonMatchingSchemas(
            jsonDocument.root,
            schemaRoot,
            jcNodeOffset(node),
            valueNode,
            true
        )
        var matchIndex = 0
        while (matchIndex < jcLength(matchingSchemas)) {
            val match = matchingSchemas[matchIndex]
            if (jcStrictEq(match.node, node) && !jcTruthy(match.inverted) && jcTruthy(match.schema)) {
                val schema = match.schema
                if (jcStrictEq(node.type, "array") && jcTruthy(schema.items)) {
                    var itemCollector = collector
                    if (jcTruthy(schema.uniqueItems)) {
                        val existingValues = jcHelpers.createMap()
                        val children = node.children
                        val itemNodes = if (jcTruthy(children)) children else node.items
                        var itemNodeIndex = 0
                        while (itemNodeIndex < jcLength(itemNodes)) {
                            val itemNode = itemNodes[itemNodeIndex]
                            if (jcTruthy(itemNode) &&
                                !jcStrictEq(itemNode.type, "array") &&
                                !jcStrictEq(itemNode.type, "object")
                            ) {
                                existingValues.set(jcJsonStringify(itemNode.value), true)
                            }
                            itemNodeIndex++
                        }
                        itemCollector = collector.uniqueSubset(existingValues)
                    }
                    if (jcIsArray(schema.items)) {
                        val itemIndex = jcHelpers.findItemAtOffset(node, text, offset) as Int
                        if (itemIndex < jcLength(schema.items)) {
                            jcAddSchemaValueCompletions(
                                jsxResolveSchemaRef(schema.items[itemIndex], schemaRoot),
                                separatorAfter,
                                itemCollector,
                                types,
                                schemaRoot
                            )
                        }
                    } else {
                        jcAddSchemaValueCompletions(
                            jsxResolveSchemaRef(schema.items, schemaRoot),
                            separatorAfter,
                            itemCollector,
                            types,
                            schemaRoot
                        )
                    }
                }
                if (!jcIsUndefined(parentKey)) {
                    var propertyMatched = false
                    val schemaProperties = schema.properties
                    if (jcTruthy(schemaProperties)) {
                        val propertySchema = schemaProperties[parentKey]
                        if (jcTruthy(propertySchema)) {
                            propertyMatched = true
                            jcAddSchemaValueCompletions(
                                jsxResolveSchemaRef(propertySchema, schemaRoot),
                                separatorAfter,
                                collector,
                                types,
                                schemaRoot
                            )
                        }
                    }
                    val patternProperties = schema.patternProperties
                    if (jcTruthy(patternProperties) && !propertyMatched) {
                        val patterns = jcKeys(patternProperties)
                        var patternIndex = 0
                        while (patternIndex < jcLength(patterns)) {
                            val pattern = patterns[patternIndex]
                            if (jcTruthy(jcHelpers.matchesPattern(pattern, parentKey))) {
                                propertyMatched = true
                                jcAddSchemaValueCompletions(
                                    jsxResolveSchemaRef(
                                        patternProperties[pattern],
                                        schemaRoot
                                    ),
                                    separatorAfter,
                                    collector,
                                    types,
                                    schemaRoot
                                )
                            }
                            patternIndex++
                        }
                    }
                    val additionalProperties = schema.additionalProperties
                    if (jcTruthy(additionalProperties) && !propertyMatched) {
                        jcAddSchemaValueCompletions(
                            jsxResolveSchemaRef(additionalProperties, schemaRoot),
                            separatorAfter,
                            collector,
                            types,
                            schemaRoot
                        )
                    }
                }
            }
            matchIndex++
        }
        // `$schema` 自动补全依赖 schema service 的 getRegisteredSchemaIds；阶段 2 的
        // registry 还没有暴露该列表，CA 也不会触发（文档根只有 rawtext 等业务字段）。
        if (jcStrictEq(parentKey, "\$schema") && !jcTruthy(node.parent)) {
            // 旧实现会在这里加入内置 draft schema 候选；CA 场景下没有注册的 $schema id。
        }
        if (jcTruthy(types["boolean"])) {
            jcAddBooleanValueCompletion(true, separatorAfter, collector)
            jcAddBooleanValueCompletion(false, separatorAfter, collector)
        }
        if (jcTruthy(types["null"])) {
            jcAddNullValueCompletion(separatorAfter, collector)
        }
    }
}

// ---------------- schema-less completions（旧实现的 fallback 路径） ----------------

private fun jcAddSchemaLessPropertyCompletions(
    jsonDocument: dynamic,
    node: dynamic,
    collector: JsonCompletionCollector
) {
    val parent = node.parent
    if (jcTruthy(parent)) {
        if (jcStrictEq(parent.type, "property")) {
            val parentKey = parent.keyNode.value
            jcWalkObjectProperties(jsonDocument.root, parentKey, parent) { valueNode ->
                val properties = valueNode.properties
                var index = 0
                while (index < jcLength(properties)) {
                    val key = properties[index].keyNode.value
                    val suggestion = jsxNewObject()
                    suggestion.kind = 10
                    suggestion.label = key
                    suggestion.insertText = jcHelpers.insertTextForValue(key, "")
                    suggestion.insertTextFormat = 2
                    suggestion.filterText = jcJsonStringify(key)
                    suggestion.documentation = ""
                    collector.add(suggestion)
                    index++
                }
            }
        } else if (jcStrictEq(parent.type, "array")) {
            val items = parent.items
            var index = 0
            while (index < jcLength(items)) {
                val item = items[index]
                if (jcTruthy(item) && jcStrictEq(item.type, "object") && !jcStrictEq(item, node)) {
                    val properties = item.properties
                    var propertyIndex = 0
                    while (propertyIndex < jcLength(properties)) {
                        val key = properties[propertyIndex].keyNode.value
                        val suggestion = jsxNewObject()
                        suggestion.kind = 10
                        suggestion.label = key
                        suggestion.insertText = jcHelpers.insertTextForValue(key, "")
                        suggestion.insertTextFormat = 2
                        suggestion.filterText = jcJsonStringify(key)
                        suggestion.documentation = ""
                        collector.add(suggestion)
                        propertyIndex++
                    }
                }
                index++
            }
        }
    } else if (jcStrictEq(node.type, "object")) {
        val suggestion = jsxNewObject()
        suggestion.kind = 10
        suggestion.label = "\$schema"
        suggestion.insertText = jcGetInsertTextForProperty(
            "\$schema",
            jsxUndefined,
            true,
            "",
            jsxUndefined
        )
        suggestion.insertTextFormat = 2
        suggestion.documentation = ""
        suggestion.filterText = jcJsonStringify("\$schema")
        collector.add(suggestion)
    }
}

private fun jcGetSchemaLessValueCompletions(
    jsonDocument: dynamic,
    startNode: dynamic,
    offset: Int,
    document: dynamic,
    collector: JsonCompletionCollector
) {
    var node = startNode
    var offsetForSeparator = offset
    if (jcTruthy(node) && jcIsLeafNode(node)) {
        offsetForSeparator = jcNodeEnd(node)
        node = node.parent
    }
    if (!jcTruthy(node)) {
        val emptyObjectItem = jsxNewObject()
        emptyObjectItem.kind = jcSuggestionKind("object")
        emptyObjectItem.label = "Empty object"
        val emptyObject = jsxNewObject()
        emptyObjectItem.insertText = jcHelpers.insertTextForValue(emptyObject, "")
        emptyObjectItem.insertTextFormat = 2
        emptyObjectItem.documentation = ""
        collector.add(emptyObjectItem)
        val emptyArrayItem = jsxNewObject()
        emptyArrayItem.kind = jcSuggestionKind("array")
        emptyArrayItem.label = "Empty array"
        val emptyArray = jsxNewArray()
        emptyArrayItem.insertText = jcHelpers.insertTextForValue(emptyArray, "")
        emptyArrayItem.insertTextFormat = 2
        emptyArrayItem.documentation = ""
        collector.add(emptyArrayItem)
        return
    }

    val text = jcText(document)
    val separatorAfter = jcHelpers.evaluateSeparatorAfter(text, offsetForSeparator) as String

    if (jcStrictEq(node.type, "property") && offset > jcColonOffset(node)) {
        val currentValueNode = node.valueNode
        if (jcTruthy(currentValueNode) &&
            (offset > jcNodeEnd(currentValueNode) ||
                jcStrictEq(currentValueNode.type, "object") ||
                jcStrictEq(currentValueNode.type, "array"))
        ) {
            return
        }
        val parentKey = node.keyNode.value
        jcWalkPropertyValues(jsonDocument.root, parentKey) { value ->
            val parentNode = value.parent
            if (jcTruthy(parentNode)) {
                val start = jcNodeOffset(parentNode)
                val end = jcNodeEnd(parentNode)
                val contains = (offset >= start && offset < end) ||
                    (offset == end)
                if (!contains || jcStrictEq(value.type, "object") ||
                    jcStrictEq(value.type, "array")
                ) {
                    val suggestion = jsxNewObject()
                    suggestion.kind = jcSuggestionKind(value.type)
                    suggestion.label = jcHelpers.labelTextForMatchingNode(value, text)
                    suggestion.insertText =
                        jcHelpers.insertTextForMatchingNode(value, text, separatorAfter)
                    suggestion.insertTextFormat = 2
                    suggestion.documentation = ""
                    collector.add(suggestion)
                }
            }
            if (jcStrictEq(value.type, "boolean")) {
                jcAddBooleanValueCompletion(!(value.value as Boolean), separatorAfter, collector)
            }
        }
    }

    if (jcStrictEq(node.type, "array")) {
        val parent = node.parent
        if (jcTruthy(parent) && jcStrictEq(parent.type, "property")) {
            val parentKey = parent.keyNode.value
            jcWalkPropertyValues(jsonDocument.root, parentKey) { value ->
                if (jcStrictEq(value.type, "array")) {
                    val items = value.items
                    var index = 0
                    while (index < jcLength(items)) {
                        jcCollectSchemaLessValue(items[index], document, offset, separatorAfter, collector)
                        index++
                    }
                }
            }
        } else {
            val items = node.items
            var index = 0
            while (index < jcLength(items)) {
                jcCollectSchemaLessValue(items[index], document, offset, separatorAfter, collector)
                index++
            }
        }
    }
}

private fun jcCollectSchemaLessValue(
    value: dynamic,
    document: dynamic,
    offset: Int,
    separatorAfter: String,
    collector: JsonCompletionCollector
) {
    if (!jcTruthy(value)) return
    val parent = value.parent
    if (jcTruthy(parent)) {
        val start = jcNodeOffset(parent)
        val end = jcNodeEnd(parent)
        if (offset >= start && offset < end) return
    }
    val suggestion = jsxNewObject()
    suggestion.kind = jcSuggestionKind(value.type)
    suggestion.label = jcHelpers.labelTextForMatchingNode(value, jcText(document))
    suggestion.insertText = jcHelpers.insertTextForMatchingNode(value, jcText(document), separatorAfter)
    suggestion.insertTextFormat = 2
    suggestion.documentation = ""
    collector.add(suggestion)
    if (jcStrictEq(value.type, "boolean")) {
        jcAddBooleanValueCompletion(!(value.value as Boolean), separatorAfter, collector)
    }
}

// ---------------- doComplete ----------------

private fun jcGetCurrentWord(text: String, offset: Int): String {
    var index = offset - 1
    while (index >= 0 && " \t\n\r\u000b\":{[,]}".indexOf(text[index]) == -1) {
        index--
    }
    return text.substring(index + 1, offset)
}

private fun jcDoComplete(
    document: dynamic,
    position: dynamic,
    jsonDocumentInput: dynamic,
    documentSettings: dynamic,
    registry: JsonSchemaRegistry
): dynamic {
    val result = jsxNewObject()
    result.items = jsxNewArray()
    result.isIncomplete = false

    if (!jcTruthy(document) || !jcTruthy(position)) return result

    val jsonDocument =
        if (jcTruthy(jsonDocumentInput)) jsonDocumentInput else parseJsonDocument(document)
    val text = jcText(document)
    val offset = document.offsetAt(position) as Int
    var node = jcNodeAt(jsonDocument, offset)

    if (jcHelpers.isInComment(text, if (jcTruthy(node)) jcNodeOffset(node) else 0, offset) as Int != 0) {
        return result
    }

    if (jcTruthy(node) && offset == jcNodeEnd(node) && offset > 0) {
        val previous = text[offset - 1]
        if ((jcStrictEq(node.type, "object") && previous == '}') ||
            (jcStrictEq(node.type, "array") && previous == ']')
        ) {
            node = node.parent
        }
    }

    val currentWord = jcGetCurrentWord(text, offset)
    var overwriteStart: Int
    var overwriteEnd: Int
    if (jcTruthy(node) && jcIsLeafNode(node)) {
        overwriteStart = jcNodeOffset(node)
        overwriteEnd = jcNodeEnd(node)
    } else {
        overwriteStart = offset - currentWord.length
        if (overwriteStart > 0 && text[overwriteStart - 1] == '"') overwriteStart--
        overwriteEnd = offset
    }

    val proposed = jcHelpers.createMap()
    val collector =
        JsonCompletionCollector(document, proposed, result, overwriteStart, overwriteEnd)

    val schema = registry.schemaForResource(document.uri, jsonDocument)

    var addValue = true
    var currentProperty: dynamic = jsxUndefined
    if (jcTruthy(node) && jcStrictEq(node.type, "string")) {
        val parent = node.parent
        if (jcTruthy(parent) && jcStrictEq(parent.type, "property") &&
            jcStrictEq(parent.keyNode, node)
        ) {
            addValue = !jcTruthy(parent.valueNode)
            currentProperty = parent
            node = parent.parent
        }
    }

    if (jcTruthy(node) && jcStrictEq(node.type, "object")) {
        if (jcNodeOffset(node) == offset) {
            return result
        }
        val properties = node.properties
        var index = 0
        while (index < jcLength(properties)) {
            val property = properties[index]
            if (jcIsUndefined(currentProperty) || !jcStrictEq(currentProperty, property)) {
                val placeholder = jsxNewObject()
                placeholder.label = "__"
                proposed.set(property.keyNode.value, placeholder)
            }
            index++
        }

        val separatorAfter =
            if (addValue) {
                jcHelpers.evaluateSeparatorAfter(text, overwriteEnd) as String
            } else {
                ""
            }
        if (jcTruthy(schema)) {
            jcGetPropertyCompletions(
                schema,
                jsonDocument,
                node,
                addValue,
                separatorAfter,
                collector
            )
        } else {
            jcAddSchemaLessPropertyCompletions(jsonDocument, node, collector)
        }

        if (!jcTruthy(schema) && currentWord.length > 0 &&
            (jcHelpers.charAt(text, offset - currentWord.length - 1) as String) != "\""
        ) {
            val suggestion = jsxNewObject()
            suggestion.kind = 10
            suggestion.label = jcJsonStringify(currentWord)
            suggestion.insertText = jcGetInsertTextForProperty(
                currentWord,
                jsxUndefined,
                false,
                separatorAfter,
                jsxUndefined
            )
            suggestion.insertTextFormat = 2
            suggestion.documentation = ""
            collector.add(suggestion)
            result.isIncomplete = true
        }
    }

    val types = jsxNewObject()
    if (jcTruthy(schema)) {
        jcGetValueCompletions(
            schema,
            jsonDocument,
            node,
            offset,
            document,
            collector,
            types
        )
    } else {
        jcGetSchemaLessValueCompletions(jsonDocument, node, offset, document, collector)
    }

    if (collector.count() == 0) {
        var offsetForSeparator = offset
        if (jcTruthy(node) && jcIsLeafNode(node)) {
            offsetForSeparator = jcNodeEnd(node)
        }
        val separatorAfter = jcHelpers.evaluateSeparatorAfter(text, offsetForSeparator) as String
        jcAddFillerValueCompletions(types, separatorAfter, collector)
    }

    return result
}

// ---------------- API 组装 ----------------

private fun jcBuildLanguageService(options: dynamic): dynamic {
    // validation / matching / parseJSONDocument 直接复用阶段 2 的 service，
    // 保证 doValidation / getMatchingSchemas 的行为与阶段 2 完全一致。
    val stage2 = buildJsonLanguageServiceStage2()
    val validationService = stage2.getLanguageService(options)
    val registry = JsonSchemaRegistry()
    val service = jsxNewObject()

    service.configure = { settings: dynamic ->
        validationService.configure(settings)
        registry.configure(settings)
        jsxUndefined
    }

    service.parseJSONDocument = { document: dynamic ->
        validationService.parseJSONDocument(document)
    }

    service.doValidation = { document: dynamic, jsonDocument: dynamic, documentSettings: dynamic, schema: dynamic ->
        validationService.doValidation(document, jsonDocument, documentSettings, schema)
    }

    service.getMatchingSchemas = { document: dynamic, jsonDocument: dynamic, schema: dynamic, offset: dynamic ->
        validationService.getMatchingSchemas(document, jsonDocument, schema, offset)
    }

    service.doComplete = { document: dynamic, position: dynamic, jsonDocument: dynamic, documentSettings: dynamic ->
        jcDoComplete(document, position, jsonDocument, documentSettings, registry)
    }

    return service
}

/**
 * 组装阶段 3 的 JSONLanguageService API：
 * `TextDocument`、`findNodeAtOffset`、`getLanguageService(options)`；后者的
 * `configure` / `parseJSONDocument` / `doValidation` / `getMatchingSchemas` / `doComplete`
 * 都可用，前四项复用阶段 1/2 的实现。
 *
 * 返回普通 JS 对象，不依赖 MapScript / 全局注册；调用方决定何时接入。
 */
fun buildJsonLanguageServiceStage3(): dynamic {
    val core = buildJsonLanguageServiceStage2()
    core.getLanguageService = { options: dynamic -> jcBuildLanguageService(options) }
    return core
}

/**
 * 供 Node parity 测试（`test/json-language-service/stage3-parity.test.js`）调用的入口。
 *
 * 只通过 webpack bundle 暴露为
 * `require(bundle).ca.kotlin.caJsonLanguageServiceStage3()`；
 * 不参与 Rhino/MapScript 的正式接入。
 */
@OptIn(kotlin.js.ExperimentalJsExport::class)
@JsExport
fun caJsonLanguageServiceStage3(): dynamic = buildJsonLanguageServiceStage3()
