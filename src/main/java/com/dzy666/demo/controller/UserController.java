package com.dzy666.demo.controller;

import com.dzy666.demo.entity.User;
import com.dzy666.demo.service.UserService;
import com.dzy666.demo.util.JsonResult;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 用户注册 - 修复版本
     */
    @PostMapping("/register")
    public JsonResult<User> register(@RequestBody User user) {
        try {
            User registeredUser = userService.register(user.getUsername(), user.getEmail(), user.getPassword());
            // 清除敏感信息
            registeredUser.setPassword(null);
            registeredUser.setSalt(null);
            return JsonResult.success("注册成功", registeredUser);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    /**
     * 用户登录 - 修复版本
     */
    @PostMapping("/login")
    public JsonResult<User> login(@RequestBody User user, HttpSession session) {
        try {
            User loggedInUser = userService.login(user.getUsername(), user.getPassword());
            // 清除敏感信息
            loggedInUser.setPassword(null);
            loggedInUser.setSalt(null);

            // 将用户信息存入session
            session.setAttribute("currentUser", loggedInUser);
            System.out.println("用户登录成功: " + loggedInUser.getUsername() + ", Session ID: " + session.getId());

            return JsonResult.success("登录成功", loggedInUser);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    /**
     * 检查认证状态 - 修复版本
     */
    @GetMapping("/check-auth")
    public JsonResult<User> checkAuth(HttpSession session) {
        try {
            User user = (User) session.getAttribute("currentUser");
            if (user != null) {
                System.out.println("检查认证状态: 用户已登录 - " + user.getUsername());
                // 返回用户信息（不包含敏感信息）
                User safeUser = new User();
                safeUser.setId(user.getId());
                safeUser.setUsername(user.getUsername());
                safeUser.setEmail(user.getEmail());
                safeUser.setNickname(user.getNickname());
                safeUser.setBio(user.getBio());
                safeUser.setLastLoginTime(user.getLastLoginTime());
                return JsonResult.success("用户已登录", safeUser);
            } else {
                System.out.println("检查认证状态: 用户未登录");
                return JsonResult.error(401, "用户未登录");
            }
        } catch (Exception e) {
            System.err.println("检查认证状态失败: " + e.getMessage());
            return JsonResult.error("检查认证状态失败: " + e.getMessage());
        }
    }

    /**
     * 退出登录 - 修复版本
     */
    @PostMapping("/logout")
    public JsonResult<Boolean> logout(HttpSession session) {
        try {
            User user = (User) session.getAttribute("currentUser");
            if (user != null) {
                System.out.println("用户退出登录: " + user.getUsername());
                userService.logout(user.getId());
                session.invalidate();
            }
            return JsonResult.success("登出成功", true);
        } catch (Exception e) {
            System.err.println("登出失败: " + e.getMessage());
            return JsonResult.error("登出失败: " + e.getMessage());
        }
    }

    /**
     * 更新用户资料 - 修复版本
     */
    @PutMapping("/user/profile")
    public JsonResult<User> updateProfile(@RequestBody Map<String, Object> profileData, HttpSession session) {
        try {
            User currentUser = (User) session.getAttribute("currentUser");
            if (currentUser == null) {
                return JsonResult.error(401, "用户未登录");
            }

            String email = (String) profileData.get("email");
            String nickname = (String) profileData.get("nickname");
            String bio = (String) profileData.get("bio");

            User updatedUser = userService.updateProfile(currentUser.getId(), email, nickname, bio);
            // 清除敏感信息
            updatedUser.setPassword(null);
            updatedUser.setSalt(null);

            // 更新session中的用户信息
            session.setAttribute("currentUser", updatedUser);

            return JsonResult.success("资料更新成功", updatedUser);
        } catch (Exception e) {
            return JsonResult.error("资料更新失败: " + e.getMessage());
        }
    }

    /**
     * 修改密码 - 新增接口
     */
    /**
     * 修改密码 - 新增接口
     */
    @PostMapping("/change-password")
    public JsonResult<Boolean> changePassword(@RequestBody Map<String, String> passwordData, HttpSession session) {
        try {
            // 1. 检查用户是否登录
            User currentUser = (User) session.getAttribute("currentUser");
            if (currentUser == null) {
                System.out.println("修改密码失败: 用户未登录");
                return JsonResult.error(401, "用户未登录");
            }

            // 2. 获取参数
            String oldPassword = passwordData.get("oldPassword");
            String newPassword = passwordData.get("newPassword");

            // 3. 参数验证
            if (oldPassword == null || oldPassword.trim().isEmpty()) {
                return JsonResult.error("请输入原密码");
            }

            if (newPassword == null || newPassword.trim().isEmpty()) {
                return JsonResult.error("请输入新密码");
            }

            if (newPassword.length() < 6) {
                return JsonResult.error("新密码至少需要6个字符");
            }

            // 4. 调用Service修改密码
            boolean success = userService.changePassword(currentUser.getId(), oldPassword, newPassword);

            if (success) {
                System.out.println("用户 " + currentUser.getUsername() + " 密码修改成功");
                return JsonResult.success("密码修改成功", true);
            } else {
                System.out.println("用户 " + currentUser.getUsername() + " 密码修改失败");
                return JsonResult.error("密码修改失败");
            }

        } catch (RuntimeException e) {
            System.err.println("修改密码失败: " + e.getMessage());
            return JsonResult.error(e.getMessage());
        } catch (Exception e) {
            System.err.println("修改密码失败: " + e.getMessage());
            return JsonResult.error("修改密码失败: " + e.getMessage());
        }
    }
    // 其他方法保持不变...
}