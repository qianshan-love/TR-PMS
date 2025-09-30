package com.yu.yupicture.service;

import com.yu.yupicture.modle.dto.admin.LogQueryDTO;
import com.yu.yupicture.modle.dto.audit.ReportDataDTO;
import com.yu.yupicture.modle.vo.AdminApiLogVO;
import com.yu.yupicture.modle.vo.PageVO;

import java.time.LocalDateTime;

public interface AuditAdminService {
    /**
     * 安全管理员查询日志列表
     * @param queryDTO
     * @return
     */
    PageVO<AdminApiLogVO> queryAdminApiLog(LogQueryDTO queryDTO);

    ReportDataDTO getStatistics(LocalDateTime startTime, LocalDateTime endTime, boolean includeExamples);
}
