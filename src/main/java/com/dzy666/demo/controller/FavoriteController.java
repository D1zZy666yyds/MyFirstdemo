package com.dzy666.demo.controller;

import com.dzy666.demo.entity.Document;
import com.dzy666.demo.entity.Category;
import com.dzy666.demo.entity.Tag;
import com.dzy666.demo.service.FavoriteService;
import com.dzy666.demo.util.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorite")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    /**
     * 收藏文档
     * POST /api/favorite/document/{documentId}?userId={userId}
     */
    @PostMapping("/document/{documentId}")
    public JsonResult<Boolean> addFavorite(@PathVariable Long documentId,
                                           @RequestParam Long userId) {
        try {
            boolean success = favoriteService.addFavorite(documentId, userId);
            return JsonResult.success("收藏成功", success);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    /**
     * 取消收藏
     * DELETE /api/favorite/document/{documentId}?userId={userId}
     */
    @DeleteMapping("/document/{documentId}")
    public JsonResult<Boolean> removeFavorite(@PathVariable Long documentId,
                                              @RequestParam Long userId) {
        try {
            boolean success = favoriteService.removeFavorite(documentId, userId);
            return JsonResult.success("取消收藏成功", success);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    /**
     * 检查是否已收藏
     * GET /api/favorite/document/{documentId}?userId={userId}
     */
    @GetMapping("/document/{documentId}")
    public JsonResult<Boolean> isFavorite(@PathVariable Long documentId,
                                          @RequestParam Long userId) {
        try {
            boolean isFavorite = favoriteService.isFavorite(documentId, userId);
            return JsonResult.success(isFavorite);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    /**
     * 获取用户的收藏文档列表（支持筛选）
     * GET /api/favorite/user/{userId}
     * GET /api/favorite/user/{userId}?categoryId=1
     * GET /api/favorite/user/{userId}?tagId=2
     * GET /api/favorite/user/{userId}?categoryId=1&tagId=2
     */
    @GetMapping("/user/{userId}")
    public JsonResult<List<Document>> getUserFavorites(
            @PathVariable Long userId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId) {
        try {
            List<Document> favorites = favoriteService.getUserFavorites(userId, categoryId, tagId);
            return JsonResult.success("获取成功", favorites);
        } catch (Exception e) {
            return JsonResult.error("获取收藏列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取文档的收藏数量
     * GET /api/favorite/document/{documentId}/count
     */
    @GetMapping("/document/{documentId}/count")
    public JsonResult<Integer> getFavoriteCount(@PathVariable Long documentId) {
        try {
            int count = favoriteService.getFavoriteCount(documentId);
            return JsonResult.success(count);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    /**
     * 获取收藏中的分类列表（仅包含有收藏的分类）
     * GET /api/favorite/user/{userId}/categories
     */
    @GetMapping("/user/{userId}/categories")
    public JsonResult<List<Category>> getFavoriteCategories(@PathVariable Long userId) {
        try {
            List<Category> categories = favoriteService.getFavoriteCategories(userId);
            return JsonResult.success("获取成功", categories);
        } catch (Exception e) {
            return JsonResult.error("获取收藏分类失败: " + e.getMessage());
        }
    }

    /**
     * 获取收藏中的标签列表（仅包含有收藏的标签）
     * GET /api/favorite/user/{userId}/tags
     */
    @GetMapping("/user/{userId}/tags")
    public JsonResult<List<Tag>> getFavoriteTags(@PathVariable Long userId) {
        try {
            List<Tag> tags = favoriteService.getFavoriteTags(userId);
            return JsonResult.success("获取成功", tags);
        } catch (Exception e) {
            return JsonResult.error("获取收藏标签失败: " + e.getMessage());
        }
    }

    /**
     * 批量检查收藏状态
     * POST /api/favorite/batch-check?userId={userId}
     * 请求体: {"documentIds": [1, 2, 3]}
     */
    @PostMapping("/batch-check")
    public JsonResult<Map<Long, Boolean>> batchCheckFavoriteStatus(
            @RequestBody Map<String, Object> request,
            @RequestParam Long userId) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> documentIds = (List<Long>) request.get("documentIds");
            if (documentIds == null || documentIds.isEmpty()) {
                return JsonResult.success("参数为空", Map.of());
            }

            Map<Long, Boolean> result = favoriteService.batchCheckFavoriteStatus(documentIds, userId);
            return JsonResult.success("批量查询成功", result);
        } catch (Exception e) {
            return JsonResult.error("批量查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取收藏统计信息
     * GET /api/favorite/user/{userId}/stats
     */
    @GetMapping("/user/{userId}/stats")
    public JsonResult<Map<String, Object>> getFavoriteStats(@PathVariable Long userId) {
        try {
            Map<String, Object> stats = favoriteService.getFavoriteStats(userId);
            return JsonResult.success("获取成功", stats);
        } catch (Exception e) {
            return JsonResult.error("获取收藏统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取热门收藏（最常被收藏的文档）
     * GET /api/favorite/hot/{userId}?limit=10
     */
    @GetMapping("/hot/{userId}")
    public JsonResult<List<Document>> getHotFavorites(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<Document> hotFavorites = favoriteService.getHotFavorites(userId, limit);
            return JsonResult.success("获取成功", hotFavorites);
        } catch (Exception e) {
            return JsonResult.error("获取热门收藏失败: " + e.getMessage());
        }
    }

    /**
     * 搜索收藏文档
     * GET /api/favorite/search/{userId}?keyword=搜索词
     * GET /api/favorite/search/{userId}?keyword=搜索词&categoryId=1
     * GET /api/favorite/search/{userId}?keyword=搜索词&tagId=2
     */
    @GetMapping("/search/{userId}")
    public JsonResult<List<Document>> searchFavoriteDocuments(
            @PathVariable Long userId,
            @RequestParam String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId) {
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                return JsonResult.error("搜索关键词不能为空");
            }

            List<Document> results = favoriteService.searchFavoriteDocuments(
                    userId, keyword.trim(), categoryId, tagId);
            return JsonResult.success("搜索成功", results);
        } catch (Exception e) {
            return JsonResult.error("搜索收藏文档失败: " + e.getMessage());
        }
    }

    /**
     * 批量更新文档收藏信息
     * POST /api/favorite/enrich-documents?userId={userId}
     * 请求体: 文档列表
     */
    @PostMapping("/enrich-documents")
    public JsonResult<List<Document>> enrichDocumentsWithFavoriteInfo(
            @RequestBody List<Document> documents,
            @RequestParam Long userId) {
        try {
            List<Document> enrichedDocs = favoriteService.batchEnrichDocumentsWithFavoriteInfo(documents, userId);
            return JsonResult.success("文档信息丰富成功", enrichedDocs);
        } catch (Exception e) {
            return JsonResult.error("丰富文档信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取分类下的收藏文档数量统计
     * GET /api/favorite/category-stats/{userId}
     */
    @GetMapping("/category-stats/{userId}")
    public JsonResult<Map<Long, Integer>> getCategoryFavoriteCounts(@PathVariable Long userId) {
        try {
            Map<Long, Integer> categoryStats = favoriteService.getCategoryFavoriteCounts(userId);
            return JsonResult.success("获取成功", categoryStats);
        } catch (Exception e) {
            return JsonResult.error("获取分类统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户的收藏文档总数
     * GET /api/favorite/user/{userId}/count
     */
    @GetMapping("/user/{userId}/count")
    public JsonResult<Integer> getFavoriteCountByUser(@PathVariable Long userId) {
        try {
            int count = favoriteService.getUserFavorites(userId).size();
            return JsonResult.success(count);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }
}