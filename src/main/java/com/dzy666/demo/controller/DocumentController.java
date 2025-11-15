package com.dzy666.demo.controller;

import com.dzy666.demo.entity.Document;
import com.dzy666.demo.service.DocumentService;
import com.dzy666.demo.util.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest; // 添加这行导入
import java.util.List;

@RestController
@RequestMapping("/api/document")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @PostMapping
    public JsonResult<Document> createDocument(@RequestBody Document document,
                                               HttpServletRequest request) { // 这里使用了 HttpServletRequest
        try {
            Document created = documentService.createDocument(document);
            return JsonResult.success("文档创建成功", created);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public JsonResult<Document> getDocument(@PathVariable Long id,
                                            @RequestParam Long userId) {
        try {
            Document document = documentService.getDocument(id, userId);
            if (document == null) {
                return JsonResult.error("文档不存在");
            }
            return JsonResult.success(document);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    @GetMapping("/category/{categoryId}")
    public JsonResult<List<Document>> getDocumentsByCategory(@PathVariable Long categoryId,
                                                             @RequestParam Long userId) {
        try {
            List<Document> documents = documentService.getDocumentsByCategory(categoryId, userId);
            return JsonResult.success(documents);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    @GetMapping("/user/{userId}")
    public JsonResult<List<Document>> getUserDocuments(@PathVariable Long userId) {
        try {
            List<Document> documents = documentService.getUserDocuments(userId);
            return JsonResult.success(documents);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public JsonResult<Document> updateDocument(@PathVariable Long id,
                                               @RequestBody Document document) {
        try {
            document.setId(id);
            Document updated = documentService.updateDocument(document);
            return JsonResult.success("文档更新成功", updated);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public JsonResult<Boolean> deleteDocument(@PathVariable Long id,
                                              @RequestParam Long userId) {
        try {
            boolean success = documentService.deleteDocument(id, userId);
            return JsonResult.success(success ? "删除成功" : "删除失败", success);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }
}