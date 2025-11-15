import api from './index';

export const searchApi = {
    // 搜索文档
    search(keyword, userId, limit = 10) {
        return api.get('/search', {
            params: { keyword, userId, limit }
        });
    },

    // 重建索引
    rebuildIndex(userId) {
        return api.post('/search/rebuild', null, {
            params: { userId }
        });
    }
};