package com.dzy666.demo.mapper;

import com.dzy666.demo.entity.SystemSetting;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SystemSettingMapper {

    @Insert("INSERT INTO system_settings(setting_key, setting_value, setting_type, description, created_by, created_time, updated_time) " +
            "VALUES(#{settingKey}, #{settingValue}, #{settingType}, #{description}, #{createdBy}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SystemSetting systemSetting);

    @Select("SELECT id, setting_key as settingKey, setting_value as settingValue, setting_type as settingType, " +
            "description, created_by as createdBy, created_time as createdTime, updated_time as updatedTime " +
            "FROM system_settings WHERE id = #{id}")
    SystemSetting selectById(Long id);

    @Select("SELECT id, setting_key as settingKey, setting_value as settingValue, setting_type as settingType, " +
            "description, created_by as createdBy, created_time as createdTime, updated_time as updatedTime " +
            "FROM system_settings WHERE setting_key = #{key}")
    SystemSetting selectByKey(String key);

    @Select("SELECT id, setting_key as settingKey, setting_value as settingValue, setting_type as settingType, " +
            "description, created_by as createdBy, created_time as createdTime, updated_time as updatedTime " +
            "FROM system_settings")
    List<SystemSetting> selectAll();

    @Update("UPDATE system_settings SET setting_value = #{settingValue}, updated_time = NOW() WHERE setting_key = #{key}")
    int updateValueByKey(@Param("key") String key, @Param("settingValue") String settingValue);

    @Update("UPDATE system_settings SET setting_value = #{settingValue}, setting_type = #{settingType}, " +
            "description = #{description}, updated_time = NOW() WHERE id = #{id}")
    int update(SystemSetting systemSetting);

    @Delete("DELETE FROM system_settings WHERE id = #{id}")
    int deleteById(Long id);

    @Delete("DELETE FROM system_settings WHERE setting_key = #{key}")
    int deleteByKey(String key);
}