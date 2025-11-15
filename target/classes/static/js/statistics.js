import api from './index';

export const statisticsApi = {
    // 获取用户总体统计
    getUserOverview(userId) {
        return api.get(`/statistics/overview/${userId}`);
    },

    // 获取分类分布
    getCategoryDistribution(userId) {
        return api.get(`/statistics/category-distribution/${userId}`);
    },

    // 获取标签使用统计
    getTagUsage(userId) {
        return api.get(`/statistics/tag-usage/${userId}`);
    },

    // 获取文档创建趋势
    getCreationTrend(userId, months = 6) {
        return api.get(`/statistics/creation-trend/${userId}`, {
            params: { months }
        });
    },

    // 获取热门文档
    getPopularDocuments(userId, limit = 10) {
        return api.get(`/statistics/popular-documents/${userId}`, {
            params: { limit }
        });
    }
};