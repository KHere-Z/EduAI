package com.eduai.security.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 用户公开信息 VO（GET /api/v1/users/{uid}）
 */
@Data
@Builder
public class PublicUserVO {

    /** 内部 ID */
    private Long id;

    /** 8位展示 UID（如 "00007349"） */
    private String uid;

    /** 姓名 */
    private String name;

    /** 角色（teacher/student/admin） */
    private String role;

    /** 学科列表 */
    private List<String> subjects;

    /** 个人简介 */
    private String bio;

    /** 头像URL */
    private String avatar;

    /** 当前登录用户是否与此用户关联 */
    private Boolean isRelated;
}
