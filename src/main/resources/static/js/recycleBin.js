import api from './index';

export const recycleBinApi = {
    // 获取回收站文档
    getDeletedDocuments(userId) {
        return api.get(`/recycle-bin/user/${userId}`);
    },

    // 恢复文档
    restoreDocument(documentId, userId) {
        return api.post(`/recycle-bin/restore/${documentId}`, null, {
            params: { userId }
        });
    },

    // 彻底删除文档
    permanentDelete(documentId, userId) {
        return api.delete(`/recycle-bin/permanent/${documentId}`, {
            params: { userId }
        });
    },

    // 清空回收站
    clearRecycleBin(userId) {
        return api.delete('/recycle-bin/clear', {
            params: { userId }
        });
    }
};