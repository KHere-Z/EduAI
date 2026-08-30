package com.eduai.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 学习资源 · 资源文件实体（resource_file）
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "resource_file")
public class ResourceFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属小节ID，关联 resource_section.id */
    @Column(name = "section_id", nullable = false)
    private Long sectionId;

    /** 学科 key，如 math */
    @Column(nullable = false, length = 20)
    private String subject;

    /** 类型：课件/学案/作业/试卷（前端 tag 同义） */
    @Column(nullable = false, length = 20)
    private String type;

    /** 年份，如 2026 */
    @Column(length = 10)
    private String year;

    /** 资源点（1:10 体系）：管理员端统一 20；老师端 10/30/50（后端只存不校验） */
    @Column(nullable = false)
    private Integer price;

    /** 标题，缺省取文件名 */
    @Column(length = 200)
    private String title;

    /** 原文件名 */
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    /** 服务端存储路径（相对 uploads/） */
    @Column(name = "file_path", length = 500)
    private String filePath;

    /** 压缩包内预览入口文件路径（非压缩包为 NULL，由文件自身类型实时判定预览能力） */
    @Column(name = "preview_path", length = 500)
    private String previewPath;

    /** 文件大小（字节） */
    @Column(name = "file_size")
    private Long fileSize;

    /** 上传者 */
    @Column(length = 50)
    private String author;

    /** 上传者用户ID（用于下载分成，关联 users.id） */
    @Column(name = "uploader_id")
    private Long uploaderId;

    /** 是否共享：true=所有登录用户可见；false=仅上传者及其已接受的学生可见 */
    @Column(nullable = false)
    @Builder.Default
    private Boolean shared = true;

    /** 下载量 */
    @Column(name = "download_count")
    private Integer downloadCount;

    /** 审核状态：pending/approved/rejected */
    @Column(length = 20, nullable = false)
    @Builder.Default
    private String status = "approved";

    /** 驳回理由（仅 rejected 时非空） */
    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    /** 审核管理员 users.id */
    @Column(name = "reviewer_id")
    private Long reviewerId;

    /** 审核时间 */
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    /** 创建时间 */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.subject == null) this.subject = "math";
        if (this.price == null) this.price = 0;
        if (this.fileSize == null) this.fileSize = 0L;
        if (this.downloadCount == null) this.downloadCount = 0;
        if (this.shared == null) this.shared = true;
        if (this.status == null) this.status = "approved";
    }
}
