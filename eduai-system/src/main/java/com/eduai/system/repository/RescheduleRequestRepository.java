package com.eduai.system.repository;

import com.eduai.system.entity.RescheduleRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 调课申请 Repository
 */
@Repository
public interface RescheduleRequestRepository extends JpaRepository<RescheduleRequest, Long> {

    /** 按老师ID查询所有调课申请 */
    List<RescheduleRequest> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);

    /** 按学生ID查询 */
    List<RescheduleRequest> findByStudentIdOrderByCreatedAtDesc(Long studentId);
}