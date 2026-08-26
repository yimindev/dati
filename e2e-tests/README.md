# E2E 测试用例编写规范

本目录包含 DatI 后端的端到端测试用例。测试由 AI Agent 自动执行——Agent 读取用例、理解意图、自主调用 API、生成报告。

## 总宗旨

**站在用户使用角度审视一切。**

- 用例是操作指引，不是完整验收标准
- Agent 执行时必须主动审视返回内容的完整性、合理性、自洽性
- 即使用例没写，响应中任何让用户困惑的地方都应报告
- 「用例通过」= 「用户用起来没问题」

## 目录结构

```
e2e-tests/
  README.md           # 本文件
  test-env.yaml       # 环境配置（Agent 的环境参考）
  test-data.yaml      # 公共测试数据（可复用的用户、连接信息等）
  test-cases/         # 测试用例，按模块分文件
    auth.md           # 认证
    api-key.md        # API Key 管理（TC-AK-001~009，含 MCP 验收固定 key 构造）
    datasource.md     # 数据源管理
    mcp-service.md    # MCP 服务管理
    mcp-endpoint.md   # MCP 对外 endpoint（/{code}/mcp JSON-RPC）
    ...
  conformance/        # MCP Conformance 验收资产
    baseline.yml      # 预期失败基线（入库）
    results/          # 单次运行结果（可再生产物，gitignore）
```

> MCP 验收脚本（`mcp-verify.sh`、`mcp-conformance-proxy.js`）属通用测试能力，
> 位于 `.agents/skills/e2e-tester/scripts/`（与 `service.sh`/`run-tests.sh` 同级），用法见 skill 文档。

## 用例编写格式

每个用例是一个 `## TC-XXX 标题` 块，包含：

```markdown
## TC-DS-001 创建数据源
**级别：** P0
**前置：** 已登录

1. 创建一个 MySQL 数据源，连接信息使用 test-data.yaml 中的 datasources.mysql_local
2. 在数据源列表中搜索该数据源名称，确认能找到
3. 删除该数据源（清理）
```

### 级别

- **P0**：核心流程，每次必跑（CRUD、认证）
- **P1**：重要分支和边界（错误处理、权限、并发）
- **P2**：边缘场景，低频变更时才跑（特殊输入、极端参数）

### 前置

描述测试开始前需要满足的条件。Agent 会自动完成：
- `已登录`：自动执行注册+登录获取 token
- `已有数据源`：Agent 会先创建一个再做后续测试
- `数据库可用`：表示需要真实的数据库连接，本地没有时会跳过

### 步骤写法

**用自然语言描述操作意图，不写 API 路径和方法。** Agent 会自己从 `../docs/api/openapi.json` 匹配。

✅ 好的写法：
- `创建一个 MySQL 数据源，使用 test-data.yaml 中的 xxx`
- `搜索名为 xxx 的数据源，确认能找到`
- `删除该数据源（清理）`

❌ 避免的写法：
- `POST /v1/data-sources` — 不需要写路径
- `payload: {name: "xxx"}` — 不需要写 body 结构
- `expect: status: 200` — 不需要写断言代码

### 数据引用

引用 test-data.yaml 中的数据用自然语言描述：
- `使用 test-data.yaml 中的 datasources.mysql_local`
- `用户使用 test-data.yaml 中的 users.admin`

### 预期结果

用自然语言描述预期：
- `应返回 200 且响应包含 id`
- `应返回 400 错误`
- `列表中应包含刚创建的数据源`
- `连接测试应失败`
- `token 应有效，后续可访问受保护接口`

## 运行方式

```
Agent 加载 e2e-tester skill → 选择目标模块 → Agent 自主执行并报告
```

## 已知陷阱（Agent 必读）

执行测试前必须了解以下约定，避免踩坑：

### 字段命名
- DataSource 用 `jdbc_url`（完整 JDBC URL），**不是** `host` + `port` + `database`
- DataSource 的 `type` 是 **大写枚举值**（`POSTGRESQL`、`MYSQL`、`H2` 等），不是小写
- Login 的 required 字段是 `name`、`password`、`type`，**不是** `username`
- 所有 JSON body 使用 **snake_case**（Spring 配置了 `SNAKE_CASE`）

### 认证
- 大多数 `/v1/**` 接口需要认证（`Authorization: Bearer <token>`）
- 免认证路径仅限：`/v1/auth/login`、`/v1/auth/register`、`/v1/public/**`
- `test-connection` 接口**也需要认证**

### 服务启动
- 启动命令必须指定 workingDirectory：
  ```bash
  mvn -f backend/pom.xml spring-boot:run -Dspring-boot.run.workingDirectory=/Users/zhangyimin/IdeaProjects/dati
  ```
- 以 H2 file 模式运行，数据库文件在 `./db/dati`

### 测试用 PostgreSQL
- `public` schema 下有 11 张 Chinook 示例表：album、artist、customer、employee、genre、invoice、invoiceline、mediatype、playlist、playlisttrack、track
- 连接信息见 `test-data.yaml` 中的 `datasources.postgres_local`
