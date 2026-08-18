package com.eduai.security.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户信息 VO（不含密码等敏感字段）
 */
@Data
@Builder
public class UserVO {

    /** 8位展示UID（如 "00007349"） */
    private String uid;

    /** 内部ID */
    private Long id;

    private String username;
    private String realName;
    private String nickname;
    private String avatar;

    /** 个人简介 */
    private String bio;

    /** 手机号（脱敏，如 138****1234） */
    private String phone;

    /** 手机号掩码 */
    private String phoneMasked;

    private String email;
    private Integer roleType;
    private String roleName;
    private Integer status;

    /** 学科列表 */
    private List<String> subjects;

    /** 教师专属：学员UID列表 */
    private List<Long> studentUids;

    /** 学生专属：老师UID */
    private Long teacherUid;

    // ===== 教师扩展字段（从 teachers 表查询） =====
    private Long orgId;
    private String orgName;
    private String title;

    /** 智学点余额 */
    private Integer points;

    /** 是否绑定微信 */
    private Boolean bindWechat;

    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
}
