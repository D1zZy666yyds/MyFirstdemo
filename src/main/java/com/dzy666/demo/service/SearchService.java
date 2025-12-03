package com.dzy666.demo.service;

import com.dzy666.demo.entity.SearchHistory;
import com.dzy666.demo.mapper.SearchHistoryMapper;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

    // 🎯 新增：构造函数，确保服务启动时索引目录存在
    public SearchService() {
        System.out.println("=== SearchService 初始化开始 ===");
        System.out.println("索引目录路径: " + new File(indexDir).getAbsolutePath());

        try {
            // 确保索引目录存在
            ensureIndexDirExists();

            // 初始化空索引（如果需要）
            initializeEmptyIndex();

            System.out.println("SearchService 初始化完成");
        } catch (Exception e) {
            System.err.println("SearchService 初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("=== SearchService 初始化结束 ===");
    }

    // 🎯 新增：确保索引目录存在
    private void ensureIndexDirExists() {
        try {
            File dir = new File(indexDir);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (created) {
                    System.out.println("✅ 成功创建Lucene索引目录: " + dir.getAbsolutePath());
                } else {
                    System.err.println("❌ 创建索引目录失败，可能权限不足");
                }
            } else {
                System.out.println("✅ Lucene索引目录已存在: " + dir.getAbsolutePath());
            }

            // 检查目录权限
            if (!dir.canWrite()) {
                System.err.println("❌ 警告：索引目录不可写: " + dir.getAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("确保索引目录存在时出错: " + e.getMessage());
        }
    }

    // 🎯 新增：初始化空索引
    private void initializeEmptyIndex() {
        try {
            Directory directory = FSDirectory.open(Paths.get(indexDir));

            // 检查索引是否存在
            if (!DirectoryReader.indexExists(directory)) {
                System.out.println("索引不存在，创建空索引...");
                IndexWriterConfig config = new IndexWriterConfig(analyzer);
                config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

                try (IndexWriter writer = new IndexWriter(directory, config)) {
                    writer.commit();
                    System.out.println("✅ 空索引创建成功");
                }
            } else {
                try (IndexReader reader = DirectoryReader.open(directory)) {
                    System.out.println("✅ 索引已存在，文档数量: " + reader.numDocs());
                }
            }
        } catch (Exception e) {
            System.err.println("初始化空索引失败: " + e.getMessage());
        }
    }

    /**
     * 为文档创建索引
     */
    public void indexDocument(com.dzy666.demo.entity.Document doc) throws IOException {
        System.out.println("为文档创建索引: " + doc.getId() + " - " + doc.getTitle());

        try {
            Directory directory = FSDirectory.open(Paths.get(indexDir));
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

            try (IndexWriter writer = new IndexWriter(directory, config)) {
                // 删除旧索引（如果存在）
                writer.deleteDocuments(new Term("id", doc.getId().toString()));

                // 创建新文档
                Document luceneDoc = new Document();
                luceneDoc.add(new StringField("id", doc.getId().toString(), Field.Store.YES));

                // 标题字段
                if (doc.getTitle() != null && !doc.getTitle().trim().isEmpty()) {
                    luceneDoc.add(new TextField("title", doc.getTitle(), Field.Store.YES));
                } else {
                    luceneDoc.add(new TextField("title", "无标题", Field.Store.YES));
                }

                // 内容字段
                if (doc.getContent() != null && !doc.getContent().trim().isEmpty()) {
                    luceneDoc.add(new TextField("content", doc.getContent(), Field.Store.YES));
                } else {
                    luceneDoc.add(new TextField("content", "无内容", Field.Store.YES));
                }

                // 用户ID
                if (doc.getUserId() != null) {
                    luceneDoc.add(new LongPoint("userId", doc.getUserId()));
                    luceneDoc.add(new StoredField("userId", doc.getUserId()));
                } else {
                    System.err.println("⚠️ 警告：文档缺少userId: " + doc.getId());
                }

                // 分类信息
                if (doc.getCategoryId() != null) {
                    luceneDoc.add(new LongPoint("categoryId", doc.getCategoryId()));
                    luceneDoc.add(new StoredField("categoryId", doc.getCategoryId()));
                }

                // 添加创建时间
                if (doc.getCreatedTime() != null) {
                    long createdTimeMillis = doc.getCreatedTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                    luceneDoc.add(new LongPoint("createdTime", createdTimeMillis));
                    luceneDoc.add(new StoredField("createdTime", createdTimeMillis));
                }

                // 添加更新时间
                if (doc.getUpdatedTime() != null) {
                    long updatedTimeMillis = doc.getUpdatedTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                    luceneDoc.add(new LongPoint("updatedTime", updatedTimeMillis));
                    luceneDoc.add(new StoredField("updatedTime", updatedTimeMillis));
                }

                writer.addDocument(luceneDoc);
                writer.commit();

                System.out.println("✅ 文档索引创建成功: " + doc.getId());
            }
        } catch (Exception e) {
            System.err.println("❌ 创建文档索引失败: " + doc.getId() + " - " + e.getMessage());
            throw e;
        }
    }

    /**
     * 基础搜索方法 - 完整修复版
     */
    public List<Long> search(String keyword, Long userId, int limit) throws IOException {
        System.out.println("=== 开始基础搜索 ===");
        System.out.println("参数 - 关键词: '" + keyword + "', 用户ID: " + userId + ", 限制: " + limit);

        // 记录搜索历史
        saveSearchHistory(userId, keyword, "BASIC", 0);

        try {
            // 确保索引目录存在
            ensureIndexDirExists();

            Directory directory = FSDirectory.open(Paths.get(indexDir));
            List<Long> results = new ArrayList<>();

            // 检查索引是否存在
            if (!DirectoryReader.indexExists(directory)) {
                System.err.println("❌ 索引不存在，创建空索引");
                initializeEmptyIndex();
                return results; // 返回空结果
            }

            try (IndexReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);

                // 🎯 修复：构建更灵活的多字段查询
                BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();

                if (keyword != null && !keyword.trim().isEmpty()) {
                    String trimmedKeyword = keyword.trim().toLowerCase();

                    // 🎯 修复：使用更智能的查询构建
                    QueryParser titleParser = new QueryParser("title", analyzer);
                    QueryParser contentParser = new QueryParser("content", analyzer);

                    Query titleQuery = titleParser.parse(trimmedKeyword);
                    Query contentQuery = contentParser.parse(trimmedKeyword);

                    // 标题查询权重更高
                    titleQuery = new BoostQuery(titleQuery, 2.0f);

                    BooleanQuery.Builder keywordQuery = new BooleanQuery.Builder();
                    keywordQuery.add(titleQuery, BooleanClause.Occur.SHOULD);
                    keywordQuery.add(contentQuery, BooleanClause.Occur.SHOULD);

                    booleanQuery.add(keywordQuery.build(), BooleanClause.Occur.MUST);
                }

                // 用户过滤 - 必须属于该用户
                Query userQuery = LongPoint.newExactQuery("userId", userId);
                booleanQuery.add(userQuery, BooleanClause.Occur.MUST);

                // 按相关性或更新时间排序
                TopDocs topDocs = searcher.search(booleanQuery.build(), Math.min(limit, 1000));

                System.out.println("🔍 基础搜索找到 " + topDocs.totalHits.value + " 个匹配");

                for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                    org.apache.lucene.document.Document doc = searcher.doc(scoreDoc.doc);
                    try {
                        Long docId = Long.parseLong(doc.get("id"));
                        System.out.println("匹配文档ID: " + docId + ", 标题: " + doc.get("title"));
                        results.add(docId);
                    } catch (NumberFormatException e) {
                        System.err.println("❌ 解析文档ID失败: " + doc.get("id"));
                    }
                }

                // 更新搜索历史结果数量
                updateSearchHistoryResultCount(userId, keyword, results.size());
            }

            return results;

        } catch (Exception e) {
            System.err.println("❌ 搜索过程中发生异常: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            System.out.println("=== 基础搜索结束 ===");
        }
    }

    /**
     * 高级搜索 - 完整修复版
     */
    public List<Long> advancedSearch(String keyword, Long categoryId, List<Long> tagIds,
                                     String dateRange, Long userId, int limit) throws IOException {
        System.out.println("=== 高级搜索开始 ===");
        System.out.println("参数: 关键词='" + keyword + "', 分类ID=" + categoryId + ", 标签=" + tagIds + ", 日期范围=" + dateRange);

        // 记录搜索历史
        saveSearchHistory(userId, keyword, "ADVANCED", 0);

        try {
            // 确保索引目录存在
            ensureIndexDirExists();

            Directory directory = FSDirectory.open(Paths.get(indexDir));
            List<Long> results = new ArrayList<>();

            try (IndexReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();

                // 1. 关键词搜索（支持标题或内容匹配）
                if (keyword != null && !keyword.trim().isEmpty()) {
                    String trimmedKeyword = keyword.trim();
                    Query titleWildcard = new WildcardQuery(new Term("title", "*" + trimmedKeyword + "*"));
                    Query contentWildcard = new WildcardQuery(new Term("content", "*" + trimmedKeyword + "*"));

                    BooleanQuery.Builder keywordQuery = new BooleanQuery.Builder();
                    keywordQuery.add(titleWildcard, BooleanClause.Occur.SHOULD);
                    keywordQuery.add(contentWildcard, BooleanClause.Occur.SHOULD);

                    // 关键词搜索是必须的条件
                    booleanQuery.add(keywordQuery.build(), BooleanClause.Occur.MUST);
                    System.out.println("关键词搜索条件已添加: " + trimmedKeyword);
                }

                // 2. 用户过滤 - 必须属于该用户
                Query userQuery = LongPoint.newExactQuery("userId", userId);
                booleanQuery.add(userQuery, BooleanClause.Occur.MUST);
                System.out.println("用户过滤已添加: " + userId);

                // 3. 分类过滤
                if (categoryId != null) {
                    Query categoryQuery = LongPoint.newExactQuery("categoryId", categoryId);
                    booleanQuery.add(categoryQuery, BooleanClause.Occur.MUST);
                    System.out.println("分类过滤已添加: " + categoryId);
                } else {
                    System.out.println("分类过滤: 未指定");
                }

                // 4. 日期范围过滤
                if (dateRange != null && !dateRange.trim().isEmpty()) {
                    long[] dateRangeMillis = parseDateRange(dateRange);
                    if (dateRangeMillis != null) {
                        Query dateQuery = LongPoint.newRangeQuery("createdTime", dateRangeMillis[0], dateRangeMillis[1]);
                        booleanQuery.add(dateQuery, BooleanClause.Occur.MUST);
                        System.out.println("日期范围过滤已添加: " + dateRange);
                    } else {
                        System.out.println("日期范围过滤: 无效格式 " + dateRange);
                    }
                } else {
                    System.out.println("日期范围过滤: 未指定");
                }

                // 5. 构建查询
                Query query = booleanQuery.build();
                System.out.println("构建查询完成，开始搜索...");

                // 6. 按更新时间倒序排序
                Sort sort = new Sort(new SortField("updatedTime", SortField.Type.LONG, true));
                TopDocs topDocs = searcher.search(query, Math.min(limit, 1000), sort);

                System.out.println("Lucene高级搜索找到 " + topDocs.totalHits.value + " 个文档");

                // 7. 提取结果
                for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                    org.apache.lucene.document.Document doc = searcher.doc(scoreDoc.doc);
                    try {
                        Long docId = Long.parseLong(doc.get("id"));
                        results.add(docId);

                        if (results.size() <= 5) {
                            System.out.println("匹配文档ID: " + docId + ", 标题: " + doc.get("title"));
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("解析文档ID失败: " + doc.get("id"));
                    }
                }

                // 8. 标签过滤（如果有的话）
                if (tagIds != null && !tagIds.isEmpty() && !results.isEmpty()) {
                    System.out.println("进行标签过滤，标签ID: " + tagIds + "，原始结果数: " + results.size());
                    results = filterByTags(results, tagIds, userId);
                } else {
                    System.out.println("标签过滤: 未指定或结果为空");
                }

                // 9. 限制最终结果数量
                if (results.size() > limit) {
                    results = results.subList(0, limit);
                }

                // 10. 更新搜索历史结果数量
                updateSearchHistoryResultCount(userId, keyword, results.size());

            } catch (Exception e) {
                System.err.println("高级搜索发生异常: " + e.getMessage());
                e.printStackTrace();
                // 返回空结果而不是抛出异常
                return new ArrayList<>();
            }

            System.out.println("高级搜索完成，找到 " + results.size() + " 个文档");
            return results;

        } catch (Exception e) {
            System.err.println("高级搜索失败: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            System.out.println("=== 高级搜索结束 ===");
        }
    }

    // 修改 filterByTags 方法
    private List<Long> filterByTags(List<Long> documentIds, List<Long> tagIds, Long userId) {
        if (tagIds == null || tagIds.isEmpty() || documentIds.isEmpty()) {
            return documentIds;
        }

        System.out.println("标签过滤: " + tagIds.size() + " 个标签，文档数: " + documentIds.size());

        // 批量获取所有文档的标签
        Map<Long, List<Long>> documentTagMap = new HashMap<>();

        try {
            // 批量查询所有文档的标签
            for (Long docId : documentIds) {
                List<Long> documentTags = tagService.getDocumentTags(docId, userId)
                        .stream()
                        .map(tag -> tag.getId())
                        .collect(Collectors.toList());
                documentTagMap.put(docId, documentTags);
            }

            // 并行过滤（如果文档很多）
            List<Long> filteredResults = documentIds.parallelStream()
                    .filter(docId -> {
                        List<Long> docTags = documentTagMap.get(docId);
                        if (docTags == null || docTags.isEmpty()) {
                            return false;
                        }
                        // 检查文档是否包含任意一个指定的标签
                        return docTags.stream().anyMatch(tagIds::contains);
                    })
                    .collect(Collectors.toList());

            System.out.println("标签过滤后剩余: " + filteredResults.size() + " 个文档");
            return filteredResults;

        } catch (Exception e) {
            System.err.println("标签过滤失败: " + e.getMessage());
            // 如果过滤失败，返回原始结果
            return documentIds;
        }
    }

    /**
     * 增强的日期范围解析方法（支持更多格式）
     */
    private long[] parseDateRange(String dateRange) {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startDate;

            switch (dateRange.toLowerCase()) {
                case "today":
                    startDate = now.toLocalDate().atStartOfDay();
                    break;
                case "yesterday":
                    startDate = now.minusDays(1).toLocalDate().atStartOfDay();
                    LocalDateTime endDate = now.toLocalDate().atStartOfDay();
                    return new long[]{
                            startDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                            endDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    };
                case "week":
                    startDate = now.minusWeeks(1);
                    break;
                case "month":
                    startDate = now.minusMonths(1);
                    break;
                case "year":
                    startDate = now.minusYears(1);
                    break;
                case "last7days":
                    startDate = now.minusDays(7);
                    break;
                case "last30days":
                    startDate = now.minusDays(30);
                    break;
                case "all": // 所有时间
                    return null;
                default:
                    // 尝试解析自定义日期范围格式: "YYYY-MM-DD~YYYY-MM-DD"
                    if (dateRange.contains("~")) {
                        String[] parts = dateRange.split("~");
                        if (parts.length == 2) {
                            LocalDateTime start = LocalDate.parse(parts[0].trim()).atStartOfDay();
                            LocalDateTime end = LocalDate.parse(parts[1].trim()).atTime(23, 59, 59);
                            return new long[]{
                                    start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                                    end.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            };
                        }
                    }
                    System.err.println("未知的日期范围格式: " + dateRange);
                    return null;
            }

            return new long[]{
                    startDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            };
        } catch (Exception e) {
            System.err.println("解析日期范围失败: " + dateRange + ", error: " + e.getMessage());
            return null;
        }
    }

    /**
     * 分类内搜索
     */
    public List<Long> searchByCategory(String keyword, Long categoryId, Long userId, int limit) throws IOException {
        saveSearchHistory(userId, keyword, "CATEGORY", 0);

        try {
            Directory directory = FSDirectory.open(Paths.get(indexDir));
            List<Long> results = new ArrayList<>();

            try (IndexReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();

                // 关键词搜索
                if (keyword != null && !keyword.trim().isEmpty()) {
                    // 使用通配符搜索
                    Query titleWildcard = new WildcardQuery(new Term("title", "*" + keyword.trim() + "*"));
                    Query contentWildcard = new WildcardQuery(new Term("content", "*" + keyword.trim() + "*"));

                    booleanQuery.add(titleWildcard, BooleanClause.Occur.SHOULD);
                    booleanQuery.add(contentWildcard, BooleanClause.Occur.SHOULD);
                }

                // 用户过滤
                Query userQuery = LongPoint.newExactQuery("userId", userId);
                booleanQuery.add(userQuery, BooleanClause.Occur.MUST);

                // 分类过滤
                Query categoryQuery = LongPoint.newExactQuery("categoryId", categoryId);
                booleanQuery.add(categoryQuery, BooleanClause.Occur.MUST);

                // 按更新时间倒序
                Sort sort = new Sort(new SortField("updatedTime", SortField.Type.LONG, true));
                TopDocs topDocs = searcher.search(booleanQuery.build(), limit, sort);

                for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                    org.apache.lucene.document.Document doc = searcher.doc(scoreDoc.doc);
                    try {
                        results.add(Long.parseLong(doc.get("id")));
                    } catch (NumberFormatException e) {
                        System.err.println("解析文档ID失败: " + doc.get("id"));
                    }
                }

                updateSearchHistoryResultCount(userId, keyword, results.size());
            }

            return results;
        } catch (Exception e) {
            System.err.println("分类搜索失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 标签搜索
     */
    public List<Long> searchByTag(String keyword, Long tagId, Long userId, int limit) throws IOException {
        saveSearchHistory(userId, keyword, "TAG", 0);
        System.out.println("=== 开始标签搜索 ===");
        System.out.println("参数: 关键词='" + keyword + "', 标签ID=" + tagId);

        try {
            // 先通过标签获取文档ID
            List<Long> taggedDocumentIds = tagService.getDocumentIdsByTag(tagId, userId);
            System.out.println("标签关联的文档数量: " + taggedDocumentIds.size());

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
                    // 使用通配符搜索
                    Query titleWildcard = new WildcardQuery(new Term("title", "*" + keyword.trim() + "*"));
                    Query contentWildcard = new WildcardQuery(new Term("content", "*" + keyword.trim() + "*"));

                    booleanQuery.add(titleWildcard, BooleanClause.Occur.SHOULD);
                    booleanQuery.add(contentWildcard, BooleanClause.Occur.SHOULD);
                }

                // 用户过滤
                Query userQuery = LongPoint.newExactQuery("userId", userId);
                booleanQuery.add(userQuery, BooleanClause.Occur.MUST);

                // 文档ID过滤 - 只搜索有该标签的文档
                BooleanQuery.Builder docIdQuery = new BooleanQuery.Builder();
                for (Long docId : taggedDocumentIds) {
                    docIdQuery.add(new TermQuery(new Term("id", docId.toString())), BooleanClause.Occur.SHOULD);
                }
                booleanQuery.add(docIdQuery.build(), BooleanClause.Occur.MUST);

                // 按更新时间倒序
                Sort sort = new Sort(new SortField("updatedTime", SortField.Type.LONG, true));
                TopDocs topDocs = searcher.search(booleanQuery.build(), limit, sort);

                System.out.println("Lucene搜索找到 " + topDocs.totalHits.value + " 个匹配");

                for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                    org.apache.lucene.document.Document doc = searcher.doc(scoreDoc.doc);
                    try {
                        Long docId = Long.parseLong(doc.get("id"));
                        results.add(docId);
                        System.out.println("匹配文档ID: " + docId);
                    } catch (NumberFormatException e) {
                        System.err.println("解析文档ID失败: " + doc.get("id"));
                    }
                }

                updateSearchHistoryResultCount(userId, keyword, results.size());
            }

            System.out.println("标签搜索完成，找到 " + results.size() + " 个文档");
            return results;
        } catch (Exception e) {
            System.err.println("标签搜索失败: " + e.getMessage());
            return new ArrayList<>();
        } finally {
            System.out.println("=== 标签搜索结束 ===");
        }
    }

    /**
     * 获取搜索历史
     */
    public List<Map<String, Object>> getSearchHistory(Long userId, int limit) {
        try {
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
        } catch (Exception e) {
            System.err.println("获取搜索历史失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 清除搜索历史
     */
    public boolean clearSearchHistory(Long userId) {
        try {
            int deleted = searchHistoryMapper.deleteByUserId(userId);
            System.out.println("清除搜索历史，用户ID: " + userId + ", 删除记录数: " + deleted);
            return true;
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
            // 获取搜索统计
            Map<String, Object> searchStats = searchHistoryMapper.getSearchStats(userId);
            if (searchStats != null) {
                stats.putAll(searchStats);
            }

            // 热门搜索词
            List<Map<String, Object>> popularKeywords = searchHistoryMapper.getPopularKeywords(userId);
            stats.put("popularKeywords", popularKeywords);

        } catch (Exception e) {
            System.err.println("获取搜索统计失败: " + e.getMessage());
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
        System.out.println("删除文档索引: " + docId);

        try {
            Directory directory = FSDirectory.open(Paths.get(indexDir));
            IndexWriterConfig config = new IndexWriterConfig(analyzer);

            try (IndexWriter writer = new IndexWriter(directory, config)) {
                writer.deleteDocuments(new Term("id", docId.toString()));
                writer.commit();
                System.out.println("✅ 文档索引删除成功: " + docId);
            }
        } catch (Exception e) {
            System.err.println("❌ 删除文档索引失败: " + docId + " - " + e.getMessage());
            throw e;
        }
    }

    /**
     * 重建所有文档索引
     */
    public void rebuildIndex(Long userId) throws IOException {
        System.out.println("=== 开始重建索引，用户ID: " + userId + " ===");

        try {
            // 先清空索引
            Directory directory = FSDirectory.open(Paths.get(indexDir));
            IndexWriterConfig config = new IndexWriterConfig(analyzer);

            try (IndexWriter writer = new IndexWriter(directory, config)) {
                writer.deleteAll();
                writer.commit();
                System.out.println("✅ 索引已清空");
            }

            // 重新索引所有文档
            List<com.dzy666.demo.entity.Document> documents = documentService.getUserDocuments(userId);
            System.out.println("需要索引的文档数量: " + documents.size());

            int successCount = 0;
            int failCount = 0;

            for (com.dzy666.demo.entity.Document doc : documents) {
                try {
                    indexDocument(doc);
                    successCount++;
                    System.out.println("✅ 已索引文档: " + doc.getId() + " - " + doc.getTitle());
                } catch (Exception e) {
                    failCount++;
                    System.err.println("❌ 索引文档失败: " + doc.getId() + " - " + e.getMessage());
                }
            }

            System.out.println("✅ 索引重建完成 - 成功: " + successCount + ", 失败: " + failCount);
        } catch (Exception e) {
            System.err.println("❌ 索引重建失败: " + e.getMessage());
            throw e;
        } finally {
            System.out.println("=== 索引重建结束 ===");
        }
    }

    // 修改 saveSearchHistory 方法
    private void saveSearchHistory(Long userId, String keyword, String searchType, int resultCount) {
        try {
            if (userId == null || keyword == null) {
                System.err.println("保存搜索历史失败：参数为空");
                return;
            }

            // 检查今天是否已经搜索过相同关键词
            int exists = searchHistoryMapper.existsToday(userId, keyword);

            if (exists > 0) {
                // 如果今天已经搜索过，更新结果数量
                searchHistoryMapper.updateResultCount(userId, keyword, resultCount);
                System.out.println("更新今日搜索历史: " + keyword + ", 用户: " + userId);
            } else {
                // 新增搜索历史记录
                SearchHistory history = new SearchHistory();
                history.setUserId(userId);
                history.setKeyword(keyword);
                history.setSearchType(searchType);
                history.setResultCount(resultCount);
                history.setSearchTime(LocalDateTime.now());

                searchHistoryMapper.insert(history);
                System.out.println("保存新搜索历史: " + keyword + ", 用户: " + userId);
            }
        } catch (Exception e) {
            System.err.println("保存搜索历史失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 修改 updateSearchHistoryResultCount 方法
    private void updateSearchHistoryResultCount(Long userId, String keyword, int resultCount) {
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                System.out.println("关键词为空，跳过更新");
                return;
            }

            // 🎯 使用新的更新方法
            int updated = searchHistoryMapper.updateResultCount(userId, keyword, resultCount);

            if (updated > 0) {
                System.out.println("成功更新搜索结果数量: 关键词=" + keyword +
                        ", 结果数=" + resultCount + ", 影响行数=" + updated);
            } else {
                System.out.println("未找到匹配的搜索历史记录，创建新的...");

                // 如果更新失败（可能记录不存在），创建新的
                SearchHistory history = new SearchHistory();
                history.setUserId(userId);
                history.setKeyword(keyword);
                history.setSearchType("SEARCH_RESULT");
                history.setResultCount(resultCount);
                history.setSearchTime(LocalDateTime.now());

                searchHistoryMapper.insert(history);
                System.out.println("创建新的搜索历史记录: " + keyword + ", 结果数=" + resultCount);
            }

        } catch (Exception e) {
            System.err.println("更新搜索结果数量失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 修改 getSearchSuggestions 方法以使用新的 Mapper 方法
    public List<String> getSearchSuggestions(String keyword, Long userId) {
        List<String> suggestions = new ArrayList<>();

        if (keyword == null || keyword.trim().isEmpty()) {
            return suggestions;
        }

        try {
            // 使用新的 Mapper 方法获取相关关键词
            List<String> historySuggestions = searchHistoryMapper.findKeywordsByPrefix(userId, keyword, 5);
            suggestions.addAll(historySuggestions);

            System.out.println("从历史记录获取建议: " + historySuggestions.size() + " 个");

            // 如果历史建议不足，添加一些通用建议
            if (suggestions.size() < 5) {
                String[] commonSuggestions = {
                        keyword + " 笔记",
                        keyword + " 文档",
                        "关于" + keyword,
                        keyword + " 总结",
                        keyword + " 知识",
                        keyword + " 学习",
                        keyword + " 教程",
                        keyword + " 方法"
                };

                for (String suggestion : commonSuggestions) {
                    if (!suggestions.contains(suggestion) && suggestions.size() < 10) {
                        suggestions.add(suggestion);
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("获取搜索建议时出错: " + e.getMessage());
            // 返回一些默认建议
            suggestions.add(keyword + " 相关内容");
            suggestions.add(keyword + " 文档");
        }

        return suggestions.stream()
                .distinct()
                .limit(10)  // 限制返回数量
                .collect(Collectors.toList());
    }
}