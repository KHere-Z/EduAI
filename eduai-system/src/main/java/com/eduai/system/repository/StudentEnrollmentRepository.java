package com.eduai.system.repository;

import com.eduai.system.entity.StudentEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 学生报名科目 Repository
 */
@Repository
public interface StudentEnrollmentRepository extends JpaRepository<StudentEnrollment, Long> {

    /**
     * 按老师-学生关系ID查询所有报名科目
     */
    List<StudentEnrollment> findByTeacherStudentId(Long teacherStudentId);

    /**
     * 统计总报名数
     */
    @Query("SELECT COUNT(e) FROM StudentEnrollment e")
    long countTotal();
}