package com.eduai.system.controller;

import com.eduai.common.Result;
import com.eduai.system.dto.StudentDTO;
import com.eduai.system.service.StudentService;
import com.eduai.system.vo.CalendarVO;
import com.eduai.system.vo.StudentPageVO;
import com.eduai.system.vo.StudentVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 学生管理 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    /**
     * 分页查询学生列表
     */
    @GetMapping
    public Result<StudentPageVO> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String grade) {
        return Result.ok(studentService.list(page, pageSize, keyword, subject, grade));
    }

    /**
     * 查询单个学生详情
     */
    @GetMapping("/{id}")
    public Result<StudentVO> getById(@PathVariable Long id) {
        return Result.ok(studentService.getById(id));
    }

    /**
     * 新增学生
     */
    @PostMapping
    public Result<StudentVO> create(@Valid @RequestBody StudentDTO dto) {
        try {
            log.info("POST /api/v1/students 收到请求: {}", dto);
            Result<StudentVO> result = Result.ok(studentService.create(dto));
            log.info("POST /api/v1/students 成功返回: {}", result);
            return result;
        } catch (Exception e) {
            log.error("POST /api/v1/students 异常，请求体={}", dto, e);
            throw e;
        }
    }

    /**
     * 更新学生信息
     */
    @PutMapping("/{id}")
    public Result<StudentVO> update(@PathVariable Long id, @Valid @RequestBody StudentDTO dto) {
        return Result.ok(studentService.update(id, dto));
    }

    /**
     * 删除学生
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        studentService.delete(id);
        return Result.ok();
    }

    /**
     * 调整剩余课时
     */
    @PatchMapping("/{id}/hours")
    public Result<Void> updateHours(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer delta = body.get("delta");
        if (delta == null) {
            return Result.fail("delta 参数不能为空");
        }
        studentService.updateHours(id, delta);
        return Result.ok();
    }

    /**
     * 日历排课查询
     */
    @GetMapping("/calendar")
    public Result<CalendarVO> calendar(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) Long teacherId) {
        return Result.ok(studentService.calendar(year, month, teacherId));
    }
}