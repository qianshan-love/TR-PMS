package com.yu.yupicture.modle.dto.user;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.Data;

import java.io.Serializable;

@Data
public class ApproveRequest implements Serializable {

    private static final long serialVersionUID = -2587847865492458914L;

    private String unionID;

    private String belongGroup;

    private String reviewerID;

    private Boolean approve;

}
