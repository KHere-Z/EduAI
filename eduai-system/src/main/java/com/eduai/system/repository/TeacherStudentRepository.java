package com.eduai.system.repository;

import com.eduai.system.entity.TeacherStudent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 老师-学生关系 Repository
 */
@Repository
public interface TeacherStudentRepository extends JpaRepository<TeacherStudent, Long>, JpaSpecificationExecutor<TeacherStudent> {

    /**
     * 按老师ID和学生ID查找（用于判断是否已存在关系）
     */
    Optional<TeacherStudent> findByTeacherIdAndStudentId(Long teacherId, Long studentId);

    /**
     * 按ID和老师ID查询（数据隔离）
     */
    Optional<TeacherStudent> findByIdAndTeacherId(Long id, Long teacherId);

    /**
     * 按学生ID查询所有老师关系（管理员用）
     */
    List<TeacherStudent> findByStudentId(Long studentId);

    /**
     * 按老师ID查询所有学生关系
     */
    List<TeacherStudent> findByTeacherId(Long teacherId);

    /**
     * 按老师ID统计学生数量
     */
    @Query("SELECT COUNT(ts) FROM TeacherStudent ts WHERE ts.teacherId = :teacherId")
    long countStudentsByTeacherId(@Param("teacherId") Long teacherId);

    /**
     * 按老师ID汇总剩余课时
     */
    @Query("SELECT COALESCE(SUM(ts.hoursLeft), 0) FROM TeacherStudent ts WHERE ts.teacherId = :teacherId")
    long sumHoursByTeacherId(@Param("teacherId") Long teacherId);

    /**
     * 统计总的关系数量
     */
    @Query("SELECT COUNT(ts) FROM TeacherStudent ts")
    long countTotal();

    /**
     * 汇总所有剩余课时
     */
    @Query("SELECT COALESCE(SUM(ts.hoursLeft), 0) FROM TeacherStudent ts")
    long sumTotalHours();
}