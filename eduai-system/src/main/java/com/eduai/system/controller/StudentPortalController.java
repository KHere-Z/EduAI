package com.eduai.system.controller;

import com.eduai.common.Result;
import com.eduai.system.dto.RescheduleDTO;
import com.eduai.system.service.QuestionBankService;
import com.eduai.system.service.StudentPortalService;
import com.eduai.system.vo.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 学生端 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/student")
@RequiredArgsConstructor
public class StudentPortalController {

    private final StudentPortalService studentPortalService;
    private final QuestionBankService questionBankService;

    /** 学生报名科目列表（学科中心） */
    @GetMapping("/enrollments")
    public Result<StudentEnrollmentVO> getEnrollments() {
        return Result.ok(studentPortalService.getEnrollments());
    }

    /** 学生课表 */
    @GetMapping("/schedule")
    public Result<StudentScheduleVO> getSchedule(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return Result.ok(studentPortalService.getSchedule(year, month));
    }

    /** 提交调课申请 */
    @PostMapping("/reschedule")
    public Result<RescheduleVO> submitReschedule(@Valid @RequestBody RescheduleDTO dto) {
        log.info("POST /api/v1/student/reschedule body={}", dto);
        return Result.ok(studentPortalService.submitReschedule(dto));
    }

    /** 学习打卡 */
    @PostMapping("/checkin")
    public Result<Void> checkin() {
        studentPortalService.checkin();
        return Result.ok();
    }

    /** 打卡连续天数 */
    @GetMapping("/streak")
    public Result<StreakVO> getStreak() {
        return Result.ok(studentPortalService.getStreak());
    }

    // ==================== 错题录入 ====================

    /**
     * 学生录入错题（AI 分析后保存）
     * <p>
     * POST /api/v1/student/subject/{subject}/wrong-question
     */
    @PostMapping("/subject/{subject}/wrong-question")
    public Result<QuestionVO> addWrongQuestion(@PathVariable String subject,
                                               @RequestBody Map<String, Object> body) {
        log.info("POST /api/v1/student/subject/{}/wrong-question title={}",
                subject,
                body.get("title") != null ? body.get("title").toString().substring(0, Math.min(30, body.get("title").toString().length())) : "");
        return Result.ok(questionBankService.addWrongQuestion(subject, body));
    }
}