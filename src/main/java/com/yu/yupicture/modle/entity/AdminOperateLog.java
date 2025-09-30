package com.yu.yupicture.modle.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 管理员操作日志（审计核心实体）
 * 记录所有管理员的关键操作（如敏感词增删改、系统配置修改等）
 */
@Data
@Document(collection = "admin_operate_log")
public class AdminOperateLog implements Serializable {
    private static final long serialVersionUID = -5134258045993130926L;
    @Id // MongoDB主键（自动生成ObjectId，用String接收更灵活）
    private String id;

    // -------------------------- 操作人信息（关联管理员表） --------------------------
    private String operatorId; // 操作人ID（关联Administrator的id，如"security_admin01"）
    private String operatorName; // 操作人姓名（冗余存储，避免联查，如"安全管理员-张三"）

    // -------------------------- 操作基础信息 --------------------------
    private Date operationTime; // 操作时间（精确到毫秒，用于时序排序和筛选）
    private String operationIp; // 操作IP地址（用于定位操作来源，兼容反向代理场景）
    private String operationModule; // 操作模块（如"sensitive_word"敏感词管理、"system_config"系统配置）
    private String operationType; // 操作类型（如"add"添加、"delete"删除、"query"查询、"update"修改）
    private String operationDesc; // 操作描述（人性化说明，如"添加敏感词：test"）
    private String operationContent; // 操作详情（JSON格式字符串，存储请求参数，如"{\"sensitiveWord\":\"test\",\"belongGroup\":\"default\"}"）

    // -------------------------- 操作结果信息 --------------------------
    private String operationResult; // 操作结果（固定值：SUCCESS-成功、FAIL-失败）
    private String errorMsg; // 失败原因（结果为FAIL时非空，如"敏感词已存在"）

    // -------------------------- 防篡改信息 --------------------------
    private String logSignature; // 日志签名（SHA256哈希值，基于operatorId+operationTime+operationContent+密钥生成）
}
