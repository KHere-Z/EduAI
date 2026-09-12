-- 智学点流水软删
--
-- 背景：用户在充值页可勾选删除自己的智学点变动记录。point_transactions 同时是
-- 支付对账依据（与微信/支付宝订单关联），物理删除会让后台再也对不上渠道流水，
-- 故改为软删：行永远保留，只置 deleted = 1，仅对用户不可见。
--
-- ⚠️ 生产环境 ddl-auto=validate，必须先执行本脚本再部署新 jar，否则启动即校验失败。

ALTER TABLE point_transactions
    ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删标记:0=正常,1=用户已删除';

-- 列表查询是 WHERE user_id = ? AND deleted = 0 ORDER BY created_at DESC，
-- 复合索引让三个条件都走索引，避免用 idx_user_id 取回后再过滤 + filesort。
-- 建好后 idx_user_id 成为冗余（本索引最左前缀已覆盖 user_id），可择机 drop。
CREATE INDEX idx_user_deleted_created ON point_transactions (user_id, deleted, created_at);
