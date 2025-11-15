package com.dzy666.demo.service;

import com.dzy666.demo.entity.Tag;
import com.dzy666.demo.mapper.TagMapper;
import com.dzy666.demo.mapper.DocumentTagMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TagService {

    private final TagMapper tagMapper;
    private final DocumentTagMapper documentTagMapper;

    public TagService(TagMapper tagMapper, DocumentTagMapper documentTagMapper) {
        this.tagMapper = tagMapper;
        this.documentTagMapper = documentTagMapper;
    }

    public Tag createTag(Tag tag) {
        // 检查标签是否已存在
        Tag existingTag = tagMapper.selectByNameAndUser(tag.getName(), tag.getUserId());
        if (existingTag != null) {
            return existingTag; // 返回已存在的标签
        }
        tagMapper.insert(tag);
        return tag;
    }

    public Tag getTagById(Long id, Long userId) {
        return tagMapper.selectByIdAndUser(id, userId);
    }

    public List<Tag> getUserTags(Long userId) {
        return tagMapper.selectByUserId(userId);
    }

    public Tag updateTag(Tag tag) {
        tagMapper.update(tag);
        return tagMapper.selectByIdAndUser(tag.getId(), tag.getUserId());
    }

    @Transactional
    public boolean deleteTag(Long id, Long userId) {
        // 检查是否有文档使用该标签
        int docCount = tagMapper.countDocumentsByTag(id);
        if (docCount > 0) {
            throw new RuntimeException("该标签已被文档使用，无法删除");
        }

        return tagMapper.deleteByIdAndUser(id, userId) > 0;
    }

    public List<Tag> getDocumentTags(Long documentId, Long userId) {
        return tagMapper.selectByDocumentId(documentId, userId);
    }

    /**
     * 为文档添加标签
     */
    @Transactional
    public boolean addTagToDocument(Long documentId, Long tagId, Long userId) {
        // 验证标签属于该用户
        Tag tag = tagMapper.selectByIdAndUser(tagId, userId);
        if (tag == null) {
            throw new RuntimeException("标签不存在或无权访问");
        }

        // 检查是否已关联
        if (documentTagMapper.exists(documentId, tagId) > 0) {
            throw new RuntimeException("文档已包含该标签");
        }

        return documentTagMapper.insert(documentId, tagId) > 0;
    }

    /**
     * 从文档移除标签
     */
    @Transactional
    public boolean removeTagFromDocument(Long documentId, Long tagId, Long userId) {
        // 验证标签属于该用户
        Tag tag = tagMapper.selectByIdAndUser(tagId, userId);
        if (tag == null) {
            throw new RuntimeException("标签不存在或无权访问");
        }

        return documentTagMapper.delete(documentId, tagId) > 0;
    }

    /**
     * 创建或获取标签（如果已存在）
     */
    public Tag createOrGetTag(String tagName, Long userId) {
        Tag existingTag = tagMapper.selectByNameAndUser(tagName, userId);
        if (existingTag != null) {
            return existingTag;
        }

        Tag newTag = new Tag();
        newTag.setName(tagName);
        newTag.setUserId(userId);
        tagMapper.insert(newTag);
        return newTag;
    }

    /**
     * 批量为文档设置标签（先清除原有标签，再设置新标签）
     */
    @Transactional
    public boolean setDocumentTags(Long documentId, List<Long> tagIds, Long userId) {
        // 验证所有标签都属于该用户
        for (Long tagId : tagIds) {
            Tag tag = tagMapper.selectByIdAndUser(tagId, userId);
            if (tag == null) {
                throw new RuntimeException("标签不存在或无权访问: " + tagId);
            }
        }

        // 删除文档的所有现有标签
        documentTagMapper.deleteByDocumentId(documentId);

        // 添加新标签
        for (Long tagId : tagIds) {
            documentTagMapper.insert(documentId, tagId);
        }

        return true;
    }

    /**
     * 获取热门标签（按使用次数排序）
     */
    public List<Tag> getPopularTags(Long userId, int limit) {
        List<Tag> allTags = tagMapper.selectByUserId(userId);

        // 为每个标签计算使用次数
        for (Tag tag : allTags) {
            int usageCount = tagMapper.countDocumentsByTag(tag.getId());
            // 这里可以添加使用次数字段，暂时先返回所有标签
        }

        return allTags.stream()
                .limit(limit)
                .collect(java.util.stream.Collectors.toList());
    }
}