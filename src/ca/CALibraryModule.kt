package ca.kotlin.ca

import ca.kotlin.core.*


/**
 * modules/CA.Library.js 的 Kotlin/JS 迁移。
 *
 * 与原文件不同，这里不直接构造全局 CA.Library，也不调用 MapScript.loadModule；
 * 只提供 buildCaLibraryModule()，由 CA.kt 在总装时执行：
 *
 *     caApi.Library = buildCaLibraryModule()
 *
 * 迁移约定：
 *  - 原对象字面量里的 this 改成显式局部对象 libApi；内部方法之间只通过 libApi 调用。
 *  - 外部模块（CA.settings / CA.IntelliSense / Common / ExternalStorage / Loader /
 *    Threads / NeteaseAdapter / NetworkUtils / Tutorial / ScriptInterface 等）保持
 *    和原文件一样的动态查找，不提前缓存。
 *  - JS 的 truthy、宽松相等、typeof、in、delete、数组/Java 数组 length、加法拼接
 *    等语义都走带参 JS helper；js("...") 里不引用 Kotlin 局部变量。
 *  - 原有 bug 按原样保留：
 *      * removeLibrary 在清理安装器记录之前就 return，后面的清理是死代码；
 *      * addLibrary 计算了 cmp 但从未使用；
 *      * checkLibrary 内部的 iterateObject 把未定义的 v 传给 valueIter（该分支未被调用）；
 *      * loadLibrary 的 catch 回退里 m 可能是 undefined；
 *      * filterCmd 重复声明了 t（对行为无影响）。
 *  - 原文件里 `function (o) { ... this.ignoreVersion ... }`、stat.toString 和
 *    checkLibrary 错误对象的 toString 依赖动态 this；这里分别通过 libApi 或
 *    专门的 JS 函数保留。
 *  - 内置库的 eval 用直接 eval helper；它不再能看见原函数里的局部变量，但除此之外
 *    的求值行为与旧文件一致。
 */

// ---------------- JS 语义 helper ----------------

private val clmTruthyJs: dynamic = js("function (v) { return !!v; }")
private fun clmTruthy(v: dynamic): Boolean = clmTruthyJs(v)

private val clmLengthJs: dynamic = js("function (v) { return v.length | 0; }")
private fun clmLength(v: dynamic): Int = clmLengthJs(v)

private val clmStringJs: dynamic = js("function (v) { return String(v); }")
private fun clmString(v: dynamic): dynamic = clmStringJs(v)

private val clmTypeofJs: dynamic = js("function (v) { return typeof v; }")
private fun clmTypeof(v: dynamic): dynamic = clmTypeofJs(v)

private val clmIsArrayJs: dynamic = js("function (v) { return Array.isArray(v); }")
private fun clmIsArray(v: dynamic): Boolean = clmIsArrayJs(v)

private val clmIsFunctionJs: dynamic = js("function (v) { return typeof v === 'function'; }")
private fun clmIsFunction(v: dynamic): Boolean = clmIsFunctionJs(v)

private val clmInstanceOfJs: dynamic = js("function (v, c) { return v instanceof c; }")
private fun clmInstanceOf(v: dynamic, c: dynamic): Boolean = clmInstanceOfJs(v, c)

private val clmLooseEqJs: dynamic = js("function (a, b) { return a == b; }")
private fun clmLooseEq(a: dynamic, b: dynamic): Boolean = clmLooseEqJs(a, b)

private val clmLooseNeJs: dynamic = js("function (a, b) { return a != b; }")
private fun clmLooseNe(a: dynamic, b: dynamic): Boolean = clmLooseNeJs(a, b)

private val clmStrictEqJs: dynamic = js("function (a, b) { return a === b; }")
private fun clmStrictEq(a: dynamic, b: dynamic): Boolean = clmStrictEqJs(a, b)

private val clmStrictNeJs: dynamic = js("function (a, b) { return a !== b; }")
private fun clmStrictNe(a: dynamic, b: dynamic): Boolean = clmStrictNeJs(a, b)

private val clmLtJs: dynamic = js("function (a, b) { return a < b; }")
private fun clmLt(a: dynamic, b: dynamic): Boolean = clmLtJs(a, b)

private val clmLeJs: dynamic = js("function (a, b) { return a <= b; }")
private fun clmLe(a: dynamic, b: dynamic): Boolean = clmLeJs(a, b)

private val clmGtJs: dynamic = js("function (a, b) { return a > b; }")
private fun clmGt(a: dynamic, b: dynamic): Boolean = clmGtJs(a, b)

private val clmGeJs: dynamic = js("function (a, b) { return a >= b; }")
private fun clmGe(a: dynamic, b: dynamic): Boolean = clmGeJs(a, b)

private val clmAddJs: dynamic = js("function (a, b) { return a + b; }")
private fun clmAdd(a: dynamic, b: dynamic): dynamic = clmAddJs(a, b)

private val clmSubJs: dynamic = js("function (a, b) { return a - b; }")
private fun clmSub(a: dynamic, b: dynamic): dynamic = clmSubJs(a, b)

private val clmOrJs: dynamic = js("function (a, b) { return a || b; }")
private fun clmOr(a: dynamic, b: dynamic): dynamic = clmOrJs(a, b)

private val clmAndJs: dynamic = js("function (a, b) { return a && b; }")
private fun clmAnd(a: dynamic, b: dynamic): dynamic = clmAndJs(a, b)

private val clmInJs: dynamic = js("function (k, o) { return k in o; }")
private fun clmIn(k: dynamic, o: dynamic): Boolean = clmInJs(k, o)

private val clmDeleteJs: dynamic = js("function (o, k) { delete o[k]; }")
private fun clmDelete(o: dynamic, k: dynamic) {
    clmDeleteJs(o, k)
}

private val clmForInJs: dynamic = js("function (o, f) { for (var k in o) f(k); }")
private fun clmForIn(o: dynamic, f: dynamic) {
    clmForInJs(o, f)
}

private val clmObjectKeysJs: dynamic = js("function (o) { return Object.keys(o); }")
private fun clmObjectKeys(o: dynamic): dynamic = clmObjectKeysJs(o)

private val clmObjectCopyJs: dynamic = js("function (o) { return Object.copy(o); }")
private fun clmObjectCopy(o: dynamic): dynamic = clmObjectCopyJs(o)

private val clmJsonParseJs: dynamic = js("function (v) { return JSON.parse(v); }")
private fun clmJsonParse(v: dynamic): dynamic = clmJsonParseJs(v)

private val clmDateParseJs: dynamic = js("function (v) { return Date.parse(v); }")
private fun clmDateParse(v: dynamic): dynamic = clmDateParseJs(v)

private val clmDateNowJs: dynamic = js("function () { return Date.now(); }")
private fun clmDateNow(): dynamic = clmDateNowJs()

private val clmIsNaNRawJs: dynamic = js("function (v) { return isNaN(v); }")
private fun clmIsNaN(v: dynamic): Boolean = clmIsNaNRawJs(v)

private val clmParseIntJs: dynamic = js("function (v) { return parseInt(v); }")
private fun clmParseInt(v: dynamic): dynamic = clmParseIntJs(v)

private val clmMathMaxJs: dynamic = js("function (a, b) { return Math.max(a, b); }")
private fun clmMathMax(a: dynamic, b: dynamic): dynamic = clmMathMaxJs(a, b)

private val clmMathMinJs: dynamic = js("function (a, b) { return Math.min(a, b); }")
private fun clmMathMin(a: dynamic, b: dynamic): dynamic = clmMathMinJs(a, b)

private val clmGetMinecraftVersionJs: dynamic = js("function () { return getMinecraftVersion(); }")
private fun clmGetMinecraftVersion(): dynamic = clmGetMinecraftVersionJs()

private val clmEvalExprJs: dynamic = js("function (c) { return eval('(' + c + ')'); }")
private fun clmEvalExpr(code: dynamic): dynamic = clmEvalExprJs(code)

private val clmNormalizeSourceUrlJs: dynamic = js(
    "function (url) {" +
        " url = String(url || '').trim();" +
        " if (!url) return '';" +
        " if (!/^https?:\\/\\//i.test(url)) url = 'https://' + url;" +
        " return url.replace(/\\/+$/, '') + '/';" +
        "}"
)
private fun clmNormalizeSourceUrl(url: dynamic): dynamic = clmNormalizeSourceUrlJs(url)

private val clmRegexUnsignedJs: dynamic = js("function (v) { return /^\\d+$/.test(v); }")
private fun clmRegexUnsigned(v: dynamic): Boolean = clmRegexUnsignedJs(v)

private val clmDecodeUriJs: dynamic = js("function (v) { return decodeURIComponent(String(v)); }")
private fun clmDecodeUri(v: dynamic): dynamic = clmDecodeUriJs(v)

private val clmCheckErrorToStringJs: dynamic = js(
    "function () { return this.stack.join('->') + this.message; }"
)
private val clmStatToStringJs: dynamic = js(
    "function () {" +
        " return ['命令数:', this.command, '\\n枚举数:', this.enums," +
        " '\\n选择器数:', this.selector, '\\n版本包数:', this.availablePack," +
        " '/', this.versionPack, '\\n命令模式数:', this.pattern," +
        " '\\n枚举项目数:', this.enumitem].join('');" +
        "}"
)

private val clmUriPathJs: dynamic = js(
    "function (u) { return String(u.getPath ? u.getPath() : u); }"
)
private fun clmUriPath(uri: dynamic): dynamic = clmUriPathJs(uri)

private val clmSetLengthJs: dynamic = js("function (o, n) { o.length = n; }")
private fun clmSetLength(o: dynamic, n: dynamic) {
    clmSetLengthJs(o, n)
}

private fun clmObject(): dynamic = js("({})")
private fun clmArray(): dynamic = js("[]")
private fun clmNull(): dynamic = js("null")
private fun clmUndefined(): dynamic = js("undefined")

private fun clmArrayOf(vararg items: dynamic): dynamic {
    val a = clmArray()
    var i = 0
    while (i < items.size) {
        a.push(items[i])
        i++
    }
    return a
}

private fun clmThrow(v: dynamic) {
    js("function (m) { throw m; }")(v)
}

// ---------------- Java / Android 互操作 helper ----------------

private val clmNewFile1Js: dynamic = js("function (p) { return new java.io.File(p); }")
private fun clmNewFile1(p: dynamic): dynamic = clmNewFile1Js(p)

private val clmNewFile2Js: dynamic = js("function (p, c) { return new java.io.File(p, c); }")
private fun clmNewFile2(p: dynamic, c: dynamic): dynamic = clmNewFile2Js(p, c)

private val clmNewBaseLibsDirJs: dynamic = js(
    "function () { return new java.io.File(MapScript.baseDir, 'libs'); }"
)
private fun clmNewBaseLibsDir(): dynamic = clmNewBaseLibsDirJs()

private val clmNewBaseLibsPathJs: dynamic = js(
    "function () { return new java.io.File(MapScript.baseDir + 'libs'); }"
)
private fun clmNewBaseLibsPath(): dynamic = clmNewBaseLibsPathJs()

private val clmGetExternalFilesDirJs: dynamic = js(
    "function () { return ctx.getExternalFilesDir('libs'); }"
)
private fun clmGetExternalFilesDir(): dynamic = clmGetExternalFilesDirJs()

private val clmNewByteArrayOutputStreamJs: dynamic = js(
    "function () { return new java.io.ByteArrayOutputStream(); }"
)
private fun clmNewByteArrayOutputStream(): dynamic = clmNewByteArrayOutputStreamJs()

private val clmNewByteArrayJs: dynamic = js(
    "function (n) { return java.lang.reflect.Array.newInstance(java.lang.Byte.TYPE, n); }"
)
private fun clmNewByteArray(n: dynamic): dynamic = clmNewByteArrayJs(n)

private val clmNewString1Js: dynamic = js("function (v) { return new java.lang.String(v); }")
private fun clmNewString1(v: dynamic): dynamic = clmNewString1Js(v)

private val clmNewString2Js: dynamic = js("function (b, e) { return new java.lang.String(b, e); }")
private fun clmNewString2(b: dynamic, e: dynamic): dynamic = clmNewString2Js(b, e)

private val clmStringBytesUtf8Js: dynamic = js(
    "function (s) { return new java.lang.String(s).getBytes('UTF-8'); }"
)
private fun clmStringBytesUtf8(s: dynamic): dynamic = clmStringBytesUtf8Js(s)

private val clmStringBytesJs: dynamic = js(
    "function (s) { return new java.lang.String(s).getBytes(); }"
)
private fun clmStringBytes(s: dynamic): dynamic = clmStringBytesJs(s)

private val clmNewGzipReaderJs: dynamic = js(
    "function (s) {" +
        " return new java.io.BufferedReader(new java.io.InputStreamReader(" +
        " new java.util.zip.GZIPInputStream(s)));" +
        "}"
)
private fun clmNewGzipReader(s: dynamic): dynamic = clmNewGzipReaderJs(s)

private val clmNewGzipOutputStreamJs: dynamic = js(
    "function (s) { return new java.util.zip.GZIPOutputStream(s); }"
)
private fun clmNewGzipOutputStream(s: dynamic): dynamic = clmNewGzipOutputStreamJs(s)

private val clmNewFileInputStreamJs: dynamic = js(
    "function (f) { return new java.io.FileInputStream(f); }"
)
private fun clmNewFileInputStream(f: dynamic): dynamic = clmNewFileInputStreamJs(f)

private val clmNewFileOutputStreamJs: dynamic = js(
    "function (f) { return new java.io.FileOutputStream(f); }"
)
private fun clmNewFileOutputStream(f: dynamic): dynamic = clmNewFileOutputStreamJs(f)

private val clmNewURLJs: dynamic = js("function (s) { return new java.net.URL(s); }")
private fun clmNewURL(s: dynamic): dynamic = clmNewURLJs(s)

private val clmNewMessageDigestJs: dynamic = js(
    "function (n) { return java.security.MessageDigest.getInstance(n); }"
)
private fun clmNewMessageDigest(n: dynamic): dynamic = clmNewMessageDigestJs(n)

private val clmNewSignatureJs: dynamic = js(
    "function (n) { return java.security.Signature.getInstance(n); }"
)
private fun clmNewSignature(n: dynamic): dynamic = clmNewSignatureJs(n)

private val clmNewKeyFactoryJs: dynamic = js(
    "function (n) { return java.security.KeyFactory.getInstance(n); }"
)
private fun clmNewKeyFactory(n: dynamic): dynamic = clmNewKeyFactoryJs(n)

private val clmNewX509EncodedKeySpecJs: dynamic = js(
    "function (b) { return new java.security.spec.X509EncodedKeySpec(b); }"
)
private fun clmNewX509EncodedKeySpec(b: dynamic): dynamic = clmNewX509EncodedKeySpecJs(b)

private val clmNewByteBufferAllocateJs: dynamic = js(
    "function (n) { return java.nio.ByteBuffer.allocate(n); }"
)
private fun clmNewByteBufferAllocate(n: dynamic): dynamic = clmNewByteBufferAllocateJs(n)

private val clmNewByteBufferWrapJs: dynamic = js(
    "function (a) { return java.nio.ByteBuffer.wrap(a); }"
)
private fun clmNewByteBufferWrap(a: dynamic): dynamic = clmNewByteBufferWrapJs(a)

private val clmByteBufferPutHeaderJs: dynamic = js(
    "function (ar) {" +
        " ar.put([0x4c, 0x49, 0x42, 0x52, 0x41, 0x52, 0x59])" +
        "  .putLong((new java.util.Date()).getTime());" +
        "}"
)
private fun clmByteBufferPutHeader(ar: dynamic) {
    clmByteBufferPutHeaderJs(ar)
}

private val clmBase64DecodeJs: dynamic = js(
    "function (s) { return android.util.Base64.decode(s, 2); }"
)
private fun clmBase64Decode(s: dynamic): dynamic = clmBase64DecodeJs(s)

private val clmBase64EncodeJs: dynamic = js(
    "function (b) { return android.util.Base64.encodeToString(b, android.util.Base64.NO_WRAP); }"
)
private fun clmBase64Encode(b: dynamic): dynamic = clmBase64EncodeJs(b)

private val clmGetVerifyKeyJs: dynamic = js(
    "function () { return ScriptInterface.getVerifyKey(); }"
)
private fun clmGetVerifyKey(): dynamic = clmGetVerifyKeyJs()

// ---------------- 公共对象构造 ----------------

/**
 * 构造与原 modules/CA.Library.js 等价的对象。
 *
 * 每次调用都会得到独立状态（inner / cache / sourceInfoCache 等），不会写全局 CA。
 */
fun buildCaLibraryModule(): dynamic {
    val libApi = js("({})")

    libApi.inner = clmObject()
    libApi.cache = clmObject()
    libApi.loadingStatus = clmNull()
    libApi.builtinLibs = "__BUILTIN_LIBS__"
    libApi.currentLoadingLibrary = clmNull()

    libApi.initLibrary = { callback: dynamic -> clmInitLibrary(libApi, callback) }
    libApi.loadBuiltinLibraries = { -> clmLoadBuiltinLibraries(libApi) }
    libApi.clearCache = { uriStr: dynamic -> clmClearCache(libApi, uriStr) }
    libApi.isLibrary = { uriStr: dynamic -> clmIsLibrary(libApi, uriStr) }
    libApi.isDeprecated = { uuid: dynamic, version: dynamic -> clmIsDeprecated(uuid, version) }
    libApi.enableLibrary = { uriStr: dynamic -> clmEnableLibrary(uriStr) }
    libApi.disableLibrary = { uriStr: dynamic -> clmDisableLibrary(uriStr) }
    libApi.removeLibrary = { uriStr: dynamic -> clmRemoveLibrary(libApi, uriStr) }
    libApi.enableCoreLibrary = { uriStr: dynamic -> clmEnableCoreLibrary(uriStr) }
    libApi.toVersionArray = { v: dynamic -> clmToVersionArray(v) }
    libApi.compareLibraryVersion = { a: dynamic, b: dynamic ->
        clmCompareLibraryVersion(libApi, a, b)
    }
    libApi.dedupeLibraries = { -> clmDedupeLibraries(libApi) }
    libApi.addLibrary = { uriStr: dynamic -> clmAddLibrary(libApi, uriStr) }
    libApi.summarizeAddResults = { results: dynamic -> clmSummarizeAddResults(results) }
    libApi.orderLibraries = { -> clmOrderLibraries(libApi) }
    libApi.loadLibrary = { uriStr: dynamic, targetLib: dynamic ->
        clmLoadLibrary(libApi, uriStr, targetLib)
    }
    libApi.readLibrary = { uriStr: dynamic -> clmReadLibrary(libApi, uriStr) }
    libApi.installerNameKey = { uriStr: dynamic -> clmInstallerNameKey(libApi, uriStr) }
    libApi.getInstallerRecord = { uriStr: dynamic -> clmGetInstallerRecord(libApi, uriStr) }
    libApi.markInstallerExecuted = { uriStr: dynamic, data: dynamic ->
        clmMarkInstallerExecuted(libApi, uriStr, data)
    }
    libApi.evalLib = { uri: dynamic, code: dynamic -> clmEvalLib(libApi, uri, code) }
    libApi.safeEval = { uri: dynamic, code: dynamic, defaultValue: dynamic, error: dynamic ->
        clmSafeEval(libApi, uri, code, defaultValue, error)
    }
    libApi.testSecurityLevel = { uri: dynamic -> clmTestSecurityLevel(libApi, uri) }
    libApi.resolveLibrary = { cur: dynamic, l: dynamic -> clmResolveLibrary(libApi, cur, l) }
    libApi.onLibraryLoadFinished = { lib: dynamic -> clmOnLibraryLoadFinished(libApi, lib) }
    libApi.processDeprecated = { -> clmProcessDeprecated(libApi) }
    libApi.findByUUID = { uuid: dynamic -> clmFindByUUID(libApi, uuid) }
    libApi.isPrefixed = { uri: dynamic -> clmIsPrefixed(libApi, uri) }
    libApi.loadPrefixed = { uri: dynamic, defaultValue: dynamic, error: dynamic ->
        clmLoadPrefixed(libApi, uri, defaultValue, error)
    }
    libApi.savePrefixed = { uri: dynamic, obj: dynamic -> clmSavePrefixed(uri, obj) }
    libApi.checkLibrary = { a: dynamic -> clmCheckLibrary(a) }
    libApi.checkPackVer = { o: dynamic -> clmCheckPackVer(libApi, o) }
    libApi.joinPack = { cur: dynamic, l: dynamic -> clmJoinPack(libApi, cur, l) }
    libApi.statLib = { l: dynamic -> clmStatLib(l) }
    libApi.sourceInfoCache = clmObject()
    libApi.requestDefaultSourceInfo = { -> clmRequestDefaultSourceInfo(libApi) }
    libApi.requestSourceInfoCached = { url: dynamic ->
        clmRequestSourceInfoCached(libApi, url)
    }
    libApi.requestSourceInfo = { url: dynamic -> clmRequestSourceInfo(url) }
    libApi.requestSourceIndex = { info: dynamic, pageNo: dynamic ->
        clmRequestSourceIndex(info, pageNo)
    }
    libApi.requestSourceMap = { info: dynamic -> clmRequestSourceMap(info) }
    libApi.getSourceUrl = { -> clmGetSourceUrl(libApi) }
    libApi.getOriginSourceUrl = { -> clmGetOriginSourceUrl() }
    libApi.getSources = { -> clmGetSources(libApi) }
    libApi.normalizeSourceUrl = { url: dynamic -> clmNormalizeSourceUrl(url) }
    libApi.findSource = { url: dynamic -> clmFindSource(libApi, url) }
    libApi.enabledSources = { -> clmEnabledSources(libApi) }
    libApi.addSource = { url: dynamic -> clmAddSource(libApi, url) }
    libApi.removeSource = { url: dynamic -> clmRemoveSource(libApi, url) }
    libApi.setSourceEnabled = { url: dynamic, enabled: dynamic ->
        clmSetSourceEnabled(libApi, url, enabled)
    }
    libApi.saveState = { -> clmSaveState() }
    libApi.saveSources = { -> clmSaveSources() }
    libApi.setSourceNote = { url: dynamic, note: dynamic -> clmSetSourceNote(libApi, url, note) }
    libApi.getVerify = { source: dynamic -> clmGetVerify(libApi, source) }
    libApi.getLibraryRoot = { -> clmGetLibraryRoot() }
    libApi.getLibraryExternalRoot = { -> clmGetLibraryExternalRoot() }
    libApi.arrayStartsWith = { array: dynamic, start: dynamic -> clmArrayStartsWith(array, start) }
    libApi.readAsArray = { stream: dynamic, keep: dynamic -> clmReadAsArray(stream, keep) }
    libApi.downloadAsArray = { url: dynamic -> clmDownloadAsArray(url) }
    libApi.downloadLib = { libinfo: dynamic, source: dynamic ->
        clmDownloadLib(libApi, libinfo, source)
    }
    libApi.shouldVerifySigned = { uri: dynamic -> clmShouldVerifySigned(libApi, uri) }
    libApi.loadSignedV1 = { uri: dynamic, defaultValue: dynamic, error: dynamic ->
        clmLoadSignedV1(libApi, uri, defaultValue, error)
    }
    libApi.getOfficialVerify = { -> clmGetOfficialVerify(libApi) }
    libApi.bytesEqual = { a: dynamic, b: dynamic -> clmBytesEqual(a, b) }
    libApi.warnLowSecurity = { -> clmWarnLowSecurity(libApi) }
    libApi.isCommunityLib = { uri: dynamic -> clmIsCommunityLib(uri) }
    libApi.tryReleaseLibraryFile = { uri: dynamic -> clmTryReleaseLibraryFile(libApi, uri) }
    libApi.cleanLibrary = { -> clmCleanLibrary() }
    libApi.requestUpdateUrlFromDefSrc = { uuid: dynamic ->
        clmRequestUpdateUrlFromDefSrc(libApi, uuid)
    }
    libApi.requestUpdateInfo = { libinfo: dynamic, callback: dynamic ->
        clmRequestUpdateInfo(libApi, libinfo, callback)
    }
    libApi.doUpdate = { updateInfo: dynamic, libInfo: dynamic, statusListener: dynamic ->
        clmDoUpdate(libApi, updateInfo, libInfo, statusListener)
    }
    libApi.updateLibraries = { level: dynamic -> clmUpdateLibraries(libApi, level) }
    libApi.versionToString = { v: dynamic -> clmVersionToString(v) }

    return libApi
}

// ---------------- init / 内置库 / 缓存 ----------------

private fun clmInitLibrary(libApi: dynamic, callback: dynamic): dynamic {
    var flag = true
    if (clmTruthy(libApi.loadingStatus)) return false
    libApi.loadingStatus = "core"
    clmLoadBuiltinLibraries(libApi)

    val info = clmArray()
    val lib = clmObject()
    lib.commands = clmObject()
    lib.enums = clmObject()
    lib.selectors = clmObject()
    lib.json = clmObject()
    lib.jsonSchema = clmObject()
    lib.states = clmObject()
    lib.help = clmObject()
    lib.tutorials = clmArray()
    lib.idlist = clmArray()
    lib.info = info
    js("CA").IntelliSense.library = lib
    // 库对象换了，ID 列表构建缓存随之失效；否则增删拓展包后可能读到旧库的列表
    js("CA")._idListBuildCache = clmArray()
    js("CA")._idAllIndices = clmNull()
    clmProcessDeprecated(libApi)
    // 同一 uuid 只保留最高版本；低版本移除（内置的改为禁用）
    val removed = clmDedupeLibraries(libApi)
    if (clmGt(removed, 0)) {
        js("Common").toast(
            clmAdd("有更高版本的拓展包，已自动移除 ", clmAdd(removed, " 个低版本拓展包"))
        )
    }
    // 前提包排在依赖它的拓展包前面，避免顺序导致「前提包并未全部加载」的报错
    clmOrderLibraries(libApi)

    js("CA").settings.coreLibrarys.forEach({ e: dynamic, i: dynamic, _a: dynamic ->
        libApi.currentLoadingLibrary = e
        val data = libApi.loadLibrary(clmString(e), clmNull())
        data.core = true
        data.index = i
        if (clmTruthy(data.hasError)) flag = false
        info.push(data)
        js("undefined")
    })

    libApi.loadingStatus = "normal"
    js("Threads").run({
        try {
            js("CA").settings.enabledLibrarys.forEach({ e: dynamic, i: dynamic, _a: dynamic ->
                libApi.currentLoadingLibrary = e
                val data = libApi.loadLibrary(clmString(e), lib)
                data.index = i
                if (clmTruthy(data.hasError)) flag = false
                info.push(data)
                js("undefined")
            })
            //快捷操作
            libApi.onLibraryLoadFinished(lib)
            libApi.loadingStatus = clmNull()
            if (clmTruthy(callback)) callback(flag)
        } catch (e: dynamic) {
            js("erp")(e)
        }
        js("undefined")
    })
    return true
}

private fun clmLoadBuiltinLibraries(libApi: dynamic): dynamic {
    val libs = libApi.builtinLibs
    // 清理已经不存在的内置库（比如从构建里移除的），否则每次启动都会报"文件不存在"。
    // 必须放在下面的提前返回之前：内置库被全部移除时 libs 是空数组，
    // 提前返回会导致这里永远不执行，残留的 builtin:// URI 就一直报错。
    clmArrayOf(js("CA").settings.enabledLibrarys, js("CA").settings.coreLibrarys).forEach(
        { list: dynamic ->
            var k = clmLength(list) - 1
            while (k >= 0) {
                if (clmLooseEq(clmString(list[k]).indexOf("builtin://"), 0) &&
                    !clmTruthy(libApi.inner[list[k]])
                ) {
                    list.splice(k, 1)
                }
                k--
            }
            js("undefined")
        }
    )
    // 若构建时没有注入内置命令库（例如旧脚本），这里仍是占位符字符串，
    // 字符串没有 forEach，必须直接跳过，否则初始化命令库时会抛 TypeError
    if (!clmTruthy(libs) || clmLength(libs) == 0 || !clmIsFunction(libs.forEach)) {
        return clmUndefined()
    }
    libs.forEach({ lib: dynamic ->
        val uri = clmAdd("builtin://", lib.path)
        if (clmTruthy(libApi.inner[uri])) {
            libApi.enableLibrary(uri)
        } else {
            try {
                val code = clmNewString2(clmBase64Decode(lib.source), "UTF-8")
                val obj = clmEvalExpr(code)
                if (!clmTruthy(obj) || clmStrictNe(clmTypeof(obj), "object")) {
                    clmThrow("无效的拓展包对象")
                }
                libApi.inner[uri] = obj
                libApi.enableLibrary(uri)
            } catch (e: dynamic) {
                js("erp")(e)
            }
        }
        js("undefined")
    })
    return clmUndefined()
}

private fun clmClearCache(libApi: dynamic, uriStr: dynamic): dynamic {
    if (clmTruthy(uriStr)) {
        clmDelete(libApi.cache, uriStr)
    } else {
        libApi.cache = clmObject()
    }
    return clmUndefined()
}

private fun clmIsLibrary(libApi: dynamic, uriStr: dynamic): dynamic {
    if (clmIn(uriStr, libApi.inner)) return true
    return js("ExternalStorage").canRead(js("ExternalStorage").toUri(uriStr))
}

private fun clmIsDeprecated(uuid: dynamic, version: dynamic): dynamic {
    if (!clmIsArray(version)) return true
    if (clmLooseEq(uuid, "04a9e9b2-8fae-4f30-84fa-d52f9457f4eb")) return true //自适配ID表：用户瞎加载
    if (clmLooseEq(uuid, "06b2fb31-668e-4693-92ad-c0ac8da3e7a9") &&
        clmLt(js("NeteaseAdapter").compareVersion(version, clmArrayOf(2, 0, 0)), 0)
    ) {
        return true //MC图标：bug
    }
    if (clmLooseEq(uuid, "5a204d07-4b6d-4c51-9470-a2d8c8676ab8")) return true //调试屏幕：根本没用
    return false
}

private fun clmEnableLibrary(uriStr: dynamic): dynamic {
    js("Common").removeSet(js("CA").settings.disabledLibrarys, uriStr)
    js("Common").removeSet(js("CA").settings.coreLibrarys, uriStr)
    return js("Common").addSet(js("CA").settings.enabledLibrarys, uriStr)
}

private fun clmDisableLibrary(uriStr: dynamic): dynamic {
    js("Common").removeSet(js("CA").settings.enabledLibrarys, uriStr)
    js("Common").removeSet(js("CA").settings.coreLibrarys, uriStr)
    return js("Common").addSet(js("CA").settings.disabledLibrarys, uriStr)
}

private fun clmRemoveLibrary(libApi: dynamic, uriStr: dynamic): dynamic {
    val uri = js("ExternalStorage").toUri(uriStr)
    var fl: dynamic = false
    js("ExternalStorage").tryReleaseImportUri(uri)
    clmTryReleaseLibraryFile(libApi, uri)
    fl = clmOr(js("Common").removeSet(js("CA").settings.enabledLibrarys, uriStr), fl)
    fl = clmOr(js("Common").removeSet(js("CA").settings.coreLibrarys, uriStr), fl)
    return clmOr(js("Common").removeSet(js("CA").settings.disabledLibrarys, uriStr), fl)
    // 原有 bug：上面已经 return，下面清理安装器记录的代码永远不会执行。
    // 为保持原行为，这里只保留注释，不执行清理：
    //   if (CA.settings.installedInstallers) {
    //     delete CA.settings.installedInstallers[String(uriStr)];
    //     delete CA.settings.installedInstallers[this.installerNameKey(uriStr)];
    //     try { CA.trySave(); } catch (e) { }
    //   }
}

private fun clmEnableCoreLibrary(uriStr: dynamic): dynamic {
    js("Common").removeSet(js("CA").settings.enabledLibrarys, uriStr)
    js("Common").removeSet(js("CA").settings.disabledLibrarys, uriStr)
    return js("Common").addSet(js("CA").settings.coreLibrarys, uriStr)
}

/** 版本归一化成数组（compareVersion 依赖数组下标） */
private fun clmToVersionArray(v: dynamic): dynamic = if (clmIsArray(v)) v else clmArrayOf(v)

/** 比较两个拓展包版本（数组或字符串都行）：a > b 返回 1，相等 0，a < b 返回 -1 */
private fun clmCompareLibraryVersion(libApi: dynamic, a: dynamic, b: dynamic): dynamic =
    js("NeteaseAdapter").compareVersion(clmToVersionArray(a), clmToVersionArray(b))

/**
 * 同一 uuid 只保留最高版本：低版本从列表里移除（内置包删不掉文件，改为禁用）。
 * 在 initLibrary 里、真正加载之前调用；返回被移除的个数。
 */
private fun clmDedupeLibraries(libApi: dynamic): dynamic {
    val best = clmObject()
    val dropped = clmArray()
    val lists = clmArrayOf(js("CA").settings.coreLibrarys, js("CA").settings.enabledLibrarys)
    lists.forEach({ list: dynamic ->
        list.slice().forEach({ uriStr: dynamic ->
            var rd: dynamic = clmUndefined()
            try {
                rd = libApi.readLibrary(clmString(uriStr))
            } catch (e: dynamic) {
                rd = clmUndefined() // 读不出来的交给正常加载流程去报错
            }
            if (clmTruthy(rd) && !clmTruthy(rd.error) && clmTruthy(rd.data) &&
                clmTruthy(rd.data.uuid)
            ) {
                val info = clmObject()
                info.uri = clmString(uriStr)
                info.uuid = rd.data.uuid
                info.version = clmToVersionArray(rd.data.version)
                info.name = rd.data.name
                val t = best[info.uuid]
                if (!clmTruthy(t)) {
                    best[info.uuid] = info
                } else if (clmGe(libApi.compareLibraryVersion(info.version, t.version), 0)) {
                    // 同版本时保留后面加载的那份（"加载最后的相同版本"）
                    dropped.push(t)
                    best[info.uuid] = info
                } else {
                    dropped.push(info)
                }
            }
            js("undefined")
        })
        js("undefined")
    })
    dropped.forEach({ info: dynamic ->
        // 内置拓展包（inner 里的 / builtin:// 形式的）没法从仓库删掉，放进禁用列表
        if (clmLooseEq(clmString(info.uri).indexOf("builtin://"), 0) ||
            clmTruthy(libApi.inner[info.uri])
        ) {
            libApi.disableLibrary(info.uri)
        } else {
            libApi.removeLibrary(info.uri)
        }
        js("undefined")
    })
    return clmLength(dropped)
}

/**
 * 导入/加载一个外置拓展包（替代直接 enableLibrary），返回状态字符串：
 *   "added"   新增并启用
 *   "updated" 版本更高：移除低版本（内置的改为禁用）后启用
 *   "ignored" 已有版本更高或相同：忽略本次导入，并删掉导入进来的文件
 *   "enabled" 读不出元数据时按原行为启用（后续加载会报具体错误）
 * 这里不弹提示：批量导入时由调用方汇总数量，只提示包的数量。
 */
private fun clmAddLibrary(libApi: dynamic, uriStr: dynamic): dynamic {
    val uri = clmString(uriStr)
    // 手动导入 = 重新安装：先清掉旧记录，让这一份真的执行一次
    // （启动时加载则相反，靠记录跳过，不再执行）
    if (clmTruthy(js("CA").settings.installedInstallers)) {
        clmDelete(js("CA").settings.installedInstallers, uri)
        clmDelete(js("CA").settings.installedInstallers, libApi.installerNameKey(uri))
    }
    var rd: dynamic = clmNull()
    try {
        rd = libApi.readLibrary(uri)
    } catch (e: dynamic) {
        rd = clmNull()
    }
    // 安装器包：加载/卸载本身就会触发一次重新加载，别让它连带跑一轮全量更新检查
    if (clmTruthy(rd) && clmTruthy(rd.data) && clmTruthy(rd.data.installer)) {
        libApi.installerDirty = true
    }
    if (!clmTruthy(rd) || clmTruthy(rd.error) || !clmTruthy(rd.data)) {
        libApi.enableLibrary(uriStr)
        libApi.saveState()
        return "enabled"
    }
    val data = rd.data
    val existing = libApi.findByUUID(data.uuid)
    if (!clmTruthy(existing)) {
        libApi.enableLibrary(uriStr)
        libApi.saveState()
        return "added"
    }
    if (clmLooseEq(uri, clmString(existing.src))) {
        // 同一个文件，不用处理
        return "ignored"
    }
    // 原文件计算了 cmp 但从未使用；保留计算与注释，避免误以为这里有版本判断。
    val cmp = libApi.compareLibraryVersion(data.version, existing.version)
    // cmp > 0：升级；== 0：同版本重装；< 0：降级。都允许覆盖安装
    if (clmTruthy(existing.src) &&
        (clmLooseEq(clmString(existing.src).indexOf("builtin://"), 0) ||
            clmTruthy(libApi.inner[clmString(existing.src)]))
    ) {
        libApi.disableLibrary(clmString(existing.src)) // 内置包不能删，保持禁用
    } else if (clmTruthy(existing.src)) {
        libApi.removeLibrary(clmString(existing.src)) // 低版本直接移除
    }
    libApi.enableLibrary(uriStr)
    libApi.saveState()
    return "updated"
}

/** 把一批 addLibrary 的返回值汇总成一句提示（只报数量） */
private fun clmSummarizeAddResults(results: dynamic): dynamic {
    var added = 0
    var updated = 0
    var ignored = 0
    results.forEach({ r: dynamic ->
        if (clmLooseEq(r, "updated")) {
            updated++
        } else if (clmLooseEq(r, "ignored")) {
            ignored++
        } else {
            added++
        }
        js("undefined")
    })
    val parts = clmArray()
    if (updated != 0) parts.push(clmAdd("更新 ", clmAdd(updated, " 个")))
    if (added != 0) parts.push(clmAdd("新增 ", clmAdd(added, " 个")))
    if (ignored != 0) parts.push(clmAdd("忽略 ", clmAdd(ignored, " 个")))
    return if (clmLength(parts) > 0) {
        clmAdd("拓展包：", parts.join("，"))
    } else {
        "没有可导入的拓展包"
    }
}

/**
 * 按前提包（require）给「已启用」列表排序：前提包排在依赖它的拓展包前面。
 * 读取走 loadLibrary 的缓存，不会重复 eval；循环依赖时也能正常结束（保持可加载的相对顺序）。
 * 返回是否调整过顺序。
 */
private fun clmOrderLibraries(libApi: dynamic): dynamic {
    val list = js("CA").settings.enabledLibrarys
    val meta = clmObject()
    val byUuid = clmObject()
    val order = clmArray()
    val visiting = clmObject()
    list.slice().forEach({ uri: dynamic ->
        var rd: dynamic = clmUndefined()
        try {
            rd = libApi.readLibrary(clmString(uri))
        } catch (e: dynamic) {
            rd = clmUndefined()
        }
        if (clmTruthy(rd) && !clmTruthy(rd.error) && clmTruthy(rd.data)) {
            val key = clmString(uri)
            val m = clmObject()
            m.uuid = rd.data.uuid
            m.require = if (clmIsArray(rd.data.require)) rd.data.require else clmArray()
            meta[key] = m
            if (clmTruthy(rd.data.uuid)) byUuid[rd.data.uuid] = key
        }
        js("undefined")
    })
    list.slice().forEach({ uri: dynamic ->
        clmOrderVisit(uri, meta, byUuid, order, visiting)
        js("undefined")
    })
    // 读不出元数据的库照样保留（放最后，交给加载流程报错）
    list.slice().forEach({ uri: dynamic ->
        if (clmLt(order.indexOf(uri), 0)) order.push(uri)
        js("undefined")
    })
    var changed = false
    var oi = 0
    while (oi < clmLength(order)) {
        if (clmLooseNe(order[oi], list[oi])) {
            changed = true
            break
        }
        oi++
    }
    if (changed) {
        list.splice(0, clmLength(list))
        order.forEach({ uri: dynamic ->
            list.push(uri)
            js("undefined")
        })
    }
    return changed
}

private fun clmOrderVisit(
    uri: dynamic, meta: dynamic, byUuid: dynamic, order: dynamic, visiting: dynamic
) {
    if (clmGe(order.indexOf(uri), 0)) return
    if (clmTruthy(visiting[uri])) return // 循环依赖：不重复进栈
    visiting[uri] = true
    val m = meta[uri]
    val req = if (clmTruthy(m)) m.require else clmArray()
    var j = 0
    while (j < clmLength(req)) {
        val r = req[j]
        val ruuid = if (clmStrictEq(clmTypeof(r), "string")) {
            r
        } else if (clmTruthy(r)) {
            r.uuid
        } else {
            r
        }
        val ruri = if (clmTruthy(ruuid)) byUuid[ruuid] else ruuid
        // 前提包也在启用列表里：先放它
        if (clmTruthy(ruri) && clmLooseNe(ruri, uri)) {
            clmOrderVisit(ruri, meta, byUuid, order, visiting)
        }
        j++
    }
    visiting[uri] = false
    if (clmLt(order.indexOf(uri), 0)) order.push(uri)
}

private fun clmLoadLibrary(
    libApi: dynamic, uriStr: dynamic, targetLib: dynamic
): dynamic {
    var m: dynamic = clmUndefined()
    var community: dynamic = clmUndefined()
    var cur: dynamic = clmUndefined()
    var resolved: dynamic = clmUndefined()
    try {
        if (clmTruthy(libApi.cache[uriStr])) {
            cur = libApi.cache[uriStr].data
            m = libApi.cache[uriStr].mode
            community = libApi.cache[uriStr].community
        } else {
            cur = libApi.readLibrary(uriStr)
            if (!clmTruthy(cur)) clmThrow("无法读取或解析拓展包")
            if (clmTruthy(cur.error)) clmThrow(cur.error)
            if (!clmInstanceOf(cur.data, js("Object"))) clmThrow("错误的拓展包格式")
            libApi.cache[uriStr] = cur
            m = cur.mode
            community = cur.community
            cur = cur.data
        }
        resolved = clmObject()
        resolved.src = uriStr
        resolved.name = cur.name
        resolved.author = cur.author
        resolved.description = cur.description
        resolved.uuid = cur.uuid
        resolved.version = if (clmIsArray(cur.version)) cur.version else clmArrayOf(cur.version)
        resolved.update = cur.update
        resolved.menu = cur.menu
        resolved.deprecated = if (clmTruthy(cur.deprecated)) {
            cur.deprecated
        } else {
            libApi.isDeprecated(cur.uuid, cur.version)
        }
        resolved.mode = m
        resolved.community = clmStrictEq(community, true)
        // 安装器包（安装完会把自己删掉的特殊包）：进列表，但不参与更新检查
        resolved.installer = clmStrictEq(cur.installer, true)
        resolved.stat = if (!clmTruthy(cur.noCommand) && clmTruthy(targetLib)) {
            libApi.resolveLibrary(targetLib, cur)
        } else {
            clmNull()
        }
        resolved.loaded = true
        return resolved
    } catch (err: dynamic) {
        if (clmTruthy(resolved)) {
            resolved.hasError = true
            resolved.error = err
            return resolved
        } else {
            // 原有 bug：若在 readLibrary 之前就抛错，m 仍是 undefined；
            // 这里保留 m 的初始 undefined，回退行为的怪异之处与原文件一致。
            val r = clmObject()
            r.src = uriStr
            r.name = if (clmLooseEq(m, 0)) {
                uriStr
            } else {
                js("ExternalStorage").uriToName(js("ExternalStorage").toUri(uriStr))
            }
            r.hasError = true
            r.mode = m
            r.error = err
            return r
        }
    }
}

private fun clmReadLibrary(libApi: dynamic, uriStr: dynamic): dynamic {
    var t: dynamic
    var er: dynamic
    var uri: dynamic
    var done: dynamic
    val securityLevel = js("CA").settings.securityLevel
    var requiredSecLevel: dynamic
    // 安全级别（数字越大越严格）：
    //   0 完全加载，不使用安全机制：什么都不检查
    //   1 不允许开放拓展包：放行未签名的锁定包，但提醒一次
    //   2 中  ：只允许已签名的（官方 + 社区）
    //   3 高  ：只允许官方签名的
    //   4 严格：只加载内置
    t = libApi.inner[uriStr]
    if (clmTruthy(t)) {
        val r = clmObject()
        r.data = t
        r.mode = 0
        return r
    }
    er = clmObject()
    er.error = "未知错误"
    if (clmGe(securityLevel, 4)) {
        val r = clmObject()
        r.error = "当前安全级别为「严格」，只加载内置命令库\n可在「管理拓展包」右上角 ▼ 的「设置安全级别」里调整"
        return r
    }
    uri = js("ExternalStorage").toUri(uriStr)
    js("ExternalStorage").tryTakeUriPermission(uri)
    if (!clmTruthy(js("ExternalStorage").isFile(uri))) {
        val r = clmObject()
        r.error = "拓展包文件不存在"
        return r
    }
    requiredSecLevel = libApi.testSecurityLevel(uri) // 2=签名 1=LIBRARY 0=裸JS
    // 按级别决定"最低要求的包类型"：0 不限，1 要 LIBRARY 以上，2 及以上要签名
    val minRequired: dynamic =
        if (clmLe(securityLevel, 0)) 0 else (if (clmLooseEq(securityLevel, 1)) 1 else 2)
    if (clmLt(requiredSecLevel, minRequired)) {
        val r = clmObject()
        r.error = clmAdd(
            "当前安全级别不允许加载",
            clmAdd(
                if (clmGe(minRequired, 2)) "未签名的" else "未打包的",
                "拓展包\n可在「管理拓展包」右上角 ▼ 的「设置安全级别」里调整"
            )
        )
        return r
    }
    // 高：只认官方签名，社区包也拒绝
    if (clmGe(securityLevel, 3) && clmTruthy(libApi.isCommunityLib(uri))) {
        val r = clmObject()
        r.error = "当前安全级别为「高」，不允许加载社区拓展包\n它由第三方拓展包源签名，没有官方背书；切到「中」可以加载"
        return r
    }
    // 低：允许 LIBRARY 未签名包，但要提醒一次
    if (clmLooseEq(securityLevel, 1) && clmLt(requiredSecLevel, 2)) {
        libApi.warnLowSecurity()
    }
    if (clmGe(requiredSecLevel, 2)) {
        t = libApi.loadSignedV1(uri, clmNull(), er)
        if (clmTruthy(t)) {
            val r = clmObject()
            r.data = t
            r.mode = 3
            r.community = libApi.isCommunityLib(uri)
            return r
        }
    } else if (clmLooseEq(requiredSecLevel, 1)) {
        t = libApi.loadPrefixed(uri, clmNull(), er)
        if (clmTruthy(t)) {
            val r = clmObject()
            r.data = t
            r.mode = 2
            return r
        }
    } else if (clmLooseEq(requiredSecLevel, 0)) {
        t = js("ExternalStorage").readFileContent(uri, "UTF-8", { error: dynamic ->
            val eo = clmObject()
            eo.error = error
            er = eo
            js("undefined")
        })
        if (clmTruthy(t)) {
            // 安装器包执行过一次后只保留元数据：进列表，但不重新执行。
            // 否则每次启动都会重写脚本、重建快捷方式、重复弹窗。
            done = libApi.getInstallerRecord(uriStr)
            if (clmTruthy(done)) {
                val r = clmObject()
                r.data = done
                r.mode = 1
                return r
            }
            t = libApi.safeEval(uri, t, er)
            if (clmTruthy(t)) {
                val r = clmObject()
                r.data = t
                r.mode = 1
                return r
            }
        }
    } else {
        val r = clmObject()
        r.error = "无法解析此命令库"
        return r
    }
    return er
}

// ---------------- 安装器记录 / eval ----------------

/** 安装器副本的文件名（重复导入会复制到不同目录，文件名不变） */
private fun clmInstallerNameKey(libApi: dynamic, uriStr: dynamic): dynamic {
    var name: dynamic
    try {
        name = js("ExternalStorage").uriToName(js("ExternalStorage").toUri(clmString(uriStr)))
    } catch (e: dynamic) {
        name = clmString(uriStr).split("/").pop()
    }
    try {
        name = clmDecodeUri(clmString(name))
    } catch (e: dynamic) {
    }
    return clmAdd("name:", name)
}

/** 查这个安装器是否已经执行过（先按 uri，再按文件名——同名副本只执行一次） */
private fun clmGetInstallerRecord(libApi: dynamic, uriStr: dynamic): dynamic {
    val m = js("CA").settings.installedInstallers
    if (!clmTruthy(m)) return clmNull()
    val first = m[clmString(uriStr)]
    val rec = if (clmTruthy(first)) {
        first
    } else {
        val second = m[libApi.installerNameKey(uriStr)]
        if (clmTruthy(second)) second else clmNull()
    }
    if (!clmTruthy(rec)) return clmNull()
    // 老版本存的记录可能缺字段：checkLibrary 会要求 require/version 是数组，
    // 读出来先补全，免得升级后报「require 不是数组」。
    if (!clmIsArray(rec.require)) rec.require = clmArray()
    if (!clmIsArray(rec.version)) rec.version = clmArrayOf(rec.version)
    if (!clmIsArray(rec.menu)) rec.menu = clmArray()
    rec.noCommand = true
    rec.installer = true
    return rec
}

/** 记下「这个安装器已经执行过」：只存元数据，之后进列表但不再执行 */
private fun clmMarkInstallerExecuted(
    libApi: dynamic, uriStr: dynamic, data: dynamic
): dynamic {
    if (!clmTruthy(js("CA").settings.installedInstallers)) {
        js("CA").settings.installedInstallers = clmObject()
    }
    // 必须补齐插件信息里 checkLibrary 会校验的字段（require/version 都要是数组），
    // 否则下次启动读到缓存记录时会报「require 不是数组」。
    val rec = clmObject()
    rec.name = data.name
    rec.author = data.author
    rec.description = data.description
    rec.uuid = data.uuid
    rec.version = if (clmIsArray(data.version)) data.version else clmArrayOf(data.version)
    rec.require = if (clmIsArray(data.require)) data.require else clmArray()
    rec.menu = clmArray()
    // 安装器不提供命令，标记成 noCommand：加载时不必再走 resolveLibrary
    rec.noCommand = true
    rec.installer = true
    rec.shortcutName = clmOr(data.shortcutName, "")
    js("CA").settings.installedInstallers[clmString(uriStr)] = rec
    // 同名副本（重复导入）也认这条记录，避免每份都执行一次、弹多次提示
    js("CA").settings.installedInstallers[libApi.installerNameKey(uriStr)] = rec
    libApi.installerDirty = true
    try {
        js("CA").trySave()
    } catch (e: dynamic) {
    }
    return clmUndefined()
}

private fun clmEvalLib(libApi: dynamic, uri: dynamic, code: dynamic): dynamic {
    val key = clmString(uri)
    // LIBRARY / 裸 JS 两条读取路径最后都走这里，安装器的「只执行一次」就放在这个总口上：
    //   已经执行过 → 直接返回元数据（进列表，但不重新写脚本、不重复弹窗）
    val done = libApi.getInstallerRecord(key)
    if (clmTruthy(done)) {
        return done
    }
    val scope = clmObject()
    scope.path = clmUriPath(uri)
    scope.code = code
    val libInfo = clmObject()
    libInfo.uri = uri
    libInfo.code = code
    scope.LibInfo = libInfo
    val t = js("Loader").evalSpecial(
        clmAdd("(", clmAdd(code, ")")),
        js("ExternalStorage").uriToName(uri),
        0,
        scope,
        libApi
    )
    if (clmTruthy(t) && clmStrictEq(t.installer, true)) {
        libApi.markInstallerExecuted(key, t)
    }
    return t
}

private fun clmSafeEval(
    libApi: dynamic, uri: dynamic, code: dynamic, defaultValue: dynamic, error: dynamic
): dynamic {
    try {
        return libApi.evalLib(uri, code)
    } catch (e: dynamic) {
        if (clmTruthy(error)) error.error = e
        return defaultValue
    }
}

private fun clmTestSecurityLevel(libApi: dynamic, uri: dynamic): dynamic {
    if (clmGe(libApi.shouldVerifySigned(uri), 0)) {
        return 2
    } else if (clmTruthy(libApi.isPrefixed(uri))) {
        return 1
    } else {
        return 0
    }
}

private fun clmResolveLibrary(libApi: dynamic, cur: dynamic, l: dynamic): dynamic {
    val libinfo = js("CA").IntelliSense.library.info
    var t = libApi.checkPackVer(l)
    if (clmLooseNe(t, 0)) {
        clmThrow(if (clmGt(t, 0)) "拓展包版本过低" else "游戏版本过低") //兼容旧版
    }
    if (clmTruthy(l.minCAVersion) &&
        clmLt(clmDateParse(js("CA").publishDate), clmDateParse(l.minCAVersion))
    ) {
        clmThrow("命令助手版本过低")
    }
    val stat = libApi.statLib(l)
    libApi.checkLibrary(l)
    if (clmTruthy(libApi.findByUUID(l.uuid))) clmThrow("已存在相同的拓展包")
    val missing = l.require.some({ e1: dynamic ->
        var requuid: dynamic = clmUndefined()
        var vertester: dynamic = clmUndefined()
        if (clmStrictEq(clmTypeof(e1), "string")) {
            requuid = e1
        } else {
            //扩展写法：{ uuid : "...", min : [...], max : [...] }，可限定前提包的版本区间
            requuid = e1.uuid
            vertester = { ver: dynamic ->
                if (clmTruthy(e1.min) &&
                    clmGt(js("NeteaseAdapter").compareVersion(e1.min, ver), 0)
                ) {
                    false
                } else if (clmTruthy(e1.max) &&
                    clmLt(js("NeteaseAdapter").compareVersion(e1.max, ver), 0)
                ) {
                    false
                } else {
                    true
                }
            }
        }
        !libinfo.some({ e2: dynamic ->
            if (clmTruthy(vertester)) {
                if (clmLooseEq(requuid, e2.uuid)) vertester(e2.version) else false
            } else {
                clmLooseEq(requuid, e2.uuid)
            }
        })
    })
    if (clmTruthy(missing)) {
        clmThrow("前提包并未全部加载或不满足版本条件，请检查加载顺序及拓展包列表")
    }
    libApi.joinPack(cur, clmObjectCopy(l)) //创建副本
    if (!clmTruthy(l.versionPack)) return clmUndefined()
    val c = l.versionPack
    clmForIn(c, { i: dynamic ->
        t = libApi.joinPack(cur, c[i]) //加载版本包
        if (clmTruthy(stat) && clmTruthy(t)) stat.availablePack = clmAdd(stat.availablePack, 1)
        js("undefined")
    })
    return stat
}

private fun clmOnLibraryLoadFinished(libApi: dynamic, lib: dynamic): dynamic {
    val t = lib.commands
    lib.command_snap = clmObject()
    clmObjectKeys(t).forEach({ e: dynamic ->
        var t2 = e
        while (clmTruthy(t[t2].alias)) t2 = t[t2].alias
        val target = t[t2]
        lib.command_snap[e] = clmOr(target.description, "")
        js("undefined")
    })
    js("Tutorial").library = lib.tutorials
    if (clmTruthy(libApi.installerDirty)) {
        // 安装器加载/卸载自己导致的重载：不触发更新检查，否则一次安装会连查两轮
        libApi.installerDirty = false
    } else {
        libApi.updateLibraries(js("CA").settings.libraryAutoUpdate)
    }
    lib.info.forEach({ e: dynamic ->
        if (clmTruthy(e.deprecated) && !clmTruthy(e.updateInfo)) {
            js("Common").addSet(js("CA").settings.deprecatedLibrarys, e.src)
        }
        js("undefined")
    })
    return clmUndefined()
}

private fun clmProcessDeprecated(libApi: dynamic): dynamic {
    js("CA").settings.deprecatedLibrarys.forEach({ e: dynamic ->
        libApi.disableLibrary(e)
        js("undefined")
    })
    clmSetLength(js("CA").settings.deprecatedLibrarys, 0)
    return clmUndefined()
}

private fun clmFindByUUID(libApi: dynamic, uuid: dynamic): dynamic {
    val a = js("CA").IntelliSense.library.info
    var i = 0
    while (i < clmLength(a)) {
        if (clmLooseEq(uuid, a[i].uuid)) return a[i]
        i++
    }
    return clmNull()
}

// ---------------- LIBRARY / LIBSIGN01 读取 ----------------

private fun clmIsPrefixed(libApi: dynamic, uri: dynamic): dynamic {
    try {
        val start = clmArrayOf(0x4c, 0x49, 0x42, 0x52, 0x41, 0x52, 0x59)
        var rd = js("ExternalStorage").openInputStream(uri)
        while (clmLength(start) > 0) {
            if (clmLooseNe(rd.read(), start.shift())) {
                rd.close()
                return false
            }
        }
        rd.skip(8)
        rd = clmNewGzipReader(rd)
        while (true) {
            val q = rd.readLine()
            if (!clmTruthy(q)) break
        }
        rd.close()
        return true
    } catch (e: dynamic) {
        return false
    }
}

private fun clmLoadPrefixed(
    libApi: dynamic, uri: dynamic, defaultValue: dynamic, error: dynamic
): dynamic {
    try {
        val start = clmArrayOf(0x4c, 0x49, 0x42, 0x52, 0x41, 0x52, 0x59)
        var rd = js("ExternalStorage").openInputStream(uri)
        while (clmLength(start) > 0) {
            if (clmLooseNe(rd.read(), start.shift())) {
                rd.close()
                clmThrow("不是已锁定的拓展包")
            }
        }
        rd.skip(8)
        rd = clmNewGzipReader(rd)
        val s = clmArray()
        while (true) {
            val q = rd.readLine()
            if (!clmTruthy(q)) break
            s.push(q)
        }
        rd.close()
        return libApi.evalLib(uri, s.join("\n"))
    } catch (e: dynamic) {
        if (clmTruthy(error)) error.error = e
        return defaultValue
    }
}

private fun clmSavePrefixed(uri: dynamic, obj: dynamic): dynamic {
    var wr = js("ExternalStorage").openOutputStream(uri)
    val ar = clmNewByteBufferAllocate(15) // LIBRARY
    clmByteBufferPutHeader(ar)
    wr.write(ar.array())
    wr = clmNewGzipOutputStream(wr)
    wr.write(clmStringBytes(js("MapScript").toSource(obj)))
    wr.close()
    return clmUndefined()
}

// ---------------- checkLibrary ----------------

// 原 IIFE 的闭包状态：stack / last 在多次调用之间保留。
private var clmCheckStack: dynamic = clmNull()
private var clmCheckLast: dynamic = clmNull()

private fun clmCheckError(d: dynamic): dynamic {
    val o = clmObject()
    o.message = d
    o.stack = clmCheckStack
    o.source = clmCheckLast
    o.toString = clmCheckErrorToStringJs
    return o
}

private fun clmCheckObject(o: dynamic) {
    if (!clmTruthy(o) || !clmInstanceOf(o, js("Object"))) clmThrow(clmCheckError("不是对象"))
}

private fun clmCheckArray(o: dynamic) {
    if (!clmIsArray(o)) clmThrow(clmCheckError("不是数组"))
}

private fun clmCheckUnsignedInt(o: dynamic) {
    if (!clmRegexUnsigned(o)) clmThrow(clmCheckError("不是正整数"))
}

private fun clmCheckString(o: dynamic) {
    if (clmStrictNe(clmTypeof(o), "string")) clmThrow(clmCheckError("不是字符串"))
}

private fun clmCheckNotEmptyString(o: dynamic) {
    clmCheckString(o)
    if (!clmTruthy(o)) clmThrow(clmCheckError("是空字符串"))
}

private fun clmCheckIterateArray(o: dynamic, iter: dynamic): dynamic {
    val l = clmLength(clmCheckStack)
    clmCheckArray(o)
    clmSetLength(clmCheckStack, clmAdd(l, 1))
    var i = 0
    while (i < clmLength(o)) {
        clmCheckStack[l] = i
        iter(o[i])
        i++
    }
    clmSetLength(clmCheckStack, l)
    return clmUndefined()
}

private fun clmCheckIterateObject(
    o: dynamic, keyIter: dynamic, valueIter: dynamic
): dynamic {
    val l = clmLength(clmCheckStack)
    clmCheckObject(o)
    clmSetLength(clmCheckStack, clmAdd(l, 1))
    clmForIn(o, { k: dynamic ->
        clmCheckStack[l] = k
        keyIter(k)
        // 原有 bug：原文件这里写的是 valueIter(v)，而 v 未定义。
        // 该函数未被调用；为保留错误形态，这里传 undefined，而不是 o[k]。
        valueIter(clmUndefined())
        js("undefined")
    })
    clmSetLength(clmCheckStack, l)
    return clmUndefined()
}

private fun clmCheckLibrary(a: dynamic): dynamic {
    clmCheckStack = clmArrayOf("根")
    clmCheckLast = a
    clmCheckObject(a)
    clmCheckStack.push("名称(name)")
    clmCheckNotEmptyString(a.name)
    clmCheckStack[1] = "作者(author)"
    clmCheckNotEmptyString(a.author)
    clmCheckStack[1] = "简介(description)"
    clmCheckString(a.description)
    clmCheckStack[1] = "UUID(uuid)"
    clmCheckNotEmptyString(a.uuid)
    clmCheckStack[1] = "版本(version)"
    clmCheckIterateArray(a.version, { e: dynamic ->
        clmCheckUnsignedInt(e)
        js("undefined")
    })
    clmCheckStack[1] = "前提包(require)"
    //require 是数组，元素可以是 UUID 字符串，也可以是带版本区间的对象
    clmCheckArray(a.require)
    return clmUndefined()
}

// ---------------- checkPackVer ----------------

// 原 IIFE 的 a 闭包变量；每次 checkPackVer 调用时重新赋值。
private var clmCheckPackA: dynamic = clmUndefined()

private fun clmCheckPackOpt(a: dynamic): dynamic {
    if (clmLooseEq(a, "*")) return js("Infinity")
    if (clmIsNaN(a)) return -1
    return clmParseInt(a)
}

private fun clmCheckPackCompare(b: dynamic): dynamic {
    val bs = clmString(b).split(".")
    val n = clmMathMax(clmLength(clmCheckPackA), clmLength(bs))
    var i = 0
    while (clmLt(i, n)) {
        val p1 = clmCheckPackOpt(clmCheckPackA[i])
        val p2 = clmCheckPackOpt(bs[i])
        if (clmLt(p1, p2)) {
            return -1 //pe版本过低
        } else if (clmGt(p1, p2)) {
            return 1 //拓展包版本过低
        }
        i++
    }
    return 0
}

private fun clmCheckPackInRange(
    min: dynamic, max: dynamic, exclusiveMin: dynamic, exclusiveMax: dynamic
): dynamic {
    if (clmTruthy(min) && clmLt(clmCheckPackCompare(min), 0)) return -1
    if (clmTruthy(max) && clmGt(clmCheckPackCompare(max), 0)) return 1
    //开区间：最小版本不包含
    if (clmTruthy(exclusiveMin) && clmLe(clmCheckPackCompare(exclusiveMin), 0)) return -1
    if (clmTruthy(exclusiveMax) && clmGe(clmCheckPackCompare(exclusiveMax), 0)) return 1
    return 0
}

private fun clmCheckPackVer(libApi: dynamic, o: dynamic): dynamic {
    var r: dynamic = 0
    if (clmTruthy(libApi.ignoreVersion)) return 0
    clmCheckPackA = clmString(clmGetMinecraftVersion()).split(".")
    if (clmTruthy(o.minSupportVer) || clmTruthy(o.maxSupportVer)) {
        r = clmCheckPackInRange(o.minSupportVer, o.maxSupportVer, clmNull(), clmNull())
        if (clmLooseNe(r, 0)) return r //这两个参数是总范围
    }
    if (clmIsArray(o.supportVer)) {
        val n = clmLength(o.supportVer)
        r = 1
        var i = 0
        while (i < n) {
            val e = o.supportVer[i]
            //趋向返回游戏版本过低
            r = clmMathMin(r, clmCheckPackInRange(e.min, e.max, e.exclusiveMin, e.exclusiveMax))
            if (clmLooseEq(r, 0)) return 0 //这段只要存在一个范围符合条件就返回0
            i++
        }
    }
    return r
}

// ---------------- joinPack ----------------

private fun clmJoinPackJoinCmd(src: dynamic, o: dynamic): dynamic {
    if (clmTruthy(o.description)) src.description = o.description
    if (clmTruthy(o.help)) src.help = o.help
    if (clmTruthy(o.noparams)) src.noparams = o.noparams
    if (clmTruthy(o.patterns)) {
        val op = o.patterns
        val sp = src.patterns
        if (clmIsArray(sp) != clmIsArray(op)) clmThrow("命令模式格式不一致，无法合并")
        if (clmIsArray(op)) { // Deprecated
            clmForIn(op, { i: dynamic ->
                val t = sp.indexOf(op[i])
                if (clmLt(t, 0)) sp.push(op[i])
                js("undefined")
            })
        } else {
            clmForIn(op, { i: dynamic ->
                sp[i] = op[i]
                js("undefined")
            })
        }
    }
    return clmUndefined()
}

private fun clmJoinPackFilterCmd(src: dynamic, o: dynamic): dynamic {
    if (clmTruthy(o.noparams)) clmDelete(src, "noparams")
    if (clmTruthy(o.patterns)) {
        val op = o.patterns
        val sp = src.patterns
        if (clmIsArray(sp) != clmIsArray(op)) clmThrow("命令模式格式不一致，无法过滤")
        if (clmIsArray(op)) { // Deprecated
            clmForIn(op, { i: dynamic ->
                val t = sp.indexOf(op[i])
                if (clmGe(t, 0)) sp.splice(t, 1)
                js("undefined")
            })
        } else {
            clmForIn(op, { i: dynamic ->
                clmDelete(sp, i)
                js("undefined")
            })
        }
    }
    return clmUndefined()
}

private fun clmJoinPackJoinEnum(src: dynamic, o: dynamic): dynamic {
    if (clmIsArray(src) && clmIsArray(o)) {
        clmForIn(o, { i: dynamic ->
            val t = src.indexOf(o[i])
            if (clmLt(t, 0)) src.push(o[i])
            js("undefined")
        })
    } else if (clmIsArray(src) && !clmIsArray(o)) {
        clmThrow("枚举列表格式不一致，无法合并")
    } else if (!clmIsArray(src) && clmIsArray(o)) {
        clmForIn(o, { i: dynamic ->
            if (!clmTruthy(src[o[i]])) src[o[i]] = ""
            js("undefined")
        })
    } else {
        clmForIn(o, { i: dynamic ->
            if (!clmTruthy(src[i]) || clmLooseNe(o[i], "")) src[i] = o[i]
            js("undefined")
        })
    }
    return clmUndefined()
}

private fun clmJoinPackFilterEnum(src: dynamic, o: dynamic): dynamic {
    val f = if (clmIsArray(o)) o else clmObjectKeys(o)
    if (clmIsArray(src)) {
        clmForIn(f, { i: dynamic ->
            val t = src.indexOf(f[i])
            if (clmGe(t, 0)) src.splice(t, 1)
            js("undefined")
        })
    } else {
        clmForIn(f, { i: dynamic ->
            clmDelete(src, f[i])
            js("undefined")
        })
    }
    return clmUndefined()
}

private fun clmJoinPackParseAliasEnum(g: dynamic, o: dynamic): dynamic {
    if (clmStrictNe(clmTypeof(o), "string")) return o
    if (!clmIn(o, g.enums)) clmThrow("无效的枚举引用")
    return g.enums[o]
}

private fun clmJoinPackJoinTutorial(src: dynamic, o: dynamic): dynamic {
    var i = 0
    while (i < clmLength(src)) {
        if (clmLooseEq(src[i].id, o.id)) {
            src[i] = o
            return clmUndefined()
        }
        i++
    }
    src.push(o)
    return clmUndefined()
}

private fun clmJoinPackFilterTutorial(src: dynamic, o: dynamic): dynamic {
    var i = 0
    while (i < clmLength(src)) {
        if (clmLooseEq(src[i].id, o.id)) {
            src.splice(i, 1)
            return clmUndefined()
        }
        i++
    }
    return clmUndefined()
}

private fun clmJoinPackJoinIDList(src: dynamic, o: dynamic): dynamic {
    var i = 0
    while (i < clmLength(src)) {
        if (clmLooseEq(src[i].name, o.name)) {
            src[i] = o
            return clmUndefined()
        }
        i++
    }
    src.push(o)
    return clmUndefined()
}

private fun clmJoinPackFilterIDList(src: dynamic, o: dynamic): dynamic {
    var i = 0
    while (i < clmLength(src)) {
        if (clmLooseEq(src[i].name, o.name)) {
            src.splice(i, 1)
            return clmUndefined()
        }
        i++
    }
    return clmUndefined()
}

private fun clmJoinPack(libApi: dynamic, cur: dynamic, l: dynamic): dynamic {
    if (clmLooseNe(libApi.checkPackVer(l), 0)) return false
    if (!clmInstanceOf(l.commands, js("Object"))) l.commands = clmObject()
    if (!clmInstanceOf(l.enums, js("Object"))) l.enums = clmObject()
    if (!clmInstanceOf(l.selectors, js("Object"))) l.selectors = clmObject()
    // 拓展包可以携带 JSON Schema，供 jsonPro 类型的参数做补全与校验
    if (!clmInstanceOf(l.jsonSchema, js("Object"))) l.jsonSchema = clmObject()
    if (!clmInstanceOf(l.states, js("Object"))) l.states = clmObject()
    if (!clmInstanceOf(l.help, js("Object"))) l.help = clmObject()
    clmForIn(l.commands, { i: dynamic ->
        if (clmLooseEq(l.mode, "remove")) {
            if (clmTruthy(l.commands[i])) {
                clmJoinPackFilterCmd(cur.commands[i], l.commands[i])
            } else {
                clmDelete(cur.commands, i)
            }
        } else if (clmIn(i, cur.commands) && clmLooseNe(l.mode, "overwrite")) {
            clmJoinPackJoinCmd(cur.commands[i], l.commands[i])
        } else {
            cur.commands[i] = clmObjectCopy(l.commands[i])
        }
        js("undefined")
    })
    clmForIn(l.enums, { i: dynamic ->
        if (clmLooseEq(l.mode, "remove")) {
            if (clmTruthy(l.enums[i])) {
                clmJoinPackFilterEnum(
                    cur.enums[i], clmJoinPackParseAliasEnum(cur, l.enums[i])
                )
            } else {
                clmDelete(cur.enums, i)
            }
        } else if (clmIn(i, cur.enums) && clmLooseNe(l.mode, "overwrite")) {
            clmJoinPackJoinEnum(cur.enums[i], clmJoinPackParseAliasEnum(cur, l.enums[i]))
        } else {
            cur.enums[i] = clmObjectCopy(clmJoinPackParseAliasEnum(cur, l.enums[i]))
        }
        js("undefined")
    })
    clmForIn(l.selectors, { i: dynamic ->
        if (clmLooseEq(l.mode, "remove")) {
            clmDelete(cur.selectors, i)
        } else {
            cur.selectors[i] = l.selectors[i]
        }
        js("undefined")
    })
    clmForIn(l.json, { i: dynamic ->
        if (clmLooseEq(l.mode, "remove")) {
            clmDelete(cur.json, i)
        } else {
            cur.json[i] = l.json[i]
        }
        js("undefined")
    })
    clmForIn(l.jsonSchema, { i: dynamic ->
        if (clmLooseEq(l.mode, "remove")) {
            clmDelete(cur.jsonSchema, i)
        } else {
            cur.jsonSchema[i] = l.jsonSchema[i]
        }
        js("undefined")
    })
    // 方块状态（status 参数用）：namespace（如 block）→ 状态表
    clmForIn(l.states, { i: dynamic ->
        if (clmLooseEq(l.mode, "remove")) {
            clmDelete(cur.states, i)
        } else {
            cur.states[i] = l.states[i]
        }
        js("undefined")
    })
    clmForIn(l.help, { i: dynamic ->
        if (clmLooseEq(l.mode, "remove")) {
            clmDelete(cur.help, i)
        } else {
            cur.help[i] = l.help[i]
        }
        js("undefined")
    })
    clmForIn(l.tutorials, { i: dynamic ->
        if (clmLooseEq(l.mode, "remove")) {
            clmJoinPackFilterTutorial(cur.tutorials, l.tutorials[i])
        } else {
            clmJoinPackJoinTutorial(cur.tutorials, l.tutorials[i])
        }
        js("undefined")
    })
    clmForIn(l.idlist, { i: dynamic ->
        if (clmLooseEq(l.mode, "remove")) {
            clmJoinPackFilterIDList(cur.idlist, l.idlist[i])
        } else {
            clmJoinPackJoinIDList(cur.idlist, l.idlist[i])
        }
        js("undefined")
    })
    return true
}

// ---------------- statLib ----------------

// 原 IIFE 的 stat 闭包变量。
private var clmStat: dynamic = clmUndefined()

private fun clmStatCalcCmd(c: dynamic): dynamic {
    if (!clmTruthy(c)) return clmUndefined()
    clmStat.command = clmAdd(clmStat.command, 1)
    if (clmTruthy(c.noparams)) clmStat.pattern = clmAdd(clmStat.pattern, 1)
    // patterns 是 可枚举类型 包括但不限于 数组、对象
    clmForIn(c.patterns, { _i: dynamic ->
        clmStat.pattern = clmAdd(clmStat.pattern, 1)
        js("undefined")
    })
    return clmUndefined()
}

private fun clmStatCalcSelectors(c: dynamic): dynamic {
    if (!clmTruthy(c)) return clmUndefined()
    clmStat.selector = clmAdd(clmStat.selector, clmLength(clmObjectKeys(c)))
    return clmUndefined()
}

private fun clmStatCalcEnum(c: dynamic): dynamic {
    if (!clmTruthy(c)) return 0
    return if (clmStrictEq(clmTypeof(c), "string")) {
        0
    } else if (clmIsArray(c)) {
        clmLength(c)
    } else {
        clmLength(clmObjectKeys(c))
    }
}

private fun clmStatCalcEnums(c: dynamic): dynamic {
    if (!clmTruthy(c)) return clmUndefined()
    clmForIn(c, { i: dynamic ->
        clmStat.enums = clmAdd(clmStat.enums, 1)
        clmStat.enumitem = clmAdd(clmStat.enumitem, clmStatCalcEnum(c[i]))
        js("undefined")
    })
    return clmUndefined()
}

private fun clmStatCalcCommands(k: dynamic): dynamic {
    if (!clmTruthy(k)) return clmUndefined()
    clmForIn(k, { i: dynamic ->
        clmStatCalcCmd(k[i])
        js("undefined")
    })
    return clmUndefined()
}

private fun clmStatLib(l: dynamic): dynamic {
    clmStat = clmObject()
    clmStat.availablePack = 0
    clmStat.command = 0
    clmStat.versionPack = 0
    clmStat.enums = 0
    clmStat.selector = 0
    clmStat.pattern = 0
    clmStat.enumitem = 0
    clmStat.toString = clmStatToStringJs
    clmStatCalcCommands(l.commands)
    clmStatCalcEnums(l.enums)
    clmStatCalcSelectors(l.selectors)
    clmForIn(l.versionPack, { i: dynamic ->
        if (clmIn("commands", l.versionPack[i])) clmStatCalcCommands(l.versionPack[i].commands)
        if (clmIn("enums", l.versionPack[i])) clmStatCalcEnums(l.versionPack[i].enums)
        if (clmIn("selectors", l.versionPack[i])) {
            clmStatCalcSelectors(l.versionPack[i].selectors)
        }
        clmStat.versionPack = clmAdd(clmStat.versionPack, 1)
        js("undefined")
    })
    return clmStat
}

// ---------------- 在线源 / 源信息 ----------------

private fun clmRequestDefaultSourceInfo(libApi: dynamic): dynamic =
    libApi.requestSourceInfoCached(libApi.getSourceUrl())

private fun clmRequestSourceInfoCached(libApi: dynamic, url: dynamic): dynamic {
    var u = clmString(url)
    if (clmLooseNe(u.slice(-1), "/")) u = clmAdd(u, "/")
    var sourceInfo = libApi.sourceInfoCache[u]
    if (!clmTruthy(sourceInfo) || !clmLt(clmDateNow(), sourceInfo.accessExpired)) {
        sourceInfo = libApi.requestSourceInfo(u)
        if (!clmTruthy(sourceInfo)) return clmUndefined()
        sourceInfo.accessExpired = clmAdd(clmDateNow(), 60000)
        libApi.sourceInfoCache[u] = sourceInfo
        // 源端声明了 name 时顺手同步已添加源的显示名：
        // 早期只能取 maintainer，加过源的会显示成人名
        if (clmTruthy(sourceInfo.name)) {
            val src = libApi.findSource(u)
            if (clmTruthy(src) && !clmTruthy(src.builtin) &&
                clmLooseNe(src.name, clmString(sourceInfo.name))
            ) {
                src.name = clmString(sourceInfo.name)
                libApi.saveSources()
            }
        }
    }
    return sourceInfo
}

private fun clmRequestSourceInfo(url: dynamic): dynamic {
    var u = clmString(url)
    if (clmLooseNe(u.slice(-1), "/")) u = clmAdd(u, "/")
    val infourl = clmAdd(u, "info.json")
    var info: dynamic
    try {
        info = clmJsonParse(js("NetworkUtils").queryPage(infourl))
    } catch (e: dynamic) {
        js("Log").e(e)
        return clmUndefined()
    }
    info.pages = clmArray()
    info.nextPage = if (clmGt(info.indexPages, 0)) info.index else clmNull()
    info.pageCount = info.indexPages
    info.libCount = info.indexLibs
    info.url = u
    return info
}

private fun clmRequestSourceIndex(info: dynamic, pageNo: dynamic): dynamic {
    if (clmLt(pageNo, 0) || clmGe(pageNo, info.indexPages)) return clmUndefined()
    if (clmLt(pageNo, clmLength(info.pages))) return info.pages[pageNo]
    var page: dynamic
    try {
        while (true) {
            page = clmJsonParse(js("NetworkUtils").queryPage(info.nextPage))
            if (clmLooseNe(page.pageNo, clmLength(info.pages)) ||
                clmLooseNe(page.sourceId, info.sourceId)
            ) {
                clmThrow("Not a regular library source")
            }
            info.nextPage = page.nextPage
            info.pages.push(page.content)
            if (clmLt(pageNo, clmLength(info.pages))) return info.pages[pageNo]
        }
    } catch (e: dynamic) {
        js("Log").e(e)
        return clmUndefined()
    }
}

private fun clmRequestSourceMap(info: dynamic): dynamic {
    if (clmTruthy(info.libMap)) return info.libMap
    try {
        val map = clmJsonParse(js("NetworkUtils").queryPage(info.map))
        if (clmLooseNe(map.sourceId, info.sourceId)) {
            clmThrow("Not a regular library source")
        }
        info.libMap = map.content
        return info.libMap
    } catch (e: dynamic) {
        js("Log").e(e)
        return clmUndefined()
    }
}

private fun clmGetSourceUrl(libApi: dynamic): dynamic = libApi.getOriginSourceUrl()

private fun clmGetOriginSourceUrl(): dynamic = "https://ca.projectxero.top/clib/"

// ───────────── 拓展包源管理 ─────────────
// 源对象：{ url, name, enabled, builtin }，存在 CA.settings.librarySources 里

/** 取源列表；第一次使用时补上内置官方源 */
private fun clmGetSources(libApi: dynamic): dynamic {
    val name = "命令助手官方拓展包源"
    var list = js("CA").settings.librarySources
    if (!clmIsArray(list)) {
        list = clmArray()
        js("CA").settings.librarySources = list
    }
    val builtin = list.filter({ s: dynamic -> clmTruthy(s) && clmTruthy(s.builtin) })[0]
    if (!clmTruthy(builtin)) {
        val o = clmObject()
        o.url = libApi.getOriginSourceUrl()
        o.name = name
        o.enabled = true
        o.builtin = true
        list.unshift(o)
    } else if (clmLooseNe(builtin.name, name)) {
        // 旧版本叫“官方源”/“命令助手拓展包源”，统一改名（内置源的显示名不开放编辑，直接覆盖）
        builtin.name = name
    }
    return list
}

/** 规范化源的 URL：补 http(s)://、去掉多余的空白、保证以 / 结尾 */
private fun clmFindSource(libApi: dynamic, url: dynamic): dynamic {
    val list = libApi.getSources()
    var i = 0
    while (i < clmLength(list)) {
        if (clmTruthy(list[i]) &&
            clmLooseEq(libApi.normalizeSourceUrl(list[i].url), libApi.normalizeSourceUrl(url))
        ) {
            return list[i]
        }
        i++
    }
    return clmNull()
}

/** 启用中的源 */
private fun clmEnabledSources(libApi: dynamic): dynamic =
    libApi.getSources().filter({ s: dynamic -> clmTruthy(s) && clmTruthy(s.enabled) })

/**
 * 校验并添加一个源。url 可以是源站根目录，也可以是 info.json 的地址。
 * 成功返回源对象；失败抛错（供界面显示）。
 */
private fun clmAddSource(libApi: dynamic, url: dynamic): dynamic {
    var u = libApi.normalizeSourceUrl(url)
    if (!clmTruthy(u)) clmThrow("请输入源的地址")
    if (clmLooseEq(u.slice(-10), "info.json/")) u = u.slice(0, -10)
    if (clmTruthy(libApi.findSource(u))) clmThrow("该源已经存在")
    val info = libApi.requestSourceInfo(u)
    if (!clmTruthy(info)) clmThrow(clmAdd("无法访问该源（读取 info.json 失败）\n", u))
    if (!clmTruthy(info.sourceId)) clmThrow("不是有效的拓展包源（缺少 sourceId）")
    val src = clmObject()
    src.url = u
    // 名称优先用 info.json 的 name，没有就退回维护者 / 描述 / 地址
    src.name = if (clmTruthy(info.name)) {
        clmString(info.name)
    } else if (clmTruthy(info.maintainer) && clmLooseNe(info.maintainer, u)) {
        clmString(info.maintainer)
    } else if (clmTruthy(info.details)) {
        clmString(info.details)
    } else {
        u
    }
    src.enabled = true
    src.builtin = false
    libApi.getSources().push(src)
    libApi.saveSources()
    return src
}

private fun clmRemoveSource(libApi: dynamic, url: dynamic): dynamic {
    val list = libApi.getSources()
    var i = 0
    while (i < clmLength(list)) {
        if (clmTruthy(list[i]) &&
            clmLooseEq(libApi.normalizeSourceUrl(list[i].url), libApi.normalizeSourceUrl(url))
        ) {
            if (clmTruthy(list[i].builtin)) clmThrow("内置源不能删除")
            list.splice(i, 1)
            libApi.saveSources()
            return true
        }
        i++
    }
    return false
}

private fun clmSetSourceEnabled(
    libApi: dynamic, url: dynamic, enabled: dynamic
): dynamic {
    val src = libApi.findSource(url)
    if (!clmTruthy(src)) return false
    if (!clmTruthy(enabled) && clmTruthy(src.builtin)) clmThrow("内置源不能禁用")
    src.enabled = clmTruthy(enabled)
    libApi.saveSources()
    return true
}

/**
 * 把设置立刻写盘。
 * 之前源的增删改、拓展包的启用/禁用/安装只改内存里的 CA.settings，
 * 进程被系统杀掉（或用户手动结束）后这些改动就全丢了。
 */
private fun clmSaveState(): dynamic {
    try {
        if (clmLooseEq(clmTypeof(js("CA").trySave), "function")) js("CA").trySave()
    } catch (e: dynamic) {
        js("Log").e(e)
    }
    return clmUndefined()
}

/** 兼容旧名字 */
private fun clmSaveSources(): dynamic = clmSaveState()

/** 改源备注并保存 */
private fun clmSetSourceNote(
    libApi: dynamic, url: dynamic, note: dynamic
): dynamic {
    val src = libApi.findSource(url)
    if (!clmTruthy(src)) return false
    src.note = clmString(clmOr(note, "")).trim()
    libApi.saveSources()
    return true
}

private fun clmGetVerify(libApi: dynamic, source: dynamic): dynamic {
    if (clmTruthy(source.verifyObject)) {
        return source.verifyObject
    } else {
        source.verifyObject = libApi.downloadAsArray(source.pubkey)
        return source.verifyObject
    }
}

private fun clmGetLibraryRoot(): dynamic = clmNewBaseLibsDir()

private fun clmGetLibraryExternalRoot(): dynamic = clmGetExternalFilesDir()

private fun clmArrayStartsWith(array: dynamic, start: dynamic): dynamic {
    var i = 0
    if (clmLength(array) < clmLength(start)) return false
    while (i < clmLength(start)) {
        if (clmLooseNe(start[i], array[i])) return false
        i++
    }
    return true
}

private fun clmReadAsArray(stream: dynamic, keep: dynamic): dynamic {
    val BUFFER_SIZE = 2048
    val os = clmNewByteArrayOutputStream()
    val buf = clmNewByteArray(BUFFER_SIZE)
    var hr = stream.read(buf)
    while (clmGt(hr, 0)) {
        os.write(buf, 0, hr)
        hr = stream.read(buf)
    }
    if (!clmTruthy(keep)) stream.close()
    return os.toByteArray()
}

private fun clmDownloadAsArray(url: dynamic): dynamic {
    val u = clmNewURL(url)
    val conn = u.openConnection()
    conn.setConnectTimeout(5000)
    conn.setUseCaches(false)
    conn.setRequestMethod("GET")
    conn.connect()
    return clmReadAsArray(conn.getInputStream(), clmUndefined())
}

// ---------------- 下载 / 签名校验 ----------------

private fun clmDownloadLib(
    libApi: dynamic, libinfo: dynamic, source: dynamic
): dynamic {
    val arr = libApi.downloadAsArray(libinfo.downloadurl)
    var digest = clmNewMessageDigest("SHA-1")
    digest.update(arr)
    if (clmLooseNe(libinfo.sha1, clmBase64Encode(digest.digest()))) {
        clmThrow("文件校验失败")
    }
    val libraryRoot = libApi.getLibraryRoot()
    val libraryFile = clmNewFile2(
        libraryRoot, clmAdd(clmAdd(libinfo.uuid, "/"), libinfo.name)
    )
    if (libApi.arrayStartsWith(
            arr, clmArrayOf(0x53, 0x49, 0x47, 0x4e, 0x4c, 0x49, 0x42, 0x30, 0x31)
        )
    ) { //SIGNLIB01
        val verify = libApi.getVerify(source)
        val signature = clmNewSignature("SHA256withRSA")
        val keyFactory = clmNewKeyFactory("RSA")
        val keySpec = clmNewX509EncodedKeySpec(verify)
        signature.initVerify(keyFactory.generatePublic(keySpec))
        val signlen = 256
        val dataStart = 9 + signlen
        signature.update(arr, dataStart, clmSub(clmLength(arr), dataStart))
        if (!clmTruthy(signature.verify(arr, 9, signlen))) clmThrow("库的签名不正确")
        // 判断这是不是"社区包"：拿验签用的公钥和**官方源的公钥**比。
        // - 一致       → [官方]，有官方源背书
        // - 不一致     → [社区]，源自己的 key 验的签，能装但没官方背书
        // - 官方 key 拿不到 → 无法判定，按 [官方] 处理（不误标）
        // 注意这里不能拿 ScriptInterface.getVerifyKey() 来比：那是 App 内置的
        // hotfix 公钥，和官方拓展包源用的 verify.key 是两把不同的钥匙。
        // 两把 key 都必须存在才可能判为社区包。
        var isCommunity = false
        if (clmTruthy(source) && !clmTruthy(source.builtin)) {
            // 非官方源的包：默认就是社区包；若它的 key 恰好等于官方 key
            // （例如官方源的镜像），仍按官方处理。
            val officialKey = libApi.getOfficialVerify()
            isCommunity = !clmTruthy(officialKey) || !clmBytesEqual(verify, officialKey)
        }
        libraryFile.getParentFile().mkdirs()
        var os = clmNewFileOutputStream(libraryFile)
        os.write(clmStringBytesUtf8("LIBSIGN01"))
        val bytes = clmStringBytesUtf8(clmString(source.sourceId))
        val buf = clmNewByteBufferAllocate(4)
        buf.order(js("java.nio.ByteOrder").BIG_ENDIAN)
        buf.putInt(clmLength(bytes))
        os.write(buf.array())
        os.write(bytes)
        os.write(arr, 9, clmSub(clmLength(arr), 9))
        os.close()
        digest = clmNewMessageDigest("SHA-1")
        digest.update(arr, 9, clmSub(clmLength(arr), 9))
        var bytes2 = digest.digest()
        digest.update(clmGetVerifyKey())
        digest.update(bytes2)
        bytes2 = digest.digest()
        os = clmNewFileOutputStream(
            clmNewFile2(libraryFile.getParent(), clmAdd(libraryFile.getName(), ".hash"))
        )
        os.write(bytes2)
        os.close()
        // 社区包标记；官方包则清掉可能残留的旧标记
        val markFile = clmNewFile2(
            libraryFile.getParent(), clmAdd(libraryFile.getName(), ".community")
        )
        if (isCommunity) {
            // Rhino 里没有 byte 这个类型，new byte[0] 会报"byte 未定义"；
            // 写一个空字符串的字节就是 0 字节文件。
            os = clmNewFileOutputStream(markFile)
            os.write(clmStringBytesUtf8(""))
            os.close()
        } else if (clmTruthy(markFile.exists())) {
            markFile.delete()
        }
    } else {
        libraryFile.getParentFile().mkdirs()
        val os = clmNewFileOutputStream(libraryFile)
        os.write(arr)
        os.close()
    }
    return js("ExternalStorage").toUri(libraryFile)
}

private fun clmShouldVerifySigned(libApi: dynamic, uri: dynamic): dynamic {
    if (!clmTruthy(js("ExternalStorage").isFile(uri))) return -1
    val file = js("ExternalStorage").uriToFile(uri)
    if (!clmTruthy(file)) return -1
    var arr = libApi.readAsArray(clmNewFileInputStream(file), clmUndefined())
    if (libApi.arrayStartsWith(
            arr, clmArrayOf(0x4c, 0x49, 0x42, 0x53, 0x49, 0x47, 0x4e, 0x30, 0x31)
        )
    ) { //LIBSIGN01
        val buf = clmNewByteBufferWrap(arr)
        val sourceSize = buf.getInt(9)
        val hashFile = clmNewFile1(clmAdd(file.getPath(), ".hash"))
        if (!clmTruthy(hashFile.isFile())) return 0
        var digest = clmNewMessageDigest("SHA-1")
        digest.update(
            arr, clmAdd(13, sourceSize), clmSub(clmLength(arr), clmAdd(13, sourceSize))
        )
        var bytes = digest.digest()
        digest.update(clmGetVerifyKey())
        digest.update(bytes)
        bytes = digest.digest()
        arr = libApi.readAsArray(clmNewFileInputStream(hashFile), clmUndefined())
        if (clmLooseNe(clmLength(arr), clmLength(bytes))) return 0
        var i = 0
        while (i < clmLength(arr)) {
            if (clmLooseNe(arr[i], bytes[i])) return 0
            i++
        }
        return 1
    } else {
        return -1
    }
}

private fun clmLoadSignedV1(
    libApi: dynamic, uri: dynamic, defaultValue: dynamic, error: dynamic
): dynamic {
    try {
        val start = clmArrayOf(0x4c, 0x49, 0x42, 0x53, 0x49, 0x47, 0x4e, 0x30, 0x31) //LIBSIGN01
        var rd = js("ExternalStorage").openInputStream(uri)
        while (clmLength(start) > 0) {
            if (clmLooseNe(rd.read(), start.shift())) {
                rd.close()
                clmThrow("不是已签名的拓展包")
            }
        }
        val buf = clmNewByteBufferAllocate(4)
        buf.order(js("java.nio.ByteOrder").BIG_ENDIAN)
        rd.read(buf.array())
        rd.skip(clmAdd(clmAdd(buf.getInt(0), 256), 8))
        rd = clmNewGzipReader(rd)
        val s = clmArray()
        while (true) {
            val q = rd.readLine()
            if (!clmTruthy(q)) break
            s.push(q)
        }
        rd.close()
        return libApi.evalLib(uri, s.join("\n"))
    } catch (e: dynamic) {
        if (clmTruthy(error)) error.error = e
        return defaultValue
    }
}

/**
 * 官方源的公钥（DER）；取不到时返回 null。
 * 注意：getSources() 的元素只有 {url,name,enabled,builtin}，
 * pubkey 字段来自 info.json，必须先 requestSourceInfoCached 再取。
 */
private fun clmGetOfficialVerify(libApi: dynamic): dynamic {
    val sources = libApi.getSources()
    var src: dynamic = clmNull()
    var i = 0
    while (i < clmLength(sources)) {
        if (clmTruthy(sources[i]) && clmTruthy(sources[i].builtin)) {
            src = sources[i]
            break
        }
        i++
    }
    if (!clmTruthy(src) || !clmTruthy(src.url)) return clmNull()
    try {
        val info = libApi.requestSourceInfoCached(src.url)
        if (!clmTruthy(info) || !clmTruthy(info.pubkey)) return clmNull()
        return libApi.getVerify(info)
    } catch (e: dynamic) {
        return clmNull()
    }
}

/** 两个字节数组是否完全相同 */
private fun clmBytesEqual(a: dynamic, b: dynamic): dynamic {
    if (!clmTruthy(a) || !clmTruthy(b) || clmLooseNe(clmLength(a), clmLength(b))) return false
    var i = 0
    while (i < clmLength(a)) {
        if (clmLooseNe(a[i], b[i])) return false
        i++
    }
    return true
}

/** 低安全级别下对未签名包的提醒（每次运行只提示一次） */
private fun clmWarnLowSecurity(libApi: dynamic): dynamic {
    if (clmTruthy(libApi.lowSecurityWarned)) return clmUndefined()
    libApi.lowSecurityWarned = true
    try {
        js("G").ui({
            try {
                js("Common").toast("当前安全级别会加载未签名的锁定包（LIBRARY），请确认来源可信")
            } catch (e: dynamic) {
                js("erp")(e)
            }
            js("undefined")
        })
    } catch (e: dynamic) {
    }
    return clmUndefined()
}

/** 是否为社区包：由非官方公钥验签通过、且带 .community 标记的已签名包 */
private fun clmIsCommunityLib(uri: dynamic): dynamic {
    try {
        val file = js("ExternalStorage").uriToFile(uri)
        if (!clmTruthy(file)) return false
        return clmNewFile1(clmAdd(file.getPath(), ".community")).isFile()
    } catch (e: dynamic) {
        return false
    }
}

private fun clmTryReleaseLibraryFile(libApi: dynamic, uri: dynamic): dynamic {
    val file = js("ExternalStorage").uriToFile(uri)
    if (clmTruthy(file)) {
        val libraryRoot = libApi.getLibraryRoot()
        val libraryExternalRoot = libApi.getLibraryExternalRoot()
        val inLibraryRoot = js("ExternalStorage").isParentOrSelf(file, libraryRoot)
        val inLibraryExternalRoot =
            js("ExternalStorage").isParentOrSelf(file, libraryExternalRoot)
        if (clmTruthy(inLibraryRoot) || clmTruthy(inLibraryExternalRoot)) {
            val parentFile = file.getParentFile()
            val hashFile = clmNewFile2(parentFile, clmAdd(file.getName(), ".hash"))
            val communityFile = clmNewFile2(
                parentFile, clmAdd(file.getName(), ".community")
            )
            file.delete()
            if (clmTruthy(hashFile.exists())) hashFile.delete()
            if (clmTruthy(communityFile.exists())) communityFile.delete()
            if (clmTruthy(inLibraryRoot)) {
                js("ExternalStorage").releaseEmptyDirectory(parentFile, libraryRoot)
            }
            if (clmTruthy(inLibraryExternalRoot)) {
                js("ExternalStorage").releaseEmptyDirectory(parentFile, libraryExternalRoot)
            }
        }
    }
    return clmUndefined()
}

private fun clmCleanLibrary(): dynamic {
    val base = clmNewBaseLibsPath()
    base.mkdirs()
    val libs = js("CA").settings.enabledLibrarys.concat(
        js("CA").settings.coreLibrarys, js("CA").settings.disabledLibrarys
    )
    val fl = base.listFiles()
    var i = 0
    while (i < clmLength(fl)) {
        if (!clmTruthy(fl[i].isFile())) {
            i++
            continue
        }
        val fn = clmString(fl[i].getAbsolutePath())
        if (clmGe(libs.indexOf(fn), 0)) {
            i++
            continue
        }
        if (clmLooseEq(fn.slice(-5), ".hash") && clmGe(libs.indexOf(fn.slice(0, -5)), 0)) {
            i++
            continue
        }
        fl[i].delete()
        i++
    }
    return clmUndefined()
}

private fun clmRequestUpdateUrlFromDefSrc(libApi: dynamic, uuid: dynamic): dynamic {
    val srcs = libApi.enabledSources()
    var i = 0
    // 按源的顺序找，第一个能给出更新地址的源胜出
    while (i < clmLength(srcs)) {
        val source = libApi.requestSourceInfoCached(srcs[i].url)
        if (clmTruthy(source)) {
            val map = libApi.requestSourceMap(source)
            if (clmTruthy(map) && clmTruthy(map[uuid])) return map[uuid]
        }
        i++
    }
    return clmUndefined()
}

private fun clmRequestUpdateInfo(
    libApi: dynamic, libinfo: dynamic, callback: dynamic
): dynamic {
    val u = libinfo.update
    var r: dynamic = clmUndefined()
    try {
        if (clmLooseEq(clmTypeof(u), "function")) {
            r = libinfo.update()
        } else if (clmLooseEq(clmTypeof(u), "string") &&
            (u.startsWith("http://") || u.startsWith("https://"))
        ) {
            r = clmJsonParse(js("NetworkUtils").queryPage(u))
        } else {
            val t = libApi.requestUpdateUrlFromDefSrc(libinfo.uuid)
            if (clmTruthy(t)) r = clmJsonParse(js("NetworkUtils").queryPage(t))
        }
        if (!clmInstanceOf(r, js("Object")) || !clmIsArray(r.version)) {
            callback(-1)
            return clmUndefined()
        }
    } catch (e: dynamic) {
        callback(-2, e)
        return clmUndefined()
    }
    callback(
        if (clmGt(js("NeteaseAdapter").compareVersion(r.version, libinfo.version), 0)) 1 else 0,
        r,
        libinfo
    )
    return clmUndefined()
}

private fun clmDoUpdate(
    libApi: dynamic, updateInfo: dynamic, libInfo: dynamic, statusListener: dynamic
): dynamic {
    if (clmLooseEq(updateInfo.method, "intent")) { //通过链接启动
        statusListener("downloadFromUri", clmString(updateInfo.uri))
    } else {
        statusListener("startDownload")
        try {
            if (clmTruthy(updateInfo.source)) {
                val info = clmObject()
                info.downloadurl = updateInfo.url
                info.sha1 = updateInfo.sha1
                info.uuid = updateInfo.uuid
                val uriStr = clmString(
                    libApi.downloadLib(
                        info, libApi.requestSourceInfoCached(updateInfo.source)
                    )
                )
                if (clmLooseNe(uriStr, libInfo.src)) {
                    js("ExternalStorage").tryReleaseImportUri(
                        js("ExternalStorage").toUri(libInfo.src)
                    )
                    if (clmTruthy(js("Common").inSet(js("CA").settings.coreLibrarys, libInfo.src))) {
                        js("Common").replaceLinkedSet(
                            js("CA").settings.coreLibrarys, libInfo.src, uriStr
                        )
                        js("Common").removeSet(js("CA").settings.enabledLibrarys, uriStr)
                    } else {
                        js("Common").replaceLinkedSet(
                            js("CA").settings.enabledLibrarys, libInfo.src, uriStr
                        )
                        js("Common").removeSet(js("CA").settings.coreLibrarys, uriStr)
                    }
                    if (clmLooseEq(libInfo.mode, 0)) {
                        js("Common").addSet(js("CA").settings.disabledLibrarys, libInfo.src)
                    }
                }
            } else {
                js("NetworkUtils").downloadToUri(
                    updateInfo.url, js("ExternalStorage").toUri(libInfo.src)
                )
            }
        } catch (e: dynamic) {
            statusListener("downloadError", e)
        }
        statusListener("completeDownload", updateInfo.message)
    }
    return clmUndefined()
}

private fun clmUpdateLibraries(libApi: dynamic, level: dynamic): dynamic {
    val fUpdate = clmLooseEq(level, 2)
    var updateCount: dynamic = 0
    if (clmLe(level, 0)) return 0
    js("CA").IntelliSense.library.info.forEach({ e: dynamic ->
        if (clmTruthy(e.installer)) {
            // 安装器包不参与更新检查
            js("undefined")
        } else {
            e.updateState = "checking"
            js("Threads").awaitDefault({
                try {
                    libApi.requestUpdateInfo(
                        e,
                        { statusCode: dynamic, arg1: dynamic, arg2: dynamic ->
                            if (clmLooseEq(statusCode, 1)) {
                                e.updateInfo = arg1
                                e.updateState = "ready"
                                updateCount = clmAdd(updateCount, 1)
                                if (fUpdate) {
                                    libApi.clearCache(e.src)
                                    libApi.doUpdate(arg1, arg2, { statusMessage: dynamic ->
                                        if (clmLooseEq(statusMessage, "downloadFromUri")) {
                                            e.updateState = "waitForUser"
                                        } else if (clmLooseEq(statusMessage, "downloadError")) {
                                            e.updateState = "error"
                                        } else if (clmLooseEq(
                                                statusMessage, "completeDownload"
                                            )
                                        ) {
                                            e.updateState = "finished"
                                        }
                                        js("undefined")
                                    })
                                }
                            } else if (clmLooseEq(statusCode, 0)) {
                                e.updateState = "latest"
                            } else if (clmLt(statusCode, 0)) {
                                e.updateState = "unavailable"
                            }
                            js("undefined")
                        }
                    )
                } catch (e: dynamic) {
                    js("erp")(e)
                }
                js("undefined")
            }, 5000)
        }
        js("undefined")
    })
    return updateCount
}

private fun clmVersionToString(v: dynamic): dynamic {
    if (!clmIsArray(v)) return clmString(v)
    // 快照 / 安装器包的版本是 [年, 月, 日, 时, 分, 秒]，
    // 时、分、秒补成两位，列表里不至于出现 21.5.8 这种参差的对齐
    if (clmLength(v) == 6 && clmGe(v[0], 2000)) {
        return clmArrayOf(
            v[0], v[1], v[2], clmPad2(v[3]), clmPad2(v[4]), clmPad2(v[5])
        ).join(".")
    }
    return v.join(".")
}

private fun clmPad2(n: dynamic): dynamic = clmString(clmAdd("0", n)).slice(-2)
