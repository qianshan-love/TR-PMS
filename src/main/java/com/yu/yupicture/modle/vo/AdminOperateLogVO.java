package com.yu.yupicture.modle.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

@Data
public class AdminOperateLogVO implements Serializable {
    private static final long serialVersionUID = -5472712572003092825L;
    private String id;
    private String operatorId;
    private String operatorName;
    private Date operationTime;
    private String operationIp;
    private String operationModule;
    private String operationType;
    private String operationDesc;
    private String operationContent;
    private String operationResult;
    private String errorMsg;
}
