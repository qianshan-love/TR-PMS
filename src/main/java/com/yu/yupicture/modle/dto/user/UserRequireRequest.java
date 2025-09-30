package com.yu.yupicture.modle.dto.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.yu.yupicture.modle.dto.PageRequest;
import lombok.Data;

import java.io.Serializable;

@Data
public class UserRequireRequest extends PageRequest implements Serializable {
    /**
     * 自动生成长id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 账号
     */
    private String userAccount;


    /**
     * 用户昵称
     */
    private String userName;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
