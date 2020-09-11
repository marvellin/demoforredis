package com.kk.linyuanbin.demoforredis.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * @author linyuanbin
 * @description functions for article management
 * @date 2020/9/10-15:06
 */
@Service
public class Chapter01Imple implements Chapter01{
    private static final int ONE_WEEK_IN_SECONDS = 7 * 60 * 60 * 24;
    private static final int VOTE_SCORE = ONE_WEEK_IN_SECONDS / 200;
    private static final int ARTICLES_PER_PAGE = 25;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    public static void main(String[] args) {

    }

    public void run(){
        redisTemplate.opsForValue().set("myRedisTestInDemoForRedis","test");
    }

    @Override
    public void post() {
        redisTemplate.opsForValue().set("myRedisTestInDemoForRedis","test");
    }

    @Override
    public String get() {
        return (String) redisTemplate.opsForValue().get("myRedisTestInDemoForRedis");
    }
}
