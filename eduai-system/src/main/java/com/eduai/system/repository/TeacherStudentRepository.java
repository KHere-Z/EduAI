package com.eduai.system.repository;

import com.eduai.system.entity.TeacherStudent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

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
}