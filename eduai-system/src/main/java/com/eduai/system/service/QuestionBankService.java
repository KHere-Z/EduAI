package com.eduai.system.service;

import com.eduai.system.dto.QuestionUpdateDTO;
import com.eduai.system.dto.QuestionUploadDTO;
import com.eduai.system.vo.GradeResultVO;
import com.eduai.system.vo.QuestionPageVO;
import com.eduai.system.vo.QuestionVO;
import com.eduai.system.vo.SaveAnswerVO;
import com.eduai.system.vo.StudentBriefVO;
import org.springframework.web.multipart.MultipartFile;

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

    /**
     * 老师查看指定学生的错题（分页，可选按学科筛选）。
     * <p>
     * 两步鉴权：① 当前登录人是老师（角色校验）；② 存在 {@code (teacherId=当前老师, studentId=参数)}
     * 的绑定关系。不满足返回 403（而非空列表，避免前端误判为「无数据」）；学生不存在返回 404。
     *
     * @param studentId 学生ID（students.id）
     */
    QuestionPageVO listTeacherStudentWrongQuestions(int page, int pageSize, String subject, Long studentId);

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

    /**
     * 题目 AI 批改（扣 5 智学点）。
     *
     * @param questionId 题目ID
     * @param file       学生作答图片（multipart，必填）
     * @param answerText 学生作答文字（可选）
     * @return 批改结果 {@code {correct, result}}；重复提交返回缓存且不重复扣点
     */
    GradeResultVO gradeQuestion(Long questionId, MultipartFile file, String answerText);

    /**
     * 保存学生答案（只保存、不批改、不扣点）。
     * <p>
     * 按 {@code (questionId, studentId)} 覆盖式保存最近一次作答图片/文字，供「不批改直接保存」
     * 及跨刷新恢复上次作答使用。
     *
     * @param questionId 题目ID
     * @param file       学生作答图片（multipart）
     * @param answerText 学生作答文字（可选）
     * @return 保存结果 {@code {questionId, answerImageUrl, answerText}}
     */
    SaveAnswerVO saveAnswer(Long questionId, MultipartFile file, String answerText);

    /**
     * 删除学生答案存档（只删 answer 存档，不涉及批改记录）。
     * <p>
     * 归属校验与保存一致：错题仅本人可删、共享新题所有学生可删。删除不存在的记录时静默成功（幂等）。
     */
    void deleteAnswer(Long questionId);
}