package com.yu.yupicture.modle.dto.picture;

import lombok.Data;

import java.util.List;

@Data
public class PictureTagCategory {

    private List<String> tags;

    private List<String> category;

}
