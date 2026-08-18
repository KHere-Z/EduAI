package com.eduai.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 学生打卡记录实体
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "student_checkin", uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "checkin_date"}))
public class StudentCheckin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 学生ID（→ students.id） */
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    /** 打卡日期 */
    @Column(name = "checkin_date", nullable = false)
    private LocalDate checkinDate;
}