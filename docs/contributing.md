# 贡献指南（Contributing）

感谢你对 Data Conn AI（Dati）的关注与贡献！本指南帮助你高效协作。

## 提交流程（建议）
1. 先开 Issue，描述问题或需求，并与维护者达成一致范围
2. 从 `main`（或默认分支）创建功能分支：`feature/...`、`fix/...`、`docs/...`
3. 提交代码并确保本地通过构建/测试
4. 发起 Pull Request，关联 Issue，清晰描述改动与影响面

## 代码规范
- 后端：遵循 Spring/Java 社区惯例，使用 Lombok 减少样板；异常统一走 `DciException`
- 前端：组件 PascalCase，类型完备；尽量保持无副作用的函数式写法
- 提交信息：可参考 Conventional Commits（`feat:`/`fix:`/`docs:`/`refactor:`/`test:`/`chore:`）

## 测试与构建
- 后端单元测试：`cd backend && mvn test`
- 前端类型检查与构建：`cd frontend && npm run build`

## 文档
- 在 `docs/` 下维护模块文档、API、架构与开发指南
- 新增重大架构/设计决策，请追加 ADR（`docs/adr/`）

## 行为准则
- 尊重与合作，拒绝人身攻击与歧视
- 评审以问题为中心，谨慎使用强绝对语气

## License
- 若引入第三方代码或资源，请确认 License 兼容并在文档中注明
