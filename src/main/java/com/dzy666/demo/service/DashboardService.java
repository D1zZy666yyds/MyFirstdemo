package com.dzy666.demo.service;

import com.dzy666.demo.mapper.DashboardMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardMapper dashboardMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 获取完整的仪表盘统计数据
     */
    public Map<String, Object> getDashboardStats(Long userId) {
        Map<String, Object> stats = new LinkedHashMap<>();

        // 基础统计数据
        stats.put("totalDocuments", dashboardMapper.countDocumentsByUserId(userId));
        stats.put("todayDocuments", dashboardMapper.countTodayDocuments(userId));
        stats.put("weekDocuments", dashboardMapper.countWeekDocuments(userId));
        stats.put("totalCategories", dashboardMapper.countCategoriesByUserId(userId));
        stats.put("totalTags", dashboardMapper.countTagsByUserId(userId));
        stats.put("totalFavorites", dashboardMapper.countFavoritesByUserId(userId));

        // 最近活动
        Map<String, Object> recentActivity = dashboardMapper.getRecentActivity(userId);
        stats.put("recentActivity", recentActivity != null ? recentActivity : new HashMap<>());

        // 文档趋势（最近7天）
        List<Map<String, Object>> documentTrend = dashboardMapper.getDocumentTrend(userId);
        stats.put("documentTrend", formatTrendData(documentTrend));

        // 分类分布
        List<Map<String, Object>> categoryDistribution = dashboardMapper.getCategoryDocumentDistribution(userId);
        stats.put("categoryDistribution", formatCategoryDistribution(categoryDistribution));

        // 热门标签
        List<Map<String, Object>> popularTags = dashboardMapper.getPopularTags(userId);
        stats.put("popularTags", formatPopularTags(popularTags));

        // 学习统计
        Map<String, Object> learningStats = dashboardMapper.getLearningStatistics(userId);
        stats.put("learningStats", learningStats != null ? learningStats : new HashMap<>());

        // 最近文档（最多5篇）
        List<Map<String, Object>> recentDocuments = dashboardMapper.getRecentDocuments(userId, 5);
        stats.put("recentDocuments", formatRecentDocuments(recentDocuments));

        // 用户活跃统计
        Map<String, Object> userActivity = dashboardMapper.getUserActivityStats(userId);
        stats.put("userActivity", userActivity != null ? userActivity : new HashMap<>());

        // 文档大小统计
        Map<String, Object> sizeStats = dashboardMapper.getDocumentSizeStats(userId);
        stats.put("sizeStats", sizeStats != null ? sizeStats : new HashMap<>());

        // 最近操作
        List<Map<String, Object>> recentOperations = dashboardMapper.getRecentOperations(userId);
        stats.put("recentOperations", formatRecentOperations(recentOperations));

        // 时间信息
        stats.put("currentDate", LocalDate.now().format(DATE_FORMATTER));
        stats.put("currentTime", LocalDateTime.now().toString());
        stats.put("serverTime", System.currentTimeMillis());

        return stats;
    }

    /**
     * 获取快速统计数据
     */
    public Map<String, Object> getQuickStats(Long userId) {
        Map<String, Object> quickStats = new HashMap<>();

        quickStats.put("totalDocuments", dashboardMapper.countDocumentsByUserId(userId));
        quickStats.put("todayDocuments", dashboardMapper.countTodayDocuments(userId));
        quickStats.put("totalCategories", dashboardMapper.countCategoriesByUserId(userId));
        quickStats.put("totalTags", dashboardMapper.countTagsByUserId(userId));
        quickStats.put("totalFavorites", dashboardMapper.countFavoritesByUserId(userId));

        return quickStats;
    }

    /**
     * 获取文档创建趋势（按日期范围）
     */
    public Map<String, Object> getDocumentTrendByRange(Long userId, String startDate, String endDate) {
        Map<String, Object> result = new HashMap<>();

        result.put("userId", userId);
        result.put("startDate", startDate);
        result.put("endDate", endDate);

        // 这里可以扩展实现按日期范围查询趋势
        // 暂时返回最近7天趋势
        List<Map<String, Object>> trendData = dashboardMapper.getDocumentTrend(userId);
        result.put("trendData", formatTrendData(trendData));

        // 计算统计信息
        int totalDocs = 0;
        List<String> dates = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();

        for (Map<String, Object> item : trendData) {
            totalDocs += Integer.parseInt(item.get("count").toString());
            dates.add(item.get("date").toString());
            counts.add(Integer.parseInt(item.get("count").toString()));
        }

        result.put("totalDocsInPeriod", totalDocs);
        result.put("avgDocsPerDay", trendData.isEmpty() ? 0 : totalDocs / trendData.size());
        result.put("maxDocsPerDay", counts.isEmpty() ? 0 : Collections.max(counts));
        result.put("dates", dates);
        result.put("counts", counts);

        return result;
    }

    // ============= 数据格式化方法 =============

    private Map<String, Object> formatTrendData(List<Map<String, Object>> trendData) {
        Map<String, Object> formatted = new HashMap<>();

        List<String> dates = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();

        if (trendData != null && !trendData.isEmpty()) {
            for (Map<String, Object> item : trendData) {
                dates.add(item.get("date").toString());
                counts.add(Integer.parseInt(item.get("count").toString()));
            }
        }

        formatted.put("dates", dates);
        formatted.put("counts", counts);
        formatted.put("total", counts.stream().mapToInt(Integer::intValue).sum());

        return formatted;
    }

    private List<Map<String, Object>> formatCategoryDistribution(List<Map<String, Object>> distribution) {
        if (distribution == null || distribution.isEmpty()) {
            return new ArrayList<>();
        }

        // 计算总数
        int total = distribution.stream()
                .mapToInt(item -> Integer.parseInt(item.get("documentCount").toString()))
                .sum();

        // 添加百分比
        distribution.forEach(item -> {
            int count = Integer.parseInt(item.get("documentCount").toString());
            double percentage = total > 0 ? (count * 100.0) / total : 0;
            item.put("percentage", String.format("%.1f", percentage));
        });

        return distribution;
    }

    private List<Map<String, Object>> formatPopularTags(List<Map<String, Object>> tags) {
        if (tags == null || tags.isEmpty()) {
            return new ArrayList<>();
        }

        // 排序并添加排名
        tags.sort((a, b) -> {
            int countA = Integer.parseInt(a.get("usageCount").toString());
            int countB = Integer.parseInt(b.get("usageCount").toString());
            return Integer.compare(countB, countA);
        });

        for (int i = 0; i < tags.size(); i++) {
            tags.get(i).put("rank", i + 1);
        }

        return tags;
    }

    private List<Map<String, Object>> formatRecentDocuments(List<Map<String, Object>> documents) {
        if (documents == null || documents.isEmpty()) {
            return new ArrayList<>();
        }

        documents.forEach(doc -> {
            // 格式化日期
            if (doc.get("createdTime") != null) {
                try {
                    LocalDateTime dateTime = (LocalDateTime) doc.get("createdTime");
                    doc.put("formattedDate", dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                    doc.put("relativeTime", formatRelativeTime(dateTime));
                } catch (Exception e) {
                    doc.put("formattedDate", doc.get("createdTime").toString());
                    doc.put("relativeTime", "未知时间");
                }
            }

            // 截取内容预览
            if (doc.get("contentPreview") != null) {
                String preview = doc.get("contentPreview").toString();
                if (preview.length() > 100) {
                    doc.put("contentPreview", preview.substring(0, 100) + "...");
                }
            }
        });

        return documents;
    }

    private List<Map<String, Object>> formatRecentOperations(List<Map<String, Object>> operations) {
        if (operations == null || operations.isEmpty()) {
            return new ArrayList<>();
        }

        operations.forEach(op -> {
            // 格式化操作时间
            if (op.get("createdTime") != null) {
                try {
                    LocalDateTime dateTime = (LocalDateTime) op.get("createdTime");
                    op.put("formattedTime", dateTime.format(DateTimeFormatter.ofPattern("MM-dd HH:mm")));
                    op.put("relativeTime", formatRelativeTime(dateTime));
                } catch (Exception e) {
                    op.put("formattedTime", op.get("createdTime").toString());
                }
            }

            // 添加操作图标
            String operationType = op.get("operationType") != null ?
                    op.get("operationType").toString() : "";
            op.put("icon", getOperationIcon(operationType));
        });

        return operations;
    }

    private String formatRelativeTime(LocalDateTime dateTime) {
        long hours = java.time.Duration.between(dateTime, LocalDateTime.now()).toHours();

        if (hours < 1) {
            return "刚刚";
        } else if (hours < 24) {
            return hours + "小时前";
        } else {
            long days = hours / 24;
            return days + "天前";
        }
    }

    private String getOperationIcon(String operationType) {
        switch (operationType.toUpperCase()) {
            case "CREATE": return "📝";
            case "UPDATE": return "✏️";
            case "DELETE": return "🗑️";
            case "LOGIN": return "🔐";
            case "LOGOUT": return "🚪";
            case "VIEW": return "👁️";
            case "SEARCH": return "🔍";
            default: return "📋";
        }
    }
}