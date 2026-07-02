package com.eduai.system.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 老师-学生关系实体
 * <p>
 * 解决"同一学生被多个老师添加"和"同一老师对同一学生报多科"的问题。
 * 每个 teacher_student 记录代表一个老师收了一个学生，下面可以有多个科目的报名。
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "teacher_student")
public class TeacherStudent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 老师ID */
    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    /** 学生ID */
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    /** 关联的学生（只读，用于 JOIN 查询和展示） */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", insertable = false, updatable = false)
    private Student student;

    /** 剩余课时 */
    @Column(name = "hours_left")
    @Builder.Default
    private Integer hoursLeft = 0;

    /** 报名时间 */
    @Column(name = "reg_date")
    private LocalDate regDate;

    /** 创建时间 */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** 报名科目列表（级联持久化/删除） */
    @OneToMany(mappedBy = "teacherStudent", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<StudentEnrollment> enrollments = new ArrayList<>();

    /** 添加报名科目（维护双向关系） */
    public void addEnrollment(StudentEnrollment enrollment) {
        enrollments.add(enrollment);
        enrollment.setTeacherStudent(this);
    }

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.hoursLeft == null) {
            this.hoursLeft = 0;
        }
        if (this.enrollments == null) {
            this.enrollments = new ArrayList<>();
        }
    }
}