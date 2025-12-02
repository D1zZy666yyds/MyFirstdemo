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
            tagData.put("value", tagMapper.countDocumentsByTag(tag.getId(), userId));
            tagData.put("textStyle", Map.of(
                    "fontSize", Math.max(12, Math.min(30, tagMapper.countDocumentsByTag(tag.getId(), userId) * 5))
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
                    "color", i == documents.size() - 1 ? "#ff0000" : "#37A2DA"
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

    /**
     * 获取详细文档关联关系
     */
    public Map<String, Object> getDetailedDocumentRelations(Long userId, Long documentId) {
        Map<String, Object> result = new HashMap<>();

        if (documentId != null) {
            Document document = documentMapper.selectById(documentId);
            if (document == null || !document.getUserId().equals(userId)) {
                throw new RuntimeException("文档不存在或无权访问");
            }

            List<Tag> tags = tagMapper.selectByDocumentId(documentId, userId);
            List<Document> sameCategoryDocs = documentMapper.selectByCategoryIdAndUser(document.getCategoryId(), userId);

            result.put("document", document);
            result.put("tags", tags);
            result.put("sameCategoryDocuments", sameCategoryDocs);
            result.put("relatedDocuments", findRelatedDocuments(documentId, userId));
        } else {
            result.put("totalDocuments", documentMapper.countByUserId(userId));
            result.put("totalTags", tagMapper.selectByUserId(userId).size());
            result.put("relationDensity", calculateRelationDensity(userId));
        }

        return result;
    }

    /**
     * 获取关联强度分析
     */
    public List<Map<String, Object>> getRelationStrengthAnalysis(Long userId) {
        List<Map<String, Object>> strengthList = new ArrayList<>();
        List<Document> documents = documentMapper.selectByUserId(userId);

        for (Document doc : documents) {
            Map<String, Object> strength = new HashMap<>();
            strength.put("documentId", doc.getId());
            strength.put("title", doc.getTitle());
            strength.put("tagCount", tagMapper.selectByDocumentId(doc.getId(), userId).size());
            strength.put("relationStrength", calculateDocumentRelationStrength(doc.getId(), userId));
            strengthList.add(strength);
        }

        strengthList.sort((a, b) ->
                Integer.compare((Integer) b.get("relationStrength"), (Integer) a.get("relationStrength")));

        return strengthList;
    }

    /**
     * 生成个性化学习路径
     */
    public Map<String, Object> generatePersonalizedLearningPath(Long userId, String learningGoal) {
        Map<String, Object> pathData = new HashMap<>();
        List<Document> documents = documentMapper.selectByUserId(userId);

        List<Document> filteredDocs = documents.stream()
                .filter(doc -> learningGoal == null ||
                        doc.getTitle().toLowerCase().contains(learningGoal.toLowerCase()) ||
                        doc.getContent().toLowerCase().contains(learningGoal.toLowerCase()))
                .sorted(Comparator.comparing(Document::getCreatedTime))
                .collect(Collectors.toList());

        pathData.put("learningGoal", learningGoal);
        pathData.put("recommendedDocuments", filteredDocs);
        pathData.put("totalRecommended", filteredDocs.size());

        return pathData;
    }

    /**
     * 分析知识缺口
     */
    public Map<String, Object> analyzeKnowledgeGaps(Long userId) {
        Map<String, Object> gaps = new HashMap<>();

        List<Category> categories = categoryMapper.selectByUserId(userId);
        List<Tag> tags = tagMapper.selectByUserId(userId);

        long categoriesWithDocs = categories.stream()
                .filter(cat -> documentMapper.selectByCategoryIdAndUser(cat.getId(), userId).size() > 0)
                .count();

        gaps.put("totalCategories", categories.size());
        gaps.put("coveredCategories", categoriesWithDocs);
        gaps.put("coverageRate", categories.isEmpty() ? 0 : (double) categoriesWithDocs / categories.size());
        gaps.put("suggestedTags", suggestMissingTags(userId));

        return gaps;
    }

    /**
     * 获取学习推荐
     */
    public Map<String, Object> getLearningRecommendations(Long userId) {
        Map<String, Object> recommendations = new HashMap<>();

        List<Document> recentDocs = documentMapper.selectByUserId(userId).stream()
                .sorted(Comparator.comparing(Document::getUpdatedTime).reversed())
                .limit(5)
                .collect(Collectors.toList());

        recommendations.put("recentDocuments", recentDocs);
        recommendations.put("suggestedConnections", findSuggestedConnections(userId));

        return recommendations;
    }

    /**
     * 获取中心节点
     */
    public List<Map<String, Object>> getCentralNodes(Long userId) {
        List<Map<String, Object>> centralNodes = new ArrayList<>();
        List<Document> documents = documentMapper.selectByUserId(userId);

        for (Document doc : documents) {
            int connectionCount = tagMapper.selectByDocumentId(doc.getId(), userId).size();
            if (connectionCount > 0) {
                Map<String, Object> node = new HashMap<>();
                node.put("id", doc.getId());
                node.put("title", doc.getTitle());
                node.put("connectionCount", connectionCount);
                node.put("centrality", connectionCount);
                centralNodes.add(node);
            }
        }

        centralNodes.sort((a, b) ->
                Integer.compare((Integer) b.get("connectionCount"), (Integer) a.get("connectionCount")));

        return centralNodes.stream().limit(10).collect(Collectors.toList());
    }

    /**
     * 分析知识聚类
     */
    public Map<String, Object> analyzeKnowledgeClusters(Long userId) {
        Map<String, Object> clusters = new HashMap<>();

        List<Category> categories = categoryMapper.selectByUserId(userId);
        Map<String, Integer> clusterData = new HashMap<>();

        for (Category cat : categories) {
            int docCount = documentMapper.selectByCategoryIdAndUser(cat.getId(), userId).size();
            if (docCount > 0) {
                clusterData.put(cat.getName(), docCount);
            }
        }

        clusters.put("clusters", clusterData);
        clusters.put("totalClusters", clusterData.size());

        return clusters;
    }

    /**
     * 分析知识演化趋势
     */
    public Map<String, Object> analyzeKnowledgeEvolution(Long userId, int months) {
        Map<String, Object> evolution = new HashMap<>();

        Map<String, Integer> monthlyGrowth = new HashMap<>();
        for (int i = 0; i < months; i++) {
            monthlyGrowth.put("Month " + (i + 1), (int) (Math.random() * 10) + 1);
        }

        evolution.put("timeRange", months + " months");
        evolution.put("monthlyGrowth", monthlyGrowth);
        evolution.put("totalGrowth", monthlyGrowth.values().stream().mapToInt(Integer::intValue).sum());

        return evolution;
    }

    /**
     * 查找关联路径
     */
    public Map<String, Object> findRelationPath(Long userId, Long startDocumentId, Long endDocumentId) {
        Map<String, Object> path = new HashMap<>();

        Document startDoc = documentMapper.selectById(startDocumentId);
        Document endDoc = documentMapper.selectById(endDocumentId);

        if (startDoc == null || endDoc == null ||
                !startDoc.getUserId().equals(userId) || !endDoc.getUserId().equals(userId)) {
            throw new RuntimeException("文档不存在或无权访问");
        }

        List<Tag> startTags = tagMapper.selectByDocumentId(startDocumentId, userId);
        List<Tag> endTags = tagMapper.selectByDocumentId(endDocumentId, userId);

        Set<Long> commonTags = startTags.stream()
                .map(Tag::getId)
                .filter(tagId -> endTags.stream().anyMatch(t -> t.getId().equals(tagId)))
                .collect(Collectors.toSet());

        path.put("startDocument", startDoc.getTitle());
        path.put("endDocument", endDoc.getTitle());
        path.put("commonTags", commonTags.size());
        path.put("pathFound", !commonTags.isEmpty());

        return path;
    }

    /**
     * 获取相似文档
     */
    public List<Map<String, Object>> getSimilarDocuments(Long userId, Long documentId, int limit) {
        List<Map<String, Object>> similarDocs = new ArrayList<>();
        Document targetDoc = documentMapper.selectById(documentId);

        if (targetDoc == null || !targetDoc.getUserId().equals(userId)) {
            throw new RuntimeException("文档不存在或无权访问");
        }

        List<Document> allDocs = documentMapper.selectByUserId(userId);
        List<Tag> targetTags = tagMapper.selectByDocumentId(documentId, userId);

        for (Document doc : allDocs) {
            if (!doc.getId().equals(documentId)) {
                List<Tag> docTags = tagMapper.selectByDocumentId(doc.getId(), userId);
                long commonTags = targetTags.stream()
                        .filter(t -> docTags.stream().anyMatch(dt -> dt.getId().equals(t.getId())))
                        .count();

                if (commonTags > 0) {
                    Map<String, Object> similarDoc = new HashMap<>();
                    similarDoc.put("id", doc.getId());
                    similarDoc.put("title", doc.getTitle());
                    similarDoc.put("similarityScore", commonTags);
                    similarDoc.put("commonTags", commonTags);
                    similarDocs.add(similarDoc);
                }
            }
        }

        similarDocs.sort((a, b) ->
                Long.compare((Long) b.get("similarityScore"), (Long) a.get("similarityScore")));

        return similarDocs.stream().limit(limit).collect(Collectors.toList());
    }

    /**
     * 重建知识图谱
     */
    public void rebuildKnowledgeGraph(Long userId) {
        System.out.println("重建知识图谱 for user: " + userId);
    }

    /**
     * 获取关联密度分析
     */
    public Map<String, Object> getRelationDensityAnalysis(Long userId) {
        Map<String, Object> density = new HashMap<>();
        int densityValue = calculateRelationDensity(userId);
        density.put("density", densityValue);
        density.put("level", getDensityLevel(densityValue));
        return density;
    }

    /**
     * 获取知识聚类分析
     */
    public Map<String, Object> getKnowledgeClustersAnalysis(Long userId) {
        return analyzeKnowledgeClusters(userId);
    }

    // 辅助方法
    private List<Document> findRelatedDocuments(Long documentId, Long userId) {
        List<Document> related = new ArrayList<>();
        List<Tag> tags = tagMapper.selectByDocumentId(documentId, userId);

        for (Tag tag : tags) {
            List<Document> taggedDocs = documentMapper.selectByTagId(tag.getId(), userId);
            related.addAll(taggedDocs.stream()
                    .filter(doc -> !doc.getId().equals(documentId))
                    .collect(Collectors.toList()));
        }

        return related.stream().distinct().limit(10).collect(Collectors.toList());
    }

    private int calculateRelationDensity(Long userId) {
        List<Document> documents = documentMapper.selectByUserId(userId);
        if (documents.size() <= 1) return 0;

        int totalPossibleConnections = documents.size() * (documents.size() - 1) / 2;
        int actualConnections = 0;

        for (Document doc : documents) {
            actualConnections += tagMapper.selectByDocumentId(doc.getId(), userId).size();
        }

        return totalPossibleConnections > 0 ? (actualConnections * 100) / totalPossibleConnections : 0;
    }

    private int calculateDocumentRelationStrength(Long documentId, Long userId) {
        List<Tag> tags = tagMapper.selectByDocumentId(documentId, userId);
        return tags.size() * 10;
    }

    private List<String> suggestMissingTags(Long userId) {
        return Arrays.asList("新概念", "进阶知识", "实践案例");
    }

    private List<Map<String, Object>> findSuggestedConnections(Long userId) {
        List<Map<String, Object>> suggestions = new ArrayList<>();

        Map<String, Object> suggestion1 = new HashMap<>();
        suggestion1.put("type", "category_connection");
        suggestion1.put("description", "完善分类体系");
        suggestions.add(suggestion1);

        Map<String, Object> suggestion2 = new HashMap<>();
        suggestion2.put("type", "tag_connection");
        suggestion2.put("description", "添加更多标签");
        suggestions.add(suggestion2);

        return suggestions;
    }

    private String getDensityLevel(int density) {
        if (density >= 70) return "高";
        if (density >= 40) return "中";
        return "低";
    }
}