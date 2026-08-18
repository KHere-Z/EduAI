package com.eduai.system.controller;

import com.eduai.common.Result;
import com.eduai.system.dto.ExamPaperDTO;
import com.eduai.system.service.ExamPaperService;
import com.eduai.system.vo.ExamPaperVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 学生端 — 试卷分析 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/student/exam-papers")
@RequiredArgsConstructor
public class StudentExamPaperController {

    private final ExamPaperService examPaperService;

    /** 上传试卷（含 AI 分析结果） */
    @PostMapping
    public Result<ExamPaperVO> createExamPaper(@Valid @RequestBody ExamPaperDTO dto) {
        log.info("POST /api/v1/student/exam-papers subject={}, examType={}",
                dto.getSubject(), dto.getExamType());
        return Result.ok(examPaperService.createExamPaper(dto));
    }

    /** 我的试卷列表（可选按学科筛选） */
    @GetMapping
    public Result<List<ExamPaperVO>> listExamPapers(
            @RequestParam(required = false) String subject) {
        return Result.ok(examPaperService.listExamPapers(subject));
    }

    /** 试卷详情 */
    @GetMapping("/{id}")
    public Result<ExamPaperVO> getExamPaperDetail(@PathVariable Long id) {
        return Result.ok(examPaperService.getExamPaperDetail(id));
    }

    /** 删除试卷 */
    @DeleteMapping("/{id}")
    public Result<Void> deleteExamPaper(@PathVariable Long id) {
        examPaperService.deleteExamPaper(id);
        return Result.ok();
    }

    /** 导出试卷分析 PDF */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long id) {
        byte[] pdf = examPaperService.generatePdf(id);
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition",
                        "attachment; filename*=UTF-8''%E8%AF%95%E5%8D%B7%E5%88%86%E6%9E%90%E6%8A%A5%E5%91%8A.pdf")
                .body(pdf);
    }

    /** 填写分数 */
    @PutMapping("/{id}")
    public Result<ExamPaperVO> updateScore(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return Result.ok(examPaperService.updateStudentExamPaper(id, body));
    }
}
