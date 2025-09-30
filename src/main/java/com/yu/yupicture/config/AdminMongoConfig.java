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
 * admin库的MongoDB配置
 */
@Configuration
public class AdminMongoConfig {

    // 读取配置文件中spring.data.mongodb.admin前缀的参数
    @Bean
    @ConfigurationProperties(prefix = "spring.data.mongodb.admin")
    public MongoProperties adminMongoProperties() {
        return new MongoProperties();
    }

    // 创建admin库的MongoClient
    @Bean(name = "adminMongoClient")
    public MongoClient adminMongoClient() {
        MongoProperties properties = adminMongoProperties();
        return MongoClients.create(properties.getUri());
    }

    // 创建admin库的MongoTemplate（指定操作admin库）
    @Bean(name = "adminMongoTemplate")
    public MongoTemplate adminMongoTemplate() {
        return new MongoTemplate(
            new SimpleMongoClientDatabaseFactory(
                adminMongoClient(), 
                adminMongoProperties().getDatabase()  // 数据库名：admin
            )
        );
    }
}