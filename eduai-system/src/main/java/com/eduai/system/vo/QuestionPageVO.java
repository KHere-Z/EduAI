package com.eduai.system.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 题目分页 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionPageVO {

    private List<QuestionVO> list;
    private long total;
    private int page;
    private int pageSize;
}