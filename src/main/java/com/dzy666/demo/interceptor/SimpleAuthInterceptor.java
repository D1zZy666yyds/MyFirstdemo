package com.dzy666.demo.interceptor;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class SimpleAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();

        // 公开路径直接放行
        if (isPublicPath(path)) {
            return true;
        }

        // 检查登录状态
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("currentUser") == null) {
            // API请求返回401，页面请求重定向到登录页
            if (path.startsWith("/api/")) {
                response.setStatus(401);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"success\":false,\"message\":\"请先登录\",\"code\":401}");
            } else {
                response.sendRedirect("/login");
            }
            return false;
        }

        return true;
    }

    private boolean isPublicPath(String path) {
        return path.equals("/") ||
                path.equals("/login") ||
                path.startsWith("/api/login") ||
                path.startsWith("/api/register") ||
                path.startsWith("/api/check-auth") ||
                path.startsWith("/css/") ||
                path.startsWith("/js/") ||
                path.startsWith("/images/") ||
                path.equals("/error") ||
                path.equals("/favicon.ico");
    }
}