package com.eduai.system.controller;

import com.eduai.common.Result;
import com.eduai.system.service.QuestionBankService;
import com.eduai.system.vo.GradeResultVO;
import com.eduai.system.vo.QuestionPageVO;
import com.eduai.system.vo.QuestionVO;
import com.eduai.system.vo.SaveAnswerVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

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
            @RequestParam(required = false) String questionType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int pageSize) {
        return Result.ok(questionBankService.listStudentNewQuestions(page, pageSize, subject, gradeLevel, questionType));
    }

    /**
     * 按知识点获取同类题目（随机 N 道）
     * <p>
     * GET /api/v1/student/questions/similar?subject=math&kpId=5&count=3
     */
    @GetMapping("/similar")
    public Result<List<QuestionVO>> listSimilar(
            @RequestParam String subject,
            @RequestParam Long kpId,
            @RequestParam(defaultValue = "3") int count,
            @RequestParam(required = false) Long excludeId) {
        return Result.ok(questionBankService.listSimilarQuestions(subject, kpId, count, excludeId));
    }

    /**
     * 学生更新掌握度和完成状态（仅 mastery + completed）
     * <p>
     * PUT /api/v1/student/questions/{id}/mastery
     */
    @PutMapping("/{id}/mastery")
    public Result<Void> updateMastery(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        log.info("PUT /api/v1/student/questions/{}/mastery body={}", id, body);
        questionBankService.updateMastery(id, body);
        return Result.ok();
    }

    /**
     * 题目 AI 批改（扣 5 智学点）
     * <p>
     * POST /api/v1/student/questions/{questionId}/grade
     * multipart: file=答题图片（必填）, answerText=作答文字（可选）
     */
    @PostMapping("/{questionId}/grade")
    public Result<GradeResultVO> grade(@PathVariable Long questionId,
                                       @RequestParam("file") MultipartFile file,
                                       @RequestParam(required = false) String answerText) {
        log.info("POST /api/v1/student/questions/{}/grade answerText={}", questionId,
                answerText != null && answerText.length() > 50 ? answerText.substring(0, 50) + "..." : answerText);
        return Result.ok(questionBankService.gradeQuestion(questionId, file, answerText));
    }

    /**
     * 保存答案（只保存、不批改、不扣点）
     * <p>
     * POST /api/v1/student/questions/{questionId}/answer
     * multipart: file=答题图片（必填）, answerText=作答文字（可选）
     */
    @PostMapping("/{questionId}/answer")
    public Result<SaveAnswerVO> saveAnswer(@PathVariable Long questionId,
                                           @RequestParam("file") MultipartFile file,
                                           @RequestParam(required = false) String answerText) {
        log.info("POST /api/v1/student/questions/{}/answer answerText={}", questionId,
                answerText != null && answerText.length() > 50 ? answerText.substring(0, 50) + "..." : answerText);
        return Result.ok(questionBankService.saveAnswer(questionId, file, answerText));
    }

    /**
     * 删除学生答案存档（不批改、不扣点，删除持久化，跨刷新生效）
     * <p>
     * DELETE /api/v1/student/questions/{questionId}/answer
     */
    @DeleteMapping("/{questionId}/answer")
    public Result<Void> deleteAnswer(@PathVariable Long questionId) {
        log.info("DELETE /api/v1/student/questions/{}/answer", questionId);
        questionBankService.deleteAnswer(questionId);
        return Result.ok();
    }
}