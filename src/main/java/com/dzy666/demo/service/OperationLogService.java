package com.dzy666.demo.service;

import com.dzy666.demo.entity.OperationLog;
import com.dzy666.demo.mapper.OperationLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OperationLogService {

    @Autowired
    private OperationLogMapper operationLogMapper;

    /**
     * 记录操作日志
     */
    @Transactional
    public void logOperation(OperationLog operationLog) {
        operationLogMapper.insert(operationLog);
    }

    /**
     * 记录操作日志（简化版）
     */
    @Transactional
    public void logOperation(Long userId, String operationType,
                             String targetType, Long targetId, String description) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setOperationType(operationType);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDescription(description);

        logOperation(log);
    }

    /**
     * 记录带IP和UserAgent的操作日志
     */
    @Transactional
    public void logOperationWithIp(Long userId, String operationType,
                                   String targetType, Long targetId,
                                   String description, String ipAddress) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setOperationType(operationType);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDescription(description);
        log.setIpAddress(ipAddress);

        logOperation(log);
    }

    /**
     * 获取用户的操作日志
     */
    public List<OperationLog> getUserLogs(Long userId) {
        return operationLogMapper.selectByUserId(userId);
    }

    /**
     * 获取最近的操作日志
     */
    public List<OperationLog> getRecentLogs(int limit) {
        return operationLogMapper.selectRecentLogs(limit);
    }

    /**
     * 根据操作类型获取日志
     */
    public List<OperationLog> getLogsByOperationType(String operationType) {
        return operationLogMapper.selectByOperationType(operationType);
    }

    /**
     * 清理旧日志（保留30天）
     */
    @Transactional
    public int cleanOldLogs() {
        return operationLogMapper.deleteOldLogs();
    }

    /**
     * 获取用户操作统计
     */
    public int getUserOperationCount(Long userId) {
        return operationLogMapper.countByUserId(userId);
    }
}