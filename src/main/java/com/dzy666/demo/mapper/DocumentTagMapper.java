package com.dzy666.demo.mapper;

import com.dzy666.demo.entity.Tag;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DocumentTagMapper {

    @Insert("INSERT INTO document_tags(document_id, tag_id) VALUES(#{documentId}, #{tagId})")
    int insert(@Param("documentId") Long documentId, @Param("tagId") Long tagId);

    @Delete("DELETE FROM document_tags WHERE document_id = #{documentId} AND tag_id = #{tagId}")
    int delete(@Param("documentId") Long documentId, @Param("tagId") Long tagId);

    @Delete("DELETE FROM document_tags WHERE document_id = #{documentId}")
    int deleteByDocumentId(Long documentId);

    @Delete("DELETE FROM document_tags WHERE tag_id = #{tagId}")
    int deleteByTagId(Long tagId);

    @Select("SELECT COUNT(*) FROM document_tags WHERE document_id = #{documentId} AND tag_id = #{tagId}")
    int exists(@Param("documentId") Long documentId, @Param("tagId") Long tagId);

    @Select("SELECT tag_id FROM document_tags WHERE document_id = #{documentId}")
    List<Long> selectTagIdsByDocumentId(Long documentId);

    /**
     * 修复：将 d.deleted = false 改为 d.deleted = 0
     */
    @Select("SELECT d.id FROM documents d " +
            "JOIN document_tags dt ON d.id = dt.document_id " +
            "WHERE dt.tag_id = #{tagId} AND d.user_id = #{userId} AND d.deleted = 0")
    List<Long> findDocumentIdsByTagIdAndUserId(@Param("tagId") Long tagId, @Param("userId") Long userId);

    @Select("SELECT t.* FROM tags t " +
            "JOIN document_tags dt ON t.id = dt.tag_id " +
            "WHERE dt.document_id = #{documentId} AND t.user_id = #{userId}")
    List<Tag> findTagsByDocumentIdAndUserId(@Param("documentId") Long documentId, @Param("userId") Long userId);

}