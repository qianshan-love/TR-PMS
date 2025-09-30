package com.yu.yupicture.modle.dto.audit;

import lombok.Data;

import java.util.List;

@Data
public class ModuleStatDTO {
    private String moduleName;      // 模块名称
    private long opCount;           // 操作次数
    private double proportion;      // 占比（%）
    private List<String> apiExamples; // 接口示例
}
