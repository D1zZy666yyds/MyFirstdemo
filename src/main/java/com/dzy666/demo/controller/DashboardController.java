package com.dzy666.demo.controller;

import com.dzy666.demo.service.DashboardService;
import com.dzy666.demo.util.JsonResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 获取仪表盘统计数据
     */
    @GetMapping("/stats")
    public JsonResult<Map<String, Object>> getDashboardStats(
            @RequestParam Long userId,
            @RequestHeader(value = "Authorization", required = false) String token) {

        try {
            log.info("获取用户 {} 的仪表盘统计", userId);

            // 这里可以添加权限验证
            // verifyUserPermission(userId, token);

            Map<String, Object> stats = dashboardService.getDashboardStats(userId);
            return JsonResult.success("获取仪表盘统计数据成功", stats);

        } catch (Exception e) {
            log.error("获取仪表盘统计失败 - userId: {}", userId, e);
            return JsonResult.error(500, "获取统计数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取快速统计（轻量级）
     */
    @GetMapping("/quick-stats")
    public JsonResult<Map<String, Object>> getQuickStats(@RequestParam Long userId) {
        try {
            log.debug("获取用户 {} 的快速统计", userId);
            Map<String, Object> quickStats = dashboardService.getQuickStats(userId);
            return JsonResult.success("获取快速统计成功", quickStats);
        } catch (Exception e) {
            log.error("获取快速统计失败", e);
            return JsonResult.error(500, "获取快速统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取文档创建趋势
     */
    @GetMapping("/trend")
    public JsonResult<Map<String, Object>> getDocumentTrend(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            log.debug("获取用户 {} 的文档趋势，天数: {}", userId, days);

            Map<String, Object> trendData;
            if (startDate != null && endDate != null) {
                // 使用自定义日期范围
                trendData = dashboardService.getDocumentTrendByRange(
                        userId,
                        startDate.toString(),
                        endDate.toString()
                );
            } else {
                // 使用默认天数
                trendData = dashboardService.getDocumentTrendByRange(
                        userId,
                        LocalDate.now().minusDays(days).toString(),
                        LocalDate.now().toString()
                );
            }

            return JsonResult.success("获取趋势数据成功", trendData);

        } catch (Exception e) {
            log.error("获取文档趋势失败", e);
            return JsonResult.error(500, "获取趋势数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取最近文档列表
     */
    @GetMapping("/recent-documents")
    public JsonResult<Map<String, Object>> getRecentDocuments(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "10") int limit) {

        try {
            log.debug("获取用户 {} 的最近文档，数量: {}", userId, limit);

            Map<String, Object> result = new HashMap<>();
            // 这里可以调用扩展的服务方法获取最近文档
            // 暂时返回模拟数据或从统计中提取
            Map<String, Object> stats = dashboardService.getDashboardStats(userId);
            result.put("documents", stats.get("recentDocuments"));
            result.put("total", ((java.util.List<?>) stats.get("recentDocuments")).size());

            return JsonResult.success("获取最近文档成功", result);

        } catch (Exception e) {
            log.error("获取最近文档失败", e);
            return JsonResult.error(500, "获取最近文档失败: " + e.getMessage());
        }
    }

    /**
     * 获取热门标签
     */
    @GetMapping("/popular-tags")
    public JsonResult<Map<String, Object>> getPopularTags(@RequestParam Long userId) {
        try {
            log.debug("获取用户 {} 的热门标签", userId);

            Map<String, Object> result = new HashMap<>();
            Map<String, Object> stats = dashboardService.getDashboardStats(userId);
            result.put("tags", stats.get("popularTags"));

            return JsonResult.success("获取热门标签成功", result);

        } catch (Exception e) {
            log.error("获取热门标签失败", e);
            return JsonResult.error(500, "获取热门标签失败: " + e.getMessage());
        }
    }

    /**
     * 获取分类分布
     */
    @GetMapping("/category-distribution")
    public JsonResult<Map<String, Object>> getCategoryDistribution(@RequestParam Long userId) {
        try {
            log.debug("获取用户 {} 的分类分布", userId);

            Map<String, Object> result = new HashMap<>();
            Map<String, Object> stats = dashboardService.getDashboardStats(userId);
            result.put("distribution", stats.get("categoryDistribution"));

            return JsonResult.success("获取分类分布成功", result);

        } catch (Exception e) {
            log.error("获取分类分布失败", e);
            return JsonResult.error(500, "获取分类分布失败: " + e.getMessage());
        }
    }

    /**
     * 获取学习统计
     */
    @GetMapping("/learning-stats")
    public JsonResult<Map<String, Object>> getLearningStats(@RequestParam Long userId) {
        try {
            log.debug("获取用户 {} 的学习统计", userId);

            Map<String, Object> result = new HashMap<>();
            Map<String, Object> stats = dashboardService.getDashboardStats(userId);
            result.put("stats", stats.get("learningStats"));

            return JsonResult.success("获取学习统计成功", result);

        } catch (Exception e) {
            log.error("获取学习统计失败", e);
            return JsonResult.error(500, "获取学习统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户活跃统计
     */
    @GetMapping("/user-activity")
    public JsonResult<Map<String, Object>> getUserActivityStats(@RequestParam Long userId) {
        try {
            log.debug("获取用户 {} 的活跃统计", userId);

            Map<String, Object> result = new HashMap<>();
            Map<String, Object> stats = dashboardService.getDashboardStats(userId);
            result.put("activity", stats.get("userActivity"));

            return JsonResult.success("获取用户活跃统计成功", result);

        } catch (Exception e) {
            log.error("获取用户活跃统计失败", e);
            return JsonResult.error(500, "获取用户活跃统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取文档大小统计
     */
    @GetMapping("/size-stats")
    public JsonResult<Map<String, Object>> getDocumentSizeStats(@RequestParam Long userId) {
        try {
            log.debug("获取用户 {} 的文档大小统计", userId);

            Map<String, Object> result = new HashMap<>();
            Map<String, Object> stats = dashboardService.getDashboardStats(userId);
            result.put("sizeStats", stats.get("sizeStats"));

            return JsonResult.success("获取文档大小统计成功", result);

        } catch (Exception e) {
            log.error("获取文档大小统计失败", e);
            return JsonResult.error(500, "获取文档大小统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取最近操作日志
     */
    @GetMapping("/recent-operations")
    public JsonResult<Map<String, Object>> getRecentOperations(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "10") int limit) {

        try {
            log.debug("获取用户 {} 的最近操作，数量: {}", userId, limit);

            Map<String, Object> result = new HashMap<>();
            Map<String, Object> stats = dashboardService.getDashboardStats(userId);
            result.put("operations", stats.get("recentOperations"));

            return JsonResult.success("获取最近操作成功", result);

        } catch (Exception e) {
            log.error("获取最近操作失败", e);
            return JsonResult.error(500, "获取最近操作失败: " + e.getMessage());
        }
    }
}