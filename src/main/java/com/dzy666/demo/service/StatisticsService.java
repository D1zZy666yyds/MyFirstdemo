package com.dzy666.demo.service;
import com.dzy666.demo.entity.Document;
import com.dzy666.demo.entity.Category;
import com.dzy666.demo.entity.Tag;
import java.util.stream.Collectors;
import com.dzy666.demo.mapper.DocumentMapper;
import com.dzy666.demo.mapper.CategoryMapper;
import com.dzy666.demo.mapper.TagMapper;
import com.dzy666.demo.mapper.FavoriteMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatisticsService {

    @Autowired
    private DocumentMapper documentMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private FavoriteMapper favoriteMapper;

    /**
     * 获取用户总体统计信息
     */
    public Map<String, Object> getUserStatistics(Long userId) {
        Map<String, Object> stats = new HashMap<>();

        // 文档统计
        List<Document> documents = documentMapper.selectByUserId(userId);
        stats.put("totalDocuments", documents.size());

        // 分类统计
        List<Category> categories = categoryMapper.selectByUserId(userId);
        stats.put("totalCategories", categories.size());

        // 标签统计
        List<Tag> tags = tagMapper.selectByUserId(userId);
        stats.put("totalTags", tags.size());

        // 收藏统计
        List<Long> favorites = favoriteMapper.selectFavoriteDocumentIds(userId);
        stats.put("totalFavorites", favorites.size());

        // 最近活跃度（最近7天创建的文档）
        LocalDate weekAgo = LocalDate.now().minusDays(7);
        long recentDocuments = documents.stream()
                .filter(doc -> doc.getCreatedTime().toLocalDate().isAfter(weekAgo))
                .count();
        stats.put("recentDocuments", recentDocuments);

        return stats;
    }

    /**
     * 获取分类分布统计
     */
    public Map<String, Object> getCategoryDistribution(Long userId) {
        Map<String, Object> distribution = new HashMap<>();

        List<Category> categories = categoryMapper.selectByUserId(userId);
        for (Category category : categories) {
            int docCount = documentMapper.selectByCategoryIdAndUser(category.getId(), userId).size();
            distribution.put(category.getName(), docCount);
        }

        // 未分类的文档
        List<Document> uncategorized = documentMapper.selectByUserId(userId).stream()
                .filter(doc -> doc.getCategoryId() == null)
                .collect(Collectors.toList());
        distribution.put("未分类", uncategorized.size());

        return distribution;
    }

    /**
     * 获取标签使用统计
     */
    public List<Map<String, Object>> getTagUsageStatistics(Long userId) {
        List<Tag> tags = tagMapper.selectByUserId(userId);

        return tags.stream().map(tag -> {
            Map<String, Object> tagStats = new HashMap<>();
            tagStats.put("name", tag.getName());
            tagStats.put("usageCount", tagMapper.countDocumentsByTag(tag.getId()));
            return tagStats;
        }).collect(Collectors.toList());
    }

    /**
     * 获取文档创建趋势（按月份）
     */
    public Map<String, Long> getDocumentCreationTrend(Long userId, int months) {
        Map<String, Long> trend = new HashMap<>();
        List<Document> documents = documentMapper.selectByUserId(userId);

        LocalDate now = LocalDate.now();
        for (int i = months - 1; i >= 0; i--) {
            LocalDate monthStart = now.minusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

            String monthKey = monthStart.getMonthValue() + "月";
            long count = documents.stream()
                    .filter(doc -> {
                        LocalDate docDate = doc.getCreatedTime().toLocalDate();
                        return !docDate.isBefore(monthStart) && !docDate.isAfter(monthEnd);
                    })
                    .count();
            trend.put(monthKey, count);
        }

        return trend;
    }

    /**
     * 获取热门文档（按收藏数）
     */
    public List<Map<String, Object>> getPopularDocuments(Long userId, int limit) {
        List<Document> documents = documentMapper.selectByUserId(userId);

        return documents.stream()
                .map(doc -> {
                    Map<String, Object> docStats = new HashMap<>();
                    docStats.put("id", doc.getId());
                    docStats.put("title", doc.getTitle());
                    docStats.put("favoriteCount", favoriteMapper.countByDocumentId(doc.getId()));
                    docStats.put("createdTime", doc.getCreatedTime());
                    return docStats;
                })
                .sorted((a, b) -> Integer.compare(
                        (Integer) b.get("favoriteCount"),
                        (Integer) a.get("favoriteCount")
                ))
                .limit(limit)
                .collect(Collectors.toList());
    }
}