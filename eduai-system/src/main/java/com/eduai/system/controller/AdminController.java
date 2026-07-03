package com.eduai.system.controller;

import com.eduai.common.Result;
import com.eduai.system.dto.AdminSettingsDTO;
import com.eduai.system.dto.AdminStudentDTO;
import com.eduai.system.dto.AdminTeacherDTO;
import com.eduai.system.service.AdminService;
import com.eduai.system.vo.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员 Controller
 * <p>
 * 管理员端 API：学生管理、老师管理、排课查看、概览统计、系统设置
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // ==================== 学生管理 ====================

    /**
     * 全局学生列表（不过滤 teacher_id）
     */
    @GetMapping("/students")
    public Result<AdminStudentPageVO> listStudents(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String grade) {
        log.info("GET /api/v1/admin/students page={} pageSize={} keyword={} grade={}", page, pageSize, keyword, grade);
        return Result.ok(adminService.listStudents(page, pageSize, keyword, grade));
    }

    /**
     * 新增学生（仅基本档案，不含排课）
     */
    @PostMapping("/students")
    public Result<AdminStudentVO> createStudent(@Valid @RequestBody AdminStudentDTO dto) {
        log.info("POST /api/v1/admin/students body={}", dto);
        return Result.ok(adminService.createStudent(dto));
    }

    /**
     * 更新学生信息
     */
    @PutMapping("/students/{id}")
    public Result<AdminStudentVO> updateStudent(@PathVariable Long id, @Valid @RequestBody AdminStudentDTO dto) {
        log.info("PUT /api/v1/admin/students/{} body={}", id, dto);
        return Result.ok(adminService.updateStudent(id, dto));
    }

    /**
     * 删除学生（级联删除关联的 teacher_student / enrollment / session）
     */
    @DeleteMapping("/students/{id}")
    public Result<Void> deleteStudent(@PathVariable Long id) {
        log.info("DELETE /api/v1/admin/students/{}", id);
        adminService.deleteStudent(id);
        return Result.ok();
    }

    // ==================== 老师管理 ====================

    /**
     * 老师列表（含学生数/课时汇总）
     */
    @GetMapping("/teachers")
    public Result<AdminTeacherPageVO> listTeachers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {
        log.info("GET /api/v1/admin/teachers page={} pageSize={} keyword={}", page, pageSize, keyword);
        return Result.ok(adminService.listTeachers(page, pageSize, keyword));
    }

    /**
     * 老师详情 + 学生列表
     */
    @GetMapping("/teachers/{id}")
    public Result<AdminTeacherDetailVO> getTeacherDetail(@PathVariable Long id) {
        log.info("GET /api/v1/admin/teachers/{}", id);
        return Result.ok(adminService.getTeacherDetail(id));
    }

    /**
     * 新增老师（创建 User + Teacher）
     */
    @PostMapping("/teachers")
    public Result<AdminTeacherVO> createTeacher(@Valid @RequestBody AdminTeacherDTO dto) {
        log.info("POST /api/v1/admin/teachers body={}", dto);
        return Result.ok(adminService.createTeacher(dto));
    }

    /**
     * 更新老师信息
     */
    @PutMapping("/teachers/{userId}")
    public Result<AdminTeacherVO> updateTeacher(@PathVariable Long userId, @Valid @RequestBody AdminTeacherDTO dto) {
        log.info("PUT /api/v1/admin/teachers/{} body={}", userId, dto);
        return Result.ok(adminService.updateTeacher(userId, dto));
    }

    /**
     * 删除老师（级联删除 teacher_student / Teacher / User）
     */
    @DeleteMapping("/teachers/{userId}")
    public Result<Void> deleteTeacher(@PathVariable Long userId) {
        log.info("DELETE /api/v1/admin/teachers/{}", userId);
        adminService.deleteTeacher(userId);
        return Result.ok();
    }

    // ==================== 排课查看 ====================

    /**
     * 全部排课（可按 teacher_id / 日期范围过滤）
     */
    @GetMapping("/schedules")
    public Result<AdminScheduleVO> listSchedules(
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize) {
        log.info("GET /api/v1/admin/schedules teacherId={} year={} month={} page={} pageSize={}",
                teacherId, year, month, page, pageSize);
        return Result.ok(adminService.listSchedules(teacherId, year, month, page, pageSize));
    }

    // ==================== 概览统计 ====================

    /**
     * 概览统计（老师数 / 学生数 / 总课时等）
     */
    @GetMapping("/stats")
    public Result<AdminStatsVO> getStats() {
        log.info("GET /api/v1/admin/stats");
        return Result.ok(adminService.getStats());
    }

    // ==================== 系统设置 ====================

    /**
     * 获取当前系统配置
     */
    @GetMapping("/settings")
    public Result<Map<String, String>> getSettings() {
        log.info("GET /api/v1/admin/settings");
        return Result.ok(adminService.getSettings());
    }

    /**
     * 更新系统配置（AI模型等）
     */
    @PutMapping("/settings")
    public Result<Map<String, String>> updateSettings(@RequestBody AdminSettingsDTO dto) {
        log.info("PUT /api/v1/admin/settings body={}", dto);
        return Result.ok(adminService.updateSettings(dto));
    }
}