package ca.kotlin

/**
 * JSONLanguageService 迁移阶段 1 的 API 组装。
 *
 * 只暴露阶段 1 已经可用的能力，方便以后在 `CAIntelliSenseModule.kt` 里替换旧 vendor：
 *
 *   val core = buildJsonLanguageServiceCore()
 *   core.TextDocument.create(uri, languageId, version, text)
 *   core.findNodeAtOffset(root, offset, includeRightBound)
 *   val service = core.getLanguageService(options)
 *   service.configure({ schemas: [...] })
 *   service.parseJSONDocument(document)
 *
 * 明确不做的事（按迁移计划）：
 * - 不调用 `MapScript.loadModule`；
 * - 不注册 `JSONLanguageService`；
 * - 不实现 schema 校验与补全：`doComplete` / `doValidation` 会抛出明确的未实现错误。
 */

private val jlsUndefined: dynamic = js("undefined")

private val jlsTruthyIntJs: dynamic = js("function (v) { return !!v ? 1 : 0; }")
private fun jlsTruthy(v: dynamic): Boolean = (jlsTruthyIntJs(v) as Int) != 0

private val jlsThrowJs: dynamic = js("function (m) { throw new Error(m); }")
private fun jlsThrowNotImplemented(method: String): dynamic {
    jlsThrowJs(
        "JSONLanguageService." + method +
            " is not implemented in migration stage 1 " +
            "(parser / TextDocument / findNodeAtOffset only)"
    )
    return jlsUndefined
}

/** findNodeAtOffset 的 JS 门面：补上 `includeRightBound === undefined -> false` 的默认语义。 */
private val jlsFindNodeAtOffsetFactoryJs: dynamic = js(
    """
    (function (impl) {
        return function (node, offset, includeRightBound) {
            return impl(node, offset, includeRightBound ? 1 : 0);
        };
    })
    """
)

private fun jlsBuildFindNodeAtOffset(): dynamic {
    val factory = jlsFindNodeAtOffsetFactoryJs
    return factory({ node: dynamic, offset: dynamic, includeRightBound: dynamic ->
        jsonFindNodeAtOffset(node, offset as Int, (includeRightBound as Int) != 0)
    })
}

private fun jlsBuildLanguageService(options: dynamic): dynamic {
    // 阶段 1 不消费 options（schemaRequestService / promiseConstructor 等），
    // 保留参数只为匹配旧签名；ca 的调用总会传对象。
    val service = js("({})")

    service.configure = { _settings: dynamic ->
        // 阶段 1：schema registry 尚未实现，只保证调用不报错。
        jlsUndefined
    }

    service.parseJSONDocument = { document: dynamic -> parseJsonDocument(document) }

    service.doComplete = { _document: dynamic, _position: dynamic, _jsonDocument: dynamic ->
        jlsThrowNotImplemented("doComplete")
    }

    service.doValidation = { _document: dynamic, _jsonDocument: dynamic ->
        jlsThrowNotImplemented("doValidation")
    }

    return service
}

/**
 * 组装阶段 1 的 JSONLanguageService API 对象：
 * `TextDocument`、`findNodeAtOffset`、`getLanguageService(options)`。
 *
 * 返回的是普通 JS 对象，不依赖 MapScript / 全局注册；调用方决定何时、以什么名字接入。
 */
fun buildJsonLanguageServiceCore(): dynamic {
    val core = js("({})")
    core.TextDocument = buildJsonTextDocumentCtor()
    core.findNodeAtOffset = jlsBuildFindNodeAtOffset()
    core.getLanguageService = { options: dynamic -> jlsBuildLanguageService(options) }
    return core
}

/**
 * 供 Node parity 测试（`test/json-language-service/`）通过 webpack bundle 调用的入口。
 *
 * 这是唯一一个 `@JsExport` 声明，只在构建产物里暴露成
 * `require(bundle).ca.kotlin.caJsonLanguageServiceStage1Core()`；
 * 不参与 Rhino/MapScript 的正式接入。
 */
@OptIn(kotlin.js.ExperimentalJsExport::class)
@JsExport
fun caJsonLanguageServiceStage1Core(): dynamic = buildJsonLanguageServiceCore()
