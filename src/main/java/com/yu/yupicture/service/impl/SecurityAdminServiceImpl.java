package com.yu.yupicture.service.impl;

import com.google.protobuf.ByteString;
import com.mongodb.client.result.DeleteResult;
import com.yu.yupicture.common.ErrorCode;
import com.yu.yupicture.exception.BusinessException;
import com.yu.yupicture.modle.dto.PageRequest;
import com.yu.yupicture.dao.SecurityAdminDao;
import com.yu.yupicture.exception.ThrowUtils;
import com.yu.yupicture.modle.dto.admin.LogQueryDTO;
import com.yu.yupicture.modle.dto.log.SearchLogsRequest;
import com.yu.yupicture.modle.entity.*;
import com.yu.yupicture.modle.vo.AdminApiLogVO;
import com.yu.yupicture.modle.vo.AdminOperateLogVO;
import com.yu.yupicture.modle.vo.PageVO;
import com.yu.yupicture.service.SecurityAdminService;
import com.yu.yupicture.common.SignatureUtils;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@Slf4j
public class SecurityAdminServiceImpl implements SecurityAdminService {

    @Resource
    private MongoTemplate mongoTemplate;
    @Resource
    private SecurityAdminDao securityAdminDao;

    // gRPC服务地址（从配置文件读取）
    @Value("${grpc.server.address:localhost:50051}")
    private String grpcServerAddress;

    // 最大消息长度（10MB）
    private static final int MAX_MESSAGE_LENGTH = 10 * 1024 * 1024;


    /**
     * 安全管理员添加敏感词
     * @param sensitiveWordStore
     * @return
     */
    @Override
    public SensitiveWordStore addSensitiveWord(SensitiveWordStore sensitiveWordStore) {
        //获取参数信息
        String sensitiveWord = sensitiveWordStore.getSensitiveWord();
        String belongGroup = sensitiveWordStore.getBelongGroup();
        //校验参数
        ThrowUtils.throwIf(sensitiveWord == null, ErrorCode.PARAMS_ERROR,"敏感词不能为空");
        ThrowUtils.throwIf(belongGroup == null, ErrorCode.PARAMS_ERROR,"敏感词所属组不能为空");
        //校验敏感词是否存在
        boolean result = securityAdminDao.existsBySensitiveWord(sensitiveWord);
        ThrowUtils.throwIf(result, ErrorCode.PARAMS_ERROR,"敏感词已存在");
        //添加敏感词
        SensitiveWordStore save = securityAdminDao.saveSensitiveWord(sensitiveWordStore);
        log.info("SecurityAdminServiceImpl::addSensitiveWord::添加敏感词成功，敏感词：{}，所属组：{}",sensitiveWord,belongGroup);
        //返回结果
        return save;
    }

    /**
     * 安全管理员删除敏感词
     * @param sensitiveWord
     * @return
     */
    @Override
    public DeleteResult deleteSensitiveWord(String sensitiveWord) {

        ThrowUtils.throwIf(sensitiveWord == null, ErrorCode.PARAMS_ERROR,"敏感词不能为空");
        //校验敏感词是否存在
        boolean result = securityAdminDao.existsBySensitiveWord(sensitiveWord);
        ThrowUtils.throwIf(!result, ErrorCode.PARAMS_ERROR,"敏感词不存在");
        //删除敏感词
        DeleteResult remove = securityAdminDao.deleteBySensitiveWord(sensitiveWord);
        log.info("SecurityAdminServiceImpl::deleteSensitiveWord::删除敏感词成功，敏感词：{}",sensitiveWord);
        //返回结果
        return remove;

    }

    /**
     * 安全管理员查询敏感词
     * @param sensitiveWord
     * @return
     */
    @Override
    public SensitiveWordStore querySensitiveWord(String sensitiveWord) {
        //校验参数
        ThrowUtils.throwIf(sensitiveWord == null, ErrorCode.PARAMS_ERROR,"敏感词不能为空");
        //查询敏感词
        SensitiveWordStore sensitiveWordStore = securityAdminDao.findSensitiveWord(sensitiveWord);
        //校验敏感词是否存在
        ThrowUtils.throwIf(sensitiveWordStore == null, ErrorCode.NOT_FOUND_ERROR,"敏感词不存在");
        log.info("SecurityAdminServiceImpl::querySensitiveWord::查询敏感词成功，敏感词：{}",sensitiveWord);
        return sensitiveWordStore;
    }

    /**
     * 安全管理员查询敏感词列表
     * @param pageRequest
     * @return
     */
    @Override
    public PageVO<SensitiveWordStore> querySensitiveWordByPage(PageRequest pageRequest) {

        int page = pageRequest.getPage();
        int size = pageRequest.getSize();
        ThrowUtils.throwIf(page <= 0 || size <= 0, ErrorCode.PARAMS_ERROR,"分页参数错误");
        List<SensitiveWordStore> sensitiveWordStores = securityAdminDao.querySensitiveWordByPage(page, size);
        PageVO<SensitiveWordStore> sensitiveWordStorePageVO = new PageVO<>();
        //设置数据列表
        sensitiveWordStorePageVO.setList(sensitiveWordStores);
        //设置总条数
        sensitiveWordStorePageVO.setTotal(sensitiveWordStores.size());
        //设置当前页码
        sensitiveWordStorePageVO.setPageNum(page);
        //设置每页条数
        sensitiveWordStorePageVO.setPageSize(size);
        //设置总页数
        sensitiveWordStorePageVO.setTotalPage((int) Math.ceil((double) sensitiveWordStores.size() / size));
        log.info("SecurityAdminServiceImpl::querySensitiveWordByPage::获取敏感词列表成功");
        return sensitiveWordStorePageVO;
    }

    @Override
    public List<Logs> getLogs(SearchLogsRequest searchLogsRequest) {
        // 1. 参数校验（补充业务规则）
        if (searchLogsRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "查询参数不能为空");
        }

        // 2. 处理默认分页参数（如果前端未传）
        // 默认每页10条
        if (searchLogsRequest.getLimit() == null || searchLogsRequest.getLimit() <= 0) {
            searchLogsRequest.setLimit((byte) 10);
        }
        // 默认跳过0条（第一页）
        if (searchLogsRequest.getSkip() == null || searchLogsRequest.getSkip() < 0) {
            searchLogsRequest.setSkip(0);
        }
        List<Logs> logs = securityAdminDao.getLogs(searchLogsRequest);
        return logs;
    }

    @Override
    public List<AutoDetectResult> getDetectResults(String belongGroup) {
        // 1. 参数校验（补充业务规则）
        if (belongGroup == null || belongGroup.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "所属组不能为空");
        }

        // 2. 调用DAO层查询
        List<AutoDetectResult> results = securityAdminDao.getDetectRes(belongGroup);

        // 3. 返回统一响应
        return results;
    }

}
