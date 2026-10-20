# 模板预览 - 端到端测试

## TC-TPL-001 从 SQL 提取模板信息
**级别：** P1
**前置：** 已登录

1. 调用模板提取接口，传入一段示例 SQL 文本（如 `chinook.e2e.template.extract_sql`）
2. 验证返回结果：
   - 状态码 200
   - 响应体非空，包含提取出的模板结构信息
   - 提取的信息与输入 SQL 相关（不是完全无关的内容）

---

## TC-TPL-002 模板预览
**级别：** P1
**前置：** 已登录

1. 调用模板预览接口，传入一个有效的模板配置
2. 验证返回结果：
   - 状态码 200
   - 响应体包含预览内容（非空）
   - 预览内容格式合理（非乱码、非错误信息）

---

## TC-TPL-003 提取变量自动过滤系统内置参数
**级别：** P1
**前置：** 已登录

1. 调用模板提取接口 `POST /v1/template/extract`，传入包含业务参数与系统参数的 SQL 模板：
   ```json
   {
     "template": "SELECT * FROM orders WHERE owner_id = {{_user.id}} AND created_at <= {{_now}} AND status = {{status}} AND amount >= {{min_amount}}"
   }
   ```
2. 验证返回结果：
   - 状态码 200
   - `variables` 包含且仅包含 `["status", "min_amount"]`（集合相等）
   - `variables` 中**不含** `_user.id`、`_now`、`_date` 等系统变量（避免 UI 参数提取将系统变量作为业务入参）

---

## TC-TPL-004 模板预览系统参数注入与防伪造
**级别：** P1
**前置：** 已登录（当前用户记为 `current_user`）

1. 调用模板预览接口 `POST /v1/template/preview`，传入：
   ```json
   {
     "mode": "SQL",
     "template": "SELECT * FROM tasks WHERE owner = {{_user.id}} AND name = {{_user.name}} AND status = {{status}}",
     "values": {
       "status": "open",
       "_user.id": "fake_hacker_id"
     }
   }
   ```
2. 验证返回结果：
   - 状态码 200
   - `rendered` 中 `owner = '...'` 的值为当前登录用户的真实 `id`（而不是传入的 `fake_hacker_id`，系统参数强制覆盖）
   - `rendered` 中 `name = '...'` 包含当前登录用户的真实 `name`
   - `rendered` 中 `status = 'open'`

---

## TC-TPL-005 模板预览：字面问号与引号转义
**级别：** P1
**前置：** 已登录

> 预览 SQL 仅用于展示（不执行），但必须与真实执行语句一致，且可被用户直接复制。

1. **字面 `?`（LIKE 模式）**：`POST /v1/template/preview`
   ```json
   {"mode":"SQL","template":"SELECT * FROM track WHERE name LIKE '%?%' AND genreid = {{g}}","values":{"g":1}}
   ```
   - 200；`rendered` == `SELECT * FROM track WHERE name LIKE '%?%' AND genreid = 1`（字面 `?` 保留，绑定值落在真正的占位符）
2. **字面 `?`（PostgreSQL JSONB 运算符）**：
   ```json
   {"mode":"SQL","template":"SELECT * FROM t WHERE data ? 'key' AND name = {{n}} AND id = {{i}}","values":{"n":"x","i":7}}
   ```
   - 200；`rendered` == `SELECT * FROM t WHERE data ? 'key' AND name = 'x' AND id = 7`（`?` 运算符不被吃掉，绑定值不错位）
3. **数组与字面 `?` 混用**：template=`SELECT * FROM t WHERE note = '?' AND id IN ({{ids}})`，values=`{"ids":[1,2]}`
   - `rendered` == `SELECT * FROM t WHERE note = '?' AND id IN (1, 2)`
4. **字符串单引号转义**：
   ```json
   {"mode":"SQL","template":"SELECT * FROM artist WHERE name = {{name}}","values":{"name":"O'Brien"}}
   ```
   - `rendered` == `SELECT * FROM artist WHERE name = 'O''Brien'`（SQL 标准转义，复制后可执行）
