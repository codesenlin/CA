package ca.kotlin.json

import ca.kotlin.core.*


/** 阶段 4：用 Kotlin 实现注册 JSONLanguageService，替代 508KB vendor bundle。 */
private val jsonLanguageServiceApi: dynamic = js("({})")

fun installJSONLanguageService() {
    val core = buildJsonLanguageServiceStage3()
    jsonLanguageServiceApi.getLanguageService = core.getLanguageService
    jsonLanguageServiceApi.TextDocument = core.TextDocument
    jsonLanguageServiceApi.findNodeAtOffset = core.findNodeAtOffset
    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("JSONLanguageService", jsonLanguageServiceApi)
    }
}
