/* data/idlibs/schema_v1.json 说明
 *
 * 命令库（.json）的 JSON Schema 定义，**不是数据**——它规定上面那些字段的类型与约束。
 *
 * 字段说明：
 *   $schema          —— 指向本目录的 schema_v1.json，校验用
 *   $id              —— JSON Schema 自身的标识
 *   definitions      —— JSON Schema 内部定义（当前 55 项）
 *   type             —— JSON Schema 的类型声明
 *   title            —— JSON Schema 的标题
 *   description      —— 一句话说明这个库包含什么、什么时候该启用
 *   properties       —— JSON Schema 的属性定义（当前 12 项）
 *   required         —— JSON Schema 的必填字段（当前 6 项）
 *   allOf            —— （见 JSON 内容）（当前 1 项）
 *
 * 修改方式：
 *   1. 改这个文件确认字段含义
 *   2. 改 data/idlibs/schema_v1.json 里对应的值
 */
