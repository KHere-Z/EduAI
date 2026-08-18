package com.eduai.system.controller;

import com.eduai.common.Result;
import com.eduai.system.service.FeedbackService;
import com.eduai.system.vo.FeedbackVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学生端 — 学习反馈 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/student/feedbacks")
@RequiredArgsConstructor
public class StudentFeedbackController {

    private final FeedbackService feedbackService;

    /** 收到的反馈（可选按学科筛选） */
    @GetMapping
    public Result<List<FeedbackVO>> list(@RequestParam(required = false) String subject) {
        log.info("GET /api/v1/student/feedbacks subject={}", subject);
        return Result.ok(feedbackService.listStudentFeedbacks(subject));
    }
}
