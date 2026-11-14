import kotlin.system.exitProcess
import top.projectxero.ca.accel.Accel

/** matchString 的等价性用例：与 JS 回退实现逐项对齐 */
object MatchStringTest {
    @JvmStatic
    fun main(args: Array<String>) {
        // 用例 1：数组，前缀匹配
        val r1 = Accel.matchString("ap", listOf("apple", "apricot", "banana"))
        check(r1.inputs == listOf("apple", "apricot"), "数组匹配 inputs")
        check(r1.outputs.size == 2 && "apple" == r1.outputs["apple"], "数组匹配 outputs")

        // 用例 2：对象，key 或描述命中
        val m = linkedMapOf("apple" to "苹果", "banana" to "", "cherry" to "樱桃")
        val r2 = Accel.matchString("ap", m)
        check(r2.inputs == listOf("apple - 苹果"), "对象匹配：只命中 apple")
        check("apple" == r2.outputs["apple - 苹果"], "对象匹配：output 映射回 key")

        // 用例 3：描述命中
        val r3 = Accel.matchString("樱", m)
        check(r3.inputs == listOf("cherry - 樱桃"), "描述命中")

        // 用例 4：空前缀 → 全部命中
        val r4 = Accel.matchString("", listOf("a", "b"))
        check(r4.inputs == listOf("a", "b"), "空前缀全命中")

        // 用例 5：无命中
        val r5 = Accel.matchString("zzz", listOf("a", "b"))
        check(r5.inputs.isEmpty() && r5.outputs.isEmpty(), "无命中")

        // 用例 6：排序（带描述的在前，按字典序）
        val m2 = linkedMapOf("zebra" to "斑马", "apple" to "苹果")
        val r6 = Accel.matchString("", m2)
        check(r6.inputs == listOf("apple - 苹果", "zebra - 斑马"), "带描述的按字典序")

        println("  all passed")
    }

    private fun check(ok: Boolean, name: String) {
        println("  " + (if (ok) "✓" else "✗") + " " + name)
        if (!ok) exitProcess(1)
    }
}
