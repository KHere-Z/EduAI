package com.eduai.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 老师上传新题 DTO
 */
@Data
public class QuestionUploadDTO {

    /** 学科 */
    @NotBlank(message = "学科不能为空")
    private String subject;

    /** 题目文字 */
    @NotBlank(message = "题目内容不能为空")
    private String title;

    /** 正确答案 */
    private String answer;

    /** 关联知识点ID（逗号分隔，如 "3,7,12"） */
    private String knowledgePointIds;

    /** 难度：EASY / MEDIUM / HARD */
    private String difficulty;

    /** 年级·学期 */
    private String gradeLevel;

    /** 原图片URL（拍照上传） */
    private String originalImageUrl;

    /** 配图URL */
    private String diagramImageUrl;

    /** 配图状态：NONE / AUTO / MANUAL */
    private String diagramStatus;

    /** AI识别文字 */
    private String aiExtractedText;

    /** 老师文字解析 */
    private String teacherAnalysis;

    /** 老师解析配图 */
    private String teacherAnalysisImage;

    /** 解析配图文件类型 */
    private String teacherAnalysisImageType;
}