package com.eduai.system.dto;

import lombok.Data;

import java.util.List;

/**
 * 前端日志批量上报请求体。
 * <p>
 * 顶层 role/uid/token 用于后端归集，entries 为一批日志（前端 2s 节流、每批最多 20 条）。
 */
@Data
public class ClientLogReportDTO {

    /** 角色：1=管理员 3=老师 4=学生；null=未登录 */
    private Integer role;

    /** 用户 id（可能为空字符串） */
    private String uid;

    /** 登录 token（可能为空字符串，后端仅留存，不做校验） */
    private String token;

    /** 一批日志条目 */
    private List<ClientLogEntry> entries;
}
