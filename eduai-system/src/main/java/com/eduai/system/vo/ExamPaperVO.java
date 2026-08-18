package com.eduai.system.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 试卷详情 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamPaperVO {

    private Long id;
    private Long studentId;
    private String studentName;
    private String subject;
    private String examType;
    private String school;

    /** 原卷图片URL列表 */
    private List<String> paperImages;

    /** AI识别的错题（JSON） */
    private String wrongQuestions;

    /** AI试卷分析 */
    private String paperAnalysis;

    /** 提分建议 */
    private String suggestions;

    /** 成绩 */
    private Double score;

    /** 知识点清单 */
    private String kpList;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
