package com.eduai.system.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;

/**
 * 题库图片存储 — 把 base64 data URL 落盘为 {@code /uploads/question-images/**} 文件，返回 web URL。
 * <p>
 * 背景：{@code question_bank} 的 {@code original_image_url} / {@code diagram_image_url} /
 * {@code teacher_analysis_image} 三列是 MEDIUMTEXT，历史上直接存 base64 data URL，列表查询时
 * 整列载入堆内存，高并发下是内存/带宽热点。本类在写入侧拦截 base64 并落盘，DB 只存轻量 URL。
 * <p>
 * 读路径无需改动：返回的 {@code /uploads/...} URL 由 WebMvcConfig 静态映射 + Nginx 反代直接
 * 提供给前端 {@code <img src>}；PDF 导出 / AI 识图已能解析 {@code /uploads/...} 路径。
 */
@Slf4j
@Component
public class ImageStorageService {

    @Value("${eduai.upload.dir:uploads}")
    private String uploadDir;

    /**
     * 若 value 是 base64 data URL（{@code data:image/...;base64,...}）则解码落盘并返回
     * {@code /uploads/question-images/{subDir}/{yyyy-MM-dd}/{uuid}.{ext}}；否则原样返回
     * （已是 URL / 相对路径 / 空串 / null）。落盘失败时回退原值，功能不中断，仅失去性能优化。
     */
    public String persistIfBase64(String value, String subDir) {
        if (value == null || !value.startsWith("data:image/")) {
            return value;
        }
        try {
            int comma = value.indexOf(',');
            if (comma <= 0) {
                return value; // 非标准 data URL，保留原值
            }
            String header = value.substring(0, comma);
            String base64 = value.substring(comma + 1).trim();
            String ext = extractExt(header);
            byte[] bytes = Base64.getDecoder().decode(base64);

            String dateDir = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            Path dir = Path.of(uploadDir, "question-images", subDir, dateDir)
                    .toAbsolutePath().normalize();
            Files.createDirectories(dir);

            String name = UUID.randomUUID().toString() + "." + ext;
            Path target = dir.resolve(name);
            Files.write(target, bytes);

            String url = "/uploads/question-images/" + subDir + "/" + dateDir + "/" + name;
            log.info("图片落盘: {} bytes → {}", bytes.length, url);
            return url;
        } catch (Exception e) {
            log.warn("图片落盘失败，保留 base64 原值: {}", e.getMessage());
            return value;
        }
    }

    /** 从 {@code data:image/xxx;base64} 头提取扩展名 */
    private String extractExt(String header) {
        if (header.contains("image/png")) return "png";
        if (header.contains("image/jpeg") || header.contains("image/jpg")) return "jpg";
        if (header.contains("image/gif")) return "gif";
        if (header.contains("image/webp")) return "webp";
        if (header.contains("image/svg")) return "svg";
        return "png";
    }
}
