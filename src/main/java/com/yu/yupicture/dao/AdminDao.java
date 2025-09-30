package com.yu.yupicture.dao;

import cn.hutool.core.util.StrUtil;
import com.yu.yupicture.common.EncryptPassword;
import com.yu.yupicture.common.ErrorCode;
import com.yu.yupicture.exception.BusinessException;
import com.yu.yupicture.exception.ThrowUtils;
import com.yu.yupicture.modle.dto.admin.AdminLoginRequest;
import com.yu.yupicture.modle.entity.Administrator;
import com.yu.yupicture.modle.vo.AdminVO;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Repository
public class AdminDao {

    @Resource
    private MongoTemplate mongoTemplate;

    /**
     * 管理员登录
     * @param phoneNumber
     * @param password
     * @param phoneCode
     * @return
     */
    public Administrator adminLogin(String phoneNumber,String password,String phoneCode) {
        if (phoneNumber == null || StrUtil.isEmpty(phoneNumber)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "手机号不能为空");
        }
        if (password == null || StrUtil.isEmpty(password)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不能为空");
        }
        if (phoneCode == null || StrUtil.isEmpty(phoneCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码不能为空");
        }
        // 创建查询条件
        Query query = new Query(Criteria.where("phoneNumber").is(phoneNumber));
        //查询用户
        Administrator administrator = mongoTemplate.findOne(query, Administrator.class);
        ThrowUtils.throwIf(administrator == null,ErrorCode.NOT_FOUND_ERROR,"用户不存在");
        return administrator;
    }

    /**
     * 根据手机号查询管理员
     * @param phoneNumber
     * @return
     */
    public Administrator queryAdmin(String phoneNumber) {
        Query query = new Query(Criteria.where("phoneNumber").is(phoneNumber));
        return mongoTemplate.findOne(query, Administrator.class);
    }
}
