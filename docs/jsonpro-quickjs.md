# jsonPro → QuickJS 实验记录（暂停）

> 状态：**暂停，未接入生产路径**。jsonPro 仍走 Rhino + JSONLanguageService。
> 实验代码尚未提交，处理清单见文末。

## 目标

把 `CA.IntelliSense.js` 的 `case "jsonPro"` 补全从 Rhino 主线程搬到 QuickJS
worker，理由：JSONLanguageService 是纯 JS，且每次输入要做 parse + doComplete，
对每个 object/array 候选还会再模拟一次 doComplete。

## 已尝试的方案

1. **构建期生成 worker 资产**
   - `cabuildtools/tasks/shellWriteJsonProWorker.js`
   - 生成 `cadroid/app/src/main/assets/jsonpro-worker.js`：
     `MapScript` 桩 + `modules/JSONLanguageService.js` + worker 包装函数
     `__jsonProComplete(name, schemaJson, text, offset)`
   - 构建产物约 **492,880 字节**
2. **Android 侧加载入口**
   - `ScriptInterface.quickWorkerEvalAsset(assetName)`：读 assets，交给
     `QuickJsWorker.eval` 执行
3. **worker 里执行**
   - 计划由 `QuickAccel` lazy 初始化，失败时回退 Rhino 路径

## 失败现象与根因

worker eval 资产时稳定失败：

```
{"__error":"Require the async eval flag."}
```

换了多种包装：

| 方式 | 结果 |
|---|---|
| 直接 eval | `Require the async eval flag.` |
| `(function(){ ... })()` | 同上 |
| `async` IIFE | 同上 |
| 顶层加 `await 0;` | `Invalid result promise object.` |
| `Promise.resolve().then(function(){ ... })` | `Invalid result promise object.` |

根因在 quickjs-kt（本地源码 `/home/forest/Git/quickjs-kt`）：

- `quickjs/native/jni/quickjs_jni.c:579`：`store_evaluate_result()` 要求 eval
  结果是 **Promise**，否则报 `Require the async eval flag.`
- `quickjs_jni.c:1049`：await 时再次检查 Promise 句柄，失败报
  `Invalid result promise object.`
- Kotlin `evaluateInternal` → `evalAndAwait` 是按 async 代码设计的；
  worker 的普通 `eval` **不是"同步执行大脚本"的入口**。

结论：不是 JSONLanguageService 的语法/大小问题，而是 **worker 的 eval API
与"加载一个 493KB 同步模块"的需求不匹配**。

## 为什么暂不继续

- jsonPro 只有 `/tellraw` 的 `rawtext` 参数在用，收益面窄
- 要跑通需要 patch quickjs-kt 的 native JNI，新增同步 eval API，重编 AAR/`.so`，
  处理 16KB page、NDK 工具链、依赖版本漂移，维护成本高
- 还没有真机 profiling 证明 jsonPro 是热点；worker 方案还要承担每次输入
  文档/结果 JSON 的回程成本
- 当前已落地的加速（ID 列表、Assist、命令匹配、classifyArray、matchString
  缓存等）都是实测有收益的；不值得为 jsonPro 冒这个险

## 如果以后要继续

1. 先加 jsonPro 分支的 `Date.now()` 计时，确认真机热点
2. 在 quickjs-kt 加**同步 eval** JNI/API（不走 Promise 检查），或直接提供
   模块加载/bytecode 加载能力
3. 重新编译 AAR/`.so`，在 cadroid 用本地依赖替换官方 AAR
4. 只把 JSONLanguageService 的 doComplete 搬 worker，Rhino 侧保留
   schema/selector/UI 逻辑；失败自动回退
5. 真机对比：加载耗时、每次补全耗时、内存、首次输入延迟

## 未提交实验文件清单（明天处理）

CA：

```
M cabuildtools/tasks/shellBuildDebug.js   # 加了 shellWriteJsonProWorker 链
M modules/DebugUtils.js                    # 加了 jsonProWorkerTest
?? cabuildtools/tasks/shellWriteJsonProWorker.js
```

cadroid：

```
M app/src/main/java/com/xero/ca/ScriptInterface.kt   # 加了 quickWorkerEvalAsset
?? app/src/main/assets/jsonpro-worker.js              # 构建生成，~493KB
?? app/src/main/assets/jsonpro-test.js                # 小资产测试用
```

清理步骤：

1. 删除 `shellWriteJsonProWorker.js` 和 assets 下两个测试文件
2. 还原 `shellBuildDebug.js` 的构建链、删除 `DebugUtils.jsonProWorkerTest`
3. 删除 cadroid 的 `quickWorkerEvalAsset`
4. 跑 `npm test`、`node ./cabuildtools/main.js buildRelease`，确认工作区干净
5. 之后按 CA 30 个提交 + cadroid 1 个提交推送收尾
