package com.eduai.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 资源下载记录（用于「同一学生重复下载不重复扣费」）
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "resource_downloads",
        uniqueConstraints = @UniqueConstraint(columnNames = {"resource_id", "user_id"}))
public class ResourceDownload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 资源ID (resource_file.id) */
    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    /** 下载用户ID (users.id) */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
