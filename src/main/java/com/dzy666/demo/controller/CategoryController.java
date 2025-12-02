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

    @PostMapping
    public JsonResult<Category> createCategory(@RequestBody Category category) {
        try {
            Category created = categoryService.createCategory(category);
            return JsonResult.success("分类创建成功", created);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

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

    @GetMapping("/user/{userId}")
    public JsonResult<List<Category>> getUserCategories(@PathVariable Long userId) {
        try {
            List<Category> categories = categoryService.getUserCategories(userId);
            return JsonResult.success(categories);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    @GetMapping("/user/{userId}/root")
    public JsonResult<List<Category>> getRootCategories(@PathVariable Long userId) {
        try {
            List<Category> categories = categoryService.getRootCategories(userId);
            return JsonResult.success(categories);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    @GetMapping("/user/{userId}/tree")
    public JsonResult<List<Category>> getCategoryTree(@PathVariable Long userId) {
        try {
            List<Category> categoryTree = categoryService.getCategoryTree(userId);
            return JsonResult.success(categoryTree);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

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
     * 更新分类顺序
     */
    @PutMapping("/order")
    public JsonResult<Boolean> updateCategoryOrder(@RequestBody Map<String, Object> orderRequest) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> categoryOrders = (List<Map<String, Object>>) orderRequest.get("categoryOrders");
            Long userId = Long.valueOf(orderRequest.get("userId").toString());

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
     * 搜索分类
     */
    @GetMapping("/search")
    public JsonResult<List<Category>> searchCategories(@RequestParam String keyword,
                                                       @RequestParam Long userId) {
        try {
            List<Category> categories = categoryService.searchCategories(keyword, userId);
            return JsonResult.success(categories);
        } catch (Exception e) {
            return JsonResult.error("搜索分类失败: " + e.getMessage());
        }
    }
}