package com.dzy666.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StaticResourceController {

    /**
     * 处理Vue路由的SPA回退 - 修复版本
     */
    @GetMapping({
            "/",
            "/dashboard",
            "/documents",
            "/categories",
            "/tags",
            "/search",
            "/statistics",
            "/knowledge-graph",
            "/backup",
            "/settings",
            "/login",
            "/login-new",
            "/document-view"
    })
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}