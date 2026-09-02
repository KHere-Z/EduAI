-- ============================================================
-- 资源文件表（resource_file）从「只绑小节」改为「绑定任意层级节点」
-- 新增通用锚点 node_type + node_id，替代单一 section_id。
-- 生产 ddl-auto=validate，需在服务器手动执行本脚本后再重启。
-- ============================================================

USE `eduai`;

-- 1. 加通用节点锚点（先可空，刷数据后再收紧）
ALTER TABLE resource_file
    ADD COLUMN node_type VARCHAR(20) NULL COMMENT '节点类型 textbook/chapter/section',
    ADD COLUMN node_id   BIGINT      NULL COMMENT '节点ID（对应层级实体主键）';

-- 2. 存量数据刷成 section（新数据不再写 section_id）
UPDATE resource_file
    SET node_type = 'section', node_id = section_id
    WHERE section_id IS NOT NULL;

-- 3. 收紧为 NOT NULL + section_id 改可空
ALTER TABLE resource_file
    MODIFY COLUMN node_type  VARCHAR(20) NOT NULL COMMENT '节点类型 textbook/chapter/section',
    MODIFY COLUMN node_id    BIGINT      NOT NULL COMMENT '节点ID（对应层级实体主键）',
    MODIFY COLUMN section_id BIGINT      NULL     COMMENT '所属小节（仅存量兼容，新数据不写）';

-- 4. 建索引
CREATE INDEX idx_rf_node ON resource_file (node_type, node_id);
