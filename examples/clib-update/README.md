# 拓展包更新机制 · 最小可运行模拟

这个文件夹是一个**能本地跑起来**的迷你模型：一份 clib 源码 → 构建成源站 → 模拟 App
检查更新 → 下载 → 验签 → 转成本地格式。每一步都对应真实代码里的一个函数。

## 目录

```
clib/            拓展包源码（对应 caclib/src/<id>/）
  library.js       元数据：id/name/author/source/uuid/version/versionCode
  main.js          库本体：App 运行时 eval 的那个对象（含 update 字段）
build.js         构建脚本（对应 cabuildtools/clibspawn.js）
simulate.js      模拟 App（对应 CA.Library 的更新流程）
site/            构建产物（不入库，相当于 caclib/outputs/ 发布出去的那一套）
keys/            演示密钥（不入库）
```

## 跑起来（不需要手机）

```bash
# 1. 发布 1.0.0
node build.js --version 1.0.0

# 2. 模拟「本地装的是 1.0.0」——会显示已是最新
node simulate.js --installed 1.0.0

# 3. 发布 1.0.1（版本号变了）
node build.js --version 1.0.1

# 4. 再模拟「本地还是 1.0.0」——完整走一遍更新流程
node simulate.js --installed 1.0.0
```

第 4 步会依次打印：

```
[1] GET info.json     源站入口
[2] GET map.json      uuid -> 更新描述地址
[3] GET demo.json     更新描述（version/url/sha1/source）
[4] 比较版本          1.0.1 vs 1.0.0 → 有更新
[5] GET demo.lib      SHA-1 校验 + RSA 验签
[6] 转 LIBSIGN01      本地格式（App 还会写 .hash）
[7] 完成              "重启命令助手生效"
```

并生成 `simulated-local.lib`（就是 App 更新成功后落盘的东西）。

## 每一步对应的真实代码

| 模拟里的步骤 | App 里的实现 |
| --- | --- |
| `[1] GET info.json` | `CA.Library.requestSourceInfo` |
| `[2] GET map.json` | `CA.Library.requestSourceMap` |
| `[3] GET demo.json` | `CA.Library.requestUpdateInfo`（读库的 `update` 字段） |
| `[4] 比较版本` | `NeteaseAdapter.compareVersion` |
| `[5] SHA-1 + RSA` | `CA.Library.downloadLib` |
| `[6] 转 LIBSIGN01` | 同 `downloadLib`（`LIBSIGN01 + sourceId + [9..]`） |
| `[7] 重启生效` | 拓展包管理界面文案 |

## 和真实 caclib 的对应

| 这里 | CA 仓库里 |
| --- | --- |
| `clib/` | `caclib/src/<id>/` |
| `build.js` | `cabuildtools/clibspawn.js` |
| `site/` | `caclib/outputs/`（发布到源站的那一套） |
| `keys/` | `caclib/signature/`（不入库） |

## 真机试一下

```bash
# 1. 用你电脑的局域网 IP 构建（手机要能访问到）
node build.js --base http://192.168.x.x:8000/ --version 1.1.0

# 2. 起一个静态服务器
python3 -m http.server 8000 -d site

# 3. 手机导入 site/demo-source.js（未编译的源码，安全级别 0 就能加载）
#    然后在「拓展包管理」里点「检测更新」→ 它会去 demo-source.js 的 update 字段指向的地址
# 4. 更新成功后重启命令助手；注意下载回来的 LIBSIGN01 需要安全级别 ≥ 2 才会加载
```

## 注意

- `keys/` 是演示用的临时密钥，**别拿去签真实的东西**
- `build.js --version` 会**改写 `clib/` 里的版本号**（为了让演示能反复构建不同版本）
- 版本比较用的是 `versionCode`（数组），不是 `version` 字符串
- 真实环境里 `library.js` 的 `version` 变了才会重新编译（`index.lock.json` 缓存），这里不做缓存
