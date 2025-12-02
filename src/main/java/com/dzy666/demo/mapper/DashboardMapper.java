package com.dzy666.demo.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface DashboardMapper {

    /**
     * 获取用户文档总数
     */
    @Select("SELECT COUNT(*) FROM documents WHERE user_id = #{userId} AND deleted = 0")
    int countDocumentsByUserId(@Param("userId") Long userId);

    /**
     * 获取今日新增文档数
     */
    @Select("SELECT COUNT(*) FROM documents WHERE user_id = #{userId} AND DATE(created_time) = CURDATE() AND deleted = 0")
    int countTodayDocuments(@Param("userId") Long userId);

    /**
     * 获取本周新增文档数
     */
    @Select("SELECT COUNT(*) FROM documents WHERE user_id = #{userId} AND YEARWEEK(created_time) = YEARWEEK(NOW()) AND deleted = 0")
    int countWeekDocuments(@Param("userId") Long userId);

    /**
     * 获取用户分类总数
     */
    @Select("SELECT COUNT(*) FROM categories WHERE user_id = #{userId}")
    int countCategoriesByUserId(@Param("userId") Long userId);

    /**
     * 获取用户标签总数
     */
    @Select("SELECT COUNT(*) FROM tags WHERE user_id = #{userId}")
    int countTagsByUserId(@Param("userId") Long userId);

    /**
     * 获取用户收藏总数
     */
    @Select("SELECT COUNT(*) FROM favorites WHERE user_id = #{userId}")
    int countFavoritesByUserId(@Param("userId") Long userId);

    /**
     * 获取最近活动（最新创建的文档）
     */
    @Select("SELECT title, created_time as createdTime FROM documents WHERE user_id = #{userId} AND deleted = 0 ORDER BY created_time DESC LIMIT 1")
    Map<String, Object> getRecentActivity(@Param("userId") Long userId);

    /**
     * 获取文档创建趋势（最近7天）
     */
    @Select("SELECT DATE(created_time) as date, COUNT(*) as count " +
            "FROM documents " +
            "WHERE user_id = #{userId} AND created_time >= DATE_SUB(NOW(), INTERVAL 7 DAY) AND deleted = 0 " +
            "GROUP BY DATE(created_time) " +
            "ORDER BY date")
    List<Map<String, Object>> getDocumentTrend(@Param("userId") Long userId);

    /**
     * 获取分类文档分布
     */
    @Select("SELECT c.name as categoryName, COUNT(d.id) as documentCount " +
            "FROM categories c " +
            "LEFT JOIN documents d ON c.id = d.category_id AND d.deleted = 0 " +
            "WHERE c.user_id = #{userId} " +
            "GROUP BY c.id, c.name " +
            "ORDER BY documentCount DESC")
    List<Map<String, Object>> getCategoryDocumentDistribution(@Param("userId") Long userId);

    /**
     * 获取热门标签（使用次数最多的标签）
     */
    @Select("SELECT t.name as tagName, COUNT(dt.document_id) as usageCount " +
            "FROM tags t " +
            "LEFT JOIN document_tags dt ON t.id = dt.tag_id " +
            "LEFT JOIN documents d ON dt.document_id = d.id AND d.deleted = 0 " +
            "WHERE t.user_id = #{userId} " +
            "GROUP BY t.id, t.name " +
            "ORDER BY usageCount DESC " +
            "LIMIT 10")
    List<Map<String, Object>> getPopularTags(@Param("userId") Long userId);

    /**
     * 获取用户学习统计
     */
    @Select("SELECT " +
            "COUNT(DISTINCT DATE(created_time)) as activeDays, " +
            "AVG(LENGTH(content)) as avgDocumentLength, " +
            "MAX(created_time) as lastStudyTime " +
            "FROM documents " +
            "WHERE user_id = #{userId} AND deleted = 0 AND created_time >= DATE_SUB(NOW(), INTERVAL 30 DAY)")
    Map<String, Object> getLearningStatistics(@Param("userId") Long userId);
}