package com.dzy666.demo.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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


}