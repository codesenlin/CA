# CA 开发文档

根目录 [README.md](../README.md) 负责安装、构建和命令库使用说明；本目录记录当前架构、字段约定和维护规则。

## 构建与发布

| 文档 | 内容 |
| --- | --- |
| [branches.md](branches.md) | `canary → dev → beta → main` 四条分支的定位与推送约定 |
| [../config/README.md](../config/README.md) | `config/` 下构建配置字段与用法 |
| [flavor-build.md](flavor-build.md) | 风味构建（`-huangyxHUTAO` / `-flavor`）、主题与产物命名 |

## 架构

| 文档 | 内容 |
| --- | --- |
| [kotlin-readme.md](kotlin-readme.md) | 当前 Kotlin/JS 工程结构、构建与调试 |
| [kotlin-migration.md](kotlin-migration.md) | 迁移路线与各阶段收尾状态 |
| [ca-migration.md](ca-migration.md) | CA 主对象 9 组共享 `caApi` 的组装约定 |
| [json-language-service-migration.md](json-language-service-migration.md) | JSONLanguageService 的 Kotlin 实现与兼容边界 |
| [data-and-accel.md](data-and-accel.md) | 数据外置、caaccel / QuickJS 加速与实测 |
| [intellisense-selector.md](intellisense-selector.md) | 选择器补全返回值结构 |
| [ISegment.md](ISegment.md) | ISegment 补全引擎说明 |

## 数据

| 文档 | 内容 |
| --- | --- |
| [data-notes/README.md](data-notes/README.md) | `data/` 下公共数据的逐文件说明 |

## 运行时代码位置

```text
modules/main.js          宿主入口（Loader AST 的构建根）
modules/bootstrap/       钩子、MapScript、ctx、Log、Loader、启动顺序
src/<分类>/              Kotlin/JS 业务源码
build/outputs/kotlin-js/ 构建产物（不提交）
```
