package com.yu.yupicture.modle.entity;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件自动检测结果实体类（与Go的AutoDetectResult结构体完全对齐）
 * 映射MongoDB的"detectresults"集合
 */
@Data
@Document(collection = "detectresults") // 对应Go中操作的"detectresults"集合
public class AutoDetectResult implements Serializable {
    private static final long serialVersionUID = 784148939136559399L;
    // 对应Go的Id字段（MongoDB的主键_id）
    @Field("_id") // 映射bson中的"_id"
    private String id;

    // 文件名称（对应Go的FileName）
    private String fileName;

    // 创建者ID（对应Go的CreateUser）
    private String createUser;

    // 创建时间（对应Go的CreateTime，time.Time→LocalDateTime）
    private LocalDateTime createTime;

    // 文件ID（对应Go的FileId）
    private String fileId;

    // 所含关键词（对应Go的Keywords，[]string→List<String>）
    private List<String> keywords;

    // 检测时间（对应Go的DetectTime，time.Time→LocalDateTime）
    private LocalDateTime detectTime;

    // 是否被删除（对应Go的IsDelete）
    private Boolean isDelete;

    // 所属组（对应Go的BelongGroup）
    private String belongGroup;
}