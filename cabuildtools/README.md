# 命令助手构建工具

命令助手（CA）与 Android 壳子的构建流水线：脚本打包、Kotlin/JS 产物、APK、签名、发布和拓展包。

## 环境

```text
Node.js ^22.18.0 或 >=24.11.0
```

依赖通过根目录的 npm workspace 管理：

```bash
npm install
```

## 常用命令

```bash
node ./cabuildtools/main.js help

npm run build        # 正式版脚本 + 安装器 .lib
npm run snapshot     # 快照版
npm test             # Loader / async transform 测试
npm run kotlin:js    # 只构建 Kotlin/JS 产物
npm run pack         # 打包 .lib
npm run unpack       # 解包 .lib
```

任务参数、渠道和风味说明直接看 `npm run help`。

## 目录

```text
main.js               CLI 与任务加载器
tasks/
  build/              脚本/Kotlin 构建、源码拼装、产物写入
  shell/              Android APK、签名、对齐、Gradle 与热更新
  publish/            SFTP / 发布流水线
  clib/               内置命令库
  caaccel/            CA 加速库构建 / 测试 / 基准
  config/             构建配置、目录初始化、import 索引
  help.js             帮助
cli/                  CLI 入口（deps / gradlew / libtool / js2localib）
lib/                  构建库（loader、签名、lib 格式、发布、配置等）
tests/                Loader AST / async 转换测试
```

任务按 `tasks/` 下的文件名注册，目录可递归；新增任务放进对应分类即可，改文件名等于改任务名。

## 注意

- 私钥、SFTP 密码等敏感文件不在这里提交，路径由根目录 `config/` 和 `source-config.json` 指定。
- 生成物统一写到根目录 `build/`，不提交构建产物。
