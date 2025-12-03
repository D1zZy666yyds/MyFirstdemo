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
            // 记录日志，但不影响主要功能
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

    public List<Document> getDocumentsByIds(List<Long> ids, Long userId) {
        return ids.stream()
                .map(id -> documentMapper.selectByIdAndUser(id, userId))
                .filter(doc -> doc != null)
                .collect(Collectors.toList());
    }

    /**
     * 根据分类获取文档
     */
    public List<Document> getDocumentsByCategory(Long categoryId, Long userId) {
        return documentMapper.selectByCategoryIdAndUser(categoryId, userId);
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

        // 保存分享信息到数据库
        // documentMapper.saveShareInfo(documentId, shareToken, expireTime, userId);

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

        // 这里应该实现收藏逻辑
        // 暂时返回模拟数据
        Map<String, Object> result = new HashMap<>();
        result.put("documentId", documentId);
        result.put("isFavorited", true); // 模拟切换后的状态
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
        document.setContentType(Document.ContentType.TEXT);  // 修复这里：使用枚举值而不是字符串
        document.setCategoryId(categoryId);
        document.setUserId(userId);

        documentMapper.insert(document);

        operationLogService.logOperation(userId, "IMPORT", "DOCUMENT", document.getId(),
                "导入文档: " + title);

        return document;
    }

    /**
     * 获取文档详情（包含分类名称和标签）- 修复增强版
     * 🎯 修复：确保返回的字段名与前端期望完全匹配
     */
    public List<Map<String, Object>> getDocumentsWithDetailsByIds(List<Long> ids, Long userId) {
        if (ids == null || ids.isEmpty()) {
            System.out.println("📭 getDocumentsWithDetailsByIds: 文档ID列表为空");
            return new ArrayList<>();
        }

        // 去重
        List<Long> uniqueIds = ids.stream().distinct().collect(Collectors.toList());
        System.out.println("📋 getDocumentsWithDetailsByIds: 处理 " + uniqueIds.size() + " 个唯一文档ID");

        List<Map<String, Object>> result = new ArrayList<>();

        for (Long id : uniqueIds) {
            try {
                // 获取文档基本信息
                Document doc = documentMapper.selectByIdAndUser(id, userId);
                if (doc == null) {
                    System.out.println("⚠️  文档不存在或无权访问: id=" + id + ", userId=" + userId);
                    continue;
                }

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
                docWithDetails.put("updateTime", doc.getUpdatedTime()); // 🎯 新增：前端也使用updateTime字段
                docWithDetails.put("contentType", doc.getContentType());
                docWithDetails.put("deleted", doc.getDeleted() != null ? doc.getDeleted() : false);
                docWithDetails.put("isFavorite", doc.getIsFavorite() != null ? doc.getIsFavorite() : false);
                docWithDetails.put("favoriteCount", doc.getFavoriteCount() != null ? doc.getFavoriteCount() : 0);

                // 🎯 修复：添加分类名称（前端使用categoryDisplay方法需要categoryId或categoryName）
                if (doc.getCategoryId() != null) {
                    // 这里需要调用CategoryService获取分类名称
                    // 暂时使用简单格式，实际应该调用categoryService.getCategoryName()
                    docWithDetails.put("categoryName", "分类" + doc.getCategoryId());
                } else {
                    docWithDetails.put("categoryName", "未分类");
                }

                // 🎯 修复：标签字段 - 确保与前端formatTags方法兼容
                List<Tag> tags = tagService.getDocumentTags(doc.getId(), userId);

                // 方案1：返回标签对象列表（包含id和name）
                docWithDetails.put("tagList", tags);

                // 方案2：返回标签名称字符串数组（前端formatTags方法期望的格式）
                List<String> tagNames = tags.stream()
                        .map(Tag::getName)
                        .collect(Collectors.toList());
                docWithDetails.put("tags", tagNames);

                // 方案3：返回标签ID列表（可选）
                List<Long> tagIds = tags.stream()
                        .map(Tag::getId)
                        .collect(Collectors.toList());
                docWithDetails.put("tagIds", tagIds);

                result.add(docWithDetails);
                System.out.println("✅ 已加载文档详情: id=" + doc.getId() + ", title=" + doc.getTitle());

            } catch (Exception e) {
                System.err.println("❌ 获取文档详情失败: id=" + id + ", error=" + e.getMessage());
                e.printStackTrace();
            }
        }

        // 🎯 修复：按更新时间倒序排序（匹配搜索服务的排序）
        result.sort((a, b) -> {
            LocalDateTime timeA = (LocalDateTime) a.get("updatedTime");
            LocalDateTime timeB = (LocalDateTime) b.get("updatedTime");
            if (timeA == null && timeB == null) return 0;
            if (timeA == null) return 1;
            if (timeB == null) return -1;
            return timeB.compareTo(timeA); // 降序：最新的在前
        });

        System.out.println("🎉 getDocumentsWithDetailsByIds 返回 " + result.size() + " 个文档详情");
        return result;
    }

}