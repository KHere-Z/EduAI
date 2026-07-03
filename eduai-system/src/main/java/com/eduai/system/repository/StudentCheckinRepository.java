package com.eduai.system.repository;

import com.eduai.system.entity.StudentCheckin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 学生打卡 Repository
 */
@Repository
public interface StudentCheckinRepository extends JpaRepository<StudentCheckin, Long> {

    /** 查找某天的打卡 */
    Optional<StudentCheckin> findByStudentIdAndCheckinDate(Long studentId, LocalDate date);

    /** 查找某学生所有打卡（按日期降序） */
    List<StudentCheckin> findByStudentIdOrderByCheckinDateDesc(Long studentId);

    /** 统计打卡天数 */
    @Query("SELECT COUNT(c) FROM StudentCheckin c WHERE c.studentId = :studentId")
    long countByStudentId(@Param("studentId") Long studentId);
}