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

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private final String indexDir = "lucene-index";
    private SmartChineseAnalyzer analyzer = new SmartChineseAnalyzer();
    private final Lock indexLock = new ReentrantLock();

    @Autowired
    @Lazy
    private DocumentService documentService;

    @Autowired
    private SearchHistoryMapper searchHistoryMapper;

    @Autowired
    private TagService tagService;

    public SearchService() {
        System.out.println("=== SearchService 初始化开始 ===");
        try {
            ensureIndexDirExists();
            initializeEmptyIndex();
            System.out.println("SearchService 初始化完成");
        } catch (Exception e) {
            System.err.println("SearchService 初始化失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("搜索服务初始化失败", e);
        }
        System.out.println("=== SearchService 初始化结束 ===");
    }

    private void ensureIndexDirExists() throws IOException {
        File dir = new File(indexDir);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                throw new IOException("创建索引目录失败: " + dir.getAbsolutePath());
            }
            System.out.println("✅ 创建Lucene索引目录: " + dir.getAbsolutePath());
        }

        if (!dir.canWrite()) {
            throw new IOException("索引目录不可写: " + dir.getAbsolutePath());
        }
    }

    private void initializeEmptyIndex() {
        indexLock.lock();
        try {
            Directory directory = FSDirectory.open(Paths.get(indexDir));

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
            throw new RuntimeException("索引初始化失败", e);
        } finally {
            indexLock.unlock();
        }
    }

    /**
     * 🎯 修复：为文档创建索引（改进标签存储）
     */
    public void indexDocument(com.dzy666.demo.entity.Document doc) throws IOException {
        System.out.println("为文档创建索引: " + doc.getId() + " - " + doc.getTitle());

        indexLock.lock();
        try {
            Directory directory = FSDirectory.open(Paths.get(indexDir));
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

            try (IndexWriter writer = new IndexWriter(directory, config)) {
                writer.deleteDocuments(new Term("id", doc.getId().toString()));

                Document luceneDoc = createLuceneDocument(doc);
                writer.addDocument(luceneDoc);
                writer.commit();

                System.out.println("✅ 文档索引创建成功: " + doc.getId());
            }
        } catch (Exception e) {
            System.err.println("❌ 创建文档索引失败: " + doc.getId() + " - " + e.getMessage());
            throw e;
        } finally {
            indexLock.unlock();
        }
    }

    /**
     * 🎯 修复：创建Lucene文档（改进标签存储格式）
     */
    private Document createLuceneDocument(com.dzy666.demo.entity.Document doc) {
        Document luceneDoc = new Document();
        luceneDoc.add(new StringField("id", doc.getId().toString(), Field.Store.YES));

        // 标题字段
        String title = (doc.getTitle() != null && !doc.getTitle().trim().isEmpty())
                ? doc.getTitle() : "无标题";
        luceneDoc.add(new TextField("title", title, Field.Store.YES));

        // 内容字段
        String content = (doc.getContent() != null && !doc.getContent().trim().isEmpty())
                ? doc.getContent() : "无内容";
        luceneDoc.add(new TextField("content", content, Field.Store.YES));

        // 用户ID
        if (doc.getUserId() == null) {
            throw new IllegalArgumentException("文档缺少userId: " + doc.getId());
        }
        luceneDoc.add(new LongPoint("userId", doc.getUserId()));
        luceneDoc.add(new StoredField("userId", doc.getUserId()));

        // 分类信息
        if (doc.getCategoryId() != null) {
            luceneDoc.add(new LongPoint("categoryId", doc.getCategoryId()));
            luceneDoc.add(new StoredField("categoryId", doc.getCategoryId()));
        }

        // 添加创建时间
        if (doc.getCreatedTime() != null) {
            long createdTimeMillis = doc.getCreatedTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            luceneDoc.add(new LongPoint("createdTime", createdTimeMillis));
            luceneDoc.add(new StoredField("createdTime", createdTimeMillis));
        }

        // 添加更新时间
        if (doc.getUpdatedTime() != null) {
            long updatedTimeMillis = doc.getUpdatedTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            luceneDoc.add(new LongPoint("updatedTime", updatedTimeMillis));
            luceneDoc.add(new StoredField("updatedTime", updatedTimeMillis));
        }

        // 🎯 修复：改进标签信息存储格式
        try {
            List<com.dzy666.demo.entity.Tag> tags = tagService.getDocumentTags(doc.getId(), doc.getUserId());
            if (tags != null && !tags.isEmpty()) {
                // 标签名称（用于全文搜索）
                String tagNames = tags.stream()
                        .map(com.dzy666.demo.entity.Tag::getName)
                        .collect(Collectors.joining(" "));
                luceneDoc.add(new TextField("tagNames", tagNames, Field.Store.YES));

                // 🎯 关键修复：标签ID存储格式 - 用逗号包围每个ID，便于准确匹配
                // 格式：",1,3,5," 这样每个ID都被逗号包围
                String tagIds = tags.stream()
                        .map(tag -> tag.getId().toString())
                        .collect(Collectors.joining(",", ",", ","));
                luceneDoc.add(new StringField("tagIds", tagIds, Field.Store.YES));

                System.out.println("✅ 索引标签信息 - 文档ID: " + doc.getId() +
                        ", 标签: " + tagNames + ", 标签IDs格式: " + tagIds);
            } else {
                // 无标签的文档也要存储空字符串，便于查询
                luceneDoc.add(new TextField("tagNames", "", Field.Store.YES));
                luceneDoc.add(new StringField("tagIds", ",", Field.Store.YES)); // 只有一个逗号
                System.out.println("📭 文档无标签信息 - 文档ID: " + doc.getId());
            }
        } catch (Exception e) {
            System.err.println("❌ 获取标签信息失败，文档ID: " + doc.getId() + " - " + e.getMessage());
            luceneDoc.add(new TextField("tagNames", "", Field.Store.YES));
            luceneDoc.add(new StringField("tagIds", ",", Field.Store.YES));
        }

        return luceneDoc;
    }

    /**
     * 🎯 修复：基础搜索方法 - 支持排序
     */
    public List<Long> search(String keyword, Long userId, int limit, String sortBy) throws IOException {
        System.out.println("=== 开始基础搜索 ===");
        System.out.println("参数 - 关键词: '" + keyword + "', 用户ID: " + userId + ", 限制: " + limit + ", 排序: " + sortBy);

        saveSearchHistory(userId, keyword, "BASIC", 0);

        try {
            Directory directory = FSDirectory.open(Paths.get(indexDir));
            List<Long> results = new ArrayList<>();

            if (!DirectoryReader.indexExists(directory)) {
                System.err.println("❌ 索引不存在，创建空索引");
                initializeEmptyIndex();
                return results;
            }

            try (IndexReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);

                BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();

                if (keyword != null && !keyword.trim().isEmpty()) {
                    String trimmedKeyword = keyword.trim().toLowerCase();
                    Query keywordQuery = buildKeywordQuery(trimmedKeyword);
                    if (keywordQuery != null) {
                        booleanQuery.add(keywordQuery, BooleanClause.Occur.MUST);
                    }
                }

                Query userQuery = LongPoint.newExactQuery("userId", userId);
                booleanQuery.add(userQuery, BooleanClause.Occur.MUST);

                // 🎯 新增：排序逻辑
                Sort sort = getSortByType(sortBy);
                TopDocs topDocs;

                if (sort != null) {
                    topDocs = searcher.search(booleanQuery.build(), Math.min(limit, 1000), sort);
                } else {
                    topDocs = searcher.search(booleanQuery.build(), Math.min(limit, 1000));
                }

                System.out.println("🔍 基础搜索找到 " + topDocs.totalHits.value + " 个匹配");

                for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                    org.apache.lucene.document.Document doc = searcher.doc(scoreDoc.doc);
                    try {
                        Long docId = Long.parseLong(doc.get("id"));
                        System.out.println("匹配文档ID: " + docId + ", 标题: " + doc.get("title") + ", 评分: " + scoreDoc.score);
                        results.add(docId);
                    } catch (NumberFormatException e) {
                        System.err.println("❌ 解析文档ID失败: " + doc.get("id"));
                    }
                }

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
     * 🎯 新增：智能查询构建方法（支持标签字段）
     */
    private Query buildKeywordQuery(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }

        String trimmedKeyword = keyword.trim().toLowerCase();

        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        // 搜索标题、内容和标签
        String[] fields = {"title", "content", "tagNames"};

        for (String field : fields) {
            // 如果包含空格，构建AND查询
            if (trimmedKeyword.contains(" ")) {
                String[] terms = trimmedKeyword.split("\\s+");
                BooleanQuery.Builder fieldQuery = new BooleanQuery.Builder();

                for (String term : terms) {
                    if (!term.isEmpty()) {
                        // 使用通配符查询，支持模糊匹配
                        Query termQuery = new WildcardQuery(new Term(field, "*" + term + "*"));
                        fieldQuery.add(termQuery, BooleanClause.Occur.MUST);
                    }
                }

                builder.add(fieldQuery.build(), BooleanClause.Occur.SHOULD);
            } else {
                // 单一词项，使用通配符查询
                Query termQuery = new WildcardQuery(new Term(field, "*" + trimmedKeyword + "*"));
                builder.add(termQuery, BooleanClause.Occur.SHOULD);
            }
        }

        return builder.build();
    }

    /**
     * 🎯 新增：根据排序类型获取Sort对象
     */
    private Sort getSortByType(String sortBy) {
        if (sortBy == null) {
            return Sort.RELEVANCE;
        }

        switch (sortBy.toLowerCase()) {
            case "relevance":
                return Sort.RELEVANCE;  // 相关性排序
            case "time_desc":
                return new Sort(new SortField("updatedTime", SortField.Type.LONG, true));  // 时间降序
            case "time_asc":
                return new Sort(new SortField("updatedTime", SortField.Type.LONG, false)); // 时间升序
            case "title_asc":
                return new Sort(new SortField("title", SortField.Type.STRING, false));     // 标题升序
            case "title_desc":
                return new Sort(new SortField("title", SortField.Type.STRING, true));      // 标题降序
            default:
                return Sort.RELEVANCE;
        }
    }

    /**
     * 🎯 修复：高级搜索 - 支持多标签和排序
     */
    public List<Long> advancedSearch(String keyword, Long categoryId, List<Long> tagIds,
                                     String dateRange, Long userId, int limit, String sortBy) throws IOException {
        System.out.println("=== 高级搜索开始 ===");
        System.out.println("参数: 关键词='" + keyword + "', 分类ID=" + categoryId +
                ", 标签=" + tagIds + ", 日期范围=" + dateRange + ", 排序=" + sortBy);

        saveSearchHistory(userId, keyword, "ADVANCED", 0);

        try {
            Directory directory = FSDirectory.open(Paths.get(indexDir));
            List<Long> results = new ArrayList<>();

            try (IndexReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();

                // 1. 关键词搜索
                if (keyword != null && !keyword.trim().isEmpty()) {
                    Query keywordQuery = buildKeywordQuery(keyword.trim());
                    if (keywordQuery != null) {
                        booleanQuery.add(keywordQuery, BooleanClause.Occur.MUST);
                    }
                }

                // 2. 用户过滤
                Query userQuery = LongPoint.newExactQuery("userId", userId);
                booleanQuery.add(userQuery, BooleanClause.Occur.MUST);

                // 3. 分类过滤
                if (categoryId != null) {
                    Query categoryQuery = LongPoint.newExactQuery("categoryId", categoryId);
                    booleanQuery.add(categoryQuery, BooleanClause.Occur.MUST);
                }

                // 4. 🎯 关键修复：标签过滤 - 使用新的查询方法
                if (tagIds != null && !tagIds.isEmpty()) {
                    Query tagQuery = buildTagQuery(tagIds);
                    if (tagQuery != null) {
                        booleanQuery.add(tagQuery, BooleanClause.Occur.MUST);
                    }
                }

                // 5. 日期范围过滤
                if (dateRange != null && !dateRange.trim().isEmpty()) {
                    long[] dateRangeMillis = parseDateRange(dateRange);
                    if (dateRangeMillis != null) {
                        Query dateQuery = LongPoint.newRangeQuery("createdTime", dateRangeMillis[0], dateRangeMillis[1]);
                        booleanQuery.add(dateQuery, BooleanClause.Occur.MUST);
                    }
                }

                // 6. 构建查询并排序
                Query query = booleanQuery.build();
                Sort sort = getSortByType(sortBy);
                TopDocs topDocs;

                if (sort != null) {
                    topDocs = searcher.search(query, Math.min(limit, 1000), sort);
                } else {
                    topDocs = searcher.search(query, Math.min(limit, 1000));
                }

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

                // 8. 限制最终结果数量
                if (results.size() > limit) {
                    results = results.subList(0, limit);
                }

                updateSearchHistoryResultCount(userId, keyword, results.size());
            } catch (Exception e) {
                System.err.println("高级搜索发生异常: " + e.getMessage());
                e.printStackTrace();
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

    /**
     * 🎯 关键修复：构建多标签查询（AND关系）
     * 查询格式：tagIds字段格式为 ",1,3,5,"
     * 查找包含 ",tagId," 的文档
     */
    private Query buildTagQuery(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return null;
        }

        BooleanQuery.Builder tagQueryBuilder = new BooleanQuery.Builder();

        System.out.println("🎯 构建标签查询，标签IDs: " + tagIds);

        for (Long tagId : tagIds) {
            // 🎯 关键修复：准确匹配格式 ",tagId,"
            // 避免误匹配，如查找"3"不会匹配到"13"或"35"
            Query tagQuery = new WildcardQuery(new Term("tagIds", "*," + tagId + ",*"));
            tagQueryBuilder.add(tagQuery, BooleanClause.Occur.MUST);

            System.out.println("  添加标签查询: *," + tagId + ",*");
        }

        Query finalQuery = tagQueryBuilder.build();
        System.out.println("✅ 标签查询构建完成");
        return finalQuery;
    }

    /**
     * 🎯 修复：分类内搜索 - 支持排序
     */
    public List<Long> searchByCategory(String keyword, Long categoryId, Long userId, int limit, String sortBy) throws IOException {
        saveSearchHistory(userId, keyword, "CATEGORY", 0);

        try {
            Directory directory = FSDirectory.open(Paths.get(indexDir));
            List<Long> results = new ArrayList<>();

            try (IndexReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();

                if (keyword != null && !keyword.trim().isEmpty()) {
                    Query keywordQuery = buildKeywordQuery(keyword.trim());
                    if (keywordQuery != null) {
                        booleanQuery.add(keywordQuery, BooleanClause.Occur.MUST);
                    }
                }

                Query userQuery = LongPoint.newExactQuery("userId", userId);
                booleanQuery.add(userQuery, BooleanClause.Occur.MUST);

                Query categoryQuery = LongPoint.newExactQuery("categoryId", categoryId);
                booleanQuery.add(categoryQuery, BooleanClause.Occur.MUST);

                // 按指定方式排序
                Sort sort = getSortByType(sortBy);
                TopDocs topDocs;

                if (sort != null) {
                    topDocs = searcher.search(booleanQuery.build(), limit, sort);
                } else {
                    topDocs = searcher.search(booleanQuery.build(), limit);
                }

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
     * 🎯 修复：标签搜索 - 支持排序
     */
    public List<Long> searchByTag(String keyword, Long tagId, Long userId, int limit, String sortBy) throws IOException {
        saveSearchHistory(userId, keyword, "TAG", 0);
        System.out.println("=== 开始标签搜索 ===");
        System.out.println("参数: 关键词='" + keyword + "', 标签ID=" + tagId + ", 排序=" + sortBy);

        try {
            // 使用标签查询直接筛选
            Directory directory = FSDirectory.open(Paths.get(indexDir));
            List<Long> results = new ArrayList<>();

            try (IndexReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();

                // 关键词搜索
                if (keyword != null && !keyword.trim().isEmpty()) {
                    Query keywordQuery = buildKeywordQuery(keyword.trim());
                    if (keywordQuery != null) {
                        booleanQuery.add(keywordQuery, BooleanClause.Occur.MUST);
                    }
                }

                // 用户过滤
                Query userQuery = LongPoint.newExactQuery("userId", userId);
                booleanQuery.add(userQuery, BooleanClause.Occur.MUST);

                // 🎯 标签过滤 - 使用新的查询格式
                if (tagId != null) {
                    Query tagQuery = new WildcardQuery(new Term("tagIds", "*," + tagId + ",*"));
                    booleanQuery.add(tagQuery, BooleanClause.Occur.MUST);
                    System.out.println("标签查询: *," + tagId + ",*");
                }

                // 排序
                Sort sort = getSortByType(sortBy);
                TopDocs topDocs;

                if (sort != null) {
                    topDocs = searcher.search(booleanQuery.build(), limit, sort);
                } else {
                    topDocs = searcher.search(booleanQuery.build(), limit);
                }

                System.out.println("Lucene标签搜索找到 " + topDocs.totalHits.value + " 个匹配");

                for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                    org.apache.lucene.document.Document doc = searcher.doc(scoreDoc.doc);
                    try {
                        Long docId = Long.parseLong(doc.get("id"));
                        String storedTagIds = doc.get("tagIds");
                        System.out.println("匹配文档ID: " + docId + ", 存储的标签IDs: " + storedTagIds);
                        results.add(docId);
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
     * 🎯 新增：验证标签查询功能
     */
    public List<Long> testTagSearch(List<Long> tagIds, Long userId) throws IOException {
        System.out.println("=== 测试标签查询 ===");
        System.out.println("测试参数: 标签IDs=" + tagIds + ", 用户ID=" + userId);

        List<Long> results = advancedSearch(null, null, tagIds, null, userId, 100, "relevance");

        System.out.println("测试结果: 找到 " + results.size() + " 个文档");

        // 详细输出前5个文档的标签信息
        if (!results.isEmpty()) {
            System.out.println("前5个文档的标签信息:");
            for (int i = 0; i < Math.min(results.size(), 5); i++) {
                Long docId = results.get(i);
                try {
                    List<com.dzy666.demo.entity.Tag> tags = tagService.getDocumentTags(docId, userId);
                    String tagInfo = tags.stream()
                            .map(tag -> tag.getId() + ":" + tag.getName())
                            .collect(Collectors.joining(", "));
                    System.out.println("  文档" + docId + ": " + tagInfo);
                } catch (Exception e) {
                    System.err.println("  获取文档" + docId + "的标签失败: " + e.getMessage());
                }
            }
        }

        return results;
    }

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
                case "all":
                    return null;
                default:
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

    public Map<String, Object> getSearchStatistics(Long userId) {
        Map<String, Object> stats = new HashMap<>();

        try {
            Map<String, Object> searchStats = searchHistoryMapper.getSearchStats(userId);
            if (searchStats != null) {
                stats.putAll(searchStats);
            }

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

    public void deleteDocument(Long docId) throws IOException {
        System.out.println("删除文档索引: " + docId);

        indexLock.lock();
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
        } finally {
            indexLock.unlock();
        }
    }

    public void rebuildIndex(Long userId) throws IOException {
        System.out.println("=== 开始重建索引，用户ID: " + userId + " ===");

        indexLock.lock();
        try {
            Directory directory = FSDirectory.open(Paths.get(indexDir));
            IndexWriterConfig config = new IndexWriterConfig(analyzer);

            try (IndexWriter writer = new IndexWriter(directory, config)) {
                writer.deleteAll();
                writer.commit();
                System.out.println("✅ 索引已清空");
            }

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
            indexLock.unlock();
            System.out.println("=== 索引重建结束 ===");
        }
    }

    private void saveSearchHistory(Long userId, String keyword, String searchType, int resultCount) {
        try {
            if (userId == null || keyword == null) {
                System.err.println("保存搜索历史失败：参数为空");
                return;
            }

            int exists = searchHistoryMapper.existsToday(userId, keyword);

            if (exists > 0) {
                searchHistoryMapper.updateResultCount(userId, keyword, resultCount);
                System.out.println("更新今日搜索历史: " + keyword + ", 用户: " + userId);
            } else {
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

    private void updateSearchHistoryResultCount(Long userId, String keyword, int resultCount) {
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                System.out.println("关键词为空，跳过更新");
                return;
            }

            int updated = searchHistoryMapper.updateResultCount(userId, keyword, resultCount);

            if (updated > 0) {
                System.out.println("成功更新搜索结果数量: 关键词=" + keyword +
                        ", 结果数=" + resultCount + ", 影响行数=" + updated);
            } else {
                System.out.println("未找到匹配的搜索历史记录，创建新的...");

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

    public List<String> getSearchSuggestions(String keyword, Long userId) {
        List<String> suggestions = new ArrayList<>();

        if (keyword == null || keyword.trim().isEmpty()) {
            return suggestions;
        }

        try {
            List<String> historySuggestions = searchHistoryMapper.findKeywordsByPrefix(userId, keyword, 5);
            suggestions.addAll(historySuggestions);

            System.out.println("从历史记录获取建议: " + historySuggestions.size() + " 个");

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
            suggestions.add(keyword + " 相关内容");
            suggestions.add(keyword + " 文档");
        }

        return suggestions.stream()
                .distinct()
                .limit(10)
                .collect(Collectors.toList());
    }
}