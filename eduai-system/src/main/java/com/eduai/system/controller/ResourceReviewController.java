package com.eduai.system.controller;

import com.eduai.common.Result;
import com.eduai.system.service.ResourceService;
import com.eduai.system.vo.ResourceReviewVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 资源审核 Controller（管理员端）
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/resource/review")
@RequiredArgsConstructor
public class ResourceReviewController {

    private final ResourceService resourceService;

    /** 待审核资源列表（仅管理员） */
    @GetMapping("/pending")
    public Result<List<ResourceReviewVO>> pending() {
        return Result.ok(resourceService.listPendingResources());
    }

    /** 审核操作：通过（可改价）/ 驳回（必填理由） */
    @PostMapping("/{id}")
    public Result<Void> review(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean approved = Boolean.TRUE.equals(body.get("approved"));
        Integer price = body.get("price") instanceof Number n ? n.intValue() : null;
        String reason = body.get("reason") == null ? null : body.get("reason").toString();
        log.info("POST /api/v1/resource/review/{} approved={} price={} reason={}", id, approved, price, reason);
        resourceService.reviewResource(id, approved, price, reason);
        return Result.ok();
    }
}
