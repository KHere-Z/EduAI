package com.eduai.system.repository;

import com.eduai.system.entity.QuestionGradeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 题目 AI 批改记录 Repository（一题一学生一次，作幂等键）
 */
@Repository
public interface QuestionGradeRecordRepository extends JpaRepository<QuestionGradeRecord, Long> {

    Optional<QuestionGradeRecord> findByQuestionIdAndStudentId(Long questionId, Long studentId);
}
