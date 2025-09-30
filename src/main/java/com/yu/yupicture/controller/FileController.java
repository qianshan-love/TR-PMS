package com.yu.yupicture.controller;

import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.utils.IOUtils;
import com.yu.yupicture.common.BaseResponse;
import com.yu.yupicture.common.ErrorCode;
import com.yu.yupicture.common.ResultUtils;
import com.yu.yupicture.exception.BusinessException;
import com.yu.yupicture.mannger.tencentCOS.CosManager;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {
    @Resource
    private CosManager cosManager;

    /**
     * 测试文件上传
     *
     * @param multipartFile
     * @return
     */
    @PostMapping("/upload")
    public BaseResponse<String> uploadFile(@RequestParam("文件") MultipartFile multipartFile) {
        //获取文件原始名称
        String originalFilename = multipartFile.getOriginalFilename();
        //创建文件新路径
        String filepath = String.format("/test/%s", originalFilename);
        log.info(filepath);
        File file = null;
        //创建临时文件
        try {
            file = File.createTempFile(filepath, null);
            log.info(file.getName());
            multipartFile.transferTo(file);
            PutObjectResult putObjectResult = cosManager.putObject(filepath, file);
            return ResultUtils.success(filepath);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            if (file != null) {
                file.delete();
            }
        }
    }

    @PostMapping("/download")
    @SneakyThrows
    public void downloadFile(String filePath, HttpServletResponse response) {
        COSObject object = cosManager.getObject(filePath);
        COSObjectInputStream objectContent = null;
        try {
            // 获取对象的输入流
            objectContent = object.getObjectContent();
            byte[] byteArray = IOUtils.toByteArray(objectContent);
            // 设置响应头信息
            response.setContentType("application/octet-stream");
            // 设置响应头信息，告诉浏览器这是一个文件下载请求
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filePath + "\"");
            // 将文件内容写入响应输出流
            response.getOutputStream().write(byteArray);
            // 刷新响应输出流
            response.getOutputStream().flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (objectContent != null) {
                objectContent.close();
            }
        }
    }

}
