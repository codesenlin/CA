package ca.kotlin

/** intl/index.js 的 Kotlin/JS 版本。语言资源仍由 Loader.fromFile 在构建期内联。 */
private val intlZhLang: dynamic = js("({ language: 'zh', country: 'CN', ignoreCountry: true })")
private val intlEnLang: dynamic = js("({ language: 'en', country: 'US', ignoreCountry: true })")
private val intlZhSpec: dynamic = js("({ language: 'zh', country: 'CN', unspecifiedLang: true })")
private val intlEnSpec: dynamic = js("({ language: 'en', country: 'US' })")

fun installIntlIndex() {
    val g = js("globalThis")
    g.__caIntlZhLang = intlZhLang
    g.__caIntlEnLang = intlEnLang
    val intl = js("Intl")
    intl.defaultLang = Loader.fromFile("../../../data/intl/zh_CN/main.json")
    val langs = js("({ zh_CN: globalThis.__caIntlZhLang, en_US: globalThis.__caIntlEnLang })")
    val locale = intl.lookupLang(langs)
    if (locale == "en_US") {
        intl.loadLang(intlEnSpec, Loader.fromFile("../../../data/intl/en_US/main.json"), false)
    } else {
        intl.loadLang(intlZhSpec, js("({})"), false)
    }
}
