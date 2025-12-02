package com.dzy666.demo.controller;

import com.dzy666.demo.entity.Tag;
import com.dzy666.demo.entity.Document;
import com.dzy666.demo.service.TagService;
import com.dzy666.demo.service.DocumentService;
import com.dzy666.demo.util.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tag")
public class TagController {

    @Autowired
    private TagService tagService;

    @Autowired
    private DocumentService documentService;

    @PostMapping
    public JsonResult<Tag> createTag(@RequestBody Tag tag) {
        try {
            Tag created = tagService.createTag(tag);
            return JsonResult.success("标签创建成功", created);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public JsonResult<Tag> getTag(@PathVariable Long id,
                                  @RequestParam Long userId) {
        try {
            Tag tag = tagService.getTagById(id, userId);
            if (tag == null) {
                return JsonResult.error("标签不存在");
            }
            return JsonResult.success(tag);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    @GetMapping("/user/{userId}")
    public JsonResult<List<Tag>> getUserTags(@PathVariable Long userId) {
        try {
            List<Tag> tags = tagService.getUserTags(userId);
            return JsonResult.success(tags);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public JsonResult<Tag> updateTag(@PathVariable Long id,
                                     @RequestBody Tag tag) {
        try {
            tag.setId(id);
            Tag updated = tagService.updateTag(tag);
            return JsonResult.success("标签更新成功", updated);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public JsonResult<Boolean> deleteTag(@PathVariable Long id,
                                         @RequestParam Long userId) {
        try {
            boolean success = tagService.deleteTag(id, userId);
            return JsonResult.success(success ? "删除成功" : "删除失败", success);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    @GetMapping("/document/{documentId}")
    public JsonResult<List<Tag>> getDocumentTags(@PathVariable Long documentId,
                                                 @RequestParam Long userId) {
        try {
            List<Tag> tags = tagService.getDocumentTags(documentId, userId);
            return JsonResult.success(tags);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    @PostMapping("/document/{documentId}/tag/{tagId}")
    public JsonResult<Boolean> addTagToDocument(@PathVariable Long documentId,
                                                @PathVariable Long tagId,
                                                @RequestParam Long userId) {
        try {
            boolean success = tagService.addTagToDocument(documentId, tagId, userId);
            return JsonResult.success("标签添加成功", success);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    @DeleteMapping("/document/{documentId}/tag/{tagId}")
    public JsonResult<Boolean> removeTagFromDocument(@PathVariable Long documentId,
                                                     @PathVariable Long tagId,
                                                     @RequestParam Long userId) {
        try {
            boolean success = tagService.removeTagFromDocument(documentId, tagId, userId);
            return JsonResult.success("标签移除成功", success);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    /**
     * 批量为文档设置标签
     */
    @PostMapping("/document/{documentId}/batch")
    public JsonResult<Boolean> setDocumentTags(@PathVariable Long documentId,
                                               @RequestBody List<Long> tagIds,
                                               @RequestParam Long userId) {
        try {
            boolean success = tagService.setDocumentTags(documentId, tagIds, userId);
            return JsonResult.success("标签设置成功", success);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    /**
     * 获取热门标签
     */
    @GetMapping("/popular/{userId}")
    public JsonResult<List<Tag>> getPopularTags(@PathVariable Long userId,
                                                @RequestParam(defaultValue = "10") int limit) {
        try {
            List<Tag> tags = tagService.getPopularTags(userId, limit);
            return JsonResult.success(tags);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    /**
     * 根据标签获取关联的文档
     */
    @GetMapping("/document/{tagId}/documents")
    public JsonResult<List<Document>> getDocumentsByTag(@PathVariable Long tagId,
                                                        @RequestParam Long userId) {
        try {
            // 获取文档ID列表
            List<Long> documentIds = tagService.getDocumentIdsByTag(tagId, userId);

            // 通过DocumentService获取文档详情
            List<Document> documents = documentService.getDocumentsByIds(documentIds, userId);

            return JsonResult.success(documents);
        } catch (Exception e) {
            return JsonResult.error("获取文档失败: " + e.getMessage());
        }
    }
}