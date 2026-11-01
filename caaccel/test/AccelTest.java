import top.projectxero.ca.accel.Accel;

/** caaccel 的最小单测：不依赖 JUnit，直接 java 跑 */
public class AccelTest {
    public static void main(String[] a) {
        check("1.0.0".equals(Accel.version()), "version");
        System.out.println("  all passed");
    }
    static void check(boolean ok, String name) {
        System.out.println("  " + (ok ? "✓" : "✗") + " " + name);
        if (!ok) System.exit(1);
    }
}
