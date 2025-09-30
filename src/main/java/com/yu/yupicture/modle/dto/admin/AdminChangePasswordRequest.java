package com.yu.yupicture.modle.dto.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.Data;

import java.io.Serializable;

@Data
public class AdminChangePasswordRequest  implements Serializable {

    private static final long serialVersionUID = 4841510141153994848L;

    // 手机号
    private String phoneNumber;
    // 旧密码
    private String password;
    // 新密码
    private String newPassword;
    // 确认新密码
    private String checkPassword;
}
