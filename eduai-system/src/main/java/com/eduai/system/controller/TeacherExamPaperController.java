package com.eduai.system.controller;

import com.eduai.common.Result;
import com.eduai.system.service.ExamPaperService;
import com.eduai.system.vo.ExamPaperVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 老师端 — 试卷查看 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/teacher/exam-papers")
@RequiredArgsConstructor
public class TeacherExamPaperController {

    private final ExamPaperService examPaperService;

    /** 老师查看学生试卷列表（可选按学生/学科筛选） */
    @GetMapping
    public Result<List<ExamPaperVO>> list(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) String subject) {
        return Result.ok(examPaperService.listTeacherExamPapers(studentId, subject));
    }

    /** 老师查看试卷详情 */
    @GetMapping("/{id}")
    public Result<ExamPaperVO> detail(@PathVariable Long id) {
        return Result.ok(examPaperService.getTeacherExamPaperDetail(id));
    }

    /** 老师编辑试卷（评分/分析/建议/知识点） */
    @PutMapping("/{id}")
    public Result<ExamPaperVO> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return Result.ok(examPaperService.updateTeacherExamPaper(id, body));
    }

    /** 老师删除试卷 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        examPaperService.deleteTeacherExamPaper(id);
        return Result.ok();
    }
}
