package ca.kotlin

/**
 * modules/CA.js 的总装入口。
 *
 * 各组只负责把成员挂到同一个 caApi 上；这里负责顺序、CA.Library/IntelliSense/Assist
 * 三个大子模块的加载，以及最终注册。全部组通过后才允许替换旧 CA.js。
 */
private val caApi: dynamic = js("({})")

fun installCA() {
    // 原 CA.js 末尾的对象字面量属性；先挂好，避免组安装期读到缺失字段。
    caApi.Library = buildCaLibraryModule()
    caApi.IntelliSense = buildCaIntelliSenseModule()
    caApi.Assist = buildCaAssistModule()

    installCaCore(caApi)
    installCaIcon(caApi)
    installCaMain(caApi)
    installCaFavorite(caApi)
    installCaSettings(caApi)
    installCaLibrary(caApi)
    installCaIdList(caApi)
    installCaBatch(caApi)
    installCaAgreement(caApi)

    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("CA", caApi)
    }
}
