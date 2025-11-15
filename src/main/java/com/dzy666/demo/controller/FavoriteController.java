package com.dzy666.demo.controller;

import com.dzy666.demo.entity.Document;
import com.dzy666.demo.service.FavoriteService;
import com.dzy666.demo.util.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorite")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    @PostMapping("/document/{documentId}")
    public JsonResult<Boolean> addFavorite(@PathVariable Long documentId,
                                           @RequestParam Long userId) {
        try {
            boolean success = favoriteService.addFavorite(documentId, userId);
            return JsonResult.success("收藏成功", success);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    @DeleteMapping("/document/{documentId}")
    public JsonResult<Boolean> removeFavorite(@PathVariable Long documentId,
                                              @RequestParam Long userId) {
        try {
            boolean success = favoriteService.removeFavorite(documentId, userId);
            return JsonResult.success("取消收藏成功", success);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    @GetMapping("/document/{documentId}")
    public JsonResult<Boolean> isFavorite(@PathVariable Long documentId,
                                          @RequestParam Long userId) {
        try {
            boolean isFavorite = favoriteService.isFavorite(documentId, userId);
            return JsonResult.success(isFavorite);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    @GetMapping("/user/{userId}")
    public JsonResult<List<Document>> getUserFavorites(@PathVariable Long userId) {
        try {
            List<Document> favorites = favoriteService.getUserFavorites(userId);
            return JsonResult.success(favorites);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    @GetMapping("/document/{documentId}/count")
    public JsonResult<Integer> getFavoriteCount(@PathVariable Long documentId) {
        try {
            int count = favoriteService.getFavoriteCount(documentId);
            return JsonResult.success(count);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }
}