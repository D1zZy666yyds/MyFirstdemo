package com.dzy666.demo.controller;

import com.dzy666.demo.entity.User;
import com.dzy666.demo.service.UserService;
import com.dzy666.demo.util.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public JsonResult<User> register(@RequestBody User user) {
        try {
            User registeredUser = userService.register(user.getUsername(), user.getEmail(), user.getPassword());
            // 不返回密码和盐
            registeredUser.setPassword(null);
            registeredUser.setSalt(null);
            return JsonResult.success("注册成功", registeredUser);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    @PostMapping("/login")
    public JsonResult<User> login(@RequestBody User user) {
        try {
            User loggedInUser = userService.login(user.getUsername(), user.getPassword());
            // 不返回密码和盐
            loggedInUser.setPassword(null);
            loggedInUser.setSalt(null);
            return JsonResult.success("登录成功", loggedInUser);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }
}