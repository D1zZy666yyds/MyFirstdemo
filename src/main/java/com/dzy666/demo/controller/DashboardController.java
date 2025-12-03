package com.dzy666.demo.controller;

import com.dzy666.demo.service.DashboardService;
import com.dzy666.demo.service.StatisticsService;
import com.dzy666.demo.util.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private StatisticsService statisticsService;

    @Autowired
    private DashboardService dashboardService;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * 获取仪表盘统计数据
     */
    @GetMapping("/stats")
    public JsonResult<Map<String, Object>> getDashboardStats(@RequestParam Long userId) {
        try {
            // 验证用户ID
            if (userId == null || userId <= 0) {
                return JsonResult.error("无效的用户ID");
            }

            // 使用DashboardService获取统计数据
            Map<String, Object> stats = dashboardService.getDashboardStats(userId);

            if (stats == null) {
                stats = new HashMap<>();
            }

            // 创建返回的数据结构
            Map<String, Object> dashboardStats = new HashMap<>();

            // 文档统计 - 确保值不为null
            dashboardStats.put("totalDocuments", getSafeInteger(stats.get("totalDocuments")));
            dashboardStats.put("todayDocuments", getSafeInteger(stats.get("todayDocuments")));
            dashboardStats.put("weekDocuments", getSafeInteger(stats.get("weekDocuments")));

            // 分类统计
            dashboardStats.put("totalCategories", getSafeInteger(stats.get("totalCategories")));

            // 标签统计
            dashboardStats.put("totalTags", getSafeInteger(stats.get("totalTags")));

            // 收藏统计
            dashboardStats.put("totalFavorites", getSafeInteger(stats.get("totalFavorites")));

            // 最近活动
            dashboardStats.put("recentActivity", getSafeString(stats.get("recentActivity"), "暂无活动"));

            // 学习进度
            dashboardStats.put("learningProgress", getSafeDouble(stats.get("learningProgress")));

            // 系统信息 - 确保时间格式正确
            Object lastUpdateObj = stats.get("lastUpdate");
            if (lastUpdateObj instanceof LocalDateTime) {
                dashboardStats.put("lastUpdate", ((LocalDateTime) lastUpdateObj).format(DATE_TIME_FORMATTER));
            } else if (lastUpdateObj instanceof String) {
                dashboardStats.put("lastUpdate", lastUpdateObj);
            } else {
                dashboardStats.put("lastUpdate", LocalDateTime.now().format(DATE_TIME_FORMATTER));
            }

            dashboardStats.put("systemStatus", getSafeString(stats.get("systemStatus"), "正常"));

            // 添加额外信息
            dashboardStats.put("activeDays", getSafeInteger(stats.get("activeDays")));
            dashboardStats.put("dataTimestamp", System.currentTimeMillis());

            return JsonResult.success("仪表盘数据获取成功", dashboardStats);
        } catch (Exception e) {
            // 记录详细错误日志
            e.printStackTrace();
            return JsonResult.error("获取仪表盘数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取快速统计数据
     */
    @GetMapping("/quick-stats/{userId}")
    public JsonResult<Map<String, Object>> getQuickStats(@PathVariable Long userId) {
        try {
            // 验证用户ID
            if (userId == null || userId <= 0) {
                return JsonResult.error("无效的用户ID");
            }

            Map<String, Object> stats = dashboardService.getQuickStats(userId);

            // 确保返回的map不为null
            if (stats == null) {
                stats = new HashMap<>();
            }

            return JsonResult.success("快速统计获取成功", stats);
        } catch (Exception e) {
            e.printStackTrace();
            return JsonResult.error("获取快速统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取图表数据
     */
    @GetMapping("/charts")
    public JsonResult<Map<String, Object>> getChartData(@RequestParam Long userId) {
        try {
            // 验证用户ID
            if (userId == null || userId <= 0) {
                return JsonResult.error("无效的用户ID");
            }

            Map<String, Object> chartData = dashboardService.getChartData(userId);

            // 确保返回的map不为null
            if (chartData == null) {
                chartData = new HashMap<>();
            }

            return JsonResult.success("图表数据获取成功", chartData);
        } catch (Exception e) {
            e.printStackTrace();
            return JsonResult.error("获取图表数据失败: " + e.getMessage());
        }
    }

    /**
     * 安全的获取整数值
     */
    private Integer getSafeInteger(Object value) {
        if (value == null) {
            return 0;
        }

        if (value instanceof Integer) {
            return (Integer) value;
        }

        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 安全的获取字符串值
     */
    private String getSafeString(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }

        return value.toString();
    }

    /**
     * 安全的获取浮点数值
     */
    private Double getSafeDouble(Object value) {
        if (value == null) {
            return 0.0;
        }

        if (value instanceof Double) {
            return (Double) value;
        }

        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}