package com.eduai.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识点实体（全平台共享，所有老师可管理）
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "knowledge_points")
public class KnowledgePoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 学科 */
    @Column(nullable = false, length = 20)
    private String subject;

    /** 知识点名称 */
    @Column(nullable = false, length = 100)
    private String name;

    /** 年级·学期 */
    @Column(name = "grade_level", length = 50)
    private String gradeLevel;

    /** 父知识点ID（树形结构） */
    @Column(name = "parent_id")
    private Long parentId;

    /** 排序 */
    @Column(name = "sort_order")
    private Integer sortOrder;

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
        if (this.sortOrder == null) {
            this.sortOrder = 0;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}