# MCP 服务模块操作要点

> 接口定义以 `docs/api/openapi.json` 为准,本文件只补充 openapi 之外的业务知识。

## 状态机

```text
DRAFT ──publish──▶ PUBLISHED ──disable──▶ DISABLED
                     ▲                       │
                     └───────enable──────────┘
```

- **发布 ≠ 上线**:`DISABLED` 状态下 publish,状态保持 DISABLED(只出新快照版本)
- 每次 publish 生成一个**快照版本**(version_number 递增),返回 `IdResponse` 即快照 id

## 创建与发布流程

```text
1. POST /v1/mcp-services                    创建即带数据范围(必填!)
2. PUT  /v1/mcp-services/{id}/data-scope    后续可修改数据范围
3. POST /v1/mcp-services/{id}/publish       发布;body 可选 {release_note}
```

- 创建 body 的 `data_scopes` **必填**(minItems: 1),格式 `[{scope_type, reference_id}]`,`scope_type` 为 `DATA_SOURCE` / `SUBJECT`;`reference_id` 对应数据源/主题 id
- **数据范围为空时发布失败**:`MS_SERVICE_DATA_SCOPE_EMPTY`
- `GET data-scope` 可查当前范围;`PUT data-scope` body 为 `{items: [...]}` 全量替换

## 版本管理

- `GET /v1/mcp-services/{id}/diff`:**发布前**调用,对比草稿与当前线上版本的差异(工具/数据范围变更)
- `GET .../snapshots` → 快照列表(version_number、release_note)
- `POST .../rollback` → body `{target_version_number, release_note?}` 回滚到指定版本
- 回滚内部会做发布;回滚后如需上线,注意当前状态(参考状态机)

## 工具(Tools)

- 8 种工具类型(7 预置 + 1 自定义 PARAMETERIZED_SQL),用途、config 结构、使用策略见 [tools.md](tools.md)
- `POST .../tools/detect-annotations` → 从模板 SQL 检测操作属性,body `{template, parameters?}`,返回 `read_only` / `idempotent` / `destructive` / `detected_operation`(发布前建议检测)
- `POST .../tools/{toolId}/test` → 测试工具执行,返回 `ToolTestResponse`;测试是发布前的必要验证手段

## Prompt

- `GET / POST .../prompts` → 列表/创建(`McpPromptRequest`);`PUT / DELETE .../prompts/{promptId}`
- prompt 内容使用与 SQL 模板**同一套模板语法**,参数见 `PromptParameter`(name/description/required),语法参考 [templates.md](templates.md)

## 模板预览

- `POST /v1/template/preview` → 渲染模板(用于工具 prompt 等场景的预览)
- `POST /v1/template/extract` → 从模板文本提取变量列表
