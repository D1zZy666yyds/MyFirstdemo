package com.dzy666.demo.controller;

import com.dzy666.demo.entity.User;
import com.dzy666.demo.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/api")
public class SimpleAuthController {

    @Autowired
    private UserMapper userMapper;

    // 登录接口 - 修复密码验证
    @PostMapping("/login")
    @ResponseBody
    public Map<String, Object> login(@RequestBody Map<String, String> loginData,
                                     HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();

        String username = loginData.get("username");
        String password = loginData.get("password");

        System.out.println("=== 登录请求 ===");
        System.out.println("用户名: " + username);
        System.out.println("输入密码: " + password);

        try {
            // 查询用户
            User user = userMapper.selectByUsername(username);
            if (user == null) {
                System.out.println("用户不存在");
                result.put("success", false);
                result.put("message", "用户不存在");
                return result;
            }

            System.out.println("数据库密码: " + user.getPassword());
            System.out.println("数据库盐值: " + user.getSalt());

            // 简化密码验证 - 直接比较明文
            boolean passwordValid = password.equals(user.getPassword());

            System.out.println("密码验证结果: " + passwordValid);

            if (passwordValid) {
                // 创建session
                HttpSession session = request.getSession();
                session.setAttribute("currentUser", user);
                session.setMaxInactiveInterval(30 * 60);

                // 不返回敏感信息
                user.setPassword(null);
                user.setSalt(null);

                result.put("success", true);
                result.put("message", "登录成功");
                result.put("data", user);

                System.out.println("登录成功: " + username);
            } else {
                System.out.println("密码不匹配");
                System.out.println("输入: " + password);
                System.out.println("数据库: " + user.getPassword());
                result.put("success", false);
                result.put("message", "密码错误");
            }

        } catch (Exception e) {
            System.err.println("登录异常: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "系统错误: " + e.getMessage());
        }

        return result;
    }

    // 注册接口 - 修复版本
    @PostMapping("/register")
    @ResponseBody
    public Map<String, Object> register(@RequestBody Map<String, String> registerData) {
        Map<String, Object> result = new HashMap<>();

        String username = registerData.get("username");
        String email = registerData.get("email");
        String password = registerData.get("password");

        System.out.println("=== 注册请求开始 ===");
        System.out.println("用户名: " + username);
        System.out.println("邮箱: " + email);
        System.out.println("密码长度: " + (password != null ? password.length() : "null"));

        try {
            // 1. 数据验证
            if (username == null || username.trim().isEmpty()) {
                System.out.println("验证失败: 用户名为空");
                result.put("success", false);
                result.put("message", "用户名不能为空");
                return result;
            }

            if (password == null || password.length() < 6) {
                System.out.println("验证失败: 密码长度不足");
                result.put("success", false);
                result.put("message", "密码至少6位");
                return result;
            }

            // 2. 检查用户是否存在
            User existingUser = userMapper.selectByUsername(username);
            System.out.println("检查用户是否存在: " + (existingUser != null ? "存在" : "不存在"));

            if (existingUser != null) {
                result.put("success", false);
                result.put("message", "用户名已存在");
                return result;
            }

            User existingEmail = userMapper.selectByEmail(email);
            System.out.println("检查邮箱是否存在: " + (existingEmail != null ? "存在" : "不存在"));

            if (existingEmail != null) {
                result.put("success", false);
                result.put("message", "邮箱已被注册");
                return result;
            }

            // 3. 创建新用户 - 生成随机盐值
            String salt = generateSalt();
            System.out.println("生成盐值: " + salt);

            User newUser = new User();
            newUser.setUsername(username.trim());
            newUser.setEmail(email.trim());
            newUser.setPassword(password); // 暂时使用明文，后面可以改为加密
            newUser.setSalt(salt);

            System.out.println("准备插入用户: " + newUser.getUsername() + ", " + newUser.getEmail());

            // 4. 插入数据库
            int rows = userMapper.insert(newUser);
            System.out.println("插入结果: " + rows + " 行受影响");
            System.out.println("新用户ID: " + newUser.getId());

            if (rows > 0 && newUser.getId() != null) {
                System.out.println("注册成功");
                // 不返回敏感信息
                newUser.setPassword(null);
                newUser.setSalt(null);

                result.put("success", true);
                result.put("message", "注册成功");
                result.put("data", newUser);
            } else {
                System.out.println("注册失败: 数据库插入失败");
                result.put("success", false);
                result.put("message", "注册失败，请重试");
            }

        } catch (Exception e) {
            System.err.println("注册异常: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "系统错误: " + e.getMessage());
        }

        System.out.println("=== 注册请求结束 ===");
        System.out.println("返回结果: " + result);
        return result;
    }

    // 生成随机盐值
    private String generateSalt() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    // 检查登录状态
    @GetMapping("/check-auth")
    @ResponseBody
    public Map<String, Object> checkAuth(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();

        HttpSession session = request.getSession(false);
        if (session != null) {
            User user = (User) session.getAttribute("currentUser");
            if (user != null) {
                user.setPassword(null);
                user.setSalt(null);
                result.put("success", true);
                result.put("data", user);
                return result;
            }
        }

        result.put("success", false);
        result.put("message", "未登录");
        return result;
    }

    // 退出登录
    @PostMapping("/logout")
    @ResponseBody
    public Map<String, Object> logout(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        result.put("success", true);
        result.put("message", "退出成功");
        return result;
    }

    // 测试接口
    @GetMapping("/test-register")
    @ResponseBody
    public Map<String, Object> testRegister() {
        Map<String, Object> result = new HashMap<>();

        try {
            // 直接测试插入
            User testUser = new User();
            testUser.setUsername("test_" + System.currentTimeMillis());
            testUser.setEmail("test_" + System.currentTimeMillis() + "@test.com");
            testUser.setPassword("123456");
            testUser.setSalt(generateSalt());

            int rows = userMapper.insert(testUser);
            result.put("success", rows > 0);
            result.put("message", "测试插入: " + rows + " 行");
            result.put("userId", testUser.getId());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "测试错误: " + e.getMessage());
        }

        return result;
    }
}