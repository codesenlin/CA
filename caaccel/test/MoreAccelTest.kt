import kotlin.system.exitProcess
import top.projectxero.ca.accel.Accel

/** 新增加速点的等价性用例 */
object MoreAccelTest {
    @JvmStatic
    fun main(args: Array<String>) {
        // compareVersionArray：与 JS 实现逐项对齐
        check(Accel.compareVersionArray(intArrayOf(1, 2, 3), intArrayOf(1, 2, 10)) == -1, "1.2.3 < 1.2.10")
        check(Accel.compareVersionArray(intArrayOf(2, 0), intArrayOf(1, 9, 9)) == 1, "2.0 > 1.9.9")
        check(Accel.compareVersionArray(intArrayOf(1, 0), intArrayOf(1)) == 1, "1.0 > 1（缺位按 -1，0 > -1，与 JS 原实现一致）")
        check(Accel.compareVersionArray(intArrayOf(1), intArrayOf(1, 0)) == -1, "1 < 1.0（缺位 -1 < 0，与 JS 原实现一致）")
        check(Accel.compareVersionArray(intArrayOf(), intArrayOf()) == 0, "空数组相等")

        // classify：精确命中 / 模糊命中 / 不命中
        val keys = arrayOf<String?>("block", "block_state", "item", "minecraft:stone")
        val descs = arrayOf<String?>("方块", "方块状态", "物品", null)
        val f = Accel.classify(keys, descs, "block")
        check(f[0] == 1, "'block' 精确命中（ps == e）")
        check(f[1] == 2, "'block_state' 模糊命中（e 含 ps）")
        check(f[2] == 0, "'item' 不命中")
        check(f[3] == 0, "'minecraft:stone' 不命中")

        val f2 = Accel.classify(keys, descs, "block ")
        check(f2[0] == 1, "'block ' 精确命中（ps.startsWith(e + ' ')）")
        check(f2[1] == 0, "'block_state' 对 'block ' 不命中")

        val f3 = Accel.classify(keys, descs, "方块")
        check(f3[0] == 2 && f3[1] == 2, "描述命中")

        // classifyArray：数组 enum 分支（与 JS 实现逐项对齐）
        val arr = arrayOf<String?>("block", "block_state", "item", "minecraft:stone")
        val r1 = Accel.classifyArray(arr, "block")
        check(r1.flags[0] == 1 && r1.canFinish, "classifyArray 'block' 完整命中")
        check(r1.flags[1] == 2, "classifyArray 'block_state' 前缀命中")
        check(r1.flags[2] == 0, "classifyArray 'item' 不命中")
        check(r1.length == "block".length, "classifyArray 完整命中的 length 取 e.length")

        val r2 = Accel.classifyArray(arr, "block ")
        check(r2.flags[0] == 1 && r2.canFinish, "classifyArray 'block ' 完整命中")
        check(r2.flags[1] == 0, "classifyArray 'block ' 对 block_state 不命中")

        val r3 = Accel.classifyArray(arr, "i")
        check(r3.flags[2] == 2 && !r3.canFinish, "classifyArray 'i' 前缀命中")

        val r4 = Accel.classifyArray(arr, "")
        check(r4.flags[0] == 2 && r4.flags[1] == 2 && !r4.canFinish, "classifyArray 空前缀全部前缀命中")

        // filterIDTexts：缓存 ID 文本 + 前缀过滤（与 CA.filterIDList 的语义对齐）
        Accel.setIDTexts(arrayOf<String?>("minecraft:stone", "minecraft:dirt", "item:apple", null, "minecraft:stone_block"))
        val ids = Accel.filterIDTexts("minecraft")
        check(ids.size == 3 && ids[0] == 0 && ids[1] == 1 && ids[2] == 4, "filterIDTexts 前缀命中")
        val empty = Accel.filterIDTexts("")
        check(empty.size == 4, "filterIDTexts 空前缀跳过 null")
        check(Accel.filterIDTexts("zzz").isEmpty(), "filterIDTexts 无命中")

        // matchCommandNames：命令表缓存 + 名称/描述匹配 + 排序
        Accel.setCommandSnapshot(
            arrayOf<String?>("block", "block_state", "item", "minecraft:stone"),
            arrayOf<String?>("方块", "方块状态", "物品", "方块"),
        )
        val cmds = Accel.matchCommandNames("block")
        check(cmds.size == 2 && "block" == cmds[0] && "block_state" == cmds[1], "matchCommandNames 名称匹配并排序")
        check(Accel.matchCommandNames("方块").size == 3, "matchCommandNames 描述匹配")
        check(Accel.matchCommandNames("zzz").isEmpty(), "matchCommandNames 无命中")

        // filterTexts：建议列表缓存过滤
        Accel.setFilterTexts(arrayOf<String?>("minecraft:stone", "minecraft:dirt", "item:apple", null))
        val keep = Accel.filterTexts("minecraft")
        check(keep.size == 4 && keep[0] && keep[1] && !keep[2] && !keep[3], "filterTexts 命中与 null 处理")
        val keepAll = Accel.filterTexts("")
        check(keepAll.size == 4 && keepAll[0] && keepAll[1] && keepAll[2] && !keepAll[3], "filterTexts 空前缀")

        // matchCached：数组/对象两种模式与 matchString 等价
        val list = arrayListOf("apple", "banana", "apricot")
        Accel.setMatchSource(arrayOf<String?>("apple", "banana", "apricot"), null, false)
        val cr = Accel.matchCached("ap")
        check(cr.inputs.size == 2 && "apple" == cr.inputs[0] && "apricot" == cr.inputs[1], "matchCached 数组模式")
        check("apple" == cr.outputs["apple"], "matchCached 数组 output")

        val map = linkedMapOf("apple" to "苹果", "banana" to "香蕉", "apricot" to "杏")
        Accel.setMatchSource(
            arrayOf<String?>("apple", "banana", "apricot"),
            arrayOf<String?>("苹果", "香蕉", "杏"),
            true,
        )
        val cm = Accel.matchCached("ap")
        check(cm.outputs["apple - 苹果"] != null && cm.outputs["apricot - 杏"] != null, "matchCached 对象模式")

        // Result 数组访问器与 inputs/outputs 一致
        check(cr.inputArray().size == cr.inputs.size, "Result.inputArray")
        check(cr.outputKeys().size == cr.outputs.size && cr.outputValues().size == cr.outputs.size, "Result.outputKeys/Values")

        println("  all passed")
    }

    private fun check(ok: Boolean, name: String) {
        println("  " + (if (ok) "✓" else "✗") + " " + name)
        if (!ok) exitProcess(1)
    }
}
