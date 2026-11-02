# 风味构建（-huangyxHUTAO / -flavor）

在**渠道**（stable / beta / dev / canary）之外，构建还可以叠一个**风味**。
风味只改脚本内容和产物名，不改构建形态（release / snapshot）与渠道：

- 默认主题（`data/theme.json` 里的主题 id）
- 新用户的初始欢迎语（历史列表）
- 启动加载文字
- 产物名后缀；并且**不写** update.json 等更新清单，避免和正式版更新源混在一起

风味参数是**可选的**：不写时使用 `flavors/default`（基线数据），行为与之前完全一致。

## 用法

```bash
npm run build -- -huangyxHUTAO                  # 正式版 + 南鸢晨星社区版风味
npm run buildSnapshot -- -huangyxHUTAO          # 快照版 + 社区版风味
node ./cabuildtools/main.js shellBuildRelease -huangyxHUTAO
node ./cabuildtools/main.js shellBuildDebug -huangyxHUTAO

# 自定义风味（需先在 config/build.json 的 flavors 里定义）
npm run build -- -flavor <名字>
```

产物的风味后缀：

```
build/dist/命令助手(1.2.14).apk              # stable
build/dist/命令助手(1.2.14-huangyxHUTAO).apk # huangyxHUTAO
build/dist/命令助手(2026-10-31) stable-huangyxHUTAO.js
build/outputs/debugApk/app-debug-huangyxHUTAO.apk
```

## 目录结构

风味定义和数据都放在 `flavors/` 下，一个风味一个文件夹：

```
data/
└── theme.json             # 公共主题配色：light / dark / pink / blue / purple
flavors/
├── default/               # 基线风味（默认）：先加载，其它风味在它上面覆盖
│   └── flavor.json        # 默认参数（name/theme/startupText/aboutText/welcome）
└── huangyxHUTAO/          # 南鸢晨星社区版
    └── flavor.json        # 风味元数据（默认主题 pink、themeOrder 等）
```

- `flavors/<名字>/flavor.json`：风味元数据（必填，没有这个文件就不算一个风味）；
  `default` 也带一套默认参数，其它风味按字段覆盖它
- 主题配色是公共数据 `data/theme.json`；风味只通过 `theme` / `themeOrder`
  决定默认主题和选择页顺序
- **`flavors/default` 是基线**：构建时先取 default 的数据，再用所选风味的数据
  按文件名深度合并（对象逐键合并、数组/标量直接覆盖）
- 公共数据仍在 `data/`（例如 `data/tips.json`），不参与风味合并

### flavor.json

```json
{
    "name": "南鸢晨星社区版",
    "theme": "pink",
    "startupText": "命令助手 by ProjectXero&南鸢晨星",
    "aboutText": "非官方社区版 由 南鸢晨星 维护，原作者 ProjectXero",
    "welcome": [
        "/say 你好呀喵~ 我是命令助手呀~ 这里是历史喵~ 右滑是收藏哦 可以拖来拖去，还可以长按编辑呢喵~",
        "/say 命令助手 - 南鸢晨星社区版 喵喵喵~"
    ]
}
```

`flavors/default/flavor.json` 也写全这些参数（作为默认值，不是空对象）：

```json
{
    "name": "默认版",
    "theme": "light",
    "startupText": "命令助手 by ProjectXero",
    "aboutText": "Developed by ProjectXero",
    "welcome": [
        "/say 你好，我是命令助手！左边是历史，右边是收藏，可以拖来拖去，也可以长按编辑哦"
    ]
}
```

其它风味的 `flavor.json` 按字段覆盖 default，未写的字段继续用 default 的值。

| 字段 | 作用 |
| --- | --- |
| `name` | 显示用的风味名（产物名/安装器显示名里用） |
| `theme` | 默认主题 id；主题定义在公共 `data/theme.json` |
| `themeOrder` | 可选：覆盖主题选择列表顺序；不写时按 `data/theme.json` 的键顺序 |
| `startupText` | 启动加载文字（替换默认的 `命令助手 by ProjectXero`） |
| `aboutText` | 关于页副标题（默认 `Developed by ProjectXero`） |
| `welcome` | 新用户的初始历史/欢迎语数组 |

### data/theme.json

主题配色是所有风味共享的公共数据，放在 `data/theme.json`，格式：

```json
{
    "pink": {
        "nameKey": "common.theme.pink",
        "bgcolor": "#FFF5F7",
        "float_bgcolor": "#FFECF0",
        "message_bgcolor": "#FFF9FA",
        "textcolor": "#8A4B7C",
        "promptcolor": "#E89CB8",
        "highlightcolor": "#FF6B9C",
        "criticalcolor": "#FF3366",
        "go_bgcolor": "#FFB6C1",
        "go_textcolor": "#FFFFFF",
        "go_touchbgcolor": "#FF85A2",
        "go_touchtextcolor": "#FFFFFF"
    }
}
```

字段含义：

| 字段 | 说明 |
| --- | --- |
| `nameKey` | 主题名称的多语言 key，加载时用 `Intl.get()` 解析成 `name`；`light` 用 `common.theme.light` |
| `bgcolor` | 主界面背景色 |
| `float_bgcolor` | 浮动栏（滑动时与屏幕保持静止的栏）背景色 |
| `message_bgcolor` | 浮动界面背景色 |
| `textcolor` | 普通文本颜色 |
| `promptcolor` | 提示文本颜色 |
| `highlightcolor` | 高亮文本颜色 |
| `criticalcolor` | 警示文本颜色 |
| `go_bgcolor` | GO 按钮（主要动作按钮）背景色 |
| `go_textcolor` | GO 按钮文本颜色 |
| `go_touchbgcolor` | GO 按钮按下时背景色 |
| `go_touchtextcolor` | GO 按钮按下时文本颜色 |

- `light` 主题现在也显式给出全部颜色；`Common.loadTheme` 的默认值只作为旧数据兜底
- 颜色必须是 `#RRGGBB` 形式，`loadTheme` 会解析成 ARGB int
- 新增主题时在 `data/theme.json` 加一项，并在
  `intl/<lang>/main.json` 的 `common.theme` 下补名称
- 主题列表顺序默认按 `data/theme.json` 的键顺序；风味仍可在 `flavor.json`
  里用 `themeOrder` 覆盖（可选，不是必填）

构建时 `modules/builtinData.js` 用 `Loader.fromFile("../data/theme.json")`
生成 `Common.themelist`；风味再用 `theme` / `themeOrder` 覆盖默认值和顺序。

## 新增风味

1. 新建 `flavors/<名字>/flavor.json`（至少写 `name`）
2. 用 `theme` 选择默认主题；需要特殊顺序可加可选的 `themeOrder`
3. 构建时加 `-flavor <名字>`；`-huangyxHUTAO` 是社区版的快捷写法

## 现有主题

所有主题都在 `data/theme.json`：

- `light`：浅色风格（白色；默认版默认主题）
- `dark`：夜间风格
- `pink`：晨星粉（社区版默认主题）
- `blue`：晨雾蓝
- `purple`：略略紫

`data/theme.json` 的键顺序 = pink/blue/purple/light/dark，两个版本的主题选择页都按
这个顺序显示；默认版选中白色 `light`，社区版选中粉色 `pink`。

## 参考

社区版（命令助手 Reforged）的效果参考图在本地下载里（历史页、设置页、主题选择页），
对应源码在 `Git/CA_reforged`。主题 id 的对应关系：

- `pink` ← `pink_theme`（颜色定义在 `modules/Common.js`）
- `blue` ← `blue_theme`
- `purple` ← `purple_theme`

## 验证

```bash
# 只验证脚本内容（不跑 Gradle）
node -e '
const getBuildConfig = require("./cabuildtools/tasks/getBuildConfig");
const loader = require("./cabuildtools/loader-ast");
for (const flavor of ["default", "huangyxHUTAO"]) {
  const ctx = {};
  getBuildConfig(ctx, ["release", "stable", flavor]);
  const out = loader.load("./main.js", { buildConfig: ctx.buildConfig, updateSources: require("./config/update-sources.json") }, "utf-8", { transformAsync: true });
  console.log(flavor, out.includes("南鸢晨星社区版 喵喵喵"), out.includes("by ProjectXero&南鸢晨星"));
}
'

# 真机
node ./cabuildtools/main.js shellBuildDebug -huangyxHUTAO
adb install -r build/outputs/debugApk/app-debug-huangyxHUTAO.apk
```

验证过（PERM10 早前）：huangyxHUTAO 风味启动后初始历史为社区版欢迎语、界面为粉色主题；
现在主题数据已归入 `data/theme.json`，两种风味只改变默认主题和主题选择页顺序。
