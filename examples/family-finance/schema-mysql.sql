-- ============================================================
-- 家庭记账 (Family Finance) 示例数据库初始化脚本 (MySQL 8.0+)
-- ============================================================

CREATE DATABASE IF NOT EXISTS `family_finance` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `family_finance`;

-- 1. 收支分类表 (公共标准分类，全员共享)
CREATE TABLE IF NOT EXISTS `category` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `type` VARCHAR(10) NOT NULL COMMENT 'EXPENSE(支出) / INCOME(收入)',
    `description` VARCHAR(200) COMMENT '分类说明',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `uk_category_name` UNIQUE (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收支分类表';

-- 2. 资金账户表 (按用户隔离)
CREATE TABLE IF NOT EXISTS `account` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_name` VARCHAR(50) NOT NULL COMMENT '所属用户名',
    `name` VARCHAR(50) NOT NULL COMMENT '账户名称',
    `type` VARCHAR(20) NOT NULL COMMENT 'CASH, BANK_CARD, ALIPAY, WECHAT, OTHER',
    `description` VARCHAR(200) COMMENT '账户说明',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `uk_account_user_name` UNIQUE (`user_name`, `name`),
    INDEX `idx_account_user` (`user_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资金账户表';

-- 3. 收支流水表 (按用户记录，支持全家透明查账)
CREATE TABLE IF NOT EXISTS `transaction` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_name` VARCHAR(50) NOT NULL COMMENT '记账人用户名',
    `account_id` BIGINT NOT NULL COMMENT '资金账户ID',
    `transfer_to_account_id` BIGINT DEFAULT NULL COMMENT '转入账户ID (转账时填写)',
    `category_id` BIGINT DEFAULT NULL COMMENT '收支分类ID',
    `type` VARCHAR(10) NOT NULL COMMENT 'EXPENSE / INCOME / TRANSFER',
    `amount` DECIMAL(12,2) NOT NULL COMMENT '交易金额',
    `transaction_date` DATE NOT NULL COMMENT '交易日期',
    `merchant` VARCHAR(100) DEFAULT NULL COMMENT '商户/交易对方',
    `note` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_txn_account` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`),
    CONSTRAINT `fk_txn_transfer_account` FOREIGN KEY (`transfer_to_account_id`) REFERENCES `account` (`id`),
    CONSTRAINT `fk_txn_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`),
    INDEX `idx_txn_user_date` (`user_name`, `transaction_date`),
    INDEX `idx_txn_user_account` (`user_name`, `account_id`),
    INDEX `idx_txn_date` (`transaction_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收支流水表';

-- 4. 初始化基础标准分类
INSERT INTO `category` (`name`, `type`, `description`) VALUES
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
ON DUPLICATE KEY UPDATE `type` = VALUES(`type`), `description` = VALUES(`description`);
