package ca.kotlin

/**
 * modules/LockClasses.js 的 Kotlin/JS 版本。
 *
 * 这段不是模块注册，而是最后执行的锁定脚本；必须在所有模块加载完成后再调用。
 */
private fun lockClasses() {
    Loader.lockMethods(CA.Library)
    Loader.lockProperty(CA, "Library")
    Loader.lockMethods(CA, js("['showDonate', 'showDonateDialog']"))
    Loader.freezeFields(Updater, js("['sources', 'betaSources']"))
    Loader.protectMethods(MapScript.global, "UserManager", js("['getSettingItem', 'processUriAction', 'enqueueExp', 'showAuthorize', 'initialize']"))
    Loader.protectMethods(MapScript.global, "IssueService", js("['showIssuesWithAgreement']"))
}

fun exposeLate() {
    val late = js("({})")
    late.installLockClasses = { lockClasses() }
    js("globalThis").CaKotlinLate = late
}
