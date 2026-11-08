# CA Kotlin/JS 工程

CA 核心的 Kotlin/JS 迁移工程。

## 当前阶段

Phase 1 + Phase 2：

- Kotlin/JS（Kotlin Multiplatform 的 JS 目标）
- 输出 ES5
- webpack production bundle，单文件 UMD
- 在 Rhino 1.9.2 中完成加载验证
- `Internal` 已迁移到 Kotlin/JS，并与旧 JS 行为对比通过
- bundle 已通过 `Loader.fromFile` 接入 `main.js`
- `CaKotlinLate` / `CaKotlinPending` 支持按原位置安装
- cadroid 已注入 `JavaBridge`
- cabuildtools 的 assemble 任务会自动先构建 Kotlin bundle
- Threads 已通过 pending installer 启用，真机启动验证通过
- 热更新和完整 CI 接入尚未处理

## 模块

已迁移：

- `Internal`：`src/Internal.kt`
- `EventSender`：`src/EventSender.kt`
- `MemSaver`：`src/MemSaver.kt`
- `UIHelp`：`src/UIHelp.kt`
- `GlobalUtils`（`appendSSB` / `ES6Ex`）：`src/GlobalUtils.kt`
- `LockClasses`：`src/LockClasses.kt`（延迟安装）
- `Threads`：`src/Threads.kt`（延迟安装）
- `IntColor`：`src/IntColor.kt`（延迟安装，JavaReflect 精确签名）
- `SafeFileUtils`：`src/SafeFileUtils.kt`
- `SettingsCompat`：`src/SettingsCompat.kt`
- `JavaReflect`：`src/JavaReflect.kt`
- `DexPlugin`：`src/DexPlugin.kt`
- `LPlugins`：`src/LPlugins.kt`
- `Accel`：`src/Accel.kt`
- `QuickAccel`：`src/QuickAccel.kt`
- `ExternalStorage`：`src/ExternalStorage.kt`
- `DBSettings`：`src/DBSettings.kt`
- `MCAdapter`：`src/MCAdapter.kt`
- `NeteaseAdapter`：`src/NeteaseAdapter.kt`
- `NetworkUtils` / `Updater` / `WSServer` / `PushService`
- `Intl` / `FCString` / `ISegment`
- `UserManager` / `IssueService` / `GiteeFeedback` / `AndroidBridge`
- `ListAdapterBase` / `EmptyAdapter` / `RhinoListAdapter` / `FilterListAdapter`
- `SimpleListAdapter` / `MultipleListAdapter` / `ExpandableListAdapter` / `MoreListAdapter`
- `G` / `L` / `PWM` / `ConsolePanel`
- `PopupWindow` / `PopupPage`
- `NetworkUI` / `JSONEdit` / `DebugUtils`
- `Common` / `Tutorial` / `EasterEgg` / `Plugins`
- `CA`：`CA.kt` + CACore/CAIcon/CAMain/CAFavorite/CASettings/CALibrary/CAIdList/CABatch/CAAgreement

构建：

```bash
npm run kotlin:js
```

旧的 JS parity 测试依赖已删除的旧模块，已随迁移结束移除。

## 构建

```bash
./gradlew jsBrowserProductionWebpack
```

产物：

```text
build/ca-kotlin/kotlin-webpack/js/productionExecutable/ca-kotlin.js
```

也可以从 CA 根目录运行：

```bash
npm run kotlin:js
```

会把产物复制到：

```text
build/outputs/kotlin-js/ca-kotlin.js
build/outputs/kotlin-js/Internal.kotlin.js
```

## 导出约定

Kotlin 侧通过 `MapScript.loadModule("Internal", api)` 注册，保持现有 JS 调用方不变。
`globalThis.CaKotlinInternal` 仅作为测试/调试入口，不是长期 API。

## 注意

- 不能依赖 DOM / window
- 不能假设 Node 的 require/module
- Java 互操作依赖脚本作用域里的 `java` / `android` / `com`
- 复杂 Java 调用集中到 cadroid 的 `JavaBridge`
- production bundle 会压缩；调试时可用 `jsBrowserDevelopmentWebpack`

## 源码分类

`src/` 按职责分组；所有文件仍使用 `package ca.kotlin`，避免跨组 import 噪音：

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
