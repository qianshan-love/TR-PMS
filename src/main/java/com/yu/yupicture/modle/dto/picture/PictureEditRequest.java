package com.yu.yupicture.modle.dto.picture;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 图片更新请求
 * 用户更新图片信息
 *
 * @author Yu
 */
@Data
public class PictureEditRequest implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签（JSON 数组）
     */
    private List<String> tags;


    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
