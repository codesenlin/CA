package ca.kotlin

/**
 * modules/utils/SettingsCompat.js 的 Kotlin/JS 版本。
 * 原作者 czy1121，Apache-2.0；由 ProjectXero 翻译为 JS，这里再迁到 Kotlin/JS。
 *
 * 所有方法都是单例调用，不需要动态 this；onCreate 由 MapScript 调用。
 */
private val settingsApi: dynamic = js("({})")

private fun startSafely(intent: dynamic): Boolean {
    try {
        if (js("ctx").getPackageManager().queryIntentActivities(intent, js("android.content.pm.PackageManager").MATCH_DEFAULT_ONLY).size() > 0) {
            intent.addFlags(js("android.content.Intent").FLAG_ACTIVITY_NEW_TASK)
            js("ctx").startActivity(intent)
            return true
        }
    } catch (e: dynamic) { js("Log").e(e) }
    return false
}

private fun showAppSettings() {
    val intent = js("new android.content.Intent()")
    intent.addFlags(js("android.content.Intent").FLAG_ACTIVITY_NEW_TASK)
    if (settingsApi.SYSVER >= 9) {
        intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS")
        intent.setData(js("android.net.Uri").fromParts("package", js("ctx").getPackageName(), null))
    } else {
        intent.setAction(js("android.content.Intent").ACTION_VIEW)
        intent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails")
        intent.putExtra("com.android.settings.ApplicationPkgName", js("ctx").getPackageName())
    }
    startSafely(intent)
}

private fun canDrawOverlays(): Boolean {
    if (settingsApi.SYSVER >= 23) return js("android.provider.Settings").canDrawOverlays(js("ctx"))
    if (settingsApi.SYSVER >= 18) return checkOp(js("ctx"), 24)
    return true
}

private fun checkOp(context: dynamic, op: dynamic): Boolean {
    try {
        return context.getSystemService("appops").checkOp(op, js("android.os.Binder").getCallingUid(), context.getPackageName()) == 0
    } catch (e: dynamic) { js("Log").e(e) }
    return false
}

private fun setMode(context: dynamic, op: dynamic, allowed: dynamic): Boolean {
    if (settingsApi.SYSVER < 18 || settingsApi.SYSVER >= 21) return false
    try {
        context.getSystemService("appops").setMode(op, js("android.os.Binder").getCallingUid(), context.getPackageName(), allowed)
        return true
    } catch (e: dynamic) { js("Log").e(e) }
    return false
}

private fun manageDrawOverlays() {
    if (settingsApi.SYSVER >= 18) {
        if (manageDrawOverlaysForRom()) return
    }
    if (settingsApi.SYSVER >= 23) {
        val intent = js("new android.content.Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION)")
        intent.setData(js("android.net.Uri").parse("package:" + js("ctx").getPackageName()))
        intent.addFlags(js("android.content.Intent").FLAG_ACTIVITY_NEW_TASK)
        if (startSafely(intent)) return
    }
    showAppSettings()
}

private fun manageDrawOverlaysForRom(): Boolean {
    val handler = settingsApi.ShowManager[settingsApi.rom]
    if (handler == null) return false
    return handler()
}

private fun getProp(key: String): String? {
    var ln: dynamic = null
    var ins: dynamic = null
    try {
        val p = js("java.lang.Runtime").getRuntime().exec("getprop " + key)
        ins = js("new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()), 1024)")
        ln = ins.readLine()
    } catch (e: dynamic) { js("Log").e(e) }
    try { if (ins != null) ins.close() } catch (e: dynamic) { js("Log").e(e) }
    return if (ln != null) ln.toString() else null
}

private fun showMiui(): Boolean {
    val intent = js("new android.content.Intent('miui.intent.action.APP_PERM_EDITOR')")
    intent.putExtra("extra_pkgname", js("ctx").getPackageName())
    intent.setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.AppPermissionsEditorActivity")
    if (startSafely(intent)) return true
    intent.setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")
    if (startSafely(intent)) return true
    if (settingsApi.SYSVER < 21) {
        val i2 = js("new android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)")
        i2.setData(js("android.net.Uri").fromParts("package", js("ctx").getPackageName(), null))
        startSafely(i2)
    }
    return false
}

private fun showEmui(): Boolean {
    val intent = js("new android.content.Intent()")
    if (settingsApi.SYSVER >= 21) {
        intent.setClassName("com.huawei.systemmanager", "com.huawei.systemmanager.addviewmonitor.AddViewMonitorActivity")
        if (startSafely(intent)) return true
    }
    intent.setClassName("com.huawei.systemmanager", "com.huawei.notificationmanager.ui.NotificationManagmentActivity")
    intent.putExtra("showTabsNumber", 1)
    if (startSafely(intent)) return true
    intent.setClassName("com.huawei.systemmanager", "com.huawei.permissionmanager.ui.MainActivity")
    return startSafely(intent)
}

private fun showOppo(): Boolean {
    val intent = js("new android.content.Intent()")
    intent.putExtra("packageName", js("ctx").getPackageName())
    intent.setAction("com.oppo.safe")
    intent.setClassName("com.oppo.safe", "com.oppo.safe.permission.floatwindow.FloatWindowListActivity")
    if (startSafely(intent)) return true
    intent.setAction("com.color.safecenter")
    intent.setClassName("com.color.safecenter", "com.color.safecenter.permission.floatwindow.FloatWindowListActivity")
    if (startSafely(intent)) return true
    intent.setAction("com.coloros.safecenter")
    intent.setClassName("com.coloros.safecenter", "com.coloros.safecenter.sysfloatwindow.FloatWindowListActivity")
    return startSafely(intent)
}

private fun showVivo(): Boolean {
    val intent = js("new android.content.Intent('com.iqoo.secure')")
    intent.setClassName("com.iqoo.secure", "com.iqoo.secure.MainActivity")
    return startSafely(intent)
}

private fun showSmartisan(): Boolean {
    if (settingsApi.SYSVER >= 23) return false
    if (settingsApi.SYSVER >= 21) {
        val intent = js("new android.content.Intent('com.smartisanos.security.action.SWITCHED_PERMISSIONS_NEW')")
        intent.setClassName("com.smartisanos.security", "com.smartisanos.security.SwitchedPermissions")
        intent.putExtra("index", 17)
        return startSafely(intent)
    }
    val intent = js("new android.content.Intent('com.smartisanos.security.action.SWITCHED_PERMISSIONS')")
    intent.setClassName("com.smartisanos.security", "com.smartisanos.security.SwitchedPermissions")
    val b = js("new android.os.Bundle()")
    b.putStringArray("permission", js("([android.Manifest.permission.SYSTEM_ALERT_WINDOW])"))
    intent.putExtras(b)
    return startSafely(intent)
}

private fun showFlyme(): Boolean {
    val intent = js("new android.content.Intent('com.meizu.safe.security.SHOW_APPSEC')")
    intent.setClassName("com.meizu.safe", "com.meizu.safe.security.AppSecActivity")
    intent.putExtra("packageName", js("ctx").getPackageName())
    return startSafely(intent)
}

private fun show360(): Boolean {
    val intent = js("new android.content.Intent()")
    intent.setClassName("com.android.settings", "com.android.settings.Settings\$OverlaySettingsActivity")
    if (startSafely(intent)) return true
    intent.setClassName("com.qihoo360.mobilesafe", "com.qihoo360.mobilesafe.ui.index.AppEnterActivity")
    return startSafely(intent)
}

fun installSettingsCompat() {
    settingsApi.SYSVER = js("android.os.Build.VERSION.SDK_INT")
    settingsApi.ensureCanFloat = { silent: dynamic ->
        var ok = canDrawOverlays() || setMode(js("ctx"), 24, true)
        if (!ok && !silent) {
            try {
                js("G").ui({
                    try { js("G").Toast.makeText(js("ctx"), "系统不允许命令助手显示悬浮窗，请在设置中启用", 1).show() } catch (e: dynamic) { js("erp")(e) }
                })
            } catch (e: dynamic) { }
            manageDrawOverlays()
        }
        ok
    }
    settingsApi.showAppSettings = { showAppSettings() }
    settingsApi.canDrawOverlays = { canDrawOverlays() }
    settingsApi.setDrawOverlays = { allowed: dynamic -> setMode(js("ctx"), 24, allowed) }
    settingsApi.checkOp = { context: dynamic, op: dynamic -> checkOp(context, op) }
    settingsApi.setMode = { context: dynamic, op: dynamic, allowed: dynamic -> setMode(context, op, allowed) }
    settingsApi.manageDrawOverlays = { manageDrawOverlays() }
    settingsApi.manageDrawOverlaysForRom = { manageDrawOverlaysForRom() }
    settingsApi.startSafely = { intent: dynamic -> startSafely(intent) }
    settingsApi.getProp = { key: dynamic -> getProp(key.toString()) }

    val sm = js("({})")
    sm.MIUI = { showMiui() }
    sm.EMUI = { showEmui() }
    sm.OPPO = { showOppo() }
    sm.VIVO = { showVivo() }
    sm.SMARTISAN = { showSmartisan() }
    sm.FLYME = { showFlyme() }
    sm.QIKU = { show360() }
    sm["360"] = { show360() }
    settingsApi.ShowManager = sm

    val rc = js("({})")
    rc.MIUI = { getProp("ro.miui.ui.version.name") }
    rc.EMUI = { getProp("ro.build.version.emui") }
    rc.OPPO = { getProp("ro.build.version.opporom") }
    rc.VIVO = { getProp("ro.vivo.os.version") }
    rc.SMARTISAN = { getProp("ro.smartisan.version") }
    rc.FLYME = {
        val r = js("android.os.Build.DISPLAY")
        if (r.contains("FLYME")) r else null
    }
    settingsApi.RomCheck = rc

    settingsApi.onCreate = {
        settingsApi.rom = js("android.os.Build.MANUFACTURER").toUpperCase()
        settingsApi.version = "unknown"
        val keys = js("Object").keys(settingsApi.RomCheck)
        var i = 0
        var result: dynamic = null
        val keyCount = keys.length as Int
        while (i < keyCount) {
            val k = keys[i]
            val t = settingsApi.RomCheck[k].call(settingsApi)
            if (t != null) {
                result = js("({})")
                result.rom = k
                result.version = t
                break
            }
            i++
        }
        if (result != null) {
            settingsApi.rom = result.rom
            settingsApi.version = result.version
        }
    }

    if (js("typeof MapScript !== 'undefined'")) MapScript.loadModule("SettingsCompat", settingsApi)
}
