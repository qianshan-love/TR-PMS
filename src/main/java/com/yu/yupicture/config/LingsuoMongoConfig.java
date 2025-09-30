package com.yu.yupicture.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

/**
 * lingsuo库的MongoDB配置
 */
@Configuration
public class LingsuoMongoConfig {

    // 读取配置文件中spring.data.mongodb.lingsuo前缀的参数
    @Bean
    @ConfigurationProperties(prefix = "spring.data.mongodb.lingsuo")
    public MongoProperties lingsuoMongoProperties() {
        return new MongoProperties();
    }

    // 创建lingsuo库的MongoClient
    @Bean(name = "lingsuoMongoClient")
    public MongoClient lingsuoMongoClient() {
        MongoProperties properties = lingsuoMongoProperties();
        return MongoClients.create(properties.getUri());
    }

    // 创建lingsuo库的MongoTemplate（指定操作lingsuo库）
    @Bean(name = "lingsuoMongoTemplate")
    public MongoTemplate lingsuoMongoTemplate() {
        return new MongoTemplate(
            new SimpleMongoClientDatabaseFactory(
                lingsuoMongoClient(), 
                lingsuoMongoProperties().getDatabase()  // 数据库名：lingsuo
            )
        );
    }
}