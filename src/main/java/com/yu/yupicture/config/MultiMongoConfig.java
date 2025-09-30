package com.yu.yupicture.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.Configuration;

// 排除默认的Mongo自动配置，避免默认MongoTemplate干扰
@Configuration
@EnableAutoConfiguration(exclude = MongoAutoConfiguration.class)
public class MultiMongoConfig {
}
