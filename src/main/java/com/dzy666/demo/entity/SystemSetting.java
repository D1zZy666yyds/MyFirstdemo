package com.dzy666.demo.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SystemSetting {
    private Long id;
    private String settingKey;
    private String settingValue;
    private String settingType;  // STRING, NUMBER, BOOLEAN, JSON
    private String description;
    private Long createdBy;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public enum SettingType {
        STRING, NUMBER, BOOLEAN, JSON
    }
}