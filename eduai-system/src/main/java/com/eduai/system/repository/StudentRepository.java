package com.eduai.system.repository;

import com.eduai.system.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 学生 Repository（全局，不按老师过滤）
 */
@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    /**
     * 按姓名和学校查找已有学生（用于去重）
     */
    Optional<Student> findByNameAndSchool(String name, String school);
}