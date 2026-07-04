package com.eduai.system.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 知识点分页 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgePointPageVO {

    private List<KnowledgePointVO> list;
    private long total;
    private int page;
    private int pageSize;
}