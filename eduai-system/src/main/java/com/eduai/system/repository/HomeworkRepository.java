package com.eduai.system.repository;

import com.eduai.system.entity.Homework;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HomeworkRepository extends JpaRepository<Homework, Long> {

    List<Homework> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);

    List<Homework> findByTeacherIdAndSubjectOrderByCreatedAtDesc(Long teacherId, String subject);

    List<Homework> findBySubjectOrderByCreatedAtDesc(String subject);

    List<Homework> findByTeacherIdInOrderByCreatedAtDesc(List<Long> teacherIds);

    List<Homework> findByTeacherIdInAndSubjectOrderByCreatedAtDesc(List<Long> teacherIds, String subject);
}
