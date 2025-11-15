import axios from 'axios';
import { ElMessage } from 'element-plus';

// 创建axios实例
const api = axios.create({
    baseURL: 'http://localhost:8080/api',
    timeout: 10000,
    withCredentials: true // 允许携带cookie
});

// 请求拦截器
api.interceptors.request.use(
    config => {
        // 从localStorage获取token（如果使用JWT）
        const token = localStorage.getItem('token');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }

        // 添加用户ID到请求头（如果需要）
        const currentUser = JSON.parse(localStorage.getItem('currentUser') || '{}');
        if (currentUser.id) {
            config.headers['X-User-Id'] = currentUser.id;
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
        const res = response.data;

        // 根据你的JsonResult格式处理
        if (res.success === false) {
            ElMessage.error(res.message || '请求失败');
            return Promise.reject(new Error(res.message || 'Error'));
        }

        return res;
    },
    error => {
        if (error.response?.status === 401) {
            ElMessage.error('登录已过期，请重新登录');
            localStorage.removeItem('currentUser');
            localStorage.removeItem('token');
            window.location.href = '/login';
        } else if (error.response?.status === 403) {
            ElMessage.error('没有权限执行此操作');
        } else if (error.response?.status === 500) {
            ElMessage.error('服务器内部错误');
        } else {
            ElMessage.error('网络错误或服务器异常');
        }
        return Promise.reject(error);
    }
);

export default api;