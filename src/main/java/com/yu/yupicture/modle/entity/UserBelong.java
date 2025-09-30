package com.yu.yupicture.modle.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.util.Date;

@Data
@Document(collection = "user_belong")
public class UserBelong implements Serializable {

    private static final long serialVersionUID = 4975261595589427136L;

    @Id
    private String id;

    /**
     * 所属组的管理员ID
     */
    @Field("adminId")
    private String adminId;

    /**
     * 组成员ID
     */
    @Field("staffId")
    private String staffId;

    /**
     * 组成员头像URL
     */
    @Field("staffImage")
    private String staffImage;

    /**
     * 组成员名称
     */
    @Field("staffName")
    private String staffName;

    /**
     * 所属组标识
     */
    @Field("BelongGroup")
    private String belongGroup;

    /**
     * 成员在该组的到期时间
     * Spring Data MongoDB 会自动处理 Date 类型与 MongoDB Date 之间的转换。
     */
    @Field("expireTime")
    private Date expireTime;
}
