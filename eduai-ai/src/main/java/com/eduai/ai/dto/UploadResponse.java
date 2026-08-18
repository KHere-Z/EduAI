package com.eduai.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 文件上传响应
 */
@Data
@AllArgsConstructor
public class UploadResponse {
    /** 文件访问 URL */
    private String url;
    /** 原始文件名 */
    private String originalName;
}