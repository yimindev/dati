---
name: dati-ops
description: Use when performing configuration or operations on the DatI platform via its HTTP API — managing data sources, MCP services, subjects, terms, users, or running admin operations.
---

# Dati Ops

## 核心原则

**技能即前端。** 本技能让 agent 通过 HTTP API 完成前端界面上普通用户能做的全部配置与操作,作为 DatI 界面的替代品。

- 站在用户视角审视每一步:返回是否合理、字段是否自洽、操作顺序是否符合业务
- 本技能是**操作指南**,不是测试工具;验证 API 行为正确性用 `e2e-tester` 技能

## 前置要求与认证配置

优先从环境变量中读取连接信息，未配置时由用户在对话中提供：

| 项 | 对应环境变量 | 默认值 / 格式 | 说明 |
|---|---|---|---|
| `baseUrl` | `DATI_BASE_URL` | `http://localhost:8085` | 后端服务地址 |
| `apiKey` | `DATI_API_KEY` | `sk_...` | 访问凭据，所有 HTTP 请求带 `Authorization: Bearer <apiKey>` |

- **推荐方式**：在环境变量中设置 `export DATI_BASE_URL=...` 与 `export DATI_API_KEY=...`，Agent 执行时直接引用环境变量（如 `curl -H "Authorization: Bearer $DATI_API_KEY" $DATI_BASE_URL/v1/...`）。
- **兜底交互**：若环境变量未设置且当前对话上下文未提供，**必须先主动询问用户**获取，禁止自行臆测。认证失败表现为 401。

## 接口事实来源

本技能自包含: 接口定义以**技能目录下的 `openapi.json`** 为准(随技能分发,仓库内由项目脚本 `scripts/fetch-openapi.sh` 同步)。**不要整文件读取**(48KB ≈ 15K token),用查询工具按需取(统一加 `python3` 前缀调用,不依赖执行权限):

```bash
# 首选: 从仓库根调用
python3 skills/dati-ops/scripts/openapi.py /v1/data-sources POST
python3 skills/dati-ops/scripts/openapi.py --list
python3 skills/dati-ops/scripts/openapi.py --search password

# 若上述路径不可用(Git Bash / 目录变动): 先定位再调用
PY=$(find . -path "*dati-ops/scripts/openapi.py" | head -1)
python3 "$PY" /v1/data-sources POST
```

openapi.json 是自动生成物,可能滞后于代码;与后端实际行为不一致时,以实际请求结果为准。

## 使用流程

1. **确认环境**:baseUrl、apiKey 是否已知;未知先问用户
2. **查询接口**:`./scripts/openapi.py <path> <method>`,确认参数、请求体、响应
3. **构造请求**:body 用 snake_case;分离状态码与响应体,如 `curl -w "\n__HTTP__%{http_code}"`
4. **语义检查**:返回是否符合业务预期(ID 有效、状态流转正确、分页 total 合理);异常即使接口成功也要指出

## 功能面

### 数据源(含表、列)

| 操作 | 接口 |
|---|---|
| 连接测试 | `POST /v1/data-sources/test-connection` |
| 数据源 CRUD | `POST /v1/data-sources` · `GET/PUT/DELETE /v1/data-sources/{id}` · `GET /v1/data-sources` |
| 浏览结构 | `GET .../schemas` · `GET .../schemas/{schema}/tables` · `GET .../tables` · `GET .../tables/{tableId}/columns` |
| 建表 | `GET .../tables/added-names` → `POST .../tables/batch` → `POST .../tables/{tableId}/columns/sync` |
| 列元数据/字典值 | `PUT .../tables/{tableId}/columns/{id}` · `GET/PUT .../columns/{columnId}/values` · `POST .../values/extract`(完整路径见 datasource.md) |

→ 业务要点见 [references/datasource.md](references/datasource.md)

### 主题与术语

| 操作 | 接口 |
|---|---|
| 主题 CRUD | `POST /v1/subjects` · `GET/PUT/DELETE /v1/subjects/{id}` · `GET /v1/subjects` |
| 主题选表 | `GET .../available-tables` → `POST /v1/subjects/{id}/tables` · `DELETE .../tables/{tableId}` |
| 术语 CRUD | `GET/POST /v1/subjects/{subjectId}/terms` · `GET/PUT/DELETE /v1/terms/{id}` |
| 术语关联 | `POST /v1/terms/{id}/relations` · `DELETE .../relations/{tableId}/{fieldName}` |

→ 业务要点见 [references/subject.md](references/subject.md)

### MCP 服务(含工具、prompt)

| 操作 | 接口 |
|---|---|
| 服务 CRUD | `POST /v1/mcp-services` · `GET/PUT/DELETE /v1/mcp-services/{id}` · `GET /v1/mcp-services` |
| 数据范围 | `GET/PUT /v1/mcp-services/{id}/data-scope` |
| 发布/启停 | `POST .../publish` · `POST .../enable` · `POST .../disable` |
| 版本管理 | `GET .../diff` · `GET .../snapshots` · `POST .../rollback` |
| 工具 | `GET/POST .../tools` · `PUT/DELETE .../tools/{toolId}` · `POST .../tools/{toolId}/test` · `POST .../tools/detect-annotations` |
| prompt | `GET/POST .../prompts` · `PUT/DELETE .../prompts/{promptId}` |
| 模板 | `POST /v1/template/preview` · `POST /v1/template/extract` |

→ 工具类型/配置/使用策略见 [references/tools.md](references/tools.md),模板语法见 [references/templates.md](references/templates.md),场景设计方法论见 [references/scenario-design.md](references/scenario-design.md)

→ 业务要点见 [references/mcp-service.md](references/mcp-service.md)

### 不覆盖

注册/登录(`/v1/auth/register|login`)、API Keys(`/v1/auth/api-keys`)、ACL 授权(`/v1/acls/*`)、用户搜索(`/v1/users/search`)、系统配置(`/v1/system/config`)、MCP 端点调用(`/{code}/mcp`)。

## 端到端工作流

### 工作流 1:接入数据源 → 建主题 → 术语

```text
1. POST /v1/data-sources/test-connection  验证连接(返回 true)
2. POST /v1/data-sources                  创建数据源 → 拿 {id}
3. GET  /v1/data-sources/{id}/schemas     列出 schema
4. GET  .../schemas/{schema}/tables       浏览表
5. GET  .../tables/added-names            确认待添加表
6. POST .../tables/batch                  批量建表(AddTableRequest 数组)
7. POST .../tables/{tableId}/columns/sync 同步列
8. PUT  .../columns/{id}                  列元数据增强:维护描述、别名(对业务关键列)
9. POST .../columns/{columnId}/values/extract  开启字典值抽取(枚举/维度列)
10. PUT .../columns/{columnId}/values    保存/覆盖字典值与同义词
11. POST /v1/subjects                     创建主题(datasource_id + name)
12. GET  /v1/subjects/{id}/available-tables?schema=xxx
13. POST /v1/subjects/{id}/tables         添加表
14. POST /v1/subjects/{subjectId}/terms   创建术语
```

> 步骤 8-10 是**语义层建设的关键**——同步列只是拿到原始结构,描述/别名/字典值才让 LLM 理解业务语义。只接数不标注,后续 NL2SQL 命中率会差(见 references/scenario-design.md)。

### 工作流 2:创建并发布 MCP 服务

```text
1. POST /v1/mcp-services                  创建服务,body 必须含 data_scopes(数据源/主题引用)
2. POST .../publish                       发布 → 返回 snapshot id;状态 DRAFT → PUBLISHED
3. POST .../disable                       停用(下线)
4. POST .../enable                       恢复上线
5. GET  .../diff                          ⚠ 发布前使用,对比草稿与线上差异
6. GET  .../snapshots → POST .../rollback 回滚到指定版本(target_version_number)
```

## 常见错误

| 错误 | 后果 |
|---|---|
| 整文件读 openapi.json | 浪费 ~15K token;应查询 |
| 漏 `Authorization: Bearer` 头 | 401 |
| body 用 camelCase | 字段解析失败;必须 snake_case |
| 请求体带 `id`/`created_at` 等只读字段 | 语义混乱;只传业务字段 |
| 误读 `batchAddTables` 响应 | 返回的 id 是**添加的表数量**,不是表 id |
| 数据范围未配就发布 | `MS_SERVICE_DATA_SCOPE_EMPTY` |
| 误认为 DISABLED 下发布=上线 | 发布≠上线,DISABLED 状态发布后仍为 DISABLED |
