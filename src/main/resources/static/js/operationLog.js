import api from './index';

export const operationLogApi = {
    // 获取用户操作日志
    getUserLogs(userId) {
        return api.get(`/operation-logs/user/${userId}`);
    },

    // 获取最近日志
    getRecentLogs(limit = 50) {
        return api.get('/operation-logs/recent', {
            params: { limit }
        });
    },

    // 根据类型获取日志
    getLogsByType(operationType) {
        return api.get(`/operation-logs/type/${operationType}`);
    },

    // 清理旧日志
    cleanOldLogs() {
        return api.delete('/operation-logs/clean');
    }
};