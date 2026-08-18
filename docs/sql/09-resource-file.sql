-- 学习资源 · 资源文件表（第 4 张表）
-- 前 3 张（教材/章节/小节）DDL 见 08-resource-catalog.sql
-- 本表由 JPA ddl-auto=update 自动建表亦可；生产(validate)请手动执行本文件。
DROP TABLE IF EXISTS `resource_file`;
CREATE TABLE IF NOT EXISTS resource_file (
  id BIGINT NOT NULL AUTO_INCREMENT,
  section_id BIGINT NOT NULL COMMENT '所属小节，关联 resource_section.id',
  subject VARCHAR(20) NOT NULL DEFAULT 'math',
  type VARCHAR(20) NOT NULL COMMENT '课件/学案/作业/试卷',
  year VARCHAR(10) DEFAULT NULL COMMENT '年份，如 2026',
  price INT NOT NULL DEFAULT 0 COMMENT '资源点：管理员端统一 200；老师端 100/300/500',
  title VARCHAR(200) DEFAULT NULL COMMENT '标题，缺省取文件名',
  file_name VARCHAR(255) NOT NULL,
  file_path VARCHAR(500) DEFAULT NULL COMMENT '服务端存储路径（相对 uploads/）',
  file_size BIGINT DEFAULT 0,
  author VARCHAR(50) DEFAULT NULL,
  uploader_id BIGINT DEFAULT NULL COMMENT '上传者 users.id（用于下载分成）',
  shared TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否共享：1=所有用户可见；0=仅上传者及其已接受学生可见',
  download_count INT NOT NULL DEFAULT 0 COMMENT '下载量',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_rf_section (section_id),
  KEY idx_rf_subject (subject)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
