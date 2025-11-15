package com.dzy666.demo.service;

import com.dzy666.demo.entity.SystemSetting;
import com.dzy666.demo.mapper.SystemSettingMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SystemSettingService {

    @Autowired
    private SystemSettingMapper systemSettingMapper;

    @Autowired
    private OperationLogService operationLogService;

    /**
     * 获取所有系统设置
     */
    public List<SystemSetting> getAllSettings() {
        return systemSettingMapper.selectAll();
    }

    /**
     * 根据键获取系统设置
     */
    public SystemSetting getSettingByKey(String key) {
        return systemSettingMapper.selectByKey(key);
    }

    /**
     * 获取系统设置值
     */
    public String getSettingValue(String key) {
        SystemSetting setting = systemSettingMapper.selectByKey(key);
        return setting != null ? setting.getSettingValue() : null;
    }

    /**
     * 获取布尔类型的系统设置
     */
    public boolean getBooleanSetting(String key) {
        String value = getSettingValue(key);
        return Boolean.parseBoolean(value);
    }

    /**
     * 获取整数类型的系统设置
     */
    public int getIntSetting(String key) {
        String value = getSettingValue(key);
        return value != null ? Integer.parseInt(value) : 0;
    }

    /**
     * 更新系统设置
     */
    @Transactional
    public boolean updateSetting(String key, String value, Long userId) {
        SystemSetting existing = systemSettingMapper.selectByKey(key);
        if (existing == null) {
            throw new RuntimeException("系统设置不存在: " + key);
        }

        int result = systemSettingMapper.updateValueByKey(key, value);

        if (result > 0) {
            operationLogService.logOperation(userId, "UPDATE", "SYSTEM", existing.getId(),
                    "更新系统设置: " + key + " = " + value);
        }

        return result > 0;
    }

    /**
     * 创建系统设置
     */
    @Transactional
    public boolean createSetting(SystemSetting setting, Long userId) {
        SystemSetting existing = systemSettingMapper.selectByKey(setting.getSettingKey());
        if (existing != null) {
            throw new RuntimeException("系统设置已存在: " + setting.getSettingKey());
        }

        setting.setCreatedBy(userId);
        int result = systemSettingMapper.insert(setting);

        if (result > 0) {
            operationLogService.logOperation(userId, "CREATE", "SYSTEM", setting.getId(),
                    "创建系统设置: " + setting.getSettingKey());
        }

        return result > 0;
    }

    /**
     * 删除系统设置
     */
    @Transactional
    public boolean deleteSetting(String key, Long userId) {
        SystemSetting setting = systemSettingMapper.selectByKey(key);
        if (setting == null) {
            throw new RuntimeException("系统设置不存在: " + key);
        }

        int result = systemSettingMapper.deleteByKey(key);

        if (result > 0) {
            operationLogService.logOperation(userId, "DELETE", "SYSTEM", setting.getId(),
                    "删除系统设置: " + key);
        }

        return result > 0;
    }

    /**
     * 获取系统配置信息
     */
    public Map<String, Object> getSystemInfo() {
        Map<String, Object> info = new HashMap<>();

        // 系统基本信息
        info.put("appName", getSettingValue("app.name"));
        info.put("appVersion", getSettingValue("app.version"));
        info.put("backupAutoEnabled", getBooleanSetting("backup.auto_enabled"));
        info.put("backupAutoInterval", getIntSetting("backup.auto_interval"));
        info.put("searchEnableLucene", getBooleanSetting("search.enable_lucene"));
        info.put("uiTheme", getSettingValue("ui.theme"));
        info.put("uiLanguage", getSettingValue("ui.language"));

        // 运行时信息
        Runtime runtime = Runtime.getRuntime();
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("osName", System.getProperty("os.name"));
        info.put("totalMemory", runtime.totalMemory() / (1024 * 1024) + " MB");
        info.put("freeMemory", runtime.freeMemory() / (1024 * 1024) + " MB");
        info.put("maxMemory", runtime.maxMemory() / (1024 * 1024) + " MB");

        return info;
    }

    /**
     * 初始化默认系统设置
     */
    @Transactional
    public void initializeDefaultSettings(Long userId) {
        // 应用设置
        createOrUpdateSetting("app.name", "个人知识库管理系统", "STRING", "应用名称", userId);
        createOrUpdateSetting("app.version", "1.0.0", "STRING", "应用版本", userId);

        // 备份设置
        createOrUpdateSetting("backup.auto_enabled", "false", "BOOLEAN", "自动备份开关", userId);
        createOrUpdateSetting("backup.auto_interval", "7", "NUMBER", "自动备份间隔(天)", userId);

        // 搜索设置
        createOrUpdateSetting("search.enable_lucene", "true", "BOOLEAN", "启用Lucene全文检索", userId);

        // 界面设置
        createOrUpdateSetting("ui.theme", "light", "STRING", "界面主题", userId);
        createOrUpdateSetting("ui.language", "zh-CN", "STRING", "界面语言", userId);
    }

    /**
     * 创建或更新系统设置
     */
    private void createOrUpdateSetting(String key, String value, String type, String description, Long userId) {
        SystemSetting existing = systemSettingMapper.selectByKey(key);
        if (existing != null) {
            systemSettingMapper.updateValueByKey(key, value);
        } else {
            SystemSetting setting = new SystemSetting();
            setting.setSettingKey(key);
            setting.setSettingValue(value);
            setting.setSettingType(type);
            setting.setDescription(description);
            setting.setCreatedBy(userId);
            systemSettingMapper.insert(setting);
        }
    }
}