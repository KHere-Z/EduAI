package com.eduai.common.util;

import cn.hutool.crypto.digest.BCrypt;

/**
 * 密码工具：BCrypt 单向哈希 + 存量明文兼容校验
 * <p>
 * 用于登录/注册/管理员建号等场景，杜绝密码明文入库。
 */
public final class PasswordUtil {

    private PasswordUtil() {
    }

    /** 判断字符串是否为 BCrypt 哈希（$2a/$2b/$2y 开头） */
    public static boolean isBcrypt(String s) {
        return s != null && s.length() > 3 && s.charAt(0) == '$' && s.charAt(1) == '2';
    }

    /** 明文 → BCrypt 哈希（null/blank 返回 null） */
    public static String encode(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return BCrypt.hashpw(raw);
    }

    /** 校验明文与存储值是否匹配（兼容存量明文，命中后由调用方透明升级） */
    public static boolean matches(String raw, String stored) {
        if (raw == null || stored == null) {
            return false;
        }
        if (isBcrypt(stored)) {
            return BCrypt.checkpw(raw, stored);
        }
        // 存量明文：常量时间比较，防时序侧信道
        return constantTimeEquals(raw, stored);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }
}
