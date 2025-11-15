package com.dzy666.demo.mapper;

import com.dzy666.demo.entity.OperationLog;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface OperationLogMapper {

    @Insert("INSERT INTO operation_logs(user_id, operation_type, target_type, target_id, description, ip_address, user_agent, created_time) " +
            "VALUES(#{userId}, #{operationType}, #{targetType}, #{targetId}, #{description}, #{ipAddress}, #{userAgent}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(OperationLog operationLog);

    @Select("SELECT id, user_id as userId, operation_type as operationType, target_type as targetType, " +
            "target_id as targetId, description, ip_address as ipAddress, user_agent as userAgent, " +
            "created_time as createdTime " +
            "FROM operation_logs WHERE user_id = #{userId} ORDER BY created_time DESC")
    List<OperationLog> selectByUserId(Long userId);

    @Select("SELECT id, user_id as userId, operation_type as operationType, target_type as targetType, " +
            "target_id as targetId, description, ip_address as ipAddress, user_agent as userAgent, " +
            "created_time as createdTime " +
            "FROM operation_logs ORDER BY created_time DESC LIMIT #{limit}")
    List<OperationLog> selectRecentLogs(@Param("limit") int limit);

    @Select("SELECT id, user_id as userId, operation_type as operationType, target_type as targetType, " +
            "target_id as targetId, description, ip_address as ipAddress, user_agent as userAgent, " +
            "created_time as createdTime " +
            "FROM operation_logs WHERE operation_type = #{operationType} ORDER BY created_time DESC")
    List<OperationLog> selectByOperationType(String operationType);

    @Select("SELECT COUNT(*) FROM operation_logs WHERE user_id = #{userId}")
    int countByUserId(Long userId);

    @Delete("DELETE FROM operation_logs WHERE created_time < DATE_SUB(NOW(), INTERVAL 30 DAY)")
    int deleteOldLogs();
}