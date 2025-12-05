package com.dzy666.demo.service;

import java.time.LocalDateTime;
import com.dzy666.demo.entity.Category;
import com.dzy666.demo.entity.Document;
import com.dzy666.demo.mapper.CategoryMapper;
import com.dzy666.demo.mapper.DocumentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

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

            // 设置创建时间
            category.setCreatedTime(LocalDateTime.now());
            System.out.println("设置创建时间: " + category.getCreatedTime());

            System.out.println("准备插入数据库...");

            // 插入数据库
            int result = categoryMapper.insert(category);
            System.out.println("数据库插入结果: " + result + " 行受影响");

            if (result > 0) {
                System.out.println("分类创建成功，生成的ID: " + category.getId());

                // 重新从数据库查询以确保数据完整
                Category savedCategory = getCategoryById(category.getId(), category.getUserId());
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
        // 获取基础分类信息
        Category category = categoryMapper.selectByIdAndUser(id, userId);
        if (category != null) {
            // 设置文档数量
            int docCount = categoryMapper.countDocumentsInCategory(id, userId);
            category.setDocumentCount(docCount);
        }
        return category;
    }

    public List<Category> getUserCategories(Long userId) {
        // 使用带文档数量的查询方法
        return categoryMapper.selectWithDocumentCount(userId);
    }

    public List<Category> getRootCategories(Long userId) {
        // 获取根分类并设置文档数量
        List<Category> rootCategories = categoryMapper.selectRootCategories(userId);
        for (Category category : rootCategories) {
            int docCount = categoryMapper.countDocumentsInCategory(category.getId(), userId);
            category.setDocumentCount(docCount);
        }
        return rootCategories;
    }

    public List<Category> getChildCategories(Long userId, Long parentId) {
        // 获取子分类并设置文档数量
        List<Category> childCategories = categoryMapper.selectByParentId(userId, parentId);
        for (Category category : childCategories) {
            int docCount = categoryMapper.countDocumentsInCategory(category.getId(), userId);
            category.setDocumentCount(docCount);
        }
        return childCategories;
    }

    public Category updateCategory(Category category) {
        categoryMapper.update(category);
        // 返回更新后的分类，包含文档数量
        return getCategoryById(category.getId(), category.getUserId());
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
     * 构建分类树形结构（带文档数量）
     */
    public List<Category> getCategoryTree(Long userId) {
        // 获取所有分类（带文档数量）
        List<Category> allCategories = getUserCategories(userId);

        // 创建ID到分类的映射
        Map<Long, Category> categoryMap = new HashMap<>();
        List<Category> rootCategories = new ArrayList<>();

        // 第一遍：建立映射并找到根分类
        for (Category category : allCategories) {
            categoryMap.put(category.getId(), category);
            if (category.getParentId() == null) {
                rootCategories.add(category);
            }
        }

        // 第二遍：建立树形结构
        for (Category category : allCategories) {
            if (category.getParentId() != null && categoryMap.containsKey(category.getParentId())) {
                Category parent = categoryMap.get(category.getParentId());
                if (parent.getChildren() == null) {
                    parent.setChildren(new ArrayList<>());
                }
                parent.getChildren().add(category);
            }
        }

        return rootCategories;
    }

    /**
     * 递归构建分类树（旧方法，保持兼容性）
     */
    private void buildCategoryTree(Category parentCategory, Long userId) {
        List<Category> children = getChildCategories(userId, parentCategory.getId());
        parentCategory.setChildren(children);

        for (Category child : children) {
            buildCategoryTree(child, userId);
        }
    }

    // 🔄 分类排序、移动、统计功能

    @Transactional
    public boolean updateCategoryOrder(List<Map<String, Object>> categoryOrders, Long userId) {
        try {
            for (Map<String, Object> order : categoryOrders) {
                Long categoryId = Long.valueOf(order.get("categoryId").toString());
                Integer sortOrder = Integer.valueOf(order.get("sortOrder").toString());

                int result = categoryMapper.updateSortOrder(categoryId, userId, sortOrder);
                if (result <= 0) {
                    throw new RuntimeException("更新分类排序失败: " + categoryId);
                }
            }
            return true;
        } catch (Exception e) {
            throw new RuntimeException("更新分类顺序失败: " + e.getMessage(), e);
        }
    }

    @Transactional
    public Category moveCategory(Long categoryId, Long newParentId, Long userId) {
        try {
            Category targetCategory = getCategoryById(categoryId, userId);
            if (targetCategory == null) {
                throw new RuntimeException("分类不存在");
            }

            // 检查是否形成循环引用
            if (isCircularReference(categoryId, newParentId, userId)) {
                throw new RuntimeException("不能将分类移动到其子分类中");
            }

            // 检查是否尝试移动到自己的子分类
            if (newParentId != null && isParentOf(categoryId, newParentId, userId)) {
                throw new RuntimeException("不能将分类移动到自己的子分类中");
            }

            int result = categoryMapper.updateParentId(categoryId, userId, newParentId);
            if (result <= 0) {
                throw new RuntimeException("移动分类失败");
            }

            return getCategoryById(categoryId, userId);
        } catch (Exception e) {
            throw new RuntimeException("移动分类失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检查是否形成循环引用
     */
    private boolean isCircularReference(Long categoryId, Long newParentId, Long userId) {
        if (newParentId == null) {
            return false; // 移动到根目录不会形成循环
        }

        if (categoryId.equals(newParentId)) {
            return true; // 不能将自己设为自己的父分类
        }

        // 检查新父分类是否在目标分类的子分类中
        List<Category> children = getAllChildren(categoryId, userId);
        return children.stream().anyMatch(child -> child.getId().equals(newParentId));
    }

    /**
     * 检查一个分类是否是另一个分类的父分类
     */
    private boolean isParentOf(Long parentId, Long childId, Long userId) {
        Category current = getCategoryById(childId, userId);
        while (current != null && current.getParentId() != null) {
            if (current.getParentId().equals(parentId)) {
                return true;
            }
            current = getCategoryById(current.getParentId(), userId);
        }
        return false;
    }

    private List<Category> getAllChildren(Long parentId, Long userId) {
        List<Category> allChildren = new ArrayList<>();
        List<Category> directChildren = getChildCategories(userId, parentId);

        for (Category child : directChildren) {
            allChildren.add(child);
            allChildren.addAll(getAllChildren(child.getId(), userId));
        }

        return allChildren;
    }

    public Map<String, Object> getCategoryStatistics(Long userId) {
        Map<String, Object> stats = new HashMap<>();

        List<Category> categories = getUserCategories(userId);

        // 基本统计
        stats.put("totalCategories", categories.size());
        stats.put("rootCategories", getRootCategories(userId).size());

        // 深度统计
        int maxDepth = calculateMaxDepth(categories);
        stats.put("maxDepth", maxDepth);

        // 文档分布统计
        Map<String, Integer> documentDistribution = new HashMap<>();
        int totalDocuments = 0;

        for (Category category : categories) {
            int docCount = category.getDocumentCount() != null ? category.getDocumentCount() : 0;
            documentDistribution.put(category.getName(), docCount);
            totalDocuments += docCount;
        }

        stats.put("documentDistribution", documentDistribution);
        stats.put("totalDocumentsInCategories", totalDocuments);

        // 未分类文档数量
        int uncategorizedCount = documentMapper.selectByUserId(userId).stream()
                .filter(doc -> doc.getCategoryId() == null)
                .collect(Collectors.toList())
                .size();
        stats.put("uncategorizedDocuments", uncategorizedCount);

        return stats;
    }

    private int calculateMaxDepth(List<Category> categories) {
        int maxDepth = 0;
        for (Category category : categories) {
            if (category.getParentId() == null) {
                int depth = calculateDepth(category, categories, 1);
                maxDepth = Math.max(maxDepth, depth);
            }
        }
        return maxDepth;
    }

    private int calculateDepth(Category category, List<Category> allCategories, int currentDepth) {
        int maxChildDepth = currentDepth;
        List<Category> children = allCategories.stream()
                .filter(c -> category.getId().equals(c.getParentId()))
                .collect(Collectors.toList());

        for (Category child : children) {
            int childDepth = calculateDepth(child, allCategories, currentDepth + 1);
            maxChildDepth = Math.max(maxChildDepth, childDepth);
        }

        return maxChildDepth;
    }

    public Map<String, Object> getCategoryDocumentCount(Long categoryId, Long userId) {
        Map<String, Object> countInfo = new HashMap<>();

        Category category = getCategoryById(categoryId, userId);
        if (category == null) {
            throw new RuntimeException("分类不存在");
        }

        // 直接文档数量
        int directCount = categoryMapper.countDocumentsInCategory(categoryId, userId);
        countInfo.put("directDocumentCount", directCount);

        // 子分类文档数量（递归计算）
        int childDocumentCount = calculateChildDocumentCount(categoryId, userId);
        countInfo.put("childDocumentCount", childDocumentCount);
        countInfo.put("totalDocumentCount", directCount + childDocumentCount);

        countInfo.put("categoryName", category.getName());

        return countInfo;
    }

    private int calculateChildDocumentCount(Long parentId, Long userId) {
        int total = 0;
        List<Category> children = getChildCategories(userId, parentId);

        for (Category child : children) {
            total += child.getDocumentCount() != null ? child.getDocumentCount() : 0;
            total += calculateChildDocumentCount(child.getId(), userId);
        }

        return total;
    }

    @Transactional
    public boolean batchUpdateCategories(List<Category> categories, Long userId) {
        try {
            for (Category category : categories) {
                Category existing = getCategoryById(category.getId(), userId);
                if (existing == null) {
                    throw new RuntimeException("分类不存在或无权访问: " + category.getId());
                }

                int result = categoryMapper.update(category);
                if (result <= 0) {
                    throw new RuntimeException("更新分类失败: " + category.getId());
                }
            }
            return true;
        } catch (Exception e) {
            throw new RuntimeException("批量更新分类失败: " + e.getMessage(), e);
        }
    }

    public List<Map<String, Object>> getCategoryUsageFrequency(Long userId) {
        List<Category> categories = getUserCategories(userId);

        return categories.stream()
                .map(category -> {
                    Map<String, Object> frequency = new HashMap<>();
                    frequency.put("categoryId", category.getId());
                    frequency.put("categoryName", category.getName());

                    int documentCount = category.getDocumentCount() != null ? category.getDocumentCount() : 0;
                    frequency.put("documentCount", documentCount);

                    String frequencyLevel;
                    if (documentCount >= 10) frequencyLevel = "高频";
                    else if (documentCount >= 5) frequencyLevel = "中频";
                    else if (documentCount >= 1) frequencyLevel = "低频";
                    else frequencyLevel = "未使用";

                    frequency.put("frequencyLevel", frequencyLevel);
                    frequency.put("lastUsed", getLastUsedTime(category.getId(), userId));

                    return frequency;
                })
                .sorted((a, b) -> Integer.compare(
                        (Integer) b.get("documentCount"),
                        (Integer) a.get("documentCount")
                ))
                .collect(Collectors.toList());
    }

    private LocalDateTime getLastUsedTime(Long categoryId, Long userId) {
        List<Document> documents = documentMapper.selectByCategoryIdAndUser(categoryId, userId);
        if (documents == null || documents.isEmpty()) {
            return null;
        }
        return documents.stream()
                .map(doc -> doc.getUpdatedTime() != null ? doc.getUpdatedTime() : doc.getCreatedTime())
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }

    /**
     * 搜索分类（带文档数量）
     */
    public List<Category> searchCategories(String keyword, Long userId) {
        List<Category> categories = categoryMapper.searchByName(keyword, userId);
        // 为搜索结果设置文档数量
        for (Category category : categories) {
            int docCount = categoryMapper.countDocumentsInCategory(category.getId(), userId);
            category.setDocumentCount(docCount);
        }
        return categories;
    }

    @Transactional
    public boolean batchDeleteCategories(List<Long> categoryIds, Long userId) {
        try {
            int successCount = 0;
            List<String> errorMessages = new ArrayList<>();

            for (Long categoryId : categoryIds) {
                try {
                    boolean deleted = deleteCategory(categoryId, userId);
                    if (deleted) {
                        successCount++;
                    }
                } catch (Exception e) {
                    Category category = getCategoryById(categoryId, userId);
                    String categoryName = category != null ? category.getName() : "未知分类";
                    errorMessages.add(categoryName + ": " + e.getMessage());
                }
            }

            if (successCount > 0 && errorMessages.isEmpty()) {
                return true;
            } else if (successCount > 0) {
                String errorMsg = "成功删除 " + successCount + " 个分类，失败 " + errorMessages.size() + " 个";
                throw new RuntimeException(errorMsg + "。详情: " + String.join("; ", errorMessages));
            } else {
                throw new RuntimeException("删除全部失败: " + String.join("; ", errorMessages));
            }
        } catch (Exception e) {
            throw new RuntimeException("批量删除分类失败: " + e.getMessage(), e);
        }
    }

    @Transactional
    public boolean batchMoveCategories(List<Long> categoryIds, Long newParentId, Long userId) {
        try {
            int successCount = 0;
            List<String> errorMessages = new ArrayList<>();

            for (Long categoryId : categoryIds) {
                try {
                    Category movedCategory = moveCategory(categoryId, newParentId, userId);
                    if (movedCategory != null) {
                        successCount++;
                    }
                } catch (Exception e) {
                    Category category = getCategoryById(categoryId, userId);
                    String categoryName = category != null ? category.getName() : "未知分类";
                    errorMessages.add(categoryName + ": " + e.getMessage());
                }
            }

            if (successCount > 0 && errorMessages.isEmpty()) {
                return true;
            } else if (successCount > 0) {
                String errorMsg = "成功移动 " + successCount + " 个分类，失败 " + errorMessages.size() + " 个";
                throw new RuntimeException(errorMsg + "。详情: " + String.join("; ", errorMessages));
            } else {
                throw new RuntimeException("移动全部失败: " + String.join("; ", errorMessages));
            }
        } catch (Exception e) {
            throw new RuntimeException("批量移动分类失败: " + e.getMessage(), e);
        }
    }

    // 新增：获取分类树的高效方法（使用一次查询）
    public List<Category> getCategoryTreeOptimized(Long userId) {
        // 获取所有分类（带文档数量）
        List<Category> allCategories = getUserCategories(userId);

        // 创建ID到分类的映射
        Map<Long, Category> categoryMap = new HashMap<>();
        List<Category> rootCategories = new ArrayList<>();

        // 第一遍：建立映射并找到根分类
        for (Category category : allCategories) {
            categoryMap.put(category.getId(), category);
            if (category.getParentId() == null) {
                rootCategories.add(category);
            } else {
                // 确保子分类的children列表被初始化
                category.setChildren(new ArrayList<>());
            }
        }

        // 第二遍：建立树形结构
        for (Category category : allCategories) {
            if (category.getParentId() != null && categoryMap.containsKey(category.getParentId())) {
                Category parent = categoryMap.get(category.getParentId());
                if (parent.getChildren() == null) {
                    parent.setChildren(new ArrayList<>());
                }
                parent.getChildren().add(category);
            }
        }

        return rootCategories;
    }
}