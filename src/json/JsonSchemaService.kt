package ca.kotlin

/**
 * JSONLanguageService 迁移阶段 2：schema registry、schema 匹配与 JSON Schema 校验。
 *
 * 目标范围（按 docs/json-language-service-migration.md）：
 *  - `configure({ schemas: [{ uri, fileMatch, schema }] })`；
 *  - 按 fileMatch（支持 `*` / `**` / `!` 否定）或 uri 匹配 document；
 *  - CA 实际用到的 JSON Schema 子集：`$ref`（本地 `#/...`）、type、properties、required、
 *    items、additionalProperties、oneOf/anyOf/allOf、enum、description、default、
 *    `ca:parser` 等扩展键透传；
 *  - `getMatchingSchemas(rootOrDocument, schema, offset)`：返回与旧
 *    vscode-json-languageservice `JSONDocument.getMatchingSchemas` 同形的
 *    `[{ node, schema }]` 数组。
 *
 * 与旧 vendor 的关系：
 *  - 旧 bundle 在 `modules/JSONLanguageService.js` 里保留为 parity oracle；
 *  - 本文件不注册 MapScript 模块，也不触碰任何现有接入文件。
 *
 * 校验引擎是旧 `jsonParser._validate` 的精简移植。为了让 CA 的 jsonPro 行为不漂移，
 * 保留了 oneOf/anyOf 的“最佳候选选择”策略（problems / enum / primary / properties
 * 计数与旧实现一致），而不是简单的“第一个能通过的分支”。
 */

// ---------------- JS 语义 helper ----------------

internal val jsxUndefined: dynamic = js("undefined")

private val jsxTruthyJs: dynamic = js("function (v) { return !!v ? 1 : 0; }")
internal fun jsxTruthy(v: dynamic): Boolean = (jsxTruthyJs(v) as Int) != 0

private val jsxTypeofJs: dynamic = js("function (v) { return typeof v; }")
internal fun jsxTypeof(v: dynamic): String = jsxTypeofJs(v) as String

private val jsxIsUndefinedJs: dynamic = js("function (v) { return v === undefined ? 1 : 0; }")
internal fun jsxIsUndefined(v: dynamic): Boolean = (jsxIsUndefinedJs(v) as Int) != 0

private val jsxIsArrayJs: dynamic = js("function (v) { return Array.isArray(v) ? 1 : 0; }")
internal fun jsxIsArray(v: dynamic): Boolean = (jsxIsArrayJs(v) as Int) != 0

internal fun jsxIsObject(v: dynamic): Boolean = jsxTypeof(v) == "object" && !jsxIsNull(v)

private val jsxIsNullJs: dynamic = js("function (v) { return v === null ? 1 : 0; }")
internal fun jsxIsNull(v: dynamic): Boolean = (jsxIsNullJs(v) as Int) != 0

internal fun jsxIsString(v: dynamic): Boolean = jsxTypeof(v) == "string"

private val jsxStrictEqJs: dynamic = js("function (a, b) { return a === b ? 1 : 0; }")
internal fun jsxStrictEq(a: dynamic, b: dynamic): Boolean = (jsxStrictEqJs(a, b) as Int) != 0

private val jsxNewObjectJs: dynamic = js("function () { return {}; }")
internal fun jsxNewObject(): dynamic = jsxNewObjectJs()

private val jsxNewArrayJs: dynamic = js("function () { return []; }")
internal fun jsxNewArray(): dynamic = jsxNewArrayJs()

private val jsxKeysJs: dynamic = js("function (o) { return Object.keys(o); }")
internal fun jsxKeys(o: dynamic): dynamic = jsxKeysJs(o)

private val jsxHasOwnJs: dynamic = js("function (o, k) { return o.hasOwnProperty(k) ? 1 : 0; }")
internal fun jsxHasOwn(o: dynamic, k: dynamic): Boolean = (jsxHasOwnJs(o, k) as Int) != 0

private val jsxArrayIndexOfJs: dynamic = js("function (a, v) { return a.indexOf(v); }")
internal fun jsxArrayIndexOf(a: dynamic, v: dynamic): Int = jsxArrayIndexOfJs(a, v) as Int

private val jsxStringJs: dynamic = js("function (v) { return String(v); }")
internal fun jsxString(v: dynamic): dynamic = jsxStringJs(v)

private val jsxJsonStringifyJs: dynamic = js("JSON.stringify")
internal fun jsxJsonStringify(v: dynamic): dynamic = jsxJsonStringifyJs(v)

internal fun jsxLength(v: dynamic): Int = v.length as Int

/** 与旧实现 `objects.equals` 等价的 JSON 深比较（enum / const 用）。 */
private val jsxDeepEqualJs: dynamic = js(
    """
    (function () {
        function eq(a, b) {
            if (a === b) return true;
            if (typeof a !== typeof b) return false;
            if (a === null || b === null || typeof a !== 'object') return false;
            var aArray = Array.isArray(a);
            var bArray = Array.isArray(b);
            if (aArray !== bArray) return false;
            if (aArray) {
                if (a.length !== b.length) return false;
                for (var i = 0; i < a.length; i++) {
                    if (!eq(a[i], b[i])) return false;
                }
                return true;
            }
            var aKeys = Object.keys(a);
            var bKeys = Object.keys(b);
            if (aKeys.length !== bKeys.length) return false;
            for (var j = 0; j < aKeys.length; j++) {
                var key = aKeys[j];
                if (!Object.prototype.hasOwnProperty.call(b, key)) return false;
                if (!eq(a[key], b[key])) return false;
            }
            return true;
        }
        return function (a, b) { return eq(a, b) ? 1 : 0; };
    })()
    """
)
internal fun jsxDeepEqual(a: dynamic, b: dynamic): Boolean = (jsxDeepEqualJs(a, b) as Int) != 0

// ---------------- schema 匹配 helper ----------------

/**
 * 旧 `normalizeResourceForMatching`：去掉 fragment / query。
 * 这里不做完整 URI 解析，只处理 CA 会遇到的 `scheme://path` 形式。
 */
private val jsxNormalizeResourceJs: dynamic = js(
    """
    function (resource) {
        if (typeof resource !== 'string') return '';
        var hash = resource.indexOf('#');
        if (hash >= 0) resource = resource.substring(0, hash);
        var query = resource.indexOf('?');
        if (query >= 0) resource = resource.substring(0, query);
        return resource;
    }
    """
)
private fun jsxNormalizeResource(resource: dynamic): dynamic = jsxNormalizeResourceJs(resource)

/**
 * 旧 `utils/glob.createRegex` 的最小移植。
 *
 * 旧实现会先给 pattern 加上 `**\/` 前缀，因此 `*.json`、`json.json` 都能匹配
 * `ca://json.json`；CA 的 `fileMatch: ["ca://json.json"]` 也是靠这个语义命中的。
 */
private val jsxGlobTestJs: dynamic = js(
    """
    (function () {
        function createRegex(glob) {
            var str = '**/' + String(glob);
            var reStr = '';
            var inGroup = false;
            var c;
            for (var i = 0, len = str.length; i < len; i++) {
                c = str[i];
                switch (c) {
                    case '/':
                    case '$':
                    case '^':
                    case '+':
                    case '.':
                    case '(':
                    case ')':
                    case '=':
                    case '!':
                    case '|':
                        reStr += '\\' + c;
                        break;
                    case '?':
                        reStr += '.';
                        break;
                    case '[':
                    case ']':
                        reStr += c;
                        break;
                    case '{':
                        inGroup = true;
                        reStr += '(';
                        break;
                    case '}':
                        inGroup = false;
                        reStr += ')';
                        break;
                    case ',':
                        if (inGroup) {
                            reStr += '|';
                            break;
                        }
                        reStr += '\\' + c;
                        break;
                    case '*':
                        var prevChar = str[i - 1];
                        var starCount = 1;
                        while (str[i + 1] === '*') {
                            starCount++;
                            i++;
                        }
                        var nextChar = str[i + 1];
                        var isGlobstar = starCount > 1 &&
                            (prevChar === '/' || prevChar === undefined || prevChar === '{' || prevChar === ',') &&
                            (nextChar === '/' || nextChar === undefined || nextChar === ',' || nextChar === '}');
                        if (isGlobstar) {
                            if (nextChar === '/') {
                                i++;
                            } else if (prevChar === '/' && reStr.slice(-2) === '\\/') {
                                reStr = reStr.substring(0, reStr.length - 2);
                            }
                            reStr += '((?:[^/]*(?:/|$))*)';
                        } else {
                            reStr += '([^/]*)';
                        }
                        break;
                    default:
                        reStr += c;
                }
            }
            reStr = '^' + reStr + '$';
            return new RegExp(reStr);
        }
        return function (glob, text) {
            try {
                return createRegex(glob).test(text) ? 1 : 0;
            } catch (e) {
                return 0;
            }
        };
    })()
    """
)
private fun jsxGlobTest(glob: dynamic, text: dynamic): Boolean = (jsxGlobTestJs(glob, text) as Int) != 0

// ---------------- $ref / JSON pointer helper ----------------

/** 本地 `#/...` JSON pointer 解析；`#` 表示 schema 根。无法解析时返回 undefined。 */
private val jsxResolvePointerJs: dynamic = js(
    """
    function (root, pointer) {
        if (typeof pointer !== 'string') return undefined;
        if (pointer === '#') return root;
        if (pointer.charAt(0) !== '#' || pointer.charAt(1) !== '/') return undefined;
        var path = pointer.substring(2);
        try {
            path = decodeURIComponent(path);
        } catch (e) {
            // 保持原串，后续按普通 key 处理
        }
        var parts = path.split('/');
        var current = root;
        for (var i = 0; i < parts.length; i++) {
            if (current === undefined || current === null) return undefined;
            var key = parts[i].replace(/~1/g, '/').replace(/~0/g, '~');
            current = current[key];
        }
        return current;
    }
    """
)
private fun jsxResolvePointer(root: dynamic, pointer: dynamic): dynamic = jsxResolvePointerJs(root, pointer)

/** 解析 `$ref`（只支持本地 `#/...`），返回目标 schema；无法解析时原样返回。 */
internal fun jsxResolveSchemaRef(schema: dynamic, rootSchema: dynamic): dynamic {
    var current = schema
    var guard = 0
    while (jsxIsObject(current) && jsxTruthy(current["\$ref"])) {
        val ref = current["\$ref"]
        if (!jsxIsString(ref)) break
        val value = ref as String
        if (!value.startsWith("#")) break
        val target = jsxResolvePointer(rootSchema, value)
        if (jsxIsUndefined(target) || jsxStrictEq(target, current)) break
        current = target
        guard++
        if (guard > 64) break
    }
    return current
}

// ---------------- schema registry ----------------

/**
 * 等价于旧 `JSONSchemaService` + `configure` 的极小实现。
 *
 * CA 每次 `configure` 只注册一个 schema；这里仍支持多个 schema 同时命中
 * （旧实现会组合成 `allOf`），保证 configure 的语义不会被悄悄缩窄。
 */
internal class JsonSchemaRegistry {
    private val entries: dynamic = jsxNewArray()

    internal var validationEnabled: Boolean = true
        private set
    internal var allowComments: Boolean = false
        private set

    fun configure(settings: dynamic) {
        while (jsxLength(entries) > 0) entries.pop()
        validationEnabled = true
        allowComments = false

        if (jsxIsUndefined(settings) || !jsxTruthy(settings)) return
        if (jsxStrictEq(settings.validate, false)) validationEnabled = false
        if (jsxTruthy(settings.allowComments)) allowComments = true

        val schemas = settings.schemas
        if (jsxIsUndefined(schemas) || !jsxIsArray(schemas)) return
        var index = 0
        while (index < jsxLength(schemas)) {
            val raw = schemas[index]
            if (jsxIsObject(raw)) {
                val entry = jsxNewObject()
                entry.uri = raw.uri
                entry.fileMatch = raw.fileMatch
                entry.schema = raw.schema
                entries.push(entry)
            }
            index++
        }
    }

    /** 文档里 `$schema` 指向的已注册 schema（与旧 `getSchemaFromProperty` 对齐）。 */
    private fun schemaFromDocumentProperty(jsonDocument: dynamic): dynamic {
        if (jsxIsUndefined(jsonDocument) || !jsxIsObject(jsonDocument)) return jsxUndefined
        val root = jsonDocument.root
        if (!jsxIsObject(root) || !jsxStrictEq(root.type, "object")) return jsxUndefined
        val properties = root.properties
        if (jsxIsUndefined(properties) || !jsxIsArray(properties)) return jsxUndefined
        var index = 0
        while (index < jsxLength(properties)) {
            val property = properties[index]
            val keyNode = property.keyNode
            val valueNode = property.valueNode
            if (jsxTruthy(keyNode) && jsxStrictEq(keyNode.value, "\$schema") &&
                jsxTruthy(valueNode) && jsxStrictEq(valueNode.type, "string")
            ) {
                return valueNode.value
            }
            index++
        }
        return jsxUndefined
    }

    private fun entryMatches(entry: dynamic, resource: dynamic): Boolean {
        val fileMatch = entry.fileMatch
        if (jsxIsUndefined(fileMatch) || !jsxIsArray(fileMatch) || jsxLength(fileMatch) == 0) {
            // 没有 fileMatch 时退化为 uri 精确匹配（阶段 2 的补充语义）。
            val uri = entry.uri
            return jsxIsString(uri) && jsxStrictEq(uri, resource)
        }

        var include = false
        var index = 0
        while (index < jsxLength(fileMatch)) {
            var pattern = fileMatch[index]
            if (!jsxIsString(pattern)) {
                index++
                continue
            }
            var text = pattern as String
            if (text.isEmpty()) {
                index++
                continue
            }
            val isInclude = text[0] != '!'
            if (!isInclude) text = text.substring(1)
            if (text.startsWith("/")) text = text.substring(1)
            if (text.isNotEmpty() && jsxGlobTest(text, resource)) {
                include = isInclude
            }
            index++
        }
        return include
    }

    /** 按 `$schema` 或 fileMatch/uri 找出所有命中的 schema（保持注册顺序）。 */
    fun schemasForResource(resource: dynamic, jsonDocument: dynamic): dynamic {
        val result = jsxNewArray()

        val schemaId = schemaFromDocumentProperty(jsonDocument)
        if (!jsxIsUndefined(schemaId)) {
            var index = 0
            while (index < jsxLength(entries)) {
                val entry = entries[index]
                if (jsxStrictEq(entry.uri, schemaId) && !jsxIsUndefined(entry.schema)) {
                    result.push(entry.schema)
                }
                index++
            }
            if (jsxLength(result) > 0) return result
        }

        val normalized = jsxNormalizeResource(resource)
        var index = 0
        while (index < jsxLength(entries)) {
            val entry = entries[index]
            if (entryMatches(entry, normalized) && !jsxIsUndefined(entry.schema)) {
                result.push(entry.schema)
            }
            index++
        }
        return result
    }

    /**
     * 与旧 `getSchemaForResource` 对齐的查询：
     * 单个命中返回该 schema；多个命中组合成 `allOf`；没有命中返回 undefined。
     */
    fun schemaForResource(resource: dynamic, jsonDocument: dynamic): dynamic {
        val schemas = schemasForResource(resource, jsonDocument)
        val count = jsxLength(schemas)
        if (count == 0) return jsxUndefined
        if (count == 1) return schemas[0]
        val combined = jsxNewObject()
        combined.allOf = schemas
        return combined
    }
}

// ---------------- 匹配收集器 ----------------

/**
 * 旧 `SchemaCollector` 的移植。
 *
 * `focusOffset == -1` 表示不限范围；子收集器（oneOf / anyOf / allOf 分支）固定为 -1，
 * 这与旧实现 `newSub()` 的行为一致。`enabled == false` 时只做校验、不收集，供
 * `doValidation` 复用同一套递归。
 */
internal class JsonSchemaCollector(
    private val focusOffset: Int,
    private val exclude: dynamic,
    private val enabled: Boolean
) {
    internal val schemas: dynamic = jsxNewArray()

    fun include(node: dynamic): Boolean {
        if (!enabled) return true
        if (focusOffset != -1) {
            val start = node.offset as Int
            val end = start + (node.length as Int)
            if (!(focusOffset >= start && focusOffset < end)) return false
        }
        if (!jsxIsUndefined(exclude) && jsxStrictEq(node, exclude)) return false
        return true
    }

    fun add(node: dynamic, schema: dynamic) {
        if (!enabled) return
        val item = jsxNewObject()
        item.node = node
        item.schema = schema
        schemas.push(item)
    }

    fun newSub(): JsonSchemaCollector = JsonSchemaCollector(-1, exclude, enabled)

    fun merge(other: JsonSchemaCollector) {
        if (!enabled) return
        var index = 0
        while (index < jsxLength(other.schemas)) {
            schemas.push(other.schemas[index])
            index++
        }
    }
}

// ---------------- 校验结果（旧 ValidationResult 的精简版） ----------------

internal class JsonValidationResult {
    internal var problems: dynamic = jsxNewArray()
    internal var propertiesMatches: Int = 0
    internal var propertiesValueMatches: Int = 0
    internal var primaryValueMatches: Int = 0
    internal var enumValueMatch: Boolean = false
    internal var enumValues: dynamic = jsxUndefined
    internal val processedProperties: dynamic = jsxNewArray()

    fun hasProblems(): Boolean = jsxLength(problems) > 0

    fun merge(other: JsonValidationResult) {
        problems = problems.concat(other.problems)
        propertiesMatches += other.propertiesMatches
        propertiesValueMatches += other.propertiesValueMatches
        mergeProcessedProperties(other)
    }

    fun mergeProcessedProperties(other: JsonValidationResult) {
        var index = 0
        while (index < jsxLength(other.processedProperties)) {
            val property = other.processedProperties[index]
            if (jsxArrayIndexOf(processedProperties, property) < 0) {
                processedProperties.push(property)
            }
            index++
        }
    }

    fun mergePropertyMatch(other: JsonValidationResult) {
        problems = problems.concat(other.problems)
        propertiesMatches++
        if (other.enumValueMatch || (!other.hasProblems() && other.propertiesMatches > 0)) {
            propertiesValueMatches++
        }
        if (other.enumValueMatch && !jsxIsUndefined(other.enumValues) &&
            jsxLength(other.enumValues) == 1
        ) {
            primaryValueMatches++
        }
    }

    fun mergeEnumValues(other: JsonValidationResult) {
        if (!enumValueMatch && !other.enumValueMatch &&
            !jsxIsUndefined(enumValues) && !jsxIsUndefined(other.enumValues)
        ) {
            enumValues = enumValues.concat(other.enumValues)
        }
    }

    fun updateEnumMismatchProblemMessages() {
        if (enumValueMatch || jsxIsUndefined(enumValues)) return
        val joined = jsxNewArray()
        var index = 0
        while (index < jsxLength(enumValues)) {
            joined.push(jsxJsonStringify(enumValues[index]))
            index++
        }
        val message = "Value is not accepted. Valid values: " +
            joined.join(", ") + "."
        index = 0
        while (index < jsxLength(problems)) {
            val problem = problems[index]
            if (jsxStrictEq(problem.code, 1)) {
                problem.message = message
            }
            index++
        }
    }

    fun compare(other: JsonValidationResult): Int {
        val hasProblems = hasProblems()
        if (hasProblems != other.hasProblems()) {
            return if (hasProblems) -1 else 1
        }
        if (enumValueMatch != other.enumValueMatch) {
            return if (other.enumValueMatch) -1 else 1
        }
        if (primaryValueMatches != other.primaryValueMatches) {
            return primaryValueMatches - other.primaryValueMatches
        }
        if (propertiesValueMatches != other.propertiesValueMatches) {
            return propertiesValueMatches - other.propertiesValueMatches
        }
        return propertiesMatches - other.propertiesMatches
    }
}

// ---------------- 校验引擎 ----------------

private fun jsxAddProblem(
    result: JsonValidationResult,
    offset: Int,
    length: Int,
    message: dynamic
) {
    val location = jsxNewObject()
    location.offset = offset
    location.length = length
    val problem = jsxNewObject()
    problem.location = location
    problem.message = message
    result.problems.push(problem)
}

private fun jsxAddProblemWithCode(
    result: JsonValidationResult,
    offset: Int,
    length: Int,
    message: dynamic,
    code: Int
) {
    val location = jsxNewObject()
    location.offset = offset
    location.length = length
    val problem = jsxNewObject()
    problem.location = location
    problem.message = message
    problem.code = code
    result.problems.push(problem)
}

private fun jsxErrorMessage(schema: dynamic, fallback: String): dynamic {
    val errorMessage = schema.errorMessage
    return if (jsxTruthy(errorMessage)) errorMessage else fallback
}

private fun jsxMatchesType(node: dynamic, type: dynamic): Boolean {
    if (jsxStrictEq(node.type, type)) return true
    return jsxStrictEq(type, "integer") &&
        jsxStrictEq(node.type, "number") &&
        jsxTruthy(node.isInteger)
}

private fun jsxAsSchema(schema: dynamic): dynamic {
    if (jsxStrictEq(schema, true)) return jsxNewObject()
    if (jsxStrictEq(schema, false)) {
        val not = jsxNewObject()
        not["not"] = jsxNewObject()
        return not
    }
    return schema
}

/** 节点 → JS 值（enum / const 深比较用）。 */
private fun jsxNodeValue(node: dynamic): dynamic {
    if (jsxIsUndefined(node) || jsxIsUndefined(node.type)) return jsxUndefined
    val type = node.type
    if (jsxStrictEq(type, "object")) {
        val value = jsxNewObject()
        val properties = node.properties
        var index = 0
        while (!jsxIsUndefined(properties) && index < jsxLength(properties)) {
            val property = properties[index]
            value[property.keyNode.value] = jsxNodeValue(property.valueNode)
            index++
        }
        return value
    }
    if (jsxStrictEq(type, "array")) {
        val value = jsxNewArray()
        val items = node.items
        var index = 0
        while (!jsxIsUndefined(items) && index < jsxLength(items)) {
            value.push(jsxNodeValue(items[index]))
            index++
        }
        return value
    }
    return node.value
}

private fun jsxSchemaPropertyKeys(schema: dynamic): dynamic {
    val keys = schema.properties
    if (jsxIsUndefined(keys) || !jsxIsObject(keys)) return jsxNewArray()
    return jsxKeys(keys)
}

private fun jsxValidateNode(
    node: dynamic,
    schemaRef: dynamic,
    rootSchema: dynamic,
    result: JsonValidationResult,
    collector: JsonSchemaCollector,
    severity: Int,
    resolveRefs: Boolean
) {
    if (jsxIsUndefined(node) || !jsxTruthy(node)) return
    if (!collector.include(node)) return

    if (jsxStrictEq(node.type, "property")) {
        jsxValidateNode(node.valueNode, schemaRef, rootSchema, result, collector, severity, resolveRefs)
        return
    }

    val schema = if (resolveRefs) jsxResolveSchemaRef(schemaRef, rootSchema) else schemaRef
    validateNodeKeywords(node, schema, rootSchema, result, collector, severity, resolveRefs)

    if (jsxStrictEq(node.type, "object")) {
        validateObjectNode(node, schema, rootSchema, result, collector, severity, resolveRefs)
    } else if (jsxStrictEq(node.type, "array")) {
        validateArrayNode(node, schema, rootSchema, result, collector, severity, resolveRefs)
    }

    collector.add(node, schema)
}

/** 旧的 `_validateNode()`：type、allOf、anyOf/oneOf、enum。 */
private fun validateNodeKeywords(
    node: dynamic,
    schema: dynamic,
    rootSchema: dynamic,
    result: JsonValidationResult,
    collector: JsonSchemaCollector,
    severity: Int,
    resolveRefs: Boolean
) {
    if (!jsxIsObject(schema)) return

    val hasType = !jsxIsUndefined(schema.type) && jsxTruthy(schema.type)
    if (hasType) {
        val type = schema.type
        if (jsxIsArray(type)) {
            var matched = false
            var index = 0
            while (index < jsxLength(type)) {
                if (jsxMatchesType(node, type[index])) {
                    matched = true
                    break
                }
                index++
            }
            if (!matched) {
                var joined = ""
                index = 0
                while (index < jsxLength(type)) {
                    if (index > 0) joined += ", "
                    joined += jsxString(type[index])
                    index++
                }
                jsxAddProblem(
                    result,
                    node.offset as Int,
                    node.length as Int,
                    jsxErrorMessage(schema, "Incorrect type. Expected one of " + joined + ".")
                )
            }
        } else if (!jsxMatchesType(node, type)) {
            jsxAddProblem(
                result,
                node.offset as Int,
                node.length as Int,
                jsxErrorMessage(schema, "Incorrect type. Expected \"" + jsxString(type) + "\".")
            )
        }
    }

    val allOf = schema.allOf
    if (jsxIsArray(allOf)) {
        var index = 0
        while (index < jsxLength(allOf)) {
            val subResult = JsonValidationResult()
            val subCollector = collector.newSub()
            jsxValidateNode(node, jsxAsSchema(allOf[index]), rootSchema, subResult, subCollector, severity, resolveRefs)
            result.merge(subResult)
            collector.merge(subCollector)
            index++
        }
    }

    val notValue = schema["not"]
    if (!jsxIsUndefined(notValue) && !jsxIsNull(notValue)) {
        val notSchema = jsxAsSchema(notValue)
        if (jsxTruthy(notSchema)) {
            val notResult = JsonValidationResult()
            val notCollector = collector.newSub()
            jsxValidateNode(node, notSchema, rootSchema, notResult, notCollector, severity, resolveRefs)
            if (!notResult.hasProblems()) {
                jsxAddProblem(
                    result,
                    node.offset as Int,
                    node.length as Int,
                    jsxErrorMessage(schema, "Matches a schema that is not allowed.")
                )
            }
        }
    }

    val anyOf = schema.anyOf
    if (jsxIsArray(anyOf)) {
        testAlternatives(node, anyOf, false, rootSchema, result, collector, severity, resolveRefs)
    }
    val oneOf = schema.oneOf
    if (jsxIsArray(oneOf)) {
        testAlternatives(node, oneOf, true, rootSchema, result, collector, severity, resolveRefs)
    }

    val enumValues = schema["enum"]
    if (jsxIsArray(enumValues)) {
        val value = jsxNodeValue(node)
        var matched = false
        var index = 0
        while (index < jsxLength(enumValues)) {
            if (jsxDeepEqual(value, enumValues[index])) {
                matched = true
                break
            }
            index++
        }
        result.enumValues = enumValues
        result.enumValueMatch = matched
        if (!matched) {
            var joined = ""
            index = 0
            while (index < jsxLength(enumValues)) {
                if (index > 0) joined += ", "
                joined += jsxJsonStringify(enumValues[index])
                index++
            }
            jsxAddProblemWithCode(
                result,
                node.offset as Int,
                node.length as Int,
                jsxErrorMessage(
                    schema,
                    "Value is not accepted. Valid values: " + joined + "."
                ),
                1
            )
        }
    }
}

/**
 * 旧 `testAlternatives(alternatives, maxOneMatch)`。
 * 返回匹配分支数量；诊断只采用“最佳候选”的结果。
 */
private fun testAlternatives(
    node: dynamic,
    alternatives: dynamic,
    maxOneMatch: Boolean,
    rootSchema: dynamic,
    result: JsonValidationResult,
    collector: JsonSchemaCollector,
    severity: Int,
    resolveRefs: Boolean
): Int {
    val matches = jsxNewArray()

    var bestResult: JsonValidationResult? = null
    var bestCollector: JsonSchemaCollector? = null

    var index = 0
    while (index < jsxLength(alternatives)) {
        val subSchema = jsxAsSchema(alternatives[index])
        val subResult = JsonValidationResult()
        val subCollector = collector.newSub()
        jsxValidateNode(node, subSchema, rootSchema, subResult, subCollector, severity, resolveRefs)

        if (!subResult.hasProblems()) {
            matches.push(subSchema)
        }

        if (bestResult == null) {
            bestResult = subResult
            bestCollector = subCollector
        } else {
            val best = bestResult
            if (!maxOneMatch && !subResult.hasProblems() && !best.hasProblems()) {
                bestCollector!!.merge(subCollector)
                best.propertiesMatches += subResult.propertiesMatches
                best.propertiesValueMatches += subResult.propertiesValueMatches
                best.mergeProcessedProperties(subResult)
            } else {
                val compared = subResult.compare(best)
                if (compared > 0) {
                    bestResult = subResult
                    bestCollector = subCollector
                } else if (compared == 0) {
                    bestCollector!!.merge(subCollector)
                    best.mergeEnumValues(subResult)
                }
            }
        }
        index++
    }

    if (jsxLength(matches) > 1 && maxOneMatch) {
        jsxAddProblem(
            result,
            node.offset as Int,
            1,
            "Matches multiple schemas when only one must validate."
        )
    }

    val best = bestResult
    if (best != null) {
        best.updateEnumMismatchProblemMessages()
        result.merge(best)
        collector.merge(bestCollector!!)
    }
    return jsxLength(matches)
}

private fun validateObjectNode(
    node: dynamic,
    schema: dynamic,
    rootSchema: dynamic,
    result: JsonValidationResult,
    collector: JsonSchemaCollector,
    severity: Int,
    resolveRefs: Boolean
) {
    if (!jsxIsObject(schema)) return

    val seenKeys = jsxNewObject()
    val unprocessed = jsxNewArray()
    val properties = node.properties
    var index = 0
    while (!jsxIsUndefined(properties) && index < jsxLength(properties)) {
        val property = properties[index]
        val key = property.keyNode.value
        seenKeys[key] = property.valueNode
        if (jsxArrayIndexOf(unprocessed, key) < 0) unprocessed.push(key)
        index++
    }

    val required = schema.required
    if (jsxIsArray(required)) {
        index = 0
        while (index < jsxLength(required)) {
            val propertyName = required[index]
            if (!jsxTruthy(seenKeys[propertyName])) {
                val parent = node.parent
                val keyNode = if (jsxTruthy(parent) && jsxStrictEq(parent.type, "property")) {
                    parent.keyNode
                } else {
                    jsxUndefined
                }
                val offset: Int
                val length: Int
                if (jsxTruthy(keyNode)) {
                    offset = keyNode.offset as Int
                    length = keyNode.length as Int
                } else {
                    offset = node.offset as Int
                    length = 1
                }
                jsxAddProblem(
                    result,
                    offset,
                    length,
                    "Missing property \"" + jsxString(propertyName) + "\"."
                )
            }
            index++
        }
    }

    val schemaProperties = schema.properties
    if (jsxIsObject(schemaProperties)) {
        val keys = jsxSchemaPropertyKeys(schema)
        index = 0
        while (index < jsxLength(keys)) {
            val propertyName = keys[index]
            removeProcessed(unprocessed, result, propertyName)
            val propertySchema = schemaProperties[propertyName]
            val child = seenKeys[propertyName]
            if (jsxTruthy(child)) {
                if (jsxStrictEq(propertySchema, false)) {
                    addDisallowedProperty(result, child, propertyName, schema)
                } else if (jsxStrictEq(propertySchema, true)) {
                    result.propertiesMatches++
                    result.propertiesValueMatches++
                } else if (!jsxIsUndefined(propertySchema)) {
                    val childResult = JsonValidationResult()
                    jsxValidateNode(child, propertySchema, rootSchema, childResult, collector, severity, resolveRefs)
                    result.mergePropertyMatch(childResult)
                }
            }
            index++
        }
    }

    val additionalProperties = schema.additionalProperties
    if (!jsxIsUndefined(additionalProperties)) {
        val pending = jsxNewArray()
        index = 0
        while (index < jsxLength(unprocessed)) {
            pending.push(unprocessed[index])
            index++
        }
        index = 0
        while (index < jsxLength(pending)) {
            val propertyName = pending[index]
            removeProcessed(unprocessed, result, propertyName)
            val child = seenKeys[propertyName]
            if (jsxTruthy(child)) {
                if (jsxStrictEq(additionalProperties, false)) {
                    addDisallowedProperty(result, child, propertyName, schema)
                } else if (!jsxStrictEq(additionalProperties, true)) {
                    val childResult = JsonValidationResult()
                    jsxValidateNode(
                        child,
                        additionalProperties,
                        rootSchema,
                        childResult,
                        collector,
                        severity,
                        resolveRefs
                    )
                    result.mergePropertyMatch(childResult)
                }
            }
            index++
        }
    }
}

private fun removeProcessed(
    unprocessed: dynamic,
    result: JsonValidationResult,
    propertyName: dynamic
) {
    val position = jsxArrayIndexOf(unprocessed, propertyName)
    if (position >= 0) unprocessed.splice(position, 1)
    if (jsxArrayIndexOf(result.processedProperties, propertyName) < 0) {
        result.processedProperties.push(propertyName)
    }
}

private fun addDisallowedProperty(
    result: JsonValidationResult,
    child: dynamic,
    propertyName: dynamic,
    schema: dynamic
) {
    val propertyNode = child.parent
    val keyNode = if (jsxTruthy(propertyNode) && jsxTruthy(propertyNode.keyNode)) {
        propertyNode.keyNode
    } else {
        child
    }
    jsxAddProblem(
        result,
        keyNode.offset as Int,
        keyNode.length as Int,
        jsxErrorMessage(schema, "Property " + jsxString(propertyName) + " is not allowed.")
    )
}

private fun validateArrayNode(
    node: dynamic,
    schema: dynamic,
    rootSchema: dynamic,
    result: JsonValidationResult,
    collector: JsonSchemaCollector,
    severity: Int,
    resolveRefs: Boolean
) {
    if (!jsxIsObject(schema)) return

    val items = node.items
    val itemCount = if (jsxIsUndefined(items)) 0 else jsxLength(items)

    var index = 0
    val prefixItems = schema.prefixItems
    if (jsxIsArray(prefixItems)) {
        val max = if (jsxLength(prefixItems) < itemCount) jsxLength(prefixItems) else itemCount
        while (index < max) {
            val itemResult = JsonValidationResult()
            jsxValidateNode(
                items[index],
                jsxAsSchema(prefixItems[index]),
                rootSchema,
                itemResult,
                collector,
                severity,
                resolveRefs
            )
            result.mergePropertyMatch(itemResult)
            result.processedProperties.push(jsxString(index))
            index++
        }
    }

    val itemsSchema = schema.items
    if (jsxIsArray(itemsSchema) && !jsxIsArray(prefixItems)) {
        // 旧实现默认按 2020-12 处理；这里额外兼容旧写法的 tuple items，行为对 CA 无影响。
        val max = if (jsxLength(itemsSchema) < itemCount) jsxLength(itemsSchema) else itemCount
        while (index < max) {
            val itemResult = JsonValidationResult()
            jsxValidateNode(
                items[index],
                jsxAsSchema(itemsSchema[index]),
                rootSchema,
                itemResult,
                collector,
                severity,
                resolveRefs
            )
            result.mergePropertyMatch(itemResult)
            result.processedProperties.push(jsxString(index))
            index++
        }
    } else if (!jsxIsUndefined(itemsSchema)) {
        if (jsxStrictEq(itemsSchema, false)) {
            if (index < itemCount) {
                jsxAddProblem(
                    result,
                    node.offset as Int,
                    node.length as Int,
                    "Array has too many items according to schema. Expected " +
                        jsxString(index) + " or fewer."
                )
            }
            while (index < itemCount) {
                result.processedProperties.push(jsxString(index))
                result.propertiesValueMatches++
                index++
            }
        } else if (jsxStrictEq(itemsSchema, true)) {
            while (index < itemCount) {
                result.processedProperties.push(jsxString(index))
                result.propertiesValueMatches++
                index++
            }
        } else {
            while (index < itemCount) {
                val itemResult = JsonValidationResult()
                jsxValidateNode(items[index], itemsSchema, rootSchema, itemResult, collector, severity, resolveRefs)
                result.mergePropertyMatch(itemResult)
                index++
            }
        }
    }
}

/** 校验一个 schema，返回旧 `ValidationResult.problems` 形状的数组。 */
internal fun jsonValidateProblems(
    root: dynamic,
    schema: dynamic,
    severity: Int,
    resolveRefs: Boolean = true
): dynamic {
    val result = JsonValidationResult()
    val collector = JsonSchemaCollector(-1, jsxUndefined, false)
    jsxValidateNode(root, schema, schema, result, collector, severity, resolveRefs)
    return result.problems
}

/** 校验并收集匹配 schema，返回 `[{ node, schema }]`。 */
internal fun jsonMatchingSchemas(
    rootOrDocument: dynamic,
    schema: dynamic,
    offset: Int,
    exclude: dynamic,
    resolveRefs: Boolean
): dynamic {
    var root = rootOrDocument
    if (jsxIsObject(rootOrDocument) && !jsxIsUndefined(rootOrDocument.root)) {
        root = rootOrDocument.root
    }
    if (jsxIsUndefined(root) || !jsxTruthy(root)) return jsxNewArray()
    if (jsxIsUndefined(schema) || !jsxTruthy(schema)) return jsxNewArray()

    val collector = JsonSchemaCollector(offset, exclude, true)
    jsxValidateNode(
        root,
        schema,
        schema,
        JsonValidationResult(),
        collector,
        2,
        resolveRefs
    )
    return collector.schemas
}

// ---------------- 对外 JS 门面 ----------------

/** 独立可用的 schema service 门面（阶段 2 内部与测试用）。 */
fun buildJsonSchemaService(): dynamic {
    val registry = JsonSchemaRegistry()
    val api = jsxNewObject()
    api.configure = { settings: dynamic ->
        registry.configure(settings)
        jsxUndefined
    }
    api.getSchemaForResource = { resource: dynamic, jsonDocument: dynamic ->
        registry.schemaForResource(resource, jsonDocument)
    }
    api.schemasForResource = { resource: dynamic, jsonDocument: dynamic ->
        registry.schemasForResource(resource, jsonDocument)
    }
    api.getMatchingSchemas = { rootOrDocument: dynamic, schema: dynamic, offset: dynamic ->
        val effectiveOffset = if (jsxIsUndefined(offset)) -1 else offset as Int
        jsonMatchingSchemas(rootOrDocument, schema, effectiveOffset, jsxUndefined, true)
    }
    return api
}
