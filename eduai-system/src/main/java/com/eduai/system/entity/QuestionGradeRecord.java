package com.eduai.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 题目 AI 批改记录（幂等防重复扣点）。
 * <p>
 * 一题一学生只批改一次：{@code (question_id, student_id)} 唯一。首次批改时扣点 + 调 AI，
 * 结果落表；重复提交直接返回缓存结果，不再扣点、不再调 AI，避免前端重试/误点导致重复扣 5 智学点。
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "question_grade_records",
        uniqueConstraints = @UniqueConstraint(name = "uk_question_student",
                columnNames = {"question_id", "student_id"}))
public class QuestionGradeRecord {

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

    /** 批改结论：true=正确 / false=错误 */
    private Boolean correct;

    /** 批改说明 */
    @Column(columnDefinition = "TEXT")
    private String result;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
