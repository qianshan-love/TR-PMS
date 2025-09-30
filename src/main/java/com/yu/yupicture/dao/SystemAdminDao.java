package com.yu.yupicture.dao;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.yu.yupicture.common.ErrorCode;
import com.yu.yupicture.config.SystemConfig;
import com.yu.yupicture.exception.BusinessException;
import com.yu.yupicture.exception.ThrowUtils;
import com.yu.yupicture.modle.dto.user.UserQueryRequest;
import com.yu.yupicture.modle.entity.Administrator;
import com.yu.yupicture.modle.entity.PendingApproval;
import com.yu.yupicture.modle.entity.SystemMonitorDoc;
import com.yu.yupicture.modle.entity.UserBelong;
import com.yu.yupicture.modle.vo.PageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Repository
@Slf4j
public class SystemAdminDao {

    @Resource
    private MongoTemplate mongoTemplate;

    //<<=====================================================impl:入组申请==================================================================>>

    /**
     * 根据unionId查询入组申请
     * @param unionId
     * @return
     */
    public PendingApproval findByUnionId(String unionId) {
        // 创建查询条件：根据 union_id 字段精确匹配
        Query query = new Query(Criteria.where("union_id").is(unionId));
        // 执行查询并返回单个结果
        return mongoTemplate.findOne(query, PendingApproval.class);
    }

    //<<=====================================================impl:查询待审核用户列表==================================================================>>
    /**
     * 分页查询待审核用户列表
     * @param status
     * @param pageNum
     * @param pageSize
     * @param sortField
     * @param sortDirection
     * @return
     */
    public PageVO<PendingApproval> findPendingByStatusWithPage(String status, int pageNum, int pageSize, String sortField, Sort.Direction sortDirection
    ) {
        // 1. 构建查询条件
        Criteria criteria = Criteria.where("status").is(status);
        Query query = new Query(criteria);

        // 2. 计算总条数
        long total = mongoTemplate.count(query, PendingApproval.class);

        // 3. 设置分页和排序
        query.skip((long) (pageNum - 1) * pageSize)
                .limit(pageSize)
                .with(Sort.by(sortDirection, sortField));

        // 4. 执行查询
        List<PendingApproval> dataList = mongoTemplate.find(query, PendingApproval.class);

        // 5. 封装分页结果
        PageVO<PendingApproval> pageVO = new PageVO<>();
        pageVO.setList(dataList);
        pageVO.setTotal(total);
        pageVO.setPageNum(pageNum);
        pageVO.setPageSize(pageSize);
        pageVO.setTotalPage((int) Math.ceil((double) total / pageSize));

        return pageVO;
    }

    //<<=====================================================impl:查询用户组列表==================================================================>>
    /**
     * 分页查询用户
     * @param pageNum
     * @param pageSize
     * @return
     */
    public PageVO<UserBelong> findUsersByPage(int pageNum, int pageSize) {
        // 1. 构建查询条件
        Query query = new Query();

        // 2. 计算总条数
        long total = mongoTemplate.count(query, UserBelong.class);

        // 3. 设置分页
        query.skip((long) (pageNum - 1) * pageSize)
                .limit(pageSize);

        // 4. 执行查询
        List<UserBelong> dataList = mongoTemplate.find(query, UserBelong.class);

        // 5. 封装分页结果
        PageVO<UserBelong> pageVO = new PageVO<>();
        pageVO.setList(dataList);
        pageVO.setTotal(total);
        pageVO.setPageNum(pageNum);
        pageVO.setPageSize(pageSize);
        pageVO.setTotalPage((int) Math.ceil((double) total / pageSize));

        return pageVO;
    }

    //<<=====================================================impl:查询用户信息==================================================================>>
    /**
     * 查询用户
     * @param userQueryRequest
     * @return
     */
    public List<UserBelong> findUsers(UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        String keyWord = userQueryRequest.getKeyWord().trim();
        // 构建查询条件：staffId精确匹配 或 staffName模糊匹配（忽略大小写）
        Criteria criteria = new Criteria().orOperator(
                Criteria.where("staffId").is(keyWord), // 精确匹配staffId
                Criteria.where("staffName").regex(keyWord, "i") // 模糊匹配昵称（忽略大小写）
        );
        Query query = new Query(criteria);
        List<UserBelong> userBelongList = mongoTemplate.find(query, UserBelong.class);
        return userBelongList;
    }

    //<<=====================================================impl:用户退组==================================================================>>

    /**
     * 根据staffId查询用户
     * @param staffId
     * @return
     */
    public UserBelong findByStaffId(String staffId) {
        // 创建查询条件：根据 union_id 字段精确匹配
        Query query = new Query(Criteria.where("staffId").is(staffId));
        // 执行查询并返回单个结果
        return mongoTemplate.findOne(query, UserBelong.class);
    }

    /**
     * 根据staffId删除用户
     * @param staffId
     * @return
     */
    public DeleteResult deleteUserByStaffId(String staffId) {
        Query query = new Query(Criteria.where("staffId").is(staffId));
        return mongoTemplate.remove(query, UserBelong.class);
    }

    /**
     * 根据adminId查询管理员
     * @param adminId
     * @return
     */
    public Administrator findByAdminId(String adminId) {
        // 创建查询条件：根据 union_id 字段精确匹配
        Query query = new Query(Criteria.where("_id").is(adminId));
        // 执行查询并返回单个结果
        return mongoTemplate.findOne(query, Administrator.class);
    }

    /**
     * 根据staffId更新用户状态
     * @param staffId
     * @return
     */
    public UpdateResult updateStatus(String staffId) {
        Query query1 = new Query(Criteria.where("union_id").is(staffId));
        Update set = new Update().set("status", "rejected");
        UpdateResult updateResult = mongoTemplate.updateMulti(query1, set, PendingApproval.class);
        if (updateResult.getMatchedCount() == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"该用户审核表中申请记录不存在，无法更新status字段，用户可重新申请入组");
        }
        return updateResult;
    }

    /**
     * 增加管理员添加用户次数
     * @param adminId
     */
    public void incrementAddedCount(String adminId) {
        Query query = new Query(Criteria.where("_id").is(adminId));
        Update update = new Update().inc("addedCount", 1); // 使用 $inc 原子操作
        mongoTemplate.updateFirst(query, update, Administrator.class);
    }

    //<<=====================================================impl:系统管理员审核用户入组==================================================================>>

    /**
     * 校验用户是否存在
     * @param unionID
     * @param belongGroup
     * @param status
     * @return
     */
    public PendingApproval findOneByUnionIDAndGroupAndStatus(String unionID, String belongGroup, String status) {
        Query query = new Query(Criteria.where("unionID").is(unionID)
                .and("belongGroup").is(belongGroup)
                .and("status").is(status));
        return mongoTemplate.findOne(query, PendingApproval.class);
    }

    /**
     * 保存用户审核记录
     * @param entity
     * @return
     */
    public PendingApproval save(PendingApproval entity) {
        return mongoTemplate.save(entity);
    }

    /**
     * 校验管理员是否存在，并检查管理员剩余添加次数
     * @param reviewerID
     * @param requiredRole
     * @return
     */
    public Administrator DecrementAddedCount(String reviewerID, int requiredRole) {
        // 构建查询条件：ID匹配、角色匹配、addedCount大于0
        Query query = new Query(Criteria.where("_id").is(reviewerID)
                .and("role").is(requiredRole)
                .and("addedCount").gt(0));

        // 更新操作：将addedCount减1
        Update update = new Update().inc("addedCount", -1);

        // 执行原子性的查找并更新，返回更新后的文档
        return mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true), // 返回更新后的文档
                Administrator.class
        );
    }
    //<<=====================================================impl:系统管理员存储监控数据==================================================================>>
    /**
     * 保存系统监控数据（带去重逻辑）
     */
    public void saveWithDuplicateCheck(SystemMonitorDoc monitorDoc, String storeType) {
        long currentTimestamp = monitorDoc.getMetricTimestamp();
        // 去重逻辑：同一分钟内仅保存1条
        long minuteStart = currentTimestamp - (currentTimestamp % (60 * 1000));
        long minuteEnd = minuteStart + 60 * 1000 - 1;

        // 判断当前分钟内数据库中是否已存在数据
        if (existsByMetricTimestampBetween(minuteStart, minuteEnd)) {
            log.debug("{} - 当前分钟（{}~{}）已存在监控数据，跳过存储", storeType, minuteStart, minuteEnd);
            return;
        }

        // 执行保存
        try {
            SystemMonitorDoc savedDoc = mongoTemplate.save(monitorDoc);
            log.info("{}成功 - 存储监控数据，时间戳：{}，过期时间戳：{}，CPU使用率：{}%",
                    storeType, savedDoc.getMetricTimestamp(), savedDoc.getExpireTime(), savedDoc.getCpuValue());
        } catch (Exception e) {
            log.error("{}失败 - 存储监控数据（时间戳：{}）", storeType, currentTimestamp, e);
        }
    }

    /**
     * 判断时间范围内数据库中是否存在数据
     */
    public boolean existsByMetricTimestampBetween(Long startTime, Long endTime) {
        Query query = Query.query(
                Criteria.where("metric_timestamp")
                        .gte(startTime)
                        .lte(endTime)
        );
        // 统计符合条件的记录数，>0 表示存在
        return mongoTemplate.count(query, SystemMonitorDoc.class) > 0;
    }

    /**
     * 初始化 TTL 索引（确保数据自动过期）
     */
    public void initTTLIndex() {
        try {
            IndexOperations indexOps = mongoTemplate.indexOps(SystemMonitorDoc.class);
            // 判断 TTL 索引是否已存在（通过字段名 "expire_time" 判断）
            boolean ttlIndexExists = indexOps.getIndexInfo().stream()
                    .anyMatch(indexInfo -> indexInfo.getIndexFields().stream()
                            .anyMatch(field -> "expire_time".equals(field.getKey())));

            if (!ttlIndexExists) {
                // 创建 TTL 索引：expire_time 字段，过期后立即删除
                Index ttlIndex = new Index()
                        .on("expire_time", org.springframework.data.domain.Sort.Direction.ASC)
                        .expire(0, TimeUnit.SECONDS)
                        .named("system_monitor_ttl_index");
                indexOps.ensureIndex(ttlIndex);
                log.info("成功为 system_monitor 集合创建TTL索引（默认15天过期）");
            } else {
                log.info("system_monitor 集合的TTL索引已存在，无需重复创建");
            }
        } catch (Exception e) {
            log.error("初始化 system_monitor 集合TTL索引失败", e);
            throw new RuntimeException("TTL索引初始化失败，过期机制无法生效", e);
        }
    }

    /**
     * 按天查询（基础分页）
     */
    public Page<SystemMonitorDoc> findByDay(Date queryDate, Integer pageNum, Integer pageSize) {
        // 计算当天时间范围（00:00:00 ~ 23:59:59）
        long dayStartTime = getDayStartTime(queryDate);
        long dayEndTime = getDayEndTime(queryDate);
        // 构建分页参数（MongoDB分页从0开始，需减1）
        Pageable pageable = PageRequest.of(
                pageNum - 1,
                pageSize,
                Sort.by(Sort.Direction.ASC, "metric_timestamp") // 默认按时间升序
        );
        // 构建查询条件
        Query query = Query.query(Criteria.where("metric_timestamp")
                        .gte(dayStartTime)
                        .lte(dayEndTime))
                .with(pageable); // 应用分页参数

        // 执行查询
        long totalCount = mongoTemplate.count(query, SystemMonitorDoc.class);
        List<SystemMonitorDoc> dataList = mongoTemplate.find(query, SystemMonitorDoc.class);

        return new PageImpl<>(dataList, pageable, totalCount);
    }

    // 排序字段白名单（仅允许这些字段排序，防止注入）
    private static final List<String> validSortFields = Arrays.asList(
            "metric_timestamp", "cpu_value", "sys_mem_used",
            "disk_used", "jvm_used", "net_download_speed"
    );

    /**
     * 按天查询（支持排序）
     */
    public Page<SystemMonitorDoc> findByDayWithSort(Date queryDate, Integer pageNum, Integer pageSize,
                                                    String sortField, String sortDir) {
        // 校验并修正排序字段（不在白名单则使用默认字段）
        String actualSortField = validSortFields.contains(sortField)
                ? sortField
                : "metric_timestamp";

        // 确定排序方向（默认升序）
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        // 构建分页+排序参数
        Pageable pageable =PageRequest.of(
                pageNum - 1,
                pageSize,
                direction,
                actualSortField
        );
        // 复用时间范围计算逻辑
        long dayStartTime = getDayStartTime(queryDate);
        long dayEndTime = getDayEndTime(queryDate);
        // 构建带排序的查询条件（排序已包含在pageable中）
        Query query = Query.query(Criteria.where("metric_timestamp")
                        .gte(dayStartTime)
                        .lte(dayEndTime))
                .with(pageable); // 应用分页+排序参数

        // 执行查询
        long totalCount = mongoTemplate.count(query, SystemMonitorDoc.class);
        List<SystemMonitorDoc> dataList = mongoTemplate.find(query, SystemMonitorDoc.class);

        return new PageImpl<>(dataList, pageable, totalCount);
    }

    /**
     * 计算总页数：(总条数 + 每页条数 - 1) / 每页条数（向上取整）
     */
    private int calculateTotalPage(long total, int pageSize) {
        if (total <= 0) {
            return 0;
        }
        return (int) ((total + pageSize - 1) / pageSize);
    }

    /**
     * 计算某天00:00:00的时间戳（毫秒）
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
     * 计算某天23:59:59的时间戳（毫秒）
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
     * 获取当前预警阈值配置（单条唯一）
     */
    public SystemConfig getWarningThresholdConfig() {
        // 按固定ID查询（确保配置唯一）
        Query query = Query.query(Criteria.where("_id").is("WARNING_THRESHOLD"));
        return mongoTemplate.findOne(query, SystemConfig.class);
    }

    /**
     * 更新预警阈值配置（支持部分更新，如仅改CPU阈值）
     */
    public SystemConfig updateWarningThreshold(SystemConfig config) {
        // 条件：按固定ID匹配
        Query query = Query.query(Criteria.where("_id").is("WARNING_THRESHOLD"));
        // 构建更新内容（仅更新非空字段）
        Update update = new Update();
        if (config.getCpuWarningThreshold() != null) {
            update.set("cpu_warning_threshold", config.getCpuWarningThreshold());
        }
        if (config.getMemoryWarningThreshold() != null) {
            update.set("memory_warning_threshold", config.getMemoryWarningThreshold());
        }
        if (config.getDiskWarningThreshold() != null) {
            update.set("disk_warning_threshold", config.getDiskWarningThreshold());
        }
        if (config.getJvmWarningThreshold() != null) {
            update.set("jvm_warning_threshold", config.getJvmWarningThreshold());
        }
        // 强制更新修改时间
        update.set("update_time", new Date());

        // upsert：存在则更新，不存在则插入（避免空指针）
        mongoTemplate.upsert(query, update, SystemConfig.class);
        // 返回更新后的完整配置
        return getWarningThresholdConfig();
    }

    /**
     * 初始化默认预警阈值配置（系统启动时调用，确保配置存在）
     */
    public void initDefaultWarningConfig() {
        try {
            // 先查询是否已有配置
            SystemConfig existingConfig = getWarningThresholdConfig();
            if (existingConfig == null) {
                // 不存在则创建默认配置
                SystemConfig defaultConfig = new SystemConfig();
                defaultConfig.setId("WARNING_THRESHOLD"); // 强制设置ID，避免空值
                defaultConfig.setUpdateTime(new Date());
                mongoTemplate.save(defaultConfig);
                log.info("初始化默认预警阈值配置成功：{}", defaultConfig);
            } else {
                log.info("系统已存在预警阈值配置，无需初始化");
            }
        } catch (Exception e) {
            // 即使查询失败（如数据库连接延迟），仍尝试插入默认配置
            log.error("查询现有配置失败，尝试强制插入默认配置", e);
            try {
                SystemConfig defaultConfig = new SystemConfig();
                defaultConfig.setId("WARNING_THRESHOLD");
                defaultConfig.setUpdateTime(new Date());
                mongoTemplate.save(defaultConfig);
                log.warn("强制插入默认预警阈值配置成功");
            } catch (Exception ex) {
                log.error("强制插入默认配置失败！请检查MongoDB连接", ex);
            }
        }
    }
}
