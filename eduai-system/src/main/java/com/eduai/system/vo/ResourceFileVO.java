package com.eduai.system.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 学习资源 · 资源文件 VO
 * <p>
 * 前端兼容字段：type/tag/year/price/fileName/title/fileSize（tag 与 type 同义）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceFileVO {

    private Long id;
    private Long sectionId;
    private String subject;

    /** 类型：课件/学案/作业/试卷 */
    private String type;

    /** 与 type 同义 */
    private String tag;

    private String year;
    private Integer price;
    private String title;
    private String fileName;
    private Long fileSize;
    private String filePath;
    private String author;
    private Long uploaderId;
    /** 是否共享：true=所有用户可见；false=仅上传者及其学生可见 */
    private Boolean shared;
    private Integer downloadCount;
    /** 审核状态：pending/approved/rejected */
    private String status;
    /** 驳回理由 */
    private String rejectReason;
    private LocalDateTime createdAt;
}
