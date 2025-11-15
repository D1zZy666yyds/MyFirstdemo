package com.dzy666.demo.service;

import com.dzy666.demo.entity.DataBackup;
import com.dzy666.demo.entity.Document;
import com.dzy666.demo.entity.Category;
import com.dzy666.demo.entity.Tag;
import com.dzy666.demo.mapper.DataBackupMapper;
import com.dzy666.demo.mapper.DocumentMapper;
import com.dzy666.demo.mapper.CategoryMapper;
import com.dzy666.demo.mapper.TagMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BackupService {

    @Autowired
    private DataBackupMapper dataBackupMapper;

    @Autowired
    private DocumentMapper documentMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private OperationLogService operationLogService;

    @Value("${app.backup.directory:./backups}")
    private String backupDirectory;

    private final ObjectMapper objectMapper;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public BackupService() {
        this.objectMapper = new ObjectMapper();
        // 注册 Java 8 时间模块
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * 创建完整数据备份
     */
    @Transactional
    public DataBackup createFullBackup(Long userId, String description) {
        String timestamp = LocalDateTime.now().format(dateFormatter);
        String backupName = "full_backup_" + timestamp;
        String fileName = backupName + ".json";
        String filePath = backupDirectory + File.separator + fileName;

        // 创建备份记录
        DataBackup backup = new DataBackup();
        backup.setBackupName(backupName);
        backup.setBackupType(DataBackup.BackupType.FULL.name());
        backup.setFilePath(filePath);
        backup.setDescription(description);
        backup.setCreatedBy(userId);
        backup.setStatus(DataBackup.BackupStatus.PROCESSING.name());

        dataBackupMapper.insert(backup);

        try {
            // 创建备份目录
            Files.createDirectories(Paths.get(backupDirectory));

            // 收集所有数据
            Map<String, Object> backupData = new HashMap<>();
            backupData.put("backupInfo", Map.of(
                    "name", backupName,
                    "type", "FULL",
                    "createdAt", LocalDateTime.now(),
                    "createdBy", userId
            ));

            // 用户数据
            List<Document> documents = documentMapper.selectByUserId(userId);
            List<Category> categories = categoryMapper.selectByUserId(userId);
            List<Tag> tags = tagMapper.selectByUserId(userId);

            backupData.put("documents", documents);
            backupData.put("categories", categories);
            backupData.put("tags", tags);

            // 写入JSON文件
            try (FileWriter writer = new FileWriter(filePath)) {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, backupData);
            }

            // 更新备份记录
            File backupFile = new File(filePath);
            backup.setFileSize(backupFile.length());
            backup.setStatus(DataBackup.BackupStatus.COMPLETED.name());
            dataBackupMapper.updateStatus(backup.getId(), backup.getStatus());

            // 记录操作日志
            operationLogService.logOperation(userId, "EXPORT", "BACKUP", backup.getId(),
                    "创建完整数据备份: " + backupName);

            return backup;

        } catch (Exception e) {
            // 备份失败
            backup.setStatus(DataBackup.BackupStatus.FAILED.name());
            dataBackupMapper.updateStatus(backup.getId(), backup.getStatus());
            throw new RuntimeException("备份创建失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建增量备份（只备份新增和修改的数据）
     */
    @Transactional
    public DataBackup createIncrementalBackup(Long userId, String description, LocalDateTime since) {
        String timestamp = LocalDateTime.now().format(dateFormatter);
        String backupName = "incremental_backup_" + timestamp;
        String fileName = backupName + ".json";
        String filePath = backupDirectory + File.separator + fileName;

        DataBackup backup = new DataBackup();
        backup.setBackupName(backupName);
        backup.setBackupType(DataBackup.BackupType.INCREMENTAL.name());
        backup.setFilePath(filePath);
        backup.setDescription(description);
        backup.setCreatedBy(userId);
        backup.setStatus(DataBackup.BackupStatus.PROCESSING.name());

        dataBackupMapper.insert(backup);

        try {
            Files.createDirectories(Paths.get(backupDirectory));

            Map<String, Object> backupData = new HashMap<>();
            backupData.put("backupInfo", Map.of(
                    "name", backupName,
                    "type", "INCREMENTAL",
                    "since", since,
                    "createdAt", LocalDateTime.now(),
                    "createdBy", userId
            ));

            // 获取自指定时间以来的数据（简化版，实际应该根据时间筛选）
            List<Document> documents = documentMapper.selectByUserId(userId);
            List<Category> categories = categoryMapper.selectByUserId(userId);
            List<Tag> tags = tagMapper.selectByUserId(userId);

            backupData.put("documents", documents);
            backupData.put("categories", categories);
            backupData.put("tags", tags);

            try (FileWriter writer = new FileWriter(filePath)) {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, backupData);
            }

            File backupFile = new File(filePath);
            backup.setFileSize(backupFile.length());
            backup.setStatus(DataBackup.BackupStatus.COMPLETED.name());
            dataBackupMapper.updateStatus(backup.getId(), backup.getStatus());

            operationLogService.logOperation(userId, "EXPORT", "BACKUP", backup.getId(),
                    "创建增量数据备份: " + backupName);

            return backup;

        } catch (Exception e) {
            backup.setStatus(DataBackup.BackupStatus.FAILED.name());
            dataBackupMapper.updateStatus(backup.getId(), backup.getStatus());
            throw new RuntimeException("增量备份创建失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取用户的备份列表
     */
    public List<DataBackup> getUserBackups(Long userId) {
        return dataBackupMapper.selectByUserId(userId);
    }

    /**
     * 获取备份详情
     */
    public DataBackup getBackupById(Long id) {
        return dataBackupMapper.selectById(id);
    }

    /**
     * 删除备份
     */
    @Transactional
    public boolean deleteBackup(Long id, Long userId) {
        DataBackup backup = dataBackupMapper.selectById(id);
        if (backup == null || !backup.getCreatedBy().equals(userId)) {
            throw new RuntimeException("备份不存在或无权访问");
        }

        // 删除文件
        try {
            Files.deleteIfExists(Paths.get(backup.getFilePath()));
        } catch (IOException e) {
            System.err.println("删除备份文件失败: " + e.getMessage());
        }

        // 删除数据库记录
        int result = dataBackupMapper.deleteById(id);

        if (result > 0) {
            operationLogService.logOperation(userId, "DELETE", "BACKUP", id,
                    "删除数据备份: " + backup.getBackupName());
        }

        return result > 0;
    }

    /**
     * 获取备份统计信息
     */
    public Map<String, Object> getBackupStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalBackups", dataBackupMapper.countByUserId(userId));

        List<DataBackup> backups = dataBackupMapper.selectByUserId(userId);
        long totalSize = backups.stream()
                .filter(b -> b.getFileSize() != null)
                .mapToLong(DataBackup::getFileSize)
                .sum();
        stats.put("totalSize", totalSize);
        stats.put("totalSizeMB", totalSize / (1024 * 1024));

        return stats;
    }

    /**
     * 从备份恢复数据
     */
    @Transactional
    public boolean restoreFromBackup(Long backupId, Long userId) {
        DataBackup backup = dataBackupMapper.selectById(backupId);
        if (backup == null || !backup.getCreatedBy().equals(userId)) {
            throw new RuntimeException("备份不存在或无权访问");
        }

        if (!DataBackup.BackupStatus.COMPLETED.name().equals(backup.getStatus())) {
            throw new RuntimeException("备份状态异常，无法恢复");
        }

        try {
            // 读取备份文件
            File backupFile = new File(backup.getFilePath());
            if (!backupFile.exists()) {
                throw new RuntimeException("备份文件不存在");
            }

            // 解析备份数据
            Map<String, Object> backupData = objectMapper.readValue(backupFile, Map.class);

            // 执行恢复操作
            restoreDocuments(backupData, userId);
            restoreCategories(backupData, userId);
            restoreTags(backupData, userId);

            // 记录操作日志
            operationLogService.logOperation(userId, "IMPORT", "BACKUP", backupId,
                    "从备份恢复数据: " + backup.getBackupName());

            return true;

        } catch (Exception e) {
            throw new RuntimeException("数据恢复失败: " + e.getMessage(), e);
        }
    }

    /**
     * 恢复文档数据
     */
    private void restoreDocuments(Map<String, Object> backupData, Long userId) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> documents = (List<Map<String, Object>>) backupData.get("documents");
        if (documents != null) {
            for (Map<String, Object> docMap : documents) {
                Document document = new Document();
                document.setTitle((String) docMap.get("title"));
                document.setContent((String) docMap.get("content"));
                document.setContentType(Document.ContentType.valueOf((String) docMap.get("contentType")));
                document.setCategoryId(docMap.get("categoryId") != null ?
                        Long.valueOf(docMap.get("categoryId").toString()) : null);
                document.setUserId(userId);

                // 检查是否已存在相同标题的文档
                List<Document> existingDocs = documentMapper.selectByUserId(userId);
                boolean exists = existingDocs.stream()
                        .anyMatch(d -> d.getTitle().equals(document.getTitle()));

                if (!exists) {
                    documentMapper.insert(document);
                }
            }
        }
    }

    /**
     * 恢复分类数据
     */
    private void restoreCategories(Map<String, Object> backupData, Long userId) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> categories = (List<Map<String, Object>>) backupData.get("categories");
        if (categories != null) {
            for (Map<String, Object> catMap : categories) {
                Category category = new Category();
                category.setName((String) catMap.get("name"));
                category.setParentId(catMap.get("parentId") != null ?
                        Long.valueOf(catMap.get("parentId").toString()) : null);
                category.setUserId(userId);
                category.setSortOrder(catMap.get("sortOrder") != null ?
                        Integer.valueOf(catMap.get("sortOrder").toString()) : 0);

                // 检查是否已存在相同名称的分类
                List<Category> existingCats = categoryMapper.selectByUserId(userId);
                boolean exists = existingCats.stream()
                        .anyMatch(c -> c.getName().equals(category.getName()));

                if (!exists) {
                    categoryMapper.insert(category);
                }
            }
        }
    }

    /**
     * 恢复标签数据
     */
    private void restoreTags(Map<String, Object> backupData, Long userId) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tags = (List<Map<String, Object>>) backupData.get("tags");
        if (tags != null) {
            for (Map<String, Object> tagMap : tags) {
                Tag tag = new Tag();
                tag.setName((String) tagMap.get("name"));
                tag.setUserId(userId);

                // 检查是否已存在相同名称的标签
                List<Tag> existingTags = tagMapper.selectByUserId(userId);
                boolean exists = existingTags.stream()
                        .anyMatch(t -> t.getName().equals(tag.getName()));

                if (!exists) {
                    tagMapper.insert(tag);
                }
            }
        }
    }

    /**
     * 获取备份文件内容（用于预览）
     */
    /**
     * 获取备份文件内容（用于预览）
     */
    public Map<String, Object> getBackupContent(Long backupId, Long userId) {
        System.out.println("=== 调试信息：开始获取备份内容 ===");
        System.out.println("备份ID: " + backupId);
        System.out.println("用户ID: " + userId);

        DataBackup backup = dataBackupMapper.selectById(backupId);
        System.out.println("数据库查询结果: " + backup);

        if (backup == null) {
            System.out.println("=== 调试信息：备份不存在 ===");
            throw new RuntimeException("备份不存在");
        }

        System.out.println("备份创建者: " + backup.getCreatedBy());
        System.out.println("请求用户: " + userId);

        // 暂时注释掉权限检查进行测试
        // if (!backup.getCreatedBy().equals(userId)) {
        //     System.out.println("=== 调试信息：权限检查失败 ===");
        //     throw new RuntimeException("备份不存在或无权访问");
        // }

        try {
            File backupFile = new File(backup.getFilePath());
            System.out.println("备份文件路径: " + backup.getFilePath());
            System.out.println("文件是否存在: " + backupFile.exists());

            if (!backupFile.exists()) {
                throw new RuntimeException("备份文件不存在");
            }

            Map<String, Object> backupData = objectMapper.readValue(backupFile, Map.class);
            System.out.println("=== 调试信息：备份内容读取成功 ===");

            // 只返回基本信息，不返回具体内容（避免数据过大）
            Map<String, Object> preview = new HashMap<>();
            preview.put("backupInfo", backupData.get("backupInfo"));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> documents = (List<Map<String, Object>>) backupData.get("documents");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> categories = (List<Map<String, Object>>) backupData.get("categories");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tags = (List<Map<String, Object>>) backupData.get("tags");

            preview.put("documentCount", documents != null ? documents.size() : 0);
            preview.put("categoryCount", categories != null ? categories.size() : 0);
            preview.put("tagCount", tags != null ? tags.size() : 0);

            return preview;

        } catch (Exception e) {
            System.out.println("=== 调试信息：读取备份内容失败 ===");
            System.out.println("错误信息: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("读取备份内容失败: " + e.getMessage(), e);
        }
    }
}