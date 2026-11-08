package ca.kotlin

/**
 * 需要在不同加载位置安装的模块。
 *
 * bundle 提前加载，但 onCreate 必须等到原位置调用 install*，
 * 避免线程池、全局原型等副作用打乱启动顺序。
 */
fun exposePending() {
    val pending = js("({})")
    pending.installThreads = { installThreads() }
    pending.installIntColor = { installIntColor() }
    pending.installSafeFileUtils = { installSafeFileUtils() }
    pending.installSettingsCompat = { installSettingsCompat() }
    pending.installJavaReflect = { installJavaReflect() }
    pending.installDexPlugin = { installDexPlugin() }
    pending.installLPlugins = { installLPlugins() }
    pending.installAccel = { installAccel() }
    pending.installQuickAccel = { installQuickAccel() }
    pending.installExternalStorage = { installExternalStorage() }
    pending.installDBSettings = { installDBSettings() }
    pending.installMCAdapter = { installMCAdapter() }
    pending.installNeteaseAdapter = { installNeteaseAdapter() }
    pending.installNetworkUtils = { installNetworkUtils() }
    pending.installUpdater = { installUpdater() }
    pending.installWSServer = { installWSServer() }
    pending.installPushService = { installPushService() }
    pending.installIntl = { installIntl() }
    pending.installFCString = { installFCString() }
    pending.installISegment = { installISegment() }
    pending.installUserManager = { installUserManager() }
    pending.installIssueService = { installIssueService() }
    pending.installGiteeFeedback = { installGiteeFeedback() }
    pending.installAndroidBridge = { installAndroidBridge() }
    pending.installListAdapterBase = { installListAdapterBase() }
    pending.installEmptyAdapter = { installEmptyAdapter() }
    pending.installRhinoListAdapter = { installRhinoListAdapter() }
    pending.installFilterListAdapter = { installFilterListAdapter() }
    pending.installSimpleListAdapter = { installSimpleListAdapter() }
    pending.installMultipleListAdapter = { installMultipleListAdapter() }
    pending.installExpandableListAdapter = { installExpandableListAdapter() }
    pending.installMoreListAdapter = { installMoreListAdapter() }
    pending.installG = { installG() }
    pending.installL = { installL() }
    pending.installPWM = { installPWM() }
    pending.installConsolePanel = { installConsolePanel() }
    pending.installPopupWindow = { installPopupWindow() }
    pending.installPopupPage = { installPopupPage() }
    pending.installNetworkUI = { installNetworkUI() }
    pending.installJSONEdit = { installJSONEdit() }
    pending.installDebugUtils = { installDebugUtils() }
    pending.installCommon = { installCommon() }
    pending.installPlugins = { installPlugins() }
    pending.installTutorial = { installTutorial() }
    pending.installEasterEgg = { installEasterEgg() }
    pending.installFileLogger = { installFileLogger() }
    pending.installBuildConfig = { installBuildConfig() }
    pending.installUpdateSources = { installUpdateSources() }
    pending.installIntlIndex = { installIntlIndex() }
    pending.installBuiltinData = { installBuiltinData() }
    pending.installJSONLanguageService = { installJSONLanguageService() }
    pending.installCA = { installCA() }
    js("globalThis").CaKotlinPending = pending
}
