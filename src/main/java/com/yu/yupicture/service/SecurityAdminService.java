package com.yu.yupicture.service;

import com.mongodb.client.result.DeleteResult;
import com.yu.yupicture.modle.dto.PageRequest;
import com.yu.yupicture.modle.dto.admin.LogQueryDTO;
import com.yu.yupicture.modle.dto.log.SearchLogsRequest;
import com.yu.yupicture.modle.entity.AdminOperateLog;
import com.yu.yupicture.modle.entity.AutoDetectResult;
import com.yu.yupicture.modle.entity.Logs;
import com.yu.yupicture.modle.entity.SensitiveWordStore;
import com.yu.yupicture.modle.vo.AdminApiLogVO;
import com.yu.yupicture.modle.vo.AdminOperateLogVO;
import com.yu.yupicture.modle.vo.PageVO;

import java.util.List;

public interface SecurityAdminService {

    /**
     * 安全管理员添加敏感词
     * @param sensitiveWordStore
     * @return
     */
    SensitiveWordStore addSensitiveWord(SensitiveWordStore sensitiveWordStore);
    /**
     * 安全管理员删除敏感词
     * @param sensitiveWord
     * @return
     */
    DeleteResult deleteSensitiveWord(String sensitiveWord);

    /**
     * 安全管理员查询敏感词
     * @param sensitiveWord
     * @return
     */
    SensitiveWordStore querySensitiveWord(String sensitiveWord);

    /**
     * 安全管理员获取敏感词列表
     * @param pageRequest
     * @return
     */
    PageVO<SensitiveWordStore> querySensitiveWordByPage(PageRequest pageRequest);

    /**
     * 安全管理员获取日志列表
     * @param searchLogsRequest
     * @return
     */
    List<Logs> getLogs(SearchLogsRequest searchLogsRequest);

    /**
     * 获取指定分组的文件检测结果
     * @param belongGroup 所属组
     */
    List<AutoDetectResult> getDetectResults(String belongGroup);
}
