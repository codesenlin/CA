package ca.kotlin

/**
 * modules/NeteaseAdapter.js 的 Kotlin/JS 版本。
 *
 * 保留原模块的 API、包名表与 UI 回调流程：
 *   - onCreate 依旧把 getMinecraftVersion 注册成全局函数；
 *   - getCoreVersion 会扫描 packNames、按发行商取版本并排序；
 *   - 网易包的 versionCode → Minecraft 版本表原样保留；
 *   - askPackage / askPublisher / askCustomVersion / switchVersion 的对话框回调保持原结构。
 *
 * Java 互操作集中在 JS helper 里，Kotlin 侧只传参数，不把局部变量写进 js("...")。
 */
private val neteaseApi: dynamic = js("({})")

// ---------------- JS 语义 helper ----------------
private val neTruthyJs: dynamic = js("function (v) { return !!v; }")
private val neStringJs: dynamic = js("function (v) { return String(v); }")
private val neLengthJs: dynamic = js("function (v) { return v.length | 0; }")
private fun neLength(v: dynamic): Int = neLengthJs(v)
private val neInJs: dynamic = js("function (o, k) { return k in o; }")
private val neSplitJs: dynamic = js("function (s, sep) { return String(s).split(sep); }")
private val neAppendTextJs: dynamic = js("function (t, s) { t.text = t.text + s; }")

// ---------------- Java / Android helper ----------------
private val neInstalledPackagesJs: dynamic = js("function (flags) { return ctx.getPackageManager().getInstalledPackages(flags); }")
private val neGetActivitiesJs: dynamic = js("function (pkg) { return ctx.getPackageManager().getPackageInfo(pkg, 1).activities; }")
private val neApplicationLabelJs: dynamic = js("function (info) { return ctx.getPackageManager().getApplicationLabel(info); }")
private val nePackageInfoJs: dynamic = js("function (pkg, flags) { return ctx.getPackageManager().getPackageInfo(pkg, flags); }")
private val neGlobalGetMinecraftVersionJs: dynamic = js("function () { return getMinecraftVersion(); }")

private val nePackagesJs: dynamic = js("""({
    "com.mojang.minecraftpe": { desc: "国际版", publisher: "Mojang" },
    "com.netease.x19": { desc: "网易-官方版", publisher: "Netease" },
    "com.netease.mc.aligames": { desc: "网易-阿里游戏版", publisher: "Netease" },
    "com.netease.mc.bili": { desc: "网易-Bilibili游戏版", publisher: "Netease" },
    "com.netease.mc.baidu": { desc: "网易-百度手机助手版", publisher: "Netease" },
    "com.tencent.tmgp.wdsj666": { desc: "网易-腾讯应用宝版", publisher: "Netease" },
    "com.netease.mc.m4399": { desc: "网易-4399游戏盒版", publisher: "Netease" },
    "com.netease.mc.wdsj.yyxx.yyh": { desc: "网易-应用汇版", publisher: "Netease" },
    "com.netease.mc.qihoo": { desc: "网易-360手机助手版", publisher: "Netease" },
    "com.netease.wdsj.yyxx.mzw": { desc: "网易-拇指玩版", publisher: "Netease" },
    "com.netease.wdsj.yyxx.downjoy": { desc: "网易-当乐版", publisher: "Netease" },
    "com.netease.wdsj.yyxx.sougou": { desc: "网易-搜狗应用商店版", publisher: "Netease" },
    "com.netease.mc.mi": { desc: "网易-小米应用商店版", publisher: "Netease" },
    "com.netease.mc.huawei": { desc: "网易-华为应用商店版", publisher: "Netease" },
    "com.netease.mc.vivo": { desc: "网易-vivo应用商店版", publisher: "Netease" },
    "com.netease.mc.nearme.gamecenter": { desc: "网易-OPPO应用商店版", publisher: "Netease" },
    "com.netease.mc.lenovo": { desc: "网易-乐商店版", publisher: "Netease" },
    "com.netease.mc.coolpad": { desc: "网易-酷派应用商店版", publisher: "Netease" },
    "com.netease.mc.am": { desc: "网易-金立应用商店版", publisher: "Netease" },
    "com.netease.mctest": { desc: "网易-测试版", publisher: "Netease" },
    "com.zhekasmirnov.innercore": { desc: "Inner Core", publisher: "innercore" }
})""")

// ---------------- 版本解析 ----------------
private fun neGetMinecraftVersion(force: dynamic): dynamic {
    if (!neTruthyJs(force) && neTruthyJs(neteaseApi.mcVersion)) return neteaseApi.mcVersion
    try {
        neteaseApi.mcVersion = neGetCoreVersion()
        return neteaseApi.mcVersion
    } catch (e: dynamic) {
        js("Log").e(e)
        neteaseApi.mcVersion = "*"
        return neteaseApi.mcVersion
    }
}

private fun neGetCoreVersion(): dynamic {
    if (MapScript.host == "BlockLauncher") return js("ModPE").getMinecraftVersion()
    if (neTruthyJs(CA.settings.mcPublisher) && neTruthyJs(CA.settings.mcPackName)) {
        neteaseApi.multiVersions = false
        neteaseApi.mcPackage = CA.settings.mcPackName
        neteaseApi.mcPublisher = CA.settings.mcPublisher
        neteaseApi.autoSelect = false
        try {
            return neGetVersionByPar(CA.settings.mcPackName, CA.settings.mcPublisher)
        } catch (e: dynamic) {
            js("erp")(e, true)
        }
        CA.settings.mcPackName = null
        CA.settings.mcPublisher = null
    }
    val result = js("[]")
    val packNames: dynamic = neteaseApi.packNames
    val n = neLength(packNames)
    var i = 0
    while (i < n) {
        val packName: dynamic = packNames[i]
        if (neTruthyJs(js("MCAdapter").existPackage(packName))) {
            val t = js("({})")
            t["package"] = packName
            t.publisher = neteaseApi.packages[packName].publisher
            t.version = neSplitJs(neGetVersionByPar(t["package"], t.publisher), ".")
            result.push(t)
        }
        i++
    }
    if (neLength(result) > 1) {
        result.sort({ a: dynamic, b: dynamic -> neteaseApi.compareVersion(b.version, a.version) })
    }
    neteaseApi.multiVersions = neLength(result) > 1
    neteaseApi.autoSelect = true
    if (neLength(result) > 0) {
        neteaseApi.mcPackage = result[0]["package"]
        neteaseApi.mcPublisher = result[0].publisher
        return result[0].version.join(".")
    }
    neteaseApi.mcPackage = null
    neteaseApi.mcPublisher = null
    return "*"
}

private fun neGetVersionByPar(packName: dynamic, publisher: dynamic): dynamic {
    if (publisher === "Mojang") return neGetMojangVersion(packName)
    if (publisher === "Netease") return neGetNeteaseVersion(packName)
    if (publisher === "Custom") return packName
    return "*"
}

private fun neGetMojangVersion(packageName: dynamic): dynamic =
    neStringJs(nePackageInfoJs(packageName, 0).versionName)

private fun neGetNeteaseVersion(packageName: dynamic): dynamic {
    val c: dynamic = nePackageInfoJs(packageName, 0).versionCode
    neteaseApi.supportWS = c >= 840035545 && c < 840094571
    if (c >= 840227362) return "1.18.32.0.0" // 2.7.5.227362
    if (c >= 840223715) return "1.18.31.0.0" // 2.6.5.223715
    if (c >= 840217492) return "1.18.30.0.0" // 2.5.5.217492
    if (c >= 840213405) return "1.18.2.0.0" // 2.4.5.213405
    if (c >= 840209578) return "1.18.1.0.0" // 2.3.15.209578
    if (c >= 840204111) return "1.18.0.0.0" // 2.2.15.204111
    if (c >= 840162567) return "1.17.3.0.0" // 2.1.5.162567
    if (c >= 840153450) return "1.17.2.0.0" // 2.0.0.153450
    if (c >= 840146956) return "1.16.203.2.0" // 1.25.5.146956
    if (c >= 840141220) return "1.16.202.2.0" // 1.24.5.141220
    if (c >= 840129766) return "1.16.201.2.0" // 1.23.5.129766
    if (c >= 840122057) return "1.16.12.2.0" // 1.22.10.122057
    if (c >= 840115731) return "1.16.10.2.0" // 1.21.5.115731
    if (c >= 840109731) return "1.14.32.0.0" // 1.20.5.109731
    if (c >= 840105182) return "1.14.31.0.0" // 1.19.10.105182
    if (c >= 840099153) return "1.14.30.0.0" // 1.18.10.99153
    if (c >= 840091142) return "1.13.4.0.0" // 1.17.5.91142
    if (c >= 840084547) return "1.13.3.0.0" // 1.16.5.84547
    if (c >= 840075495) return "1.12.0.28.1" // 1.15.0.75495
    if (c >= 840068012) return "1.11.4.2" // 1.14.0.68012
    if (c >= 840064213) return "1.9.1.15" // 1.13.0.64213
    if (c >= 840060355) return "1.9.0.15" // 1.12.4.60355
    if (c >= 840055312) return "1.8.1.1" // 1.11.0.55312
    if (c >= 840052467) return "1.7.0.13" // 1.10.0.52467
    if (c >= 840047903) return "1.6.2.0" // 1.8.0.47903
    if (c >= 840045722) return "1.5.2.0" // 1.7.0.45722
    if (c >= 840043535) return "1.4.1.5" // 1.6.1.43535
    if (c >= 840035545) return "1.2.5.50" // 1.0.0.35545
    return "1.1.3.52" // 未确认
}

// ---------------- 版本选择 UI ----------------
private fun neAskPackage(callback: dynamic, canCustomize: dynamic) {
    js("Common").showProgressDialog({ o: dynamic -> neShowPackageList(o, callback, canCustomize) }, true)
}

private fun neShowPackageList(o: dynamic, callback: dynamic, canCustomize: dynamic) {
    o.setText("正在加载列表……")
    val lp: dynamic = neInstalledPackagesJs(0).toArray()
    val r = js("[]")
    val n = neLength(lp)
    var i = 0
    while (i < n) {
        val app: dynamic = lp[i]
        if (neTruthyJs(app.applicationInfo)) {
            var usable = true
            try {
                // 非常神奇的 Exception: Package manager has died
                val activities: dynamic = neGetActivitiesJs(app.packageName)
                if (neTruthyJs(activities)) {
                    val an = neLength(activities)
                    var j = 0
                    while (j < an) {
                        if (activities[j].name == "com.mojang.minecraftpe.MainActivity") {
                            usable = false
                            break
                        }
                        j++
                    }
                }
                if (usable) {
                    i++
                    continue
                }
            } catch (e: dynamic) {
                js("Log").e(e)
            }
            val t = js("({})")
            t.text = neApplicationLabelJs(app.applicationInfo)
            t.result = app.packageName
            if (neTruthyJs(neInJs(neteaseApi.packages, t.result))) {
                t.description = neStringJs(neteaseApi.packages[t.result].desc) + " - " + neStringJs(app.versionName)
                t.publisher = neteaseApi.packages[t.result].publisher
            } else {
                t.description = "未知的版本:" + neStringJs(app.packageName) + " - " + neStringJs(app.versionName)
            }
            if (neTruthyJs(canCustomize) && neteaseApi.mcPackage == t.result) {
                neAppendTextJs(t, if (neTruthyJs(neteaseApi.autoSelect)) " (自动选择)" else " (当前选择)")
            }
            r.push(t)
        }
        i++
    }
    if (neTruthyJs(canCustomize)) {
        val auto = js("({})")
        auto.text = "自动选择"
        auto.description = if (neTruthyJs(neteaseApi.autoSelect)) "启用中" else "未启用"
        auto.auto = true
        r.unshift(auto)
        val custom = js("({})")
        custom.text = "自定义"
        custom.description = if (neteaseApi.mcPublisher == "Custom") "正在使用版本: " + neStringJs(neteaseApi.mcVersion) else "未启用"
        custom.custom = true
        r.push(custom)
    }
    if (neTruthyJs(o.cancelled)) return
    if (neLength(r) > 0) {
        js("Common").showListChooser(r, { id: dynamic -> neOnPackageChosen(r, id, callback) })
    } else {
        js("Common").toast("找不到可用的Minecraft版本")
    }
}

private fun neOnPackageChosen(r: dynamic, id: dynamic, callback: dynamic) {
    val res: dynamic = r[id]
    if (neTruthyJs(res.auto)) {
        callback(null, null)
    } else if (neTruthyJs(res.custom)) {
        neAskCustomVersion({ v: dynamic -> callback(v, "Custom") })
    } else if (neTruthyJs(res.publisher)) {
        callback(neStringJs(res.result), res.publisher)
    } else {
        neAskPublisher({ pub: dynamic -> callback(neStringJs(res.result), pub) })
        js("Common").toast("请选择对应的发行商")
    }
}

private fun neAskPublisher(callback: dynamic) {
    val r = js("[]")
    val m = js("({})")
    m.text = "Minecraft"
    m.description = "国际版"
    m.result = "Mojang"
    r.push(m)
    val w = js("({})")
    w.text = "我的世界"
    w.description = "网易版"
    w.result = "Netease"
    r.push(w)
    val o = js("({})")
    o.text = "其他版本"
    o.result = "unknown"
    r.push(o)
    js("Common").showListChooser(r, { id: dynamic -> callback(r[id].result) })
}

private fun neSwitchVersion(callback: dynamic) {
    if (MapScript.host == "BlockLauncher") {
        js("Common").toast("您正在使用启动器加载本JS，因此不能切换版本")
        return
    }
    neAskPackage({ name: dynamic, publisher: dynamic ->
        CA.settings.mcPackName = name
        CA.settings.mcPublisher = publisher
        neteaseApi.mcVersion = null
        callback()
    }, true)
}

private fun neAskCustomVersion(callback: dynamic) {
    val z = js("({})")
    z.title = "自定义版本"
    z.callback = { s: dynamic -> callback(s) }
    z.singleLine = true
    z.defaultValue = neGlobalGetMinecraftVersionJs()
    js("Common").showInputDialog(z)
}

private fun neCompareVersion(a: dynamic, b: dynamic): dynamic = js("Accel").compareVersionArray(a, b)

// ---------------- 安装 ----------------
fun installNeteaseAdapter() {
    neteaseApi.packNames = Loader.fromFile("../../../data/packnames.json")
    neteaseApi.packages = nePackagesJs

    neteaseApi.onCreate = {
        MapScript.loadModule("getMinecraftVersion", neteaseApi.getMinecraftVersion)
    }
    neteaseApi.getMinecraftVersion = { force: dynamic -> neGetMinecraftVersion(force) }
    neteaseApi.getCoreVersion = { neGetCoreVersion() }
    neteaseApi.getVersionByPar = { packName: dynamic, publisher: dynamic -> neGetVersionByPar(packName, publisher) }
    neteaseApi.getMojangVersion = { packageName: dynamic -> neGetMojangVersion(packageName) }
    neteaseApi.getNeteaseVersion = { packageName: dynamic -> neGetNeteaseVersion(packageName) }
    neteaseApi.askPackage = { callback: dynamic, canCustomize: dynamic -> neAskPackage(callback, canCustomize) }
    neteaseApi.askPublisher = { callback: dynamic -> neAskPublisher(callback) }
    neteaseApi.switchVersion = { callback: dynamic -> neSwitchVersion(callback) }
    neteaseApi.askCustomVersion = { callback: dynamic -> neAskCustomVersion(callback) }
    neteaseApi.compareVersion = { a: dynamic, b: dynamic -> neCompareVersion(a, b) }

    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("NeteaseAdapter", neteaseApi)
    }
}
