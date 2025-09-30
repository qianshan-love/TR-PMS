package com.yu.yupicture.common;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

@Component
public class SignatureUtils {
    @Value("${log.signature.secret:audit_secret_key_2024}") // 密钥配置在application.properties
    private String secret;

    /**
     * 生成日志签名
     */
    public String generateSignature(String operatorId, long timestamp, String operationType, String content) {
        String source = operatorId + timestamp + operationType + content + secret;
        return DigestUtils.md5DigestAsHex(source.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 校验签名
     */
    public boolean verifySignature(String operatorId, long timestamp, String operationType,
                                   String content, String storedSignature) {
        String generated = generateSignature(operatorId, timestamp, operationType, content);
        return generated.equals(storedSignature);
    }
}
