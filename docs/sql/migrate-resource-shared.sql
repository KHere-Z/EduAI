-- ============================================================
-- 学习资源「共享/不共享」可见性 迁移脚本
-- 执行前请备份；重复执行需先检查列是否已存在
-- ============================================================

-- resource_file 加 shared 字段（true/1=共享所有人可见；false/0=仅上传者及其已接受学生可见）
-- 存量资源默认 shared=1（共享），保持向后兼容（老数据所有人可见不变）
ALTER TABLE resource_file
    ADD COLUMN shared TINYINT(1) NOT NULL DEFAULT 1
        COMMENT '是否共享：1=所有用户可见；0=仅上传者及其已接受学生可见';

-- ============================================================
-- 说明
--   owner 复用已有 uploader_id（上传时写入 StpUtil.getLoginIdAsLong() = users.id），
--   无需新增字段。可见性规则见 ResourceServiceImpl.canReadResource：
--     * 管理员：全部可见
--     * shared=1：所有登录用户可见
--     * shared=0：仅上传者本人 + 其已接受师生关系的学生可见
--   「已接受学生」判定：user_relations 中 type='teacher_student' 且 status='accepted'
--   （含 users.teacher_uid 兜底）。
-- ============================================================
