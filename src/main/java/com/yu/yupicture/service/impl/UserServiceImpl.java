package com.yu.yupicture.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yu.yupicture.common.ErrorCode;
import com.yu.yupicture.common.UserConstant;
import com.yu.yupicture.exception.BusinessException;
import com.yu.yupicture.exception.ThrowUtils;
import com.yu.yupicture.modle.dto.user.UserLoginRequest;
import com.yu.yupicture.modle.dto.user.UserRegisterRequest;
import com.yu.yupicture.modle.dto.user.UserRequireRequest;
import com.yu.yupicture.modle.dto.user.UserUpdateRequest;
import com.yu.yupicture.modle.entity.User;
import com.yu.yupicture.modle.enums.UserRole;
import com.yu.yupicture.modle.vo.UserLoginVO;
import com.yu.yupicture.modle.vo.UserVO;
import com.yu.yupicture.service.UserService;
import com.yu.yupicture.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.Time;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
* @author Yu
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-05-09 14:03:42
*/
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    @Resource
    private StringRedisTemplate redisTemplateForDb1;

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 用户注册
     *
     * @param userRegisterRequest
     * @return
     */
    @Override
    public long userRegister(UserRegisterRequest userRegisterRequest) {
        /**
         * 获取参数
         */
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        //校验请求参数是否为空
        if (StrUtil.isEmpty(userAccount)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR.getCode(),"账号为空");
        }
        if (StrUtil.isEmpty(userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR.getCode(),"密码为空");
        }
        if (StrUtil.isEmpty(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR.getCode(),"校验密码为空");
        }
        //校验账号是否重复
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("userAccount",userAccount);
        ThrowUtils.throwIf(this.baseMapper.selectCount(userQueryWrapper) > 0,ErrorCode.PARAMS_ERROR,"账号重复");
        //校验密码是否相同
        ThrowUtils.throwIf(!userPassword.equals(checkPassword),ErrorCode.PARAMS_ERROR,"两次密码不一致");
        //加密
        String password = encryptPassword(userPassword);
        // 插入数据库
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(password);
        user.setUserRole(UserRole.USER.getName());
        user.setUserName("用户" + userAccount);
        int insert = this.baseMapper.insert(user);
        ThrowUtils.throwIf(insert < 0, ErrorCode.SYSTEM_ERROR, "注册失败");
        // 返回
        return user.getId();
    }

    /**
     * 用户登录
     * @param userLoginRequest
     * @param request
     * @return
     */
    @Override
    public UserLoginVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request, HttpServletResponse httpServletResponse) {

        //校验
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        ThrowUtils.throwIf(StrUtil.hasBlank(userAccount,userPassword),ErrorCode.PARAMS_ERROR,"账号或密码为空");
        //加密密码
        String password = encryptPassword(userPassword);
        //判断账号密码是否存在
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("userAccount",userAccount);
        userQueryWrapper.eq("userPassword",password);
        User user = this.baseMapper.selectOne(userQueryWrapper);
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            ThrowUtils.throwIf(user == null , ErrorCode.PARAMS_ERROR ,"账号或密码错误");
        }
        String userJson = JSONUtil.toJsonStr(user);
        String setSessionId = UUID.randomUUID().toString();
        //记录用户登录态
        request.getSession().setAttribute(UserConstant.USERLOGIN,user);
        return getUserLoginVO(user);
    }


    /**
     * 获取登录态用户
     * @param request
     * @return
     */
    @Override
    public User getLoginSession(HttpServletRequest request) {
        //获取session
        Object getUser = request.getSession().getAttribute(UserConstant.USERLOGIN);
        ThrowUtils.throwIf(getUser == null, ErrorCode.NOT_LOGIN_ERROR,"用户未登录");
        //根据session获取用户信息并强转为User类型
        User user = (User) getUser;
        //根据用户id获取用户信息
        User userById = this.getById(user.getId());
        ThrowUtils.throwIf(userById == null , ErrorCode.NOT_LOGIN_ERROR);
        return userById;
    }

    /**
     * 退出登录
     * @param request
     * @return
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        //判断是否登录
        Object attribute = request.getSession().getAttribute(UserConstant.USERLOGIN);
        ThrowUtils.throwIf(attribute == null , ErrorCode.NOT_LOGIN_ERROR);
        request.getSession().removeAttribute(UserConstant.USERLOGIN);
        return  true;
    }

    /**
     * 获取查询条件
     * @param userRequireRequest
     * @return
     */
    @Override
    public QueryWrapper<User> getQueryWrapper(UserRequireRequest userRequireRequest) {
        Long id = userRequireRequest.getId();
        String userAccount = userRequireRequest.getUserAccount();
        String userName = userRequireRequest.getUserName();
        String sortField = userRequireRequest.getSortField();
        String sortOrder = userRequireRequest.getSoftOrder();
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq(ObjectUtil.isNotNull(id),"id",id);
        userQueryWrapper.like(ObjectUtil.isNotNull(userAccount),"userAccount",userAccount);
        userQueryWrapper.like(ObjectUtil.isNotNull(userName),"userName",userName);
        userQueryWrapper.orderBy(StrUtil.isNotBlank(sortField),sortOrder.equals("descend"),sortField);
        return userQueryWrapper;
}

    /**
     * 密码加密
     * @param password
     * @return
     */
    public String encryptPassword (String password) {
        //加盐值
        final String salt = "YU";
        //加密
        DigestUtil digestUtil = new DigestUtil();
        return digestUtil.md5Hex((password+salt).getBytes());
    }

    /**
     * 判断是否是管理员
     * @param user
     * @return
     */
    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRole.ADMIN.equals(UserRole.getEnumByName(user.getUserRole()));
    }

    public UserLoginVO getUserLoginVO(User user) {
        UserLoginVO userLoginVO = new UserLoginVO();
        BeanUtil.copyProperties(user , userLoginVO);
        return userLoginVO;
    }

    @Override
    public UserVO getUserVo(User user) {
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user , userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVoList(List<User> userList) {
        List<UserVO> userVOS = new ArrayList<>();
        for ( User user : userList ) {
            UserVO userVo = this.getUserVo(user);
            userVOS.add(userVo);
        }
        return userVOS;
    }

    @Override
    public Integer updateUser(UserUpdateRequest userUpdateRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userUpdateRequest == null, ErrorCode.PARAMS_ERROR,"参数为空");
        Object attribute = request.getSession().getAttribute(UserConstant.USERLOGIN);
        User user = (User)attribute;
        String userPassword = userUpdateRequest.getUserPassword();
        String checkPassword = userUpdateRequest.getCheckPassword();
        ThrowUtils.throwIf(!userPassword.equals(checkPassword),ErrorCode.PARAMS_ERROR,"两次密码不一致");
        String newPassword = encryptPassword(userPassword);
        User userUpdate = new User();
        Long userId = user.getId();
        BeanUtil.copyProperties(userUpdateRequest,userUpdate);
        userUpdate.setId(userId);
        userUpdate.setUserPassword(newPassword);
        int id = userMapper.updateById(userUpdate);
        ThrowUtils.throwIf(id<0,ErrorCode.SYSTEM_ERROR,"更新失败");
        return id;
    }

}





