package com.eduai.system.dto;

import lombok.Data;

import java.util.List;

/**
 * 目录节点排序请求体：{@code { "orderedIds": [id1, id2, ...] }}
 * <p>
 * 后端按数组顺序写 sort_order（index+1），未出现在数组里的保持原值。
 */
@Data
public class ReorderDTO {

    /** 按新顺序排列的节点 ID 列表 */
    private List<Long> orderedIds;
}
