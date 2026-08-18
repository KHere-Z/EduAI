-- =====================================================
-- 扩宽图片字段以支持 base64 data URL（原 VARCHAR(500) 不够用）
-- 执行后重启即可
-- =====================================================

ALTER TABLE question_bank
    MODIFY COLUMN original_image_url MEDIUMTEXT,
    MODIFY COLUMN diagram_image_url MEDIUMTEXT,
    MODIFY COLUMN teacher_analysis_image MEDIUMTEXT;