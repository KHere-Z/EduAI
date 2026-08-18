package com.eduai.security.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户关系 VO
 */
@Data
@Builder
public class RelationVO {

    private Long id;
    /** 对方 UID（8位字符串） */
    private String uid;
    /** 对方姓名 */
    private String name;
    /** 对方昵称 */
    private String nickname;
    /** 对方学科 */
    private List<String> subjects;
    /** 对方角色（teacher/student） */
    private String role;
    /** 关系类型：teacher_student / colleague */
    private String type;
    /** pending / accepted / rejected */
    private String status;
    /** 请求方向：sent=我发出的, received=发给我的 */
    private String direction;
    private LocalDateTime createdAt;
}
