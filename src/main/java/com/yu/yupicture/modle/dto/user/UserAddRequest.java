package com.yu.yupicture.modle.dto.user;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;

@Data
public class UserAddRequest implements Serializable {


    /**
     * 账号
     */
    private String userAccount;


    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;


    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
