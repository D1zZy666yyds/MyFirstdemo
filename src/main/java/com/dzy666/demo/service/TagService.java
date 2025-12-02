package com.dzy666.demo.service;

import com.dzy666.demo.entity.Tag;
import com.dzy666.demo.mapper.TagMapper;
import com.dzy666.demo.mapper.DocumentTagMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TagService {

    private final TagMapper tagMapper;
    private final DocumentTagMapper documentTagMapper;

    public TagService(TagMapper tagMapper, DocumentTagMapper documentTagMapper) {
        this.tagMapper = tagMapper;
        this.documentTagMapper = documentTagMapper;
    }

    public Tag createTag(Tag tag) {
        Tag existingTag = tagMapper.selectByNameAndUser(tag.getName(), tag.getUserId());
        if (existingTag != null) {
            System.out.println("标签 '" + tag.getName() + "' 已存在，返回现有标签 ID: " + existingTag.getId());
            return existingTag;
        }

        System.out.println("创建新标签: " + tag.getName() + ", 用户ID: " + tag.getUserId());
        tagMapper.insert(tag);
        return tag;
    }

    public Tag getTagById(Long id, Long userId) {
        Tag tag = tagMapper.selectByIdAndUser(id, userId);
        if (tag != null) {
            // 修复：添加userId参数
            int count = tagMapper.countDocumentsByTag(id, userId);
            tag.setDocumentCount(count);
        }
        return tag;
    }

    public List<Tag> getUserTags(Long userId) {
        List<Tag> tags = tagMapper.selectByUserId(userId);
        // 为每个标签设置文档计数
        for (Tag tag : tags) {
            // 修复：添加userId参数
            int count = tagMapper.countDocumentsByTag(tag.getId(), userId);
            tag.setDocumentCount(count);
            System.out.println("用户 " + userId + " 的标签: '" + tag.getName() + "' (ID:" + tag.getId() + ") 文档计数: " + count);
        }
        return tags;
    }

    public Tag updateTag(Tag tag) {
        Tag existingTag = tagMapper.selectByNameAndUser(tag.getName(), tag.getUserId());
        if (existingTag != null && !existingTag.getId().equals(tag.getId())) {
            throw new RuntimeException("标签名称 '" + tag.getName() + "' 已存在");
        }

        tagMapper.update(tag);
        Tag updatedTag = tagMapper.selectByIdAndUser(tag.getId(), tag.getUserId());
        if (updatedTag != null) {
            // 修复：添加userId参数
            int count = tagMapper.countDocumentsByTag(tag.getId(), tag.getUserId());
            updatedTag.setDocumentCount(count);
        }
        return updatedTag;
    }

    @Transactional
    public boolean deleteTag(Long id, Long userId) {
        // 修复：添加userId参数
        int docCount = tagMapper.countDocumentsByTag(id, userId);
        if (docCount > 0) {
            throw new RuntimeException("该标签已被文档使用，无法删除");
        }

        return tagMapper.deleteByIdAndUser(id, userId) > 0;
    }

    public List<Tag> getDocumentTags(Long documentId, Long userId) {
        List<Tag> tags = tagMapper.selectByDocumentId(documentId, userId);
        for (Tag tag : tags) {
            // 修复：添加userId参数
            int count = tagMapper.countDocumentsByTag(tag.getId(), userId);
            tag.setDocumentCount(count);
        }
        return tags;
    }

    @Transactional
    public boolean addTagToDocument(Long documentId, Long tagId, Long userId) {
        Tag tag = tagMapper.selectByIdAndUser(tagId, userId);
        if (tag == null) {
            throw new RuntimeException("标签不存在或无权访问");
        }

        if (documentTagMapper.exists(documentId, tagId) > 0) {
            throw new RuntimeException("文档已包含该标签");
        }

        return documentTagMapper.insert(documentId, tagId) > 0;
    }

    @Transactional
    public boolean removeTagFromDocument(Long documentId, Long tagId, Long userId) {
        Tag tag = tagMapper.selectByIdAndUser(tagId, userId);
        if (tag == null) {
            throw new RuntimeException("标签不存在或无权访问");
        }

        return documentTagMapper.delete(documentId, tagId) > 0;
    }

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

    @Transactional
    public boolean setDocumentTags(Long documentId, List<Long> tagIds, Long userId) {
        for (Long tagId : tagIds) {
            Tag tag = tagMapper.selectByIdAndUser(tagId, userId);
            if (tag == null) {
                throw new RuntimeException("标签不存在或无权访问: " + tagId);
            }
        }

        documentTagMapper.deleteByDocumentId(documentId);

        for (Long tagId : tagIds) {
            documentTagMapper.insert(documentId, tagId);
        }

        return true;
    }

    public List<Long> getDocumentIdsByTag(Long tagId, Long userId) {
        return documentTagMapper.findDocumentIdsByTagIdAndUserId(tagId, userId);
    }

    public List<Tag> getPopularTags(Long userId, int limit) {
        List<Tag> allTags = tagMapper.selectByUserId(userId);

        // 为每个标签计算使用次数并排序
        return allTags.stream()
                .peek(tag -> {
                    // 修复：添加userId参数
                    int count = tagMapper.countDocumentsByTag(tag.getId(), userId);
                    tag.setDocumentCount(count);
                })
                .sorted((tag1, tag2) -> Integer.compare(tag2.getDocumentCount(), tag1.getDocumentCount()))
                .limit(limit)
                .collect(Collectors.toList());
    }
}