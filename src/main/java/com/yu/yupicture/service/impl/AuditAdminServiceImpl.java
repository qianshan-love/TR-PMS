package com.yu.yupicture.service.impl;

import com.yu.yupicture.common.TimeUtil;
import com.yu.yupicture.dao.AuditAdminDao;
import com.yu.yupicture.modle.dto.admin.LogQueryDTO;
import com.yu.yupicture.modle.dto.audit.ModuleStatDTO;
import com.yu.yupicture.modle.dto.audit.OverviewDTO;
import com.yu.yupicture.modle.dto.audit.ReportDataDTO;
import com.yu.yupicture.modle.dto.audit.RoleStatDTO;
import com.yu.yupicture.modle.entity.AdminApiLogEntity;
import com.yu.yupicture.modle.vo.AdminApiLogVO;
import com.yu.yupicture.modle.vo.PageVO;
import com.yu.yupicture.service.AuditAdminService;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuditAdminServiceImpl implements AuditAdminService {
    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private AuditAdminDao auditAdminDao;

    @Resource
    private TimeUtil timeUtil;

    /**
     * 审计管理员查询日志列表
     * @param queryDTO
     * @return
     */
    @Override
    public PageVO<AdminApiLogVO> queryAdminApiLog(LogQueryDTO queryDTO) {
        // 1. 数据库查询：按条件（如时间范围、管理员角色）查询原始日志Entity
        List<AdminApiLogEntity> entityList = auditAdminDao.findAll(queryDTO);
        long total = auditAdminDao.total(queryDTO);
        // 2. Entity → VO：后端处理数据
        List<AdminApiLogVO> voList = entityList.stream().map(entity -> {
            AdminApiLogVO vo = new AdminApiLogVO();
            // 2.1 管理员信息：编码转描述
            vo.setAdminUserName(entity.getAdminUserName());
            // 数字转中文描述
            vo.setAdminRoleDesc(convertAdminRole(entity.getAdminRole()));

            // 2.2 接口信息：直接复用（已符合前端需求）
            vo.setApiModule(entity.getApiModule());
            vo.setApiName(entity.getApiName());

            // 2.3 调用信息：格式优化
            vo.setCallTime(formatLocalDateTime(entity.getCallTime())); // 时间格式化
            vo.setClientIp(entity.getClientIp());
            vo.setRequestParamsDesc(formatRequestParams(entity.getRequestParams())); // 参数格式化

            return vo;
        }).collect(Collectors.toList());

        // 3. 组装分页结果（返回VO列表，而非Entity列表）
        PageVO<AdminApiLogVO> pageVO = new PageVO<>();
        pageVO.setList(voList);
        pageVO.setTotal(total);
        pageVO.setPageNum(queryDTO.getPage());
        pageVO.setPageSize(queryDTO.getSize());
        pageVO.setTotalPage((int) Math.ceil((double) total / queryDTO.getSize()));

        return pageVO;
    }

    @Override
    public ReportDataDTO getStatistics(LocalDateTime startTime, LocalDateTime endTime, boolean includeExamples) {
        // 1. 处理时间范围（默认当月）
        LocalDateTime[] timeRange = timeUtil.handleTimeRange(startTime, endTime);
        LocalDateTime actualStart = timeRange[0];
        LocalDateTime actualEnd = timeRange[1];

        // 2. 统计总操作次数
        long totalOpCount = auditAdminDao.countByCallTimeBetween(actualStart, actualEnd);

        // 3. 统计角色操作数据
        List<RoleStatDTO> roleStats = statRoleOperations(actualStart, actualEnd, totalOpCount);

        // 4. 统计模块操作数据
        List<ModuleStatDTO> moduleStats = statModuleOperations(actualStart, actualEnd, totalOpCount, includeExamples);

        // 5. 组装返回数据
        ReportDataDTO result = new ReportDataDTO();
        result.setOverview(buildOverview(actualStart, actualEnd, totalOpCount));
        result.setRoleStats(roleStats);
        result.setModuleStats(moduleStats);
        result.setGenerateTime(timeUtil.format(LocalDateTime.now()));

        return result;
    }

    // 构建总览数据
    private OverviewDTO buildOverview(LocalDateTime start, LocalDateTime end, long totalOpCount) {
        OverviewDTO overview = new OverviewDTO();
        overview.setTotalOpCount(totalOpCount);
        overview.setStatCycle(timeUtil.formatRange(start, end));
        return overview;
    }

    // 统计角色操作
    private List<RoleStatDTO> statRoleOperations(LocalDateTime start, LocalDateTime end, long total) {
        // MongoDB聚合查询：按角色分组统计
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("callTime").gte(start).lte(end)),
                Aggregation.group("adminRole").count().as("opCount"),
                Aggregation.project("opCount").and("_id").as("roleCode")
        );

        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, AdminApiLogEntity.class, Map.class);

        // 转换为DTO并计算占比
        return results.getMappedResults().stream()
                .map(map -> {
                    RoleStatDTO dto = new RoleStatDTO();
                    dto.setRoleCode(((Number) map.get("roleCode")).intValue());
                    dto.setRoleName(convertAdminRole(dto.getRoleCode())); // 角色编码转名称
                    dto.setOpCount(((Number) map.get("opCount")).longValue());
                    dto.setProportion(total > 0 ? (dto.getOpCount() * 100.0 / total) : 0);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // 统计模块操作
    private List<ModuleStatDTO> statModuleOperations(LocalDateTime start, LocalDateTime end,
                                                     long total, boolean includeExamples) {
        // 1. 统计模块操作次数
        Aggregation countAgg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("callTime").gte(start).lte(end)),
                Aggregation.group("apiModule").count().as("opCount"),
                Aggregation.project("opCount").and("_id").as("moduleName")
        );

        AggregationResults<Map> countResults = mongoTemplate.aggregate(
                countAgg, AdminApiLogEntity.class, Map.class);

        // 2. 转换为DTO并计算占比
        List<ModuleStatDTO> modules = countResults.getMappedResults().stream()
                .map(map -> {
                    ModuleStatDTO dto = new ModuleStatDTO();
                    dto.setModuleName((String) map.get("moduleName"));
                    dto.setOpCount(((Number) map.get("opCount")).longValue());
                    dto.setProportion(total > 0 ? (dto.getOpCount() * 100.0 / total) : 0);
                    return dto;
                })
                .collect(Collectors.toList());

        // 3. （可选）获取接口示例
        if (includeExamples) {
            modules.forEach(module -> {
                List<String> examples = auditAdminDao.findDistinctApiNameByModuleAndTimeRange(
                        module.getModuleName(), start, end, 2); // 取前2个
                module.setApiExamples(examples);
            });
        }

        return modules;
    }
    // 辅助方法1：管理员角色数字转中文描述
    private String convertAdminRole(Integer adminRole) {
        if (adminRole == null) {
            return "未知角色";
        }
        switch (adminRole) {
            case 0:
                return "安全管理员";
            case 1:
                return "系统管理员";
            case 2:
                return "审计管理员";
            default:
                return "未知角色";
        }
    }

    // 辅助方法2：LocalDateTime格式化（如“2024-09-01 15:30:45”）
    private String formatLocalDateTime(LocalDateTime time) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return time.format(formatter);
    }

    /**
     * 辅助方法3：请求参数Map格式化为友好字符串（支持内嵌JSON字符串脱敏）
     */
    private String formatRequestParams(Map<String, Object> requestParams) {
        if (requestParams == null || requestParams.isEmpty()) {
            return "无参数";
        }

        Set<String> sensitiveParams = new HashSet<>();
        sensitiveParams.add("phoneNumber");
        sensitiveParams.add("password");
        sensitiveParams.add("newPassword");
        sensitiveParams.add("checkPassword");

        // 递归脱敏（支持内嵌 JSON 字符串）
        Object desensitizedObj = desensitizeNestedParams(requestParams, sensitiveParams);

        // 确保返回的是 Map（理论上一定是）
        if (!(desensitizedObj instanceof Map)) {
            return "参数格式异常";
        }
        Map<String, Object> desensitizedMap = (Map<String, Object>) desensitizedObj;

        // 格式化为友好字符串
        StringBuilder paramsDesc = new StringBuilder();
        for (Map.Entry<String, Object> entry : desensitizedMap.entrySet()) {
            String paramName = entry.getKey();
            Object paramValue = entry.getValue();

            String valueStr = getReadableValue(paramValue);

            if (paramsDesc.length() > 0) {
                paramsDesc.append("，");
            }
            paramsDesc.append(paramName).append("：").append(valueStr);
        }

        return paramsDesc.toString();
    }

    /**
     * 递归脱敏（支持 Map、Collection、数组、内嵌 JSON 字符串）
     */
    private Object desensitizeNestedParams(Object value, Set<String> sensitiveParams) {
        if (value == null) {
            return null;
        }

        // 1. Map 类型：递归处理每个 key-value
        if (value instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) value;
            Map<String, Object> desensitizedMap = new HashMap<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String key = entry.getKey();
                Object val = entry.getValue();

                if (sensitiveParams.contains(key)) {
                    desensitizedMap.put(key, "*********");
                } else {
                    desensitizedMap.put(key, desensitizeNestedParams(val, sensitiveParams));
                }
            }
            return desensitizedMap;
        }

        // 2. 集合类型：递归处理每个元素
        if (value instanceof Collection) {
            Collection<?> collection = (Collection<?>) value;
            return collection.stream()
                    .map(item -> desensitizeNestedParams(item, sensitiveParams))
                    .collect(Collectors.toList());
        }

        // 3. 数组类型：递归处理每个元素
        if (value.getClass().isArray()) {
            Object[] array = (Object[]) value;
            return Arrays.stream(array)
                    .map(item -> desensitizeNestedParams(item, sensitiveParams))
                    .toArray();
        }

        // 4. ****** 新增：字符串内嵌 JSON 也解析 ******
        if (value instanceof String) {
            String str = (String) value;
            if (isJsonString(str)) {
                // 解析成 Map 再递归
                try {
                    Map<String, Object> innerMap = com.alibaba.fastjson.JSONObject.parseObject(str, Map.class);
                    Map<String, Object> desensitizedInner = (Map<String, Object>) desensitizeNestedParams(innerMap, sensitiveParams);
                    // 再转回 JSON 字符串
                    return com.alibaba.fastjson.JSONObject.toJSONString(desensitizedInner);
                } catch (Exception e) {
                    // 不是合法 JSON，原样返回
                    return str;
                }
            }
        }

        // 5. 基本类型：原样返回
        if (isBasicType(value.getClass())) {
            return value;
        }

        // 6. 其他复杂对象：先转 Map 再递归
        try {
            Map<String, Object> objMap = com.alibaba.fastjson.JSONObject.parseObject(
                    com.alibaba.fastjson.JSONObject.toJSONString(value), Map.class);
            return desensitizeNestedParams(objMap, sensitiveParams);
        } catch (Exception e) {
            // 转 Map 失败，原样返回
            return value;
        }
    }

    /**
     * 辅助方法：将参数值转换为可读字符串
     */
    private String getReadableValue(Object value) {
        if (value == null) {
            return "null";
        }

        if (value instanceof String) {
            return (String) value;
        }

        if (value instanceof Collection || value.getClass().isArray()) {
            return com.alibaba.fastjson.JSONObject.toJSONString(value);
        }

        if (!isBasicType(value.getClass())) {
            return com.alibaba.fastjson.JSONObject.toJSONString(value);
        }

        return value.toString();
    }

    /**
     * 判断是否为基本数据类型或包装类
     */
    private boolean isBasicType(Class<?> clazz) {
        return clazz.isPrimitive()
                || clazz == String.class
                || Number.class.isAssignableFrom(clazz)
                || clazz == Boolean.class;
    }


    /**
     * 判断字符串是否为合法 JSON
     */
    private boolean isJsonString(String str) {
        if (str == null || str.isEmpty()) return false;
        try {
            com.alibaba.fastjson.JSONObject.parseObject(str);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
