package com.yu.yupicture.mannger.uploadTemplate.impl;

import cn.hutool.core.io.FileUtil;
import com.yu.yupicture.common.ErrorCode;
import com.yu.yupicture.exception.ThrowUtils;
import com.yu.yupicture.mannger.uploadTemplate.PictureUploadTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

@Component
public class FilePictureUpload extends PictureUploadTemplate {

    @Override
    public void validPicture(Object file) {
        MultipartFile file1 = (MultipartFile) file;
        ThrowUtils.throwIf(file == null, ErrorCode.PARAMS_ERROR, "文件为空");
        //校验文件大小
        long size = file1.getSize();
        final long MAX_SIZE = 1024 * 1024L;
        ThrowUtils.throwIf(size > 2 * MAX_SIZE, ErrorCode.PARAMS_ERROR, "文件大小不能超过2M");
        //校验文件类型
        String suffix = FileUtil.getSuffix(file1.getOriginalFilename());
        boolean contains = Arrays.asList("jpg", "png", "jpeg", "webp","gif").contains(suffix);
        ThrowUtils.throwIf(!contains, ErrorCode.PARAMS_ERROR, "文件类型错误");
    }

    @Override
    public String getOriginalFilename(Object file) {
        MultipartFile file1 = (MultipartFile) file;
        String originalFilename = ((MultipartFile) file).getOriginalFilename();
        return originalFilename;
    }

    @Override
    public void writeInTempFile(Object realFile, File tempFile) {
         MultipartFile file1 = (MultipartFile) realFile;
        try {
            file1.transferTo(tempFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getSuffix(String name) {
        String suffix = FileUtil.getSuffix(name);
        return suffix;
    }

}
