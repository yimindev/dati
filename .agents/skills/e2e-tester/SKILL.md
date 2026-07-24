---
name: e2e-tester
description: Use when you need to run E2E HTTP integration tests against the DatI backend, verify bug fixes, or validate API behavior using test cases defined in e2e-tests/test-cases/
---

# E2E Tester

## 核心原则

**站在用户使用角度审视一切。** 用例只是操作指引，不代表完整验收标准。执行每个步骤时主动审视返回内容的合理性：

- 检查所有返回字段是否完整、合理、自洽
- 时间戳是否合理？ID 格式是否规范？关联数据是否一致？
- **发现任何不合理之处，即使用例没写，也要在报告中标出**（WARN 级别）

## 执行模式

### 模式 A：当前进程（单模块快速验证）

Agent 逐步执行用例，负责服务的完整生命周期：启动 → 执行 → 关闭。

### 模式 B：子进程（批量 / CI）

调度器通过封装脚本并行运行多个模块：

```bash
# 全部模块
scripts/run-tests.sh

# 指定模块
scripts/run-tests.sh datasource subject term
```

子进程关键：`pi -p` 后台运行时必须 `< /dev/null` 重定向 stdin，否则卡住。脚本已封装此细节，直接调用即可。

## 文件结构

```
e2e-tests/
  test-env.yaml              # 环境配置（base_url、启动命令）
  test-data.yaml             # 公共测试数据（用户、连接信息）
  test-cases/<module>.md     # 测试用例
  BUGS.yaml / BUGS-FIXED.yaml
```

## Execution Flow

### 1. 加载配置

始终加载 `e2e-tests/test-env.yaml` 和 `docs/api/openapi.json`。

### 2. 认证

用例前置包含「已登录」时：
- 使用 test-data.yaml 中的固定用户 `qa_user` 直接登录
- 若用户不存在则注册，若注册返回「already exists」则直接登录，均不视为失败

### 3. 执行步骤

对每个步骤：理解意图 → 匹配 API（openapi.json）→ 构造 curl 请求 → 语义断言。

构造请求规则：
- 字段名以 openapi.json schema 为准
- body 使用 snake_case
- 分离响应体与状态码：`curl -w "\n__HTTP__%{http_code}"`

### 4. 生成报告

```markdown
## E2E Test Report

PASS  TC-DS-001  创建、查询、删除数据源
  ✓ 创建成功 → 200
  ⚠ password 字段在列表响应中明文暴露

FAIL  TC-DS-008  删除不存在的数据源
  ✗ 预期返回 404，实际返回 200

---
Total: 2 executed, 1 passed, 1 failed
Warnings: 1
```

### 5. Bug 跟踪

**禁止手改 BUGS.yaml / BUGS-FIXED.yaml。** 所有 Bug 生命周期操作必须通过 `scripts/bug-tracker.sh` 执行，确保 ID 格式和字段结构一致。

**Bug 修复后必须 E2E 验证通过才能标记 FIXED

测试完成后交叉比对 `BUGS.yaml`：

```
发现的问题 → 查 BUGS.yaml
  ├─ 匹配到已有   → 更新 found_at
  ├─ 匹配到 FIXED  → REGRESSION！移回 BUGS.yaml
  └─ 未匹配       → 用 bug-tracker.sh add 新增
```

之前 open 的问题本次未复现 → 用 `bug-tracker.sh fix` 移入 FIXED。
之前 fixed 的问题本次复现 → 用 `bug-tracker.sh regress` 移回 BUGS.yaml。

## 脚本工具

| 脚本 | 用途 |
|------|------|
| `scripts/service.sh` | 服务生命周期 |
| `scripts/run-tests.sh` | 一键测试调度器 |
| `scripts/bug-tracker.sh` | Bug 生命周期管理 |

所有脚本支持 `--help` 查看详细用法。Agent 直接调用：

```bash
scripts/run-tests.sh                        # 全模块
scripts/run-tests.sh datasource             # 单模块
scripts/bug-tracker.sh fix BUG-20260724-001
```

## 常见陷阱

### 字段命名
| 错误 | 正确 |
|------|------|
| 登录传 `username` | required 字段是 `name`、`password`、`type` |
| DataSource 用 `host`+`port` | 用 **`jdbc_url`**（完整 JDBC URL） |
| `type` 用小写 | 大写枚举：`POSTGRESQL`、`MYSQL`、`H2` |
| 忘传 `type: "local"` | Login 的 `type` 是 required |

### 认证
- `/v1/**` 大多需要 token（除 `/v1/auth/*`）
- `test-connection` **也需要认证**

### 服务
- 启动命令必须指定 workingDirectory
- 子进程 pi -p 必须 `< /dev/null` 重定向 stdin
