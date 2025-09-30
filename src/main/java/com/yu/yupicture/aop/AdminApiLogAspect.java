package com.yu.yupicture.aop;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson.JSONObject;
import com.yu.yupicture.annotation.AdminApiLog;
import com.yu.yupicture.common.HttpUtils;
import com.yu.yupicture.modle.entity.AdminApiLogEntity;
import com.yu.yupicture.modle.entity.Administrator;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
@Slf4j
public class AdminApiLogAspect {
    // 注入MongoTemplate（操作MongoDB，现有依赖）
    @Resource
    private @Qualifier("adminMongoTemplate") MongoTemplate adminMongoTemplate;

    /**
     * 环绕通知：拦截所有加了@AdminApiLog的方法
     */
    @Around("@annotation(com.yu.yupicture.annotation.AdminApiLog)")
    public Object recordApiLog(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 初始化日志实体
        AdminApiLogEntity logEntity = new AdminApiLogEntity();
        logEntity.setCallTime(LocalDateTime.now()); // 调用时间
        logEntity.setClientIp(HttpUtils.getClientIp()); // 调用IP

        // 2. 获取接口信息（从@AdminApiLog注解）
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        AdminApiLog apiLogAnnotation = method.getAnnotation(AdminApiLog.class);
        logEntity.setApiModule(apiLogAnnotation.module()); // 接口模块
        logEntity.setApiName(apiLogAnnotation.apiName()); // 接口名称

        // 3. 获取管理员信息（从Sa-Token+MongoDB）
        // 3.1 从Sa-Token获取当前登录管理员的id（前提：管理员登录时已用id作为登录标识）
        String adminId = StpUtil.getLoginIdAsString();
        logEntity.setAdminId(adminId);
        // 3.2 从MongoDB查询管理员的userName和role（关联administrators集合）
        Query adminQuery = Query.query(Criteria.where("id").is(adminId));
        Administrator admin = adminMongoTemplate.findOne(adminQuery, Administrator.class, "administrators");
        if (admin != null) {
            logEntity.setAdminUserName(admin.getUserName()); // 管理员登录名
            logEntity.setAdminRole(admin.getRole()); // 管理员角色
        } else {
            logEntity.setAdminUserName("unknown_admin"); // 异常情况默认值
            logEntity.setAdminRole(-1);
        }

        // 4. 获取接口请求参数（转为Map，便于查看）
        String[] paramNames = signature.getParameterNames(); // 参数名数组
        Object[] paramValues = joinPoint.getArgs(); // 参数值数组
        Map<String, Object> paramMap = new HashMap<>();
        for (int i = 0; i < paramNames.length; i++) {
            // 避免参数值是复杂对象，用FastJSON转为字符串（现有依赖）
            paramMap.put(paramNames[i], JSONObject.toJSONString(paramValues[i]));
        }
        logEntity.setRequestParams(paramMap);

        // 5. 执行原接口方法（不影响原有业务逻辑）
        Object result = joinPoint.proceed();

        // 6. 保存日志到MongoDB（核心：仅做存储，无其他操作）
        adminMongoTemplate.save(logEntity);

        // 7. 返回原接口结果
        return result;
    }
}
