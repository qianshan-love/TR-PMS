package com.yu.yupicture.modle.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;

@Data
@Document(collection = "system_monitor")
public class SystemMonitorDoc implements Serializable {

    private static final long serialVersionUID = 1349985505510905581L;

    @Id
    private String id; // MongoDB自动生成的主键

    @Indexed // 添加索引，提升时间范围查询效率
    @Field("metric_timestamp")
    private Long metricTimestamp; // 数据生成时间戳（毫秒级）

    // ------------------- 过期机制核心字段 -------------------
    /**
     * 数据过期时间戳（毫秒级）
     * TTL索引：expireAfterSeconds = 0 → 当expireTime < 当前时间时，MongoDB自动删除该文档
     */
    @Indexed(expireAfterSeconds = 0)
    @Field("expire_time")
    private Long expireTime;

    //------------------- CPU 监控指标 -------------------

    @Field("cpu_value")
    private Double cpuValue; // CPU使用率

    @Field("cpu_unit")
    private String cpuUnit; // CPU使用率单位（%）

    //------------------- 系统内存监控指标 -------------------

    @Field("sys_mem_total")
    private Long sysMemTotal; // 系统内存总量（MB）

    @Field("sys_mem_used")
    private Long sysMemUsed; // 系统内存已用（MB）

    @Field("sys_mem_free")
    private Long sysMemFree; // 系统内存空闲（MB）

    @Field("sys_mem_cache")
    private Long sysMemCache; // 系统内存缓存（MB）

    @Field("mem_unit")
    private String memUnit; // 内存单位（MB）

    //------------------- 磁盘监控指标 -------------------

    @Field("disk_total")
    private Double diskTotal; // 磁盘总量（GB）

    @Field("disk_used")
    private Double diskUsed; // 磁盘已用（GB）

    @Field("disk_unit")
    private String diskUnit; // 磁盘单位（GB）

    //------------------- JVM 内存监控指标 -------------------

    @Field("jvm_used")
    private Long jvmUsed; // JVM内存已用（MB）

    @Field("jvm_max")
    private Long jvmMax; // JVM内存最大（MB）

    @Field("jvm_unit")
    private String jvmUnit; // JVM内存单位（MB）

    //------------------- 网络监控指标 -------------------

    @Field("net_download_speed")
    private Double netDownloadSpeed; // 网络下载速率（MB/s）

    @Field("net_upload_speed")
    private Double netUploadSpeed; // 网络上传速率（MB/s）

    @Field("net_speed_unit")
    private String netSpeedUnit; // 网络速率单位（MB/s）

    @Field("net_total_in")
    private Double netTotalIn; // 网络累计入站流量（MB）

    @Field("net_total_out")
    private Double netTotalOut; // 网络累计出站流量（MB）

    //------------------- 系统基础信息 -------------------

    @Field("os_version")
    private String osVersion; // 操作系统版本

    @Field("server_ip")
    private String serverIp; // 服务器IP地址

    @Field("system_up_time")
    private String systemUpTime; // 系统运行时长
    //-------------------数据产生时的预警阈值 -------------------

    @Field("cpu_threshold")
    private double cpuThreshold; // 当时的CPU预警阈值

    @Field("memory_threshold")
    private double memoryThreshold; // 当时的内存预警阈值

    @Field("disk_threshold")
    private double diskThreshold; // 当时的磁盘预警阈值

    @Field("jvm_threshold")
    private double jvmThreshold; // 当时的JVM预警阈值

    // 新增：临时存储预警状态（不映射数据库）
    @Transient // 标识该字段不存入数据库
    private boolean hasWarning;

    @Transient
    private String warningMsg;
}
