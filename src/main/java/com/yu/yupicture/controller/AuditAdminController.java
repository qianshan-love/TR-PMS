package com.yu.yupicture.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.yu.yupicture.annotation.AdminApiLog;
import com.yu.yupicture.common.BaseResponse;
import com.yu.yupicture.common.ResultUtils;
import com.yu.yupicture.modle.dto.admin.LogQueryDTO;
import com.yu.yupicture.modle.dto.audit.ReportDataDTO;
import com.yu.yupicture.modle.vo.AdminApiLogVO;
import com.yu.yupicture.modle.vo.PageVO;
import com.yu.yupicture.service.AuditAdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@RequestMapping("/audit")
@RestController
@Slf4j
@SaCheckLogin
public class AuditAdminController {
    @Resource
    private AuditAdminService auditAdminService;

    /**
     * 审计管理员查询日志列表
     * @param queryDTO
     * @return
     */
    @PostMapping("/query/log")
    @SaCheckPermission("audit:query")
    @AdminApiLog(module = "audit-审计日志管理", apiName = "queryAdminLog-查询审计日志")
    public BaseResponse<PageVO<AdminApiLogVO>> queryAdminLog(@RequestBody LogQueryDTO queryDTO) {
        PageVO<AdminApiLogVO> pageVO = auditAdminService.queryAdminApiLog(queryDTO);
        return ResultUtils.success(pageVO);
    }

    /**
     * 审计管理员查询日志统计
     * @param startTime
     * @param endTime
     * @param includeExamples
     * @return
     */
    @GetMapping("/get/statistics")
    @SaCheckPermission("audit:get")
    @AdminApiLog(module = "audit-审计日志管理", apiName = "getStatistics-审计日志报告")
    public BaseResponse<ReportDataDTO> getStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "true") boolean includeExamples) {

        ReportDataDTO data = auditAdminService.getStatistics(startTime, endTime, includeExamples);
        return ResultUtils.success(data);
    }

}
