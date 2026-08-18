package com.eduai.system.service;

import com.eduai.system.dto.ExamPaperDTO;
import com.eduai.system.vo.ExamPaperVO;

import java.util.List;

/**
 * 试卷分析 Service
 */
public interface ExamPaperService {

    /** 上传试卷（含 AI 分析结果） */
    ExamPaperVO createExamPaper(ExamPaperDTO dto);

    /** 我的试卷列表（可选按学科筛选） */
    List<ExamPaperVO> listExamPapers(String subject);

    /** 试卷详情 */
    ExamPaperVO getExamPaperDetail(Long id);

    /** 删除试卷 */
    void deleteExamPaper(Long id);

    // ==================== 老师端 ====================

    /** 老师查看学生的试卷列表（可选按学生/学科筛选） */
    List<ExamPaperVO> listTeacherExamPapers(Long studentId, String subject);

    /** 老师查看试卷详情 */
    ExamPaperVO getTeacherExamPaperDetail(Long id);

    /** 老师编辑试卷（评分/分析/建议/知识点） */
    ExamPaperVO updateTeacherExamPaper(Long id, java.util.Map<String, Object> body);

    /** 老师删除试卷 */
    void deleteTeacherExamPaper(Long id);

    /** 学生填写分数 */
    ExamPaperVO updateStudentExamPaper(Long id, java.util.Map<String, Object> body);

    /** 导出试卷分析 PDF */
    byte[] generatePdf(Long id);

    /** 出卷 PDF 导出 */
    byte[] generateExamPaperPdf(java.util.Map<String, Object> body);
}
