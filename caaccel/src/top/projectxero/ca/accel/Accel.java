package top.projectxero.ca.accel;

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
     * 把 #RRGGBB 解析成 ARGB int。
     * 对应 JS: Common.argbInt / 颜色串解析。
     *
     * @param hex 形如 "#A1B2C3" 或 "A1B2C3"
     * @return ARGB 整数；解析失败返回 0
     */
    public static int parseColor(String hex) {
        if (hex == null) return 0;
        String s = hex.trim();
        if (s.startsWith("#")) s = s.substring(1);
        if (s.length() != 6) return 0;
        int rgb = 0;
        for (int i = 0; i < 6; i++) {
            int d = Character.digit(s.charAt(i), 16);
            if (d < 0) return 0;
            rgb = (rgb << 4) | d;
        }
        return 0xFF000000 | rgb;
    }

    /**
     * 版本号比较（按点分段，逐段数值比较）。
     * 对应 JS: NeteaseAdapter 里的版本判断。
     *
     * @return 负数表示 a &lt; b，0 相等，正数表示 a &gt; b
     */
    public static int compareVersion(String a, String b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        String[] pa = a.split("\\.");
        String[] pb = b.split("\\.");
        int n = Math.max(pa.length, pb.length);
        for (int i = 0; i < n; i++) {
            long x = i < pa.length ? parseLong(pa[i]) : 0;
            long y = i < pb.length ? parseLong(pb[i]) : 0;
            if (x != y) return x < y ? -1 : 1;
        }
        return 0;
    }

    private static long parseLong(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }
}
