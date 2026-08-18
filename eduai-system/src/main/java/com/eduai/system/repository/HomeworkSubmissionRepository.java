package com.eduai.system.repository;

import com.eduai.system.entity.HomeworkSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HomeworkSubmissionRepository extends JpaRepository<HomeworkSubmission, Long> {

    List<HomeworkSubmission> findByHomeworkId(Long homeworkId);

    Optional<HomeworkSubmission> findByHomeworkIdAndStudentId(Long homeworkId, Long studentId);

    List<HomeworkSubmission> findByStudentId(Long studentId);

    void deleteByHomeworkId(Long homeworkId);
}
