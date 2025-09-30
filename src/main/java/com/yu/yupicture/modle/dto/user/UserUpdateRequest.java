package com.yu.yupicture.modle.dto.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class UserUpdateRequest implements Serializable {

    /**
     * 密码
     */
    private String userPassword;
    /**
     * 校验密码
     */
    private String checkPassword;

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

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
