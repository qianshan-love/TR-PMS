package com.yu.yupicture.aop;

import com.fasterxml.jackson.databind.cfg.ContextAttributes;
import com.yu.yupicture.annotation.AuthCheck;
import com.yu.yupicture.common.ErrorCode;
import com.yu.yupicture.common.UserConstant;
import com.yu.yupicture.exception.BusinessException;
import com.yu.yupicture.modle.entity.User;
import com.yu.yupicture.modle.enums.UserRole;
import com.yu.yupicture.modle.vo.UserLoginVO;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
@Slf4j
public class AuthCheckAOP {
    @Around("@annotation(authCheck)")
    public Object AuthCheckAOP(ProceedingJoinPoint pjp, AuthCheck authCheck) throws Throwable {
        //获取注解上的权限设置
        String s = authCheck.userRole();
        UserRole userRole = UserRole.getEnumByName(s);
        if (userRole == null) {
            log.error("AuthCheck annotation parameter error");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "权限校验注解参数错误");
        }
        //获取用户权限
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        //校验用户权限
        Object attribute = request.getSession().getAttribute(UserConstant.USERLOGIN);
        if (attribute == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR,"用户未登录");
        }
        User user = (User) attribute;
        String userRole1 = user.getUserRole();
        UserRole enumByValue = UserRole.getEnumByName(userRole1);
        if (userRole1 == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限");
        }
        if (UserRole.ADMIN.equals(userRole) && !UserRole.ADMIN.equals(enumByValue)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限");
        }
        return pjp.proceed();
    }
}
