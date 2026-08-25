-- ============================================================
-- 学习资源「试看预览」迁移脚本
-- 执行前请备份；重复执行需先检查列是否已存在
-- ============================================================

-- resource_file 加 preview_path 字段：
--   压缩包(zip)内预览入口文件路径（如 课件/第1章.pdf）
--   非压缩包为 NULL —— 预览能力由文件自身类型实时判定，无需 preview_type 列
ALTER TABLE resource_file
    ADD COLUMN preview_path VARCHAR(500) DEFAULT NULL
        COMMENT '压缩包内预览入口文件路径（非压缩包为 NULL）';

-- ============================================================
-- 说明
--   预览文件与原始文件隔离：原文件存 uploads/resource/，预览文件存
--   uploads/previews/（截断+水印的独立文件），二者路径不重叠，杜绝改 URL 拿原文件。
--   预览能力判定见 ResourcePreviewService：
--     * 扩展名 .pdf → pdf
--     * 扩展名 .png/.jpg/.jpeg/.gif → image
--     * 扩展名 .doc/.docx/.ppt/.pptx/.xls/.xlsx → office（LibreOffice 转 PDF）
--     * 扩展名 .zip → 按 preview_path 抽内部文件后递归判定
--     * 其余 → none（无可预览内容）
-- ============================================================
