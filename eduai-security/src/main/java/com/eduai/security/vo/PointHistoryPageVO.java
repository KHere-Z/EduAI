package com.eduai.security.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 智学点变动历史分页 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointHistoryPageVO {

    private List<PointHistoryItemVO> list;
    private long total;
    private int page;
    private int pageSize;
}
