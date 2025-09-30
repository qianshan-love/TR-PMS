package com.yu.yupicture;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.yu.yupicture.mapper")
@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
public class YuPictureApplication {

    public static void main(String[] args) {
        SpringApplication.run(YuPictureApplication.class, args);

        Cache<Object, Object> cache = Caffeine.newBuilder()
                .maximumSize(50)
                .build();
    }

}
