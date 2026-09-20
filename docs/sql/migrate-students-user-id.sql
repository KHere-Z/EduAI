-- ============================================================
-- EduAI students.user_id —— 补列 + 唯一索引
-- 版本: v1
-- 适用: MySQL 8.0+
--
-- 背景: Student 实体映射了 user_id（Student.java「关联用户ID → users.id」），
--       但 docs/sql/ 里从来没有这一列的 DDL —— init-database.sql 与
--       migrate-v2-student.sql 的 students 建表都没有它（全仓 grep user_id
--       只命中 teachers 表，见 init-database.sql 的 uk_user_id / fk_teacher_user）。
--       开发环境靠 application-dev.yml 的 `ddl-auto: update` 自动加上了；
--       生产是 application-prod.yml 的 `ddl-auto: validate` —— 这一列大概率是
--       上线时手工补的，没有任何版本记录。
--
-- 为什么现在必须补: 「注册即建学生档案」把 user_id 从「可选冗余」变成了关键路径 ——
--       AuthServiceImpl.createStudentProfile 的 INSERT 要写它，
--       之后 updateProfile / saveStudentGradeSchool / ensureStudentRecord 的
--       `UPDATE ... WHERE user_id = ?` 也全靠它定位。
--       ⚠️ 缺列会让**所有学生注册 500**：register 是 @Transactional，INSERT 失败会
--          连带回滚 users 行与注册礼包；而短信验证码已在 Redis 被 Lua 原子消费
--          （register 里的 CONSUME_CODE_SCRIPT，不参与事务），退不回来 ——
--          用户拿到 500、账号没建成、60 秒内（SMS_LIMIT_TTL）还不能重试，只能换号。
--
-- ⚠️ 执行顺序（硬性）：先在开发/预发跑本脚本 → 再生产执行 → 再打包重启服务。
--    反过来做（先重启）会让所有学生注册失败。
-- ============================================================

USE `eduai`;

-- ------------------------------------------------------------
-- 第 0 步 前置校验（先跑这四条，肉眼确认后再往下）
-- ------------------------------------------------------------

-- 0.1 列是否已存在？期望 1 行 = 已存在（第 1 步会自动跳过）；0 行 = 需新增
-- SELECT COUNT(*) AS col_exists
-- FROM information_schema.COLUMNS
-- WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'students' AND COLUMN_NAME = 'user_id';

-- 0.2 ⚠️ 门禁：是否已有重复绑定（同一个 users.id 挂多条档案）？
--     必须返回**空集**才能建唯一索引（与 migrate-auth-indexes.sql 的前置查重同口径）。
--     有输出时先按 0.3 人工合并，禁止直接往下跑。
-- SELECT user_id, COUNT(*) c, GROUP_CONCAT(id ORDER BY id) ids
-- FROM students WHERE user_id IS NOT NULL
-- GROUP BY user_id HAVING c > 1;

-- 0.3 若 0.2 有输出：先看各条重复档案挂了什么，决定保留哪一条。
--     保留被 exam_papers.student_id 引用的那条（否则试卷会被 CASCADE 删掉），
--     把其余行的 exam_papers.student_id / teacher_student.student_id 改到保留行后再删。
-- SELECT s.id, s.name, s.user_id, s.school,
--        (SELECT COUNT(*) FROM exam_papers      ep WHERE ep.student_id  = s.id) AS papers,
--        (SELECT COUNT(*) FROM teacher_student  ts WHERE ts.student_id  = s.id) AS relations
-- FROM students s
-- WHERE s.user_id IN (SELECT user_id FROM (SELECT user_id FROM students
--                       WHERE user_id IS NOT NULL GROUP BY user_id HAVING COUNT(*) > 1) t)
-- ORDER BY s.user_id, s.id;

-- 0.4 影响面：还有多少条档案没绑账号（存量未回填 + 管理员手工建）。记下这个数，第 3 步复核
-- SELECT COUNT(*) AS unbound FROM students WHERE user_id IS NULL;

-- ------------------------------------------------------------
-- 第 1 步 补列
-- 幂等: 已存在则输出一行 skipped，而不是报 1060 Duplicate column name。
-- 保持 NULL 可空 —— 存量未绑定账号的档案就是 NULL，不做 NOT NULL 收紧。
-- ------------------------------------------------------------
SET @ddl := (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `students` ADD COLUMN `user_id` BIGINT NULL COMMENT ''关联用户ID（学生登录账号 → users.id）'' AFTER `school`',
              'SELECT ''students.user_id 已存在，跳过'' AS skipped')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'students' AND COLUMN_NAME = 'user_id'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 第 2 步 唯一索引
-- 幂等: 已存在则输出一行 skipped。
--
-- 为什么必须是 UNIQUE，而不是「随便加个索引」:
--   1. 一个账号只能有一条档案。四处建档案代码全是「先 SELECT/COUNT 再 INSERT」、
--      没有任何锁（AuthServiceImpl.createStudentProfile 之外的三条兜底路径：
--      saveStudentGradeSchool、RelationServiceImpl.ensureStudentRecord、
--      AdminServiceImpl.createStudent）—— 唯一索引是唯一能挡住并发重复建档的东西。
--      一旦重复，StudentRepository.findByUserId 会抛
--      IncorrectResultSizeDataAccessException，学生端所有页面 500，
--      且这个号「再也修不好」（同 migrate-auth-indexes.sql 对 uk_phone 的论述）。
--      注: 上面两处兜底路径已加 DuplicateKeyException 捕获，命中后改为复用既有行。
--   2. 等值查询 user_id 从此走索引（原来全表扫，学生端每个请求都查它）。
-- 注: NULL 在 MySQL 唯一索引下可重复，存量未绑定账号的档案不受影响。
-- ------------------------------------------------------------
SET @ddl := (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `students` ADD UNIQUE INDEX `uk_user_id` (`user_id`)',
              'SELECT ''students.uk_user_id 已存在，跳过'' AS skipped')
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'students' AND INDEX_NAME = 'uk_user_id'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 若生产当年手工建过非唯一的 idx_user_id，上面这步之后它已被 uk_user_id 完全覆盖，
-- 可按 migrate-auth-indexes.sql 处理 idx_phone 的先例删掉（先 SHOW INDEX 确认它确实存在）:
-- ALTER TABLE `students` DROP INDEX `idx_user_id`;

-- ------------------------------------------------------------
-- 第 3 步 验证（期望 uk_user_id 的 Non_unique = 0；unbound 等于第 0.4 步的数）
-- ------------------------------------------------------------
-- SHOW INDEX FROM `students`;
-- SELECT COUNT(*) AS unbound FROM students WHERE user_id IS NULL;
-- SELECT COUNT(*) AS total   FROM students;
-- 端到端（注册一个测试学生后）—— 期望恰好 1 行:
-- SELECT id, name, user_id, contact, grade, school, subjects
-- FROM students WHERE user_id = <新学生 users.id>;

-- ------------------------------------------------------------
-- 刻意不做的事
-- ------------------------------------------------------------
-- 1. 不回填存量档案（产品已拍板：等老师添加时自然建）。若将来要回填，语句是:
--      INSERT INTO students (name, user_id, contact, created_at, updated_at)
--      SELECT COALESCE(NULLIF(u.real_name,''), NULLIF(u.nickname,''), '新同学'),
--             u.id, u.phone, NOW(), NOW()
--      FROM users u LEFT JOIN students s ON s.user_id = u.id
--      WHERE u.role_type = 4 AND s.id IS NULL;
--    ⚠️ 但现在**不能**直接跑：管理员当年手工建过的档案（user_id 为 NULL、姓名靠人工
--    对齐）不在这个 JOIN 的排除范围内，会被再建一份，反而制造重复。要回填必须先
--    人工对齐这批档案的 user_id。
-- 2. 不加 FK students.user_id → users.id：存量可能有悬空值（管理员 deleteStudent 只删
--    档案不删账号，见 AdminServiceImpl.deleteStudent），加 FK 会失败或把「删档案」变成
--    新的约束博弈。账号与档案的一致性由代码路径 + uk_user_id 保证。
-- 3. 不改 Student 实体加 @Column(unique = true)：dev 的 ddl-auto=update 会尝试建唯一
--    约束，一旦 dev 库里有脏数据就会**启动失败**。索引只由本脚本负责。
