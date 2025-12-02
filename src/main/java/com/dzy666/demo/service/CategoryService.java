package com.dzy666.demo.service;

import java.time.LocalDateTime;
import com.dzy666.demo.entity.Category;
import com.dzy666.demo.entity.Document; // 添加 Document 导入
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

    // 原有方法保持不变...
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

    // 🔄 新增方法 - 分类排序、移动、统计功能

    /**
     * 更新分类顺序
     */
    @Transactional
    public boolean updateCategoryOrder(List<Map<String, Object>> categoryOrders, Long userId) {
        try {
            for (Map<String, Object> order : categoryOrders) {
                Long categoryId = Long.valueOf(order.get("categoryId").toString());
                Integer sortOrder = Integer.valueOf(order.get("sortOrder").toString());

                // 更新分类排序
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

    /**
     * 移动分类到新的父分类
     */
    @Transactional
    public Category moveCategory(Long categoryId, Long newParentId, Long userId) {
        try {
            // 检查目标分类是否存在且属于同一用户
            Category targetCategory = categoryMapper.selectByIdAndUser(categoryId, userId);
            if (targetCategory == null) {
                throw new RuntimeException("分类不存在");
            }

            // 检查是否形成循环引用
            if (isCircularReference(categoryId, newParentId, userId)) {
                throw new RuntimeException("不能将分类移动到其子分类中");
            }

            // 更新父分类ID
            int result = categoryMapper.updateParentId(categoryId, userId, newParentId);
            if (result <= 0) {
                throw new RuntimeException("移动分类失败");
            }

            // 返回更新后的分类
            return categoryMapper.selectByIdAndUser(categoryId, userId);
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
     * 获取分类的所有子分类（包括子分类的子分类）
     */
    private List<Category> getAllChildren(Long parentId, Long userId) {
        List<Category> allChildren = new ArrayList<>();
        List<Category> directChildren = categoryMapper.selectByParentId(userId, parentId);

        for (Category child : directChildren) {
            allChildren.add(child);
            allChildren.addAll(getAllChildren(child.getId(), userId));
        }

        return allChildren;
    }

    /**
     * 获取分类统计信息
     */
    public Map<String, Object> getCategoryStatistics(Long userId) {
        Map<String, Object> stats = new HashMap<>();

        List<Category> categories = categoryMapper.selectByUserId(userId);

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
            int docCount = categoryMapper.countDocumentsInCategory(category.getId(), userId);
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

    /**
     * 计算分类树的最大深度
     */
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

    /**
     * 获取分类下的文档数量统计
     */
    public Map<String, Object> getCategoryDocumentCount(Long categoryId, Long userId) {
        Map<String, Object> countInfo = new HashMap<>();

        Category category = categoryMapper.selectByIdAndUser(categoryId, userId);
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

    /**
     * 递归计算子分类的文档数量
     */
    private int calculateChildDocumentCount(Long parentId, Long userId) {
        int total = 0;
        List<Category> children = categoryMapper.selectByParentId(userId, parentId);

        for (Category child : children) {
            // 直接文档数量
            total += categoryMapper.countDocumentsInCategory(child.getId(), userId);
            // 递归计算子分类的文档数量
            total += calculateChildDocumentCount(child.getId(), userId);
        }

        return total;
    }

    /**
     * 批量更新分类
     */
    @Transactional
    public boolean batchUpdateCategories(List<Category> categories, Long userId) {
        try {
            for (Category category : categories) {
                // 验证分类属于该用户
                Category existing = categoryMapper.selectByIdAndUser(category.getId(), userId);
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

    /**
     * 获取分类使用频率统计
     */
    public List<Map<String, Object>> getCategoryUsageFrequency(Long userId) {
        List<Category> categories = categoryMapper.selectByUserId(userId);

        return categories.stream()
                .map(category -> {
                    Map<String, Object> frequency = new HashMap<>();
                    frequency.put("categoryId", category.getId());
                    frequency.put("categoryName", category.getName());

                    int documentCount = categoryMapper.countDocumentsInCategory(category.getId(), userId);
                    frequency.put("documentCount", documentCount);

                    // 计算使用频率等级
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

    /**
     * 获取分类最后使用时间
     */
    private LocalDateTime getLastUsedTime(Long categoryId, Long userId) {
        // 这里可以查询该分类下文档的最新创建或修改时间
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
     * 搜索分类
     */
    public List<Category> searchCategories(String keyword, Long userId) {
        // 使用 Mapper 的搜索方法
        return categoryMapper.searchByName(keyword, userId);
    }
}