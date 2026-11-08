package ca.kotlin

/**
 * modules/CA.js 的 core 组迁移（原文件约 1-313 行，以成员名为准）。
 *
 * 本文件只提供 installCaCore(api)：把 core 组的字段与方法挂到调用方传入的
 * 共享 API 对象上。不调用 MapScript.loadModule、不定义全局 caApi、不安装自己；
 * 后续由 CA.kt 的 installCA() 按固定顺序调用各组安装函数并注册整个对象。
 *
 * 迁移约定：
 *  - 原实现里的 this 改成显式 api；跨组成员（Library / showAgreementSync /
 *    checkFeatures / showIcon 等）只通过 api 动态解析，本文件不定义它们。
 *  - 保留原 JS 的 truthy、宽松相等、NaN、for-in 与数组长度语义；转换、比较、
 *    拼接统一走带参 JS helper，不在 js("...") 里引用 Kotlin 局部变量。
 *  - 原实现的既有行为/缺陷按原样保留，并在对应位置注释：
 *      * addHistory 读取 CA.settings.histroyCount（拼写错误；正常设置里是
 *        historyCount，所以历史容量实际上基本不生效）
 *      * load 的 Object.keys(...).forEach 第二个参数把 this 设为 settings，
 *        回调里的 this.enabledLibrarys / this.disabledLibrarys 指 settings
 *      * screenChangeHook 是命名函数表达式 function self(...)，上次屏幕名
 *        存在函数对象 self.l 上，而不是 CA 上
 *      * "恢复默认数据" 通过 CA.suppressAgreement 跳过同步协议，避免死锁
 *  - settings 默认值仍在 load() 无可用档案时构建，与旧行为一致。
 */

// ---------------- JS 语义 helper ----------------

private val caTruthyJs: dynamic = js("function (v) { return !!v; }")
private fun caTruthy(v: dynamic): Boolean = caTruthyJs(v)

private val caLengthJs: dynamic = js("function (v) { return v.length | 0; }")
private fun caLength(v: dynamic): Int = caLengthJs(v)

private val caStrJs: dynamic = js("function (v) { return String(v); }")
private fun caStr(v: dynamic): dynamic = caStrJs(v)

private val caAddJs: dynamic = js("function (a, b) { return a + b; }")
private fun caAdd(a: dynamic, b: dynamic): dynamic = caAddJs(a, b)

private val caSubJs: dynamic = js("function (a, b) { return a - b; }")
private fun caSub(a: dynamic, b: dynamic): dynamic = caSubJs(a, b)

private val caOrJs: dynamic = js("function (a, b) { return a || b; }")
private fun caOr(a: dynamic, b: dynamic): dynamic = caOrJs(a, b)

private val caLooseEqJs: dynamic = js("function (a, b) { return a == b; }")
private fun caLooseEq(a: dynamic, b: dynamic): Boolean = caLooseEqJs(a, b)

private val caLooseNeJs: dynamic = js("function (a, b) { return a != b; }")
private fun caLooseNe(a: dynamic, b: dynamic): Boolean = caLooseNeJs(a, b)

private val caStrictEqJs: dynamic = js("function (a, b) { return a === b; }")
private fun caStrictEq(a: dynamic, b: dynamic): Boolean = caStrictEqJs(a, b)

private val caLtJs: dynamic = js("function (a, b) { return a < b; }")
private fun caLt(a: dynamic, b: dynamic): Boolean = caLtJs(a, b)

private val caGtJs: dynamic = js("function (a, b) { return a > b; }")
private fun caGt(a: dynamic, b: dynamic): Boolean = caGtJs(a, b)

private val caLeJs: dynamic = js("function (a, b) { return a <= b; }")
private fun caLe(a: dynamic, b: dynamic): Boolean = caLeJs(a, b)

private val caGeJs: dynamic = js("function (a, b) { return a >= b; }")
private fun caGe(a: dynamic, b: dynamic): Boolean = caGeJs(a, b)

/** 对应原 JS 的 `v == undefined`（同时匹配 null 与 undefined）。 */
private val caLooseUndefinedJs: dynamic = js("function (v) { return v == undefined; }")
private fun caLooseUndefined(v: dynamic): Boolean = caLooseUndefinedJs(v)

private val caIsNaNJs: dynamic = js("function (v) { return isNaN(v); }")
private fun caIsNaN(v: dynamic): Boolean = caIsNaNJs(v)

private val caIsArrayJs: dynamic = js("function (v) { return Array.isArray(v); }")
private fun caIsArray(v: dynamic): Boolean = caIsArrayJs(v)

private val caInstanceOfJs: dynamic = js("function (v, c) { return v instanceof c; }")
private fun caInstanceOf(v: dynamic, c: dynamic): Boolean = caInstanceOfJs(v, c)

private val caObjectKeysJs: dynamic = js("function (o) { return Object.keys(o); }")
private fun caObjectKeys(o: dynamic): dynamic = caObjectKeysJs(o)

private val caDeleteJs: dynamic = js("function (o, k) { delete o[k]; }")
private fun caDelete(o: dynamic, k: dynamic) {
    caDeleteJs(o, k)
}

private val caDateParseJs: dynamic = js("function (v) { return Date.parse(v); }")
private fun caDateParse(v: dynamic): dynamic = caDateParseJs(v)

private val caNowJs: dynamic = js("function () { return Date.now(); }")
private fun caNow(): dynamic = caNowJs()

private val caNewFile1Js: dynamic = js("function (p) { return new java.io.File(p); }")
private fun caNewFile1(p: dynamic): dynamic = caNewFile1Js(p)

private val caNewFile2Js: dynamic = js("function (parent, child) { return new java.io.File(parent, child); }")
private fun caNewFile2(parent: dynamic, child: dynamic): dynamic = caNewFile2Js(parent, child)

/** 对应原 JS 的 /^\//.test(s)。 */
private val caTestSlashJs: dynamic = js("function (s) { return /^\\//.test(s); }")
private fun caTestSlash(s: dynamic): Boolean = caTestSlashJs(s)

private val caMinecraftVersionJs: dynamic = js("function () { return getMinecraftVersion(); }")
private fun caMinecraftVersion(): dynamic = caMinecraftVersionJs()

/**
 * 对应 getFavoriteDir 里的 `for (i in folder)`：
 * 包含数组下标与继承的可枚举属性，匹配 key 时用宽松相等，children 只做 truthy 判断。
 */
private val caFindFavoriteDirJs: dynamic = js(
    "function (folder, key) {" +
        " for (var i in folder) {" +
        "  if (key == folder[i].key && folder[i].children) return folder[i];" +
        " }" +
        " return undefined;" +
        "}"
)
private fun caFindFavoriteDir(folder: dynamic, key: dynamic): dynamic = caFindFavoriteDirJs(folder, key)

private val caNewFavoriteDirJs: dynamic = js("function (key) { return { key: key, children: [] }; }")

/** 生成带函数对象自身状态 self 的 1 参函数，保持 screenChangeHook 的 self.l 语义与 arity。 */
private val caMakeSelf1Js: dynamic = js(
    "function (f) { var self = function (a) { return f(self, this, a); }; return self; }"
)

private val caSaveDataJs: dynamic = js(
    "function (history, favorite, cmd, settings, publishDate) {" +
        " return { history: history, favorite: favorite, cmd: cmd, settings: settings, publishDate: publishDate };" +
        "}"
)

private val caDefaultHistoryJs: dynamic = js(
    "function () { return ['/say 你好，我是命令助手！左边是历史，右边是收藏，可以拖来拖去，也可以长按编辑哦']; }"
)

private val caDefaultFavoritesJs: dynamic = js(
    "function () { return [" +
        " { key: '获得命令方块', value: '/give @p command_block' }," +
        " { key: '关闭命令提示', value: '/gamerule commandblockoutput false' }," +
        " { key: '命令助手设置', value: '/help' }" +
        "]; }"
)

/** 延迟预热 ID 列表的回调；api 作为闭包参数传入，不依赖全局 CA。 */
private val caPrewarmJs: dynamic = js(
    "function (api) {" +
        " return function () {" +
        "  try { java.lang.Thread.sleep(1500); api.prewarmIDList(); } catch (e) { }" +
        " };" +
        "}"
)

/** 原 JS 的 30 * 24 * 60 * 60 * 1000。 */
private const val CA_THIRTY_DAYS_MS = 2592000000.0

// ---------------- core 成员实现 ----------------

/** 关于面板/版本信息里显示的版本号：默认只有版本名，带渠道构建时追加「-渠道」。 */
private fun caGetVersionLabel(): dynamic {
    val channel = js("BuildConfig").channel
    return caAdd(
        js("BuildConfig").version,
        if (caTruthy(channel) && caLooseNe(channel, "stable")) caAdd("-", channel) else ""
    )
}

private fun caInitialize(api: dynamic) {
    try {
        api.plugin = js("Plugins").inject(api)
        api.load()
        // suppressAgreement：运行时重建（如“恢复默认数据”）不能走同步等待协议的路径，
        // 那会卡在主线程上等用户点同意，而设置页此时还没退场，弹窗出不来 → 死锁。
        // 这种情况由调用方在初始化完成后异步弹协议。
        if (!caTruthy(api.suppressAgreement) &&
            !caGt(api.settings.readAgreement, caDateParse(js("BuildConfig").licenceUpdate))
        ) {
            api.showAgreementSync()
        }
        api.initializeAfterAgreement()
    } catch (e: dynamic) {
        js("erp")(e)
    }
}

private fun caInitializeAfterAgreement(api: dynamic) {
    try {
        api.checkFeatures()
        if (!caTruthy(api.hasFeature("enableCommand"))) {
            js("Common").showTextDialog(
                "兼容性警告\n\n您的Minecraft PE版本过低（" + caMinecraftVersion() +
                    "），没有命令和命令方块等功能，无法正常使用命令助手。请升级您的Minecraft PE至1.2及以上。"
            )
        } else if (!caTruthy(api.hasFeature("enableCommandBlock"))) {
            js("Common").showTextDialog(
                "兼容性警告\n\n您的Minecraft PE版本较低（" + caMinecraftVersion() +
                    "），可以使用命令，但没有命令方块等功能，部分命令助手的功能可能无法使用。推荐升级您的Minecraft PE至1.2及以上。"
            )
        } else if (caTruthy(api.hasFeature("version_1_1"))) {
            js("Common").showTextDialog(
                "兼容性警告\n\n您的Minecraft PE版本较低（" + caMinecraftVersion() +
                    "），可以使用命令，但没有ID表，且部分命令有bug。推荐升级您的Minecraft PE至1.2及以上，或者使用网易代理的我的世界最新版本。\n" +
                    "您也可在设置→拓展包→切换版本→自定义中设置版本为1.2。"
            )
        }
        // 延迟到启动界面稳定后再预热，避免和首屏初始化抢 CPU
        js("Threads").run(caPrewarmJs(api))
        js("Common").toast(
            "命令助手 " + api.version.join(".") + " by ProjectXero\n\n" + api.getTip(),
            1
        )
        api.fine = true
        api.screenChangeHook()
    } catch (e: dynamic) {
        js("erp")(e)
    }
}

private fun caUnload(api: dynamic) {
    try {
        api.trySave()
        js("G").ui(api.resetGUI)
    } catch (e: dynamic) {
        js("erp")(e)
    }
}

private fun caChatHook(api: dynamic, s: dynamic) {
    try {
        if (caTestSlash(s)) api.addHistory(s)
    } catch (e: dynamic) {
        js("erp")(e)
    }
}

private fun caScreenChangeHook(self: dynamic, thisObj: dynamic, screenNameIn: dynamic): dynamic {
    var screenName = screenNameIn
    try {
        if (caTruthy(screenName)) {
            self.l = screenName
        } else {
            screenName = self.l
        }
        if (!caTruthy(thisObj.fine)) return js("undefined")
        if (caLooseNe(js("MapScript").host, "BlockLauncher") ||
            !caTruthy(thisObj.settings.autoHideIcon) ||
            caGt(js("PopupPage").getCount(), 0)
        ) {
            return thisObj.showIcon()
        }
        if (caLooseEq(screenName, "chat_screen") ||
            caLooseEq(screenName, "command_block_screen") ||
            (caTruthy(caLength(thisObj.cmdstr)) && caLooseEq(screenName, "hud_screen"))
        ) {
            thisObj.showIcon()
        } else {
            thisObj.hideIcon()
        }
    } catch (e: dynamic) {
        js("erp")(e)
    }
    return js("undefined")
}

private fun caLoad(api: dynamic) {
    val pf = caNewFile1(api.profilePath)
    var f = js("SafeFileUtils").readJSON(pf, null)
    if (!caTruthy(f)) {
        val t = caNewFile2(
            js("android.os.Environment").getExternalStorageDirectory(),
            "games/com.mojang/minecraftWorlds/" +
                (if (caLooseEq(js("BuildConfig").variants, "release")) {
                    "xero_commandassist.dat"
                } else {
                    "xero_commandassist_snapshot.dat"
                })
        )
        if (caTruthy(t.isFile())) f = js("SafeFileUtils").readJSON(t, null)
        t.delete()
    }
    if (caTruthy(f) && caIsArray(f.history) &&
        caInstanceOf(f.favorite, js("Object")) && caInstanceOf(f.settings, js("Object"))
    ) {
        val settings = f.settings
        api.his = f.history
        api.fav = f.favorite
        api.cmdstr = if (caTruthy(f.cmd)) caStr(f.cmd) else ""
        api.settings = settings
        if (caTruthy(f.theme)) {
            settings.alpha = if (caTruthy(settings.alpha)) 0.75 else 1
            js("Common").loadTheme(f.theme)
        } else {
            js("Common").loadTheme(settings.theme)
        }
        if (!caTruthy(settings.enabledLibrarys)) settings.enabledLibrarys = api.Library.recommended
        if (!caTruthy(settings.coreLibrarys)) settings.coreLibrarys = js("[]")
        if (!caTruthy(settings.disabledLibrarys)) settings.disabledLibrarys = api.Library.defaultDisabled
        if (!caTruthy(settings.deprecatedLibrarys)) settings.deprecatedLibrarys = js("[]")
        if (caTruthy(settings.libPath)) {
            api.Library.enableLibrary(settings.libPath)
            caDelete(settings, "libPath")
        }
        // 原实现的第二个参数把回调里的 this 设为 settings；这里显式使用 settings，
        // 保持 this.enabledLibrarys / this.disabledLibrarys 的语义。
        val libKeys = caObjectKeys(api.Library.inner)
        var li = 0
        val libCount = caLength(libKeys)
        while (li < libCount) {
            val e = libKeys[li]
            if (caLt(settings.enabledLibrarys.indexOf(e), 0) &&
                caLt(settings.disabledLibrarys.indexOf(e), 0)
            ) {
                if (caGe(api.Library.recommended.indexOf(e), 0)) {
                    settings.enabledLibrarys.push(e)
                } else {
                    settings.disabledLibrarys.push(e)
                }
            }
            li++
        }
        if (caIsNaN(settings.firstUse)) {
            settings.firstUse = caSub(caDateParse(api.publishDate), CA_THIRTY_DAYS_MS) // 30d
        }
        if (caIsNaN(settings.nextAskSupport)) {
            settings.nextAskSupport = caAdd(caNow(), CA_THIRTY_DAYS_MS) // 30d
        }
        if (caLooseUndefined(settings.icon)) settings.icon = "default"
        if (!caIsArray(api.fav)) {
            val fav = js("[]")
            val keys = caObjectKeys(f.favorite)
            var fi = 0
            val favCount = caLength(keys)
            while (fi < favCount) {
                val e = keys[fi]
                val item = js("({})")
                item.key = e
                item.value = f.favorite[e]
                fav.push(item)
                fi++
            }
            api.fav = fav
        }
        if (!caTruthy(settings.customExpression)) settings.customExpression = js("[]")
        if (caLooseUndefined(settings.noWebImage)) settings.noWebImage = false
        if (!(caGe(settings.securityLevel, -9) && caLe(settings.securityLevel, 9))) {
            settings.securityLevel = 2
        }
        api.migrateSecurityLevel(settings)
        if (caTruthy(settings.customTips)) api.tips = settings.customTips
        if (caIsNaN(settings.libraryAutoUpdate)) settings.libraryAutoUpdate = 1
        if (!caTruthy(settings.quickBarActions)) {
            settings.quickBarActions = js("Object").copy(api.quickBarDefaultActions)
        }
        val variants = js("BuildConfig").variants
        if (caLooseEq(variants, "release")) {
            js("erp").notReport = api.settings.notReportError
        } else if (caLooseEq(variants, "snapshot")) {
            js("erp").notReport = false
        } else {
            js("erp").notReport = true
        }

        api.settingsVersion = caDateParse(f.publishDate)
        if (caLt(api.settingsVersion, caDateParse("2017-10-22"))) {
            settings.senseDelay = true
        }
        if (caLt(api.settingsVersion, caDateParse("2018-03-10"))) {
            settings.pasteMode = if (caTruthy(settings.disablePaste)) 0 else 1
        }
        if (caLt(api.settingsVersion, caDateParse("2018-12-03"))) {
            if (caLooseEq(settings.historyCount, 0)) settings.historyCount = 200
            api.his.splice(settings.historyCount)
        }

        api.Library.initLibrary({ flag: dynamic ->
            if (!caTruthy(flag)) js("Common").toast("有拓展包未能加载，可在「管理拓展包」里查看原因")
        })
        if (caLt(caDateParse(f.publishDate), caDateParse(api.publishDate))) {
            js("Updater").showNewVersionInfo(f.publishDate)
        }
    } else {
        if (caTruthy(pf.exists())) {
            js("erp")(
                "Profile cannot resolved:\n" + js("SafeFileUtils").readText(pf, "Content cannot read"),
                true
            )
        }
        // 风味构建可以换初始历史/欢迎语（例如 community 风味的社区版文案）
        val flavorData = js("BuildConfig").flavorData
        api.his = if (caTruthy(flavorData) && caIsArray(flavorData.welcome) &&
            caTruthy(caLength(flavorData.welcome))
        ) {
            flavorData.welcome.slice()
        } else {
            caDefaultHistoryJs()
        }
        api.fav = caDefaultFavoritesJs()
        api.cmdstr = ""
        api.settings = caDefaultSettings(api)
        api.tips = api.defalutTips
        js("Common").loadTheme()
        api.checkFeatures()
        api.Library.initLibrary()
    }
}

private fun caDefaultSettings(api: dynamic): dynamic {
    val settings = js("({})")
    settings.firstUse = caNow()
    settings.nextAskSupport = caAdd(caNow(), CA_THIRTY_DAYS_MS)
    settings.autoHideIcon = false
    settings.autoFormatCmd = false
    settings.alpha = 1
    settings.noAnimation = false
    settings.senseDelay = true
    settings.pasteMode = 1
    settings.historyCount = 200
    settings.splitScreenMode = false
    settings.keepWhenIME = false
    settings.icon = "default"
    settings.noWebImage = false
    settings.iconAlpha = 0
    settings.tipsRead = 0
    settings.iiMode = -1
    settings.libraryAutoUpdate = 1
    // 拓展包源（{url, name, enabled, builtin}），至少含内置官方源
    settings.librarySources = js("[]")
    settings.securityLevel = 2
    // 已执行过的安装器包：{ uri: {name, uuid, version, installer:true} }
    // 记录后就只显示在列表里，不再重新执行（否则每次启动都会重写脚本、重建快捷方式）
    settings.installedInstallers = js("({})")
    settings.securityLevelMigrated = true
    settings.enabledLibrarys = api.Library.recommended
    settings.coreLibrarys = js("[]")
    settings.disabledLibrarys = api.Library.defaultDisabled
    settings.deprecatedLibrarys = js("[]")
    settings.customExpression = js("[]")
    settings.quickBarActions = js("Object").copy(api.quickBarDefaultActions)
    return settings
}

private fun caSave(api: dynamic) {
    if (caTruthy(js("Common").theme)) api.settings.theme = js("Common").theme.id
    js("SafeFileUtils").writeJSON(
        caNewFile1(api.profilePath),
        caSaveDataJs(api.his, api.fav, api.cmdstr, api.settings, api.publishDate)
    )
}

private fun caAddHistory(api: dynamic, t: dynamic) {
    val i = api.his.indexOf(caStr(t))
    if (caGe(i, 0)) api.his.splice(i, 1)
    api.his.unshift(caStr(t))
    // 原实现的拼写错误：设置里的字段是 historyCount，这里读的是 histroyCount，
    // 所以历史容量限制实际上基本不生效。按原样保留。
    if (caGe(api.settings.histroyCount, 0)) {
        api.his.splice(api.settings.histroyCount)
    }
}

private fun caGetFavoriteDir(api: dynamic, key: dynamic, folderIn: dynamic, doNotCreate: dynamic): dynamic {
    var folder = folderIn
    if (!caTruthy(folder)) folder = api.fav
    val found = caFindFavoriteDir(folder, key)
    if (found !== js("undefined")) return found
    if (caTruthy(doNotCreate)) return js("null")
    val t = caNewFavoriteDirJs(key)
    folder.push(t)
    return t
}

private fun caAddFavorite(api: dynamic, data: dynamic, folderIn: dynamic) {
    var folder = folderIn
    if (!caTruthy(folder)) folder = api.fav
    if (caTruthy(data.children)) {
        val t = api.getFavoriteDir(data.key, folder, true)
        if (caTruthy(t)) {
            val a = data.children
            var i = 0
            val count = caLength(a)
            while (i < count) {
                api.addFavorite(a[i], t.children)
                i++
            }
            return
        }
    }
    folder.push(data)
}

private fun caRemoveFavorite(api: dynamic, data: dynamic, folderIn: dynamic) {
    var folder = folderIn
    if (!caTruthy(folder)) folder = api.fav
    val i = folder.indexOf(data)
    if (caLt(i, 0)) return
    folder.splice(i, 1)
}

/**
 * 安全级别编号迁移。
 * 旧：-1 受限 / 0 低 / 1 中 / 2 高
 * 新：-1 → 4（严格）/ 0 完全加载 / 1 不允许开放拓展包 / 2 中 / 3 高
 * 不迁移的话，老用户的「中」会变成新的「低」，等于被悄悄放宽了。
 */
private fun caMigrateSecurityLevel(api: dynamic, settingsIn: dynamic) {
    val settings = caOr(settingsIn, api.settings)
    if (!caTruthy(settings) || caTruthy(settings.securityLevelMigrated)) return
    val v = settings.securityLevel
    // 只有原来的「受限」换了编号（-1 → 4，现在叫「严格」），其余档位含义一一对应，不需要平移
    if (caStrictEq(v, -1)) settings.securityLevel = 4
    settings.securityLevelMigrated = true
}

private fun caTrySave(api: dynamic): Boolean {
    try {
        api.save()
        return true
    } catch (e: dynamic) {
        js("erp")(e, true)
        js("Common").showTextDialog(
            "命令助手无法在您的手机上运行：文件写入失败。\n原因可能为：\n1、您的内部存储没有足够的空间\n" +
                "2、文件被保护\n3、未开放文件读写权限\n\n请检查您的系统。\n\n错误原因：" + caStr(e)
        )
    }
    return false
}

// ---------------- 安装 ----------------

fun installCaCore(api: dynamic) {
    api.icon = js("null")
    api.qbar = js("null")
    api.gen = js("null")
    api.con = js("null")
    api.cmd = js("null")
    api.history = js("null")
    api.assist = js("null")
    api.fcs = js("null")
    api.paste = js("null")

    api.his = js("null")
    api.fav = js("null")
    api.cmdstr = ""
    api.settings = js("({})")
    api.fine = false

    api.profilePath = caAdd(
        js("MapScript").baseDir,
        if (caLooseEq(js("BuildConfig").variants, "release")) {
            "xero_commandassist.dat"
        } else {
            "xero_commandassist_snapshot.dat"
        }
    )
    api.name = "CA"
    api.author = "ProjectXero"
    api.uuid = "d4235eed-520c-4e23-9b67-d024a30ed54c"
    api.version = js("BuildConfig").versionCode
    api.versionName = js("BuildConfig").version
    api.getVersionLabel = { caGetVersionLabel() }
    api.publishDate = js("BuildConfig").date
    // 供拓展包/插件读取当前渠道（BuildConfig 是模块内的局部变量，eval 出来的作用域看不到）
    api.variants = js("BuildConfig").variants
    api.tips = js("[]")

    api.initialize = { caInitialize(api) }
    api.initializeAfterAgreement = { caInitializeAfterAgreement(api) }
    api.unload = { caUnload(api) }
    api.chatHook = { s: dynamic -> caChatHook(api, s) }
    // 命名函数表达式 self 的状态（self.l）留在函数对象上，保持原语义与 1 参 arity
    api.screenChangeHook = caMakeSelf1Js { self: dynamic, thisObj: dynamic, screenName: dynamic ->
        caScreenChangeHook(self, thisObj, screenName)
    }
    api.load = { caLoad(api) }
    api.save = { caSave(api) }
    api.addHistory = { t: dynamic -> caAddHistory(api, t) }
    api.getFavoriteDir = { key: dynamic, folder: dynamic, doNotCreate: dynamic ->
        caGetFavoriteDir(api, key, folder, doNotCreate)
    }
    api.addFavorite = { data: dynamic, folder: dynamic -> caAddFavorite(api, data, folder) }
    api.removeFavorite = { data: dynamic, folder: dynamic -> caRemoveFavorite(api, data, folder) }
    api.migrateSecurityLevel = { settings: dynamic -> caMigrateSecurityLevel(api, settings) }
    api.trySave = { caTrySave(api) }
}
