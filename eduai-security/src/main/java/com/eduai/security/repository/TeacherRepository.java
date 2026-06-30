package com.eduai.security.repository;

import com.eduai.security.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 教师信息 Repository
 */
@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    /** 根据用户ID查询教师信息 */
    Optional<Teacher> findByUserId(Long userId);
}