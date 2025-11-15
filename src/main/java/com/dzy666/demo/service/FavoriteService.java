package com.dzy666.demo.service;

import com.dzy666.demo.entity.Document;
import com.dzy666.demo.mapper.FavoriteMapper;
import com.dzy666.demo.mapper.DocumentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FavoriteService {

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private DocumentMapper documentMapper;

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
     * 获取用户的收藏文档列表
     */
    public List<Document> getUserFavorites(Long userId) {
        List<Long> documentIds = favoriteMapper.selectFavoriteDocumentIds(userId);
        return documentIds.stream()
                .map(id -> documentMapper.selectByIdAndUser(id, userId))
                .filter(doc -> doc != null)
                .collect(Collectors.toList());
    }

    /**
     * 获取文档的收藏数量
     */
    public int getFavoriteCount(Long documentId) {
        return favoriteMapper.countByDocumentId(documentId);
    }
}