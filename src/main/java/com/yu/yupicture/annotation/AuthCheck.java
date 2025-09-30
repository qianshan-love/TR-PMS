package com.yu.yupicture.annotation;

import com.yu.yupicture.modle.enums.UserRole;
import org.aspectj.lang.ProceedingJoinPoint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthCheck {
    String userRole() default "";
}
