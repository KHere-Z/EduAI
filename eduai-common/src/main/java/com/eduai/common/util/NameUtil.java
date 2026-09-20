package com.eduai.common.util;

/**
 * 显示名口径工具。
 * <p>
 * 学生档案的 {@code students.name} 是 NOT NULL，但「这个人叫什么」在 {@code users} 里
 * 可能三个字段都为空（自助注册不强制真名、昵称也可不填）。历史上三处建档代码
 * 各写了一遍 {@code realName → nickname → "新同学"} 的兜底链，此处收敛为一处 ——
 * 漏掉任一处，同一学生就会在不同页面显示成不同名字，且列表里会出现「新同学」认不出是谁。
 */
public final class NameUtil {

    /** 学生档案缺姓名时的兜底显示名（老师/管理员列表里会直接看到，改文案等于改产品） */
    public static final String DEFAULT_STUDENT_NAME = "新同学";

    private NameUtil() {
    }

    /**
     * 学生显示名：{@code realName → nickname → "新同学"}。
     * <p>
     * 四处「建/补学生档案」必须共用同一套兜底：自助注册建档案、个人中心补档案、
     * 师生关系通过时补档案、以及 {@code AuthServiceImpl.createUserByRole} 建号
     * （该方法当前无调用方，恢复调用时学生分支也要走这里）。
     * <p>
     * 长度无需截断：{@code students.name} 是 VARCHAR(50)，而 realName/nickname 同出
     * {@code users.real_name} / {@code users.nickname}（同为 VARCHAR(50)），
     * 超长在写 users 时已经先报错了。
     */
    public static String displayName(String realName, String nickname) {
        if (realName != null && !realName.isBlank()) {
            return realName.trim();
        }
        if (nickname != null && !nickname.isBlank()) {
            return nickname.trim();
        }
        return DEFAULT_STUDENT_NAME;
    }
}
