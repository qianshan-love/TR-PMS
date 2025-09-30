package com.yu.yupicture.service.impl;

import cn.dev33.satoken.stp.StpInterface;
import com.yu.yupicture.common.UserConstant;
import com.yu.yupicture.modle.entity.Administrator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class StpInterfaceImpl implements StpInterface {

    @Resource
    private @Qualifier("adminMongoTemplate") MongoTemplate adminMongoTemplate;

    /**
     * 正确实现：返回用户的角色列表（如 SYSTEM_ADMIN、SECURITY_ADMIN）
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // 1. 从 MongoDB 查询用户信息（根据 loginId 即管理员ID）
        Query query = new Query(Criteria.where("_id").is(loginId));
        Administrator admin = adminMongoTemplate.findOne(query, Administrator.class);
        if (admin == null) {
            return new ArrayList<>(); // 用户不存在，返回空角色列表
        }

        // 2. 根据 admin.getRole() 转换为角色标识
        List<String> roleList = new ArrayList<>();
        switch (admin.getRole()) {
            case 0:
                roleList.add(UserConstant.SYSTEM_ADMIN);
                break;
            case 1:
                roleList.add(UserConstant.SECURITY_ADMIN);
                break;
            case 2:
                roleList.add(UserConstant.AUDIT_ADMIN);
                break;
            default:
                // 未知角色，返回空
        }
        log.info("--- 用户角色列表 ---" + roleList);
        return roleList;
    }

    /**
     * 正确实现：返回用户的权限列表（如 security:check、audit:log）
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        log.info("--- 获取用户权限列表 ---");
        // 1. 先获取角色列表（调用上面的 getRoleList 方法）
        List<String> roleList = getRoleList(loginId, loginType);
        List<String> permList = new ArrayList<>();

        // 2. 根据角色分配权限
        if (roleList.contains(UserConstant.SYSTEM_ADMIN)) {
            permList.add("system:check");
            permList.add("system:log");
            permList.add("system:add");
            permList.add("system:delete");
            permList.add("system:query");
            permList.add("system:update");
            permList.add("system:get");
        } else if (roleList.contains(UserConstant.SECURITY_ADMIN)) {
            permList.add("security:check");
            permList.add("security:log");
            permList.add("security:add");
            permList.add("security:delete");
            permList.add("security:query");
        } else if (roleList.contains(UserConstant.AUDIT_ADMIN)) {
            permList.add("audit:log");
            permList.add("audit:export");
            permList.add("audit:query");
            permList.add("audit:get");
        }
        log.info("--- 用户权限列表 ---" + permList);
        return permList;
    }
}