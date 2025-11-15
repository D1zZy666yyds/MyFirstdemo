package com.dzy666.demo.controller;

import com.dzy666.demo.entity.Category;
import com.dzy666.demo.service.CategoryService;
import com.dzy666.demo.util.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
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
            System.out.println("=== 收到创建分类请求 ===");
            System.out.println("分类名称: " + category.getName());
            System.out.println("用户ID: " + category.getUserId());
            System.out.println("父分类ID: " + category.getParentId());

            Category created = categoryService.createCategory(category);
            return JsonResult.success("分类创建成功", created);
        } catch (Exception e) {
            System.err.println("创建分类失败: " + e.getMessage());
            e.printStackTrace();
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
    // 在 CategoryController 中添加
    @PostMapping("/public-test")
    @ResponseBody
    public Map<String, Object> publicTest(@RequestBody Map<String, Object> requestData) {
        System.out.println("=== 公开测试接口被调用 ===");
        System.out.println("接收到的数据: " + requestData);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "公开测试接口正常工作");
        response.put("timestamp", new Date());
        response.put("receivedData", requestData);

        return response;
    }
}