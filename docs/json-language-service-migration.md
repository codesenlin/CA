# JSONLanguageService 迁移记录

`modules/JSONLanguageService.js` 是 508KB 的 `vscode-json-languageservice` webpack bundle，
不是普通业务模块。它不是“拆开一个对象”就能翻译的代码，因此单独作为迁移阶段处理。

## 当前用途

CA 只使用其中一小部分 API：

```text
JSONLanguageService.getLanguageService(options)
JSONLanguageService.TextDocument.create(uri, languageId, version, text)
JSONLanguageService.findNodeAtOffset(root, offset, includeRightBound)

service.configure({ schemas: [...] })
service.parseJSONDocument(document)
service.doComplete(document, position, jsonDocument)
service.doValidation(document, jsonDocument)
```

调用位置：

- `src/ca/CAIntelliSenseModule.kt`
  - jsonPro 补全：`doComplete`
  - 参数校验：`doValidation`
  - 光标节点定位：`findNodeAtOffset`

## 目标架构

新增 Kotlin 实现：

```text
src/json/JsonTextDocument.kt
src/json/JsoncParser.kt
src/json/JsonSchemaService.kt
src/json/JsonLanguageServiceCore.kt（阶段 1）
```

最终由 Kotlin 注册 `MapScript.loadModule("JSONLanguageService", api)`，删除旧 vendor JS。

## 阶段

1. ✅ **Parser / TextDocument / findNodeAtOffset**：位置、节点树、语法错误。
2. ✅ **Schema registry / configure / validation**：schema 匹配与诊断。
3. ✅ **Completion**：`doComplete` 的对象/数组/值候选、detail、insertText。
4. ✅ **parity**：用旧 vendor 作为 oracle，覆盖 CA 实际使用的 schema 与 jsonPro 输入（阶段 1-3 parity 已通过）。
5. ✅ **接入 CAIntelliSenseModule.kt，真机验证通过，已删除 vendor 和 oracle 测试。**

迁移完成后旧 `JSONLanguageService.js` 已删除；仓库中不再保留该 vendor。
