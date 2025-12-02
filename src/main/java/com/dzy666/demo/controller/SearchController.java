package com.dzy666.demo.controller;

import com.dzy666.demo.entity.Document;
import com.dzy666.demo.service.DocumentService;
import com.dzy666.demo.service.SearchService;
import com.dzy666.demo.util.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    @Autowired
    private SearchService searchService;

    @Autowired
    private DocumentService documentService;

    // 原有接口保持不变...
    @GetMapping
    public JsonResult<List<Document>> search(@RequestParam String keyword,
                                             @RequestParam Long userId,
                                             @RequestParam(defaultValue = "10") int limit) {
        try {
            List<Long> docIds = searchService.search(keyword, userId, limit);
            List<Document> documents = documentService.getDocumentsByIds(docIds, userId);
            return JsonResult.success("搜索完成", documents);
        } catch (IOException e) {
            return JsonResult.error("搜索失败: " + e.getMessage());
        }
    }

    @PostMapping("/rebuild")
    public JsonResult<String> rebuildIndex(@RequestParam Long userId) {
        try {
            searchService.rebuildIndex(userId);
            return JsonResult.success("索引重建完成");
        } catch (IOException e) {
            return JsonResult.error("索引重建失败: " + e.getMessage());
        }
    }

    // 🔄 新增接口 - 高级搜索功能

    /**
     * 高级搜索
     */
    @PostMapping("/advanced")
    public JsonResult<List<Document>> advancedSearch(@RequestBody Map<String, Object> searchCriteria,
                                                     @RequestParam Long userId) {
        try {
            String keyword = (String) searchCriteria.get("keyword");
            Long categoryId = searchCriteria.get("categoryId") != null ?
                    Long.valueOf(searchCriteria.get("categoryId").toString()) : null;
            List<Long> tagIds = (List<Long>) searchCriteria.get("tagIds");
            String dateRange = (String) searchCriteria.get("dateRange");
            int limit = searchCriteria.get("limit") != null ?
                    (Integer) searchCriteria.get("limit") : 20;

            List<Long> docIds = searchService.advancedSearch(keyword, categoryId, tagIds, dateRange, userId, limit);
            List<Document> documents = documentService.getDocumentsByIds(docIds, userId);
            return JsonResult.success("高级搜索完成", documents);
        } catch (Exception e) {
            return JsonResult.error("高级搜索失败: " + e.getMessage());
        }
    }

    /**
     * 分类内搜索
     */
    @GetMapping("/category")
    public JsonResult<List<Document>> searchByCategory(@RequestParam String keyword,
                                                       @RequestParam Long categoryId,
                                                       @RequestParam Long userId,
                                                       @RequestParam(defaultValue = "10") int limit) {
        try {
            List<Long> docIds = searchService.searchByCategory(keyword, categoryId, userId, limit);
            List<Document> documents = documentService.getDocumentsByIds(docIds, userId);
            return JsonResult.success("分类搜索完成", documents);
        } catch (Exception e) {
            return JsonResult.error("分类搜索失败: " + e.getMessage());
        }
    }

    /**
     * 标签搜索
     */
    @GetMapping("/tag")
    public JsonResult<List<Document>> searchByTag(@RequestParam String keyword,
                                                  @RequestParam Long tagId,
                                                  @RequestParam Long userId,
                                                  @RequestParam(defaultValue = "10") int limit) {
        try {
            List<Long> docIds = searchService.searchByTag(keyword, tagId, userId, limit);
            List<Document> documents = documentService.getDocumentsByIds(docIds, userId);
            return JsonResult.success("标签搜索完成", documents);
        } catch (Exception e) {
            return JsonResult.error("标签搜索失败: " + e.getMessage());
        }
    }

    /**
     * 获取搜索建议
     */
    @GetMapping("/suggestions")
    public JsonResult<List<String>> getSearchSuggestions(@RequestParam String keyword,
                                                         @RequestParam Long userId) {
        try {
            List<String> suggestions = searchService.getSearchSuggestions(keyword, userId);
            return JsonResult.success(suggestions);
        } catch (Exception e) {
            return JsonResult.error("获取搜索建议失败: " + e.getMessage());
        }
    }

    /**
     * 获取搜索历史
     */
    @GetMapping("/history")
    public JsonResult<List<Map<String, Object>>> getSearchHistory(@RequestParam Long userId,
                                                                  @RequestParam(defaultValue = "10") int limit) {
        try {
            List<Map<String, Object>> history = searchService.getSearchHistory(userId, limit);
            return JsonResult.success(history);
        } catch (Exception e) {
            return JsonResult.error("获取搜索历史失败: " + e.getMessage());
        }
    }

    /**
     * 清除搜索历史
     */
    @DeleteMapping("/history")
    public JsonResult<Boolean> clearSearchHistory(@RequestParam Long userId) {
        try {
            boolean success = searchService.clearSearchHistory(userId);
            return JsonResult.success(success ? "搜索历史已清除" : "清除失败", success);
        } catch (Exception e) {
            return JsonResult.error("清除搜索历史失败: " + e.getMessage());
        }
    }

    /**
     * 获取搜索统计
     */
    @GetMapping("/stats")
    public JsonResult<Map<String, Object>> getSearchStats(@RequestParam Long userId) {
        try {
            Map<String, Object> stats = searchService.getSearchStatistics(userId);
            return JsonResult.success(stats);
        } catch (Exception e) {
            return JsonResult.error("获取搜索统计失败: " + e.getMessage());
        }
    }
}