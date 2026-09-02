package com.eduai.security.dto;

import lombok.Data;

import java.util.List;

/**
 * 更新个人信息请求
 */
@Data
public class UpdateProfileRequest {

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatar;

    /** 个人简介 */
    private String bio;

    /** 真实姓名 */
    private String realName;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 学科列表（如 ["数学","物理"]） */
    private List<String> subjects;

    /** 年级（学生） */
    private String grade;

    /** 学校（学生） */
    private String school;
}
