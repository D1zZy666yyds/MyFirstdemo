package com.dzy666.demo.entity;

import java.time.LocalDateTime;

public class SearchHistory {
    private Long id;
    private Long userId;
    private String keyword;
    private Integer resultCount;
    private LocalDateTime searchTime;
    private String searchType;

    // 构造函数
    public SearchHistory() {}

    public SearchHistory(Long userId, String keyword, String searchType) {
        this.userId = userId;
        this.keyword = keyword;
        this.searchType = searchType;
        this.searchTime = LocalDateTime.now();
        this.resultCount = 0;
    }

    // Getter 和 Setter 方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Integer getResultCount() {
        return resultCount;
    }

    public void setResultCount(Integer resultCount) {
        this.resultCount = resultCount;
    }

    public LocalDateTime getSearchTime() {
        return searchTime;
    }

    public void setSearchTime(LocalDateTime searchTime) {
        this.searchTime = searchTime;
    }

    public String getSearchType() {
        return searchType;
    }

    public void setSearchType(String searchType) {
        this.searchType = searchType;
    }

    @Override
    public String toString() {
        return "SearchHistory{" +
                "id=" + id +
                ", userId=" + userId +
                ", keyword='" + keyword + '\'' +
                ", resultCount=" + resultCount +
                ", searchTime=" + searchTime +
                ", searchType='" + searchType + '\'' +
                '}';
    }
}