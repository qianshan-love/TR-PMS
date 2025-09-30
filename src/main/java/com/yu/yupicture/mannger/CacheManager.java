package com.yu.yupicture.mannger;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yu.yupicture.common.ErrorCode;
import com.yu.yupicture.exception.ThrowUtils;
import com.yu.yupicture.modle.dto.picture.PictureQueryRequest;
import com.yu.yupicture.modle.entity.Picture;
import com.yu.yupicture.modle.enums.PictureReviewStatusEnum;
import com.yu.yupicture.modle.vo.PictureVO;
import com.yu.yupicture.service.PictureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class CacheManager {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private PictureService pictureService;
    //创建cache对象
    int time = 300 + RandomUtil.randomInt(0,300);
    private Cache<Object, Object> cache = Caffeine.newBuilder()
            .maximumSize(5000L)
            .expireAfterWrite(time, TimeUnit.HOURS)
            .build();

    /**
     * 多级缓存查询图片
     * @param pictureQueryRequest
     * @param httpServletRequest
     * @return
     */
    public String queryByMultiCache(PictureQueryRequest pictureQueryRequest, HttpServletRequest httpServletRequest){

        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR,"查询参数不能为空");
        int page = pictureQueryRequest.getPage();
        int size = pictureQueryRequest.getSize();
        //防抓取
        ThrowUtils.throwIf(size > 20,ErrorCode.PARAMS_ERROR,"查询数量过多");
        //将请求转换为json字符串
        String picRequestJson = JSONUtil.toJsonStr(pictureQueryRequest);
        //压缩md5算法压缩字符串长度
        String hashKey = DigestUtils.md5DigestAsHex(picRequestJson.getBytes());
        //创建本地缓存key
        String cacheKey = "yuPicture:" + hashKey;
        //本地缓存中查询图片
        String cachePicture = (String)cache.getIfPresent(cacheKey);
        if (cachePicture != null) {
            return cachePicture;
        }
        //获取redis操作对象
        ValueOperations<String, String> stringStringValueOperations = stringRedisTemplate.opsForValue();
        //创建redis缓存key
        String redisKey = "yuPicture:ListPicturePageWithCache" + hashKey;
        //redis中查询图片
        String redisPicture = stringStringValueOperations.get(redisKey);
        if (redisPicture != null) {
            cache.put(cacheKey,redisPicture);
            log.info("已存入本地缓存");
            return redisPicture;
        }
        //数据库中查询图片
        Page<Picture> mysqlPicture = pictureService.page(new Page<>(page, size), pictureService.getQueryWrapper(pictureQueryRequest));
        //将图片进行脱敏
        Page<PictureVO> pictureVOList = pictureService.getPictureVOList(mysqlPicture, httpServletRequest);
        //将图片转换成字符串
        String mysqlPictureJson = JSONUtil.toJsonStr(pictureVOList);
        //将查询的图片存进本地缓存
        cache.put(cacheKey,mysqlPictureJson);
        //将查询的图片存进redis缓存
        stringStringValueOperations.set(redisKey,mysqlPictureJson,time,TimeUnit.SECONDS);
        return mysqlPictureJson;

    }

    /**
     * Redis缓存查询图片
     * @param pictureQueryRequest
     * @param httpServletRequest
     * @return
     */
    public String queryByRedisCache (PictureQueryRequest pictureQueryRequest, HttpServletRequest httpServletRequest) {

        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR,"查询参数不能为空");
        int page = pictureQueryRequest.getPage();
        int size = pictureQueryRequest.getSize();
        //防抓取
        ThrowUtils.throwIf(size > 20,ErrorCode.PARAMS_ERROR,"查询数量过多");
        //将请求转换为json字符串
        String pictureQueryRequestJson = JSONUtil.toJsonStr(pictureQueryRequest);
        //使用md5进行加密压缩长度
        String newPictureQueryRequestJson = DigestUtils.md5DigestAsHex(pictureQueryRequestJson.getBytes());
        //设置redis存储键值
        String redisKey = "yuPicture:ListPicturePageWithCache" + newPictureQueryRequestJson;
        //获取操作redis模板对象
        ValueOperations<String, String> redisTemplate = stringRedisTemplate.opsForValue();
        //先从redis缓存中查询
        String picture = redisTemplate.get(redisKey);
        if (picture != null) {
            return picture;
        }
        //从数据库中查询
        Page<Picture> page1 = pictureService.page(new Page<>(page, size), pictureService.getQueryWrapper(pictureQueryRequest));
        Page<PictureVO> pictureVOList = pictureService.getPictureVOList(page1, httpServletRequest);
        String jsonStr = JSONUtil.toJsonStr(pictureVOList);
        //存储进缓存中
        redisTemplate.set(redisKey,jsonStr,time, TimeUnit.SECONDS);
        return jsonStr;

    }

    /**
     * 本都缓存查询图片
     * @param pictureQueryRequest
     * @param httpServletRequest
     * @return
     */
    public String queryByCaffeine(PictureQueryRequest pictureQueryRequest, HttpServletRequest httpServletRequest){

        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR,"查询参数不能为空");
        int page = pictureQueryRequest.getPage();
        int size = pictureQueryRequest.getSize();
        //防抓取
        ThrowUtils.throwIf(size > 20,ErrorCode.PARAMS_ERROR,"查询数量过多");
        //将请求转换为json字符串
        String picRequestJson = JSONUtil.toJsonStr(pictureQueryRequest);
        //压缩md5算法压缩字符串长度
        String hashKey = DigestUtils.md5DigestAsHex(picRequestJson.getBytes());
        //创建本地缓存key
        String cacheKey = "yuPicture:" + hashKey;
        //本地缓存中查询图片
        String cachePicture = (String)cache.getIfPresent(cacheKey);
        if (cachePicture != null) {
            return cachePicture;
        }
        //数据库中查询图片
        Page<Picture> mysqlPicture = pictureService.page(new Page<>(page, size), pictureService.getQueryWrapper(pictureQueryRequest));
        //将图片进行脱敏
        Page<PictureVO> pictureVOList = pictureService.getPictureVOList(mysqlPicture, httpServletRequest);
        //将图片转换成字符串
        String mysqlPictureJson = JSONUtil.toJsonStr(pictureVOList);
        //将查询的图片存进本地缓存
        cache.put(cacheKey,mysqlPictureJson);
        return mysqlPictureJson;

    }
}
