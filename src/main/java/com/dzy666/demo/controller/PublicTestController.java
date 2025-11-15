package com.dzy666.demo.controller;

import com.dzy666.demo.entity.User;
import com.dzy666.demo.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/public")
public class PublicTestController {

    @Autowired
    private UserMapper userMapper;

    // 完全公开的测试接口
    @GetMapping("/test")
    public Map<String, Object> test() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "公开接口正常工作");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    // 完全公开的注册接口
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> registerData) {
        Map<String, Object> result = new HashMap<>();

        String username = registerData.get("username");
        String email = registerData.get("email");
        String password = registerData.get("password");

        System.out.println("=== 公开注册接口 ===");
        System.out.println("用户名: " + username);

        try {
            // 简单验证
            if (username == null || username.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "用户名不能为空");
                return result;
            }

            // 检查用户是否存在
            if (userMapper.selectByUsername(username) != null) {
                result.put("success", false);
                result.put("message", "用户名已存在");
                return result;
            }

            // 创建新用户
            User newUser = new User();
            newUser.setUsername(username.trim());
            newUser.setEmail(email.trim());
            newUser.setPassword(password);
            newUser.setSalt(UUID.randomUUID().toString().substring(0, 16));

            int rows = userMapper.insert(newUser);

            if (rows > 0) {
                newUser.setPassword(null);
                result.put("success", true);
                result.put("message", "注册成功");
                result.put("data", newUser);
                System.out.println("注册成功: " + username);
            } else {
                result.put("success", false);
                result.put("message", "注册失败");
            }

        } catch (Exception e) {
            System.err.println("注册异常: " + e.getMessage());
            result.put("success", false);
            result.put("message", "系统错误: " + e.getMessage());
        }

        return result;
    }
}