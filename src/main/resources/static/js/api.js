// API基础配置
const API_BASE_URL = 'http://localhost:8080/api';

// 创建axios实例
const api = axios.create({
    baseURL: API_BASE_URL,
    timeout: 10000,
    withCredentials: true
});

// 请求拦截器
api.interceptors.request.use(
    config => {
        const token = localStorage.getItem('token');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    error => {
        return Promise.reject(error);
    }
);

// 响应拦截器
api.interceptors.response.use(
    response => {
        return response.data;
    },
    error => {
        if (error.response?.status === 401) {
            alert('登录已过期，请重新登录');
            localStorage.removeItem('currentUser');
            localStorage.removeItem('token');
            window.location.href = '/login';
        }
        return Promise.reject(error);
    }
);

// 认证API
const authApi = {
    login(username, password) {
        return api.post('/user/login', { username, password });
    },
    register(username, email, password) {
        return api.post('/user/register', { username, email, password });
    },
    logout() {
        return api.post('/logout');
    },
    getUserInfo() {
        return api.get('/userinfo');
    },
    getDashboardStats() {
        return api.get('/dashboard/stats');
    }
};

// 文档API
const documentApi = {
    createDocument(documentData) {
        return api.post('/document', documentData);
    },
    getDocument(id, userId) {
        return api.get(`/document/${id}`, { params: { userId } });
    },
    updateDocument(id, documentData) {
        return api.put(`/document/${id}`, documentData);
    },
    deleteDocument(id, userId) {
        return api.delete(`/document/${id}`, { params: { userId } });
    },
    getUserDocuments(userId) {
        return api.get(`/document/user/${userId}`);
    },
    getDocumentsByCategory(categoryId, userId) {
        return api.get(`/document/category/${categoryId}`, { params: { userId } });
    }
};

// 分类API
const categoryApi = {
    createCategory(categoryData) {
        return api.post('/category', categoryData);
    },
    getCategoryTree(userId) {
        return api.get(`/category/user/${userId}/tree`);
    },
    updateCategory(id, categoryData) {
        return api.put(`/category/${id}`, categoryData);
    },
    deleteCategory(id, userId) {
        return api.delete(`/category/${id}`, { params: { userId } });
    },
    getUserCategories(userId) {
        return api.get(`/category/user/${userId}`);
    }
};

// 标签API
const tagApi = {
    createTag(tagData) {
        return api.post('/tag', tagData);
    },
    getUserTags(userId) {
        return api.get(`/tag/user/${userId}`);
    },
    deleteTag(id, userId) {
        return api.delete(`/tag/${id}`, { params: { userId } });
    },
    addTagToDocument(documentId, tagId, userId) {
        return api.post(`/tag/document/${documentId}/tag/${tagId}`, null, { params: { userId } });
    },
    removeTagFromDocument(documentId, tagId, userId) {
        return api.delete(`/tag/document/${documentId}/tag/${tagId}`, { params: { userId } });
    }
};

// 搜索API
const searchApi = {
    search(keyword, userId, limit = 10) {
        return api.get('/search', { params: { keyword, userId, limit } });
    }
};

// 收藏API
const favoriteApi = {
    addFavorite(documentId, userId) {
        return api.post(`/favorite/document/${documentId}`, null, { params: { userId } });
    },
    removeFavorite(documentId, userId) {
        return api.delete(`/favorite/document/${documentId}`, { params: { userId } });
    },
    getUserFavorites(userId) {
        return api.get(`/favorite/user/${userId}`);
    }
};