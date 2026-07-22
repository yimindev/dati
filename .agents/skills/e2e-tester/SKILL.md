---
name: e2e-tester
description: Use when you need to run E2E HTTP integration tests against the running DatI backend using test cases defined in e2e-tests/test-cases/
---

# E2E Tester

## 核心原则

**站在用户使用角度审视一切。** 用例只是操作指引，不代表完整的验收标准。Agent 在执行每个步骤时，必须主动审视返回内容的合理性：

- 不仅检查用例明确写的断言，更要**检查所有返回字段是否完整、合理、自洽**
- 想象你是真实用户——响应里有没有让你困惑的地方？缺少了应该有的信息？多了不该暴露的敏感数据？
- 时间戳是否合理（不能是未来或 1970 年）？ID 格式是否规范？关联数据是否一致？
- **发现任何不合理之处，即使用例没写，也要在报告中标出**（WARN 级别，不影响 PASS/FAIL）

用例通过 = 用户用起来没问题。

## Overview

对 DatI 后端执行黑盒端到端 HTTP 测试。测试用例用自然语言 Markdown 描述操作意图，Agent 自主理解意图、匹配 API、构造请求、执行断言、生成报告。

## 执行模式

### 模式 A：当前进程执行（默认）

Agent 在当前会话中逐步执行用例。适合单个模块、快速验证。

当前进程负责服务的完整生命周期：启动 → 执行 → 关闭。

### 模式 B：子进程执行（批量 / CI）

当前 Agent **不亲自执行测试**，而是充当调度器，用 `pi -p` 启动独立子进程来跑测试：

```bash
pi -p --name "e2e-<module>" \
   --model deepseek/deepseek-v4-flash \
   "执行 <模块> 模块的 E2E 测试。服务已就绪，按 e2e-tests/test-cases/<module>.md 中的用例逐个执行，完成后输出测试报告。"
```

调度器的职责：
1. 确保后端服务已启动（若未启动则先启动）
2. 对每个目标模块 spawn 一个 `pi -p` 子进程，多个模块可以**并行执行**（各子进程独立，互不干扰）
3. 子进程末尾加 `&` 放入后台，用 `wait` 等待全部完成
4. 收集所有子进程的输出
5. 汇总各模块的 PASS/FAIL/WARN 结果，输出合并的总报告
6. **所有子进程完成后，由调度器关闭后端服务**

**子进程的职责**（与模式 A 不同）：
- 子进程**不负责服务的启停**，假设服务已就绪
- 传给子进程的 prompt 不应包含「启动后端服务」
- 子进程 pi 在项目根目录启动时会**自动发现并加载 `.agents/skills/e2e-tester/SKILL.md`**，无需显式传 `--skill`

何时用子进程模式：
- 跑多个模块（全部 / 一组），避免当前上下文爆炸
- CI / 定时任务
- 用户说「跑全部测试」「批量验证」等触发词

## 文件结构

```
e2e-tests/
  test-env.yaml              # 环境配置（base_url、认证方式、约定）
  test-data.yaml             # 公共测试数据（用户、连接信息等）
  test-cases/<module>.md     # 测试用例，按模块分 Markdown 文件
  README.md                  # 用例编写规范

docs/api/
  openapi.json               # API schema（自动生成）
```

## Execution Flow

### 1. 加载

**始终加载 `e2e-tests/test-env.yaml`** 获取环境配置。

按用户指定的模块加载对应用例文件（如 `e2e-tests/test-cases/datasource.md`）。未指定则列出所有可用模块供用户选择。

**总是加载 `docs/api/openapi.json`** 作为 API 字典。

用例引用了 test-data.yaml 中数据时，加载对应部分。

### 2. 确保服务运行

检查 `test-env.yaml` 中的 `server.base_url` 是否可达：

```bash
curl -s -o /dev/null -w "%{http_code}" <base_url>
```

若不可达，按 `server.start_command` 启动服务，等待 `server.ready_signal` 出现。

### 3. 解析用例

用例是 Markdown 格式，每个用例以 `## TC-XXX 标题` 开始。解析：

- **级别**：P0（核心）/ P1（重要）/ P2（边缘），默认全部执行，可筛选
- **前置**：自然语言描述。Agent 理解后自动准备（如「已登录」→ 执行注册+登录）
- **步骤**：编号列表，每行一段自然语言操作描述
- **预期**：自然语言描述结果，Agent 做语义级断言

### 4. 理解意图 → 匹配 API

对每个步骤的自然语言描述，Agent 自主判断：

1. **操作类型**：创建/查询/更新/删除/测试连接/执行SQL/...
2. **目标资源**：数据源/MCP服务/主题/术语/用户/...
3. **匹配端点**：在 openapi.json 的 paths 中找语义匹配的端点
4. **构造请求**：
   - 字段名以 openapi.json schema 为准（如 LoginRequest 用 `name` 不是 `username`）
   - 字段值从步骤描述和 test-data.yaml 中提取
   - body 使用 snake_case

**Schema 校验规则（强制执行）**：
- 请求 body 的字段名必须与 openapi.json schema 的 properties 一致
- required 字段必须提供，optional 字段可省略
- 若步骤描述中有 schema 不存在的字段名 → 报错并提示正确字段名

### 5. 认证

若用例前置包含「已登录」或步骤需要认证：

a. 生成唯一用户名：`qa-{timestamp}-{4-random-chars}`（参考 test-env.yaml 约定）
b. 注册 + 登录获取 token
c. 若注册返回「already exists」**不视为失败**，直接登录
d. 后续请求带 `Authorization: Bearer <token>`
e. 登录必须传 `type: "local"`（LoginRequest schema required 字段）

### 6. 执行步骤

对每个步骤构造 curl 请求：

```bash
curl -s -w "\n__HTTP__%{http_code}" -X <METHOD> <URL> \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '<JSON_BODY>'
```

分离响应体和状态码：`__HTTP__` 之后为状态码，之前为响应体。用 `sed` 提取。

### 7. 断言

根据步骤中的预期描述做语义级判断：

- 「返回 200」→ 状态码 == 200
- 「返回 token」→ 响应 JSON 包含 `token` 字段且非空
- 「能搜索到」→ 列表中包含目标数据
- 「返回 400」→ 状态码 == 400
- 「连接失败」→ 状态码 != 200
- 「与原始一致」→ 新建/修改后查详情，关键字段与输入匹配

### 8. 生成报告

```markdown
## E2E Test Report

PASS  TC-AUTH-001  用户注册与登录
  ✓ 注册新用户 → 200
  ✓ 登录获取 token → 200
  ✓ token 访问受保护接口 → 200
  ⚠ password 字段在列表响应中明文暴露

FAIL  TC-AUTH-002  错误密码登录
  ✗ 预期返回 400/401，实际返回 200

---
Total: N executed, M passed, K failed
Warnings: W (用户视角发现的不合理之处)
```

### 9. 清理

- **模式 A（当前进程）**：测试完成后自动关闭服务
- **模式 B（子进程）**：子进程不关闭服务，由父进程调度器在所有子进程完成后统一关闭
- 用例中明确标注「（清理）」的删除操作，Agent 应执行

### 10. Bug 跟踪

测试完成后，将 WARN 和 FAIL 与 `e2e-tests/BUGS.yaml` 交叉比对：

```
发现的问题 → 查 BUGS.yaml
  ├─ 匹配到已有记录   → 更新 found_at（已知问题，仍在）
  ├─ 匹配到但在 FIXED  → REGRESSION！从 FIXED 移回 BUGS.yaml
  └─ 未匹配           → 新问题 → 追加到 BUGS.yaml 并分配 BUG-NNN
```

**之前 open 的问题本次未复现** → 从 BUGS.yaml 删除，移入 `e2e-tests/BUGS-FIXED.yaml`。

**之前 fixed 的问题本次复现** → 从 FIXED 移回 BUGS.yaml，标记 REGRESSION。

报告末尾输出：

```markdown
## Bug 跟踪

### 本次新增
- BUG-004 创建后不返回完整字段 → 已录入 BUGS.yaml

### 已知仍在
- BUG-002 created_user_name 为 null（第 3 次复现）

### 已修复验证
- BUG-001 连接超时 → 本次未复现，已移入 BUGS-FIXED.yaml
```

## Common Mistakes

### 字段命名陷阱
| 错误 | 正确做法 |
|------|---------|
| 登录传 `username` | LoginRequest 的 required 字段是 `name`、`password`、`type` |
| DataSource 用 `host`+`port`+`database` 分拆字段 | DataSource 用 **`jdbc_url`**（完整 JDBC URL，如 `jdbc:postgresql://localhost:5432/postgres`） |
| `type` 用小写（`postgresql`、`mysql`） | `type` 必须用**大写枚举值**：`POSTGRESQL`、`MYSQL`、`H2` 等 |
| 忘记传 `type: "local"` | Login 的 `type` 是 required |

### 认证陷阱
| 错误 | 正确做法 |
|------|---------|
| 以为 `test-connection` 不需要认证 | **需要认证**，必须带 token |
| 忘记认证直接调受保护接口 | 检查用例前置，先完成认证再执行步骤 |

### 服务陷阱
| 错误 | 正确做法 |
|------|---------|
| 直接 `mvn spring-boot:run` | 必须指定 workingDirectory 为项目根目录（见 test-env.yaml） |
| 用硬编码用户名导致冲突 | 必须用唯一用户名：`qa-{timestamp}-{random}` |
