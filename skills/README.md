# skills/

DatI 仓库技能的唯一事实来源。技能以 [Agent Skills 开放标准](https://agentskills.io)格式存放,任何支持该标准的 agent 均可使用。

## 接入方式

仓库通过 symlink 将技能接入 `.agents/skills/`(pi / Codex / Gemini 的项目级技能位置),克隆即用,保持单一事实来源:

```
skills/dati-ops/          # 技能源码(唯一事实来源)
.agents/skills/dati-ops   # symlink → ../../skills/dati-ops
```

> 注:symlink 在 macOS / Linux 下开箱即用;Windows 下 git 会将其 checkout 成普通文本文件(静默失效),如何处理待定。

## 约定

- 每个技能一个目录 `dati-<purpose>`(小写 + 连字符)
- 结构:`SKILL.md`(frontmatter + 原则)+ 可选 `references/`(重参考,按需加载)+ `scripts/`(辅助脚本)+ `examples/`(完整流程)

## 技能列表

| 技能 | 说明 |
|------|------|
| `dati-ops` | 通过 HTTP API 对 DatI 平台进行配置与操作(数据源/主题术语/MCP 服务;不含注册登录/系统管理)。已通过真实环境场景验证 |
