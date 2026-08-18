package com.eduai.system.repository;

import com.eduai.system.entity.TeacherEarning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeacherEarningRepository extends JpaRepository<TeacherEarning, Long> {

    List<TeacherEarning> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);

    /** 老师累计分成总额（分） */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM TeacherEarning e WHERE e.teacherId = :teacherId")
    long sumAmountByTeacherId(@Param("teacherId") Long teacherId);
}
