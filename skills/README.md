# skills/

DatI 仓库技能的唯一事实来源。技能以 [Agent Skills 开放标准](https://agentskills.io)格式存放,任何支持该标准的 agent 均可使用。

## 接入方式

仓库通过**薄壳技能**将交付技能接入 `.agents/skills/`(pi / Codex / Gemini 的项目级技能位置):

```
skills/dati-ops/            # 技能源码(单一事实来源,可独立交付/打包)
.agents/skills/dati-ops/    # 薄壳:SKILL.md 简介 + REQUIRED 指向外层,供仓库内 agent 发现
```

> 薄壳 SKILL.md 的 `description` 与真技能保持一致(保证触发);内容必须引导 agent 读取外层完整文档,禁止自行承载可执行内容。

> 不用 symlink 的原因:Windows 下 git checkout 符号链接依赖开发者模式/`core.symlinks`,环境不一致;薄壳是普通文件,跨平台无差异。

## 约定

- 每个技能一个目录 `dati-<purpose>`(小写 + 连字符)
- 结构:`SKILL.md`(frontmatter + 原则)+ 可选 `references/`(重参考,按需加载)+ `scripts/`(辅助脚本)+ `examples/`(完整流程)

## 技能列表

| 技能 | 说明 |
|------|------|
| `dati-ops` | 通过 HTTP API 对 DatI 平台进行配置与操作(数据源/主题术语/MCP 服务;不含注册登录/系统管理)。已通过真实环境场景验证 |
