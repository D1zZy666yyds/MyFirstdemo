package com.dzy666.demo.controller;

import com.dzy666.demo.entity.Document;
import com.dzy666.demo.service.DocumentService;
import com.dzy666.demo.service.SearchService;
import com.dzy666.demo.util.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    @Autowired
    private SearchService searchService;

    @Autowired
    private DocumentService documentService;

    /**
     * 基础搜索（全部分类）- 增强修复版
     * 🎯 添加详细日志，便于调试
     */
    @GetMapping
    public JsonResult<List<Map<String, Object>>> search(@RequestParam String keyword,
                                                        @RequestParam Long userId,
                                                        @RequestParam(defaultValue = "50") int limit) {
        System.out.println("=== 🔍 基础搜索开始 ===");
        System.out.println("📋 参数 - 关键词: '" + keyword + "', 用户ID: " + userId + ", 限制: " + limit);

        try {
            // 1. 调用搜索服务获取文档ID列表
            List<Long> docIds = searchService.search(keyword, userId, limit);
            System.out.println("📊 Lucene返回 " + docIds.size() + " 个文档ID");

            if (docIds.isEmpty()) {
                System.out.println("📭 未找到相关文档");
                return JsonResult.success("未找到相关文档", new ArrayList<>());
            }

            // 2. 🎯 获取完整文档信息（包含分类、标签等）
            List<Map<String, Object>> documents = documentService.getDocumentsWithDetailsByIds(docIds, userId);
            System.out.println("✅ 文档服务返回 " + documents.size() + " 个文档详情");

            // 3. 打印文档结构便于调试
            logDocumentDetails(documents);

            return JsonResult.success("搜索完成", documents);
        } catch (IOException e) {
            System.err.println("❌ 搜索失败: " + e.getMessage());
            e.printStackTrace();
            return JsonResult.error("搜索失败: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ 其他错误: " + e.getMessage());
            e.printStackTrace();
            return JsonResult.error("搜索处理失败: " + e.getMessage());
        } finally {
            System.out.println("=== 🔍 基础搜索结束 ===");
        }
    }

    /**
     * 高级搜索 - 增强修复版
     */
    @PostMapping("/advanced")
    public JsonResult<List<Map<String, Object>>> advancedSearch(@RequestBody Map<String, Object> searchCriteria,
                                                                @RequestParam Long userId) {
        System.out.println("=== 🔍 高级搜索开始 ===");
        System.out.println("📋 参数 - 用户ID: " + userId + ", 条件: " + searchCriteria);

        try {
            String keyword = extractString(searchCriteria, "keyword");
            Long categoryId = extractLong(searchCriteria, "categoryId");
            List<Long> tagIds = extractTagIds(searchCriteria);
            String dateRange = extractString(searchCriteria, "dateRange");
            int limit = extractInt(searchCriteria, "limit", 50);

            System.out.println("🔧 解析参数: 关键词='" + keyword + "', 分类ID=" + categoryId +
                    ", 标签=" + tagIds + ", 日期范围=" + dateRange);

            // 调用高级搜索
            List<Long> docIds = searchService.advancedSearch(keyword, categoryId, tagIds, dateRange, userId, limit);
            System.out.println("📊 高级搜索返回 " + docIds.size() + " 个文档ID");

            if (docIds.isEmpty()) {
                return JsonResult.success("未找到匹配的文档", new ArrayList<>());
            }

            // 🎯 获取完整文档信息
            List<Map<String, Object>> documents = documentService.getDocumentsWithDetailsByIds(docIds, userId);
            System.out.println("✅ 获取到 " + documents.size() + " 个文档详情");

            return JsonResult.success("高级搜索完成", documents);
        } catch (Exception e) {
            System.err.println("❌ 高级搜索失败: " + e.getMessage());
            e.printStackTrace();
            return JsonResult.error("高级搜索失败: " + e.getMessage());
        } finally {
            System.out.println("=== 🔍 高级搜索结束 ===");
        }
    }

    /**
     * 分类内搜索 - 增强修复版
     */
    @GetMapping("/category")
    public JsonResult<List<Map<String, Object>>> searchByCategory(@RequestParam String keyword,
                                                                  @RequestParam Long categoryId,
                                                                  @RequestParam Long userId,
                                                                  @RequestParam(defaultValue = "50") int limit) {
        System.out.println("=== 🔍 分类搜索开始 ===");
        System.out.println("📋 参数 - 关键词: '" + keyword + "', 分类ID: " + categoryId + ", 用户ID: " + userId);

        try {
            List<Long> docIds = searchService.searchByCategory(keyword, categoryId, userId, limit);
            System.out.println("📊 Lucene分类搜索返回 " + docIds.size() + " 个文档ID");

            if (docIds.isEmpty()) {
                System.out.println("📭 该分类下未找到相关文档");
                return JsonResult.success("该分类下未找到相关文档", new ArrayList<>());
            }

            // 🎯 获取完整文档信息
            List<Map<String, Object>> documents = documentService.getDocumentsWithDetailsByIds(docIds, userId);
            System.out.println("✅ 获取到 " + documents.size() + " 个文档详情");

            return JsonResult.success("分类搜索完成", documents);
        } catch (Exception e) {
            System.err.println("❌ 分类搜索失败: " + e.getMessage());
            e.printStackTrace();
            return JsonResult.error("分类搜索失败: " + e.getMessage());
        } finally {
            System.out.println("=== 🔍 分类搜索结束 ===");
        }
    }

    /**
     * 标签搜索 - 增强修复版
     */
    @GetMapping("/tag")
    public JsonResult<List<Map<String, Object>>> searchByTag(@RequestParam String keyword,
                                                             @RequestParam Long tagId,
                                                             @RequestParam Long userId,
                                                             @RequestParam(defaultValue = "50") int limit) {
        System.out.println("=== 🔍 标签搜索开始 ===");
        System.out.println("📋 参数 - 关键词: '" + keyword + "', 标签ID: " + tagId + ", 用户ID: " + userId);

        try {
            List<Long> docIds = searchService.searchByTag(keyword, tagId, userId, limit);
            System.out.println("📊 Lucene标签搜索返回 " + docIds.size() + " 个文档ID");

            if (docIds.isEmpty()) {
                System.out.println("📭 该标签下未找到相关文档");
                return JsonResult.success("该标签下未找到相关文档", new ArrayList<>());
            }

            // 🎯 获取完整文档信息
            List<Map<String, Object>> documents = documentService.getDocumentsWithDetailsByIds(docIds, userId);
            System.out.println("✅ 获取到 " + documents.size() + " 个文档详情");

            return JsonResult.success("标签搜索完成", documents);
        } catch (Exception e) {
            System.err.println("❌ 标签搜索失败: " + e.getMessage());
            e.printStackTrace();
            return JsonResult.error("标签搜索失败: " + e.getMessage());
        } finally {
            System.out.println("=== 🔍 标签搜索结束 ===");
        }
    }

    /**
     * 🎯 新增：智能搜索接口（统一入口）
     * 前端可以直接调用此接口，内部根据参数自动选择搜索策略
     */
    @GetMapping("/smart")
    public JsonResult<List<Map<String, Object>>> smartSearch(
            @RequestParam String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId,
            @RequestParam Long userId,
            @RequestParam(defaultValue = "50") int limit) {

        System.out.println("=== 🤖 智能搜索开始 ===");
        System.out.println("📋 智能搜索参数:");
        System.out.println("  • 关键词: '" + keyword + "'");
        System.out.println("  • 分类ID: " + categoryId);
        System.out.println("  • 标签ID: " + tagId);
        System.out.println("  • 用户ID: " + userId);
        System.out.println("  • 限制数: " + limit);

        try {
            List<Long> docIds;

            // 🎯 智能路由：根据参数自动选择搜索策略
            if (categoryId != null && tagId != null) {
                // 情况1：分类 + 标签组合搜索
                System.out.println("🔄 执行分类+标签组合搜索");
                List<Long> tagIds = Collections.singletonList(tagId);
                docIds = searchService.advancedSearch(keyword, categoryId, tagIds, null, userId, limit);
            } else if (categoryId != null) {
                // 情况2：仅分类搜索
                System.out.println("🔄 执行分类搜索");
                docIds = searchService.searchByCategory(keyword, categoryId, userId, limit);
            } else if (tagId != null) {
                // 情况3：仅标签搜索
                System.out.println("🔄 执行标签搜索");
                docIds = searchService.searchByTag(keyword, tagId, userId, limit);
            } else {
                // 情况4：基础搜索
                System.out.println("🔄 执行基础搜索");
                docIds = searchService.search(keyword, userId, limit);
            }

            System.out.println("📊 智能搜索返回 " + docIds.size() + " 个文档ID");

            if (docIds.isEmpty()) {
                System.out.println("📭 未找到匹配的文档");
                return JsonResult.success("未找到匹配的文档", new ArrayList<>());
            }

            // 🎯 获取完整文档信息
            List<Map<String, Object>> documents = documentService.getDocumentsWithDetailsByIds(docIds, userId);
            System.out.println("✅ 获取到 " + documents.size() + " 个文档详情");

            // 记录搜索统计
            logSearchStatistics(documents, keyword, categoryId, tagId);

            return JsonResult.success("智能搜索完成", documents);
        } catch (Exception e) {
            System.err.println("❌ 智能搜索失败: " + e.getMessage());
            e.printStackTrace();
            return JsonResult.error("智能搜索失败: " + e.getMessage());
        } finally {
            System.out.println("=== 🤖 智能搜索结束 ===");
        }
    }

    /**
     * 🎯 新增：快速搜索（不带筛选条件，用于全局搜索框）
     */
    @GetMapping("/quick")
    public JsonResult<List<Map<String, Object>>> quickSearch(@RequestParam String keyword,
                                                             @RequestParam Long userId,
                                                             @RequestParam(defaultValue = "20") int limit) {
        System.out.println("⚡ 快速搜索: '" + keyword + "'");

        try {
            // 使用基础搜索但限制结果数
            List<Long> docIds = searchService.search(keyword, userId, Math.min(limit, 20));

            if (docIds.isEmpty()) {
                return JsonResult.success("未找到相关文档", new ArrayList<>());
            }

            List<Map<String, Object>> documents = documentService.getDocumentsWithDetailsByIds(docIds, userId);
            return JsonResult.success("快速搜索完成", documents);
        } catch (Exception e) {
            System.err.println("快速搜索失败: " + e.getMessage());
            return JsonResult.error("快速搜索失败: " + e.getMessage());
        }
    }

    /**
     * 🎯 新增：获取搜索建议
     */
    @GetMapping("/suggestions")
    public JsonResult<List<String>> getSearchSuggestions(@RequestParam String keyword,
                                                         @RequestParam Long userId,
                                                         @RequestParam(defaultValue = "5") int limit) {
        try {
            List<String> suggestions = searchService.getSearchSuggestions(keyword, userId);

            // 限制返回数量
            if (suggestions.size() > limit) {
                suggestions = suggestions.subList(0, limit);
            }

            return JsonResult.success("搜索建议获取成功", suggestions);
        } catch (Exception e) {
            System.err.println("获取搜索建议失败: " + e.getMessage());
            return JsonResult.error("获取搜索建议失败: " + e.getMessage());
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 记录文档详情（调试用）
     */
    private void logDocumentDetails(List<Map<String, Object>> documents) {
        if (documents.isEmpty()) return;

        Map<String, Object> firstDoc = documents.get(0);
        System.out.println("📝 文档结构调试信息:");
        System.out.println("  🔑 所有字段: " + firstDoc.keySet());

        // 检查关键字段
        String[] criticalFields = {"id", "title", "content", "categoryId", "tags", "updatedTime"};
        for (String field : criticalFields) {
            Object value = firstDoc.get(field);
            System.out.println("  📌 " + field + ": " +
                    (value != null ? value.toString() : "null") +
                    " (类型: " + (value != null ? value.getClass().getSimpleName() : "null") + ")");
        }

        // 检查标签字段
        Object tags = firstDoc.get("tags");
        if (tags != null) {
            System.out.println("  🏷️  标签详情: " + tags);
            if (tags instanceof List) {
                System.out.println("  📊 标签数量: " + ((List<?>) tags).size());
            }
        }
    }

    /**
     * 记录搜索统计信息
     */
    private void logSearchStatistics(List<Map<String, Object>> documents, String keyword,
                                     Long categoryId, Long tagId) {
        System.out.println("📈 搜索统计信息:");
        System.out.println("  • 结果数量: " + documents.size());
        System.out.println("  • 关键词: '" + keyword + "'");
        if (categoryId != null) {
            System.out.println("  • 分类ID: " + categoryId);
        }
        if (tagId != null) {
            System.out.println("  • 标签ID: " + tagId);
        }

        // 统计标签分布
        if (!documents.isEmpty()) {
            Map<String, Integer> tagDistribution = new HashMap<>();
            for (Map<String, Object> doc : documents) {
                Object tags = doc.get("tags");
                if (tags instanceof List) {
                    for (Object tag : (List<?>) tags) {
                        String tagName = tag.toString();
                        tagDistribution.put(tagName, tagDistribution.getOrDefault(tagName, 0) + 1);
                    }
                }
            }
            if (!tagDistribution.isEmpty()) {
                System.out.println("  • 标签分布: " + tagDistribution);
            }
        }
    }

    /**
     * 从Map中提取字符串
     */
    private String extractString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 从Map中提取Long
     */
    private Long extractLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        try {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            System.err.println("解析Long失败: key=" + key + ", value=" + value);
            return null;
        }
    }

    /**
     * 从Map中提取Int
     */
    private int extractInt(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        try {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            System.err.println("解析Int失败: key=" + key + ", value=" + value);
            return defaultValue;
        }
    }

    /**
     * 从Map中提取标签ID列表
     */
    @SuppressWarnings("unchecked")
    private List<Long> extractTagIds(Map<String, Object> map) {
        Object value = map.get("tagIds");
        if (value == null) return null;

        if (value instanceof List) {
            List<Object> list = (List<Object>) value;
            List<Long> tagIds = new ArrayList<>();
            for (Object item : list) {
                try {
                    if (item instanceof Number) {
                        tagIds.add(((Number) item).longValue());
                    } else {
                        tagIds.add(Long.valueOf(item.toString()));
                    }
                } catch (NumberFormatException e) {
                    System.err.println("解析标签ID失败: " + item);
                }
            }
            return tagIds.isEmpty() ? null : tagIds;
        }
        return null;
    }

    // 其他方法保持不变...
}