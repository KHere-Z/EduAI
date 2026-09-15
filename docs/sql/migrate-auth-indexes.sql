-- ============================================================
-- EduAI 认证 —— 唯一约束补强
-- 版本: v1
-- 适用: MySQL 8.0+
-- 背景: migrate-auth-upgrade.sql 建 user_wechat 时，uid 与 unionid 建了
--       唯一索引，但 user_uid 和 users.phone 只建了普通索引。本脚本补齐。
-- ⚠️ 前置: 先跑下方「查重」三行，必须全部返回空再执行 ALTER
-- （已在生产验证：2026-09-15 查重为空，Cardinality=3）
-- ============================================================

USE `eduai`;

-- ------------------------------------------------------------
-- 前置查重（有任何一行输出 = 存在重复，必须先人工合并，禁止直接 ALTER）
-- ------------------------------------------------------------
-- SELECT unionid,  COUNT(*) c FROM user_wechat GROUP BY unionid  HAVING c > 1;
-- SELECT user_uid, COUNT(*) c FROM user_wechat GROUP BY user_uid HAVING c > 1;
-- SELECT phone,    COUNT(*) c FROM users       GROUP BY phone    HAVING c > 1;

-- ============================================================
-- 1. 一个账号只允许一条微信绑定
-- 不修的风险: 小程序/公众号上线后同一用户可挂多条绑定（openid 是 per-app 的），
--            WechatBindingService.findUserByUserUid 返回多行
--            → IncorrectResultSizeDataAccessException → 微信登录 500
-- ============================================================
ALTER TABLE `user_wechat`
    ADD UNIQUE INDEX `uk_user_uid` (`user_uid`);

-- 旧的非唯一索引已被 uk_user_uid 覆盖（FK fk_wx_user 仍由 uk_user_uid 支撑）
ALTER TABLE `user_wechat`
    DROP INDEX `idx_user_uid`;

-- ============================================================
-- 2. 手机号唯一
-- 不修的风险: 同号多账号 → UserRepository.findByPhone 返回多行
--            → IncorrectResultSizeDataAccessException → 短信登录 500；
--            且「微信扫码是否还是同一账号」失去确定性
-- 注: phone 可空（NULL 在 MySQL 唯一索引下可重复），故不影响无手机号账号
-- ============================================================
ALTER TABLE `users`
    ADD UNIQUE INDEX `uk_phone` (`phone`);

ALTER TABLE `users`
    DROP INDEX `idx_phone`;

-- ============================================================
-- 验证
-- ============================================================
-- 期望 Non_unique = 0（uk_phone / uk_user_uid / idx_unionid / idx_uid）
-- SHOW INDEX FROM `users`        WHERE Key_name IN ('uk_phone','idx_uid');
-- SHOW INDEX FROM `user_wechat`  WHERE Key_name IN ('uk_user_uid','idx_unionid');
