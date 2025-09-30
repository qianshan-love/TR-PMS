package com.yu.yupicture.modle.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.util.Date;

@Data
@Document(collection = "pending_approvals")
public class PendingApproval implements Serializable {

    private static final long serialVersionUID = -5977115321720898463L;

    @Id
    private String id; // MongoDB 主键，使用字符串类型

    @Field("union_id") // 映射到文档中的 union_id 字段
    private String unionID;

    @Field("mp_id")
    private String mpID;

    @Field("nickname")
    private String nickname;

    @Field("avatar_url")
    private String avatarURL;

    @Field("belong_group")
    private String belongGroup;

    @Field("reviewer_id")
    private String reviewerID;

    @Field("register_time")
    private Date registerTime;

    @Field("expire_time")
    private Date expireTime;

    @Field("is_super")
    private String isSuper; // 注意：数据库中存储为字符串 "true"/"false"

    @Field("created_at")
    private Date createdAt;

    private String status; // 审核状态：pending, approved, rejected 等
}
