package com.smart.retry.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 前端路由控制器
 * 将所有非API请求转发到 index.html，由Vue Router处理
 */
@Controller
public class ForwardController {
    
    /**
     * 将所有非API、非静态资源的请求转发到 index.html
     * 支持 Vue Router 的 History 模式
     */
    @GetMapping(value = {
        "/dashboard",
        "/instance", 
        "/task",
        "/dashboard/**",
        "/instance/**",
        "/task/**"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
