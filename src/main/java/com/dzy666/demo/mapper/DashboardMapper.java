package com.dzy666.demo.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
    @Select("SELECT COUNT(*) FROM documents WHERE user_id = #{userId} " +
            "AND DATE(created_time) = CURDATE() AND deleted = 0")
    int countTodayDocuments(@Param("userId") Long userId);

    /**
     * 获取本周新增文档数
     */
    @Select("SELECT COUNT(*) FROM documents WHERE user_id = #{userId} " +
            "AND YEARWEEK(created_time) = YEARWEEK(NOW()) AND deleted = 0")
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
    @Select("SELECT title, created_time as createdTime " +
            "FROM documents WHERE user_id = #{userId} AND deleted = 0 " +
            "ORDER BY created_time DESC LIMIT 1")
    Map<String, Object> getRecentActivity(@Param("userId") Long userId);

    /**
     * 获取文档创建趋势（最近7天）
     */
    @Select("SELECT DATE(created_time) as date, COUNT(*) as count " +
            "FROM documents " +
            "WHERE user_id = #{userId} AND created_time >= DATE_SUB(NOW(), INTERVAL 7 DAY) " +
            "AND deleted = 0 " +
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
     * 获取用户学习统计 - 根据您的数据库调整
     */
    @Select("SELECT " +
            "COUNT(DISTINCT DATE(created_time)) as activeDays, " +
            "AVG(LENGTH(content)) as avgDocumentLength, " +
            "MAX(created_time) as lastStudyTime " +
            "FROM documents " +
            "WHERE user_id = #{userId} AND deleted = 0 AND created_time >= DATE_SUB(NOW(), INTERVAL 30 DAY)")
    Map<String, Object> getLearningStatistics(@Param("userId") Long userId);

    // ============= 新增方法，利用您的丰富数据库 =============

    /**
     * 获取最近创建的文档（带更多信息）
     */
    @Select("SELECT id, title, content_preview as contentPreview, " +
            "created_time as createdTime, category_id as categoryId " +
            "FROM documents " +
            "WHERE user_id = #{userId} AND deleted = 0 " +
            "ORDER BY created_time DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> getRecentDocuments(@Param("userId") Long userId,
                                                 @Param("limit") int limit);

    /**
     * 获取用户活跃统计
     */
    @Select("SELECT " +
            "(SELECT COUNT(*) FROM documents WHERE user_id = #{userId} AND deleted = 0) as totalDocs, " +
            "(SELECT COUNT(DISTINCT DATE(created_time)) FROM documents WHERE user_id = #{userId} " +
            "AND deleted = 0 AND created_time >= DATE_SUB(NOW(), INTERVAL 30 DAY)) as activeDays30, " +
            "(SELECT COUNT(*) FROM favorites WHERE user_id = #{userId}) as totalFavs, " +
            "(SELECT MAX(created_time) FROM documents WHERE user_id = #{userId} AND deleted = 0) as lastCreated")
    Map<String, Object> getUserActivityStats(@Param("userId") Long userId);

    /**
     * 获取文档大小统计
     */
    @Select("SELECT " +
            "MIN(LENGTH(content)) as minSize, " +
            "MAX(LENGTH(content)) as maxSize, " +
            "AVG(LENGTH(content)) as avgSize, " +
            "SUM(LENGTH(content)) as totalSize " +
            "FROM documents " +
            "WHERE user_id = #{userId} AND deleted = 0")
    Map<String, Object> getDocumentSizeStats(@Param("userId") Long userId);

    /**
     * 获取最近操作日志
     */
    @Select("SELECT operation_type as operationType, target_type as targetType, " +
            "description, created_time as createdTime " +
            "FROM operation_logs " +
            "WHERE user_id = #{userId} " +
            "ORDER BY created_time DESC " +
            "LIMIT 10")
    List<Map<String, Object>> getRecentOperations(@Param("userId") Long userId);
}