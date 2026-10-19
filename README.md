# 命令助手

## 简介

一个支持对 Minecraft（基岩版）的命令进行补全与辅助输入的 App。本仓库为 App 的核心代码，逻辑用 JavaScript 编写，通过 Rhino 引擎在 Android 上执行。

App 工程参见 [命令助手Android版](https://gitee.com/projectxero/cadroid)。

### 特性

* 支持命令的智能补全与纠错
* 支持命令在线帮助
* 加入了已汉化的 ID 表
* 一键粘贴命令
* 支持历史命令与命令收藏夹
* 支持外加命令库来加入支持更多的功能或命令补全
* 内置一组默认命令库，无需手动导入
* 支持通过远程清单更新命令库
* 支持更换主题样式

### 信息

* 宣传片：[BV1nx411V7jB](https://www.bilibili.com/video/BV1nx411V7jB)
* [正式版下载链接](https://www.coolapk.com/game/com.xero.ca)

## 目录结构

- `modules/` — CA 的核心 JS 模块
- `cabuildtools/` — 构建工具（git subtree）
- `caclib/` — 内置命令库源码（git subtree）
- `libraries.json` — 远程命令库清单
- `config/` — 构建配置
- `build/` — 构建产物（不入版本库）

## 生成

### 准备工作

1. 安装依赖：`npm install`
2. 配置（一般不用改）。`config/shell.js` 与 `config/import.js` 里只写和机器无关的默认值，
   构建时会自动探测本机路径：

   | 要找到的东西 | 探测顺序 |
   | --- | --- |
   | cadroid 项目 | 环境变量 `CA_CADROID` → CA 旁边的 `cadroid/` 或 `ca/` |
   | Android SDK | `ANDROID_HOME` / `ANDROID_SDK_ROOT` → cadroid 的 `local.properties`（`sdk.dir`）→ 常见安装位置 |
   | Build Tools | 优先 `config/shell.js` 的 `buildToolVersion`（默认 34.0.0），否则取已安装的最高版本 |
   | JDK | `JAVA_HOME` → `~/.jdks`（优先 17）→ Android Studio 自带的 JBR → `/usr/lib/jvm` |
   | android.jar | SDK 里最新的 platform |

   解析结果会写到 `build/generated/shellConfig.json`（`generateImport` 则写 `importConfig.json`），
   排查问题时直接看这份就知道用了哪个 SDK / JDK / cadroid。要固定某个路径，改
   `config/shell.js` 里对应字段即可（填了就不再探测）。

   配置一览（都在 `config/` 下，全部是 JS，按注释改即可）：

   | 文件 | 内容 |
   | --- | --- |
   | `build.js` | 版本号、versionCode、日期、许可更新日期、更新说明（会写进核心的 BuildConfig 与 update.json） |
   | `update.js` | 下载地址表、更新站点根目录 |
   | `publish.js` | 发布方式（`local` 复制到目录 / `sftp` 上传）、目标路径与 SSH 参数 |
   | `shell.js` | 壳子打包相关路径（一般不用改，自动探测） |
   | `import.js` | `generateImport` 用的 android.jar（自动探测） |

   `caclib/` 里还有 clib 自己的元数据：`config.js`（发布源设置）、`config/publish.js`
   （发布方式）、`src/*/library.js`（每个命令库的 id/名称/版本/entry）。这些原来也是
   `@key value` 的 txt，现在统一成 JS 模块（可写注释；`preparePublish` 也兼容 `.js` / `.json` / `.txt`）。

   > 注意：`caclib/signature/`（clib 签名私钥）和 `caclib/outputs/` 不入库，所以 `clibspawn`
   > 在全新检出的仓库里跑不起来，需要自己准备私钥后才会生成产物。

### 生成正式版 JS

    npm run build

产物导出至 `build/outputs/release/main.js`。

### 生成快照版 JS

    npm run buildSnapshot

产物导出至 `build/outputs/snapshot/main.js`。

### 生成渠道版 JS（Beta / Dev / Canary）

构建命令可以附加渠道参数，产物名会带上渠道名，一眼能看出是什么构建物：

    node ./cabuildtools/main.js buildRelease -beta      # build/dist/命令助手(2026-10-15) beta.js
    node ./cabuildtools/main.js buildSnapshot -dev      # build/outputs/snapshot/dev.js + dev.lib
    node ./cabuildtools/main.js buildSnapshot -canary   # build/outputs/snapshot/canary.js + canary.lib

不带参数就是默认渠道（`buildRelease` -> release，`buildSnapshot` -> snapshot）。渠道包不写
`update.json`，避免覆盖正式更新通道。

`.lib` 是「安装器」：装进 App 后把该渠道的脚本设为调试源并建快捷方式。**同版本号下渠道优先级为
`canary > dev > beta > release`**（snapshot 视为 dev 一档），低优先级不会覆盖高优先级；版本更新的
仍然可以装。

### 生成正式版 APP

    npm run shellBuildRelease

## 内置命令库

`caclib/index.json` 列出的命令库会在构建时被嵌入到最终 JS 中，运行时通过 `CA.Library.builtinLibs` 注册到 `CA.Library.inner`，用户无需手动导入。

## 远程命令库

`libraries.json` 定义了一组远程命令库，包含 uuid、名称与下载地址。CA 启动时通过 `CA.RemoteLibrary.sync()` 拉取清单并下载未加载的库，注册到 `CA.Library.inner`。