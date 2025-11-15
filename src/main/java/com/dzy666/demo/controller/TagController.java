package com.dzy666.demo.controller;

import com.dzy666.demo.entity.Tag;
import com.dzy666.demo.service.TagService;
import com.dzy666.demo.util.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tag")
public class TagController {

    @Autowired
    private TagService tagService;

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
}