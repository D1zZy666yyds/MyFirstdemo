package com.dzy666.demo.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class Document {
    private Long id;
    private String title;
    private String content;
    private ContentType contentType;
    private Long categoryId;
    private Long userId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedTime;

    // 添加删除状态字段
    private Boolean deleted;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime deletedTime;

    // 收藏相关字段
    private Boolean isFavorite;
    private Integer favoriteCount;

    // 标签列表
    private List<Tag> tags;

    public enum ContentType {
        MARKDOWN, RICH_TEXT
    }
}