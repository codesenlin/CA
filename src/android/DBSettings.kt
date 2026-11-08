package ca.kotlin.android

import ca.kotlin.core.*


/**
 * modules/utils/DBSettings.js 的 Kotlin/JS 版本。
 *
 * 与原模块一致：
 * - installDBSettings() 在原来的加载位置注册 DBSettings；
 * - MapScript.loadModule 会立即调用 onCreate，因此所有方法必须先在 install 中挂好；
 * - 原文件末尾的 MapScript.loadModule("Settings", DBSettings.getNamespace("settings"))
 *   也要一起执行，否则依赖 Settings 的调用方会失去这个全局对象。
 *
 * 所有方法通过闭包里的 dbApi 调用，不依赖 JS 的 this。
 * JS 数组长度一律经 dbLength() 走 ToInt32，避免 Rhino 的 Java 数组长度类型差异。
 */
private val dbApi: dynamic = js("({})")

private val dbTruthyJs: dynamic = js("function (v) { return !!v; }")
private val dbLengthJs: dynamic = js("function (v) { return v.length | 0; }")
private val dbStringJs: dynamic = js("function (v) { return String(v); }")
private val dbTypeJs: dynamic = js("function (v) { return typeof v; }")
private val dbIsArrayJs: dynamic = js("function (v) { return Array.isArray(v); }")
private val dbInstanceOfJs: dynamic = js("function (v, c) { return v instanceof c; }")
private val dbToIntJs: dynamic = js("function (v) { return Number(v) | 0; }")
private val dbNewLongJs: dynamic = js("function (v) { return new java.lang.Long(v); }")
private val dbNewDoubleJs: dynamic = js("function (v) { return new java.lang.Double(v); }")
private val dbNewContentValuesJs: dynamic = js("function (n) { return new android.content.ContentValues(n); }")
private val dbNewFileJs: dynamic = js("function (a, b) { return new java.io.File(a, b); }")
private val dbNewArrayJs: dynamic = js("function (n) { return new Array(n); }")
private val dbNewErrorJs: dynamic = js("function (m) { return new Error(m); }")
private val dbNewDateJs: dynamic = js("function (v) { return new Date(v); }")
private val dbDeleteKeyJs: dynamic = js("function (o, k) { delete o[k]; }")
private val dbKeyInJs: dynamic = js("function (o, k) { return k in o; }")
private val dbDefineIdJs: dynamic = js("function (o, id) { Object.defineProperty(o, '__id__', { value: id }); }")
private val dbEscapeIdentifierJs: dynamic = js("function (id) { return '\"' + id.replace(/\"/g, '\"\"') + '\"'; }")
private val dbUpperCaseJs: dynamic = js("function (v) { return v.toUpperCase(); }")

private fun dbTruthy(v: dynamic): Boolean = dbTruthyJs(v)
private fun dbLength(v: dynamic): Int = dbLengthJs(v)
private fun dbString(v: dynamic): dynamic = dbStringJs(v)
private fun dbType(v: dynamic): dynamic = dbTypeJs(v)
private fun dbIsArrayValue(v: dynamic): Boolean = dbIsArrayJs(v)
private fun dbInstanceOf(v: dynamic, c: dynamic): Boolean = dbInstanceOfJs(v, c)
private fun dbToInt(v: dynamic): Int = dbToIntJs(v)
private fun dbNewLong(v: dynamic): dynamic = dbNewLongJs(v)
private fun dbNewDouble(v: dynamic): dynamic = dbNewDoubleJs(v)
private fun dbNewError(message: dynamic): dynamic = dbNewErrorJs(message)
private fun dbDeleteKey(o: dynamic, k: dynamic) { dbDeleteKeyJs(o, k) }
private fun dbKeyIn(o: dynamic, k: dynamic): Boolean = dbKeyInJs(o, k)
private fun dbDefineId(o: dynamic, id: dynamic) { dbDefineIdJs(o, id) }

private fun dbEmptyObject(): dynamic = js("({})")
private fun dbEmptyArray(): dynamic = js("[]")

private fun dbTransaction(f: dynamic) {
    dbApi.db.beginTransaction()
    try {
        f()
        dbApi.db.setTransactionSuccessful()
    } finally {
        dbApi.db.endTransaction()
    }
}

private fun dbOnCreate() {
    // const file = new java.io.File(ctx.getDir("rhino", 0), this.dbFile);
    val file = dbNewFileJs(js("ExternalStorage").getAppSpecificDirectory("settings"), dbApi.dbFile)
    try {
        dbApi.db = js("android.database.sqlite.SQLiteDatabase").openOrCreateDatabase(file, null)
    } catch (err: dynamic) {
        js("erp")(err)
        dbApi.db = js("android.database.sqlite.SQLiteDatabase").create(null)
    }
    dbTransaction({
        val dbVersion = dbApi.db.getVersion()
        if (dbVersion < 1) {
            dbApi.createVersionStore()
            dbApi.db.setVersion(1)
        }
    })
}

private fun dbObjectToContentValues(obj: dynamic): dynamic {
    val keys = js("Object").keys(obj)
    val ret = dbNewContentValuesJs(dbLength(keys))
    var i = 0
    val n = dbLength(keys)
    while (i < n) {
        val k = keys[i]
        if (dbType(obj[k]) == "number") {
            ret.put(k, dbNewDouble(obj[k]))
        } else {
            ret.put(k, obj[k])
        }
        i++
    }
    return ret
}

private fun dbReadCursorRow(cursor: dynamic): dynamic {
    val ret = dbEmptyObject()
    val columnNames = cursor.getColumnNames()
    var columnValue: dynamic = js("undefined")
    var i = 0
    val n = dbLength(columnNames)
    while (i < n) {
        val columnType = cursor.getType(i)
        if (columnType == 0) { // FIELD_TYPE_NULL
            columnValue = null
        } else if (columnType == 1) { // FIELD_TYPE_INTEGER
            columnValue = dbNewLong(cursor.getLong(i))
        } else if (columnType == 2) { // FIELD_TYPE_FLOAT
            columnValue = cursor.getDouble(i)
        } else if (columnType == 3) { // FIELD_TYPE_STRING
            columnValue = cursor.getString(i)
        } else if (columnType == 4) { // FIELD_TYPE_BLOB
            columnValue = cursor.getBlob(i)
        }
        ret[columnNames[i]] = columnValue
        i++
    }
    return ret
}

private fun dbReadCursorOnce(cursor: dynamic): dynamic {
    if (cursor.moveToFirst()) {
        return dbReadCursorRow(cursor)
    }
    return null
}

private fun dbReadCursorAll(cursor: dynamic): dynamic {
    val ret = dbNewArrayJs(cursor.getCount())
    cursor.moveToPosition(-1)
    while (cursor.moveToNext()) {
        ret[cursor.getPosition()] = dbReadCursorRow(cursor)
    }
    return ret
}

private fun dbForEachColumn(cursor: dynamic, f: dynamic) {
    cursor.moveToPosition(-1)
    while (cursor.moveToNext()) {
        val column = dbReadCursorRow(cursor)
        f(column, cursor.getPosition(), cursor)
    }
}

private fun dbEscapeIdentifier(identifier: dynamic): dynamic = dbEscapeIdentifierJs(identifier)

private fun dbBuildColumnDefinition(statement: dynamic, key: dynamic, value: dynamic) {
    statement.push(dbApi.escapeIdentifier(key))
    if (dbType(value) == "object" && value != null) {
        if (dbTruthy(value.type)) {
            statement.push(" " + dbUpperCaseJs(value.type))
        }
        if (dbTruthy(value.primaryKey)) {
            statement.push(" PRIMARY KEY")
            if (dbTruthy(value.autoIncrement)) {
                statement.push(" AUTOINCREMENT")
            }
        }
        if (dbTruthy(value.unique)) {
            statement.push(" UNIQUE")
        }
        if (dbTruthy(value.notNull)) {
            statement.push(" NOT NULL")
        }
    } else if (dbType(value) == "string") {
        statement.push(" " + dbUpperCaseJs(value))
    }
}

private fun dbPrepareSql(statement: dynamic): dynamic {
    val sql = dbEmptyArray()
    val args = dbEmptyArray()
    if (dbIsArrayValue(statement)) {
        var i = 0
        val n = dbLength(statement)
        while (i < n) {
            val part = statement[i]
            if (dbIsArrayValue(part)) {
                sql.push(part[0])
                var j = 1
                val m = dbLength(part)
                while (j < m) {
                    args.push(part[j])
                    j++
                }
            } else {
                sql.push(part)
            }
            i++
        }
    } else {
        sql.push(statement)
    }
    val result = dbEmptyArray()
    result.push(sql.join(""))
    result.push(args)
    return result
}

private fun dbCompileSql(statement: dynamic): dynamic {
    val prepared = dbApi.prepareSql(statement)
    val sql = prepared[0]
    val args = prepared[1]
    val stmt = dbApi.db.compileStatement(sql)
    var i = 1
    val n = dbLength(args)
    while (i <= n) {
        val arg = args[i - 1]
        if (arg == null) {
            stmt.bindNull(i)
        } else if (dbType(arg) == "number") {
            stmt.bindDouble(i, arg)
        } else if (dbType(arg) == "boolean") { // Will cast to 0l or 1l
            stmt.bindLong(i, if (dbTruthy(arg)) 1 else 0)
        } else if (dbInstanceOf(arg, js("java.lang.Long"))) {
            stmt.bindLong(i, arg)
        } else if (dbInstanceOf(arg, dbApi.BlobType)) {
            stmt.bindBlob(i, arg)
        } else {
            stmt.bindString(i, dbString(arg))
        }
        i++
    }
    return stmt
}

private fun dbDoQuery(statement: dynamic): dynamic {
    val prepared = dbApi.prepareSql(statement)
    val sql = prepared[0]
    val args = prepared[1]
    return dbApi.db.rawQuery(sql, args)
}

private fun dbCreateStore(name: dynamic, fields: dynamic, throwIfExists: dynamic) {
    val statement = dbEmptyArray()
    statement.push("CREATE TABLE ")
    if (!dbTruthy(throwIfExists)) {
        statement.push("IF NOT EXISTS ")
    }
    statement.push(dbApi.escapeIdentifier(name))
    statement.push("(")
    val entries = js("Object").entries(fields)
    var i = 0
    val n = dbLength(entries)
    while (i < n) {
        if (i > 0) {
            statement.push(",")
        }
        val entry = entries[i]
        dbApi.buildColumnDefinition(statement, entry[0], entry[1])
        i++
    }
    statement.push(")")
    dbApi.compileSQL(statement).execute()
}

private fun dbDeleteStore(name: dynamic, ignoreIfExists: dynamic) {
    val statement = dbEmptyArray()
    statement.push("DROP TABLE ")
    if (dbTruthy(ignoreIfExists)) {
        statement.push("IF EXISTS ")
    }
    statement.push(dbApi.escapeIdentifier(name))
    dbApi.compileSQL(statement).execute()
}

private fun dbRenameStore(oldName: dynamic, newName: dynamic) {
    val statement = dbEmptyArray()
    statement.push("ALTER TABLE ")
    statement.push(dbApi.escapeIdentifier(oldName))
    statement.push(" RENAME TO ")
    statement.push(dbApi.escapeIdentifier(newName))
    dbApi.compileSQL(statement).execute()
}

private fun dbMigrateStore(name: dynamic, changes: dynamic) {
    val entries = js("Object").entries(changes)
    var i = 0
    val n = dbLength(entries)
    while (i < n) {
        val entry = entries[i]
        val key = entry[0]
        val value = entry[1]
        val statement = dbEmptyArray()
        statement.push("ALTER TABLE ")
        statement.push(dbApi.escapeIdentifier(name))
        if (value == null) {
            statement.push(" DROP ")
            statement.push(dbApi.escapeIdentifier(key))
        } else if (dbType(value) == "string") {
            statement.push(" RENAME ")
            statement.push(dbApi.escapeIdentifier(key))
            statement.push(" TO ")
            statement.push(dbApi.escapeIdentifier(value))
        } else if (dbType(value) == "object") {
            statement.push(" ADD ")
            dbApi.buildColumnDefinition(statement, key, value)
        }
        dbApi.compileSQL(statement).execute()
        i++
    }
}

private fun dbIsStoreExists(name: dynamic): Boolean {
    val pair = dbEmptyArray()
    pair.push("SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = ?")
    pair.push(name)
    val statement = dbEmptyArray()
    statement.push(pair)
    return dbApi.compileSQL(statement).simpleQueryForLong() > 0
}

private fun dbClearStore(name: dynamic): dynamic {
    val options = dbEmptyObject()
    options.store = name
    return dbApi.deleteItems(options)
}

private fun dbQueryItems(options: dynamic): dynamic {
    val statement = dbEmptyArray()
    statement.push("SELECT ")
    if (dbTruthy(options.distinct)) {
        statement.push("DISTINCT ")
    }
    if (dbTruthy(options.fields)) {
        val escapedFields = dbEmptyArray()
        if (dbIsArrayValue(options.fields)) {
            var i = 0
            val n = dbLength(options.fields)
            while (i < n) {
                escapedFields.push(dbApi.escapeIdentifier(options.fields[i]))
                i++
            }
        } else {
            val entries = js("Object").entries(options.fields)
            var i = 0
            val n = dbLength(entries)
            while (i < n) {
                val entry = entries[i]
                escapedFields.push(entry[1] + " AS " + dbApi.escapeIdentifier(entry[0]))
                i++
            }
        }
        statement.push(escapedFields.join(","))
    } else {
        statement.push("*")
    }
    statement.push(" FROM ")
    statement.push(dbApi.escapeIdentifier(options.store))
    if (dbTruthy(options.selection)) {
        statement.push(" WHERE ")
        statement.push(options.selection)
    }
    if (dbTruthy(options.groupBy)) {
        statement.push(" GROUP BY ")
        statement.push(options.groupBy)
        if (dbTruthy(options.having)) {
            statement.push(" HAVING ")
            statement.push(options.having)
        }
    }
    if (dbTruthy(options.orderBy)) {
        statement.push(" ORDER BY ")
        statement.push(options.orderBy)
    }
    if (dbTruthy(options.limit)) {
        statement.push(" LIMIT ")
        statement.push(options.limit)
    }
    return dbApi.doQuery(statement)
}

private fun dbConflictAlgorithm(name: dynamic): dynamic {
    return dbApi.ConflictAlgorithm[if (dbTruthy(name)) name else "none"]
}

private fun dbPutItems(options: dynamic): dynamic {
    val statement = dbEmptyArray()
    statement.push("INSERT")
    statement.push(dbConflictAlgorithm(options.onConflict))
    statement.push(" INTO ")
    statement.push(dbApi.escapeIdentifier(options.store))
    statement.push("(")
    val items = options.items
    val keys = js("Object").keys(items[0])
    var i = 0
    val keyCount = dbLength(keys)
    while (i < keyCount) {
        if (i > 0) {
            statement.push(",")
        }
        statement.push(dbApi.escapeIdentifier(keys[i]))
        i++
    }
    statement.push(") VALUES ")
    var k = 0
    val itemCount = dbLength(items)
    while (k < itemCount) {
        if (k > 0) {
            statement.push(",")
        }
        statement.push("(")
        val item = items[k]
        var j = 0
        while (j < keyCount) {
            if (j > 0) {
                statement.push(",")
            }
            val arg = dbEmptyArray()
            arg.push("?")
            arg.push(item[keys[j]])
            statement.push(arg)
            j++
        }
        statement.push(")")
        k++
    }
    return dbApi.compileSQL(statement).executeInsert()
}

private fun dbSetItem(options: dynamic): dynamic {
    val items = dbEmptyArray()
    items.push(options.item)
    val putOptions = dbEmptyObject()
    putOptions.store = options.store
    putOptions.items = items
    putOptions.onConflict = "replace"
    return dbApi.putItems(putOptions)
}

private fun dbUpdateItems(options: dynamic): dynamic {
    val statement = dbEmptyArray()
    statement.push("UPDATE")
    statement.push(dbConflictAlgorithm(options.onConflict))
    statement.push(dbApi.escapeIdentifier(options.store))
    statement.push(" SET ")
    val changes = options.changes
    val keys = js("Object").keys(changes)
    var i = 0
    val n = dbLength(keys)
    while (i < n) {
        if (i > 0) {
            statement.push(",")
        }
        val k = keys[i]
        statement.push(dbApi.escapeIdentifier(k))
        val arg = dbEmptyArray()
        arg.push("=?")
        arg.push(changes[k])
        statement.push(arg)
        i++
    }
    if (dbTruthy(options.selection)) {
        statement.push(" WHERE ")
        statement.push(options.selection)
    }
    return dbApi.compileSQL(statement).executeUpdateDelete()
}

private fun dbDeleteItems(options: dynamic): dynamic {
    val statement = dbEmptyArray()
    statement.push("DELETE FROM ")
    statement.push(dbApi.escapeIdentifier(options.store))
    if (dbTruthy(options.selection)) {
        statement.push(" WHERE ")
        statement.push(options.selection)
    }
    return dbApi.compileSQL(statement).executeUpdateDelete()
}

private fun dbCheckFieldType(value: dynamic) {
    val type = dbType(value)
    if (type == "object" && (value == null || dbInstanceOf(value, dbApi.BlobType))) {
        return
    }
    if (type == "object"
        || type == "undefined"
        || type == "bigint"
        || type == "symbol"
        || type == "function") {
        throw dbNewError("DBSettings cannot storage " + type)
    }
}

private fun dbCreateVersionStore() {
    val fields = dbEmptyObject()
    val storeName = dbEmptyObject()
    storeName.type = "text"
    storeName.primaryKey = true
    fields.store_name = storeName
    fields.version = "int"
    dbApi.createStore("version", fields)
}

private fun dbGetStoreVersion(name: dynamic): dynamic {
    val fields = dbEmptyArray()
    fields.push("version")
    val options = dbEmptyObject()
    options.store = "version"
    options.fields = fields
    val selection = dbEmptyArray()
    selection.push("store_name = ?")
    selection.push(name)
    options.selection = selection
    options.limit = "1"
    val cursor = dbApi.queryItems(options)
    val ret = dbApi.readCursorOnce(cursor)
    return if (ret != null) ret.version else 0
}

private fun dbSetStoreVersion(name: dynamic, version: dynamic) {
    val item = dbEmptyObject()
    item.store_name = name
    item.version = version
    val options = dbEmptyObject()
    options.store = "version"
    options.item = item
    dbApi.setItem(options)
}

private fun dbDeleteStoreWithVersion(name: dynamic) {
    dbApi.deleteStore(name)
    val selection = dbEmptyArray()
    selection.push("store_name = ?")
    selection.push(name)
    val options = dbEmptyObject()
    options.store = "version"
    options.selection = selection
    dbApi.deleteItems(options)
}

private fun dbGetUndefinedInstance(cx: dynamic, scope: dynamic): dynamic {
    try {
        val u = js("com.xero.ca.script.WrappedUndefined").get(cx, scope)
        if (dbTruthy(u)) return u
    } catch (e: dynamic) {
        // 类不存在，用 Rhino 内置的兜底
    }
    return js("org.mozilla.javascript.Undefined").instance
}

/**
 * 旧版 Rhino 没有原生 Proxy，原模块的 Scriptable 手写兜底原样保留在 JS helper 里。
 * 这段只在 typeof Proxy != "function" 时执行；Rhino 1.9.2 实际走下面的原生分支。
 */
private val dbLegacyProxyJs: dynamic = js(
    """function (target, handler) {
        var cx = org.mozilla.javascript.Context.getCurrentContext();
        var topScope = eval.call(null, "this");
        var u;
        try { u = com.xero.ca.script.WrappedUndefined.get(cx, topScope); } catch (e) { }
        var Undefined = u || org.mozilla.javascript.Undefined.instance;
        var targetScriptable = ScriptInterface.wrapAsScriptable
            ? ScriptInterface.wrapAsScriptable(target, topScope)
            : new org.mozilla.javascript.NativeJavaObject(topScope, target, org.mozilla.javascript.Scriptable);
        var OriginCall = function (method) { return targetScriptable[method].bind(targetScriptable); };
        var scriptable = new org.mozilla.javascript.Scriptable({
            delete: handler.deleteProperty ? function (key) {
                handler.deleteProperty(target, key);
            } : OriginCall("delete"),
            get: handler.get ? function (key, start) {
                var r = handler.get(target, key, start);
                if (typeof r == "undefined") {
                    return Undefined;
                }
                return r;
            } : function (key, start) {
                var r = targetScriptable.get(key, start);
                if (typeof r == "undefined") {
                    return Undefined;
                }
                return r;
            },
            getClassName: function () { return "Proxy<" + targetScriptable.getClassName() + ">"; },
            getDefaultValue: OriginCall("getDefaultValue"),
            getIds: handler.ownKeys ? function () {
                var keys = handler.ownKeys(target);
                var arr = java.lang.reflect.Array.newInstance(java.lang.Object, keys.length);
                for (var i = 0; i < keys.length; i++) {
                    if (typeof keys[i] == "number") {
                        arr[i] = new java.lang.Integer(keys[i]);
                    } else {
                        arr[i] = keys[i];
                    }
                }
                return arr;
            } : OriginCall("getIds"),
            getParentScope: OriginCall("getParentScope"),
            getPrototype: handler.getPrototypeOf ? function () {
                return handler.getPrototypeOf(target);
            } : OriginCall("getPrototype"),
            has: handler.has ? function (key, start) {
                return handler.has(target, key, start);
            } : OriginCall("has"),
            hasInstance: OriginCall("hasInstance"),
            put: handler.set ? function (key, start, value) {
                handler.set(target, key, value, start);
            } : function (key, start, value) {
                targetScriptable.put(key, target, value);
            },
            setParentScope: OriginCall("setParentScope"),
            setPrototype: handler.setPrototypeOf ? function (proto) {
                return handler.setPrototypeOf(target, proto);
            } : OriginCall("setPrototype")
        });
        return cx.toObject(scriptable, topScope);
    }"""
)

private fun dbCreateProxy(target: dynamic, handler: dynamic): dynamic {
    if (js("typeof Proxy") == "function") {
        val traps = dbEmptyObject()
        if (dbTruthy(handler.get)) {
            traps.get = { t: dynamic, k: dynamic, r: dynamic -> handler.get(t, k, r) }
        }
        traps.set = { t: dynamic, k: dynamic, v: dynamic, r: dynamic ->
            if (dbTruthy(handler.set)) {
                handler.set(t, k, v, r) !== false
            } else {
                t[k] = v
                true
            }
        }
        traps.deleteProperty = { t: dynamic, k: dynamic ->
            if (dbTruthy(handler.deleteProperty)) {
                handler.deleteProperty(t, k)
            }
            true
        }
        if (dbTruthy(handler.has)) {
            traps.has = { t: dynamic, k: dynamic -> handler.has(t, k) }
        }
        if (dbTruthy(handler.ownKeys)) {
            traps.ownKeys = { t: dynamic -> handler.ownKeys(t) }
        }
        if (dbTruthy(handler.getPrototypeOf)) {
            traps.getPrototypeOf = { t: dynamic -> handler.getPrototypeOf(t) }
        }
        return js("function (t, tr) { return new Proxy(t, tr); }")(target, traps)
    }
    return dbLegacyProxyJs(target, handler)
}

private fun dbEnsureNamespace(storeName: dynamic) {
    val version = dbApi.getStoreVersion(storeName)
    if (version < 1) {
        val fields = dbEmptyObject()
        val idField = dbEmptyObject()
        idField.type = "integer"
        idField.primaryKey = true
        idField.autoIncrement = true
        fields.id = idField
        fields.parent = "integer"
        fields.key = null
        fields.type = "integer"
        fields.value = null
        dbApi.createStore(storeName, fields)
        dbApi.setStoreVersion(storeName, 1)
    }
}

private fun dbAllocateRefId(storeName: dynamic): dynamic {
    val fields = dbEmptyObject()
    fields.max = "max(value)"
    val options = dbEmptyObject()
    options.store = storeName
    options.fields = fields
    options.selection = "type = 3 or type = 4"
    val query = dbApi.queryItems(options)
    val ret = dbApi.readCursorOnce(query)
    val next: dynamic = if (ret.max == null) 1 else ret.max + 1
    return dbNewLong(next)
}

private fun dbConvertToJSValue(storeName: dynamic, dbTypeId: dynamic, dbValue: dynamic): dynamic {
    when (dbToInt(dbTypeId)) {
        0 -> return dbValue // primitive: number / string / byte[] / null
        1 -> return dbValue != 0 // boolean
        2 -> return dbNewDateJs(dbValue) // Date
        3 -> return dbApi.getNamespaceProxy(storeName, dbValue, "object")
        4 -> return dbApi.getNamespaceProxy(storeName, dbValue, "array")
    }
    return js("undefined")
}

private fun dbConvertToDBValue(storeName: dynamic, jsValue: dynamic): dynamic {
    val type = dbType(jsValue)
    if (type == "number" || type == "string") {
        val ret = dbEmptyObject()
        ret.type = 0
        ret.value = jsValue
        return ret
    }
    if (type == "boolean") {
        val ret = dbEmptyObject()
        ret.type = 1
        ret.value = if (dbTruthy(jsValue)) 1 else 0
        return ret
    }
    if (type == "object") {
        if (jsValue == null || dbInstanceOf(jsValue, dbApi.BlobType) || dbInstanceOf(jsValue, js("java.lang.Long"))) {
            val ret = dbEmptyObject()
            ret.type = 0
            ret.value = jsValue
            return ret
        }
        if (dbInstanceOf(jsValue, js("Date"))) {
            val ret = dbEmptyObject()
            ret.type = 2
            ret.value = jsValue.getTime()
            return ret
        }
        val isArray = dbInstanceOf(jsValue, js("Array"))
        if (dbTruthy(jsValue.__id__)) {
            val ret = dbEmptyObject()
            ret.type = if (isArray) 4 else 3
            ret.value = jsValue.__id__
            return ret
        }
        val refId = dbApi.allocateRefId(storeName)
        val proxy = dbApi.getNamespaceProxy(storeName, refId, if (isArray) "array" else "object")
        val ret = dbEmptyObject()
        ret.type = if (isArray) 4 else 3
        ret.value = refId
        ret.proxy = proxy
        ret.afterStored = {
            val entries = js("Object").entries(jsValue)
            var i = 0
            val n = dbLength(entries)
            while (i < n) {
                val entry = entries[i]
                proxy[entry[0]] = entry[1]
                i++
            }
        }
        return ret
    }
    return js("undefined")
}

private fun dbNamespaceProxySet(
    storeName: dynamic,
    startId: dynamic,
    idMap: dynamic,
    base: dynamic,
    key: dynamic,
    value: dynamic
): Boolean {
    val converted = dbApi.convertToDBValue(storeName, value)
    if (!dbTruthy(converted)) {
        throw dbNewError("Cannot set " + key + " to " + value)
    }
    val item = dbEmptyObject()
    item.parent = startId
    item.key = if (dbType(key) == "number") dbNewLong(key) else key
    item.type = converted.type
    item.value = converted.value
    if (dbKeyIn(idMap, key)) {
        item.id = idMap[key]
    }
    val setOptions = dbEmptyObject()
    setOptions.store = storeName
    setOptions.item = item
    idMap[key] = dbApi.setItem(setOptions)
    base[key] = if (dbTruthy(converted.proxy)) converted.proxy else value
    if (dbTruthy(converted.afterStored)) {
        converted.afterStored()
    }
    return true
}

private fun dbNamespaceProxyDelete(storeName: dynamic, idMap: dynamic, base: dynamic, key: dynamic) {
    val id = idMap[key]
    dbDeleteKey(base, key)
    dbDeleteKey(idMap, key)
    val selection = dbEmptyArray()
    selection.push("id = ?")
    selection.push(id)
    val options = dbEmptyObject()
    options.store = storeName
    options.selection = selection
    dbApi.deleteItems(options)
}

private fun dbGetNamespaceProxy(storeName: dynamic, startId: dynamic, startType: dynamic): dynamic {
    val base: dynamic = if (startType == "array") dbEmptyArray() else dbEmptyObject()
    val idMap = dbEmptyObject()
    val selection = dbEmptyArray()
    selection.push("parent = ?")
    selection.push(startId)
    val options = dbEmptyObject()
    options.store = storeName
    options.selection = selection
    val cursor = dbApi.queryItems(options)
    dbDefineId(base, startId)
    dbApi.forEachColumn(cursor, { entry: dynamic ->
        base[entry.key] = dbApi.convertToJSValue(storeName, entry.type, entry.value)
        idMap[entry.key] = entry.id
    })
    val handler = dbEmptyObject()
    handler.set = { t: dynamic, key: dynamic, value: dynamic -> dbNamespaceProxySet(storeName, startId, idMap, base, key, value) }
    handler.deleteProperty = { t: dynamic, key: dynamic -> dbNamespaceProxyDelete(storeName, idMap, base, key) }
    return dbApi.createProxy(base, handler)
}

private fun dbVisitNamespace(entries: dynamic, parentRef: dynamic, parent: dynamic) {
    var i = 0
    val n = dbLength(entries)
    while (i < n) {
        val child = entries[i]
        if (child.parent == parentRef) {
            if (dbTruthy(child.visited) && parent != null) {
                parent.circular = true
            }
            child.visited = true
            if (child.type == 3 || child.type == 4) {
                dbVisitNamespace(entries, child.value, child.id)
            }
        }
        i++
    }
}

private fun dbDoNamespaceGC(storeName: dynamic) {
    val options = dbEmptyObject()
    options.store = storeName
    val cursor = dbApi.queryItems(options)
    val entries = dbApi.readCursorAll(cursor)
    dbVisitNamespace(entries, 0, null)
    dbTransaction({
        var i = 0
        val n = dbLength(entries)
        while (i < n) {
            val entry = entries[i]
            if (!dbTruthy(entry.visited) || dbTruthy(entry.circular)) {
                val selection = dbEmptyArray()
                selection.push("id=?")
                selection.push(entry.id)
                val deleteOptions = dbEmptyObject()
                deleteOptions.store = storeName
                deleteOptions.selection = selection
                dbApi.deleteItems(deleteOptions)
            }
            i++
        }
    })
}

private fun dbGetNamespace(storeName: dynamic): dynamic {
    dbApi.ensureNamespace(storeName)
    dbApi.doNamespaceGC(storeName)
    return dbApi.getNamespaceProxy(storeName, 0, "object")
}

fun installDBSettings() {
    dbApi.dbFile = "settings.db"
    dbApi.db = null
    dbApi.BlobType = js("Packages")["[B"]

    dbApi.onCreate = { dbOnCreate(); js("undefined") }
    dbApi.transaction = { f: dynamic -> dbTransaction(f); js("undefined") }
    dbApi.objectToContentValues = { obj: dynamic -> dbObjectToContentValues(obj) }
    dbApi.readCursorRow = { cursor: dynamic -> dbReadCursorRow(cursor) }
    dbApi.readCursorOnce = { cursor: dynamic -> dbReadCursorOnce(cursor) }
    dbApi.readCursorAll = { cursor: dynamic -> dbReadCursorAll(cursor) }
    dbApi.forEachColumn = { cursor: dynamic, f: dynamic -> dbForEachColumn(cursor, f); js("undefined") }
    dbApi.escapeIdentifier = { identifier: dynamic -> dbEscapeIdentifier(identifier) }
    dbApi.buildColumnDefinition = { statement: dynamic, key: dynamic, value: dynamic ->
        dbBuildColumnDefinition(statement, key, value)
        js("undefined")
    }

    // 原注释：Android 不强制标识符转义，所以这里走自定义 execSql。
    dbApi.prepareSql = { statement: dynamic -> dbPrepareSql(statement) }
    dbApi.compileSQL = { statement: dynamic -> dbCompileSql(statement) }
    dbApi.doQuery = { statement: dynamic -> dbDoQuery(statement) }
    dbApi.createStore = { name: dynamic, fields: dynamic, throwIfExists: dynamic ->
        dbCreateStore(name, fields, throwIfExists)
        js("undefined")
    }
    dbApi.deleteStore = { name: dynamic, ignoreIfExists: dynamic ->
        dbDeleteStore(name, ignoreIfExists)
        js("undefined")
    }
    dbApi.renameStore = { oldName: dynamic, newName: dynamic ->
        dbRenameStore(oldName, newName)
        js("undefined")
    }
    dbApi.migrateStore = { name: dynamic, changes: dynamic ->
        dbMigrateStore(name, changes)
        js("undefined")
    }
    dbApi.isStoreExists = { name: dynamic -> dbIsStoreExists(name) }

    val conflictAlgorithm = dbEmptyObject()
    conflictAlgorithm.none = ""
    conflictAlgorithm.rollback = " OR ROLLBACK"
    conflictAlgorithm.abort = " OR ABORT"
    conflictAlgorithm.fail = " OR FAIL"
    conflictAlgorithm.ignore = " OR IGNORE"
    conflictAlgorithm.replace = " OR REPLACE"
    dbApi.ConflictAlgorithm = conflictAlgorithm

    dbApi.clearStore = { name: dynamic -> dbClearStore(name) }
    dbApi.queryItems = { options: dynamic -> dbQueryItems(options) }
    dbApi.putItems = { options: dynamic -> dbPutItems(options) }
    dbApi.setItem = { options: dynamic -> dbSetItem(options) }
    dbApi.updateItems = { options: dynamic -> dbUpdateItems(options) }
    dbApi.deleteItems = { options: dynamic -> dbDeleteItems(options) }
    dbApi.checkFieldType = { value: dynamic -> dbCheckFieldType(value); js("undefined") }
    dbApi.createVersionStore = { dbCreateVersionStore(); js("undefined") }
    dbApi.getStoreVersion = { name: dynamic -> dbGetStoreVersion(name) }
    dbApi.setStoreVersion = { name: dynamic, version: dynamic -> dbSetStoreVersion(name, version); js("undefined") }
    dbApi.deleteStoreWithVersion = { name: dynamic -> dbDeleteStoreWithVersion(name); js("undefined") }
    dbApi.getUndefinedInstance = { cx: dynamic, scope: dynamic -> dbGetUndefinedInstance(cx, scope) }
    dbApi.createProxy = { target: dynamic, handler: dynamic -> dbCreateProxy(target, handler) }
    dbApi.ensureNamespace = { storeName: dynamic -> dbEnsureNamespace(storeName); js("undefined") }
    dbApi.allocateRefId = { storeName: dynamic -> dbAllocateRefId(storeName) }
    dbApi.convertToJSValue = { storeName: dynamic, dbTypeId: dynamic, dbValue: dynamic ->
        dbConvertToJSValue(storeName, dbTypeId, dbValue)
    }
    dbApi.convertToDBValue = { storeName: dynamic, jsValue: dynamic ->
        dbConvertToDBValue(storeName, jsValue)
    }
    dbApi.getNamespaceProxy = { storeName: dynamic, startId: dynamic, startType: dynamic ->
        dbGetNamespaceProxy(storeName, startId, startType)
    }
    dbApi.doNamespaceGC = { storeName: dynamic -> dbDoNamespaceGC(storeName); js("undefined") }
    dbApi.getNamespace = { storeName: dynamic -> dbGetNamespace(storeName) }

    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("DBSettings", dbApi)
        // 原文件末尾的副作用：把 "settings" 命名空间注册成全局 Settings。
        MapScript.loadModule("Settings", dbApi.getNamespace("settings"))
    }
}
