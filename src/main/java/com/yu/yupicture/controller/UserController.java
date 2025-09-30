package com.yu.yupicture.controller;

import ch.qos.logback.classic.spi.EventArgUtil;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yu.yupicture.annotation.AuthCheck;
import com.yu.yupicture.common.*;
import com.yu.yupicture.exception.ThrowUtils;
import com.yu.yupicture.modle.dto.admin.AdminRegistRequest;
import com.yu.yupicture.modle.dto.admin.AdminUpdateRequest;
import com.yu.yupicture.modle.dto.user.*;
import com.yu.yupicture.modle.entity.Administrator;
import com.yu.yupicture.modle.entity.User;
import com.yu.yupicture.modle.enums.UserRole;
import com.yu.yupicture.modle.vo.UserLoginVO;
import com.yu.yupicture.modle.vo.UserVO;
import com.yu.yupicture.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@RequestMapping("/user")
@Slf4j
@CrossOrigin(origins = {"http://localhost:5173"},allowCredentials = "true")
public class UserController {

    @Resource
    private UserService userService;
    @Autowired
    private MongoTemplate mongoTemplate;


    @PostMapping("/joinGroup")
    public BaseResponse<String> joinGroup(@RequestBody UserJoinGroupRequest userJoinGroupRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userJoinGroupRequest == null, ErrorCode.PARAMS_ERROR);

        return ResultUtils.success("ok");
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public BaseResponse<String> userRegister(@RequestBody AdminRegistRequest adminRegistRequest) {
        ThrowUtils.throwIf(adminRegistRequest == null, ErrorCode.PARAMS_ERROR);
        Administrator administrator = new Administrator();
        administrator.setUserName(adminRegistRequest.getUserName());
        administrator.setPassword(adminRegistRequest.getPassword());
        administrator.setBelongGroup(adminRegistRequest.getBelongGroup());
        administrator.setAddedCount(adminRegistRequest.getAddedCount());
        administrator.setPhoneNumber(adminRegistRequest.getPhoneNumber());
        administrator.setPhoneCode(adminRegistRequest.getPhoneCode());
        administrator.setRole(adminRegistRequest.getRole());
        mongoTemplate.save(administrator,"administrators");
        return ResultUtils.success("ok");
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public BaseResponse<UserLoginVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request, HttpServletResponse httpServletResponse) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        UserLoginVO userLoginVO = userService.userLogin(userLoginRequest, request,httpServletResponse);
        return ResultUtils.success(userLoginVO);
    }

    /**
     * 获取登录态用户
     */
    @GetMapping("/login/get")
    public BaseResponse<UserLoginVO> getLoginSession(HttpServletRequest request) {
        User loginSession = userService.getLoginSession(request);
        return ResultUtils.success(userService.getUserLoginVO(loginSession));
    }

    /**
     * 用户注销
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        boolean result = userService.userLogout(request);
        return ResultUtils.success(true);
    }

    /**
     * 用户更新
     */
    @PostMapping("/update")
    public BaseResponse<Integer> userUpdate(@RequestBody UserUpdateRequest userUpdateRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userUpdateRequest == null, ErrorCode.PARAMS_ERROR, "参数错误");
        Integer id = userService.updateUser(userUpdateRequest, request);
        return ResultUtils.success(id);
    }

    /**
     * 用户查询
     */
    @GetMapping("/query")
    public BaseResponse<UserVO> userQuery(UserRequireRequest userRequireRequest) {
        ThrowUtils.throwIf(userRequireRequest == null, ErrorCode.PARAMS_ERROR, "参数错误");
        QueryWrapper<User> queryWrapper = userService.getQueryWrapper(userRequireRequest);
        User userOne = userService.getOne(queryWrapper);
        ThrowUtils.throwIf(userOne == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        UserVO userVo = userService.getUserVo(userOne);
        return ResultUtils.success(userVo);
    }

    @PostMapping("/list/query")
    public BaseResponse<Page<UserVO>> listUserQuery(UserRequireRequest userRequireRequest) {
        ThrowUtils.throwIf(userRequireRequest == null, ErrorCode.PARAMS_ERROR, "参数错误");
        int size = userRequireRequest.getSize();
        int page = userRequireRequest.getPage();
        Page<User> userByPage = userService.page(new Page(page, size), userService.getQueryWrapper(userRequireRequest));
        Page<UserVO> userVoByPage = new Page<>(page, size, userByPage.getTotal());
        List<UserVO> userVoList = userService.getUserVoList(userByPage.getRecords());
        userVoByPage.setRecords(userVoList);
        return ResultUtils.success(userVoByPage);
    }

    /**
     * 管理员添加用户
     */
    @PostMapping("/add")
    @AuthCheck(userRole = UserConstant.ADMIN)
    public BaseResponse<Boolean> userAdd(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR, "参数错误");
        User user = new User();
        BeanUtil.copyProperties(userAddRequest, user);
        final String password = "123456";
        String s = userService.encryptPassword(password);
        user.setUserPassword(s);
        boolean save = userService.save(user);
        if (!save) {
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "添加失败");
        }
        return ResultUtils.success(save);
    }

    /**
     * 管理员删除用户
     */
    @PostMapping("/delete")
    @AuthCheck(userRole = UserConstant.ADMIN)
    public BaseResponse<Boolean> userDelete(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest == null, ErrorCode.PARAMS_ERROR, "参数错误");
        Long userId = deleteRequest.getId();
        boolean removed = userService.removeById(userId);
        ThrowUtils.throwIf(!removed, ErrorCode.SYSTEM_ERROR, "删除失败");
        return ResultUtils.success(removed);
    }

    /**
     * 管理员更新用户
     */
    @PostMapping("/admin/update")
    @AuthCheck(userRole = UserConstant.ADMIN)
    public BaseResponse<Boolean> adminUpdate(@RequestBody AdminUpdateRequest adminUpdateRequest) {
        ThrowUtils.throwIf(adminUpdateRequest == null || adminUpdateRequest.getId() == null, ErrorCode.PARAMS_ERROR, "参数错误");
        User user = new User();
        BeanUtil.copyProperties(adminUpdateRequest, user);
        boolean updateById = userService.updateById(user);
        return ResultUtils.success(updateById);
    }


}