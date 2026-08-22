package com.eduai.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 学生答案存档（只保存、不批改、不扣点）。
 * <p>
 * 前端「💾 不批改，直接保存」走此表：按 {@code (question_id, student_id)} 唯一、覆盖式保存，
 * 只留最近一次作答图片/文字。与 AI 批改记录 {@link QuestionGradeRecord} 分离——后者才携带批改结论
 * {@code correct}/{@code result} 并承担幂等扣点，两者互不影响。
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "student_answers",
        uniqueConstraints = @UniqueConstraint(name = "uk_student_answer_question",
                columnNames = {"question_id", "student_id"}))
public class StudentAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 题目ID → question_bank.id */
    @Column(name = "question_id", nullable = false)
    private Long questionId;

    /** 学生ID → students.id */
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    /** 学生作答图片 URL（COS 或本地 /uploads/...） */
    @Column(name = "answer_image_url", length = 1000)
    private String answerImageUrl;

    /** 学生作答文字（可选） */
    @Column(name = "answer_text", columnDefinition = "TEXT")
    private String answerText;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
