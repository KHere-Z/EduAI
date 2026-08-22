package com.eduai.system.controller;

import com.eduai.common.Result;
import com.eduai.system.service.QuestionBankService;
import com.eduai.system.vo.QuestionPageVO;
import com.eduai.system.vo.StudentBriefVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 老师端通用 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/teacher")
@RequiredArgsConstructor
public class TeacherController {

    private final QuestionBankService questionBankService;

    /**
     * 获取当前老师名下指定学科的学生列表（用于错题筛选下拉和上传题目学生选择）
     */
    @GetMapping("/math-students")
    public Result<List<StudentBriefVO>> listMathStudents() {
        // student_enrollment.subject 存的是中文（"数学"），不是英文编码
        return Result.ok(questionBankService.listTeacherStudents("数学"));
    }

    /**
     * 老师查看指定学生的错题（分页，可选按学科筛选）
     * <p>
     * GET /api/v1/teacher/students/{studentId}/wrong-questions?subject=&page=&pageSize=
     * <p>
     * 后端两步鉴权：① 老师角色；② teacherId↔studentId 绑定关系。未绑定返回 403，学生不存在返回 404。
     */
    @GetMapping("/students/{studentId}/wrong-questions")
    public Result<QuestionPageVO> listStudentWrongQuestions(
            @PathVariable Long studentId,
            @RequestParam(required = false) String subject,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int pageSize) {
        log.info("GET /api/v1/teacher/students/{}/wrong-questions subject={} page={}", studentId, subject, page);
        return Result.ok(questionBankService.listTeacherStudentWrongQuestions(page, pageSize, subject, studentId));
    }
}