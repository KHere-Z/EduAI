package com.eduai.system.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 学生报名科目实体（关联 teacher_student 而非 student）
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "student_enrollment")
public class StudentEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联的老师-学生关系 */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_student_id", nullable = false)
    private TeacherStudent teacherStudent;

    /** teacher_student_id（只读，用于 Repository 查询） */
    @Column(name = "teacher_student_id", insertable = false, updatable = false)
    private Long teacherStudentId;

    /** 科目 */
    @Column(nullable = false, length = 50)
    private String subject;

    /** 创建时间 */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** 上课时间列表（双向映射） */
    @OneToMany(mappedBy = "enrollment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<StudentSession> sessions = new ArrayList<>();

    /** 添加上课时间（维护双向关系） */
    public void addSession(StudentSession session) {
        sessions.add(session);
        session.setEnrollment(this);
    }

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.sessions == null) {
            this.sessions = new ArrayList<>();
        }
    }
}