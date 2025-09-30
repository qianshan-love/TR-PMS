package com.yu.yupicture.modle.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class PageRequest implements Serializable {

    private static final long serialVersionUID = -5754596776247407361L;
    /**
     * 当前页
     */
    private int page;
    /**
     * 每页大小
     */
    private int size;
    /**
     * 排序字段
     */
    private String sortField;
    /**
     * 排序方式
     */
    private String softOrder = "descend";

    /**
     * 状态
     */
    private String status = "pending";
}
