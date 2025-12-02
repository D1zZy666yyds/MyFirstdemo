package com.dzy666.demo.mapper;

import com.dzy666.demo.entity.Tag;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TagMapper {

    @Insert("INSERT INTO tags(name, user_id, created_time) VALUES(#{name}, #{userId}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Tag tag);

    @Select("SELECT id, name, user_id as userId, created_time as createdTime " +
            "FROM tags WHERE id = #{id} AND user_id = #{userId}")
    Tag selectByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    @Select("SELECT id, name, user_id as userId, created_time as createdTime " +
            "FROM tags WHERE user_id = #{userId} ORDER BY created_time DESC")
    List<Tag> selectByUserId(Long userId);

    @Select("SELECT id, name, user_id as userId, created_time as createdTime " +
            "FROM tags WHERE user_id = #{userId} AND name = #{name}")
    Tag selectByNameAndUser(@Param("name") String name, @Param("userId") Long userId);

    @Update("UPDATE tags SET name = #{name} WHERE id = #{id} AND user_id = #{userId}")
    int update(Tag tag);

    @Delete("DELETE FROM tags WHERE id = #{id} AND user_id = #{userId}")
    int deleteByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 统计标签关联的文档数量（只统计未删除的文档）
     * 修复：添加用户ID过滤，确保只统计当前用户的文档
     */
    @Select("SELECT COUNT(DISTINCT dt.document_id) " +
            "FROM document_tags dt " +
            "INNER JOIN documents d ON dt.document_id = d.id " +
            "WHERE dt.tag_id = #{tagId} AND d.user_id = #{userId} AND d.deleted = 0")
    int countDocumentsByTag(@Param("tagId") Long tagId, @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM tags WHERE user_id = #{userId}")
    int countByUserId(Long userId);

    // 根据文档ID查询标签
    @Select("SELECT t.id, t.name, t.user_id as userId, t.created_time as createdTime " +
            "FROM tags t INNER JOIN document_tags dt ON t.id = dt.tag_id " +
            "WHERE dt.document_id = #{documentId} AND t.user_id = #{userId}")
    List<Tag> selectByDocumentId(@Param("documentId") Long documentId, @Param("userId") Long userId);
}