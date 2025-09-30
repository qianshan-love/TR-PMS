package com.yu.yupicture.modle.dto.user;

import lombok.Data;

/**
 * 用户登录请求体
 *
 * @author Yu
 * @description 用户登录请求体
 * @createDate 2025-05-09 14:03:42
 */
@Data
public class UserLoginRequest {
    private String userAccount;
    private String userPassword;
}
