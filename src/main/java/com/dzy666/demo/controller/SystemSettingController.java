package com.dzy666.demo.controller;

import com.dzy666.demo.entity.SystemSetting;
import com.dzy666.demo.service.SystemSettingService;
import com.dzy666.demo.util.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemSettingController {

    @Autowired
    private SystemSettingService systemSettingService;

    /**
     * 获取所有系统设置
     */
    @GetMapping("/settings")
    public JsonResult<List<SystemSetting>> getAllSettings() {
        try {
            List<SystemSetting> settings = systemSettingService.getAllSettings();
            return JsonResult.success(settings);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    /**
     * 获取系统配置信息
     */
    @GetMapping("/info")
    public JsonResult<Map<String, Object>> getSystemInfo() {
        try {
            Map<String, Object> info = systemSettingService.getSystemInfo();
            return JsonResult.success(info);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    /**
     * 根据键获取系统设置
     */
    @GetMapping("/settings/{key}")
    public JsonResult<SystemSetting> getSettingByKey(@PathVariable String key) {
        try {
            SystemSetting setting = systemSettingService.getSettingByKey(key);
            if (setting == null) {
                return JsonResult.error("系统设置不存在");
            }
            return JsonResult.success(setting);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    /**
     * 更新系统设置
     */
    @PutMapping("/settings/{key}")
    public JsonResult<Boolean> updateSetting(@PathVariable String key,
                                             @RequestBody Map<String, Object> request) {
        try {
            String value = (String) request.get("value");
            Long userId = Long.valueOf(request.get("userId").toString());

            boolean success = systemSettingService.updateSetting(key, value, userId);
            return JsonResult.success("系统设置更新成功", success);
        } catch (Exception e) {
            return JsonResult.error("系统设置更新失败: " + e.getMessage());
        }
    }
}