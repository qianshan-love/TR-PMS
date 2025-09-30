package com.yu.yupicture.modle.dto.admin;

import lombok.Data;

import java.util.Date;

@Data
public class ReportGenerateDTO {
    private Date startTime; // 报告时间范围开始
    private Date endTime; // 报告时间范围结束
    private String format; // 报告格式（excel/pdf）
}
