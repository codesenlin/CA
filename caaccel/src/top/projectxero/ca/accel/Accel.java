package top.projectxero.ca.accel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 加速库入口。
 *
 * 设计原则：
 * 1. 纯 Java，不依赖 Android、不依赖 Rhino —— 可以单独在 JVM 上跑单测
 * 2. 所有方法都是 static、无状态，Rhino 直接 Callable 调用
 * 3. 每个方法在 JS 侧都有等价实现，jar 不存在时自动回退
 *
 * 版本：{ VERSION } 由构建脚本注入
 */
public final class Accel {
    private Accel() {}

    /** 供 JS 探测版本，回退实现与加速实现共用同一套约定 */
    public static String version() {
        return "1.0.0";
    }

/**
     * 补全的字符串匹配。对应 JS: CA.IntelliSense.matchString。
     *
     * 输入的 a 支持两种形态：
     *   - List&lt;String&gt;：候选值列表
     *   - Map&lt;String,String&gt;：候选值 → 描述
     * 返回匹配到的新增条目，由 JS 侧合并进 r.input / r.output，
     * 这样调用方拿到的 r 与纯 JS 实现完全一致。
     *
     * 相比 JS 版的收益：JS 版用 indexOf 去重是 O(n²)，这里用 LinkedHashSet 是 O(n)。
     */
    private static String[] matchKeys = null;
    private static String[] matchDescs = null;
    private static boolean matchIsMap = false;

    /** 缓存 matchString 的候选源：数组模式 descs 传 null，对象模式 descs 为描述 */
    public static void setMatchSource(String[] keys, String[] descs, boolean isMap) {
        matchKeys = keys;
        matchDescs = descs;
        matchIsMap = isMap;
    }

    /** 在缓存的候选源上做 matchString；与 matchString(ps, a) 结果等价 */
    public static Result matchCached(String ps) {
        Result r = new Result();
        String[] keys = matchKeys;
        if (keys == null) return r;
        String p = ps == null ? "" : ps;
        if (!matchIsMap) {
            List<String> hits = new ArrayList<String>();
            for (int i = 0; i < keys.length; i++) {
                String e = keys[i];
                if (e == null || e.indexOf(p) < 0) continue;
                r.outputs.put(e, e);
                hits.add(e);
            }
            Collections.sort(hits);
            r.inputs.addAll(hits);
            return r;
        }
        String[] descs = matchDescs;
        List<String> withDesc = new ArrayList<String>();
        List<String> plain = new ArrayList<String>();
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            if (key == null) continue;
            String val = (descs != null && i < descs.length && descs[i] != null) ? descs[i] : "";
            if (key.indexOf(p) < 0 && val.indexOf(p) < 0) continue;
            if (val.length() > 0) {
                String display = key + " - " + val;
                r.outputs.put(display, key);
                withDesc.add(display);
            } else {
                r.outputs.put(key, key);
                plain.add(key);
            }
        }
        Collections.sort(withDesc);
        Collections.sort(plain);
        r.inputs.addAll(withDesc);
        r.inputs.addAll(plain);
        return r;
    }

    public static Result matchString(String ps, Object a) {
        Result r = new Result();
        String p = ps == null ? "" : ps;
        if (a instanceof Map) {
            List<String> withDesc = new ArrayList<String>();
            List<String> plain = new ArrayList<String>();
            for (Object o : ((Map<?, ?>) a).entrySet()) {
                Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
                String key = String.valueOf(e.getKey());
                String val = e.getValue() == null ? "" : String.valueOf(e.getValue());
                if (key.indexOf(p) < 0 && val.indexOf(p) < 0) continue;
                if (val.length() > 0) {
                    String display = key + " - " + val;
                    r.outputs.put(display, key);
                    withDesc.add(display);
                } else {
                    r.outputs.put(key, key);
                    plain.add(key);
                }
            }
            Collections.sort(withDesc);
            Collections.sort(plain);
            r.inputs.addAll(withDesc);
            r.inputs.addAll(plain);
        } else if (a instanceof Iterable) {
            List<String> hits = new ArrayList<String>();
            for (Object o : (Iterable<?>) a) {
                String e = String.valueOf(o);
                if (e.indexOf(p) < 0) continue;
                r.outputs.put(e, e);
                hits.add(e);
            }
            Collections.sort(hits);
            r.inputs.addAll(hits);
        }
        return r;
    }

/** 通用文本过滤缓存：CA.Assist 建议列表每次输入不再传整个数组 */
    private static String[] filterTextsCache = null;

    public static void setFilterTexts(String[] texts) {
        filterTextsCache = texts;
    }

    /** 在缓存的文本里标记包含 s 的元素；没有缓存返回空掩码 */
    public static boolean[] filterTexts(String s) {
        String[] texts = filterTextsCache;
        boolean[] keep = new boolean[texts == null ? 0 : texts.length];
        if (texts == null) return keep;
        String p = s == null ? "" : s;
        for (int i = 0; i < texts.length; i++) {
            keep[i] = texts[i] != null && texts[i].contains(p);
        }
        return keep;
    }

    /** 命令名/描述缓存：命令库重建时同步一次，之后只传前缀（CA.IntelliSense 用） */
    private static String[] commandKeys = null;
    private static String[] commandDescs = null;

    /** 同步命令快照；keys 为命令名，descs 为对应描述（可为 null） */
    public static void setCommandSnapshot(String[] keys, String[] descs) {
        commandKeys = keys;
        commandDescs = descs;
    }

    /** 匹配命令名/描述中包含 prefix 的命令，按名称排序返回 */
    public static String[] matchCommandNames(String prefix) {
        String[] keys = commandKeys;
        String[] descs = commandDescs;
        if (keys == null) return new String[0];
        String p = prefix == null ? "" : prefix;
        List<String> out = new ArrayList<String>();
        for (int i = 0; i < keys.length; i++) {
            String k = keys[i];
            if (k == null) continue;
            String d = (descs != null && i < descs.length && descs[i] != null) ? descs[i] : "";
            if (k.contains(p) || d.contains(p)) out.add(k);
        }
        Collections.sort(out);
        return out.toArray(new String[0]);
    }

    /** ID 列表文本缓存：buildIDList 时同步一次，之后只传前缀（CA.showIDList 用） */
    private static String[] idTexts = null;

    /** 同步 ID 列表文本；列表重建时由 JS 侧重新调用 */
    public static void setIDTexts(String[] texts) {
        idTexts = texts;
    }

    /** 生成 [0, n) 的下标数组；ID 列表空前缀直接复用，避免在 Rhino 里循环构建 */
    public static int[] rangeIndices(int n) {
        if (n <= 0) return new int[0];
        int[] out = new int[n];
        for (int i = 0; i < n; i++) out[i] = i;
        return out;
    }

    /**
     * 在缓存的 ID 文本里查找包含 s 的下标；没有缓存/无匹配返回空数组。
     * 相比 filter(String[] arr, String s) 省掉每次调用都从 Rhino 转 4000+ 字符串的开销。
     */
    public static int[] filterIDTexts(String s) {
        String[] texts = idTexts;
        if (texts == null) return new int[0];
        String p = s == null ? "" : s;
        // 单遍扫描：先按全量预分配，最后再裁到命中数，避免为了精确分配扫两遍
        int[] out = new int[texts.length];
        int i, count = 0;
        for (i = 0; i < texts.length; i++) {
            if (texts[i] != null && texts[i].contains(p)) out[count++] = i;
        }
        if (count == out.length) return out;
        int[] trimmed = new int[count];
        System.arraycopy(out, 0, trimmed, 0, count);
        return trimmed;
    }

    /**
     * 批量过滤：保留 arr 中包含 s 的元素，返回下标掩码。
     *
     * 对应 JS 侧的 setFilter(function(e, i) { return e.indexOf(s) >= 0; })：
     * 语义完全一样，但字符串查找在 Java 里一次做完，JS 侧只剩 keep[i] 取下标，
     * 避免每个元素都从 Java 回调进 Rhino 做一次 String.indexOf。
     */
    public static boolean[] filter(String[] arr, String s) {
        boolean[] keep = new boolean[arr == null ? 0 : arr.length];
        if (arr == null) return keep;
        String p = s == null ? "" : s;
        for (int i = 0; i < arr.length; i++) {
            String e = arr[i];
            keep[i] = e != null && e.contains(p);
        }
        return keep;
    }

/**
     * 版本号数组比较。对应 JS: NeteaseAdapter.compareVersion。
     * 非数字段按 -1 处理（与原实现一致）。
     */
    public static int compareVersionArray(int[] a, int[] b) {
        int n = Math.max(a == null ? 0 : a.length, b == null ? 0 : b.length);
        for (int i = 0; i < n; i++) {
            int p1 = (a != null && i < a.length) ? a[i] : -1;
            int p2 = (b != null && i < b.length) ? b[i] : -1;
            if (p1 < p2) return -1;
            if (p1 > p2) return 1;
        }
        return 0;
    }

    /**
     * 枚举候选分类。对应 CA.IntelliSense 的 enum 对象分支里那段判定：
     *
     *   if (ps.startsWith(e + " ") || ps == e)      -> 精确命中（可结束），位 1
     *   else if (e.indexOf(ps) >= 0 || t[e].indexOf(ps) >= 0) -> 模糊命中，位 2
     *
     * 返回与 keys 等长的标志数组，JS 侧只做位测试，不再碰字符串。
     */
    public static int[] classify(String[] keys, String[] descs, String ps) {
        int n = keys == null ? 0 : keys.length;
        int[] flags = new int[n];
        String p = ps == null ? "" : ps;
        for (int i = 0; i < n; i++) {
            String k = keys[i];
            if (k == null) continue;
            if (p.equals(k) || p.startsWith(k + " ")) {
                flags[i] = 1;
                continue;
            }
            String d = (descs != null && i < descs.length && descs[i] != null) ? descs[i] : "";
            if (k.contains(p) || d.contains(p)) flags[i] = 2;
        }
        return flags;
    }

    /**
     * 数组形式的枚举候选匹配（CA.IntelliSense 的 enum 数组分支）。
     * 返回标志数组 + 匹配到的最大长度 + 是否可结束，JS 侧只负责拼 label。
     *
     * flags: 1 = ps 完整命中（ps == e 或 ps.startsWith(e + " ")，可结束）
     *        2 = 前缀命中（e.startsWith(ps)）
     *        0 = 不命中
     */
    public static ArrayClassifyResult classifyArray(String[] arr, String ps) {
        int n = arr == null ? 0 : arr.length;
        int[] flags = new int[n];
        int length = -1;
        boolean canFinish = false;
        String p = ps == null ? "" : ps;
        for (int i = 0; i < n; i++) {
            String e = arr[i];
            if (e == null) continue;
            if (p.equals(e) || p.startsWith(e + " ")) {
                flags[i] = 1;
                if (e.length() > length) length = e.length();
                canFinish = true;
            } else if (e.startsWith(p)) {
                flags[i] = 2;
                if (p.length() > length) length = p.length();
            }
        }
        return new ArrayClassifyResult(flags, length, canFinish);
    }

    /** classifyArray 的结果 */
    public static final class ArrayClassifyResult {
        public final int[] flags;
        public final int length;
        public final boolean canFinish;

        public ArrayClassifyResult(int[] flags, int length, boolean canFinish) {
            this.flags = flags;
            this.length = length;
            this.canFinish = canFinish;
        }
    }

    /** matchString 的结果：新增的候选与新增的显示映射 */
    public static final class Result {
        public final List<String> inputs = new ArrayList<String>();
        public final Map<String, String> outputs = new LinkedHashMap<String, String>();

        // 给 JS 侧批量取用，避免对 1000+ 项逐个 keySet()/Map.get() 穿过 Rhino
        public String[] inputArray() {
            return inputs.toArray(new String[0]);
        }

        public String[] outputKeys() {
            return outputs.keySet().toArray(new String[0]);
        }

        public String[] outputValues() {
            return outputs.values().toArray(new String[0]);
        }
    }
}
