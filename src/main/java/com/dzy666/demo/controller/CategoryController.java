package com.dzy666.demo.controller;

import com.dzy666.demo.entity.Category;
import com.dzy666.demo.service.CategoryService;
import com.dzy666.demo.util.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 创建分类
     */
    @PostMapping
    public JsonResult<Category> createCategory(@RequestBody Category category) {
        try {
            Category created = categoryService.createCategory(category);
            return JsonResult.success("分类创建成功", created);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    /**
     * 根据ID获取分类
     */
    @GetMapping("/{id}")
    public JsonResult<Category> getCategory(@PathVariable Long id,
                                            @RequestParam Long userId) {
        try {
            Category category = categoryService.getCategoryById(id, userId);
            if (category == null) {
                return JsonResult.error("分类不存在");
            }
            return JsonResult.success(category);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    /**
     * 获取用户所有分类（扁平列表，带文档数量）
     */
    @GetMapping("/user/{userId}")
    public JsonResult<List<Category>> getUserCategories(@PathVariable Long userId) {
        try {
            List<Category> categories = categoryService.getUserCategories(userId);
            return JsonResult.success(categories);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    /**
     * 获取用户根分类（带文档数量）
     */
    @GetMapping("/user/{userId}/root")
    public JsonResult<List<Category>> getRootCategories(@PathVariable Long userId) {
        try {
            List<Category> categories = categoryService.getRootCategories(userId);
            return JsonResult.success(categories);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    /**
     * 获取分类树（带文档数量，优化版本）
     */
    @GetMapping("/user/{userId}/tree")
    public JsonResult<List<Category>> getCategoryTree(@PathVariable Long userId) {
        try {
            List<Category> categoryTree = categoryService.getCategoryTreeOptimized(userId);
            return JsonResult.success(categoryTree);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    /**
     * 获取子分类（带文档数量）
     */
    @GetMapping("/user/{userId}/parent/{parentId}")
    public JsonResult<List<Category>> getChildCategories(@PathVariable Long userId,
                                                         @PathVariable Long parentId) {
        try {
            List<Category> categories = categoryService.getChildCategories(userId, parentId);
            return JsonResult.success(categories);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    /**
     * 更新分类
     */
    @PutMapping("/{id}")
    public JsonResult<Category> updateCategory(@PathVariable Long id,
                                               @RequestBody Category category) {
        try {
            category.setId(id);
            Category updated = categoryService.updateCategory(category);
            return JsonResult.success("分类更新成功", updated);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    /**
     * 删除单个分类
     */
    @DeleteMapping("/{id}")
    public JsonResult<Boolean> deleteCategory(@PathVariable Long id,
                                              @RequestParam Long userId) {
        try {
            boolean success = categoryService.deleteCategory(id, userId);
            return JsonResult.success(success ? "删除成功" : "删除失败", success);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    /**
     * 批量删除分类
     */
    @DeleteMapping("/batch")
    public JsonResult<Boolean> batchDeleteCategories(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> categoryIds = (List<Long>) request.get("categoryIds");
            Long userId = Long.valueOf(request.get("userId").toString());

            // 验证参数
            if (categoryIds == null || categoryIds.isEmpty()) {
                return JsonResult.error("请选择要删除的分类");
            }

            boolean success = categoryService.batchDeleteCategories(categoryIds, userId);
            return JsonResult.success(success ? "批量删除成功" : "批量删除失败", success);
        } catch (Exception e) {
            return JsonResult.error("批量删除失败: " + e.getMessage());
        }
    }

    /**
     * 更新分类顺序
     */
    @PutMapping("/order")
    public JsonResult<Boolean> updateCategoryOrder(@RequestBody Map<String, Object> orderRequest) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> categoryOrders = (List<Map<String, Object>>) orderRequest.get("categoryOrders");
            Long userId = Long.valueOf(orderRequest.get("userId").toString());

            // 验证参数
            if (categoryOrders == null || categoryOrders.isEmpty()) {
                return JsonResult.error("请提供要排序的分类");
            }

            boolean success = categoryService.updateCategoryOrder(categoryOrders, userId);
            return JsonResult.success(success ? "分类顺序更新成功" : "更新失败", success);
        } catch (Exception e) {
            return JsonResult.error("分类顺序更新失败: " + e.getMessage());
        }
    }

    /**
     * 移动分类到新的父分类
     */
    @PutMapping("/{categoryId}/move")
    public JsonResult<Category> moveCategory(@PathVariable Long categoryId,
                                             @RequestParam Long newParentId,
                                             @RequestParam Long userId) {
        try {
            Category movedCategory = categoryService.moveCategory(categoryId, newParentId, userId);
            return JsonResult.success("分类移动成功", movedCategory);
        } catch (Exception e) {
            return JsonResult.error("分类移动失败: " + e.getMessage());
        }
    }

    /**
     * 批量移动分类
     */
    @PostMapping("/batch/move")
    public JsonResult<Boolean> batchMoveCategories(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> categoryIds = (List<Long>) request.get("categoryIds");
            Long newParentId = request.get("newParentId") == null ?
                    null : Long.valueOf(request.get("newParentId").toString());
            Long userId = Long.valueOf(request.get("userId").toString());

            // 验证参数
            if (categoryIds == null || categoryIds.isEmpty()) {
                return JsonResult.error("请选择要移动的分类");
            }

            boolean success = categoryService.batchMoveCategories(categoryIds, newParentId, userId);
            return JsonResult.success(success ? "批量移动成功" : "批量移动失败", success);
        } catch (Exception e) {
            return JsonResult.error("批量移动失败: " + e.getMessage());
        }
    }

    /**
     * 获取分类统计信息
     */
    @GetMapping("/stats/{userId}")
    public JsonResult<Map<String, Object>> getCategoryStats(@PathVariable Long userId) {
        try {
            Map<String, Object> stats = categoryService.getCategoryStatistics(userId);
            return JsonResult.success(stats);
        } catch (Exception e) {
            return JsonResult.error("获取分类统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取分类下的文档数量统计
     */
    @GetMapping("/{categoryId}/documents/count")
    public JsonResult<Map<String, Object>> getCategoryDocumentCount(@PathVariable Long categoryId,
                                                                    @RequestParam Long userId) {
        try {
            Map<String, Object> countInfo = categoryService.getCategoryDocumentCount(categoryId, userId);
            return JsonResult.success(countInfo);
        } catch (Exception e) {
            return JsonResult.error("获取分类文档数量失败: " + e.getMessage());
        }
    }

    /**
     * 搜索分类（带文档数量）
     */
    @GetMapping("/search")
    public JsonResult<List<Category>> searchCategories(@RequestParam String keyword,
                                                       @RequestParam Long userId) {
        try {
            // 验证参数
            if (keyword == null || keyword.trim().isEmpty()) {
                return JsonResult.error("请输入搜索关键词");
            }

            List<Category> categories = categoryService.searchCategories(keyword.trim(), userId);
            return JsonResult.success(categories);
        } catch (Exception e) {
            return JsonResult.error("搜索分类失败: " + e.getMessage());
        }
    }

    /**
     * 获取分类使用频率统计
     */
    @GetMapping("/usage/frequency/{userId}")
    public JsonResult<List<Map<String, Object>>> getCategoryUsageFrequency(@PathVariable Long userId) {
        try {
            List<Map<String, Object>> frequencyList = categoryService.getCategoryUsageFrequency(userId);
            return JsonResult.success(frequencyList);
        } catch (Exception e) {
            return JsonResult.error("获取分类使用频率失败: " + e.getMessage());
        }
    }

    /**
     * 批量更新分类
     */
    @PutMapping("/batch")
    public JsonResult<Boolean> batchUpdateCategories(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Category> categories = (List<Category>) request.get("categories");
            Long userId = Long.valueOf(request.get("userId").toString());

            // 验证参数
            if (categories == null || categories.isEmpty()) {
                return JsonResult.error("请提供要更新的分类");
            }

            boolean success = categoryService.batchUpdateCategories(categories, userId);
            return JsonResult.success(success ? "批量更新成功" : "批量更新失败", success);
        } catch (Exception e) {
            return JsonResult.error("批量更新失败: " + e.getMessage());
        }
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public JsonResult<String> healthCheck() {
        return JsonResult.success("分类服务运行正常");
    }
}