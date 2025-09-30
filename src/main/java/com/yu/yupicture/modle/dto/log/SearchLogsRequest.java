package com.yu.yupicture.modle.dto.log;

import lombok.Data;

/**
 * 日志查询条件实体类（与Go的SearchLogs结构体字段完全对齐）
 */
@Data
public class SearchLogsRequest {
    // 开始时间（时间戳，单位：毫秒，与Go的StartTime对应）
    private Long startTime;

    // 结束时间（时间戳，单位：毫秒，与Go的EndTime对应）
    private Long endTime;

    // 排序方向：1=正序（按actionTime升序），-1=倒序（按actionTime降序）（与Go的Sort对应）
    private Byte sort;

    // 返回日志个数（与Go的Limit对应）
    private Byte limit;

    // 跳过日志数（与Go的Skip对应）
    private Integer skip;
}
