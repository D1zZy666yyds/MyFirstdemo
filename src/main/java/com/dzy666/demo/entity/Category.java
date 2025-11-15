package com.dzy666.demo.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class Category {
    private Long id;
    private String name;
    private Long parentId;
    private Long userId;
    private Integer sortOrder;
    private LocalDateTime createdTime;

    // 用于树形结构的子分类列表
    private List<Category> children;

    // 文档数量（用于前端显示）
    private Integer documentCount;
}