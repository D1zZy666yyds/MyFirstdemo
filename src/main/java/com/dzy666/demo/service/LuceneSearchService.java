package com.dzy666.demo.service;
// LuceneSearchService.java

import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class LuceneSearchService {

    private final String indexDir = "lucene-index";
    private SmartChineseAnalyzer analyzer = new SmartChineseAnalyzer();

    public void indexDocument(Long docId, String title, String content) throws IOException {
        Directory directory = FSDirectory.open(Paths.get(indexDir));
        IndexWriterConfig config = new IndexWriterConfig(analyzer);

        try (IndexWriter writer = new IndexWriter(directory, config)) {
            Document doc = new Document();
            doc.add(new StringField("id", docId.toString(), Field.Store.YES));
            doc.add(new TextField("title", title, Field.Store.YES));
            doc.add(new TextField("content", content, Field.Store.YES));

            writer.updateDocument(new Term("id", docId.toString()), doc);
            writer.commit();
        }
    }

    public List<Long> search(String keyword, int limit) throws IOException {
        Directory directory = FSDirectory.open(Paths.get(indexDir));
        List<Long> results = new ArrayList<>();

        try (IndexReader reader = DirectoryReader.open(directory)) {
            IndexSearcher searcher = new IndexSearcher(reader);

            BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();

            Query titleQuery = new TermQuery(new Term("title", keyword));
            Query contentQuery = new TermQuery(new Term("content", keyword));

            booleanQuery.add(titleQuery, BooleanClause.Occur.SHOULD);
            booleanQuery.add(contentQuery, BooleanClause.Occur.SHOULD);

            TopDocs topDocs = searcher.search(booleanQuery.build(), limit);

            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                org.apache.lucene.document.Document doc = searcher.doc(scoreDoc.doc);
                results.add(Long.parseLong(doc.get("id")));
            }
        }

        return results;
    }

    public void deleteDocument(Long docId) throws IOException {
        Directory directory = FSDirectory.open(Paths.get(indexDir));
        IndexWriterConfig config = new IndexWriterConfig(analyzer);

        try (IndexWriter writer = new IndexWriter(directory, config)) {
            writer.deleteDocuments(new Term("id", docId.toString()));
            writer.commit();
        }
    }
}