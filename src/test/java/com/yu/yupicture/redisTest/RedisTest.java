package com.yu.yupicture.redisTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;
@SpringBootTest
public class RedisTest {
    @Resource
    RedisTemplate redisTemplate;
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Test
    public void test(){
        ValueOperations valueOperations = redisTemplate.opsForValue();
        String key = "class";
        String value = "class1";
        valueOperations.set(key,value);
        Object o = valueOperations.get(key);
        System.out.println(o);
        String key1  = "fruit";
        Object o1 = valueOperations.get(key1);
        System.out.println(o1);
        ValueOperations<String, String> stringStringValueOperations = stringRedisTemplate.opsForValue();
        String s = stringStringValueOperations.get(key1);
        System.out.println(s);}
}
