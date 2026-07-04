package com.eduai.system.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 题目详情 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionVO {

    private Long id;
    private String subject;
    private String type;
    private String source;
    private Long studentId;
    private String studentName;
    private Long teacherId;
    private String teacherName;

    // 图片相关
    private String originalImageUrl;
    private String diagramImageUrl;
    private String diagramStatus;

    // AI 相关
    private String aiExtractedText;
    private String title;
    private String knowledgePointIds;
    private String knowledgePointNames;    // 知识点名称（逗号分隔，如"有理数运算,一元一次方程"）

    // 答案与解析
    private String answer;
    private String analysis;
    private String solution;
    private String similarJson;

    // 老师解析
    private String teacherAnalysis;
    private String teacherAnalysisImage;
    private String teacherAnalysisImageType;

    // 分类
    private String difficulty;
    private String mastery;
    private String errorType;
    private String gradeLevel;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}