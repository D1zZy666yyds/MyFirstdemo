package com.dzy666.demo.service;

import com.dzy666.demo.entity.Document;
import com.dzy666.demo.entity.Category;
import com.dzy666.demo.entity.Tag;
import com.dzy666.demo.mapper.DocumentMapper;
import com.dzy666.demo.mapper.CategoryMapper;
import com.dzy666.demo.mapper.DocumentTagMapper;
import com.dzy666.demo.mapper.TagMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class KnowledgeGraphService {

    @Autowired
    private DocumentMapper documentMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private DocumentTagMapper documentTagMapper;

    /**
     * 生成知识图谱数据（ECharts力导向图格式）
     */
    public Map<String, Object> generateKnowledgeGraph(Long userId) {
        Map<String, Object> graphData = new HashMap<>();

        // 节点列表
        List<Map<String, Object>> nodes = new ArrayList<>();
        // 关系列表
        List<Map<String, Object>> links = new ArrayList<>();

        // 获取用户的所有文档、分类、标签
        List<Document> documents = documentMapper.selectByUserId(userId);
        List<Category> categories = categoryMapper.selectByUserId(userId);
        List<Tag> tags = tagMapper.selectByUserId(userId);

        // 添加分类节点
        for (Category category : categories) {
            Map<String, Object> node = new HashMap<>();
            node.put("id", "category_" + category.getId());
            node.put("name", category.getName());
            node.put("category", "分类");
            node.put("symbolSize", 40);
            node.put("itemStyle", Map.of("color", "#5470c6"));
            nodes.add(node);
        }

        // 添加文档节点
        for (Document document : documents) {
            Map<String, Object> node = new HashMap<>();
            node.put("id", "document_" + document.getId());
            node.put("name", document.getTitle());
            node.put("category", "文档");
            node.put("symbolSize", 30);
            node.put("itemStyle", Map.of("color", "#91cc75"));
            nodes.add(node);

            // 添加文档与分类的关系
            if (document.getCategoryId() != null) {
                Map<String, Object> link = new HashMap<>();
                link.put("source", "document_" + document.getId());
                link.put("target", "category_" + document.getCategoryId());
                link.put("name", "属于");
                links.add(link);
            }
        }

        // 添加标签节点和文档标签关系
        for (Tag tag : tags) {
            Map<String, Object> node = new HashMap<>();
            node.put("id", "tag_" + tag.getId());
            node.put("name", tag.getName());
            node.put("category", "标签");
            node.put("symbolSize", 25);
            node.put("itemStyle", Map.of("color", "#fac858"));
            nodes.add(node);

            // 获取使用该标签的文档
            List<Document> taggedDocuments = documentMapper.selectByTagId(tag.getId(), userId);
            for (Document doc : taggedDocuments) {
                Map<String, Object> link = new HashMap<>();
                link.put("source", "document_" + doc.getId());
                link.put("target", "tag_" + tag.getId());
                link.put("name", "标记");
                links.add(link);
            }
        }

        // 添加分类之间的父子关系
        for (Category category : categories) {
            if (category.getParentId() != null) {
                Map<String, Object> link = new HashMap<>();
                link.put("source", "category_" + category.getId());
                link.put("target", "category_" + category.getParentId());
                link.put("name", "子分类");
                links.add(link);
            }
        }

        graphData.put("nodes", nodes);
        graphData.put("links", links);

        return graphData;
    }

    /**
     * 生成文档关联关系图（文档之间的相似性）
     */
    public Map<String, Object> generateDocumentRelations(Long userId) {
        Map<String, Object> graphData = new HashMap<>();
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> links = new ArrayList<>();

        List<Document> documents = documentMapper.selectByUserId(userId);
        List<Tag> tags = tagMapper.selectByUserId(userId);

        // 添加文档节点
        for (Document document : documents) {
            Map<String, Object> node = new HashMap<>();
            node.put("id", "doc_" + document.getId());
            node.put("name", document.getTitle());
            node.put("value", 1);
            node.put("category", "文档");
            node.put("symbolSize", 35);
            node.put("itemStyle", Map.of("color", "#ee6666"));
            nodes.add(node);
        }

        // 添加标签节点
        for (Tag tag : tags) {
            Map<String, Object> node = new HashMap<>();
            node.put("id", "tag_" + tag.getId());
            node.put("name", tag.getName());
            node.put("value", 1);
            node.put("category", "标签");
            node.put("symbolSize", 20);
            node.put("itemStyle", Map.of("color", "#73c0de"));
            nodes.add(node);
        }

        // 建立文档-标签关联
        for (Document document : documents) {
            // 获取文档的所有标签
            List<Tag> documentTags = tagMapper.selectByDocumentId(document.getId(), userId);
            for (Tag tag : documentTags) {
                Map<String, Object> link = new HashMap<>();
                link.put("source", "doc_" + document.getId());
                link.put("target", "tag_" + tag.getId());
                link.put("value", "标记");
                links.add(link);
            }
        }

        // 建立文档-文档关联（通过共同标签）
        for (int i = 0; i < documents.size(); i++) {
            for (int j = i + 1; j < documents.size(); j++) {
                Document doc1 = documents.get(i);
                Document doc2 = documents.get(j);

                // 获取文档1的标签
                List<Tag> tags1 = tagMapper.selectByDocumentId(doc1.getId(), userId);
                // 获取文档2的标签
                List<Tag> tags2 = tagMapper.selectByDocumentId(doc2.getId(), userId);

                // 计算共同标签数量
                Set<Long> commonTagIds = tags1.stream()
                        .map(Tag::getId)
                        .filter(tagId -> tags2.stream().anyMatch(t -> t.getId().equals(tagId)))
                        .collect(Collectors.toSet());

                if (!commonTagIds.isEmpty()) {
                    Map<String, Object> link = new HashMap<>();
                    link.put("source", "doc_" + doc1.getId());
                    link.put("target", "doc_" + doc2.getId());
                    link.put("value", "相关(" + commonTagIds.size() + "个共同标签)");
                    link.put("lineStyle", Map.of("width", commonTagIds.size()));
                    links.add(link);
                }
            }
        }

        graphData.put("nodes", nodes);
        graphData.put("links", links);

        return graphData;
    }

    /**
     * 获取标签云数据
     */
    public List<Map<String, Object>> generateTagCloud(Long userId) {
        List<Tag> tags = tagMapper.selectByUserId(userId);

        return tags.stream().map(tag -> {
            Map<String, Object> tagData = new HashMap<>();
            tagData.put("name", tag.getName());
            tagData.put("value", tagMapper.countDocumentsByTag(tag.getId()));
            tagData.put("textStyle", Map.of(
                    "fontSize", Math.max(12, Math.min(30, tagMapper.countDocumentsByTag(tag.getId()) * 5))
            ));
            return tagData;
        }).collect(Collectors.toList());
    }

    /**
     * 获取学习路径分析
     */
    public Map<String, Object> generateLearningPath(Long userId) {
        Map<String, Object> pathData = new HashMap<>();
        List<Document> documents = documentMapper.selectByUserId(userId);

        // 按创建时间排序
        documents.sort(Comparator.comparing(Document::getCreatedTime));

        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> links = new ArrayList<>();

        // 创建时间线节点
        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            Map<String, Object> node = new HashMap<>();
            node.put("name", doc.getTitle());
            node.put("value", doc.getCreatedTime().toString());
            node.put("symbolSize", 20 + (i * 2));
            node.put("itemStyle", Map.of(
                    "color", i == documents.size() - 1 ? "#ff0000" : "#37A2DA"  // 最新文档标红
            ));
            nodes.add(node);

            // 添加时间线连接
            if (i > 0) {
                Map<String, Object> link = new HashMap<>();
                link.put("source", i - 1);
                link.put("target", i);
                links.add(link);
            }
        }

        pathData.put("nodes", nodes);
        pathData.put("links", links);
        pathData.put("totalSteps", documents.size());
        pathData.put("startDate", documents.isEmpty() ? "" : documents.get(0).getCreatedTime().toString());
        pathData.put("latestDate", documents.isEmpty() ? "" : documents.get(documents.size() - 1).getCreatedTime().toString());

        return pathData;
    }
}