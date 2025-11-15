package com.dzy666.demo.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OperationLog {
    private Long id;
    private Long userId;
    private String operationType;  // CREATE, UPDATE, DELETE, LOGIN, etc.
    private String targetType;     // USER, DOCUMENT, CATEGORY, TAG
    private Long targetId;
    private String description;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdTime;

    public enum OperationType {
        CREATE, UPDATE, DELETE, LOGIN, LOGOUT, SEARCH, EXPORT, IMPORT
    }

    public enum TargetType {
        USER, DOCUMENT, CATEGORY, TAG, FAVORITE, SYSTEM
    }
}