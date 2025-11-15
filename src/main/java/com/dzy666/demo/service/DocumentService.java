package com.dzy666.demo.service;

import com.dzy666.demo.entity.Document;
import com.dzy666.demo.mapper.DocumentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    @Autowired
    private DocumentMapper documentMapper;

    @Autowired
    @Lazy
    private SearchService searchService;

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
}