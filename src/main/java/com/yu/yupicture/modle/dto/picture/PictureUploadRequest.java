package com.yu.yupicture.modle.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureUploadRequest implements Serializable {

    private static final long serialVersionUID = 4716175110967725142L;
    /**
     * 图片id
     * 根据图片id在数据库中获取图片在存储桶中的地址
     */
    private Long id;
}
