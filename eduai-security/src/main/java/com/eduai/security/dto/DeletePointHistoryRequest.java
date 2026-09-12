package com.eduai.security.dto;

import lombok.Data;

import java.util.List;

/**
 * 批量删除智学点变动记录请求
 * <p>
 * 刻意不加 {@code @NotEmpty}：与前端的约定是「空数组 / 全部 id 不属于本人」都返回
 * {@code deleted: 0}，让前端只判一个分支，不必为校验失败再写一条错误路径。
 */
@Data
public class DeletePointHistoryRequest {

    /** 待删除的记录 ID 列表（仅当前登录用户自己的记录会被删除） */
    private List<Long> ids;
}
