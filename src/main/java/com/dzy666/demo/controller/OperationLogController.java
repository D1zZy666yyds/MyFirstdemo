package com.dzy666.demo.controller;

import com.dzy666.demo.entity.OperationLog;
import com.dzy666.demo.service.OperationLogService;
import com.dzy666.demo.util.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/operation-logs")
public class OperationLogController {

    @Autowired
    private OperationLogService operationLogService;

    /**
     * 获取用户的操作日志
     */
    @GetMapping("/user/{userId}")
    public JsonResult<List<OperationLog>> getUserLogs(@PathVariable Long userId) {
        try {
            List<OperationLog> logs = operationLogService.getUserLogs(userId);
            return JsonResult.success(logs);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    /**
     * 获取最近的操作日志
     */
    @GetMapping("/recent")
    public JsonResult<List<OperationLog>> getRecentLogs(@RequestParam(defaultValue = "50") int limit) {
        try {
            List<OperationLog> logs = operationLogService.getRecentLogs(limit);
            return JsonResult.success(logs);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    /**
     * 根据操作类型获取日志
     */
    @GetMapping("/type/{operationType}")
    public JsonResult<List<OperationLog>> getLogsByType(@PathVariable String operationType) {
        try {
            List<OperationLog> logs = operationLogService.getLogsByOperationType(operationType);
            return JsonResult.success(logs);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }

    /**
     * 清理旧日志
     */
    @DeleteMapping("/clean")
    public JsonResult<Integer> cleanOldLogs() {
        try {
            int deletedCount = operationLogService.cleanOldLogs();
            return JsonResult.success("清理完成，删除了 " + deletedCount + " 条旧日志", deletedCount);
        } catch (Exception e) {
            return JsonResult.error(e.getMessage());
        }
    }
}