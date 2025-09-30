package com.yu.yupicture.modle.dto.user;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.util.Date;
@Data
public class UserJoinGroupRequest implements Serializable {

    private static final long serialVersionUID = -3784625175253850762L;

    /**
     * 用户唯一标识 (UnionID)，必填
     */
    private String unionId;

    /**
     * 小程序 OpenID (MPId)
     */
    private String mpId;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 用户头像 URL
     */
    private String avatarUrl;

    /**
     * 审核人 ID
     */
    private String reviewerId;

    /**
     * 用户希望加入的组标识
     */
    private String belongGroup;

    /**
     * 注册时间
     */
    private Date registerTime;

    /**
     * 期望的成员资格到期时间
     */
    private Date expireTime;

    /**
     * 是否超级用户
     */
    private String isSuper;
}
