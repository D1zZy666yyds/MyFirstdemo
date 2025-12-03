package com.dzy666.demo.mapper;

import com.dzy666.demo.entity.SearchHistory;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface SearchHistoryMapper {

    // 插入搜索历史
    @Insert("INSERT INTO search_history (user_id, keyword, result_count, search_time, search_type) " +
            "VALUES (#{userId}, #{keyword}, #{resultCount}, #{searchTime}, #{searchType})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SearchHistory searchHistory);

    // 获取用户的搜索历史
    @Select("SELECT * FROM search_history WHERE user_id = #{userId} ORDER BY search_time DESC LIMIT #{limit}")
    List<SearchHistory> findByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    // 清除用户的搜索历史
    @Delete("DELETE FROM search_history WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    // 🎯 新增：更新搜索结果数量 - 修复版
    @Update("UPDATE search_history SET result_count = #{resultCount}, search_time = NOW() " +
            "WHERE user_id = #{userId} AND keyword = #{keyword} " +
            "ORDER BY search_time DESC LIMIT 1")
    int updateResultCount(@Param("userId") Long userId,
                          @Param("keyword") String keyword,
                          @Param("resultCount") int resultCount);

    // 🎯 新增：检查搜索历史是否存在
    @Select("SELECT COUNT(*) FROM search_history " +
            "WHERE user_id = #{userId} AND keyword = #{keyword} " +
            "AND DATE(search_time) = CURDATE()")
    int existsToday(@Param("userId") Long userId, @Param("keyword") String keyword);

    // 获取搜索统计
    @Select("SELECT " +
            "COUNT(*) as total_searches, " +
            "COUNT(DISTINCT keyword) as unique_keywords, " +
            "AVG(result_count) as avg_results, " +
            "MAX(search_time) as last_search " +
            "FROM search_history WHERE user_id = #{userId}")
    Map<String, Object> getSearchStats(@Param("userId") Long userId);

    // 获取热门搜索词
    @Select("SELECT keyword, COUNT(*) as search_count, AVG(result_count) as avg_results " +
            "FROM search_history WHERE user_id = #{userId} " +
            "GROUP BY keyword ORDER BY search_count DESC LIMIT 10")
    List<Map<String, Object>> getPopularKeywords(@Param("userId") Long userId);

    // 🎯 新增：获取最近搜索词（用于搜索建议）
    @Select("SELECT DISTINCT keyword FROM search_history " +
            "WHERE user_id = #{userId} AND keyword LIKE CONCAT('%', #{prefix}, '%') " +
            "ORDER BY search_time DESC LIMIT #{limit}")
    List<String> findKeywordsByPrefix(@Param("userId") Long userId,
                                      @Param("prefix") String prefix,
                                      @Param("limit") int limit);
}