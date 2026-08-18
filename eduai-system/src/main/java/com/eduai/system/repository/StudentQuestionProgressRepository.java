package com.eduai.system.repository;

import com.eduai.system.entity.StudentQuestionProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 学生题目掌握度 Repository（共享题按学生隔离）
 */
@Repository
public interface StudentQuestionProgressRepository extends JpaRepository<StudentQuestionProgress, Long> {

    Optional<StudentQuestionProgress> findByStudentIdAndQuestionId(Long studentId, Long questionId);

    List<StudentQuestionProgress> findByStudentIdAndQuestionIdIn(Long studentId, Collection<Long> questionIds);
}
