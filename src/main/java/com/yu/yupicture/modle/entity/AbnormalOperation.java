package com.yu.yupicture.modle.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.Date;

/**
 * 敏感操作异常记录（用于审计提醒）
 */
@Data
@Document(collection = "abnormal_operation")
public class AbnormalOperation implements Serializable {
    private static final long serialVersionUID = 1903260817583243919L;
    @Id
    private String id;

    private String logId; // 关联的操作日志ID
    private String operatorId; // 操作人ID
    private String abnormalType; // 异常类型（如"HIGH_FREQUENCY"高频操作、"OFF_WORK"非工作时间）
    private String abnormalDesc; // 异常描述（如"1小时内删除10个敏感词"）
    private Date detectTime; // 检测时间（定时任务发现异常的时间）
    private boolean isHandled; // 是否已处理（默认false，审计管理员查看后可标记为true）
}