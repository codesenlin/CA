import top.projectxero.ca.accel.Accel;
import java.util.*;

/** 新增加速点的等价性用例 */
public class MoreAccelTest {
    public static void main(String[] a) {
        // compareVersionArray：与 JS 实现逐项对齐
        check(Accel.compareVersionArray(new int[]{1,2,3}, new int[]{1,2,10}) == -1, "1.2.3 < 1.2.10");
        check(Accel.compareVersionArray(new int[]{2,0}, new int[]{1,9,9}) == 1, "2.0 > 1.9.9");
        check(Accel.compareVersionArray(new int[]{1,0}, new int[]{1}) == 1, "1.0 > 1（缺位按 -1，0 > -1，与 JS 原实现一致）");
        check(Accel.compareVersionArray(new int[]{1}, new int[]{1,0}) == -1, "1 < 1.0（缺位 -1 < 0，与 JS 原实现一致）");
        check(Accel.compareVersionArray(new int[]{}, new int[]{}) == 0, "空数组相等");

        // classify：精确命中 / 模糊命中 / 不命中
        String[] keys = {"block", "block_state", "item", "minecraft:stone"};
        String[] descs = {"方块", "方块状态", "物品", null};
        int[] f = Accel.classify(keys, descs, "block");
        check(f[0] == 1, "'block' 精确命中（ps == e）");
        check(f[1] == 2, "'block_state' 模糊命中（e 含 ps）");
        check(f[2] == 0, "'item' 不命中");
        check(f[3] == 0, "'minecraft:stone' 不命中");

        int[] f2 = Accel.classify(keys, descs, "block ");
        check(f2[0] == 1, "'block ' 精确命中（ps.startsWith(e + ' ')）");
        check(f2[1] == 0, "'block_state' 对 'block ' 不命中");

        int[] f3 = Accel.classify(keys, descs, "方块");
        check(f3[0] == 2 && f3[1] == 2, "描述命中");

        // classifyArray：数组 enum 分支（与 JS 实现逐项对齐）
        String[] arr = {"block", "block_state", "item", "minecraft:stone"};
        Accel.ArrayClassifyResult r1 = Accel.classifyArray(arr, "block");
        check(r1.flags[0] == 1 && r1.canFinish, "classifyArray 'block' 完整命中");
        check(r1.flags[1] == 2, "classifyArray 'block_state' 前缀命中");
        check(r1.flags[2] == 0, "classifyArray 'item' 不命中");
        check(r1.length == "block".length(), "classifyArray 完整命中的 length 取 e.length");

        Accel.ArrayClassifyResult r2 = Accel.classifyArray(arr, "block ");
        check(r2.flags[0] == 1 && r2.canFinish, "classifyArray 'block ' 完整命中");
        check(r2.flags[1] == 0, "classifyArray 'block ' 对 block_state 不命中");

        Accel.ArrayClassifyResult r3 = Accel.classifyArray(arr, "i");
        check(r3.flags[2] == 2 && !r3.canFinish, "classifyArray 'i' 前缀命中");

        Accel.ArrayClassifyResult r4 = Accel.classifyArray(arr, "");
        check(r4.flags[0] == 2 && r4.flags[1] == 2 && !r4.canFinish, "classifyArray 空前缀全部前缀命中");

        // filterIDTexts：缓存 ID 文本 + 前缀过滤（与 CA.filterIDList 的语义对齐）
        Accel.setIDTexts(new String[]{"minecraft:stone", "minecraft:dirt", "item:apple", null, "minecraft:stone_block"});
        int[] ids = Accel.filterIDTexts("minecraft");
        check(ids.length == 3 && ids[0] == 0 && ids[1] == 1 && ids[2] == 4, "filterIDTexts 前缀命中");
        int[] empty = Accel.filterIDTexts("");
        check(empty.length == 4, "filterIDTexts 空前缀跳过 null");
        check(Accel.filterIDTexts("zzz").length == 0, "filterIDTexts 无命中");

        // matchCommandNames：命令表缓存 + 名称/描述匹配 + 排序
        Accel.setCommandSnapshot(
            new String[]{"block", "block_state", "item", "minecraft:stone"},
            new String[]{"方块", "方块状态", "物品", "方块"});
        String[] cmds = Accel.matchCommandNames("block");
        check(cmds.length == 2 && "block".equals(cmds[0]) && "block_state".equals(cmds[1]), "matchCommandNames 名称匹配并排序");
        check(Accel.matchCommandNames("方块").length == 3, "matchCommandNames 描述匹配");
        check(Accel.matchCommandNames("zzz").length == 0, "matchCommandNames 无命中");

        // filterTexts：建议列表缓存过滤
        Accel.setFilterTexts(new String[]{"minecraft:stone", "minecraft:dirt", "item:apple", null});
        boolean[] keep = Accel.filterTexts("minecraft");
        check(keep.length == 4 && keep[0] && keep[1] && !keep[2] && !keep[3], "filterTexts 命中与 null 处理");
        boolean[] keepAll = Accel.filterTexts("");
        check(keepAll.length == 4 && keepAll[0] && keepAll[1] && keepAll[2] && !keepAll[3], "filterTexts 空前缀");

        // matchCached：数组/对象两种模式与 matchString 等价
        java.util.List<String> list = new java.util.ArrayList<String>();
        list.add("apple"); list.add("banana"); list.add("apricot");
        Accel.setMatchSource(new String[]{"apple", "banana", "apricot"}, null, false);
        Accel.Result cr = Accel.matchCached("ap");
        check(cr.inputs.size() == 2 && "apple".equals(cr.inputs.get(0)) && "apricot".equals(cr.inputs.get(1)), "matchCached 数组模式");
        check("apple".equals(cr.outputs.get("apple")), "matchCached 数组 output");

        java.util.Map<String, String> map = new java.util.LinkedHashMap<String, String>();
        map.put("apple", "苹果"); map.put("banana", "香蕉"); map.put("apricot", "杏");
        Accel.setMatchSource(new String[]{"apple", "banana", "apricot"}, new String[]{"苹果", "香蕉", "杏"}, true);
        Accel.Result cm = Accel.matchCached("ap");
        check(cm.outputs.get("apple - 苹果") != null && cm.outputs.get("apricot - 杏") != null, "matchCached 对象模式");

        // Result 数组访问器与 inputs/outputs 一致
        check(cr.inputArray().length == cr.inputs.size(), "Result.inputArray");
        check(cr.outputKeys().length == cr.outputs.size() && cr.outputValues().length == cr.outputs.size(), "Result.outputKeys/Values");

        System.out.println("  all passed");
    }

    static void check(boolean ok, String name) {
        System.out.println("  " + (ok ? "✓" : "✗") + " " + name);
        if (!ok) System.exit(1);
    }
}
