package com.eduai.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 管理员 - 老师创建/更新 DTO
 */
@Data
public class AdminTeacherDTO {

    /** 真实姓名（必填） */
    @NotBlank(message = "姓名不能为空")
    private String realName;

    /** 用户名（新增时必填，编辑时可选） */
    private String username;

    /** 密码（新增时必填，编辑时留空=不修改密码） */
    private String password;

    /** 职称 */
    private String title;

    /** 任教学科列表 → 逗号分隔存入 teachers.subject_ids */
    private List<String> subjectIds;

    /** 所属机构ID（后端解析 orgName 后填充，前端一般传 orgName） */
    private Long orgId;

    /** 机构名称（前端传此字段，后端按 name 查找/创建 organization） */
    private String orgName;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;
}