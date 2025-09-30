package com.yu.yupicture.modle.enums;

import cn.hutool.core.util.ObjectUtil;


public enum PictureReviewStatusEnum {

    REVIEWING("待审核", 0),
    PASS("审核通过", 1),
    REJECT("拒绝通过", 2);

    /**
     * 审核状态
     */
    private String status;

    /**
     * 审核状态值
     */
    private int value;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    PictureReviewStatusEnum(String status, int value) {
        this.status = status;
        this.value = value;
    }

    /**
     * 根据值获取枚举
     *
     * @param value
     * @return
     */
    public static PictureReviewStatusEnum getReviewStatusEnum(int value) {
        if (ObjectUtil.isNull(value)) {
            return null;
        }
        for (PictureReviewStatusEnum pictureReviewStatusEnum : PictureReviewStatusEnum.values()) {
            if (pictureReviewStatusEnum.value == value) {
                return pictureReviewStatusEnum;
            }
        }
        return null;
    }
}
