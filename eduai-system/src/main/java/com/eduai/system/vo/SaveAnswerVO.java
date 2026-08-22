package com.eduai.system.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 保存答案（不批改）结果 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveAnswerVO {

    private Long questionId;

    /** 保存后的作答图片 URL */
    private String answerImageUrl;

    /** 作答文字（可选） */
    private String answerText;
}
