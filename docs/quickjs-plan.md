# QuickJS worker 后续使用计划

> 状态：P1（无 caaccel jar 时的 QuickJS 回退）已实现并真机验证；jsonPro 实验因
> quickjs-kt eval API 限制已暂停。
> （见 [jsonpro-quickjs.md](jsonpro-quickjs.md)）。

## 一、QuickJS worker 现有能力与限制

协议见 `modules/utils/QuickAccel.js` 与 cadroid `QuickJsWorker`：

- `quickWorkerSetVar(name, json)`：把大块数据缓存进 worker 全局变量，只传一次
- `quickWorkerRegister(source)`：注册纯 JS 函数
- `quickWorkerCall(name, argsJson)`：调用函数，JSON 进 / JSON 出
- worker 单线程串行，`memoryLimit = 128MB`，`evaluationTimeoutMillis = 10s`
- **没有 Java 桥**：只能传可序列化数据，结果集大时回程 JSON 解析会吃掉收益
- 成本模型：设原耗时 `T`，QuickJS 约 `T/28`，穿越成本 `C`，需要 `C < 0.96T`

适合：

- 纯 JS、无 Android/Rhino 对象依赖
- 数据能缓存一次、每次只传小参数（前缀/下标）
- 结果集小，或能压缩成下标/位掩码

不适合：

- 需要构造 `G.*` UI 对象/`Spannable` 的路径
- 每次都要把大对象传过去或结果集非常大
- 需要访问 CA 库状态、选择器、命令解析器的复杂逻辑

## 二、候选与优先级

### P1：无 caaccel jar 时的 QuickJS 加速回退（已实现）

已接入 `QuickAccel`：

- `filterTextsCached`：建议列表文本缓存一次，回传 boolean 掩码
- `matchCommands`：命令名匹配（原有）

`QuickAccel` 里还保留了 `classifyCached` / `classifyArrayCached` 供基准使用，
生产回退不接入。
- `Accel.js` 的回退顺序：`filterTexts` / 命令名匹配为 **Java jar → QuickJS worker → Rhino JS**；
  `classify` / `classifyArray` 经大数据基准后仍走 Java/JS，不接 worker
- `CA.IntelliSense` 的枚举对象增加 `enumClassifyCache`，避免每次输入都重新
  `Object.keys` 并同步 keys/descs

真机验证（PERM10，无 caaccel jar）：

```
QuickFallback avail=false
classify=[1,2,0]
array={"flags":[1,2,0],"length":5,"canFinish":true}
filter=[true,false,false]
cmd=["cmd"]
```

与有 jar 时结果完全一致；调试控制台入口 `DebugUtils.benchQuickAccel()`。

大数据基准（PERM10，`DebugUtils.benchQuickAccelLarge(20)`，热态，3 次）：

| 场景 | n | Rhino JS | QuickJS worker | 结论 |
|---|---|---|---|---|
| classify | 1749 | 36.7~40.7ms | 33.2~69.4ms | 波动大，无明显收益 |
| classifyArray | 1749 | 31.4~35.3ms | 33.9~36.6ms | 略慢 |
| filterTexts | 4434 | 34.7~36.0ms | 16.8~26.6ms | 约 1.3~2x |
| 命令名匹配 | 3000 | 32.1~35.8ms | 19.9~21.6ms | 约 1.6x |

因此只把 `filterTexts` 和命令名匹配接入无 jar 回退；`classify*` 保留基准代码，
生产继续走 Rhino。`quickWorkerRegister` 的多行源码换行问题已在 cadroid 修复。

背景：

- `caaccel.jar` 是**可选**的；jar 不在时 `modules/Accel.js` 走 Rhino 里的
  等价 JS 实现
- 这些回退实现是纯 JS，却跑在慢的 Rhino 上；QuickJS worker 已经在包里，
  可以先用 worker，最后才回退 Rhino

最终落地范围（以结果集大小/穿越成本为准）：

| 方法 | worker 方案 | 状态 |
|---|---|---|
| `Accel.classify(keys, descs, ps)` | 缓存 keys/descs，回传 flags | ⚠️ 仅基准，不接入（无稳定收益） |
| `Accel.classifyArray(arr, ps)` | 缓存 arr，回传 flags/length/canFinish | ⚠️ 仅基准，不接入（略慢） |
| `Accel.filterTexts(s)` | 缓存文本，回传 boolean 掩码 | ✅ |
| `Accel.matchCommandNames` | 缓存命令快照，回传匹配名 | ✅（原有） |
| `Accel.matchString(ps, a, r)` | 缓存候选源，回传 inputs/outputs | ❌ 结果集大，继续 Java/JS |
| `Accel.filter(arr, s)` | 每次传整个输入数组 | ❌ 穿越成本高，继续 Java/JS |

建议实现顺序：

1. ✅ 在 `QuickAccel.js` 增加 worker 版 `classify` / `classifyArray` / `filterTexts`
   基准；最终只把 `filterTexts` 和命令匹配接入回退（`matchString` / `filter` 不接入）
2. ✅ 在 `Accel.js` 的回退路径里按 `jar → worker → Rhino JS` 顺序分发
3. 缓存按 JS 对象身份检测（与 `Accel.setMatchSource` 同思路），
   候选源变化时才 `quickWorkerSetVar`
4. 结果统一走 JSON；结果特别大时优先回传下标/掩码减小体积
5. worker 不可用/超时/异常 → 自动回退 Rhino，功能不变

验收：

- ✅ 行为与 Java 路径逐项一致（benchQuickAccel；有/无 jar 结果相同）
- ✅ 大数据量 Rhino vs worker 基准（见上表；classify/classifyArray 不接入）
- ✅ 有 jar 时仍走 Java，QuickJS 只在 `impl == null` 时介入

风险：

- 结果集大（如 1749 项 matchString）回程 JSON 可能不比 Rhino 快，
  需要按列表规模选择 worker 或 JS 回退
- worker 同步调用会阻塞脚本线程，和现有命令匹配一致，需要确认可接受

### P2：ID 列表首次构建 A/B（先基准，不直接接）

现状：`CA.buildIDList` 首次打开约 **262~351ms**（去重/格式化/排序/同步缓存）。

方案：

- 把枚举数据 JSON 化一次缓存到 worker
- worker 里做展开、去重、排序、生成 ids/texts，回传两个数组
- JS 侧合并到现有构建结果缓存

风险/门槛：

- 默认库「全部」有 4434 项，回传 JSON 约 100~200KB，解析成本可能吃掉收益
- 枚举数据本身较大（idlibs 共 ~760KB），首次同步成本要计入
- 只有 A/B 结果明显优于 262ms 才接入，否则放弃

验收：真机首次/重复打开耗时、内存、worker 同步耗时。

### P3：批量生成/导出（低频，暂缓）

`CA.showBatchBuilder` 的 `bundleStrings` / `export` 是纯组合字符串生成，
输入变量、输出最终文本，适合 worker；但：

- 只在导入/导出批量命令时触发
- 依赖 `CA.BatchPattern` 的函数，需要先抽成可序列化数据/纯函数
- 结果可能很大（多变量组合），回程成本未知

结论：等真有批量卡顿反馈再做。

### P4：大文本搜索（未来）

如果后续出现“在几千条历史/帮助/提示里实时搜索”的 UI，可缓存文本到 worker、
只传前缀、回传下标。目前现有搜索规模小，暂不投入。

## 三、明确不适合 QuickJS 的

| 项 | 原因 |
|---|---|
| jsonPro 补全 | 需加载 493KB JSONLanguageService；quickjs-kt eval 要求 Promise，实验失败（见 jsonpro 文档） |
| ID 列表过滤 | 结果最多几千个下标，回程 JSON 大；Java 缓存已到 8~12ms |
| matchString（有 jar 时） | Java 约 1ms；worker 回程结果集大，不划算 |
| JSON parse/format | Rhino 的 JSON 是 Java 实现，通常比 worker 回传整段文本快 |
| 语法高亮 / FCString | 结果要变成大量 `G.Span`，大头在 UI 侧 |
| 命令解析/选择器 | 依赖 CA 库状态和复杂 JS 对象，不适合序列化 |

## 四、实施顺序与验收

1. ✅ **P1**：无 jar 回退（filterTexts/命令匹配）
   - 已实现 `benchQuickAccel` 等价验证（有/无 jar 结果一致）
   - 大数据量基准已补；classify/classifyArray 不接入
2. **P2**：ID 构建 A/B，只写基准，不接生产，除非明显更快
3. **P3/P4**：按真实性能反馈再排期
4. 清理 jsonPro 实验代码后再推送当前 30+ 个加速/去重提交

## 五、测量方法

- debug 包在 `Script` 标签打日志；控制台可用 `DebugUtils.*` 基准入口
- 每个候选测两种环境：
  - caaccel jar 在 vs 不在
  - worker 可用 vs 不可用
- 指标：
  - 冷启动首次调用
  - 热路径 P50/P95
  - worker 同步/回程 JSON 解析耗时
  - 内存（worker 128MB 限制）
- 样本：内置默认库 + 至少一个大型拓展包（如 Hiland / 1.18 威力增强版）
