package com.eduai.system.controller;

import com.eduai.common.Result;
import com.eduai.system.dto.QuestionUpdateDTO;
import com.eduai.system.dto.QuestionUploadDTO;
import com.eduai.system.service.QuestionBankService;
import com.eduai.system.vo.QuestionPageVO;
import com.eduai.system.vo.QuestionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 题库 Controller（老师端管理）
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/teacher/questions")
@RequiredArgsConstructor
public class TeacherQuestionController {

    private final QuestionBankService questionBankService;

    /**
     * 题库列表（7维筛选 + 分页，默认每页15条）
     * <p>
     * 查询参数：subject(必填), kpId, type, studentId, gradeLevel, date, page, pageSize
     */
    @GetMapping
    public Result<QuestionPageVO> list(
            @RequestParam String subject,
            @RequestParam(required = false) Long kpId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) String gradeLevel,
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int pageSize) {
        return Result.ok(questionBankService.listTeacherQuestions(
                page, pageSize, subject, kpId, type, studentId, gradeLevel, date));
    }

    /**
     * 题目详情（含全部字段：原图/配图/AI解析/老师解析）
     */
    @GetMapping("/{id}")
    public Result<QuestionVO> getById(@PathVariable Long id) {
        return Result.ok(questionBankService.getQuestion(id));
    }

    /**
     * 更新题目（知识点标签/老师解析/配图等，只传需更新的字段）
     */
    @PutMapping("/{id}")
    public Result<QuestionVO> update(@PathVariable Long id, @Valid @RequestBody QuestionUpdateDTO dto) {
        log.info("PUT /api/v1/teacher/questions/{} body={}", id, dto);
        return Result.ok(questionBankService.updateQuestion(id, dto));
    }

    /**
     * 老师上传新题（全校共享）
     */
    @PostMapping("/upload")
    public Result<QuestionVO> upload(@Valid @RequestBody QuestionUploadDTO dto) {
        log.info("POST /api/v1/teacher/questions/upload body: subject={}, title={}",
                dto.getSubject(), dto.getTitle());
        return Result.ok(questionBankService.uploadQuestion(dto));
    }

    /**
     * 删除题目
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        log.info("DELETE /api/v1/teacher/questions/{}", id);
        questionBankService.deleteQuestion(id);
        return Result.ok();
    }
}