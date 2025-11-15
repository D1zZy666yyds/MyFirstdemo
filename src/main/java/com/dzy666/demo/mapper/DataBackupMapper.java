package com.dzy666.demo.mapper;

import com.dzy666.demo.entity.DataBackup;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DataBackupMapper {

    @Insert("INSERT INTO data_backups(backup_name, backup_type, file_path, file_size, description, created_by, status, created_time) " +
            "VALUES(#{backupName}, #{backupType}, #{filePath}, #{fileSize}, #{description}, #{createdBy}, #{status}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DataBackup dataBackup);

    @Select("SELECT id, backup_name as backupName, backup_type as backupType, file_path as filePath, " +
            "file_size as fileSize, description, created_by as createdBy, created_time as createdTime, status " +
            "FROM data_backups WHERE id = #{id}")
    DataBackup selectById(Long id);

    @Select("SELECT id, backup_name as backupName, backup_type as backupType, file_path as filePath, " +
            "file_size as fileSize, description, created_by as createdBy, created_time as createdTime, status " +
            "FROM data_backups WHERE created_by = #{userId} ORDER BY created_time DESC")
    List<DataBackup> selectByUserId(Long userId);

    @Select("SELECT id, backup_name as backupName, backup_type as backupType, file_path as filePath, " +
            "file_size as fileSize, description, created_by as createdBy, created_time as createdTime, status " +
            "FROM data_backups ORDER BY created_time DESC LIMIT #{limit}")
    List<DataBackup> selectRecentBackups(@Param("limit") int limit);

    @Update("UPDATE data_backups SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Delete("DELETE FROM data_backups WHERE id = #{id}")
    int deleteById(Long id);

    @Select("SELECT COUNT(*) FROM data_backups WHERE created_by = #{userId}")
    int countByUserId(Long userId);
}