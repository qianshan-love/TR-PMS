package com.yu.yupicture.modle.vo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class AdminVO implements Serializable {

    private static final long serialVersionUID = -9191929709046966686L;

    private String id; // 管理员唯一标识（对应MongoDB的_id）

    private String userName; // 登录用户名

    private String belongGroup; // 所属分组（如lingsuo）

    private LocalDateTime expireTime; // 账号过期时间

    private Long addedCount = 0L; // 新增数量（如用户/文件新增计数）

    private String phoneNumber; // 手机号

    private Integer role = 0; // 管理员类别：0-安全管理员，1-系统管理员，2-审计管理员

    private String status; // 账号状态（如joined-已加入）

    private String postUrl; // 关联的服务地址
}
