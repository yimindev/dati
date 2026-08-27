---
name: dati-ops
description: Use when performing configuration or operations on the DatI platform via its HTTP API — managing data sources, MCP services, subjects, terms, users, or running admin operations.
---

# Dati Ops(仓库接入层)

本文件是仓库内的**接入入口**,技能的完整内容位于交付目录 `skills/dati-ops/`(单一事实来源)。

## REQUIRED: 使用前必读

开始任何任务前,必须完整读取 `skills/dati-ops/SKILL.md`,并根据任务需要读取其 `references/` 下文档与 `scripts/` 下工具。**禁止仅依据本文件内容执行任务。**

外层技能目录结构:

```
skills/dati-ops/
├── SKILL.md                    # 核心原则、认证前置、功能面、工作流、常见错误
├── openapi.json                # 内置 API 文档(技能自包含)
├── scripts/openapi.py          # OpenAPI 按需查询工具(仓库根调用: skills/dati-ops/scripts/openapi.py)
└── references/
    ├── datasource.md           # 数据源/表/列操作要点
    ├── subject.md              # 主题/术语操作要点
    ├── mcp-service.md          # MCP 服务生命周期
    ├── templates.md            # 模板语法(含系统变量、安全语义)
    ├── tools.md                # 工具类型/config/使用策略
    └── scenario-design.md      # 场景设计方法论
```
