package com.yu.yupicture.modle.vo;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.qcloud.cos.utils.StringUtils;
import com.yu.yupicture.modle.entity.Picture;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.baomidou.mybatisplus.extension.ddl.DdlScriptErrorHandler.PrintlnLogErrorHandler.log;

@Data
@Slf4j
public class PictureVO implements Serializable {
    private static final long serialVersionUID = 2583500618497987999L;
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 图片 url
     */
    private String url;

    /**
     * 缩略图url
     */
    private String thumbnailUrl;

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

    /**
     * 图片体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private Integer picWidth;

    /**
     * 图片高度
     */
    private Integer picHeight;

    /**
     * 图片宽高比例
     */
    private Double picScale;

    /**
     * 图片格式
     */
    private String picFormat;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    private UserVO user;

    public static Picture toPicture(PictureVO pictureVO) {

        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureVO, picture);
        String jsonStr = JSONUtil.toJsonStr(pictureVO.getTags());
        picture.setTags(jsonStr);
        return picture;

    }

    public static PictureVO toPictureVO(Picture picture) {

        // 防御性检查：处理picture为null的情况
        if (picture == null) {
            return null;
        }

        PictureVO pictureVO = new PictureVO();
        BeanUtil.copyProperties(picture, pictureVO);

        // 处理tags字段
        String tagsStr = picture.getTags();
        List<String> tagList = JSONUtil.toList(tagsStr, String.class);
        pictureVO.setTags(tagList);
        return pictureVO;

    }
}
