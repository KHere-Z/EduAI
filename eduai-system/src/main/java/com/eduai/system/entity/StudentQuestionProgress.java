package com.eduai.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 学生题目掌握度（共享题按学生隔离）。
 * <p>
 * 背景：{@code question_bank} 的 {@code mastery} / {@code completed} 直接挂在题行上，
 * 对「学生私有错题」（一行一学生）没问题，但老师上传的共享新题（{@code shared=1}）被
 * 多个学生各自练习时，会互相覆盖掌握度/完成状态。本表把共享题的进度按
 * {@code (student_id, question_id)} 隔离存储。
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "student_question_progress",
        uniqueConstraints = @UniqueConstraint(name = "uk_student_question",
                columnNames = {"student_id", "question_id"}))
public class StudentQuestionProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 学生ID → students.id */
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    /** 题目ID → question_bank.id */
    @Column(name = "question_id", nullable = false)
    private Long questionId;

    /** 掌握度：UNMASTERED / FAMILIAR / MASTERED */
    @Column(length = 20)
    private String mastery;

    /** 是否完成 */
    @Column(nullable = false)
    @Builder.Default
    private Boolean completed = false;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.updatedAt = now;
        if (this.mastery == null) {
            this.mastery = "UNMASTERED";
        }
        if (this.completed == null) {
            this.completed = false;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
