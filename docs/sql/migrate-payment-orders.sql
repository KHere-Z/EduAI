-- ============================================================
-- 支付订单表 payment_orders（微信 Native 支付）
-- 生产 ddl-auto=validate，需在服务器手动执行本脚本后再重启。
-- ============================================================

USE `eduai`;

CREATE TABLE IF NOT EXISTS `payment_orders` (
    `id`             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id`       VARCHAR(32) NOT NULL COMMENT '商户订单号 out_trade_no',
    `user_id`        BIGINT      NOT NULL COMMENT '用户ID',
    `plan`           VARCHAR(20) NULL COMMENT '会员方案 month/quarter/halfyear/year',
    `points`         INT         NULL COMMENT '充值智学点',
    `total_cents`    INT         NOT NULL COMMENT '金额(分)',
    `status`         VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending/paid/closed',
    `transaction_id` VARCHAR(64) NULL COMMENT '微信支付交易号',
    `created_at`     DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_id` (`order_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付订单表';
