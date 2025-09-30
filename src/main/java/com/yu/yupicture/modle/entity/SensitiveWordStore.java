package com.yu.yupicture.modle.entity;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;

@Data
@Document(collection = "sensitive_word_store")
public class SensitiveWordStore implements Serializable {

    private static final long serialVersionUID = 1533136040672476389L;

    /**
     * 敏感词
     */
    @Field("sensitiveWord")
    private String sensitiveWord;

    /**
     * 敏感词所属组
     */
    @Field("belongGroup")
    private String belongGroup;

}
