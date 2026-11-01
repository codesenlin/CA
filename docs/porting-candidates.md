# JS / Java / QuickJS 跨语言迁移候选

> 状态：ID 列表 + 数组 enum + ID 列表/Assist 缓存 + 命令匹配 + matchString 缓存已实现；其余为分析记录。目标是找真正值得跨边界的点，而不是把代码搬来搬去。
> 已有加速层与基准见 [data-and-accel.md](data-and-accel.md)。

## 结论速览

| 方向 | 候选 | 位置 | 优先级 | 预期收益 |
|---|---|---|---|---|
| JS → Java | ID 列表过滤 ✅ | `CA.js` `showIDList.update` | **P0** | 旧 JS 扫描 → `Accel.filter`；后续再加 Java 侧缓存，非空前缀降到 8~12ms |
| JS → Java | ID 列表初始化去重 ✅ | `CA.js` `showIDList.init` | **P0** | `indexOf` O(n²)，桌面 Rhino 已 342ms/5239 项；先改 JS 用 map，必要时再上 Java |
| JS → Java | 数组 enum 匹配 ✅ | `CA.IntelliSense.js` `matchParam` 的 `case "enum"` 数组分支 | P1 | 最多 1749 项、每次输入；新增 Java `classifyArray`（精确/前缀标志） |
| JS → QuickJS | jsonPro 补全 | `CA.IntelliSense.js:311-520` | P1/P2 | 每次输入 parse + doComplete，且对每个 object/array 候选再模拟一次 doComplete；纯 JS，适合 worker |
| QuickJS → Java | 命令名匹配 ✅ | `QuickAccel.matchCommandNames` → `Accel.matchCommandNames` | P1 | 已改为 Java 优先，worker/JS 回退；内置 74 条命令实测 Java 0~4ms |
| Java → JS | `Accel.parseColor` / `compareVersion` | `Accel.java:147-180` | P2 | Java 侧未接调用点；删掉或接 JS，无加速收益 |
| Java → QuickJS | — | — | — | 不建议：worker 无 Java 桥，纯 Java 算法搬过去只能 JSON 进出，且 QuickJS ≈ Java |
| QuickJS → JS | — | — | — | worker 不可用时已有等价 JS 回退，无需迁移 |

## 一、JS → Java

### 1. ID 列表过滤（P0，最简单）

`CA.js` 的 `showIDList.update`（约 5701 行）每次输入都遍历整个 `self.texts`：

```js
for (i = 0; i < self.texts.length; i++) {
    if (self.texts[i].indexOf(s) >= 0) arr.push(i);
}
```

- 默认库的「全部」ID 表约 **5239 项**（21 个 enum 合并，最大单表 `recipe` 1749 项）
- 已有 `Accel.filter(String[] arr, String s)`（`CA.Assist.js:334` 已用），一次调用返回 `boolean[]`，JS 侧只收集下标
- 无需新增 Java 方法，属于"接线"级别改动

桌面 JVM Rhino 快速基准（5239 项，非真机）：

```
prefix=[]        JS(Rhino)=7.41ms   Java=0.027ms
prefix=block_0   JS(Rhino)=5.24ms   Java=0.083ms
prefix=item_1    JS(Rhino)=5.50ms   Java=0.060ms
```

真机按现有文档的 ~4x 环境差粗略换算，全量扫描约 16~30ms/次；Java 侧可忽略。
**注意**：结果集是 boolean 掩码 + JS 下标收集，不是大数组回程，符合 Java 加速的条件。

✅ 已实现（`937c82a`）：`showIDList.update` 已改为 `Accel.filter(self.texts, s)`，
JS 侧只收集掩码下标；没有 jar 时走 `Accel.js` 的等价 JS 回退。

真机实测（PERM10 / Android 14 / 4434 项「全部」ID 表，
debug 控制台调用 `DebugUtils.benchIDListCompare()`）：

| 前缀 | 旧 JS 循环 | `CA.filterIDList`（含 mask 收集与 debug 日志） | `Accel.filter` 纯 Java 调用 |
|---|---|---|---|
| `""` | 159 ms | 72 ms | 17 ms |
| `minecraft:` | 110 ms | 74 ms | 25 ms |
| `block` | 87 ms | 53 ms | 13 ms |
| `item_1` | 76 ms | 50 ms | 13 ms |
| `zzz` | 79 ms | 48 ms | 12 ms |

结论：

- 第一版新路径比旧 JS 循环快 **~1.5~2.2x**，但仍有 12~25ms 的
  跨边界/数组转换成本（每次把 4434 个字符串传给 Java）
- 已继续实现 **Java 侧缓存**（`Accel.setIDTexts` + `filterIDTexts`，见下文），
  非空前缀的过滤已降到 **8~12ms**
- `buildIDList` 首次打开真机约 **262~351ms**（有波动）；重复打开同一 ID 表时
  按「库对象 + 列表 key」命中缓存，耗时 **≈0ms**；首次构建从 504ms 降到 262ms
  （约 -48%，排序/去重优化 + 预分配）；
  旧 O(n²) 版本没有直接计时，按算法规模估算会更慢

### 2. ID 列表初始化去重（P0，算法问题优先）

`CA.js` 的 `showIDList.init`（约 5658 行）用 `self.ids.indexOf(e)` 去重，项数一多就是 O(n²)：

```js
if (self.ids.indexOf(e[j]) >= 0) { off--; continue; }
```

桌面 JVM Rhino 快速基准（5239 项）：

```
O(n²) indexOf: 341.6ms    object-map: 109.1ms
```

按 ~4x 环境差，真机「全部」列表可能在 1s 量级；这属于**先改算法**的问题，
不一定要 Java。建议：

1. JS 侧用对象/Map 做 `seen`，把去重降到 O(n)
2. 若仍不够快，再加 `Accel.uniqueTexts(String[])`（一次返回去重后的数组）

✅ 已实现（`937c82a`）：JS 侧改为 `Object.create(null)` 的 `seen` 表 O(n) 去重，
`ids/texts` 用 push 构建；200 组随机数据与旧实现逐项对比一致。

同步升级的还有 `self.texts` 构建和 `ISegment.kvSort`；排序是一次性的，收益不大。

### 3. 数组 enum 匹配（P1）

`CA.IntelliSense.js` 的 `matchParam` 里 `case "enum"` 数组分支（约 809 行）：

```js
r.input = t.filter(function(e) {
    if (ps.startsWith(e + " ") || ps == e) { ... canFinish ... }
    else if (e.startsWith(ps)) { ... }
    else return false;
    ...
});
```

- 候选最多 1749（`recipe`），每次输入都会跑
- 对象 enum 分支已经走 `Accel.classify`；数组分支还没有
- 新增 `Accel.classifyArray(String[] arr, String ps)` 返回 `int[] flags`
  （1=完整命中/可结束，2=前缀命中，0=不命中），JS 只负责拼 label 和 length
- 预计与 `filter` 同级收益，改动小

✅ 已实现：`Accel.classifyArray`（Java + `Accel.js` 回退），
数组 enum 分支改为读 flags/length/canFinish；单测与 3000 组随机 JS 回退对比通过。

## 一、补充：matchString 候选源缓存（已实现）

`CA.IntelliSense.matchString` 原来每次输入都把候选数组/对象传给 Java
`matchString`；现在按 JS 对象身份缓存（`Accel.setMatchSource` + `matchCached`），
同一候选源重复输入时不再重复跨边界转换。Java 侧新增
`setMatchSource` / `matchCached`，单测与 `matchString` 结果一致；老 jar 自动回退
到原 `matchString`。

真机实测（PERM10，合成 1749 个候选、前缀命中全部，debug 控制台
`DebugUtils.benchMatchString()`）：

| 路径 | 10 次平均 |
|---|---|
| 旧路径（每次传候选对象 + 原 merge） | ~500ms |
| 旧路径（每次传候选对象，merge 改 seen 去重） | ~196ms |
| 旧路径 + 数组访问器 | ~152ms |
| 缓存路径（候选源只同步一次 + seen merge + 数组访问器） | **~107ms** |

结论：

- JS 侧合并结果的 `indexOf` 也是 O(n²)，改成 `seen` 表后先把最坏情况从
  500ms 降到 196ms（这一步对所有 matchString 调用都有效）
- 候选源缓存再省掉重复的 1749 项跨边界转换，最坏情况降到 132ms
- `Result` 再暴露 `inputArray/outputKeys/outputValues` 数组访问器，避免逐项
  `Map.get` 穿 Rhino，最坏情况进一步降到 **~107ms**
- 1749 项全命中是最坏情况；实际补全中位数约 142 项，耗时远低于此
- 还试过 Java `Result` 序列化 JSON + JS `JSON.parse` 的路径：~103.9ms
  vs 当前数组访问器 ~107.5ms，收益不足且增加转义复杂度，**未采用**

## 二、JS → QuickJS

### 1. jsonPro 补全（P1/P2，实验受阻，暂停）

> QuickJS 后续整体规划（无 jar 回退、ID 构建 A/B 等）见
> [quickjs-plan.md](quickjs-plan.md)。

> 实验记录、根因和清理清单见 [jsonpro-quickjs.md](jsonpro-quickjs.md)。
> 结论：quickjs-kt worker 当前 eval API 要求 Promise，加载 493KB 模块失败；
> 要跑通需 patch native 同步 eval。暂不接入生产路径。

`CA.IntelliSense.js:311-520` 的 `case "jsonPro"` 每次输入会：

1. `JSONLanguageService.TextDocument.create` + `parseJSONDocument`
2. `ls.doComplete(...)`
3. 对每个对象/数组类型的候选，**再构造一份插入后的文本并模拟 `doComplete`**，过滤非法候选

JSONLanguageService 是纯 JS（无 Java 依赖），完全符合 worker 的前提。
适合的协议：

- 首次：把 schema / language service 源码传进 worker 缓存
- 每次：传 `{ text, offset }`，返回 completion items（结果通常几十条，回程不大）
- 失败/worker 不可用：保留现有 Rhino 路径

风险：

- JSONLanguageService 约 11k 行 / 400KB 级，注册进 worker 有首次编译/内存成本
  （worker `memoryLimit=128MB`，`evaluationTimeoutMillis=10_000`）
- 需要把 `TextDocument` / schema configure 的逻辑一起搬，模块边界要切干净
- 建议先做 lazy 初始化（第一次遇到 jsonPro 才注册），并真机计时

### 2. ID 列表过滤（不建议用 worker 替代 Java）

worker 缓存 `texts` 后返回匹配下标看似可行，但「全部」列表有 5239 项，
空前缀时结果就是 5239 个下标的 JSON，回程解析会吃掉收益（见
[data-and-accel.md](data-and-accel.md) 的成本模型）。这个场景 Java 更合适。

### 3. Batch 导出 `bundleStrings`（P2，低频）

`CA.js:6180` 的 `bundleStrings` 是纯数组组合生成，输入变量、输出字符串；
理论可 offload 到 worker，但只在导入/导出批量命令时触发，优先级低于 jsonPro。

## 三、Java → JS

| 候选 | 情况 | 建议 |
|---|---|---|
| `Accel.parseColor` | 未接调用点 | 删除，或接 JS 颜色解析；无收益 |
| `Accel.compareVersion` | 未接调用点 | 删除，或接 JS 版本比较；无收益 |
| `Accel.compareVersionArray` | `NeteaseAdapter.compareVersion` 在用；数组只有 3~4 个元素 | 迁移到 JS 可能反而更稳（省一次反射/桥），但收益微小，先不动 |
| cadroid 的 Java/Kotlin | `ScriptFileStream` 验签、`Secret`、引擎/服务/权限都是 Android/安全职责 | 不适合下放脚本 |

结论：**没有值得为了性能而从 Java 搬到 JS 的代码**；只有清理未使用的 Java 方法。

## 四、Java → QuickJS

不建议。

- QuickJS worker 没有 Java 桥，只能 JSON 进出；Java 是宿主，把 Java 算法重写成
  QuickJS JS 再传 JSON，通常更慢、更难维护
- 文档已有的真机基准：`matchString` 场景 QuickJS ≈ Java（0.97ms vs 1.08ms）
- 唯一勉强成立的场景是"Java 侧只是占位、真正计算在 JS 侧"，目前代码里没有

## 五、QuickJS → Java

### 1. 命令名匹配 ✅（已实现，Java 优先）

原方案：`QuickAccel.matchCommandNames`（`modules/utils/QuickAccel.js:84`）：

- 命令库同步到 worker 一次
- 每次输入传前缀，worker 里 `matchCommands` 扫描 + 排序
- 现有文档记录：Rhino 162ms/次 → worker 稳态约 25ms/次

Java 替代方案：

- `Accel.setCommandSnapshot(String[] keys, String[] descs)` 缓存到 Java 静态字段
  （命令库 reload 时 reset；这会让 Accel 从"完全无状态"变成"有一个可重置缓存"）
- `Accel.matchCommandNames(String prefix)` 返回匹配的命令名数组
- 预计 Java 扫描 3000 条约 1~2ms，省掉 worker 排队 + JSON 编解码 + 线程同步

已实现：

- `Accel.setCommandSnapshot` / `matchCommandNames`：Java 缓存命令名/描述，
  只传前缀；`Accel.js` 做快照变更检测（同一 snapshot 不重复转换）
- `CA.IntelliSense` 改为 **Java 优先**，无 jar 时 QuickJS worker，最后 JS 回退
- 真机 A/B（内置 74 条命令，debug 控制台 `DebugUtils.benchCommandMatch`）：

| 前缀 | Rhino | Java | QuickJS worker |
|---|---|---|---|
| `""` | 2ms | 4ms（预热含快照同步） | 2ms |
| `block` | 4ms | 1ms | 2ms |
| `cmd` | 5ms | 0ms | 2ms |
| `minecraft` | 5ms | 0ms | 2ms |
| `zzz` | 4ms | 2ms | 2ms |

内置命令很少时三者都在 5ms 内；Java 省掉 worker 排队/JSON 编解码，命令表大时
（文档里的 3000 条场景 worker 约 25ms）优势会更明显。实测结果一致（same=true）。

注意：

- Accel 增加了命令表缓存（快照变更时重置，跨库不残留）
- QuickAccel 保留为无 jar 回退；不再是首选路径

### 2. QuickJS → JS

worker 不可用时已有完整 JS 回退，不需要迁移。

### 2. ID 列表过滤：Java 侧缓存文本数组 ✅（已实现）

第一版真机数据说明 `Accel.filter` 的瓶颈已经从"扫描"变成"每次把 4434 个
字符串从 Rhino 转成 Java `String[]`"（纯 Java 调用 12~25ms，占总耗时的一半以上）。

已实现：

- `Accel.java`：`setIDTexts(String[] texts)` 缓存 + `filterIDTexts(String s)`
  返回匹配下标 `int[]`；文本数组只在 `buildIDList` 时同步一次
- `Accel.js`：同名包装 + JS 缓存回退；`CA.buildIDList` 同步缓存，
  `CA.filterIDList` 改调 `filterIDTexts`（老 jar 自动回退）
- 真机（同上 4434 项，debug 控制台 `benchIDListCompare`）：

| 前缀 | 旧 JS | 第一版 filter | 缓存版 |
|---|---|---|---|
| `""` | 109~159ms | 68~72ms | **0~1ms**（直接复用构建时缓存的 4434 个下标） |
| `minecraft:` | 110~115ms | 73~74ms | 40~44ms（607 项） |
| `block` | 86~87ms | 53ms | 11~12ms（212 项） |
| `item_1` | 72~76ms | 50ms | 8~9ms（0 项） |
| `zzz` | 71~79ms | 48ms | 8~9ms（0 项） |

结论：非空前缀已降到 8~12ms（约 6~10x）；空前缀改为返回构建时缓存的
`_idAllIndices`，耗时降到 0~1ms，打开列表/清空搜索框不再卡。

另外 `CA.Assist` 的建议列表也改用 `Accel.setFilterTexts` + `filterTexts`
缓存接口，不再每次输入都从 Rhino 传整个建议数组。

有状态缓存的约定：列表重建时 `buildIDList` 会重新 `setIDTexts`，不跨列表复用。

`buildIDList` 的构建/排序也连续做了优化：

1. ID 都是字符串 → 原生 `ids.sort()`，避免 5 万次 JS 比较器回调
2. 去重时 `seen[id]` 直接存显示文本，排序后按 id 还原 `texts`，省掉再建
   id→text 映射（rhino 里 4434 项对象插入约 115ms）
3. `texts` / `_idAllIndices` 预分配数组
4. 整次构建结果按「库对象 + 列表 key」缓存，重复打开直接复用；
   `Accel.setIDTexts` 遇到同一个 `texts` 数组也跳过重复的 Java 转换

真机阶段数据（debug 日志）：

```
IDList init 262ms, 4434 项 (resolve=1 build=109 sort=86 sync=40 all=26)
```

首次 init 从 504ms 降到 262ms（约 -48%）；重复打开同一列表 ≈0ms。

## 六、附带清理项

- `modules/utils/JSONHelper.js`（293 行）目前**没有被 `main.js` 加载**，
  是死代码；确认无引用后可删除，或接入 JSON 编辑高亮
- `caaccel` 的 `parseColor` / `compareVersion` 未接调用点，可删
- QuickAccel 里的 `runBench` / `runMatchBench` 是调试入口，保留但不要进 UI

## 七、建议实施顺序

1. ✅ **P0（已完成，`937c82a`）**：`showIDList.init` 改 JS map 去重；
   `showIDList.update` 接 `Accel.filter`。改动小、用户可见（打开「全部」ID 列表 + 输入过滤）
   - 计时入口：debug 包在 `Script` 日志输出 `IDList init ...ms` / `IDList filter ...ms`；
     调试控制台可执行 `DebugUtils.benchIDList()` 或 `DebugUtils.benchIDListCompare()`
   - 真机数据见上文：新路径 ~1.5~2.2x，但仍有 12~25ms 的数组转换成本
2. ✅ **P1 数组 enum 已完成**：`Accel.classifyArray` + `CA.IntelliSense` 接线
3. ✅ **ID 列表 Java 缓存已完成**：`setIDTexts` + `filterIDTexts`，非空前缀 8~12ms
4. 下一步：jsonPro 的 QuickJS 原型（工作量大）；ID 列表非空前缀仍有
   8~40ms 的 Java→JS 结果构造成本，可选优化
3. **P1/P2**：jsonPro 的 QuickJS 原型（lazy 注册、真机计时、失败回退）
4. **P2**：清理未使用 Java / JSONHelper 死代码
5. 每次改动后按 [data-and-accel.md](data-and-accel.md) 的流程验证：
   Rhino/Java 行为对比、`npm test`、`buildRelease`、真机计时

## 八、测量方法

- 在 `CA.showIDList.init/update`、`CA.IntelliSense` jsonPro 分支加 `Date.now()` 计时日志
  （debug 包，日志标签 `Script`）
- P0 目前只做了逻辑等价验证 + 真机启动冒烟；「全部」ID 列表的 UI 路径还没手工
  点开计时，下一步应在真机上打开该列表、输入短前缀，记录 init/update 前后耗时
- 同一台设备对比：有 jar / 无 jar；worker 可用 / 不可用
- 关注 P50/P95，不只看一次值；输入过滤要测短前缀、空前缀、超长列表
- 结果记录到 `docs/data-and-accel.md` 或本文件，避免下次重复猜
