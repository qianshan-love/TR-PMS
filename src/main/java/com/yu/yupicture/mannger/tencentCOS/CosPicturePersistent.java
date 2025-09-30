package com.yu.yupicture.mannger.tencentCOS;

import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.yu.yupicture.config.CosClientConfig;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
@Component
public class CosPicturePersistent {

    @Resource
    private CosClientConfig cosClientConfig;

   public PutObjectRequest getCosPersistent(File file,String key,String fileId1,String fileId2,String rule1,String rule2) {

       //获取存储桶
       String bucketName = cosClientConfig.getBucket();
       //源文件存储路径
       String fileKey = key;
       PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fileKey, file);

       PicOperations picOperations = new PicOperations();
       picOperations.setIsPicInfo(1);
       // 添加图片处理规则
       List<PicOperations.Rule> ruleList = new LinkedList<>();
       PicOperations.Rule ruleOne = new PicOperations.Rule();
       ruleOne.setBucket(bucketName);
       ruleOne.setFileId(fileId1);
       ruleOne.setRule(rule1);
       PicOperations.Rule ruleTwo = new PicOperations.Rule();
       ruleTwo.setBucket(bucketName);
       ruleTwo.setFileId(fileId2);
       ruleTwo.setRule(rule2);
       picOperations.setRules(ruleList);
       putObjectRequest.setPicOperations(picOperations);
       return putObjectRequest;

   }
}
