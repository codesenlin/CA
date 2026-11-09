# 数据外置 + 高性能加速库：改造方案

> 注：本文保留加速层的设计与实测；部分路径来自 Kotlin 迁移前的 JS 版本。
> 当前实现分别在 `src/core/Accel.kt`、`src/core/QuickAccel.kt`、`src/ca/CAAssistModule.kt` 等 Kotlin 文件中。

> 目标：把 CA 核心脚本里的「纯数据」和「高性能计算」从 JS 里剥出来，
> 数据集中到一个目录、计算交给 Java 加速库；**构建产物行为不变**。

## 一、现状盘点

### 1.1 数据在哪

| 位置 | 内容 | 大小 | 加载方式 |
|---|---|---|---|
| `data/idlibs/*.json` | ID 表、状态表、实验表、教育版表、JSON Schema | 共 ~760 KB | `Loader.fromFile("data/idlibs/xxx.json")` |
| `src/core/BuiltinData.kt` | 主题配置（`Common.themelist`）、内置库注册表 | 90 行 | JS 字面量内嵌 |
| `src/ca/`（迁移前 CA.js） | 大量配置/常量对象（约 580 处字面量） | 7396 行 | JS 字面量内嵌 |
| `src/game/NeteaseAdapter.kt` | 版本映射表 | 76 处字面量 | JS 字面量内嵌 |
| `src/ca/CAIntelliSenseModule.kt` | 补全用的模式/参数表 | 47 处字面量 | JS 字面量内嵌 |
| `src/android/AndroidBridge.kt` | 权限/API 映射 | 60 处字面量 | JS 字面量内嵌 |
| `data/intl/**/*.json` | 多语言文案 | 若干 | `Loader.fromFile` |

### 1.2 注释在哪

JSON 不能写注释，实际现状是：

- `builtinLibrarys/*.json` 里的注释以 **`description` 字段**承载（每个库文件都有）
- `src/core/BuiltinData.kt` 的主题配置来自 `data/theme.json`；迁移前它曾是 JS 对象 + 行内注释
- 迁移前 `CA.js` / `CA.IntelliSense.js` 里的常量块常带块注释说明字段含义

### 1.3 高性能计算在哪

| 文件 | 行数 | 计算性质 |
|---|---|---|
| `CA.IntelliSense.js` | 1009 | 命令补全：正则匹配、参数解析、候选排序——**最热** |
| `CA.Assist.js` | 1031 | 命令辅助/纠错 |
| `Common.js` | 1862 | 通用工具：字符串、颜色、格式化 |
| `CA.js` | 7396 | 主逻辑，含大量 ID 查找 |
| `NeteaseAdapter.js` | 363 | 版本号比较/映射 |

### 1.4 构建机制

`cabuildtools/lib/loader-ast.js` 用 AST 把 `Loader.fromFile("xxx")` **在构建期内联成字面量**
（另外兼容 `.json` 文本里的同名调用）：

```js
.map(e => e.replace(/Loader.fromFile\((".+")\)/g, ...))
```

所以「数据外置」在**源工程层面**是纯搬家，产物不受影响——只要路径写对。

## 二、目标结构

```
CA/
├─ data/                        ← 新增：所有纯数据
│  ├─ idlibs/                   ← 从 modules/builtinLibrarys 搬来
│  │  ├─ default.json
│  │  ├─ addition.json
│  │  └─ ...
│  └─ notes/                    ← 与 json 一一对应的注释文件（目录结构镜像）
│     ├─ idlibs/
│     │  ├─ default.js          ← 说明 default.json 每个字段什么意思
│     │  └─ ...
│     ├─ theme.js
│     └─ ...
├─ caaccel/                     ← 新增：Java 加速库源码
│  ├─ build.gradle              ← 独立模块，产出 caaccel.jar
│  └─ src/main/java/top/projectxero/ca/accel/
│     ├─ IntelliSense.java      ← 补全匹配
│     └─ IdTable.java           ← ID 查找
├─ modules/                     ← 保持结构，只改数据加载路径
│  └─ builtinData.js            → Loader.fromFile("data/idlibs/default.json")
└─ ...
```

## 三、注释文件的形式

`docs/data-notes/` 下每个数据文件对应一篇 markdown 说明，不参与构建（不会被 loader 打包），
作用是"改数据前先看这里"：

```js
/* idlibs/default.json 字段说明
 *
 * {
 *   "block": {                     // 方块 ID 表
 *     "minecraft:stone": {         // 命名空间:ID
 *       "name": "石头",            // 显示名
 *       "state": ["..."]           // 方块状态（可选）
 *     }
 *   },
 *   ...
 * }
 */
```

约定：**`docs/data-notes/` 的目录结构镜像 `data/`，每个 JSON 对应一篇同名 `.md`**。
例如 `data/idlibs/default.json` ↔ `docs/data-notes/idlibs/default.md`。

> 主题配色现已归入公共数据 `data/theme.json`；风味只决定默认主题与显示顺序，
> 文档见 [flavor-build.md](flavor-build.md)。

## 四、加速库的调用约定

JS 侧不直接 `new java 类`，而是通过一层薄封装，保证"没有 jar 也能跑"：

```js
// 迁移前 modules/Accel.js；当前 src/core/Accel.kt
var Accel = (function() {
    var impl = null;
    try {
        impl = Packages.top.projectxero.ca.accel.IntelliSense;   // jar 在就加速
    } catch (e) {}
    return {
        available: !!impl,
        match: function(input, table) {
            return impl ? impl.match(input, table) : jsFallback.match(input, table);
        }
    };
})();
```

这样：

- **有 jar**：走 Java，快
- **没 jar**（比如纯 JS 环境、或构建时没放 jar）：走等价 JS 实现，行为一致
- **产物不受影响**：`script.js` 的对外行为不变

## 五、分阶段（已完成）

| 阶段 | 内容 | 状态 |
|---|---|---|
| P0 | 盘点 + 方案 | ✅ |
| P1 | 数据搬家：`builtinLibrarys` → `data/idlibs`（7 个 JSON，760 KB） | ✅ `c044e41` |
| P2 | 提取内嵌数据：tips / theme / packnames / keepermenu | ✅ `e896f76` `ee09fd8` `4852061` |
| P3 | 注释文件：11 份 notes（7 个 idlibs + 4 个数据） | ✅ `34eaace` |
| P4 | `caaccel` Java 模块 + `.jar` + 单测 | ✅ `53e13b4` |
| P5 | 第一个加速点 `matchString` + 接进 APK | ✅ `823722b` `f7f9c62` |
| 基准 | Rhino vs Java 对比（实测 23x） | ✅ `8db652f` |

### P5 的效果

`src/ca/CAIntelliSenseModule.kt` 的 `matchString` 是补全里唯一"元素数量随数据规模增长"的
O(n²) 去重（遍历整个候选表，3000 个 ID 级别）。搬到 Java 后：

```
数组输入  JS(Rhino) 6323.72 ms   Java 275.73 ms   加速 22.9x
对象输入  JS(Rhino) 11999.54 ms  Java 456.40 ms   加速 26.3x
```

其余几处 `indexOf` 去重（`CA.IntelliSense:178/736`、`builtinData:16`、`CA.js:97`）
数组长度都是个位数，O(n²) 在这里等于 O(1)，**不值得搬**。

## 六、以后怎么继续

### 加一个加速点

1. `caaccel/src/top/projectxero/ca/accel/Accel.java` 加 static 方法（保持无状态）
2. `caaccel/test/` 加用例，跑 `npm run caaccel:test`
3. `src/core/Accel.kt` 里加 JS 等价实现 + 分发分支
4. 调用方加 `if (Accel.available()) return Accel.xxx(...)`
5. **`cadroid/app/proguard-rules.pro` 确认有 keep 规则**（见下）
6. 跑 `node cabuildtools/main.js buildRelease` 验证产物

### 必须注意：R8 会裁掉加速库

`Accel` 只被核心脚本通过 Rhino 反射调用（`Packages.top.projectxero.ca.accel.Accel`），
Java 侧没有任何静态引用。release 构建时 R8 会把它整个裁掉——**必须**在
`cadroid/app/proguard-rules.pro` 里保留：

```
-keep class top.projectxero.ca.accel.** { *; }
```

第一次接入时漏了这条，构建成功但 dex 里查不到类，到真机才炸。

### 接入 Android（已完成的步骤，留档）

1. `cabuildtools/main.js caaccel build`（`npm run caaccel`）产出 `caaccel.jar`
2. 复制到 `cadroid/app/libs/`
3. cadroid 的 `app/build.gradle` / `app/build.gradle.kts`：`implementation(files("libs/caaccel.jar"))`
4. proguard keep 规则

### 判断"值不值得搬"的三个问题

1. **是算法问题还是解释器问题？** `matchString` 的 23x 主要来自 O(n²)→O(n)，
   解释器开销是次要的。纯算法问题在 JS 里改也一样有效。
2. **输入规模会随数据增长吗？** 个位数的数组不值得搬，跨 JNI 的开销会吃掉收益。
3. **真机上哪一步最慢？** 目前只能靠静态分析猜，缺真机 profiling 数据。

---

# 引擎选型与实测记录

## 一、为什么继续用 Rhino

真机实测（OnePlus PERM10 / Android 14 / arm64 / 3000 候选 / 每次补全跑一遍匹配）：

| 引擎 | 每次调用 | 相对 Rhino |
|---|---|---|
| Rhino 解释模式（`optimizationLevel = -1`，现用） | 28.03 ms | 1.00x |
| Rhino 编译模式（`0`，运行时生成 dex） | 23.68 ms | 1.18x |
| QuickJS（`app.cash.quickjs`） | 0.97 ms | 28.7x |
| Java（`caaccel.matchString`） | 1.08 ms | 26.0x |

**关键结论：QuickJS ≈ caaccel（差 5%）。**

已经在 Java 里实现的加速点，换成 QuickJS 不会更快；而没加速的点，加一个 Java 实现
比换引擎便宜得多。

### 编译模式值不值得开

- 真机只有 **1.18x**（桌面 JVM 上是 2.5x，因为 HotSpot 能跨边界优化，ART 不行）
- 瓶颈不在"函数体怎么执行"，而在 `r.output[e] = e` / `forEach` / `sort` 这些**运行时操作**
- 会引入运行时 dex 生成、首次启动变慢、调试栈变化

**结论：保持 `-1`。** 18% 换这些复杂度不划算。

### 顺便澄清一个误区：Android 14 不禁这条路

`AndroidContextFactory.createClassLoader` 在 API 26+ 走的是
`InMemoryDexClassLoader`（dex 全在内存里，不落盘）。Android 14 限制的是
"从可写目录加载代码文件"，**内存加载不受影响**。

已在 targetSdk 34 + Android 14 真机上验证编译模式可以正常工作。

## 二、QuickJS 当侧车：已落地（侧载 worker）

思路：Rhino 跑所有碰 Java 的代码，另起一个 QuickJS 虚拟机只做纯计算。
**这个方案已落地**（为什么要这么做、性能前后对比、给后来者的验证清单见
cadroid 仓库 [docs/quickjs-sidecar.md](https://github.com/codesenlin/cadroid/blob/main/docs/quickjs-sidecar.md)），
实现见 cadroid 的 `QuickJsWorker` + 本仓库 `src/core/QuickAccel.kt`：

- worker 用 `io.github.dokar3:quickjs-kt-android:1.0.15`（`.so` 已 16KB page 对齐）
- 协议：JSON 进 / JSON 出；大块数据（命令库）用 `quickWorkerSetVar` 只同步一次
- 命令名匹配：现在**优先 Java** `Accel.matchCommandNames`（命令表缓存，只传前缀）；
  无 jar 时用 QuickJS worker，再回退 JS
  - QuickJS 参考：3000 条命令 Rhino 162ms/次 → worker 稳态 **~25ms/次**
  - 内置 74 条命令实测：Java 0~4ms / worker 2ms / Rhino 2~5ms（Java 省掉 worker
    排队与 JSON 编解码，库越大优势越明显）
- 仍然成立的限制：**没有 Java 桥**，只能传可序列化的数据；结果集很大时回程
  JSON 解析会吃掉收益（成本模型见下）

成本模型：设原耗时 `T`，QuickJS 是 `T/28`，穿越成本 `C`。只要 `C < 0.96T` 就划算。
但**结果集不能大**——返回上千个匹配项时，回程 JSON 解析会吃掉收益。

| 函数类型 | 侧车是否划算 |
|---|---|
| 匹配/过滤（结果上千条） | ⚠️ 回程成本高 → 不如搬 Java |
| 查找最佳匹配（结果一个数） | ✅ 划算 |
| 解析/校验/计算 | ✅ 划算 |

（补记：命令名匹配虽然结果有几十条，但用"worker 里缓存数据 + 只传前缀"
的方式，回程 JSON 很小，实测仍明显划算。真正要评估的是"每次都要把大对象
传过去"的函数。）

## 三、desugaring 是不是 Rhino 慢的原因：不是

曾怀疑 `coreLibraryDesugaringEnabled` 让 Rhino 的调用绕了 `j$.util.*` 一层。
排查后否定：

- APK 里确实有 54 个 `j$` 类（`j$/util/Objects`、`Optional`、`stream/...`）
- 但同比例对比后不成立：

| 引擎 | 桌面 JVM | 真机 | 倍数 |
|---|---|---|---|
| `Accel.matchString` | 0.275 ms | 1.08 ms | 3.9x |
| Rhino (`-1`) | 6.9 ms | 28.0 ms | 4.1x |

**两个引擎在手机上都慢 4 倍左右**——差异来自运行环境（ART vs HotSpot、后台负载、
散热），不是 desugaring。

（早期版本说"Java 在手机上反而更快"是错的：那组数字拿的是另一段 Java 实现，
不是同一段代码。跨设备比较必须先保证代码相同。）

desugaring 只是把 `java.util.Objects.requireNonNull` 换成
`j$/util/Objects.requireNonNull`，多一次静态调用；而 Rhino 每个 AST 节点要走几十条
指令，这点开销占比微乎其微。

**要真正定位 Rhino 的热点，得用 debug 包 + Android Studio Profiler 或
`Debug.startMethodTracing`。** simpleperf 在 release 包上会被 JIT 符号和 UI 噪声稀释。

## 四、两条真实的加速路径（已落地）

| 加速点 | 位置 | 手段 |
|---|---|---|
| 补全字符串匹配 | `CA.IntelliSense.matchString` | `Accel.matchString`，O(n²) 去重 → O(n)；候选源按对象身份缓存（`setMatchSource` / `matchCached`）；JS 侧结果合并也用 `seen` 去重，最坏 1749 项全命中从 ~500ms 降到 ~107ms，再去掉 merge 里的 Java String 显式转换后约 60~99ms |
| 批量生成/导出 | `CA.showBatchBuilder.export` | 可序列化的字符串/数组/`map`/`syncmap` 片段交给 QuickJS worker；含 `expr` 函数时回退原 JS。9216 行实测 Rhino ~1.0s → QuickJS ~120ms，complex=true |
| 大文本搜索 | 暂无生产 UI | `QuickAccel.searchTexts` 已提供：12k 条文本首次同步 528ms，之后单次搜索 13ms（Rhino 427ms）；等搜索 UI 出现再接 |
| ID 列表过滤 | `src/ca/CAAssistModule.kt` 的 `setFilter` | `Accel.filter`，字符串查找一次算完，JS 侧只剩取下标 |
| 数组 enum 匹配 | `CA.IntelliSense` 的 enum 数组分支 | `Accel.classifyArray`，Java 一次算完 flags/length/canFinish |
| 命令名匹配（无参数补全） | `CA.IntelliSense` 命令列表 | `Accel.matchCommandNames`，缓存命令表；无 jar 时 QuickJS/JS 回退 |
| ID 列表过滤（缓存） | `CA.showIDList.update` | `Accel.setIDTexts` + `filterIDTexts`，只传前缀；有 jar 时直接返回 Java `int[]` 给 readOnly 适配器，不再逐项转 JS 数组；空前缀复用构建时下标 |
| 建议列表过滤（缓存） | `CA.Assist` 的 `setFilter` | `Accel.setFilterTexts` + `filterTexts`，不再每次传整个建议数组 |

第二条尤其重要：原来的写法是**每个元素都从 Java 回调进 Rhino 做一次
`String.indexOf`**，上千个元素就是上千次边界穿越。

### 候选规模的真实数据（别再拿总数吓自己）

### ID 列表预热

第一次打开 ID 选择器时，`CA.buildIDList` 要构建默认「全部」列表（约 4400~4800 项）。
启动后延迟 1.5s，等 `CA.Library.loadingStatus` 清空（拓展包是异步加载的），
再在 `Threads` 后台执行一次同样的 `buildIDList` 预热；之后用户第一次打开 ID 选择器
直接命中 `CA._idListBuildCache`，构建耗时为 0。

- 预热在后台线程，不占首屏
- 必须等库加载完成，避免缓存不完整的列表
- 设备实测：预热 ~417ms，首次打开 `IDList init cached ... 0ms`

跨会话缓存：

- 构建结果写入 `app_rhino/idlist_cache.dat`（gzip 行格式；旧 JSON 缓存仍可读）
- 缓存键包含版本、内置/启用库列表、`lib.info` 指纹和 ID 原始列表
- 启动预热先读缓存；命中则不再重建，直接放入 `_idListBuildCache` 并同步 Java 文本缓存
- 设备实测：首次构建+落盘 ~612ms，第二次启动磁盘命中 ~90~145ms；首次打开仍为缓存命中
- 数据/库变化时缓存键变化，自动失效

`matchString` / `filter` 的输入是**当前参数对应的那一个列表**，不是全部 enums：

| | 列表数 | 总项数 | 最大单表 |
|---|---|---|---|
| 内置 `default.json` | 25 | 9,002 | `recipe`=1749 |
| Hiland 6.11.0 拓展包 | 39 | 6,713 | `block`=1485 |
| 1.18 威力增强版 | 29 | 5,586 | `loot_tool`=1090 |

**单次补全的候选上限约 1,750，中位数只有 142。** 装再多拓展包也不会线性变慢，
因为同名列表是覆盖而不是相加。

按 3000 候选为基准线性换算（Rhino 28 ms / Java 1.08 ms）：

| 场景 | 候选数 | Rhino | caaccel |
|---|---|---|---|
| 中位列表 | 142 | 1.3 ms | 0.05 ms |
| `block` | 857 | 7.7 ms | 0.27 ms |
| `recipe`（最大） | 1,749 | 15.7 ms | 0.55 ms |

## 五、可选加速的契约

见 `caaccel/README.md`。一句话：**每个 `Accel.xxx()` 都有等价的 JS 实现**，
jar 在不在、方法有没有都不影响功能，只影响速度。业务代码直接调用，不判断 jar 是否存在。

