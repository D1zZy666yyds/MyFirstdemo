package com.dzy666.demo.controller;

import com.dzy666.demo.service.StatisticsService;
import com.dzy666.demo.util.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    @GetMapping("/overview/{userId}")
    public JsonResult<Map<String, Object>> getUserOverview(@PathVariable Long userId) {
        try {
            Map<String, Object> stats = statisticsService.getUserStatistics(userId);
            return JsonResult.success(stats);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    @GetMapping("/category-distribution/{userId}")
    public JsonResult<Map<String, Object>> getCategoryDistribution(@PathVariable Long userId) {
        try {
            Map<String, Object> distribution = statisticsService.getCategoryDistribution(userId);
            return JsonResult.success(distribution);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    @GetMapping("/tag-usage/{userId}")
    public JsonResult<List<Map<String, Object>>> getTagUsage(@PathVariable Long userId) {
        try {
            List<Map<String, Object>> tagStats = statisticsService.getTagUsageStatistics(userId);
            return JsonResult.success(tagStats);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    @GetMapping("/creation-trend/{userId}")
    public JsonResult<Map<String, Long>> getCreationTrend(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "6") int months) {
        try {
            Map<String, Long> trend = statisticsService.getDocumentCreationTrend(userId, months);
            return JsonResult.success(trend);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    @GetMapping("/popular-documents/{userId}")
    public JsonResult<List<Map<String, Object>>> getPopularDocuments(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<Map<String, Object>> popularDocs = statisticsService.getPopularDocuments(userId, limit);
            return JsonResult.success(popularDocs);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    /**
     * 获取用户活跃度分析
     */
    @GetMapping("/activity-analysis/{userId}")
    public JsonResult<Map<String, Object>> getActivityAnalysis(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "30") int days) {
        try {
            Map<String, Object> activity = statisticsService.getUserActivityAnalysis(userId, days);
            return JsonResult.success(activity);
        } catch (Exception e) {
            return JsonResult.error("获取活跃度分析失败: " + e.getMessage());
        }
    }

    /**
     * 获取时间分布统计
     */
    @GetMapping("/time-distribution/{userId}")
    public JsonResult<Map<String, Object>> getTimeDistribution(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "7") int days) {
        try {
            Map<String, Object> timeDistribution = statisticsService.getTimeDistribution(userId, days);
            return JsonResult.success(timeDistribution);
        } catch (Exception e) {
            return JsonResult.error("获取时间分布统计失败: " + e.getMessage());
        }
    }
}