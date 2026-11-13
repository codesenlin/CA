package ca.kotlin.android

import ca.kotlin.core.*


/**
 * modules/utils/ExternalStorage.js 的 Kotlin/JS 版本。
 *
 * 保持原模块的字段（StorageRoot / AppFilesRoot / ImportFilesRoot / TempFilesRoot）
 * 和全部公开方法；所有方法通过闭包里的 esApi 调用，不依赖 JS 的 this。
 *
 * Java 数组长度一律经 esLength() 走 ToInt32（Rhino 下 length 不保证是原始 number）；
 * 需要传参的 Java 构造/调用都走 JS helper，避免在 js("...") 里引用 Kotlin 局部变量。
 */
private val esApi: dynamic = js("({})")

private val esTruthyJs: dynamic = js("function (v) { return !!v; }")
private val esLengthJs: dynamic = js("function (v) { return v.length | 0; }")
private val esStringJs: dynamic = js("function (v) { return String(v); }")
private val esTypeJs: dynamic = js("function (v) { return typeof v; }")
private val esToIntJs: dynamic = js("function (v) { return Number(v) | 0; }")
private val esStrictEqJs: dynamic = js("function (a, b) { return a === b; }")
private val esIsFileJs: dynamic = js("function (v) { return v instanceof java.io.File; }")
private val esIsUriJs: dynamic = js("function (v) { return v instanceof android.net.Uri; }")
private val esIsArrayJs: dynamic = js("function (v) { return Array.isArray(v); }")
private val esIsFunctionJs: dynamic = js("function (v) { return typeof v === 'function'; }")
private val esIsCharSequenceJs: dynamic = js("function (v) { return v instanceof java.lang.CharSequence; }")
private val esStartsWithJs: dynamic = js("function (s, p) { return s.startsWith(p); }")
private val esOrJs: dynamic = js("function (a, b) { return a | b; }")
private val esDpJs: dynamic = js("function (n) { return n * G.dp; }")
private val esNewFile1Js: dynamic = js("function (a) { return new java.io.File(a); }")
private val esNewFile2Js: dynamic = js("function (a, b) { return new java.io.File(a, b); }")
private val esNewFileOutputStreamJs: dynamic = js("function (f) { return new java.io.FileOutputStream(f); }")
private val esNewString1Js: dynamic = js("function (v) { return new java.lang.String(v); }")
private val esNewString2Js: dynamic = js("function (b, e) { return new java.lang.String(b, e); }")
private val esNewDateLocaleJs: dynamic = js("function (v) { return new Date(v).toLocaleString(); }")
private val esReadableUriJs: dynamic = js("function (u) { return decodeURIComponent(String(u).replace(/%2(5|F)/g, encodeURIComponent)); }")
private val esReplaceStarsJs: dynamic = js("function (h, i) { return h.replace(/\\*/g, i); }")
private val esReplaceDoubleStarJs: dynamic = js("function (h, n) { return h.replace('**', n); }")
private val esNewArrayJs: dynamic = js("function (n) { return new Array(n); }")
private val esNewErrorJs: dynamic = js("function (m) { return new Error(m); }")

// UI 构造 helper：Kotlin 侧不能对 dynamic 使用 new，统一从这里传参。
private val esNewTextViewJs: dynamic = js("function (c) { return new G.TextView(c); }")
private val esNewLinearLayoutJs: dynamic = js("function (c) { return new G.LinearLayout(c); }")
private val esNewLinearLayoutParams2Js: dynamic = js("function (w, h) { return new G.LinearLayout.LayoutParams(w, h); }")
private val esNewLinearLayoutParams3Js: dynamic = js("function (w, h, wt) { return new G.LinearLayout.LayoutParams(w, h, wt); }")
private val esNewAbsListViewParamsJs: dynamic = js("function (w, h) { return new G.AbsListView.LayoutParams(w, h); }")
private val esNewViewClickListenerJs: dynamic = js("function (l) { return new G.View.OnClickListener(l); }")
private val esNewItemClickListenerJs: dynamic = js("function (l) { return new G.AdapterView.OnItemClickListener(l); }")
private val esNewItemLongClickListenerJs: dynamic = js("function (l) { return new G.AdapterView.OnItemLongClickListener(l); }")
private val esNewListViewJs: dynamic = js("function (c) { return new G.ListView(c); }")
private val esNewEditTextJs: dynamic = js("function (c) { return new G.EditText(c); }")
private val esNewSimpleListAdapterJs: dynamic = js("function (a, b, c, d, e) { return new SimpleListAdapter(a, b, c, d, e); }")
private val esGetListAdapterControllerJs: dynamic = js("function (a) { return SimpleListAdapter.getController(a); }")
private val esNewPopupPageJs: dynamic = js("function (v, n) { return new PopupPage(v, n); }")

private fun esTruthy(v: dynamic): Boolean = esTruthyJs(v)
private fun esLength(v: dynamic): Int = esLengthJs(v)
private fun esString(v: dynamic): dynamic = esStringJs(v)
private fun esType(v: dynamic): dynamic = esTypeJs(v)
private fun esToInt(v: dynamic): Int = esToIntJs(v)
private fun esStrictEq(a: dynamic, b: dynamic): Boolean = esStrictEqJs(a, b)
private fun esIsFile(v: dynamic): Boolean = esIsFileJs(v)
private fun esIsUri(v: dynamic): Boolean = esIsUriJs(v)
private fun esIsArray(v: dynamic): Boolean = esIsArrayJs(v)
private fun esIsFunction(v: dynamic): Boolean = esIsFunctionJs(v)
private fun esIsCharSequence(v: dynamic): Boolean = esIsCharSequenceJs(v)
private fun esDp(n: Int): dynamic = esDpJs(n)

private fun esEmptyArray(): dynamic = js("[]")
private fun esEmptyObject(): dynamic = js("({})")
private fun esSingleArray(v: dynamic): dynamic {
    val ret = esEmptyArray()
    ret.push(v)
    return ret
}

private fun esIsMimeDir(mimeType: dynamic): Boolean =
    js("android.provider.DocumentsContract.Document.MIME_TYPE_DIR").equals(mimeType)

private fun esViewClickListener(handler: dynamic): dynamic {
    val listener = esEmptyObject()
    listener.onClick = handler
    return esNewViewClickListenerJs(listener)
}

private fun esItemClickListener(handler: dynamic): dynamic {
    val listener = esEmptyObject()
    listener.onItemClick = handler
    return esNewItemClickListenerJs(listener)
}

private fun esItemLongClickListener(handler: dynamic): dynamic {
    val listener = esEmptyObject()
    listener.onItemLongClick = handler
    return esNewItemLongClickListenerJs(listener)
}

private fun esOnCreate() {
    esApi.StorageRoot = esApi.getExternalStorageRoot()
    esApi.AppFilesRoot = esApi.getAppSpecificDirectory(null)
    esApi.ImportFilesRoot = esApi.getAppSpecificDirectory("import")
    esApi.TempFilesRoot = esApi.getAppSpecificDirectory("temp")
    esApi.AppFilesRoot.mkdirs()
}

private fun esInitialize() {
    esApi.cleanTempFiles()
}

private fun esGetStorageAccessLevel(): dynamic {
    if (js("ScriptInterface").isAndroidSdkAtLeast(30)) { // Android 11 (R)
        if (esTruthy(js("android.os.Environment").isExternalStorageManager())) {
            return "storage"
        }
    }
    if (js("ScriptInterface").isAndroidSdkAtLeast(29)) { // Android 10 (Q)
        if (esTruthy(js("android.os.Environment").isExternalStorageLegacy())) {
            val canReadStorage = js("AndroidBridge").requestPermissions(
                js("['android.permission.READ_EXTERNAL_STORAGE']"), "", null, 3
            ) == 0
            if (canReadStorage) {
                return "storage"
            }
        }
        return "self"
    }
    val canAccessStorage = js("AndroidBridge").requestPermissions(
        js("""["android.permission.READ_EXTERNAL_STORAGE","android.permission.WRITE_EXTERNAL_STORAGE"]"""),
        "", null, 3
    ) == 0
    return if (canAccessStorage) "storage" else "self"
}

private fun esGetExternalStorageRoot(): dynamic {
    if (js("ScriptInterface").isAndroidSdkAtLeast(30)) {
        val storageManager = js("ctx").getSystemService(js("ctx").STORAGE_SERVICE)
        val primaryVolume = storageManager.getPrimaryStorageVolume()
        return primaryVolume.getDirectory()
    }
    return js("android.os.Environment").getExternalStorageDirectory()
}

private fun esGetAppSpecificDirectory(name: dynamic): dynamic {
    val external = js("ctx").getExternalFilesDir(name)
    return if (external != null) external else js("ctx").getDir(name, 0)
}

private fun esGetAppSpecificCacheDirectory(): dynamic {
    val external = js("ctx").getExternalCacheDir()
    return if (external != null) external else js("ctx").getCacheDir()
}

private fun esGetAccessibleRoot(): dynamic {
    val accessLevel = esApi.getStorageAccessLevel()
    if (accessLevel == "storage" && esTruthy(esApi.StorageRoot)) {
        return esApi.StorageRoot
    }
    return esApi.AppFilesRoot
}

private fun esIsParentOrSelf(file: dynamic, parent: dynamic): Boolean {
    val filePath = if (esIsFile(file)) file.getCanonicalPath() else file
    val parentPath = if (esIsFile(parent)) parent.getCanonicalPath() else parent
    return parentPath == filePath || esStartsWithJs(filePath, parentPath + js("java.io.File").separator)
}

private fun esIsInStorage(path: dynamic): Boolean {
    if (!esTruthy(esApi.StorageRoot)) return false
    return esApi.isParentOrSelf(path, esApi.StorageRoot)
}

private fun esIsAccessible(path: dynamic): Boolean {
    return esApi.isParentOrSelf(path, esApi.getAccessibleRoot())
}

private fun esReleaseEmptyDirectory(file: dynamic, root: dynamic) {
    val children = file.list()
    if (children != null && esLength(children) == 0) {
        val parentFile = file.getParentFile()
        if (esApi.isParentOrSelf(parentFile, root)) {
            if (esTruthy(file.delete())) {
                // 原模块这里漏传了 root，保持同样的回退行为。
                esApi.releaseEmptyDirectory(parentFile)
            }
        }
    }
}

private fun esAfterEnsureConfirm(callback: dynamic, id: dynamic) {
    if (id != 0) return
    if (js("ScriptInterface").isAndroidSdkAtLeast(30)) {
        esApi.requestManageStoragePermission(callback)
    } else {
        esApi.requestStoragePermission(callback)
    }
}

private fun esEnsureExternalStorage(callback: dynamic) {
    val options = esEmptyObject()
    options.title = "权限请求"
    options.description = "访问此部分数据需要申请额外的权限，是否继续？"
    options.callback = { id: dynamic -> esAfterEnsureConfirm(callback, id) }
    js("Common").showConfirmDialog(options)
}

private fun esAfterStoragePermission(callback: dynamic, success: dynamic, sync: dynamic) {
    if (!esTruthy(sync)) {
        if (esLength(success) > 0) {
            js("Common").toast("权限请求成功")
            if (esTruthy(callback)) callback()
        }
    }
}

private fun esRequestStoragePermission(callback: dynamic) {
    val group = esEmptyObject()
    group.permissions = js("""["android.permission.READ_EXTERNAL_STORAGE","android.permission.WRITE_EXTERNAL_STORAGE"]""")
    group.explanation = "读取内部存储\n写入内部存储\n\n这些权限将用于读写命令库、编辑JSON、记录错误日志等"
    group.callback = { _flag: dynamic, success: dynamic, _denied: dynamic, sync: dynamic ->
        esAfterStoragePermission(callback, success, sync)
    }
    group.mode = 2
    val groups = esEmptyArray()
    groups.push(group)
    js("AndroidBridge").requestPermissionsByGroup(groups)
}

private fun esRunManageStorageTask(activity: dynamic, intent: dynamic, callback: dynamic): dynamic {
    var paused = false
    var granted = false
    fun checkPermission() {
        if (granted) return
        if (esTruthy(js("android.os.Environment").isExternalStorageManager())) {
            granted = true
            js("Common").toast("权限请求成功")
            if (esTruthy(callback)) callback()
        }
    }
    val task = esEmptyObject()
    task.onPause = {
        try {
            paused = true
        } catch (e: dynamic) { js("erp")(e) }
    }
    task.onResume = {
        try {
            if (paused) {
                checkPermission()
                activity.finish()
            }
        } catch (e: dynamic) { js("erp")(e) }
    }
    task.onDestroy = {
        try {
            checkPermission()
            js("PopupPage").show()
        } catch (e: dynamic) { js("erp")(e) }
    }
    try {
        activity.startActivity(intent)
        return task
    } catch (err: dynamic) {
        js("Log").e(err)
        js("Common").toast("调用外部应用失败，请检查您是否授予了命令助手后台弹出界面或类似的权限\n" + err)
        activity.finish()
        return js("undefined")
    }
}

private fun esRequestManageStoragePermission(callback: dynamic) {
    val intent = js("new android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)")
    intent.setData(js("android.net.Uri").parse("package:" + js("ctx").getPackageName()))
    js("AndroidBridge").beginForegroundTask("requestManageStoragePermission", { activity: dynamic ->
        esRunManageStorageTask(activity, intent, callback)
    })
}

private fun esToUri(obj: dynamic): dynamic {
    if (esIsUri(obj)) {
        return obj
    }
    if (esIsFile(obj)) {
        return js("android.net.Uri").fromFile(obj)
    }
    val uri = js("android.net.Uri").parse(esString(obj))
    if (esTruthy(uri.isAbsolute())) {
        return uri
    }
    val path = uri.path
    return js("android.net.Uri").fromFile(esNewFile1Js(if (esTruthy(path)) path else ""))
}

private fun esUriToFile(uri: dynamic): dynamic {
    if (uri.getScheme() == "file") {
        return esNewFile1Js(uri.getPath())
    }
    return null
}

private fun esGetUriType(uri: dynamic): dynamic {
    if (esTruthy(js("android.provider.DocumentsContract").isDocumentUri(js("ctx"), uri))) {
        return "documentsProvider"
    }
    return uri.getScheme()
}

private fun esQueryForUri(uri: dynamic, column: dynamic, columnType: dynamic, defaultValue: dynamic): dynamic {
    val resolver = js("ctx").getContentResolver()
    var cursor: dynamic = js("undefined")
    var r: dynamic = defaultValue
    try {
        val projection = esEmptyArray()
        projection.push(js("android.provider.DocumentsContract.Document")[column])
        cursor = resolver.query(uri, projection, null, null, null)
        if (!esTruthy(cursor)) throw esNewErrorJs("Uri is not available for query")
        if (esTruthy(cursor.moveToFirst()) && !esTruthy(cursor.isNull(0))) {
            if (columnType == "string") {
                r = esString(cursor.getString(0))
            } else if (columnType == "integer") {
                r = cursor.getLong(0)
            } else if (columnType == "float") {
                r = cursor.getDouble(0)
            } else {
                r = cursor.getBlob(0)
            }
        }
    } catch (e: dynamic) { js("Log").e(e) }
    if (cursor != null) cursor.close()
    return r
}

private fun esExists(uri: dynamic): dynamic {
    if (uri.getScheme() == "file") {
        return esApi.uriToFile(uri).exists()
    } else if (esTruthy(js("android.provider.DocumentsContract").isDocumentUri(js("ctx"), uri))) {
        return esApi.queryForUri(uri, "COLUMN_DOCUMENT_ID", "string", null) != null
    }
    return false
}

private fun esIsDirectory(uri: dynamic): dynamic {
    if (uri.getScheme() == "file") {
        return esApi.uriToFile(uri).isDirectory()
    } else if (esTruthy(js("android.provider.DocumentsContract").isDocumentUri(js("ctx"), uri))) {
        val mimeType = esApi.queryForUri(uri, "COLUMN_MIME_TYPE", "string", null)
        return esIsMimeDir(mimeType)
    }
    return false
}

private fun esIsFileUri(uri: dynamic): dynamic {
    if (uri.getScheme() == "file") {
        return esApi.uriToFile(uri).isFile()
    } else if (esTruthy(js("android.provider.DocumentsContract").isDocumentUri(js("ctx"), uri))) {
        val mimeType = esApi.queryForUri(uri, "COLUMN_MIME_TYPE", "string", null)
        return esTruthy(mimeType) && !esIsMimeDir(mimeType)
    }
    return false
}

private fun esCanRead(uri: dynamic): dynamic {
    if (uri.getScheme() == "file") {
        return esApi.uriToFile(uri).canRead()
    }
    return js("ctx").checkCallingOrSelfUriPermission(
        uri, js("android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION")
    ) == 0
}

private fun esCanWrite(uri: dynamic): dynamic {
    if (uri.getScheme() == "file") {
        return esApi.uriToFile(uri).canWrite()
    }
    return js("ctx").checkCallingOrSelfUriPermission(
        uri, js("android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION")
    ) == 0
}

private fun esGetName(uri: dynamic): dynamic {
    if (uri.getScheme() == "file") {
        return esString(esApi.uriToFile(uri).getName())
    } else if (esTruthy(js("android.provider.DocumentsContract").isDocumentUri(js("ctx"), uri))) {
        return esApi.queryForUri(uri, "COLUMN_DISPLAY_NAME", "string", null)
    }
    return null
}

private fun esGetMimeType(uri: dynamic): dynamic {
    if (esTruthy(js("android.provider.DocumentsContract").isDocumentUri(js("ctx"), uri))) {
        val mimeType = esApi.queryForUri(uri, "COLUMN_MIME_TYPE", "string", null)
        if (esIsMimeDir(mimeType)) {
            return null
        }
        return mimeType
    }
    return esApi.getMimeTypeFromExtension(esApi.getExtensionFromName(esApi.uriToName(uri)))
}

private fun esGetLastModified(uri: dynamic): dynamic {
    if (uri.getScheme() == "file") {
        return esApi.uriToFile(uri).lastModified()
    } else if (esTruthy(js("android.provider.DocumentsContract").isDocumentUri(js("ctx"), uri))) {
        return esApi.queryForUri(uri, "COLUMN_LAST_MODIFIED", "integer", null)
    }
    return null
}

private fun esGetLength(uri: dynamic): dynamic {
    if (uri.getScheme() == "file") {
        return esApi.uriToFile(uri).length()
    } else if (esTruthy(js("android.provider.DocumentsContract").isDocumentUri(js("ctx"), uri))) {
        return esApi.queryForUri(uri, "COLUMN_SIZE", "integer", null)
    }
    return null
}

private fun esDelete(uri: dynamic): dynamic {
    if (uri.getScheme() == "file") {
        return esApi.deleteTreeFile(esApi.uriToFile(uri))
    } else if (esTruthy(js("android.provider.DocumentsContract").isDocumentUri(js("ctx"), uri))) {
        try {
            val resolver = js("ctx").getContentResolver()
            return js("android.provider.DocumentsContract").deleteDocument(resolver, uri)
        } catch (e: dynamic) { js("Log").e(e) }
    }
    return false
}

private fun esDeleteTreeFile(file: dynamic): dynamic {
    if (esTruthy(file.isDirectory())) {
        val children = file.listFiles()
        if (children != null) {
            var i = 0
            val n = esLength(children)
            while (i < n) {
                esApi.deleteTreeFile(children[i])
                i++
            }
        }
    }
    return file.delete()
}

private fun esRenameFile(uri: dynamic, newName: dynamic): dynamic {
    if (uri.getScheme() == "file") {
        val source = esApi.uriToFile(uri)
        val dest = esNewFile2Js(source.getParent(), newName)
        if (esTruthy(source.renameTo(dest))) {
            return esApi.toUri(dest)
        }
    } else if (esTruthy(js("android.provider.DocumentsContract").isDocumentUri(js("ctx"), uri))) {
        try {
            val resolver = js("ctx").getContentResolver()
            return js("android.provider.DocumentsContract").renameDocument(resolver, uri, newName)
        } catch (e: dynamic) { js("Log").e(e) }
    }
    return null
}

private fun esListFiles(uri: dynamic): dynamic {
    if (uri.getScheme() == "file") {
        val files = esApi.uriToFile(uri).listFiles()
        if (files != null) {
            val r = esEmptyArray()
            var i = 0
            val n = esLength(files)
            while (i < n) {
                r.push(esApi.toUri(files[i]))
                i++
            }
            return r
        }
    } else if (esTruthy(js("android.provider.DocumentsContract").isDocumentUri(js("ctx"), uri))) {
        val resolver = js("ctx").getContentResolver()
        val parentId = js("android.provider.DocumentsContract").getDocumentId(uri)
        val childrenUri = js("android.provider.DocumentsContract").buildChildDocumentsUriUsingTree(uri, parentId)
        var cursor: dynamic = js("undefined")
        val r = esEmptyArray()
        var success = false
        try {
            val projection = esEmptyArray()
            projection.push(js("android.provider.DocumentsContract.Document").COLUMN_DOCUMENT_ID)
            cursor = resolver.query(childrenUri, projection, null, null, null)
            if (!esTruthy(cursor)) throw esNewErrorJs("Uri is not available for query")
            while (cursor.moveToNext()) {
                val childId = cursor.getString(0)
                val childUri = js("android.provider.DocumentsContract").buildDocumentUriUsingTree(uri, childId)
                r.push(childUri)
            }
            success = true
        } catch (e: dynamic) { js("Log").e(e) }
        if (cursor != null) cursor.close()
        if (success) {
            return r
        }
    }
    return null
}

private fun esListFilesWithDetails(uri: dynamic): dynamic {
    if (uri.getScheme() == "file") {
        val files = esApi.uriToFile(uri).listFiles()
        if (files != null) {
            val r = esEmptyArray()
            var i = 0
            val n = esLength(files)
            while (i < n) {
                val file = files[i]
                val entry = esEmptyObject()
                entry.uri = esApi.toUri(file)
                entry.name = file.getName()
                entry.directory = file.isDirectory()
                entry.lastModified = file.lastModified()
                entry.length = file.length()
                r.push(entry)
                i++
            }
            return r
        }
    } else if (esTruthy(js("android.provider.DocumentsContract").isDocumentUri(js("ctx"), uri))) {
        val resolver = js("ctx").getContentResolver()
        val parentId = js("android.provider.DocumentsContract").getDocumentId(uri)
        val childrenUri = js("android.provider.DocumentsContract").buildChildDocumentsUriUsingTree(uri, parentId)
        var cursor: dynamic = js("undefined")
        val r = esEmptyArray()
        var success = false
        try {
            val projection = esEmptyArray()
            val doc = js("android.provider.DocumentsContract.Document")
            projection.push(doc.COLUMN_DOCUMENT_ID)
            projection.push(doc.COLUMN_MIME_TYPE)
            projection.push(doc.COLUMN_DISPLAY_NAME)
            projection.push(doc.COLUMN_LAST_MODIFIED)
            projection.push(doc.COLUMN_SIZE)
            cursor = resolver.query(childrenUri, projection, null, null, null)
            val docIdColumn = cursor.getColumnIndexOrThrow(doc.COLUMN_DOCUMENT_ID)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(doc.COLUMN_MIME_TYPE)
            val nameColumn = cursor.getColumnIndexOrThrow(doc.COLUMN_DISPLAY_NAME)
            val mtimeColumn = cursor.getColumnIndexOrThrow(doc.COLUMN_LAST_MODIFIED)
            val sizeColumn = cursor.getColumnIndexOrThrow(doc.COLUMN_SIZE)
            while (cursor.moveToNext()) {
                val childId = cursor.getString(docIdColumn)
                val childMimeType = cursor.getString(mimeTypeColumn)
                val childUri = js("android.provider.DocumentsContract").buildDocumentUriUsingTree(uri, childId)
                val entry = esEmptyObject()
                entry.uri = childUri
                entry.mimeType = esString(childMimeType)
                entry.name = esString(cursor.getString(nameColumn))
                entry.directory = childMimeType == doc.MIME_TYPE_DIR
                entry.lastModified = cursor.getLong(mtimeColumn)
                entry.length = cursor.getLong(sizeColumn)
                r.push(entry)
            }
            success = true
        } catch (e: dynamic) { js("Log").e(e) }
        if (cursor != null) cursor.close()
        if (success) {
            return r
        }
    }
    return null
}

private fun esGetParentDirectory(uri: dynamic): dynamic {
    if (uri.getScheme() == "file") {
        val parentFile = esApi.uriToFile(uri).getParentFile()
        if (!esTruthy(esApi.StorageRoot) || esApi.isParentOrSelf(parentFile, esApi.StorageRoot)) {
            return esApi.toUri(parentFile)
        }
    } else if (esTruthy(js("android.provider.DocumentsContract").isDocumentUri(js("ctx"), uri))) {
        if (js("ScriptInterface").isAndroidSdkAtLeast(26)) {
            try {
                val resolver = js("ctx").getContentResolver()
                val path = js("android.provider.DocumentsContract").findDocumentPath(resolver, uri)
                if (esTruthy(path)) {
                    val segments = path.getPath()
                    val n = esLength(segments)
                    if (n > 1) {
                        return js("android.provider.DocumentsContract").buildDocumentUriUsingTree(uri, segments[n - 2])
                    }
                }
            } catch (e: dynamic) { js("Log").e(e) }
        }
    }
    return null
}

private fun esGetChildFile(uri: dynamic, name: dynamic): dynamic {
    if (uri.getScheme() == "file") {
        val parentFile = esApi.uriToFile(uri)
        val childFile = esNewFile2Js(parentFile, name)
        if (esTruthy(childFile.exists())) {
            return esApi.toUri(childFile)
        }
    } else {
        val children = esApi.listFilesWithDetails(uri)
        if (children != null) {
            var i = 0
            val n = esLength(children)
            while (i < n) {
                val child = children[i]
                if (child.name == name) return child.uri
                i++
            }
        }
    }
    return null
}

private fun esCreateFile(uri: dynamic, name: dynamic, mimeType: dynamic): dynamic {
    if (uri.getScheme() == "file") {
        val directory = esApi.uriToFile(uri)
        val newFile = esNewFile2Js(directory, name)
        if (esTruthy(newFile.createNewFile())) {
            return esApi.toUri(newFile)
        }
    } else if (esTruthy(js("android.provider.DocumentsContract").isDocumentUri(js("ctx"), uri))) {
        try {
            val resolver = js("ctx").getContentResolver()
            return js("android.provider.DocumentsContract").createDocument(resolver, uri, mimeType, name)
        } catch (e: dynamic) { js("Log").e(e) }
    }
    return null
}

private fun esCreateDirectory(uri: dynamic, name: dynamic): dynamic {
    if (uri.getScheme() == "file") {
        val directory = esApi.uriToFile(uri)
        val newDirectory = esNewFile2Js(directory, name)
        if (esTruthy(newDirectory.mkdir())) {
            return esApi.toUri(newDirectory)
        }
    } else if (esTruthy(js("android.provider.DocumentsContract").isDocumentUri(js("ctx"), uri))) {
        try {
            val resolver = js("ctx").getContentResolver()
            val mimeType = js("android.provider.DocumentsContract.Document.MIME_TYPE_DIR")
            return js("android.provider.DocumentsContract").createDocument(resolver, uri, mimeType, name)
        } catch (e: dynamic) { js("Log").e(e) }
    }
    return null
}

private fun esUriToName(uri: dynamic): dynamic {
    val name = esApi.getName(uri)
    if (name != null) return name
    val lastSegment = esString(uri.getLastPathSegment())
    return if (esTruthy(lastSegment)) lastSegment else esString(uri)
}

private fun esUriToReadablePath(uri: dynamic): dynamic {
    val file = esApi.uriToFile(uri)
    if (file != null) {
        return esString(file.getPath())
    }
    return esReadableUriJs(uri)
}

private fun esGetExtensionFromName(name: dynamic): dynamic {
    val dot = name.lastIndexOf(".")
    if (dot >= 1) {
        return name.slice(dot + 1)
    }
    return ""
}

private fun esGetMimeTypeFromExtension(extension: dynamic): dynamic {
    val mimeTypeMap = js("android.webkit.MimeTypeMap").getSingleton()
    val mimeType = mimeTypeMap.getMimeTypeFromExtension(extension)
    if (esTruthy(mimeType)) return esString(mimeType)
    return null
}

private fun esGetLengthString(uri: dynamic, longer: dynamic): dynamic {
    val length = esApi.getLength(uri)
    if (esTruthy(longer)) {
        return esString(js("android.text.format.Formatter").formatFileSize(js("ctx"), length))
    }
    return esString(js("android.text.format.Formatter").formatShortFileSize(js("ctx"), length))
}

private fun esGetLastModifiedString(uri: dynamic): dynamic {
    return esNewDateLocaleJs(esApi.getLastModified(uri))
}

private fun esOpenInputStream(uri: dynamic): dynamic {
    val resolver = js("ctx").getContentResolver()
    return resolver.openInputStream(uri)
}

private fun esOpenOutputStream(uri: dynamic, mode: dynamic): dynamic {
    val resolver = js("ctx").getContentResolver()
    if (esTruthy(mode)) {
        return resolver.openOutputStream(uri, mode)
    }
    return resolver.openOutputStream(uri, "wt")
}

private fun esPipe(input: dynamic, output: dynamic, bufferSize: dynamic) {
    val buffer = JavaReflect.array("byte", if (esTruthy(bufferSize)) bufferSize else 2048)
    var readBytes = input.read(buffer)
    while (readBytes > 0) {
        output.write(buffer, 0, readBytes)
        readBytes = input.read(buffer)
    }
}

private fun esCopy(sourceUri: dynamic, destUri: dynamic) {
    val input = esApi.openInputStream(sourceUri)
    val output = esApi.openOutputStream(destUri, "wt")
    esApi.pipe(input, output)
    input.close()
    output.close()
}

private fun esReadFileContent(uri: dynamic, encoding: dynamic, defaultValue: dynamic): dynamic {
    try {
        val input = esApi.openInputStream(uri)
        val output = js("new java.io.ByteArrayOutputStream()")
        esApi.pipe(input, output)
        input.close()
        if (esTruthy(encoding)) {
            return esNewString2Js(output.toByteArray(), encoding)
        }
        return output.toByteArray()
    } catch (e: dynamic) {
        return if (esIsFunction(defaultValue)) defaultValue(e) else defaultValue
    }
}

private fun esWriteFileContent(uri: dynamic, content: dynamic, encoding: dynamic, append: dynamic) {
    val output = esApi.openOutputStream(uri, if (esTruthy(append)) "wa" else "wt")
    if (esTruthy(encoding) || esType(content) == "string" || esIsCharSequence(content)) {
        val str = esNewString1Js(content)
        val bytes = str.getBytes(if (esTruthy(encoding)) encoding else "UTF-8")
        output.write(bytes)
    } else {
        output.write(content)
    }
    output.close()
}

private fun esCreateImportFile(hint: dynamic): dynamic {
    esApi.ImportFilesRoot.mkdirs()
    var h = hint
    if (!esTruthy(h)) h = "*"
    var importFile: dynamic = js("undefined")
    if (h.indexOf("*") >= 0) {
        var index = 1
        while (index < 10000) {
            importFile = esNewFile2Js(esApi.ImportFilesRoot, esReplaceStarsJs(h, index))
            if (!esTruthy(importFile.exists())) break
            index++
        }
    } else {
        importFile = esNewFile2Js(esApi.ImportFilesRoot, h)
    }
    return importFile
}

private fun esTryReleaseImportFile(fileOrPath: dynamic): dynamic {
    val file = esNewFile1Js(fileOrPath)
    if (esApi.isParentOrSelf(file, esApi.ImportFilesRoot)) {
        file.delete()
        esApi.releaseEmptyDirectory(file.getParentFile(), esApi.ImportFilesRoot)
        return true
    }
    return false
}

private fun esTryReleaseImportUri(uri: dynamic): dynamic {
    val importFile = esApi.uriToFile(uri)
    if (importFile != null) {
        return esApi.tryReleaseImportFile(importFile)
    }
    return false
}

private fun esImportFile(uri: dynamic, hint: dynamic): dynamic {
    var h = hint
    if (esTruthy(h)) h = esReplaceDoubleStarJs(h, esApi.uriToName(uri))
    val importFile = esApi.createImportFile(h)
    val input = esApi.openInputStream(uri)
    importFile.getParentFile().mkdirs()
    val output = esNewFileOutputStreamJs(importFile)
    esApi.pipe(input, output)
    input.close()
    output.close()
    return importFile
}

private fun esCreateTempFile(hint: dynamic): dynamic {
    esApi.TempFilesRoot.mkdirs()
    var h = hint
    if (!esTruthy(h)) h = "*"
    var tempFile: dynamic = js("undefined")
    if (h.indexOf("*") >= 0) {
        var index = 1
        while (index < 10000) {
            tempFile = esNewFile2Js(esApi.TempFilesRoot, esReplaceStarsJs(h, index))
            if (!esTruthy(tempFile.exists())) break
            index++
        }
    } else {
        tempFile = esNewFile2Js(esApi.TempFilesRoot, h)
    }
    return tempFile
}

private fun esCleanTempFiles() {
    esApi.deleteTreeFile(esApi.TempFilesRoot)
}

private fun esSelectContentResult(callback: dynamic, resultCode: dynamic, data: dynamic) {
    if (resultCode != -1) return // RESULT_OK
    callback(data.getData())
}

private fun esSelectContent(mimeType: dynamic, callback: dynamic) {
    var intent = js("new android.content.Intent(android.content.Intent.ACTION_GET_CONTENT)")
    intent.setType(mimeType)
    intent.addCategory(js("android.content.Intent.CATEGORY_OPENABLE"))
    intent = js("android.content.Intent").createChooser(intent, "选择")
    js("AndroidBridge").startActivityForResult(intent, { resultCode: dynamic, data: dynamic ->
        esSelectContentResult(callback, resultCode, data)
    })
}

private fun esCreateDocumentResult(callback: dynamic, resultCode: dynamic, data: dynamic) {
    if (resultCode != -1) return // RESULT_OK
    callback(data.getData())
}

private fun esCreateDocument(mimeType: dynamic, defaultTitle: dynamic, callback: dynamic, initialUri: dynamic) {
    val intent = js("new android.content.Intent(android.content.Intent.ACTION_CREATE_DOCUMENT)")
    intent.addCategory(js("android.content.Intent.CATEGORY_OPENABLE"))
    intent.addFlags(js("android.content.Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION"))
    intent.setType(mimeType)
    if (esTruthy(defaultTitle)) {
        intent.putExtra(js("android.content.Intent.EXTRA_TITLE"), esNewString1Js(defaultTitle))
    }
    if (esTruthy(initialUri)) {
        intent.putExtra(js("android.provider.DocumentsContract.EXTRA_INITIAL_URI"), initialUri)
    }
    js("AndroidBridge").startActivityForResult(intent, { resultCode: dynamic, data: dynamic ->
        esCreateDocumentResult(callback, resultCode, data)
    })
}

private fun esOpenDocumentResult(callback: dynamic, allowMultiple: dynamic, resultCode: dynamic, data: dynamic) {
    if (resultCode != -1) return // RESULT_OK
    val uri = data.getData()
    if (esTruthy(allowMultiple)) {
        val clipData = data.getClipData()
        if (esTruthy(clipData)) {
            val uriList = esEmptyArray()
            var i = 0
            val n = esToInt(clipData.getItemCount())
            while (i < n) {
                uriList.push(clipData.getItemAt(i).getUri())
                i++
            }
            callback(uriList)
        } else {
            callback(if (uri != null) esSingleArray(uri) else esEmptyArray())
        }
    } else {
        callback(uri)
    }
}

private fun esOpenDocument(mimeType: dynamic, callback: dynamic, initialUri: dynamic, allowMultiple: dynamic) {
    val intent = js("new android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT)")
    intent.addCategory(js("android.content.Intent.CATEGORY_OPENABLE"))
    intent.addFlags(js("android.content.Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION"))
    if (esIsArray(mimeType)) {
        val mimeTypeList = JavaReflect.array("string", esLength(mimeType))
        var i = 0
        val n = esLength(mimeType)
        while (i < n) {
            mimeTypeList[i] = mimeType[i]
            i++
        }
        intent.putExtra(js("android.content.Intent.EXTRA_MIME_TYPES"), mimeTypeList)
        intent.setType("*/*")
    } else {
        intent.setType(mimeType)
    }
    if (esTruthy(initialUri)) {
        intent.putExtra(js("android.provider.DocumentsContract.EXTRA_INITIAL_URI"), initialUri)
    }
    if (esTruthy(allowMultiple)) {
        intent.putExtra(js("android.content.Intent.EXTRA_ALLOW_MULTIPLE"), true)
    }
    js("AndroidBridge").startActivityForResult(intent, { resultCode: dynamic, data: dynamic ->
        esOpenDocumentResult(callback, allowMultiple, resultCode, data)
    })
}

private fun esOpenDocumentTreeResult(callback: dynamic, resultCode: dynamic, data: dynamic) {
    if (resultCode != -1) return // RESULT_OK
    val uri = data.getData()
    esApi.checkReleaseUriPermissions()
    esApi.tryTakeUriPermission(uri)
    val documentId = js("android.provider.DocumentsContract").getTreeDocumentId(uri)
    val documentUri = js("android.provider.DocumentsContract").buildDocumentUriUsingTree(uri, documentId)
    callback(documentUri, uri)
}

private fun esOpenDocumentTree(callback: dynamic, initialUri: dynamic, showAdvanced: dynamic) {
    val intent = js("new android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT_TREE)")
    intent.addFlags(js("android.content.Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION"))
    if (esTruthy(initialUri)) {
        intent.putExtra(js("android.provider.DocumentsContract.EXTRA_INITIAL_URI"), initialUri)
    }
    if (esTruthy(showAdvanced)) {
        intent.putExtra("android.provider.extra.SHOW_ADVANCED", true)
        intent.putExtra("android.content.extra.SHOW_ADVANCED", true)
    }
    js("AndroidBridge").startActivityForResult(intent, { resultCode: dynamic, data: dynamic ->
        esOpenDocumentTreeResult(callback, resultCode, data)
    })
}

private fun esAfterDocumentTreeConfirm(documentUri: dynamic, callback: dynamic, id: dynamic) {
    if (id != 0) return
    esApi.openDocumentTree({ ->
        if (esTruthy(esApi.canRead(documentUri))) {
            if (esTruthy(callback)) callback(documentUri)
        } else {
            js("Common").toast("权限授予失败")
        }
    }, documentUri, true)
}

private fun esRequestDocumentTreeAccess(path: dynamic, callback: dynamic) {
    val authority = "com.android.externalstorage.documents"
    val documentId = "primary:" + path
    val treeUri = js("android.provider.DocumentsContract").buildTreeDocumentUri(authority, documentId)
    val documentUri = js("android.provider.DocumentsContract").buildDocumentUriUsingTree(treeUri, documentId)
    if (esTruthy(esApi.canRead(documentUri))) {
        esApi.tryTakeUriPermission(treeUri)
        if (esTruthy(callback)) callback(documentUri)
        return
    }
    val options = esEmptyObject()
    options.title = "目录访问权限"
    options.description = "为了获得访问对应目录的权限，请在之后弹出的界面中选择“使用此文件夹”。"
    options.buttons = js("""["同意","取消"]""")
    options.callback = { id: dynamic -> esAfterDocumentTreeConfirm(documentUri, callback, id) }
    js("Common").showConfirmDialog(options)
}

private fun esNeedAdditionalPermissions(uri: dynamic): dynamic {
    if (uri.getScheme() == "file") {
        val file = esApi.uriToFile(uri)
        if (esTruthy(esApi.isInStorage(file))) {
            if (esTruthy(esApi.isAccessible(file))) {
                val androidData = esNewFile2Js(esApi.StorageRoot, "Android/data")
                val androidObb = esNewFile2Js(esApi.StorageRoot, "Android/obb")
                if (esTruthy(esApi.isParentOrSelf(file, androidData))) {
                    return "androidData"
                }
                if (esTruthy(esApi.isParentOrSelf(file, androidObb))) {
                    return "androidObb"
                }
                if (esTruthy(file.canRead())) {
                    return null
                }
            } else {
                return "storage"
            }
        }
        return ""
    }
    return null
}

private fun esRequestAdditionalPermissions(type: dynamic, callback: dynamic) {
    if (type == "storage") {
        esApi.ensureExternalStorage({
            if (esTruthy(callback)) callback(esApi.toUri(esApi.getAccessibleRoot()))
        })
    } else if (type == "androidData" || type == "androidObb") {
        var requestDirectory: dynamic = if (type == "androidData") "Android/data" else "Android/obb"
        if (js("ScriptInterface").isAndroidSdkAtLeast(33)) { // Android 13 (Tiramisu)
            esApi.listAppExternalPath(requestDirectory, { pkg: dynamic ->
                requestDirectory = requestDirectory + "/" + pkg
                esApi.requestDocumentTreeAccess(requestDirectory, callback)
            })
        } else {
            esApi.requestDocumentTreeAccess(requestDirectory, callback)
        }
    }
}

private fun esFilterAppPackages(packages: dynamic, prefix: dynamic): dynamic {
    val filtered = esEmptyArray()
    var i = 0
    val n = esLength(packages)
    while (i < n) {
        val entry = packages[i]
        val directory = esNewFile2Js(esApi.StorageRoot, prefix + "/" + entry.packageName)
        entry.description = esString(directory.getCanonicalPath())
        if (esTruthy(directory.exists())) {
            filtered.push(entry)
        }
        i++
    }
    return filtered
}

private fun esListAppExternalPath(prefix: dynamic, callback: dynamic) {
    js("Common").showProgressDialog({ dialog: dynamic ->
        dialog.setText("正在加载列表……")
        val pm = js("ctx").getPackageManager()
        var packages = esEmptyArray()
        val applications = pm.getInstalledApplications(0)
        val applicationCount = esToInt(applications.size())
        var i = 0
        while (i < applicationCount) {
            dialog.setText("正在加载列表……\n应用: " + (i + 1) + " / " + applicationCount)
            val info = applications.get(i)
            val entry = esEmptyObject()
            entry.packageName = info.packageName
            entry.text = info.loadLabel(pm)
            packages.push(entry)
            i++
        }
        val mainIntent = js("new android.content.Intent(android.content.Intent.ACTION_MAIN)")
        mainIntent.addCategory(js("android.content.Intent.CATEGORY_LAUNCHER"))
        val launchables = pm.queryIntentActivities(mainIntent, 0)
        val launchableCount = esToInt(launchables.size())
        i = 0
        while (i < launchableCount) {
            dialog.setText("正在加载列表……\n主 Activity: " + (i + 1) + " / " + launchableCount)
            val info = launchables.get(i)
            val packageName = info.activityInfo.packageName
            var found = false
            var j = 0
            while (j < esLength(packages)) {
                if (packages[j].packageName == packageName) {
                    found = true
                    break
                }
                j++
            }
            if (!found) {
                val entry = esEmptyObject()
                entry.packageName = packageName
                entry.text = info.activityInfo.loadLabel(pm)
                packages.push(entry)
            }
            i++
        }
        packages = esFilterAppPackages(packages, prefix)
        packages.sort({ a: dynamic, b: dynamic -> js("Common").stringComparator(a.description, b.description) })
        dialog.close()
        if (!esTruthy(dialog.cancelled)) {
            js("Common").showListChooser(packages, { id: dynamic -> callback(packages[id].packageName) })
        }
    }, true)
}

private fun esIsPersistedUriPermission(uri: dynamic): dynamic {
    //  1 - uri 永久可用
    //  0 - uri 暂时可用，每次使用时需要 tryTakeUriPermission(uri) 刷新时间
    // -1 - uri 不可用
    if (uri.getScheme() == "file") {
        return if (esTruthy(esApi.uriToFile(uri).canRead())) 1 else -1
    }
    return js("ctx").checkCallingOrSelfUriPermission(
        uri, js("android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION")
    )
}

private fun esTryTakeUriPermission(uri: dynamic): dynamic {
    if (uri.getScheme() == "file") {
        return esApi.uriToFile(uri).canRead()
    }
    try {
        val resolver = js("ctx").getContentResolver()
        resolver.takePersistableUriPermission(uri, 0x3) // READ | WRITE
        return true
    } catch (e: dynamic) {
        js("Log").e(e)
        return false
    }
}

private fun esReleaseUriPermissions(uri: dynamic) {
    val resolver = js("ctx").getContentResolver()
    resolver.releasePersistableUriPermission(uri, 0x3)
}

private fun esReleaseAllUriPermissions() {
    val resolver = js("ctx").getContentResolver()
    val permissionList = resolver.getPersistedUriPermissions()
    var i = 0
    val n = esToInt(permissionList.size())
    while (i < n) {
        resolver.releasePersistableUriPermission(permissionList.get(i).getUri(), 0x3)
        i++
    }
}

private fun esCheckReleaseUriPermissions() {
    val resolver = js("ctx").getContentResolver()
    val permissionList = resolver.getPersistedUriPermissions()
    val permissions = esNewArrayJs(permissionList.size())
    var i = 0
    val n = esLength(permissions)
    while (i < n) {
        permissions[i] = permissionList.get(i)
        i++
    }
    if (esLength(permissions) > 100) {
        permissions.sort({ a: dynamic, b: dynamic -> b.getPersistedTime() - a.getPersistedTime() })
        val rest = permissions.slice(100)
        i = 0
        val restCount = esLength(rest)
        while (i < restCount) {
            resolver.releasePersistableUriPermission(rest[i].getUri(), 0x3)
            i++
        }
    }
}

private fun esImportActionsCallback(options: dynamic, uri: dynamic) {
    if (esTruthy(options.uri)) {
        if (esTruthy(options.allowMultiple)) {
            options.uri(if (esIsArray(uri)) uri else esSingleArray(uri))
        } else {
            if (esIsArray(uri)) {
                var i = 0
                val n = esLength(uri)
                while (i < n) {
                    options.uri(uri[i])
                    i++
                }
            } else {
                options.uri(uri)
            }
        }
    } else if (esTruthy(options.file)) {
        js("Common").showProgressDialog({ dialog: dynamic ->
            dialog.setTextDelayed("正在导入文件……", 200)
            val files: dynamic
            if (esIsArray(uri)) {
                val list = esEmptyArray()
                var i = 0
                val n = esLength(uri)
                while (i < n) {
                    list.push(esApi.importFile(uri[i], options.hint))
                    i++
                }
                files = list
            } else {
                files = esSingleArray(esApi.importFile(uri, options.hint))
            }
            dialog.close({
                if (esTruthy(options.allowMultiple)) {
                    options.file(files)
                } else {
                    var i = 0
                    val n = esLength(files)
                    while (i < n) {
                        options.file(files[i])
                        i++
                    }
                }
            })
        })
    }
}

private fun esShowImportActions(options: dynamic) {
    val importCallback = { uri: dynamic -> esImportActionsCallback(options, uri) }
    val actions = esEmptyArray()
    val fromOther = esEmptyObject()
    fromOther.text = "从其他应用导入"
    fromOther.onclick = {
        esApi.selectContent(options.mimeType, importCallback)
    }
    actions.push(fromOther)
    val fromFileManager = esEmptyObject()
    fromFileManager.text = "从文件管理器导入"
    fromFileManager.onclick = {
        esApi.openDocument(options.mimeType, importCallback, null, options.allowMultiple)
    }
    actions.push(fromFileManager)
    val gap = esEmptyObject()
    gap.gap = esDp(10)
    actions.push(gap)
    val builtinFileDialog = esEmptyObject()
    builtinFileDialog.type = if (esTruthy(options.allowMultiple)) 3 else 0
    builtinFileDialog.callback = importCallback
    val builtin = esEmptyObject()
    builtin.text = "使用内置文件浏览器导入"
    builtin.onclick = {
        esApi.showFileDialog(builtinFileDialog)
    }
    actions.push(builtin)
    js("Common").showOperateDialog(actions)
}

private fun esOpenActionsCallback(options: dynamic, uri: dynamic) {
    if (esTruthy(options.allowMultiple)) {
        options.callback(if (esIsArray(uri)) uri else esSingleArray(uri))
    } else {
        if (esIsArray(uri)) {
            var i = 0
            val n = esLength(uri)
            while (i < n) {
                options.callback(uri[i])
                i++
            }
        } else {
            options.callback(uri)
        }
    }
}

private fun esShowOpenActions(options: dynamic) {
    val openCallback = { uri: dynamic -> esOpenActionsCallback(options, uri) }
    val actions = esEmptyArray()
    val fromFileManager = esEmptyObject()
    fromFileManager.text = "从文件管理器打开"
    fromFileManager.onclick = {
        esApi.openDocument(options.mimeType, openCallback, null, options.allowMultiple)
    }
    actions.push(fromFileManager)
    val gap = esEmptyObject()
    gap.gap = esDp(10)
    actions.push(gap)
    val builtinFileDialog = esEmptyObject()
    builtinFileDialog.type = if (esTruthy(options.allowMultiple)) 3 else 0
    builtinFileDialog.callback = openCallback
    val builtin = esEmptyObject()
    builtin.text = "使用内置文件浏览器打开"
    builtin.onclick = {
        esApi.showFileDialog(builtinFileDialog)
    }
    actions.push(builtin)
    js("Common").showOperateDialog(actions)
}

private fun esShowOpenDirectoryActions(options: dynamic) {
    val actions = esEmptyArray()
    val fromFileManager = esEmptyObject()
    fromFileManager.text = "从文件管理器选择目录"
    fromFileManager.onclick = {
        esApi.openDocumentTree(options.callback)
    }
    actions.push(fromFileManager)
    val gap = esEmptyObject()
    gap.gap = esDp(10)
    actions.push(gap)
    val builtin = esEmptyObject()
    builtin.text = "使用内置文件浏览器选择目录（兼容模式）"
    builtin.onclick = {
        val dialogOptions = esEmptyObject()
        dialogOptions.type = 2
        dialogOptions.callback = options.callback
        esApi.showFileDialog(dialogOptions)
    }
    actions.push(builtin)
    js("Common").showOperateDialog(actions)
}

private fun esExportActionsCallback(options: dynamic, uri: dynamic, next: dynamic) {
    if (esType(options.export) == "function") {
        options.export(uri)
        if (esTruthy(next)) next()
    } else {
        js("Common").showProgressDialog({ dialog: dynamic ->
            dialog.setTextDelayed("正在导出文件……", 200)
            esApi.copy(esApi.toUri(options.export), uri)
            dialog.close({
                if (esTruthy(next)) next()
            })
        })
    }
}

private fun esShowExportActions(options: dynamic) {
    val exportCallback = { uri: dynamic, next: dynamic -> esExportActionsCallback(options, uri, next) }
    val actions = esEmptyArray()
    val share = esEmptyObject()
    share.text = "分享至其他应用"
    share.onclick = {
        val tempFile = esApi.createTempFile(options.hint)
        exportCallback(esApi.toUri(tempFile), {
            js("AndroidBridge").sendFile(tempFile, options.mimeType, true)
        })
    }
    actions.push(share)
    val exportToFileManager = esEmptyObject()
    exportToFileManager.text = "导出至文件管理器"
    exportToFileManager.onclick = {
        esApi.createDocument(options.mimeType, options.hint, exportCallback)
    }
    actions.push(exportToFileManager)
    val gap = esEmptyObject()
    gap.gap = esDp(10)
    actions.push(gap)
    val builtinFileDialog = esEmptyObject()
    builtinFileDialog.type = 1
    builtinFileDialog.defaultFileName = options.hint
    builtinFileDialog.callback = exportCallback
    val builtin = esEmptyObject()
    builtin.text = "使用内置文件浏览器导出（兼容模式）"
    builtin.onclick = {
        esApi.showFileDialog(builtinFileDialog)
    }
    actions.push(builtin)
    js("Common").showOperateDialog(actions)
}

private fun esShowSaveActions(options: dynamic) {
    val actions = esEmptyArray()
    val saveToFileManager = esEmptyObject()
    saveToFileManager.text = "保存至文件管理器"
    saveToFileManager.onclick = {
        esApi.createDocument(options.mimeType, options.hint, options.callback)
    }
    actions.push(saveToFileManager)
    val gap = esEmptyObject()
    gap.gap = esDp(10)
    actions.push(gap)
    val builtinFileDialog = esEmptyObject()
    builtinFileDialog.type = 1
    builtinFileDialog.defaultFileName = options.hint
    builtinFileDialog.callback = options.callback
    val builtin = esEmptyObject()
    builtin.text = "使用内置文件浏览器保存（兼容模式）"
    builtin.onclick = {
        esApi.showFileDialog(builtinFileDialog)
    }
    actions.push(builtin)
    js("Common").showOperateDialog(actions)
}

// ------------------------------ 内置文件浏览器 ------------------------------

private val esFileChooser: dynamic = js("({})")

private fun esFileChooserVmaker(): dynamic {
    val name = esNewTextViewJs(js("ctx"))
    name.setPadding(esDp(15), esDp(15), esDp(15), esDp(15))
    name.setSingleLine(true)
    name.setEllipsize(js("G.TextUtils.TruncateAt.END"))
    name.setLayoutParams(esNewAbsListViewParamsJs(-1, -2))
    return name
}

private fun esFileChooserVbinder(holder: dynamic, e: dynamic) {
    if (esTruthy(e.parent)) {
        holder.self.setText("\uD83D\uDCC2 " + esFileChooser.intl.parentDir) // Emoji: Expanded Folder
        js("Common").applyStyle(holder.self, "item_default", 3)
    } else {
        var icon = "\uD83D\uDCC4" // Emoji: Document
        if (esTruthy(e.directory)) {
            icon = "\uD83D\uDCC1" // Emoji: Collapsed Folder
        }
        holder.self.setText(icon + " " + e.name)
        if (esTruthy(e.selected)) {
            js("Common").applyStyle(holder.self, "item_highlight", 3)
            holder.self.setBackgroundColor(js("Common").theme.go_bgcolor)
        } else {
            js("Common").applyStyle(holder.self, "item_default", 3)
            holder.self.setBackgroundColor(js("G.Color").TRANSPARENT)
        }
    }
}

private fun esFileChooserCompare(a: dynamic, b: dynamic): dynamic {
    if (a.directory != b.directory) {
        return if (esTruthy(a.directory)) -1 else 1
    }
    return a.name.localeCompare(b.name)
}

private fun esFileChooserChoose(e: dynamic): Boolean {
    val options = esFileChooser.sets
    if (esTruthy(options.check) && !esTruthy(options.check(e))) return false
    esFileChooser.popup.exit()
    if (!esTruthy(options.result)) {
        options.result = e
        if (esTruthy(options.callback)) options.callback(e)
        esFileChooser.lastDir = options.stack[0]
    }
    return true
}

private fun esFileChooserRefresh() {
    val options = esFileChooser.sets
    var children = esApi.listFilesWithDetails(options.stack[0])
    if (children == null) children = esEmptyArray()
    if (esTruthy(options.filter)) {
        var i = 0
        while (i < esLength(children)) {
            if (!esTruthy(options.filter(children[i]))) {
                children.splice(i, 1)
                i--
            }
            i++
        }
    }
    children.sort(if (esTruthy(options.compare)) options.compare else esFileChooser.compare)
    val parent = esApi.getParentDirectory(options.stack[0])
    if (esTruthy(parent)) {
        if (esTruthy(esApi.canRead(parent)) || esTruthy(esApi.needAdditionalPermissions(parent))) {
            val entry = esEmptyObject()
            entry.parent = true
            entry.uri = parent
            entry.directory = true
            children.unshift(entry)
        }
    } else if (esLength(options.stack) > 1) {
        val entry = esEmptyObject()
        entry.parent = true
        entry.uri = options.stack[1]
        entry.directory = true
        children.unshift(entry)
    }
    js("G").ui({
        try {
            esFileChooser.path.setText(options.stack[0].toString())
            esFileChooser.adapter.setArray(children)
        } catch (e: dynamic) { js("erp")(e) }
    })
}

private fun esFileChooserPostRefresh() {
    js("Common").showProgressDialog({ dialog: dynamic ->
        dialog.setTextDelayed(esFileChooser.intl.loading, 200)
        esFileChooserRefresh()
    })
}

private fun esFileChooserAuthorizeAndOpen(uri: dynamic) {
    val additionPermission = esApi.needAdditionalPermissions(uri)
    if (esTruthy(additionPermission)) {
        esApi.requestAdditionalPermissions(additionPermission, { newUri: dynamic ->
            val type = esApi.getUriType(newUri)
            val options = esFileChooser.sets
            if (type == esApi.getUriType(options.stack[0])) {
                val stack = esEmptyArray()
                stack.push(newUri)
                options.stack = stack
            } else {
                options.stack.unshift(newUri)
            }
            esFileChooserPostRefresh()
        })
    } else if (additionPermission == "") {
        js("Common").toast(esFileChooser.intl.directoryCannotRead)
    }
}

private fun esFileChooserBackClick(): Boolean {
    try {
        esFileChooser.popup.exit()
        return true
    } catch (e: dynamic) {
        js("erp")(e)
        return false
    }
}

private fun esFileChooserPathEntered(s: dynamic) {
    val uri = esApi.toUri(s)
    if (!esTruthy(esApi.isDirectory(uri))) {
        js("Common").toast(esFileChooser.intl.directoryCannotRead)
        return
    }
    if (esTruthy(esApi.canRead(uri))) {
        val stack = esEmptyArray()
        stack.push(uri)
        esFileChooser.sets.stack = stack
        esFileChooserPostRefresh()
    } else {
        esFileChooserAuthorizeAndOpen(uri)
    }
}

private fun esFileChooserPathClick(): Boolean {
    try {
        val options = esFileChooser.sets
        val dialogOptions = esEmptyObject()
        dialogOptions.title = esFileChooser.intl.path
        dialogOptions.singleLine = true
        dialogOptions.defaultValue = options.stack[0].toString()
        dialogOptions.callback = { s: dynamic -> esFileChooserPathEntered(s) }
        js("Common").showInputDialog(dialogOptions)
        return true
    } catch (e: dynamic) {
        js("erp")(e)
        return false
    }
}

private fun esFileChooserNewDirName(s: dynamic) {
    if (esLength(s) == 0) {
        js("Common").toast(esFileChooser.intl.emptyDirName)
        return
    }
    try {
        val newDirectory = esApi.createDirectory(esFileChooser.sets.cursor, s)
        if (!esTruthy(newDirectory)) {
            js("Common").toast(esFileChooser.intl.failedCreateDir)
            return
        }
        esFileChooserPostRefresh()
    } catch (e: dynamic) {
        js("Common").toast(esFileChooser.intl.resolve("errCreateDir", e))
    }
}

private fun esFileChooserNewDirClick(): Boolean {
    try {
        val dialogOptions = esEmptyObject()
        dialogOptions.title = esFileChooser.intl.createDir
        dialogOptions.callback = { s: dynamic -> esFileChooserNewDirName(s) }
        js("Common").showInputDialog(dialogOptions)
        return true
    } catch (e: dynamic) {
        js("erp")(e)
        return false
    }
}

private fun esFileChooserItemClick(pos: dynamic): Boolean {
    try {
        val options = esFileChooser.sets
        val e = esFileChooser.adapter.array[pos]
        if (esTruthy(e.directory)) {
            if (esTruthy(esApi.canRead(e.uri))) {
                if (esTruthy(e.parent)) {
                    if (esLength(options.stack) > 1 && esTruthy(options.stack[1].equals(e.uri))) {
                        options.stack.shift()
                    }
                    options.stack[0] = e.uri
                } else {
                    options.stack.unshift(e.uri)
                }
                esFileChooserPostRefresh()
            } else {
                esFileChooserAuthorizeAndOpen(e.uri)
            }
        } else if (options.type == 0) {
            esFileChooserChoose(e.uri)
        } else if (options.type == 1) {
            esFileChooser.fname.setText(e.name)
        } else if (options.type == 3) {
            e.selected = !esTruthy(e.selected)
            esFileChooser.adapter.notifyChange()
        }
        return true
    } catch (e: dynamic) {
        js("erp")(e)
        return true
    }
}

private fun esFileChooserItemLongClick(pos: dynamic): Boolean {
    try {
        val e = esFileChooser.adapter.array[pos]
        if (!esTruthy(e.parent)) {
            js("Common").toast(e.name)
        }
        return true
    } catch (e: dynamic) {
        js("erp")(e)
        return true
    }
}

private fun esFileChooserExitClick(): Boolean {
    try {
        val options = esFileChooser.sets
        if (options.type == 1) {
            val fname = esString(esFileChooser.fname.getText())
            if (esLength(fname) == 0) {
                js("Common").toast(esFileChooser.intl.emptyFileName)
                return true
            }
            val newFile = esApi.createFile(options.stack[0], fname, options.mimeType)
            if (!esTruthy(newFile)) {
                js("Common").toast(esFileChooser.intl.invaildFileName)
                return true
            }
            esFileChooserChoose(newFile)
        } else if (options.type == 2) {
            esFileChooserChoose(options.stack[0])
        } else if (options.type == 3) {
            val selected = esEmptyArray()
            val array = esFileChooser.adapter.array
            var i = 0
            val n = esLength(array)
            while (i < n) {
                if (esTruthy(array[i].selected)) selected.push(array[i])
                i++
            }
            val uris = esEmptyArray()
            i = 0
            val selectedCount = esLength(selected)
            while (i < selectedCount) {
                uris.push(selected[i].uri)
                i++
            }
            esFileChooserChoose(uris)
        }
        return true
    } catch (e: dynamic) {
        js("erp")(e)
        return false
    }
}

private fun esBuildFileChooser() {
    esFileChooser.intl = js("Intl").getNamespace("common.FileChooser")
    esFileChooser.vmaker = { esFileChooserVmaker() }
    esFileChooser.vbinder = { holder: dynamic, e: dynamic -> esFileChooserVbinder(holder, e) }
    esFileChooser.compare = { a: dynamic, b: dynamic -> esFileChooserCompare(a, b) }
    esFileChooser.choose = { e: dynamic -> esFileChooserChoose(e) }
    esFileChooser.postRefresh = { esFileChooserPostRefresh() }
    esFileChooser.refresh = { esFileChooserRefresh() }
    esFileChooser.authorizeAndOpen = { uri: dynamic -> esFileChooserAuthorizeAndOpen(uri) }

    val linear = esNewLinearLayoutJs(js("ctx"))
    linear.setOrientation(js("G.LinearLayout.VERTICAL"))
    esFileChooser.linear = linear

    val header = esNewLinearLayoutJs(js("ctx"))
    header.setOrientation(js("G.LinearLayout.HORIZONTAL"))
    js("Common").applyStyle(header, "bar_float")
    esFileChooser.header = header

    val back = esNewTextViewJs(js("ctx"))
    back.setText("< " + js("Common").intl.back)
    back.setGravity(js("G.Gravity.CENTER"))
    back.setPadding(esDp(20), 0, esDp(20), 0)
    js("Common").applyStyle(back, "button_highlight", 2)
    back.setOnClickListener(esViewClickListener({ _v: dynamic -> esFileChooserBackClick() }))
    header.addView(back, esNewLinearLayoutParams2Js(-2, -1))
    esFileChooser.back = back

    val title = esNewTextViewJs(js("ctx"))
    title.setPadding(0, esDp(10), 0, esDp(10))
    js("Common").applyStyle(title, "textview_default", 4)
    header.addView(title, esNewLinearLayoutParams2Js(-2, -2))
    esFileChooser.title = title

    val path = esNewTextViewJs(js("ctx"))
    path.setGravity(esOrJs(js("G.Gravity.CENTER"), js("G.Gravity.LEFT")))
    path.setPadding(esDp(15), 0, esDp(5), 0)
    path.setSingleLine(true)
    path.setEllipsize(js("G.TextUtils.TruncateAt.START"))
    js("Common").applyStyle(path, "textview_prompt", 2)
    path.setOnClickListener(esViewClickListener({ _v: dynamic -> esFileChooserPathClick() }))
    header.addView(path, esNewLinearLayoutParams3Js(0, -1, 1.0))
    esFileChooser.path = path

    val newDir = esNewTextViewJs(js("ctx"))
    newDir.setText("\uD83D\uDCC1+") // Emoji: Collapsed Folder
    newDir.setGravity(js("G.Gravity.CENTER"))
    newDir.setPadding(esDp(20), 0, esDp(20), 0)
    js("Common").applyStyle(newDir, "button_default", 2)
    newDir.setOnClickListener(esViewClickListener({ _v: dynamic -> esFileChooserNewDirClick() }))
    header.addView(newDir, esNewLinearLayoutParams2Js(-2, -1))
    esFileChooser.newDir = newDir

    linear.addView(header, esNewLinearLayoutParams2Js(-1, -2))

    val adapterArray = esEmptyArray()
    esFileChooser.adapter = esGetListAdapterControllerJs(
        esNewSimpleListAdapterJs(adapterArray, esFileChooser.vmaker, esFileChooser.vbinder, null, true)
    )

    val list = esNewListViewJs(js("ctx"))
    list.setAdapter(esFileChooser.adapter.self)
    js("Common").applyStyle(list, "message_bg")
    list.setOnItemClickListener(esItemClickListener({ _parent: dynamic, _view: dynamic, pos: dynamic, _id: dynamic ->
        esFileChooserItemClick(pos)
    }))
    list.setOnItemLongClickListener(esItemLongClickListener({ _parent: dynamic, _view: dynamic, pos: dynamic, _id: dynamic ->
        esFileChooserItemLongClick(pos)
    }))
    list.setFastScrollEnabled(true)
    list.setFastScrollAlwaysVisible(false)
    linear.addView(list, esNewLinearLayoutParams3Js(-1, 0, 1.0))
    esFileChooser.list = list

    val inputbar = esNewLinearLayoutJs(js("ctx"))
    inputbar.setOrientation(js("G.LinearLayout.HORIZONTAL"))
    js("Common").applyStyle(inputbar, "bar_float")
    esFileChooser.inputbar = inputbar

    val fname = esNewEditTextJs(js("ctx"))
    fname.setHint(esFileChooser.intl.fileName)
    fname.setSingleLine(true)
    fname.setGravity(esOrJs(js("G.Gravity.LEFT"), js("G.Gravity.CENTER")))
    fname.setInputType(js("G.InputType.TYPE_CLASS_TEXT"))
    fname.setPadding(esDp(10), esDp(10), esDp(10), esDp(10))
    js("Common").applyStyle(fname, "edittext_default", 3)
    inputbar.addView(fname, esNewLinearLayoutParams3Js(0, -1, 4.0))
    esFileChooser.fname = fname

    val exit = esNewTextViewJs(js("ctx"))
    exit.setText(js("Common").intl.ok)
    exit.setGravity(js("G.Gravity.CENTER"))
    exit.setPadding(esDp(10), esDp(10), esDp(10), esDp(10))
    js("Common").applyStyle(exit, "button_critical", 3)
    exit.setOnClickListener(esViewClickListener({ _v: dynamic -> esFileChooserExitClick() }))
    inputbar.addView(exit, esNewLinearLayoutParams3Js(0, -2, 1.0))
    linear.addView(inputbar, esNewLinearLayoutParams2Js(-1, -2))
    esFileChooser.exit = exit

    esFileChooser.popup = esNewPopupPageJs(linear, "ExternalStorage.FileChooser")
    js("PWM").registerResetFlag(esFileChooser, "popup")
}

private fun esShowFileDialogImpl(o: dynamic) {
    try {
        if (!esTruthy(esFileChooser.popup)) {
            esBuildFileChooser()
        }
        if (esTruthy(o.onDismiss)) esFileChooser.popup.on("exit", o.onDismiss)
        esFileChooser.sets = o
        o.result = null
        try {
            var initial = o.initial
            if (!esTruthy(initial)) initial = esFileChooser.lastDir
            if (!esTruthy(initial)) initial = esApi.getAccessibleRoot()
            val stack = esEmptyArray()
            stack.push(esApi.toUri(initial))
            o.stack = stack
            if (!esTruthy(esApi.canRead(o.stack[0]))) {
                val fallback = esEmptyArray()
                fallback.push(esApi.toUri(esApi.getAccessibleRoot()))
                o.stack = fallback
            }
        } catch (e: dynamic) {
            js("Common").toast(esFileChooser.intl.resolve("errAccessDir", e))
            return
        }
        if (esApi.getUriType(o.stack[0]) != "file") {
            o.stack.push(esApi.toUri(esApi.getAccessibleRoot()))
        }
        esFileChooser.title.setText(js("Common").toString(if (esTruthy(o.title)) o.title else esFileChooser.intl.defaultTitle))
        val dialogType = o.type
        if (esStrictEq(dialogType, 1)) { // 新建文件（保存）
            esFileChooser.exit.setVisibility(js("G.View.VISIBLE"))
            esFileChooser.fname.setVisibility(js("G.View.VISIBLE"))
            esFileChooser.fname.setText(esString(if (esTruthy(o.defaultFileName)) o.defaultFileName else ""))
        } else if (esStrictEq(dialogType, 2) || esStrictEq(dialogType, 3)) { // 选择目录 / 选择多个文件（打开）
            esFileChooser.exit.setVisibility(js("G.View.VISIBLE"))
            esFileChooser.fname.setVisibility(js("G.View.GONE"))
        } else { // 选择文件（打开）
            o.type = 0
            esFileChooser.exit.setVisibility(js("G.View.GONE"))
            esFileChooser.fname.setVisibility(js("G.View.GONE"))
        }
        esFileChooser.popup.enter()
        esFileChooserPostRefresh()
    } catch (e: dynamic) {
        js("erp")(e)
    }
}

private fun esShowFileDialog(o: dynamic) {
    js("G").ui({ esShowFileDialogImpl(o); js("undefined") })
}

fun installExternalStorage() {
    esApi.onCreate = { esOnCreate(); js("undefined") }
    esApi.initialize = { esInitialize(); js("undefined") }
    esApi.getStorageAccessLevel = { esGetStorageAccessLevel() }
    esApi.getExternalStorageRoot = { esGetExternalStorageRoot() }
    esApi.getAppSpecificDirectory = { name: dynamic -> esGetAppSpecificDirectory(name) }
    esApi.getAppSpecificCacheDirectory = { esGetAppSpecificCacheDirectory() }
    esApi.getAccessibleRoot = { esGetAccessibleRoot() }
    esApi.isParentOrSelf = { file: dynamic, parent: dynamic -> esIsParentOrSelf(file, parent) }
    esApi.isInStorage = { path: dynamic -> esIsInStorage(path) }
    esApi.isAccessible = { path: dynamic -> esIsAccessible(path) }
    esApi.releaseEmptyDirectory = { file: dynamic, root: dynamic -> esReleaseEmptyDirectory(file, root); js("undefined") }
    esApi.ensureExternalStorage = { callback: dynamic -> esEnsureExternalStorage(callback); js("undefined") }
    esApi.requestStoragePermission = { callback: dynamic -> esRequestStoragePermission(callback); js("undefined") }
    esApi.requestManageStoragePermission = { callback: dynamic -> esRequestManageStoragePermission(callback); js("undefined") }
    esApi.toUri = { obj: dynamic -> esToUri(obj) }
    esApi.uriToFile = { uri: dynamic -> esUriToFile(uri) }
    esApi.getUriType = { uri: dynamic -> esGetUriType(uri) }
    esApi.queryForUri = { uri: dynamic, column: dynamic, columnType: dynamic, defaultValue: dynamic ->
        esQueryForUri(uri, column, columnType, defaultValue)
    }
    esApi.exists = { uri: dynamic -> esExists(uri) }
    esApi.isDirectory = { uri: dynamic -> esIsDirectory(uri) }
    esApi.isFile = { uri: dynamic -> esIsFileUri(uri) }
    esApi.canRead = { uri: dynamic -> esCanRead(uri) }
    esApi.canWrite = { uri: dynamic -> esCanWrite(uri) }
    esApi.getName = { uri: dynamic -> esGetName(uri) }
    esApi.getMimeType = { uri: dynamic -> esGetMimeType(uri) }
    esApi.getLastModified = { uri: dynamic -> esGetLastModified(uri) }
    esApi.getLength = { uri: dynamic -> esGetLength(uri) }
    esApi.delete = { uri: dynamic -> esDelete(uri) }
    esApi.deleteTreeFile = { file: dynamic -> esDeleteTreeFile(file) }
    esApi.renameFile = { uri: dynamic, newName: dynamic -> esRenameFile(uri, newName) }
    esApi.listFiles = { uri: dynamic -> esListFiles(uri) }
    esApi.listFilesWithDetails = { uri: dynamic -> esListFilesWithDetails(uri) }
    esApi.getParentDirectory = { uri: dynamic -> esGetParentDirectory(uri) }
    esApi.getChildFile = { uri: dynamic, name: dynamic -> esGetChildFile(uri, name) }
    esApi.createFile = { uri: dynamic, name: dynamic, mimeType: dynamic -> esCreateFile(uri, name, mimeType) }
    esApi.createDirectory = { uri: dynamic, name: dynamic -> esCreateDirectory(uri, name) }
    esApi.uriToName = { uri: dynamic -> esUriToName(uri) }
    esApi.uriToReadablePath = { uri: dynamic -> esUriToReadablePath(uri) }
    esApi.getExtensionFromName = { name: dynamic -> esGetExtensionFromName(name) }
    esApi.getMimeTypeFromExtension = { extension: dynamic -> esGetMimeTypeFromExtension(extension) }
    esApi.getLengthString = { uri: dynamic, longer: dynamic -> esGetLengthString(uri, longer) }
    esApi.getLastModifiedString = { uri: dynamic -> esGetLastModifiedString(uri) }
    esApi.openInputStream = { uri: dynamic -> esOpenInputStream(uri) }
    esApi.openOutputStream = { uri: dynamic, mode: dynamic -> esOpenOutputStream(uri, mode) }
    esApi.pipe = { input: dynamic, output: dynamic, bufferSize: dynamic -> esPipe(input, output, bufferSize); js("undefined") }
    esApi.copy = { sourceUri: dynamic, destUri: dynamic -> esCopy(sourceUri, destUri); js("undefined") }
    esApi.readFileContent = { uri: dynamic, encoding: dynamic, defaultValue: dynamic ->
        esReadFileContent(uri, encoding, defaultValue)
    }
    esApi.writeFileContent = { uri: dynamic, content: dynamic, encoding: dynamic, append: dynamic ->
        esWriteFileContent(uri, content, encoding, append); js("undefined")
    }
    esApi.createImportFile = { hint: dynamic -> esCreateImportFile(hint) }
    esApi.tryReleaseImportFile = { fileOrPath: dynamic -> esTryReleaseImportFile(fileOrPath) }
    esApi.tryReleaseImportUri = { uri: dynamic -> esTryReleaseImportUri(uri) }
    esApi.importFile = { uri: dynamic, hint: dynamic -> esImportFile(uri, hint) }
    esApi.createTempFile = { hint: dynamic -> esCreateTempFile(hint) }
    esApi.cleanTempFiles = { esCleanTempFiles(); js("undefined") }
    esApi.selectContent = { mimeType: dynamic, callback: dynamic -> esSelectContent(mimeType, callback); js("undefined") }
    esApi.createDocument = { mimeType: dynamic, defaultTitle: dynamic, callback: dynamic, initialUri: dynamic ->
        esCreateDocument(mimeType, defaultTitle, callback, initialUri); js("undefined")
    }
    esApi.openDocument = { mimeType: dynamic, callback: dynamic, initialUri: dynamic, allowMultiple: dynamic ->
        esOpenDocument(mimeType, callback, initialUri, allowMultiple); js("undefined")
    }
    esApi.openDocumentTree = { callback: dynamic, initialUri: dynamic, showAdvanced: dynamic ->
        esOpenDocumentTree(callback, initialUri, showAdvanced); js("undefined")
    }
    esApi.requestDocumentTreeAccess = { path: dynamic, callback: dynamic ->
        esRequestDocumentTreeAccess(path, callback); js("undefined")
    }
    esApi.needAdditionalPermissions = { uri: dynamic -> esNeedAdditionalPermissions(uri) }
    esApi.requestAdditionalPermissions = { type: dynamic, callback: dynamic ->
        esRequestAdditionalPermissions(type, callback); js("undefined")
    }
    esApi.listAppExternalPath = { prefix: dynamic, callback: dynamic -> esListAppExternalPath(prefix, callback); js("undefined") }
    esApi.isPersistedUriPermission = { uri: dynamic -> esIsPersistedUriPermission(uri) }
    esApi.tryTakeUriPermission = { uri: dynamic -> esTryTakeUriPermission(uri) }
    esApi.releaseUriPermissions = { uri: dynamic -> esReleaseUriPermissions(uri); js("undefined") }
    esApi.releaseAllUriPermissions = { esReleaseAllUriPermissions(); js("undefined") }
    esApi.checkReleaseUriPermissions = { esCheckReleaseUriPermissions(); js("undefined") }
    esApi.showImportActions = { options: dynamic -> esShowImportActions(options); js("undefined") }
    esApi.showOpenActions = { options: dynamic -> esShowOpenActions(options); js("undefined") }
    esApi.showOpenDirectoryActions = { options: dynamic -> esShowOpenDirectoryActions(options); js("undefined") }
    esApi.showExportActions = { options: dynamic -> esShowExportActions(options); js("undefined") }
    esApi.showSaveActions = { options: dynamic -> esShowSaveActions(options); js("undefined") }
    esApi.showFileDialog = { o: dynamic -> esShowFileDialog(o); js("undefined") }

    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("ExternalStorage", esApi)
    }
}
