package com.yu.yupicture.modle.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 自定义分页结果类
 * @param <T> 数据类型
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageVO<T> {
    // 数据列表
    private List<T> list;
    // 总条数
    private long total;
    // 当前页码（从1开始）
    private int pageNum;
    // 每页条数
    private int pageSize;
    // 总页数
    private int totalPage;

}