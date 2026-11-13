package ca.kotlin.network

import ca.kotlin.core.*


/**
 * modules/network/Updater.js 的 Kotlin/JS 版本。
 *
 * 更新检查、更新源回退、Beta 快照下载与各种确认框都保持原调用链；
 * 模块内部状态统一放在 updaterApi 上，避免依赖动态 this。
 */
private val updaterApi: dynamic = js("({})")

// ---------------- JS 语义 helper ----------------
private val upTruthyJs: dynamic = js("function (v) { return !!v; }")
private val upStringJs: dynamic = js("function (v) { return String(v); }")
private val upLengthJs: dynamic = js("function (v) { return v.length | 0; }")
private val upForInJs: dynamic = js("function (o, f) { for (var i in o) f(i); }")
private val upIsArrayJs: dynamic = js("function (v) { return Array.isArray(v); }")
private val upJoinJs: dynamic = js("function (a, sep) { return a.join(sep); }")
private val upIndexOfJs: dynamic = js("function (a, v) { return a.indexOf(v); }")
private val upObjectJs: dynamic = js("function (v) { return Object(v); }")
private val upNowJs: dynamic = js("function () { return Date.now(); }")
private val upDateParseJs: dynamic = js("function (v) { return Date.parse(v); }")
private val upIndirectEvalJs: dynamic = js("function (code) { return eval.call(null, code); }")
private val upOpenUrlJs: dynamic = js(
    """function (url) {
        var intent = new android.content.Intent(
            android.content.Intent.ACTION_VIEW,
            android.net.Uri.parse(url)
        );
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }"""
)
private val upNewSimpleDateFormatJs: dynamic = js(
    "function (p) { return new java.text.SimpleDateFormat(p); }"
)
private val upNewDateJs: dynamic = js("function (d) { return new java.util.Date(d); }")

private fun upTruthy(v: dynamic): Boolean = upTruthyJs(v)
private fun upString(v: dynamic): dynamic = upStringJs(v)
private fun upLength(v: dynamic): Int = upLengthJs(v)
private fun upIsArray(v: dynamic): Boolean = upIsArrayJs(v)
private fun upJoin(a: dynamic, sep: dynamic): dynamic = upJoinJs(a, sep)
private fun upIndexOf(a: dynamic, v: dynamic): Int = upIndexOfJs(a, v)
private fun upObject(v: dynamic): dynamic = upObjectJs(v)
private fun upNow(): dynamic = upNowJs()
private fun upDateParse(v: dynamic): dynamic = upDateParseJs(v)

private fun upToChineseDate(d: dynamic): dynamic {
    return upNewSimpleDateFormatJs("yyyy'年'MM'月'dd'日' HH:mm").format(upNewDateJs(d))
}

private fun upQueryFromSources(sources: dynamic): dynamic {
    var lastError: dynamic = js("undefined")
    var i = 0
    val n = upLength(sources)
    while (i < n) {
        try {
            return js("NetworkUtils").queryPage(sources[i])
        } catch (e: dynamic) {
            lastError = e
        }
        i++
    }
    throw lastError
}

private fun upGetUpdateInfo(source: dynamic, callback: dynamic) {
    try {
        var r: dynamic = js("undefined")
        if (upTruthy(updaterApi.cacheUpdateData[source.id])) {
            r = updaterApi.cacheUpdateData[source.id]
        } else {
            r = js("JSON").parse(updaterApi.queryFromSources(source.content))
            updaterApi.cacheUpdateData[source.id] = r
        }
        callback(null, r)
    } catch (e: dynamic) {
        js("Log").e(e)
        callback(e)
    }
}

private fun upCleanCache() {
    updaterApi.latest = null
    updaterApi.updateFlag = js("NaN")
    js("AndroidBridge").notifySettings()
}

private fun upGetVersionInfo(): dynamic {
    if (upTruthy(updaterApi.checking)) return "正在检查版本……"
    if (!upTruthy(updaterApi.latest)) return "版本：" + js("BuildConfig").date
    if (updaterApi.updateFlag > 0) {
        return "更新：" + js("BuildConfig").date + " -> " + updaterApi.latest
    } else if (updaterApi.updateFlag == 0) {
        return "您使用的是最新版本"
    } else {
        return "Beta版本：" + js("BuildConfig").date
    }
}

private fun upCheckUpdate(statusListener: dynamic, silently: dynamic): dynamic {
    if (!upTruthy(updaterApi.sources) || !upTruthy(updaterApi.sources.content) ||
        !(upLength(updaterApi.sources.content) > 0)
    ) {
        if (upTruthy(statusListener)) statusListener("completed", 0)
        if (!upTruthy(silently)) js("Common").toast("未配置更新源")
        return false
    }
    if (upTruthy(updaterApi.checking)) {
        js("Common").toast("正在检查更新中，请稍候")
        return false
    }
    updaterApi.checking = true
    if (upTruthy(statusListener)) statusListener("checking")
    js("Threads").run({
        try {
            updaterApi.getUpdateInfo(updaterApi.sources, { err: dynamic, info: dynamic ->
                updaterApi.checking = false
                if (upTruthy(err)) {
                    if (upTruthy(statusListener)) statusListener("errorGetInfo", err)
                    js("Common").toast("检测更新失败，请检查网络连接\n(" + err + ")")
                } else {
                    val flag = upDateParse(info.version) - upDateParse(js("BuildConfig").date)
                    updaterApi.latest = info.version
                    updaterApi.updateFlag = flag
                    if (flag > 0) {
                        if (upTruthy(statusListener)) statusListener("showingDialog")
                        updaterApi.showUpdateDialog(info, {
                            if (upTruthy(statusListener)) statusListener("dialogClosed")
                            js("undefined")
                        })
                    } else if (!upTruthy(silently)) {
                        if (flag == 0) {
                            js("Common").toast("当前已经是最新版本：" + js("BuildConfig").date)
                        } else {
                            js("Common").toast("目前您正在使用Beta版本，目前暂未公开Beta版的更新")
                        }
                        if (upTruthy(statusListener)) statusListener("completed", flag)
                    }
                }
                js("undefined")
            })
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
    return js("undefined")
}

private fun upTestSupport(requirements: dynamic): dynamic {
    if (!upIsArray(requirements)) return null
    val err = js("[]")
    val sdkInt = js("ScriptInterface").getAndroidSdk()
    val abis = js("AndroidBridge").getABIs()
    upForInJs(requirements) { i: dynamic ->
        val e = upObject(requirements[i])
        val type = e.type
        if (type == "expr") {
            try {
                upIndirectEvalJs(e.value)
            } catch (err2: dynamic) {
                err.push(err2)
            }
        } else if (type == "minsdk") {
            if (sdkInt < e.value) err.push("您的Android版本较低(" + sdkInt + "<" + e.value + ")")
        } else if (type == "maxsdk") {
            if (sdkInt > e.value) err.push("您的Android版本过高(" + sdkInt + ">" + e.value + ")")
        } else if (type == "abis") {
            if (!upIsArray(e.values)) {
                val values = js("[]")
                values.push(e.value)
                e.values = values
            }
            e.values.forEach({ abi: dynamic ->
                if (upIndexOf(abis, abi) < 0) err.push("您的CPU不支持" + abi + "指令集")
            })
        }
    }
    return if (upLength(err) > 0) err else js("undefined")
}

private fun upUnsupportText(unsupport: dynamic): dynamic {
    if (upTruthy(unsupport)) {
        val o = js("({})")
        o.text = "您的设备可能无法安装这个新版本，原因是：\n" + upJoin(unsupport, "\n") + "\n\n"
        o.color = "criticalcolor"
        o.bold = true
        return o
    }
    return ""
}

private fun upShowUpdateDialog(info: dynamic, callback: dynamic) {
    val unsupport = upTestSupport(info.requirements)
    var selected: dynamic = js("undefined")
    val buttons = js("[]")

    val hotfix = js("({})")
    hotfix.text = "快速更新"
    hotfix.id = "hotfix"
    hotfix.onclick = { cb: dynamic ->
        js("Common").showProgressDialog({ dia: dynamic ->
            dia.setText("下载中……")
            try {
                js("NetworkUtils").download(info.hotfix.url, js("MapScript").baseDir + "core.js")
                js("NetworkUtils").download(info.hotfix.sign, js("MapScript").baseDir + "core.sign")
                js("Common").toast("更新成功，将在下次启动时生效")
            } catch (e: dynamic) {
                js("Common").toast("下载更新失败\n" + e)
            }
            cb()
            js("undefined")
        })
        js("undefined")
    }
    hotfix.visible = {
        js("MapScript").host == "Android" && upTruthy(info.hotfix) &&
            info.hotfix.shell == js("ScriptInterface").getShellVersion()
    }
    buttons.push(hotfix)

    val manual = js("({})")
    manual.text = "手动更新"
    manual.id = "manaul"
    manual.onclick = { cb: dynamic ->
        updaterApi.chooseUpdateSource(info, cb)
        js("undefined")
    }
    buttons.push(manual)

    val later = js("({})")
    later.text = "稍后提醒"
    later.id = "remindLater"
    buttons.push(later)

    val never = js("({})")
    never.text = "不再提醒"
    never.id = "neverRemind"
    never.onclick = { cb: dynamic ->
        js("CA").settings.skipCheckUpdate = true
        js("Common").toast("命令助手将不再自动检查更新")
        cb()
        js("undefined")
    }
    never.visible = { upTruthy(unsupport) && upLength(unsupport) > 0 }
    buttons.push(never)

    val visible = buttons.filter({ e: dynamic ->
        var keep = true
        if (upTruthy(e.visible) && !upTruthy(e.visible())) keep = false
        keep
    })

    val raw = js("[]")
    val rawText: dynamic = { upUnsupportText(unsupport) }
    raw.push(rawText)
    val extraBlock = js("({})")
    val extra = js("[]")
    val latest = js("({})")
    latest.text = "最新版本：" + info.version
    latest.bold = true
    extra.push(latest)
    extra.push("\n发布时间：")
    extra.push(upString(updaterApi.toChineseDate(info.time)))
    extra.push("\n更新内容：\n")
    extra.push(info.info)
    extraBlock.extra = extra
    extraBlock.color = "textcolor"
    raw.push(extraBlock)

    val dialog = js("({})")
    dialog.title = "命令助手更新啦！"
    dialog.description = js("ISegment").rawJson(raw)
    dialog.buttons = visible.map({ e: dynamic -> e.text })
    dialog.callback = { i: dynamic ->
        selected = true
        val b = visible[i]
        if (upTruthy(b.onclick)) {
            b.onclick({
                if (upTruthy(callback)) callback(b.id)
                js("undefined")
            })
        } else if (upTruthy(callback)) {
            callback(b.id)
        }
        js("undefined")
    }
    dialog.onDismiss = {
        if (!upTruthy(selected) && upTruthy(callback)) callback("remindLater")
        js("undefined")
    }
    js("Common").showConfirmDialog(dialog)
}

private fun upChooseUpdateSource(info: dynamic, callback: dynamic) {
    val d = js("[]")
    upForInJs(info.downloads) { i: dynamic ->
        val item = js("({})")
        item.text = i
        item.description = info.downloads[i]
        d.push(item)
    }
    js("Common").showListChooser(d, { i: dynamic ->
        try {
            js("AndroidBridge").startActivity(upOpenUrlJs(d[i].description))
        } catch (e: dynamic) {
            js("Common").toast("打开链接失败\n" + e)
        }
        js("undefined")
    }, true, callback)
}

private fun upGetBetaVersionInfo(): dynamic {
    val snapshotVer = updaterApi.getSnapshotVersion()
    if (upTruthy(updaterApi.checkingBeta)) return "正在检查版本……"
    if (!upTruthy(updaterApi.latestBeta)) return "版本：" + snapshotVer
    if (updaterApi.updateFlagBeta > 0) {
        return "更新：" + snapshotVer + " -> " + updaterApi.latest
    } else if (updaterApi.updateFlagBeta == 0) {
        return "您使用的是最新版本"
    } else {
        return "Beta版本：" + snapshotVer
    }
}

private fun upGetSnapshotVersion(): dynamic {
    if (upTruthy(updaterApi.snapshotVer)) return updaterApi.snapshotVer
    try {
        updaterApi.snapshotVer = js("JSON").parse(
            js("Common").readFile(js("MapScript").baseDir + "snapshot.json", null, true)
        ).version
    } catch (e: dynamic) {
        js("Log").e(e)
    }
    return updaterApi.snapshotVer
}

private fun upCheckUpdateBeta(statusListener: dynamic, silently: dynamic): dynamic {
    if (!upTruthy(updaterApi.betaSources) || !upTruthy(updaterApi.betaSources.content) ||
        !(upLength(updaterApi.betaSources.content) > 0)
    ) {
        if (upTruthy(statusListener)) statusListener("completed", 0)
        if (!upTruthy(silently)) js("Common").toast("未配置更新源")
        return false
    }
    if (upTruthy(updaterApi.checkingBeta)) {
        js("Common").toast("正在检查更新中，请稍候")
        return false
    }
    updaterApi.checkingBeta = true
    if (upTruthy(statusListener)) statusListener("checking")
    js("Threads").run({
        try {
            val snapshotVer = updaterApi.getSnapshotVersion()
            updaterApi.getUpdateInfo(updaterApi.betaSources, { err: dynamic, info: dynamic ->
                updaterApi.checkingBeta = false
                if (upTruthy(err)) {
                    if (upTruthy(statusListener)) statusListener("errorGetInfo", err)
                    js("Common").toast("检测更新失败，请检查网络连接\n(" + err + ")")
                } else {
                    val flag = upDateParse(info.version) - upDateParse(snapshotVer)
                    updaterApi.latestBeta = info.version
                    updaterApi.updateFlagBeta = flag
                    if (flag > 0) {
                        if (upTruthy(statusListener)) statusListener("showingDialog")
                        updaterApi.showBetaUpdateDialog(info, {
                            if (upTruthy(statusListener)) statusListener("dialogClosed")
                            js("undefined")
                        })
                    } else if (!upTruthy(silently)) {
                        if (flag == 0) {
                            js("Common").toast("当前已经是最新Beta版本：" + snapshotVer)
                        } else {
                            js("Common").toast(
                                "目前没有更新的Beta版本，您可以在设置中关闭Beta计划以查看是否有更新的正式版"
                            )
                        }
                        if (upTruthy(statusListener)) statusListener("completed", flag)
                    }
                }
                js("undefined")
            })
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
    return js("undefined")
}

private fun upShowBetaUpdateDialog(info: dynamic, callback: dynamic) {
    var selected = false
    val buttons = js("[]")

    val hotfix = js("({})")
    hotfix.text = "快速更新"
    hotfix.id = "hotfix"
    hotfix.onclick = { cb: dynamic ->
        js("Common").showProgressDialog({ dia: dynamic ->
            dia.setText("下载中……")
            var error: dynamic = null
            try {
                updaterApi.downloadBeta(info)
                js("Common").toast("更新成功，将在下次启动快照时生效")
            } catch (e: dynamic) {
                js("Log").e(e)
                error = e
                js("Common").toast("下载更新失败\n" + e)
            }
            if (upTruthy(error)) updaterApi.cleanBetaFiles()
            if (upTruthy(cb)) cb(error)
            js("undefined")
        })
        js("undefined")
    }
    hotfix.visible = { js("MapScript").host == "Android" }
    buttons.push(hotfix)

    val manual = js("({})")
    manual.text = "加入内测群获得"
    manual.id = "manaul"
    manual.onclick = { cb: dynamic ->
        js("Common").setClipboardText("671317302")
        js("Common").toast("群号已复制到剪贴板")
        cb()
        js("undefined")
    }
    manual.visible = { js("MapScript").host != "Android" }
    buttons.push(manual)

    val later = js("({})")
    later.text = "稍后提醒"
    later.id = "remindLater"
    buttons.push(later)

    val visible = buttons.filter({ e: dynamic ->
        var keep = true
        if (upTruthy(e.visible) && !upTruthy(e.visible())) keep = false
        keep
    })

    val raw = js("({})")
    val extra = js("[]")
    val latest = js("({})")
    latest.text = "最新版本：" + info.version
    latest.bold = true
    extra.push(latest)
    extra.push("\n发布时间：")
    extra.push(upString(updaterApi.toChineseDate(info.time)))
    extra.push("\n更新内容：\n")
    extra.push(info.info)
    raw.extra = extra
    raw.color = "textcolor"

    val dialog = js("({})")
    dialog.title = "命令助手Beta版更新啦！"
    dialog.description = js("ISegment").rawJson(raw)
    dialog.buttons = visible.map({ e: dynamic -> e.text })
    dialog.callback = { i: dynamic ->
        selected = true
        val b = visible[i]
        if (upTruthy(b.onclick)) {
            b.onclick({
                if (upTruthy(callback)) callback(b.id)
                js("undefined")
            })
        } else if (upTruthy(callback)) {
            callback(b.id)
        }
        js("undefined")
    }
    dialog.onDismiss = {
        if (!selected && upTruthy(callback)) callback("remindLater")
        js("undefined")
    }
    js("Common").showConfirmDialog(dialog)
}

private fun upCleanBetaCache() {
    updaterApi.latestBeta = null
    updaterApi.updateFlagBeta = js("NaN")
    js("AndroidBridge").notifySettings()
}

private fun upDownloadBeta(info: dynamic) {
    if (!upTruthy(
            js("NetworkUtils").downloadGz(
                info.snapshot.url,
                js("MapScript").baseDir + "snapshot.js",
                info.snapshot.sha1
            )
        )
    ) {
        updaterApi.cleanBetaFiles()
        js("throw '文件校验失败'")
    }
    js("ctx").getSharedPreferences("user_settings", js("ctx").MODE_PRIVATE).edit()
        .putString("debugSource", js("MapScript").baseDir + "snapshot.js").apply()
    js("Common").saveFile(js("MapScript").baseDir + "snapshot.json", js("JSON").stringify(info), true)
}

private fun upInstallBeta(callback: dynamic) {
    js("Common").showProgressDialog({ dia: dynamic ->
        dia.setText("下载中……")
        var error: dynamic = null
        try {
            updaterApi.downloadBeta(
                js("JSON").parse(updaterApi.queryFromSources(updaterApi.betaSources.content))
            )
        } catch (e: dynamic) {
            js("Log").e(e)
            error = e
        }
        if (upTruthy(error)) updaterApi.cleanBetaFiles()
        if (upTruthy(callback)) callback(error)
        js("undefined")
    })
}

private fun upCleanBetaFiles() {
    js("Common").deleteFile(js("MapScript").baseDir + "snapshot.json")
    js("Common").deleteFile(js("MapScript").baseDir + "snapshot.js")
}

private fun upShowNewVersionInfo(oldVer: dynamic) {
    val raw = js("[]")
    val head = js("({})")
    head.text = "命令助手已更新！\n" + oldVer + " -> " + js("BuildConfig").date
    head.bold = true
    raw.push(head)
    raw.push("\t(" + js("BuildConfig").version + ")")
    raw.push("\n发布时间：" + upToChineseDate(js("BuildConfig").publishTime))
    raw.push("\n\n更新内容：\n")
    raw.push(js("BuildConfig").description)
    js("Common").showTextDialog(js("ISegment").rawJson(raw))
}

private fun upVariantLabel(): dynamic {
    val variants = js("BuildConfig").variants
    return if (variants == "release") {
        "正式版本"
    } else if (variants == "snapshot") {
        "快照版本"
    } else if (variants == "debug") {
        "调试版本"
    } else {
        variants
    }
}

private fun upShowCurrentVersionInfo() {
    val raw = js("[]")
    val block = js("({})")
    val extra = js("[]")
    val title = js("({})")
    title.text = "命令助手 " + js("CA").getVersionLabel()
    title.bold = true
    extra.push(title)
    extra.push("\n")
    val variant: dynamic = { upVariantLabel() }
    extra.push(variant)
    extra.push(" ")
    extra.push(js("BuildConfig").date)
    extra.push("\n发布于 " + upToChineseDate(js("BuildConfig").publishTime))
    block.extra = extra
    block.align = "center"
    raw.push(block)
    raw.push("\n\n更新内容：\n")
    raw.push(js("BuildConfig").description)
    js("Common").showTextDialog(js("ISegment").rawJson(raw))
}

private fun upAskHurryDevelop(callback: dynamic) {
    val dialog = js("({})")
    dialog.title = "催更命令助手"
    dialog.description = "是不是觉得命令助手更新速度太慢？不如催促命令助手更新吧"
    val buttons = js("[]")
    buttons.push("立即催更")
    buttons.push("暂不催更")
    dialog.buttons = buttons
    dialog.callback = { id: dynamic ->
        callback(id == 0)
        js("undefined")
    }
    js("Common").showConfirmDialog(dialog)
}

private fun upIsConnected(): Boolean {
    val cm = js("ctx").getSystemService(js("ctx").CONNECTIVITY_SERVICE)
    val an = cm.getActiveNetworkInfo()
    return upTruthy(an) && upTruthy(an.isConnected())
}

private fun upInitialize() {
    if (upIsConnected()) {
        if (js("BuildConfig").variants == "release") {
            if (!upTruthy(js("CA").settings.skipCheckUpdate) &&
                !(js("CA").settings.nextCheckUpdate > upNow())
            ) {
                updaterApi.checkUpdate({ statusMsg: dynamic ->
                    if (statusMsg == "completed") {
                        js("CA").settings.nextCheckUpdate = upNow() + 7 * 24 * 3600 * 1000
                    }
                    js("undefined")
                }, true)
            }
        } else {
            if (!upTruthy(js("CA").settings.skipCheckUpdate)) {
                updaterApi.checkUpdateBeta(null, true)
            }
        }
    }
}

fun installUpdater() {
    updaterApi.toChineseDate = { d: dynamic -> upToChineseDate(d) }
    updaterApi.toAnchor = { title: dynamic, url: dynamic ->
        "<a href=\"" + url + "\">" + title + "</a>"
    }
    updaterApi.queryFromSources = { sources: dynamic -> upQueryFromSources(sources) }
    updaterApi.getUpdateInfo = { source: dynamic, callback: dynamic ->
        upGetUpdateInfo(source, callback)
        js("undefined")
    }
    updaterApi.cleanCache = {
        upCleanCache()
        js("undefined")
    }
    updaterApi.getVersionInfo = { upGetVersionInfo() }
    updaterApi.checkUpdate = { statusListener: dynamic, silently: dynamic ->
        upCheckUpdate(statusListener, silently)
    }
    updaterApi.testSupport = { requirements: dynamic -> upTestSupport(requirements) }
    updaterApi.showUpdateDialog = { info: dynamic, callback: dynamic ->
        upShowUpdateDialog(info, callback)
        js("undefined")
    }
    updaterApi.chooseUpdateSource = { info: dynamic, callback: dynamic ->
        upChooseUpdateSource(info, callback)
        js("undefined")
    }
    updaterApi.getBetaVersionInfo = { upGetBetaVersionInfo() }
    updaterApi.getSnapshotVersion = { upGetSnapshotVersion() }
    updaterApi.checkUpdateBeta = { statusListener: dynamic, silently: dynamic ->
        upCheckUpdateBeta(statusListener, silently)
    }
    updaterApi.showBetaUpdateDialog = { info: dynamic, callback: dynamic ->
        upShowBetaUpdateDialog(info, callback)
        js("undefined")
    }
    updaterApi.cleanBetaCache = {
        upCleanBetaCache()
        js("undefined")
    }
    updaterApi.downloadBeta = { info: dynamic ->
        upDownloadBeta(info)
        js("undefined")
    }
    updaterApi.installBeta = { callback: dynamic ->
        upInstallBeta(callback)
        js("undefined")
    }
    updaterApi.cleanBetaFiles = {
        upCleanBetaFiles()
        js("undefined")
    }
    updaterApi.showNewVersionInfo = { oldVer: dynamic ->
        upShowNewVersionInfo(oldVer)
        js("undefined")
    }
    updaterApi.showCurrentVersionInfo = {
        upShowCurrentVersionInfo()
        js("undefined")
    }
    updaterApi.askHurryDevelop = { callback: dynamic ->
        upAskHurryDevelop(callback)
        js("undefined")
    }
    updaterApi.isConnected = { upIsConnected() }
    updaterApi.initialize = {
        upInitialize()
        js("undefined")
    }

    updaterApi.cacheUpdateData = js("({})")
    updaterApi.sources = js("UpdateSources").release
    updaterApi.betaSources = js("UpdateSources").beta

    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("Updater", updaterApi)
    }
}
