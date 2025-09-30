package com.yu.yupicture.common;

import cn.hutool.crypto.digest.DigestUtil;

public class EncryptPassword {
    /**
     * 密码加密
     * @param password
     * @return
     */
    public static String encryptPassword (String password) {
        //加盐值
        final String salt = "LING_SUO";
        //加密
        DigestUtil digestUtil = new DigestUtil();
        return digestUtil.md5Hex((password+salt).getBytes());
    }
}
