package com.yu.yupicture.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yu.yupicture.annotation.AuthCheck;
import com.yu.yupicture.common.*;
import com.yu.yupicture.exception.ThrowUtils;
import com.yu.yupicture.mannger.CacheManager;
import com.yu.yupicture.modle.dto.picture.*;
import com.yu.yupicture.modle.entity.Picture;
import com.yu.yupicture.modle.entity.User;
import com.yu.yupicture.modle.enums.PictureReviewStatusEnum;
import com.yu.yupicture.modle.enums.UserRole;
import com.yu.yupicture.modle.vo.PictureVO;
import com.yu.yupicture.service.PictureService;
import com.yu.yupicture.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/picture")
@Slf4j
public class PictureController {

    @Resource
    private PictureService pictureService;
    @Resource
    private UserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private CacheManager cacheManager;

//=======================================================增加=======================================================//
    /**
     * 普通上传图片
     *
     * @param file
     * @param pictureUploadRequest
     * @return
     */
    @PostMapping("/upload/file")
    public BaseResponse<PictureVO> uploadFilePicture(@RequestPart("图片") MultipartFile file, PictureUploadRequest pictureUploadRequest, HttpServletRequest request) {

        ThrowUtils.throwIf(file == null, ErrorCode.PARAMS_ERROR, "文件为空");
        PictureVO pictureVO = pictureService.fileUploadPicture(file, pictureUploadRequest, request);
        return ResultUtils.success(pictureVO);

    }

    /**
     * url上传图片
     * @param url
     * @param pictureUploadRequest
     * @param request
     * @return
     */
    @PostMapping("/upload/url")
    public BaseResponse<PictureVO> uploadUrlPicture(String url, PictureUploadRequest pictureUploadRequest, HttpServletRequest request) {

        ThrowUtils.throwIf(ObjectUtil.isEmpty(url), ErrorCode.PARAMS_ERROR, "文件为空");
        PictureVO pictureVO = pictureService.urlUploadPicture(url, pictureUploadRequest, request);
        return ResultUtils.success(pictureVO);

    }

    /**
     * 批量抓取图片
     * @param pictureUploadByBatchRequest
     * @param httpServletRequest
     * @return
     */
    @AuthCheck(userRole = UserConstant.ADMIN)
    @PostMapping("/upload/batch")
    public BaseResponse<Integer> pictureUploadByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null,ErrorCode.PARAMS_ERROR,"请输入图片类型");
        Integer i = pictureService.pictureUploadByBatch(pictureUploadByBatchRequest, httpServletRequest);
        return ResultUtils.success(i);
    }

    /**
     * 测试使用
     * @param url
     * @return
     */
    @Deprecated
    @PostMapping("head")
    public BaseResponse<String> getHead(String url) {
        HttpRequest head = HttpRequest.head(url);
        HttpResponse response = head.execute();
        Picture picture = new Picture();
        String header = "";
        String header1 = "";
        if (response.isOk()) {
            header = response.header("Content-Length");
            header1 = response.header("Content-Type");
        }

        return ResultUtils.success(header + header1);
    }
//=======================================================删除=======================================================//
    /**
     * 删除图片
     * @param deleteRequest
     * @return
     */
    @DeleteMapping("/delete")
    public BaseResponse<Boolean> deletePicture(DeleteRequest deleteRequest, HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(ObjectUtil.isEmpty(deleteRequest), ErrorCode.PARAMS_ERROR, "参数为空");
        Long id = deleteRequest.getId();
        Picture byId = pictureService.getById(id);
        ThrowUtils.throwIf(byId == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        Long userId = byId.getUserId();
        User user = (User) httpServletRequest.getSession().getAttribute(UserConstant.USERLOGIN);
        Long userId1 = user.getId();
        ThrowUtils.throwIf((!userId.equals(userId1)) && !userService.isAdmin(user), ErrorCode.NO_AUTH_ERROR, "无权限删除图片");
        boolean b = pictureService.removeById(id);
        ThrowUtils.throwIf(!b, ErrorCode.SYSTEM_ERROR, "删除失败");
        return ResultUtils.success(b);
    }

//=======================================================更新=======================================================//
    /**
     * 管理员更新图片信息
     * @param pictureUpdateRequest
     * @return
     */
    @AuthCheck(userRole = UserConstant.ADMIN)
    @PostMapping("/update")
    public BaseResponse<Boolean> updatePicture(PictureUpdateRequest pictureUpdateRequest,HttpServletRequest httpServletRequest) {

        ThrowUtils.throwIf(ObjectUtil.isEmpty(pictureUpdateRequest) || pictureUpdateRequest.getId()<=0, ErrorCode.PARAMS_ERROR, "参数为空");
        //校验图片
        pictureService.validPicture(pictureUpdateRequest);
        //查询图片是否存在
        Long id = pictureUpdateRequest.getId();
        Picture byId = pictureService.getById(id);
        ThrowUtils.throwIf(byId == null,ErrorCode.NOT_FOUND_ERROR,"没有找到图片信息");
        //类型转换
        Picture picture = new Picture();
        List<String> tags = pictureUpdateRequest.getTags();
        String jsonTags = JSONUtil.toJsonStr(tags);
        BeanUtil.copyProperties(pictureUpdateRequest,picture);
        picture.setTags(jsonTags);
        pictureService.fillReviewStatus(picture,httpServletRequest);
        //更新数据库
        boolean b = pictureService.updateById(picture);
        ThrowUtils.throwIf(!b,ErrorCode.SYSTEM_ERROR,"更新失败");
        return ResultUtils.success(b);

    }

    /**
     * 用户编辑图片信息
     * @param pictureUpdateRequest
     * @param httpServletRequest
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editPicture(PictureUpdateRequest pictureUpdateRequest,HttpServletRequest httpServletRequest) {

        ThrowUtils.throwIf(ObjectUtil.isEmpty(pictureUpdateRequest) || pictureUpdateRequest.getId()<=0, ErrorCode.PARAMS_ERROR, "参数为空");
        //校验图片
        pictureService.validPicture(pictureUpdateRequest);
        User loginUser = (User)httpServletRequest.getSession().getAttribute(UserConstant.USERLOGIN);
        ThrowUtils.throwIf(loginUser == null,ErrorCode.NO_AUTH_ERROR,"用户未登录");
        //查询图片是否存在
        Long id = pictureUpdateRequest.getId();
        Picture byId = pictureService.getById(id);
        ThrowUtils.throwIf(byId == null,ErrorCode.NOT_FOUND_ERROR,"没有找到图片信息");
        //判断是否是本人操作或管理员操作
        Long userId = byId.getUserId();
        Long userId1 = loginUser.getId();
        ThrowUtils.throwIf(!userId.equals(userId1)  && !(loginUser.getUserRole().equals(UserRole.getEnumByName(UserConstant.ADMIN))),ErrorCode.NO_AUTH_ERROR,"无权限编辑图片");
        //类型转换
        Picture picture = new Picture();
        List<String> tags = pictureUpdateRequest.getTags();
        String jsonTags = JSONUtil.toJsonStr(tags);
        BeanUtil.copyProperties(pictureUpdateRequest,picture);
        picture.setTags(jsonTags);
        pictureService.fillReviewStatus(picture,httpServletRequest);
        //更新数据库
        boolean b = pictureService.updateById(picture);
        ThrowUtils.throwIf(!b,ErrorCode.SYSTEM_ERROR,"更新失败");
        return ResultUtils.success(b);

    }

//=====================================================查询=========================================================//

    /**
     * 管理员查询单个图片信息
     *
     * @param id
     * @param httpServletRequest
     * @return
     */
    @AuthCheck(userRole = UserConstant.ADMIN)
    @GetMapping("/admin/query")
    public BaseResponse<Picture> queryPictureAdmin(Long id, HttpServletRequest httpServletRequest) {

        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR, "参数为空");
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "没查到相关图片");
        return ResultUtils.success(picture);

    }

    /**
     * 用户分页查询
     *
     * @param pictureQueryRequest
     * @param httpServletRequest
     * @return
     */
    @GetMapping("/page/user/query")
    public BaseResponse<Page<PictureVO>> queryPictureByPageUser( PictureQueryRequest pictureQueryRequest, HttpServletRequest httpServletRequest) {

        int page = pictureQueryRequest.getPage();
        int size = pictureQueryRequest.getSize();
        ThrowUtils.throwIf(size>20,ErrorCode.SYSTEM_ERROR,"请求错误");
        //创建分页对象
        Page<Picture> page1 = new Page<>(page, size);
        //获取查询条件
        QueryWrapper<Picture> queryWrapper = pictureService.getQueryWrapper(pictureQueryRequest);
        //分页查询
        Page<Picture> page2 = pictureService.page(page1, queryWrapper);
        //查询结果脱敏
        Page<PictureVO> pictureVOList = pictureService.getPictureVOList(page2, httpServletRequest);
        ThrowUtils.throwIf(pictureVOList.getTotal() == 0,ErrorCode.NOT_FOUND_ERROR,"没查到相关图片");
        return ResultUtils.success(pictureVOList);

    }

    /**
     * 管理员分页查询
     *
     * @param pictureQueryRequest
     * @param httpServletRequest
     * @return
     */
    @AuthCheck(userRole = UserConstant.ADMIN)
    @GetMapping("/page/admin/query")
    public BaseResponse<Page<Picture>> queryPictureByPageAdmin( PictureQueryRequest pictureQueryRequest, HttpServletRequest httpServletRequest) {

        int page = pictureQueryRequest.getPage();
        int size = pictureQueryRequest.getSize();
        Page<Picture> page1 = new Page<>(page, size);
        QueryWrapper<Picture> queryWrapper = pictureService.getQueryWrapper(pictureQueryRequest);
        Page<Picture> picturePage = pictureService.page(page1, queryWrapper);
        ThrowUtils.throwIf(picturePage.getTotal() == 0,ErrorCode.NOT_FOUND_ERROR,"没查到相关图片");
        return ResultUtils.success(picturePage);

    }
    /**
     * Redis缓存查询
     */
    @PostMapping("/page/user/query/redisCache")
    public BaseResponse<Page<PictureVO>> queryPictureByPageWithRedisCache ( PictureQueryRequest pictureQueryRequest, HttpServletRequest httpServletRequest) {

        ThrowUtils.throwIf(pictureQueryRequest == null,ErrorCode.PARAMS_ERROR,"查询参数不能为空");
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        String pictureJson = cacheManager.queryByRedisCache(pictureQueryRequest, httpServletRequest);
        Page<PictureVO> pictureVOList = JSONUtil.toBean(pictureJson, Page.class);
        log.info("通过redis缓存查询");
        return ResultUtils.success(pictureVOList);

    }
    /**
     * 本地缓存查询
     */
    @PostMapping("/page/user/query/caffeine")
    public BaseResponse<Page<PictureVO>> queryPictureByPageWithCaffeine ( PictureQueryRequest pictureQueryRequest, HttpServletRequest httpServletRequest) {

        ThrowUtils.throwIf(pictureQueryRequest == null,ErrorCode.PARAMS_ERROR,"查询参数不能为空");
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        String pictureJson = cacheManager.queryByCaffeine(pictureQueryRequest, httpServletRequest);
        Page<PictureVO> pictureVOList = JSONUtil.toBean(pictureJson, Page.class);
        log.info("通过本地缓存查询");
        return ResultUtils.success(pictureVOList);

    }
    /**
     * 多级缓存查询图片
     * @param pictureQueryRequest
     * @param httpServletRequest
     * @return
     */
    @PostMapping("/page/user/query/multiCache")
    public BaseResponse<Page<PictureVO>> queryPictureByPageWithMultiCache(PictureQueryRequest pictureQueryRequest, HttpServletRequest httpServletRequest) {

        ThrowUtils.throwIf(pictureQueryRequest == null,ErrorCode.PARAMS_ERROR,"请求参数为空");
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        String pictureJson = cacheManager.queryByMultiCache(pictureQueryRequest,httpServletRequest);
        Page<PictureVO> pictureVOList = JSONUtil.toBean(pictureJson, Page.class);
        log.info("通过多级缓存查询");
        return ResultUtils.success(pictureVOList);

    }
//=====================================================其他=========================================================//
    /**
     * 推荐标签和分类列表
     * @return
     */
    @GetMapping("tag_category")
    public BaseResponse<PictureTagCategory> getPictureTagCategory() {

        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tags = Arrays.asList("风景", "人文", "动画", "旅游", "骑行", "文化");
        List<String> category = Arrays.asList("商业", "校园", "生活");
        pictureTagCategory.setTags(tags);
        pictureTagCategory.setCategory(category);
        return ResultUtils.success(pictureTagCategory);

    }

    /**
     * 审核图片
     * @param pictureReviewRequest
     * @param httpServletRequest
     * @return
     */
    @PostMapping("review")
    @AuthCheck(userRole = UserConstant.ADMIN)
    public BaseResponse<Boolean> PictureReview(PictureReviewRequest pictureReviewRequest,HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(ObjectUtil.isNull(pictureReviewRequest),ErrorCode.PARAMS_ERROR,"参数为空");
        User userLogin = userService.getLoginSession(httpServletRequest);
        pictureService.pictureReview(pictureReviewRequest,userLogin);
        return ResultUtils.success(true);
    }

}
