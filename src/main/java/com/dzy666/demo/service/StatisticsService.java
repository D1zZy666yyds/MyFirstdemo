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
import java.time.temporal.ChronoUnit;
import java.util.*;

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

    // 原有方法保持不变...
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
            // 修复：添加userId参数
            tagStats.put("usageCount", tagMapper.countDocumentsByTag(tag.getId(), userId));
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

    // 🔄 新增方法 - 学习进度、活跃度分析

    /**
     * 获取学习进度统计
     */
    public Map<String, Object> getLearningProgress(Long userId) {
        Map<String, Object> progress = new HashMap<>();
        List<Document> documents = documentMapper.selectByUserId(userId);

        // 总体进度
        progress.put("totalDocuments", documents.size());

        // 按时间段的进度
        LocalDate now = LocalDate.now();
        progress.put("today", countDocumentsInPeriod(documents, now, now));
        progress.put("thisWeek", countDocumentsInPeriod(documents, now.minusDays(7), now));
        progress.put("thisMonth", countDocumentsInPeriod(documents, now.minusDays(30), now));

        // 学习连续性
        int continuousDays = calculateContinuousLearningDays(documents);
        progress.put("continuousLearningDays", continuousDays);

        // 学习目标完成度（假设目标为每月10篇文档）
        int monthlyGoal = 10;
        long thisMonthCount = countDocumentsInPeriod(documents, now.withDayOfMonth(1), now);
        double goalCompletion = Math.min(100.0, (thisMonthCount * 100.0) / monthlyGoal);
        progress.put("monthlyGoalCompletion", goalCompletion);

        // 复习进度
        long totalDocs = documents.size();
        long reviewedDocs = documents.stream()
                .filter(doc -> shouldReviewForProgress(doc.getCreatedTime()))
                .count();
        progress.put("reviewedDocuments", reviewedDocs);
        progress.put("reviewProgress", totalDocs > 0 ?
                (double) reviewedDocs / totalDocs * 100 : 0);
        progress.put("completionRate", calculateCompletionRate(userId));

        return progress;
    }

    private long countDocumentsInPeriod(List<Document> documents, LocalDate start, LocalDate end) {
        return documents.stream()
                .filter(doc -> {
                    LocalDate docDate = doc.getCreatedTime().toLocalDate();
                    return !docDate.isBefore(start) && !docDate.isAfter(end);
                })
                .count();
    }

    private int calculateContinuousLearningDays(List<Document> documents) {
        if (documents.isEmpty()) return 0;

        // 按日期分组
        Map<LocalDate, Long> documentsByDate = documents.stream()
                .collect(Collectors.groupingBy(
                        doc -> doc.getCreatedTime().toLocalDate(),
                        Collectors.counting()
                ));

        // 计算连续学习天数
        LocalDate currentDate = LocalDate.now();
        int continuousDays = 0;

        while (documentsByDate.containsKey(currentDate)) {
            continuousDays++;
            currentDate = currentDate.minusDays(1);
        }

        return continuousDays;
    }

    /**
     * 获取用户活跃度分析
     */
    public Map<String, Object> getUserActivityAnalysis(Long userId, int days) {
        Map<String, Object> activity = new HashMap<>();
        List<Document> documents = documentMapper.selectByUserId(userId);

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        // 每日活跃度
        Map<String, Long> dailyActivity = new LinkedHashMap<>();
        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            long count = countDocumentsInPeriod(documents, date, date);
            dailyActivity.put(date.toString(), count);
        }
        activity.put("dailyActivity", dailyActivity);

        // 活跃度统计
        long totalActivity = dailyActivity.values().stream().mapToLong(Long::longValue).sum();
        long activeDays = dailyActivity.values().stream().filter(count -> count > 0).count();

        activity.put("totalActivity", totalActivity);
        activity.put("activeDays", activeDays);
        activity.put("activityRate", (double) activeDays / days * 100);

        // 活跃度等级
        String activityLevel;
        double avgDailyActivity = (double) totalActivity / days;
        if (avgDailyActivity >= 2) activityLevel = "非常活跃";
        else if (avgDailyActivity >= 1) activityLevel = "活跃";
        else if (avgDailyActivity >= 0.5) activityLevel = "一般";
        else activityLevel = "不活跃";

        activity.put("activityLevel", activityLevel);
        activity.put("averageDailyActivity", avgDailyActivity);

        // 活动趋势
        activity.put("activityTrend", getActivityTrend(dailyActivity));

        return activity;
    }

    private Map<String, Integer> getActivityTrend(Map<String, Long> dailyActivity) {
        Map<String, Integer> trend = new HashMap<>();
        List<Long> values = new ArrayList<>(dailyActivity.values());

        for (int i = 0; i < values.size(); i++) {
            trend.put("Day" + (i + 1), values.get(i).intValue());
        }
        return trend;
    }

    /**
     * 获取知识覆盖度统计
     */
    public Map<String, Object> getKnowledgeCoverage(Long userId) {
        Map<String, Object> coverage = new HashMap<>();
        List<Category> categories = categoryMapper.selectByUserId(userId);
        List<Document> documents = documentMapper.selectByUserId(userId);

        // 分类覆盖度
        int categoriesWithDocuments = (int) categories.stream()
                .filter(category ->
                        documents.stream().anyMatch(doc ->
                                category.getId().equals(doc.getCategoryId())
                        )
                )
                .count();

        double categoryCoverage = categories.isEmpty() ? 0 :
                (double) categoriesWithDocuments / categories.size() * 100;
        coverage.put("categoryCoverage", categoryCoverage);

        // 标签覆盖度
        List<Tag> tags = tagMapper.selectByUserId(userId);
        int tagsWithDocuments = (int) tags.stream()
                // 修复：添加userId参数
                .filter(tag -> tagMapper.countDocumentsByTag(tag.getId(), userId) > 0)
                .count();

        double tagCoverage = tags.isEmpty() ? 0 :
                (double) tagsWithDocuments / tags.size() * 100;
        coverage.put("tagCoverage", tagCoverage);

        // 总体知识密度（平均每个分类的文档数）
        double knowledgeDensity = categories.isEmpty() ? 0 :
                (double) documents.size() / categories.size();
        coverage.put("knowledgeDensity", knowledgeDensity);

        return coverage;
    }

    /**
     * 获取学习效率统计
     */
    public Map<String, Object> getLearningEfficiency(Long userId, int days) {
        Map<String, Object> efficiency = new HashMap<>();
        List<Document> documents = documentMapper.selectByUserId(userId);

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        // 计算时间段内的文档
        List<Document> recentDocuments = documents.stream()
                .filter(doc -> {
                    LocalDate docDate = doc.getCreatedTime().toLocalDate();
                    return !docDate.isBefore(startDate) && !docDate.isAfter(endDate);
                })
                .collect(Collectors.toList());

        // 学习效率指标
        efficiency.put("documentsCreated", recentDocuments.size());
        efficiency.put("averageDocumentsPerDay",
                recentDocuments.isEmpty() ? 0 : (double) recentDocuments.size() / days);

        // 内容质量指标（基于文档长度）
        double avgContentLength = recentDocuments.stream()
                .mapToInt(doc -> doc.getContent() != null ? doc.getContent().length() : 0)
                .average()
                .orElse(0);
        efficiency.put("averageContentLength", avgContentLength);

        // 分类使用效率
        long categoriesUsed = recentDocuments.stream()
                .map(Document::getCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        efficiency.put("categoriesUsed", categoriesUsed);

        return efficiency;
    }

    /**
     * 获取复习提醒统计
     */
    public List<Map<String, Object>> getReviewReminders(Long userId) {
        List<Document> documents = documentMapper.selectByUserId(userId);
        List<Map<String, Object>> reminders = new ArrayList<>();

        LocalDate now = LocalDate.now();

        for (Document doc : documents) {
            LocalDate createdDate = doc.getCreatedTime().toLocalDate();
            long daysSinceCreation = ChronoUnit.DAYS.between(createdDate, now);

            // 基于艾宾浩斯遗忘曲线的复习提醒
            if (shouldReview(daysSinceCreation)) {
                Map<String, Object> reminder = new HashMap<>();
                reminder.put("documentId", doc.getId());
                reminder.put("title", doc.getTitle());
                reminder.put("daysSinceCreation", daysSinceCreation);
                reminder.put("reviewStage", getReviewStage(daysSinceCreation));
                reminders.add(reminder);
            }
        }

        return reminders.stream()
                .sorted((a, b) -> Long.compare(
                        (Long) a.get("daysSinceCreation"),
                        (Long) b.get("daysSinceCreation")
                ))
                .collect(Collectors.toList());
    }

    private boolean shouldReview(long daysSinceCreation) {
        // 艾宾浩斯复习时间点：1天、2天、4天、7天、15天、30天、60天、90天...
        long[] reviewDays = {1, 2, 4, 7, 15, 30, 60, 90};
        return Arrays.stream(reviewDays).anyMatch(day -> day == daysSinceCreation);
    }

    private boolean shouldReviewForProgress(LocalDateTime createdTime) {
        long daysSinceCreation = java.time.Duration.between(createdTime, LocalDateTime.now()).toDays();
        return shouldReview(daysSinceCreation);
    }

    private String getReviewStage(long daysSinceCreation) {
        switch ((int) daysSinceCreation) {
            case 1: return "第一次复习";
            case 2: return "第二次复习";
            case 4: return "第三次复习";
            case 7: return "第四次复习";
            case 15: return "第五次复习";
            case 30: return "第六次复习";
            case 60: return "第七次复习";
            case 90: return "第八次复习";
            default: return "定期复习";
        }
    }

    /**
     * 获取学习目标完成情况
     */
    public Map<String, Object> getLearningGoalsProgress(Long userId) {
        Map<String, Object> goalsProgress = new HashMap<>();
        List<Document> documents = documentMapper.selectByUserId(userId);

        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);

        // 月度目标
        long monthlyDocuments = countDocumentsInPeriod(documents, monthStart, now);
        goalsProgress.put("monthlyDocuments", monthlyDocuments);
        goalsProgress.put("monthlyGoal", 10); // 假设月度目标为10篇
        goalsProgress.put("monthlyProgress", Math.min(100, (monthlyDocuments * 100) / 10));

        // 年度目标
        LocalDate yearStart = now.withDayOfYear(1);
        long yearlyDocuments = countDocumentsInPeriod(documents, yearStart, now);
        goalsProgress.put("yearlyDocuments", yearlyDocuments);
        goalsProgress.put("yearlyGoal", 100); // 假设年度目标为100篇
        goalsProgress.put("yearlyProgress", Math.min(100, (yearlyDocuments * 100) / 100));

        // 分类覆盖目标
        List<Category> categories = categoryMapper.selectByUserId(userId);
        long categoriesWithDocuments = categories.stream()
                .filter(category ->
                        documents.stream().anyMatch(doc ->
                                category.getId().equals(doc.getCategoryId())
                        )
                )
                .count();
        goalsProgress.put("categoriesCovered", categoriesWithDocuments);
        goalsProgress.put("totalCategories", categories.size());
        goalsProgress.put("categoryCoverageProgress",
                categories.isEmpty() ? 0 : (categoriesWithDocuments * 100) / categories.size());

        return goalsProgress;
    }

    /**
     * 获取时间分布统计
     */
    public Map<String, Object> getTimeDistribution(Long userId, int days) {
        Map<String, Object> timeDistribution = new HashMap<>();
        List<Document> documents = documentMapper.selectByUserId(userId);

        // 按小时分布
        Map<String, Long> hourlyDistribution = new HashMap<>();
        for (int hour = 0; hour < 24; hour++) {
            final int currentHour = hour;
            long count = documents.stream()
                    .filter(doc -> doc.getCreatedTime().getHour() == currentHour)
                    .count();
            hourlyDistribution.put(String.format("%02d:00", hour), count);
        }
        timeDistribution.put("hourlyDistribution", hourlyDistribution);

        // 按星期分布
        Map<String, Long> weeklyDistribution = new HashMap<>();
        String[] weekDays = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        for (int i = 0; i < 7; i++) {
            final int dayOfWeek = i + 1; // Monday = 1 in Java
            long count = documents.stream()
                    .filter(doc -> doc.getCreatedTime().getDayOfWeek().getValue() == dayOfWeek)
                    .count();
            weeklyDistribution.put(weekDays[i], count);
        }
        timeDistribution.put("weeklyDistribution", weeklyDistribution);

        return timeDistribution;
    }

    /**
     * 获取用户成就统计
     */
    public Map<String, Object> getUserAchievements(Long userId) {
        Map<String, Object> achievements = new HashMap<>();
        List<Document> documents = documentMapper.selectByUserId(userId);
        List<Category> categories = categoryMapper.selectByUserId(userId);
        List<Tag> tags = tagMapper.selectByUserId(userId);

        // 成就列表
        List<Map<String, Object>> achievementList = new ArrayList<>();

        // 文档数量成就
        addAchievement(achievementList, "初出茅庐", "创建第一篇文档", documents.size() >= 1, documents.size() >= 1 ? 100 : 0);
        addAchievement(achievementList, "知识积累者", "创建10篇文档", documents.size() >= 10, Math.min(documents.size() * 10, 100));
        addAchievement(achievementList, "知识大师", "创建50篇文档", documents.size() >= 50, Math.min(documents.size() * 2, 100));

        // 分类成就
        addAchievement(achievementList, "分类专家", "创建5个分类", categories.size() >= 5, Math.min(categories.size() * 20, 100));

        // 标签成就
        addAchievement(achievementList, "标签达人", "使用10个标签", tags.size() >= 10, Math.min(tags.size() * 10, 100));

        // 连续学习成就
        int continuousDays = calculateContinuousLearningDays(documents);
        addAchievement(achievementList, "学习习惯", "连续学习7天", continuousDays >= 7, Math.min(continuousDays * 100 / 7, 100));

        achievements.put("achievements", achievementList);
        achievements.put("totalAchievements", achievementList.size());
        achievements.put("completedAchievements",
                achievementList.stream().filter(a -> (Boolean) a.get("completed")).count());

        return achievements;
    }

    private void addAchievement(List<Map<String, Object>> achievements, String name,
                                String description, boolean completed, int progress) {
        Map<String, Object> achievement = new HashMap<>();
        achievement.put("name", name);
        achievement.put("description", description);
        achievement.put("completed", completed);
        achievement.put("progress", progress);
        achievements.add(achievement);
    }

    /**
     * 计算学习完成率
     */
    private double calculateCompletionRate(Long userId) {
        List<Document> documents = documentMapper.selectByUserId(userId);
        List<Category> categories = categoryMapper.selectByUserId(userId);

        if (documents.isEmpty() || categories.isEmpty()) {
            return 0.0;
        }

        // 计算分类覆盖率和文档数量综合完成率
        long categoriesWithDocuments = categories.stream()
                .filter(category -> documents.stream()
                        .anyMatch(doc -> category.getId().equals(doc.getCategoryId())))
                .count();

        double categoryCoverage = (double) categoriesWithDocuments / categories.size() * 100;
        double documentProgress = Math.min(documents.size() / 50.0 * 100, 100); // 假设50篇为完成目标

        return (categoryCoverage + documentProgress) / 2;
    }

    /**
     * 获取对比统计（与平均数据对比）
     */
    public Map<String, Object> getComparisonStatistics(Long userId) {
        Map<String, Object> comparison = new HashMap<>();
        List<Document> documents = documentMapper.selectByUserId(userId);
        List<Category> categories = categoryMapper.selectByUserId(userId);

        // 假设的平均数据（在实际应用中，这些数据应该来自数据库统计）
        double avgDocumentsPerUser = 25.0;
        double avgCategoriesPerUser = 5.0;
        double avgTagsPerUser = 8.0;

        // 用户当前数据
        double userDocuments = documents.size();
        double userCategories = categories.size();
        double userTags = tagMapper.selectByUserId(userId).size();

        // 对比计算
        comparison.put("documentsComparison",
                userDocuments > 0 ? (userDocuments - avgDocumentsPerUser) / avgDocumentsPerUser * 100 : -100);
        comparison.put("categoriesComparison",
                userCategories > 0 ? (userCategories - avgCategoriesPerUser) / avgCategoriesPerUser * 100 : -100);
        comparison.put("tagsComparison",
                userTags > 0 ? (userTags - avgTagsPerUser) / avgTagsPerUser * 100 : -100);

        comparison.put("userDocuments", userDocuments);
        comparison.put("averageDocuments", avgDocumentsPerUser);
        comparison.put("userCategories", userCategories);
        comparison.put("averageCategories", avgCategoriesPerUser);
        comparison.put("userTags", userTags);
        comparison.put("averageTags", avgTagsPerUser);

        return comparison;
    }
}