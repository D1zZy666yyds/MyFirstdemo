package com.dzy666.demo.service;

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
import java.util.ArrayList;
import java.util.List;

@Service
public class SearchService {

    private final String indexDir = "lucene-index";
    private SmartChineseAnalyzer analyzer = new SmartChineseAnalyzer();

    // 使用 @Lazy 注解打破循环依赖
    @Autowired
    @Lazy
    private DocumentService documentService;

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

            writer.addDocument(luceneDoc);
            writer.commit();
        }
    }

    /**
     * 搜索文档
     */
    public List<Long> search(String keyword, Long userId, int limit) throws IOException {
        Directory directory = FSDirectory.open(Paths.get(indexDir));
        List<Long> results = new ArrayList<>();

        try (IndexReader reader = DirectoryReader.open(directory)) {
            IndexSearcher searcher = new IndexSearcher(reader);

            // 构建查询：搜索标题和内容，并且限制用户
            BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();

            // 标题搜索
            Query titleQuery = new TermQuery(new Term("title", keyword));
            booleanQuery.add(titleQuery, BooleanClause.Occur.SHOULD);

            // 内容搜索
            Query contentQuery = new TermQuery(new Term("content", keyword));
            booleanQuery.add(contentQuery, BooleanClause.Occur.SHOULD);

            // 用户过滤
            Query userQuery = LongPoint.newExactQuery("userId", userId);
            booleanQuery.add(userQuery, BooleanClause.Occur.MUST);

            TopDocs topDocs = searcher.search(booleanQuery.build(), limit);

            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                org.apache.lucene.document.Document doc = searcher.doc(scoreDoc.doc);
                results.add(Long.parseLong(doc.get("id")));
            }
        }

        return results;
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
}