package com.eduai.system.controller;

import com.eduai.common.Result;
import com.eduai.system.dto.DownloadFile;
import com.eduai.system.dto.ResourceChapterDTO;
import com.eduai.system.dto.ResourceSectionDTO;
import com.eduai.system.dto.ResourceTextbookDTO;
import com.eduai.system.entity.ResourceChapter;
import com.eduai.system.entity.ResourceSection;
import com.eduai.system.entity.ResourceTextbook;
import com.eduai.system.service.ResourceService;
import com.eduai.system.vo.ResourceFileVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 学习资源 Controller（教材 → 章节 → 小节 → 资源文件）
 * <p>
 * 浏览/下载：登录即可；上传/删除目录与资源：教师(3) + 管理员(1)。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/resource")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    // ==================== 教材 ====================

    /** 教材列表 */
    @GetMapping("/textbooks")
    public Result<List<ResourceTextbook>> listTextbooks(
            @RequestParam(defaultValue = "math") String subject) {
        return Result.ok(resourceService.listTextbooks(subject));
    }

    /** 新建教材 */
    @PostMapping("/textbooks")
    public Result<ResourceTextbook> createTextbook(@Valid @RequestBody ResourceTextbookDTO dto) {
        log.info("POST /api/v1/resource/textbooks body={}", dto);
        return Result.ok(resourceService.createTextbook(dto));
    }

    /** 删除教材（级联删章节/小节/资源） */
    @DeleteMapping("/textbooks/{id}")
    public Result<Void> deleteTextbook(@PathVariable Long id) {
        log.info("DELETE /api/v1/resource/textbooks/{}", id);
        resourceService.deleteTextbook(id);
        return Result.ok();
    }

    // ==================== 章节 ====================

    /** 章节列表 */
    @GetMapping("/textbooks/{textbookId}/chapters")
    public Result<List<ResourceChapter>> listChapters(@PathVariable Long textbookId) {
        return Result.ok(resourceService.listChapters(textbookId));
    }

    /** 新建章节 */
    @PostMapping("/textbooks/{textbookId}/chapters")
    public Result<ResourceChapter> createChapter(@PathVariable Long textbookId,
                                                 @Valid @RequestBody ResourceChapterDTO dto) {
        log.info("POST /api/v1/resource/textbooks/{}/chapters body={}", textbookId, dto);
        return Result.ok(resourceService.createChapter(textbookId, dto));
    }

    /** 删除章节（级联删小节/资源） */
    @DeleteMapping("/chapters/{id}")
    public Result<Void> deleteChapter(@PathVariable Long id) {
        log.info("DELETE /api/v1/resource/chapters/{}", id);
        resourceService.deleteChapter(id);
        return Result.ok();
    }

    // ==================== 小节 ====================

    /** 小节列表 */
    @GetMapping("/chapters/{chapterId}/sections")
    public Result<List<ResourceSection>> listSections(@PathVariable Long chapterId) {
        return Result.ok(resourceService.listSections(chapterId));
    }

    /** 新建小节 */
    @PostMapping("/chapters/{chapterId}/sections")
    public Result<ResourceSection> createSection(@PathVariable Long chapterId,
                                                 @Valid @RequestBody ResourceSectionDTO dto) {
        log.info("POST /api/v1/resource/chapters/{}/sections body={}", chapterId, dto);
        return Result.ok(resourceService.createSection(chapterId, dto));
    }

    /** 删除小节（级联删资源） */
    @DeleteMapping("/sections/{id}")
    public Result<Void> deleteSection(@PathVariable Long id) {
        log.info("DELETE /api/v1/resource/sections/{}", id);
        resourceService.deleteSection(id);
        return Result.ok();
    }

    // ==================== 资源文件 ====================

    /** 资源列表（按小节） */
    @GetMapping("/resources")
    public Result<List<ResourceFileVO>> listResources(
            @RequestParam Long sectionId,
            @RequestParam(required = false) String subject) {
        return Result.ok(resourceService.listResources(sectionId, subject));
    }

    /** 上传资源（多文件） */
    @PostMapping("/resources/upload")
    public Result<List<ResourceFileVO>> uploadResources(
            @RequestParam("sectionId") Long sectionId,
            @RequestParam(value = "subject", required = false) String subject,
            @RequestParam("tag") String tag,
            @RequestParam(value = "year", required = false) String year,
            @RequestParam(value = "price", required = false) Integer price,
            @RequestParam(value = "shared", defaultValue = "true") Boolean shared,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @RequestParam(value = "files[]", required = false) List<MultipartFile> filesAlt) {
        List<MultipartFile> allFiles = new ArrayList<>();
        if (files != null) allFiles.addAll(files);
        if (filesAlt != null) allFiles.addAll(filesAlt);
        log.info("POST /api/v1/resource/resources/upload sectionId={} tag={} year={} price={} shared={} files={}",
                sectionId, tag, year, price, shared, allFiles.size());
        return Result.ok(resourceService.uploadResources(sectionId, subject, tag, year, price, shared, allFiles));
    }

    /** 删除资源（含磁盘文件） */
    @DeleteMapping("/resources/{id}")
    public Result<Void> deleteResource(@PathVariable Long id) {
        log.info("DELETE /api/v1/resource/resources/{}", id);
        resourceService.deleteResource(id);
        return Result.ok();
    }

    /** 下载资源文件（流式，避免整文件载入内存） */
    @GetMapping("/resources/{id}/download")
    public ResponseEntity<Resource> downloadResource(@PathVariable Long id) {
        DownloadFile df = resourceService.downloadResource(id);
        String encodedName = URLEncoder.encode(df.fileName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .header("Content-Type", "application/octet-stream")
                .header("Content-Disposition",
                        "attachment; filename*=UTF-8''" + encodedName);
        try {
            long len = df.resource().contentLength();
            if (len >= 0) {
                builder.contentLength(len);
            }
        } catch (IOException ignored) {
            // 长度未知时省略 Content-Length，仍可流式传输
        }
        return builder.body(df.resource());
    }
}
