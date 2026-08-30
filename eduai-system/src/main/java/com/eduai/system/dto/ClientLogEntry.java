package com.eduai.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 前端日志单条记录（内存环形缓冲元素）。
 * <p>
 * 对应前端 logger.js 上报的 entries 元素（level/msg/url/time），
 * role/uid 由上报体顶层字段在入队时回填。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientLogEntry {

    /** 级别：log / info / warn / error / debug */
    private String level;

    /** 日志内容（前端已截断至 ≤1000 字符） */
    private String msg;

    /** 触发页面路径，如 /teacher/students */
    private String url;

    /** 角色：1=管理员 3=老师 4=学生；null=未登录 */
    private Integer role;

    /** 用户 id（可能为空字符串） */
    private String uid;

    /** 毫秒时间戳 */
    private Long time;
}
