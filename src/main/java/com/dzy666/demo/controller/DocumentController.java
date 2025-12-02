package com.dzy666.demo.controller;

import com.dzy666.demo.entity.Document;
import com.dzy666.demo.service.DocumentService;
import com.dzy666.demo.util.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/document")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    // 原有接口保持不变...
    @PostMapping
    public JsonResult<Document> createDocument(@RequestBody Document document,
                                               HttpServletRequest request) {
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

    // 🔄 新增接口 - 文档版本管理和高级功能

    /**
     * 批量删除文档
     */
    @PostMapping("/batch-delete")
    public JsonResult<Boolean> batchDeleteDocuments(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> documentIds = (List<Long>) request.get("documentIds");
            Long userId = Long.valueOf(request.get("userId").toString());

            boolean success = documentService.batchDeleteDocuments(documentIds, userId);
            return JsonResult.success(success ? "批量删除成功" : "批量删除失败", success);
        } catch (Exception e) {
            return JsonResult.error("批量删除失败: " + e.getMessage());
        }
    }

    /**
     * 获取文档版本历史
     */
    @GetMapping("/{documentId}/versions")
    public JsonResult<List<Document>> getDocumentVersions(@PathVariable Long documentId,
                                                          @RequestParam Long userId) {
        try {
            List<Document> versions = documentService.getDocumentVersions(documentId, userId);
            return JsonResult.success(versions);
        } catch (Exception e) {
            return JsonResult.error("获取版本历史失败: " + e.getMessage());
        }
    }

    /**
     * 恢复到指定版本
     */
    @PostMapping("/{documentId}/restore/{versionId}")
    public JsonResult<Document> restoreDocumentVersion(@PathVariable Long documentId,
                                                       @PathVariable Long versionId,
                                                       @RequestParam Long userId) {
        try {
            Document restored = documentService.restoreToVersion(documentId, versionId, userId);
            return JsonResult.success("版本恢复成功", restored);
        } catch (Exception e) {
            return JsonResult.error("版本恢复失败: " + e.getMessage());
        }
    }

    /**
     * 文档分享（生成分享链接）
     */
    @PostMapping("/{documentId}/share")
    public JsonResult<Map<String, Object>> shareDocument(@PathVariable Long documentId,
                                                         @RequestParam Long userId,
                                                         @RequestParam(defaultValue = "24") Integer expireHours) {
        try {
            Map<String, Object> shareInfo = documentService.generateShareInfo(documentId, userId, expireHours);
            return JsonResult.success("文档分享链接生成成功", shareInfo);
        } catch (Exception e) {
            return JsonResult.error("分享失败: " + e.getMessage());
        }
    }

    /**
     * 通过分享token获取文档
     */
    @GetMapping("/shared/{shareToken}")
    public JsonResult<Document> getSharedDocument(@PathVariable String shareToken) {
        try {
            Document document = documentService.getDocumentByShareToken(shareToken);
            return JsonResult.success(document);
        } catch (Exception e) {
            return JsonResult.error("获取分享文档失败: " + e.getMessage());
        }
    }

    /**
     * 文档收藏状态切换
     */
    @PostMapping("/{documentId}/toggle-favorite")
    public JsonResult<Map<String, Object>> toggleFavorite(@PathVariable Long documentId,
                                                          @RequestParam Long userId) {
        try {
            Map<String, Object> result = documentService.toggleFavorite(documentId, userId);
            String message = (Boolean) result.get("isFavorited") ? "收藏成功" : "取消收藏成功";
            return JsonResult.success(message, result);
        } catch (Exception e) {
            return JsonResult.error("操作失败: " + e.getMessage());
        }
    }

    /**
     * 获取最近编辑的文档
     */
    @GetMapping("/user/{userId}/recent")
    public JsonResult<List<Document>> getRecentDocuments(@PathVariable Long userId,
                                                         @RequestParam(defaultValue = "10") int limit) {
        try {
            List<Document> recentDocuments = documentService.getRecentDocuments(userId, limit);
            return JsonResult.success(recentDocuments);
        } catch (Exception e) {
            return JsonResult.error("获取最近文档失败: " + e.getMessage());
        }
    }

    /**
     * 复制文档
     */
    @PostMapping("/{documentId}/copy")
    public JsonResult<Document> copyDocument(@PathVariable Long documentId,
                                             @RequestParam Long userId) {
        try {
            Document copiedDocument = documentService.copyDocument(documentId, userId);
            return JsonResult.success("文档复制成功", copiedDocument);
        } catch (Exception e) {
            return JsonResult.error("文档复制失败: " + e.getMessage());
        }
    }

    /**
     * 文档导入（从文本创建）
     */
    @PostMapping("/import")
    public JsonResult<Document> importDocument(@RequestBody Map<String, Object> importRequest) {
        try {
            String title = (String) importRequest.get("title");
            String content = (String) importRequest.get("content");
            Long categoryId = importRequest.get("categoryId") != null ?
                    Long.valueOf(importRequest.get("categoryId").toString()) : null;
            Long userId = Long.valueOf(importRequest.get("userId").toString());

            Document document = documentService.importDocument(title, content, categoryId, userId);
            return JsonResult.success("文档导入成功", document);
        } catch (Exception e) {
            return JsonResult.error("文档导入失败: " + e.getMessage());
        }
    }
    @GetMapping("/deleted/{userId}")
    public JsonResult<List<Document>> getDeletedDocuments(@PathVariable Long userId) {
        try {
            List<Document> deletedDocs = documentService.getDeletedDocuments(userId);
            return JsonResult.success(deletedDocs);
        } catch (Exception e) {
            return JsonResult.error("获取回收站失败: " + e.getMessage());
        }
    }

    @PutMapping("/restore/{id}")
    public JsonResult<Boolean> restoreDocument(@PathVariable Long id,
                                               @RequestParam Long userId) {
        try {
            boolean success = documentService.restoreDocument(id, userId);
            return JsonResult.success(success ? "文档恢复成功" : "文档恢复失败", success);
        } catch (Exception e) {
            return JsonResult.error("恢复文档失败: " + e.getMessage());
        }
    }

    /**
     * 永久删除文档
     */
    @DeleteMapping("/permanent/{id}")
    public JsonResult<Boolean> permanentDelete(@PathVariable Long id,
                                               @RequestParam Long userId) {
        try {
            boolean success = documentService.permanentDelete(id, userId);
            return JsonResult.success(success ? "文档已永久删除" : "删除失败", success);
        } catch (Exception e) {
            return JsonResult.error("永久删除失败: " + e.getMessage());
        }
    }

    /**
     * 清空回收站
     */
    @DeleteMapping("/recycle-bin/clear")
    public JsonResult<Boolean> clearRecycleBin(@RequestParam Long userId) {
        try {
            boolean success = documentService.clearRecycleBin(userId);
            return JsonResult.success(success ? "回收站已清空" : "清空失败", success);
        } catch (Exception e) {
            return JsonResult.error("清空回收站失败: " + e.getMessage());
        }
    }
}