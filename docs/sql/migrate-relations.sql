

-- 学生表补充字段（已存在则忽略错误）
ALTER TABLE students ADD COLUMN subjects JSON COMMENT '学科列表';

-- 用户关系表（师生绑定、关联请求）
CREATE TABLE IF NOT EXISTS user_relations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    from_uid BIGINT NOT NULL COMMENT '发起方UID',
    to_uid BIGINT NOT NULL COMMENT '接收方UID',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending=待确认 / accepted=已关联 / rejected=已拒绝',
    type VARCHAR(20) NOT NULL DEFAULT 'teacher_student' COMMENT '关系类型：teacher_student=师生 / colleague=同事',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_from_to (from_uid, to_uid),
    INDEX idx_to_uid (to_uid),
    INDEX idx_from_uid (from_uid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户关系表';

SELECT s.id, s.class_date, ts.teacher_id, st.name
FROM student_session s
         JOIN student_enrollment e ON s.enrollment_id = e.id
         JOIN teacher_student ts ON e.teacher_student_id = ts.id
         JOIN students st ON ts.student_id = st.id
WHERE ts.teacher_id = 11
  AND s.class_date BETWEEN '2026-07-01' AND '2026-08-31';
