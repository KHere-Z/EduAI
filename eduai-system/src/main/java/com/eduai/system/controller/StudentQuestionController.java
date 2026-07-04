package com.eduai.system.controller;

import com.eduai.common.Result;
import com.eduai.system.service.QuestionBankService;
import com.eduai.system.vo.QuestionPageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 题库 Controller（学生端）
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/student/questions")
@RequiredArgsConstructor
public class StudentQuestionController {

    private final QuestionBankService questionBankService;

    /**
     * 我的错题库
     */
    @GetMapping("/wrong")
    public Result<QuestionPageVO> listWrong(
            @RequestParam(required = false) String subject,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int pageSize) {
        return Result.ok(questionBankService.listStudentWrongQuestions(page, pageSize, subject));
    }

    /**
     * 待做题（全校老师上传的新题，可按年级筛选）
     */
    @GetMapping("/new")
    public Result<QuestionPageVO> listNew(
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String gradeLevel,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int pageSize) {
        return Result.ok(questionBankService.listStudentNewQuestions(page, pageSize, subject, gradeLevel));
    }
}