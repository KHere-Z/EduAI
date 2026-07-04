package com.eduai.system.controller;

import com.eduai.common.Result;
import com.eduai.system.service.QuestionBankService;
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
}