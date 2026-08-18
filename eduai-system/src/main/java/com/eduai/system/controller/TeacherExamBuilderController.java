package com.eduai.system.controller;

import com.eduai.system.service.ExamPaperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 老师端 — 出卷 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/teacher/exam-builder")
@RequiredArgsConstructor
public class TeacherExamBuilderController {

    private final ExamPaperService examPaperService;

    /** 导出 PDF 试卷 */
    @PostMapping("/export-pdf")
    public ResponseEntity<byte[]> exportPdf(@RequestBody Map<String, Object> body) {
        log.info("POST /api/v1/teacher/exam-builder/export-pdf title={}",
                body.getOrDefault("title", ""));
        byte[] pdf = examPaperService.generateExamPaperPdf(body);
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition",
                        "attachment; filename*=UTF-8''%E6%95%B0%E5%AD%A6%E8%AF%95%E5%8D%B7.pdf")
                .body(pdf);
    }
}
