package com.eduai.system.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 管理员视角 - 教师列表项 VO（含学生数/课时汇总）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminTeacherVO {

    private Long id;
    private Long userId;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private List<String> subjects;
    private String title;
    private String orgName;
    private String avatar;
    private Integer status;

    /** 学生数量 */
    private Long studentCount;

    /** 总剩余课时 */
    private Long totalHours;
}