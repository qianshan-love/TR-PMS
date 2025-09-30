package com.yu.yupicture.mannger;

import com.yu.yupicture.common.ErrorCode;
import com.yu.yupicture.exception.ThrowUtils;
import com.yu.yupicture.mannger.uploadTemplate.impl.FilePictureUpload;
import com.yu.yupicture.mannger.uploadTemplate.impl.UrlPictureUpload;
import com.yu.yupicture.modle.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;

@Service
@Slf4j
public class FileMannger {
    @Resource
    private FilePictureUpload filePictureUpload;
    @Resource
    private UrlPictureUpload urlPictureUpload;

    /**
     * 文件上传
     *
     * @param file
     * @param uploadPathPrefix
     * @return
     */
    public UploadPictureResult FileUploadPicture(MultipartFile file, String uploadPathPrefix) {
        ThrowUtils.throwIf(file == null, ErrorCode.PARAMS_ERROR, "文件为空");
        ThrowUtils.throwIf(uploadPathPrefix == null, ErrorCode.PARAMS_ERROR, "上传路径前缀为空");
        UploadPictureResult uploadPictureResult = filePictureUpload.uploadPicture(file, uploadPathPrefix);
        return uploadPictureResult;
    }
    public UploadPictureResult UrlUploadPicture(String file, String uploadPathPrefix) {
        ThrowUtils.throwIf(file == null, ErrorCode.PARAMS_ERROR, "文件为空");
        ThrowUtils.throwIf(uploadPathPrefix == null, ErrorCode.PARAMS_ERROR, "上传路径前缀为空");
        UploadPictureResult uploadPictureResult = urlPictureUpload.uploadPicture(file, uploadPathPrefix);
        return uploadPictureResult;
    }



}
