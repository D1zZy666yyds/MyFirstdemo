import api from './index';

export const tagApi = {
    // 创建标签
    createTag(tagData) {
        return api.post('/tag', tagData);
    },

    // 获取标签详情
    getTag(id, userId) {
        return api.get(`/tag/${id}`, {
            params: { userId }
        });
    },

    // 更新标签
    updateTag(id, tagData) {
        return api.put(`/tag/${id}`, tagData);
    },

    // 删除标签
    deleteTag(id, userId) {
        return api.delete(`/tag/${id}`, {
            params: { userId }
        });
    },

    // 获取用户所有标签
    getUserTags(userId) {
        return api.get(`/tag/user/${userId}`);
    },

    // 获取文档的标签
    getDocumentTags(documentId, userId) {
        return api.get(`/tag/document/${documentId}`, {
            params: { userId }
        });
    },

    // 为文档添加标签
    addTagToDocument(documentId, tagId, userId) {
        return api.post(`/tag/document/${documentId}/tag/${tagId}`, null, {
            params: { userId }
        });
    },

    // 从文档移除标签
    removeTagFromDocument(documentId, tagId, userId) {
        return api.delete(`/tag/document/${documentId}/tag/${tagId}`, {
            params: { userId }
        });
    }
};