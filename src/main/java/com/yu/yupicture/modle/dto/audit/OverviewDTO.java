package com.yu.yupicture.modle.dto.audit;

import lombok.Data;

@Data
public class OverviewDTO {
    private long totalOpCount;  // 总操作次数
    private String statCycle;   // 统计周期描述
}
