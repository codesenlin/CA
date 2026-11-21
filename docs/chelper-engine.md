# CHelper 命令引擎接入

本文记录 CA 接入 [CHelper-Core](https://github.com/Yancey2023/CHelper-Core) 的架构、互斥规则和维护约定。

CHelper-Core 使用 GPL-3.0，与 CA 的许可证一致。接入的是**解析内核**，不是 CHelper Android 客户端。

## 1. 分层

```text
CHelper-Core                  C++23 内核
  ├─ 命令词法/语法解析
  ├─ AST、结构串、参数提示
  ├─ 补全建议、语法高亮、错误定位
  └─ .cpack 资源包

CHelper-Android               CHelper 官方 Android 工程
  ├─ yancey.chelper.core     JNI 包装类（CHelperCore / CommandContext）
  └─ app/src/main/libs/...   libCHelperAndroid.so

cadroid                       CA 的 Android 壳
  ├─ ScriptInterface         暴露给 CA 脚本层的 Java 入口
  ├─ CHelperBridge.kt        query / probe / applySuggestion
  ├─ jniLibs/arm64-v8a/      编译后的 libCHelperAndroid.so
  └─ assets/cpack/           release-vanilla-*.cpack

CA                            Kotlin/JS 命令核心
  ├─ CASettings.kt           命令引擎、内核版本、设置显隐
  └─ CAMain.kt               CHelper 查询、候选、光标、回写
```

CA 的列表、提示行、输入框仍是 CA 自己的 UI；CHelper 只替换**解析和补全的数据来源**。

## 2. 命令引擎必须互斥

`CA.settings.commandEngine` 只有两个值：

```text
builtin   内置解析器（默认）
chelper   CHelper（Alpha）
```

### 内置解析器模式

- 走 `caGenTextUpdate` 的内置状态机
- 使用 `IntelliSense.proc` / `callDelay`
- `/help` 由内置逻辑处理
- 不初始化 CHelper，不查询 JNI

### CHelper 模式

- `caGenTextUpdate` 在设置检查后**无条件提前返回**
- 不调用内置 `proc` / `callDelay`
- `/help` 也交给 CHelper，不再回落到内置状态机
- 只使用 CHelper 的 `query` / `applySuggestion`
- 内置 IntelliSense 仅作为 UI 外壳（列表、提示行、候选点击容器）

> 这是硬边界，不要用“先查 CHelper，失败再回落内置”的混合策略。
> 两套语法表对同一个 token 的解释可能不同，混用会互相覆盖结果。

设置页对应关系：

```text
内置解析器：显示智能模式、启用多线程
CHelper：隐藏智能模式、启用多线程，显示内核版本
```

## 3. 一次补全的完整链路

### 3.1 文本变化

```text
EditText TextWatcher
  → caGenTextUpdate
  → engine == chelper
  → post 到下一轮 UI 消息
  → caGenCHelperSuggest(text, cursor)
```

TextWatcher 回调时 Selection 可能还没更新。直接在回调里读 `getSelectionStart()` 会拿到上一帧光标，快速输入 `@a`、`@n` 时会出现“字符多余”或候选落后一个字符。因此查询必须 post 到下一轮 UI 消息。

### 3.2 查询

`CHelperBridge.query(assets, pack, command, cursor)` 返回六段，以 `\u0001` 分隔：

```text
structure
paramHint
error
suggestions（\u0002 分隔）
syntaxTypes
errorRanges
```

CA 只使用前四段渲染提示行和候选；语法高亮、错误下划线尚未接入。

### 3.3 点击候选

候选必须保留 **Core 原始 index**：

```kotlin
val index = i   // items 的原始下标，不要用过滤后的序号
```

候选可以因去重、重复命令等原因不显示，但 `output` 回调里的 `which` 必须仍然是 Core 的下标，否则会出现：

```text
点 aimassist，实际 applySuggestion(0) 得到 ?
```

### 3.4 回写文本

```text
applySuggestion(index)
  → { text, selection, isAddSpace }
  → setText / setSelection
  → post 新的 query
```

应用候选期间必须设置 `_chelperApplying = true`，并且 `caGenTextUpdate` 在 CHelper 模式下要直接返回。否则 `setText` 触发的 TextWatcher 会掉进内置解析器分支，用内置语法表覆盖 CHelper 的结果。

## 4. 尾随空格

CHelper 的 `Suggestion.isAddSpace` 表示“光标在末尾时是否补一个空格”。这个标记通过 JNI 暴露：

```kotlin
// cadroid: yancey.chelper.core.Suggestion
@Native
var isAddSpace: Boolean = false
```

```cpp
// CHelper-Android JNI
suggestionAddSpace = GetFieldID(suggestionType, "isAddSpace", "Z");
SetBooleanField(result, bindings.suggestionAddSpace, suggestion.isAddSpace);
```

规则：

1. 不无差别给所有候选补空格。
2. 只有 `isAddSpace == true` 时才考虑补。
3. 应用后用 CHelper 再查询一次新文本；如果解析错误包含
   `多余部分` 或 `意外的空格`，且文本以空格结尾，才删除尾空格。

不要用“所有候选统一 trim”代替上述判断：命令与参数之间的空格是语法的一部分。

## 5. 重复命令候选

Core 在 `? ` 之后仍可能给出 `?` 候选。直接应用会得到：

```text
? ? 
→ 命令后面有多余部分
```

处理方式：

- 候选本身保留，不隐藏，避免索引错位
- 命令名已经输入过时，点击重复候选只提示，不追加
- 候选过滤只能在 CA 渲染层做，不能改变 `which` 对应的 Core 下标

## 6. 构建

### 6.1 依赖

- CMake 3.31+
- 支持 C++23 `std::expected` 的编译器
- Android 构建使用 NDK（当前为 r30）和 arm64-v8a
- 第三方依赖：fmt、utfcpp、xxHash、unordered_dense、glaze、spdlog

CA 当前使用 GPL-3.0 的 CHelper-Core 源码；不要只替换 `.so` 而不记录对应的 Core/JNI 改动。

### 6.2 编译 Android 内核

```sh
cmake -S CHelper-Core -B build/android \
  -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK/build/cmake/android.toolchain.cmake" \
  -DANDROID_ABI=arm64-v8a \
  -DANDROID_PLATFORM=android-24 \
  -DCMAKE_BUILD_TYPE=Release \
  -DBUILD_TESTING=OFF

cmake --build build/android --target CHelperAndroid -j4
```

产物：

```text
build/android/libCHelperAndroid.so
```

复制到：

```text
cadroid/app/src/main/jniLibs/arm64-v8a/libCHelperAndroid.so
```

### 6.3 cpack

至少放入一个资源包：

```text
cadroid/app/src/main/assets/cpack/release-vanilla-<version>.cpack
```

CA 设置页会扫描 `assets/cpack` 生成「内核版本」列表；找不到资源包时，CHelper 引擎不可用。

### 6.4 CA 构建

```sh
cd CA
npm run kotlin:js
node ./cabuildtools/main.mjs shellBuildDebug
```

调试 APK：

```text
build/outputs/debugApk/app-debug.apk
```

## 7. 排查

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| `找不到函数 initContent` | CHelper 提前调用 `IntelliSense.show()`，`showAssist` 尚未初始化 | 先 `api.showAssist()`，再 `show()` |
| `[object Object]` | `applySuggestion` 返回值被直接 `as String`，Rhino Java 字符串不是 JS 原始值 | 用 `caString` / `parseInt` 转换 |
| `找不到函数 toInt` | dynamic 没有 Kotlin 扩展 | 用 `parseInt` |
| `字符多余: initiator` | 应用候选时掉进内置解析器 | `_chelperApplying` 期间 CHelper 模式直接返回 |
| 快速输入 `@a` 提示落后 | TextWatcher 中 Selection 未更新 | post 查询 |
| 点候选无反应/错误 | 候选显示序号与 Core 序号错位 | 过滤时保留原始 index |
| `? ?` 被追加 | Core 重复给出 `?` 候选 | 重复命令候选提示，不应用 |
| `camera @ ` 报意外空格 | 无条件补空格 | 只按 `isAddSpace`，再用查询结果兜底 |
| 切换引擎后设置已生效但界面不刷新 | 设置页只在 `showSettings` 时过滤 `hidden`，`fset`/`notifyChange` 只重绑文本 | 用动态区和 `Common.showSettings.refreshText()`，见第 9 节 |

### 7.1 这些问题分别属于哪一层

上面这些坑不能全部归成“引擎混用”：

| 类别 | 问题 | 结论 |
| --- | --- | --- |
| 引擎混用 | `字符多余: initiator` | 应用候选时掉进内置解析器，内置语法表覆盖了 CHelper 结果。必须硬互斥，CHelper 模式不执行内置 `proc`。 |
| 候选数据/索引 | `? ?`、点候选错位 | Core 重复候选 + CA 过滤/自动应用造成。保留 `?` 候选、重复点击不应用、过滤时保留原始 index。 |
| JNI/JS 边界 | `[object Object]`、`找不到函数 toInt` | Rhino 返回的 Java `String[]` 不是 JS 原始值，dynamic 也没有 Kotlin 扩展；统一走 `caString` / `parseInt`。 |
| UI 生命周期 | `找不到函数 initContent` | CHelper 在 `showAssist` 初始化前调用了 `IntelliSense.show()`；先建辅助面板再显示。 |
| CA 空格策略 | `camera @ `、`? aimassist ` | CA 曾无差别补空格；现在只按 `isAddSpace`，并用 CHelper 查询结果做兜底。 |

因此：

- `initiator` 是真正的**引擎混用**问题，靠互斥规则解决。
- `? ?` 是**候选重复/索引**问题，不是内置解析器参与。
- `[object Object]` 和 `toInt` 是 **JNI/JS 类型边界**问题，与选哪个引擎无关。
- `initContent` 是 **UI 初始化顺序**问题。


## 9. 设置页：静态快照与动态区

设置页不是普通的响应式界面。`Common.showSettings` 在进入页面时会把整个数组做一次 `hidden()` 过滤，生成快照；之后：

- `fset()` / `notifyChange()` 只重新绑定可见行
- 不会重新执行 `hidden()`
- 不会重新执行 `get()`

所以会出现“设置已经写进 `api.settings`，但当前页面还显示旧值，返回再进才更新”的现象。

### 9.1 动态项

`Common` 的设置框架现在支持：

```kotlin
{
    type = "dynamic",
    provider = { -> arrayOfSettingItems }
}
```

`dynamic` 项只渲染 `provider()` 返回的子项。`Common.showSettings.refreshText()` 会重新执行当前页的 `notifyChange()`，动态区重新构建子项，而外层的输入框、拓展包不参与重建。

### 9.2 智能补全的布局

```text
智能补全
  ├─ [dynamic] 解析器
  │    ├─ 命令引擎
  │    ├─ 内核版本
  │    ├─ 智能模式
  │    └─ 启用多线程
  ├─ 输入框
  └─ 拓展包
```

- `self.intellisenseParser` 是解析器数组，作为 `provider` 的返回值
- `self.intellisense` 只保留一个 `dynamic` 项 + 输入框/拓展包静态项
- 命令引擎、内核版本的选择列表都放在动态区里

切换引擎/内核版本后的刷新：

```kotlin
api.settings.commandEngine = "chelper"
Common.showSettings.refreshText()
```

不要调用：

```kotlin
Common.refreshSettings()
```

`refreshSettings` 是另一个函数对象，拿不到 `showSettings` 的 `popup` 等状态，会报：

```text
TypeError: 无法读取 undefined 的属性 “showing”
```

### 9.3 判定方法

如果切换后：

- `api.settings` 已经变了
- 返回上一页再进入，显示正确

说明设置本身没问题，是当前设置页的快照没有重新计算；优先检查动态区 `provider` 和 `refreshText()` 调用。


## 8. 验证

- 本机可用 CHelper-Core 的 host probe 直接调用 `createContext` / `getSuggestions` / `applySuggestion`，确认是 Core 行为还是 CA 层问题。
- Android 侧可在 `CHelperBridge.query` 加临时日志，对比 JNI 返回的 `error` / `structure` 与 UI 显示是否一致。
- 每次修改候选应用逻辑后，至少验证：
  - 空输入
  - `camera`
  - `camera @`
  - `camera @n`
  - `camera @initiator`
  - `?`
  - `? aimassist`
  - 连续点击同一候选
