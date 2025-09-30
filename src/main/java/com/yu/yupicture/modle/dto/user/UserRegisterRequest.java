package com.yu.yupicture.modle.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = -3638289777322319698L;
    /**
     * 账号
     */
    private String userAccount;
    /**
     * 密码
     */
    private String userPassword;
    /**
     *
     */
    private String checkPassword;

}
