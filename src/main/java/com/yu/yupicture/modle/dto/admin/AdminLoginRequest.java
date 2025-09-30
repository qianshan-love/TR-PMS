package com.yu.yupicture.modle.dto.admin;

import lombok.Data;

@Data
public class AdminLoginRequest {

    /**
     * 手机号
     */
    private String phoneNumber;

    /**
     * 密码
     */
    private String password;

    /**
     * 验证码
     */
    private String phoneCode;

}
