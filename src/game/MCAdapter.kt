package ca.kotlin

/**
 * modules/MCAdapter.js 的 Kotlin/JS 版本。
 *
 * 保持原模块的 API 与调用方式：
 *   - onCreate 依据 MapScript.host 切换 getInfo / available 的实现；
 *   - newLevel / leaveGame / modTick / initialize 仍是 MapScript 的 hook 方法；
 *   - adapters 的 callback 会被 Common.showListChooser 以 .call(self) 调用，
 *     这里统一引用模块单例，不依赖动态 this；
 *   - Java 互操作集中在 JS helper 里，Kotlin 侧只传参数，不把局部变量写进 js("...")。
 *
 * 注意：原模块里 askShortcut 定义了两次，对象字面量以第二个（带 CA.trySave 的）为准，
 * 这里只保留最终生效的那份。
 */
private val mcAdapterApi: dynamic = js("({})")

// ---------------- JS 语义 helper ----------------
private val mcTruthyJs: dynamic = js("function (v) { return !!v; }")
private val mcStringJs: dynamic = js("function (v) { return String(v); }")
private val mcBooleanJs: dynamic = js("function (v) { return Boolean(v); }")
private val mcParseIntJs: dynamic = js("function (v) { return parseInt(v); }")
private val mcForInJs: dynamic = js("function (o, f) { for (var i in o) f(i); }")
private val mcInJs: dynamic = js("function (o, k) { return k in o; }")
private val mcIsNaNNs: dynamic = js("function (v) { return isNaN(v); }")
private val mcSqrtJs: dynamic = js("function (v) { return Math.sqrt(v); }")
private val mcArray2Js: dynamic = js("function (a, b) { return [a, b]; }")
private val mcArray3Js: dynamic = js("function (a, b, c) { return [a, b, c]; }")
private val mcBiomeTextJs: dynamic = js("function (name, id) { return String(name + '(' + id + ')'); }")

// ---------------- Java / Android helper ----------------
private val mcNewIntentJs: dynamic = js("function (action) { return new android.content.Intent(action); }")
private val mcUriFromPartsJs: dynamic = js("function (scheme, ssp, fragment) { return android.net.Uri.fromParts(scheme, ssp, fragment); }")
private val mcUriParseJs: dynamic = js("function (s) { return android.net.Uri.parse(s); }")
private val mcNewFileJs: dynamic = js("function (path) { return new java.io.File(path); }")
private val mcNewFileChildJs: dynamic = js("function (parent, child) { return new java.io.File(parent, child); }")
private val mcNewFileOutputStreamJs: dynamic = js("function (path) { return new java.io.FileOutputStream(path); }")
private val mcOpenAssetJs: dynamic = js("function (fn) { return ctx.getAssets().open(fn); }")
private val mcNewZipInputStreamJs: dynamic = js("function (stream) { return new java.util.zip.ZipInputStream(stream); }")
private val mcNewByteArrayJs: dynamic = js("function (size) { return java.lang.reflect.Array.newInstance(java.lang.Byte.TYPE, size); }")
private val mcExternalFilesDirJs: dynamic = js("function () { return ctx.getExternalFilesDir(null); }")
private val mcPackageInfoJs: dynamic = js("function (pkg, flags) { return ctx.getPackageManager().getPackageInfo(pkg, flags); }")
private val mcVersionCodeJs: dynamic = js("function (pkg) { return ctx.getPackageManager().getPackageInfo(pkg, 0).versionCode; }")
private val mcIsNameNotFoundJs: dynamic = js("function (e) { return e.javaException instanceof android.content.pm.PackageManager.NameNotFoundException; }")
private val mcShortcutIntentJs: dynamic = js("function () { return new android.content.Intent(ScriptInterface.ACTION_START_FROM_SHORTCUT); }")
private val mcSmallIconJs: dynamic = js("function () { return com.xero.ca.R.mipmap.icon_small; }")
private val mcAddImportFlagsJs: dynamic = js(
    "function (intent) { intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION | android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION | android.content.Intent.FLAG_ACTIVITY_NEW_TASK); }"
)
private val mcAddNewTaskFlagJs: dynamic = js("function (intent) { intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK); }")
private val mcAddCategoryDefaultJs: dynamic = js("function (intent) { intent.addCategory(android.content.Intent.CATEGORY_DEFAULT); }")

private fun mcComponentPackage(intent: dynamic): dynamic = intent.getComponent().getPackageName()

// ---------------- 信息查询 ----------------
private fun mcGetInfoModPE(id: dynamic): dynamic {
    var p: dynamic = null
    // 原实现 levelbiome 分支引用了未赋值的 b；保留 undefined 语义。
    var b: dynamic = js("undefined")
    try {
        if (id === "playernames") return js("Server").getAllPlayerNames()
        if (id === "playerposition") return mcArray3Js(js("Player").getX(), js("Player").getY(), js("Player").getZ())
        if (id === "playerrotation") {
            p = js("Player").getEntity()
            return mcArray2Js(js("Entity").getPitch(p), js("Entity").getYaw(p))
        }
        if (id === "pointedblockpos") {
            return mcArray3Js(js("Player").getPointedBlockX(), js("Player").getPointedBlockY(), js("Player").getPointedBlockZ())
        }
        if (id === "pointedblockinfo") {
            return mcArray3Js(js("Player").getPointedBlockId(), js("Player").getPointedBlockData(), js("Player").getPointedBlockSide())
        }
        if (id === "levelbiome") return mcBiomeTextJs(js("Level").biomeIdToName(b), b)
        if (id === "levelbrightness") {
            return js("Level").getBrightness(js("Player").getX(), js("Player").getY(), js("Player").getZ())
        }
        if (id === "leveltime") return js("Level").getTime()
    } catch (e: dynamic) {
        js("erp")(e, true)
    }
    return null
}

private fun mcGetInfoAdapter(id: dynamic): dynamic {
    if (!mcTruthyJs(mcAdapterApi.bundle) || !mcTruthyJs(mcAdapterApi.bundle.containsKey(id))) return null
    return mcAdapterApi.bundle.get(id)
}

private fun mcGetInfoWSServer(id: dynamic): dynamic {
    if (id === "playernames") return js("[]")
    if (id === "playerposition") {
        val pos: dynamic = mcAdapterApi.wsdata.playerpos
        return if (mcTruthyJs(pos)) pos else mcArray3Js(0, 0, 0)
    }
    if (id === "playerrotation") return mcArray2Js(0, 0)
    if (id === "pointedblockpos") return mcArray3Js(0, 0, 0)
    if (id === "pointedblockinfo") return mcArray3Js(0, 0, 0)
    if (id === "levelbiome") return "Unreachable"
    if (id === "levelbrightness") return 0
    if (id === "leveltime") return 0
    return js("undefined")
}

private fun mcGetInfoAndroid(id: dynamic): dynamic {
    if (mcTruthyJs(mcAdapterApi.available_Adapter())) return mcAdapterApi.getInfo_Adapter(id)
    return mcAdapterApi.getInfo_WSServer(id)
}

private fun mcAvailableAndroid(): dynamic {
    val adapter: dynamic = mcAdapterApi.available_Adapter()
    return if (mcTruthyJs(adapter)) adapter else mcAdapterApi.available_WSServer()
}

private fun mcAvailableAdapter(): dynamic = mcAdapterApi.bundle != null

private fun mcAvailableWSServer(): dynamic = js("WSServer").isConnected()

// ---------------- 生命周期 / hook ----------------
private fun mcOnCreate() {
    if (MapScript.host == "Android") {
        mcAdapterApi.getInfo = mcAdapterApi.getInfo_Android
        mcAdapterApi.available = mcAdapterApi.available_Android
    } else if (MapScript.host == "BlockLauncher") {
        mcAdapterApi.getInfo = mcAdapterApi.getInfo_ModPE
        mcAdapterApi.available = mcAdapterApi.available_ModPE
    }
}

private fun mcModTick() {
    mcAdapterApi.ticker = mcAdapterApi.ticker - 1
    if (mcAdapterApi.ticker > 0) return
    mcAdapterApi.notifyInfoUpdate()
    mcAdapterApi.ticker = 5
}

private fun mcNotifyInfoUpdate() {
    try {
        mcForInJs(mcAdapterApi.updateListener, { k: dynamic -> mcAdapterApi.updateListener[k]() })
    } catch (e: dynamic) {
        js("erp")(e, true)
    }
}

private fun mcUpdateInfo(data: dynamic) {
    mcAdapterApi.bundle = data
    mcAdapterApi.notifyInfoUpdate()
}

private fun mcCallHook(name: dynamic, args: dynamic) {
    if (mcTruthyJs(mcInJs(MapScript.global, name))) {
        MapScript.global[name].apply(null, args)
    }
}

private fun mcApplySense(t: dynamic) {
    if (MapScript.host != "Android" || mcTruthyJs(mcAdapterApi.asked)) return
    if (!mcTruthyJs(t.input)) t.input = js("[]")
    if (!mcTruthyJs(t.menu)) t.menu = js("({})")
    t.input.push("（加载适配器以显示更多游戏相关信息……）")
    t.menu["（加载适配器以显示更多游戏相关信息……）"] = { mcAdapterApi.listAdapters() }
}

private fun mcInitWSServer() {
    js("WSServer").subscribeEvent("PlayerTravelled", { json: dynamic ->
        mcAdapterApi.onWSPlayerTravelled(json)
        mcAdapterApi.notifyInfoUpdate()
    })
}

private fun mcDistance(dx: dynamic, dy: dynamic, dz: dynamic): dynamic =
    mcSqrtJs(dx * dx + dy * dy + dz * dz)

private fun mcOnWSPlayerTravelled(json: dynamic) {
    val obj = json.measurements
    var x: dynamic = obj.PosAvgX
    var y: dynamic = obj.PosAvgY
    var z: dynamic = obj.PosAvgZ
    var o: dynamic = mcAdapterApi.wsdata.playerpos
    if (mcTruthyJs(o)) {
        val d: dynamic = obj.MetersTravelled / mcAdapterApi.distance(x - o[0], y - o[1], z - o[2])
        x = x + (x - o[0]) * d
        y = y + (y - o[1]) * d
        z = z + (z - o[2]) * d
    } else {
        o = js("[]")
        mcAdapterApi.wsdata.playerpos = o
    }
    o[0] = x
    o[1] = y
    o[2] = z
}

// ---------------- 快捷方式 ----------------
private fun mcCreateShortcut(name: dynamic, pkg: dynamic) {
    val sc: dynamic = mcShortcutIntentJs()
    sc.setClassName("com.xero.ca", "com.xero.ca.MainActivity")
    sc.setData(mcUriFromPartsJs("package", pkg, null))
    js("AndroidBridge").createShortcut(sc, name, mcSmallIconJs())
}

private fun mcAskShortcut(name: dynamic, pkg: dynamic) {
    val z = js("({})")
    z.title = "是否创建快捷方式？"
    z.description = "需要给予对应权限"
    z.canSkip = false
    z.skip = { f: dynamic ->
        CA.settings.neverAskShortcut = mcBooleanJs(f)
        CA.trySave()
    }
    z.callback = { id: dynamic ->
        if (mcTruthyJs(CA.settings.neverAskShortcut)) {
            CA.settings.needShortcut = mcParseIntJs(id)
        }
        if (id == 0) {
            mcCreateShortcut(name, pkg)
        }
    }
    if (mcTruthyJs(CA.settings.neverAskShortcut)) {
        z.callback(CA.settings.needShortcut)
    } else {
        js("Common").showConfirmDialog(z)
    }
}

// ---------------- 适配器列表 ----------------
private fun mcAdapterEntry(text: String, description: String, callback: dynamic): dynamic {
    val e = js("({})")
    e.text = text
    e.description = description
    e.callback = callback
    return e
}

private fun mcInstallModPEAdapter() {
    val f: dynamic = mcNewFileChildJs(mcExternalFilesDirJs(), "ModPE适配器.js")
    val i: dynamic = mcNewIntentJs("net.zhuoweizhang.mcpelauncher.action.IMPORT_SCRIPT")
    if (mcTruthyJs(mcAdapterApi.existPackage("net.zhuoweizhang.mcpelauncher.pro"))) {
        i.setClassName("net.zhuoweizhang.mcpelauncher.pro", "net.zhuoweizhang.mcpelauncher.api.ImportScriptActivity")
    } else if (mcTruthyJs(mcAdapterApi.existPackage("net.zhuoweizhang.mcpelauncher"))) {
        i.setClassName("net.zhuoweizhang.mcpelauncher", "net.zhuoweizhang.mcpelauncher.api.ImportScriptActivity")
    } else {
        js("Common").toast("未找到BlockLauncher/BlockLauncher PRO")
        return
    }
    try {
        mcAdapterApi.unpackAssets("adapter/ModPE.js", f)
    } catch (e: dynamic) {
        js("Log").e(e)
        js("Common").toast("释放适配器失败\n" + mcStringJs(e))
        return
    }
    i.setDataAndType(js("AndroidBridge").fileToUri(f), "application/x-javascript")
    mcAddImportFlagsJs(i)
    if (!mcTruthyJs(js("SettingsCompat").startSafely(i))) {
        js("Common").toast("导入适配器失败")
        return
    }
    mcAdapterApi.askShortcut("BlockLauncher", mcComponentPackage(i))
}

private fun mcInstallBoxAdapter() {
    val f: dynamic = mcNewFileChildJs(mcExternalFilesDirJs(), "多玩我的世界盒子适配器.js")
    val i: dynamic = mcNewIntentJs(android.content.Intent.ACTION_VIEW)
    if (mcTruthyJs(mcAdapterApi.existPackage("com.duowan.groundhog.mctools"))) {
        i.setClassName("com.duowan.groundhog.mctools", "com.duowan.groundhog.mctools.activity.plug.PluginOutsideImportActivity")
    } else {
        js("Common").toast("未找到多玩我的世界盒子")
        return
    }
    try {
        mcAdapterApi.unpackAssets("adapter/ModPE_Sandbox.js", f)
    } catch (e: dynamic) {
        js("Log").e(e)
        js("Common").toast("释放适配器失败\n" + mcStringJs(e))
        return
    }
    i.setDataAndType(js("AndroidBridge").fileToUri(f), "application/x-javascript")
    mcAddImportFlagsJs(i)
    if (!mcTruthyJs(js("SettingsCompat").startSafely(i))) {
        js("Common").toast("导入适配器失败")
        return
    }
    mcAdapterApi.askShortcut("多玩我的世界盒子", mcComponentPackage(i))
    js("Common").showTextDialog("因为多玩我的世界盒子采用了沙盒机制，该适配器可能无法与本体连接。")
}

private fun mcInstallInnerCoreAdapter() {
    val ver: dynamic = mcAdapterApi.getPackageVersion("com.zhekasmirnov.innercore")
    if (ver > 10) {
        // 这个数字是原实现里的经验值，介于 1～25 之间。
        val f: dynamic = mcNewFileChildJs(mcExternalFilesDirJs(), "InnerCore适配器.icmod")
        val i: dynamic = mcNewIntentJs(android.content.Intent.ACTION_VIEW)
        i.setClassName("com.zhekasmirnov.innercore", "zhekasmirnov.launcher.core.ExtractModActivity")
        mcAdapterApi.unpackAssets("adapter/InnerCore.icmod", f)
        i.setDataAndType(js("AndroidBridge").fileToUri(f), "application/icmod")
        mcAddImportFlagsJs(i)
        if (!mcTruthyJs(js("SettingsCompat").startSafely(i))) {
            js("Common").toast("导入适配器失败")
            return
        }
    } else if (!mcTruthyJs(mcIsNaNNs(ver))) {
        // 旧版 InnerCore 不支持 .icmod，需要把包里的 ICAdpt/ 手工铺到 mods 目录。
        // 包里本来就是这几个文件，直接从 InnerCore.icmod 解压，不再单独维护一份副本。
        try {
            mcAdapterApi.unpackIcmod("adapter/InnerCore.icmod", "/sdcard/games/com.mojang/mods")
        } catch (e: dynamic) {
            js("Log").e(e)
            js("Common").toast("释放适配器失败\n" + mcStringJs(e))
            return
        }
        js("Common").toast("Mod文件已释放")
    } else {
        js("Common").toast("未找到InnerCore")
        return
    }
    mcAdapterApi.askShortcut("Inner Core", "com.zhekasmirnov.innercore")
}

private fun mcCreateAdapters(): dynamic {
    val adapters = js("[]")
    adapters.push(mcAdapterEntry("ModPE适配器（通用）", "适用于BlockLauncher/BlockLauncher PRO", { mcInstallModPEAdapter() }))
    adapters.push(mcAdapterEntry("ModPE适配器（盒子专版）", "适用于多玩我的世界盒子", { mcInstallBoxAdapter() }))
    adapters.push(mcAdapterEntry("InnerCore适配器", "旧版 | 适用于Inner Core", { mcInstallInnerCoreAdapter() }))
    return adapters
}

private fun mcListAdapters() {
    val self = mcAdapterApi
    js("Common").showListChooser(mcAdapterApi.adapters, { id: dynamic ->
        self.adapters[id].callback.call(self)
    })
    js("Common").toast("请选择系统适用的适配器")
}

// ---------------- 文件 / 包工具 ----------------
private fun mcUnpackIcmod(fn: dynamic, destDir: dynamic) {
    val zis: dynamic = mcNewZipInputStreamJs(mcOpenAssetJs(fn))
    try {
        var ze: dynamic
        var hr: dynamic
        while (true) {
            ze = zis.getNextEntry()
            if (ze == null) break
            val name = mcStringJs(ze.getName())
            if (mcTruthyJs(ze.isDirectory())) continue
            val dest: dynamic = mcNewFileChildJs(destDir, name)
            dest.getParentFile().mkdirs()
            val os: dynamic = mcNewFileOutputStreamJs(dest)
            val buf: dynamic = mcNewByteArrayJs(8192)
            while (true) {
                hr = zis.read(buf)
                if (!(hr > 0)) break
                os.write(buf, 0, hr)
            }
            os.close()
            zis.closeEntry()
        }
    } finally {
        zis.close()
    }
}

private fun mcUnpackAssets(fn: dynamic, path: dynamic) {
    val bufferSize = 8192
    val ins: dynamic = mcOpenAssetJs(fn)
    mcNewFileJs(path).getParentFile().mkdirs()
    val os: dynamic = mcNewFileOutputStreamJs(path)
    val buf: dynamic = mcNewByteArrayJs(bufferSize)
    var hr: dynamic
    while (true) {
        hr = ins.read(buf)
        if (!(hr > 0)) break
        os.write(buf, 0, hr)
    }
    ins.close()
    os.close()
}

private fun mcViewFile(path: dynamic, mime: dynamic) {
    val intent: dynamic = mcNewIntentJs(android.content.Intent.ACTION_VIEW)
    mcAddNewTaskFlagJs(intent)
    mcAddCategoryDefaultJs(intent)
    intent.setDataAndType(mcUriParseJs("file://" + mcStringJs(path)), mime)
    js("AndroidBridge").startActivity(intent)
}

private fun mcExistPackage(pkg: dynamic): dynamic {
    try {
        if (mcTruthyJs(mcPackageInfoJs(pkg, 0))) return true
    } catch (e: dynamic) {
        if (!mcTruthyJs(mcIsNameNotFoundJs(e))) {
            js("Log").e(e)
        }
    }
    return false
}

private fun mcGetPackageVersion(pkg: dynamic): dynamic {
    try {
        return mcVersionCodeJs(pkg)
    } catch (e: dynamic) {
        js("Log").e(e)
    }
    return js("NaN")
}

// ---------------- 安装 ----------------
fun installMCAdapter() {
    mcAdapterApi.targetVersion = 2
    mcAdapterApi.bundle = null
    mcAdapterApi.updateListener = js("({})")
    mcAdapterApi.ticker = 0
    mcAdapterApi.wsdata = js("({})")
    mcAdapterApi.inLevel = false

    mcAdapterApi.onCreate = { mcOnCreate() }
    mcAdapterApi.initialize = { mcAdapterApi.asked = true }
    mcAdapterApi.newLevel = { mcAdapterApi.inLevel = true }
    mcAdapterApi.leaveGame = { mcAdapterApi.inLevel = false }
    mcAdapterApi.modTick = { mcModTick() }

    mcAdapterApi.getInfo_Android = { id: dynamic -> mcGetInfoAndroid(id) }
    mcAdapterApi.available_Android = { mcAvailableAndroid() }
    mcAdapterApi.getInfo_ModPE = { id: dynamic -> mcGetInfoModPE(id) }
    mcAdapterApi.available_ModPE = { mcAdapterApi.inLevel }
    mcAdapterApi.getInfo_Adapter = { id: dynamic -> mcGetInfoAdapter(id) }
    mcAdapterApi.available_Adapter = { mcAvailableAdapter() }
    mcAdapterApi.getInfo_WSServer = { id: dynamic -> mcGetInfoWSServer(id) }
    mcAdapterApi.available_WSServer = { mcAvailableWSServer() }
    mcAdapterApi.getInfo = { id: dynamic -> null }
    mcAdapterApi.available = { false }

    mcAdapterApi.updateInfo = { data: dynamic -> mcUpdateInfo(data) }
    mcAdapterApi.notifyInfoUpdate = { mcNotifyInfoUpdate() }
    mcAdapterApi.callHook = { name: dynamic, args: dynamic -> mcCallHook(name, args) }
    mcAdapterApi.applySense = { t: dynamic -> mcApplySense(t) }
    mcAdapterApi.initWSServer = { mcInitWSServer() }
    mcAdapterApi.distance = { dx: dynamic, dy: dynamic, dz: dynamic -> mcDistance(dx, dy, dz) }
    mcAdapterApi.onWSPlayerTravelled = { json: dynamic -> mcOnWSPlayerTravelled(json) }

    mcAdapterApi.askShortcut = { name: dynamic, pkg: dynamic -> mcAskShortcut(name, pkg) }
    mcAdapterApi.createShortcut = { name: dynamic, pkg: dynamic -> mcCreateShortcut(name, pkg) }
    mcAdapterApi.adapters = mcCreateAdapters()
    mcAdapterApi.listAdapters = { mcListAdapters() }
    mcAdapterApi.unpackIcmod = { fn: dynamic, destDir: dynamic -> mcUnpackIcmod(fn, destDir) }
    mcAdapterApi.unpackAssets = { fn: dynamic, path: dynamic -> mcUnpackAssets(fn, path) }
    mcAdapterApi.viewFile = { path: dynamic, mime: dynamic -> mcViewFile(path, mime) }
    mcAdapterApi.existPackage = { pkg: dynamic -> mcExistPackage(pkg) }
    mcAdapterApi.getPackageVersion = { pkg: dynamic -> mcGetPackageVersion(pkg) }

    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("MCAdapter", mcAdapterApi)
    }
}
