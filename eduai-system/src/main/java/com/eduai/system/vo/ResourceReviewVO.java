package com.eduai.system.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 资源审核列表 VO（管理员端）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceReviewVO {

    private Long id;
    private String fileName;
    /** 与 type 同义（课件/学案/作业/试卷） */
    private String tag;
    private String year;
    private Integer price;
    private Boolean shared;
    /** 上传老师 uid（8 位零填充） */
    private String uploaderId;
    /** 上传老师姓名 */
    private String uploaderName;
    private String subject;
    private LocalDateTime createdAt;
}
