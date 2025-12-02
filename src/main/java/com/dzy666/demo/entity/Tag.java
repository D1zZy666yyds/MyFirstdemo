package com.dzy666.demo.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Tag {
    private Long id;
    private String name;
    private Long userId;
    private LocalDateTime createdTime;
    // 新增文档计数字段
    private Integer documentCount;
}