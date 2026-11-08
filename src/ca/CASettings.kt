package ca.kotlin

/**
 * modules/CA.js settings 组的 Kotlin/JS 版本（原文件约 2707-4375 行，以成员名为准）。
 *
 * 只提供 installCaSettings(api: dynamic)：把本组的 15 个顶层成员挂到调用方传入的
 * CA 共享 API 上；不注册模块、不定义 caApi、也不调用安装函数。
 *
 * 原实现里 showSettings / showFCS / showCustomExpEdit / showPaste 都是具名函数
 * 表达式 `function self(...)`，把 root / appearance / prompt / linear / bar 等
 * 一次性构建结果缓存在函数对象自身。这里用薄 JS wrapper 生成同名的
 * `function self(...)`，把真实 self 传回 Kotlin；缓存仍挂在同一个函数对象上，
 * 跨次调用与 CA.showSettings.xxx / CA.showPaste.xxx 的外部访问语义不变。
 *
 * 所有原来是 `this` 的引用改成显式 api；跨组字段/方法（showMain、showIcon、
 * Library、PluginExpression、his/fav/tips、con/cmd/fcs/paste 等）统一走
 * api.xxx 动态调用，本文件不定义它们。
 *
 * 原 JS 的已知行为/毛病按原样保留，并在对应位置注释：
 *   - 设置项里的 get/set/current/hidden/onclick 依赖调用方绑定的 this
 *     （this.id / this.values / this.list / this.intent / this.path / this.view），
 *     统一用保留动态 this 的薄 wrapper；
 *   - histroyCount 拼写错误（与 core 组一致）；
 *   - 图标大小的 get 越界时返回下标 3，历史容量越界时返回 200（而不是列表末项）；
 *   - showCustomExpEdit 的删除按钮把 TextView 赋给意外的全局 del（这里收成局部变量，
 *     不改变可见行为）；
 *   - showPaste 的 ACTION_UP 未 break 时会继续落入 ACTION_CANCEL 的收尾逻辑。
 */

// ---------------- JS 语义 helper ----------------

private val casTruthyJs: dynamic = js("function (v) { return !!v; }")
private fun casTruthy(v: dynamic): Boolean = casTruthyJs(v)

private val casBoolJs: dynamic = js("function (v) { return Boolean(v); }")
private fun casBool(v: dynamic): Boolean = casBoolJs(v)

private val casStringJs: dynamic = js("function (v) { return String(v); }")
private fun casString(v: dynamic): dynamic = casStringJs(v)

private val casAddJs: dynamic = js("function (a, b) { return a + b; }")
private fun casAdd(a: dynamic, b: dynamic): dynamic = casAddJs(a, b)

private val casSubJs: dynamic = js("function (a, b) { return a - b; }")
private fun casSub(a: dynamic, b: dynamic): dynamic = casSubJs(a, b)

private val casMulJs: dynamic = js("function (a, b) { return a * b; }")
private fun casMul(a: dynamic, b: dynamic): dynamic = casMulJs(a, b)

private val casDivJs: dynamic = js("function (a, b) { return a / b; }")
private fun casDiv(a: dynamic, b: dynamic): dynamic = casDivJs(a, b)

private val casNegJs: dynamic = js("function (v) { return -v; }")
private fun casNeg(v: dynamic): dynamic = casNegJs(v)

private val casAbsJs: dynamic = js("function (v) { return Math.abs(v); }")
private fun casAbs(v: dynamic): dynamic = casAbsJs(v)

private val casBitOrJs: dynamic = js("function (a, b) { return a | b; }")
private fun casBitOr(a: dynamic, b: dynamic): dynamic = casBitOrJs(a, b)

// 所有 length 统一走 `| 0`，兼容 Rhino 的 Java 数组/包装对象。
private val casLengthJs: dynamic = js("function (v) { return v.length | 0; }")
private fun casLength(v: dynamic): Int = casLengthJs(v)

private val casLooseEqJs: dynamic = js("function (a, b) { return a == b; }")
private fun casLooseEq(a: dynamic, b: dynamic): Boolean = casLooseEqJs(a, b)

private val casLooseNeJs: dynamic = js("function (a, b) { return a != b; }")
private fun casLooseNe(a: dynamic, b: dynamic): Boolean = casLooseNeJs(a, b)

private val casLtJs: dynamic = js("function (a, b) { return a < b; }")
private fun casLt(a: dynamic, b: dynamic): Boolean = casLtJs(a, b)

private val casGtJs: dynamic = js("function (a, b) { return a > b; }")
private fun casGt(a: dynamic, b: dynamic): Boolean = casGtJs(a, b)

private val casLeJs: dynamic = js("function (a, b) { return a <= b; }")
private fun casLe(a: dynamic, b: dynamic): Boolean = casLeJs(a, b)

private val casGeJs: dynamic = js("function (a, b) { return a >= b; }")
private fun casGe(a: dynamic, b: dynamic): Boolean = casGeJs(a, b)

private val casInJs: dynamic = js("function (k, o) { return k in o; }")
private fun casIn(k: dynamic, o: dynamic): Boolean = casInJs(k, o)

private val casIsArrayJs: dynamic = js("function (v) { return Array.isArray(v); }")
private fun casIsArray(v: dynamic): Boolean = casIsArrayJs(v)

private val casIsNaNRawJs: dynamic = js("function (v) { return isNaN(v); }")
private fun casIsNaN(v: dynamic): Boolean = casIsNaNRawJs(v)

private val casParseIntJs: dynamic = js("function (v) { return parseInt(v); }")
private fun casParseInt(v: dynamic): dynamic = casParseIntJs(v)

private val casDpJs: dynamic = js("function (n) { return n * G.dp; }")
private fun casDp(n: Int): dynamic = casDpJs(n)

private val casOrJs: dynamic = js("function (a, b) { return a || b; }")
private fun casOr(a: dynamic, b: dynamic): dynamic = casOrJs(a, b)

private val casThrowJs: dynamic = js("function (m) { throw m; }")
private fun casThrow(m: dynamic) {
    casThrowJs(m)
}

/** 保留回调里的真实 this（设置项的 get/set/current/hidden/onclick 依赖它）。 */
private val casThis0Js: dynamic = js("function (f) { return function () { return f(this); }; }")
private val casThis1Js: dynamic = js("function (f) { return function (a) { return f(this, a); }; }")

/** 生成具名函数表达式 `function self(...)`，与旧 API 的 arity/name 对齐。 */
private val casSelf0Js: dynamic = js("function (f) { return function self() { f(self); }; }")
private val casSelf1Js: dynamic = js("function (f) { return function self(o) { f(self, o); }; }")

/** 丢弃 Kotlin Unit 返回值，保持旧 API 返回 undefined。 */
private val casVoid0Js: dynamic = js("function (f) { return function () { f(); }; }")
private val casVoid1Js: dynamic = js("function (f) { return function (a) { f(a); }; }")

/** function touch(v, e) { ... }：状态挂在 OnTouchListener 的函数对象上。 */
private val casTouchFactoryJs: dynamic = js(
    "function (impl) { var self = function touch(v, e) { return impl(self, v, e); }; return self; }"
)

private val casUrlObjJs: dynamic = js("function (u) { return { url: u }; }")
private fun casUrlObj(u: dynamic): dynamic = casUrlObjJs(u)

private fun casObj(): dynamic = js("({})")

private fun casArray(vararg items: dynamic): dynamic {
    val a = js("[]")
    var i = 0
    while (i < items.size) {
        a.push(items[i])
        i++
    }
    return a
}

// ---------------- Android UI helper ----------------

private val casNewLinearLayoutJs: dynamic = js("function () { return new G.LinearLayout(ctx); }")
private val casNewFrameLayoutJs: dynamic = js("function () { return new G.FrameLayout(ctx); }")
private val casNewTextViewJs: dynamic = js("function () { return new G.TextView(ctx); }")
private val casNewImageViewJs: dynamic = js("function () { return new G.ImageView(ctx); }")
private val casNewListViewJs: dynamic = js("function () { return new G.ListView(ctx); }")
private val casNewScrollViewJs: dynamic = js("function () { return new G.ScrollView(ctx); }")
private val casNewLinearParams2Js: dynamic = js(
    "function (w, h) { return new G.LinearLayout.LayoutParams(w, h); }"
)
private val casNewLinearParams3Js: dynamic = js(
    "function (w, h, weight) { return new G.LinearLayout.LayoutParams(w, h, weight); }"
)
private val casNewFrameParams2Js: dynamic = js(
    "function (w, h) { return new G.FrameLayout.LayoutParams(w, h); }"
)
private val casNewFrameParams3Js: dynamic = js(
    "function (w, h, gravity) { return new G.FrameLayout.LayoutParams(w, h, gravity); }"
)
private val casNewAbsParamsJs: dynamic = js(
    "function (w, h) { return new G.AbsListView.LayoutParams(w, h); }"
)
private val casNewTranslate4Js: dynamic = js(
    "function (a, b, c, d) { return new G.TranslateAnimation(a, b, c, d); }"
)
private val casNewDecelerateJs: dynamic = js(
    "function (v) { return new G.DecelerateInterpolator(v); }"
)
private val casNewAccelerateJs: dynamic = js(
    "function (v) { return new G.AccelerateInterpolator(v); }"
)
private val casNewClickListenerJs: dynamic = js(
    "function (f) { return new G.View.OnClickListener({ onClick: f }); }"
)
private val casNewLongClickListenerJs: dynamic = js(
    "function (f) { return new G.View.OnLongClickListener({ onLongClick: f }); }"
)
private val casNewTouchListenerJs: dynamic = js(
    "function (f) { return new G.View.OnTouchListener({ onTouch: f }); }"
)
private val casNewItemClickListenerJs: dynamic = js(
    "function (f) { return new G.AdapterView.OnItemClickListener({ onItemClick: f }); }"
)
private val casNewItemLongClickListenerJs: dynamic = js(
    "function (f) { return new G.AdapterView.OnItemLongClickListener({ onItemLongClick: f }); }"
)
private val casNewAnimationListenerJs: dynamic = js(
    "function (f) { return new G.Animation.AnimationListener({ onAnimationEnd: f }); }"
)
private val casNewPopupPageJs: dynamic = js(
    "function (v, n) { return new PopupPage(v, n); }"
)
private val casNewPopupWindowJs: dynamic = js(
    "function (v, n) { return new PopupWindow(v, n); }"
)
private val casNewSimpleListAdapterJs: dynamic = js(
    "function (a, m, b) { return new SimpleListAdapter(a, m, b); }"
)
private val casSimpleGetControllerJs: dynamic = js(
    "function (a) { return SimpleListAdapter.getController(a); }"
)
private val casNewFile1Js: dynamic = js("function (p) { return new java.io.File(p); }")
private val casNewFile2Js: dynamic = js("function (p, n) { return new java.io.File(p, n); }")
private val casNewBundleJs: dynamic = js("function () { return new android.os.Bundle(); }")
private val casNewViewIntentJs: dynamic = js(
    "function () { return new android.content.Intent(android.content.Intent.ACTION_VIEW); }"
)

private fun casNewLinearLayout(): dynamic = casNewLinearLayoutJs()
private fun casNewFrameLayout(): dynamic = casNewFrameLayoutJs()
private fun casNewTextView(): dynamic = casNewTextViewJs()
private fun casNewImageView(): dynamic = casNewImageViewJs()
private fun casNewListView(): dynamic = casNewListViewJs()
private fun casNewScrollView(): dynamic = casNewScrollViewJs()
private fun casNewLinearParams2(w: dynamic, h: dynamic): dynamic = casNewLinearParams2Js(w, h)
private fun casNewLinearParams3(w: dynamic, h: dynamic, weight: dynamic): dynamic =
    casNewLinearParams3Js(w, h, weight)
private fun casNewFrameParams2(w: dynamic, h: dynamic): dynamic = casNewFrameParams2Js(w, h)
private fun casNewFrameParams3(w: dynamic, h: dynamic, gravity: dynamic): dynamic =
    casNewFrameParams3Js(w, h, gravity)
private fun casNewAbsParams(w: dynamic, h: dynamic): dynamic = casNewAbsParamsJs(w, h)
private fun casNewTranslate4(a: dynamic, b: dynamic, c: dynamic, d: dynamic): dynamic =
    casNewTranslate4Js(a, b, c, d)
private fun casNewDecelerate(v: dynamic): dynamic = casNewDecelerateJs(v)
private fun casNewAccelerate(v: dynamic): dynamic = casNewAccelerateJs(v)
private fun casNewClickListener(f: dynamic): dynamic = casNewClickListenerJs(f)
private fun casNewLongClickListener(f: dynamic): dynamic = casNewLongClickListenerJs(f)
private fun casNewTouchListener(f: dynamic): dynamic = casNewTouchListenerJs(f)
private fun casNewItemClickListener(f: dynamic): dynamic = casNewItemClickListenerJs(f)
private fun casNewItemLongClickListener(f: dynamic): dynamic = casNewItemLongClickListenerJs(f)
private fun casNewAnimationListener(f: dynamic): dynamic = casNewAnimationListenerJs(f)
private fun casNewPopupPage(v: dynamic, n: dynamic): dynamic = casNewPopupPageJs(v, n)
private fun casNewPopupWindow(v: dynamic, n: dynamic): dynamic = casNewPopupWindowJs(v, n)
private fun casNewSimpleListAdapter(a: dynamic, m: dynamic, b: dynamic): dynamic =
    casNewSimpleListAdapterJs(a, m, b)
private fun casSimpleGetController(a: dynamic): dynamic = casSimpleGetControllerJs(a)
private fun casNewFile1(p: dynamic): dynamic = casNewFile1Js(p)
private fun casNewFile2(p: dynamic, n: dynamic): dynamic = casNewFile2Js(p, n)
private fun casNewBundle(): dynamic = casNewBundleJs()
private fun casNewViewIntent(): dynamic = casNewViewIntentJs()

// ---------------- showSettings ----------------

private fun casShowSettings(api: dynamic, self: dynamic) {
    js("G").ui({
        try {
            if (!casTruthy(self.root)) casBuildSettingsRoot(api, self)
            self.refreshed = false
            js("Common").showSettings("设置", self.root, { ->
                api.trySave()
                js("undefined")
            })
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

private fun casBuildSettingsRoot(api: dynamic, self: dynamic) {
    self.getsettingbool = casThis0Js { item: dynamic ->
        casBool(api.settings[item.id])
    }
    self.setsettingbool = casThis1Js { item: dynamic, v: dynamic ->
        api.settings[item.id] = casBool(v)
        js("undefined")
    }
    self.refresh = { f: dynamic ->
        js("Common").loadTheme(js("Common").theme.id)
        if (casTruthy(self.refreshed)) {
            js("undefined")
        } else {
            self.refreshed = true
            api.resetGUI()
            api.showMain(true)
            if (casTruthy(f)) api.showSettings()
            api.showIcon()
            js("undefined")
        }
    }

    val root = casArray()
    root.push(js("UserManager").getSettingItem())
    root.push(casSpaceItem(12))
    root.push(casNavItem("外观", "主题、悬浮窗、背景图片", { _fset: dynamic ->
        js("Common").showSettings("外观", self.appearance)
        js("undefined")
    }))
    root.push(casNavItem("智能补全", "智能模式、拓展包管理、粘贴模式", { _fset: dynamic ->
        js("Common").showSettings("智能补全", self.intellisense)
        js("undefined")
    }))
    root.push(casNavItem("用户数据", "历史、收藏、自定义短语", { _fset: dynamic ->
        js("Common").showSettings("用户数据", self.userdata)
        js("undefined")
    }))
    val push = casNavItem("推送信息", "推送信息管理与历史信息查看", { _fset: dynamic ->
        js("PushService").showSettings("推送设置")
        js("undefined")
    })
    push.hidden = { -> !casLooseEq(js("MapScript").host, "Android") }
    root.push(push)
    val access = casNavItem("辅助功能", "无障碍服务、WebSocket服务器", { _fset: dynamic ->
        js("AndroidBridge").showSettings("辅助功能设置")
        js("undefined")
    })
    access.hidden = { -> !casLooseEq(js("MapScript").host, "Android") }
    root.push(access)
    root.push(casNavItem("关于命令助手", null, { _fset: dynamic ->
        js("Common").showSettings("关于", self.about)
        js("undefined")
    }))
    self.root = root

    self.about = casBuildSettingsAbout(api)
    self.appearance = casBuildAppearance(api, self)
    self.intellisense = casBuildIntellisense(api, self)
    self.userdata = casBuildUserdata(api, self)
}

private fun casSpaceItem(height: Int): dynamic {
    val o = casObj()
    o.type = "space"
    o.height = casDp(height)
    return o
}

private fun casNavItem(name: dynamic, description: dynamic, onclick: dynamic): dynamic {
    val o = casObj()
    o.name = name
    if (casTruthy(description)) o.description = description
    o.type = "custom"
    o.onclick = onclick
    return o
}

private fun casTagItem(name: dynamic): dynamic {
    val o = casObj()
    o.name = name
    o.type = "tag"
    return o
}

private fun casBuildSettingsAbout(api: dynamic): dynamic {
    val flavorData = casOr(js("BuildConfig").flavorData, casObj())
    val aboutTitle = casOr(flavorData.aboutTitle, "命令助手")
    val aboutText = casOr(flavorData.aboutText, "Developed by ProjectXero")
    val communityVersion = casOr(flavorData.communityVersion, "")
    val communityMaintainer = casOr(flavorData.communityMaintainer, "")
    val communityInfo = if (casIsArray(flavorData.communityInfo)) {
        flavorData.communityInfo
    } else {
        casArray()
    }
    val officialProjectUrl = "https://ca.projectxero.top/"
    val officialAboutUrl = "https://ca.projectxero.top/blog/about/"
    val officialLicenseUrl = "https://ca.projectxero.top/blog/about/license/"
    val officialPrivacyUrl = "https://ca.projectxero.top/blog/about/privacy/"

    val about = casArray()
    if (casLooseEq(js("MapScript").host, "Android")) {
        val item = casObj()
        item.type = "layout"
        item.maker = { _holder: dynamic ->
            casAboutPlatformMaker(aboutTitle, aboutText, api)
        }
        item.onclick = { ->
            js("Updater").showCurrentVersionInfo()
            js("undefined")
        }
        item.onExit = casThis0Js { it: dynamic ->
            it.view = js("null")
            js("undefined")
        }
        about.push(item)
    } else {
        val item = casObj()
        item.name = "版本信息"
        item.type = "custom"
        item.get = { -> api.versionName }
        item.onclick = { ->
            js("Updater").showCurrentVersionInfo()
            js("undefined")
        }
        about.push(item)
    }

    val check = casObj()
    check.name = "检查更新"
    check.type = "custom"
    check.get = { -> js("Updater").getVersionInfo() }
    check.onclick = { fset: dynamic ->
        if (casLooseEq(js("BuildConfig").variants, "release")) {
            if (casLe(js("Updater").updateFlag, 0)) {
                js("Common").toast("目前没有已公开的更新版本哦\n点击下面的“加入交流群”，加入官方交流群然后@作者催更吧")
            } else {
                js("Updater").checkUpdate({ _statusMsg: dynamic ->
                    fset()
                    js("undefined")
                })
            }
        } else {
            if (casLe(js("Updater").updateFlagBeta, 0)) {
                js("Common").toast("目前没有已公开的更新Beta版本哦\n点击下面的“加入交流群”，加入官方交流群然后@作者催更吧")
            } else {
                js("Updater").checkUpdateBeta({ _statusMsg: dynamic ->
                    fset()
                    js("undefined")
                })
            }
        }
        js("undefined")
    }
    about.push(check)

    val share = casObj()
    share.name = "分享软件"
    share.type = "custom"
    share.onclick = { ->
        val t = officialProjectUrl
        try {
            js("AndroidBridge").shareText("Hi，我发现一款很棒的Minecraft辅助软件，命令助手。下载链接：" + t)
        } catch (e: dynamic) {
            js("Common").setClipboardText(t)
            js("Common").toast("下载链接已复制到剪贴板")
        }
        js("undefined")
    }
    about.push(share)

    val feedback = casObj()
    feedback.name = "意见与反馈"
    feedback.type = "custom"
    feedback.onclick = { ->
        js("IssueService").showIssuesWithAgreement()
        js("undefined")
    }
    about.push(feedback)

    val site = casObj()
    site.name = "项目官网"
    site.type = "custom"
    site.onclick = { ->
        try {
            js("AndroidBridge").viewUri(officialProjectUrl)
        } catch (e: dynamic) {
            js("Common").toast("项目官网打开失败")
            js("Log").e(e)
        }
        js("undefined")
    }
    about.push(site)

    val afdian = casObj()
    afdian.name = "爱发电主页"
    afdian.type = "custom"
    afdian.onclick = { ->
        try {
            if (!casTruthy(js("AndroidBridge").viewUri(
                    "afd://app/openapp?params=%7B%22url%22:%22https://ifdian.net/a/projectxero%22,%22type%22:%221%22%7D"
                ))
            ) {
                js("AndroidBridge").viewUri("https://ifdian.net/a/projectxero")
            }
        } catch (e: dynamic) {
            js("Common").toast("爱发电打开失败")
            js("Log").e(e)
        }
        js("undefined")
    }
    about.push(afdian)

    val betaGroup = casObj()
    betaGroup.name = "加入内测群"
    betaGroup.type = "custom"
    betaGroup.onclick = { ->
        try {
            js("AndroidBridge").viewUri("https://projectxero.top/qqgroup/cabeta/")
        } catch (e: dynamic) {
            js("Log").e(e)
        }
        js("undefined")
    }
    about.push(betaGroup)

    val license = casObj()
    license.name = "许可协议"
    license.type = "custom"
    license.onclick = { ->
        js("Common").showWebViewDialog(casUrlObj(officialLicenseUrl))
        js("undefined")
    }
    about.push(license)

    val privacy = casObj()
    privacy.name = "隐私政策"
    privacy.type = "custom"
    privacy.onclick = { ->
        js("Common").showWebViewDialog(casUrlObj(officialPrivacyUrl))
        js("undefined")
    }
    about.push(privacy)

    val aboutEntry = casObj()
    aboutEntry.name = "关于命令助手"
    aboutEntry.type = "custom"
    aboutEntry.onclick = { ->
        try {
            js("AndroidBridge").viewUri(officialAboutUrl)
        } catch (e: dynamic) {
            js("Common").toast("关于页面打开失败")
            js("Log").e(e)
        }
        js("undefined")
    }
    about.push(aboutEntry)

    val autoUpdate = casObj()
    autoUpdate.name = "自动检查更新"
    autoUpdate.type = "boolean"
    autoUpdate.get = { -> !casTruthy(api.settings.skipCheckUpdate) }
    autoUpdate.set = { v: dynamic ->
        api.settings.skipCheckUpdate = !casTruthy(v)
        js("undefined")
    }
    about.push(autoUpdate)

    val diagnostics = casObj()
    diagnostics.name = "自动发送诊断信息"
    diagnostics.description = "信息将用来定位、分析命令助手中的问题，可能包含用户数据"
    diagnostics.type = "boolean"
    diagnostics.get = { -> !casTruthy(js("erp").notReport) }
    diagnostics.set = { vArg: dynamic ->
        var v = vArg
        if (casLooseEq(js("BuildConfig").variants, "snapshot") && !casTruthy(v)) {
            v = true
            js("Common").toast("快照版必须启用此选项")
        } else if (casLooseEq(js("BuildConfig").variants, "debug") && casTruthy(v)) {
            v = false
            js("Common").toast("调试版必须禁用此选项")
        }
        api.settings.notReportError = !casTruthy(v)
        js("erp").notReport = api.settings.notReportError
        js("undefined")
    }
    about.push(diagnostics)

    about.push(casTagItem("开发者工具"))

    val debugAction = casObj()
    debugAction.id = "enableDebugAction"
    debugAction.name = "启用自定义动作"
    debugAction.type = "boolean"
    debugAction.refresh = { ->
        js("DebugUtils").updateDebugAction()
        js("undefined")
    }
    // get/set 必须像旧实现一样在调用时读取 this.id。
    debugAction.get = casThis0Js { it: dynamic ->
        casBool(api.settings[it.id])
    }
    debugAction.set = casThis1Js { it: dynamic, v: dynamic ->
        api.settings[it.id] = casBool(v)
        js("undefined")
    }
    about.push(debugAction)

    val jsonEdit = casObj()
    jsonEdit.name = "JSON编辑器"
    jsonEdit.type = "custom"
    jsonEdit.onclick = { ->
        js("JSONEdit").main()
        js("undefined")
    }
    about.push(jsonEdit)

    val errors = casObj()
    errors.name = "错误记录"
    errors.type = "custom"
    errors.onclick = { ->
        api.manageErrors()
        js("undefined")
    }
    about.push(errors)

    val console = casObj()
    console.name = "控制台"
    console.type = "custom"
    console.onclick = { _fset: dynamic ->
        js("DebugUtils").showDebugDialog()
        js("undefined")
    }
    about.push(console)

    // 官方信息标签固定在最上方；社区风味在官方信息之后追加蓝色标签和条目。
    val officialLabel = casObj()
    officialLabel.type = "layout"
    officialLabel.maker = { ->
        val label = casNewTextView()
        label.setText("官方信息")
        label.setPadding(casDp(20), casDp(20), casDp(20), 0)
        js("Common").applyStyle(label, "textview_highlight", 2)
        label
    }
    about.splice(1, 0, officialLabel)

    if (casLength(communityInfo) > 0) {
        val communityRows = casArray()
        val communityLabel = casObj()
        communityLabel.type = "layout"
        communityLabel.maker = { ->
            val label = casNewTextView()
            label.setText("社区信息")
            label.setPadding(casDp(20), casDp(20), casDp(20), 0)
            // 跟随主题高亮色：白色主题下为蓝字，粉色主题下为粉字
            js("Common").applyStyle(label, "textview_highlight", 2)
            label
        }
        communityRows.push(communityLabel)
        if (casTruthy(communityVersion)) {
            val item = casObj()
            item.name = "社区版本"
            item.type = "custom"
            item.get = { -> communityVersion }
            communityRows.push(item)
        }
        if (casTruthy(communityMaintainer)) {
            val item = casObj()
            item.name = "维护者"
            item.type = "custom"
            item.get = { -> communityMaintainer }
            communityRows.push(item)
        }
        var ci = 0
        val communityCount = casLength(communityInfo)
        while (ci < communityCount) {
            communityRows.push(casCommunityRow(communityInfo[ci]))
            ci++
        }
        // 社区信息放在官方信息之后、通用设置之前。
        var insertAt = -1
        ci = 0
        val aboutCount = casLength(about)
        while (ci < aboutCount) {
            if (casLooseEq(about[ci].name, "自动检查更新")) {
                insertAt = ci
                break
            }
            ci++
        }
        if (insertAt < 0) insertAt = casLength(about)
        ci = casLength(communityRows) - 1
        while (ci >= 0) {
            about.splice(insertAt, 0, communityRows[ci])
            ci--
        }
    }

    // 自动检查更新/诊断信息放在社区信息下方，单独列「通用设置」
    val generalTag = casTagItem("通用设置")
    var generalAt = -1
    var gi = 0
    val generalCount = casLength(about)
    while (gi < generalCount) {
        if (casLooseEq(about[gi].name, "自动检查更新")) {
            generalAt = gi
            break
        }
        gi++
    }
    if (generalAt >= 0) about.splice(generalAt, 0, generalTag)

    return about
}

private fun casAboutPlatformMaker(aboutTitle: dynamic, aboutText: dynamic, api: dynamic): dynamic {
    var linear: dynamic
    var icon: dynamic
    var title: dynamic
    var desp: dynamic
    linear = casNewLinearLayout()
    linear.setOrientation(js("G.LinearLayout").VERTICAL)
    linear.setPadding(0, casDp(20), 0, casDp(20))
    linear.setGravity(js("G.Gravity").CENTER)
    icon = casNewImageView()
    icon.setImageResource(js("com.xero.ca.R.drawable.icon"))
    icon.setLayoutParams(casNewLinearParams2(casDp(80), casDp(80)))
    linear.addView(icon)
    title = casNewTextView()
    title.setPadding(0, casDp(15), 0, 0)
    title.setGravity(js("G.Gravity").CENTER)
    title.setText(casAdd(aboutTitle, casAdd("  ", api.getVersionLabel())))
    title.setLayoutParams(casNewLinearParams2(-1, -2))
    js("Common").applyStyle(title, "textview_default", 4)
    linear.addView(title)
    desp = casNewTextView()
    desp.setGravity(js("G.Gravity").CENTER)
    // 风味可以覆盖关于页副标题（例如社区版归属说明），默认保持作者署名
    desp.setText(aboutText)
    desp.setTypeface(casOr(js("G.Typeface").SERIF, js("G.Typeface").DEFAULT))
    desp.setLayoutParams(casNewLinearParams2(-1, -2))
    js("Common").applyStyle(desp, "textview_prompt", 2)
    linear.addView(desp)
    return linear
}

private fun casCommunityRow(item: dynamic): dynamic {
    val row = casObj()
    row.name = casString(casOr(item.name, "社区信息"))
    row.type = "custom"
    row.onclick = { ->
        try {
            if (casTruthy(item.copy)) {
                js("Common").setClipboardText(casString(item.copy))
                js("Common").toast("已复制到剪贴板")
            }
            if (casTruthy(item.url)) js("AndroidBridge").viewUri(casString(item.url))
        } catch (e: dynamic) {
            js("Log").e(e)
        }
        js("undefined")
    }
    if (casTruthy(item.value)) {
        row.get = { -> item.value }
    } else if (casTruthy(item.description)) {
        row.description = casString(item.description)
    }
    return row
}

// ---------------- 设置子页 ----------------

private fun casTextItem(text: dynamic): dynamic {
    val o = casObj()
    o.text = text
    return o
}

private fun casBoolSettingsItem(id: dynamic, name: dynamic, description: dynamic, self: dynamic): dynamic {
    val o = casObj()
    o.id = id
    o.name = name
    if (casTruthy(description)) o.description = description
    o.type = "boolean"
    o.get = self.getsettingbool
    o.set = self.setsettingbool
    return o
}

private fun casBuildAppearance(api: dynamic, self: dynamic): dynamic {
    val appearance = casArray()

    val theme = casObj()
    theme.name = "界面主题"
    theme.type = "custom"
    theme.get = { -> js("Common").theme.name }
    theme.onclick = { ->
        js("Common").showChangeTheme({ ->
            self.refresh(true)
            js("undefined")
        })
        js("undefined")
    }
    appearance.push(theme)

    appearance.push(casBoolSettingsItem("noAnimation", "关闭动画", "关闭部分动画以减轻卡顿。", self))
    appearance.push(casTagItem("悬浮图标"))

    val iconStyle = casObj()
    iconStyle.name = "图标样式"
    iconStyle.type = "custom"
    iconStyle.get = { -> "点击以修改" }
    iconStyle.onclick = { ->
        api.showIconChooser({ ->
            if (casTruthy(api.showIcon.refresh)) api.showIcon.refreshIcon()
            js("undefined")
        })
        js("undefined")
    }
    appearance.push(iconStyle)

    val iconSize = casObj()
    iconSize.name = "图标大小"
    iconSize.type = "seekbar"
    iconSize.values = casArray(0.25, 0.5, 0.75, 1, 1.25, 1.5, 1.75, 2, 2.5, 3, 4)
    iconSize.current = casThis1Js { it: dynamic, p: dynamic ->
        casAdd(casParseInt(casMul(it.values[p], 100)), "%")
    }
    iconSize.max = 10
    iconSize.get = casThis0Js { it: dynamic ->
        val i = it.values.indexOf(api.settings.iconSize)
        if (casGe(i, 0)) i else 3
    }
    iconSize.set = casThis1Js { it: dynamic, v: dynamic ->
        api.settings.iconSize = it.values[v]
        if (casTruthy(api.showIcon.refresh)) {
            api.showIcon.refreshIcon()
            api.showIcon.refreshPos()
        }
        js("undefined")
    }
    appearance.push(iconSize)

    val alpha = casObj()
    alpha.name = "不透明度"
    alpha.type = "seekbar"
    alpha.current = { p: dynamic -> if (casLooseEq(p, 0)) "自动" else casAdd(p, "0%") }
    alpha.max = 10
    alpha.get = { -> if (casIsNaN(api.settings.iconAlpha)) 0 else api.settings.iconAlpha }
    alpha.set = { v: dynamic ->
        api.settings.iconAlpha = v
        if (casTruthy(api.showIcon.refresh)) api.showIcon.refreshAlpha()
        js("undefined")
    }
    appearance.push(alpha)

    val drag = casObj()
    drag.name = "拖动方式"
    drag.type = "custom"
    drag.list = casArray("自由拖动", "自动贴边", "固定")
    drag.get = casThis0Js { it: dynamic ->
        if (casIn(api.settings.iconDragMode, it.list)) {
            it.list[api.settings.iconDragMode]
        } else {
            api.settings.iconDragMode = 0
            it.list[0]
        }
    }
    drag.onclick = casThis1Js { it: dynamic, fset: dynamic ->
        js("Common").showListChooser(it.list, { i: dynamic ->
            api.settings.iconDragMode = i
            if (casTruthy(api.showIcon.refresh)) api.showIcon.refreshPos()
            fset()
            js("undefined")
        })
        js("undefined")
    }
    appearance.push(drag)

    val autoHide = casBoolSettingsItem("autoHideIcon", "自动隐藏悬浮窗", null, self)
    autoHide.hidden = { -> !casLooseEq(js("MapScript").host, "BlockLauncher") }
    appearance.push(autoHide)

    val quickBar = casObj()
    quickBar.name = "快捷栏动作菜单"
    quickBar.type = "custom"
    quickBar.get = { -> casAdd(casLength(api.settings.quickBarActions), "个动作") }
    quickBar.onclick = { fset: dynamic ->
        api.showActionEdit(api.settings.quickBarActions, fset, api.quickBarDefaultActions)
        js("undefined")
    }
    appearance.push(quickBar)

    appearance.push(casTagItem("悬浮窗"))

    val unfocused = casObj()
    unfocused.name = "不活跃时不透明度"
    unfocused.type = "seekbar"
    unfocused.current = { p: dynamic -> casAdd(p, "%") }
    unfocused.max = 100
    unfocused.get = { ->
        if (casIsNaN(api.settings.unfocusedAlpha)) 70 else casMul(api.settings.unfocusedAlpha, 100)
    }
    unfocused.set = { v: dynamic ->
        val value = casDiv(v, 100)
        api.settings.unfocusedAlpha = value
        js("PopupPage").setAlpha(1, value)
        js("undefined")
    }
    appearance.push(unfocused)

    appearance.push(casTagItem("命令生成器"))

    val bgImage = casObj()
    bgImage.name = "背景图片"
    bgImage.type = "custom"
    bgImage.hidden = { -> casLt(js("android.os.Build.VERSION").SDK_INT, 16) }
    bgImage.get = { -> "点击选择" }
    bgImage.onclick = { ->
        api.showManageBgImage({ ->
            self.refresh(true)
            js("undefined")
        })
        js("undefined")
    }
    appearance.push(bgImage)

    appearance.push(casBoolSettingsItem("keepWhenIME", "禁用压缩列表栏", "当输入法弹出时不再压缩列表栏。", self))

    val split = casBoolSettingsItem("splitScreenMode", "双栏模式", "推荐大屏手机/Pad使用", self)
    split.refresh = self.refresh
    appearance.push(split)

    val longClick = casBoolSettingsItem("openMenuByLongClick", "长按输入栏打开菜单", null, self)
    longClick.refresh = self.refresh
    appearance.push(longClick)

    val clear = casBoolSettingsItem("showClearButton", "显示输入栏右侧的删除图标", null, self)
    clear.refresh = self.refresh
    appearance.push(clear)

    appearance.push(casBoolSettingsItem("noWebImage", "不加载图片", "加载网页时不加载图片", self))

    return appearance
}

private fun casBuildIntellisense(api: dynamic, self: dynamic): dynamic {
    val intellisense = casArray()

    val iiMode = casObj()
    iiMode.name = "智能模式"
    iiMode.type = "custom"
    iiMode.get = { ->
        val t = api.settings.iiMode
        if (casLooseEq(t, 1)) "初学者模式"
        else if (casLooseEq(t, 2)) "专家模式"
        else if (casLooseEq(t, 3)) "自动选择"
        else "关闭"
    }
    iiMode.onclick = { ->
        api.showModeChooser({ ->
            self.refresh(true)
            js("undefined")
        })
        js("undefined")
    }
    intellisense.push(iiMode)

    intellisense.push(casBoolSettingsItem("senseDelay", "启用多线程", "IntelliSense将不会即时输出结果以避免卡顿。", self))
    intellisense.push(casBoolSettingsItem("autoFormatCmd", "启用样式代码显示", "输入框会自动解释输入命令中的样式代码。", self))

    val pasteMode = casObj()
    pasteMode.name = "粘贴模式"
    pasteMode.type = "custom"
    pasteMode.list = casArray(casTextItem("仅复制"), casTextItem("复制并显示粘贴栏"), casTextItem("复制并立即粘贴"))
    pasteMode.get = casThis0Js { it: dynamic ->
        if (casIn(api.settings.pasteMode, it.list)) {
            it.list[api.settings.pasteMode].text
        } else {
            api.settings.pasteMode = 1
            it.list[1].text
        }
    }
    pasteMode.onclick = casThis1Js { it: dynamic, fset: dynamic ->
        js("Common").showListChooser(it.list, { i: dynamic ->
            api.settings.pasteMode = i
            fset()
            js("undefined")
        })
        js("undefined")
    }
    intellisense.push(pasteMode)

    val pasteGravity = casObj()
    pasteGravity.name = "粘贴栏位置"
    pasteGravity.type = "custom"
    pasteGravity.list = casArray(casTextItem("左侧"), casTextItem("右侧"))
    pasteGravity.get = casThis0Js { it: dynamic ->
        if (casIn(api.settings.pasteBarGravity, it.list)) {
            it.list[api.settings.pasteBarGravity].text
        } else {
            api.settings.pasteBarGravity = 0
            it.list[0].text
        }
    }
    pasteGravity.onclick = casThis1Js { it: dynamic, fset: dynamic ->
        js("Common").showListChooser(it.list, { i: dynamic ->
            api.settings.pasteBarGravity = i
            fset()
            api.hidePaste()
            js("undefined")
        })
        js("undefined")
    }
    intellisense.push(pasteGravity)

    val pasteDelay = casObj()
    pasteDelay.name = "粘贴延迟"
    pasteDelay.type = "custom"
    pasteDelay.hidden = { ->
        casLooseEq(js("MapScript").host, "AutoJs") || casLooseEq(js("MapScript").host, "Android")
    }
    pasteDelay.get = { ->
        val v = if (casIsNaN(api.settings.pasteDelay)) 2 else casDiv(api.settings.pasteDelay, 20)
        if (casGt(v, 0)) casAdd(v, "秒") else "无"
    }
    pasteDelay.onclick = { fset: dynamic ->
        api.showPasteDelaySet(fset)
        js("undefined")
    }
    intellisense.push(pasteDelay)

    val overwrite = casBoolSettingsItem(
        "overwriteMCTextbox",
        "替换MC文本框文本",
        "粘贴文本时会自动将文本框中原来的文本替换为新的文本，而不是直接粘贴",
        self
    )
    overwrite.hidden = { ->
        !(casLooseEq(js("MapScript").host, "AutoJs") || casLooseEq(js("MapScript").host, "Android"))
    }
    intellisense.push(overwrite)

    intellisense.push(casTagItem("拓展包"))

    val localLib = casObj()
    localLib.name = "本地拓展包"
    localLib.type = "custom"
    localLib.get = { -> casAdd(casLength(api.settings.enabledLibrarys), "个已启用") }
    localLib.onclick = { fset: dynamic ->
        api.showLibraryMan({ ->
            fset()
            js("undefined")
        })
        js("undefined")
    }
    intellisense.push(localLib)

    val gameVersion = casObj()
    gameVersion.name = "游戏版本"
    gameVersion.description = "命令助手只会解析适合该游戏版本的内容"
    gameVersion.type = "custom"
    gameVersion.get = { -> js("NeteaseAdapter").getMinecraftVersion() }
    gameVersion.onclick = { fset: dynamic ->
        js("NeteaseAdapter").switchVersion({ ->
            val progress = js("Common").showProgressDialog()
            progress.setText("正在重新加载拓展包……")
            api.checkFeatures()
            if (!casTruthy(api.Library.initLibrary({ _fl: dynamic ->
                    js("Common").toast(casAdd(casAdd("版本已切换为", js("getMinecraftVersion")()), "。"))
                    progress.close()
                    fset()
                    js("undefined")
                }))
            ) {
                progress.close()
                js("Common").toast("无法加载拓展包，请稍后重试")
                fset()
            }
            js("undefined")
        })
        js("undefined")
    }
    intellisense.push(gameVersion)

    val autoUpdate = casObj()
    autoUpdate.name = "自动更新"
    autoUpdate.type = "custom"
    autoUpdate.list = casArray(casTextItem("关闭"), casTextItem("检测更新并提示"), casTextItem("检测更新并下载"))
    autoUpdate.get = casThis0Js { it: dynamic ->
        if (casIn(api.settings.libraryAutoUpdate, it.list)) {
            it.list[api.settings.libraryAutoUpdate].text
        } else {
            api.settings.libraryAutoUpdate = 1
            it.list[1].text
        }
    }
    autoUpdate.onclick = casThis1Js { it: dynamic, fset: dynamic ->
        js("Common").showListChooser(it.list, { i: dynamic ->
            api.settings.libraryAutoUpdate = i
            fset()
            js("undefined")
        })
        js("undefined")
    }
    intellisense.push(autoUpdate)

    val source = casObj()
    source.name = "拓展包源"
    source.type = "custom"
    source.get = { ->
        val srcs = api.Library.enabledSources()
        casAdd(casLength(srcs), "个已启用")
    }
    source.onclick = { fset: dynamic ->
        api.showSourceMan()
        if (casTruthy(fset)) fset()
        js("undefined")
    }
    intellisense.push(source)

    val online = casObj()
    online.name = "在线拓展包"
    online.type = "custom"
    online.onclick = { fset: dynamic ->
        if (casLt(api.settings.securityLevel, 4)) {
            api.showOnlineLib(fset)
        } else {
            js("Common").toast("当前安全级别下不加载外部拓展包")
        }
        js("undefined")
    }
    intellisense.push(online)

    return intellisense
}

private fun casBuildUserdata(api: dynamic, self: dynamic): dynamic {
    val userdata = casArray()

    val history = casObj()
    history.name = "管理历史"
    history.type = "custom"
    history.get = { -> casAdd(casAdd("共有", casLength(api.his)), "条记录") }
    history.onclick = { fset: dynamic ->
        api.showHistoryEdit(js("null"), { ->
            fset()
            if (casTruthy(api.history)) api.showHistory()
            js("undefined")
        })
        js("undefined")
    }
    userdata.push(history)

    val favorite = casObj()
    favorite.name = "管理收藏"
    favorite.type = "custom"
    favorite.get = { -> casAdd(casAdd("共有", casLength(api.fav)), "条记录") }
    favorite.onclick = { fset: dynamic ->
        api.showFavoriteEdit(js("null"), { ->
            fset()
            if (casTruthy(api.history)) api.showHistory()
            js("undefined")
        })
        js("undefined")
    }
    userdata.push(favorite)

    val historyCap = casObj()
    historyCap.name = "历史记录容量"
    historyCap.type = "seekbar"
    historyCap.current = casThis1Js { it: dynamic, p: dynamic ->
        if (casLooseEq(p, 0)) "不保存历史" else casAdd(it.list[p], "条")
    }
    historyCap.list = casArray(0, 1, 3, 5, 8, 10, 20, 30, 50, 100, 200)
    historyCap.max = 10
    historyCap.get = casThis0Js { it: dynamic ->
        val k = it.list.indexOf(api.settings.histroyCount)
        if (casLt(k, 0)) 200 else it.list[k]
    }
    historyCap.set = casThis1Js { it: dynamic, v: dynamic ->
        api.settings.histroyCount = casParseInt(it.list[v])
        if (casTruthy(api.settings.histroyCount)) api.his.splice(api.settings.histroyCount)
        js("undefined")
    }
    userdata.push(historyCap)

    val customExp = casObj()
    customExp.name = "管理自定义短语"
    customExp.type = "custom"
    customExp.get = { -> casAdd(casAdd("共有", casLength(api.settings.customExpression)), "条短语") }
    customExp.onclick = { fset: dynamic ->
        api.showCustomExpEdit({ ->
            fset()
            js("undefined")
        })
        js("undefined")
    }
    userdata.push(customExp)

    val tips = casObj()
    tips.name = "每日提示"
    tips.type = "custom"
    tips.get = { -> casAdd(casAdd("共", casLength(api.tips)), "条") }
    tips.onclick = { fset: dynamic ->
        val input = casObj()
        input.title = "每日提示"
        input.defaultValue = api.tips.join("\n")
        input.callback = { s: dynamic ->
            if (!casTruthy(s)) {
                api.tips = api.defalutTips
            } else {
                val parts = s.split("\n")
                api.settings.customTips = parts
                api.tips = parts
            }
            fset()
            js("undefined")
        }
        js("Common").showInputDialog(input)
        js("undefined")
    }
    userdata.push(tips)

    val importData = casObj()
    importData.name = "导入用户数据"
    importData.type = "custom"
    importData.onclick = { ->
        val opts = casObj()
        opts.mimeType = "*/*"
        opts.file = { f: dynamic ->
            try {
                api.importSettings(f)
            } catch (e: dynamic) {
                js("Common").toast(casAdd("导入用户数据失败\n", e))
            }
            js("ExternalStorage").tryReleaseImportFile(f)
            js("undefined")
        }
        js("ExternalStorage").showImportActions(opts)
        js("undefined")
    }
    userdata.push(importData)

    val exportData = casObj()
    exportData.name = "导出用户数据"
    exportData.type = "custom"
    exportData.onclick = { ->
        api.exportSettings()
        js("undefined")
    }
    userdata.push(exportData)

    val importRelease = casObj()
    importRelease.name = "导入正式版数据"
    importRelease.type = "custom"
    importRelease.hidden = { -> casLooseEq(js("BuildConfig").variants, "release") }
    importRelease.onclick = { ->
        val confirm = casObj()
        confirm.title = "确定导入正式版数据？"
        confirm.description = "*此操作无法撤销"
        confirm.callback = { id: dynamic ->
            if (casLooseEq(id, 0)) {
                js("G").ui({ ->
                    try {
                        api.importSettings(casNewFile1(casAdd(js("MapScript").baseDir, "xero_commandassist.dat")))
                    } catch (e: dynamic) {
                        js("erp")(e)
                    }
                    js("undefined")
                })
            }
            js("undefined")
        }
        js("Common").showConfirmDialog(confirm)
        js("undefined")
    }
    userdata.push(importRelease)

    val restore = casObj()
    restore.name = "恢复默认数据"
    restore.type = "custom"
    restore.onclick = { ->
        val confirm = casObj()
        confirm.title = "确定恢复默认？"
        confirm.description = "*此操作无法撤销"
        confirm.callback = { id: dynamic ->
            if (casLooseEq(id, 0)) {
                js("G").ui({ ->
                    try {
                        api.resetGUI()
                        js("SafeFileUtils").delete(casNewFile1(api.profilePath))
                        // 配置已删，readAgreement 归零。这里不能同步等协议：
                        // 设置页还在，弹窗出不来，主线程会卡在 awaitPromise 上。
                        // 先跳过协议完成初始化，再异步弹协议让用户重新同意。
                        api.suppressAgreement = true
                        try {
                            api.initialize()
                        } finally {
                            api.suppressAgreement = false
                        }
                        api.showAgreement({ ->
                            api.trySave()
                            js("undefined")
                        })
                        js("Common").toast("命令助手已重新启动")
                    } catch (e: dynamic) {
                        js("erp")(e)
                    }
                    js("undefined")
                })
            }
            js("undefined")
        }
        js("Common").showConfirmDialog(confirm)
        js("undefined")
    }
    userdata.push(restore)

    return userdata
}

// ---------------- 导入 / 导出 / 错误记录 / resetGUI ----------------

private fun casImportSettings(api: dynamic, f: dynamic) {
    var bytes: dynamic = js("undefined")
    try {
        bytes = js("SafeFileUtils").readUnsafe(f)
    } catch (e: dynamic) {
        js("Common").toast(casAdd("配置导入失败\n", e))
        return
    }
    api.resetGUI()
    js("SafeFileUtils").write(casNewFile1(api.profilePath), bytes)
    api.initialize()
    js("Common").toast("配置已导入")
}

private fun casExportSettings(api: dynamic) {
    api.trySave()

    val exportItem = casObj()
    exportItem.text = "导出"
    exportItem.onclick = { ->
        val opts = casObj()
        opts.mimeType = "application/octet-stream"
        opts.hint = "ca_settings.dat"
        opts.export = { uri: dynamic ->
            try {
                js("ExternalStorage").copy(js("ExternalStorage").toUri(api.profilePath), uri)
                js("Common").toast("配置已导出")
            } catch (e: dynamic) {
                js("erp")(e, true)
                js("Common").toast(casAdd("文件保存失败，无法导出\n", e))
            }
            js("undefined")
        }
        js("ExternalStorage").showExportActions(opts)
        js("undefined")
    }

    val sendItem = casObj()
    sendItem.text = "发送"
    sendItem.path = casNewFile2(js("ctx").getExternalCacheDir(), "ca_settings.dat")
    sendItem.onclick = casThis0Js { it: dynamic ->
        try {
            js("Common").fileCopy(casNewFile1(api.profilePath), it.path)
            js("AndroidBridge").sendFile(it.path)
        } catch (e: dynamic) {
            js("Log").e(e)
            js("Common").toast(casAdd("发送配置文件失败\n", e))
        }
        js("undefined")
    }

    js("Common").showOperateDialog(casArray(exportItem, sendItem))
}

private fun casManageErrors(api: dynamic) {
    val f = casNewFile2(js("MapScript").baseDir, "com.xero.ca.error.log")
    if (!casTruthy(f.isFile())) {
        js("Common").toast("无错误记录")
        return
    }

    var intent: dynamic = js("undefined")
    try {
        // 原实现把 Intent 的构造、setDataAndType、addFlags 全放在 try 里并 return；
        // 任何一步抛错时整个 intent 都是 undefined，这里同样只在全部成功后才赋值。
        val created = casNewViewIntent()
        created.setDataAndType(js("AndroidBridge").fileToUri(f), "text/plain")
        val flags = casBitOr(
            casBitOr(
                js("android.content.Intent").FLAG_GRANT_READ_URI_PERMISSION,
                js("android.content.Intent").FLAG_GRANT_WRITE_URI_PERMISSION
            ),
            js("android.content.Intent").FLAG_ACTIVITY_NEW_TASK
        )
        created.addFlags(flags)
        intent = created
    } catch (e: dynamic) {
        js("Log").e(e)
    }

    val open = casObj()
    open.text = "打开"
    open.intent = intent
    open.onclick = casThis0Js { it: dynamic ->
        js("AndroidBridge").startActivity(it.intent)
        js("undefined")
    }
    open.hidden = casThis0Js { it: dynamic ->
        !casTruthy(it.intent)
    }

    val view = casObj()
    view.text = "查看"
    view.onclick = { ->
        api.listErrors()
        js("undefined")
    }

    val send = casObj()
    send.text = "发送"
    send.onclick = { ->
        js("AndroidBridge").sendFile(f)
        js("undefined")
    }

    val clear = casObj()
    clear.text = "清空"
    clear.onclick = { ->
        f.delete()
        js("Common").toast("错误信息已清空")
        js("undefined")
    }

    js("Common").showOperateDialog(casArray(open, view, send, clear))
}

private fun casListErrors() {
    val f = js("Common").readFile(casAdd(js("MapScript").baseDir, "com.xero.ca.error.log"), "")
    if (casLength(f) == 0) return
    val a = f.slice(9).split("\n* ")
    a.reverse()
    js("Common").showListChooser(a, { id: dynamic ->
        js("Common").setClipboardText(a[id])
        js("Common").toast("错误信息已复制")
        js("undefined")
    })
}

private fun casResetGUI() {
    try {
        js("PWM").dismissFloat()
        js("PWM").dismissPopup()
        js("PWM").reset()
        js("PWM").resetUICache()
    } catch (e: dynamic) {
        js("erp")(e)
    }
}

// ---------------- FCS（样式代码栏） ----------------

private fun casShowFCS(api: dynamic, self: dynamic, v: dynamic) {
    js("G").ui({
        try {
            var i: Int
            var j: Int
            if (!casTruthy(self.prompt)) {
                val data = casArray(
                    casArray("§", "§l§§l", "§m§§m", "§n§§n", "§o§§o", "§§k", "§§r"),
                    casArray("§0§§0", "§1§§1", "§2§§2", "§3§§3", "§4§§4", "§5§§5", "§6§§6", "§7§§7"),
                    casArray("§8§§8", "§9§§9", "§a§§a", "§b§§b", "§c§§c", "§d§§d", "§e§§e", "§f§§f")
                )
                val frcolor = js("G.Color").WHITE
                val bgcolor = js("G.Color").BLACK

                self.setVisible = { visible: dynamic ->
                    if (casTruthy(visible)) {
                        self.scr.setVisibility(js("G.View").VISIBLE)
                        self.hide.setVisibility(js("G.View").GONE)
                    } else {
                        self.scr.setVisibility(js("G.View").GONE)
                        self.hide.setVisibility(js("G.View").VISIBLE)
                    }
                    js("undefined")
                }

                self.frame = casNewFrameLayout()
                self.frame.setLayoutParams(casNewFrameParams3(-1, -2, js("G.Gravity").BOTTOM))

                self.hide = casNewTextView()
                self.hide.setText("..")
                self.hide.setTextColor(frcolor)
                self.hide.setTextSize(js("Common").theme.textsize[2])
                self.hide.setGravity(js("G.Gravity").CENTER)
                self.hide.setTypeface(casOr(js("G.Typeface").MONOSPACE, js("G.Typeface").DEFAULT))
                self.hide.setPadding(casDp(10), casDp(10), casDp(10), casDp(10))
                self.hide.setBackgroundColor(js("Common").setAlpha(bgcolor, 0xC0))
                self.hide.setLayoutParams(casNewFrameParams3(-2, -2, js("G.Gravity").RIGHT))
                self.hide.setOnClickListener(casNewClickListener({ _view: dynamic ->
                    try {
                        self.setVisible(true)
                    } catch (e: dynamic) {
                        js("erp")(e)
                    }
                    js("undefined")
                }))
                self.hide.setOnLongClickListener(casNewLongClickListener({ _view: dynamic ->
                    var result: dynamic = js("undefined")
                    try {
                        api.showCustomExpression()
                        result = true
                    } catch (e: dynamic) {
                        js("erp")(e)
                    }
                    result
                }))
                self.frame.addView(self.hide)

                self.scr = casNewScrollView()
                self.scr.setBackgroundColor(js("Common").setAlpha(bgcolor, 0xC0))
                self.scr.setLayoutParams(casNewFrameParams2(-1, -2))

                self.line = casNewLinearLayout()
                self.line.setLayoutParams(casNewFrameParams2(-1, -2))
                self.line.setOrientation(js("G.LinearLayout").VERTICAL)

                self.prompt = casNewTextView()
                val lp1 = casNewLinearParams2(-1, -2)
                self.prompt.setLayoutParams(lp1)
                self.prompt.setTextColor(frcolor)
                self.prompt.setSingleLine(true)
                self.prompt.setEllipsize(js("G.TextUtils.TruncateAt").START)
                self.prompt.setTextSize(js("Common").theme.textsize[2])
                self.prompt.setPadding(casDp(20), casDp(10), casDp(10), casDp(10))
                self.prompt.setTypeface(casOr(js("G.Typeface").MONOSPACE, js("G.Typeface").DEFAULT))
                self.prompt.setOnClickListener(casNewClickListener({ _view: dynamic ->
                    try {
                        self.setVisible(false)
                    } catch (e: dynamic) {
                        js("erp")(e)
                    }
                    js("undefined")
                }))
                self.line.addView(self.prompt)

                val lp2 = casNewLinearParams3(0, -2, 1)
                val onclick = casNewClickListener({ view: dynamic ->
                    try {
                        js("Common").replaceSelection(api.cmd.getText(), view.getText().toString())
                    } catch (e: dynamic) {
                        js("erp")(e)
                    }
                    js("undefined")
                })

                self.tableline = js("[]")
                self.tableview = js("[]")
                i = 0
                while (i < casLength(data)) {
                    val l = casNewLinearLayout()
                    self.tableline.push(l)
                    l.setOrientation(js("G.LinearLayout").HORIZONTAL)
                    self.tableview.push(js("[]"))
                    j = 0
                    while (j < casLength(data[i])) {
                        val b = casNewTextView()
                        self.tableview[i].push(b)
                        b.setTextColor(frcolor)
                        b.setTextSize(js("Common").theme.textsize[2])
                        b.setGravity(js("G.Gravity").CENTER)
                        b.setTypeface(casOr(js("G.Typeface").MONOSPACE, js("G.Typeface").DEFAULT))
                        b.setPadding(casDp(5), casDp(10), casDp(5), casDp(10))
                        b.setText(js("FCString").parseFC(data[i][j]))
                        b.setOnClickListener(onclick)
                        l.addView(b, lp2)
                        j++
                    }
                    self.line.addView(l, lp1)
                    i++
                }
                val more = casNewTextView()
                self.tableview[0].push(more)
                more.setTextColor(frcolor)
                more.setTextSize(js("Common").theme.textsize[2])
                more.setGravity(js("G.Gravity").CENTER)
                more.setTypeface(casOr(js("G.Typeface").MONOSPACE, js("G.Typeface").DEFAULT))
                more.setPadding(0, casDp(10), 0, casDp(10))
                more.setText("..")
                more.setOnClickListener(casNewClickListener({ _view: dynamic ->
                    try {
                        api.showCustomExpression()
                    } catch (e: dynamic) {
                        js("erp")(e)
                    }
                    js("undefined")
                }))
                self.tableline[0].addView(more, lp2)

                self.exit = casNewTextView()
                self.exit.setLayoutParams(casNewLinearParams2(-1, -2))
                self.exit.setText("关闭")
                self.exit.setTextSize(js("Common").theme.textsize[2])
                self.exit.setGravity(js("G.Gravity").CENTER)
                self.exit.setTextColor(frcolor)
                self.exit.setPadding(casDp(10), casDp(10), casDp(10), casDp(20))
                self.exit.setOnClickListener(casNewClickListener({ _view: dynamic ->
                    try {
                        api.hideFCS()
                    } catch (e: dynamic) {
                        js("erp")(e)
                    }
                    js("undefined")
                }))
                self.line.addView(self.exit)

                self.scr.addView(self.line)
                self.frame.addView(self.scr)

                js("PWM").registerResetFlag(api, "fcs")
                js("PWM").registerResetFlag(self, "prompt")
            }
            if (casTruthy(v)) self.prompt.setText(js("FCString").parseFC(v))
            // hideFCS 是 G.ui 异步的（post 出去后这里还在同步执行），
            // 直接 addView 会撞 "child already has a parent"，所以旧窗口要同步摘掉
            if (casTruthy(api.fcs)) {
                try {
                    api.con.removeView(api.fcs)
                } catch (e: dynamic) {
                }
                api.fcs = js("null")
            }
            api.fcs = self.frame
            self.setVisible(true)
            api.con.addView(api.fcs)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

private fun casHideFCS(api: dynamic) {
    js("G").ui({
        try {
            if (!casTruthy(api.fcs)) {
                js("undefined")
            } else {
                api.con.removeView(api.fcs)
                api.fcs = js("null")
            }
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

// ---------------- 自定义短语 ----------------

private fun casShowCustomExpression(api: dynamic) {
    val edit = casObj()
    edit.text = "(编辑自定义短语)"
    edit.custom = true
    val a = api.PluginExpression.concat(api.settings.customExpression, edit)
    js("Common").showListChooser(a, { i: dynamic ->
        if (casTruthy(a[i])) {
            if (casTruthy(a[i].get)) {
                val r = a[i].get()
                if (casTruthy(r)) {
                    js("Common").replaceSelection(api.cmd.getText(), js("Common").toString(r))
                }
            } else if (casTruthy(a[i].custom)) {
                api.showCustomExpEdit()
            } else {
                js("Common").replaceSelection(
                    api.cmd.getText(),
                    js("Common").toString(casOr(a[i].text, a[i]))
                )
            }
        }
        js("undefined")
    })
}

private fun casShowCustomExpEdit(api: dynamic, self: dynamic, callback: dynamic) {
    js("G").ui({
        try {
            if (!casTruthy(self.linear)) casBuildCustomExpEdit(api, self)
            self.refresh()
            self.callback = callback
            self.popup.enter()
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

private fun casBuildCustomExpEdit(api: dynamic, self: dynamic) {
    self.adapter = js("null")
    self.refresh = { ->
        if (casLength(api.settings.customExpression) == 0) {
            self.adapter = js("null")
            self.list.setAdapter(js("EmptyAdapter"))
        } else {
            if (casTruthy(self.adapter)) {
                self.adapter.notifyChange()
            } else {
                val a = casNewSimpleListAdapter(api.settings.customExpression, self.vmaker, self.vbinder)
                self.list.setAdapter(a)
                self.adapter = casSimpleGetController(a)
            }
        }
        js("undefined")
    }
    self.vmaker = { holder: dynamic ->
        val layout = casNewLinearLayout()
        val text = casNewTextView()
        holder.text = text
        val del = casNewTextView()
        layout.setGravity(js("G.Gravity").CENTER)
        layout.setLayoutParams(casNewAbsParams(-1, -2))
        layout.setOrientation(js("G.LinearLayout").HORIZONTAL)
        text.setPadding(casDp(15), casDp(15), casDp(15), casDp(15))
        text.setLayoutParams(casNewLinearParams3(0, -2, 1.0))
        text.setMaxLines(2)
        text.setEllipsize(js("G.TextUtils.TruncateAt").END)
        js("Common").applyStyle(text, "textview_default", 3)
        layout.addView(text)
        del.setText("×")
        del.setPadding(casDp(15), casDp(15), casDp(15), casDp(15))
        del.setLayoutParams(casNewLinearParams2(-2, -2))
        js("Common").applyStyle(del, "textview_default", 2)
        del.setOnClickListener(casNewClickListener({ _v: dynamic ->
            try {
                api.settings.customExpression.splice(holder.pos, 1)
                self.refresh()
            } catch (e: dynamic) {
                js("erp")(e)
            }
            js("undefined")
        }))
        layout.addView(del)
        layout
    }
    self.vbinder = { holder: dynamic, e: dynamic, _i: dynamic ->
        holder.text.setText(e)
        js("undefined")
    }

    self.linear = casNewLinearLayout()
    self.linear.setOrientation(js("G.LinearLayout").VERTICAL)
    js("Common").applyStyle(self.linear, "container_default")
    self.header = casNewLinearLayout()
    self.header.setOrientation(js("G.LinearLayout").HORIZONTAL)
    self.header.setLayoutParams(casNewLinearParams2(-1, -2))
    js("Common").applyStyle(self.header, "bar_float")
    self.back = casNewTextView()
    self.back.setText("< 返回")
    self.back.setGravity(js("G.Gravity").CENTER)
    self.back.setPadding(casDp(15), 0, casDp(15), 0)
    self.back.setLayoutParams(casNewLinearParams2(-2, -1))
    js("Common").applyStyle(self.back, "button_highlight", 2)
    self.back.setOnClickListener(casNewClickListener({ _v: dynamic ->
        var result: dynamic = js("undefined")
        try {
            self.popup.exit()
            result = true
        } catch (e: dynamic) {
            js("erp")(e)
        }
        result
    }))
    self.header.addView(self.back)
    self.title = casNewTextView()
    self.title.setText("自定义短语")
    self.title.setPadding(0, casDp(10), casDp(15), casDp(10))
    self.title.setLayoutParams(casNewLinearParams3(-2, -2, 1))
    js("Common").applyStyle(self.title, "textview_default", 4)
    self.header.addView(self.title)
    self.add = casNewTextView()
    self.add.setLayoutParams(casNewLinearParams2(-2, -1))
    self.add.setText("添加")
    self.add.setGravity(js("G.Gravity").CENTER)
    self.add.setPadding(casDp(10), casDp(10), casDp(10), casDp(10))
    js("Common").applyStyle(self.add, "button_highlight", 2)
    self.add.setOnClickListener(casNewClickListener({ _v: dynamic ->
        try {
            val input = casObj()
            input.title = "添加自定义短语"
            input.callback = { s: dynamic ->
                if (!casTruthy(s)) {
                    js("Common").toast("短语不能为空")
                } else if (casGe(api.settings.customExpression.indexOf(s), 0)) {
                    js("Common").toast(casAdd(casAdd("自定义短语“", s), "”已存在"))
                } else {
                    api.settings.customExpression.push(s)
                    self.refresh()
                }
                js("undefined")
            }
            input.singleLine = true
            js("Common").showInputDialog(input)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }))
    self.header.addView(self.add)
    self.linear.addView(self.header)

    self.list = casNewListView()
    self.list.setOnItemClickListener(casNewItemClickListener({ _parent: dynamic, _view: dynamic, pos: dynamic, _id: dynamic ->
        try {
            val input = casObj()
            input.title = "编辑自定义短语"
            input.callback = { s: dynamic ->
                val i = api.settings.customExpression.indexOf(s)
                if (!casTruthy(s)) {
                    api.settings.customExpression.splice(pos, 1)
                    self.refresh()
                } else if (casGe(i, 0) && casLooseNe(i, pos)) {
                    js("Common").toast(casAdd(casAdd("自定义短语“", s), "”已存在"))
                } else if (casLt(i, 0)) {
                    api.settings.customExpression[pos] = s
                    self.refresh()
                }
                js("undefined")
            }
            input.singleLine = true
            input.defaultValue = api.settings.customExpression[pos]
            js("Common").showInputDialog(input)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }))
    self.linear.addView(self.list)

    self.popup = casNewPopupPage(self.linear, "ca.CustomExpEdit")
    self.popup.on("exit", { ->
        if (casTruthy(self.callback)) self.callback()
        js("undefined")
    })

    js("PWM").registerResetFlag(self, "linear")
}

// ---------------- 粘贴 ----------------

private fun casIsMinecraftTextbox(packageName: dynamic): Boolean {
    return casLooseEq(packageName, "net.zhuoweizhang.mcpelauncher.pro") ||
        casLooseEq(packageName, "net.zhuoweizhang.mcpelauncher") ||
        casGe(js("NeteaseAdapter").packNames.indexOf(packageName), 0)
}

private fun casPerformPaste(api: dynamic, cmd: dynamic, warnSvcNotRun: dynamic) {
    js("Common").setClipboardText(cmd)
    if (casLooseEq(js("MapScript").host, "AutoJs") || casLooseEq(js("MapScript").host, "Android")) {
        try {
            if (casLooseEq(js("MapScript").host, "AutoJs")) {
                val widgets = js("editable")().find()
                var success = false
                if (casTruthy(widgets.empty())) casThrow("找不到文本框")
                widgets.each({ e: dynamic ->
                    if (casTruthy(api.settings.overwriteMCTextbox) &&
                        casIsMinecraftTextbox(casString(e.packageName()))
                    ) {
                        if (casTruthy(e.setText(cmd)) || success) success = true
                    } else {
                        if (casTruthy(e.paste()) || success) success = true
                    }
                    js("undefined")
                })
                if (!success) casThrow("粘贴失败")
            } else if (casLooseEq(js("MapScript").host, "Android")) {
                val svc = js("ScriptInterface").getAccessibilitySvc()
                if (!casTruthy(svc)) {
                    if (casTruthy(warnSvcNotRun)) {
                        casThrow("请打开无障碍服务")
                    } else {
                        return
                    }
                }
                if (casLt(js("android.os.Build.VERSION").SDK_INT, 18)) {
                    casThrow("系统版本过低！请升级系统至Android 4.3及以上")
                }
                var node = svc.getRootInActiveWindow()
                if (!casTruthy(node)) casThrow("无法获取窗口内容")
                node = node.findFocus(js("android.view.accessibility.AccessibilityNodeInfo").FOCUS_INPUT)
                if (!casTruthy(node)) casThrow("找不到焦点输入控件")
                if (!casTruthy(node.isEditable())) casThrow("当前焦点输入控件不可编辑")
                if (casTruthy(api.settings.overwriteMCTextbox) &&
                    casIsMinecraftTextbox(casString(node.getPackageName()))
                ) {
                    val bundle = casNewBundle()
                    bundle.putCharSequence(
                        js("android.view.accessibility.AccessibilityNodeInfo").ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        cmd
                    )
                    if (!casTruthy(node.performAction(
                            js("android.view.accessibility.AccessibilityNodeInfo").ACTION_SET_TEXT,
                            bundle
                        ))
                    ) {
                        casThrow("设置文本失败")
                    }
                } else {
                    if (!casTruthy(node.performAction(
                            js("android.view.accessibility.AccessibilityNodeInfo").ACTION_PASTE
                        ))
                    ) {
                        casThrow("粘贴失败")
                    }
                }
            }
        } catch (e: dynamic) {
            js("Common").toast(e)
        }
    } else {
        try {
            if (casGt(api.settings.pasteDelay, 0)) {
                js("Common").toast(
                    casAdd(casAdd("请在", casDiv(api.settings.pasteDelay, 20)), "秒内点击需要粘贴的文本框")
                )
                js("gHandler").postDelayed({ ->
                    try {
                        js("ctx").updateTextboxText(cmd)
                    } catch (e: dynamic) {
                        js("Common").toast(casAdd("当前版本暂不支持粘贴命令\n", e))
                    }
                    js("undefined")
                }, casMul(api.settings.pasteDelay, 50))
            } else if (casLooseEq(api.settings.pasteDelay, 0)) {
                js("ctx").updateTextboxText(cmd)
            } else {
                api.showPasteDelaySet({ ->
                    api.performPaste(cmd, warnSvcNotRun)
                    js("undefined")
                })
            }
        } catch (e: dynamic) {
            js("Common").toast(casAdd("当前版本暂不支持粘贴命令\n", e))
        }
    }
}

private fun casPasteUpdateWidth(self: dynamic, widthArg: dynamic) {
    var width = widthArg
    if (casGt(width, self.widthMax)) width = self.widthMax
    if (casLt(width, self.widthMin)) {
        self.inDrawer = true
        width = js("G").dp
    } else {
        self.inDrawer = false
    }
    self.lparam.width = width
    self.linear.setLayoutParams(self.lparam)
}

private fun casPasteAnimateShow(self: dynamic) {
    val t = casNewTranslate4(casNeg(casMul(self.dir, self.lparam.width)), 0, 0, 0)
    t.setInterpolator(casNewDecelerate(2.0))
    t.setDuration(100)
    self.linear.startAnimation(t)
}

private fun casPasteAnimateHide(api: dynamic, self: dynamic) {
    val t = casNewTranslate4(0, casNeg(casMul(self.dir, self.lparam.width)), 0, 0)
    t.setInterpolator(casNewAccelerate(2.0))
    t.setDuration(100)
    t.setAnimationListener(casNewAnimationListener({ _a: dynamic ->
        api.hidePaste()
        js("undefined")
    }))
    self.linear.startAnimation(t)
}

private fun casPasteCancelTail(api: dynamic, self: dynamic) {
    if (!casTruthy(self.inDrawer)) self.list.setVisibility(js("G.View").VISIBLE)
    api.paste.attributes.width = casAdd(self.lparam.width, casDp(16))
    api.paste.update()
}

private fun casPasteTouch(api: dynamic, self: dynamic, touch: dynamic, v: dynamic, e: dynamic): dynamic {
    try {
        val action = e.getAction()
        if (casLooseEq(action, e.ACTION_MOVE)) {
            if (!casTruthy(touch.verticalScroll)) {
                if (casTruthy(touch.stead)) {
                    val dx = casAbs(casSub(touch.lx, e.getRawX()))
                    if (casLt(dx, casDp(16))) {
                        // 原实现：水平位移不足 16dp 时什么都不做。
                    } else if (casLt(dx, casMul(casAbs(casSub(touch.ly, e.getRawY())), 2))) {
                        touch.verticalScroll = true
                    } else {
                        touch.stead = false
                        if (!casTruthy(self.inDrawer)) self.list.setVisibility(js("G.View").GONE)
                        api.paste.attributes.width = js("Common").getScreenWidth()
                        api.paste.update()
                    }
                } else {
                    self.updateWidth(casAdd(touch.slw, casMul(casSub(e.getRawX(), touch.lx), self.dir)))
                }
            }
        } else if (casLooseEq(action, e.ACTION_DOWN)) {
            touch.lx = e.getRawX()
            touch.ly = e.getRawY()
            touch.slw = self.lparam.width
            self.widthMin = casMul(0.1, js("Common").getScreenWidth())
            self.widthMax = casMul(9, self.widthMin)
            touch.stead = true
            touch.verticalScroll = false
        } else if (casLooseEq(action, e.ACTION_UP)) {
            if (!casTruthy(touch.verticalScroll) && !casTruthy(touch.stead)) {
                self.updateWidth(casAdd(touch.slw, casMul(casSub(e.getRawX(), touch.lx), self.dir)))
                // ACTION_UP 未 break，会继续执行 ACTION_CANCEL 的收尾逻辑。
                casPasteCancelTail(api, self)
            }
        } else if (casLooseEq(action, e.ACTION_CANCEL)) {
            casPasteCancelTail(api, self)
        }
        return casLooseEq(v, self.bar)
    } catch (err: dynamic) {
        js("erp")(err)
        return true
    }
}

private fun casShowPaste(api: dynamic, self: dynamic) {
    js("G").ui({
        try {
            if (!casTruthy(self.bar)) {
                self.vmaker = { _holder: dynamic ->
                    val view = casNewTextView()
                    view.setLayoutParams(casNewAbsParams(-1, -2))
                    view.setPadding(casDp(5), casDp(5), casDp(5), casDp(5))
                    js("Common").applyStyle(view, "textview_default", 2)
                    view
                }
                self.vbinder = { holder: dynamic, s: dynamic, _i: dynamic, _a: dynamic ->
                    holder.self.setText(s)
                    js("undefined")
                }
                self.adapter = casSimpleGetController(
                    casNewSimpleListAdapter(api.his, self.vmaker, self.vbinder)
                )
                self.refresh = { ->
                    self.adapter.notifyChange()
                    js("undefined")
                }
                self.updateWidth = { width: dynamic -> casPasteUpdateWidth(self, width) }
                self.animateShow = { -> casPasteAnimateShow(self); js("undefined") }
                self.animateHide = { -> casPasteAnimateHide(api, self); js("undefined") }
                self.touchListener = casNewTouchListener(
                    casTouchFactoryJs { touch: dynamic, v: dynamic, e: dynamic ->
                        casPasteTouch(api, self, touch, v, e)
                    }
                )

                self.inDrawer = false
                self.bar = casNewFrameLayout()
                self.bar.setOnTouchListener(self.touchListener)
                self.linear = casNewLinearLayout()
                self.linear.setOrientation(js("G.LinearLayout").VERTICAL)
                val lparam = casNewFrameParams3(
                    casMul(0.4, js("Common").getScreenWidth()),
                    -1,
                    js("G.Gravity").LEFT
                )
                self.lparam = lparam
                self.linear.setLayoutParams(lparam)
                js("Common").applyStyle(self.linear, "bar_float")
                self.header = casNewLinearLayout()
                self.header.setOrientation(js("G.LinearLayout").HORIZONTAL)
                self.header.setLayoutParams(casNewLinearParams2(-1, -2))
                self.title = casNewTextView()
                self.title.setLayoutParams(casNewLinearParams3(0, -2, 1))
                self.title.setPadding(casDp(5), casDp(5), casDp(5), casDp(5))
                self.title.setText("粘贴栏")
                self.title.setSingleLine(true)
                js("Common").applyStyle(self.title, "textview_prompt", 1)
                self.header.addView(self.title)
                self.exit = casNewTextView()
                self.exit.setLayoutParams(casNewLinearParams2(-2, -2))
                self.exit.setPadding(casDp(5), casDp(5), casDp(5), casDp(5))
                self.exit.setText("x")
                self.exit.setOnClickListener(casNewClickListener({ _view: dynamic ->
                    try {
                        if (casTruthy(api.settings.noAnimation)) {
                            api.hidePaste()
                        } else {
                            self.animateHide()
                        }
                    } catch (e: dynamic) {
                        js("erp")(e)
                    }
                    js("undefined")
                }))
                js("Common").applyStyle(self.exit, "button_critical", 1)
                self.header.addView(self.exit)
                self.linear.addView(self.header)
                self.list = casNewListView()
                self.list.setLayoutParams(casNewLinearParams2(-1, -1))
                self.list.setAdapter(self.adapter.self)
                self.list.setOnTouchListener(self.touchListener)
                self.list.setOnItemClickListener(casNewItemClickListener({ _parent: dynamic, _view: dynamic, pos: dynamic, _id: dynamic ->
                    try {
                        api.performPaste(self.adapter.array[pos], false)
                    } catch (e: dynamic) {
                        js("erp")(e)
                    }
                    js("undefined")
                }))
                if (casLooseEq(js("MapScript").host, "Android")) {
                    self.list.setOnItemLongClickListener(casNewItemLongClickListener({ _parent: dynamic, _view: dynamic, pos: dynamic, _id: dynamic ->
                        try {
                            if (casTruthy(js("WSServer").isConnected())) {
                                js("WSServer").sendCommand(self.adapter.array[pos], { json: dynamic ->
                                    js("Common").toast(
                                        casAdd(
                                            casAdd("已执行！状态代码：", json.statusCode),
                                            casAdd("\n", json.statusMessage)
                                        )
                                    )
                                    js("undefined")
                                })
                            } else {
                                if (!casTruthy(js("WSServer").isAvailable())) {
                                    js("Common").toast("请先在设置中打开WebSocket服务器")
                                } else {
                                    js("Common").toast(
                                        casAdd(
                                            "请在客户端输入以下指令之一来连接到服务器。\n",
                                            js("WSServer").getConnectCommands().join("\n")
                                        )
                                    )
                                }
                            }
                            true
                        } catch (e: dynamic) {
                            js("erp")(e)
                            true
                        }
                    }))
                }
                self.linear.addView(self.list)
                self.bar.addView(self.linear)
                js("PWM").registerResetFlag(self, "bar")
                js("PWM").registerResetFlag(api, "paste")
            }
            self.widthMin = casMul(0.1, js("Common").getScreenWidth())
            self.widthMax = casMul(9, self.widthMin)
            if (casTruthy(self.inDrawer)) {
                self.updateWidth(casMul(4, self.widthMin))
                self.list.setVisibility(js("G.View").VISIBLE)
                if (casTruthy(api.paste)) {
                    val updateOpts = casObj()
                    updateOpts.width = casAdd(self.lparam.width, casDp(16))
                    api.paste.update(updateOpts)
                    if (!casTruthy(api.settings.noAnimation)) self.animateShow()
                }
            }
            self.refresh()
            // 原实现 if (CA.paste) return; 返回的是 G.ui 回调，这里用 else 分支保持语义。
            if (!casTruthy(api.paste)) {
                if (casLooseEq(api.settings.pasteBarGravity, 1)) {
                    self.lparam.gravity = js("G.Gravity").RIGHT
                    self.gravity = js("G.Gravity").RIGHT
                    self.lparam.setMargins(casDp(16), 0, 0, 0)
                    self.dir = -1
                } else {
                    self.lparam.gravity = js("G.Gravity").LEFT
                    self.gravity = js("G.Gravity").LEFT
                    self.lparam.setMargins(0, 0, casDp(16), 0)
                    self.dir = 1
                }
                val popup = casNewPopupWindow(self.bar, "CA.PasteBar")
                api.paste = popup
                val showOpts = casObj()
                showOpts.width = casAdd(self.lparam.width, casDp(16))
                showOpts.height = -1
                showOpts.gravity = self.gravity
                showOpts.x = 0
                showOpts.y = 0
                showOpts.focusable = false
                showOpts.touchable = true
                popup.show(showOpts)
                if (!casTruthy(api.settings.noAnimation)) self.animateShow()
                js("PWM").addPopup(api.paste)
            }
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

private fun casHidePaste(api: dynamic) {
    js("G").ui({
        try {
            if (casTruthy(api.paste)) api.paste.hide()
            api.paste = js("null")
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

private fun casShowPasteDelaySet(api: dynamic, callback: dynamic) {
    val o = casObj()
    o.max = 100
    o.progress = if (casIsNaN(api.settings.pasteDelay)) 40 else api.settings.pasteDelay
    o.prompt = { progress: dynamic ->
        if (casGt(progress, 0)) {
            casAdd(
                casAdd("延迟", casDiv(progress, 20).toFixed(2)),
                "秒后粘贴（仅适用于启动器）\n\n点击“粘贴”时将不会立即粘贴，你需要在这段延迟时间中点击需要粘贴的文本框。\n您可以在设置中修改该设置。"
            )
        } else {
            "立即粘贴\n\n点击“粘贴”时将会立即粘贴，但你只能粘贴到聊天框中。\n您可以在设置中修改该设置。"
        }
    }
    o.callback = { progress: dynamic ->
        api.settings.pasteDelay = casParseInt(progress)
        js("undefined")
    }
    o.onDismiss = callback
    js("Common").showSlider(o)
}

// ---------------- 安装 ----------------

fun installCaSettings(api: dynamic) {
    api.showSettings = casSelf0Js { self: dynamic ->
        casShowSettings(api, self)
    }
    api.importSettings = casVoid1Js { f: dynamic ->
        casImportSettings(api, f)
    }
    api.exportSettings = casVoid0Js {
        casExportSettings(api)
    }
    api.manageErrors = casVoid0Js {
        casManageErrors(api)
    }
    api.listErrors = casVoid0Js {
        casListErrors()
    }
    api.resetGUI = casVoid0Js {
        casResetGUI()
    }
    api.showFCS = casSelf1Js { self: dynamic, v: dynamic ->
        casShowFCS(api, self, v)
    }
    api.hideFCS = casVoid0Js {
        casHideFCS(api)
    }
    api.showCustomExpression = casVoid0Js {
        casShowCustomExpression(api)
    }
    api.showCustomExpEdit = casSelf1Js { self: dynamic, callback: dynamic ->
        casShowCustomExpEdit(api, self, callback)
    }
    api.isMinecraftTextbox = { packageName: dynamic ->
        casIsMinecraftTextbox(packageName)
    }
    api.performPaste = { cmd: dynamic, warnSvcNotRun: dynamic ->
        casPerformPaste(api, cmd, warnSvcNotRun)
        js("undefined")
    }
    api.showPaste = casSelf0Js { self: dynamic ->
        casShowPaste(api, self)
    }
    api.hidePaste = casVoid0Js {
        casHidePaste(api)
    }
    api.showPasteDelaySet = casVoid1Js { callback: dynamic ->
        casShowPasteDelaySet(api, callback)
    }
}
