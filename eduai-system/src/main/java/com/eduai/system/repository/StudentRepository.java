package com.eduai.system.repository;

import com.eduai.system.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 学生 Repository（全局，不按老师过滤）
 */
@Repository
public interface StudentRepository extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {

    /**
     * 按姓名和学校查找已有学生（用于去重）
     */
    Optional<Student> findByNameAndSchool(String name, String school);

    /**
     * 按 userId 查找学生（学生端登录用）
     */
    Optional<Student> findByUserId(Long userId);

    /**
     * 统计学生总数
     */
    @Query("SELECT COUNT(s) FROM Student s")
    long countTotal();
}