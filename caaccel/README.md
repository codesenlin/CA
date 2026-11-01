# caaccel —— 命令助手高性能加速库

**可选加速层。** JS 侧每个调用点都有等价的纯 JS 实现，jar 在不在、方法有没有，
功能都一模一样，只是慢一点。

## 设计约束

1. 纯 Java，不依赖 Android、不依赖 Rhino —— 可以单独在 JVM 上跑单测
2. 所有方法都是 static；除 `setIDTexts` 的 ID 列表缓存外无状态，Rhino 直接
   Callable 调用（列表重建时 JS 侧会重新 `setIDTexts`，不依赖跨列表状态）
3. **每个方法在 `modules/Accel.js` 里都有一份语义等价的 JS 实现**

## 契约：谁负责什么

```
modules/CA.Assist.js       业务逻辑：决定"要过滤"
        │  Accel.filter(list, s)
        ▼
modules/Accel.js           决策层：jar 在不在？方法有没有？
        │                  有 → 调 Java；没有 → 跑自己的 JS 实现
        ▼
caaccel/Accel.java         加速层：可选。下载代码的人想快就在这里实现
```

**想让某个操作变快**：在 `Accel.java` 里实现对应方法，重新 `npm run caaccel`，把 jar 放进
`cadroid/app/libs/`。
**不想管**：什么都不做。`Accel.js` 会走纯 JS 实现，功能完全一样。

## 接口清单

| 方法 | 签名 | 语义 | JS 回退 | 被谁调用 |
|---|---|---|---|---|
| `version` | `String version()` | 加速库版本号 | 返回 `"js"` | 仅调试 |
| `matchString` | `Result matchString(String ps, Object a)` | 补全的字符串匹配：`a` 是 `List` 或 `Map`，返回新增的 `inputs` 与 `outputs` | `jsMatchString` | `CA.IntelliSense.js:824` |
| `setMatchSource` | `void setMatchSource(String[] keys, String[] descs, boolean isMap)` | 缓存 matchString 的候选源，避免每次输入重复传大数组 | `Accel.js` 内缓存 | `CA.IntelliSense` |
| `matchCached` | `Result matchCached(String ps)` | 在缓存候选源上做同样的匹配 | `jsMatchString` | `CA.IntelliSense` |
| `filter` | `boolean[] filter(String[] arr, String s)` | 保留 `arr` 中含 `s` 的元素，返回下标掩码 | `Accel.js` 内的循环 | `CA.Assist.js:334` |
| `setIDTexts` | `void setIDTexts(String[] texts)` | 缓存 ID 列表文本（`buildIDList` 时同步一次） | `Accel.js` 内缓存 | `CA.buildIDList` |
| `setFilterTexts` | `void setFilterTexts(String[] texts)` | 缓存建议列表文本（打开 Assist 时同步一次） | `Accel.js` 内缓存 | `CA.Assist` |
| `filterTexts` | `boolean[] filterTexts(String s)` | 在缓存建议文本里标出包含 `s` 的元素 | `Accel.js` 内循环 | `CA.Assist` |
| `filterIDTexts` | `int[] filterIDTexts(String s)` | 在缓存的 ID 文本里找包含 `s` 的下标 | `Accel.js` 内循环 | `CA.filterIDList` |
| `rangeIndices` | `int[] rangeIndices(int n)` | 生成 `[0, n)` 下标数组 | `Accel.js` 内循环 | `CA.buildIDList` 空前缀下标 |
| `setCommandSnapshot` | `void setCommandSnapshot(String[] keys, String[] descs)` | 缓存命令名/描述，命令库重建时同步一次 | `Accel.js` 内缓存 | `CA.IntelliSense` |
| `matchCommandNames` | `String[] matchCommandNames(String prefix)` | 匹配命令名/描述中包含 prefix 的命令，按名称排序 | `Accel.js` 内循环 | `CA.IntelliSense`（无 jar 时 QuickJS/JS 回退） |
| `compareVersionArray` | `int compareVersionArray(int[] a, int[] b)` | 版本号数组比较，缺位按 -1 | `Accel.js` 内循环 | `NeteaseAdapter.compareVersion` |
| `classify` | `int[] classify(String[] keys, String[] descs, String ps)` | 枚举候选分类：1=精确命中 2=模糊命中 0=不命中 | `Accel.js` 内循环 | `CA.IntelliSense` enum 分支 |
| `classifyArray` | `ArrayClassifyResult classifyArray(String[] arr, String ps)` | 数组 enum 匹配：flags（1=完整/2=前缀/0=不命中）+ length + canFinish | `Accel.js` 内循环 | `CA.IntelliSense` enum 数组分支 |


## 构建

```sh
npm run caaccel        # 产出 build/caaccel.jar
npm run caaccel:test   # 单测（test/ 下每个 *Test 都会跑）
npm run caaccel:bench  # 与 Rhino 的对比基准（需要 rhino-runtime jar）

# 等价的直接调用（不经过 npm）
node ./caaccel/cli.js build|test|bench
```

`cli.js` 需要 javac/jar/java：优先用 `$JAVA_HOME/bin`，没有就用 PATH 里的。

## 接入 Android（可选）

1. `npm run caaccel` 产出 `caaccel.jar`
2. 复制到 `cadroid/app/libs/`
3. `app/build.gradle` 里已有：
   ```gradle
   if (caaccelAvailable) { implementation files('libs/caaccel.jar') }
   ```
4. `app/proguard-rules.pro` 里已有：
   ```proguard
   -keep class top.projectxero.ca.accel.** { *; }
   ```
   **这条必须有**：Accel 只被脚本通过 Rhino 反射调用，Java 侧没有静态引用，
   R8 会把它整个裁掉。

不做 1-3 也能编译运行，只是没有加速。

## 新增加速点

1. `Accel.java` 加 static 方法（无状态）
2. `caaccel/test/` 加用例，跑 `npm run caaccel:test`
3. `modules/Accel.js` 加同名包装 + **JS 等价实现**（关键：没人实现 Java 时靠它）
4. 业务代码直接 `Accel.xxx(...)`，不需要判断 jar 在不在
5. `app/proguard-rules.pro` 的 keep 规则已覆盖整个包，不用改
