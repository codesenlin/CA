# 数据外置 + 高性能加速库：改造方案

> 目标：把 CA 核心脚本里的「纯数据」和「高性能计算」从 JS 里剥出来，
> 数据集中到一个目录、计算交给 Java 加速库；**构建产物行为不变**。

## 一、现状盘点

### 1.1 数据在哪

| 位置 | 内容 | 大小 | 加载方式 |
|---|---|---|---|
| `modules/builtinLibrarys/*.json` | ID 表、状态表、实验表、教育版表、JSON Schema | 共 ~760 KB | `Loader.fromFile("builtinLibrarys/xxx.json")` |
| `modules/builtinData.js` | 主题配置（`Common.themelist`）、内置库注册表 | 90 行 | JS 字面量内嵌 |
| `modules/CA.js` | 大量配置/常量对象（约 580 处字面量） | 7396 行 | JS 字面量内嵌 |
| `modules/NeteaseAdapter.js` | 版本映射表 | 76 处字面量 | JS 字面量内嵌 |
| `modules/CA.IntelliSense.js` | 补全用的模式/参数表 | 47 处字面量 | JS 字面量内嵌 |
| `modules/AndroidBridge.js` | 权限/API 映射 | 60 处字面量 | JS 字面量内嵌 |
| `intl/**/*.json` | 多语言文案 | 若干 | `Loader.fromFile` |

### 1.2 注释在哪

JSON 不能写注释，实际现状是：

- `builtinLibrarys/*.json` 里的注释以 **`description` 字段**承载（每个库文件都有）
- `builtinData.js` 的主题配置是**JS 对象 + 行内注释**（`//主题ID`、`//主界面背景色`），这是最典型的"注释与数据混在一起"
- `CA.js` / `CA.IntelliSense.js` 里的常量块常带块注释说明字段含义

### 1.3 高性能计算在哪

| 文件 | 行数 | 计算性质 |
|---|---|---|
| `CA.IntelliSense.js` | 1009 | 命令补全：正则匹配、参数解析、候选排序——**最热** |
| `CA.Assist.js` | 1031 | 命令辅助/纠错 |
| `Common.js` | 1862 | 通用工具：字符串、颜色、格式化 |
| `CA.js` | 7396 | 主逻辑，含大量 ID 查找 |
| `NeteaseAdapter.js` | 363 | 版本号比较/映射 |

### 1.4 构建机制

`cabuildtools/loader.js:78` 用正则把 `Loader.fromFile("xxx")` **在构建期内联成字面量**：

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
│  ├─ theme.json                ← 从 builtinData.js 的 Common.themelist 提取
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

`data/notes/idlibs.default.js` 用**纯注释**写成，不参与构建（不会被 loader 打包），
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

约定：**`data/notes/` 的目录结构镜像 `data/`，每个 JSON 对应一个同名 `.js`**，内容只有注释。
例如 `data/idlibs/default.json` ↔ `data/notes/idlibs/default.js`。

## 四、加速库的调用约定

JS 侧不直接 `new java 类`，而是通过一层薄封装，保证"没有 jar 也能跑"：

```js
// modules/Accel.js
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

`module/CA.IntelliSense.js` 的 `matchString` 是补全里唯一"元素数量随数据规模增长"的
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
2. `caaccel/test/` 加用例，跑 `./caaccel/test.sh`
3. `modules/Accel.js` 里加 JS 等价实现 + 分发分支
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

1. `CA/caaccel/build.sh` 产出 `caaccel.jar`
2. 复制到 `cadroid/app/libs/`
3. `cadroid/app/build.gradle`：`implementation files('libs/caaccel.jar')`
4. proguard keep 规则

### 判断"值不值得搬"的三个问题

1. **是算法问题还是解释器问题？** `matchString` 的 23x 主要来自 O(n²)→O(n)，
   解释器开销是次要的。纯算法问题在 JS 里改也一样有效。
2. **输入规模会随数据增长吗？** 个位数的数组不值得搬，跨 JNI 的开销会吃掉收益。
3. **真机上哪一步最慢？** 目前只能靠静态分析猜，缺真机 profiling 数据。
