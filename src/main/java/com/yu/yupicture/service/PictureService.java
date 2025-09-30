package com.yu.yupicture.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yu.yupicture.modle.dto.picture.*;
import com.yu.yupicture.modle.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yu.yupicture.modle.entity.User;
import com.yu.yupicture.modle.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

/**
* @author Yu
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-06-06 13:51:37
*/
public interface PictureService extends IService<Picture> {

    /**
     * 文件上传图片
     */
    PictureVO fileUploadPicture(MultipartFile file , PictureUploadRequest pictureUploadRequest , HttpServletRequest httpServletRequest);

    /**
     * url上传图片
     * @param file
     * @param pictureUploadRequest
     * @param httpServletRequest
     * @return
     */
    PictureVO urlUploadPicture(String file , PictureUploadRequest pictureUploadRequest , HttpServletRequest httpServletRequest);
    /**
     * 获取查询条件
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest picture);
    /**
     * 查询单个图片信息
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);
    /**
     * 分页查询图片信息
     */
    Page<PictureVO> getPictureVOList(Page<Picture> picture , HttpServletRequest request);
    /**
     * 校验图片
     */
    void validPicture(PictureUpdateRequest picture);

    /**
     * 图片审核
     * @param pictureReviewRequest
     * @param loginUser
     */
    void pictureReview(PictureReviewRequest pictureReviewRequest , User loginUser);

    /**
     * 填充审核状态
     * @param picture
     * @param httpServletRequest
     */
    void fillReviewStatus(Picture picture , HttpServletRequest httpServletRequest);

    /**
     * 批量上传图片
     * @param pictureReloadByBatchRequest
     * @param
     * @return
     */
    Integer pictureUploadByBatch(PictureUploadByBatchRequest pictureReloadByBatchRequest, HttpServletRequest httpServletRequest);
}
