package com.yu.yupicture.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yu.yupicture.modle.dto.user.UserLoginRequest;
import com.yu.yupicture.modle.dto.user.UserRegisterRequest;
import com.yu.yupicture.modle.dto.user.UserRequireRequest;
import com.yu.yupicture.modle.dto.user.UserUpdateRequest;
import com.yu.yupicture.modle.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yu.yupicture.modle.vo.UserLoginVO;
import com.yu.yupicture.modle.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
* @author Yu
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-05-09 14:03:42
*/
public interface UserService extends IService<User> {
    /**
     * 用户注册
     * @param userRegisterRequest
     * @return 返回用户id
     */
    long userRegister(UserRegisterRequest userRegisterRequest);

    /**
     * 用户登录
     * @param userLoginRequest
     * @param request
     * @return 返回脱脱敏用户信息
     */
    UserLoginVO  userLogin(UserLoginRequest userLoginRequest , HttpServletRequest request, HttpServletResponse httpServletResponse);

    /**
     * 获取登录态用户
     * @param request
     * @return 返回用户信息
     */
    User getLoginSession (HttpServletRequest request);

    /**
     * 用户脱敏
     * @param user
     * @return 返回脱敏后的用户信息
     */
    UserLoginVO getUserLoginVO(User user);
    /**
     * 用户脱敏
     * @param user
     * @return 返回脱敏后的用户信息
     */
    UserVO getUserVo(User user);
    /**
     * 用户脱敏
     * @param userList
     * @return 返回脱敏后的列表用户信息
     */
    List<UserVO> getUserVoList(List<User> userList);

    Integer updateUser(UserUpdateRequest userUpdateRequest,HttpServletRequest request);
    /**
     * 用户注销
     * @param request
     * @return 返回是否注销成功
     */
    boolean userLogout(HttpServletRequest request);
    /**
     * 获取查询条件
     * @param userRequireRequest
     * @return 返回查询条件
     */
    QueryWrapper<User> getQueryWrapper(UserRequireRequest userRequireRequest);
    /**
     * 密码加密
     * @param password
     * @return 返回加密后的密码
     */
    String encryptPassword (String password);
    /**
     * 判断是否是管理员
     * @param user
     * @return 返回是否匹配
     */
    boolean isAdmin(User user);
}

