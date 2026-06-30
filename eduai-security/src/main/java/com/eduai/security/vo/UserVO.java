package com.eduai.security.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 用户信息 VO（不含密码）
 */
@Data
@Builder
public class UserVO {

    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private Integer roleType;
    private Integer status;

    // ===== 教师专属字段（roleType=3 时返回）=====
    /** 任教学科列表，如 ["math", "physics"] */
    private List<String> subjects;
    /** 所属机构ID */
    private Long orgId;
    /** 所属机构名称 */
    private String orgName;
    /** 职称 */
    private String title;
    /** 头像 */
    private String avatar;
}