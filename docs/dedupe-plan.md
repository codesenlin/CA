# 去重后续计划

第三轮去重已收尾（release `main.js` 534,928 → 518,124 字节，约 -3.1%）。
下面继续记录尚未处理/已处理的项。

## P1：登录 / 评论 / 反馈输入表单

涉及：

- `modules/network/GiteeFeedback.js`：`showLogin`（账号密码登录、匿名使用、浏览器登录）、
  `showFeedbackDetail` 评论框
- `modules/network/UserManager.js`：登录 / 注册 / 找回密码/ 改密表单
- `modules/network/IssueService.js`：评论输入框

重复内容：`L.ScrollView + L.LinearLayout + L.TextView/L.EditText` 的弹窗表单、
按钮组、进度对话框和登录回调，约 **100 行**。

已完成（本轮）：

- `NetworkUI.commentBar`：评论输入条（GiteeFeedback / IssueService 共用）
- `NetworkUI.editIssueDialog`：编辑/新建话题对话框（GiteeFeedback / IssueService 共用）
- `IssueService.showEditIssue`、`GiteeFeedback.showEditIssue`、两处评论条已迁移
- `IssueService` / `GiteeFeedback` 的话题详情页已收敛到 `issueDetailHeader` +
  `issueDetailPage`
- `NetworkUI.formDialog`：通用标题 + 输入框 + 说明 + 按钮表单
- `GiteeFeedback.showLogin`、`UserManager` 的登录 / 注册 / 忘记密码 / 改密表单
  已迁移；自动构建通过，线上账号登录流程仍需真机手动验证

剩余验证：

- 真实账号/联网手动走一遍：登录、匿名登录、浏览器登录、评论发送、
  注册/找回密码/改密流程
- 自动构建目前只能验证脚本编译、启动和表单 UI 构造

## P2：JSONLanguageService 内部剩余副本

涉及：`modules/JSONLanguageService.js`（vendored bundle）。

已处理：独立的 `jsonc-parser` bundle（整份删除）、`_interopRequireWildcard`
（8 份 → 1 份）、`mergeSort`（2 份 → 1 份）、module 30 的重复
`FullTextDocument`（改为 require module 29 的 `vscode-languageserver-textdocument`）。

剩余重复：

- meta-schema 常量（如 `$vocabulary` / `multipleOf` 相关 schema 数组）在 bundle 内出现若干份

已验证：
- Node 下对比改动前后 `TextDocument.create/update/applyEdits`、`doComplete`
  多个光标位置、`findNodeAtOffset`，结果一致
- 删除约 5.9KB / 141 行

不建议手改：这些是 webpack 模块的独立副本，删一份需要改模块引用，容易破坏解析/补全。
后续做法：

1. 从上游 `vscode-json-languageservice` + `jsonc-parser` 源码重新打包这一份文件，
   只保留一份 jsonc-parser，并重新套上本仓库的同步 Promise / Rhino polyfill 补丁
2. 用 Node 对比原/新 bundle：`getLanguageService` 的 `doComplete` 多个光标位置、
   `findNodeAtOffset` 结果一致
3. 跑 `loader` 编译、`npm test`、`buildRelease` 对比体积、真机验证 jsonPro 补全

风险：bundle 重建涉及版本/打包配置，需单独验证补全、schema 校验、错误提示。

## cabuildtools 子项目

已完成：

- 新增 `cabuildtools/channel.js`：`buildRelease` / `buildSnapshot` 共用的渠道参数解析
- 新增 `cabuildtools/libformat.js`：`.lib` 容器格式（LIBRARY / SIGNLIB01、
  gzip、时间戳、签名）的唯一实现；`libtool.js`、`js2lib.js`、`js2signlib.js`
  共用，去掉三份重复实现

已完成（本轮）：

- ✅ `clibspawn` 切到 `loader-ast`，删除旧 `loader.js`（94 行）；
  AST loader 补齐 LOADER 注释里的 `fs` / 原始文件 `load` / 相对 `require` 兼容，
  `.json` clib 源按原文读取
- 新增 loader 测试覆盖 LOADER 注释兼容；`clibspawn` 用临时签名密钥跑通全部 5 个库

后续可做：

- `main.js` 中 `Build.execute` 与 `procArgs` 的 Process/Success 日志重复
  （约 10 行，收益低）
- `publish*` 各任务里 sftp/文件写入样板（量小，暂不处理）

## 验证清单（每次去重后）

```bash
node --check <改动的单文件>
node ./cabuildtools/main.js buildRelease        # 看产物大小
npm test                                        # 6 + 7
node ./cabuildtools/main.js shellBuildDebug     # 真机安装
adb logcat | grep Script                        # 脚本编译/执行
```

JSONLanguageService 改动额外做：

```js
// Node 里加载模块，对比补丁前后
doComplete(...多个位置...)
findNodeAtOffset(root, offset, true)
```

## 已完成（参考）

- 删除独立 jsonc-parser bundle；`_interopRequireWildcard` / `mergeSort` 收敛
- `ListAdapterBase`：observer/errorView + `create()` 公共方法
- `ConsolePanel`：DebugUtils / WSServer 控制台
- `NetworkUI`：列表页外壳、列表项 vmaker、关闭按钮、状态菜单、进度对话框、分页流程
- `NetworkUI.commentBar` / `editIssueDialog`：网络评论输入条与编辑话题表单
- `NetworkUI.issueDetailHeader` / `issueDetailPage`：GiteeFeedback / IssueService
  话题详情标题栏与页面外壳（返回、标题、菜单、列表挂载、关闭）
- `cabuildtools/main.js`：Process/Success/Failed 日志收敛到 logTaskStart/Success/Failure
- `cabuildtools/publish.js`：preparePublish→publishFile→endPublish 链、Publishing 日志、
  异步/串行上传收敛；publishRelease/Snapshot/Hotfix/Directory/File 共用
- `UIHelp.settingItem`：CA.js / Common.js 的设置项 vmaker
