package ca.kotlin.network

import ca.kotlin.core.*


/**
 * modules/network/PushService.js 的 Kotlin/JS 版本。
 *
 * 通知渠道 / 通知栏、推送列表设置页和历史推送列表都保持原对象结构；
 * 设置项的 get / set 会被 Common.showSettings 以设置项自身为 this 调用，
 * 因此这两个入口通过 JS wrapper 把动态 this 传回 Kotlin。
 */
private val pushApi: dynamic = js("({})")
private val pushSettingsSelf: dynamic = js("({})")
private val pushHistorySelf: dynamic = js("({})")

// ---------------- JS 语义 helper ----------------
private val pushTruthyJs: dynamic = js("function (v) { return !!v; }")
private val pushStringJs: dynamic = js("function (v) { return String(v); }")
private val pushLengthJs: dynamic = js("function (v) { return v.length | 0; }")
private val pushParseIntJs: dynamic = js("function (v) { return parseInt(v); }")
private val pushIsNaNNs: dynamic = js("function (v) { return isNaN(v); }")
private val pushThisGetterJs: dynamic = js("function (f) { return function () { return f(this); }; }")
private val pushThisSetterJs: dynamic = js("function (f) { return function (v) { return f(this, v); }; }")
private val pushArray1Js: dynamic = js("function (v) { return [v]; }")
private val pushQueryJs: dynamic = js(
    "function (since, limit, dir, sort) { return { since: since, limit: limit, dir: dir, sort: sort }; }"
)

// ---------------- Java / Android helper ----------------
private val pushNewNotificationChannelJs: dynamic = js(
    "function (id, name, importance) { return new android.app.NotificationChannel(id, name, importance); }"
)
private val pushNewNotificationBuilder1Js: dynamic = js(
    "function (c) { return new android.app.Notification.Builder(c); }"
)
private val pushNewNotificationBuilder2Js: dynamic = js(
    "function (c, ch) { return new android.app.Notification.Builder(c, ch); }"
)
private val pushParseUriJs: dynamic = js(
    "function (s, f) { return android.content.Intent.parseUri(s, f); }"
)
private val pushNewTextViewJs: dynamic = js("function (c) { return new G.TextView(c); }")
private val pushNewAbsListViewParamsJs: dynamic = js(
    "function (w, h) { return new G.AbsListView.LayoutParams(w, h); }"
)
private val pushNewSimpleListAdapterJs: dynamic = js(
    "function (a, b, c) { return new SimpleListAdapter(a, b, c); }"
)
private val pushGetListAdapterControllerJs: dynamic = js(
    "function (a) { return SimpleListAdapter.getController(a); }"
)
private val pushSmallIconJs: dynamic = js("function () { return com.xero.ca.R.mipmap.icon_small; }")
private val pushDecodeResourceJs: dynamic = js(
    "function (resources, icon) { return G.BitmapFactory.decodeResource(resources, icon); }"
)
private val pushCreatePendingIntentJs: dynamic = js(
    """function (type, intent, actions, id) {
        return AndroidBridge.createPendingIntent(type, intent, actions, id);
    }"""
)

private fun pushTruthy(v: dynamic): Boolean = pushTruthyJs(v)
private fun pushString(v: dynamic): dynamic = pushStringJs(v)
private fun pushLength(v: dynamic): Int = pushLengthJs(v)
private fun pushParseInt(v: dynamic): dynamic = pushParseIntJs(v)
private fun pushIsNaN(v: dynamic): Boolean = pushIsNaNNs(v)
private fun pushSdkInt(): Int = js("android.os.Build.VERSION.SDK_INT")

private fun pushOnCreate() {
    if (js("MapScript").host != "Android") return
    pushApi.nms = js("ctx").getSystemService(js("ctx").NOTIFICATION_SERVICE)
    if (!pushTruthy(pushApi.nms)) return
    if (pushSdkInt() >= 26) {
        pushApi.channel = pushNewNotificationChannelJs(
            "capush",
            "推送信息",
            pushApi.nms.IMPORTANCE_DEFAULT
        )
        pushApi.channel.setDescription("命令助手自带推送，仅命令助手在后台时工作")
        pushApi.nms.createNotificationChannel(pushApi.channel)
    }
}

private fun pushInitialize() {
    pushLoad()
    pushNotify()
}

private fun pushLoad() {
    pushApi.enabled = !pushTruthy(js("CA").settings.disablePush) && js("MapScript").host == "Android"
    val tags = js("CA").settings.disabledPushTags
    if (pushTruthy(tags)) {
        pushApi.disabledTags = tags
    } else {
        js("CA").settings.disabledPushTags = js("[]")
        pushApi.disabledTags = js("CA").settings.disabledPushTags
    }
    val lastPushId = js("CA").settings.lastPushId
    pushApi.lastPushId = if (pushIsNaN(lastPushId)) -1 else lastPushId
}

private fun pushSave() {
    js("CA").settings.lastPushId = pushApi.lastPushId
}

private fun pushGetIntent(post: dynamic): dynamic {
    var intent: dynamic = null
    try {
        if (pushTruthy(post.url)) {
            intent = pushParseUriJs(post.url, 0x7)
            intent.addFlags(intent.FLAG_ACTIVITY_NEW_TASK)
        }
    } catch (e: dynamic) {
        js("Log").e(e)
    }
    return intent
}

private fun pushShowNotification(o: dynamic): dynamic {
    val id = pushParseInt(o.id)
    val builder: dynamic
    if (pushSdkInt() >= 26) {
        builder = pushNewNotificationBuilder2Js(js("ctx"), pushApi.channel.id)
    } else {
        builder = pushNewNotificationBuilder1Js(js("ctx"))
    }
    builder.setContentTitle(pushString(o.name))
    if (pushTruthy(o.desc)) builder.setContentText(pushString(o.desc))
    builder.setAutoCancel(true)
    val icon = pushSmallIconJs()
    builder.setSmallIcon(icon)
    builder.setLargeIcon(pushDecodeResourceJs(js("ctx").getResources(), icon))
    val intent = pushApi.getIntent(o)
    if (pushTruthy(intent)) {
        builder.setContentIntent(pushCreatePendingIntentJs("activity", intent, pushArray1Js("updateCurrent"), id))
    }
    val nof: dynamic
    if (pushSdkInt() >= 16) {
        nof = builder.build()
    } else {
        nof = builder.getNotification()
    }
    pushApi.nms.notify("capush", id, nof)
    return true
}

private fun pushCancelNotification(o: dynamic) {
    pushApi.nms.cancel("capush", pushParseInt(o.id))
}

private fun pushShowDialog(o: dynamic) {
    val intent = pushApi.getIntent(o)
    if (pushTruthy(intent)) {
        val dialog = js("({})")
        dialog.title = o.name
        dialog.description = o.desc
        val buttons = js("[]")
        buttons.push(js("Common").intl.open)
        buttons.push(js("Common").intl.close)
        dialog.buttons = buttons
        dialog.callback = { index: dynamic ->
            if (index == 0) js("AndroidBridge").startActivity(intent)
            js("undefined")
        }
        js("Common").showConfirmDialog(dialog)
    } else {
        val dialog = js("({})")
        dialog.title = o.name
        dialog.description = o.desc
        val buttons = js("[]")
        buttons.push(js("Common").intl.ok)
        dialog.buttons = buttons
        js("Common").showConfirmDialog(dialog)
    }
}

private fun pushGetPosts(since: dynamic, limit: dynamic, direction: dynamic, sort: dynamic): dynamic {
    return js("JSON").parse(
        js("NetworkUtils").queryPage(
            pushApi.apiHost + "/push?" +
                js("NetworkUtils").toQueryString(pushQueryJs(since, limit, direction, sort))
        )
    )
}

private fun pushGetTags(): dynamic {
    return js("JSON").parse(js("NetworkUtils").queryPage(pushApi.apiHost + "/push/tags"))
}

private fun pushReadPushs(): dynamic {
    val pushs = pushApi.getPosts(pushApi.lastPushId + 1, 10, "forward", "desc")
    val firstTime = pushApi.lastPushId < 0
    if (pushLength(pushs) > 0) {
        pushApi.lastPushId = pushParseInt(pushs[0].id)
    }
    return if (firstTime) js("[]") else pushs
}

private fun pushPeekLatestPush(): dynamic {
    return pushApi.getPosts(0, 1, "forward", "desc")
}

private fun pushShouldShowPush(push: dynamic): Boolean {
    var i = 0
    val n = pushLength(push.tags)
    while (i < n) {
        val tag = push.tags[i]
        if (tag == "hiddenForPush") return false
        if (pushApi.disabledTags.indexOf(tag) >= 0) return false
        i++
    }
    if (push.minver > pushApi.version) return false
    if (push.maxver < pushApi.version) return false
    return true
}

private fun pushShouldShowAsDialog(push: dynamic): Boolean {
    var i = 0
    val n = pushLength(push.tags)
    while (i < n) {
        val tag = push.tags[i]
        if (tag == "important" || tag == "showAsDialog") return true
        i++
    }
    return false
}

private fun pushShowPushs(pushs: dynamic) {
    var i = 0
    val n = pushLength(pushs)
    while (i < n) {
        pushs[i].tags = pushs[i].tags.split("|")
        if (pushShouldShowPush(pushs[i])) {
            if (pushShouldShowAsDialog(pushs[i])) {
                pushShowDialog(pushs[i])
            } else {
                pushShowNotification(pushs[i])
            }
        }
        i++
    }
}

private fun pushDoCheckPush() {
    js("Threads").run({
        try {
            var pushs: dynamic = js("undefined")
            try {
                pushs = pushApi.readPushs()
            } catch (e: dynamic) {
                js("Log").e(e)
            }
            if (pushTruthy(pushs)) {
                pushApi.save()
                js("G").ui({
                    try {
                        pushApi.showPushs(pushs)
                    } catch (e: dynamic) {
                        js("erp")(e)
                    }
                    js("undefined")
                })
            }
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

private fun pushNotify() {
    val now = js("Date").now()
    if (!pushTruthy(pushApi.enabled)) return
    if (now - pushApi.lastCheck < 3600000) return
    pushApi.lastCheck = now
    pushApi.doCheckPush()
}

private fun pushBuildSettings(tags: dynamic): dynamic {
    if (pushTruthy(tags)) {
        val mapped = tags.map({ e: dynamic ->
            val o = js("({})")
            o.id = e.id
            o.name = e.name
            o.description = e.desc
            o.type = "boolean"
            o.get = pushSettingsSelf.getTagEnabled
            o.set = pushSettingsSelf.setTagEnabled
            o
        })
        return pushSettingsSelf.base.concat(
            pushSettingsSelf.tagsHeader,
            mapped,
            pushSettingsSelf.historyPost
        )
    }
    return pushSettingsSelf.base.concat(pushSettingsSelf.offline)
}

private fun pushGetArray(callback: dynamic) {
    js("NetworkUI").progress("正在加载……", {
        var tags = pushApi.tags
        if (!pushTruthy(tags)) {
            try {
                tags = pushApi.getTags()
                pushApi.tags = tags
            } catch (e: dynamic) {
                js("Log").e(e)
            }
        }
        callback(pushBuildSettings(tags))
        js("undefined")
    })
}

private fun pushShowSettings(title: dynamic) {
    if (!pushTruthy(pushSettingsSelf.base)) {
        val base = js("[]")
        val enable = js("({})")
        enable.name = "启用推送"
        enable.type = "boolean"
        enable.get = { pushApi.enabled }
        enable.set = { v: dynamic ->
            js("CA").settings.disablePush = !pushTruthy(v)
            pushApi.load()
            js("undefined")
        }
        base.push(enable)
        pushSettingsSelf.base = base

        val offline = js("[]")
        val offlineText = js("({})")
        offlineText.text = "部分选项因目前处于离线状态而不可用"
        offlineText.type = "text"
        offline.push(offlineText)
        pushSettingsSelf.offline = offline

        val tagsHeader = js("[]")
        val tagsTitle = js("({})")
        tagsTitle.name = "标签管理"
        tagsTitle.type = "tag"
        tagsHeader.push(tagsTitle)
        pushSettingsSelf.tagsHeader = tagsHeader

        val historyPost = js("[]")
        val historyTitle = js("({})")
        historyTitle.name = "历史推送"
        historyTitle.type = "tag"
        historyPost.push(historyTitle)
        val historyEntry = js("({})")
        historyEntry.name = "查看历史推送信息"
        historyEntry.type = "custom"
        historyEntry.onclick = { _fset: dynamic ->
            pushApi.showHistoryPost()
            js("undefined")
        }
        historyPost.push(historyEntry)
        pushSettingsSelf.historyPost = historyPost

        pushSettingsSelf.getTagEnabled = pushThisGetterJs { self: dynamic ->
            pushApi.disabledTags.indexOf(self.id) < 0
        }
        pushSettingsSelf.setTagEnabled = pushThisSetterJs { self: dynamic, v: dynamic ->
            if (pushTruthy(v)) {
                js("Common").removeSet(pushApi.disabledTags, self.id)
            } else {
                js("Common").addSet(pushApi.disabledTags, self.id)
            }
            js("undefined")
        }
        pushSettingsSelf.getArray = { callback: dynamic ->
            pushGetArray(callback)
            js("undefined")
        }
    }
    pushSettingsSelf.getArray({ arr: dynamic ->
        js("Common").showSettings(title, arr)
        js("undefined")
    })
}

private fun pushHistoryVbinder(holder: dynamic, e: dynamic, i: dynamic, a: dynamic) {
    holder.text1.setText(e.name)
    js("Common").applyStyle(holder.text1, "item_default", 3)
    val desc = if (pushLength(e.desc) > 60) e.desc.slice(0, 59) + ".." else e.desc
    holder.text2.setText("发布于" + e.posted + "\n\n" + desc)
    holder.text2.setVisibility(if (pushTruthy(e.desc)) js("G.View.VISIBLE") else js("G.View.GONE"))
}

private fun pushReloadFetch() {
    var first: dynamic = js("undefined")
    var data: dynamic = js("undefined")
    try {
        first = pushApi.getPosts(0, pushApi.perPage, "forward", "desc")
        if (pushLength(first) == pushApi.perPage) {
            data = js("({})")
            data.next = pushParseInt(first[pushLength(first) - 1].id) - 1
        } else {
            data = js("({})")
            data.next = -1
        }
        val pages = js("[]")
        pages.push(first)
        data.pages = pages
    } catch (e: dynamic) {
        js("Log").e(e)
    }
    if (!pushTruthy(data)) {
        js("Common").toast("推送信息列表加载失败")
        return
    }
    pushHistorySelf.postData = data
    js("G").ui({
        try {
            pushHistorySelf.addPageData(first)
            pushHistorySelf.loading = false
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

private fun pushReload() {
    if (pushTruthy(pushHistorySelf.loading)) {
        js("Common").toast("正在加载中……")
        return
    }
    pushHistorySelf.loading = true
    pushHistorySelf.posts.length = 0
    pushHistorySelf.adpt.notifyChange()
    js("NetworkUI").progress("正在加载……", {
        pushReloadFetch()
        js("undefined")
    })
}

private fun pushAppendFetch(): dynamic {
    val page = pushApi.getPosts(
        pushHistorySelf.postData.next,
        pushApi.perPage,
        "backward",
        "desc"
    )
    pushHistorySelf.postData.pages.push(page)
    if (pushLength(page) == pushApi.perPage) {
        pushHistorySelf.postData.next = pushParseInt(page[pushLength(page) - 1].id) - 1
    } else {
        pushHistorySelf.postData.next = -1
    }
    return page
}

private fun pushAddPageData(page: dynamic) {
    val off = pushLength(pushHistorySelf.posts)
    val n = pushLength(page)
    pushHistorySelf.posts.length = off + n
    var i = 0
    while (i < n) {
        pushHistorySelf.posts[i + off] = page[i]
        i++
    }
    pushHistorySelf.adpt.notifyChange()
    val hasNext = pushHistorySelf.postData.next >= 0
    js("NetworkUI").updateMore(
        pushHistorySelf,
        hasNext,
        if (hasNext) "显示下" + pushApi.perPage + "个推送信息……" else null
    )
}

private fun pushAppendPage(sync: dynamic) {
    val opts = js("({})")
    opts.failText = "推送信息列表加载失败"
    opts.fetch = { pushAppendFetch() }
    opts.apply = { page: dynamic ->
        pushHistorySelf.addPageData(page)
        js("undefined")
    }
    js("NetworkUI").appendPage(pushHistorySelf, sync, opts)
}

private fun pushShowHistoryPost(callback: dynamic) {
    js("G").ui({
        try {
            if (!pushTruthy(pushHistorySelf.linear)) {
                val menu = js("[]")
                val refresh = js("({})")
                refresh.text = "刷新"
                refresh.onclick = { _v: dynamic, _tag: dynamic ->
                    pushHistorySelf.reload()
                    js("undefined")
                }
                menu.push(refresh)
                pushHistorySelf.contextMenu = menu

                pushHistorySelf.vmaker = js("NetworkUI").listItem
                pushHistorySelf.vbinder = { holder: dynamic, e: dynamic, i: dynamic, a: dynamic ->
                    pushHistoryVbinder(holder, e, i, a)
                    js("undefined")
                }
                pushHistorySelf.reload = {
                    pushReload()
                    js("undefined")
                }
                pushHistorySelf.appendPage = { sync: dynamic ->
                    pushAppendPage(sync)
                    js("undefined")
                }
                pushHistorySelf.addPageData = { page: dynamic ->
                    pushAddPageData(page)
                    js("undefined")
                }
                pushHistorySelf.posts = js("[]")
                pushHistorySelf.adpt = pushGetListAdapterControllerJs(
                    pushNewSimpleListAdapterJs(
                        pushHistorySelf.posts,
                        pushHistorySelf.vmaker,
                        pushHistorySelf.vbinder
                    )
                )

                js("NetworkUI").createListPage(
                    pushHistorySelf,
                    "推送信息",
                    { _parent: dynamic, data: dynamic ->
                        val intent = pushApi.getIntent(data)
                        if (pushTruthy(intent)) js("AndroidBridge").startActivity(intent)
                        js("undefined")
                    }
                )
            }
            pushHistorySelf.callback = callback
            pushHistorySelf.popup.enter()
            pushHistorySelf.reload()
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

fun installPushService() {
    pushApi.apiHost = js("NetworkUtils").urlBase.api
    pushApi.version = 1
    pushApi.perPage = 20

    pushApi.onCreate = {
        pushOnCreate()
        js("undefined")
    }
    pushApi.initialize = {
        pushInitialize()
        js("undefined")
    }
    pushApi.load = {
        pushLoad()
        js("undefined")
    }
    pushApi.save = {
        pushSave()
        js("undefined")
    }
    pushApi.getIntent = { post: dynamic -> pushGetIntent(post) }
    pushApi.showNotification = { o: dynamic -> pushShowNotification(o) }
    pushApi.cancelNotification = { o: dynamic ->
        pushCancelNotification(o)
        js("undefined")
    }
    pushApi.showDialog = { o: dynamic ->
        pushShowDialog(o)
        js("undefined")
    }
    pushApi.getPosts = { since: dynamic, limit: dynamic, direction: dynamic, sort: dynamic ->
        pushGetPosts(since, limit, direction, sort)
    }
    pushApi.getTags = { pushGetTags() }
    pushApi.readPushs = { pushReadPushs() }
    pushApi.peekLatestPush = { pushPeekLatestPush() }
    pushApi.shouldShowPush = { push: dynamic -> pushShouldShowPush(push) }
    pushApi.shouldShowAsDialog = { push: dynamic -> pushShouldShowAsDialog(push) }
    pushApi.showPushs = { pushs: dynamic ->
        pushShowPushs(pushs)
        js("undefined")
    }
    pushApi.doCheckPush = {
        pushDoCheckPush()
        js("undefined")
    }
    pushApi.notify = {
        pushNotify()
        js("undefined")
    }
    pushApi.showSettings = { title: dynamic ->
        pushShowSettings(title)
        js("undefined")
    }
    pushApi.showHistoryPost = { callback: dynamic ->
        pushShowHistoryPost(callback)
        js("undefined")
    }

    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("PushService", pushApi)
    }
}
