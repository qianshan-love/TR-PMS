package com.yu.yupicture.modle.entity;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户操作日志实体类（与Go的Logs结构体字段完全对齐）
 * 映射MongoDB的"logs"集合
 */
@Data
@Document(collection = "logs") // 对应Go中操作的"logs"集合
public class Logs implements Serializable {
    private static final long serialVersionUID = -6981706636025851237L;
    // 用户unionid（与Go的UserId字段对应）
    private String userId;

    // 行为：0=文件上传，1=文件下载，2=文件召回（与Go的Action字段对应）
    private String action;

    // 行为时间（与Go的ActionTime对应，MongoDB中存储为日期类型）
    private LocalDateTime actionTime;

    // 文件名称（与Go的FileName对应）
    private String fileName;

    // 文件所属类别（与Go的FileCategory对应）
    private String fileCategory;
}