package com.yu.yupicture.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.yu.yupicture.common.EncryptPassword;
import com.yu.yupicture.common.ErrorCode;
import com.yu.yupicture.common.UserConstant;
import com.yu.yupicture.dao.AdminDao;
import com.yu.yupicture.exception.BusinessException;
import com.yu.yupicture.exception.ThrowUtils;
import com.yu.yupicture.modle.dto.admin.AdminChangePasswordRequest;
import com.yu.yupicture.modle.dto.admin.AdminLoginRequest;
import com.yu.yupicture.modle.dto.admin.AdminRegistRequest;
import com.yu.yupicture.modle.entity.Administrator;
import com.yu.yupicture.modle.entity.UserBelong;
import com.yu.yupicture.modle.vo.AdminVO;
import com.yu.yupicture.modle.vo.UserBelongVO;
import com.yu.yupicture.service.AdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service
public class AdminServiceImpl implements AdminService {

    @Resource
    private MongoTemplate mongoTemplate;
    @Resource
    private AdminDao adminDao;

    /**
     * 管理员注册
     * @param adminRegistRequest
     * @return
     */
    @Override
    public String adminRegister(@RequestBody AdminRegistRequest adminRegistRequest) {

        ThrowUtils.throwIf(adminRegistRequest == null,ErrorCode.PARAMS_ERROR,"注册请求不能为空");
        try {
            //加密
            String passWord = EncryptPassword.encryptPassword(adminRegistRequest.getPassword());
            Administrator administrator = new Administrator();
            administrator.setUserName(adminRegistRequest.getUserName());
            administrator.setPassword(passWord);
            administrator.setBelongGroup(adminRegistRequest.getBelongGroup());
            administrator.setAddedCount(adminRegistRequest.getAddedCount());
            administrator.setPhoneNumber(adminRegistRequest.getPhoneNumber());
            administrator.setPhoneCode(adminRegistRequest.getPhoneCode());
            administrator.setRole(adminRegistRequest.getRole());
            Administrator administrators = mongoTemplate.save(administrator, "administrators");
            return administrators.getId();
        } catch (Exception e) {
            log.error("Admin register erro", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,e.getMessage());
        }
    }

    /**
     * 管理员登录
     * @param adminLoginRequest
     * @return
     */
    @Override
    public AdminVO adminLogin(@RequestBody AdminLoginRequest adminLoginRequest) {

        // 获取参数信息
        String phoneNumber = adminLoginRequest.getPhoneNumber();
        // 使用相同算法加密密码
        String password = EncryptPassword.encryptPassword(adminLoginRequest.getPassword());
        String phoneCode = adminLoginRequest.getPhoneCode();
        // 校验参数是否为空
        if (phoneNumber == null || StrUtil.isEmpty(phoneNumber)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "手机号不能为空");
        }
        if (password == null || StrUtil.isEmpty(password)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不能为空");
        }
        if (phoneCode == null || StrUtil.isEmpty(phoneCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码不能为空");
        }
        Administrator administrator = adminDao.adminLogin(phoneNumber, password, phoneCode);
        // 获取数据库密码
        String passwordOfDB = administrator.getPassword();
        // 获取数据库验证码
        String phoneCodeOfDB = administrator.getPhoneCode();
        ThrowUtils.throwIf(!password.equals(passwordOfDB),ErrorCode.PARAMS_ERROR,"密码错误");
        ThrowUtils.throwIf(!phoneCode.equals(phoneCodeOfDB),ErrorCode.PARAMS_ERROR,"验证码错误");
        // 用户信息脱敏
        AdminVO adminVO = getAdminVO(administrator);
        // 登录：传入用户唯一标识
        StpUtil.login(administrator.getId());
        // 将脱敏信息存入 Sa-Token 会话
        StpUtil.getSession().set(UserConstant.ADMIN,adminVO);
        return adminVO;

    }

    /**
     * 管理员退出登录
     * @param httpServletRequest
     * @return
     */
    @Override
    public boolean adminLogout(HttpServletRequest httpServletRequest) {

        //校验用户是否登录
        StpUtil.checkLogin();
        AdminVO adminVO = (AdminVO) StpUtil.getSession().get(UserConstant.ADMIN);
        //退出登录
        StpUtil.logout();
        return true;

    }

    /**
     * 管理员修改密码
     * @param adminChangePasswordRequest
     * @return
     */
    @Override
    public Administrator changePassword(@RequestBody AdminChangePasswordRequest adminChangePasswordRequest) {
        // 1. 参数校验
        String phoneNumber = adminChangePasswordRequest.getPhoneNumber();
        String password = adminChangePasswordRequest.getPassword(); // 这是用户输入的旧密码
        String newPassword = adminChangePasswordRequest.getNewPassword();
        String checkPassword = adminChangePasswordRequest.getCheckPassword();

        // 检查 null 和空字符串 ""
        ThrowUtils.throwIf(StringUtils.isBlank(phoneNumber), ErrorCode.PARAMS_ERROR, "手机号不能为空");
        ThrowUtils.throwIf(StringUtils.isBlank(password), ErrorCode.PARAMS_ERROR, "密码不能为空");
        ThrowUtils.throwIf(StringUtils.isBlank(newPassword), ErrorCode.PARAMS_ERROR, "新密码不能为空");
        ThrowUtils.throwIf(StringUtils.isBlank(checkPassword), ErrorCode.PARAMS_ERROR, "确认新密码不能为空");
        ThrowUtils.throwIf(!newPassword.equals(checkPassword), ErrorCode.PARAMS_ERROR, "两次输入的新密码不一致");

        // 1.1 （新增）校验新密码强度，例如长度至少8位
        ThrowUtils.throwIf(newPassword.length() < 8, ErrorCode.PARAMS_ERROR, "新密码长度至少为8位");

        // 2. 校验管理员是否存在
        Administrator administrator = adminDao.queryAdmin(phoneNumber);
        ThrowUtils.throwIf(administrator == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在"); // 修改了错误提示和错误码

        // 3. 校验用户输入的旧密码是否正确
        String encryptedOldPassword = EncryptPassword.encryptPassword(password);
        // 比较加密后的旧密码输入是否与数据库存储的密码一致
        ThrowUtils.throwIf(!encryptedOldPassword.equals(administrator.getPassword()), ErrorCode.PARAMS_ERROR, "旧密码错误");

        // 4. 加密新密码
        String newPasswordEncrypt = EncryptPassword.encryptPassword(newPassword); // 同上，建议升级加密方式

        // 5. 更新密码
        administrator.setPassword(newPasswordEncrypt);
        Administrator save = mongoTemplate.save(administrator);
        return save;
    }

    /**
     * 信息脱敏 获取脱敏后的管理员类
     * @param administrator
     * @return
     */
    public AdminVO getAdminVO(Administrator administrator) {

        ThrowUtils.throwIf(administrator == null,ErrorCode.PARAMS_ERROR,"脱敏目标不能为空");
        AdminVO adminVO =  new AdminVO();
        BeanUtil.copyProperties(administrator,adminVO);
        return adminVO;

    }

}
