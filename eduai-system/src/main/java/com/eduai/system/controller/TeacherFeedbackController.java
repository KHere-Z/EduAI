package com.eduai.system.controller;

import com.eduai.common.Result;
import com.eduai.system.dto.FeedbackDTO;
import com.eduai.system.service.FeedbackService;
import com.eduai.system.vo.FeedbackVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 老师端 — 学习反馈 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/teacher/feedbacks")
@RequiredArgsConstructor
public class TeacherFeedbackController {

    private final FeedbackService feedbackService;

    /** 写反馈 */
    @PostMapping
    public Result<FeedbackVO> create(@Valid @RequestBody FeedbackDTO dto) {
        log.info("POST /api/v1/teacher/feedbacks studentId={}, subject={}", dto.getStudentId(), dto.getSubject());
        return Result.ok(feedbackService.create(dto));
    }

    /** 已发送反馈列表（可选按学科筛选） */
    @GetMapping
    public Result<List<FeedbackVO>> list(@RequestParam(required = false) String subject) {
        log.info("GET /api/v1/teacher/feedbacks subject={}", subject);
        return Result.ok(feedbackService.listTeacherFeedbacks(subject));
    }

    /** 删除反馈 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        log.info("DELETE /api/v1/teacher/feedbacks/{}", id);
        feedbackService.delete(id);
        return Result.ok();
    }
}
