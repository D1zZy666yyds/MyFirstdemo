package com.dzy666.demo.mapper;

import com.dzy666.demo.entity.Category;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CategoryMapper {

    @Insert("INSERT INTO categories(name, parent_id, user_id, sort_order, created_time) " +
            "VALUES(#{name}, #{parentId}, #{userId}, #{sortOrder}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Category category);

    @Select("SELECT id, name, parent_id as parentId, user_id as userId, " +
            "sort_order as sortOrder, created_time as createdTime " +
            "FROM categories WHERE id = #{id} AND user_id = #{userId}")
    Category selectByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    @Select("SELECT id, name, parent_id as parentId, user_id as userId, " +
            "sort_order as sortOrder, created_time as createdTime " +
            "FROM categories WHERE user_id = #{userId} ORDER BY sort_order, created_time")
    List<Category> selectByUserId(Long userId);

    @Select("SELECT id, name, parent_id as parentId, user_id as userId, " +
            "sort_order as sortOrder, created_time as createdTime " +
            "FROM categories WHERE user_id = #{userId} AND parent_id = #{parentId} " +
            "ORDER BY sort_order, created_time")
    List<Category> selectByParentId(@Param("userId") Long userId, @Param("parentId") Long parentId);

    @Select("SELECT id, name, parent_id as parentId, user_id as userId, " +
            "sort_order as sortOrder, created_time as createdTime " +
            "FROM categories WHERE user_id = #{userId} AND parent_id IS NULL " +
            "ORDER BY sort_order, created_time")
    List<Category> selectRootCategories(Long userId);

    @Update("UPDATE categories SET name=#{name}, parent_id=#{parentId}, sort_order=#{sortOrder} " +
            "WHERE id=#{id} AND user_id=#{userId}")
    int update(Category category);

    @Delete("DELETE FROM categories WHERE id = #{id} AND user_id = #{userId}")
    int deleteByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM categories WHERE parent_id = #{categoryId} AND user_id = #{userId}")
    int countChildren(@Param("categoryId") Long categoryId, @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM documents WHERE category_id = #{categoryId} AND user_id = #{userId}")
    int countDocumentsInCategory(@Param("categoryId") Long categoryId, @Param("userId") Long userId);

    // 获取用户分类数量
    @Select("SELECT COUNT(*) FROM categories WHERE user_id = #{userId}")
    int countByUserId(Long userId);

    // 新增方法：更新分类排序
    @Update("UPDATE categories SET sort_order = #{sortOrder} WHERE id = #{id} AND user_id = #{userId}")
    int updateSortOrder(@Param("id") Long id, @Param("userId") Long userId, @Param("sortOrder") Integer sortOrder);

    // 新增方法：更新父分类ID
    @Update("UPDATE categories SET parent_id = #{parentId} WHERE id = #{id} AND user_id = #{userId}")
    int updateParentId(@Param("id") Long id, @Param("userId") Long userId, @Param("parentId") Long parentId);

    // 新增方法：搜索分类
    @Select("SELECT id, name, parent_id as parentId, user_id as userId, " +
            "sort_order as sortOrder, created_time as createdTime " +
            "FROM categories WHERE user_id = #{userId} AND name LIKE CONCAT('%', #{keyword}, '%') " +
            "ORDER BY sort_order, created_time")
    List<Category> searchByName(@Param("keyword") String keyword, @Param("userId") Long userId);
}