package com.eduai.security.enums;

import lombok.Getter;

/**
 * 用户角色枚举
 */
@Getter
public enum RoleEnum {

    TEACHER("teacher", 3, "教师"),
    STUDENT("student", 4, "学生");

    /** 前端标识 */
    private final String code;
    /** 数据库 role_type 值 */
    private final int dbValue;
    /** 中文名 */
    private final String label;

    RoleEnum(String code, int dbValue, String label) {
        this.code = code;
        this.dbValue = dbValue;
        this.label = label;
    }

    /** 根据前端 code 查找 */
    public static RoleEnum fromCode(String code) {
        for (RoleEnum r : values()) {
            if (r.code.equalsIgnoreCase(code)) return r;
        }
        throw new IllegalArgumentException("无效的角色类型: " + code);
    }

    /** 根据数据库值查找 */
    public static RoleEnum fromDbValue(int dbValue) {
        for (RoleEnum r : values()) {
            if (r.dbValue == dbValue) return r;
        }
        throw new IllegalArgumentException("无效的角色类型值: " + dbValue);
    }
}
