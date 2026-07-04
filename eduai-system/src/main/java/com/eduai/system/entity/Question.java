package com.eduai.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 题库实体（老师上传新题全校共享，学生错题个人专属）
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "question_bank")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 学科 */
    @Column(nullable = false, length = 20)
    private String subject;

    /** 类型：WRONG=错题 / NEW=新题 */
    @Column(nullable = false, length = 10)
    private String type;

    /** 来源：STUDENT / TEACHER */
    @Column(nullable = false, length = 10)
    private String source;

    /** 错题专属学生ID → students.id */
    @Column(name = "student_id")
    private Long studentId;

    /** 新题上传老师ID → users.id */
    @Column(name = "teacher_id")
    private Long teacherId;

    // ==================== 图片相关 ====================

    /** 原上传图片URL */
    @Column(name = "original_image_url", length = 500)
    private String originalImageUrl;

    /** 配图（AI截取/老师手动上传/画图） */
    @Column(name = "diagram_image_url", length = 500)
    private String diagramImageUrl;

    /** 配图状态：NONE / AUTO / MANUAL */
    @Column(name = "diagram_status", length = 10)
    private String diagramStatus;

    // ==================== AI 相关 ====================

    /** AI从图片识别出的原始文字 */
    @Column(name = "ai_extracted_text", columnDefinition = "TEXT")
    private String aiExtractedText;

    /** 题目文字 */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;

    /** 关联知识点ID（逗号分隔，如 "3,7,12"） */
    @Column(name = "knowledge_point_ids", length = 500)
    private String knowledgePointIds;

    // ==================== 答案与解析 ====================

    /** 正确答案 */
    @Column(columnDefinition = "TEXT")
    private String answer;

    /** AI错因分析 */
    @Column(columnDefinition = "TEXT")
    private String analysis;

    /** AI解题步骤 */
    @Column(columnDefinition = "TEXT")
    private String solution;

    /** 举一反三题目（JSON） */
    @Column(name = "similar_json", columnDefinition = "JSON")
    private String similarJson;

    // ==================== 老师解析 ====================

    /** 老师文字解析 */
    @Column(name = "teacher_analysis", columnDefinition = "TEXT")
    private String teacherAnalysis;

    /** 老师解析配图（PNG/JPG本地文件路径） */
    @Column(name = "teacher_analysis_image", length = 500)
    private String teacherAnalysisImage;

    /** 文件类型（image/png 或 image/jpeg） */
    @Column(name = "teacher_analysis_image_type", length = 20)
    private String teacherAnalysisImageType;

    // ==================== 分类 ====================

    /** 难度：EASY / MEDIUM / HARD */
    @Column(length = 10)
    private String difficulty;

    /** 掌握度：UNMASTERED / FAMILIAR / MASTERED */
    @Column(length = 20)
    private String mastery;

    /** 错误类型（错题） */
    @Column(name = "error_type", length = 50)
    private String errorType;

    /** 年级·学期 */
    @Column(name = "grade_level", length = 50)
    private String gradeLevel;

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
        if (this.diagramStatus == null) {
            this.diagramStatus = "NONE";
        }
        if (this.difficulty == null) {
            this.difficulty = "MEDIUM";
        }
        if (this.mastery == null) {
            this.mastery = "UNMASTERED";
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}