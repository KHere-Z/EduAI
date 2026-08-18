package com.eduai.system.service;

import com.eduai.system.dto.HomeworkDTO;
import com.eduai.system.vo.HomeworkVO;
import com.eduai.system.vo.SubmissionVO;

import java.util.List;
import java.util.Map;

public interface HomeworkService {

    /** 批量获取作业提交（按学科，扁平结构） */
    List<Map<String, Object>> listAllSubmissions(String subject);

    // ==================== 老师端 ====================

    /** 布置作业 */
    HomeworkVO createHomework(HomeworkDTO dto);

    /** 作业列表 */
    List<HomeworkVO> listTeacherHomework(String subject);

    /** 删除作业 */
    void deleteHomework(Long id);

    /** 查看学生提交 */
    List<SubmissionVO> listSubmissions(Long homeworkId);

    /** 上传答案解析 */
    HomeworkVO uploadAnswer(Long homeworkId, Map<String, String> body);

    /** 批改作业 */
    SubmissionVO correctSubmission(Long homeworkId, Map<String, Object> body);

    // ==================== 学生端 ====================

    /** 学生作业列表 */
    List<HomeworkVO> listStudentHomework(String subject);

    /** 提交作业 */
    SubmissionVO submitHomework(Long homeworkId, Map<String, Object> body);
}
