package com.eduai.system.repository;

import com.eduai.system.entity.StudentSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 上课时间 Repository
 */
@Repository
public interface StudentSessionRepository extends JpaRepository<StudentSession, Long> {

    /**
     * 按老师ID和日期范围查询排课（日历用）
     * student_session → student_enrollment → teacher_student → students
     */
    @Query("""
        SELECT s FROM StudentSession s
        JOIN StudentEnrollment e ON s.enrollmentId = e.id
        JOIN TeacherStudent ts ON e.teacherStudentId = ts.id
        JOIN Student st ON ts.studentId = st.id
        WHERE ts.teacherId = :teacherId
        AND s.classDate BETWEEN :start AND :end
        ORDER BY s.classDate, s.startTime
        """)
    List<StudentSession> findSessionsByTeacherAndDateRange(
            @Param("teacherId") Long teacherId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    /**
     * 管理员：查询全部排课（可按日期范围过滤，不限制老师）
     */
    @Query("""
        SELECT s FROM StudentSession s
        JOIN StudentEnrollment e ON s.enrollmentId = e.id
        JOIN TeacherStudent ts ON e.teacherStudentId = ts.id
        JOIN Student st ON ts.studentId = st.id
        WHERE s.classDate BETWEEN :start AND :end
        ORDER BY s.classDate, s.startTime
        """)
    List<StudentSession> findAllSessionsByDateRange(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    /**
     * 管理员：按老师ID和日期范围查询排课
     */
    @Query("""
        SELECT s FROM StudentSession s
        JOIN StudentEnrollment e ON s.enrollmentId = e.id
        JOIN TeacherStudent ts ON e.teacherStudentId = ts.id
        JOIN Student st ON ts.studentId = st.id
        WHERE ts.teacherId = :teacherId
        AND s.classDate BETWEEN :start AND :end
        ORDER BY s.classDate, s.startTime
        """)
    List<StudentSession> findAllSessionsByTeacherAndDateRange(
            @Param("teacherId") Long teacherId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    /**
     * 统计总排课数
     */
    @Query("SELECT COUNT(s) FROM StudentSession s")
    long countTotal();

    /**
     * 学生端：按学生ID和日期范围查询排课
     */
    @Query("""
        SELECT s FROM StudentSession s
        JOIN StudentEnrollment e ON s.enrollmentId = e.id
        JOIN TeacherStudent ts ON e.teacherStudentId = ts.id
        WHERE ts.studentId = :studentId
        AND s.classDate BETWEEN :start AND :end
        ORDER BY s.classDate, s.startTime
        """)
    List<StudentSession> findSessionsByStudentAndDateRange(
            @Param("studentId") Long studentId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    /**
     * 管理员：查询全部排课（分页，不过滤日期）
     */
    @Query(value = """
        SELECT s FROM StudentSession s
        JOIN StudentEnrollment e ON s.enrollmentId = e.id
        JOIN TeacherStudent ts ON e.teacherStudentId = ts.id
        JOIN Student st ON ts.studentId = st.id
        ORDER BY s.classDate DESC, s.startTime
        """)
    List<StudentSession> findAllSessionsWithDetails();
}