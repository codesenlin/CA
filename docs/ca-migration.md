# CA.js → Kotlin/JS 迁移记录

> 状态：已完成。本文保留迁移约定；当前源码在 `src/ca/`，旧 `modules/CA.js` 已删除。

`modules/CA.js` 是最后的核心单例（约 8361 行、82 个顶层成员）。它不能像普通模块一样替换一半，
因此采用“共享 API 对象 + 分组安装”的方式：

- `src/ca/CA.kt` 定义 `caApi` 和 `installCA()`。
- 每个分组文件只提供 `fun installCaXxx(api: dynamic)`，把本组字段/方法挂到同一个 API 对象上。
- 所有分组完成后，`installCA()` 按固定顺序调用各组安装函数，并 `MapScript.loadModule("CA", caApi)`。
- 迁移期间 `modules/main.js` 曾继续加载旧 `modules/CA.js`；完成后由启动段统一调用 `CaKotlinPending.installAll()`，旧文件已删除。
- 分组翻译时把原来的 `this` 改成显式 `api`；对外仍通过 `CA.xxx` 调用。
- 共享字段只在 core 组初始化；其他组只读写 `api`。
- 对外 API 名称、函数 arity、嵌套对象（Icon/Features/BatchPattern/Actions 等）必须保持。

## 分组

| 组 | 文件 | 原 JS 行范围 | 内容 |
|---|---|---|---|
| core | CACore.kt | 1-313 | settings 默认值、initialize/load/save、历史/收藏、hooks、trySave |
| main | CAMain.kt | 602-1755 | showMain/showGen/hideGen/showHistory/hideHistory |
| favorite | CAFavorite.kt | 1765-2706 | 收藏编辑、Assist、历史编辑、performExit |
| settings | CASettings.kt | 2707-4375 | 设置、导入导出、错误管理、FCS、自定义表达式、粘贴 |
| library | CALibrary.kt | 4376-5913 | 拓展包管理、在线库、模式/背景/图标、Features |
| idlist | CAIdList.kt | 5914-6477 | 二维码/捐赠、ID 列表、缓存、过滤 |
| batch | CABatch.kt | 6478-7990 | 提示、批量生成器、参数表、插件菜单/动作 |
| agreement | CAAgreement.kt | 7990-8361 | 动作编辑、协议 |

> 行范围仅作为拆分参考；以成员名为准，跨组调用通过 `api`。

## 验收

1. 所有组编译通过，`installCA()` 能在 Rhino 中注册完整 API。
2. 旧 `modules/CA.js` 与 Kotlin CA 的 API key/函数 arity 对齐。
3. 真机启动、主界面、输入/生成、历史/收藏、设置、拓展包、ID 列表、批量生成、协议逐项验证。
4. 验证通过后才替换 `modules/main.js` 中的 `Loader.fromFile("modules/CA.js")`。
