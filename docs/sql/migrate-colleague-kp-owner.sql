-- ============================================================
-- 同事关系 + 知识点 owner 可见性 迁移脚本
-- 执行前请备份；重复执行需先检查列是否已存在
-- ============================================================

-- 1. user_relations 加 type 字段（teacher_student=师生 / colleague=同事）
ALTER TABLE user_relations
    ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'teacher_student'
        COMMENT '关系类型：teacher_student=师生 / colleague=同事';

-- 2. knowledge_points 加 teacher_uid（owner）
ALTER TABLE knowledge_points
    ADD COLUMN teacher_uid BIGINT NULL
        COMMENT '所属老师UID（owner，为空表示历史共享数据，仅管理员可见/老师只读）';

-- 3. teacher_uid 索引（可见性过滤按 teacher_uid 查询）
CREATE INDEX idx_kp_teacher_uid ON knowledge_points (teacher_uid);

-- 4. kp_resources 加 teacher_uid（上传者 uid）
ALTER TABLE kp_resources
    ADD COLUMN teacher_uid BIGINT NULL
        COMMENT '上传者UID（为空表示历史数据，仅管理员可删）';

-- ============================================================
-- 回填说明（按需执行）
-- 现有知识点 teacher_uid 为空 → 后端视为「历史共享数据」：
--   * 管理员（roleType=1）：可见 + 可改
--   * 老师（roleType=3）：可见但只读（无 owner）
--   * 学生（roleType=4）：不可见
-- 若希望把存量知识点归属到某位老师（即可编辑），执行：
--   UPDATE knowledge_points SET teacher_uid = <某老师uid> WHERE teacher_uid IS NULL;
-- ============================================================
