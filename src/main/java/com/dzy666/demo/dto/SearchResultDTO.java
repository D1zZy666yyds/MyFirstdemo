// SearchResultDTO.java
package com.dzy666.demo.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultDTO {
    private Long id;
    private String title;
    private String contentPreview;
    private Long categoryId;
    private String categoryName;
    private List<TagDTO> tags;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    private Double relevanceScore;  // Lucene相关性评分
    private String contentType;
}