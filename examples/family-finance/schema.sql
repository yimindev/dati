-- ============================================================
-- 家庭记账 (Family Finance) 示例数据库初始化脚本 (PostgreSQL)
-- ============================================================

-- 1. 收支分类表 (公共标准分类，全员共享)
CREATE TABLE IF NOT EXISTS category (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    type VARCHAR(10) NOT NULL, -- EXPENSE(支出) / INCOME(收入)
    description VARCHAR(200),
    created_at TIMESTAMP DEFAULT now()
);

-- 2. 资金账户表 (按用户隔离)
CREATE TABLE IF NOT EXISTS account (
    id BIGSERIAL PRIMARY KEY,
    user_name VARCHAR(50) NOT NULL,
    name VARCHAR(50) NOT NULL,
    type VARCHAR(20) NOT NULL, -- CASH, BANK_CARD, ALIPAY, WECHAT, OTHER
    description VARCHAR(200),
    created_at TIMESTAMP DEFAULT now(),
    CONSTRAINT uk_account_user_name UNIQUE (user_name, name)
);
CREATE INDEX IF NOT EXISTS idx_account_user ON account(user_name);

-- 3. 收支流水表 (按用户记录，支持全家透明查账)
CREATE TABLE IF NOT EXISTS transaction (
    id BIGSERIAL PRIMARY KEY,
    user_name VARCHAR(50) NOT NULL,
    account_id BIGINT NOT NULL REFERENCES account(id),
    transfer_to_account_id BIGINT REFERENCES account(id),
    category_id BIGINT REFERENCES category(id),
    type VARCHAR(10) NOT NULL, -- EXPENSE / INCOME / TRANSFER
    amount NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    transaction_date DATE NOT NULL,
    merchant VARCHAR(100),
    note VARCHAR(500),
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_txn_user_date ON transaction(user_name, transaction_date);
CREATE INDEX IF NOT EXISTS idx_txn_user_account ON transaction(user_name, account_id);
CREATE INDEX IF NOT EXISTS idx_txn_date ON transaction(transaction_date);

-- 4. 初始化基础标准分类
INSERT INTO category (name, type, description) VALUES
  ('餐饮', 'EXPENSE', '日常饮食、外卖、聚餐'),
  ('交通', 'EXPENSE', '公交地铁、打车、加油'),
  ('购物', 'EXPENSE', '日用品、服饰、数码'),
  ('居住', 'EXPENSE', '房租、水电燃气、物业'),
  ('娱乐', 'EXPENSE', '电影、游戏、旅游'),
  ('医疗', 'EXPENSE', '看病、买药'),
  ('教育', 'EXPENSE', '课程、书籍'),
  ('人情', 'EXPENSE', '红包、礼金'),
  ('其他', 'EXPENSE', '未分类支出'),
  ('工资', 'INCOME', '固定薪资收入'),
  ('奖金', 'INCOME', '绩效与年终奖'),
  ('理财收益', 'INCOME', '利息、基金收益')
ON CONFLICT (name) DO NOTHING;
