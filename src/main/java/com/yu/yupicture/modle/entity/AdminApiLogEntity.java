package com.yu.yupicture.modle.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 管理员接口调用日志（MongoDB集合：admin_api_log）
 * 仅保留核心字段：谁调用了什么接口
 */
@Data
@Document(collection = "admin_api_log")
public class AdminApiLogEntity implements Serializable {
    private static final long serialVersionUID = 1903260817583243919L;
    @Id // MongoDB自动生成唯一ID（ObjectId）
    private String id;

    // -------------------------- 管理员信息（谁调用） --------------------------
    private String adminId; // 管理员唯一标识（关联administrators集合的id）
    private String adminUserName; // 管理员登录名（冗余存储，避免联查）
    private Integer adminRole; // 管理员角色（0-安全/1-系统/2-审计，对应Administrator的role）

    // -------------------------- 接口信息（调用了什么） --------------------------
    private String apiModule; // 接口模块（从@AdminApiLog注解获取）
    private String apiName; // 接口名称（从@AdminApiLog注解获取）

    // -------------------------- 调用基础信息 --------------------------
    private LocalDateTime callTime; // 调用时间（默认当前时间）
    private String clientIp; // 调用者IP
    private Map<String, Object> requestParams; // 接口请求参数（JSON格式，便于查看）

    // 新增：日志过期时间（系统自动计算，非管理员设置）
    private LocalDateTime expireTime;

    // 构造方法中自动赋值（合规留存期从配置文件读取，如6个月）
    public AdminApiLogEntity() {
        // 方案1：固定留存期（如3个月）
        this.expireTime = LocalDateTime.now().plusMonths(3);

    }
}
