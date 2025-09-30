package com.yu.yupicture.dao;

import com.yu.yupicture.modle.dto.admin.LogQueryDTO;
import com.yu.yupicture.modle.entity.AdminApiLogEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@Slf4j
public class AuditAdminDao {
    @Resource
    private MongoTemplate mongoTemplate;

    public List<AdminApiLogEntity> findAll(LogQueryDTO queryDTO) {
        Query query = buildLogQuery(queryDTO); // 自己实现：构建时间、管理员等筛选条件
        List<AdminApiLogEntity> entityList = mongoTemplate.find(query, AdminApiLogEntity.class);
        return entityList;
    }

    public long total(LogQueryDTO queryDTO) {
        Query query = buildLogQuery(queryDTO); // 自己实现：构建时间、管理员等筛选条件
        long total = mongoTemplate.count(query, AdminApiLogEntity.class);
        return total;
    }
    /**
     * 辅助方法5：构建查询条件（适配所有筛选字段，默认查询全部+按最新时间排序）
     */
    private Query buildLogQuery(LogQueryDTO queryDTO) {
        Criteria criteria = new Criteria(); // 空条件=查询所有日志

        // 1. 时间范围筛选（支持单独传开始/结束时间）
        if (queryDTO.getStartTime() != null) {
            criteria.and("callTime").gte(queryDTO.getStartTime()); // 大于等于开始时间
        }
        if (queryDTO.getEndTime() != null) {
            criteria.and("callTime").lte(queryDTO.getEndTime()); // 小于等于结束时间
        }

        // 2. 管理员筛选（精确/模糊匹配）
        if (queryDTO.getAdminId() != null && !queryDTO.getAdminId().isEmpty()) {
            criteria.and("adminId").is(queryDTO.getAdminId()); // 精确匹配管理员ID
        }
        if (queryDTO.getAdminRole() != null) {
            criteria.and("adminRole").is(queryDTO.getAdminRole()); // 精确匹配角色
        }
        if (queryDTO.getAdminUserName() != null && !queryDTO.getAdminUserName().isEmpty()) {
            // 模糊匹配用户名（如"zhang"匹配"security_zhang"）
            criteria.and("adminUserName").regex(queryDTO.getAdminUserName(), "i"); // "i"表示忽略大小写
        }

        // 3. 接口筛选（模糊匹配）
        if (queryDTO.getApiModule() != null && !queryDTO.getApiModule().isEmpty()) {
            criteria.and("apiModule").regex(queryDTO.getApiModule(), "i"); // 模糊匹配模块
        }
        if (queryDTO.getApiName() != null && !queryDTO.getApiName().isEmpty()) {
            criteria.and("apiName").regex(queryDTO.getApiName(), "i"); // 模糊匹配接口名
        }

        // 4. 构建查询对象（空条件时自动查询所有日志）
        Query query = Query.query(criteria);

        // 5. 分页处理（修正原代码的字段名：pageSize而非size）
        int pageNum = queryDTO.getPage() == 0 ? 1 : queryDTO.getPage();
        int pageSize = queryDTO.getSize() == 0 ? 10 : queryDTO.getSize();
        query.skip((long) (pageNum - 1) * pageSize) // 跳过前N条
                .limit(pageSize); // 取M条

        // 6. 强制按调用时间倒序（最新日志在前），无论是否有筛选条件
        query.with(Sort.by(Sort.Direction.DESC, "callTime"));

        return query;
    }

    /**
     * 统计指定时间范围内的总操作次数
     */
    public long countByCallTimeBetween(LocalDateTime startTime, LocalDateTime endTime) {
        // 构建查询条件：callTime在startTime和endTime之间
        Query query = new Query();
        query.addCriteria(Criteria.where("callTime").gte(startTime).lte(endTime));

        // 使用MongoTemplate的count方法统计符合条件的记录数
        return mongoTemplate.count(query, AdminApiLogEntity.class);
    }

    /**
     * 查询指定模块在时间范围内的前N个不同接口名称
     */
    public List<String> findDistinctApiNameByModuleAndTimeRange(
            String moduleName, LocalDateTime startTime, LocalDateTime endTime, int limit) {

        // 构建查询条件：模块名称匹配且时间在范围内
        Query query = new Query();
        query.addCriteria(Criteria.where("apiModule").is(moduleName)
                .and("callTime").gte(startTime).lte(endTime));

        // 限制返回结果数量
        query.limit(limit);

        // 查询去重的apiName字段
        return mongoTemplate.findDistinct(query, "apiName", AdminApiLogEntity.class, String.class);
    }


}
