package ca.kotlin

/** 构建期注入的 BuildConfig/UpdateSources，改为 Kotlin 安装时从生成的 JSON 读取。 */
fun installBuildConfig() {
    js("globalThis").BuildConfig = Loader.fromFile("../../../build/kotlin-data/BuildConfig.json")
}

fun installUpdateSources() {
    js("globalThis").UpdateSources = Loader.fromFile("../../../build/kotlin-data/UpdateSources.json")
}
