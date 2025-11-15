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

    /**
     * 获取完整知识图谱
     */
    @GetMapping("/full/{userId}")
    public JsonResult<Map<String, Object>> getFullKnowledgeGraph(@PathVariable Long userId) {
        try {
            Map<String, Object> graphData = knowledgeGraphService.generateKnowledgeGraph(userId);
            return JsonResult.success(graphData);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    /**
     * 获取文档关联关系图
     */
    @GetMapping("/document-relations/{userId}")
    public JsonResult<Map<String, Object>> getDocumentRelations(@PathVariable Long userId) {
        try {
            Map<String, Object> relations = knowledgeGraphService.generateDocumentRelations(userId);
            return JsonResult.success(relations);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    /**
     * 获取标签云数据
     */
    @GetMapping("/tag-cloud/{userId}")
    public JsonResult<List<Map<String, Object>>> getTagCloud(@PathVariable Long userId) {
        try {
            List<Map<String, Object>> tagCloud = knowledgeGraphService.generateTagCloud(userId);
            return JsonResult.success(tagCloud);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    /**
     * 获取学习路径分析
     */
    @GetMapping("/learning-path/{userId}")
    public JsonResult<Map<String, Object>> getLearningPath(@PathVariable Long userId) {
        try {
            Map<String, Object> learningPath = knowledgeGraphService.generateLearningPath(userId);
            return JsonResult.success(learningPath);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }
}