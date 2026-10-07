# 典型实战案例：伴侣记录助手 (Couple Journal)

本案例演示如何基于 DatI 快速构建一个**记录伴侣生活足迹（美食、观影、旅行、日常）与吵架和解复盘**的 MCP 服务。

---

## 业务诉求与设计理念

在情侣与伴侣的生活记录场景中，通常有以下核心诉求：
* **美好足迹记录（高光时刻与日常）**：记录看电影、外出就餐、假期旅游、日常约会，兼顾消费金额与体验评分；
* **单日与多天事件自适应**：通过 `start_date` 与 `end_date`，既能记录单日看电影，也能记录持续数天的跨省旅游或度假；
* **情感治理与吵架复盘（特色场景）**：吵架不仅是情绪宣泄，更是加深理解的契机。记录矛盾起因、谁先主动破冰求和、激烈程度，以及最核心的**“双方和好达成的共识与约定承诺”**；
* **两表联动与故事闭环**：通过可选的 `moment_id` 外键，清晰记录“在去云南旅游的第3天吵架”，还原真实生活细节；
* **物理级防篡改与责任到人**：记事、复盘与删除操作通过系统变量 `{{_user.name}}` 强制绑定当前登录用户，严防越权篡改他人记录；
* **零额外调用感知（Context 注入）**：预置 `execute_sql` 工具描述中自动注入当前登录用户信息（`Context: current user name: xxx, user id: yyy`），大模型单轮即可自主判断个人记录与共同统计。

---

## 一、 数据库准备

### 1. 数据模型与实体关系

```
+-------------------------------------------------------------+
|                        couple_moment                        |
|                    (伴侣生活足迹与高光时刻表)                  |
+-------------------------------------------------------------+
| id                  BIGINT PK AUTO_INCREMENT                |
| category            VARCHAR(32)    -- DINING/MOVIE/TRAVEL.. |
| title               VARCHAR(128)   -- 电影名/餐厅/旅游目的地  |
| start_date          DATE           -- 开始日期              |
| end_date            DATE           -- 结束日期(单日=start)   |
| cost                DECIMAL(10,2)  -- 花费金额(默认0)        |
| rating              TINYINT        -- 评分/心情(1-5，默认3)  |
| content             TEXT           -- 感受评价与细节备注    |
| recorder            VARCHAR(64)    -- 记录人用户名          |
+-------------------------------------------------------------+
                              ^
                              | 0..1 (可选外键: 旅行/就餐中途发生吵架)
+-------------------------------------------------------------+
|                       conflict_record                       |
|                     (争吵与和解复盘表)                       |
+-------------------------------------------------------------+
| id                  BIGINT PK AUTO_INCREMENT                |
| moment_id           BIGINT NULL FK -- 关联足迹ID (可为空)   |
| incident_date       DATE           -- 争吵发生日期          |
| reason              TEXT           -- 起因与核心矛盾        |
| severity            TINYINT        -- 激烈程度(1-5，默认1)  |
| peacemaker          VARCHAR(64)    -- 谁先道歉/主动破冰     |
| agreement           TEXT           -- 和好约定与承诺        |
| recorder            VARCHAR(64)    -- 记录人用户名          |
+-------------------------------------------------------------+
```

### 2. 初始化数据库

可以直接执行本目录下的 SQL 脚本完成建库、建表与示例数据初始化：

```bash
mysql -h <host> -P 3306 -u root -p < schema-mysql.sql
```

---

## 二、 DatI 平台配置步骤

### 步骤 1：接入数据源与元数据标注

1. 登录 DatI 管理端，进入「**数据源管理**」→ 点击「**新建数据源**」；
2. 填写数据库连接（如 `jdbc:mysql://<host>:3306/couple_journal`），测试通过后保存；
3. 进入数据源详情页，批量添加数据表：`couple_moment`、`conflict_record` 并执行「**同步列**」；
4. **元数据与别名增强**：
   * `couple_moment`：表别名添加 `["生活足迹", "足迹", "约会", "回忆", "旅程", "账单", "日常记录", "生活记录"]`；
   * `conflict_record`：表别名添加 `["吵架记录", "矛盾", "复盘", "和好", "约定", "反思", "吵架复盘", "谁先道歉"]`；
   * 列别名标注（详见文档设计）：如 `cost` 添加 `["消费", "花了多少钱", "金额", "支出", "花销"]`；`peacemaker` 添加 `["谁先低头", "谁先道歉", "谁哄的", "谁破冰", "认错"]`。
5. **值匹配与字典抽取**：
   * 对 `category` 列配置标准枚举与丰富同义词：
     * `DINING` ➜ `["餐饮", "美食", "吃饭", "聚餐", "下馆子", "探店", "火锅", "西餐", "晚饭", "午饭", "吃大餐"]`
     * `MOVIE` ➜ `["看电影", "影院", "观影", "电影", "追剧", "片子", "院线", "大片"]`
     * `TRAVEL` ➜ `["旅游", "旅行", "出游", "自驾", "度假", "长途", "游玩", "去外地"]`
     * `DAILY` ➜ `["日常", "约会", "逛街", "散步", "小确幸", "送礼", "琐事", "周末"]`
   * 对 `title` 列开启值提取，提升大模型对餐厅名、景点名的实体识别率。

---

### 步骤 2：创建 MCP 服务

1. 进入「**MCP 服务**」→ 点击「**新建服务**」；
2. 填写服务信息：
   * **服务代码**：`couple-journal`
   * **服务名称**：`伴侣记录`
   * **服务描述**：`伴侣生活足迹与关系记录 MCP 服务：记录与查询吃喝玩乐、观影、旅行出游、日常约会，以及矛盾吵架复盘与和解约定。`
3. **数据范围 (Data Scope)**：绑定 `couple-journal` 数据源。

---

### 步骤 3：配置自定义业务工具 (Custom Tools)

在 MCP 服务详情页配置以下 4 个自定义参数化工具：

#### 1. `add_moment`（记录生活足迹）
* **Title**：`记录生活足迹`
* **描述**：
  > `记录伴侣一起做的一件事（餐饮美食、看电影、出游旅行、日常约会等）。自动绑定当前操作人为记录人；单日事件可不填 end_date（自动同 start_date）；未指定 rating 默认为 3（中立）；未指定 cost 默认为 0；TRAVEL 类型多天旅游建议填写 start_date 与 end_date。`
* **SQL 模板**：
  ```sql
  INSERT INTO couple_moment (category, title, start_date, end_date, cost, rating, content, recorder)
  VALUES ({{category}}, {{title}}, {{start_date}}, COALESCE({{end_date}}, {{start_date}}), COALESCE({{cost}}, 0.00), COALESCE({{rating}}, 3), {{content}}, {{_user.name}})
  ```

#### 2. `add_conflict`（记录吵架复盘）
* **Title**：`记录吵架复盘`
* **描述**：
  > `记录一次伴侣争吵与和解复盘。必须包含矛盾起因、谁先破冰道歉认错、以及和好后双方达成的约定与承诺；若发生在某次特定旅行或约会中，可传入 moment_id 关联；severity 默认为 1（轻度拌嘴）；自动归属当前操作人。`
* **SQL 模板**：
  ```sql
  INSERT INTO conflict_record (moment_id, incident_date, reason, severity, peacemaker, agreement, recorder)
  VALUES ({{moment_id}}, {{incident_date}}, {{reason}}, COALESCE({{severity}}, 1), {{peacemaker}}, {{agreement}}, {{_user.name}})
  ```

#### 3. `update_moment`（修改足迹记录）
* **Title**：`修改足迹记录`
* **描述**：
  > `修改已有生活足迹的金额、评分、感受备注或标题。仅能修改当前用户自己录入的记录。传入待修改的 id 以及需要更新的字段即可。若 affected_rows 为 0 说明记录不存在或非本人创建。`
* **SQL 模板**：
  ```sql
  UPDATE couple_moment SET
  {{#if title}}title = {{title}},{{/if}}
  {{#if cost}}cost = {{cost}},{{/if}}
  {{#if rating}}rating = {{rating}},{{/if}}
  {{#if content}}content = {{content}},{{/if}}
  id = id
  WHERE id = {{id}} AND recorder = {{_user.name}}
  ```

#### 4. `update_conflict`（修改吵架复盘）
* **Title**：`修改吵架复盘`
* **描述**：
  > `修改已有吵架记录的和解约定承诺、起因或破冰人。仅能修改当前用户自己录入的记录。传入待修改的 id 以及需要更新的字段即可。若 affected_rows 为 0 说明记录不存在或非本人创建。`
* **SQL 模板**：
  ```sql
  UPDATE conflict_record SET
  {{#if reason}}reason = {{reason}},{{/if}}
  {{#if peacemaker}}peacemaker = {{peacemaker}},{{/if}}
  {{#if agreement}}agreement = {{agreement}},{{/if}}
  {{#if severity}}severity = {{severity}},{{/if}}
  id = id
  WHERE id = {{id}} AND recorder = {{_user.name}}
  ```

#### 5. `delete_moment` / `delete_conflict`（删记录 —— 物理级防越权）
* **SQL 模板**：
  ```sql
  DELETE FROM couple_moment WHERE id = {{id}} AND recorder = {{_user.name}}
  -- 或
  DELETE FROM conflict_record WHERE id = {{id}} AND recorder = {{_user.name}}
  ```

---

### 步骤 4：发布 MCP 服务

点击右上角「**发布**」，发布后 Endpoint 为 `http://<host>:<port>/couple-journal/mcp`，可接入 Claude Desktop、Cursor、Dify、Coze 等各类 Agent。

---

## 三、 对话交互效果演练

### 场景 1：日常自然语言记事（1 轮直出）
> **用户**：“今天跟宝宝去万达看了《阿凡达3》，花了140，视效超震撼给了5分好评”
>
> **大模型**：调用 `add_moment(category="MOVIE", title="看电影《阿凡达3》", start_date="2026-10-02", cost=140, rating=5, content="在万达看的，视效超震撼")`
>
> **服务端**：自动绑定当前记录人 `admin`，落库成功。
>
> **大模型回答**：“已为您成功记录观影足迹！《阿凡达3》已收入你们的电影库（评分：5星，花费：140元）。”

---

### 场景 2：多天长途旅游记录
> **用户**：“我们国庆10月1号到5号去云南大理自驾游了5天，总共花了6500块，玩得超开心”
>
> **大模型**：调用 `add_moment(category="TRAVEL", title="云南大理丽江5日游", start_date="2026-10-01", end_date="2026-10-05", cost=6500, rating=5, content="环洱海自驾，玩得超开心")`
>
> **大模型回答**：“已记录你们的云南大理丽江5日自驾游！旅途愉快，总花销 6500.00 元已计入旅游账本。”

---

### 场景 3：吵架复盘与约定立规矩
> **用户**：“今天因为周末谁洗碗拌嘴了，我先主动道歉认错，和好后我们约定下周买洗碗机，平时轮流洗不过夜”
>
> **大模型**：调用 `add_conflict(incident_date="2026-10-02", reason="周末洗碗家务推脱分歧", peacemaker="admin", agreement="下周买洗碗机，平时轮流洗绝不过夜", severity=1)`
>
> **大模型回答**：“已为您完成吵架复盘记录。主动道歉是爱意与包容的体现！达成的约定『下周买洗碗机，平时轮流洗绝不过夜』已归档，愿你们生活更甜蜜！”

---

### 场景 4：查回忆与查账（NL2SQL 通用查询）
> **用户**：“我们今年一共吃过多少次大餐？总共花了多少钱？评分最高的是哪家？”
>
> **大模型生成 SQL**：
> ```sql
> SELECT COUNT(*) AS dining_count, SUM(cost) AS total_cost, MAX(cost) AS max_cost
> FROM couple_moment 
> WHERE category = 'DINING' AND start_date >= '2026-01-01';
> 
> SELECT title, cost, rating, content 
> FROM couple_moment 
> WHERE category = 'DINING' AND start_date >= '2026-01-01'
> ORDER BY rating DESC, cost DESC LIMIT 1;
> ```
>
> **大模型回答**：“今年你们一共外出就餐 2 次，总消费 708.00 元。其中评分最高的是**三里屯牛排家**（5星好评，花费 520 元，纪念日大餐）！”

---

### 场景 5：情感分析与历史约定检索
> **用户**：“我们以前吵架都达成过哪些约定？平时一般是谁先道歉？”
>
> **大模型生成 SQL**：
> ```sql
> -- 1. 查询所有历史约定
> SELECT incident_date, reason, agreement FROM conflict_record ORDER BY incident_date DESC;
> -- 2. 统计道歉次数
> SELECT peacemaker, COUNT(*) AS peace_count FROM conflict_record GROUP BY peacemaker;
> ```
>
> **大模型回答**：“你们目前达成了以下宝贵约定：
> 1. 『约定以后副驾只报路名，不催促也不抱怨，心平气和』（云南自驾走错路时达成）；
> 2. 『下周买洗碗机；平时轮流洗，绝不过夜』（洗碗分歧时达成）。
> 
> 在道歉与破冰方面，目前记录中主要是您主动破冰化解矛盾，非常棒的态度！”

---

### 场景 6：修改记录与补充和好约定（改账与更新）
> **用户**：“把上次云南旅游的那次吵架约定补充一下：约定以后副驾只报路名，如果不小心开错路就当探索新风景，绝不抱怨”
>
> **大模型**：先查询该条记录 ID（或直接已知 ID 1），调用 `update_conflict(id=1, agreement="约定以后副驾只报路名，如果不小心开错路就当探索新风景，心平气和绝不抱怨")`
>
> **服务端**：核验 `recorder = admin` 匹配，返回 `affected_rows: 1`。
>
> **大模型回答**：“已为您成功更新和解约定！新约定『如果不小心开错路就当探索新风景，心平气和绝不抱怨』已生效，心态超棒！”
