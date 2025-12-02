package com.dzy666.demo.mapper;

import com.dzy666.demo.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {

    // 修复INSERT语句 - 移除nickname和bio字段
    @Insert("INSERT INTO users(username, email, password, salt, created_time) " +
            "VALUES(#{username}, #{email}, #{password}, #{salt}, #{createdTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Select("SELECT * FROM users WHERE id = #{id}")
    User selectById(Long id);

    @Select("SELECT * FROM users WHERE username = #{username}")
    User selectByUsername(String username);

    @Select("SELECT * FROM users WHERE email = #{email}")
    User selectByEmail(String email);

    @Update("UPDATE users SET password = #{password}, salt = #{salt}, updated_time = NOW() WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password, @Param("salt") String salt);

    // 修复UPDATE语句 - 处理nickname和bio可能为null的情况
    @Update("<script>" +
            "UPDATE users SET " +
            "email = #{email}, " +
            "<if test='nickname != null'>nickname = #{nickname},</if>" +
            "<if test='bio != null'>bio = #{bio},</if>" +
            "last_login_time = #{lastLoginTime}, " +
            "last_logout_time = #{lastLogoutTime}, " +
            "updated_time = #{updatedTime} " +
            "WHERE id = #{id}" +
            "</script>")
    int update(User user);

    @Update("UPDATE users SET last_login_time = NOW() WHERE id = #{userId}")
    int updateLastLoginTime(Long userId);

    // 统计相关方法保持不变
    @Select("SELECT COUNT(*) FROM documents WHERE user_id = #{userId} AND deleted = 0")
    Integer countDocumentsByUserId(Long userId);

    @Select("SELECT COUNT(*) FROM categories WHERE user_id = #{userId}")
    Integer countCategoriesByUserId(Long userId);

    @Select("SELECT COUNT(*) FROM tags WHERE user_id = #{userId}")
    Integer countTagsByUserId(Long userId);

    @Select("SELECT COUNT(*) FROM favorites WHERE user_id = #{userId}")
    Integer countFavoritesByUserId(Long userId);
}