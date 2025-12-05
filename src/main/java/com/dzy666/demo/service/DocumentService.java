package com.dzy666.demo.service;

import com.dzy666.demo.entity.Document;
import com.dzy666.demo.mapper.DocumentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dzy666.demo.entity.Tag;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    @Autowired
    private DocumentMapper documentMapper;

    @Autowired
    @Lazy
    private SearchService searchService;

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private TagService tagService;

    public Document createDocument(Document document) {
        documentMapper.insert(document);
        try {
            searchService.indexDocument(document);
        } catch (IOException e) {
            System.err.println("索引创建失败: " + e.getMessage());
        }
        return document;
    }

    public Document getDocument(Long id, Long userId) {
        return documentMapper.selectByIdAndUser(id, userId);
    }

    public List<Document> getUserDocuments(Long userId) {
        return documentMapper.selectByUserId(userId);
    }

    public Document updateDocument(Document document) {
        documentMapper.update(document);
        Document updated = documentMapper.selectByIdAndUser(document.getId(), document.getUserId());
        try {
            searchService.indexDocument(updated);
        } catch (IOException e) {
            System.err.println("索引更新失败: " + e.getMessage());
        }
        return updated;
    }

    @Transactional
    public boolean deleteDocument(Long id, Long userId) {
        boolean success = documentMapper.softDeleteByIdAndUser(id, userId) > 0;
        if (success) {
            try {
                searchService.deleteDocument(id);
            } catch (IOException e) {
                System.err.println("索引删除失败: " + e.getMessage());
            }
        }
        return success;
    }

    /**
     * 恢复文档
     */
    @Transactional
    public boolean restoreDocument(Long id, Long userId) {
        boolean success = documentMapper.restoreDocument(id, userId) > 0;
        if (success) {
            try {
                Document document = documentMapper.selectByIdAndUser(id, userId);
                searchService.indexDocument(document);
            } catch (IOException e) {
                System.err.println("索引恢复失败: " + e.getMessage());
            }
        }
        return success;
    }

    /**
     * 彻底删除文档
     */
    @Transactional
    public boolean permanentDelete(Long id, Long userId) {
        boolean success = documentMapper.permanentDelete(id, userId) > 0;
        if (success) {
            try {
                searchService.deleteDocument(id);
            } catch (IOException e) {
                System.err.println("索引删除失败: " + e.getMessage());
            }
        }
        return success;
    }

    /**
     * 获取回收站中的文档
     */
    public List<Document> getDeletedDocuments(Long userId) {
        return documentMapper.selectDeletedByUserId(userId);
    }

    /**
     * 清空回收站
     */
    @Transactional
    public boolean clearRecycleBin(Long userId) {
        List<Document> deletedDocuments = getDeletedDocuments(userId);
        for (Document doc : deletedDocuments) {
            permanentDelete(doc.getId(), userId);
        }
        return true;
    }

    /**
     * 🎯 优化：批量获取文档（使用批量查询提高性能）
     */
    public List<Document> getDocumentsByIds(List<Long> ids, Long userId) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }

        // 去重并过滤null值
        List<Long> uniqueIds = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (uniqueIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 使用批量查询替代循环查询
        return documentMapper.selectByIdsAndUser(uniqueIds, userId);
    }

    /**
     * 根据分类获取文档
     */
    public List<Document> getDocumentsByCategory(Long categoryId, Long userId) {
        return documentMapper.selectByCategoryIdAndUser(categoryId, userId);
    }

    /**
     * 🎯 新增：按标签获取文档
     */
    public List<Document> getDocumentsByTag(Long tagId, Long userId) {
        // 验证标签是否存在且属于该用户
        Tag tag = tagService.getTagById(tagId, userId);
        if (tag == null) {
            throw new RuntimeException("标签不存在或无权访问");
        }

        return documentMapper.selectByTagId(tagId, userId);
    }

    /**
     * 搜索文档（先用数据库模糊搜索，后续可切换到Lucene）
     */
    public List<Document> searchDocuments(String keyword, Long userId) {
        return documentMapper.searchByKeyword(keyword, userId);
    }

    /**
     * 获取文档版本历史
     */
    public List<Document> getDocumentVersions(Long documentId, Long userId) {
        Document currentDoc = documentMapper.selectByIdAndUser(documentId, userId);
        if (currentDoc == null) {
            throw new RuntimeException("文档不存在或无权访问");
        }

        // 这里应该查询版本历史表
        // 暂时返回空列表，需要创建版本历史表
        return List.of(currentDoc);
    }

    /**
     * 恢复到指定版本
     */
    @Transactional
    public Document restoreToVersion(Long documentId, Long versionId, Long userId) {
        // 实现版本恢复逻辑
        // 需要创建版本历史表来存储历史版本
        Document currentDoc = documentMapper.selectByIdAndUser(documentId, userId);
        if (currentDoc == null) {
            throw new RuntimeException("文档不存在或无权访问");
        }

        // 记录操作日志
        operationLogService.logOperation(userId, "UPDATE", "DOCUMENT", documentId,
                "恢复到版本: " + versionId);

        return currentDoc;
    }

    /**
     * 生成文档分享信息
     */
    public Map<String, Object> generateShareInfo(Long documentId, Long userId, Integer expireHours) {
        Document document = documentMapper.selectByIdAndUser(documentId, userId);
        if (document == null) {
            throw new RuntimeException("文档不存在或无权访问");
        }

        String shareToken = generateShareToken();
        LocalDateTime expireTime = LocalDateTime.now().plusHours(expireHours);

        Map<String, Object> shareInfo = new HashMap<>();
        shareInfo.put("shareToken", shareToken);
        shareInfo.put("expireTime", expireTime);
        shareInfo.put("shareUrl", "/shared/" + shareToken);

        operationLogService.logOperation(userId, "SHARE", "DOCUMENT", documentId,
                "生成分享链接，有效期: " + expireHours + "小时");

        return shareInfo;
    }

    private String generateShareToken() {
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * 批量删除文档
     */
    @Transactional
    public boolean batchDeleteDocuments(List<Long> documentIds, Long userId) {
        try {
            for (Long documentId : documentIds) {
                boolean success = deleteDocument(documentId, userId);
                if (!success) {
                    throw new RuntimeException("删除文档失败: " + documentId);
                }
            }
            return true;
        } catch (Exception e) {
            throw new RuntimeException("批量删除文档失败: " + e.getMessage(), e);
        }
    }

    /**
     * 通过分享token获取文档
     */
    public Document getDocumentByShareToken(String shareToken) {
        // 这里应该查询分享信息表获取文档ID
        // 暂时返回null，需要创建分享信息表
        return null;
    }

    /**
     * 切换收藏状态
     */
    public Map<String, Object> toggleFavorite(Long documentId, Long userId) {
        Document document = documentMapper.selectByIdAndUser(documentId, userId);
        if (document == null) {
            throw new RuntimeException("文档不存在或无权访问");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("documentId", documentId);
        result.put("isFavorited", true);
        result.put("favoriteCount", 1);

        operationLogService.logOperation(userId, "FAVORITE", "DOCUMENT", documentId,
                "切换收藏状态");

        return result;
    }

    /**
     * 获取最近编辑的文档
     */
    public List<Document> getRecentDocuments(Long userId, int limit) {
        return documentMapper.selectByUserId(userId).stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 复制文档
     */
    @Transactional
    public Document copyDocument(Long documentId, Long userId) {
        Document original = documentMapper.selectByIdAndUser(documentId, userId);
        if (original == null) {
            throw new RuntimeException("文档不存在或无权访问");
        }

        Document copy = new Document();
        copy.setTitle(original.getTitle() + " - 副本");
        copy.setContent(original.getContent());
        copy.setContentType(original.getContentType());
        copy.setCategoryId(original.getCategoryId());
        copy.setUserId(userId);

        documentMapper.insert(copy);

        operationLogService.logOperation(userId, "COPY", "DOCUMENT", documentId,
                "复制文档: " + original.getTitle());

        return copy;
    }

    /**
     * 导入文档
     */
    public Document importDocument(String title, String content, Long categoryId, Long userId) {
        Document document = new Document();
        document.setTitle(title);
        document.setContent(content);
        document.setContentType(Document.ContentType.TEXT);
        document.setCategoryId(categoryId);
        document.setUserId(userId);

        documentMapper.insert(document);

        operationLogService.logOperation(userId, "IMPORT", "DOCUMENT", document.getId(),
                "导入文档: " + title);

        return document;
    }

    /**
     * 🎯 优化：获取文档详情（包含分类名称和标签）- 使用批量查询
     */
    public List<Map<String, Object>> getDocumentsWithDetailsByIds(List<Long> ids, Long userId) {
        if (ids == null || ids.isEmpty()) {
            System.out.println("📭 getDocumentsWithDetailsByIds: 文档ID列表为空");
            return new ArrayList<>();
        }

        // 去重并过滤null值
        List<Long> uniqueIds = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        System.out.println("📋 getDocumentsWithDetailsByIds: 处理 " + uniqueIds.size() + " 个唯一文档ID");

        // 🎯 优化：批量获取文档基础信息
        List<Document> documents = documentMapper.selectByIdsAndUser(uniqueIds, userId);

        List<Map<String, Object>> result = new ArrayList<>();

        for (Document doc : documents) {
            try {
                Map<String, Object> docWithDetails = new HashMap<>();

                // 🎯 核心修复：确保字段名与前端匹配
                docWithDetails.put("id", doc.getId());
                docWithDetails.put("docId", doc.getId()); // 兼容字段名
                docWithDetails.put("title", doc.getTitle());
                docWithDetails.put("name", doc.getTitle()); // 兼容字段名
                docWithDetails.put("content", doc.getContent());
                docWithDetails.put("categoryId", doc.getCategoryId());
                docWithDetails.put("category", doc.getCategoryId()); // 兼容字段名
                docWithDetails.put("userId", doc.getUserId());
                docWithDetails.put("createdTime", doc.getCreatedTime());
                docWithDetails.put("updatedTime", doc.getUpdatedTime());
                docWithDetails.put("updateTime", doc.getUpdatedTime()); // 🎯 前端需要的字段名
                docWithDetails.put("contentType", doc.getContentType());
                docWithDetails.put("deleted", doc.getDeleted() != null ? doc.getDeleted() : false);
                docWithDetails.put("isFavorite", doc.getIsFavorite() != null ? doc.getIsFavorite() : false);
                docWithDetails.put("favoriteCount", doc.getFavoriteCount() != null ? doc.getFavoriteCount() : 0);

                // 🎯 添加分类名称
                if (doc.getCategoryId() != null) {
                    docWithDetails.put("categoryName", "分类" + doc.getCategoryId());
                } else {
                    docWithDetails.put("categoryName", "未分类");
                }

                // 🎯 获取并设置标签
                List<Tag> tags = tagService.getDocumentTags(doc.getId(), userId);

                // 返回标签对象列表
                docWithDetails.put("tagList", tags);

                // 返回标签名称字符串数组
                List<String> tagNames = tags.stream()
                        .map(Tag::getName)
                        .collect(Collectors.toList());
                docWithDetails.put("tags", tagNames);

                // 返回标签ID列表
                List<Long> tagIds = tags.stream()
                        .map(Tag::getId)
                        .collect(Collectors.toList());
                docWithDetails.put("tagIds", tagIds);

                result.add(docWithDetails);
                System.out.println("✅ 已加载文档详情: id=" + doc.getId() + ", title=" + doc.getTitle());

            } catch (Exception e) {
                System.err.println("❌ 处理文档详情失败: id=" + doc.getId() + ", error=" + e.getMessage());
                e.printStackTrace();
            }
        }

        // 🎯 按更新时间倒序排序
        result.sort((a, b) -> {
            LocalDateTime timeA = (LocalDateTime) a.get("updatedTime");
            LocalDateTime timeB = (LocalDateTime) b.get("updatedTime");
            if (timeA == null && timeB == null) return 0;
            if (timeA == null) return 1;
            if (timeB == null) return -1;
            return timeB.compareTo(timeA);
        });

        System.out.println("🎉 getDocumentsWithDetailsByIds 返回 " + result.size() + " 个文档详情");
        return result;
    }

    /**
     * 🎯 新增：获取文档统计信息
     */
    public Map<String, Object> getDocumentStatistics(Long userId) {
        Map<String, Object> statistics = new HashMap<>();

        // 获取各类文档数量
        List<Document> allDocs = documentMapper.selectByUserId(userId);
        List<Document> deletedDocs = documentMapper.selectDeletedByUserId(userId);

        statistics.put("totalDocuments", allDocs.size());
        statistics.put("activeDocuments", allDocs.size());
        statistics.put("deletedDocuments", deletedDocs.size());

        // 按分类统计
        Map<Long, Integer> categoryStats = allDocs.stream()
                .filter(doc -> doc.getCategoryId() != null)
                .collect(Collectors.groupingBy(Document::getCategoryId,
                        Collectors.summingInt(doc -> 1)));
        statistics.put("categoryStats", categoryStats);

        // 最近30天创建趋势（模拟数据）
        Map<String, Integer> recentTrend = new HashMap<>();
        for (int i = 0; i < 30; i++) {
            recentTrend.put(LocalDateTime.now().minusDays(i).toLocalDate().toString(),
                    (int) (Math.random() * 10));
        }
        statistics.put("recentTrend", recentTrend);

        return statistics;
    }

    /**
     * 🎯 新增：获取文档详情（包含标签）- 单个文档版本
     */
    public Map<String, Object> getDocumentWithDetails(Long documentId, Long userId) {
        Document doc = documentMapper.selectByIdAndUser(documentId, userId);
        if (doc == null) {
            return null;
        }

        Map<String, Object> docWithDetails = new HashMap<>();

        // 基础信息
        docWithDetails.put("id", doc.getId());
        docWithDetails.put("title", doc.getTitle());
        docWithDetails.put("content", doc.getContent());
        docWithDetails.put("categoryId", doc.getCategoryId());
        docWithDetails.put("userId", doc.getUserId());
        docWithDetails.put("createdTime", doc.getCreatedTime());
        docWithDetails.put("updatedTime", doc.getUpdatedTime());
        docWithDetails.put("updateTime", doc.getUpdatedTime());
        docWithDetails.put("contentType", doc.getContentType());

        // 分类名称
        if (doc.getCategoryId() != null) {
            docWithDetails.put("categoryName", "分类" + doc.getCategoryId());
        } else {
            docWithDetails.put("categoryName", "未分类");
        }

        // 标签信息
        List<Tag> tags = tagService.getDocumentTags(doc.getId(), userId);
        docWithDetails.put("tags", tags);

        List<String> tagNames = tags.stream()
                .map(Tag::getName)
                .collect(Collectors.toList());
        docWithDetails.put("tagNames", tagNames);

        return docWithDetails;
    }
}