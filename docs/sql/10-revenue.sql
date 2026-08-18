-- 老师资源下载分成 + 提现（3 张新表）
-- 金额字段单位统一为「分」（1 元 = 100 分 = 100 智学点）。
-- 开发环境由 JPA ddl-auto=update 自动建表；生产(validate)请手动执行本文件。

-- 1. 资源下载记录（防「同一学生重复下载重复扣费」）
CREATE TABLE IF NOT EXISTS resource_downloads (
  id BIGINT NOT NULL AUTO_INCREMENT,
  resource_id BIGINT NOT NULL COMMENT 'resource_file.id',
  user_id BIGINT NOT NULL COMMENT '下载用户 users.id',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_download (resource_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 老师收益流水（下载分成 50%）
CREATE TABLE IF NOT EXISTS teacher_earnings (
  id BIGINT NOT NULL AUTO_INCREMENT,
  teacher_id BIGINT NOT NULL COMMENT '老师 users.id',
  resource_id BIGINT DEFAULT NULL COMMENT 'resource_file.id',
  resource_title VARCHAR(255) DEFAULT NULL COMMENT '资源标题（快照）',
  student_id BIGINT DEFAULT NULL COMMENT '下载学生 users.id',
  amount BIGINT NOT NULL COMMENT '分成金额（分）',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_te_teacher (teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 提现申请
CREATE TABLE IF NOT EXISTS withdraw_requests (
  id BIGINT NOT NULL AUTO_INCREMENT,
  teacher_id BIGINT NOT NULL COMMENT '老师 users.id',
  amount BIGINT NOT NULL COMMENT '提现金额（分）',
  bank_name VARCHAR(100) DEFAULT NULL COMMENT '开户行',
  bank_card_no VARCHAR(50) DEFAULT NULL COMMENT '银行卡号',
  account_name VARCHAR(50) DEFAULT NULL COMMENT '开户名',
  status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending/approved/paid/rejected',
  review_note VARCHAR(200) DEFAULT NULL COMMENT '审核备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  reviewed_at DATETIME DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_wr_teacher (teacher_id),
  KEY idx_wr_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
