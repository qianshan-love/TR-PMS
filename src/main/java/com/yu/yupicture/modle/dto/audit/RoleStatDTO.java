package com.yu.yupicture.modle.dto.audit;

import lombok.Data;

@Data
public class RoleStatDTO {
    private int roleCode;       // 角色编码
    private String roleName;    // 角色名称
    private long opCount;       // 操作次数
    private double proportion;  // 占比（%）
}
