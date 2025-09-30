package com.yu.yupicture.modle.dto.admin;

import lombok.Data;

@Data
public class AdminRegistRequest {

    private String userName; // 登录用户名

    private String password; // 加密后的密码（如BCrypt）

    private String belongGroup; // 所属分组（如lingsuo）

    private Long addedCount = 0L; // 新增数量（如用户/文件新增计数）

    private String phoneNumber; // 手机号

    private String phoneCode; // 手机验证码

    private Integer role = 0; // 管理员类别：0-安全管理员，1-系统管理员，2-审计管理员

}
