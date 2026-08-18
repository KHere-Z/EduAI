package com.eduai.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识点资源实体（学案/讲义/习题/试卷等文件）
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "kp_resources")
public class KpResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 知识点ID */
    @Column(name = "kp_id", nullable = false)
    private Long kpId;

    /** 上传者 UID（owner，为空表示历史数据） */
    @Column(name = "teacher_uid")
    private Long teacherUid;

    /** 原文件名 */
    @Column(name = "file_name", nullable = false, length = 200)
    private String fileName;

    /** 文件大小（字节） */
    @Column(name = "file_size")
    private Long fileSize;

    /** 存储路径 */
    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    /** 文件类型 pdf/docx/doc */
    @Column(name = "file_type", length = 20)
    private String fileType;

    /** 标签：学案/讲义/习题/试卷 */
    @Column(name = "tag", length = 10)
    private String tag;

    /** 创建时间 */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
