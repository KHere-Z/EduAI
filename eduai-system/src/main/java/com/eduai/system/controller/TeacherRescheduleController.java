package com.eduai.system.controller;

import com.eduai.common.Result;
import com.eduai.system.service.TeacherRescheduleService;
import com.eduai.system.vo.RescheduleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 老师端 - 调课管理 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/teacher")
@RequiredArgsConstructor
public class TeacherRescheduleController {

    private final TeacherRescheduleService teacherRescheduleService;

    /** 查看学生的调课申请 */
    @GetMapping("/reschedules")
    public Result<List<RescheduleVO>> listReschedules() {
        return Result.ok(teacherRescheduleService.listReschedules());
    }

    /** 批准调课 */
    @PutMapping("/reschedules/{id}/approve")
    public Result<RescheduleVO> approve(@PathVariable Long id) {
        log.info("PUT /api/v1/teacher/reschedules/{}/approve", id);
        return Result.ok(teacherRescheduleService.approve(id));
    }

    /** 标记为待议 */
    @PutMapping("/reschedules/{id}/defer")
    public Result<RescheduleVO> defer(@PathVariable Long id) {
        log.info("PUT /api/v1/teacher/reschedules/{}/defer", id);
        return Result.ok(teacherRescheduleService.defer(id));
    }

    /** 关闭申请 */
    @DeleteMapping("/reschedules/{id}")
    public Result<Void> closeReschedule(@PathVariable Long id) {
        log.info("DELETE /api/v1/teacher/reschedules/{}", id);
        teacherRescheduleService.close(id);
        return Result.ok();
    }
}