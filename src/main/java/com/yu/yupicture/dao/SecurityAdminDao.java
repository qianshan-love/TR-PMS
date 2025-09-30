package com.yu.yupicture.dao;

import com.mongodb.client.result.DeleteResult;
import com.yu.yupicture.modle.dto.log.SearchLogsRequest;
import com.yu.yupicture.modle.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;

import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Repository
@Slf4j
public class SecurityAdminDao {

    @Resource
    private @Qualifier("adminMongoTemplate") MongoTemplate adminMongoTemplate;

    /**
     * 校验敏感词是否存在
     * @param sensitiveWord 敏感词
     * @return 是否存在
     */
    public boolean existsBySensitiveWord(String sensitiveWord) {
        Query query = new Query(Criteria.where("sensitiveWord").is(sensitiveWord));
        return adminMongoTemplate.exists(query, SensitiveWordStore.class);
    }

    /**
     * 保存敏感词
     * @param entity 敏感词实体
     * @return 敏感词实体
     */
    public SensitiveWordStore saveSensitiveWord(SensitiveWordStore entity) {
        return adminMongoTemplate.save(entity);
    }

    /**
     * 删除敏感词
     * @param sensitiveWord 敏感词
     * @return 删除结果
     */
    public DeleteResult deleteBySensitiveWord(String sensitiveWord) {
        Query query = new Query(Criteria.where("sensitiveWord").is(sensitiveWord));
        return adminMongoTemplate.remove(query, SensitiveWordStore.class);
    }

    /**
     * 查询敏感词
     * @param sensitiveWord 敏感词
     * @return 敏感词实体
     */
    public SensitiveWordStore findSensitiveWord(String sensitiveWord) {
        Query query = new Query(Criteria.where("sensitiveWord").is(sensitiveWord));
        return adminMongoTemplate.findOne(query, SensitiveWordStore.class);
    }

    /**
     * 查询敏感词列表
     * @param page 页码
     * @param size 每页数量
     * @return 敏感词列表
     */
    public List<SensitiveWordStore> querySensitiveWordByPage(int page, int size) {
        //查询敏感词列表
        Query query = new Query();
        //分页
        query.skip((long)(page - 1) * size).limit(size);
        List<SensitiveWordStore> sensitiveWordStores = adminMongoTemplate.find(query, SensitiveWordStore.class);
        return sensitiveWordStores;
    }

    /**
     * 多条件查询用户操作日志（与Go的GetLogs功能完全对齐）
     * @param search 查询条件（对应Go的*model.SearchLogs）
     * @return 符合条件的日志列表（对应Go的*[]model.Logs）
     */
    public List<Logs> getLogs(SearchLogsRequest search) {
        try {
            Query query = new Query();
            Criteria criteria = new Criteria();

            // 1. 时间范围筛选（时间戳转LocalDateTime）
            if (search.getStartTime() != null && search.getEndTime() != null) {
                LocalDateTime start = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(search.getStartTime()),
                        ZoneId.systemDefault()
                );
                LocalDateTime end = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(search.getEndTime()),
                        ZoneId.systemDefault()
                );
                criteria.and("actionTime").gte(start).lte(end);
            } else if (search.getStartTime() != null) {
                LocalDateTime start = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(search.getStartTime()),
                        ZoneId.systemDefault()
                );
                criteria.and("actionTime").gte(start);
            } else if (search.getEndTime() != null) {
                LocalDateTime end = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(search.getEndTime()),
                        ZoneId.systemDefault()
                );
                criteria.and("actionTime").lte(end);
            }

            if (!criteria.getCriteriaObject().isEmpty()) {
                query.addCriteria(criteria);
            }

            // 2. 排序逻辑
            if (search.getSort() != null) {
                if (search.getSort() == 1) {
                    query.with(org.springframework.data.domain.Sort.by("actionTime").ascending());
                } else if (search.getSort() == -1) {
                    query.with(org.springframework.data.domain.Sort.by("actionTime").descending());
                }
            } else {
                query.with(org.springframework.data.domain.Sort.by("actionTime").descending());
            }

            // 3. 分页逻辑
            if (search.getSkip() != null && search.getSkip() > 0) {
                query.skip(search.getSkip());
            }
            if (search.getLimit() != null && search.getLimit() > 0) {
                query.limit(search.getLimit());
            }

            // 4. 执行查询
            List<Logs> logs = adminMongoTemplate.find(query, Logs.class);
            log.info("查询用户操作日志成功，条件：{}，结果条数：{}", search, logs.size());
            return logs;

        } catch (Exception e) {
            log.error("查询用户操作日志失败，条件：{}", search, e);
            // 修复：用Java 8支持的new ArrayList<>()替代List.of()
            return new ArrayList<>();
        }
    }

    /**
     * 核心方法：查询指定分组且未删除的检测结果
     * 与Go的GetDetectRes逻辑完全一致
     */
    public List<AutoDetectResult> getDetectRes(String belongGroup) {
        try {
            // 构建查询条件：belongGroup = 传入值 AND isDelete = false（对应Go的bson.M条件）
            Query query = new Query();
            query.addCriteria(Criteria.where("belongGroup").is(belongGroup));
            query.addCriteria(Criteria.where("isDelete").is(false));

            // 执行查询（对应Go的Find+cursor遍历）
            List<AutoDetectResult> results = adminMongoTemplate.find(query, AutoDetectResult.class);
            log.info("查询分组[{}]的检测结果成功，条数：{}", belongGroup, results.size());
            return results;

        } catch (Exception e) {
            log.error("查询分组[{}]的检测结果失败", belongGroup, e);
            throw new RuntimeException("获取检测结果失败", e); // 抛出异常由Service层处理
        }
    }

    // 保存操作日志
    public void saveAdminOperateLog(AdminOperateLog log) {
        adminMongoTemplate.save(log);
    }

    // 多条件查询日志（分页）
    public List<AdminOperateLog> queryAdminOperateLog(String operatorId, Date startTime,
                                                      Date endTime, String operationType,
                                                      int page, int size) {
        Criteria criteria = Criteria.where("operationTime").gte(startTime).lte(endTime);
        if (operatorId != null) {
            criteria.and("operatorId").is(operatorId);
        }
        if (operationType != null) {
            criteria.and("operationType").is(operationType);
        }
        Query query = Query.query(criteria)
                .skip((long) (page - 1) * size)
                .limit(size)
                .with(Sort.by(Sort.Direction.DESC, "operationTime"));
        return adminMongoTemplate.find(query, AdminOperateLog.class);
    }

    // 统计日志总数（用于分页）
    public long countAdminOperateLog(String operatorId, Date startTime,
                                     Date endTime, String operationType) {
        Criteria criteria = Criteria.where("operationTime").gte(startTime).lte(endTime);
        if (operatorId != null) {
            criteria.and("operatorId").is(operatorId);
        }
        if (operationType != null) {
            criteria.and("operationType").is(operationType);
        }
        return adminMongoTemplate.count(Query.query(criteria), AdminOperateLog.class);
    }

    // 根据ID查询日志（用于校验）
    public AdminOperateLog getAdminOperateLogById(String logId) {
        return adminMongoTemplate.findById(logId, AdminOperateLog.class);
    }

    // 保存异常操作记录
    public void saveAbnormalOperation(AbnormalOperation abnormal) {
        adminMongoTemplate.save(abnormal);
    }

    // 查询未处理的异常操作（用于审计提醒）
    public List<AbnormalOperation> getUnhandledAbnormal() {
        Query query = Query.query(Criteria.where("isHandled").is(false));
        return adminMongoTemplate.find(query, AbnormalOperation.class);
    }

    // 查询管理员信息（用于日志记录姓名）
    public Administrator getAdminById(String adminId) {
        return adminMongoTemplate.findById(adminId, Administrator.class);
    }
}
