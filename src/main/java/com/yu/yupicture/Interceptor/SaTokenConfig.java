package com.yu.yupicture.Interceptor;

import cn.dev33.satoken.interceptor.SaInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 这是一个Sa-Token的配置类：注册拦截器
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

   @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器
        registry.addInterceptor(new SaInterceptor(handle -> {
            // 空实现：表示不添加全局拦截规则，仅处理接口上的注解
        }))
        .addPathPatterns("/**") // 拦截所有接口
        .excludePathPatterns("/login")
        .excludePathPatterns("/doc.html")
        .excludePathPatterns("/v2/api-docs")
        .excludePathPatterns("/swagger-resources/**")
        .excludePathPatterns("/webjars/**")
        ; // 排除登录接口（无需校验）
    }
}