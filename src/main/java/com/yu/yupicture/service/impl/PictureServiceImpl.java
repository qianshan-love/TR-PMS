package com.yu.yupicture.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yu.yupicture.common.ErrorCode;
import com.yu.yupicture.exception.BusinessException;
import com.yu.yupicture.exception.ThrowUtils;
import com.yu.yupicture.mannger.FileMannger;
import com.yu.yupicture.modle.dto.file.UploadPictureResult;
import com.yu.yupicture.modle.dto.picture.*;
import com.yu.yupicture.modle.entity.Picture;
import com.yu.yupicture.modle.entity.User;
import com.yu.yupicture.modle.enums.PictureReviewStatusEnum;
import com.yu.yupicture.modle.vo.PictureVO;
import com.yu.yupicture.modle.vo.UserVO;
import com.yu.yupicture.service.PictureService;
import com.yu.yupicture.mapper.PictureMapper;
import com.yu.yupicture.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author Yu
* @description 针对表【picture(图片)】的数据库操作Service实现
* @createDate 2025-06-06 13:51:37
*/
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{

    @Resource
    private FileMannger fileMannger;
    @Resource
    private UserService userService;

//=======================================================增加=======================================================//
    /**
     * 上传图片
     * @param file
     * @param pictureUploadRequest
     * @param httpServletRequest
     * @return
     */
    @Override
    public PictureVO fileUploadPicture(MultipartFile file, PictureUploadRequest pictureUploadRequest, HttpServletRequest httpServletRequest) {

        ThrowUtils.throwIf(file == null , ErrorCode.PARAMS_ERROR, "文件为空");
        User userLogin = userService.getLoginSession(httpServletRequest);
        //判断用户是否登录
        ThrowUtils.throwIf(userLogin == null ,ErrorCode.NOT_LOGIN_ERROR,"未登录");

        //判断是否是更新图片
        if (pictureUploadRequest.getId() != null) {
            boolean exists = this.lambdaQuery().eq(Picture::getId, pictureUploadRequest.getId()).exists();
            ThrowUtils.throwIf(!exists, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }
        /**
         * 上传文件逻辑
         */
        //拼接上传文件的路径前缀
        String uploadPathPrefix = String.format("public/%s",userLogin.getId());
        //调用文件上传方法，上传图片进存储桶
        UploadPictureResult uploadPictureResult = fileMannger.FileUploadPicture(file, uploadPathPrefix);
        //创建图片信息类，填充图片信息
        log.info(uploadPictureResult.getThumbnailUrl());
        Picture picture = new Picture();
        picture.setId(pictureUploadRequest.getId());
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        picture.setName(uploadPictureResult.getName());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(userLogin.getId());
        //填充审核状态
        fillReviewStatus(picture, httpServletRequest);
        //更新操作，要更新图片的更新时间和编辑时间
        if (pictureUploadRequest.getId() != null) {
            picture.setEditTime(new Date());
            picture.setUpdateTime(new Date());
        }
        boolean ok = saveOrUpdate(picture);
        ThrowUtils.throwIf(!ok, ErrorCode.SYSTEM_ERROR, "上传失败");
        PictureVO pictureVO = getPictureVO(picture);
        return pictureVO;

    }

    /**
     * 使用url进行图片上传
     * @param fileUrl
     * @param pictureUploadRequest
     * @param httpServletRequest
     * @return
     */
    @Override
    public PictureVO urlUploadPicture(String fileUrl, PictureUploadRequest pictureUploadRequest, HttpServletRequest httpServletRequest) {

        ThrowUtils.throwIf(ObjectUtil.isEmpty(fileUrl) , ErrorCode.PARAMS_ERROR, "文件为空");
        User userLogin = userService.getLoginSession(httpServletRequest);
        //判断用户是否登录
        ThrowUtils.throwIf(userLogin == null ,ErrorCode.NOT_LOGIN_ERROR,"未登录");

        //判断是否是更新图片
        if (pictureUploadRequest.getId() != null) {
            boolean exists = this.lambdaQuery().eq(Picture::getId, pictureUploadRequest.getId()).exists();
            ThrowUtils.throwIf(!exists, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }
        /**
         * 上传文件逻辑
         */
        //拼接上传文件的路径前缀
        String uploadPathPrefix = String.format("public/%s",userLogin.getId());
        //调用文件上传方法，上传图片进存储桶
        UploadPictureResult uploadPictureResult = fileMannger.UrlUploadPicture(fileUrl, uploadPathPrefix);
        //创建图片信息类，填充图片信息
        Picture picture = new Picture();
        picture.setId(pictureUploadRequest.getId());
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setName(uploadPictureResult.getName());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(userLogin.getId());
        //填充审核状态
        fillReviewStatus(picture, httpServletRequest);
        //更新操作，要更新图片的更新时间和编辑时间
        if (pictureUploadRequest.getId() != null) {
            picture.setEditTime(new Date());
            picture.setUpdateTime(new Date());
        }
        boolean ok = saveOrUpdate(picture);
        ThrowUtils.throwIf(!ok, ErrorCode.SYSTEM_ERROR, "上传失败");
        PictureVO pictureVO = getPictureVO(picture);
        return pictureVO;

    }

    /**
     * 批量抓取图片
     * @param pictureReloadByBatchRequest
     * @param httpServletRequest
     * @return
     */
    @Override
    public Integer pictureUploadByBatch(PictureUploadByBatchRequest pictureReloadByBatchRequest, HttpServletRequest httpServletRequest) {
        //从批量上传请求中提取搜索关键词和需上传的图片数量，校验数量不超过 30 条，超出则抛参数错误异常。
        String searchText = pictureReloadByBatchRequest.getSearchText();
        Integer count = pictureReloadByBatchRequest.getCount();
        ThrowUtils.throwIf(searchText.isEmpty(),ErrorCode.PARAMS_ERROR,"搜索词不能为空");
        ThrowUtils.throwIf(count>30 || count<0,ErrorCode.PARAMS_ERROR,"搜索数量不能大于30");
        //用搜索关键词拼接必应图片异步搜索 URL（含 mmasync=1 参数），通过 Jsoup 请求并解析该 URL 对应的 HTML 文档，请求失败则抛操作错误异常。
        String url = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            document = Jsoup.connect(url).get();
        } catch (IOException e) {
            log.error("无法解析url",e);
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"无法解析url");
        }
        //从解析的文档中定位 class 为 “dgControl” 的 div 容器元素，若不存在则抛操作错误异常。
        Element div = document.getElementsByClass("dgControl").first();
        if (div == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"无法获得路径下的该容器元素");
        }
        //在该 div 容器中筛选出所有 class 为 “mimg” 的 img 元素，形成图片元素集合。
        Elements selectList = div.select("img.mimg");
        if (selectList == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"无法获得该容器下的图片列表");
        }
        int uploadCount = 0;
        for (Element element : selectList) {
            //遍历图片元素集合，提取每个 img 元素的 src 属性值作为图片 URL：
            String srcUrl = element.attr("src");
            if (srcUrl == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"无法获得图片列表中的图片");
            }
            //若 URL 含问号（查询参数），则截取问号前的部分作为处理后的 URL。
            int i = srcUrl.indexOf("?");
            if (i > -1) {
                srcUrl = srcUrl.substring(0, i);
            }
            //调用单张图片上传方法上传处理后的 URL，记录成功上传数量，若达到预设数量则终止遍历。
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            urlUploadPicture(srcUrl, pictureUploadRequest, httpServletRequest);
            uploadCount ++;
            if (uploadCount >= count) {
                break;
            }
        }
        //返回成功上传的图片总数。
        return uploadCount;
    }
//=====================================================查询=========================================================//
    /**
     * 获取查询条件
     * @param picture
     * @return
     */
    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest picture) {

        ThrowUtils.throwIf(picture == null,ErrorCode.PARAMS_ERROR,"查询参数为空");
        QueryWrapper<Picture> pictureQueryWrapper = new QueryWrapper<>();
        Long id = picture.getId();
        String name = picture.getName();
        String introduction = picture.getIntroduction();
        String category = picture.getCategory();
        List<String> tags = picture.getTags();
        String searchText = picture.getSearchText();
        /**
         * 处理搜索查询
         * 1. 先判断搜索文本是否为空
         * 2. 再判断标签列表是否为空
         * 3. 最后返回查询条件
         */
        if (ObjectUtil.isNotEmpty(searchText)) {
            pictureQueryWrapper.and(qw -> {
                qw.like("name", searchText).or().like("introduction", searchText);
            });
        }
        /**
         * 处理标签查询
         * 1. 先判断标签列表是否为空
         * 2. 再遍历标签列表，将每个标签添加到查询条件中
         * 3. 最后返回查询条件
         */
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                pictureQueryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        Long userId = picture.getUserId();
        Long picSize = picture.getPicSize();
        Integer picWidth = picture.getPicWidth();
        Integer picHeight = picture.getPicHeight();
        Double picScale = picture.getPicScale();
        String picFormat = picture.getPicFormat();
        String sortField = picture.getSortField();
        String softOrder = picture.getSoftOrder();
        pictureQueryWrapper.eq(ObjectUtil.isNotNull(id),"id", id);
        pictureQueryWrapper.eq(ObjectUtil.isNotNull(userId),"userId", userId);
        pictureQueryWrapper.like(ObjectUtil.isNotNull(name),"name", name);
        pictureQueryWrapper.like(ObjectUtil.isNotNull(introduction),"introduction", introduction);
        pictureQueryWrapper.eq(ObjectUtil.isNotNull(category),"category", category);
        pictureQueryWrapper.eq(ObjectUtil.isNotNull(picSize),"picSize", picSize);
        pictureQueryWrapper.eq(ObjectUtil.isNotNull(picWidth),"picWidth", picWidth);
        pictureQueryWrapper.eq(ObjectUtil.isNotNull(picHeight),"picHeight", picHeight);
        pictureQueryWrapper.eq(ObjectUtil.isNotNull(picScale),"picScale", picScale);
        pictureQueryWrapper.eq("reviewStatus",PictureReviewStatusEnum.PASS.getValue());
        pictureQueryWrapper.like(ObjectUtil.isNotNull(picFormat),"picFormat", picFormat);
        pictureQueryWrapper.orderBy(ObjectUtil.isNotNull(softOrder),softOrder.equals("descend"),sortField);
        return pictureQueryWrapper;

    }

    /**
     * 分页获取图片
     * @param picture
     * @param request
     * @return
     */
    public Page<PictureVO> getPictureVOList(Page<Picture> picture , HttpServletRequest request) {

        ThrowUtils.throwIf(picture == null , ErrorCode.PARAMS_ERROR,"方法参数为空");
        //获取所有元素并存储在列表中
        List<Picture> pictureList = picture.getRecords();
        Page<PictureVO> picturePage = new Page<>(picture.getCurrent(), picture.getSize(), picture.getTotal());
        //将列表中的元素脱敏
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::toPictureVO).collect(Collectors.toList());
        //获取图片中的用户id并转换为set集合，set集合中不允许有相同元素，避免查询用户信息时进行重复查询
        Set<Long> userId = pictureVOList.stream().map(PictureVO::getUserId).collect(Collectors.toSet());
        //set集合查询用户信息，并使用collectors的groupingBy功能进行分组，根据“什么”分组，“什么”就是键值
        Map<Long, List<User>> collect = userService.listByIds(userId).stream().collect(Collectors.groupingBy(User::getId));
        pictureVOList.forEach(pictureVo ->{
            Long userId1 = pictureVo.getUserId();
            User user = null;
            if (collect.containsKey(userId1)) {
                //获取集合中的第一个元素
                user = collect.get(userId1).get(0);
            }
            pictureVo.setUser(userService.getUserVo(user));
        });
        picturePage.setRecords(pictureVOList);
        return picturePage;

    }

    /**
     * 获取单张图片信息并脱敏
     * @param picture
     * @return
     */
    public static PictureVO getPictureVO(Picture picture) {

        PictureVO pictureVO = new PictureVO();
        BeanUtil.copyProperties(picture,pictureVO);
        return pictureVO;

    }

//=====================================================其他=========================================================//

    /**
     * 图片信息脱敏
     * 绑定用户信息
     * @param picture
     * @param request
     * @return
     */
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {

        PictureVO pictureVO = new PictureVO();
        pictureVO = PictureVO.toPictureVO(picture);
        Long userId = picture.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"没有上传用户信息");
        }
        User userById = userService.getById(userId);
        UserVO userVo = userService.getUserVo(userById);
        pictureVO.setUser(userVo);
        return pictureVO;
    }

    /**
     * 校验图片
     * @param picture
     */
    @Override
    public void validPicture(PictureUpdateRequest picture) {
        ThrowUtils.throwIf(picture == null ,ErrorCode.PARAMS_ERROR, "参数为空");
        String introduction = picture.getIntroduction();
        String name = picture.getName();
        List<String> tags = picture.getTags();
        ThrowUtils.throwIf(ObjectUtil.isNull(picture.getId()),ErrorCode.PARAMS_ERROR,"图片id为空");
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 500 ,ErrorCode.PARAMS_ERROR,"简介太长");
        }
        if (StrUtil.isNotBlank(name)) {
            ThrowUtils.throwIf(name.length() > 15 ,ErrorCode.PARAMS_ERROR,"名称太长");
        }
        if (CollUtil.isEmpty(tags)) {
            ThrowUtils.throwIf(tags.stream().anyMatch(tag ->tag.length()>9),ErrorCode.PARAMS_ERROR,"标签太长");
        }
    }

    @Override
    public void pictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        ThrowUtils.throwIf(ObjectUtil.isNull(pictureReviewRequest),ErrorCode.SYSTEM_ERROR,"参数错误");
        //获取图片id和审核状态
        Long id = pictureReviewRequest.getId();
        int reviewStatus = pictureReviewRequest.getReviewStatus();
        //根据审核状态获取枚举值
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getReviewStatusEnum(reviewStatus);
        ThrowUtils.throwIf(id <=0 || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum) || reviewStatusEnum == null ,ErrorCode.SYSTEM_ERROR);
        //检查图片是否存在
        Picture byId = this.getById(id);
        if (byId == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"没有该图片");
        }
        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureReviewRequest,picture);
        if (byId.getReviewStatus() == PictureReviewStatusEnum.PASS.getValue()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"禁止重复审核");
        }
        picture.setReviewTime(new Date());
        picture.setReviewerId(loginUser.getId());
        //更新审核信息
        boolean b = this.updateById(picture);
        ThrowUtils.throwIf(!b,ErrorCode.SYSTEM_ERROR);
    }

    /**
     * 添加审核参数
     * @param picture
     * @param httpServletRequest
     * @return
     */
    public void fillReviewStatus(Picture picture , HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(ObjectUtil.isNull(picture),ErrorCode.PARAMS_ERROR,"参数为空");
        User loginSession = userService.getLoginSession(httpServletRequest);
        if (userService.isAdmin(loginSession)) {
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginSession.getId());
            picture.setReviewTime(new Date());
            picture.setReviewMessage("管理员自动通过");
        }else {
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }


}




