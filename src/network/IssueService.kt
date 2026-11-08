package ca.kotlin

/**
 * modules/network/IssueService.js 的 Kotlin/JS 版本。
 *
 * 保持原模块的字段、错误表、列表/详情页构建逻辑与网络请求参数；
 * showIssues / showIssueDetail 的静态状态挂在各自的 self 对象上，
 * 与原来 named function expression 的持久状态一致。
 */
private val issueServiceApi: dynamic = js("({})")
private val isShowIssuesState: dynamic = js("({})")
private val isShowIssueDetailState: dynamic = js("({})")

private val isErrorMessages: dynamic = js(
    """({
        "precond.issue.list.type.missing": "缺少话题类型",
        "precond.issue.list.state.missing": "缺少话题状态",
        "precond.issue.list.start.invalid": "缺少话题起始索引",
        "precond.issue.list.limit.invalid": "缺少话题最大数目",

        "precond.issue.batchGet.tokens.missing": "缺少话题令牌列表",

        "error.issue.info.invalidToken": "无效的话题令牌",

        "precond.issue.create.title.missing": "缺少话题标题",
        "precond.issue.create.title.wrong": "话题标题过短、过长或含有违规词语",
        "precond.issue.create.content.wrong": "话题内容过长或含有违规词语",
        "precond.issue.create.creatorToken.missing": "缺少话题创建者令牌",
        "error.issue.create.writeError": "写入话题失败",

        "precond.issue.setContent.token.missing": "缺少话题令牌",
        "precond.issue.setContent.title.missing": "缺少话题标题",
        "precond.issue.setContent.title.wrong": "话题标题过短、过长或含有违规词语",
        "precond.issue.setContent.content.wrong": "话题内容过长或含有违规词语",
        "precond.issue.setContent.creatorToken.missing": "缺少话题创建者令牌",
        "error.issue.setContent.denied": "您不是话题的创建者，无法修改话题内容",
        "error.issue.setContent.cannotSet": "当前话题状态不允许您修改话题内容",
        "error.issue.setContent.writeError": "写入话题失败",

        "precond.issue.setState.token.missing": "缺少话题令牌",
        "precond.issue.setState.state.missing": "缺少话题状态",
        "precond.issue.setState.creatorToken.missing": "缺少话题创建者令牌",
        "error.issue.setState.denied": "您不是话题的创建者，无法修改话题状态",
        "error.issue.setState.cannotSet": "当前话题状态不允许您将话题状态修改为此状态",
        "error.issue.setState.writeError": "写入话题失败",

        "precond.issue.getComments.start.invalid": "缺少消息起始索引",
        "precond.issue.getComments.limit.invalid": "缺少消息最大数目",

        "precond.issue.addComment.content.missing": "缺少消息内容",
        "precond.issue.addComment.content.wrong": "消息内容过长或含有违规词语",
        "precond.issue.addComment.creatorToken.missing": "缺少消息创建者令牌",
        "error.issue.addComment.cannotAdd": "当前话题状态不允许您发送消息",
        "error.issue.addComment.writeError": "写入消息记录失败",

        "error.issue.removeComment.writeError": "删除消息记录失败",

        "error.issue.archiveIssue.cannotSet": "无法将此话题设为归档状态",
        "error.issue.archiveIssue.writeError": "写入话题失败",

        "error.issue.banIssue.cannotSet": "无法将此话题设为屏蔽状态",
        "error.issue.banIssue.writeError": "写入话题失败",

        "error.issue.normalizeIssue.cannotSet": "无法将此话题设为正常状态",
        "error.issue.normalizeIssue.writeError": "写入话题失败",

        "error.issue.removeIssue.writeError": "删除话题失败",

        "error.issue.updateListener.register.invalidData": "无效的订阅请求",
        "error.issue.updateListener.register.tooMuchListener": "订阅监听器过多",
        "error.issue.updateListener.unregister.invalidData": "无效的取消订阅请求"
    })"""
)

// ---------------- JS 语义 helper ----------------
private val isTruthyJs: dynamic = js("function (v) { return !!v; }")
private val isLengthJs: dynamic = js("function (v) { return v.length | 0; }")
private val isToIntJs: dynamic = js("function (v) { return Number(v) | 0; }")
private val isStringJs: dynamic = js("function (v) { return String(v); }")
private val isJoinJs: dynamic = js("function (a, s) { return a.join(s); }")
private val isIndexOfJs: dynamic = js("function (a, v) { return a.indexOf(v); }")
private val isPushApplyJs: dynamic = js(
    "function (target, source) { Array.prototype.push.apply(target, source); }"
)
private val isDpJs: dynamic = js("function (n) { return n * G.dp; }")

// ---------------- UI / Java 构造 helper ----------------
private val isNewLinearLayoutJs: dynamic = js("function (c) { return new G.LinearLayout(c); }")
private val isNewTextViewJs: dynamic = js("function (c) { return new G.TextView(c); }")
private val isNewAbsListViewParamsJs: dynamic = js(
    "function (w, h) { return new G.AbsListView.LayoutParams(w, h); }"
)
private val isNewLinearLayoutParamsJs: dynamic = js(
    "function (w, h) { return new G.LinearLayout.LayoutParams(w, h); }"
)
private val isNewPopupPageJs: dynamic = js("function (v, n) { return new PopupPage(v, n); }")
private val isNewSimpleListAdapterJs: dynamic = js(
    "function (a, b, c) { return new SimpleListAdapter(a, b, c); }"
)
private val isSimpleListAdapterControllerJs: dynamic = js(
    "function (a) { return SimpleListAdapter.getController(a); }"
)
private val isNewMoreListAdapterJs: dynamic = js(
    "function (a, b) { return new MoreListAdapter(a, b); }"
)
private val isMoreListAdapterControllerJs: dynamic = js(
    "function (a) { return MoreListAdapter.getController(a); }"
)
private val isOpenFaqJs: dynamic = js(
    """function (url) {
        return new android.content.Intent(
            android.content.Intent.ACTION_VIEW,
            android.net.Uri.parse(url)
        ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
    }"""
)

private fun isTruthy(v: dynamic): Boolean = isTruthyJs(v)
private fun isLength(v: dynamic): Int = isLengthJs(v)
private fun isToInt(v: dynamic): Int = isToIntJs(v)
private fun isString(v: dynamic): dynamic = isStringJs(v)
private fun isJoin(a: dynamic, s: dynamic): dynamic = isJoinJs(a, s)
private fun isIndexOf(a: dynamic, v: dynamic): Int = isIndexOfJs(a, v)
private fun isPushApply(target: dynamic, source: dynamic) {
    isPushApplyJs(target, source)
}
private fun isDp(n: Int): dynamic = isDpJs(n)

private fun isEmptyArray(): dynamic = js("[]")
private fun isNewObject(): dynamic = js("({})")

private fun isNewLayout(width: dynamic, height: dynamic): dynamic {
    val o = isNewObject()
    o.width = width
    o.height = height
    return o
}

private fun isNewWeightedLayout(width: dynamic, height: dynamic, weight: dynamic): dynamic {
    val o = isNewObject()
    o.width = width
    o.height = height
    o.weight = weight
    return o
}

private fun isNewPadding(left: dynamic, top: dynamic, right: dynamic, bottom: dynamic): dynamic {
    val r = isEmptyArray()
    r.push(left)
    r.push(top)
    r.push(right)
    r.push(bottom)
    return r
}

// ---------------- 网络 API ----------------
private fun isCreateIssue(title: dynamic, content: dynamic): dynamic {
    val data = isNewObject()
    data.title = title
    data.content = content
    return js("NetworkUtils").requestApi(
        "POST", issueServiceApi.apiHost + "/issue", issueServiceApi.fillCreator(data)
    )
}

private fun isGetIssue(token: dynamic): dynamic {
    val data = isNewObject()
    data.token = token
    return js("NetworkUtils").requestApi("GET", issueServiceApi.apiHost + "/issue/:token", data)
}

private fun isBatchGetIssue(tokens: dynamic): dynamic {
    val data = isNewObject()
    data.tokens = isJoin(tokens, "|")
    return js("NetworkUtils").requestApi("GET", issueServiceApi.apiHost + "/issue/get", data)
}

private fun isListIssues(type: dynamic, state: dynamic, start: dynamic, limit: dynamic): dynamic {
    val data = isNewObject()
    data.type = type
    data.state = state
    data.start = start
    data.limit = limit
    return js("NetworkUtils").requestApi(
        "GET", issueServiceApi.apiHost + "/issue/list", issueServiceApi.fillCreator(data)
    )
}

private fun isSetContent(token: dynamic, title: dynamic, content: dynamic) {
    val query = isNewObject()
    query.token = token
    val data = isNewObject()
    data.title = title
    data.content = content
    js("NetworkUtils").requestApi(
        "PATCH", issueServiceApi.apiHost + "/issue/:token/content", query, issueServiceApi.fillCreator(data)
    )
}

private fun isSetState(token: dynamic, state: dynamic) {
    val query = isNewObject()
    query.token = token
    val data = isNewObject()
    data.state = state
    js("NetworkUtils").requestApi(
        "PATCH", issueServiceApi.apiHost + "/issue/:token/state", query, issueServiceApi.fillCreator(data)
    )
}

private fun isListComments(
    token: dynamic, start: dynamic, limit: dynamic, sort: dynamic, direction: dynamic
): dynamic {
    val query = isNewObject()
    query.token = token
    val data = isNewObject()
    data.start = start
    data.limit = limit
    data.sort = sort
    data.dir = direction
    return js("NetworkUtils").requestApi(
        "GET", issueServiceApi.apiHost + "/issue/:token/comment", query, data
    )
}

private fun isAddComment(token: dynamic, content: dynamic): dynamic {
    val query = isNewObject()
    query.token = token
    val data = isNewObject()
    data.content = content
    return js("NetworkUtils").requestApi(
        "POST", issueServiceApi.apiHost + "/issue/:token/comment", query, issueServiceApi.fillCreator(data)
    )
}

private fun isFillCreator(obj: dynamic): dynamic {
    val actor = issueServiceApi.internal.UserManager.allocateActor()
    obj.creator_type = actor.type
    obj.token = actor.token
    return obj
}

// ---------------- 列表页 ----------------
private fun isBuildIssuesContextMenu(self: dynamic): dynamic {
    val menu = isEmptyArray()

    val refresh = isNewObject()
    refresh.text = "刷新"
    refresh.onclick = { _v: dynamic, _tag: dynamic ->
        self.reload()
        js("undefined")
    }
    menu.push(refresh)

    val onlyOpen = isNewObject()
    onlyOpen.text = "仅显示未解决"
    onlyOpen.hidden = { self.issueState == "open" }
    onlyOpen.onclick = { _v: dynamic, _tag: dynamic ->
        self.issueState = "open"
        self.reload()
        js("undefined")
    }
    menu.push(onlyOpen)

    val showAll = isNewObject()
    showAll.text = "显示所有"
    showAll.hidden = { self.issueState == "all" }
    showAll.onclick = { _v: dynamic, _tag: dynamic ->
        self.issueState = "all"
        self.reload()
        js("undefined")
    }
    menu.push(showAll)

    val faq = isNewObject()
    faq.text = "查看常见问题解答(FAQ)"
    faq.onclick = { _v: dynamic, _tag: dynamic ->
        js("AndroidBridge").startActivity(isOpenFaqJs("https://ca.projectxero.top/blog/faq/"))
        js("undefined")
    }
    menu.push(faq)

    val agreement = isNewObject()
    agreement.text = "查看反馈说明"
    agreement.onclick = { _v: dynamic, _tag: dynamic ->
        issueServiceApi.showIssueAgreement()
        js("undefined")
    }
    menu.push(agreement)

    return menu
}

private fun isMakeAddIssue(self: dynamic): dynamic {
    var retry: dynamic = js("undefined")
    retry = { newIssue: dynamic ->
        js("NetworkUI").progress("正在创建……", {
            var ok = true
            try {
                issueServiceApi.createIssue(newIssue.title, newIssue.content)
            } catch (e: dynamic) {
                js("Log").e(e)
                ok = false
                issueServiceApi.showEditIssue("issue", newIssue, retry)
                js("Common").toast("保存话题失败\n" + e)
            }
            if (ok) {
                js("G").ui({
                    try {
                        self.reload()
                    } catch (e: dynamic) {
                        js("erp")(e)
                    }
                    js("undefined")
                })
            }
            js("undefined")
        })
        js("undefined")
    }
    return retry
}

private fun isAppendIssuesPage(self: dynamic, limit: dynamic): dynamic {
    var issues: dynamic = js("undefined")
    try {
        if (isTruthy(issueServiceApi.privilegedMode)) {
            val args = isNewObject()
            args.start = isLength(self.issues)
            args.limit = limit
            args.type = "issue"
            args.state = self.issueState
            issues = issueServiceApi.internal.UserManager.executeAdminAction("Issue.listIssues", args)
        } else {
            issues = issueServiceApi.listIssues("issue", self.issueState, isLength(self.issues), limit)
        }
    } catch (e: dynamic) {
        js("Log").e(e)
        js("Common").toast("数据加载失败\n" + e)
    }
    return issues
}

private fun isRefreshIssue(self: dynamic, pos: dynamic) {
    js("NetworkUI").progress("正在刷新列表……", {
        val oldIssue = self.issues[pos]
        var newIssue: dynamic = js("undefined")
        var ok = true
        try {
            newIssue = issueServiceApi.getIssue(oldIssue.token) // getIssue不会返回token
            newIssue.token = oldIssue.token
            self.issues[pos] = newIssue
        } catch (e: dynamic) {
            js("Log").e(e)
            ok = false
            js("Common").toast("刷新列表失败\n" + e)
        }
        if (ok) {
            js("G").ui({
                try {
                    self.arrayAdpt.notifyChange()
                } catch (e: dynamic) {
                    js("erp")(e)
                }
                js("undefined")
            })
        }
        js("undefined")
    })
}

private fun isBuildIssueVmaker(holder: dynamic): dynamic {
    val layout = isNewLinearLayoutJs(js("ctx"))
    val text1 = isNewTextViewJs(js("ctx"))
    val text2 = isNewTextViewJs(js("ctx"))
    holder.text1 = text1
    holder.text2 = text2
    layout.setLayoutParams(isNewAbsListViewParamsJs(-1, -2))
    layout.setOrientation(js("G").LinearLayout.VERTICAL)
    layout.setPadding(isDp(15), isDp(15), isDp(15), isDp(15))
    text1.setLayoutParams(isNewLinearLayoutParamsJs(-1, -2))
    text1.setEllipsize(js("G").TextUtils.TruncateAt.END)
    text1.setSingleLine(true)
    layout.addView(text1)
    text2.setPadding(0, isDp(5), 0, 0)
    text2.setLayoutParams(isNewLinearLayoutParamsJs(-1, -2))
    js("Common").applyStyle(text2, "textview_prompt", 1)
    layout.addView(text2)
    return layout
}

private fun isBuildIssueVbinder(holder: dynamic, e: dynamic) {
    if (e.state == "issue_open" || e.state == "issue_progressing") {
        holder.text1.setText(
            (if (e.state == "issue_open") "[待处理] " else "") + e.title
        )
        js("Common").applyStyle(holder.text1, "item_default", 3)
    } else {
        holder.text1.setText(
            (if (e.state == "issue_rejected") "[已拒绝] " else "[已解决] ") + e.title
        )
        js("Common").applyStyle(holder.text1, "item_disabled", 3)
    }
    holder.text2.setText("最近更新于 " + js("Updater").toChineseDate(js("Date").parse(e.update_time)))
}

private fun isBuildIssuesPopup(self: dynamic) {
    // 顶部标题与菜单
    val header = isNewObject()
    header.orientation = js("L").LinearLayout("horizontal")
    header.padding = isNewPadding(0, 0, 0, isDp(10))
    header.layout = isNewLayout(-1, -2)
    header.style = "message_bg"
    val headerChildren = isEmptyArray()

    val title = isNewObject()
    title.text = "意见与反馈"
    title.gravity = js("L").Gravity("left|center")
    title.padding = isNewPadding(isDp(10), 0, isDp(10), 0)
    title.style = "textview_default"
    title.fontSize = 4
    title.layout = isNewWeightedLayout(0, -1, 1.0)
    headerChildren.push(js("L").TextView(title))

    val arrow = isNewObject()
    arrow.text = "▼"
    arrow.gravity = js("L").Gravity("left|center")
    arrow.padding = isNewPadding(isDp(10), 0, isDp(10), 0)
    arrow.style = "button_highlight"
    arrow.fontSize = 3
    arrow.layout = isNewLayout(-2, -1)
    headerChildren.push(js("L").TextView(arrow))

    header.children = headerChildren
    header.onClick = {
        try {
            js("Common").showOperateDialog(self.contextMenu)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }

    // 加载更多 loader
    val loadingView = isNewObject()
    loadingView.text = "加载中"
    loadingView.gravity = js("L").Gravity("center")
    loadingView.padding = isNewPadding(0, isDp(15), 0, isDp(15))
    loadingView.focusable = true
    loadingView.style = "item_disabled"
    loadingView.fontSize = 3
    loadingView.layoutParams = isNewAbsListViewParamsJs(-1, -2)

    val loader = isNewObject()
    loader.loadingView = js("L").TextView(loadingView)
    loader.load = { callback: dynamic, session: dynamic ->
        var limit: dynamic = js("undefined")
        if (self.lastIssuesLen > isLength(self.issues)) {
            limit = self.lastIssuesLen - isLength(self.issues)
        } else {
            limit = issueServiceApi.perPage
        }
        js("Threads").run({
            try {
                val result = self.appendPage(limit)
                js("G").ui({
                    try {
                        if (self.loader.latestSession == session) {
                            if (isTruthy(result)) {
                                isPushApply(self.issues, result)
                                callback(isLength(result) < isToInt(limit), true)
                            } else {
                                callback(true, true)
                            }
                            self.arrayAdpt.notifyChange()
                        }
                    } catch (e: dynamic) {
                        js("erp")(e)
                    }
                    js("undefined")
                })
            } catch (e: dynamic) {
                js("erp")(e)
            }
            js("undefined")
        })
        js("undefined")
    }
    loader.autoload = true
    self.loader = loader

    self.adpt = isMoreListAdapterControllerJs(
        isNewMoreListAdapterJs(self.arrayAdpt.self, self.loader)
    )

    val newIssueView = isNewObject()
    newIssueView.gravity = js("L").Gravity("center")
    newIssueView.text = "+ 创建反馈"
    newIssueView.padding = isNewPadding(isDp(15), isDp(15), isDp(15), isDp(15))
    newIssueView.layout = isNewLayout(-1, -2)
    newIssueView.style = "item_default"
    newIssueView.fontSize = 3

    val listOpts = isNewObject()
    listOpts.layout = isNewWeightedLayout(-1, 0, 1.0)
    listOpts._adapter = self.adpt.self
    listOpts.onItemClick = { _parent: dynamic, _view: dynamic, pos: dynamic, _id: dynamic ->
        try {
            if (pos == 0) {
                self.addIssue()
            } else {
                var p = pos - 1 // HeaderView的个数
                val issue = self.issues[p]
                if (isTruthy(issue)) {
                    issueServiceApi.showIssueDetail(issue, {
                        self.refreshIssue(p)
                        js("undefined")
                    })
                }
            }
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }
    listOpts._newIssueView = js("L").TextView(newIssueView)
    listOpts.inflate = { listView: dynamic ->
        listView.addHeaderView(listOpts._newIssueView)
        listView.adapter = self.adpt.self
        js("undefined")
    }
    self.list = js("L").ListView(listOpts)

    val children = isEmptyArray()
    children.push(js("L").LinearLayout(header))
    children.push(self.list)
    children.push(js("NetworkUI").closeButton({ self.popup }))

    val linear = isNewObject()
    linear.orientation = js("L").LinearLayout("vertical")
    linear.padding = isNewPadding(isDp(15), isDp(15), isDp(15), 0)
    linear.style = "message_bg"
    linear.children = children

    self.popup = isNewPopupPageJs(js("L").LinearLayout(linear), "issue.Issues")
    self.popup.on("exit", {
        if (isTruthy(self.callback)) self.callback()
        js("undefined")
    })
    js("PWM").registerResetFlag(self, "popup")
}

private fun isShowIssues(callback: dynamic) {
    val self = isShowIssuesState
    js("G").ui({
        try {
            if (!isTruthy(self.popup)) {
                self.contextMenu = isBuildIssuesContextMenu(self)
                val retry = isMakeAddIssue(self)
                self.addIssue = {
                    val initial = isNewObject()
                    initial.newIssue = true
                    initial.title = ""
                    initial.content = ""
                    issueServiceApi.showEditIssue("issue", initial, retry)
                    js("undefined")
                }
                self.issueState = "open"
                self.reload = {
                    self.lastIssuesLen = isLength(self.issues)
                    self.issues.length = 0
                    self.adpt.reset(false, true)
                    self.arrayAdpt.notifyChange()
                    js("undefined")
                }
                self.appendPage = { limit: dynamic -> isAppendIssuesPage(self, limit) }
                self.refreshIssue = { pos: dynamic ->
                    isRefreshIssue(self, pos)
                    js("undefined")
                }
                self.vmaker = { holder: dynamic -> isBuildIssueVmaker(holder) }
                self.vbinder = { holder: dynamic, e: dynamic, _i: dynamic, _a: dynamic ->
                    isBuildIssueVbinder(holder, e)
                    js("undefined")
                }

                val issues = isEmptyArray()
                self.issues = issues
                self.arrayAdpt = isSimpleListAdapterControllerJs(
                    isNewSimpleListAdapterJs(issues, self.vmaker, self.vbinder)
                )

                val talkOpts = isNewObject()
                talkOpts.onSend = { s: dynamic ->
                    self.addComment(s, {
                        js("G").ui({
                            try {
                                self.talkbox.text = ""
                            } catch (e: dynamic) {
                                js("erp")(e)
                            }
                            js("undefined")
                        })
                        js("undefined")
                    })
                    js("undefined")
                }
                self.talkBar = js("NetworkUI").commentBar(talkOpts)
                self.talk = self.talkBar.view
                self.talkbox = self.talkBar.input

                isBuildIssuesPopup(self)
            }
            self.callback = callback
            self.popup.enter()
            self.reload()
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

// ---------------- 详情页 ----------------
private fun isBuildDetailContextMenu(self: dynamic): dynamic {
    val menu = isEmptyArray()

    val refresh = isNewObject()
    refresh.text = "刷新"
    refresh.onclick = {
        self.reloadIssue()
        js("undefined")
    }
    menu.push(refresh)

    val edit = isNewObject()
    edit.text = "编辑话题"
    edit.hidden = { !isTruthy(self.contentModifiable) }
    edit.onclick = {
        val issue = self.issue
        val data = isNewObject()
        data.title = issue.title
        data.content = issue.content
        issueServiceApi.showEditIssue("issue", data, { newIssue: dynamic ->
            js("NetworkUI").progress("正在加载……", {
                var ok = true
                try {
                    issueServiceApi.setContent(issue.token, newIssue.title, newIssue.content)
                } catch (e: dynamic) {
                    js("Log").e(e)
                    ok = false
                    js("Common").toast("保存话题失败\n" + e)
                }
                if (ok) {
                    js("G").ui({
                        try {
                            self.reloadIssue()
                        } catch (e: dynamic) {
                            js("erp")(e)
                        }
                        js("undefined")
                    })
                }
                js("undefined")
            })
            js("undefined")
        })
        js("undefined")
    }
    menu.push(edit)

    val resolved = isNewObject()
    resolved.text = "标记为已解决"
    resolved.hidden = {
        val state = self.issue.state
        !isTruthy(issueServiceApi.privilegedMode) && state != "issue_open" && state != "issue_progressing"
    }
    resolved.onclick = {
        self.setState("issue_closed")
        js("undefined")
    }
    menu.push(resolved)

    val unresolved = isNewObject()
    unresolved.text = "标记为未解决"
    unresolved.hidden = {
        val state = self.issue.state
        !isTruthy(issueServiceApi.privilegedMode) && state != "issue_closed"
    }
    unresolved.onclick = {
        self.setState("issue_open")
        js("undefined")
    }
    menu.push(unresolved)

    val progressing = isNewObject()
    progressing.text = "标记为处理中"
    progressing.hidden = { !isTruthy(issueServiceApi.privilegedMode) }
    progressing.onclick = {
        self.setState("issue_progressing")
        js("undefined")
    }
    menu.push(progressing)

    val rejected = isNewObject()
    rejected.text = "标记为已拒绝"
    rejected.hidden = { !isTruthy(issueServiceApi.privilegedMode) }
    rejected.onclick = {
        self.setState("issue_rejected")
        js("undefined")
    }
    menu.push(rejected)

    val detail = isNewObject()
    detail.text = "查看详情"
    detail.onclick = {
        js("NetworkUI").progress("正在加载……", {
            var issue: dynamic = js("undefined")
            var creatorName: dynamic = js("undefined")
            var ok = true
            try {
                issue = issueServiceApi.getIssue(self.issue.token)
                creatorName = issueServiceApi.internal.UserManager.getActorName(
                    issue.creator_type, issue.creator_id
                )
            } catch (e: dynamic) {
                js("Log").e(e)
                ok = false
                js("Common").toast("加载失败\n" + e)
            }
            if (ok) {
                val translated = self.issueStateTranslation[issue.state]
                val lines = isEmptyArray()
                lines.push("ID: " + issue.id)
                lines.push("创建者: " + creatorName)
                lines.push("创建时间: " + js("Updater").toChineseDate(js("Date").parse(issue.create_time)))
                lines.push("更新时间: " + js("Updater").toChineseDate(js("Date").parse(issue.update_time)))
                lines.push("状态: " + (if (isTruthy(translated)) translated else issue.state))
                js("Common").showTextDialog(isJoin(lines, "\n"))
            }
            js("undefined")
        })
        js("undefined")
    }
    menu.push(detail)

    return menu
}

private fun isBuildStatePermissions(): dynamic {
    val modifyContent = isEmptyArray()
    modifyContent.push("issue_open")
    modifyContent.push("discuss_open")
    modifyContent.push("discuss_blocked")

    val modifyComment = isEmptyArray()
    modifyComment.push("issue_open")
    modifyComment.push("issue_progressing")
    modifyComment.push("discuss_open")

    val o = isNewObject()
    o.modifyContent = modifyContent
    o.modifyComment = modifyComment
    return o
}

private fun isBuildStateTranslation(): dynamic {
    val o = isNewObject()
    o.removed = "被移除"
    o.unspecified = "未指定"
    o.issue_open = "待处理"
    o.issue_progressing = "正在处理"
    o.issue_closed = "已解决"
    o.issue_rejected = "被拒绝"
    o.issue_archived = "被归档"
    return o
}

private fun isNewCommentItem(title: dynamic, content: dynamic, createTime: dynamic): dynamic {
    val o = isNewObject()
    o.titleItem = true
    o.title = title
    o.content = content
    o.create_time = createTime
    return o
}

private fun isBuildDetailPopup(self: dynamic) {
    val loadingView = isNewObject()
    loadingView.text = "加载中"
    loadingView.gravity = js("L").Gravity("center")
    loadingView.padding = isNewPadding(0, isDp(15), 0, isDp(15))
    loadingView.focusable = true
    loadingView.style = "item_disabled"
    loadingView.fontSize = 3
    loadingView.layoutParams = isNewAbsListViewParamsJs(-1, -2)

    val loader = isNewObject()
    loader.loadingView = js("L").TextView(loadingView)
    loader.load = { callback: dynamic, session: dynamic ->
        var limit: dynamic = js("undefined")
        if (self.lastCommentsLen > isLength(self.comments)) {
            limit = self.lastCommentsLen - isLength(self.comments)
        } else {
            limit = issueServiceApi.perPage
        }
        js("Threads").run({
            try {
                val result = self.appendPage(self.startComment, limit)
                js("G").ui({
                    try {
                        if (self.loader.latestSession == session) {
                            if (isTruthy(result)) {
                                isPushApply(self.comments, result)
                                callback(isLength(result) < isToInt(limit), true)
                                if (isLength(result) > 0) {
                                    val latestId = result[isLength(result) - 1].id
                                    self.startComment = latestId + 1
                                }
                            } else {
                                callback(true, true)
                            }
                            self.arrayAdpt.notifyChange()
                        }
                    } catch (e: dynamic) {
                        js("erp")(e)
                    }
                    js("undefined")
                })
            } catch (e: dynamic) {
                js("erp")(e)
            }
            js("undefined")
        })
        js("undefined")
    }
    loader.autoload = true
    self.loader = loader

    self.adpt = isMoreListAdapterControllerJs(
        isNewMoreListAdapterJs(self.arrayAdpt.self, self.loader)
    )

    val listOpts = isNewObject()
    listOpts.dividerHeight = 0
    listOpts.layout = isNewWeightedLayout(-1, 0, 1.0)
    listOpts.adapter = self.adpt.self
    listOpts.onItemClick = { _parent: dynamic, _view: dynamic, _pos: dynamic, _id: dynamic ->
        try {
            // do nothing
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    }
    self.list = js("L").ListView(listOpts)

    val children = isEmptyArray()
    children.push(js("NetworkUI").issueDetailHeader(self, true))
    children.push(self.list)
    children.push(self.talk)
    js("NetworkUI").issueDetailPage(self, children)
}

private fun isShowIssueDetail(issue: dynamic, callback: dynamic) {
    val self = isShowIssueDetailState
    js("G").ui({
        try {
            if (!isTruthy(self.popup)) {
                self.contextMenu = isBuildDetailContextMenu(self)
                self.statePermissions = isBuildStatePermissions()
                self.issueStateTranslation = isBuildStateTranslation()

                self.addComment = { text: dynamic, cb: dynamic ->
                    js("NetworkUI").progress("正在发送……", {
                        var ok = true
                        try {
                            issueServiceApi.addComment(self.issue.token, text)
                        } catch (e: dynamic) {
                            js("Log").e(e)
                            ok = false
                            js("Common").toast("发送失败\n" + e)
                        }
                        if (ok) {
                            self.checkUpdate()
                            cb()
                        }
                        js("undefined")
                    })
                    js("undefined")
                }

                val talkOpts = isNewObject()
                talkOpts.onSend = { s: dynamic ->
                    self.addComment(s, {
                        js("G").ui({
                            try {
                                self.talkbox.text = ""
                            } catch (e: dynamic) {
                                js("erp")(e)
                            }
                            js("undefined")
                        })
                        js("undefined")
                    })
                    js("undefined")
                }
                self.talkBar = js("NetworkUI").commentBar(talkOpts)
                self.talk = self.talkBar.view
                self.talkbox = self.talkBar.input

                self.setState = { state: dynamic ->
                    js("NetworkUI").progress("正在标记……", {
                        var ok = true
                        try {
                            if (isTruthy(issueServiceApi.privilegedMode)) {
                                val args = isNewObject()
                                args.token = self.issue.token
                                args.state = state
                                issueServiceApi.internal.UserManager.executeAdminAction("Issue.setState", args)
                            } else {
                                issueServiceApi.setState(self.issue.token, state)
                            }
                        } catch (e: dynamic) {
                            js("Log").e(e)
                            ok = false
                            js("Common").toast("标记失败\n" + e)
                        }
                        if (ok) self.checkUpdate()
                        js("undefined")
                    })
                    js("undefined")
                }

                self.checkUpdate = {
                    var result: dynamic = true
                    val oldIssue = self.issue
                    val newIssue = issueServiceApi.getIssue(oldIssue.token) // getIssue不会返回token
                    newIssue.token = oldIssue.token
                    if (newIssue.update_time == self.issue.update_time) {
                        result = false
                    } else {
                        self.issue = newIssue
                        var change: dynamic = "reload"
                        if (newIssue.id == oldIssue.id &&
                            newIssue.title == oldIssue.title &&
                            newIssue.content == oldIssue.content &&
                            newIssue.create_time == oldIssue.create_time &&
                            newIssue.state == oldIssue.state &&
                            newIssue.start_comment == oldIssue.start_comment
                        ) {
                            change = "newComment"
                        }
                        js("G").ui({
                            try {
                                if (change == "reload") {
                                    self.reloadIssue()
                                } else if (change == "newComment") {
                                    if (isTruthy(self.loader.finished)) {
                                        self.adpt.reset(false, false)
                                    }
                                }
                            } catch (e: dynamic) {
                                js("erp")(e)
                            }
                            js("undefined")
                        })
                    }
                    result
                }

                self.reloadIssue = {
                    js("NetworkUI").progress("正在加载……", {
                        val token = self.issue.token
                        var ok = true
                        try {
                            self.issue = issueServiceApi.getIssue(token) // getIssue不会返回token
                            self.issue.token = token
                        } catch (e: dynamic) {
                            js("Log").e(e)
                            ok = false
                            js("Common").toast("获取话题失败\n" + e)
                        }
                        if (ok) {
                            js("G").ui({
                                try {
                                    self.reload()
                                } catch (e: dynamic) {
                                    js("erp")(e)
                                }
                                js("undefined")
                            })
                        }
                        js("undefined")
                    })
                    js("undefined")
                }

                self.reload = {
                    self.lastCommentsLen = isLength(self.comments)
                    self.myActorName = issueServiceApi.internal.UserManager.getMyActorName()
                    self.startComment = self.issue.start_comment
                    self.contentModifiable = isIndexOf(
                        self.statePermissions.modifyContent, self.issue.state
                    ) >= 0
                    self.commentModifiable = isIndexOf(
                        self.statePermissions.modifyComment, self.issue.state
                    ) >= 0
                    self.comments.length = 1
                    self.comments[0] = isNewCommentItem(
                        "主题", self.issue.title, self.issue.create_time
                    )
                    if (isTruthy(self.issue.content)) {
                        self.comments.push(
                            isNewCommentItem("内容", self.issue.content, self.issue.create_time)
                        )
                    }
                    self.title.setText(self.issue.title)
                    self.talk.setVisibility(
                        if (isTruthy(self.commentModifiable)) js("G").View.VISIBLE else js("G").View.GONE
                    )
                    self.adpt.reset(self.startComment == 0, true)
                    self.arrayAdpt.notifyChange()
                    js("undefined")
                }

                self.appendPage = { start: dynamic, limit: dynamic ->
                    var comments: dynamic = js("undefined")
                    try {
                        comments = issueServiceApi.listComments(
                            self.issue.token, start, limit, "forward", "asc"
                        )
                        comments.forEach({ e: dynamic ->
                            e.fromThis = issueServiceApi.internal.UserManager.isMyActor(
                                e.creator_type, e.creator_id
                            )
                            if (!isTruthy(e.fromThis)) {
                                e.creator_name = issueServiceApi.internal.UserManager.getActorName(
                                    e.creator_type, e.creator_id
                                )
                            }
                            js("undefined")
                        })
                    } catch (e: dynamic) {
                        js("Log").e(e)
                        js("Common").toast("数据加载失败\n" + e)
                    }
                    comments
                }

                self.vmaker = { holder: dynamic ->
                    val opts = isNewObject()
                    opts.padding = isNewPadding(isDp(20), isDp(15), isDp(20), isDp(15))
                    opts.width = -2
                    opts.text1Style = "textview_prompt"
                    opts.text1FontSize = 1
                    opts.text2Style = "item_default"
                    opts.text2FontSize = 3
                    js("NetworkUI").listItem(holder, opts)
                }

                self.vbinder = { holder: dynamic, e: dynamic, _i: dynamic, _a: dynamic ->
                    if (isTruthy(e.titleItem)) {
                        holder.linear.setGravity(js("G").Gravity.LEFT)
                        holder.text1.setText(e.title)
                    } else if (isTruthy(e.fromThis)) {
                        holder.linear.setGravity(js("G").Gravity.RIGHT)
                        holder.text1.setText(self.myActorName)
                    } else {
                        holder.linear.setGravity(js("G").Gravity.LEFT)
                        holder.text1.setText(e.creator_name)
                    }
                    holder.text2.setText(e.content)
                    js("undefined")
                }

                val comments = isEmptyArray()
                self.comments = comments
                self.arrayAdpt = isSimpleListAdapterControllerJs(
                    isNewSimpleListAdapterJs(comments, self.vmaker, self.vbinder)
                )

                isBuildDetailPopup(self)
            }
            self.issue = issue
            self.callback = callback
            self.popup.enter()
            self.reload()
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

private fun isShowIssueDetailByToken(token: dynamic, callback: dynamic) {
    js("NetworkUI").progress("正在加载……", {
        var issue: dynamic = js("undefined")
        var ok = true
        try {
            issue = issueServiceApi.getIssue(token) // getIssue不会返回token
            issue.token = token
        } catch (e: dynamic) {
            js("Log").e(e)
            ok = false
            js("Common").toast("获取话题失败\n" + e)
        }
        if (ok) {
            js("G").ui({
                try {
                    issueServiceApi.showIssueDetail(issue, callback)
                } catch (e: dynamic) {
                    js("erp")(e)
                }
                js("undefined")
            })
        }
        js("undefined")
    })
}

private fun isShowEditIssue(
    type: dynamic, o: dynamic, callback: dynamic, onDismiss: dynamic
) {
    val opts = isNewObject()
    opts.popupName = "issue.EditIssueContent"
    opts.dialogTitle = (if (isTruthy(o.newIssue)) "新建" else "编辑") +
        (if (type == "discuss") "讨论" else "反馈") + "话题"
    opts.titleHint = if (type == "discuss") "讨论标题" else "在此处用一句话描述反馈的问题或建议"
    opts.contentText = o.content
    opts.contentHint = if (type == "discuss") {
        "讨论内容"
    } else {
        "在这里补充说明发生问题时的现象、复现步骤与报错信息。如果是建议，请在这里说明提出建议的原因"
    }
    opts.contentField = "content"
    opts.data = o
    opts.onSubmit = callback
    opts.onDismiss = onDismiss
    js("NetworkUI").editIssueDialog(opts)
}

private fun isShowIssueAgreement(callback: dynamic) {
    val raw = isNewObject()
    val extra = isEmptyArray()
    extra.push("请务必看完本说明！\n")
    extra.push(
        "\n1. 在反馈之前，请先查看常见问题解答(在意见与反馈列表界面点击右上角三角显示的菜单内)。" +
            "如果常见问题解答能解决你的问题，请不要重复反馈。"
    )
    extra.push(
        "\n2. 您如果重复反馈或反馈与命令助手本身无关的内容，这条反馈会被标注为“已拒绝”。" +
            "如果最近您提出的被拒绝反馈数量超过两个，您会被暂时禁止反馈一段时间。"
    )
    extra.push(
        "\n3. 在自己的反馈下多次发送重复或无关反馈的内容会让这条反馈被标注为“已拒绝”。"
    )
    extra.push(
        "\n4. 请将您想反馈的内容表达尽可能具体。过于简单的反馈很可能会被标注为“已拒绝”。"
    )
    extra.push(
        "\n5. 您的未处理的反馈随时都可能收到回复，推荐一天检查一次反馈。" +
            "被标注为“已解决”或“已拒绝”的反馈不能被回复。"
    )
    extra.push(
        "\n6. 未注册用户时，您的反馈会以匿名游客名义创建。注册用户后您无法查看以匿名游客名义创建的反馈。"
    )
    extra.push(
        "\n7. 如果您有很难用文字解释或者涉及隐私的反馈，建议私聊作者反馈：" +
            "QQ:2687587184 电子邮箱:projectxero@163.com。"
    )
    extra.push(
        "\n\n您随时都可以在最近反馈界面点击右上角三角显示的菜单内再次查看本说明"
    )
    raw.extra = extra
    raw.color = "textcolor"

    val buttons = isEmptyArray()
    buttons.push("我了解了")

    val opts = isNewObject()
    opts.title = "反馈使用说明"
    opts.description = js("ISegment").rawJson(raw)
    opts.buttons = buttons
    opts.callback = { _id: dynamic ->
        js("CA").settings.readFeedbackAgreement = true
        if (isTruthy(callback)) callback()
        js("undefined")
    }
    js("Common").showConfirmDialog(opts)
}

private fun isShowIssuesWithAgreement() {
    if (isTruthy(js("CA").settings.readFeedbackAgreement)) {
        if (isTruthy(issueServiceApi.internal.UserManager.isAdmin())) {
            issueServiceApi.showIssuesPrivileged()
        } else {
            issueServiceApi.showIssues()
        }
    } else {
        issueServiceApi.showIssueAgreement({
            issueServiceApi.showIssuesWithAgreement()
            js("undefined")
        })
    }
}

private fun isShowIssuesPrivileged() {
    issueServiceApi.privilegedMode = true
    issueServiceApi.internal.UserManager.showAdminAuth({
        issueServiceApi.showIssues({
            issueServiceApi.privilegedMode = false
            js("undefined")
        })
        js("undefined")
    })
}

private fun isOnCreate() {
    js("Internal").add("IssueService", issueServiceApi)
    js("NetworkUtils").addErrorMessages(issueServiceApi.errorMessage)
}

fun installIssueService() {
    issueServiceApi.name = "IssueService"
    issueServiceApi.author = "ProjectXero"
    val version = isEmptyArray()
    version.push(1)
    version.push(0)
    version.push(0)
    issueServiceApi.version = version
    issueServiceApi.uuid = "1c1426ac-b4c2-4738-9b1b-da6860962674"
    issueServiceApi.apiHost = js("NetworkUtils").urlBase.api
    issueServiceApi.wsHost = js("NetworkUtils").urlBase.ws
    issueServiceApi.perPage = 10
    issueServiceApi.errorMessage = isErrorMessages

    issueServiceApi.createIssue = { title: dynamic, content: dynamic ->
        isCreateIssue(title, content)
    }
    issueServiceApi.getIssue = { token: dynamic -> isGetIssue(token) }
    issueServiceApi.batchGetIssue = { tokens: dynamic -> isBatchGetIssue(tokens) }
    issueServiceApi.listIssues = { type: dynamic, state: dynamic, start: dynamic, limit: dynamic ->
        isListIssues(type, state, start, limit)
    }
    issueServiceApi.setContent = { token: dynamic, title: dynamic, content: dynamic ->
        isSetContent(token, title, content)
        js("undefined")
    }
    issueServiceApi.setState = { token: dynamic, state: dynamic ->
        isSetState(token, state)
        js("undefined")
    }
    issueServiceApi.listComments = {
            token: dynamic, start: dynamic, limit: dynamic, sort: dynamic, direction: dynamic ->
        isListComments(token, start, limit, sort, direction)
    }
    issueServiceApi.addComment = { token: dynamic, content: dynamic ->
        isAddComment(token, content)
    }
    issueServiceApi.fillCreator = { obj: dynamic -> isFillCreator(obj) }

    issueServiceApi.showIssues = { callback: dynamic ->
        isShowIssues(callback)
        js("undefined")
    }
    issueServiceApi.showIssueDetail = { issue: dynamic, callback: dynamic ->
        isShowIssueDetail(issue, callback)
        js("undefined")
    }
    issueServiceApi.showIssueDetailByToken = { token: dynamic, callback: dynamic ->
        isShowIssueDetailByToken(token, callback)
        js("undefined")
    }
    issueServiceApi.showEditIssue = {
            type: dynamic, o: dynamic, callback: dynamic, onDismiss: dynamic ->
        isShowEditIssue(type, o, callback, onDismiss)
        js("undefined")
    }
    issueServiceApi.showIssueAgreement = { callback: dynamic ->
        isShowIssueAgreement(callback)
        js("undefined")
    }
    issueServiceApi.showIssuesWithAgreement = {
        isShowIssuesWithAgreement()
        js("undefined")
    }
    issueServiceApi.showIssuesPrivileged = {
        isShowIssuesPrivileged()
        js("undefined")
    }

    issueServiceApi.onCreate = {
        isOnCreate()
        js("undefined")
    }

    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("IssueService", issueServiceApi)
    }
}
