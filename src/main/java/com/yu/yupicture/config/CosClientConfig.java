package com.yu.yupicture.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.BasicSessionCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "cos.client")
@Data
public class CosClientConfig {
    //secretId
    private String secretId;
    //密钥
    private String secretKey;
    //存储桶域名
    private String host;
    //存储桶名称
    private String bucket;
    //存储桶区域
    private String region;

    // 创建 COSClient 实例，这个实例用来后续调用请求
    @Bean
    COSClient createCOSClient() {
        // 设置用户身份信息。
        // SECRETID 和 SECRETKEY 请登录访问管理控制台 https://console.cloud.tencent.com/cam/capi 进行查看和管理
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        // ClientConfig 中包含了后续请求 COS 的客户端设置：
        ClientConfig clientConfig = new ClientConfig();
        // 设置 bucket 的地域
        // COS_REGION 请参见 https://cloud.tencent.com/document/product/436/6224
        clientConfig.setRegion(new Region(region));
        // 生成 cos 客户端。
        return new COSClient(cred, clientConfig);
    }
}
