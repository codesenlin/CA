package ca.kotlin.core


/** modules/builtinData.js 的 Kotlin/JS 版本。数据仍由 Loader.fromFile 在构建期内联。 */
fun installBuiltinData() {
    val ca = js("CA")
    val lib = ca.Library
    lib.inner["default"] = Loader.fromFile("../../../data/idlibs/default.json")
    lib.inner["addition"] = Loader.fromFile("../../../data/idlibs/addition.json")
    lib.inner["education"] = Loader.fromFile("../../../data/idlibs/education.json")
    lib.inner["experiment"] = Loader.fromFile("../../../data/idlibs/experiment.json")
    lib.inner["basicedu"] = Loader.fromFile("../../../data/idlibs/basicedu.json")
    lib.inner["JSONSchema"] = Loader.fromFile("../../../data/idlibs/JSONSchema.json")
    lib.inner["states"] = Loader.fromFile("../../../data/idlibs/states.json")

    val manifest = Loader.fromFile("../../../data/builtinLibs.json")
    val recommended = js("[]")
    val defaultDisabled = js("[]")
    val libs = manifest.libraries
    var i = 0
    val libCount = libs.length as Int
    while (i < libCount) {
        val entry = libs[i]
        val id = entry.id
        if (lib.inner[id] != null) {
            if (entry["default"] == "disabled") defaultDisabled.push(id) else recommended.push(id)
        }
        i++
    }
    val keys = js("Object").keys(lib.inner)
    i = 0
    val keyCount = keys.length as Int
    while (i < keyCount) {
        val id = keys[i]
        if (!(recommended.indexOf(id) >= 0) && !(defaultDisabled.indexOf(id) >= 0)) defaultDisabled.push(id)
        i++
    }
    lib.recommended = recommended
    lib.defaultDisabled = defaultDisabled

    val common = js("Common")
    val list = Loader.fromFile("../../../data/theme.json")
    val themeKeys = js("Object").keys(list)
    i = 0
    val themeCount = themeKeys.length as Int
    while (i < themeCount) {
        val k = themeKeys[i]
        val item = list[k]
        if (item.nameKey != null) {
            item.name = js("Intl").get(item.nameKey)
            js("function(o, k) { delete o[k]; }")(item, "nameKey")
        }
        i++
    }
    val flavor = js("BuildConfig").flavorData
    val order = if (flavor != null) flavor.themeOrder else null
    if (js("Array.isArray")(order)) {
        val ordered = js("({})")
        i = 0
        val orderCount = order.length as Int
        while (i < orderCount) {
            val id = order[i]
            if (list[id] != null) ordered[id] = list[id]
            i++
        }
        val all = js("Object").keys(list)
        i = 0
        val allCount = all.length as Int
        while (i < allCount) {
            val id = all[i]
            if (!js("function(o, k) { return k in o; }")(ordered, id)) ordered[id] = list[id]
            i++
        }
        common.themelist = ordered
    } else {
        common.themelist = list
    }

    ca.tips = Loader.fromFile("../../../data/tips.json")
    ca.defalutTips = ca.tips
}
