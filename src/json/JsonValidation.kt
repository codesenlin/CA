package ca.kotlin

/**
 * JSONLanguageService 迁移阶段 2：validation 与 API 组装。
 *
 * 组合阶段 1 的能力（TextDocument / findNodeAtOffset / parseJSONDocument）与阶段 2 的
 * schema registry / validation：
 *
 *   val service = buildJsonLanguageServiceStage2().getLanguageService(options)
 *   service.configure({ schemas: [...] })
 *   val document = core.TextDocument.create(uri, 'json', 1, text)
 *   val jsonDocument = service.parseJSONDocument(document)
 *   service.doValidation(document, jsonDocument)
 *   jsonDocument.getMatchingSchemas(schema, offset)
 *
 * `doValidation` 返回的是旧 vendor 形状的诊断数组（`range` / `message` / `severity` /
 * `code` / `source`）。CA 的 `CAIntelliSenseModule.kt` 里对部分 message 做了精确匹配，
 * 所以这里的文案与旧 vscode-json-languageservice 保持一致。
 *
 * 明确不做的事：
 *  - 不调用 `MapScript.loadModule`、不注册 `JSONLanguageService`；
 *  - 不修改 `CAIntelliSenseModule.kt` 或旧 vendor；
 *  - `doComplete` 仍是明确的未实现错误（阶段 3）。
 */

private fun jvThrowNotImplemented(method: String): dynamic {
    val thrower: dynamic = js("function (m) { throw new Error(m); }")
    thrower(
        "JSONLanguageService." + method +
            " is not implemented in migration stage 2 " +
            "(schema registry / validation only)"
    )
    return jsxUndefined
}

// ---------------- diagnostic helper ----------------

private fun jvRange(document: dynamic, startOffset: Int, endOffset: Int): dynamic {
    val range = jsxNewObject()
    range.start = document.positionAt(startOffset)
    range.end = document.positionAt(endOffset)
    return range
}

private fun jvSignature(diagnostic: dynamic): String {
    val start = diagnostic.range.start
    return jsxString(start.line) + " " + jsxString(start.character) + " " +
        jsxString(diagnostic.message)
}

private fun jvAddProblem(diagnostics: dynamic, added: dynamic, diagnostic: dynamic) {
    val signature = jvSignature(diagnostic)
    if (!jsxHasOwn(added, signature)) {
        added[signature] = true
        diagnostics.push(diagnostic)
    }
}

private fun jvMapSeverity(value: dynamic): Int {
    if (jsxStrictEq(value, "error")) return 1
    if (jsxStrictEq(value, "warning")) return 2
    return -1
}

/** 处理 documentSettings 里的 `comments` / `trailingCommas` / `schemaValidation`。 */
private fun jvDocumentSettingSeverity(
    documentSettings: dynamic,
    key: String,
    defaultSeverity: Int
): Int {
    if (jsxIsUndefined(documentSettings) || !jsxTruthy(documentSettings)) {
        return defaultSeverity
    }
    val value = documentSettings[key]
    if (jsxIsUndefined(value)) return defaultSeverity
    return jvMapSeverity(value)
}

private fun jvProblemsToDiagnostics(
    document: dynamic,
    problems: dynamic,
    severity: Int
): dynamic {
    val diagnostics = jsxNewArray()
    var index = 0
    while (index < jsxLength(problems)) {
        val problem = problems[index]
        val location = problem.location
        val start = location.offset as Int
        val end = start + (location.length as Int)
        val diagnostic = jsxNewObject()
        diagnostic.range = jvRange(document, start, end)
        diagnostic.message = problem.message
        diagnostic.severity = severity
        if (!jsxIsUndefined(problem.code)) diagnostic.code = problem.code
        diagnostics.push(diagnostic)
        index++
    }
    return diagnostics
}

// ---------------- JSONDocument 扩展 ----------------

private fun jvMatchingSchemasForDocument(
    resource: dynamic,
    jsonDocument: dynamic,
    offset: Int,
    exclude: dynamic,
    registry: JsonSchemaRegistry,
    resolveRefs: Boolean
): dynamic {
    val result = jsxNewArray()
    val schemas = registry.schemasForResource(resource, jsonDocument)
    var index = 0
    while (index < jsxLength(schemas)) {
        val matches = jsonMatchingSchemas(jsonDocument.root, schemas[index], offset, exclude, resolveRefs)
        var matchIndex = 0
        while (matchIndex < jsxLength(matches)) {
            result.push(matches[matchIndex])
            matchIndex++
        }
        index++
    }
    return result
}

/**
 * 给阶段 1 的 JSONDocument 补上旧 vendor 的两个方法：
 *  - `getMatchingSchemas(schema, offset, exclude)`：CA 的 jsonPro 会直接调用；
 *  - `validate(textDocument, schema, severity?)`：与旧 JSONDocument 对齐。
 */
private fun jvAttachDocumentMethods(
    textDocument: dynamic,
    jsonDocument: dynamic,
    registry: JsonSchemaRegistry
) {
    jsonDocument.getMatchingSchemas = { schema: dynamic, offset: dynamic, exclude: dynamic ->
        val effectiveOffset = if (jsxIsUndefined(offset)) -1 else offset as Int
        if (jsxIsUndefined(schema)) {
            // 阶段 2 的补充语义：没有显式 schema 时按 configure 的 fileMatch 查找。
            jvMatchingSchemasForDocument(
                textDocument.uri,
                jsonDocument,
                effectiveOffset,
                exclude,
                registry,
                false
            )
        } else if (!jsxTruthy(schema)) {
            // 旧 JSONDocument.getMatchingSchemas 对 falsy schema 返回空数组。
            jsxNewArray()
        } else {
            jsonMatchingSchemas(jsonDocument.root, schema, effectiveOffset, exclude, false)
        }
    }

    jsonDocument.validate = { document: dynamic, schema: dynamic, severity: dynamic, _draft: dynamic ->
        if (jsxIsUndefined(schema) || !jsxTruthy(schema)) {
            jsxUndefined
        } else {
            val effectiveSeverity =
                if (jsxIsUndefined(severity) || !jsxTruthy(severity)) 2 else severity as Int
            // 与旧 JSONDocument.validate 一致：这里不做 $ref 解析（doValidation 才走
            // schema service 的解析路径）。
            val problems = jsonValidateProblems(jsonDocument.root, schema, effectiveSeverity, false)
            jvProblemsToDiagnostics(document, problems, effectiveSeverity)
        }
    }
}

// ---------------- doValidation ----------------

private fun jvValidateWithSchema(
    document: dynamic,
    jsonDocument: dynamic,
    schema: dynamic,
    severity: Int,
    diagnostics: dynamic,
    added: dynamic
) {
    if (jsxIsUndefined(jsonDocument.root) || !jsxTruthy(jsonDocument.root)) return
    val problems = jsonValidateProblems(jsonDocument.root, schema, severity)
    var index = 0
    while (index < jsxLength(problems)) {
        val problem = problems[index]
        val location = problem.location
        val start = location.offset as Int
        val end = start + (location.length as Int)
        val diagnostic = jsxNewObject()
        diagnostic.range = jvRange(document, start, end)
        diagnostic.message = problem.message
        diagnostic.severity = severity
        if (!jsxIsUndefined(problem.code)) diagnostic.code = problem.code
        jvAddProblem(diagnostics, added, diagnostic)
        index++
    }
}

private fun jvDoValidation(
    document: dynamic,
    jsonDocument: dynamic,
    documentSettings: dynamic,
    explicitSchema: dynamic,
    registry: JsonSchemaRegistry
): dynamic {
    if (!registry.validationEnabled) return jsxNewArray()

    val effectiveDocument =
        if (jsxIsUndefined(jsonDocument) || !jsxTruthy(jsonDocument)) {
            parseJsonDocument(document)
        } else {
            jsonDocument
        }

    val diagnostics = jsxNewArray()
    val added = jsxNewObject()

    val schemaValidationSeverity =
        jvDocumentSettingSeverity(documentSettings, "schemaValidation", 2)
    if (schemaValidationSeverity >= 0) {
        if (!jsxIsUndefined(explicitSchema) && jsxTruthy(explicitSchema)) {
            jvValidateWithSchema(
                document,
                effectiveDocument,
                explicitSchema,
                schemaValidationSeverity,
                diagnostics,
                added
            )
        } else {
            val schemas = registry.schemasForResource(document.uri, effectiveDocument)
            var index = 0
            while (index < jsxLength(schemas)) {
                jvValidateWithSchema(
                    document,
                    effectiveDocument,
                    schemas[index],
                    schemaValidationSeverity,
                    diagnostics,
                    added
                )
                index++
            }
        }
    }

    val trailingCommaSeverity =
        jvDocumentSettingSeverity(documentSettings, "trailingCommas", 1)
    val syntaxErrors = effectiveDocument.syntaxErrors
    var index = 0
    while (!jsxIsUndefined(syntaxErrors) && index < jsxLength(syntaxErrors)) {
        val problem = syntaxErrors[index]
        if (jsxStrictEq(problem.code, 519)) {
            if (trailingCommaSeverity < 0) {
                index++
                continue
            }
            if (!jsxStrictEq(problem.severity, trailingCommaSeverity)) {
                val copy = jsxNewObject()
                copy.range = problem.range
                copy.message = problem.message
                copy.severity = trailingCommaSeverity
                if (!jsxIsUndefined(problem.code)) copy.code = problem.code
                if (!jsxIsUndefined(problem.source)) copy.source = problem.source
                jvAddProblem(diagnostics, added, copy)
                index++
                continue
            }
        }
        jvAddProblem(diagnostics, added, problem)
        index++
    }

    val defaultCommentSeverity = if (registry.allowComments) -1 else 1
    val commentSeverity =
        jvDocumentSettingSeverity(documentSettings, "comments", defaultCommentSeverity)
    if (commentSeverity >= 0) {
        val comments = effectiveDocument.comments
        index = 0
        while (!jsxIsUndefined(comments) && index < jsxLength(comments)) {
            val diagnostic = jsxNewObject()
            diagnostic.range = comments[index]
            diagnostic.message = "Comments are not permitted in JSON."
            diagnostic.severity = commentSeverity
            diagnostic.code = 521
            jvAddProblem(diagnostics, added, diagnostic)
            index++
        }
    }

    return diagnostics
}

// ---------------- service ----------------

private fun jvBuildLanguageService(options: dynamic): dynamic {
    // 阶段 2 不消费 options；保留参数只为匹配旧签名。
    val registry = JsonSchemaRegistry()
    val service = jsxNewObject()

    service.configure = { settings: dynamic ->
        registry.configure(settings)
        jsxUndefined
    }

    service.parseJSONDocument = { document: dynamic ->
        val jsonDocument = parseJsonDocument(document)
        jvAttachDocumentMethods(document, jsonDocument, registry)
        jsonDocument
    }

    service.doValidation = { document: dynamic, jsonDocument: dynamic, documentSettings: dynamic, schema: dynamic ->
        jvDoValidation(document, jsonDocument, documentSettings, schema, registry)
    }

    service.getMatchingSchemas = { document: dynamic, jsonDocument: dynamic, schema: dynamic, offset: dynamic ->
        val effectiveOffset = if (jsxIsUndefined(offset)) -1 else offset as Int
        val effectiveDocument =
            if (jsxIsUndefined(jsonDocument) || !jsxTruthy(jsonDocument)) {
                parseJsonDocument(document)
            } else {
                jsonDocument
            }
        if (jsxIsUndefined(schema) || !jsxTruthy(schema)) {
            jvMatchingSchemasForDocument(
                document.uri,
                effectiveDocument,
                effectiveOffset,
                jsxUndefined,
                registry,
                true
            )
        } else {
            jsonMatchingSchemas(effectiveDocument.root, schema, effectiveOffset, jsxUndefined, true)
        }
    }

    service.doComplete = { _document: dynamic, _position: dynamic, _jsonDocument: dynamic ->
        jvThrowNotImplemented("doComplete")
    }

    return service
}

/**
 * 组装阶段 2 的 JSONLanguageService API：
 * `TextDocument`、`findNodeAtOffset`、`getLanguageService(options)`。
 *
 * 返回普通 JS 对象，不依赖 MapScript / 全局注册；调用方决定何时接入。
 */
fun buildJsonLanguageServiceStage2(): dynamic {
    val core = buildJsonLanguageServiceCore()
    core.getLanguageService = { options: dynamic -> jvBuildLanguageService(options) }
    return core
}

/**
 * 供 Node parity 测试（`test/json-language-service/stage2-parity.test.js`）调用的入口。
 *
 * 只通过 webpack bundle 暴露为
 * `require(bundle).ca.kotlin.caJsonLanguageServiceStage2()`；
 * 不参与 Rhino/MapScript 的正式接入。
 */
@OptIn(kotlin.js.ExperimentalJsExport::class)
@JsExport
fun caJsonLanguageServiceStage2(): dynamic = buildJsonLanguageServiceStage2()
