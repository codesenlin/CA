package ca.kotlin

/** modules/test/FileLogger.js 的 Kotlin/JS 版本（调试日志，构建时可能被裁剪）。 */
fun installFileLogger() {
    try {
        val fp = js("android.os.Environment").getExternalStorageDirectory().getAbsolutePath() + "/ca.debug.txt"
        val pw = js("new java.io.PrintWriter(fp)")
        js("Log").start({ tag: dynamic, data: dynamic ->
            try {
                pw.println(js("new Date()").toString() + " " + tag + "\n" + data)
                pw.flush()
            } catch (e: dynamic) { }
        })
    } catch (e: dynamic) {
        // Android 10+ 分区存储下根目录不可写；FileLogger 本身只是调试设施，不能阻断启动。
        js("Log").e(e)
    }
}
