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
        // 学生绑定关系以 teacher_student 表为准（管理员内定 + uid 账号绑定两套流程都写它）。
        // 学科信息现存在 students.subjects(英文JSON)/teachers.subject_ids(英文逗号分隔)，
        // 而 student_enrollment 是旧版 StudentService 才维护的孤儿表，此处不再按学科过滤，
        // 否则两套绑定流程下该下拉都会恒空。
        return Result.ok(questionBankService.listTeacherStudents(null));
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