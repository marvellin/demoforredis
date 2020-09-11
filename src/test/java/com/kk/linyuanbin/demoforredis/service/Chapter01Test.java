package com.kk.linyuanbin.demoforredis.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;

/**
 * @author linyuanbin
 * @description
 * @date 2020/9/10-16:50
 */
class Chapter01Test {
    @Resource
    private RedisTemplate<Object, Object> redisTemplate;

    @Test
    public void run() {
        redisTemplate.opsForValue().set("myRedisTestInDemoForRedis",String.valueOf(System.currentTimeMillis()));
    }
}