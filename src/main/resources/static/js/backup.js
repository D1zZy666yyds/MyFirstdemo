import api from './index';

export const backupApi = {
    // 创建完整备份
    createFullBackup(userId, description) {
        return api.post('/backup/full', {
            userId,
            description
        });
    },

    // 创建增量备份
    createIncrementalBackup(userId, description, since) {
        return api.post('/backup/incremental', {
            userId,
            description,
            since
        });
    },

    // 获取用户备份列表
    getUserBackups(userId) {
        return api.get(`/backup/user/${userId}`);
    },

    // 获取备份详情
    getBackup(backupId) {
        return api.get(`/backup/${backupId}`);
    },

    // 删除备份
    deleteBackup(backupId, userId) {
        return api.delete(`/backup/${backupId}`, {
            params: { userId }
        });
    },

    // 获取备份统计
    getBackupStats(userId) {
        return api.get(`/backup/stats/${userId}`);
    },

    // 从备份恢复
    restoreFromBackup(backupId, userId) {
        return api.post(`/backup/restore/${backupId}`, null, {
            params: { userId }
        });
    },

    // 获取备份预览
    getBackupPreview(backupId, userId) {
        return api.get(`/backup/preview/${backupId}`, {
            params: { userId }
        });
    }
};