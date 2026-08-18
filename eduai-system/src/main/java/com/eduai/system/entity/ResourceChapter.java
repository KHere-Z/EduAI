package com.eduai.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 学习资源 · 章节实体（resource_chapter）
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "resource_chapter")
public class ResourceChapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属教材ID */
    @Column(name = "textbook_id", nullable = false)
    private Long textbookId;

    /** 章节名，如：第1章 有理数 */
    @Column(nullable = false, length = 100)
    private String name;

    /** 章节简称，如：有理数 */
    @Column(length = 50)
    private String chapter;

    /** 排序 */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    /** 创建时间 */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.sortOrder == null) this.sortOrder = 0;
    }
}
