package com.yu.yupicture.common;

import com.yu.yupicture.modle.entity.AdminApiLogEntity;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@Component
public class LogArchiveTask {
    @Resource
    private MongoTemplate mongoTemplate;
    /**
     * cron表达式：0 0 2 * * ? → 每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanExpiredLog() {
        // 1. 构建查询条件：过期时间 < 当前时间
        Query query = Query.query(Criteria.where("expireTime").lt(LocalDateTime.now()));

        // 2. 统计要删除的日志数量（可选，用于日志记录）
        long deleteCount = mongoTemplate.count(query, AdminApiLogEntity.class);
        if (deleteCount == 0) {
            // 没有过期日志，直接返回
            return;
        }

        // 3. 执行删除操作
        mongoTemplate.remove(query, AdminApiLogEntity.class);

        // 4. 记录删除日志（可选，便于追溯删除情况）
        System.out.printf("【日志自动删除】删除过期日志数量：%d，删除时间：%s%n",
                deleteCount, LocalDateTime.now());
    }
}