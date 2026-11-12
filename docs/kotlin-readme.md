# CA Kotlin/JS 工程

CA 核心的 Kotlin/JS 迁移工程。

## 当前状态

迁移主体已完成：

- Kotlin/JS（Kotlin Multiplatform 的 JS 目标），输出 ES5
- webpack production bundle，单文件 UMD，在 Rhino 1.9.2 中验证
- 全部业务模块、CA 主对象和 JSONLanguageService 已迁移
- 旧 JS 模块与 JSON vendor 已删除，仓库内只剩构建工具 JS
- `modules/main.cjs` 拆成 `modules/bootstrap/`，Kotlin bundle 由 `Loader.fromFile` 在构建期内联
- `CaKotlinPending.installAll()` 统一维护模块安装顺序
- cadroid 已注入 `JavaBridge`
- cabuildtools 的 assemble 任务会自动先构建 Kotlin bundle
- 真机启动、主界面、设置页、外观侧栏与 jsonPro 已验证

仍需处理：热更新签名链路和完整 CI。

## 模块

源码按职责分类，直接看目录即可：

```text
src/core/       运行时、互操作、线程、加速、安装器
src/ui/         通用 UI、弹窗、页面与列表适配器
src/android/    AndroidBridge、存储、设置、Dex、插件
src/game/       MC / 网易适配
src/network/    网络、更新、WebSocket、账号与反馈
src/text/       多语言、样式文本、ISegment
src/ca/         CA 主对象与 9 组共享 caApi 模块
src/json/       JSONLanguageService
```

包名与目录对应（`ca.kotlin.core`、`ca.kotlin.ui` 等）。

## 构建

```bash
./gradlew jsBrowserProductionWebpack
```

产物：

```text
build/ca-kotlin/kotlin-webpack/js/productionExecutable/ca-kotlin.cjs
```

也可以从 CA 根目录运行：

```bash
npm run kotlin:js
```

会把产物复制到：

```text
build/outputs/kotlin-js/ca-kotlin.cjs
build/outputs/kotlin-js/Internal.kotlin.cjs
```

## 导出约定

Kotlin 侧通过 `MapScript.loadModule` 注册；`globalThis.CaKotlinInternal` 仅作为测试/调试入口，不是长期 API。

## 注意

- 不能依赖 DOM / window
- 不能假设 Node 的 require/module
- Java 互操作依赖脚本作用域里的 `java` / `android` / `com`
- 复杂 Java 调用集中到 cadroid 的 `JavaBridge`
- production bundle 会压缩；调试时可用 `jsBrowserDevelopmentWebpack`

## 源码分类

`src/` 按职责分组，包名与目录对应（`ca.kotlin.core`、`ca.kotlin.ui` 等）；跨组符号按需 import：

```text
core/      Interop、Internal、EventSender、Threads、MemSaver、JavaReflect、
           LockClasses、Pending、GlobalUtils、Accel、QuickAccel
ui/        G、L、PWM、ConsolePanel、Common、PopupPage、PopupWindow、UIHelp、
           NetworkUI、JSONEdit、DebugUtils、Tutorial、EasterEgg、Plugins
ui/adapter/ ListAdapterBase、EmptyAdapter、RhinoListAdapter、FilterListAdapter、
           SimpleListAdapter、MultipleListAdapter、ExpandableListAdapter、MoreListAdapter
android/   AndroidBridge、ExternalStorage、DBSettings、SafeFileUtils、
           SettingsCompat、IntColor、DexPlugin、LPlugins
game/      MCAdapter、NeteaseAdapter
network/   NetworkUtils、Updater、WSServer、PushService、UserManager、
           IssueService、GiteeFeedback
text/      Intl、FCString、ISegment
ca/        CA.kt、CACore、CAIcon、CAMain、CAFavorite、CASettings、
           CALibrary、CALibraryModule、CAIdList、CABatch、CAAgreement、
           CAIntelliSenseModule、CAAssistModule
```
