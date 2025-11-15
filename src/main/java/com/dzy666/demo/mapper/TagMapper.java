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

    @Select("SELECT COUNT(*) FROM document_tags WHERE tag_id = #{tagId}")
    int countDocumentsByTag(Long tagId);

    @Select("SELECT COUNT(*) FROM tags WHERE user_id = #{userId}")
    int countByUserId(Long userId);
    // 根据文档ID查询标签 - 修复参数注解
    @Select("SELECT t.id, t.name, t.user_id as userId, t.created_time as createdTime " +
            "FROM tags t INNER JOIN document_tags dt ON t.id = dt.tag_id " +
            "WHERE dt.document_id = #{documentId} AND t.user_id = #{userId}")
    List<Tag> selectByDocumentId(@Param("documentId") Long documentId, @Param("userId") Long userId);
}