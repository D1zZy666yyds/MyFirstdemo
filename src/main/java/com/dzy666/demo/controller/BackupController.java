package com.dzy666.demo.controller;

import com.dzy666.demo.entity.DataBackup;
import com.dzy666.demo.service.BackupService;
import com.dzy666.demo.util.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/backup")
public class BackupController {

    @Autowired
    private BackupService backupService;

    /**
     * 创建完整备份
     */
    @PostMapping("/full")
    public JsonResult<DataBackup> createFullBackup(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            String description = (String) request.get("description");

            DataBackup backup = backupService.createFullBackup(userId, description);
            return JsonResult.success("完整备份创建成功", backup);
        } catch (Exception e) {
            return JsonResult.error("备份创建失败: " + e.getMessage());
        }
    }

    /**
     * 创建增量备份
     */
    @PostMapping("/incremental")
    public JsonResult<DataBackup> createIncrementalBackup(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            String description = (String) request.get("description");
            LocalDateTime since = LocalDateTime.parse((String) request.get("since"));

            DataBackup backup = backupService.createIncrementalBackup(userId, description, since);
            return JsonResult.success("增量备份创建成功", backup);
        } catch (Exception e) {
            return JsonResult.error("增量备份创建失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户的备份列表
     */
    @GetMapping("/user/{userId}")
    public JsonResult<List<DataBackup>> getUserBackups(@PathVariable Long userId) {
        try {
            List<DataBackup> backups = backupService.getUserBackups(userId);
            return JsonResult.success(backups);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    /**
     * 获取备份详情
     */
    @GetMapping("/{backupId}")
    public JsonResult<DataBackup> getBackup(@PathVariable Long backupId) {
        try {
            DataBackup backup = backupService.getBackupById(backupId);
            if (backup == null) {
                return JsonResult.error("备份不存在");
            }
            return JsonResult.success(backup);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    /**
     * 删除备份
     */
    @DeleteMapping("/{backupId}")
    public JsonResult<Boolean> deleteBackup(@PathVariable Long backupId,
                                            @RequestParam Long userId) {
        try {
            boolean success = backupService.deleteBackup(backupId, userId);
            return JsonResult.success(success ? "备份删除成功" : "备份删除失败", success);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    /**
     * 获取备份统计
     */
    @GetMapping("/stats/{userId}")
    public JsonResult<Map<String, Object>> getBackupStats(@PathVariable Long userId) {
        try {
            Map<String, Object> stats = backupService.getBackupStats(userId);
            return JsonResult.success(stats);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    /**
     * 从备份恢复数据
     */
    @PostMapping("/restore/{backupId}")
    public JsonResult<Boolean> restoreFromBackup(@PathVariable Long backupId,
                                                 @RequestParam Long userId) {
        try {
            boolean success = backupService.restoreFromBackup(backupId, userId);
            return JsonResult.success("数据恢复成功", success);
        } catch (Exception e) {
            return JsonResult.error("数据恢复失败: " + e.getMessage());
        }
    }

    /**
     * 获取备份内容预览
     */
    @GetMapping("/preview/{backupId}")
    public JsonResult<Map<String, Object>> getBackupPreview(@PathVariable Long backupId,
                                                            @RequestParam Long userId) {
        try {
            Map<String, Object> preview = backupService.getBackupContent(backupId, userId);
            return JsonResult.success(preview);
        } catch (Exception e) {
            return JsonResult.error("获取备份预览失败: " + e.getMessage());
        }
    }
}