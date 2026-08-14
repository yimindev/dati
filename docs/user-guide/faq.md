# 常见问题

## 数据源连接

### Q: 连接测试失败怎么办？

1. 检查 JDBC 连接字符串是否正确
2. 确认数据库网络可达（如数据库在本地需使用 `host.docker.internal` 代替 `localhost`）
3. 验证用户名和密码是否正确
4. 确认数据库已开启远程访问权限

### Q: 支持哪些数据库？

支持 MySQL、PostgreSQL、ClickHouse、Oracle 等 10 种主流数据库，更多数据库持续添加中。

## MCP 服务

### Q: 发布后可以修改吗？

已发布服务的修改会保存为草稿，线上继续运行旧版本。修改完成后点击「发布变更」生成新快照，新版本即时生效；也可以随时停用、启用或回滚到历史版本。

### Q: MCP 端点地址是什么格式？

MCP 端点是一个 JSON-RPC over HTTP 接口（Streamable HTTP，MCP 协议 2025-11-25）：

```
POST http://your-domain/{服务标识}/mcp
```

例如服务标识为 `user-analysis`，则端点地址为：

```
POST http://your-domain/user-analysis/mcp
```

请求需携带 `MCP-Protocol-Version: 2025-11-25` 请求头，并先通过应用层认证（JWT 或 API Key）。完整的端点地址可在服务详情页复制。

### Q: Tool 和 Prompt 的区别是什么？

| | Tool | Prompt |
|------|------|------|
| **用途** | 供 LLM 调用的数据查询接口 | 供 LLM 获取的上下文模板 |
| **输出** | SQL 执行结果 | 渲染后的文本 |
| **协议** | `tools/list`、`tools/call` | `prompts/list`、`prompts/get` |

## 模板语法

### Q: 模板渲染结果不对怎么办？

1. 使用 Tool 编辑器中的「测试渲染」按钮预览结果
2. 检查参数名称是否与模板中一致
3. 确认参数类型与 SQL 中的预期类型匹配
4. 参考 [模板语法](/template-syntax) 了解各语法元素的行为

### Q: 如何调试复杂的条件模板？

建议将复杂模板拆分为多个简单的条件块，分别测试每个条件分支的正确性。
