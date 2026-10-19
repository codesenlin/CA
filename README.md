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
   | `update-sources.js` | 客户端检查更新用的源地址（可按渠道配置、可清空关闭），以及发布端的下载地址表、更新站点根目录 |
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

### 生成渠道版 JS（standard / beta / dev / canary）

构建命令可以附加渠道参数，产物名会带上渠道名，一眼能看出是什么构建物：

    node ./cabuildtools/main.js buildRelease            # 命令助手(2026-10-15) standard.js + 命令助手standard<日期>.lib
    node ./cabuildtools/main.js buildRelease -beta      # 命令助手(2026-10-15) beta.js + 命令助手beta<日期>.lib
    node ./cabuildtools/main.js buildSnapshot           # build/outputs/snapshot/snapshot.js + 命令助手standard<日期>.lib
    node ./cabuildtools/main.js buildSnapshot -dev      # dev.js + 命令助手dev<日期>.lib
    node ./cabuildtools/main.js buildSnapshot -canary   # canary.js + 命令助手canary<日期>.lib

渠道名和构建形态是分开的两个概念：

- **形态**（`BuildConfig.variants`）决定核心行为：`buildRelease` -> `release`，`buildSnapshot` -> `snapshot`
  （快照形态：不写 `update.json`，核心按非正式版处理）
- **渠道**（产物名 + `.lib` 的 uuid）默认都是 **standard**，加 `-beta` / `-dev` / `-canary` 才换成对应渠道

`standard` 只是对外的名字，内部仍然是 `release`/`snapshot`，所以核心代码里 `variants == "release"`
这类判断不受影响。

`.lib` 是安装器：装进 App 后把该渠道的脚本设为调试源并建快捷方式。每个渠道固定一个 uuid，
版本号取构建日期 `[年, 月, 日]`，重新构建的包会被识别为更新。**快照形态沿用作者原来的
`f6cf1729-...` uuid**（唯一特例，保证老的快照包还能被覆盖更新）；**同版本号下渠道优先级
`canary > dev > beta > standard`**，低优先级不会覆盖高优先级。

### 生成正式版 APP

    npm run shellBuildRelease

## 内置命令库

`caclib/index.json` 列出的命令库会在构建时被嵌入到最终 JS 中，运行时通过 `CA.Library.builtinLibs` 注册到 `CA.Library.inner`，用户无需手动导入。

## 命令库的签名格式

命令库有三种容器，`01` 是格式版本号：

| 魔数 | 生成工具 | 用途 |
| --- | --- | --- |
| `LIBRARY` | `js2lib.js` | 本地/安装器库（安装器 lib 用这个） |
| `SIGNLIB01` | `js2signlib.js` | 分发（源站/商店下载），9 字节魔数 + 256 字节 RSA 签名 + 8 字节时间戳 + gzip 载荷 |
| `LIBSIGN01` | App 下载时 / `js2localib.js` | 本地已验签库，9 字节魔数 + int32 sourceId 长度 + sourceId + SIGNLIB01 的 [9..] |

    # 分发格式
    node ./cabuildtools/js2signlib.js <源码.js> <私钥.pem> <输出.lib>

    # 本地格式（源站 uuid + App 公钥，公钥在 cadroid 的 Secret.HOTFIX_PUBLIC_KEY）
    node ./cabuildtools/js2localib.js <输入.lib> <源站uuid> <输出.lib> [app公钥]

签名私钥必须是 **RSA 2048 位**（App 里验签段长度写死 256 字节）。`LIBSIGN01` 旁边的 `.hash`
是 `SHA1(appVerifyKey || SHA1(payload))`，用 App 内置的公钥算——所以只有 App（或掌握其公钥的
本地流程）能产出合法 `.hash`。`SIGNLIB01` 只在下载路径里验签；直接拷贝进手机的 `SIGNLIB01`
会被当成未知格式，需要先转成 `LIBSIGN01`。

## 拓展包源

拓展包的「检测更新」和「在线拓展包」都支持多个源。入口：「拓展包管理」菜单 →「**拓展包源**」。

- **内置官方源**默认存在，列表里带 `[官方]` 前缀，不能禁用或删除
- **添加源**：填源站根目录（或它的 `info.json` 地址），先请求 `info.json` 校验，成功才保存
- **点按源**：启用 / 禁用（禁用后不参与更新检查和在线列表）
- **备注**：每个源可以写一行备注，显示在源列表和在线列表的「来源」后面
- **删除**：非官方源可删除（二次确认）
- 配置存在 `CA.settings.librarySources`，源对象为 `{url, name, enabled, note, builtin}`

检查更新时（`requestUpdateUrlFromDefSrc`）按顺序遍历启用中的源，第一个给出该 uuid 更新地址的源
胜出；源内部仍然校验 `sourceId`、SHA-1 与 RSA 签名。

「在线拓展包」会汇总**所有启用中的源**，同一个 uuid 只显示版本最高的那个，每项名称下面显示
`来源 : <源名称>（备注）`，下载时用该条目所属的源验签。

> 已移除原先"从 GitHub 自动下载远程拓展包"的机制（`modules/RemoteLibrary.js` 与
> `libraries.json`）。
