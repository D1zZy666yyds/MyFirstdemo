package com.dzy666.demo.service;

import com.dzy666.demo.entity.User;
import com.dzy666.demo.mapper.UserMapper;
import com.dzy666.demo.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class UserService {

    private final UserMapper userMapper;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

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
        user.setCreatedTime(LocalDateTime.now());
        // nickname和bio默认为null，由数据库默认值处理

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

        // 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.update(user);

        return user;
    }

    /**
     * 登出（清除会话）- 修复版本
     */
    public boolean logout(Long userId) {
        try {
            if (redisTemplate != null) {
                // 清除Redis中的会话数据
                String sessionKey = "user_session:" + userId;
                redisTemplate.delete(sessionKey);
            }

            // 更新用户登出时间
            User user = userMapper.selectById(userId);
            if (user != null) {
                user.setLastLogoutTime(LocalDateTime.now());
                userMapper.update(user);
                return true;
            }
            return false;
        } catch (Exception e) {
            System.err.println("登出失败: " + e.getMessage());
            return false;
        }
    }

    public User updateProfile(Long userId, String email, String nickname, String bio) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (email != null && !email.equals(user.getEmail())) {
            User existingUser = userMapper.selectByEmail(email);
            if (existingUser != null && !existingUser.getId().equals(userId)) {
                throw new RuntimeException("邮箱已被使用");
            }
            user.setEmail(email);
        }

        // 允许设置为null或空字符串
        user.setNickname(nickname);
        user.setBio(bio);
        user.setUpdatedTime(LocalDateTime.now());

        userMapper.update(user);
        return userMapper.selectById(userId);
    }

    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (!PasswordUtil.verifyPassword(oldPassword, user.getSalt(), user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }

        String newSalt = PasswordUtil.generateSalt();
        String newEncryptedPassword = PasswordUtil.encryptPassword(newPassword, newSalt);

        return userMapper.updatePassword(userId, newEncryptedPassword, newSalt) > 0;
    }

    public Map<String, Object> getUserStatistics(Long userId) {
        Map<String, Object> stats = new HashMap<>();

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        stats.put("totalDocuments", userMapper.countDocumentsByUserId(userId));
        stats.put("totalCategories", userMapper.countCategoriesByUserId(userId));
        stats.put("totalTags", userMapper.countTagsByUserId(userId));
        stats.put("totalFavorites", userMapper.countFavoritesByUserId(userId));
        stats.put("accountCreated", user.getCreatedTime());
        stats.put("lastLogin", user.getLastLoginTime());

        return stats;
    }

    public Map<String, Object> getUserSessions(Long userId) {
        Map<String, Object> sessions = new HashMap<>();

        if (redisTemplate != null) {
            // 从Redis获取会话信息
            String sessionKey = "user_session:" + userId;
            Map<Object, Object> sessionData = redisTemplate.opsForHash().entries(sessionKey);
            sessions.put("activeSessions", sessionData.size());
            sessions.put("sessions", sessionData);
        } else {
            sessions.put("activeSessions", 1);
            sessions.put("sessions", Map.of("default", "session_management_disabled"));
        }

        sessions.put("currentTime", LocalDateTime.now());
        return sessions;
    }

    public boolean isUsernameAvailable(String username) {
        return userMapper.selectByUsername(username) == null;
    }

    public boolean sendPasswordResetEmail(String email) {
        User user = userMapper.selectByEmail(email);
        if (user == null) {
            // 出于安全考虑，不提示用户是否存在
            return true;
        }

        if (redisTemplate != null) {
            // 生成重置token
            String resetToken = UUID.randomUUID().toString();
            String resetKey = "password_reset:" + resetToken;

            // 存储到Redis，30分钟过期
            Map<String, Object> resetData = new HashMap<>();
            resetData.put("userId", user.getId());
            resetData.put("email", email);
            resetData.put("createdAt", LocalDateTime.now());

            redisTemplate.opsForHash().putAll(resetKey, resetData);
            redisTemplate.expire(resetKey, 30, TimeUnit.MINUTES);

            // 这里应该调用邮件服务发送重置链接
            System.out.println("密码重置链接: http://localhost:8080/reset-password?token=" + resetToken);
        } else {
            System.out.println("Redis未配置，密码重置功能受限");
        }

        return true;
    }

    public String generateUserToken(User user) {
        if (redisTemplate == null) {
            return "session_based"; // 降级为session方式
        }

        String token = UUID.randomUUID().toString();
        String tokenKey = "user_token:" + token;

        Map<String, Object> userSession = new HashMap<>();
        userSession.put("userId", user.getId());
        userSession.put("username", user.getUsername());
        userSession.put("email", user.getEmail());
        userSession.put("loginTime", LocalDateTime.now());

        // 存储到Redis，7天过期
        redisTemplate.opsForHash().putAll(tokenKey, userSession);
        redisTemplate.expire(tokenKey, 7, TimeUnit.DAYS);

        return token;
    }

    public User validateToken(String token) {
        if (redisTemplate == null || "session_based".equals(token)) {
            return null; // 降级处理
        }

        String tokenKey = "user_token:" + token;
        Map<Object, Object> sessionData = redisTemplate.opsForHash().entries(tokenKey);

        if (sessionData.isEmpty()) {
            return null;
        }

        Long userId = Long.valueOf(sessionData.get("userId").toString());
        return userMapper.selectById(userId);
    }

}