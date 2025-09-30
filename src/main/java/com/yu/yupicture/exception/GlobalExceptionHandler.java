package com.yu.yupicture.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.yu.yupicture.common.BaseResponse;
import com.yu.yupicture.common.ErrorCode;
import com.yu.yupicture.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException", e);
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }

    @ExceptionHandler(NotLoginException.class) // 捕获指定异常
    public BaseResponse<Void> handleNotLoginException(NotLoginException e) {
        // Sa-Token 提供 getCode() 方法，区分未登录的具体原因（如 token 不存在、过期、无效等）
        String msg;
        switch (e.getType()) {
            case NotLoginException.NOT_TOKEN:
                msg = "未获取到登录令牌（token 不存在）";
                break;
            case NotLoginException.INVALID_TOKEN:
                msg = "登录令牌无效（token 格式错误或已被篡改）";
                break;
            case NotLoginException.TOKEN_TIMEOUT:
                msg = "登录令牌已过期，请重新登录";
                break;
            case NotLoginException.BE_REPLACED:
                msg = "账号已在其他设备登录，当前登录已失效";
                break;
            case NotLoginException.KICK_OUT:
                msg = "账号已被强制下线，请重新登录";
                break;
            default:
                msg = "未登录或登录状态已失效";
        }
        // 返回统一响应（状态码 401，提示消息如上）
        return ResultUtils.error(ErrorCode.OPERATION_ERROR,msg);
    }

    // 处理角色权限不足异常
    @ExceptionHandler(NotRoleException.class)
    public BaseResponse<Void> handleNotRoleException(NotRoleException e) {
        return ResultUtils.error(ErrorCode.NO_AUTH_ERROR, "需要角色：" + e.getRole() + "，但当前用户没有该角色");
    }

    // 处理权限不足异常（如果用到了 permission）
    @ExceptionHandler(NotPermissionException.class)
    public BaseResponse<Void> handleNotPermissionException(NotPermissionException e) {
        return ResultUtils.error(ErrorCode.NO_AUTH_ERROR, "需要权限：" + e.getPermission() + "，但当前用户没有该权限");
    }
}

