package com.yu.yupicture.modle.dto.user;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.util.Date;

@Data
public class UserQueryRequest implements Serializable {

    private static final long serialVersionUID = -9197445068181066387L;

    /**
     * 搜索关键词
     */
    private String keyWord;


}
