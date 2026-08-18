-- 智学点充值 & 会员体系
ALTER TABLE users ADD COLUMN points INT DEFAULT 0 COMMENT '智学点余额';

CREATE TABLE IF NOT EXISTS point_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID(users.id)',
    amount INT NOT NULL COMMENT '变动量(正=充值,负=消费)',
    type VARCHAR(20) NOT NULL COMMENT 'charge/consume/refund/gift',
    description VARCHAR(200) COMMENT '变动说明',
    balance_after INT COMMENT '变动后余额',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点数变动记录';

CREATE TABLE IF NOT EXISTS membership (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL COMMENT '用户ID(users.id)',
    plan VARCHAR(20) COMMENT 'month/quarter/year',
    started_at DATETIME,
    expires_at DATETIME,
    status VARCHAR(10) DEFAULT 'active' COMMENT 'active/expired/cancelled',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员记录';
