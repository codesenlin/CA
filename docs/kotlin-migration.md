# CA Kotlin 化路线

> 状态：纳入规划，未开工。目标是先把边界和迁移顺序定下来，避免把“能转”误做成“整体重写”。

## 结论

CA 不整体重写成 Kotlin，采用分层迁移：

```text
cadroid 壳 / Android 宿主     Kotlin（已是）
caaccel 性能热点              Java（保留）
cabuildtools 编排与发布       Kotlin 优先迁移
CA 核心、命令库、拓展包 JS    Rhino / QuickJS（保留）
```

核心 JS 和 `.lib` 拓展包是 CA 的脚本生态与热更新基础，不应为了语言统一先动它。

## 目标

- 减少 `cabuildtools` 对 Node 编排逻辑的依赖
- 用 Kotlin/Gradle 承载版本、风味、签名、发布、APK 流程
- 保持现有产物、命令库格式、热更新和拓展包生态不变
- 让构建流程有类型检查、IDE 跳转和统一测试入口

## 非目标

- 不立刻用 Kotlin 重写 CA 核心 JS
- 不改变 `core.js` 签名热更新协议
- 不改变 `.lib`（`LIBRARY` / `SIGNLIB01` / `LIBSIGN01`）格式
- 不改变 `MapScript.loadModule` / `Plugins.inject` 的现有契约
- 不把 Babel 的 JS AST 处理硬改成 Kotlin 自研实现

## 阶段

### P0：边界与兼容性

- 梳理 `cabuildtools` 任务依赖图
- 标记必须依赖 Node/Babel 的步骤：
  - `loader-ast.js`
  - `postprocess.js`
  - `clibspawn.js` 的 JS 打包部分
- 确定 Kotlin 与 Node 的调用边界和退出码/日志约定
- 产物校验基线：同配置下文件列表、SHA-1、APK 签名结果一致

### P1：cabuildtools 编排 Kotlin 化

优先迁移不依赖 Babel 的部分：

- `main.js` 任务分发
- `config/build.json`、flavor、channel 读取
- `.lib` 打包、签名、SHA-1 计算
- APK 签名、对齐、导出
- Gradle 调用
- SFTP 发布
- 文件复制、压缩、清理

实现形态：

- Kotlin/JVM CLI，或 Gradle 插件 + `buildSrc`
- Kotlin 进程内调用 Node 执行 Babel 相关脚本
- 统一日志和失败退出码，保证 CI 行为不变

### P2：宿主与原生能力

- 新的 Android 原生能力优先用 Kotlin 写在壳里
- 通过 `ScriptInterface` / `AndroidBridge` 暴露给 JS
- `caaccel` 保持 Java，不为语言统一重写热点代码
- QuickJS worker 继续走现有 native 绑定

### P3：脚本生态保持 JS

- 核心模块继续 `MapScript.loadModule`
- 插件库继续 `Plugins.inject`
- 拓展包继续 `.lib` + 源签名
- 热更新继续 `core.js` + `core.sign`

### P4：核心迁移评估（可选，需单独决策）

只有同时满足以下条件才考虑：

- P1–P3 稳定运行至少一个发布周期
- 已设计出 Kotlin 插件 ABI、签名和热更新方案
- 有明确的性能或维护收益，而不是单纯“统一语言”
- 接受拓展包/命令库生态的迁移成本

否则核心保持 Rhino / QuickJS。

## 技术选型

| 需求 | 方案 |
| --- | --- |
| 构建编排 | Kotlin/JVM + Gradle |
| 配置序列化 | kotlinx.serialization 或现有 JSON |
| 压缩/解压 | `java.util.zip` |
| 签名/哈希 | JCA（RSA / SHA-1 / SHA-256） |
| SFTP | JSch 或现有发布接口 |
| JS AST / 压缩 | 保留 Node + Babel，Kotlin 子进程调用 |
| 产物校验 | Kotlin 测试 + 现有 Node 测试并行 |

## 验收标准

每个阶段都要满足：

- 相同输入下产物内容一致（除时间戳等既有可变字段）
- `npm test` 与现有命令继续通过
- `buildRelease` / `buildSnapshot` / `shellBuildDebug` 行为不变
- 失败时退出码、错误信息和日志可定位
- 不要求用户额外安装第二套构建环境；JVM 由 Android/Gradle 提供

## 风险

- **Babel 依赖**：AST 改写和压缩仍必须 Node，完全去 Node 不现实
- **双工具链**：过渡期同时维护 Kotlin 编排和 Node 子进程
- **产物漂移**：签名、对齐、压缩参数变化会导致 APK 不一致
- **拓展包兼容**：任何加载器/签名改动都可能破坏社区源
- **收益不明确**：如果只迁移编排层，代码量下降有限，主要收益是类型和 Gradle 集成

## 第一步

从 P1 的任务分发和配置读取开始，保持 Node 的 `loader-ast` / `postprocess` 不动；先做可并行的 Kotlin CLI 脚手架，不替换现有 `npm run build`。
