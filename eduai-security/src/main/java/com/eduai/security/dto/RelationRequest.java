package com.eduai.security.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发送关联请求
 */
@Data
public class RelationRequest {

    @NotNull(message = "目标UID不能为空")
    private Long targetUid;

    /** 关系类型：teacher_student（师生，默认）/ colleague（同事） */
    private String type;
}
