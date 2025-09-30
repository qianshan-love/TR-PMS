package com.yu.yupicture.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.RandomUtil;
import com.mongodb.client.result.DeleteResult;
import com.yu.yupicture.annotation.AdminApiLog;
import com.yu.yupicture.common.BaseResponse;
import com.yu.yupicture.common.CsvUtils;
import com.yu.yupicture.common.ErrorCode;
import com.yu.yupicture.exception.BusinessException;
import com.yu.yupicture.modle.dto.PageRequest;
import com.yu.yupicture.common.ResultUtils;
import com.yu.yupicture.config.SystemConfig;
import com.yu.yupicture.exception.ThrowUtils;
import com.yu.yupicture.modle.dto.user.ApproveRequest;
import com.yu.yupicture.modle.dto.user.UserJoinGroupRequest;
import com.yu.yupicture.modle.dto.user.UserQueryRequest;
import com.yu.yupicture.modle.entity.PendingApproval;
import com.yu.yupicture.modle.entity.SystemMonitorDoc;
import com.yu.yupicture.modle.vo.PageVO;
import com.yu.yupicture.modle.vo.UserBelongVO;
import com.yu.yupicture.service.SystemAdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/system")
@Slf4j
@SaCheckLogin
public class SystemAdminController {

    @Resource
    private SystemAdminService systemAdminService;

    /**
     * 用户入组
     * @param userJoinGroupRequest
     * @param request
     * @return
     */
    @PostMapping("/joinGroup")
    @SaCheckPermission("system:add")
    @AdminApiLog(module = "system-用户管理", apiName = "joinGroup-用户入组")
    public BaseResponse<Boolean> joinGroup(@RequestBody UserJoinGroupRequest userJoinGroupRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userJoinGroupRequest == null, ErrorCode.PARAMS_ERROR);
        boolean ok = systemAdminService.joinGroup(userJoinGroupRequest);
        return ResultUtils.success(ok);
    }

    /**
     * 系统管理员分页查询待审核用户列表
     * @param queryRequest
     * @return
     */
    @GetMapping("/page/list/review")
    @SaCheckPermission("system:query")
    @AdminApiLog(module = "system-用户管理", apiName = "queryPendingList-查询待审核用户")
    public BaseResponse<PageVO<PendingApproval>> queryPendingList(PageRequest queryRequest) {
        return ResultUtils.success(systemAdminService.queryPendingList(queryRequest));
    }

    /**
     * 系统管理员分页查询用户列表
     * @param queryRequest
     * @return
     */
    @PostMapping("/page/list/user")
    @SaCheckPermission("system:query")
    @AdminApiLog(module = "system-用户管理", apiName = "queryUserList-查询用户")
    public BaseResponse<PageVO<UserBelongVO>> queryUserList(PageRequest queryRequest) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR, "参数错误");
        PageVO<UserBelongVO> userBelongVOPageVO = systemAdminService.queryUserList(queryRequest);
        return ResultUtils.success(userBelongVOPageVO);
    }

    /**
     * 系统管理员根据条件查询用户列表
     * @param userQueryRequest
     * @return
     */
    @PostMapping("/query/user")
    @SaCheckPermission("system:query")
    @AdminApiLog(module = "system-用户管理", apiName = "queryUser-指定条件查询用户")
    public BaseResponse<List<UserBelongVO>> queryUser(UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR, "参数错误");
        List<UserBelongVO> userBelongVOList = systemAdminService.queryUser(userQueryRequest);
        return ResultUtils.success(userBelongVOList);
    }

    /**
     * 系统管理员删除用户
     * @param staffId
     * @return
     */
    @PostMapping("/delete/user")
    @SaCheckPermission("system:delete")
    @AdminApiLog(module = "system-用户管理", apiName = "deleteUser-删除用户")
    public BaseResponse<DeleteResult> deleteUser(String staffId) {
        ThrowUtils.throwIf(staffId == null, ErrorCode.PARAMS_ERROR, "参数错误");
        DeleteResult ok = systemAdminService.deleteUser(staffId);
        return ResultUtils.success(ok);
    }

    /**
     * 系统管理员审核用户入组
     * @param approveRequest
     * @return
     */
    @PostMapping("/approve/user")
    @SaCheckPermission("system:add")
    @AdminApiLog(module = "system-用户管理", apiName = "approveUser-审核用户入组")
    public BaseResponse<Boolean> approveUsers(ApproveRequest approveRequest) {
        ThrowUtils.throwIf(approveRequest == null, ErrorCode.PARAMS_ERROR, "参数错误");
        systemAdminService.approveUser(approveRequest);
        return ResultUtils.success(true);
    }

    /**
     * 系统管理员获取剩余添加用户数量
     * @return
     */
    @GetMapping("/get/addedCount")
    @SaCheckPermission("system:get")
    @AdminApiLog(module = "system-用户管理", apiName = "getAddedCount-获取剩余添加用户数量")
    public BaseResponse<Long> getAddedCount(){
        Long addedCount = systemAdminService.getAddedCount();
        log.info("SecurityAdminController::getAddedCount::获取新增数量");
        return ResultUtils.success(addedCount);
    }

    /**
     * 系统管理员获取实时监控数据（含预警判断）
     * @return
     */
    @GetMapping("/get/realTimeChartData")
    @SaCheckPermission("system:get")
    @AdminApiLog(module = "system-用户管理", apiName = "getRealTimeChartData-获取实时监控数据")
    public BaseResponse<Map<String, Object>> getSystemData() {
        // 直接返回采集到的实时数据
        Map<String, Object> realTimeChartData = systemAdminService.getSystemData();
        return ResultUtils.success(realTimeChartData);
    }

    /**
     * 按天查询监控数据（基础分页，使用自定义PageRequest）
     */
    @GetMapping("/query/byDay")
    @SaCheckPermission("system:query")
    @AdminApiLog(module = "system-用户管理", apiName = "queryByDay-按天查询监控数据")
    public BaseResponse<Page<SystemMonitorDoc>> querySystemDataByDay(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date queryDate,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<SystemMonitorDoc> systemMonitorDocs = systemAdminService.queryByDay(queryDate, pageNum, pageSize);
        return ResultUtils.success(systemMonitorDocs);
    }

    /**
     * 按天查询监控数据（带排序，复用自定义PageRequest中的排序字段）
     */
    @GetMapping("/query/byDayWithSort")
    @SaCheckPermission("system:query")
    @AdminApiLog(module = "system-用户管理", apiName = "queryByDayWithSort-按天查询监控数据（带排序）")
    public BaseResponse<Page<SystemMonitorDoc>> querySystemDataByDayWithSort(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date queryDate,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(defaultValue = "metric_timestamp") String sortField,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Page<SystemMonitorDoc> systemMonitorDocs = systemAdminService.queryByDayWithSort(queryDate, pageNum, pageSize, sortField, sortDir);
        return ResultUtils.success(systemMonitorDocs);
    }

    /**
     * 获取当前预警阈值配置
     */
    @GetMapping("/query/warning-threshold")
    @SaCheckPermission("system:query")
    @AdminApiLog(module = "system-用户管理", apiName = "getCurrentWarningThreshold-获取当前预警阈值配置")
    public BaseResponse<SystemConfig> getCurrentWarningThreshold() {
        SystemConfig config = systemAdminService.getCurrentWarningThreshold();
        return ResultUtils.success(config);
    }

    /**
     * 修改预警阈值（需管理员登录，通过请求头获取adminId）
     */
    @PutMapping("/update/warning-threshold")
    @SaCheckPermission("system:update")
    @AdminApiLog(module = "system-用户管理", apiName = "updateWarningThreshold-修改预警阈值")
    public BaseResponse<SystemConfig> updateWarningThreshold(@RequestBody SystemConfig config) { // 从请求头获取管理员ID（确保权限）
        SystemConfig updatedConfig = systemAdminService.updateWarningThreshold(config);
        return ResultUtils.success(updatedConfig);
    }



    /**
     * 按天下载监控数据
     * @param dateStr
     * @param nonce
     * @param response
     */
  @GetMapping("/monitor-data/download")
  @SaCheckPermission("system:get")
  @AdminApiLog(module = "system-用户管理", apiName = "downloadMonitorData-按天下载监控数据")
  public void downloadMonitorData(
          @RequestParam("date") String dateStr,
          // 新增：接收前端可选的随机参数（若前端未传，后端自动生成）
          @RequestParam(value = "nonce", required = false) String nonce,
          HttpServletResponse response) {
      // 1. 确保URL唯一性（核心优化：解决相同URL缓存问题）
      if (nonce == null || nonce.isEmpty()) {
          // 后端自动生成随机字符串（长度16位，比6位更难重复）
          nonce = RandomUtil.randomString(16);
      }

      // 2. 定义日期格式化器和编码
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

      try {
          // 3. 解析日期（原有逻辑）
          Date queryDate;
          try {
              queryDate = sdf.parse(dateStr);
          } catch (Exception e) {
              throw new BusinessException(ErrorCode.PARAMS_ERROR, "日期格式错误，请使用yyyy-MM-dd");
          }

          // 4. 查询数据（原有逻辑）
          List<SystemMonitorDoc> dataList = systemAdminService.getAllMonitorDataByDay(queryDate);
          if (dataList == null || dataList.isEmpty()) {
              throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "该天没有监控数据");
          }

          // 5. 生成CSV内容（原有逻辑）
          String csvContent = CsvUtils.convertMonitorDataToCsv(dataList);
          if (!StringUtils.hasText(csvContent)) {
              throw new BusinessException(ErrorCode.SYSTEM_ERROR, "CSV内容生成失败");
          }

          // 6. 处理文件名（增强版：覆盖更多特殊字符）
          String fileName = "监控数据_" + dateStr + "_" + nonce + ".csv"; // 用nonce确保文件名唯一
          // 替换所有非字母数字的字符（比手动替换更全面）
          String safeFileName = fileName.replaceAll("[^a-zA-Z0-9_\\u4e00-\\u9fa5（）.-]", "_");

          // 7. 设置响应头（针对国产浏览器增强）
          response.setCharacterEncoding("UTF-8");
          // 兼容MIME类型：同时声明csv和通用二进制类型
          response.setContentType("text/csv; charset=UTF-8");
          response.setHeader("Content-Type", "application/octet-stream; charset=UTF-8");

          // 8. 文件名编码（增强生僻字支持）
          String encodedFileName = URLEncoder.encode(safeFileName, StandardCharsets.UTF_8.name())
                  .replace("+", "%20");
          // 仅保留UTF-8编码（现代国产浏览器已放弃IE内核，ISO-8859-1反而可能导致生僻字丢失）
          String contentDisposition = String.format(
                  "attachment; filename*=UTF-8''%s",
                  encodedFileName
          );
          response.setHeader("Content-Disposition", contentDisposition);

          // 9. 安全头补充（适配统信UOS/奇安信的安全策略）
          response.setHeader("X-Content-Type-Options", "nosniff"); // 禁止MIME类型嗅探
          response.setHeader("X-XSS-Protection", "1; mode=block"); // 防御XSS
          response.setHeader("X-Frame-Options", "SAMEORIGIN"); // 防止点击劫持

          // 10. 缓存控制（更严格的策略）
          response.setHeader("Pragma", "no-cache");
          response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
          response.setDateHeader("Expires", 0); // 立即过期，不缓存

          // 11. 写入响应流（新增Content-Length，避免部分浏览器下载中断）
          byte[] csvBytes = csvContent.getBytes(StandardCharsets.UTF_8);
          response.setContentLength(csvBytes.length); // 明确文件大小
          try (ServletOutputStream out = response.getOutputStream()) { // 用字节流而非字符流，避免编码转换问题
              out.write(csvBytes);
              out.flush();
          }

      } catch (BusinessException e) {
          throw e;
      } catch (Exception e) {
          log.error("下载监控数据失败（原始错误）", e);
          try {
              response.setContentType("text/plain; charset=UTF-8");
              response.getWriter().write("文件下载失败，请稍后重试");
          } catch (IOException ex) {
              log.error("写入错误提示失败", ex);
          }
          throw new BusinessException(ErrorCode.OPERATION_ERROR, "下载失败，请重试");
      }
  }

}
