package com.yu.yupicture.modle.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureUploadByBatchRequest implements Serializable {

    private static final long serialVersionUID = 1472760257962315671L;
    private String searchText;
    private Integer count;

}
