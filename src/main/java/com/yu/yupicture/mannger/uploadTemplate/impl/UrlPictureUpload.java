package com.yu.yupicture.mannger.uploadTemplate.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.yu.yupicture.common.ErrorCode;
import com.yu.yupicture.exception.ThrowUtils;
import com.yu.yupicture.mannger.uploadTemplate.PictureUploadTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

@Slf4j
@Component
public class UrlPictureUpload extends PictureUploadTemplate {

    @Override
    public void validPicture(Object file) {
        String file1 = (String) file;
        ThrowUtils.throwIf(file == null, ErrorCode.PARAMS_ERROR, "文件为空");

        //校验url格式
        try {
            new URL(file1);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        //校验url是否符合协议要求
        if(!file1.startsWith("http://") && !file1.startsWith("https://")) {
            ThrowUtils.throwIf(true,ErrorCode.PARAMS_ERROR,"url不符合Http或Https协议");
        }
        //获取请求头中的图片大小和类型
        //创建一个http的head请求但不执行
        HttpRequest head = HttpRequest.head(file1);
        //执行head请求并获得响应体
        HttpResponse response = head.timeout(5000).execute();
        ThrowUtils.throwIf(!response.isOk(),ErrorCode.PARAMS_ERROR,"无法获取该url的详细信息");
        String pictureSize = response.header("Content-Length");
        String pictureType = response.header("Content-Type");
        log.info(pictureType);
        if (pictureSize == null || pictureSize.isEmpty()) {
            ThrowUtils.throwIf(true,ErrorCode.PARAMS_ERROR,"无法获取到图片类型");
        }
        Long size = Long.parseLong(pictureSize);
        //校验文件大小
        final long MAX_SIZE = 1024 * 1024L;
        ThrowUtils.throwIf( size == null || size > 2 * MAX_SIZE, ErrorCode.PARAMS_ERROR, "文件大小不能超过2M");
        //校验文件类型
        boolean contains = Arrays.asList("image/jpg", "image/png", "image/jpeg", "image/webp","image/gif").contains(pictureType);
        ThrowUtils.throwIf(pictureType == null || !contains , ErrorCode.PARAMS_ERROR, "文件类型错误");
    }

    @Override
    public String getOriginalFilename(Object file) {
        String  file1 = (String) file;
        String originalFilename = FileUtil.mainName(file1);
        //创建一个http的head请求但不执行
        HttpRequest head = HttpRequest.head(file1);
        //执行head请求并获得响应体
        HttpResponse response = head.timeout(5000).execute();
        ThrowUtils.throwIf(!response.isOk(),ErrorCode.PARAMS_ERROR,"无法获取该url的详细信息");
        String pictureType = response.header("Content-Type");
        int length = pictureType.length();
        int indexOf = pictureType.indexOf("/");
        pictureType = pictureType.substring(indexOf+1,length);
        log.info("文件名称是" + originalFilename + "文件扩展名是" + pictureType);
        String format = String.format("%s.%s",originalFilename,pictureType);
        log.info("文件全名是" + format);
        return format;
    }

    @Override
    public void writeInTempFile(Object realFile, File tempFile) {
         String  file1 = (String) realFile;
         HttpUtil.downloadFile(file1,tempFile);
    }

    @Override
    public String getSuffix(String name) {
        String suffix = FileUtil.getSuffix(name);
        return suffix;
    }
}
