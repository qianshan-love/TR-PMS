package com.yu.yupicture.modle.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class AdminApiLogVO implements Serializable {
    private static final long serialVersionUID = -791643723408494550L;
    // 1. 管理员信息（前端需要显示“谁操作”）
    private String adminUserName; // 管理员登录名（如“security_zhang”）
    private String adminRoleDesc; // 管理员角色描述（如“安全管理员”，而非数字0）

    // 2. 接口信息（前端需要显示“操作了什么”）
    private String apiModule; // 接口模块（如“sensitive_word-敏感词管理”）
    private String apiName; // 接口名称（如“addSensitiveWord-添加敏感词”）

    // 3. 调用信息（前端需要显示“操作时间、IP、参数”）
    private String callTime; // 格式化后的调用时间（如“2024-09-01 15:30:45”）
    private String clientIp; // 客户端IP（如“192.168.1.100”）
    private String requestParamsDesc; // 格式化后的请求参数（如“敏感词：test，所属组：default”）
}