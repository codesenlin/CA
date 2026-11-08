# data/theme.json 主题配色

所有风味共享的主题配色数据。构建时内联进脚本，`src/core/BuiltinData.kt` 读取后
生成 `Common.themelist`；风味只通过 `data/flavors/<名字>/flavor.json` 的 `theme`
和 `themeOrder` 决定默认选中项与选择页顺序。

## 当前主题

| id | 名称 | 默认 | 说明 |
| --- | --- | --- | --- |
| `light` | 浅色风格 | 默认版 | 白色基线；显式给出全部颜色，不再依赖 loadTheme 兜底 |
| `dark` | 夜间风格 | — | 深色 |
| `pink` | 晨星粉 | 社区版 | 南鸢晨星社区版配色 |
| `blue` | 晨雾蓝 | — | 社区版蓝 |
| `purple` | 略略紫 | — | 社区版紫 |

默认主题：

- `data/flavors/default/flavor.json`：`theme = light`
- `data/flavors/huangyxHUTAO/flavor.json`：`theme = pink`

主题选择页顺序默认按本文件的键顺序（pink/blue/purple/light/dark）；风味可以用
可选的 `themeOrder` 覆盖。

## 字段

| 字段 | 说明 |
| --- | --- |
| `nameKey` | 主题名称的多语言 key，加载时解析成 `name` |
| `bgcolor` | 主界面背景色 |
| `float_bgcolor` | 浮动栏背景色 |
| `message_bgcolor` | 浮动界面背景色 |
| `textcolor` | 普通文本颜色 |
| `promptcolor` | 提示文本颜色 |
| `highlightcolor` | 高亮文本颜色 |
| `criticalcolor` | 警示文本颜色 |
| `go_bgcolor` | GO 按钮背景色 |
| `go_textcolor` | GO 按钮文本色 |
| `go_touchbgcolor` | GO 按钮按下背景色 |
| `go_touchtextcolor` | GO 按钮按下文本色 |

颜色使用 `#RRGGBB`；`Common.loadTheme` 解析成 ARGB int。`light` 现在也显式给出
全部颜色，`Common.loadTheme` 里的默认值只作为旧数据/缺字段兜底。

配色来源：`Git/CA_reforged` 的 `pink_theme`（当前 `src/ui/Common.kt`）、
`blue_theme` / `purple_theme`（`src/core/BuiltinData.kt`）。
