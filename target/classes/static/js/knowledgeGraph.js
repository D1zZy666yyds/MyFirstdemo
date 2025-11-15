import api from './index';

export const knowledgeGraphApi = {
    // 获取完整知识图谱
    getFullKnowledgeGraph(userId) {
        return api.get(`/knowledge-graph/full/${userId}`);
    },

    // 获取文档关联关系
    getDocumentRelations(userId) {
        return api.get(`/knowledge-graph/document-relations/${userId}`);
    },

    // 获取标签云数据
    getTagCloud(userId) {
        return api.get(`/knowledge-graph/tag-cloud/${userId}`);
    },

    // 获取学习路径分析
    getLearningPath(userId) {
        return api.get(`/knowledge-graph/learning-path/${userId}`);
    }
};