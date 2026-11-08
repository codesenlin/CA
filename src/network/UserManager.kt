package ca.kotlin

/**
 * modules/network/UserManager.js 的 Kotlin/JS 版本。
 *
 * 保持原模块的字段、方法、错误表、回调与网络请求参数；
 * 模块内部状态统一放在 userManagerApi 上，不依赖动态 this。
 * onCreate / initialize 仍按原位置由 MapScript 调用。
 */
private val userManagerApi: dynamic = js("({})")
private var umDebugInterfaceCache: dynamic = js("undefined")

private val umErrorMessages: dynamic = js(
    """({
        "precond.user.info.token.missing": "缺少访问令牌",
        "error.user.info.invalidToken": "无效的访问令牌",
        "error.user.info.userNotExist": "用户已被注销",

        "precond.user.register.email.missing": "缺少邮箱地址",
        "precond.user.register.email.wrong": "邮箱地址过长或格式不正确",
        "precond.user.register.name.missing": "缺少用户名",
        "precond.user.register.name.wrong": "用户名过短、过长或含有违规词语",
        "precond.user.register.pass.missing": "缺少密码",
        "precond.user.register.pass.wrong": "密码过短或过长",
        "error.user.register.emailOccupied": "邮箱已被使用",
        "error.user.register.nameOccupied": "用户名已被占用",
        "error.user.register.abuseEmail": "请不要向这个邮箱发送过多邮件或发送邮件发送过于频繁",
        "error.user.register.writeError": "写入数据库失败",
        "error.user.register.sendEmailFailed": "发送验证邮件失败",

        "precond.user.login.user.missing": "缺少用户名",
        "precond.user.login.pass.missing": "缺少密码",
        "error.user.login.userNotExist": "用户名或密码不正确",
        "error.user.login.wrongPassword": "用户名或密码不正确",
        "error.user.login.writeError": "写入数据库失败",

        "precond.user.refreshLogin.token.missing": "缺少刷新令牌",
        "error.user.refreshLogin.writeError": "写入数据库失败",
        "error.user.refreshLogin.invalidToken": "刷新令牌不可用",

        "precond.user.authorize.token.missing": "缺少刷新令牌",
        "error.user.authorize.writeError": "写入数据库失败",
        "error.user.authorize.invalidToken": "刷新令牌不可用",

        "precond.user.loginOAuth.token.missing": "授权令牌不可用",
        "error.user.loginOAuth.invalidToken": "已取得OAuth令牌或授权令牌不可用",

        "precond.user.forgetPwd.email.missing": "缺少邮箱地址",
        "precond.user.forgetPwd.pass.missing": "缺少密码",
        "precond.user.forgetPwd.pass.wrong": "密码过短或过长",
        "error.user.forgetPwd.userNotExist": "没有账户和该邮箱绑定",
        "error.user.forgetPwd.abuseEmail": "请不要向这个邮箱发送过多邮件或发送邮件发送过于频繁",
        "error.user.forgetPwd.sendEmailFailed": "发送重置密码邮件失败",

        "precond.user.setPwd.accessToken.missing": "缺少访问令牌",
        "precond.user.setPwd.oldPassword.missing": "缺少旧密码",
        "precond.user.setPwd.newPassword.missing": "缺少新密码",
        "precond.user.setPwd.newPassword.wrong": "密码过短或过长",
        "error.user.setPwd.wrongOldPassword": "旧密码错误",
        "error.user.setPwd.writeError": "写入账户记录失败",

        "precond.user.setName.accessToken.missing": "缺少访问令牌",
        "precond.user.setName.name.missing": "缺少用户名",
        "precond.user.setName.name.wrong": "用户名过短、过长或含有违规词语",
        "error.user.setName.nameOccupied": "用户名被占用",
        "error.user.setName.writeError": "写入账户记录失败",

        "precond.user.changeEmail.token.missing": "缺少访问令牌",
        "precond.user.changeEmail.email.missing": "缺少邮箱地址",
        "precond.user.changeEmail.email.wrong": "邮箱地址过长或格式不正确",
        "info.user.changeEmail.sameAddress": "旧邮箱与新邮箱相同",
        "error.user.changeEmail.emailOccupied": "该邮箱已与其他账户绑定",
        "error.user.changeEmail.sendEmailFailed": "发送验证邮件失败",

        "precond.user.requestRemove.token.missing": "缺少访问令牌",
        "error.user.requestRemove.sendEmailFailed": "发送确认邮件失败",

        "precond.user.addExp.token.missing": "缺少访问令牌",
        "precond.user.addExp.reason.missing": "缺少经验来源",
        "precond.user.addExp.reason.wrong": "不是有效的经验来源",
        "error.user.addExp.notAllowed": "在指定时间前，经验已到达上限",
        "info.user.addExp.limitedToday": "本日获得经验已到达上限",
        "error.user.addExp.writeError": "写入账户记录失败",

        "precond.user.checkIn.token.missing": "缺少访问令牌",
        "error.user.checkIn.writeError": "写入账户记录失败",

        "precond.admin.auth.token.missing": "缺少令牌",
        "error.admin.auth.notAdmin": "没有权限",
        "error.admin.auth.invalidToken": "无效的管理员令牌",
        "error.admin.action.notExists": "管理员任务不存在",
        "error.admin.action.error": "管理员任务执行出错",

        "precond.visitor.allocate.tag.missing": "缺少附加数据",
        "precond.visitor.allocate.tag.wrong": "附加数据过短或过长",
        "error.visitor.create.writeError": "写入访客记录失败",

        "precond.visitor.remove.token.missing": "缺少访客令牌",
        "precond.visitor.remove.secret.missing": "缺少访客密钥",
        "error.visitor.remove.writeError": "写入访客记录失败",

        "error.visitor.info.invalidToken": "无效的访客令牌",
        "error.visitor.info.banned": "您的访客账户已被封禁",

        "error.visitor.update.writeError": "写入访客记录失败",

        "error.actor.notSupported": "不支持的创建者类型"
    })"""
)

private val umReasons: dynamic = js(
    """({
        "checkin": { name: "签到", exp: 2 },
        "copyCommand": { name: "复制命令", exp: 1 },
        "createTemplate": { name: "创建批量生成模板", exp: 2 }
    })"""
)

private val umPublicUserInfoCache: dynamic = js("({})")

// ---------------- JS 语义 helper ----------------
private val umTruthyJs: dynamic = js("function (v) { return !!v; }")
private val umStringJs: dynamic = js("function (v) { return String(v); }")
private val umLengthJs: dynamic = js("function (v) { return v.length | 0; }")
private val umToIntJs: dynamic = js("function (v) { return Number(v) | 0; }")
private val umIsArrayJs: dynamic = js("function (v) { return Array.isArray(v); }")
private val umJoinJs: dynamic = js("function (a, s) { return a.join(s); }")
private val umInJs: dynamic = js("function (k, o) { return k in o; }")
private val umForInJs: dynamic = js("function (o, f) { for (var i in o) f(i); }")
private val umEncodeJs: dynamic = js("function (v) { return encodeURIComponent(v); }")
private val umObjectCreateJs: dynamic = js("function (o) { return Object.create(o); }")
private val umBindJs: dynamic = js(
    "function (f, scope) { var a = Array.prototype.slice.call(arguments, 2); return f.bind.apply(f, [scope].concat(a)); }"
)
private val umThis0Js: dynamic = js("function (f) { return function () { return f(this); }; }")
private val umDpJs: dynamic = js("function (n) { return n * G.dp; }")
private val umEmailRegexJs: dynamic = js(
    "function (s) { return /^([A-Za-z0-9_\\-\\.])+\\@([A-Za-z0-9_\\-\\.])+\\.([A-Za-z]{2,4})$/.test(s); }"
)

// ---------------- Java 互操作 helper ----------------
private val umNewQueueJs: dynamic = js(
    "function () { return new java.util.concurrent.ConcurrentLinkedQueue(); }"
)

private fun umTruthy(v: dynamic): Boolean = umTruthyJs(v)
private fun umString(v: dynamic): dynamic = umStringJs(v)
private fun umLength(v: dynamic): Int = umLengthJs(v)
private fun umToInt(v: dynamic): Int = umToIntJs(v)
private fun umIsArray(v: dynamic): Boolean = umIsArrayJs(v)
private fun umJoin(a: dynamic, sep: dynamic): dynamic = umJoinJs(a, sep)
private fun umIn(key: dynamic, obj: dynamic): Boolean = umInJs(key, obj)
private fun umForIn(obj: dynamic, f: dynamic) {
    umForInJs(obj, f)
}
private fun umEncode(v: dynamic): dynamic = umEncodeJs(v)
private fun umDp(n: Int): dynamic = umDpJs(n)

private fun umEmptyArray(): dynamic = js("[]")
private fun umSingleArray(v: dynamic): dynamic {
    val r = umEmptyArray()
    r.push(v)
    return r
}

private fun umNewLayout(width: dynamic, height: dynamic): dynamic {
    val o = js("({})")
    o.width = width
    o.height = height
    return o
}

private fun umNewWeightedLayout(width: dynamic, height: dynamic, weight: dynamic): dynamic {
    val o = js("({})")
    o.width = width
    o.height = height
    o.weight = weight
    return o
}

private fun umNewPadding(left: dynamic, top: dynamic, right: dynamic, bottom: dynamic): dynamic {
    val r = umEmptyArray()
    r.push(left)
    r.push(top)
    r.push(right)
    r.push(bottom)
    return r
}

private fun umNewObject(): dynamic = js("({})")

// ---------------- 网络 API ----------------
private fun umLogin(emailOrName: dynamic, password: dynamic) {
    val data = umNewObject()
    data.name = emailOrName
    data.pass = password
    val result = js("NetworkUtils").requestApi("POST", userManagerApi.apiHost + "/user/login", data)
    userManagerApi.saveToken(result)
}

private fun umLogout() {
    userManagerApi.clearToken()
}

private fun umRegister(email: dynamic, name: dynamic, password: dynamic) {
    val data = umNewObject()
    data.email = email
    data.name = name
    data.pass = password
    js("NetworkUtils").requestApi("POST", userManagerApi.apiHost + "/user/register", data)
}

private fun umRefreshLogin(refreshToken: dynamic) {
    val data = umNewObject()
    data.token = refreshToken
    val result = js("NetworkUtils").requestApi("GET", userManagerApi.apiHost + "/user/refresh", data)
    userManagerApi.saveToken(result)
}

private fun umGetUserInfo(): dynamic {
    val data = umNewObject()
    data.token = userManagerApi.accessToken
    return js("NetworkUtils").requestApi("GET", userManagerApi.apiHost + "/user/info", data)
}

private fun umGetPublicUserInfo(id: dynamic): dynamic {
    val data = umNewObject()
    data.id = id
    return js("NetworkUtils").requestApi("GET", userManagerApi.apiHost + "/user/public/:id", data)
}

private fun umAuthorize(): dynamic {
    val data = umNewObject()
    data.token = userManagerApi.accessData.refreshToken
    return js("NetworkUtils").requestApi("POST", userManagerApi.apiHost + "/user/authorize", data)
}

private fun umRequestResetPassword(email: dynamic, password: dynamic) {
    val data = umNewObject()
    data.email = email
    data.pass = password
    js("NetworkUtils").requestApi("POST", userManagerApi.apiHost + "/user/forget_password", data)
}

private fun umSetPassword(oldPassword: dynamic, newPassword: dynamic) {
    val data = umNewObject()
    data.accessToken = userManagerApi.accessToken
    data.oldPwd = oldPassword
    data.newPwd = newPassword
    js("NetworkUtils").requestApi("POST", userManagerApi.apiHost + "/user/set_password", data)
}

private fun umSetUsername(name: dynamic) {
    val data = umNewObject()
    data.accessToken = userManagerApi.accessToken
    data.name = name
    js("NetworkUtils").requestApi("POST", userManagerApi.apiHost + "/user/set_username", data)
}

private fun umChangeEmail(email: dynamic) {
    val data = umNewObject()
    data.accessToken = userManagerApi.accessToken
    data.email = email
    js("NetworkUtils").requestApi("POST", userManagerApi.apiHost + "/user/change_email", data)
}

private fun umRequestRemove() {
    val data = umNewObject()
    data.accessToken = userManagerApi.accessToken
    js("NetworkUtils").requestApi("POST", userManagerApi.apiHost + "/user/request_remove", data)
}

private fun umCheckIn(): dynamic {
    val data = umNewObject()
    data.token = userManagerApi.accessToken
    return js("NetworkUtils").requestApi("POST", userManagerApi.apiHost + "/user/check_in", data)
}

private fun umAddExp(reasons: dynamic): dynamic {
    val data = umNewObject()
    data.token = userManagerApi.accessToken
    data.reasons = if (umIsArray(reasons)) umJoin(reasons, ",") else reasons
    return js("NetworkUtils").requestApi("POST", userManagerApi.apiHost + "/user/add_exp", data)
}

private fun umAcquireAdminToken() {
    val data = umNewObject()
    data.token = userManagerApi.accessToken
    val result = js("NetworkUtils").requestApi("GET", userManagerApi.apiHost + "/admin/auth", data)
    userManagerApi.adminToken = result
}

private fun umExecuteAdminAction(name: dynamic, data: dynamic): dynamic {
    val query = umNewObject()
    query.token = userManagerApi.adminToken
    query.action = name
    return js("NetworkUtils").requestApi("POST", userManagerApi.apiHost + "/admin/action", query, data)
}

private fun umAllocateVisitorToken(): dynamic {
    val data = umNewObject()
    data.tag = umString(js("AndroidBridge").getUserID())
    return js("NetworkUtils").requestApi("POST", userManagerApi.apiHost + "/visitor/allocate", data)
}

private fun umClearVisitorToken(token: dynamic, secret: dynamic) {
    val data = umNewObject()
    data.token = token
    data.secret = secret
    js("NetworkUtils").requestApi("POST", userManagerApi.apiHost + "/visitor/clear", data)
}

private fun umGetVisitorID(token: dynamic): dynamic {
    val data = umNewObject()
    data.token = token
    return js("NetworkUtils").requestApi("GET", userManagerApi.apiHost + "/visitor/id", data)
}

// ---------------- Token / 用户信息 ----------------
private fun umLoadToken() {
    userManagerApi.accessData = js("CA").settings.userSettings
    if (umTruthy(userManagerApi.accessData)) {
        val refreshToken = userManagerApi.accessData.refreshToken
        userManagerApi.showRefreshLogin(refreshToken, true)
    }
}

private fun umSaveToken(result: dynamic) {
    userManagerApi.accessToken = result.accessToken
    val accessData = umNewObject()
    accessData.refreshToken = result.refreshToken
    userManagerApi.accessData = accessData
    js("CA").settings.userSettings = accessData
    userManagerApi.updateUserInfo()
}

private fun umClearToken() {
    userManagerApi.accessToken = null
    userManagerApi.accessData = null
    js("CA").settings.userSettings = null
    userManagerApi.userInfo = null
}

private fun umUpdateUserInfo() {
    userManagerApi.userInfo = userManagerApi.getUserInfo()
}

private fun umGetCachedUserInfo(): dynamic = userManagerApi.userInfo

private fun umGetVisitorTokenCached(): dynamic {
    var visitorToken = js("CA").settings.visitorToken
    if (!umTruthy(visitorToken)) {
        visitorToken = userManagerApi.allocateVisitorToken()
        js("CA").settings.visitorToken = visitorToken
    }
    return visitorToken
}

private fun umGetVisitorIDCached(): dynamic {
    if (!umTruthy(userManagerApi.visitorID)) {
        userManagerApi.visitorID = userManagerApi.getVisitorID(userManagerApi.getVisitorTokenCached().token)
    }
    return userManagerApi.visitorID
}

private fun umAllocateActor(): dynamic {
    if (umTruthy(userManagerApi.accessToken)) {
        val actor = umNewObject()
        actor.type = 0 // User
        actor.token = userManagerApi.accessToken
        return actor
    } else {
        val visitorToken = userManagerApi.getVisitorTokenCached()
        val actor = umNewObject()
        actor.type = 1 // Visitor
        actor.token = visitorToken.token
        return actor
    }
}

private fun umIsMyActor(creatorType: dynamic, creatorID: dynamic): Boolean {
    if (umTruthy(userManagerApi.accessToken) && creatorType == 0) {
        if (userManagerApi.userInfo.id == creatorID) return true
    } else if (!umTruthy(userManagerApi.accessToken) && creatorType == 1) {
        if (userManagerApi.getVisitorIDCached() == creatorID) return true
    }
    return false
}

private fun umGetPublicUserInfoCached(id: dynamic, forceUpdate: dynamic): dynamic {
    var data = userManagerApi.publicUserInfoCache[id]
    if (!umTruthy(data) || umTruthy(forceUpdate)) {
        data = userManagerApi.getPublicUserInfo(id)
    }
    userManagerApi.publicUserInfoCache[id] = data
    return data
}

private fun umGetMyActorName(): dynamic {
    if (umTruthy(userManagerApi.accessToken)) {
        return userManagerApi.userInfo.name
    }
    return "匿名游客"
}

private fun umGetActorName(creatorType: dynamic, creatorID: dynamic): dynamic {
    if (creatorType == 0) {
        var userInfo: dynamic = js("undefined")
        try {
            userInfo = userManagerApi.getPublicUserInfoCached(creatorID)
        } catch (e: dynamic) {
            js("Log").e(e)
        }
        if (umTruthy(userInfo)) {
            return userInfo.name
        } else {
            return "用户" + creatorID
        }
    }
    return "匿名游客" + creatorID
}

private fun umIsAdmin(): Boolean {
    return if (umTruthy(userManagerApi.userInfo)) userManagerApi.userInfo.status == 999 else false
}

private fun umIsOnline(): Boolean {
    return js("MapScript").host == "Android" &&
        umTruthy(js("ScriptInterface").isOnlineMode()) &&
        userManagerApi.userInfo != null
}

private fun umGetOfflineReason(): dynamic {
    if (js("MapScript").host != "Android") {
        return "目前您使用的命令助手的版本不支持在线模式，请更换为APP版本。"
    }
    val reason = js("ScriptInterface").getOfflineReason()
    return if (umTruthy(reason)) reason else "未提供"
}

private fun umGetLevelExp(level: dynamic): dynamic {
    if (level > 0 && level <= 15) {
        return level * 2 + 7
    } else if (level > 15 && level <= 30) {
        return level * 5 - 38
    } else if (level > 30) {
        return level * 9 - 158
    } else {
        return 10000
    }
}

private fun umParseExpLevel(exp: dynamic): dynamic {
    var lev = 1
    var rest = exp
    var levExp = umGetLevelExp(1)
    while (rest >= levExp) {
        rest = rest - levExp
        lev++
        levExp = umGetLevelExp(lev)
    }
    val result = umNewObject()
    result.total = exp
    result.level = lev
    result.rest = rest
    result.levelExp = levExp
    return result
}

private fun umEnqueueExp(reason: dynamic) {
    if (umTruthy(userManagerApi.isOnline()) && umToInt(userManagerApi.expQueue.size()) < 20 &&
        umIn(reason, userManagerApi.reasons)
    ) {
        userManagerApi.expQueue.add(reason)
    }
}

private fun umHasExpToSync(): Boolean {
    if (!umTruthy(userManagerApi.isOnline())) return false
    return !(umTruthy(userManagerApi.expQueue.isEmpty()) && umTruthy(userManagerApi.userInfo.checkedIn))
}

private fun umSyncExp(): dynamic {
    val queue = userManagerApi.expQueue
    var list = umEmptyArray()
    var checkInExp: dynamic = 0
    val reasons = umNewObject()
    if (!umTruthy(userManagerApi.isOnline())) {
        val offline = umNewObject()
        offline.offline = true
        return offline
    }
    if (!umTruthy(userManagerApi.userInfo.checkedIn)) {
        checkInExp = userManagerApi.checkIn().add
        reasons["checkin"] = 1
    }
    var e: dynamic = queue.poll()
    while (e != null) {
        list.push(e)
        reasons[e] = if (umIn(e, reasons)) reasons[e] + 1 else 1
        e = queue.poll()
    }
    if (umLength(list) == 0) list = umSingleArray("info")
    val result = userManagerApi.addExp(list)
    result.add = result.add + checkInExp
    result.reasons = reasons
    userManagerApi.updateUserInfo()
    return result
}

// ---------------- URI / UI ----------------
private fun umProcessUriAction(type: dynamic, query: dynamic) {
    if (type == "login") {
        userManagerApi.showLogin()
    } else if (type == "info") {
        userManagerApi.showUpdateUserInfo()
    } else if (type == "autologin") {
        userManagerApi.showRefreshLogin(query.token, false)
    }
}

private fun umSubmitLogin(fields: dynamic, popup: dynamic, callback: dynamic) {
    if (!umTruthy(fields.username.length())) {
        js("Common").toast("用户名不能为空")
        return
    }
    if (!umTruthy(fields.password.length())) {
        js("Common").toast("密码不能为空")
        return
    }
    js("Common").showProgressDialog({ dia: dynamic ->
        dia.setText("正在登录...")
        var ok = true
        try {
            userManagerApi.login(umString(fields.username.text), umString(fields.password.text))
        } catch (e: dynamic) {
            js("Log").e(e)
            ok = false
            js("Common").toast("登录失败\n" + e)
        }
        if (ok) {
            js("CA").trySave()
            js("G").ui({
                try {
                    popup.exit()
                    js("Common").toast("登录成功")
                    if (umTruthy(callback)) callback()
                } catch (e: dynamic) {
                    js("erp")(e)
                }
                js("undefined")
            })
        }
        js("undefined")
    })
}

private fun umShowLogin(callback: dynamic) {
    val username = umNewObject()
    username.name = "username"
    username.hint = "邮箱或用户名"
    username.inputType = "TYPE_CLASS_TEXT|TYPE_TEXT_VARIATION_EMAIL_ADDRESS"

    val password = umNewObject()
    password.name = "password"
    password.hint = "密码"
    password.password = true

    val fields = umEmptyArray()
    fields.push(username)
    fields.push(password)

    val login = umNewObject()
    login.text = "登录"
    login.onClick = { f: dynamic, p: dynamic ->
        umSubmitLogin(f, p, callback)
        js("undefined")
    }

    val register = umNewObject()
    register.text = "注册"
    register.onClick = { _fields: dynamic, p: dynamic ->
        p.exit()
        userManagerApi.showRegister()
        js("undefined")
    }

    val forget = umNewObject()
    forget.text = "忘记密码"
    forget.onClick = { _fields: dynamic, p: dynamic ->
        p.exit()
        userManagerApi.showForgetPassword()
        js("undefined")
    }

    val buttons = umEmptyArray()
    buttons.push(login)
    buttons.push(register)
    buttons.push(forget)

    val opts = umNewObject()
    opts.popupName = "usermanager.Login"
    opts.title = "登录"
    opts.fields = fields
    opts.buttons = buttons
    js("NetworkUI").formDialog(opts)
}

private fun umSubmitRegister(fields: dynamic, popup: dynamic) {
    if (!umTruthy(fields.email.length())) {
        js("Common").toast("邮箱地址不能为空")
        return
    }
    if (umToInt(fields.email.length()) > 80) {
        js("Common").toast("邮箱地址过长")
        return
    }
    if (!umTruthy(umEmailRegexJs(fields.email.text))) {
        js("Common").toast("邮箱地址不正确")
        return
    }
    if (!umTruthy(fields.username.length())) {
        js("Common").toast("用户名不能为空")
        return
    }
    if (umToInt(fields.username.length()) < 2) {
        js("Common").toast("用户名太短\n用户名长度应大于等于2字符且小于等于80字符")
        return
    }
    if (umToInt(fields.username.length()) > 80) {
        js("Common").toast("用户名太长\n用户名长度应大于等于2字符且小于等于80字符")
        return
    }
    if (!umTruthy(fields.password.length())) {
        js("Common").toast("密码不能为空")
        return
    }
    if (umToInt(fields.password.length()) < 6) {
        js("Common").toast("密码太短\n密码长度应大于等于6字符且小于等于1000字符")
        return
    }
    if (umToInt(fields.password.length()) > 1000) {
        js("Common").toast("密码太长\n密码长度应大于等于6字符且小于等于1000字符")
        return
    }
    if (umString(fields.password.text) != umString(fields.password2.text)) {
        js("Common").toast("两次密码输入不一致")
        return
    }
    js("Common").showProgressDialog({ dia: dynamic ->
        dia.setText("正在发送验证邮件...")
        var ok = true
        try {
            userManagerApi.register(
                umString(fields.email.text),
                umString(fields.username.text),
                umString(fields.password.text)
            )
        } catch (e: dynamic) {
            js("Log").e(e)
            ok = false
            js("Common").toast("注册失败\n" + e)
        }
        if (ok) {
            js("G").ui({
                try {
                    popup.exit()
                    js("Common").showTextDialog(
                        "一份用于验证的邮件正在发送至" + fields.email.text +
                            "\n请在1天内点击邮件内的链接来确认注册"
                    )
                } catch (e: dynamic) {
                    js("erp")(e)
                }
                js("undefined")
            })
        }
        js("undefined")
    })
}

private fun umShowRegister() {
    val email = umNewObject()
    email.name = "email"
    email.hint = "邮箱"
    email.inputType = "TYPE_CLASS_TEXT|TYPE_TEXT_VARIATION_EMAIL_ADDRESS"

    val username = umNewObject()
    username.name = "username"
    username.hint = "用户名"
    username.inputType = "TYPE_CLASS_TEXT|TYPE_TEXT_VARIATION_EMAIL_ADDRESS"

    val password = umNewObject()
    password.name = "password"
    password.hint = "密码"
    password.password = true

    val password2 = umNewObject()
    password2.name = "password2"
    password2.hint = "确认密码"
    password2.password = true

    val fields = umEmptyArray()
    fields.push(email)
    fields.push(username)
    fields.push(password)
    fields.push(password2)

    val register = umNewObject()
    register.text = "注册"
    register.padding = umNewPadding(umDp(10), umDp(15), umDp(10), umDp(15))
    register.onClick = { f: dynamic, p: dynamic ->
        umSubmitRegister(f, p)
        js("undefined")
    }

    val buttons = umEmptyArray()
    buttons.push(register)

    val opts = umNewObject()
    opts.popupName = "usermanager.Register"
    opts.title = "注册"
    opts.fields = fields
    opts.buttons = buttons
    js("NetworkUI").formDialog(opts)
}

private fun umShowRefreshLogin(refreshToken: dynamic, silent: dynamic) {
    if (umTruthy(silent)) {
        js("Threads").run({
            try {
                userManagerApi.refreshLogin(refreshToken)
                js("CA").trySave()
            } catch (e: dynamic) {
                js("Log").e(e)
            }
            js("undefined")
        })
    } else {
        js("Common").showProgressDialog({ dia: dynamic ->
            dia.setText("正在登录...")
            var ok = true
            try {
                userManagerApi.refreshLogin(refreshToken)
            } catch (e: dynamic) {
                js("Log").e(e)
                ok = false
                js("Common").toast("登录失败\n" + e)
            }
            if (ok) {
                js("CA").trySave()
                js("Common").toast("登录成功")
            }
            js("undefined")
        })
    }
}

private fun umShowUpdateUserInfo() {
    js("Common").showProgressDialog({ dia: dynamic ->
        dia.setText("正在更新用户信息...")
        try {
            userManagerApi.updateUserInfo()
        } catch (e: dynamic) {
            js("Log").e(e)
            js("Common").toast("更新用户信息失败\n" + e)
        }
        js("undefined")
    })
}

private fun umSubmitForgetPassword(fields: dynamic, popup: dynamic) {
    if (!umTruthy(fields.email.length())) {
        js("Common").toast("邮箱地址不能为空")
        return
    }
    if (!umTruthy(fields.password.length())) {
        js("Common").toast("密码不能为空")
        return
    }
    if (umString(fields.password.text) != umString(fields.password2.text)) {
        js("Common").toast("两次密码输入不一致")
        return
    }
    js("Common").showProgressDialog({ dia: dynamic ->
        dia.setText("正在发送重置密码邮件...")
        var ok = true
        try {
            userManagerApi.requestResetPassword(umString(fields.email.text), umString(fields.password.text))
        } catch (e: dynamic) {
            js("Log").e(e)
            ok = false
            js("Common").toast("重置密码失败\n" + e)
        }
        if (ok) {
            js("G").ui({
                try {
                    popup.exit()
                    js("Common").showTextDialog(
                        "一份用于确认重置密码的邮件正在发送至" + fields.email.text +
                            "\n请在1天内点击邮件内的链接来确认重置密码"
                    )
                } catch (e: dynamic) {
                    js("erp")(e)
                }
                js("undefined")
            })
        }
        js("undefined")
    })
}

private fun umShowForgetPassword() {
    val email = umNewObject()
    email.name = "email"
    email.hint = "邮箱"
    email.inputType = "TYPE_CLASS_TEXT|TYPE_TEXT_VARIATION_EMAIL_ADDRESS"

    val password = umNewObject()
    password.name = "password"
    password.hint = "密码"
    password.password = true

    val password2 = umNewObject()
    password2.name = "password2"
    password2.hint = "确认密码"
    password2.password = true

    val fields = umEmptyArray()
    fields.push(email)
    fields.push(password)
    fields.push(password2)

    val reset = umNewObject()
    reset.text = "重置密码"
    reset.padding = umNewPadding(umDp(10), umDp(15), umDp(10), umDp(15))
    reset.onClick = { f: dynamic, p: dynamic ->
        umSubmitForgetPassword(f, p)
        js("undefined")
    }

    val buttons = umEmptyArray()
    buttons.push(reset)

    val opts = umNewObject()
    opts.popupName = "usermanager.ForgetPassword"
    opts.title = "忘记密码"
    opts.fields = fields
    opts.buttons = buttons
    js("NetworkUI").formDialog(opts)
}

private fun umShowAuthorize(query: dynamic) {
    if (umTruthy(userManagerApi.accessToken)) {
        js("Common").showProgressDialog({ dia: dynamic ->
            dia.setText("正在使用命令助手登录...")
            try {
                val authToken = userManagerApi.authorize()
                js("AndroidBridge").viewUri(query.redirect + "?token=" + umEncode(authToken))
            } catch (e: dynamic) {
                js("Log").e(e)
                js("Common").toast("使用命令助手登录失败\n" + e)
            }
            js("undefined")
        })
    } else {
        userManagerApi.showLogin({
            userManagerApi.showAuthorize(query)
            js("undefined")
        })
    }
}

private fun umShowAdminAuth(callback: dynamic) {
    js("Common").showProgressDialog({ dia: dynamic ->
        dia.setText("正在以管理员权限登录..")
        var ok = true
        try {
            userManagerApi.acquireAdminToken()
        } catch (e: dynamic) {
            js("Log").e(e)
            ok = false
            js("Common").toast("使用管理员权限登录失败\n" + e)
        }
        if (ok) {
            js("G").ui({
                try {
                    if (umTruthy(callback)) callback()
                } catch (e: dynamic) {
                    js("erp")(e)
                }
                js("undefined")
            })
        }
        js("undefined")
    })
}

private fun umSubmitChangePassword(fields: dynamic, popup: dynamic) {
    if (!umTruthy(fields.oldpwd.length())) {
        js("Common").toast("原密码不能为空")
        return
    }
    if (!umTruthy(fields.newpwd.length())) {
        js("Common").toast("新密码不能为空")
        return
    }
    if (umString(fields.newpwd.text) != umString(fields.newpwd2.text)) {
        js("Common").toast("两次新密码输入不一致")
        return
    }
    js("Common").showProgressDialog({ dia: dynamic ->
        dia.setText("正在保存密码..")
        var ok = true
        try {
            userManagerApi.setPassword(umString(fields.oldpwd.text), umString(fields.newpwd.text))
        } catch (e: dynamic) {
            js("Log").e(e)
            ok = false
            js("Common").toast("密码保存失败\n" + e)
        }
        if (ok) {
            js("G").ui({
                try {
                    popup.exit()
                    js("Common").toast("密码已保存")
                } catch (e: dynamic) {
                    js("erp")(e)
                }
                js("undefined")
            })
        }
        js("undefined")
    })
}

private fun umShowChangePassword() {
    val oldpwd = umNewObject()
    oldpwd.name = "oldpwd"
    oldpwd.hint = "旧密码"
    oldpwd.password = true

    val newpwd = umNewObject()
    newpwd.name = "newpwd"
    newpwd.hint = "新密码"
    newpwd.password = true

    val newpwd2 = umNewObject()
    newpwd2.name = "newpwd2"
    newpwd2.hint = "确认新密码"
    newpwd2.password = true

    val fields = umEmptyArray()
    fields.push(oldpwd)
    fields.push(newpwd)
    fields.push(newpwd2)

    val save = umNewObject()
    save.text = "设置密码"
    save.padding = umNewPadding(umDp(10), umDp(15), umDp(10), umDp(15))
    save.onClick = { f: dynamic, p: dynamic ->
        umSubmitChangePassword(f, p)
        js("undefined")
    }

    val buttons = umEmptyArray()
    buttons.push(save)

    val opts = umNewObject()
    opts.popupName = "usermanager.ForgetPassword"
    opts.title = "更改密码"
    opts.fields = fields
    opts.buttons = buttons
    js("NetworkUI").formDialog(opts)
}

private fun umShowChangeName(callback: dynamic) {
    val userInfo = userManagerApi.userInfo
    val opts = umNewObject()
    opts.title = "更改用户名"
    opts.defaultValue = userInfo.name
    opts.callback = { s: dynamic ->
        js("Common").showProgressDialog({ dia: dynamic ->
            dia.setText("正在保存用户名..")
            var ok = true
            try {
                userManagerApi.setUsername(s)
                userInfo.name = s
            } catch (e: dynamic) {
                js("Log").e(e)
                ok = false
                js("Common").toast("用户名保存失败\n" + e)
            }
            if (ok) {
                js("G").ui({
                    try {
                        js("Common").toast("用户名已保存")
                        if (umTruthy(callback)) callback()
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
    js("Common").showInputDialog(opts)
}

private fun umShowSyncExp(callback: dynamic, silent: dynamic) {
    if (umTruthy(silent)) {
        js("Threads").run({
            try {
                userManagerApi.syncExp()
                if (umTruthy(callback)) callback()
            } catch (e: dynamic) {
                js("Log").e(e)
            }
            js("undefined")
        })
    } else {
        js("Common").showProgressDialog({ dia: dynamic ->
            dia.setText("正在同步经验...")
            try {
                val result = userManagerApi.syncExp()
                userManagerApi.showExpManage(result, callback)
            } catch (e: dynamic) {
                js("Log").e(e)
                js("Common").toast("同步经验失败\n" + e)
            }
            js("undefined")
        })
    }
}

private fun umShowExpManage(expAdd: dynamic, callback: dynamic) {
    js("G").ui({
        try {
            var popup: dynamic = js("undefined")
            val userInfo = userManagerApi.userInfo
            val viewList = umEmptyArray()
            val exp = userManagerApi.parseExpLevel(userInfo.experience)
            val mode = when {
                !umTruthy(expAdd) -> "offline"
                expAdd.add > 0 -> "add"
                expAdd.today_experience >= expAdd.max_today_exp -> "info_full"
                else -> "info"
            }
            val checkedIn = umTruthy(expAdd) && expAdd.reasons["checkin"] > 0

            val title = umNewObject()
            title.text = "每日经验"
            title.padding = umNewPadding(0, 0, 0, umDp(15))
            title.layout = umNewLayout(-1, -2)
            title.gravity = js("L").Gravity("center")
            title.style = "textview_default"
            title.fontSize = 4
            viewList.push(js("L").TextView(title))

            if (mode == "offline") {
                val reasonText = umNewObject()
                reasonText.text = "当前命令助手运行在离线模式下，签到与经验系统自动被禁用。\n\n技术原因：\n" +
                    userManagerApi.getOfflineReason()
                reasonText.padding = umNewPadding(0, umDp(10), 0, umDp(10))
                reasonText.layout = umNewLayout(-1, -2)
                reasonText.style = "textview_default"
                reasonText.fontSize = 1
                viewList.push(js("L").TextView(reasonText))

                val detail = umNewObject()
                detail.text = "了解详情"
                detail.padding = umNewPadding(0, umDp(15), 0, umDp(15))
                detail.gravity = js("L").Gravity("left")
                detail.layout = umNewLayout(-1, -2)
                detail.style = "button_highlight"
                detail.fontSize = 3
                detail.onClick = {
                    try {
                        js("AndroidBridge").viewUri("https://ca.projectxero.top/blog/notice/why-show-ads/")
                        popup.exit()
                    } catch (e: dynamic) {
                        js("erp")(e)
                    }
                    js("undefined")
                }
                viewList.push(js("L").TextView(detail))
            } else {
                val progress = umNewObject()
                progress.orientation = js("L").LinearLayout("horizontal")
                progress.backgroundColor = js("Common").theme.textcolor
                progress.layout = umNewLayout(-1, umDp(2))
                progress.weightSum = expAdd.max_today_exp
                val progressFill = umNewObject()
                progressFill.backgroundColor = js("Common").theme.criticalcolor
                progressFill.layout = umNewWeightedLayout(0, -1, expAdd.today_experience)
                progress.child = js("L").View(progressFill)
                viewList.push(js("L").LinearLayout(progress))

                val labels = umNewObject()
                labels.orientation = js("L").LinearLayout("horizontal")
                labels.layout = umNewLayout(-1, -2)
                labels.padding = umNewPadding(0, 0, 0, umDp(5))
                val labelChildren = umEmptyArray()

                val left = umNewObject()
                left.text = if (mode == "add") {
                    "+" + expAdd.add + if (expAdd.today_experience >= expAdd.max_today_exp) "(到达上限)" else ""
                } else {
                    "今日获得"
                }
                left.layout = umNewWeightedLayout(-2, -2, 1.0)
                left.style = "textview_default"
                left.fontSize = 1
                labelChildren.push(js("L").TextView(left))

                val right = umNewObject()
                right.text = expAdd.today_experience + "/" + expAdd.max_today_exp
                right.layout = umNewLayout(-2, -2)
                right.style = "textview_default"
                right.fontSize = 1
                labelChildren.push(js("L").TextView(right))

                labels.children = labelChildren
                viewList.push(js("L").LinearLayout(labels))

                if (mode == "add") {
                    umForIn(expAdd.reasons) { key: dynamic ->
                        val reasonCount = expAdd.reasons[key]
                        val reasonInfo = userManagerApi.reasons[key]
                        val row = umNewObject()
                        row.orientation = js("L").LinearLayout("horizontal")
                        row.gravity = js("L").Gravity("center")
                        row.layout = umNewLayout(-1, -2)
                        val children = umEmptyArray()

                        val nameText = umNewObject()
                        nameText.text = reasonInfo.name
                        nameText.padding = umNewPadding(0, umDp(10), 0, umDp(10))
                        nameText.gravity = js("L").Gravity("left")
                        nameText.layout = umNewLayout(-2, -2)
                        nameText.style = "textview_default"
                        nameText.fontSize = 3
                        children.push(js("L").TextView(nameText))

                        val countText = umNewObject()
                        countText.text = "(" + reasonCount + "次)"
                        countText.visibility = js("L").View(if (reasonCount > 1) "visible" else "gone")
                        countText.layout = umNewLayout(-2, -2)
                        countText.style = "textview_prompt"
                        countText.fontSize = 1
                        children.push(js("L").TextView(countText))

                        val space = umNewObject()
                        space.layout = umNewWeightedLayout(0, -1, 1.0)
                        children.push(js("L").Space(space))

                        val expText = umNewObject()
                        expText.text = "+" + reasonCount * reasonInfo.exp
                        expText.layout = umNewLayout(-2, -2)
                        expText.style = "textview_prompt"
                        expText.fontSize = 1
                        children.push(js("L").TextView(expText))

                        row.children = children
                        viewList.push(js("L").LinearLayout(row))
                    }
                } else if (mode == "info") {
                    val ways = umNewObject()
                    ways.text = "获取途径"
                    ways.padding = umNewPadding(0, umDp(20), 0, umDp(15))
                    ways.layout = umNewLayout(-1, -2)
                    ways.gravity = js("L").Gravity("center")
                    ways.style = "textview_default"
                    ways.fontSize = 4
                    viewList.push(js("L").TextView(ways))

                    umForIn(userManagerApi.reasons) { key: dynamic ->
                        val reasonInfo = userManagerApi.reasons[key]
                        val row = umNewObject()
                        row.orientation = js("L").LinearLayout("horizontal")
                        row.gravity = js("L").Gravity("center")
                        row.layout = umNewLayout(-1, -2)
                        val children = umEmptyArray()

                        val nameText = umNewObject()
                        nameText.text = reasonInfo.name
                        nameText.padding = umNewPadding(0, umDp(10), 0, umDp(10))
                        nameText.gravity = js("L").Gravity("left")
                        nameText.layout = umNewLayout(-2, -2)
                        nameText.style = "textview_default"
                        nameText.fontSize = 3
                        children.push(js("L").TextView(nameText))

                        val space = umNewObject()
                        space.layout = umNewWeightedLayout(0, -1, 1.0)
                        children.push(js("L").Space(space))

                        val expText = umNewObject()
                        expText.text = "+" + reasonInfo.exp
                        expText.layout = umNewLayout(-2, -2)
                        expText.style = "textview_prompt"
                        expText.fontSize = 1
                        children.push(js("L").TextView(expText))

                        row.children = children
                        viewList.push(js("L").LinearLayout(row))
                    }
                } else {
                    val full = umNewObject()
                    full.text = "本日获取经验\n已到达上限"
                    full.padding = umNewPadding(0, umDp(20), 0, umDp(15))
                    full.layout = umNewLayout(-1, -2)
                    full.gravity = js("L").Gravity("center")
                    full.style = "textview_prompt"
                    full.fontSize = 3
                    viewList.push(js("L").TextView(full))
                }
            }

            val bottomSpace = umNewObject()
            bottomSpace.layout = umNewWeightedLayout(-1, 0, 1.0)
            viewList.push(js("L").Space(bottomSpace))

            val checkInTitle = umNewObject()
            checkInTitle.text = "签到"
            checkInTitle.padding = umNewPadding(0, umDp(20), 0, umDp(15))
            checkInTitle.layout = umNewLayout(-1, -2)
            checkInTitle.gravity = js("L").Gravity("center")
            checkInTitle.style = "textview_default"
            checkInTitle.fontSize = 4
            viewList.push(js("L").TextView(checkInTitle))

            val continuous = umNewObject()
            continuous.orientation = js("L").LinearLayout("horizontal")
            continuous.gravity = js("L").Gravity("center")
            continuous.layout = umNewLayout(-1, -2)
            val continuousChildren = umEmptyArray()

            val continuousName = umNewObject()
            continuousName.text = "连续签到"
            continuousName.padding = umNewPadding(0, umDp(10), 0, umDp(10))
            continuousName.gravity = js("L").Gravity("left")
            continuousName.layout = umNewWeightedLayout(-2, -2, 1.0)
            continuousName.style = "textview_default"
            continuousName.fontSize = 3
            continuousChildren.push(js("L").TextView(continuousName))

            val continuousDays = umNewObject()
            continuousDays.text = userInfo.checkInContinuousDays + "天"
            continuousDays.layout = umNewLayout(-2, -2)
            continuousDays.style = "textview_default"
            continuousDays.fontSize = 1
            continuousChildren.push(js("L").TextView(continuousDays))

            val continuousPlus = umNewObject()
            continuousPlus.text = "(+1)"
            continuousPlus.visibility = js("L").View(if (checkedIn) "visible" else "gone")
            continuousPlus.layout = umNewLayout(-2, -2)
            continuousPlus.style = "textview_prompt"
            continuousPlus.fontSize = 1
            continuousChildren.push(js("L").TextView(continuousPlus))

            continuous.children = continuousChildren
            viewList.push(js("L").LinearLayout(continuous))

            val total = umNewObject()
            total.orientation = js("L").LinearLayout("horizontal")
            total.gravity = js("L").Gravity("center")
            total.layout = umNewLayout(-1, -2)
            val totalChildren = umEmptyArray()

            val totalName = umNewObject()
            totalName.text = "累计签到"
            totalName.padding = umNewPadding(0, umDp(10), 0, umDp(10))
            totalName.gravity = js("L").Gravity("left")
            totalName.layout = umNewWeightedLayout(-2, -2, 1.0)
            totalName.style = "textview_default"
            totalName.fontSize = 3
            totalChildren.push(js("L").TextView(totalName))

            val totalDays = umNewObject()
            totalDays.text = userInfo.checkInTotalDays + "天"
            totalDays.layout = umNewLayout(-2, -2)
            totalDays.style = "textview_default"
            totalDays.fontSize = 1
            totalChildren.push(js("L").TextView(totalDays))

            val totalPlus = umNewObject()
            totalPlus.text = "(+1)"
            totalPlus.visibility = js("L").View(if (checkedIn) "visible" else "gone")
            totalPlus.layout = umNewLayout(-2, -2)
            totalPlus.style = "textview_prompt"
            totalPlus.fontSize = 1
            totalChildren.push(js("L").TextView(totalPlus))

            total.children = totalChildren
            viewList.push(js("L").LinearLayout(total))

            val totalExpTitle = umNewObject()
            totalExpTitle.text = "累计经验"
            totalExpTitle.padding = umNewPadding(0, umDp(20), 0, umDp(15))
            totalExpTitle.layout = umNewLayout(-1, -2)
            totalExpTitle.gravity = js("L").Gravity("center")
            totalExpTitle.style = "textview_default"
            totalExpTitle.fontSize = 4
            viewList.push(js("L").TextView(totalExpTitle))

            val expBar = umNewObject()
            expBar.orientation = js("L").LinearLayout("horizontal")
            expBar.backgroundColor = js("Common").theme.promptcolor
            expBar.layout = umNewLayout(-1, umDp(2))
            expBar.weightSum = exp.levelExp
            val expBarChildren = umEmptyArray()

            val restBar = umNewObject()
            restBar.backgroundColor = js("Common").theme.highlightcolor
            restBar.layout = umNewWeightedLayout(0, -1, exp.rest)
            expBarChildren.push(js("L").View(restBar))

            if (mode != "offline") {
                val remainBar = umNewObject()
                remainBar.backgroundColor = js("Common").theme.textcolor
                remainBar.layout = umNewWeightedLayout(0, -1, expAdd.max_today_exp - expAdd.today_experience)
                expBarChildren.push(js("L").View(remainBar))
            } else {
                val gone = umNewObject()
                gone.visibility = js("L").View("gone")
                expBarChildren.push(js("L").View(gone))
            }

            expBar.children = expBarChildren
            viewList.push(js("L").LinearLayout(expBar))

            val levelRow = umNewObject()
            levelRow.orientation = js("L").LinearLayout("horizontal")
            levelRow.layout = umNewLayout(-1, -2)
            levelRow.padding = umNewPadding(0, 0, 0, umDp(5))
            val levelChildren = umEmptyArray()

            val levelText = umNewObject()
            levelText.text = "Lv. " + exp.level
            levelText.layout = umNewWeightedLayout(-2, -2, 1.0)
            levelText.style = "textview_default"
            levelText.fontSize = 1
            levelChildren.push(js("L").TextView(levelText))

            val expText = umNewObject()
            expText.text = exp.rest + "/" + exp.levelExp
            expText.layout = umNewLayout(-2, -2)
            expText.style = "textview_default"
            expText.fontSize = 1
            levelChildren.push(js("L").TextView(expText))

            levelRow.children = levelChildren
            viewList.push(js("L").LinearLayout(levelRow))

            viewList.push(js("NetworkUI").closeButton({ popup }))

            val scroll = umNewObject()
            scroll.style = "message_bg"
            scroll.fillViewport = true
            val linear = umNewObject()
            linear.orientation = js("L").LinearLayout("vertical")
            linear.padding = umNewPadding(umDp(20), umDp(20), umDp(20), 0)
            linear.layout = umNewLayout(-1, -1)
            linear.children = viewList
            scroll.child = js("L").LinearLayout(linear)

            popup = js("PopupPage").showSideBar(
                "usermanager.ExpManage", js("L").ScrollView(scroll), "left", umDp(160), 0.2
            )
            if (umTruthy(callback)) popup.on("exit", callback)
            popup.enter()
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

// ---------------- 设置 / 管理界面 ----------------
private fun umGetSettingItem(): dynamic {
    val item = umNewObject()
    item.type = "custom"
    item.get = umThis0Js { self: dynamic ->
        val userInfo = userManagerApi.userInfo
        if (umTruthy(userInfo)) {
            self.name = userInfo.name
            self.description = userInfo.email
        } else {
            self.name = "未登录"
            self.description = "点击登录命令助手账号"
        }
        ""
    }
    item.onclick = { fset: dynamic ->
        if (umTruthy(userManagerApi.userInfo)) {
            userManagerApi.showManage(fset)
        } else {
            userManagerApi.showLogin(fset)
        }
        js("undefined")
    }
    return item
}

private fun umShowManage(callback: dynamic) {
    js("G").ui({
        try {
            var popup: dynamic = js("undefined")
            val userInfo = userManagerApi.userInfo
            val exp = userManagerApi.parseExpLevel(userInfo.experience)
            val hasExpToSync = userManagerApi.hasExpToSync()

            val title = umNewObject()
            title.text = userInfo.name
            title.layout = umNewLayout(-1, -2)
            title.style = "textview_default"
            title.fontSize = 4

            val email = umNewObject()
            email.text = userInfo.email
            email.layout = umNewLayout(-1, -2)
            email.padding = umNewPadding(0, 0, 0, umDp(5))
            email.style = "textview_prompt"
            email.fontSize = 1

            val progress = umNewObject()
            progress.orientation = js("L").LinearLayout("horizontal")
            progress.backgroundColor = js("Common").theme.promptcolor
            progress.layout = umNewLayout(-1, umDp(2))
            progress.weightSum = exp.levelExp
            val progressFill = umNewObject()
            progressFill.backgroundColor = js("Common").theme.highlightcolor
            progressFill.layout = umNewWeightedLayout(0, -1, exp.rest)
            progress.child = js("L").View(progressFill)

            val levelRow = umNewObject()
            levelRow.orientation = js("L").LinearLayout("horizontal")
            levelRow.layout = umNewLayout(-1, -2)
            levelRow.padding = umNewPadding(0, 0, 0, umDp(5))
            val levelChildren = umEmptyArray()

            val levelText = umNewObject()
            levelText.text = "Lv. " + exp.level
            levelText.layout = umNewWeightedLayout(-2, -2, 1.0)
            levelText.style = "textview_default"
            levelText.fontSize = 1
            levelChildren.push(js("L").TextView(levelText))

            val expText = umNewObject()
            expText.text = exp.rest + "/" + exp.levelExp
            expText.layout = umNewLayout(-2, -2)
            expText.style = "textview_default"
            expText.fontSize = 1
            levelChildren.push(js("L").TextView(expText))

            levelRow.children = levelChildren

            val sync = umNewObject()
            sync.text = if (umTruthy(hasExpToSync) && !umTruthy(userInfo.checkedIn)) "签到" else "同步经验"
            sync.padding = umNewPadding(0, umDp(15), 0, umDp(15))
            sync.gravity = js("L").Gravity("left")
            sync.layout = umNewLayout(-1, -2)
            sync.style = if (umTruthy(hasExpToSync)) "button_critical" else "button_highlight"
            sync.fontSize = 3
            sync.onClick = {
                try {
                    if (umTruthy(userManagerApi.isOnline())) {
                        userManagerApi.showSyncExp(callback)
                    } else {
                        userManagerApi.showExpManage(null)
                    }
                    popup.exit()
                } catch (e: dynamic) {
                    js("erp")(e)
                }
                js("undefined")
            }

            val changeName = umNewObject()
            changeName.text = "更改用户名"
            changeName.padding = umNewPadding(0, umDp(15), 0, umDp(15))
            changeName.gravity = js("L").Gravity("left")
            changeName.layout = umNewLayout(-1, -2)
            changeName.style = "button_highlight"
            changeName.fontSize = 3
            changeName.onClick = {
                try {
                    userManagerApi.showChangeName(callback)
                    popup.exit()
                } catch (e: dynamic) {
                    js("erp")(e)
                }
                js("undefined")
            }

            val changePassword = umNewObject()
            changePassword.text = "更改密码"
            changePassword.padding = umNewPadding(0, umDp(15), 0, umDp(15))
            changePassword.gravity = js("L").Gravity("left")
            changePassword.layout = umNewLayout(-1, -2)
            changePassword.style = "button_highlight"
            changePassword.fontSize = 3
            changePassword.onClick = {
                try {
                    userManagerApi.showChangePassword()
                    popup.exit()
                } catch (e: dynamic) {
                    js("erp")(e)
                }
                js("undefined")
            }

            val logout = umNewObject()
            logout.text = "注销"
            logout.padding = umNewPadding(0, umDp(15), 0, umDp(15))
            logout.gravity = js("L").Gravity("left")
            logout.layout = umNewLayout(-1, -2)
            logout.style = "button_highlight"
            logout.fontSize = 3
            logout.onClick = {
                try {
                    userManagerApi.logout()
                    if (umTruthy(callback)) callback()
                    popup.exit()
                } catch (e: dynamic) {
                    js("erp")(e)
                }
                js("undefined")
            }

            val admin = umNewObject()
            admin.text = "进入管理界面"
            admin.visibility = js("L").View(if (umTruthy(userManagerApi.isAdmin())) "visible" else "gone")
            admin.padding = umNewPadding(0, umDp(15), 0, umDp(15))
            admin.gravity = js("L").Gravity("left")
            admin.layout = umNewLayout(-1, -2)
            admin.style = "button_highlight"
            admin.fontSize = 3
            admin.onClick = {
                try {
                    userManagerApi.showAdminAuth({
                        js("DebugUtils").showDebugDialog(userManagerApi.getDebugInterface())
                        js("undefined")
                    })
                    popup.exit()
                } catch (e: dynamic) {
                    js("erp")(e)
                }
                js("undefined")
            }

            val spacer = umNewObject()
            spacer.layout = umNewWeightedLayout(-1, 0, 1.0)

            val children = umEmptyArray()
            children.push(js("L").TextView(title))
            children.push(js("L").TextView(email))
            children.push(js("L").LinearLayout(progress))
            children.push(js("L").LinearLayout(levelRow))
            children.push(js("L").TextView(sync))
            children.push(js("L").TextView(changeName))
            children.push(js("L").TextView(changePassword))
            children.push(js("L").TextView(logout))
            children.push(js("L").TextView(admin))
            children.push(js("L").Space(spacer))
            children.push(js("NetworkUI").closeButton({ popup }))

            val linear = umNewObject()
            linear.orientation = js("L").LinearLayout("vertical")
            linear.padding = umNewPadding(umDp(20), umDp(20), umDp(20), 0)
            linear.layout = umNewLayout(-1, -1)
            linear.children = children

            val scroll = umNewObject()
            scroll.style = "message_bg"
            scroll.fillViewport = true
            scroll.child = js("L").LinearLayout(linear)

            popup = js("PopupPage").showSideBar(
                "usermanager.Manage", js("L").ScrollView(scroll), "left", umDp(160), 0.2
            )
            popup.enter()
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
}

private fun umGetDebugInterface(): dynamic {
    if (umDebugInterfaceCache != null &&
        umDebugInterfaceCache.accessToken == userManagerApi.accessToken
    ) {
        return umDebugInterfaceCache
    }
    val scope = umObjectCreateJs(userManagerApi.internal)
    scope.list = umBindJs(userManagerApi.executeAdminAction, userManagerApi, "Admin.listActions")
    scope.action = umBindJs(userManagerApi.executeAdminAction, userManagerApi)
    scope.lastError = umBindJs(userManagerApi.executeAdminAction, userManagerApi, "Admin.getLastError")

    val cache = umNewObject()
    cache.accessToken = userManagerApi.accessToken
    cache.getWelcomeText = {
        "欢迎使用管理员控制台 本控制台拓展了默认控制台的功能"
    }
    cache.getGlobal = {
        js("MapScript").global
    }
    cache.evalExpr = { expr: dynamic ->
        js("Loader").evalSpecial(expr, "AdminDebugInterface", 0, scope, null)
    }
    cache.onCommand = { _cmd: dynamic -> false }
    cache.setPrinter = {
        js("undefined")
    }
    umDebugInterfaceCache = cache
    return cache
}

private fun umOnCreate() {
    js("Internal").add("UserManager", userManagerApi)
}

private fun umInitialize() {
    js("NetworkUtils").addErrorMessages(userManagerApi.errorMessage)
    umLoadToken()
}

fun installUserManager() {
    userManagerApi.apiHost = js("NetworkUtils").urlBase.api
    userManagerApi.errorMessage = umErrorMessages
    userManagerApi.reasons = umReasons
    userManagerApi.publicUserInfoCache = umPublicUserInfoCache
    userManagerApi.expQueue = umNewQueueJs()

    userManagerApi.login = { emailOrName: dynamic, password: dynamic ->
        umLogin(emailOrName, password)
        js("undefined")
    }
    userManagerApi.logout = {
        umLogout()
        js("undefined")
    }
    userManagerApi.register = { email: dynamic, name: dynamic, password: dynamic ->
        umRegister(email, name, password)
        js("undefined")
    }
    userManagerApi.refreshLogin = { refreshToken: dynamic ->
        umRefreshLogin(refreshToken)
        js("undefined")
    }
    userManagerApi.getUserInfo = { umGetUserInfo() }
    userManagerApi.getPublicUserInfo = { id: dynamic -> umGetPublicUserInfo(id) }
    userManagerApi.authorize = { umAuthorize() }
    userManagerApi.requestResetPassword = { email: dynamic, password: dynamic ->
        umRequestResetPassword(email, password)
        js("undefined")
    }
    userManagerApi.setPassword = { oldPassword: dynamic, newPassword: dynamic ->
        umSetPassword(oldPassword, newPassword)
        js("undefined")
    }
    userManagerApi.setUsername = { name: dynamic ->
        umSetUsername(name)
        js("undefined")
    }
    userManagerApi.changeEmail = { email: dynamic ->
        umChangeEmail(email)
        js("undefined")
    }
    userManagerApi.requestRemove = {
        umRequestRemove()
        js("undefined")
    }
    userManagerApi.checkIn = { umCheckIn() }
    userManagerApi.addExp = { reasons: dynamic -> umAddExp(reasons) }
    userManagerApi.acquireAdminToken = {
        umAcquireAdminToken()
        js("undefined")
    }
    userManagerApi.executeAdminAction = { name: dynamic, data: dynamic ->
        umExecuteAdminAction(name, data)
    }
    userManagerApi.allocateVisitorToken = { umAllocateVisitorToken() }
    userManagerApi.clearVisitorToken = { token: dynamic, secret: dynamic ->
        umClearVisitorToken(token, secret)
        js("undefined")
    }
    userManagerApi.getVisitorID = { token: dynamic -> umGetVisitorID(token) }

    userManagerApi.loadToken = {
        umLoadToken()
        js("undefined")
    }
    userManagerApi.saveToken = { result: dynamic ->
        umSaveToken(result)
        js("undefined")
    }
    userManagerApi.clearToken = {
        umClearToken()
        js("undefined")
    }
    userManagerApi.updateUserInfo = {
        umUpdateUserInfo()
        js("undefined")
    }
    userManagerApi.getCachedUserInfo = { umGetCachedUserInfo() }
    userManagerApi.getVisitorTokenCached = { umGetVisitorTokenCached() }
    userManagerApi.getVisitorIDCached = { umGetVisitorIDCached() }
    userManagerApi.allocateActor = { umAllocateActor() }
    userManagerApi.isMyActor = { creatorType: dynamic, creatorID: dynamic ->
        umIsMyActor(creatorType, creatorID)
    }
    userManagerApi.getPublicUserInfoCached = { id: dynamic, forceUpdate: dynamic ->
        umGetPublicUserInfoCached(id, forceUpdate)
    }
    userManagerApi.getMyActorName = { umGetMyActorName() }
    userManagerApi.getActorName = { creatorType: dynamic, creatorID: dynamic ->
        umGetActorName(creatorType, creatorID)
    }
    userManagerApi.isAdmin = { umIsAdmin() }
    userManagerApi.isOnline = { umIsOnline() }
    userManagerApi.getOfflineReason = { umGetOfflineReason() }
    userManagerApi.getLevelExp = { level: dynamic -> umGetLevelExp(level) }
    userManagerApi.parseExpLevel = { exp: dynamic -> umParseExpLevel(exp) }
    userManagerApi.enqueueExp = { reason: dynamic ->
        umEnqueueExp(reason)
        js("undefined")
    }
    userManagerApi.hasExpToSync = { umHasExpToSync() }
    userManagerApi.syncExp = { umSyncExp() }
    userManagerApi.processUriAction = { type: dynamic, query: dynamic ->
        umProcessUriAction(type, query)
        js("undefined")
    }

    userManagerApi.showLogin = { callback: dynamic ->
        umShowLogin(callback)
        js("undefined")
    }
    userManagerApi.showRegister = {
        umShowRegister()
        js("undefined")
    }
    userManagerApi.showRefreshLogin = { refreshToken: dynamic, silent: dynamic ->
        umShowRefreshLogin(refreshToken, silent)
        js("undefined")
    }
    userManagerApi.showUpdateUserInfo = {
        umShowUpdateUserInfo()
        js("undefined")
    }
    userManagerApi.showForgetPassword = {
        umShowForgetPassword()
        js("undefined")
    }
    userManagerApi.showAuthorize = { query: dynamic ->
        umShowAuthorize(query)
        js("undefined")
    }
    userManagerApi.showAdminAuth = { callback: dynamic ->
        umShowAdminAuth(callback)
        js("undefined")
    }
    userManagerApi.showChangePassword = {
        umShowChangePassword()
        js("undefined")
    }
    userManagerApi.showChangeName = { callback: dynamic ->
        umShowChangeName(callback)
        js("undefined")
    }
    userManagerApi.showSyncExp = { callback: dynamic, silent: dynamic ->
        umShowSyncExp(callback, silent)
        js("undefined")
    }
    userManagerApi.showExpManage = { expAdd: dynamic, callback: dynamic ->
        umShowExpManage(expAdd, callback)
        js("undefined")
    }
    userManagerApi.getSettingItem = { umGetSettingItem() }
    userManagerApi.showManage = { callback: dynamic ->
        umShowManage(callback)
        js("undefined")
    }
    userManagerApi.getDebugInterface = { umGetDebugInterface() }

    userManagerApi.onCreate = {
        umOnCreate()
        js("undefined")
    }
    userManagerApi.initialize = {
        umInitialize()
        js("undefined")
    }

    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("UserManager", userManagerApi)
    }
}
