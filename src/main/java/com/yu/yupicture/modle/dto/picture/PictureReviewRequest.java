package com.yu.yupicture.modle.dto.picture;

import lombok.Data;

import java.util.Date;

@Data
public class PictureReviewRequest {
    /**
     * 图片id
     */
    private Long id;

    /**
     * 审核状态
     */
    private int reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;

    /**
     * 审核人id
     */
    private Long reviewerId;

    /**
     * 审核时间
     */
    private Date reviewTime;

}
