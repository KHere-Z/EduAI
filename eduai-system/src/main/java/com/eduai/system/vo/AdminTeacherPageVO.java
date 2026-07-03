package com.eduai.system.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 管理员 - 教师分页 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminTeacherPageVO {

    private List<AdminTeacherVO> list;
    private Long total;
    private Integer page;
    private Integer pageSize;
}