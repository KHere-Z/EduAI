package com.eduai.system.service;

import com.eduai.system.dto.QuestionUpdateDTO;
import com.eduai.system.dto.QuestionUploadDTO;
import com.eduai.system.vo.QuestionPageVO;
import com.eduai.system.vo.QuestionVO;
import com.eduai.system.vo.StudentBriefVO;

import java.util.List;
import java.util.Map;

/**
 * 题库 Service
 */
public interface QuestionBankService {

    // ==================== 老师端 ====================

    /**
     * 题库列表（7维筛选+分页）
     */
    QuestionPageVO listTeacherQuestions(int page, int pageSize, String subject,
                                        Long kpId, String type, Long studentId,
                                        String gradeLevel, String date,
                                        String questionType);

    /**
     * 题目详情
     */
    QuestionVO getQuestion(Long id);

    /**
     * 更新题目（知识点标签/老师解析/配图）
     */
    QuestionVO updateQuestion(Long id, QuestionUpdateDTO dto);

    /**
     * 老师上传新题（全校共享）
     */
    QuestionVO uploadQuestion(QuestionUploadDTO dto);

    /**
     * 删除题目
     */
    void deleteQuestion(Long id);

    /**
     * 获取当前老师名下指定学科的学生列表（用于错题筛选和上传学生选择）
     */
    List<StudentBriefVO> listTeacherStudents(String subject);

    // ==================== 学生端 ====================

    /**
     * 学生错题库
     */
    QuestionPageVO listStudentWrongQuestions(int page, int pageSize, String subject);

    /**
     * 学生待做题（新题）
     */
    QuestionPageVO listStudentNewQuestions(int page, int pageSize, String subject, String gradeLevel, String questionType);

    /**
     * 学生录入错题
     *
     * @param subject 学科
     * @param body    请求体（title/answer/knowledgePointNames/difficulty/gradeLevel/analysis/solution/errorType/diagramImageUrl）
     * @return 保存后的 QuestionVO
     */
    QuestionVO addWrongQuestion(String subject, Map<String, Object> body);

    /**
     * 学生更新掌握度和完成状态（仅 mastery + completed，不校验教师权限）
     */
    void updateMastery(Long questionId, Map<String, Object> body);

    /**
     * 按知识点获取同类题目（随机 N 道）
     *
     * @param subject 学科
     * @param kpId    知识点 ID
     * @param count   返回数量（默认3）
     * @return 随机打乱的同知识点题目列表
     */
    List<QuestionVO> listSimilarQuestions(String subject, Long kpId, int count, Long excludeId);
}