package com.yu.yupicture.common;

public interface SystemConstant {
    // 1. 单位转换常量
     double KB = 1024d;
      double MB = KB * 1024;
      double GB = MB * 1024;
     long SECONDS_PER_HOUR = 3600;

    // 2. 过期机制配置（默认15天保存）
     int DEFAULT_RETENTION_DAYS = 15;
     long DEFAULT_RETENTION_MILLIS = DEFAULT_RETENTION_DAYS * 24L * 60 * 60 * 1000;

}
