package top.projectxero.ca.accel

import java.util.ArrayList
import java.util.Collections
import java.util.LinkedHashMap

/**
 * 加速库入口。
 *
 * 设计原则：
 * 1. 纯 Kotlin/JVM，不依赖 Android、不依赖 Rhino
 * 2. 所有方法都是 static、无状态，Rhino 直接 Callable 调用
 * 3. 每个方法在 JS 侧都有等价实现，jar 不存在时自动回退
 */
class Accel private constructor() {
    companion object {
        private var matchKeys: Array<String?>? = null
        private var matchDescs: Array<String?>? = null
        private var matchIsMap = false

        private var filterTextsCache: Array<String?>? = null
        private var commandKeys: Array<String?>? = null
        private var commandDescs: Array<String?>? = null
        private var idTexts: Array<String?>? = null

        /** 供 JS 探测版本，回退实现与加速实现共用同一套约定 */
        @JvmStatic
        fun version(): String = "1.0.0"

        /** 缓存 matchString 的候选源：数组模式 descs 传 null，对象模式 descs 为描述 */
        @JvmStatic
        fun setMatchSource(keys: Array<String?>?, descs: Array<String?>?, isMap: Boolean) {
            matchKeys = keys
            matchDescs = descs
            matchIsMap = isMap
        }

        /** 在缓存的候选源上做 matchString；与 matchString(ps, a) 结果等价 */
        @JvmStatic
        fun matchCached(ps: String?): Result {
            val r = Result()
            val keys = matchKeys ?: return r
            val p = ps ?: ""
            if (!matchIsMap) {
                val hits = ArrayList<String>()
                for (e in keys) {
                    if (e == null || e.indexOf(p) < 0) continue
                    r.outputs[e] = e
                    hits.add(e)
                }
                Collections.sort(hits)
                r.inputs.addAll(hits)
                return r
            }
            val descs = matchDescs
            val withDesc = ArrayList<String>()
            val plain = ArrayList<String>()
            for (i in keys.indices) {
                val key = keys[i] ?: continue
                val value = if (descs != null && i < descs.size && descs[i] != null) descs[i]!! else ""
                if (key.indexOf(p) < 0 && value.indexOf(p) < 0) continue
                if (value.isNotEmpty()) {
                    val display = "$key - $value"
                    r.outputs[display] = key
                    withDesc.add(display)
                } else {
                    r.outputs[key] = key
                    plain.add(key)
                }
            }
            Collections.sort(withDesc)
            Collections.sort(plain)
            r.inputs.addAll(withDesc)
            r.inputs.addAll(plain)
            return r
        }

        @JvmStatic
        fun matchString(ps: String?, a: Any?): Result {
            val r = Result()
            val p = ps ?: ""
            if (a is Map<*, *>) {
                val withDesc = ArrayList<String>()
                val plain = ArrayList<String>()
                for (entry in a.entries) {
                    val key = entry.key.toString()
                    val value = entry.value?.toString() ?: ""
                    if (key.indexOf(p) < 0 && value.indexOf(p) < 0) continue
                    if (value.isNotEmpty()) {
                        val display = "$key - $value"
                        r.outputs[display] = key
                        withDesc.add(display)
                    } else {
                        r.outputs[key] = key
                        plain.add(key)
                    }
                }
                Collections.sort(withDesc)
                Collections.sort(plain)
                r.inputs.addAll(withDesc)
                r.inputs.addAll(plain)
            } else if (a is Iterable<*>) {
                val hits = ArrayList<String>()
                for (item in a) {
                    val e = item.toString()
                    if (e.indexOf(p) < 0) continue
                    r.outputs[e] = e
                    hits.add(e)
                }
                Collections.sort(hits)
                r.inputs.addAll(hits)
            }
            return r
        }

        @JvmStatic
        fun setFilterTexts(texts: Array<String?>?) {
            filterTextsCache = texts
        }

        /** 在缓存的文本里标记包含 s 的元素；没有缓存返回空掩码 */
        @JvmStatic
        fun filterTexts(s: String?): BooleanArray {
            val texts = filterTextsCache ?: return BooleanArray(0)
            val keep = BooleanArray(texts.size)
            val p = s ?: ""
            for (i in texts.indices) {
                keep[i] = texts[i] != null && texts[i]!!.contains(p)
            }
            return keep
        }

        @JvmStatic
        fun setCommandSnapshot(keys: Array<String?>?, descs: Array<String?>?) {
            commandKeys = keys
            commandDescs = descs
        }

        /** 匹配命令名/描述中包含 prefix 的命令，按名称排序返回 */
        @JvmStatic
        fun matchCommandNames(prefix: String?): Array<String> {
            val keys = commandKeys ?: return emptyArray()
            val descs = commandDescs
            val p = prefix ?: ""
            val out = ArrayList<String>()
            for (i in keys.indices) {
                val key = keys[i] ?: continue
                val desc = if (descs != null && i < descs.size && descs[i] != null) descs[i]!! else ""
                if (key.contains(p) || desc.contains(p)) out.add(key)
            }
            Collections.sort(out)
            return out.toTypedArray()
        }

        @JvmStatic
        fun setIDTexts(texts: Array<String?>?) {
            idTexts = texts
        }

        /** 生成 [0, n) 的下标数组；ID 列表空前缀直接复用，避免在 Rhino 里循环构建 */
        @JvmStatic
        fun rangeIndices(n: Int): IntArray {
            if (n <= 0) return IntArray(0)
            return IntArray(n) { it }
        }

        /**
         * 在缓存的 ID 文本里查找包含 s 的下标；没有缓存/无匹配返回空数组。
         */
        @JvmStatic
        fun filterIDTexts(s: String?): IntArray {
            val texts = idTexts ?: return IntArray(0)
            val p = s ?: ""
            val out = IntArray(texts.size)
            var count = 0
            for (i in texts.indices) {
                if (texts[i] != null && texts[i]!!.contains(p)) out[count++] = i
            }
            if (count == out.size) return out
            return out.copyOf(count)
        }

        /**
         * 批量过滤：保留 arr 中包含 s 的元素，返回下标掩码。
         */
        @JvmStatic
        fun filter(arr: Array<String?>?, s: String?): BooleanArray {
            if (arr == null) return BooleanArray(0)
            val keep = BooleanArray(arr.size)
            val p = s ?: ""
            for (i in arr.indices) {
                keep[i] = arr[i] != null && arr[i]!!.contains(p)
            }
            return keep
        }

        /** 版本号数组比较。对应 JS: NeteaseAdapter.compareVersion。 */
        @JvmStatic
        fun compareVersionArray(a: IntArray?, b: IntArray?): Int {
            val n = maxOf(a?.size ?: 0, b?.size ?: 0)
            for (i in 0 until n) {
                val p1 = if (a != null && i < a.size) a[i] else -1
                val p2 = if (b != null && i < b.size) b[i] else -1
                if (p1 < p2) return -1
                if (p1 > p2) return 1
            }
            return 0
        }

        /**
         * 枚举候选分类。对应 CA.IntelliSense 的 enum 对象分支里那段判定。
         */
        @JvmStatic
        fun classify(keys: Array<String?>?, descs: Array<String?>?, ps: String?): IntArray {
            val n = keys?.size ?: 0
            val flags = IntArray(n)
            val p = ps ?: ""
            for (i in 0 until n) {
                val k = keys!![i] ?: continue
                if (p == k || p.startsWith("$k ")) {
                    flags[i] = 1
                    continue
                }
                val d = if (descs != null && i < descs.size && descs[i] != null) descs[i]!! else ""
                if (k.contains(p) || d.contains(p)) flags[i] = 2
            }
            return flags
        }

        /**
         * 数组形式的枚举候选匹配（CA.IntelliSense 的 enum 数组分支）。
         */
        @JvmStatic
        fun classifyArray(arr: Array<String?>?, ps: String?): ArrayClassifyResult {
            val n = arr?.size ?: 0
            val flags = IntArray(n)
            var length = -1
            var canFinish = false
            val p = ps ?: ""
            for (i in 0 until n) {
                val e = arr!![i] ?: continue
                if (p == e || p.startsWith("$e ")) {
                    flags[i] = 1
                    if (e.length > length) length = e.length
                    canFinish = true
                } else if (e.startsWith(p)) {
                    flags[i] = 2
                    if (p.length > length) length = p.length
                }
            }
            return ArrayClassifyResult(flags, length, canFinish)
        }
    }

    /** classifyArray 的结果 */
    class ArrayClassifyResult(
        @JvmField val flags: IntArray,
        @JvmField val length: Int,
        @JvmField val canFinish: Boolean,
    )

    /** matchString 的结果：新增的候选与新增的显示映射 */
    class Result {
        @JvmField val inputs: MutableList<String> = ArrayList()
        @JvmField val outputs: MutableMap<String, String> = LinkedHashMap()

        fun inputArray(): Array<String> = inputs.toTypedArray()
        fun outputKeys(): Array<String> = outputs.keys.toTypedArray()
        fun outputValues(): Array<String> = outputs.values.toTypedArray()
    }
}
