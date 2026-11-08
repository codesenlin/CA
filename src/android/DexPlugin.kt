package ca.kotlin

/** modules/core/DexPlugin.js 的 Kotlin/JS 版本。 */
private val dexApi: dynamic = js("({})")
private val dexBridge: dynamic = js("({})")

fun installDexPlugin() {
    dexBridge.get = { receiver: dynamic, className: dynamic ->
        var cls: dynamic = null
        if (className != null) {
            if (js("className in receiver.classes")) {
                cls = receiver.classes[className]
            } else {
                try {
                    val cx = js("org.mozilla.javascript.Context").getCurrentContext()
                    cls = cx.getWrapFactory().wrapJavaClass(cx, MapScript.global, receiver.classLoader.loadClass(className.toString()))
                } catch (e: dynamic) { js("Log").e(e) }
                receiver.classes[className] = cls
            }
        } else {
            cls = receiver.mainClass
        }
        cls
    }

    dexApi.load = { packageName: dynamic, mainClass: dynamic ->
        var pi: dynamic = null
        var cl: dynamic = null
        try {
            pi = js("ctx").getPackageManager().getPackageInfo(packageName, 0)
            val dir = pi.applicationInfo.publicSourceDir
            val cx = js("org.mozilla.javascript.Context").getCurrentContext()
            cl = js("Packages.dalvik.system.DexClassLoader")(dir, js("ctx").getDir("dex", 0).getAbsolutePath(), null, cx.getApplicationClassLoader())
        } catch (e: dynamic) { js("Log").e(e) }
        var r: dynamic = null
        if (cl != null) {
            r = js("Object").create(dexApi)
            r.classes = js("({})")
            r.packageInfo = pi
            r.classLoader = cl
            if (mainClass != null) r.mainClass = r.get(mainClass)
        }
        r
    }
    dexApi.get = js("function(className) { return globalThis.__caDex.get(this, className); }")
    js("globalThis").__caDex = dexBridge

    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("DexPlugin", dexApi)
    }
}
