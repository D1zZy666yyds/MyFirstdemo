package com.dzy666.demo.controller;

import com.dzy666.demo.entity.Document;
import com.dzy666.demo.service.DocumentService;
import com.dzy666.demo.util.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recycle-bin")
public class RecycleBinController {

    @Autowired
    private DocumentService documentService;

    /**
     * 获取回收站中的文档
     */
    @GetMapping("/user/{userId}")
    public JsonResult<List<Document>> getDeletedDocuments(@PathVariable Long userId) {
        try {
            List<Document> documents = documentService.getDeletedDocuments(userId);
            return JsonResult.success(documents);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    /**
     * 恢复文档
     */
    @PostMapping("/restore/{documentId}")
    public JsonResult<Boolean> restoreDocument(@PathVariable Long documentId,
                                               @RequestParam Long userId) {
        try {
            boolean success = documentService.restoreDocument(documentId, userId);
            return JsonResult.success("文档恢复成功", success);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    /**
     * 彻底删除文档
     */
    @DeleteMapping("/permanent/{documentId}")
    public JsonResult<Boolean> permanentDelete(@PathVariable Long documentId,
                                               @RequestParam Long userId) {
        try {
            boolean success = documentService.permanentDelete(documentId, userId);
            return JsonResult.success("文档已彻底删除", success);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    /**
     * 清空回收站
     */
    @DeleteMapping("/clear")
    public JsonResult<Boolean> clearRecycleBin(@RequestParam Long userId) {
        try {
            boolean success = documentService.clearRecycleBin(userId);
            return JsonResult.success("回收站已清空", success);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }
}