package com.yu.yupicture.modle.enums;

import lombok.Getter;

@Getter
public enum UserRole {
    ADMIN("admin", "管理员"),
    USER("user", "普通用户"),
    BAN("ban", "被封号");
    private final String name;
    private final String value;


    UserRole(String name, String value) {
        this.name = name;
        this.value = value;
    }

    // 根据 value 获取枚举
    public static UserRole getEnumByValue(String value) {
        for (UserRole anEnum : UserRole.values()) {
            if (anEnum.getValue().equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

    public static UserRole getEnumByName(String name) {
        for (UserRole anEnum : UserRole.values()) {
            if (anEnum.getName().equals(name)) {
                return anEnum;
            }
        }
        return null;
    }
}
