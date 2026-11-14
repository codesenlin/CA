import kotlin.system.exitProcess
import top.projectxero.ca.accel.Accel

/** caaccel 的最小单测：不依赖 JUnit，直接跑 main */
object AccelTest {
    @JvmStatic
    fun main(args: Array<String>) {
        check("1.0.0" == Accel.version(), "version")
        println("  all passed")
    }

    private fun check(ok: Boolean, name: String) {
        println("  " + (if (ok) "✓" else "✗") + " " + name)
        if (!ok) exitProcess(1)
    }
}
