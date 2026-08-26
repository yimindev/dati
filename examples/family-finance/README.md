# 典型实战案例：家庭共享记账助手 (Family Finance)

本案例演示如何基于 DatI 快速构建一个**多成员协作、全员透明共享、记账责任到人**的家庭记账 MCP 服务。

---

## 业务诉求与架构设计

在家庭或小型团队协作记账场景中，通常有两类核心诉求：
* **查账（读 - 全员透明共享）**：任何成员都可查询全家总支出、分类汇总、月度趋势以及各成员的花销对比；
* **记账（写 - 责任自动归属）**：记账、改账、删账通过系统参数 `{{_user.name}}` 进行物理级强绑定，无需用户显式提供用户名，严格杜绝越权篡改他人账单；
* **零额外调用感知（Context 注入）**：`execute_sql` 工具描述中自动注入当前登录用户信息（`Context: current user name: xxx, user id: yyy`），大模型单轮即可自主判断是否追加 `WHERE user_name = 'xxx'`；
* **新用户开箱自愈**：利用轻量幂等工具 `init_default_accounts`，新用户首次记账遇到空账户时无需人工干预，大模型自动完成账户初始化并完成记账。

---

## 一、 数据库准备

### 1. 数据模型与实体关系

```
+----------------+        +--------------------------+        +----------------+
|    account     |        |       transaction        |        |    category    |
| (资金账户表)    |        |       (收支流水表)       |        | (收支分类表)    |
+----------------+        +--------------------------+        +----------------+
| id (PK)        |<-------| account_id (FK)          |------->| id (PK)        |
| user_name (UK) |        | user_name                |        | name (UK)      |
| name (UK)      |<-------| transfer_to_account_id   |        | type           |
| type           |        | category_id (FK)         |        +----------------+
+----------------+        | type (EXPENSE/INCOME/..) |
                          | amount (> 0)             |
                          +--------------------------+
```

### 2. 初始化数据库

可以直接执行本目录下的 [`schema.sql`](schema.sql) 脚本完成 PostgreSQL 建表与基础分类初始化：

```bash
psql -h localhost -p 5432 -U postgres -d family_finance -f schema.sql
```

---

## 二、 DatI 平台配置步骤

### 步骤 1：接入数据源与元数据标注

1. 登录 DatI 管理端，进入「**数据源管理**」→ 点击「**新建数据源**」；
2. 填写数据库连接信息（如 `jdbc:postgresql://localhost:5432/family_finance`），连接测试通过后保存；
3. 进入数据源详情页，添加数据表：`account`、`category`、`transaction` 并同步列；
4. 增强元数据语义标注：
   * `transaction.user_name`：描述设为 `记账人用户名/家庭成员标识`，别名添加 `["记账人", "用户", "成员", "谁记的"]`；
   * `account.name`：描述设为 `账户名称`，别名添加 `["账户名", "名称", "钱包"]`。

---

### 步骤 2：创建 MCP 服务

1. 进入「**MCP 服务**」→ 点击「**新建服务**」；
2. 填写服务信息：
   * **服务名称**：`家庭记账`
   * **服务代码**：`family-finance`
   * **服务描述**：`家庭记账 MCP 服务：记账、查账、账单分析（流水/分类/账户）`
3. **数据范围 (Data Scope)**：绑定刚才创建的 `family-finance` 数据源。

---

### 步骤 3：配置自定义业务工具 (Custom Tools)

在 MCP 服务详情页的「**自定义工具**」中添加以下 4 个参数化 SQL 工具：

#### 1. `add_transaction`（记账工具）
* **Title**：`记账`
* **描述**：
  > `记一笔家庭账（支出/收入/转账）。自动归属当前操作用户；amount 必须为正数；EXPENSE/INCOME 必须填 category；TRANSFER 必须填 transfer_to_account 且不填 category；账户与分类填名称即可，自动解析为 ID。注意：若调用后受影响行数为 0（affected_rows: 0），说明该账户可能尚未初始化，请先调用 init_default_accounts 工具初始化常用账户，然后重新调用本工具完成记账。`
* **SQL 模板**：
  ```sql
  INSERT INTO transaction (user_name, account_id, transfer_to_account_id, category_id, type, amount, transaction_date, merchant, note)
  SELECT {{_user.name}}, a.id, ta.id, c.id, {{type}}, {{amount}}, {{transaction_date}}, {{merchant}}, {{note}}
  FROM account a
  LEFT JOIN account ta ON ta.name = {{transfer_to_account}} AND ta.user_name = {{_user.name}}
  LEFT JOIN category c ON c.name = {{category}}
  WHERE a.name = {{account}} AND a.user_name = {{_user.name}}
  ```
* **参数列表**：
  | 参数名 | 类型 | 必填 | 说明 |
  | :--- | :--- | :--- | :--- |
  | `transaction_date` | DateTime | 是 | 交易日期，格式 YYYY-MM-DD |
  | `type` | String | 是 | 收支类型：EXPENSE=支出, INCOME=收入, TRANSFER=转账 |
  | `amount` | Number | 是 | 金额，必须为正数 |
  | `account` | String | 是 | 账户名称（现金/招商银行卡/支付宝/微信） |
  | `category` | String | 否 | 分类名称，EXPENSE/INCOME 必填，TRANSFER 不填 |
  | `transfer_to_account` | String | 否 | 转入账户名称，仅 TRANSFER 必填 |
  | `merchant` | String | 否 | 商户或消费对象 |
  | `note` | String | 否 | 备注说明 |

---

#### 2. `init_default_accounts`（常用账户初始化工具 ★ 幂等自愈）
* **Title**：`初始化常用账户`
* **描述**：`为当前登录用户初始化 4 个标准常用资金账户（微信、支付宝、现金、银行卡）。若已存在则自动忽略（幂等安全）。`
* **参数列表**：`[]`（无需入参）
* **SQL 模板**：
  ```sql
  INSERT INTO account (user_name, name, type, description)
  VALUES 
    ({{_user.name}}, '微信', 'WECHAT', '微信零钱/零钱通'),
    ({{_user.name}}, '支付宝', 'ALIPAY', '支付宝余额/余额宝'),
    ({{_user.name}}, '现金', 'CASH', '日常现金钱包'),
    ({{_user.name}}, '银行卡', 'BANK_CARD', '常用银行储蓄卡')
  ON CONFLICT (user_name, name) DO NOTHING
  ```

---

#### 3. `update_transaction`（改账工具）
* **Title**：`改账`
* **描述**：`修改一笔当前用户自己的已有流水，仅更新传入的字段。transaction_id 必填，其余字段按需传入。`
* **SQL 模板**：
  ```sql
  UPDATE transaction SET
  {{#if amount}}amount = {{amount}},{{/if}}
  {{#if type}}type = {{type}},{{/if}}
  {{#if transaction_date}}transaction_date = {{transaction_date}},{{/if}}
  {{#if account}}account_id = (SELECT id FROM account WHERE name = {{account}} AND user_name = {{_user.name}}),{{/if}}
  {{#if category}}category_id = (SELECT id FROM category WHERE name = {{category}}),{{/if}}
  {{#if merchant}}merchant = {{merchant}},{{/if}}
  {{#if note}}note = {{note}},{{/if}}
  updated_at = now()
  WHERE id = {{transaction_id}} AND user_name = {{_user.name}}
  ```
* **参数列表**：`transaction_id` (Number, 必填)，其余 `amount`、`type`、`transaction_date`、`account`、`category`、`merchant`、`note` 均为选填。

---

#### 4. `delete_transaction`（删账工具）
* **Title**：`删账`
* **描述**：`删除一笔当前用户自己的流水（按交易 ID，仅删除单条）。`
* **SQL 模板**：
  ```sql
  DELETE FROM transaction WHERE id = {{transaction_id}} AND user_name = {{_user.name}}
  ```
* **参数列表**：`transaction_id` (Number, 必填)。

---

### 步骤 4：预置通用查询工具说明 (`execute_sql`)

DatI 会在对外暴露 MCP `tools/list` 时，自动在 `execute_sql` 工具的描述末尾追加当前认证用户的上下文：

```text
Execute an SQL query or statement against a data source. Allowed operations: SELECT, INSERT. Max rows: 1000. Context: current user name: zhangsan, user id: c90c07a5-...
```

大模型收到工具列表后：
* 面对“**我**花了多少钱”、“**我的**账单”时：结合 `Context: current user name: zhangsan`，自动在 SQL 中追加 `WHERE user_name = 'zhangsan'`；
* 面对“**全家**总共花了多少钱”、“**各成员**花销对比”时：直接编写全局聚合 SQL（如 `GROUP BY user_name`）。

---

### 步骤 5：发布 MCP 服务

点击管理端右上角「**发布**」按钮生成快照版本。发布后，MCP 服务 Endpoint（如 `http://localhost:8085/family-finance/mcp`）即可接入各类 Agent 客户端（Claude Desktop、Cursor、Dify、Coze 等）。

---

## 三、 对话交互效果演练

### 场景 1：老用户日常记账（1 轮直出）
> **用户**：“帮我记一笔今天午饭 35 元，微信付的”
>
> **大模型**：调用 `add_transaction(account="微信", category="餐饮", amount=35, transaction_date="2026-08-26")`
>
> **服务端**：自动注入当前登录用户名（如 `admin`），返回 `affected_rows: 1`。
>
> **大模型回答**：“已为您成功记录一笔 35.00 元的餐饮支出（微信支付）！”

---

### 场景 2：新用户首次记账（开箱自愈）
> **新用户（未初始化账户）**：“记一笔买菜 50 支付宝”
>
> **大模型执行过程**：
> 1. 调用 `add_transaction` → 返回 `affected_rows: 0`；
> 2. 根据工具描述自动调用 `init_default_accounts` → 成功为该用户创建 4 个常用账户（`affected_rows: 4`）；
> 3. 重新调用 `add_transaction` → 成功落库（`affected_rows: 1`）。
>
> **大模型回答**：“已为您自动初始化常用账户（微信、支付宝、现金、银行卡），并成功记录买菜支出 50.00 元！”

---

### 场景 3：全家汇总查询 vs 个人专属查询
> **用户**：“我们家这个月一共花了多少钱？各成员分别花了多少？”
>
> **大模型生成 SQL**：
> ```sql
> SELECT user_name, SUM(amount) AS total 
> FROM transaction 
> WHERE type = 'EXPENSE' AND to_char(transaction_date, 'YYYY-MM') = '2026-08'
> GROUP BY user_name;
> ```

> **用户**：“那我个人在餐饮上花了多少？”
>
> **大模型生成 SQL**（感知当前登录用户为 `zhangsan`）：
> ```sql
> SELECT SUM(t.amount) AS my_food_expense 
> FROM transaction t 
> JOIN category c ON c.id = t.category_id 
> WHERE t.user_name = 'zhangsan' 
>   AND c.name = '餐饮' 
>   AND to_char(t.transaction_date, 'YYYY-MM') = '2026-08';
> ```

---

### 场景 4：跨用户越权防篡改保护
> 若用户 A 尝试要求：“修改账单 ID 10 的金额为 9999 元”（该流水由用户 B 创建）：
> * 大模型调用 `update_transaction(transaction_id=10, amount=9999)`；
> * 服务端执行 `UPDATE ... WHERE id = 10 AND user_name = 'user_A'`；
> * 返回 `affected_rows: 0`，数据库物理层面保护数据不被非法修改。
