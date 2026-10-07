-- ============================================================
-- 伴侣记录 (Couple Journal) 示例数据库初始化脚本 (MySQL 8.0+)
-- ============================================================

CREATE DATABASE IF NOT EXISTS couple_journal DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE couple_journal;

-- 1. 伴侣生活足迹表 (记录旅游、电影、餐饮、日常等生活点滴)
CREATE TABLE IF NOT EXISTS couple_moment (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    category   VARCHAR(32)    NOT NULL COMMENT '分类: DINING(餐饮), MOVIE(观影), TRAVEL(旅游), DAILY(日常/约会)',
    title      VARCHAR(128)   NOT NULL COMMENT '做了什么/标题/电影/餐厅',
    start_date DATE           NOT NULL COMMENT '开始日期',
    end_date   DATE           NOT NULL COMMENT '结束日期(单日同start_date)',
    cost       DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '花费金额',
    rating     TINYINT        NOT NULL DEFAULT 3    COMMENT '体验评分/心情指数(1-5，默认3分中立)',
    content    TEXT           DEFAULT NULL          COMMENT '感受与细节备注',
    recorder   VARCHAR(64)    NOT NULL              COMMENT '记录人',
    INDEX idx_moment_dates (start_date, end_date),
    INDEX idx_moment_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='伴侣生活足迹表';

-- 2. 吵架复盘表 (记录争吵起因、破冰求和与和解承诺)
CREATE TABLE IF NOT EXISTS conflict_record (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    moment_id     BIGINT      DEFAULT NULL          COMMENT '关联足迹ID(如旅途中吵架，可为空)',
    incident_date DATE        NOT NULL              COMMENT '吵架日期',
    reason        TEXT        NOT NULL              COMMENT '争吵起因与矛盾',
    severity      TINYINT     NOT NULL DEFAULT 1    COMMENT '激烈程度(1-5，默认1级拌嘴闹脾气)',
    peacemaker    VARCHAR(64) NOT NULL              COMMENT '谁先道歉/主动破冰',
    agreement     TEXT        NOT NULL              COMMENT '和好约定与承诺',
    recorder      VARCHAR(64) NOT NULL              COMMENT '记录人',
    INDEX idx_conflict_date (incident_date),
    CONSTRAINT fk_conflict_moment FOREIGN KEY (moment_id) REFERENCES couple_moment (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='吵架复盘表';

-- 3. 初始演示数据 (Mock Data)
INSERT INTO couple_moment (id, category, title, start_date, end_date, cost, rating, content, recorder)
VALUES
  (1, 'TRAVEL', '云南大理丽江5日游', '2026-10-01', '2026-10-05', 6500.00, 5, '环洱海自驾，天气超好，风景美绝了', 'admin'),
  (2, 'MOVIE', '看电影《阿凡达3》', '2026-09-20', '2026-09-20', 140.00, 5, '在万达IMAX看的，视觉特效拉满', 'admin'),
  (3, 'MOVIE', '看电影《疯狂动物城2》', '2026-08-15', '2026-08-15', 90.00, 4, '剧情很有趣，爆米花很好吃', 'admin'),
  (4, 'DINING', '太二酸菜鱼', '2026-09-28', '2026-09-28', 188.00, 4, '酸菜鱼依然稳定发挥，加了脆豆腐', 'admin'),
  (5, 'DINING', '三里屯牛排家', '2026-08-18', '2026-08-18', 520.00, 5, '恋爱纪念日大餐，黑松露牛排超赞', 'admin'),
  (6, 'DAILY', '周末奥森公园野餐散步', '2026-09-13', '2026-09-13', 65.00, 5, '带了小蛋糕和水果，吹着初秋的微风', 'admin')
ON DUPLICATE KEY UPDATE title = VALUES(title);

INSERT INTO conflict_record (id, moment_id, incident_date, reason, severity, peacemaker, agreement, recorder)
VALUES
  (1, 1, '2026-10-03', '自驾走错路，因为看导航分歧着急上火', 2, 'admin', '约定以后副驾只报路名，不催促也不抱怨，心平气和', 'admin'),
  (2, NULL, '2026-09-08', '周末吃完饭谁都不想洗碗堆在水槽里', 1, 'admin', '下周买洗碗机；平时轮流洗，绝不过夜', 'admin')
ON DUPLICATE KEY UPDATE reason = VALUES(reason);
