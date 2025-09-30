package com.yu.yupicture.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.yu.yupicture.annotation.AdminApiLog;
import com.yu.yupicture.common.BaseResponse;
import com.yu.yupicture.common.ErrorCode;
import com.yu.yupicture.common.ResultUtils;
import com.yu.yupicture.exception.ThrowUtils;
import com.yu.yupicture.modle.dto.admin.AdminChangePasswordRequest;
import com.yu.yupicture.modle.dto.admin.AdminLoginRequest;
import com.yu.yupicture.modle.dto.admin.AdminRegistRequest;
import com.yu.yupicture.modle.entity.Administrator;
import com.yu.yupicture.modle.vo.AdminVO;
import com.yu.yupicture.service.AdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RequestMapping("/admin")
@RestController
@Slf4j
public class AdminAuthController {
    @Resource
    private AdminService adminService;

    @PostMapping("/register")
    public BaseResponse<String> adminRegister(@RequestBody AdminRegistRequest adminRegistRequest){

        ThrowUtils.throwIf(adminRegistRequest == null, ErrorCode.PARAMS_ERROR, "管理员注册请求不能为空");
        String save = adminService.adminRegister(adminRegistRequest);
        return ResultUtils.success(save);

    }

    /**
     * 管理员登录
     * @param adminLoginRequest
     * @return
     */
    @PostMapping("/login")
    //@AdminApiLog(module = "admin-管理员",apiName = "adminLogin-登录")
    public BaseResponse<AdminVO> adminLogin(@RequestBody AdminLoginRequest adminLoginRequest){
        ThrowUtils.throwIf(adminLoginRequest == null, ErrorCode.PARAMS_ERROR, "管理员登录请求不能为空");
        AdminVO adminVO = adminService.adminLogin(adminLoginRequest);
        return ResultUtils.success(adminVO);
    }

    /**
     * 管理员退出登录
     * @param httpServletRequest
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> adminLogout(HttpServletRequest httpServletRequest) {
        // 临时打印当前用户的权限码列表
        List<String> permList = StpUtil.getPermissionList();
        System.out.println("当前用户权限码：" + permList);
        adminService.adminLogout(httpServletRequest);
        return ResultUtils.success(true);
    }

    /**
     * 管理员修改密码
     * @param adminChangePasswordRequest
     * @return
     */
    @PostMapping("/changePassword")
    @AdminApiLog(module = "admin-管理员",apiName = "changePassword-修改密码")
    public BaseResponse<Administrator> changePassword(@RequestBody AdminChangePasswordRequest adminChangePasswordRequest) {
        ThrowUtils.throwIf(adminChangePasswordRequest == null, ErrorCode.PARAMS_ERROR);
        Administrator ok = adminService.changePassword(adminChangePasswordRequest);
        return ResultUtils.success(ok);
    }


}
