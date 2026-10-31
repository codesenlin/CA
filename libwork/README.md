# libwork：批量转换 .lib 的工作目录

把源码 / 包 / 密钥按下面目录放好，直接跑 `npm run pack` / `unpack` / `info`。
命令在给「文件」时是单文件模式，给「目录」或不给目标时是批量模式（本目录）。

## 目录约定

```
libwork/
├── src/    放源码：.json（JSON 库）/.js（插件库）
├── lib/    放 .lib：待解包 / 待转签名的包
└── keys/   放密钥：私钥（--sign 用）、公钥或证书（--key / info 自动验签用）
```

产物统一写到 `build/libwork/` 下（`build/` 已被 gitignore）：

```
build/libwork/
├── pack/      src/ 里源码打包出的 .lib
└── unpack/    lib/ 里 .lib 解包出的 .json / .js
```

`libwork/src`、`libwork/lib` 的文件可以按需入库；**`libwork/keys` 全部内容
不入库**（见 `libwork/keys/.gitignore`），避免把私钥/证书提交上去。

git 不跟踪空目录，所以 `src/`、`lib/` 里放 `.gitkeep` 占位、`keys/` 里用
`.gitignore`（内容 `*` + `!.gitignore`）占位——这样三个目录都能进仓库，
同时 keys 的内容仍然全部忽略。


## 目标写法

| 写法 | 行为 |
|---|---|
| 不给目标 | 批量处理本工作区：`src/` → `build/libwork/pack/`，`lib/` → `build/libwork/unpack/`（不参与密钥） |
| 纯文件名 | 只在工作区对应目录找（pack→`src/`，unpack→`lib/`）；可省略后缀，会自动匹配并提示 |
| 带路径文件 | 对指定文件操作；可省略后缀（提示后处理） |
| 带路径文件夹 | 只处理里面的 `.json`/`.js`（pack）或 `.lib`（unpack） |

```bash
npm run pack                          # 批量：src/ → build/libwork/pack
npm run pack -- foo                   # 工作区 src/foo.json 或 foo.js
npm run pack -- /data/lib --sign testkey_private.pem   # 指定文件夹
npm run unpack -- foo.lib
npm run unpack -- /data/libs --key /data/keys
npm run info -- build/libwork/pack/foo.lib
```

## 密钥规则

| `--sign`（pack）/ `--key`（unpack、info） | 行为 |
|---|---|
| 不给 | pack 不加密；unpack/info 遍历工作区 `keys/` 尝试匹配签名 |
| 纯文件名 | 只在工作区 `keys/` 里找，**必须完整文件名（含后缀）** |
| 带路径文件 | 用指定密钥文件；显式 key 验签失败退出码 2 |
| 带路径文件夹 | pack **不支持**；unpack/info 先工作区 `keys/`，再遍历该目录 |

`info` 只读单个文件：非签名包直接输出基础信息，签名包按上表验签。
参数按 npm 标准写法放在 `--` 后面。
