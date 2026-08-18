package com.eduai.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 题目-知识点关联表（多对多中间表）。
 * <p>
 * 替代 {@code question_bank.knowledge_point_ids} 逗号分隔 CSV 列，使「按知识点筛题」从
 * 4 个 {@code LIKE} 全表扫描改为走 {@code idx_qkp_kp} 索引精确匹配，并消除前缀歧义
 * （查 5 不会误中 15/25）。CSV 列暂保留作为展示用的反规范化缓存。
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "question_knowledge_point",
        uniqueConstraints = @UniqueConstraint(name = "uk_question_kp",
                columnNames = {"question_id", "knowledge_point_id"}),
        indexes = @Index(name = "idx_qkp_kp", columnList = "knowledge_point_id"))
public class QuestionKnowledgePoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 题目ID → question_bank.id */
    @Column(name = "question_id", nullable = false)
    private Long questionId;

    /** 知识点ID → knowledge_points.id */
    @Column(name = "knowledge_point_id", nullable = false)
    private Long knowledgePointId;
}
