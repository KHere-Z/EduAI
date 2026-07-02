package com.eduai.system.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 上课时间实体
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "student_session")
public class StudentSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联报名科目（双向映射，拥有方） */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private StudentEnrollment enrollment;

    /** 关联报名科目ID（只读，用于 Repository 查询） */
    @Column(name = "enrollment_id", insertable = false, updatable = false)
    private Long enrollmentId;

    /** 上课日期 */
    @Column(name = "class_date", nullable = false)
    private LocalDate classDate;

    /** 开始时间（小时） */
    @Column(name = "start_time", nullable = false, length = 10)
    private String startTime;

    /** 结束时间（小时） */
    @Column(name = "end_time", nullable = false, length = 10)
    private String endTime;
}