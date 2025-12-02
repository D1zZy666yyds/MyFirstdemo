package com.dzy666.demo.controller;

import com.dzy666.demo.service.KnowledgeGraphService;
import com.dzy666.demo.util.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge-graph")
public class KnowledgeGraphController {

    @Autowired
    private KnowledgeGraphService knowledgeGraphService;

    @GetMapping("/full/{userId}")
    public JsonResult<Map<String, Object>> getFullKnowledgeGraph(@PathVariable Long userId) {
        try {
            Map<String, Object> graphData = knowledgeGraphService.generateKnowledgeGraph(userId);
            return JsonResult.success(graphData);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    @GetMapping("/document-relations/{userId}")
    public JsonResult<Map<String, Object>> getDocumentRelations(@PathVariable Long userId) {
        try {
            Map<String, Object> relations = knowledgeGraphService.generateDocumentRelations(userId);
            return JsonResult.success(relations);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    @GetMapping("/tag-cloud/{userId}")
    public JsonResult<List<Map<String, Object>>> getTagCloud(@PathVariable Long userId) {
        try {
            List<Map<String, Object>> tagCloud = knowledgeGraphService.generateTagCloud(userId);
            return JsonResult.success(tagCloud);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    @GetMapping("/similar-documents/{userId}")
    public JsonResult<List<Map<String, Object>>> getSimilarDocuments(
            @PathVariable Long userId,
            @RequestParam Long documentId,
            @RequestParam(defaultValue = "5") int limit) {
        try {
            List<Map<String, Object>> similarDocs = knowledgeGraphService.getSimilarDocuments(userId, documentId, limit);
            return JsonResult.success(similarDocs);
        } catch (Exception e) {
            return JsonResult.error("获取相似文档失败: " + e.getMessage());
        }
    }

    @PostMapping("/rebuild/{userId}")
    public JsonResult<String> rebuildKnowledgeGraph(@PathVariable Long userId) {
        try {
            knowledgeGraphService.rebuildKnowledgeGraph(userId);
            return JsonResult.success("知识图谱重建完成");
        } catch (Exception e) {
            return JsonResult.error("知识图谱重建失败: " + e.getMessage());
        }
    }

    @GetMapping("/central-nodes/{userId}")
    public JsonResult<List<Map<String, Object>>> getCentralNodes(@PathVariable Long userId) {
        try {
            List<Map<String, Object>> centralNodes = knowledgeGraphService.getCentralNodes(userId);
            return JsonResult.success(centralNodes);
        } catch (Exception e) {
            return JsonResult.error("获取中心节点失败: " + e.getMessage());
        }
    }

    @GetMapping("/relation-density/{userId}")
    public JsonResult<Map<String, Object>> getRelationDensity(@PathVariable Long userId) {
        try {
            Map<String, Object> density = knowledgeGraphService.getRelationDensityAnalysis(userId);
            return JsonResult.success(density);
        } catch (Exception e) {
            return JsonResult.error("获取关联密度失败: " + e.getMessage());
        }
    }

    @GetMapping("/knowledge-clusters/{userId}")
    public JsonResult<Map<String, Object>> getKnowledgeClusters(@PathVariable Long userId) {
        try {
            Map<String, Object> clusters = knowledgeGraphService.getKnowledgeClustersAnalysis(userId);
            return JsonResult.success(clusters);
        } catch (Exception e) {
            return JsonResult.error("获取知识聚类失败: " + e.getMessage());
        }
    }

    @GetMapping("/learning-path/{userId}")
    public JsonResult<Map<String, Object>> getLearningPath(@PathVariable Long userId) {
        try {
            Map<String, Object> learningPath = knowledgeGraphService.generateLearningPath(userId);
            return JsonResult.success(learningPath);
        } catch (Exception e) {
            return JsonResult.error("获取学习路径失败: " + e.getMessage());
        }
    }

    @GetMapping("/relation-strength/{userId}")
    public JsonResult<List<Map<String, Object>>> getRelationStrength(@PathVariable Long userId) {
        try {
            List<Map<String, Object>> strength = knowledgeGraphService.getRelationStrengthAnalysis(userId);
            return JsonResult.success(strength);
        } catch (Exception e) {
            return JsonResult.error("获取关联强度失败: " + e.getMessage());
        }
    }

    @GetMapping("/knowledge-gaps/{userId}")
    public JsonResult<Map<String, Object>> getKnowledgeGaps(@PathVariable Long userId) {
        try {
            Map<String, Object> gaps = knowledgeGraphService.analyzeKnowledgeGaps(userId);
            return JsonResult.success(gaps);
        } catch (Exception e) {
            return JsonResult.error("获取知识缺口分析失败: " + e.getMessage());
        }
    }
}