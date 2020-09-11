package com.kk.linyuanbin.demoforredis.controller;

import com.kk.linyuanbin.demoforredis.service.Chapter01;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author linyuanbin
 * @description controller to chapter01
 * @date 2020/9/10-17:52
 */
@RestController
public class Chapter01Controller {
    @Autowired
    Chapter01 chapter01;

    @PostMapping("/")
    public String post(){
        chapter01.post();
        return "success";
    }

    @GetMapping("/")
    public String get(){
        return chapter01.get();
    }
}
