package com.dzy666.demo.controller;

import com.dzy666.demo.entity.Document;
import com.dzy666.demo.service.DocumentService;
import com.dzy666.demo.service.SearchService;
import com.dzy666.demo.util.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    @Autowired
    private SearchService searchService;

    @Autowired
    private DocumentService documentService;

    @GetMapping
    public JsonResult<List<Document>> search(@RequestParam String keyword,
                                             @RequestParam Long userId,
                                             @RequestParam(defaultValue = "10") int limit) {
        try {
            List<Long> docIds = searchService.search(keyword, userId, limit);
            List<Document> documents = documentService.getDocumentsByIds(docIds, userId);

            return JsonResult.success("搜索完成", documents);
        } catch (IOException e) {
            return JsonResult.error("搜索失败: " + e.getMessage());
        }
    }

    @PostMapping("/rebuild")
    public JsonResult<String> rebuildIndex(@RequestParam Long userId) {
        try {
            searchService.rebuildIndex(userId);
            return JsonResult.success("索引重建完成");
        } catch (IOException e) {
            return JsonResult.error("索引重建失败: " + e.getMessage());
        }
    }
}