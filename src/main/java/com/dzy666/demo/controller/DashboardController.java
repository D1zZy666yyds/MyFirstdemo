package com.dzy666.demo.controller;

import com.dzy666.demo.service.StatisticsService;
import com.dzy666.demo.util.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private StatisticsService statisticsService;

    /**
     * 获取仪表盘统计数据
     */
    @GetMapping("/stats")
    public JsonResult<Map<String, Object>> getDashboardStats(@RequestParam Long userId) {
        try {
            // 获取基础统计数据
            Map<String, Object> stats = statisticsService.getUserStatistics(userId);

            // 添加仪表盘特定数据
            Map<String, Object> dashboardStats = new HashMap<>();

            // 文档统计
            dashboardStats.put("totalDocuments", stats.getOrDefault("totalDocuments", 0));
            dashboardStats.put("todayDocuments", stats.getOrDefault("todayDocuments", 0));
            dashboardStats.put("weekDocuments", stats.getOrDefault("weekDocuments", 0));

            // 分类统计
            dashboardStats.put("totalCategories", stats.getOrDefault("totalCategories", 0));

            // 标签统计
            dashboardStats.put("totalTags", stats.getOrDefault("totalTags", 0));

            // 收藏统计
            dashboardStats.put("totalFavorites", stats.getOrDefault("totalFavorites", 0));

            // 最近活动
            dashboardStats.put("recentActivity", stats.getOrDefault("recentActivity", "暂无活动"));

            // 学习进度
            dashboardStats.put("learningProgress", stats.getOrDefault("learningProgress", 0));

            // 系统信息
            dashboardStats.put("lastUpdate", LocalDateTime.now());
            dashboardStats.put("systemStatus", "正常");

            return JsonResult.success("仪表盘数据获取成功", dashboardStats);
        } catch (Exception e) {
            return JsonResult.error("获取仪表盘数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取快速统计数据
     */
    @GetMapping("/quick-stats/{userId}")
    public JsonResult<Map<String, Object>> getQuickStats(@PathVariable Long userId) {
        try {
            Map<String, Object> stats = statisticsService.getUserStatistics(userId);

            Map<String, Object> quickStats = new HashMap<>();
            quickStats.put("documents", stats.getOrDefault("totalDocuments", 0));
            quickStats.put("categories", stats.getOrDefault("totalCategories", 0));
            quickStats.put("tags", stats.getOrDefault("totalTags", 0));
            quickStats.put("favorites", stats.getOrDefault("totalFavorites", 0));

            return JsonResult.success(quickStats);
        } catch (Exception e) {
            return JsonResult.error("获取快速统计失败: " + e.getMessage());
        }
    }
}