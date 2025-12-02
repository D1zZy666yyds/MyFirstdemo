package com.dzy666.demo.service;

import com.dzy666.demo.entity.SearchHistory;
import com.dzy666.demo.mapper.SearchHistoryMapper;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private final String indexDir = "lucene-index";
    private SmartChineseAnalyzer analyzer = new SmartChineseAnalyzer();

    @Autowired
    @Lazy
    private DocumentService documentService;

    @Autowired
    private SearchHistoryMapper searchHistoryMapper;

    @Autowired
    private TagService tagService;

    /**
     * 为文档创建索引
     */
    public void indexDocument(com.dzy666.demo.entity.Document doc) throws IOException {
        Directory directory = FSDirectory.open(Paths.get(indexDir));
        IndexWriterConfig config = new IndexWriterConfig(analyzer);

        try (IndexWriter writer = new IndexWriter(directory, config)) {
            // 删除旧索引（如果存在）
            writer.deleteDocuments(new Term("id", doc.getId().toString()));

            // 创建新文档
            Document luceneDoc = new Document();
            luceneDoc.add(new StringField("id", doc.getId().toString(), Field.Store.YES));
            luceneDoc.add(new TextField("title", doc.getTitle(), Field.Store.YES));
            luceneDoc.add(new TextField("content", doc.getContent(), Field.Store.YES));
            luceneDoc.add(new LongPoint("userId", doc.getUserId()));
            luceneDoc.add(new StoredField("userId", doc.getUserId()));

            // 添加分类和标签信息用于高级搜索
            if (doc.getCategoryId() != null) {
                luceneDoc.add(new LongPoint("categoryId", doc.getCategoryId()));
                luceneDoc.add(new StoredField("categoryId", doc.getCategoryId()));
            }

            // 添加创建时间用于日期范围搜索
            if (doc.getCreatedTime() != null) {
                long createdTimeMillis = doc.getCreatedTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                luceneDoc.add(new LongPoint("createdTime", createdTimeMillis));
                luceneDoc.add(new StoredField("createdTime", createdTimeMillis));
            }

            writer.addDocument(luceneDoc);
            writer.commit();
        }
    }

    /**
     * 搜索文档 - 基础搜索
     */
    public List<Long> search(String keyword, Long userId, int limit) throws IOException {
        // 记录搜索历史
        saveSearchHistory(userId, keyword, "BASIC", 0); // 结果数量稍后更新

        Directory directory = FSDirectory.open(Paths.get(indexDir));
        List<Long> results = new ArrayList<>();

        try (IndexReader reader = DirectoryReader.open(directory)) {
            IndexSearcher searcher = new IndexSearcher(reader);

            // 构建查询：搜索标题和内容，并且限制用户
            BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();

            // 标题搜索 - 使用模糊查询提高召回率
            Query titleQuery = new FuzzyQuery(new Term("title", keyword));
            booleanQuery.add(titleQuery, BooleanClause.Occur.SHOULD);

            // 内容搜索
            Query contentQuery = new FuzzyQuery(new Term("content", keyword));
            booleanQuery.add(contentQuery, BooleanClause.Occur.SHOULD);

            // 用户过滤
            Query userQuery = LongPoint.newExactQuery("userId", userId);
            booleanQuery.add(userQuery, BooleanClause.Occur.MUST);

            TopDocs topDocs = searcher.search(booleanQuery.build(), limit);

            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                org.apache.lucene.document.Document doc = searcher.doc(scoreDoc.doc);
                results.add(Long.parseLong(doc.get("id")));
            }

            // 更新搜索历史的结果数量
            updateSearchHistoryResultCount(userId, keyword, results.size());
        }

        return results;
    }

    /**
     * 高级搜索
     */
    public List<Long> advancedSearch(String keyword, Long categoryId, List<Long> tagIds,
                                     String dateRange, Long userId, int limit) throws IOException {
        // 记录高级搜索历史
        saveSearchHistory(userId, keyword, "ADVANCED", 0);

        Directory directory = FSDirectory.open(Paths.get(indexDir));
        List<Long> results = new ArrayList<>();

        try (IndexReader reader = DirectoryReader.open(directory)) {
            IndexSearcher searcher = new IndexSearcher(reader);

            BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();

            // 关键词搜索（标题和内容）
            if (keyword != null && !keyword.trim().isEmpty()) {
                Query titleQuery = new FuzzyQuery(new Term("title", keyword));
                Query contentQuery = new FuzzyQuery(new Term("content", keyword));

                BooleanQuery.Builder keywordQuery = new BooleanQuery.Builder();
                keywordQuery.add(titleQuery, BooleanClause.Occur.SHOULD);
                keywordQuery.add(contentQuery, BooleanClause.Occur.SHOULD);

                booleanQuery.add(keywordQuery.build(), BooleanClause.Occur.MUST);
            }

            // 用户过滤
            Query userQuery = LongPoint.newExactQuery("userId", userId);
            booleanQuery.add(userQuery, BooleanClause.Occur.MUST);

            // 分类过滤
            if (categoryId != null) {
                Query categoryQuery = LongPoint.newExactQuery("categoryId", categoryId);
                booleanQuery.add(categoryQuery, BooleanClause.Occur.MUST);
            }

            // 日期范围过滤
            if (dateRange != null && !dateRange.isEmpty()) {
                long[] dateRangeMillis = parseDateRange(dateRange);
                if (dateRangeMillis != null) {
                    Query dateQuery = LongPoint.newRangeQuery("createdTime", dateRangeMillis[0], dateRangeMillis[1]);
                    booleanQuery.add(dateQuery, BooleanClause.Occur.MUST);
                }
            }

            TopDocs topDocs = searcher.search(booleanQuery.build(), limit);

            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                org.apache.lucene.document.Document doc = searcher.doc(scoreDoc.doc);
                results.add(Long.parseLong(doc.get("id")));
            }

            // 标签过滤（在数据库层面处理，因为Lucene索引不包含标签信息）
            if (tagIds != null && !tagIds.isEmpty()) {
                results = filterByTags(results, tagIds, userId);
            }

            // 更新搜索历史的结果数量
            updateSearchHistoryResultCount(userId, keyword, results.size());
        }

        return results;
    }

    /**
     * 分类内搜索
     */
    public List<Long> searchByCategory(String keyword, Long categoryId, Long userId, int limit) throws IOException {
        saveSearchHistory(userId, keyword, "CATEGORY", 0);

        Directory directory = FSDirectory.open(Paths.get(indexDir));
        List<Long> results = new ArrayList<>();

        try (IndexReader reader = DirectoryReader.open(directory)) {
            IndexSearcher searcher = new IndexSearcher(reader);

            BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();

            // 关键词搜索
            if (keyword != null && !keyword.trim().isEmpty()) {
                Query titleQuery = new FuzzyQuery(new Term("title", keyword));
                Query contentQuery = new FuzzyQuery(new Term("content", keyword));

                BooleanQuery.Builder keywordQuery = new BooleanQuery.Builder();
                keywordQuery.add(titleQuery, BooleanClause.Occur.SHOULD);
                keywordQuery.add(contentQuery, BooleanClause.Occur.SHOULD);

                booleanQuery.add(keywordQuery.build(), BooleanClause.Occur.MUST);
            }

            // 用户过滤
            Query userQuery = LongPoint.newExactQuery("userId", userId);
            booleanQuery.add(userQuery, BooleanClause.Occur.MUST);

            // 分类过滤
            Query categoryQuery = LongPoint.newExactQuery("categoryId", categoryId);
            booleanQuery.add(categoryQuery, BooleanClause.Occur.MUST);

            TopDocs topDocs = searcher.search(booleanQuery.build(), limit);

            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                org.apache.lucene.document.Document doc = searcher.doc(scoreDoc.doc);
                results.add(Long.parseLong(doc.get("id")));
            }

            updateSearchHistoryResultCount(userId, keyword, results.size());
        }

        return results;
    }

    /**
     * 标签搜索
     */
    public List<Long> searchByTag(String keyword, Long tagId, Long userId, int limit) throws IOException {
        saveSearchHistory(userId, keyword, "TAG", 0);

        // 先通过标签获取文档ID
        List<Long> taggedDocumentIds = tagService.getDocumentIdsByTag(tagId, userId);

        if (taggedDocumentIds.isEmpty()) {
            return new ArrayList<>();
        }

        Directory directory = FSDirectory.open(Paths.get(indexDir));
        List<Long> results = new ArrayList<>();

        try (IndexReader reader = DirectoryReader.open(directory)) {
            IndexSearcher searcher = new IndexSearcher(reader);

            BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();

            // 关键词搜索
            if (keyword != null && !keyword.trim().isEmpty()) {
                Query titleQuery = new FuzzyQuery(new Term("title", keyword));
                Query contentQuery = new FuzzyQuery(new Term("content", keyword));

                BooleanQuery.Builder keywordQuery = new BooleanQuery.Builder();
                keywordQuery.add(titleQuery, BooleanClause.Occur.SHOULD);
                keywordQuery.add(contentQuery, BooleanClause.Occur.SHOULD);

                booleanQuery.add(keywordQuery.build(), BooleanClause.Occur.MUST);
            }

            // 用户过滤
            Query userQuery = LongPoint.newExactQuery("userId", userId);
            booleanQuery.add(userQuery, BooleanClause.Occur.MUST);

            // 文档ID过滤（只搜索有该标签的文档）
            BooleanQuery.Builder docIdQuery = new BooleanQuery.Builder();
            for (Long docId : taggedDocumentIds) {
                docIdQuery.add(new TermQuery(new Term("id", docId.toString())), BooleanClause.Occur.SHOULD);
            }
            booleanQuery.add(docIdQuery.build(), BooleanClause.Occur.MUST);

            TopDocs topDocs = searcher.search(booleanQuery.build(), limit);

            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                org.apache.lucene.document.Document doc = searcher.doc(scoreDoc.doc);
                results.add(Long.parseLong(doc.get("id")));
            }

            updateSearchHistoryResultCount(userId, keyword, results.size());
        }

        return results;
    }

    /**
     * 获取搜索建议
     */
    public List<String> getSearchSuggestions(String keyword, Long userId) {
        List<String> suggestions = new ArrayList<>();

        if (keyword == null || keyword.trim().isEmpty()) {
            return suggestions;
        }

        try {
            Directory directory = FSDirectory.open(Paths.get(indexDir));

            try (IndexReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);

                // 搜索标题中包含关键词的文档
                Query titleQuery = new PrefixQuery(new Term("title", keyword.toLowerCase()));
                Query userQuery = LongPoint.newExactQuery("userId", userId);

                BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
                booleanQuery.add(titleQuery, BooleanClause.Occur.MUST);
                booleanQuery.add(userQuery, BooleanClause.Occur.MUST);

                TopDocs topDocs = searcher.search(booleanQuery.build(), 10);

                Set<String> titleSuggestions = new HashSet<>();
                for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                    org.apache.lucene.document.Document doc = searcher.doc(scoreDoc.doc);
                    String title = doc.get("title");
                    if (title != null && title.toLowerCase().contains(keyword.toLowerCase())) {
                        // 提取包含关键词的部分作为建议
                        String suggestion = extractSuggestion(title, keyword);
                        if (suggestion != null) {
                            titleSuggestions.add(suggestion);
                        }
                    }
                }

                suggestions.addAll(titleSuggestions);
            }
        } catch (IOException e) {
            // 如果索引不可用，返回空建议
            System.err.println("获取搜索建议时出错: " + e.getMessage());
        }

        // 添加一些默认建议
        if (suggestions.size() < 5) {
            suggestions.add(keyword + " 教程");
            suggestions.add(keyword + " 笔记");
            suggestions.add(keyword + " 文档");
        }

        return suggestions.stream().limit(10).collect(Collectors.toList());
    }

    /**
     * 获取搜索历史
     */
    public List<Map<String, Object>> getSearchHistory(Long userId, int limit) {
        List<SearchHistory> histories = searchHistoryMapper.findByUserId(userId, limit);

        return histories.stream().map(history -> {
            Map<String, Object> historyMap = new HashMap<>();
            historyMap.put("id", history.getId());
            historyMap.put("keyword", history.getKeyword());
            historyMap.put("resultCount", history.getResultCount());
            historyMap.put("searchTime", history.getSearchTime());
            historyMap.put("searchType", history.getSearchType());
            return historyMap;
        }).collect(Collectors.toList());
    }

    /**
     * 清除搜索历史
     */
    public boolean clearSearchHistory(Long userId) {
        try {
            int deleted = searchHistoryMapper.deleteByUserId(userId);
            return deleted >= 0; // 即使没有记录也返回true
        } catch (Exception e) {
            System.err.println("清除搜索历史失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取搜索统计
     */
    public Map<String, Object> getSearchStatistics(Long userId) {
        Map<String, Object> stats = new HashMap<>();

        try {
            // 基础搜索统计
            Map<String, Object> searchStats = searchHistoryMapper.getSearchStats(userId);
            if (searchStats != null) {
                stats.putAll(searchStats);
            }

            // 热门搜索词
            List<Map<String, Object>> popularKeywords = searchHistoryMapper.getPopularKeywords(userId);
            stats.put("popularKeywords", popularKeywords);

            // 搜索类型分布
            stats.put("searchTypeDistribution", getSearchTypeDistribution(userId));

        } catch (Exception e) {
            System.err.println("获取搜索统计失败: " + e.getMessage());
            // 返回默认统计信息
            stats.put("total_searches", 0);
            stats.put("unique_keywords", 0);
            stats.put("avg_results", 0);
            stats.put("popularKeywords", new ArrayList<>());
        }

        return stats;
    }

    /**
     * 删除文档索引
     */
    public void deleteDocument(Long docId) throws IOException {
        Directory directory = FSDirectory.open(Paths.get(indexDir));
        IndexWriterConfig config = new IndexWriterConfig(analyzer);

        try (IndexWriter writer = new IndexWriter(directory, config)) {
            writer.deleteDocuments(new Term("id", docId.toString()));
            writer.commit();
        }
    }

    /**
     * 重建所有文档索引
     */
    public void rebuildIndex(Long userId) throws IOException {
        // 先清空索引
        Directory directory = FSDirectory.open(Paths.get(indexDir));
        IndexWriterConfig config = new IndexWriterConfig(analyzer);

        try (IndexWriter writer = new IndexWriter(directory, config)) {
            writer.deleteAll();
            writer.commit();
        }

        // 重新索引所有文档
        List<com.dzy666.demo.entity.Document> documents = documentService.getUserDocuments(userId);
        for (com.dzy666.demo.entity.Document doc : documents) {
            indexDocument(doc);
        }
    }

    // ============ 私有工具方法 ============

    private void saveSearchHistory(Long userId, String keyword, String searchType, int resultCount) {
        try {
            SearchHistory history = new SearchHistory();
            history.setUserId(userId);
            history.setKeyword(keyword);
            history.setSearchType(searchType);
            history.setResultCount(resultCount);
            history.setSearchTime(LocalDateTime.now());

            searchHistoryMapper.insert(history);
        } catch (Exception e) {
            System.err.println("保存搜索历史失败: " + e.getMessage());
        }
    }

    private void updateSearchHistoryResultCount(Long userId, String keyword, int resultCount) {
        // 这里简化处理，实际应该更新最近的一条记录
        // 由于搜索历史主要是记录，不严格要求实时更新结果数量
    }

    private long[] parseDateRange(String dateRange) {
        // 简化实现，支持常见日期范围格式
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startDate;

            switch (dateRange.toLowerCase()) {
                case "today":
                    startDate = now.toLocalDate().atStartOfDay();
                    break;
                case "week":
                    startDate = now.minusWeeks(1);
                    break;
                case "month":
                    startDate = now.minusMonths(1);
                    break;
                case "year":
                    startDate = now.minusYears(1);
                    break;
                default:
                    // 尝试解析自定义日期范围 "2024-01-01_to_2024-12-31"
                    if (dateRange.contains("_to_")) {
                        String[] dates = dateRange.split("_to_");
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                        startDate = LocalDateTime.parse(dates[0] + "T00:00:00");
                        LocalDateTime endDate = LocalDateTime.parse(dates[1] + "T23:59:59");
                        return new long[]{
                                startDate.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                                endDate.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                        };
                    }
                    return null;
            }

            return new long[]{
                    startDate.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    now.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            };
        } catch (Exception e) {
            System.err.println("解析日期范围失败: " + e.getMessage());
            return null;
        }
    }

    private List<Long> filterByTags(List<Long> documentIds, List<Long> tagIds, Long userId) {
        // 过滤出包含所有指定标签的文档
        return documentIds.stream()
                .filter(docId -> hasAllTags(docId, tagIds, userId))
                .collect(Collectors.toList());
    }

    private boolean hasAllTags(Long documentId, List<Long> tagIds, Long userId) {
        try {
            List<Long> documentTags = tagService.getDocumentTags(documentId, userId)
                    .stream()
                    .map(tag -> tag.getId())
                    .collect(Collectors.toList());
            return documentTags.containsAll(tagIds);
        } catch (Exception e) {
            return false;
        }
    }

    private String extractSuggestion(String title, String keyword) {
        int index = title.toLowerCase().indexOf(keyword.toLowerCase());
        if (index >= 0) {
            // 提取关键词及其周围文本作为建议
            int start = Math.max(0, index - 10);
            int end = Math.min(title.length(), index + keyword.length() + 10);
            String suggestion = title.substring(start, end).trim();
            return suggestion.length() > keyword.length() ? suggestion : title;
        }
        return title;
    }

    private Map<String, Long> getSearchTypeDistribution(Long userId) {
        // 简化实现，实际应该从数据库查询
        Map<String, Long> distribution = new HashMap<>();
        distribution.put("BASIC", 0L);
        distribution.put("ADVANCED", 0L);
        distribution.put("CATEGORY", 0L);
        distribution.put("TAG", 0L);
        return distribution;
    }
}