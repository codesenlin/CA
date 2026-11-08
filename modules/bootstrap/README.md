# Bootstrap

`modules/main.js` 是宿主入口，构建期由 `cabuildtools` 的 Loader AST 按顺序展开：

| 文件 | 作用 |
|---|---|
| `hooks.js` | Minecraft / BlockLauncher 钩子占位 |
| `mapscript.js` | `MapScript` 模块注册、JSON-EX、生命周期 |
| `context.js` | `ctx` / `gHandler` 宿主环境 |
| `logging.js` | `Log` / `erp` |
| `loader.js` | `Loader`（构建期 `fromFile` 与运行时模块缓存） |
| `init.js` | 加载 Kotlin bundle，调 `CaKotlinPending.installAll()` |

生成后的 `assets/script.js` 是单文件，不依赖运行时 Loader。
