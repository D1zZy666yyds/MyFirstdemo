package com.dzy666.demo.service;
// UserService.java

import com.dzy666.demo.entity.User;
import com.dzy666.demo.mapper.UserMapper;
import com.dzy666.demo.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserService {

    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public User register(String username, String email, String password) {
        // 验证用户名和邮箱是否已存在
        if (userMapper.selectByUsername(username) != null) {
            throw new RuntimeException("用户名已存在");
        }
        if (userMapper.selectByEmail(email) != null) {
            throw new RuntimeException("邮箱已被注册");
        }

        // 密码加密
        String salt = PasswordUtil.generateSalt();
        String encryptedPassword = PasswordUtil.encryptPassword(password, salt);

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(encryptedPassword);
        user.setSalt(salt);

        userMapper.insert(user);
        return user;
    }

    public User login(String username, String password) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (!PasswordUtil.verifyPassword(password, user.getSalt(), user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        return user;
    }
}