package com.eduai.system.controller;

import com.eduai.common.Result;
import com.eduai.system.service.HomeworkService;
import com.eduai.system.vo.HomeworkVO;
import com.eduai.system.vo.SubmissionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/student/homework")
@RequiredArgsConstructor
public class StudentHomeworkController {

    private final HomeworkService homeworkService;

    /** 作业列表（含提交状态） */
    @GetMapping
    public Result<List<HomeworkVO>> list(@RequestParam(required = false) String subject) {
        return Result.ok(homeworkService.listStudentHomework(subject));
    }

    /** 提交作业 */
    @PostMapping("/{id}/submit")
    public Result<SubmissionVO> submit(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        log.info("POST /api/v1/student/homework/{}/submit", id);
        return Result.ok(homeworkService.submitHomework(id, body));
    }
}
