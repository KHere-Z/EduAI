package com.eduai.system.controller;

import com.eduai.common.Result;
import com.eduai.system.dto.DownloadFile;
import com.eduai.system.dto.KnowledgePointDTO;
import com.eduai.system.service.KnowledgePointService;
import com.eduai.system.vo.KnowledgePointPageVO;
import com.eduai.system.vo.KnowledgePointVO;
import com.eduai.system.vo.KpResourceVO;
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
import java.util.List;

/**
 * 知识点 Controller（全平台共享，教师/管理员可管理，学生可查看）
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/knowledge-points")
@RequiredArgsConstructor
public class KnowledgePointController {

    private final KnowledgePointService knowledgePointService;

    /**
     * 知识点列表（统一入口）
     * <p>
     * 老师端：?subject=math&gradeLevel=初三·上学期 → 单年级筛选（分页15）
     * <br>学生端：?subject=math&grades=初一·上学期,初一·下学期,... → 多学期批量查询（分页50）
     */
    @GetMapping
    public Result<KnowledgePointPageVO> list(
            @RequestParam String subject,
            @RequestParam(required = false) String gradeLevel,
            @RequestParam(required = false) String grades,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) Integer pageSize) {
        if (grades != null && !grades.isBlank()) {
            int ps = pageSize != null ? pageSize : 50;
            return Result.ok(knowledgePointService.listByGrades(page, ps, subject, grades));
        }
        int ps = pageSize != null ? pageSize : 15;
        return Result.ok(knowledgePointService.list(page, ps, subject, gradeLevel));
    }

    /** 新增知识点 */
    @PostMapping
    public Result<KnowledgePointVO> create(@Valid @RequestBody KnowledgePointDTO dto) {
        log.info("POST /api/v1/knowledge-points body={}", dto);
        return Result.ok(knowledgePointService.create(dto));
    }

    /** 修改知识点 */
    @PutMapping("/{id}")
    public Result<KnowledgePointVO> update(@PathVariable Long id, @Valid @RequestBody KnowledgePointDTO dto) {
        log.info("PUT /api/v1/knowledge-points/{} body={}", id, dto);
        return Result.ok(knowledgePointService.update(id, dto));
    }

    /** 删除知识点 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        log.info("DELETE /api/v1/knowledge-points/{}", id);
        knowledgePointService.delete(id);
        return Result.ok();
    }

    // ==================== 知识点资源 ====================

    /** 上传资源文件 */
    @PostMapping("/{kpId}/resources")
    public Result<KpResourceVO> uploadResource(
            @PathVariable Long kpId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("tag") String tag) {
        log.info("POST /api/v1/knowledge-points/{}/resources file={} tag={}",
                kpId, file.getOriginalFilename(), tag);
        return Result.ok(knowledgePointService.uploadResource(kpId, file, tag));
    }

    /** 资源列表 */
    @GetMapping("/{kpId}/resources")
    public Result<List<KpResourceVO>> listResources(@PathVariable Long kpId) {
        return Result.ok(knowledgePointService.listResources(kpId));
    }

    /** 下载资源文件（流式，避免整文件载入内存） */
    @GetMapping("/resources/{id}/download")
    public ResponseEntity<Resource> downloadResource(@PathVariable Long id) {
        DownloadFile df = knowledgePointService.downloadResource(id);
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

    /** 删除资源 */
    @DeleteMapping("/resources/{id}")
    public Result<Void> deleteResource(@PathVariable Long id) {
        log.info("DELETE /api/v1/knowledge-points/resources/{}", id);
        knowledgePointService.deleteResource(id);
        return Result.ok();
    }
}
