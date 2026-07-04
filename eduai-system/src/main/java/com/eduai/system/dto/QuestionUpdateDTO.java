package com.eduai.system.dto;

import lombok.Data;

/**
 * 老师更新题目 DTO（只传需要更新的字段）
 */
@Data
public class QuestionUpdateDTO {

    /** 题目文字 */
    private String title;

    /** 正确答案 */
    private String answer;

    /** 关联知识点ID（逗号分隔） */
    private String knowledgePointIds;

    /** 难度：EASY / MEDIUM / HARD */
    private String difficulty;

    /** 年级·学期 */
    private String gradeLevel;

    /** 配图URL */
    private String diagramImageUrl;

    /** 配图状态：NONE / AUTO / MANUAL */
    private String diagramStatus;

    /** 老师文字解析 */
    private String teacherAnalysis;

    /** 老师解析配图 */
    private String teacherAnalysisImage;

    /** 解析配图文件类型 */
    private String teacherAnalysisImageType;

    /** 掌握度：UNMASTERED / FAMILIAR / MASTERED */
    private String mastery;

    /** AI错因分析 */
    private String analysis;

    /** AI解题步骤 */
    private String solution;

    /** 错误类型 */
    private String errorType;
}