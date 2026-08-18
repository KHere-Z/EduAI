package com.eduai.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 试卷上传/分析 DTO
 */
@Data
public class ExamPaperDTO {

    /** 学科 */
    @NotBlank(message = "学科不能为空")
    private String subject;

    /** 考试类型：月考/期中/期末/模拟考 */
    @NotBlank(message = "考试类型不能为空")
    private String examType;

    /** 学校 */
    private String school;

    /** 原卷图片URL列表 */
    private List<String> paperImages;

    /** AI识别的错题（JSON字符串） */
    private String wrongQuestions;

    /** AI试卷分析 */
    private String paperAnalysis;

    /** 提分建议 */
    private String suggestions;

    /** 成绩 */
    private Double score;

    /** 知识点清单 */
    private String kpList;
}
