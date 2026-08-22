package com.eduai.system.repository;

import com.eduai.system.entity.StudentAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 学生答案存档 Repository（一题一学生一条，覆盖式更新）
 */
@Repository
public interface StudentAnswerRepository extends JpaRepository<StudentAnswer, Long> {

    Optional<StudentAnswer> findByQuestionIdAndStudentId(Long questionId, Long studentId);

    List<StudentAnswer> findByStudentIdAndQuestionIdIn(Long studentId, Collection<Long> questionIds);
}
