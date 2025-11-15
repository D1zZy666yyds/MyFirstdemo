package com.dzy666.demo.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DataBackup {
    private Long id;
    private String backupName;
    private String backupType;  // FULL, INCREMENTAL
    private String filePath;
    private Long fileSize;
    private String description;
    private Long createdBy;
    private LocalDateTime createdTime;
    private String status;      // PROCESSING, COMPLETED, FAILED

    public enum BackupType {
        FULL, INCREMENTAL
    }

    public enum BackupStatus {
        PROCESSING, COMPLETED, FAILED
    }
}