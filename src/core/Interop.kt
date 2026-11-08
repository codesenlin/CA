package ca.kotlin.core


/**
 * CA 现有脚本作用域里的全局对象。
 *
 * 这些对象由 Rhino / QuickJS 注入，Kotlin/JS 只做声明，不负责实现。
 */
external val MapScript: dynamic
external val Plugins: dynamic
external val Loader: dynamic
external val JavaBridge: dynamic
external val JavaReflect: dynamic
external val CA: dynamic
external val Updater: dynamic

/**
 * Rhino 作用域里的 Java 包入口。
 *
 * Kotlin/JS 没有 JVM 的 java.lang 类型系统，但生成代码跑在 Rhino 时
 * 这个全局是可用的；复杂构造/反射建议集中到 JavaBridge。
 */
external val java: dynamic
external val android: dynamic
external val com: dynamic
