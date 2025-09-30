package com.yu.yupicture.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.mongodb.client.result.DeleteResult;

import com.yu.yupicture.common.*;
import com.yu.yupicture.config.SystemConfig;
import com.yu.yupicture.dao.SystemAdminDao;
import com.yu.yupicture.exception.BusinessException;
import com.yu.yupicture.exception.ThrowUtils;
import com.yu.yupicture.modle.dto.PageRequest;
import com.yu.yupicture.modle.dto.user.ApproveRequest;
import com.yu.yupicture.modle.dto.user.UserJoinGroupRequest;
import com.yu.yupicture.modle.dto.user.UserQueryRequest;
import com.yu.yupicture.modle.entity.Administrator;
import com.yu.yupicture.modle.entity.PendingApproval;
import com.yu.yupicture.modle.entity.SystemMonitorDoc;
import com.yu.yupicture.modle.entity.UserBelong;
import com.yu.yupicture.modle.vo.AdminVO;
import com.yu.yupicture.modle.vo.PageVO;
import com.yu.yupicture.modle.vo.UserBelongVO;
import com.yu.yupicture.service.SystemAdminService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.software.os.OperatingSystem;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SystemAdminServiceImpl implements SystemAdminService {

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private SystemAdminDao systemAdminDao;

    private final MeterRegistry meterRegistry;

    // Oshi硬件/系统信息（仅初始化一次，复用实例）
    private final HardwareAbstractionLayer hardware;
    private final OperatingSystem operatingSystem;
    private final GlobalMemory globalMemory;
    private volatile NetworkIF validNetworkIF;

    // 构造方法：初始化Oshi实例和外部依赖
    public SystemAdminServiceImpl(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        SystemInfo systemInfo = new SystemInfo();
        this.hardware = systemInfo.getHardware();
        this.operatingSystem = systemInfo.getOperatingSystem();
        this.globalMemory = hardware.getMemory();
        this.validNetworkIF = findValidNetworkIF();
    }

    // 服务启动时初始化TTL索引（调用DAO层方法）
    @PostConstruct
    public void init() {
        systemAdminDao.initTTLIndex(); // 委托DAO层处理索引初始化
        systemAdminDao.initDefaultWarningConfig(); // 初始化默认预警配置
    }

    // ===============================================核心业务逻辑===================================================

    /**
     * 入组申请
     * @param userJoinGroupRequest
     * @return
     */
    @Override
    public boolean joinGroup(UserJoinGroupRequest userJoinGroupRequest) {
        // 1. 基础参数校验（非空+时间合法性）
        ThrowUtils.throwIf(userJoinGroupRequest == null, ErrorCode.PARAMS_ERROR, "入组申请不能为空");
        String unionId = userJoinGroupRequest.getUnionId();
        Date expireTime = userJoinGroupRequest.getExpireTime();

        // 核心字段非空校验（unionId是用户唯一标识，expireTime是必要业务字段）
        ThrowUtils.throwIf(StringUtils.isBlank(unionId), ErrorCode.PARAMS_ERROR, "用户唯一标识不能为空");

        // 2. 核心控制：同一用户对固定组的重复申请校验（查询待审核表中该用户的历史申请）
        PendingApproval existingPending = systemAdminDao.findByUnionId(unionId);

        if (existingPending != null) {
            // 2.1 存在历史申请，根据状态处理
            String status = existingPending.getStatus();
            // 若状态为待审核：不允许重复申请
            if (status.equals("pending")) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "已提交入组申请，正在审核中，请勿重复申请");
            }
            // 若状态为已通过：不允许重复申请（固定组已加入）
            else if (status.equals("approved")) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "已通过审核加入组，请勿重复申请");
            }
            // 若状态为已拒绝：允许重新申请（更新为待审核状态，刷新申请时间）
            else if (status.equals("rejected")) {
                existingPending.setStatus("pending");
                existingPending.setCreatedAt(new Date()); // 刷新申请时间为当前时间，便于管理员识别最新申请
                systemAdminDao.save(existingPending);
                return true;
            }
        }

        // 3. 无历史申请，创建新申请（固定组无需指定组名，或直接使用请求中的组名）
        PendingApproval newPending = new PendingApproval();
        newPending.setUnionID(unionId);
        newPending.setMpID(userJoinGroupRequest.getMpId());
        newPending.setNickname(userJoinGroupRequest.getNickname());
        newPending.setAvatarURL(userJoinGroupRequest.getAvatarUrl());
        newPending.setBelongGroup(userJoinGroupRequest.getBelongGroup()); // 固定组，直接使用请求中的值
        newPending.setReviewerID(userJoinGroupRequest.getReviewerId());
        newPending.setRegisterTime(userJoinGroupRequest.getRegisterTime());
        newPending.setExpireTime(expireTime);
        newPending.setIsSuper(userJoinGroupRequest.getIsSuper());
        newPending.setCreatedAt(new Date());
        newPending.setStatus("pending"); // 使用枚举避免硬编码
        systemAdminDao.save(newPending);

        return true;
    }

    /**
     * 查询待审核用户列表
     * @param queryRequest
     * @return
     */
    @Override
    public PageVO<PendingApproval> queryPendingList(PageRequest queryRequest) {
        // 1. 解析分页参数和校验
        int pageNum = queryRequest.getPage();
        int pageSize = queryRequest.getSize();
        String status = queryRequest.getStatus(); // 假设PageRequest中有status字段

        // 校验分页参数合法性
        ThrowUtils.throwIf(pageNum < 1 || pageSize < 1 || pageSize > 100,
                ErrorCode.PARAMS_ERROR, "分页参数不合法");

        // 2. 【使用DAO层】执行分页查询
        // 替换原来的所有MongoTemplate操作
        PageVO<PendingApproval> pageVO = systemAdminDao.findPendingByStatusWithPage(
                status,
                pageNum,
                pageSize,
                "createdAt", // 排序字段
                Sort.Direction.DESC // 排序方向
        );

        return pageVO;
    }

    /**
     * 查询用户组列表
     * @param queryRequest
     * @return
     */
    @Override
    public PageVO<UserBelongVO> queryUserList(PageRequest queryRequest) {
        // 1. 解析分页参数和校验
        int pageNum = queryRequest.getPage();
        int pageSize = queryRequest.getSize();

        // 校验分页参数合法性
        ThrowUtils.throwIf(pageNum < 1 || pageSize < 1,
                ErrorCode.PARAMS_ERROR, "分页参数不合法");

        // 2. 执行分页查询
        PageVO<UserBelong> userBelongPage = systemAdminDao.findUsersByPage(pageNum, pageSize);

        // 3. 转换为VO对象
        List<UserBelongVO> userBelongVOList = getUserBelongVOList(userBelongPage.getList());

        // 4. 重新封装分页结果
        PageVO<UserBelongVO> pageVO = new PageVO<>();
        pageVO.setList(userBelongVOList);
        pageVO.setTotal(userBelongPage.getTotal());
        pageVO.setPageNum(userBelongPage.getPageNum());
        pageVO.setPageSize(userBelongPage.getPageSize());
        pageVO.setTotalPage(userBelongPage.getTotalPage());

        return pageVO;
    }

    /**
     * 查询用户信息
     * @param userQueryRequest
     * @return
     */
    @Override
    public List<UserBelongVO> queryUser(UserQueryRequest userQueryRequest) {

        List<UserBelong> userBelongList = systemAdminDao.findUsers(userQueryRequest);
        if (userBelongList.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"用户不存在");
        }
        List<UserBelongVO> userBelongVOList = getUserBelongVOList(userBelongList);
        return userBelongVOList;
    }

    /**
     * 用户退组
     * @param staffId
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeleteResult deleteUser(String staffId) {

        ThrowUtils.throwIf(staffId == null,ErrorCode.PARAMS_ERROR,"参数错误");

        // 查询用户是否存在
        UserBelong userBelong = systemAdminDao.findByStaffId(staffId);
        if (userBelong == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"用户不存在");
        }

        String adminId = userBelong.getAdminId();

        // 查询管理员是否存在
        Administrator administrator = systemAdminDao.findByAdminId(adminId);
        if (administrator == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"管理员不存在");
        }

        // 删除用户
        DeleteResult remove = systemAdminDao.deleteUserByStaffId(staffId);

        try {
            // 4. 使用原子操作恢复管理员配额
            systemAdminDao.incrementAddedCount(adminId);

            // 5. 更新用户状态
            systemAdminDao.updateStatus(staffId);
        } catch (Exception e) {
            // 如果更新配额或状态失败，记录日志并抛出异常，事务会因@Transactional而回滚，用户删除操作也会撤销
            log.error("删除用户后更新管理员配额或状态失败，事务已回滚。staffId: {}, adminId: {}", staffId, adminId, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除用户失败，请重试");
        }
        return remove;
    }

    /**
     * 系统管理员审核用户入组
     * @param approveRequest
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approveUser(ApproveRequest approveRequest) {
        String unionID = approveRequest.getUnionID();
        String belongGroup = approveRequest.getBelongGroup();
        String reviewerID = approveRequest.getReviewerID();
        Boolean approve = approveRequest.getApprove();

        // 1. 基础参数校验
        ThrowUtils.throwIf(StringUtils.isBlank(unionID) || StringUtils.isBlank(belongGroup) || StringUtils.isBlank(reviewerID),
                ErrorCode.PARAMS_ERROR, "参数不能为空");

        // 2. 定位待审核记录（精准匹配：用户+组+待审核状态）
        PendingApproval pendingApproval = systemAdminDao.findOneByUnionIDAndGroupAndStatus(unionID, belongGroup, "pending");
        ThrowUtils.throwIf(pendingApproval == null, ErrorCode.PARAMS_ERROR, "待审核申请不存在或已处理");

        // 3. 拒绝逻辑（单独处理，不涉及配额扣减）
        if (!approve) {
            pendingApproval.setStatus("rejected");
            pendingApproval.setReviewerID(reviewerID);
            pendingApproval.setCreatedAt(new Date());
            systemAdminDao.save(pendingApproval);
            return true;
        }
        // 4.查找管理员并检查管理员是否还有剩余添加次数
        Administrator updatedAdmin = systemAdminDao.DecrementAddedCount(reviewerID, 0);
        // 4.1校验扣减结果（若返回null，说明管理员不存在/角色不符/配额不足）
        ThrowUtils.throwIf(updatedAdmin == null, ErrorCode.PARAMS_ERROR,
                "审核失败：管理员不存在、无权限或可用添加次数不足");

        // 5. 更新待审核记录状态
        pendingApproval.setStatus("approved");
        pendingApproval.setReviewerID(reviewerID);
        pendingApproval.setCreatedAt(new Date());
        mongoTemplate.save(pendingApproval);

        // 6. 创建用户组关系记录
        UserBelong userBelong = new UserBelong();
        userBelong.setAdminId(reviewerID); // 使用当前审核人ID作为组管理员
        userBelong.setStaffId(unionID);
        userBelong.setBelongGroup(belongGroup);
        userBelong.setStaffName(pendingApproval.getNickname());
        userBelong.setStaffImage(pendingApproval.getAvatarURL());
        userBelong.setExpireTime(pendingApproval.getExpireTime()); // 同步到期时间
        mongoTemplate.save(userBelong);

        return true;
    }

    /**
     * 获取系统管理员剩余添加用户数量
     * @return
     */
    @Override
    public long getAddedCount() {

        AdminVO administrator = (AdminVO) StpUtil.getSession().get(UserConstant.ADMIN);
        String id = administrator.getId();
        Administrator administrator1 = mongoTemplate.findById(id, Administrator.class);
        long num = administrator1.getAddedCount();
        return num;
    }


    /**
     * 获取监控数据
     * @return
     */
    @Override
    public Map<String, Object> getSystemData() {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> chart = new LinkedHashMap<>();
        Map<String, Object> warnings = new LinkedHashMap<>();

        // 步骤1：采集指标并封装实体（调用内部方法）
        SystemMonitorDoc monitorDoc = collectMonitorDoc();
        long currentTimestamp = monitorDoc.getMetricTimestamp();

        // 步骤2：组装图表数据 + 预警判断
        assembleChartDataAndCheckWarning(monitorDoc, chart, warnings);

        // 步骤3：若触发预警，调用DAO层保存数据
        if (!warnings.isEmpty()) {
            systemAdminDao.saveWithDuplicateCheck(monitorDoc, "预警触发存储");
        }

        // 步骤4：组装最终响应
        assembleResponse(result, chart, warnings, monitorDoc, currentTimestamp);

        return result;
    }

    /**
     * 定时存储监控数据
     */
    @Override
    @Scheduled(cron = "0 0/30 * * * ?")
    public void scheduledStoreMonitorData() {

        try {
            SystemMonitorDoc monitorDoc = collectMonitorDoc();
            systemAdminDao.saveWithDuplicateCheck(monitorDoc, "定时存储");
        } catch (Exception e) {
            log.error("定时存储监控数据失败", e);
        }
    }

    /**
     * 采集监控数据
     * @return
     */
    @Override
    public SystemMonitorDoc collectMonitorDoc() {
        SystemMonitorDoc doc = new SystemMonitorDoc();
        long currentTimestamp = System.currentTimeMillis();

        // 1. 设置基础时间字段（生成时间 + 过期时间）
        doc.setMetricTimestamp(currentTimestamp);
        doc.setExpireTime(currentTimestamp + SystemConstant.DEFAULT_RETENTION_MILLIS); // 默认15天过期
        log.debug("监控数据生成：时间戳={}，过期时间戳={}（默认15天后）", currentTimestamp, doc.getExpireTime());

        // 2. 采集CPU指标
        doc.setCpuValue(calculateCpuUsage());
        doc.setCpuUnit("%");

        // 3. 采集系统内存指标
        Map<String, Long> memoryData = calculateMemory();
        doc.setSysMemTotal(memoryData.get("total"));
        doc.setSysMemUsed(memoryData.get("used"));
        doc.setSysMemFree(memoryData.get("free"));
        doc.setSysMemCache(memoryData.get("cache"));
        doc.setMemUnit("MB");

        // 4. 采集磁盘指标
        Map<String, Double> diskData = calculateDisk();
        doc.setDiskTotal(diskData.getOrDefault("total", -1.0));
        doc.setDiskUsed(diskData.getOrDefault("used", -1.0));
        doc.setDiskUnit("GB");

        // 5. 采集JVM指标
        Map<String, Long> jvmData = calculateJvm();
        doc.setJvmUsed(jvmData.get("used"));
        doc.setJvmMax(jvmData.get("max"));
        doc.setJvmUnit("MB");

        // 6. 采集网络指标
        Map<String, Object> netData = calculateNetwork();
        doc.setNetDownloadSpeed((Double) netData.get("downloadSpeed"));
        doc.setNetUploadSpeed((Double) netData.get("uploadSpeed"));
        doc.setNetSpeedUnit("MB/s");
        doc.setNetTotalIn((Double) netData.get("netIn"));
        doc.setNetTotalOut((Double) netData.get("netOut"));

        // 7. 采集系统基础信息
        doc.setOsVersion(osStr());
        doc.setServerIp(systemIpStr());
        doc.setSystemUpTime(upTimeStr());

        // 8. 采集数据产生时的阈值，并保存到doc中
        SystemConfig currentConfig = getCurrentWarningThreshold(); // 此时的“当前配置”就是数据产生时的配置
        doc.setCpuThreshold(currentConfig.getCpuWarningThreshold());
        doc.setMemoryThreshold(currentConfig.getMemoryWarningThreshold());
        doc.setDiskThreshold(currentConfig.getDiskWarningThreshold());
        doc.setJvmThreshold(currentConfig.getJvmWarningThreshold());

        return doc;
    }

    /**
     * 根据日期查询监控数据（分页+排序）
     * @param queryDate
     * @param pageNum
     * @param pageSize
     * @param sortField
     * @param sortDir
     * @return
     */
    @Override
    public Page<SystemMonitorDoc> queryByDayWithSort(Date queryDate, Integer pageNum, Integer pageSize,
                                                     String sortField, String sortDir) {
        // 基础参数校验
        validateBaseParams(queryDate, pageNum, pageSize);

        // 调用DAO层查询
        return systemAdminDao.findByDayWithSort(queryDate, pageNum, pageSize,
                sortField, sortDir);
    }

    /**
     * 根据日期查询监控数据（分页）
     * @param queryDate
     * @param pageNum
     * @param pageSize
     * @return
     */
    @Override
    public Page<SystemMonitorDoc> queryByDay(Date queryDate, Integer pageNum, Integer pageSize) {
        // 参数校验
        validateBaseParams(queryDate, pageNum, pageSize);

        // 调用DAO层查询
        return systemAdminDao.findByDay(queryDate, pageNum, pageSize);
    }

    /**
     * 获取当前预警阈值配置（供管理员查看）
     * @return 当前阈值配置
     */
    @Override
    public SystemConfig getCurrentWarningThreshold() {
        SystemConfig config = systemAdminDao.getWarningThresholdConfig();
        if (config == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "预警阈值配置不存在，请重启系统重试");
        }
        return config;
    }

    /**
     * 管理员手动修改预警阈值
     * @param config 待修改的阈值（支持部分字段，如仅传cpuWarningThreshold）
     * @return 修改后的完整配置
     */
    @Override
    public SystemConfig updateWarningThreshold(SystemConfig config) {
        // 1. 参数校验
        ThrowUtils.throwIf(config == null, ErrorCode.PARAMS_ERROR, "修改参数不能为空");

        // 2. 阈值范围校验（0~100，null表示不修改该字段）
        if (config.getCpuWarningThreshold() != null) {
            ThrowUtils.throwIf(config.getCpuWarningThreshold() < 0 || config.getCpuWarningThreshold() > 100,
                    ErrorCode.PARAMS_ERROR, "CPU预警阈值必须在0~100之间");
        }
        if (config.getMemoryWarningThreshold() != null) {
            ThrowUtils.throwIf(config.getMemoryWarningThreshold() < 0 || config.getMemoryWarningThreshold() > 100,
                    ErrorCode.PARAMS_ERROR, "内存预警阈值必须在0~100之间");
        }
        if (config.getDiskWarningThreshold() != null) {
            ThrowUtils.throwIf(config.getDiskWarningThreshold() < 0 || config.getDiskWarningThreshold() > 100,
                    ErrorCode.PARAMS_ERROR, "磁盘预警阈值必须在0~100之间");
        }
        if (config.getJvmWarningThreshold() != null) {
            ThrowUtils.throwIf(config.getJvmWarningThreshold() < 0 || config.getJvmWarningThreshold() > 100,
                    ErrorCode.PARAMS_ERROR, "JVM预警阈值必须在0~100之间");
        }

        // 3. 调用DAO层更新配置
        return systemAdminDao.updateWarningThreshold(config);
    }

    /**
     * 下载某一天的监控数据（CSV格式）
     * @param dateStr 日期字符串（格式：yyyy-MM-dd）
     * @param response HTTP响应对象（用于输出文件）
     */
    @Override
    public void downloadMonitorDataByDay(String dateStr,  HttpServletResponse response) {
        // 1. 权限校验
        // 2. 解析日期并计算时间范围（当天00:00:00至23:59:59）
        Date queryDate = parseDate(dateStr);
        long startTime = getDayStartTime(queryDate);  // 当天开始时间戳
        long endTime = getDayEndTime(queryDate);      // 当天结束时间戳

        // 3. 使用MongoTemplate查询该天的所有监控数据
        List<SystemMonitorDoc> allMonitorData = this.queryAllMonitorDataByTimeRange(startTime, endTime);
        ThrowUtils.throwIf(allMonitorData.isEmpty(),
                ErrorCode.NOT_FOUND_ERROR, "该日期没有监控数据");

        // 4. 转换为CSV格式
        String csvContent = CsvUtils.convertMonitorDataToCsv(allMonitorData);

        // 5. 输出到响应流，触发下载
        writeCsvToResponse(csvContent, dateStr, response);
    }

    /**
     * 获取某一天的所有监控数据（带预警状态计算）
     */
    @Override
    public List<SystemMonitorDoc> getAllMonitorDataByDay(Date queryDate) {
        // 1. 原有逻辑：查询数据库数据
        List<SystemMonitorDoc> dataList = new ArrayList<>();
        int pageNum = 1;
        int pageSize = 200;
        while (true) {
            Page<SystemMonitorDoc> page = queryByDay(queryDate, pageNum, pageSize);
            List<SystemMonitorDoc> currentPageData = page.getContent();
            if (currentPageData.isEmpty()) break;
            dataList.addAll(currentPageData);
            if (pageNum >= page.getTotalPages()) break;
            pageNum++;
        }

        // 2. 新增：获取当前阈值配置
        SystemConfig warningConfig = getCurrentWarningThreshold();

        // 3. 新增：为每条数据添加预警状态（需在SystemMonitorDoc中新增临时字段，或用Map包装）
        // 方式1：若允许修改SystemMonitorDoc，新增hasWarning和warningMsg字段
        for (SystemMonitorDoc doc : dataList) {
            Map<String, Object> warningStatus = calculateWarningStatus(doc);
            doc.setHasWarning((boolean) warningStatus.get("hasWarning"));
            doc.setWarningMsg((String) warningStatus.get("warningMsg"));
        }

        // 方式2：若不允许修改实体类，用List<Map>包装数据（示例略）
        return dataList;
    }

    // ===============================================私有工具方法：指标计算与响应组装===================================================

    /**
     * 基础参数校验
     */
    private void validateBaseParams(Date queryDate, Integer pageNum, Integer pageSize) {
        if (queryDate == null) {
            throw new IllegalArgumentException("查询日期不能为空");
        }
        if (pageNum < 1) {
            throw new IllegalArgumentException("页码必须≥1");
        }
        if (pageSize < 1) {
            throw new IllegalArgumentException("每页条数必须≥1");
        }
    }

    /**
     * 组装图表数据 + 预警判断（修改：从数据库获取阈值）
     */
    private void assembleChartDataAndCheckWarning(SystemMonitorDoc doc, Map<String, Object> chart, Map<String, Object> warnings) {
        // 新增：获取当前预警阈值配置
        SystemConfig warningConfig = systemAdminDao.getWarningThresholdConfig();
        if (warningConfig == null) {
            log.error("获取预警阈值配置失败，使用默认值");
            // 降级：使用原硬编码默认值，避免系统异常
            warningConfig = new SystemConfig();
        }

        // CPU指标 + 预警（替换：用配置的阈值）
        double cpuUsage = doc.getCpuValue();
        chart.put("cpuValue", cpuUsage);
        chart.put("cpuUnit", "%");
        checkWarning(warnings, "cpu", cpuUsage, warningConfig.getCpuWarningThreshold(),
                "CPU使用率过高（当前: %.1f%%，阈值: %.1f%%）");

        // 内存指标 + 预警（替换：用配置的阈值）
        chart.put("memTotalValue", doc.getSysMemTotal());
        chart.put("memUsedValue", doc.getSysMemUsed());
        chart.put("memFreeValue", doc.getSysMemFree());
        chart.put("memCacheValue", doc.getSysMemCache());
        chart.put("memUnit", "MB");
        double memoryUsage = (double) doc.getSysMemUsed() / doc.getSysMemTotal() * 100;
        checkWarning(warnings, "memory", memoryUsage, warningConfig.getMemoryWarningThreshold(),
                "内存使用率过高（当前: %.1f%%，阈值: %.1f%%）");

        // 磁盘指标 + 预警（替换：用配置的阈值）
        double diskTotal = doc.getDiskTotal();
        double diskUsed = doc.getDiskUsed();
        chart.put("diskTotalValue", diskTotal);
        chart.put("diskUsedValue", diskUsed);
        chart.put("diskUnit", "GB");
        if (diskTotal > 0 && diskUsed >= 0) {
            double diskUsage = diskUsed / diskTotal * 100;
            checkWarning(warnings, "disk", diskUsage, warningConfig.getDiskWarningThreshold(),
                    "磁盘使用率过高（当前: %.1f%%，阈值: %.1f%%）");
        } else {
            log.warn("磁盘信息获取失败，跳过预警判断");
        }

        // JVM指标 + 预警（替换：用配置的阈值）
        chart.put("jvmUsedValue", doc.getJvmUsed());
        chart.put("jvmMaxValue", doc.getJvmMax());
        chart.put("jvmUnit", "MB");
        double jvmUsage = (double) doc.getJvmUsed() / doc.getJvmMax() * 100;
        checkWarning(warnings, "jvm", jvmUsage, warningConfig.getJvmWarningThreshold(),
                "JVM内存使用率过高（当前: %.1f%%，阈值: %.1f%%）");

        // 网络指标（无预警，保留原有逻辑）
        chart.put("downloadSpeedValue", doc.getNetDownloadSpeed());
        chart.put("uploadSpeedValue", doc.getNetUploadSpeed());
        chart.put("speedUnit", "MB/s");
    }

    /**
     * 组装最终响应
     */
    private void assembleResponse(Map<String, Object> result, Map<String, Object> chart, Map<String, Object> warnings, SystemMonitorDoc doc, long currentTimestamp) {
        result.put("chart", chart);
        result.put("warnings", warnings);
        result.put("hasWarning", !warnings.isEmpty());
        // 系统基础信息
        result.put("os", doc.getOsVersion());
        result.put("systemIp", doc.getServerIp());
        result.put("upTime", doc.getSystemUpTime());
        result.put("netIn", String.format("%.1fMB", doc.getNetTotalIn()));
        result.put("netOut", String.format("%.1fMB", doc.getNetTotalOut()));
        result.put("timestamp", currentTimestamp);
    }

    /**
     * 预警判断工具方法
     */
    private void checkWarning(Map<String, Object> warnings, String type, double currentValue, double threshold, String messageFormat) {
        if (currentValue > threshold) {
            Map<String, Object> warning = new HashMap<>();
            warning.put("current", String.format("%.1f", currentValue));
            warning.put("threshold", String.format("%.1f", threshold));
            warning.put("message", String.format(messageFormat, currentValue, threshold));
            warnings.put(type, warning);
        }
    }

    /**
     * 计算CPU使用率
     */
    private double calculateCpuUsage() {
        return Optional.ofNullable(meterRegistry.find("system.cpu.usage").gauge())
                .map(g -> g.value() * 100)
                .orElse(0.0);
    }

    /**
     * 计算系统内存
     */
    private Map<String, Long> calculateMemory() {
        Map<String, Long> memory = new HashMap<>();
        long total = globalMemory.getTotal() / (long) SystemConstant.MB;
        long free = globalMemory.getAvailable() / (long) SystemConstant.MB;
        long used = total - free;
        long cache = memCacheValue();

        memory.put("total", total);
        memory.put("used", used);
        memory.put("free", free);
        memory.put("cache", cache);
        return memory;
    }

    /**
     * 计算磁盘信息
     */
    private Map<String, Double> calculateDisk() {
        Map<String, Double> disk = new HashMap<>();
        try {
            FileStore rootStore = Files.getFileStore(Paths.get(FileSystems.getDefault().getSeparator()));
            double total = rootStore.getTotalSpace() / SystemConstant.GB;
            double used = (rootStore.getTotalSpace() - rootStore.getUsableSpace()) / SystemConstant.GB;
            disk.put("total", total);
            disk.put("used", used);
        } catch (Exception e) {
            log.error("计算磁盘信息失败", e);
            disk.put("total", -1.0);
            disk.put("used", -1.0);
        }
        return disk;
    }

    /**
     * 计算JVM内存
     */
    private Map<String, Long> calculateJvm() {
        Map<String, Long> jvm = new HashMap<>();
        long used = Optional.ofNullable(meterRegistry.find("jvm.memory.used").gauge())
                .map(g -> (long) (g.value() / SystemConstant.MB))
                .orElse(0L);
        long max = Optional.ofNullable(meterRegistry.find("jvm.memory.max").gauge())
                .map(g -> (long) (g.value() / SystemConstant.MB))
                .orElse(0L);
        jvm.put("used", used);
        jvm.put("max", max);
        return jvm;
    }

    /**
     * 计算网络指标
     */
    private Map<String, Object> calculateNetwork() {
        Map<String, Object> netData = new HashMap<>();
        if (validNetworkIF == null) {
            validNetworkIF = findValidNetworkIF();
        }

        if (validNetworkIF == null) {
            netData.put("downloadSpeed", 0.0);
            netData.put("uploadSpeed", 0.0);
            netData.put("netIn", 0.0);
            netData.put("netOut", 0.0);
            return netData;
        }

        try {
            NetworkIF n = validNetworkIF;
            double netIn = n.getBytesRecv() / SystemConstant.MB;
            double netOut = n.getBytesSent() / SystemConstant.MB;
            netData.put("netIn", netIn);
            netData.put("netOut", netOut);

            // 异步计算网络速率（避免阻塞）
            CompletableFuture<Double[]> speedFuture = CompletableFuture.supplyAsync(() -> {
                long rx1 = n.getBytesRecv();
                long tx1 = n.getBytesSent();
                try {
                    TimeUnit.MILLISECONDS.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                n.updateAttributes();
                long rx2 = n.getBytesRecv();
                long tx2 = n.getBytesSent();
                double downloadSpeed = (rx2 - rx1) / SystemConstant.MB / 0.2;
                double uploadSpeed = (tx2 - tx1) / SystemConstant.MB / 0.2;
                return new Double[]{downloadSpeed, uploadSpeed};
            });

            Double[] speeds = speedFuture.get(300, TimeUnit.MILLISECONDS);
            netData.put("downloadSpeed", speeds[0]);
            netData.put("uploadSpeed", speeds[1]);

        } catch (Exception e) {
            log.warn("计算网络指标失败", e);
            netData.put("downloadSpeed", 0.0);
            netData.put("uploadSpeed", 0.0);
        }

        return netData;
    }

    /**
     * 筛选有效网络接口
     */
    private NetworkIF findValidNetworkIF() {
        List<NetworkIF> networkIFs = hardware.getNetworkIFs();
        return networkIFs.stream()
                .filter(n -> !isVirtualInterface(n.getName()))
                .filter(n -> n.getIPv4addr().length > 0 && n.getBytesRecv() > 0)
                .findFirst()
                .orElse(null);
    }

    /**
     * 判断是否为虚拟网络接口
     */
    private boolean isVirtualInterface(String name) {
        String lowerName = name.toLowerCase();
        return lowerName.startsWith("docker")
                || lowerName.startsWith("br-")
                || lowerName.startsWith("veth")
                || lowerName.startsWith("lo");
    }

    /**
     * 计算内存缓存值（区分Windows/Linux）
     */
    private long memCacheValue() {
        try {
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                return (globalMemory.getTotal() - (globalMemory.getTotal() - globalMemory.getAvailable())) / (long) SystemConstant.MB;
            } else {
                String memInfo = globalMemory.toString();
                long cache = 0;
                for (String line : memInfo.split("\n")) {
                    line = line.trim();
                    if (line.startsWith("Cached:") || line.startsWith("Buffers:")) {
                        cache += Long.parseLong(line.split("\\s+")[1]) * SystemConstant.KB;
                    }
                }
                return (long) (cache / SystemConstant.MB);
            }
        } catch (Exception e) {
            log.error("计算内存缓存失败", e);
            return -1L;
        }
    }

    /**
     * 获取操作系统版本
     */
    private String osStr() {
        return operatingSystem.toString();
    }

    /**
     * 获取服务器IP
     */
    private String systemIpStr() {
        if (validNetworkIF == null || validNetworkIF.getIPv4addr().length == 0) {
            return "获取失败";
        }
        return Arrays.toString(validNetworkIF.getIPv4addr());
    }

    /**
     * 获取系统运行时间
     */
    private String upTimeStr() {
        long uptimeSeconds = operatingSystem.getSystemUptime();
        long hours = uptimeSeconds / SystemConstant.SECONDS_PER_HOUR;
        long minutes = (uptimeSeconds % SystemConstant.SECONDS_PER_HOUR) / 60;
        return String.format("%d小时%d分钟", hours, minutes);
    }

    /**
     * 信息脱敏
     * @param administrator
     * @return
     */
    public AdminVO getAdminVO(Administrator administrator) {

        ThrowUtils.throwIf(administrator == null,ErrorCode.PARAMS_ERROR,"脱敏目标不能为空");
        AdminVO adminVO =  new AdminVO();
        BeanUtil.copyProperties(administrator,adminVO);
        return adminVO;

    }

    /**
     * 转换为VO列表
     * @param userBelongList
     * @return
     */
    public List<UserBelongVO> getUserBelongVOList(List<UserBelong> userBelongList) {
        List<UserBelongVO> userBelongVOList = userBelongList.stream().map(userBelong -> {
            UserBelongVO userBelongVO = new UserBelongVO();
            BeanUtil.copyProperties(userBelong, userBelongVO);
            return userBelongVO;
        }).collect(Collectors.toList());
        return userBelongVOList;
    }

    /**
     * 使用MongoTemplate查询指定时间范围内的所有监控数据（分页查询全量数据）
     * @param startTime 开始时间戳（毫秒）
     * @param endTime 结束时间戳（毫秒）
     * @return 全量监控数据列表
     */
    private List<SystemMonitorDoc> queryAllMonitorDataByTimeRange(long startTime, long endTime) {
        List<SystemMonitorDoc> allData = new ArrayList<>();
        int pageNum = 1;
        int pageSize = 200;  // 每次查询200条，避免内存溢出

        // 构造查询条件：metric_timestamp在[startTime, endTime]之间
        Criteria criteria = Criteria.where("metric_timestamp")
                .gte(startTime)
                .lte(endTime);
        Query query = Query.query(criteria)
                .with(Sort.by(Sort.Direction.ASC, "metric_timestamp"));  // 按时间升序

        while (true) {
            // 分页查询：设置当前页和每页条数
            Pageable pageable = org.springframework.data.domain.PageRequest.of(pageNum - 1, pageSize);// Pageable页码从0开始
            Query pageQuery = query.with(pageable);

            // 使用MongoTemplate执行查询
            List<SystemMonitorDoc> currentPageData = mongoTemplate.find(pageQuery, SystemMonitorDoc.class);
            if (currentPageData.isEmpty()) {
                break;  // 没有更多数据，退出循环
            }

            allData.addAll(currentPageData);
            pageNum++;

            // 避免无限循环（理论上不会触发，保险措施）
            if (pageNum > 100) {  // 最多查100页（20000条），可根据实际需求调整
                log.warn("查询数据超过20000条，可能存在性能风险");
                break;
            }
        }

        log.info("查询到时间范围[{}~{}]的监控数据共{}条",
                new Date(startTime), new Date(endTime), allData.size());
        return allData;
    }

    /**
     * 解析日期字符串（yyyy-MM-dd）
     */
    private Date parseDate(String dateStr) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse(dateStr);
        } catch (ParseException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "日期格式错误，请使用yyyy-MM-dd");
        }
    }

    /**
     * 获取某一天的开始时间戳（00:00:00）
     */
    private long getDayStartTime(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    /**
     * 获取某一天的结束时间戳（23:59:59）
     */
    private long getDayEndTime(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }

    /**
     * 将CSV内容写入响应流，触发浏览器下载
     */
    private void writeCsvToResponse(String csvContent, String dateStr, HttpServletResponse response) {
        response.setContentType("text/csv;charset=UTF-8");
        String fileName = "监控数据_" + dateStr + ".csv";

        try {
            // 处理中文文件名乱码
            String encodedFileName = new String(fileName.getBytes("UTF-8"), "ISO-8859-1");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedFileName + "\"");
            // 写入响应体
            response.getWriter().write(csvContent);
            response.getWriter().flush();
        } catch (IOException e) {
            log.error("下载监控数据失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "文件下载失败，请重试");
        }
    }

    /**
     * 计算单条监控数据的预警状态和信息（基于数据产生时的阈值）
     * @param doc 监控数据（包含产生时的阈值）
     * @return 包含是否预警和预警信息的Map
     */
    private Map<String, Object> calculateWarningStatus(SystemMonitorDoc doc) {
        Map<String, Object> result = new HashMap<>();
        List<String> warningMessages = new ArrayList<>();

        // 1. 计算各指标使用率（原有逻辑）
        double cpuUsage = doc.getCpuValue();
        double memoryUsage = doc.getSysMemTotal() > 0 ?
                (double) doc.getSysMemUsed() / doc.getSysMemTotal() * 100 : 0;
        double diskUsage = doc.getDiskTotal() > 0 ?
                doc.getDiskUsed() / doc.getDiskTotal() * 100 : 0;
        double jvmUsage = doc.getJvmMax() > 0 ?
                (double) doc.getJvmUsed() / doc.getJvmMax() * 100 : 0;

        // 2. 与“数据产生时的阈值”比较（核心修改）
        if (cpuUsage > doc.getCpuThreshold()) { // 使用当时的CPU阈值
            warningMessages.add(String.format("CPU使用率过高（当前:%.2f%%，阈值:%.2f%%）",
                    cpuUsage, doc.getCpuThreshold()));
        }
        if (memoryUsage > doc.getMemoryThreshold()) { // 使用当时的内存阈值
            warningMessages.add(String.format("内存使用率过高（当前:%.2f%%，阈值:%.2f%%）",
                    memoryUsage, doc.getMemoryThreshold()));
        }
        if (diskUsage > doc.getDiskThreshold()) { // 使用当时的磁盘阈值
            warningMessages.add(String.format("磁盘使用率过高（当前:%.2f%%，阈值:%.2f%%）",
                    diskUsage, doc.getDiskThreshold()));
        }
        if (jvmUsage > doc.getJvmThreshold()) { // 使用当时的JVM阈值
            warningMessages.add(String.format("JVM使用率过高（当前:%.2f%%，阈值:%.2f%%）",
                    jvmUsage, doc.getJvmThreshold()));
        }

        // 3. 组装结果
        result.put("hasWarning", !warningMessages.isEmpty());
        result.put("warningMsg", String.join("；", warningMessages));
        return result;
    }
}
