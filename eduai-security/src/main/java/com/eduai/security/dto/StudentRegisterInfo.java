package com.eduai.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 学生注册专属信息
 */
@Data
public class StudentRegisterInfo {

    @NotBlank(message = "昵称不能为空")
    private String nickname;

    /** 所学学科列表，如 ["math","english"] */
    @NotEmpty(message = "请至少选择一个学科")
    private List<String> subjects;

    /** 老师UID（可选） */
    private Long teacherUid;
}
