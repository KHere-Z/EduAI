package com.eduai.system.controller;

import com.eduai.common.Result;
import com.eduai.system.dto.KnowledgePointDTO;
import com.eduai.system.service.KnowledgePointService;
import com.eduai.system.vo.KnowledgePointPageVO;
import com.eduai.system.vo.KnowledgePointVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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
        // 学生端：多学期批量查询
        if (grades != null && !grades.isBlank()) {
            int ps = pageSize != null ? pageSize : 50;
            return Result.ok(knowledgePointService.listByGrades(page, ps, subject, grades));
        }
        // 老师端：单年级筛选
        int ps = pageSize != null ? pageSize : 15;
        return Result.ok(knowledgePointService.list(page, ps, subject, gradeLevel));
    }

    /**
     * 新增知识点
     */
    @PostMapping
    public Result<KnowledgePointVO> create(@Valid @RequestBody KnowledgePointDTO dto) {
        log.info("POST /api/v1/knowledge-points body={}", dto);
        return Result.ok(knowledgePointService.create(dto));
    }

    /**
     * 修改知识点
     */
    @PutMapping("/{id}")
    public Result<KnowledgePointVO> update(@PathVariable Long id, @Valid @RequestBody KnowledgePointDTO dto) {
        log.info("PUT /api/v1/knowledge-points/{} body={}", id, dto);
        return Result.ok(knowledgePointService.update(id, dto));
    }

    /**
     * 删除知识点
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        log.info("DELETE /api/v1/knowledge-points/{}", id);
        knowledgePointService.delete(id);
        return Result.ok();
    }
}