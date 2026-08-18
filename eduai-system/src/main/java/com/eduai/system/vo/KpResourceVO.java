package com.eduai.system.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识点资源 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KpResourceVO {

    private Long id;
    private Long kpId;
    /** 上传者 UID（8位字符串，前端 isOwnRes 与 auth.user.uid 比较） */
    private String teacherId;
    private String fileName;
    private Long fileSize;
    private String fileUrl;
    private String fileType;
    private String tag;
    private LocalDateTime createdAt;
}
