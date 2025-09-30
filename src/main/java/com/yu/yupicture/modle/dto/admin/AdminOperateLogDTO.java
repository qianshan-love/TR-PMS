package com.yu.yupicture.modle.dto.admin;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class AdminOperateLogDTO implements Serializable {

    private static final long serialVersionUID = -8069662771466145994L;
    private String id;
    private String operatorId;
    private String operatorName;
    private LocalDateTime operationTime;
    private String operationIp;
    private String operationType;
    private String operationDesc;
    private String operationContent;
    private String operationResult;
    private String errorMsg;
}
