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
2. 按需修改 `config/` 内的配置文件。这两份都是**模板**，填上自己机器上的路径即可；
   本机路径不必提交，仓库里保持模板原样：
   - `config/shell.txt` — cadroid 项目路径、Build Tools 目录、classes.dex 路径、签名文件与密钥路径
     （每个键的说明见文件内注释；`javaHome` 只在命令行环境找不到 java 时才需要，
     密钥轮换相关的 `lineagePath` / `lineageSigners` 默认留空 = 单密钥签名）
   - `config/import.txt` — `android.jar` 路径，用于生成命令库的导入信息，越新的 SDK 越好

### 生成正式版 JS

    npm run build

产物导出至 `build/outputs/release/main.js`。

### 生成快照版 JS

    npm run buildSnapshot

产物导出至 `build/outputs/snapshot/main.js`。

### 生成正式版 APP

    npm run shellBuildRelease

## 内置命令库

`caclib/index.json` 列出的命令库会在构建时被嵌入到最终 JS 中，运行时通过 `CA.Library.builtinLibs` 注册到 `CA.Library.inner`，用户无需手动导入。

## 远程命令库

`libraries.json` 定义了一组远程命令库，包含 uuid、名称与下载地址。CA 启动时通过 `CA.RemoteLibrary.sync()` 拉取清单并下载未加载的库，注册到 `CA.Library.inner`。