package com.yu.yupicture.mannger.uploadTemplate;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.yu.yupicture.common.ErrorCode;
import com.yu.yupicture.config.CosClientConfig;
import com.yu.yupicture.exception.BusinessException;
import com.yu.yupicture.exception.ThrowUtils;
import com.yu.yupicture.mannger.tencentCOS.CosManager;
import com.yu.yupicture.modle.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;


@Slf4j
public abstract class PictureUploadTemplate {
    @Resource
    private CosClientConfig cosClientConfig;
    @Resource
    private CosManager cosManager;

    public UploadPictureResult uploadPicture(Object file, String uploadPathPrefix) {

        ThrowUtils.throwIf(file == null, ErrorCode.PARAMS_ERROR, "文件为空");
        ThrowUtils.throwIf(uploadPathPrefix == null, ErrorCode.PARAMS_ERROR, "上传路径前缀为空");
        //校验文件
        validPicture(file);
        //获取文件原始名称
        String originalFilename = getOriginalFilename(file);
        //获取文件后缀
        String suffix = getSuffix(originalFilename);
        //拼接上传文件名称
        String uploadFileName = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), RandomUtil.randomString(16), suffix);
        //拼接上传文件路径
        String filePath = String.format("/%s/%s", uploadPathPrefix, uploadFileName);
        //创建空文件
        File newFile = null;
        try {
            //创建临时文件
            newFile = File.createTempFile(filePath, null);
            //写入临时文件
            writeInTempFile(file,newFile);
            PutObjectResult putObjectResult = cosManager.putInfoObject(filePath, newFile,suffix);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + filePath);
            uploadPictureResult.setName(FileUtil.mainName(originalFilename));
            uploadPictureResult.setPicSize(FileUtil.size(newFile));
            uploadPictureResult.setPicWidth(imageInfo.getWidth());
            uploadPictureResult.setPicHeight(imageInfo.getHeight());
            double round = NumberUtil.round((imageInfo.getWidth() * 1.0) / imageInfo.getHeight(), 2).doubleValue();
            uploadPictureResult.setPicScale(round);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());
            return uploadPictureResult;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            deleteTempFile(newFile);
        }

    }

    public static void deleteTempFile(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        boolean delete = file.delete();
        if (!delete) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除临时文件失败");
        }
    }

    /**
     * 校验图片
     * @param file
     */
    public abstract void validPicture(Object file);

    /**
     * 获得图片原始名称
     * @param file
     * @return
     */
    public abstract String getOriginalFilename(Object file);

    /**
     * 写入临时文件
     * @param realFile
     * @param tempFile
     */
    public abstract void writeInTempFile(Object realFile , File tempFile);

    /**
     * 获取文件名后缀
     * @param realFile
     * @param tempFile
     */
    public abstract String getSuffix(String name);

    }

