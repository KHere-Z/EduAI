package com.eduai.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 调课申请实体
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "reschedule_request")
public class RescheduleRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联的上课记录ID */
    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    /** 申请人（学生ID → students.id） */
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    /** 所属老师ID */
    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    /** 原上课日期 */
    @Column(name = "original_date")
    private LocalDate originalDate;

    /** 原开始时间 */
    @Column(name = "original_start", length = 10)
    private String originalStart;

    /** 原结束时间 */
    @Column(name = "original_end", length = 10)
    private String originalEnd;

    /** 申请调至日期 */
    @Column(name = "requested_date")
    private LocalDate requestedDate;

    /** 申请调至开始时间 */
    @Column(name = "requested_start", length = 10)
    private String requestedStart;

    /** 申请调至结束时间 */
    @Column(name = "requested_end", length = 10)
    private String requestedEnd;

    /** 调课原因 */
    @Column(columnDefinition = "TEXT")
    private String reason;

    /** 状态：pending=待审批 approved=已批准 deferred=待议 closed=已关闭 */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "pending";

    /** 科目 */
    @Column(length = 50)
    private String subject;

    /** 创建时间 */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = "pending";
    }
}