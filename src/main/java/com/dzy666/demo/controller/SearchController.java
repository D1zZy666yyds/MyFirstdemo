package com.dzy666.demo.controller;

import com.dzy666.demo.dto.SearchResultDTO;
import com.dzy666.demo.dto.TagDTO;
import com.dzy666.demo.entity.Document;
import com.dzy666.demo.entity.Tag;
import com.dzy666.demo.service.DocumentService;
import com.dzy666.demo.service.SearchService;
import com.dzy666.demo.util.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    @Autowired
    private SearchService searchService;

    @Autowired
    private DocumentService documentService;

    /**
     * 基础搜索（全部分类）- 修复：添加排序参数
     */
    @GetMapping
    public JsonResult<List<SearchResultDTO>> search(@RequestParam String keyword,
                                                    @RequestParam Long userId,
                                                    @RequestParam(defaultValue = "50") int limit,
                                                    @RequestParam(defaultValue = "relevance") String sortBy) {
        System.out.println("=== 🔍 基础搜索开始 ===");
        System.out.println("📋 参数 - 关键词: '" + keyword + "', 用户ID: " + userId + ", 限制: " + limit + ", 排序: " + sortBy);

        try {
            // 1. 调用搜索服务获取文档ID列表（传入排序参数）
            List<Long> docIds = searchService.search(keyword, userId, limit, sortBy);
            System.out.println("📊 Lucene返回 " + docIds.size() + " 个文档ID");

            if (docIds.isEmpty()) {
                System.out.println("📭 未找到相关文档");
                return JsonResult.success("未找到相关文档", new ArrayList<>());
            }

            // 2. 获取完整文档信息并转换为标准DTO
            List<SearchResultDTO> results = convertToSearchResultDTO(docIds, userId);
            System.out.println("✅ 转换为 " + results.size() + " 个搜索结果DTO");

            return JsonResult.success("搜索完成", results);
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
     * 高级搜索 - 修复：添加排序参数，支持多标签
     */
    @PostMapping("/advanced")
    public JsonResult<List<SearchResultDTO>> advancedSearch(@RequestBody Map<String, Object> searchCriteria,
                                                            @RequestParam Long userId) {
        System.out.println("=== 🔍 高级搜索开始 ===");
        System.out.println("📋 参数 - 用户ID: " + userId + ", 条件: " + searchCriteria);

        try {
            String keyword = extractString(searchCriteria, "keyword");
            Long categoryId = extractLong(searchCriteria, "categoryId");
            List<Long> tagIds = extractTagIds(searchCriteria);
            String dateRange = extractString(searchCriteria, "dateRange");
            String sortBy = extractString(searchCriteria, "sortBy");
            if (sortBy == null) sortBy = "relevance";
            int limit = extractInt(searchCriteria, "limit", 50);

            System.out.println("🔧 解析参数: 关键词='" + keyword + "', 分类ID=" + categoryId +
                    ", 标签=" + tagIds + ", 日期范围=" + dateRange + ", 排序=" + sortBy);

            // 调用高级搜索（传入排序参数）
            List<Long> docIds = searchService.advancedSearch(keyword, categoryId, tagIds, dateRange, userId, limit, sortBy);
            System.out.println("📊 高级搜索返回 " + docIds.size() + " 个文档ID");

            if (docIds.isEmpty()) {
                return JsonResult.success("未找到匹配的文档", new ArrayList<>());
            }

            // 转换为标准DTO
            List<SearchResultDTO> results = convertToSearchResultDTO(docIds, userId);
            System.out.println("✅ 获取到 " + results.size() + " 个搜索结果");

            return JsonResult.success("高级搜索完成", results);
        } catch (Exception e) {
            System.err.println("❌ 高级搜索失败: " + e.getMessage());
            e.printStackTrace();
            return JsonResult.error("高级搜索失败: " + e.getMessage());
        } finally {
            System.out.println("=== 🔍 高级搜索结束 ===");
        }
    }

    /**
     * 分类内搜索 - 修复：添加排序参数
     */
    @GetMapping("/category")
    public JsonResult<List<SearchResultDTO>> searchByCategory(@RequestParam String keyword,
                                                              @RequestParam Long categoryId,
                                                              @RequestParam Long userId,
                                                              @RequestParam(defaultValue = "50") int limit,
                                                              @RequestParam(defaultValue = "relevance") String sortBy) {
        System.out.println("=== 🔍 分类搜索开始 ===");
        System.out.println("📋 参数 - 关键词: '" + keyword + "', 分类ID: " + categoryId +
                ", 用户ID: " + userId + ", 排序: " + sortBy);

        try {
            List<Long> docIds = searchService.searchByCategory(keyword, categoryId, userId, limit, sortBy);
            System.out.println("📊 Lucene分类搜索返回 " + docIds.size() + " 个文档ID");

            if (docIds.isEmpty()) {
                System.out.println("📭 该分类下未找到相关文档");
                return JsonResult.success("该分类下未找到相关文档", new ArrayList<>());
            }

            // 转换为标准DTO
            List<SearchResultDTO> results = convertToSearchResultDTO(docIds, userId);
            System.out.println("✅ 获取到 " + results.size() + " 个搜索结果");

            return JsonResult.success("分类搜索完成", results);
        } catch (Exception e) {
            System.err.println("❌ 分类搜索失败: " + e.getMessage());
            e.printStackTrace();
            return JsonResult.error("分类搜索失败: " + e.getMessage());
        } finally {
            System.out.println("=== 🔍 分类搜索结束 ===");
        }
    }

    /**
     * 标签搜索 - 修复：添加排序参数
     */
    @GetMapping("/tag")
    public JsonResult<List<SearchResultDTO>> searchByTag(@RequestParam String keyword,
                                                         @RequestParam Long tagId,
                                                         @RequestParam Long userId,
                                                         @RequestParam(defaultValue = "50") int limit,
                                                         @RequestParam(defaultValue = "relevance") String sortBy) {
        System.out.println("=== 🔍 标签搜索开始 ===");
        System.out.println("📋 参数 - 关键词: '" + keyword + "', 标签ID: " + tagId +
                ", 用户ID: " + userId + ", 排序: " + sortBy);

        try {
            List<Long> docIds = searchService.searchByTag(keyword, tagId, userId, limit, sortBy);
            System.out.println("📊 Lucene标签搜索返回 " + docIds.size() + " 个文档ID");

            if (docIds.isEmpty()) {
                System.out.println("📭 该标签下未找到相关文档");
                return JsonResult.success("该标签下未找到相关文档", new ArrayList<>());
            }

            // 转换为标准DTO
            List<SearchResultDTO> results = convertToSearchResultDTO(docIds, userId);
            System.out.println("✅ 获取到 " + results.size() + " 个搜索结果");

            return JsonResult.success("标签搜索完成", results);
        } catch (Exception e) {
            System.err.println("❌ 标签搜索失败: " + e.getMessage());
            e.printStackTrace();
            return JsonResult.error("标签搜索失败: " + e.getMessage());
        } finally {
            System.out.println("=== 🔍 标签搜索结束 ===");
        }
    }

    /**
     * 🎯 修复：智能搜索接口（统一入口）- 支持多标签和排序
     */
    @GetMapping("/smart")
    public JsonResult<List<SearchResultDTO>> smartSearch(
            @RequestParam String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) List<Long> tagIds,  // 改为List支持多标签
            @RequestParam Long userId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "relevance") String sortBy) {

        System.out.println("=== 🤖 智能搜索开始 ===");
        System.out.println("📋 智能搜索参数:");
        System.out.println("  • 关键词: '" + keyword + "'");
        System.out.println("  • 分类ID: " + categoryId);
        System.out.println("  • 标签IDs: " + tagIds);
        System.out.println("  • 用户ID: " + userId);
        System.out.println("  • 限制数: " + limit);
        System.out.println("  • 排序方式: " + sortBy);

        try {
            List<Long> docIds;

            // 🎯 智能路由：根据参数自动选择搜索策略
            if (categoryId != null && tagIds != null && !tagIds.isEmpty()) {
                // 情况1：分类 + 多标签组合搜索
                System.out.println("🔄 执行分类+多标签组合搜索");
                docIds = searchService.advancedSearch(keyword, categoryId, tagIds, null, userId, limit, sortBy);
            } else if (categoryId != null) {
                // 情况2：仅分类搜索
                System.out.println("🔄 执行分类搜索");
                docIds = searchService.searchByCategory(keyword, categoryId, userId, limit, sortBy);
            } else if (tagIds != null && !tagIds.isEmpty()) {
                // 情况3：仅多标签搜索
                System.out.println("🔄 执行多标签搜索");
                if (tagIds.size() == 1) {
                    // 单个标签使用专门的标签搜索方法
                    docIds = searchService.searchByTag(keyword, tagIds.get(0), userId, limit, sortBy);
                } else {
                    // 多个标签使用高级搜索
                    docIds = searchService.advancedSearch(keyword, null, tagIds, null, userId, limit, sortBy);
                }
            } else {
                // 情况4：基础搜索
                System.out.println("🔄 执行基础搜索");
                docIds = searchService.search(keyword, userId, limit, sortBy);
            }

            System.out.println("📊 智能搜索返回 " + docIds.size() + " 个文档ID");

            if (docIds.isEmpty()) {
                System.out.println("📭 未找到匹配的文档");
                return JsonResult.success("未找到匹配的文档", new ArrayList<>());
            }

            // 🎯 转换为标准DTO格式
            List<SearchResultDTO> results = convertToSearchResultDTO(docIds, userId);
            System.out.println("✅ 获取到 " + results.size() + " 个搜索结果");

            // 记录搜索统计
            logSearchStatistics(results, keyword, categoryId, tagIds);

            return JsonResult.success("智能搜索完成", results);
        } catch (Exception e) {
            System.err.println("❌ 智能搜索失败: " + e.getMessage());
            e.printStackTrace();
            return JsonResult.error("智能搜索失败: " + e.getMessage());
        } finally {
            System.out.println("=== 🤖 智能搜索结束 ===");
        }
    }

    /**
     * 🎯 新增：快速搜索（带排序）
     */
    @GetMapping("/quick")
    public JsonResult<List<SearchResultDTO>> quickSearch(@RequestParam String keyword,
                                                         @RequestParam Long userId,
                                                         @RequestParam(defaultValue = "20") int limit,
                                                         @RequestParam(defaultValue = "relevance") String sortBy) {
        System.out.println("⚡ 快速搜索: '" + keyword + "', 排序: " + sortBy);

        try {
            // 使用基础搜索但限制结果数
            List<Long> docIds = searchService.search(keyword, userId, Math.min(limit, 20), sortBy);

            if (docIds.isEmpty()) {
                return JsonResult.success("未找到相关文档", new ArrayList<>());
            }

            List<SearchResultDTO> results = convertToSearchResultDTO(docIds, userId);
            return JsonResult.success("快速搜索完成", results);
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

    /**
     * 🎯 新增：将文档ID列表转换为标准搜索结果的DTO
     */
    private List<SearchResultDTO> convertToSearchResultDTO(List<Long> docIds, Long userId) {
        System.out.println("🔄 开始转换搜索结果，文档ID数量: " + docIds.size());

        // 获取文档详情
        List<Map<String, Object>> documents = documentService.getDocumentsWithDetailsByIds(docIds, userId);

        if (documents == null || documents.isEmpty()) {
            System.out.println("⚠️ 未获取到文档详情");
            return new ArrayList<>();
        }

        List<SearchResultDTO> results = new ArrayList<>();

        for (Map<String, Object> doc : documents) {
            try {
                SearchResultDTO dto = SearchResultDTO.builder()
                        .id(getLongValue(doc, "id"))
                        .title(getStringValue(doc, "title", "无标题"))
                        .contentPreview(getContentPreview(getStringValue(doc, "content", "")))
                        .categoryId(getLongValue(doc, "categoryId"))
                        .categoryName(getStringValue(doc, "categoryName", "未分类"))
                        .tags(convertToTagDTOs(doc))
                        .createdTime(getLocalDateTimeValue(doc, "createdTime"))
                        .updatedTime(getLocalDateTimeValue(doc, "updatedTime"))
                        .contentType(getStringValue(doc, "contentType", "TEXT"))
                        .relevanceScore(0.8) // 暂时固定值，后续可从Lucene获取
                        .build();

                results.add(dto);
                System.out.println("✅ 转换文档: " + dto.getTitle() + " (ID: " + dto.getId() + ")");

            } catch (Exception e) {
                System.err.println("❌ 转换文档失败: " + doc.get("id") + " - " + e.getMessage());
            }
        }

        System.out.println("🎉 成功转换 " + results.size() + " 个搜索结果");
        return results;
    }

    /**
     * 🎯 新增：内容预览生成
     */
    private String getContentPreview(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "无内容";
        }

        // 移除HTML标签
        String plainText = content.replaceAll("<[^>]*>", "");

        // 限制长度
        int maxLength = 200;
        if (plainText.length() <= maxLength) {
            return plainText;
        }

        return plainText.substring(0, maxLength) + "...";
    }

    /**
     * 🎯 新增：转换为TagDTO列表
     */
    private List<TagDTO> convertToTagDTOs(Map<String, Object> doc) {
        try {
            Object tagsObj = doc.get("tagList");
            if (tagsObj instanceof List) {
                List<?> tagList = (List<?>) tagsObj;
                return tagList.stream()
                        .filter(item -> item instanceof Tag)
                        .map(item -> {
                            Tag tag = (Tag) item;
                            return TagDTO.builder()
                                    .id(tag.getId())
                                    .name(tag.getName())
                                    .build();
                        })
                        .collect(Collectors.toList());
            }

            // 备用：从tagNames转换
            Object tagNamesObj = doc.get("tags");
            if (tagNamesObj instanceof List) {
                List<?> tagNamesList = (List<?>) tagNamesObj;
                return tagNamesList.stream()
                        .filter(item -> item instanceof String)
                        .map(item -> TagDTO.builder()
                                .id(0L) // 没有ID
                                .name((String) item)
                                .build())
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            System.err.println("转换标签失败: " + e.getMessage());
        }

        return new ArrayList<>();
    }

    // ========== 辅助方法 ==========

    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        try {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private java.time.LocalDateTime getLocalDateTimeValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof java.time.LocalDateTime) {
            return (java.time.LocalDateTime) value;
        }
        return null;
    }

    /**
     * 记录搜索统计信息
     */
    private void logSearchStatistics(List<SearchResultDTO> results, String keyword,
                                     Long categoryId, List<Long> tagIds) {
        System.out.println("📈 搜索统计信息:");
        System.out.println("  • 结果数量: " + results.size());
        System.out.println("  • 关键词: '" + keyword + "'");
        if (categoryId != null) {
            System.out.println("  • 分类ID: " + categoryId);
        }
        if (tagIds != null && !tagIds.isEmpty()) {
            System.out.println("  • 标签IDs: " + tagIds);
        }

        // 统计标签分布
        if (!results.isEmpty()) {
            Map<String, Integer> tagDistribution = new HashMap<>();
            for (SearchResultDTO result : results) {
                if (result.getTags() != null) {
                    for (TagDTO tag : result.getTags()) {
                        tagDistribution.put(tag.getName(), tagDistribution.getOrDefault(tag.getName(), 0) + 1);
                    }
                }
            }
            if (!tagDistribution.isEmpty()) {
                System.out.println("  • 标签分布: " + tagDistribution);
            }
        }
    }

    private String extractString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

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

    /**
     * 🎯 新增：测试标签搜索功能
     */
    @GetMapping("/test/tag-search")
    public JsonResult<List<Long>> testTagSearch(@RequestParam List<Long> tagIds,
                                                @RequestParam Long userId) {
        try {
            System.out.println("测试标签搜索: 标签IDs=" + tagIds + ", 用户ID=" + userId);
            List<Long> results = searchService.testTagSearch(tagIds, userId);
            return JsonResult.success("标签搜索测试完成", results);
        } catch (Exception e) {
            System.err.println("标签搜索测试失败: " + e.getMessage());
            return JsonResult.error("标签搜索测试失败: " + e.getMessage());
        }
    }
}