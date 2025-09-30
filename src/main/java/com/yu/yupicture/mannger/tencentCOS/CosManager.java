package com.yu.yupicture.mannger.tencentCOS;

import cn.hutool.core.io.FileUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.yu.yupicture.config.CosClientConfig;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;

@Component
public class CosManager {
    @Resource
    private CosClientConfig cosClientConfig;
    @Resource
    private COSClient cosClient;
    @Resource
    private CosPicturePersistent cosPicturePersistent;

    //文件上传
    public PutObjectResult putObject(String key, File file) {

        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);
        return putObjectResult;

    }

    public COSObject getObject(String key) {

        String bucket = cosClientConfig.getBucket();
        GetObjectRequest getObjectRequest = new GetObjectRequest(bucket, key);
        COSObject cosObject = cosClient.getObject(getObjectRequest);
        return cosObject;

    }


    /**
     * 上传图片，通过数据万象获取信息
     *
     * @param filePath
     * @param file
     * @return
     */
    public PutObjectResult putInfoObject(String filePath, File file,String suffix) {

        if (suffix.equals("gif")) {
            PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), filePath, file);
            PicOperations picOperations = new PicOperations();
            picOperations.setIsPicInfo(1);
            putObjectRequest.setPicOperations(picOperations);
            PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);
            return putObjectResult;
        }
        //指定更改文件格式后的“新存储路径+文件名称”
        String fileId1 = FileUtil.mainName(filePath)+".webp";
        //设置规则：指定对图片进行什么处理
        String rule1 = "imageMogr2/format/webp";
        System.out.println(fileId1);
        String fileId2 = FileUtil.mainName(filePath) + "_thumbnail." + suffix;
        String rule2 = "imageMogr2/thumbnail/128x128>";
        System.out.println(fileId2);
        PutObjectRequest putObjectRequest = cosPicturePersistent.getCosPersistent(file, filePath,fileId1, fileId2,rule1,rule2);
        PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);
        return putObjectResult;

    }
}
