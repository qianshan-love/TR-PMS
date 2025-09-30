package com.yu.yupicture.modle.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Document(collection = "system_config")
public class SystemConfigDoc {
    @Id
    private String id = "monitor_retention_config"; // 固定主键，确保唯一配置记录

    @Field("retention_days")
    private Integer retentionDays = 15; // 数据保留天数（默认15天）

    @Field("updated_by")
    private String updatedBy; // 最后修改人

    @Field("updated_at")
    private Long updatedAt; // 最后修改时间戳（毫秒级）

    @Field("config_desc")
    private String configDesc = "系统监控数据保留配置，支持1~90天调整"; // 配置说明
}
