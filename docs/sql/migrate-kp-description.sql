-- ============================================================
-- 知识点（knowledge_points）新增 description 描述字段
-- 生产 ddl-auto=validate，需在服务器手动执行本脚本后再重启。
-- ============================================================

ALTER TABLE knowledge_points
    ADD COLUMN description TEXT DEFAULT NULL
        COMMENT '知识点描述';
