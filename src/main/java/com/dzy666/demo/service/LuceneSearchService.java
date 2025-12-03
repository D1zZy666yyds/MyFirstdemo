package com.dzy666.demo.service;

import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LuceneSearchService {

    private final String indexDir = "lucene-index";
    private SmartChineseAnalyzer analyzer = new SmartChineseAnalyzer();

    // 🎯 添加构造函数，确保索引目录存在
    public LuceneSearchService() {
        System.out.println("=== LuceneSearchService 初始化 ===");
        System.out.println("索引目录: " + new File(indexDir).getAbsolutePath());
        ensureIndexDirExists();
        System.out.println("=== LuceneSearchService 初始化完成 ===");
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

    public void indexDocument(Long docId, String title, String content) throws IOException {
        System.out.println("索引文档: " + docId + " - " + (title != null ? title.substring(0, Math.min(title.length(), 50)) : "无标题"));

        try {
            // 确保索引目录存在
            ensureIndexDirExists();

            Directory directory = FSDirectory.open(Paths.get(indexDir));
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

            try (IndexWriter writer = new IndexWriter(directory, config)) {
                Document doc = new Document();
                doc.add(new StringField("id", docId.toString(), Field.Store.YES));

                // 标题字段
                if (title != null && !title.trim().isEmpty()) {
                    doc.add(new TextField("title", title, Field.Store.YES));
                } else {
                    doc.add(new TextField("title", "无标题", Field.Store.YES));
                }

                // 内容字段
                if (content != null && !content.trim().isEmpty()) {
                    doc.add(new TextField("content", content, Field.Store.YES));
                } else {
                    doc.add(new TextField("content", "无内容", Field.Store.YES));
                }

                writer.updateDocument(new Term("id", docId.toString()), doc);
                writer.commit();
                System.out.println("✅ 文档索引更新成功: " + docId);
            }
        } catch (Exception e) {
            System.err.println("❌ 索引文档失败: " + docId + " - " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public List<Long> search(String keyword, int limit) throws IOException {
        System.out.println("=== LuceneSearchService 搜索开始 ===");
        System.out.println("关键词: '" + keyword + "', 限制: " + limit);

        try {
            // 确保索引目录存在
            ensureIndexDirExists();

            Directory directory = FSDirectory.open(Paths.get(indexDir));
            List<Long> results = new ArrayList<>();

            // 🎯 安全的IndexReader创建
            if (!DirectoryReader.indexExists(directory)) {
                System.err.println("❌ 索引不存在，创建空索引");
                initializeEmptyIndex();
                return results; // 返回空结果
            }

            try (IndexReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);

                BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();

                Query titleQuery = new TermQuery(new Term("title", keyword));
                Query contentQuery = new TermQuery(new Term("content", keyword));

                booleanQuery.add(titleQuery, BooleanClause.Occur.SHOULD);
                booleanQuery.add(contentQuery, BooleanClause.Occur.SHOULD);

                TopDocs topDocs = searcher.search(booleanQuery.build(), limit);

                System.out.println("Lucene搜索找到 " + topDocs.totalHits.value + " 个匹配");

                for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                    org.apache.lucene.document.Document doc = searcher.doc(scoreDoc.doc);
                    try {
                        Long docId = Long.parseLong(doc.get("id"));
                        results.add(docId);
                        System.out.println("匹配文档ID: " + docId + ", 标题: " + doc.get("title"));
                    } catch (NumberFormatException e) {
                        System.err.println("解析文档ID失败: " + doc.get("id"));
                    }
                }
            }

            return results;
        } catch (Exception e) {
            System.err.println("❌ Lucene搜索失败: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>(); // 返回空结果而不是抛出异常
        } finally {
            System.out.println("=== LuceneSearchService 搜索结束 ===");
        }
    }

    public void deleteDocument(Long docId) throws IOException {
        System.out.println("删除文档索引: " + docId);

        try {
            // 确保索引目录存在
            ensureIndexDirExists();

            Directory directory = FSDirectory.open(Paths.get(indexDir));
            IndexWriterConfig config = new IndexWriterConfig(analyzer);

            try (IndexWriter writer = new IndexWriter(directory, config)) {
                writer.deleteDocuments(new Term("id", docId.toString()));
                writer.commit();
                System.out.println("✅ 文档索引删除成功: " + docId);
            }
        } catch (Exception e) {
            System.err.println("❌ 删除文档索引失败: " + docId + " - " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    // 🎯 新增：获取索引状态
    public Map<String, Object> getIndexStatus() {
        Map<String, Object> status = new HashMap<>();

        try {
            File dir = new File(indexDir);
            status.put("directoryExists", dir.exists());
            status.put("directoryPath", dir.getAbsolutePath());
            status.put("writable", dir.canWrite());

            if (dir.exists()) {
                Directory directory = FSDirectory.open(Paths.get(indexDir));
                if (DirectoryReader.indexExists(directory)) {
                    try (IndexReader reader = DirectoryReader.open(directory)) {
                        status.put("indexExists", true);
                        status.put("documentCount", reader.numDocs());
                        status.put("maxDoc", reader.maxDoc());
                        status.put("hasDeletions", reader.hasDeletions());
                    }
                } else {
                    status.put("indexExists", false);
                }
            }
        } catch (Exception e) {
            status.put("error", e.getMessage());
        }

        return status;
    }

    // 🎯 新增：清空所有索引
    public void clearAllIndexes() throws IOException {
        System.out.println("清空所有索引...");

        try {
            ensureIndexDirExists();

            Directory directory = FSDirectory.open(Paths.get(indexDir));
            IndexWriterConfig config = new IndexWriterConfig(analyzer);

            try (IndexWriter writer = new IndexWriter(directory, config)) {
                writer.deleteAll();
                writer.commit();
                System.out.println("✅ 所有索引已清空");
            }
        } catch (Exception e) {
            System.err.println("❌ 清空索引失败: " + e.getMessage());
            throw e;
        }
    }
}