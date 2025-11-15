import api from './index';

export const favoriteApi = {
    // 添加收藏
    addFavorite(documentId, userId) {
        return api.post(`/favorite/document/${documentId}`, null, {
            params: { userId }
        });
    },

    // 取消收藏
    removeFavorite(documentId, userId) {
        return api.delete(`/favorite/document/${documentId}`, {
            params: { userId }
        });
    },

    // 检查是否收藏
    isFavorite(documentId, userId) {
        return api.get(`/favorite/document/${documentId}`, {
            params: { userId }
        });
    },

    // 获取用户收藏列表
    getUserFavorites(userId) {
        return api.get(`/favorite/user/${userId}`);
    },

    // 获取文档收藏数
    getFavoriteCount(documentId) {
        return api.get(`/favorite/document/${documentId}/count`);
    }
};