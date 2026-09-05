package com.eduai.system.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 学习资源 · 资源文件分页 VO
 * <p>
 * 用于 GET /api/v1/resource/resources 传 page/pageSize 时的返回结构。
 * total 为聚合（含子级）后的全量条数，与 list 的切片关系对齐前端 el-pagination。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceFilePageVO {

    private List<ResourceFileVO> list;
    private Long total;
    private Integer page;
    private Integer pageSize;
}
