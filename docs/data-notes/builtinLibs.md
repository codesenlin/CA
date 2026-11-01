# data/builtinLibs.json 说明

`data/builtinLibs.json` 是**内置拓展包清单**；拓展包本体是
`data/idlibs/*.json`（构建时由 `modules/builtinData.js` 用 `Loader.fromFile`
内联进脚本，注册成 `CA.Library.inner["<id>"]`）。这个目录只放拓展包，本文档说明清单。

## 字段

| 字段 | 说明 |
| --- | --- |
| `libraries[].id` | 对应 `modules/builtinData.js` 里注册的 id |
| `libraries[].file` | 拓展包文件（`data/idlibs/` 下，给人和工具看的，加载不依赖它） |
| `libraries[].name` | 显示名（保留给以后用） |
| `libraries[].default` | `enabled` = 默认启用；`disabled` = 默认禁用（仍可在设置里手动开启） |

`enabled` 的包写进 `CA.Library.recommended`，`disabled` 的写进
`CA.Library.defaultDisabled`；清单里漏登记的包按默认禁用兜底。

## 当前清单

| id | 文件 | 默认状态 |
| --- | --- | --- |
| default | default.json | 启用 |
| addition | addition.json | 启用 |
| basicedu | basicedu.json | 启用 |
| education | education.json | 禁用 |
| experiment | experiment.json | 禁用 |
| JSONSchema | JSONSchema.json | 禁用 |
| states | states.json | 禁用 |

## 新增内置拓展包

1. 把 `xxx.json` 放进 `data/idlibs/`
2. `modules/builtinData.js` 里加一行
   `CA.Library.inner["xxx"] = Loader.fromFile("../data/idlibs/xxx.json");`
3. `data/builtinLibs.json` 的 `libraries` 里登记
   `{"id":"xxx","file":"xxx.json","default":"enabled" 或 "disabled","name":"..."}`
4. 更新本文档的「当前清单」表
5. 跑 `npm test`，再按需构建

注意：`caclib/` 是在线拓展包源（clib）的工程仓库，不是内置包，不要把两者混用，
否则同一份内容会在命令库列表里出现两次。各拓展包文件本身的字段说明见
`docs/data-notes/idlibs/*.md`（目录结构与 `data/idlibs/` 镜像）。
