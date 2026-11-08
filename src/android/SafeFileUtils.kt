package ca.kotlin.android

import ca.kotlin.core.*


/**
 * modules/utils/SafeFileUtils.js 的 Kotlin/JS 版本。
 * 单例模块，所有调用都走 SafeFileUtils.xxx。
 */
private val safeApi: dynamic = js("({})")
private fun normOff(bytes: dynamic, off: dynamic): dynamic =
    if (off == null || js("Number").isNaN(off)) 0 else off
private fun normLen(bytes: dynamic, off: dynamic, len: dynamic): dynamic =
    if (len == null || js("Number").isNaN(len)) (bytes.length as Int) - (normOff(bytes, off) as Int) else len

fun installSafeFileUtils() {
    safeApi.read = { file: dynamic ->
        val safeFile = safeApi.getSafeFile(file)
        var result: dynamic = null
        try {
            if (safeFile.isFile()) result = safeApi.readUnsafe(safeFile)
        } catch (e: dynamic) { js("Log").e(e) }
        if (result == null) {
            try {
                if (file.isFile()) result = safeApi.readUnsafe(file)
            } catch (e: dynamic) { js("Log").e(e) }
        }
        result
    }
    safeApi.readText = { file: dynamic, defaultValue: dynamic ->
        val result = safeApi.read(file)
        if (result != null) safeApi.bytesToStr(result) else defaultValue
    }
    safeApi.readJSON = { file: dynamic, defaultValue: dynamic ->
        val result = safeApi.readText(file, null)
        if (result != null) {
            try {
                js("eval")("(" + result + ")")
            } catch (e: dynamic) { js("Log").e(e); defaultValue }
        } else defaultValue
    }
    safeApi.write = { file: dynamic, bytes: dynamic, off: dynamic, len: dynamic ->
        val safeFile = safeApi.getSafeFile(file)
        val fbytes = safeApi.gzipBytes(bytes, off, len)
        safeApi.writeBytes(safeFile, fbytes)
        safeApi.writeBytes(file, fbytes)
    }
    safeApi.writeText = { file: dynamic, str: dynamic -> safeApi.write(file, safeApi.strToBytes(str)) }
    safeApi.writeJSON = { file: dynamic, json: dynamic -> safeApi.writeText(file, MapScript.toSource(json)) }
    safeApi.delete = { file: dynamic ->
        val safeFile = safeApi.getSafeFile(file)
        var ok = true
        if (file.exists() && !file.delete()) ok = false
        if (safeFile.exists() && !safeFile.delete()) ok = false
        ok
    }
    safeApi.getSafeFile = { file: dynamic -> js("new java.io.File(file.getPath() + '.new')") }
    safeApi.readUnsafe = { file: dynamic ->
        val bufferSize = 2048
        val stream = js("new java.util.zip.GZIPInputStream(new java.io.FileInputStream(file))")
        val os = js("new java.io.ByteArrayOutputStream()")
        val buf = js("java.lang.reflect.Array").newInstance(js("java.lang.Byte").TYPE, bufferSize)
        var hr: dynamic
        do {
            hr = stream.read(buf)
            if (hr > 0) os.write(buf, 0, hr)
        } while (hr > 0)
        stream.close()
        os.toByteArray()
    }
    safeApi.writeUnsafe = { file: dynamic, bytes: dynamic, off: dynamic, len: dynamic ->
        val o = normOff(bytes, off)
        val l = normLen(bytes, o, len)
        val stream = js("new java.util.zip.GZIPOutputStream(new java.io.FileOutputStream(file))")
        stream.write(bytes, o, l)
        stream.close()
    }
    safeApi.readBytes = { file: dynamic ->
        val bufferSize = 2048
        val stream = js("new java.io.FileInputStream(file)")
        val os = js("new java.io.ByteArrayOutputStream()")
        val buf = js("java.lang.reflect.Array").newInstance(js("java.lang.Byte").TYPE, bufferSize)
        var hr: dynamic
        do {
            hr = stream.read(buf)
            if (hr > 0) os.write(buf, 0, hr)
        } while (hr > 0)
        stream.close()
        os.toByteArray()
    }
    safeApi.writeBytes = { file: dynamic, bytes: dynamic, off: dynamic, len: dynamic ->
        val o = normOff(bytes, off)
        val l = normLen(bytes, o, len)
        val stream = js("new java.io.FileOutputStream(file)")
        stream.write(bytes, o, l)
        stream.close()
    }
    safeApi.gzipBytes = { bytes: dynamic, off: dynamic, len: dynamic ->
        val o = normOff(bytes, off)
        val l = normLen(bytes, o, len)
        val bs = js("new java.io.ByteArrayOutputStream()")
        val stream = js("new java.util.zip.GZIPOutputStream(bs)")
        stream.write(bytes, o, l)
        stream.close()
        bs.toByteArray()
    }
    safeApi.ungzipBytes = { bytes: dynamic, off: dynamic, len: dynamic ->
        val o = normOff(bytes, off)
        val l = normLen(bytes, o, len)
        val bufferSize = 2048
        val ins = js("new java.util.zip.GZIPInputStream(new java.io.ByteArrayInputStream(bytes, o, l))")
        val os = js("new java.io.ByteArrayOutputStream()")
        val buf = js("java.lang.reflect.Array").newInstance(js("java.lang.Byte").TYPE, bufferSize)
        var hr: dynamic
        do {
            hr = ins.read(buf)
            if (hr > 0) os.write(buf, 0, hr)
        } while (hr > 0)
        ins.close()
        os.toByteArray()
    }
    safeApi.strToBytes = { str: dynamic, charset: dynamic ->
        val s = js("new java.lang.String(str)")
        if (charset != null) s.getBytes(charset) else s.getBytes()
    }
    safeApi.bytesToStr = { bytes: dynamic, off: dynamic, len: dynamic, charset: dynamic ->
        val o = normOff(bytes, off)
        val l = normLen(bytes, o, len)
        if (charset != null) js("new java.lang.String(bytes, o, l, charset)").toString()
        else js("new java.lang.String(bytes, o, l)").toString()
    }

    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("SafeFileUtils", safeApi)
    }
}
