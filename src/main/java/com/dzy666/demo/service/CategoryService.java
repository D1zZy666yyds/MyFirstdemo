package com.dzy666.demo.service;
import java.time.LocalDateTime;
import com.dzy666.demo.entity.Category;
import com.dzy666.demo.mapper.CategoryMapper;
import com.dzy666.demo.mapper.DocumentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Service
public class CategoryService {

    private final CategoryMapper categoryMapper;
    private final DocumentMapper documentMapper;

    public CategoryService(CategoryMapper categoryMapper, DocumentMapper documentMapper) {
        this.categoryMapper = categoryMapper;
        this.documentMapper = documentMapper;
    }
    @Transactional
    public Category createCategory(Category category) {
        try {
            System.out.println("=== CategoryService.createCategory 开始 ===");
            System.out.println("接收到的分类数据: " + category);

            // 验证必要字段
            if (category.getName() == null || category.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("分类名称不能为空");
            }
            if (category.getUserId() == null) {
                throw new IllegalArgumentException("用户ID不能为空");
            }

            // 设置默认排序值
            if (category.getSortOrder() == null) {
                category.setSortOrder(0);
                System.out.println("设置默认排序值: 0");
            }

            // 设置创建时间 - 使用 LocalDateTime.now()
            category.setCreatedTime(LocalDateTime.now());
            System.out.println("设置创建时间: " + category.getCreatedTime());

            System.out.println("准备插入数据库...");

            // 插入数据库
            int result = categoryMapper.insert(category);
            System.out.println("数据库插入结果: " + result + " 行受影响");

            if (result > 0) {
                System.out.println("分类创建成功，生成的ID: " + category.getId());

                // 重新从数据库查询以确保数据完整
                Category savedCategory = categoryMapper.selectByIdAndUser(category.getId(), category.getUserId());
                System.out.println("从数据库查询到的分类: " + savedCategory);

                System.out.println("=== CategoryService.createCategory 完成 ===");
                return savedCategory;
            } else {
                throw new RuntimeException("数据库插入失败，没有行受影响");
            }

        } catch (Exception e) {
            System.err.println("=== CategoryService.createCategory 异常 ===");
            System.err.println("错误信息: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("创建分类失败: " + e.getMessage(), e);
        }
    }

    public Category getCategoryById(Long id, Long userId) {
        return categoryMapper.selectByIdAndUser(id, userId);
    }

    public List<Category> getUserCategories(Long userId) {
        return categoryMapper.selectByUserId(userId);
    }

    public List<Category> getRootCategories(Long userId) {
        return categoryMapper.selectRootCategories(userId);
    }

    public List<Category> getChildCategories(Long userId, Long parentId) {
        return categoryMapper.selectByParentId(userId, parentId);
    }

    public Category updateCategory(Category category) {
        categoryMapper.update(category);
        return categoryMapper.selectByIdAndUser(category.getId(), category.getUserId());
    }

    public boolean deleteCategory(Long id, Long userId) {
        // 检查是否有子分类
        int childCount = categoryMapper.countChildren(id, userId);
        if (childCount > 0) {
            throw new RuntimeException("该分类下存在子分类，无法删除");
        }

        // 检查是否有文档
        int docCount = categoryMapper.countDocumentsInCategory(id, userId);
        if (docCount > 0) {
            throw new RuntimeException("该分类下存在文档，无法删除");
        }

        return categoryMapper.deleteByIdAndUser(id, userId) > 0;
    }

    /**
     * 构建分类树形结构
     */
    public List<Category> getCategoryTree(Long userId) {
        List<Category> rootCategories = getRootCategories(userId);
        for (Category rootCategory : rootCategories) {
            buildCategoryTree(rootCategory, userId);
        }
        return rootCategories;
    }

    private void buildCategoryTree(Category parentCategory, Long userId) {
        List<Category> children = getChildCategories(userId, parentCategory.getId());
        parentCategory.setChildren(children);

        for (Category child : children) {
            buildCategoryTree(child, userId);
        }
    }
}