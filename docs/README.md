# CA 开发文档

这里按主题汇总仓库里的开发文档。根目录 [README.md](../README.md) 负责安装、构建和命令库使用说明；
本目录只记录改造方案、字段约定和收尾状态。

## 构建与发布

| 文档 | 内容 |
| --- | --- |
| [branches.md](branches.md) | `canary → dev → beta → main` 四条线的定位与推送约定 |
| [config.md](config.md) | `config/` 下构建配置字段 |
| [flavor-build.md](flavor-build.md) | 风味构建（`-huangyxHUTAO` / `-flavor`）、主题与产物命名 |
| [data-notes/README.md](data-notes/README.md) | `data/` 下公共数据的逐文件说明 |

## 架构与改造

| 文档 | 内容 |
| --- | --- |
| [data-and-accel.md](data-and-accel.md) | 数据外置、caaccel / QuickJS 加速方案与实测 |
| [dedupe-plan.md](dedupe-plan.md) | 去重计划和遗留 LOADER 清理记录 |
| [porting-candidates.md](porting-candidates.md) | JS / Java / QuickJS 跨语言迁移候选与结论 |
| [kotlin-migration.md](kotlin-migration.md) | CA 项目 Kotlin 化分层路线：cabuildtools 先迁移，核心 JS/拓展包保持 |
| [quickjs-plan.md](quickjs-plan.md) | QuickJS worker 的能力、限制和 P1–P4 计划 |
| [jsonpro-quickjs.md](jsonpro-quickjs.md) | jsonPro → QuickJS 实验记录（暂停） |
| [intellisense-selector.md](intellisense-selector.md) | 选择器补全返回值结构 |
| [ISegment.md](ISegment.md) | ISegment 补全引擎说明 |

## 当前收尾状态

- 去重计划已完成；`LOADER` 中重复的逐文件 `postprocessor` 已移除，压缩/日期替换统一由 `cabuildtools/postprocess.js` 处理
- 加速已完成：`matchString` / ID 过滤 / 命令匹配 / ID 预热与跨会话缓存 / 大批量导出
- 大文本搜索 worker 接口已就绪，等生产搜索 UI 出现再接
- jsonPro → QuickJS 仍受 quickjs-kt eval API 限制，暂停
