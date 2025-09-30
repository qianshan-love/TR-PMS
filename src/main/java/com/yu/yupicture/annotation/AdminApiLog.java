package com.yu.yupicture.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AdminApiLog {

    String module(); // 接口所属模块（如"sensitive_word-敏感词管理"、"system_config-系统配置"）
    String apiName(); // 接口名称（如"addSensitiveWord-添加敏感词"、"deleteSensitiveWord-删除敏感词"）

}
