package com.yu.yupicture.modle.dto.audit;

import lombok.Data;

import java.util.List;

@Data
public class ReportDataDTO {
    private OverviewDTO overview;             // 总览数据
    private List<RoleStatDTO> roleStats;      // 角色统计
    private List<ModuleStatDTO> moduleStats;  // 模块统计
    private String generateTime;              // 数据生成时间
}