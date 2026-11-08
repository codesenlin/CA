package ca.kotlin

/**
 * modules/AndroidBridge.js 的 Kotlin/JS 版本。
 *
 * 保持原有对象 API、Java/Android 反射调用、权限请求、通知动作与快捷设置行为：
 *   - onCreate 在 MapScript.loadModule 时立刻执行，initialize 仍是全局 initialize hook；
 *   - 大量 Java 回调（intent / 权限 / 前台任务 / 媒体扫描 / TileService）保持原签名；
 *   - 原实现里带 `return` 的匿名函数改成普通 Kotlin 函数，避免 dynamic lambda 的 return 限制；
 *   - Java/JS 数组 length、in、delete、truthy、loose equality 都通过薄 JS helper 保留；
 *   - 需要 new 的 Java/Android 对象或重载集中的调用统一放到 JS helper，只传参数。
 */
private val abApi: dynamic = js("({})")
private val abShowSettingsState: dynamic = js("({})")

// ---------------- JS 语义 helper ----------------
private val abTruthyJs: dynamic = js("function (v) { return !!v; }")
private val abStringJs: dynamic = js("function (v) { return String(v); }")
private val abBooleanJs: dynamic = js("function (v) { return Boolean(v); }")
private val abLengthJs: dynamic = js("function (v) { return v.length | 0; }")
private val abToIntJs: dynamic = js("function (v) { return Number(v) | 0; }")
private val abForInJs: dynamic = js("function (o, f) { for (var i in o) f(i); }")
private val abInJs: dynamic = js("function (o, k) { return k in o; }")
private val abDeleteJs: dynamic = js("function (o, k) { delete o[k]; }")
private val abIsArrayJs: dynamic = js("function (v) { return Array.isArray(v); }")
private val abIsFunctionJs: dynamic = js("function (v) { return typeof v === 'function'; }")
private val abIsStringJs: dynamic = js("function (v) { return typeof v === 'string'; }")
private val abIsNaNNs: dynamic = js("function (v) { return isNaN(v); }")
private val abLooseEqJs: dynamic = js("function (a, b) { return a == b; }")
private val abIsFileJs: dynamic = js("function (v) { return v instanceof java.io.File; }")
private val abIsUriJs: dynamic = js("function (v) { return v instanceof android.net.Uri; }")
private val abOrJs: dynamic = js("function (a, b) { return a | b; }")
private val abOrValueJs: dynamic = js("function (a, b) { return a || b; }")
private val abMulJs: dynamic = js("function (a, b) { return a * b; }")
private val abThis1Js: dynamic = js("function (f) { return function (a) { return f(this, a); }; }")
private val abThis2Js: dynamic = js("function (f) { return function (a, b) { return f(this, a, b); }; }")
private val abUnescapeJs: dynamic = js("function (v) { return unescape(v); }")
private val abJsonParseJs: dynamic = js("function (v) { return JSON.parse(v); }")
private val abStripLeadingSlashJs: dynamic = js("function (s) { return s.replace(/^\\//, ''); }")
private val abQueryPartsJs: dynamic = js("function (q) { var t; return q.slice(t + 1).split('&'); }")
private val abDpJs: dynamic = js("function (n) { return n * G.dp; }")
private val abIconSizeJs: dynamic = js("function (size) { return 32 * G.dp * size; }")
private val abDpSizeJs: dynamic = js("function (size) { return G.dp * size; }")
private val abDiv256Js: dynamic = js("function (v) { return v / 256; }")

private fun abTruthy(v: dynamic): Boolean = abTruthyJs(v)
private fun abString(v: dynamic): dynamic = abStringJs(v)
private fun abBoolean(v: dynamic): dynamic = abBooleanJs(v)
private fun abLength(v: dynamic): Int = abLengthJs(v)
private fun abToInt(v: dynamic): Int = abToIntJs(v)
private fun abIn(o: dynamic, k: dynamic): Boolean = abInJs(o, k)
private fun abDelete(o: dynamic, k: dynamic) {
    abDeleteJs(o, k)
}
private fun abIsArray(v: dynamic): Boolean = abIsArrayJs(v)
private fun abIsFunction(v: dynamic): Boolean = abIsFunctionJs(v)
private fun abIsString(v: dynamic): Boolean = abIsStringJs(v)
private fun abIsNaN(v: dynamic): Boolean = abIsNaNNs(v)
private fun abLooseEq(a: dynamic, b: dynamic): Boolean = abLooseEqJs(a, b)
private fun abIsFile(v: dynamic): Boolean = abIsFileJs(v)
private fun abIsUri(v: dynamic): Boolean = abIsUriJs(v)
private fun abOr(a: dynamic, b: dynamic): dynamic = abOrJs(a, b)
private fun abOrValue(a: dynamic, b: dynamic): dynamic = abOrValueJs(a, b)
private fun abMul(a: dynamic, b: dynamic): dynamic = abMulJs(a, b)
private fun abDp(n: Int): dynamic = abDpJs(n)

private fun abEmptyArray(): dynamic = js("[]")
private fun abEmptyObject(): dynamic = js("({})")
private fun abSingleArray(v: dynamic): dynamic {
    val a = abEmptyArray()
    a.push(v)
    return a
}

// ---------------- Java / Android helper ----------------
private val abNewIntent1Js: dynamic = js("function (a) { return new android.content.Intent(a); }")
private val abNewIntent2Js: dynamic = js("function (a, u) { return new android.content.Intent(a, u); }")
private val abUriParseJs: dynamic = js("function (s) { return android.net.Uri.parse(s); }")
private val abUriFromFileJs: dynamic = js("function (f) { return android.net.Uri.fromFile(f); }")
private val abNewFileJs: dynamic = js("function (p) { return new java.io.File(p); }")
private val abNewJavaStringJs: dynamic = js("function (s) { return new java.lang.String(s); }")
private val abNewZipFileJs: dynamic = js("function (p) { return new java.util.zip.ZipFile(p); }")
private val abNewSecurityExceptionJs: dynamic = js("function (s) { return new java.lang.SecurityException(s); }")
private val abThrowSecurityExceptionJs: dynamic = js("function (s) { throw new java.lang.SecurityException(s); }")
private val abThrowNumberJs: dynamic = js("function (n) { throw n; }")
private val abMessageDigestJs: dynamic = js("function (n) { return java.security.MessageDigest.getInstance(n); }")
private val abBase64Js: dynamic =
    js("function (b) { return android.util.Base64.encodeToString(b, android.util.Base64.NO_WRAP); }")
private val abPackageInfoJs: dynamic = js("function (pkg, flags) { return ctx.getPackageManager().getPackageInfo(pkg, flags); }")
private val abApplicationInfoJs: dynamic = js("function (pkg, flags) { return ctx.getPackageManager().getApplicationInfo(pkg, flags); }")
private val abLaunchIntentJs: dynamic = js("function (pkg) { return ctx.getPackageManager().getLaunchIntentForPackage(pkg); }")
private val abApplicationLabelJs: dynamic = js("function (info) { return ctx.getPackageManager().getApplicationLabel(info); }")
private val abApplicationIconJs: dynamic = js("function (pkg) { return ctx.getPackageManager().getApplicationIcon(pkg); }")
private val abResourcesForApplicationJs: dynamic =
    js("function (appi) { return ctx.getPackageManager().getResourcesForApplication(appi); }")
private val abInstalledPackagesJs: dynamic = js("function (flags) { return ctx.getPackageManager().getInstalledPackages(flags); }")
private val abQueryIntentActivitiesJs: dynamic =
    js("function (intent) { return ctx.getPackageManager().queryIntentActivities(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY); }")
private val abRevokeUriPermissionJs: dynamic = js("function (uri, flags) { ctx.revokeUriPermission(uri, flags); }")
private val abGetContentResolverJs: dynamic = js("function () { return ctx.getContentResolver(); }")
private val abResolveQueryJs: dynamic = js(
    "function (uri, projection, selection, args, sort) { return ctx.getContentResolver().query(uri, projection, selection, args, sort); }"
)
private val abIsDocumentUriJs: dynamic =
    js("function (uri) { return android.provider.DocumentsContract.isDocumentUri(ctx, uri); }")
private val abDocumentIdJs: dynamic = js("function (uri) { return android.provider.DocumentsContract.getDocumentId(uri); }")
private val abWithAppendedIdJs: dynamic =
    js("function (uri, id) { return android.content.ContentUris.withAppendedId(uri, id); }")
private val abExternalStorageDirJs: dynamic = js("function () { return android.os.Environment.getExternalStorageDirectory(); }")
private val abFileToUriJs: dynamic = js("function (f) { return ScriptInterface.fileToUri(f); }")
private val abFileUriFromFileJs: dynamic = js("function (f) { return android.net.Uri.fromFile(f); }")
private val abShortcutIconResourceJs: dynamic =
    js("function (context, icon) { return android.content.Intent.ShortcutIconResource.fromContext(context, icon); }")
private val abNewShortcutBuilderJs: dynamic =
    js("function (context, name) { return new android.content.pm.ShortcutInfo.Builder(context, name); }")
private val abIconWithResourceJs: dynamic =
    js("function (context, icon) { return android.graphics.drawable.Icon.createWithResource(context, icon); }")
private val abNewMediaScannerJs: dynamic =
    js("function (context, listener) { return new android.media.MediaScannerConnection(context, listener); }")
private val abSecureGetStringJs: dynamic =
    js("function (resolver, name) { return android.provider.Settings.Secure.getString(resolver, name); }")
private val abRuntimeExecJs: dynamic = js("function (cmd) { return java.lang.Runtime.getRuntime().exec(cmd); }")
private val abNewHandlerJs: dynamic = js("function () { return new android.os.Handler(); }")
private val abToastMakeTextJs: dynamic =
    js("function (context, text, duration) { return android.widget.Toast.makeText(context, text, duration); }")
private val abNewAlertBuilderJs: dynamic = js("function (a) { return new android.app.AlertDialog.Builder(a); }")
private val abMessageObtainJs: dynamic = js("function () { return android.os.Message.obtain(); }")
private val abNewLinearLayoutJs: dynamic = js("function (c) { return new G.LinearLayout(c); }")
private val abNewTextViewJs: dynamic = js("function (c) { return new G.TextView(c); }")
private val abNewButtonJs: dynamic = js("function (c) { return new G.Button(c); }")
private val abNewFrameLayoutJs: dynamic = js("function (c) { return new G.FrameLayout(c); }")
private val abNewImageViewJs: dynamic = js("function (c) { return new G.ImageView(c); }")
private val abNewAbsListViewParamsJs: dynamic =
    js("function (w, h) { return new G.AbsListView.LayoutParams(w, h); }")
private val abNewLinearLayoutParams2Js: dynamic =
    js("function (w, h) { return new G.LinearLayout.LayoutParams(w, h); }")
private val abNewViewGroupParamsJs: dynamic = js("function (w, h) { return new G.ViewGroup.LayoutParams(w, h); }")
private val abNewFrameLayoutParamsJs: dynamic =
    js("function (w, h) { return new G.FrameLayout.LayoutParams(w, h); }")
private val abNewViewClickListenerJs: dynamic = js("function (l) { return new G.View.OnClickListener(l); }")
private val abNewDialogClickListenerJs: dynamic =
    js("function (l) { return new android.content.DialogInterface.OnClickListener(l); }")
private val abNewCanvasJs: dynamic = js("function (b) { return new G.Canvas(b); }")
private val abNewPaintJs: dynamic = js("function () { return new G.Paint(); }")
private val abNewPathJs: dynamic = js("function () { return new G.Path(); }")
private val abCreateBitmapJs: dynamic =
    js("function (w, h) { return G.Bitmap.createBitmap(w, h, G.Bitmap.Config.ARGB_8888); }")
private val abNewForegroundIntentJs: dynamic =
    js("function () { return new android.content.Intent('com.xero.ca.script.ForegroundScript'); }")

private fun abThrowSecurityException(message: dynamic) {
    abThrowSecurityExceptionJs(message)
}

private fun abThrowNumber(n: Int) {
    abThrowNumberJs(n)
}

private fun abJsonParse(v: dynamic): dynamic = abJsonParseJs(v)
private fun abUnescape(v: dynamic): dynamic = abUnescapeJs(v)

// ---------------- 生命周期 ----------------
private fun abOnCreate() {
    js("G").ui(abApi.initIcon)
    if (js("MapScript").host != "Android") return
    //作者原版是常开的（if(true)…）。这里保持「仅正式版校验」：
    //verifySign 的证书哈希已换成我们自己的，verifyDex 的 $dexCrc$ 由 shellWriteScript
    //按正式包 classes.dex 的 CRC32 注入，所以发布流程产出的包能直接通过；
    //要打开就取消下面这行的注释。注意：调试包用的是调试构建的 dex，CRC 对不上。
    // if (CA.RELEASE) gHandler.post(this.verifyApk);
}

private fun abInitialize() {
    try {
        if (js("MapScript").host != "Android") return
        val bridge = abEmptyObject()
        bridge.applyIntent = { intent: dynamic -> abBridgeApplyIntent(intent) }
        bridge.onAccessibilitySvcCreate = {
            abBridgeNotifySettings()
            js("undefined")
        }
        bridge.onAccessibilitySvcDestroy = {
            abBridgeNotifySettings()
            js("undefined")
        }
        bridge.onActivityResult = { requestCode: dynamic, resultCode: dynamic, data: dynamic ->
            abBridgeOnActivityResult(requestCode, resultCode, data)
        }
        bridge.onBeginPermissionRequest = { activity: dynamic -> abBridgeOnBeginPermissionRequest(activity) }
        bridge.onBeginForegroundTask = { activity: dynamic, intent: dynamic ->
            abBridgeOnBeginForegroundTask(activity, intent)
        }
        bridge.onKeyEvent = { e: dynamic -> abBridgeOnKeyEvent(e) }
        bridge.onNewIntent = { intent: dynamic ->
            abBridgeOnNewIntent(intent)
        }
        bridge.onRemoteEnabled = {
            try {
                js("Common").toast("正在连接至Minecraft适配器……/\n等待游戏数据传输……")
            } catch (e: dynamic) {
                js("erp")(e)
            }
            js("undefined")
        }
        bridge.onRemoteMessage = { msg: dynamic -> abBridgeOnRemoteMessage(msg) }
        bridge.onRemoteDisabled = {
            abBridgeOnRemoteDisabled()
            js("undefined")
        }
        bridge.onTileReady = { config: dynamic -> abBridgeOnTileReady(config) }
        bridge.onTileClick = { config: dynamic -> abBridgeOnTileClick(config) }
        js("ScriptInterface").setBridge(bridge)

        abApi.onNewIntent(js("ScriptInterface").getIntent(), true)
        if (abTruthy(js("CA").settings.autoStartAccSvcRoot)) {
            abApi.startAccessibilitySvcByRootAsync(null, true)
        }
        if (abTruthy(js("CA").settings.startWSSOnStart)) js("WSServer").start(true)
        if (abTruthy(js("G").shouldFloat)) abApi.showActivityContent(js("G").supportFloat)
        if (!abTruthy(js("CA").settings.permissionChecked)) {
            abApi.checkNecessaryPermissionsSync()
            js("CA").settings.permissionChecked = true
        }
        if (abTruthy(js("G").supportFloat)) abApi.exitLoading(!abTruthy(js("CA").settings.hideRecent))
    } catch (e: dynamic) {
        js("erp")(e)
    }
}

private fun abBridgeApplyIntent(intent: dynamic): dynamic {
    try {
        abApi.callHide()
        return true
    } catch (e: dynamic) {
        js("erp")(e)
    }
    return js("undefined")
}

private fun abBridgeNotifySettings() {
    try {
        abApi.notifySettings()
    } catch (e: dynamic) {
        js("erp")(e)
    }
}

private fun abBridgeOnActivityResult(requestCode: dynamic, resultCode: dynamic, data: dynamic) {
    try {
        val cb = abApi.intentCallback[requestCode]
        if (!abTruthy(cb)) return
        js("PopupPage").show()
        abDelete(abApi.intentCallback, requestCode)
        cb(resultCode, data)
    } catch (e: dynamic) {
        js("erp")(e)
    }
}

private fun abBridgeOnBeginPermissionRequest(activity: dynamic): dynamic {
    try {
        return abApi.onBeginPermissionRequest(activity)
    } catch (e: dynamic) {
        js("erp")(e)
    }
    return js("undefined")
}

private fun abBridgeOnBeginForegroundTask(activity: dynamic, intent: dynamic): dynamic {
    try {
        return abApi.onBeginForegroundTask(activity, intent)
    } catch (e: dynamic) {
        js("erp")(e)
    }
    return js("undefined")
}

private fun abBridgeOnKeyEvent(e: dynamic) {
    try {
        if (abLooseEq(e.getAction(), e.ACTION_DOWN)) {
            val k = e.getKeyCode()
            if (abLooseEq(k, e.KEYCODE_HOME) || abLooseEq(k, e.KEYCODE_MENU) ||
                abLooseEq(k, e.KEYCODE_ENDCALL) || abLooseEq(k, e.KEYCODE_POWER) ||
                abLooseEq(k, e.KEYCODE_NOTIFICATION)
            ) {
                abApi.callHide()
            }
        }
    } catch (e2: dynamic) {
        js("erp")(e2)
    }
}

private fun abBridgeOnNewIntent(intent: dynamic) {
    try {
        abApi.onNewIntent(intent, false)
    } catch (e: dynamic) {
        js("erp")(e)
    }
}

private fun abBridgeOnRemoteMessage(msg: dynamic) {
    try {
        if (!abLooseEq(msg.what, 1)) return
        val data = msg.getData()
        if (!abLooseEq(data.getString("action"), "init") && !abTruthy(js("MCAdapter").client)) {
            // 防止其他App误发消息被识别为适配器消息
            if (abTruthy(msg.reply)) {
                val msg2 = abMessageObtainJs()
                msg2.what = 2
                msg.replyTo.send(msg2)
            }
            return
        }
        val action = abString(data.getString("action"))
        if (action == "init") {
            js("MCAdapter").client = msg.replyTo
            js("MCAdapter").connInit = true
            js("MCAdapter").version = data.getInt("version", 0)
            abApi.notifySettings()
            js("Common").toast(
                "已连接至Minecraft适配器，终端：" + data.getString("platform") + "\n" +
                    (if (js("MCAdapter").targetVersion > js("MCAdapter").version) {
                        "此适配器版本较旧，可能不支持部分提示，请在设置中重新加载适配器"
                    } else {
                        "当前适配器为最新版本"
                    })
            )
        } else if (action == "info") {
            js("MCAdapter").updateInfo(data.getBundle("info"))
        } else if (action == "event") {
            try {
                js("MCAdapter").callHook(data.getString("name"), abJsonParse(data.getString("param")))
            } catch (e: dynamic) {
                js("erp")(e, true)
            }
        } else if (action == "resetMCV") {
            js("NeteaseAdapter").mcVersion = abString(data.getString("version"))
            js("Common").toast("正在切换拓展包版本，请稍候……")
            js("CA").checkFeatures()
            js("CA").Library.initLibrary({ flag: dynamic ->
                if (abTruthy(flag)) {
                    js("Common").toast("拓展包加载完毕")
                } else {
                    js("Common").toast("有拓展包未能加载，可在「管理拓展包」里查看原因")
                }
                js("undefined")
            })
        }
    } catch (e: dynamic) {
        js("erp")(e)
    }
}

private fun abBridgeOnRemoteDisabled() {
    try {
        js("Common").toast("已断开至Minecraft适配器的连接")
        js("MCAdapter").bundle = null
        js("MCAdapter").client = null
        js("MCAdapter").connInit = false
        abApi.notifySettings()
    } catch (e: dynamic) {
        js("erp")(e)
    }
}

private fun abBridgeOnTileReady(config: dynamic) {
    try {
        val tile = abApi.getTileService()
        if (!abTruthy(tile.initialized)) {
            if (abTruthy(tile.service.initTile)) tile.service.initTile(tile.data, config, tile.context)
            tile.initialized = true
        }
        if (abTruthy(tile.service.updateTile)) tile.service.updateTile(tile.data, config, tile.context)
    } catch (e: dynamic) {
        js("erp")(e)
    }
}

private fun abBridgeOnTileClick(config: dynamic) {
    try {
        val tile = abApi.getTileService()
        if (abTruthy(tile.service.onTileClick)) tile.service.onTileClick(tile.data, config, tile.context)
    } catch (e: dynamic) {
        js("erp")(e)
    }
}

// ---------------- Intent 处理 ----------------
private fun abOnNewIntent(intent: dynamic, startByIntent: dynamic) {
    fun onReturn() {
        if (!abTruthy(js("CA").trySave())) return
        if (abTruthy(startByIntent)) {
            //CA.performExit();
        }
    }
    if (!abTruthy(intent)) return
    val action = intent.getAction()
    if (abLooseEq(action, js("ScriptInterface").ACTION_ADD_LIBRARY)) {
        val t = intent.getData()
        if (!abTruthy(t)) {
            js("Common").toast("无法读取拓展包")
        } else {
            val dialog = abEmptyObject()
            dialog.title = "确定加载拓展包“" + js("ExternalStorage").uriToName(t) + "”？"
            dialog.callback = { id: dynamic ->
                abAddLibraryConfirm(t, { onReturn(); js("undefined") }, id)
                js("undefined")
            }
            dialog.onDismiss = {
                // Intent.FLAG_GRANT_READ_URI_PERMISSION | FLAG_GRANT_WRITE_URI_PERMISSION
                abRevokeUriPermissionJs(t, 0x3)
                js("undefined")
            }
            js("Common").showConfirmDialog(dialog)
        }
    } else if (abLooseEq(action, js("ScriptInterface").ACTION_EDIT_COMMAND)) {
        var t = intent.getExtras()
        t = if (abTruthy(t)) t.getString("text", "") else ""
        js("G").ui({
            try {
                js("CA").showGen(true)
                js("CA").cmd.setText(t)
                js("CA").showGen.activate(false)
            } catch (e: dynamic) {
                js("erp")(e)
            }
            js("undefined")
        })
    } else if (abLooseEq(action, js("ScriptInterface").ACTION_START_FROM_SHORTCUT)) {
        if (abTruthy(intent.getData())) {
            val t = abLaunchIntentJs(intent.getData().getSchemeSpecificPart())
            if (abTruthy(t)) abApi.startActivity(t)
        }
    } else if (abLooseEq(action, js("ScriptInterface").ACTION_SCRIPT_ACTION)) {
        if (!abTruthy(startByIntent)) abApi.scriptAction()
    } else if (abLooseEq(action, js("ScriptInterface").ACTION_URI_ACTION)) {
        abApi.openUriAction(intent.getData(), intent.getExtras())
    } else {
        if (abTruthy(startByIntent) && abTruthy(js("CA").settings.chainLaunch)) {
            val t = abLaunchIntentJs(js("CA").settings.chainLaunch)
            if (abTruthy(t)) abApi.startActivity(t)
        }
        if (!abTruthy(startByIntent)) js("CA").showIcon()
    }
}

private fun abAddLibraryConfirm(t: dynamic, onReturn: dynamic, id: dynamic) {
    if (id != 0) {
        onReturn()
        return
    }
    val importFile = js("ExternalStorage").importFile(t, "caclib/*/**")
    try {
        val status = js("CA").Library.addLibrary(abString(js("ExternalStorage").toUri(importFile)))
        js("CA").Library.initLibrary({
            js("Common").toast(
                if (abLooseEq(status, "ignored")) {
                    "已存在版本更高或相同的拓展包，本次导入已忽略"
                } else {
                    "导入成功！"
                }
            )
            js("CA").showLibraryMan(onReturn)
            js("undefined")
        })
    } catch (e: dynamic) {
        js("Log").e(e)
        js("Common").toast("无法导入该拓展包\n" + e)
        js("CA").showLibraryMan(onReturn)
    }
}

// ---------------- 校验 ----------------
private fun abVerifyApk() {
    if (abLooseEq(js("ctx").getPackageName(), "com.xero.ca")) {
        // ok
    } else {
        abThrowSecurityException("101")
    }
    abApi.verifySign()
    abApi.verifyContext()
    if (abTruthy(abApi.HOTFIX)) return
    abApi.verifyDex()
}

private fun abVerifySign() {
    try {
        val sn = abPackageInfoJs(
            js("ctx").getPackageName(),
            js("android.content.pm.PackageManager").GET_SIGNATURES
        ).signatures
        val vc = abEmptyArray()
        val md = abMessageDigestJs("SHA-256")
        abForInJs(sn) { i: dynamic ->
            md.update(sn[i].toByteArray())
            vc.push(abBase64Js(md.digest()))
        }
        //这份哈希必须等于 app/signatures/{release,debug}.signature 那张证书的
        //SHA-256（Base64）。换签名密钥时同步更新，否则 verifyApk 会抛 SecurityException(102)。
        if (!abLooseEq(vc.join(""), "pA2oClnRcMqpUM8VwYxFTUejmyaYnYtkDs10W6cb9dw=")) abThrowNumber(102)
    } catch (e: dynamic) {
        abThrowSecurityException(abString(e))
    }
}

private fun abMethodArgs(vararg items: dynamic): dynamic {
    val a = abEmptyArray()
    var i = 0
    while (i < items.size) {
        a.push(items[i])
        i++
    }
    return a
}

private fun abVerifyContext() {
    try {
        val cls = js("ctx").getApplicationContext().getClass()
        if (!abLooseEq(cls, js("com.xero.ca.XApplication"))) abThrowNumber(104)
        if (abTruthy(
                abApi.findDeclaredMethodClass(
                    cls,
                    abMethodArgs("attachBaseContext", js("android.content.Context")),
                    js("android.app.Application")
                )
            )
        ) {
            abThrowNumber(105)
        }
        if (!abLooseEq(
                abApi.findDeclaredMethodClass(cls, abMethodArgs("onCreate"), js("android.app.Application")),
                js("com.xero.ca.XApplication")
            )
        ) {
            abThrowNumber(106)
        }
        /*cls = ctx.getClass();
        if (cls != com.xero.ca.MainActivity) throw 107;
        if (this.findDeclaredMethodClass(cls, ["attachBaseContext", android.content.Context], com.xero.ca.MainActivity)) throw 108;
        if (this.findDeclaredMethodClass(cls, ["onCreate", android.os.Bundle], com.xero.ca.MainActivity) != com.xero.ca.MainActivity) throw 109;*/
    } catch (e: dynamic) {
        abThrowSecurityException(abString(e))
    }
}

private fun abVerifyDex() {
    val zf = abNewZipFileJs(js("ctx").getPackageCodePath())
    val e = zf.getEntry("classes.dex")
    if (!abLooseEq(js("java.lang.Long").toHexString(e.getCrc()), "\$dexCrc\$")) {
        abThrowSecurityException("103")
    }
}

private fun abFindDeclaredMethodClass(cls: dynamic, params: dynamic, parent: dynamic): dynamic {
    try {
        cls.getDeclaredMethod.apply(cls, params)
        return cls
    } catch (e: dynamic) {
        //Class not found
    }
    var p = parent
    if (!abTruthy(p)) p = js("java.lang.Object")
    if (abLooseEq(cls, js("java.lang.Object")) || abLooseEq(cls, p)) return null
    return abApi.findDeclaredMethodClass(cls.getSuperclass(), params, p)
}

// ---------------- 通用动作 ----------------
private fun abCallHide() {
    js("G").ui({
        try {
            if (js("PopupPage").getCount() > 0) {
                js("PopupPage").hide()
            }
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

private fun abScriptAction() {
    js("CA").showActions(abApi.getKeeperMenu())
}

private fun abOpenUriAction(uri: dynamic, extras: dynamic) {
    if (!abTruthy(uri)) return
    val path = uri.getPath()
    val query = uri.getEncodedQuery()
    val fragment = uri.getFragment()
    if (abLooseEq(abString(uri.getHost()).toLowerCase(), "base")) {
        if (abTruthy(path)) {
            val obj = abApi.getBaseUriAction(abString(path))
            if (abTruthy(obj)) {
                obj(
                    if (abTruthy(fragment)) abString(fragment) else null,
                    if (abTruthy(query)) abApi.getQueryKV(abString(query)) else abEmptyObject(),
                    extras
                )
            } else {
                js("Common").toast("未知的调用：" + path)
            }
        }
    }
}

private fun abGetBaseUriAction(path: dynamic): dynamic {
    var obj = abApi.uriActions
    val parts = abStripLeadingSlashJs(abString(path).toLowerCase()).split("/")
    var i = 0
    while (i < abLength(parts)) {
        val par = obj
        obj = obj[parts[i]]
        if (!abTruthy(obj)) {
            obj = if (abTruthy(par.get)) {
                par.get(parts.slice(i))
            } else if (abIsFunction(par)) {
                par
            } else {
                par.default
            }
            break
        }
        i++
    }
    if (abIsFunction(obj)) return obj
    return null
}

private fun abGetQueryKV(query: dynamic): dynamic {
    val r = abEmptyObject()
    val strs = abQueryPartsJs(abString(query))
    abForInJs(strs) { i: dynamic ->
        val t = strs[i].indexOf("=")
        if (t >= 0) {
            r[strs[i].slice(0, t)] = abUnescape(strs[i].slice(t + 1))
        }
    }
    return r
}

private fun abNotifySettings() {
    js("G").ui({
        try {
            if (abTruthy(js("Common").showSettings.refreshText)) js("Common").showSettings.refreshText()
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

// ---------------- 设置页 ----------------
private fun abShowSettings(title: dynamic) {
    val state = abShowSettingsState
    if (!abTruthy(state.root)) {
        val preference = js("ScriptInterface").getPreference()
        val root = abEmptyArray()

        val accessibility = abEmptyObject()
        accessibility.name = "管理无障碍服务"
        accessibility.description = "用于支持粘贴命令以及一些其他操作"
        accessibility.type = "custom"
        accessibility.get = {
            if (abTruthy(js("ScriptInterface").getAccessibilitySvc())) "已启用" else "未启用"
        }
        accessibility.onclick = { _fset: dynamic ->
            try {
                js("ScriptInterface").goToAccessibilitySetting()
            } catch (e: dynamic) {
                js("Common").toast("无法打开无障碍设置\n" + e)
            }
            js("undefined")
        }
        root.push(accessibility)

        val adapter = abEmptyObject()
        adapter.name = "加载适配器……"
        adapter.description = "在输入命令时提供一些与游戏相关的信息"
        adapter.type = "custom"
        adapter.get = {
            if (abTruthy(js("MCAdapter").connInit)) "已连接" else "未连接"
        }
        adapter.onclick = { fset: dynamic ->
            fset(adapter.get())
            js("MCAdapter").listAdapters()
            js("undefined")
        }
        root.push(adapter)

        val chain = abEmptyObject()
        chain.name = "连锁启动……"
        chain.description = "设置启动命令助手时自动启动的应用"
        chain.type = "custom"
        chain.get = {
            val r = js("CA").settings.chainLaunch
            var ai: dynamic = null
            try {
                if (abTruthy(r)) ai = abApplicationInfoJs(r, 128)
            } catch (e: dynamic) {
                //App not found
            }
            if (!abTruthy(ai)) "无" else abApplicationLabelJs(ai)
        }
        chain.onclick = abThis1Js { self: dynamic, fset: dynamic ->
            abApi.listLaunchableApp({ pkg: dynamic ->
                abChainLaunchPicked(self, fset, pkg)
                js("undefined")
            }, true)
            js("undefined")
        }
        root.push(chain)

        val websocket = abEmptyObject()
        websocket.name = "WebSocket服务器"
        websocket.description = "实验性功能"
        websocket.type = "custom"
        websocket.get = {
            if (js("WSServer").isAvailable()) {
                if (js("WSServer").isConnected()) "已连接" else "已启动"
            } else {
                "未启动"
            }
        }
        websocket.onclick = { _fset: dynamic ->
            if (js("WSServer").isConnected()) {
                js("WSServer").showConsole()
            } else if (js("WSServer").isAvailable()) {
                js("WSServer").howToUse()
            } else {
                js("WSServer").start()
            }
            js("undefined")
        }
        root.push(websocket)

        val bootStart = abEmptyObject()
        bootStart.name = "开机自动启动"
        bootStart.description = "需要系统允许开机自启"
        bootStart.type = "boolean"
        bootStart.get = preference.getBootStart.bind(preference)
        bootStart.set = preference.setBootStart.bind(preference)
        root.push(bootStart)

        val hideSplash = abEmptyObject()
        hideSplash.name = "隐藏启动界面"
        hideSplash.type = "boolean"
        hideSplash.get = preference.getHideSplash.bind(preference)
        hideSplash.set = preference.setHideSplash.bind(preference)
        root.push(hideSplash)

        val hideRecent = abEmptyObject()
        hideRecent.name = "隐藏后台任务"
        hideRecent.type = "boolean"
        hideRecent.get = {
            if (abApi.shouldForceRemoveTask()) {
                true
            } else {
                abBoolean(js("CA").settings.hideRecent)
            }
        }
        hideRecent.set = { v: dynamic -> abHideRecentSet(v) }
        root.push(hideRecent)

        val autoStartAcc = abEmptyObject()
        autoStartAcc.name = "自动启动无障碍服务"
        autoStartAcc.description = "需要Root"
        autoStartAcc.type = "boolean"
        autoStartAcc.get = { abBoolean(js("CA").settings.autoStartAccSvcRoot) }
        autoStartAcc.set = { v: dynamic ->
            js("CA").settings.autoStartAccSvcRoot = abBoolean(v)
            if (abTruthy(v)) abApi.startAccessibilitySvcByRootAsync()
            js("undefined")
        }
        root.push(autoStartAcc)

        val neverAsk = abEmptyObject()
        neverAsk.name = "隐藏“启用适配器”的提示"
        neverAsk.type = "boolean"
        neverAsk.get = { abBoolean(js("CA").settings.neverAskAdapter) }
        neverAsk.set = { v: dynamic ->
            js("CA").settings.neverAskAdapter = abBoolean(v)
            js("undefined")
        }
        root.push(neverAsk)

        val startWSS = abEmptyObject()
        startWSS.name = "启动时自动启动WebSocket服务器"
        startWSS.type = "boolean"
        startWSS.get = { abBoolean(js("CA").settings.startWSSOnStart) }
        startWSS.set = { v: dynamic ->
            js("CA").settings.startWSSOnStart = abBoolean(v)
            js("undefined")
        }
        root.push(startWSS)

        val actionMenu = abEmptyObject()
        actionMenu.name = "通知动作菜单"
        actionMenu.type = "custom"
        actionMenu.get = { abLength(abApi.getKeeperMenu()).toString() + "个动作" }
        actionMenu.onclick = { fset: dynamic ->
            js("CA").showActionEdit(abApi.getKeeperMenu(), fset, abApi.defaultKeeperMenu)
            js("undefined")
        }
        root.push(actionMenu)

        val tile = abEmptyObject()
        tile.name = "快捷设置开关"
        tile.type = "custom"
        tile.hidden = { js("android.os.Build.VERSION.SDK_INT") < 24 }
        tile.get = {
            val tileService = abApi.getTileService()
            tileService.service.name
        }
        tile.onclick = { fset: dynamic ->
            abTileSettingsClick(fset)
            js("undefined")
        }
        root.push(tile)

        state.root = root
    }
    js("Common").showSettings(title, state.root)
}

private fun abChainLaunchPicked(self: dynamic, fset: dynamic, pkg: dynamic) {
    if (abLooseEq(pkg, js("ctx").getPackageName())) {
        js("Common").toast("不能连锁启动自身！")
        return
    }
    js("CA").settings.chainLaunch = pkg
    fset(self.get())
}

private fun abHideRecentSet(v: dynamic) {
    if (abApi.shouldForceRemoveTask()) {
        js("Common").toast("您的设备不支持显示命令助手的后台任务")
        return
    }
    js("CA").settings.hideRecent = abBoolean(v)
    js("Common").toast("本项设置将在重启命令助手后应用")
}

private fun abTileSettingsClick(fset: dynamic) {
    val tile = abApi.getTileService()
    abApi.showEditTile(tile.data, { tileData: dynamic ->
        js("CA").settings.qstile = tileData
        tile.invalid = true
        abApi.notifyTileUpdate()
        fset()
        js("undefined")
    })
}

private fun abGetAppIcon(): dynamic {
    val appi = abApplicationInfoJs("com.xero.ca", 128)
    val res = abResourcesForApplicationJs(appi)
    return if (js("android.os.Build.VERSION.SDK_INT") >= 21) {
        res.getDrawable(appi.icon, null)
    } else {
        res.getDrawable(appi.icon)
    }
}

private fun abGetAppIconBadged(): dynamic {
    return abApplicationIconJs("com.xero.ca")
}

private fun abInitIcon() {
    var logo: dynamic = null
    var icon: dynamic = null
    try {
        icon = abApi.getAppIcon()
    } catch (e: dynamic) {
        js("Log").e(e)
    }
    if (abTruthy(icon)) {
        js("CA").Icon.default0 = js("CA").Icon.default
        js("CA").Icon.default = { size: dynamic ->
            val w = abIconSizeJs(size)
            val bmp = abCreateBitmapJs(w, w)
            val cv = abNewCanvasJs(bmp)
            cv.scale(abDiv256Js(w), abDiv256Js(w))
            val pt = abNewPaintJs()
            pt.setAntiAlias(true)
            js("IntColor").Paint.setColor(pt, js("G.Color.BLACK"))
            js("IntColor").Paint.setShadowLayer(pt, 16, 0, 0, js("G.Color.BLACK"))
            val ph = abNewPathJs()
            ph.addCircle(128, 128, 112, js("G.Path.Direction.CW"))
            cv.drawPath(ph, pt)
            cv.clipPath(ph)
            icon.setBounds(16, 16, 240, 240)
            icon.draw(cv)
            val frm = abNewFrameLayoutJs(js("ctx"))
            val view = abNewImageViewJs(js("ctx"))
            view.setImageBitmap(bmp)
            view.setLayoutParams(abNewFrameLayoutParamsJs(w, w))
            frm.addView(view)
            frm
        }
        return
    }
    try {
        logo = abApi.getAppIconBadged()
    } catch (e: dynamic) {
        js("Log").e(e)
    }
    if (abTruthy(logo)) {
        js("CA").Icon.default0 = js("CA").Icon.default
        js("CA").Icon.default = { size: dynamic ->
            val zp = abDpSizeJs(size)
            val frm = abNewFrameLayoutJs(js("ctx"))
            val view = abNewImageViewJs(js("ctx"))
            view.setImageDrawable(logo)
            view.setLayoutParams(abNewFrameLayoutParamsJs(abMul(32, zp), abMul(32, zp)))
            frm.addView(view)
            frm
        }
    }
}

// ---------------- 应用列表 / 启动 ----------------
private fun abListLaunchableApp(callback: dynamic, includeCancel: dynamic) {
    js("Common").showProgressDialog({ o: dynamic ->
        abListLaunchableAppTask(o, callback, includeCancel)
        js("undefined")
    }, true)
}

private fun abListLaunchableAppTask(o: dynamic, callback: dynamic, includeCancel: dynamic) {
    val pm = js("ctx").getPackageManager()
    o.setTextDelayed("正在加载列表……", 200)
    val lp = abInstalledPackagesJs(0).toArray()
    val r = abEmptyArray()
    if (abTruthy(includeCancel)) {
        val cancel = abEmptyObject()
        cancel.text = "取消选择"
        cancel.result = null
        r.push(cancel)
    }
    var i = 0
    val n = abLength(lp)
    while (i < n) {
        if (!abTruthy(lp[i].applicationInfo) || !abTruthy(abLaunchIntentJs(lp[i].packageName))) {
            i++
            continue
        }
        val item = abEmptyObject()
        item.text = abApplicationLabelJs(lp[i].applicationInfo)
        item.description = lp[i].versionName
        item.result = abString(lp[i].packageName)
        r.push(item)
        i++
    }
    o.close()
    if (abTruthy(o.cancelled)) return
    js("Common").showListChooser(r, { id: dynamic ->
        callback(r[id].result)
        js("undefined")
    })
}

private fun abListApp(callback: dynamic) {
    abApi.listLaunchableApp(callback, true)
}

private fun abStartActivity(intent: dynamic): dynamic {
    try {
        if (abQueryIntentActivitiesJs(intent).size() > 0) {
            intent.addFlags(js("android.content.Intent").FLAG_ACTIVITY_NEW_TASK)
            js("ctx").startActivity(intent)
            return true
        }
    } catch (e: dynamic) {
        js("Log").e(e)
        js("Common").toast(
            "打开外部应用失败，请检查您是否授予了命令助手后台弹出界面或类似的权限\n" + e
        )
    }
    return false
}

private fun abStartActivityForResult(intent: dynamic, callback: dynamic) {
    var i = 0
    while (i < 65536) {
        if (!abIn(abApi.intentCallback, i)) break
        i++
    }
    if (i >= 65536) {
        js("Common").toast("启动Intent失败：同时请求的Intent过多")
        return
    }
    abApi.intentCallback[i] = callback
    try {
        js("ScriptInterface").startActivityForResult(intent, i)
    } catch (e: dynamic) {
        js("Log").e(e)
        js("Common").toast(
            "调用外部应用失败，请检查您是否授予了命令助手后台弹出界面或类似的权限\n" + e
        )
    }
}

private fun abBeginForegroundTask(name: dynamic, callback: dynamic) {
    if (abTruthy(abApi.foregroundTask[name])) return
    abApi.foregroundTask[name] = callback
    try {
        val intent = abNewForegroundIntentJs()
        intent.putExtra("taskName", abNewJavaStringJs(name))
        js("ScriptInterface").beginForegroundTask(intent)
    } catch (e: dynamic) {
        js("Log").e(e)
        js("Common").toast(
            "无法切换至前台，请检查您是否授予了命令助手后台弹出界面或类似的权限\n" + e
        )
    }
}

private fun abOnBeginForegroundTask(activity: dynamic, intent: dynamic) {
    val taskName = intent.getStringExtra("taskName")
    val task = abApi.foregroundTask[taskName]
    abDelete(abApi.foregroundTask, taskName)
    if (abTruthy(task)) {
        val delegee = task(activity, intent)
        if (abTruthy(delegee)) activity.setDelegee(delegee)
    }
}

// ---------------- 权限 ----------------
private fun abRequestPermissionsByGroup(groups: dynamic, callback: dynamic) {
    val result = abEmptyObject()
    result.flag = true
    result.success = abEmptyArray()
    result.denied = abEmptyArray()
    result.sync = true
    var count = abLength(groups)
    groups.forEach({ e: dynamic ->
        abApi.requestPermissions(e.permissions, e.explanation, { flag: dynamic, success: dynamic, denied: dynamic, sync: dynamic ->
            if (abTruthy(e.callback)) e.callback(flag, success, denied, sync)
            count--
            if (!abTruthy(flag)) result.flag = false
            if (abLength(success) > 0) result.success = result.success.concat(success)
            if (abLength(denied) > 0) result.denied = result.denied.concat(denied)
            if (!abTruthy(sync)) result.sync = false
            if (count <= 0 && abTruthy(callback)) {
                callback(result.flag, result.success, result.denied, result.sync)
            }
            js("undefined")
        }, e.mode)
        js("undefined")
    })
    if (count == 0 && abTruthy(callback)) callback(true, abEmptyArray(), abEmptyArray(), true)
}

private fun abRequestPermissions(permissions: dynamic, explanation: dynamic, callback: dynamic, mode: dynamic): dynamic {
    val denied = abEmptyArray()
    var i = 0
    while (i < abLength(permissions)) {
        // PERMISSION_GRANTED == 0
        if (!abLooseEq(js("ScriptInterface").checkSelfPermission(permissions[i]), 0)) {
            denied.push(permissions[i])
        }
        i++
    }
    if (abLength(denied) > 0 && !abLooseEq(mode, 3)) {
        val data = abEmptyObject()
        data.permissions = denied
        data.explanation = explanation
        data.callback = callback
        data.mode = mode
        abApi.permissionRequestData[abApi.permissionRequestData.end] = data
        abApi.permissionRequestData.end = abApi.permissionRequestData.end + 1
        if (!abTruthy(abApi.permissionRequest)) {
            try {
                js("ScriptInterface").beginPermissionRequest()
            } catch (e: dynamic) {
                js("Log").e(e)
                js("Common").toast(
                    "打开授权界面失败，可能造成部分App功能无法使用，请检查您是否授予了命令助手后台弹出界面或类似的权限\n" + e
                )
            }
        }
    } else if (abTruthy(callback)) {
        callback(true, permissions.slice(), abEmptyArray(), true)
    }
    return abLength(denied)
}

private fun abOnBeginPermissionRequest(activity: dynamic): dynamic {
    var lastData: dynamic = null
    var code = 0
    abApi.permissionRequest = activity
    lastData = abApi.permissionRequestData[abApi.permissionRequestData.start]
    if (abApi.permissionRequestData.start >= abApi.permissionRequestData.end) return activity.finish()
    abApi.doPermissionRequest(activity, lastData, code)
    val callback = abEmptyObject()
    callback.onRequestPermissionsResult = { activity2: dynamic, requestCode: dynamic, permissions: dynamic, grantResults: dynamic ->
        try {
            val succeed = abEmptyArray()
            val failed = abEmptyArray()
            if (abLooseEq(code, requestCode) && abTruthy(lastData)) {
                abForInJs(grantResults) { i: dynamic ->
                    // PERMISSION_GRANTED == 0
                    if (abLooseEq(grantResults[i], 0)) {
                        succeed.push(abString(permissions[i]))
                    } else {
                        failed.push(abString(permissions[i]))
                    }
                }
                lastData.showRationale = failed.every({ e: dynamic ->
                    activity2.shouldShowRequestPermissionRationale(e)
                })
                if (abLength(failed) == 0 || abLooseEq(lastData.mode, 2) || !abTruthy(lastData.showRationale)) {
                    val start = abApi.permissionRequestData.start
                    abDelete(abApi.permissionRequestData, start)
                    abApi.permissionRequestData.start = start + 1
                    if (abTruthy(lastData.callback)) {
                        lastData.callback(abLength(failed) == 0, succeed, failed, false)
                    }
                }
            }
            lastData = abApi.permissionRequestData[abApi.permissionRequestData.start]
            if (abApi.permissionRequestData.start < abApi.permissionRequestData.end) {
                code++
                abApi.doPermissionRequest(activity2, lastData, code)
            } else {
                activity2.finish()
            }
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }
    callback.onEndPermissionRequest = { activity2: dynamic ->
        abApi.permissionRequest = null
        js("undefined")
    }
    activity.setCallback(callback)
    return js("undefined")
}

private fun abDoPermissionRequest(activity: dynamic, data: dynamic, code: dynamic) {
    val msg = "命令助手需要申请" + abLength(data.permissions) + "个权限。" +
        (if (abTruthy(data.explanation)) "\n" + data.explanation else "")
    if (abTruthy(data.showRationale)) {
        val listener = abEmptyObject()
        listener.onClick = { _dia: dynamic, _w: dynamic ->
            activity.requestPermissionsCompat(code, data.permissions)
            js("undefined")
        }
        abNewAlertBuilderJs(activity)
            .setTitle("请求权限")
            .setCancelable(false)
            .setMessage(msg)
            .setPositiveButton("确定", abNewDialogClickListenerJs(listener))
            .show()
    } else if (abTruthy(data.explanation)) {
        val handler = abNewHandlerJs()
        val toast = abToastMakeTextJs(activity, msg, 0)
        toast.show()
        handler.postDelayed({
            try {
                activity.requestPermissionsCompat(code, data.permissions)
            } catch (e: dynamic) {
                js("erp")(e)
            }
            js("undefined")
        }, 1500)
    } else {
        activity.requestPermissionsCompat(code, data.permissions)
    }
}

// ---------------- PendingIntent / ABI ----------------
private fun abCreatePendingIntent(type: dynamic, intent: dynamic, flagList0: dynamic, requestCode0: dynamic): dynamic {
    var flags: dynamic = 0
    var flagList = flagList0
    var requestCode = requestCode0
    if (!abIsArray(flagList)) flagList = abEmptyArray()
    if (abIsNaN(requestCode)) requestCode = 0
    if (js("android.os.Build.VERSION.SDK_INT") >= 23) {
        if (flagList.includes("mutable")) {
            if (js("android.os.Build.VERSION.SDK_INT") >= 31) {
                flags = abOr(flags, js("android.app.PendingIntent").FLAG_MUTABLE)
            }
        } else {
            flags = abOr(flags, js("android.app.PendingIntent").FLAG_IMMUTABLE)
        }
    }
    if (flagList.includes("updateCurrent")) {
        flags = abOr(flags, js("android.app.PendingIntent").FLAG_UPDATE_CURRENT)
    } else if (flagList.includes("cancelCurrent")) {
        flags = abOr(flags, js("android.app.PendingIntent").FLAG_CANCEL_CURRENT)
    } else if (flagList.includes("noCreate")) {
        flags = abOr(flags, js("android.app.PendingIntent").FLAG_NO_CREATE)
    }
    if (flagList.includes("oneShot")) {
        flags = abOr(flags, js("android.app.PendingIntent").FLAG_ONE_SHOT)
    }
    if (type == "service" || type == "foregroundService") {
        if (js("android.os.Build.VERSION.SDK_INT") > 26 && type == "foregroundService") {
            return js("android.app.PendingIntent").getForegroundService(js("ctx"), requestCode, intent, flags)
        }
        return js("android.app.PendingIntent").getService(js("ctx"), requestCode, intent, flags)
    }
    if (type == "broadcast") {
        return js("android.app.PendingIntent").getBroadcast(js("ctx"), requestCode, intent, flags)
    }
    if (abIsArray(intent)) {
        return js("android.app.PendingIntent").getActivities(js("ctx"), requestCode, intent, flags)
    }
    return js("android.app.PendingIntent").getActivity(js("ctx"), requestCode, intent, flags)
}

private fun abGetABIs(): dynamic {
    if (js("android.os.Build.VERSION.SDK_INT") >= 21) {
        return js("android.os.Build.SUPPORTED_ABIS").map({ e: dynamic -> abString(e) })
    }
    val r = abEmptyArray()
    r.push(abString(js("android.os.Build.CPU_ABI")))
    r.push(abString(js("android.os.Build.CPU_ABI2")))
    return r
}

// ---------------- Uri / 文件 ----------------
private fun abUriToFile(uri0: dynamic): dynamic {
    var uri = uri0
    var r: dynamic = null
    var cursor: dynamic = null
    var selection: dynamic = null
    var selectionArgs: dynamic = null
    val isKitKat = js("android.os.Build.VERSION.SDK_INT") >= 19
    if (!abIsUri(uri)) return null
    if (uri.getScheme().equalsIgnoreCase("content")) {
        if (isKitKat && abIsDocumentUriJs(uri)) {
            if (abLooseEq(abString(uri.getAuthority()), "com.android.externalstorage.documents")) {
                val docs = abString(abDocumentIdJs(uri)).split(":")
                if (docs[0] == "primary") {
                    return abExternalStorageDirJs() + "/" + docs[1]
                }
            } else if (abLooseEq(abString(uri.getAuthority()), "com.android.providers.downloads.documents")) {
                uri = abWithAppendedIdJs(
                    abUriParseJs("content://downloads/public_downloads"),
                    js("parseInt")(abDocumentIdJs(uri))
                )
            } else if (abLooseEq(abString(uri.getAuthority()), "com.android.providers.media.documents")) {
                val docs = abString(abDocumentIdJs(uri)).split(":")
                if (docs[0] == "image") {
                    uri = js("android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI")
                } else if (docs[0] == "video") {
                    uri = js("android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI")
                } else if (docs[0] == "audio") {
                    uri = js("android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI")
                }
                selection = "_id=?"
                selectionArgs = abSingleArray(docs[1])
            }
        }
        try {
            val projection = abEmptyArray()
            projection.push("_data")
            cursor = abResolveQueryJs(uri, projection, selection, selectionArgs, null)
            if (abTruthy(cursor) && abTruthy(cursor.moveToFirst())) {
                r = abString(cursor.getString(cursor.getColumnIndexOrThrow("_data")))
            }
        } catch (e: dynamic) {
            js("Log").e(e)
        }
        if (abTruthy(cursor)) cursor.close()
        return r
    } else if (uri.getScheme().equalsIgnoreCase("file")) {
        return abString(uri.getPath())
    }
    return null
}

private fun abFileToUri(file0: dynamic): dynamic {
    val file = if (abIsFile(file0)) file0 else abNewFileJs(file0)
    return if (js("MapScript").host == "Android") {
        abFileToUriJs(file)
    } else {
        abFileUriFromFileJs(file)
    }
}

private fun abSelectFile(mimeType: dynamic, callback: dynamic) {
    val intent = abNewIntent1Js(js("android.content.Intent").ACTION_GET_CONTENT)
    intent.setType(mimeType)
    abApi.startActivityForResult(intent, { resultCode: dynamic, data: dynamic ->
        abSelectFileResult(callback, resultCode, data)
        js("undefined")
    })
}

private fun abSelectFileResult(callback: dynamic, resultCode: dynamic, data: dynamic) {
    if (!abLooseEq(resultCode, -1)) return // RESULT_OK
    callback(abApi.uriToFile(data.getData()))
}

private fun abSelectImage(callback: dynamic) {
    try {
        abApi.selectFile("image/*", { path: dynamic ->
            callback(path)
            js("undefined")
        })
        return
    } catch (e: dynamic) {
        js("erp")(e, true)
    }
}

private fun abSendText(text: dynamic, withSharesheet: dynamic): dynamic {
    var intent = abNewIntent1Js(js("android.content.Intent").ACTION_SEND)
    intent.setType("text/plain")
    intent.putExtra(js("android.content.Intent").EXTRA_TEXT, abNewJavaStringJs(abString(text)))
    if (abTruthy(withSharesheet)) {
        intent = js("android.content.Intent").createChooser(intent, null)
    }
    return abApi.startActivity(intent)
}

private fun abSendUri(uri: dynamic, mimeType: dynamic, withSharesheet: dynamic): dynamic {
    var intent = abNewIntent1Js(js("android.content.Intent").ACTION_SEND)
    intent.setType(mimeType)
    intent.putExtra(js("android.content.Intent").EXTRA_STREAM, uri)
    intent.addFlags(
        abOr(
            js("android.content.Intent").FLAG_GRANT_READ_URI_PERMISSION,
            js("android.content.Intent").FLAG_GRANT_WRITE_URI_PERMISSION
        )
    )
    if (abTruthy(withSharesheet)) {
        intent = js("android.content.Intent").createChooser(intent, null)
    }
    return abApi.startActivity(intent)
}

private fun abSendFile(file: dynamic, mimeType: dynamic, withSharesheet: dynamic): dynamic {
    return abApi.sendUri(abApi.fileToUri(file), mimeType, withSharesheet)
}

private fun abShareText(text: dynamic): dynamic {
    return abApi.sendText(text, true)
}

private fun abViewUri(uri: dynamic): dynamic {
    return js("AndroidBridge").startActivity(
        abNewIntent2Js(js("android.content.Intent").ACTION_VIEW, abUriParseJs(uri))
    )
}

private fun abCreateShortcut(intent: dynamic, name: dynamic, icon: dynamic) {
    if (js("android.os.Build.VERSION.SDK_INT") >= 26) {
        abApi.doCreateShortcut(js("ctx"), intent, name, icon)
    } else {
        val i = abNewIntent1Js("com.android.launcher.action.INSTALL_SHORTCUT")
        i.putExtra(js("android.content.Intent").EXTRA_SHORTCUT_NAME, name)
        i.putExtra("duplicate", false)
        i.putExtra(js("android.content.Intent").EXTRA_SHORTCUT_INTENT, intent)
        if (abIsNaN(icon)) {
            i.putExtra(js("android.content.Intent").EXTRA_SHORTCUT_ICON, icon)
        } else {
            i.putExtra(
                js("android.content.Intent").EXTRA_SHORTCUT_ICON_RESOURCE,
                abShortcutIconResourceJs(js("ctx"), icon)
            )
        }
        js("ctx").sendBroadcast(i)
    }
}

private fun abDoCreateShortcut(context: dynamic, intent: dynamic, name: dynamic, icon: dynamic) {
    val manager = context.getSystemService(context.SHORTCUT_SERVICE)
    val builder = abNewShortcutBuilderJs(context, name)
    builder.setShortLabel(name)
    builder.setLongLabel(name)
    builder.setIcon(if (abIsNaN(icon)) icon else abIconWithResourceJs(context, icon))
    builder.setIntent(intent)
    val shortcut = builder.build()
    val callback = abApi.createPendingIntent(
        "broadcast",
        manager.createShortcutResultIntent(shortcut),
        abSingleArray("oneShot"),
        js("undefined")
    )
    manager.requestPinShortcut(shortcut, callback.getIntentSender())
}

private fun abScanMedia(files0: dynamic, statusListener: dynamic) {
    var files = files0
    var scanConn: dynamic = null
    var i = 0
    if (!abIsArray(files)) files = abSingleArray(files)
    fun scanNext() {
        if (i >= abLength(files)) {
            scanConn.disconnect()
            if (abTruthy(statusListener)) statusListener("disconnected")
            return
        }
        val e = files[i]
        if (abTruthy(statusListener)) statusListener("scanStart", e, i, abLength(files))
        if (abIsString(e)) {
            scanConn.scanFile(e, null)
        } else if (abIsFile(e)) {
            scanConn.scanFile(e.getPath(), null)
        } else {
            scanConn.scanFile(e.path, if (abTruthy(e.mimeTypes)) e.mimeTypes else null)
        }
    }
    val listener = abEmptyObject()
    listener.onMediaScannerConnected = {
        try {
            if (abTruthy(statusListener)) statusListener("connected")
            scanNext()
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }
    listener.onScanCompleted = { path: dynamic, uri: dynamic ->
        try {
            if (abTruthy(statusListener)) {
                statusListener("scanCompleted", uri, files[i], i, abLength(files))
            }
            i++
            scanNext()
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }
    scanConn = abNewMediaScannerJs(js("ctx"), listener)
    scanConn.connect()
}

private fun abStartAccessibilitySvcByRoot(): dynamic {
    val s = abString(
        abSecureGetStringJs(
            abGetContentResolverJs(),
            js("android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES")
        )
    ).split(":")
    val t = "com.xero.ca/com.xero.ca.AccessibilitySvc"
    val found = s.some({ e: dynamic -> abLooseEq(e, t) })
    if (abTruthy(found)) return true
    s.push(t)
    try {
        var p = abRuntimeExecJs(abMethodArgs("su", "root", "settings", "put", "secure", "enabled_accessibility_services", s.join(":")))
        p.waitFor()
        if (p.getErrorStream().available() > 0) return false
        p = abRuntimeExecJs(abMethodArgs("su", "root", "settings", "put", "secure", "accessibility_enabled", "1"))
        p.waitFor()
        if (p.getErrorStream().available() > 0) return false
        return true
    } catch (e: dynamic) {
        js("Log").e(e)
    }
    return false
}

private fun abStartAccessibilitySvcByRootAsyncTask(callback: dynamic, silently: dynamic) {
    val success = abApi.startAccessibilitySvcByRoot()
    if (abTruthy(callback)) callback(success)
    if (abTruthy(silently)) return
    if (abTruthy(success)) {
        js("Common").toast("无障碍服务已启动")
    } else {
        js("Common").toast("无障碍服务启动失败")
    }
}

private fun abStartAccessibilitySvcByRootAsync(callback: dynamic, silently: dynamic) {
    js("Threads").run({
        abStartAccessibilitySvcByRootAsyncTask(callback, silently)
        js("undefined")
    })
}

private fun abGetUserID(): dynamic {
    return js("ScriptInterface").getUserID()
}

private fun abShouldForceRemoveTask(): Boolean {
    return abLooseEq(js("SettingsCompat").rom, "FLYME") || abLooseEq(js("SettingsCompat").rom, "MEIZU")
    //JavaException: android.view.WindowManager$BadTokenException: Unable to add window -- token null is not valid; is your activity running?
    //魅族Flyme com.meizu.widget.OptionPopupWindow 不支持在有后台任务但context是服务的情况下显示
    //且客服说是本App的问题，因此我只能勉为其难地剥夺了魅族用户取消选择隐藏后台任务的能力
}

private fun abExitLoadingTask(activity: dynamic, keepActivity: dynamic) {
    try {
        if (abTruthy(keepActivity)) {
            try {
                activity.moveTaskToBack(false)
                return
            } catch (e: dynamic) {
                js("Log").e(e)
            }
        }
        if (abLooseEq(js("G").style, "Material")) {
            activity.finishAndRemoveTask()
        } else {
            activity.finish()
        }
    } catch (e: dynamic) {
        js("erp")(e)
    }
}

private fun abExitLoading(keepActivity0: dynamic) {
    val activity = js("ScriptInterface").getBindActivity()
    if (!abTruthy(activity)) return
    var keepActivity = keepActivity0
    if (abApi.shouldForceRemoveTask()) keepActivity = false
    activity.runOnUiThread({
        abExitLoadingTask(activity, keepActivity)
        js("undefined")
    })
}

private fun abEnsureFloatClick() {
    try {
        if (js("SettingsCompat").ensureCanFloat(false)) {
            js("G").ui({
                try {
                    js("G").Toast.makeText(js("ctx"), "悬浮窗权限已打开", 0).show()
                } catch (e: dynamic) {
                    js("erp")(e)
                }
                js("undefined")
            })
        }
    } catch (e: dynamic) {
        js("erp")(e)
    }
}

private fun abExitAppClick() {
    try {
        js("CA").performExit()
    } catch (e: dynamic) {
        js("erp")(e)
    }
}

private fun abShowActivityContentTask(activity: dynamic, canFloat: dynamic) {
    try {
        val layout = abNewLinearLayoutJs(js("ctx"))
        layout.setBackgroundColor(js("G.Color.WHITE"))
        layout.setOrientation(js("G.LinearLayout.VERTICAL"))
        layout.setGravity(js("G.Gravity.CENTER"))
        layout.setPadding(abDp(10), abDp(10), abDp(10), abDp(10))
        layout.setLayoutParams(abNewViewGroupParamsJs(-1, -1))

        val help = abNewTextViewJs(js("ctx"))
        help.setGravity(js("G.Gravity.CENTER"))
        help.setTextSize(16)
        help.setPadding(abDp(10), abDp(10), abDp(10), abDp(10))
        help.setText(
            if (abTruthy(canFloat)) {
                "当前模式∶悬浮窗模式\n您现在可以在屏幕上找到命令助手的悬浮窗，找不到的话请手动打开命令助手的悬浮窗权限"
            } else {
                "当前模式∶页面模式\n检测到命令助手没有悬浮窗权限，无法以悬浮窗模式打开命令助手。如果您已给予权限，请手动重启命令助手。"
            }
        )
        help.setLayoutParams(abNewViewGroupParamsJs(-1, -2))
        layout.addView(help)

        val ensurefloat = abNewButtonJs(js("ctx"))
        ensurefloat.setText("检查悬浮窗权限")
        ensurefloat.setLayoutParams(abNewLinearLayoutParams2Js(-1, -2))
        val ensureListener = abEmptyObject()
        ensureListener.onClick = { _v: dynamic ->
            abEnsureFloatClick()
            js("undefined")
        }
        ensurefloat.setOnClickListener(abNewViewClickListenerJs(ensureListener))
        layout.addView(ensurefloat)

        val exit = abNewButtonJs(js("ctx"))
        exit.setText("退出命令助手")
        exit.setLayoutParams(abNewLinearLayoutParams2Js(-1, -2))
        val exitListener = abEmptyObject()
        exitListener.onClick = { _v: dynamic ->
            abExitAppClick()
            js("undefined")
        }
        exit.setOnClickListener(abNewViewClickListenerJs(exitListener))
        layout.addView(exit)

        activity.setContentView(layout)
    } catch (e: dynamic) {
        js("erp")(e)
    }
}

private fun abShowActivityContent(canFloat: dynamic) {
    val activity = js("ScriptInterface").getBindActivity()
    if (!abTruthy(activity)) return
    activity.runOnUiThread({
        abShowActivityContentTask(activity, canFloat)
        js("undefined")
    })
}

private fun abCheckNecessaryPermissionsSync() {
    js("Threads").awaitPromise({ resolve: dynamic ->
        abApi.checkNecessaryPermissions(resolve)
        js("undefined")
    })
}

private fun abCheckNecessaryPermissions(callback: dynamic) {
    val group = abEmptyObject()
    val permissions = abEmptyArray()
    permissions.push("android.permission.READ_PHONE_STATE")
    group.permissions = permissions
    group.explanation = "获取手机识别码（可选）\n\n此权限用于向命令助手作者反馈错误时唯一标识用户"
    group.mode = 2
    val groups = abEmptyArray()
    groups.push(group)
    abApi.requestPermissionsByGroup(groups, { _flag: dynamic, _success: dynamic, _denied: dynamic, _sync: dynamic ->
        if (abTruthy(callback)) callback()
        js("undefined")
    })
}

// ---------------- 通知动作 / TileService ----------------
private fun abGetKeeperMenu(): dynamic {
    if (!abTruthy(js("CA").settings.notificationActions)) {
        js("CA").settings.notificationActions = js("Object").copy(abApi.defaultKeeperMenu)
    }
    return js("CA").settings.notificationActions
}

private fun abBuildTiles(): dynamic {
    val tiles = abEmptyObject()

    val none = abEmptyObject()
    none.name = "无"
    none.initTile = { _data: dynamic, tile: dynamic ->
        tile.label = "命令助手"
        tile.subtitle = ""
        js("undefined")
    }
    none.updateTile = { _data: dynamic, tile: dynamic ->
        tile.state = tile.STATE_INACTIVE
        js("undefined")
    }
    none.onTileClick = abThis2Js { self: dynamic, data: dynamic, tile: dynamic ->
        self.updateTile(data, tile)
        js("undefined")
    }
    tiles["null"] = none

    val action = abEmptyObject()
    action.name = "执行动作"
    action.description = "点击后执行一个动作"
    action.create = {
        val r = abEmptyObject()
        r.action = abEmptyObject()
        r
    }
    action.edit = { data: dynamic, newCreated: dynamic, callback: dynamic ->
        abEditTileAction(data, newCreated, callback)
        js("undefined")
    }
    action.updateTile = { data: dynamic, tile: dynamic ->
        abUpdateTileAction(data, tile)
        js("undefined")
    }
    action.onTileClick = { data: dynamic ->
        abClickTileAction(data)
        js("undefined")
    }
    tiles["action"] = action

    val iconVisibility = abEmptyObject()
    iconVisibility.name = "切换悬浮图标显示/隐藏"
    iconVisibility.initTile = { _data: dynamic, tile: dynamic ->
        tile.label = "悬浮图标"
        tile.subtitle = ""
        js("undefined")
    }
    iconVisibility.updateTile = { _data: dynamic, tile: dynamic ->
        tile.state = if (abTruthy(js("CA").icon)) tile.STATE_ACTIVE else tile.STATE_INACTIVE
        js("undefined")
    }
    iconVisibility.onTileClick = { _data: dynamic, tile: dynamic ->
        if (abTruthy(js("CA").icon)) {
            js("CA").hideIcon()
            tile.state = tile.STATE_INACTIVE
        } else {
            js("CA").showIcon()
            tile.state = tile.STATE_ACTIVE
        }
        js("undefined")
    }
    tiles["ca.iconVisibility"] = iconVisibility

    return tiles
}

private fun abEditTileAction(data: dynamic, newCreated: dynamic, callback: dynamic) {
    var keys = js("Object").keys(js("CA").Actions)
    var curIndex: dynamic = null
    var curKey: dynamic = null
    var curAction: dynamic = null
    var curData = data.action
    curIndex = if (!abTruthy(newCreated)) keys.indexOf(curData.action) else -1
    if (curIndex >= 0) {
        curKey = keys[curIndex]
        curAction = js("CA").Actions[curKey]
        keys.splice(curIndex, 1)
    }
    keys = keys.map({ e: dynamic ->
        val action = js("CA").Actions[e]
        val item = abEmptyObject()
        item.text = action.name
        item.description = action.description
        item.key = e
        item.action = action
        item
    })
    if (abTruthy(curAction)) {
        val current = abEmptyObject()
        current.text = "(当前) " + curAction.name
        current.description = curAction.description
        current.key = curKey
        current.action = curAction
        current.current = true
        keys.unshift(current)
    }
    js("Common").showListChooser(keys, { i: dynamic ->
        val e = keys[i]
        val action = e.action
        if (!abTruthy(e.current)) {
            curData = if (abTruthy(action.create)) action.create() else abEmptyObject()
            curData.action = e.key
        }
        if (abTruthy(action.edit)) {
            action.edit(curData, !abTruthy(e.current), {
                data.action = curData
                callback(data)
                js("undefined")
            })
        } else {
            data.action = curData
            callback(data)
        }
        js("undefined")
    })
}

private fun abUpdateTileAction(data: dynamic, tile: dynamic) {
    val actionData = data.action
    val action = js("CA").Actions[actionData.action]
    if (!abTruthy(action)) return
    if (abTruthy(action.available) && !abTruthy(action.available(actionData))) return
    tile.label = abOrValue(if (abTruthy(action.getName)) action.getName(actionData) else action.name, "")
    tile.subtitle = abOrValue(
        if (abTruthy(action.getDescription)) action.getDescription(actionData) else action.description,
        ""
    )
    tile.state = tile.STATE_INACTIVE
}

private fun abClickTileAction(data: dynamic) {
    val actionData = data.action
    val action = js("CA").Actions[actionData.action]
    if (!abTruthy(action)) return
    if (abTruthy(action.available) && !abTruthy(action.available(actionData))) return
    action.execute(actionData)
}

private fun abGetTileService(): dynamic {
    if (!abTruthy(js("CA").settings.qstile)) {
        js("CA").settings.qstile = js("Object").copy(abApi.defaultTile)
    }
    val lastTileService = abApi.lastTileService
    val tile = js("CA").settings.qstile
    var tileService = abApi.Tiles[tile.tile]
    var tileContext = abEmptyObject()
    if (!abTruthy(tileService)) {
        tileService = abApi.Tiles["null"]
    }
    if (abTruthy(lastTileService) && abLooseEq(tileService, lastTileService.service) &&
        abLooseEq(tile, lastTileService.data) && !abTruthy(lastTileService.invalid)
    ) {
        return lastTileService
    }
    if (abTruthy(lastTileService) && abTruthy(lastTileService.service.unload)) {
        lastTileService.service.unload(lastTileService.data, lastTileService.context)
    }
    if (abTruthy(tileService.load)) {
        val loaded = tileService.load(tile, tileContext)
        tileContext = if (abTruthy(loaded)) loaded else tileContext
    }
    val result = abEmptyObject()
    result.data = tile
    result.service = tileService
    result.context = tileContext
    abApi.lastTileService = result
    return result
}

private fun abNotifyTileUpdate() {
    js("ScriptInterface").notifyTileUpdate()
}

private fun abShowEditTileTask(data0: dynamic, callback: dynamic) {
    var curIndex: dynamic = null
    var curKey: dynamic = null
    var curTile: dynamic = null
    val curData = data0
    var keys = js("Object").keys(abApi.Tiles)
    curIndex = keys.indexOf(data0.tile)
    if (curIndex >= 0) {
        curKey = keys[curIndex]
        curTile = abApi.Tiles[curKey]
        keys.splice(curIndex, 1)
    }
    keys = keys.map({ e: dynamic ->
        val tileData = abApi.Tiles[e]
        val item = abEmptyObject()
        item.text = tileData.name
        item.description = tileData.description
        item.key = e
        item.data = tileData
        item
    })
    val current = abEmptyObject()
    current.text = "(当前) " + curTile.name
    current.description = curTile.description
    current.key = curKey
    current.data = curTile
    current.current = true
    keys.unshift(current)
    js("Common").showListChooser(keys, { i: dynamic ->
        val e = keys[i]
        val tile = e.data
        var newData: dynamic
        if (abTruthy(e.current)) {
            newData = curData
        } else {
            newData = if (abTruthy(tile.create)) tile.create() else abEmptyObject()
            newData.tile = e.key
        }
        if (abTruthy(tile.edit)) {
            tile.edit(newData, !abTruthy(e.current), {
                callback(newData)
                js("undefined")
            })
        } else {
            callback(newData)
        }
        js("undefined")
    })
}

private fun abShowEditTile(data: dynamic, callback: dynamic) {
    js("G").ui({
        try {
            abShowEditTileTask(data, callback)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

private fun abBuildUriActions(): dynamic {
    val uriActions = abEmptyObject()

    val open = abEmptyObject()
    open.default = {
        js("CA").showGen(true)
        js("undefined")
    }
    uriActions.open = open

    val command = abEmptyObject()
    command.edit = { _fragment: dynamic, query: dynamic, _extras: dynamic ->
        js("G").ui({
            try {
                js("CA").showGen(true)
                js("CA").cmd.setText(abString(query.text))
                js("CA").showGen.activate(false)
            } catch (e: dynamic) {
                js("erp")(e)
            }
            js("undefined")
        })
        js("undefined")
    }
    uriActions.command = command

    val feedback = abEmptyObject()
    feedback.authorize = { _fragment: dynamic, query: dynamic, _extras: dynamic ->
        js("GiteeFeedback").callbackOAuth(abString(query.code))
        js("undefined")
    }
    uriActions.feedback = feedback

    val push = abEmptyObject()
    push.settings = {
        js("PushService").showSettings("推送设置")
        js("undefined")
    }
    uriActions.push = push

    val user = abEmptyObject()
    user.login = {
        js("UserManager").processUriAction("login")
        js("undefined")
    }
    user.info = {
        js("UserManager").processUriAction("info")
        js("undefined")
    }
    user.autologin = { _fragment: dynamic, query: dynamic, _extras: dynamic ->
        js("UserManager").processUriAction("autologin", query)
        js("undefined")
    }
    user.authorize = { _fragment: dynamic, query: dynamic, _extras: dynamic ->
        js("UserManager").showAuthorize(query)
        js("undefined")
    }
    uriActions.user = user

    return uriActions
}

// ---------------- 安装 ----------------
fun installAndroidBridge() {
    abApi.intentCallback = abEmptyObject()
    val requestData = abEmptyObject()
    requestData.start = 0
    requestData.end = 0
    abApi.permissionRequestData = requestData
    val permissionCallback = abEmptyObject()
    permissionCallback.length = 0
    abApi.permissionCallback = permissionCallback
    abApi.foregroundTask = abEmptyObject()

    abApi.onCreate = {
        abOnCreate()
        js("undefined")
    }
    abApi.initialize = {
        abInitialize()
        js("undefined")
    }
    abApi.onNewIntent = { intent: dynamic, startByIntent: dynamic ->
        abOnNewIntent(intent, startByIntent)
        js("undefined")
    }
    abApi.verifyApk = {
        abVerifyApk()
        js("undefined")
    }
    abApi.verifySign = {
        abVerifySign()
        js("undefined")
    }
    abApi.verifyContext = {
        abVerifyContext()
        js("undefined")
    }
    abApi.verifyDex = {
        abVerifyDex()
        js("undefined")
    }
    abApi.findDeclaredMethodClass = { cls: dynamic, params: dynamic, parent: dynamic ->
        abFindDeclaredMethodClass(cls, params, parent)
    }
    abApi.callHide = {
        abCallHide()
        js("undefined")
    }
    abApi.scriptAction = {
        abScriptAction()
        js("undefined")
    }
    abApi.openUriAction = { uri: dynamic, extras: dynamic ->
        abOpenUriAction(uri, extras)
        js("undefined")
    }
    abApi.getBaseUriAction = { path: dynamic -> abGetBaseUriAction(path) }
    abApi.getQueryKV = { query: dynamic -> abGetQueryKV(query) }
    abApi.notifySettings = {
        abNotifySettings()
        js("undefined")
    }
    abApi.showSettings = { title: dynamic ->
        abShowSettings(title)
        js("undefined")
    }
    abApi.getAppIcon = { abGetAppIcon() }
    abApi.getAppIconBadged = { abGetAppIconBadged() }
    abApi.initIcon = {
        abInitIcon()
        js("undefined")
    }
    abApi.listLaunchableApp = { callback: dynamic, includeCancel: dynamic ->
        abListLaunchableApp(callback, includeCancel)
        js("undefined")
    }
    abApi.listApp = { callback: dynamic ->
        abListApp(callback)
        js("undefined")
    }
    abApi.startActivity = { intent: dynamic -> abStartActivity(intent) }
    abApi.startActivityForResult = { intent: dynamic, callback: dynamic ->
        abStartActivityForResult(intent, callback)
        js("undefined")
    }
    abApi.beginForegroundTask = { name: dynamic, callback: dynamic ->
        abBeginForegroundTask(name, callback)
        js("undefined")
    }
    abApi.onBeginForegroundTask = { activity: dynamic, intent: dynamic ->
        abOnBeginForegroundTask(activity, intent)
        js("undefined")
    }
    abApi.requestPermissionsByGroup = { groups: dynamic, callback: dynamic ->
        abRequestPermissionsByGroup(groups, callback)
        js("undefined")
    }
    abApi.requestPermissions = { permissions: dynamic, explanation: dynamic, callback: dynamic, mode: dynamic ->
        abRequestPermissions(permissions, explanation, callback, mode)
    }
    abApi.onBeginPermissionRequest = { activity: dynamic -> abOnBeginPermissionRequest(activity) }
    abApi.doPermissionRequest = { activity: dynamic, data: dynamic, code: dynamic ->
        abDoPermissionRequest(activity, data, code)
        js("undefined")
    }
    abApi.createPendingIntent = { type: dynamic, intent: dynamic, flagList: dynamic, requestCode: dynamic ->
        abCreatePendingIntent(type, intent, flagList, requestCode)
    }
    abApi.getABIs = { abGetABIs() }
    abApi.uriToFile = { uri: dynamic -> abUriToFile(uri) }
    abApi.fileToUri = { file: dynamic -> abFileToUri(file) }
    abApi.selectFile = { mimeType: dynamic, callback: dynamic ->
        abSelectFile(mimeType, callback)
        js("undefined")
    }
    abApi.selectImage = { callback: dynamic ->
        abSelectImage(callback)
        js("undefined")
    }
    abApi.sendText = { text: dynamic, withSharesheet: dynamic -> abSendText(text, withSharesheet) }
    abApi.sendUri = { uri: dynamic, mimeType: dynamic, withSharesheet: dynamic ->
        abSendUri(uri, mimeType, withSharesheet)
    }
    abApi.sendFile = { file: dynamic, mimeType: dynamic, withSharesheet: dynamic ->
        abSendFile(file, mimeType, withSharesheet)
    }
    abApi.shareText = { text: dynamic -> abShareText(text) }
    abApi.viewUri = { uri: dynamic -> abViewUri(uri) }
    abApi.createShortcut = { intent: dynamic, name: dynamic, icon: dynamic ->
        abCreateShortcut(intent, name, icon)
        js("undefined")
    }
    abApi.doCreateShortcut = { context: dynamic, intent: dynamic, name: dynamic, icon: dynamic ->
        abDoCreateShortcut(context, intent, name, icon)
        js("undefined")
    }
    abApi.scanMedia = { files: dynamic, statusListener: dynamic ->
        abScanMedia(files, statusListener)
        js("undefined")
    }
    abApi.startAccessibilitySvcByRoot = { abStartAccessibilitySvcByRoot() }
    abApi.startAccessibilitySvcByRootAsync = { callback: dynamic, silently: dynamic ->
        abStartAccessibilitySvcByRootAsync(callback, silently)
        js("undefined")
    }
    abApi.getUserID = { abGetUserID() }
    abApi.shouldForceRemoveTask = { abShouldForceRemoveTask() }
    abApi.exitLoading = { keepActivity: dynamic ->
        abExitLoading(keepActivity)
        js("undefined")
    }
    abApi.showActivityContent = { canFloat: dynamic ->
        abShowActivityContent(canFloat)
        js("undefined")
    }
    abApi.checkNecessaryPermissionsSync = {
        abCheckNecessaryPermissionsSync()
        js("undefined")
    }
    abApi.checkNecessaryPermissions = { callback: dynamic ->
        abCheckNecessaryPermissions(callback)
        js("undefined")
    }
    abApi.getKeeperMenu = { abGetKeeperMenu() }
    abApi.defaultKeeperMenu = Loader.fromFile("../../../data/keepermenu.json").default
    abApi.Tiles = abBuildTiles()
    abApi.getTileService = { abGetTileService() }
    abApi.notifyTileUpdate = {
        abNotifyTileUpdate()
        js("undefined")
    }
    val defaultTile = abEmptyObject()
    defaultTile.tile = "null"
    abApi.defaultTile = defaultTile
    abApi.showEditTile = { data: dynamic, callback: dynamic ->
        abShowEditTile(data, callback)
        js("undefined")
    }
    abApi.uriActions = abBuildUriActions()

    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("AndroidBridge", abApi)
    }
}
