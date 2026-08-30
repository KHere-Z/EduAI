-- ============================================================
-- 学习资源审核 + 站内消息 迁移脚本
-- 生产 ddl-auto=validate，需在服务器手动执行本脚本后再重启。
-- 重复执行请先检查列/表是否已存在。
-- ============================================================

-- 1. resource_file 增加审核字段（存量资源默认 approved，视为已通过）
ALTER TABLE resource_file
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'approved'
        COMMENT '审核状态：pending/approved/rejected',
    ADD COLUMN reject_reason VARCHAR(500) DEFAULT NULL
        COMMENT '驳回理由（仅 rejected 时非空）',
    ADD COLUMN reviewer_id BIGINT DEFAULT NULL
        COMMENT '审核管理员 users.id',
    ADD COLUMN reviewed_at DATETIME DEFAULT NULL
        COMMENT '审核时间';

-- 2. 站内消息表（user_message，列名避开 MySQL 保留字 read）
CREATE TABLE IF NOT EXISTS user_message (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL COMMENT '接收者 users.id',
  type VARCHAR(50) DEFAULT NULL COMMENT '消息类型，如 RESOURCE_REVIEW',
  title VARCHAR(200) DEFAULT NULL,
  content TEXT,
  read_flag TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已读：1=已读 0=未读',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_um_user (user_id),
  KEY idx_um_user_read (user_id, read_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
