package com.eduai.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 教师注册专属信息
 */
@Data
public class TeacherRegisterInfo {

    @NotBlank(message = "昵称不能为空")
    private String nickname;

    /** 任教学科列表，如 ["math","physics"] */
    @NotEmpty(message = "请至少选择一个学科")
    private List<String> subjects;

    /** 学员UID列表（可选） */
    private List<Long> studentUids;
}
