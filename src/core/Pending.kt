package ca.kotlin.core

import ca.kotlin.android.*
import ca.kotlin.ca.*
import ca.kotlin.game.*
import ca.kotlin.json.*
import ca.kotlin.network.*
import ca.kotlin.text.*
import ca.kotlin.ui.*
import ca.kotlin.ui.adapter.*


/**
 * 需要在不同加载位置安装的模块。
 *
 * bundle 提前加载，但 onCreate 必须等到原位置调用 install*，
 * 避免线程池、全局原型等副作用打乱启动顺序。
 */

/**
 * 一次性按原顺序安装全部 Kotlin 模块。
 * 顺序就是旧 main.js 的加载顺序；尾部 LockClasses 必须在所有模块之后。
 */
fun installAll() {
    installBuildConfig()
    installFileLogger()
    installG()
    installJavaReflect()
    installIntColor()
    installL()
    installPWM()
    installPopupPage()
    installIntl()
    installAccel()
    installJSONLanguageService()
    installCA()
    installPopupWindow()
    installThreads()
    installQuickAccel()
    installCommon()
    installPlugins()
    installFCString()
    installTutorial()
    installListAdapterBase()
    installEmptyAdapter()
    installRhinoListAdapter()
    installFilterListAdapter()
    installSimpleListAdapter()
    installMultipleListAdapter()
    installExpandableListAdapter()
    installMoreListAdapter()
    installConsolePanel()
    installNetworkUI()
    installNetworkUtils()
    installUpdateSources()
    installUpdater()
    installISegment()
    installJSONEdit()
    installSettingsCompat()
    installEasterEgg()
    installMCAdapter()
    installAndroidBridge()
    installExternalStorage()
    installDexPlugin()
    installNeteaseAdapter()
    installWSServer()
    installUserManager()
    installIssueService()
    installGiteeFeedback()
    installPushService()
    installLPlugins()
    installSafeFileUtils()
    installDebugUtils()
    installIntlIndex()
    installBuiltinData()
    installLockClassesNow()
    installDBSettings()
}

fun exposePending() {
    val pending = js("({})")
    // 启动顺序统一由 Kotlin installAll 维护；JS 入口只负责触发一次。
    pending.installAll = { installAll() }
    js("globalThis").CaKotlinPending = pending
}
