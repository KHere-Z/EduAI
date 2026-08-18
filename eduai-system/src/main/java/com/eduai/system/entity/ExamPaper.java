package com.eduai.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 试卷分析实体（学生上传试卷 + AI 分析结果）
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "exam_papers")
public class ExamPaper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 学生ID → students.id */
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    /** 学科 */
    @Column(length = 20)
    private String subject;

    /** 考试类型：月考/期中/期末/模拟考 */
    @Column(name = "exam_type", length = 20)
    private String examType;

    /** 学校 */
    @Column(length = 100)
    private String school;

    /** 原卷图片URL（逗号分隔） */
    @Column(name = "paper_images", columnDefinition = "TEXT")
    private String paperImages;

    /** AI识别的错题（JSON） */
    @Column(name = "wrong_questions", columnDefinition = "TEXT")
    private String wrongQuestions;

    /** AI试卷分析 */
    @Column(name = "paper_analysis", columnDefinition = "TEXT")
    private String paperAnalysis;

    /** 提分建议 */
    @Column(columnDefinition = "TEXT")
    private String suggestions;

    /** 成绩 */
    @Column(name = "score", columnDefinition = "DECIMAL(5,1)")
    private Double score;

    /** 知识点清单 */
    @Column(name = "kp_list", columnDefinition = "TEXT")
    private String kpList;

    /** 创建时间 */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
