# 场景设计方法论

> 目标:设计一个「LLM 开箱即用、行为可控」的 DatI 场景(MCP 服务)。

## 设计流程

```text
1. 数据建模 → 2. 元数据标注 → 3. 工具设计 → 4. 发布与验证
```

## 1. 数据建模

- 表结构贴合业务语义,字段命名直观(LLM 没有业务文档,表名/字段名就是第一手信息)
- 需要"归属/责任"语义的数据,必须设计归属字段(如 `user_name`),这是防越权的基础
- 关键约束落到数据库层(`CHECK`/唯一约束),不能只写在工具描述里:如 `hours CHECK (>0 AND <=24)`、唯一约束支撑幂等覆盖

## 2. 元数据标注(决定 LLM 语义命中率)

**描述(description)+ 别名(aliases)是 LLM 理解业务的入口**,标注质量直接影响查询准确率:

| 做法 | 示例 |
|---|---|
| 字段描述写业务含义 | `user_name` → "记账人用户名/家庭成员标识" |
| 别名覆盖口语表达 | `account.name` → 别名 `["账户名", "名称", "钱包"]` |
| 枚举字段写取值含义 | `type` → "EXPENSE=支出, INCOME=收入, TRANSFER=转账" |

判断标准:用户口语里的每个词,都能在描述/别名里找到映射。别名为数组,通过 `PUT .../columns/{id}` 保存。

## 3. 工具设计(核心模式)

### 模式 A:读/写分工
- **读**:优先预置 `execute_sql`(自带 Context 注入,见 tools.md);无需为读场景建自定义工具
- **写**:必须自定义参数化工具,承载业务校验、归属绑定、幂等

### 模式 B:系统变量责任绑定(防越权)
```sql
UPDATE transaction SET amount = {{amount}}
WHERE id = {{transaction_id}} AND user_name = {{_user.name}}
```
用户 A 操作 B 的数据 → 匹配 0 行 → 物理层拒绝。**越权统一表现为 `affected_rows: 0` 或空结果,不是报错**——LLM 收到后按工具描述引导用户,不暴露他人数据。

### 模式 C:幂等(安全重试)
```sql
INSERT INTO account (user_name, name, type) VALUES ({{_user.name}}, '微信', 'WECHAT')
ON CONFLICT (user_name, name) DO NOTHING        -- 初始化类:重复调用无害
```
- 初始化类用 `DO NOTHING`;登记/更新类用 `DO UPDATE SET ... ` 覆盖,配合 `COALESCE(EXCLUDED.note, work_log.note)` 保留未传字段

### 模式 D:自愈(描述驱动流程)
工具描述写明失败恢复路径,LLM 自动完成多步流程:
> "若 affected_rows: 0,说明账户未初始化,请先调用 init_default_accounts 再重试"
→ 边界状态(新用户/空数据/名称写错)无需人工干预。

### 模式 E:上下文注入(零额外感知)
`execute_sql` 描述自动带 `Context: current user name: xxx`,LLM 单轮即可决定"我的数据"(加 WHERE)还是"全体的"(GROUP BY)。

### 模式 F:硬隔离(读写隔离是硬约束时)
当"只能看/改自己的数据"是**不可妥协的业务约束**时,软引导(模式 E)不够,需要物理级隔离:

1. **关闭预置 `execute_sql`**(`enabled=false`):堵住"绕过自定义工具直查全表"的旁路
2. **读也走自定义工具**:查询模板恒带 `WHERE user_name = {{_user.name}}`,不存在"忘了加过滤"的可能
3. **角色门禁(SQL 内嵌 RBAC)**:权限校验写进模板,如汇总工具恒带
   `WHERE EXISTS (SELECT 1 FROM team_member WHERE user_name = {{_user.name}} AND role = 'LEADER')` —— 普通成员拿到空结果,与越权 0 行同一语义
4. **字典值提取作为信息通道**:关闭 execute_sql 后,LLM 认识"有哪些项目/枚举值"靠 `POST .../values/extract` 提取字典值 + `search_metadata` 检索,而不是直接查表

## 4. 发布与验证

1. 发布前:`GET .../diff` 确认变更;对每个自定义工具 `POST .../tools/{toolId}/test` 实测(传参、看渲染 SQL 与结果);`POST .../tools/detect-annotations` 核对 read_only/idempotent/destructive 与设计一致
2. 发布后:设计**对话演练场景**验证 LLM 行为,至少覆盖:
   - 场景 1:老用户日常操作 → 单轮直出(验证描述清晰)
   - 场景 2:边界状态(新用户/空数据/名称写错) → 触发自愈(验证恢复指引)
   - 场景 3:多语义查询(个人 vs 全体) → 命中正确 SQL(验证元数据标注)
   - 场景 4:越权操作 → 物理拒绝(验证归属绑定/角色门禁)

## 常见失误

| 失误 | 后果 |
|---|---|
| 描述/别名缺失 | LLM 猜字段,SQL 写错或查错表 |
| 写操作直接用 execute_sql | 无业务校验、无归属绑定,越权风险 |
| 模板用 `{{{var}}}` 拼接用户输入 | SQL 注入风险 |
| 工具描述含糊("记一笔账") | LLM 不知道参数约束、失败后不会恢复 |
| 非幂等初始化 | LLM 重试导致重复数据 |
| 硬隔离场景不关 execute_sql | 存在直查全表的旁路,隔离形同虚设 |
