package com.eduai.system.repository;

import com.eduai.system.entity.ExamPaper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 试卷 Repository
 */
@Repository
public interface ExamPaperRepository extends JpaRepository<ExamPaper, Long>, JpaSpecificationExecutor<ExamPaper> {

    /**
     * 按学生ID查询所有试卷（最新在前）
     */
    List<ExamPaper> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    /**
     * 按学生ID和学科查询试卷（最新在前）
     */
    List<ExamPaper> findByStudentIdAndSubjectOrderByCreatedAtDesc(Long studentId, String subject);

    /**
     * 按ID和学生ID查询（所有权验证）
     */
    Optional<ExamPaper> findByIdAndStudentId(Long id, Long studentId);

    /**
     * 统计学生的试卷总数
     */
    long countByStudentId(Long studentId);

    /**
     * 按学生ID列表查询试卷（老师端查看多个学生的试卷）
     */
    List<ExamPaper> findByStudentIdInOrderByCreatedAtDesc(List<Long> studentIds);

    /**
     * 按学生ID列表+学科查询试卷
     */
    List<ExamPaper> findByStudentIdInAndSubjectOrderByCreatedAtDesc(List<Long> studentIds, String subject);
}
