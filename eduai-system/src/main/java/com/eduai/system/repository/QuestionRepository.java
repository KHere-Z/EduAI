package com.eduai.system.repository;

import com.eduai.system.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 题库 Repository
 */
@Repository
public interface QuestionRepository extends JpaRepository<Question, Long>,
        JpaSpecificationExecutor<Question> {

    /**
     * 按学生ID和学科查询错题（学生端我的错题库）
     */
    Page<Question> findByStudentIdAndSubjectAndType(Long studentId, String subject, String type, Pageable pageable);

    /**
     * 按学生ID查询所有错题
     */
    Page<Question> findByStudentIdAndType(Long studentId, String type, Pageable pageable);

    /**
     * 按学生ID + 学科 + 年级查询待做题（新题，全校共享）
     */
    Page<Question> findByTypeAndSubjectAndGradeLevel(String type, String subject, String gradeLevel, Pageable pageable);

    /**
     * 按学生ID + 学科查询待做题（不按年级筛选）
     */
    Page<Question> findByTypeAndSubject(String type, String subject, Pageable pageable);

    /**
     * 按老师ID查询上传的题目
     */
    List<Question> findByTeacherId(Long teacherId);

    /** 按学生ID删除所有题目（管理员删学生时级联清理） */
    void deleteByStudentId(Long studentId);

    /** 查找仍存 base64 data URL 的题目（用于存量图片落盘迁移，见 QuestionImageMigrationRunner） */
    @Query("SELECT q FROM Question q WHERE q.originalImageUrl LIKE 'data:%' "
            + "OR q.diagramImageUrl LIKE 'data:%' OR q.teacherAnalysisImage LIKE 'data:%'")
    List<Question> findWithBase64Images();
}