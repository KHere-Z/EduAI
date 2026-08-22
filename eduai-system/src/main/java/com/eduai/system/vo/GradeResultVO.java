package com.eduai.system.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 题目 AI 批改结果 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeResultVO {

    /** 批改结论：true=正确 / false=错误 */
    private Boolean correct;

    /** 批改说明 */
    private String result;

    /** 是否命中缓存（重复提交返回已批改结果，未再扣点） */
    private boolean cached;
}
