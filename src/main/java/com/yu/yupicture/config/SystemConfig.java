package com.yu.yupicture.config;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

@Data
@Document(collection = "system_config")
public class SystemConfig {

    /**
     * 配置ID（固定为 "WARNING_THRESHOLD"，确保唯一）
     */
    @Field("_id")
    private String id = "WARNING_THRESHOLD";

    /**
     * CPU预警阈值（%，0~100），默认80.0
     */
    @Field("cpu_warning_threshold")
    private Double cpuWarningThreshold = 80.0;

    /**
     * 内存预警阈值（%，0~100），默认80.0
     */
    @Field("memory_warning_threshold")
    private Double memoryWarningThreshold = 80.0;

    /**
     * 磁盘预警阈值（%，0~100），默认85.0
     */
    @Field("disk_warning_threshold")
    private Double diskWarningThreshold = 85.0;

    /**
     * JVM预警阈值（%，0~100），默认90.0
     */
    @Field("jvm_warning_threshold")
    private Double jvmWarningThreshold = 90.0;


    /**
     * 最后修改时间
     */
    @Field("update_time")
    private Date updateTime;
}
