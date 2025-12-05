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

    // 🎯 修复：更新搜索结果数量 - 使用子查询
    @Update("UPDATE search_history SET result_count = #{resultCount}, search_time = NOW() " +
            "WHERE id = (" +
            "    SELECT id FROM (" +
            "        SELECT id FROM search_history " +
            "        WHERE user_id = #{userId} AND keyword = #{keyword} " +
            "        ORDER BY search_time DESC LIMIT 1" +
            "    ) AS temp" +
            ")")
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

    // 🎯 修复：获取最近搜索词（优化模糊查询）
    @Select("SELECT DISTINCT keyword FROM search_history " +
            "WHERE user_id = #{userId} " +
            "AND keyword LIKE CONCAT(#{prefix}, '%') " +
            "ORDER BY search_time DESC LIMIT #{limit}")
    List<String> findKeywordsByPrefix(@Param("userId") Long userId,
                                      @Param("prefix") String prefix,
                                      @Param("limit") int limit);

    // 🎯 新增：批量获取文档标签
    @Select({
            "<script>",
            "SELECT dt.document_id, t.id as tag_id, t.name as tag_name ",
            "FROM document_tag dt ",
            "JOIN tag t ON dt.tag_id = t.id ",
            "WHERE dt.document_id IN ",
            "<foreach collection='documentIds' item='id' open='(' separator=',' close=')'>",
            "   #{id}",
            "</foreach>",
            "AND t.user_id = #{userId}",
            "</script>"
    })
    List<Map<String, Object>> getDocumentsTagsBatch(@Param("documentIds") List<Long> documentIds,
                                                    @Param("userId") Long userId);
}