package com.dzy666.demo.mapper;

import com.dzy666.demo.entity.Document;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface DocumentMapper {

    @Insert("INSERT INTO documents(title, content, content_type, category_id, user_id, created_time, updated_time, deleted) " +
            "VALUES(#{title}, #{content}, #{contentType}, #{categoryId}, #{userId}, NOW(), NOW(), 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Document document);

    // 逻辑删除
    @Update("UPDATE documents SET deleted = 1, deleted_time = NOW() WHERE id = #{id} AND user_id = #{userId}")
    int softDeleteByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    // 恢复文档
    @Update("UPDATE documents SET deleted = 0, deleted_time = NULL WHERE id = #{id} AND user_id = #{userId}")
    int restoreDocument(@Param("id") Long id, @Param("userId") Long userId);

    // 物理删除
    @Delete("DELETE FROM documents WHERE id = #{id} AND user_id = #{userId}")
    int permanentDelete(@Param("id") Long id, @Param("userId") Long userId);

    // 查询正常文档（排除已删除的）
    @Select("SELECT id, title, content, content_type as contentType, category_id as categoryId, " +
            "user_id as userId, created_time as createdTime, updated_time as updatedTime, " +
            "deleted, deleted_time as deletedTime " +
            "FROM documents WHERE id = #{id} AND user_id = #{userId} AND deleted = 0")
    Document selectByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    // 查询用户的所有正常文档
    @Select("SELECT id, title, content, content_type as contentType, category_id as categoryId, " +
            "user_id as userId, created_time as createdTime, updated_time as updatedTime, " +
            "deleted, deleted_time as deletedTime " +
            "FROM documents WHERE user_id = #{userId} AND deleted = 0 ORDER BY updated_time DESC")
    List<Document> selectByUserId(Long userId);

    // 查询回收站中的文档
    @Select("SELECT id, title, content, content_type as contentType, category_id as categoryId, " +
            "user_id as userId, created_time as createdTime, updated_time as updatedTime, " +
            "deleted, deleted_time as deletedTime " +
            "FROM documents WHERE user_id = #{userId} AND deleted = 1 ORDER BY deleted_time DESC")
    List<Document> selectDeletedByUserId(Long userId);

    // 根据分类查询文档
    @Select("SELECT id, title, content, content_type as contentType, category_id as categoryId, " +
            "user_id as userId, created_time as createdTime, updated_time as updatedTime, " +
            "deleted, deleted_time as deletedTime " +
            "FROM documents WHERE category_id = #{categoryId} AND user_id = #{userId} AND deleted = 0 " +
            "ORDER BY updated_time DESC")
    List<Document> selectByCategoryIdAndUser(@Param("categoryId") Long categoryId, @Param("userId") Long userId);

    // 数据库模糊搜索（添加这个方法）
    @Select("SELECT id, title, content, content_type as contentType, category_id as categoryId, " +
            "user_id as userId, created_time as createdTime, updated_time as updatedTime, " +
            "deleted, deleted_time as deletedTime " +
            "FROM documents WHERE user_id = #{userId} AND deleted = 0 " +
            "AND (title LIKE CONCAT('%', #{keyword}, '%') OR content LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY updated_time DESC")
    List<Document> searchByKeyword(@Param("keyword") String keyword, @Param("userId") Long userId);

    // 更新文档
    @Update("UPDATE documents SET title=#{title}, content=#{content}, category_id=#{categoryId}, " +
            "updated_time=NOW() WHERE id=#{id} AND user_id=#{userId}")
    int update(Document document);

    // 在 DocumentMapper 中添加
    @Select("SELECT d.id, d.title, d.content, d.content_type as contentType, d.category_id as categoryId, " +
            "d.user_id as userId, d.created_time as createdTime, d.updated_time as updatedTime, " +
            "d.deleted, d.deleted_time as deletedTime " +
            "FROM documents d INNER JOIN document_tags dt ON d.id = dt.document_id " +
            "WHERE dt.tag_id = #{tagId} AND d.user_id = #{userId} AND d.deleted = 0")
    List<Document> selectByTagId(@Param("tagId") Long tagId, @Param("userId") Long userId);

    // 获取用户文档总数
    @Select("SELECT COUNT(*) FROM documents WHERE user_id = #{userId} AND deleted = 0")
    int countByUserId(Long userId);

    // 获取用户最近创建的文档（用于最近文档列表）
    @Select("SELECT id, title, created_time as createdTime, '默认分类' as category " +
            "FROM documents WHERE user_id = #{userId} AND deleted = 0 ORDER BY created_time DESC LIMIT #{limit}")
    List<Map<String, Object>> selectRecentDocuments(@Param("userId") Long userId, @Param("limit") int limit);


}