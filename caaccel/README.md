# caaccel —— 命令助手高性能加速库

纯 Java 实现的计算密集逻辑，供核心脚本（Rhino）通过 `Packages.top.projectxero.ca.accel.Accel`
调用；也可以在普通 JVM 上单测。

## 设计约束

1. **零依赖**：不依赖 Android、不依赖 Rhino，只有 JDK
2. **无状态**：所有方法 static，不持有上下文，Rhino 直接当 Callable 调
3. **必须有等价回退**：每个方法在 JS 侧都有一份实现，jar 不存在时自动走 JS

## 构建

```bash
./build.sh      # 产出 build/caaccel.jar
./test.sh       # 构建 + 跑单测
```

要求 JDK 8+（`JAVA_HOME` 有效时自动用它，否则用 PATH 里的 `javac`/`jar`）。

## 当前提供的能力

| 方法 | 说明 | JS 回退 |
|---|---|---|
| `version()` | 库版本，供 JS 探测 | 常量字符串 |
| `parseColor(hex)` | `#RRGGBB` → ARGB int | `Common.argbInt` 系列 |
| `compareVersion(a, b)` | 点分版本号比较 | `NeteaseAdapter` 里的版本判断 |

## 接入 Android

把 `caaccel.jar` 放进 `cadroid/app/libs/`，Gradle 侧 `implementation files('libs/caaccel.jar')`。
Rhino 里通过 `Packages.top.projectxero.ca.accel.Accel.xxx()` 直接调。

## 新增加速点

1. 在 `src/top/projectxero/ca/accel/` 加方法（保持 static、无状态）
2. 在 `test/AccelTest.java` 加用例
3. 在 JS 侧写等价实现，放进 `modules/Accel.js` 的回退分支
4. 跑 `./test.sh` + `node cabuildtools/main.js buildRelease` 双重验证
