package com.yu.yupicture.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mongodb.client.result.DeleteResult;
import com.yu.yupicture.annotation.AdminApiLog;
import com.yu.yupicture.common.BaseResponse;
import com.yu.yupicture.common.ErrorCode;
import com.yu.yupicture.modle.dto.PageRequest;
import com.yu.yupicture.common.ResultUtils;
import com.yu.yupicture.exception.ThrowUtils;
import com.yu.yupicture.modle.dto.admin.LogQueryDTO;
import com.yu.yupicture.modle.dto.log.SearchLogsRequest;
import com.yu.yupicture.modle.entity.AutoDetectResult;
import com.yu.yupicture.modle.entity.Logs;
import com.yu.yupicture.modle.entity.SensitiveWordStore;
import com.yu.yupicture.modle.vo.AdminApiLogVO;
import com.yu.yupicture.modle.vo.PageVO;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.yu.yupicture.service.SecurityAdminService;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Base64;
import java.util.List;

@RequestMapping("/security")
@RestController
@Slf4j
@SaCheckLogin
public class SecurityAdminController {

    @Resource
    private SecurityAdminService securityAdminService;

    @Value("${grpc.server.address:localhost:50051}")
    private String grpcServerAddress;

    // 最大消息长度（10MB）
    private static final int MAX_MESSAGE_LENGTH = 10 * 1024 * 1024;
    /**
     * 安全管理员添加敏感词
     * @param sensitiveWordStore
     * @return
     */
    @PostMapping("/add/sensitiveWord")
    @SaCheckPermission("security:add")
    @AdminApiLog(module = "sensitive_word-敏感词管理", apiName = "addSensitiveWord-添加敏感词")
    public BaseResponse<SensitiveWordStore> addSensitiveWord(@RequestBody SensitiveWordStore sensitiveWordStore){
        ThrowUtils.throwIf(sensitiveWordStore == null, ErrorCode.PARAMS_ERROR,"敏感词不能为空");
        //添加敏感词
        SensitiveWordStore sensitiveWordStore1 = securityAdminService.addSensitiveWord(sensitiveWordStore);
        log.info("SecurityAdminController::addSensitiveWord::添加敏感词");
        return ResultUtils.success(sensitiveWordStore1);
    }

    /**
     * 安全管理员删除敏感词
     * @param sensitiveWord
     * @return
     */
    @PostMapping("/delete/sensitiveWord")
    @SaCheckPermission("security:delete")
    @AdminApiLog(module = "sensitive_word-敏感词管理", apiName = "deleteSensitiveWord-删除敏感词")
    public BaseResponse<DeleteResult> deleteSensitiveWord(String sensitiveWord){
        ThrowUtils.throwIf(sensitiveWord == null, ErrorCode.PARAMS_ERROR,"敏感词不能为空");
        //删除敏感词
        DeleteResult result = securityAdminService.deleteSensitiveWord(sensitiveWord);
        log.info("SecurityAdminController::deleteSensitiveWord::删除敏感词");
        return ResultUtils.success(result);
    }

    /**
     * 安全管理员查询敏感词
     * @param sensitiveWord
     * @return
     */
    @PostMapping("/query/sensitiveWord")
    @SaCheckPermission("security:query")
    @AdminApiLog(module = "sensitive_word-敏感词管理", apiName = "querySensitiveWord-查询敏感词")
    public BaseResponse<SensitiveWordStore> querySensitiveWord(String sensitiveWord){
        ThrowUtils.throwIf(sensitiveWord == null, ErrorCode.PARAMS_ERROR,"敏感词不能为空");
        //查询敏感词
        SensitiveWordStore sensitiveWordStore = securityAdminService.querySensitiveWord(sensitiveWord);
        log.info("SecurityAdminController::querySensitiveWord::查询敏感词");
        return ResultUtils.success(sensitiveWordStore);
    }

    /**
     * 安全管理员查询敏感词列表
     * @param pageRequest
     * @return
     */
    @PostMapping("/page/list/sensitiveWord")
    @SaCheckPermission("security:query")
    @AdminApiLog(module = "sensitive_word-敏感词管理", apiName = "querySensitiveWordByPage-查询敏感词列表")
    public BaseResponse<PageVO<SensitiveWordStore>> querySensitiveWordByPage(PageRequest pageRequest){
        ThrowUtils.throwIf(pageRequest == null, ErrorCode.PARAMS_ERROR,"参数错误");
        //查询敏感词
        PageVO<SensitiveWordStore> sensitiveWordStorePageVO = securityAdminService.querySensitiveWordByPage(pageRequest);
        log.info("SecurityAdminController::querySensitiveWordByPage::获取敏感词列表");
        return ResultUtils.success(sensitiveWordStorePageVO);
    }

    /**
     * 查询用户操作日志接口
     * 前端通过GET请求传递参数（适合查询场景）
     */
    @GetMapping("/query")
    @SaCheckPermission("security:query")
    @AdminApiLog(module = "logs-日志管理", apiName = "queryLogs-查询用户操作日志")
    public BaseResponse<List<Logs>> queryLogs(@RequestBody SearchLogsRequest search) {
        // 直接调用Service层，由Service处理业务逻辑
        List<Logs> logs = securityAdminService.getLogs(search);
        return ResultUtils.success(logs);
    }

    /**
     * 获取指定分组的文件检测结果接口
     */
    @GetMapping("/results")
    @SaCheckPermission("security:query")
    @AdminApiLog(module = "detectresults-文件检测结果管理", apiName = "queryDetectResults-查询文件检测结果")
    public BaseResponse<List<AutoDetectResult>> getDetectResults(
            @RequestParam("belongGroup") String belongGroup) {
        // 调用Service层处理业务
        List<AutoDetectResult> results = securityAdminService.getDetectResults(belongGroup);
        return ResultUtils.success(results);
    }
}
