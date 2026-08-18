package com.eduai.system.repository;

import com.eduai.system.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 学习反馈 Repository
 */
@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long>,
        JpaSpecificationExecutor<Feedback> {

    List<Feedback> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);

    List<Feedback> findByStudentIdOrderByCreatedAtDesc(Long studentId);
}
