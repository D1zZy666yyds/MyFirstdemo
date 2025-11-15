package com.dzy666.demo.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface FavoriteMapper {

    @Insert("INSERT INTO favorites(document_id, user_id, created_time) VALUES(#{documentId}, #{userId}, NOW())")
    int insert(@Param("documentId") Long documentId, @Param("userId") Long userId);

    @Delete("DELETE FROM favorites WHERE document_id = #{documentId} AND user_id = #{userId}")
    int delete(@Param("documentId") Long documentId, @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM favorites WHERE document_id = #{documentId} AND user_id = #{userId}")
    int exists(@Param("documentId") Long documentId, @Param("userId") Long userId);

    @Select("SELECT document_id FROM favorites WHERE user_id = #{userId} ORDER BY created_time DESC")
    List<Long> selectFavoriteDocumentIds(Long userId);

    @Select("SELECT COUNT(*) FROM favorites WHERE document_id = #{documentId}")
    int countByDocumentId(Long documentId);

    // 添加缺失的方法
    @Select("SELECT COUNT(*) FROM favorites WHERE user_id = #{userId}")
    int countByUserId(Long userId);
}