package com.dzy666.demo.service;

import com.dzy666.demo.entity.Document;
import com.dzy666.demo.entity.Category;
import com.dzy666.demo.entity.Tag;
import com.dzy666.demo.mapper.FavoriteMapper;
import com.dzy666.demo.mapper.DocumentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class FavoriteService {

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private DocumentMapper documentMapper;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private TagService tagService;

    /**
     * 收藏文档
     */
    @Transactional
    public boolean addFavorite(Long documentId, Long userId) {
        // 检查文档是否存在且属于该用户
        Document document = documentMapper.selectByIdAndUser(documentId, userId);
        if (document == null) {
            throw new RuntimeException("文档不存在或无权访问");
        }

        // 检查是否已收藏
        if (favoriteMapper.exists(documentId, userId) > 0) {
            throw new RuntimeException("文档已收藏");
        }

        return favoriteMapper.insert(documentId, userId) > 0;
    }

    /**
     * 取消收藏
     */
    @Transactional
    public boolean removeFavorite(Long documentId, Long userId) {
        return favoriteMapper.delete(documentId, userId) > 0;
    }

    /**
     * 检查是否已收藏
     */
    public boolean isFavorite(Long documentId, Long userId) {
        return favoriteMapper.exists(documentId, userId) > 0;
    }

    /**
     * 获取用户的收藏文档列表（支持分类和标签筛选）
     */
    public List<Document> getUserFavorites(Long userId, Long categoryId, Long tagId) {
        // 获取用户的所有收藏文档ID
        List<Long> documentIds = favoriteMapper.selectFavoriteDocumentIds(userId);

        if (documentIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量查询所有收藏文档（性能优化）
        List<Document> allFavorites = documentMapper.selectByIdsAndUser(documentIds, userId);

        // 为每个文档加载标签信息
        allFavorites = allFavorites.stream()
                .peek(doc -> {
                    try {
                        // 加载文档的标签
                        List<Tag> tags = tagService.getDocumentTags(doc.getId(), userId);
                        doc.setTags(tags);

                        // 设置收藏状态
                        doc.setIsFavorite(true);

                        // 设置收藏数量
                        int favoriteCount = favoriteMapper.countByDocumentId(doc.getId());
                        doc.setFavoriteCount(favoriteCount);
                    } catch (Exception e) {
                        // 如果加载标签失败，设置为空列表
                        doc.setTags(new ArrayList<>());
                        System.err.println("为文档加载标签失败 (文档ID: " + doc.getId() + "): " + e.getMessage());
                    }
                })
                .collect(Collectors.toList());

        // 如果没有筛选条件，返回所有
        if (categoryId == null && tagId == null) {
            return allFavorites;
        }

        // 应用筛选
        return allFavorites.stream()
                .filter(doc -> {
                    boolean passCategory = true;
                    boolean passTag = true;

                    // 分类筛选
                    if (categoryId != null) {
                        passCategory = doc.getCategoryId() != null &&
                                doc.getCategoryId().equals(categoryId);
                    }

                    // 标签筛选
                    if (tagId != null) {
                        List<Tag> tags = doc.getTags();
                        if (tags == null || tags.isEmpty()) {
                            passTag = false;
                        } else {
                            passTag = tags.stream()
                                    .anyMatch(tag -> tag.getId().equals(tagId));
                        }
                    }

                    return passCategory && passTag;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取用户的收藏文档列表（简化版，无筛选）
     */
    public List<Document> getUserFavorites(Long userId) {
        return getUserFavorites(userId, null, null);
    }

    /**
     * 获取文档的收藏数量
     */
    public int getFavoriteCount(Long documentId) {
        return favoriteMapper.countByDocumentId(documentId);
    }

    /**
     * 获取用户收藏中的分类列表（仅包含有收藏的分类）
     */
    public List<Category> getFavoriteCategories(Long userId) {
        List<Long> documentIds = favoriteMapper.selectFavoriteDocumentIds(userId);

        if (documentIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取所有收藏文档的分类ID（去重）
        Set<Long> categoryIds = new HashSet<>();
        for (Long docId : documentIds) {
            try {
                Document doc = documentMapper.selectByIdAndUser(docId, userId);
                if (doc != null && doc.getCategoryId() != null) {
                    categoryIds.add(doc.getCategoryId());
                }
            } catch (Exception e) {
                // 文档可能已被删除，跳过
                System.err.println("获取收藏文档失败 (文档ID: " + docId + "): " + e.getMessage());
            }
        }

        // 获取分类详情
        List<Category> categories = new ArrayList<>();
        for (Long categoryId : categoryIds) {
            try {
                Category category = categoryService.getCategoryById(categoryId, userId);
                if (category != null) {
                    // 计算该分类下有多少收藏文档
                    int favoriteDocCount = countFavoriteDocumentsInCategory(userId, categoryId);
                    category.setDocumentCount(favoriteDocCount);
                    categories.add(category);
                }
            } catch (Exception e) {
                // 分类可能已被删除，跳过
                System.err.println("获取收藏分类失败 (分类ID: " + categoryId + "): " + e.getMessage());
            }
        }

        // 按文档数量排序
        return categories.stream()
                .sorted((c1, c2) -> Integer.compare(
                        c2.getDocumentCount() != null ? c2.getDocumentCount() : 0,
                        c1.getDocumentCount() != null ? c1.getDocumentCount() : 0
                ))
                .collect(Collectors.toList());
    }

    /**
     * 统计某个分类下的收藏文档数量
     */
    private int countFavoriteDocumentsInCategory(Long userId, Long categoryId) {
        List<Long> favoriteDocIds = favoriteMapper.selectFavoriteDocumentIds(userId);
        if (favoriteDocIds.isEmpty()) {
            return 0;
        }

        // 查询该分类下的所有文档
        List<Document> categoryDocs = documentMapper.selectByCategoryIdAndUser(categoryId, userId);

        // 统计收藏的文档数量
        Set<Long> favoriteDocIdSet = new HashSet<>(favoriteDocIds);
        return (int) categoryDocs.stream()
                .filter(doc -> favoriteDocIdSet.contains(doc.getId()))
                .count();
    }

    /**
     * 获取用户收藏中的标签列表（仅包含有收藏的标签）
     */
    public List<Tag> getFavoriteTags(Long userId) {
        List<Long> documentIds = favoriteMapper.selectFavoriteDocumentIds(userId);

        if (documentIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 收集所有标签及其使用次数
        Map<Long, Tag> tagMap = new HashMap<>();

        for (Long docId : documentIds) {
            try {
                List<Tag> tags = tagService.getDocumentTags(docId, userId);
                if (tags != null) {
                    for (Tag tag : tags) {
                        Tag existingTag = tagMap.get(tag.getId());
                        if (existingTag == null) {
                            // 设置标签使用次数
                            tag.setDocumentCount(1);
                            tagMap.put(tag.getId(), tag);
                        } else {
                            // 增加使用次数
                            existingTag.setDocumentCount(
                                    existingTag.getDocumentCount() + 1
                            );
                        }
                    }
                }
            } catch (Exception e) {
                // 标签可能加载失败，跳过
                System.err.println("获取文档标签失败 (文档ID: " + docId + "): " + e.getMessage());
            }
        }

        // 转换为列表并按使用次数排序
        return tagMap.values().stream()
                .sorted((t1, t2) -> Integer.compare(
                        t2.getDocumentCount() != null ? t2.getDocumentCount() : 0,
                        t1.getDocumentCount() != null ? t1.getDocumentCount() : 0
                ))
                .collect(Collectors.toList());
    }

    /**
     * 获取收藏的统计信息
     */
    public Map<String, Object> getFavoriteStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();

        // 基础统计
        int totalFavorites = favoriteMapper.countByUserId(userId);
        List<Category> favoriteCategories = getFavoriteCategories(userId);
        List<Tag> favoriteTags = getFavoriteTags(userId);

        stats.put("totalCount", totalFavorites);
        stats.put("categoryCount", favoriteCategories.size());
        stats.put("tagCount", favoriteTags.size());

        // 按分类统计
        Map<String, Integer> categoryStats = new HashMap<>();
        for (Category category : favoriteCategories) {
            categoryStats.put(category.getName(), category.getDocumentCount());
        }
        stats.put("categoryStats", categoryStats);

        // 按标签统计
        Map<String, Integer> tagStats = new HashMap<>();
        for (Tag tag : favoriteTags) {
            tagStats.put(tag.getName(), tag.getDocumentCount());
        }
        stats.put("tagStats", tagStats);

        // 最近收藏（最近10个）
        List<Long> recentFavoriteIds = favoriteMapper.selectFavoriteDocumentIds(userId);
        List<Document> recentFavorites = new ArrayList<>();
        int count = 0;
        for (Long docId : recentFavoriteIds) {
            if (count >= 10) break;
            try {
                Document doc = documentMapper.selectByIdAndUser(docId, userId);
                if (doc != null) {
                    recentFavorites.add(doc);
                    count++;
                }
            } catch (Exception e) {
                // 跳过无效文档
            }
        }
        stats.put("recentFavorites", recentFavorites);

        return stats;
    }

    /**
     * 批量检查文档收藏状态（性能优化）
     */
    public Map<Long, Boolean> batchCheckFavoriteStatus(List<Long> documentIds, Long userId) {
        Map<Long, Boolean> result = new HashMap<>();

        if (documentIds == null || documentIds.isEmpty()) {
            return result;
        }

        // 获取用户的所有收藏ID
        List<Long> favoriteDocIds = favoriteMapper.selectFavoriteDocumentIds(userId);
        Set<Long> favoriteIdSet = new HashSet<>(favoriteDocIds);

        // 批量检查
        for (Long docId : documentIds) {
            result.put(docId, favoriteIdSet.contains(docId));
        }

        return result;
    }

    /**
     * 获取用户最常收藏的文档（热门收藏）
     */
    public List<Document> getHotFavorites(Long userId, int limit) {
        // 先获取用户的收藏文档
        List<Document> favorites = getUserFavorites(userId);

        // 按收藏次数排序（从全局收藏次数排序）
        return favorites.stream()
                .sorted((d1, d2) -> {
                    int count1 = favoriteMapper.countByDocumentId(d1.getId());
                    int count2 = favoriteMapper.countByDocumentId(d2.getId());
                    return Integer.compare(count2, count1);
                })
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 检查文档是否被当前用户收藏，并更新文档的收藏信息
     */
    public Document enrichDocumentWithFavoriteInfo(Document document, Long userId) {
        if (document == null || userId == null) {
            return document;
        }

        try {
            // 检查收藏状态
            boolean isFavorite = isFavorite(document.getId(), userId);
            document.setIsFavorite(isFavorite);

            // 设置收藏数量
            int favoriteCount = getFavoriteCount(document.getId());
            document.setFavoriteCount(favoriteCount);

        } catch (Exception e) {
            // 如果查询失败，设置为默认值
            document.setIsFavorite(false);
            document.setFavoriteCount(0);
            System.err.println("更新文档收藏信息失败: " + e.getMessage());
        }

        return document;
    }

    /**
     * 批量更新文档的收藏信息
     */
    public List<Document> batchEnrichDocumentsWithFavoriteInfo(List<Document> documents, Long userId) {
        if (documents == null || documents.isEmpty() || userId == null) {
            return documents;
        }

        // 获取文档ID列表
        List<Long> docIds = documents.stream()
                .map(Document::getId)
                .collect(Collectors.toList());

        // 批量检查收藏状态
        Map<Long, Boolean> favoriteStatus = batchCheckFavoriteStatus(docIds, userId);

        // 为每个文档设置收藏信息
        return documents.stream()
                .peek(doc -> {
                    Boolean isFavorite = favoriteStatus.get(doc.getId());
                    if (isFavorite != null) {
                        doc.setIsFavorite(isFavorite);
                    } else {
                        doc.setIsFavorite(false);
                    }

                    // 设置收藏数量
                    try {
                        int favoriteCount = getFavoriteCount(doc.getId());
                        doc.setFavoriteCount(favoriteCount);
                    } catch (Exception e) {
                        doc.setFavoriteCount(0);
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取分类下的收藏文档数量统计
     */
    public Map<Long, Integer> getCategoryFavoriteCounts(Long userId) {
        List<Category> categories = categoryService.getUserCategories(userId);
        Map<Long, Integer> result = new HashMap<>();

        for (Category category : categories) {
            int count = countFavoriteDocumentsInCategory(userId, category.getId());
            result.put(category.getId(), count);
        }

        return result;
    }

    /**
     * 搜索收藏文档
     */
    public List<Document> searchFavoriteDocuments(Long userId, String keyword, Long categoryId, Long tagId) {
        // 先获取用户的收藏文档
        List<Document> favorites = getUserFavorites(userId, categoryId, tagId);

        if (keyword == null || keyword.trim().isEmpty()) {
            return favorites;
        }

        String keywordLower = keyword.toLowerCase().trim();

        // 在本地进行搜索
        return favorites.stream()
                .filter(doc -> {
                    boolean matchTitle = doc.getTitle() != null &&
                            doc.getTitle().toLowerCase().contains(keywordLower);
                    boolean matchContent = doc.getContent() != null &&
                            doc.getContent().toLowerCase().contains(keywordLower);

                    // 检查标签匹配
                    boolean matchTag = false;
                    if (doc.getTags() != null) {
                        matchTag = doc.getTags().stream()
                                .anyMatch(tag -> tag.getName() != null &&
                                        tag.getName().toLowerCase().contains(keywordLower));
                    }

                    return matchTitle || matchContent || matchTag;
                })
                .collect(Collectors.toList());
    }
}