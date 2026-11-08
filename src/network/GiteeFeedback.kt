package ca.kotlin.network

import ca.kotlin.core.*


/**
 * modules/network/GiteeFeedback.js 的 Kotlin/JS 版本。
 *
 * 保持原有对象 API、网络调用与反馈 UI 流程：
 *   - initialize 仍由 MapScript 的 initialize hook 按原位置调用；
 *   - showIssues / showIssueDetail / showRecentFeedback / showFeedbackDetail
 *     原本把状态挂在具名函数表达式 self 上，这里改成稳定的状态对象，
 *     NetworkUI 仍按 self.xxx 的约定读写；
 *   - 所有网络 URL / 参数 / 返回 JSON 处理都与原实现一致；
 *   - Java/JS 数组长度、in、delete、truthy 等语义通过薄 JS helper 保留。
 */
private val giteeApi: dynamic = js("({})")

// 各弹窗函数原先把状态挂在自身函数对象上，这里用独立状态对象保持相同生命周期。
private val giteeIssuesState: dynamic = js("({})")
private val giteeIssueDetailState: dynamic = js("({})")
private val giteeRecentState: dynamic = js("({})")
private val giteeFeedbackDetailState: dynamic = js("({})")

// ---------------- JS 语义 helper ----------------
private val gfTruthyJs: dynamic = js("function (v) { return !!v; }")
private val gfStringJs: dynamic = js("function (v) { return String(v); }")
private val gfLengthJs: dynamic = js("function (v) { return v.length | 0; }")
private val gfForInJs: dynamic = js("function (o, f) { for (var i in o) f(i); }")
private val gfInJs: dynamic = js("function (o, k) { return k in o; }")
private val gfDeleteJs: dynamic = js("function (o, k) { delete o[k]; }")
private val gfIsArrayJs: dynamic = js("function (v) { return Array.isArray(v); }")
private val gfIsNaNNs: dynamic = js("function (v) { return isNaN(v); }")
private val gfLooseEqJs: dynamic = js("function (a, b) { return a == b; }")
private val gfJsonParseJs: dynamic = js("function (v) { return JSON.parse(v); }")
private val gfJsonStringifyJs: dynamic = js("function (v) { return JSON.stringify(v); }")
private val gfNowJs: dynamic = js("function () { return Date.now(); }")
private val gfDateParseJs: dynamic = js("function (v) { return Date.parse(v); }")
private val gfLocaleDateJs: dynamic = js("function (v) { return new Date(v).toLocaleString(); }")
private val gfExecIssueTagJs: dynamic = js("function (text) { return /\\[Issue#(.+)\\]/i.exec(text); }")

private fun gfTruthy(v: dynamic): Boolean = gfTruthyJs(v)
private fun gfString(v: dynamic): dynamic = gfStringJs(v)
private fun gfLength(v: dynamic): Int = gfLengthJs(v)
private fun gfToInt(v: dynamic): Int = js("function (v) { return Number(v) | 0; }")(v)
private fun gfIn(o: dynamic, k: dynamic): Boolean = gfInJs(o, k)
private fun gfDelete(o: dynamic, k: dynamic) {
    gfDeleteJs(o, k)
}
private fun gfIsArray(v: dynamic): Boolean = gfIsArrayJs(v)
private fun gfIsNaN(v: dynamic): Boolean = gfIsNaNNs(v)
private fun gfLooseEq(a: dynamic, b: dynamic): Boolean = gfLooseEqJs(a, b)
private fun gfNow(): dynamic = gfNowJs()
private fun gfDateParse(v: dynamic): dynamic = gfDateParseJs(v)

private fun gfEmptyArray(): dynamic = js("[]")
private fun gfEmptyObject(): dynamic = js("({})")
private fun gfArray3(a: dynamic, b: dynamic, c: dynamic): dynamic = js("function (a, b, c) { return [a, b, c]; }")(a, b, c)
private fun gfNewListAdapter3(a: dynamic, b: dynamic, c: dynamic): dynamic =
    js("function (a, b, c) { return new SimpleListAdapter(a, b, c); }")(a, b, c)
private fun gfNewListAdapter5(a: dynamic, b: dynamic, c: dynamic, d: dynamic, e: dynamic): dynamic =
    js("function (a, b, c, d, e) { return new SimpleListAdapter(a, b, c, d, e); }")(a, b, c, d, e)
private fun gfListAdapterController(a: dynamic): dynamic =
    js("function (a) { return SimpleListAdapter.getController(a); }")(a)
private fun gfNewLinearLayout(context: dynamic): dynamic =
    js("function (c) { return new G.LinearLayout(c); }")(context)
private fun gfNewTextView(context: dynamic): dynamic =
    js("function (c) { return new G.TextView(c); }")(context)
private fun gfNewAbsListViewParams(width: dynamic, height: dynamic): dynamic =
    js("function (w, h) { return new G.AbsListView.LayoutParams(w, h); }")(width, height)
private fun gfNewLinearLayoutParams(width: dynamic, height: dynamic): dynamic =
    js("function (w, h) { return new G.LinearLayout.LayoutParams(w, h); }")(width, height)
private fun gfNewViewIntent(url: dynamic): dynamic = js(
    "function (url) { return new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK); }"
)(url)
private fun gfDp(n: Int): dynamic = js("function (n) { return n * G.dp; }")(n)

private fun gfQueryString(obj: dynamic): dynamic = js("NetworkUtils").toQueryString(obj)
private fun gfJsonParse(s: dynamic): dynamic = gfJsonParseJs(s)
private fun gfJsonStringify(o: dynamic): dynamic = gfJsonStringifyJs(o)
private fun gfShowProgress(text: dynamic, task: dynamic) {
    js("NetworkUI").progress(text, task)
}
private fun gfRejectDelay(count: Int): dynamic =
    js("function (n) { return 24 * 3600 * 1000 * Math.pow(3, n); }")(count)


// ---------------- 登录 / 存储 ----------------
private fun gfInitialize() {
    if (js("MapScript").host == "Android") {
        giteeApi.clientId = gfString(js("ScriptInterface").getGiteeClientId())
        giteeApi.clientSecret = gfString(js("ScriptInterface").getGiteeClientSecret())
        giteeApi.redirectUrl = "https://projectxero.gitee.io/ca/feedback.html"
    }
    if (js("CA").settingsVersion < gfDateParse("2019-04-05")) {
        gfUpgradeRecentFeedback()
    }
}

private fun gfUpgradeRecentFeedback() {
    val old = js("CA").settings.recentFeedback
    if (!gfTruthy(old)) return
    // 原实现把转换结果写进局部 r 后没有回写 settings，这里保持同样的行为。
    val r = gfEmptyObject()
    var i = 0
    val n = gfLength(old)
    while (i < n) {
        val entry = gfEmptyObject()
        entry.lastModified = old[i].lastModified
        r[old[i].number] = entry
        i++
    }
}

private fun gfGetAuthorizeUrl(): dynamic {
    val query = gfEmptyObject()
    query.client_id = giteeApi.clientId
    query.redirect_uri = giteeApi.redirectUrl
    query.response_type = "code"
    return "https://gitee.com/oauth/authorize?" + gfQueryString(query)
}

private fun gfAcquireAccessTokenAnonymous() {
    giteeApi.accessType = "anonymous"
    giteeApi.accessToken = js("undefined")
    giteeApi.accessData = null
}

private fun gfAcquireAccessTokenOAuth(authorizationCode: dynamic) {
    val query = gfEmptyObject()
    query.grant_type = "authorization_code"
    query.code = authorizationCode
    query.client_id = giteeApi.clientId
    query.redirect_uri = giteeApi.redirectUrl
    query.client_secret = giteeApi.clientSecret
    val d = gfJsonParse(js("NetworkUtils").postPage("https://gitee.com/oauth/token?" + gfQueryString(query)))
    giteeApi.accessType = "oauth"
    giteeApi.accessToken = d.access_token
    d.expiredDate = d.created_at + d.expires_in
    giteeApi.accessData = d
}

private fun gfAcquireAccessToken(userName: dynamic, password: dynamic) {
    val query = gfEmptyObject()
    query.grant_type = "password"
    query.username = userName
    query.password = password
    query.client_id = giteeApi.clientId
    query.client_secret = giteeApi.clientSecret
    query.scope = "user_info issues notes"
    val d = gfJsonParse(
        js("NetworkUtils").postPage(
            "https://gitee.com/oauth/token",
            gfQueryString(query),
            "application/x-www-form-urlencoded"
        )
    )
    giteeApi.accessType = "basic"
    giteeApi.accessToken = d.access_token
    d.expiredDate = d.created_at + d.expires_in
    giteeApi.accessData = d
}

private fun gfRefreshAccessToken(force: dynamic): dynamic {
    val data = giteeApi.accessData
    if (gfTruthy(data)) {
        if (gfTruthy(force) || data.expiredDate < gfNow()) {
            val query = gfEmptyObject()
            query.grant_type = "refresh_token"
            query.refresh_token = data.refresh_token
            val d = gfJsonParse(js("NetworkUtils").postPage("https://gitee.com/oauth/token?" + gfQueryString(query)))
            giteeApi.accessToken = d.access_token
            d.expiredDate = d.created_at + d.expires_in
            giteeApi.accessData = d
            return 1
        }
        return 0
    }
    return -1
}

private fun gfGetRecentFeedback(state: dynamic): dynamic {
    var a = js("CA").settings.recentFeedback
    if (gfTruthy(giteeApi.userInfo)) {
        var r: dynamic = null
        try {
            r = giteeApi.getUserIssues(giteeApi.userInfo.name, state, 1)
        } catch (e: dynamic) {
            js("Log").e(e)
        }
        if (!gfTruthy(r)) r = gfEmptyArray()
        if (!gfTruthy(a)) a = gfEmptyObject()
        var i = 0
        val n = gfLength(r)
        while (i < n) {
            val e = r[i]
            e.updated_utc = gfDateParse(e.updated_at)
            if (gfIn(a, e.number)) {
                e.lastModified = a[e.number].lastModified
            } else {
                e.lastModified = e.updated_utc
            }
            e.isNew = e.lastModified < e.updated_utc
            i++
        }
        return r
    } else if (gfTruthy(a)) {
        val r = gfEmptyArray()
        gfForInJs(a) { i: dynamic ->
            try {
                val data = giteeApi.getIssue(i)
                if (!(!gfLooseEq(state, "all") && !gfLooseEq(data.state, state))) {
                    data.updated_utc = gfDateParse(data.updated_at)
                    data.lastModified = a[i].lastModified
                    data.isNew = data.lastModified < data.updated_utc
                    r.push(data)
                }
            } catch (e: dynamic) {
                js("Log").e(e)
            }
        }
        r.sort({ a1: dynamic, b1: dynamic -> b1.updated_utc - a1.updated_utc })
        val maxCount = gfToInt(giteeApi.maxFeedbackCount)
        if (gfLength(r) > maxCount) {
            var i = maxCount
            while (i < gfLength(r)) {
                gfDelete(a, r[i].number)
                i++
            }
            r.length = maxCount
        }
        return r
    }
    return gfEmptyArray()
}

private fun gfUpdateRecentFeedback(data: dynamic) {
    if (!gfTruthy(js("CA").settings.recentFeedback)) js("CA").settings.recentFeedback = gfEmptyObject()
    val a = js("CA").settings.recentFeedback
    if (gfIn(a, data.number)) {
        a[data.number].lastModified = gfDateParse(data.updated_at)
    } else {
        val entry = gfEmptyObject()
        entry.lastModified = gfDateParse(data.updated_at)
        a[data.number] = entry
    }
}

// ---------------- Gitee API ----------------
private fun gfGetUserInfo(): dynamic {
    val query = gfEmptyObject()
    query.access_token = giteeApi.accessToken
    return gfJsonParse(js("NetworkUtils").queryPage("https://gitee.com/api/v5/user?" + gfQueryString(query)))
}

private fun gfGetIssues(state: dynamic, page: dynamic): dynamic {
    val query = gfEmptyObject()
    query.state = state
    query.sort = "created"
    query.direction = "desc"
    query.page = page
    query.per_page = giteeApi.perPage
    return gfJsonParse(
        js("NetworkUtils").queryPage(
            "https://gitee.com/api/v5/repos/" + giteeApi.targetOwner + "/" + giteeApi.targetRepo +
                "/issues?" + gfQueryString(query)
        )
    )
}

private fun gfGetUserIssues(userName: dynamic, state: dynamic, page: dynamic): dynamic {
    val query = gfEmptyObject()
    query.state = state
    query.sort = "updated"
    query.direction = "desc"
    query.page = page
    query.per_page = giteeApi.maxFeedbackCount
    query.creator = userName
    return gfJsonParse(
        js("NetworkUtils").queryPage(
            "https://gitee.com/api/v5/repos/" + giteeApi.targetOwner + "/" + giteeApi.targetRepo +
                "/issues?" + gfQueryString(query)
        )
    )
}

private fun gfCreateIssue(title: dynamic, body: dynamic): dynamic {
    val data = gfEmptyObject()
    data["access_token"] = giteeApi.accessToken
    data["repo"] = giteeApi.targetRepo
    data["title"] = title
    data["body"] = body
    return gfJsonParse(
        js("NetworkUtils").postPage(
            "https://gitee.com/api/v5/repos/" + giteeApi.targetOwner + "/issues",
            gfJsonStringify(data),
            "application/json;charset=UTF-8"
        )
    )
}

private fun gfGetIssue(number: dynamic): dynamic {
    return gfJsonParse(
        js("NetworkUtils").queryPage(
            "https://gitee.com/api/v5/repos/" + giteeApi.targetOwner + "/" + giteeApi.targetRepo +
                "/issues/" + number
        )
    )
}

private fun gfUpdateIssue(number: dynamic, map: dynamic): dynamic {
    val data = gfEmptyObject()
    data["access_token"] = giteeApi.accessToken
    data["repo"] = giteeApi.targetRepo
    data["title"] = map.title
    data["body"] = map.body
    data["state"] = map.state
    return gfJsonParse(
        js("NetworkUtils").request(
            "https://gitee.com/api/v5/repos/" + giteeApi.targetOwner + "/issues/" + number,
            "PATCH",
            gfJsonStringify(data),
            "application/json;charset=UTF-8"
        )
    )
}

private fun gfGetIssueComment(id: dynamic): dynamic {
    return gfJsonParse(
        js("NetworkUtils").queryPage(
            "https://gitee.com/api/v5/repos/" + giteeApi.targetOwner + "/" + giteeApi.targetRepo +
                "/issues/comments/" + id
        )
    )
}

private fun gfGetIssueComments(number: dynamic, page: dynamic): dynamic {
    val query = gfEmptyObject()
    query.page = page
    query.per_page = giteeApi.perPage
    return gfJsonParse(
        js("NetworkUtils").queryPage(
            "https://gitee.com/api/v5/repos/" + giteeApi.targetOwner + "/" + giteeApi.targetRepo +
                "/issues/" + number + "/comments?" + gfQueryString(query)
        )
    )
}

private fun gfCreateIssueComment(number: dynamic, body: dynamic): dynamic {
    val data = gfEmptyObject()
    data["access_token"] = giteeApi.accessToken
    data["body"] = body
    return gfJsonParse(
        js("NetworkUtils").postPage(
            "https://gitee.com/api/v5/repos/" + giteeApi.targetOwner + "/" + giteeApi.targetRepo +
                "/issues/" + number + "/comments",
            gfJsonStringify(data),
            "application/json;charset=UTF-8"
        )
    )
}

private fun gfUpdateIssueComment(id: dynamic, body: dynamic): dynamic {
    val data = gfEmptyObject()
    data["access_token"] = giteeApi.accessToken
    data["body"] = body
    return gfJsonParse(
        js("NetworkUtils").request(
            "https://gitee.com/api/v5/repos/" + giteeApi.targetOwner + "/" + giteeApi.targetRepo +
                "/issues/comments/" + id,
            "PATCH",
            gfJsonStringify(data),
            "application/json;charset=UTF-8"
        )
    )
}

private fun gfDeleteIssueComment(id: dynamic): dynamic {
    val data = gfEmptyObject()
    data["access_token"] = giteeApi.accessToken
    return gfJsonParse(
        js("NetworkUtils").request(
            "https://gitee.com/api/v5/repos/" + giteeApi.targetOwner + "/" + giteeApi.targetRepo +
                "/issues/comments/" + id,
            "DELETE",
            gfJsonStringify(data),
            "application/json;charset=UTF-8"
        )
    )
}

// ---------------- Issues 列表 ----------------
private fun gfShowIssues(callback: dynamic) {
    js("G").ui({
        try {
            gfShowIssuesImpl(callback)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

private fun gfShowIssuesImpl(callback: dynamic) {
    val state = giteeIssuesState
    if (!gfTruthy(state.linear)) {
        val menu = gfEmptyArray()
        val refresh = gfEmptyObject()
        refresh.text = "刷新"
        refresh.onclick = { _v: dynamic, _tag: dynamic ->
            gfIssuesReload()
            js("undefined")
        }
        menu.push(refresh)
        state.contextMenu = menu.concat(js("NetworkUI").issueStateMenu(state))
        state.vmaker = js("NetworkUI").listItem
        state.vbinder = { holder: dynamic, e: dynamic, _i: dynamic, _a: dynamic ->
            holder.text1.setText(if (e.state == "open") e.title else "[已处理]" + e.title)
            js("Common").applyStyle(
                holder.text1,
                if (e.state == "open") "item_default" else "item_disabled",
                3
            )
            val body = e.body
            holder.text2.setText(
                if (gfTruthy(body)) {
                    if (gfLength(body) > 60) body.slice(0, 59) + ".." else body
                } else {
                    "(无)"
                }
            )
            js("undefined")
        }
        state.reload = {
            gfIssuesReload()
            js("undefined")
        }
        state.appendPage = { sync: dynamic ->
            gfIssuesAppendPage(sync)
            js("undefined")
        }
        state.issues = gfEmptyArray()
        state.adpt = gfListAdapterController(gfNewListAdapter3(state.issues, state.vmaker, state.vbinder))
        state.issueState = "all"

        js("NetworkUI").createListPage(state, "Issues", { _parent: dynamic, data: dynamic ->
            giteeApi.showIssueDetail(data.number)
            js("undefined")
        })
    }
    state.callback = callback
    state.popup.enter()
    gfIssuesReload()
}

private fun gfIssuesReload() {
    val state = giteeIssuesState
    if (gfTruthy(state.loading)) {
        js("Common").toast("正在加载中……")
        return
    }
    state.loading = true
    state.issues.length = 0
    state.adpt.notifyChange()
    gfShowProgress("正在加载……", {
        gfIssuesReloadTask()
        js("undefined")
    })
}

private fun gfIssuesReloadTask() {
    val state = giteeIssuesState
    var data: dynamic = null
    try {
        val pages = gfEmptyArray()
        pages.push(giteeApi.getIssues(state.issueState, 1))
        data = gfEmptyObject()
        data.next = 1
        data.pages = pages
    } catch (e: dynamic) {
        js("Log").e(e)
    }
    if (!gfTruthy(data)) {
        js("Common").toast("Issue列表加载失败")
        return
    }
    state.issueData = data
    state.loading = false
    gfIssuesAppendPage(true)
}

private fun gfIssuesAppendPage(sync: dynamic) {
    val state = giteeIssuesState
    val opts = gfEmptyObject()
    opts.failText = "Issue列表加载失败"
    opts.fetch = {
        gfIssuesFetchPage()
    }
    opts.apply = { page: dynamic ->
        gfIssuesApplyPage(page)
        js("undefined")
    }
    js("NetworkUI").appendPage(state, sync, opts)
}

private fun gfIssuesFetchPage(): dynamic {
    val state = giteeIssuesState
    var page: dynamic
    if (state.issueData.next < gfLength(state.issueData.pages)) {
        page = state.issueData.pages[state.issueData.next]
    } else {
        page = giteeApi.getIssues(state.issueState, state.issueData.next)
        state.issueData.pages[state.issueData.next] = page
    }
    if (gfLength(page) != 0) {
        state.issueData.next = state.issueData.next + 1
        if (state.issueData.next >= gfLength(state.issueData.pages)) {
            state.issueData.pages[state.issueData.next] =
                giteeApi.getIssues(state.issueState, state.issueData.next)
        }
        if (gfLength(state.issueData.pages[state.issueData.next]) == 0) state.issueData.next = js("NaN")
    } else {
        state.issueData.next = js("NaN")
    }
    return page
}

private fun gfIssuesApplyPage(page: dynamic) {
    val state = giteeIssuesState
    val off = gfLength(state.issues)
    state.issues.length = off + gfLength(page)
    var i = 0
    while (i < gfLength(page)) {
        state.issues[i + off] = page[i]
        i++
    }
    state.adpt.notifyChange()
    js("NetworkUI").updateMore(
        state,
        state.issueData.next,
        if (gfTruthy(state.issueData.next)) "显示下" + giteeApi.perPage + "个Issues……" else null
    )
}

// ---------------- 单个 Issue 详情 ----------------
private fun gfShowIssueDetail(number: dynamic, callback: dynamic) {
    js("G").ui({
        try {
            gfShowIssueDetailImpl(number, callback)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

private fun gfShowIssueDetailImpl(number: dynamic, callback: dynamic) {
    val state = giteeIssueDetailState
    if (!gfTruthy(state.linear)) {
        val menu = gfEmptyArray()
        val refresh = gfEmptyObject()
        refresh.text = "刷新"
        refresh.onclick = { _v: dynamic, _tag: dynamic ->
            gfIssueDetailReload()
            js("undefined")
        }
        menu.push(refresh)

        val desc = gfEmptyObject()
        desc.text = "倒序浏览"
        desc.hidden = { gfLooseEq(state.viewOrder, "desc") }
        desc.onclick = { _v: dynamic, _tag: dynamic ->
            state.viewOrder = "desc"
            gfIssueDetailReload()
            js("undefined")
        }
        menu.push(desc)

        val asc = gfEmptyObject()
        asc.text = "正序浏览"
        asc.hidden = { gfLooseEq(state.viewOrder, "asc") }
        asc.onclick = { _v: dynamic, _tag: dynamic ->
            state.viewOrder = "asc"
            gfIssueDetailReload()
            js("undefined")
        }
        menu.push(asc)
        state.contextMenu = menu

        state.vmaker = { holder: dynamic -> gfIssueDetailVmaker(holder) }
        state.vbinder = { holder: dynamic, e: dynamic, _i: dynamic, _a: dynamic ->
            holder.linear.setGravity(
                if (gfTruthy(e.fromThis) && gfLooseEq(state.viewOrder, "asc")) {
                    js("G.Gravity.RIGHT")
                } else {
                    js("G.Gravity.LEFT")
                }
            )
            holder.text1.setText(e.user.name)
            js("Common").applyStyle(holder.text1, "item_default", 3)
            holder.text2.setText(e.body)
            js("undefined")
        }
        state.reload = {
            gfIssueDetailReload()
            js("undefined")
        }
        state.appendPage = { sync: dynamic ->
            gfIssueDetailAppendPage(sync)
            js("undefined")
        }
        state.comments = gfEmptyArray()
        state.adpt = gfListAdapterController(gfNewListAdapter3(state.comments, state.vmaker, state.vbinder))
        state.viewOrder = "asc"

        state.issueBody = gfNewTextView(js("ctx"))
        state.issueBody.setPadding(gfDp(15), gfDp(15), gfDp(15), gfDp(15))
        state.issueBody.setLayoutParams(gfNewAbsListViewParams(-1, -2))
        js("Common").applyStyle(state.issueBody, "textview_default", 2)

        val pageOpts = gfEmptyObject()
        pageOpts.headerView = state.issueBody
        pageOpts.dividerHeight = 0
        pageOpts.popupKey = "feedback.IssueDetail"
        js("NetworkUI").createListPage(state, "", { _parent: dynamic, _data: dynamic ->
            js("undefined")
        }, pageOpts)
    }
    state.currentNumber = number
    state.callback = callback
    state.popup.enter()
    gfIssueDetailReload()
}

private fun gfIssueDetailVmaker(holder: dynamic): dynamic {
    val context = js("ctx")
    val layout = gfNewLinearLayout(context)
    holder.linear = layout
    val text1 = gfNewTextView(context)
    holder.text1 = text1
    val text2 = gfNewTextView(context)
    holder.text2 = text2
    layout.setLayoutParams(gfNewAbsListViewParams(-1, -2))
    layout.setOrientation(js("G.LinearLayout.VERTICAL"))
    layout.setPadding(gfDp(15), gfDp(15), gfDp(15), gfDp(15))
    text1.setLayoutParams(gfNewLinearLayoutParams(-2, -2))
    text1.setEllipsize(js("G.TextUtils.TruncateAt.END"))
    text1.setSingleLine(true)
    layout.addView(text1)
    text2.setPadding(0, gfDp(5), 0, 0)
    text2.setLayoutParams(gfNewLinearLayoutParams(-2, -2))
    js("Common").applyStyle(text2, "textview_prompt", 1)
    layout.addView(text2)
    return layout
}

private fun gfIssueDetailReload() {
    val state = giteeIssueDetailState
    if (gfTruthy(state.loading)) {
        js("Common").toast("正在加载中……")
        return
    }
    state.loading = true
    state.comments.length = 0
    state.adpt.notifyChange()
    gfShowProgress("正在加载……", {
        gfIssueDetailReloadTask()
        js("undefined")
    })
}

private fun gfIssueDetailReloadTask() {
    val state = giteeIssueDetailState
    var data: dynamic = null
    try {
        val topic = giteeApi.getIssue(state.currentNumber)
        data = gfEmptyObject()
        data.topic = topic
        data.totalPages = js("Math").ceil(topic.comments / giteeApi.perPage)
        data.next = if (gfLooseEq(state.viewOrder, "desc")) data.totalPages else 1
    } catch (e: dynamic) {
        js("Log").e(e)
    }
    state.loading = false
    if (!gfTruthy(data)) {
        js("Common").toast("评论列表加载失败")
        return
    }
    state.commentData = data
    js("G").ui({
        try {
            state.title.setText(data.topic.title)
            state.issueBody.setText(
                data.topic.body + "\n\n#" + data.topic.number + " by " + data.topic.user.name +
                    "\n" + gfLocaleDateJs(data.topic.created_at)
            )
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
    gfIssueDetailAppendPage(true)
}

private fun gfIssueDetailAppendPage(sync: dynamic) {
    val state = giteeIssueDetailState
    val opts = gfEmptyObject()
    opts.failText = "评论列表加载失败"
    opts.fetch = {
        giteeApi.getIssueComments(state.currentNumber, state.commentData.next)
    }
    opts.apply = { page: dynamic ->
        val off = gfLength(state.comments)
        state.comments.length = off + gfLength(page)
        if (gfLooseEq(state.viewOrder, "desc")) {
            page.reverse()
            if (state.commentData.next > 1) {
                state.commentData.next = state.commentData.next - 1
            } else {
                state.commentData.next = js("NaN")
            }
        } else {
            if (state.commentData.next < state.commentData.totalPages) {
                state.commentData.next = state.commentData.next + 1
            } else {
                state.commentData.next = js("NaN")
            }
        }
        var i = 0
        while (i < gfLength(page)) {
            page[i].fromThis = gfLooseEq(page[i].user.id, giteeApi.myUserId)
            state.comments[i + off] = page[i]
            i++
        }
        state.adpt.notifyChange()
        js("NetworkUI").updateMore(
            state,
            state.commentData.next,
            if (gfTruthy(state.commentData.next)) {
                "显示剩下" + (state.commentData.totalPages - state.commentData.next + 1) + "页……"
            } else {
                null
            }
        )
        js("undefined")
    }
    js("NetworkUI").appendPage(state, sync, opts)
}

// ---------------- 最近反馈 ----------------
private fun gfShowRecentFeedback(callback: dynamic) {
    js("G").ui({
        try {
            gfShowRecentFeedbackImpl(callback)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

private fun gfShowRecentFeedbackImpl(callback: dynamic) {
    val state = giteeRecentState
    if (!gfTruthy(state.linear)) {
        val menu = gfEmptyArray()
        val refresh = gfEmptyObject()
        refresh.text = "刷新"
        refresh.onclick = { _v: dynamic, _tag: dynamic ->
            gfRecentRefresh()
            js("undefined")
        }
        menu.push(refresh)

        val switchAccount = gfEmptyObject()
        switchAccount.text = "切换账号"
        switchAccount.onclick = { _v: dynamic, _tag: dynamic ->
            giteeApi.showLogin({
                gfRecentReload()
                js("undefined")
            })
            js("undefined")
        }
        menu.push(switchAccount)

        val extra = gfEmptyArray()
        val allFeedback = gfEmptyObject()
        allFeedback.text = "查看所有反馈"
        allFeedback.onclick = { _v: dynamic, _tag: dynamic ->
            giteeApi.showIssues()
            js("undefined")
        }
        extra.push(allFeedback)

        val faq = gfEmptyObject()
        faq.text = "查看常见问题解答(FAQ)"
        faq.onclick = { _v: dynamic, _tag: dynamic ->
            js("AndroidBridge").startActivity(
                gfNewViewIntent("https://gitee.com/projectxero/ca/wikis/pages?title=FAQ")
            )
            js("undefined")
        }
        extra.push(faq)

        val agreement = gfEmptyObject()
        agreement.text = "查看反馈说明"
        agreement.onclick = { _v: dynamic, _tag: dynamic ->
            giteeApi.showAgreement()
            js("undefined")
        }
        extra.push(agreement)

        state.contextMenu = menu.concat(js("NetworkUI").issueStateMenu(state), extra)
        state.vmaker = js("NetworkUI").listItem
        state.vbinder = { holder: dynamic, e: dynamic, _i: dynamic, _a: dynamic ->
            val prefix = if (e.state == "open") {
                if (gfTruthy(e.isNew)) "[有新消息]" else ""
            } else if (e.state == "rejected") {
                "[已拒绝]"
            } else {
                "[已处理]"
            }
            holder.text1.setText(prefix + e.title)
            js("Common").applyStyle(
                holder.text1,
                if (e.state == "open") "item_default" else "item_disabled",
                3
            )
            holder.text2.setText("最近更新于 " + js("Updater").toChineseDate(e.updated_utc))
            js("undefined")
        }
        state.reload = {
            gfRecentReload()
            js("undefined")
        }
        state.addIssue = {
            gfRecentAddIssue()
            js("undefined")
        }
        state.adpt = gfListAdapterController(
            gfNewListAdapter5(gfEmptyArray(), state.vmaker, state.vbinder, null, true)
        )
        state.issueState = "all"

        state.add = gfNewTextView(js("ctx"))
        state.add.setGravity(js("G.Gravity.CENTER"))
        state.add.setText("新建反馈话题")
        state.add.setPadding(gfDp(15), gfDp(15), gfDp(15), gfDp(15))
        state.add.setLayoutParams(gfNewAbsListViewParams(-1, -2))
        js("Common").applyStyle(state.add, "textview_prompt", 2)

        val recentPageOpts = gfEmptyObject()
        recentPageOpts.headerView = state.add
        recentPageOpts.popupKey = "feedback.Recent"
        js("NetworkUI").createListPage(
            state,
            "最近反馈",
            { _parent: dynamic, data: dynamic, view: dynamic ->
                if (gfLooseEq(view, state.add)) {
                    state.addIssue()
                } else {
                    giteeApi.showFeedbackDetail(data.number, 0, {
                        gfRecentReload()
                        js("undefined")
                    })
                }
                js("undefined")
            },
            recentPageOpts
        )
    }
    state.callback = callback
    state.popup.enter()
    gfRecentReload()
}

private fun gfRecentRefresh() {
    val state = giteeRecentState
    if (state.nextReload > gfNow()) {
        js("Common").toast("请不要频繁刷新页面")
        return
    }
    gfRecentReload()
}

private fun gfRecentReload() {
    val state = giteeRecentState
    if (gfTruthy(state.loading)) {
        js("Common").toast("正在加载中……")
        return
    }
    state.loading = true
    state.adpt.setArray(gfEmptyArray())
    gfShowProgress("正在加载……", {
        gfRecentReloadTask()
        js("undefined")
    })
}

private fun gfRecentReloadTask() {
    val state = giteeRecentState
    val a = giteeApi.getRecentFeedback(state.issueState)
    var rejectCount = 0
    var rejectLatest: dynamic = js("-Infinity")
    var latest: dynamic = js("-Infinity")
    var i = 0
    val n = gfLength(a)
    while (i < n) {
        val e = a[i]
        e.created_utc = gfDateParse(e.created_at)
        latest = js("Math").max(latest, e.created_utc)
        if (e.state == "rejected") {
            rejectCount++
            rejectLatest = js("Math").max(rejectLatest, e.created_utc)
        }
        i++
    }
    state.rejectTime = if (rejectCount > 2) {
        rejectLatest + gfRejectDelay(rejectCount - 3)
    } else {
        js("-Infinity")
    }
    state.nextAdd = latest + 60 * 1000
    state.nextReload = gfNow() + 20 * 1000
    try {
        giteeApi.myUserId = if (gfTruthy(giteeApi.userInfo)) {
            giteeApi.userInfo.id
        } else {
            giteeApi.getUserInfo().id
        }
    } catch (e: dynamic) {
        js("Log").e(e)
    }
    state.loading = false
    js("G").ui({
        try {
            state.title.setText(
                "最近反馈 - " + (if (gfTruthy(giteeApi.userInfo)) giteeApi.userInfo.name else "匿名")
            )
            state.adpt.setArray(a)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

private fun gfRecentAddIssue() {
    val state = giteeRecentState
    if (!gfTruthy(giteeApi.accessToken)) {
        js("Common").toast("您尚未登录，因此不能创建反馈")
        return
    }
    if (state.rejectTime > gfNow()) {
        js("Common").toast("因为您发布了无效的反馈，为防止服务器资源继续被浪费，您已被暂时禁止发布反馈！")
        return
    }
    if (state.nextAdd > gfNow()) {
        js("Common").toast("服务器忙，请1分钟后重试")
        return
    }
    val o = gfEmptyObject()
    o.title = ""
    o.body = ""
    o.newIssue = true
    giteeApi.showEditIssue(o, { submitted: dynamic ->
        gfRecentCreateIssue(submitted)
        js("undefined")
    })
}

private fun gfRecentCreateIssue(o: dynamic) {
    gfShowProgress("正在创建……", {
        gfRecentCreateIssueTask(o)
        js("undefined")
    })
}

private fun gfRecentCreateIssueTask(o: dynamic) {
    var d: dynamic = null
    try {
        d = giteeApi.createIssue(o.title, o.body)
    } catch (e: dynamic) {
        js("Log").e(e)
    }
    if (!gfTruthy(d)) {
        js("Common").toast("话题创建失败")
        return
    }
    js("java.lang.Thread").sleep(giteeApi.databaseDelay) //等待数据库更新
    giteeApi.updateRecentFeedback(d)
    js("G").ui({
        try {
            gfRecentReload()
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

// ---------------- 反馈详情 ----------------
private fun gfShowFeedbackDetail(number: dynamic, readOnly: dynamic, callback: dynamic) {
    js("G").ui({
        try {
            gfShowFeedbackDetailImpl(number, readOnly, callback)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

private fun gfShowFeedbackDetailImpl(number: dynamic, readOnly: dynamic, callback: dynamic) {
    val state = giteeFeedbackDetailState
    if (!gfTruthy(state.popup)) {
        state.getWritable = { gfFeedbackGetWritable() }
        state.setState = { newState: dynamic ->
            gfFeedbackSetState(newState)
            js("undefined")
        }

        val menu = gfEmptyArray()
        val refresh = gfEmptyObject()
        refresh.text = "刷新"
        refresh.onclick = { _v: dynamic, _tag: dynamic ->
            gfFeedbackReload()
            js("undefined")
        }
        menu.push(refresh)

        val edit = gfEmptyObject()
        edit.text = "编辑主题"
        edit.hidden = { !gfTruthy(state.writable) }
        edit.onclick = { _v: dynamic, _tag: dynamic ->
            gfFeedbackEditTopic()
            js("undefined")
        }
        menu.push(edit)

        val markClosed = gfEmptyObject()
        markClosed.text = "标记为已处理"
        markClosed.hidden = { !gfTruthy(state.writable) }
        markClosed.onclick = { _v: dynamic, _tag: dynamic ->
            val dialog = gfEmptyObject()
            dialog.title = "确定将该反馈标记为已处理？"
            dialog.description = "*此操作无法撤销，且标记之后您将无法编辑反馈"
            dialog.callback = { id: dynamic ->
                gfFeedbackConfirmMarkClosed(state, id)
                js("undefined")
            }
            js("Common").showConfirmDialog(dialog)
            js("undefined")
        }
        menu.push(markClosed)
        state.contextMenu = menu

        state.vmaker = { holder: dynamic ->
            val opts = gfEmptyObject()
            val padding = gfEmptyArray()
            padding.push(gfDp(20))
            padding.push(gfDp(15))
            padding.push(gfDp(20))
            padding.push(gfDp(15))
            opts.padding = padding
            opts.width = -2
            opts.text1Style = "item_default"
            opts.text1FontSize = 3
            js("NetworkUI").listItem(holder, opts)
        }
        state.vbinder = { holder: dynamic, e: dynamic, _i: dynamic, _a: dynamic ->
            holder.linear.setGravity(
                if (gfTruthy(e.fromThis)) js("G.Gravity.RIGHT") else js("G.Gravity.LEFT")
            )
            holder.text1.setText(
                if (gfTruthy(e.fromThis) && !gfTruthy(giteeApi.userInfo)) {
                    "匿名用户"
                } else {
                    e.user.name
                }
            )
            holder.text2.setText(e.body)
            js("undefined")
        }
        state.reload = {
            gfFeedbackReload()
            js("undefined")
        }
        state.appendPage = { sync: dynamic ->
            gfFeedbackAppendPage(sync)
            js("undefined")
        }
        state.addComment = { text: dynamic ->
            gfFeedbackAddComment(text)
            js("undefined")
        }
        state.clickData = { data: dynamic, _remote: dynamic ->
            gfFeedbackClickData(data)
        }
        state.comments = gfEmptyArray()
        state.adpt = gfListAdapterController(gfNewListAdapter3(state.comments, state.vmaker, state.vbinder))

        val talkOpts = gfEmptyObject()
        talkOpts.hint = "发送评论"
        talkOpts.onSend = { s: dynamic ->
            state.addComment(s)
            state.talkbox.text = ""
            js("undefined")
        }
        state.talkBar = js("NetworkUI").commentBar(talkOpts)
        state.talk = state.talkBar.view
        state.talkbox = state.talkBar.input

        val moreOpts = gfEmptyObject()
        moreOpts.gravity = js("L").Gravity("center")
        moreOpts.text = "显示更多"
        val morePadding = gfEmptyArray()
        morePadding.push(gfDp(15))
        morePadding.push(gfDp(15))
        morePadding.push(gfDp(15))
        morePadding.push(gfDp(15))
        moreOpts.padding = morePadding
        val moreLayout = gfEmptyObject()
        moreLayout.width = -1
        moreLayout.height = -2
        moreOpts.layout = moreLayout
        moreOpts.style = "textview_prompt"
        moreOpts.fontSize = 2
        state.more = js("L").TextView(moreOpts)

        val listOpts = gfEmptyObject()
        listOpts.adapter = state.adpt.self
        listOpts.dividerHeight = 0
        listOpts.stackFromBottom = true
        val listLayout = gfEmptyObject()
        listLayout.width = -1
        listLayout.height = 0
        listLayout.weight = 1.0
        listOpts.layout = listLayout
        listOpts.onItemClick = { _parent: dynamic, view: dynamic, pos: dynamic, _id: dynamic ->
            gfFeedbackOnItemClick(view, pos)
            js("undefined")
        }
        listOpts._talkView = state.talk
        listOpts._moreView = state.more
        state.list = js("L").ListView(listOpts)

        val children = gfEmptyArray()
        children.push(js("NetworkUI").issueDetailHeader(state, false))
        children.push(state.list)
        val pageOpts = gfEmptyObject()
        pageOpts.closeButton = true
        js("NetworkUI").issueDetailPage(state, children, pageOpts)
    }
    state.currentNumber = number
    state.readOnly = readOnly
    state.callback = callback
    state.popup.enter()
    gfFeedbackReload()
}

private fun gfFeedbackGetWritable(): dynamic {
    val state = giteeFeedbackDetailState
    if (!gfTruthy(giteeApi.userInfo)) return false
    if (state.readOnly > 0) {
        return true
    } else if (state.readOnly < 0) {
        return false
    } else {
        val t = state.commentData
        return if (gfTruthy(t)) gfLooseEq(t.topic.state, "open") else t
    }
}

private fun gfFeedbackConfirmMarkClosed(state: dynamic, id: dynamic) {
    if (id != 0) return
    state.setState("closed")
}

private fun gfFeedbackSetState(newState: dynamic) {
    val state = giteeFeedbackDetailState
    val topic = state.commentData.topic
    gfShowProgress("正在标记……", {
        gfFeedbackSetStateTask(topic, newState)
        js("undefined")
    })
}

private fun gfFeedbackSetStateTask(topic: dynamic, newState: dynamic) {
    val map = gfEmptyObject()
    map.state = newState
    var d: dynamic = null
    try {
        d = giteeApi.updateIssue(topic.number, map)
    } catch (e: dynamic) {
        js("Log").e(e)
    }
    if (!gfTruthy(d)) {
        js("Common").toast("话题修改失败")
        return
    }
    js("java.lang.Thread").sleep(giteeApi.databaseDelay) //等待数据库更新
    js("G").ui({
        try {
            gfFeedbackReload()
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

private fun gfFeedbackEditTopic() {
    val state = giteeFeedbackDetailState
    val topic = state.commentData.topic
    giteeApi.showEditIssue(topic, { _submitted: dynamic ->
        gfShowProgress("正在保存……", {
            gfFeedbackSaveTopicTask(topic)
            js("undefined")
        })
        js("undefined")
    })
}

private fun gfFeedbackSaveTopicTask(topic: dynamic) {
    var d: dynamic = null
    try {
        d = giteeApi.updateIssue(topic.number, topic)
    } catch (e: dynamic) {
        js("Log").e(e)
    }
    if (!gfTruthy(d)) {
        js("Common").toast("话题修改失败")
        return
    }
    js("java.lang.Thread").sleep(giteeApi.databaseDelay) //等待数据库更新
    js("G").ui({
        try {
            gfFeedbackReload()
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

private fun gfFeedbackReload() {
    val state = giteeFeedbackDetailState
    if (gfTruthy(state.loading)) {
        js("Common").toast("正在加载中……")
        return
    }
    state.loading = true
    state.comments.length = 0
    state.adpt.notifyChange()
    gfShowProgress("正在加载……", {
        gfFeedbackReloadTask()
        js("undefined")
    })
}

private fun gfFeedbackReloadTask() {
    val state = giteeFeedbackDetailState
    var data: dynamic = null
    try {
        val topic = giteeApi.getIssue(state.currentNumber)
        data = gfEmptyObject()
        data.topic = topic
        data.totalPages = js("Math").ceil(topic.comments / giteeApi.perPage)
        data.next = data.totalPages
    } catch (e: dynamic) {
        js("Log").e(e)
    }
    state.loading = false
    if (!gfTruthy(data)) {
        js("Common").toast("评论列表加载失败")
        return
    }
    giteeApi.updateRecentFeedback(data.topic)
    state.commentData = data
    state.writable = state.getWritable()
    gfFeedbackAppendPage(true)
    js("G").ui({
        try {
            state.title.setText(data.topic.title)
            if (gfTruthy(state.writable) && !gfTruthy(state.talkVisible)) {
                state.talkVisible = true
                state.list.addFooterView(state.talk)
            } else if (!gfTruthy(state.writable) && gfTruthy(state.talkVisible)) {
                state.talkVisible = false
                state.list.removeFooterView(state.talk)
            }
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

private fun gfFeedbackAppendPage(sync: dynamic) {
    val state = giteeFeedbackDetailState
    val opts = gfEmptyObject()
    opts.failText = "评论列表加载失败"
    opts.canLoad = { !gfIsNaN(state.commentData.next) }
    opts.fetch = {
        giteeApi.getIssueComments(state.currentNumber, state.commentData.next)
    }
    opts.apply = { page: dynamic ->
        gfFeedbackApplyPage(page)
        js("undefined")
    }
    opts.afterApply = { page: dynamic ->
        if (gfLength(page) < 3 && !gfIsNaN(state.commentData.next)) state.appendPage(false)
        js("undefined")
    }
    js("NetworkUI").appendPage(state, sync, opts)
}

private fun gfFeedbackApplyPage(page: dynamic) {
    val state = giteeFeedbackDetailState
    var i: Int
    if (state.commentData.next > 1) {
        state.commentData.next = state.commentData.next - 1
    } else {
        state.commentData.next = js("NaN")
    }
    val off = gfLength(state.comments)
    state.comments.length = off + gfLength(page)
    i = gfLength(state.comments) - 1
    while (i >= gfLength(page)) {
        state.comments[i] = state.comments[i - gfLength(page)]
        i--
    }
    i = 0
    while (i < gfLength(page)) {
        page[i].fromThis = gfLooseEq(page[i].user.id, giteeApi.myUserId)
        state.comments[i] = page[i]
        i++
    }
    if (!gfTruthy(state.commentData.next)) {
        val topic = state.commentData.topic
        val comment = gfEmptyObject()
        comment["fromThis"] = true
        comment["body"] = if (gfTruthy(topic.body)) topic.body else topic.title
        comment["created_at"] = topic.created_at
        comment["user"] = topic.user
        state.comments.unshift(comment)
    }
    state.adpt.notifyChange()
    js("NetworkUI").updateMore(state, state.commentData.next, null, true)
}

private fun gfFeedbackAddComment(text: dynamic) {
    gfShowProgress("正在发送……", {
        gfFeedbackAddCommentTask(text)
        js("undefined")
    })
}

private fun gfFeedbackAddCommentTask(text: dynamic) {
    var d: dynamic = null
    try {
        d = giteeApi.createIssueComment(giteeFeedbackDetailState.currentNumber, text)
    } catch (e: dynamic) {
        js("Log").e(e)
    }
    if (!gfTruthy(d)) js("Common").toast("评论创建失败")
    js("java.lang.Thread").sleep(giteeApi.databaseDelay) //等待数据库更新
    js("G").ui({
        try {
            gfFeedbackReload()
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

private fun gfFeedbackClickData(data: dynamic): dynamic {
    val match = gfExecIssueTagJs(data.body)
    if (gfTruthy(match)) return giteeApi.showIssueDetail(match[1])
    return js("undefined")
}

private fun gfFeedbackOnItemClick(view: dynamic, pos: dynamic) {
    val state = giteeFeedbackDetailState
    try {
        if (gfLooseEq(view, state.more)) {
            state.appendPage()
            return
        }
        val data = state.adpt.array[pos]
        state.clickData(data)
    } catch (e: dynamic) {
        js("erp")(e)
    }
}

// ---------------- 编辑 / 登录 / 协议 ----------------
private fun gfShowEditIssue(o: dynamic, callback: dynamic, onDismiss: dynamic) {
    val opts = gfEmptyObject()
    opts.popupName = "feedback.EditIssue"
    opts.dialogTitle = if (gfTruthy(o.newIssue)) "新建反馈话题" else "编辑反馈话题"
    opts.titleHint = "在此处用一句话描述反馈的问题或建议"
    opts.contentText = o.body
    opts.contentHint = "在这里补充说明发生问题时的现象、复现步骤与报错信息。如果是建议，请在这里说明提出建议的原因"
    opts.contentField = "body"
    opts.data = o
    opts.onSubmit = callback
    opts.onDismiss = onDismiss
    js("NetworkUI").editIssueDialog(opts)
}

private fun gfCheckLogin(callback: dynamic) {
    if (gfTruthy(giteeApi.accessToken)) {
        giteeApi.showRecentFeedback(callback)
    } else {
        gfLoad()
        if (gfTruthy(giteeApi.accessType)) {
            js("Common").showProgressDialog({ dia: dynamic ->
                gfCheckLoginAuto(dia, callback)
                js("undefined")
            })
        } else {
            giteeApi.showLogin({
                giteeApi.showRecentFeedback(callback)
                js("undefined")
            })
        }
    }
}

private fun gfCheckLoginAuto(dia: dynamic, callback: dynamic) {
    dia.setText("正在自动登录...")
    if (gfLooseEq(giteeApi.accessType, "anonymous")) {
        giteeApi.acquireAccessTokenAnonymous()
        giteeApi.save(null)
    } else {
        try {
            giteeApi.refreshAccessToken(true)
            giteeApi.save(giteeApi.getUserInfo())
        } catch (e: dynamic) {
            js("Log").e(e)
            giteeApi.accessType = null
            giteeApi.accessData = null
            giteeApi.save(null)
            js("Common").toast("登录失败\n" + e)
            return
        }
    }
    giteeApi.showRecentFeedback(callback)
}

private fun gfShowLogin(callback: dynamic) {
    val opts = gfEmptyObject()
    opts.popupName = "feedback.GiteeLogin"
    opts.title = "登录码云"
    val padding = gfEmptyArray()
    padding.push(gfDp(15))
    padding.push(gfDp(15))
    padding.push(gfDp(15))
    padding.push(0)
    opts.padding = padding

    val username = gfEmptyObject()
    username.name = "username"
    username.hint = "用户名(邮箱)"
    username.inputType = "TYPE_CLASS_TEXT|TYPE_TEXT_VARIATION_EMAIL_ADDRESS"

    val password = gfEmptyObject()
    password.name = "password"
    password.hint = "密码"
    password.password = true
    password.singleLine = false

    val fields = gfEmptyArray()
    fields.push(username)
    fields.push(password)
    opts.fields = fields

    val note = gfEmptyObject()
    note.text = "您需要一个账户才能创建和回复反馈，您也可以选择使用匿名用户来查看反馈"
    note.style = "textview_highlight"
    note.fontSize = 2
    val notePadding = gfEmptyArray()
    notePadding.push(0)
    notePadding.push(gfDp(20))
    notePadding.push(0)
    notePadding.push(gfDp(10))
    note.padding = notePadding
    opts.note = note

    val buttons = gfEmptyArray()

    val login = gfEmptyObject()
    login.text = "登录"
    login.onClick = { formFields: dynamic, popup: dynamic ->
        gfLoginWithPassword(formFields, popup, callback)
        js("undefined")
    }
    buttons.push(login)

    val anonymous = gfEmptyObject()
    anonymous.text = "匿名使用"
    anonymous.onClick = { _formFields: dynamic, popup: dynamic ->
        gfLoginAnonymous(popup, callback)
        js("undefined")
    }
    buttons.push(anonymous)

    val browser = gfEmptyObject()
    browser.text = "使用浏览器登录或注册"
    val browserPadding = gfEmptyArray()
    browserPadding.push(gfDp(10))
    browserPadding.push(gfDp(10))
    browserPadding.push(gfDp(10))
    browserPadding.push(gfDp(20))
    browser.padding = browserPadding
    browser.onClick = { formFields: dynamic, _popup: dynamic ->
        giteeApi.startOAuth(callback)
        formFields.__popup.exit()
        js("undefined")
    }
    buttons.push(browser)
    opts.buttons = buttons

    js("NetworkUI").formDialog(opts)
}

private fun gfLoginWithPassword(formFields: dynamic, popup: dynamic, callback: dynamic) {
    if (!gfTruthy(formFields.username.length())) {
        js("Common").toast("用户名不能为空")
        return
    }
    if (!gfTruthy(formFields.password.length())) {
        js("Common").toast("密码不能为空")
        return
    }
    js("Common").showProgressDialog({ dia: dynamic ->
        gfLoginWithPasswordTask(dia, formFields, popup, callback)
        js("undefined")
    })
}

private fun gfLoginWithPasswordTask(dia: dynamic, formFields: dynamic, popup: dynamic, callback: dynamic) {
    dia.setText("正在登录...")
    try {
        giteeApi.acquireAccessToken(formFields.username.text, formFields.password.text)
        giteeApi.save(giteeApi.getUserInfo())
    } catch (e: dynamic) {
        js("Log").e(e)
        js("Common").toast("登录失败，请检查您是否已连接互联网且用户名与密码正确\n" + e)
        return
    }
    js("G").ui({
        try {
            popup.exit()
            if (gfTruthy(callback)) callback()
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

private fun gfLoginAnonymous(popup: dynamic, callback: dynamic) {
    js("Common").showProgressDialog({ dia: dynamic ->
        gfLoginAnonymousTask(dia, popup, callback)
        js("undefined")
    })
}

private fun gfLoginAnonymousTask(dia: dynamic, popup: dynamic, callback: dynamic) {
    dia.setText("正在登录...")
    try {
        giteeApi.acquireAccessTokenAnonymous()
        giteeApi.save(null)
    } catch (e: dynamic) {
        js("erp")(e, true)
        js("Common").toast("登录失败\n" + e)
        return
    }
    js("G").ui({
        try {
            popup.exit()
            if (gfTruthy(callback)) callback()
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

private fun gfLoad() {
    var settings = js("CA").settings.feedbackSettings
    if (!gfTruthy(settings)) {
        settings = gfEmptyObject()
        js("CA").settings.feedbackSettings = settings
    }
    giteeApi.settings = settings
    giteeApi.accessType = settings.accessType
    giteeApi.accessData = settings.accessData
}

private fun gfSave(userInfo: dynamic) {
    var settings = js("CA").settings.feedbackSettings
    if (!gfTruthy(settings)) {
        settings = gfEmptyObject()
        js("CA").settings.feedbackSettings = settings
    }
    giteeApi.settings = settings
    settings.accessType = giteeApi.accessType
    settings.accessData = giteeApi.accessData
    giteeApi.userInfo = userInfo
}

private fun gfStartOAuth(callback: dynamic) {
    giteeApi.oauthCallback = callback
    js("AndroidBridge").startActivity(gfNewViewIntent(giteeApi.getAuthorizeUrl()))
}

private fun gfCallbackOAuth(code: dynamic) {
    js("Common").showProgressDialog({ dia: dynamic ->
        gfCallbackOAuthTask(dia, code)
        js("undefined")
    })
}

private fun gfCallbackOAuthTask(dia: dynamic, code: dynamic) {
    dia.setText("正在登录...")
    try {
        giteeApi.acquireAccessTokenOAuth(code)
        giteeApi.save(giteeApi.getUserInfo())
    } catch (e: dynamic) {
        js("Log").e(e)
        js("Common").toast("登录失败\n" + e)
        return
    }
    js("G").ui({
        try {
            js("PopupPage").show()
            if (gfTruthy(giteeApi.oauthCallback)) giteeApi.oauthCallback()
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

private fun gfShowFeedbacks(callback: dynamic) {
    if (gfTruthy(giteeApi.clientId)) {
        if (gfTruthy(js("CA").settings.readFeedbackAgreement)) {
            giteeApi.checkLogin(callback)
        } else {
            giteeApi.showAgreement({
                giteeApi.checkLogin(callback)
                js("undefined")
            })
        }
    } else {
        js("Common").toast("您目前使用的版本无法创建反馈")
        giteeApi.showIssues(callback)
    }
}

private fun gfShowAgreement(callback: dynamic) {
    val options = gfEmptyObject()
    options.title = "反馈使用说明"
    val raw = gfEmptyObject()
    val extra = gfEmptyArray()
    extra.push("请务必看完本说明！\n")
    extra.push("\n1. 所有人反馈的内容都是公开的，请注意保护自己的隐私。")
    extra.push(
        "\n2. 在反馈之前，请先查看常见问题解答(在最近反馈界面点击右上角三角显示的菜单内)。" +
            "如果常见问题解答能解决你的问题，请不要重复反馈。"
    )
    extra.push(
        "\n3. 您如果重复反馈或反馈与命令助手无关的内容，这条反馈会被标注为“已拒绝”。" +
            "如果您提出的被拒绝反馈数量超过两个，您会被暂时禁止反馈。"
    )
    extra.push("\n4. 在自己的反馈下多次发送重复或无关反馈的内容会让这条反馈被标注为“已拒绝”。")
    extra.push("\n5. 请将您想反馈的内容表达尽可能具体。过于简单的反馈很可能会被标注为“已拒绝”。")
    extra.push(
        "\n6. 您的未处理的反馈随时都可能收到回复，推荐一天检查一次反馈。" +
            "被标注为“已处理”或“已拒绝”的反馈不能被回复。"
    )
    extra.push(
        "\n7. 如果您有很难用文字解释或者涉及隐私的反馈，建议私聊作者反馈：" +
            "QQ:2687587184 电子邮箱:projectxero@163.com。"
    )
    extra.push("\n\n您随时都可以在最近反馈界面点击右上角三角显示的菜单内再次查看本说明")
    raw.extra = extra
    raw.color = "textcolor"
    options.description = js("ISegment").rawJson(raw)

    val buttons = gfEmptyArray()
    buttons.push("我了解了")
    options.buttons = buttons
    options.callback = { _id: dynamic ->
        js("CA").settings.readFeedbackAgreement = true
        if (gfTruthy(callback)) callback()
        js("undefined")
    }
    js("Common").showConfirmDialog(options)
}

fun installGiteeFeedback() {
    giteeApi.name = "GiteeFeedback"
    giteeApi.author = "ProjectXero"
    giteeApi.version = gfArray3(1, 0, 0)
    giteeApi.uuid = "3c7b3f7f-bda9-4ed9-a336-cdee2ebae433"
    giteeApi.targetOwner = "projectxero"
    giteeApi.targetRepo = "ca"
    giteeApi.perPage = 20
    giteeApi.maxFeedbackCount = 50
    giteeApi.databaseDelay = 2000

    giteeApi.initialize = {
        gfInitialize()
        js("undefined")
    }
    giteeApi.getAuthorizeUrl = { gfGetAuthorizeUrl() }
    giteeApi.acquireAccessTokenAnonymous = {
        gfAcquireAccessTokenAnonymous()
        js("undefined")
    }
    giteeApi.acquireAccessTokenOAuth = { code: dynamic ->
        gfAcquireAccessTokenOAuth(code)
        js("undefined")
    }
    giteeApi.acquireAccessToken = { userName: dynamic, password: dynamic ->
        gfAcquireAccessToken(userName, password)
        js("undefined")
    }
    giteeApi.refreshAccessToken = { force: dynamic -> gfRefreshAccessToken(force) }
    giteeApi.getRecentFeedback = { state: dynamic -> gfGetRecentFeedback(state) }
    giteeApi.updateRecentFeedback = { data: dynamic ->
        gfUpdateRecentFeedback(data)
        js("undefined")
    }
    giteeApi.upgradeRecentFeedback = {
        gfUpgradeRecentFeedback()
        js("undefined")
    }
    giteeApi.getUserInfo = { gfGetUserInfo() }
    giteeApi.getIssues = { state: dynamic, page: dynamic -> gfGetIssues(state, page) }
    giteeApi.getUserIssues = { userName: dynamic, state: dynamic, page: dynamic ->
        gfGetUserIssues(userName, state, page)
    }
    giteeApi.createIssue = { title: dynamic, body: dynamic -> gfCreateIssue(title, body) }
    giteeApi.getIssue = { number: dynamic -> gfGetIssue(number) }
    giteeApi.updateIssue = { number: dynamic, map: dynamic -> gfUpdateIssue(number, map) }
    giteeApi.getIssueComment = { id: dynamic -> gfGetIssueComment(id) }
    giteeApi.getIssueComments = { number: dynamic, page: dynamic -> gfGetIssueComments(number, page) }
    giteeApi.createIssueComment = { number: dynamic, body: dynamic -> gfCreateIssueComment(number, body) }
    giteeApi.updateIssueComment = { id: dynamic, body: dynamic -> gfUpdateIssueComment(id, body) }
    giteeApi.deleteIssueComment = { id: dynamic -> gfDeleteIssueComment(id) }
    giteeApi.showIssues = { callback: dynamic -> gfShowIssues(callback) }
    giteeApi.showIssueDetail = { number: dynamic, callback: dynamic -> gfShowIssueDetail(number, callback) }
    giteeApi.showRecentFeedback = { callback: dynamic -> gfShowRecentFeedback(callback) }
    giteeApi.showFeedbackDetail = { number: dynamic, readOnly: dynamic, callback: dynamic ->
        gfShowFeedbackDetail(number, readOnly, callback)
    }
    giteeApi.showEditIssue = { o: dynamic, callback: dynamic, onDismiss: dynamic ->
        gfShowEditIssue(o, callback, onDismiss)
    }
    giteeApi.checkLogin = { callback: dynamic -> gfCheckLogin(callback) }
    giteeApi.showLogin = { callback: dynamic -> gfShowLogin(callback) }
    giteeApi.load = {
        gfLoad()
        js("undefined")
    }
    giteeApi.save = { userInfo: dynamic ->
        gfSave(userInfo)
        js("undefined")
    }
    giteeApi.startOAuth = { callback: dynamic -> gfStartOAuth(callback) }
    giteeApi.callbackOAuth = { code: dynamic -> gfCallbackOAuth(code) }
    giteeApi.showFeedbacks = { callback: dynamic -> gfShowFeedbacks(callback) }
    giteeApi.showAgreement = { callback: dynamic -> gfShowAgreement(callback) }

    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("GiteeFeedback", giteeApi)
    }
}
