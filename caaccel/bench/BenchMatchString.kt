import java.util.ArrayList
import java.util.LinkedHashMap
import java.util.Locale
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.Scriptable
import top.projectxero.ca.accel.Accel

/**
 * matchString 的 JS(Rhino) 与 Kotlin/JVM 实现对比基准。
 *
 * 用同一份数据、同一批前缀，各跑 N 轮，比较总耗时。
 * 数据模拟真实补全场景：几千个候选值，其中一部分带描述。
 */
object BenchMatchString {
    private const val CANDIDATES = 3000
    private const val ROUNDS = 200
    private const val WARMUP = 20

    @JvmStatic
    fun main(args: Array<String>) {
        val list = ArrayList<String>()
        val map = LinkedHashMap<String, String>()
        for (i in 0 until CANDIDATES) {
            val key = "minecraft:" + String.format(Locale.ROOT, "block_%04d", i)
            list.add(key)
            map[key] = "方块 $i"
        }
        val prefixes = arrayOf("mine", "block_00", "block_01", "1", "zzz")

        // JS 侧：把 jsMatchString 原样喂给 Rhino
        val jsSrc = """
            function jsMatchString(ps, a, r) {
              var t, t2, t3;
              if (!(r instanceof Object)) r = {};
              if (!Array.isArray(r.input)) r.input = [];
              if (!(r.output instanceof Object)) r.output = {};
              if (Array.isArray(a)) {
                t = [];
                a.forEach(function(e) {
                  if (e.indexOf(ps) < 0) return;
                  r.output[e] = e;
                  if (r.input.indexOf(e) < 0) t.push(e);
                });
                t.sort();
                r.input = r.input.concat(t);
              } else {
                t = []; t2 = [];
                Object.keys(a).forEach(function(e) {
                  if (e.indexOf(ps) < 0 && a[e].indexOf(ps) < 0) return;
                  if (a[e]) {
                    t3 = e + ' - ' + a[e];
                    r.output[t3] = e;
                    if (r.input.indexOf(t3) < 0) t.push(t3);
                  } else {
                    r.output[e] = e;
                    if (r.input.indexOf(e) < 0) t2.push(e);
                  }
                });
                t.sort(); t2.sort();
                r.input = r.input.concat(t, t2);
              }
              return r;
            }
        """.trimIndent()

        val cx = Context.enter()
        cx.optimizationLevel = -1 // 和 App 里一致：纯解释模式
        val scope = cx.initStandardObjects()
        cx.evaluateString(scope, jsSrc, "bench", 1, null)
        val fnObj = scope.get("jsMatchString", scope) as Function

        // 构造 JS 侧用的原生数组 / 对象（App 里脚本传的就是 JS 原生结构）
        val jsList = cx.newArray(scope, list.toTypedArray<Any?>())
        val jsMapObj = cx.newObject(scope)
        for ((key, value) in map) {
            jsMapObj.put(key, jsMapObj, value)
        }

        // 预热
        for (i in 0 until WARMUP) {
            for (p in prefixes) {
                Accel.matchString(p, list)
                Accel.matchString(p, map)
                fnObj.call(cx, scope, scope, arrayOf<Any?>(p, jsList, null))
                fnObj.call(cx, scope, scope, arrayOf<Any?>(p, jsMapObj, null))
            }
        }

        var jsArr = 0L
        var jsMap = 0L
        var cpuArr = 0L
        var cpuMap = 0L
        for (i in 0 until ROUNDS) {
            for (p in prefixes) {
                var t0 = System.nanoTime()
                fnObj.call(cx, scope, scope, arrayOf<Any?>(p, jsList, null))
                jsArr += System.nanoTime() - t0

                t0 = System.nanoTime()
                Accel.matchString(p, list)
                cpuArr += System.nanoTime() - t0

                t0 = System.nanoTime()
                fnObj.call(cx, scope, scope, arrayOf<Any?>(p, jsMapObj, null))
                jsMap += System.nanoTime() - t0

                t0 = System.nanoTime()
                Accel.matchString(p, map)
                cpuMap += System.nanoTime() - t0
            }
        }
        Context.exit()

        val calls = ROUNDS * prefixes.size
        println(String.format(Locale.ROOT, "  候选 %d 个，前缀 %d 种，每种跑 %d 轮，共 %d 次调用", CANDIDATES, prefixes.size, ROUNDS, calls))
        println()
        println(String.format(Locale.ROOT, "  数组输入  JS(Rhino): %8.2f ms   Kotlin: %8.2f ms   加速 %.1fx", jsArr / 1e6, cpuArr / 1e6, jsArr.toDouble() / maxOf(1L, cpuArr)))
        println(String.format(Locale.ROOT, "  对象输入  JS(Rhino): %8.2f ms   Kotlin: %8.2f ms   加速 %.1fx", jsMap / 1e6, cpuMap / 1e6, jsMap.toDouble() / maxOf(1L, cpuMap)))
    }
}
