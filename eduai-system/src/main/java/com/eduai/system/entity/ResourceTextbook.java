package com.eduai.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 学习资源 · 教材实体（resource_textbook）
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "resource_textbook")
public class ResourceTextbook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 学科 key，如 math */
    @Column(nullable = false, length = 20)
    private String subject;

    /** 版本 key：suke/renjiao/bsd/zj/hk */
    @Column(length = 20)
    private String version;

    /** 学段：primary/junior/senior */
    @Column(length = 10)
    private String stage;

    /** 教材名，如：七年级上册 苏科版 */
    @Column(nullable = false, length = 100)
    private String name;

    /** 年级，如：七年级上册 / 高一 */
    @Column(length = 20)
    private String grade;

    /** 排序 */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    /** 创建时间 */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.subject == null) this.subject = "math";
        if (this.version == null) this.version = "suke";
        if (this.stage == null) this.stage = "junior";
        if (this.sortOrder == null) this.sortOrder = 0;
    }
}
