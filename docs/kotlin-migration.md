# CA Kotlin/JS 迁移路线

> 目标：把 CA 核心逐步迁移到 Kotlin/JS。
>
> 运行时仍是 Rhino / QuickJS；迁移的是源码语言和构建链，不是换掉 JS 引擎。

## 基本原则

- Kotlin/JS 产物仍是 JS，继续走现有 `core.js`、热更新和脚本加载
- `MapScript.loadModule`、`Plugins.inject`、`Loader.fromFile` 的调用方保持兼容
- `.lib` 拓展包、社区插件继续使用 JS，不强制迁移
- `java.*` / `android.*` / `com.*` 互操作集中封装，不散落在业务代码里
- 先迁叶子模块，再碰 UI、网络和入口
- 每个迁移模块都必须能在 Rhino 上跑通，再谈 QuickJS

## 技术方案

```text
Kotlin 源码
  → Kotlin Multiplatform JS target
  → ES5 输出
  → webpack production bundle（单文件 UMD）
  → Rhino / QuickJS
```

Phase 0 已建立：

- 根目录 Gradle Kotlin/JS 工程（`build.gradle.kts`、`src/`、`test/`）
- Kotlin 2.4.21 + Kotlin Multiplatform 的 `js()` 目标
- ES5 目标
- production webpack 单文件 bundle
- `npm run kotlin:js` 构建入口
- 产物复制到 `build/outputs/kotlin-js/`
- Rhino 1.9.2 语法加载验证

## Java 互操作

Kotlin/JS 没有 JVM 的 `java.lang` 类型系统，但 Rhino 作用域里有 Java 包入口。

cadroid 已在 Rhino scope 注入 `JavaBridge`（`com.xero.ca.script.KotlinJsBridge`）：

- `newInstance(className, argsJson)`
- `callStatic(className, method, argsJson)`
- `newArray(className, length)`
- `classOf(className)`
- `context()`

约定：

- 简单调用仍可直接用 `js("new java.lang.Runnable(...)")`
- 复杂构造、反射、内部类、数组走 `JavaBridge`
- 参数用 JSON 数组传递，避免 Rhino 对 Kotlin vararg/Array 的适配差异
- 现有 JS 继续直接使用 Rhino 的 Java 互操作，不经过桥

## 阶段

### Phase 0：工具链（已完成）

- Kotlin/JS 子工程
- ES5 + production bundle
- Rhino 加载验证
- `npm run kotlin:js`
- `cabuildtools` 的 assemble 任务会自动先构建 Kotlin bundle
- 单 bundle + `CaKotlinPending` 延迟安装，支持按原位置注册

### Phase 1：业务模块迁移（已完成）

已完成 Kotlin/JS 迁移并通过行为对比：

- `Internal`：`getKey` / `onCreate` / `initialize` / `add` / `get` / `once`
- `EventSender`：`init` / `on` / `off` / `trigger` / `clearListeners`
- `MemSaver`：LRU 缓存接口
- `UIHelp`：`settingItem`
- `GlobalUtils`：`appendSSB` / `ES6Ex`
- `LockClasses`：延迟安装，保持最后执行的时序
- `Threads`：延迟安装，保留原线程池初始化时序
- `IntColor`：使用 JavaReflect 精确选择 int/long 重载（避免 Paint.setColor 落到 long 分支）
- `SafeFileUtils`：文件读写、JSON、GZIP 与安全回退
- `SettingsCompat`：悬浮窗权限与各厂商 ROM 设置页
- `JavaReflect`：反射构造/方法/数组/字段与原版 API 兼容
- `DexPlugin`：DexClassLoader 与类缓存
- `LPlugins`：L 插件方法注入与 SimpleAdapter
- `Accel`：caaccel.jar / QuickJS / 纯 JS 三层回退
- `QuickAccel`：QuickJS worker 注册、缓存变量与命令匹配桥
- `ExternalStorage`：文件、URI、SAF 与临时文件
- `DBSettings`：SQLite 设置存储、事务与 Proxy 命名空间
- `MCAdapter`：基岩版/网易版适配与版本检测
- `NeteaseAdapter`：包名表、版本与发布者识别
- `NetworkUtils` / `Updater`：网络请求、更新检查与下载
- `WSServer` / `PushService`：WebSocket 服务、推送与通知
- `Intl` / `FCString` / `ISegment`：语言资源、样式文本、流解析
- `UserManager` / `IssueService` / `GiteeFeedback`：账号、问题反馈、Gitee 反馈
- `AndroidBridge`：Android 侧 Java/UI/通知/悬浮窗桥
- `listAdapter/*`：列表工厂、过滤、多选、展开与更多菜单
- `G` / `L` / `PWM` / `ConsolePanel`：UI 底层、布局模板、弹窗管理与控制台
- `PopupWindow` / `PopupPage`：浮动窗口、页面栈、对话框与侧栏
- `NetworkUI` / `JSONEdit` / `DebugUtils`：网络 UI、JSON 编辑器、调试面板
- `Common` / `Tutorial` / `EasterEgg` / `Plugins`：主题、通用 UI、教程、彩蛋、插件系统

`BuildConfig` / `UpdateSources` 是构建注入的数据，不作为代码模块迁移。

已接入 `main.js` 的 `Loader.fromFile`；迁移完成的旧 JS 文件已删除，不再保留对照副本。

构建注入数据改为 assemble 阶段生成 `build/kotlin-data/*.json`，由 Kotlin 安装器读入。

### Phase 2：模块注册与加载

- Kotlin 产物接入 `cabuildtools` 的 source assemble
- 通过 `MapScript.loadModule` 注册模块
- 保留 LOADER / postprocess 流程
- 处理 `globalThis`、UMD、source map

### Phase 3：Android 互操作

- cadroid 增加 `JavaBridge`
- 封装 `Runnable`、`Context`、`G.*`、`ScriptInterface`
- 迁移 UI / 网络等有 Android 依赖的模块
- 保留失败回退和异常栈可读性

### Phase 4：入口与热更新

- `main.js` / 启动链
- `core.js` 签名热更新
- snapshot / debug source
- 产物体积、启动耗时回归

### Phase 5：生态边界

- `.lib` 拓展包继续 JS
- `Plugins.inject` 插件继续 JS
- 只迁核心，不破坏社区生态

## 验收标准

每个迁移模块：

- `npm test` 通过
- `buildRelease` / `buildSnapshot` 通过
- Rhino 真机启动无异常
- 对外 API 和错误行为一致
- 包体积和启动耗时不明显回退

## 风险

- Kotlin/JS stdlib 体积
- ES5 兼容和 Rhino 语法限制
- `globalThis` / UMD 与现有 loader 的拼接
- Java 互操作的可读性和性能
- 热更新脚本必须继续可签名
- 迁移期间两套构建并存

## 当前状态

Phase 0 已完成；Phase 1/2 的模块已接入 `main.js`。
JavaBridge、延迟安装和 cabuildtools 自动构建已补齐；全部业务模块（含 `CA.js` 和 `JSONLanguageService`）已完成 Kotlin/JS 迁移。CA 采用 9 组共享 caApi 的组装方式，真机启动、主界面、设置页、外观侧栏与常用页面验证通过。热更新和完整 CI 仍未完成。
