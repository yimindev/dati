# MCP 工具参考与使用策略

## 工具类型

MCP 服务共 **8 种工具类型**:7 种预置(平台内置,随发布生效)+ 1 种自定义。

### 预置工具(只读 4 种)

| 工具名 | 用途 | 特性 |
|---|---|---|
| `search_metadata` | 跨数据源按关键词搜索表、列、样例值、业务术语 | 只读,默认启用 |
| `get_table_info` | 获取最多 20 张表的完整列 schema(名称/类型/注释/样例值) | 只读,默认启用 |
| `list_tables` | 列出可用表(schema/名称/描述,不含列) | 只读,默认启用 |
| `execute_sql` | 执行 SQL 查询或语句,允许 `SELECT`/`INSERT`,最大 1000 行 | 读写,默认启用 |

> **Context 注入**:`execute_sql` 暴露给 LLM 时,描述末尾**自动追加当前认证用户信息**(`Context: current user name: xxx, user id: yyy`)。LLM 据此判断何时加 `WHERE user_name = 'xxx'`(个人查询)或写全局聚合(家庭/团队查询)。

### 预置工具(元数据写入 3 种)

| 工具名 | 用途 | 特性 |
|---|---|---|
| `update_table_info` | 分析后补充表描述/别名,改善后续检索 | 写,幂等,默认关闭 |
| `update_column_info` | 补充列描述(枚举含义、值格式)/别名 | 写,幂等,默认关闭 |
| `upsert_term` | 在主题下创建/更新业务术语 | 写,幂等,默认关闭 |

### 自定义工具(PARAMETERIZED_SQL)

用户定义的参数化 SQL 工具,名称/描述/模板/参数全部自定义,用于**写操作和复杂业务操作**。

> 响应结构: `GET .../tools` 返回 `{prebuilt: [...], custom: [...]}`(**不是平铺数组**)。

## 自定义工具配置

创建(`POST /v1/mcp-services/{serviceId}/tools`)/更新(`PUT .../tools/{toolId}`)时,`config` 为 **JSON 字符串**(注意:camelCase):

```json
{
  "tool_type": "PARAMETERIZED_SQL",
  "name": "add_transaction",
  "title": "记账",
  "description": "记一笔家庭账...",
  "config": "{\"dataSourceId\":\"<数据源id>\",\"sqlTemplate\":\"INSERT INTO ... {{type}} ...\",\"parameters\":[{\"name\":\"type\",\"type\":\"String\",\"required\":true,\"description\":\"收支类型\"}],\"timeout\":30,\"maxRows\":1000,\"readOnly\":false,\"idempotent\":true,\"destructive\":false}"
}
```

**config(ParamSqlConfig)字段**:

| 字段 | 类型 | 默认 | 说明 |
|---|---|---|---|
| `dataSourceId` | string | — | 数据源 ID(模板 SQL 执行的目标) |
| `sqlTemplate` | string | — | SQL 模板(语法见 [templates.md](templates.md)) |
| `parameters` | ToolParameter[] | — | 参数声明(决定 LLM 调用时的入参 schema) |
| `timeout` | int | 30 | 超时秒数 |
| `maxRows` | int | 1000 | 最大返回行数 |
| `readOnly` | bool | — | 声明只读(影响 LLM 决策与安全检查) |
| `idempotent` | bool | — | 声明幂等(可安全重试) |
| `destructive` | bool | — | 声明破坏性(高影响操作) |

**ToolParameter**:`name`、`type`(`String`/`Number`/`Boolean`/`DateTime`/`Array`)、`required`、`defaultValue`、`description`。参数类型决定 LLM 传参格式,`description` 要写清格式约束(如 `日期格式 YYYY-MM-DD`)。

## 工具使用策略

**读场景 → `execute_sql` 或预置只读工具**;写场景/复杂业务 → **自定义参数化工具**。设计规范:

1. **写操作必须走自定义工具**(`execute_sql` 只允许 SELECT/INSERT,且自定义工具能承载业务校验与归属绑定)
2. **归属绑定**:模板里用 `{{_user.name}}` 把数据绑定到当前用户(如 `WHERE user_name = {{_user.name}}`),物理级防越权
3. **描述 = 给 LLM 的操作手册**:写清触发条件、必填参数、边界约束、失败恢复指引。示例:
   > "amount 必须为正数;EXPENSE/INCOME 必须填 category;TRANSFER 必须填 transfer_to_account 且不填 category...若调用后受影响行数为 0(affected_rows: 0),说明账户可能尚未初始化,请先调用 init_default_accounts 再重试"
4. **幂等优先**:初始化类操作用 `ON CONFLICT DO NOTHING` 等幂等写法,LLM 可安全重试
5. **模板安全**:默认用 `{{var}}`(参数绑定);仅可信值用 `{{{var}}}`
6. **发布前验证**:`POST .../tools/{toolId}/test` 实测工具(传参、看渲染 SQL 与结果);`POST .../tools/detect-annotations` 检测模板的只读/幂等/破坏性属性
7. **工具开关**:预置元数据写入工具默认关闭,按需开启;自定义工具 `enabled` 默认 true
