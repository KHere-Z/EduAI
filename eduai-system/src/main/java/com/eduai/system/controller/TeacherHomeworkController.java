package com.eduai.system.controller;

import com.eduai.common.Result;
import com.eduai.system.dto.HomeworkDTO;
import com.eduai.system.service.HomeworkService;
import com.eduai.system.vo.HomeworkVO;
import com.eduai.system.vo.SubmissionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/teacher/homework")
@RequiredArgsConstructor
public class TeacherHomeworkController {

    private final HomeworkService homeworkService;

    /** 布置作业 */
    @PostMapping
    public Result<HomeworkVO> create(@Valid @RequestBody HomeworkDTO dto) {
        log.info("POST /api/v1/teacher/homework subject={}", dto.getSubject());
        return Result.ok(homeworkService.createHomework(dto));
    }

    /** 批量获取所有作业的提交（扁平结构） */
    @GetMapping("/submissions")
    public Result<List<Map<String, Object>>> allSubmissions(@RequestParam(required = false) String subject) {
        return Result.ok(homeworkService.listAllSubmissions(subject));
    }

    /** 作业列表 */
    @GetMapping
    public Result<List<HomeworkVO>> list(@RequestParam(required = false) String subject) {
        return Result.ok(homeworkService.listTeacherHomework(subject));
    }

    /** 删除作业 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        homeworkService.deleteHomework(id);
        return Result.ok();
    }

    /** 查看学生提交 */
    @GetMapping("/{id}/submissions")
    public Result<List<SubmissionVO>> submissions(@PathVariable Long id) {
        return Result.ok(homeworkService.listSubmissions(id));
    }

    /** 批改作业 */
    @PostMapping("/{id}/correct")
    public Result<SubmissionVO> correct(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        log.info("POST /api/v1/teacher/homework/{}/correct studentId={}", id, body.get("studentId"));
        return Result.ok(homeworkService.correctSubmission(id, body));
    }

    /** 上传答案解析 */
    @PostMapping("/{id}/answer")
    public Result<HomeworkVO> answer(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.ok(homeworkService.uploadAnswer(id, body));
    }
}
