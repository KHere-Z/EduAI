package com.eduai.system.controller;

import com.eduai.common.BusinessException;
import com.eduai.common.Result;
import com.eduai.common.service.MetricService;
import com.eduai.system.dto.DownloadFile;
import com.eduai.system.dto.PreviewFile;
import com.eduai.system.dto.ReorderDTO;
import com.eduai.system.dto.ResourceChapterDTO;
import com.eduai.system.dto.ResourceSectionDTO;
import com.eduai.system.dto.ResourceTextbookDTO;
import com.eduai.system.entity.ResourceChapter;
import com.eduai.system.entity.ResourceSection;
import com.eduai.system.entity.ResourceTextbook;
import com.eduai.system.service.ResourceService;
import com.eduai.system.vo.ArchiveEntryVO;
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
import java.util.Map;

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
    private final MetricService metricService;

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

    /** 更新教材（只改传入字段） */
    @PutMapping("/textbooks/{id}")
    public Result<ResourceTextbook> updateTextbook(@PathVariable Long id,
                                                   @RequestBody Map<String, Object> body) {
        log.info("PUT /api/v1/resource/textbooks/{} body={}", id, body);
        return Result.ok(resourceService.updateTextbook(id, body));
    }

    /** 教材排序 */
    @PutMapping("/textbooks/reorder")
    public Result<Void> reorderTextbooks(@RequestBody ReorderDTO dto) {
        log.info("PUT /api/v1/resource/textbooks/reorder orderedIds={}", dto != null ? dto.getOrderedIds() : null);
        resourceService.reorderTextbooks(dto != null ? dto.getOrderedIds() : null);
        return Result.ok();
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

    /** 更新章节（只改传入字段） */
    @PutMapping("/chapters/{id}")
    public Result<ResourceChapter> updateChapter(@PathVariable Long id,
                                                 @RequestBody Map<String, Object> body) {
        log.info("PUT /api/v1/resource/chapters/{} body={}", id, body);
        return Result.ok(resourceService.updateChapter(id, body));
    }

    /** 章节排序 */
    @PutMapping("/chapters/reorder")
    public Result<Void> reorderChapters(@RequestBody ReorderDTO dto) {
        log.info("PUT /api/v1/resource/chapters/reorder orderedIds={}", dto != null ? dto.getOrderedIds() : null);
        resourceService.reorderChapters(dto != null ? dto.getOrderedIds() : null);
        return Result.ok();
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

    /** 更新小节（只改传入字段） */
    @PutMapping("/sections/{id}")
    public Result<ResourceSection> updateSection(@PathVariable Long id,
                                                 @RequestBody Map<String, Object> body) {
        log.info("PUT /api/v1/resource/sections/{} body={}", id, body);
        return Result.ok(resourceService.updateSection(id, body));
    }

    /** 小节排序 */
    @PutMapping("/sections/reorder")
    public Result<Void> reorderSections(@RequestBody ReorderDTO dto) {
        log.info("PUT /api/v1/resource/sections/reorder orderedIds={}", dto != null ? dto.getOrderedIds() : null);
        resourceService.reorderSections(dto != null ? dto.getOrderedIds() : null);
        return Result.ok();
    }

    /** 删除小节（级联删资源） */
    @DeleteMapping("/sections/{id}")
    public Result<Void> deleteSection(@PathVariable Long id) {
        log.info("DELETE /api/v1/resource/sections/{}", id);
        resourceService.deleteSection(id);
        return Result.ok();
    }

    // ==================== 资源文件 ====================

    /** 资源列表（按任意层级节点，聚合子级） */
    @GetMapping("/resources")
    public Result<List<ResourceFileVO>> listResources(
            @RequestParam(required = false) String nodeType,
            @RequestParam(required = false) Long nodeId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) String subject) {
        // 兼容旧客户端：只传 sectionId 时映射为 section 节点
        String[] node = resolveNode(nodeType, nodeId, sectionId);
        return Result.ok(resourceService.listResources(node[0], Long.valueOf(node[1]), subject));
    }

    /** 上传资源（多文件，挂任意层级节点） */
    @PostMapping("/resources/upload")
    public Result<List<ResourceFileVO>> uploadResources(
            @RequestParam(required = false) String nodeType,
            @RequestParam(required = false) Long nodeId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(value = "subject", required = false) String subject,
            @RequestParam("tag") String tag,
            @RequestParam(value = "year", required = false) String year,
            @RequestParam(value = "price", required = false) Integer price,
            @RequestParam(value = "shared", defaultValue = "true") Boolean shared,
            @RequestParam(value = "previewPaths", required = false) String previewPaths,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @RequestParam(value = "files[]", required = false) List<MultipartFile> filesAlt) {
        List<MultipartFile> allFiles = new ArrayList<>();
        if (files != null) allFiles.addAll(files);
        if (filesAlt != null) allFiles.addAll(filesAlt);
        String[] node = resolveNode(nodeType, nodeId, sectionId);
        log.info("POST /api/v1/resource/resources/upload nodeType={} nodeId={} tag={} year={} price={} shared={} previewPaths={} files={}",
                node[0], node[1], tag, year, price, shared, previewPaths, allFiles.size());
        return Result.ok(resourceService.uploadResources(node[0], Long.valueOf(node[1]), subject, tag, year, price, shared, previewPaths, allFiles));
    }

    /** 解析节点参数：优先 nodeType+nodeId，兼容仅传 sectionId 的老客户端 */
    private String[] resolveNode(String nodeType, Long nodeId, Long sectionId) {
        if (nodeType != null && !nodeType.isBlank() && nodeId != null) {
            return new String[]{nodeType, String.valueOf(nodeId)};
        }
        if (sectionId != null) {
            return new String[]{"section", String.valueOf(sectionId)};
        }
        throw new BusinessException(400, "请提供 nodeType + nodeId（或兼容的 sectionId）");
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
        metricService.incrDownload();
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

    /** 检查压缩包内部文件清单（仅 zip，上传者可用） */
    @PostMapping("/archive/inspect")
    public Result<List<ArchiveEntryVO>> inspectArchive(@RequestParam("file") MultipartFile file) {
        log.info("POST /api/v1/resource/archive/inspect fileName={}",
                file != null ? file.getOriginalFilename() : null);
        return Result.ok(resourceService.inspectArchive(file));
    }

    /** 预览元信息：{type, url}，无可预览内容时 {type:"none"} */
    @GetMapping("/resources/{id}/preview")
    public Result<Map<String, String>> previewResource(@PathVariable Long id) {
        log.info("GET /api/v1/resource/resources/{}/preview", id);
        return Result.ok(resourceService.previewResource(id));
    }

    /** 预览文件流（截断+水印，与原始文件隔离） */
    @GetMapping("/resources/{id}/preview/file")
    public ResponseEntity<Resource> previewResourceFile(@PathVariable Long id) {
        PreviewFile pf = resourceService.previewResourceFile(id);
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .header("Content-Type", pf.contentType());
        try {
            long len = pf.resource().contentLength();
            if (len >= 0) {
                builder.contentLength(len);
            }
        } catch (IOException ignored) {
            // 长度未知时省略 Content-Length
        }
        return builder.body(pf.resource());
    }
}
