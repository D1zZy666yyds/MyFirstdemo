import api from './index';

export const categoryApi = {
    // 创建分类
    createCategory(categoryData) {
        return api.post('/category', categoryData);
    },

    // 获取分类详情
    getCategory(id, userId) {
        return api.get(`/category/${id}`, {
            params: { userId }
        });
    },

    // 更新分类
    updateCategory(id, categoryData) {
        return api.put(`/category/${id}`, categoryData);
    },

    // 删除分类
    deleteCategory(id, userId) {
        return api.delete(`/category/${id}`, {
            params: { userId }
        });
    },

    // 获取用户所有分类
    getUserCategories(userId) {
        return api.get(`/category/user/${userId}`);
    },

    // 获取根分类
    getRootCategories(userId) {
        return api.get(`/category/user/${userId}/root`);
    },

    // 获取分类树
    getCategoryTree(userId) {
        return api.get(`/category/user/${userId}/tree`);
    },

    // 获取子分类
    getChildCategories(userId, parentId) {
        return api.get(`/category/user/${userId}/parent/${parentId}`);
    },

    // 公开测试接口
    publicTest(data) {
        return api.post('/category/public-test', data);
    }
};