package com.eduai.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 老师收益流水（资源下载分成）
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "teacher_earnings")
public class TeacherEarning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 老师用户ID (users.id) */
    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    /** 资源ID (resource_file.id) */
    @Column(name = "resource_id")
    private Long resourceId;

    /** 资源标题（快照） */
    @Column(name = "resource_title", length = 255)
    private String resourceTitle;

    /** 下载学生用户ID */
    @Column(name = "student_id")
    private Long studentId;

    /** 分成金额（单位：分；100智学点=1元，老师按50%分成） */
    @Column(nullable = false)
    private Long amount;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
