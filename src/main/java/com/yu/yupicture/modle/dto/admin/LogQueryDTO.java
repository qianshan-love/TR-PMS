package com.yu.yupicture.modle.dto.admin;

import com.yu.yupicture.modle.dto.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class LogQueryDTO extends PageRequest implements Serializable {
    private static final long serialVersionUID = -7504651319212825204L;
    private String operatorId; // 操作人ID（可选）
    private Date startTime;    // 开始时间（必选）
    private Date endTime;      // 结束时间（必选）
    private String operationType; // 操作类型（可选）
    // -------------------------- 管理员筛选（可选） --------------------------
    private String adminId; // 管理员ID（精确匹配，如"admin001"）
    private Integer adminRole; // 管理员角色（0-安全/1-系统/2-审计，精确匹配）
    private String adminUserName; // 管理员用户名（模糊匹配，如"zhang"）

    // -------------------------- 接口筛选（可选） --------------------------
    private String apiModule; // 接口模块（模糊匹配，如"sensitive_word"）
    private String apiName; // 接口名称（模糊匹配，如"addSensitiveWord"）
}
