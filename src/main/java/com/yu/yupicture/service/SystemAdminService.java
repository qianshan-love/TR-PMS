package com.yu.yupicture.service;

import com.mongodb.client.result.DeleteResult;
import com.yu.yupicture.modle.dto.PageRequest;
import com.yu.yupicture.config.SystemConfig;
import com.yu.yupicture.modle.dto.user.ApproveRequest;
import com.yu.yupicture.modle.dto.user.UserJoinGroupRequest;
import com.yu.yupicture.modle.dto.user.UserQueryRequest;
import com.yu.yupicture.modle.entity.PendingApproval;
import com.yu.yupicture.modle.entity.SystemMonitorDoc;
import com.yu.yupicture.modle.vo.PageVO;
import com.yu.yupicture.modle.vo.UserBelongVO;
import org.springframework.data.domain.Page;

import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface SystemAdminService {

    /**
     * 加入用户组
     * @param userJoinGroupRequest
     */
    boolean joinGroup(UserJoinGroupRequest userJoinGroupRequest);

    /**
     * 查询待审核用户列表
     * @param queryRequest
     * @return
     */
    PageVO<PendingApproval> queryPendingList(PageRequest queryRequest);

    /**
     * 查询用户组列表
     * @param queryRequest
     * @return
     */
    PageVO<UserBelongVO> queryUserList(PageRequest queryRequest);

    /**
     * 查询用户信息
     * @param userQueryRequest
     * @return
     */
    List<UserBelongVO> queryUser(UserQueryRequest userQueryRequest);

    /**
     * 删除用户
     * @param staffId
     * @return
     */
    DeleteResult deleteUser(String staffId);

    /**
     * 安全管理员审核用户入组
     * @param approveRequest
     * @return
     */
    boolean approveUser(ApproveRequest approveRequest);

    /**
     * 获取剩余添加用户数量
     * @return
     */
    long getAddedCount();

    /**
     * 获取实时监控数据（含预警判断）
     * @return 实时监控数据（图表数据+预警信息+系统基础信息）
     */
    Map<String, Object> getSystemData();

    /**
     * 定时存储监控数据（每半小时执行一次）
     */
    void scheduledStoreMonitorData();

    /**
     * 采集监控指标并封装为实体（供内部调用）
     * @return 封装后的监控数据实体
     */
    SystemMonitorDoc collectMonitorDoc();

    /**
     * 按天查询监控数据（分页）
     * @param queryDate 待查询日期（yyyy-MM-dd）
     * @param pageNum 页码（从1开始）
     * @param pageSize 每页条数
     * @return 分页查询结果
     */
    Page<SystemMonitorDoc> queryByDay(Date queryDate, Integer pageNum, Integer pageSize);

    /**
     * 按天查询监控数据（支持按字段排序）
     * @param queryDate 待查询日期
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param sortField 排序字段（如cpu_value、sys_mem_used等）
     * @param sortDir 排序方向（asc/desc）
     * @return 分页查询结果
     */
    Page<SystemMonitorDoc> queryByDayWithSort(Date queryDate, Integer pageNum, Integer pageSize,
                                              String sortField, String sortDir);

    /**
     * 获取当前预警阈值
     * @return
     */
    SystemConfig getCurrentWarningThreshold();

    /**
     * 系统管理员修改预警阈值
     * @param config
     * @return
     */
    SystemConfig updateWarningThreshold(SystemConfig config);

    /**
     * 下载某一天的监控数据
     * @param dateStr
     * @param response
     */
    void downloadMonitorDataByDay(String dateStr, HttpServletResponse response);

    /**
     * 按天查询所有监控数据
     * @param queryDate 待查询日期
     * @return 该天所有监控数据列表
     */
    List<SystemMonitorDoc> getAllMonitorDataByDay(Date queryDate);//
}
