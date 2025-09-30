package com.yu.yupicture.service;

import com.yu.yupicture.modle.dto.admin.AdminChangePasswordRequest;
import com.yu.yupicture.modle.dto.admin.AdminLoginRequest;
import com.yu.yupicture.modle.dto.admin.AdminRegistRequest;
import com.yu.yupicture.modle.entity.Administrator;
import com.yu.yupicture.modle.vo.AdminVO;

import javax.servlet.http.HttpServletRequest;

public interface AdminService {
    /**
     * 管理员注册
     * @param adminRegistRequest
     * @return
     */
    String adminRegister(AdminRegistRequest adminRegistRequest);

    /**
     * 管理员登录
     * @param adminLoginRequest
     * @return
     */
    AdminVO adminLogin(AdminLoginRequest adminLoginRequest);

    /**
     * 管理员退出登录
     * @param httpServletRequest
     * @return
     */
    boolean adminLogout(HttpServletRequest httpServletRequest);

    /**
     * 管理员修改密码
     * @param adminChangePasswordRequest
     * @return
     */
    Administrator changePassword(AdminChangePasswordRequest adminChangePasswordRequest);



}
